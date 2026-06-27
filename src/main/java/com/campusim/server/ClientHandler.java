package com.campusim.server;

import com.campusim.common.Message;
import com.campusim.common.MessageType;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CampusIMServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private boolean authenticated;
    private Map<String, AvatarUploadState> avatarUploads;
    private Map<String, PendingFileStore> pendingFileStores;

    private static class AvatarUploadState {
        String tempPath;
        FileOutputStream fos;
        int receivedChunks;
        int totalChunks;
    }

    private static class PendingFileStore {
        String fileId;
        String filePath;
        long fileSize;
        String fileName;
        FileOutputStream fos;
        PendingFileStore(String fileId, String filePath, long fileSize, String fileName) {
            this.fileId = fileId;
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.fileName = fileName;
        }
    }

    public ClientHandler(Socket socket, CampusIMServer server) {
        this.socket = socket;
        this.server = server;
        this.avatarUploads = new HashMap<>();
        this.pendingFileStores = new HashMap<>();
    }

    public void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send message to " + username + ": " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Message msg = (Message) in.readObject();
                if (msg == null) break;
                handleMessage(msg);
            }
        } catch (EOFException | SocketException e) {
            System.out.println("Client disconnected: " + (username != null ? username : "unknown"));
        } catch (IOException | ClassNotFoundException e) {
            if (authenticated) {
                System.err.println("Connection error for " + username + ": " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case LOGIN_REQUEST:
                handleLogin(msg);
                break;
            case REGISTER_REQUEST:
                handleRegister(msg);
                break;
            case LOGOUT_REQUEST:
                handleLogout();
                break;
            case TEXT_P2P:
                handleP2PText(msg);
                break;
            case TEXT_GROUP:
                handleGroupText(msg);
                break;
            case IMAGE_P2P:
                handleImageP2P(msg);
                break;
            case IMAGE_GROUP:
                handleImageGroup(msg);
                break;
            case FILE_INFO:
                handleFileInfo(msg);
                break;
            case FILE_CHUNK:
                handleFileChunk(msg);
                break;
            case FILE_ACCEPT:
            case FILE_REJECT:
                handleFileResponse(msg);
                break;
            case FILE_DOWNLOAD_REQ:
                handleFileDownloadReq(msg);
                break;
            case FILE_DOWNLOAD_DONE:
                handleFileDownloadDone(msg);
                break;
            case ONLINE_USERS_REQUEST:
                handleOnlineUsersRequest();
                break;
            case USER_LIST_REQ:
                handleUserListReq();
                break;
            case HISTORY_REQUEST:
                handleHistoryRequest(msg);
                break;
            case PASSWORD_RESET_REQUEST:
                handlePasswordReset(msg);
                break;
            case PROFILE_GET_REQ:
                handleProfileGet();
                break;
            case PROFILE_UPDATE_USERNAME_REQ:
                handleProfileUpdateUsername(msg);
                break;
            case PROFILE_UPDATE_SIGNATURE_REQ:
                handleProfileUpdateSignature(msg);
                break;
            case PROFILE_UPDATE_PASSWORD_REQ:
                handleProfileUpdatePassword(msg);
                break;
            case DEPT_LIST_REQ:
                handleDeptListReq();
                break;
            case MAJOR_LIST_REQ:
                handleMajorListReq(msg);
                break;
            case AVATAR_UPLOAD_START:
                handleAvatarUploadStart(msg);
                break;
            case AVATAR_UPLOAD_CHUNK:
                handleAvatarUploadChunk(msg);
                break;
            case MARK_READ_REQ:
                handleMarkRead(msg);
                break;
            default:
                System.out.println("Unknown message type: " + msg.getType());
        }
    }

    private void handleDeptListReq() {
        List<String> depts = UserManager.getAllDepartments();
        sendMessage(Message.createDeptListResponse(depts));
    }

    private void handleMajorListReq(Message msg) {
        String deptName = msg.getContent();
        List<String> majors = UserManager.getMajorsByDepartment(deptName);
        sendMessage(Message.createMajorListResponse(majors));
    }

    private void handleLogin(Message msg) {
        String credential = msg.getSender();
        String password = msg.getContent();
        String role = msg.getRole();
        String loginMethod = msg.getLoginMethod();

        if (role == null || loginMethod == null) {
            sendMessage(Message.createLoginFail("登录信息不完整"));
            return;
        }

        if (password == null || password.length() < 8 || password.length() > 15) {
            sendMessage(Message.createLoginFail("密码长度需8~15位"));
            return;
        }
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*")) {
            sendMessage(Message.createLoginFail("密码需至少包含一位字母和一位数字"));
            return;
        }

        String actualUsername = UserManager.login(credential, password, role, loginMethod);

        if (actualUsername == null) {
            sendMessage(Message.createLoginFail("用户名/学号/工号或密码错误"));
            return;
        }

        if (OnlineUserManager.isOnline(actualUsername)) {
            sendMessage(Message.createLoginFail("用户已在线"));
            return;
        }

        this.authenticated = true;
        this.username = actualUsername;
        OnlineUserManager.addUser(actualUsername, this);
        sendMessage(Message.createLoginSuccess(actualUsername, actualUsername, role));

        server.broadcast(Message.createUserStatusNotification(actualUsername, true), null);
        System.out.println("User logged in: " + actualUsername + " (role=" + role + ")");
    }

    private void handleRegister(Message msg) {
        String username = msg.getSender();
        String password = msg.getContent();
        String role = msg.getRole();
        String gender = msg.getGender();
        String studentId = msg.getStudentId();
        String employeeId = msg.getEmployeeId();
        String grade = msg.getGrade();
        String major = msg.getMajor();
        String className = msg.getClassName();
        String title = msg.getTitle();
        String department = msg.getDepartment();

        String error = validateRegisterInput(username, password, role, gender, studentId, employeeId, grade, major, className, title, department);
        if (error != null) {
            sendMessage(Message.createRegisterResponse(false, error));
            return;
        }

        boolean success;
        if ("STUDENT".equals(role)) {
            success = UserManager.registerStudent(username, password, gender, studentId, grade, major, className, department);
        } else {
            success = UserManager.registerTeacher(username, password, gender, employeeId, title, department);
        }

        if (success) {
            sendMessage(Message.createRegisterResponse(true, "注册成功"));
            System.out.println("User registered: " + username + " (role=" + role + ")");
        } else {
            sendMessage(Message.createRegisterResponse(false, "用户名或学号/工号已存在"));
        }
    }

    private String validateRegisterInput(String username, String password, String role,
                                          String gender, String studentId, String employeeId,
                                          String grade, String major, String className,
                                          String title, String department) {
        if (username == null || username.length() < 2 || username.length() > 10)
            return "用户名长度需2~10位";
        if (password == null || password.length() < 8 || password.length() > 15)
            return "密码长度需8~15位";
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*"))
            return "密码需至少包含一位字母和一位数字";
        if (gender == null || !gender.matches("男|女"))
            return "性别只能为男或女";

        if ("STUDENT".equals(role)) {
            if (studentId == null || !studentId.matches("S\\d{7}"))
                return "学号格式不正确，需为S加7位数字";
            if (grade == null || grade.length() != 4)
                return "入学年份长度需为4位";
            if (className == null || className.length() != 4)
                return "班级长度需为4位";
            if (department == null || !UserManager.isValidDepartment(department))
                return "学院不存在";
            if (major == null || !UserManager.isValidMajor(major, department))
                return "该学院下无此专业";
        } else if ("TEACHER".equals(role)) {
            if (employeeId == null || !employeeId.matches("T\\d{7}"))
                return "工号格式不正确，需为T加7位数字";
            if (title == null || title.length() < 2 || title.length() > 10)
                return "职称长度需2~10位";
            if (department == null || !UserManager.isValidDepartment(department))
                return "学院不存在";
        } else {
            return "无效的角色";
        }
        return null;
    }

    private void handlePasswordReset(Message msg) {
        String username = msg.getSender();
        String oldPassword = msg.getContent();
        String role = msg.getRole();
        String studentId = msg.getStudentId();
        String employeeId = msg.getEmployeeId();

        String identityId;
        if ("STUDENT".equals(role)) {
            identityId = studentId;
        } else if ("TEACHER".equals(role)) {
            identityId = employeeId;
        } else {
            sendMessage(Message.createPasswordResetResponse(false, "该角色不支持密码重置"));
            return;
        }

        if (identityId == null || identityId.isEmpty()) {
            sendMessage(Message.createPasswordResetResponse(false, "缺少学号/工号信息"));
            return;
        }

        String newPassword = UserManager.resetPassword(username, identityId, oldPassword, role);
        if (newPassword != null) {
            sendMessage(Message.createPasswordResetResponse(true, "密码已重置，新密码为 " + newPassword));
            System.out.println("Password reset for: " + username);
        } else {
            sendMessage(Message.createPasswordResetResponse(false, "重置失败：信息验证不通过"));
        }
    }

    private void handleImageP2P(Message msg) {
        String fileId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        saveImageToServer(msg, fileId);
        msg.setContent(fileId);

        String target = msg.getReceiver();
        ClientHandler handler = OnlineUserManager.getHandler(target);
        msg.setSender(this.username);
        MessageStore.saveMessage(msg, handler != null);
        if (handler != null) {
            handler.sendMessage(msg);
        }
    }

    private void handleImageGroup(Message msg) {
        String fileId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        saveImageToServer(msg, fileId);
        msg.setContent(fileId);
        MessageStore.saveMessage(msg);

        msg.setSender(this.username);
        server.broadcast(msg, this.username);
    }

    private void saveImageToServer(Message msg, String fileId) {
        try {
            new File("data/server_files").mkdirs();
            String filePath = "data/server_files/" + fileId + "_" + msg.getFileName();
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(msg.getChunkData());
            }
        } catch (IOException e) {
            System.err.println("Failed to save image to server: " + e.getMessage());
        }
    }

    private void handleFileDownloadReq(Message msg) {
        String fileId = msg.getContent();
        if (fileId == null || fileId.isEmpty()) return;

        File dir = new File("data/server_files");
        File[] files = dir.listFiles((d, name) -> name.startsWith(fileId + "_"));
        if (files == null || files.length == 0) return;

        File file = files[0];
        String fileName = file.getName().substring(fileId.length() + 1);
        long fileSize = file.length();
        int totalChunks = (int) Math.ceil((double) fileSize / 65536);

        // Send FILE_INFO with download flag via content field
        Message info = new Message(MessageType.FILE_INFO);
        info.setFileName(fileName);
        info.setFileSize(fileSize);
        info.setContent("DOWNLOAD:" + fileId);
        sendMessage(info);

        // Send chunks
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int bytesRead;
            int seqNum = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                Message chunk = Message.createFileChunk("SERVER", msg.getSender(), seqNum, totalChunks, chunkData);
                chunk.setContent("DOWNLOAD:" + fileId + ":" + fileName);
                sendMessage(chunk);
                seqNum++;
            }
        } catch (IOException e) {
            System.err.println("Failed to send file: " + e.getMessage());
        }
    }

    private void handleFileDownloadDone(Message msg) {
        String fileId = msg.getContent();
        if (fileId != null && !fileId.isEmpty()) {
            UserManager.markGroupFileDownloaded(fileId, this.username);
            UserManager.markP2PFileDownloaded(fileId);
        }
    }

    private void handleLogout() {
        cleanup();
    }

    private void handleP2PText(Message msg) {
        String target = msg.getReceiver();
        ClientHandler handler = OnlineUserManager.getHandler(target);

        if (handler != null) {
            MessageStore.saveMessage(msg, true);
            handler.sendMessage(msg);
        } else {
            MessageStore.saveMessage(msg, false);
            sendMessage(Message.createLoginFail("用户 " + target + " 不在线，消息已保存"));
        }
    }

    private void handleGroupText(Message msg) {
        MessageStore.saveMessage(msg);
        server.broadcast(msg, this.username);
    }

    private void handleFileInfo(Message msg) {
        String target = msg.getReceiver();
        ClientHandler handler = OnlineUserManager.getHandler(target);

        String fileId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String filePath = "data/server_files/" + fileId + "_" + msg.getFileName();

        try {
            new File("data/server_files").mkdirs();
            FileOutputStream fos = new FileOutputStream(filePath);
            pendingFileStores.put(this.username, new PendingFileStore(fileId, filePath, msg.getFileSize(), msg.getFileName()));
        } catch (IOException e) {
            System.err.println("Failed to prepare file store: " + e.getMessage());
        }

        msg.setContent(fileId);
        msg.setSender(this.username);
        // FILE_INFO 不立即转发，等 chunks 完成后由 handleFileChunk 发送
    }

    private void handleFileChunk(Message msg) {
        // Save chunk to server storage
        PendingFileStore pfs = pendingFileStores.get(this.username);
        if (pfs != null) {
            try {
                if (pfs.fos == null) {
                    pfs.fos = new FileOutputStream(pfs.filePath, true);
                }
                pfs.fos.write(msg.getChunkData());
                pfs.fos.flush();

                // Last chunk → close stream, store in MessageStore
                if (msg.getSeqNum() + 1 >= msg.getTotalChunks()) {
                    pfs.fos.close();
                    Message storeMsg = new Message(MessageType.FILE_INFO);
                    storeMsg.setSender(this.username);
                    storeMsg.setReceiver(msg.getReceiver());
                    storeMsg.setContent(pfs.fileId);
                    storeMsg.setFileName(pfs.fileName);
                    storeMsg.setFileSize(pfs.fileSize);

                    if ("GROUP".equals(msg.getReceiver())) {
                        UserManager.onGroupFileComplete(pfs.fileId, pfs.fileName, pfs.fileSize, this.username);
                        MessageStore.saveMessage(storeMsg);
                        server.broadcast(storeMsg, this.username);
                    } else {
                        ClientHandler targetHandler = OnlineUserManager.getHandler(msg.getReceiver());
                        MessageStore.saveMessage(storeMsg, false);
                        if (targetHandler != null) {
                            targetHandler.sendMessage(storeMsg);
                        }
                    }
                    pendingFileStores.remove(this.username);
                }
            } catch (IOException e) {
                System.err.println("Failed to write file chunk: " + e.getMessage());
            }
        } else {
            // No server storage needed for this sender (legacy flow)
        }

        // Forward to receiver (existing behavior)
        String target = msg.getReceiver();
        ClientHandler handler = OnlineUserManager.getHandler(target);
        if (handler != null) {
            handler.sendMessage(msg);
        }
    }

    private void handleFileResponse(Message msg) {
        String target = msg.getReceiver();
        ClientHandler handler = OnlineUserManager.getHandler(target);

        if (handler != null) {
            handler.sendMessage(msg);
        }
    }

    private void handleOnlineUsersRequest() {
        Message msg = Message.createOnlineUsersResponse(OnlineUserManager.getOnlineUsers());
        msg.setUserIdentityMap(OnlineUserManager.getOnlineIdentities());
        sendMessage(msg);
    }

    private void handleUserListReq() {
        List<String[]> allUsers = UserManager.getAllUsers();
        List<String[]> enriched = new ArrayList<>();
        Map<String, String> fullMap = new HashMap<>();
        for (String[] u : allUsers) {
            enriched.add(new String[]{u[0], u[1], u[2], OnlineUserManager.isOnline(u[0]) ? "1" : "0"});
            fullMap.put(u[0], u[2]);
        }
        Message resp = new Message(MessageType.USER_LIST_RESP);
        resp.setUserList(enriched);
        resp.setUserIdentityMap(fullMap);
        sendMessage(resp);

        // Send unread counts for the current user
        if (username != null) {
            Map<String, Integer> unread = MessageStore.getUnreadCounts(username);
            Message unreadMsg = new Message(MessageType.UNREAD_COUNTS_RESP);
            Map<String, String> unreadStr = new HashMap<>();
            for (Map.Entry<String, Integer> e : unread.entrySet()) {
                unreadStr.put(e.getKey(), String.valueOf(e.getValue()));
            }
            unreadMsg.setUserIdentityMap(unreadStr);
            sendMessage(unreadMsg);
        }
    }

    private void handleMarkRead(Message msg) {
        String sender = msg.getContent(); // the other user's username
        MessageStore.markAsRead(username, sender);
        Message res = new Message(MessageType.MARK_READ_RES);
        res.setSender(sender);
        res.setSuccess(true);
        sendMessage(res);
    }

    private void handleHistoryRequest(Message msg) {
        String target = msg.getReceiver();
        Message response = new Message(MessageType.HISTORY_RESPONSE);

        if ("GROUP".equals(target)) {
            List<Message> history = MessageStore.getGroupHistory();
            for (Message m : history) {
                if (m.getType() == MessageType.FILE_INFO) {
                    String fileId = m.getContent();
                    boolean downloaded = UserManager.isGroupFileDownloaded(fileId, this.username);
                    m.setContent(fileId + ":" + (downloaded ? "1" : "0"));
                }
            }
            response.setHistoryMessages(history);
        } else {
            response.setHistoryMessages(MessageStore.getP2PHistory(this.username, target));
        }
        sendMessage(response);
    }

    private void handleProfileGet() {
        String dbUrl = "jdbc:sqlite:data/campusim.db";
        Message resp = new Message(MessageType.PROFILE_GET_RESP);
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT username, role, gender, signature, created_at FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                resp.setSender(rs.getString("username"));
                resp.setRole(rs.getString("role"));
                resp.setContent(rs.getString("signature"));
                resp.setTimestamp(rs.getString("created_at"));
                String gender = rs.getString("gender");
                resp.setGender(gender != null ? gender : "");

                String role = rs.getString("role");
                if ("STUDENT".equals(role)) {
                    try (PreparedStatement ps = conn.prepareStatement(
                             "SELECT student_id, grade, major, class_name, department FROM students WHERE user_id = (SELECT id FROM users WHERE username = ?)")) {
                        ps.setString(1, username);
                        ResultSet rs2 = ps.executeQuery();
                        if (rs2.next()) {
                            resp.setStudentId(rs2.getString("student_id"));
                            resp.setUserExtra(rs2.getString("grade") + "|" + rs2.getString("major") + "|" + rs2.getString("class_name") + "|" + rs2.getString("department"));
                        }
                    }
                } else if ("TEACHER".equals(role)) {
                    try (PreparedStatement ps = conn.prepareStatement(
                             "SELECT employee_id, title, department FROM teachers WHERE user_id = (SELECT id FROM users WHERE username = ?)")) {
                        ps.setString(1, username);
                        ResultSet rs2 = ps.executeQuery();
                        if (rs2.next()) {
                            resp.setEmployeeId(rs2.getString("employee_id"));
                            resp.setUserExtra(rs2.getString("title") + "|" + rs2.getString("department"));
                        }
                    }
                }

                String avatarUrl = UserManager.getAvatarUrl(username);
                resp.setFileName(avatarUrl != null ? avatarUrl : "");
                resp.setSuccess(true);
            }
        } catch (SQLException e) {
            resp.setSuccess(false);
            resp.setReason("获取个人信息失败");
        }
        sendMessage(resp);
    }

    private void handleProfileUpdateUsername(Message msg) {
        String newUsername = msg.getContent();
        if (newUsername == null || newUsername.length() < 2 || newUsername.length() > 10) {
            Message res = new Message(MessageType.PROFILE_UPDATE_USERNAME_RES);
            res.setSuccess(false);
            res.setReason("用户名长度需为2~10个字符");
            sendMessage(res);
            return;
        }
        if (UserManager.checkUsernameExists(newUsername)) {
            Message res = new Message(MessageType.PROFILE_UPDATE_USERNAME_RES);
            res.setSuccess(false);
            res.setReason("该用户名已被使用");
            sendMessage(res);
            return;
        }
        if (UserManager.updateUsername(username, newUsername)) {
            String oldUsername = username;
            username = newUsername;
            OnlineUserManager.removeUser(oldUsername);
            OnlineUserManager.addUser(username, this);
            server.broadcast(Message.createUserStatusNotification(oldUsername, false), null);
            server.broadcast(Message.createUserStatusNotification(username, true), null);
            Message res = new Message(MessageType.PROFILE_UPDATE_USERNAME_RES);
            res.setSuccess(true);
            res.setSender(newUsername);
            res.setReason("用户名修改成功");
            sendMessage(res);
            System.out.println("Username changed: " + oldUsername + " -> " + newUsername);
        } else {
            Message res = new Message(MessageType.PROFILE_UPDATE_USERNAME_RES);
            res.setSuccess(false);
            res.setReason("用户名修改失败");
            sendMessage(res);
        }
    }

    private void handleProfileUpdateSignature(Message msg) {
        String signature = msg.getContent();
        if (signature == null || signature.length() < 2 || signature.length() > 20) {
            Message res = new Message(MessageType.PROFILE_UPDATE_SIGNATURE_RES);
            res.setSuccess(false);
            res.setReason("签名长度需为2~20个字符");
            sendMessage(res);
            return;
        }
        if (UserManager.updateSignature(username, signature)) {
            Message res = new Message(MessageType.PROFILE_UPDATE_SIGNATURE_RES);
            res.setSuccess(true);
            res.setReason("签名修改成功");
            sendMessage(res);
        } else {
            Message res = new Message(MessageType.PROFILE_UPDATE_SIGNATURE_RES);
            res.setSuccess(false);
            res.setReason("签名修改失败");
            sendMessage(res);
        }
    }

    private void handleProfileUpdatePassword(Message msg) {
        String oldPassword = msg.getContent();
        String newPassword = msg.getNewPassword();
        String role = msg.getRole();
        String studentId = msg.getStudentId();
        String employeeId = msg.getEmployeeId();

        String identityId;
        if ("STUDENT".equals(role)) {
            identityId = studentId;
        } else if ("TEACHER".equals(role)) {
            identityId = employeeId;
        } else {
            identityId = "";
        }

        if (UserManager.updatePassword(username, identityId, oldPassword, newPassword, role)) {
            Message res = new Message(MessageType.PROFILE_UPDATE_PASSWORD_RES);
            res.setSuccess(true);
            res.setReason("密码修改成功");
            sendMessage(res);
            System.out.println("Password changed for: " + username);
        } else {
            Message res = new Message(MessageType.PROFILE_UPDATE_PASSWORD_RES);
            res.setSuccess(false);
            res.setReason("密码修改失败：信息验证不通过");
            sendMessage(res);
        }
    }

    private void handleAvatarUploadStart(Message msg) {
        String avatarDir = "data/avatars";
        new File(avatarDir).mkdirs();
        String identityId = getAvatarIdentity();
        String tempPath = avatarDir + "/" + identityId + "_tmp.png";
        try {
            AvatarUploadState state = new AvatarUploadState();
            state.tempPath = tempPath;
            state.fos = new FileOutputStream(tempPath);
            state.receivedChunks = 0;
            state.totalChunks = msg.getTotalChunks();
            avatarUploads.put(username, state);
        } catch (IOException e) {
            e.printStackTrace();
            Message res = new Message(MessageType.AVATAR_UPLOAD_DONE);
            res.setSuccess(false);
            res.setReason("头像上传失败: " + e.getMessage());
            sendMessage(res);
        }
    }

    private void handleAvatarUploadChunk(Message msg) {
        AvatarUploadState state = avatarUploads.get(username);
        if (state == null) return;

        try {
            state.fos.write(msg.getChunkData());
            state.receivedChunks++;

            if (state.receivedChunks >= state.totalChunks) {
                state.fos.close();

                String identityId = getAvatarIdentity();
                File oldFile = new File("data/avatars/" + identityId + ".png");
                if (oldFile.exists()) oldFile.delete();

                new File(state.tempPath).renameTo(oldFile);

                UserManager.updateAvatarUrl(username, "avatars/" + identityId + ".png");
                avatarUploads.remove(username);

                Message res = new Message(MessageType.AVATAR_UPLOAD_DONE);
                res.setSuccess(true);
                res.setReason("头像更新成功");
                sendMessage(res);
                System.out.println("Avatar updated for: " + username);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Message res = new Message(MessageType.AVATAR_UPLOAD_DONE);
            res.setSuccess(false);
            res.setReason("头像上传失败: " + e.getMessage());
            sendMessage(res);
        }
    }

    private String getAvatarIdentity() {
        String dbUrl = "jdbc:sqlite:data/campusim.db";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT u.role, s.student_id, t.employee_id FROM users u " +
                 "LEFT JOIN students s ON u.id = s.user_id " +
                 "LEFT JOIN teachers t ON u.id = t.user_id WHERE u.username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                if ("STUDENT".equals(role)) {
                    String sid = rs.getString("student_id");
                    if (sid != null) return sid;
                } else if ("TEACHER".equals(role)) {
                    String eid = rs.getString("employee_id");
                    if (eid != null) return eid;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return username;
    }

    private void cleanup() {
        if (username != null) {
            OnlineUserManager.removeUser(username);
            server.broadcast(Message.createUserStatusNotification(username, false), null);
            System.out.println("User logged out: " + username);
        }
        for (PendingFileStore pfs : pendingFileStores.values()) {
            try { if (pfs.fos != null) pfs.fos.close(); } catch (IOException e) { /* ignore */ }
        }
        pendingFileStores.clear();
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
