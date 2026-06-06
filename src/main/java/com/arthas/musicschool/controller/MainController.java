package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.service.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
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
import java.util.List;

public class MainController {

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

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        loadStudents();
        startClock();
        studentTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) onEditStudent();
        });
        Platform.runLater(() ->
            studentTable.getScene().setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.F11) onToggleFullscreen();
            })
        );
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
        new StudentDialog(null).showAndWait().ifPresent(student -> {
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
        new StudentDialog(selected).showAndWait().ifPresent(student -> {
            try {
                studentService.save(student);
                loadStudents();
            } catch (Exception e) { showError("Erro ao salvar aluno", e.getMessage()); }
        });
    }

    @FXML
    void onDeleteStudent() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um aluno para excluir."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Excluir \"" + selected.getName() + "\"?\nTodos os pagamentos e aulas serão removidos.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar Exclusão");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                studentService.delete(selected.getId());
                loadStudents();
            } catch (Exception e) { showError("Erro ao excluir aluno", e.getMessage()); }
        });
    }

    @FXML
    void onPayments() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um aluno para ver os pagamentos."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/arthas/musicschool/payment-view.fxml"));
            Stage stage = new Stage();
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
            int saved = 0;
            for (Student s : imported) {
                studentService.save(s);
                paymentService.generatePendingMonths(s);
                saved++;
            }
            showInfo("Importação concluída: " + saved + " aluno(s) importado(s).");
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
