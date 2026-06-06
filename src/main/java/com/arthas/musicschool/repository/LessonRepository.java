package com.arthas.musicschool.repository;

import com.arthas.musicschool.model.Lesson;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LessonRepository {

    private final Connection connection;

    public LessonRepository() {
        try { this.connection = DatabaseManager.getConnection(); }
        catch (SQLException e) { throw new RuntimeException("Falha ao conectar ao banco", e); }
    }

    public LessonRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Lesson> findByStudent(int studentId) throws SQLException {
        List<Lesson> list = new ArrayList<>();
        String sql = "SELECT * FROM lessons WHERE student_id=? ORDER BY lesson_date DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public long countAttended(int studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM lessons WHERE student_id=? AND attended=1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    public long countTotal(int studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM lessons WHERE student_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    public long countInMonth(int studentId, String yearMonth) throws SQLException {
        String sql = "SELECT COUNT(*) FROM lessons WHERE student_id=? AND lesson_date LIKE ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setString(2, yearMonth + "-%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    public long countAbsencesInMonth(int studentId, String yearMonth) throws SQLException {
        String sql = "SELECT COUNT(*) FROM lessons WHERE student_id=? AND lesson_date LIKE ? AND attended=0";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setString(2, yearMonth + "-%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    public Lesson save(Lesson l) throws SQLException {
        return l.getId() == 0 ? insert(l) : update(l);
    }

    private Lesson insert(Lesson l) throws SQLException {
        String sql = "INSERT INTO lessons (student_id, lesson_date, attended, notes) VALUES (?,?,?,?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(stmt, l);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) l.setId(keys.getInt(1));
            }
        }
        return l;
    }

    private Lesson update(Lesson l) throws SQLException {
        String sql = "UPDATE lessons SET student_id=?, lesson_date=?, attended=?, notes=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bind(stmt, l);
            stmt.setInt(5, l.getId());
            stmt.executeUpdate();
        }
        return l;
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM lessons WHERE id=?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private void bind(PreparedStatement stmt, Lesson l) throws SQLException {
        stmt.setInt(1,    l.getStudentId());
        stmt.setString(2, l.getLessonDate());
        stmt.setInt(3,    l.isAttended() ? 1 : 0);
        stmt.setString(4, l.getNotes() != null ? l.getNotes() : "");
    }

    private Lesson mapRow(ResultSet rs) throws SQLException {
        Lesson l = new Lesson();
        l.setId(rs.getInt("id"));
        l.setStudentId(rs.getInt("student_id"));
        l.setLessonDate(rs.getString("lesson_date"));
        l.setAttended(rs.getInt("attended") == 1);
        l.setNotes(rs.getString("notes"));
        return l;
    }
}
