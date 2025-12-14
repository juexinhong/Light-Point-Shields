package com.gy_mod.gy_trinket.shield;

import com.gy_mod.gy_trinket.Config;
import com.gy_mod.gy_trinket.Config.ShieldConfig;
import com.gy_mod.gy_trinket.event.ShieldEvent;
import com.gy_mod.gy_trinket.item.ShieldItemGy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 护盾核心管理类，负责管理玩家的护盾数据和基本操作
 */
public class ShieldManager {
    // 存储玩家的护盾数据：UUID -> 剩余护盾值（放大100倍存储为整数，实现小数效果）
    private static final Map<UUID, Integer> PLAYER_SHIELD = new HashMap<>();
    
    // 存储客户端玩家的激活护盾类型：UUID -> 护盾物品的registry name
    // 仅在客户端使用，用于解决客户端无法访问末影箱的问题
    private static final Map<UUID, String> CLIENT_ACTIVE_SHIELD_TYPE = new HashMap<>();

    /**
     * 获取玩家当前护盾值
     * @param player 玩家对象
     * @return 当前护盾值（float类型，已缩小100倍）
     */
    public static float getPlayerShield(Player player) {
        UUID playerId = player.getUUID();
        int scaledShield = PLAYER_SHIELD.getOrDefault(playerId, 0);
        return scaledShield / 100.0f;
    }

    /**
     * 设置玩家护盾值
     * @param player 玩家对象
     * @param shieldValue 新的护盾值（float类型）
     */
    public static void setPlayerShield(Player player, float shieldValue) {
        UUID playerId = player.getUUID();
        int scaledShield = Math.round(shieldValue * 100);
        PLAYER_SHIELD.put(playerId, scaledShield);
    }

    /**
     * 获取玩家当前护盾值（放大后的值，用于内部计算）
     * @param player 玩家对象
     * @return 当前护盾值（整数类型，已放大100倍）
     */
    public static int getPlayerShieldScaled(Player player) {
        UUID playerId = player.getUUID();
        return PLAYER_SHIELD.getOrDefault(playerId, 0);
    }

    /**
     * 设置玩家当前护盾值（放大后的值，用于内部计算）
     * @param player 玩家对象
     * @param scaledShield 新的护盾值（整数类型，已放大100倍）
     */
    public static void setPlayerShieldScaled(Player player, int scaledShield) {
        UUID playerId = player.getUUID();
        PLAYER_SHIELD.put(playerId, scaledShield);
    }

    /**
     * 获取最大护盾值（基于默认配置）
     * @return 最大护盾值（float类型）
     */
    public static float getMaxShield() {
        return (float) Config.maxShield;
    }

    /**
     * 获取最大护盾值（基于默认配置，放大后的值）
     * @return 最大护盾值（整数类型，已放大100倍）
     */
    public static int getMaxShieldScaled() {
        return Config.maxShield * 100;
    }
    
    /**
     * 获取当前玩家的最大护盾值（基于当前激活的护盾类型）
     * @param player 玩家对象
     * @return 当前激活护盾的最大护盾值（float类型）
     */
    public static float getMaxShield(Player player) {
        ShieldItemGy activeShield = getActiveShieldItem(player);
        if (activeShield != null) {
            return activeShield.getMaxShield();
        }
        return getMaxShield();
    }
    
    /**
     * 获取当前玩家的最大护盾值（基于当前激活的护盾类型，放大后的值）
     * @param player 玩家对象
     * @return 当前激活护盾的最大护盾值（整数类型，已放大100倍）
     */
    public static int getMaxShieldScaled(Player player) {
        return Math.round(getMaxShield(player) * 100);
    }

    /**
     * 检查玩家末影箱内是否有自定义护盾物品
     * @param player 玩家对象
     * @return 是否有护盾物品
     */
    public static boolean hasEnderShield(Player player) {
        return getEnderShield(player) != null;
    }
    
    /**
     * 获取玩家末影箱内第一个护盾物品
     * 按末影箱栏位顺序查找，确保只生效最前面的护盾
     * @param player 玩家对象
     * @return 找到的第一个护盾物品，如果没有则返回null
     */
    public static ItemStack getEnderShield(Player player) {
        // 获取玩家的末影箱库存
        PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
        if (enderChest == null) {
            return null;
        }

        // 按栏位顺序遍历末影箱，返回找到的第一个护盾物品
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            ItemStack stack = enderChest.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ShieldItemGy) {
                // 检查物品是否是护盾物品（支持所有ShieldItemGy类型）
                return stack;
            }
        }
        return null;
    }
    
    /**
     * 获取当前激活的护盾物品
     * @param player 玩家对象
     * @return 当前激活的护盾物品，如果没有则返回null
     */
    public static ShieldItemGy getActiveShieldItem(Player player) {
        ItemStack shieldStack = getEnderShield(player);
        if (shieldStack != null && shieldStack.getItem() instanceof ShieldItemGy) {
            return (ShieldItemGy) shieldStack.getItem();
        }
        return null;
    }
    
    /**
     * 获取当前激活的护盾配置
     * @param player 玩家对象
     * @return 当前激活的护盾配置，如果没有护盾则返回默认配置
     */
    public static ShieldConfig getActiveShieldConfig(Player player) {
        // 优先使用客户端激活护盾类型（仅客户端有效）
        String clientShieldType = getClientActiveShieldType(player);
        if (!clientShieldType.isEmpty()) {
            return Config.getShieldConfigOrDefault(clientShieldType);
        }
        
        // 在服务端或客户端无法获取客户端激活护盾类型时，使用末影箱中的护盾物品
        ShieldItemGy activeShield = getActiveShieldItem(player);
        if (activeShield != null) {
            String itemId = ForgeRegistries.ITEMS.getKey(activeShield).toString();
            return Config.getShieldConfigOrDefault(itemId);
        }
        
        // 如果没有激活的护盾，返回默认配置
        return Config.getShieldConfigOrDefault("gy_trinket:shield_gy");
    }
    
    /**
     * 获取当前激活护盾的重构等待时间
     * @param player 玩家对象
     * @return 重构等待时间（tick）
     */
    public static int getShieldRebuildWaitTime(Player player) {
        return getActiveShieldConfig(player).shieldRebuildWaitTime;
    }
    
    /**
     * 获取当前激活护盾的恢复速度
     * @param player 玩家对象
     * @return 恢复速度（百分比/秒）
     */
    public static double getShieldNormalRegenPercentage(Player player) {
        return getActiveShieldConfig(player).shieldNormalRegenPercentage;
    }
    
    /**
     * 获取当前激活护盾的恢复间隔
     * @param player 玩家对象
     * @return 恢复间隔（tick）
     */
    public static int getShieldNormalRegenInterval(Player player) {
        return getActiveShieldConfig(player).shieldNormalRegenInterval;
    }
    
    /**
     * 获取当前激活护盾的最大等待时间
     * @param player 玩家对象
     * @return 最大等待时间（tick）
     */
    public static int getShieldMaxWaitTime(Player player) {
        return getActiveShieldConfig(player).shieldMaxWaitTime;
    }
    
    /**
     * 获取当前激活护盾的无敌时间
     * @param player 玩家对象
     * @return 无敌时间（tick）
     */
    public static int getShieldInvulnerabilityDuration(Player player) {
        return getActiveShieldConfig(player).shieldInvulnerabilityDuration;
    }
    
    /**
     * 获取当前激活护盾的伤害时等待时间延长
     * @param player 玩家对象
     * @return 伤害时等待时间延长（tick）
     */
    public static int getShieldWaitTimeExtensionOnHurt(Player player) {
        return getActiveShieldConfig(player).shieldWaitTimeExtensionOnHurt;
    }
    
    /**
     * 获取当前激活护盾的冷却延长倍数
     * @param player 玩家对象
     * @return 冷却延长倍数
     */
    public static double getShieldCooldownExtensionMultiplier(Player player) {
        return getActiveShieldConfig(player).shieldCooldownExtensionMultiplier;
    }
    
    /**
     * 获取当前激活护盾的护盾重构持续时间
     * @param player 玩家对象
     * @return 护盾重构持续时间（tick）
     */
    public static int getShieldRebuildDuration(Player player) {
        return getActiveShieldConfig(player).shieldRebuildDuration;
    }
    
    /**
     * 获取当前激活护盾的零护盾冷却延迟
     * @param player 玩家对象
     * @return 零护盾冷却延迟（tick）
     */
    public static int getShieldZeroShieldCooldownDelay(Player player) {
        return getActiveShieldConfig(player).shieldZeroShieldCooldownDelay;
    }

    /**
     * 初始化玩家护盾数据
     * @param player 玩家对象
     */
    public static void initializePlayerShield(Player player) {
        UUID playerId = player.getUUID();
        PLAYER_SHIELD.putIfAbsent(playerId, 0);
    }

    /**
     * 清理玩家护盾数据
     * @param player 玩家对象
     */
    public static void clearPlayerShield(Player player) {
        clearPlayerShieldData(player);
    }
    
    /**
     * 清理玩家护盾数据（用于统一接口）
     * @param player 玩家对象
     */
    public static void clearPlayerShieldData(Player player) {
        UUID playerId = player.getUUID();
        PLAYER_SHIELD.remove(playerId);
    }

    /**
     * 检查玩家是否有护盾值
     * @param player 玩家对象
     * @return 是否有护盾值
     */
    public static boolean hasShield(Player player) {
        return getPlayerShieldScaled(player) > 0;
    }
    
    /**
     * 设置客户端玩家的激活护盾类型
     * 仅在客户端使用
     * @param player 玩家对象
     * @param shieldType 护盾物品的registry name
     */
    public static void setClientActiveShieldType(Player player, String shieldType) {
        UUID playerId = player.getUUID();
        CLIENT_ACTIVE_SHIELD_TYPE.put(playerId, shieldType);
    }
    
    /**
     * 获取客户端玩家的激活护盾类型
     * 仅在客户端使用
     * @param player 玩家对象
     * @return 护盾物品的registry name，如果没有则返回空字符串
     */
    public static String getClientActiveShieldType(Player player) {
        UUID playerId = player.getUUID();
        return CLIENT_ACTIVE_SHIELD_TYPE.getOrDefault(playerId, "");
    }
    
    /**
     * 在客户端检查当前激活的护盾是否是火环护盾
     * @param player 玩家对象
     * @return 是否是火环护盾
     */
    public static boolean isClientActiveShieldFireRing(Player player) {
        String shieldType = getClientActiveShieldType(player);
        return "gy_trinket:shield_fire_ring".equals(shieldType);
    }
}
