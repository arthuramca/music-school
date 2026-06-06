package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.Lesson;
import com.arthas.musicschool.model.Makeup;
import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.service.LessonService;
import com.arthas.musicschool.service.MakeupService;
import com.arthas.musicschool.service.StudentService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MakeupController {

    // Tabela de alunos com reposição pendente
    @FXML private TableView<Student>         studentTable;
    @FXML private TableColumn<Student, String>  colName;
    @FXML private TableColumn<Student, String>  colInstrument;
    @FXML private TableColumn<Student, String>  colTeacher;
    @FXML private TableColumn<Student, String>  colSlot;
    @FXML private TableColumn<Student, Number>  colPending;
    @FXML private Label countLabel;

    // Tabela de reposições agendadas
    @FXML private TableView<Makeup>          makeupTable;
    @FXML private TableColumn<Makeup, String>  mColName;
    @FXML private TableColumn<Makeup, String>  mColInstrument;
    @FXML private TableColumn<Makeup, String>  mColSlot;
    @FXML private TableColumn<Makeup, String>  mColDate;
    @FXML private TableColumn<Makeup, String>  mColNotes;
    @FXML private Label makeupCountLabel;

    private final StudentService studentService = new StudentService();
    private final MakeupService  makeupService  = new MakeupService();
    private final LessonService  lessonService  = new LessonService();

    private static final String[] DAYS  = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
    private static final String[] TIMES = {"08:00","09:00","10:00","11:00","12:00","13:00",
                                            "14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00"};

    @FXML
    public void initialize() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colInstrument.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getInstrument())));
        colTeacher.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getTeacher())));
        colSlot.setCellValueFactory(c -> {
            String d = safe(c.getValue().getLessonDay());
            String t = safe(c.getValue().getLessonTime());
            return new SimpleStringProperty(d.isBlank() ? "—" : d + " " + t);
        });
        colPending.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getMakeupPending()));
        colPending.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.intValue() + " pendente(s)");
                setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });

        mColName.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getStudentName())));
        mColInstrument.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getStudentInstrument())));
        mColSlot.setCellValueFactory(c -> {
            String d = safe(c.getValue().getDayOfWeek());
            String t = safe(c.getValue().getSlotTime());
            return new SimpleStringProperty(d.isBlank() ? "—" : d + " " + t);
        });
        mColDate.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getScheduledDate())));
        mColNotes.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getNotes())));

        load();
        loadMakeups();
    }

    private void load() {
        try {
            List<Student> pending = studentService.getAllStudents().stream()
                .filter(s -> s.getMakeupPending() > 0).toList();
            studentTable.setItems(FXCollections.observableArrayList(pending));
            countLabel.setText(pending.size() + " aluno(s) com reposição pendente");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void loadMakeups() {
        try {
            List<Makeup> makeups = makeupService.getAllPending();
            makeupTable.setItems(FXCollections.observableArrayList(makeups));
            makeupCountLabel.setText(makeups.size() + " reposição(ões) agendada(s)");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML void onRefresh() { load(); loadMakeups(); }

    @FXML
    void onSchedule() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um aluno para agendar a reposição."); return; }

        ComboBox<String> dayBox  = new ComboBox<>();
        ComboBox<String> timeBox = new ComboBox<>();
        dayBox.getItems().addAll(DAYS);
        timeBox.getItems().addAll(TIMES);
        dayBox.setPromptText("Dia da semana");
        timeBox.setPromptText("Horário");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField notesField  = new TextField();
        notesField.setPromptText("Observações...");
        Label slotStatus = new Label();
        slotStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");

        Runnable updateStatus = () -> {
            String d = dayBox.getValue(); String t = timeBox.getValue();
            if (d == null || t == null) { slotStatus.setText(""); return; }
            try {
                long total = studentService.countBySlot(d, t, 0) + makeupService.countBySlot(d, t);
                boolean cantoBloq = safe(selected.getInstrument()).equalsIgnoreCase("Canto")
                    && isCantoIncompatible(d, t);
                if (cantoBloq) {
                    slotStatus.setText("⚠ Apenas alunos de Canto neste horário");
                } else if (total >= 2) {
                    slotStatus.setText("✗ Horário lotado (máx 2 para reposição)");
                } else {
                    slotStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60;");
                    slotStatus.setText(total == 0 ? "✓ Livre" : "1 aluno — vaga disponível");
                }
            } catch (Exception ex) { slotStatus.setText(""); }
        };
        dayBox.setOnAction(e -> updateStatus.run());
        timeBox.setOnAction(e -> updateStatus.run());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(16));
        grid.add(new Label("Dia:"),    0, 0); grid.add(dayBox,     1, 0);
        grid.add(new Label("Horário:"),0, 1); grid.add(timeBox,    1, 1);
        grid.add(slotStatus,           1, 2);
        grid.add(new Label("Data:"),   0, 3); grid.add(datePicker, 1, 3);
        grid.add(new Label("Obs.:"),   0, 4); grid.add(notesField, 1, 4);

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.initOwner(studentTable.getScene().getWindow());
        dlg.setTitle("Agendar Reposição");
        dlg.setHeaderText("Aluno: " + selected.getName() + " — " + safe(selected.getInstrument()));
        dlg.getDialogPane().setContent(grid);
        ButtonType saveBtn = new ButtonType("Agendar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        Button saveButton = (Button) dlg.getDialogPane().lookupButton(saveBtn);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String d = dayBox.getValue(); String t = timeBox.getValue();
            if (d == null || d.isBlank() || t == null || t.isBlank()) {
                slotStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
                slotStatus.setText("Selecione o dia e horário."); event.consume(); return;
            }
            try {
                if (safe(selected.getInstrument()).equalsIgnoreCase("Canto") && isCantoIncompatible(d, t)) {
                    slotStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
                    slotStatus.setText("⚠ Apenas alunos de Canto neste horário."); event.consume(); return;
                }
                long total = studentService.countBySlot(d, t, 0) + makeupService.countBySlot(d, t);
                if (total >= 2) {
                    slotStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
                    slotStatus.setText("✗ Horário lotado (máx 2 para reposição)."); event.consume();
                }
            } catch (Exception ex) { event.consume(); }
        });

        Optional<ButtonType> result = dlg.showAndWait();
        if (result.isPresent() && result.get() == saveBtn) {
            try {
                String date = datePicker.getValue() != null ? datePicker.getValue().toString() : "";
                makeupService.schedule(selected.getId(), date,
                    dayBox.getValue(), timeBox.getValue(), notesField.getText().trim());
                studentService.adjustMakeupPending(selected.getId(), -1);
                load(); loadMakeups();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
            }
        }
    }

    @FXML
    void onConfirmPresence() {
        Makeup selected = makeupTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione uma reposição para confirmar presença."); return; }

        String dateStr = selected.getScheduledDate() != null && !selected.getScheduledDate().isBlank()
            ? selected.getScheduledDate() : LocalDate.now().toString();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Confirmar presença de " + selected.getStudentName() + " na reposição?\n\nData: " + dateStr,
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar Presença");
        confirm.initOwner(makeupTable.getScene().getWindow());
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                Lesson lesson = new Lesson(selected.getStudentId(), dateStr, true);
                lesson.setNotes("Reposição");
                lessonService.save(lesson);
                makeupService.markDone(selected.getId());
                load(); loadMakeups();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
            }
        });
    }

    @FXML
    void onZero() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um aluno para zerar o contador."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Zerar contador de reposições de " + selected.getName() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.initOwner(studentTable.getScene().getWindow());
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { studentService.zeroMakeupPending(selected.getId()); load(); }
            catch (Exception e) { new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait(); }
        });
    }

    @FXML
    void onDeleteMakeup() {
        Makeup selected = makeupTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione uma reposição para cancelar."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Cancelar a reposição de " + selected.getStudentName() + "?\n\nO contador será revertido.",
            ButtonType.YES, ButtonType.NO);
        confirm.initOwner(makeupTable.getScene().getWindow());
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                makeupService.delete(selected.getId());
                studentService.adjustMakeupPending(selected.getStudentId(), +1);
                load(); loadMakeups();
            } catch (Exception e) { new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait(); }
        });
    }

    private boolean isCantoIncompatible(String day, String time) throws Exception {
        for (Student s : studentService.findBySlot(day, time))
            if (!safe(s.getInstrument()).equalsIgnoreCase("Canto")) return true;
        for (Makeup m : makeupService.findBySlot(day, time))
            if (!safe(m.getStudentInstrument()).equalsIgnoreCase("Canto")) return true;
        return false;
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private String safe(String v) { return v != null ? v : ""; }
}
