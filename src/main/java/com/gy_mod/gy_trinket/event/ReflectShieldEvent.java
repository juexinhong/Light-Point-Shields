package com.gy_mod.gy_trinket.event;

import com.gy_mod.gy_trinket.Config;
import com.gy_mod.gy_trinket.item.ReflectShield;
import com.gy_mod.gy_trinket.item.ShieldItemGy;
import com.gy_mod.gy_trinket.shield.ShieldDamageHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import java.util.List;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.gy_mod.gy_trinket.shield.ShieldManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 反射护盾事件处理类
 * 继承自基础护盾机制，额外实现反射弹射物的特殊效果
 */
public class  ReflectShieldEvent {
    // 反射护盾无敌时间（刻） - 现在使用配置文件中的值
    // private static final int REFLECT_SHIELD_INVULNERABILITY_TICKS = 5; // 0.25秒

    /**
     * 注册事件
     */
    public static void register() {
        // 注册反射护盾特有的事件
        MinecraftForge.EVENT_BUS.register(new ReflectShieldEvent());
    }

    /**
     * 在ShieldEvent中处理反射逻辑的辅助方法
     * 当正常护盾机制处理完弹射物伤害后，调用此方法执行反射
     * @param player 玩家对象
     * @param damage 实际伤害值
     */
    public static void handleReflectAfterShield(Player player, float damage) {
        // 检查是否是服务器端
        if (player.level().isClientSide()) {
            return;
        }

        // 获取玩家UUID
        UUID playerId = player.getUUID();
        int currentTick = player.tickCount;

        // 检查玩家是否已经处于无敌状态
        if (ShieldDamageHandler.isInvulnerable(player)) {
            return;
        }

        // 获取玩家最近一次的弹射物伤害信息
        ShieldEvent.ProjectileDamageInfo projectileInfo = ShieldEvent.getLastProjectileInfo(player);
        if (projectileInfo == null) {
            return; // 没有弹射物信息，无法反射
        }

        Projectile projectile = projectileInfo.getProjectile();
        
        // 检查玩家是否处于无敌时间，如果是则不反射
        if (ShieldDamageHandler.isInvulnerable(player)) {
            return;
        }
        
        // 检查玩家当前激活的护盾是否是反射护盾
        ShieldItemGy activeShield = ShieldManager.getActiveShieldItem(player);
        if (!(activeShield instanceof ReflectShield)) {
            return;
        }
        
        // 检查玩家是否有护盾值且不在重构状态
        // 使用ShieldEvent提供的公共方法获取护盾值
        float currentShieldFloat = ShieldEvent.getPlayerShield(player);
        int currentShield = Math.round(currentShieldFloat * 100); // 放大100倍
        
        // 在生成反射烈焰弹之前检查护盾值是否为零
        if (currentShield <= 0) {
            return;
        }
        
        // 反射弹射物
        reflectProjectile(projectile, player, damage, projectileInfo);
        
        // 移除弹射物信息，避免重复处理
        ShieldEvent.removeLastProjectileInfo(player);
    }

    /**
     * 自定义大型烈焰弹类 - 爆炸时不生成火焰
     */
    public static class NoFireLargeFireball extends LargeFireball {
        // 存储爆炸半径
        private final int explosionRadius;
        // 存储原弹射物的伤害值
        private final float originalDamage;
        // 存储当前护盾值的10%因子
        private final double shieldFactor;
        // 存储反射伤害修正系数
        private final double damageModifier;
        
        public NoFireLargeFireball(Level worldIn, LivingEntity shooter, double accelX, double accelY, double accelZ, int explosionRadiusIn, float originalDamageIn, double shieldFactorIn, double damageModifierIn) {
            super(worldIn, shooter, accelX, accelY, accelZ, explosionRadiusIn);
            this.explosionRadius = explosionRadiusIn;
            this.originalDamage = originalDamageIn;
            this.shieldFactor = shieldFactorIn;
            this.damageModifier = damageModifierIn;
        }
        
        @Override
        protected void onHit(HitResult result) {
            // 不调用super.onHit(result)，避免生成火焰和处理默认碰撞
            if (!this.level().isClientSide()) {
                // 如果命中实体，检查是否是Fireball类型
                if (result.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityResult = (EntityHitResult) result;
                    Entity hitEntity = entityResult.getEntity();
                    
                    // 如果命中的是Fireball实体，不触发爆炸，直接返回
                    // 这样可以避免反射火球与其他火球（如烈焰人的火球）相互引爆
                    if (hitEntity instanceof Fireball) {
                        return;
                    }
                    
                    // 对命中的实体造成直接伤害
                    // 伤害公式：原弹射物伤害 × 护盾因子（当前护盾值的10%，最低1） × 反射伤害修正系数
                    float damage = (float)(this.originalDamage * this.shieldFactor * this.damageModifier);
                    hitEntity.hurt(this.damageSources().fireball(this, this.getOwner()), damage);
                    
                    // 应用伤害效果
                    if (hitEntity != null && this.getOwner() instanceof LivingEntity) {
                        ((LivingEntity) this.getOwner()).doEnchantDamageEffects(
                            (LivingEntity) this.getOwner(), 
                            hitEntity
                        );
                    }
                }
                
                // 创建一个不破坏地形但有伤害的爆炸
                // 使用NONE爆炸类型，这种类型完全不会破坏地形但会对实体造成伤害
                this.level().explode(
                    this.getOwner(), // 爆炸源实体
                    this.getX(), // X坐标
                    this.getY(), // Y坐标
                    this.getZ(), // Z坐标
                    (float) this.explosionRadius, // 爆炸半径
                    false, // 是否生成火焰
                    Level.ExplosionInteraction.NONE // 爆炸交互类型，NONE完全不会破坏地形但会对实体造成伤害
                );
                
                // 移除烈焰弹实体
                this.discard();
            }
        }
        
        @Override
        public void tick() {
            super.tick();
            
            // 只在服务端执行追踪逻辑
            if (this.level().isClientSide()) {
                return;
            }
            
            // 搜索范围内的敌人实体，追踪范围为10格
            List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().inflate(10.0D), entity -> {
                // 排除：
                // 1. 自己
                // 2. 发射者（玩家）
                // 3. 其他火球
                // 4. 玩家
                // 5. 掉落物
                // 6. 经验球
                // 7. 盔甲架
                // 8. 画
                // 9. 非生物实体
                if (entity == this || entity == this.getOwner() || entity instanceof Fireball) {
                    return false;
                }
                if (entity instanceof Player) {
                    return false;
                }
                // 更兼容1.18.2版本的实体类型检查
                if (entity.getType() == net.minecraft.world.entity.EntityType.ITEM_FRAME || 
                    entity.getType() == net.minecraft.world.entity.EntityType.GLOW_ITEM_FRAME) {
                    return false;
                }
                if (entity.getType() == net.minecraft.world.entity.EntityType.ARMOR_STAND) {
                    return false;
                }
                if (entity.getType() == net.minecraft.world.entity.EntityType.EXPERIENCE_ORB) {
                    return false;
                }
                if (entity.getType() == net.minecraft.world.entity.EntityType.ITEM) {
                    return false;
                }
                if (!(entity instanceof LivingEntity)) {
                    return false;
                }
                // 确保是敌对实体
                if (entity instanceof LivingEntity) {
                    // 在1.18.2中，检查实体是否是怪物（实现了Monster接口）
                    return entity instanceof net.minecraft.world.entity.monster.Monster;
                }
                return false;
            });
            
            // 如果找到合适的目标
            if (!entities.isEmpty()) {
                // 找到最近的敌人
                Entity nearestEntity = null;
                double nearestDistance = Double.MAX_VALUE;
                
                for (Entity entity : entities) {
                    double distance = this.distanceToSqr(entity);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestEntity = entity;
                    }
                }
                
                if (nearestEntity != null) {
                    // 计算追踪方向
                    double dx = nearestEntity.getX() - this.getX();
                    double dy = nearestEntity.getY() + nearestEntity.getBbHeight() * 0.5 - this.getY(); // 瞄准实体中心
                    double dz = nearestEntity.getZ() - this.getZ();
                    
                    // 计算追踪方向的单位向量
                    double magnitude = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (magnitude > 0) {
                        dx /= magnitude;
                        dy /= magnitude;
                        dz /= magnitude;
                        
                        // 获取当前运动方向
                        double currentDx = this.getDeltaMovement().x();
                        double currentDy = this.getDeltaMovement().y();
                        double currentDz = this.getDeltaMovement().z();
                        
                        // 计算当前运动速度
                        double currentSpeed = Math.sqrt(currentDx * currentDx + currentDy * currentDy + currentDz * currentDz);
                        
                        // 平滑追踪：逐渐转向目标方向，每次转向60%（增强追踪效果）
                        double newDx = currentDx * 0.4 + dx * 0.6;
                        double newDy = currentDy * 0.4 + dy * 0.6;
                        double newDz = currentDz * 0.4 + dz * 0.6;
                        
                        // 保持速度不变
                        double newMagnitude = Math.sqrt(newDx * newDx + newDy * newDy + newDz * newDz);
                        if (newMagnitude > 0) {
                            newDx = newDx / newMagnitude * currentSpeed;
                            newDy = newDy / newMagnitude * currentSpeed;
                            newDz = newDz / newMagnitude * currentSpeed;
                        }
                        
                        // 设置新的运动方向
                        this.setDeltaMovement(newDx, newDy, newDz);
                    }
                }
            }
        }
        

    }

    /**
     * 反射弹射物 - 将所有弹射物反射为恶魂的烈焰弹
     * @param projectile 要反射的弹射物
     * @param player 执行反射的玩家
     * @param actualDamage 弹射物的实际伤害值
     * @param info 弹射物伤害信息，包含位置、速度等
     */
    private static void reflectProjectile(Projectile projectile, Player player, float actualDamage, ShieldEvent.ProjectileDamageInfo info) {
        // 使用传入的实际伤害值，避免重复计算
        float originalDamage = actualDamage;
        
        // 移除原弹射物
        projectile.remove(Entity.RemovalReason.DISCARDED);
        
        // 获取当前护盾值的10%（最低1）
        float currentShieldFloat = ShieldEvent.getPlayerShield(player);
        double shieldFactor = Math.max(currentShieldFloat * 0.1, 1.0);
        
        // 计算新速度：（原速度 × 护盾因子 + 3）× 配置的速度修正系数
        double newSpeed = (info.getOriginalSpeed() * shieldFactor + 3) * Config.reflectShieldSpeedModifier;
        
        // 计算反射方向的单位向量（原方向的反方向）
        double dirX = -info.getDirX();
        double dirY = -info.getDirY();
        double dirZ = -info.getDirZ();
        
        // 打印配置值，确认配置是否正确加载

        
        // 使用自定义的NoFireLargeFireball类，确保爆炸时不生成火焰
        NoFireLargeFireball fireball = new NoFireLargeFireball(
            player.level(),
            player, // 发射者
            dirX,
            dirY,
            dirZ,
            (int) Math.round(Config.reflectShieldExplosionRadius), // 使用配置的爆炸范围
            originalDamage, // 原弹射物的伤害值
            shieldFactor, // 当前护盾值的10%因子
            Config.reflectShieldDamageModifier // 反射伤害修正系数
        );
        
        // 计算偏移量：沿着原弹射物运动方向的反方向偏移0.5个方块
        double offsetX = info.getDirX() > 0 ? 0.5 : -0.5;
        double offsetY = info.getDirY() > 0 ? 0.5 : -0.5;
        double offsetZ = info.getDirZ() > 0 ? 0.5 : -0.5;
        
        // 将烈焰弹的生成位置设置为原弹射物位置加上偏移量
        fireball.setPos(info.getPosX() + offsetX, info.getPosY() + offsetY, info.getPosZ() + offsetZ);
        
        // 设置烈焰弹的速度
        fireball.setDeltaMovement(
            dirX * newSpeed,
            dirY * newSpeed,
            dirZ * newSpeed
        );
        
        // 设置所有者为玩家
        fireball.setOwner(player);
        
        // 重置状态
        fireball.tickCount = 0;
        fireball.setInvulnerable(false);
        
        // 将烈焰弹添加到世界
        player.level().addFreshEntity(fireball);
    }
    

}