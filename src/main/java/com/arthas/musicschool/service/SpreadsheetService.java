package com.arthas.musicschool.service;

import com.arthas.musicschool.model.Student;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.List;

public class SpreadsheetService {

    private static final String[] HEADERS = {
        "ID", "Nome", "CPF", "Nascimento", "Telefone", "E-mail", "Endereço",
        "Instrumento", "Nível", "Professor", "Início", "Mensalidade (R$)",
        "Vencimento (dia)", "Status", "Observações"
    };

    public void exportToXlsx(List<Student> students, File file) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Alunos");
            createHeader(wb, sheet);
            for (int i = 0; i < students.size(); i++) {
                writeRow(sheet.createRow(i + 1), students.get(i));
            }
            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }
        }
    }

    private void createHeader(Workbook wb, Sheet sheet) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row row = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeRow(Row row, Student s) {
        row.createCell(0).setCellValue(s.getId());
        row.createCell(1).setCellValue(safe(s.getName()));
        row.createCell(2).setCellValue(safe(s.getCpf()));
        row.createCell(3).setCellValue(s.getBirthDate() != null ? s.getBirthDate().toString() : "");
        row.createCell(4).setCellValue(safe(s.getPhone()));
        row.createCell(5).setCellValue(safe(s.getEmail()));
        row.createCell(6).setCellValue(safe(s.getAddress()));
        row.createCell(7).setCellValue(safe(s.getInstrument()));
        row.createCell(8).setCellValue(safe(s.getLevel()));
        row.createCell(9).setCellValue(safe(s.getTeacher()));
        row.createCell(10).setCellValue(s.getStartDate() != null ? s.getStartDate().toString() : "");
        row.createCell(11).setCellValue(s.getMonthlyFee());
        row.createCell(12).setCellValue(s.getPaymentDueDay());
        row.createCell(13).setCellValue(safe(s.getStatus()));
        row.createCell(14).setCellValue(safe(s.getNotes()));
    }

    private String safe(String v) { return v != null ? v : ""; }
}
