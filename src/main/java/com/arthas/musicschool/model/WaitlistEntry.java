package com.arthas.musicschool.model;

import java.time.LocalDate;

public class WaitlistEntry {

    private int    id;
    private String name;
    private String phone;
    private String email;
    private String instrument;
    private String preferredDay;
    private String preferredTime;
    private String registeredDate;
    private String notes;

    public WaitlistEntry() {
        this.registeredDate = LocalDate.now().toString();
    }

    public int    getId()             { return id; }
    public void   setId(int id)       { this.id = id; }

    public String getName()           { return name; }
    public void   setName(String v)   { this.name = v; }

    public String getPhone()          { return phone; }
    public void   setPhone(String v)  { this.phone = v; }

    public String getEmail()          { return email; }
    public void   setEmail(String v)  { this.email = v; }

    public String getInstrument()         { return instrument; }
    public void   setInstrument(String v) { this.instrument = v; }

    public String getPreferredDay()         { return preferredDay; }
    public void   setPreferredDay(String v) { this.preferredDay = v; }

    public String getPreferredTime()         { return preferredTime; }
    public void   setPreferredTime(String v) { this.preferredTime = v; }

    public String getRegisteredDate()         { return registeredDate; }
    public void   setRegisteredDate(String v) { this.registeredDate = v; }

    public String getNotes()          { return notes; }
    public void   setNotes(String v)  { this.notes = v; }

    public String getSlotLabel() {
        if ((preferredDay == null || preferredDay.isBlank()) &&
            (preferredTime == null || preferredTime.isBlank())) return "—";
        return (preferredDay != null ? preferredDay : "") + " " +
               (preferredTime != null ? preferredTime : "");
    }
}
