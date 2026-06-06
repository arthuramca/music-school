package com.arthas.musicschool.repository;

import com.arthas.musicschool.model.Payment;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentRepositoryTest {

    private static Connection connection;
    private PaymentRepository repository;

    @BeforeAll
    static void setupDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE payments (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id      INTEGER NOT NULL,
                    reference_month TEXT    NOT NULL,
                    amount          REAL    DEFAULT 0.0,
                    paid_date       TEXT    DEFAULT '',
                    status          TEXT    DEFAULT 'Pendente',
                    notes           TEXT    DEFAULT ''
                )
                """);
        }
    }

    @BeforeEach
    void setup() { repository = new PaymentRepository(connection); }

    @AfterEach
    void clean() throws SQLException {
        connection.createStatement().execute("DELETE FROM payments");
    }

    @AfterAll
    static void close() throws SQLException { connection.close(); }

    @Test @Order(1)
    void deveSalvarPagamento() throws SQLException {
        Payment p = new Payment(1, "2026-06", 200.0);
        Payment saved = repository.save(p);

        assertTrue(saved.getId() > 0);
        assertEquals("2026-06", saved.getReferenceMonth());
        assertEquals("Pendente", saved.getStatus());
    }

    @Test @Order(2)
    void deveBuscarPorAluno() throws SQLException {
        repository.save(new Payment(1, "2026-05", 200.0));
        repository.save(new Payment(1, "2026-06", 200.0));
        repository.save(new Payment(2, "2026-06", 150.0));

        List<Payment> p1 = repository.findByStudent(1);
        assertEquals(2, p1.size());

        List<Payment> p2 = repository.findByStudent(2);
        assertEquals(1, p2.size());
    }

    @Test @Order(3)
    void deveBuscarPorAlunoEMes() throws SQLException {
        repository.save(new Payment(1, "2026-06", 200.0));

        Payment found = repository.findByStudentAndMonth(1, "2026-06");
        assertNotNull(found);
        assertEquals(200.0, found.getAmount(), 0.001);

        Payment notFound = repository.findByStudentAndMonth(1, "2026-07");
        assertNull(notFound);
    }

    @Test @Order(4)
    void deveAtualizarStatusParaPago() throws SQLException {
        Payment p = new Payment(1, "2026-06", 200.0);
        repository.save(p);

        p.setStatus("Pago");
        p.setPaidDate("2026-06-05");
        repository.save(p);

        Payment updated = repository.findByStudentAndMonth(1, "2026-06");
        assertEquals("Pago", updated.getStatus());
        assertEquals("2026-06-05", updated.getPaidDate());
    }

    @Test @Order(5)
    void deveContarPorStatus() throws SQLException {
        Payment p1 = new Payment(1, "2026-04", 200.0); p1.setStatus("Pago");     repository.save(p1);
        Payment p2 = new Payment(1, "2026-05", 200.0); p2.setStatus("Atrasado"); repository.save(p2);
        Payment p3 = new Payment(1, "2026-06", 200.0); p3.setStatus("Pendente"); repository.save(p3);

        assertEquals(1, repository.countByStatus("Pago"));
        assertEquals(1, repository.countByStatus("Atrasado"));
        assertEquals(1, repository.countByStatus("Pendente"));
    }

    @Test @Order(6)
    void deveDeletarPagamento() throws SQLException {
        Payment p = new Payment(1, "2026-06", 200.0);
        repository.save(p);
        repository.delete(p.getId());

        assertTrue(repository.findByStudent(1).isEmpty());
    }

    @Test @Order(7)
    void deveRetornarMonthLabelFormatado() {
        Payment p = new Payment(1, "2026-06", 0);
        assertEquals("06/2026", p.getMonthLabel());
    }
}
