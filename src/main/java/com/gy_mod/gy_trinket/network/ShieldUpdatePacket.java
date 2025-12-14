package com.gy_mod.gy_trinket.network;

import com.gy_mod.gy_trinket.capability.shield.hud.ShieldHudOverlay;
import com.gy_mod.gy_trinket.shield.ShieldManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 护盾更新数据包
 * 用于在服务端和客户端之间传输玩家的护盾状态信息
 */
public class ShieldUpdatePacket {
    // 当前护盾值
    private final float currentShield;
    // 最大护盾值
    private final int maxShield;
    // 当前冷却时间（刻）
    private final int currentCooldown;
    // 最大冷却时间（刻）
    private final int maxCooldown;
    // 是否正在进行护盾重构
    private final boolean isRebuilding;
    // 当前激活的护盾类型（物品的registry name）
    private final String activeShieldType;
    // 当前伤害提升百分比
    private final float damageBonusPercentage;

    /**
     * 构造函数
     * 创建一个新的护盾更新数据包
     * @param currentShield 当前护盾值
     * @param maxShield 最大护盾值
     * @param currentCooldown 当前冷却时间（刻）
     * @param maxCooldown 最大冷却时间（刻）
     */
    public ShieldUpdatePacket(float currentShield, int maxShield, int currentCooldown, int maxCooldown) {
        this(currentShield, maxShield, currentCooldown, maxCooldown, false, "", 0.0F);
    }
    
    /**
     * 构造函数
     * 创建一个新的护盾更新数据包，包含重构状态和激活护盾类型
     * @param currentShield 当前护盾值
     * @param maxShield 最大护盾值
     * @param currentCooldown 当前冷却时间（刻）
     * @param maxCooldown 最大冷却时间（刻）
     * @param isRebuilding 是否正在进行护盾重构
     * @param activeShieldType 当前激活的护盾类型（物品的registry name）
     * @param damageBonusPercentage 当前伤害提升百分比
     */
    public ShieldUpdatePacket(float currentShield, int maxShield, int currentCooldown, int maxCooldown, boolean isRebuilding, String activeShieldType, float damageBonusPercentage) {
        this.currentShield = currentShield;
        this.maxShield = maxShield;
        this.currentCooldown = currentCooldown;
        this.maxCooldown = maxCooldown;
        this.isRebuilding = isRebuilding;
        this.activeShieldType = activeShieldType;
        this.damageBonusPercentage = damageBonusPercentage;
    }

    /**
     * 编码方法
     * 将数据包的数据写入字节缓冲区
     * @param packet 要编码的数据包
     * @param buffer 字节缓冲区
     */
    public static void encode(ShieldUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.currentShield);
        buffer.writeInt(packet.maxShield);
        buffer.writeInt(packet.currentCooldown);
        buffer.writeInt(packet.maxCooldown);
        buffer.writeBoolean(packet.isRebuilding);
        buffer.writeUtf(packet.activeShieldType);
        buffer.writeFloat(packet.damageBonusPercentage);
    }

    /**
     * 解码方法
     * 从字节缓冲区读取数据并创建数据包
     * @param buffer 字节缓冲区
     * @return 解码后的数据包
     */
    public static ShieldUpdatePacket decode(FriendlyByteBuf buffer) {
        float currentShield = buffer.readFloat();
        int maxShield = buffer.readInt();
        int currentCooldown = buffer.readInt();
        int maxCooldown = buffer.readInt();
        boolean isRebuilding = buffer.readBoolean();
        String activeShieldType = buffer.readUtf();
        float damageBonusPercentage = buffer.readFloat();
        return new ShieldUpdatePacket(currentShield, maxShield, currentCooldown, maxCooldown, isRebuilding, activeShieldType, damageBonusPercentage);
    }

    /**
     * 处理方法
     * 在客户端接收并处理数据包
     * @param packet 接收到的数据包
     * @param contextSupplier 网络事件上下文提供者
     */
    public static void handle(ShieldUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        // 确保在客户端主线程处理更新操作
        context.enqueueWork(() -> {
            // 更新客户端HUD界面的护盾显示数据
            ShieldHudOverlay.getInstance().updateShieldData(packet.currentShield, packet.maxShield, packet.currentCooldown, packet.maxCooldown, packet.damageBonusPercentage);
            // 如果正在重构，通知HUD处理粒子效果
            ShieldHudOverlay.getInstance().handleShieldRebuilding(packet.isRebuilding);
            
            // 关键修复：在客户端更新ShieldManager的护盾值，以便渲染层能够获取到正确的护盾值
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                ShieldManager.setPlayerShield(minecraft.player, packet.currentShield);
                // 存储激活的护盾类型信息到客户端ShieldManager
                ShieldManager.setClientActiveShieldType(minecraft.player, packet.activeShieldType);
            }
            
            // 检查护盾是否破裂（护盾值变为0）
            if (packet.currentShield <= 0) {
                // 在客户端生成护盾破裂粒子效果
                com.gy_mod.gy_trinket.shield.particle.ShieldParticleGenerator.generateShieldBreakParticlesClient();
            }
        });
        // 标记数据包已处理
        context.setPacketHandled(true);
    }

    /**
     * 静态方法：发送数据包给指定玩家
     * @param channel 网络通道
     * @param packet 要发送的数据包
     * @param player 目标玩家
     */
    public static void sendToPlayer(SimpleChannel channel, ShieldUpdatePacket packet, ServerPlayer player) {
        // 使用PLAY_TO_CLIENT方向确保数据包发送到客户端
        channel.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
