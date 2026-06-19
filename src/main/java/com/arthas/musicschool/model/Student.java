package com.arthas.musicschool.model;

import java.time.LocalDate;

public class Student {

    private int id;
    private String name;
    private String cpf;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String address;
    private String instrument;
    private String level;
    private String teacher;
    private LocalDate startDate;
    private double monthlyFee;
    private int paymentDueDay;
    private String status;
    private String notes;
    private String photoPath;
    private String lessonDay;
    private String lessonTime;
    private String instrument2;
    private String lessonDay2;
    private String lessonTime2;
    private int makeupPending;

    public Student() {
        this.paymentDueDay = 5;
        this.status = "Ativo";
        this.monthlyFee = 0.0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getInstrument() { return instrument; }
    public void setInstrument(String instrument) { this.instrument = instrument; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public double getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(double monthlyFee) { this.monthlyFee = monthlyFee; }

    public int getPaymentDueDay() { return paymentDueDay; }
    public void setPaymentDueDay(int paymentDueDay) { this.paymentDueDay = paymentDueDay; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getLessonDay()  { return lessonDay; }
    public void setLessonDay(String lessonDay)   { this.lessonDay  = lessonDay; }

    public String getLessonTime() { return lessonTime; }
    public void setLessonTime(String lessonTime) { this.lessonTime = lessonTime; }

    public String getInstrument2() { return instrument2; }
    public void setInstrument2(String instrument2) { this.instrument2 = instrument2; }

    public String getLessonDay2()  { return lessonDay2; }
    public void setLessonDay2(String lessonDay2) { this.lessonDay2 = lessonDay2; }

    public String getLessonTime2() { return lessonTime2; }
    public void setLessonTime2(String lessonTime2) { this.lessonTime2 = lessonTime2; }

    public int getMakeupPending() { return makeupPending; }
    public void setMakeupPending(int makeupPending) { this.makeupPending = makeupPending; }

    @Override
    public String toString() {
        return "Student{id=" + id + ", name='" + name + "', instrument='" + instrument + "'}";
    }
}
