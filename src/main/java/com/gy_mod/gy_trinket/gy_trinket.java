package com.gy_mod.gy_trinket;

import com.gy_mod.gy_trinket.block.ModBlocks;
import com.gy_mod.gy_trinket.client.ClientSetup;
import com.gy_mod.gy_trinket.event.ShieldEvent;
import com.gy_mod.gy_trinket.event.ReflectShieldEvent;
import com.gy_mod.gy_trinket.event.FireRingShieldEvent;
import com.gy_mod.gy_trinket.event.AmplifierShieldEvent;
import com.gy_mod.gy_trinket.item.ModCreativeModeTabs;
import com.gy_mod.gy_trinket.item.ModItems;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组主类
 * 作为模组的入口点，负责初始化和注册所有模组组件
 */
// The value here should match an entry in the META-INF/mods.toml file
@Mod(gy_trinket.MOD_ID)
public class gy_trinket
{
    // 模组ID（必须与mods.toml中的一致）
    public static final String MOD_ID = "gy_trinket";
    // 日志工具
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * 模组构造函数
     * 在模组加载时自动调用，执行模组的初始化工作
     * @param context FMLJavaMod加载上下文
     */
    public gy_trinket(FMLJavaModLoadingContext context)
    {
        // 获取模组事件总线
        IEventBus modEventBus = context.getModEventBus();

        // 1. 注册物品
        ModItems.register(modEventBus);
        // 2. 注册创意模式标签
        ModCreativeModeTabs.register(modEventBus);
        // 3. 注册方块
        ModBlocks.register(modEventBus);


        // 4. 注册护盾事件
        ShieldEvent.register();
        ReflectShieldEvent.register();
        FireRingShieldEvent.register();
        AmplifierShieldEvent.register();

        // 5. 客户端初始化（仅在客户端执行）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.register();
        }

        // 6. 注册网络通信
        com.gy_mod.gy_trinket.network.ModMessages.register();

        // 7. 注册配置文件
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, Config.SPEC);

        // 记录模组加载完成日志
        LOGGER.info("[" + MOD_ID + "] 模组加载完成");
    }

}