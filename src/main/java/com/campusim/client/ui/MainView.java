package com.campusim.client.ui;

import com.campusim.client.network.ServerConnector;
import com.campusim.common.Message;
import com.campusim.common.MessageType;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

import javafx.scene.image.Image;

public class MainView {
    private static final int CHUNK_SIZE = 65536;
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "gif", "bmp", "webp"));

    private final Stage stage;
    private final ServerConnector connector;
    private final String username;
    private final String role;
    private ListView<String> onlineListView;
    private TabPane chatTabPane;
    private Tab publicChatTab;
    private ChatView publicChatView;
    private Map<String, Tab> p2pTabs;
    private Map<String, ChatView> p2pChatViews;
    private Label statusLabel;
    private Label onlineCountLabel;
    private ProfileView profileView;

    private Map<String, FileReceiveState> fileReceives;
    private List<String[]> userDataList = new ArrayList<>();
    private Map<String, Integer> unreadCount = new HashMap<>();
    private Map<String, String> downloadedFiles; // fileId → local path

    private static class FileReceiveState {
        String fileName;
        long fileSize;
        int totalChunks;
        int receivedChunks;
        FileOutputStream fos;
        String tempFilePath;
        ChatView chatView;
    }

    public MainView(Stage stage, ServerConnector connector, String username, String role) {
        this.stage = stage;
        this.connector = connector;
        this.username = username;
        this.role = role;
        this.p2pTabs = new HashMap<>();
        this.p2pChatViews = new HashMap<>();
        this.fileReceives = new HashMap<>();
        this.downloadedFiles = new HashMap<>();

        connector.setMessageListener(new ServerConnector.MessageListener() {
            @Override
            public void onMessageReceived(Message msg) {
                Platform.runLater(() -> handleServerMessage(msg));
            }

            @Override
            public void onConnectionError(String error) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, error + "\n程序将退出。");
                    alert.showAndWait();
                    System.exit(1);
                });
            }
        });
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();

        VBox leftPanel = new VBox(5);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(220);
        leftPanel.setStyle("-fx-background-color: #2c3e50;");

        Label onlineTitle = new Label("在线用户");
        onlineTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));
        onlineTitle.setStyle("-fx-text-fill: white;");

        onlineCountLabel = new Label("共 0 人在线");
        onlineCountLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12;");

        Button refreshBtn = new Button("刷新");
        refreshBtn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-border-color: #7f8c8d; -fx-border-radius: 3; -fx-background-radius: 3;");
        refreshBtn.setOnAction(e -> requestUserList());

        onlineListView = new ListView<>();
        onlineListView.setStyle("-fx-background-color: #34495e; -fx-control-inner-background: #34495e; -fx-text-fill: white;");
        onlineListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    AvatarView avatar = new AvatarView(item, 28);
                    avatar.loadAvatar(item);
                    Label name = new Label(item);
                    name.setStyle("-fx-text-fill: white; -fx-font-size: 13;");

                    HBox dotBox = new HBox(3);
                    dotBox.setAlignment(Pos.CENTER_LEFT);

                    boolean isOnline = false;
                    String roleLabel = "";
                    for (String[] u : userDataList) {
                        if (u[0].equals(item)) {
                            isOnline = "1".equals(u[3]);
                            roleLabel = u[1];
                            break;
                        }
                    }

                    Circle greenDot = new Circle(4);
                    greenDot.setFill(isOnline ? Color.web("#2ecc71") : Color.TRANSPARENT);

                    Integer unread = unreadCount.get(item);
                    Circle redDot = new Circle(4);
                    redDot.setFill((unread != null && unread > 0) ? Color.web("#e74c3c") : Color.TRANSPARENT);

                    dotBox.getChildren().addAll(greenDot, redDot);

                    HBox cell = new HBox(6, avatar, name, dotBox);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(cell);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 3 5 3 5;");
                    if (item.equals(username)) {
                        name.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 13; -fx-font-weight: bold;");
                    }
                }
            }
        });

        onlineListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                String selected = onlineListView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.equals(username)) {
                    unreadCount.remove(selected);
                    openP2PChat(selected);
                    onlineListView.refresh();
                }
            }
        });

        leftPanel.getChildren().addAll(onlineTitle, onlineCountLabel, refreshBtn, onlineListView);
        VBox.setVgrow(onlineListView, Priority.ALWAYS);

        chatTabPane = new TabPane();
        chatTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        publicChatView = new ChatView(true);
        publicChatTab = new Tab("公共聊天室", publicChatView.getView());
        publicChatTab.setClosable(false);
        publicChatView.setOnSendMessage(text -> {
            connector.sendMessage(Message.createTextGroup(username, text));
            publicChatView.appendMessage(username, text, java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), true, true);
        });
        publicChatView.setOnSendFile(file -> {
            sendFile("GROUP", file, publicChatView);
        });
        chatTabPane.getTabs().add(publicChatTab);

        Message groupHistoryReq = new Message(MessageType.HISTORY_REQUEST);
        groupHistoryReq.setSender(username);
        groupHistoryReq.setReceiver("GROUP");
        connector.sendMessage(groupHistoryReq);

        HBox bottomBar = new HBox(5);
        bottomBar.setPadding(new Insets(5, 10, 5, 10));
        bottomBar.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0;");
        statusLabel = new Label("已登录: " + username);
        statusLabel.setStyle("-fx-font-size: 12;");
        Hyperlink profileLink = new Hyperlink("个人中心");
        profileLink.setStyle("-fx-font-size: 12;");
        profileLink.setOnMouseClicked(e -> openProfile());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bottomBar.getChildren().addAll(statusLabel, spacer, profileLink);

        root.setLeft(leftPanel);
        root.setCenter(chatTabPane);
        root.setBottom(bottomBar);

        requestUserList();

        Scene scene = new Scene(root, 900, 600);
        stage.setMinWidth(700);
        stage.setMinHeight(450);
        return scene;
    }

    private void openP2PChat(String targetUser) {
        if (p2pTabs.containsKey(targetUser)) {
            chatTabPane.getSelectionModel().select(p2pTabs.get(targetUser));
            return;
        }

        ChatView chatView = new ChatView();
        BorderPane chatPane = chatView.getView();
        String tabTitle = targetUser;

        Tab tab = new Tab(tabTitle, chatPane);
        tab.setOnClosed(e -> {
            p2pTabs.remove(targetUser);
            p2pChatViews.remove(targetUser);
        });

        chatView.setOnSendMessage(text -> {
            connector.sendMessage(Message.createTextP2P(username, targetUser, text));
            chatView.appendMessage(username, text, java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), true, false);
        });

        chatView.setOnSendFile(file -> {
            sendFile(targetUser, file, chatView);
        });

        p2pTabs.put(targetUser, tab);
        p2pChatViews.put(targetUser, chatView);
        chatTabPane.getTabs().add(tab);
        chatTabPane.getSelectionModel().select(tab);

        Message historyReq = new Message(MessageType.HISTORY_REQUEST);
        historyReq.setSender(username);
        historyReq.setReceiver(targetUser);
        connector.sendMessage(historyReq);

        Message markReq = new Message(MessageType.MARK_READ_REQ);
        markReq.setContent(targetUser);
        connector.sendMessage(markReq);
    }

    private void sendFile(String targetUser, File file, ChatView chatView) {
        if (!file.exists()) {
            chatView.setStatus("文件不存在");
            return;
        }

        long fileSize = file.length();
        String fileName = file.getName();
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";

        if (IMAGE_EXTENSIONS.contains(ext) && fileSize <= 5 * 1024 * 1024) {
            chatView.setStatus("正在发送图片...");
            new Thread(() -> {
                try {
                    byte[] imageData = new byte[(int) fileSize];
                    try (FileInputStream fis = new FileInputStream(file)) {
                        fis.read(imageData);
                    }

                    Message imgMsg;
                    if ("GROUP".equals(targetUser)) {
                        imgMsg = Message.createImageGroup(username, fileName, fileSize, imageData);
                    } else {
                        imgMsg = Message.createImageP2P(username, targetUser, fileName, fileSize, imageData);
                    }
                    connector.sendMessage(imgMsg);

                    // Save local copy for display
                    new File("data/chat_images/" + username).mkdirs();
                    String localPath = "data/chat_images/" + username + "/" + fileName;
                    try (FileOutputStream fos = new FileOutputStream(localPath)) {
                        fos.write(imageData);
                    }

                    Platform.runLater(() -> {
                        chatView.appendImageMessage(username, localPath,
                                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                true, "GROUP".equals(targetUser));
                        chatView.setStatus("图片发送完成");
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> chatView.setStatus("图片发送失败: " + e.getMessage()));
                }
            }, "ImageSender-" + fileName).start();
        } else {
            // File (non-image or large) — use server-stored chunked transfer
            chatView.setStatus("正在发送...");
            int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

            Message infoMsg = Message.createFileInfo(username, targetUser, fileName, fileSize);
            connector.sendMessage(infoMsg);

            new Thread(() -> {
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[CHUNK_SIZE];
                    int bytesRead;
                    int seqNum = 0;

                    while ((bytesRead = fis.read(buffer)) != -1) {
                        byte[] chunkData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, chunkData, 0, bytesRead);

                        Message chunkMsg = Message.createFileChunk(username, targetUser, seqNum, totalChunks, chunkData);
                        connector.sendMessage(chunkMsg);
                        seqNum++;

                        double progress = (double) seqNum / totalChunks;
                        Platform.runLater(() -> {
                            chatView.setProgress(progress);
                            chatView.setStatus(String.format("发送中... %d%%", (int)(progress * 100)));
                        });

                        Thread.sleep(10);
                    }

                    Platform.runLater(() -> {
                        chatView.appendFileMessage(username, "", fileName, fileSize,
                                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                true, "GROUP".equals(targetUser), true, null);
                        chatView.setStatus("文件发送完成");
                        chatView.showProgress(false);
                    });
                } catch (IOException | InterruptedException e) {
                    Platform.runLater(() -> {
                        chatView.setStatus("文件发送失败: " + e.getMessage());
                        chatView.showProgress(false);
                    });
                }
            }, "FileSender-" + fileName).start();
        }
    }

    private void handleServerMessage(Message msg) {
        switch (msg.getType()) {
            case USER_LIST_RESP:
                if (msg.getUserIdentityMap() != null) {
                    AvatarView.updateIdentityCache(msg.getUserIdentityMap());
                }
                if (msg.getUserList() != null) {
                    updateUserList(msg.getUserList());
                }
                publicChatView.refreshAvatars();
                for (ChatView cv : p2pChatViews.values()) {
                    cv.refreshAvatars();
                }
                break;
            case TEXT_P2P:
                handleIncomingP2PMessage(msg);
                break;
            case TEXT_GROUP:
                handleIncomingGroupMessage(msg);
                break;
            case IMAGE_P2P:
                handleIncomingImageP2P(msg);
                break;
            case IMAGE_GROUP:
                handleIncomingImageGroup(msg);
                break;
            case USER_ONLINE:
                requestUserList();
                statusLabel.setText("用户 " + msg.getSender() + " 上线了");
                break;
            case USER_OFFLINE:
                requestUserList();
                statusLabel.setText("用户 " + msg.getSender() + " 下线了");
                break;
            case FILE_INFO:
                handleIncomingFileInfo(msg);
                break;
            case FILE_CHUNK:
                handleIncomingFileChunk(msg);
                break;
            case FILE_ACCEPT:
                statusLabel.setText("文件请求被接受");
                break;
            case FILE_REJECT:
                statusLabel.setText("文件请求被拒绝");
                break;
            case FILE_COMPLETE:
                handleFileComplete(msg);
                break;
            case HISTORY_RESPONSE:
                if (msg.getHistoryMessages() != null) {
                    displayHistory(msg.getHistoryMessages());
                }
                break;
            case PROFILE_GET_RESP:
                if (profileView != null) {
                    profileView.handleProfileResponse(msg);
                }
                break;
            case PROFILE_UPDATE_USERNAME_RES:
                if (msg.isSuccess()) {
                    stage.setTitle("校园即时通讯系统 - " + msg.getSender());
                    statusLabel.setText("用户名已修改为: " + msg.getSender());
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, msg.getReason());
                    alert.showAndWait();
                }
                break;
            case PROFILE_UPDATE_SIGNATURE_RES:
            case PROFILE_UPDATE_PASSWORD_RES:
                if (profileView != null) {
                    Alert alert = new Alert(msg.isSuccess() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR, msg.getReason());
                    alert.initOwner(stage);
                    alert.showAndWait();
                }
                break;
            case AVATAR_UPLOAD_DONE:
                if (profileView != null) {
                    Alert alert = new Alert(msg.isSuccess() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR, msg.getReason());
                    alert.initOwner(stage);
                    alert.showAndWait();
                }
                break;
            case UNREAD_COUNTS_RESP:
                if (msg.getUserIdentityMap() != null) {
                    for (Map.Entry<String, String> e : msg.getUserIdentityMap().entrySet()) {
                        try {
                            int count = Integer.parseInt(e.getValue());
                            unreadCount.put(e.getKey(), count);
                        } catch (NumberFormatException ex) { /* ignore */ }
                    }
                    onlineListView.refresh();
                }
                break;
            case MARK_READ_RES:
                if (msg.isSuccess()) {
                    unreadCount.remove(msg.getSender());
                    onlineListView.refresh();
                }
                break;
        }
    }

    private void openProfile() {
        profileView = new ProfileView(stage, connector, username);
        profileView.show();
        profileView.getStage().setOnHidden(e -> profileView = null);
    }

    private void updateUserList(List<String[]> users) {
        userDataList = users;
        List<String[]> sorted = new ArrayList<>();
        List<String[]> onlineStudents = new ArrayList<>();
        List<String[]> onlineTeachers = new ArrayList<>();
        List<String[]> offlineStudents = new ArrayList<>();
        List<String[]> offlineTeachers = new ArrayList<>();

        for (String[] u : users) {
            boolean online = "1".equals(u[3]);
            if ("STUDENT".equals(u[1])) {
                if (online) onlineStudents.add(u);
                else offlineStudents.add(u);
            } else {
                if (online) onlineTeachers.add(u);
                else offlineTeachers.add(u);
            }
        }

        onlineStudents.sort((a, b) -> a[2].compareTo(b[2]));
        onlineTeachers.sort((a, b) -> a[2].compareTo(b[2]));
        offlineStudents.sort((a, b) -> a[2].compareTo(b[2]));
        offlineTeachers.sort((a, b) -> a[2].compareTo(b[2]));

        sorted.addAll(onlineStudents);
        sorted.addAll(onlineTeachers);
        sorted.addAll(offlineStudents);
        sorted.addAll(offlineTeachers);

        List<String> displayNames = new ArrayList<>();
        for (String[] u : sorted) {
            displayNames.add(u[0]);
        }

        onlineListView.getItems().setAll(displayNames);
        int onlineCount = onlineStudents.size() + onlineTeachers.size();
        onlineCountLabel.setText("共 " + users.size() + " 人（在线 " + onlineCount + "）");
    }

    private void requestUserList() {
        connector.sendMessage(new Message(MessageType.USER_LIST_REQ));
    }

    private boolean isCurrentChat(String username) {
        Tab selected = chatTabPane.getSelectionModel().getSelectedItem();
        return username != null && selected == p2pTabs.get(username);
    }

    private void handleIncomingP2PMessage(Message msg) {
        String from = msg.getSender();
        if (!isCurrentChat(from)) {
            unreadCount.put(from, unreadCount.getOrDefault(from, 0) + 1);
        }

        ChatView chatView = p2pChatViews.get(from);
        if (chatView != null) {
            chatView.appendMessage(from, msg.getContent(), msg.getTimestamp(), false, false);
        }

        onlineListView.refresh();
    }

    private void handleIncomingGroupMessage(Message msg) {
        publicChatView.appendMessage(msg.getSender(), msg.getContent(), msg.getTimestamp(), false, true);
    }

    private void handleIncomingImageP2P(Message msg) {
        String from = msg.getSender();
        if (!isCurrentChat(from)) {
            unreadCount.put(from, unreadCount.getOrDefault(from, 0) + 1);
        }

        saveIncomingImage(from, msg.getFileName(), msg.getChunkData());
        String localPath = "data/chat_images/" + from + "/" + msg.getFileName();

        ChatView chatView = p2pChatViews.get(from);
        if (chatView != null) {
            chatView.appendImageMessage(from, localPath, msg.getTimestamp(), false, false);
        }
        onlineListView.refresh();
    }

    private void handleIncomingImageGroup(Message msg) {
        saveIncomingImage(msg.getSender(), msg.getFileName(), msg.getChunkData());
        String localPath = "data/chat_images/" + msg.getSender() + "/" + msg.getFileName();
        publicChatView.appendImageMessage(msg.getSender(), localPath, msg.getTimestamp(), false, true);
    }

    private void saveIncomingImage(String sender, String fileName, byte[] data) {
        try {
            new File("data/chat_images/" + sender).mkdirs();
            try (FileOutputStream fos = new FileOutputStream("data/chat_images/" + sender + "/" + fileName)) {
                fos.write(data);
            }
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }

    private void handleIncomingFileInfo(Message msg) {
        String from = msg.getSender();
        String fileName = msg.getFileName();
        long fileSize = msg.getFileSize();
        String content = msg.getContent();

        // Handle download response
        if (content != null && content.startsWith("DOWNLOAD:")) {
            String[] parts = content.split(":", 2);
            if (parts.length == 2) {
                String fileId = parts[1];
                FileReceiveState state = fileReceives.get("DOWNLOAD_" + fileId);
                if (state != null) {
                    state.totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);
                    fileReceives.put("DOWNLOAD_" + fileId, state);
                }
            }
            return;
        }

        // Normal incoming file
        String fileId = content;
        boolean alreadyDownloaded = false;

        // Parse download status from history (format: "fileId:isDownloaded")
        if (fileId != null && fileId.contains(":")) {
            String[] parts = fileId.split(":", 2);
            fileId = parts[0];
            alreadyDownloaded = parts.length > 1 && "1".equals(parts[1]);
        }

        // Check in-memory cache as fallback
        if (!alreadyDownloaded && fileId != null) {
            alreadyDownloaded = downloadedFiles.containsKey(fileId);
        }

        boolean isGroup = "GROUP".equals(msg.getReceiver());
        ChatView chatView = isGroup ? publicChatView : p2pChatViews.get(from);
        final ChatView cv = chatView;
        final String fId = fileId;
        final String fFrom = from;
        final boolean fIsGroup = isGroup;

        if (!isGroup && fId != null && !isCurrentChat(fFrom)) {
            unreadCount.put(fFrom, unreadCount.getOrDefault(fFrom, 0) + 1);
        }

        if (cv != null) {
            cv.appendFileMessage(fFrom, fId, fileName, fileSize, msg.getTimestamp(),
                    false, fIsGroup, alreadyDownloaded,
                    () -> downloadFile(fId, fileName, fileSize, fFrom, fIsGroup, cv));
        }
        onlineListView.refresh();
    }

    private void handleIncomingFileChunk(Message msg) {
        String from = msg.getSender();
        String content = msg.getContent();

        // Check if this is a download chunk
        if (content != null && content.startsWith("DOWNLOAD:")) {
            String[] parts = content.split(":");
            if (parts.length >= 3) {
                String fileId = parts[1];
                String fileName = parts[2];
                FileReceiveState state = fileReceives.get("DOWNLOAD_" + fileId);
                if (state != null) {
                    try {
                        if (state.fos == null) {
                            File saveFile = new File("data/downloads/" + fileName);
                            new File("data/downloads").mkdirs();
                            state.fos = new FileOutputStream(saveFile);
                            state.tempFilePath = saveFile.getAbsolutePath();
                        }
                        state.fos.write(msg.getChunkData());
                        state.receivedChunks++;
                        double progress = (double) state.receivedChunks / msg.getTotalChunks();
                        statusLabel.setText(String.format("下载 %s... %d%%", fileName, (int)(progress * 100)));

                        if (state.receivedChunks >= msg.getTotalChunks()) {
                            state.fos.close();
                            fileReceives.remove("DOWNLOAD_" + fileId);
                            downloadedFiles.put(fileId, state.tempFilePath);
                            if (state.chatView != null) {
                                state.chatView.markFileDownloaded(fileId);
                            }
                            // Notify server
                            Message doneMsg = new Message(MessageType.FILE_DOWNLOAD_DONE);
                            doneMsg.setContent(fileId);
                            connector.sendMessage(doneMsg);
                            statusLabel.setText("下载完成: " + fileName);
                        }
                    } catch (IOException e) {
                        statusLabel.setText("下载失败: " + e.getMessage());
                    }
                }
            }
            return;
        }

        // Normal P2P file chunk (existing flow)
        FileReceiveState state = fileReceives.get(from);
        if (state == null) return;

        try {
            state.fos.write(msg.getChunkData());
            state.receivedChunks++;

            double progress = (double) state.receivedChunks / msg.getTotalChunks();
            statusLabel.setText(String.format("接收 %s... %d%%", state.fileName, (int)(progress * 100)));

            if (state.receivedChunks >= msg.getTotalChunks()) {
                state.fos.close();
                fileReceives.remove(from);
                statusLabel.setText("文件 " + state.fileName + " 接收完成");
                ChatView chatView = p2pChatViews.get(from);
                if (chatView != null) {
                    chatView.appendSystemMessage("已接收文件: " + state.fileName);
                }
            }
        } catch (IOException e) {
            statusLabel.setText("文件接收失败: " + e.getMessage());
        }
    }

    private void handleFileComplete(Message msg) {
        ChatView chatView = p2pChatViews.get(msg.getSender());
        if (chatView != null) {
            chatView.appendFileMessage(msg.getSender(), msg.getContent(), msg.getFileName(), msg.getFileSize(),
                    msg.getTimestamp(), true, false, true, null);
            chatView.setStatus("文件发送完成");
            chatView.showProgress(false);
        }
    }

    private void downloadFile(String fileId, String fileName, long fileSize, String from, boolean isGroup, ChatView chatView) {
        if (fileId == null || fileId.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存文件");
        fileChooser.setInitialFileName(fileName);
        File saveFile = fileChooser.showSaveDialog(stage);
        if (saveFile == null) return;

        statusLabel.setText("正在下载 " + fileName + "...");

        // Send download request to server
        Message req = new Message(MessageType.FILE_DOWNLOAD_REQ);
        req.setContent(fileId);
        connector.sendMessage(req);

        // Prepare to receive chunks
        FileReceiveState state = new FileReceiveState();
        state.fileName = fileName;
        state.fileSize = fileSize;
        state.receivedChunks = 0;
        state.tempFilePath = saveFile.getAbsolutePath();
        state.chatView = chatView;
        try {
            state.fos = new FileOutputStream(saveFile);
        } catch (IOException e) {
            statusLabel.setText("下载失败: " + e.getMessage());
            return;
        }
        fileReceives.put("DOWNLOAD_" + fileId, state);
    }

    private void displayHistory(List<Message> history) {
        if (history.isEmpty()) return;

        String targetUser = null;
        for (Message msg : history) {
            boolean isSelf = msg.getSender().equals(username);

            if (msg.getType() == MessageType.TEXT_P2P || msg.getType() == MessageType.IMAGE_P2P) {
                String other = isSelf ? msg.getReceiver() : msg.getSender();
                if (targetUser == null) targetUser = other;
                ChatView chatView = p2pChatViews.get(other);
                if (chatView != null) {
                    if (msg.getType() == MessageType.IMAGE_P2P) {
                        String fileId = msg.getContent();
                        String fileName = msg.getFileName();
                        String localPath = "data/chat_images/" + msg.getSender() + "/" + fileName;
                        if (new File(localPath).exists()) {
                            chatView.appendImageMessage(msg.getSender(), localPath, msg.getTimestamp(), isSelf, false);
                        } else {
                            chatView.appendFileMessage(msg.getSender(), fileId, fileName, msg.getFileSize(),
                                    msg.getTimestamp(), isSelf, false, downloadedFiles.containsKey(fileId),
                                    () -> downloadFile(fileId, fileName, msg.getFileSize(), other, false, chatView));
                        }
                    } else {
                        chatView.appendMessage(msg.getSender(), msg.getContent(), msg.getTimestamp(), isSelf, false);
                    }
                }
            } else if (msg.getType() == MessageType.TEXT_GROUP || msg.getType() == MessageType.IMAGE_GROUP) {
                if (msg.getType() == MessageType.IMAGE_GROUP) {
                    String fileName = msg.getFileName();
                    String localPath = "data/chat_images/" + msg.getSender() + "/" + fileName;
                    if (new File(localPath).exists()) {
                        publicChatView.appendImageMessage(msg.getSender(), localPath, msg.getTimestamp(), isSelf, true);
                    } else {
                        String fileId = msg.getContent();
                        publicChatView.appendFileMessage(msg.getSender(), fileId, fileName, msg.getFileSize(),
                                msg.getTimestamp(), isSelf, true, downloadedFiles.containsKey(fileId),
                                () -> downloadFile(fileId, fileName, msg.getFileSize(), "GROUP", true, publicChatView));
                    }
                } else {
                    publicChatView.appendMessage(msg.getSender(), msg.getContent(), msg.getTimestamp(), isSelf, true);
                }
            } else if (msg.getType() == MessageType.FILE_INFO) {
                String receiver = msg.getReceiver();
                String rawContent = msg.getContent();
                String parsedFileId = rawContent;
                boolean isDownloaded = false;

                if (rawContent != null && rawContent.contains(":")) {
                    String[] parts = rawContent.split(":", 2);
                    parsedFileId = parts[0];
                    isDownloaded = parts.length > 1 && "1".equals(parts[1]);
                }
                if (!isDownloaded && parsedFileId != null) {
                    isDownloaded = downloadedFiles.containsKey(parsedFileId);
                }

                final String fileId = parsedFileId;
                final boolean downloaded = isDownloaded;

                if ("GROUP".equals(receiver)) {
                    publicChatView.appendFileMessage(msg.getSender(), fileId, msg.getFileName(), msg.getFileSize(),
                            msg.getTimestamp(), isSelf, true, downloaded,
                            () -> downloadFile(fileId, msg.getFileName(), msg.getFileSize(), "GROUP", true, publicChatView));
                } else {
                    String other = isSelf ? receiver : msg.getSender();
                    if (targetUser == null) targetUser = other;
                    ChatView cView = p2pChatViews.get(other);
                    if (cView != null) {
                        final ChatView finalCv = cView;
                        cView.appendFileMessage(msg.getSender(), fileId, msg.getFileName(), msg.getFileSize(),
                                msg.getTimestamp(), isSelf, false, downloaded,
                                () -> downloadFile(fileId, msg.getFileName(), msg.getFileSize(), other, false, finalCv));
                    }
                }
            }
        }

        if (targetUser != null) {
            ChatView chatView = p2pChatViews.get(targetUser);
            if (chatView != null) {
                chatView.appendSystemMessage("--- 以上为历史消息 ---");
            }
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}
