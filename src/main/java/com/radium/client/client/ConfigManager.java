package com.radium.client.client;

import com.google.gson.*;
import com.radium.client.gui.settings.*;
import com.radium.client.modules.Module;
import com.radium.client.modules.client.ClickGUI;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class ConfigManager {
    private JsonObject profile;
    private File configFile;

    public ConfigManager() {
        if (RadiumClient.mc != null) {
            File radiumDir = new File(RadiumClient.mc.runDirectory, "radium");
            if (!radiumDir.exists()) {
                radiumDir.mkdirs();
            }
            configFile = new File(radiumDir, "config.json");
        }
    }

    private File getConfigFile() {
        if (configFile == null && RadiumClient.mc != null) {
            File radiumDir = new File(RadiumClient.mc.runDirectory, "radium");
            if (!radiumDir.exists()) {
                radiumDir.mkdirs();
            }
            configFile = new File(radiumDir, "config.json");
        }
        return configFile;
    }

    public void loadProfile() {
        File file = getConfigFile();
        if (file == null || !file.exists()) {
            profile = new JsonObject();
            return;
        }

        try {
            JsonObject loaded = loadJsonFromFile(file.toPath());
            if (loaded != null) {
                profile = loaded;
                applyProfileFromJson(loaded);

                // Show toast notification for config load
                if (RadiumClient.getModuleManager() != null) {
                    com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
                    if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                        com.radium.client.utils.ToastNotificationManager.getInstance().show(
                                "Config Loaded",
                                "Profile loaded successfully",
                                com.radium.client.utils.ToastNotification.ToastType.CONFIG_LOAD
                        );
                    }
                }
            } else {
                profile = new JsonObject();
            }
        } catch (Exception e) {
            profile = new JsonObject();
        }
    }

    private void applyProfileFromJson(JsonObject profileJson) {
        if (profileJson == null) {
            profile = new JsonObject();
            return;
        }

        profile = profileJson;

        boolean hadModules = false;
        boolean hadKeybinds = false;
        boolean hadSettings = false;

        if (profile.has("modules") && profile.get("modules").isJsonObject()) {
            JsonObject modulesJson = profile.getAsJsonObject("modules");
            for (Module module : RadiumClient.moduleManager.getModules()) {
                if (modulesJson.has(module.getName())) {
                    boolean enabled = modulesJson.get(module.getName()).getAsBoolean();
                    if (enabled != module.isEnabled()) module.toggle();
                    hadModules = true;
                }
            }
        }

        if (profile.has("keybinds") && profile.get("keybinds").isJsonObject()) {
            JsonObject keybindsJson = profile.getAsJsonObject("keybinds");
            for (Module module : RadiumClient.moduleManager.getModules()) {
                int keyCode = module.getKeyBind();
                if (keybindsJson.has(module.getName())) {
                    try {
                        JsonElement el = keybindsJson.get(module.getName());
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber())
                            keyCode = el.getAsInt();
                    } catch (Exception ignored) {
                    }
                }
                if ("Radium".equalsIgnoreCase(module.getName()) && keyCode <= 0)
                    keyCode = 344;
                module.setKeyBind(keyCode);
                hadKeybinds = true;
            }
        }

        if (profile.has("settings") && profile.get("settings").isJsonObject()) {
            JsonObject settingsJson = profile.getAsJsonObject("settings");
            loadSettingsFromJson(settingsJson);
            hadSettings = true;
        }

        if (profile.has("guiPreference") && profile.get("guiPreference").isJsonPrimitive()) {
            String guiPreference = profile.get("guiPreference").getAsString();
            saveGuiPreference(guiPreference);
        }

        if (!hadModules || !hadKeybinds || !hadSettings) {
            for (Module module : RadiumClient.moduleManager.getModules()) {
                if (module.isEnabled()) module.toggle();
                module.setKeyBind(-1);
            }

            Module discordPresence = RadiumClient.moduleManager.getModule("Discord RPC");
            if (discordPresence != null && !discordPresence.isEnabled()) {
                discordPresence.toggle();
            }
        }
    }

    public void saveProfile() {
        try {
            profile = buildConfigJson();
            File file = getConfigFile();
            if (file == null) return;


            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(profile, writer);

                // Show toast notification for config save (after successful save)
                if (RadiumClient.getModuleManager() != null) {
                    com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.ClickGUI.class);
                    if (clickGUI != null && clickGUI.toastNotifications.getValue()) {
                        com.radium.client.utils.ToastNotificationManager.getInstance().show(
                                "Config Saved",
                                "Profile saved successfully",
                                com.radium.client.utils.ToastNotification.ToastType.CONFIG_SAVE
                        );
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JsonObject buildConfigJson() {
        if (profile == null) {
            profile = new JsonObject();
        }

        JsonObject modulesJson = new JsonObject();
        for (Module module : RadiumClient.moduleManager.getModules()) {
            modulesJson.addProperty(module.getName(), module.isEnabled());
        }
        profile.add("modules", modulesJson);

        JsonObject keybindsJson = new JsonObject();
        for (Module module : RadiumClient.moduleManager.getModules()) {
            if (module.getKeyBind() <= 0) {
                if (module == RadiumClient.moduleManager.getModule(ClickGUI.class)) {
                    keybindsJson.addProperty(module.getName(), 344);
                } else {
                    keybindsJson.addProperty(module.getName(), "None");
                }
            } else {
                keybindsJson.addProperty(module.getName(), module.getKeyBind());
            }
        }
        profile.add("keybinds", keybindsJson);

        JsonObject settingsJson = getSettingsAsJson();
        profile.add("settings", settingsJson);

        if (profile.has("guiPreference") && profile.get("guiPreference").isJsonPrimitive()) {

        } else if (profile.has("guiPreference")) {

            profile.remove("guiPreference");
        }

        return profile;
    }

    public void saveKeybinds() {
        saveProfile();
    }

    public void saveSettings() {
        saveProfile();
    }

    public boolean hasGuiPreference() {
        return profile != null && profile.has("guiPreference") && profile.get("guiPreference").isJsonPrimitive();
    }

    public void applyProfile(String cfg) {
        if (cfg.contains("NOT FOUND")) {
            profile = new JsonObject();
        } else {
            profile = JsonParser.parseString(cfg).getAsJsonObject();
        }

        if (profile != null) {
            boolean hadModules = false;
            boolean hadKeybinds = false;
            boolean hadSettings = false;

            if (profile.has("modules") && profile.get("modules").isJsonObject()) {
                JsonObject modulesJson = profile.getAsJsonObject("modules");
                for (Module module : RadiumClient.moduleManager.getModules()) {
                    if (modulesJson.has(module.getName())) {
                        boolean enabled = modulesJson.get(module.getName()).getAsBoolean();
                        if (enabled != module.isEnabled()) module.toggle();
                        hadModules = true;
                    }
                }
            }

            if (profile.has("keybinds") && profile.get("keybinds").isJsonObject()) {
                JsonObject keybindsJson = profile.getAsJsonObject("keybinds");
                for (Module module : RadiumClient.moduleManager.getModules()) {
                    int keyCode = module.getKeyBind();
                    if (keybindsJson.has(module.getName())) {
                        try {
                            JsonElement el = keybindsJson.get(module.getName());
                            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber())
                                keyCode = el.getAsInt();
                        } catch (Exception ignored) {
                        }
                    }
                    if ("Radium".equalsIgnoreCase(module.getName()) && keyCode <= 0)
                        keyCode = 344;
                    module.setKeyBind(keyCode);
                    hadKeybinds = true;
                }
            }

            if (profile.has("settings") && profile.get("settings").isJsonObject()) {
                JsonObject settingsJson = profile.getAsJsonObject("settings");
                loadSettingsFromJson(settingsJson);
                hadSettings = true;
            }

            if (profile.has("guiPreference") && profile.get("guiPreference").isJsonPrimitive()) {
                String guiPreference = profile.get("guiPreference").getAsString();
                saveGuiPreference(guiPreference);
            }

            if (!hadModules || !hadKeybinds || !hadSettings) {
                for (Module module : RadiumClient.moduleManager.getModules()) {
                    if (module.isEnabled()) module.toggle();
                    module.setKeyBind(-1);
                }

                Module discordPresence = RadiumClient.moduleManager.getModule("Discord RPC");
                if (discordPresence != null && !discordPresence.isEnabled()) {
                    discordPresence.toggle();
                }
            }
        }
    }

    public JsonObject getSettingsAsJson() {
        JsonObject settingsJson = new JsonObject();

        for (Module module : RadiumClient.moduleManager.getModules()) {
            JsonObject moduleSettings = new JsonObject();

            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof BooleanSetting boolSetting) {
                    moduleSettings.addProperty(setting.getName(), boolSetting.getValue());
                } else if (setting instanceof NumberSetting numSetting) {
                    moduleSettings.addProperty(setting.getName(), numSetting.getValue());
                } else if (setting instanceof DoubleSetting doubleSetting) {
                    moduleSettings.addProperty(setting.getName(), doubleSetting.getValue());
                } else if (setting instanceof StringSetting strSetting) {
                    moduleSettings.addProperty(setting.getName(), strSetting.getValue());
                } else if (setting instanceof SliderSetting sliderSetting) {
                    moduleSettings.addProperty(setting.getName(), sliderSetting.getValue());
                } else if (setting instanceof ModeSetting modeSetting) {
                    moduleSettings.addProperty(setting.getName(), modeSetting.getValue().toString());
                } else if (setting instanceof EnchantmentSetting enchSetting) {

                    JsonObject enchObj = new JsonObject();


                    JsonArray vanillaArr = new JsonArray();
                    for (RegistryKey<Enchantment> enchKey : enchSetting.getEnchantments()) {
                        try {
                            vanillaArr.add(enchKey.getValue().toString());
                        } catch (Exception ignored) {
                        }
                    }
                    enchObj.add("vanilla", vanillaArr);


                    JsonArray amethystArr = new JsonArray();
                    for (String name : enchSetting.getAmethystEnchants()) {
                        amethystArr.add(name);
                    }
                    enchObj.add("amethyst", amethystArr);


                    JsonObject metadataObj = new JsonObject();
                    JsonArray selectedArr = new JsonArray();
                    for (String name : enchSetting.getAmethystEnchants()) {
                        selectedArr.add(name);
                    }
                    metadataObj.add("selectedAmethystEnchants", selectedArr);
                    enchObj.add("metadata", metadataObj);

                    moduleSettings.add(setting.getName(), enchObj);
                } else if (setting instanceof KeybindSetting keybindSetting) {
                    if (!setting.getName().equalsIgnoreCase("Keybind")) {
                        moduleSettings.addProperty(setting.getName(), keybindSetting.getValue());
                    }
                } else if (setting instanceof ColorSetting colorSetting) {
                    Color c = colorSetting.getValue();
                    String hex = String.format("#%02x%02x%02x%02x",
                            c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
                    moduleSettings.addProperty(setting.getName(), hex);
                } else if (setting instanceof StringListSetting stringListSetting) {
                    JsonArray stringArray = new JsonArray();
                    for (String str : stringListSetting.getValue()) {
                        stringArray.add(str);
                    }
                    moduleSettings.add(setting.getName(), stringArray);
                } else if (setting instanceof ItemSetting itemSetting) {
                    Item item = itemSetting.getValue();
                    if (item != null) {
                        Identifier itemId = Registries.ITEM.getId(item);
                        moduleSettings.addProperty(setting.getName(), itemId.toString());
                    }
                }
            }

            if (!moduleSettings.isEmpty()) settingsJson.add(module.getName(), moduleSettings);
        }

        return settingsJson;
    }

    public void loadSettingsFromJson(JsonObject settingsJson) {
        for (Module module : RadiumClient.moduleManager.getModules()) {
            if (!settingsJson.has(module.getName())) continue;
            JsonObject moduleSettings = settingsJson.getAsJsonObject(module.getName());

            for (Setting<?> setting : module.getSettings()) {
                if (!moduleSettings.has(setting.getName())) continue;

                try {
                    if (setting instanceof BooleanSetting boolSetting) {
                        boolSetting.setValue(moduleSettings.get(setting.getName()).getAsBoolean());
                    } else if (setting instanceof NumberSetting numSetting) {
                        numSetting.setValue(moduleSettings.get(setting.getName()).getAsDouble());
                    } else if (setting instanceof DoubleSetting doubleSetting) {
                        doubleSetting.setValue(moduleSettings.get(setting.getName()).getAsDouble());
                    } else if (setting instanceof StringSetting strSetting) {
                        strSetting.setValue(moduleSettings.get(setting.getName()).getAsString());
                    } else if (setting instanceof SliderSetting sliderSetting) {
                        sliderSetting.setValue(moduleSettings.get(setting.getName()).getAsDouble());
                    } else if (setting instanceof ModeSetting modeSetting) {
                        modeSetting.setValue(moduleSettings.get(setting.getName()).getAsString());
                    } else if (setting instanceof EnchantmentSetting enchSetting) {

                        JsonElement el = moduleSettings.get(setting.getName());
                        if (!el.isJsonObject()) {

                            try {
                                JsonArray oldArray = moduleSettings.getAsJsonArray(setting.getName());
                                enchSetting.clear();
                                for (JsonElement e : oldArray) {
                                    String idStr = e.getAsString();
                                    try {
                                        Identifier id = Identifier.of(idStr);
                                        RegistryKey<Enchantment> key = RegistryKey.of(RegistryKeys.ENCHANTMENT, id);
                                        enchSetting.addEnchantment(key);
                                    } catch (Exception ignored) {
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            JsonObject enchObj = el.getAsJsonObject();
                            enchSetting.clear();


                            if (enchObj.has("vanilla") && enchObj.get("vanilla").isJsonArray()) {
                                JsonArray vanillaArr = enchObj.getAsJsonArray("vanilla");
                                for (JsonElement e : vanillaArr) {
                                    try {
                                        String idStr = e.getAsString();
                                        Identifier id = Identifier.of(idStr);
                                        RegistryKey<Enchantment> key = RegistryKey.of(RegistryKeys.ENCHANTMENT, id);
                                        enchSetting.addEnchantment(key);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }


                            List<String> amethystListForMetadata = new ArrayList<>();
                            if (enchObj.has("amethyst") && enchObj.get("amethyst").isJsonArray()) {
                                JsonArray amethystArr = enchObj.getAsJsonArray("amethyst");
                                for (JsonElement e : amethystArr) {
                                    try {
                                        String name = e.getAsString();
                                        if (name != null && !name.isEmpty()) {
                                            enchSetting.addAmethystEnchant(name);
                                            amethystListForMetadata.add(name);
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                            }


                            if (enchObj.has("metadata") && enchObj.get("metadata").isJsonObject()) {
                                JsonObject metadataObj = enchObj.getAsJsonObject("metadata");
                                for (String key : metadataObj.keySet()) {
                                    JsonElement mEl = metadataObj.get(key);
                                    if (mEl.isJsonArray()) {
                                        List<String> list = new ArrayList<>();
                                        for (JsonElement arrEl : mEl.getAsJsonArray()) {
                                            try {
                                                list.add(arrEl.getAsString());
                                            } catch (Exception ignored) {
                                            }
                                        }

                                        enchSetting.setMetadata(key, list);


                                        if ("selectedAmethystEnchants".equals(key)) {
                                            for (String s : list) {
                                                if (!enchSetting.hasAmethystEnchant(s))
                                                    enchSetting.addAmethystEnchant(s);
                                            }
                                        }
                                    } else if (mEl.isJsonPrimitive()) {
                                        if (mEl.getAsJsonPrimitive().isBoolean()) {
                                            enchSetting.setMetadata(key, mEl.getAsBoolean());
                                        } else if (mEl.getAsJsonPrimitive().isNumber()) {
                                            enchSetting.setMetadata(key, mEl.getAsNumber());
                                        } else {
                                            enchSetting.setMetadata(key, mEl.getAsString());
                                        }
                                    }
                                }
                            } else {

                                if (!amethystListForMetadata.isEmpty()) {
                                    enchSetting.setMetadata("selectedAmethystEnchants", amethystListForMetadata);
                                }
                            }


                            try {
                                enchSetting.loadAmethystFromMetadata();
                            } catch (Exception ignored) {
                            }
                        }
                    } else if (setting instanceof KeybindSetting keybindSetting) {
                        if (!setting.getName().equalsIgnoreCase("Keybind")) {
                            keybindSetting.setValue(moduleSettings.get(setting.getName()).getAsInt());
                        }
                    } else if (setting instanceof ColorSetting colorSetting) {
                        String hex = moduleSettings.get(setting.getName()).getAsString();
                        try {

                            Color c = Color.decode(hex.substring(0, Math.min(hex.length(), 7)));
                            if (hex.length() >= 9) {
                                int alpha = Integer.parseInt(hex.substring(7, 9), 16);
                                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                            }
                            colorSetting.setValue(c);
                        } catch (Exception e) {
                        }
                    } else if (setting instanceof StringListSetting stringListSetting) {
                        JsonArray stringArray = moduleSettings.getAsJsonArray(setting.getName());
                        stringListSetting.clear();
                        for (int i = 0; i < stringArray.size(); i++) {
                            String str = stringArray.get(i).getAsString();
                            if (str != null && !str.trim().isEmpty()) {
                                stringListSetting.add(str);
                            }
                        }
                    } else if (setting instanceof ItemSetting itemSetting) {
                        String itemIdStr = moduleSettings.get(setting.getName()).getAsString();
                        try {
                            Identifier itemId = Identifier.of(itemIdStr);
                            Item item = Registries.ITEM.get(itemId);
                            if (item != null) {
                                itemSetting.setValue(item);
                            }
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    private JsonObject loadJsonFromFile(Path path) {
        if (!Files.exists(path)) return null;
        try (Reader reader = new FileReader(path.toFile())) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }


    public void saveGuiPreference(String guiType) {
        if (profile == null) {
            profile = new JsonObject();
        }
        profile.addProperty("guiPreference", guiType);
    }

    public String loadGuiPreference() {
        if (profile != null && profile.has("guiPreference") && profile.get("guiPreference").isJsonPrimitive()) {
            return profile.get("guiPreference").getAsString();
        }
        return "DEFAULT";
    }

    public void saveHudPositions() {
        try {
            File radiumDir = new File(RadiumClient.mc.runDirectory, "radium");
            if (!radiumDir.exists()) {
                radiumDir.mkdirs();
            }
            File posFile = new File(radiumDir, "pos.json");

            JsonObject posJson = new JsonObject();

            Module hudModule = RadiumClient.moduleManager.getModule("HUD");
            if (hudModule != null) {
                for (Setting<?> setting : hudModule.getSettings()) {
                    if (setting instanceof NumberSetting numSetting) {
                        String name = setting.getName();
                        if (name.endsWith(" X") || name.endsWith(" Y")) {
                            posJson.addProperty(name, numSetting.getValue());
                        }
                    }
                    if (setting instanceof BooleanSetting boolSetting) {
                        String name = setting.getName();
                        if (name.endsWith(" Centered")) {
                            posJson.addProperty(name, boolSetting.getValue());
                        }
                    }
                }
            }

            Module mediaPlayer = RadiumClient.moduleManager.getModule("MediaPlayer");
            if (mediaPlayer != null) {
                for (Setting<?> setting : mediaPlayer.getSettings()) {
                    if (setting instanceof NumberSetting numSetting) {
                        String name = setting.getName();
                        if (name.equals("X") || name.equals("Y") || name.equals("Position X") || name.equals("Position Y")) {
                            posJson.addProperty("MediaPlayer " + name, numSetting.getValue());
                        }
                    }
                }
            }

            try (FileWriter writer = new FileWriter(posFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(posJson, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadHudPositions() {
        try {
            File radiumDir = new File(RadiumClient.mc.runDirectory, "radium");
            File posFile = new File(radiumDir, "pos.json");

            if (!posFile.exists()) {
                return;
            }

            JsonObject posJson = loadJsonFromFile(posFile.toPath());
            if (posJson == null) {
                return;
            }

            Module hudModule = RadiumClient.moduleManager.getModule("HUD");
            if (hudModule != null) {
                for (Setting<?> setting : hudModule.getSettings()) {
                    String name = setting.getName();
                    if (posJson.has(name)) {
                        if (setting instanceof NumberSetting numSetting) {
                            numSetting.setValue(posJson.get(name).getAsDouble());
                        }
                        if (setting instanceof BooleanSetting boolSetting) {
                            boolSetting.setValue(posJson.get(name).getAsBoolean());
                        }
                    }
                }
            }

            Module mediaPlayer = RadiumClient.moduleManager.getModule("MediaPlayer");
            if (mediaPlayer != null) {
                for (Setting<?> setting : mediaPlayer.getSettings()) {
                    String name = setting.getName();
                    String posName = "MediaPlayer " + name;
                    if (posJson.has(posName) && setting instanceof NumberSetting numSetting) {
                        numSetting.setValue(posJson.get(posName).getAsDouble());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


