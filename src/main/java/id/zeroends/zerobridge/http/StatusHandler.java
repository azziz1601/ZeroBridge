package id.zeroends.zerobridge.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import id.zeroends.zerobridge.ZeroBridge;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;

public class StatusHandler implements HttpHandler {

    private final ZeroBridge plugin;
    private final String validApiKey;
    private final DecimalFormat df = new DecimalFormat("#.##");

    public StatusHandler(ZeroBridge plugin, String validApiKey) {
        this.plugin = plugin;
        this.validApiKey = validApiKey;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("X-API-Key");
        if (authHeader == null || !authHeader.equals(validApiKey)) {
            sendResponse(exchange, 401, "{\"error\": \"Unauthorized\"}");
            return;
        }

        try {
            String jsonResponse = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                
                // --- 1. General & Config Data ---
                String serverName = Bukkit.getName(); // Paper, Purpur, etc
                String serverVersion = Bukkit.getMinecraftVersion();
                String motd = Bukkit.getMotd().replace("\"", "'").replace("\n", " ");
                boolean isWhitelist = Bukkit.hasWhitelist();
                boolean isOnlineMode = Bukkit.getOnlineMode();
                boolean isPvp = Bukkit.getDefaultGameMode() != org.bukkit.GameMode.CREATIVE; // Simplifikasi
                String difficulty = Bukkit.getWorlds().get(0).getDifficulty().toString();
                String defaultGamemode = Bukkit.getDefaultGameMode().toString();
                
                // --- 2. Limits & Settings ---
                int maxPlayers = Bukkit.getMaxPlayers();
                int viewDistance = Bukkit.getViewDistance();
                int simDistance = Bukkit.getSimulationDistance();
                
                // --- 3. System Info ---
                OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                File root = new File(".");
                long freeDisk = root.getFreeSpace() / 1024 / 1024 / 1024;
                long totalDisk = root.getTotalSpace() / 1024 / 1024 / 1024;
                int cores = osBean.getAvailableProcessors();
                double loadAvg = osBean.getSystemLoadAverage();

                // --- 4. JVM Memory ---
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory() / 1024 / 1024;
                long totalMemory = runtime.totalMemory() / 1024 / 1024;
                long freeMemory = runtime.freeMemory() / 1024 / 1024;
                long usedMemory = totalMemory - freeMemory;

                // --- 5. Performance ---
                double[] tps = Bukkit.getTPS();
                
                // --- 6. World Stats ---
                int totalChunks = 0;
                int totalEntities = 0;
                for (World world : Bukkit.getWorlds()) {
                    totalChunks += world.getLoadedChunks().length;
                    totalEntities += world.getEntityCount();
                }

                // Construct JSON Parts
                String metaJson = String.format(
                    "\"meta\": {\"software\": \"%s\", \"version\": \"%s\", \"motd\": \"%s\", \"online_mode\": %b}",
                    serverName, serverVersion, motd, isOnlineMode
                );

                String configJson = String.format(
                    "\"config\": {\"whitelist\": %b, \"difficulty\": \"%s\", \"gamemode\": \"%s\", \"pvp_enabled\": %b}",
                    isWhitelist, difficulty, defaultGamemode, isPvp
                );

                String settingsJson = String.format(
                    "\"settings\": {\"max_players\": %d, \"view_distance\": %d, \"sim_distance\": %d}",
                    maxPlayers, viewDistance, simDistance
                );

                String performanceJson = String.format(
                    "\"performance\": {\"tps\": [%s, %s, %s], \"cpu_load\": %s, \"ram_used_mb\": %d, \"ram_max_mb\": %d}",
                    df.format(tps[0]), df.format(tps[1]), df.format(tps[2]), df.format(loadAvg), usedMemory, maxMemory
                );
                
                String worldJson = String.format(
                    "\"world\": {\"loaded_chunks\": %d, \"entities\": %d, \"disk_free_gb\": %d}",
                    totalChunks, totalEntities, freeDisk
                );

                return String.format("{%s, %s, %s, %s, %s}", metaJson, configJson, settingsJson, performanceJson, worldJson);

            }).get();

            sendResponse(exchange, 200, jsonResponse);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\": \"Internal Server Error\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
