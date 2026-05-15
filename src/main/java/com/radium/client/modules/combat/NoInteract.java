package com.radium.client.modules.combat;
// radium client

import com.radium.client.events.event.AttackListener;
import com.radium.client.events.event.BlockBreakingListener;
import com.radium.client.events.event.ItemUseListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.BlockUtil;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;

import static com.radium.client.client.RadiumClient.eventManager;

public final class NoInteract extends Module implements ItemUseListener, AttackListener, BlockBreakingListener {
    private final BooleanSetting doubleGlowstone = new BooleanSetting("Double Glowstone", false);
    private final BooleanSetting obiPunch = new BooleanSetting("Obi Punch", false);
    private final BooleanSetting echestClick = new BooleanSetting("E-chest click", false);
    private final BooleanSetting anvilClick = new BooleanSetting("Anvil click", false);

    public NoInteract() {
        super("NoInteract",
                "Prevents you from certain actions",
                Category.COMBAT);
        addSettings(doubleGlowstone, obiPunch, echestClick, anvilClick);
    }

    @Override
    public void onEnable() {
        eventManager.add(BlockBreakingListener.class, this);
        eventManager.add(AttackListener.class, this);
        eventManager.add(ItemUseListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(BlockBreakingListener.class, this);
        eventManager.remove(AttackListener.class, this);
        eventManager.remove(ItemUseListener.class, this);
        super.onDisable();
    }

    @Override
    public void onAttack(AttackEvent event) {
        if (mc.crosshairTarget instanceof BlockHitResult hit) {
            if (BlockUtil.isBlockAtPosition(hit.getBlockPos(), Blocks.OBSIDIAN) && obiPunch.getValue() && mc.player.isHolding(Items.END_CRYSTAL))
                event.cancel();
        }
    }

    @Override
    public void onBlockBreaking(BlockBreakingEvent event) {
        if (mc.crosshairTarget instanceof BlockHitResult hit) {
            if (BlockUtil.isBlockAtPosition(hit.getBlockPos(), Blocks.OBSIDIAN) && obiPunch.getValue() && mc.player.isHolding(Items.END_CRYSTAL))
                event.cancel();
        }
    }

    @Override
    public void onItemUse(ItemUseEvent event) {
        if (mc.crosshairTarget instanceof BlockHitResult hit) {
            if (BlockUtil.isAnchorCharged(hit.getBlockPos()) && doubleGlowstone.getValue() && mc.player.isHolding(Items.GLOWSTONE))
                event.cancel();

            if (BlockUtil.isBlockAtPosition(hit.getBlockPos(), Blocks.ENDER_CHEST) && echestClick.getValue())
                event.cancel();

            if ((BlockUtil.isBlockAtPosition(hit.getBlockPos(), Blocks.ANVIL) ||
                    BlockUtil.isBlockAtPosition(hit.getBlockPos(), Blocks.CHIPPED_ANVIL) ||
                    BlockUtil.isBlockAtPosition(hit.getBlockPos(), Blocks.DAMAGED_ANVIL)) && anvilClick.getValue())
                event.cancel();
        }
    }
}

