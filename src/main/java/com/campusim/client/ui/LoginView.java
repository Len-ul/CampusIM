package com.campusim.client.ui;

import com.campusim.client.network.ServerConnector;
import com.campusim.common.Message;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class LoginView {
    private final Stage stage;
    private final ServerConnector connector;
    private final LoginCallback callback;
    private ToggleButton studentBtn;
    private ToggleButton teacherBtn;
    private VBox formBox;
    private HBox radioBox;
    private HBox buttonBox;
    private Label passwordLabel;
    private TextField usernameField;
    private TextField identityField;
    private PasswordField passwordField;
    private Label usernameLabel;
    private Label identityLabel;
    private Label statusLabel;
    private ToggleGroup loginMethodGroup;
    private RadioButton usernameRadio;
    private RadioButton idRadio;
    private Button loginBtn;
    private Button registerBtn;
    private String currentRole = "STUDENT";
    private Hyperlink forgotPwdLink;

    public interface LoginCallback {
        void onLoginSuccess(String username, String role);
    }

    public LoginView(Stage stage, ServerConnector connector, LoginCallback callback) {
        this.stage = stage;
        this.connector = connector;
        this.callback = callback;
        connector.setMessageListener(new ServerConnector.MessageListener() {
            @Override
            public void onMessageReceived(Message msg) {
                Platform.runLater(() -> handleServerMessage(msg));
            }

            @Override
            public void onConnectionError(String error) {
                Platform.runLater(() -> statusLabel.setText(error));
            }
        });
    }

    public Scene createScene() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #f5f5f5;");

        Label title = new Label("校园即时通讯系统");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: #2c3e50;");

        HBox roleBox = new HBox(15);
        roleBox.setAlignment(Pos.CENTER);
        roleBox.setPadding(new Insets(10, 0, 5, 0));

        ToggleGroup roleGroup = new ToggleGroup();
        studentBtn = new ToggleButton("学生登录");
        studentBtn.setToggleGroup(roleGroup);
        studentBtn.setSelected(true);
        studentBtn.setPrefSize(140, 40);
        studentBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14; -fx-background-radius: 4; -fx-border-color: #2980b9; -fx-border-width: 2;");

        teacherBtn = new ToggleButton("教师登录");
        teacherBtn.setToggleGroup(roleGroup);
        teacherBtn.setPrefSize(140, 40);
        teacherBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14; -fx-background-radius: 4; -fx-border-color: #7f8c8d; -fx-border-width: 2;");
        roleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            loginMethodGroup.selectToggle(usernameRadio);
            if (newVal == studentBtn) {
                currentRole = "STUDENT";
                studentBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-border-color: #2980b9;");
                teacherBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-border-color: #7f8c8d;");
            } else {
                currentRole = "TEACHER";
                studentBtn.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-border-color: #95a5a6;");
                teacherBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-border-color: #d35400;");
            }
        });

        roleBox.getChildren().addAll(studentBtn, teacherBtn);

        formBox = new VBox(8);
        formBox.setMaxWidth(340);
        formBox.setPadding(new Insets(20));
        formBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        loginMethodGroup = new ToggleGroup();
        usernameRadio = new RadioButton("用户名登录");
        usernameRadio.setToggleGroup(loginMethodGroup);
        usernameRadio.setSelected(true);
        idRadio = new RadioButton();
        idRadio.setToggleGroup(loginMethodGroup);
        loginMethodGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            rebuildForm();
        });

        radioBox = new HBox(15, usernameRadio, idRadio);
        radioBox.setPadding(new Insets(0, 0, 5, 0));

        usernameLabel = new Label("用户名");
        usernameLabel.setFont(Font.font("Microsoft YaHei", 13));
        usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");
        usernameField.setPrefHeight(32);

        identityLabel = new Label("学号");
        identityLabel.setFont(Font.font("Microsoft YaHei", 13));
        identityField = new TextField();
        identityField.setPromptText("请输入学号");
        identityField.setPrefHeight(32);

        passwordLabel = new Label("密码");
        passwordLabel.setFont(Font.font("Microsoft YaHei", 13));
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setPrefHeight(32);

        buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(8, 0, 3, 0));

        loginBtn = new Button("登  录");
        loginBtn.setPrefSize(110, 35);
        loginBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14; -fx-background-radius: 4;");
        loginBtn.setOnAction(e -> handleLogin());

        registerBtn = new Button("注  册");
        registerBtn.setPrefSize(110, 35);
        registerBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14; -fx-background-radius: 4;");
        registerBtn.setOnAction(e -> {
            RegisterView rv = new RegisterView(stage);
            rv.show();
        });

        buttonBox.getChildren().addAll(loginBtn, registerBtn);

        forgotPwdLink = new Hyperlink("忘记密码?");
        forgotPwdLink.setStyle("-fx-font-size: 12;");
        forgotPwdLink.setOnAction(e -> showForgotPasswordDialog());

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        rebuildForm();
        root.getChildren().addAll(title, roleBox, formBox);

        passwordField.setOnAction(e -> handleLogin());

        return new Scene(root, 420, 520);
    }

    private void rebuildForm() {
        List<Node> children = new ArrayList<>();

        if ("STUDENT".equals(currentRole)) {
            idRadio.setText("学号登录");
        } else {
            idRadio.setText("工号登录");
        }

        children.add(radioBox);

        if (loginMethodGroup.getSelectedToggle() == usernameRadio) {
            usernameLabel.setText("用户名");
            usernameField.setPromptText("请输入用户名");
            usernameField.clear();
            children.add(usernameLabel);
            children.add(usernameField);
        } else {
            if ("STUDENT".equals(currentRole)) {
                identityLabel.setText("学号");
                identityField.setPromptText("请输入学号");
            } else {
                identityLabel.setText("工号");
                identityField.setPromptText("请输入工号");
            }
            identityField.clear();
            children.add(identityLabel);
            children.add(identityField);
        }

        passwordField.clear();
        children.add(passwordLabel);
        children.add(passwordField);
        children.add(buttonBox);
        children.add(forgotPwdLink);
        children.add(statusLabel);

        formBox.getChildren().setAll(children);
    }

    private void handleLogin() {
        String credential;
        String loginMethod;
        if (loginMethodGroup.getSelectedToggle() == usernameRadio) {
            credential = usernameField.getText().trim();
            loginMethod = "USERNAME";
        } else {
            credential = identityField.getText().trim();
            loginMethod = currentRole.equals("STUDENT") ? "STUDENT_ID" : "EMPLOYEE_ID";
        }

        String password = passwordField.getText().trim();

        if (credential.isEmpty() || password.isEmpty()) {
            statusLabel.setText("请填写完整信息");
            return;
        }

        if (password.length() < 8 || password.length() > 15) {
            statusLabel.setText("密码长度需8~15位");
            return;
        }
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*")) {
            statusLabel.setText("密码需至少包含一位字母和一位数字");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");
        statusLabel.setText("正在登录...");

        if (!connector.isConnected()) {
            if (!connector.connect()) {
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
                statusLabel.setText("无法连接到服务器");
                return;
            }
        }

        connector.sendMessage(Message.createLoginRequest(credential, password, currentRole, loginMethod));
    }



    private void showForgotPasswordDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("密码重置");
        dialog.setHeaderText("验证身份以重置密码");

        VBox content = new VBox(8);
        content.setPadding(new Insets(10));

        Label roleLabel = new Label("当前角色: " + (currentRole.equals("STUDENT") ? "学生" : "教师"));
        roleLabel.setStyle("-fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("用户名");

        TextField idField = new TextField();
        idField.setPromptText(currentRole.equals("STUDENT") ? "学号" : "工号");

        PasswordField oldPwdField = new PasswordField();
        oldPwdField.setPromptText("曾用密码");

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");

        content.getChildren().addAll(roleLabel,
                new Label("用户名"), usernameField,
                new Label(currentRole.equals("STUDENT") ? "学号" : "工号"), idField,
                new Label("曾用密码"), oldPwdField,
                resultLabel);

        ButtonType submitBtn = new ButtonType("提交重置", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(submitBtn, cancelBtn);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == submitBtn) {
                String username = usernameField.getText().trim();
                String identityId = idField.getText().trim();
                String oldPwd = oldPwdField.getText().trim();

                if (username.isEmpty() || identityId.isEmpty() || oldPwd.isEmpty()) {
                    resultLabel.setText("请填写所有字段");
                    return null;
                }

                Message req = Message.createPasswordResetRequest(username,
                        currentRole.equals("STUDENT") ? identityId : "",
                        currentRole.equals("TEACHER") ? identityId : "",
                        oldPwd);
                req.setRole(currentRole);
                connector.sendMessage(req);
                return "submitted";
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void handleServerMessage(Message msg) {
        switch (msg.getType()) {
            case LOGIN_SUCCESS:
                callback.onLoginSuccess(msg.getSender(), msg.getRole());
                break;
            case LOGIN_FAIL:
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
                statusLabel.setText(msg.getReason());
                break;
            case REGISTER_FAIL:
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
                statusLabel.setText(msg.getReason());
                break;
            case PASSWORD_RESET_SUCCESS:
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("密码重置成功");
                successAlert.setHeaderText(null);
                successAlert.setContentText(msg.getReason());
                successAlert.showAndWait();
                break;
            case PASSWORD_RESET_FAIL:
                Alert failAlert = new Alert(Alert.AlertType.ERROR);
                failAlert.setTitle("密码重置失败");
                failAlert.setHeaderText(null);
                failAlert.setContentText(msg.getReason());
                failAlert.showAndWait();
                break;
        }
    }
}
