package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.Student;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StudentDialog extends Dialog<Student> {

    private final TextField nameField       = new TextField();
    private final TextField cpfField        = new TextField();
    private final DatePicker birthDatePicker = new DatePicker();
    private final TextField phoneField      = new TextField();
    private final TextField emailField      = new TextField();
    private final TextField addressField    = new TextField();
    private final TextField instrumentField = new TextField();
    private final ComboBox<String> levelBox = new ComboBox<>();
    private final TextField teacherField    = new TextField();
    private final DatePicker startDatePicker = new DatePicker();
    private final TextField feeField        = new TextField("0,00");
    private final TextField dueDayField     = new TextField("5");
    private final ComboBox<String> statusBox = new ComboBox<>();
    private final TextArea notesArea        = new TextArea();
    private final ImageView photoPreview    = new ImageView();
    private final Label photoLabel          = new Label("Nenhuma foto");
    private String photoPath = "";

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
        notesArea.setPrefRowCount(2);
        notesArea.setWrapText(true);
        photoPreview.setFitWidth(70); photoPreview.setFitHeight(70); photoPreview.setPreserveRatio(true);

        getDialogPane().setContent(buildGrid());
        getDialogPane().setPrefWidth(540);

        if (!isNew) populate(student);

        Button ok = (Button) getDialogPane().lookupButton(saveBtn);
        ok.setDisable(nameField.getText().isBlank());
        nameField.textProperty().addListener((obs, o, v) -> ok.setDisable(v.isBlank()));

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
        instrumentField.setPromptText("Violão, Piano, Bateria...");
        teacherField.setPromptText("Nome do professor");

        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(emailField, Priority.ALWAYS);
        GridPane.setHgrow(addressField, Priority.ALWAYS);
        GridPane.setHgrow(instrumentField, Priority.ALWAYS);
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

        g.add(lbl("Instrumento"),  0, r); g.add(instrumentField,  1, r++);
        g.add(lbl("Nível"),        0, r); g.add(levelBox,          1, r++);
        g.add(lbl("Professor"),    0, r); g.add(teacherField,      1, r++);
        g.add(lbl("Início"),       0, r); g.add(startDatePicker,   1, r++);

        Separator sep2 = new Separator(); GridPane.setColumnSpan(sep2, 2);
        g.add(sep2, 0, r++);

        HBox feeBox = new HBox(10, feeField, lbl("Vencimento (dia):"), dueDayField);
        HBox.setHgrow(feeField, Priority.ALWAYS);
        dueDayField.setMaxWidth(50);
        g.add(lbl("Mensalidade (R$)"), 0, r); g.add(feeBox,     1, r++);
        g.add(lbl("Status"),           0, r); g.add(statusBox,   1, r++);
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
        instrumentField.setText(safe(s.getInstrument()));
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
    }

    private Student buildStudent(Student existing) {
        Student s = existing != null ? existing : new Student();
        s.setName(nameField.getText().trim());
        s.setCpf(cpfField.getText().trim());
        s.setBirthDate(birthDatePicker.getValue());
        s.setPhone(phoneField.getText().trim());
        s.setEmail(emailField.getText().trim());
        s.setAddress(addressField.getText().trim());
        s.setInstrument(instrumentField.getText().trim());
        s.setLevel(levelBox.getValue());
        s.setTeacher(teacherField.getText().trim());
        s.setStartDate(startDatePicker.getValue());
        s.setMonthlyFee(parseDouble(feeField.getText()));
        s.setPaymentDueDay(parseInt(dueDayField.getText(), 5));
        s.setStatus(statusBox.getValue());
        s.setNotes(notesArea.getText().trim());
        s.setPhotoPath(photoPath);
        return s;
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
