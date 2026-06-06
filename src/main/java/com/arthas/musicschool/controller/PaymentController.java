package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.Payment;
import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.service.PaymentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PaymentController {

    @FXML private Label studentNameLabel;
    @FXML private Label summaryLabel;
    @FXML private TableView<Payment> paymentTable;
    @FXML private TableColumn<Payment, String> colMonth;
    @FXML private TableColumn<Payment, String> colAmount;
    @FXML private TableColumn<Payment, String> colStatus;
    @FXML private TableColumn<Payment, String> colPaidDate;
    @FXML private TableColumn<Payment, String> colNotes;

    private final PaymentService service = new PaymentService();
    private Student student;

    @FXML
    public void initialize() {
        colMonth.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMonthLabel()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(String.format("R$ %.2f", c.getValue().getAmount())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "Pago"     -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
                    case "Atrasado" -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
                    default         -> "-fx-text-fill: #e67e22;";
                });
            }
        });
        colPaidDate.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getPaidDate())));
        colNotes.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getNotes())));
    }

    public void setStudent(Student student) {
        this.student = student;
        studentNameLabel.setText(student.getName() + " — " + safe(student.getInstrument()));
        loadPayments();
    }

    private void loadPayments() {
        try {
            List<Payment> payments = service.getPayments(student.getId());
            paymentTable.setItems(FXCollections.observableArrayList(payments));
            long paid = payments.stream().filter(p -> "Pago".equals(p.getStatus())).count();
            long pending = payments.stream().filter(p -> !"Pago".equals(p.getStatus())).count();
            summaryLabel.setText("Pagos: " + paid + "   |   Pendentes/Atrasados: " + pending);
        } catch (Exception e) {
            showError("Erro ao carregar pagamentos", e.getMessage());
        }
    }

    @FXML
    void onMarkPaid() {
        Payment selected = paymentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Selecione um mês para registrar o pagamento."); return; }
        if ("Pago".equals(selected.getStatus())) { showInfo("Este mês já está marcado como pago."); return; }

        TextInputDialog dlg = new TextInputDialog(String.format("%.2f", selected.getAmount()).replace('.', ','));
        dlg.setTitle("Registrar Pagamento");
        dlg.setHeaderText("Mês: " + selected.getMonthLabel());
        dlg.setContentText("Valor recebido (R$):");
        Optional<String> result = dlg.showAndWait();
        result.ifPresent(val -> {
            try {
                double amount = Double.parseDouble(val.replace(",", ".").replaceAll("[^\\d.]", ""));
                service.markAsPaid(student.getId(), selected.getReferenceMonth(), amount);
                loadPayments();
            } catch (Exception e) { showError("Erro ao registrar pagamento", e.getMessage()); }
        });
    }

    @FXML
    void onGenerateMonths() {
        try {
            service.generatePendingMonths(student);
            loadPayments();
        } catch (Exception e) { showError("Erro ao gerar meses", e.getMessage()); }
    }

    @FXML
    void onDelete() {
        Payment selected = paymentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Remover registro do mês " + selected.getMonthLabel() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { service.delete(selected.getId()); loadPayments(); }
            catch (Exception e) { showError("Erro ao excluir", e.getMessage()); }
        });
    }

    private void showError(String title, String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
    private String safe(String v) { return v != null ? v : ""; }
}
