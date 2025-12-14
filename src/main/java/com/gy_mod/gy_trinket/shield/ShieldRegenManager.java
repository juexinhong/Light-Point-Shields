package com.gy_mod.gy_trinket.shield;

import com.gy_mod.gy_trinket.Config;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 护盾恢复管理类，负责护盾的重构和自然恢复逻辑
 */
public class ShieldRegenManager {
    // 存储玩家的护盾重构开始时间：UUID -> 开始重构的刻数
    private static final Map<UUID, Integer> SHIELD_REBUILD_START = new HashMap<>();
    // 存储玩家的护盾重构前的初始值：UUID -> 重构开始时的护盾值（放大100倍）
    private static final Map<UUID, Integer> SHIELD_REBUILD_INITIAL = new HashMap<>();

    /**
     * 处理玩家的护盾恢复逻辑（在玩家tick时调用）
     * @param player 玩家对象
     */
    public static void handleShieldRegen(Player player) {
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        int currentShield = ShieldManager.getPlayerShieldScaled(player);
        int maxShieldScaled = ShieldManager.getMaxShieldScaled(player);

        // 检查护盾是否已满
        if (currentShield >= maxShieldScaled) {
            clearRebuildState(player);
            ShieldCooldownManager.endCooldown(player);
            return;
        }

        // 处理自然恢复效果
        handleNaturalRegen(player);

        // 处理重构逻辑
        handleRebuild(player, currentTick, currentShield, maxShieldScaled);
    }

    /**
     * 处理自然恢复效果
     * @param player 玩家对象
     */
    private static void handleNaturalRegen(Player player) {
        int currentTick = player.tickCount;
        int currentShield = ShieldManager.getPlayerShieldScaled(player);
        int maxShieldScaled = ShieldManager.getMaxShieldScaled(player);

        // 只要护盾值不为零且未满，按照配置的间隔和百分比恢复
        int regenInterval = ShieldManager.getShieldNormalRegenInterval(player);
        double regenPercentage = ShieldManager.getShieldNormalRegenPercentage(player);
        
        if (currentTick % regenInterval == 0 && currentShield > 0 && currentShield < maxShieldScaled) {
            // 计算恢复量：当前最大护盾值 × 配置的恢复百分比（放大100倍存储）
            double regenAmountDouble = maxShieldScaled * (regenPercentage / 100.0);
            int regenAmount = (int) regenAmountDouble;

            // 如果恢复量大于0但小于1，则至少恢复1点护盾值
            if (regenAmountDouble > 0 && regenAmount == 0) {
                regenAmount = 1;
            }

            int newShield = Math.min(currentShield + regenAmount, maxShieldScaled);

            // 更新护盾值
            ShieldManager.setPlayerShieldScaled(player, newShield);
            // 发送更新到客户端
            ShieldNetworkManager.sendShieldUpdate(player);
        }
    }

    /**
     * 处理重构逻辑
     * @param player 玩家对象
     * @param currentTick 当前刻数
     * @param currentShield 当前护盾值（放大后）
     * @param maxShieldScaled 最大护盾值（放大后）
     */
    private static void handleRebuild(Player player, int currentTick, int currentShield, int maxShieldScaled) {
        UUID playerId = player.getUUID();

        // 1. 检查是否正在进行护盾重构
        if (SHIELD_REBUILD_START.containsKey(playerId)) {
            int rebuildStartTick = SHIELD_REBUILD_START.get(playerId);
            // 获取当前激活护盾的重构持续时间
            int rebuildDuration = ShieldManager.getShieldRebuildDuration(player);

            if (currentTick - rebuildStartTick <= rebuildDuration) {
                // 正在重构中
                // 计算固定恢复速率：最大护盾值 / 重构持续时间（刻）
                int regenRate = maxShieldScaled / rebuildDuration;

                // 计算当前应该恢复的护盾值
                int elapsedTicks = currentTick - rebuildStartTick;
                int initialShield = SHIELD_REBUILD_INITIAL.getOrDefault(playerId, 0);
                int newShield = initialShield + (regenRate * elapsedTicks);
                newShield = Math.min(newShield, maxShieldScaled);

                // 更新护盾值
                if (newShield != currentShield) {
                    ShieldManager.setPlayerShieldScaled(player, newShield);
                    // 重构期间每1刻更新一次客户端
                    if (currentTick % 1 == 0) {
                        ShieldNetworkManager.sendShieldUpdate(player);
                    }
                }
            } else {
                // 重构完成，确保护盾值为最大值
                if (currentShield != maxShieldScaled) {
                    ShieldManager.setPlayerShieldScaled(player, maxShieldScaled);
                    ShieldNetworkManager.sendShieldUpdate(player);
                }
                // 清除重构状态
                clearRebuildState(player);
                ShieldCooldownManager.endCooldown(player);
            }
            return;
        }

        // 2. 检查是否正在冷却中
        if (ShieldCooldownManager.isInCooldown(player)) {
            int cooldownStartTick = ShieldCooldownManager.getCooldownStartTick(player);
            int currentTickInCooldown = player.tickCount;

            if (currentTickInCooldown - cooldownStartTick >= ShieldManager.getShieldRebuildWaitTime(player)) {
                // 冷却时间结束，开始护盾重构
                startRebuild(player);
            } else {
                // 在冷却期间，每5刻更新一次客户端
                if (currentTickInCooldown % 5 == 0) {
                    ShieldNetworkManager.sendShieldUpdate(player);
                }
            }
        } else if (currentShield < maxShieldScaled) {
            // 3. 如果护盾值不满且不在冷却或重构状态，开始冷却计时
            ShieldCooldownManager.startCooldown(player);
            ShieldNetworkManager.sendShieldUpdate(player);
        }
    }

    /**
     * 开始护盾重构
     * @param player 玩家对象
     */
    public static void startRebuild(Player player) {
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        int currentShield = ShieldManager.getPlayerShieldScaled(player);

        SHIELD_REBUILD_START.put(playerId, currentTick);
        SHIELD_REBUILD_INITIAL.put(playerId, currentShield);
        ShieldCooldownManager.endCooldown(player);
        ShieldNetworkManager.sendShieldUpdate(player);
    }

    /**
     * 检查玩家是否正在重构中
     * @param player 玩家对象
     * @return 是否正在重构中
     */
    public static boolean isRebuilding(Player player) {
        UUID playerId = player.getUUID();
        return SHIELD_REBUILD_START.containsKey(playerId);
    }

    /**
     * 清除重构状态
     * @param player 玩家对象
     */
    public static void clearRebuildState(Player player) {
        UUID playerId = player.getUUID();
        SHIELD_REBUILD_START.remove(playerId);
        SHIELD_REBUILD_INITIAL.remove(playerId);
    }

    /**
     * 清理玩家重构数据
     * @param player 玩家对象
     */
    public static void clearPlayerRebuildData(Player player) {
        UUID playerId = player.getUUID();
        SHIELD_REBUILD_START.remove(playerId);
        SHIELD_REBUILD_INITIAL.remove(playerId);
    }

    /**
     * 清理所有玩家的重构状态（配置重载时使用）
     */
    public static void clearAllRebuildStates() {
        SHIELD_REBUILD_START.clear();
        SHIELD_REBUILD_INITIAL.clear();
    }
}
