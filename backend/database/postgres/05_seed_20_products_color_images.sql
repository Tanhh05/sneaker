-- =============================================================================
-- 05_seed_20_products_color_images.sql (PostgreSQL 16)
-- 1) Add COLOR_ID to PRODUCT_IMAGES for instant per-color image switching
-- 2) Enrich SP001..SP010 with 4 verified color-matched sneaker images each
-- 3) Seed 20 new authentic sneaker products (SP011..SP030) across Nike, Adidas,
--    Puma, Jordan, New Balance, Converse, Vans, Asics — each with 4 color-matched
--    images + 16 variants (4 colors x 4 sizes)
-- =============================================================================

ALTER TABLE PRODUCT_IMAGES ADD COLUMN IF NOT EXISTS COLOR_ID BIGINT;

DO $$
DECLARE
    -- Verified HTTP 200 color-accurate sneaker photo pools (Unsplash)
    img_w1   CONSTANT TEXT := 'https://images.unsplash.com/photo-1587563871167-1ee9c731aefb?auto=format&fit=crop&w=800&q=80';
    img_w2   CONSTANT TEXT := 'https://images.unsplash.com/photo-1536085439-e1532af5457c?auto=format&fit=crop&w=800&q=80';
    img_w3   CONSTANT TEXT := 'https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=800&q=80';
    img_w4   CONSTANT TEXT := 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80';
    img_w5   CONSTANT TEXT := 'https://images.unsplash.com/photo-1689417714892-9357ae68cdcb?auto=format&fit=crop&w=800&q=80';
    img_w6   CONSTANT TEXT := 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=800&q=80';

    img_b1   CONSTANT TEXT := 'https://images.unsplash.com/photo-1518002171953-a080ee817e1f?auto=format&fit=crop&w=800&q=80';
    img_b2   CONSTANT TEXT := 'https://images.unsplash.com/photo-1581923597046-427a5d83f932?auto=format&fit=crop&w=800&q=80';
    img_b3   CONSTANT TEXT := 'https://images.unsplash.com/photo-1535398274614-061b9455a9f5?auto=format&fit=crop&w=800&q=80';
    img_b4   CONSTANT TEXT := 'https://images.unsplash.com/photo-1565963513169-108e0c01a074?auto=format&fit=crop&w=800&q=80';
    img_b5   CONSTANT TEXT := 'https://images.unsplash.com/photo-1607522370275-f14206abe5d3?auto=format&fit=crop&w=800&q=80';
    img_b6   CONSTANT TEXT := 'https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&w=800&q=80';

    img_r1   CONSTANT TEXT := 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80';
    img_r2   CONSTANT TEXT := 'https://images.unsplash.com/photo-1597045566677-8cf032ed6634?auto=format&fit=crop&w=800&q=80';
    img_r3   CONSTANT TEXT := 'https://images.unsplash.com/photo-1570348271518-e4b3b63aaea0?auto=format&fit=crop&w=800&q=80';
    img_r4   CONSTANT TEXT := 'https://images.unsplash.com/photo-1513188732907-5f732b831ca8?auto=format&fit=crop&w=800&q=80';
    img_r5   CONSTANT TEXT := 'https://images.unsplash.com/photo-1630497970947-a11d1c809fd1?auto=format&fit=crop&w=800&q=80';
    img_r6   CONSTANT TEXT := 'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=800&q=80';

    img_gry1 CONSTANT TEXT := 'https://images.unsplash.com/photo-1536181211993-cf4b2c100475?auto=format&fit=crop&w=800&q=80';
    img_gry2 CONSTANT TEXT := 'https://images.unsplash.com/photo-1576229086948-cfba99afcca5?auto=format&fit=crop&w=800&q=80';
    img_gry3 CONSTANT TEXT := 'https://images.unsplash.com/photo-1623789195459-d40e163cd9f9?auto=format&fit=crop&w=800&q=80';

    img_nvy1 CONSTANT TEXT := 'https://images.unsplash.com/photo-1643427806881-e8bc8e05b796?auto=format&fit=crop&w=800&q=80';
    img_nvy2 CONSTANT TEXT := 'https://images.unsplash.com/photo-1504281623087-1a6dd8f827c2?auto=format&fit=crop&w=800&q=80';
    img_nvy3 CONSTANT TEXT := 'https://images.unsplash.com/photo-1560769629-975ec94e6a86?auto=format&fit=crop&w=800&q=80';
    img_nvy4 CONSTANT TEXT := 'https://images.unsplash.com/photo-1644001992668-3b9ed080533a?auto=format&fit=crop&w=800&q=80';

    img_crm1 CONSTANT TEXT := 'https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?auto=format&fit=crop&w=800&q=80';
    img_crm2 CONSTANT TEXT := 'https://images.unsplash.com/photo-1520256862855-398228c41684?auto=format&fit=crop&w=800&q=80';

    img_org1 CONSTANT TEXT := 'https://images.unsplash.com/photo-1581088293192-ab8c02530c0b?auto=format&fit=crop&w=800&q=80';
    img_org2 CONSTANT TEXT := 'https://images.unsplash.com/photo-1562613521-6b5293e5b0ea?auto=format&fit=crop&w=800&q=80';
    img_org3 CONSTANT TEXT := 'https://images.unsplash.com/photo-1551107696-a4b0c5a0d9a2?auto=format&fit=crop&w=800&q=80';

    img_grn1 CONSTANT TEXT := 'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&w=800&q=80';
    img_grn2 CONSTANT TEXT := 'https://images.unsplash.com/photo-1617814121568-9b184eaabf08?auto=format&fit=crop&w=800&q=80';
    img_grn3 CONSTANT TEXT := 'https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&w=800&q=80';
    img_grn4 CONSTANT TEXT := 'https://images.unsplash.com/photo-1606296149205-69c6c18fa499?auto=format&fit=crop&w=800&q=80';
    img_grn5 CONSTANT TEXT := 'https://images.unsplash.com/photo-1699198489130-2e02f3726612?auto=format&fit=crop&w=800&q=80';
    img_grn6 CONSTANT TEXT := 'https://images.unsplash.com/photo-1704949841973-9db544ac35ec?auto=format&fit=crop&w=800&q=80';

    v_pid BIGINT;
    v_col_id BIGINT;
    v_sz_id BIGINT;
    v_col_code TEXT;
    v_sz_val TEXT;
    v_sku TEXT;
    v_idx INT;
    v_item RECORD;
    v_cols BIGINT[];
    v_urls TEXT[];
BEGIN
    -- =========================================================================
    -- PART A: Enrich existing SP001..SP010 with 4 color-linked images each
    -- =========================================================================
    -- SP001 Nike Air Force 1 '07 Low (W=1, B=2, R=3, GRN=8)
    SELECT id INTO v_pid FROM products WHERE product_code = 'SP001';
    IF v_pid IS NOT NULL THEN
        DELETE FROM product_images WHERE product_id = v_pid;
        INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order) VALUES
            (v_pid, 1, img_w6, 1, 1),
            (v_pid, 2, img_b6, 0, 2),
            (v_pid, 3, img_r1, 0, 3),
            (v_pid, 8, img_grn4, 0, 4);
        INSERT INTO product_variants (product_id, size_id, color_id, sku, import_price, sale_price, available_quantity, status) VALUES
            (v_pid, 5, 3, 'AF1-40-R', 1800000, 2950000, 14, 'ACTIVE'),
            (v_pid, 6, 3, 'AF1-41-R', 1800000, 2950000, 12, 'ACTIVE'),
            (v_pid, 6, 8, 'AF1-41-GRN', 1800000, 2950000, 10, 'ACTIVE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- SP002 Air Jordan 1 Retro High OG (R=3, B=2, NVY=5, GRN=8)
    SELECT id INTO v_pid FROM products WHERE product_code = 'SP002';
    IF v_pid IS NOT NULL THEN
        DELETE FROM product_images WHERE product_id = v_pid;
        INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order) VALUES
            (v_pid, 3, img_r2, 1, 1),
            (v_pid, 2, img_b6, 0, 2),
            (v_pid, 5, img_nvy1, 0, 3),
            (v_pid, 8, img_grn5, 0, 4);
        INSERT INTO product_variants (product_id, size_id, color_id, sku, import_price, sale_price, available_quantity, status) VALUES
            (v_pid, 6, 5, 'AJ1-41-NVY', 3200000, 5200000, 11, 'ACTIVE'),
            (v_pid, 7, 8, 'AJ1-42-GRN', 3200000, 5200000, 9, 'ACTIVE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- SP003 Adidas Ultraboost Light (W=1, B=2, GRY=4, R=3)
    SELECT id INTO v_pid FROM products WHERE product_code = 'SP003';
    IF v_pid IS NOT NULL THEN
        DELETE FROM product_images WHERE product_id = v_pid;
        INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order) VALUES
            (v_pid, 1, img_w1, 1, 1),
            (v_pid, 2, img_b1, 0, 2),
            (v_pid, 4, img_gry1, 0, 3),
            (v_pid, 3, img_r1, 0, 4);
        INSERT INTO product_variants (product_id, size_id, color_id, sku, import_price, sale_price, available_quantity, status) VALUES
            (v_pid, 6, 3, 'UB-41-R', 2500000, 4200000, 10, 'ACTIVE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- SP004 Adidas Samba OG Classic (W=1, B=2, R=3, GRN=8)
    SELECT id INTO v_pid FROM products WHERE product_code = 'SP004';
    IF v_pid IS NOT NULL THEN
        DELETE FROM product_images WHERE product_id = v_pid;
        INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order) VALUES
            (v_pid, 2, img_b1, 1, 1),
            (v_pid, 1, img_w2, 0, 2),
            (v_pid, 3, img_r6, 0, 3),
            (v_pid, 8, img_grn1, 0, 4);
        INSERT INTO product_variants (product_id, size_id, color_id, sku, import_price, sale_price, available_quantity, status) VALUES
            (v_pid, 5, 3, 'SAMBA-40-R', 1750000, 2890000, 13, 'ACTIVE'),
            (v_pid, 6, 8, 'SAMBA-41-GRN', 1750000, 2890000, 11, 'ACTIVE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- SP005 New Balance 550 Vintage (CRM=6, GRN=8, GRY=4, R=3)
    SELECT id INTO v_pid FROM products WHERE product_code = 'SP005';
    IF v_pid IS NOT NULL THEN
        DELETE FROM product_images WHERE product_id = v_pid;
        INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order) VALUES
            (v_pid, 8, img_grn3, 1, 1),
            (v_pid, 6, img_crm1, 0, 2),
            (v_pid, 4, img_gry1, 0, 3),
            (v_pid, 3, img_r4, 0, 4);
        INSERT INTO product_variants (product_id, size_id, color_id, sku, import_price, sale_price, available_quantity, status) VALUES
            (v_pid, 6, 3, 'NB550-41-R', 2100000, 3450000, 12, 'ACTIVE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- SP006 Vans Old Skool Classic Skate (B=2, NVY=5, R=3, W=1)
    SELECT id INTO v_pid FROM products WHERE product_code = 'SP006';
    IF v_pid IS NOT NULL THEN
        DELETE FROM product_images WHERE product_id = v_pid;
        INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order) VALUES
            (v_pid, 3, img_r6, 1, 1),
            (v_pid, 2, img_b3, 0, 2),
            (v_pid, 5, img_nvy2, 0, 3),
            (v_pid, 1, img_w5, 0, 4);
        INSERT INTO product_variants (product_id, size_id, color_id, sku, import_price, sale_price, available_quantity, status) VALUES
            (v_pid, 5, 3, 'VANS-40-R', 1100000, 1850000, 18, 'ACTIVE'),
            (v_pid, 5, 1, 'VANS-40-W', 1100000, 1850000, 15, 'ACTIVE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- SP007 Converse Chuck 70 High Top (B=2, CRM=6, R=3, W=1)
    SELECT id INTO v_pid FROM products WHERE product_code = 'SP007';
    IF v_pid IS NOT NULL THEN
        DELETE FROM product_images WHERE product_id = v_pid;
        INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order) VALUES
            (v_pid, 2, img_b5, 1, 1),
            (v_pid, 6, img_crm2, 0, 2),
            (v_pid, 3, img_r3, 0, 3),
            (v_pid, 1, img_w4, 0, 4);
        INSERT INTO product_variants (product_id, size_id, color_id, sku, import_price, sale_price, available_quantity, status) VALUES
            (v_pid, 4, 3, 'CV70-39-R', 1250000, 2100000, 14, 'ACTIVE'),
            (v_pid, 5, 1, 'CV70-40-W', 1250000, 2100000, 16, 'ACTIVE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- SP008 Asics Gel-Kayano 30 Running (NVY=5, ORG=7, GRY=4, R=3)
    SELECT id INTO v_pid FROM products WHERE product_code = 'SP008';
    IF v_pid IS NOT NULL THEN
        DELETE FROM product_images WHERE product_id = v_pid;
        INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order) VALUES
            (v_pid, 5, img_nvy3, 1, 1),
            (v_pid, 7, img_org2, 0, 2),
            (v_pid, 4, img_gry3, 0, 3),
            (v_pid, 3, img_r1, 0, 4);
        INSERT INTO product_variants (product_id, size_id, color_id, sku, import_price, sale_price, available_quantity, status) VALUES
            (v_pid, 6, 4, 'KAYANO30-41-GRY', 2400000, 3950000, 10, 'ACTIVE'),
            (v_pid, 6, 3, 'KAYANO30-41-R', 2400000, 3950000, 9, 'ACTIVE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- SP009 Nike Metcon 9 Training (R=3, B=2, ORG=7, GRN=8)
    SELECT id INTO v_pid FROM products WHERE product_code = 'SP009';
    IF v_pid IS NOT NULL THEN
        DELETE FROM product_images WHERE product_id = v_pid;
        INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order) VALUES
            (v_pid, 3, img_r1, 1, 1),
            (v_pid, 2, img_b6, 0, 2),
            (v_pid, 7, img_org1, 0, 3),
            (v_pid, 8, img_grn1, 0, 4);
        INSERT INTO product_variants (product_id, size_id, color_id, sku, import_price, sale_price, available_quantity, status) VALUES
            (v_pid, 7, 3, 'METCON9-42-R', 2150000, 3550000, 12, 'ACTIVE'),
            (v_pid, 7, 8, 'METCON9-42-GRN', 2150000, 3550000, 11, 'ACTIVE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- SP010 Puma Suede Classic XXI (W=1, B=2, R=3, NVY=5)
    SELECT id INTO v_pid FROM products WHERE product_code = 'SP010';
    IF v_pid IS NOT NULL THEN
        DELETE FROM product_images WHERE product_id = v_pid;
        INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order) VALUES
            (v_pid, 1, img_w3, 1, 1),
            (v_pid, 2, img_b2, 0, 2),
            (v_pid, 3, img_r5, 0, 3),
            (v_pid, 5, img_nvy2, 0, 4);
        INSERT INTO product_variants (product_id, size_id, color_id, sku, import_price, sale_price, available_quantity, status) VALUES
            (v_pid, 5, 1, 'SUEDE-40-W', 1350000, 2250000, 16, 'ACTIVE'),
            (v_pid, 6, 5, 'SUEDE-41-NVY', 1350000, 2250000, 14, 'ACTIVE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- =========================================================================
    -- PART B: 20 NEW SNEAKER PRODUCTS (SP011..SP030) - 4 COLOR-MATCHED IMAGES EACH
    -- Brands: 1=Nike, 2=Adidas, 3=Puma, 4=Converse, 5=New Balance, 6=Vans, 7=Jordan, 8=Asics
    -- Categories: 1=Running, 2=Basketball, 3=Lifestyle, 4=Training, 5=Skateboarding
    -- Colors: 1=W, 2=B, 3=R, 4=GRY, 5=NVY, 6=CRM, 7=ORG, 8=GRN
    -- Sizes: 3=38, 4=39, 5=40, 6=41, 7=42, 8=43
    -- =========================================================================
    FOR v_item IN
        SELECT * FROM (VALUES
            ('SP011', 'Nike Dunk Low Retro Panda', 1::BIGINT, 3::BIGINT,
                'Da thật (Full-grain Leather) & Đế cao su khâu Cupsole',
                'Biểu tượng đường phố đình đám của Nike với phối màu tương phản dễ phối mọi phong cách từ streetwear tới casual.',
                1950000::NUMERIC, 3190000::NUMERIC,
                2::BIGINT, img_b6,  1::BIGINT, img_w4,  3::BIGINT, img_r4,  8::BIGINT, img_grn4,
                ARRAY[4,5,6,7]::BIGINT[]),

            ('SP012', 'Nike Air Max 90 Essential', 1::BIGINT, 3::BIGINT,
                'Lưới thoáng khí, da tổng hợp & Bộ đệm khí Max Air lộ thiên',
                'Thiết kế huyền thoại năm 1990 với cửa sổ đệm Air đặc trưng ở gót giày, mang lại cảm giác êm ái suốt cả ngày.',
                2100000::NUMERIC, 3490000::NUMERIC,
                1::BIGINT, img_w6,  3::BIGINT, img_r1,  2::BIGINT, img_b6,  4::BIGINT, img_gry1,
                ARRAY[5,6,7,8]::BIGINT[]),

            ('SP013', 'Nike Air Zoom Pegasus 41', 1::BIGINT, 1::BIGINT,
                'Vải dệt Engineered Mesh & Đế bọt ReactX kết hợp 2 túi khí Air Zoom',
                'Mẫu giày chạy bộ đa dụng bán chạy nhất của Nike với hoàn trả năng lượng tăng 13%, phù hợp chạy hằng ngày và bán marathon.',
                2250000::NUMERIC, 3690000::NUMERIC,
                3::BIGINT, img_r1,  5::BIGINT, img_nvy4,  1::BIGINT, img_w1,  7::BIGINT, img_org1,
                ARRAY[5,6,7,8]::BIGINT[]),

            ('SP014', 'Nike SB Dunk Low Pro Skate', 1::BIGINT, 5::BIGINT,
                'Da lộn dày chống mài mòn, lưỡi gà độn phồng & Đệm Zoom Air',
                'Phiên bản trượt ván chuyên nghiệp với lưỡi gà dày êm ái, bảo vệ cổ chân và độ bám ván cực nhạy.',
                2150000::NUMERIC, 3550000::NUMERIC,
                8::BIGINT, img_grn2,  2::BIGINT, img_b4,  3::BIGINT, img_r5,  6::BIGINT, img_crm1,
                ARRAY[4,5,6,7]::BIGINT[]),

            ('SP015', 'Nike ZoomX Vaporfly Next% 3', 1::BIGINT, 1::BIGINT,
                'Sợi Flyknit siêu nhẹ, đế bọt ZoomX & Tấm carbon Flyplate toàn phần',
                'Siêu giày đua marathon chuyên dụng giúp bứt tốc tối đa với trọng lượng siêu nhẹ dưới 200g.',
                3900000::NUMERIC, 6290000::NUMERIC,
                7::BIGINT, img_org3,  1::BIGINT, img_w1,  3::BIGINT, img_r1,  2::BIGINT, img_b6,
                ARRAY[5,6,7,8]::BIGINT[]),

            ('SP016', 'Adidas Gazelle Indoor Terrace', 2::BIGINT, 3::BIGINT,
                'Da lộn cao cấp (Premium Suede) & Đế Gum trong suốt đặc trưng',
                'Dòng giày Terrace đang làm mưa làm gió với phom dáng thon gọn, màu sắc nổi bật và đế cao su gum cổ điển.',
                1850000::NUMERIC, 2990000::NUMERIC,
                3::BIGINT, img_r6,  2::BIGINT, img_b1,  1::BIGINT, img_w2,  8::BIGINT, img_grn1,
                ARRAY[3,4,5,6]::BIGINT[]),

            ('SP017', 'Adidas Superstar Classic Shell Toe', 2::BIGINT, 3::BIGINT,
                'Da thật mềm mịn, mũi vỏ sò cao su & 3 sọc răng cưa',
                'Huyền thoại hơn 50 năm tuổi của nhà 3 sọc, dễ dàng kết hợp cùng quần jeans, kaki hay outfit thể thao.',
                1600000::NUMERIC, 2650000::NUMERIC,
                1::BIGINT, img_w2,  2::BIGINT, img_b1,  3::BIGINT, img_r4,  6::BIGINT, img_crm1,
                ARRAY[4,5,6,7]::BIGINT[]),

            ('SP018', 'Adidas Stan Smith Lux', 2::BIGINT, 3::BIGINT,
                'Da bò nguyên tấm siêu mềm & Lót da cao cấp',
                'Thiết kế tối giản vượt thời gian với 3 hàng lỗ thoáng khí tinh tế, nâng cấp chất liệu da sang trọng.',
                1800000::NUMERIC, 2950000::NUMERIC,
                1::BIGINT, img_w5,  8::BIGINT, img_grn4,  2::BIGINT, img_b1,  6::BIGINT, img_crm2,
                ARRAY[3,4,5,6]::BIGINT[]),

            ('SP019', 'Adidas Adizero Boston 12', 2::BIGINT, 1::BIGINT,
                'Lưới siêu thoáng, đệm Lightstrike Pro & Thanh trợ lực ENERGYRODS 2.0',
                'Đôi giày chạy tốc độ và luyện tập cự ly dài mang cảm giác bật nảy mạnh mẽ của dòng Adizero.',
                2450000::NUMERIC, 3990000::NUMERIC,
                5::BIGINT, img_nvy4,  1::BIGINT, img_w1,  7::BIGINT, img_org2,  2::BIGINT, img_b1,
                ARRAY[5,6,7,8]::BIGINT[]),

            ('SP020', 'Adidas Forum Low CL', 2::BIGINT, 2::BIGINT,
                'Da thật nhiều lớp & Đế cao su cổ điển thập niên 80',
                'Tái hiện tinh thần bóng rổ cổ điển 1984 với dáng cổ thấp năng động, đệm lót dày dặn nâng đỡ bàn chân.',
                1750000::NUMERIC, 2890000::NUMERIC,
                1::BIGINT, img_w2,  5::BIGINT, img_nvy1,  3::BIGINT, img_r3,  2::BIGINT, img_b1,
                ARRAY[4,5,6,7]::BIGINT[]),

            ('SP021', 'Puma Palermo Lth Terrace', 3::BIGINT, 3::BIGINT,
                'Da thật kết hợp mũi chữ T da lộn & Đế Gum',
                'Lấy cảm hứng từ thành phố Palermo nước Ý những năm 80, phom gọn thanh lịch và cực kỳ tôn dáng chân.',
                1550000::NUMERIC, 2550000::NUMERIC,
                3::BIGINT, img_r5,  1::BIGINT, img_w3,  2::BIGINT, img_b2,  8::BIGINT, img_grn3,
                ARRAY[3,4,5,6]::BIGINT[]),

            ('SP022', 'Puma RS-X Efekt Turbo', 3::BIGINT, 1::BIGINT,
                'Vải lưới, da lộn tổng hợp & Công nghệ đệm Running System (RS)',
                'Phong cách chunky sneaker hiện đại với các đường cắt táo bạo, tăng chiều cao tự nhiên và siêu êm chân.',
                1950000::NUMERIC, 3250000::NUMERIC,
                2::BIGINT, img_b2,  4::BIGINT, img_gry2,  7::BIGINT, img_org2,  1::BIGINT, img_w3,
                ARRAY[5,6,7,8]::BIGINT[]),

            ('SP023', 'Puma Speedcat OG Motorsport', 3::BIGINT, 3::BIGINT,
                'Da lộn nguyên tấm mềm mịn & Đế cao su thấp lấy cảm hứng F1',
                'Siêu phẩm low-profile từ đường đua công thức 1 đang phủ sóng giới thời trang với phom ôm chân cá tính.',
                1700000::NUMERIC, 2790000::NUMERIC,
                3::BIGINT, img_r1,  2::BIGINT, img_b2,  5::BIGINT, img_nvy2,  6::BIGINT, img_crm2,
                ARRAY[3,4,5,6]::BIGINT[]),

            ('SP024', 'Air Jordan 1 Low SE Craft', 7::BIGINT, 2::BIGINT,
                'Da lộn kết hợp da hạt & Đệm Air-Sole giấu gót',
                'Phiên bản cổ thấp tinh tế của dòng Jordan 1 huyền thoại, dễ mang hàng ngày với chi tiết chỉ khâu thủ công.',
                2400000::NUMERIC, 3890000::NUMERIC,
                3::BIGINT, img_r3,  2::BIGINT, img_b6,  5::BIGINT, img_nvy1,  8::BIGINT, img_grn6,
                ARRAY[4,5,6,7]::BIGINT[]),

            ('SP025', 'Air Jordan 4 Retro Bred Reimagined', 7::BIGINT, 2::BIGINT,
                'Da thật cao cấp (Tumbled Leather), lưới thoáng & Đệm Air lộ thiên',
                'Một trong những phối màu được săn đón nhất lịch sử Sneakerhead với phần cánh nhựa hỗ trợ buộc dây đặc trưng.',
                3600000::NUMERIC, 5890000::NUMERIC,
                2::BIGINT, img_b6,  3::BIGINT, img_r2,  1::BIGINT, img_w4,  4::BIGINT, img_gry1,
                ARRAY[5,6,7,8]::BIGINT[]),

            ('SP026', 'New Balance 9060 Sea Salt', 5::BIGINT, 3::BIGINT,
                'Da lộn Pigskin cao cấp, lưới thoáng & Đế kép ABZORB + SBS',
                'Thiết kế tương lai lấy cảm hứng từ kỷ nguyên Y2K với phần đế điêu khắc độc đáo và độ êm chuẩn New Balance.',
                2650000::NUMERIC, 4290000::NUMERIC,
                6::BIGINT, img_crm1,  4::BIGINT, img_gry1,  2::BIGINT, img_b4,  8::BIGINT, img_grn3,
                ARRAY[4,5,6,7]::BIGINT[]),

            ('SP027', 'New Balance 2002R Protection Pack', 5::BIGINT, 1::BIGINT,
                'Da lộn cắt rách nghệ thuật, lưới mesh & Đệm N-ergy siêu nhẹ',
                'Phiên bản phá cách được giới mộ điệu yêu thích nhờ các lớp da lộn xếp tầng tinh xảo và cảm giác mang êm như mây.',
                2550000::NUMERIC, 4150000::NUMERIC,
                4::BIGINT, img_gry2,  5::BIGINT, img_nvy3,  6::BIGINT, img_crm2,  2::BIGINT, img_b3,
                ARRAY[5,6,7,8]::BIGINT[]),

            ('SP028', 'Converse Run Star Hike Platform', 4::BIGINT, 3::BIGINT,
                'Vải Canvas hữu cơ & Đế răng cưa Platform tôn chiều cao 5cm',
                'Biến tấu đầy phá cách của Chuck Taylor với đế răng cưa hai tông màu cực chất, hack chiều cao ấn tượng.',
                1650000::NUMERIC, 2750000::NUMERIC,
                2::BIGINT, img_b5,  1::BIGINT, img_w4,  6::BIGINT, img_crm1,  3::BIGINT, img_r4,
                ARRAY[3,4,5,6]::BIGINT[]),

            ('SP029', 'Vans Sk8-Hi Pro Classic', 6::BIGINT, 5::BIGINT,
                'Da lộn kết hợp Canvas 10oz & Đệm lót PopCush chống sốc',
                'Mẫu giày trượt ván cổ cao kinh điển bảo vệ mắt cá chân tối ưu, mũi giày gia cố Duracap siêu bền.',
                1350000::NUMERIC, 2250000::NUMERIC,
                2::BIGINT, img_b3,  3::BIGINT, img_r6,  5::BIGINT, img_nvy2,  1::BIGINT, img_w5,
                ARRAY[4,5,6,7]::BIGINT[]),

            ('SP030', 'Asics Gel-1130 Retro Runner', 8::BIGINT, 1::BIGINT,
                'Lưới thoáng khí ánh kim, đệm GEL gót & Thanh chống xoắn TRUSSTIC',
                'Mẫu retro runner thập niên 2000 cực hot với trọng lượng nhẹ, ôm chân thoáng mát và dễ phối đồ đường phố.',
                1900000::NUMERIC, 3150000::NUMERIC,
                4::BIGINT, img_gry3,  5::BIGINT, img_nvy4,  1::BIGINT, img_w5,  7::BIGINT, img_org3,
                ARRAY[4,5,6,7]::BIGINT[])
        ) AS t(p_code, p_name, b_id, c_id, mat, descr, imp, sal, c1, u1, c2, u2, c3, u3, c4, u4, szs)
    LOOP
        SELECT id INTO v_pid FROM products WHERE product_code = v_item.p_code;

        IF v_pid IS NULL THEN
            INSERT INTO products (
                product_code, product_name, brand_id, category_id,
                material, description, thumbnail_url, import_price, sale_price, status
            ) VALUES (
                v_item.p_code, v_item.p_name, v_item.b_id, v_item.c_id,
                v_item.mat, v_item.descr, v_item.u1, v_item.imp, v_item.sal, 'ACTIVE'
            ) RETURNING id INTO v_pid;
        END IF;

        -- Insert 4 color-linked images for this product
        v_cols := ARRAY[v_item.c1, v_item.c2, v_item.c3, v_item.c4];
        v_urls := ARRAY[v_item.u1, v_item.u2, v_item.u3, v_item.u4];

        DELETE FROM product_images WHERE product_id = v_pid;
        FOR v_idx IN 1..4 LOOP
            INSERT INTO product_images (product_id, color_id, image_url, is_primary, display_order, status)
            VALUES (v_pid, v_cols[v_idx], v_urls[v_idx], CASE WHEN v_idx = 1 THEN 1 ELSE 0 END, v_idx, 'ACTIVE');
        END LOOP;

        -- Insert 4 colors x 4 sizes = 16 variants for this product
        FOREACH v_col_id IN ARRAY v_cols LOOP
            SELECT color_code INTO v_col_code FROM colors WHERE id = v_col_id;
            FOREACH v_sz_id IN ARRAY v_item.szs LOOP
                SELECT size_value INTO v_sz_val FROM sizes WHERE id = v_sz_id;
                v_sku := v_item.p_code || '-' || v_sz_val || '-' || v_col_code;
                INSERT INTO product_variants (
                    product_id, size_id, color_id, sku,
                    import_price, sale_price, available_quantity, status
                ) VALUES (
                    v_pid, v_sz_id, v_col_id, v_sku,
                    v_item.imp, v_item.sal, 10 + ((v_pid + v_sz_id + v_col_id) % 15), 'ACTIVE'
                ) ON CONFLICT DO NOTHING;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;
