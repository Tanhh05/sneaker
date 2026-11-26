package com.sneakerstore.repository;

import com.sneakerstore.entity.VoucherRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoucherRuleRepository extends JpaRepository<VoucherRule, Long> {

    @EntityGraph(attributePaths = {"voucher"})
    List<VoucherRule> findByDeletedFalseOrderByIdDesc();

    @EntityGraph(attributePaths = {"voucher"})
    List<VoucherRule> findByStatusAndDeletedFalseOrderByThresholdValueAsc(String status);

    @EntityGraph(attributePaths = {"voucher"})
    Optional<VoucherRule> findWithVoucherById(Long id);
}
