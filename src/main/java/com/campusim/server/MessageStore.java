package com.campusim.server;

import com.campusim.common.Message;
import com.campusim.common.MessageType;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageStore {
    private static final String DB_URL = "jdbc:sqlite:data/campusim.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS messages (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  type TEXT NOT NULL," +
                "  sender TEXT NOT NULL," +
                "  receiver TEXT NOT NULL," +
                "  content TEXT," +
                "  file_name TEXT," +
                "  file_size INTEGER," +
                "  timestamp TEXT NOT NULL," +
                "  is_read INTEGER DEFAULT 1" +
                ")"
            );
            try {
                stmt.executeUpdate("ALTER TABLE messages ADD COLUMN is_read INTEGER DEFAULT 1");
            } catch (SQLException e) { /* column may already exist */ }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveMessage(Message msg) {
        saveMessage(msg, true);
    }

    public static void saveMessage(Message msg, boolean isRead) {
        String sql = "INSERT INTO messages (type, sender, receiver, content, file_name, file_size, timestamp, is_read) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, msg.getType().name());
            pstmt.setString(2, msg.getSender());
            pstmt.setString(3, msg.getReceiver());
            pstmt.setString(4, msg.getContent());
            pstmt.setString(5, msg.getFileName());
            pstmt.setLong(6, msg.getFileSize());
            pstmt.setString(7, msg.getTimestamp());
            pstmt.setInt(8, isRead ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Integer> getUnreadCounts(String username) {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT sender, COUNT(*) AS cnt FROM messages WHERE (type = 'TEXT_P2P' OR type = 'IMAGE_P2P' OR type = 'FILE_INFO') AND receiver = ? AND is_read = 0 GROUP BY sender";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("sender"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void markAsRead(String receiver, String sender) {
        String sql = "UPDATE messages SET is_read = 1 WHERE (type = 'TEXT_P2P' OR type = 'IMAGE_P2P' OR type = 'FILE_INFO') AND receiver = ? AND sender = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiver);
            pstmt.setString(2, sender);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Message> getP2PHistory(String user1, String user2) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE (type = 'TEXT_P2P' OR type = 'IMAGE_P2P' OR type = 'FILE_INFO') AND " +
                     "((sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)) " +
                     "ORDER BY id ASC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user1);
            pstmt.setString(2, user2);
            pstmt.setString(3, user2);
            pstmt.setString(4, user1);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String type = rs.getString("type");
                Message msg;
                if ("IMAGE_P2P".equals(type)) {
                    msg = new Message(MessageType.IMAGE_P2P);
                    msg.setFileName(rs.getString("file_name"));
                    msg.setFileSize(rs.getLong("file_size"));
                } else if ("FILE_INFO".equals(type)) {
                    msg = new Message(MessageType.FILE_INFO);
                    msg.setFileName(rs.getString("file_name"));
                    msg.setFileSize(rs.getLong("file_size"));
                } else {
                    msg = new Message(MessageType.TEXT_P2P);
                }
                msg.setSender(rs.getString("sender"));
                msg.setReceiver(rs.getString("receiver"));
                msg.setContent(rs.getString("content"));
                msg.setTimestamp(rs.getString("timestamp"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public static List<Message> getGroupHistory() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE (type = 'TEXT_GROUP' OR type = 'IMAGE_GROUP' OR (type = 'FILE_INFO' AND receiver = 'GROUP')) ORDER BY id ASC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String type = rs.getString("type");
                Message msg;
                if ("IMAGE_GROUP".equals(type)) {
                    msg = new Message(MessageType.IMAGE_GROUP);
                    msg.setFileName(rs.getString("file_name"));
                    msg.setFileSize(rs.getLong("file_size"));
                } else if ("FILE_INFO".equals(type)) {
                    msg = new Message(MessageType.FILE_INFO);
                    msg.setFileName(rs.getString("file_name"));
                    msg.setFileSize(rs.getLong("file_size"));
                } else {
                    msg = new Message(MessageType.TEXT_GROUP);
                }
                msg.setSender(rs.getString("sender"));
                msg.setReceiver("GROUP");
                msg.setContent(rs.getString("content"));
                msg.setTimestamp(rs.getString("timestamp"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
}
