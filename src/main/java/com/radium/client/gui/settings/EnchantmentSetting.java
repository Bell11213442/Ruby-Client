package com.radium.client.gui.settings;
// radium client

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;

import java.util.*;

public class EnchantmentSetting extends Setting<List<RegistryKey<Enchantment>>> {

    private final Set<String> amethystEnchants = new HashSet<>();


    private final Map<String, Object> metadata = new HashMap<>();

    public EnchantmentSetting(String name) {
        super(name, new ArrayList<>());
    }

    public List<RegistryKey<Enchantment>> getEnchantments() {
        return getValue();
    }

    public boolean isEmpty() {
        return getValue().isEmpty() && amethystEnchants.isEmpty();
    }

    public void addEnchantment(RegistryKey<Enchantment> enchantment) {
        if (!getValue().contains(enchantment)) getValue().add(enchantment);
    }

    public void removeEnchantment(RegistryKey<Enchantment> enchantment) {
        getValue().remove(enchantment);
    }

    public void clear() {
        getValue().clear();
        amethystEnchants.clear();
    }


    public void addAmethystEnchant(String enchantName) {
        amethystEnchants.add(enchantName);
        saveAmethystMetadata();
    }

    public void removeAmethystEnchant(String enchantName) {
        amethystEnchants.remove(enchantName);
        saveAmethystMetadata();
    }

    public boolean hasAmethystEnchant(String enchantName) {
        return amethystEnchants.contains(enchantName);
    }

    public Set<String> getAmethystEnchants() {
        return new HashSet<>(amethystEnchants);
    }


    public boolean hasAmethystPickaxe() {
        return amethystEnchants.contains("Amethyst Pickaxe");
    }

    public boolean hasAmethystAxe() {
        return amethystEnchants.contains("Amethyst Axe");
    }

    public boolean hasAmethystSellAxe() {
        return amethystEnchants.contains("Amethyst Sell Axe");
    }

    public boolean hasAmethystShovel() {
        return amethystEnchants.contains("Amethyst Shovel");
    }


    public int getTotalCount() {
        return getValue().size() + amethystEnchants.size();
    }


    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }


    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return (T) metadata.get(key);
    }


    @SuppressWarnings("unchecked")
    public List<String> getMetadataList(String key) {
        Object obj = metadata.get(key);
        if (obj instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof String) result.add((String) o);
            }
            return result;
        }
        return new ArrayList<>();
    }


    public void loadAmethystFromMetadata() {
        List<String> saved = getMetadataList("selectedAmethystEnchants");
        amethystEnchants.clear();
        amethystEnchants.addAll(saved);
    }


    private void saveAmethystMetadata() {
        setMetadata("selectedAmethystEnchants", new ArrayList<>(amethystEnchants));
    }
}

