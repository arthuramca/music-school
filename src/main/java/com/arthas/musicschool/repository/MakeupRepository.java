package com.arthas.musicschool.repository;

import com.arthas.musicschool.model.Makeup;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MakeupRepository {

    private final Connection connection;

    public MakeupRepository() {
        try { this.connection = DatabaseManager.getConnection(); }
        catch (SQLException e) { throw new RuntimeException("Falha ao conectar ao banco", e); }
    }

    public List<Makeup> findByStudent(int studentId) throws SQLException {
        List<Makeup> list = new ArrayList<>();
        String sql = "SELECT * FROM makeups WHERE student_id=? ORDER BY scheduled_date DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Makeup> findAllPending() throws SQLException {
        List<Makeup> list = new ArrayList<>();
        String sql = """
            SELECT m.*, s.name AS student_name, s.instrument AS student_instrument
            FROM makeups m JOIN students s ON m.student_id = s.id
            WHERE m.status = 'Pendente'
            ORDER BY m.scheduled_date, s.name
            """;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Makeup mk = mapRow(rs);
                mk.setStudentName(rs.getString("student_name"));
                mk.setStudentInstrument(rs.getString("student_instrument"));
                list.add(mk);
            }
        }
        return list;
    }

    public List<Makeup> findBySlot(String dayOfWeek, String slotTime) throws SQLException {
        List<Makeup> list = new ArrayList<>();
        String sql = """
            SELECT m.*, s.name AS student_name, s.instrument AS student_instrument
            FROM makeups m JOIN students s ON m.student_id = s.id
            WHERE m.day_of_week=? AND m.slot_time=? AND m.status='Pendente'
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, dayOfWeek);
            stmt.setString(2, slotTime);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Makeup mk = mapRow(rs);
                    mk.setStudentName(rs.getString("student_name"));
                    mk.setStudentInstrument(rs.getString("student_instrument"));
                    list.add(mk);
                }
            }
        }
        return list;
    }

    public long countBySlot(String dayOfWeek, String slotTime) throws SQLException {
        String sql = "SELECT COUNT(*) FROM makeups WHERE day_of_week=? AND slot_time=? AND status='Pendente'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, dayOfWeek);
            stmt.setString(2, slotTime);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    public Makeup save(Makeup m) throws SQLException {
        String sql = m.getId() == 0
            ? "INSERT INTO makeups (student_id, scheduled_date, notes, status, day_of_week, slot_time) VALUES (?,?,?,?,?,?)"
            : "UPDATE makeups SET student_id=?, scheduled_date=?, notes=?, status=?, day_of_week=?, slot_time=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, m.getStudentId());
            stmt.setString(2, m.getScheduledDate() != null ? m.getScheduledDate() : "");
            stmt.setString(3, m.getNotes() != null ? m.getNotes() : "");
            stmt.setString(4, m.getStatus());
            stmt.setString(5, m.getDayOfWeek() != null ? m.getDayOfWeek() : "");
            stmt.setString(6, m.getSlotTime() != null ? m.getSlotTime() : "");
            if (m.getId() != 0) stmt.setInt(7, m.getId());
            stmt.executeUpdate();
            if (m.getId() == 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) m.setId(keys.getInt(1));
                }
            }
        }
        return m;
    }

    public void updateStatus(int id, String status) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE makeups SET status=? WHERE id=?")) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM makeups WHERE id=?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Makeup mapRow(ResultSet rs) throws SQLException {
        Makeup m = new Makeup();
        m.setId(rs.getInt("id"));
        m.setStudentId(rs.getInt("student_id"));
        m.setScheduledDate(rs.getString("scheduled_date"));
        m.setNotes(rs.getString("notes"));
        m.setStatus(rs.getString("status"));
        m.setDayOfWeek(rs.getString("day_of_week"));
        m.setSlotTime(rs.getString("slot_time"));
        return m;
    }
}
