package com.gy_mod.gy_trinket.shield;

import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 玩家事件处理类，负责处理玩家的各种事件，如加入、重生、登出等
 */
public class ShieldPlayerEventHandler {
    /**
     * 处理玩家加入游戏事件
     * @param player 玩家对象
     */
    public static void handlePlayerJoin(Player player) {
        UUID playerId = player.getUUID();

        // 初始化玩家的护盾数据
        ShieldManager.setPlayerShieldScaled(player, ShieldManager.getMaxShieldScaled());

        // 清除可能存在的旧数据
        ShieldCooldownManager.clearPlayerCooldownData(player);
        ShieldRegenManager.clearRebuildState(player);
        ShieldDamageHandler.clearPlayerInvulnerabilityData(player);

        // 发送初始护盾值到客户端
        ShieldNetworkManager.sendShieldUpdate(player);
    }

    /**
     * 处理玩家重生事件
     * @param player 玩家对象
     */
    public static void handlePlayerRespawn(Player player) {
        // 玩家重生时，重置护盾为满值
        ShieldManager.setPlayerShieldScaled(player, ShieldManager.getMaxShieldScaled());

        // 清除所有相关状态
        ShieldCooldownManager.clearPlayerCooldownData(player);
        ShieldRegenManager.clearRebuildState(player);
        ShieldDamageHandler.clearPlayerInvulnerabilityData(player);

        // 发送更新到客户端
        ShieldNetworkManager.sendShieldUpdate(player);
    }

    /**
     * 处理玩家登出游戏事件
     * @param player 玩家对象
     */
    public static void handlePlayerLogout(Player player) {
        UUID playerId = player.getUUID();

        // 清理玩家的所有护盾相关数据，防止内存泄漏
        ShieldManager.clearPlayerShieldData(player);
        ShieldCooldownManager.clearPlayerCooldownData(player);
        ShieldRegenManager.clearPlayerRebuildData(player);
        ShieldDamageHandler.clearPlayerInvulnerabilityData(player);
    }

    /**
     * 处理玩家Tick事件
     * @param player 玩家对象
     */
    public static void handlePlayerTick(Player player) {
        // 检查玩家是否有护盾物品
        if (!ShieldManager.hasEnderShield(player)) {
            // 玩家没有护盾物品，清理护盾数据并发送更新
            ShieldManager.setPlayerShieldScaled(player, 0);
            ShieldNetworkManager.sendShieldReset(player);
            ShieldCooldownManager.clearPlayerCooldownData(player);
            ShieldRegenManager.clearRebuildState(player);
            ShieldDamageHandler.clearPlayerInvulnerabilityData(player);
            return;
        }

        // 处理护盾恢复逻辑
        ShieldRegenManager.handleShieldRegen(player);

        // 处理无敌时间
        ShieldDamageHandler.updateInvulnerability(player);
    }
}
