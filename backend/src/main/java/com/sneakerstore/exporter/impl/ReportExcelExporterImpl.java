package com.sneakerstore.exporter.impl;

import com.sneakerstore.dto.response.ComprehensiveReportResponse;
import com.sneakerstore.dto.response.DashboardResponse;
import com.sneakerstore.exporter.ReportExcelExporter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReportExcelExporterImpl implements ReportExcelExporter {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    public byte[] exportToExcel(ComprehensiveReportResponse report) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles st = new Styles(wb);
            String filterMeta = report.getFilterSummary() != null ? report.getFilterSummary() : "Toàn hệ thống";
            String exportedAt = "Ngày xuất báo cáo: " + LocalDateTime.now().format(DT_FMT);

            buildSheet1RevenueCashflow(wb, st, report, filterMeta, exportedAt);
            buildSheet2Channels(wb, st, report, filterMeta, exportedAt);
            buildSheet3Cancellations(wb, st, report, filterMeta, exportedAt);
            buildSheet4VariantsAndBrands(wb, st, report, filterMeta, exportedAt);
            buildSheet5Employees(wb, st, report, filterMeta, exportedAt);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Lỗi xuất bảng kê kế toán Excel: " + ex.getMessage(), ex);
        }
    }

    private void buildSheet1RevenueCashflow(Workbook wb, Styles st, ComprehensiveReportResponse r, String meta, String exportedAt) {
        Sheet sheet = wb.createSheet("1_DoanhThu_DongTien");
        int row = writeTitleHeader(sheet, st, "BÁO CÁO DOANH THU & DÒNG TIỀN – ADHA SNEAKER", meta, exportedAt, 8);

        String[] cols = {
                "Kỳ báo cáo", "Số đơn hoàn thành", "Tiền hàng trước thuế", "Tiền thuế VAT",
                "Tiền mặt thu được", "Chuyển khoản (SePay)", "Tiền COD thu được", "Tổng doanh thu thực nhận"
        };
        writeTableHeader(sheet, row++, cols, st);

        int dataStart = row + 1;
        List<DashboardResponse.RevenuePointDto> series = r.getRevenueSeries() != null ? r.getRevenueSeries() : List.of();
        for (DashboardResponse.RevenuePointDto pt : series) {
            Row tr = sheet.createRow(row++);
            cellText(tr, 0, pt.getPeriod(), st.text);
            cellNum(tr, 1, pt.getOrderCount(), st.num);
            cellMoney(tr, 2, pt.getPreTaxRevenue(), st.money);
            cellMoney(tr, 3, pt.getVatAmount(), st.money);
            cellMoney(tr, 4, pt.getCashRevenue(), st.money);
            cellMoney(tr, 5, pt.getBankTransferRevenue(), st.money);
            cellMoney(tr, 6, pt.getCodRevenue(), st.money);
            cellMoney(tr, 7, pt.getRevenue(), st.moneyBold);
        }

        Row tot = sheet.createRow(row++);
        cellText(tot, 0, "TỔNG CỘNG KỲ BÁO CÁO", st.totalLabel);
        if (!series.isEmpty()) {
            int dataEnd = row - 1;
            setFormula(tot, 1, "SUM(B" + dataStart + ":B" + dataEnd + ")", st.totalNum);
            setFormula(tot, 2, "SUM(C" + dataStart + ":C" + dataEnd + ")", st.totalMoney);
            setFormula(tot, 3, "SUM(D" + dataStart + ":D" + dataEnd + ")", st.totalMoney);
            setFormula(tot, 4, "SUM(E" + dataStart + ":E" + dataEnd + ")", st.totalMoney);
            setFormula(tot, 5, "SUM(F" + dataStart + ":F" + dataEnd + ")", st.totalMoney);
            setFormula(tot, 6, "SUM(G" + dataStart + ":G" + dataEnd + ")", st.totalMoney);
            setFormula(tot, 7, "SUM(H" + dataStart + ":H" + dataEnd + ")", st.totalMoney);
        } else {
            for (int i = 1; i < cols.length; i++) cellMoney(tot, i, BigDecimal.ZERO, st.totalMoney);
        }

        row++;
        writeFormulaNotes(sheet, row, st, new String[]{
                "Ghi chú công thức kế toán:",
                "- Doanh thu thực nhận = Tổng giá trị các đơn hàng ở trạng thái HOÀN THÀNH (COMPLETED).",
                "- Tổng doanh thu = Tiền hàng trước thuế + Tiền thuế VAT = Tiền mặt + Chuyển khoản (SePay) + COD thu được.",
                "- AOV (Giá trị trung bình 1 đơn) = Tổng doanh thu thực nhận / Số đơn hoàn thành."
        });
        row += 5;
        writeSignatureBlock(sheet, row, st, 8);
        autoSize(sheet, cols.length);
    }

    private void buildSheet2Channels(Workbook wb, Styles st, ComprehensiveReportResponse r, String meta, String exportedAt) {
        Sheet sheet = wb.createSheet("2_HieuQua_KenhBan");
        int row = writeTitleHeader(sheet, st, "BÁO CÁO HIỆU QUẢ KÊNH BÁN HÀNG (POS VS ONLINE)", meta, exportedAt, 9);

        String[] cols = {
                "Kênh bán hàng", "Tổng đơn đặt", "Đơn thành công", "Đơn hủy / Bom hàng",
                "Tỷ lệ thành công (%)", "Tỷ lệ hủy (%)", "Phí giao vận phát sinh", "AOV (VNĐ/đơn)", "Doanh thu thực nhận"
        };
        writeTableHeader(sheet, row++, cols, st);

        for (DashboardResponse.ChannelComparisonDto ch : (r.getChannelComparison() != null ? r.getChannelComparison() : List.<DashboardResponse.ChannelComparisonDto>of())) {
            Row tr = sheet.createRow(row++);
            cellText(tr, 0, ch.getLabel(), st.textBold);
            cellNum(tr, 1, ch.getTotalOrders(), st.num);
            cellNum(tr, 2, ch.getCompletedOrders(), st.num);
            cellNum(tr, 3, ch.getCancelledOrders(), st.num);
            cellText(tr, 4, ch.getSuccessRate() + "%", st.textCenter);
            cellText(tr, 5, ch.getCancellationRate() + "%", st.textCenter);
            cellMoney(tr, 6, ch.getShippingFee(), st.money);
            cellMoney(tr, 7, ch.getAov(), st.money);
            cellMoney(tr, 8, ch.getActualRevenue(), st.moneyBold);
        }

        row += 2;
        writeSignatureBlock(sheet, row, st, 9);
        autoSize(sheet, cols.length);
    }

    private void buildSheet3Cancellations(Workbook wb, Styles st, ComprehensiveReportResponse r, String meta, String exportedAt) {
        Sheet sheet = wb.createSheet("3_DonHuy_BomHang");
        int row = writeTitleHeader(sheet, st, "BÁO CÁO ĐƠN HÀNG HỦY & HOÀN TRẢ (BOM HÀNG)", meta, exportedAt, 7);

        String[] rCols = {"Nhóm lý do hủy đơn", "Số lượng đơn hủy", "Tỷ trọng (%)", "Giá trị thất thoát (VNĐ)"};
        writeTableHeader(sheet, row++, rCols, st);
        for (ComprehensiveReportResponse.CancelReasonStatDto cr : (r.getCancelReasonStats() != null ? r.getCancelReasonStats() : List.<ComprehensiveReportResponse.CancelReasonStatDto>of())) {
            Row tr = sheet.createRow(row++);
            cellText(tr, 0, cr.getReasonCategory(), st.text);
            cellNum(tr, 1, cr.getCount(), st.num);
            cellText(tr, 2, cr.getPercentage() + "%", st.textCenter);
            cellMoney(tr, 3, cr.getLostValue(), st.money);
        }

        row += 2;
        String[] dCols = {"Mã đơn hàng", "Kênh bán", "Khách hàng", "SĐT", "Ngày đặt", "Giá trị đơn", "Nhóm lý do / Ghi chú hủy"};
        writeTableHeader(sheet, row++, dCols, st);
        for (ComprehensiveReportResponse.CancelledOrderItemDto c : (r.getCancelledOrders() != null ? r.getCancelledOrders() : List.<ComprehensiveReportResponse.CancelledOrderItemDto>of())) {
            Row tr = sheet.createRow(row++);
            cellText(tr, 0, c.getOrderCode(), st.textBold);
            cellText(tr, 1, "ONLINE".equals(c.getChannel()) ? "Online Store" : "Tại quầy (POS)", st.textCenter);
            cellText(tr, 2, c.getCustomerName(), st.text);
            cellText(tr, 3, c.getCustomerPhone(), st.text);
            cellText(tr, 4, c.getOrderDate() != null ? c.getOrderDate().format(DT_FMT) : "", st.textCenter);
            cellMoney(tr, 5, c.getAmount(), st.money);
            cellText(tr, 6, c.getReasonCategory() + " - " + (c.getCancelNote() != null ? c.getCancelNote() : ""), st.text);
        }

        row += 2;
        writeSignatureBlock(sheet, row, st, 7);
        autoSize(sheet, dCols.length);
    }

    private void buildSheet4VariantsAndBrands(Workbook wb, Styles st, ComprehensiveReportResponse r, String meta, String exportedAt) {
        Sheet sheet = wb.createSheet("4_Size_MauSac_ThuongHieu");
        int row = writeTitleHeader(sheet, st, "BÁO CÁO BIẾN THỂ SIZE, MÀU SẮC & THƯƠNG HIỆU SNEAKER", meta, exportedAt, 6);

        String[] sCols = {"Size giày", "Phân nhóm", "Số đôi đã bán", "Tỷ trọng bán (%)", "Tồn kho hiện tại", "Doanh thu (VNĐ)"};
        writeTableHeader(sheet, row++, sCols, st);
        for (ComprehensiveReportResponse.SizeSalesStatDto sz : (r.getSizeStats() != null ? r.getSizeStats() : List.<ComprehensiveReportResponse.SizeSalesStatDto>of())) {
            Row tr = sheet.createRow(row++);
            cellText(tr, 0, "EU " + sz.getSizeValue() + (sz.isGoldenSize() ? " [SIZE VÀNG]" : ""), st.textBold);
            cellText(tr, 1, "SIZE_NU".equals(sz.getSizeGroup()) ? "Size Nữ (36-39)" : ("SIZE_NAM".equals(sz.getSizeGroup()) ? "Size Nam (40-45)" : "Khác"), st.text);
            cellNum(tr, 2, sz.getQuantitySold(), st.num);
            cellText(tr, 3, sz.getSharePercent() + "%", st.textCenter);
            cellNum(tr, 4, sz.getCurrentStock(), st.num);
            cellMoney(tr, 5, sz.getRevenue(), st.money);
        }

        row += 2;
        String[] bCols = {"Thương hiệu", "Số mẫu bán ra", "Số đôi đã bán", "Tỷ trọng doanh thu (%)", "Doanh thu (VNĐ)"};
        writeTableHeader(sheet, row++, bCols, st);
        for (ComprehensiveReportResponse.BrandSalesStatDto b : (r.getBrandStats() != null ? r.getBrandStats() : List.<ComprehensiveReportResponse.BrandSalesStatDto>of())) {
            Row tr = sheet.createRow(row++);
            cellText(tr, 0, b.getBrandName(), st.textBold);
            cellNum(tr, 1, b.getProductCount(), st.num);
            cellNum(tr, 2, b.getQuantitySold(), st.num);
            cellText(tr, 3, b.getSharePercent() + "%", st.textCenter);
            cellMoney(tr, 4, b.getRevenue(), st.moneyBold);
        }

        row += 2;
        writeSignatureBlock(sheet, row, st, 6);
        autoSize(sheet, 6);
    }

    private void buildSheet5Employees(Workbook wb, Styles st, ComprehensiveReportResponse r, String meta, String exportedAt) {
        Sheet sheet = wb.createSheet("5_HieuSuat_NhanVien");
        int row = writeTitleHeader(sheet, st, "BÁO CÁO HIỆU SUẤT NHÂN VIÊN BÁN HÀNG (KPI / HOA HỒNG)", meta, exportedAt, 6);

        String[] cols = {"Mã NV", "Tên nhân viên", "Số ca làm việc", "Số đơn thành công", "Tổng doanh thu (VNĐ)", "Giá trị TB / đơn (AOV)"};
        writeTableHeader(sheet, row++, cols, st);
        for (ComprehensiveReportResponse.EmployeeSalesStatDto emp : (r.getEmployeeRevenueStats() != null ? r.getEmployeeRevenueStats() : List.<ComprehensiveReportResponse.EmployeeSalesStatDto>of())) {
            Row tr = sheet.createRow(row++);
            cellText(tr, 0, emp.getEmployeeCode(), st.textBold);
            cellText(tr, 1, emp.getEmployeeName(), st.text);
            cellNum(tr, 2, emp.getWorkingShifts(), st.num);
            cellNum(tr, 3, emp.getCompletedOrderCount(), st.num);
            cellMoney(tr, 4, emp.getTotalRevenue(), st.moneyBold);
            cellMoney(tr, 5, emp.getAverageOrderValue(), st.money);
        }

        row += 2;
        writeSignatureBlock(sheet, row, st, 6);
        autoSize(sheet, cols.length);
    }

    private int writeTitleHeader(Sheet sheet, Styles st, String title, String meta, String exportedAt, int colCount) {
        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(title);
        c0.setCellStyle(st.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));

        Row r1 = sheet.createRow(1);
        Cell c1 = r1.createCell(0);
        c1.setCellValue("Điều kiện lọc: " + meta);
        c1.setCellStyle(st.sub);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, colCount - 1));

        Row r2 = sheet.createRow(2);
        Cell c2 = r2.createCell(0);
        c2.setCellValue(exportedAt);
        c2.setCellStyle(st.sub);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, colCount - 1));
        return 4;
    }

    private void writeTableHeader(Sheet sheet, int rowIdx, String[] cols, Styles st) {
        Row r = sheet.createRow(rowIdx);
        r.setHeightInPoints(24);
        for (int i = 0; i < cols.length; i++) {
            Cell c = r.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(st.header);
        }
    }

    private void writeFormulaNotes(Sheet sheet, int startRow, Styles st, String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            Row r = sheet.createRow(startRow + i);
            Cell c = r.createCell(0);
            c.setCellValue(lines[i]);
            c.setCellStyle(i == 0 ? st.textBoldNoBorder : st.sub);
        }
    }

    private void writeSignatureBlock(Sheet sheet, int startRow, Styles st, int colCount) {
        int col2 = Math.max(2, colCount / 2);
        int col3 = Math.max(4, colCount - 2);

        Row rHead = sheet.createRow(startRow);
        Cell s1 = rHead.createCell(0); s1.setCellValue("NGƯỜI LẬP BIỂU"); s1.setCellStyle(st.sigTitle);
        Cell s2 = rHead.createCell(col2); s2.setCellValue("KẾ TOÁN TRƯỞNG"); s2.setCellStyle(st.sigTitle);
        Cell s3 = rHead.createCell(col3); s3.setCellValue("CHỦ CỬA HÀNG / GIÁM ĐỐC"); s3.setCellStyle(st.sigTitle);

        Row rSub = sheet.createRow(startRow + 1);
        Cell sb1 = rSub.createCell(0); sb1.setCellValue("(Ký, ghi rõ họ tên)"); sb1.setCellStyle(st.sigSub);
        Cell sb2 = rSub.createCell(col2); sb2.setCellValue("(Ký, ghi rõ họ tên)"); sb2.setCellStyle(st.sigSub);
        Cell sb3 = rSub.createCell(col3); sb3.setCellValue("(Ký, đóng dấu)"); sb3.setCellStyle(st.sigSub);
    }

    private void cellText(Row r, int col, String v, CellStyle style) {
        Cell c = r.createCell(col);
        c.setCellValue(v != null ? v : "");
        c.setCellStyle(style);
    }

    private void cellNum(Row r, int col, long v, CellStyle style) {
        Cell c = r.createCell(col);
        c.setCellValue(v);
        c.setCellStyle(style);
    }

    private void cellMoney(Row r, int col, BigDecimal v, CellStyle style) {
        Cell c = r.createCell(col);
        c.setCellValue(v != null ? v.doubleValue() : 0.0);
        c.setCellStyle(style);
    }

    private void setFormula(Row r, int col, String formula, CellStyle style) {
        Cell c = r.createCell(col);
        c.setCellFormula(formula);
        c.setCellStyle(style);
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
            int cur = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(Math.max(cur + 600, 3500), 14000));
        }
    }

    private static class Styles {
        final CellStyle title, sub, header, text, textBold, textCenter, textBoldNoBorder, num, money, moneyBold;
        final CellStyle totalLabel, totalNum, totalMoney, sigTitle, sigSub;

        Styles(Workbook wb) {
            DataFormat fmt = wb.createDataFormat();

            Font fTitle = wb.createFont(); fTitle.setBold(true); fTitle.setFontHeightInPoints((short) 13);
            title = wb.createCellStyle(); title.setFont(fTitle);

            Font fSub = wb.createFont(); fSub.setItalic(true); fSub.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            sub = wb.createCellStyle(); sub.setFont(fSub);

            Font fHeader = wb.createFont(); fHeader.setBold(true); fHeader.setColor(IndexedColors.WHITE.getIndex());
            header = wb.createCellStyle();
            header.setFont(fHeader);
            header.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            border(header);

            Font fBold = wb.createFont(); fBold.setBold(true);

            text = wb.createCellStyle(); border(text);
            textBold = wb.createCellStyle(); textBold.setFont(fBold); border(textBold);
            textCenter = wb.createCellStyle(); textCenter.setAlignment(HorizontalAlignment.CENTER); border(textCenter);
            textBoldNoBorder = wb.createCellStyle(); textBoldNoBorder.setFont(fBold);

            num = wb.createCellStyle(); num.setAlignment(HorizontalAlignment.CENTER); num.setDataFormat(fmt.getFormat("#,##0")); border(num);
            money = wb.createCellStyle(); money.setAlignment(HorizontalAlignment.RIGHT); money.setDataFormat(fmt.getFormat("#,##0")); border(money);
            moneyBold = wb.createCellStyle(); moneyBold.setFont(fBold); moneyBold.setAlignment(HorizontalAlignment.RIGHT); moneyBold.setDataFormat(fmt.getFormat("#,##0")); border(moneyBold);

            totalLabel = wb.createCellStyle(); totalLabel.setFont(fBold); totalLabel.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); totalLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND); border(totalLabel);
            totalNum = wb.createCellStyle(); totalNum.setFont(fBold); totalNum.setAlignment(HorizontalAlignment.CENTER); totalNum.setDataFormat(fmt.getFormat("#,##0")); totalNum.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); totalNum.setFillPattern(FillPatternType.SOLID_FOREGROUND); border(totalNum);
            totalMoney = wb.createCellStyle(); totalMoney.setFont(fBold); totalMoney.setAlignment(HorizontalAlignment.RIGHT); totalMoney.setDataFormat(fmt.getFormat("#,##0")); totalMoney.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); totalMoney.setFillPattern(FillPatternType.SOLID_FOREGROUND); border(totalMoney);

            sigTitle = wb.createCellStyle(); sigTitle.setFont(fBold); sigTitle.setAlignment(HorizontalAlignment.CENTER);
            sigSub = wb.createCellStyle(); sigSub.setFont(fSub); sigSub.setAlignment(HorizontalAlignment.CENTER);
        }

        private void border(CellStyle s) {
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
        }
    }
}
