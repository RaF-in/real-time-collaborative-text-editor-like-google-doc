package com.mmtext.editorservermain.service;


import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZooKeeper-based service registry for dynamic service discovery
 *
 * Architecture:
 * - Each application server registers itself in ZooKeeper on startup
 * - Creates ephemeral node: /editor/servers/{serverId}
 * - Load balancer watches /editor/servers for changes
 * - Automatic deregistration when server goes down (ephemeral node)
 * - Consistent hashing uses live servers from ZooKeeper
 */
@Service
public class ZooKeeperServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperServiceRegistry.class);

    private static final String SERVERS_PATH = "/editor/servers";
    private static final String HEALTH_PATH = "/editor/health";

    @Value("${zookeeper.connection-string:localhost:2181}")
    private String zookeeperConnectionString;

    @Value("${editor.server.id}")
    private String serverId;

    @Value("${server.port}")
    private int serverPort;

    private CuratorFramework client;
    private PathChildrenCache serversCache;

    // Live servers discovered from ZooKeeper
    private final Set<String> liveServers = ConcurrentHashMap.newKeySet();

    // Listeners for server changes
    private final List<ServerChangeListener> listeners = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            // Initialize ZooKeeper client
            client = CuratorFrameworkFactory.builder()
                    .connectString(zookeeperConnectionString)
                    .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                    .sessionTimeoutMs(30000)
                    .connectionTimeoutMs(15000)
                    .build();

            client.start();
            logger.info("ZooKeeper client started, connecting to: {}", zookeeperConnectionString);

            // Wait for connection
            client.blockUntilConnected();
            logger.info("Connected to ZooKeeper");

            // Create base paths if they don't exist
            createPathIfNotExists(SERVERS_PATH);
            createPathIfNotExists(HEALTH_PATH);

            // Register this server
            registerServer();

            // Watch for server changes
            watchServers();

            logger.info("ZooKeeper service registry initialized for server: {}", serverId);

        } catch (Exception e) {
            logger.error("Failed to initialize ZooKeeper service registry", e);
            throw new RuntimeException("ZooKeeper initialization failed", e);
        }
    }

    /**
     * Register this server in ZooKeeper as an ephemeral node
     * Node will be automatically deleted when connection is lost
     */
    private void registerServer() throws Exception {
        String serverPath = SERVERS_PATH + "/" + serverId;

        // Get server address
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        String serverInfo = hostAddress + ":" + serverPort;

        // Create ephemeral node (auto-deleted on disconnect)
        if (client.checkExists().forPath(serverPath) != null) {
            client.delete().forPath(serverPath);
        }

        client.create()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(serverPath, serverInfo.getBytes());

        logger.info("Registered server in ZooKeeper: {} -> {}", serverId, serverInfo);

        // Add to live servers
        liveServers.add(serverId);
    }

    /**
     * Watch for changes in registered servers
     */
    private void watchServers() throws Exception {
        serversCache = new PathChildrenCache(client, SERVERS_PATH, true);

        serversCache.getListenable().addListener((client, event) -> {
            handleServerChange(event);
        });

        serversCache.start();
        logger.info("Started watching for server changes in ZooKeeper");
    }

    /**
     * Handle server registration/deregistration events
     */
    private void handleServerChange(PathChildrenCacheEvent event) {
        try {
            String path = event.getData() != null ? event.getData().getPath() : null;
            String serverName = path != null ? path.substring(path.lastIndexOf('/') + 1) : null;

            switch (event.getType()) {
                case CHILD_ADDED:
                    logger.info("Server registered: {}", serverName);
                    if (serverName != null) {
                        liveServers.add(serverName);
                        notifyServerAdded(serverName);
                    }
                    break;

                case CHILD_REMOVED:
                    logger.warn("Server deregistered: {}", serverName);
                    if (serverName != null) {
                        liveServers.remove(serverName);
                        notifyServerRemoved(serverName);
                    }
                    break;

                case CHILD_UPDATED:
                    logger.debug("Server updated: {}", serverName);
                    break;

                case CONNECTION_LOST:
                    logger.error("ZooKeeper connection lost!");
                    break;

                case CONNECTION_RECONNECTED:
                    logger.info("ZooKeeper connection restored");
                    // Re-register this server
                    registerServer();
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("Error handling server change event", e);
        }
    }

    /**
     * Get all currently live servers from ZooKeeper
     */
    public Set<String> getLiveServers() {
        Set<String> liveServersConcurrent = ConcurrentHashMap.newKeySet();
        liveServersConcurrent.addAll(liveServers);
        return liveServersConcurrent;

    }

    /**
     * Get server info from ZooKeeper
     */
    public String getServerInfo(String serverId) {
        try {
            String path = SERVERS_PATH + "/" + serverId;
            byte[] data = client.getData().forPath(path);
            return new String(data);
        } catch (Exception e) {
            logger.error("Failed to get server info for: {}", serverId, e);
            return null;
        }
    }

    /**
     * Update server health status
     */
    public void updateHealthStatus(String status) {
        try {
            String healthPath = HEALTH_PATH + "/" + serverId;

            if (client.checkExists().forPath(healthPath) != null) {
                client.setData().forPath(healthPath, status.getBytes());
            } else {
                client.create()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(healthPath, status.getBytes());
            }

            logger.debug("Updated health status: {}", status);
        } catch (Exception e) {
            logger.error("Failed to update health status", e);
        }
    }

    /**
     * Check if a specific server is alive
     */
    public boolean isServerAlive(String serverId) {
        return liveServers.contains(serverId);
    }

    /**
     * Get total number of live servers
     */
    public int getLiveServerCount() {
        return liveServers.size();
    }

    /**
     * Register listener for server changes
     */
    public void addServerChangeListener(ServerChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyServerAdded(String serverId) {
        for (ServerChangeListener listener : listeners) {
            try {
                listener.onServerAdded(serverId);
            } catch (Exception e) {
                logger.error("Error notifying listener of server addition", e);
            }
        }
    }

    private void notifyServerRemoved(String serverId) {
        for (ServerChangeListener listener : listeners) {
            try {
                listener.onServerRemoved(serverId);
            } catch (Exception e) {
                logger.error("Error notifying listener of server removal", e);
            }
        }
    }

    /**
     * Create path if it doesn't exist
     */
    private void createPathIfNotExists(String path) throws Exception {
        if (client.checkExists().forPath(path) == null) {
            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(path);
            logger.info("Created ZooKeeper path: {}", path);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (serversCache != null) {
                serversCache.close();
            }
            if (client != null) {
                client.close();
            }
            logger.info("ZooKeeper service registry cleaned up");
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }

    /**
     * Interface for server change notifications
     */
    public interface ServerChangeListener {
        void onServerAdded(String serverId);
        void onServerRemoved(String serverId);
    }
}
