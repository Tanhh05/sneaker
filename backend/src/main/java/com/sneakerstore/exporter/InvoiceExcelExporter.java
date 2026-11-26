package com.sneakerstore.exporter;

import com.sneakerstore.entity.Invoice;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class InvoiceExcelExporter {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public byte[] export(List<Invoice> invoices) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BangKeHoaDon");

            // Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(headerStyle);

            DataFormat fmt = workbook.createDataFormat();
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(fmt.getFormat("#,##0"));
            moneyStyle.setAlignment(HorizontalAlignment.RIGHT);
            setBorders(moneyStyle);

            CellStyle textStyle = workbook.createCellStyle();
            setBorders(textStyle);

            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            CellStyle totalLabelStyle = workbook.createCellStyle();
            totalLabelStyle.setFont(totalFont);
            totalLabelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(totalLabelStyle);

            CellStyle totalMoneyStyle = workbook.createCellStyle();
            totalMoneyStyle.setFont(totalFont);
            totalMoneyStyle.setDataFormat(fmt.getFormat("#,##0"));
            totalMoneyStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalMoneyStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalMoneyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(totalMoneyStyle);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BẢNG KÊ HÓA ĐƠN BÁN HÀNG – ADHA SNEAKER");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 14));

            // Headers
            String[] columns = {
                    "STT", "Mã hóa đơn", "Mã đơn hàng", "Kênh bán", "Loại hóa đơn",
                    "Khách hàng / Công ty", "SĐT", "Email", "Mã số thuế",
                    "Giá trị trước thuế", "Chiết khấu", "Phí vận chuyển",
                    "Tiền thuế VAT", "Tổng thanh toán", "Ngày lập", "Trạng thái hóa đơn", "Trạng thái Email"
            };

            Row headerRow = sheet.createRow(2);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            BigDecimal validSubtotalSum = BigDecimal.ZERO;
            BigDecimal validDiscountSum = BigDecimal.ZERO;
            BigDecimal validShippingSum = BigDecimal.ZERO;
            BigDecimal validTaxSum = BigDecimal.ZERO;
            BigDecimal validTotalSum = BigDecimal.ZERO;

            int rowIdx = 3;
            int stt = 1;
            for (Invoice inv : invoices) {
                Row row = sheet.createRow(rowIdx++);
                String custDisplay = (inv.getCompanyName() != null && !inv.getCompanyName().isBlank())
                        ? inv.getCompanyName() + " (" + inv.getCustomerName() + ")"
                        : inv.getCustomerName();

                setTextCell(row, 0, String.valueOf(stt++), textStyle);
                setTextCell(row, 1, inv.getInvoiceCode(), textStyle);
                setTextCell(row, 2, inv.getSalesOrder() != null ? inv.getSalesOrder().getOrderCode() : "", textStyle);
                setTextCell(row, 3, "ONLINE".equals(inv.getSaleChannel()) ? "Online" : "Tại quầy", textStyle);
                setTextCell(row, 4, "VAT".equals(inv.getInvoiceType()) ? "Hóa đơn VAT" : "Bán lẻ", textStyle);
                setTextCell(row, 5, custDisplay, textStyle);
                setTextCell(row, 6, safe(inv.getCustomerPhone()), textStyle);
                setTextCell(row, 7, safe(inv.getCustomerEmail()), textStyle);
                setTextCell(row, 8, safe(inv.getTaxCode()), textStyle);

                setMoneyCell(row, 9, inv.getSubtotal(), moneyStyle);
                setMoneyCell(row, 10, inv.getDiscountAmount(), moneyStyle);
                setMoneyCell(row, 11, inv.getShippingFee(), moneyStyle);
                setMoneyCell(row, 12, inv.getTaxAmount(), moneyStyle);
                setMoneyCell(row, 13, inv.getTotalAmount(), moneyStyle);

                setTextCell(row, 14, inv.getIssuedAt() != null ? inv.getIssuedAt().format(DT_FMT) : "", textStyle);
                setTextCell(row, 15, mapInvoiceStatus(inv.getInvoiceStatus()), textStyle);
                setTextCell(row, 16, mapEmailStatus(inv.getEmailStatus()), textStyle);

                if (!"CANCELLED".equalsIgnoreCase(inv.getInvoiceStatus())) {
                    validSubtotalSum = validSubtotalSum.add(nvl(inv.getSubtotal()));
                    validDiscountSum = validDiscountSum.add(nvl(inv.getDiscountAmount()));
                    validShippingSum = validShippingSum.add(nvl(inv.getShippingFee()));
                    validTaxSum = validTaxSum.add(nvl(inv.getTaxAmount()));
                    validTotalSum = validTotalSum.add(nvl(inv.getTotalAmount()));
                }
            }

            // Total Row (Valid non-cancelled invoices only)
            Row totalRow = sheet.createRow(rowIdx);
            for (int i = 0; i < columns.length; i++) {
                totalRow.createCell(i).setCellStyle(totalLabelStyle);
            }
            totalRow.getCell(0).setCellValue("TỔNG CỘNG (HÓA ĐƠN HỢP LỆ - KHÔNG GỒM ĐÃ HỦY)");
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 8));
            setMoneyCell(totalRow, 9, validSubtotalSum, totalMoneyStyle);
            setMoneyCell(totalRow, 10, validDiscountSum, totalMoneyStyle);
            setMoneyCell(totalRow, 11, validShippingSum, totalMoneyStyle);
            setMoneyCell(totalRow, 12, validTaxSum, totalMoneyStyle);
            setMoneyCell(totalRow, 13, validTotalSum, totalMoneyStyle);

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất file Excel bảng kê hóa đơn: " + e.getMessage(), e);
        }
    }

    private void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void setTextCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void setMoneyCell(Row row, int col, BigDecimal val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(nvl(val).doubleValue());
        cell.setCellStyle(style);
    }

    private BigDecimal nvl(BigDecimal b) {
        return b != null ? b : BigDecimal.ZERO;
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private String mapInvoiceStatus(String s) {
        if (s == null) return "";
        return switch (s) {
            case "ISSUED" -> "Đã phát hành";
            case "PENDING_CANCEL" -> "Chờ duyệt hủy";
            case "CANCELLED" -> "Đã hủy";
            case "ADJUSTED" -> "Đã điều chỉnh";
            default -> s;
        };
    }

    private String mapEmailStatus(String s) {
        if (s == null) return "";
        return switch (s) {
            case "NOT_SENT" -> "Chưa gửi";
            case "SENDING" -> "Đang gửi";
            case "SENT" -> "Thành công";
            case "FAILED" -> "Thất bại";
            default -> s;
        };
    }
}
