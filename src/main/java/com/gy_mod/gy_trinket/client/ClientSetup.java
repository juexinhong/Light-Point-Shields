package com.gy_mod.gy_trinket.client;

import com.gy_mod.gy_trinket.capability.shield.hud.ShieldHudOverlay;
import com.gy_mod.gy_trinket.gy_trinket;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 客户端事件注册（仅HUD渲染）
 * 负责处理与客户端相关的事件注册，特别是护盾HUD的渲染功能
 */
@Mod.EventBusSubscriber(modid = gy_trinket.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {
    /**
     * 客户端初始化方法
     * 在客户端加载时执行，用于设置护盾HUD的初始状态
     * @param event 客户端设置事件
     */
    public static void init(FMLClientSetupEvent event) {
        gy_trinket.LOGGER.info("[" + gy_trinket.MOD_ID + "] 护盾HUD初始化完成");
    }

    /**
     * 渲染护盾HUD事件处理方法
     * 在GUI渲染后调用，负责绘制护盾HUD
     * @param event GUI渲染事件
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        ShieldHudOverlay.getInstance().render(event.getGuiGraphics());
    }




    
    /**
     * 客户端事件注册方法
     * 供主类调用，用于注册客户端相关的事件监听器
     */
    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::init);
    }
}