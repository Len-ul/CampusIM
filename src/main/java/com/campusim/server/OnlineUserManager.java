package com.campusim.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineUserManager {
    private static final ConcurrentHashMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();
    private static final String DB_URL = "jdbc:sqlite:data/campusim.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void addUser(String username, ClientHandler handler) {
        onlineUsers.put(username, handler);
    }

    public static void removeUser(String username) {
        onlineUsers.remove(username);
    }

    public static ClientHandler getHandler(String username) {
        return onlineUsers.get(username);
    }

    public static boolean isOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public static List<String> getOnlineUsers() {
        return new ArrayList<>(onlineUsers.keySet());
    }

    public static Map<String, String> getOnlineIdentities() {
        Map<String, String> map = new HashMap<>();
        for (String username : onlineUsers.keySet()) {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT u.role, s.student_id, t.employee_id FROM users u " +
                     "LEFT JOIN students s ON u.id = s.user_id " +
                     "LEFT JOIN teachers t ON u.id = t.user_id WHERE u.username = ?")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String role = rs.getString("role");
                    String id = null;
                    if ("STUDENT".equals(role)) {
                        id = rs.getString("student_id");
                    } else if ("TEACHER".equals(role)) {
                        id = rs.getString("employee_id");
                    }
                    if (id != null) {
                        map.put(username, id);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    public static int getOnlineCount() {
        return onlineUsers.size();
    }
}
