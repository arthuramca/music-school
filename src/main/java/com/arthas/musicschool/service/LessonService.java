package com.arthas.musicschool.service;

import com.arthas.musicschool.model.Lesson;
import com.arthas.musicschool.repository.LessonRepository;

import java.sql.SQLException;
import java.util.List;

public class LessonService {

    private final LessonRepository repository;

    public LessonService() { this.repository = new LessonRepository(); }
    public LessonService(LessonRepository repository) { this.repository = repository; }

    public List<Lesson> getLessons(int studentId) throws SQLException {
        return repository.findByStudent(studentId);
    }

    public Lesson save(Lesson lesson) throws SQLException {
        return repository.save(lesson);
    }

    public void delete(int id) throws SQLException {
        repository.delete(id);
    }

    public String getAttendanceRate(int studentId) throws SQLException {
        long total = repository.countTotal(studentId);
        if (total == 0) return "—";
        long attended = repository.countAttended(studentId);
        return String.format("%.0f%%", (attended * 100.0) / total);
    }
}
