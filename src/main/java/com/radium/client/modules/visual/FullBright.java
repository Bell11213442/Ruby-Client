package com.radium.client.modules.visual;
// radium client

import com.radium.client.modules.Module;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class FullBright extends Module {

    public FullBright() {
        super("FullBright", "Makes everything bright", Category.VISUAL);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 999999, 0, false, false, false));
        } else {
            StatusEffectInstance currentEffect = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (currentEffect != null && currentEffect.getDuration() < 200) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 999999, 0, false, false, false));
            }
        }
    }
}
