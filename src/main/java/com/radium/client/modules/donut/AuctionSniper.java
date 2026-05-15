package com.radium.client.modules.donut;
// radium client

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.radium.client.gui.settings.*;
import com.radium.client.modules.Module;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuctionSniper extends Module {
    private final ItemSetting snipingItem = new ItemSetting("Sniping Item", Items.AIR);
    private final StringSetting price = new StringSetting("Price", "1k");
    private final ModeSetting<Mode> mode = new ModeSetting<>("Mode", Mode.MANUAL, Mode.class).setDescription("Manual is faster but api doesnt require auction gui opened all the time");
    private final StringSetting apiKey = new StringSetting("Api Key", "");
    private final NumberSetting refreshDelay = new NumberSetting("Refresh Delay", 2.0, 0.0, 100.0, 1.0);
    private final NumberSetting buyDelay = new NumberSetting("Buy Delay", 2.0, 0.0, 100.0, 1.0);
    private final NumberSetting apiRefreshRate = new NumberSetting("API Refresh Rate", 250.0, 10.0, 5000.0, 10.0);
    private final BooleanSetting showApiNotifications = new BooleanSetting("Show API Notifications", true);

    private final EnchantmentSetting requiredEnchantments = new EnchantmentSetting("Required Enchantments");
    private final EnchantmentSetting forbiddenEnchantments = new EnchantmentSetting("Forbidden Enchantments");
    private final NumberSetting minEnchantmentLevel = new NumberSetting("Min Enchantment Level", 1.0, 1.0, 10.0, 1.0);
    private final BooleanSetting exactEnchantmentMatch = new BooleanSetting("Exact Enchantment Match", false);
    private final StringSetting minSelfDestructTime = new StringSetting("Min Self Destruct", "0");
    private final HttpClient httpClient;
    private final Gson gson;
    private final Map<String, Double> snipingItems = new HashMap<String, Double>();
    private int delayCounter;
    private boolean isProcessing;
    private long lastApiCallTimestamp = 0L;
    private boolean isApiQueryInProgress = false;
    private boolean isAuctionSniping = false;
    private int auctionPageCounter = -1;
    private String currentSellerName = "";

    public AuctionSniper() {
        super("AuctionSniper", "Snipes items on auction house for cheap", Module.Category.DONUT);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
        this.gson = new Gson();
        Setting<?>[] settingArray = new Setting<?>[]{this.snipingItem, this.price, this.mode, this.apiKey, this.refreshDelay, this.buyDelay, this.apiRefreshRate, this.showApiNotifications, this.requiredEnchantments, this.forbiddenEnchantments, this.minEnchantmentLevel, this.exactEnchantmentMatch, this.minSelfDestructTime};
        this.addSettings(settingArray);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        double d = this.parsePrice(this.price.getValue());
        if (d == -1.0) {
            if (mc.player != null) {
                ClientPlayerEntity clientPlayerEntity = mc.player;
                clientPlayerEntity.sendMessage(Text.of("Invalid Price"), true);
            }
            this.toggle();
            return;
        }
        if (this.snipingItem.getItem() != Items.AIR) {
            Map<String, Double> map = this.snipingItems;
            map.put(this.snipingItem.getItem().toString(), d);
        }
        this.lastApiCallTimestamp = 0L;
        this.isApiQueryInProgress = false;
        this.isAuctionSniping = false;
        this.currentSellerName = "";
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.isAuctionSniping = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        if (this.delayCounter > 0) {
            --this.delayCounter;
            return;
        }
        if (this.mode.isMode(Mode.API)) {
            this.handleApiMode();
            return;
        }
        if (!this.mode.isMode(Mode.MANUAL)) {
            return;
        }
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
            String searchCommand = this.constructSearchCommand();
            mc.getNetworkHandler().sendChatCommand(searchCommand);
            this.delayCounter = 20;
            return;
        }
        if (((GenericContainerScreenHandler) screenHandler).getRows() == 6) {
            this.processSixRowAuction((GenericContainerScreenHandler) screenHandler);
        } else if (((GenericContainerScreenHandler) screenHandler).getRows() == 3) {
            this.processThreeRowAuction((GenericContainerScreenHandler) screenHandler);
        }
    }

    private String constructSearchCommand() {
        if (!this.requiredEnchantments.getAmethystEnchants().isEmpty()) {
            String amethystEnchant = this.requiredEnchantments.getAmethystEnchants().iterator().next();
            return "ah " + amethystEnchant;
        }

        String[] stringArray = this.snipingItem.getItem().getTranslationKey().split("\\.");
        String string2 = stringArray[stringArray.length - 1];
        String formattedItemName = Arrays.stream(string2.replace("_", " ").split(" "))
                .map(string -> string.substring(0, 1).toUpperCase() + string.substring(1))
                .collect(Collectors.joining(" "));

        if (!this.requiredEnchantments.isEmpty()) {
            List<String> enchantNames = new ArrayList<>();
            for (RegistryKey<Enchantment> enchKey : this.requiredEnchantments.getEnchantments()) {
                String enchIdStr = enchKey.getValue().toString();
                if (enchIdStr.contains(":")) {
                    enchIdStr = enchIdStr.substring(enchIdStr.lastIndexOf(':') + 1);
                } else {
                    enchIdStr = enchIdStr.substring(enchIdStr.lastIndexOf('/') + 1);
                }
                String formattedEnchant = Arrays.stream(enchIdStr.replace("_", " ").split(" "))
                        .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                        .collect(Collectors.joining(" "));
                enchantNames.add(formattedEnchant);
            }

            String enchantString = String.join(" ", enchantNames);
            return "ah " + formattedItemName + " " + enchantString;
        }

        return "ah " + formattedItemName;
    }

    private void handleApiMode() {
        if (!this.isAuctionSniping) {
            if (this.auctionPageCounter != -1) {
                if (this.auctionPageCounter <= 40) {
                    ++this.auctionPageCounter;
                } else {
                    this.isAuctionSniping = false;
                    this.currentSellerName = "";
                }
            } else {
                mc.getNetworkHandler().sendChatCommand("ah " + this.currentSellerName);
                this.auctionPageCounter = 0;
            }
        } else {
            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
                if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && mc.currentScreen.getTitle().getString().contains("Page")) {
                    mc.player.closeHandledScreen();
                    this.delayCounter = 20;
                    return;
                }
                if (this.isApiQueryInProgress) {
                    return;
                }
                long l = System.currentTimeMillis();
                long l2 = l - this.lastApiCallTimestamp;
                if (l2 > (long) this.apiRefreshRate.getValue().intValue()) {
                    this.lastApiCallTimestamp = l;
                    if (this.apiKey.getValue().isEmpty()) {
                        if (this.showApiNotifications.getValue()) {
                            ClientPlayerEntity clientPlayerEntity = mc.player;
                            clientPlayerEntity.sendMessage(Text.of("§cAPI key is not set. Set it using /api in-game."), false);
                        }
                        return;
                    }
                    this.isApiQueryInProgress = true;
                    this.queryApi().thenAccept(this::processApiResponse);
                }
                return;
            }
            this.auctionPageCounter = -1;
            if (((GenericContainerScreenHandler) screenHandler).getRows() == 6) {
                this.processSixRowAuction((GenericContainerScreenHandler) screenHandler);
            } else if (((GenericContainerScreenHandler) screenHandler).getRows() == 3) {
                this.processThreeRowAuction((GenericContainerScreenHandler) screenHandler);
            }
        }
    }

    private CompletableFuture<List<JsonObject>> queryApi() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String string = "https://api.donutsmp.net/v1/auctions/search";
                HttpResponse<String> httpResponse = this.httpClient.send(HttpRequest.newBuilder().uri(URI.create(string)).header("Authorization", "Bearer " + this.apiKey.getValue()).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"sort\": \"recently_listed\"}")).build(), HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    if (this.showApiNotifications.getValue() && mc.player != null) {
                        ClientPlayerEntity clientPlayerEntity = mc.player;
                        clientPlayerEntity.sendMessage(Text.of("§cAPI Error: " + httpResponse.statusCode()), false);
                    }
                    ArrayList<JsonObject> arrayList = new ArrayList<>();
                    this.isApiQueryInProgress = false;
                    return arrayList;
                }
                Gson gson = this.gson;
                JsonArray jsonArray = gson.fromJson(httpResponse.body(), JsonObject.class).getAsJsonArray("result");
                ArrayList<JsonObject> arrayList = new ArrayList<>();
                for (JsonElement jsonElement : jsonArray) {
                    arrayList.add(jsonElement.getAsJsonObject());
                }
                this.isApiQueryInProgress = false;
                return arrayList;
            } catch (Throwable _t) {
                return List.of();
            }
        });
    }

    private void processApiResponse(List<JsonObject> list) {
        for (JsonObject e : list) {
            try {
                double d = 0.0;
                String string = "";
                String string2 = e.getAsJsonObject("item").get("id").getAsString();
                long l = e.get("price").getAsLong();
                String string3 = e.getAsJsonObject("seller").get("name").getAsString();
                Iterator<Map.Entry<String, Double>> iterator = this.snipingItems.entrySet().iterator();
                do {
                    if (!iterator.hasNext()) continue;
                    Map.Entry<String, Double> entry = iterator.next();
                    string = entry.getKey();
                    d = entry.getValue();
                } while (!string2.contains(string) || !((double) l <= d));
                if (this.showApiNotifications.getValue() && mc.player != null) {
                    ClientPlayerEntity clientPlayerEntity = mc.player;
                    clientPlayerEntity.sendMessage(Text.of("§aFound " + string2 + " for " + this.formatPrice(l) + " §r(threshold: " + this.formatPrice(d) + ") §afrom seller: " + string3), false);
                }
                this.isAuctionSniping = true;
                this.currentSellerName = string3;
                return;
            } catch (Exception exception) {
                if (!this.showApiNotifications.getValue() || mc.player == null) continue;
                ClientPlayerEntity clientPlayerEntity = mc.player;
                clientPlayerEntity.sendMessage(Text.of("§cError processing auction: " + exception.getMessage()), false);
            }
        }
    }

    private void processSixRowAuction(GenericContainerScreenHandler genericContainerScreenHandler) {
        ItemStack itemStack = genericContainerScreenHandler.getSlot(47).getStack();
        if (itemStack.isOf(Items.AIR)) {
            this.delayCounter = 2;
            return;
        }
        for (Object e : itemStack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC)) {
            String string = e.toString();
            if (!string.contains("Recently Listed") || !((Text) e).getStyle().toString().contains("white") && !string.contains("white"))
                continue;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 47, 1, SlotActionType.QUICK_MOVE, mc.player);
            this.delayCounter = 5;
            return;
        }
        for (int i = 0; i < 44; ++i) {
            ItemStack itemStack2 = genericContainerScreenHandler.getSlot(i).getStack();

            if (!this.isValidAuctionItem(itemStack2)) continue;

            if (this.isProcessing) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 1, SlotActionType.QUICK_MOVE, mc.player);
                this.isProcessing = false;
                return;
            }
            this.isProcessing = true;
            this.delayCounter = this.buyDelay.getValue().intValue();
            return;
        }
        if (this.isAuctionSniping) {
            this.isAuctionSniping = false;
            this.currentSellerName = "";
            mc.player.closeHandledScreen();
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 49, 1, SlotActionType.QUICK_MOVE, mc.player);
            this.delayCounter = this.refreshDelay.getValue().intValue();
        }
    }

    private void processThreeRowAuction(GenericContainerScreenHandler genericContainerScreenHandler) {
        ItemStack confirmationItem = genericContainerScreenHandler.getSlot(13).getStack();

        if (!confirmationItem.isOf(Items.AIR) && this.isValidAuctionItem(confirmationItem)) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 15, 0, SlotActionType.PICKUP, mc.player);
            this.delayCounter = 20;
        }

        if (this.isAuctionSniping) {
            this.isAuctionSniping = false;
            this.currentSellerName = "";
        }
    }

    private double parseTooltipPrice(List<Text> list) {
        String string = "";
        String string2 = "";
        if (list == null || list.isEmpty()) {
            return -1.0;
        }
        Iterator<Text> iterator = list.iterator();
        while (iterator.hasNext()) {
            String string3 = iterator.next().getString();
            if (!string3.matches("(?i).*price\\s*:\\s*\\$.*")) continue;
            String string4 = string3.replaceAll("[,$]", "");
            Matcher matcher = Pattern.compile("([\\d]+(?:\\.[\\d]+)?)\\s*([KMB])?", 2).matcher(string4);
            if (!matcher.find()) continue;
            string2 = matcher.group(1);
            string = matcher.group(2) != null ? matcher.group(2).toUpperCase() : "";
            break;
        }
        return this.parsePrice(string2 + string);
    }

    private boolean isValidAuctionItem(ItemStack itemStack) {
        List<Text> list = itemStack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);


        if (!this.requiredEnchantments.getAmethystEnchants().isEmpty()) {
            boolean isValid = this.isValidAmethystItem(itemStack, list);
            return isValid;
        }

        if (!this.requiredEnchantments.isEmpty()) {
            if (!itemStack.isOf(this.snipingItem.getItem())) {
                return false;
            }
        } else {
            if (!itemStack.isOf(this.snipingItem.getItem())) {
                return false;
            }
        }

        double d = this.parseTooltipPrice(list) / (double) itemStack.getCount();
        double d2 = this.parsePrice(this.price.getValue());

        if (d2 == -1.0) {
            if (mc.player != null) {
                ClientPlayerEntity clientPlayerEntity = mc.player;
                clientPlayerEntity.sendMessage(Text.of("Invalid Price"), true);
            }
            this.toggle();
            return false;
        }
        if (d == -1.0) {
            if (mc.player != null) {
                ClientPlayerEntity clientPlayerEntity = mc.player;
                clientPlayerEntity.sendMessage(Text.of("Invalid Auction Item Price"), true);
            }
            this.toggle();
            return false;
        }

        boolean priceValid = d <= d2;
        if (!priceValid) {
            return false;
        }

        return this.checkEnchantmentFilter(itemStack);
    }

    private boolean isValidAmethystItem(ItemStack itemStack, List<Text> list) {
        Item item = itemStack.getItem();
        boolean isNetheritePickaxe = item == Items.NETHERITE_PICKAXE;
        boolean isNetheriteAxe = item == Items.NETHERITE_AXE;
        boolean isNetheriteShovel = item == Items.NETHERITE_SHOVEL;


        String itemName = itemStack.getName().getString();

        if (this.requiredEnchantments.hasAmethystPickaxe()) {
            if (!isNetheritePickaxe || !itemName.contains("ᴀᴍᴇᴛʜʏsᴛ")) {
                return false;
            }
        } else if (this.requiredEnchantments.hasAmethystAxe()) {
            if (!isNetheriteAxe || !itemName.contains("ᴀᴍᴇᴛʜʏsᴛ")) {
                return false;
            }
        } else if (this.requiredEnchantments.hasAmethystSellAxe()) {
            if (!isNetheriteAxe || !itemName.contains("ᴀᴍᴇᴛʜʏsᴛ")) {
                return false;
            }
        } else if (this.requiredEnchantments.hasAmethystShovel()) {
            if (!isNetheriteShovel || !itemName.contains("ᴀᴍᴇᴛʜʏsᴛ")) {
                return false;
            }
        } else {
            String amethystEnchant = this.requiredEnchantments.getAmethystEnchants().iterator().next();
            if (!itemName.contains(amethystEnchant)) {
                return false;
            }

            if (amethystEnchant.contains("Pickaxe") && !isNetheritePickaxe) {
                return false;
            }
            if (amethystEnchant.contains("Axe") && !isNetheriteAxe) {
                return false;
            }
            if (amethystEnchant.contains("Shovel") && !isNetheriteShovel) {
                return false;
            }
        }

        double minTime = this.parsePrice(this.minSelfDestructTime.getValue());
        if (minTime > 0.0) {
            double selfDestructMins = this.parseSelfDestructTime(list);
            if (selfDestructMins < minTime) {
                return false;
            }
        }

        double d = this.parseTooltipPrice(list) / (double) itemStack.getCount();
        double d2 = this.parsePrice(this.price.getValue());

        boolean isValid = d != -1.0 && d2 != -1.0 && d <= d2;
        return isValid;
    }

    private double parseSelfDestructTime(List<Text> list) {
        Pattern pattern = Pattern.compile("Self Destruct:");
        Pattern timePattern = Pattern.compile("(\\d+)d\\s*(\\d+)h\\s*(\\d+)m");

        boolean foundSelfDestruct = false;
        for (Text text : list) {
            String line = text.getString();
            if (pattern.matcher(line).find()) {
                foundSelfDestruct = true;
                continue;
            }
            if (foundSelfDestruct) {
                Matcher timeMatcher = timePattern.matcher(line);
                if (timeMatcher.find()) {
                    int days = Integer.parseInt(timeMatcher.group(1));
                    int hours = Integer.parseInt(timeMatcher.group(2));
                    int mins = Integer.parseInt(timeMatcher.group(3));
                    return (days * 24 * 60) + (hours * 60) + mins;
                }
                break;
            }
        }
        return 0.0;
    }

    private boolean checkEnchantmentFilter(ItemStack itemStack) {
        try {
            if (this.requiredEnchantments.getEnchantments().isEmpty() && this.forbiddenEnchantments.isEmpty()) {
                return true;
            }

            Map<RegistryKey<Enchantment>, Integer> itemEnchantments = new HashMap<>();

            itemStack.getEnchantments().getEnchantments().forEach(enchantment -> {
                enchantment.getKey().ifPresent(key -> {
                    itemEnchantments.put(key, itemStack.getEnchantments().getLevel(enchantment));
                });
            });

            if (!itemEnchantments.isEmpty()) {
                for (Map.Entry<RegistryKey<Enchantment>, Integer> entry : itemEnchantments.entrySet()) {
                }
            } else {
            }

            for (RegistryKey<Enchantment> forbidden : this.forbiddenEnchantments.getEnchantments()) {
                if (itemEnchantments.containsKey(forbidden)) {
                    return false;
                }
            }

            if (!this.requiredEnchantments.getEnchantments().isEmpty()) {
                if (this.exactEnchantmentMatch.getValue()) {
                    if (itemEnchantments.size() != this.requiredEnchantments.getEnchantments().size()) {
                        return false;
                    }
                    for (RegistryKey<Enchantment> required : this.requiredEnchantments.getEnchantments()) {
                        if (!itemEnchantments.containsKey(required)) {
                            return false;
                        }
                        int level = itemEnchantments.get(required);
                        if (level < this.minEnchantmentLevel.getValue().intValue()) {
                            return false;
                        }
                    }
                } else {
                    boolean hasRequired = false;
                    for (RegistryKey<Enchantment> required : this.requiredEnchantments.getEnchantments()) {
                        if (itemEnchantments.containsKey(required)) {
                            int level = itemEnchantments.get(required);
                            if (level >= this.minEnchantmentLevel.getValue().intValue()) {
                                hasRequired = true;
                                break;
                            }
                        }
                    }
                    return hasRequired;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private double parsePrice(String string) {
        if (string == null) return -1.0;
        if (string.isEmpty()) {
            return -1.0;
        }
        String string2 = string.trim().toUpperCase();
        double d = 1.0;
        if (string2.endsWith("B")) {
            d = 1.0E9;
            string2 = string2.substring(0, string2.length() - 1);
        } else if (string2.endsWith("M")) {
            d = 1000000.0;
            string2 = string2.substring(0, string2.length() - 1);
        } else if (string2.endsWith("K")) {
            d = 1000.0;
            string2 = string2.substring(0, string2.length() - 1);
        }
        try {
            return Double.parseDouble(string2) * d;
        } catch (NumberFormatException numberFormatException) {
            return -1.0;
        }
    }

    private String formatPrice(double d) {
        if (d >= 1.0E9) {
            Object[] objectArray = new Object[]{d / 1.0E9};
            return String.format("%.2fB", objectArray);
        }
        if (d >= 1000000.0) {
            Object[] objectArray = new Object[]{d / 1000000.0};
            return String.format("%.2fM", objectArray);
        }
        if (d >= 1000.0) {
            Object[] objectArray = new Object[]{d / 1000.0};
            return String.format("%.2fK", objectArray);
        }
        Object[] objectArray = new Object[]{d};
        return String.format("%.2f", objectArray);
    }

    public enum Mode {
        API("API", 0),
        MANUAL("MANUAL", 1);

        Mode(final String name, final int ordinal) {
        }
    }
}
