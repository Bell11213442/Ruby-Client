package com.radium.client.utils;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.Module;
import com.radium.client.modules.client.ClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class ParticleUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static ClickGUI getClickGUI() {
        if (RadiumClient.getModuleManager() == null) return null;
        return RadiumClient.getModuleManager().getModule(ClickGUI.class);
    }

    private static int getParticleCount() {
        return 12;
    }

    private static double getParticleSpread() {
        return 0.3;
    }

    /**
     * Spawns success particles (green stars) when a module is enabled
     */
    public static void spawnSuccessParticles(Module module) {
        if (mc.player == null || mc.world == null) return;

        Vec3d pos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        double spread = getParticleSpread();
        int count = getParticleCount();

        // Spawn green particles (using HAPPY_VILLAGER for star-like effect)
        spawnParticleBurst(ParticleTypes.HAPPY_VILLAGER, pos, spread, spread, spread, count);

        // Also spawn some green dust particles for more visual effect
        spawnColoredDustBurst(pos, 0.0f, 1.0f, 0.0f, (int) (count * 0.7)); // Green color
    }

    /**
     * Spawns disable particles (red dust) when a module is disabled
     */
    public static void spawnDisableParticles(Module module) {
        if (mc.player == null || mc.world == null) return;

        Vec3d pos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        double spread = getParticleSpread();
        int count = getParticleCount();

        // Spawn red dust particles
        spawnColoredDustBurst(pos, 1.0f, 0.0f, 0.0f, count); // Red color

        // Also spawn some smoke particles for disable effect
        spawnParticleBurst(ParticleTypes.SMOKE, pos, spread * 0.7, spread * 0.7, spread * 0.7, (int) (count * 0.6));
    }

    /**
     * Spawns category-specific particles based on module category
     */
    public static void spawnCategoryParticles(Module module, boolean enabled) {
        if (enabled) {
            spawnCategorySuccessParticles(module);
        } else {
            spawnCategoryDisableParticles(module);
        }
    }

    private static void spawnCategorySuccessParticles(Module module) {
        if (mc.player == null || mc.world == null) return;

        Vec3d pos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        ParticleEffect particleType;
        Vector3f color;
        double spread = getParticleSpread();
        int count = getParticleCount();

        switch (module.getCategory()) {
            case COMBAT:
                // Red particles for combat modules
                particleType = ParticleTypes.CRIT;
                color = new Vector3f(1.0f, 0.2f, 0.2f); // Red
                spawnParticleBurst(particleType, pos, spread, spread, spread, count);
                spawnColoredDustBurst(pos, color.x, color.y, color.z, (int) (count * 0.7));
                break;

            case VISUAL:
                // Cyan/Blue particles for visual modules
                particleType = ParticleTypes.ENCHANT;
                color = new Vector3f(0.2f, 0.8f, 1.0f); // Cyan
                spawnParticleBurst(particleType, pos, spread, spread, spread, count);
                spawnColoredDustBurst(pos, color.x, color.y, color.z, (int) (count * 0.7));
                break;

            case MISC:
                // Yellow/Orange particles for misc modules
                particleType = ParticleTypes.HAPPY_VILLAGER;
                color = new Vector3f(1.0f, 0.8f, 0.2f); // Yellow
                spawnParticleBurst(particleType, pos, spread, spread, spread, count);
                spawnColoredDustBurst(pos, color.x, color.y, color.z, (int) (count * 0.7));
                break;

            case DONUT:
                // Purple particles for donut modules
                particleType = ParticleTypes.WITCH;
                color = new Vector3f(0.8f, 0.2f, 1.0f); // Purple
                spawnParticleBurst(particleType, pos, spread, spread, spread, count);
                spawnColoredDustBurst(pos, color.x, color.y, color.z, (int) (count * 0.7));
                break;

            case CLIENT:
                // White/Gold particles for client modules
                particleType = ParticleTypes.TOTEM_OF_UNDYING;
                color = new Vector3f(1.0f, 0.9f, 0.6f); // Gold
                spawnParticleBurst(particleType, pos, spread, spread, spread, count);
                spawnColoredDustBurst(pos, color.x, color.y, color.z, (int) (count * 0.8));
                break;

            default:
                // Default green particles
                spawnSuccessParticles(module);
                break;
        }
    }

    private static void spawnCategoryDisableParticles(Module module) {
        if (mc.player == null || mc.world == null) return;

        Vec3d pos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        Vector3f color;
        double spread = getParticleSpread();
        int count = getParticleCount();

        switch (module.getCategory()) {
            case COMBAT:
                color = new Vector3f(0.7f, 0.1f, 0.1f); // Dark red
                break;
            case VISUAL:
                color = new Vector3f(0.1f, 0.5f, 0.7f); // Dark cyan
                break;
            case MISC:
                color = new Vector3f(0.7f, 0.5f, 0.1f); // Dark yellow
                break;
            case DONUT:
                color = new Vector3f(0.5f, 0.1f, 0.7f); // Dark purple
                break;
            case CLIENT:
                color = new Vector3f(0.6f, 0.5f, 0.4f); // Dark gold
                break;
            default:
                color = new Vector3f(1.0f, 0.0f, 0.0f); // Red
                break;
        }

        spawnColoredDustBurst(pos, color.x, color.y, color.z, count);
        spawnParticleBurst(ParticleTypes.SMOKE, pos, spread * 0.7, spread * 0.7, spread * 0.7, (int) (count * 0.5));
    }

    /**
     * Spawns a burst of particles at the given position
     */
    private static void spawnParticleBurst(ParticleEffect particle, Vec3d pos, double spreadX, double spreadY, double spreadZ, int count) {
        ClientWorld world = mc.world;
        if (world == null) return;

        for (int i = 0; i < count; i++) {
            double offsetX = (Math.random() - 0.5) * spreadX * 2;
            double offsetY = (Math.random() - 0.5) * spreadY * 2;
            double offsetZ = (Math.random() - 0.5) * spreadZ * 2;

            double velocityX = (Math.random() - 0.5) * 0.1;
            double velocityY = (Math.random() - 0.5) * 0.1;
            double velocityZ = (Math.random() - 0.5) * 0.1;

            world.addParticle(particle,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    velocityX,
                    velocityY,
                    velocityZ);
        }
    }

    /**
     * Spawns colored dust particles (custom colored particles)
     */
    private static void spawnColoredDustBurst(Vec3d pos, float r, float g, float b, int count) {
        ClientWorld world = mc.world;
        if (world == null) return;

        Vector3f color = new Vector3f(r, g, b);
        DustParticleEffect dustEffect = new DustParticleEffect(color, 1.0f);

        for (int i = 0; i < count; i++) {
            double offsetX = (Math.random() - 0.5) * 0.6;
            double offsetY = (Math.random() - 0.5) * 0.6;
            double offsetZ = (Math.random() - 0.5) * 0.6;

            double velocityX = (Math.random() - 0.5) * 0.15;
            double velocityY = (Math.random() - 0.5) * 0.15;
            double velocityZ = (Math.random() - 0.5) * 0.15;

            world.addParticle(dustEffect,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    velocityX,
                    velocityY,
                    velocityZ);
        }
    }
}
