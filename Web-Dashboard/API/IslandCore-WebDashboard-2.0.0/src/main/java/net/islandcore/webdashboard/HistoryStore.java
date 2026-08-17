package net.islandcore.webdashboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class HistoryStore {
    public record LogLine(long time, String text, String type, String player) {}

    private final int chatLimit;
    private final Deque<LogLine> chat = new ArrayDeque<>();
    private final Object lock = new Object();

    public HistoryStore(int chatLimit) {
        this.chatLimit = Math.max(50, chatLimit);
    }

    public void addChat(String player, String message) {
        String text = "<" + player + "> " + message;
        synchronized (lock) {
            chat.addLast(new LogLine(System.currentTimeMillis(), text, "chat", player));
            while (chat.size() > chatLimit) chat.removeFirst();
        }
    }

    public List<LogLine> chatSnapshot(int limit) {
        synchronized (lock) {
            return tail(chat, limit);
        }
    }

    public int chatCapacity() { return chatLimit; }

    private static List<LogLine> tail(Deque<LogLine> source, int limit) {
        List<LogLine> result = new ArrayList<>();
        int skip = Math.max(0, source.size() - Math.max(1, limit));
        int i = 0;
        for (LogLine line : source) if (i++ >= skip) result.add(line);
        return result;
    }
}
