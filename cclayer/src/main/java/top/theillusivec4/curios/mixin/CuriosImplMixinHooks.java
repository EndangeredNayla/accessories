/*
 * Copyright (c) 2018-2023 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.theillusivec4.curios.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.SlotBasedPredicate;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.cclayer.CCLayer;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.CuriosRegistry;
import top.theillusivec4.curios.common.capability.ItemizedCurioCapability;
import top.theillusivec4.curios.common.data.CuriosEntityManager;
import top.theillusivec4.curios.common.data.CuriosSlotManager;
import top.theillusivec4.curios.compat.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class CuriosImplMixinHooks {

  private static final Map<Item, ICurioItem> REGISTRY = new ConcurrentHashMap<>();

  public static void registerCurio(Item item, ICurioItem icurio) {
    REGISTRY.put(item, icurio);

    AccessoriesAPI.registerAccessory(item, new WrappedCurio(icurio));
  }

  public static Optional<ICurioItem> getCurioFromRegistry(Item item) {
    var iCurioItem = REGISTRY.get(item);

    if(iCurioItem != null) return Optional.of(iCurioItem);

    return Optional.ofNullable(AccessoriesAPI.getAccessory(item)).map(WrappedAccessory::new);
  }

  public static Optional<ISlotType> getSlot(String id) {
    return Optional.ofNullable(CuriosApi.getSlots().get(id));
  }

  public static ResourceLocation getSlotIcon(String id) {
    var type = SlotTypeLoader.INSTANCE.getSlotTypes(false).get(CuriosWrappingUtils.curiosToAccessories(id));

    if(type == null) return new ResourceLocation(CuriosApi.MODID, "slot/empty_curio_slot");

    return type.icon();
  }

  public static Map<String, ISlotType> getSlots(boolean isClient) {
    return (isClient ? CuriosSlotManager.CLIENT : CuriosSlotManager.SERVER).getSlots();
  }

  public static Map<String, ISlotType> getEntitySlots(EntityType<?> type, boolean isClient) {
    return (isClient ? CuriosEntityManager.SERVER : CuriosEntityManager.CLIENT).getEntitySlots(type);
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack, boolean isClient) {
//    Map<String, ISlotType> result = new HashMap<>();
//    Set<String> ids = stack.getTags()
//        .filter(tagKey -> {
//          var namespace = tagKey.location().getNamespace();
//
//          return namespace.equals(CuriosApi.MODID) || namespace.equals("trinkets") || namespace.equals(Accessories.MODID);
//        })
//        .map(tagKey -> {
//          var location = tagKey.location();
//
//          if(location.getNamespace().equals("trinkets")){
//            var path = location.getPath();
//
//            if(!path.contains("/")) return path;
//
//            var parts = path.split("/");
//
//            if(!parts[0].isBlank()) return path;
//
//            StringBuilder builder = new StringBuilder();
//
//            for (int i = 1; i < parts.length; i++) builder.append(parts[i]);
//
//            return builder.toString();
//          } else if(location.getNamespace().equals(CuriosApi.MODID)) {
//            return CuriosWrappingUtils.curiosToAccessories(location.getPath());
//          } else {
//            return location.getPath();
//          }
//        }).collect(Collectors.toSet());
//
//    Map<String, ISlotType> allSlots = getSlots();
//
//    for (String id : ids) {
//      ISlotType slotType = allSlots.get(id);
//
//      if (slotType != null) {
//        result.put(id, slotType);
//      } else {
//        result.put(id, new SlotType.Builder(id).build());
//      }
//    }
//    return result;
    Level level = CCLayer.getDummyLevel(isClient);

    return filteredSlots(slotType -> {
      SlotContext slotContext = new SlotContext(slotType.getIdentifier(), null, 0, false, true);
      SlotResult slotResult = new CursedSlotResult(slotContext, stack, level);
      return CuriosApi.testCurioPredicates(slotType.getValidators(), slotResult);
    }, CuriosApi.getSlots(isClient));
  }

  private static class CursedSlotResult extends SlotResult {

    public final Level level;

    public CursedSlotResult(SlotContext slotContext, ItemStack stack, Level level) {
      super(slotContext, stack);
      this.level = level;
    }
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack, LivingEntity livingEntity) {
//    Map<String, ISlotType> result = new HashMap<>();
//    Set<String> ids = stack.getTags()
//        .filter(tagKey -> tagKey.location().getNamespace().equals(CuriosApi.MODID))
//        .map(tagKey -> tagKey.location().getPath()).collect(Collectors.toSet());
//    Map<String, ISlotType> entitySlots = getEntitySlots(livingEntity.getType());
//
//    for (String id : ids) {
//      ISlotType slotType = entitySlots.get(id);
//
//      if (slotType != null) {
//        result.put(id, slotType);
//      } else {
//        result.put(id, new SlotType.Builder(id).build());
//      }
//    }
//
//    var additionalSlots = AccessoriesAPI.getStackSlotTypes(livingEntity.level(), stack);
//
//    for (io.wispforest.accessories.api.slot.SlotType additionalSlot : additionalSlots) {
//      if(!result.containsKey(additionalSlot.name())){
//        result.put(additionalSlot.name(), new WrappedSlotType(additionalSlot));
//      }
//    }
//
//    return result;
    return filteredSlots(slotType -> {
      SlotContext slotContext = new SlotContext(slotType.getIdentifier(), livingEntity, 0, false, true);
      SlotResult slotResult = new SlotResult(slotContext, stack);
      return CuriosApi.testCurioPredicates(slotType.getValidators(), slotResult);
    }, CuriosApi.getEntitySlots(livingEntity));
  }

  private static Map<String, ISlotType> filteredSlots(Predicate<ISlotType> filter, Map<String, ISlotType> map) {
    Map<String, ISlotType> result = new HashMap<>();

    for (Map.Entry<String, ISlotType> entry : map.entrySet()) {
      ISlotType slotType = entry.getValue();

      if (filter.test(slotType)) {
        result.put(entry.getKey(), slotType);
      }
    }

    return result;
  }

  public static LazyOptional<ICurio> getCurio(ItemStack stack) {
    var capability = stack.getCapability(CuriosCapability.ITEM);

    if(capability.isPresent()) return capability;

    var accessory = AccessoriesAPI.getAccessory(stack);

    if(accessory != null) return LazyOptional.of(() -> new ItemizedCurioCapability(new WrappedAccessory(accessory), stack));

    return LazyOptional.empty();
  }

  public static LazyOptional<ICuriosItemHandler> getCuriosInventory(LivingEntity livingEntity) {
    var accessoryCapability = (AccessoriesCapabilityImpl) livingEntity.accessoriesCapability();

    if (accessoryCapability != null) {
      return LazyOptional.of(() -> new WrappedCurioItemHandler(() -> accessoryCapability));
    } else {
      return LazyOptional.empty();
    }
  }

  public static boolean isStackValid(SlotContext slotContext, ItemStack stack) {
    var slotName = CuriosWrappingUtils.curiosToAccessories(slotContext.identifier());

    /*
     * blodhgarm: This is a cursed workaround for people who decided that passing a null entity for SlotContext
     * is a valid solution and for the ones that do it... you make my life hard even if it works within curios.
     * Good news is that this will not be needed within the future... I HOPE!
     */
    if(slotContext.entity() == null) {
      var slotType = SlotTypeLoader.INSTANCE.getSlotTypes(false).get(slotName);

      if(slotType == null) return false;

      boolean bl = false;

      try {
        Level level = CCLayer.getDummyLevel(false);

        bl = AccessoriesAPI.getPredicateResults(slotType.validators(), level, slotType, 0, stack);
      } catch (Exception ignored) {}

      if(!bl) return false;

      boolean bl2 = false;

      try {
        bl2 = AccessoriesAPI.canEquip(stack, SlotReference.of(null, slotName, 0));
      } catch (Exception ignored) {}

      if(bl2) return true;
    } else {
      boolean isValid = AccessoriesAPI.canInsertIntoSlot(stack, SlotReference.of(slotContext.entity(), slotName, slotContext.index()));

      if(isValid) return true;
    }

    String id = slotContext.identifier();
    Set<String> slots = getItemStackSlots(stack, slotContext.entity()).keySet();
    return (!slots.isEmpty() && id.equals("curio")) || slots.contains(id) ||
            slots.contains("curio");
  }

  public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
    Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();

    if (stack.getTag() != null && stack.getTag().contains("CurioAttributeModifiers", 9)) {
      ListTag listnbt = stack.getTag().getList("CurioAttributeModifiers", 10);
      String identifier = slotContext.identifier();

      for (int i = 0; i < listnbt.size(); ++i) {
        CompoundTag compoundnbt = listnbt.getCompound(i);

        if (compoundnbt.getString("Slot").equals(identifier)) {
          ResourceLocation rl = ResourceLocation.tryParse(compoundnbt.getString("AttributeName"));
          UUID id = uuid;

          if (rl != null) {

            if (compoundnbt.contains("UUID")) {
              id = compoundnbt.getUUID("UUID");
            }

            if (id.getLeastSignificantBits() != 0L && id.getMostSignificantBits() != 0L) {
              AttributeModifier.Operation operation =
                      AttributeModifier.Operation.fromValue(compoundnbt.getInt("Operation"));
              double amount = compoundnbt.getDouble("Amount");
              String name = compoundnbt.getString("Name");

              if (rl.getNamespace().equals("curios")) {
                String identifier1 = CuriosWrappingUtils.curiosToAccessories(rl.getPath());

                if (CuriosApi.getSlot(identifier1).isPresent()) {
                  CuriosApi.addSlotModifier(multimap, identifier1, id, amount, operation);
                }
              } else {
                Attribute attribute = BuiltInRegistries.ATTRIBUTE.getOptional(rl).orElse(null);

                if (attribute != null) {
                  multimap.put(attribute, new AttributeModifier(id, name, amount, operation));
                }
              }
            }
          }
        }
      }
    } else {
      multimap = getCurio(stack).map(curio -> curio.getAttributeModifiers(slotContext, uuid))
              .orElse(multimap);
    }
    CurioAttributeModifierEvent evt = new CurioAttributeModifierEvent(stack, slotContext, uuid, multimap);
    MinecraftForge.EVENT_BUS.post(evt);
    return HashMultimap.create(evt.getModifiers());
  }

  public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, String identifier, UUID uuid, double amount, AttributeModifier.Operation operation) {
    map.put(io.wispforest.accessories.api.attributes.SlotAttribute.getSlotAttribute(CuriosWrappingUtils.curiosToAccessories(identifier)), new AttributeModifier(uuid, identifier, amount, operation));
  }

  public static void addSlotModifier(ItemStack stack, String identifier, String name, UUID uuid, double amount, AttributeModifier.Operation operation, String slot) {
    io.wispforest.accessories.api.attributes.SlotAttribute.addSlotAttribute(stack, CuriosWrappingUtils.curiosToAccessories(identifier), slot, name, uuid, amount, operation, false);
  }

  public static void addModifier(ItemStack stack, Attribute attribute, String name, UUID uuid, double amount, AttributeModifier.Operation operation, String slot) {
    AccessoriesAPI.addAttribute(stack, slot, attribute, name, uuid, amount, operation, false);
  }

  //--

  public static void broadcastCurioBreakEvent(SlotContext slotContext) {
    AccessoriesAPI.breakStack(CuriosWrappingUtils.fromContext(slotContext));
  }

  private static final Map<String, UUID> UUIDS = new HashMap<>();

  public static UUID getUuid(SlotContext slotContext) {
    String key = slotContext.identifier() + slotContext.index();
    return UUIDS.computeIfAbsent(key, (k) -> UUID.nameUUIDFromBytes(k.getBytes()));
  }

  private static final Map<ResourceLocation, Predicate<SlotResult>> SLOT_RESULT_PREDICATES = new HashMap<>();

  public static void registerCurioPredicate(ResourceLocation resourceLocation, Predicate<SlotResult> validator) {
    SLOT_RESULT_PREDICATES.putIfAbsent(resourceLocation, validator);

    AccessoriesAPI.registerPredicate(resourceLocation, new SafeSlotBasedPredicate(resourceLocation, validator));
  }

  public static Optional<Predicate<SlotResult>> getCurioPredicate(ResourceLocation resourceLocation) {
    return Optional.ofNullable(SLOT_RESULT_PREDICATES.get(resourceLocation));
  }

  public static Map<ResourceLocation, Predicate<SlotResult>> getCurioPredicates() {
    return ImmutableMap.copyOf(SLOT_RESULT_PREDICATES);
  }

  public static boolean testCurioPredicates(Set<ResourceLocation> predicates, SlotResult slotResult) {
    var convertedSet = new HashSet<ResourceLocation>();

    for (var location : predicates) {
      convertedSet.add(CuriosWrappingUtils.curiosToAccessories_Validators(location));
    }

    var ref = CuriosWrappingUtils.fromContext(slotResult.slotContext());

    SlotType slotType;
    Level level;

    if(slotResult instanceof CursedSlotResult cursedSlotResult) {
      slotType = SlotTypeLoader.INSTANCE.getSlotTypes(cursedSlotResult.level.isClientSide).get(ref.slotName());
      level = cursedSlotResult.level;
    } else {
      slotType = ref.type();
      level = ref.entity().level();
    }

    if(slotType == null) {
      throw new IllegalStateException("Unable to get a SlotType using the WrappedTrinketInventory from the SlotTypeLoader! [Name: " + slotType.name() +"]");
    }

    try {
      return AccessoriesAPI.getPredicateResults(convertedSet, level, slotType, ref.slot(), slotResult.stack());
    } catch (Exception e) {
      return false;
    }
  }

  static {
    registerCurioPredicate(new ResourceLocation(CuriosApi.MODID, "all"), (slotResult) -> true);
    registerCurioPredicate(new ResourceLocation(CuriosApi.MODID, "none"), (slotResult) -> false);
    registerCurioPredicate(new ResourceLocation(CuriosApi.MODID, "tag"), (slotResult) -> {
      String id = slotResult.slotContext().identifier();
      TagKey<Item> tag1 = ItemTags.create(new ResourceLocation(CuriosApi.MODID, id));
      TagKey<Item> tag2 = ItemTags.create(new ResourceLocation(CuriosApi.MODID, "curio"));
      ItemStack stack = slotResult.stack();
      return stack.is(tag1) || stack.is(tag2);
    });
  }

  //--

  private final static class SafeSlotBasedPredicate implements SlotBasedPredicate {
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean hasErrored = false;

    private final ResourceLocation location;
    private final Predicate<SlotResult> curiosValidator;

    public SafeSlotBasedPredicate(ResourceLocation location, Predicate<SlotResult> curiosValidator) {
      this.location = location;
      this.curiosValidator = curiosValidator;
    }

    @Override
    public TriState isValid(Level level, io.wispforest.accessories.api.slot.SlotType slotType, int slot, ItemStack stack) {
      if(hasErrored) return TriState.DEFAULT;

      try {
        return TriState.of(this.curiosValidator.test(new SlotResult(new SlotContext(slotType.name(), null, slot, false, true), stack)));
      } catch (Exception e) {
        this.hasErrored = true;
        LOGGER.warn("Unable to handle Curios Slot Predicate converted to Accessories Slot Predicate due to fundamental incompatibility, issues may be present with it! [Slot: {}, Predicate ID: {}]", slotType.name(), this.location, e);
      }

      return TriState.DEFAULT;
    }
  }
}
