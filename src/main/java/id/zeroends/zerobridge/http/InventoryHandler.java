package id.zeroends.zerobridge.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import id.zeroends.zerobridge.ZeroBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class InventoryHandler implements HttpHandler {

    private final ZeroBridge plugin;
    private final String validApiKey;

    public InventoryHandler(ZeroBridge plugin, String validApiKey) {
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

        // Ambil parameter query ?player=NamaPlayer
        String query = exchange.getRequestURI().getQuery();
        String playerName = null;
        if (query != null && query.contains("player=")) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && pair[0].equals("player")) {
                    playerName = pair[1];
                    break;
                }
            }
        }

        if (playerName == null) {
            sendResponse(exchange, 400, "{\"error\": \"Missing 'player' parameter\"}");
            return;
        }

        final String targetName = playerName;

        try {
            // Akses Inventory harus di Main Thread
            String jsonResponse = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                Player player = Bukkit.getPlayer(targetName);
                if (player == null) {
                    return "{\"error\": \"Player not found or offline\"}";
                }

                // Ambil isi inventory (non-null items)
                ItemStack[] contents = player.getInventory().getContents();
                String itemsJson = Arrays.stream(contents)
                    .filter(Objects::nonNull) // Hapus slot kosong
                    .map(item -> String.format(
                        "{\"item\": \"%s\", \"amount\": %d}",
                        item.getType().toString(), item.getAmount()
                    ))
                    .collect(Collectors.joining(","));

                // Ambil Armor
                ItemStack[] armor = player.getInventory().getArmorContents();
                String armorJson = Arrays.stream(armor)
                    .filter(Objects::nonNull)
                    .map(item -> String.format("\"%s\"", item.getType().toString()))
                    .collect(Collectors.joining(","));

                return String.format(
                    "{\"player\": \"%s\", \"inventory\": [%s], \"armor\": [%s]}",
                    player.getName(), itemsJson, armorJson
                );
            }).get();

            if (jsonResponse.contains("\"error\": \"Player")) {
                sendResponse(exchange, 404, jsonResponse);
            } else {
                sendResponse(exchange, 200, jsonResponse);
            }

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
