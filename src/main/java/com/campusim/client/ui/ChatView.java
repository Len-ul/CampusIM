package com.campusim.client.ui;

import com.campusim.common.EmojiManager;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Popup;

import java.io.File;
import java.util.function.Consumer;

public class ChatView {
    private final VBox messageContainer;
    private final ScrollPane scrollPane;
    private final EmojiInputField inputField;
    private final Button sendBtn;
    private final Button fileBtn;
    private final Label statusLabel;
    private final ProgressBar progressBar;
    private final BorderPane root;
    private Consumer<String> onSendMessage;
    private Consumer<File> onSendFile;
    private int savedCaret;

    public ChatView() {
        this(true);
    }

    public ChatView(boolean showFileBtn) {
        messageContainer = new VBox(2);
        messageContainer.setPadding(new Insets(5));

        scrollPane = new ScrollPane(messageContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-border-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        messageContainer.heightProperty().addListener((obs, old, nv) -> scrollPane.setVvalue(1.0));

        inputField = new EmojiInputField();

        ImageView emojiIcon = new ImageView(new Image(new File(EmojiManager.getEmojiDir() + "1f600.png").toURI().toString()));
        emojiIcon.setFitHeight(20);
        emojiIcon.setPreserveRatio(true);
        Button emojiBtn = new Button("", emojiIcon);
        emojiBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
        emojiBtn.setOnAction(e -> showEmojiPicker(emojiBtn));

        sendBtn = new Button("发送");
        sendBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4;");

        fileBtn = new Button("文件");
        fileBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 4;");

        progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7f8c8d;");

        HBox bottomBox = new HBox(5);
        bottomBox.setPadding(new Insets(5));
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        VBox inputArea = new VBox(3);
        HBox inputRow = new HBox(5);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        inputRow.getChildren().addAll(inputField, emojiBtn, sendBtn);
        if (showFileBtn) {
            inputRow.getChildren().add(fileBtn);
        }

        inputArea.getChildren().addAll(inputRow, statusLabel);

        root = new BorderPane();
        root.setCenter(scrollPane);
        root.setBottom(inputArea);

        sendBtn.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty() && onSendMessage != null) {
                onSendMessage.accept(text);
                inputField.clear();
            }
        });

        inputField.setOnSendCallback(() -> sendBtn.fire());

        fileBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择要发送的文件");
            File file = fileChooser.showOpenDialog(null);
            if (file != null && onSendFile != null) {
                onSendFile.accept(file);
            }
        });
    }

    private void showEmojiPicker(Button owner) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-background-radius: 6; -fx-border-radius: 6; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);");

        int col = 0, row = 0;
        for (String e : EmojiManager.getAllEmojis()) {
            Image img = EmojiManager.getImage(e);
            ImageView iv = img != null ? new ImageView(img) : null;
            if (iv != null) {
                iv.setFitHeight(24);
                iv.setPreserveRatio(true);
            }
            Button btn = iv != null ? new Button("", iv) : new Button(e);
            btn.setStyle("-fx-font-size: 20; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
            btn.setOnAction(ev -> {
                inputField.insertEmoji(e);
                popup.hide();
            });
            grid.add(btn, col, row);
            col++;
            if (col >= 8) { col = 0; row++; }
        }

        popup.getContent().add(grid);
        Bounds b = owner.localToScreen(owner.getBoundsInLocal());
        popup.show(owner, b.getMinX(), b.getMinY() - 220);
    }

    public BorderPane getView() {
        return root;
    }

    public void appendMessage(String sender, String content, String timestamp,
                              boolean isSelf, boolean isGroup) {
        MessageBubble bubble = new MessageBubble(sender, content, timestamp, isSelf, isGroup);
        Platform.runLater(() -> messageContainer.getChildren().add(bubble));
    }

    public void appendImageMessage(String sender, String imagePath, String timestamp,
                                   boolean isSelf, boolean isGroup) {
        MessageBubble bubble = new MessageBubble(sender, imagePath, timestamp, isSelf, isGroup, MessageBubble.BubbleType.IMAGE);
        Platform.runLater(() -> messageContainer.getChildren().add(bubble));
    }

    public void appendFileMessage(String sender, String fileId, String fileName, long fileSize, String timestamp,
                                  boolean isSelf, boolean isGroup, boolean isDownloaded, Runnable onDownload) {
        MessageBubble bubble = new MessageBubble(sender, fileId, fileName, fileSize, timestamp,
                isSelf, isGroup, isDownloaded, onDownload);
        Platform.runLater(() -> messageContainer.getChildren().add(bubble));
    }

    public void markFileDownloaded(String fileId) {
        Platform.runLater(() -> {
            for (javafx.scene.Node child : messageContainer.getChildren()) {
                if (child instanceof MessageBubble) {
                    MessageBubble bubble = (MessageBubble) child;
                    if (fileId.equals(bubble.getFileId())) {
                        bubble.setDownloaded(true);
                        break;
                    }
                }
            }
        });
    }

    public void appendSystemMessage(String text) {
        Label sys = new Label(text);
        sys.setStyle("-fx-font-size: 11; -fx-text-fill: #95a5a6; -fx-font-style: italic;");
        sys.setPadding(new Insets(4, 0, 4, 0));
        sys.setMaxWidth(Double.MAX_VALUE);
        sys.setAlignment(Pos.CENTER);
        Platform.runLater(() -> messageContainer.getChildren().add(sys));
    }

    public void clearMessages() {
        messageContainer.getChildren().clear();
    }

    public void refreshAvatars() {
        Platform.runLater(() -> {
            for (Node child : messageContainer.getChildren()) {
                if (child instanceof MessageBubble) {
                    ((MessageBubble) child).refreshAvatar();
                }
            }
        });
    }

    public void setOnSendMessage(Consumer<String> callback) {
        this.onSendMessage = callback;
    }

    public void setOnSendFile(Consumer<File> callback) {
        this.onSendFile = callback;
    }

    public void showProgress(boolean show) {
        progressBar.setVisible(show);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void setProgress(double value) {
        progressBar.setProgress(value);
    }
}
