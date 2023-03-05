package net.citizensnpcs.api.util;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import me.clip.placeholderapi.PlaceholderAPI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensDisableEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;

public class Placeholders implements Listener {
    public static interface PlaceholderFunction {
        public String apply(NPC npc, CommandSender sender, String input);
    }

    private static class PlaceholderProvider {
        PlaceholderFunction func;
        Pattern regex;

        PlaceholderProvider(Pattern regex, PlaceholderFunction func) {
            this.regex = regex;
            this.func = func;
        }
    }

    private static OfflinePlayer getPlayer(BlockCommandSender sender) {
        return CitizensAPI.getNMSHelper().getPlayer(sender);
    }

    @EventHandler
    private static void onCitizensDisable(CitizensDisableEvent event) {
        PLACEHOLDERS.clear();
    }

    public static void registerNPCPlaceholder(Pattern regex, PlaceholderFunction func) {
        if (regex.pattern().charAt(0) != '<') {
            regex = Pattern.compile('<' + regex.pattern() + '>', regex.flags());
        }
        PLACEHOLDERS.add(new PlaceholderProvider(regex, func));
    }

    public static String replace(String text, CommandSender sender, NPC npc) {
        text = replace(text, sender instanceof OfflinePlayer ? (OfflinePlayer) sender
                : sender instanceof BlockCommandSender ? getPlayer((BlockCommandSender) sender) : null);
        if (npc == null || text == null) {
            return text;
        }
        StringBuffer out = new StringBuffer();
        Matcher matcher = PLACEHOLDER_MATCHER.matcher(text);
        while (matcher.find()) {
            String replacement = "";
            String group = matcher.group(1);
            switch (group) {
                case "id":
                    replacement = Integer.toString(npc.getId());
                    break;
                case "npc":
                    replacement = npc.getFullName();
                    break;
                case "owner":
                    replacement = npc.getOrAddTrait(Owner.class).getOwner();
                    break;
            }
            matcher.appendReplacement(out, "");
            out.append(replacement);
        }
        matcher.appendTail(out);
        for (PlaceholderProvider entry : PLACEHOLDERS) {
            matcher = entry.regex.matcher(out.toString());
            out = new StringBuffer();
            while (matcher.find()) {
                String group = matcher.group().substring(1, matcher.group().length() - 1);
                matcher.appendReplacement(out, "");
                out.append(entry.func.apply(npc, sender, group));
            }
            matcher.appendTail(out);
        }
        return out.toString();
    }

    public static String replace(String text, OfflinePlayer player) {
        if (player == null || (!player.isOnline() && !player.hasPlayedBefore())) {
            return setPlaceholderAPIPlaceholders(text, player);
        }
        if (text == null) {
            return text;
        }
        if (player.getPlayer() != null) {
            StringBuffer out = new StringBuffer();
            Matcher matcher = PLAYER_PLACEHOLDER_MATCHER.matcher(text);
            while (matcher.find()) {
                String replacement = "";
                String group = matcher.group(1);
                if (PLAYER_VARIABLES.contains(group)) {
                    replacement = player.getName();
                } else {
                    switch (group) {
                        case "<random_player>":
                            Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
                            Player possible = Iterables.get(players, new Random().nextInt(players.size()), null);
                            if (possible != null) {
                                replacement = possible.getName();
                            }
                            break;
                        case "<random_npc>":
                            List<NPC> all = Lists.newArrayList(CitizensAPI.getNPCRegistry());
                            if (all.size() > 0) {
                                replacement = all.get(new Random().nextInt(all.size())).getName();
                            }
                            break;
                        case "<random_npc_id>":
                            all = Lists.newArrayList(CitizensAPI.getNPCRegistry());
                            if (all.size() > 0) {
                                replacement = Integer.toString(all.get(new Random().nextInt(all.size())).getId());
                            }
                            break;
                        case "<nearest_player>":
                            double min = Double.MAX_VALUE;
                            Player closest = null;
                            Location location = player.getPlayer().getLocation();
                            for (Player entity : CitizensAPI.getLocationLookup()
                                    .getNearbyPlayers(player.getPlayer().getLocation(), 25)) {
                                if (entity == player || CitizensAPI.getNPCRegistry().isNPC(entity))
                                    continue;
                                double dist = entity.getLocation().distanceSquared(location);
                                if (dist > min)
                                    continue;
                                min = dist;
                                closest = entity;
                            }
                            if (closest != null) {
                                replacement = closest.getName();
                            }
                            break;
                        case "<world>":
                            replacement = player.getPlayer().getWorld().getName();
                            break;

                    }
                }
                matcher.appendReplacement(out, "");
                out.append(replacement);
            }
            matcher.appendTail(out);
            text = out.toString();
        } else {
            for (int i = 0; i < PLAYER_PLACEHOLDERS.length; i++) {
                text = text.replace(PLAYER_PLACEHOLDERS[i], player.getName());
            }
        }
        return setPlaceholderAPIPlaceholders(text, player);
    }

    private static String setPlaceholderAPIPlaceholders(String text, OfflinePlayer player) {
        if (!PLACEHOLDERAPI_ENABLED) {
            return text;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable t) {
            PLACEHOLDERAPI_ENABLED = false;
            return text;
        }
    }

    private static final Pattern PLACEHOLDER_MATCHER = Pattern.compile("<(id|npc|owner)>");
    private static boolean PLACEHOLDERAPI_ENABLED = true;
    private static final List<PlaceholderProvider> PLACEHOLDERS = Lists.newArrayList();
    private static final Pattern PLAYER_PLACEHOLDER_MATCHER = Pattern.compile(
            "(<player>|<p>|@p|%player%|<random_player>|<random_npc>|<random_npc_id>|<nearest_player>|<world>)");
    private static final String[] PLAYER_PLACEHOLDERS = { "<player>", "<p>", "@p", "%player%" };
    private static final Collection<String> PLAYER_VARIABLES = ImmutableSet.of("<player>", "<p>", "@p", "%player%");
}
