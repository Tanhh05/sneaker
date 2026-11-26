package com.sneakerstore.repository;

import com.sneakerstore.entity.InvoiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, Long> {
    List<InvoiceDetail> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
