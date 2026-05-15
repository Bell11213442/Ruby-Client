package com.radium.client.modules.combat;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.donut.TunnelBaseFinder;
import com.radium.client.modules.misc.BaseDigger;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class AutoEat extends Module implements TickListener {

    private final NumberSetting healthThreshold = new NumberSetting("Health Threshold", 10.0, 1.0, 19.0, 1.0);
    private final NumberSetting hungerThreshold = new NumberSetting("Hunger Threshold", 16.0, 1.0, 19.0, 1.0);

    private boolean eating;
    private int slot;
    private int prevSlot;

    public AutoEat() {
        super("AutoEat", "Automatically eats food when health or hunger is low.", Category.COMBAT);
        this.addSettings(this.healthThreshold, this.hungerThreshold);
    }

    @Override
    public void onEnable() {
        RadiumClient.getEventManager().add(TickListener.class, this);
        super.onEnable();
        eating = false;
        slot = -1;
        prevSlot = -1;
    }

    @Override
    public void onDisable() {
        RadiumClient.getEventManager().remove(TickListener.class, this);
        super.onDisable();
        if (eating) {
            stopEating();
        }
    }

    @Override
    public void onTick2() {
        if (mc.player == null || mc.world == null) return;

        if (RadiumClient.moduleManager.getModule(TunnelBaseFinder.class).isEnabled()) {
            return;
        }
        if (RadiumClient.moduleManager.getModule(BaseDigger.class).isEnabled() && RadiumClient.moduleManager.getModule(BaseDigger.class).autoEat.getValue() == true && RadiumClient.moduleManager.getModule(BaseDigger.class).diggingState != BaseDigger.DiggingState.NONE) {
            return;
        }

        if (eating) {
            if (!shouldEat()) {
                stopEating();
                return;
            }

            ItemStack currentStack = mc.player.getInventory().getStack(slot);
            if (currentStack.get(DataComponentTypes.FOOD) == null) {
                int newSlot = findFoodSlot();
                if (newSlot == -1) {
                    stopEating();
                    return;
                }
                changeSlot(newSlot);
            }

            eat();
        } else {
            if (shouldEat()) {
                startEating();
            }
        }
    }

    private void startEating() {
        prevSlot = mc.player.getInventory().selectedSlot;
        eat();
    }

    private void eat() {
        changeSlot(slot);
        mc.options.useKey.setPressed(true);
        if (!mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
        eating = true;
    }

    private void stopEating() {
        changeSlot(prevSlot);
        mc.options.useKey.setPressed(false);
        eating = false;
    }

    private void changeSlot(int slot) {
        mc.player.getInventory().selectedSlot = slot;
        this.slot = slot;
    }

    private boolean shouldEat() {
        if (mc.player == null) return false;

        boolean healthLow = mc.player.getHealth() <= healthThreshold.getValue().floatValue();
        boolean hungerLow = mc.player.getHungerManager().getFoodLevel() <= hungerThreshold.getValue().intValue();

        slot = findFoodSlot();
        if (slot == -1) return false;

        ItemStack foodStack = mc.player.getInventory().getStack(slot);
        FoodComponent food = foodStack.get(DataComponentTypes.FOOD);
        if (food == null) return false;

        return (healthLow || hungerLow) && (mc.player.getHungerManager().isNotFull() || food.canAlwaysEat());
    }

    private int findFoodSlot() {
        int bestSlot = -1;
        int bestHunger = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
            if (foodComponent == null) continue;

            int hunger = foodComponent.nutrition();
            if (hunger > bestHunger) {
                bestSlot = i;
                bestHunger = hunger;
            }
        }

        return bestSlot;
    }
}

