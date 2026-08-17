package net.islandcore.plugin.commands;

import net.islandcore.plugin.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class ReportIslandCommand implements CommandExecutor {

    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1535699556111614005/rkDboW9p0B-PRnoztnbJgr_HlSL2RnjkWC3o517SKkEFYhOdPS-XiJJNdrYO8eCHXNh4";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player reporter)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 2) {
            Msg.send(reporter, "&cUsage: /reportisland <player> <reason>");
            return true;
        }

        String targetName = args[0];
        StringBuilder reason = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) reason.append(" ");
            reason.append(args[i]);
        }

        String reasonStr = reason.toString();
        String reporterName = reporter.getName();
        String timestamp = Instant.now().toString();

        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("IslandCore"),
                () -> sendToDiscord(reporterName, targetName, reasonStr, timestamp)
        );

        Msg.send(reporter, "&aYour island report has been submitted. Thank you!");
        Msg.send(reporter, "&fRemember! Submitting false reports is against the rules.");
        return true;
    }

    private void sendToDiscord(String reporterName, String target, String reason, String timestamp) {
        try {
            String json = "{"
                    + "\"embeds\": [{"
                    + "\"title\": \"🏝️ Island Report\","
                    + "\"color\": 3447003,"
                    + "\"fields\": ["
                    + "  {\"name\": \"Reporter\", \"value\": \"" + escape(reporterName) + "\", \"inline\": true},"
                    + "  {\"name\": \"Reported Island Owner\", \"value\": \"" + escape(target) + "\", \"inline\": true},"
                    + "  {\"name\": \"Reason\", \"value\": \"" + escape(reason) + "\", \"inline\": false}"
                    + "],"
                    + "\"footer\": {\"text\": \"" + escape(timestamp) + "\"}"
                    + "}]"
                    + "}";

            HttpURLConnection conn = (HttpURLConnection) new URL(WEBHOOK_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            Bukkit.getLogger().warning("[IslandCore] Failed to send island report to Discord: " + e.getMessage());
        }
    }

    private static String escape(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}