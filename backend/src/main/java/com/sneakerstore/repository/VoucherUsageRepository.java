package com.sneakerstore.repository;

import com.sneakerstore.entity.VoucherUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    long countByVoucherIdAndCustomerIdAndStatus(Long voucherId, Long customerId, String status);

    @EntityGraph(attributePaths = {"voucher", "customerVoucher", "salesOrder", "customer"})
    Optional<VoucherUsage> findFirstBySalesOrderIdAndStatus(Long salesOrderId, String status);

    @EntityGraph(attributePaths = {"voucher", "customerVoucher", "salesOrder", "customer"})
    List<VoucherUsage> findBySalesOrderId(Long salesOrderId);

    @EntityGraph(attributePaths = {"voucher", "customerVoucher", "salesOrder", "customer"})
    @Query("SELECT u FROM VoucherUsage u " +
           "LEFT JOIN u.salesOrder o " +
           "LEFT JOIN u.customer c " +
           "WHERE (:keyword IS NULL OR :keyword = '' " +
           "  OR LOWER(u.voucherCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "  OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "  OR (c IS NOT NULL AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "      OR LOWER(c.phone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))))) " +
           "AND (:channel IS NULL OR :channel = '' OR u.channel = CAST(:channel AS string)) " +
           "AND (:status IS NULL OR :status = '' OR u.status = CAST(:status AS string))")
    Page<VoucherUsage> searchUsages(
            @Param("keyword") String keyword,
            @Param("channel") String channel,
            @Param("status") String status,
            Pageable pageable);
}
