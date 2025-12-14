package com.gy_mod.gy_trinket.shield;

import com.gy_mod.gy_trinket.Config;
import com.gy_mod.gy_trinket.network.ModMessages;
import com.gy_mod.gy_trinket.network.ShieldUpdatePacket;
import com.gy_mod.gy_trinket.item.ShieldItemGy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * 护盾网络同步类，负责发送护盾值更新到客户端
 */
public class ShieldNetworkManager {
    /**
     * 发送护盾值更新到客户端
     * @param player 玩家对象
     */
    public static void sendShieldUpdate(Player player) {
        // 只在服务端执行，并确保是服务器玩家
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            UUID playerId = player.getUUID();
            int scaledShield = ShieldManager.getPlayerShieldScaled(player);
            float actualShield = scaledShield / 100.0f;

            // 计算冷却时间信息
            int currentCooldown = 0;
            int maxCooldown = Config.shieldRebuildWaitTime;

            // 检查玩家是否在冷却中
            if (ShieldCooldownManager.isInCooldown(player)) {
                int startTick = ShieldCooldownManager.getCooldownStartTick(player);
                int currentTick = player.tickCount;

                // 计算已经过去的冷却时间
                currentCooldown = currentTick - startTick;

                // 计算实际的冷却结束时间 = startTick + Config.shieldRebuildWaitTime
                int endTick = startTick + Config.shieldRebuildWaitTime;

                // 计算实际的总冷却时间
                int actualMaxCooldown = endTick - startTick;

                // 确保maxCooldown至少为原始值
                maxCooldown = Math.max(actualMaxCooldown, Config.shieldRebuildWaitTime);
            }

            // 检查玩家是否正在进行护盾重构
            boolean isRebuilding = ShieldRegenManager.isRebuilding(player);
            
            // 获取当前激活的护盾类型
            String activeShieldType = "";
            ShieldItemGy activeShield = ShieldManager.getActiveShieldItem(player);
            if (activeShield != null) {
                activeShieldType = ForgeRegistries.ITEMS.getKey(activeShield).toString();
            }

            // 发送护盾更新数据包到客户端，包含冷却时间信息、重构状态和激活护盾类型
            ShieldUpdatePacket.sendToPlayer(ModMessages.INSTANCE, new ShieldUpdatePacket(actualShield, Config.maxShield, currentCooldown, maxCooldown, isRebuilding, activeShieldType, 0.0F), serverPlayer);
        }
    }

    /**
     * 发送护盾值更新到客户端（简化版，用于快速更新）
     * @param player 玩家对象
     */
    public static void sendShieldUpdate(Player player, float shieldValue, float maxShield) {
        // 只在服务端执行，并确保是服务器玩家
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // 获取当前激活的护盾类型
            String activeShieldType = "";
            ShieldItemGy activeShield = ShieldManager.getActiveShieldItem(player);
            if (activeShield != null) {
                activeShieldType = ForgeRegistries.ITEMS.getKey(activeShield).toString();
            }
            
            // 发送护盾更新数据包到客户端，将maxShield转换为int类型
            ShieldUpdatePacket.sendToPlayer(ModMessages.INSTANCE, new ShieldUpdatePacket(shieldValue, (int) maxShield, 0, 0, false, activeShieldType, 0.0F), serverPlayer);
        }
    }

    /**
     * 发送护盾重置更新到客户端（用于隐藏HUD）
     * @param player 玩家对象
     */
    public static void sendShieldReset(Player player) {
        // 只在服务端执行，并确保是服务器玩家
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // 发送护盾更新数据包到客户端（0护盾和0最大护盾值，用于隐藏HUD）
            ShieldUpdatePacket.sendToPlayer(ModMessages.INSTANCE, new ShieldUpdatePacket(0, 0, 0, 0), serverPlayer);
        }
    }
}
