package top.theillusivec4.curios.compat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;

import java.util.Optional;
import java.util.UUID;

public class CuriosWrappingUtils {

    public static SlotContext create(SlotReference reference){
        return create(reference, true);
    }

    public static SlotContext create(SlotReference reference, boolean visible){
        return new SlotContext(accessoriesToCurios(reference.slotName()), reference.entity(), reference.slot(), false, visible);
    }

    public static SlotReference fromContext(SlotContext context){
        return SlotReference.of(context.entity(), curiosToAccessories(context.identifier()), context.index());
    }

    //--

    public static ICurio.DropRule convert(DropRule dropRule){
        return switch (dropRule){
            case KEEP -> ICurio.DropRule.ALWAYS_KEEP;
            case DROP -> ICurio.DropRule.ALWAYS_DROP;
            case DESTROY -> ICurio.DropRule.DESTROY;
            case DEFAULT -> ICurio.DropRule.DEFAULT;
        };
    }

    public static DropRule convert(ICurio.DropRule dropRule){
        return switch (dropRule){
            case DEFAULT -> DropRule.DEFAULT;
            case ALWAYS_DROP -> DropRule.DROP;
            case ALWAYS_KEEP -> DropRule.KEEP;
            case DESTROY -> DropRule.DESTROY;
        };
    }

    public static TriState convert(Event.Result result){
        if(result == null) return TriState.DEFAULT;

        return switch (result){
            case DENY -> TriState.FALSE;
            case ALLOW -> TriState.TRUE;
            case DEFAULT -> TriState.DEFAULT;
        };
    }

    //--

    public static Optional<Accessory> of(ItemStack stack){
        return CuriosImplMixinHooks.getCurioFromRegistry(stack.getItem())
                .or(() -> Optional.ofNullable((stack.getItem() instanceof ICurioItem itemCurio) ? itemCurio : null))
                .map(WrappedCurio::new);
    }

    //--

    public static String curiosToAccessories(String curiosType){
        return switch (curiosType){
            case "curio" -> "any"; // CONFIRM THIS IS WORKING?
            case "body" -> "cape";
            case "bracelet" -> "wrist";
            case "head" -> "hat";
            case "hands" -> "hand";
            case "feet" -> "shoes"; // Special Case for artifacts
            default -> curiosType;
        };
    }

    public static String accessoriesToCurios(String accessoryType){
        return switch (accessoryType){
            case "any" -> "curio"; // CONFIRM THIS IS WORKING?
            case "cape" -> "body" ;
            case "wrist" -> "bracelet";
            case "hat" -> "head";
            case "hand" -> "hands";
            case "shoes" -> "feet"; // Special Case for artifacts
            default -> accessoryType;
        };
    }

    public static ResourceLocation curiosToAccessories_Validators(ResourceLocation location) {
        return switch (location.toString()){
            case "curios:all" -> Accessories.of("all");
            case "curios:none" -> Accessories.of("none");
            case "curios:tag" -> Accessories.of("tag");
            default -> location;
        };
    }

    //--

}
