package io.github.mmueller96.schematicshaderfix;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.vertices.ImmediateState;

public final class IrisHelper
{
	public static final Logger LOGGER = LoggerFactory.getLogger("schematic_shader_fix");

	private static final Set<PrimitiveTopology> DEBUG_LINE_TOPOLOGIES = Set.of(PrimitiveTopology.DEBUG_LINES,
	                                                                           PrimitiveTopology.DEBUG_LINE_STRIP);
	private static final Set<VertexFormat> COLORED_LINE_FORMATS = Set.of(DefaultVertexFormat.POSITION_COLOR,
	                                                                     DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH);
	private static final Object EXTEND_LOCK = new Object();

	private IrisHelper() {}

	public static boolean isShaderActive()
	{
		return IrisApi.getInstance().isShaderPackInUse();
	}

	/**
	 * Create a BufferBuilder for a schematic block layer while a shader pack is active.
	 * <p>
	 * Iris compiles Litematica's terrain pipelines with its extended TERRAIN vertex format, but it only
	 * extends a BLOCK BufferBuilder if it gets constructed while {@link ImmediateState#isRenderingLevel}
	 * is set. Litematica builds its chunk meshes on worker threads, so that flag is effectively random
	 * there, and any mesh built in the plain BLOCK format gets read with the wrong vertex stride
	 * ("exploding" geometry). Force the extension here instead.
	 */
	public static BufferBuilder createBlockBufferBuilder(ByteBufferBuilder alloc, PrimitiveTopology topology, VertexFormat format)
	{
		if (!format.equals(DefaultVertexFormat.BLOCK))
		{
			return new BufferBuilder(alloc, topology, format);
		}

		synchronized (EXTEND_LOCK)
		{
			final boolean wasRenderingLevel = ImmediateState.isRenderingLevel;
			final Boolean wasSkipping = ImmediateState.skipExtension.get();

			try
			{
				ImmediateState.skipExtension.set(false);
				ImmediateState.isRenderingLevel = true;
				return new BufferBuilder(alloc, topology, format);
			}
			finally
			{
				ImmediateState.isRenderingLevel = wasRenderingLevel;
				ImmediateState.skipExtension.set(wasSkipping);
			}
		}
	}

	/**
	 * MaLiLib assigns its debug line pipelines (plain GL lines, POSITION_COLOR_LINE_WIDTH) to the Iris
	 * LINES program. That shader expands lines into screen-space quads using a per-vertex normal, which
	 * these pipelines don't have, so the line endpoints get displaced and show up as diagonal "X" lines
	 * across every overlay face. Map them to the BASIC_COLOR shader instead, which only needs position
	 * and color and draws the GL lines as they are. Every other pipeline is assigned as MaLiLib intended.
	 */
	public static void assignMaLiLibPipeline(IrisApi api, RenderPipeline pipeline, IrisProgram program)
	{
		if (program == IrisProgram.LINES &&
		    DEBUG_LINE_TOPOLOGIES.contains(pipeline.getPrimitiveTopology()) &&
		    COLORED_LINE_FORMATS.contains(pipeline.getVertexFormatBinding(0)))
		{
			LOGGER.debug("Assigning MaLiLib pipeline {} to BASIC_COLOR instead of LINES", pipeline.getLocation());
			IrisPipelines.assignPipeline(pipeline, ShaderKey.BASIC_COLOR);
			return;
		}

		api.assignPipeline(pipeline, program);
	}
}
