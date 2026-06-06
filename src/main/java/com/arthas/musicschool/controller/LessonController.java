package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.Lesson;
import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.service.LessonService;
import com.arthas.musicschool.service.StudentService;
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

    private final LessonService   service        = new LessonService();
    private final StudentService  studentService = new StudentService();
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

            long thisMonth        = service.countLessonsInCurrentMonth(student.getId());
            long absencesThisMonth = service.countAbsencesInCurrentMonth(student.getId());
            int  consecutive      = service.countConsecutiveAbsences(student.getId());

            StringBuilder sb = new StringBuilder();
            sb.append("Frequência: ").append(service.getAttendanceRate(student.getId()));
            sb.append("  |  ").append(lessons.size()).append(" aulas");
            sb.append("  |  Mês atual: ").append(thisMonth).append("/4");
            if (absencesThisMonth > 0)
                sb.append("  |  ").append(absencesThisMonth).append(" falta(s) no mês");
            if (consecutive >= 2)
                sb.append("  |  ⚠ ").append(consecutive).append(" seguidas");
            attendanceLabel.setText(sb.toString());
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    void onRegisterLesson() {
        Dialog<Lesson> dlg = new Dialog<>();
        dlg.initOwner(lessonTable.getScene().getWindow());
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
                long prevTotal    = service.countLessonsInCurrentMonth(student.getId());
                long prevAbsences = service.countAbsencesInCurrentMonth(student.getId());

                service.save(lesson);
                loadLessons();

                if (!lesson.isAttended()) {
                    long newTotal   = prevTotal + 1;
                    long newAbs     = prevAbsences + 1;
                    long prevNeeded = Math.max(0, prevAbsences - Math.max(0, prevTotal - 4));
                    long newNeeded  = Math.max(0, newAbs - Math.max(0, newTotal - 4));
                    if (newNeeded > prevNeeded)
                        studentService.adjustMakeupPending(student.getId(), (int)(newNeeded - prevNeeded));
                }

                checkAbsenceAlert();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
            }
        });
    }

    private void checkAbsenceAlert() {
        try {
            long absencesThisMonth = service.countAbsencesInCurrentMonth(student.getId());
            int  consecutive       = service.countConsecutiveAbsences(student.getId());

            boolean monthlyAlert     = absencesThisMonth >= 3;
            boolean consecutiveAlert = consecutive >= 2;
            if (!monthlyAlert && !consecutiveAlert) return;

            long totalThisMonth = service.countLessonsInCurrentMonth(student.getId());
            String monthInfo = totalThisMonth >= 4
                ? "Meta de 4 aulas do mês já cumprida."
                : "Aulas este mês: " + totalThisMonth + "/4  —  faltam " + (4 - totalThisMonth) + ".";

            String header = monthlyAlert
                ? "⚠  " + student.getName() + " tem " + absencesThisMonth + " faltas no mês"
                : "⚠  " + student.getName() + " tem " + consecutive + " faltas consecutivas";

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(lessonTable.getScene().getWindow());
            alert.setTitle("Alerta de Faltas");
            alert.setHeaderText(header);
            alert.setContentText("Entre em contato e agende uma reposição.\n\n" + monthInfo);
            alert.getButtonTypes().setAll(new ButtonType("Ciente", ButtonBar.ButtonData.OK_DONE));
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Erro ao verificar faltas: " + e.getMessage());
        }
    }

    @FXML
    void onDeleteLesson() {
        Lesson selected = lessonTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Excluir aula do dia " + selected.getLessonDate() + "?", ButtonType.YES, ButtonType.NO);
        confirm.initOwner(lessonTable.getScene().getWindow());
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { service.delete(selected.getId()); loadLessons(); }
            catch (Exception e) { new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait(); }
        });
    }
}
