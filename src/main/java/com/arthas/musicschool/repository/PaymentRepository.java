package com.arthas.musicschool.repository;

import com.arthas.musicschool.model.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentRepository {

    private final Connection connection;

    public PaymentRepository() {
        try { this.connection = DatabaseManager.getConnection(); }
        catch (SQLException e) { throw new RuntimeException("Falha ao conectar ao banco", e); }
    }

    public PaymentRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Payment> findByStudent(int studentId) throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE student_id=? ORDER BY reference_month DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Payment findByStudentAndMonth(int studentId, String month) throws SQLException {
        String sql = "SELECT * FROM payments WHERE student_id=? AND reference_month=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setString(2, month);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public long countByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payments WHERE status=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    public Payment save(Payment p) throws SQLException {
        return p.getId() == 0 ? insert(p) : update(p);
    }

    private Payment insert(Payment p) throws SQLException {
        String sql = "INSERT INTO payments (student_id, reference_month, amount, paid_date, status, notes) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(stmt, p);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
        }
        return p;
    }

    private Payment update(Payment p) throws SQLException {
        String sql = "UPDATE payments SET student_id=?, reference_month=?, amount=?, paid_date=?, status=?, notes=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bind(stmt, p);
            stmt.setInt(7, p.getId());
            stmt.executeUpdate();
        }
        return p;
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM payments WHERE id=?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private void bind(PreparedStatement stmt, Payment p) throws SQLException {
        stmt.setInt(1,    p.getStudentId());
        stmt.setString(2, p.getReferenceMonth());
        stmt.setDouble(3, p.getAmount());
        stmt.setString(4, safe(p.getPaidDate()));
        stmt.setString(5, safe(p.getStatus()));
        stmt.setString(6, safe(p.getNotes()));
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("id"));
        p.setStudentId(rs.getInt("student_id"));
        p.setReferenceMonth(rs.getString("reference_month"));
        p.setAmount(rs.getDouble("amount"));
        p.setPaidDate(rs.getString("paid_date"));
        p.setStatus(rs.getString("status"));
        p.setNotes(rs.getString("notes"));
        return p;
    }

    private String safe(String v) { return v != null ? v : ""; }
}
