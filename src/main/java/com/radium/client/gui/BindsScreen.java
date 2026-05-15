package com.radium.client.gui;
// radium client

import com.radium.client.client.KeybindManager;
import com.radium.client.client.RadiumClient;
import com.radium.client.gui.utils.BlurUtil;
import com.radium.client.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

public class BindsScreen extends Screen {

    private int selectedKey = -1;

    public BindsScreen() {
        super(Text.literal("Keybinds"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        BlurUtil.applyBlur(2);

        this.renderBackground(context, mouseX, mouseY, delta);
        drawKeyboard(context, mouseX, mouseY);

        if (selectedKey != -1) {
            drawUnboundModules(context, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawKeyboard(DrawContext context, int mouseX, int mouseY) {
        int keyWidth = 24;
        int keyHeight = 24;
        int keySpacing = 8;
        int startY = height / 2 - 140;

        int[] rowLengths = {14, 14, 13, 12, 7};
        int[][] keyCodes = {
                {GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_BACKSPACE},
                {GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET, GLFW.GLFW_KEY_BACKSLASH},
                {GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_ENTER},
                {GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_RIGHT_SHIFT},
                {GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_RIGHT_ALT, GLFW.GLFW_KEY_RIGHT_SUPER, GLFW.GLFW_KEY_RIGHT_CONTROL}
        };

        int keyboardWidth = rowLengths[0] * (keyWidth + keySpacing) + 10;
        int keyboardHeight = rowLengths.length * (keyHeight + keySpacing) + 10;
        int keyboardX = (width - keyboardWidth) / 2;
        int keyboardY = startY - 5;

        context.fill(keyboardX, keyboardY, keyboardX + keyboardWidth, keyboardY + keyboardHeight, 0x80000000);
        context.drawBorder(keyboardX, keyboardY, keyboardWidth, keyboardHeight, 0xFFFFFFFF);


        for (int row = 0; row < rowLengths.length; row++) {
            int rowWidth = rowLengths[row] * (keyWidth + keySpacing) - keySpacing;
            int rowStartX = (width - rowWidth) / 2;
            for (int col = 0; col < rowLengths[row]; col++) {
                int keyCode = keyCodes[row][col];
                String keyName = KeybindManager.getKeyName(keyCode);
                int x = rowStartX + col * (keyWidth + keySpacing);
                int y = startY + row * (keyHeight + keySpacing);

                boolean hovered = mouseX >= x && mouseX <= x + keyWidth && mouseY >= y && mouseY <= y + keyHeight;
                int color = hovered ? 0xFF808080 : 0xFF404040;
                if (selectedKey == keyCode) {
                    color = 0xFFFFFFFF;
                }

                context.fill(x, y, x + keyWidth, y + keyHeight, color);
                context.drawCenteredTextWithShadow(textRenderer, keyName, x + keyWidth / 2, y + (keyHeight - 8) / 2, 0xFFFFFFFF);
            }
        }
    }

    private void drawUnboundModules(DrawContext context, int mouseX, int mouseY) {
        List<Module> unboundModules = RadiumClient.moduleManager.getModules().stream()
                .filter(module -> module.getKeyBind() == -1)
                .collect(Collectors.toList());

        if (unboundModules.isEmpty()) {
            return;
        }

        int listWidth = 120;
        int listHeight = unboundModules.size() * 14 + 6;
        int listX = (width - listWidth) / 2;
        int listY = height / 2 + 60;

        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0xC0000000);

        int moduleY = listY + 4;
        for (Module module : unboundModules) {
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= moduleY && mouseY <= moduleY + 12;
            int color = hovered ? 0xFFFFFFFF : 0xFFA0A0A0;
            context.drawTextWithShadow(textRenderer, module.getName(), listX + 4, moduleY, color);
            moduleY += 14;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int keyWidth = 24;
        int keyHeight = 24;
        int keySpacing = 8;
        int startY = height / 2 - 120;

        int[] rowLengths = {14, 14, 13, 12, 7};
        int[][] keyCodes = {
                {GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_BACKSPACE},
                {GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET, GLFW.GLFW_KEY_BACKSLASH},
                {GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_ENTER},
                {GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_RIGHT_SHIFT},
                {GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_RIGHT_ALT, GLFW.GLFW_KEY_RIGHT_SUPER, GLFW.GLFW_KEY_RIGHT_CONTROL}
        };

        for (int row = 0; row < rowLengths.length; row++) {
            int rowWidth = rowLengths[row] * (keyWidth + keySpacing) - keySpacing;
            int rowStartX = (width - rowWidth) / 2;
            for (int col = 0; col < rowLengths[row]; col++) {
                int x = rowStartX + col * (keyWidth + keySpacing);
                int y = startY + row * (keyHeight + keySpacing);

                if (mouseX >= x && mouseX <= x + keyWidth && mouseY >= y && mouseY <= y + keyHeight) {
                    selectedKey = keyCodes[row][col];
                    return true;
                }
            }
        }

        if (selectedKey != -1) {
            List<Module> unboundModules = RadiumClient.moduleManager.getModules().stream()
                    .filter(module -> module.getKeyBind() == -1)
                    .collect(Collectors.toList());

            if (unboundModules.isEmpty()) {
                selectedKey = -1;
                return false;
            }

            int listWidth = 120;
            int listX = (width - listWidth) / 2;
            int listY = height / 2 + 60;
            int moduleY = listY + 4;

            for (Module module : unboundModules) {
                if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= moduleY && mouseY <= moduleY + 12) {
                    module.setKeyBind(selectedKey);
                    selectedKey = -1;
                    return true;
                }
                moduleY += 14;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
