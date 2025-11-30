package com.mmtext.editorserver.controller;

import com.mmtext.editorserver.service.ZooKeeperConsistentHashingService;
import com.mmtext.editorserver.service.ZooKeeperServiceRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Load Balancer Controller with ZooKeeper-based service discovery
 *
 * Features:
 * - Dynamic server discovery from ZooKeeper
 * - Consistent hashing for server selection
 * - Real-time server health monitoring
 */
@RestController
@RequestMapping("/api/loadbalancer")
@CrossOrigin(origins = "*")
public class LoadBalancerController {

    private final ZooKeeperConsistentHashingService consistentHashing;
    private final ZooKeeperServiceRegistry serviceRegistry;

    public LoadBalancerController(ZooKeeperConsistentHashingService consistentHashing,
                                  ZooKeeperServiceRegistry serviceRegistry) {
        this.consistentHashing = consistentHashing;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Get server assignment for a user/document using consistent hashing
     *
     * Example: GET /api/loadbalancer/server?key=user123
     */
    @GetMapping("/server")
    public ResponseEntity<Map<String, String>> getServerForKey(@RequestParam String key) {
        String server = consistentHashing.getServer(key);

        if (server == null) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "No servers available",
                    "message", "All servers are currently unavailable"
            ));
        }

        String wsUrl = consistentHashing.getServerWebSocketUrl(key);
        String serverInfo = serviceRegistry.getServerInfo(server);

        return ResponseEntity.ok(Map.of(
                "key", key,
                "serverId", server,
                "serverAddress", serverInfo != null ? serverInfo : "unknown",
                "wsUrl", wsUrl != null ? wsUrl : "unavailable",
                "source", "zookeeper"
        ));
    }

    /**
     * Get all available servers from ZooKeeper
     */
    @GetMapping("/servers")
    public ResponseEntity<Map<String, Object>> getAvailableServers() {
        Set<String> servers = consistentHashing.getAvailableServers();

        List<Map<String, String>> serverDetails = new ArrayList<>();
        for (String serverId : servers) {
            String serverInfo = serviceRegistry.getServerInfo(serverId);
            boolean alive = serviceRegistry.isServerAlive(serverId);

            serverDetails.add(Map.of(
                    "serverId", serverId,
                    "address", serverInfo != null ? serverInfo : "unknown",
                    "status", alive ? "UP" : "DOWN"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "servers", serverDetails,
                "count", servers.size(),
                "source", "zookeeper",
                "hashRingSize", consistentHashing.getHashRingSize()
        ));
    }

    /**
     * Test consistent hashing distribution
     */
    @GetMapping("/distribution")
    public ResponseEntity<Map<String, Object>> testDistribution(
            @RequestParam(defaultValue = "1000") int sampleSize) {

        // Generate sample keys
        List<String> sampleKeys = new ArrayList<>();
        for (int i = 0; i < sampleSize; i++) {
            sampleKeys.add("user-" + i);
        }

        // Get distribution stats
        Map<String, Integer> stats = consistentHashing.getDistributionStats(sampleKeys);

        // Calculate distribution quality
        int avgPerServer = sampleSize / stats.size();
        double variance = stats.values().stream()
                .mapToDouble(count -> Math.pow(count - avgPerServer, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);

        return ResponseEntity.ok(Map.of(
                "sampleSize", sampleSize,
                "serverCount", stats.size(),
                "distribution", stats,
                "averagePerServer", avgPerServer,
                "standardDeviation", stdDev,
                "source", "zookeeper"
        ));
    }

    /**
     * Health check for load balancer
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        int serverCount = serviceRegistry.getLiveServerCount();
        boolean healthy = serverCount > 0;

        return ResponseEntity.ok(Map.of(
                "status", healthy ? "UP" : "DOWN",
                "liveServers", serverCount,
                "hashRingNodes", consistentHashing.getHashRingSize(),
                "zookeeper", "connected"
        ));
    }
}
