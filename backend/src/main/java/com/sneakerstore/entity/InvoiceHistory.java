package com.sneakerstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "INVOICE_HISTORIES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_invoice_histories")
    @SequenceGenerator(name = "seq_invoice_histories", sequenceName = "SEQ_INVOICE_HISTORIES", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVOICE_ID", nullable = false)
    private Invoice invoice;

    @Column(name = "ACTION", nullable = false, length = 40)
    private String action;

    @Column(name = "FROM_STATUS", length = 30)
    private String fromStatus;

    @Column(name = "TO_STATUS", length = 30)
    private String toStatus;

    @Column(name = "ACTOR", nullable = false, length = 80)
    private String actor;

    @Column(name = "REASON", length = 500)
    private String reason;

    @Column(name = "DETAIL", length = 1000)
    private String detail;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
