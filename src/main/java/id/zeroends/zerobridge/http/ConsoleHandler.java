package id.zeroends.zerobridge.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import id.zeroends.zerobridge.ZeroBridge;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class ConsoleHandler implements HttpHandler {

    private final ZeroBridge plugin;
    private final String validApiKey;

    public ConsoleHandler(ZeroBridge plugin, String validApiKey) {
        this.plugin = plugin;
        this.validApiKey = validApiKey;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("X-API-Key");
        if (authHeader == null || !authHeader.equals(validApiKey)) {
            sendResponse(exchange, 401, "{\"error\": \"Unauthorized\"}");
            return;
        }

        // Baca Body Request
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder buf = new StringBuilder();
        int b;
        while ((b = br.read()) != -1) {
            buf.append((char) b);
        }
        String body = buf.toString();
        br.close();
        isr.close();

        // Parse Manual JSON Sederhana untuk mengambil value "command"
        // Format diharapkan: {"command": "say hello world"}
        String cmd = extractCommand(body);

        if (cmd == null || cmd.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\": \"Invalid JSON or missing 'command' field\"}");
            return;
        }

        try {
            // Eksekusi di Main Thread (WAJIB untuk kestabilan server)
            boolean success = Bukkit.getScheduler().callSyncMethod(plugin, () -> 
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            ).get();

            String jsonResponse = String.format("{\"status\": \"executed\", \"command\": \"%s\", \"success\": %b}", cmd, success);
            sendResponse(exchange, 200, jsonResponse);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\": \"Execution failed\"}");
        }
    }

    // Helper sederhana parsing JSON string
    private String extractCommand(String json) {
        try {
            int keyIndex = json.indexOf("\"command\"");
            if (keyIndex == -1) return null;
            
            int colonIndex = json.indexOf(":", keyIndex);
            int firstQuote = json.indexOf("\"", colonIndex + 1);
            int lastQuote = json.lastIndexOf("\""); // Ambil quote terakhir (hati-hati jika ada quote di dalam command)
            
            // Logika sederhana: ambil string di antara quote pertama setelah colon dan quote terakhir sebelum kurung kurawal
            // Untuk command kompleks disarankan pakai library Gson, tapi ini cukup untuk basic command
            if (firstQuote != -1 && lastQuote > firstQuote) {
                return json.substring(firstQuote + 1, lastQuote);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
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
