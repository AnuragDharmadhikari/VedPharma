package org.ved.crm.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ved.crm.common.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    // GET all invoices
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getAllInvoices() {
        List<InvoiceDto> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(ApiResponse.success("Invoices retrieved successfully", invoices));
    }

    // GET invoice by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceById(@PathVariable UUID id) {
        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success("Invoice retrieved successfully", invoice));
    }

    // POST generate invoice from a confirmed order
    // No request body — orderId in the path is all we need
    // The service reads the order and computes everything server-side
    @PostMapping("/generate/{orderId}")
    public ResponseEntity<ApiResponse<InvoiceDto>> generateInvoice(@PathVariable UUID orderId) {
        InvoiceDto invoice = invoiceService.generateInvoice(orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice generated successfully", invoice));
    }

    // PATCH update invoice status — DRAFT → ISSUED → PAID
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<InvoiceDto>> updateInvoiceStatus(
            @PathVariable UUID id,
            @RequestParam InvoiceStatus status) {
        InvoiceDto invoice = invoiceService.updateInvoiceStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Invoice status updated successfully", invoice));
    }

    // GET outstanding invoices with remaining balance
// Used by PaymentNewPage to show accurate remaining amounts per invoice
    @GetMapping("/outstanding")
    public ResponseEntity<ApiResponse<List<OutstandingInvoiceDto>>> getOutstandingInvoices() {
        return ResponseEntity.ok(ApiResponse.success(
                "Outstanding invoices retrieved",
                invoiceService.getOutstandingInvoices()));
    }

    // GET PDF — streams the invoice as a downloadable PDF file
    // Returns application/pdf with Content-Disposition header
    // so browser either downloads or opens it in a PDF viewer
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable UUID id) {

        byte[] pdfBytes = invoicePdfService.generatePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // inline → browser opens PDF viewer
        // attachment → browser downloads the file
        headers.setContentDispositionFormData("inline",
                "invoice-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

}