package com.arthas.musicschool.model;

import java.util.List;

public class FinancialReport {

    private String month; // formato: "2026-06"
    private double totalPrevisto;
    private double totalApurado;
    private double totalPendente;
    private List<FinancialReportItem> items;

    public String getMonth()        { return month; }
    public void   setMonth(String v)     { this.month = v; }

    public double getTotalPrevisto() { return totalPrevisto; }
    public void   setTotalPrevisto(double v) { this.totalPrevisto = v; }

    public double getTotalApurado()  { return totalApurado; }
    public void   setTotalApurado(double v)  { this.totalApurado = v; }

    public double getTotalPendente() { return totalPendente; }
    public void   setTotalPendente(double v) { this.totalPendente = v; }

    public List<FinancialReportItem> getItems() { return items; }
    public void setItems(List<FinancialReportItem> items) { this.items = items; }

    public String getMonthLabel() {
        if (month == null || month.length() < 7) return month;
        String[] p = month.split("-");
        String[] months = {"","Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                           "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
        try { return months[Integer.parseInt(p[1])] + "/" + p[0]; }
        catch (Exception e) { return month; }
    }
}
