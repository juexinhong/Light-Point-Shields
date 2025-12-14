package com.gy_mod.gy_trinket.shield;

import com.gy_mod.gy_trinket.Config;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 护盾冷却管理类，负责处理护盾的冷却时间和内置冷却机制
 */
public class ShieldCooldownManager {
    // 存储玩家的护盾冷却时间开始时间：UUID -> 开始冷却的刻数
    private static final Map<UUID, Integer> SHIELD_COOLDOWN_START = new HashMap<>();
    // 存储玩家最后一次触发护盾冷却的刻数（用于防止高频触发）
    private static final Map<UUID, Integer> LAST_COOLDOWN_TRIGGER = new HashMap<>();

    /**
     * 处理护盾受到伤害时的冷却逻辑
     * @param player 玩家对象
     * @param damage 伤害值
     * @param shouldStartCooldown 是否应该开始冷却
     */
    public static void handleShieldDamage(Player player, float damage, boolean shouldStartCooldown) {
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        int maxShieldScaled = ShieldManager.getMaxShieldScaled();

        if (shouldStartCooldown) {
            // 如果正在护盾冷却中，延长冷却时间
            if (SHIELD_COOLDOWN_START.containsKey(playerId)) {
                int cooldownStartTick = SHIELD_COOLDOWN_START.get(playerId);
                int originalCooldownEnd = cooldownStartTick + Config.shieldRebuildWaitTime;
                int remainingCooldown = Math.max(0, originalCooldownEnd - currentTick);

                // 基于伤害值计算冷却延长时间：基础值 * (提升倍数 ^ 伤害值)
                int extendedCooldownTime = (int) (Config.shieldWaitTimeExtensionOnHurt * Math.pow(Config.shieldCooldownExtensionMultiplier, damage));

                // 确保延长时间至少为基础值
                extendedCooldownTime = Math.max(extendedCooldownTime, Config.shieldWaitTimeExtensionOnHurt);

                // 计算新的冷却结束时间：当前剩余冷却时间 + 新的冷却延长时间
                int newCooldownEnd = currentTick + remainingCooldown + extendedCooldownTime;
                // 不超过最大冷却时间
                newCooldownEnd = Math.min(newCooldownEnd, currentTick + Config.shieldMaxWaitTime);

                // 更新冷却开始时间（相当于延长冷却），确保不小于当前时间
                int newCooldownStart = newCooldownEnd - Config.shieldRebuildWaitTime;
                SHIELD_COOLDOWN_START.put(playerId, Math.max(newCooldownStart, currentTick));
            } else {
                // 开始新的护盾冷却
                SHIELD_COOLDOWN_START.put(playerId, currentTick);
                // 清除可能的重构状态
                ShieldRegenManager.clearRebuildState(player);
            }
        }
    }

    /**
     * 处理护盾值为0时受到伤害的冷却逻辑
     * @param player 玩家对象
     * @param damage 伤害值
     */
    public static void handleShieldZeroDamage(Player player, float damage) {
        // 实现不变
    }
    
    /**
     * 处理伤害事件中的冷却逻辑（用于LivingDamageEvent）
     * @param player 玩家对象
     * @param event 伤害事件
     */
    public static void handleDamageCooldown(Player player, LivingDamageEvent event) {
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        int currentShield = ShieldManager.getPlayerShieldScaled(player);
        
        float damage = event.getAmount();
        
        // 如果护盾值为0，处理零护盾时的冷却逻辑
        if (currentShield <= 0) {
            handleShieldZeroDamage(player, damage);
        }
        // 如果护盾值大于0，处理护盾受伤害时的冷却逻辑
        else {
            // 确定是否应该开始冷却
            boolean shouldStartCooldown = true;
            
            // 检查是否正在护盾冷却中，延长冷却时间
            handleShieldDamage(player, damage, shouldStartCooldown);
        }
    }

    /**
     * 检查玩家是否正在冷却中
     * @param player 玩家对象
     * @return 是否正在冷却中
     */
    public static boolean isInCooldown(Player player) {
        UUID playerId = player.getUUID();
        if (!SHIELD_COOLDOWN_START.containsKey(playerId)) {
            return false;
        }

        int cooldownStartTick = SHIELD_COOLDOWN_START.get(playerId);
        int currentTick = player.tickCount;
        return currentTick - cooldownStartTick < Config.shieldRebuildWaitTime;
    }

    /**
     * 获取冷却开始时间
     * @param player 玩家对象
     * @return 冷却开始时间刻数，未冷却时返回-1
     */
    public static int getCooldownStartTick(Player player) {
        UUID playerId = player.getUUID();
        return SHIELD_COOLDOWN_START.getOrDefault(playerId, -1);
    }

    /**
     * 开始新的冷却
     * @param player 玩家对象
     */
    public static void startCooldown(Player player) {
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        SHIELD_COOLDOWN_START.put(playerId, currentTick);
        ShieldRegenManager.clearRebuildState(player);
    }

    /**
     * 结束冷却
     * @param player 玩家对象
     */
    public static void endCooldown(Player player) {
        UUID playerId = player.getUUID();
        SHIELD_COOLDOWN_START.remove(playerId);
    }

    /**
     * 清理玩家的冷却数据
     * @param player 玩家对象
     */
    public static void clearPlayerCooldownData(Player player) {
        UUID playerId = player.getUUID();
        SHIELD_COOLDOWN_START.remove(playerId);
        LAST_COOLDOWN_TRIGGER.remove(playerId);
    }
}
