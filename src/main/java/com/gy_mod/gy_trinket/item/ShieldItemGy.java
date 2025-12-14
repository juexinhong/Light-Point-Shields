package com.gy_mod.gy_trinket.item;

import com.gy_mod.gy_trinket.Config;
import com.gy_mod.gy_trinket.shield.effect.ShieldEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 自定义护盾物品基类
 * 用于添加自定义的物品描述，显示配置文件中的护盾参数
 */
public abstract class ShieldItemGy extends Item {
    // 护盾效果实例
    private final ShieldEffect shieldEffect;

    
    // 护盾的基础属性，可以在创建不同类型护盾时设置不同的默认值
    private final float baseMaxShield;
    private final int baseRebuildWaitTime;
    private final double baseNormalRegenPercentage;
    private final int baseNormalRegenInterval;
    private final int baseInvulnerabilityDuration;
    
    /**
     * 完整构造函数，用于创建不同类型的护盾，设置不同的基础属性和效果
     * @param properties 物品属性
     * @param baseMaxShield 基础最大护盾值
     * @param baseRebuildWaitTime 基础重构等待时间（刻）
     * @param baseNormalRegenPercentage 基础自然恢复百分比
     * @param baseNormalRegenInterval 基础自然恢复间隔（刻）
     * @param baseInvulnerabilityDuration 基础无敌持续时间（刻）
     * @param shieldEffect 护盾效果实例
     */
    public ShieldItemGy(Properties properties, 
                       float baseMaxShield,
                       int baseRebuildWaitTime,
                       double baseNormalRegenPercentage,
                       int baseNormalRegenInterval,
                       int baseInvulnerabilityDuration,
                       ShieldEffect shieldEffect) {
        super(properties);
        this.baseMaxShield = baseMaxShield;
        this.baseRebuildWaitTime = baseRebuildWaitTime;
        this.baseNormalRegenPercentage = baseNormalRegenPercentage;
        this.baseNormalRegenInterval = baseNormalRegenInterval;
        this.baseInvulnerabilityDuration = baseInvulnerabilityDuration;
        this.shieldEffect = shieldEffect;
    }
    
    /**
     * 简化构造函数，用于创建默认类型的护盾
     * @param properties 物品属性
     */
    public ShieldItemGy(Properties properties, ShieldEffect shieldEffect) {
        this(properties,
             20.0f,    // 基础最大护盾值
             130,      // 基础重构等待时间（刻）
             0.1,      // 基础自然恢复百分比
             20,       // 基础自然恢复间隔（刻）
             10,       // 基础无敌持续时间（刻）
             shieldEffect);
    }
    
    /**
     * 获取护盾的最大护盾值
     * @return 最大护盾值
     */
    public float getMaxShield() {
        String itemId = ForgeRegistries.ITEMS.getKey(this).toString();
        return Config.getShieldConfigOrDefault(itemId).maxShield;
    }
    
    /**
     * 获取护盾的重构等待时间
     * @return 重构等待时间（刻）
     */
    public int getRebuildWaitTime() {
        String itemId = ForgeRegistries.ITEMS.getKey(this).toString();
        return Config.getShieldConfigOrDefault(itemId).shieldRebuildWaitTime;
    }
    
    /**
     * 获取护盾的自然恢复百分比
     * @return 自然恢复百分比
     */
    public double getNormalRegenPercentage() {
        String itemId = ForgeRegistries.ITEMS.getKey(this).toString();
        return Config.getShieldConfigOrDefault(itemId).shieldNormalRegenPercentage;
    }
    
    /**
     * 获取护盾的自然恢复间隔
     * @return 自然恢复间隔（刻）
     */
    public int getNormalRegenInterval() {
        String itemId = ForgeRegistries.ITEMS.getKey(this).toString();
        return Config.getShieldConfigOrDefault(itemId).shieldNormalRegenInterval;
    }
    
    /**
     * 获取护盾的无敌持续时间
     * @return 无敌持续时间（刻）
     */
    public int getInvulnerabilityDuration() {
        String itemId = ForgeRegistries.ITEMS.getKey(this).toString();
        return Config.getShieldConfigOrDefault(itemId).shieldInvulnerabilityDuration;
    }
    
    /**
     * 添加物品描述
     * @param stack 物品栈
     * @param level 世界
     * @param components 描述组件列表
     * @param flag 提示标志
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
        super.appendHoverText(stack, level, components, flag);
        
        // 添加护盾参数描述，显示当前护盾类型的配置值
        components.add(Component.translatable("tooltip.gy_trinket.shield.max_shield", getMaxShield()));
        components.add(Component.translatable("tooltip.gy_trinket.shield.cooldown_time", getRebuildWaitTime() / 20.0));
        components.add(Component.translatable("tooltip.gy_trinket.shield.regen_percentage", getNormalRegenPercentage()));
        components.add(Component.translatable("tooltip.gy_trinket.shield.regen_interval", getNormalRegenInterval() / 20.0));
        components.add(Component.translatable("tooltip.gy_trinket.shield.invulnerability_duration", getInvulnerabilityDuration() / 20.0));
        
        // 添加护盾类型的特殊描述
        addSpecialDescription(components);
    }
    
    /**
     * 添加护盾类型的特殊描述
     * 子类可以重写此方法来添加自己的特殊描述
     * @param components 描述组件列表
     */
    protected abstract void addSpecialDescription(List<Component> components);
    
    /**
     * 获取当前护盾的特殊效果
     * @return 护盾效果实例
     */
    public ShieldEffect getShieldEffect() {
        return shieldEffect;
    }
}