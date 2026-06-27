package com.campusim.client.ui;

import com.campusim.common.EmojiManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.TextField;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class EmojiInputField extends StackPane {

    private static final double FONT_SIZE = 13.0;
    private static final double CARET_HEIGHT = 14.0;
    private static final double CARET_Y = 10.0;

    private final TextField textField;
    private final Pane clipPane;
    private final HBox contentLayer;
    private final Line caret;
    private final Timeline caretBlink;

    public EmojiInputField() {
        setPadding(new Insets(0));
        setMinWidth(0);
        setStyle("-fx-background-color: white;");

        contentLayer = new HBox(0);
        contentLayer.setAlignment(Pos.CENTER_LEFT);
        contentLayer.setPadding(new Insets(10, 0, 0, 5));
        contentLayer.setPrefWidth(0);
        contentLayer.setMinWidth(0);

        caret = new Line(0, CARET_Y, 0, CARET_Y + CARET_HEIGHT);
        caret.setManaged(false);
        caret.setStrokeWidth(1.5);
        caret.setVisible(false);

        caretBlink = new Timeline(
            new KeyFrame(Duration.seconds(0.5), e -> caret.setVisible(!caret.isVisible()))
        );
        caretBlink.setCycleCount(Animation.INDEFINITE);

        clipPane = new Pane(contentLayer);
        clipPane.setMouseTransparent(true);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clipPane.setClip(clip);

        textField = new TextField();
        textField.setStyle("-fx-text-fill: white; -fx-background-color: transparent; -fx-border-color: transparent; -fx-highlight-fill: transparent; -fx-highlight-text-fill: transparent; -fx-caret-color: black; -fx-padding: 10 0 0 5;");
        textField.setPrefHeight(35);
        textField.setPrefWidth(0);

        textField.textProperty().addListener((obs, ov, nv) -> {
            contentLayer.getChildren().clear();
            if (nv != null && !nv.isEmpty()) {
                renderText(nv);
            }
            contentLayer.getChildren().add(caret);
            updateCaret();
            if (textField.isFocused()) {
                caret.setVisible(true);
                caretBlink.playFromStart();
            }
            scrollToEnd();
        });

        textField.caretPositionProperty().addListener((obs, ov, nv) -> updateCaret());

        textField.focusedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                caret.setVisible(true);
                caretBlink.playFromStart();
            } else {
                caret.setVisible(false);
                caretBlink.stop();
            }
        });

        setOnMouseClicked(e -> {
            textField.requestFocus();
            caret.setVisible(true);
            caretBlink.playFromStart();
        });

        widthProperty().addListener((obs, ov, nv) -> scrollToEnd());

        getChildren().addAll(textField, clipPane);
    }

    private void scrollToEnd() {
        Platform.runLater(() -> {
            double contentWidth = computeContentWidth();
            double visibleWidth = getWidth();
            if (visibleWidth <= 0) return;
            if (contentWidth > visibleWidth) {
                contentLayer.setTranslateX(visibleWidth - contentWidth);
            } else {
                contentLayer.setTranslateX(0);
            }
        });
    }

    private double computeContentWidth() {
        double w = contentLayer.getPadding().getLeft();
        for (Node child : contentLayer.getChildrenUnmodifiable()) {
            if (child == caret) continue;
            if (child instanceof Text) {
                w += ((Text) child).getLayoutBounds().getWidth();
            } else if (child instanceof ImageView) {
                ImageView iv = (ImageView) child;
                Image img = iv.getImage();
                if (img != null && img.getWidth() > 0 && !img.isError()) {
                    w += iv.getFitHeight() * img.getWidth() / img.getHeight();
                } else {
                    w += iv.getFitHeight();
                }
            }
        }
        return w;
    }

    private void updateCaret() {
        double x = computeCaretX();
        caret.setLayoutX(x);
    }

    private double computeCaretX() {
        String text = textField.getText();
        double x = contentLayer.getPadding().getLeft();
        if (text == null || text.isEmpty()) return x;

        int caretPos = textField.getCaretPosition();
        if (caretPos <= 0) return x;

        int end = Math.min(caretPos, text.length());
        int i = 0;
        while (i < end) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            int charLen = Character.charCount(cp);

            if (EmojiManager.isEmoji(ch)) {
                Image img = EmojiManager.getImage(ch);
                if (img != null && img.getWidth() > 0 && !img.isError()) {
                    x += FONT_SIZE * img.getWidth() / img.getHeight();
                } else {
                    x += FONT_SIZE;
                }
            } else {
                Text tmp = new Text(ch);
                tmp.setFont(Font.font(FONT_SIZE));
                x += tmp.getLayoutBounds().getWidth();
            }
            i += charLen;
        }
        return x;
    }

    private void renderText(String text) {
        if (text == null || text.isEmpty()) return;

        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));

            if (EmojiManager.isEmoji(ch)) {
                if (buf.length() > 0) {
                    Text tn = new Text(buf.toString());
                    tn.setFont(Font.font(FONT_SIZE));
                    contentLayer.getChildren().add(tn);
                    buf.setLength(0);
                }
                Image img = EmojiManager.getImage(ch);
                if (img != null) {
                    ImageView iv = new ImageView(img);
                    iv.setFitHeight(FONT_SIZE);
                    iv.setPreserveRatio(true);
                    iv.setTranslateY(1.0);
                    contentLayer.getChildren().add(iv);
                } else {
                    Text tn = new Text(ch);
                    tn.setFont(Font.font(FONT_SIZE));
                    contentLayer.getChildren().add(tn);
                }
                i += Character.charCount(cp);
            } else {
                int len = Character.charCount(cp);
                buf.append(text, i, i + len);
                i += len;
            }
        }
        if (buf.length() > 0) {
            Text tn = new Text(buf.toString());
            tn.setFont(Font.font(FONT_SIZE));
            contentLayer.getChildren().add(tn);
        }
    }

    public String getText() {
        return textField.getText();
    }

    public void clear() {
        textField.clear();
    }

    @Override
    public void requestFocus() {
        textField.requestFocus();
        caret.setVisible(true);
        caretBlink.playFromStart();
    }

    public void insertEmoji(String unicode) {
        String newText = textField.getText() + unicode;
        textField.setText(newText);
        textField.positionCaret(newText.length());
        textField.requestFocus();
        caret.setVisible(true);
        caretBlink.playFromStart();
        Platform.runLater(() -> caret.setLayoutX(computeContentWidth()));
    }

    public void setOnSendCallback(Runnable action) {
        textField.setOnAction(e -> action.run());
    }
}
