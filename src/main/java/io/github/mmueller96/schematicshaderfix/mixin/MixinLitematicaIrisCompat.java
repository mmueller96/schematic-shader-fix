package io.github.mmueller96.schematicshaderfix.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.litematica.compat.iris.IrisCompat;
import io.github.mmueller96.schematicshaderfix.IrisHelper;

@Mixin(value = IrisCompat.class, remap = false)
public abstract class MixinLitematicaIrisCompat
{
	// Iris refuses to re-assign a pipeline later on, so the assignment has to be changed at the source
	@Redirect(method = "registerPipelines",
	          at = @At(value = "INVOKE",
	                   target = "Lnet/irisshaders/iris/api/v0/IrisApi;assignPipeline(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/irisshaders/iris/api/v0/IrisProgram;)V"))
	private static void schematicshaderfix$assignPipeline(IrisApi api, RenderPipeline pipeline, IrisProgram program)
	{
		IrisHelper.assignLitematicaPipeline(api, pipeline, program);
	}
}
