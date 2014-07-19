package com.igaru.infinityworld;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public class UnderChunkGenerator extends ChunkGenerator {
	private InfinityWorld plugin;

	public UnderChunkGenerator(InfinityWorld instance){
		plugin = instance;
	}



	@Override
	public List<BlockPopulator> getDefaultPopulators(World world){
		List<BlockPopulator> populators = new ArrayList<BlockPopulator>();

		populators.add(new RelationBlockPopulator(plugin,world));

		return populators;
	}


	private int coordsToInt(int x, int y , int z){
		return (x*16 +z) * 256 + y;
	}

	public byte[] generate(World world,Random rand, int chunkX, int chunkY){
		byte[] blocks = new byte[16*16*256];


		for(int x=0;x<16;x++)for(int y=0;y<255;y++)for(int z=0;z<16;z++)
		{
			blocks[coordsToInt(x,y,z)] = (byte) Material.STONE.getId();
		}

		return blocks;
	}
	//*/
}
