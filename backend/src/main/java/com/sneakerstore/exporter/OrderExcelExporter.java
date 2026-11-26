package com.sneakerstore.exporter;

import com.sneakerstore.dto.response.SalesOrderDetailDto;
import com.sneakerstore.dto.response.SalesOrderDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderExcelExporter {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public byte[] export(List<SalesOrderDto> orders) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("DanhSachDonHang");

            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);

            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            border(headerStyle);

            DataFormat fmt = wb.createDataFormat();
            CellStyle textStyle = wb.createCellStyle();
            border(textStyle);

            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            border(centerStyle);

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setAlignment(HorizontalAlignment.RIGHT);
            moneyStyle.setDataFormat(fmt.getFormat("#,##0"));
            border(moneyStyle);

            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            CellStyle totalLabel = wb.createCellStyle();
            totalLabel.setFont(boldFont);
            totalLabel.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            border(totalLabel);

            CellStyle totalMoney = wb.createCellStyle();
            totalMoney.setFont(boldFont);
            totalMoney.setAlignment(HorizontalAlignment.RIGHT);
            totalMoney.setDataFormat(fmt.getFormat("#,##0"));
            totalMoney.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalMoney.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            border(totalMoney);

            Row rTitle = sheet.createRow(0);
            Cell cTitle = rTitle.createCell(0);
            cTitle.setCellValue("DANH SÁCH ĐƠN HÀNG – ADHA SNEAKER");
            cTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

            String[] cols = {
                    "STT", "Mã đơn hàng", "Kênh bán", "Thời gian tạo", "Khách hàng",
                    "Số điện thoại", "Sản phẩm trong đơn", "Tổng SL", "Tổng thanh toán (VNĐ)",
                    "Phương thức thanh toán", "Trạng thái đơn", "Ghi chú"
            };
            Row rHead = sheet.createRow(2);
            rHead.setHeightInPoints(24);
            for (int i = 0; i < cols.length; i++) {
                Cell c = rHead.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            BigDecimal totalSum = BigDecimal.ZERO;
            int rowIdx = 3;
            int stt = 1;
            for (SalesOrderDto o : orders) {
                Row row = sheet.createRow(rowIdx++);
                boolean isOnline = "ONLINE".equalsIgnoreCase(o.getChannel()) || o.getEmployeeId() == null;
                String itemsSummary = o.getDetails() == null ? "" : o.getDetails().stream()
                        .map(d -> d.getProductName() + " (Size " + nvlStr(d.getSizeValue()) + "/" + nvlStr(d.getColorName()) + " x" + d.getQuantity() + ")")
                        .collect(Collectors.joining("; "));
                int qtySum = o.getDetails() == null ? 0 : o.getDetails().stream()
                        .mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0).sum();

                String pmLabel = "BANK_TRANSFER".equalsIgnoreCase(o.getPaymentMethod())
                        ? "Chuyển khoản (SePay)"
                        : (isOnline || "COD".equalsIgnoreCase(o.getPaymentMethod()) ? "COD (Thu hộ)" : "Tiền mặt (POS)");

                setText(row, 0, String.valueOf(stt++), centerStyle);
                setText(row, 1, o.getOrderCode(), textStyle);
                setText(row, 2, isOnline ? "Online Store" : "Tại quầy (POS)", centerStyle);
                setText(row, 3, o.getOrderDate() != null ? o.getOrderDate().format(DT_FMT) : "", centerStyle);
                setText(row, 4, nvlStr(o.getCustomerName()), textStyle);
                setText(row, 5, nvlStr(o.getCustomerPhone()), textStyle);
                setText(row, 6, itemsSummary, textStyle);
                Cell qtyCell = row.createCell(7);
                qtyCell.setCellValue(qtySum);
                qtyCell.setCellStyle(centerStyle);
                Cell amtCell = row.createCell(8);
                BigDecimal finalAmt = o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO;
                amtCell.setCellValue(finalAmt.doubleValue());
                amtCell.setCellStyle(moneyStyle);
                setText(row, 9, pmLabel, textStyle);
                setText(row, 10, mapStatus(o.getStatus()), centerStyle);
                setText(row, 11, nvlStr(o.getNote()), textStyle);

                if (!"CANCELLED".equalsIgnoreCase(o.getStatus())) {
                    totalSum = totalSum.add(finalAmt);
                }
            }

            Row totRow = sheet.createRow(rowIdx);
            for (int i = 0; i < cols.length; i++) {
                totRow.createCell(i).setCellStyle(totalLabel);
            }
            totRow.getCell(0).setCellValue("TỔNG GIÁ TRỊ ĐƠN (KHÔNG GỒM ĐÃ HỦY)");
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 7));
            Cell totAmt = totRow.getCell(8);
            totAmt.setCellValue(totalSum.doubleValue());
            totAmt.setCellStyle(totalMoney);

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
                int cur = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(Math.max(cur + 500, 3200), 14000));
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Lỗi xuất Excel danh sách đơn hàng: " + ex.getMessage(), ex);
        }
    }

    private void border(CellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private void setText(Row r, int col, String v, CellStyle s) {
        Cell c = r.createCell(col);
        c.setCellValue(v != null ? v : "");
        c.setCellStyle(s);
    }

    private String nvlStr(String s) {
        return s != null ? s : "";
    }

    private String mapStatus(String st) {
        if (st == null) return "";
        return switch (st) {
            case "DRAFT" -> "Đơn nháp";
            case "PENDING" -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "SHIPPING" -> "Đang giao hàng";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> st;
        };
    }
}
