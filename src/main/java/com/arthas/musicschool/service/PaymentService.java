package com.arthas.musicschool.service;

import com.arthas.musicschool.model.Payment;
import com.arthas.musicschool.model.Student;
import com.arthas.musicschool.repository.PaymentRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class PaymentService {

    private final PaymentRepository repository;

    public PaymentService() { this.repository = new PaymentRepository(); }
    public PaymentService(PaymentRepository repository) { this.repository = repository; }

    public List<Payment> getPayments(int studentId) throws SQLException {
        return repository.findByStudent(studentId);
    }

    public Payment save(Payment payment) throws SQLException {
        return repository.save(payment);
    }

    public void delete(int id) throws SQLException {
        repository.delete(id);
    }

    public Payment markAsPaid(int studentId, String referenceMonth, double amount) throws SQLException {
        Payment existing = repository.findByStudentAndMonth(studentId, referenceMonth);
        if (existing == null) {
            existing = new Payment(studentId, referenceMonth, amount);
        }
        existing.setStatus("Pago");
        existing.setPaidDate(LocalDate.now().toString());
        existing.setAmount(amount);
        return repository.save(existing);
    }

    /**
     * Gera registros de pagamento para todos os meses desde o início até o mês atual,
     * criando apenas os meses que ainda não existem no banco.
     */
    public void generatePendingMonths(Student student) throws SQLException {
        if (student.getStartDate() == null) return;

        YearMonth start = YearMonth.from(student.getStartDate());
        YearMonth current = YearMonth.now();

        for (YearMonth ym = start; !ym.isAfter(current); ym = ym.plusMonths(1)) {
            String ref = ym.toString(); // yyyy-MM
            Payment existing = repository.findByStudentAndMonth(student.getId(), ref);
            if (existing == null) {
                Payment p = new Payment(student.getId(), ref, student.getMonthlyFee());
                // Marca como atrasado se o vencimento já passou
                LocalDate dueDate = ym.atDay(Math.min(student.getPaymentDueDay(), ym.lengthOfMonth()));
                if (dueDate.isBefore(LocalDate.now())) {
                    p.setStatus("Atrasado");
                } else {
                    p.setStatus("Pendente");
                }
                repository.save(p);
            }
        }
    }

    public long countPending() throws SQLException {
        return repository.countByStatus("Pendente") + repository.countByStatus("Atrasado");
    }
}
