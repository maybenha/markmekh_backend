package com.panha.markmekh.service;

import com.panha.markmekh.dto.PayslipHistoryDto;
import com.panha.markmekh.entity.PayslipHistory;
import com.panha.markmekh.entity.User;
import com.panha.markmekh.repository.PayslipHistoryRepository;
import com.panha.markmekh.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayslipHistoryService {

    private final PayslipHistoryRepository repository;
    private final UserRepository userRepository;

    public PayslipHistoryService(PayslipHistoryRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public void savePayslip(String username, String employeeName, String employeeCode,
                            String invoiceNumber, String date, MultipartFile file) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PayslipHistory history = new PayslipHistory();
        history.setUser(user);
        history.setEmployeeName(employeeName);
        history.setEmployeeCode(employeeCode);
        history.setInvoiceNumber(invoiceNumber);
        history.setDate(date);
        history.setPdfData(file.getBytes());

        repository.save(history);
    }

    public List<PayslipHistoryDto> getHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return repository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(h -> new PayslipHistoryDto(
                        h.getId(), h.getEmployeeName(), h.getEmployeeCode(), h.getInvoiceNumber(), h.getDate()))
                .collect(Collectors.toList());
    }

    // In getPdfBytes:
    public byte[] getPdfBytes(Long id, String username) {
        PayslipHistory history = repository.findByIdWithUser(id) // 👈 Changed here
                .orElseThrow(() -> new RuntimeException("Payslip not found"));

        if (!history.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized");
        }
        return history.getPdfData();
    }

    // In deleteHistory:
    public void deleteHistory(Long id, String username) {
        PayslipHistory history = repository.findByIdWithUser(id) // 👈 Changed here
                .orElseThrow(() -> new RuntimeException("Payslip not found"));
        if (history.getUser().getUsername().equals(username)) {
            repository.delete(history);
        }
    }
}