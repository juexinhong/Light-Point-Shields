package com.gy_mod.gy_trinket.shield.particle;


import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import com.gy_mod.gy_trinket.Config;
import com.gy_mod.gy_trinket.Config.ShieldConfig;
import com.gy_mod.gy_trinket.item.ShieldItemGy;
import com.gy_mod.gy_trinket.shield.ShieldManager;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 护盾粒子效果生成器
 * 负责生成各种护盾相关的粒子效果
 */
public class ShieldParticleGenerator {
    
    /**
     * 生成护盾破裂粒子效果（服务器端）- 产生更多更大范围的粒子
     */
    public static void generateShieldBreakParticles(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        // 转换为服务器端玩家
        net.minecraft.server.level.ServerPlayer serverPlayer = (net.minecraft.server.level.ServerPlayer) player;
        
        // 获取玩家位置
        Vec3 playerPos = serverPlayer.position().add(0, serverPlayer.getBbHeight() / 2, 0);
        
        // 创建粒子效果
        net.minecraft.server.level.ServerLevel level = serverPlayer.serverLevel();
        
        // 生成末影人传送粒子（护盾破裂时：数量增加到40个，范围扩大到2格）
        for (int i = 0; i < 40; i++) {
            // 计算粒子位置：距离中心0-2.0格（更大范围）
            double offsetX = java.util.concurrent.ThreadLocalRandom.current().nextDouble(-2.0, 2.0);
            double offsetY = java.util.concurrent.ThreadLocalRandom.current().nextDouble(-2.0, 2.0);
            double offsetZ = java.util.concurrent.ThreadLocalRandom.current().nextDouble(-2.0, 2.0);
            
            // 计算粒子速度：X和Z轴向中心移动（归一化向量），Y轴单独向上
            double speed = 0.3;
            
            // 计算X和Z轴向中心的方向向量
            double dirX = -offsetX;
            double dirZ = -offsetZ;
            
            // 计算X-Z平面向量的长度（模长）用于归一化
            double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
            
            // 归一化X和Z轴向量并乘以速度
            double velocityX = (dirX / length) * speed;
            double velocityZ = (dirZ / length) * speed;
            
            // Y轴速度单独设置为向上
            double velocityY = 0.3;
            
            // 在玩家位置生成末影人传送粒子
            level.sendParticles(
                ParticleTypes.PORTAL,  // 末影人传送粒子效果
                playerPos.x + offsetX, 
                playerPos.y + offsetY, 
                playerPos.z + offsetZ, 
                1,  // 每个循环生成1个粒子
                velocityX, velocityY, velocityZ, 
                0  // 粒子速度无额外随机
            );
        }
    }
    
    /**
     * 生成火环粒子效果
     * 以玩家为中心，半径3格，水平圆圈边缘生成火焰粒子，粒子按顺时针方向移动
     */
    public static void generateFireRingParticles(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.level == null) {
            return;
        }
        
        // 获取玩家周围火环范围内的所有非玩家生物
        ShieldConfig config = ShieldManager.getActiveShieldConfig(player);
        double radius = config.fireRingRadius;
        java.util.List<net.minecraft.world.entity.Entity> entities = minecraft.level.getEntities(player, player.getBoundingBox().inflate(radius));
        boolean hasEnemies = false;
        
        // 检查是否有敌人
        for (net.minecraft.world.entity.Entity entity : entities) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity && !(entity instanceof net.minecraft.world.entity.player.Player)) {
                hasEnemies = true;
                break;
            }
        }
        
        // 只有当有敌人时才生成粒子
        if (!hasEnemies) {
            return;
        }
        
        // 使用传入的玩家参数
        double playerX = player.getX();
        double playerY = player.getY() + player.getBbHeight() / 2.0; // 玩家胸部位置
        double playerZ = player.getZ();
        
        int particleCount = 10;
        // radius变量已经在上面定义过了，使用之前定义的值
        
        for (int i = 0; i < particleCount; i++) {
            // 计算每个粒子的角度（顺时针）
            double angle = (i / (double) particleCount) * Math.PI * 2;
            
            // 添加微小的随机偏移到角度
            double angleOffset = (Math.random() - 0.5) * 0.2;
            double finalAngle = angle + angleOffset;
            
            // 计算粒子生成位置
            double x = playerX + Math.cos(finalAngle) * radius;
            double y = playerY + (Math.random() - 0.5) * 0.2; // 微小的Y轴偏移
            double z = playerZ + Math.sin(finalAngle) * radius;
            
            // 计算粒子速度：顺时针切线方向，降低速度到原来的1/3
            double vx = -Math.sin(finalAngle) * 0.166; // 0.5 / 3 ≈ 0.166
            double vz = Math.cos(finalAngle) * 0.166;
            
            // 添加微小的随机偏移到速度
            double speedOffset = (Math.random() - 0.5) * 0.05; // 减小速度随机偏移
            vx += speedOffset;
            vz += speedOffset;
            
            // Y轴速度设为0，保持水平移动
            double vy = 0.0;
            
            // 生成火焰粒子
            minecraft.level.addParticle(
                ParticleTypes.FLAME,
                x, y, z,
                vx, vy, vz
            );
        }
    }
    
    /**
     * 客户端生成护盾破裂粒子效果
     * 用于在客户端直接生成粒子效果- 从以玩家为中心半径1格的圆形边缘产生，X和Z轴向玩家移动，Y轴单独向上
     */
    public static void generateShieldBreakParticlesClient() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        
        // 获取玩家位置，在玩家身高一半处产生粒子
        // 玩家.position()返回中心位置，也就是身高一半的位置
        Vec3 playerPos = minecraft.player.position().add(0, 0, 0);
        
        // 生成末影人传送粒子（护盾值为零时：数量保持20个）
        for (int i = 0; i < 20; i++) {
            // 使用极坐标系统生成粒子位置：从半径0.1格的圆形边缘产生
            double radius = 0.1; // 将半径减小到0.1格
            double angle = java.util.concurrent.ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI); // 随机角度
            
            // 极坐标转换为笛卡尔坐标
            double offsetX = radius * Math.cos(angle);
            double offsetY = java.util.concurrent.ThreadLocalRandom.current().nextDouble(-0.1, 0.3); // Y轴有小范围随机偏移
            double offsetZ = radius * Math.sin(angle);
            
            // 计算粒子速度：X和Z轴使用归一化向量向中心移动，Y轴单独向上
            double speed = 0.6; // 保持较快速度
            
            // 计算X和Z轴向中心的方向向量：(0 - offsetX, 0 - offsetZ) = (-offsetX, -offsetZ)
            double dirX = -offsetX;
            double dirZ = -offsetZ;
            
            // 计算X-Z平面向量的长度（模长）用于归一化
            double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
            
            // 归一化X和Z轴向量并乘以速度
            double velocityX = (dirX / length) * speed;
            double velocityZ = (dirZ / length) * speed;
            
            // Y轴速度单独设置为向上
            double velocityY = -0.5;
            
            // 在玩家位置生成末影人传送粒子
            minecraft.level.addParticle(
                ParticleTypes.PORTAL,  // 末影人传送粒子效果
                playerPos.x + offsetX,  // 粒子生成位置：玩家中心 + X偏移
                playerPos.y + offsetY,  // 粒子生成位置：玩家中心 + Y偏移
                playerPos.z + offsetZ,  // 粒子生成位置：玩家中心 + Z偏移
                velocityX,              // X方向速度：向中心
                velocityY,              // Y方向速度：向上
                velocityZ               // Z方向速度：向中心
            );
        }
    }
    
    /**
     * 生成护盾重构粒子效果
     * 在客户端生成护盾重构时的粒子效果
     */
    public static void generateShieldRebuildParticles() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        
        Player player = minecraft.player;
        
        // 玩家位置（中心）
        double playerX = player.getX();
        double playerY = player.getY() + player.getBbHeight() / 4.0; // 玩家中心
        double playerZ = player.getZ();
        
        // 减少粒子数量到60个，避免遮挡视野
        for (int i = 0; i < 60; i++) {
            // 随机方向（从中心向外发散）
            double angle1 = minecraft.level.random.nextDouble() * Math.PI * 2; // 水平方向角度
            double angle2 = minecraft.level.random.nextDouble() * Math.PI; // 垂直方向角度
            double distance = minecraft.level.random.nextDouble() * 2.0; // 距离中心0-2格
            
            // 计算粒子位置（从中心向外的随机位置）
            double x = playerX + Math.sin(angle1) * Math.sin(angle2) * distance;
            double y = playerY + Math.cos(angle2) * distance * 0.25; // 高度限制在身高一半
            double z = playerZ + Math.cos(angle1) * Math.sin(angle2) * distance;
            
            // 计算速度方向（从中心向外发散）
            double dx = x - playerX;
            double dy = y - playerY;
            double dz = z - playerZ;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            
            // 标准化并添加随机偏移，使粒子向外发散
            double speed = 0.3 + minecraft.level.random.nextDouble() * 0.2;
            double vx = (dx / length) * speed + (minecraft.level.random.nextDouble() - 0.5) * 0.1;
            double vy = (dy / length) * speed + (minecraft.level.random.nextDouble() - 0.5) * 0.1;
            double vz = (dz / length) * speed + (minecraft.level.random.nextDouble() - 0.5) * 0.1;
            
            if (i % 2 == 0) {
                // 生成明显的蓝色粒子（使用SPLASH粒子）
                minecraft.level.addParticle(
                    ParticleTypes.SPLASH,
                    x, y, z,
                    vx, vy, vz
                );
            } else {
                // 生成白色粒子
                minecraft.level.addParticle(
                    ParticleTypes.CLOUD,
                    x, y, z,
                    vx, vy, vz
                );
            }
        }
    }
    

}