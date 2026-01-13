package id.zeroends.zerobridge.http;

import com.sun.net.httpserver.HttpServer;
import id.zeroends.zerobridge.ZeroBridge;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ApiServer {

    private final ZeroBridge plugin;
    private final int port;
    private final String apiKey;
    private HttpServer server;

    public ApiServer(ZeroBridge plugin, int port, String apiKey) {
        this.plugin = plugin;
        this.port = port;
        this.apiKey = apiKey;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/api/status", new StatusHandler(plugin, apiKey));
        server.createContext("/api/players", new PlayerHandler(plugin, apiKey));
        server.createContext("/api/console", new ConsoleHandler(plugin, apiKey));
        server.createContext("/api/inventory", new InventoryHandler(plugin, apiKey));
        
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
