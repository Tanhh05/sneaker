<template>
  <div class="page-container orders-workspace">
    <!-- A. Header & Global Actions -->
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">QUẢN LÝ ĐƠN HÀNG</h2>
        <p class="page-subtitle">
          Điều phối đơn hàng đa kênh (Tại quầy POS &amp; Online Store), đối soát thanh toán SePay và xử lý giao vận 1-click.
        </p>
      </div>
      <div class="header-actions">
        <el-button class="btn-soft btn-refresh" :disabled="loading" @click="handleManualRefresh">
          <el-icon :class="{ 'is-spinning': loading }"><Refresh /></el-icon>
          <span>Làm mới</span>
          <span v-if="lastUpdatedText" class="refresh-time">{{ lastUpdatedText }}</span>
        </el-button>
        <el-button class="btn-soft" :loading="exporting" @click="handleExportExcel">
          <el-icon><Download /></el-icon>
          <span>Xuất danh sách Excel</span>
        </el-button>
        <el-button type="primary" class="btn-pos-cta" @click="$router.push('/pos')">
          <el-icon><Plus /></el-icon>
          <span>Tạo đơn tại quầy POS</span>
        </el-button>
      </div>
    </div>

    <!-- Main Surface Card (Tabs + Filters + Batch Bar + Table + Pagination) -->
    <div class="orders-surface">
      <!-- B. Order Status Tabs -->
      <div class="status-tabs-wrap">
        <el-tabs v-model="activeStatusTab" class="order-status-tabs" @tab-change="handleTabChange">
          <el-tab-pane v-for="tab in statusTabs" :key="tab.key" :name="tab.key">
            <template #label>
              <span class="tab-label-inner">
                <span>{{ tab.label }}</span>
                <span
                  class="tab-count-badge"
                  :class="[
                    `badge-${tab.badgeTone}`,
                    { 'is-zero': (statusCounts[tab.key] || 0) === 0 }
                  ]"
                >
                  {{ statusCounts[tab.key] || 0 }}
                </span>
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- C. Secondary Filters & Toolbar -->
      <div class="filters-toolbar">
        <div class="filter-row filter-row-segments">
          <!-- Payment Method Segmented Control -->
          <div class="filter-group">
            <span class="filter-label">Thanh toán:</span>
            <el-radio-group
              v-model="filters.paymentMethod"
              size="small"
              class="segmented-radio-group"
              @change="handleFilterChange"
            >
              <el-radio-button value="">Tất cả PT</el-radio-button>
              <el-radio-button value="CASH">Tiền mặt (CASH)</el-radio-button>
              <el-radio-button value="BANK_TRANSFER">Chuyển khoản</el-radio-button>
              <el-radio-button value="COD">COD (Thu hộ)</el-radio-button>
            </el-radio-group>
          </div>

          <!-- Sales Channel Segmented Control -->
          <div class="filter-group">
            <span class="filter-label">Kênh bán:</span>
            <el-radio-group
              v-model="filters.channel"
              size="small"
              class="segmented-radio-group"
              @change="handleFilterChange"
            >
              <el-radio-button value="">Tất cả kênh</el-radio-button>
              <el-radio-button value="POS">Tại quầy (POS)</el-radio-button>
              <el-radio-button value="ONLINE">Online Store</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="filter-row filter-row-inputs">
          <!-- Multi-purpose Search Box -->
          <div class="search-box-wrap">
            <el-input
              v-model="filters.keyword"
              placeholder="Tìm mã đơn (HDxxxx), tên KH, SĐT người nhận, mã vận đơn..."
              clearable
              class="search-input"
              @keyup.enter="handleImmediateSearch"
              @clear="handleImmediateSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <!-- DateRangePicker with Shortcuts -->
          <div class="date-range-wrap">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              unlink-panels
              range-separator="→"
              start-placeholder="Từ ngày"
              end-placeholder="Đến ngày"
              value-format="YYYY-MM-DD"
              format="DD/MM/YYYY"
              :shortcuts="dateShortcuts"
              clearable
              class="date-range-picker"
              @change="handleDateRangeChange"
            />
          </div>

          <!-- Reset Button -->
          <el-tooltip content="Đặt lại toàn bộ bộ lọc" placement="top">
            <el-button
              class="btn-soft btn-reset"
              :disabled="!hasActiveFilters"
              @click="resetFilters"
            >
              <el-icon><RefreshLeft /></el-icon>
              <span>Đặt lại</span>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <!-- E. Batch Actions Bar (Visible when rows selected) -->
      <transition name="slide-fade">
        <div v-if="selectedRows.length > 0" class="batch-actions-bar">
          <div class="batch-left">
            <span class="batch-count-pill">{{ selectedRows.length }}</span>
            <span class="batch-text">
              Đã chọn <strong>{{ selectedRows.length }}</strong> đơn hàng
            </span>
            <button type="button" class="batch-clear-link" @click="clearSelection">
              Bỏ chọn tất cả
            </button>
          </div>
          <div class="batch-right">
            <el-button
              size="small"
              type="primary"
              :loading="batchLoading === 'CONFIRM'"
              :disabled="batchConfirmableCount === 0"
              @click="handleBatchConfirm"
            >
              <el-icon><Check /></el-icon>
              <span>Xác nhận các đơn đã chọn ({{ batchConfirmableCount }})</span>
            </el-button>
            <el-button
              size="small"
              class="btn-soft"
              :loading="batchLoading === 'PRINT'"
              @click="handleBatchPrintPacking"
            >
              <el-icon><Printer /></el-icon>
              <span>In hàng loạt phiếu đóng gói</span>
            </el-button>
            <el-button
              size="small"
              type="danger"
              plain
              :loading="batchLoading === 'CANCEL'"
              :disabled="batchCancellableCount === 0"
              @click="handleBatchCancel"
            >
              <el-icon><Close /></el-icon>
              <span>Hủy đơn hàng loạt ({{ batchCancellableCount }})</span>
            </el-button>
          </div>
        </div>
      </transition>

      <!-- D. Optimized Order Data Table (Uniform 2-line Rhythm & Vertically Aligned Action Rail) -->
      <div class="table-wrapper" v-loading="loading">
        <el-table
          ref="tableRef"
          :data="items"
          row-key="id"
          stripe
          class="orders-table"
          style="width: 100%"
          @selection-change="handleSelectionChange"
          @row-click="handleRowClick"
        >
          <template #empty>
            <el-empty description="Không có đơn hàng nào phù hợp với bộ lọc hiện tại" :image-size="90">
              <el-button v-if="hasActiveFilters" type="primary" size="small" @click="resetFilters">
                Đặt lại bộ lọc
              </el-button>
            </el-empty>
          </template>

          <el-table-column type="selection" width="42" align="center" />

          <!-- 1. Mã đơn & Kênh bán (2-line compact) -->
          <el-table-column label="Mã đơn & Thời gian" width="182">
            <template #default="{ row }">
              <div class="col-order-code">
                <div class="order-code-head">
                  <button
                    type="button"
                    class="order-code-tag"
                    title="Xem nhanh chi tiết đơn hàng"
                    @click.stop="openQuickDrawer(row)"
                  >
                    {{ row.orderCode }}
                  </button>
                  <span v-if="isOnlineOrder(row)" class="channel-pill channel-online">
                    Online
                  </span>
                  <span v-else class="channel-pill channel-pos">
                    POS
                  </span>
                </div>
                <div class="order-time-text">
                  {{ formatCompactDateTime(row.orderDate) }}
                </div>
              </div>
            </template>
          </el-table-column>

          <!-- 2. Khách hàng (2-line crisp) -->
          <el-table-column label="Khách hàng" min-width="195">
            <template #default="{ row }">
              <div class="col-customer">
                <div class="customer-head-line">
                  <span class="customer-name">{{ getDisplayCustomerName(row) }}</span>
                </div>
                <div class="customer-sub-line">
                  <span class="phone-text">{{ getDisplayCustomerPhone(row) }}</span>
                  <button
                    v-if="hasCopyablePhone(row)"
                    type="button"
                    class="copy-phone-btn"
                    title="Sao chép số điện thoại"
                    @click.stop="copyPhone(getDisplayCustomerPhone(row))"
                  >
                    <el-icon><CopyDocument /></el-icon>
                  </button>
                  <template v-if="getShippingAddress(row)">
                    <span class="sub-dot">&middot;</span>
                    <el-tooltip
                      :content="getShippingAddress(row)"
                      placement="top"
                      :show-after="350"
                    >
                      <span class="customer-address">{{ getShippingAddress(row) }}</span>
                    </el-tooltip>
                  </template>
                  <template v-else-if="!isOnlineOrder(row) && row.employeeName">
                    <span class="sub-dot">&middot;</span>
                    <span class="pos-staff-note">NV: {{ row.employeeName }}</span>
                  </template>
                </div>
              </div>
            </template>
          </el-table-column>

          <!-- 3. Sản phẩm trong đơn (Uniform 2-line Quick Glance + Inline Hover Popover) -->
          <el-table-column label="Sản phẩm trong đơn" min-width="255">
            <template #default="{ row }">
              <div v-if="row.details && row.details.length > 0" class="col-products">
                <div class="primary-product-row">
                  <div class="product-thumb-box">
                    <img
                      v-if="row.details[0].thumbnailUrl"
                      :src="resolveThumbUrl(row.details[0].thumbnailUrl)"
                      :alt="row.details[0].productName"
                      class="product-thumb-img"
                      @error="handleImgError"
                    />
                    <span v-else class="thumb-placeholder">AD</span>
                  </div>
                  <div class="product-meta">
                    <div class="product-name" :title="row.details[0].productName">
                      {{ row.details[0].productName || 'Sản phẩm' }}
                    </div>
                    <div class="product-variant-line">
                      <span>EU {{ row.details[0].sizeValue || '-' }}</span>
                      <span class="dot-sep">/</span>
                      <span class="color-trunc">{{ row.details[0].colorName || '-' }}</span>
                      <span class="qty-badge">x{{ row.details[0].quantity }}</span>

                      <!-- Inline Popover pill for 2+ items (keeps uniform 2-line row height!) -->
                      <el-popover
                        v-if="row.details.length > 1"
                        placement="right"
                        :width="360"
                        trigger="hover"
                        popper-class="order-items-popover"
                      >
                        <template #reference>
                          <span class="more-items-pill" @click.stop>
                            +{{ row.details.length - 1 }} SP ({{ getTotalQty(row) }}đ)
                          </span>
                        </template>
                        <div class="popover-items-header">
                          <span>Chi tiết {{ row.details.length }} dòng sản phẩm ({{ row.orderCode }})</span>
                        </div>
                        <div class="popover-items-list">
                          <div
                            v-for="(item, idx) in row.details"
                            :key="item.id || idx"
                            class="popover-item-row"
                          >
                            <div class="popover-thumb-box">
                              <img
                                v-if="item.thumbnailUrl"
                                :src="resolveThumbUrl(item.thumbnailUrl)"
                                :alt="item.productName"
                                class="product-thumb-img"
                                @error="handleImgError"
                              />
                              <span v-else class="thumb-placeholder">AD</span>
                            </div>
                            <div class="popover-item-info">
                              <div class="popover-item-name">{{ item.productName }}</div>
                              <div class="popover-item-sub">
                                Size {{ item.sizeValue || '-' }} / {{ item.colorName || '-' }} &middot;
                                <strong>x{{ item.quantity }}</strong>
                              </div>
                            </div>
                            <div class="popover-item-price">
                              {{ formatCurrency(item.subtotal) }}
                            </div>
                          </div>
                        </div>
                      </el-popover>
                    </div>
                  </div>
                </div>
              </div>
              <span v-else class="muted-empty">Chưa có dữ liệu sản phẩm</span>
            </template>
          </el-table-column>

          <!-- 4. Tổng tiền & Thanh toán (2-line inline badges) -->
          <el-table-column label="Tổng tiền & Thanh toán" width="185" align="right">
            <template #default="{ row }">
              <div class="col-payment">
                <div class="order-total-amount">{{ formatCurrency(row.finalAmount) }}</div>
                <div class="payment-badges-inline">
                  <template v-if="getEffectivePayment(row) === 'BANK_TRANSFER'">
                    <span class="pay-tag pay-bank">CK</span>
                    <span v-if="isSepayMatched(row)" class="pay-status-badge status-sepay-paid">
                      ✓ SePay
                    </span>
                    <span v-else-if="row.status === 'CANCELLED'" class="pay-status-badge status-muted">
                      Đã hủy
                    </span>
                    <span v-else class="pay-status-badge status-unpaid">
                      Chờ TT
                    </span>
                  </template>

                  <template v-else-if="getEffectivePayment(row) === 'COD'">
                    <span class="pay-tag pay-cod">COD</span>
                    <span v-if="isCodCollected(row)" class="pay-status-badge status-cod-paid">
                      ✓ Đã thu
                    </span>
                    <span v-else-if="row.status === 'CANCELLED'" class="pay-status-badge status-muted">
                      Đã hủy
                    </span>
                    <span v-else class="pay-status-badge status-unpaid">
                      Chưa thu
                    </span>
                  </template>

                  <template v-else>
                    <span class="pay-tag pay-cash">Tiền mặt</span>
                    <span v-if="isCashPaid(row)" class="pay-status-badge status-cod-paid">
                      ✓ Đã thu
                    </span>
                  </template>
                </div>
              </div>
            </template>
          </el-table-column>

          <!-- 5. Trạng thái đơn hàng -->
          <el-table-column label="Trạng thái" width="132" align="center">
            <template #default="{ row }">
              <span class="status-pill" :class="`status-pill-${(row.status || '').toLowerCase()}`">
                <span class="status-dot" />
                <span>{{ getStatusLabel(row.status) }}</span>
              </span>
            </template>
          </el-table-column>

          <!-- 6. Thao tác (Right-pinned vertical icon rail + optional workflow CTA on left) -->
          <el-table-column label="Thao tác" width="205" fixed="right" align="right" class-name="small-padding fixed-width">
            <template #default="{ row }">
              <div class="vea-table-actions orders-action-rail">
                <!-- Slot trái: Nút chuyển trạng thái 1-click (chỉ hiện khi cần xử lý) -->
                <template v-if="row.status === 'PENDING' || row.status === 'DRAFT' || row.status === 'CONFIRMED' || row.status === 'SHIPPING'">
                  <el-button
                    v-if="row.status === 'PENDING' || row.status === 'DRAFT'"
                    size="small"
                    type="primary"
                    class="vea-cta-btn"
                    :loading="rowActionLoadingId === row.id"
                    @click.stop="handleQuickTransition(row, 'CONFIRMED')"
                  >
                    <el-icon><Check /></el-icon>
                    <span>Duyệt</span>
                  </el-button>
                  <el-button
                    v-else-if="row.status === 'CONFIRMED'"
                    size="small"
                    type="warning"
                    class="vea-cta-btn"
                    :loading="rowActionLoadingId === row.id"
                    @click.stop="handleQuickTransition(row, 'SHIPPING')"
                  >
                    <el-icon><Van /></el-icon>
                    <span>Giao</span>
                  </el-button>
                  <el-button
                    v-else-if="row.status === 'SHIPPING'"
                    size="small"
                    type="success"
                    class="vea-cta-btn"
                    :loading="rowActionLoadingId === row.id"
                    @click.stop="handleQuickTransition(row, 'COMPLETED')"
                  >
                    <el-icon><CircleCheck /></el-icon>
                    <span>Xong</span>
                  </el-button>
                  <span class="action-rail-sep" />
                </template>

                <!-- Slot phải: 3 nút icon luôn thẳng hàng dọc tuyệt đối trên mọi dòng -->
                <div class="action-icon-rail">
                  <el-tooltip content="Xem chi tiết đầy đủ" placement="top" :show-after="200">
                    <el-button
                      size="small"
                      class="vea-icon-btn vea-icon-primary"
                      aria-label="Xem chi tiết đơn hàng"
                      @click.stop="openDetailPage(row)"
                    >
                      <el-icon><View /></el-icon>
                    </el-button>
                  </el-tooltip>

                  <el-tooltip content="In hóa đơn" placement="top" :show-after="200">
                    <el-button
                      size="small"
                      class="vea-icon-btn vea-icon-info"
                      aria-label="In hóa đơn"
                      @click.stop="openPrintInvoice(row)"
                    >
                      <el-icon><Printer /></el-icon>
                    </el-button>
                  </el-tooltip>

                  <el-dropdown trigger="click" @command="(cmd) => handleRowMenuCommand(row, cmd)">
                    <span @click.stop>
                      <el-tooltip content="Thêm thao tác" placement="top" :show-after="200">
                        <el-button
                          size="small"
                          class="vea-icon-btn vea-icon-more"
                          aria-label="Thêm thao tác"
                        >
                          <el-icon><MoreFilled /></el-icon>
                        </el-button>
                      </el-tooltip>
                    </span>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="QUICK_VIEW">
                          <el-icon><Document /></el-icon>
                          <span>Xem nhanh trong Drawer</span>
                        </el-dropdown-item>
                        <el-dropdown-item command="PRINT_PACKING">
                          <el-icon><Printer /></el-icon>
                          <span>In phiếu giao hàng / đóng gói</span>
                        </el-dropdown-item>
                        <el-dropdown-item command="EDIT_NOTE">
                          <el-icon><Edit /></el-icon>
                          <span>Chỉnh sửa ghi chú</span>
                        </el-dropdown-item>
                        <el-dropdown-item
                          v-if="canCancel(row)"
                          command="CANCEL"
                          divided
                          class="dropdown-danger-item"
                        >
                          <el-icon><Close /></el-icon>
                          <span>Hủy đơn hàng</span>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="pagination-bar">
          <div class="pagination-meta">
            Hiển thị <strong>{{ items.length }}</strong> / <strong>{{ pagination.total }}</strong> đơn hàng
          </div>
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- Quick Order Detail Drawer (click on order code) -->
    <el-drawer
      v-model="drawerVisible"
      size="540px"
      destroy-on-close
      class="order-quick-drawer"
    >
      <template #header>
        <div v-if="drawerOrder" class="drawer-header-content">
          <div class="drawer-title-line">
            <span class="drawer-code">{{ drawerOrder.orderCode }}</span>
            <span class="status-pill" :class="`status-pill-${(drawerOrder.status || '').toLowerCase()}`">
              <span class="status-dot" />
              <span>{{ getStatusLabel(drawerOrder.status) }}</span>
            </span>
          </div>
          <div class="drawer-sub-line">
            {{ isOnlineOrder(drawerOrder) ? 'Kênh Online Store' : 'Kênh Tại quầy (POS)' }} &middot;
            {{ formatCompactDateTime(drawerOrder.orderDate) }}
          </div>
        </div>
      </template>

      <div v-if="drawerOrder" v-loading="drawerLoading" class="drawer-body">
        <!-- Quick Customer & Shipping Info -->
        <div class="drawer-section">
          <div class="drawer-section-title">Thông tin khách hàng &amp; giao nhận</div>
          <div class="drawer-info-grid">
            <div class="info-row">
              <span class="info-label">Khách hàng:</span>
              <span class="info-value font-bold">{{ getDisplayCustomerName(drawerOrder) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Điện thoại:</span>
              <span class="info-value">
                {{ getDisplayCustomerPhone(drawerOrder) }}
                <button
                  v-if="hasCopyablePhone(drawerOrder)"
                  type="button"
                  class="copy-phone-btn"
                  @click="copyPhone(getDisplayCustomerPhone(drawerOrder))"
                >
                  <el-icon><CopyDocument /></el-icon>
                </button>
              </span>
            </div>
            <div v-if="getShippingAddress(drawerOrder)" class="info-row">
              <span class="info-label">Địa chỉ nhận:</span>
              <span class="info-value">{{ getShippingAddress(drawerOrder) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Thanh toán:</span>
              <span class="info-value">
                {{ getEffectivePaymentLabel(drawerOrder) }}
                <span v-if="isSepayMatched(drawerOrder)" class="pay-status-badge status-sepay-paid" style="margin-left: 6px">
                  Đã khớp SePay
                </span>
              </span>
            </div>
            <div v-if="drawerOrder.note" class="info-row">
              <span class="info-label">Ghi chú:</span>
              <span class="info-value note-box">{{ drawerOrder.note }}</span>
            </div>
          </div>
        </div>

        <!-- Products List -->
        <div class="drawer-section">
          <div class="drawer-section-title">
            Sản phẩm trong đơn ({{ getTotalQty(drawerOrder) }} đôi)
          </div>
          <div class="drawer-products-list">
            <div
              v-for="(item, idx) in drawerOrder.details || []"
              :key="item.id || idx"
              class="drawer-product-item"
            >
              <div class="product-thumb-box">
                <img
                  v-if="item.thumbnailUrl"
                  :src="resolveThumbUrl(item.thumbnailUrl)"
                  :alt="item.productName"
                  class="product-thumb-img"
                  @error="handleImgError"
                />
                <span v-else class="thumb-placeholder">AD</span>
              </div>
              <div class="drawer-product-info">
                <div class="product-name">{{ item.productName }}</div>
                <div class="product-variant-line">
                  <span>SKU: {{ item.sku || '-' }}</span>
                  <span class="dot-sep">&middot;</span>
                  <span>Size {{ item.sizeValue || '-' }} / {{ item.colorName || '-' }}</span>
                </div>
              </div>
              <div class="drawer-product-right">
                <div class="drawer-qty">x{{ item.quantity }}</div>
                <div class="drawer-subtotal">{{ formatCurrency(item.subtotal) }}</div>
              </div>
            </div>
          </div>
          <div class="drawer-total-row">
            <span>Tổng thanh toán</span>
            <strong>{{ formatCurrency(drawerOrder.finalAmount) }}</strong>
          </div>
        </div>

        <!-- Drawer Status History -->
        <div v-if="drawerOrder.histories && drawerOrder.histories.length" class="drawer-section">
          <div class="drawer-section-title">Lịch sử xử lý</div>
          <div class="drawer-timeline">
            <div
              v-for="(h, idx) in drawerOrder.histories"
              :key="h.id || idx"
              class="timeline-item"
            >
              <div class="timeline-dot" />
              <div class="timeline-content">
                <div class="timeline-top">
                  <strong>{{ getStatusLabel(h.toStatus) }}</strong>
                  <span>{{ formatCompactDateTime(h.changedAt) }}</span>
                </div>
                <div v-if="h.note" class="timeline-note">{{ h.note }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div v-if="drawerOrder" class="drawer-footer-actions">
          <el-button class="btn-soft" @click="openDetailPage(drawerOrder)">
            Mở trang chi tiết
          </el-button>
          <el-button class="btn-soft" @click="printPackingSlips([drawerOrder])">
            <el-icon><Printer /></el-icon>
            <span>In phiếu giao</span>
          </el-button>
          <el-button
            v-if="drawerOrder.status === 'PENDING' || drawerOrder.status === 'DRAFT'"
            type="primary"
            :loading="rowActionLoadingId === drawerOrder.id"
            @click="handleQuickTransition(drawerOrder, 'CONFIRMED', true)"
          >
            Duyệt đơn ngay
          </el-button>
          <el-button
            v-else-if="drawerOrder.status === 'CONFIRMED'"
            type="warning"
            :loading="rowActionLoadingId === drawerOrder.id"
            @click="handleQuickTransition(drawerOrder, 'SHIPPING', true)"
          >
            Giao hàng ngay
          </el-button>
          <el-button
            v-else-if="drawerOrder.status === 'SHIPPING'"
            type="success"
            :loading="rowActionLoadingId === drawerOrder.id"
            @click="handleQuickTransition(drawerOrder, 'COMPLETED', true)"
          >
            Giao thành công
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- Printable Invoice Modal -->
    <InvoicePrintModal v-model="invoiceVisible" :order="selectedOrderForPrint" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Download,
  Refresh,
  RefreshLeft,
  Search,
  View,
  Printer,
  MoreFilled,
  CopyDocument,
  Check,
  Close,
  Edit,
  Document,
  Van,
  CircleCheck
} from '@element-plus/icons-vue'
import { orderService } from '@/services/orderService'
import { formatCurrency } from '@/utils/formatters'
import { ORDER_STATUS_MAP } from '@/constants'
import { useDebouncedWatch } from '@/composables/useDebounce'
import InvoicePrintModal from '@/components/InvoicePrintModal.vue'

const route = useRoute()
const router = useRouter()

const tableRef = ref(null)
const loading = ref(false)
const exporting = ref(false)
const rowActionLoadingId = ref(null)
const batchLoading = ref('')
const items = ref([])
const selectedRows = ref([])
const lastUpdatedText = ref('')
const isHydratingQuery = ref(false)

const statusTabs = [
  { key: 'ALL', label: 'Tất cả', badgeTone: 'neutral' },
  { key: 'PENDING', label: 'Chờ xác nhận', badgeTone: 'alert' },
  { key: 'CONFIRMED', label: 'Đã xác nhận', badgeTone: 'primary' },
  { key: 'SHIPPING', label: 'Đang giao hàng', badgeTone: 'warning' },
  { key: 'COMPLETED', label: 'Hoàn thành', badgeTone: 'success' },
  { key: 'CANCELLED', label: 'Đã hủy', badgeTone: 'muted' }
]

const statusCounts = reactive({
  ALL: 0,
  PENDING: 0,
  CONFIRMED: 0,
  SHIPPING: 0,
  COMPLETED: 0,
  CANCELLED: 0
})

const activeStatusTab = ref('ALL')

const filters = reactive({
  keyword: '',
  paymentMethod: '',
  channel: 'POS',
  fromDate: null,
  toDate: null
})

const dateRange = ref(null)

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// Quick Detail Drawer state
const drawerVisible = ref(false)
const drawerLoading = ref(false)
const drawerOrder = ref(null)

// Invoice Modal state
const invoiceVisible = ref(false)
const selectedOrderForPrint = ref(null)

// DateRangePicker shortcuts
function formatLocalYMD(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const dateShortcuts = [
  {
    text: 'Hôm nay',
    value: () => {
      const today = new Date()
      return [today, today]
    }
  },
  {
    text: '7 ngày qua',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      return [start, end]
    }
  },
  {
    text: 'Tháng này',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      return [start, now]
    }
  }
]

const hasActiveFilters = computed(() => {
  return Boolean(
    activeStatusTab.value !== 'ALL' ||
      filters.paymentMethod ||
      filters.channel !== 'POS' ||
      (filters.keyword && filters.keyword.trim()) ||
      filters.fromDate ||
      filters.toDate
  )
})

const batchConfirmableCount = computed(
  () => selectedRows.value.filter((r) => r.status === 'PENDING' || r.status === 'DRAFT').length
)

const batchCancellableCount = computed(
  () => selectedRows.value.filter((r) => canCancel(r)).length
)

// ==================== HELPERS ====================

function isOnlineOrder(row) {
  if (!row) return false
  if (row.channel) return row.channel.toUpperCase() === 'ONLINE'
  return !row.employeeId
}

function getEffectivePayment(row) {
  if (!row) return 'CASH'
  const raw = (row.paymentMethod || 'CASH').toUpperCase()
  if (raw === 'BANK_TRANSFER') return 'BANK_TRANSFER'
  if (raw === 'COD') return 'COD'
  if (raw === 'CASH' && isOnlineOrder(row)) return 'COD'
  return 'CASH'
}

function getEffectivePaymentLabel(row) {
  const pm = getEffectivePayment(row)
  if (pm === 'BANK_TRANSFER') return 'Chuyển khoản ngân hàng'
  if (pm === 'COD') return 'COD (Giao hàng thu tiền)'
  return 'Tiền mặt tại quầy'
}

function hasPaidPayment(row) {
  if (!row?.payments || !Array.isArray(row.payments)) return false
  return row.payments.some((p) => (p.status || '').toUpperCase() === 'PAID')
}

function isSepayMatched(row) {
  if (getEffectivePayment(row) !== 'BANK_TRANSFER') return false
  if (row.status === 'CANCELLED') return false
  return (
    hasPaidPayment(row) ||
    ['CONFIRMED', 'SHIPPING', 'COMPLETED'].includes((row.status || '').toUpperCase())
  )
}

function isCodCollected(row) {
  if (getEffectivePayment(row) !== 'COD') return false
  return row.status === 'COMPLETED' || hasPaidPayment(row)
}

function isCashPaid(row) {
  if (getEffectivePayment(row) !== 'CASH') return false
  return row.status === 'COMPLETED' || hasPaidPayment(row)
}

function canCancel(row) {
  return row && row.status !== 'COMPLETED' && row.status !== 'CANCELLED'
}

function getStatusLabel(status) {
  const map = {
    DRAFT: 'Đơn nháp',
    PENDING: 'Chờ xác nhận',
    CONFIRMED: 'Đã xác nhận',
    SHIPPING: 'Đang giao hàng',
    COMPLETED: 'Hoàn thành',
    CANCELLED: 'Đã hủy'
  }
  return map[status] || ORDER_STATUS_MAP[status]?.label || status || '---'
}

function formatCompactDateTime(value) {
  if (!value) return '---'
  const d = new Date(value)
  if (isNaN(d.getTime())) return value
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const MM = String(d.getMonth() + 1).padStart(2, '0')
  const yyyy = d.getFullYear()
  return `${hh}:${mm} ${dd}/${MM}/${yyyy}`
}

function extractNoteField(note, fieldLabel) {
  if (!note) return ''
  const regex = new RegExp(`${fieldLabel}:\\s*([^|]+)`, 'i')
  const match = note.match(regex)
  return match ? match[1].trim() : ''
}

function getDisplayCustomerName(row) {
  if (!row) return 'Khách lẻ'
  if (isOnlineOrder(row)) {
    const fromNote = extractNoteField(row.note, 'Người nhận')
    if (fromNote) return fromNote
  }
  return row.customerName || 'Khách lẻ tại quầy'
}

function getDisplayCustomerPhone(row) {
  if (!row) return '---'
  if (isOnlineOrder(row)) {
    const fromNote = extractNoteField(row.note, 'SĐT')
    if (fromNote) return fromNote
  }
  return row.customerPhone || '---'
}

function hasCopyablePhone(row) {
  const phone = getDisplayCustomerPhone(row)
  return Boolean(phone && phone !== '---' && phone.trim().length >= 6)
}

function getShippingAddress(row) {
  if (!row) return ''
  if (isOnlineOrder(row)) {
    const fromNote = extractNoteField(row.note, 'Đ/C nhận')
    if (fromNote) return fromNote
  }
  return row.customerAddress || ''
}

function getTotalQty(row) {
  if (!row?.details || !Array.isArray(row.details)) return 0
  return row.details.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
}

function resolveThumbUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('/')) {
    return url
  }
  return `/${url}`
}

function handleImgError(e) {
  if (e?.target) {
    e.target.style.display = 'none'
  }
}

async function copyPhone(phone) {
  try {
    await navigator.clipboard.writeText(phone)
    ElMessage.success(`Đã sao chép SĐT: ${phone}`)
  } catch {
    const ta = document.createElement('textarea')
    ta.value = phone
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success(`Đã sao chép SĐT: ${phone}`)
  }
}

function updateLastUpdatedStamp() {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  lastUpdatedText.value = `${hh}:${mm}:${ss}`
}

// ==================== URL QUERY SYNC ====================

function hydrateFromUrl() {
  isHydratingQuery.value = true
  const q = route.query
  const validStatuses = ['ALL', 'PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED']
  const st = typeof q.status === 'string' ? q.status.toUpperCase() : 'ALL'
  activeStatusTab.value = validStatuses.includes(st) ? st : 'ALL'

  const pm = typeof q.payment === 'string' ? q.payment.toUpperCase() : ''
  filters.paymentMethod = ['CASH', 'BANK_TRANSFER', 'COD'].includes(pm) ? pm : ''

  const ch = typeof q.channel === 'string' ? q.channel.toUpperCase() : null
  if (ch === 'ALL') {
    filters.channel = ''
  } else if (ch && ['POS', 'ONLINE'].includes(ch)) {
    filters.channel = ch
  } else {
    filters.channel = 'POS'
  }

  filters.keyword = typeof q.keyword === 'string' ? q.keyword : ''
  filters.fromDate = typeof q.from === 'string' && q.from ? q.from : null
  filters.toDate = typeof q.to === 'string' && q.to ? q.to : null
  dateRange.value = filters.fromDate && filters.toDate ? [filters.fromDate, filters.toDate] : null

  const p = Number(q.page)
  pagination.page = Number.isInteger(p) && p > 0 ? p : 1

  const s = Number(q.size)
  pagination.size = [10, 20, 50, 100].includes(s) ? s : 20

  setTimeout(() => {
    isHydratingQuery.value = false
  }, 0)
}

function syncUrlQuery() {
  const query = {}
  if (activeStatusTab.value && activeStatusTab.value !== 'ALL') {
    query.status = activeStatusTab.value
  }
  if (filters.paymentMethod) query.payment = filters.paymentMethod
  if (filters.channel === '') query.channel = 'ALL'
  else if (filters.channel !== 'POS') query.channel = filters.channel
  if (filters.keyword && filters.keyword.trim()) query.keyword = filters.keyword.trim()
  if (filters.fromDate) query.from = filters.fromDate
  if (filters.toDate) query.to = filters.toDate
  if (pagination.page > 1) query.page = String(pagination.page)
  if (pagination.size !== 20) query.size = String(pagination.size)

  router.replace({ query }).catch(() => {})
}

// ==================== DATA FETCHING ====================

function buildCommonFilterParams() {
  return {
    keyword: filters.keyword?.trim() || undefined,
    paymentMethod: filters.paymentMethod || undefined,
    channel: filters.channel || undefined,
    fromDate: filters.fromDate || undefined,
    toDate: filters.toDate || undefined
  }
}

async function fetchStatusCounts() {
  try {
    const res = await orderService.getStatusCounts(buildCommonFilterParams())
    const data = res.data || {}
    statusCounts.ALL = Number(data.ALL || 0)
    statusCounts.PENDING = Number(data.PENDING || 0)
    statusCounts.CONFIRMED = Number(data.CONFIRMED || 0)
    statusCounts.SHIPPING = Number(data.SHIPPING || 0)
    statusCounts.COMPLETED = Number(data.COMPLETED || 0)
    statusCounts.CANCELLED = Number(data.CANCELLED || 0)
  } catch {
    // non-blocking
  }
}

async function loadData(showLoader = true) {
  if (showLoader) loading.value = true
  try {
    const params = {
      ...buildCommonFilterParams(),
      status: activeStatusTab.value !== 'ALL' ? activeStatusTab.value : undefined,
      page: pagination.page - 1,
      size: pagination.size
    }
    const [ordersRes] = await Promise.all([
      orderService.getOrders(params),
      fetchStatusCounts()
    ])
    items.value = ordersRes.data?.content || []
    pagination.total = ordersRes.data?.totalElements || 0
    updateLastUpdatedStamp()
  } finally {
    if (showLoader) loading.value = false
  }
}

function handleTabChange() {
  pagination.page = 1
  clearSelection()
  syncUrlQuery()
  loadData()
}

function handleFilterChange() {
  pagination.page = 1
  clearSelection()
  syncUrlQuery()
  loadData()
}

function handleDateRangeChange(val) {
  if (Array.isArray(val) && val.length === 2) {
    filters.fromDate = val[0]
    filters.toDate = val[1]
  } else {
    filters.fromDate = null
    filters.toDate = null
  }
  handleFilterChange()
}

function handleImmediateSearch() {
  pagination.page = 1
  syncUrlQuery()
  loadData()
}

function handlePageChange() {
  clearSelection()
  syncUrlQuery()
  loadData()
}

function handlePageSizeChange() {
  pagination.page = 1
  clearSelection()
  syncUrlQuery()
  loadData()
}

function resetFilters() {
  activeStatusTab.value = 'ALL'
  filters.keyword = ''
  filters.paymentMethod = ''
  filters.channel = 'POS'
  filters.fromDate = null
  filters.toDate = null
  dateRange.value = null
  pagination.page = 1
  clearSelection()
  syncUrlQuery()
  loadData()
}

function handleManualRefresh() {
  loadData(true)
}

useDebouncedWatch(
  () => filters.keyword,
  () => {
    if (isHydratingQuery.value) return
    pagination.page = 1
    syncUrlQuery()
    loadData()
  },
  350
)

// ==================== ROW ACTIONS ====================

function handleSelectionChange(rows) {
  selectedRows.value = rows || []
}

function handleRowClick(row, column) {
  if (!column || column.type === 'selection' || column.label === 'Thao tác') return
  openQuickDrawer(row)
}

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

function openDetailPage(row) {
  router.push(`/admin/orders/${row.id}`)
}

async function openQuickDrawer(row) {
  drawerOrder.value = row
  drawerVisible.value = true
  drawerLoading.value = true
  try {
    const res = await orderService.getOrderById(row.id)
    drawerOrder.value = res.data
  } finally {
    drawerLoading.value = false
  }
}

async function openPrintInvoice(row) {
  const res = await orderService.getOrderById(row.id)
  selectedOrderForPrint.value = res.data
  invoiceVisible.value = true
}

async function handleQuickTransition(row, targetStatus, refreshDrawer = false) {
  const defaultNotes = {
    CONFIRMED: 'Nhân viên duyệt nhanh đơn hàng (1-Click)',
    SHIPPING: 'Đơn hàng đã đóng gói và bàn giao đơn vị vận chuyển',
    COMPLETED: 'Giao hàng thành công và hoàn tất thu tiền'
  }
  const labels = {
    CONFIRMED: 'Đã xác nhận',
    SHIPPING: 'Đang giao hàng',
    COMPLETED: 'Hoàn thành'
  }
  rowActionLoadingId.value = row.id
  try {
    const res = await orderService.updateOrderStatus(row.id, {
      status: targetStatus,
      note: defaultNotes[targetStatus]
    })
    ElMessage.success(`Đơn ${row.orderCode} đã chuyển sang "${labels[targetStatus]}"`)
    if (refreshDrawer && drawerVisible.value) {
      drawerOrder.value = res.data
    }
    await loadData(false)
  } finally {
    rowActionLoadingId.value = null
  }
}

function handleRowMenuCommand(row, command) {
  if (command === 'QUICK_VIEW') {
    openQuickDrawer(row)
  } else if (command === 'PRINT_PACKING') {
    printPackingSlips([row])
  } else if (command === 'EDIT_NOTE') {
    handleEditNote(row)
  } else if (command === 'CANCEL') {
    handleCancelSingleOrder(row)
  }
}

function handleEditNote(row) {
  ElMessageBox.prompt(
    `Chỉnh sửa ghi chú nội bộ / thông tin giao nhận cho đơn ${row.orderCode}:`,
    'Chỉnh sửa ghi chú đơn hàng',
    {
      confirmButtonText: 'Lưu ghi chú',
      cancelButtonText: 'Hủy',
      inputType: 'textarea',
      inputValue: row.note || ''
    }
  )
    .then(async ({ value }) => {
      await orderService.updateOrderNote(row.id, { note: value || '' })
      ElMessage.success(`Đã cập nhật ghi chú cho đơn ${row.orderCode}`)
      await loadData(false)
    })
    .catch(() => {})
}

function handleCancelSingleOrder(row) {
  ElMessageBox.prompt(
    `Vui lòng nhập lý do hủy đơn hàng ${row.orderCode} (tồn kho sẽ được hoàn trả tự động):`,
    'Hủy đơn hàng',
    {
      confirmButtonText: 'Xác nhận hủy đơn',
      cancelButtonText: 'Đóng',
      confirmButtonClass: 'el-button--danger',
      inputPlaceholder: 'Ví dụ: Khách đổi ý / Hết hàng / Sai thông tin nhận hàng...',
      inputValidator: (val) => {
        if (!val || !val.trim()) return 'Vui lòng nhập lý do hủy đơn hàng'
        return true
      }
    }
  )
    .then(async ({ value }) => {
      rowActionLoadingId.value = row.id
      try {
        await orderService.updateOrderStatus(row.id, {
          status: 'CANCELLED',
          note: `Lý do hủy: ${value.trim()}`
        })
        ElMessage.success(`Đã hủy đơn hàng ${row.orderCode} và hoàn trả tồn kho`)
        await loadData(false)
      } finally {
        rowActionLoadingId.value = null
      }
    })
    .catch(() => {})
}

// ==================== BATCH ACTIONS ====================

async function handleBatchConfirm() {
  const targets = selectedRows.value.filter(
    (r) => r.status === 'PENDING' || r.status === 'DRAFT'
  )
  if (!targets.length) {
    ElMessage.warning('Không có đơn hàng nào ở trạng thái Chờ xác nhận trong danh sách đã chọn')
    return
  }

  try {
    await ElMessageBox.confirm(
      `Xác nhận duyệt hàng loạt ${targets.length} đơn hàng đang chờ xác nhận?`,
      'Xác nhận hàng loạt',
      {
        confirmButtonText: `Duyệt ${targets.length} đơn`,
        cancelButtonText: 'Đóng',
        type: 'info'
      }
    )
  } catch {
    return
  }

  batchLoading.value = 'CONFIRM'
  let successCount = 0
  let failCount = 0
  try {
    for (const order of targets) {
      try {
        await orderService.updateOrderStatus(order.id, {
          status: 'CONFIRMED',
          note: 'Xác nhận hàng loạt từ màn hình Quản lý Đơn hàng'
        })
        successCount++
      } catch {
        failCount++
      }
    }
    if (successCount > 0) {
      ElMessage.success(
        `Đã xác nhận thành công ${successCount}/${targets.length} đơn hàng` +
          (failCount > 0 ? ` (${failCount} đơn thất bại)` : '')
      )
    } else {
      ElMessage.error('Không thể xác nhận các đơn hàng đã chọn')
    }
    clearSelection()
    await loadData(false)
  } finally {
    batchLoading.value = ''
  }
}

async function handleBatchCancel() {
  const targets = selectedRows.value.filter((r) => canCancel(r))
  if (!targets.length) {
    ElMessage.warning('Các đơn hàng đã chọn không thể hủy (đã hoàn thành hoặc đã hủy)')
    return
  }

  let reason = ''
  try {
    const res = await ElMessageBox.prompt(
      `Nhập lý do hủy cho ${targets.length} đơn hàng đã chọn (tồn kho sẽ được hoàn trả tự động):`,
      'Hủy đơn hàng loạt',
      {
        confirmButtonText: `Hủy ${targets.length} đơn`,
        cancelButtonText: 'Đóng',
        confirmButtonClass: 'el-button--danger',
        inputPlaceholder: 'Nhập lý do hủy đơn...',
        inputValidator: (val) => (!val || !val.trim() ? 'Vui lòng nhập lý do hủy đơn' : true)
      }
    )
    reason = res.value.trim()
  } catch {
    return
  }

  batchLoading.value = 'CANCEL'
  let successCount = 0
  let failCount = 0
  try {
    for (const order of targets) {
      try {
        await orderService.updateOrderStatus(order.id, {
          status: 'CANCELLED',
          note: `Hủy hàng loạt - Lý do: ${reason}`
        })
        successCount++
      } catch {
        failCount++
      }
    }
    if (successCount > 0) {
      ElMessage.success(
        `Đã hủy ${successCount}/${targets.length} đơn hàng` +
          (failCount > 0 ? ` (${failCount} đơn lỗi)` : '')
      )
    }
    clearSelection()
    await loadData(false)
  } finally {
    batchLoading.value = ''
  }
}

async function handleBatchPrintPacking() {
  if (!selectedRows.value.length) return
  batchLoading.value = 'PRINT'
  try {
    printPackingSlips(selectedRows.value)
  } finally {
    batchLoading.value = ''
  }
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function printPackingSlips(orders) {
  if (!orders || !orders.length) return
  const printWindow = window.open('', '_blank', 'width=900,height=750')
  if (!printWindow) {
    ElMessage.warning('Trình duyệt đang chặn cửa sổ bật lên (popup). Vui lòng cho phép popup để in.')
    return
  }

  const slipsHtml = orders
    .map((order) => {
      const custName = getDisplayCustomerName(order)
      const custPhone = getDisplayCustomerPhone(order)
      const shipAddr = getShippingAddress(order) || 'Nhận trực tiếp tại cửa hàng ADHA SNEAKER'
      const pm = getEffectivePayment(order)
      const codCollect =
        pm === 'COD' && !isCodCollected(order) ? formatCurrency(order.finalAmount) : '0 ₫ (Đã thanh toán)'

      const rowsHtml = (order.details || [])
        .map(
          (item, i) => `
          <tr>
            <td style="text-align:center">${i + 1}</td>
            <td><strong>${escapeHtml(item.productName)}</strong><br/><span style="color:#64748b;font-size:11px">SKU: ${escapeHtml(item.sku || '-')}</span></td>
            <td style="text-align:center">${escapeHtml(item.sizeValue || '-')} / ${escapeHtml(item.colorName || '-')}</td>
            <td style="text-align:center;font-weight:700">${item.quantity}</td>
            <td style="text-align:right">${escapeHtml(formatCurrency(item.subtotal))}</td>
          </tr>
        `
        )
        .join('')

      return `
        <div class="slip">
          <div class="slip-header">
            <div>
              <div class="brand">ADHA SNEAKER STORE</div>
              <div class="sub">120 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh &middot; Hotline: 0901 000 001</div>
            </div>
            <div class="slip-meta">
              <div class="slip-title">PHIẾU ĐÓNG GÓI &amp; GIAO HÀNG</div>
              <div class="slip-code">${escapeHtml(order.orderCode)}</div>
              <div class="sub">${escapeHtml(formatCompactDateTime(order.orderDate))}</div>
            </div>
          </div>

          <div class="info-box">
            <div><strong>Người nhận:</strong> ${escapeHtml(custName)}</div>
            <div><strong>Điện thoại:</strong> ${escapeHtml(custPhone)}</div>
            <div style="grid-column: 1 / -1"><strong>Địa chỉ giao:</strong> ${escapeHtml(shipAddr)}</div>
            <div><strong>Kênh bán:</strong> ${isOnlineOrder(order) ? 'Online Store' : 'Tại quầy (POS)'}</div>
            <div><strong>Phương thức TT:</strong> ${escapeHtml(getEffectivePaymentLabel(order))}</div>
          </div>

          <table>
            <thead>
              <tr>
                <th style="width:36px;text-align:center">#</th>
                <th>Sản phẩm</th>
                <th style="width:130px;text-align:center">Size / Màu</th>
                <th style="width:48px;text-align:center">SL</th>
                <th style="width:120px;text-align:right">Thành tiền</th>
              </tr>
            </thead>
            <tbody>${rowsHtml}</tbody>
          </table>

          <div class="totals-grid">
            <div class="note-area">
              <strong>Ghi chú đơn hàng:</strong><br/>
              ${escapeHtml(order.note || 'Không có ghi chú')}
            </div>
            <div class="cod-box">
              <div class="cod-row"><span>Tổng giá trị đơn:</span><strong>${escapeHtml(formatCurrency(order.finalAmount))}</strong></div>
              <div class="cod-row cod-highlight"><span>TIỀN THU HỘ (COD):</span><strong>${escapeHtml(codCollect)}</strong></div>
            </div>
          </div>

          <div class="slip-footer">
            <span>Nhân viên đóng gói: ______________________</span>
            <span>Chữ ký người nhận: ______________________</span>
          </div>
        </div>
      `
    })
    .join('')

  printWindow.document.write(`
    <!DOCTYPE html>
    <html lang="vi">
      <head>
        <meta charset="UTF-8" />
        <title>In Phiếu Đóng Gói (${orders.length} đơn)</title>
        <style>
          * { box-sizing: border-box; }
          body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif; color: #0f172a; margin: 0; padding: 16px; background: #fff; }
          .slip { border: 1.5px solid #0f172a; border-radius: 6px; padding: 18px 20px; margin-bottom: 24px; page-break-after: always; }
          .slip:last-child { page-break-after: auto; }
          .slip-header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #0f172a; padding-bottom: 12px; margin-bottom: 14px; }
          .brand { font-size: 18px; font-weight: 800; letter-spacing: 0.04em; }
          .sub { font-size: 12px; color: #475569; margin-top: 3px; }
          .slip-meta { text-align: right; }
          .slip-title { font-size: 12px; font-weight: 700; color: #475569; letter-spacing: 0.05em; }
          .slip-code { font-size: 18px; font-weight: 800; color: #ea580c; margin-top: 2px; }
          .info-box { display: grid; grid-template-columns: 1fr 1fr; gap: 6px 14px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 5px; padding: 10px 12px; font-size: 13px; margin-bottom: 14px; }
          table { width: 100%; border-collapse: collapse; font-size: 13px; margin-bottom: 14px; }
          th, td { border: 1px solid #cbd5e1; padding: 7px 9px; text-align: left; }
          th { background: #0f172a; color: #fff; font-weight: 600; font-size: 12px; }
          .totals-grid { display: flex; justify-content: space-between; gap: 16px; align-items: stretch; }
          .note-area { flex: 1; font-size: 12px; color: #334155; background: #f8fafc; padding: 10px; border-radius: 5px; border: 1px dashed #cbd5e1; }
          .cod-box { width: 290px; border: 1.5px solid #0f172a; border-radius: 5px; padding: 10px 12px; font-size: 13px; }
          .cod-row { display: flex; justify-content: space-between; margin-bottom: 6px; }
          .cod-highlight { border-top: 1px solid #cbd5e1; padding-top: 6px; margin-bottom: 0; font-size: 14px; color: #dc2626; }
          .slip-footer { display: flex; justify-content: space-between; margin-top: 22px; font-size: 12px; color: #475569; }
          @media print { body { padding: 0; } .slip { margin-bottom: 0; } }
        </style>
      </head>
      <body>${slipsHtml}</body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => printWindow.print(), 200)
}

// ==================== EXCEL EXPORT (.xlsx thật từ Backend Apache POI) ====================

async function handleExportExcel() {
  exporting.value = true
  try {
    const res = await orderService.exportOrdersExcel({
      ...buildCommonFilterParams(),
      status: activeStatusTab.value !== 'ALL' ? activeStatusTab.value : undefined
    })
    const raw = res instanceof Blob ? res : (res?.data ?? res)
    const blob = new Blob([raw], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `ADHA_DonHang_${formatLocalYMD(new Date())}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    ElMessage.success('Đã xuất danh sách đơn hàng ra file Excel (.xlsx) thành công')
  } catch {
    ElMessage.error('Không thể xuất file Excel đơn hàng')
  } finally {
    exporting.value = false
  }
}

// ==================== LIFECYCLE & POLLING ====================

let autoRefreshTimer = null

onMounted(() => {
  hydrateFromUrl()
  loadData(true)

  autoRefreshTimer = setInterval(() => {
    if (
      !loading.value &&
      !invoiceVisible.value &&
      !drawerVisible.value &&
      selectedRows.value.length === 0 &&
      !rowActionLoadingId.value &&
      !batchLoading.value
    ) {
      loadData(false)
    }
  }, 12000)
})

onUnmounted(() => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
})
</script>

<style scoped lang="scss">
.orders-workspace {
  gap: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-refresh {
  .refresh-time {
    margin-left: 6px;
    padding-left: 6px;
    border-left: 1px solid #e5e5e5;
    font-size: 11px;
    font-weight: 600;
    color: #737373;
    font-variant-numeric: tabular-nums;
  }
}

.is-spinning {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Main Surface */
.orders-surface {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  overflow: hidden;
}

/* Status Tabs */
.status-tabs-wrap {
  padding: 6px 22px 0;
  border-bottom: 1px solid #e5e5e5;
  background: #ffffff;
}

.order-status-tabs {
  :deep(.el-tabs__header) {
    margin: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__active-bar) {
    height: 2.5px;
    background-color: var(--vea-theme, #409eff);
  }

  :deep(.el-tabs__item) {
    height: 40px;
    font-size: 12.5px;
    font-weight: 600;
    color: #606266;
    padding: 0 15px;

    &.is-active {
      color: var(--vea-theme, #409eff);
      font-weight: 700;
    }

    &:hover {
      color: var(--vea-theme, #409eff);
    }
  }
}

.tab-label-inner {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.tab-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;

  &.badge-neutral {
    background: #f1f5f9;
    color: #334155;
  }

  &.badge-alert {
    background: #fef2f2;
    color: #dc2626;
    border: 1px solid #fecaca;
  }

  &.badge-primary {
    background: #eff6ff;
    color: #1d4ed8;
  }

  &.badge-warning {
    background: #fffbeb;
    color: #b45309;
  }

  &.badge-success {
    background: #ecfdf5;
    color: #047857;
  }

  &.badge-muted {
    background: #f1f5f9;
    color: #64748b;
  }

  &.is-zero {
    background: #f8fafc;
    color: #94a3b8;
    border-color: #e2e8f0;
  }
}

/* Filters Toolbar */
.filters-toolbar {
  padding: 10px 16px;
  background: #fafbfc;
  border-bottom: 1px solid #e6ebf5;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-row-segments {
  justify-content: space-between;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 12px;
  font-weight: 800;
  color: #525252;
  white-space: nowrap;
}

.segmented-radio-group {
  :deep(.el-radio-button__inner) {
    font-size: 12px;
    font-weight: 700;
    padding: 7px 13px;
    border-color: #d4d4d4;
    color: #404040;
    background: #ffffff;
    box-shadow: none !important;
  }

  :deep(.el-radio-button:first-child .el-radio-button__inner) {
    border-radius: 5px 0 0 5px;
  }

  :deep(.el-radio-button:last-child .el-radio-button__inner) {
    border-radius: 0 5px 5px 0;
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background-color: var(--vea-theme, #409eff);
    border-color: var(--vea-theme, #409eff);
    color: #ffffff;
  }
}

.search-box-wrap {
  flex: 1;
  min-width: 220px;
}

.date-range-wrap {
  width: 245px;
  flex-shrink: 0;

  :deep(.el-date-editor) {
    width: 100% !important;
    min-width: 0 !important;
    box-sizing: border-box;
  }
}

.date-range-picker {
  width: 100% !important;
}

.btn-reset {
  flex-shrink: 0;
  white-space: nowrap;
}

/* Batch Actions Bar */
.batch-actions-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 18px;
  background: #eff6ff;
  border-bottom: 1px solid #bfdbfe;
}

.batch-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #1e3a8a;
}

.batch-count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: #1d4ed8;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}

.batch-clear-link {
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  padding: 2px 4px;
  text-decoration: underline;

  &:hover {
    color: #1e40af;
  }
}

.batch-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* Table */
.table-wrapper {
  background: #ffffff;
}

.orders-table {
  :deep(th.el-table__cell) {
    background: #f8fafc !important;
    color: #515a6e;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.01em;
    padding: 10px 0;
  }

  :deep(td.el-table__cell) {
    padding: 9px 0;
    vertical-align: middle;
  }

  :deep(.el-table__row) {
    cursor: pointer;
    transition: background-color 0.14s ease;

    &:hover > td.el-table__cell {
      background-color: #f4f9ff !important;
    }
  }
}

/* Col 1: Order Code & Time (2-line compact) */
.col-order-code {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.order-code-head {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.order-code-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid var(--vea-theme-light-7, #c6e2ff);
  background: var(--vea-theme-light-9, #ecf5ff);
  color: var(--vea-theme, #409eff);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;

  &:hover {
    background: var(--vea-theme, #409eff);
    border-color: var(--vea-theme, #409eff);
    color: #ffffff;
  }
}

.channel-pill {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10.5px;
  font-weight: 700;
  white-space: nowrap;

  &.channel-pos {
    background: #e7faf0;
    color: #13ce66;
    border: 1px solid #d0f5e0;
  }

  &.channel-online {
    background: var(--vea-theme-light-9, #ecf5ff);
    color: var(--vea-theme, #409eff);
    border: 1px solid var(--vea-theme-light-7, #c6e2ff);
  }
}

.order-time-text {
  font-size: 11.5px;
  color: #8c95a6;
  font-variant-numeric: tabular-nums;
}

/* Col 2: Customer (2-line crisp) */
.col-customer {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.customer-head-line {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.customer-name {
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.customer-sub-line {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  font-size: 12px;
  color: #64748b;
}

.phone-text {
  font-variant-numeric: tabular-nums;
  color: #475569;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.sub-dot {
  color: #cbd5e1;
  flex-shrink: 0;
}

.copy-phone-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  padding: 0;
  flex-shrink: 0;
  transition: all 0.15s ease;

  &:hover {
    background: var(--vea-theme-light-9, #ecf5ff);
    border-color: var(--vea-theme-light-7, #c6e2ff);
    color: var(--vea-theme, #409eff);
  }
}

.customer-address {
  font-size: 11.5px;
  color: #8c95a6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.pos-staff-note {
  font-size: 11.5px;
  color: #8c95a6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Col 3: Products Quick Glance */
.col-products {
  display: flex;
  flex-direction: column;
}

.primary-product-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-thumb-box {
  width: 38px;
  height: 38px;
  border-radius: 5px;
  border: 1px solid #e6ebf5;
  background: #f8fafc;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumb-placeholder {
  font-size: 10.5px;
  font-weight: 800;
  color: #94a3b8;
}

.product-meta {
  min-width: 0;
  flex: 1;
}

.product-name {
  font-size: 12.5px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-variant-line {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
  font-size: 11.5px;
  color: #64748b;
  margin-top: 2px;
  min-width: 0;
}

.color-trunc {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 90px;
}

.dot-sep {
  color: #cbd5e1;
}

.qty-badge {
  margin-left: 2px;
  padding: 0 5px;
  border-radius: 3px;
  background: #f1f5f9;
  color: #303133;
  font-weight: 700;
  font-size: 11px;
  flex-shrink: 0;
}

.more-items-pill {
  display: inline-flex;
  align-items: center;
  margin-left: 4px;
  padding: 0 6px;
  border-radius: 3px;
  background: var(--vea-theme-light-9, #ecf5ff);
  border: 1px solid var(--vea-theme-light-7, #c6e2ff);
  color: var(--vea-theme, #409eff);
  font-size: 10.5px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;

  &:hover {
    background: var(--vea-theme, #409eff);
    color: #ffffff;
  }
}

.muted-empty {
  font-size: 12px;
  color: #94a3b8;
  font-style: italic;
}

/* Popover List */
.popover-items-header {
  font-size: 12.5px;
  font-weight: 700;
  color: #0f172a;
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.popover-items-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 280px;
  overflow-y: auto;
}

.popover-item-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.popover-thumb-box {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.popover-item-info {
  flex: 1;
  min-width: 0;
}

.popover-item-name {
  font-size: 12.5px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.popover-item-sub {
  font-size: 11.5px;
  color: #64748b;
}

.popover-item-price {
  font-size: 12.5px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
}

/* Col 4: Amount & Payment (2-line inline badges) */
.col-payment {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.order-total-amount {
  font-size: 14px;
  font-weight: 800;
  color: #dc2626;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.payment-badges-inline {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  flex-wrap: nowrap;
}

.pay-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 7px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;

  &.pay-bank {
    background: #eff6ff;
    color: #1d4ed8;
    border: 1px solid #bfdbfe;
  }

  &.pay-cod {
    background: #f0f9ff;
    color: #0369a1;
    border: 1px solid #bae6fd;
  }

  &.pay-cash {
    background: #fefce8;
    color: #854d0e;
    border: 1px solid #fde68a;
  }
}

.pay-status-badge {
  display: inline-flex;
  align-items: center;
  padding: 1px 7px;
  border-radius: 4px;
  font-size: 10.5px;
  font-weight: 700;

  &.status-sepay-paid {
    background: #059669;
    color: #ffffff;
  }

  &.status-cod-paid {
    background: #ecfdf5;
    color: #047857;
    border: 1px solid #a7f3d0;
  }

  &.status-unpaid {
    background: #fff7ed;
    color: #c2410c;
    border: 1px solid #fed7aa;
  }

  &.status-muted {
    background: #f1f5f9;
    color: #64748b;
  }
}

/* Col 5: Status Pill */
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;

  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: currentColor;
  }

  &.status-pill-pending,
  &.status-pill-draft {
    background: #fff7ed;
    color: #c2410c;
    border: 1px solid #fdba74;
  }

  &.status-pill-confirmed {
    background: #eff6ff;
    color: #1d4ed8;
    border: 1px solid #93c5fd;
  }

  &.status-pill-shipping {
    background: #fefce8;
    color: #a16207;
    border: 1px solid #fde047;
  }

  &.status-pill-completed {
    background: #ecfdf5;
    color: #047857;
    border: 1px solid #6ee7b7;
  }

  &.status-pill-cancelled {
    background: #fef2f2;
    color: #b91c1c;
    border: 1px solid #fca5a5;
  }
}

/* Col 6: Quick Actions — Right-pinned vertical icon rail */
.orders-action-rail {
  width: 100%;
  justify-content: flex-end !important;
  padding-right: 4px;
}

.action-rail-sep {
  width: 1px;
  height: 18px;
  background: #e6ebf5;
  margin: 0 2px;
  flex-shrink: 0;
}

.action-icon-rail {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
}

:deep(.dropdown-danger-item) {
  color: #dc2626 !important;
}

/* Pagination */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 18px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
}

.pagination-meta {
  font-size: 13px;
  color: #64748b;
}

/* Drawer */
.drawer-header-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.drawer-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.drawer-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 17px;
  font-weight: 800;
  color: #0f172a;
}

.drawer-sub-line {
  font-size: 12.5px;
  color: #64748b;
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.drawer-section {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 14px;
  background: #ffffff;
}

.drawer-section-title {
  font-size: 12.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #475569;
  margin-bottom: 10px;
}

.drawer-info-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}

.info-row {
  display: flex;
  gap: 8px;

  .info-label {
    width: 105px;
    flex-shrink: 0;
    color: #64748b;
  }

  .info-value {
    flex: 1;
    color: #0f172a;
  }

  .font-bold {
    font-weight: 700;
  }
}

.note-box {
  background: #f8fafc;
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
  font-size: 12.5px;
}

.drawer-products-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.drawer-product-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #e2e8f0;

  &:last-child {
    padding-bottom: 0;
    border-bottom: none;
  }
}

.drawer-product-info {
  flex: 1;
  min-width: 0;
}

.drawer-product-right {
  text-align: right;
  flex-shrink: 0;
}

.drawer-qty {
  font-size: 12px;
  color: #64748b;
}

.drawer-subtotal {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.drawer-total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1.5px solid #e2e8f0;
  font-size: 14px;

  strong {
    font-size: 17px;
    color: #dc2626;
  }
}

.drawer-timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.timeline-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--vea-theme, #409eff);
  margin-top: 5px;
  flex-shrink: 0;
}

.timeline-content {
  flex: 1;
  font-size: 12.5px;
}

.timeline-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: #0f172a;

  span {
    color: #64748b;
    font-size: 11.5px;
  }
}

.timeline-note {
  color: #475569;
  margin-top: 2px;
}

.drawer-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* Transitions */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.2s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 768px) {
  .workspace-header {
    flex-direction: column;
  }

  .date-range-wrap {
    width: 100%;
  }

  .search-box-wrap {
    width: 100%;
    min-width: 100%;
  }
}
</style>
