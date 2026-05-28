package org.ved.crm.billing;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.common.exception.ResourceNotFoundException;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

// ── InvoicePdfService ─────────────────────────────────────────
// Generates a GST-compliant PDF invoice for Akuner Life Sciences
// Uses OpenPDF (free fork of iText 5) for PDF generation
// Returns a byte array — controller streams it as application/pdf

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoicePdfService {

    private final InvoiceRepository invoiceRepository;

    // ── Company Constants ─────────────────────────────────────
    private static final String COMPANY_NAME    = "Akuner Life Sciences";
    private static final String COMPANY_ADDRESS = "H No. 1-18-407, Shop No. 1, Purna Road";
    private static final String COMPANY_CITY    = "Nandkishor Nagar, Nanded - 431 605 (M.S.)";
    private static final String COMPANY_PHONE   = "Mob: 9422873109";
    private static final String COMPANY_GSTIN   = "GSTIN: 27AJGPD5800L1ZA";
    private static final String COMPANY_DL      = "DL No: 20B-146368 / 21B-316369";
    private static final String BANK_NAME       = "Bank of Maharashtra";
    private static final String BANK_ACCOUNT    = "A/c No: 60247682542";
    private static final String BANK_IFSC       = "IFSC: MAHB0000720";
    private static final String BANK_BRANCH     = "Branch: Assadullabad, Nanded - 431 605";
    private static final String DECLARATION     =
            "This is to certify that our S.T.No. in force on date of this sale. " +
                    "The drugs supplied under this invoice do not contravene in any way " +
                    "the provision of section 18 of the Drugs & Cosmetics Act, 1940. " +
                    "Subject to Nanded Jurisdiction.";

    // ── Colors ────────────────────────────────────────────────
    private static final Color COLOR_PRIMARY   = new Color(180, 20, 40);
    private static final Color COLOR_HEADER_BG = new Color(245, 245, 245);
    private static final Color COLOR_BORDER    = new Color(200, 200, 200);
    private static final Color COLOR_WHITE     = Color.WHITE;
    private static final Color COLOR_DARK_TEXT = new Color(30, 30, 30);
    private static final Color COLOR_NET_BG    = new Color(255, 235, 235);

    // ── Fonts ─────────────────────────────────────────────────
    private static final Font FONT_TITLE     = new Font(Font.HELVETICA, 18, Font.BOLD,   COLOR_PRIMARY);
    private static final Font FONT_HEADING   = new Font(Font.HELVETICA, 9,  Font.BOLD,   COLOR_DARK_TEXT);
    private static final Font FONT_NORMAL    = new Font(Font.HELVETICA, 8,  Font.NORMAL, COLOR_DARK_TEXT);
    private static final Font FONT_SMALL     = new Font(Font.HELVETICA, 7,  Font.NORMAL, COLOR_DARK_TEXT);
    private static final Font FONT_BOLD      = new Font(Font.HELVETICA, 8,  Font.BOLD,   COLOR_DARK_TEXT);
    private static final Font FONT_LABEL     = new Font(Font.HELVETICA, 7,  Font.BOLD,   new Color(100, 100, 100));
    private static final Font FONT_COMPANY   = new Font(Font.HELVETICA, 14, Font.BOLD,   COLOR_PRIMARY);
    private static final Font FONT_TOTAL     = new Font(Font.HELVETICA, 9,  Font.BOLD,   COLOR_PRIMARY);
    private static final Font FONT_TABLE_HDR = new Font(Font.HELVETICA, 7,  Font.BOLD,   COLOR_WHITE);

    private static final DateTimeFormatter DATE_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EXPIRY_FMT = DateTimeFormatter.ofPattern("MM/yyyy");

    // ── Rs. helper ────────────────────────────────────────────
    // OpenPDF's built-in Helvetica doesn't support the Rs. Unicode character
    // Use "Rs." prefix — standard in Indian accounting PDFs
    private static String rs(BigDecimal amount) {
        return "Rs." + amount.setScale(2, RoundingMode.HALF_UP);
    }

    // ── Main Entry Point ──────────────────────────────────────
    public byte[] generatePdf(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice", "id", invoiceId));
        try {
            return buildPdf(invoice);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate PDF for invoice: " + invoice.getInvoiceNumber(), e);
        }
    }

    // ── PDF Builder ───────────────────────────────────────────
    private byte[] buildPdf(Invoice invoice) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        doc.add(buildHeader(invoice));
        doc.add(Chunk.NEWLINE);
        doc.add(buildBillToSection(invoice));
        doc.add(Chunk.NEWLINE);
        doc.add(buildLineItemsTable(invoice));
        doc.add(Chunk.NEWLINE);
        doc.add(buildTotalsSection(invoice));
        doc.add(Chunk.NEWLINE);
        doc.add(buildAmountInWords(invoice));
        doc.add(Chunk.NEWLINE);
        doc.add(buildFooter());

        doc.close();
        return baos.toByteArray();
    }

    // ── Section 1: Header ─────────────────────────────────────
    private PdfPTable buildHeader(Invoice invoice) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{60f, 40f});

        // Left — company info
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(4);
        leftCell.addElement(new Paragraph(COMPANY_NAME, FONT_COMPANY));
        leftCell.addElement(new Paragraph(COMPANY_ADDRESS, FONT_NORMAL));
        leftCell.addElement(new Paragraph(COMPANY_CITY, FONT_NORMAL));
        leftCell.addElement(new Paragraph(COMPANY_PHONE, FONT_NORMAL));
        leftCell.addElement(new Paragraph(COMPANY_GSTIN, FONT_BOLD));
        leftCell.addElement(new Paragraph(COMPANY_DL, FONT_NORMAL));

        // Right — invoice title
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.LEFT);
        rightCell.setBorderColor(COLOR_PRIMARY);
        rightCell.setBorderWidth(2f);
        rightCell.setPadding(8);
        rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        rightCell.setBackgroundColor(new Color(255, 248, 248));

        Paragraph title = new Paragraph("GST INVOICE", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        rightCell.addElement(title);

        Paragraph separator = new Paragraph(
                "________________________________", FONT_LABEL);
        separator.setAlignment(Element.ALIGN_CENTER);
        rightCell.addElement(separator);

        Paragraph invNo = new Paragraph(invoice.getInvoiceNumber(), FONT_HEADING);
        invNo.setAlignment(Element.ALIGN_CENTER);
        rightCell.addElement(invNo);

        Paragraph invDate = new Paragraph(
                "Date: " + invoice.getInvoiceDate().format(DATE_FMT), FONT_NORMAL);
        invDate.setAlignment(Element.ALIGN_CENTER);
        rightCell.addElement(invDate);

        Paragraph status = new Paragraph(
                "Status: " + invoice.getStatus().name(), FONT_LABEL);
        status.setAlignment(Element.ALIGN_CENTER);
        rightCell.addElement(status);

        table.addCell(leftCell);
        table.addCell(rightCell);
        return table;
    }

    // ── Section 2: Bill To + Invoice Meta ─────────────────────
    private PdfPTable buildBillToSection(Invoice invoice) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        String buyerName, buyerGstin, buyerAddress, buyerCity, buyerPhone;

        if (invoice.getBilledTo() == BilledTo.STOCKIST
                && invoice.getStockist() != null) {
            buyerName    = invoice.getStockist().getFirmName();
            buyerGstin   = nvl(invoice.getStockist().getGstin(), "N/A");
            buyerAddress = nvl(invoice.getStockist().getAddress(), "");
            buyerCity    = invoice.getStockist().getCity();
            buyerPhone   = invoice.getStockist().getPhone();
        } else {
            buyerName    = invoice.getChemist().getFirmName();
            buyerGstin   = nvl(invoice.getChemist().getGstin(), "N/A");
            buyerAddress = nvl(invoice.getChemist().getAddress(), "");
            buyerCity    = invoice.getChemist().getCity();
            buyerPhone   = invoice.getChemist().getPhone();
        }

        // Left — Bill To
        PdfPCell billToCell = new PdfPCell();
        billToCell.setBorderColor(COLOR_BORDER);
        billToCell.setPadding(6);
        addLV(billToCell, "BILL TO", "");
        addLV(billToCell, "M/s.", buyerName);
        addLV(billToCell, "GSTIN:", buyerGstin);
        addLV(billToCell, "Address:", buyerAddress + ", " + buyerCity);
        addLV(billToCell, "Phone:", buyerPhone);

        // Right — Invoice meta
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorderColor(COLOR_BORDER);
        metaCell.setPadding(6);
        addLV(metaCell, "Invoice No:", invoice.getInvoiceNumber());
        addLV(metaCell, "Date:", invoice.getInvoiceDate().format(DATE_FMT));
        addLV(metaCell, "Sales Rep:", invoice.getRep().getFullName());
        addLV(metaCell, "Tax Type:", invoice.getTaxType() == TaxType.CGST_SGST
                ? "CGST + SGST (Intra-state)" : "IGST (Inter-state)");
        addLV(metaCell, "Billed To:", invoice.getBilledTo().name());
        addLV(metaCell, "Transport:", "—");
        addLV(metaCell, "LR No:", "—");

        table.addCell(billToCell);
        table.addCell(metaCell);
        return table;
    }

    // ── Section 3: Line Items Table ───────────────────────────
    private PdfPTable buildLineItemsTable(Invoice invoice) {
        boolean isIgst = invoice.getTaxType() == TaxType.IGST;

        // 14 columns
        PdfPTable table = new PdfPTable(14);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{
                3f,   // Sr
                7f,   // HSN
                16f,  // Description
                5f,   // Pack
                8f,   // Batch
                7f,   // Expiry
                7f,   // MRP
                4f,   // Qty
                5f,   // Free
                7f,   // Rate
                6f,   // Disc%
                5f,   // GST%
                8f,   // IGST
                9f    // Amount
        });

        // Header row
        String[] headers = {
                "Sr", "HSN", "Description", "Pack", "Batch No.",
                "Expiry", "MRP", "Qty", "Free", "Rate",
                "Disc%", "GST%",
                isIgst ? "IGST" : "SGST",
                isIgst ? "Amount" : "CGST"
        };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_TABLE_HDR));
            cell.setBackgroundColor(COLOR_PRIMARY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(4);
            table.addCell(cell);
        }

        // Data rows
        List<InvoiceLineItemPdfDto> items = invoice.getLineItems()
                .stream().map(this::toDto).toList();

        int sr = 1;
        for (InvoiceLineItemPdfDto item : items) {
            Color bg = (sr % 2 == 0) ? COLOR_HEADER_BG : COLOR_WHITE;

            addTC(table, String.valueOf(sr++), bg, Element.ALIGN_CENTER);
            addTC(table, item.hsnCode(), bg, Element.ALIGN_CENTER);
            addTC(table, item.productName(), bg, Element.ALIGN_LEFT);
            addTC(table, "—", bg, Element.ALIGN_CENTER);
            addTC(table, nvl(item.batchNumber(), "—"), bg, Element.ALIGN_CENTER);
            addTC(table, item.expiryDate() != null
                            ? item.expiryDate().format(EXPIRY_FMT) : "—",
                    bg, Element.ALIGN_CENTER);
            addTC(table, rs(item.mrp()), bg, Element.ALIGN_RIGHT);
            addTC(table, String.valueOf(item.quantity()), bg, Element.ALIGN_CENTER);
            addTC(table, item.freeQuantity() > 0
                            ? String.valueOf(item.freeQuantity()) : "—",
                    bg, Element.ALIGN_CENTER);
            addTC(table, rs(item.unitPrice()), bg, Element.ALIGN_RIGHT);
            addTC(table, item.discountPct() + "%", bg, Element.ALIGN_CENTER);
            addTC(table, item.gstRate() + "%", bg, Element.ALIGN_CENTER);

            if (isIgst) {
                addTC(table, rs(item.igstAmt()), bg, Element.ALIGN_RIGHT);
                addTC(table, rs(item.lineTotal()), bg, Element.ALIGN_RIGHT);
            } else {
                addTC(table, rs(item.sgstAmt()), bg, Element.ALIGN_RIGHT);
                addTC(table, rs(item.cgstAmt()), bg, Element.ALIGN_RIGHT);
            }
        }

        // Totals row at bottom of table
        PdfPCell totalLabelCell = new PdfPCell(
                new Phrase("TOTAL", FONT_TABLE_HDR));
        totalLabelCell.setColspan(7);
        totalLabelCell.setBackgroundColor(COLOR_PRIMARY);
        totalLabelCell.setPadding(4);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalLabelCell);

        int totalQty = items.stream().mapToInt(InvoiceLineItemPdfDto::quantity).sum();
        int totalFree = items.stream().mapToInt(InvoiceLineItemPdfDto::freeQuantity).sum();

        addTC(table, String.valueOf(totalQty),
                COLOR_PRIMARY, Element.ALIGN_CENTER, FONT_TABLE_HDR);
        addTC(table, totalFree > 0 ? String.valueOf(totalFree) : "—",
                COLOR_PRIMARY, Element.ALIGN_CENTER, FONT_TABLE_HDR);

        PdfPCell blankCell = new PdfPCell(new Phrase("", FONT_TABLE_HDR));
        blankCell.setColspan(2);
        blankCell.setBackgroundColor(COLOR_PRIMARY);
        blankCell.setPadding(4);
        table.addCell(blankCell);

        addTC(table, rs(invoice.getSubtotal()),
                COLOR_PRIMARY, Element.ALIGN_RIGHT, FONT_TABLE_HDR);
        addTC(table, rs(isIgst ? invoice.getTotalIgst()
                        : invoice.getTotalCgst().add(invoice.getTotalSgst())),
                COLOR_PRIMARY, Element.ALIGN_RIGHT, FONT_TABLE_HDR);

        return table;
    }

    // ── Section 4: Totals ─────────────────────────────────────
    private PdfPTable buildTotalsSection(Invoice invoice) {
        boolean isIgst = invoice.getTaxType() == TaxType.IGST;

        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(100);
        outer.setWidths(new float[]{55f, 45f});

        // Empty left spacer — no rowspan needed, use nested table on right
        PdfPCell spacer = new PdfPCell(new Phrase(""));
        spacer.setBorder(Rectangle.NO_BORDER);
        spacer.setMinimumHeight(100f);
        outer.addCell(spacer);

        // Right side — nested table with all totals
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(100);

        addTR(totals, "Subtotal (Taxable):", rs(invoice.getSubtotal()));
        addTR(totals, "Total Discount:",
                invoice.getTotalDiscount().compareTo(BigDecimal.ZERO) > 0
                        ? "- " + rs(invoice.getTotalDiscount())
                        : "Rs.0.00");

        if (isIgst) {
            addTR(totals, "IGST:", rs(invoice.getTotalIgst()));
        } else {
            addTR(totals, "SGST:", rs(invoice.getTotalSgst()));
            addTR(totals, "CGST:", rs(invoice.getTotalCgst()));
        }

        BigDecimal roundOff = invoice.getGrandTotal()
                .subtract(invoice.getGrandTotal().setScale(0, RoundingMode.HALF_UP))
                .abs().setScale(2, RoundingMode.HALF_UP);
        addTR(totals, "Round Off:", rs(roundOff));

        // NET AMOUNT row
        PdfPCell netLabel = new PdfPCell(new Phrase("NET AMOUNT:", FONT_TOTAL));
        netLabel.setPadding(6);
        netLabel.setFixedHeight(30f);
        netLabel.setBorderColor(COLOR_PRIMARY);
        netLabel.setBorderWidth(1.5f);
        netLabel.setBackgroundColor(COLOR_NET_BG);

        PdfPCell netValue = new PdfPCell(
                new Phrase("Rs." + invoice.getGrandTotal()
                        .setScale(0, RoundingMode.HALF_UP)
                        .add(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP), FONT_TOTAL));
        netValue.setPadding(6);
        netValue.setFixedHeight(30f);
        netValue.setBorderColor(COLOR_PRIMARY);
        netValue.setBorderWidth(1.5f);
        netValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netValue.setBackgroundColor(COLOR_NET_BG);

        totals.addCell(netLabel);
        totals.addCell(netValue);

        PdfPCell totalsWrapper = new PdfPCell(totals);
        totalsWrapper.setPadding(0);
        totalsWrapper.setBorder(Rectangle.NO_BORDER);
        outer.addCell(totalsWrapper);

        return outer;
    }
    // ── Section 5: Amount in Words ────────────────────────────
    private PdfPTable buildAmountInWords(Invoice invoice) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        long amount = invoice.getGrandTotal()
                .setScale(0, RoundingMode.HALF_UP).longValue();
        String words = "Amount in Words: "
                + numberToWords(amount) + " Rupees Only";

        PdfPCell cell = new PdfPCell(new Phrase(words, FONT_BOLD));
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setPadding(6);
        cell.setBorderColor(COLOR_BORDER);
        table.addCell(cell);
        return table;
    }

    // ── Section 6: Footer ─────────────────────────────────────
    private PdfPTable buildFooter() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        // Bank details
        PdfPCell bankCell = new PdfPCell();
        bankCell.setPadding(6);
        bankCell.setBorderColor(COLOR_BORDER);

        Paragraph bankTitle = new Paragraph("BANK DETAILS", FONT_HEADING);
        bankTitle.setSpacingAfter(4);
        bankCell.addElement(bankTitle);
        bankCell.addElement(new Paragraph(BANK_NAME, FONT_NORMAL));
        bankCell.addElement(new Paragraph(BANK_ACCOUNT, FONT_NORMAL));
        bankCell.addElement(new Paragraph(BANK_IFSC, FONT_NORMAL));
        bankCell.addElement(new Paragraph(BANK_BRANCH, FONT_NORMAL));

        // Declaration + signature
        PdfPCell declCell = new PdfPCell();
        declCell.setPadding(6);
        declCell.setBorderColor(COLOR_BORDER);

        Paragraph decl = new Paragraph(DECLARATION, FONT_SMALL);
        decl.setSpacingAfter(20);
        declCell.addElement(decl);

        Paragraph sig = new Paragraph("For: " + COMPANY_NAME, FONT_BOLD);
        sig.setAlignment(Element.ALIGN_RIGHT);
        declCell.addElement(sig);

        Paragraph authSign = new Paragraph("Auth. Sign.", FONT_LABEL);
        authSign.setAlignment(Element.ALIGN_RIGHT);
        declCell.addElement(authSign);

        table.addCell(bankCell);
        table.addCell(declCell);
        return table;
    }

    // ── Helpers ───────────────────────────────────────────────

    private void addLV(PdfPCell cell, String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", FONT_LABEL));
        p.add(new Chunk(value, label.equals("M/s.") || label.equals("Invoice No:")
                ? FONT_HEADING : FONT_NORMAL));
        p.setSpacingAfter(2);
        cell.addElement(p);
    }

    private void addTC(PdfPTable table, String text, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_SMALL));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        cell.setBorderColor(COLOR_BORDER);
        table.addCell(cell);
    }

    private void addTC(PdfPTable table, String text,
                       Color bg, int align, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3);
        cell.setBorderColor(COLOR_BORDER);
        table.addCell(cell);
    }

    private void addTR(PdfPTable table, String label, String value) {
        PdfPCell lc = new PdfPCell(new Phrase(label, FONT_BOLD));
        lc.setPadding(4);
        lc.setBorderColor(COLOR_BORDER);

        PdfPCell vc = new PdfPCell(new Phrase(value, FONT_NORMAL));
        vc.setPadding(4);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setBorderColor(COLOR_BORDER);

        table.addCell(lc);
        table.addCell(vc);
    }

    private String nvl(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private InvoiceLineItemPdfDto toDto(InvoiceLineItem item) {
        return new InvoiceLineItemPdfDto(
                item.getProduct().getName(),
                item.getHsnCode(),
                item.getProduct().getMrp(),
                item.getQuantity(),
                item.getFreeQuantity(),
                item.getUnitPrice(),
                item.getDiscountPct(),
                item.getTaxableAmount(),
                item.getCgstAmt(),
                item.getSgstAmt(),
                item.getIgstAmt(),
                item.getLineTotal(),
                item.getBatchNumber(),
                item.getExpiryDate(),
                item.getProduct().getGstRate().getRate()
        );
    }

    // ── Number to Words ───────────────────────────────────────
    private String numberToWords(long n) {
        if (n == 0) return "Zero";
        String[] ones = {"", "One", "Two", "Three", "Four", "Five", "Six",
                "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
                "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
                "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty",
                "Sixty", "Seventy", "Eighty", "Ninety"};
        if (n < 20)      return ones[(int) n];
        if (n < 100)     return tens[(int) n / 10]
                + (n % 10 != 0 ? " " + ones[(int) n % 10] : "");
        if (n < 1000)    return ones[(int) n / 100] + " Hundred"
                + (n % 100 != 0 ? " " + numberToWords(n % 100) : "");
        if (n < 100000)  return numberToWords(n / 1000) + " Thousand"
                + (n % 1000 != 0 ? " " + numberToWords(n % 1000) : "");
        if (n < 10000000) return numberToWords(n / 100000) + " Lakh"
                + (n % 100000 != 0 ? " " + numberToWords(n % 100000) : "");
        return numberToWords(n / 10000000) + " Crore"
                + (n % 10000000 != 0 ? " " + numberToWords(n % 10000000) : "");
    }
}