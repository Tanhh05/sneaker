package com.sneakerstore.repository;

import com.sneakerstore.entity.InvoiceCancelRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceCancelRequestRepository extends JpaRepository<InvoiceCancelRequest, Long> {
    List<InvoiceCancelRequest> findByInvoiceIdOrderByRequestedAtDescIdDesc(Long invoiceId);
    Optional<InvoiceCancelRequest> findFirstByInvoiceIdAndStatusOrderByRequestedAtDesc(Long invoiceId, String status);
    boolean existsByInvoiceIdAndStatus(Long invoiceId, String status);
    List<InvoiceCancelRequest> findByStatusOrderByRequestedAtDesc(String status);
}
