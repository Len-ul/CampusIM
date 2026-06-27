package com.campusim.client.network;

import com.campusim.common.Message;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ServerConnector {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String serverHost;
    private int serverPort;
    private volatile boolean connected;
    private MessageListener listener;

    public interface MessageListener {
        void onMessageReceived(Message msg);
        void onConnectionError(String error);
    }

    public ServerConnector(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            new Thread(() -> {
                while (connected) {
                    try {
                        Message msg = (Message) in.readObject();
                        if (msg != null && listener != null) {
                            listener.onMessageReceived(msg);
                        }
                    } catch (EOFException | SocketException e) {
                        break;
                    } catch (IOException | ClassNotFoundException e) {
                        if (connected) {
                            listener.onConnectionError("连接断开: " + e.getMessage());
                        }
                        break;
                    }
                }
            }, "MessageReceiver").start();

            return true;
        } catch (IOException e) {
            if (listener != null) {
                listener.onConnectionError("连接服务器失败: " + e.getMessage());
            }
            return false;
        }
    }

    public void sendMessage(Message msg) {
        try {
            if (out != null) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onConnectionError("发送消息失败: " + e.getMessage());
            }
        }
    }

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public void disconnect() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
