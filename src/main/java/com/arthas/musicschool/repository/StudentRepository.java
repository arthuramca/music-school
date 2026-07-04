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

    public List<Student> findBySlot(String day, String time) throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE lesson_day=? AND lesson_time=? ORDER BY name COLLATE NOCASE";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, day);
            stmt.setString(2, time);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public long countBySlot(String day, String time, int excludeId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM students
            WHERE ((lesson_day=? AND lesson_time=?) OR (lesson_day2=? AND lesson_time2=?))
            AND id!=?
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, day); stmt.setString(2, time);
            stmt.setString(3, day); stmt.setString(4, time);
            stmt.setInt(5, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    public List<String> findInstrumentsBySlot(String day, String time, int excludeId) throws SQLException {
        List<String> instruments = new ArrayList<>();
        String sql1 = "SELECT instrument FROM students WHERE lesson_day=? AND lesson_time=? AND id!=?";
        String sql2 = "SELECT instrument2 FROM students WHERE lesson_day2=? AND lesson_time2=? AND id!=? AND instrument2 != ''";
        for (String sql : new String[]{sql1, sql2}) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, day); stmt.setString(2, time); stmt.setInt(3, excludeId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String v = rs.getString(1);
                        if (v != null && !v.isBlank()) instruments.add(v);
                    }
                }
            }
        }
        return instruments;
    }

    public boolean existsById(int id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM students WHERE id=?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void adjustMakeupPending(int studentId, int delta) throws SQLException {
        String sql = "UPDATE students SET makeup_pending = MAX(0, makeup_pending + ?) WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, studentId);
            stmt.executeUpdate();
        }
    }

    public void zeroMakeupPending(int studentId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE students SET makeup_pending=0 WHERE id=?")) {
            stmt.setInt(1, studentId);
            stmt.executeUpdate();
        }
    }

    private Student insert(Student s) throws SQLException {
        String sql = """
            INSERT INTO students (name, cpf, birth_date, phone, email, address,
                instrument, level, teacher, start_date, monthly_fee, payment_due_day,
                status, notes, photo, lesson_day, lesson_time, makeup_pending,
                instrument2, lesson_day2, lesson_time2)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
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
                status=?, notes=?, photo=?, lesson_day=?, lesson_time=?, makeup_pending=?,
                instrument2=?, lesson_day2=?, lesson_time2=?
            WHERE id=?
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bind(stmt, s);
            stmt.setInt(22, s.getId());
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

    public List<Integer> findDuplicateIds() throws SQLException {
        // Mantém o menor ID para cada nome (case-insensitive); retorna os IDs a excluir
        String sql = """
            SELECT id FROM students
            WHERE id NOT IN (
                SELECT MIN(id) FROM students GROUP BY LOWER(name)
            )
            AND LOWER(name) IN (
                SELECT LOWER(name) FROM students GROUP BY LOWER(name) HAVING COUNT(*) > 1
            )
            """;
        List<Integer> ids = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) ids.add(rs.getInt(1));
        }
        return ids;
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
        stmt.setBytes(15,  s.getPhoto());
        stmt.setString(16, safe(s.getLessonDay()));
        stmt.setString(17, safe(s.getLessonTime()));
        stmt.setInt(18,    s.getMakeupPending());
        stmt.setString(19, safe(s.getInstrument2()));
        stmt.setString(20, safe(s.getLessonDay2()));
        stmt.setString(21, safe(s.getLessonTime2()));
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
        s.setPhoto(rs.getBytes("photo"));
        s.setLessonDay(rs.getString("lesson_day"));
        s.setLessonTime(rs.getString("lesson_time"));
        s.setMakeupPending(rs.getInt("makeup_pending"));
        s.setInstrument2(rs.getString("instrument2"));
        s.setLessonDay2(rs.getString("lesson_day2"));
        s.setLessonTime2(rs.getString("lesson_time2"));
        return s;
    }

    private LocalDate parseDate(String v) {
        return (v != null && !v.isBlank()) ? LocalDate.parse(v) : null;
    }

    private String safe(String v) { return v != null ? v : ""; }
}
