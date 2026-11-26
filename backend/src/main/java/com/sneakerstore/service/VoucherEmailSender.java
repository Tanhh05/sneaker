package com.sneakerstore.service;

import com.sneakerstore.entity.Customer;
import com.sneakerstore.entity.CustomerVoucher;
import com.sneakerstore.entity.SystemSetting;
import com.sneakerstore.entity.Voucher;
import com.sneakerstore.repository.CustomerVoucherRepository;
import com.sneakerstore.repository.SystemSettingRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoucherEmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SystemSettingRepository settingRepository;
    private final CustomerVoucherRepository customerVoucherRepository;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.invoice.from-email:no-reply@adhasneaker.vn}")
    private String fromEmail;

    @Value("${app.invoice.store-name:ADHA SNEAKER STORE}")
    private String storeName;

    @Value("${app.invoice.store-address:120 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh}")
    private String storeAddress;

    @Value("${app.invoice.store-hotline:0901 000 001}")
    private String storeHotline;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private String getSettingOrDefault(String key, String fallback) {
        return settingRepository.findById(key)
                .map(SystemSetting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(fallback);
    }

    private JavaMailSender resolveMailSender(String[] outFromEmail) {
        String effectiveHost = (mailHost != null && !mailHost.isBlank()) ? mailHost.trim() : getSettingOrDefault("SMTP_HOST", "");
        String effectiveUser = (mailUsername != null && !mailUsername.isBlank()) ? mailUsername.trim() : getSettingOrDefault("SMTP_USERNAME", "");
        String effectivePass = (mailPassword != null && !mailPassword.isBlank()) ? mailPassword.trim() : getSettingOrDefault("SMTP_PASSWORD", "");
        String effectiveFrom = getSettingOrDefault("SMTP_FROM", effectiveUser.isBlank() ? fromEmail : effectiveUser);
        int effectivePort = mailPort;
        try {
            effectivePort = Integer.parseInt(getSettingOrDefault("SMTP_PORT", String.valueOf(mailPort)).trim());
        } catch (Exception ignored) {
        }
        outFromEmail[0] = effectiveFrom;

        if (effectiveHost.isBlank() || effectiveUser.isBlank() || effectivePass.isBlank()) {
            return null;
        }
        if (mailHost != null && !mailHost.isBlank() && effectiveHost.equals(mailHost)) {
            JavaMailSender bean = mailSenderProvider.getIfAvailable();
            if (bean != null) return bean;
        }
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(effectiveHost);
        impl.setPort(effectivePort);
        impl.setUsername(effectiveUser);
        impl.setPassword(effectivePass);
        impl.setDefaultEncoding("UTF-8");
        Properties props = impl.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", effectivePort != 465 ? "true" : "false");
        if (effectivePort == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");
        return impl;
    }

    @Async("invoiceEmailExecutor")
    @Transactional
    public void sendVoucherAwardEmailAsync(Long customerVoucherId) {
        CustomerVoucher cv = customerVoucherRepository.findById(customerVoucherId).orElse(null);
        if (cv == null || cv.isAwardEmailSent()) return;
        Customer cust = cv.getCustomer();
        Voucher v = cv.getVoucher();
        if (cust == null || v == null || cust.getEmail() == null || cust.getEmail().isBlank()) return;

        String[] fromHolder = new String[1];
        JavaMailSender sender = resolveMailSender(fromHolder);
        if (sender == null) {
            log.info("[VoucherEmailSender] SMTP chưa cấu hình — bỏ qua gửi email tặng voucher {} tới {}", cv.getPersonalCode(), cust.getEmail());
            return;
        }

        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromHolder[0]);
            helper.setTo(cust.getEmail().trim());
            helper.setSubject("[ADHA SNEAKER] Chúc mừng bạn nhận được Voucher ưu đãi " + cv.getPersonalCode());
            helper.setText(buildAwardEmailHtml(cust, v, cv), true);
            sender.send(msg);

            cv.setAwardEmailSent(true);
            customerVoucherRepository.save(cv);
            log.info("[VoucherEmailSender] Đã gửi email tặng voucher {} tới {}", cv.getPersonalCode(), cust.getEmail());
        } catch (Exception ex) {
            log.warn("[VoucherEmailSender] Gửi email tặng voucher {} thất bại: {}", cv.getPersonalCode(), ex.getMessage());
        }
    }

    public boolean sendExpiryReminderEmail(CustomerVoucher cv, int daysLeft) {
        Customer cust = cv.getCustomer();
        Voucher v = cv.getVoucher();
        if (cust == null || v == null || cust.getEmail() == null || cust.getEmail().isBlank()) return false;

        String[] fromHolder = new String[1];
        JavaMailSender sender = resolveMailSender(fromHolder);
        if (sender == null) {
            log.info("[VoucherEmailSender] SMTP chưa cấu hình — bỏ qua nhắc hết hạn voucher {} (còn {} ngày) tới {}", cv.getPersonalCode(), daysLeft, cust.getEmail());
            return false;
        }

        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromHolder[0]);
            helper.setTo(cust.getEmail().trim());
            helper.setSubject("[ADHA SNEAKER] Voucher " + cv.getPersonalCode() + " chỉ còn " + daysLeft + " ngày trước khi hết hạn!");
            helper.setText(buildExpiryReminderHtml(cust, v, cv, daysLeft), true);
            sender.send(msg);

            LocalDate today = LocalDate.now();
            if (daysLeft <= 1) {
                cv.setExpiryReminded1dDate(today);
            } else {
                cv.setExpiryReminded3dDate(today);
            }
            customerVoucherRepository.save(cv);
            log.info("[VoucherEmailSender] Đã gửi email nhắc hết hạn ({}d) voucher {} tới {}", daysLeft, cv.getPersonalCode(), cust.getEmail());
            return true;
        } catch (Exception ex) {
            log.warn("[VoucherEmailSender] Gửi email nhắc hết hạn voucher {} thất bại: {}", cv.getPersonalCode(), ex.getMessage());
            return false;
        }
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(amount) + " ₫";
    }

    private String formatDiscount(Voucher v) {
        if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
            String base = "Giảm " + v.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
            if (v.getMaxDiscountAmount() != null && v.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                base += " (tối đa " + formatVnd(v.getMaxDiscountAmount()) + ")";
            }
            return base;
        }
        return "Giảm trực tiếp " + formatVnd(v.getDiscountValue());
    }

    private String formatChannel(String channel) {
        if ("POS".equalsIgnoreCase(channel)) return "Tại quầy cửa hàng (POS)";
        if ("ONLINE".equalsIgnoreCase(channel)) return "Đặt hàng trực tuyến trên Website";
        return "Cả tại quầy POS và đặt hàng Online";
    }

    private String buildAwardEmailHtml(Customer cust, Voucher v, CustomerVoucher cv) {
        String exp = cv.getExpiresAt() != null ? cv.getExpiresAt().format(DT_FMT) : "-";
        return "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;color:#0f172a;border:1px solid #e2e8f0;border-radius:8px;overflow:hidden\">"
                + "<div style=\"background:#000000;color:#ffffff;padding:20px 24px\">"
                + "<h2 style=\"margin:0;font-size:18px;letter-spacing:1px\">" + storeName + "</h2>"
                + "<p style=\"margin:4px 0 0;font-size:12px;color:#d4d4d4\">Quà tặng Voucher tri ân khách hàng thân thiết</p>"
                + "</div>"
                + "<div style=\"padding:24px;line-height:1.6;font-size:14px\">"
                + "<p>Xin chào <strong>" + cust.getFullName() + "</strong>,</p>"
                + "<p>Chúc mừng bạn đã đạt mốc ưu đãi khách hàng thân thiết tại <strong>ADHA Sneaker</strong>! Chúng tôi gửi tặng bạn một mã Voucher đặc biệt dành riêng cho tài khoản của bạn:</p>"
                + "<div style=\"background:#fafafa;border:2px dashed #000000;padding:16px;text-align:center;margin:16px 0\">"
                + "<div style=\"font-size:11px;letter-spacing:1px;color:#737373;text-transform:uppercase\">MÃ VOUCHER CÁ NHÂN CỦA BẠN</div>"
                + "<div style=\"font-size:24px;font-weight:900;letter-spacing:2px;color:#000000;margin:6px 0\">" + cv.getPersonalCode() + "</div>"
                + "<div style=\"font-size:14px;font-weight:700;color:#dc2626\">" + formatDiscount(v) + "</div>"
                + "</div>"
                + "<table style=\"width:100%;border-collapse:collapse;font-size:13.5px;background:#f8fafc;border:1px solid #e2e8f0\">"
                + "<tr><td style=\"padding:8px 12px;color:#64748b\">Chương trình:</td><td style=\"padding:8px 12px;font-weight:bold\">" + v.getName() + "</td></tr>"
                + "<tr><td style=\"padding:8px 12px;color:#64748b\">Đơn hàng tối thiểu:</td><td style=\"padding:8px 12px\">" + formatVnd(v.getMinOrderAmount()) + "</td></tr>"
                + "<tr><td style=\"padding:8px 12px;color:#64748b\">Kênh áp dụng:</td><td style=\"padding:8px 12px\">" + formatChannel(v.getChannel()) + "</td></tr>"
                + "<tr><td style=\"padding:8px 12px;color:#64748b\">Hạn sử dụng đến:</td><td style=\"padding:8px 12px;font-weight:bold;color:#dc2626\">" + exp + "</td></tr>"
                + "</table>"
                + "<p style=\"margin-top:16px\"><strong>Hướng dẫn sử dụng:</strong><br/>"
                + "• <strong>Mua Online:</strong> Đăng nhập bằng tài khoản/SĐT của bạn, chọn mã từ <em>Kho Voucher của tôi</em> ngay tại bước Giỏ hàng & Thanh toán.<br/>"
                + "• <strong>Mua tại cửa hàng (POS):</strong> Đọc SĐT hoặc mã <strong>" + cv.getPersonalCode() + "</strong> cho nhân viên thu ngân để được trừ trực tiếp vào hóa đơn.</p>"
                + "<p style=\"font-size:12.5px;color:#64748b;margin-top:18px\">" + storeAddress + " | Hotline: " + storeHotline + "</p>"
                + "</div></div>";
    }

    private String buildExpiryReminderHtml(Customer cust, Voucher v, CustomerVoucher cv, int daysLeft) {
        String exp = cv.getExpiresAt() != null ? cv.getExpiresAt().format(DT_FMT) : "-";
        return "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;color:#0f172a;border:1px solid #e2e8f0;border-radius:8px;overflow:hidden\">"
                + "<div style=\"background:#000000;color:#ffffff;padding:20px 24px\">"
                + "<h2 style=\"margin:0;font-size:18px;letter-spacing:1px\">" + storeName + "</h2>"
                + "<p style=\"margin:4px 0 0;font-size:12px;color:#fca5a5\">Nhắc nhở Voucher ưu đãi sắp hết hạn</p>"
                + "</div>"
                + "<div style=\"padding:24px;line-height:1.6;font-size:14px\">"
                + "<p>Xin chào <strong>" + cust.getFullName() + "</strong>,</p>"
                + "<p>Voucher ưu đãi cá nhân của bạn chỉ còn <strong>" + daysLeft + " ngày</strong> nữa là hết hiệu lực. Đừng bỏ lỡ cơ hội sở hữu đôi sneaker yêu thích với mức giá tốt nhất!</p>"
                + "<div style=\"background:#fef2f2;border:2px dashed #dc2626;padding:16px;text-align:center;margin:16px 0\">"
                + "<div style=\"font-size:12px;color:#991b1b;font-weight:700\">SẮP HẾT HẠN: " + exp + "</div>"
                + "<div style=\"font-size:24px;font-weight:900;letter-spacing:2px;color:#000000;margin:6px 0\">" + cv.getPersonalCode() + "</div>"
                + "<div style=\"font-size:14px;font-weight:700;color:#dc2626\">" + formatDiscount(v) + " · Đơn từ " + formatVnd(v.getMinOrderAmount()) + "</div>"
                + "</div>"
                + "<p>Bạn có thể áp dụng trực tiếp tại giỏ hàng Online hoặc đọc số điện thoại cho thu ngân khi ghé cửa hàng <strong>ADHA Sneaker</strong>.</p>"
                + "<p style=\"font-size:12.5px;color:#64748b;margin-top:18px\">" + storeAddress + " | Hotline: " + storeHotline + "</p>"
                + "</div></div>";
    }
}
