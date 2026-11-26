package com.sneakerstore.service;

import com.sneakerstore.entity.Invoice;
import com.sneakerstore.entity.InvoiceDetail;
import com.sneakerstore.entity.InvoiceEmailHistory;
import com.sneakerstore.entity.InvoiceHistory;
import com.sneakerstore.entity.SystemSetting;
import com.sneakerstore.exporter.InvoicePdfGenerator;
import com.sneakerstore.repository.InvoiceDetailRepository;
import com.sneakerstore.repository.InvoiceEmailHistoryRepository;
import com.sneakerstore.repository.InvoiceHistoryRepository;
import com.sneakerstore.repository.InvoiceRepository;
import com.sneakerstore.repository.SystemSettingRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository detailRepository;
    private final InvoiceEmailHistoryRepository emailHistoryRepository;
    private final InvoiceHistoryRepository historyRepository;
    private final SystemSettingRepository settingRepository;
    private final InvoicePdfGenerator pdfGenerator;

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

    private static final int MAX_RETRIES = 2;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private String getSettingOrDefault(String key, String fallback) {
        return settingRepository.findById(key)
                .map(SystemSetting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(fallback);
    }

    private JavaMailSender resolveMailSender(String effectiveHost, int effectivePort, String effectiveUser, String effectivePass) {
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
    public void sendInvoiceEmailAsync(Long invoiceId, Long emailHistoryId, String actor) {
        Invoice invoice = invoiceRepository.findWithRelationsById(invoiceId).orElse(null);
        InvoiceEmailHistory emailHist = emailHistoryRepository.findById(emailHistoryId).orElse(null);
        if (invoice == null || emailHist == null) {
            return;
        }

        String recipient = emailHist.getRecipientEmail();
        String effectiveHost = (mailHost != null && !mailHost.isBlank()) ? mailHost.trim() : getSettingOrDefault("SMTP_HOST", "");
        String effectiveUser = (mailUsername != null && !mailUsername.isBlank()) ? mailUsername.trim() : getSettingOrDefault("SMTP_USERNAME", "");
        String effectivePass = (mailPassword != null && !mailPassword.isBlank()) ? mailPassword.trim() : getSettingOrDefault("SMTP_PASSWORD", "");
        String effectiveFrom = getSettingOrDefault("SMTP_FROM", effectiveUser.isBlank() ? fromEmail : effectiveUser);
        int effectivePort = mailPort;
        try {
            String pStr = getSettingOrDefault("SMTP_PORT", String.valueOf(mailPort));
            effectivePort = Integer.parseInt(pStr.trim());
        } catch (Exception ignored) {
        }

        if (effectiveHost.isBlank() || effectiveUser.isBlank() || effectivePass.isBlank()) {
            markFailed(invoice, emailHist, actor,
                    "Chưa cấu hình tài khoản SMTP (cập nhật bảng system_settings: SMTP_HOST, SMTP_USERNAME, SMTP_PASSWORD hoặc biến môi trường MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD). Hóa đơn vẫn giữ nguyên trạng thái đã phát hành.");
            return;
        }

        JavaMailSender mailSender = resolveMailSender(effectiveHost, effectivePort, effectiveUser, effectivePass);

        List<InvoiceDetail> details = detailRepository.findByInvoiceIdOrderByIdAsc(invoiceId);
        byte[] pdfBytes;
        try {
            pdfBytes = pdfGenerator.generate(invoice, details);
        } catch (Exception ex) {
            markFailed(invoice, emailHist, actor, "Lỗi tạo file PDF đính kèm: " + ex.getMessage());
            return;
        }

        int attempt = 0;
        Exception lastError = null;
        while (attempt <= MAX_RETRIES) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(effectiveFrom);
                helper.setTo(recipient);
                helper.setSubject(emailHist.getSubject());
                helper.setText(buildEmailHtml(invoice), true);
                helper.addAttachment("HoaDon_" + invoice.getInvoiceCode() + ".pdf", new ByteArrayResource(pdfBytes));

                mailSender.send(message);

                emailHist.setSendStatus("SENT");
                emailHist.setSentAt(LocalDateTime.now());
                emailHist.setRetryCount(attempt);
                emailHist.setErrorMessage(null);
                emailHistoryRepository.save(emailHist);

                invoice.setEmailStatus("SENT");
                invoiceRepository.save(invoice);

                historyRepository.save(InvoiceHistory.builder()
                        .invoice(invoice)
                        .action("EMAIL_SENT")
                        .fromStatus(invoice.getInvoiceStatus())
                        .toStatus(invoice.getInvoiceStatus())
                        .actor(actor)
                        .detail("Đã gửi hóa đơn PDF thành công tới " + recipient)
                        .build());

                log.info("[InvoiceEmailSender] Sent invoice {} to {}", invoice.getInvoiceCode(), recipient);
                return;
            } catch (Exception ex) {
                lastError = ex;
                attempt++;
                log.warn("[InvoiceEmailSender] Attempt {} failed for invoice {} -> {}: {}",
                        attempt, invoice.getInvoiceCode(), recipient, ex.getMessage());
                if (attempt <= MAX_RETRIES) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        emailHist.setRetryCount(Math.max(0, attempt - 1));
        markFailed(invoice, emailHist, actor,
                lastError != null ? "Gửi Email thất bại sau " + attempt + " lần thử: " + lastError.getMessage()
                        : "Không thể gửi Email qua SMTP");
    }

    private void markFailed(Invoice invoice, InvoiceEmailHistory emailHist, String actor, String errorMessage) {
        String truncated = errorMessage != null && errorMessage.length() > 950
                ? errorMessage.substring(0, 950) : errorMessage;

        emailHist.setSendStatus("FAILED");
        emailHist.setSentAt(LocalDateTime.now());
        emailHist.setErrorMessage(truncated);
        emailHistoryRepository.save(emailHist);

        // QUAN TRỌNG: Không bao giờ thay đổi invoice_status khi gửi Email thất bại
        invoice.setEmailStatus("FAILED");
        invoiceRepository.save(invoice);

        historyRepository.save(InvoiceHistory.builder()
                .invoice(invoice)
                .action("EMAIL_FAILED")
                .fromStatus(invoice.getInvoiceStatus())
                .toStatus(invoice.getInvoiceStatus())
                .actor(actor)
                .detail("Gửi hóa đơn tới " + emailHist.getRecipientEmail() + " thất bại: " + truncated)
                .build());
    }

    private String buildEmailHtml(Invoice inv) {
        String orderCode = inv.getSalesOrder() != null ? inv.getSalesOrder().getOrderCode() : "-";
        String issued = inv.getIssuedAt() != null ? inv.getIssuedAt().format(DT_FMT) : "-";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        String total = nf.format(inv.getTotalAmount()) + " ₫";

        return "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;color:#0f172a;border:1px solid #e2e8f0;border-radius:8px;overflow:hidden\">"
                + "<div style=\"background:#0f172a;color:#fff;padding:18px 24px\">"
                + "<h2 style=\"margin:0;font-size:18px;letter-spacing:1px\">" + storeName + "</h2>"
                + "<p style=\"margin:4px 0 0;font-size:12px;color:#cbd5e1\">Hóa đơn mua hàng điện tử đính kèm PDF</p>"
                + "</div>"
                + "<div style=\"padding:22px 24px;line-height:1.6;font-size:14px\">"
                + "<p>Xin chào <strong>" + inv.getCustomerName() + "</strong>,</p>"
                + "<p>Cảm ơn Quý khách đã tin tưởng và mua sắm tại <strong>ADHA Sneaker</strong>. "
                + "Chúng tôi xin gửi kèm hóa đơn mua hàng định dạng PDF trong email này:</p>"
                + "<table style=\"width:100%;border-collapse:collapse;margin:14px 0;font-size:13.5px;background:#f8fafc;border:1px solid #e2e8f0\">"
                + "<tr><td style=\"padding:8px 12px;color:#64748b\">Mã hóa đơn:</td><td style=\"padding:8px 12px;font-weight:bold\">" + inv.getInvoiceCode() + "</td></tr>"
                + "<tr><td style=\"padding:8px 12px;color:#64748b\">Mã đơn hàng:</td><td style=\"padding:8px 12px;font-weight:bold\">" + orderCode + "</td></tr>"
                + "<tr><td style=\"padding:8px 12px;color:#64748b\">Loại hóa đơn:</td><td style=\"padding:8px 12px\">" + ("VAT".equals(inv.getInvoiceType()) ? "Hóa đơn VAT" : "Hóa đơn bán lẻ") + "</td></tr>"
                + "<tr><td style=\"padding:8px 12px;color:#64748b\">Ngày lập hóa đơn:</td><td style=\"padding:8px 12px\">" + issued + "</td></tr>"
                + "<tr><td style=\"padding:8px 12px;color:#64748b\">Tổng thanh toán:</td><td style=\"padding:8px 12px;font-weight:bold;color:#dc2626\">" + total + "</td></tr>"
                + "</table>"
                + "<p style=\"font-size:13px;color:#475569\">Nếu cần hỗ trợ về sản phẩm hoặc xuất hóa đơn VAT, vui lòng liên hệ:<br/>"
                + "Địa chỉ: " + storeAddress + "<br/>Hotline: " + storeHotline + "</p>"
                + "</div></div>";
    }
}
