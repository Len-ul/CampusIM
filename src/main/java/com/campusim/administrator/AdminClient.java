package com.campusim.administrator;

import com.campusim.administrator.ui.AdminLoginView;
import com.campusim.administrator.ui.AdminMainView;
import javafx.application.Application;
import javafx.stage.Stage;

public class AdminClient extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("校园即时通讯 - 管理员后台");

        AdminLoginView loginView = new AdminLoginView(primaryStage, (account, name) -> {
            showMainView(primaryStage, account, name);
        });

        primaryStage.setScene(loginView.createScene());
        primaryStage.show();
    }

    private void showMainView(Stage stage, String account, String name) {
        stage.setTitle("校园即时通讯 - 管理员 [" + (name != null ? name : account) + "]");
        AdminMainView mainView = new AdminMainView(stage, account, name);
        stage.setScene(mainView.createScene());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
