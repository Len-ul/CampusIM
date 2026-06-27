package com.campusim.client.ui;

import com.campusim.common.EmojiManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.File;

public class MessageBubble extends HBox {
    private final String bubbleSender;
    private Label fileStatusLabel;
    private String fileId;

    public enum BubbleType { TEXT, IMAGE, FILE }

    public MessageBubble(String sender, String content, String timestamp, boolean isSelf, boolean isGroup) {
        this.bubbleSender = sender;
        setPadding(new Insets(3, 8, 3, 8));
        setAlignment(Pos.TOP_LEFT);
        buildTextBubble(sender, content, timestamp, isSelf, isGroup);
    }

    public MessageBubble(String sender, String imagePath, String timestamp, boolean isSelf, boolean isGroup, BubbleType type) {
        this.bubbleSender = sender;
        setPadding(new Insets(3, 8, 3, 8));
        setAlignment(Pos.TOP_LEFT);

        if (type == BubbleType.IMAGE) {
            buildImageBubble(sender, imagePath, timestamp, isSelf, isGroup);
        } else {
            buildTextBubble(sender, "", timestamp, isSelf, isGroup);
        }
    }

    public MessageBubble(String sender, String fileId, String fileName, long fileSize, String timestamp,
                         boolean isSelf, boolean isGroup, boolean isDownloaded, Runnable onDownload) {
        this.bubbleSender = sender;
        this.fileId = fileId;
        setPadding(new Insets(3, 8, 3, 8));
        setAlignment(Pos.TOP_LEFT);
        buildFileBubble(sender, fileId, fileName, fileSize, timestamp, isSelf, isGroup, isDownloaded, onDownload);
    }

    public void setDownloaded(boolean downloaded) {
        if (fileStatusLabel != null) {
            String statusText = downloaded ? "[已下载]" : "[未下载]";
            fileStatusLabel.setText(fileStatusLabel.getText().replaceAll("\\[.*?\\]", "    " + statusText));
        }
    }

    public String getFileId() {
        return fileId;
    }

    private void buildTextBubble(String sender, String content, String timestamp, boolean isSelf, boolean isGroup) {
        if (isGroup && !isSelf) {
            buildGroupMessage(sender, content, timestamp);
        } else if (isSelf) {
            buildSelfMessage(sender, content, timestamp);
        } else {
            buildP2PMessage(sender, content, timestamp);
        }
    }

    private void buildP2PMessage(String sender, String content, String timestamp) {
        AvatarView avatar = new AvatarView(sender, 30);
        avatar.loadAvatar(sender);

        VBox bubble = new VBox(2);
        Node text = createBubbleContent(content, "#ecf0f1", "#2c3e50");
        Label time = new Label(formatTime(timestamp));
        time.setStyle("-fx-font-size: 10; -fx-text-fill: #95a5a6;");
        bubble.getChildren().addAll(text, time);
        getChildren().addAll(avatar, spacer(6), bubble);
    }

    private void buildSelfMessage(String sender, String content, String timestamp) {
        setAlignment(Pos.CENTER_RIGHT);
        VBox bubble = new VBox(2);
        bubble.setAlignment(Pos.CENTER_RIGHT);
        Node text = createBubbleContent(content, "#3498db", "white");
        Label time = new Label(formatTime(timestamp));
        time.setStyle("-fx-font-size: 10; -fx-text-fill: #95a5a6;");
        bubble.getChildren().addAll(text, time);
        AvatarView avatar = new AvatarView(sender, 30);
        avatar.loadAvatar(sender);
        getChildren().addAll(bubble, spacer(6), avatar);
    }

    private void buildGroupMessage(String sender, String content, String timestamp) {
        AvatarView avatar = new AvatarView(sender, 30);
        avatar.loadAvatar(sender);
        VBox right = new VBox(2);
        HBox header = new HBox(6);
        Label name = new Label(sender);
        name.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
        Label time = new Label(formatTime(timestamp));
        time.setStyle("-fx-font-size: 10; -fx-text-fill: #95a5a6;");
        header.getChildren().addAll(name, time);
        Node text = createBubbleContent(content, "#ecf0f1", "#2c3e50");
        right.getChildren().addAll(header, text);
        getChildren().addAll(avatar, spacer(6), right);
    }

    private void buildImageBubble(String sender, String imagePath, String timestamp, boolean isSelf, boolean isGroup) {
        Image img = new Image(new File(imagePath).toURI().toString(), 200, 200, true, true);
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setFitWidth(200);
        iv.setStyle("-fx-background-radius: 6;");

        Label time = new Label(formatTime(timestamp));
        time.setStyle("-fx-font-size: 10; -fx-text-fill: #95a5a6;");

        if (isGroup && !isSelf) {
            AvatarView avatar = new AvatarView(sender, 30);
            avatar.loadAvatar(sender);
            VBox right = new VBox(2);
            HBox header = new HBox(6);
            Label name = new Label(sender);
            name.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
            header.getChildren().addAll(name, time);
            right.getChildren().addAll(header, iv);
            getChildren().addAll(avatar, spacer(6), right);
        } else if (isSelf) {
            setAlignment(Pos.CENTER_RIGHT);
            VBox bubble = new VBox(2);
            bubble.setAlignment(Pos.CENTER_RIGHT);
            bubble.getChildren().addAll(iv, time);
            AvatarView avatar = new AvatarView(sender, 30);
            avatar.loadAvatar(sender);
            getChildren().addAll(bubble, spacer(6), avatar);
        } else {
            AvatarView avatar = new AvatarView(sender, 30);
            avatar.loadAvatar(sender);
            VBox bubble = new VBox(2);
            bubble.getChildren().addAll(iv, time);
            getChildren().addAll(avatar, spacer(6), bubble);
        }
    }

    private void buildFileBubble(String sender, String fileId, String fileName, long fileSize, String timestamp,
                                 boolean isSelf, boolean isGroup, boolean isDownloaded, Runnable onDownload) {
        VBox content = new VBox(4);
        content.setPadding(new Insets(8, 12, 8, 12));
        String bg = isSelf ? "#3498db" : "#ecf0f1";
        content.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 6;");
        content.setMaxWidth(300);

        Label fileNameLabel = new Label("📄 " + fileName);
        fileNameLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: " + (isSelf ? "white" : "#2c3e50") + "; -fx-cursor: hand;");
        fileNameLabel.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                if (isDownloaded) {
                    try {
                        new ProcessBuilder("explorer", "/select,", getLocalFilePath(fileName)).start();
                    } catch (Exception ex) { /* ignore */ }
                } else if (onDownload != null) {
                    onDownload.run();
                }
            }
        });

        String sizeStr = formatFileSize(fileSize);
        String statusText = isDownloaded ? "[已下载]" : "[未下载]";
        if (isSelf) {
            Label sizeLabel = new Label(sizeStr);
            sizeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #d5e8f7;");
            content.getChildren().addAll(fileNameLabel, sizeLabel);
        } else {
            fileStatusLabel = new Label(sizeStr + "    " + statusText);
            fileStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7f8c8d;");
            content.getChildren().addAll(fileNameLabel, fileStatusLabel);
        }

        Label time = new Label(formatTime(timestamp));
        time.setStyle("-fx-font-size: 10; -fx-text-fill: #95a5a6;");

        if (isGroup && !isSelf) {
            AvatarView avatar = new AvatarView(sender, 30);
            avatar.loadAvatar(sender);
            VBox right = new VBox(2);
            HBox header = new HBox(6);
            Label name = new Label(sender);
            name.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
            header.getChildren().addAll(name, time);
            right.getChildren().addAll(header, content);
            getChildren().addAll(avatar, spacer(6), right);
        } else if (isSelf) {
            setAlignment(Pos.CENTER_RIGHT);
            VBox bubble = new VBox(2);
            bubble.setAlignment(Pos.CENTER_RIGHT);
            bubble.getChildren().addAll(content, time);
            AvatarView avatar = new AvatarView(sender, 30);
            avatar.loadAvatar(sender);
            getChildren().addAll(bubble, spacer(6), avatar);
        } else {
            AvatarView avatar = new AvatarView(sender, 30);
            avatar.loadAvatar(sender);
            VBox bubble = new VBox(2);
            bubble.getChildren().addAll(content, time);
            getChildren().addAll(avatar, spacer(6), bubble);
        }
    }

    private String getLocalFilePath(String fileName) {
        return "data/downloads/" + fileName;
    }

    public void refreshAvatar() {
        findAndReloadAvatar(this);
    }

    private void findAndReloadAvatar(javafx.scene.Parent parent) {
        for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof AvatarView) {
                ((AvatarView) child).loadAvatar(bubbleSender);
            } else if (child instanceof javafx.scene.Parent) {
                findAndReloadAvatar((javafx.scene.Parent) child);
            }
        }
    }

    private Region spacer(int w) {
        Region r = new Region();
        r.setPrefWidth(w);
        return r;
    }

    private String formatTime(String timestamp) {
        if (timestamp == null || timestamp.length() < 16) return timestamp != null ? timestamp : "";
        return timestamp.substring(11, 16);
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private Node createBubbleContent(String content, String bgColor, String textColor) {
        TextFlow flow = new TextFlow();
        flow.setMaxWidth(260);
        flow.setPadding(new Insets(6, 10, 6, 10));
        flow.setStyle("-fx-font-size: 13; -fx-background-color: " + bgColor + "; -fx-background-radius: 6;");
        EmojiManager.renderTo(flow, content, 13);
        Color fill = Color.web(textColor);
        for (Node child : flow.getChildren()) {
            if (child instanceof Text) {
                ((Text) child).setFill(fill);
            }
        }
        return flow;
    }
}
