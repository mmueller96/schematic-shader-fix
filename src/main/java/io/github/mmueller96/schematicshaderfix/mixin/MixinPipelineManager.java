package io.github.mmueller96.schematicshaderfix.mixin;

import net.irisshaders.iris.pipeline.PipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.mmueller96.schematicshaderfix.IrisHelper;

@Mixin(value = PipelineManager.class, remap = false)
public abstract class MixinPipelineManager
{
	// Runs on every dimension change and shader reload, after all Iris programs got destroyed
	@Inject(method = "destroyPipeline", at = @At("TAIL"))
	private void schematicshaderfix$clearOverrideCache(CallbackInfo ci)
	{
		IrisHelper.clearPipelineOverrideCache();
	}
}
