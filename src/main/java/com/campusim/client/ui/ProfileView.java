package com.campusim.client.ui;

import com.campusim.client.network.ServerConnector;
import com.campusim.common.Message;
import com.campusim.common.MessageType;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.util.regex.Pattern;

public class ProfileView {
    private final Stage stage;
    private final ServerConnector connector;
    private final String loginUsername;

    private AvatarView avatarComponent;
    private TextField usernameInput;
    private TextField signatureInput;
    private Label pwdDisplay;
    private Label statusMsg;

    private Label genderVal;
    private Label roleVal;
    private Label studentIdVal;
    private Label gradeVal;
    private Label majorVal;
    private Label classVal;
    private Label employeeIdVal;
    private Label titleVal;
    private Label deptVal;
    private Label createdAtVal;
    private HBox rowStudentId;
    private HBox rowGrade;
    private HBox rowMajor;
    private HBox rowClass;
    private HBox rowEmployeeId;
    private HBox rowTitle;
    private HBox rowDept;

    private String currentUsername;

    private static final Pattern REG_USERNAME = Pattern.compile("^.{2,10}$");
    private static final Pattern REG_PASSWORD = Pattern.compile("^(?=.*[a-zA-Z]).{8,13}$");
    private static final Pattern REG_SIGNATURE = Pattern.compile("^.{2,20}$");

    private static final String[] AVATAR_COLORS = {
        "#3498db", "#e74c3c", "#2ecc71", "#e67e22", "#9b59b6",
        "#1abc9c", "#f39c12", "#34495e", "#16a085", "#c0392b"
    };

    public ProfileView(Stage owner, ServerConnector connector, String username) {
        this.stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("个人中心");
        stage.setResizable(false);
        this.connector = connector;
        this.loginUsername = username;
        this.currentUsername = username;
    }

    public Scene createScene() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f5f5;");

        Label title = new Label("个人中心");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 20));

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #f5f5f5; -fx-border-color: transparent;");

        VBox form = new VBox(8);
        form.setPadding(new Insets(0, 5, 0, 5));

        // --- avatar ---
        avatarComponent = new AvatarView(currentUsername, 80);
        avatarComponent.loadAvatar(currentUsername);

        Button changeAvatar = new Button("更换头像");
        styleBtn(changeAvatar, "#3498db");
        changeAvatar.setOnAction(e -> handleChangeAvatar());

        VBox avatarSection = new VBox(5, avatarComponent, changeAvatar);
        avatarSection.setAlignment(Pos.CENTER);
        avatarSection.setPadding(new Insets(5, 0, 10, 0));
        form.getChildren().add(avatarSection);

        // --- editable fields ---
        usernameInput = new TextField(currentUsername);
        usernameInput.setPrefWidth(140);
        Button saveUname = new Button("保存用户名");
        styleBtn(saveUname, "#3498db");
        saveUname.setOnAction(e -> saveUsername());
        form.getChildren().add(makeEditRow("用户名", usernameInput, saveUname));

        pwdDisplay = new Label("*********");
        pwdDisplay.setStyle("-fx-font-size: 13;");
        Button changePwd = new Button("修改密码");
        styleBtn(changePwd, "#e67e22");
        changePwd.setOnAction(e -> showChangePasswordDialog());
        form.getChildren().add(makeRowWithBtn("密码", pwdDisplay, changePwd));

        signatureInput = new TextField("点击加载后编辑");
        signatureInput.setPrefWidth(140);
        Button saveSig = new Button("保存签名");
        styleBtn(saveSig, "#3498db");
        saveSig.setOnAction(e -> saveSignature());
        form.getChildren().add(makeEditRow("个人签名", signatureInput, saveSig));

        form.getChildren().add(new Separator());

        // --- readonly fields ---
        genderVal = new Label();
        roleVal = new Label();
        studentIdVal = new Label();
        gradeVal = new Label();
        majorVal = new Label();
        classVal = new Label();
        employeeIdVal = new Label();
        titleVal = new Label();
        deptVal = new Label();
        createdAtVal = new Label();

        form.getChildren().add(makeReadRow("性别", genderVal));
        form.getChildren().add(makeReadRow("角色", roleVal));
        rowStudentId = makeReadRow("学号", studentIdVal); form.getChildren().add(rowStudentId);
        rowDept = makeReadRow("学院", deptVal); form.getChildren().add(rowDept);
        rowMajor = makeReadRow("专业", majorVal); form.getChildren().add(rowMajor);
        rowGrade = makeReadRow("入学年份", gradeVal); form.getChildren().add(rowGrade);
        rowClass = makeReadRow("班级", classVal); form.getChildren().add(rowClass);
        rowEmployeeId = makeReadRow("工号", employeeIdVal); form.getChildren().add(rowEmployeeId);
        rowTitle = makeReadRow("职称", titleVal); form.getChildren().add(rowTitle);
        form.getChildren().add(makeReadRow("注册时间", createdAtVal));

        statusMsg = new Label();
        statusMsg.setStyle("-fx-font-size: 12; -fx-text-fill: #e74c3c;");
        form.getChildren().add(statusMsg);

        scroll.setContent(form);
        root.getChildren().addAll(title, scroll);
        requestProfile();
        return new Scene(root, 380, 560);
    }

    // ---- helpers ----

    private void setRowVisible(HBox row, boolean visible) {
        row.setManaged(visible);
        row.setVisible(visible);
    }

    private HBox makeEditRow(String label, TextField input, Button btn) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label + "：");
        lbl.setPrefWidth(70);
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        row.getChildren().addAll(lbl, input, btn);
        return row;
    }

    private HBox makeRowWithBtn(String label, Node display, Button btn) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label + "：");
        lbl.setPrefWidth(70);
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        row.getChildren().addAll(lbl, display, btn);
        return row;
    }

    private HBox makeReadRow(String label, Label val) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label + "：");
        lbl.setPrefWidth(70);
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        val.setStyle("-fx-font-size: 13;");
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private void styleBtn(Button btn, String color) {
        btn.setStyle("-fx-font-size: 11; -fx-background-color: " + color
            + "; -fx-text-fill: white; -fx-background-radius: 3;");
    }

    // ---- avatar ----

    // ---- network ----

    private void requestProfile() {
        connector.sendMessage(new Message(MessageType.PROFILE_GET_REQ));
    }

    public void handleProfileResponse(Message msg) {
        if (!msg.isSuccess()) return;

        currentUsername = msg.getSender();
        usernameInput.setText(currentUsername);
        signatureInput.setText(msg.getContent());
        genderVal.setText(msg.getGender() != null && !msg.getGender().isEmpty() ? msg.getGender() : "未设置");
        roleVal.setText(formatRole(msg.getRole()));
        createdAtVal.setText(msg.getTimestamp());

        String role = msg.getRole();

        studentIdVal.setText((msg.getStudentId() != null ? msg.getStudentId() : ""));
        employeeIdVal.setText((msg.getEmployeeId() != null ? msg.getEmployeeId() : ""));

        String extra = msg.getUserExtra();
        if ("STUDENT".equals(role) && extra != null) {
            String[] p = extra.split("\\|");
            gradeVal.setText(p.length > 0 ? p[0] : "");
            majorVal.setText(p.length > 1 ? p[1] : "");
            classVal.setText(p.length > 2 ? p[2] : "");
            deptVal.setText(p.length > 3 ? p[3] : "");
            setRowVisible(rowStudentId, true);
            setRowVisible(rowGrade, true);
            setRowVisible(rowMajor, true);
            setRowVisible(rowClass, true);
            setRowVisible(rowDept, true);
            setRowVisible(rowEmployeeId, false);
            setRowVisible(rowTitle, false);
        } else if ("TEACHER".equals(role) && extra != null) {
            String[] p = extra.split("\\|");
            titleVal.setText(p.length > 0 ? p[0] : "");
            deptVal.setText(p.length > 1 ? p[1] : "");
            setRowVisible(rowStudentId, false);
            setRowVisible(rowGrade, false);
            setRowVisible(rowMajor, false);
            setRowVisible(rowClass, false);
            setRowVisible(rowEmployeeId, true);
            setRowVisible(rowTitle, true);
            setRowVisible(rowDept, true);
        } else {
            setRowVisible(rowStudentId, false);
            setRowVisible(rowGrade, false);
            setRowVisible(rowMajor, false);
            setRowVisible(rowClass, false);
            setRowVisible(rowEmployeeId, false);
            setRowVisible(rowTitle, false);
            setRowVisible(rowDept, false);
        }

        avatarComponent.loadAvatar(currentUsername);
    }

    // ---- actions ----

    private void saveUsername() {
        String name = usernameInput.getText().trim();
        if (!REG_USERNAME.matcher(name).matches()) {
            statusMsg.setText("用户名需2~10个字符");
            return;
        }
        Message m = new Message(MessageType.PROFILE_UPDATE_USERNAME_REQ);
        m.setContent(name);
        connector.sendMessage(m);
    }

    private void saveSignature() {
        String sig = signatureInput.getText().trim();
        if (!REG_SIGNATURE.matcher(sig).matches()) {
            statusMsg.setText("签名需2~20个字符");
            return;
        }
        Message m = new Message(MessageType.PROFILE_UPDATE_SIGNATURE_REQ);
        m.setContent(sig);
        connector.sendMessage(m);
    }

    private void showChangePasswordDialog() {
        Dialog<String> d = new Dialog<>();
        d.initOwner(stage);
        d.setTitle("修改密码");
        d.setHeaderText("验证身份并设置新密码");

        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        TextField uname = new TextField(currentUsername);
        uname.setEditable(false);
        TextField idField = new TextField();
        idField.setPromptText("学号或工号");
        PasswordField oldPwd = new PasswordField();
        oldPwd.setPromptText("旧密码");
        PasswordField newPwd = new PasswordField();
        newPwd.setPromptText("新密码（8~13位，至少1个字母）");
        PasswordField confirm = new PasswordField();
        confirm.setPromptText("确认新密码");
        Label err = new Label();
        err.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");

        box.getChildren().addAll(
            new Label("用户名"), uname,
            new Label("学号/工号"), idField,
            new Label("旧密码"), oldPwd,
            new Label("新密码"), newPwd,
            new Label("确认新密码"), confirm,
            err
        );

        ButtonType submit = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        d.getDialogPane().getButtonTypes().addAll(submit, cancel);
        d.getDialogPane().setContent(box);

        d.setResultConverter(btn -> {
            if (btn != submit) return null;
            String id = idField.getText().trim();
            String oldP = oldPwd.getText();
            String newP = newPwd.getText();
            String cfm = confirm.getText();
            if (id.isEmpty() || oldP.isEmpty() || newP.isEmpty()) {
                err.setText("请填写所有字段"); return null;
            }
            if (!newP.equals(cfm)) { err.setText("两次新密码不一致"); return null; }
            if (!REG_PASSWORD.matcher(newP).matches()) {
                err.setText("新密码需8~13位且至少1个字母"); return null;
            }
            Message m = new Message(MessageType.PROFILE_UPDATE_PASSWORD_REQ);
            m.setContent(oldP);
            m.setNewPassword(newP);
            m.setStudentId(id);
            m.setEmployeeId(id);
            connector.sendMessage(m);
            return "ok";
        });
        d.showAndWait();
    }

    private void handleChangeAvatar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("选择头像");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片", "*.png","*.jpg","*.jpeg","*.gif"));
        File file = fc.showOpenDialog(stage);
        if (file == null) return;
        try {
            Image img = new Image(file.toURI().toString());
            if (img.getWidth() > 500 || img.getHeight() > 500) {
                statusMsg.setText("图片尺寸不超过500x500");
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            int chunkSize = 65536;
            int total = (int) Math.ceil((double) data.length / chunkSize);

            Message start = new Message(MessageType.AVATAR_UPLOAD_START);
            start.setTotalChunks(total);
            connector.sendMessage(start);

            for (int i = 0; i < total; i++) {
                int from = i * chunkSize;
                int to = Math.min(from + chunkSize, data.length);
                byte[] chunk = new byte[to - from];
                System.arraycopy(data, from, chunk, 0, chunk.length);

                Message m = new Message(MessageType.AVATAR_UPLOAD_CHUNK);
                m.setSeqNum(i);
                m.setTotalChunks(total);
                m.setChunkData(chunk);
                connector.sendMessage(m);
                Thread.sleep(5);
            }
        } catch (Exception e) {
            statusMsg.setText("头像上传失败");
        }
    }

    // ---- misc ----

    private String formatRole(String role) {
        if ("ADMIN".equals(role)) return "管理员";
        if ("TEACHER".equals(role)) return "教师";
        if ("STUDENT".equals(role)) return "学生";
        return role;
    }

    // ---- lifecycle ----

    public void show() {
        stage.setScene(createScene());
        stage.centerOnScreen();
        stage.show();
    }

    public Stage getStage() {
        return stage;
    }
}
