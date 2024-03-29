package net.arcturus.mc.bcl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.arcturus.mc.bcl.datastore.DataStoreManager;
import net.arcturus.mc.bcl.datastore.IDataStore;
import net.arcturus.mc.bcl.datastore.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class CommandExec implements CommandExecutor {
	BetterChunkLoader instance;

	CommandExec(BetterChunkLoader instance) {
		this.instance = instance;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equals("betterchunkloader")) {
			String usage = ChatColor.GOLD + "Usage: /" + label + " [info|list|chunks|delete|purge|reload]";
			if (args.length == 0) {
				sender.sendMessage(usage);
				return false;
			}
			switch (args[0].toLowerCase()) {
				case "info":
					return info(sender);
				case "list":
					return list(sender, label, args);
				case "chunks":
					return chunks(sender, label, args);
				case "delete":
					return delete(sender, label, args);
				case "purge":
					return purge(sender);
				case "reload":
					return reload(sender);
			}
			sender.sendMessage(usage);
		}
		return false;
	}

	private boolean info(CommandSender sender) {
		if (!sender.hasPermission("betterchunkloader.info")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to run this command.");
			return false;
		}
		List<CChunkLoader> chunkLoaders = DataStoreManager.getDataStore().getChunkLoaders();
		if (chunkLoaders.isEmpty()) {
			sender.sendMessage("No statistics available.");
			return true;
		}
		int alwaysOnLoaders = 0, onlineOnlyLoaders = 0, alwaysOnChunks = 0, onlineOnlyChunks = 0, maxChunksCount = 0, players = 0;
		UUID maxChunksPlayer = null;
		HashMap<UUID, Integer> loadedChunksForPlayer = new HashMap<>();
		for (CChunkLoader chunkLoader : chunkLoaders) {
			if (chunkLoader.isAlwaysOn()) {
				alwaysOnLoaders++;
				alwaysOnChunks += chunkLoader.size();
			} else {
				onlineOnlyLoaders++;
				onlineOnlyChunks += chunkLoader.size();
			}
			Integer count = loadedChunksForPlayer.get(chunkLoader.getOwner());
			if (count == null)
				count = Integer.valueOf(0);
			count = Integer.valueOf(count.intValue() + chunkLoader.size());
			loadedChunksForPlayer.put(chunkLoader.getOwner(), count);
		}
		loadedChunksForPlayer.remove(CChunkLoader.adminUUID);
		players = loadedChunksForPlayer.size();
		for (Map.Entry<UUID, Integer> entry : loadedChunksForPlayer.entrySet()) {
			if (maxChunksCount < ((Integer)entry.getValue()).intValue()) {
				maxChunksCount = ((Integer)entry.getValue()).intValue();
				maxChunksPlayer = entry.getKey();
			}
		}
		sender.sendMessage(ChatColor.GOLD + "=== BetterChunkLoader statistics ===\n" + ChatColor.WHITE + "OnlineOnly: " + onlineOnlyLoaders + " chunk loaders (" + onlineOnlyChunks + " chunks)\nAlwaysOn: " + alwaysOnLoaders + " chunk loaders (" + alwaysOnChunks + " chunks)\nNumber of players using chunk loaders: " + players + "\nPlayer with the highest loaded chunks amount: " + this.instance

				.getServer().getOfflinePlayer(maxChunksPlayer).getName() + " (" + maxChunksCount + " chunks)\n");
		return true;
	}

	private boolean list(CommandSender sender, String label, String[] args) {
		if (args.length < 2) {
			sender.sendMessage(ChatColor.GOLD + "Usage: /bcl list (own|PlayerName|all) [page]");
			return false;
		}
		int page = 1;
		if (args.length == 3)
			try {
				page = Integer.valueOf(args[2]).intValue();
				if (page < 1)
					throw new NumberFormatException();
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.RED + "Invalid page");
				return false;
			}
		if (args[1].equalsIgnoreCase("all")) {
			if (!sender.hasPermission("betterchunkloader.list.others")) {
				sender.sendMessage(ChatColor.RED + "You don't have permission to run this command.");
				return false;
			}
			List<CChunkLoader> clList = DataStoreManager.getDataStore().getChunkLoaders();
			printChunkLoadersList(clList, sender, page);
		} else if (args[1].equalsIgnoreCase("alwayson")) {
			if (!sender.hasPermission("betterchunkloader.list.others")) {
				sender.sendMessage(ChatColor.RED + "You don't have permission to run this command.");
				return false;
			}
			List<CChunkLoader> clList = new ArrayList<>();
			for (CChunkLoader cl : DataStoreManager.getDataStore().getChunkLoaders()) {
				if (cl.isAlwaysOn())
					clList.add(cl);
			}
			printChunkLoadersList(clList, sender, page);
		} else {
			String playerName = args[1];
			if (playerName.equalsIgnoreCase("own"))
				playerName = sender.getName();
			if (sender.getName().equalsIgnoreCase(playerName)) {
				if (!sender.hasPermission("betterchunkloader.list.own")) {
					sender.sendMessage(ChatColor.RED + "You don't have permission to run this command.");
					return false;
				}
			} else if (!sender.hasPermission("betterchunkloader.list.others")) {
				sender.sendMessage(ChatColor.RED + "You don't have permission to run this command.");
				return false;
			}
			OfflinePlayer player = this.instance.getServer().getOfflinePlayer(playerName);
			if (player == null || !player.hasPlayedBefore()) {
				sender.sendMessage(ChatColor.RED + "Player not found.");
				return false;
			}
			List<CChunkLoader> clList = DataStoreManager.getDataStore().getChunkLoaders(player.getUniqueId());
			if (clList == null || clList.size() == 0) {
				sender.sendMessage(ChatColor.RED + "This player doesn't have any chunk loader.");
				return false;
			}
			int clSize = clList.size();
			int pages = (int)Math.ceil(clSize / 5.0D);
			if (page > pages) {
				sender.sendMessage(ChatColor.RED + "Invalid page");
				return false;
			}
			sender.sendMessage(ChatColor.GOLD + "== " + player.getName() + " chunk loaders list (" + page + "/" + pages + ") ==");
			sender.sendMessage(ChatColor.GRAY + "(AlwaysOn - Size - Position)");
			for (int i = (page - 1) * 5; i < page * 5 && i < clSize; i++) {
				CChunkLoader chunkLoader = clList.get(i);
				sender.sendMessage(chunkLoader.toString());
			}
		}
		return true;
	}

	private static boolean printChunkLoadersList(List<CChunkLoader> clList, CommandSender sender, int page) {
		int clSize = clList.size();
		if (clSize == 0) {
			sender.sendMessage(ChatColor.RED + "There isn't any chunk loader yet!");
			return false;
		}
		int pages = (int)Math.ceil(clSize / 5.0D);
		if (page > pages) {
			sender.sendMessage(ChatColor.RED + "Invalid page");
			return false;
		}
		sender.sendMessage(ChatColor.GOLD + "== Chunk loaders list (" + page + "/" + pages + ") ==");
		sender.sendMessage(ChatColor.GRAY + "(Owner - AlwaysOn - Size - Position)");
		for (int i = (page - 1) * 5; i < page * 5 && i < clSize; i++) {
			CChunkLoader chunkLoader = clList.get(i);
			sender.sendMessage(chunkLoader.getOwnerName() + " - " + chunkLoader.toString());
		}
		return true;
	}

	private boolean chunks(CommandSender sender, String label, String[] args) {
		Integer amount;
		String usage = "Usage: /" + label + " chunks (add|set) (PlayerName) (alwayson|onlineonly) (amount)";
		if (args.length < 5) {
			sender.sendMessage(chunksInfo((OfflinePlayer)sender));
			return false;
		}
		if (!sender.hasPermission("betterchunkloader.chunks")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to run this command.");
			return false;
		}
		OfflinePlayer player = Bukkit.getOfflinePlayer(args[2]);
		if (player == null) {
			sender.sendMessage(args[2] + " is not a valid player name\n" + usage);
			return false;
		}
		try {
			amount = Integer.valueOf(args[4]);
		} catch (NumberFormatException e) {
			sender.sendMessage("Invalid argument " + args[4] + "\n" + usage);
			return false;
		}
		sender.sendMessage(chunksInfo(player));
		if (args[1].equalsIgnoreCase("add")) {
			if (sender.hasPermission("betterchunkloaders.chunks.add")) {
				if (args[3].equalsIgnoreCase("alwayson")) {
					DataStoreManager.getDataStore().addAlwaysOnChunksLimit(player.getUniqueId(), amount.intValue());
					sender.sendMessage("Added " + amount + " always-on chunks to " + player.getName());
				} else if (args[3].equalsIgnoreCase("onlineonly")) {
					DataStoreManager.getDataStore().addOnlineOnlyChunksLimit(player.getUniqueId(), amount.intValue());
					sender.sendMessage("Added " + amount + " online-only chunks to " + player.getName());
				} else {
					sender.sendMessage("Invalid argument " + args[3] + "\n" + usage);
					return false;
				}
			} else {
				sender.sendMessage("You do not have permission to use this command.");
				return false;
			}
		} else if (args[1].equalsIgnoreCase("set")) {
			if (sender.hasPermission("betterchunkloaders.chunks.set")) {
				if (amount.intValue() < 0) {
					sender.sendMessage("Invalid argument " + args[4] + "\n" + usage);
					return false;
				}
				if (args[3].equalsIgnoreCase("alwayson")) {
					DataStoreManager.getDataStore().setAlwaysOnChunksLimit(player.getUniqueId(), amount.intValue());
					sender.sendMessage("Set " + amount + " always-on chunks to " + player.getName());
				} else if (args[3].equalsIgnoreCase("onlineonly")) {
					DataStoreManager.getDataStore().setOnlineOnlyChunksLimit(player.getUniqueId(), amount.intValue());
					sender.sendMessage("Set " + amount + " online-only chunks to " + player.getName());
				} else {
					sender.sendMessage("Invalid argument " + args[3] + "\n" + usage);
					return false;
				}
			} else {
				sender.sendMessage("You do not have permission to use this command.");
				return false;
			}
		} else {
			sender.sendMessage("Invalid argument " + args[1] + "\n" + usage);
			return false;
		}
		return true;
	}

	private boolean delete(CommandSender sender, String label, String[] args) {
		if (!sender.hasPermission("betterchunkloader.delete")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to run this command.");
			return false;
		}
		if (args.length < 2) {
			sender.sendMessage(ChatColor.GOLD + "Usage: /bcl delete (PlayerName)");
			return false;
		}
		OfflinePlayer player = this.instance.getServer().getOfflinePlayer(args[1]);
		if (player == null || !player.hasPlayedBefore()) {
			sender.sendMessage(ChatColor.RED + "Player not found.");
			return false;
		}
		List<CChunkLoader> clList = DataStoreManager.getDataStore().getChunkLoaders(player.getUniqueId());
		if (clList == null) {
			sender.sendMessage(ChatColor.RED + "This player doesn't have any chunk loader.");
			return false;
		}
		DataStoreManager.getDataStore().removeChunkLoaders(player.getUniqueId());
		sender.sendMessage(ChatColor.RED + "All chunk loaders placed by this player have been removed.");
		this.instance.getLogger().info(sender.getName() + " deleted all chunk loaders placed by " + player.getName());
		return true;
	}

	private boolean purge(CommandSender sender) {
		if (!sender.hasPermission("betterchunkloader.purge")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to run this command.");
			return false;
		}
		IDataStore ds = DataStoreManager.getDataStore();
		List<CChunkLoader> chunkLoaders = new ArrayList<>(DataStoreManager.getDataStore().getChunkLoaders());
		for (CChunkLoader cl : chunkLoaders) {
			if (!cl.blockCheck())
				ds.removeChunkLoader(cl);
		}
		sender.sendMessage(ChatColor.GOLD + "All invalid chunk loaders have been removed.");
		return true;
	}

	private boolean reload(CommandSender sender) {
		if (!sender.hasPermission("betterchunkloader.reload")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to run this command.");
			return false;
		}
		this.instance.getLogger().info(sender.getName() + " reloaded this plugin");
		Bukkit.getPluginManager().disablePlugin((Plugin)this.instance);
		Bukkit.getPluginManager().enablePlugin((Plugin)this.instance);
		sender.sendMessage(ChatColor.RED + "BetterChunkLoader reloaded.");
		return true;
	}

	static String chunksInfo(OfflinePlayer player) {
		IDataStore dataStore = DataStoreManager.getDataStore();
		int freeAlwaysOn = dataStore.getAlwaysOnFreeChunksAmount(player.getUniqueId());
		int freeOnlineOnly = dataStore.getOnlineOnlyFreeChunksAmount(player.getUniqueId());
		PlayerData pd = dataStore.getPlayerData(player.getUniqueId());
		int amountAlwaysOn = pd.getAlwaysOnChunksAmount();
		int amountOnlineOnly = pd.getOnlineOnlyChunksAmount();
		return ChatColor.GOLD + "=== " + player.getName() + " chunks amount ===\n" + ChatColor.GREEN + "Always-on - Free: " + freeAlwaysOn + " Used: " + (amountAlwaysOn - freeAlwaysOn) + " Total: " + amountAlwaysOn + "\nOnline-only - Free: " + freeOnlineOnly + " Used: " + (amountOnlineOnly - freeOnlineOnly) + " Total: " + amountOnlineOnly;
	}
}