package id.zeroends.zerobridge;

import id.zeroends.zerobridge.http.ApiServer;
import id.zeroends.zerobridge.http.ChatSocketServer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;

public class ZeroBridge extends JavaPlugin {

    private ApiServer apiServer;
    private ChatSocketServer chatSocketServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // 1. Start HTTP API (Port 8081)
        int httpPort = getConfig().getInt("server.port", 8081);
        String apiKey = getConfig().getString("server.api-key");
        try {
            apiServer = new ApiServer(this, httpPort, apiKey);
            apiServer.start();
            getLogger().info("API Server started on port " + httpPort);
        } catch (IOException e) {
            getLogger().severe("Failed to start API Server: " + e.getMessage());
        }

        // 2. Start WebSocket Chat (Port 8082)
        int wsPort = getConfig().getInt("websocket.port", 8082);
        chatSocketServer = new ChatSocketServer(this, wsPort);
        chatSocketServer.start();

        // 3. Register Events
        getServer().getPluginManager().registerEvents(new ChatListener(chatSocketServer), this);
    }

    @Override
    public void onDisable() {
        if (apiServer != null) {
            apiServer.stop();
        }
        if (chatSocketServer != null) {
            try {
                chatSocketServer.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        getLogger().info("ZeroBridge stopped.");
    }
}
