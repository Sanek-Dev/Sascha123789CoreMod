package net.minecraft.item;

import net.minecraft.text.Text;

import java.util.function.Supplier;

public class MinecraftItemGroup extends ItemGroup {
    private int page;

    MinecraftItemGroup(Row row, int column, Type type, Text displayName, Supplier<ItemStack> iconSupplier, EntryCollector entryCollector, int page) {
        super(row, column, type, displayName, iconSupplier, entryCollector);
        this.page = page;
    }

    public static MinecraftItemGroupBuilder createGroup() {
        return new MinecraftItemGroupBuilder(Row.TOP, 7);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
