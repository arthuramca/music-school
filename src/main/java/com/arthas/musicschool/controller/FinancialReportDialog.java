package com.arthas.musicschool.controller;

import com.arthas.musicschool.model.FinancialReport;
import com.arthas.musicschool.model.FinancialReportItem;
import com.arthas.musicschool.service.FinancialReportService;
import com.arthas.musicschool.service.PdfService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class FinancialReportDialog extends Dialog<Void> {

    private static final String[] MONTHS = {
        "Janeiro","Fevereiro","Março","Abril","Maio","Junho",
        "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"
    };

    private final ComboBox<String>  monthBox = new ComboBox<>();
    private final ComboBox<Integer> yearBox  = new ComboBox<>();
    private final Label lblPrevisto  = new Label("R$ 0,00");
    private final Label lblApurado   = new Label("R$ 0,00");
    private final Label lblPendente  = new Label("R$ 0,00");
    private final TableView<FinancialReportItem> table = new TableView<>();
    private final Button btnExport   = new Button("Exportar PDF");

    private final FinancialReportService service = new FinancialReportService();
    private final PdfService pdfService = new PdfService();
    private FinancialReport currentReport;

    public FinancialReportDialog(Window owner) {
        if (owner != null) initOwner(owner);
        setTitle("Relatório Financeiro Mensal");
        setHeaderText(null);
        setResizable(true);

        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().setContent(buildContent());
        getDialogPane().setPrefWidth(780);
        getDialogPane().setPrefHeight(580);

        // Pré-seleciona mês atual
        LocalDate today = LocalDate.now();
        monthBox.setValue(MONTHS[today.getMonthValue() - 1]);
        yearBox.setValue(today.getYear());
        loadReport();
    }

    private VBox buildContent() {
        // Seletor de mês/ano
        monthBox.getItems().addAll(MONTHS);
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 3; y <= currentYear + 1; y++) yearBox.getItems().add(y);

        Button btnGerar = new Button("Gerar");
        btnGerar.setStyle("-fx-background-color:#3498db; -fx-text-fill:white; -fx-font-weight:bold; -fx-cursor:hand;");
        btnGerar.setOnAction(e -> loadReport());

        HBox selector = new HBox(10, new Label("Mês:"), monthBox, new Label("Ano:"), yearBox, btnGerar);
        selector.setAlignment(Pos.CENTER_LEFT);
        selector.setPadding(new Insets(0, 0, 10, 0));

        // Cards de totais
        HBox cards = new HBox(16, buildCard("Total Previsto", lblPrevisto, "#2980b9"),
                                   buildCard("Total Apurado",  lblApurado,  "#27ae60"),
                                   buildCard("Total Pendente", lblPendente, "#e74c3c"));
        HBox.setHgrow(cards.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(cards.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(cards.getChildren().get(2), Priority.ALWAYS);

        // Tabela
        buildTable();

        // Botão exportar
        btnExport.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-font-weight:bold; -fx-cursor:hand;");
        btnExport.setDisable(true);
        btnExport.setOnAction(e -> exportPdf());
        HBox footer = new HBox(btnExport);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 0, 0, 0));

        VBox content = new VBox(12, selector, cards, table, footer);
        content.setPadding(new Insets(16));
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private VBox buildCard(String title, Label valueLabel, String color) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#7f8c8d; -fx-font-weight:bold;");
        valueLabel.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:" + color + ";");
        VBox card = new VBox(4, lbl, valueLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 20, 14, 20));
        card.setStyle("-fx-background-color:#f8f9fa; -fx-background-radius:8; -fx-border-color:#e9ecef; -fx-border-radius:8;");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<FinancialReportItem, String> colName = new TableColumn<>("Nome");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStudentName()));
        colName.setPrefWidth(200);

        TableColumn<FinancialReportItem, String> colInstr = new TableColumn<>("Instrumento");
        colInstr.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getInstrument()));
        colInstr.setPrefWidth(120);

        TableColumn<FinancialReportItem, String> colFee = new TableColumn<>("Mensalidade");
        colFee.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("R$ %.2f", c.getValue().getMonthlyFee())));
        colFee.setPrefWidth(110);
        colFee.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<FinancialReportItem, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentStatus()));
        colStatus.setPrefWidth(100);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "Pago"        -> "-fx-text-fill:#27ae60; -fx-font-weight:bold;";
                    case "Isento"      -> "-fx-text-fill:#2980b9; -fx-font-weight:bold;";
                    case "Atrasado"    -> "-fx-text-fill:#e74c3c; -fx-font-weight:bold;";
                    case "Sem registro"-> "-fx-text-fill:#95a5a6;";
                    default            -> "-fx-text-fill:#e67e22; -fx-font-weight:bold;";
                });
            }
        });

        TableColumn<FinancialReportItem, String> colPaid = new TableColumn<>("Valor Pago");
        colPaid.setCellValueFactory(c -> {
            double v = c.getValue().getAmountPaid();
            return new SimpleStringProperty(v > 0 ? String.format("R$ %.2f", v) : "—");
        });
        colPaid.setPrefWidth(110);
        colPaid.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<FinancialReportItem, String> colDate = new TableColumn<>("Data Pagamento");
        colDate.setCellValueFactory(c -> {
            String d = c.getValue().getPaidDate();
            return new SimpleStringProperty(d != null && !d.isBlank() ? d : "—");
        });
        colDate.setPrefWidth(120);

        table.getColumns().addAll(colName, colInstr, colFee, colStatus, colPaid, colDate);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Selecione um mês e clique em Gerar."));
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void loadReport() {
        int monthIdx = monthBox.getSelectionModel().getSelectedIndex() + 1;
        int year = yearBox.getValue();
        String month = String.format("%d-%02d", year, monthIdx);

        try {
            currentReport = service.buildReport(month);
            lblPrevisto.setText(String.format("R$ %.2f", currentReport.getTotalPrevisto()));
            lblApurado.setText(String.format("R$ %.2f",  currentReport.getTotalApurado()));
            lblPendente.setText(String.format("R$ %.2f", currentReport.getTotalPendente()));
            table.setItems(FXCollections.observableArrayList(currentReport.getItems()));
            btnExport.setDisable(false);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Erro ao gerar relatório: " + ex.getMessage()).showAndWait();
        }
    }

    private void exportPdf() {
        if (currentReport == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Salvar Relatório PDF");
        fc.setInitialFileName("relatorio_" + currentReport.getMonth() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(getDialogPane().getScene().getWindow());
        if (file == null) return;
        try {
            pdfService.generateFinancialReport(currentReport, file);
            new Alert(Alert.AlertType.INFORMATION, "PDF exportado com sucesso!\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Erro ao exportar PDF: " + ex.getMessage()).showAndWait();
        }
    }
}
