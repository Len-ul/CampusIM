package com.campusim.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserManager {
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
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  username TEXT UNIQUE NOT NULL CHECK(length(username) BETWEEN 2 AND 10)," +
                "  password TEXT NOT NULL," +
                "  role TEXT NOT NULL DEFAULT 'STUDENT' CHECK(role IN ('STUDENT', 'TEACHER', 'ADMIN'))," +
                "  gender TEXT CHECK(gender IN ('男', '女'))," +
                "  avatar_url TEXT," +
                "  signature TEXT DEFAULT '这家伙很懒，什么都没有留下~'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS students (" +
                "  user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE," +
                "  student_id TEXT UNIQUE NOT NULL CHECK(length(student_id) = 8 AND student_id GLOB 'S[0-9][0-9][0-9][0-9][0-9][0-9][0-9]')," +
                "  grade TEXT NOT NULL CHECK(length(grade) = 4)," +
                "  major TEXT NOT NULL CHECK(length(major) BETWEEN 2 AND 10)," +
                "  class_name TEXT NOT NULL CHECK(length(class_name) = 4)," +
                "  department TEXT NOT NULL CHECK(length(department) BETWEEN 4 AND 12)" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS teachers (" +
                "  user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE," +
                "  employee_id TEXT UNIQUE NOT NULL CHECK(length(employee_id) = 8 AND employee_id GLOB 'T[0-9][0-9][0-9][0-9][0-9][0-9][0-9]')," +
                "  title TEXT NOT NULL CHECK(length(title) BETWEEN 2 AND 10)," +
                "  department TEXT NOT NULL CHECK(length(department) BETWEEN 4 AND 12)" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS departments (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  name TEXT UNIQUE NOT NULL" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS majors (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  name TEXT NOT NULL," +
                "  department_id INTEGER NOT NULL REFERENCES departments(id) ON DELETE CASCADE," +
                "  UNIQUE(name, department_id)" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS group_files (" +
                "  file_id TEXT PRIMARY KEY," +
                "  file_name TEXT NOT NULL," +
                "  file_size INTEGER NOT NULL," +
                "  sender TEXT NOT NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS group_file_downloads (" +
                "  file_id TEXT NOT NULL REFERENCES group_files(file_id)," +
                "  username TEXT NOT NULL," +
                "  is_downloaded INTEGER DEFAULT 0," +
                "  downloaded_at TIMESTAMP," +
                "  PRIMARY KEY (file_id, username)" +
                ")"
            );
            initDepartmentData();
            initPresetUsers();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void initDepartmentData() {
        String[][] deptMajors = {
            {"电气与动力工程学院", "电气工程及其自动化", "自动化"},
            {"机械与运载工程学院", "机械设计制造及其自动化", "机械电子工程", "机器人工程", "车辆工程"},
            {"计算机学院", "计算机科学与技术", "物联网工程", "信息安全"},
            {"大数据学院", "数据科学与大数据技术", "人工智能"},
            {"软件学院", "软件工程"},
            {"外国语学院", "英语"},
            {"土木工程学院", "土木工程"},
            {"艺术学院", "动画", "影视摄影与制作", "音乐表演", "舞蹈表演"}
        };
        for (String[] row : deptMajors) {
            String deptName = row[0];
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT OR IGNORE INTO departments (name) VALUES (?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, deptName);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    long deptId = rs.getLong(1);
                    try (PreparedStatement pm = conn.prepareStatement(
                             "INSERT OR IGNORE INTO majors (name, department_id) VALUES (?, ?)")) {
                        for (int i = 1; i < row.length; i++) {
                            pm.setString(1, row[i]);
                            pm.setLong(2, deptId);
                            pm.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) { /* ignore */ }
        }
    }

    private static void initPresetUsers() {
        String[][] adminUsers = {
            {"admin01_li", "admin090909", "ADMIN", "男"}
        };
        String[][] studentUsers = {
            {"苏苏", "edu090909", "女", "S2024001", "2024", "软件工程", "2401", "软件学院"},
            {"宁宁", "isu090909", "男", "S2024002", "2024", "软件工程", "2401", "软件学院"},
            {"玉玉", "edu090909", "女", "S2024003", "2024", "软件工程", "2401", "软件学院"}
        };
        String[][] teacherUsers = {
            {"书书", "edu090909", "女", "T2024001", "教授", "软件学院"},
            {"秋秋", "edu090909", "女", "T2024002", "副教授", "软件学院"}
        };
        for (String[] u : adminUsers) {
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                tryInsertUser(conn, u[0], u[1], "ADMIN", u[3]);
            } catch (SQLException e) { /* ignore */ }
        }
        for (String[] u : studentUsers) {
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                long id = tryInsertUser(conn, u[0], u[1], "STUDENT", u[2]);
                if (id > 0) {
                    try (PreparedStatement pstmt = conn.prepareStatement(
                             "INSERT OR IGNORE INTO students (user_id, student_id, grade, major, class_name, department) VALUES (?, ?, ?, ?, ?, ?)")) {
                        pstmt.setLong(1, id);
                        pstmt.setString(2, u[3]);
                        pstmt.setString(3, u[4]);
                        pstmt.setString(4, u[5]);
                        pstmt.setString(5, u[6]);
                        pstmt.setString(6, u[7]);
                        pstmt.executeUpdate();
                    }
                }
            } catch (SQLException e) { /* ignore */ }
        }
        for (String[] u : teacherUsers) {
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                long id = tryInsertUser(conn, u[0], u[1], "TEACHER", u[2]);
                if (id > 0) {
                    try (PreparedStatement pstmt = conn.prepareStatement(
                             "INSERT OR IGNORE INTO teachers (user_id, employee_id, title, department) VALUES (?, ?, ?, ?)")) {
                        pstmt.setLong(1, id);
                        pstmt.setString(2, u[3]);
                        pstmt.setString(3, u[4]);
                        pstmt.setString(4, u[5]);
                        pstmt.executeUpdate();
                    }
                }
            } catch (SQLException e) { /* ignore */ }
        }
    }

    private static long tryInsertUser(Connection conn, String username, String password, String role, String gender) {
        try (PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT OR IGNORE INTO users (username, password, role, gender) VALUES (?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            pstmt.setString(3, role);
            pstmt.setString(4, gender);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) { /* ignore */ }
        return -1;
    }

    public static String login(String credential, String password, String role, String loginMethod) {
        String sql;
        switch (loginMethod) {
            case "USERNAME":
                sql = "SELECT password FROM users WHERE username = ? AND role = ?";
                break;
            case "STUDENT_ID":
                sql = "SELECT u.password FROM users u JOIN students s ON u.id = s.user_id WHERE s.student_id = ? AND u.role = ?";
                break;
            case "EMPLOYEE_ID":
                sql = "SELECT u.password FROM users u JOIN teachers t ON u.id = t.user_id WHERE t.employee_id = ? AND u.role = ?";
                break;
            default:
                return null;
        }
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, credential);
            pstmt.setString(2, role);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                if (rs.getString("password").equals(hashPassword(password))) {
                    return lookupUsername(credential, role, loginMethod);
                }
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String lookupUsername(String credential, String role, String loginMethod) {
        String sql;
        switch (loginMethod) {
            case "USERNAME":
                return credential;
            case "STUDENT_ID":
                sql = "SELECT u.username FROM users u JOIN students s ON u.id = s.user_id WHERE s.student_id = ?";
                break;
            case "EMPLOYEE_ID":
                sql = "SELECT u.username FROM users u JOIN teachers t ON u.id = t.user_id WHERE t.employee_id = ?";
                break;
            default:
                return null;
        }
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, credential);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("username");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean registerStudent(String username, String password, String gender, String studentId, String grade, String major, String className, String department) {
        String sql = "INSERT INTO users (username, password, role, gender) VALUES (?, ?, 'STUDENT', ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            pstmt.setString(3, gender);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                long userId = rs.getLong(1);
                try (PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO students (user_id, student_id, grade, major, class_name, department) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setLong(1, userId);
                    ps.setString(2, studentId);
                    ps.setString(3, grade);
                    ps.setString(4, major);
                    ps.setString(5, className);
                    ps.setString(6, department);
                    ps.executeUpdate();
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean registerTeacher(String username, String password, String gender, String employeeId, String title, String department) {
        String sql = "INSERT INTO users (username, password, role, gender) VALUES (?, ?, 'TEACHER', ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            pstmt.setString(3, gender);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                long userId = rs.getLong(1);
                try (PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO teachers (user_id, employee_id, title, department) VALUES (?, ?, ?, ?)")) {
                    ps.setLong(1, userId);
                    ps.setString(2, employeeId);
                    ps.setString(3, title);
                    ps.setString(4, department);
                    ps.executeUpdate();
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    public static String resetPassword(String username, String identityId, String oldPassword, String role) {
        String sql;
        if ("STUDENT".equals(role)) {
            sql = "SELECT u.password FROM users u JOIN students s ON u.id = s.user_id WHERE u.username = ? AND s.student_id = ?";
        } else if ("TEACHER".equals(role)) {
            sql = "SELECT u.password FROM users u JOIN teachers t ON u.id = t.user_id WHERE u.username = ? AND t.employee_id = ?";
        } else {
            return null;
        }
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, identityId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                if (!rs.getString("password").equals(hashPassword(oldPassword))) {
                    return null;
                }
                String newPassword = "123456";
                try (PreparedStatement update = conn.prepareStatement(
                         "UPDATE users SET password = ? WHERE username = ?")) {
                    update.setString(1, hashPassword(newPassword));
                    update.setString(2, username);
                    update.executeUpdate();
                }
                return newPassword;
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkUsernameExists(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean updateUsername(String oldUsername, String newUsername) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET username = ? WHERE username = ?")) {
            pstmt.setString(1, newUsername);
            pstmt.setString(2, oldUsername);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean updateSignature(String username, String signature) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET signature = ? WHERE username = ?")) {
            pstmt.setString(1, signature);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean updatePassword(String username, String identityId, String oldPassword, String newPassword, String role) {
        String sql;
        if ("STUDENT".equals(role)) {
            sql = "SELECT u.password FROM users u JOIN students s ON u.id = s.user_id WHERE u.username = ? AND s.student_id = ?";
        } else if ("TEACHER".equals(role)) {
            sql = "SELECT u.password FROM users u JOIN teachers t ON u.id = t.user_id WHERE u.username = ? AND t.employee_id = ?";
        } else {
            sql = "SELECT password FROM users WHERE username = ? AND role = 'ADMIN'";
        }
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            if (!"ADMIN".equals(role)) {
                pstmt.setString(2, identityId);
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getString("password").equals(hashPassword(oldPassword))) {
                try (PreparedStatement update = conn.prepareStatement("UPDATE users SET password = ? WHERE username = ?")) {
                    update.setString(1, hashPassword(newPassword));
                    update.setString(2, username);
                    update.executeUpdate();
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getAvatarUrl(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT avatar_url FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("avatar_url");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateAvatarUrl(String username, String avatarUrl) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET avatar_url = ? WHERE username = ?")) {
            pstmt.setString(1, avatarUrl);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String[]> getAllUsers() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT u.username, u.role, s.student_id, t.employee_id FROM users u " +
                     "LEFT JOIN students s ON u.id = s.user_id " +
                     "LEFT JOIN teachers t ON u.id = t.user_id WHERE u.role != 'ADMIN'";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String username = rs.getString("username");
                String role = rs.getString("role");
                String identity = "STUDENT".equals(role) ? rs.getString("student_id") : rs.getString("employee_id");
                list.add(new String[]{username, role, identity != null ? identity : ""});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getAllDepartments() {
        List<String> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT name FROM departments ORDER BY id")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static List<String> getMajorsByDepartment(String deptName) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT m.name FROM majors m JOIN departments d ON m.department_id = d.id WHERE d.name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deptName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static boolean isValidDepartment(String name) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM departments WHERE name = ?")) {
            pstmt.setString(1, name);
            return pstmt.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public static boolean isValidMajor(String name, String deptName) {
        String sql = "SELECT m.id FROM majors m JOIN departments d ON m.department_id = d.id WHERE m.name = ? AND d.name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, deptName);
            return pstmt.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public static void onGroupFileComplete(String fileId, String fileName, long fileSize, String sender) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT OR IGNORE INTO group_files (file_id, file_name, file_size, sender) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, fileId);
            pstmt.setString(2, fileName);
            pstmt.setLong(3, fileSize);
            pstmt.setString(4, sender);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void markGroupFileDownloaded(String fileId, String username) {
        String sql = "INSERT OR REPLACE INTO group_file_downloads (file_id, username, is_downloaded, downloaded_at) " +
                     "VALUES (?, ?, 1, CURRENT_TIMESTAMP)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static boolean isGroupFileDownloaded(String fileId, String username) {
        String sql = "SELECT is_downloaded FROM group_file_downloads WHERE file_id = ? AND username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt("is_downloaded") == 1;
        } catch (SQLException e) { return false; }
    }

    public static void markP2PFileDownloaded(String fileId) {
        String sql = "UPDATE messages SET is_read = 1 WHERE content = ? AND type = 'FILE_INFO'";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
