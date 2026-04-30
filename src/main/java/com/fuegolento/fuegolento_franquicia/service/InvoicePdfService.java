package com.fuegolento.fuegolento_franquicia.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.OrderItem;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;


import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

@Service
public class InvoicePdfService {

    // ===== Fallback constants (used when order has no restaurant) =====
    private static final String FALLBACK_NAME = "Fuego Lento";
    private static final String FALLBACK_ADDRESS = "Madrid";
    private static final String FALLBACK_PHONE = "+34 999 999 999";
    private static final String FOOTER_LINE = "Gracias por su visita!";

    // Brand colors
    private static final DeviceRgb BRAND_DARK = new DeviceRgb(15, 23, 43);   // dark navy
    private static final DeviceRgb BRAND_ACCENT = new DeviceRgb(254, 161, 22); // orange
    private static final DeviceRgb LIGHT_BG = new DeviceRgb(248, 249, 251);  // very light gray

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void writeInvoicePdf(Order order, OutputStream out) throws IOException {

        // Resolve restaurant info from order (null-safe)
        Restaurant restaurant = order.getRestaurant();
        String restaurantName = (restaurant != null && restaurant.getName() != null)
                ? restaurant.getName() : FALLBACK_NAME;
        String restaurantAddress = (restaurant != null)
                ? buildAddress(restaurant) : FALLBACK_ADDRESS;
        String restaurantPhone = (restaurant != null && restaurant.getPhone() != null)
                ? restaurant.getPhone() : FALLBACK_PHONE;

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(28, 28, 28, 28);

        // ===== Header block (logo + name) =====
        Table header = new Table(new float[]{1.2f, 4.8f})
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(BRAND_DARK)
                .setBorder(Border.NO_BORDER);

        // Logo
        Image logo = loadLogo("/static/img/logo.png");
        if (logo != null) {
            logo.setAutoScale(true);
            // Make it visually consistent
            logo.setMaxHeight(52);
            Cell logoCell = new Cell().add(logo)
                    .setBorder(Border.NO_BORDER)
                    .setPaddingLeft(12)
                    .setPaddingTop(10)
                    .setPaddingBottom(10);
            header.addCell(logoCell);
        } else {
            header.addCell(new Cell().add(new Paragraph(" "))
                    .setBorder(Border.NO_BORDER));
        }

        // Restaurant name
        Paragraph name = new Paragraph(restaurantName)
                .setFontColor(BRAND_ACCENT)
                .setBold()
                .setFontSize(26)
                .setMargin(0);

        Paragraph subtitle = new Paragraph("Factura")
                .setFontColor(ColorConstants.WHITE)
                .setFontSize(12)
                .setMarginTop(2)
                .setMarginBottom(0);

        Cell titleCell = new Cell()
                .add(name)
                .add(subtitle)
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(10)
                .setPaddingBottom(10);
        header.addCell(titleCell);

        document.add(header);

        // Small separator line
        SolidLine line = new SolidLine(1f);
        line.setColor(new DeviceRgb(230, 230, 230));
        document.add(new LineSeparator(line).setMarginTop(14).setMarginBottom(14));

        // ===== Invoice title =====
        Paragraph invTitle = new Paragraph("Factura · Pedido N°" + order.getId())
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(16)
                .setMarginBottom(12);
        document.add(invTitle);

        // ===== Info table (nice card style) =====
        Table info = new Table(new float[]{2.1f, 5.9f})
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(LIGHT_BG)
                .setBorder(new SolidBorder(new DeviceRgb(235, 235, 235), 1))
                .setMarginBottom(14);

        info.addCell(infoLabel("Cliente:"));
        info.addCell(infoValue(order.getUser().getUsername()));

        info.addCell(infoLabel("Creado el:"));
        info.addCell(infoValue(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "-"));

        info.addCell(infoLabel("Mesa:"));
        info.addCell(infoValue(order.getTableNumber() != null ? String.valueOf(order.getTableNumber()) : "-"));

        info.addCell(infoLabel("Estado:"));
        info.addCell(infoValue(order.getStatus().name()));

        if (order.getCustomerNote() != null && !order.getCustomerNote().isBlank()) {
            info.addCell(infoLabel("Nota general:"));
            info.addCell(infoValue(order.getCustomerNote()));
        }

        document.add(info);

        // ===== Items table =====
        Table items = new Table(new float[]{5.0f, 1.0f, 1.6f, 1.6f})
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(12);

        // Header row
        items.addHeaderCell(th("Plato"));
        items.addHeaderCell(th("Cant."));
        items.addHeaderCell(th("Unit. (€)"));
        items.addHeaderCell(th("Total (€)"));

        boolean zebra = false;
        for (OrderItem oi : order.getItems()) {
            zebra = !zebra;

            String dishName = oi.getDish().getName();

            // Extra details (meat point + note)
            StringBuilder extra = new StringBuilder();
            if (oi.getMeatPoint() != null && !oi.getMeatPoint().isBlank()) {
                extra.append("Punto de carne: ").append(oi.getMeatPoint());
            }
            if (oi.getKitchenNote() != null && !oi.getKitchenNote().isBlank()) {
                if (extra.length() > 0) extra.append(" · ");
                extra.append("Nota: ").append(oi.getKitchenNote());
            }

            Paragraph dishP = new Paragraph(dishName).setMargin(0).setBold();
            if (extra.length() > 0) {
                dishP.add("\n");
                dishP.add(new Paragraph(extra.toString())
                        .setFontSize(10)
                        .setFontColor(new DeviceRgb(90, 90, 90))
                        .setMargin(0));
            }

            DeviceRgb rowBg = zebra ? new DeviceRgb(252, 252, 252) : new DeviceRgb(255, 255, 255);

            items.addCell(td(dishP, rowBg));
            items.addCell(tdRight(String.valueOf(oi.getQuantity()), rowBg));
            items.addCell(tdRight(money(oi.getUnitPrice()), rowBg));
            items.addCell(tdRight(money(oi.getTotalPrice()), rowBg));
        }

        document.add(items);

        // ===== Totals box =====
        BigDecimal total = (order.getTotalPrice() != null)
                ? order.getTotalPrice()
                : order.calculateTotalFromItems();

        total = total.setScale(2, RoundingMode.HALF_UP);

        Table totals = new Table(new float[]{6.6f, 1.4f})
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(6);

        Cell totalLabel = new Cell()
                .add(new Paragraph("TOTAL").setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(BRAND_DARK)
                .setBorder(Border.NO_BORDER)
                .setPadding(10);

        Cell totalValue = new Cell()
                .add(new Paragraph("€" + money(total)).setBold().setFontColor(ColorConstants.WHITE))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(BRAND_DARK)
                .setBorder(Border.NO_BORDER)
                .setPadding(10);

        totals.addCell(totalLabel);
        totals.addCell(totalValue);

        document.add(totals);

        // ===== Footer =====
        document.add(new Paragraph(" ").setMarginTop(18));

        Paragraph footer = new Paragraph(
                restaurantAddress + "\n" +
                restaurantPhone + "\n" +
                FOOTER_LINE
        )
        .setTextAlignment(TextAlignment.CENTER)
        .setFontSize(10)
        .setFontColor(new DeviceRgb(90, 90, 90));

        document.add(footer);

        document.close();
    }

    // ====== Helpers ======

    private Image loadLogo(String classpathLocation) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(classpathLocation)) {
            if (is == null) return null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);

            ImageData data = ImageDataFactory.create(baos.toByteArray());
            return new Image(data);
        }
    }

    private Cell infoLabel(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBorder(Border.NO_BORDER)
                .setPadding(8);
    }

    private Cell infoValue(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "-"))
                .setBorder(Border.NO_BORDER)
                .setPadding(8);
    }

    private Cell th(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(BRAND_DARK)
                .setBorder(Border.NO_BORDER)
                .setPadding(8);
    }

    private Cell td(Paragraph p, DeviceRgb bg) {
        return new Cell()
                .add(p)
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(new DeviceRgb(235, 235, 235), 1))
                .setPadding(8);
    }

    private Cell tdRight(String text, DeviceRgb bg) {
        return new Cell()
                .add(new Paragraph(text).setMargin(0))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(new DeviceRgb(235, 235, 235), 1))
                .setPadding(8);
    }

    private String money(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String buildAddress(Restaurant r) {
        StringBuilder sb = new StringBuilder();
        if (r.getAddress() != null) sb.append(r.getAddress());
        if (r.getCity() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(r.getCity());
        }
        return sb.length() > 0 ? sb.toString() : FALLBACK_ADDRESS;
    }
}