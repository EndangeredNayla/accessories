package io.wispforest.testccessories.fabric.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.testccessories.fabric.TestMenu;
import io.wispforest.testccessories.fabric.Testccessories;
import io.wispforest.testccessories.fabric.accessories.AppleAccessory;
import io.wispforest.testccessories.fabric.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.fabric.accessories.PotatoAccessory;
import io.wispforest.testccessories.fabric.accessories.TntAccessory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Equipable;

public class TestccessoriesClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AppleAccessory.clientInit();
        PotatoAccessory.clientInit();
        PointedDripstoneAccessory.clientInit();
        TntAccessory.clientInit();

        MenuScreens.register(Testccessories.TEST_MENU_TYPE, TestScreen::new);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<FabricClientCommandSource>literal("open_test_screen")
                            .executes(context -> {
                                AccessoriesInternals.getNetworkHandler().sendToServer(new TestScreenPacket());

                                return 1;
                            })
            );
        });

        BuiltInRegistries.ITEM.forEach(AccessoriesRendererRegistry::registerArmorRendering);

        RegistryEntryAddedCallback.event(BuiltInRegistries.ITEM).register((i, location, item) -> AccessoriesRendererRegistry.registerArmorRendering(item));
    }
}