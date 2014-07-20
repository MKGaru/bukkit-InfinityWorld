package com.igaru.infinityworld;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.entity.CommonEntity;

public class InfinityWorld extends JavaPlugin{

	/**Config*/
	public int worldLayerMargin = 5;
	public int worldLayerBoder = 32;


	/**
	 *  world_layer-010          → world          , -010
	 *  world_resource_layer+100 → world_resource , +100
	 */
	private Pattern ifworldID = Pattern.compile("(.*)_layer([-+]?\\d+)$");

	public World getIFWorld(String base,int n){
		return getIFWorld(base,n,true);
	}
	public World getIFWorld(String base,int n,boolean generate){
		DecimalFormat df = new DecimalFormat("000");
		String worldName = n==0? base :(base+"_layer"+ (n>0?"+":"") +df.format(n) );

		World world = Bukkit.getServer().getWorld(worldName);
		if(world==null && generate){
			WorldCreator worldcreator = new WorldCreator(worldName);

			if(n>0) worldcreator.generator(new EmptyChunkGenerator(this));
			if(n<0) {
				worldcreator.environment(Environment.NORMAL);
				worldcreator.generator(new UnderChunkGenerator(this));
			}
			if(n!=0) worldcreator.seed(Bukkit.getServer().getWorld(base).getSeed());
			world = Bukkit.getServer().createWorld(worldcreator);

		}
		return world;
	}

	public boolean isIFWorld(World world){
		String worldName = world.getName();
        Matcher m =  ifworldID.matcher(worldName);

		return m.find();
	}

	//ワールドのつなぎ目部分のうち、該当ロケーションがマスター扱いかを判定
	//レイヤー0により近い部分がマスター
	public boolean isMasterLocation(Location location){
		World world1 = location.getWorld();
		Location relationLocation = getRelationLocation(location);
		if(relationLocation == null) return true;
		World world2 =relationLocation.getWorld();
		if(world2 == null) return true;
		LayerInfo layer1 = getLayerInfo(world1);
		LayerInfo layer2 = getLayerInfo(world2);

		return Math.abs(layer1.number) > Math.abs(layer2.number);
	}

	public LayerInfo getLayerInfo(World world) {
		String worldName = world.getName();
        Matcher m =  ifworldID.matcher(worldName);
        if( m.find() ){
        	return new LayerInfo(m.group(1),Integer.parseInt(m.group(2)));
        }
        return new LayerInfo(worldName,0);
	}


	private void teleportWithSyncWorld(Entity entity,Location location){
		CommonEntity<Entity> common = new CommonEntity<Entity>(entity);
		common.teleport(location);
		World nextWorld = location.getWorld();
		nextWorld.setTime(entity.getWorld().getTime());
		nextWorld.setStorm(entity.getWorld().hasStorm());
		nextWorld.setWeatherDuration(entity.getWorld().getWeatherDuration());
	}

	public void toUp(Entity entity){
		LayerInfo layer = getLayerInfo(entity.getWorld());
		boolean isPlayer = entity instanceof Player;
		World nextWorld = getIFWorld(layer.base,layer.number+1,isPlayer);
		if(entity.isInsideVehicle()){
			toUp(entity.getVehicle());
			return;
		}
		if(nextWorld!=null  && !entity.isInsideVehicle()){
			Location nextLocation = entity.getLocation().clone();
			nextLocation.setWorld(nextWorld);
			nextLocation.setY(entity.getLocation().getY() + (worldLayerBoder*3 - 255));
			entity.setMetadata("from", new FixedMetadataValue(this,entity.getLocation().getWorld()));
			teleportWithSyncWorld(entity,nextLocation);

			if(isPlayer){
				Player player = (Player)entity;
				logMessage(layer.base+"の"+player.getName()+"は、"+layer.number+"より上に昇った！ ("+entity.getWorld().getName()+":"+entity.getWorld().getSeed()+"→"+nextWorld.getName()+":"+nextWorld.getSeed()+")");
			}
		}

	}
	public void toDown(Entity entity){
		LayerInfo layer = getLayerInfo(entity.getWorld());
		boolean isPlayer = entity instanceof Player;

		World nextWorld = getIFWorld(layer.base,layer.number-1,isPlayer);
		if(entity.isInsideVehicle()){
			toDown(entity.getVehicle());
			return;
		}

		if(nextWorld!=null ){
			Location nextLocation = entity.getLocation().clone();
			nextLocation.setWorld(nextWorld);
			nextLocation.setY( entity.getLocation().getY() - (worldLayerBoder*3 - 255) );
			entity.setMetadata("from", new FixedMetadataValue(this,entity.getLocation().getWorld()));
			teleportWithSyncWorld(entity,nextLocation);

			if(isPlayer){
				Player player = (Player)entity;
				logMessage(layer.base+"の"+player.getName()+"は、"+layer.number+"より下へ降りた！ ("+entity.getWorld().getName()+":"+entity.getWorld().getSeed()+"→"+nextWorld.getName()+":"+nextWorld.getSeed()+")");
			}
		}
	}

	private Location getRelationLocation(Location location){
		LayerInfo layer = getLayerInfo(location.getWorld());
		Location relationLocation = location.clone();
		if(location.getY() > (255 - worldLayerBoder*3)){
			relationLocation.setWorld(getIFWorld(layer.base,layer.number+1));
			relationLocation.setY(location.getY() + (worldLayerBoder*3 -255));
			return relationLocation;
		}
		else if(location.getY() < (worldLayerBoder*3)){
			relationLocation.setWorld(getIFWorld(layer.base,layer.number-1));
			relationLocation.setY(location.getY() - (worldLayerBoder*3 -255));
			return relationLocation;
		}
		else{
			return null;
		}
	}

	public Block getRelationBlock(Block block)
	{
		Location location = getRelationLocation(block.getLocation());
		if(location==null) return null;
		return location.getWorld().getBlockAt(location);
	}


	@Override
    public void onEnable() {
		logMessage("InfinityWorld Enabled.");

		EventListener listener = new EventListener(this);
    }

    @Override
    public void onDisable() {
    	logMessage("InfinityWorld Disabled.");
    }


    public void logMessage(String msg){
    	getLogger().info(msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
    	if(cmd.getName().equals("up")){
    		if(sender instanceof Player){
    			Player player = (Player)sender;
				toUp(player);
				return true;
    		}
    	}
    	if(cmd.getName().equals("down")){
    		if(sender instanceof Player){
    			Player player = (Player)sender;
    			toDown(player);
				return true;
    		}
    	}
    	/* Chunk Reset Command
    	if(cmd.getName().equals("fix")){
    		if(sender instanceof Player){
    			Player player = (Player)sender;
    			int r = 1;
    			if(args.length>0){
    				try{
    					r = Integer.parseInt(args[0]);
    				}catch(NumberFormatException e){
    					return false;
    				}
    			}
    			Chunk chunk = player.getLocation().getChunk();
    			for(int i=-r;i<=r;i++)
    				for(int j=-r;j<=r;j++)
    						player.getWorld().regenerateChunk(chunk.getX()+i,chunk.getZ()+j);
    			return true;
    		}
    	}
    	*/
    	return false;
    }
}
