package com.craftyn.casinoslots.slot.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.craftyn.casinoslots.slot.Reward;
import com.craftyn.casinoslots.slot.SlotMachine;
import com.craftyn.casinoslots.slot.Type;
import com.craftyn.casinoslots.util.Stat;

public class ResultsTask implements Runnable {
	
	private Game game;
	
	// Deploys rewards after game is finished
	public ResultsTask(Game game) {
		this.game = game;
	}

	@Override
	public void run() {
		
		Type type = game.getType();
		Player player = game.getPlayer();
		String name = type.getName();
		Double cost = type.getCost();
		Double won = 0.0;
		
		ArrayList<Reward> results = getResults();
		
		if(!results.isEmpty()) {
			SlotMachine slot = game.getSlot();
			
			if(!(slot.getSign() == null)) {
				Block b = slot.getSign();
				if (b.getType().equals(Material.WALL_SIGN) || b.getType().equals(Material.SIGN_POST)) {
					Sign sign = (Sign) b.getState();
					sign.setLine(3, player.getDisplayName());
					sign.update(true);
				}else {
					game.plugin.error("The block stored for the sign is NOT a sign, please remove it.");
				}
			}
			
			// Send the rewards
			for(Reward reward : results) {
				game.plugin.rewardData.send(player, reward, type);
				won += reward.getMoney();
				if(game.plugin.configData.inDebug()) game.plugin.debug("The player has won an amount of: " + won);
			}
			
			// Managed
			if(slot.isManaged()) {
				
				slot.withdraw(won);
				game.plugin.slotData.saveSlot(slot);
				Double max = game.plugin.typeData.getMaxPrize(type.getName());
				if(slot.getFunds() < max) {
					slot.setEnabled(false);
				}
			}
		}
		
		// No win
		else {
			if(game.plugin.configData.inDebug()) game.plugin.debug("The player has won an amount of: " + won);
			game.plugin.sendMessage(player, type.getMessages().get("noWin"));
		}
		
		// Register statistics
		if(game.plugin.configData.trackStats) {
			Stat stat;
			
			//Already have some stats for this type
			if(game.plugin.statsData.isStat(name)) {
				stat = game.plugin.statsData.getStat(name);
				if(!results.isEmpty()) {
					stat.addWon(won, cost);
					game.plugin.statsData.addWonStat(stat);
				}else {
					stat.addLost(won, cost);
					game.plugin.statsData.addLostStat(stat);
				}
			} else {
				if(game.plugin.configData.inDebug()) game.plugin.debug("The player has won an amount of: " + won);
				if(game.plugin.configData.inDebug()) game.plugin.debug("The player has lost an amount of: " + cost);
				if(!results.isEmpty()) {
					stat = new Stat(name, 1, 1, 0, won, cost);
					game.plugin.statsData.addWonStat(stat);
				}else {
					stat = new Stat(name, 1, 0, 1, won, cost);
					game.plugin.statsData.addLostStat(stat);
				}
			}
			game.plugin.configData.saveStats();
		}
		
		// All done
		game.getSlot().toggleBusy();
		
	}
	
	// Gets the results
	private ArrayList<Reward> getResults() {
		
		ArrayList<Reward> results = new ArrayList<Reward>();
		ArrayList<Block> blocks = game.getSlot().getBlocks();
		
		// checks horizontal matches
		for(int i = 0; i < 5; i++) {
			Reward reward;
			ArrayList<String> currentId = new ArrayList<String>();
			List<Block> current = null;
			
			if(i < 3) {
				int start = 0 + 3 * i;
				int end = 3 + 3 * i;
				current = blocks.subList(start, end);
			}else {
				//diagonals
				if(game.plugin.configData.allowDiagonals) {
					current = new ArrayList<Block>();
					for(int j = 0; j < 3; j++) {
						if(i == 3) {
							current.add(blocks.get(j*4));
						}else {
							current.add(blocks.get(2+(2*j)));
						}
					}
				}else {
					// Break loop if diagonals are disabled
					break;
				}
			}
			
			for(Block b : current) {
				currentId.add(b.getTypeId() + ":" + b.getData());
			}
			
			// Check for matches, deploy rewards
			Set<String> currentSet = new HashSet<String>(currentId);
			if(currentSet.size() == 1) {
				
				// Added for the damage value blocks and rewards
				int id = current.get(0).getTypeId();
				byte data = current.get(0).getData();
				reward = game.getType().getReward(id + ":" + data);
				
					// Break loop if and don't reward for something that doesn't have a reward.
					if (reward == null) {
						break;
					}
				
				results.add(reward);
				
				//Play some sounds on rewards!
				Location location = game.getSlot().getController().getLocation();
				game.getPlayer().playNote(location, Instrument.PIANO, new Note((byte) 0, Tone.G, false));
				game.getPlayer().playNote(location, Instrument.PIANO, new Note((byte) 0, Tone.E, false));
			}	
		}
		
		return results;
	}

}