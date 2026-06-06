package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.WaitlistEntry;
import com.arthas.musicschool.service.WaitlistService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class WaitlistController {

    @FXML private TableView<WaitlistEntry> waitlistTable;
    @FXML private TableColumn<WaitlistEntry, String> colName;
    @FXML private TableColumn<WaitlistEntry, String> colPhone;
    @FXML private TableColumn<WaitlistEntry, String> colInstrument;
    @FXML private TableColumn<WaitlistEntry, String> colSlot;
    @FXML private TableColumn<WaitlistEntry, String> colDate;
    @FXML private Label countLabel;

    private static final String[] DAYS  = {"", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
    private static final String[] TIMES = {"", "08:00","09:00","10:00","11:00","12:00","13:00",
                                            "14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00"};

    private final WaitlistService service = new WaitlistService();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colPhone.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getPhone())));
        colInstrument.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getInstrument())));
        colSlot.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlotLabel()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getRegisteredDate())));
        loadEntries();
    }

    private void loadEntries() {
        try {
            List<WaitlistEntry> entries = service.getAll();
            waitlistTable.setItems(FXCollections.observableArrayList(entries));
            countLabel.setText(entries.size() + " na fila");
        } catch (Exception e) {
            showError("Erro ao carregar fila", e.getMessage());
        }
    }

    @FXML
    void onAdd() {
        Dialog<WaitlistEntry> dlg = new Dialog<>();
        dlg.setTitle("Adicionar à Fila de Espera");
        dlg.initOwner(waitlistTable.getScene().getWindow());
        ButtonType saveBtn = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField nameField  = new TextField(); nameField.setPromptText("Nome completo*");
        TextField phoneField = new TextField(); phoneField.setPromptText("Telefone");
        TextField emailField = new TextField(); emailField.setPromptText("E-mail");
        TextField instrField = new TextField(); instrField.setPromptText("Instrumento de interesse");

        ComboBox<String> dayBox  = new ComboBox<>(); dayBox.getItems().addAll(DAYS);  dayBox.setValue("");
        ComboBox<String> timeBox = new ComboBox<>(); timeBox.getItems().addAll(TIMES); timeBox.setValue("");

        TextArea notesArea = new TextArea(); notesArea.setPromptText("Observações..."); notesArea.setPrefRowCount(2);

        VBox content = new VBox(8,
            lbl("Nome *"), nameField,
            lbl("Telefone"), phoneField,
            lbl("E-mail"), emailField,
            lbl("Instrumento"), instrField,
            lbl("Dia preferido"), dayBox,
            lbl("Horário preferido"), timeBox,
            lbl("Observações"), notesArea
        );
        content.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().setPrefWidth(380);

        Button ok = (Button) dlg.getDialogPane().lookupButton(saveBtn);
        ok.setDisable(true);
        nameField.textProperty().addListener((obs, o, v) -> ok.setDisable(v.isBlank()));

        dlg.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            WaitlistEntry w = new WaitlistEntry();
            w.setName(nameField.getText().trim());
            w.setPhone(phoneField.getText().trim());
            w.setEmail(emailField.getText().trim());
            w.setInstrument(instrField.getText().trim());
            w.setPreferredDay(dayBox.getValue() != null ? dayBox.getValue() : "");
            w.setPreferredTime(timeBox.getValue() != null ? timeBox.getValue() : "");
            w.setRegisteredDate(LocalDate.now().toString());
            w.setNotes(notesArea.getText().trim());
            return w;
        });

        dlg.showAndWait().ifPresent(w -> {
            try { service.save(w); loadEntries(); }
            catch (Exception e) { showError("Erro ao salvar", e.getMessage()); }
        });
    }

    @FXML
    void onDelete() {
        WaitlistEntry selected = waitlistTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um aluno da fila para remover."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Remover \"" + selected.getName() + "\" da fila de espera?",
            ButtonType.YES, ButtonType.NO);
        confirm.initOwner(waitlistTable.getScene().getWindow());
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { service.delete(selected.getId()); loadEntries(); }
            catch (Exception e) { showError("Erro ao remover", e.getMessage()); }
        });
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private String safe(String v) { return v != null ? v : ""; }
}
