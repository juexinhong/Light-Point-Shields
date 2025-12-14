package com.gy_mod.gy_trinket.shield;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gy_mod.gy_trinket.Config;
import com.gy_mod.gy_trinket.item.ShieldItemGy;
import com.gy_mod.gy_trinket.shield.effect.ShieldEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 护盾伤害处理类，负责处理护盾的伤害吸收和无敌时间管理
 */
public class ShieldDamageHandler {
    // 日志记录器
    private static final Logger LOGGER = LogManager.getLogger(ShieldDamageHandler.class);
    // 存储玩家的无敌时间结束刻数：UUID -> 无敌时间结束的刻数
    private static final Map<UUID, Integer> INVULNERABILITY_END = new HashMap<>();

    /**
     * 处理玩家受到的伤害，使用护盾吸收
     * @param player 玩家对象
     * @param damageSource 伤害源
     * @param damage 伤害值
     * @return 是否取消了原始攻击事件
     */
    public static boolean handleDamage(Player player, DamageSource damageSource, float damage) {
        return handleShieldAbsorption(player, damageSource, damage);
    }
    
    /**
     * 处理玩家受到的伤害，使用护盾吸收（用于LivingAttackEvent）
     * @param player 玩家对象
     * @param event 攻击事件
     */
    public static void handleShieldAbsorption(Player player, LivingAttackEvent event) {
        handleDamage(player, event.getSource(), event.getAmount());
    }
    
    /**
     * 处理玩家受到的伤害，使用护盾吸收（内部实现）
     * @param player 玩家对象
     * @param damageSource 伤害源
     * @param damage 伤害值
     * @return 是否取消了原始攻击事件
     */
    private static boolean handleShieldAbsorption(Player player, DamageSource damageSource, float damage) {
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        int currentShield = ShieldManager.getPlayerShieldScaled(player);
        int maxShieldScaled = ShieldManager.getMaxShieldScaled(player);

        // 检查是否在无敌时间内
        if (isInvulnerable(player)) {
            return true; // 取消攻击事件
        }

        // 检查是否正在重构
        boolean isRebuilding = ShieldRegenManager.isRebuilding(player);

        // 如果有剩余护盾值且不在重构状态，取消攻击事件以防止受击特效
        if (currentShield > 0 && !isRebuilding) {
            // 将伤害放大100倍来匹配护盾值的存储方式
            int scaledDamage = Math.round(damage * 100);
            // 计算可以吸收的伤害（不超过当前护盾值）
            int absorbed = Math.min(scaledDamage, currentShield);
            // 计算剩余伤害（放大后）
            int remainingScaledDamage = scaledDamage - absorbed;
            // 剩余伤害缩小回原始比例
            float remainingDamage = remainingScaledDamage / 100.0f;
            // 实际吸收的伤害值（用于效果计算）
            float absorbedDamage = absorbed / 100.0f;

            // 更新护盾值
            int newShield = currentShield - absorbed;
            ShieldManager.setPlayerShieldScaled(player, newShield);
            // 发送护盾值更新到客户端
            ShieldNetworkManager.sendShieldUpdate(player);

            // 检查护盾是否破裂（护盾值变为0）
            if (newShield <= 0) {
                // 生成护盾破裂粒子效果
                com.gy_mod.gy_trinket.shield.particle.ShieldParticleGenerator.generateShieldBreakParticles(player);
            }

            // 如果有剩余伤害，手动应用到玩家身上
            if (remainingDamage > 0) {
                player.hurt(damageSource, remainingDamage);
            }

            // 设置短暂无敌时间
            setInvulnerability(player, currentTick);

            // 调用护盾特殊效果（如果有）
            ShieldItemGy activeShield = ShieldManager.getActiveShieldItem(player);
            if (activeShield != null) {
                ShieldEffect effect = activeShield.getShieldEffect();
                if (effect != null) {
                    effect.onDamageAbsorbed(activeShield, player, damageSource, absorbedDamage);
                }
            }

            // 检查当前护盾值是否未满或为零
            int updatedShield = ShieldManager.getPlayerShieldScaled(player);
            if (updatedShield < maxShieldScaled) {
                // 处理冷却逻辑
                ShieldCooldownManager.handleShieldDamage(player, damage, updatedShield < maxShieldScaled);
            }

            return true; // 取消原始攻击事件
        } else if (currentShield == 0 && !isRebuilding) {
            // 护盾值为0且不在重构状态，允许伤害直接应用
            // 处理冷却逻辑
            ShieldCooldownManager.handleShieldZeroDamage(player, damage);
            return false; // 不取消原始攻击事件
        }

        return false;
    }

    /**
     * 设置玩家无敌时间
     * @param player 玩家对象
     * @param currentTick 当前刻数
     */
    public static void setInvulnerability(Player player, int currentTick) {
        UUID playerId = player.getUUID();
        int invulnerabilityDuration = ShieldManager.getShieldInvulnerabilityDuration(player);
        INVULNERABILITY_END.put(playerId, currentTick + invulnerabilityDuration);
    }

    /**
     * 检查玩家是否处于无敌时间内
     * @param player 玩家对象
     * @return 是否处于无敌时间内
     */
    public static boolean isInvulnerable(Player player) {
        UUID playerId = player.getUUID();
        if (!INVULNERABILITY_END.containsKey(playerId)) {
            return false;
        }
        int invulnerabilityEnd = INVULNERABILITY_END.get(playerId);
        int currentTick = player.tickCount;
        if (currentTick <= invulnerabilityEnd) {
            return true;
        } else {
            // 无敌时间结束，移除无敌状态
            INVULNERABILITY_END.remove(playerId);
            return false;
        }
    }

    /**
     * 清理玩家的无敌时间数据
     * @param player 玩家对象
     */
    public static void clearPlayerInvulnerabilityData(Player player) {
        UUID playerId = player.getUUID();
        INVULNERABILITY_END.remove(playerId);
    }

    /**
     * 更新玩家的无敌时间状态
     * @param player 玩家对象
     */
    public static void updateInvulnerability(Player player) {
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;

        // 检查玩家的无敌时间是否已结束
        if (INVULNERABILITY_END.containsKey(playerId)) {
            int endTick = INVULNERABILITY_END.get(playerId);
            if (currentTick > endTick) {
                // 无敌时间结束，清理数据
                INVULNERABILITY_END.remove(playerId);
            }
        }
    }
}
