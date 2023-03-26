package net.sascha123789.sascha123789_core_mod.itemApi;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.MinecraftItemGroup;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MinecraftItemRegistry {
    private static final List<MinecraftItemGroup> groups;

    static {
        groups = new ArrayList<>();
    }

    public static void regGroup(MinecraftItemGroup group) {
        groups.add(group);
    }

    public static void init() {

    }

    public static List<MinecraftItemGroup> getGroups() {
        int len = groups.size();
        int cnt = 14;
        int size = 0;
        List<ItemGroup> arr = List.of(MinecraftItemGroups.BUILDING_BLOCKS, MinecraftItemGroups.INVENTORY, MinecraftItemGroups.INGREDIENTS, MinecraftItemGroups.COLORED_BLOCKS, MinecraftItemGroups.COMBAT, MinecraftItemGroups.FOOD_AND_DRINK, MinecraftItemGroups.FUNCTIONAL, MinecraftItemGroups.HOTBAR, MinecraftItemGroups.NATURAL, MinecraftItemGroups.OPERATOR, MinecraftItemGroups.REDSTONE, MinecraftItemGroups.SEARCH, MinecraftItemGroups.SPAWN_EGGS, MinecraftItemGroups.TOOLS);

        for(int i = 0; i < len; i++) {
            MinecraftItemGroup group = groups.get(i);

            if(arr.contains((ItemGroup) group)) {
                group.setPage(0);
                continue;
            }

            ItemGroup.Row row;
            int column = size;

            if(size > (cnt / 2)) {
                row = ItemGroup.Row.BOTTOM;
            } else {
                row = ItemGroup.Row.TOP;
            }
            group.setPage(1);

            Class<?> cls = group.getClass().getSuperclass();
            Field rowF = null;
            Field columnF = null;

            try {
                rowF = cls.getDeclaredField("row");
                columnF = cls.getDeclaredField("column");

                rowF.setAccessible(true);
                columnF.setAccessible(true);

                rowF.set(group, row);
                columnF.set(group, column);
            } catch(Exception e) {
                e.printStackTrace();
            }

            size++;
        }

        return groups;
    }
}
