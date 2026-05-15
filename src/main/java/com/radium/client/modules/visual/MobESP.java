package com.radium.client.modules.visual;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.MobEspRenderer;
import com.radium.client.utils.ChatUtils;
import com.radium.client.utils.ToastUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MobESP extends Module {
    public final NumberSetting range = new NumberSetting("Range", 50.0, 10.0, 200.0, 10.0);
    public final BooleanSetting fill = new BooleanSetting("Fill", true);
    public final BooleanSetting outline = new BooleanSetting("Outline", true);
    public final BooleanSetting tracers = new BooleanSetting("Tracers", false);
    public final ColorSetting fillColor = new ColorSetting("Fill Color", new Color(255, 0, 0, 102));
    public final ColorSetting outlineColor = new ColorSetting("Outline Color", new Color(255, 0, 0, 255));
    public final BooleanSetting notifyToast = new BooleanSetting("Toast Notification", true);
    public final BooleanSetting notifyChat = new BooleanSetting("Chat Notification", true);

    private final Set<Integer> knownMobIds = new HashSet<>();
    private long lastScanTime = 0;

    public MobESP() {
        super("MobESP", "Highlights all mobs and notifies you", Category.VISUAL);
        addSettings(range, fill, outline, tracers, fillColor, outlineColor, notifyToast, notifyChat);
        MobEspRenderer.register();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (System.currentTimeMillis() - lastScanTime < 2000) return;
        lastScanTime = System.currentTimeMillis();

        Box searchBox = new Box(mc.player.getBlockPos()).expand(range.getValue());
        List<LivingEntity> mobs = mc.world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                e -> !(e instanceof PlayerEntity)
        );

        for (LivingEntity mob : mobs) {
            int id = mob.getId();
            if (knownMobIds.add(id)) {
                String name = mob.getName().getString();

                if (notifyChat.getValue()) {
                    ChatUtils.m("Detected: " + name);
                }

                if (notifyToast.getValue()) {
                    mc.getToastManager().add(
                            new ToastUtil(Items.ZOMBIE_HEAD, "Mob ESP", "Found: " + name, false)
                    );
                }
            }
        }


        knownMobIds.removeIf(id -> mc.world.getEntityById(id) == null);
    }

    @Override
    public void onDisable() {
        knownMobIds.clear();
    }

    public int getFillColor() {
        return fillColor.getValue().getRGB();
    }

    public int getOutlineColor() {
        return outlineColor.getValue().getRGB();
    }
}

