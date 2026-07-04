package com.arthas.musicschool.service;

import com.arthas.musicschool.model.FinancialReport;
import com.arthas.musicschool.model.FinancialReportItem;
import com.arthas.musicschool.model.Lesson;
import com.arthas.musicschool.model.Payment;
import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.repository.LessonRepository;
import com.arthas.musicschool.repository.PaymentRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
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

        // Cabeçalho com foto à esquerda e título à direita
        PdfPTable header = new PdfPTable(new float[]{1, 4});
        header.setWidthPercentage(100);
        header.setSpacingAfter(10);

        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorder(Rectangle.NO_BORDER);
        photoCell.setPadding(4);
        byte[] photoData = student.getPhoto();
        if (photoData != null && photoData.length > 0) {
            try {
                Image img = Image.getInstance(photoData);
                img.scaleToFit(80, 80);
                photoCell.addElement(img);
            } catch (Exception ignored) {}
        }
        header.addCell(photoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(4);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph titlePar = new Paragraph(student.getName() != null ? student.getName() : "FICHA CADASTRAL", titleFont);
        titlePar.setSpacingAfter(3);
        titleCell.addElement(titlePar);
        titleCell.addElement(new Paragraph("FICHA CADASTRAL DO ALUNO  •  Music School",
                new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY)));
        header.addCell(titleCell);

        doc.add(header);

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

    public void generateFinancialReport(FinancialReport report, File file) throws Exception {
        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();

        Font titleFont   = new Font(Font.HELVETICA, 16, Font.BOLD,   new Color(44, 62, 80));
        Font subFont     = new Font(Font.HELVETICA, 10, Font.ITALIC,  Color.GRAY);
        Font sectionFont = new Font(Font.HELVETICA, 11, Font.BOLD,   new Color(52, 152, 219));
        Font thFont      = new Font(Font.HELVETICA,  9, Font.BOLD,   Color.WHITE);
        Font tdFont      = new Font(Font.HELVETICA,  9, Font.NORMAL, Color.BLACK);
        Font cardLbl     = new Font(Font.HELVETICA,  8, Font.BOLD,   Color.GRAY);
        Font cardVal     = new Font(Font.HELVETICA, 13, Font.BOLD,   Color.BLACK);

        // Título
        Paragraph title = new Paragraph("RELATÓRIO FINANCEIRO MENSAL", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);
        Paragraph sub = new Paragraph("Music School  •  " + report.getMonthLabel(), subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        doc.add(sub);
        doc.add(Chunk.NEWLINE);

        // Cards de totais (3 colunas)
        PdfPTable cards = new PdfPTable(3);
        cards.setWidthPercentage(100);
        cards.setSpacingAfter(14);
        cards.addCell(financialCard("TOTAL PREVISTO",
                String.format("R$ %.2f", report.getTotalPrevisto()),
                new Color(41, 128, 185), cardLbl, cardVal));
        cards.addCell(financialCard("TOTAL APURADO",
                String.format("R$ %.2f", report.getTotalApurado()),
                new Color(39, 174, 96), cardLbl, cardVal));
        cards.addCell(financialCard("TOTAL PENDENTE",
                String.format("R$ %.2f", report.getTotalPendente()),
                new Color(231, 76, 60), cardLbl, cardVal));
        doc.add(cards);

        // Resumo numérico
        int total   = report.getItems().size();
        long pagos  = report.getItems().stream().filter(i -> "Pago".equals(i.getPaymentStatus())).count();
        long pend   = report.getItems().stream().filter(i ->
                "Pendente".equals(i.getPaymentStatus()) || "Atrasado".equals(i.getPaymentStatus())
                || "Sem registro".equals(i.getPaymentStatus())).count();

        addSection(doc, sectionFont, "RESUMO");
        doc.add(new Paragraph(
            "Alunos ativos: " + total +
            "   |   Pagos: " + pagos +
            "   |   Pendentes: " + pend,
            new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK)));
        doc.add(Chunk.NEWLINE);

        // Tabela detalhada
        addSection(doc, sectionFont, "DETALHAMENTO POR ALUNO");
        PdfPTable tbl = new PdfPTable(new float[]{3, 2, 2, 2, 2, 2});
        tbl.setWidthPercentage(100);
        for (String h : new String[]{"Nome", "Instrumento", "Mensalidade", "Status", "Valor Pago", "Data Pagamento"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thFont));
            c.setBackgroundColor(new Color(44, 62, 80));
            c.setPadding(5);
            tbl.addCell(c);
        }
        for (FinancialReportItem item : report.getItems()) {
            Color statusColor = switch (item.getPaymentStatus()) {
                case "Pago"         -> new Color(39, 174, 96);
                case "Isento"       -> new Color(41, 128, 185);
                case "Atrasado"     -> new Color(231, 76, 60);
                case "Sem registro" -> new Color(149, 165, 166);
                default             -> new Color(230, 126, 34);
            };
            tbl.addCell(cell(item.getStudentName(), tdFont));
            tbl.addCell(cell(safe(item.getInstrument()), tdFont));
            tbl.addCell(cell(String.format("R$ %.2f", item.getMonthlyFee()), tdFont));
            tbl.addCell(cell(item.getPaymentStatus(), new Font(Font.HELVETICA, 9, Font.BOLD, statusColor)));
            tbl.addCell(cell(item.getAmountPaid() > 0 ? String.format("R$ %.2f", item.getAmountPaid()) : "—", tdFont));
            tbl.addCell(cell(item.getPaidDate() != null && !item.getPaidDate().isBlank() ? item.getPaidDate() : "—", tdFont));
        }
        doc.add(tbl);

        doc.close();
    }

    private PdfPCell financialCard(String label, String value, Color accent, Font lFont, Font vFont) {
        PdfPCell c = new PdfPCell();
        c.setPadding(10);
        c.setBorderColor(accent);
        c.setBorderWidth(2);
        Paragraph lp = new Paragraph(label, lFont);
        lp.setAlignment(Element.ALIGN_CENTER);
        Paragraph vp = new Paragraph(value, new Font(Font.HELVETICA, 13, Font.BOLD, accent));
        vp.setAlignment(Element.ALIGN_CENTER);
        c.addElement(lp);
        c.addElement(vp);
        return c;
    }

    private String safe(String v)           { return v != null ? v : "—"; }
    private String date(Object d)           { return d != null ? d.toString() : "—"; }
}
