package com.arthas.musicschool.controller;

import com.arthas.musicschool.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private Label formTitle;
    @FXML private Label subtitleLabel;
    @FXML private Label errorLabel;
    @FXML private Label confirmLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Button loginBtn;

    private final AuthService authService = new AuthService();
    private boolean authenticated = false;

    @FXML
    public void initialize() {
        if (authService.isFirstRun()) {
            formTitle.setText("Configurar Acesso");
            subtitleLabel.setText("Primeiro acesso — defina seu usuário e senha");
            loginBtn.setText("Configurar");
            confirmLabel.setVisible(true);
            confirmLabel.setManaged(true);
            confirmField.setVisible(true);
            confirmField.setManaged(true);
        }
        passwordField.setOnAction(e -> onLogin());
    }

    @FXML
    void onLogin() {
        errorLabel.setText("");
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isBlank() || pass.isBlank()) {
            errorLabel.setText("Preencha usuário e senha.");
            return;
        }

        if (authService.isFirstRun()) {
            String confirm = confirmField.getText();
            if (!pass.equals(confirm)) { errorLabel.setText("As senhas não coincidem."); return; }
            if (pass.length() < 4)    { errorLabel.setText("Senha deve ter ao menos 4 caracteres."); return; }
            try {
                authService.setupInitialCredentials(user, pass);
                authenticated = true;
                close();
            } catch (Exception e) {
                errorLabel.setText("Erro ao salvar: " + e.getMessage());
            }
        } else {
            if (authService.authenticate(user, pass)) {
                authenticated = true;
                close();
            } else {
                errorLabel.setText("Usuário ou senha incorretos.");
                passwordField.clear();
                passwordField.requestFocus();
            }
        }
    }

    public boolean isAuthenticated() { return authenticated; }

    private void close() {
        ((Stage) loginBtn.getScene().getWindow()).close();
    }
}
