package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.Lesson;
import com.arthas.musicschool.model.Makeup;
import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.service.LessonService;
import com.arthas.musicschool.service.MakeupService;
import com.arthas.musicschool.service.StudentService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.*;

public class AgendaController {

    @FXML private ScrollPane scrollPane;

    private static final String[] DAYS  = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
    private static final String[] TIMES = {
        "08:00","09:00","10:00","11:00","12:00","13:00",
        "14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00"
    };

    private final StudentService studentService = new StudentService();
    private final MakeupService  makeupService  = new MakeupService();
    private final LessonService  lessonService  = new LessonService();

    @FXML
    public void initialize() { buildGrid(); }

    public void refresh() { buildGrid(); }

    private void buildGrid() {
        try {
            List<Student> all = studentService.getAllStudents();

            Map<String, List<Student>> slotMap = new HashMap<>();
            for (Student s : all) {
                String day  = s.getLessonDay();
                String time = s.getLessonTime();
                if (day == null || day.isBlank() || time == null || time.isBlank()) continue;
                slotMap.computeIfAbsent(day + "|" + time, k -> new ArrayList<>()).add(s);
            }

            Map<String, List<Makeup>> makeupMap = new HashMap<>();
            for (String day : DAYS)
                for (String time : TIMES) {
                    List<Makeup> makeups = makeupService.findBySlot(day, time);
                    if (!makeups.isEmpty()) makeupMap.put(day + "|" + time, makeups);
                }

            GridPane grid = new GridPane();
            grid.setHgap(2); grid.setVgap(2); grid.setPadding(new Insets(10));

            grid.getColumnConstraints().add(new ColumnConstraints(70));
            for (int d = 0; d < DAYS.length; d++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setPrefWidth(150); col.setHgrow(Priority.ALWAYS);
                grid.getColumnConstraints().add(col);
            }

            grid.add(headerCell("Horário"), 0, 0);
            for (int d = 0; d < DAYS.length; d++)
                grid.add(headerCell(DAYS[d]), d + 1, 0);

            for (int t = 0; t < TIMES.length; t++) {
                grid.add(timeCell(TIMES[t]), 0, t + 1);
                for (int d = 0; d < DAYS.length; d++) {
                    String key = DAYS[d] + "|" + TIMES[t];
                    List<Student> inSlot  = slotMap.getOrDefault(key, List.of());
                    List<Makeup>  makeups = makeupMap.getOrDefault(key, List.of());
                    grid.add(slotCell(inSlot, makeups), d + 1, t + 1);
                }
            }

            scrollPane.setContent(grid);
            scrollPane.setFitToWidth(true);
        } catch (Exception e) {
            scrollPane.setContent(new Label("Erro ao carregar agenda: " + e.getMessage()));
        }
    }

    private Label headerCell(String text) {
        Label l = new Label(text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(6, 8, 6, 8));
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        return l;
    }

    private Label timeCell(String time) {
        Label l = new Label(time);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(6, 8, 6, 8));
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-background-color: #ecf0f1; -fx-font-weight: bold; -fx-font-size: 12px;");
        return l;
    }

    private VBox slotCell(List<Student> students, List<Makeup> makeups) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(4, 6, 4, 6));
        box.setMinHeight(48);
        box.setMaxWidth(Double.MAX_VALUE);

        int total = students.size() + makeups.size();
        String bg = total == 0 ? "#f8f9fa" : total <= 2 ? "#d5f5e3" : "#fdebd0";
        box.setStyle("-fx-background-color:" + bg + "; -fx-border-color: #dfe6e9; -fx-border-width: 1;");

        for (Student s : students) {
            VBox entry = new VBox(1);
            Label name  = new Label(s.getName());
            name.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-cursor: hand;");
            Label instr = new Label(s.getInstrument() != null ? s.getInstrument() : "");
            instr.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
            entry.getChildren().addAll(name, instr);

            ContextMenu menu = studentMenu(s);
            entry.setOnContextMenuRequested(e -> menu.show(entry, e.getScreenX(), e.getScreenY()));
            entry.setOnMouseClicked(e -> { if (e.getClickCount() == 2) openEditStudent(s); });
            entry.setStyle("-fx-cursor: hand;");
            box.getChildren().add(entry);
        }

        for (Makeup m : makeups) {
            VBox entry = new VBox(1);
            Label name  = new Label("(R) " + (m.getStudentName() != null ? m.getStudentName() : ""));
            name.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #7d3c98; -fx-cursor: hand;");
            Label instr = new Label(m.getStudentInstrument() != null ? m.getStudentInstrument() : "");
            instr.setStyle("-fx-font-size: 10px; -fx-text-fill: #a569bd;");
            entry.getChildren().addAll(name, instr);

            ContextMenu menu = makeupMenu(m);
            entry.setOnContextMenuRequested(e -> menu.show(entry, e.getScreenX(), e.getScreenY()));
            entry.setStyle("-fx-cursor: hand;");
            box.getChildren().add(entry);
        }

        if (total >= 3) {
            Label warn = new Label("⚠ " + total + " alunos");
            warn.setStyle("-fx-font-size: 10px; -fx-text-fill: #e67e22; -fx-font-weight: bold;");
            box.getChildren().add(warn);
        }

        return box;
    }

    private ContextMenu studentMenu(Student s) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 1; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),6,0,0,3);");
        menu.getItems().addAll(
            ctxItem("✏   Editar Horário",  "#2c3e50", "#3498db", e -> openEditStudent(s)),
            ctxItem("📋  Ver Aulas",       "#2c3e50", "#3498db", e -> openLessons(s))
        );
        return menu;
    }

    private ContextMenu makeupMenu(Makeup m) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 1; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),6,0,0,3);");
        menu.getItems().addAll(
            ctxItem("✓   Confirmar Presença",  "#27ae60", "#229954", e -> confirmPresence(m)),
            ctxItem("✗   Cancelar Reposição",  "#e74c3c", "#c0392b", e -> cancelMakeup(m))
        );
        return menu;
    }

    private CustomMenuItem ctxItem(String text, String color, String hoverBg,
                                   javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(200);
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

    private void openEditStudent(Student s) {
        new StudentDialog(s, scrollPane.getScene().getWindow()).showAndWait().ifPresent(updated -> {
            try {
                studentService.save(updated);
                buildGrid();
            } catch (Exception e) { showError("Erro ao salvar aluno: " + e.getMessage()); }
        });
    }

    private void openLessons(Student s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/arthas/musicschool/lesson-view.fxml"));
            Stage stage = new Stage();
            stage.initOwner(scrollPane.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Aulas — " + s.getName());
            stage.setScene(new Scene(loader.load(), 600, 450));
            LessonController ctrl = loader.getController();
            ctrl.setStudent(s);
            stage.showAndWait();
        } catch (Exception e) { showError("Erro ao abrir aulas: " + e.getMessage()); }
    }

    private void confirmPresence(Makeup m) {
        String dateStr = m.getScheduledDate() != null && !m.getScheduledDate().isBlank()
            ? m.getScheduledDate() : LocalDate.now().toString();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Confirmar presença de " + m.getStudentName() + " na reposição?\nData: " + dateStr,
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar Presença");
        confirm.initOwner(scrollPane.getScene().getWindow());
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                Lesson lesson = new Lesson(m.getStudentId(), dateStr, true);
                lesson.setNotes("Reposição");
                lessonService.save(lesson);
                makeupService.markDone(m.getId());
                buildGrid();
            } catch (Exception e) { showError("Erro ao confirmar presença: " + e.getMessage()); }
        });
    }

    private void cancelMakeup(Makeup m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Cancelar a reposição de " + m.getStudentName() + "?\nO contador será revertido.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Cancelar Reposição");
        confirm.initOwner(scrollPane.getScene().getWindow());
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                makeupService.delete(m.getId());
                studentService.adjustMakeupPending(m.getStudentId(), +1);
                buildGrid();
            } catch (Exception e) { showError("Erro ao cancelar reposição: " + e.getMessage()); }
        });
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
