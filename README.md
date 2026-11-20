# HỆ THỐNG QUẢN LÝ CỬA HÀNG GIÀY SNEAKER (SNEAKERHUB PRO)

Phần mềm quản lý nội bộ chuyên nghiệp dành cho **Chủ cửa hàng (ADMIN)**, **Quản lý cửa hàng (MANAGER)** và **Nhân viên bán hàng (STAFF)**, được xây dựng bằng **Java 17 + Spring Boot 3.x**, **Vue.js 3 (Composition API + Script Setup + Vite + Pinia + Element Plus)** và **Oracle Database 19c+**.

> **Phạm vi hệ thống**:
> - Chỉ phục vụ quản lý nội bộ cửa hàng bán giày Sneaker (bán hàng tại quầy POS, đơn hàng, sản phẩm, biến thể Size/Màu/SKU, nhập hàng từ nhà cung cấp, khách hàng, nhân viên, báo cáo thống kê).
> - Không xây dựng website thương mại điện tử cho khách hàng.
> - Không xây dựng phân hệ quản lý tồn kho độc lập (chỉ kiểm tra số lượng sẵn có `AVAILABLE_QUANTITY` tại thời điểm bán hàng và cộng khi xác nhận phiếu nhập).
> - Không xây dựng phân hệ quản lý khuyến mãi / mã giảm giá.

---

## 1. Kiến trúc & Công nghệ Sử dụng

### Backend (`/backend`)
- **Java 17** & **Spring Boot 3.2.5**
- **Spring Web (RESTful API)**, **Spring Data JPA / Hibernate** (`@EntityGraph` chống N+1 query)
- **Spring Security 6 + JWT Authentication (HS256)**
- **Bean Validation (`jakarta.validation`)** & **GlobalExceptionHandler**
- **Oracle JDBC Driver (`ojdbc11`)**
- **Swagger / OpenAPI 3 (`springdoc-openapi`)**: Truy cập tại `http://localhost:8080/swagger-ui.html`
- **Apache POI (`poi-ooxml`)**: Xuất báo cáo thống kê ra file Excel (`.xlsx`)
- **JUnit 5 & Mockito**: Unit Test cho tầng nghiệp vụ (`OrderServiceTest`, `PurchaseServiceTest`)

### Frontend (`/frontend`)
- **Vue.js 3** (Composition API + `<script setup>`)
- **Vite 5**, **Vue Router 4** (Route Guard & Phân quyền theo Role)
- **Pinia** (State Management), **Axios** (Interceptors tự động gắn JWT & xử lý lỗi)
- **Element Plus** + **SCSS** (Thiết kế Admin Dashboard hiện đại với tông màu Đen `#0F172A`, Trắng, Xám và Cam nhấn `#F97316`)

### Database (`/database`)
- **Oracle Database 19c / 21c / 23ai / 26ai**
- **19 Bảng chuẩn hóa** kèm **19 Sequence**, **Primary Key**, **Foreign Key**, **Unique Constraint**, **Check Constraint**, **Index** và **Audit Columns** (`CREATED_AT`, `UPDATED_AT`, `CREATED_BY`, `UPDATED_BY`).
- Xem chi tiết sơ đồ ERD tại: [`docs/ERD.md`](./docs/ERD.md)

---

## 2. Tài khoản Đăng nhập Mẫu

Tất cả tài khoản mẫu đều được khởi tạo sẵn trong database và tự động đồng bộ mật khẩu khi khởi động Backend:

| Vai trò | Tên đăng nhập (`username`) | Mật khẩu (`password`) | Quyền hạn chính |
|---|---|---|---|
| **ADMIN** (Chủ cửa hàng / Quản trị) | `admin` | `123456` | Toàn quyền hệ thống, quản lý tài khoản & phân quyền, nhân sự, sản phẩm, nhập hàng, POS, báo cáo |
| **MANAGER** (Quản lý cửa hàng) | `manager` | `123456` | Quản lý danh mục, thương hiệu, sản phẩm, biến thể SKU, nhà cung cấp, phiếu nhập hàng, nhân viên, đơn hàng, báo cáo |
| **STAFF** (Nhân viên bán hàng) | `staff` | `123456` | Bán hàng tại quầy (POS), quản lý đơn hàng, quản lý khách hàng, xem sản phẩm/biến thể và tổng quan |
| **STAFF** (Nhân viên tư vấn) | `staff2` | `123456` | Nhân viên bán hàng tại quầy |
| **LOCKED** (Tài khoản bị khóa) | `locked_user` | `123456` | Dùng để kiểm thử chặn đăng nhập tài khoản bị khóa |

---

## 3. Cấu trúc Thư mục Dự án

```text
btl/
├── database/
│   ├── 00_create_schema.sql          # Tạo user/schema SNEAKER_STORE trên Oracle
│   ├── 01_sequences.sql              # 19 Sequences cho 19 bảng
│   ├── 02_tables_and_constraints.sql # 19 Bảng + PK, FK, Unique, Check Constraints
│   ├── 03_indexes.sql                # Các Index tối ưu truy vấn
│   ├── 04_sample_data.sql            # Dữ liệu mẫu thực tế đầy đủ cho 19 bảng
│   └── init_all.sql                  # Script chạy tổng hợp theo thứ tự phụ thuộc
├── docs/
│   └── ERD.md                        # Sơ đồ thực thể liên kết (Mermaid ERD) & mô tả bảng
├── backend/                          # Dự án Java 17 Spring Boot 3.x
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/sneakerstore/
│       │   ├── config/               # SecurityConfig, OpenApiConfig, DataInitializer
│       │   ├── security/             # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
│       │   ├── common/               # ApiResponse, PageResponse, GlobalExceptionHandler, AppConstants
│       │   ├── auth/                 # Đăng nhập, Đăng xuất, Đổi mật khẩu
│       │   ├── user/                 # Quản lý Users & Roles
│       │   ├── employee/             # Quản lý Nhân viên & Liên kết User
│       │   ├── customer/             # Quản lý Khách hàng
│       │   ├── category/             # Quản lý Danh mục giày
│       │   ├── brand/                # Quản lý Thương hiệu Sneaker
│       │   ├── product/              # Quản lý Sản phẩm & Hình ảnh
│       │   ├── variant/              # Quản lý Biến thể SKU, Size (36-45), Màu sắc (HEX)
│       │   ├── supplier/             # Quản lý Nhà cung cấp
│       │   ├── purchase/             # Quản lý Phiếu nhập hàng (DRAFT -> CONFIRMED / CANCELLED)
│       │   ├── sales/                # Bán hàng tại quầy POS (/api/sales)
│       │   ├── order/                # Quản lý Đơn hàng & Lịch sử trạng thái (/api/orders)
│       │   ├── payment/              # Quản lý Thanh toán (CASH / BANK_TRANSFER)
│       │   └── report/               # Dashboard, Báo cáo thống kê & Xuất Excel (.xlsx)
│       └── test/java/com/sneakerstore/ # JUnit 5 & Mockito Unit Tests
├── frontend/                         # Dự án Vue.js 3 + Vite + Pinia + Element Plus
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── layouts/AdminLayout.vue
│       ├── components/InvoicePrintModal.vue
│       ├── router/index.js
│       ├── stores/auth.js
│       ├── services/api.js
│       └── views/                    # Đầy đủ 13 phân hệ màn hình
├── PROJECT_PROGRESS.md               # Bảng theo dõi tiến độ & trạng thái hoàn thành
└── README.md                         # Hướng dẫn cài đặt và vận hành hệ thống
```

---

## 4. Hướng dẫn Khởi tạo Database Oracle & Chạy Hệ thống

### Bước 1: Khởi tạo Oracle Database
Mặc định hệ thống kết nối tới Oracle Database tại:
- **JDBC URL**: `jdbc:oracle:thin:@//localhost:1521/FREEPDB1`
- **Username**: `SNEAKER_STORE`
- **Password**: `SneakerStore123`

Nếu sử dụng container Docker Oracle (`oracle26ai` / `oracle-free`), chạy lệnh sau để khởi tạo toàn bộ Schema, 19 Bảng và Dữ liệu mẫu:
```bash
docker exec -i -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle26ai sqlplus -s / as sysdba < database/00_create_schema.sql

cat database/01_sequences.sql \
    database/02_tables_and_constraints.sql \
    database/03_indexes.sql \
    database/04_sample_data.sql \
    | docker exec -i -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle26ai sqlplus -s SNEAKER_STORE/SneakerStore123@//localhost:1521/FREEPDB1
```
*(Nếu dùng SQL Developer / DBeaver, mở và chạy lần lượt từ `00_create_schema.sql` đến `04_sample_data.sql`).*

### Bước 2: Chạy Backend Spring Boot (Port `8080`)
```bash
cd backend
mvn spring-boot:run
```
- **API Base URL**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

### Bước 3: Chạy Frontend Vue.js 3 (Port `5173`)
```bash
cd frontend
npm install
npm run dev
```
- Mở trình duyệt tại: **`http://localhost:5173`**
- Đăng nhập bằng `admin` / `123456`, `manager` / `123456`, hoặc `staff` / `123456`.

