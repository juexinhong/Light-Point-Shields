package com.gy_mod.gy_trinket.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gy_mod.gy_trinket.Config;
import com.gy_mod.gy_trinket.item.AmplifierShield;
import com.gy_mod.gy_trinket.item.ShieldItemGy;
import com.gy_mod.gy_trinket.shield.ShieldManager;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 增幅护盾事件处理类
 * 继承自基础护盾机制，实现增幅护盾的特殊效果
 */
public class AmplifierShieldEvent {
    // 日志记录器
    private static final Logger LOGGER = LogManager.getLogger(AmplifierShieldEvent.class);
    
    // 存储玩家损失的护盾值：UUID -> 损失的护盾值
    private static final Map<UUID, Float> AMPLIFIER_DAMAGE_STORAGE = new HashMap<>();
    
    // 记录消耗的护盾值的Map：UUID -> 消耗的护盾值
    private static final Map<UUID, Float> AMPLIFIER_SHIELD_CONSUMPTION = new HashMap<>();
    
    // 存储玩家的所有伤害加成：UUID -> List<DamageBonus>
    private static final Map<UUID, List<DamageBonus>> AMPLIFIER_DAMAGE_BONUSES = new HashMap<>();
    
    // 伤害加成持续时间（3秒 = 60tick）
    private static final int BONUS_DURATION_TICKS = 60;
    
    // 修饰符开放期（1秒 = 20tick）
    private static final int BONUS_OPEN_TICKS = 20;
    
    // 最大修饰符数量
    private static final int MAX_BONUSES = 5;
    
    /**
     * 伤害加成数据类
     */
    private static class DamageBonus {
        private final UUID uuid;
        private double amount;
        private int remainingTicks;
        private int remainingOpenTicks;
        private boolean isApplied;
        
        public DamageBonus(double amount) {
            this.uuid = UUID.randomUUID();
            this.amount = amount;
            this.remainingTicks = BONUS_DURATION_TICKS;
            this.remainingOpenTicks = BONUS_OPEN_TICKS;
            this.isApplied = false;
        }
        
        public UUID getUuid() {
            return uuid;
        }
        
        public double getAmount() {
            return amount;
        }
        
        public void addAmount(double additionalAmount) {
            this.amount += additionalAmount;
        }
        
        public int getRemainingTicks() {
            return remainingTicks;
        }
        
        public int getRemainingOpenTicks() {
            return remainingOpenTicks;
        }
        
        public boolean isOpen() {
            return remainingOpenTicks > 0;
        }
        
        public boolean isApplied() {
            return isApplied;
        }
        
        public void setApplied(boolean applied) {
            isApplied = applied;
        }
        
        public void decrementTicks() {
            remainingTicks--;
            if (isOpen()) {
                remainingOpenTicks--;
            }
        }
    }

    /**
     * 注册事件
     */
    public static void register() {
        // 注册增幅护盾特有的事件
        MinecraftForge.EVENT_BUS.register(new AmplifierShieldEvent());
    }
    
    /**
     * 监听玩家退出事件，清除玩家的所有伤害加成
     */
    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        // 在PlayerLoggedOutEvent中，玩家对象可以通过getEntity()方法获取
        Player player = (Player) event.getEntity();
        UUID playerId = player.getUUID();
        
        // 清除玩家的所有伤害加成
        removeAllDamageBonuses(player);
        
        // 清除玩家的伤害存储
        AMPLIFIER_DAMAGE_STORAGE.remove(playerId);
        
        // 清除玩家的护盾消耗记录
        AMPLIFIER_SHIELD_CONSUMPTION.remove(playerId);
        

    }
    
    /**
     * 监听玩家登录事件，清除可能残留的伤害加成
     * 解决玩家强制退出游戏后修饰符残留的问题
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // 在PlayerLoggedInEvent中，玩家对象可以通过getEntity()方法获取
        Player player = (Player) event.getEntity();
        UUID playerId = player.getUUID();
        
        // 清除玩家的所有伤害加成（防止强制退出后残留）
        removeAllDamageBonuses(player);
        
        // 清除玩家的伤害存储
        AMPLIFIER_DAMAGE_STORAGE.remove(playerId);
        
        // 清除玩家的护盾消耗记录
        AMPLIFIER_SHIELD_CONSUMPTION.remove(playerId);
        

    }

    /**
     * 在LivingDamageEvent中处理增幅护盾的特殊效果
     * 当正常护盾机制处理完伤害后，调用此方法执行增幅效果
     * @param player 玩家对象
     * @param damage 实际伤害值
     */
    public static void handleAmplifierAfterShield(Player player, float damage) {
        // 检查是否是服务器端
        if (player.level().isClientSide()) {
            return;
        }

        // 获取玩家UUID
        UUID playerId = player.getUUID();

        // 检查玩家当前激活的护盾是否是增幅护盾
        ShieldItemGy activeShield = ShieldManager.getActiveShieldItem(player);
        if (!(activeShield instanceof AmplifierShield)) {
            return;
        }

        // 在这里实现增幅护盾的特殊效果逻辑
        // 由于用户没有具体说明特殊效果，可以根据需要添加
        // 例如：增加玩家攻击力、速度等
    }

    /**
 * 监听玩家Tick事件，处理增幅护盾的持续效果
 */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 检查是否是服务器端和结束阶段
        if (event.player.level().isClientSide() || event.phase != TickEvent.Phase.END) {
            return;
        }

        // 获取玩家
        Player player = event.player;
        UUID playerId = player.getUUID();

        // 处理现有伤害加成的计时
        handleDamageBonusesTiming(player);

        // 检查玩家当前激活的护盾是否是增幅护盾
        ShieldItemGy activeShield = ShieldManager.getActiveShieldItem(player);
        if (!(activeShield instanceof AmplifierShield)) {
            // 如果玩家不再使用增幅护盾，清除所有相关效果
            removeAllDamageBonuses(player);
            AMPLIFIER_DAMAGE_STORAGE.remove(playerId);
            return;
        }

        // 检查当前护盾值是否大于0
        float currentShield = ShieldManager.getPlayerShield(player);
        if (currentShield <= 0) {
            // 护盾值为0，移除所有伤害加成
            removeAllDamageBonuses(player);
            return;
        }

        // 获取增幅护盾配置
        Config.ShieldConfig amplifierConfig = Config.SHIELD_CONFIGS.get("gy_trinket:shield_amplifier");
        
        // 处理记录的伤害值，创建或更新伤害加成
        float storedDamage = AMPLIFIER_DAMAGE_STORAGE.getOrDefault(playerId, 0.0F);
        if (storedDamage > 0) {
            // 计算伤害加成：每点存储值增加配置的伤害加成
            double damageBonusPercent = storedDamage * amplifierConfig.amplifierDamageBonusPerShield;
            
            // 创建或更新伤害加成
            createOrUpdateDamageBonus(player, damageBonusPercent);
            
            // 清除记录的损失护盾值
            AMPLIFIER_DAMAGE_STORAGE.put(playerId, 0.0F);
        }
        
        // 按照配置的频率更新修饰符应用状态
        if (player.tickCount % amplifierConfig.amplifierDamageBonusFrequency == 0) {
            applyDamageBonuses(player);
            
            // 消耗最大护盾值的配置百分比（只要激活了增幅护盾且护盾值大于零）
            if (currentShield > 0) {
                float maxShield = ShieldManager.getMaxShield(player);
                float shieldCost = maxShield * (float)(amplifierConfig.amplifierDamageBonusShieldCost / 100.0);
                
                // 记录持续消耗的护盾值
                recordShieldDamage(player, shieldCost);
                
                // 更新护盾值
                float newShield = Math.max(0, currentShield - shieldCost);
                ShieldEvent.updatePlayerShield(player, newShield);

                // 检查护盾值是否为零，若为零则重新开始护盾冷却
                if (newShield < shieldCost ) {
                    ShieldEvent.triggerShieldCooldown(player);
                }

                // 记录消耗的护盾值
                float totalConsumption = AMPLIFIER_SHIELD_CONSUMPTION.getOrDefault(playerId, 0.0F) + shieldCost;
                AMPLIFIER_SHIELD_CONSUMPTION.put(playerId, totalConsumption);

            }
        }
    }
    
    /**
     * 处理现有伤害加成的计时
     */
    private void handleDamageBonusesTiming(Player player) {
        UUID playerId = player.getUUID();
        List<DamageBonus> bonuses = AMPLIFIER_DAMAGE_BONUSES.getOrDefault(playerId, new ArrayList<>());
        
        if (bonuses.isEmpty()) {
            return;
        }
        
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }
        
        // 使用迭代器安全地移除过期的伤害加成
        Iterator<DamageBonus> iterator = bonuses.iterator();
        while (iterator.hasNext()) {
            DamageBonus bonus = iterator.next();
            bonus.decrementTicks();
            
            if (bonus.getRemainingTicks() <= 0) {
                // 移除过期的伤害加成
                if (bonus.isApplied()) {
                    attackDamage.removeModifier(bonus.getUuid());
                }
                iterator.remove();
            }
        }
        
        // 更新或移除空列表
        if (bonuses.isEmpty()) {
            AMPLIFIER_DAMAGE_BONUSES.remove(playerId);
        } else {
            AMPLIFIER_DAMAGE_BONUSES.put(playerId, bonuses);
        }
    }
    
    /**
     * 创建或更新伤害加成
     * @param player 玩家对象
     * @param bonusPercent 伤害加成百分比
     */
    private void createOrUpdateDamageBonus(Player player, double bonusPercent) {
        // 获取玩家UUID
        UUID playerId = player.getUUID();
        
        // 获取玩家的伤害加成列表
        List<DamageBonus> bonuses = AMPLIFIER_DAMAGE_BONUSES.getOrDefault(playerId, new ArrayList<>());
        
        // 查找是否存在开放期内的修饰符
        DamageBonus openBonus = null;
        for (DamageBonus bonus : bonuses) {
            if (bonus.isOpen()) {
                openBonus = bonus;
                break;
            }
        }
        
        if (openBonus != null) {
            // 存在开放期的修饰符，累加伤害提升数值
            double additionalAmount = bonusPercent / 100.0;
            openBonus.addAmount(additionalAmount);
        } else {
            // 不存在开放期的修饰符，检查是否可以创建新的
            if (bonuses.size() < MAX_BONUSES) {
                // 创建新的伤害加成
                double bonusAmount = bonusPercent / 100.0;
                DamageBonus newBonus = new DamageBonus(bonusAmount);
                bonuses.add(newBonus);
            } else {
                // 已经达到最大修饰符数量，忽略新的伤害提升
            }
        }
        
        // 更新伤害加成列表
        AMPLIFIER_DAMAGE_BONUSES.put(playerId, bonuses);
    }
    
    /**
     * 应用所有开放期结束的伤害加成
     * @param player 玩家对象
     */
    private void applyDamageBonuses(Player player) {
        UUID playerId = player.getUUID();
        List<DamageBonus> bonuses = AMPLIFIER_DAMAGE_BONUSES.getOrDefault(playerId, new ArrayList<>());
        
        if (bonuses.isEmpty()) {
            return;
        }
        
        // 获取玩家的攻击力属性
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }
        
        // 应用所有开放期结束且未应用的伤害加成
        for (DamageBonus bonus : bonuses) {
            if (!bonus.isOpen() && !bonus.isApplied()) {
                // 创建并添加新的修饰符
                AttributeModifier modifier = new AttributeModifier(
                    bonus.getUuid(),
                    "Amplifier Shield Damage Bonus",
                    bonus.getAmount(),
                    AttributeModifier.Operation.MULTIPLY_TOTAL
                );
                
                attackDamage.addPermanentModifier(modifier);
                bonus.setApplied(true);
            }
        }
    }
    

    
    /**
     * 移除玩家的所有伤害加成
     * @param player 玩家对象
     */
    private void removeAllDamageBonuses(Player player) {
        UUID playerId = player.getUUID();
        List<DamageBonus> bonuses = AMPLIFIER_DAMAGE_BONUSES.remove(playerId);
        
        if (bonuses == null || bonuses.isEmpty()) {
            return;
        }
        
        // 获取玩家的攻击力属性
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) return;
        
        // 移除所有伤害加成
        for (DamageBonus bonus : bonuses) {
            attackDamage.removeModifier(bonus.getUuid());
        }
    }

    /**
     * 监听LivingDamageEvent事件，处理增幅护盾的伤害处理效果
     */
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        // 检查是否是服务器端
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // 检查伤害实体是否是玩家
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 检查玩家当前激活的护盾是否是增幅护盾
        ShieldItemGy activeShield = ShieldManager.getActiveShieldItem(player);
        if (!(activeShield instanceof AmplifierShield)) {
            return;
        }
    }
    
    /**
     * 计算玩家当前的总伤害提升百分比
     * @param player 玩家对象
     * @return 总伤害提升百分比（例如：0.25 表示 25%）
     */
    public static float getTotalDamageBonus(Player player) {
        UUID playerId = player.getUUID();
        List<DamageBonus> bonuses = AMPLIFIER_DAMAGE_BONUSES.getOrDefault(playerId, new ArrayList<>());
        
        float totalBonus = 0.0F;
        for (DamageBonus bonus : bonuses) {
            if (bonus.isApplied()) {
                totalBonus += bonus.getAmount();
            }
        }
        
        return totalBonus;
    }
    
    /**
     * 计算玩家当前所有正在应用的伤害提升百分比
     * @param player 玩家对象
     * @return 总伤害提升百分比（包括增幅护盾、药水效果、装备等所有来源）
     */
    public static float getAllDamageModifiers(Player player) {
        float totalBonus = 0.0F;
        
        // 获取玩家的攻击力属性
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            // 遍历所有修饰符
            for (AttributeModifier modifier : attackDamage.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
                totalBonus += modifier.getAmount();
            }
        }
        
        return totalBonus;
    }
    
    /**
     * 记录损失的护盾值
     * @param player 玩家对象
     * @param damage 损失的护盾值
     */
    public static void recordShieldDamage(Player player, float damage) {
        UUID playerId = player.getUUID();
        
        // 检查当前护盾值是否大于0
        float currentShield = ShieldManager.getPlayerShield(player);
        if (currentShield > 0) {
            // 记录损失的护盾值（加算）
            float storedDamage = AMPLIFIER_DAMAGE_STORAGE.getOrDefault(playerId, 0.0F) + damage;
            AMPLIFIER_DAMAGE_STORAGE.put(playerId, storedDamage);
        } else {
            // 当前护盾值为零或以下，跳过伤害记录
        }
    }
}
