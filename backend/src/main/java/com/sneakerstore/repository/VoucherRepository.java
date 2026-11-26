package com.sneakerstore.repository;

import com.sneakerstore.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndIdNotAndDeletedFalse(String code, Long id);

    @Query("SELECT v FROM Voucher v WHERE v.deleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' " +
           "  OR LOWER(v.code) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "  OR LOWER(v.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
           "AND (:channel IS NULL OR :channel = '' OR v.channel = CAST(:channel AS string)) " +
           "AND (:discountType IS NULL OR :discountType = '' OR v.discountType = CAST(:discountType AS string)) " +
           "AND (:voucherType IS NULL OR :voucherType = '' OR v.voucherType = CAST(:voucherType AS string)) " +
           "AND (:status IS NULL OR :status = '' OR v.status = CAST(:status AS string)) " +
           "AND (CAST(:fromDate AS timestamp) IS NULL OR v.endDate >= :fromDate) " +
           "AND (CAST(:toDate AS timestamp) IS NULL OR v.startDate <= :toDate)")
    Page<Voucher> searchVouchers(
            @Param("keyword") String keyword,
            @Param("channel") String channel,
            @Param("discountType") String discountType,
            @Param("voucherType") String voucherType,
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT v FROM Voucher v WHERE v.deleted = false AND v.status = 'ACTIVE' " +
           "AND v.voucherType = 'PUBLIC' " +
           "AND v.startDate <= :now AND v.endDate >= :now " +
           "AND (v.channel = 'ALL' OR v.channel = :channel) " +
           "AND (v.totalQuantity IS NULL OR v.usedCount < v.totalQuantity) " +
           "ORDER BY v.discountValue DESC")
    List<Voucher> findActivePublicVouchersForChannel(@Param("channel") String channel, @Param("now") LocalDateTime now);

    List<Voucher> findByDeletedFalseOrderByIdDesc();
}
