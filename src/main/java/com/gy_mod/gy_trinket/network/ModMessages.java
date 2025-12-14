package com.gy_mod.gy_trinket.network;

import com.gy_mod.gy_trinket.gy_trinket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络消息注册类
 * 管理模组的所有网络通信通道和数据包注册
 */
public class ModMessages {
    // 网络通信通道协议版本（用于版本兼容检查）
    private static final String PROTOCOL_VERSION = "1.0";
    
    /**
     * 网络通道实例
     * 使用模组ID作为命名空间确保通道唯一性
     */
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            // 创建唯一的通道标识符
            ResourceLocation.fromNamespaceAndPath(gy_trinket.MOD_ID, "main"),
            // 客户端提供的协议版本
            () -> PROTOCOL_VERSION,
            // 服务端接受的客户端版本检查
            PROTOCOL_VERSION::equals,
            // 客户端接受的服务端版本检查
            PROTOCOL_VERSION::equals
    );

    /**
     * 注册所有网络数据包
     * 为模组的网络通信通道注册所有需要的数据包类型
     */
    public static void register() {
        // 注册护盾值更新数据包
        INSTANCE.registerMessage(
                0,                           // 数据包唯一ID（从0开始，不能重复）
                ShieldUpdatePacket.class,     // 数据包类
                ShieldUpdatePacket::encode,   // 数据包序列化方法
                ShieldUpdatePacket::decode,   // 数据包反序列化方法
                ShieldUpdatePacket::handle    // 数据包处理方法
        );
    }
}
