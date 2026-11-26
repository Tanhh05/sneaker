package com.sneakerstore.repository;

import com.sneakerstore.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByInvoiceCodeIgnoreCase(String invoiceCode);

    @EntityGraph(attributePaths = {"salesOrder", "salesOrder.employee", "customer", "originalInvoice"})
    Optional<Invoice> findWithRelationsById(Long id);

    @EntityGraph(attributePaths = {"salesOrder", "salesOrder.employee", "customer", "originalInvoice"})
    @Query("SELECT i FROM Invoice i WHERE i.salesOrder.id = :orderId AND i.recordKind = 'ORIGINAL'")
    Optional<Invoice> findOriginalByOrderId(@Param("orderId") Long orderId);

    @EntityGraph(attributePaths = {"salesOrder", "customer"})
    List<Invoice> findBySalesOrderIdOrderByIssuedAtDesc(Long orderId);

    @EntityGraph(attributePaths = {"salesOrder", "customer"})
    List<Invoice> findByOriginalInvoiceIdOrderByIssuedAtDesc(Long originalInvoiceId);

    @EntityGraph(attributePaths = {"salesOrder", "salesOrder.employee", "customer", "originalInvoice"})
    @Query(value = "SELECT i FROM Invoice i " +
            "JOIN i.salesOrder so " +
            "LEFT JOIN so.employee emp " +
            "LEFT JOIN emp.user empUser " +
            "WHERE (:keyword IS NULL OR :keyword = '' " +
            "   OR LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(so.orderCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(i.customerName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(COALESCE(i.companyName, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(COALESCE(i.customerPhone, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(COALESCE(i.customerEmail, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(COALESCE(i.taxCode, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
            "AND (:saleChannel IS NULL OR :saleChannel = '' OR i.saleChannel = CAST(:saleChannel AS string)) " +
            "AND (:invoiceType IS NULL OR :invoiceType = '' OR i.invoiceType = CAST(:invoiceType AS string)) " +
            "AND (:invoiceStatus IS NULL OR :invoiceStatus = '' OR i.invoiceStatus = CAST(:invoiceStatus AS string)) " +
            "AND (:emailStatus IS NULL OR :emailStatus = '' OR i.emailStatus = CAST(:emailStatus AS string)) " +
            "AND (CAST(:fromDate AS timestamp) IS NULL OR i.issuedAt >= :fromDate) " +
            "AND (CAST(:toDate AS timestamp) IS NULL OR i.issuedAt <= :toDate) " +
            "AND (:scopedUsername IS NULL OR :scopedUsername = '' " +
            "   OR LOWER(COALESCE(i.createdBy, '')) = LOWER(CAST(:scopedUsername AS string)) " +
            "   OR LOWER(COALESCE(so.createdBy, '')) = LOWER(CAST(:scopedUsername AS string)) " +
            "   OR LOWER(COALESCE(empUser.username, '')) = LOWER(CAST(:scopedUsername AS string)))",
            countQuery = "SELECT COUNT(i) FROM Invoice i " +
            "JOIN i.salesOrder so " +
            "LEFT JOIN so.employee emp " +
            "LEFT JOIN emp.user empUser " +
            "WHERE (:keyword IS NULL OR :keyword = '' " +
            "   OR LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(so.orderCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(i.customerName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(COALESCE(i.companyName, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(COALESCE(i.customerPhone, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(COALESCE(i.customerEmail, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "   OR LOWER(COALESCE(i.taxCode, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
            "AND (:saleChannel IS NULL OR :saleChannel = '' OR i.saleChannel = CAST(:saleChannel AS string)) " +
            "AND (:invoiceType IS NULL OR :invoiceType = '' OR i.invoiceType = CAST(:invoiceType AS string)) " +
            "AND (:invoiceStatus IS NULL OR :invoiceStatus = '' OR i.invoiceStatus = CAST(:invoiceStatus AS string)) " +
            "AND (:emailStatus IS NULL OR :emailStatus = '' OR i.emailStatus = CAST(:emailStatus AS string)) " +
            "AND (CAST(:fromDate AS timestamp) IS NULL OR i.issuedAt >= :fromDate) " +
            "AND (CAST(:toDate AS timestamp) IS NULL OR i.issuedAt <= :toDate) " +
            "AND (:scopedUsername IS NULL OR :scopedUsername = '' " +
            "   OR LOWER(COALESCE(i.createdBy, '')) = LOWER(CAST(:scopedUsername AS string)) " +
            "   OR LOWER(COALESCE(so.createdBy, '')) = LOWER(CAST(:scopedUsername AS string)) " +
            "   OR LOWER(COALESCE(empUser.username, '')) = LOWER(CAST(:scopedUsername AS string)))")
    Page<Invoice> searchInvoices(@Param("keyword") String keyword,
                                 @Param("saleChannel") String saleChannel,
                                 @Param("invoiceType") String invoiceType,
                                 @Param("invoiceStatus") String invoiceStatus,
                                 @Param("emailStatus") String emailStatus,
                                 @Param("fromDate") LocalDateTime fromDate,
                                 @Param("toDate") LocalDateTime toDate,
                                 @Param("scopedUsername") String scopedUsername,
                                 Pageable pageable);

    @Query("SELECT " +
            "COUNT(i), " +
            "COALESCE(SUM(CASE WHEN i.invoiceStatus <> 'CANCELLED' THEN i.totalAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN i.invoiceStatus <> 'CANCELLED' THEN i.taxAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN i.saleChannel = 'POS' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN i.saleChannel = 'ONLINE' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN i.invoiceStatus = 'CANCELLED' THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN i.invoiceStatus = 'PENDING_CANCEL' THEN 1 ELSE 0 END), 0) " +
            "FROM Invoice i " +
            "JOIN i.salesOrder so " +
            "LEFT JOIN so.employee emp " +
            "LEFT JOIN emp.user empUser " +
            "WHERE (CAST(:fromDate AS timestamp) IS NULL OR i.issuedAt >= :fromDate) " +
            "AND (CAST(:toDate AS timestamp) IS NULL OR i.issuedAt <= :toDate) " +
            "AND (:scopedUsername IS NULL OR :scopedUsername = '' " +
            "   OR LOWER(COALESCE(i.createdBy, '')) = LOWER(CAST(:scopedUsername AS string)) " +
            "   OR LOWER(COALESCE(so.createdBy, '')) = LOWER(CAST(:scopedUsername AS string)) " +
            "   OR LOWER(COALESCE(empUser.username, '')) = LOWER(CAST(:scopedUsername AS string)))")
    Object[] aggregateKpi(@Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate,
                          @Param("scopedUsername") String scopedUsername);
}
