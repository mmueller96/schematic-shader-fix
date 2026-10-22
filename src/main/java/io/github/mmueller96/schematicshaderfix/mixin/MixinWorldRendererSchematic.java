package io.github.mmueller96.schematicshaderfix.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import io.github.mmueller96.schematicshaderfix.IrisHelper;

@Mixin(value = WorldRendererSchematic.class, remap = false)
public abstract class MixinWorldRendererSchematic
{
	@Unique private boolean schematicshaderfix$builtWithShaders;

	@Shadow public abstract void loadRenderers(ProfilerFiller profiler);

	// Toggling a shader pack changes the vertex format of the chunk meshes, so they all need rebuilding
	@Inject(method = "setupTerrain", at = @At("HEAD"))
	private void schematicshaderfix$reloadOnShaderToggle(Camera camera, Frustum frustum, int frameCount, boolean playerSpectator,
	                                                     ProfilerFiller profiler, CallbackInfo ci)
	{
		boolean shadersActive = IrisHelper.isShaderActive();

		if (shadersActive != this.schematicshaderfix$builtWithShaders)
		{
			this.schematicshaderfix$builtWithShaders = shadersActive;
			this.loadRenderers(profiler);
		}
	}
}
