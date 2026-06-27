package com.campusim.administrator.ui;

import com.campusim.administrator.AdminManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class AdminLoginView {
    private final Stage stage;
    private final AdminLoginCallback callback;
    private TextField accountField;
    private PasswordField passwordField;
    private Label statusLabel;

    public interface AdminLoginCallback {
        void onLoginSuccess(String account, String name);
    }

    public AdminLoginView(Stage stage, AdminLoginCallback callback) {
        this.stage = stage;
        this.callback = callback;
    }

    public Scene createScene() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #f5f5f5;");

        Label title = new Label("校园即时通讯 - 管理员后台");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2c3e50;");

        Label subTitle = new Label("管理员登录");
        subTitle.setFont(Font.font("Microsoft YaHei", 14));
        subTitle.setStyle("-fx-text-fill: #7f8c8d;");

        VBox formBox = new VBox(8);
        formBox.setMaxWidth(340);
        formBox.setPadding(new Insets(20));
        formBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label accountLabel = new Label("管理员账号");
        accountLabel.setFont(Font.font("Microsoft YaHei", 13));
        accountField = new TextField();
        accountField.setPromptText("请输入管理员账号");
        accountField.setPrefHeight(32);

        Label passwordLabel = new Label("密码");
        passwordLabel.setFont(Font.font("Microsoft YaHei", 13));
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setPrefHeight(32);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(8, 0, 3, 0));

        Button loginBtn = new Button("登  录");
        loginBtn.setPrefSize(110, 35);
        loginBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14; -fx-background-radius: 4;");
        loginBtn.setOnAction(e -> handleLogin());

        buttonBox.getChildren().addAll(loginBtn);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        formBox.getChildren().addAll(accountLabel, accountField,
                passwordLabel, passwordField,
                buttonBox, statusLabel);

        root.getChildren().addAll(title, subTitle, formBox);

        passwordField.setOnAction(e -> handleLogin());

        return new Scene(root, 420, 500);
    }

    private void handleLogin() {
        String account = accountField.getText().trim();
        String password = passwordField.getText().trim();

        if (account.isEmpty() || password.isEmpty()) {
            statusLabel.setText("请填写完整信息");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");
        statusLabel.setText("正在登录...");

        new Thread(() -> {
            String name = AdminManager.adminLogin(account, password);
            Platform.runLater(() -> {
                if (name != null) {
                    callback.onLoginSuccess(account, name);
                } else {
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
                    statusLabel.setText("账号或密码错误");
                }
            });
        }).start();
    }
}
