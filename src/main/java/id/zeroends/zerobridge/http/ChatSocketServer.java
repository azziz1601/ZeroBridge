package id.zeroends.zerobridge.http;

import id.zeroends.zerobridge.ZeroBridge;
import org.bukkit.Bukkit;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class ChatSocketServer extends WebSocketServer {

    private final ZeroBridge plugin;

    public ChatSocketServer(ZeroBridge plugin, int port) {
        super(new InetSocketAddress(port));
        this.plugin = plugin;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        plugin.getLogger().info("Web Client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // Silent
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Format pesan dari web diharapkan JSON: {"sender": "AdminWeb", "message": "Halo player"}
        // Tapi untuk simplifikasi, kita terima raw text dulu atau parsing manual simpel
        
        // Jalankan di Main Thread agar aman saat broadcast ke server
        Bukkit.getScheduler().runTask(plugin, () -> {
             Bukkit.broadcastMessage("§b[Web] §f" + message);
        });
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        plugin.getLogger().info("WebSocket Chat Server started on port " + getPort());
    }

    // Fungsi untuk mengirim JSON Chat ke Web
    public void broadcastToWeb(String sender, String message) {
        // Escape quote agar JSON valid
        String safeSender = sender.replace("\"", "\\\"");
        String safeMessage = message.replace("\"", "\\\"");
        
        String json = String.format("{\"type\": \"chat\", \"sender\": \"%s\", \"message\": \"%s\"}", safeSender, safeMessage);
        broadcast(json);
    }
}
