package com.sneakerstore.repository;

import com.sneakerstore.entity.CustomerVoucher;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Long> {

    @EntityGraph(attributePaths = {"customer", "voucher", "rule"})
    Optional<CustomerVoucher> findByPersonalCodeIgnoreCase(String personalCode);

    @EntityGraph(attributePaths = {"customer", "voucher", "rule"})
    List<CustomerVoucher> findByCustomerIdAndStatusOrderByExpiresAtAsc(Long customerId, String status);

    @EntityGraph(attributePaths = {"customer", "voucher", "rule"})
    List<CustomerVoucher> findByCustomerIdOrderByIssuedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"customer", "voucher", "rule"})
    Optional<CustomerVoucher> findFirstByCustomerIdAndVoucherIdAndStatusAndExpiresAtAfterOrderByExpiresAtAsc(
            Long customerId, Long voucherId, String status, LocalDateTime now);

    long countByCustomerIdAndRuleId(Long customerId, Long ruleId);

    boolean existsByCustomerIdAndRuleIdAndSourceOrderId(Long customerId, Long ruleId, Long sourceOrderId);

    boolean existsByPersonalCodeIgnoreCase(String personalCode);

    @EntityGraph(attributePaths = {"customer", "voucher", "rule"})
    @Query("SELECT cv FROM CustomerVoucher cv WHERE cv.status = 'AVAILABLE' AND cv.expiresAt > :now AND cv.expiresAt <= :cutoff")
    List<CustomerVoucher> findExpiringAvailableVouchers(@Param("now") LocalDateTime now, @Param("cutoff") LocalDateTime cutoff);

    @EntityGraph(attributePaths = {"customer", "voucher", "rule"})
    @Query("SELECT cv FROM CustomerVoucher cv WHERE cv.status = 'AVAILABLE' AND cv.expiresAt <= :now")
    List<CustomerVoucher> findExpiredStillAvailable(@Param("now") LocalDateTime now);
}
