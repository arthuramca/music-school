package com.arthas.musicschool.model;

public class Payment {

    private int id;
    private int studentId;
    private String referenceMonth; // formato: 2026-06
    private double amount;
    private String paidDate;
    private String status; // Pago, Pendente, Atrasado
    private String notes;

    public Payment() {
        this.status = "Pendente";
    }

    public Payment(int studentId, String referenceMonth, double amount) {
        this.studentId = studentId;
        this.referenceMonth = referenceMonth;
        this.amount = amount;
        this.status = "Pendente";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getReferenceMonth() { return referenceMonth; }
    public void setReferenceMonth(String referenceMonth) { this.referenceMonth = referenceMonth; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaidDate() { return paidDate; }
    public void setPaidDate(String paidDate) { this.paidDate = paidDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getMonthLabel() {
        if (referenceMonth == null || referenceMonth.length() < 7) return referenceMonth;
        String[] parts = referenceMonth.split("-");
        return parts[1] + "/" + parts[0];
    }
}
