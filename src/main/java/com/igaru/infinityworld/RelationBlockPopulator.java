package com.igaru.infinityworld;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

public class RelationBlockPopulator extends BlockPopulator
{
	private InfinityWorld plugin;
	private LayerInfo layer;
	private World relationWorld;

	public RelationBlockPopulator(InfinityWorld plugin,World world)
	{
		this.plugin = plugin;
		layer = plugin.getLayerInfo(world);
		relationWorld = plugin.getIFWorld(layer.base, layer.number + (layer.number<0?1:layer.number>0?-1:0));

	}

	@Override
	public void populate(World world, Random random, Chunk chunk)
	{
		ChunkSnapshot relationChunk = relationWorld.getChunkAt(chunk.getX(), chunk.getZ()).getChunkSnapshot();


		for(int x = 0; x < 16; x++)
		{
			for(int z = 0; z < 16; z++)
			{
				for(int y=0; y<255; y++)
				{
					int offset = (layer.number<0?-1:1)*(-(plugin.worldLayerBoder*3 -255));
					Block block = chunk.getBlock(x, y, z);

					int targetY = y + offset;

					if(0<targetY && targetY<256){
						int relationBlockTypeId = relationChunk.getBlockTypeId(x, targetY, z);
						byte relationBlockData = (byte) relationChunk.getBlockData(x, targetY, z);

						if(targetY<10&&relationBlockTypeId == Material.BEDROCK.getId()){
							relationBlockTypeId = Material.STONE.getId();
							relationBlockData = 0;
						}
						block.setTypeIdAndData(relationBlockTypeId,relationBlockData,false);
					}
				}
			}
		}

	}

}
