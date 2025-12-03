package com.mmtext.editorservermain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Consistent Hashing with ZooKeeper-based service discovery
 *
 * Dynamically updates hash ring based on live servers from ZooKeeper
 */
@Service
public class ZooKeeperConsistentHashingService implements ZooKeeperServiceRegistry.ServerChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperConsistentHashingService.class);

    private static final int VIRTUAL_NODES = 150;

    private final NavigableMap<Long, String> hashRing;
    private final ZooKeeperServiceRegistry serviceRegistry;

    public ZooKeeperConsistentHashingService(ZooKeeperServiceRegistry serviceRegistry) {
        this.hashRing = new ConcurrentSkipListMap<>();
        this.serviceRegistry = serviceRegistry;
    }

    @PostConstruct
    public void init() {
        // Register as listener for server changes
        serviceRegistry.addServerChangeListener(this);

        // Initialize hash ring with current servers
        Set<String> liveServers = serviceRegistry.getLiveServers();
        for (String server : liveServers) {
            addServerToRing(server);
        }

        logger.info("Consistent hashing initialized with {} servers", liveServers.size());
    }

    @Override
    public void onServerAdded(String serverId) {
        addServerToRing(serverId);
        logger.info("Added server to hash ring: {} (total nodes: {})",
                serverId, hashRing.size());
    }

    @Override
    public void onServerRemoved(String serverId) {
        removeServerFromRing(serverId);
        logger.info("Removed server from hash ring: {} (remaining nodes: {})",
                serverId, hashRing.size());
    }

    /**
     * Add server to hash ring with virtual nodes
     */
    private void addServerToRing(String server) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            String virtualNodeKey = server + "#" + i;
            long hash = hash(virtualNodeKey);
            hashRing.put(hash, server);
        }
    }

    /**
     * Remove server from hash ring
     */
    private void removeServerFromRing(String server) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            String virtualNodeKey = server + "#" + i;
            long hash = hash(virtualNodeKey);
            hashRing.remove(hash);
        }
    }

    /**
     * Get server for a given key using consistent hashing
     */
    public String getServer(String key) {
        if (hashRing.isEmpty()) {
            logger.error("No servers available in hash ring");
            return null;
        }

        long hash = hash(key);

        // Find the first server with hash >= key hash
        Map.Entry<Long, String> entry = hashRing.ceilingEntry(hash);

        // If not found, wrap around to the first server
        if (entry == null) {
            entry = hashRing.firstEntry();
        }

        String server = entry.getValue();

        // Verify server is still alive in ZooKeeper
        if (!serviceRegistry.isServerAlive(server)) {
            logger.warn("Selected server {} is not alive, finding alternative", server);
            return getNextAvailableServer(hash);
        }

        logger.debug("Key '{}' mapped to server '{}'", key, server);
        return server;
    }

    /**
     * Get next available server if selected server is down
     */
    private String getNextAvailableServer(long hash) {
        // Try next servers in the ring
        for (Map.Entry<Long, String> entry : hashRing.tailMap(hash).entrySet()) {
            if (serviceRegistry.isServerAlive(entry.getValue())) {
                return entry.getValue();
            }
        }

        // Wrap around
        for (Map.Entry<Long, String> entry : hashRing.entrySet()) {
            if (serviceRegistry.isServerAlive(entry.getValue())) {
                return entry.getValue();
            }
        }

        logger.error("No available servers found");
        return null;
    }

    /**
     * Get server URL for WebSocket connection
     */
    public String getServerWebSocketUrl(String key) {
        String server = getServer(key);
        if (server == null) {
            return null;
        }

        String serverInfo = serviceRegistry.getServerInfo(server);
        if (serverInfo == null) {
            return null;
        }

        // serverInfo format: "192.168.1.100:8080"
        return "ws://" + serverInfo + "/ws/editor";
    }

    /**
     * Get all available servers
     */
    public Set<String> getAvailableServers() {
        return serviceRegistry.getLiveServers();
    }

    /**
     * Hash function using MD5
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));

            // Convert first 8 bytes to long
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }

            return hash;

        } catch (NoSuchAlgorithmException e) {
            logger.error("MD5 algorithm not available", e);
            return key.hashCode();
        }
    }

    /**
     * Get distribution statistics
     */
    public Map<String, Integer> getDistributionStats(List<String> keys) {
        Map<String, Integer> stats = new HashMap<>();

        for (String server : getAvailableServers()) {
            stats.put(server, 0);
        }

        for (String key : keys) {
            String server = getServer(key);
            if (server != null) {
                stats.merge(server, 1, Integer::sum);
            }
        }

        return stats;
    }

    /**
     * Get current hash ring size
     */
    public int getHashRingSize() {
        return hashRing.size();
    }
}
