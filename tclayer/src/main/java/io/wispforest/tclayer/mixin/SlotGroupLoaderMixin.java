package io.wispforest.tclayer.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import dev.emi.trinkets.data.SlotLoader;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotGroupLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

@Mixin(SlotGroupLoader.class)
public abstract class SlotGroupLoaderMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private void injectTrinketsGroupingInfo(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci,
                                            @Local(name = "slotGroups", ordinal = 0) HashMap<String, SlotGroupLoader.SlotGroupBuilder> slotGroups,
                                            @Local(name = "allSlots", ordinal = 1) HashMap<String, SlotType> allSlots,
                                            @Local(name = "remainSlots") HashSet<String> remainSlots) {
        for (var groupEntry : SlotLoader.INSTANCE.getSlots().entrySet()) {
            var groupData = groupEntry.getValue();
            var slots = groupData.slots;

            for (var slotEntry : slots.entrySet()) {
                var slotName = slotEntry.getKey();

                var accessorySlotName = WrappingTrinketsUtils.trinketsToAccessories_Slot(Optional.of(groupEntry.getKey()), slotName);

                var group = slotGroups.getOrDefault(WrappingTrinketsUtils.trinketsToAccessories_Group(groupEntry.getKey()), null);

                if(group == null || !allSlots.containsKey(accessorySlotName)) continue;

                group.addSlot(accessorySlotName);

                allSlots.remove(accessorySlotName);
                remainSlots.remove(accessorySlotName);
            }
        }
    }
}
