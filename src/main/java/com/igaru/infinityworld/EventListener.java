package com.igaru.infinityworld;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.block.Jukebox;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.bergerkiller.bukkit.common.events.EntityMoveEvent;

public class EventListener implements Listener{

	InfinityWorld plugin;

	public EventListener(InfinityWorld plugin){
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Entity entity = event.getPlayer();
		Location location = entity.getLocation();
		LayerInfo layer = plugin.getLayerInfo(entity.getWorld());

		if(location.getY()> (255-plugin.worldLayerBoder+plugin.worldLayerMargin) ){
			plugin.toUp(entity);
		}
		else if(location.getY() < (plugin.worldLayerBoder*(layer.number==0?1:2) )){
			plugin.toDown(entity);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerDropItem(PlayerDropItemEvent event){
		event.getItemDrop().setMetadata("from", new FixedMetadataValue(plugin,event.getPlayer()));
	}


	private void UpdateBlockState(final Block dest,final Block src){
		new BukkitRunnable() {
			@Override
            public void run() {
				if(dest==null || src==null) return;

				dest.setTypeIdAndData(src.getTypeId(),src.getData(),true);
				BlockState destBlockState = dest.getState();
				BlockState state = src.getState();

				/* インベントリのコピーはせずに、参照先変更で対応
				if(state instanceof InventoryHolder){
					InventoryHolder inventoryHolder = (InventoryHolder)state;
					InventoryHolder relationInventoryHolder = (InventoryHolder)relationBlockState;
				}
				*/
				if(state instanceof Jukebox){
					Jukebox juke = (Jukebox)state;
					Jukebox destJuke = (Jukebox)destBlockState;
					if(juke.isPlaying())
						destJuke.setPlaying(juke.getPlaying());
				}
				if(state instanceof NoteBlock){
					NoteBlock note = (NoteBlock)state;
					NoteBlock destNote = (NoteBlock)destBlockState;
					destNote.setNote(note.getNote());
				}
				if(state instanceof Sign){
					Sign sign = (Sign)state;
					Sign destSign = (Sign)destBlockState;
					String[] lines = sign.getLines();
					for(int n=0;n<lines.length;n++)
						destSign.setLine(n, lines[n]);
				}
				if(state instanceof Skull){
					Skull skull = (Skull)state;
					Skull destSkull = (Skull)destBlockState;
					destSkull.setOwner(skull.getOwner());
					destSkull.setSkullType(skull.getSkullType());
					destSkull.setRotation(skull.getRotation());
				}
				destBlockState.update(true, false);
            }
		}.runTaskLater(this.plugin, 1);
	}

	/* ブロックの発生 */
	private void BlockAddEvent(BlockEvent event){
		Block block = event.getBlock();
		//plugin.logMessage(event.getEventName()+" "+block.getType());

		Block relationBlock = plugin.getRelationBlock(block);
		if(relationBlock!=null){
			UpdateBlockState(relationBlock,block);
		}
	}
	private void BlockStateChangeEvent(BlockGrowEvent event){
		BlockState state= event.getNewState();
		Block block = state.getBlock();

		//plugin.logMessage(event.getEventName()+" "+state.getType());
		Block relationBlock = plugin.getRelationBlock(block);
		if(relationBlock!=null){
			UpdateBlockState(relationBlock,block);
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event){
		BlockAddEvent(event);
	}
	@EventHandler /* 気象効果でブロックが生成される時 (水→氷、雪が積もる)*/
	public void onBlockFormEvent(BlockFormEvent event){
		BlockStateChangeEvent(event);
	}
	@EventHandler /* 植物が育つ時(コップンでは発生しない) */
	public void onBlockGrowEvent(BlockGrowEvent event){
		if(!plugin.isMasterLocation(event.getBlock().getLocation())){
			event.setCancelled(true);
		}
		else BlockStateChangeEvent(event);
	}
	@EventHandler /* 雪だるまが雪を作る時 */
	public void onEntityBlockFormEvent(EntityBlockFormEvent event){
		BlockStateChangeEvent(event);
	}
	@EventHandler /* 炎が燃え広がるとき */
	public void onBlockSpread(BlockSpreadEvent event){
		BlockStateChangeEvent(event);
	}
	@EventHandler /* 壁掛けアイテムを設置するとき */
	public void onHangingPlace(HangingPlaceEvent event){
		BlockFace blockFace = event.getBlockFace();
		Block block = event.getBlock()/*.getRelative(blockFace)*/;
		Block relationBlock = plugin.getRelationBlock(block);
		Hanging entity = event.getEntity();
		Location location = relationBlock.getLocation().clone();
		//Location location = entity.getLocation().clone();
		//location.setWorld(relationBlock.getWorld());
		//location.setY(relationBlock.getY());

		/*
		if(entity instanceof EntityItemFrame){
			EntityItemFrame itemFrame = new EntityItemFrame((World) relationBlock.getWorld(),relationBlock.getX(),relationBlock.getY(),relationBlock.getZ(),((EntityItemFrame)entity).getRotation());
			//itemFrame.spawnIn((World)relationBlock.getWorld());
			relationBlock.getWorld().spawn
		}
		*/

		Hanging hanging = relationBlock.getWorld().spawn(location, entity.getClass());
		hanging.setFacingDirection(blockFace,true);
	}

	/* ブロックの消滅 */
	private void BlockRemove(Block block){
		Block relationBlock = plugin.getRelationBlock(block);
		if(relationBlock!=null){
			if(plugin.isMasterLocation(block.getLocation()))
				relationBlock.setType(Material.AIR);
			else{
				block.setType(Material.AIR);
				relationBlock.breakNaturally();
			}
		}

	}
	private void BlockRemoveEvent(BlockEvent event){
		BlockRemove(event.getBlock());
		//plugin.logMessage(event.getEventName()+" "+event.getBlock().getType());
	}
	@EventHandler /* ブロックが壊れる時 */
	public void onBlockBreak(BlockBreakEvent event){
		BlockRemoveEvent(event);
	}
	@EventHandler /* ブロックが燃え尽きて焼失する時 */
	public void onBlockBurn(BlockBurnEvent event){
		BlockRemoveEvent(event);
	}
	@EventHandler /* ブロックが溶ける時(氷など) */
	public void onBlockFade(BlockFadeEvent event){
		BlockRemoveEvent(event);
	}
	@EventHandler /*葉が枯れる時*/
	public void onLeavesDecay(LeavesDecayEvent event){
		BlockRemoveEvent(event);
	}
	/* タイルエンティティの変更 */
	@EventHandler
	public void onSignChange(SignChangeEvent event){
		Block block = event.getBlock();
		Block relationBlock = plugin.getRelationBlock(block);
		if(relationBlock!=null){

			Sign sign = (Sign) relationBlock.getState();
			String[] lines = event.getLines();
			for(int n=0;n<lines.length;n++)
				sign.setLine(n, lines[n]);
			sign.update(true);
		}
	}

	@EventHandler
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event){
		Block block = event.getBlockClicked().getRelative(event.getBlockFace());
		Block relationBlock = plugin.getRelationBlock(block);
		UpdateBlockState(relationBlock,block);
	}

	@EventHandler
	public void onPlayerBucketFill(PlayerBucketFillEvent event){
		Block block = event.getBlockClicked();
		Block relationBlock = plugin.getRelationBlock(block);
		UpdateBlockState(relationBlock,block);
	}

	@EventHandler
	public void onFurnaceBurn(FurnaceBurnEvent event){
		Block block1 = event.getBlock();
		if(plugin.isMasterLocation(block1.getLocation())){
			Block block2 = plugin.getRelationBlock(block1);
			BlockState state = block2.getState();
			if(state instanceof Furnace){
				final Furnace furnace = (Furnace)state;
				short time = (short) event.getBurnTime();
				furnace.setBurnTime(time);
				furnace.setType(Material.BURNING_FURNACE);
				furnace.update(true);
				new BukkitRunnable() {
					@Override
		            public void run() {
						furnace.setType(Material.FURNACE);
						furnace.update(true);
					}
				}.runTaskLater(plugin, time);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteractEvent(final PlayerInteractEvent event){
		if (!event.isCancelled()){
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK){
				final Block block = event.getClickedBlock();
				final Block relationBlock = plugin.getRelationBlock(block);
				if(relationBlock!=null){
					Player player = event.getPlayer();

					//インベントリを有するブロックは、より0に近いレイヤーのブロックを参照する
					Material type = block.getType();
					BlockState relationBlockState = relationBlock.getState();
					if((!event.getPlayer().isSneaking() ) &&
						(!plugin.isMasterLocation(block.getLocation())) &&
						relationBlockState instanceof InventoryHolder
					){
						InventoryHolder inventoryBlock = (InventoryHolder)relationBlockState;
						event.setCancelled(true);
						player.openInventory(inventoryBlock.getInventory());

						if(type==Material.FURNACE||type==Material.BURNING_FURNACE){
							try{
								Furnace furnace = (Furnace)relationBlockState;
								((Furnace)block.getState()).setBurnTime(  furnace.getBurnTime());
								furnace.update(true);
							}catch(Exception e){
								e.printStackTrace();
							}
						}
					}
					else{
						//対象ブロックの更新
						UpdateBlockState(relationBlock,block);
					}
				}
			}
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event){
		List<Block> blocks = event.blockList();
		if(!event.isCancelled()){
			for(Block block : blocks){
				BlockRemove(block);
			}
		}
	}
	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent event){
		if(!event.isCancelled()){
			Block block = event.getBlock();
			Block relationBlock = plugin.getRelationBlock(block);
			UpdateBlockState(relationBlock,block);
		}
	}

	@EventHandler
	public void onEntityMove(EntityMoveEvent event){
		Entity entity = event.getEntity();
		Location location = entity.getLocation();
		LayerInfo layer = plugin.getLayerInfo(entity.getWorld());

		if(location.getY()> (255-plugin.worldLayerBoder+plugin.worldLayerMargin) ){
			plugin.toUp(entity);
		}
		else if(location.getY() < (plugin.worldLayerBoder*(layer.number==0?1:2) )){
			plugin.toDown(entity);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCreatureSpawn(CreatureSpawnEvent event){
		SpawnReason reason = event.getSpawnReason();
		Location location = event.getLocation();
		Entity entity = event.getEntity();
		if(reason != SpawnReason.EGG){
			if(!plugin.isMasterLocation(location) && !entity.hasMetadata("from")){
				event.setCancelled(true);
				//plugin.logMessage(event.getEntity().getType().getName()+"のスポーンがキャンセルされました。@"+location);
			}else{
				if(entity instanceof Monster && !entity.hasMetadata("mutant")){
					LayerInfo layer = plugin.getLayerInfo(location.getWorld());
					double level = 1+Math.abs(layer.number)/5;
					Monster monster = (Monster)entity;
					monster.setMaxHealth(monster.getMaxHealth()*level);
					monster.setHealth(monster.getHealth()*level);

					monster.setMetadata("mutant", new FixedMetadataValue(plugin,"deepMonster"));
				}
			}
		}
	}

	/*
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPhysics(BlockPhysicsEvent event){
		Block block = event.getBlock();
		if((!event.isCancelled()) && (!plugin.isMasterLocation(block.getLocation()))){
			Block relationBlock = plugin.getRelationBlock(block);
			UpdateBlockState(block,relationBlock);
		}
	}
	*/

	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemSpawn(ItemSpawnEvent event){
		Location location = event.getLocation();
		if(!plugin.isMasterLocation(location) && !event.getEntity().hasMetadata("from")){
			event.setCancelled(true);
			//plugin.logMessage(event.getEntity().getItemStack().getType()+"のスポーンがキャンセルされました。@"+location);
		}
	}


	private Player getPlayer(String name){
		for( Player player: Bukkit.getOnlinePlayers()){
			if(player.getName().equals(name)){
				return player;
			}
		}
		return null;
	}
}
