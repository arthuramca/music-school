package com.arthas.musicschool.service;

import com.arthas.musicschool.model.Lesson;
import com.arthas.musicschool.repository.LessonRepository;

import java.sql.SQLException;
import java.time.YearMonth;
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

    public long countLessonsInCurrentMonth(int studentId) throws SQLException {
        return repository.countInMonth(studentId, YearMonth.now().toString());
    }

    public long countAbsencesInCurrentMonth(int studentId) throws SQLException {
        return repository.countAbsencesInMonth(studentId, YearMonth.now().toString());
    }

    public int countConsecutiveAbsences(int studentId) throws SQLException {
        List<Lesson> lessons = repository.findByStudent(studentId); // ordenado por data DESC
        int count = 0;
        for (Lesson l : lessons) {
            if (!l.isAttended()) count++;
            else break;
        }
        return count;
    }
}
