package id.zeroends.zerobridge;

import id.zeroends.zerobridge.http.ChatSocketServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final ChatSocketServer socketServer;

    public ChatListener(ChatSocketServer socketServer) {
        this.socketServer = socketServer;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Kirim ke WebSocket
        socketServer.broadcastToWeb(event.getPlayer().getName(), event.getMessage());
    }
}
