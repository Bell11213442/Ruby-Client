package com.radium.client.modules.misc;
// radium client

import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import static com.radium.client.client.RadiumClient.eventManager;

public class AutoVillagerTrade extends Module implements TickListener {

    private final NumberSetting delay = new NumberSetting("Delay", 10.0, 1.0, 50.0, 1.0);
    private final NumberSetting tradeIndex = new NumberSetting("Trade Index", 0.0, 0.0, 10.0, 1.0);
    private final BooleanSetting autoTrade = new BooleanSetting("Auto Trade", true);
    private final BooleanSetting autoClose = new BooleanSetting("Auto Close", true);
    private final BooleanSetting onlyBestTrades = new BooleanSetting("Only Best Trades", false);

    private int cooldown = 0;

    public AutoVillagerTrade() {
        super("AutoVillagerTrade", "Automatically trades with villagers", Category.MISC);
        addSettings(delay, tradeIndex, autoTrade, autoClose, onlyBestTrades);
        eventManager.add(TickListener.class, this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        cooldown = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cooldown = 0;
    }

    @Override
    public void onTick2() {
        if (mc.player == null || mc.world == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // Check if we're in a merchant screen
        if (mc.currentScreen instanceof MerchantScreen merchantScreen) {
            MerchantScreenHandler handler = merchantScreen.getScreenHandler();

            if (autoTrade.getValue()) {
                performTrade(handler);
            }

            if (autoClose.getValue() && handler.getRecipes().size() > 0) {
                // Check if we've traded all available trades
                boolean allTradesUsed = true;
                for (int i = 0; i < handler.getRecipes().size(); i++) {
                    var recipe = handler.getRecipes().get(i);
                    if (recipe.getUses() < recipe.getMaxUses()) {
                        allTradesUsed = false;
                        break;
                    }
                }

                if (allTradesUsed) {
                    mc.player.closeHandledScreen();
                    cooldown = delay.getValue().intValue();
                }
            }
        } else {
            // Try to interact with nearby villager
            if (autoTrade.getValue()) {
                interactWithVillager();
            }
        }
    }

    private void interactWithVillager() {
        if (mc.player == null || mc.interactionManager == null) return;

        // Check if we're looking at a villager
        HitResult hitResult = mc.crosshairTarget;
        if (hitResult instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof VillagerEntity villager) {
                double distance = mc.player.distanceTo(villager);
                if (distance <= 5.0) {
                    // Interact with villager
                    mc.interactionManager.interactEntity(mc.player, villager, Hand.MAIN_HAND);
                    cooldown = delay.getValue().intValue();
                }
            }
        }
    }

    private void performTrade(MerchantScreenHandler handler) {
        if (mc.interactionManager == null || mc.player == null) return;

        int targetIndex = tradeIndex.getValue().intValue();
        int recipeCount = handler.getRecipes().size();

        if (recipeCount == 0) return;

        // Select trade
        if (onlyBestTrades.getValue()) {
            // Find best trade (highest uses remaining)
            int bestIndex = 0;
            int maxUses = 0;
            for (int i = 0; i < recipeCount; i++) {
                var recipe = handler.getRecipes().get(i);
                int remainingUses = recipe.getMaxUses() - recipe.getUses();
                if (remainingUses > maxUses) {
                    maxUses = remainingUses;
                    bestIndex = i;
                }
            }
            targetIndex = bestIndex;
        } else {
            targetIndex = Math.min(targetIndex, recipeCount - 1);
        }

        // Select the trade
        handler.switchTo(targetIndex);
        cooldown = delay.getValue().intValue();

        // Check if trade is available
        var recipe = handler.getRecipes().get(targetIndex);
        if (recipe.getUses() >= recipe.getMaxUses()) {
            // Trade is locked, try next one
            if (targetIndex + 1 < recipeCount) {
                handler.switchTo(targetIndex + 1);
                cooldown = delay.getValue().intValue();
            }
            return;
        }

        // Check if we have the required items
        var firstBuyItem = recipe.getFirstBuyItem();
        ItemStack firstInput = new ItemStack(firstBuyItem.item(), firstBuyItem.count());

        ItemStack secondInput = ItemStack.EMPTY;
        if (recipe.getSecondBuyItem().isPresent()) {
            var secondBuyItem = recipe.getSecondBuyItem().get();
            secondInput = new ItemStack(secondBuyItem.item(), secondBuyItem.count());
        }

        boolean hasFirst = hasItem(firstInput);
        boolean hasSecond = secondInput.isEmpty() || hasItem(secondInput);

        if (!hasFirst || !hasSecond) {
            // Don't have required items, wait
            return;
        }

        // Perform the trade
        // Slot 0 = first input, Slot 1 = second input, Slot 2 = output
        if (!firstInput.isEmpty()) {
            // Move first input to trade slot
            int firstSlot = findItemSlot(firstInput.getItem());
            if (firstSlot != -1) {
                moveItemToSlot(handler, firstSlot, 0, firstInput.getCount());
            }
        }

        if (!secondInput.isEmpty()) {
            // Move second input to trade slot
            int secondSlot = findItemSlot(secondInput.getItem());
            if (secondSlot != -1) {
                moveItemToSlot(handler, secondSlot, 1, secondInput.getCount());
            }
        }

        // Click the trade button (slot 2)
        mc.interactionManager.clickSlot(
                handler.syncId,
                2,
                0,
                SlotActionType.PICKUP,
                mc.player
        );

        cooldown = delay.getValue().intValue();
    }

    private boolean hasItem(ItemStack stack) {
        if (mc.player == null || stack.isEmpty()) return true;

        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack invStack = mc.player.getInventory().getStack(i);
            if (invStack.getItem() == stack.getItem()) {
                count += invStack.getCount();
            }
        }
        return count >= stack.getCount();
    }

    private int findItemSlot(net.minecraft.item.Item item) {
        if (mc.player == null) return -1;

        // Check hotbar first
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i + 36; // Player inventory slots are offset
            }
        }

        // Check rest of inventory
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i + 36;
            }
        }

        return -1;
    }

    private void moveItemToSlot(MerchantScreenHandler handler, int fromSlot, int toSlot, int count) {
        if (mc.interactionManager == null || mc.player == null) return;

        // Click the item from inventory
        mc.interactionManager.clickSlot(
                handler.syncId,
                fromSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
        );

        // Click the trade slot
        mc.interactionManager.clickSlot(
                handler.syncId,
                toSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
        );
    }
}

