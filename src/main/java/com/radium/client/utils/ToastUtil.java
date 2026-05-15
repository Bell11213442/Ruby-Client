package com.radium.client.utils;
// radium client

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class ToastUtil implements Toast {
    private static final Identifier TEXTURE = Identifier.ofVanilla("toast/advancement");
    private static final int DEFAULT_DURATION_MS = 5000;
    private final ItemStack displayItem;
    private final String title;
    private final String message;
    private final boolean playSound;
    private boolean soundPlayed;


    public ToastUtil(ItemConvertible item, String title, String message, boolean playSound) {
        this.displayItem = new ItemStack(item);
        this.title = title;
        this.message = message;
        this.playSound = playSound;
    }

    public ToastUtil(String title, String message, boolean playSound) {
        this(Items.CHEST, title, message, playSound);
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        context.drawGuiTexture(TEXTURE, 0, 0, this.getWidth(), this.getHeight());

        if (playSound && !soundPlayed) {
            manager.getClient().getSoundManager().play(
                    net.minecraft.client.sound.PositionedSoundInstance.master(
                            net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f
                    )
            );
            soundPlayed = true;
        }

        context.drawText(manager.getClient().textRenderer, title, 30, 7, 16776960 | -16777216, false);
        context.drawText(manager.getClient().textRenderer, message, 30, 18, -1, false);
        context.drawItemWithoutEntity(displayItem, 8, 8);

        return (double) startTime >= (double) DEFAULT_DURATION_MS * manager.getNotificationDisplayTimeMultiplier()
                ? Visibility.HIDE
                : Visibility.SHOW;
    }
}
