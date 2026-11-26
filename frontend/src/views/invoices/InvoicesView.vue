<template>
  <div class="page-container invoices-workspace">
    <!-- Header -->
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">QUẢN LÝ HÓA ĐƠN – ADHA SNEAKER</h2>
        <p class="page-subtitle">
          Quản lý tập trung hóa đơn bán lẻ &amp; hóa đơn VAT phát sinh từ bán tại quầy (POS) và Online Store, phân quyền hủy/điều chỉnh và gửi hóa đơn PDF qua Email.
        </p>
      </div>
      <div class="header-actions">
        <el-button class="btn-soft" :loading="loading" @click="loadAll">
          <el-icon><Refresh /></el-icon>
          <span>Làm mới</span>
        </el-button>
        <el-button
          v-if="canExportExcel"
          class="btn-soft"
          :loading="exporting"
          @click="handleExportExcel"
        >
          <el-icon><Download /></el-icon>
          <span>Xuất Excel</span>
        </el-button>
        <el-button type="primary" class="btn-primary-dark" @click="openIssueModal">
          <el-icon><Plus /></el-icon>
          <span>Phát hành hóa đơn / VAT</span>
        </el-button>
      </div>
    </div>

    <!-- Pending Cancel Requests Banner (Admin & Manager only) -->
    <div v-if="canApproveCancel && kpi.pendingCancelCount > 0" class="pending-alert-bar">
      <div class="pending-alert-left">
        <el-icon><WarningFilled /></el-icon>
        <span>
          Hiện có <strong>{{ kpi.pendingCancelCount }}</strong> yêu cầu hủy hóa đơn từ nhân viên đang chờ Admin/Quản lý phê duyệt.
        </span>
      </div>
      <el-button size="small" type="warning" @click="filterPendingCancel">
        Xem yêu cầu chờ duyệt
      </el-button>
    </div>

    <!-- 4.1 KPI Overview Cards (Real DB Aggregation) -->
    <div class="kpi-grid" v-loading="kpiLoading">
      <div class="kpi-card">
        <div class="kpi-label">Tổng số hóa đơn</div>
        <div class="kpi-value">{{ kpi.totalInvoices }}</div>
        <div class="kpi-sub">Tất cả hóa đơn trong kỳ</div>
      </div>
      <div class="kpi-card kpi-revenue">
        <div class="kpi-label">Doanh thu hóa đơn hợp lệ</div>
        <div class="kpi-value">{{ formatCurrency(kpi.totalValidRevenue) }}</div>
        <div class="kpi-sub">Không tính hóa đơn đã hủy</div>
      </div>
      <div class="kpi-card kpi-tax">
        <div class="kpi-label">Tổng tiền thuế VAT</div>
        <div class="kpi-value">{{ formatCurrency(kpi.totalValidTax) }}</div>
        <div class="kpi-sub">Thuế GTGT hóa đơn hợp lệ</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">Hóa đơn tại quầy (POS)</div>
        <div class="kpi-value">{{ kpi.posInvoices }}</div>
        <div class="kpi-sub">Phát sinh tại cửa hàng</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">Hóa đơn Online</div>
        <div class="kpi-value">{{ kpi.onlineInvoices }}</div>
        <div class="kpi-sub">Giao hàng thành công</div>
      </div>
      <div class="kpi-card kpi-cancelled">
        <div class="kpi-label">Hóa đơn đã hủy</div>
        <div class="kpi-value">{{ kpi.cancelledInvoices }}</div>
        <div class="kpi-sub">Lưu lịch sử bất biến</div>
      </div>
    </div>

    <!-- Main Surface -->
    <div class="invoices-surface">
      <!-- 4.2 Filters & Search -->
      <div class="filters-panel">
        <div class="filter-row">
          <el-input
            v-model="filters.keyword"
            placeholder="Tìm mã HĐ, mã đơn hàng, tên KH / công ty, SĐT, Email, MST..."
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <el-date-picker
            v-model="dateRange"
            type="daterange"
            unlink-panels
            range-separator="→"
            start-placeholder="Từ ngày"
            end-placeholder="Đến ngày"
            value-format="YYYY-MM-DD"
            format="DD/MM/YYYY"
            clearable
            class="date-picker"
            @change="handleDateRangeChange"
          />

          <el-button type="primary" class="btn-primary-dark" @click="handleSearch">
            Tìm kiếm
          </el-button>
          <el-button class="btn-soft" @click="resetFilters">
            Làm mới bộ lọc
          </el-button>
        </div>

        <div class="filter-row filter-selects">
          <el-select v-model="filters.saleChannel" placeholder="Kênh bán" clearable class="filter-select" @change="handleSearch">
            <el-option label="Tất cả kênh bán" value="" />
            <el-option label="Tại quầy (POS)" value="POS" />
            <el-option label="Online" value="ONLINE" />
          </el-select>

          <el-select v-model="filters.invoiceType" placeholder="Loại hóa đơn" clearable class="filter-select" @change="handleSearch">
            <el-option label="Tất cả loại HĐ" value="" />
            <el-option label="Hóa đơn bán lẻ" value="RETAIL" />
            <el-option label="Hóa đơn VAT" value="VAT" />
          </el-select>

          <el-select v-model="filters.invoiceStatus" placeholder="Trạng thái hóa đơn" clearable class="filter-select" @change="handleSearch">
            <el-option label="Tất cả trạng thái HĐ" value="" />
            <el-option label="Đã phát hành" value="ISSUED" />
            <el-option label="Chờ duyệt hủy" value="PENDING_CANCEL" />
            <el-option label="Đã hủy" value="CANCELLED" />
            <el-option label="Đã điều chỉnh" value="ADJUSTED" />
          </el-select>

          <el-select v-model="filters.emailStatus" placeholder="Trạng thái Email" clearable class="filter-select" @change="handleSearch">
            <el-option label="Tất cả trạng thái Email" value="" />
            <el-option label="Chưa gửi" value="NOT_SENT" />
            <el-option label="Đang gửi" value="SENDING" />
            <el-option label="Thành công" value="SENT" />
            <el-option label="Thất bại" value="FAILED" />
          </el-select>
        </div>
      </div>

      <!-- 4.3 Invoice Table (14 Columns) -->
      <div class="table-wrap" v-loading="loading">
        <el-table :data="items" class="invoices-table" style="width: 100%" stripe>
          <template #empty>
            <el-empty description="Chưa có hóa đơn nào phù hợp với bộ lọc" :image-size="88" />
          </template>

          <el-table-column label="Mã hóa đơn" min-width="155" fixed="left">
            <template #default="{ row }">
              <div class="code-cell">
                <button type="button" class="code-link" @click="goDetail(row)">
                  {{ row.invoiceCode }}
                </button>
                <span v-if="row.recordKind && row.recordKind !== 'ORIGINAL'" class="kind-micro-tag">
                  {{ row.recordKind === 'REPLACEMENT' ? 'Thay thế' : 'Điều chỉnh' }}
                </span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="Mã đơn hàng" width="120">
            <template #default="{ row }">
              <router-link :to="`/admin/orders/${row.orderId}`" class="order-ref-link">
                {{ row.orderCode }}
              </router-link>
            </template>
          </el-table-column>

          <el-table-column label="Kênh bán" width="105" align="center">
            <template #default="{ row }">
              <span class="channel-pill" :class="row.saleChannel === 'ONLINE' ? 'online' : 'pos'">
                {{ row.saleChannel === 'ONLINE' ? 'Online' : 'Tại quầy' }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="Khách hàng" min-width="175">
            <template #default="{ row }">
              <div class="cust-name">{{ row.companyName || row.customerName }}</div>
              <div v-if="row.companyName" class="cust-sub">{{ row.customerName }}</div>
            </template>
          </el-table-column>

          <el-table-column label="Số điện thoại" width="120">
            <template #default="{ row }">{{ row.customerPhone || '---' }}</template>
          </el-table-column>

          <el-table-column label="Email" min-width="170">
            <template #default="{ row }">
              <span v-if="row.customerEmail" class="email-text">{{ row.customerEmail }}</span>
              <span v-else class="muted-dash">Chưa có</span>
            </template>
          </el-table-column>

          <el-table-column label="Mã số thuế" width="125">
            <template #default="{ row }">
              <code v-if="row.taxCode" class="mst-code">{{ row.taxCode }}</code>
              <span v-else class="muted-dash">---</span>
            </template>
          </el-table-column>

          <el-table-column label="Loại hóa đơn" width="110" align="center">
            <template #default="{ row }">
              <span class="type-pill" :class="row.invoiceType === 'VAT' ? 'type-vat' : 'type-retail'">
                {{ row.invoiceType === 'VAT' ? 'VAT' : 'Bán lẻ' }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="Tổng tiền" width="135" align="right">
            <template #default="{ row }">
              <span class="money-strong" :class="{ 'money-cancelled': row.invoiceStatus === 'CANCELLED' }">
                {{ formatCurrency(row.totalAmount) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="Tiền thuế" width="115" align="right">
            <template #default="{ row }">
              <span class="tax-text">{{ formatCurrency(row.taxAmount) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="Ngày lập" width="135">
            <template #default="{ row }">
              <span class="date-text">{{ formatCompactDate(row.issuedAt) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="Trạng thái HĐ" width="140" align="center">
            <template #default="{ row }">
              <span class="status-pill" :class="`st-${(row.invoiceStatus || '').toLowerCase()}`">
                {{ invoiceStatusLabel(row.invoiceStatus) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="Trạng thái Email" width="125" align="center">
            <template #default="{ row }">
              <span class="email-pill" :class="`em-${(row.emailStatus || '').toLowerCase()}`">
                {{ emailStatusLabel(row.emailStatus) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="Thao tác" width="175" fixed="right" align="center">
            <template #default="{ row }">
              <div class="action-cell">
                <button type="button" class="icon-btn" title="Xem chi tiết hóa đơn" @click="goDetail(row)">
                  <el-icon><View /></el-icon>
                </button>
                <button type="button" class="icon-btn" title="Xem trước & In hóa đơn" @click="openPrintModal(row)">
                  <el-icon><Printer /></el-icon>
                </button>
                <button
                  type="button"
                  class="icon-btn"
                  title="Gửi hóa đơn PDF qua Email"
                  @click="openEmailModal(row)"
                >
                  <el-icon><Message /></el-icon>
                </button>

                <el-dropdown trigger="click" @command="(cmd) => handleRowAction(row, cmd)">
                  <button type="button" class="icon-btn" title="Nghiệp vụ hóa đơn">
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="DETAIL">Xem chi tiết đầy đủ</el-dropdown-item>
                      <el-dropdown-item command="PRINT">Xem trước &amp; In hóa đơn</el-dropdown-item>
                      <el-dropdown-item command="SEND_EMAIL">Gửi / Gửi lại Email</el-dropdown-item>

                      <!-- Admin/Manager: Duyệt yêu cầu hủy -->
                      <el-dropdown-item
                        v-if="canApproveCancel && row.invoiceStatus === 'PENDING_CANCEL' && row.pendingCancelRequestId"
                        command="REVIEW_CANCEL"
                        divided
                        style="color: #d97706; font-weight: 700"
                      >
                        Duyệt / Từ chối yêu cầu hủy
                      </el-dropdown-item>

                      <!-- Admin/Manager: Điều chỉnh / Thay thế -->
                      <el-dropdown-item
                        v-if="canAdjust && row.invoiceStatus !== 'CANCELLED'"
                        command="ADJUST"
                        divided
                      >
                        Lập hóa đơn điều chỉnh / thay thế
                      </el-dropdown-item>

                      <!-- Admin/Manager: Hủy trực tiếp -->
                      <el-dropdown-item
                        v-if="canCancelDirect && row.invoiceStatus !== 'CANCELLED'"
                        command="CANCEL_DIRECT"
                        style="color: #dc2626"
                      >
                        Hủy hóa đơn (Bắt buộc lý do)
                      </el-dropdown-item>

                      <!-- Staff: Xin duyệt hủy -->
                      <el-dropdown-item
                        v-if="!canCancelDirect && row.invoiceStatus === 'ISSUED'"
                        command="REQUEST_CANCEL"
                        divided
                        style="color: #d97706"
                      >
                        Gửi yêu cầu duyệt hủy
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination-bar">
          <span class="pagination-summary">
            Hiển thị <strong>{{ items.length }}</strong> / <strong>{{ pagination.total }}</strong> hóa đơn
          </span>
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="handleSizeChange"
            @current-change="loadInvoices"
          />
        </div>
      </div>
    </div>

    <!-- Modal 1: Phát hành hóa đơn Bán lẻ / VAT từ đơn hàng hoàn thành -->
    <el-dialog v-model="issueVisible" title="PHÁT HÀNH HÓA ĐƠN BÁN LẺ / VAT" width="640px" destroy-on-close>
      <el-form :model="issueForm" label-position="top">
        <el-form-item label="Chọn đơn hàng đã hoàn thành (COMPLETED) *" required>
          <el-select
            v-model="issueForm.orderId"
            filterable
            placeholder="Tìm theo mã đơn (HDxxxx) hoặc tên khách hàng..."
            style="width: 100%"
            @change="handleSelectOrderForIssue"
          >
            <el-option
              v-for="o in completedOrders"
              :key="o.id"
              :label="`${o.orderCode} - ${o.customerName || 'Khách lẻ'} - ${formatCurrency(o.finalAmount)} (${o.channel || (o.employeeId ? 'POS' : 'ONLINE')})`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Loại hóa đơn">
              <el-radio-group v-model="issueForm.invoiceType">
                <el-radio-button value="RETAIL">Hóa đơn bán lẻ</el-radio-button>
                <el-radio-button value="VAT">Hóa đơn VAT</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Thuế suất GTGT áp dụng">
              <el-select v-model="issueForm.defaultTaxRate" style="width: 100%">
                <el-option label="0% (Không chịu thuế / 0%)" :value="0" />
                <el-option label="5% (VAT 5%)" :value="5" />
                <el-option label="8% (VAT 8% - Phổ biến)" :value="8" />
                <el-option label="10% (VAT 10%)" :value="10" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="issueForm.invoiceType === 'VAT'">
          <el-row :gutter="12">
            <el-col :span="14">
              <el-form-item label="Tên công ty / Đơn vị mua hàng *" required>
                <el-input v-model="issueForm.companyName" placeholder="VD: Công ty TNHH Thương mại ABC" />
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label="Mã số thuế (MST) *" required>
                <el-input v-model="issueForm.taxCode" placeholder="VD: 0312345678" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Tên khách hàng / Người nhận">
              <el-input v-model="issueForm.customerName" placeholder="Tự động lấy từ đơn hàng" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Email nhận hóa đơn (Không bắt buộc)">
              <el-input v-model="issueForm.customerEmail" placeholder="khachhang@email.com" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Địa chỉ xuất hóa đơn">
          <el-input v-model="issueForm.customerAddress" placeholder="Nhập địa chỉ công ty hoặc khách hàng..." />
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Cách tính thuế VAT">
              <el-radio-group v-model="issueForm.taxIncluded">
                <el-radio :value="true">Đã gồm trong giá bán</el-radio>
                <el-radio :value="false">Cộng thêm VAT</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Gửi Email tự động sau phát hành">
              <el-checkbox v-model="issueForm.autoSendEmail" :disabled="!issueForm.customerEmail">
                Gửi PDF qua Email nếu có Email hợp lệ
              </el-checkbox>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="submitting" @click="submitIssueInvoice">
          Phát hành hóa đơn
        </el-button>
      </template>
    </el-dialog>

    <!-- Modal 2: Gửi hóa đơn qua Email -->
    <el-dialog v-model="emailVisible" title="GỬI HÓA ĐƠN QUA EMAIL (ĐÍNH KÈM PDF)" width="480px" destroy-on-close>
      <div v-if="activeInvoice" class="dialog-summary">
        <div><strong>Mã hóa đơn:</strong> {{ activeInvoice.invoiceCode }} ({{ activeInvoice.orderCode }})</div>
        <div><strong>Khách hàng:</strong> {{ activeInvoice.companyName || activeInvoice.customerName }}</div>
        <div><strong>Tổng thanh toán:</strong> <span class="money-strong">{{ formatCurrency(activeInvoice.totalAmount) }}</span></div>
      </div>
      <el-form label-position="top" style="margin-top: 12px">
        <el-form-item label="Địa chỉ Email người nhận *" required>
          <el-input
            v-model="emailTarget"
            placeholder="Nhập email khách hàng (VD: customer@gmail.com)"
            clearable
          />
          <div class="field-hint">
            Tiêu đề: "Hóa đơn mua hàng – ADHA Sneaker" · Đính kèm file PDF hóa đơn. Lỗi gửi Email sẽ không làm ảnh hưởng đến trạng thái hóa đơn.
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emailVisible = false">Đóng</el-button>
        <el-button type="primary" :loading="submitting" @click="submitSendEmail">
          Gửi hóa đơn ngay
        </el-button>
      </template>
    </el-dialog>

    <!-- Modal 3: Lập hóa đơn điều chỉnh / thay thế (Admin & Manager) -->
    <el-dialog v-model="adjustVisible" title="LẬP HÓA ĐƠN ĐIỀU CHỈNH / THAY THẾ" width="760px" destroy-on-close>
      <div v-if="activeInvoice" class="dialog-summary">
        Hóa đơn gốc: <strong>{{ activeInvoice.invoiceCode }}</strong> &middot; Đơn hàng: <strong>{{ activeInvoice.orderCode }}</strong> &middot; Tổng gốc: <strong>{{ formatCurrency(activeInvoice.totalAmount) }}</strong>
        <div class="field-hint">Hóa đơn gốc sẽ được chuyển sang trạng thái "Đã điều chỉnh" và giữ nguyên dữ liệu lịch sử bất biến.</div>
      </div>

      <el-form :model="adjustForm" label-position="top" style="margin-top: 12px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Hình thức xử lý">
              <el-radio-group v-model="adjustForm.recordKind">
                <el-radio-button value="ADJUSTMENT">Hóa đơn điều chỉnh</el-radio-button>
                <el-radio-button value="REPLACEMENT">Hóa đơn thay thế</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Loại hóa đơn mới">
              <el-radio-group v-model="adjustForm.invoiceType">
                <el-radio-button value="RETAIL">Bán lẻ</el-radio-button>
                <el-radio-button value="VAT">VAT</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row v-if="adjustForm.invoiceType === 'VAT'" :gutter="12">
          <el-col :span="14">
            <el-form-item label="Tên công ty *">
              <el-input v-model="adjustForm.companyName" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="Mã số thuế *">
              <el-input v-model="adjustForm.taxCode" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Lý do điều chỉnh / thay thế *" required>
          <el-input
            v-model="adjustForm.reason"
            type="textarea"
            :rows="2"
            placeholder="VD: Khách hoàn trả 1 đôi giày / Cập nhật lại thông tin MST & thuế suất VAT..."
          />
        </el-form-item>

        <div class="adjust-items-title">Chi tiết dòng hàng &amp; thuế suất từng sản phẩm:</div>
        <el-table :data="adjustForm.items" border size="small" style="width: 100%; margin-bottom: 12px">
          <el-table-column label="Sản phẩm" min-width="180">
            <template #default="{ row }">
              <strong>{{ row.productName }}</strong>
              <div style="font-size: 11px; color: #64748b">{{ row.variantLabel }}</div>
            </template>
          </el-table-column>
          <el-table-column label="SL" width="88" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" :max="999" size="small" controls-position="right" style="width: 68px" />
            </template>
          </el-table-column>
          <el-table-column label="Đơn giá" width="125" align="right">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0" :step="50000" size="small" :controls="false" style="width: 105px" />
            </template>
          </el-table-column>
          <el-table-column label="Giảm giá" width="110" align="right">
            <template #default="{ row }">
              <el-input-number v-model="row.discountAmount" :min="0" :step="10000" size="small" :controls="false" style="width: 90px" />
            </template>
          </el-table-column>
          <el-table-column label="VAT (%)" width="96" align="center">
            <template #default="{ row }">
              <el-select v-model="row.taxRate" size="small" style="width: 76px">
                <el-option label="0%" :value="0" />
                <el-option label="5%" :value="5" />
                <el-option label="8%" :value="8" />
                <el-option label="10%" :value="10" />
              </el-select>
            </template>
          </el-table-column>
        </el-table>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Phí vận chuyển">
              <el-input-number v-model="adjustForm.shippingFee" :min="0" :step="10000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Cách tính thuế">
              <el-radio-group v-model="adjustForm.taxIncluded">
                <el-radio :value="true">Giá đã gồm VAT</el-radio>
                <el-radio :value="false">Giá chưa gồm VAT</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="adjust-preview-bar">
          <span>Trước thuế: <strong>{{ formatCurrency(adjustPreview.subtotal) }}</strong></span>
          <span>VAT: <strong>{{ formatCurrency(adjustPreview.tax) }}</strong></span>
          <span>Tổng mới: <strong class="money-strong">{{ formatCurrency(adjustPreview.total) }}</strong></span>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="adjustVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAdjustInvoice">
          Phát hành hóa đơn {{ adjustForm.recordKind === 'REPLACEMENT' ? 'thay thế' : 'điều chỉnh' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Print Preview Modal -->
    <InvoicePrintPreviewModal v-model="printVisible" :invoice="printInvoice" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  Plus,
  Search,
  View,
  Printer,
  Message,
  MoreFilled,
  WarningFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { invoiceService } from '@/services/invoiceService'
import { orderService } from '@/services/orderService'
import { formatCurrency } from '@/utils/formatters'
import { useDebouncedWatch } from '@/composables/useDebounce'
import InvoicePrintPreviewModal from '@/components/InvoicePrintPreviewModal.vue'

const router = useRouter()
const authStore = useAuthStore()

// RBAC Flags (checked on both UI and Backend API)
const canCancelDirect = computed(() => authStore.isManager) // ADMIN or MANAGER
const canApproveCancel = computed(() => authStore.isManager)
const canAdjust = computed(() => authStore.isManager)
const canExportExcel = computed(() => authStore.isManager)

const loading = ref(false)
const kpiLoading = ref(false)
const exporting = ref(false)
const submitting = ref(false)
const items = ref([])
const completedOrders = ref([])

const kpi = reactive({
  totalInvoices: 0,
  totalValidRevenue: 0,
  totalValidTax: 0,
  posInvoices: 0,
  onlineInvoices: 0,
  cancelledInvoices: 0,
  pendingCancelCount: 0
})

const filters = reactive({
  keyword: '',
  saleChannel: '',
  invoiceType: '',
  invoiceStatus: '',
  emailStatus: '',
  fromDate: null,
  toDate: null
})
const dateRange = ref(null)
const pagination = reactive({ page: 1, size: 20, total: 0 })

// Modals
const printVisible = ref(false)
const printInvoice = ref(null)

const issueVisible = ref(false)
const issueForm = reactive({
  orderId: null,
  invoiceType: 'VAT',
  defaultTaxRate: 8,
  companyName: '',
  taxCode: '',
  customerName: '',
  customerEmail: '',
  customerAddress: '',
  taxIncluded: true,
  autoSendEmail: false
})

const emailVisible = ref(false)
const activeInvoice = ref(null)
const emailTarget = ref('')

const adjustVisible = ref(false)
const adjustForm = reactive({
  recordKind: 'ADJUSTMENT',
  invoiceType: 'RETAIL',
  reason: '',
  companyName: '',
  taxCode: '',
  shippingFee: 0,
  taxIncluded: true,
  items: []
})

const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const adjustPreview = computed(() => {
  let subtotal = 0
  let tax = 0
  let totalDiscount = 0
  for (const it of adjustForm.items) {
    const qty = Number(it.quantity || 0)
    if (qty <= 0) continue
    const gross = qty * Number(it.unitPrice || 0)
    const disc = Math.max(0, Number(it.discountAmount || 0))
    const afterDisc = Math.max(0, gross - disc)
    const rate = Number(it.taxRate || 0)
    if (adjustForm.taxIncluded) {
      const preTax = Math.round(afterDisc / (1 + rate / 100))
      const t = afterDisc - preTax
      subtotal += preTax + disc
      tax += t
    } else {
      const t = Math.round((afterDisc * rate) / 100)
      subtotal += afterDisc + disc
      tax += t
    }
    totalDiscount += disc
  }
  const ship = Math.max(0, Number(adjustForm.shippingFee || 0))
  const total = Math.max(0, subtotal - totalDiscount + ship + tax)
  return { subtotal, tax, total }
})

function formatCompactDate(val) {
  if (!val) return '---'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const MM = String(d.getMonth() + 1).padStart(2, '0')
  return `${hh}:${mm} ${dd}/${MM}/${d.getFullYear()}`
}

function invoiceStatusLabel(s) {
  return (
    {
      ISSUED: 'Đã phát hành',
      PENDING_CANCEL: 'Chờ duyệt hủy',
      CANCELLED: 'Đã hủy',
      ADJUSTED: 'Đã điều chỉnh'
    }[s] || s
  )
}

function emailStatusLabel(s) {
  return (
    {
      NOT_SENT: 'Chưa gửi',
      SENDING: 'Đang gửi',
      SENT: 'Thành công',
      FAILED: 'Thất bại'
    }[s] || s
  )
}

function buildFilterParams() {
  return {
    keyword: filters.keyword?.trim() || undefined,
    saleChannel: filters.saleChannel || undefined,
    invoiceType: filters.invoiceType || undefined,
    invoiceStatus: filters.invoiceStatus || undefined,
    emailStatus: filters.emailStatus || undefined,
    fromDate: filters.fromDate || undefined,
    toDate: filters.toDate || undefined
  }
}

async function loadKpi() {
  kpiLoading.value = true
  try {
    const res = await invoiceService.getKpi({
      fromDate: filters.fromDate || undefined,
      toDate: filters.toDate || undefined
    })
    Object.assign(kpi, res.data || {})
  } finally {
    kpiLoading.value = false
  }
}

async function loadInvoices() {
  loading.value = true
  try {
    const res = await invoiceService.getInvoices({
      ...buildFilterParams(),
      page: pagination.page - 1,
      size: pagination.size
    })
    items.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadKpi(), loadInvoices()])
}

function handleSearch() {
  pagination.page = 1
  loadAll()
}

function handleDateRangeChange(val) {
  if (Array.isArray(val) && val.length === 2) {
    filters.fromDate = val[0]
    filters.toDate = val[1]
  } else {
    filters.fromDate = null
    filters.toDate = null
  }
  handleSearch()
}

function handleSizeChange() {
  pagination.page = 1
  loadInvoices()
}

function resetFilters() {
  filters.keyword = ''
  filters.saleChannel = ''
  filters.invoiceType = ''
  filters.invoiceStatus = ''
  filters.emailStatus = ''
  filters.fromDate = null
  filters.toDate = null
  dateRange.value = null
  pagination.page = 1
  loadAll()
}

function filterPendingCancel() {
  filters.invoiceStatus = 'PENDING_CANCEL'
  pagination.page = 1
  loadInvoices()
}

useDebouncedWatch(
  () => filters.keyword,
  () => {
    pagination.page = 1
    loadInvoices()
  },
  350
)

function goDetail(row) {
  router.push(`/admin/invoices/${row.id}`)
}

async function openPrintModal(row) {
  const res = await invoiceService.getInvoiceById(row.id)
  printInvoice.value = res.data
  printVisible.value = true
}

function openEmailModal(row) {
  activeInvoice.value = row
  emailTarget.value = row.customerEmail || ''
  emailVisible.value = true
}

async function submitSendEmail() {
  const target = (emailTarget.value || '').trim()
  if (!target) {
    ElMessage.warning('Vui lòng nhập địa chỉ Email nhận hóa đơn')
    return
  }
  if (!EMAIL_REGEX.test(target)) {
    ElMessage.warning('Địa chỉ Email không đúng định dạng, vui lòng kiểm tra lại!')
    return
  }
  submitting.value = true
  try {
    await invoiceService.sendEmail(activeInvoice.value.id, { recipientEmail: target })
    ElMessage.success(`Đã tiếp nhận gửi hóa đơn ${activeInvoice.value.invoiceCode} tới ${target}`)
    emailVisible.value = false
    setTimeout(() => loadInvoices(), 800)
  } finally {
    submitting.value = false
  }
}

async function openIssueModal() {
  issueForm.orderId = null
  issueForm.invoiceType = 'VAT'
  issueForm.defaultTaxRate = 8
  issueForm.companyName = ''
  issueForm.taxCode = ''
  issueForm.customerName = ''
  issueForm.customerEmail = ''
  issueForm.customerAddress = ''
  issueForm.taxIncluded = true
  issueForm.autoSendEmail = false
  issueVisible.value = true

  const res = await orderService.getOrders({ status: 'COMPLETED', page: 0, size: 100 })
  completedOrders.value = res.data?.content || []
}

function handleSelectOrderForIssue(orderId) {
  const ord = completedOrders.value.find((o) => o.id === orderId)
  if (ord) {
    issueForm.customerName = ord.customerName || ''
    issueForm.customerEmail = ord.customerEmail || ''
    issueForm.customerAddress = ord.customerAddress || ''
  }
}

async function submitIssueInvoice() {
  if (!issueForm.orderId) {
    ElMessage.warning('Vui lòng chọn đơn hàng hoàn thành')
    return
  }
  if (issueForm.invoiceType === 'VAT' && (!issueForm.companyName.trim() || !issueForm.taxCode.trim())) {
    ElMessage.warning('Hóa đơn VAT bắt buộc nhập Tên công ty và Mã số thuế (MST)')
    return
  }
  if (issueForm.customerEmail && !EMAIL_REGEX.test(issueForm.customerEmail.trim())) {
    ElMessage.warning('Email nhận hóa đơn không đúng định dạng')
    return
  }
  submitting.value = true
  try {
    const res = await invoiceService.issueInvoice({ ...issueForm })
    ElMessage.success(`Đã phát hành hóa đơn ${res.data?.invoiceCode || ''} thành công`)
    issueVisible.value = false
    await loadAll()
  } finally {
    submitting.value = false
  }
}

async function handleRowAction(row, cmd) {
  if (cmd === 'DETAIL') return goDetail(row)
  if (cmd === 'PRINT') return openPrintModal(row)
  if (cmd === 'SEND_EMAIL') return openEmailModal(row)
  if (cmd === 'CANCEL_DIRECT') return handleCancelDirect(row)
  if (cmd === 'REQUEST_CANCEL') return handleRequestCancel(row)
  if (cmd === 'REVIEW_CANCEL') return handleReviewCancel(row)
  if (cmd === 'ADJUST') return openAdjustModal(row)
}

function handleCancelDirect(row) {
  ElMessageBox.prompt(
    `Nhập lý do hủy hóa đơn ${row.invoiceCode} (dữ liệu hóa đơn gốc vẫn được lưu lịch sử bất biến):`,
    'Hủy hóa đơn',
    {
      confirmButtonText: 'Xác nhận hủy',
      cancelButtonText: 'Đóng',
      confirmButtonClass: 'el-button--danger',
      inputPlaceholder: 'Bắt buộc nhập lý do hủy...',
      inputValidator: (v) => (!v || !v.trim() ? 'Bắt buộc nhập lý do hủy hóa đơn' : true)
    }
  )
    .then(async ({ value }) => {
      await invoiceService.cancelInvoiceDirect(row.id, { reason: value.trim() })
      ElMessage.success(`Đã hủy hóa đơn ${row.invoiceCode}`)
      await loadAll()
    })
    .catch(() => {})
}

function handleRequestCancel(row) {
  ElMessageBox.prompt(
    `Nhân viên bán hàng cần gửi yêu cầu để Admin / Quản lý duyệt hủy hóa đơn ${row.invoiceCode}:`,
    'Gửi yêu cầu duyệt hủy hóa đơn',
    {
      confirmButtonText: 'Gửi yêu cầu duyệt',
      cancelButtonText: 'Đóng',
      inputPlaceholder: 'Nhập lý do đề nghị hủy hóa đơn...',
      inputValidator: (v) => (!v || !v.trim() ? 'Vui lòng nhập lý do hủy' : true)
    }
  )
    .then(async ({ value }) => {
      await invoiceService.requestCancelInvoice(row.id, { reason: value.trim() })
      ElMessage.success(`Đã gửi yêu cầu duyệt hủy cho hóa đơn ${row.invoiceCode}`)
      await loadAll()
    })
    .catch(() => {})
}

function handleReviewCancel(row) {
  ElMessageBox.confirm(
    `Nhân viên ${row.pendingCancelRequestedBy || ''} yêu cầu hủy hóa đơn ${row.invoiceCode} với lý do: "${row.pendingCancelReason || ''}". Bạn muốn Phê duyệt hay Từ chối?`,
    'Phê duyệt yêu cầu hủy hóa đơn',
    {
      confirmButtonText: 'Phê duyệt hủy',
      cancelButtonText: 'Từ chối yêu cầu',
      distinguishCancelAndClose: true,
      type: 'warning'
    }
  )
    .then(async () => {
      await invoiceService.approveCancelRequest(row.pendingCancelRequestId, { reviewNote: 'Đã duyệt hủy' })
      ElMessage.success(`Đã phê duyệt hủy hóa đơn ${row.invoiceCode}`)
      await loadAll()
    })
    .catch(async (action) => {
      if (action === 'cancel') {
        await invoiceService.rejectCancelRequest(row.pendingCancelRequestId, { reviewNote: 'Không đủ điều kiện hủy' })
        ElMessage.info(`Đã từ chối yêu cầu hủy hóa đơn ${row.invoiceCode}`)
        await loadAll()
      }
    })
}

async function openAdjustModal(row) {
  const res = await invoiceService.getInvoiceById(row.id)
  const full = res.data
  activeInvoice.value = full
  adjustForm.recordKind = 'ADJUSTMENT'
  adjustForm.invoiceType = full.invoiceType || 'RETAIL'
  adjustForm.reason = ''
  adjustForm.companyName = full.companyName || ''
  adjustForm.taxCode = full.taxCode || ''
  adjustForm.shippingFee = Number(full.shippingFee || 0)
  adjustForm.taxIncluded = true
  adjustForm.items = (full.details || []).map((d) => ({
    productId: d.productId,
    productVariantId: d.productVariantId,
    productCode: d.productCode,
    productName: d.productName,
    sku: d.sku,
    variantLabel: d.variantLabel,
    quantity: Number(d.quantity || 1),
    unitPrice: Number(d.unitPrice || 0),
    discountAmount: Number(d.discountAmount || 0),
    taxRate: Number(d.taxRate || 0)
  }))
  adjustVisible.value = true
}

async function submitAdjustInvoice() {
  if (!adjustForm.reason.trim()) {
    ElMessage.warning('Bắt buộc nhập lý do điều chỉnh / thay thế')
    return
  }
  if (adjustForm.invoiceType === 'VAT' && (!adjustForm.companyName.trim() || !adjustForm.taxCode.trim())) {
    ElMessage.warning('Hóa đơn VAT bắt buộc nhập Tên công ty và MST')
    return
  }
  submitting.value = true
  try {
    const res = await invoiceService.adjustInvoice(activeInvoice.value.id, { ...adjustForm })
    ElMessage.success(`Đã lập hóa đơn mới ${res.data?.invoiceCode || ''} liên kết với ${activeInvoice.value.invoiceCode}`)
    adjustVisible.value = false
    await loadAll()
  } finally {
    submitting.value = false
  }
}

async function handleExportExcel() {
  exporting.value = true
  try {
    const res = await invoiceService.exportExcel(buildFilterParams())
    const blob = new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `ADHA_BangKeHoaDon_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('Đã xuất báo cáo bảng kê hóa đơn Excel thành công')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped lang="scss">
.invoices-workspace {
  gap: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pending-alert-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 18px;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 6px;
  color: #92400e;
  font-size: 13px;

  .pending-alert-left {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

/* KPI Grid — Balanced 3x2 Executive Grid (never wraps currency!) */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.kpi-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;

  &:hover {
    border-color: #a3a3a3;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  }

  .kpi-label {
    font-size: 11px;
    font-weight: 800;
    color: #737373;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .kpi-value {
    font-size: 23px;
    font-weight: 900;
    color: #000000;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }

  .kpi-sub {
    font-size: 12px;
    color: #737373;
  }

  &.kpi-revenue {
    background: #0a0a0a;
    border-color: #0a0a0a;

    .kpi-label { color: #a3a3a3; }
    .kpi-value { color: #ffffff; }
    .kpi-sub { color: #a3a3a3; }
  }

  &.kpi-tax {
    border-left: 3px solid #2563eb;
    .kpi-value { color: #1d4ed8; }
  }

  &.kpi-cancelled {
    border-left: 3px solid #ef4444;
    .kpi-value { color: #dc2626; }
  }
}

/* Surface */
.invoices-surface {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  overflow: hidden;
}

.filters-panel {
  padding: 16px 22px;
  background: #fafafa;
  border-bottom: 1px solid #e5e5e5;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-selects {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.search-input {
  flex: 1;
  min-width: 260px;
}

.date-picker {
  width: 260px !important;
}

.filter-select {
  width: 100%;
}

/* Table */
.invoices-table {
  :deep(th.el-table__cell) {
    background: #f8fafc !important;
    color: #475569;
    font-size: 11.5px;
    font-weight: 700;
    text-transform: uppercase;
  }
}

.code-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
}

.code-link {
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #0f172a;
  padding: 2px 7px;
  border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;

  &:hover {
    background: #0f172a;
    border-color: #0f172a;
    color: #ffffff;
  }
}

.kind-micro-tag {
  font-size: 10.5px;
  font-weight: 700;
  color: #1d4ed8;
}

.order-ref-link {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12.5px;
  font-weight: 700;
  color: #2563eb;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.channel-pill {
  display: inline-flex;
  padding: 2px 7px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;

  &.pos {
    background: #0f172a;
    color: #ffffff;
  }

  &.online {
    background: #fff7ed;
    color: #c2410c;
    border: 1px solid #fdba74;
  }
}

.cust-name {
  font-weight: 700;
  font-size: 13px;
  color: #0f172a;
}

.cust-sub {
  font-size: 11.5px;
  color: #64748b;
}

.email-text {
  font-size: 12.5px;
  color: #334155;
  word-break: break-all;
}

.muted-dash {
  color: #94a3b8;
  font-size: 12px;
}

.mst-code {
  background: #f1f5f9;
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 11.5px;
  color: #0f172a;
}

.type-pill {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 800;

  &.type-vat {
    background: #eff6ff;
    color: #1d4ed8;
    border: 1px solid #93c5fd;
  }

  &.type-retail {
    background: #f1f5f9;
    color: #334155;
    border: 1px solid #cbd5e1;
  }
}

.money-strong {
  font-weight: 800;
  color: #dc2626;
  font-variant-numeric: tabular-nums;
}

.money-cancelled {
  color: #94a3b8;
  text-decoration: line-through;
}

.tax-text {
  font-size: 12.5px;
  color: #1d4ed8;
  font-weight: 600;
}

.date-text {
  font-size: 12px;
  color: #475569;
  font-variant-numeric: tabular-nums;
}

.status-pill {
  display: inline-flex;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 700;
  white-space: nowrap;

  &.st-issued {
    background: #ecfdf5;
    color: #047857;
    border: 1px solid #6ee7b7;
  }

  &.st-pending_cancel {
    background: #fffbeb;
    color: #b45309;
    border: 1px solid #fcd34d;
  }

  &.st-cancelled {
    background: #fef2f2;
    color: #b91c1c;
    border: 1px solid #fca5a5;
  }

  &.st-adjusted {
    background: #eff6ff;
    color: #1d4ed8;
    border: 1px solid #93c5fd;
  }
}

.email-pill {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;

  &.em-not_sent {
    background: #f1f5f9;
    color: #64748b;
  }

  &.em-sending {
    background: #eff6ff;
    color: #1d4ed8;
  }

  &.em-sent {
    background: #ecfdf5;
    color: #047857;
  }

  &.em-failed {
    background: #fef2f2;
    color: #dc2626;
  }
}

.action-cell {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.icon-btn {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #334155;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:hover {
    border-color: #0f172a;
    color: #0f172a;
    background: #f8fafc;
  }
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-top: 1px solid #e2e8f0;
  flex-wrap: wrap;
  gap: 10px;

  .pagination-summary {
    font-size: 13px;
    color: #64748b;
  }
}

.dialog-summary {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.5;
}

.field-hint {
  font-size: 11.5px;
  color: #64748b;
  margin-top: 4px;
  line-height: 1.4;
}

.adjust-items-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 6px;
}

.adjust-preview-bar {
  display: flex;
  justify-content: flex-end;
  gap: 20px;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13.5px;
}

@media (max-width: 1100px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .filter-selects {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .kpi-grid,
  .filter-selects {
    grid-template-columns: 1fr;
  }
  .date-picker {
    width: 100% !important;
  }
}
</style>
