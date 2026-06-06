package com.arthas.musicschool.service;

import com.arthas.musicschool.model.Lesson;
import com.arthas.musicschool.model.Payment;
import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.repository.LessonRepository;
import com.arthas.musicschool.repository.PaymentRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class PdfService {

    private final PaymentRepository paymentRepository;
    private final LessonRepository  lessonRepository;

    public PdfService() {
        this.paymentRepository = new PaymentRepository();
        this.lessonRepository  = new LessonRepository();
    }

    public void generateStudentCard(Student student, File file) throws Exception {
        List<Payment> payments = paymentRepository.findByStudent(student.getId());
        List<Lesson>  lessons  = lessonRepository.findByStudent(student.getId());
        long attended = lessonRepository.countAttended(student.getId());

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();

        Font titleFont   = new Font(Font.HELVETICA, 18, Font.BOLD,   new Color(44,  62,  80));
        Font sectionFont = new Font(Font.HELVETICA, 11, Font.BOLD,   new Color(52, 152, 219));
        Font labelFont   = new Font(Font.HELVETICA, 10, Font.BOLD,   Color.DARK_GRAY);
        Font valueFont   = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
        Font thFont      = new Font(Font.HELVETICA,  9, Font.BOLD,   Color.WHITE);
        Font tdFont      = new Font(Font.HELVETICA,  9, Font.NORMAL, Color.BLACK);

        // Cabeçalho
        Paragraph title = new Paragraph("FICHA CADASTRAL DO ALUNO", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);
        Paragraph sub = new Paragraph("Music School", new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY));
        sub.setAlignment(Element.ALIGN_CENTER);
        doc.add(sub);
        doc.add(Chunk.NEWLINE);

        // Dados pessoais
        addSection(doc, sectionFont, "DADOS PESSOAIS");
        PdfPTable personal = table4();
        addRow(personal, labelFont, valueFont, "Nome",       safe(student.getName()),   "CPF",      safe(student.getCpf()));
        addRow(personal, labelFont, valueFont, "Nascimento", date(student.getBirthDate()), "Telefone", safe(student.getPhone()));
        addRow(personal, labelFont, valueFont, "E-mail",     safe(student.getEmail()), "Endereço", safe(student.getAddress()));
        doc.add(personal);
        doc.add(Chunk.NEWLINE);

        // Dados musicais
        addSection(doc, sectionFont, "DADOS MUSICAIS");
        PdfPTable musical = table4();
        addRow(musical, labelFont, valueFont, "Instrumento", safe(student.getInstrument()), "Nível",      safe(student.getLevel()));
        addRow(musical, labelFont, valueFont, "Professor",   safe(student.getTeacher()),    "Início",     date(student.getStartDate()));
        addRow(musical, labelFont, valueFont, "Mensalidade", String.format("R$ %.2f", student.getMonthlyFee()),
                                                              "Vencimento",  "Dia " + student.getPaymentDueDay());
        addRow(musical, labelFont, valueFont, "Status",      safe(student.getStatus()),     "",            "");
        doc.add(musical);
        doc.add(Chunk.NEWLINE);

        // Observações
        if (student.getNotes() != null && !student.getNotes().isBlank()) {
            addSection(doc, sectionFont, "OBSERVAÇÕES");
            doc.add(new Paragraph(student.getNotes(), valueFont));
            doc.add(Chunk.NEWLINE);
        }

        // Frequência
        long total = lessons.size();
        double rate = total > 0 ? (attended * 100.0) / total : 0;
        addSection(doc, sectionFont, "FREQUÊNCIA");
        doc.add(new Paragraph(
            "Total de aulas: " + total +
            "   |   Presenças: " + attended +
            "   |   Faltas: " + (total - attended) +
            "   |   Frequência: " + String.format("%.0f%%", rate), valueFont));
        doc.add(Chunk.NEWLINE);

        // Histórico de pagamentos
        if (!payments.isEmpty()) {
            addSection(doc, sectionFont, "HISTÓRICO DE PAGAMENTOS");
            PdfPTable payTable = new PdfPTable(new float[]{2, 2, 2, 2});
            payTable.setWidthPercentage(100);
            for (String h : new String[]{"Mês", "Valor (R$)", "Status", "Data Pagamento"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, thFont));
                c.setBackgroundColor(new Color(44, 62, 80));
                c.setPadding(5);
                payTable.addCell(c);
            }
            for (Payment p : payments) {
                Color statusColor = switch (p.getStatus()) {
                    case "Pago"     -> new Color(39, 174,  96);
                    case "Atrasado" -> new Color(231, 76,  60);
                    default         -> new Color(243, 156,  18);
                };
                payTable.addCell(cell(p.getMonthLabel(), tdFont));
                payTable.addCell(cell(String.format("R$ %.2f", p.getAmount()), tdFont));
                payTable.addCell(cell(p.getStatus(), new Font(Font.HELVETICA, 9, Font.BOLD, statusColor)));
                payTable.addCell(cell(p.getPaidDate() != null && !p.getPaidDate().isBlank() ? p.getPaidDate() : "—", tdFont));
            }
            doc.add(payTable);
            doc.add(Chunk.NEWLINE);
        }

        // Registro de aulas (até 20 mais recentes)
        if (!lessons.isEmpty()) {
            addSection(doc, sectionFont, "REGISTRO DE AULAS");
            PdfPTable lessonTable = new PdfPTable(new float[]{2, 2, 4});
            lessonTable.setWidthPercentage(100);
            for (String h : new String[]{"Data", "Presença", "Observações"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, thFont));
                c.setBackgroundColor(new Color(44, 62, 80));
                c.setPadding(5);
                lessonTable.addCell(c);
            }
            int limit = Math.min(lessons.size(), 20);
            for (int i = 0; i < limit; i++) {
                Lesson l = lessons.get(i);
                Color presColor = l.isAttended() ? new Color(39, 174, 96) : new Color(231, 76, 60);
                lessonTable.addCell(cell(l.getLessonDate(), tdFont));
                lessonTable.addCell(cell(l.getAttendedLabel(), new Font(Font.HELVETICA, 9, Font.BOLD, presColor)));
                lessonTable.addCell(cell(l.getNotes() != null ? l.getNotes() : "", tdFont));
            }
            doc.add(lessonTable);
        }

        doc.close();
    }

    private void addSection(Document doc, Font font, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(4);
        p.setSpacingAfter(2);
        doc.add(p);
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setBorderWidthBottom(1);
        c.setBorderWidthTop(0);
        c.setBorderWidthLeft(0);
        c.setBorderWidthRight(0);
        c.setBorderColor(new Color(52, 152, 219));
        c.setPadding(0);
        line.addCell(c);
        doc.add(line);
        doc.add(Chunk.NEWLINE);
    }

    private PdfPTable table4() throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1, 2, 1, 2});
        t.setWidthPercentage(100);
        return t;
    }

    private void addRow(PdfPTable t, Font labelFont, Font valueFont,
                        String l1, String v1, String l2, String v2) {
        t.addCell(labelCell(l1, labelFont));
        t.addCell(cell(v1, valueFont));
        t.addCell(labelCell(l2, labelFont));
        t.addCell(cell(v2, valueFont));
    }

    private PdfPCell labelCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(new Color(236, 240, 241));
        c.setPadding(5);
        return c;
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "", font));
        c.setPadding(5);
        return c;
    }

    private String safe(String v)           { return v != null ? v : "—"; }
    private String date(Object d)           { return d != null ? d.toString() : "—"; }
}
