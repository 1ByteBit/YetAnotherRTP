package com.github.bitbyte.yetanotherrtp;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public final class YetAnotherRTP extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    ThreadLocalRandom rand = ThreadLocalRandom.current();
    int pointX = getConfig().getInt("settings.pointx");
    int pointZ = getConfig().getInt("settings.pointz");
    int minDist = getConfig().getInt("settings.min-dist");
    int maxDist = getConfig().getInt("settings.max-dist");
    int randomX;
    int randomZ;
    int highestY;
    Location safeLocation;
    int waittime = getConfig().getInt("settings.wait-time");
    String runRTP = getConfig().getString("messages.run-rtp");
    String afterRTP = getConfig().getString("messages.afterRTP");
    private void rtp(Player player) {
        player.sendPlainMessage(runRTP);
        World world = player.getWorld();
        CompletableFuture.supplyAsync(() -> {
            randomX = rand.nextInt(minDist, maxDist + 1) + pointX;
            randomZ = rand.nextInt(minDist, maxDist + 1) + pointZ;
            if (rand.nextBoolean()) {
                randomX = -randomX;
            }
            if (rand.nextBoolean()) {
                randomZ = -randomZ;
            }
            highestY = world.getHighestBlockYAt(randomX, randomZ) + 1;
            safeLocation = new Location(world, randomX, highestY, randomZ);
            return safeLocation;
        }).thenComposeAsync((safeLocation) -> world.getChunkAtAsyncUrgently(safeLocation).thenApplyAsync((chunk) -> {
            chunk.addPluginChunkTicket(this);
            getServer().getScheduler().runTaskLater(this, () -> {
                player.teleportAsync(safeLocation).thenRun(() -> {
                    chunk.removePluginChunkTicket(this);
                    player.sendPlainMessage(afterRTP);
                });
            }, waittime);
            return chunk;
        }));
    }

    String noPerm = getConfig().getString("messages.no-perm");
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("rtp")) {
            if (sender instanceof Player) {
                if (getConfig().getBoolean("settings.require-perm")) {
                    if (getConfig().getBoolean("settings.one-use")) {
                        if (sender.hasPermission("rtp.runcmd")) {
                            // Check if it's already been used
                        } else {
                            sender.sendPlainMessage(noPerm);
                        }
                    } else {
                        if (sender.hasPermission("rtp.runcmd")) {
                            rtp((Player) sender);
                            return true;
                        } else {
                            sender.sendPlainMessage(noPerm);
                            return false;
                        }
                    }
                } else if (getConfig().getBoolean("settings.one-use")) {
                    // One use
                } else {
                    rtp((Player) sender);
                    return true;
                }
            } else {
                sender.sendPlainMessage("You must be a player to execute this command");
                return false;
            }
        }
        return false;
    }
}