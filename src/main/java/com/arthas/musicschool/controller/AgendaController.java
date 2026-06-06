package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.service.StudentService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;

import java.util.*;

public class AgendaController {

    @FXML private ScrollPane scrollPane;

    private static final String[] DAYS  = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
    private static final String[] TIMES = {
        "08:00","09:00","10:00","11:00","12:00","13:00",
        "14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00"
    };

    private final StudentService studentService = new StudentService();

    @FXML
    public void initialize() {
        buildGrid();
    }

    public void refresh() {
        buildGrid();
    }

    private void buildGrid() {
        try {
            List<Student> all = studentService.getAllStudents();

            // Montar mapa (dia, hora) -> lista de alunos
            Map<String, List<Student>> slotMap = new HashMap<>();
            for (Student s : all) {
                String day  = s.getLessonDay();
                String time = s.getLessonTime();
                if (day == null || day.isBlank() || time == null || time.isBlank()) continue;
                String key = day + "|" + time;
                slotMap.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }

            GridPane grid = new GridPane();
            grid.setHgap(2);
            grid.setVgap(2);
            grid.setPadding(new Insets(10));

            // Coluna 0 (horários) + colunas 1-6 (dias)
            ColumnConstraints timeCol = new ColumnConstraints(70);
            grid.getColumnConstraints().add(timeCol);
            for (int d = 0; d < DAYS.length; d++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setPrefWidth(150);
                col.setHgrow(Priority.ALWAYS);
                grid.getColumnConstraints().add(col);
            }

            // Cabeçalho de dias
            grid.add(headerCell("Horário"), 0, 0);
            for (int d = 0; d < DAYS.length; d++) {
                grid.add(headerCell(DAYS[d]), d + 1, 0);
            }

            // Linhas de horários
            for (int t = 0; t < TIMES.length; t++) {
                grid.add(timeCell(TIMES[t]), 0, t + 1);
                for (int d = 0; d < DAYS.length; d++) {
                    String key = DAYS[d] + "|" + TIMES[t];
                    List<Student> inSlot = slotMap.getOrDefault(key, List.of());
                    grid.add(slotCell(inSlot), d + 1, t + 1);
                }
            }

            scrollPane.setContent(grid);
            scrollPane.setFitToWidth(true);
        } catch (Exception e) {
            Label err = new Label("Erro ao carregar agenda: " + e.getMessage());
            scrollPane.setContent(err);
        }
    }

    private Label headerCell(String text) {
        Label l = new Label(text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(6, 8, 6, 8));
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; " +
                   "-fx-font-weight: bold; -fx-font-size: 12px;");
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

    private VBox slotCell(List<Student> students) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(4, 6, 4, 6));
        box.setMinHeight(48);
        box.setMaxWidth(Double.MAX_VALUE);

        String bg;
        if (students.isEmpty()) {
            bg = "#f8f9fa";
        } else if (students.size() <= 2) {
            bg = "#d5f5e3"; // verde claro
        } else {
            bg = "#fdebd0"; // laranja claro
        }
        box.setStyle("-fx-background-color: " + bg + "; -fx-border-color: #dfe6e9; -fx-border-width: 1;");

        for (Student s : students) {
            Label name = new Label(s.getName());
            name.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            Label instr = new Label(s.getInstrument() != null ? s.getInstrument() : "");
            instr.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
            box.getChildren().addAll(name, instr);
        }

        if (students.size() >= 3) {
            Label warn = new Label("⚠ 3 alunos");
            warn.setStyle("-fx-font-size: 10px; -fx-text-fill: #e67e22; -fx-font-weight: bold;");
            box.getChildren().add(warn);
        }

        return box;
    }
}
