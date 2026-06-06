package com.arthas.musicschool;

import com.arthas.musicschool.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loginLoader = new FXMLLoader(App.class.getResource("login-view.fxml"));
        Stage loginStage = new Stage();
        loginStage.setTitle("Music School — Login");
        loginStage.setScene(new Scene(loginLoader.load(), 500, 430));
        loginStage.setResizable(false);
        loginStage.showAndWait();

        LoginController loginCtrl = loginLoader.getController();
        if (!loginCtrl.isAuthenticated()) return;

        FXMLLoader loader = new FXMLLoader(App.class.getResource("main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 650);
        primaryStage.setTitle("Music School — Gestão de Alunos");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.setFullScreenExitHint("Pressione ESC para sair da tela cheia");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
