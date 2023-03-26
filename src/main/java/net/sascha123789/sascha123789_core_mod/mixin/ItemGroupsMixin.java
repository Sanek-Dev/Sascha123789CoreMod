package net.sascha123789.sascha123789_core_mod.mixin;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.sascha123789.sascha123789_core_mod.itemApi.MinecraftItemRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemGroups.class)
public abstract class ItemGroupsMixin {
    @Mutable
    @Final
    @Shadow
    private static List<ItemGroup> GROUPS;

    @Final
    @Shadow
    private static ItemGroup BUILDING_BLOCKS;

    @Final
    @Shadow
    private static ItemGroup COLORED_BLOCKS;

    @Final
    @Shadow
    private static ItemGroup NATURAL;

    @Final
    @Shadow
    private static ItemGroup FUNCTIONAL;

    @Final
    @Shadow
    private static ItemGroup REDSTONE;

    @Final
    @Shadow
    private static ItemGroup HOTBAR;

    @Final
    @Shadow
    private static ItemGroup SEARCH;

    @Final
    @Shadow
    private static ItemGroup TOOLS;

    @Final
    @Shadow
    private static ItemGroup COMBAT;

    @Final
    @Shadow
    private static ItemGroup FOOD_AND_DRINK;

    @Final
    @Shadow
    private static ItemGroup INGREDIENTS;

    @Final
    @Shadow
    private static ItemGroup SPAWN_EGGS;

    @Final
    @Shadow
    private static ItemGroup OPERATOR;

    @Final
    @Shadow
    private static ItemGroup INVENTORY;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void update(CallbackInfo ci) {

        /*GROUPS = new ArrayList<>();

        GROUPS.addAll(List.of(BUILDING_BLOCKS, COLORED_BLOCKS, NATURAL, FUNCTIONAL, REDSTONE, HOTBAR, SEARCH, TOOLS, COMBAT, FOOD_AND_DRINK, INGREDIENTS, SPAWN_EGGS, INVENTORY, OPERATOR));
        GROUPS.addAll(MinecraftItemRegistry.getGroups());*/
    }

    @Inject(method = "updateEntries", at = @At("RETURN"))
    private static void injectEntries(ItemGroup.DisplayContext displayContext, CallbackInfo ci) {
        MinecraftItemRegistry.getGroups().stream().filter((group) -> {
            return group.getType() == ItemGroup.Type.CATEGORY;
        }).forEach((group) -> {
            group.updateEntries(displayContext);
        });
        MinecraftItemRegistry.getGroups().stream().filter((group) -> {
            return group.getType() != ItemGroup.Type.CATEGORY;
        }).forEach((group) -> {
            group.updateEntries(displayContext);
        });
    }
}
