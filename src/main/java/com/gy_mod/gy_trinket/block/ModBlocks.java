package com.gy_mod.gy_trinket.block;

import com.gy_mod.gy_trinket.gy_trinket;
import com.gy_mod.gy_trinket.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
         DeferredRegister.create(ForgeRegistries.BLOCKS, gy_trinket.MOD_ID);

    public static final RegistryObject<Block> Light_Point_Core_Block_gy =
            registerBlock("light_point_core_block_gy", () -> new Block(BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()));//requiresCorrectToolForDrops()需要工具破坏来产生掉落物

    public static final RegistryObject<Block> Light_Point_Base_Block_gy =
            registerBlock("light_point_base_block_gy", () -> new Block(BlockBehaviour.Properties.of().strength(20.0F, 12.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> Light_Point_Resonator_Block_gy =
            registerBlock("light_point_resonator_block_gy", () -> new Block(BlockBehaviour.Properties.of().strength(0.3F, 1.0F)
                    .requiresCorrectToolForDrops()));

    private static <T extends Block> void registerBlockItems(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> blocks = BLOCKS.register(name, block);
        registerBlockItems(name, blocks);
        return blocks;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}