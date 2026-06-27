package com.campusim.common;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class EmojiManager {

    private static final Map<String, String> EMOJI_MAP = new LinkedHashMap<>();
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();
    private static final String EMOJI_DIR = "data/emojis/";
    private static final double BASELINE_ADJUST = 1.0;

    static {
        EMOJI_MAP.put("\uD83D\uDE00", "1f600.png");
        EMOJI_MAP.put("\uD83D\uDE03", "1f603.png");
        EMOJI_MAP.put("\uD83D\uDE04", "1f604.png");
        EMOJI_MAP.put("\uD83D\uDE01", "1f601.png");
        EMOJI_MAP.put("\uD83D\uDE06", "1f606.png");
        EMOJI_MAP.put("\uD83D\uDE02", "1f602.png");
        EMOJI_MAP.put("\uD83E\uDD23", "1f923.png");
        EMOJI_MAP.put("\uD83D\uDE0A", "1f60a.png");

        EMOJI_MAP.put("\uD83D\uDE0D", "1f60d.png");
        EMOJI_MAP.put("\uD83E\uDD70", "1f970.png");
        EMOJI_MAP.put("\uD83D\uDE18", "1f618.png");
        EMOJI_MAP.put("\uD83D\uDE0B", "1f60b.png");
        EMOJI_MAP.put("\uD83D\uDE0E", "1f60e.png");
        EMOJI_MAP.put("\uD83E\uDD29", "1f929.png");
        EMOJI_MAP.put("\uD83E\uDD73", "1f973.png");
        EMOJI_MAP.put("\uD83D\uDE0F", "1f60f.png");

        EMOJI_MAP.put("\uD83D\uDC4D", "1f44d.png");
        EMOJI_MAP.put("\uD83D\uDC4E", "1f44e.png");
        EMOJI_MAP.put("\uD83D\uDC4A", "1f44a.png");
        EMOJI_MAP.put("\u270A", "270a.png");
        EMOJI_MAP.put("\uD83E\uDD1D", "1f91d.png");
        EMOJI_MAP.put("\uD83D\uDC4F", "1f44f.png");
        EMOJI_MAP.put("\uD83D\uDE4C", "1f64c.png");
        EMOJI_MAP.put("\uD83D\uDCAA", "1f4aa.png");

        EMOJI_MAP.put("\uD83D\uDC9B", "1f49b.png");
        EMOJI_MAP.put("\uD83D\uDC9A", "1f49a.png");
        EMOJI_MAP.put("\uD83D\uDC99", "1f499.png");
        EMOJI_MAP.put("\uD83D\uDC9C", "1f49c.png");
        EMOJI_MAP.put("\uD83D\uDD25", "1f525.png");
        EMOJI_MAP.put("\u2728", "2728.png");
        EMOJI_MAP.put("\uD83C\uDF89", "1f389.png");
        EMOJI_MAP.put("\uD83C\uDF8A", "1f38a.png");

        EMOJI_MAP.put("\u2B50", "2b50.png");
        EMOJI_MAP.put("\u2705", "2705.png");
        EMOJI_MAP.put("\u274C", "274c.png");
        EMOJI_MAP.put("\u2753", "2753.png");
        EMOJI_MAP.put("\u2757", "2757.png");
        EMOJI_MAP.put("\uD83C\uDF82", "1f382.png");
        EMOJI_MAP.put("\u2615", "2615.png");
        EMOJI_MAP.put("\uD83C\uDF38", "1f338.png");
    }

    public static String getEmojiDir() {
        return EMOJI_DIR;
    }

    public static Image getImage(String emoji) {
        String fn = EMOJI_MAP.get(emoji);
        if (fn == null) return null;
        return IMAGE_CACHE.computeIfAbsent(fn, k -> {
            File f = new File(EMOJI_DIR + fn);
            if (!f.exists()) return null;
            try (FileInputStream fis = new FileInputStream(f)) {
                return new Image(fis);
            } catch (IOException e) {
                return null;
            }
        });
    }

    public static boolean isEmoji(String ch) {
        return EMOJI_MAP.containsKey(ch);
    }

    public static String[] getAllEmojis() {
        return EMOJI_MAP.keySet().toArray(new String[0]);
    }

    public static void renderTo(TextFlow flow, String text, double fontSize) {
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));

            if (isEmoji(ch)) {
                if (buf.length() > 0) {
                    Text tn = new Text(buf.toString());
                    tn.setStyle("-fx-font-size: " + fontSize + ";");
                    flow.getChildren().add(tn);
                    buf.setLength(0);
                }
                Image img = getImage(ch);
                if (img != null) {
                    ImageView iv = new ImageView(img);
                    iv.setFitHeight(fontSize);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    iv.setTranslateY(BASELINE_ADJUST);
                    flow.getChildren().add(iv);
                } else {
                    Text tn = new Text(ch);
                    tn.setStyle("-fx-font-size: " + fontSize + ";");
                    flow.getChildren().add(tn);
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
            tn.setStyle("-fx-font-size: " + fontSize + ";");
            flow.getChildren().add(tn);
        }
    }
}
