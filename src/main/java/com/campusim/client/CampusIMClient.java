package com.campusim.client;

import com.campusim.client.network.ServerConnector;
import com.campusim.client.ui.LoginView;
import com.campusim.client.ui.MainView;
import com.campusim.common.Message;
import com.campusim.common.MessageType;

import javafx.application.Application;
import javafx.stage.Stage;

public class CampusIMClient extends Application {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8888;

    private ServerConnector connector;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("校园即时通讯系统 - 登录");
        connector = new ServerConnector(DEFAULT_HOST, DEFAULT_PORT);

        LoginView loginView = new LoginView(primaryStage, connector, (username, role) -> {
            showMainView(primaryStage, username, role);
        });

        primaryStage.setScene(loginView.createScene());
        primaryStage.show();
    }

    private void showMainView(Stage stage, String username, String role) {
        stage.setTitle("校园即时通讯系统 - " + username);
        MainView mainView = new MainView(stage, connector, username, role);
        stage.setScene(mainView.createScene());
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.sendMessage(new Message(MessageType.LOGOUT_REQUEST));
            connector.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
