-- =========================================================================
-- MODULE QUẢN LÝ HÓA ĐƠN – ADHA SNEAKER (PostgreSQL 16)
-- =========================================================================

-- 1. Sequences
CREATE SEQUENCE IF NOT EXISTS seq_invoices START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_invoice_details START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_invoice_histories START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_invoice_cancel_requests START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_invoice_email_histories START WITH 1 INCREMENT BY 1;

-- 2. Bảng INVOICES (Thông tin hóa đơn)
CREATE TABLE IF NOT EXISTS invoices (
    id                  BIGINT PRIMARY KEY DEFAULT nextval('seq_invoices'),
    invoice_code        VARCHAR(40) NOT NULL UNIQUE,
    order_id            BIGINT NOT NULL REFERENCES sales_orders(id),
    original_invoice_id BIGINT REFERENCES invoices(id),
    sale_channel        VARCHAR(20) NOT NULL,
    invoice_type        VARCHAR(20) NOT NULL DEFAULT 'RETAIL',
    record_kind         VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL',
    customer_id         BIGINT REFERENCES customers(id),
    customer_name       VARCHAR(150) NOT NULL,
    customer_phone      VARCHAR(30),
    customer_email      VARCHAR(150),
    customer_address    VARCHAR(350),
    company_name        VARCHAR(220),
    tax_code            VARCHAR(50),
    subtotal            NUMERIC(18, 2) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(18, 2) NOT NULL DEFAULT 0,
    shipping_fee        NUMERIC(18, 2) NOT NULL DEFAULT 0,
    taxable_amount      NUMERIC(18, 2) NOT NULL DEFAULT 0,
    tax_rate            NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    tax_amount          NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(18, 2) NOT NULL DEFAULT 0,
    payment_method      VARCHAR(30) NOT NULL DEFAULT 'CASH',
    payment_status      VARCHAR(20) NOT NULL DEFAULT 'PAID',
    invoice_status      VARCHAR(30) NOT NULL DEFAULT 'ISSUED',
    email_status        VARCHAR(20) NOT NULL DEFAULT 'NOT_SENT',
    issued_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at        TIMESTAMP,
    cancel_reason       VARCHAR(500),
    cancelled_by        VARCHAR(50),
    adjustment_reason   VARCHAR(500),
    note                VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    CONSTRAINT chk_invoices_channel CHECK (sale_channel IN ('POS', 'ONLINE')),
    CONSTRAINT chk_invoices_type CHECK (invoice_type IN ('RETAIL', 'VAT')),
    CONSTRAINT chk_invoices_kind CHECK (record_kind IN ('ORIGINAL', 'ADJUSTMENT', 'REPLACEMENT')),
    CONSTRAINT chk_invoices_status CHECK (invoice_status IN ('ISSUED', 'PENDING_CANCEL', 'CANCELLED', 'ADJUSTED')),
    CONSTRAINT chk_invoices_email_status CHECK (email_status IN ('NOT_SENT', 'SENDING', 'SENT', 'FAILED'))
);

-- Ràng buộc chống phát sinh hóa đơn gốc trùng lặp cho cùng 1 đơn hàng (Idempotency ở tầng DB)
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoices_original_order
    ON invoices (order_id)
    WHERE record_kind = 'ORIGINAL';

CREATE INDEX IF NOT EXISTS idx_invoices_order_id ON invoices (order_id);
CREATE INDEX IF NOT EXISTS idx_invoices_issued_at ON invoices (issued_at DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices (invoice_status);
CREATE INDEX IF NOT EXISTS idx_invoices_type ON invoices (invoice_type);
CREATE INDEX IF NOT EXISTS idx_invoices_channel ON invoices (sale_channel);
CREATE INDEX IF NOT EXISTS idx_invoices_email_status ON invoices (email_status);
CREATE INDEX IF NOT EXISTS idx_invoices_tax_code ON invoices (tax_code);

-- 3. Bảng INVOICE_DETAILS (Chi tiết sản phẩm hóa đơn)
CREATE TABLE IF NOT EXISTS invoice_details (
    id                 BIGINT PRIMARY KEY DEFAULT nextval('seq_invoice_details'),
    invoice_id         BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id         BIGINT,
    product_variant_id BIGINT,
    product_code       VARCHAR(50),
    product_name       VARCHAR(255) NOT NULL,
    sku                VARCHAR(80),
    variant_label      VARCHAR(150),
    quantity           INTEGER NOT NULL,
    unit_price         NUMERIC(18, 2) NOT NULL DEFAULT 0,
    discount_amount    NUMERIC(18, 2) NOT NULL DEFAULT 0,
    tax_rate           NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    tax_amount         NUMERIC(18, 2) NOT NULL DEFAULT 0,
    line_total         NUMERIC(18, 2) NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(50),
    updated_by         VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_invoice_details_invoice_id ON invoice_details (invoice_id);

-- 4. Bảng INVOICE_HISTORIES (Lịch sử thao tác hóa đơn - Audit bất biến)
CREATE TABLE IF NOT EXISTS invoice_histories (
    id          BIGINT PRIMARY KEY DEFAULT nextval('seq_invoice_histories'),
    invoice_id  BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    action      VARCHAR(40) NOT NULL,
    from_status VARCHAR(30),
    to_status   VARCHAR(30),
    actor       VARCHAR(80) NOT NULL,
    reason      VARCHAR(500),
    detail      VARCHAR(1000),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoice_histories_invoice_id ON invoice_histories (invoice_id, created_at DESC);

-- 5. Bảng INVOICE_CANCEL_REQUESTS (Yêu cầu hủy hóa đơn chờ duyệt từ Nhân viên)
CREATE TABLE IF NOT EXISTS invoice_cancel_requests (
    id           BIGINT PRIMARY KEY DEFAULT nextval('seq_invoice_cancel_requests'),
    invoice_id   BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    requested_by VARCHAR(80) NOT NULL,
    reason       VARCHAR(500) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by  VARCHAR(80),
    review_note  VARCHAR(500),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at  TIMESTAMP,
    CONSTRAINT chk_inv_cancel_req_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_inv_cancel_req_invoice_id ON invoice_cancel_requests (invoice_id);
CREATE INDEX IF NOT EXISTS idx_inv_cancel_req_status ON invoice_cancel_requests (status);

-- 6. Bảng INVOICE_EMAIL_HISTORIES (Lịch sử gửi hóa đơn qua Email)
CREATE TABLE IF NOT EXISTS invoice_email_histories (
    id              BIGINT PRIMARY KEY DEFAULT nextval('seq_invoice_email_histories'),
    invoice_id      BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    recipient_email VARCHAR(150) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    send_status     VARCHAR(20) NOT NULL,
    sent_by         VARCHAR(80) NOT NULL,
    sent_at         TIMESTAMP,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    error_message   VARCHAR(1000),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_inv_email_hist_status CHECK (send_status IN ('SENDING', 'SENT', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_inv_email_hist_invoice_id ON invoice_email_histories (invoice_id, created_at DESC);

-- 7. Backfill tự động hóa đơn cho các đơn hàng COMPLETED đã có sẵn trong DB (idempotent)
DO $$
DECLARE
    rec RECORD;
    v_inv_id BIGINT;
    v_inv_code VARCHAR(40);
    v_tax_rate NUMERIC(5,2) := 8.00;
    v_subtotal NUMERIC(18,2);
    v_tax_amount NUMERIC(18,2);
    v_channel VARCHAR(20);
    v_cust_name VARCHAR(150);
    v_cust_phone VARCHAR(30);
    v_cust_email VARCHAR(150);
    v_cust_addr VARCHAR(350);
    drec RECORD;
    d_pretax NUMERIC(18,2);
    d_tax NUMERIC(18,2);
BEGIN
    FOR rec IN
        SELECT so.id, so.order_code, so.customer_id, so.employee_id, so.order_date,
               so.total_amount, so.final_amount, so.payment_method, so.note, so.created_by,
               c.full_name, c.phone, c.email, c.address
        FROM sales_orders so
        LEFT JOIN customers c ON c.id = so.customer_id
        WHERE so.status = 'COMPLETED'
          AND NOT EXISTS (
              SELECT 1 FROM invoices inv WHERE inv.order_id = so.id AND inv.record_kind = 'ORIGINAL'
          )
        ORDER BY so.id ASC
    LOOP
        v_channel := CASE WHEN rec.employee_id IS NULL THEN 'ONLINE' ELSE 'POS' END;
        v_cust_name := COALESCE(rec.full_name, 'Khách lẻ tại quầy');
        v_cust_phone := rec.phone;
        v_cust_email := rec.email;
        v_cust_addr := rec.address;

        -- Tính tách thuế GTGT 8% đã bao gồm trong giá bán để tổng thanh toán khớp chính xác với đơn hàng
        v_subtotal := ROUND(rec.final_amount / (1 + v_tax_rate / 100.0), 0);
        v_tax_amount := rec.final_amount - v_subtotal;

        v_inv_id := nextval('seq_invoices');
        v_inv_code := 'INV-' || TO_CHAR(COALESCE(rec.order_date, CURRENT_TIMESTAMP), 'YYYYMMDD') || '-' || LPAD(v_inv_id::TEXT, 4, '0');

        INSERT INTO invoices (
            id, invoice_code, order_id, sale_channel, invoice_type, record_kind,
            customer_id, customer_name, customer_phone, customer_email, customer_address,
            subtotal, discount_amount, shipping_fee, taxable_amount, tax_rate, tax_amount, total_amount,
            payment_method, payment_status, invoice_status, email_status,
            issued_at, note, created_at, updated_at, created_by, updated_by
        ) VALUES (
            v_inv_id, v_inv_code, rec.id, v_channel, 'RETAIL', 'ORIGINAL',
            rec.customer_id, v_cust_name, v_cust_phone, v_cust_email, v_cust_addr,
            v_subtotal, 0, 0, v_subtotal, v_tax_rate, v_tax_amount, rec.final_amount,
            COALESCE(rec.payment_method, 'CASH'), 'PAID', 'ISSUED', 'NOT_SENT',
            COALESCE(rec.order_date, CURRENT_TIMESTAMP), rec.note,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, COALESCE(rec.created_by, 'SYSTEM'), COALESCE(rec.created_by, 'SYSTEM')
        );

        FOR drec IN
            SELECT sod.quantity, sod.unit_price, sod.subtotal,
                   pv.id AS pv_id, pv.sku,
                   p.id AS p_id, p.product_code, p.product_name,
                   s.size_value, col.color_name
            FROM sales_order_details sod
            JOIN product_variants pv ON pv.id = sod.product_variant_id
            JOIN products p ON p.id = pv.product_id
            LEFT JOIN sizes s ON s.id = pv.size_id
            LEFT JOIN colors col ON col.id = pv.color_id
            WHERE sod.sales_order_id = rec.id
            ORDER BY sod.id ASC
        LOOP
            d_pretax := ROUND(drec.subtotal / (1 + v_tax_rate / 100.0), 0);
            d_tax := drec.subtotal - d_pretax;
            INSERT INTO invoice_details (
                invoice_id, product_id, product_variant_id, product_code, product_name,
                sku, variant_label, quantity, unit_price, discount_amount,
                tax_rate, tax_amount, line_total, created_at, updated_at, created_by, updated_by
            ) VALUES (
                v_inv_id, drec.p_id, drec.pv_id, drec.product_code, drec.product_name,
                drec.sku, COALESCE('Size ' || drec.size_value, '') || COALESCE(' / ' || drec.color_name, ''),
                drec.quantity, drec.unit_price, 0,
                v_tax_rate, d_tax, drec.subtotal,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, COALESCE(rec.created_by, 'SYSTEM'), COALESCE(rec.created_by, 'SYSTEM')
            );
        END LOOP;

        INSERT INTO invoice_histories (
            invoice_id, action, from_status, to_status, actor, reason, detail, created_at
        ) VALUES (
            v_inv_id, 'ISSUED', NULL, 'ISSUED', COALESCE(rec.created_by, 'SYSTEM'),
            'Đồng bộ hóa đơn tự động từ đơn hàng hoàn thành ' || rec.order_code,
            'Kênh ' || v_channel || ' - Loại RETAIL - Tổng ' || rec.final_amount,
            COALESCE(rec.order_date, CURRENT_TIMESTAMP)
        );
    END LOOP;
END $$;
