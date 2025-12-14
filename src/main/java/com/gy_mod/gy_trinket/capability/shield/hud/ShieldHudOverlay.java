package com.gy_mod.gy_trinket.capability.shield.hud;

import com.gy_mod.gy_trinket.gy_trinket;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gy_mod.gy_trinket.shield.particle.ShieldParticleGenerator;

/**
 * 极简版护盾值HUD渲染类（仅显示，无交互）
 * 固定位置渲染护盾条+数值，无需按键/鼠标操作
 */
@OnlyIn(Dist.CLIENT)
public class ShieldHudOverlay {
    // 单例实例
    private static ShieldHudOverlay instance;
    // 护盾纹理（需自行准备182x10像素的纹理）
    private static final ResourceLocation SHIELD_ICON = ResourceLocation.fromNamespaceAndPath(gy_trinket.MOD_ID, "textures/gui/shield_gui.png");
    // 护盾冷却时间纹理
    private static final ResourceLocation SHIELD_COOLDOWN_ICON = ResourceLocation.fromNamespaceAndPath(gy_trinket.MOD_ID, "textures/gui/shield_cooldown_gui.png");

    // 护盾值缓存
    private float currentShield = 0;
    private float maxShield = 0;
    // 冷却时间缓存
    private long cooldownStartTime = 0;
    private int cooldownDuration = 0;
    private boolean isInCooldown = false;
    // 目标值（用于平滑过渡）
    private float targetCurrentShield = 0;
    private float targetMaxShield = 0;
    // 平滑过渡参数
    private static final float SMOOTHING_FACTOR = 0.15f; // 平滑因子，值越大过渡越快
    // 伤害提升相关
    private float damageBonusPercentage = 0.0f;
    private float targetDamageBonus = 0.0f;
    // HUD固定尺寸
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int COOLDOWN_BAR_HEIGHT = 5; // 增加冷却时间条高度为5像素，与护盾条相同高度，更明显
    private static final int TEXTURE_WIDTH = 182;
    private static final int TEXTURE_HEIGHT = 10;

    // 单例模式
    public static ShieldHudOverlay getInstance() {
        if (instance == null) {
            instance = new ShieldHudOverlay();
        }
        return instance;
    }

    private ShieldHudOverlay() {}

    // ========== 对外接口：更新护盾值和冷却时间 ==========
    public void updateShieldData(float current, float max, int currentCooldown, int maxCooldown, float newDamageBonus) {
        // 只更新目标值，实际显示值通过插值计算平滑过渡
        this.targetCurrentShield = Math.max(0, Math.min(current, max));
        this.targetMaxShield = Math.max(0, max);
        
        // 更新伤害提升目标值
        this.targetDamageBonus = newDamageBonus;
        
        // 确保最大护盾值变化时，当前显示值也能正确过渡
        if (this.maxShield != this.targetMaxShield) {
            this.maxShield = this.targetMaxShield;
        }
        
        // 处理冷却时间（仅在有冷却时间时更新）
        if (maxCooldown > 0) {
            if (currentCooldown > 0) {
                // 如果正在冷却中，重新计算冷却开始时间和持续时间
                this.cooldownDuration = maxCooldown;
                this.cooldownStartTime = System.currentTimeMillis() - (currentCooldown * 50); // 1刻 = 50毫秒
                this.isInCooldown = true;
            } else {
                // 冷却结束
                this.isInCooldown = false;
                this.cooldownStartTime = 0;
                this.cooldownDuration = 0;
            }
        } else {
            // 没有冷却时间
            this.isInCooldown = false;
            this.cooldownStartTime = 0;
            this.cooldownDuration = 0;
        }
    }
    
    // ========== 对外接口：获取当前护盾值 ==========
    public float getCurrentShield() {
        return currentShield;
    }
    
    // ========== 对外接口：获取最大护盾值 ==========
    public float getMaxShield() {
        return maxShield;
    }
    
    // ========== 对外接口：处理护盾重构状态 ==========
    public void handleShieldRebuilding(boolean isRebuilding) {
        if (isRebuilding) {
            // 护盾开始重构，生成粒子效果
            ShieldParticleGenerator.generateShieldRebuildParticles();
        }
    }

    // ========== 核心渲染逻辑 ==========    
    public void render(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        // 仅在游戏内、有玩家时渲染
        if (minecraft.player == null || minecraft.screen != null) return;

        // 平滑过渡：每帧向目标值靠近
        smoothTransition();

        Window window = minecraft.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        // HUD固定位置：屏幕上方中间，更明显的位置，避免被其他UI元素遮挡
        int hudX = screenWidth / 2 - BAR_WIDTH / 2;
        int hudY = 50; // 移动到屏幕上方50像素处

        // 渲染准备
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 只有当有护盾值时才绘制护盾HUD
        if (maxShield > 0) {
            // 1. 绘制护盾背景条（UV: 0,0 → 182,5）
            guiGraphics.blit(SHIELD_ICON, hudX, hudY, 0, 0, BAR_WIDTH, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

            // 2. 绘制护盾填充条（根据当前护盾值比例）
            if (currentShield > 0) {
                int fillWidth = (int) (currentShield / maxShield * BAR_WIDTH);
                guiGraphics.blit(SHIELD_ICON, hudX, hudY, 0, 5, fillWidth, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            }

            // 3. 绘制护盾数值文本（居中显示）
            drawShieldText(guiGraphics, hudX, hudY);
        }

        // 4. 绘制护盾冷却时间条（无论是否有护盾值，只要在冷却时间内就显示）
        drawShieldCooldown(guiGraphics, hudX, hudY);
        
        // 5. 绘制伤害提升文本
        drawDamageBonusText(guiGraphics);

        // 渲染清理
        RenderSystem.disableBlend();
    }
    
    // ========== 平滑过渡逻辑 ==========
    private void smoothTransition() {
        // 平滑过渡护盾值
        if (Math.abs(currentShield - targetCurrentShield) > 0.01f) {
            currentShield += (targetCurrentShield - currentShield) * SMOOTHING_FACTOR;
            // 确保不会因为精度问题永远无法到达目标值
            if (Math.abs(currentShield - targetCurrentShield) < 0.02f) {
                currentShield = targetCurrentShield;
            }
        }
        
        // 平滑过渡伤害提升
        if (Math.abs(damageBonusPercentage - targetDamageBonus) > 0.01f) {
            damageBonusPercentage += (targetDamageBonus - damageBonusPercentage) * SMOOTHING_FACTOR;
            // 确保不会因为精度问题永远无法到达目标值
            if (Math.abs(damageBonusPercentage - targetDamageBonus) < 0.02f) {
                damageBonusPercentage = targetDamageBonus;
            }
        }
    }

    // ========== 绘制护盾数值文本 ==========
    private void drawShieldText(GuiGraphics guiGraphics, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        String shieldText = String.format("%.2f/%.0f", currentShield, maxShield);
        float scale = 0.75f;

        // 文本缩放+居中
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);

        int textX = (int) ((x + BAR_WIDTH/2f - mc.font.width(shieldText) * scale / 2f) / scale);
        int textY = (int) ((y + BAR_HEIGHT + 2f) / scale);

        // 蓝色文本（带阴影）
        guiGraphics.drawString(mc.font, shieldText, textX, textY, ChatFormatting.BLUE.getColor(), true);

        guiGraphics.pose().popPose();
    }
    
    /**
     * 绘制伤害提升文本
     * @param guiGraphics GUI绘制上下文
     */
    private void drawDamageBonusText(GuiGraphics guiGraphics) {
        // 如果伤害提升大于0，才绘制文本
        if (damageBonusPercentage > 0.01f) {
            Minecraft mc = Minecraft.getInstance();
            Window window = mc.getWindow();
            int screenWidth = window.getGuiScaledWidth();
            int screenHeight = window.getGuiScaledHeight();
            
            // 计算饥饿值HUD的位置（在屏幕底部中心偏左）
            int foodBarX = screenWidth / 2 - 91;
            int foodBarY = screenHeight - 39;
            
            // 在饥饿值上方绘制伤害提升文本
            int textX = foodBarX;
            int textY = foodBarY - 15;
            
            // 格式化伤害提升文本，显示为百分比
            String damageBonusText = String.format("伤害提升: +%.0f%%", damageBonusPercentage * 100);
            
            // 绘制文本（绿色表示增益效果）
            guiGraphics.drawString(mc.font, damageBonusText, textX, textY, ChatFormatting.GREEN.getColor(), true);
        }
    }

    // ========== 绘制护盾冷却时间条 ==========
    private void drawShieldCooldown(GuiGraphics guiGraphics, int x, int y) {
        // 当有冷却时间且护盾值不满时才绘制冷却条
        if (isInCooldown && cooldownDuration > 0 && currentShield < maxShield) {
            // 冷却时间HUD位置：调整位置，确保在护盾HUD下方且可见，增加间距到5像素
            int cooldownY = y + BAR_HEIGHT + 5;

            // 绘制冷却时间背景条
            guiGraphics.blit(SHIELD_COOLDOWN_ICON, x, cooldownY, 0, 0, BAR_WIDTH, COOLDOWN_BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

            // 计算当前冷却进度
            long elapsedTime = System.currentTimeMillis() - cooldownStartTime;
            float progress = Math.min(1.0f, elapsedTime / (float) (cooldownDuration * 50)); // 1刻 = 50毫秒
            
            // 绘制冷却时间填充条（根据当前冷却进度比例）
            int fillWidth = (int) (progress * BAR_WIDTH);
            if (fillWidth > 0) {
                guiGraphics.blit(SHIELD_COOLDOWN_ICON, x, cooldownY, 0, COOLDOWN_BAR_HEIGHT, fillWidth, COOLDOWN_BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            }
            
            // 如果冷却完成，更新状态
            if (progress >= 1.0f) {
                isInCooldown = false;
                cooldownStartTime = 0;
                cooldownDuration = 0;
            }
        }
    }

    // ========== 粒子效果已迁移到 ShieldParticleGenerator 类 ==========
    
    // ========== 重置护盾值和冷却时间（可选） ==========
    public void reset() {
        this.currentShield = 0;
        this.maxShield = 0;
        this.cooldownStartTime = 0;
        this.cooldownDuration = 0;
        this.isInCooldown = false;
        this.targetCurrentShield = 0;
        this.targetMaxShield = 0;
        this.damageBonusPercentage = 0.0f;
        this.targetDamageBonus = 0.0f;
    }
}