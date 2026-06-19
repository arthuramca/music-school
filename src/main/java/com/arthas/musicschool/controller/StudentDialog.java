package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.service.StudentService;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class StudentDialog extends Dialog<Student> {

    private final TextField nameField       = new TextField();
    private final TextField cpfField        = new TextField();
    private final DatePicker birthDatePicker = new DatePicker();
    private final TextField phoneField      = new TextField();
    private final TextField emailField      = new TextField();
    private final TextField addressField    = new TextField();
    private final ComboBox<String> instrumentBox = new ComboBox<>();
    private final ComboBox<String> levelBox = new ComboBox<>();
    private final TextField teacherField    = new TextField();
    private final DatePicker startDatePicker = new DatePicker();
    private final TextField feeField        = new TextField("0,00");
    private final TextField dueDayField     = new TextField("5");
    private final ComboBox<String> statusBox   = new ComboBox<>();
    private final ComboBox<String> dayBox       = new ComboBox<>();
    private final ComboBox<String> timeBox      = new ComboBox<>();
    private final ComboBox<String> instrumentBox2 = new ComboBox<>();
    private final ComboBox<String> dayBox2      = new ComboBox<>();
    private final ComboBox<String> timeBox2     = new ComboBox<>();
    private final TextArea notesArea            = new TextArea();
    private final ImageView photoPreview        = new ImageView();
    private final Label photoLabel              = new Label("Nenhuma foto");
    private final Label slotStatusLabel         = new Label();
    private final Label slotStatusLabel2        = new Label();
    private String photoPath = "";

    private static final String[] DAYS  = {"", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
    private static final String[] TIMES = {"", "08:00","09:00","10:00","11:00","12:00","13:00",
                                            "14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00"};
    private static final String[] INSTRUMENTS = {
        "Bateria", "Baixo", "Canto", "Cavaquinho", "Contrabaixo", "Flauta",
        "Guitarra", "Percussão", "Piano", "Saxofone", "Teclado", "Trompete",
        "Ukulele", "Violão", "Violino"
    };

    private final StudentService studentService = new StudentService();

    public StudentDialog(Student student, javafx.stage.Window owner) {
        if (owner != null) initOwner(owner);
        boolean isNew = student == null;
        setTitle(isNew ? "Novo Aluno" : "Editar Aluno");
        setHeaderText(isNew ? "Cadastrar novo aluno" : "Editar dados do aluno");

        ButtonType saveBtn = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        levelBox.getItems().addAll("Iniciante", "Intermediário", "Avançado");
        levelBox.setValue("Iniciante");
        statusBox.getItems().addAll("Ativo", "Inativo", "Trancado");
        statusBox.setValue("Ativo");
        instrumentBox.getItems().addAll(INSTRUMENTS);
        instrumentBox.setEditable(true);
        instrumentBox.setPromptText("Selecione ou digite...");
        dayBox.getItems().addAll(DAYS);
        dayBox.setValue("");
        timeBox.getItems().addAll(TIMES);
        timeBox.setValue("");
        dayBox.setOnAction(e  -> updateSlotStatus(student));
        timeBox.setOnAction(e -> updateSlotStatus(student));
        instrumentBox.valueProperty().addListener((obs, o, v) -> updateSlotStatus(student));
        instrumentBox2.getItems().addAll(INSTRUMENTS);
        instrumentBox2.setEditable(true);
        instrumentBox2.setPromptText("Opcional...");
        dayBox2.getItems().addAll(DAYS);  dayBox2.setValue("");
        timeBox2.getItems().addAll(TIMES); timeBox2.setValue("");
        dayBox2.setOnAction(e  -> updateSlotStatus2(student));
        timeBox2.setOnAction(e -> updateSlotStatus2(student));
        instrumentBox2.valueProperty().addListener((obs, o, v) -> updateSlotStatus2(student));
        notesArea.setPrefRowCount(2);
        notesArea.setWrapText(true);
        photoPreview.setFitWidth(70); photoPreview.setFitHeight(70); photoPreview.setPreserveRatio(true);

        getDialogPane().setContent(buildGrid());
        getDialogPane().setPrefWidth(540);

        if (!isNew) populate(student);

        Button ok = (Button) getDialogPane().lookupButton(saveBtn);
        ok.setDisable(nameField.getText().isBlank());
        nameField.textProperty().addListener((obs, o, v) -> ok.setDisable(v.isBlank()));

        // Valida slots antes de salvar
        ok.addEventFilter(ActionEvent.ACTION, event -> {
            int excludeId = student != null ? student.getId() : 0;
            if (!validateSlot(dayBox.getValue(), timeBox.getValue(), excludeId,
                    instrumentBox.getValue(), slotStatusLabel, event)) return;
            // Valida slot2 se preenchido
            String d2 = dayBox2.getValue(); String t2 = timeBox2.getValue();
            if (d2 != null && !d2.isBlank() && t2 != null && !t2.isBlank()) {
                validateSlot(d2, t2, excludeId, instrumentBox2.getValue(), slotStatusLabel2, event);
            }
        });

        setResultConverter(btn -> btn == saveBtn ? buildStudent(student) : null);
    }

    private GridPane buildGrid() {
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(9);
        g.setPadding(new Insets(20, 20, 10, 20));

        nameField.setPromptText("Nome completo (obrigatório)");
        cpfField.setPromptText("000.000.000-00");
        phoneField.setPromptText("(11) 99999-9999");
        emailField.setPromptText("email@exemplo.com");
        addressField.setPromptText("Rua, número, bairro...");
        teacherField.setPromptText("Nome do professor");

        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(emailField, Priority.ALWAYS);
        GridPane.setHgrow(addressField, Priority.ALWAYS);
        GridPane.setHgrow(instrumentBox, Priority.ALWAYS);
        GridPane.setHgrow(teacherField, Priority.ALWAYS);

        Button selPhoto = new Button("Selecionar foto...");
        selPhoto.setOnAction(e -> selectPhoto());
        Button clearPhoto = new Button("Remover");
        clearPhoto.setOnAction(e -> clearPhoto());
        photoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        HBox photoBox = new HBox(10, photoPreview, new VBox(4, new HBox(6, selPhoto, clearPhoto), photoLabel));
        photoBox.setAlignment(Pos.CENTER_LEFT);

        int r = 0;
        g.add(lbl("Nome *"),       0, r); g.add(nameField,       1, r++);
        g.add(lbl("CPF"),          0, r); g.add(cpfField,         1, r++);
        g.add(lbl("Nascimento"),   0, r); g.add(birthDatePicker,  1, r++);
        g.add(lbl("Telefone"),     0, r); g.add(phoneField,       1, r++);
        g.add(lbl("E-mail"),       0, r); g.add(emailField,       1, r++);
        g.add(lbl("Endereço"),     0, r); g.add(addressField,     1, r++);

        Separator sep = new Separator(); GridPane.setColumnSpan(sep, 2);
        g.add(sep, 0, r++);

        g.add(lbl("Instrumento"),  0, r); g.add(instrumentBox,    1, r++);
        g.add(lbl("Nível"),        0, r); g.add(levelBox,          1, r++);
        g.add(lbl("Professor"),    0, r); g.add(teacherField,      1, r++);
        g.add(lbl("Início"),       0, r); g.add(startDatePicker,   1, r++);

        Separator sep2 = new Separator(); GridPane.setColumnSpan(sep2, 2);
        g.add(sep2, 0, r++);

        HBox feeBox = new HBox(10, feeField, lbl("Vencimento (dia):"), dueDayField);
        HBox.setHgrow(feeField, Priority.ALWAYS);
        dueDayField.setMaxWidth(50);
        g.add(lbl("Mensalidade (R$)"), 0, r); g.add(feeBox,      1, r++);
        g.add(lbl("Status"),           0, r); g.add(statusBox,   1, r++);

        HBox slotBox = new HBox(8, dayBox, lbl("Horário:"), timeBox, slotStatusLabel);
        slotBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        dayBox.setPrefWidth(120); timeBox.setPrefWidth(90);
        slotStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");

        g.add(lbl("Dia da aula"),      0, r); g.add(slotBox,    1, r++);

        Separator sep3 = new Separator(); GridPane.setColumnSpan(sep3, 2);
        g.add(sep3, 0, r++);
        Label sec2 = new Label("2º Instrumento / Horário (opcional)");
        sec2.setStyle("-fx-font-weight: bold; -fx-text-fill: #7d3c98;");
        GridPane.setColumnSpan(sec2, 2);
        g.add(sec2, 0, r++);
        GridPane.setHgrow(instrumentBox2, Priority.ALWAYS);
        g.add(lbl("Instrumento 2"),    0, r); g.add(instrumentBox2, 1, r++);
        HBox slotBox2 = new HBox(8, dayBox2, lbl("Horário:"), timeBox2, slotStatusLabel2);
        slotBox2.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        dayBox2.setPrefWidth(120); timeBox2.setPrefWidth(90);
        slotStatusLabel2.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        g.add(lbl("Dia da aula 2"),    0, r); g.add(slotBox2,   1, r++);
        g.add(lbl("Observações"),      0, r); g.add(notesArea,   1, r++);
        g.add(lbl("Foto"),             0, r); g.add(photoBox,    1, r);

        return g;
    }

    private void selectPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar foto do aluno");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png","*.jpg","*.jpeg","*.gif","*.bmp"));
        File f = fc.showOpenDialog(getDialogPane().getScene().getWindow());
        if (f != null) {
            photoPath = f.getAbsolutePath();
            loadPreview(photoPath);
            photoLabel.setText(f.getName());
        }
    }

    private void clearPhoto() {
        photoPath = "";
        photoPreview.setImage(null);
        photoLabel.setText("Nenhuma foto");
    }

    private void loadPreview(String path) {
        try {
            if (path != null && !path.isBlank() && Files.exists(Paths.get(path)))
                photoPreview.setImage(new Image("file:" + path, 70, 70, true, true));
        } catch (Exception ignored) {}
    }

    private void populate(Student s) {
        nameField.setText(s.getName());
        cpfField.setText(safe(s.getCpf()));
        birthDatePicker.setValue(s.getBirthDate());
        phoneField.setText(safe(s.getPhone()));
        emailField.setText(safe(s.getEmail()));
        addressField.setText(safe(s.getAddress()));
        instrumentBox.setValue(safe(s.getInstrument()));
        levelBox.setValue(s.getLevel() != null ? s.getLevel() : "Iniciante");
        teacherField.setText(safe(s.getTeacher()));
        startDatePicker.setValue(s.getStartDate());
        feeField.setText(String.format("%.2f", s.getMonthlyFee()).replace('.', ','));
        dueDayField.setText(String.valueOf(s.getPaymentDueDay()));
        statusBox.setValue(s.getStatus() != null ? s.getStatus() : "Ativo");
        notesArea.setText(safe(s.getNotes()));
        photoPath = safe(s.getPhotoPath());
        if (!photoPath.isBlank()) {
            loadPreview(photoPath);
            photoLabel.setText(Paths.get(photoPath).getFileName().toString());
        }
        dayBox.setValue(s.getLessonDay()   != null ? s.getLessonDay()   : "");
        timeBox.setValue(s.getLessonTime() != null ? s.getLessonTime()  : "");
        instrumentBox2.setValue(safe(s.getInstrument2()));
        dayBox2.setValue(s.getLessonDay2()   != null ? s.getLessonDay2()   : "");
        timeBox2.setValue(s.getLessonTime2() != null ? s.getLessonTime2()  : "");
    }

    private Student buildStudent(Student existing) {
        Student s = existing != null ? existing : new Student();
        s.setName(nameField.getText().trim());
        s.setCpf(cpfField.getText().trim());
        s.setBirthDate(birthDatePicker.getValue());
        s.setPhone(phoneField.getText().trim());
        s.setEmail(emailField.getText().trim());
        s.setAddress(addressField.getText().trim());
        s.setInstrument(instrumentBox.getValue() != null ? instrumentBox.getValue().trim() : "");
        s.setLevel(levelBox.getValue());
        s.setTeacher(teacherField.getText().trim());
        s.setStartDate(startDatePicker.getValue());
        s.setMonthlyFee(parseDouble(feeField.getText()));
        s.setPaymentDueDay(parseInt(dueDayField.getText(), 5));
        s.setStatus(statusBox.getValue());
        s.setNotes(notesArea.getText().trim());
        s.setPhotoPath(photoPath);
        s.setLessonDay(dayBox.getValue() != null ? dayBox.getValue() : "");
        s.setLessonTime(timeBox.getValue() != null ? timeBox.getValue() : "");
        s.setInstrument2(instrumentBox2.getValue() != null ? instrumentBox2.getValue().trim() : "");
        s.setLessonDay2(dayBox2.getValue()   != null ? dayBox2.getValue()   : "");
        s.setLessonTime2(timeBox2.getValue() != null ? timeBox2.getValue()  : "");
        return s;
    }

    private void updateSlotStatus(Student current) {
        refreshSlotLabel(dayBox.getValue(), timeBox.getValue(),
            instrumentBox.getValue(), current, slotStatusLabel);
    }

    private void updateSlotStatus2(Student current) {
        refreshSlotLabel(dayBox2.getValue(), timeBox2.getValue(),
            instrumentBox2.getValue(), current, slotStatusLabel2);
    }

    private void refreshSlotLabel(String day, String time, String instr, Student current, Label label) {
        if (day == null || day.isBlank() || time == null || time.isBlank()) {
            label.setText(""); return;
        }
        try {
            int excludeId = current != null ? current.getId() : 0;
            if (isCantoIncompatible(instr, day, time, excludeId)) {
                label.setText("⚠ Apenas Canto");
                label.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
                return;
            }
            long count = studentService.countBySlot(day, time, excludeId);
            label.setText(switch ((int) count) {
                case 0  -> "✓ Livre";
                case 1  -> "1 aluno";
                case 2  -> "⚠ 2 alunos (lotado)";
                default -> "✗ Lotado (máx 3)";
            });
            label.setStyle("-fx-font-size: 11px; -fx-text-fill: " + switch ((int) count) {
                case 0  -> "#27ae60;";
                case 1  -> "#f39c12;";
                default -> "#e74c3c;";
            });
        } catch (Exception ignored) { label.setText(""); }
    }

    private boolean isCantoIncompatible(String instr, String day, String time, int excludeId) throws Exception {
        if (instr == null || !instr.trim().equalsIgnoreCase("Canto")) return false;
        List<String> instruments = studentService.findInstrumentsBySlot(day, time, excludeId);
        return instruments.stream().anyMatch(i -> !i.equalsIgnoreCase("Canto"));
    }

    private boolean validateSlot(String day, String time, int excludeId, String instr,
                                 Label statusLabel, javafx.event.Event event) {
        if (day == null || day.isBlank() || time == null || time.isBlank()) return true;
        try {
            if (isCantoIncompatible(instr, day, time, excludeId)) {
                statusLabel.setText("⚠ Horário reservado para alunos de Canto.");
                statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
                event.consume(); return false;
            }
            long count = studentService.countBySlot(day, time, excludeId);
            if (count >= 3) {
                statusLabel.setText("Horário lotado (máx 3 alunos).");
                event.consume(); return false;
            } else if (count == 2) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Este horário já tem 2 alunos. Deseja adicionar um 3º aluno?",
                    ButtonType.YES, ButtonType.NO);
                confirm.setTitle("Horário quase lotado");
                confirm.initOwner(getDialogPane().getScene().getWindow());
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.YES) {
                    event.consume(); return false;
                }
            }
        } catch (Exception ignored) {}
        return true;
    }

    private double parseDouble(String v) {
        try { return Double.parseDouble(v.replace(",", ".").replaceAll("[^\\d.]", "")); }
        catch (Exception e) { return 0.0; }
    }

    private int parseInt(String v, int def) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }

    private Label lbl(String t) {
        Label l = new Label(t); l.setStyle("-fx-font-weight: bold;"); return l;
    }

    private String safe(String v) { return v != null ? v : ""; }
}
