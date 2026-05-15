package com.radium.client.client;
// radium client

import com.radium.client.modules.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModuleManager {
    private final List<Module> modules = new CopyOnWriteArrayList<>();

    public ModuleManager() {
        // Removed auth functionality
    }

    public void register(Module module) {
        if (module == null) {
            return;
        }
        modules.add(module);
    }

    public void tick() {
        for (Module module : modules) {
            try {
                if (module != null && module.isEnabled()) {
                    module.onTick();
                }
            } catch (Exception e) {

                System.err.println("Error in module " + (module != null ? module.getName() : "null") + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public List<Module> getModules() {
        return new ArrayList<>(modules);
    }

    public List<Module> getModulesByCategory(Module.Category category) {
        return modules.stream()
                .filter(module -> module.getCategory() == category)
                .toList();
    }

    public Module getModule(String name) {
        return modules.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        return modules.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }
}
