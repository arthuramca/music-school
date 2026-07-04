package com.arthas.musicschool.service;

import com.arthas.musicschool.model.FinancialReport;
import com.arthas.musicschool.model.FinancialReportItem;
import com.arthas.musicschool.repository.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FinancialReportService {

    public FinancialReport buildReport(String month) throws SQLException {
        Connection conn = DatabaseManager.getConnection();

        // Cruza alunos ativos com pagamentos do mês (LEFT JOIN)
        String sql = """
            SELECT s.id, s.name, s.instrument, s.monthly_fee,
                   COALESCE(p.amount, 0)   AS amount_paid,
                   COALESCE(p.status, 'Sem registro') AS pay_status,
                   COALESCE(p.paid_date, '') AS paid_date
            FROM students s
            LEFT JOIN payments p
                   ON p.student_id = s.id AND p.reference_month = ?
            WHERE s.status = 'Ativo'
            ORDER BY s.name COLLATE NOCASE
            """;

        List<FinancialReportItem> items = new ArrayList<>();
        double totalPrevisto = 0;
        double totalApurado  = 0;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, month);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FinancialReportItem item = new FinancialReportItem();
                    item.setStudentId(rs.getInt("id"));
                    item.setStudentName(rs.getString("name"));
                    item.setInstrument(safe(rs.getString("instrument")));
                    item.setMonthlyFee(rs.getDouble("monthly_fee"));
                    item.setAmountPaid(rs.getDouble("amount_paid"));
                    item.setPaymentStatus(rs.getString("pay_status"));
                    item.setPaidDate(rs.getString("paid_date"));

                    totalPrevisto += item.getMonthlyFee();
                    if ("Pago".equals(item.getPaymentStatus()) || "Isento".equals(item.getPaymentStatus())) {
                        totalApurado += item.getAmountPaid();
                    }

                    items.add(item);
                }
            }
        }

        FinancialReport report = new FinancialReport();
        report.setMonth(month);
        report.setItems(items);
        report.setTotalPrevisto(totalPrevisto);
        report.setTotalApurado(totalApurado);
        report.setTotalPendente(totalPrevisto - totalApurado);
        return report;
    }

    private String safe(String v) { return v != null ? v : ""; }
}
