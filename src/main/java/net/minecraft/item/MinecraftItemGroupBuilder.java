package net.minecraft.item;

import net.minecraft.text.Text;

import java.util.function.Supplier;

public class MinecraftItemGroupBuilder {
    private static final ItemGroup.EntryCollector EMPTY_ENTRIES = (displayContext, entries) -> {
    };
    private final ItemGroup.Row row;
    private final int column;
    private Text displayName = Text.empty();
    private Supplier<ItemStack> iconSupplier = () -> {
        return ItemStack.EMPTY;
    };
    private ItemGroup.EntryCollector entryCollector;
    private boolean scrollbar;
    private boolean renderName;
    private boolean special;
    private ItemGroup.Type type;
    private String texture;

    public MinecraftItemGroupBuilder(ItemGroup.Row row, int column) {
        this.entryCollector = EMPTY_ENTRIES;
        this.scrollbar = true;
        this.renderName = true;
        this.special = false;
        this.type = ItemGroup.Type.CATEGORY;
        this.texture = "items.png";
        this.row = row;
        this.column = column;
    }

    public MinecraftItemGroupBuilder displayName(Text displayName) {
        this.displayName = displayName;
        return this;
    }

    public MinecraftItemGroupBuilder icon(Supplier<ItemStack> iconSupplier) {
        this.iconSupplier = iconSupplier;
        return this;
    }

    public MinecraftItemGroupBuilder entries(ItemGroup.EntryCollector entryCollector) {
        this.entryCollector = entryCollector;
        return this;
    }

    public MinecraftItemGroupBuilder special() {
        this.special = true;
        return this;
    }

    public MinecraftItemGroupBuilder noRenderedName() {
        this.renderName = false;
        return this;
    }

    public MinecraftItemGroupBuilder noScrollbar() {
        this.scrollbar = false;
        return this;
    }

    protected MinecraftItemGroupBuilder type(ItemGroup.Type type) {
        this.type = type;
        return this;
    }

    public MinecraftItemGroupBuilder texture(String texture) {
        this.texture = texture;
        return this;
    }

    public MinecraftItemGroup build() {
        if ((this.type == ItemGroup.Type.HOTBAR || this.type == ItemGroup.Type.INVENTORY) && this.entryCollector != EMPTY_ENTRIES) {
            throw new IllegalStateException("Special tabs can't have display items");
        } else {
            MinecraftItemGroup itemGroup = new MinecraftItemGroup(this.row, this.column, this.type, this.displayName, this.iconSupplier, this.entryCollector, 0);
            itemGroup.special = this.special;
            itemGroup.renderName = this.renderName;
            itemGroup.scrollbar = this.scrollbar;
            itemGroup.texture = this.texture;
            return itemGroup;
        }
    }
}
