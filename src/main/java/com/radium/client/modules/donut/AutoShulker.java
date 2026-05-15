package com.radium.client.modules.donut;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ModeSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.ChatUtils;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoShulker extends Module {

    private static final long WAIT_TIME_MS = 50;
    private static final int MAX_BULK_BUY = 5;
    private final ModeSetting<ItemMode> itemMode = new ModeSetting<>("Item Mode", ItemMode.SHULKERS, ItemMode.class);
    private final ModeSetting<Action> action = new ModeSetting<>("Action", Action.BUY_AND_SELL, Action.class);
    private final StringSetting minPrice = new StringSetting("Min Price", "850");
    private final BooleanSetting notifications = new BooleanSetting("Notifications", true);
    private final BooleanSetting speedMode = new BooleanSetting("Speed Mode", true);
    private final BooleanSetting enableTargeting = new BooleanSetting("Enable Targeting", false);
    private final StringSetting targetPlayerName = new StringSetting("Target Player", "");
    private final BooleanSetting targetOnlyMode = new BooleanSetting("Target Only Mode", false);
    private final BooleanSetting autoDrop = new BooleanSetting("Auto Drop", true);
    private Stage stage = Stage.NONE;
    private long stageStart = 0;
    private int itemMoveIndex = 0;
    private long lastItemMoveTime = 0;
    private int exitCount = 0;
    private int finalExitCount = 0;
    private long finalExitStart = 0;
    private int bulkBuyCount = 0;
    private String targetPlayer = "";
    private boolean isTargetingActive = false;

    public AutoShulker() {
        super("AutoShulker", "Automatically buys/sells shulkers and shulker shells with player targeting", Category.DONUT);
        this.addSettings(itemMode, action, minPrice, notifications, speedMode, enableTargeting, targetPlayerName, targetOnlyMode, autoDrop);
    }

    @Override
    public void onEnable() {
        double parsedPrice = parsePrice(minPrice.getValue());
        if (parsedPrice == -1.0 && !enableTargeting.getValue()) {
            if (notifications.getValue()) {
                ChatUtils.e("Invalid minimum price format!");
            }
            toggle();
            return;
        }

        updateTargetPlayer();

        if (action.getValue() == Action.SELL_ONLY || action.getValue() == Action.ORDER_ONLY) {
            stage = Stage.WAIT;
        } else {
            stage = Stage.SHOP;
        }

        stageStart = System.currentTimeMillis();
        itemMoveIndex = 0;
        lastItemMoveTime = 0;
        exitCount = 0;
        finalExitCount = 0;
        bulkBuyCount = 0;

        if (notifications.getValue()) {
            String modeInfo = isTargetingActive ? String.format(" | Targeting: %s", targetPlayer) : "";
            String itemInfo = itemMode.getValue() == ItemMode.SHULKERS ? "Shulkers" : "Shulker Shells";
            ChatUtils.m(String.format("Activated! Mode: %s | Action: %s | Min: %s%s",
                    itemInfo, action.getValue(), minPrice.getValue(), modeInfo));
        }
    }

    @Override
    public void onDisable() {
        stage = Stage.NONE;
    }

    private void updateTargetPlayer() {
        targetPlayer = "";
        isTargetingActive = false;

        if (enableTargeting.getValue() && !targetPlayerName.getValue().trim().isEmpty()) {
            targetPlayer = targetPlayerName.getValue().trim();
            isTargetingActive = true;

            if (notifications.getValue()) {
                ChatUtils.m("Targeting enabled for player: " + targetPlayer);
            }
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();

        switch (stage) {
            case TARGET_ORDERS -> {
                mc.player.networkHandler.sendChatCommand("orders " + targetPlayer);
                stage = Stage.ORDERS;
                stageStart = now;

                if (notifications.getValue()) {
                    ChatUtils.m("Checking orders for: " + targetPlayer);
                }
            }
            case SHOP -> {
                if (action.getValue() == Action.SELL_ONLY || action.getValue() == Action.ORDER_ONLY) {
                    stage = Stage.WAIT;
                    stageStart = now;
                    return;
                }
                mc.player.networkHandler.sendChatCommand("shop");
                stage = Stage.SHOP_END;
                stageStart = now;
            }
            case SHOP_END -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    for (Slot slot : handler.slots) {
                        ItemStack stack = slot.getStack();
                        if (!stack.isEmpty() && isEndStone(stack)) {
                            mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                            stage = Stage.SHOP_ITEM;
                            stageStart = now;
                            bulkBuyCount = 0;
                            return;
                        }
                    }
                    if (now - stageStart > (speedMode.getValue() ? 1000 : 3000)) {
                        mc.player.closeHandledScreen();
                        stage = Stage.SHOP;
                        stageStart = now;
                    }
                }
            }
            case SHOP_ITEM -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    boolean foundItem = false;

                    for (Slot slot : handler.slots) {
                        ItemStack stack = slot.getStack();
                        boolean isCorrectItem = (itemMode.getValue() == ItemMode.SHULKERS && isShulkerBox(stack)) ||
                                (itemMode.getValue() == ItemMode.SHULKER_SHELLS && isShulkerShell(stack));

                        if (!stack.isEmpty() && isCorrectItem) {
                            if (itemMode.getValue() == ItemMode.SHULKERS) {

                                int clickCount = speedMode.getValue() ? 10 : 5;
                                for (int i = 0; i < clickCount; i++) {
                                    mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                }
                                stage = Stage.SHOP_CONFIRM;
                            } else {

                                mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                stage = Stage.SHOP_GLASS_PANE;
                            }
                            foundItem = true;
                            bulkBuyCount++;
                            break;
                        }
                    }

                    if (foundItem) {
                        stageStart = now;
                        return;
                    }
                    if (now - stageStart > (speedMode.getValue() ? 500 : 1500)) {
                        mc.player.closeHandledScreen();
                        stage = Stage.SHOP;
                        stageStart = now;
                    }
                }
            }
            case SHOP_GLASS_PANE -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();

                    for (Slot slot : handler.slots) {
                        ItemStack stack = slot.getStack();
                        if (!stack.isEmpty() && isGlassPane(stack) && stack.getCount() == 64) {
                            mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                            stage = Stage.SHOP_BUY;
                            stageStart = now;
                            return;
                        }
                    }

                    if (now - stageStart > (speedMode.getValue() ? 300 : 1000)) {
                        mc.player.closeHandledScreen();
                        stage = Stage.SHOP;
                        stageStart = now;
                    }
                }
            }
            case SHOP_BUY -> {
                long waitDelay = speedMode.getValue() ? 500 : 1000;
                if (now - stageStart >= waitDelay) {
                    if (mc.currentScreen instanceof GenericContainerScreen screen) {
                        ScreenHandler handler = screen.getScreenHandler();

                        for (Slot slot : handler.slots) {
                            ItemStack stack = slot.getStack();
                            if (!stack.isEmpty() && isGreenGlass(stack) && stack.getCount() == 1) {
                                int maxClicks = speedMode.getValue() ? 50 : 30;
                                for (int i = 0; i < maxClicks; i++) {
                                    mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                    if (isInventoryFull()) break;
                                }
                                stage = Stage.SHOP_CHECK_FULL;
                                stageStart = now;
                                return;
                            }
                        }

                        if (now - stageStart > (speedMode.getValue() ? 2000 : 3000)) {
                            stage = Stage.SHOP_GLASS_PANE;
                            stageStart = now;
                        }
                    }
                }
            }
            case SHOP_CONFIRM -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    boolean foundGreen = false;
                    for (Slot slot : handler.slots) {
                        ItemStack stack = slot.getStack();
                        if (!stack.isEmpty() && isGreenGlass(stack)) {
                            for (int i = 0; i < (speedMode.getValue() ? 3 : 2); i++) {
                                mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                            }
                            foundGreen = true;
                            break;
                        }
                    }
                    if (foundGreen) {
                        stage = Stage.SHOP_CHECK_FULL;
                        stageStart = now;
                        return;
                    }
                    if (now - stageStart > (speedMode.getValue() ? 200 : 800)) {
                        stage = Stage.SHOP_ITEM;
                        stageStart = now;
                    }
                }
            }
            case SHOP_CHECK_FULL -> {
                if (now - stageStart > (speedMode.getValue() ? 100 : 200)) {
                    if (isInventoryFull() || action.getValue() == Action.BUY_ONLY) {
                        mc.player.closeHandledScreen();
                        stage = Stage.SHOP_EXIT;
                        stageStart = now;
                    } else {
                        if (now - stageStart > (speedMode.getValue() ? 200 : 400)) {
                            stage = Stage.SHOP_ITEM;
                            stageStart = now;
                        }
                    }
                }
            }
            case SHOP_EXIT -> {
                if (mc.currentScreen == null) {
                    if (action.getValue() == Action.BUY_ONLY) {
                        if (autoDrop.getValue()) {
                            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                                    PlayerActionC2SPacket.Action.DROP_ALL_ITEMS,
                                    BlockPos.ORIGIN,
                                    Direction.DOWN
                            ));
                        }
                        stage = Stage.CYCLE_PAUSE;
                    } else {
                        stage = Stage.WAIT;
                    }
                    stageStart = now;
                }
                if (now - stageStart > (speedMode.getValue() ? 1000 : 5000)) {
                    mc.player.closeHandledScreen();
                    stage = Stage.SHOP;
                    stageStart = now;
                }
            }
            case WAIT -> {
                if (action.getValue() == Action.BUY_ONLY) {
                    stage = Stage.SHOP;
                    stageStart = now;
                    return;
                }

                long waitTime = speedMode.getValue() ? 25 : WAIT_TIME_MS;
                if (now - stageStart >= waitTime) {
                    if (isTargetingActive && !targetPlayer.isEmpty()) {
                        stage = Stage.TARGET_ORDERS;
                    } else {
                        String orderCommand = itemMode.getValue() == ItemMode.SHULKERS ? "orders shulker" : "orders shulker shell";
                        mc.player.networkHandler.sendChatCommand(orderCommand);
                        stage = Stage.ORDERS;
                    }
                    stageStart = now;
                }
            }
            case ORDERS -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    boolean foundOrder = false;

                    if (speedMode.getValue() && now - stageStart < 200) {
                        return;
                    }

                    for (Slot slot : handler.slots) {
                        ItemStack stack = slot.getStack();
                        boolean isCorrectItem = (itemMode.getValue() == ItemMode.SHULKERS && isShulkerBox(stack) && isPurple(stack)) ||
                                (itemMode.getValue() == ItemMode.SHULKER_SHELLS && isShulkerShell(stack));

                        if (!stack.isEmpty() && isCorrectItem) {
                            boolean shouldTakeOrder = false;
                            String orderPlayer = getOrderPlayerName(stack);
                            double orderPrice = getOrderPrice(stack);


                            if (itemMode.getValue() == ItemMode.SHULKER_SHELLS && orderPrice > 1500) {
                                continue;
                            }

                            boolean isTargetedOrder = isTargetingActive &&
                                    orderPlayer != null &&
                                    orderPlayer.equalsIgnoreCase(targetPlayer);

                            if (isTargetedOrder) {
                                shouldTakeOrder = true;
                                if (notifications.getValue()) {
                                    ChatUtils.m(String.format("Found TARGET order from %s: %s",
                                            orderPlayer, orderPrice > 0 ? formatPrice(orderPrice) : "Unknown price"));
                                }
                            } else if (!targetOnlyMode.getValue()) {
                                double minPriceValue = parsePrice(minPrice.getValue());
                                if (orderPrice >= minPriceValue) {
                                    shouldTakeOrder = true;
                                    if (notifications.getValue()) {
                                        ChatUtils.m("Found order: " + formatPrice(orderPrice));
                                    }
                                }
                            }

                            if (shouldTakeOrder) {
                                mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                stage = Stage.ORDERS_SELECT;
                                stageStart = now + (speedMode.getValue() ? 100 : 50);
                                itemMoveIndex = 0;
                                lastItemMoveTime = 0;
                                foundOrder = true;

                                if (notifications.getValue()) {
                                    ChatUtils.m("Selected order, preparing to transfer items...");
                                }
                                return;
                            }
                        }
                    }

                    if (!foundOrder && now - stageStart > (speedMode.getValue() ? 3000 : 5000)) {
                        mc.player.closeHandledScreen();
                        if (action.getValue() == Action.ORDER_ONLY) {
                            stage = Stage.CYCLE_PAUSE;
                        } else {
                            stage = Stage.SHOP;
                        }
                        stageStart = now;
                    }
                }
            }
            case ORDERS_SELECT -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();

                    if (itemMoveIndex >= 36) {
                        mc.player.closeHandledScreen();
                        stage = Stage.ORDERS_CONFIRM;
                        stageStart = now;
                        itemMoveIndex = 0;
                        return;
                    }

                    long moveDelay = speedMode.getValue() ? 10 : 100;
                    if (now - lastItemMoveTime >= moveDelay) {
                        int batchSize = speedMode.getValue() ? 3 : 1;

                        for (int batch = 0; batch < batchSize && itemMoveIndex < 36; batch++) {
                            ItemStack stack = mc.player.getInventory().getStack(itemMoveIndex);
                            boolean isCorrectItem = (itemMode.getValue() == ItemMode.SHULKERS && isShulkerBox(stack)) ||
                                    (itemMode.getValue() == ItemMode.SHULKER_SHELLS && isShulkerShell(stack));

                            if (isCorrectItem) {
                                int playerSlotId = -1;
                                for (Slot slot : handler.slots) {
                                    if (slot.inventory == mc.player.getInventory() && slot.getIndex() == itemMoveIndex) {
                                        playerSlotId = slot.id;
                                        break;
                                    }
                                }

                                if (playerSlotId != -1) {
                                    mc.interactionManager.clickSlot(handler.syncId, playerSlotId, 0, SlotActionType.QUICK_MOVE, mc.player);
                                }
                            }
                            itemMoveIndex++;
                        }
                        lastItemMoveTime = now;
                    }
                }
            }
            case ORDERS_EXIT -> {
                if (mc.currentScreen == null) {
                    exitCount++;
                    if (exitCount < 2) {
                        mc.player.closeHandledScreen();
                        stageStart = now;
                    } else {
                        exitCount = 0;
                        stage = Stage.ORDERS_CONFIRM;
                        stageStart = now;
                    }
                }
            }
            case ORDERS_CONFIRM -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ScreenHandler handler = screen.getScreenHandler();
                    for (Slot slot : handler.slots) {
                        ItemStack stack = slot.getStack();
                        if (!stack.isEmpty() && isGreenGlass(stack)) {
                            for (int i = 0; i < (speedMode.getValue() ? 15 : 5); i++) {
                                mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                            }
                            stage = Stage.ORDERS_FINAL_EXIT;
                            stageStart = now;
                            finalExitCount = 0;
                            finalExitStart = now;

                            if (notifications.getValue()) {
                                String nextAction = action.getValue() == Action.ORDER_ONLY ? "checking for more orders..." : "going back to shop...";
                                ChatUtils.m("Order completed! " + nextAction);
                            }
                            return;
                        }
                    }
                    if (now - stageStart > (speedMode.getValue() ? 2000 : 5000)) {
                        mc.player.closeHandledScreen();
                        if (action.getValue() == Action.ORDER_ONLY) {
                            stage = Stage.CYCLE_PAUSE;
                        } else {
                            stage = Stage.SHOP;
                        }
                        stageStart = now;
                    }
                }
            }
            case ORDERS_FINAL_EXIT -> {
                long exitDelay = speedMode.getValue() ? 50 : 200;

                if (finalExitCount == 0) {
                    if (System.currentTimeMillis() - finalExitStart >= exitDelay) {
                        mc.player.closeHandledScreen();
                        finalExitCount++;
                        finalExitStart = System.currentTimeMillis();
                    }
                } else if (finalExitCount == 1) {
                    if (System.currentTimeMillis() - finalExitStart >= exitDelay) {
                        mc.player.closeHandledScreen();
                        finalExitCount++;
                        finalExitStart = System.currentTimeMillis();
                    }
                } else {
                    finalExitCount = 0;
                    stage = Stage.CYCLE_PAUSE;
                    stageStart = System.currentTimeMillis();
                }
            }
            case CYCLE_PAUSE -> {
                long cycleWait = speedMode.getValue() ? 10 : 25;
                if (now - stageStart >= cycleWait) {
                    updateTargetPlayer();

                    if (action.getValue() == Action.ORDER_ONLY || action.getValue() == Action.SELL_ONLY) {
                        stage = Stage.WAIT;
                    } else {
                        stage = Stage.SHOP;
                    }
                    stageStart = now;
                }
            }
            case NONE -> {
            }
        }
    }

    private boolean isEndStone(ItemStack stack) {
        return stack.getItem() == Items.END_STONE || stack.getName().getString().toLowerCase(Locale.ROOT).contains("end");
    }

    private boolean isShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().getName().getString().toLowerCase(Locale.ROOT).contains("shulker box");
    }

    private boolean isShulkerShell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.SHULKER_SHELL;
    }

    private boolean isPurple(ItemStack stack) {
        return stack.getItem() == Items.SHULKER_BOX;
    }

    private boolean isGreenGlass(ItemStack stack) {
        return stack.getItem() == Items.LIME_STAINED_GLASS_PANE || stack.getItem() == Items.GREEN_STAINED_GLASS_PANE;
    }

    private boolean isGlassPane(ItemStack stack) {
        String itemName = stack.getItem().getName().getString().toLowerCase();
        return itemName.contains("glass") && itemName.contains("pane");
    }

    private boolean isInventoryFull() {
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) return false;
        }
        return true;
    }

    private String getOrderPlayerName(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        Item.TooltipContext tooltipContext = Item.TooltipContext.create(mc.world);
        List<Text> tooltip = stack.getTooltip(tooltipContext, mc.player, TooltipType.BASIC);

        for (Text line : tooltip) {
            String text = line.getString();

            Pattern[] namePatterns = {
                    Pattern.compile("(?i)player\\s*:\\s*([a-zA-Z0-9_]+)"),
                    Pattern.compile("(?i)from\\s*:\\s*([a-zA-Z0-9_]+)"),
                    Pattern.compile("(?i)by\\s*:\\s*([a-zA-Z0-9_]+)"),
                    Pattern.compile("(?i)seller\\s*:\\s*([a-zA-Z0-9_]+)"),
                    Pattern.compile("(?i)owner\\s*:\\s*([a-zA-Z0-9_]+)"),
                    Pattern.compile("\\b([a-zA-Z0-9_]{3,16})\\b")
            };

            for (Pattern pattern : namePatterns) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String playerName = matcher.group(1);
                    if (playerName.length() >= 3 && playerName.length() <= 16 &&
                            playerName.matches("[a-zA-Z0-9_]+")) {
                        return playerName;
                    }
                }
            }
        }

        return null;
    }

    private double getOrderPrice(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1.0;
        }

        Item.TooltipContext tooltipContext = Item.TooltipContext.create(mc.world);
        List<Text> tooltip = stack.getTooltip(tooltipContext, mc.player, TooltipType.BASIC);

        return parseTooltipPrice(tooltip);
    }

    private double parseTooltipPrice(List<Text> tooltip) {
        if (tooltip == null || tooltip.isEmpty()) {
            return -1.0;
        }

        Pattern[] pricePatterns = {
                Pattern.compile("\\$([\\d,]+(?:\\.[\\d]+)?)([kmb])?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)price\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)pay\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)reward\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("([\\d,]+(?:\\.[\\d]+)?)([kmb])?\\s*coins?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b([\\d,]+(?:\\.[\\d]+)?)([kmb])\\b", Pattern.CASE_INSENSITIVE)
        };

        for (Text line : tooltip) {
            String text = line.getString();

            for (Pattern pattern : pricePatterns) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String numberStr = matcher.group(1).replace(",", "");
                    String suffix = "";
                    if (matcher.groupCount() >= 2 && matcher.group(2) != null) {
                        suffix = matcher.group(2).toLowerCase();
                    }

                    try {
                        double basePrice = Double.parseDouble(numberStr);
                        double multiplier = 1.0;

                        switch (suffix) {
                            case "k" -> multiplier = 1_000.0;
                            case "m" -> multiplier = 1_000_000.0;
                            case "b" -> multiplier = 1_000_000_000.0;
                        }

                        return basePrice * multiplier;
                    } catch (NumberFormatException e) {

                    }
                }
            }
        }

        return -1.0;
    }

    private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            return -1.0;
        }

        String cleaned = priceStr.trim().toLowerCase().replace(",", "");
        double multiplier = 1.0;

        if (cleaned.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("m")) {
            multiplier = 1_000_000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("k")) {
            multiplier = 1_000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        try {
            return Double.parseDouble(cleaned) * multiplier;
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private String formatPrice(double price) {
        if (price >= 1_000_000_000) {
            return String.format("$%.1fB", price / 1_000_000_000.0);


        } else if (price >= 1_000_000) {
            return String.format("$%.1fM", price / 1_000_000.0);
        } else if (price >= 1_000) {
            return String.format("$%.1fK", price / 1_000.0);
        } else {
            return String.format("$%.0f", price);
        }
    }

    private boolean is_end_stone(ItemStack stack) {
        return stack.getItem() == Items.END_STONE || stack.getName().getString().toLowerCase(Locale.ROOT).contains("end");
    }

    private boolean is_shulker_shell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.SHULKER_SHELL;
    }

    private boolean is_shulker_box(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.SHULKER_BOX;
    }

    private boolean is_glass_pane(ItemStack stack) {
        String item_name = stack.getItem().getName().getString().toLowerCase();
        return item_name.contains("glass") && item_name.contains("pane");
    }

    private boolean is_buy_button(ItemStack stack) {
        String display_name = stack.getName().getString().toLowerCase();
        return display_name.contains("buy") && display_name.contains("one");
    }

    private boolean is_green_glass(ItemStack stack) {
        return stack.getItem() == Items.LIME_STAINED_GLASS_PANE || stack.getItem() == Items.GREEN_STAINED_GLASS_PANE;
    }

    private boolean is_inventory_full() {
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) return false;
        }
        return true;
    }

    public enum ItemMode {
        SHULKERS,
        SHULKER_SHELLS
    }

    public enum Action {
        BUY_AND_SELL,
        BUY_ONLY,
        SELL_ONLY,
        ORDER_ONLY
    }

    private enum Stage {
        NONE, SHOP, SHOP_END, SHOP_ITEM, SHOP_GLASS_PANE, SHOP_BUY, SHOP_CONFIRM,
        SHOP_CHECK_FULL, SHOP_EXIT, WAIT, ORDERS, ORDERS_SELECT, ORDERS_EXIT,
        ORDERS_CONFIRM, ORDERS_FINAL_EXIT, CYCLE_PAUSE, TARGET_ORDERS
    }
}
