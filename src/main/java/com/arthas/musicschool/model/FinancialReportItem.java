package com.arthas.musicschool.model;

public class FinancialReportItem {

    private int    studentId;
    private String studentName;
    private String instrument;
    private double monthlyFee;
    private double amountPaid;
    private String paymentStatus;
    private String paidDate;

    public int    getStudentId()     { return studentId; }
    public void   setStudentId(int v)     { this.studentId = v; }

    public String getStudentName()   { return studentName; }
    public void   setStudentName(String v) { this.studentName = v; }

    public String getInstrument()    { return instrument; }
    public void   setInstrument(String v) { this.instrument = v; }

    public double getMonthlyFee()    { return monthlyFee; }
    public void   setMonthlyFee(double v) { this.monthlyFee = v; }

    public double getAmountPaid()    { return amountPaid; }
    public void   setAmountPaid(double v) { this.amountPaid = v; }

    public String getPaymentStatus() { return paymentStatus; }
    public void   setPaymentStatus(String v) { this.paymentStatus = v; }

    public String getPaidDate()      { return paidDate; }
    public void   setPaidDate(String v) { this.paidDate = v; }
}
