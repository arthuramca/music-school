package com.arthas.musicschool.repository;

import com.arthas.musicschool.model.Student;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudentRepositoryTest {

    private static Connection connection;
    private StudentRepository repository;

    @BeforeAll
    static void setupDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE students (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    name            TEXT    NOT NULL,
                    cpf             TEXT    DEFAULT '',
                    birth_date      TEXT,
                    phone           TEXT    DEFAULT '',
                    email           TEXT    DEFAULT '',
                    address         TEXT    DEFAULT '',
                    instrument      TEXT    DEFAULT '',
                    level           TEXT    DEFAULT '',
                    teacher         TEXT    DEFAULT '',
                    start_date      TEXT,
                    monthly_fee     REAL    DEFAULT 0.0,
                    payment_due_day INTEGER DEFAULT 5,
                    status          TEXT    DEFAULT 'Ativo',
                    notes           TEXT    DEFAULT '',
                    photo_path      TEXT    DEFAULT ''
                )
                """);
        }
    }

    @BeforeEach
    void setup() { repository = new StudentRepository(connection); }

    @AfterEach
    void clean() throws SQLException {
        connection.createStatement().execute("DELETE FROM students");
    }

    @AfterAll
    static void close() throws SQLException { connection.close(); }

    @Test @Order(1)
    void deveSalvarEAtribuirId() throws SQLException {
        Student s = new Student();
        s.setName("João Silva");
        s.setInstrument("Violão");

        Student saved = repository.save(s);

        assertTrue(saved.getId() > 0);
        assertEquals("João Silva", saved.getName());
    }

    @Test @Order(2)
    void deveBuscarTodosOrdenadosPorNome() throws SQLException {
        Student s1 = new Student(); s1.setName("Zé");       repository.save(s1);
        Student s2 = new Student(); s2.setName("Ana");      repository.save(s2);
        Student s3 = new Student(); s3.setName("Marcos");   repository.save(s3);

        List<Student> all = repository.findAll();

        assertEquals(3, all.size());
        assertEquals("Ana",    all.get(0).getName());
        assertEquals("Marcos", all.get(1).getName());
        assertEquals("Zé",     all.get(2).getName());
    }

    @Test @Order(3)
    void deveAtualizarAluno() throws SQLException {
        Student s = new Student(); s.setName("Original"); s.setMonthlyFee(100.0);
        repository.save(s);

        s.setName("Atualizado"); s.setMonthlyFee(250.0);
        repository.save(s);

        List<Student> all = repository.findAll();
        assertEquals(1, all.size());
        assertEquals("Atualizado", all.get(0).getName());
        assertEquals(250.0, all.get(0).getMonthlyFee(), 0.001);
    }

    @Test @Order(4)
    void deveDeletarAluno() throws SQLException {
        Student s = new Student(); s.setName("Para Deletar");
        repository.save(s);
        repository.delete(s.getId());

        assertTrue(repository.findAll().isEmpty());
    }

    @Test @Order(5)
    void deveBuscarPorNomeEInstrumento() throws SQLException {
        Student s1 = new Student(); s1.setName("Ana Piano");    s1.setInstrument("Piano");    repository.save(s1);
        Student s2 = new Student(); s2.setName("Bob Violão");   s2.setInstrument("Violão");   repository.save(s2);
        Student s3 = new Student(); s3.setName("Carlos Piano"); s3.setInstrument("Piano");    repository.save(s3);

        assertEquals(2, repository.search("Piano").size());
        assertEquals(1, repository.search("Bob").size());
        assertEquals(0, repository.search("XYZ999").size());
    }

    @Test @Order(6)
    void deveFiltrarPorStatus() throws SQLException {
        Student s1 = new Student(); s1.setName("Ativo 1");  s1.setStatus("Ativo");   repository.save(s1);
        Student s2 = new Student(); s2.setName("Inativo");  s2.setStatus("Inativo"); repository.save(s2);
        Student s3 = new Student(); s3.setName("Ativo 2");  s3.setStatus("Ativo");   repository.save(s3);

        assertEquals(2, repository.findByStatus("Ativo").size());
        assertEquals(1, repository.findByStatus("Inativo").size());
    }

    @Test @Order(7)
    void devePersistirDatas() throws SQLException {
        Student s = new Student();
        s.setName("Com Data");
        s.setBirthDate(LocalDate.of(2000, 5, 15));
        s.setStartDate(LocalDate.of(2024, 1, 10));
        repository.save(s);

        Student recovered = repository.findAll().get(0);
        assertEquals(LocalDate.of(2000, 5, 15), recovered.getBirthDate());
        assertEquals(LocalDate.of(2024, 1, 10), recovered.getStartDate());
    }

    @Test @Order(8)
    void deveRetornarListaVaziaQuandoSemAlunos() throws SQLException {
        assertTrue(repository.findAll().isEmpty());
    }
}
