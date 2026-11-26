package com.sneakerstore.exporter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sneakerstore.entity.Invoice;
import com.sneakerstore.entity.InvoiceDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class InvoicePdfGenerator {

    @Value("${app.invoice.store-name:ADHA SNEAKER STORE}")
    private String storeName;

    @Value("${app.invoice.store-tax-code:0319886688}")
    private String storeTaxCode;

    @Value("${app.invoice.store-address:120 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh}")
    private String storeAddress;

    @Value("${app.invoice.store-hotline:0901 000 001}")
    private String storeHotline;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public byte[] generate(Invoice invoice, List<InvoiceDetail> details) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont baseFont = resolveUnicodeFont();
            boolean unicodeReady = isUnicodeFont(baseFont);

            Font titleFont = new Font(baseFont, 16, Font.BOLD, new Color(15, 23, 42));
            Font subTitleFont = new Font(baseFont, 12, Font.BOLD, new Color(220, 38, 38));
            Font boldFont = new Font(baseFont, 9.5f, Font.BOLD, new Color(15, 23, 42));
            Font regularFont = new Font(baseFont, 9.5f, Font.NORMAL, new Color(51, 65, 85));
            Font smallFont = new Font(baseFont, 8.5f, Font.NORMAL, new Color(100, 116, 139));
            Font thFont = new Font(baseFont, 9f, Font.BOLD, Color.WHITE);

            // Header
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{60f, 40f});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph(txt(storeName, unicodeReady), titleFont));
            leftCell.addElement(new Paragraph(txt("MST: " + storeTaxCode, unicodeReady), regularFont));
            leftCell.addElement(new Paragraph(txt("Địa chỉ: " + storeAddress, unicodeReady), regularFont));
            leftCell.addElement(new Paragraph(txt("Hotline: " + storeHotline, unicodeReady), regularFont));
            header.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            boolean isVat = "VAT".equalsIgnoreCase(invoice.getInvoiceType());
            String docTitle = isVat ? "HÓA ĐƠN GIÁ TRỊ GIA TĂNG (VAT)" : "HÓA ĐƠN BÁN LẺ";
            Paragraph pDocTitle = new Paragraph(txt(docTitle, unicodeReady), subTitleFont);
            pDocTitle.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(pDocTitle);

            Paragraph pCode = new Paragraph(txt("Số HĐ: " + invoice.getInvoiceCode(), unicodeReady), boldFont);
            pCode.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(pCode);

            String orderCode = invoice.getSalesOrder() != null ? invoice.getSalesOrder().getOrderCode() : "-";
            Paragraph pOrder = new Paragraph(txt("Đơn hàng nguồn: " + orderCode + " (" + invoice.getSaleChannel() + ")", unicodeReady), regularFont);
            pOrder.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(pOrder);

            Paragraph pDate = new Paragraph(txt("Ngày lập: " + (invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(DT_FMT) : "-"), unicodeReady), regularFont);
            pDate.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(pDate);

            header.addCell(rightCell);
            doc.add(header);

            doc.add(new Paragraph(" "));

            // Customer box
            PdfPTable custTable = new PdfPTable(2);
            custTable.setWidthPercentage(100);
            custTable.setWidths(new float[]{50f, 50f});
            addInfoCell(custTable, "Khách hàng:", invoice.getCustomerName(), boldFont, regularFont, unicodeReady);
            addInfoCell(custTable, "Số điện thoại:", safe(invoice.getCustomerPhone()), boldFont, regularFont, unicodeReady);
            if (isVat || (invoice.getCompanyName() != null && !invoice.getCompanyName().isBlank())) {
                addInfoCell(custTable, "Tên công ty:", safe(invoice.getCompanyName()), boldFont, regularFont, unicodeReady);
                addInfoCell(custTable, "Mã số thuế:", safe(invoice.getTaxCode()), boldFont, regularFont, unicodeReady);
            }
            addInfoCell(custTable, "Email:", safe(invoice.getCustomerEmail()), boldFont, regularFont, unicodeReady);
            addInfoCell(custTable, "Địa chỉ:", safe(invoice.getCustomerAddress()), boldFont, regularFont, unicodeReady);
            doc.add(custTable);

            doc.add(new Paragraph(" "));

            // Details table
            PdfPTable itemsTable = new PdfPTable(7);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{6f, 28f, 16f, 8f, 14f, 10f, 18f});

            String[] cols = {"#", "Sản phẩm", "Biến thể", "SL", "Đơn giá", "VAT", "Thành tiền"};
            for (String col : cols) {
                PdfPCell c = new PdfPCell(new Phrase(txt(col, unicodeReady), thFont));
                c.setBackgroundColor(new Color(15, 23, 42));
                c.setPadding(6f);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemsTable.addCell(c);
            }

            int idx = 1;
            for (InvoiceDetail d : details) {
                addDataCell(itemsTable, String.valueOf(idx++), regularFont, Element.ALIGN_CENTER, unicodeReady);
                addDataCell(itemsTable, d.getProductName() + (d.getSku() != null ? " [" + d.getSku() + "]" : ""), regularFont, Element.ALIGN_LEFT, unicodeReady);
                addDataCell(itemsTable, safe(d.getVariantLabel()), regularFont, Element.ALIGN_LEFT, unicodeReady);
                addDataCell(itemsTable, String.valueOf(d.getQuantity()), regularFont, Element.ALIGN_CENTER, unicodeReady);
                addDataCell(itemsTable, fmtVnd(d.getUnitPrice()), regularFont, Element.ALIGN_RIGHT, unicodeReady);
                addDataCell(itemsTable, d.getTaxRate().stripTrailingZeros().toPlainString() + "%", regularFont, Element.ALIGN_CENTER, unicodeReady);
                addDataCell(itemsTable, fmtVnd(d.getLineTotal()), boldFont, Element.ALIGN_RIGHT, unicodeReady);
            }
            doc.add(itemsTable);

            doc.add(new Paragraph(" "));

            // Financial Summary
            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(50);
            summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summary.setWidths(new float[]{55f, 45f});

            addSummaryRow(summary, "Tiền hàng trước thuế:", fmtVnd(invoice.getSubtotal()), regularFont, regularFont, unicodeReady);
            addSummaryRow(summary, "Chiết khấu / Giảm giá:", "-" + fmtVnd(invoice.getDiscountAmount()), regularFont, regularFont, unicodeReady);
            addSummaryRow(summary, "Phí vận chuyển:", fmtVnd(invoice.getShippingFee()), regularFont, regularFont, unicodeReady);
            addSummaryRow(summary, "Tiền thuế GTGT (" + invoice.getTaxRate().stripTrailingZeros().toPlainString() + "%):", fmtVnd(invoice.getTaxAmount()), regularFont, regularFont, unicodeReady);
            addSummaryRow(summary, "TỔNG THANH TOÁN:", fmtVnd(invoice.getTotalAmount()), boldFont, subTitleFont, unicodeReady);
            doc.add(summary);

            doc.add(new Paragraph(" "));
            Paragraph footer = new Paragraph(
                    txt("Cảm ơn Quý khách đã mua sắm tại " + storeName + "! " +
                            (isVat ? "Lưu ý: Đây là bản thể hiện hóa đơn VAT nội bộ; hóa đơn điện tử có giá trị pháp lý phát hành qua cổng CQT khi tích hợp NCC." :
                                    "Vui lòng giữ hóa đơn để được hỗ trợ đổi trả và bảo hành chính hãng."), unicodeReady),
                    smallFont
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo file PDF hóa đơn: " + e.getMessage(), e);
        }
    }

    private BaseFont resolveUnicodeFont() {
        String[] candidates = {
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                try {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                } catch (Exception ignored) {
                }
            }
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isUnicodeFont(BaseFont bf) {
        return BaseFont.IDENTITY_H.equals(bf.getEncoding());
    }

    private String txt(String input, boolean unicodeReady) {
        if (input == null) return "";
        if (unicodeReady) return input;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized;
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "---" : s;
    }

    private String fmtVnd(BigDecimal amount) {
        if (amount == null) return "0 VND";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(amount) + " VND";
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font boldFont, Font regularFont, boolean unicodeReady) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(248, 250, 252));
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setPadding(6f);
        Phrase p = new Phrase();
        p.add(new Chunk(txt(label + " ", unicodeReady), boldFont));
        p.add(new Chunk(txt(value == null ? "---" : value, unicodeReady), regularFont));
        cell.setPhrase(p);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String value, Font font, int align, boolean unicodeReady) {
        PdfPCell cell = new PdfPCell(new Phrase(txt(value, unicodeReady), font));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(new Color(203, 213, 225));
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font lFont, Font vFont, boolean unicodeReady) {
        PdfPCell c1 = new PdfPCell(new Phrase(txt(label, unicodeReady), lFont));
        c1.setBorder(Rectangle.BOTTOM);
        c1.setBorderColor(new Color(226, 232, 240));
        c1.setPadding(5f);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(txt(value, unicodeReady), vFont));
        c2.setBorder(Rectangle.BOTTOM);
        c2.setBorderColor(new Color(226, 232, 240));
        c2.setPadding(5f);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c2);
    }
}
