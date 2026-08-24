package dev.rpgmenu.framework.common.inventory;

import dev.rpgmenu.framework.api.inventory.InventoryPage;
import dev.rpgmenu.framework.api.inventory.InventoryQuery;
import dev.rpgmenu.framework.api.inventory.TransactionResult;
import dev.rpgmenu.framework.api.inventory.UnifiedItemEntry;
import dev.rpgmenu.framework.common.network.payload.InventoryPagePayload;
import net.minecraft.server.level.ServerPlayer;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Owns opaque per-player query state. It never trusts a client-supplied item identity. */
public final class MenuSessionManager {
    public static final MenuSessionManager INSTANCE = new MenuSessionManager();
    private static final long EXPIRES_NANOS = Duration.ofMinutes(5).toNanos();
    private static final int MAX_SESSIONS_PER_PLAYER = 8;
    private final Map<UUID, LinkedHashMap<UUID, Session>> sessions = new LinkedHashMap<>();

    private MenuSessionManager() {}

    public synchronized InventoryPagePayload query(ServerPlayer player, UUID sessionId, InventoryQuery query) {
        LinkedHashMap<UUID, Session> playerSessions = sessions.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashMap<>());
        clean(playerSessions);
        Session session = playerSessions.computeIfAbsent(sessionId, Session::new);
        while (playerSessions.size() > MAX_SESSIONS_PER_PLAYER) {
            Iterator<UUID> iterator = playerSessions.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        InventoryPage page = UnifiedInventoryService.INSTANCE.query(player, query);
        session.touch(page.storageRevision());
        session.bindings.clear();
        var networkEntries = page.entries().stream().map(entry -> {
            long opaqueId = session.nextOpaqueId++;
            session.bindings.put(opaqueId, entry);
            return new InventoryPagePayload.Entry(opaqueId, entry.displayStack(), entry.amount(), entry.sources().size());
        }).toList();
        return new InventoryPagePayload(sessionId, page.storageRevision(), page.page(), page.pageSize(), page.totalEntries(), networkEntries);
    }

    public synchronized Optional<SessionAccess> resolve(ServerPlayer player, UUID sessionId, long opaqueId) {
        LinkedHashMap<UUID, Session> playerSessions = sessions.get(player.getUUID());
        if (playerSessions == null) return Optional.empty();
        Session session = playerSessions.get(sessionId);
        if (session == null || expired(session)) return Optional.empty();
        UnifiedItemEntry entry = session.bindings.get(opaqueId);
        if (entry == null) return Optional.empty();
        session.lastAccessNanos = System.nanoTime();
        return Optional.of(new SessionAccess(session, entry));
    }

    public synchronized boolean active(ServerPlayer player, UUID sessionId) {
        LinkedHashMap<UUID, Session> playerSessions = sessions.get(player.getUUID());
        if (playerSessions == null) return false;
        Session session = playerSessions.get(sessionId);
        if (session == null || expired(session)) return false;
        session.lastAccessNanos = System.nanoTime();
        return true;
    }

    public synchronized Optional<TransactionResult> replay(ServerPlayer player, UUID sessionId, long nonce) {
        LinkedHashMap<UUID, Session> playerSessions = sessions.get(player.getUUID());
        if (playerSessions == null) return Optional.empty();
        Session session = playerSessions.get(sessionId);
        return session == null || expired(session) ? Optional.empty() : Optional.ofNullable(session.results.get(nonce));
    }

    public synchronized void remember(ServerPlayer player, UUID sessionId, long nonce, TransactionResult result) {
        LinkedHashMap<UUID, Session> playerSessions = sessions.get(player.getUUID());
        if (playerSessions == null) return;
        Session session = playerSessions.get(sessionId);
        if (session == null || expired(session)) return;
        session.results.put(nonce, result);
        while (session.results.size() > 128) session.results.remove(session.results.keySet().iterator().next());
    }

    public synchronized void close(ServerPlayer player, UUID sessionId) {
        Map<UUID, Session> playerSessions = sessions.get(player.getUUID());
        if (playerSessions != null) playerSessions.remove(sessionId);
    }

    private static void clean(Map<UUID, Session> map) { map.values().removeIf(MenuSessionManager::expired); }
    private static boolean expired(Session session) { return System.nanoTime() - session.lastAccessNanos > EXPIRES_NANOS; }

    public static final class SessionAccess {
        private final Session session;
        private final UnifiedItemEntry entry;
        private SessionAccess(Session session, UnifiedItemEntry entry) { this.session = session; this.entry = entry; }
        public UnifiedItemEntry entry() { return entry; }
        public long revision() { return session.revision; }
        public Optional<TransactionResult> replay(long nonce) { return Optional.ofNullable(session.results.get(nonce)); }
        public void remember(long nonce, TransactionResult result) {
            session.results.put(nonce, result);
            while (session.results.size() > 128) session.results.remove(session.results.keySet().iterator().next());
        }
    }

    private static final class Session {
        private final UUID id;
        private long revision;
        private long lastAccessNanos;
        private long nextOpaqueId = 1;
        private final Map<Long, UnifiedItemEntry> bindings = new LinkedHashMap<>();
        private final LinkedHashMap<Long, TransactionResult> results = new LinkedHashMap<>();
        private Session(UUID id) { this.id = id; touch(0); }
        private void touch(long revision) { this.revision = revision; this.lastAccessNanos = System.nanoTime(); }
    }
}
