package com.arthas.musicschool.service;

import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.repository.StudentRepository;

import java.sql.SQLException;
import java.util.List;

public class StudentService {

    private final StudentRepository repository;

    public StudentService() { this.repository = new StudentRepository(); }
    public StudentService(StudentRepository repository) { this.repository = repository; }

    public List<Student> getAllStudents() throws SQLException { return repository.findAll(); }
    public List<Student> search(String query) throws SQLException { return repository.search(query); }
    public List<Student> getByStatus(String status) throws SQLException { return repository.findByStatus(status); }
    public List<Student> getByInstrument(String instrument) throws SQLException { return repository.findByInstrument(instrument); }
    public List<String> getDistinctInstruments() throws SQLException { return repository.findDistinctInstruments(); }
    public Student save(Student student) throws SQLException { return repository.save(student); }
    public void delete(int id) throws SQLException { repository.delete(id); }

    public long countActive() throws SQLException {
        return repository.findByStatus("Ativo").size();
    }
}
