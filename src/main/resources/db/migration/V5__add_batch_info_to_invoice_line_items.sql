-- V5__add_batch_info_to_invoice_line_items.sql
-- Adds batch number and expiry date to invoice line items
-- Required for GST-compliant pharmaceutical invoices
-- Populated by InventoryService.deductStockForInvoice()
-- when stock is deducted FIFO during invoice generation

ALTER TABLE invoice_line_items
    ADD COLUMN batch_number VARCHAR(100),
    ADD COLUMN expiry_date  DATE;