package net.sascha123789.sascha123789_core_mod.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.resource.featuretoggle.FeatureFlag;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.sascha123789.sascha123789_core_mod.itemApi.MinecraftCreativeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler> {

    public CreativeInventoryScreenMixin(CreativeInventoryScreen.CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addButtons(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<").formatted(Formatting.BLACK), (button) -> {
            this.client.setScreen(new CreativeInventoryScreen(this.client.player, FeatureFlags.VANILLA_FEATURES, true));
        }).dimensions((this.x - 25), (this.y - 25), 20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">").formatted(Formatting.BLACK), (button) -> {
            this.client.setScreen(new MinecraftCreativeInventoryScreen(this.client.player, FeatureFlags.VANILLA_FEATURES, false, 1));
        }).dimensions((this.x + 170 + 25 + 5), (this.y - 25), 20, 20).build());
    }
}
