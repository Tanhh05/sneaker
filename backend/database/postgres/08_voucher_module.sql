-- ============================================================================
-- 08_voucher_module.sql
-- ADHA Sneaker — Omni-channel Voucher, Dynamic Trigger Rules, Personal Wallet
-- & Usage Ledger Migration (PostgreSQL)
-- ============================================================================

-- 1. VOUCHERS (Master Data)
CREATE SEQUENCE IF NOT EXISTS seq_vouchers START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS vouchers (
    id BIGINT PRIMARY KEY DEFAULT nextval('seq_vouchers'),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    discount_value NUMERIC(18, 2) NOT NULL CHECK (discount_value > 0),
    max_discount_amount NUMERIC(18, 2),
    min_order_amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_quantity INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    per_customer_limit INTEGER NOT NULL DEFAULT 1,
    channel VARCHAR(20) NOT NULL DEFAULT 'ALL' CHECK (channel IN ('ALL', 'POS', 'ONLINE')),
    voucher_type VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' CHECK (voucher_type IN ('PUBLIC', 'PRIVATE')),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED')),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_vouchers_code ON vouchers (code);
CREATE INDEX IF NOT EXISTS idx_vouchers_status_type ON vouchers (status, voucher_type, channel) WHERE is_deleted = FALSE;

-- 2. VOUCHER TRIGGER RULES (Dynamic Auto-Award Configuration)
CREATE SEQUENCE IF NOT EXISTS seq_voucher_rules START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS voucher_rules (
    id BIGINT PRIMARY KEY DEFAULT nextval('seq_voucher_rules'),
    rule_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    trigger_type VARCHAR(30) NOT NULL CHECK (trigger_type IN ('ORDER_COUNT', 'TOTAL_SPENT', 'SINGLE_ORDER_AMOUNT')),
    threshold_value NUMERIC(18, 2) NOT NULL CHECK (threshold_value > 0),
    voucher_id BIGINT NOT NULL REFERENCES vouchers(id),
    max_awards_per_customer INTEGER NOT NULL DEFAULT 1,
    validity_days INTEGER NOT NULL DEFAULT 30,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_voucher_rules_active ON voucher_rules (status, trigger_type) WHERE is_deleted = FALSE;

-- 3. CUSTOMER VOUCHERS (Personal Voucher Wallet)
CREATE SEQUENCE IF NOT EXISTS seq_customer_vouchers START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS customer_vouchers (
    id BIGINT PRIMARY KEY DEFAULT nextval('seq_customer_vouchers'),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    voucher_id BIGINT NOT NULL REFERENCES vouchers(id),
    rule_id BIGINT REFERENCES voucher_rules(id),
    source_order_id BIGINT REFERENCES sales_orders(id),
    personal_code VARCHAR(60) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'USED', 'EXPIRED', 'REVOKED')),
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    used_order_id BIGINT REFERENCES sales_orders(id),
    award_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    expiry_reminded_3d_date DATE,
    expiry_reminded_1d_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_customer_vouchers_cust ON customer_vouchers (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_customer_vouchers_exp ON customer_vouchers (status, expires_at);

-- 4. VOUCHER USAGES (Order Application Audit Log & Refund Tracking)
CREATE SEQUENCE IF NOT EXISTS seq_voucher_usages START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS voucher_usages (
    id BIGINT PRIMARY KEY DEFAULT nextval('seq_voucher_usages'),
    voucher_id BIGINT NOT NULL REFERENCES vouchers(id),
    customer_voucher_id BIGINT REFERENCES customer_vouchers(id),
    sales_order_id BIGINT NOT NULL REFERENCES sales_orders(id),
    customer_id BIGINT REFERENCES customers(id),
    voucher_code VARCHAR(60) NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('POS', 'ONLINE')),
    order_subtotal NUMERIC(18, 2) NOT NULL,
    discount_amount NUMERIC(18, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED' CHECK (status IN ('APPLIED', 'REFUNDED')),
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refunded_at TIMESTAMP,
    note VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_voucher_usages_voucher ON voucher_usages (voucher_id, customer_id, status);
CREATE INDEX IF NOT EXISTS idx_voucher_usages_order ON voucher_usages (sales_order_id);

-- 5. EXTEND SALES_ORDERS WITH VOUCHER & DISCOUNT COLUMNS
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS voucher_id BIGINT REFERENCES vouchers(id);
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS voucher_code VARCHAR(60);
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(18, 2) NOT NULL DEFAULT 0;
ALTER TABLE sales_orders ADD COLUMN IF NOT EXISTS shipping_fee NUMERIC(18, 2) NOT NULL DEFAULT 0;

-- 6. SEED STARTER VOUCHERS & DYNAMIC TRIGGER RULES (Idempotent)
INSERT INTO vouchers (code, name, description, discount_type, discount_value, max_discount_amount, min_order_amount, total_quantity, used_count, per_customer_limit, channel, voucher_type, start_date, end_date, status, created_by, updated_by)
VALUES
    ('SNEAKER10', 'Giảm 10% Toàn Hệ Thống', 'Giảm 10% tối đa 150.000đ cho đơn từ 500.000đ (POS & Online)', 'PERCENT', 10, 150000, 500000, 500, 0, 2, 'ALL', 'PUBLIC', NOW() - INTERVAL '1 day', NOW() + INTERVAL '365 days', 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('ONLINE100K', 'Ưu Đãi Đặt Hàng Online 100K', 'Giảm trực tiếp 100.000đ cho đơn đặt Online từ 1.200.000đ', 'FIXED', 100000, NULL, 1200000, 300, 0, 1, 'ONLINE', 'PUBLIC', NOW() - INTERVAL '1 day', NOW() + INTERVAL '365 days', 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('POS50K', 'Ưu Đãi Tại Quầy 50K', 'Giảm trực tiếp 50.000đ khi mua sắm tại cửa hàng POS từ 500.000đ', 'FIXED', 50000, NULL, 500000, 300, 0, 2, 'POS', 'PUBLIC', NOW() - INTERVAL '1 day', NOW() + INTERVAL '365 days', 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('LOYAL3', 'Quà Tri Ân Mốc 3 Đơn Hàng', 'Voucher thưởng cá nhân giảm 10% tối đa 200.000đ dành riêng cho khách thân thiết', 'PERCENT', 10, 200000, 500000, NULL, 0, 1, 'ALL', 'PRIVATE', NOW() - INTERVAL '1 day', NOW() + INTERVAL '730 days', 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('VIP200K', 'Voucher VIP Chi Tiêu 5 Triệu', 'Voucher thưởng VIP giảm trực tiếp 200.000đ cho đơn từ 1.000.000đ', 'FIXED', 200000, NULL, 1000000, NULL, 0, 1, 'ALL', 'PRIVATE', NOW() - INTERVAL '1 day', NOW() + INTERVAL '730 days', 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('NEXT10', 'Thưởng Đơn Giá Trị Cao 10%', 'Tặng voucher giảm 10% tối đa 300.000đ cho lần mua tiếp theo', 'PERCENT', 10, 300000, 800000, NULL, 0, 3, 'ALL', 'PRIVATE', NOW() - INTERVAL '1 day', NOW() + INTERVAL '730 days', 'ACTIVE', 'SYSTEM', 'SYSTEM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO voucher_rules (rule_name, description, trigger_type, threshold_value, voucher_id, max_awards_per_customer, validity_days, status, created_by, updated_by)
SELECT 'Tri ân khách hoàn thành mốc 3 đơn', 'Tự động tặng voucher LOYAL3 khi khách hàng tích lũy đủ 3 đơn hàng hoàn thành', 'ORDER_COUNT', 3, v.id, 1, 30, 'ACTIVE', 'SYSTEM', 'SYSTEM'
FROM vouchers v WHERE v.code = 'LOYAL3'
AND NOT EXISTS (SELECT 1 FROM voucher_rules r WHERE r.rule_name = 'Tri ân khách hoàn thành mốc 3 đơn');

INSERT INTO voucher_rules (rule_name, description, trigger_type, threshold_value, voucher_id, max_awards_per_customer, validity_days, status, created_by, updated_by)
SELECT 'Khách VIP chi tiêu tích lũy từ 5.000.000đ', 'Tự động tặng voucher VIP200K khi tổng chi tiêu hoàn thành vượt 5.000.000đ', 'TOTAL_SPENT', 5000000, v.id, 1, 45, 'ACTIVE', 'SYSTEM', 'SYSTEM'
FROM vouchers v WHERE v.code = 'VIP200K'
AND NOT EXISTS (SELECT 1 FROM voucher_rules r WHERE r.rule_name = 'Khách VIP chi tiêu tích lũy từ 5.000.000đ');

INSERT INTO voucher_rules (rule_name, description, trigger_type, threshold_value, voucher_id, max_awards_per_customer, validity_days, status, created_by, updated_by)
SELECT 'Thưởng đơn hàng trên 2.000.000đ', 'Tặng voucher giảm 10% cho lần mua tiếp theo khi đơn hoàn thành đạt từ 2.000.000đ', 'SINGLE_ORDER_AMOUNT', 2000000, v.id, 3, 30, 'ACTIVE', 'SYSTEM', 'SYSTEM'
FROM vouchers v WHERE v.code = 'NEXT10'
AND NOT EXISTS (SELECT 1 FROM voucher_rules r WHERE r.rule_name = 'Thưởng đơn hàng trên 2.000.000đ');
