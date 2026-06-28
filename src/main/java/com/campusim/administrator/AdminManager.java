package com.campusim.administrator;

import com.campusim.administrator.model.Student;
import com.campusim.administrator.model.Teacher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminManager {
    private static final String DB_URL = "jdbc:sqlite:data/campusim.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static String adminLogin(String account, String password) {
        String sql = "SELECT password FROM users WHERE username = ? AND role = 'ADMIN'";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                if (rs.getString("password").equals(hashPassword(password))) {
                    return account;
                }
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.gender, u.signature, u.created_at, " +
                     "s.student_id, s.grade, s.major, s.class_name, s.department " +
                     "FROM users u JOIN students s ON u.id = s.user_id WHERE u.role = 'STUDENT'";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Student s = new Student();
                s.setId(rs.getInt("id"));
                s.setUsername(rs.getString("username"));
                s.setGender(rs.getString("gender"));
                s.setSignature(rs.getString("signature"));
                s.setCreatedAt(rs.getString("created_at"));
                s.setStudentId(rs.getString("student_id"));
                s.setGrade(rs.getString("grade"));
                s.setMajor(rs.getString("major"));
                s.setClassName(rs.getString("class_name"));
                s.setDepartment(rs.getString("department"));
                s.setRole("STUDENT");
                list.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<Teacher> getAllTeachers() {
        List<Teacher> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.gender, u.signature, u.created_at, " +
                     "t.employee_id, t.title, t.department " +
                     "FROM users u JOIN teachers t ON u.id = t.user_id WHERE u.role = 'TEACHER'";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Teacher t = new Teacher();
                t.setId(rs.getInt("id"));
                t.setUsername(rs.getString("username"));
                t.setGender(rs.getString("gender"));
                t.setSignature(rs.getString("signature"));
                t.setCreatedAt(rs.getString("created_at"));
                t.setEmployeeId(rs.getString("employee_id"));
                t.setTitle(rs.getString("title"));
                t.setDepartment(rs.getString("department"));
                t.setRole("TEACHER");
                list.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean addStudent(String username, String password, String studentId,
                                     String gender, String grade,
                                     String major, String className, String department) {
        String insertUserSql = "INSERT INTO users (username, password, role, gender) VALUES (?, ?, 'STUDENT', ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
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
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addTeacher(String username, String password, String employeeId,
                                     String gender, String title, String department) {
        String insertUserSql = "INSERT INTO users (username, password, role, gender) VALUES (?, ?, 'TEACHER', ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
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
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateStudent(int userId, String gender, String password,
                                        String studentId, String grade, String major, String className, String department) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement userPs;
                if (password != null && !password.isEmpty()) {
                    userPs = conn.prepareStatement("UPDATE users SET gender = ?, password = ? WHERE id = ?");
                    userPs.setString(1, gender);
                    userPs.setString(2, hashPassword(password));
                    userPs.setInt(3, userId);
                } else {
                    userPs = conn.prepareStatement("UPDATE users SET gender = ? WHERE id = ?");
                    userPs.setString(1, gender);
                    userPs.setInt(2, userId);
                }
                userPs.executeUpdate();
                try (PreparedStatement stuPs = conn.prepareStatement(
                         "UPDATE students SET student_id = ?, grade = ?, major = ?, class_name = ?, department = ? WHERE user_id = ?")) {
                    stuPs.setString(1, studentId);
                    stuPs.setString(2, grade);
                    stuPs.setString(3, major);
                    stuPs.setString(4, className);
                    stuPs.setString(5, department);
                    stuPs.setInt(6, userId);
                    stuPs.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateTeacher(int userId, String gender, String password,
                                        String employeeId, String title, String department) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement userPs;
                if (password != null && !password.isEmpty()) {
                    userPs = conn.prepareStatement("UPDATE users SET gender = ?, password = ? WHERE id = ?");
                    userPs.setString(1, gender);
                    userPs.setString(2, hashPassword(password));
                    userPs.setInt(3, userId);
                } else {
                    userPs = conn.prepareStatement("UPDATE users SET gender = ? WHERE id = ?");
                    userPs.setString(1, gender);
                    userPs.setInt(2, userId);
                }
                userPs.executeUpdate();
                try (PreparedStatement teaPs = conn.prepareStatement(
                         "UPDATE teachers SET employee_id = ?, title = ?, department = ? WHERE user_id = ?")) {
                    teaPs.setString(1, employeeId);
                    teaPs.setString(2, title);
                    teaPs.setString(3, department);
                    teaPs.setInt(4, userId);
                    teaPs.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                int rows = pstmt.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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

    public static List<String> getAllMajors() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT name FROM majors ORDER BY id";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
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
}
