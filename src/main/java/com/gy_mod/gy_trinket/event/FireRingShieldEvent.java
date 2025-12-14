package com.gy_mod.gy_trinket.event;

import com.gy_mod.gy_trinket.Config;
import com.gy_mod.gy_trinket.Config.ShieldConfig;
import com.gy_mod.gy_trinket.item.FireRingShield;
import com.gy_mod.gy_trinket.item.ShieldItemGy;
import com.gy_mod.gy_trinket.shield.ShieldManager;
import com.gy_mod.gy_trinket.shield.particle.ShieldParticleGenerator;
import net.minecraft.world.damagesource.DamageSource;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.UUID;

/**
 * 火环护盾事件处理类
 * 实现火环护盾的特殊效果：当护盾值大于0时，在玩家周围产生火焰环，点燃并伤害附近的非玩家生物
 */
public class FireRingShieldEvent {
    
    // 存储玩家最近伤害的实体映射 <玩家UUID, <实体UUID, 伤害时间戳>>
    private static final Map<UUID, Map<UUID, Long>> recentlyHurtEntities = new HashMap<>();
    
    // 使用配置文件中的冷却时间（刻）
    /**
     * 注册事件
     */
    public static void register() {
        // 注册火环护盾特有的事件
        MinecraftForge.EVENT_BUS.register(new FireRingShieldEvent());
    }
    
    /**
     * 监听实体攻击事件，记录玩家最近攻击的实体
     * LivingAttackEvent在伤害结算前触发，比LivingHurtEvent更快
     */
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        
        // 检查伤害来源是否是玩家
        if (source.getEntity() instanceof Player player) {
            // 保存被攻击实体到映射中，有效期500毫秒
            UUID playerUUID = player.getUUID();
            UUID targetUUID = target.getUUID();
            long currentTime = System.currentTimeMillis();
            
            recentlyHurtEntities.computeIfAbsent(playerUUID, k -> new HashMap<>())
                              .put(targetUUID, currentTime);
        }
    }
    
    /**
     * 检查实体是否在玩家的最近伤害列表中（500毫秒内）
     */
    private boolean isEntityRecentlyHurtByPlayer(Player player, LivingEntity target) {
        UUID playerUUID = player.getUUID();
        UUID targetUUID = target.getUUID();
        
        if (recentlyHurtEntities.containsKey(playerUUID)) {
            Map<UUID, Long> playerHurtEntities = recentlyHurtEntities.get(playerUUID);
            if (playerHurtEntities.containsKey(targetUUID)) {
                long hurtTime = playerHurtEntities.get(targetUUID);
                long currentTime = System.currentTimeMillis();
                
                        // 检查是否在冷却时间内（使用配置文件中的值，转换为毫秒）
                ShieldConfig config = Config.getShieldConfig("gy_trinket:shield_fire_ring");
                long cooldownTimeMs = config.fireRingPlayerAttackExemptionTime * 50; // 1刻 = 50毫秒
                if (currentTime - hurtTime <= cooldownTimeMs) {
                    return true;
                } else {
                    // 超过冷却时间，移除该实体
                    playerHurtEntities.remove(targetUUID);
                }
            }
        }
        
        return false;
    }
    
    /**
     * 清理过期的伤害记录
     */
    private void cleanupExpiredHurtRecords() {
        long currentTime = System.currentTimeMillis();
       // 清理过期的伤害记录
        ShieldConfig config = Config.getShieldConfig("gy_trinket:shield_fire_ring");
        long cooldownTimeMs = config.fireRingPlayerAttackExemptionTime * 50; // 1刻 = 50毫秒
        
        recentlyHurtEntities.forEach((playerUUID, hurtEntities) -> {
            hurtEntities.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > cooldownTimeMs
            );
        });
        
        // 移除空的玩家条目
        recentlyHurtEntities.entrySet().removeIf(entry -> 
            entry.getValue().isEmpty()
        );
    }

    /**
     * 监听玩家tick事件，实现火环效果
     * @param event 玩家tick事件
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        
        // 客户端：生成火环粒子效果
        if (event.side.isClient()) {
            // 直接检查护盾值 - 由于网络同步机制，只要护盾值大于0，就说明玩家当前激活了护盾
            float currentShield = ShieldManager.getPlayerShield(player);
            // 将粒子生成频率降低一半（每2个tick生成一次）
            // 并检查当前激活的护盾是否是火环护盾，只有火环护盾才生成火环粒子
            if (currentShield > 0 && player.tickCount % 2 == 0 && ShieldManager.isClientActiveShieldFireRing(player)) {
                // 生成火环粒子效果
                ShieldParticleGenerator.generateFireRingParticles(player);
            }
            return;
        }

        Level level = player.level();

        // 检查玩家是否激活了火环护盾
        ShieldItemGy activeShield = ShieldManager.getActiveShieldItem(player);
        if (!(activeShield instanceof FireRingShield)) {
            return;
        }

        // 检查护盾值是否大于0，只有护盾值大于0时才触发效果
        float currentShield = ShieldEvent.getPlayerShield(player);
        if (currentShield <= 0) {
            return;
        }

        // 获取火环配置
        ShieldConfig config = Config.getShieldConfig("gy_trinket:shield_fire_ring");
        
        // 每N个刻执行一次效果，减少性能消耗（使用配置文件中的值）
        if (player.tickCount % config.fireRingTriggerFrequency != 0) {
            return;
        }
        
        // 清理过期的伤害记录
        cleanupExpiredHurtRecords();

        // 创建玩家周围的AABB检测范围（使用配置的火环半径）
        AABB aabb = player.getBoundingBox().inflate(config.fireRingRadius);

        // 获取范围内的所有非玩家生物
        List<Entity> entities = level.getEntities(player, aabb);

        // 对每个实体执行火环效果
        boolean hasEnemies = false;
        for (Entity entity : entities) {
            // 只对非玩家生物生效
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                hasEnemies = true;
                LivingEntity livingEntity = (LivingEntity) entity;
                
                // 检查敌人是否在最近500毫秒内被玩家伤害过，若是则跳过
                if (isEntityRecentlyHurtByPlayer(player, livingEntity)) {
                    continue;
                }
                
                // 定义基础伤害值（使用配置文件中的值）
                float baseDamage = (float) config.fireRingDamage;
                float actualDamage = baseDamage;
                
                // 获取目标当前生命值
                float currentHealth = livingEntity.getHealth();
                
                // 生命值检测和伤害源选择
                if (currentHealth <= baseDamage) {
                    // 目标生命值低于或等于基础伤害值时，使用玩家伤害源且伤害乘2(火伤好像穿甲的,但玩家伤害又穿不了,拉高点cos穿甲)
                    actualDamage = baseDamage * 2;
                    livingEntity.hurt(livingEntity.damageSources().playerAttack(player), actualDamage);
                } else {
                    // 目标生命值高于基础伤害值时，使用火焰伤害源
                    livingEntity.hurt(livingEntity.damageSources().inFire(), actualDamage);
                }
                
                // 直接清除敌人的伤害免疫时间，提高性能
                livingEntity.invulnerableTime = 0;
                
                // 火环伤害敌人后损耗玩家护盾值（使用配置文件中的值）
                float currentShieldValue = ShieldEvent.getPlayerShield(player);
                float newShieldValue = Math.max(0, currentShieldValue - (float) config.fireRingShieldCost);
                ShieldEvent.updatePlayerShield(player, newShieldValue);
                
                // 检查护盾值是否为零，若为零则重新开始护盾冷却
                if (newShieldValue <= 0 && currentShieldValue > 0) {
                    ShieldEvent.triggerShieldCooldown(player);
                }
            }
        }
        
        // 在服务器端发送信号给客户端，指示是否有敌人
        // 这里使用粒子生成的条件来间接控制，只有当有敌人时才会有后续的粒子生成逻辑
    }
    

}
