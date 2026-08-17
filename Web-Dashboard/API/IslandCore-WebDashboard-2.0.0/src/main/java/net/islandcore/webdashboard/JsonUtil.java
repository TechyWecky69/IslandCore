package net.islandcore.webdashboard;

import java.util.List;

public final class JsonUtil {
    private JsonUtil() {}

    public static String metrics(MetricsCollector.Snapshot s) {
        if (s == null) return "{\"error\":\"metrics unavailable\"}";
        StringBuilder b = new StringBuilder();
        b.append("{");
        field(b, "timestamp", Long.toString(s.timestamp()), false);
        field(b, "usedMemory", Long.toString(s.usedMemory()), false);
        field(b, "committedMemory", Long.toString(s.committedMemory()), false);
        field(b, "maxMemory", Long.toString(s.maxMemory()), false);
        field(b, "heapUsed", Long.toString(s.heapUsed()), false);
        field(b, "heapMax", Long.toString(s.heapMax()), false);
        field(b, "onlinePlayers", Integer.toString(s.onlinePlayers()), false);
        field(b, "maxPlayers", Integer.toString(s.maxPlayers()), false);
        field(b, "uptimeMillis", Long.toString(s.uptimeMillis()), false);
        field(b, "tps", Double.toString(s.tps()), false);
        field(b, "cpuUsage", Double.toString(s.cpuUsage()), false);
        field(b, "chunks", Long.toString(s.chunks()), false);
        field(b, "entities", Long.toString(s.entities()), false);
        field(b, "networkInterfaces", Long.toString(s.networkInterfaces()), false);
        field(b, "chatMessages", Long.toString(s.chatMessages()), false);
        field(b, "serverVersion", quote(s.serverVersion()), false);
        field(b, "javaVersion", quote(s.javaVersion()), false);
        field(b, "osName", quote(s.osName()), false);
        field(b, "osArch", quote(s.osArch()), false);
        field(b, "threads", Integer.toString(s.threads()), false);
        field(b, "diskFree", Long.toString(s.diskFree()), false);
        field(b, "diskTotal", Long.toString(s.diskTotal()), false);

        b.append("\"players\":[");
        for (int i = 0; i < s.players().size(); i++) {
            if (i > 0) b.append(',');
            b.append('"').append(esc(s.players().get(i))).append('"');
        }
        b.append("],\"worlds\":[");
        for (int i = 0; i < s.worlds().size(); i++) {
            if (i > 0) b.append(',');
            var w = s.worlds().get(i);
            b.append("{");
            field(b, "name", quote(w.name()), false);
            field(b, "path", quote(w.path()), false);
            field(b, "players", Integer.toString(w.players()), false);
            field(b, "chunks", Integer.toString(w.chunks()), false);
            field(b, "entities", Long.toString(w.entities()), true);
            b.append("}");
        }
        b.append("]}");
        return b.toString();
    }

    public static String status(MetricsCollector.Snapshot s) {
        return metrics(s);
    }

    public static String tradeLogs(List<TradeLogStore.TradeLog> trades) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < trades.size(); i++) {
            if (i > 0) b.append(',');
            TradeLogStore.TradeLog t = trades.get(i);
            b.append("{\"time\":").append(t.time())
                    .append(",\"fileName\":").append(quote(t.fileName()))
                    .append(",\"playerA\":").append(tradePlayer(t.playerA()))
                    .append(",\"playerB\":").append(tradePlayer(t.playerB()))
                    .append("}");
        }
        return b.append(']').toString();
    }

    private static String tradePlayer(TradeLogStore.PlayerTrade p) {
        if (p == null) return "null";
        StringBuilder b = new StringBuilder("{");
        field(b, "uuid", p.uuid() == null ? "null" : quote(p.uuid()), false);
        field(b, "name", p.name() == null ? "null" : quote(p.name()), false);
        b.append("\"itemsGiven\":[");
        for (int i = 0; i < p.itemsGiven().size(); i++) {
            if (i > 0) b.append(',');
            TradeLogStore.TradeItem item = p.itemsGiven().get(i);
            b.append("{\"material\":").append(quote(item.material()))
                    .append(",\"amount\":").append(item.amount()).append('}');
        }
        b.append("]}");
        return b.toString();
    }

    public static String logs(List<HistoryStore.LogLine> lines) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) b.append(',');
            HistoryStore.LogLine l = lines.get(i);
            b.append("{\"time\":").append(l.time())
                    .append(",\"type\":").append(quote(l.type()))
                    .append(",\"player\":").append(l.player() == null ? "null" : quote(l.player()))
                    .append(",\"text\":").append(quote(l.text())).append("}");
        }
        return b.append(']').toString();
    }

    private static void field(StringBuilder b, String key, String value, boolean last) {
        b.append('"').append(key).append("\":").append(value);
        if (!last) b.append(',');
    }

    private static String quote(String s) {
        return "\"" + esc(s) + "\"";
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
