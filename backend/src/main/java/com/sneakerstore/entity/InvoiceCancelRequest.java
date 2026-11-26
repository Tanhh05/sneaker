package com.sneakerstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "INVOICE_CANCEL_REQUESTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceCancelRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_invoice_cancel_requests")
    @SequenceGenerator(name = "seq_invoice_cancel_requests", sequenceName = "SEQ_INVOICE_CANCEL_REQUESTS", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVOICE_ID", nullable = false)
    private Invoice invoice;

    @Column(name = "REQUESTED_BY", nullable = false, length = 80)
    private String requestedBy;

    @Column(name = "REASON", nullable = false, length = 500)
    private String reason;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "REVIEWED_BY", length = 80)
    private String reviewedBy;

    @Column(name = "REVIEW_NOTE", length = 500)
    private String reviewNote;

    @Column(name = "REQUESTED_AT", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "REVIEWED_AT")
    private LocalDateTime reviewedAt;

    @PrePersist
    public void prePersist() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
