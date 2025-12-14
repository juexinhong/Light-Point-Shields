package com.gy_mod.gy_trinket.item;

import com.gy_mod.gy_trinket.Config;
import com.gy_mod.gy_trinket.shield.effect.ShieldEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 基础护盾实现
 * 提供最基础的护盾功能，无特殊效果
 */
public class BasicShield extends ShieldItemGy {
    public BasicShield() {
        super(
            new Properties(),
            new ShieldEffect() {
                @Override
                public void onDamageAbsorbed(ShieldItemGy shield, Player player, DamageSource damageSource, float absorbedDamage) {}

                @Override
                public void onRebuildStart(ShieldItemGy shield, Player player) {}

                @Override
                public void onRebuildComplete(ShieldItemGy shield, Player player) {}

                @Override
                public void onEquip(ShieldItemGy shield, Player player) {}

                @Override
                public void onUnequip(ShieldItemGy shield, Player player) {}

                @Override
                public void tickEffect(ShieldItemGy shield, Player player, float currentShield, float maxShield) {}
            }
        );
    }

    @Override
    protected void addSpecialDescription(List<Component> components) {
        components.add(Component.translatable("tooltip.gy_trinket.shield.gy.special"));
    }
}
