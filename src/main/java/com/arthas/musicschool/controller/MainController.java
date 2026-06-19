package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.WaitlistEntry;
import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.service.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private MenuBar appMenuBar;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> instrumentFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, String> colPhoto;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colInstrument;
    @FXML private TableColumn<Student, String> colLevel;
    @FXML private TableColumn<Student, String> colTeacher;
    @FXML private TableColumn<Student, String> colStatus;
    @FXML private TableColumn<Student, String> colFee;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    @FXML private Label clockLabel;

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");

    private final StudentService     studentService     = new StudentService();
    private final PaymentService     paymentService     = new PaymentService();
    private final SpreadsheetService spreadsheetService = new SpreadsheetService();
    private final BackupService      backupService      = new BackupService();
    private final PdfService         pdfService         = new PdfService();
    private final WaitlistService    waitlistService    = new WaitlistService();
    private final AuthService        authService        = new AuthService();

    private boolean updatingStudents = false;

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        loadStudents();
        startClock();
        setupContextMenu();
        transformMenuItems();
        studentTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) onEditStudent();
        });
        Platform.runLater(() ->
            studentTable.getScene().setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.F11) onToggleFullscreen();
            })
        );
    }

    private void transformMenuItems() {
        for (Menu menu : appMenuBar.getMenus()) {
            List<MenuItem> transformed = new ArrayList<>();
            for (MenuItem item : menu.getItems()) {
                if (item instanceof SeparatorMenuItem) { transformed.add(item); continue; }
                transformed.add(styledMenuItem(item.getText(), item.getOnAction()));
            }
            menu.getItems().setAll(transformed);
        }
    }

    private CustomMenuItem styledMenuItem(String text, javafx.event.EventHandler<ActionEvent> action) {
        boolean danger = text.contains("Excluir");
        String color   = danger ? "#e74c3c" : "#2c3e50";
        String hoverBg = danger ? "#c0392b" : "#3498db";
        Label lbl = new Label(text);
        lbl.setPrefWidth(210);
        lbl.setPadding(new Insets(7, 16, 7, 12));
        String normal = "-fx-text-fill:" + color + "; -fx-font-size:13px; -fx-background-color:transparent; -fx-cursor:hand;";
        String hover  = "-fx-text-fill:white; -fx-font-size:13px; -fx-background-color:" + hoverBg + "; -fx-cursor:hand;";
        lbl.setStyle(normal);
        lbl.setOnMouseEntered(e -> lbl.setStyle(hover));
        lbl.setOnMouseExited(e -> lbl.setStyle(normal));
        CustomMenuItem item = new CustomMenuItem(lbl, true);
        if (action != null) item.setOnAction(action);
        return item;
    }

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),6,0,0,3);");

        menu.getItems().addAll(
            ctxItem("✏   Editar",             "#2c3e50", "#3498db", e -> onEditStudent()),
            ctxItem("📋  Aulas",              "#2c3e50", "#3498db", e -> onLessons()),
            ctxItem("💰  Pagamentos",         "#2c3e50", "#3498db", e -> onPayments()),
            ctxItem("🔄  Agendar Reposição",  "#2c3e50", "#3498db", e -> openMakeupForSelected()),
            ctxItem("📄  Gerar PDF",          "#2c3e50", "#3498db", e -> onGeneratePdf()),
            new SeparatorMenuItem(),
            ctxItem("🗑  Excluir",            "#e74c3c", "#c0392b", e -> onDeleteStudent())
        );

        studentTable.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnContextMenuRequested(e -> {
                if (!row.isEmpty()) {
                    studentTable.getSelectionModel().select(row.getItem());
                    menu.show(row, e.getScreenX(), e.getScreenY());
                }
            });
            row.setOnMousePressed(e -> {
                if (menu.isShowing()) menu.hide();
            });
            return row;
        });
    }

    private CustomMenuItem ctxItem(String text, String color, String hoverBg,
                                   javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(210);
        lbl.setPadding(new Insets(7, 16, 7, 12));
        String normal = "-fx-text-fill:" + color + "; -fx-font-size:13px; -fx-background-color:transparent; -fx-cursor:hand;";
        String hover  = "-fx-text-fill:white; -fx-font-size:13px; -fx-background-color:" + hoverBg + "; -fx-cursor:hand;";
        lbl.setStyle(normal);
        lbl.setOnMouseEntered(e -> lbl.setStyle(hover));
        lbl.setOnMouseExited(e -> lbl.setStyle(normal));
        CustomMenuItem item = new CustomMenuItem(lbl, true);
        item.setOnAction(action);
        return item;
    }

    private void openMakeupForSelected() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/arthas/musicschool/makeup-view.fxml"));
            Stage stage = new Stage();
            stage.initOwner(studentTable.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Reposições — " + selected.getName());
            stage.setScene(new Scene(loader.load(), 780, 560));
            stage.showAndWait();
            loadStudents();
        } catch (Exception e) { showError("Erro ao abrir reposições", e.getMessage()); }
    }

    private void startClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT));
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1),
            e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void setupColumns() {
        colPhoto.setCellFactory(col -> new TableCell<>() {
            private final ImageView view = new ImageView();
            { view.setFitWidth(36); view.setFitHeight(36); view.setPreserveRatio(true); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setGraphic(null); return; }
                try {
                    if (Files.exists(Paths.get(item))) {
                        view.setImage(new Image("file:" + item, 36, 36, true, true));
                        setGraphic(view);
                    } else { setGraphic(null); }
                } catch (Exception ignored) { setGraphic(null); }
            }
        });
        colPhoto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhotoPath()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colInstrument.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getInstrument())));
        colLevel.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getLevel())));
        colTeacher.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getTeacher())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getStatus())));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "Ativo"    -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
                    case "Inativo"  -> "-fx-text-fill: #7f8c8d;";
                    case "Trancado" -> "-fx-text-fill: #e67e22;";
                    default         -> "";
                });
            }
        });
        colFee.setCellValueFactory(c -> new SimpleStringProperty(
            String.format("R$ %.2f", c.getValue().getMonthlyFee())));
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("Todos", "Ativo", "Inativo", "Trancado");
        statusFilter.setValue("Todos");
        instrumentFilter.setValue("Todos");
        statusFilter.setOnAction(e -> loadStudents());
        instrumentFilter.setOnAction(e -> loadStudents());
    }

    private void loadStudents() {
        if (updatingStudents) return;
        updatingStudents = true;
        try {
            List<String> instruments = studentService.getDistinctInstruments();
            String selInstr = instrumentFilter.getValue();
            instrumentFilter.getItems().setAll("Todos");
            instrumentFilter.getItems().addAll(instruments);
            instrumentFilter.setValue(selInstr != null && instruments.contains(selInstr) ? selInstr : "Todos");

            List<Student> students;
            String query = searchField.getText().trim();
            if (!query.isBlank()) {
                students = studentService.search(query);
            } else {
                students = studentService.getAllStudents();
            }

            String status = statusFilter.getValue();
            if (status != null && !status.equals("Todos"))
                students = students.stream().filter(s -> status.equals(s.getStatus())).toList();

            String instr = instrumentFilter.getValue();
            if (instr != null && !instr.equals("Todos"))
                students = students.stream().filter(s -> instr.equals(s.getInstrument())).toList();

            studentTable.setItems(FXCollections.observableArrayList(students));
            long active  = studentService.countActive();
            long pending = paymentService.countPending();
            countLabel.setText(students.size() + " alunos");
            statusLabel.setText("Ativos: " + active + "   |   Pendentes/Atrasados: " + pending);
        } catch (Exception e) {
            showError("Erro ao carregar alunos", e.getMessage());
        } finally {
            updatingStudents = false;
        }
    }

    @FXML void onSearch()      { loadStudents(); }
    @FXML void onClearSearch() { searchField.clear(); loadStudents(); }

    @FXML
    void onToggleFullscreen() {
        Stage stage = (Stage) studentTable.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    void onNewStudent() {
        new StudentDialog(null, studentTable.getScene().getWindow()).showAndWait().ifPresent(student -> {
            try {
                studentService.save(student);
                paymentService.generatePendingMonths(student);
                loadStudents();
            } catch (Exception e) { showError("Erro ao salvar aluno", e.getMessage()); }
        });
    }

    @FXML
    void onEditStudent() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um aluno para editar."); return; }
        String oldDay  = selected.getLessonDay();
        String oldTime = selected.getLessonTime();
        new StudentDialog(selected, studentTable.getScene().getWindow()).showAndWait().ifPresent(student -> {
            try {
                studentService.save(student);
                // Se o slot mudou, o slot antigo pode ter aberto vaga
                boolean dayChanged  = !eq(oldDay,  student.getLessonDay());
                boolean timeChanged = !eq(oldTime, student.getLessonTime());
                if (dayChanged || timeChanged) checkWaitlistForSlot(oldDay, oldTime);
                loadStudents();
            } catch (Exception e) { showError("Erro ao salvar aluno", e.getMessage()); }
        });
    }

    @FXML
    void onDeleteStudent() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um aluno para excluir."); return; }
        String day  = selected.getLessonDay();
        String time = selected.getLessonTime();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Excluir \"" + selected.getName() + "\"?\nTodos os pagamentos e aulas serão removidos.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar Exclusão");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                studentService.delete(selected.getId());
                loadStudents();
                checkWaitlistForSlot(day, time);
            } catch (Exception e) { showError("Erro ao excluir aluno", e.getMessage()); }
        });
    }

    @FXML
    void onMakeup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/arthas/musicschool/makeup-view.fxml"));
            Stage stage = new Stage();
            stage.initOwner(studentTable.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Reposições");
            stage.setScene(new Scene(loader.load(), 780, 500));
            stage.showAndWait();
        } catch (Exception e) { showError("Erro ao abrir reposições", e.getMessage()); }
    }

    @FXML
    void onAgenda() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/arthas/musicschool/agenda-view.fxml"));
            Stage stage = new Stage();
            stage.initOwner(studentTable.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Agenda Semanal");
            stage.setScene(new Scene(loader.load(), 980, 600));
            stage.show();
        } catch (Exception e) { showError("Erro ao abrir agenda", e.getMessage()); }
    }

    @FXML
    void onWaitlist() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/arthas/musicschool/waitlist-view.fxml"));
            Stage stage = new Stage();
            stage.initOwner(studentTable.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Fila de Espera");
            stage.setScene(new Scene(loader.load(), 750, 500));
            stage.show();
        } catch (Exception e) { showError("Erro ao abrir fila de espera", e.getMessage()); }
    }

    private void checkWaitlistForSlot(String day, String time) {
        if (day == null || day.isBlank() || time == null || time.isBlank()) return;
        try {
            long remaining = studentService.countBySlot(day, time, 0);
            if (remaining >= 2) return; // ainda lotado
            List<WaitlistEntry> fila = waitlistService.findBySlot(day, time);
            if (fila.isEmpty()) return;
            WaitlistEntry w = fila.get(0);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Vaga disponível!");
            alert.setHeaderText("Horário livre: " + day + " às " + time);
            alert.setContentText(
                "Aluno na fila de espera:\n\n" +
                "Nome:        " + w.getName() + "\n" +
                "Telefone:    " + safe(w.getPhone()) + "\n" +
                "E-mail:      " + safe(w.getEmail()) + "\n" +
                "Instrumento: " + safe(w.getInstrument()) + "\n\n" +
                (fila.size() > 1 ? "Total na fila para este horário: " + fila.size() + " aluno(s)." : "")
            );
            alert.initOwner(studentTable.getScene().getWindow());
            alert.showAndWait();
        } catch (Exception ignored) {}
    }

    private boolean eq(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.equals(b);
    }

    @FXML
    void onPayments() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um aluno para ver os pagamentos."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/arthas/musicschool/payment-view.fxml"));
            Stage stage = new Stage();
            stage.initOwner(studentTable.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Pagamentos — " + selected.getName());
            stage.setScene(new Scene(loader.load(), 700, 500));
            PaymentController ctrl = loader.getController();
            ctrl.setStudent(selected);
            stage.showAndWait();
            loadStudents();
        } catch (Exception e) { showError("Erro ao abrir pagamentos", e.getMessage()); }
    }

    @FXML
    void onLessons() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um aluno para ver as aulas."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/arthas/musicschool/lesson-view.fxml"));
            Stage stage = new Stage();
            stage.initOwner(studentTable.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Aulas — " + selected.getName());
            stage.setScene(new Scene(loader.load(), 600, 450));
            LessonController ctrl = loader.getController();
            ctrl.setStudent(selected);
            stage.showAndWait();
        } catch (Exception e) { showError("Erro ao abrir aulas", e.getMessage()); }
    }

    @FXML
    void onCharts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/arthas/musicschool/chart-view.fxml"));
            Stage stage = new Stage();
            stage.initOwner(studentTable.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Gráficos");
            stage.setScene(new Scene(loader.load(), 750, 500));
            stage.show();
        } catch (Exception e) { showError("Erro ao abrir gráficos", e.getMessage()); }
    }

    @FXML
    void onImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar lista de alunos");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Planilha Excel", "*.xlsx"));
        File file = chooser.showOpenDialog(studentTable.getScene().getWindow());
        if (file == null) return;
        try {
            List<Student> imported = spreadsheetService.importFromXlsx(file);
            int inserted = 0, updated = 0;
            for (Student s : imported) {
                boolean isNew = s.getId() == 0 || !studentService.existsById(s.getId());
                if (isNew) s.setId(0);
                studentService.save(s);
                if (isNew) { paymentService.generatePendingMonths(s); inserted++; }
                else updated++;
            }
            showInfo("Importação concluída: " + inserted + " novo(s) | " + updated + " atualizado(s).");
            loadStudents();
        } catch (Exception e) { showError("Erro ao importar", e.getMessage()); }
    }

    @FXML
    void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar lista de alunos");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Planilha Excel", "*.xlsx"));
        chooser.setInitialFileName("alunos.xlsx");
        File file = chooser.showSaveDialog(studentTable.getScene().getWindow());
        if (file == null) return;
        try {
            spreadsheetService.exportToXlsx(studentService.getAllStudents(), file);
            showInfo("Exportado com sucesso: " + file.getName());
        } catch (Exception e) { showError("Erro ao exportar", e.getMessage()); }
    }

    @FXML
    void onGeneratePdf() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um aluno para gerar a ficha."); return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar ficha cadastral em PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("ficha_" + selected.getName().replaceAll("\\s+", "_") + ".pdf");
        File file = chooser.showSaveDialog(studentTable.getScene().getWindow());
        if (file == null) return;
        try {
            pdfService.generateStudentCard(selected, file);
            showInfo("PDF gerado: " + file.getName());
        } catch (Exception e) { showError("Erro ao gerar PDF", e.getMessage()); }
    }

    @FXML
    void onBackup() {
        try {
            File dest = backupService.backupToOneDrive();
            showInfo("Backup salvo no OneDrive:\n" + dest.getName());
        } catch (Exception ex) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Escolher pasta para backup");
            File dir = chooser.showDialog(studentTable.getScene().getWindow());
            if (dir == null) return;
            try {
                File dest = backupService.backupToDirectory(dir);
                showInfo("Backup salvo em:\n" + dest.getAbsolutePath());
            } catch (Exception e2) { showError("Erro ao fazer backup", e2.getMessage()); }
        }
    }

    @FXML
    void onDeduplicate() {
        try {
            int count = studentService.deduplicateStudents();
            if (count == 0) {
                showInfo("Nenhum aluno duplicado encontrado.");
            } else {
                showInfo(count + " aluno(s) duplicado(s) removido(s).\nPagamentos e aulas associados foram excluídos automaticamente.");
                loadStudents();
            }
        } catch (Exception e) { showError("Erro ao deduplicar", e.getMessage()); }
    }

    @FXML
    void onChangePassword() {
        PasswordField currentPass = new PasswordField();
        currentPass.setPromptText("Senha atual");
        TextField newUser = new TextField(authService.getCurrentUsername());
        newUser.setPromptText("Novo usuário");
        PasswordField newPass    = new PasswordField();
        newPass.setPromptText("Nova senha");
        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Confirmar nova senha");
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");

        VBox content = new VBox(6,
            new Label("Senha atual:"), currentPass,
            new Label("Novo usuário:"), newUser,
            new Label("Nova senha:"), newPass,
            new Label("Confirmar nova senha:"), confirmPass,
            errorLabel
        );
        content.setPadding(new Insets(20));
        content.setPrefWidth(300);

        Dialog<Void> dlg = new Dialog<>();
        dlg.initOwner(studentTable.getScene().getWindow());
        dlg.setTitle("Alterar Acesso");
        dlg.setHeaderText("Alterar usuário e senha");
        dlg.getDialogPane().setContent(content);

        ButtonType saveBtn = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        Button saveButton = (Button) dlg.getDialogPane().lookupButton(saveBtn);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String cp   = currentPass.getText();
            String nu   = newUser.getText().trim();
            String np   = newPass.getText();
            String conf = confirmPass.getText();

            if (cp.isBlank())       { errorLabel.setText("Informe a senha atual.");              event.consume(); return; }
            if (nu.isBlank())       { errorLabel.setText("Usuário não pode ser vazio.");          event.consume(); return; }
            if (np.isBlank())       { errorLabel.setText("Nova senha não pode ser vazia.");       event.consume(); return; }
            if (!np.equals(conf))   { errorLabel.setText("As senhas não coincidem.");             event.consume(); return; }

            try {
                boolean ok = authService.changeCredentials(cp, nu, np);
                if (!ok) { errorLabel.setText("Senha atual incorreta."); event.consume(); }
            } catch (Exception ex) {
                errorLabel.setText("Erro ao salvar: " + ex.getMessage());
                event.consume();
            }
        });

        dlg.setResultConverter(bt -> null);
        dlg.showAndWait();
    }

    private void showError(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setTitle(title);
            a.showAndWait();
        });
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.showAndWait();
    }

    private String safe(String v) { return v != null ? v : ""; }
}
