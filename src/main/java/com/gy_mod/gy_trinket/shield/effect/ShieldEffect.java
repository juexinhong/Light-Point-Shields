package com.gy_mod.gy_trinket.shield.effect;

import com.gy_mod.gy_trinket.item.ShieldItemGy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;

/**
 * 护盾效果接口，定义了护盾可以实现的特殊效果
 * 每种护盾可以通过实现这个接口来提供独特的效果
 */
public interface ShieldEffect {
    /**
     * 当护盾吸收伤害时触发的效果
     * @param shield 触发效果的护盾物品
     * @param player 装备护盾的玩家
     * @param damageSource 伤害源
     * @param absorbedDamage 被吸收的伤害值
     */
    void onDamageAbsorbed(ShieldItemGy shield, Player player, DamageSource damageSource, float absorbedDamage);

    /**
     * 当护盾开始重构时触发的效果
     * @param shield 触发效果的护盾物品
     * @param player 装备护盾的玩家
     */
    void onRebuildStart(ShieldItemGy shield, Player player);

    /**
     * 当护盾重构完成时触发的效果
     * @param shield 触发效果的护盾物品
     * @param player 装备护盾的玩家
     */
    void onRebuildComplete(ShieldItemGy shield, Player player);

    /**
     * 当玩家装备护盾时触发的效果
     * @param shield 触发效果的护盾物品
     * @param player 装备护盾的玩家
     */
    void onEquip(ShieldItemGy shield, Player player);

    /**
     * 当玩家卸下护盾时触发的效果
     * @param shield 触发效果的护盾物品
     * @param player 卸下护盾的玩家
     */
    void onUnequip(ShieldItemGy shield, Player player);

    /**
     * 每tick执行的持续效果
     * @param shield 触发效果的护盾物品
     * @param player 装备护盾的玩家
     * @param currentShield 当前护盾值
     * @param maxShield 最大护盾值
     */
    void tickEffect(ShieldItemGy shield, Player player, float currentShield, float maxShield);
}
