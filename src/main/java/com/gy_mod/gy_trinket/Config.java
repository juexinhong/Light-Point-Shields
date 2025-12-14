package com.gy_mod.gy_trinket;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 配置文件类 - 用于管理模组的所有可配置参数
// 演示如何使用 Forge 的配置 API
@Mod.EventBusSubscriber(modid = gy_trinket.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    // 创建配置构建器
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 示例配置项：是否记录泥土方块
    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("是否在通用设置中记录泥土方块")
            .define("logDirtBlock", true);

    // 示例配置项：魔法数字
    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("一个魔法数字")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    // 示例配置项：魔法数字介绍信息
    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("魔法数字的介绍信息")
            .define("magicNumberIntroduction", "The magic number is... ");

    // 示例配置项：物品列表
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("在通用设置中记录的物品列表")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // ========== 护盾配置 ==========
    // 默认护盾配置（用于向后兼容）
    private static final ForgeConfigSpec.IntValue DEFAULT_MAX_SHIELD = BUILDER
            .comment("默认护盾的最大护盾值")
            .defineInRange("shield.gy_trinket.shield_gy.maxShield", 10, 0, 1000);

    // 默认护盾冷却时间（以刻为单位，20刻=1秒）
    private static final ForgeConfigSpec.IntValue DEFAULT_SHIELD_REBUILD_WAIT_TIME = BUILDER
            .comment("默认护盾重构机制的冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_gy.rebuildWaitTime", 130, 0, 10000);

    // 默认护盾重构持续时间（以刻为单位）
    private static final ForgeConfigSpec.IntValue DEFAULT_SHIELD_REBUILD_DURATION = BUILDER
            .comment("默认护盾重构持续时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_gy.rebuildDuration", 10, 1, 1000);

    // 默认护盾最大冷却时间（以刻为单位）
    private static final ForgeConfigSpec.IntValue DEFAULT_SHIELD_MAX_WAIT_TIME = BUILDER
            .comment("默认护盾冷却时间的最大延长值（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_gy.maxWaitTime", 130, 0, 10000);

    // 默认护盾有剩余值时的每秒恢复百分比
    private static final ForgeConfigSpec.DoubleValue DEFAULT_SHIELD_NORMAL_REGEN_PERCENTAGE = BUILDER
            .comment("默认护盾有剩余值时，每秒恢复的护盾值百分比")
            .defineInRange("shield.gy_trinket.shield_gy.normalRegenPercentage", 0.0, 0.0, 100.0);
    
    // 默认护盾自然恢复的时间间隔（以刻为单位）
    private static final ForgeConfigSpec.IntValue DEFAULT_SHIELD_NORMAL_REGEN_INTERVAL = BUILDER
            .comment("默认护盾自然恢复效果的时间间隔（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_gy.normalRegenInterval", 3, 1, 1000);
    
    // 默认护盾受到伤害时延长的冷却时间（以刻为单位）
    private static final ForgeConfigSpec.IntValue DEFAULT_SHIELD_WAIT_TIME_EXTENSION_ON_HURT = BUILDER
            .comment("默认护盾冷却期间受到伤害时，延长的冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_gy.waitTimeExtensionOnHurt", 30, 0, 10000);

    // 默认护盾每点伤害提升的冷却延长百分比（倍数值）
    private static final ForgeConfigSpec.DoubleValue DEFAULT_SHIELD_COOLDOWN_EXTENSION_MULTIPLIER = BUILDER
            .comment("默认护盾冷却期间受到伤害时，每点伤害提升的冷却延长倍数值")
            .defineInRange("shield.gy_trinket.shield_gy.cooldownExtensionMultiplier", 1.1, 1.0, 5.0);

    // 默认护盾抵挡伤害后的无敌时间（以刻为单位）
    private static final ForgeConfigSpec.IntValue DEFAULT_SHIELD_INVULNERABILITY_DURATION = BUILDER
            .comment("默认护盾抵挡伤害后，玩家获得的短暂无敌时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_gy.invulnerabilityDuration", 5, 0, 1000);

    // 默认护盾值为零时的冷却延迟触发内置冷却时间（以刻为单位）
    private static final ForgeConfigSpec.IntValue DEFAULT_SHIELD_ZERO_SHIELD_COOLDOWN_DELAY = BUILDER
            .comment("默认护盾值为零时，触发冷却时间延迟的内置冷却时间（单位：刻）[为什么要有?可以试试护盾无敌时间为零时踩火上]")
            .defineInRange("shield.gy_trinket.shield_gy.zeroShieldCooldownDelay", 5, 0, 1000);

    // ========== 反射护盾配置 ==========
    private static final ForgeConfigSpec.IntValue REINFORCED_MAX_SHIELD = BUILDER
            .comment("反射护盾的最大护盾值")
            .defineInRange("shield.gy_trinket.shield_reflect.maxShield", 20, 0, 1000);
    
    // 反射护盾的反射消耗修正系数
    private static final ForgeConfigSpec.DoubleValue REFLECT_SHIELD_COST = BUILDER
            .comment("反射护盾反射弹射物时消耗的护盾值修正系数 - 作为乘数应用于实际伤害值")
            .defineInRange("shield.gy_trinket.shield_reflect.reflectCost", 0.7, 0.0, 100.0);
    
    // 反射护盾的爆炸范围
    private static final ForgeConfigSpec.DoubleValue REFLECT_SHIELD_EXPLOSION_RADIUS = BUILDER
            .comment("反射护盾生成的烈焰弹爆炸范围")
            .defineInRange("shield.gy_trinket.shield_reflect.explosionRadius", 1.0, 0.0, 20.0);
    
    // 反射护盾的伤害修正系数
    private static final ForgeConfigSpec.DoubleValue REFLECT_SHIELD_DAMAGE_MODIFIER = BUILDER
            .comment("反射伤害修正 - 作为乘数应用于反射后的烈焰弹伤害")
            .defineInRange("shield.gy_trinket.shield_reflect.damageModifier", 1.0, 0.0, 100.0);
    
    // 反射护盾的速度修正系数
    private static final ForgeConfigSpec.DoubleValue REFLECT_SHIELD_SPEED_MODIFIER = BUILDER
            .comment("反射速度修正 - 作为乘数应用于反射后的烈焰弹速度")
            .defineInRange("shield.gy_trinket.shield_reflect.speedModifier", 0.3, 0.0, 100.0);

    private static final ForgeConfigSpec.IntValue REINFORCED_SHIELD_REBUILD_WAIT_TIME = BUILDER
            .comment("反射护盾重构机制的冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_reflect.rebuildWaitTime", 260, 0, 10000);

    private static final ForgeConfigSpec.IntValue REINFORCED_SHIELD_REBUILD_DURATION = BUILDER
            .comment("反射护盾重构持续时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_reflect.rebuildDuration", 10, 1, 1000);

    private static final ForgeConfigSpec.IntValue REINFORCED_SHIELD_MAX_WAIT_TIME = BUILDER
            .comment("反射护盾冷却时间的最大延长值（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_reflect.maxWaitTime", 400, 0, 10000);

    private static final ForgeConfigSpec.DoubleValue REINFORCED_SHIELD_NORMAL_REGEN_PERCENTAGE = BUILDER
            .comment("反射护盾有剩余值时，每秒恢复的护盾值百分比")
            .defineInRange("shield.gy_trinket.shield_reflect.normalRegenPercentage", 0.15, 0.0, 100.0);
    
    private static final ForgeConfigSpec.IntValue REINFORCED_SHIELD_NORMAL_REGEN_INTERVAL = BUILDER
            .comment("反射护盾自然恢复效果的时间间隔（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_reflect.normalRegenInterval", 3, 1, 1000);
    
    private static final ForgeConfigSpec.IntValue REINFORCED_SHIELD_WAIT_TIME_EXTENSION_ON_HURT = BUILDER
            .comment("反射护盾冷却期间受到伤害时，延长的冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_reflect.waitTimeExtensionOnHurt", 7, 0, 10000);

    private static final ForgeConfigSpec.DoubleValue REINFORCED_SHIELD_COOLDOWN_EXTENSION_MULTIPLIER = BUILDER
            .comment("反射护盾冷却期间受到伤害时，每点伤害提升的冷却延长倍数值")
            .defineInRange("shield.gy_trinket.shield_reflect.cooldownExtensionMultiplier", 3.0, 1.0, 5.0);

    private static final ForgeConfigSpec.IntValue REINFORCED_SHIELD_INVULNERABILITY_DURATION = BUILDER
            .comment("反射护盾抵挡伤害后，玩家获得的短暂无敌时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_reflect.invulnerabilityDuration", 5, 0, 1000);

    private static final ForgeConfigSpec.IntValue REINFORCED_SHIELD_ZERO_SHIELD_COOLDOWN_DELAY = BUILDER
            .comment("反射护盾值为零时，触发冷却时间延迟的内置冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_reflect.zeroShieldCooldownDelay", 5, 0, 1000);

    // ========== 火环护盾配置 ==========
    private static final ForgeConfigSpec.IntValue FIRE_RING_MAX_SHIELD = BUILDER
            .comment("火环护盾的最大护盾值")
            .defineInRange("shield.gy_trinket.shield_fire_ring.maxShield", 15, 0, 1000);

    private static final ForgeConfigSpec.IntValue FIRE_RING_SHIELD_REBUILD_WAIT_TIME = BUILDER
            .comment("火环护盾重构机制的冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_fire_ring.rebuildWaitTime", 120, 0, 10000);

    private static final ForgeConfigSpec.IntValue FIRE_RING_SHIELD_REBUILD_DURATION = BUILDER
            .comment("火环护盾重构持续时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_fire_ring.rebuildDuration", 5, 1, 1000);

    private static final ForgeConfigSpec.IntValue FIRE_RING_SHIELD_MAX_WAIT_TIME = BUILDER
            .comment("火环护盾冷却时间的最大延长值（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_fire_ring.maxWaitTime", 120, 0, 10000);

    private static final ForgeConfigSpec.DoubleValue FIRE_RING_SHIELD_NORMAL_REGEN_PERCENTAGE = BUILDER
            .comment("火环护盾有剩余值时，每秒恢复的护盾值百分比")
            .defineInRange("shield.gy_trinket.shield_fire_ring.normalRegenPercentage", 0.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.IntValue FIRE_RING_SHIELD_NORMAL_REGEN_INTERVAL = BUILDER
            .comment("火环护盾自然恢复效果的时间间隔（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_fire_ring.normalRegenInterval", 3, 1, 1000);
    
    private static final ForgeConfigSpec.IntValue FIRE_RING_SHIELD_WAIT_TIME_EXTENSION_ON_HURT = BUILDER
            .comment("火环护盾冷却期间受到伤害时，延长的冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_fire_ring.waitTimeExtensionOnHurt", 20, 0, 10000);

    private static final ForgeConfigSpec.DoubleValue FIRE_RING_SHIELD_COOLDOWN_EXTENSION_MULTIPLIER = BUILDER
            .comment("火环护盾冷却期间受到伤害时，每点伤害提升的冷却延长倍数值")
            .defineInRange("shield.gy_trinket.shield_fire_ring.cooldownExtensionMultiplier", 1.1, 1.0, 5.0);

    private static final ForgeConfigSpec.IntValue FIRE_RING_SHIELD_INVULNERABILITY_DURATION = BUILDER
            .comment("火环护盾抵挡伤害后，玩家获得的短暂无敌时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_fire_ring.invulnerabilityDuration", 5, 0, 1000);

    private static final ForgeConfigSpec.IntValue FIRE_RING_SHIELD_ZERO_SHIELD_COOLDOWN_DELAY = BUILDER
            .comment("火环护盾值为零时，触发冷却时间延迟的内置冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_fire_ring.zeroShieldCooldownDelay", 10, 0, 1000);

    // ========== 增幅护盾配置 ==========
    private static final ForgeConfigSpec.IntValue AMPLIFIER_MAX_SHIELD = BUILDER
            .comment("增幅护盾的最大护盾值,别问为啥跟新星漂移的不一样,我早觉得增幅盾不好用了,但他又只有这个加伤害明显,敢信贴脸3倍增伤的救赎感吗,然后我盾就爆了,被撞的,所以这个缺点被保留下来了,不能只有我吃别人也要吃")
            .defineInRange("shield.gy_trinket.shield_amplifier.maxShield", 20, 0, 1000);

    private static final ForgeConfigSpec.IntValue AMPLIFIER_SHIELD_REBUILD_WAIT_TIME = BUILDER
            .comment("增幅护盾重构机制的冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_amplifier.rebuildWaitTime", 110, 0, 10000);

    private static final ForgeConfigSpec.IntValue AMPLIFIER_SHIELD_REBUILD_DURATION = BUILDER
            .comment("增幅护盾重构持续时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_amplifier.rebuildDuration", 10, 1, 1000);

    private static final ForgeConfigSpec.IntValue AMPLIFIER_SHIELD_MAX_WAIT_TIME = BUILDER
            .comment("增幅护盾冷却时间的最大延长值（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_amplifier.maxWaitTime", 110, 0, 10000);

    private static final ForgeConfigSpec.DoubleValue AMPLIFIER_SHIELD_NORMAL_REGEN_PERCENTAGE = BUILDER
            .comment("增幅护盾有剩余值时，每秒恢复的护盾值百分比")
            .defineInRange("shield.gy_trinket.shield_amplifier.normalRegenPercentage", 0.0, 0.0, 100.0);
    
    private static final ForgeConfigSpec.IntValue AMPLIFIER_SHIELD_NORMAL_REGEN_INTERVAL = BUILDER
            .comment("增幅护盾自然恢复效果的时间间隔（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_amplifier.normalRegenInterval", 3, 1, 1000);
    
    private static final ForgeConfigSpec.IntValue AMPLIFIER_SHIELD_WAIT_TIME_EXTENSION_ON_HURT = BUILDER
            .comment("增幅护盾冷却期间受到伤害时，延长的冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_amplifier.waitTimeExtensionOnHurt", 10, 0, 10000);

    private static final ForgeConfigSpec.DoubleValue AMPLIFIER_SHIELD_COOLDOWN_EXTENSION_MULTIPLIER = BUILDER
            .comment("增幅护盾冷却期间受到伤害时，每点伤害提升的冷却延长倍数值")
            .defineInRange("shield.gy_trinket.shield_amplifier.cooldownExtensionMultiplier", 1.1, 1.0, 5.0);

    private static final ForgeConfigSpec.IntValue AMPLIFIER_SHIELD_INVULNERABILITY_DURATION = BUILDER
            .comment("增幅护盾抵挡伤害后，玩家获得的短暂无敌时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_amplifier.invulnerabilityDuration", 5, 0, 1000);

    private static final ForgeConfigSpec.IntValue AMPLIFIER_SHIELD_ZERO_SHIELD_COOLDOWN_DELAY = BUILDER
            .comment("增幅护盾值为零时，触发冷却时间延迟的内置冷却时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_amplifier.zeroShieldCooldownDelay", 10, 0, 1000);

    // ========== 增幅护盾特效配置 ==========
    private static final ForgeConfigSpec.DoubleValue AMPLIFIER_SHIELD_DAMAGE_BONUS_PER_SHIELD = BUILDER
            .comment("每一点损失的护盾值转换的伤害提升效果的数值（%）,伤害提升效果存在3秒,不可调,因为调差了可能会把游戏干崩溃,或是极大影响伤害,毕竟是乘算多了会指数爆炸")
            .defineInRange("shield.gy_trinket.shield_amplifier.damageBonusPerShield", 6.0, 0.0, 100.0);

    private static final ForgeConfigSpec.IntValue AMPLIFIER_SHIELD_DAMAGE_BONUS_FREQUENCY = BUILDER
            .comment("持续消耗护盾值的频率（单位：刻）,这个与伤害提升计算频率共用的哦,低了会导致有时候只吃到一段增幅")
            .defineInRange("shield.gy_trinket.shield_amplifier.damageBonusFrequency", 2, 1, 100);

    private static final ForgeConfigSpec.DoubleValue AMPLIFIER_SHIELD_DAMAGE_BONUS_SHIELD_COST = BUILDER
            .comment("持续消耗护盾值的百分比（%）")
            .defineInRange("shield.gy_trinket.shield_amplifier.damageBonusShieldCost", 1.0, 0.0, 100.0);

    // ========== 火环特效配置 ==========
    private static final ForgeConfigSpec.IntValue FIRE_RING_TRIGGER_FREQUENCY = BUILDER
            .comment("火环触发频率（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_fire_ring.fireRingTriggerFrequency", 3, 1, 1000);

    private static final ForgeConfigSpec.DoubleValue FIRE_RING_DAMAGE = BUILDER
            .comment("火环基础伤害值")
            .defineInRange("shield.gy_trinket.shield_fire_ring.fireRingDamage", 0.3, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue FIRE_RING_RADIUS = BUILDER
            .comment("火环生效半径（同时控制粒子生成半径）")
            .defineInRange("shield.gy_trinket.shield_fire_ring.fireRingRadius", 3.0, 0.5, 10.0);

    private static final ForgeConfigSpec.IntValue FIRE_RING_PLAYER_ATTACK_EXEMPTION_TIME = BUILDER
            .comment("玩家攻击豁免对象受到火环攻击的时间（单位：刻）")
            .defineInRange("shield.gy_trinket.shield_fire_ring.fireRingPlayerAttackExemptionTime", 12, 0, 1000);

    private static final ForgeConfigSpec.DoubleValue FIRE_RING_SHIELD_COST = BUILDER
            .comment("火环对自身护盾值造成的损耗数值")
            .defineInRange("shield.gy_trinket.shield_fire_ring.fireRingShieldCost", 0.05, 0.0, 100.0);

    // 构建配置规范
    static final ForgeConfigSpec SPEC = BUILDER.build();

    // ========== 配置值变量 ==========
    // 示例配置值
    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    // 护盾配置值类，用于存储单种护盾的所有配置参数
    public static class ShieldConfig {
        public int maxShield;            // 最大护盾值
        public int shieldRebuildWaitTime; // 护盾重构触发等待时间（刻）
        public int shieldRebuildDuration; // 护盾重构持续时间（刻，从0到满所需时间）
        public int shieldMaxWaitTime;    // 护盾重构最大等待时间（刻）
        public int shieldWaitTimeExtensionOnHurt; // 受到伤害时延长的等待时间（刻）
        public double shieldCooldownExtensionMultiplier; // 每点伤害提升的冷却延长倍数值
        public double shieldNormalRegenPercentage; // 护盾有剩余值时的每秒恢复百分比
        public int shieldNormalRegenInterval; // 护盾自然恢复的时间间隔（刻）
        public int shieldInvulnerabilityDuration; // 护盾抵挡伤害后的无敌时间（刻）
        public int shieldZeroShieldCooldownDelay; // 护盾值为零时的冷却触发内置冷却时间（刻）
        
        // 火环相关配置
        public int fireRingTriggerFrequency; // 火环触发频率（单位：刻）
        public double fireRingDamage; // 火环基础伤害值
        public double fireRingRadius; // 火环生效半径（同时控制粒子生成半径）
        public int fireRingPlayerAttackExemptionTime; // 玩家攻击豁免对象受到火环攻击的时间（单位：刻）
        public double fireRingShieldCost; // 火环对自身护盾值造成的损耗数值
        
        // 增幅相关配置
        public double amplifierDamageBonusPerShield; // 每一点损失的护盾值转换的伤害提升效果的数值（%）
        public int amplifierDamageBonusFrequency; // 持续消耗护盾值的频率（单位：刻）
        public double amplifierDamageBonusShieldCost; // 持续消耗护盾值的百分比（%）
        
        // 默认构造函数
        public ShieldConfig() {
            this.maxShield = 20;
            this.shieldRebuildWaitTime = 130;
            this.shieldRebuildDuration = 10;
            this.shieldMaxWaitTime = 130;
            this.shieldWaitTimeExtensionOnHurt = 40;
            this.shieldCooldownExtensionMultiplier = 1.1;
            this.shieldNormalRegenPercentage = 0.1;
            this.shieldNormalRegenInterval = 3;
            this.shieldInvulnerabilityDuration = 5;
            this.shieldZeroShieldCooldownDelay = 10;
            
            // 火环默认配置
            this.fireRingTriggerFrequency = 10;
            this.fireRingDamage = 0.3;
            this.fireRingRadius = 3.0; // 默认火环半径
            this.fireRingPlayerAttackExemptionTime = 20; // 20刻 = 1秒
            this.fireRingShieldCost = 0.1;
            
            // 增幅默认配置
            this.amplifierDamageBonusPerShield = 6.0; // 默认1损失盾值转化6%伤害提升
            this.amplifierDamageBonusFrequency = 2; // 默认每2tick执行一次
            this.amplifierDamageBonusShieldCost = 1.0; // 默认1%
        }
        
        // 完整构造函数
        public ShieldConfig(int maxShield, int shieldRebuildWaitTime, int shieldRebuildDuration,
                          int shieldMaxWaitTime, int shieldWaitTimeExtensionOnHurt,
                          double shieldCooldownExtensionMultiplier, double shieldNormalRegenPercentage,
                          int shieldNormalRegenInterval, int shieldInvulnerabilityDuration,
                          int shieldZeroShieldCooldownDelay,
                          int fireRingTriggerFrequency, double fireRingDamage, double fireRingRadius,
                          int fireRingPlayerAttackExemptionTime, double fireRingShieldCost,
                          double amplifierDamageBonusPerShield, int amplifierDamageBonusFrequency, double amplifierDamageBonusShieldCost) {
            this.maxShield = maxShield;
            this.shieldRebuildWaitTime = shieldRebuildWaitTime;
            this.shieldRebuildDuration = shieldRebuildDuration;
            this.shieldMaxWaitTime = shieldMaxWaitTime;
            this.shieldWaitTimeExtensionOnHurt = shieldWaitTimeExtensionOnHurt;
            this.shieldCooldownExtensionMultiplier = shieldCooldownExtensionMultiplier;
            this.shieldNormalRegenPercentage = shieldNormalRegenPercentage;
            this.shieldNormalRegenInterval = shieldNormalRegenInterval;
            this.shieldInvulnerabilityDuration = shieldInvulnerabilityDuration;
            this.shieldZeroShieldCooldownDelay = shieldZeroShieldCooldownDelay;
            
            // 火环默认配置
            this.fireRingTriggerFrequency = fireRingTriggerFrequency;
            this.fireRingDamage = fireRingDamage;
            this.fireRingRadius = fireRingRadius;
            this.fireRingPlayerAttackExemptionTime = fireRingPlayerAttackExemptionTime;
            this.fireRingShieldCost = fireRingShieldCost;
            
            // 增幅默认配置
            this.amplifierDamageBonusPerShield = amplifierDamageBonusPerShield;
            this.amplifierDamageBonusFrequency = amplifierDamageBonusFrequency;
            this.amplifierDamageBonusShieldCost = amplifierDamageBonusShieldCost;
        }
    }
    
    // 存储所有护盾类型的配置
    public static final Map<String, ShieldConfig> SHIELD_CONFIGS = new HashMap<>();
    
    // 向后兼容的全局配置值（保留这些值以便现有代码继续工作）
    public static int maxShield;            // 最大护盾值
    public static int shieldRebuildWaitTime; // 护盾重构触发等待时间（刻）
    public static int shieldRebuildDuration; // 护盾重构持续时间（刻，从0到满所需时间）
    public static int shieldMaxWaitTime;    // 护盾重构最大等待时间（刻）
    public static int shieldWaitTimeExtensionOnHurt; // 受到伤害时延长的等待时间（刻）
    public static double shieldCooldownExtensionMultiplier; // 每点伤害提升的冷却延长倍数值
    public static double shieldNormalRegenPercentage; // 护盾有剩余值时的每秒恢复百分比
    public static int shieldNormalRegenInterval; // 护盾自然恢复的时间间隔（刻）
    public static int shieldInvulnerabilityDuration; // 护盾抵挡伤害后的无敌时间（刻）
    public static int shieldZeroShieldCooldownDelay; // 护盾值为零时的冷却触发内置冷却时间（刻）
    public static double reflectShieldCost; // 反射护盾反射弹射物时消耗的护盾值
    public static double reflectShieldExplosionRadius; // 反射烈焰弹的爆炸范围
    public static double reflectShieldDamageModifier; // 反射伤害修正系数
    public static double reflectShieldSpeedModifier; // 反射速度修正系数

    // 验证物品名称是否有效
    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }

    // 配置加载事件
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        loadConfig();
    }
    
    // 配置重载事件 - 支持游戏运行时修改配置文件
    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        loadConfig();
        gy_trinket.LOGGER.info("[" + gy_trinket.MOD_ID + "] 配置文件已重新加载");
        
        // 通知ShieldEvent配置已重载，确保所有玩家的护盾状态更新到新配置
        com.gy_mod.gy_trinket.event.ShieldEvent.onConfigReload(event);
    }
    
    // 统一加载配置的方法
    private static void loadConfig() {
        // 加载示例配置
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // 将字符串列表转换为物品集合
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemName)))
                .collect(Collectors.toSet());

        // 清空护盾配置
        SHIELD_CONFIGS.clear();
        
        // 加载默认护盾配置（gy_trinket:shield_gy）
        ShieldConfig defaultConfig = new ShieldConfig(
                DEFAULT_MAX_SHIELD.get(),
                DEFAULT_SHIELD_REBUILD_WAIT_TIME.get(),
                DEFAULT_SHIELD_REBUILD_DURATION.get(),
                DEFAULT_SHIELD_MAX_WAIT_TIME.get(),
                DEFAULT_SHIELD_WAIT_TIME_EXTENSION_ON_HURT.get(),
                DEFAULT_SHIELD_COOLDOWN_EXTENSION_MULTIPLIER.get(),
                DEFAULT_SHIELD_NORMAL_REGEN_PERCENTAGE.get(),
                DEFAULT_SHIELD_NORMAL_REGEN_INTERVAL.get(),
                DEFAULT_SHIELD_INVULNERABILITY_DURATION.get(),
                DEFAULT_SHIELD_ZERO_SHIELD_COOLDOWN_DELAY.get(),
                10, // 默认火环触发频率 (不使用)
                0.3, // 默认火环伤害 (不使用)
                3.0, // 默认火环半径 (不使用)
                20, // 默认玩家攻击豁免时间 (不使用)
                0.1, // 默认护盾损耗 (不使用)
                0.0, // 默认无伤害加成
                20, // 默认极低频率
                0.0 // 默认无护盾消耗
        );
        
        // 存储默认护盾配置
        SHIELD_CONFIGS.put("gy_trinket:shield_gy", defaultConfig);
        
        // 加载反射护盾配置（gy_trinket:shield_reflect）
        ShieldConfig reinforcedConfig = new ShieldConfig(
                REINFORCED_MAX_SHIELD.get(),
                REINFORCED_SHIELD_REBUILD_WAIT_TIME.get(),
                REINFORCED_SHIELD_REBUILD_DURATION.get(),
                REINFORCED_SHIELD_MAX_WAIT_TIME.get(),
                REINFORCED_SHIELD_WAIT_TIME_EXTENSION_ON_HURT.get(),
                REINFORCED_SHIELD_COOLDOWN_EXTENSION_MULTIPLIER.get(),
                REINFORCED_SHIELD_NORMAL_REGEN_PERCENTAGE.get(),
                REINFORCED_SHIELD_NORMAL_REGEN_INTERVAL.get(),
                REINFORCED_SHIELD_INVULNERABILITY_DURATION.get(),
                REINFORCED_SHIELD_ZERO_SHIELD_COOLDOWN_DELAY.get(),
                10, // 默认火环触发频率 (不使用)
                0.3, // 默认火环伤害 (不使用)
                3.0, // 默认火环半径 (不使用)
                20, // 默认玩家攻击豁免时间 (不使用)
                0.1, // 默认护盾损耗 (不使用)
                0.0, // 默认无伤害加成
                20, // 默认极低频率
                0.0 // 默认无护盾消耗
        );
        
        // 存储强化护盾配置
        SHIELD_CONFIGS.put("gy_trinket:shield_reflect", reinforcedConfig);
        
        // 加载火环护盾配置（gy_trinket:shield_fire_ring）
        ShieldConfig fireRingConfig = new ShieldConfig(
                FIRE_RING_MAX_SHIELD.get(),
                FIRE_RING_SHIELD_REBUILD_WAIT_TIME.get(),
                FIRE_RING_SHIELD_REBUILD_DURATION.get(),
                FIRE_RING_SHIELD_MAX_WAIT_TIME.get(),
                FIRE_RING_SHIELD_WAIT_TIME_EXTENSION_ON_HURT.get(),
                FIRE_RING_SHIELD_COOLDOWN_EXTENSION_MULTIPLIER.get(),
                FIRE_RING_SHIELD_NORMAL_REGEN_PERCENTAGE.get(),
                FIRE_RING_SHIELD_NORMAL_REGEN_INTERVAL.get(),
                FIRE_RING_SHIELD_INVULNERABILITY_DURATION.get(),
                FIRE_RING_SHIELD_ZERO_SHIELD_COOLDOWN_DELAY.get(),
                FIRE_RING_TRIGGER_FREQUENCY.get(),
                FIRE_RING_DAMAGE.get(),
                FIRE_RING_RADIUS.get(),
                FIRE_RING_PLAYER_ATTACK_EXEMPTION_TIME.get(),
                FIRE_RING_SHIELD_COST.get(),
                0.0, // 默认无伤害加成
                20, // 默认极低频率
                0.0 // 默认无护盾消耗
        );
        
        // 存储火环护盾配置
        SHIELD_CONFIGS.put("gy_trinket:shield_fire_ring", fireRingConfig);
        
        // 加载增幅护盾配置（gy_trinket:shield_amplifier）
        ShieldConfig amplifierConfig = new ShieldConfig(
                AMPLIFIER_MAX_SHIELD.get(),
                AMPLIFIER_SHIELD_REBUILD_WAIT_TIME.get(),
                AMPLIFIER_SHIELD_REBUILD_DURATION.get(),
                AMPLIFIER_SHIELD_MAX_WAIT_TIME.get(),
                AMPLIFIER_SHIELD_WAIT_TIME_EXTENSION_ON_HURT.get(),
                AMPLIFIER_SHIELD_COOLDOWN_EXTENSION_MULTIPLIER.get(),
                AMPLIFIER_SHIELD_NORMAL_REGEN_PERCENTAGE.get(),
                AMPLIFIER_SHIELD_NORMAL_REGEN_INTERVAL.get(),
                AMPLIFIER_SHIELD_INVULNERABILITY_DURATION.get(),
                AMPLIFIER_SHIELD_ZERO_SHIELD_COOLDOWN_DELAY.get(),
                10, // 默认火环触发频率 (不使用)
                0.3, // 默认火环伤害 (不使用)
                3.0, // 默认火环半径 (不使用)
                20, // 默认玩家攻击豁免时间 (不使用)
                0.1, // 默认护盾损耗 (不使用)
                // 增幅特效配置
                AMPLIFIER_SHIELD_DAMAGE_BONUS_PER_SHIELD.get(),
                AMPLIFIER_SHIELD_DAMAGE_BONUS_FREQUENCY.get(),
                AMPLIFIER_SHIELD_DAMAGE_BONUS_SHIELD_COST.get()
        );
        
        // 存储增幅护盾配置
        SHIELD_CONFIGS.put("gy_trinket:shield_amplifier", amplifierConfig);
        
        // 为向后兼容，设置全局配置值为默认护盾的配置
        maxShield = defaultConfig.maxShield;
        shieldRebuildWaitTime = defaultConfig.shieldRebuildWaitTime;
        shieldRebuildDuration = defaultConfig.shieldRebuildDuration;
        shieldMaxWaitTime = defaultConfig.shieldMaxWaitTime;
        shieldWaitTimeExtensionOnHurt = defaultConfig.shieldWaitTimeExtensionOnHurt;
        shieldCooldownExtensionMultiplier = defaultConfig.shieldCooldownExtensionMultiplier;
        shieldNormalRegenPercentage = defaultConfig.shieldNormalRegenPercentage;
        shieldNormalRegenInterval = defaultConfig.shieldNormalRegenInterval;
        shieldInvulnerabilityDuration = defaultConfig.shieldInvulnerabilityDuration;
        shieldZeroShieldCooldownDelay = defaultConfig.shieldZeroShieldCooldownDelay;
        reflectShieldCost = REFLECT_SHIELD_COST.get();
        reflectShieldExplosionRadius = REFLECT_SHIELD_EXPLOSION_RADIUS.get();
        reflectShieldDamageModifier = REFLECT_SHIELD_DAMAGE_MODIFIER.get();
        reflectShieldSpeedModifier = REFLECT_SHIELD_SPEED_MODIFIER.get();
    }
    
    // 获取特定护盾类型的配置
    public static ShieldConfig getShieldConfig(String shieldType) {
        return SHIELD_CONFIGS.getOrDefault(shieldType, new ShieldConfig());
    }
    
    // 获取特定护盾类型的配置，如果未找到则返回默认配置
    public static ShieldConfig getShieldConfigOrDefault(String shieldType) {
        ShieldConfig defaultConfig = SHIELD_CONFIGS.get("gy_trinket:shield_gy");
        if (defaultConfig == null) {
            // 如果默认配置也不存在，创建一个临时的默认配置
            defaultConfig = new ShieldConfig();
        }
        return SHIELD_CONFIGS.getOrDefault(shieldType, defaultConfig);
    }
}