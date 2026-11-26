package com.sneakerstore.repository;

import com.sneakerstore.entity.InvoiceEmailHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceEmailHistoryRepository extends JpaRepository<InvoiceEmailHistory, Long> {
    List<InvoiceEmailHistory> findByInvoiceIdOrderByCreatedAtDescIdDesc(Long invoiceId);
    long countByInvoiceId(Long invoiceId);
}
