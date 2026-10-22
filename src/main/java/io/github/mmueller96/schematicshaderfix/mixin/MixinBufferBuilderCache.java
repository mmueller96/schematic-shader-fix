package io.github.mmueller96.schematicshaderfix.mixin;

import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.render.schematic.BufferBuilderCache;
import io.github.mmueller96.schematicshaderfix.IrisHelper;

@Mixin(value = BufferBuilderCache.class, remap = false)
public abstract class MixinBufferBuilderCache
{
	@Shadow @Final private ConcurrentHashMap<ChunkSectionLayer, BufferBuilder> blockBufferBuilders;

	@Inject(method = "getBuilder(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;)Lcom/mojang/blaze3d/vertex/BufferBuilder;",
	        at = @At("HEAD"), cancellable = true)
	private void schematicshaderfix$createExtendedBuilder(ChunkSectionLayer layer, ByteBufferBuilder alloc, CallbackInfoReturnable<BufferBuilder> cir)
	{
		if (IrisHelper.isShaderActive())
		{
			cir.setReturnValue(this.blockBufferBuilders.computeIfAbsent(layer, key -> IrisHelper.createBlockBufferBuilder(alloc, PrimitiveTopology.QUADS, key.vertexFormat())));
		}
	}
}
