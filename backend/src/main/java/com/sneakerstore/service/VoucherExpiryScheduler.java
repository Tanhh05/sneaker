package com.sneakerstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoucherExpiryScheduler {

    private final VoucherService voucherService;

    /**
     * Tác vụ định kỳ chạy mỗi ngày lúc 08:00 sáng (giờ Việt Nam):
     * - Chuyển trạng thái voucher cá nhân quá hạn sang EXPIRED
     * - Quét voucher sắp hết hạn (trước 3 ngày và trước 1 ngày) và tự động gửi email nhắc nhở
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    public void runDailyExpiryReminderScan() {
        try {
            log.info("[VoucherExpiryScheduler] Bắt đầu quét voucher sắp hết hạn định kỳ...");
            voucherService.scanAndSendExpiryReminders();
        } catch (Exception ex) {
            log.error("[VoucherExpiryScheduler] Lỗi khi quét voucher sắp hết hạn: {}", ex.getMessage(), ex);
        }
    }
}
