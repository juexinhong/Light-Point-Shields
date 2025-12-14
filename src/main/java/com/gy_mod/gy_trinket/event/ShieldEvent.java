package com.gy_mod.gy_trinket.event;

import com.gy_mod.gy_trinket.Config;
import com.gy_mod.gy_trinket.network.ModMessages;
import com.gy_mod.gy_trinket.item.ShieldItemGy;
import com.gy_mod.gy_trinket.network.ShieldUpdatePacket;
import com.gy_mod.gy_trinket.shield.ShieldManager;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.TickEvent;
import com.gy_mod.gy_trinket.item.ShieldItemGy;
import com.gy_mod.gy_trinket.item.ReflectShield;
import com.gy_mod.gy_trinket.item.AmplifierShield;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 护盾事件处理类
 * 处理玩家护盾的创建、伤害吸收、恢复冷却等逻辑
 */
public class ShieldEvent {
    // 存储玩家的护盾数据：UUID -> 剩余护盾值（放大100倍存储为整数，实现小数效果）
    private static final Map<UUID, Integer> PLAYER_SHIELD = new HashMap<>();
    // 存储玩家的护盾冷却时间开始时间：UUID -> 开始冷却的刻数
    private static final Map<UUID, Integer> SHIELD_COOLDOWN_START = new HashMap<>();
    // 存储玩家的护盾重构开始时间：UUID -> 开始重构的刻数
    private static final Map<UUID, Integer> SHIELD_REBUILD_START = new HashMap<>();
    // 存储玩家的护盾重构前的初始值：UUID -> 重构开始时的护盾值（放大100倍）
    private static final Map<UUID, Integer> SHIELD_REBUILD_INITIAL = new HashMap<>();
    // 存储玩家的无敌时间结束刻数：UUID -> 无敌时间结束的刻数
    private static final Map<UUID, Integer> INVULNERABILITY_END = new HashMap<>();
    // 存储玩家最后一次触发护盾冷却的刻数（用于防止高频触发）
    private static final Map<UUID, Integer> LAST_COOLDOWN_TRIGGER = new HashMap<>();
    
    // 存储玩家最近一次的弹射物伤害信息（用于反射护盾）
    private static final Map<UUID, ProjectileDamageInfo> LAST_PROJECTILE_INFO = new HashMap<>();
    
    // 内部类：存储弹射物伤害信息
    public static class ProjectileDamageInfo {
        private final Projectile projectile; // 原始弹射物
        private final double originalSpeed; // 原始速度
        private final double posX; // 撞击位置X
        private final double posY; // 撞击位置Y
        private final double posZ; // 撞击位置Z
        private final double dirX; // 运动方向X
        private final double dirY; // 运动方向Y
        private final double dirZ; // 运动方向Z
        
        public ProjectileDamageInfo(Projectile projectile, double originalSpeed, 
                                  double posX, double posY, double posZ,
                                  double dirX, double dirY, double dirZ) {
            this.projectile = projectile;
            this.originalSpeed = originalSpeed;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.dirX = dirX;
            this.dirY = dirY;
            this.dirZ = dirZ;
        }
        
        public Projectile getProjectile() { return projectile; }
        public double getOriginalSpeed() { return originalSpeed; }
        public double getPosX() { return posX; }
        public double getPosY() { return posY; }
        public double getPosZ() { return posZ; }
        public double getDirX() { return dirX; }
        public double getDirY() { return dirY; }
        public double getDirZ() { return dirZ; }
    }

    /**
     * 获取玩家当前护盾值（供其他类调用）
     * @param player 玩家对象
     * @return 当前护盾值（float类型，已缩小100倍）
     */
    public static float getPlayerShield(Player player) {
        UUID playerId = player.getUUID();
        int scaledShield = PLAYER_SHIELD.getOrDefault(playerId, 0);
        return scaledShield / 100.0f;
    }
    
    /**
     * 设置玩家最近一次的弹射物伤害信息
     * @param player 玩家对象
     * @param projectile 原始弹射物
     */
    public static void setLastProjectileInfo(Player player, Projectile projectile) {
        // 如果弹射物的所有者是当前玩家，则不存储该信息，避免反射自己的弹射物
        if (projectile.getOwner() == player) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // 保存原弹射物的位置和运动向量
        double projectileX = projectile.getX();
        double projectileY = projectile.getY();
        double projectileZ = projectile.getZ();
        
        // 获取原弹射物的移动向量
        double originalMotionX = projectile.getDeltaMovement().x;
        double originalMotionY = projectile.getDeltaMovement().y;
        double originalMotionZ = projectile.getDeltaMovement().z;
        
        // 计算原弹射物的速度
        double originalSpeed = Math.sqrt(originalMotionX * originalMotionX + originalMotionY * originalMotionY + originalMotionZ * originalMotionZ);
        
        // 计算运动方向的单位向量
        double magnitude = originalSpeed;
        double dirX = originalMotionX / magnitude;
        double dirY = originalMotionY / magnitude;
        double dirZ = originalMotionZ / magnitude;
        
        // 创建并存储弹射物信息
        ProjectileDamageInfo info = new ProjectileDamageInfo(projectile, originalSpeed, 
                                                           projectileX, projectileY, projectileZ,
                                                           dirX, dirY, dirZ);
        
        LAST_PROJECTILE_INFO.put(playerId, info);
    }
    
    /**
     * 获取玩家最近一次的弹射物伤害信息
     * @param player 玩家对象
     * @return 弹射物伤害信息，如果没有则返回null
     */
    public static ProjectileDamageInfo getLastProjectileInfo(Player player) {
        UUID playerId = player.getUUID();
        return LAST_PROJECTILE_INFO.getOrDefault(playerId, null);
    }
    
    /**
     * 移除玩家最近一次的弹射物伤害信息
     * @param player 玩家对象
     */
    public static void removeLastProjectileInfo(Player player) {
        UUID playerId = player.getUUID();
        LAST_PROJECTILE_INFO.remove(playerId);
    }
    
    /**
     * 更新玩家护盾值（供其他类调用）
     * @param player 玩家对象
     * @param newShieldValue 新的护盾值（float类型，会自动放大100倍存储）
     */
    public static void updatePlayerShield(Player player, float newShieldValue) {
        UUID playerId = player.getUUID();
        int scaledShield = Math.round(newShieldValue * 100);
        
        // 确保护盾值不会为负数
        scaledShield = Math.max(0, scaledShield);
        
        // 更新护盾值
        PLAYER_SHIELD.put(playerId, scaledShield);
        
        // 自动发送护盾更新数据包
        sendShieldUpdate(player);
    }

    /**
     * 获取最大护盾值
     * @return 最大护盾值（float类型）
     */
    public static float getMaxShield(Player player) {
        return (float) ShieldManager.getMaxShield(player);
    }
    
    /**
     * 触发护盾冷却（供子类使用）
     * @param player 玩家对象
     */
    protected static void triggerShieldCooldown(Player player) {
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        
        // 开始新的护盾冷却
        SHIELD_COOLDOWN_START.put(playerId, currentTick);
        // 清除可能的重构状态
        SHIELD_REBUILD_START.remove(playerId);
        SHIELD_REBUILD_INITIAL.remove(playerId);
    }

    /**
     * 发送护盾值更新到客户端
     * @param player 玩家对象
     */
    private static void sendShieldUpdate(Player player) {
        // 只在服务端执行，并确保是服务器玩家
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            UUID playerId = player.getUUID();
            int scaledShield = PLAYER_SHIELD.getOrDefault(playerId, 0);
            float actualShield = scaledShield / 100.0f;
            
            // 计算冷却时间信息
            int currentCooldown = 0;
            int maxCooldown = ShieldManager.getShieldRebuildWaitTime(player);
            
            // 检查玩家是否在冷却中
            if (SHIELD_COOLDOWN_START.containsKey(playerId)) {
                int startTick = SHIELD_COOLDOWN_START.get(playerId);
                int currentTick = player.tickCount;
                
                // 计算已经过去的冷却时间
                currentCooldown = currentTick - startTick;
                
                // 计算实际的冷却结束时间 = startTick + ShieldManager.getShieldRebuildWaitTime(player)
                // 这是因为我们通过调整开始时间来延长冷却，所以结束时间 = 新的开始时间 + 原始冷却时间
                int endTick = startTick + ShieldManager.getShieldRebuildWaitTime(player);
                
                // 计算实际的总冷却时间 = endTick - 原始开始时间
                // 由于我们可能多次调整开始时间，原始开始时间已经丢失，所以我们使用一个简单的方法：
                // 如果结束时间大于当前时间 + 原始冷却时间，说明冷却被延长了
                int actualMaxCooldown = endTick - startTick;
                
                // 确保maxCooldown至少为原始值
                maxCooldown = Math.max(actualMaxCooldown, ShieldManager.getShieldRebuildWaitTime(player));
            }
            
            // 检查玩家是否正在进行护盾重构
            boolean isRebuilding = SHIELD_REBUILD_START.containsKey(playerId);
            
            // 获取玩家当前激活的护盾类型
            ShieldItemGy activeShield = ShieldManager.getActiveShieldItem(player);
            String shieldType = "";
            if (activeShield != null) {
                shieldType = ForgeRegistries.ITEMS.getKey(activeShield.asItem()).toString();
            }
            
            // 获取所有正在应用的伤害提升百分比
            float damageBonusPercentage = 0.0F;
            if (activeShield instanceof AmplifierShield) {
                damageBonusPercentage = AmplifierShieldEvent.getAllDamageModifiers(player);
            }
            
            // 发送护盾更新数据包到客户端，包含冷却时间信息、重构状态、护盾类型和伤害提升
            ShieldUpdatePacket.sendToPlayer(ModMessages.INSTANCE, new ShieldUpdatePacket(actualShield, (int)ShieldManager.getMaxShield(player), currentCooldown, maxCooldown, isRebuilding, shieldType, damageBonusPercentage), serverPlayer);
        }
    }

    /**
     * 注册事件
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ShieldEvent());
    }

    /**
     * 处理配置重载事件，当配置文件变更时更新所有玩家的护盾值
     */
    public static void onConfigReload(ModConfigEvent event) {
        // 遍历所有玩家的护盾数据，确保它们不超过新的最大护盾值
        for (UUID playerId : PLAYER_SHIELD.keySet()) {
            int currentShield = PLAYER_SHIELD.get(playerId);
            // 由于无法直接获取玩家对象，我们使用默认的最大护盾值
            int maxShieldScaled = Config.maxShield * 100;
            
            if (currentShield > maxShieldScaled) {
                PLAYER_SHIELD.put(playerId, maxShieldScaled);
            }
        }
        
        // 重置所有玩家的护盾重构状态，确保新的配置生效
        for (UUID playerId : SHIELD_REBUILD_START.keySet()) {
            // 重置重构状态，让新的配置生效
            SHIELD_REBUILD_START.remove(playerId);
            SHIELD_REBUILD_INITIAL.remove(playerId);
        }
        
        // 添加配置重载标记，在玩家下次tick时发送更新
        configReloaded = true;
    }
    
    // 配置重载标记，用于在玩家下次tick时发送更新
    private static boolean configReloaded = false;
    
    // 存储最近一次弹射物攻击的实际伤害值，用于反射护盾计算
    private static final Map<UUID, Float> LAST_PROJECTILE_DAMAGE = new HashMap<>();
    
    /**
     * 获取玩家最近一次受到的弹射物伤害值
     * @param player 玩家
     * @return 最近一次弹射物伤害值，如果没有则返回0
     */
    public static float getLastProjectileDamage(Player player) {
        return LAST_PROJECTILE_DAMAGE.getOrDefault(player.getUUID(), 0.0F);
    }
    
    /**
     * 设置玩家最近一次受到的弹射物伤害值
     * @param player 玩家
     * @param damage 伤害值
     */
    public static void setLastProjectileDamage(Player player, float damage) {
        LAST_PROJECTILE_DAMAGE.put(player.getUUID(), damage);
    }
    
    /**
     * 处理玩家反射时的冷却延迟效果
     * @param player 玩家
     * @param damage 弹射物伤害值
     */
    public static void handleReflectionCooldown(Player player, float damage) {
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        
        // 检查当前护盾值是否未满
        float updatedShieldFloat = getPlayerShield(player);
        float maxShieldFloat = ShieldManager.getMaxShield(player);
        
        if (updatedShieldFloat < maxShieldFloat) {
            // 如果正在护盾冷却中，延长冷却时间
            if (SHIELD_COOLDOWN_START.containsKey(playerId)) {
                int cooldownStartTick = SHIELD_COOLDOWN_START.get(playerId);
                int originalCooldownEnd = cooldownStartTick + ShieldManager.getShieldRebuildWaitTime(player);
                int remainingCooldown = Math.max(0, originalCooldownEnd - currentTick);
                
                // 使用实际伤害值计算冷却延长时间：基础值 * (提升倍数 ^ 伤害值)
                int extendedCooldownTime = (int) (ShieldManager.getShieldWaitTimeExtensionOnHurt(player) * Math.pow(ShieldManager.getShieldCooldownExtensionMultiplier(player), damage));
                
                // 确保延长时间至少为基础值
                extendedCooldownTime = Math.max(extendedCooldownTime, ShieldManager.getShieldWaitTimeExtensionOnHurt(player));
                
                // 计算新的冷却结束时间：当前剩余冷却时间 + 新的冷却延长时间
                int newCooldownEnd = currentTick + remainingCooldown + extendedCooldownTime;
                // 不超过最大冷却时间
                newCooldownEnd = Math.min(newCooldownEnd, currentTick + ShieldManager.getShieldMaxWaitTime(player));
                
                // 更新冷却开始时间（相当于延长冷却），确保不小于当前时间
                int newCooldownStart = newCooldownEnd - ShieldManager.getShieldRebuildWaitTime(player);
                SHIELD_COOLDOWN_START.put(playerId, Math.max(newCooldownStart, currentTick));
            } else {
                // 开始新的护盾冷却
                SHIELD_COOLDOWN_START.put(playerId, currentTick);
                // 清除可能的重构状态
                SHIELD_REBUILD_START.remove(playerId);
                SHIELD_REBUILD_INITIAL.remove(playerId);
            }
        }
    }

    // ========== 1. 攻击事件监听（伤害应用前） ==========
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        // 检查是否是玩家受到攻击，且在服务端执行
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        // 检查伤害源，如果是溺水伤害，不进行护盾吸收
        DamageSource source = event.getSource();
        if (source.getMsgId().equals("drown")) {
            return;
        }

        // 检查玩家末影箱是否有护盾物品
        if (!hasEnderShield(player)) {
            return;
        }

        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        
        // 检查玩家是否处于无敌时间内
        if (INVULNERABILITY_END.containsKey(playerId)) {
            int invulnerabilityEnd = INVULNERABILITY_END.get(playerId);
            if (currentTick <= invulnerabilityEnd) {
                // 玩家处于无敌时间内，取消攻击事件
                event.setCanceled(true);
                return;
            } else {
                // 无敌时间结束，移除无敌状态
                INVULNERABILITY_END.remove(playerId);
            }
        }

        // 如果玩家没有护盾数据，初始化为0护盾值
        PLAYER_SHIELD.putIfAbsent(playerId, 0);
        int currentShield = PLAYER_SHIELD.get(playerId);

        // 检查是否正在进行护盾重构
        boolean isRebuilding = SHIELD_REBUILD_START.containsKey(playerId);
        
        // 获取原始伤害值（在条件块外部定义，确保整个方法都能访问）
        float damage = event.getAmount();
        
        // 检查是否是弹射物伤害，如果是，更新最近一次弹射物伤害值和信息
        // 我们已经在方法开始处获取了source变量，所以直接使用它
        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof Projectile projectile) {
            setLastProjectileDamage(player, damage);
            // 存储弹射物信息
            setLastProjectileInfo(player, projectile);
        }
        
        // 标志位，用于跟踪是否已经处理了冷却逻辑
        boolean cooldownProcessed = false;
        
        // 检查玩家是否使用反射护盾，如果是且伤害来自弹射物，则应用修正系数
        float damageToScale = damage;
        ShieldItemGy activeShield = ShieldManager.getActiveShieldItem(player);
        if (activeShield instanceof ReflectShield && directEntity instanceof Projectile) {
            // 应用反射护盾修正系数，显式转换避免精度损失警告
            damageToScale = damage * (float)Config.reflectShieldCost;
        }
        
        // 如果有剩余护盾值且不在重构状态，取消攻击事件以防止受击特效
        if (currentShield > 0 && !isRebuilding) {
            // 取消攻击事件以防止受击特效
            event.setCanceled(true);
            
            // 将伤害放大100倍来匹配护盾值的存储方式
            int scaledDamage = Math.round(damageToScale * 100); // 使用round确保精度
            // 计算可以吸收的伤害（不超过当前护盾值）
            int absorbed = Math.min(scaledDamage, currentShield);
            // 计算剩余伤害（放大后）
            int remainingScaledDamage = scaledDamage - absorbed;
            // 剩余伤害缩小回原始比例
            float remainingDamage = remainingScaledDamage / 100.0f;
            
            // 更新护盾值
            PLAYER_SHIELD.put(playerId, currentShield - absorbed);
            // 发送护盾值更新到客户端
            sendShieldUpdate(player);
            
            // 检查是否使用增幅护盾，如果是，记录损失的护盾值（伤害）
            if (activeShield instanceof com.gy_mod.gy_trinket.item.AmplifierShield) {
                // 将吸收的伤害缩小回原始比例（用于增幅护盾效果）
                float absorbedDamage = absorbed / 100.0f;
                AmplifierShieldEvent.recordShieldDamage(player, absorbedDamage);
            }
            
            // 检查是否有弹射物信息，如果有，调用反射护盾的处理逻辑
            // 使用反射伤害（被吸收的伤害）作为实际伤害值
            float actualReflectDamage = absorbed / 100.0f; // 缩小回原始比例
            if (getLastProjectileInfo(player) != null) {
                // 使用反射护盾处理逻辑
                ReflectShieldEvent.handleReflectAfterShield(player, actualReflectDamage);
            }
            
            // 如果有剩余伤害，手动应用到玩家身上
            if (remainingDamage > 0) {
                player.hurt(source, remainingDamage);
            }
            
            // 设置短暂无敌时间（在剩余伤害应用之后）
            // 从配置文件中读取无敌时间设置
            INVULNERABILITY_END.put(playerId, currentTick + ShieldManager.getShieldInvulnerabilityDuration(player));
            
            // 检查当前护盾值是否未满或为零
            int updatedShield = PLAYER_SHIELD.getOrDefault(playerId, 0);
            int maxShieldScaled = (int)(ShieldManager.getMaxShield(player) * 100);
            if (updatedShield < maxShieldScaled) {
                // 如果正在护盾冷却中，延长冷却时间
                if (SHIELD_COOLDOWN_START.containsKey(playerId)) {
                    int cooldownStartTick = SHIELD_COOLDOWN_START.get(playerId);
                    int originalCooldownEnd = cooldownStartTick + ShieldManager.getShieldRebuildWaitTime(player);
                    
                    // 基于伤害值计算冷却延长时间：基础值 * (提升倍数 ^ 伤害值)
                    int extendedCooldownTime = (int) (ShieldManager.getShieldWaitTimeExtensionOnHurt(player) * Math.pow(ShieldManager.getShieldCooldownExtensionMultiplier(player), damage));
                    
                    // 确保延长时间至少为基础值
                    extendedCooldownTime = Math.max(extendedCooldownTime, ShieldManager.getShieldWaitTimeExtensionOnHurt(player));
                    
                    // 计算新的冷却结束时间：原始冷却结束时间 + 新的冷却延长时间
                    int newCooldownEnd = originalCooldownEnd + extendedCooldownTime;
                    // 不超过最大冷却时间
                    newCooldownEnd = Math.min(newCooldownEnd, currentTick + ShieldManager.getShieldMaxWaitTime(player));
                    
                    // 更新冷却开始时间（相当于延长冷却），确保不小于原始冷却开始时间
                    int newCooldownStart = newCooldownEnd - ShieldManager.getShieldRebuildWaitTime(player);
                    SHIELD_COOLDOWN_START.put(playerId, Math.max(newCooldownStart, cooldownStartTick));
                } else {
                    // 开始新的护盾冷却
                    SHIELD_COOLDOWN_START.put(playerId, currentTick);
                    // 清除可能的重构状态
                    SHIELD_REBUILD_START.remove(playerId);
                    SHIELD_REBUILD_INITIAL.remove(playerId);
                }
                // 标记冷却逻辑已处理
                cooldownProcessed = true;
            }
        } else if (currentShield == 0 && !isRebuilding) {
            // 护盾值为0且不在重构状态，允许伤害直接应用
            // 但只在onLivingAttack中处理一次冷却逻辑，避免与onLivingDamage重复
            if (!cooldownProcessed) {
                // 使用配置文件中的内置冷却时间
                int builtInCooldownTicks = ShieldManager.getShieldZeroShieldCooldownDelay(player);
                
                // 检查是否在内置冷却时间内
                if (!LAST_COOLDOWN_TRIGGER.containsKey(playerId) || 
                    currentTick - LAST_COOLDOWN_TRIGGER.get(playerId) >= builtInCooldownTicks) {
                    
                    // 如果正在护盾冷却中，延长冷却时间；否则开始新的冷却
                    if (SHIELD_COOLDOWN_START.containsKey(playerId)) {
                        int cooldownStartTick = SHIELD_COOLDOWN_START.get(playerId);
                        int originalCooldownEnd = cooldownStartTick + ShieldManager.getShieldRebuildWaitTime(player);
                        
                        // 基于伤害值计算冷却延长时间：基础值 * (提升倍数 ^ 伤害值)
                        int extendedCooldownTime = (int) (ShieldManager.getShieldWaitTimeExtensionOnHurt(player) * Math.pow(ShieldManager.getShieldCooldownExtensionMultiplier(player), damage));
                        
                        // 确保延长时间至少为基础值
                        extendedCooldownTime = Math.max(extendedCooldownTime, ShieldManager.getShieldWaitTimeExtensionOnHurt(player));
                        
                        // 计算新的冷却结束时间：原始冷却结束时间 + 新的冷却延长时间
                        int newCooldownEnd = originalCooldownEnd + extendedCooldownTime;
                        // 不超过最大冷却时间
                        newCooldownEnd = Math.min(newCooldownEnd, currentTick + ShieldManager.getShieldMaxWaitTime(player));
                        
                        // 更新冷却开始时间（相当于延长冷却），确保不小于原始冷却开始时间
                        int newCooldownStart = newCooldownEnd - ShieldManager.getShieldRebuildWaitTime(player);
                        SHIELD_COOLDOWN_START.put(playerId, Math.max(newCooldownStart, cooldownStartTick));
                    } else {
                        // 开始新的护盾冷却
                        SHIELD_COOLDOWN_START.put(playerId, currentTick);
                    }
                    // 清除可能的重构状态
                    SHIELD_REBUILD_START.remove(playerId);
                    SHIELD_REBUILD_INITIAL.remove(playerId);
                    // 标记冷却逻辑已处理
                    cooldownProcessed = true;
                    
                    // 更新最后一次触发冷却的时间
                    LAST_COOLDOWN_TRIGGER.put(playerId, currentTick);
                }
            }
        }
    }

    // ========== 2. 伤害事件监听（用于处理无护盾时的伤害） ==========
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        // 检查是否是玩家受到伤害，且在服务端执行
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        // 检查伤害源，如果是溺水伤害，不进行护盾吸收
        DamageSource source = event.getSource();
        if (source.getMsgId().equals("drown")) {
            return;
        }

        // 检查玩家末影箱是否有护盾物品
        if (!hasEnderShield(player)) {
            return;
        }

        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        
        // 检查玩家是否处于无敌时间内，如果是，直接取消伤害事件
        if (INVULNERABILITY_END.containsKey(playerId)) {
            int invulnerabilityEnd = INVULNERABILITY_END.get(playerId);
            if (currentTick <= invulnerabilityEnd) {
                event.setCanceled(true); // 完全取消伤害事件
                return;
            }
        }
        
        // 检查玩家是否处于无敌时间内，如果是，直接取消伤害事件
        if (INVULNERABILITY_END.containsKey(playerId)) {
            int invulnerabilityEnd = INVULNERABILITY_END.get(playerId);
            if (currentTick <= invulnerabilityEnd) {
                event.setCanceled(true); // 完全取消伤害事件
                return;
            }
        }
        
        // 检查玩家是否有护盾值或正在重构，如果有，不处理伤害（由onLivingAttack处理）
        int currentShield = PLAYER_SHIELD.getOrDefault(playerId, 0);
        boolean isRebuilding = SHIELD_REBUILD_START.containsKey(playerId);
        if (currentShield > 0 || isRebuilding) {
            event.setCanceled(true);
            return;
        }
        
        // 没有护盾值时，已经在onLivingAttack中处理了冷却逻辑
        // 这里不再重复处理，避免冷却时间被延长两次
    }

    // ========== 2. 玩家Tick事件监听 ==========
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 获取玩家对象
        Player player = event.player;

        // 只在服务端的结束阶段执行
        if (player.level().isClientSide() || event.phase != TickEvent.Phase.END) {
            return;
        }

        // 如果玩家末影箱没有护盾物品，清空护盾数据
        if (!hasEnderShield(player)) {
            UUID playerId = player.getUUID();
            PLAYER_SHIELD.remove(playerId);
            SHIELD_COOLDOWN_START.remove(playerId);
            SHIELD_REBUILD_START.remove(playerId);
            SHIELD_REBUILD_INITIAL.remove(playerId);
            // 清理玩家NBT中的护盾值
            player.getPersistentData().putInt("gy_trinket:shield_value", 0);
            // 发送护盾值更新到客户端（0护盾和0最大护盾值，用于隐藏HUD）
            if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                ShieldUpdatePacket.sendToPlayer(ModMessages.INSTANCE, new ShieldUpdatePacket(0, 0, 0, 0, false, "", 0.0F), serverPlayer);
            }
            return;
        }

        UUID playerId = player.getUUID();
        // 如果玩家没有护盾数据，初始化为0护盾值，然后立即开始冷却
        if (!PLAYER_SHIELD.containsKey(playerId)) {
            PLAYER_SHIELD.put(playerId, 0);
            // 开始冷却计时
            SHIELD_COOLDOWN_START.put(playerId, player.tickCount);
            // 发送护盾值更新到客户端，包括冷却信息
            sendShieldUpdate(player);
        }
        
        int currentShield = PLAYER_SHIELD.get(playerId);
        int maxShield = (int)ShieldManager.getMaxShield(player);
        int maxShieldScaled = maxShield * 100;
        
        // 如果当前护盾值超过新的最大护盾值，将其限制为新的最大值
        if (currentShield > maxShieldScaled) {
            PLAYER_SHIELD.put(playerId, maxShieldScaled);
            sendShieldUpdate(player);
        }
        
        // 检查配置是否已重载，如果是则立即发送更新到客户端
        if (configReloaded) {
            sendShieldUpdate(player);
        }

        // 如果护盾已满，清空所有状态标记
        if (currentShield >= maxShieldScaled) {
            SHIELD_COOLDOWN_START.remove(playerId);
            SHIELD_REBUILD_START.remove(playerId);
            SHIELD_REBUILD_INITIAL.remove(playerId);
            // 发送更新到客户端，确保护盾满时冷却计时被清空
            sendShieldUpdate(player);
            
            // 在处理完所有玩家后重置配置重载标记
            if (configReloaded) {
                configReloaded = false;
            }
            
            return;
        }

        int currentTick = player.tickCount;

        // 重置配置重载标记（确保只在所有玩家都处理完毕后重置一次）
        // 使用currentTick % 20 == 0作为一个简单的同步机制，确保标记在处理完所有玩家后才重置
        if (configReloaded && currentTick % 20 == 0) {
            configReloaded = false;
        }
        
        // ========== 自然恢复效果 ==========
        // 只要护盾值不为零且未满，按照配置的间隔和百分比恢复，不受冷却和等待限制影响
        if (currentTick % ShieldManager.getShieldNormalRegenInterval(player) == 0 && currentShield > 0 && currentShield < maxShieldScaled) {
            // 计算恢复量：当前最大护盾值 × 配置的恢复百分比（放大100倍存储）
            // 先计算为double类型，以便判断是否大于0但小于1
            double regenAmountDouble = maxShieldScaled * (ShieldManager.getShieldNormalRegenPercentage(player) / 100.0);
            int regenAmount = (int)regenAmountDouble;
            
            // 如果恢复量大于0但小于1，则至少恢复1点护盾值
            if (regenAmountDouble > 0 && regenAmount == 0) {
                regenAmount = 1;
            }
            
            int newShield = Math.min(currentShield + regenAmount, maxShieldScaled);
            
            // 强制更新护盾值，确保恢复效果可见
            PLAYER_SHIELD.put(playerId, newShield);
            sendShieldUpdate(player);
        }
        
        // ========== 护盾重构逻辑 ==========
        // 1. 检查是否正在进行护盾重构
        if (SHIELD_REBUILD_START.containsKey(playerId)) {
            int rebuildStartTick = SHIELD_REBUILD_START.get(playerId);
            int rebuildDuration = ShieldManager.getShieldRebuildDuration(player);
            
            if (currentTick - rebuildStartTick <= rebuildDuration) {
                // 正在重构中
                // 计算固定恢复速率：最大护盾值 / 重构持续时间（刻）
                // 确保从0到满需要配置的重构持续时间
                int regenRate = maxShieldScaled / rebuildDuration;
                
                // 计算当前应该恢复的护盾值
                int elapsedTicks = currentTick - rebuildStartTick;
                int initialShield = SHIELD_REBUILD_INITIAL.getOrDefault(playerId, 0);
                int newShield = initialShield + (regenRate * elapsedTicks);
                newShield = Math.min(newShield, maxShieldScaled);
                
                // 更新护盾值
                if (newShield != currentShield) {
                    PLAYER_SHIELD.put(playerId, newShield);
                    // 重构期间每1刻更新一次客户端，流畅度优先毕竟只持续很短时间
                    if (currentTick % 1 == 0) {
                        sendShieldUpdate(player);
                    }
                }
            } else {
                // 重构完成，确保护盾值为最大值（放大后）
                if (currentShield != maxShieldScaled) {
                    PLAYER_SHIELD.put(playerId, maxShieldScaled);
                    sendShieldUpdate(player);
                }
                // 清除重构状态
                SHIELD_REBUILD_START.remove(playerId);
                SHIELD_REBUILD_INITIAL.remove(playerId);
                SHIELD_COOLDOWN_START.remove(playerId);
            }
            return; // 重构期间不执行其他恢复逻辑，包括伤害事件处理
        }
        
        // 2. 检查是否正在护盾冷却中
        boolean isInCooldown = false;
        if (SHIELD_COOLDOWN_START.containsKey(playerId)) {
            isInCooldown = true;
            int cooldownStartTick = SHIELD_COOLDOWN_START.get(playerId);
            
            if (currentTick - cooldownStartTick >= ShieldManager.getShieldRebuildWaitTime(player)) {
                // 冷却时间结束，开始护盾重构
                SHIELD_REBUILD_START.put(playerId, currentTick);
                SHIELD_REBUILD_INITIAL.put(playerId, currentShield);
                SHIELD_COOLDOWN_START.remove(playerId);
                isInCooldown = false;
                sendShieldUpdate(player); // 发送冷却结束和重构开始的更新
            } else {
                // 在冷却期间，每5刻更新一次客户端，平衡流畅度和性能
                if (currentTick % 5 == 0) {
                    sendShieldUpdate(player);
                }
            }
        } else if (currentShield < maxShieldScaled && !SHIELD_REBUILD_START.containsKey(playerId)) {
            // 3. 如果护盾值不满且不在冷却或重构状态，开始冷却计时
            SHIELD_COOLDOWN_START.put(playerId, currentTick);
            sendShieldUpdate(player); // 发送冷却开始的更新
        }
    }

    // ========== 3. 玩家退出事件监听 ==========
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // 从事件中获取玩家实体
        Player player = (Player) event.getEntity();
        // 获取玩家UUID
        UUID playerId = player.getUUID();
        
        // 保存护盾值到玩家NBT数据中（持久化存储）
        if (PLAYER_SHIELD.containsKey(playerId)) {
            int shieldValue = PLAYER_SHIELD.get(playerId);
            // 只有当护盾值不为零时才保存，避免死亡时保存零护盾值
            if (shieldValue > 0) {
                player.getPersistentData().putInt("gy_trinket:shield_value", shieldValue);
            } else {
                // 如果护盾值为零，保存0护盾值
                player.getPersistentData().putInt("gy_trinket:shield_value", 0);
            }
        }
        
        // 清理所有临时数据
        SHIELD_COOLDOWN_START.remove(playerId);
        SHIELD_REBUILD_START.remove(playerId);
        SHIELD_REBUILD_INITIAL.remove(playerId);
        INVULNERABILITY_END.remove(playerId);
        LAST_COOLDOWN_TRIGGER.remove(playerId);
    }

    // ========== 4. 玩家加入事件监听 ==========
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = (Player) event.getEntity();
        UUID playerId = player.getUUID();
        
        // 从玩家NBT数据中加载护盾值
        int savedShield = player.getPersistentData().getInt("gy_trinket:shield_value");
        
        // 如果有保存的护盾值，检查是否有护盾物品，如果没有则不使用保存值
        if (savedShield > 0 && hasEnderShield(player)) {
            PLAYER_SHIELD.put(playerId, savedShield);
        } else {
            // 初始化为0护盾值
            PLAYER_SHIELD.put(playerId, 0);
        }
        
        // 发送护盾值更新到客户端，确保HUD正确显示
        sendShieldUpdate(player);
    }
    
    // ========== 5. 玩家重生事件监听 ==========
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = (Player) event.getEntity();
        UUID playerId = player.getUUID();
        
        // 重生时重置所有护盾相关状态
        // 1. 初始化为0护盾值
        PLAYER_SHIELD.put(playerId, 0);
        
        // 2. 清除所有状态标记
        SHIELD_COOLDOWN_START.remove(playerId);
        SHIELD_REBUILD_START.remove(playerId);
        SHIELD_REBUILD_INITIAL.remove(playerId);
        INVULNERABILITY_END.remove(playerId);
        
        // 3. 重置玩家NBT数据中的护盾值为0
        player.getPersistentData().putInt("gy_trinket:shield_value", 0);
        
        // 4. 发送护盾值更新到客户端，确保HUD正确显示
        sendShieldUpdate(player);
    }

    // ========== 5. 辅助方法 ==========
    /**
     * 在反射时触发冷却延迟效果
     * @param player 玩家对象
     * @param damage 伤害值
     */
    protected static void triggerReflectCooldown(Player player, float damage) {
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;
        
        // 基于伤害值计算冷却延长时间：基础值 * (提升倍数 ^ 伤害值)
        int extendedCooldownTime = (int) (ShieldManager.getShieldWaitTimeExtensionOnHurt(player) * Math.pow(ShieldManager.getShieldCooldownExtensionMultiplier(player), damage));
        
        // 确保延长时间至少为基础值
        extendedCooldownTime = Math.max(extendedCooldownTime, ShieldManager.getShieldWaitTimeExtensionOnHurt(player));
        
        if (SHIELD_COOLDOWN_START.containsKey(playerId)) {
            // 获取当前冷却的开始时间
            int cooldownStartTick = SHIELD_COOLDOWN_START.get(playerId);
            // 计算原始冷却结束时间
            int originalCooldownEnd = cooldownStartTick + ShieldManager.getShieldRebuildWaitTime(player);
            // 计算剩余冷却时间
            int remainingCooldown = Math.max(0, originalCooldownEnd - currentTick);
            
            // 计算新的冷却结束时间
            int newCooldownEnd = currentTick + remainingCooldown + extendedCooldownTime;
            
            // 限制冷却时间不超过最大值
            newCooldownEnd = Math.min(newCooldownEnd, currentTick + ShieldManager.getShieldMaxWaitTime(player));
            
            // 更新冷却开始时间，确保不小于当前时间
            int newCooldownStart = newCooldownEnd - ShieldManager.getShieldRebuildWaitTime(player);
            SHIELD_COOLDOWN_START.put(playerId, Math.max(newCooldownStart, currentTick));
        } else {
            // 没有冷却时，直接开始新的冷却
            SHIELD_COOLDOWN_START.put(playerId, currentTick);
        }
    }
    
    /**
     * 检查玩家是否拥有末影箱护盾物品
     * @param player 玩家对象
     * @return 是否拥有末影箱护盾物品
     */
    public static boolean hasEnderShield(Player player) {
        // 获取玩家的末影箱库存
        PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
        if (enderChest == null) {
            return false;
        }

        // 遍历末影箱所有格子，检查是否有自定义护盾物品
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            ItemStack stack = enderChest.getItem(i);
            // 判断物品是否是我们的自定义护盾类型（ShieldItemGy）
            if (!stack.isEmpty() && stack.getItem() instanceof com.gy_mod.gy_trinket.item.ShieldItemGy) {
                return true;
            }
        }
        return false;
    }
}