package com.gy_mod.gy_trinket.item;

import com.gy_mod.gy_trinket.gy_trinket;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, gy_trinket.MOD_ID);

    // 护盾核心标签页
    public static final RegistryObject<CreativeModeTab> SHIELD_TAB = CREATIVE_MODE_TABS.register("shield_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.SHIELD_GY.get()))
                    .title(Component.translatable("creativetab.shield_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.SHIELD_GY.get()); // 基础护盾
                        pOutput.accept(ModItems.SHIELD_REFLECT.get()); // 反射护盾
                        pOutput.accept(ModItems.SHIELD_FIRE_RING.get()); // 火环护盾
                        pOutput.accept(ModItems.SHIELD_AMPLIFIER.get()); // 增幅护盾
                    })
                    .build()
    );

    // 注册标签页
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}