package com.mmtext.editorservermain.controller;

import com.mmtext.editorservermain.service.ZooKeeperConsistentHashingService;
import com.mmtext.editorservermain.service.ZooKeeperServiceRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Load Balancer Controller with ZooKeeper-based service discovery
 *
 * Features:
 * - Dynamic server discovery from ZooKeeper
 * - Consistent hashing for server selection
 * - Real-time server health monitoring
 * - Preferred server routing through client nginx
 * - Authentication and authorization for secure access
 */
@RestController
@RequestMapping("/api/loadbalancer")
@CrossOrigin(origins = "*")
public class LoadBalancerController {

    private final ZooKeeperConsistentHashingService consistentHashing;
    private final ZooKeeperServiceRegistry serviceRegistry;

    @Value("${app.client.base-url:localhost:4200}")
    private String clientBaseUrl;

    public LoadBalancerController(ZooKeeperConsistentHashingService consistentHashing,
                                  ZooKeeperServiceRegistry serviceRegistry) {
        this.consistentHashing = consistentHashing;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Get server assignment for a user/document using consistent hashing
     * Requires DOCUMENT_ACCESS permission
     *
     * Example: GET /api/loadbalancer/server?key=user123
     */
    @GetMapping("/server")
    @PreAuthorize("hasAuthority('PERMISSION_DOCUMENT_ACCESS')")
    public ResponseEntity<Map<String, String>> getServerForKey(
            @RequestParam String key,
            Authentication authentication) {

        // Get authenticated user
        String userId = authentication.getName();

        // TODO: Add document ownership/permission check here
        // For now, any authenticated user with DOCUMENT_ACCESS can access any document
        // In production, you might want to check:
        // if (!documentService.canUserAccessDocument(userId, key)) {
        //     return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
        //         "error", "Access denied",
        //         "message", "You don't have permission to access this document"
        //     ));
        // }

        String server = consistentHashing.getServer(key);

        if (server == null) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "No servers available",
                    "message", "All servers are currently unavailable"
            ));
        }

        // Build WebSocket URL pointing to client nginx with preferred server
        String wsUrl = String.format("ws://%s/ws/editor?doc=%s&preferred=%s",
                                    clientBaseUrl, key, server);

        // Also provide direct API URLs if needed
        String apiUrl = String.format("http://%s/api", clientBaseUrl);
        String serverInfo = serviceRegistry.getServerInfo(server);

        return ResponseEntity.ok(Map.of(
                "documentId", key,
                "serverId", server,
                "serverAddress", serverInfo != null ? serverInfo : "unknown",
                "wsUrl", wsUrl,
                "apiUrl", apiUrl,
                "source", "zookeeper-with-preferred-routing",
                "routing", "client-nginx",
                "userId", userId
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
                "hashRingSize", consistentHashing.getHashRingSize(),
                "websocketRouting", "client-nginx-with-preference"
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
                "source", "zookeeper",
                "routingMode", "preferred-server-via-client-nginx"
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
                "zookeeper", "connected",
                "routingMode", "preferred-server-via-client-nginx"
        ));
    }
}
