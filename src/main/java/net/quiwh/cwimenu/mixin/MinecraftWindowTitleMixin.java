package net.quiwh.cwimenu.mixin;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftWindowTitleMixin {
    @Shadow public abstract Window getWindow();

    @Inject(method = "updateTitle", at = @At("RETURN"))
    private void cwi$windowTitle(CallbackInfo ci) {
        getWindow().setTitle("Create Wasteland Industry");
    }
}
