package com.sneakerstore.repository;

import com.sneakerstore.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    boolean existsByOrderCodeIgnoreCase(String orderCode);

    @EntityGraph(attributePaths = {"customer", "employee"})
    Optional<SalesOrder> findByOrderCodeIgnoreCase(String orderCode);

    @EntityGraph(attributePaths = {"customer", "employee"})
    Optional<SalesOrder> findWithHeaderById(Long id);

    @EntityGraph(attributePaths = {"customer", "employee"})
    List<SalesOrder> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    @EntityGraph(attributePaths = {"customer", "employee"})
    @Query("SELECT DISTINCT so FROM SalesOrder so " +
           "LEFT JOIN so.customer c " +
           "WHERE (:phone IS NOT NULL AND :phone <> '' AND TRIM(c.phone) = TRIM(CAST(:phone AS string))) " +
           "   OR (:email IS NOT NULL AND :email <> '' AND LOWER(TRIM(c.email)) = LOWER(TRIM(CAST(:email AS string)))) " +
           "ORDER BY so.orderDate DESC")
    List<SalesOrder> findOrdersByPhoneOrEmail(@Param("phone") String phone, @Param("email") String email);

    @EntityGraph(attributePaths = {"customer", "employee"})
    @Query(value = "SELECT so FROM SalesOrder so " +
           "LEFT JOIN so.customer c " +
           "LEFT JOIN so.employee e " +
           "WHERE (:keyword IS NULL OR :keyword = '' " +
           "OR LOWER(so.orderCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "OR (:altKeyword IS NOT NULL AND LOWER(so.orderCode) = LOWER(CAST(:altKeyword AS string))) " +
           "OR LOWER(COALESCE(so.note, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "OR (c IS NOT NULL AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(c.phone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))))) " +
           "AND (:status IS NULL OR :status = '' OR so.status = CAST(:status AS string)) " +
           "AND (:paymentMethod IS NULL OR :paymentMethod = '' " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) = 'COD' AND (so.paymentMethod = 'COD' OR (so.paymentMethod = 'CASH' AND so.employee IS NULL))) " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) = 'CASH' AND so.paymentMethod = 'CASH' AND so.employee IS NOT NULL) " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) = 'BANK_TRANSFER' AND so.paymentMethod = 'BANK_TRANSFER') " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) NOT IN ('COD', 'CASH', 'BANK_TRANSFER') AND UPPER(so.paymentMethod) = UPPER(CAST(:paymentMethod AS string)))) " +
           "AND (:channel IS NULL OR :channel = '' " +
           "   OR (UPPER(CAST(:channel AS string)) = 'ONLINE' AND so.employee IS NULL) " +
           "   OR (UPPER(CAST(:channel AS string)) = 'POS' AND so.employee IS NOT NULL)) " +
           "AND (CAST(:fromDate AS timestamp) IS NULL OR so.orderDate >= :fromDate) " +
           "AND (CAST(:toDate AS timestamp) IS NULL OR so.orderDate <= :toDate)",
           countQuery = "SELECT COUNT(so) FROM SalesOrder so " +
           "LEFT JOIN so.customer c " +
           "LEFT JOIN so.employee e " +
           "WHERE (:keyword IS NULL OR :keyword = '' " +
           "OR LOWER(so.orderCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "OR (:altKeyword IS NOT NULL AND LOWER(so.orderCode) = LOWER(CAST(:altKeyword AS string))) " +
           "OR LOWER(COALESCE(so.note, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "OR (c IS NOT NULL AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(c.phone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))))) " +
           "AND (:status IS NULL OR :status = '' OR so.status = CAST(:status AS string)) " +
           "AND (:paymentMethod IS NULL OR :paymentMethod = '' " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) = 'COD' AND (so.paymentMethod = 'COD' OR (so.paymentMethod = 'CASH' AND so.employee IS NULL))) " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) = 'CASH' AND so.paymentMethod = 'CASH' AND so.employee IS NOT NULL) " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) = 'BANK_TRANSFER' AND so.paymentMethod = 'BANK_TRANSFER') " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) NOT IN ('COD', 'CASH', 'BANK_TRANSFER') AND UPPER(so.paymentMethod) = UPPER(CAST(:paymentMethod AS string)))) " +
           "AND (:channel IS NULL OR :channel = '' " +
           "   OR (UPPER(CAST(:channel AS string)) = 'ONLINE' AND so.employee IS NULL) " +
           "   OR (UPPER(CAST(:channel AS string)) = 'POS' AND so.employee IS NOT NULL)) " +
           "AND (CAST(:fromDate AS timestamp) IS NULL OR so.orderDate >= :fromDate) " +
           "AND (CAST(:toDate AS timestamp) IS NULL OR so.orderDate <= :toDate)")
    Page<SalesOrder> searchOrders(@Param("keyword") String keyword,
                                  @Param("altKeyword") String altKeyword,
                                  @Param("status") String status,
                                  @Param("paymentMethod") String paymentMethod,
                                  @Param("channel") String channel,
                                  @Param("fromDate") LocalDateTime fromDate,
                                  @Param("toDate") LocalDateTime toDate,
                                  Pageable pageable);

    @Query("SELECT so.status, COUNT(so) FROM SalesOrder so " +
           "LEFT JOIN so.customer c " +
           "LEFT JOIN so.employee e " +
           "WHERE (:keyword IS NULL OR :keyword = '' " +
           "OR LOWER(so.orderCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "OR (:altKeyword IS NOT NULL AND LOWER(so.orderCode) = LOWER(CAST(:altKeyword AS string))) " +
           "OR LOWER(COALESCE(so.note, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "OR (c IS NOT NULL AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(c.phone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))))) " +
           "AND (:paymentMethod IS NULL OR :paymentMethod = '' " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) = 'COD' AND (so.paymentMethod = 'COD' OR (so.paymentMethod = 'CASH' AND so.employee IS NULL))) " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) = 'CASH' AND so.paymentMethod = 'CASH' AND so.employee IS NOT NULL) " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) = 'BANK_TRANSFER' AND so.paymentMethod = 'BANK_TRANSFER') " +
           "   OR (UPPER(CAST(:paymentMethod AS string)) NOT IN ('COD', 'CASH', 'BANK_TRANSFER') AND UPPER(so.paymentMethod) = UPPER(CAST(:paymentMethod AS string)))) " +
           "AND (:channel IS NULL OR :channel = '' " +
           "   OR (UPPER(CAST(:channel AS string)) = 'ONLINE' AND so.employee IS NULL) " +
           "   OR (UPPER(CAST(:channel AS string)) = 'POS' AND so.employee IS NOT NULL)) " +
           "AND (CAST(:fromDate AS timestamp) IS NULL OR so.orderDate >= :fromDate) " +
           "AND (CAST(:toDate AS timestamp) IS NULL OR so.orderDate <= :toDate) " +
           "GROUP BY so.status")
    List<Object[]> countOrdersByStatus(@Param("keyword") String keyword,
                                       @Param("altKeyword") String altKeyword,
                                       @Param("paymentMethod") String paymentMethod,
                                       @Param("channel") String channel,
                                       @Param("fromDate") LocalDateTime fromDate,
                                       @Param("toDate") LocalDateTime toDate);

    long countByStatusIn(List<String> statuses);

    @Query("SELECT COALESCE(SUM(so.finalAmount), 0) FROM SalesOrder so WHERE so.status = 'COMPLETED' " +
           "AND (CAST(:fromDate AS timestamp) IS NULL OR so.orderDate >= :fromDate) " +
           "AND (CAST(:toDate AS timestamp) IS NULL OR so.orderDate <= :toDate)")
    BigDecimal sumCompletedRevenue(@Param("fromDate") LocalDateTime fromDate,
                                   @Param("toDate") LocalDateTime toDate);

    @Query("SELECT COUNT(so) FROM SalesOrder so WHERE so.customer.id = :customerId AND so.status = 'COMPLETED'")
    long countCompletedByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(so.finalAmount), 0) FROM SalesOrder so WHERE so.customer.id = :customerId AND so.status = 'COMPLETED'")
    BigDecimal sumCompletedFinalAmountByCustomerId(@Param("customerId") Long customerId);
}

