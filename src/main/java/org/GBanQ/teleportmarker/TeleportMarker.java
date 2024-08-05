package org.GBanQ.teleportmarker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class TeleportMarker extends JavaPlugin implements CommandExecutor {

    private Map<UUID, TeleportData> teleportData = new HashMap<>();
    private Map<UUID, Boolean> hasUsedGotp = new HashMap<>();
    private Map<UUID, BukkitTask> teleportTasks = new HashMap<>();
    private Map<UUID, Map<String, Boolean>> playerPermissions = new HashMap<>();
    private BossBar bossBar;


    @Override
    public void onEnable() {
        getCommand("setmark").setExecutor(this);
        getCommand("gotp").setExecutor(this);
        getCommand("delmark").setExecutor(this);
        getCommand("tpmhelp").setExecutor(this);

        getCommand("setmark").setTabCompleter(new setmarkTabComplete(this));
        getCommand("gotp").setTabCompleter(new gotpTabComplete(this));
        getCommand("delmark").setTabCompleter(new delmarkTabComplete(this));
        bossBar = Bukkit.createBossBar("For Teleportation - /gotp", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setVisible(false);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("setmark")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (hasPermission(player, "teleportmarker.use") || hasPermission(player, "teleportmarker.admin")) {
                    if (args.length == 5) {
                        try {
                            double x = Double.parseDouble(args[0]);
                            double y = Double.parseDouble(args[1]);
                            double z = Double.parseDouble(args[2]);
                            int time = Integer.parseInt(args[3]);
                            boolean allowRepeat = Boolean.parseBoolean(args[4]);

                            if (teleportData.containsKey(player.getUniqueId())) {
                                player.sendMessage(ChatColor.RED + "You already have a mark set. Please delete it first using the /delmark command.");
                                return true;
                            }

                            createTeleportMarker(player, new Location(player.getWorld(), x, y, z), time, allowRepeat);
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Coordinates, teleport time, and the allow repeat flag must be numbers.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Usage: /setmark <x> <y> <z> <time in seconds> <true/false>");
                        player.sendMessage(" ");
                        player.sendMessage(ChatColor.YELLOW + "<true/false> - Determines if the teleport mark can be used multiple times");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to create teleport marks.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            }
        } else if (command.getName().equalsIgnoreCase("gotp")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                boolean teleported = false;

                for (TeleportData data : teleportData.values()) {
                    if (data.allowRepeat || !hasUsedGotp.getOrDefault(player.getUniqueId(), false)) {
                        player.teleport(data.location);
                        teleported = true;
                        if (!data.allowRepeat) {
                            hasUsedGotp.put(player.getUniqueId(), true);
                        }
                        break;
                    }
                }

                if (!teleported) {
                    player.sendMessage(ChatColor.RED + "There are no open teleport marks available at the moment or you have already used this mark.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            }

            } else if (command.getName().equalsIgnoreCase("delmark")) {
                Player player = (Player) sender;
                if (hasPermission(player, "teleportmarker.use") || hasPermission(player, "teleportmarker.admin")) {
                    if (sender instanceof Player) {

                        TeleportData dataToRemove = teleportData.get(player.getUniqueId());

                        if (dataToRemove != null) {
                            teleportData.remove(player.getUniqueId());
                            BukkitTask task = teleportTasks.remove(player.getUniqueId());
                            if (task != null) task.cancel();
                            bossBar.setVisible(false);
                            bossBar.removeAll();
                            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(ChatColor.YELLOW + "Teleport mark removed by " + player.getName() + "."));
                            for (UUID uuid : hasUsedGotp.keySet()) {
                                hasUsedGotp.put(uuid, false);
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You don't have an active teleport mark.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                    }
                }
            } else if (command.getName().equalsIgnoreCase("tpmhelp")) {
               if (sender instanceof Player) {
                   Player player = (Player) sender;
                   player.sendMessage(ChatColor.GOLD + "======= Teleport Marker Plugin Help =======");player.sendMessage("");
                   player.sendMessage(ChatColor.YELLOW + "/setmark <x> <y> <z> <time> <true/false>" + ChatColor.RESET + " - Sets a teleport marker at specified coordinates.");
                   player.sendMessage(ChatColor.AQUA + "  <x> <y> <z>: " + ChatColor.RESET + "Coordinates for the teleport location.");
                   player.sendMessage(ChatColor.AQUA + "  <time>: " + ChatColor.RESET + "Time in seconds before the marker expires.");
                   player.sendMessage(ChatColor.AQUA + "  <true/false>: " + ChatColor.RESET + "Determines if the marker can be used multiple times.");
                   player.sendMessage("");
                   player.sendMessage(ChatColor.YELLOW + "/gotp" + ChatColor.RESET + " - Teleports you to the active marker.");
                   player.sendMessage("");
                   player.sendMessage(ChatColor.YELLOW + "/delmark" + ChatColor.RESET + " - Removes the active teleport marker.");
                   player.sendMessage("");
                   player.sendMessage(ChatColor.YELLOW + "Note: " + ChatColor.RESET + "Only players with appropriate permissions can set or remove teleport markers.");
                   player.sendMessage("");
                   player.sendMessage(ChatColor.GOLD + "========================================");
               } else {
                   sender.sendMessage(ChatColor.RED + "Only players can use this command.");
               }
             }
        return true;
    }

    private void createTeleportMarker(Player player, Location location, int time, boolean allowRepeat) {
        if (teleportData.containsKey(player.getUniqueId())) {
            BukkitTask oldTask = teleportTasks.remove(player.getUniqueId());
            if (oldTask != null) oldTask.cancel();
            teleportData.remove(player.getUniqueId());
        }

        TeleportData data = new TeleportData(location, time, player.getUniqueId(), allowRepeat);
        teleportData.put(player.getUniqueId(), data);
        hasUsedGotp.put(player.getUniqueId(), false);
        BukkitTask task = startTeleportTimer(player.getUniqueId(), time);
        teleportTasks.put(player.getUniqueId(), task);
        bossBar.setVisible(true);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
        updateBossBar(time, time);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(ChatColor.YELLOW + "A teleport mark has been created."));
    }



    private BukkitTask startTeleportTimer(UUID uuid, int time) {
        return Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (teleportData.containsKey(uuid)) {
                TeleportData data = teleportData.get(uuid);
                data.time--;
                updateBossBar(data.time, time);
                if (data.time <= 0) {
                    teleportData.remove(uuid);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        hasUsedGotp.remove(p.getUniqueId());
                    }
                    teleportTasks.remove(uuid).cancel();
                    bossBar.setVisible(false);
                    bossBar.removeAll();
                    Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(ChatColor.YELLOW + "The teleport mark has been closed."));

                }
            }
        }, 0L, 20L);
    }

    private void updateBossBar(int remainingTime, int totalTime) {
        double progress = (double) remainingTime / totalTime;
        bossBar.setTitle("For Teleportation - /gotp");
        bossBar.setProgress(progress);
    }

    private boolean hasPermission(Player player, String permission) {
        Map<String, Boolean> playerPermMap = playerPermissions.getOrDefault(player.getUniqueId(), new HashMap<>());
        return player.hasPermission(permission) || playerPermMap.getOrDefault(permission, false);
    }

    private static class TeleportData {
        Location location;
        int time;
        UUID creator;
        boolean allowRepeat;

        TeleportData(Location location, int time, UUID creator, boolean allowRepeat) {
            this.location = location;
            this.time = time;
            this.creator = creator;
            this.allowRepeat = allowRepeat;
        }
    }


}
