package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.Lesson;
import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.service.LessonService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.List;

public class LessonController {

    @FXML private Label studentNameLabel;
    @FXML private Label attendanceLabel;
    @FXML private TableView<Lesson> lessonTable;
    @FXML private TableColumn<Lesson, String> colDate;
    @FXML private TableColumn<Lesson, String> colAttended;
    @FXML private TableColumn<Lesson, String> colNotes;

    private final LessonService service = new LessonService();
    private Student student;

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLessonDate()));
        colAttended.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAttendedLabel()));
        colAttended.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Presente".equals(item)
                    ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                    : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
        colNotes.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getNotes() != null ? c.getValue().getNotes() : ""));
    }

    public void setStudent(Student student) {
        this.student = student;
        studentNameLabel.setText(student.getName() + " — " +
            (student.getInstrument() != null ? student.getInstrument() : ""));
        loadLessons();
    }

    private void loadLessons() {
        try {
            List<Lesson> lessons = service.getLessons(student.getId());
            lessonTable.setItems(FXCollections.observableArrayList(lessons));

            long thisMonth   = service.countLessonsInCurrentMonth(student.getId());
            int  consecutive = service.countConsecutiveAbsences(student.getId());

            StringBuilder sb = new StringBuilder();
            sb.append("Frequência: ").append(service.getAttendanceRate(student.getId()));
            sb.append("  |  ").append(lessons.size()).append(" aulas");
            sb.append("  |  Mês atual: ").append(thisMonth).append("/4");
            if (consecutive >= 2) sb.append("  |  ⚠ ").append(consecutive).append(" faltas seguidas");
            attendanceLabel.setText(sb.toString());
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    void onRegisterLesson() {
        Dialog<Lesson> dlg = new Dialog<>();
        dlg.setTitle("Registrar Aula");
        dlg.setHeaderText("Aluno: " + student.getName());
        ButtonType saveBtn = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        DatePicker datePicker  = new DatePicker(LocalDate.now());
        CheckBox   presentCheck = new CheckBox("Presente");
        presentCheck.setSelected(true);
        TextField notesField = new TextField();
        notesField.setPromptText("Observações da aula...");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10,
            new Label("Data:"), datePicker,
            presentCheck,
            new Label("Observações:"), notesField);
        box.setPadding(new javafx.geometry.Insets(20));
        dlg.getDialogPane().setContent(box);

        dlg.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            Lesson l = new Lesson(student.getId(),
                datePicker.getValue() != null ? datePicker.getValue().toString() : LocalDate.now().toString(),
                presentCheck.isSelected());
            l.setNotes(notesField.getText().trim());
            return l;
        });

        dlg.showAndWait().ifPresent(lesson -> {
            try {
                service.save(lesson);
                loadLessons();
                checkAbsenceAlert();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
            }
        });
    }

    private void checkAbsenceAlert() {
        try {
            int consecutive = service.countConsecutiveAbsences(student.getId());
            if (consecutive < 2) return;

            long thisMonth = service.countLessonsInCurrentMonth(student.getId());
            String monthInfo = thisMonth >= 4
                ? "Meta de 4 aulas do mês já cumprida."
                : "Aulas este mês: " + thisMonth + "/4  —  faltam " + (4 - thisMonth) + ".";

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Alerta de Faltas");
            alert.setHeaderText("⚠  " + student.getName() + " acumulou " + consecutive + " faltas seguidas");
            alert.setContentText("Entre em contato para agendar reposição.\n\n" + monthInfo);
            alert.showAndWait();
        } catch (Exception ignored) {}
    }

    @FXML
    void onDeleteLesson() {
        Lesson selected = lessonTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Excluir aula do dia " + selected.getLessonDate() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { service.delete(selected.getId()); loadLessons(); }
            catch (Exception e) { new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait(); }
        });
    }
}
