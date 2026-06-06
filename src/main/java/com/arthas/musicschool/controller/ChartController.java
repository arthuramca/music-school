package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.service.StudentService;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChartController {

    @FXML private PieChart instrumentChart;
    @FXML private PieChart statusChart;
    @FXML private Label totalLabel;

    private final StudentService studentService = new StudentService();

    @FXML
    public void initialize() {
        try {
            List<Student> students = studentService.getAllStudents();
            loadInstrumentChart(students);
            loadStatusChart(students);
            totalLabel.setText("Total de alunos: " + students.size()
                + "   |   Ativos: " + students.stream().filter(s -> "Ativo".equals(s.getStatus())).count());
        } catch (Exception e) {
            totalLabel.setText("Erro ao carregar dados.");
        }
    }

    private void loadInstrumentChart(List<Student> students) {
        Map<String, Long> byInstrument = students.stream()
            .filter(s -> s.getInstrument() != null && !s.getInstrument().isBlank())
            .collect(Collectors.groupingBy(Student::getInstrument, Collectors.counting()));

        instrumentChart.getData().clear();
        byInstrument.forEach((instr, count) ->
            instrumentChart.getData().add(new PieChart.Data(instr + " (" + count + ")", count)));
    }

    private void loadStatusChart(List<Student> students) {
        Map<String, Long> byStatus = students.stream()
            .collect(Collectors.groupingBy(
                s -> s.getStatus() != null ? s.getStatus() : "Indefinido",
                Collectors.counting()));

        statusChart.getData().clear();
        byStatus.forEach((status, count) ->
            statusChart.getData().add(new PieChart.Data(status + " (" + count + ")", count)));
    }
}
