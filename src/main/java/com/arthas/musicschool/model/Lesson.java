package com.arthas.musicschool.model;

public class Lesson {

    private int id;
    private int studentId;
    private String lessonDate;
    private boolean attended;
    private String notes;

    public Lesson() {
        this.attended = true;
    }

    public Lesson(int studentId, String lessonDate, boolean attended) {
        this.studentId = studentId;
        this.lessonDate = lessonDate;
        this.attended = attended;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getLessonDate() { return lessonDate; }
    public void setLessonDate(String lessonDate) { this.lessonDate = lessonDate; }

    public boolean isAttended() { return attended; }
    public void setAttended(boolean attended) { this.attended = attended; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAttendedLabel() { return attended ? "Presente" : "Falta"; }
}
