/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.oldtest.block;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CustomSignsTest.MODID)
public class CustomSignsTest {
    public static final boolean ENABLE = false; // TODO fix
    public static final String MODID = "custom_signs_test";

    public static final WoodType TEST_WOOD_TYPE = WoodType.register(new WoodType(ResourceLocation.fromNamespaceAndPath(MODID, "test").toString(), BlockSetType.ACACIA));

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredBlock<CustomStandingSignBlock> TEST_STANDING_SIGN = BLOCKS.register("test_sign", () -> new CustomStandingSignBlock(Properties.of().noCollission().strength(1.0F).sound(SoundType.WOOD), CustomSignsTest.TEST_WOOD_TYPE));
    public static final DeferredBlock<CustomWallSignBlock> TEST_WALL_SIGN = BLOCKS.register("test_wall_sign", () -> new CustomWallSignBlock(Properties.of().noCollission().strength(1.0F).sound(SoundType.WOOD), CustomSignsTest.TEST_WOOD_TYPE));
    public static final DeferredBlock<CustomCeilingHangingSignBlock> TEST_CEILING_HANGING_SIGN = BLOCKS.register("test_hanging_sign", () -> new CustomCeilingHangingSignBlock(Properties.of().noCollission().strength(1.0F).sound(SoundType.WOOD), CustomSignsTest.TEST_WOOD_TYPE));
    public static final DeferredBlock<CustomWallHangingSignBlock> TEST_WALL_HANGING_SIGN = BLOCKS.register("test_wall_hanging_sign", () -> new CustomWallHangingSignBlock(Properties.of().noCollission().strength(1.0F).sound(SoundType.WOOD), CustomSignsTest.TEST_WOOD_TYPE));

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<SignItem> TEST_SIGN = ITEMS.register("test_sign", () -> new SignItem((new Item.Properties()).stacksTo(16), TEST_STANDING_SIGN.get(), TEST_WALL_SIGN.get()));
    public static final DeferredItem<HangingSignItem> TEST_HANGING_SIGN = ITEMS.register("test_hanging_sign", () -> new HangingSignItem(TEST_CEILING_HANGING_SIGN.get(), TEST_WALL_HANGING_SIGN.get(), (new Item.Properties()).stacksTo(16)));

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CustomSignBlockEntity>> CUSTOM_SIGN = BLOCK_ENTITIES.register("custom_sign", () -> BlockEntityType.Builder.of(CustomSignBlockEntity::new, TEST_WALL_SIGN.get(), TEST_STANDING_SIGN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CustomHangingSignBlockEntity>> CUSTOM_HANGING_SIGN = BLOCK_ENTITIES.register("custom_hanging_sign", () -> BlockEntityType.Builder.of(CustomHangingSignBlockEntity::new, TEST_WALL_HANGING_SIGN.get(), TEST_CEILING_HANGING_SIGN.get()).build(null));

    public CustomSignsTest(IEventBus eventBus) {
        if (ENABLE) {
            BLOCKS.register(eventBus);
            ITEMS.register(eventBus);
            BLOCK_ENTITIES.register(eventBus);

            eventBus.addListener(this::clientSetup);
            eventBus.addListener(this::commonSetup);
            eventBus.addListener(this::addCreative);
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(TEST_SIGN);
            event.accept(TEST_HANGING_SIGN);
        }
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        BlockEntityRenderers.register(CUSTOM_SIGN.get(), SignRenderer::new);
        BlockEntityRenderers.register(CUSTOM_HANGING_SIGN.get(), HangingSignRenderer::new);
        event.enqueueWork(() -> {
            Sheets.addWoodType(TEST_WOOD_TYPE);
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> WoodType.register(TEST_WOOD_TYPE));
    }

    public static class CustomStandingSignBlock extends StandingSignBlock {
        public CustomStandingSignBlock(Properties propertiesIn, WoodType woodTypeIn) {
            super(woodTypeIn, propertiesIn);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CustomSignBlockEntity(pos, state);
        }
    }

    public static class CustomWallSignBlock extends WallSignBlock {
        public CustomWallSignBlock(Properties propertiesIn, WoodType woodTypeIn) {
            super(woodTypeIn, propertiesIn);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CustomSignBlockEntity(pos, state);
        }
    }

    public static class CustomSignBlockEntity extends SignBlockEntity {
        public CustomSignBlockEntity(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public BlockEntityType<CustomSignBlockEntity> getType() {
            return CUSTOM_SIGN.get();
        }
    }

    public static class CustomCeilingHangingSignBlock extends CeilingHangingSignBlock {
        public CustomCeilingHangingSignBlock(Properties propertiesIn, WoodType woodTypeIn) {
            super(woodTypeIn, propertiesIn);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CustomHangingSignBlockEntity(pos, state);
        }
    }

    public static class CustomWallHangingSignBlock extends WallHangingSignBlock {
        public CustomWallHangingSignBlock(Properties propertiesIn, WoodType woodTypeIn) {
            super(woodTypeIn, propertiesIn);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CustomHangingSignBlockEntity(pos, state);
        }
    }

    public static class CustomHangingSignBlockEntity extends HangingSignBlockEntity {
        public CustomHangingSignBlockEntity(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public BlockEntityType<CustomHangingSignBlockEntity> getType() {
            return CUSTOM_HANGING_SIGN.get();
        }
    }
}
