package net.minecraft.item;

import java.util.List;

public class MinecraftEntryCollector implements ItemGroup.EntryCollector {
    private List<Item> items;

    public MinecraftEntryCollector(Item... items) {
        this.items = List.of(items);
    }

    @Override
    public void accept(ItemGroup.DisplayContext displayContext, ItemGroup.Entries entries) {
        for(Item item: this.items) {
            entries.add(new ItemStack(item));
        }
    }
}
