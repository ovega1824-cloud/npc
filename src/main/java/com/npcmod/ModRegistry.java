package com.npcmod;

import com.npcmod.entity.NpcEntity;
import com.npcmod.item.NpcEditorItem;
import com.npcmod.item.NpcSpawnerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NpcMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NpcMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NpcMod.MODID);

    public static final RegistryObject<EntityType<NpcEntity>> NPC = ENTITIES.register("npc",
            () -> EntityType.Builder.of(NpcEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("npc"));

    public static final RegistryObject<Item> SPAWNER = ITEMS.register("npc_spawner",
            () -> new NpcSpawnerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> EDITOR = ITEMS.register("npc_editor",
            () -> new NpcEditorItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("npcmod",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.npcmod"))
                    .icon(() -> new ItemStack(SPAWNER.get()))
                    .displayItems((params, output) -> {
                        output.accept(SPAWNER.get());
                        output.accept(EDITOR.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private ModRegistry() {}
}
