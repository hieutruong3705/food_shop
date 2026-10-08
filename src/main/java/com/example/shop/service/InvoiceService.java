package com.example.shop.service;

import com.example.shop.domain.Order;
import com.example.shop.domain.OrderDetail;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class InvoiceService {

    public void exportInvoice(HttpServletResponse response, Order order) {
        // Cấu hình file PDF
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Hoa_don_" + order.getId() + ".pdf";
        response.setHeader(headerKey, headerValue);

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, response.getOutputStream());

            document.open();

            // === 1. CẤU HÌNH FONT CHỮ TIẾNG VIỆT ===
            // Đường dẫn đến file font trong thư mục resources
            // Lưu ý: Nếu chạy trên Windows thì file .ttf phải đúng tên (thường là chữ
            // thường)
            String fontPath = "src/main/resources/fonts/arial.ttf";

            // Tạo BaseFont với chế độ IDENTITY_H (để gõ tiếng Việt Unicode)
            BaseFont bf = BaseFont.createFont("arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, new org.springframework.core.io.ClassPathResource("fonts/arial.ttf").getInputStream().readAllBytes(), null);

            // Tạo các kiểu chữ từ BaseFont
            Font fontTitle = new Font(bf, 18, Font.BOLD); // Chữ to đậm
            Font fontInfo = new Font(bf, 12, Font.NORMAL); // Chữ thường
            Font fontHeader = new Font(bf, 12, Font.BOLD); // Chữ đậm nhỏ

            // === 2. NỘI DUNG HÓA ĐƠN (Thoải mái gõ tiếng Việt có dấu) ===

            // Tiêu đề
            Paragraph title = new Paragraph("HÓA ĐƠN BÁN HÀNG", fontTitle);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Thông tin chung
            document.add(new Paragraph("Mã đơn hàng: #" + order.getId(), fontInfo));
            document.add(new Paragraph("Khách hàng: " + order.getReceiverName(), fontInfo));
            document.add(new Paragraph("Số điện thoại: " + order.getReceiverPhone(), fontInfo));
            document.add(new Paragraph("Địa chỉ: " + order.getReceiverAddress(), fontInfo));

            // Ngày đặt in theo giờ Việt Nam, dạng ngày/tháng/năm giờ:phút
            if (order.getCreatedAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
                document.add(new Paragraph("Ngày đặt: " + formatter.format(Instant.ofEpochMilli(order.getCreatedAt().getTime())), fontInfo));
            }
            document.add(new Paragraph("\n"));

            // === 3. VẼ BẢNG SẢN PHẨM ===
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.0f, 4.0f, 1.0f, 2.5f });

            // Header bảng
            addCellToTable(table, "STT", fontHeader);
            addCellToTable(table, "Tên Sản Phẩm", fontHeader);
            addCellToTable(table, "SL", fontHeader);
            addCellToTable(table, "Thành Tiền", fontHeader);

            // Dữ liệu bảng
            int i = 1;
            Locale localeVN = new Locale("vi", "VN");
            NumberFormat currencyVN = NumberFormat.getCurrencyInstance(localeVN);

            double total = 0;
            if (order.getOrderDetails() != null) {
                for (OrderDetail detail : order.getOrderDetails()) {
                    addCellToTable(table, String.valueOf(i++), fontInfo);
                    // Tên sản phẩm tiếng Việt
                    addCellToTable(table, (detail.getProductName() + (detail.getVariantLabel()==null ? "" : " · " + detail.getVariantLabel())), fontInfo);
                    addCellToTable(table, String.valueOf(detail.getQuantity()), fontInfo);

                    double thanhTien = detail.getPrice() * detail.getQuantity();
                    total += thanhTien;
                    addCellToTable(table, currencyVN.format(thanhTien), fontInfo);
                }
            }

            document.add(table);

            // === 4. TỔNG TIỀN ===
            document.add(new Paragraph("\n"));
            // Khi đơn có phí giao hàng hoặc giảm giá thì in thêm các dòng trung gian,
            // để người đọc cộng các dòng hàng ra đúng tổng thanh toán.
            double shipping = order.getShippingFee() == null ? 0 : order.getShippingFee();
            double discount = order.getDiscountAmount() == null ? 0 : order.getDiscountAmount();
            if (shipping > 0 || discount > 0) {
                addAmountLine(document, "Tạm tính: " + currencyVN.format(total), fontInfo);
            }
            if (shipping > 0) {
                addAmountLine(document, "Phí giao hàng: " + currencyVN.format(shipping), fontInfo);
            }
            if (discount > 0) {
                String voucher = order.getVoucherCode() == null || order.getVoucherCode().isBlank() ? "" : " (mã " + order.getVoucherCode() + ")";
                addAmountLine(document, "Giảm giá" + voucher + ": -" + currencyVN.format(discount), fontInfo);
            }
            Paragraph totalParam = new Paragraph("Tổng cộng: " + currencyVN.format(order.getTotalPrice()), fontHeader);
            totalParam.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(totalParam);

            document.close();

        } catch (Exception e) {
            throw new IllegalStateException("Không thể xuất hóa đơn", e);
        }
    }

    private void addAmountLine(Document document, String text, Font font) throws DocumentException {
        Paragraph line = new Paragraph(text, font);
        line.setAlignment(Paragraph.ALIGN_RIGHT);
        document.add(line);
    }

    private void addCellToTable(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }
}
