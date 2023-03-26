package net.sascha123789.sascha123789_core_mod.itemApi;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.text.Text;
import net.sascha123789.sascha123789_core_mod.mixin.ItemGroupsMixin;

public class MinecraftItemGroups {
    public static ItemGroup BUILDING_BLOCKS;
    public static ItemGroup COLORED_BLOCKS;
    public static ItemGroup NATURAL;
    public static ItemGroup FUNCTIONAL;
    public static ItemGroup REDSTONE;
    public static ItemGroup HOTBAR;
    public static ItemGroup SEARCH;
    public static ItemGroup TOOLS;
    public static ItemGroup COMBAT;
    public static ItemGroup FOOD_AND_DRINK;
    public static ItemGroup INGREDIENTS;
    public static ItemGroup SPAWN_EGGS;
    public static ItemGroup OPERATOR;
    public static ItemGroup INVENTORY;

    static {
        for(ItemGroup el: ItemGroups.getGroups()) {
            if(el.getDisplayName().contains(Text.translatable("itemGroup.buildingBlocks"))) {
                BUILDING_BLOCKS = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.coloredBlocks"))) {
                COLORED_BLOCKS = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.natural"))) {
                NATURAL = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.functional"))) {
                FUNCTIONAL = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.redstone"))) {
                REDSTONE = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.hotbar"))) {
                HOTBAR = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.search"))) {
                SEARCH = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.tools"))) {
                TOOLS = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.combat"))) {
                COMBAT = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.foodAndDrink"))) {
                FOOD_AND_DRINK = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.ingredients"))) {
                INGREDIENTS = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.spawnEggs"))) {
                SPAWN_EGGS = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.op"))) {
                OPERATOR = el;
            } else if(el.getDisplayName().contains(Text.translatable("itemGroup.inventory"))) {
                INVENTORY = el;
            }
        }
    }
}
