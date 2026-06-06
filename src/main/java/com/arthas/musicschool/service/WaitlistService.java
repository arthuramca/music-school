package com.arthas.musicschool.service;

import com.arthas.musicschool.model.WaitlistEntry;
import com.arthas.musicschool.repository.WaitlistRepository;

import java.sql.SQLException;
import java.util.List;

public class WaitlistService {

    private final WaitlistRepository repository;

    public WaitlistService() { this.repository = new WaitlistRepository(); }

    public List<WaitlistEntry> getAll() throws SQLException {
        return repository.findAll();
    }

    public List<WaitlistEntry> findBySlot(String day, String time) throws SQLException {
        return repository.findBySlot(day, time);
    }

    public WaitlistEntry save(WaitlistEntry w) throws SQLException {
        return repository.save(w);
    }

    public void delete(int id) throws SQLException {
        repository.delete(id);
    }
}
