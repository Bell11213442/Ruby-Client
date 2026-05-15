package com.radium.client.modules.misc;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;

import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;

public class Compass extends Module {
    private final NumberSetting x = new NumberSetting("X", 10, 0, 2000, 1);
    private final NumberSetting y = new NumberSetting("Y", 10, 0, 2000, 1);
    private final NumberSetting size = new NumberSetting("Size", 120, 40, 300, 1);
    private final NumberSetting range = new NumberSetting("Range", 100, 10, 200, 1);
    private final ColorSetting backgroundColor = new ColorSetting("Background", new Color(15, 20, 35, 220));
    private final ColorSetting gridColor = new ColorSetting("Grid Color", new Color(100, 100, 255, 100));
    private final ColorSetting playerColor = new ColorSetting("Player Color", new Color(255, 50, 50));
    private final ColorSetting textColor = new ColorSetting("Text Color", new Color(200, 200, 255));
    private final BooleanSetting editHUD = new BooleanSetting("Edit Compass HUD", false);

    public Compass() {
        super("Compass", "Shows directions and players", Category.MISC);
        addSettings(x, y, size, range, backgroundColor, gridColor, playerColor, textColor, editHUD);
    }

    public void render(DrawContext context) {
        if (!enabled || mc.player == null || mc.world == null) return;

        doRender(context, x.getValue().intValue(), y.getValue().intValue());
    }

    public void renderAt(DrawContext context, int px, int py) {
        if (!enabled || mc.player == null || mc.world == null) return;
        doRender(context, px, py);
    }

    private void doRender(DrawContext context, int posX, int posY) {
        // Fonts.loadFonts(); removed
        int d = size.getValue().intValue();
        int r = d / 2;
        int cx = posX + r;
        int cy = posY + r;

        RenderUtils.fillRadialGradient(context, cx, cy, r, backgroundColor.getValue().getRGB(), backgroundColor.getValue().getRGB());

        float yaw = mc.player.getYaw();
        double centerAngle = Math.toRadians(yaw + 180);

        int gridC = gridColor.getValue().getRGB();

        RenderUtils.drawCircle(context, cx, cy, r, gridC);

        RenderUtils.drawCircle(context, cx, cy, (int) (r * 0.25), gridC);
        RenderUtils.drawCircle(context, cx, cy, (int) (r * 0.5), gridC);
        RenderUtils.drawCircle(context, cx, cy, (int) (r * 0.75), gridC);

        for (int i = 0; i < 4; i++) {
            double angle = centerAngle + (i * Math.PI / 2);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double x1 = cx + cos * (r * 0.2);
            double y1 = cy + sin * (r * 0.2);
            double x2 = cx + cos * (r - 4);
            double y2 = cy + sin * (r - 4);

            RenderUtils.drawLine(context, (int) x1, (int) y1, (int) x2, (int) y2, gridC);
        }

        for (int a = 0; a < 360; a += 30) {
            double angle = Math.toRadians(a) + centerAngle;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double x1 = cx + cos * (r * 0.3);
            double y1 = cy + sin * (r * 0.3);
            double x2 = cx + cos * (r - 6);
            double y2 = cy + sin * (r - 6);
            RenderUtils.drawLine(context, (int) x1, (int) y1, (int) x2, (int) y2, gridC);
        }

        String[] dirs = {"N", "W", "S", "E"};
        int textC = textColor.getValue().getRGB();

        for (int i = 0; i < 4; i++) {
            double angle = centerAngle + (i * Math.PI / 2) - (Math.PI / 2);
            double dist = r - 15;

            double lx = cx + Math.cos(angle) * dist;
            double ly = cy + Math.sin(angle) * dist;

            String label = dirs[i];
            float w = RadiumClient.mc.textRenderer.getWidth(label);
            float h = RadiumClient.mc.textRenderer.fontHeight;

            context.drawText(RadiumClient.mc.textRenderer, label, (int)(lx - w / 2), (int)(ly - h / 2 + 1), textC, false);
        }

        double rangeVal = range.getValue();
        int pColor = playerColor.getValue().getRGB();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            double dist = mc.player.distanceTo(player);
            if (dist > rangeVal) continue;

            double dx = player.getX() - mc.player.getX();
            double dz = player.getZ() - mc.player.getZ();

            double angleToTarget = Math.atan2(dz, dx);
            double relativeAngle = angleToTarget - Math.toRadians(yaw + 90);

            double renderDist = (dist / rangeVal) * (r - 10);

            double rx = cx + Math.cos(relativeAngle) * renderDist;
            double ry = cy + Math.sin(relativeAngle) * renderDist;

            context.fill((int) rx - 1, (int) ry - 1, (int) rx + 1, (int) ry + 1, pColor);
        }
    }

    @Override
    public void onTick() {
        if (editHUD.getValue()) {
            editHUD.setValue(false);
            if (mc != null) {
                mc.execute(() -> mc.setScreen(new com.radium.client.gui.CompassGuiEditor(this)));
            }
        }
    }

    public NumberSetting getX() {
        return x;
    }

    public NumberSetting getY() {
        return y;
    }

    public NumberSetting getSize() {
        return size;
    }
}

