package com.azukaar.difficultyoverhaul;

import org.slf4j.Logger;

import com.azukaar.difficultyoverhaul.difficulty.DifficultyCommand;
import com.azukaar.difficultyoverhaul.difficulty.DifficultyConfig;
import com.azukaar.difficultyoverhaul.event.ModEvents;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DifficultyOverhaul.MODID)
public class DifficultyOverhaul
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "fair_difficulty_overhaul";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "fair_difficulty_overhaul" namespace

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "fair_difficulty_overhaul" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> CREATIVE_TAB_ICON = ITEMS.register("creative_tab_icon", CreativeTabIcon::new);

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> afdo_tab = CREATIVE_MODE_TABS.register("afdo_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> CREATIVE_TAB_ICON.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());
            
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public DifficultyOverhaul()
    {
        LOGGER.info("[AZU] 1 ");
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        LOGGER.info("[AZU] 2");
        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        
        LOGGER.info("[AZU] 3");
		ModEntityRegistry.ENTITIES.register(modEventBus);
        ModEntityRegistry.SPAWN_EGGS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (fair_difficulty_overhaul) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ModEvents.class);

        LOGGER.info("[AZU] 4");
        // ModBiomeModifier.registerBiomeModifiers(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(ModClientSetup.class);
        }

        LOGGER.info("[AZU] 5");
        // Register the config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, DifficultyConfig.serverSpec);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("Initializing AFDO");
    }


    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTab() == afdo_tab.get()) {
            ModEntityRegistry.SPAWN_EGGS.getEntries().forEach(spawnEggHolder -> {
                event.accept(spawnEggHolder.get());
            });
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("Initializing mod");
        DifficultyCommand.register(event.getServer().getCommands().getDispatcher());

        // if per-player diff is on, force server difficulty to be hard
        if (DifficultyConfig.SERVER.perPlayerDifficulty.get()) {
            event.getServer().setDifficulty(Difficulty.HARD, true);
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        }
    }
}
