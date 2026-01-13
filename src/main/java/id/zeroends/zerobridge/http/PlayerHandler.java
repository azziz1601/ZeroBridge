package id.zeroends.zerobridge.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import id.zeroends.zerobridge.ZeroBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PlayerHandler implements HttpHandler {

    private final ZeroBridge plugin;
    private final String validApiKey;

    public PlayerHandler(ZeroBridge plugin, String validApiKey) {
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
            String response = "{\"error\": \"Unauthorized\"}";
            sendResponse(exchange, 401, response);
            return;
        }

        try {
            String jsonResponse = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                String playerList = players.stream()
                        .map(p -> "\"" + p.getName() + "\"")
                        .collect(Collectors.joining(","));
                
                return String.format(
                    "{" +
                    "\"count\": %d," +
                    "\"max\": %d," +
                    "\"list\": [%s]" +
                    "}",
                    players.size(), Bukkit.getMaxPlayers(), playerList
                );
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
