package com.gy_mod.gy_trinket.item;

import com.gy_mod.gy_trinket.gy_trinket;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册类
 * 负责注册模组中的所有物品
 */
public class ModItems {
    // 私有构造函数，防止实例化
    private ModItems() {}

    /**
     * 延迟注册器
     * 用于在游戏启动时注册物品
     */
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, gy_trinket.MOD_ID);

    /**
     * 基础护盾物品 - 使用默认配置
     */
    public static final RegistryObject<Item> SHIELD_GY = 
            ITEMS.register("shield_gy", () -> new BasicShield());
    
    /**
     * 反射护盾物品 - 更高的最大护盾值和恢复速度
     */
    public static final RegistryObject<Item> SHIELD_REFLECT = 
            ITEMS.register("shield_reflect", () -> new ReflectShield());
    
    /**
     * 火环护盾物品 - 更快的恢复速度和更长的无敌时间
     */
    public static final RegistryObject<Item> SHIELD_FIRE_RING =
            ITEMS.register("shield_fire_ring", () -> new FireRingShield());

    /**
     * 增幅护盾物品 - 具有特殊增幅效果
     */
    public static final RegistryObject<Item> SHIELD_AMPLIFIER =
            ITEMS.register("shield_amplifier", () -> new AmplifierShield());

    /**
     * 注册所有物品到事件总线
     * @param eventBus 模组事件总线
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}