-- =========================================================================
-- 07: CẤU HÌNH HỆ THỐNG ĐỘNG (VAT POS / SMTP) & XÓA MỀM (SOFT DELETE) 8 BẢNG
-- =========================================================================

-- 1. Bảng SYSTEM_SETTINGS (Lưu cấu hình động trong DB: VAT tại quầy, SMTP...)
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key   VARCHAR(80) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description   VARCHAR(500),
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES
    ('POS_VAT_RATE', '8', 'Thuế suất VAT (%) áp dụng tại quầy POS — cấu hình động trong DB, khóa sửa trên giao diện'),
    ('SMTP_HOST', '', 'Máy chủ SMTP gửi Email hóa đơn (VD: smtp.gmail.com)'),
    ('SMTP_PORT', '587', 'Cổng SMTP (587 STARTTLS / 465 SSL)'),
    ('SMTP_USERNAME', '', 'Tài khoản Email SMTP'),
    ('SMTP_PASSWORD', '', 'Mật khẩu ứng dụng (App Password) SMTP'),
    ('SMTP_FROM', 'no-reply@adhasneaker.vn', 'Email hiển thị người gửi')
ON CONFLICT (setting_key) DO NOTHING;

-- 2. Cột IS_DELETED (Xóa mềm) cho 8 phân hệ quản trị
ALTER TABLE purchase_orders  ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE suppliers        ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE customers        ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE brands           ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products         ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE employees        ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users            ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_purchase_orders_is_deleted  ON purchase_orders  (is_deleted);
CREATE INDEX IF NOT EXISTS idx_suppliers_is_deleted        ON suppliers        (is_deleted);
CREATE INDEX IF NOT EXISTS idx_customers_is_deleted        ON customers        (is_deleted);
CREATE INDEX IF NOT EXISTS idx_brands_is_deleted           ON brands           (is_deleted);
CREATE INDEX IF NOT EXISTS idx_products_is_deleted         ON products         (is_deleted);
CREATE INDEX IF NOT EXISTS idx_product_variants_is_deleted ON product_variants (is_deleted);
CREATE INDEX IF NOT EXISTS idx_employees_is_deleted        ON employees        (is_deleted);
CREATE INDEX IF NOT EXISTS idx_users_is_deleted            ON users            (is_deleted);
