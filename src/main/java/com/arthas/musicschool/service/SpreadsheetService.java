package com.arthas.musicschool.service;

import com.arthas.musicschool.model.Student;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SpreadsheetService {

    private static final String[] HEADERS = {
        "ID", "Nome", "CPF", "Nascimento", "Telefone", "E-mail", "Endereço",
        "Instrumento", "Nível", "Professor", "Início", "Mensalidade (R$)",
        "Vencimento (dia)", "Status", "Observações", "Dia da Aula", "Horário"
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

    public List<Student> importFromXlsx(File file) throws IOException {
        List<Student> students = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new FileInputStream(file))) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Student s = parseRow(row);
                if (s.getName() != null && !s.getName().isBlank()) students.add(s);
            }
        }
        return students;
    }

    private Student parseRow(Row row) {
        Student s = new Student();
        int id = (int) getLong(row, 0);
        if (id > 0) s.setId(id);
        s.setName(getString(row, 1));
        s.setCpf(getString(row, 2));
        String birth = getString(row, 3);
        if (!birth.isBlank()) { try { s.setBirthDate(LocalDate.parse(birth)); } catch (Exception ignored) {} }
        s.setPhone(getString(row, 4));
        s.setEmail(getString(row, 5));
        s.setAddress(getString(row, 6));
        s.setInstrument(getString(row, 7));
        s.setLevel(getString(row, 8));
        s.setTeacher(getString(row, 9));
        String start = getString(row, 10);
        if (!start.isBlank()) { try { s.setStartDate(LocalDate.parse(start)); } catch (Exception ignored) {} }
        s.setMonthlyFee(getDouble(row, 11));
        int dueDay = (int) getLong(row, 12);
        if (dueDay > 0) s.setPaymentDueDay(dueDay);
        String status = getString(row, 13);
        if (!status.isBlank()) s.setStatus(status);
        s.setNotes(getString(row, 14));
        s.setLessonDay(getString(row, 15));
        s.setLessonTime(getString(row, 16));
        return s;
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
        row.createCell(3).setCellValue(s.getBirthDate()  != null ? s.getBirthDate().toString()  : "");
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
        row.createCell(15).setCellValue(safe(s.getLessonDay()));
        row.createCell(16).setCellValue(safe(s.getLessonTime()));
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> "";
        };
    }

    private double getDouble(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return 0;
        return cell.getNumericCellValue();
    }

    private long getLong(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return 0;
        return (long) cell.getNumericCellValue();
    }

    private String safe(String v) { return v != null ? v : ""; }
}
