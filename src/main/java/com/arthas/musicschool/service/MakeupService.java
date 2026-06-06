package com.arthas.musicschool.service;

import com.arthas.musicschool.model.Makeup;
import com.arthas.musicschool.repository.MakeupRepository;

import java.sql.SQLException;
import java.util.List;

public class MakeupService {

    private final MakeupRepository repository = new MakeupRepository();

    public List<Makeup> getByStudent(int studentId) throws SQLException {
        return repository.findByStudent(studentId);
    }

    public Makeup schedule(int studentId, String date, String dayOfWeek, String slotTime, String notes) throws SQLException {
        Makeup m = new Makeup();
        m.setStudentId(studentId);
        m.setScheduledDate(date);
        m.setDayOfWeek(dayOfWeek != null ? dayOfWeek : "");
        m.setSlotTime(slotTime != null ? slotTime : "");
        m.setNotes(notes != null ? notes : "");
        m.setStatus("Pendente");
        return repository.save(m);
    }

    public List<Makeup> getAllPending() throws SQLException {
        return repository.findAllPending();
    }

    public List<Makeup> findBySlot(String dayOfWeek, String slotTime) throws SQLException {
        return repository.findBySlot(dayOfWeek, slotTime);
    }

    public long countBySlot(String dayOfWeek, String slotTime) throws SQLException {
        return repository.countBySlot(dayOfWeek, slotTime);
    }

    public void markDone(int makeupId) throws SQLException {
        repository.updateStatus(makeupId, "Realizada");
    }

    public void delete(int makeupId) throws SQLException {
        repository.delete(makeupId);
    }
}
