package com.sneakerstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "INVOICE_EMAIL_HISTORIES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceEmailHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_invoice_email_histories")
    @SequenceGenerator(name = "seq_invoice_email_histories", sequenceName = "SEQ_INVOICE_EMAIL_HISTORIES", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVOICE_ID", nullable = false)
    private Invoice invoice;

    @Column(name = "RECIPIENT_EMAIL", nullable = false, length = 150)
    private String recipientEmail;

    @Column(name = "SUBJECT", nullable = false, length = 255)
    private String subject;

    @Column(name = "SEND_STATUS", nullable = false, length = 20)
    private String sendStatus; // SENDING, SENT, FAILED

    @Column(name = "SENT_BY", nullable = false, length = 80)
    private String sentBy;

    @Column(name = "SENT_AT")
    private LocalDateTime sentAt;

    @Column(name = "RETRY_COUNT", nullable = false)
    private Integer retryCount;

    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (retryCount == null) retryCount = 0;
    }
}
