package org.ved.crm.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ved.crm.billing.Invoice;
import org.ved.crm.billing.InvoicePdfService;
import org.ved.crm.billing.InvoiceRepository;
import org.ved.crm.billing.TaxType;

import java.math.RoundingMode;
import java.util.UUID;

// ── EmailService ──────────────────────────────────────────────
// Sends transactional emails for Akuner Life Sciences CRM
// Uses Spring JavaMailSender with Gmail SMTP
// All email methods are @Async — they run in a background thread
// so they never block the main invoice generation flow

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final InvoicePdfService invoicePdfService;
    private final InvoiceRepository invoiceRepository;

    private static final String FROM = "Akuner Life Sciences <akunarlc9@gmail.com>";

    // ── Send Invoice Email ────────────────────────────────────
    // Called after invoice is generated
    // Sends email with PDF attachment to the buyer
    // @Async — runs in background, never blocks invoice generation
    @Async
    public void sendInvoiceEmail(UUID invoiceId, String buyerEmail, String buyerName) {

        if (buyerEmail == null || buyerEmail.isBlank()) {
            log.warn("Skipping invoice email — buyer email missing for invoice: {}", invoiceId);
            return;
        }


        try {
            // Generate PDF — fetches invoice fresh from DB in this async thread
            byte[] pdfBytes = invoicePdfService.generatePdf(invoiceId);

            // Fetch invoice details for email body
            Invoice invoice = invoiceRepository.findByIdWithDetails(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

            // Build MIME email — supports HTML body + attachment
            MimeMessage message = mailSender.createMimeMessage();

            // multipart = true — enables attachments
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM);
            helper.setTo(buyerEmail);
            helper.setSubject("Invoice " + invoice.getInvoiceNumber()
                    + " from Akuner Life Sciences");

            // HTML email body
            helper.setText(buildEmailBody(invoice, buyerName), true);

            // Attach PDF
            helper.addAttachment(
                    invoice.getInvoiceNumber() + ".pdf",
                    new org.springframework.core.io.ByteArrayResource(pdfBytes),
                    "application/pdf"
            );

            mailSender.send(message);

            log.info("Invoice email sent successfully to {} for invoice: {}",
                    buyerEmail, invoice.getInvoiceNumber());

        } catch (MessagingException e) {
            log.error("Failed to send invoice email for invoice {}: {}",
                    invoiceId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending invoice email for invoice {}: {}",
                    invoiceId, e.getMessage());
        }
    }

    // ── Email Body Builder ────────────────────────────────────
    // Builds a clean professional HTML email body
    private String buildEmailBody(Invoice invoice, String buyerName) {

        boolean isIgst = invoice.getTaxType() == TaxType.IGST;
        String taxInfo = isIgst
                ? "IGST: Rs." + invoice.getTotalIgst().setScale(2, RoundingMode.HALF_UP)
                : "CGST: Rs." + invoice.getTotalCgst().setScale(2, RoundingMode.HALF_UP)
                + " | SGST: Rs." + invoice.getTotalSgst().setScale(2, RoundingMode.HALF_UP);

        String netAmount = "Rs." + invoice.getGrandTotal()
                .setScale(0, RoundingMode.HALF_UP)
                .add(java.math.BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 24px; }
                    .header { background: #C41E3A; padding: 20px 24px; border-radius: 8px 8px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 22px; }
                    .header p { color: rgba(255,255,255,0.85); margin: 4px 0 0; font-size: 13px; }
                    .body { background: #f9f9f9; padding: 24px; border: 1px solid #e0e0e0; }
                    .summary { background: white; border-radius: 8px; padding: 16px; margin: 16px 0; }
                    .row { display: flex; justify-content: space-between; padding: 6px 0;
                           border-bottom: 1px solid #f0f0f0; font-size: 14px; }
                    .row:last-child { border-bottom: none; }
                    .label { color: #666; }
                    .value { font-weight: 600; color: #333; }
                    .net { background: #fff5f5; border: 1px solid #C41E3A; border-radius: 6px;
                           padding: 12px 16px; text-align: right; margin: 16px 0; }
                    .net .amount { font-size: 22px; font-weight: 700; color: #C41E3A; }
                    .footer { text-align: center; padding: 16px; font-size: 12px; color: #999; }
                    .note { background: #fffbe6; border-left: 3px solid #f0a500; padding: 10px 14px;
                            font-size: 13px; border-radius: 0 4px 4px 0; margin: 16px 0; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <h1>&#9670; Akuner Life Sciences</h1>
                      <p>GST Invoice — %s</p>
                    </div>
                    <div class="body">
                      <p>Dear <strong>%s</strong>,</p>
                      <p>Please find your GST invoice attached to this email.</p>

                      <div class="summary">
                        <div class="row">
                          <span class="label">Invoice No.</span>
                          <span class="value">%s</span>
                        </div>
                        <div class="row">
                          <span class="label">Invoice Date</span>
                          <span class="value">%s</span>
                        </div>
                        <div class="row">
                          <span class="label">Sales Rep</span>
                          <span class="value">%s</span>
                        </div>
                        <div class="row">
                          <span class="label">Tax Type</span>
                          <span class="value">%s</span>
                        </div>
                        <div class="row">
                          <span class="label">Subtotal</span>
                          <span class="value">Rs.%s</span>
                        </div>
                        <div class="row">
                          <span class="label">%s</span>
                          <span class="value">%s</span>
                        </div>
                      </div>

                      <div class="net">
                        <div style="font-size:13px; color:#666; margin-bottom:4px;">Net Amount Payable</div>
                        <div class="amount">%s</div>
                      </div>

                      <div class="note">
                        The invoice PDF is attached to this email. Please retain it for your records.
                      </div>

                      <p style="font-size:13px; color:#666;">
                        For any queries, please contact us at
                        <a href="mailto:akunerlc9@gmail.com">akunerlc9@gmail.com</a>
                        or call <strong>9422873109</strong>.
                      </p>
                    </div>
                    <div class="footer">
                      &copy; 2026 Akuner Life Sciences. All rights reserved.<br/>
                      H No. 1-18-407, Shop No. 1, Purna Road, Nandkishor Nagar, Nanded - 431 605 (M.S.)
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                invoice.getInvoiceNumber(),
                buyerName,
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                invoice.getRep().getFullName(),
                isIgst ? "IGST (Inter-state)" : "CGST + SGST (Intra-state)",
                invoice.getSubtotal().setScale(2, RoundingMode.HALF_UP),
                isIgst ? "IGST" : "CGST + SGST",
                taxInfo,
                netAmount
        );
    }
}