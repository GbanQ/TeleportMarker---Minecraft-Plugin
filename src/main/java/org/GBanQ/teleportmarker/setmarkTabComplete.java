package org.GBanQ.teleportmarker;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class setmarkTabComplete implements TabCompleter {
    private TeleportMarker plugin;
    public setmarkTabComplete(TeleportMarker plugin) {
        this.plugin = plugin;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                List<String> options = new ArrayList<>();
                options.add(String.valueOf((int) player.getLocation().getX()));
                return options;
            } else if (args.length == 2) {
                List<String> options = new ArrayList<>();
                options.add(String.valueOf((int) player.getLocation().getY()));
                return options;
            } else if (args.length == 3) {
                List<String> options = new ArrayList<>();
                options.add(String.valueOf((int) player.getLocation().getZ()));
                return options;
            } else if (args.length == 4) {
                return Arrays.asList("30", "60", "120", "240", "320");
            }else if (args.length == 5) {
                return Arrays.asList("true", "false");
            }

        }
        return new ArrayList<>();
    }
}
