package com.github.bitbyte.yetanotherrtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class YetAnotherRTP extends JavaPlugin {

    private static int pointX;
    private static int pointZ;
    private static int minDist;
    private static int maxDist;
    private static String runRTP;
    private static String afterRTP;
    private static String dest;
    private static int cooldownTime;
    private final ThreadLocalRandom rand = ThreadLocalRandom.current();
    private World world;
    private final Map<UUID, Integer> lastUseTime = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pointX = getConfig().getInt("settings.pointx");
        pointZ = getConfig().getInt("settings.pointz");
        minDist = getConfig().getInt("settings.min-dist");
        maxDist = getConfig().getInt("settings.max-dist") + 1;
        runRTP = getConfig().getString("messages.run-rtp");
        afterRTP = getConfig().getString("messages.afterRTP");
        dest = getConfig().getString("settings.world-dest");
        cooldownTime = getConfig().getInt("settings.cooldown");
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    private void rtp(Player player) {
        UUID uuid = player.getUniqueId();
        int currentTime = (int) System.currentTimeMillis() / 1000;
        if (lastUseTime.containsKey(uuid)) {
            int timeSinceLastUse = currentTime - lastUseTime.get(uuid);
            if (timeSinceLastUse < cooldownTime && !player.hasPermission("rtp.exempt")) {
                player.sendPlainMessage("You can't use this command for another " + (cooldownTime - timeSinceLastUse) + " seconds.");
                return;
            }
        }
        world = getServer().getWorld(dest);
        player.sendPlainMessage(runRTP);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            int randomX = rand.nextInt(minDist, maxDist) + pointX;
            int randomZ = rand.nextInt(minDist, maxDist) + pointZ;
            if (rand.nextBoolean()) {
                randomX = -randomX;
            }
            if (rand.nextBoolean()) {
                randomZ = -randomZ;
            }
            int highestY = world.getHighestBlockYAt(randomX, randomZ) + 1;
            Location safeLocation = new Location(world, randomX, highestY, randomZ);
            world.getChunkAtAsyncUrgently(safeLocation).thenAcceptBoth(player.teleportAsync(safeLocation), (chunk, voidResult) -> player.sendPlainMessage(afterRTP));
            player.setMetadata("RTP.UsedCommand", new FixedMetadataValue(this, true));
                lastUseTime.put(uuid, currentTime);
            });
        }

    private final String noPerm = getConfig().getString("messages.no-perm");
    private final String usedOnce = getConfig().getString("messages.used-once");
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rtp")) {
            if (sender instanceof Player) {
                if (getConfig().getBoolean("settings.require-perm")) {
                    if (getConfig().getBoolean("settings.one-use")) {
                        if (sender.hasPermission("rtp.runcmd")) {
                            if (!((Player) sender).hasMetadata("RTP.UsedCommand")) {
                                rtp((Player) sender);
                                return true;
                            } else {
                                sender.sendPlainMessage(usedOnce);
                            }
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
                    if (!((Player) sender).hasMetadata("RTP.UsedCommand")) {
                        rtp((Player) sender);
                        return true;
                    } else {
                        sender.sendPlainMessage(usedOnce);
                    }
                } else {
                    rtp((Player) sender);
                    return true;
                }
            } else {
                sender.sendPlainMessage("You must be a player to execute this command");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("removeused")) {
            if (args.length == 1) {
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null) {
                    sender.sendPlainMessage(getConfig().getString("messages.invalid-player"));
                    return false;
                } else {
                    player.removeMetadata("RTP.UsedCommand", this);
                    sender.sendPlainMessage("Removed metadata from " + player.getName());
                    return true;
                }
            }
        } else if (command.getName().equalsIgnoreCase("addused")) {
            if (args.length == 1) {
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null) {
                    sender.sendPlainMessage(getConfig().getString("messages.invalid-player"));
                    return false;
                } else {
                    player.setMetadata("RTP.UsedCommand", new FixedMetadataValue(this, true));
                    sender.sendPlainMessage("Added metadata to " + player.getName());
                    return true;
                }
            }
        }
        return false;
    }
}