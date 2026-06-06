package com.arthas.musicschool.repository;

import com.arthas.musicschool.model.WaitlistEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WaitlistRepository {

    private final Connection connection;

    public WaitlistRepository() {
        try { this.connection = DatabaseManager.getConnection(); }
        catch (SQLException e) { throw new RuntimeException("Falha ao conectar ao banco", e); }
    }

    public List<WaitlistEntry> findAll() throws SQLException {
        List<WaitlistEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM waitlist ORDER BY registered_date DESC, name COLLATE NOCASE";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<WaitlistEntry> findBySlot(String day, String time) throws SQLException {
        List<WaitlistEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM waitlist WHERE preferred_day=? AND preferred_time=? ORDER BY registered_date";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, day);
            stmt.setString(2, time);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public WaitlistEntry save(WaitlistEntry w) throws SQLException {
        return w.getId() == 0 ? insert(w) : update(w);
    }

    private WaitlistEntry insert(WaitlistEntry w) throws SQLException {
        String sql = """
            INSERT INTO waitlist (name, phone, email, instrument,
                preferred_day, preferred_time, registered_date, notes)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(stmt, w);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) w.setId(keys.getInt(1));
            }
        }
        return w;
    }

    private WaitlistEntry update(WaitlistEntry w) throws SQLException {
        String sql = """
            UPDATE waitlist SET name=?, phone=?, email=?, instrument=?,
                preferred_day=?, preferred_time=?, registered_date=?, notes=?
            WHERE id=?
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bind(stmt, w);
            stmt.setInt(9, w.getId());
            stmt.executeUpdate();
        }
        return w;
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM waitlist WHERE id=?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private void bind(PreparedStatement stmt, WaitlistEntry w) throws SQLException {
        stmt.setString(1, safe(w.getName()));
        stmt.setString(2, safe(w.getPhone()));
        stmt.setString(3, safe(w.getEmail()));
        stmt.setString(4, safe(w.getInstrument()));
        stmt.setString(5, safe(w.getPreferredDay()));
        stmt.setString(6, safe(w.getPreferredTime()));
        stmt.setString(7, safe(w.getRegisteredDate()));
        stmt.setString(8, safe(w.getNotes()));
    }

    private WaitlistEntry mapRow(ResultSet rs) throws SQLException {
        WaitlistEntry w = new WaitlistEntry();
        w.setId(rs.getInt("id"));
        w.setName(rs.getString("name"));
        w.setPhone(rs.getString("phone"));
        w.setEmail(rs.getString("email"));
        w.setInstrument(rs.getString("instrument"));
        w.setPreferredDay(rs.getString("preferred_day"));
        w.setPreferredTime(rs.getString("preferred_time"));
        w.setRegisteredDate(rs.getString("registered_date"));
        w.setNotes(rs.getString("notes"));
        return w;
    }

    private String safe(String v) { return v != null ? v : ""; }
}
