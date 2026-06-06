package com.arthas.musicschool.model;

public class Makeup {

    private int id;
    private int studentId;
    private String studentName;
    private String studentInstrument;
    private String dayOfWeek;
    private String slotTime;
    private String scheduledDate;
    private String notes;
    private String status; // Pendente, Realizada

    public Makeup() { this.status = "Pendente"; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentInstrument() { return studentInstrument; }
    public void setStudentInstrument(String studentInstrument) { this.studentInstrument = studentInstrument; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getSlotTime() { return slotTime; }
    public void setSlotTime(String slotTime) { this.slotTime = slotTime; }

    public String getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
