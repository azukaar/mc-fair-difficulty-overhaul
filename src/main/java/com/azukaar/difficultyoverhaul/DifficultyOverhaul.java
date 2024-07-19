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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DifficultyOverhaul.MODID)
public class DifficultyOverhaul
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "fair_difficulty_overhaul";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "fair_difficulty_overhaul" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "fair_difficulty_overhaul" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "fair_difficulty_overhaul" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<Item, Item> CREATIVE_TAB_ICON = ITEMS.register("creative_tab_icon", CreativeTabIcon::new);

        // Creates a creative tab with the id "fair_difficulty_overhaul:afdo_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> afdo_tab = CREATIVE_MODE_TABS.register("afdo_tab", () -> ModCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fair_difficulty_overhaul")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> CREATIVE_TAB_ICON.get().getDefaultInstance())
            // set icon as icon.png
            .displayItems((parameters, output) -> {
            }).build());
            
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public DifficultyOverhaul(IEventBus modEventBus, ModContainer modContainer)
    {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        
		ModEntityRegistry.ENTITIES.register(modEventBus);
        ModEntityRegistry.SPAWN_EGGS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (fair_difficulty_overhaul) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(ModEvents.class);

        ModBiomeModifier.registerBiomeModifiers(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(ModClientSetup.class);
        }

        modContainer.registerConfig(ModConfig.Type.SERVER, DifficultyConfig.serverSpec);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
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
