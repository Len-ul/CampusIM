package com.campusim.administrator.ui;

import com.campusim.administrator.AdminManager;
import com.campusim.administrator.model.Student;
import com.campusim.administrator.model.Teacher;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class AdminMainView {
    private final Stage stage;
    private final String account;
    private final String name;

    private TableView<Student> studentTable;
    private TableView<Teacher> teacherTable;
    private TabPane tabPane;

    private TextField sUsernameField, sPasswordField, sStudentIdField;
    private ComboBox<String> sGenderField;
    private TextField sGradeField, sMajorField, sClassField, sDeptField;

    private TextField tUsernameField, tPasswordField, tEmployeeIdField;
    private ComboBox<String> tGenderField;
    private TextField tTitleField, tDepartmentField;

    private Label statusLabel;

    private int editingStudentId = -1;
    private int editingTeacherId = -1;
    private Button sAddBtn, sSaveBtn, sCancelBtn;
    private Button tAddBtn, tSaveBtn, tCancelBtn;

    public AdminMainView(Stage stage, String account, String name) {
        this.stage = stage;
        this.account = account;
        this.name = name;
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();

        HBox topBar = new HBox();
        topBar.setPadding(new Insets(10, 15, 10, 15));
        topBar.setStyle("-fx-background-color: #2c3e50;");
        Label titleLabel = new Label("管理员后台 - " + (name != null ? name : account));
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: white;");
        topBar.getChildren().add(titleLabel);

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab studentTab = new Tab("学生管理", createStudentPane());
        studentTab.setOnSelectionChanged(e -> {
            if (studentTab.isSelected()) loadStudents();
        });

        Tab teacherTab = new Tab("教师管理", createTeacherPane());
        teacherTab.setOnSelectionChanged(e -> {
            if (teacherTab.isSelected()) loadTeachers();
        });

        tabPane.getTabs().addAll(studentTab, teacherTab);

        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");
        HBox bottomBar = new HBox(5);
        bottomBar.setPadding(new Insets(5, 10, 5, 10));
        bottomBar.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0;");
        bottomBar.getChildren().add(statusLabel);

        root.setTop(topBar);
        root.setCenter(tabPane);
        root.setBottom(bottomBar);

        loadStudents();

        Scene scene = new Scene(root, 900, 650);
        stage.setMinWidth(750);
        stage.setMinHeight(500);
        return scene;
    }

    private BorderPane createStudentPane() {
        BorderPane pane = new BorderPane();

        studentTable = new TableView<>();
        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Student, String> colUsername = new TableColumn<>("用户名");
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));

        TableColumn<Student, String> colStudentId = new TableColumn<>("学号");
        colStudentId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentId()));

        TableColumn<Student, String> colGender = new TableColumn<>("性别");
        colGender.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getGender() != null ? data.getValue().getGender() : ""));

        TableColumn<Student, String> colGrade = new TableColumn<>("年级");
        colGrade.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getGrade() != null ? data.getValue().getGrade() : ""));

        TableColumn<Student, String> colMajor = new TableColumn<>("专业");
        colMajor.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getMajor() != null ? data.getValue().getMajor() : ""));

        TableColumn<Student, String> colClass = new TableColumn<>("班级");
        colClass.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getClassName() != null ? data.getValue().getClassName() : ""));

        TableColumn<Student, String> colDept = new TableColumn<>("学院");
        colDept.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDepartment() != null ? data.getValue().getDepartment() : ""));

        studentTable.getColumns().addAll(colUsername, colStudentId, colGender, colGrade, colMajor, colClass, colDept);

        HBox toolBar = new HBox(10);
        toolBar.setPadding(new Insets(10));
        toolBar.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = new Button("刷新列表");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4;");
        refreshBtn.setOnAction(e -> loadStudents());

        Button editBtn = new Button("编辑选中");
        editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 4;");
        editBtn.setOnAction(e -> {
            Student selected = studentTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                setStatus("请先选中要编辑的学生", true);
                return;
            }
            populateStudentForm(selected);
        });

        Button deleteBtn = new Button("删除选中");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4;");
        deleteBtn.setOnAction(e -> {
            Student selected = studentTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                setStatus("请先选中要删除的学生", true);
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("确认删除");
            confirm.setHeaderText("确定要删除学生 [" + selected.getUsername() + "] 吗？");
            confirm.setContentText("此操作不可恢复！");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                new Thread(() -> {
                    boolean ok = AdminManager.deleteUser(selected.getUsername());
                    Platform.runLater(() -> {
                        setStatus(ok ? "删除成功" : "删除失败", !ok);
                        if (ok) loadStudents();
                    });
                }).start();
            }
        });

        toolBar.getChildren().addAll(refreshBtn, editBtn, deleteBtn);

        VBox addPanel = createStudentAddForm();

        pane.setCenter(studentTable);
        pane.setTop(toolBar);
        pane.setBottom(addPanel);
        return pane;
    }

    private VBox createStudentAddForm() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");

        Label addTitle = new Label("添加学生");
        addTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 13));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);

        sUsernameField = new TextField();
        sUsernameField.setPromptText("用户名");
        sUsernameField.setPrefWidth(80);
        sPasswordField = new TextField();
        sPasswordField.setPromptText("密码");
        sPasswordField.setPrefWidth(80);
        sStudentIdField = new TextField();
        sStudentIdField.setPromptText("学号");
        sStudentIdField.setPrefWidth(80);
        sGenderField = new ComboBox<>();
        sGenderField.getItems().addAll("男", "女");
        sGenderField.setPromptText("性别");
        sGenderField.setPrefWidth(60);
        sGradeField = new TextField();
        sGradeField.setPromptText("年级");
        sGradeField.setPrefWidth(60);
        sMajorField = new TextField();
        sMajorField.setPromptText("专业");
        sMajorField.setPrefWidth(80);
        sClassField = new TextField();
        sClassField.setPromptText("班级");
        sClassField.setPrefWidth(80);
        sDeptField = new TextField();
        sDeptField.setPromptText("学院");
        sDeptField.setPrefWidth(80);

        grid.add(new Label("用户名:"), 0, 0);
        grid.add(sUsernameField, 1, 0);
        grid.add(new Label("密码:"), 2, 0);
        grid.add(sPasswordField, 3, 0);
        grid.add(new Label("学号:"), 4, 0);
        grid.add(sStudentIdField, 5, 0);
        grid.add(new Label("性别:"), 0, 1);
        grid.add(sGenderField, 1, 1);
        grid.add(new Label("年级:"), 2, 1);
        grid.add(sGradeField, 3, 1);
        grid.add(new Label("专业:"), 4, 1);
        grid.add(sMajorField, 5, 1);
        grid.add(new Label("班级:"), 6, 1);
        grid.add(sClassField, 7, 1);
        grid.add(new Label("学院:"), 0, 2);
        grid.add(sDeptField, 1, 2);

        sAddBtn = new Button("添 加");
        sAddBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 13;");
        sAddBtn.setOnAction(e -> {
            if (sUsernameField.getText().trim().isEmpty() || sPasswordField.getText().trim().isEmpty()
                    || sStudentIdField.getText().trim().isEmpty()) {
                setStatus("用户名、密码、学号为必填项", true);
                return;
            }
            if (!sUsernameField.getText().trim().matches("^.{2,10}$")) {
                setStatus("用户名需2~10个字符", true);
                return;
            }
            if (!sPasswordField.getText().trim().matches("^(?=.*[a-zA-Z]).{8,13}$")) {
                setStatus("密码需8~13位且至少包含1个字母", true);
                return;
            }
            new Thread(() -> {
                boolean ok = AdminManager.addStudent(
                        sUsernameField.getText().trim(), sPasswordField.getText().trim(),
                        sStudentIdField.getText().trim(),
                        sGenderField.getValue(), sGradeField.getText().trim(),
                        sMajorField.getText().trim(), sClassField.getText().trim(),
                        sDeptField.getText().trim());
                Platform.runLater(() -> {
                    setStatus(ok ? "添加成功" : "添加失败，用户名或学号可能已存在", !ok);
                    if (ok) { loadStudents(); clearStudentForm(); }
                });
            }).start();
        });

        sSaveBtn = new Button("保存修改");
        sSaveBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 13;");
        sSaveBtn.setVisible(false);
        sSaveBtn.setOnAction(e -> {
            if (sUsernameField.getText().trim().isEmpty()
                    || sStudentIdField.getText().trim().isEmpty()) {
                setStatus("用户名、学号为必填项（密码留空不修改）", true);
                return;
            }
            if (!sUsernameField.getText().trim().matches("^.{2,10}$")) {
                setStatus("用户名需2~10个字符", true);
                return;
            }
            if (!sPasswordField.getText().trim().isEmpty()
                    && !sPasswordField.getText().trim().matches("^(?=.*[a-zA-Z]).{8,13}$")) {
                setStatus("密码需8~13位且至少包含1个字母", true);
                return;
            }
            new Thread(() -> {
                boolean ok = AdminManager.updateStudent(editingStudentId,
                        sGenderField.getValue(),
                        sPasswordField.getText().trim().isEmpty() ? null : sPasswordField.getText().trim(),
                        sStudentIdField.getText().trim(), sGradeField.getText().trim(),
                        sMajorField.getText().trim(), sClassField.getText().trim(),
                        sDeptField.getText().trim());
                Platform.runLater(() -> {
                    setStatus(ok ? "修改成功" : "修改失败", !ok);
                    if (ok) { loadStudents(); clearStudentForm(); }
                });
            }).start();
        });

        sCancelBtn = new Button("取消编辑");
        sCancelBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 13;");
        sCancelBtn.setVisible(false);
        sCancelBtn.setOnAction(e -> clearStudentForm());

        HBox btnBox = new HBox(10, sAddBtn, sSaveBtn, sCancelBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        btnBox.setPadding(new Insets(5, 0, 0, 0));

        panel.getChildren().addAll(addTitle, grid, btnBox);
        return panel;
    }

    private void populateStudentForm(Student s) {
        editingStudentId = s.getId();
        sUsernameField.setText(s.getUsername());
        sPasswordField.setText("");
        sPasswordField.setPromptText("留空则不修改密码");
        sStudentIdField.setText(s.getStudentId());
        sGenderField.setValue(s.getGender() != null ? s.getGender() : null);
        sGradeField.setText(s.getGrade() != null ? s.getGrade() : "");
        sMajorField.setText(s.getMajor() != null ? s.getMajor() : "");
        sClassField.setText(s.getClassName() != null ? s.getClassName() : "");
        sDeptField.setText(s.getDepartment() != null ? s.getDepartment() : "");
        sUsernameField.setEditable(false);
        sAddBtn.setVisible(false);
        sSaveBtn.setVisible(true);
        sCancelBtn.setVisible(true);
    }

    private void clearStudentForm() {
        editingStudentId = -1;
        sUsernameField.clear();
        sUsernameField.setEditable(true);
        sPasswordField.clear();
        sPasswordField.setPromptText("密码");
        sStudentIdField.clear();
        sGenderField.setValue(null);
        sGradeField.clear();
        sMajorField.clear();
        sClassField.clear();
        sDeptField.clear();
        sAddBtn.setVisible(true);
        sSaveBtn.setVisible(false);
        sCancelBtn.setVisible(false);
    }

    private BorderPane createTeacherPane() {
        BorderPane pane = new BorderPane();

        teacherTable = new TableView<>();
        teacherTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Teacher, String> colUsername = new TableColumn<>("用户名");
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));

        TableColumn<Teacher, String> colEmployeeId = new TableColumn<>("工号");
        colEmployeeId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmployeeId()));

        TableColumn<Teacher, String> colGender = new TableColumn<>("性别");
        colGender.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getGender() != null ? data.getValue().getGender() : ""));

        TableColumn<Teacher, String> colTitle = new TableColumn<>("职称");
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTitle() != null ? data.getValue().getTitle() : ""));

        TableColumn<Teacher, String> colDepartment = new TableColumn<>("部门");
        colDepartment.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDepartment() != null ? data.getValue().getDepartment() : ""));

        teacherTable.getColumns().addAll(colUsername, colEmployeeId, colGender, colTitle, colDepartment);

        HBox toolBar = new HBox(10);
        toolBar.setPadding(new Insets(10));
        toolBar.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = new Button("刷新列表");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4;");
        refreshBtn.setOnAction(e -> loadTeachers());

        Button editBtn = new Button("编辑选中");
        editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 4;");
        editBtn.setOnAction(e -> {
            Teacher selected = teacherTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                setStatus("请先选中要编辑的教师", true);
                return;
            }
            populateTeacherForm(selected);
        });

        Button deleteBtn = new Button("删除选中");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4;");
        deleteBtn.setOnAction(e -> {
            Teacher selected = teacherTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                setStatus("请先选中要删除的教师", true);
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("确认删除");
            confirm.setHeaderText("确定要删除教师 [" + selected.getUsername() + "] 吗？");
            confirm.setContentText("此操作不可恢复！");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                new Thread(() -> {
                    boolean ok = AdminManager.deleteUser(selected.getUsername());
                    Platform.runLater(() -> {
                        setStatus(ok ? "删除成功" : "删除失败", !ok);
                        if (ok) loadTeachers();
                    });
                }).start();
            }
        });

        toolBar.getChildren().addAll(refreshBtn, editBtn, deleteBtn);

        VBox addPanel = createTeacherAddForm();

        pane.setCenter(teacherTable);
        pane.setTop(toolBar);
        pane.setBottom(addPanel);
        return pane;
    }

    private VBox createTeacherAddForm() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");

        Label addTitle = new Label("添加教师");
        addTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 13));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);

        tUsernameField = new TextField();
        tUsernameField.setPromptText("用户名");
        tUsernameField.setPrefWidth(80);
        tPasswordField = new TextField();
        tPasswordField.setPromptText("密码");
        tPasswordField.setPrefWidth(80);
        tEmployeeIdField = new TextField();
        tEmployeeIdField.setPromptText("工号");
        tEmployeeIdField.setPrefWidth(80);
        tGenderField = new ComboBox<>();
        tGenderField.getItems().addAll("男", "女");
        tGenderField.setPromptText("性别");
        tGenderField.setPrefWidth(60);
        tTitleField = new TextField();
        tTitleField.setPromptText("职称");
        tTitleField.setPrefWidth(80);
        tDepartmentField = new TextField();
        tDepartmentField.setPromptText("部门");
        tDepartmentField.setPrefWidth(100);

        grid.add(new Label("用户名:"), 0, 0);
        grid.add(tUsernameField, 1, 0);
        grid.add(new Label("密码:"), 2, 0);
        grid.add(tPasswordField, 3, 0);
        grid.add(new Label("工号:"), 4, 0);
        grid.add(tEmployeeIdField, 5, 0);
        grid.add(new Label("性别:"), 0, 1);
        grid.add(tGenderField, 1, 1);
        grid.add(new Label("职称:"), 2, 1);
        grid.add(tTitleField, 3, 1);
        grid.add(new Label("部门:"), 0, 2);
        grid.add(tDepartmentField, 1, 2);

        tAddBtn = new Button("添 加");
        tAddBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 13;");
        tAddBtn.setOnAction(e -> {
            if (tUsernameField.getText().trim().isEmpty() || tPasswordField.getText().trim().isEmpty()
                    || tEmployeeIdField.getText().trim().isEmpty()) {
                setStatus("用户名、密码、工号为必填项", true);
                return;
            }
            if (!tUsernameField.getText().trim().matches("^.{2,10}$")) {
                setStatus("用户名需2~10个字符", true);
                return;
            }
            if (!tPasswordField.getText().trim().matches("^(?=.*[a-zA-Z]).{8,13}$")) {
                setStatus("密码需8~13位且至少包含1个字母", true);
                return;
            }
            new Thread(() -> {
                boolean ok = AdminManager.addTeacher(
                        tUsernameField.getText().trim(), tPasswordField.getText().trim(),
                        tEmployeeIdField.getText().trim(),
                        tGenderField.getValue(), tTitleField.getText().trim(),
                        tDepartmentField.getText().trim());
                Platform.runLater(() -> {
                    setStatus(ok ? "添加成功" : "添加失败，用户名或工号可能已存在", !ok);
                    if (ok) { loadTeachers(); clearTeacherForm(); }
                });
            }).start();
        });

        tSaveBtn = new Button("保存修改");
        tSaveBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 13;");
        tSaveBtn.setVisible(false);
        tSaveBtn.setOnAction(e -> {
            if (tUsernameField.getText().trim().isEmpty()
                    || tEmployeeIdField.getText().trim().isEmpty()) {
                setStatus("用户名、工号为必填项（密码留空不修改）", true);
                return;
            }
            if (!tUsernameField.getText().trim().matches("^.{2,10}$")) {
                setStatus("用户名需2~10个字符", true);
                return;
            }
            if (!tPasswordField.getText().trim().isEmpty()
                    && !tPasswordField.getText().trim().matches("^(?=.*[a-zA-Z]).{8,13}$")) {
                setStatus("密码需8~13位且至少包含1个字母", true);
                return;
            }
            new Thread(() -> {
                boolean ok = AdminManager.updateTeacher(editingTeacherId,
                        tGenderField.getValue(),
                        tPasswordField.getText().trim().isEmpty() ? null : tPasswordField.getText().trim(),
                        tEmployeeIdField.getText().trim(), tTitleField.getText().trim(),
                        tDepartmentField.getText().trim());
                Platform.runLater(() -> {
                    setStatus(ok ? "修改成功" : "修改失败", !ok);
                    if (ok) { loadTeachers(); clearTeacherForm(); }
                });
            }).start();
        });

        tCancelBtn = new Button("取消编辑");
        tCancelBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 13;");
        tCancelBtn.setVisible(false);
        tCancelBtn.setOnAction(e -> clearTeacherForm());

        HBox btnBox = new HBox(10, tAddBtn, tSaveBtn, tCancelBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        btnBox.setPadding(new Insets(5, 0, 0, 0));

        panel.getChildren().addAll(addTitle, grid, btnBox);
        return panel;
    }

    private void populateTeacherForm(Teacher t) {
        editingTeacherId = t.getId();
        tUsernameField.setText(t.getUsername());
        tPasswordField.setText("");
        tPasswordField.setPromptText("留空则不修改密码");
        tEmployeeIdField.setText(t.getEmployeeId());
        tGenderField.setValue(t.getGender() != null ? t.getGender() : null);
        tTitleField.setText(t.getTitle() != null ? t.getTitle() : "");
        tDepartmentField.setText(t.getDepartment() != null ? t.getDepartment() : "");
        tUsernameField.setEditable(false);
        tAddBtn.setVisible(false);
        tSaveBtn.setVisible(true);
        tCancelBtn.setVisible(true);
    }

    private void clearTeacherForm() {
        editingTeacherId = -1;
        tUsernameField.clear();
        tUsernameField.setEditable(true);
        tPasswordField.clear();
        tPasswordField.setPromptText("密码");
        tEmployeeIdField.clear();
        tGenderField.setValue(null);
        tTitleField.clear();
        tDepartmentField.clear();
        tAddBtn.setVisible(true);
        tSaveBtn.setVisible(false);
        tCancelBtn.setVisible(false);
    }

    private void loadStudents() {
        new Thread(() -> {
            List<Student> list = AdminManager.getAllStudents();
            Platform.runLater(() -> {
                studentTable.getItems().clear();
                if (list != null && !list.isEmpty()) {
                    studentTable.getItems().addAll(list);
                }
                setStatus("已加载 " + (list != null ? list.size() : 0) + " 名学生", false);
            });
        }).start();
    }

    private void loadTeachers() {
        new Thread(() -> {
            List<Teacher> list = AdminManager.getAllTeachers();
            Platform.runLater(() -> {
                teacherTable.getItems().clear();
                if (list != null && !list.isEmpty()) {
                    teacherTable.getItems().addAll(list);
                }
                setStatus("已加载 " + (list != null ? list.size() : 0) + " 名教师", false);
            });
        }).start();
    }

    private void setStatus(String text, boolean isError) {
        statusLabel.setText(text);
        statusLabel.setStyle(isError
                ? "-fx-font-size: 12; -fx-text-fill: #e74c3c;"
                : "-fx-font-size: 12; -fx-text-fill: #27ae60;");
    }
}
