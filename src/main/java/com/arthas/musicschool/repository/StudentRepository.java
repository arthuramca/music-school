package com.arthas.musicschool.repository;

import com.arthas.musicschool.model.Student;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentRepository {

    private final Connection connection;

    public StudentRepository() {
        try { this.connection = DatabaseManager.getConnection(); }
        catch (SQLException e) { throw new RuntimeException("Falha ao conectar ao banco", e); }
    }

    public StudentRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Student> findAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY name COLLATE NOCASE";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Student> search(String query) throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = """
            SELECT * FROM students
            WHERE name LIKE ? OR instrument LIKE ? OR teacher LIKE ? OR cpf LIKE ?
            ORDER BY name COLLATE NOCASE
            """;
        String p = "%" + query + "%";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, p); stmt.setString(2, p);
            stmt.setString(3, p); stmt.setString(4, p);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Student> findByStatus(String status) throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE status = ? ORDER BY name COLLATE NOCASE";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Student> findByInstrument(String instrument) throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE instrument = ? ORDER BY name COLLATE NOCASE";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, instrument);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<String> findDistinctInstruments() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT instrument FROM students WHERE instrument != '' ORDER BY instrument COLLATE NOCASE";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }

    public Student save(Student s) throws SQLException {
        return s.getId() == 0 ? insert(s) : update(s);
    }

    private Student insert(Student s) throws SQLException {
        String sql = """
            INSERT INTO students (name, cpf, birth_date, phone, email, address,
                instrument, level, teacher, start_date, monthly_fee, payment_due_day,
                status, notes, photo_path)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(stmt, s);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
        }
        return s;
    }

    private Student update(Student s) throws SQLException {
        String sql = """
            UPDATE students SET name=?, cpf=?, birth_date=?, phone=?, email=?, address=?,
                instrument=?, level=?, teacher=?, start_date=?, monthly_fee=?, payment_due_day=?,
                status=?, notes=?, photo_path=?
            WHERE id=?
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bind(stmt, s);
            stmt.setInt(16, s.getId());
            stmt.executeUpdate();
        }
        return s;
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM students WHERE id=?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private void bind(PreparedStatement stmt, Student s) throws SQLException {
        stmt.setString(1,  safe(s.getName()));
        stmt.setString(2,  safe(s.getCpf()));
        stmt.setString(3,  s.getBirthDate() != null ? s.getBirthDate().toString() : null);
        stmt.setString(4,  safe(s.getPhone()));
        stmt.setString(5,  safe(s.getEmail()));
        stmt.setString(6,  safe(s.getAddress()));
        stmt.setString(7,  safe(s.getInstrument()));
        stmt.setString(8,  safe(s.getLevel()));
        stmt.setString(9,  safe(s.getTeacher()));
        stmt.setString(10, s.getStartDate() != null ? s.getStartDate().toString() : null);
        stmt.setDouble(11, s.getMonthlyFee());
        stmt.setInt(12,    s.getPaymentDueDay());
        stmt.setString(13, safe(s.getStatus()));
        stmt.setString(14, safe(s.getNotes()));
        stmt.setString(15, safe(s.getPhotoPath()));
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setCpf(rs.getString("cpf"));
        s.setBirthDate(parseDate(rs.getString("birth_date")));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setInstrument(rs.getString("instrument"));
        s.setLevel(rs.getString("level"));
        s.setTeacher(rs.getString("teacher"));
        s.setStartDate(parseDate(rs.getString("start_date")));
        s.setMonthlyFee(rs.getDouble("monthly_fee"));
        s.setPaymentDueDay(rs.getInt("payment_due_day"));
        s.setStatus(rs.getString("status"));
        s.setNotes(rs.getString("notes"));
        s.setPhotoPath(rs.getString("photo_path"));
        return s;
    }

    private LocalDate parseDate(String v) {
        return (v != null && !v.isBlank()) ? LocalDate.parse(v) : null;
    }

    private String safe(String v) { return v != null ? v : ""; }
}
