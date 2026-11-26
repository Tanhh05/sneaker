package com.sneakerstore.repository;

import com.sneakerstore.entity.InvoiceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceHistoryRepository extends JpaRepository<InvoiceHistory, Long> {
    List<InvoiceHistory> findByInvoiceIdOrderByCreatedAtDescIdDesc(Long invoiceId);
}
