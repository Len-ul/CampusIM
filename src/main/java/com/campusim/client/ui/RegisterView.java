package com.campusim.client.ui;

import com.campusim.client.network.ServerConnector;
import com.campusim.common.Message;
import com.campusim.common.MessageType;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class RegisterView {
    private final Stage stage;
    private ServerConnector connector;
    private ToggleButton studentBtn;
    private ToggleButton teacherBtn;
    private TextField usernameField;
    private ToggleGroup genderGroup;
    private RadioButton maleRadio;
    private RadioButton femaleRadio;
    private TextField identityField;
    private ComboBox<String> deptCombo;
    private ComboBox<String> majorCombo;
    private TextField gradeField;
    private TextField classField;
    private ComboBox<String> teacherDeptCombo;
    private TextField titleField;
    private PasswordField passwordField;
    private PasswordField confirmField;
    private Label identityLabel;
    private VBox studentExtraBox;
    private VBox teacherExtraBox;
    private Label statusLabel;
    private Button registerBtn;
    private String currentRole = "STUDENT";

    private static final double LABEL_WIDTH = 60;
    private static final double FIELD_WIDTH = 220;

    public RegisterView(Stage owner) {
        this.stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("用户注册");
        stage.setResizable(false);
    }

    public Scene createScene() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #f5f5f5;");

        Label title = new Label("用户注册");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2c3e50;");

        VBox formBox = new VBox(8);
        formBox.setMaxWidth(340);
        formBox.setPadding(new Insets(20));
        formBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        HBox roleBox = new HBox(15);
        roleBox.setAlignment(Pos.CENTER);
        ToggleGroup roleGroup = new ToggleGroup();
        studentBtn = new ToggleButton("学生注册");
        studentBtn.setToggleGroup(roleGroup);
        studentBtn.setSelected(true);
        studentBtn.setPrefSize(130, 38);
        studentBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 13; -fx-background-radius: 4;");
        teacherBtn = new ToggleButton("教师注册");
        teacherBtn.setToggleGroup(roleGroup);
        teacherBtn.setPrefSize(130, 38);
        teacherBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 13; -fx-background-radius: 4;");
        roleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            usernameField.clear();
            identityField.clear();
            gradeField.clear();
            classField.clear();
            titleField.clear();
            passwordField.clear();
            confirmField.clear();
            statusLabel.setText("");
            registerBtn.setDisable(false);
            if (newVal == studentBtn) {
                currentRole = "STUDENT";
                studentBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-border-color: #2980b9;");
                teacherBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-border-color: #7f8c8d;");
                identityLabel.setText("学号");
                identityField.setPromptText("S+7位数字");
                studentExtraBox.setVisible(true);
                studentExtraBox.setManaged(true);
                teacherExtraBox.setVisible(false);
                teacherExtraBox.setManaged(false);
            } else {
                currentRole = "TEACHER";
                studentBtn.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-border-color: #95a5a6;");
                teacherBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-border-color: #d35400;");
                identityLabel.setText("工号");
                identityField.setPromptText("T+7位数字");
                studentExtraBox.setVisible(false);
                studentExtraBox.setManaged(false);
                teacherExtraBox.setVisible(true);
                teacherExtraBox.setManaged(true);
            }
        });
        roleBox.getChildren().addAll(studentBtn, teacherBtn);

        formBox.getChildren().add(roleBox);
        formBox.getChildren().add(makeRow("用户名", usernameField = new TextField() {{
            setPromptText("2~10个字符");
            setPrefHeight(32);
            setPrefWidth(FIELD_WIDTH);
        }}));

        HBox genderRow = new HBox(10);
        genderRow.setAlignment(Pos.CENTER_LEFT);
        Label genderLabel = new Label("性别");
        genderLabel.setFont(Font.font("Microsoft YaHei", 13));
        genderLabel.setPrefWidth(LABEL_WIDTH);
        genderGroup = new ToggleGroup();
        maleRadio = new RadioButton("男");
        maleRadio.setToggleGroup(genderGroup);
        maleRadio.setSelected(true);
        femaleRadio = new RadioButton("女");
        femaleRadio.setToggleGroup(genderGroup);
        genderRow.getChildren().addAll(genderLabel, maleRadio, femaleRadio);
        formBox.getChildren().add(genderRow);

        HBox identityRow = new HBox(10);
        identityRow.setAlignment(Pos.CENTER_LEFT);
        identityLabel = new Label("学号");
        identityLabel.setFont(Font.font("Microsoft YaHei", 13));
        identityLabel.setPrefWidth(LABEL_WIDTH);
        identityField = new TextField();
        identityField.setPromptText("S+7位数字，如S2024001");
        identityField.setPrefHeight(32);
        identityField.setPrefWidth(FIELD_WIDTH);
        identityRow.getChildren().addAll(identityLabel, identityField);
        formBox.getChildren().add(identityRow);

        studentExtraBox = new VBox(8);
        deptCombo = new ComboBox<>();
        deptCombo.setPrefHeight(32);
        deptCombo.setPrefWidth(FIELD_WIDTH);
        deptCombo.setPromptText("请选择学院");
        deptCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && connector != null && connector.isConnected()) {
                Message majorReq = new Message(MessageType.MAJOR_LIST_REQ);
                majorReq.setContent(newVal);
                connector.sendMessage(majorReq);
                majorCombo.getItems().clear();
                majorCombo.setPromptText("加载中...");
            }
        });
        studentExtraBox.getChildren().add(makeRow("学院", deptCombo));
        majorCombo = new ComboBox<>();
        majorCombo.setPrefHeight(32);
        majorCombo.setPrefWidth(FIELD_WIDTH);
        majorCombo.setPromptText("请先选择学院");
        majorCombo.setDisable(true);
        studentExtraBox.getChildren().add(makeRow("专业", majorCombo));
        studentExtraBox.getChildren().add(makeRow("入学年份", gradeField = new TextField() {{
            setPromptText("4位，如2024");
            setPrefHeight(32);
            setPrefWidth(FIELD_WIDTH);
        }}));
        studentExtraBox.getChildren().add(makeRow("班级", classField = new TextField() {{
            setPromptText("4位，如2401");
            setPrefHeight(32);
            setPrefWidth(FIELD_WIDTH);
        }}));
        formBox.getChildren().add(studentExtraBox);

        teacherExtraBox = new VBox(8);
        teacherDeptCombo = new ComboBox<>();
        teacherDeptCombo.setPrefHeight(32);
        teacherDeptCombo.setPrefWidth(FIELD_WIDTH);
        teacherDeptCombo.setPromptText("请选择学院");
        teacherExtraBox.getChildren().add(makeRow("学院", teacherDeptCombo));
        teacherExtraBox.getChildren().add(makeRow("职称", titleField = new TextField() {{
            setPromptText("2~10个字符");
            setPrefHeight(32);
            setPrefWidth(FIELD_WIDTH);
        }}));
        teacherExtraBox.setVisible(false);
        teacherExtraBox.setManaged(false);
        formBox.getChildren().add(teacherExtraBox);

        formBox.getChildren().add(makeRow("密码", passwordField = new PasswordField() {{
            setPromptText("8~15位，含字母+数字");
            setPrefHeight(32);
            setPrefWidth(FIELD_WIDTH);
        }}));
        formBox.getChildren().add(makeRow("确认密码", confirmField = new PasswordField() {{
            setPromptText("请再次输入密码");
            setPrefHeight(32);
            setPrefWidth(FIELD_WIDTH);
        }}));

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        formBox.getChildren().add(statusLabel);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(5, 0, 0, 0));
        registerBtn = new Button("提 交 注 册");
        registerBtn.setPrefSize(150, 36);
        registerBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 14; -fx-background-radius: 4;");
        registerBtn.setOnAction(e -> handleRegister());
        buttonBox.getChildren().add(registerBtn);
        formBox.getChildren().add(buttonBox);

        Hyperlink backLink = new Hyperlink("返回登录");
        backLink.setStyle("-fx-font-size: 12;");
        backLink.setAlignment(Pos.CENTER);
        backLink.setOnAction(e -> stage.close());
        formBox.getChildren().add(backLink);

        root.getChildren().addAll(title, formBox);
        return new Scene(root, 420, 620);
    }

    private HBox makeRow(String labelText, Control field) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.setFont(Font.font("Microsoft YaHei", 13));
        label.setPrefWidth(LABEL_WIDTH);
        row.getChildren().addAll(label, field);
        return row;
    }

    public void show() {
        stage.setScene(createScene());
        connector = new ServerConnector("127.0.0.1", 8888);
        connector.setMessageListener(new ServerConnector.MessageListener() {
            @Override
            public void onMessageReceived(Message msg) {
                Platform.runLater(() -> {
                    if (msg.getType() == MessageType.DEPT_LIST_RESP) {
                        java.util.List<String> depts = msg.getOnlineUsers();
                        deptCombo.getItems().setAll(depts);
                        teacherDeptCombo.getItems().setAll(depts);
                    } else if (msg.getType() == MessageType.MAJOR_LIST_RESP) {
                        java.util.List<String> majors = msg.getOnlineUsers();
                        majorCombo.getItems().setAll(majors);
                        majorCombo.setPromptText("请选择专业");
                        majorCombo.setDisable(false);
                    } else if (msg.getType() == MessageType.REGISTER_SUCCESS) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.initOwner(stage);
                        alert.setTitle("注册成功");
                        alert.setHeaderText(null);
                        alert.setContentText("注册成功，请返回登录");
                        alert.showAndWait();
                        stage.close();
                    } else if (msg.getType() == MessageType.REGISTER_FAIL) {
                        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
                        statusLabel.setText(msg.getReason());
                        registerBtn.setDisable(false);
                    }
                });
            }

            @Override
            public void onConnectionError(String error) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
                    statusLabel.setText("连接服务器失败");
                });
            }
        });
        if (connector.connect()) {
            connector.sendMessage(new Message(MessageType.DEPT_LIST_REQ));
        } else {
            statusLabel.setText("无法连接到服务器");
        }
        stage.showAndWait();
        if (connector != null) {
            connector.disconnect();
        }
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm = confirmField.getText().trim();
        String gender = maleRadio.isSelected() ? "男" : "女";
        String identity = identityField.getText().trim();
        String grade = gradeField.getText().trim();
        String className = classField.getText().trim();
        String title = titleField.getText().trim();
        String dept = "STUDENT".equals(currentRole) ? deptCombo.getValue() : teacherDeptCombo.getValue();
        String major = majorCombo.getValue();

        String error = validateInput(username, password, confirm, gender, identity, grade, major, className, title, dept);
        if (error != null) {
            statusLabel.setText(error);
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");
        statusLabel.setText("正在注册...");
        registerBtn.setDisable(true);

        String studentId = "STUDENT".equals(currentRole) ? identity : "";
        String employeeId = "TEACHER".equals(currentRole) ? identity : "";

        connector.sendMessage(Message.createRegisterRequest(username, password, currentRole,
                gender, studentId, employeeId,
                grade, major != null ? major : "", className,
                title, dept != null ? dept : ""));
    }

    private String validateInput(String username, String password, String confirm,
                                  String gender, String identity,
                                  String grade, String major, String className,
                                  String title, String department) {
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty() || identity.isEmpty())
            return "请填写所有字段";
        if (username.length() < 2 || username.length() > 10)
            return "用户名长度需2~10位";
        if (!password.equals(confirm))
            return "两次密码输入不一致";
        if (password.length() < 8 || password.length() > 15)
            return "密码长度需8~15位";
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*"))
            return "密码需至少包含一位字母和一位数字";
        if (!gender.matches("男|女"))
            return "性别只能为男或女";

        if ("STUDENT".equals(currentRole)) {
            if (!identity.matches("S\\d{7}"))
                return "学号格式不正确，需为S加7位数字";
            if (grade.isEmpty() || className.isEmpty())
                return "请填写所有字段";
            if (grade.length() != 4)
                return "入学年份长度需为4位";
            if (className.length() != 4)
                return "班级长度需为4位";
            if (department == null || department.isEmpty())
                return "请选择学院";
            if (major == null || major.isEmpty())
                return "请选择专业";
        } else {
            if (!identity.matches("T\\d{7}"))
                return "工号格式不正确，需为T加7位数字";
            if (title.isEmpty())
                return "请填写所有字段";
            if (title.length() < 2 || title.length() > 10)
                return "职称长度需2~10位";
            if (department == null || department.isEmpty())
                return "请选择学院";
        }
        return null;
    }
}
