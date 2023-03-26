package net.minecraft.item;

import net.minecraft.resource.featuretoggle.FeatureSet;

import java.util.Collection;
import java.util.Set;

public class MinecraftItemEntries implements ItemGroup.Entries {
    public final Collection<ItemStack> parentTabStacks = ItemStackSet.create();
    public final Set<ItemStack> searchTabStacks = ItemStackSet.create();
    private final ItemGroup group;
    private final FeatureSet enabledFeatures;

    @Override
    public void add(ItemStack stack, ItemGroup.StackVisibility visibility) {
        if (stack.getCount() != 1) {
            throw new IllegalArgumentException("Stack size must be exactly 1");
        } else {
            boolean bl = this.parentTabStacks.contains(stack) && visibility != ItemGroup.StackVisibility.SEARCH_TAB_ONLY;
            if (bl) {
                String var10002 = stack.toHoverableText().getString();
                throw new IllegalStateException("Accidentally adding the same item stack twice " + var10002 + " to a Creative Mode Tab: " + this.group.getDisplayName().getString());
            } else {
                if (stack.getItem().isEnabled(this.enabledFeatures)) {
                    switch (visibility) {
                        case PARENT_AND_SEARCH_TABS:
                            this.parentTabStacks.add(stack);
                            this.searchTabStacks.add(stack);
                            break;
                        case PARENT_TAB_ONLY:
                            this.parentTabStacks.add(stack);
                            break;
                        case SEARCH_TAB_ONLY:
                            this.searchTabStacks.add(stack);
                    }
                }

            }
        }
    }

    public MinecraftItemEntries(ItemGroup group, FeatureSet enabledFeatures) {
        this.group = group;
        this.enabledFeatures = enabledFeatures;
    }
}
