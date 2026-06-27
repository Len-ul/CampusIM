package com.campusim.server;

import com.campusim.common.Message;

import java.io.IOException;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CampusIMServer {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running;

    public void start(int port) {
        try {
            UserManager.initDatabase();
            MessageStore.initDatabase();
            new File("data/avatars").mkdirs();

            serverSocket = new ServerSocket(port);
            threadPool = Executors.newCachedThreadPool();
            running = true;

            System.out.println("CampusIM Server started on port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New connection from " + clientSocket.getInetAddress());
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    threadPool.execute(handler);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public void broadcast(Message msg, String excludeUser) {
        for (String username : OnlineUserManager.getOnlineUsers()) {
            if (excludeUser != null && username.equals(excludeUser)) continue;
            ClientHandler handler = OnlineUserManager.getHandler(username);
            if (handler != null) {
                handler.sendMessage(msg);
            }
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        System.out.println("Server shutdown complete.");
    }

    public static void main(String[] args) {
        int port = PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, using default: " + PORT);
            }
        }
        new CampusIMServer().start(port);
    }
}
