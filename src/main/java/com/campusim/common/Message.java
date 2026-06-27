package com.campusim.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String sender;
    private String receiver;
    private String content;
    private boolean success;
    private String reason;
    private List<String> onlineUsers;
    private List<Message> historyMessages;
    private String fileName;
    private long fileSize;
    private int seqNum;
    private int totalChunks;
    private byte[] chunkData;
    private String timestamp;

    private String role;
    private String gender;
    private String studentId;
    private String employeeId;
    private String loginMethod;
    private String displayName;
    private String newPassword;
    private String userExtra;
    private Map<String, String> userIdentityMap;
    private List<String[]> userList;

    private String grade;
    private String major;
    private String className;
    private String title;
    private String department;

    public Message() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Message(MessageType type) {
        this();
        this.type = type;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<String> getOnlineUsers() { return onlineUsers; }
    public void setOnlineUsers(List<String> onlineUsers) { this.onlineUsers = onlineUsers; }
    public List<Message> getHistoryMessages() { return historyMessages; }
    public void setHistoryMessages(List<Message> historyMessages) { this.historyMessages = historyMessages; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public int getSeqNum() { return seqNum; }
    public void setSeqNum(int seqNum) { this.seqNum = seqNum; }
    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    public byte[] getChunkData() { return chunkData; }
    public void setChunkData(byte[] chunkData) { this.chunkData = chunkData; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getLoginMethod() { return loginMethod; }
    public void setLoginMethod(String loginMethod) { this.loginMethod = loginMethod; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getUserExtra() { return userExtra; }
    public void setUserExtra(String userExtra) { this.userExtra = userExtra; }
    public Map<String, String> getUserIdentityMap() { return userIdentityMap; }
    public void setUserIdentityMap(Map<String, String> userIdentityMap) { this.userIdentityMap = userIdentityMap; }
    public List<String[]> getUserList() { return userList; }
    public void setUserList(List<String[]> userList) { this.userList = userList; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public static Message createLoginRequest(String credential, String password, String role, String loginMethod) {
        Message msg = new Message(MessageType.LOGIN_REQUEST);
        msg.setSender(credential);
        msg.setContent(password);
        msg.setRole(role);
        msg.setLoginMethod(loginMethod);
        return msg;
    }

    public static Message createLoginSuccess(String username, String displayName, String role) {
        Message msg = new Message(MessageType.LOGIN_SUCCESS);
        msg.setSender(username);
        msg.setDisplayName(displayName);
        msg.setRole(role);
        msg.setSuccess(true);
        msg.setReason("登录成功");
        return msg;
    }

    public static Message createLoginFail(String reason) {
        Message msg = new Message(MessageType.LOGIN_FAIL);
        msg.setSuccess(false);
        msg.setReason(reason);
        return msg;
    }

    public static Message createRegisterRequest(String username, String password, String role,
                                                 String gender, String studentId, String employeeId,
                                                 String grade, String major, String className,
                                                 String title, String department) {
        Message msg = new Message(MessageType.REGISTER_REQUEST);
        msg.setSender(username);
        msg.setContent(password);
        msg.setRole(role);
        msg.setGender(gender);
        msg.setStudentId(studentId);
        msg.setEmployeeId(employeeId);
        msg.setGrade(grade);
        msg.setMajor(major);
        msg.setClassName(className);
        msg.setTitle(title);
        msg.setDepartment(department);
        return msg;
    }

    public static Message createRegisterResponse(boolean success, String reason) {
        Message msg = new Message(success ? MessageType.REGISTER_SUCCESS : MessageType.REGISTER_FAIL);
        msg.setSuccess(success);
        msg.setReason(reason);
        return msg;
    }

    public static Message createPasswordResetRequest(String username, String studentId, String employeeId, String oldPassword) {
        Message msg = new Message(MessageType.PASSWORD_RESET_REQUEST);
        msg.setSender(username);
        msg.setStudentId(studentId);
        msg.setEmployeeId(employeeId);
        msg.setContent(oldPassword);
        return msg;
    }

    public static Message createPasswordResetResponse(boolean success, String reason) {
        Message msg = new Message(success ? MessageType.PASSWORD_RESET_SUCCESS : MessageType.PASSWORD_RESET_FAIL);
        msg.setSuccess(success);
        msg.setReason(reason);
        return msg;
    }

    public static Message createTextP2P(String from, String to, String content) {
        Message msg = new Message(MessageType.TEXT_P2P);
        msg.setSender(from);
        msg.setReceiver(to);
        msg.setContent(content);
        return msg;
    }

    public static Message createTextGroup(String from, String content) {
        Message msg = new Message(MessageType.TEXT_GROUP);
        msg.setSender(from);
        msg.setReceiver("GROUP");
        msg.setContent(content);
        return msg;
    }

    public static Message createFileInfo(String from, String to, String fileName, long fileSize) {
        Message msg = new Message(MessageType.FILE_INFO);
        msg.setSender(from);
        msg.setReceiver(to);
        msg.setFileName(fileName);
        msg.setFileSize(fileSize);
        return msg;
    }

    public static Message createFileChunk(String from, String to, int seqNum, int totalChunks, byte[] data) {
        Message msg = new Message(MessageType.FILE_CHUNK);
        msg.setSender(from);
        msg.setReceiver(to);
        msg.setSeqNum(seqNum);
        msg.setTotalChunks(totalChunks);
        msg.setChunkData(data);
        return msg;
    }

    public static Message createOnlineUsersResponse(List<String> users) {
        Message msg = new Message(MessageType.ONLINE_USERS_RESPONSE);
        msg.setOnlineUsers(users);
        return msg;
    }

    public static Message createImageP2P(String from, String to, String fileName, long fileSize, byte[] imageData) {
        Message msg = new Message(MessageType.IMAGE_P2P);
        msg.setSender(from);
        msg.setReceiver(to);
        msg.setFileName(fileName);
        msg.setFileSize(fileSize);
        msg.setChunkData(imageData);
        return msg;
    }

    public static Message createImageGroup(String from, String fileName, long fileSize, byte[] imageData) {
        Message msg = new Message(MessageType.IMAGE_GROUP);
        msg.setSender(from);
        msg.setReceiver("GROUP");
        msg.setFileName(fileName);
        msg.setFileSize(fileSize);
        msg.setChunkData(imageData);
        return msg;
    }

    public static Message createUserStatusNotification(String username, boolean online) {
        Message msg = new Message(online ? MessageType.USER_ONLINE : MessageType.USER_OFFLINE);
        msg.setSender(username);
        return msg;
    }

    public static Message createDeptListResponse(List<String> departments) {
        Message msg = new Message(MessageType.DEPT_LIST_RESP);
        msg.setOnlineUsers(departments);
        return msg;
    }

    public static Message createMajorListResponse(List<String> majors) {
        Message msg = new Message(MessageType.MAJOR_LIST_RESP);
        msg.setOnlineUsers(majors);
        return msg;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + content;
    }
}
