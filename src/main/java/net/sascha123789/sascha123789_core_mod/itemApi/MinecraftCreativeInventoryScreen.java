package net.sascha123789.sascha123789_core_mod.itemApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryListener;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipContext.Default;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.client.option.HotbarStorageEntry;
import net.minecraft.client.search.SearchManager;
import net.minecraft.client.search.SearchProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.item.ItemGroup.Row;
import net.minecraft.item.ItemGroup.Type;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Environment(EnvType.CLIENT)
public class MinecraftCreativeInventoryScreen extends AbstractInventoryScreen<MinecraftCreativeInventoryScreen.CreativeScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("textures/gui/container/creative_inventory/tabs.png");
    private static final String TAB_TEXTURE_PREFIX = "textures/gui/container/creative_inventory/tab_";
    private static final String CUSTOM_CREATIVE_LOCK_KEY = "CustomCreativeLock";
    private static final int ROWS_COUNT = 5;
    private static final int COLUMNS_COUNT = 9;
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_HEIGHT = 15;
    static final SimpleInventory INVENTORY = new SimpleInventory(45);
    private static final Text DELETE_ITEM_SLOT_TEXT = Text.translatable("inventory.binSlot");
    private static final int WHITE = 16777215;
    private static ItemGroup selectedTab = MinecraftItemRegistry.getGroups().get(0);
    private float scrollPosition;
    private boolean scrolling;
    private TextFieldWidget searchBox;
    @Nullable
    private List<Slot> slots;
    @Nullable
    private Slot deleteItemSlot;
    private CreativeInventoryListener listener;
    private boolean ignoreTypedCharacter;
    private boolean lastClickOutsideBounds;
    private final Set<TagKey<Item>> searchResultTags = new HashSet();
    private final boolean operatorTabEnabled;
    private int page;

    public MinecraftCreativeInventoryScreen(PlayerEntity player, FeatureSet enabledFeatures, boolean operatorTabEnabled, int page) {
        super(new MinecraftCreativeInventoryScreen.CreativeScreenHandler(player), player.getInventory(), ScreenTexts.EMPTY);
        player.currentScreenHandler = this.handler;
        this.passEvents = true;
        this.backgroundHeight = 136;
        this.backgroundWidth = 195;
        this.operatorTabEnabled = operatorTabEnabled;
        ItemGroups.updateDisplayContext(enabledFeatures, this.shouldShowOperatorTab(player), player.world.getRegistryManager());
        this.page = page;
    }

    private boolean shouldShowOperatorTab(PlayerEntity player) {
        return player.isCreativeLevelTwoOp() && this.operatorTabEnabled;
    }

    private void updateDisplayParameters(FeatureSet enabledFeatures, boolean showOperatorTab, RegistryWrapper.WrapperLookup wrapperLookup) {
        if (ItemGroups.updateDisplayContext(enabledFeatures, showOperatorTab, wrapperLookup)) {
            Iterator<MinecraftItemGroup> var4 = MinecraftItemRegistry.getGroups().iterator();

            while(true) {
                while(true) {
                    ItemGroup itemGroup;
                    Collection collection;
                    do {
                        if (!var4.hasNext()) {
                            return;
                        }

                        itemGroup = (ItemGroup)var4.next();
                        collection = itemGroup.getDisplayStacks();
                    } while(itemGroup != selectedTab);

                    if (itemGroup.getType() == Type.CATEGORY && collection.isEmpty()) {
                        this.setSelectedTab(MinecraftItemRegistry.getGroups().get(0));
                    } else {
                        this.refreshSelectedTab(collection);
                    }
                }
            }
        }
    }

    private void refreshSelectedTab(Collection<ItemStack> displayStacks) {
        int i = ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getRow(this.scrollPosition);
        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).itemList.clear();
        if (selectedTab.getType() == Type.SEARCH) {
            this.search();
        } else {
            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).itemList.addAll(displayStacks);
        }

        this.scrollPosition = ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getScrollPosition(i);
        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).scrollItems(this.scrollPosition);
    }

    public void handledScreenTick() {
        super.handledScreenTick();
        if (this.client != null) {
            if (this.client.player != null) {
                this.updateDisplayParameters(this.client.player.networkHandler.getEnabledFeatures(), this.shouldShowOperatorTab(this.client.player), this.client.player.world.getRegistryManager());
            }

            if (!this.client.interactionManager.hasCreativeInventory()) {
                this.client.setScreen(new InventoryScreen(this.client.player));
            } else {
                this.searchBox.tick();
            }

        }
    }

    protected void onMouseClick(@Nullable Slot slot, int slotId, int button, SlotActionType actionType) {
        if (this.isCreativeInventorySlot(slot)) {
            this.searchBox.setCursorToEnd();
            this.searchBox.setSelectionEnd(0);
        }

        boolean bl = actionType == SlotActionType.QUICK_MOVE;
        actionType = slotId == -999 && actionType == SlotActionType.PICKUP ? SlotActionType.THROW : actionType;
        ItemStack itemStack;
        if (slot == null && selectedTab.getType() != Type.INVENTORY && actionType != SlotActionType.QUICK_CRAFT) {
            if (!((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack().isEmpty() && this.lastClickOutsideBounds) {
                if (button == 0) {
                    this.client.player.dropItem(((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack(), true);
                    this.client.interactionManager.dropCreativeStack(((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack());
                    ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).setCursorStack(ItemStack.EMPTY);
                }

                if (button == 1) {
                    itemStack = ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack().split(1);
                    this.client.player.dropItem(itemStack, true);
                    this.client.interactionManager.dropCreativeStack(itemStack);
                }
            }
        } else {
            if (slot != null && !slot.canTakeItems(this.client.player)) {
                return;
            }

            if (slot == this.deleteItemSlot && bl) {
                for(int i = 0; i < this.client.player.playerScreenHandler.getStacks().size(); ++i) {
                    this.client.interactionManager.clickCreativeStack(ItemStack.EMPTY, i);
                }
            } else {
                ItemStack itemStack2;
                if (selectedTab.getType() == Type.INVENTORY) {
                    if (slot == this.deleteItemSlot) {
                        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).setCursorStack(ItemStack.EMPTY);
                    } else if (actionType == SlotActionType.THROW && slot != null && slot.hasStack()) {
                        itemStack = slot.takeStack(button == 0 ? 1 : slot.getStack().getMaxCount());
                        itemStack2 = slot.getStack();
                        this.client.player.dropItem(itemStack, true);
                        this.client.interactionManager.dropCreativeStack(itemStack);
                        this.client.interactionManager.clickCreativeStack(itemStack2, ((MinecraftCreativeInventoryScreen.CreativeSlot)slot).slot.id);
                    } else if (actionType == SlotActionType.THROW && !((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack().isEmpty()) {
                        this.client.player.dropItem(((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack(), true);
                        this.client.interactionManager.dropCreativeStack(((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack());
                        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).setCursorStack(ItemStack.EMPTY);
                    } else {
                        this.client.player.playerScreenHandler.onSlotClick(slot == null ? slotId : ((MinecraftCreativeInventoryScreen.CreativeSlot)slot).slot.id, button, actionType, this.client.player);
                        this.client.player.playerScreenHandler.sendContentUpdates();
                    }
                } else if (actionType != SlotActionType.QUICK_CRAFT && slot.inventory == INVENTORY) {
                    itemStack = ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack();
                    itemStack2 = slot.getStack();
                    ItemStack itemStack3;
                    if (actionType == SlotActionType.SWAP) {
                        if (!itemStack2.isEmpty()) {
                            itemStack3 = itemStack2.copy();
                            itemStack3.setCount(itemStack3.getMaxCount());
                            this.client.player.getInventory().setStack(button, itemStack3);
                            this.client.player.playerScreenHandler.sendContentUpdates();
                        }

                        return;
                    }

                    if (actionType == SlotActionType.CLONE) {
                        if (((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack().isEmpty() && slot.hasStack()) {
                            itemStack3 = slot.getStack().copy();
                            itemStack3.setCount(itemStack3.getMaxCount());
                            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).setCursorStack(itemStack3);
                        }

                        return;
                    }

                    if (actionType == SlotActionType.THROW) {
                        if (!itemStack2.isEmpty()) {
                            itemStack3 = itemStack2.copy();
                            itemStack3.setCount(button == 0 ? 1 : itemStack3.getMaxCount());
                            this.client.player.dropItem(itemStack3, true);
                            this.client.interactionManager.dropCreativeStack(itemStack3);
                        }

                        return;
                    }

                    if (!itemStack.isEmpty() && !itemStack2.isEmpty() && itemStack.isItemEqual(itemStack2) && ItemStack.areNbtEqual(itemStack, itemStack2)) {
                        if (button == 0) {
                            if (bl) {
                                itemStack.setCount(itemStack.getMaxCount());
                            } else if (itemStack.getCount() < itemStack.getMaxCount()) {
                                itemStack.increment(1);
                            }
                        } else {
                            itemStack.decrement(1);
                        }
                    } else if (!itemStack2.isEmpty() && itemStack.isEmpty()) {
                        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).setCursorStack(itemStack2.copy());
                        itemStack = ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack();
                        if (bl) {
                            itemStack.setCount(itemStack.getMaxCount());
                        }
                    } else if (button == 0) {
                        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).setCursorStack(ItemStack.EMPTY);
                    } else {
                        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getCursorStack().decrement(1);
                    }
                } else if (this.handler != null) {
                    itemStack = slot == null ? ItemStack.EMPTY : ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getSlot(slot.id).getStack();
                    ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).onSlotClick(slot == null ? slotId : slot.id, button, actionType, this.client.player);
                    if (ScreenHandler.unpackQuickCraftStage(button) == 2) {
                        for(int j = 0; j < 9; ++j) {
                            this.client.interactionManager.clickCreativeStack(((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getSlot(45 + j).getStack(), 36 + j);
                        }
                    } else if (slot != null) {
                        itemStack2 = ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getSlot(slot.id).getStack();
                        this.client.interactionManager.clickCreativeStack(itemStack2, slot.id - ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).slots.size() + 9 + 36);
                        int k = 45 + button;
                        if (actionType == SlotActionType.SWAP) {
                            this.client.interactionManager.clickCreativeStack(itemStack, k - ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).slots.size() + 9 + 36);
                        } else if (actionType == SlotActionType.THROW && !itemStack.isEmpty()) {
                            ItemStack itemStack4 = itemStack.copy();
                            itemStack4.setCount(button == 0 ? 1 : itemStack4.getMaxCount());
                            this.client.player.dropItem(itemStack4, true);
                            this.client.interactionManager.dropCreativeStack(itemStack4);
                        }

                        this.client.player.playerScreenHandler.sendContentUpdates();
                    }
                }
            }
        }

    }

    private boolean isCreativeInventorySlot(@Nullable Slot slot) {
        return slot != null && slot.inventory == INVENTORY;
    }

    protected void init() {
        if (this.client.interactionManager.hasCreativeInventory()) {
            super.init();
            TextRenderer var10003 = this.textRenderer;
            int var10004 = this.x + 82;
            int var10005 = this.y + 6;
            Objects.requireNonNull(this.textRenderer);
            this.searchBox = new TextFieldWidget(var10003, var10004, var10005, 80, 9, Text.translatable("itemGroup.search"));
            this.searchBox.setMaxLength(50);
            this.searchBox.setDrawsBackground(false);
            this.searchBox.setVisible(false);
            this.searchBox.setEditableColor(16777215);
            this.addSelectableChild(this.searchBox);
            ItemGroup itemGroup = selectedTab;
            selectedTab = MinecraftItemRegistry.getGroups().get(0);
            this.setSelectedTab(itemGroup);
            this.client.player.playerScreenHandler.removeListener(this.listener);
            this.listener = new CreativeInventoryListener(this.client);
            this.client.player.playerScreenHandler.addListener(this.listener);
            if (!selectedTab.shouldDisplay()) {
                this.setSelectedTab(MinecraftItemRegistry.getGroups().get(0));
            }

            this.addDrawableChild(ButtonWidget.builder(Text.literal("<").formatted(Formatting.BLACK), (button) -> {
                this.client.setScreen(new CreativeInventoryScreen(this.client.player, FeatureFlags.VANILLA_FEATURES, true));
            }).dimensions((this.x - 25), (this.y - 25), 20, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(">").formatted(Formatting.BLACK), (button) -> {
                this.client.setScreen(new MinecraftCreativeInventoryScreen(this.client.player, FeatureFlags.VANILLA_FEATURES, false, 1));
            }).dimensions((this.x + 170 + 25 + 5), (this.y - 25), 20, 20).build());
        } else {
            this.client.setScreen(new InventoryScreen(this.client.player));
        }

    }

    public void resize(MinecraftClient client, int width, int height) {
        int i = ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getRow(this.scrollPosition);
        String string = this.searchBox.getText();
        this.init(client, width, height);
        this.searchBox.setText(string);
        if (!this.searchBox.getText().isEmpty()) {
            this.search();
        }

        this.scrollPosition = ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getScrollPosition(i);
        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).scrollItems(this.scrollPosition);
    }

    public void removed() {
        super.removed();
        if (this.client.player != null && this.client.player.getInventory() != null) {
            this.client.player.playerScreenHandler.removeListener(this.listener);
        }

    }

    public boolean charTyped(char chr, int modifiers) {
        if (this.ignoreTypedCharacter) {
            return false;
        } else if (selectedTab.getType() != Type.SEARCH) {
            return false;
        } else {
            String string = this.searchBox.getText();
            if (this.searchBox.charTyped(chr, modifiers)) {
                if (!Objects.equals(string, this.searchBox.getText())) {
                    this.search();
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.ignoreTypedCharacter = false;
        if (selectedTab.getType() != Type.SEARCH) {
            if (this.client.options.chatKey.matchesKey(keyCode, scanCode)) {
                this.ignoreTypedCharacter = true;
                this.setSelectedTab(ItemGroups.getSearchGroup());
                return true;
            } else {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        } else {
            boolean bl = !this.isCreativeInventorySlot(this.focusedSlot) || this.focusedSlot.hasStack();
            boolean bl2 = InputUtil.fromKeyCode(keyCode, scanCode).toInt().isPresent();
            if (bl && bl2 && this.handleHotbarKeyPressed(keyCode, scanCode)) {
                this.ignoreTypedCharacter = true;
                return true;
            } else {
                String string = this.searchBox.getText();
                if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                    if (!Objects.equals(string, this.searchBox.getText())) {
                        this.search();
                    }

                    return true;
                } else {
                    return this.searchBox.isFocused() && this.searchBox.isVisible() && keyCode != 256 ? true : super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.ignoreTypedCharacter = false;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void search() {
        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).itemList.clear();
        this.searchResultTags.clear();
        String string = this.searchBox.getText();
        if (string.isEmpty()) {
            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).itemList.addAll(selectedTab.getDisplayStacks());
        } else {
            SearchProvider searchProvider;

                searchProvider = this.client.getSearchProvider(SearchManager.ITEM_TOOLTIP);


            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).itemList.addAll(searchProvider.findAll(string.toLowerCase(Locale.ROOT)));
        }

        this.scrollPosition = 0.0F;
        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).scrollItems(0.0F);
    }

    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        if (selectedTab.shouldRenderName()) {
            this.textRenderer.draw(matrices, selectedTab.getDisplayName(), 8.0F, 6.0F, 4210752);
        }

    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            double d = mouseX - (double)this.x;
            double e = mouseY - (double)this.y;
            Iterator var10 = MinecraftItemRegistry.getGroups().iterator();

            while(var10.hasNext()) {
                ItemGroup itemGroup = (ItemGroup)var10.next();
                if (this.isClickInTab(itemGroup, d, e)) {
                    return true;
                }
            }

            if (selectedTab.getType() != Type.INVENTORY && this.isClickInScrollbar(mouseX, mouseY)) {
                this.scrolling = this.hasScrollbar();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            double d = mouseX - (double)this.x;
            double e = mouseY - (double)this.y;
            this.scrolling = false;
            Iterator var10 = MinecraftItemRegistry.getGroups().iterator();

            while(var10.hasNext()) {
                ItemGroup itemGroup = (ItemGroup)var10.next();
                if (this.isClickInTab(itemGroup, d, e)) {
                    this.setSelectedTab(itemGroup);
                    return true;
                }
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean hasScrollbar() {
        return selectedTab.hasScrollbar() && ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).shouldShowScrollbar();
    }

    private void setSelectedTab(ItemGroup group) {
        ItemGroup itemGroup = selectedTab;
        selectedTab = group;
        this.cursorDragSlots.clear();
        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).itemList.clear();
        this.endTouchDrag();
        int i;
        int j;
        if (selectedTab.getType() == Type.HOTBAR) {
            HotbarStorage hotbarStorage = this.client.getCreativeHotbarStorage();

            for(i = 0; i < 9; ++i) {
                HotbarStorageEntry hotbarStorageEntry = hotbarStorage.getSavedHotbar(i);
                if (hotbarStorageEntry.isEmpty()) {
                    for(j = 0; j < 9; ++j) {
                        if (j == i) {
                            ItemStack itemStack = new ItemStack(Items.PAPER);
                            itemStack.getOrCreateSubNbt("CustomCreativeLock");
                            Text text = this.client.options.hotbarKeys[i].getBoundKeyLocalizedText();
                            Text text2 = this.client.options.saveToolbarActivatorKey.getBoundKeyLocalizedText();
                            itemStack.setCustomName(Text.translatable("inventory.hotbarInfo", new Object[]{text2, text}));
                            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).itemList.add(itemStack);
                        } else {
                            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).itemList.add(ItemStack.EMPTY);
                        }
                    }
                } else {
                    ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).itemList.addAll(hotbarStorageEntry);
                }
            }
        } else if (selectedTab.getType() == Type.CATEGORY) {
            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).itemList.addAll(selectedTab.getDisplayStacks());
        }

        if (selectedTab.getType() == Type.INVENTORY) {
            ScreenHandler screenHandler = this.client.player.playerScreenHandler;
            if (this.slots == null) {
                this.slots = ImmutableList.copyOf(((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).slots);
            }

            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).slots.clear();

            for(i = 0; i < screenHandler.slots.size(); ++i) {
                int n;
                int k;
                int l;
                int m;
                if (i >= 5 && i < 9) {
                    k = i - 5;
                    l = k / 2;
                    m = k % 2;
                    n = 54 + l * 54;
                    j = 6 + m * 27;
                } else if (i >= 0 && i < 5) {
                    n = -2000;
                    j = -2000;
                } else if (i == 45) {
                    n = 35;
                    j = 20;
                } else {
                    k = i - 9;
                    l = k % 9;
                    m = k / 9;
                    n = 9 + l * 18;
                    if (i >= 36) {
                        j = 112;
                    } else {
                        j = 54 + m * 18;
                    }
                }

                Slot slot = new MinecraftCreativeInventoryScreen.CreativeSlot((Slot)screenHandler.slots.get(i), i, n, j);
                ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).slots.add(slot);
            }

            this.deleteItemSlot = new Slot(INVENTORY, 0, 173, 112);
            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).slots.add(this.deleteItemSlot);
        } else if (itemGroup.getType() == Type.INVENTORY) {
            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).slots.clear();
            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).slots.addAll(this.slots);
            this.slots = null;
        }

        if (selectedTab.getType() == Type.SEARCH) {
            this.searchBox.setVisible(true);
            this.searchBox.setFocusUnlocked(false);
            this.searchBox.setFocused(true);
            if (itemGroup != group) {
                this.searchBox.setText("");
            }

            this.search();
        } else {
            this.searchBox.setVisible(false);
            this.searchBox.setFocusUnlocked(true);
            this.searchBox.setFocused(false);
            this.searchBox.setText("");
        }

        this.scrollPosition = 0.0F;
        ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).scrollItems(0.0F);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.hasScrollbar()) {
            return false;
        } else {
            this.scrollPosition = ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).getScrollPosition(this.scrollPosition, amount);
            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).scrollItems(this.scrollPosition);
            return true;
        }
    }

    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
        boolean bl = mouseX < (double)left || mouseY < (double)top || mouseX >= (double)(left + this.backgroundWidth) || mouseY >= (double)(top + this.backgroundHeight);
        this.lastClickOutsideBounds = bl && !this.isClickInTab(selectedTab, mouseX, mouseY);
        return this.lastClickOutsideBounds;
    }

    protected boolean isClickInScrollbar(double mouseX, double mouseY) {
        int i = this.x;
        int j = this.y;
        int k = i + 175;
        int l = j + 18;
        int m = k + 14;
        int n = l + 112;
        return mouseX >= (double)k && mouseY >= (double)l && mouseX < (double)m && mouseY < (double)n;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.scrolling) {
            int i = this.y + 18;
            int j = i + 112;
            this.scrollPosition = ((float)mouseY - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
            this.scrollPosition = MathHelper.clamp(this.scrollPosition, 0.0F, 1.0F);
            ((MinecraftCreativeInventoryScreen.CreativeScreenHandler)this.handler).scrollItems(this.scrollPosition);
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        if(page == 0) {
            List<ItemGroup> vanilla = List.of(MinecraftItemGroups.BUILDING_BLOCKS, MinecraftItemGroups.INVENTORY, MinecraftItemGroups.INGREDIENTS, MinecraftItemGroups.COLORED_BLOCKS, MinecraftItemGroups.COMBAT, MinecraftItemGroups.FOOD_AND_DRINK, MinecraftItemGroups.FUNCTIONAL, MinecraftItemGroups.HOTBAR, MinecraftItemGroups.NATURAL, MinecraftItemGroups.OPERATOR, MinecraftItemGroups.REDSTONE, MinecraftItemGroups.SEARCH, MinecraftItemGroups.SPAWN_EGGS, MinecraftItemGroups.TOOLS);

            for(ItemGroup el: vanilla) {
                if (this.renderTabTooltipIfHovered(matrices, el, mouseX, mouseY)) {
                    break;
                }
            }
        } else if(page == 1) {
            List<MinecraftItemGroup> groups = MinecraftItemRegistry.getGroups();

            for(MinecraftItemGroup group: groups) {
                if (this.renderTabTooltipIfHovered(matrices, group, mouseX, mouseY)) {
                    break;
                }
            }
        }

        if (this.deleteItemSlot != null && selectedTab.getType() == Type.INVENTORY && this.isPointWithinBounds(this.deleteItemSlot.x, this.deleteItemSlot.y, 16, 16, (double)mouseX, (double)mouseY)) {
            this.renderTooltip(matrices, DELETE_ITEM_SLOT_TEXT, mouseX, mouseY);
        }

        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    protected void renderTooltip(MatrixStack matrices, ItemStack stack, int x, int y) {
        boolean bl = this.focusedSlot != null && this.focusedSlot instanceof MinecraftCreativeInventoryScreen.LockableSlot;
        boolean bl2 = selectedTab.getType() == Type.CATEGORY;
        boolean bl3 = selectedTab.getType() == Type.SEARCH;
        TooltipContext.Default default_ = this.client.options.advancedItemTooltips ? Default.ADVANCED : Default.BASIC;
        TooltipContext tooltipContext = bl ? default_.withCreative() : default_;
        List<Text> list = stack.getTooltip(this.client.player, tooltipContext);
        Object list2;
        if (bl2 && bl) {
            list2 = list;
        } else {
            list2 = Lists.newArrayList(list);

            int i = 1;
            Iterator var13 = MinecraftItemRegistry.getGroups().iterator();

            while(var13.hasNext()) {
                ItemGroup itemGroup = (ItemGroup)var13.next();
                if (itemGroup.getType() != Type.SEARCH && itemGroup.contains(stack)) {
                    ((List)list2).add(i++, itemGroup.getDisplayName().copy().formatted(Formatting.BLUE));
                }
            }
        }

        this.renderTooltip(matrices, (List)list2, stack.getTooltipData(), x, y);
    }

    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        Iterator var5 = MinecraftItemRegistry.getGroups().iterator();

        while(var5.hasNext()) {
            ItemGroup itemGroup = (ItemGroup)var5.next();
            RenderSystem.setShaderTexture(0, TEXTURE);
            if (itemGroup != selectedTab) {
                this.renderTabIcon(matrices, itemGroup);
            }
        }

        RenderSystem.setShaderTexture(0, new Identifier("textures/gui/container/creative_inventory/tab_" + selectedTab.getTexture()));
        drawTexture(matrices, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
        this.searchBox.render(matrices, mouseX, mouseY, delta);
        int i = this.x + 175;
        int j = this.y + 18;
        int k = j + 112;
        RenderSystem.setShaderTexture(0, TEXTURE);
        if (selectedTab.hasScrollbar()) {
            drawTexture(matrices, i, j + (int)((float)(k - j - 17) * this.scrollPosition), 232 + (this.hasScrollbar() ? 0 : 12), 0, 12, 15);
        }

        this.renderTabIcon(matrices, selectedTab);
        if (selectedTab.getType() == Type.INVENTORY) {
            InventoryScreen.drawEntity(matrices, this.x + 88, this.y + 45, 20, (float)(this.x + 88 - mouseX), (float)(this.y + 45 - 30 - mouseY), this.client.player);
        }

    }

    private int getTabX(ItemGroup group) {
        int i = group.getColumn();
        boolean j = true;
        int k = 27 * i;
        if (group.isSpecial()) {
            k = this.backgroundWidth - 27 * (7 - i) + 1;
        }

        return k;
    }

    private int getTabY(ItemGroup group) {
        int i = 0;
        if (group.getRow() == Row.TOP) {
            i -= 32;
        } else {
            i += this.backgroundHeight;
        }

        return i;
    }

    protected boolean isClickInTab(ItemGroup group, double mouseX, double mouseY) {
        int i = this.getTabX(group);
        int j = this.getTabY(group);
        return mouseX >= (double)i && mouseX <= (double)(i + 26) && mouseY >= (double)j && mouseY <= (double)(j + 32);
    }

    protected boolean renderTabTooltipIfHovered(MatrixStack matrices, ItemGroup group, int mouseX, int mouseY) {
        int i = this.getTabX(group);
        int j = this.getTabY(group);
        if (this.isPointWithinBounds(i + 3, j + 3, 21, 27, (double)mouseX, (double)mouseY)) {
            this.renderTooltip(matrices, group.getDisplayName(), mouseX, mouseY);
            return true;
        } else {
            return false;
        }
    }

    protected void renderTabIcon(MatrixStack matrices, ItemGroup group) {
        boolean bl = group == selectedTab;
        boolean bl2 = group.getRow() == Row.TOP;
        int i = group.getColumn();
        int j = i * 26;
        int k = 0;
        int l = this.x + this.getTabX(group);
        int m = this.y;
        boolean n = true;
        if (bl) {
            k += 32;
        }

        if (bl2) {
            m -= 28;
        } else {
            k += 64;
            m += this.backgroundHeight - 4;
        }

        drawTexture(matrices, l, m, j, k, 26, 32);
        matrices.push();
        matrices.translate(0.0F, 0.0F, 100.0F);
        l += 5;
        m += 8 + (bl2 ? 1 : -1);
        ItemStack itemStack = group.getIcon();
        this.itemRenderer.renderInGuiWithOverrides(matrices, itemStack, l, m);
        this.itemRenderer.renderGuiItemOverlay(matrices, this.textRenderer, itemStack, l, m);
        matrices.pop();
    }

    public boolean isInventoryTabSelected() {
        return selectedTab.getType() == Type.INVENTORY;
    }

    public static void onHotbarKeyPress(MinecraftClient client, int index, boolean restore, boolean save) {
        ClientPlayerEntity clientPlayerEntity = client.player;
        HotbarStorage hotbarStorage = client.getCreativeHotbarStorage();
        HotbarStorageEntry hotbarStorageEntry = hotbarStorage.getSavedHotbar(index);
        int i;
        if (restore) {
            for(i = 0; i < PlayerInventory.getHotbarSize(); ++i) {
                ItemStack itemStack = (ItemStack)hotbarStorageEntry.get(i);
                ItemStack itemStack2 = itemStack.isItemEnabled(clientPlayerEntity.world.getEnabledFeatures()) ? itemStack.copy() : ItemStack.EMPTY;
                clientPlayerEntity.getInventory().setStack(i, itemStack2);
                client.interactionManager.clickCreativeStack(itemStack2, 36 + i);
            }

            clientPlayerEntity.playerScreenHandler.sendContentUpdates();
        } else if (save) {
            for(i = 0; i < PlayerInventory.getHotbarSize(); ++i) {
                hotbarStorageEntry.set(i, clientPlayerEntity.getInventory().getStack(i).copy());
            }

            Text text = client.options.hotbarKeys[index].getBoundKeyLocalizedText();
            Text text2 = client.options.loadToolbarActivatorKey.getBoundKeyLocalizedText();
            Text text3 = Text.translatable("inventory.hotbarSaved", new Object[]{text2, text});
            client.inGameHud.setOverlayMessage(text3, false);
            client.getNarratorManager().narrate(text3);
            hotbarStorage.save();
        }

    }

    @Environment(EnvType.CLIENT)
    public static class CreativeScreenHandler extends ScreenHandler {
        public final DefaultedList<ItemStack> itemList = DefaultedList.of();
        private final ScreenHandler parent;

        public CreativeScreenHandler(PlayerEntity player) {
            super((ScreenHandlerType)null, 0);
            this.parent = player.playerScreenHandler;
            PlayerInventory playerInventory = player.getInventory();

            int i;
            for(i = 0; i < 5; ++i) {
                for(int j = 0; j < 9; ++j) {
                    this.addSlot(new MinecraftCreativeInventoryScreen.LockableSlot(MinecraftCreativeInventoryScreen.INVENTORY, i * 9 + j, 9 + j * 18, 18 + i * 18));
                }
            }

            for(i = 0; i < 9; ++i) {
                this.addSlot(new Slot(playerInventory, i, 9 + i * 18, 112));
            }

            this.scrollItems(0.0F);
        }

        public boolean canUse(PlayerEntity player) {
            return true;
        }

        protected int getOverflowRows() {
            return MathHelper.ceilDiv(this.itemList.size(), 9) - 5;
        }

        protected int getRow(float scroll) {
            return Math.max((int)((double)(scroll * (float)this.getOverflowRows()) + 0.5), 0);
        }

        protected float getScrollPosition(int row) {
            return MathHelper.clamp((float)row / (float)this.getOverflowRows(), 0.0F, 1.0F);
        }

        protected float getScrollPosition(float current, double amount) {
            return MathHelper.clamp(current - (float)(amount / (double)this.getOverflowRows()), 0.0F, 1.0F);
        }

        public void scrollItems(float position) {
            int i = this.getRow(position);

            for(int j = 0; j < 5; ++j) {
                for(int k = 0; k < 9; ++k) {
                    int l = k + (j + i) * 9;
                    if (l >= 0 && l < this.itemList.size()) {
                        MinecraftCreativeInventoryScreen.INVENTORY.setStack(k + j * 9, (ItemStack)this.itemList.get(l));
                    } else {
                        MinecraftCreativeInventoryScreen.INVENTORY.setStack(k + j * 9, ItemStack.EMPTY);
                    }
                }
            }

        }

        public boolean shouldShowScrollbar() {
            return this.itemList.size() > 45;
        }

        public ItemStack quickMove(PlayerEntity player, int slot) {
            if (slot >= this.slots.size() - 9 && slot < this.slots.size()) {
                Slot slot2 = (Slot)this.slots.get(slot);
                if (slot2 != null && slot2.hasStack()) {
                    slot2.setStack(ItemStack.EMPTY);
                }
            }

            return ItemStack.EMPTY;
        }

        public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
            return slot.inventory != MinecraftCreativeInventoryScreen.INVENTORY;
        }

        public boolean canInsertIntoSlot(Slot slot) {
            return slot.inventory != MinecraftCreativeInventoryScreen.INVENTORY;
        }

        public ItemStack getCursorStack() {
            return this.parent.getCursorStack();
        }

        public void setCursorStack(ItemStack stack) {
            this.parent.setCursorStack(stack);
        }
    }

    @Environment(EnvType.CLIENT)
    private static class CreativeSlot extends Slot {
        final Slot slot;

        public CreativeSlot(Slot slot, int invSlot, int x, int y) {
            super(slot.inventory, invSlot, x, y);
            this.slot = slot;
        }

        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            this.slot.onTakeItem(player, stack);
        }

        public boolean canInsert(ItemStack stack) {
            return this.slot.canInsert(stack);
        }

        public ItemStack getStack() {
            return this.slot.getStack();
        }

        public boolean hasStack() {
            return this.slot.hasStack();
        }

        public void setStack(ItemStack stack) {
            this.slot.setStack(stack);
        }

        public void setStackNoCallbacks(ItemStack stack) {
            this.slot.setStackNoCallbacks(stack);
        }

        public void markDirty() {
            this.slot.markDirty();
        }

        public int getMaxItemCount() {
            return this.slot.getMaxItemCount();
        }

        public int getMaxItemCount(ItemStack stack) {
            return this.slot.getMaxItemCount(stack);
        }

        @Nullable
        public Pair<Identifier, Identifier> getBackgroundSprite() {
            return this.slot.getBackgroundSprite();
        }

        public ItemStack takeStack(int amount) {
            return this.slot.takeStack(amount);
        }

        public boolean isEnabled() {
            return this.slot.isEnabled();
        }

        public boolean canTakeItems(PlayerEntity playerEntity) {
            return this.slot.canTakeItems(playerEntity);
        }
    }

    @Environment(EnvType.CLIENT)
    static class LockableSlot extends Slot {
        public LockableSlot(Inventory inventory, int i, int j, int k) {
            super(inventory, i, j, k);
        }

        public boolean canTakeItems(PlayerEntity playerEntity) {
            ItemStack itemStack = this.getStack();
            if (super.canTakeItems(playerEntity) && !itemStack.isEmpty()) {
                return itemStack.isItemEnabled(playerEntity.world.getEnabledFeatures()) && itemStack.getSubNbt("CustomCreativeLock") == null;
            } else {
                return itemStack.isEmpty();
            }
        }
    }
}
