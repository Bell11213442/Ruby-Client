package com.radium.client.gui.widget;
// radium client

import com.radium.client.gui.RadiumGuiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class FuturisticButtonWidget extends ButtonWidget {

    public FuturisticButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int mainColor = this.isHovered() ? RadiumGuiTheme.getAccentColorDark() : 0xFF111113;
        int borderColor = RadiumGuiTheme.getAccentColor();
        int textColor = 0xFFE0E0E0;


        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, mainColor);


        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
        context.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);


        context.drawCenteredTextWithShadow(
                client.textRenderer,
                this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                textColor
        );
    }
}
