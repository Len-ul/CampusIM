package com.campusim.client.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AvatarView extends StackPane {
    private static final String[] COLORS = {
        "#3498db", "#e74c3c", "#2ecc71", "#e67e22", "#9b59b6",
        "#1abc9c", "#f39c12", "#34495e", "#16a085", "#c0392b"
    };

    private static final Map<String, String> identityCache = new HashMap<>();

    private final Circle bg;
    private final Text letter;
    private final ImageView imageView;
    private final int size;

    public AvatarView(String username, int size) {
        this.size = size;
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        bg = new Circle(size / 2.0);
        int idx = Math.abs(username.hashCode()) % COLORS.length;
        bg.setFill(Color.web(COLORS[idx]));

        letter = new Text(String.valueOf(username.charAt(0)));
        letter.setFill(Color.WHITE);
        letter.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, size * 0.4));

        imageView = new ImageView();
        imageView.setFitWidth(size - 4);
        imageView.setFitHeight(size - 4);
        imageView.setPreserveRatio(true);
        Circle clip = new Circle(size / 2.0 - 2, size / 2.0 - 2, size / 2.0 - 2);
        imageView.setClip(clip);
        imageView.setVisible(false);

        setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
        getChildren().addAll(bg, letter, imageView);
    }

    public AvatarView(String username) {
        this(username, 36);
    }

    public static void updateIdentityCache(Map<String, String> map) {
        identityCache.clear();
        if (map != null) {
            identityCache.putAll(map);
        }
    }

    public void loadAvatar(String username) {
        String identity = identityCache.get(username);
        if (identity == null) return;
        File f = new File("data/avatars/" + identity + ".png");
        if (f.exists()) {
            setImage(new Image(f.toURI().toString()));
        }
    }

    public void setImage(Image img) {
        if (img != null) {
            imageView.setImage(img);
            imageView.setVisible(true);
            letter.setVisible(false);
        } else {
            imageView.setImage(null);
            imageView.setVisible(false);
            letter.setVisible(true);
        }
    }
}
