package io.wispforest.accessories.mixin;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.networking.server.ScreenOpen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    public CreativeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Unique
    private Button accessoryButton = null;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen;init()V"))
    private void injectAccessoryButton(CallbackInfo ci) {
        var xOffset = Accessories.getConfig().clientData.creativeInventoryButtonXOffset;
        var yOffset = Accessories.getConfig().clientData.creativeInventoryButtonYOffset;

        accessoryButton = this.addRenderableWidget(
                Button.builder(Component.empty(), button -> {
                            AccessoriesAccess.getNetworkHandler().sendToServer(new ScreenOpen());
                        }).bounds(this.leftPos + xOffset, this.topPos + yOffset, 8, 8)
                        .tooltip(Tooltip.create(Component.translatable(Accessories.translation("open.screen"))))
                        .build()
        );

        accessoryButton.visible = false;
    }

    @Inject(method = "selectTab", at = @At(value = "TAIL"))
    private void adjustAccessoryButton(CreativeModeTab tab, CallbackInfo ci){
        accessoryButton.visible = tab.getType().equals(CreativeModeTab.Type.INVENTORY);
    }

}