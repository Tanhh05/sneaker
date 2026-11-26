<template>
  <div class="page-container vouchers-page">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">Quản lý Voucher &amp; Khuyến Mãi Tự Động</h2>
        <p class="page-subtitle">Định nghĩa mã giảm giá đa kênh (POS &amp; Online), cấu hình quy tắc tặng thưởng động và theo dõi nhật ký sử dụng</p>
      </div>
      <div class="header-actions" v-if="canManage">
        <el-button v-if="activeTab === 'vouchers'" type="primary" @click="openCreateVoucher">
          <el-icon style="margin-right: 6px"><Plus /></el-icon> Tạo Voucher mới
        </el-button>
        <template v-else-if="activeTab === 'rules'">
          <el-button plain :loading="scanningExpiry" @click="handleRunExpiryScan">
            <el-icon style="margin-right: 6px"><Bell /></el-icon> Quét nhắc hết hạn ngay
          </el-button>
          <el-button type="primary" @click="openCreateRule">
            <el-icon style="margin-right: 6px"><Plus /></el-icon> Thêm quy tắc thưởng
          </el-button>
        </template>
      </div>
    </div>

    <div class="table-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- TAB 1: VOUCHER MASTER DATA -->
        <el-tab-pane label="Danh sách Voucher" name="vouchers">
          <div class="filter-bar">
            <el-row :gutter="10">
              <el-col :xs="24" :sm="8" :md="5">
                <el-input
                  v-model="vFilters.keyword"
                  placeholder="Tìm mã hoặc tên voucher..."
                  clearable
                  @keyup.enter="loadVouchers"
                />
              </el-col>
              <el-col :xs="12" :sm="4" :md="3">
                <el-select v-model="vFilters.channel" placeholder="Kênh áp dụng" clearable style="width: 100%" @change="loadVouchers">
                  <el-option label="Tất cả kênh" value="ALL" />
                  <el-option label="Tại quầy (POS)" value="POS" />
                  <el-option label="Trực tuyến (Online)" value="ONLINE" />
                </el-select>
              </el-col>
              <el-col :xs="12" :sm="4" :md="3">
                <el-select v-model="vFilters.discountType" placeholder="Hình thức giảm" clearable style="width: 100%" @change="loadVouchers">
                  <el-option label="Giảm theo %" value="PERCENT" />
                  <el-option label="Giảm cố định" value="FIXED" />
                </el-select>
              </el-col>
              <el-col :xs="12" :sm="4" :md="3">
                <el-select v-model="vFilters.voucherType" placeholder="Loại voucher" clearable style="width: 100%" @change="loadVouchers">
                  <el-option label="Công khai (Public)" value="PUBLIC" />
                  <el-option label="Cá nhân (Private)" value="PRIVATE" />
                </el-select>
              </el-col>
              <el-col :xs="12" :sm="4" :md="3">
                <el-select v-model="vFilters.status" placeholder="Trạng thái" clearable style="width: 100%" @change="loadVouchers">
                  <el-option label="Hoạt động" value="ACTIVE" />
                  <el-option label="Tạm ngưng" value="INACTIVE" />
                  <el-option label="Hết hạn" value="EXPIRED" />
                </el-select>
              </el-col>
              <el-col :xs="24" :sm="8" :md="5">
                <el-date-picker
                  v-model="vFilters.dateRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  start-placeholder="Từ ngày"
                  end-placeholder="Đến ngày"
                  style="width: 100%"
                  @change="loadVouchers"
                />
              </el-col>
              <el-col :xs="24" :sm="4" :md="2">
                <el-button @click="resetVoucherFilters" style="width: 100%">Làm mới</el-button>
              </el-col>
            </el-row>
          </div>

          <el-table :data="vouchers" v-loading="loadingVouchers" stripe border style="width: 100%">
            <template #empty>
              <el-empty description="Chưa có voucher nào phù hợp" :image-size="80" />
            </template>
            <el-table-column label="Mã Voucher" width="145">
              <template #default="{ row }">
                <span class="code-badge">{{ row.code }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Tên chương trình" min-width="200">
              <template #default="{ row }">
                <div class="v-name-cell">
                  <strong>{{ row.name }}</strong>
                  <span v-if="row.description" class="v-desc">{{ row.description }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Mức giảm" width="165">
              <template #default="{ row }">
                <div class="discount-cell">
                  <strong class="discount-val">
                    {{ row.discountType === 'PERCENT' ? `Giảm ${row.discountValue}%` : `Giảm ${formatCurrency(row.discountValue)}` }}
                  </strong>
                  <span v-if="row.discountType === 'PERCENT' && row.maxDiscountAmount" class="discount-cap">
                    Tối đa {{ formatCurrency(row.maxDiscountAmount) }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Đơn tối thiểu" width="125" align="right">
              <template #default="{ row }">
                {{ formatCurrency(row.minOrderAmount || 0) }}
              </template>
            </el-table-column>
            <el-table-column label="Kênh / Loại" width="145" align="center">
              <template #default="{ row }">
                <div class="tag-stack">
                  <el-tag size="small" :type="row.channel === 'POS' ? 'warning' : row.channel === 'ONLINE' ? 'primary' : 'info'" effect="plain">
                    {{ channelLabel(row.channel) }}
                  </el-tag>
                  <el-tag size="small" :type="row.voucherType === 'PRIVATE' ? 'danger' : 'success'" effect="light">
                    {{ row.voucherType === 'PRIVATE' ? 'Cá nhân' : 'Công khai' }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Lượt dùng" width="125" align="center">
              <template #default="{ row }">
                <div><strong>{{ row.usedCount || 0 }}</strong> / {{ row.totalQuantity ?? '∞' }}</div>
                <div class="sub-hint">{{ row.perCustomerLimit }} lần/khách</div>
              </template>
            </el-table-column>
            <el-table-column label="Hiệu lực" width="170">
              <template #default="{ row }">
                <div class="date-cell">
                  <span>Từ: {{ formatDateTime(row.startDate) }}</span>
                  <span>Đến: {{ formatDateTime(row.endDate) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Trạng thái" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.effectiveStatus)" size="small">
                  {{ statusLabel(row.effectiveStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="canManage" label="Thao tác" width="130" fixed="right" align="center" class-name="small-padding fixed-width">
              <template #default="{ row }">
                <div class="vea-table-actions">
                  <el-tooltip content="Chỉnh sửa voucher" placement="top" :show-after="200">
                    <el-button size="small" class="vea-icon-btn vea-icon-primary" aria-label="Sửa voucher" @click="openEditVoucher(row)">
                      <el-icon><Edit /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip :content="row.status === 'ACTIVE' ? 'Tạm ngưng voucher' : 'Kích hoạt voucher'" placement="top" :show-after="200">
                    <el-button
                      size="small"
                      :class="['vea-icon-btn', row.status === 'ACTIVE' ? 'vea-icon-warning' : 'vea-icon-success']"
                      :aria-label="row.status === 'ACTIVE' ? 'Ngưng voucher' : 'Kích hoạt voucher'"
                      @click="toggleVoucherStatus(row)"
                    >
                      <el-icon>
                        <CircleClose v-if="row.status === 'ACTIVE'" />
                        <CircleCheck v-else />
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip v-if="authStore.isAdmin" content="Xóa mềm voucher" placement="top" :show-after="200">
                    <el-button
                      size="small"
                      class="vea-icon-btn vea-icon-danger"
                      aria-label="Xóa mềm voucher"
                      @click="handleSoftDeleteVoucher(row)"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="vPagination.page"
              v-model:page-size="vPagination.size"
              :total="vPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadVouchers"
              @current-change="loadVouchers"
            />
          </div>
        </el-tab-pane>

        <!-- TAB 2: DYNAMIC TRIGGER RULES -->
        <el-tab-pane label="Quy tắc Tặng Thưởng Động" name="rules">
          <div class="rule-intro-banner">
            <strong>Cơ chế tự động:</strong> Khi đơn hàng chuyển sang trạng thái <strong>Hoàn thành (COMPLETED)</strong>, hệ thống tự động đối chiếu toàn bộ quy tắc đang kích hoạt để cấp voucher vào <em>Kho Voucher cá nhân</em> của khách hàng và gửi Email chúc mừng. Tác vụ quét định kỳ chạy lúc 08:00 mỗi ngày để nhắc nhở voucher sắp hết hạn (trước 3 ngày và 1 ngày).
          </div>

          <el-table :data="rules" v-loading="loadingRules" stripe border style="width: 100%">
            <template #empty>
              <el-empty description="Chưa có quy tắc thưởng động nào" :image-size="80" />
            </template>
            <el-table-column label="Tên quy tắc" min-width="210">
              <template #default="{ row }">
                <div class="v-name-cell">
                  <strong>{{ row.ruleName }}</strong>
                  <span v-if="row.description" class="v-desc">{{ row.description }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Điều kiện kích hoạt" width="210">
              <template #default="{ row }">
                <el-tag type="info" effect="plain">{{ triggerTypeLabel(row.triggerType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Ngưỡng đạt mốc" width="150" align="right">
              <template #default="{ row }">
                <strong>
                  {{ row.triggerType === 'ORDER_COUNT' ? `${Number(row.thresholdValue)} đơn hàng` : formatCurrency(row.thresholdValue) }}
                </strong>
              </template>
            </el-table-column>
            <el-table-column label="Voucher phần thưởng" min-width="200">
              <template #default="{ row }">
                <div class="v-name-cell">
                  <div>
                    <span class="code-badge">{{ row.voucherCode }}</span>
                    <span style="margin-left: 6px; font-weight: 600">{{ row.voucherName }}</span>
                  </div>
                  <span class="v-desc">{{ row.voucherDiscountSummary }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Giới hạn / Hạn dùng" width="155" align="center">
              <template #default="{ row }">
                <div>Tối đa <strong>{{ row.maxAwardsPerCustomer }}</strong> lần/KH</div>
                <div class="sub-hint">Hiệu lực {{ row.validityDays }} ngày</div>
              </template>
            </el-table-column>
            <el-table-column label="Trạng thái" width="115" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? 'Đang chạy' : 'Tạm ngưng' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="canManage" label="Thao tác" width="130" fixed="right" align="center" class-name="small-padding fixed-width">
              <template #default="{ row }">
                <div class="vea-table-actions">
                  <el-tooltip content="Chỉnh sửa quy tắc" placement="top" :show-after="200">
                    <el-button size="small" class="vea-icon-btn vea-icon-primary" aria-label="Sửa quy tắc" @click="openEditRule(row)">
                      <el-icon><Edit /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip :content="row.status === 'ACTIVE' ? 'Tạm ngưng quy tắc' : 'Bật quy tắc'" placement="top" :show-after="200">
                    <el-button
                      size="small"
                      :class="['vea-icon-btn', row.status === 'ACTIVE' ? 'vea-icon-warning' : 'vea-icon-success']"
                      :aria-label="row.status === 'ACTIVE' ? 'Ngưng quy tắc' : 'Bật quy tắc'"
                      @click="toggleRuleStatus(row)"
                    >
                      <el-icon>
                        <CircleClose v-if="row.status === 'ACTIVE'" />
                        <CircleCheck v-else />
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip v-if="authStore.isAdmin" content="Xóa mềm quy tắc" placement="top" :show-after="200">
                    <el-button
                      size="small"
                      class="vea-icon-btn vea-icon-danger"
                      aria-label="Xóa mềm quy tắc"
                      @click="handleSoftDeleteRule(row)"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- TAB 3: VOUCHER USAGE AUDIT LOG -->
        <el-tab-pane label="Nhật ký Sử dụng Voucher" name="usages">
          <div class="filter-bar">
            <el-row :gutter="10">
              <el-col :xs="24" :sm="10" :md="8">
                <el-input
                  v-model="uFilters.keyword"
                  placeholder="Tìm mã voucher, mã đơn (HDxxxx), tên/SĐT khách..."
                  clearable
                  @keyup.enter="loadUsages"
                />
              </el-col>
              <el-col :xs="12" :sm="5" :md="4">
                <el-select v-model="uFilters.channel" placeholder="Kênh bán" clearable style="width: 100%" @change="loadUsages">
                  <el-option label="Tại quầy (POS)" value="POS" />
                  <el-option label="Trực tuyến (Online)" value="ONLINE" />
                </el-select>
              </el-col>
              <el-col :xs="12" :sm="5" :md="4">
                <el-select v-model="uFilters.status" placeholder="Trạng thái" clearable style="width: 100%" @change="loadUsages">
                  <el-option label="Đã áp dụng" value="APPLIED" />
                  <el-option label="Đã hoàn lượt (Hủy đơn)" value="REFUNDED" />
                </el-select>
              </el-col>
              <el-col :xs="24" :sm="4" :md="4">
                <el-button type="primary" @click="loadUsages">Lọc</el-button>
                <el-button @click="resetUsageFilters">Làm mới</el-button>
              </el-col>
            </el-row>
          </div>

          <el-table :data="usages" v-loading="loadingUsages" stripe border style="width: 100%">
            <template #empty>
              <el-empty description="Chưa có lịch sử sử dụng voucher nào" :image-size="80" />
            </template>
            <el-table-column label="Thời gian" width="155">
              <template #default="{ row }">{{ formatDateTime(row.usedAt) }}</template>
            </el-table-column>
            <el-table-column label="Mã Voucher" width="160">
              <template #default="{ row }">
                <span class="code-badge">{{ row.voucherCode }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Đơn hàng" width="125">
              <template #default="{ row }">
                <router-link v-if="row.orderId" :to="`/admin/orders/${row.orderId}`" class="order-link">
                  {{ row.orderCode }}
                </router-link>
                <span v-else>{{ row.orderCode }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Khách hàng" min-width="170">
              <template #default="{ row }">
                <div><strong>{{ row.customerName }}</strong></div>
                <div class="sub-hint">{{ row.customerPhone }}</div>
              </template>
            </el-table-column>
            <el-table-column label="Kênh" width="95" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.channel === 'POS' ? 'warning' : 'primary'" effect="plain">
                  {{ row.channel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Giá trị đơn" width="130" align="right">
              <template #default="{ row }">{{ formatCurrency(row.orderSubtotal) }}</template>
            </el-table-column>
            <el-table-column label="Số tiền giảm" width="130" align="right">
              <template #default="{ row }">
                <strong style="color: #dc2626">-{{ formatCurrency(row.discountAmount) }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="Trạng thái" width="135" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'APPLIED' ? 'success' : 'warning'" size="small">
                  {{ row.status === 'APPLIED' ? 'Đã áp dụng' : 'Đã hoàn lượt' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="note" label="Ghi chú" min-width="180" show-overflow-tooltip />
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="uPagination.page"
              v-model:page-size="uPagination.size"
              :total="uPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadUsages"
              @current-change="loadUsages"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- MODAL CREATE / EDIT VOUCHER -->
    <el-dialog
      v-model="voucherDialogVisible"
      :title="isEditVoucher ? `Chỉnh sửa Voucher (${voucherForm.code})` : 'Tạo Voucher Khuyến Mãi Mới'"
      width="640px"
    >
      <el-form ref="voucherFormRef" :model="voucherForm" :rules="voucherRules" label-position="top">
        <el-row :gutter="14">
          <el-col :span="10">
            <el-form-item label="Mã Voucher (*)" prop="code">
              <el-input v-model="voucherForm.code" placeholder="VD: SNEAKER10, VIP500K" @input="voucherForm.code = voucherForm.code.toUpperCase()" />
            </el-form-item>
          </el-col>
          <el-col :span="14">
            <el-form-item label="Tên chương trình (*)" prop="name">
              <el-input v-model="voucherForm.name" placeholder="VD: Giảm 10% mừng sinh nhật" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Mô tả điều kiện">
          <el-input v-model="voucherForm.description" type="textarea" :rows="2" placeholder="Mô tả ngắn hiển thị cho khách hàng..." />
        </el-form-item>

        <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="Hình thức giảm (*)" prop="discountType">
              <el-select v-model="voucherForm.discountType" style="width: 100%">
                <el-option label="Theo phần trăm (%)" value="PERCENT" />
                <el-option label="Số tiền cố định (VNĐ)" value="FIXED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="voucherForm.discountType === 'PERCENT' ? 'Mức giảm (%) (*)' : 'Số tiền giảm (VNĐ) (*)'" prop="discountValue">
              <el-input-number
                v-model="voucherForm.discountValue"
                :min="1"
                :max="voucherForm.discountType === 'PERCENT' ? 100 : 100000000"
                :step="voucherForm.discountType === 'PERCENT' ? 5 : 10000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Giảm tối đa (VNĐ)">
              <el-input-number
                v-model="voucherForm.maxDiscountAmount"
                :disabled="voucherForm.discountType !== 'PERCENT'"
                :min="0"
                :step="10000"
                placeholder="Không giới hạn"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="Đơn tối thiểu (VNĐ)">
              <el-input-number v-model="voucherForm.minOrderAmount" :min="0" :step="50000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Tổng lượt phát hành (trống = ∞)">
              <el-input-number v-model="voucherForm.totalQuantity" :min="1" placeholder="Không giới hạn" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Lượt dùng / 1 khách (*)" prop="perCustomerLimit">
              <el-input-number v-model="voucherForm.perCustomerLimit" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="Kênh áp dụng (*)" prop="channel">
              <el-select v-model="voucherForm.channel" style="width: 100%">
                <el-option label="Tất cả kênh (ALL)" value="ALL" />
                <el-option label="Chỉ tại quầy (POS)" value="POS" />
                <el-option label="Chỉ Online" value="ONLINE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Loại Voucher (*)" prop="voucherType">
              <el-select v-model="voucherForm.voucherType" style="width: 100%">
                <el-option label="Công khai (PUBLIC)" value="PUBLIC" />
                <el-option label="Thưởng cá nhân (PRIVATE)" value="PRIVATE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Trạng thái">
              <el-select v-model="voucherForm.status" style="width: 100%">
                <el-option label="ACTIVE" value="ACTIVE" />
                <el-option label="INACTIVE" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="Ngày bắt đầu hiệu lực (*)" prop="startDate">
              <el-date-picker
                v-model="voucherForm.startDate"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="Chọn thời điểm bắt đầu"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Ngày kết thúc hiệu lực (*)" prop="endDate">
              <el-date-picker
                v-model="voucherForm.endDate"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="Chọn thời điểm kết thúc"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="voucherDialogVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="savingVoucher" @click="submitVoucherForm">Lưu Voucher</el-button>
      </template>
    </el-dialog>

    <!-- MODAL CREATE / EDIT TRIGGER RULE -->
    <el-dialog
      v-model="ruleDialogVisible"
      :title="isEditRule ? 'Chỉnh sửa Quy tắc Thưởng Động' : 'Tạo Quy tắc Tự động Tặng Voucher'"
      width="580px"
    >
      <el-form ref="ruleFormRef" :model="ruleForm" :rules="ruleFormRules" label-position="top">
        <el-form-item label="Tên quy tắc (*)" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" placeholder="VD: Khách đạt mốc 3 đơn tặng voucher 10%" />
        </el-form-item>
        <el-form-item label="Mô tả">
          <el-input v-model="ruleForm.description" type="textarea" :rows="2" placeholder="Ghi chú kịch bản áp dụng..." />
        </el-form-item>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="Loại điều kiện kích hoạt (*)" prop="triggerType">
              <el-select v-model="ruleForm.triggerType" style="width: 100%">
                <el-option label="Số đơn hàng tích lũy (ORDER_COUNT)" value="ORDER_COUNT" />
                <el-option label="Tổng chi tiêu tích lũy (TOTAL_SPENT)" value="TOTAL_SPENT" />
                <el-option label="Giá trị 1 đơn hàng (SINGLE_ORDER_AMOUNT)" value="SINGLE_ORDER_AMOUNT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="ruleForm.triggerType === 'ORDER_COUNT' ? 'Số đơn hoàn thành tối thiểu (*)' : 'Ngưỡng số tiền (VNĐ) (*)'" prop="thresholdValue">
              <el-input-number
                v-model="ruleForm.thresholdValue"
                :min="1"
                :step="ruleForm.triggerType === 'ORDER_COUNT' ? 1 : 500000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Voucher phần thưởng được cấp (*)" prop="voucherId">
          <el-select v-model="ruleForm.voucherId" filterable placeholder="Chọn voucher phần thưởng" style="width: 100%">
            <el-option
              v-for="v in allVoucherOptions"
              :key="v.id"
              :label="`${v.code} — ${v.name} (${v.voucherType})`"
              :value="v.id"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="Nhận tối đa / khách (*)" prop="maxAwardsPerCustomer">
              <el-input-number v-model="ruleForm.maxAwardsPerCustomer" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Hạn dùng sau khi nhận (ngày)" prop="validityDays">
              <el-input-number v-model="ruleForm.validityDays" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Trạng thái">
              <el-select v-model="ruleForm.status" style="width: 100%">
                <el-option label="ACTIVE" value="ACTIVE" />
                <el-option label="INACTIVE" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="savingRule" @click="submitRuleForm">Lưu quy tắc</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, CircleClose, CircleCheck, Bell } from '@element-plus/icons-vue'
import { voucherService } from '@/services/voucherService'
import { useAuthStore } from '@/stores/auth'
import { formatCurrency, formatDateTime } from '@/utils/formatters'
import { useDebouncedWatch } from '@/composables/useDebounce'

const authStore = useAuthStore()
const canManage = computed(() => authStore.isAdmin || authStore.isManager)

const activeTab = ref('vouchers')

// ==================== TAB 1: VOUCHERS ====================
const loadingVouchers = ref(false)
const vouchers = ref([])
const allVoucherOptions = ref([])
const vFilters = reactive({
  keyword: '',
  channel: '',
  discountType: '',
  voucherType: '',
  status: '',
  dateRange: null
})
const vPagination = reactive({ page: 1, size: 10, total: 0 })

useDebouncedWatch(() => vFilters.keyword, () => {
  vPagination.page = 1
  loadVouchers()
}, 350)

async function loadVouchers() {
  loadingVouchers.value = true
  try {
    const res = await voucherService.searchVouchers({
      keyword: vFilters.keyword || undefined,
      channel: vFilters.channel || undefined,
      discountType: vFilters.discountType || undefined,
      voucherType: vFilters.voucherType || undefined,
      status: vFilters.status || undefined,
      fromDate: vFilters.dateRange?.[0] || undefined,
      toDate: vFilters.dateRange?.[1] || undefined,
      page: vPagination.page - 1,
      size: vPagination.size
    })
    vouchers.value = res.data?.content || []
    vPagination.total = res.data?.totalElements || 0
  } finally {
    loadingVouchers.value = false
  }
}

async function loadAllVoucherOptions() {
  const res = await voucherService.getAllVouchers()
  allVoucherOptions.value = res.data || []
}

function resetVoucherFilters() {
  vFilters.keyword = ''
  vFilters.channel = ''
  vFilters.discountType = ''
  vFilters.voucherType = ''
  vFilters.status = ''
  vFilters.dateRange = null
  vPagination.page = 1
  loadVouchers()
}

// Voucher modal
const voucherDialogVisible = ref(false)
const isEditVoucher = ref(false)
const editingVoucherId = ref(null)
const savingVoucher = ref(false)
const voucherFormRef = ref(null)

const voucherForm = reactive({
  code: '',
  name: '',
  description: '',
  discountType: 'PERCENT',
  discountValue: 10,
  maxDiscountAmount: 150000,
  minOrderAmount: 500000,
  totalQuantity: 100,
  perCustomerLimit: 1,
  channel: 'ALL',
  voucherType: 'PUBLIC',
  startDate: '',
  endDate: '',
  status: 'ACTIVE'
})

const voucherRules = {
  code: [{ required: true, message: 'Vui lòng nhập mã voucher', trigger: 'blur' }],
  name: [{ required: true, message: 'Vui lòng nhập tên voucher', trigger: 'blur' }],
  discountType: [{ required: true, message: 'Chọn hình thức giảm', trigger: 'change' }],
  discountValue: [{ required: true, message: 'Nhập giá trị giảm', trigger: 'blur' }],
  startDate: [{ required: true, message: 'Chọn ngày bắt đầu', trigger: 'change' }],
  endDate: [{ required: true, message: 'Chọn ngày kết thúc', trigger: 'change' }]
}

function toIsoLocal(dt) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}T${pad(dt.getHours())}:${pad(dt.getMinutes())}:00`
}

function openCreateVoucher() {
  isEditVoucher.value = false
  editingVoucherId.value = null
  const now = new Date()
  const end = new Date(now.getTime() + 30 * 24 * 3600 * 1000)
  Object.assign(voucherForm, {
    code: '',
    name: '',
    description: '',
    discountType: 'PERCENT',
    discountValue: 10,
    maxDiscountAmount: 150000,
    minOrderAmount: 500000,
    totalQuantity: 100,
    perCustomerLimit: 1,
    channel: 'ALL',
    voucherType: 'PUBLIC',
    startDate: toIsoLocal(now),
    endDate: toIsoLocal(end),
    status: 'ACTIVE'
  })
  voucherDialogVisible.value = true
}

function openEditVoucher(row) {
  isEditVoucher.value = true
  editingVoucherId.value = row.id
  Object.assign(voucherForm, {
    code: row.code,
    name: row.name,
    description: row.description || '',
    discountType: row.discountType,
    discountValue: Number(row.discountValue),
    maxDiscountAmount: row.maxDiscountAmount != null ? Number(row.maxDiscountAmount) : null,
    minOrderAmount: Number(row.minOrderAmount || 0),
    totalQuantity: row.totalQuantity ?? null,
    perCustomerLimit: row.perCustomerLimit || 1,
    channel: row.channel,
    voucherType: row.voucherType,
    startDate: row.startDate,
    endDate: row.endDate,
    status: row.status
  })
  voucherDialogVisible.value = true
}

async function submitVoucherForm() {
  if (!voucherFormRef.value) return
  await voucherFormRef.value.validate(async (valid) => {
    if (!valid) return
    savingVoucher.value = true
    try {
      const payload = {
        ...voucherForm,
        maxDiscountAmount: voucherForm.discountType === 'PERCENT' ? (voucherForm.maxDiscountAmount || null) : null,
        totalQuantity: voucherForm.totalQuantity || null
      }
      if (isEditVoucher.value) {
        await voucherService.updateVoucher(editingVoucherId.value, payload)
        ElMessage.success('Cập nhật voucher thành công')
      } else {
        await voucherService.createVoucher(payload)
        ElMessage.success('Tạo voucher mới thành công')
      }
      voucherDialogVisible.value = false
      loadVouchers()
      loadAllVoucherOptions()
    } finally {
      savingVoucher.value = false
    }
  })
}

async function toggleVoucherStatus(row) {
  const next = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  await voucherService.toggleVoucherStatus(row.id, next)
  ElMessage.success(`Đã chuyển voucher ${row.code} sang ${next}`)
  loadVouchers()
}

function handleSoftDeleteVoucher(row) {
  ElMessageBox.confirm(
    `Bạn có chắc chắn muốn xóa mềm voucher "${row.code}"? Lịch sử sử dụng trên các đơn cũ vẫn được giữ nguyên.`,
    'Xác nhận xóa mềm',
    { type: 'warning', confirmButtonText: 'Xóa mềm', cancelButtonText: 'Hủy', confirmButtonClass: 'el-button--danger' }
  ).then(async () => {
    await voucherService.softDeleteVoucher(row.id)
    ElMessage.success(`Đã xóa mềm voucher ${row.code}`)
    loadVouchers()
    loadAllVoucherOptions()
  }).catch(() => {})
}

// ==================== TAB 2: TRIGGER RULES ====================
const loadingRules = ref(false)
const rules = ref([])
const ruleDialogVisible = ref(false)
const isEditRule = ref(false)
const editingRuleId = ref(null)
const savingRule = ref(false)
const scanningExpiry = ref(false)
const ruleFormRef = ref(null)

const ruleForm = reactive({
  ruleName: '',
  description: '',
  triggerType: 'ORDER_COUNT',
  thresholdValue: 3,
  voucherId: null,
  maxAwardsPerCustomer: 1,
  validityDays: 30,
  status: 'ACTIVE'
})

const ruleFormRules = {
  ruleName: [{ required: true, message: 'Vui lòng nhập tên quy tắc', trigger: 'blur' }],
  triggerType: [{ required: true, message: 'Chọn loại kích hoạt', trigger: 'change' }],
  thresholdValue: [{ required: true, message: 'Nhập ngưỡng kích hoạt', trigger: 'blur' }],
  voucherId: [{ required: true, message: 'Chọn voucher phần thưởng', trigger: 'change' }]
}

async function loadRules() {
  loadingRules.value = true
  try {
    const res = await voucherService.getRules()
    rules.value = res.data || []
  } finally {
    loadingRules.value = false
  }
}

function openCreateRule() {
  isEditRule.value = false
  editingRuleId.value = null
  Object.assign(ruleForm, {
    ruleName: '',
    description: '',
    triggerType: 'ORDER_COUNT',
    thresholdValue: 3,
    voucherId: allVoucherOptions.value[0]?.id || null,
    maxAwardsPerCustomer: 1,
    validityDays: 30,
    status: 'ACTIVE'
  })
  ruleDialogVisible.value = true
}

function openEditRule(row) {
  isEditRule.value = true
  editingRuleId.value = row.id
  Object.assign(ruleForm, {
    ruleName: row.ruleName,
    description: row.description || '',
    triggerType: row.triggerType,
    thresholdValue: Number(row.thresholdValue),
    voucherId: row.voucherId,
    maxAwardsPerCustomer: row.maxAwardsPerCustomer || 1,
    validityDays: row.validityDays || 30,
    status: row.status
  })
  ruleDialogVisible.value = true
}

async function submitRuleForm() {
  if (!ruleFormRef.value) return
  await ruleFormRef.value.validate(async (valid) => {
    if (!valid) return
    savingRule.value = true
    try {
      if (isEditRule.value) {
        await voucherService.updateRule(editingRuleId.value, ruleForm)
        ElMessage.success('Cập nhật quy tắc thành công')
      } else {
        await voucherService.createRule(ruleForm)
        ElMessage.success('Tạo quy tắc thưởng động thành công')
      }
      ruleDialogVisible.value = false
      loadRules()
    } finally {
      savingRule.value = false
    }
  })
}

async function toggleRuleStatus(row) {
  const next = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  await voucherService.toggleRuleStatus(row.id, next)
  ElMessage.success(`Đã ${next === 'ACTIVE' ? 'bật' : 'tạm ngưng'} quy tắc "${row.ruleName}"`)
  loadRules()
}

function handleSoftDeleteRule(row) {
  ElMessageBox.confirm(
    `Bạn có chắc chắn muốn xóa mềm quy tắc "${row.ruleName}"?`,
    'Xác nhận xóa mềm',
    { type: 'warning', confirmButtonText: 'Xóa mềm', cancelButtonText: 'Hủy', confirmButtonClass: 'el-button--danger' }
  ).then(async () => {
    await voucherService.softDeleteRule(row.id)
    ElMessage.success(`Đã xóa mềm quy tắc "${row.ruleName}"`)
    loadRules()
  }).catch(() => {})
}

async function handleRunExpiryScan() {
  scanningExpiry.value = true
  try {
    const res = await voucherService.triggerExpiryScan()
    const d = res.data || {}
    ElMessage.success(
      `Quét xong: ${d.expiredMarked || 0} mã hết hạn, ${d.expiringCandidates || 0} mã sắp hết hạn (gửi nhắc 3 ngày: ${d.reminded3Days || 0}, 1 ngày: ${d.reminded1Day || 0})`
    )
  } finally {
    scanningExpiry.value = false
  }
}

// ==================== TAB 3: USAGE LOGS ====================
const loadingUsages = ref(false)
const usages = ref([])
const uFilters = reactive({ keyword: '', channel: '', status: '' })
const uPagination = reactive({ page: 1, size: 10, total: 0 })

useDebouncedWatch(() => uFilters.keyword, () => {
  uPagination.page = 1
  loadUsages()
}, 350)

async function loadUsages() {
  loadingUsages.value = true
  try {
    const res = await voucherService.searchUsages({
      keyword: uFilters.keyword || undefined,
      channel: uFilters.channel || undefined,
      status: uFilters.status || undefined,
      page: uPagination.page - 1,
      size: uPagination.size
    })
    usages.value = res.data?.content || []
    uPagination.total = res.data?.totalElements || 0
  } finally {
    loadingUsages.value = false
  }
}

function resetUsageFilters() {
  uFilters.keyword = ''
  uFilters.channel = ''
  uFilters.status = ''
  uPagination.page = 1
  loadUsages()
}

function onTabChange(tab) {
  if (tab === 'rules') loadRules()
  else if (tab === 'usages') loadUsages()
  else loadVouchers()
}

// ==================== FORMATTERS ====================
function channelLabel(c) {
  if (c === 'POS') return 'Tại quầy (POS)'
  if (c === 'ONLINE') return 'Online'
  return 'Tất cả kênh'
}

function statusLabel(s) {
  const map = { ACTIVE: 'Đang chạy', INACTIVE: 'Tạm ngưng', EXPIRED: 'Hết hạn', UPCOMING: 'Sắp diễn ra', DEPLETED: 'Hết lượt' }
  return map[s] || s
}

function statusTagType(s) {
  if (s === 'ACTIVE') return 'success'
  if (s === 'UPCOMING') return 'warning'
  if (s === 'EXPIRED' || s === 'DEPLETED') return 'danger'
  return 'info'
}

function triggerTypeLabel(t) {
  if (t === 'ORDER_COUNT') return 'Tích lũy số đơn hoàn thành'
  if (t === 'TOTAL_SPENT') return 'Tổng chi tiêu tích lũy'
  if (t === 'SINGLE_ORDER_AMOUNT') return 'Giá trị 1 đơn hàng'
  return t
}

onMounted(() => {
  loadVouchers()
  loadAllVoucherOptions()
  loadRules()
})
</script>

<style scoped lang="scss">
.header-actions {
  display: flex;
  gap: 8px;
}

.filter-bar {
  margin-bottom: 14px;
}

.rule-intro-banner {
  background: #fafafa;
  border: 1px solid #e5e5e5;
  border-left: 3px solid var(--vea-theme, #409eff);
  padding: 12px 16px;
  font-size: 13px;
  color: #262626;
  margin-bottom: 16px;
  line-height: 1.5;
}

.v-name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  .v-desc {
    font-size: 11.5px;
    color: #737373;
  }
}

.discount-cell {
  display: flex;
  flex-direction: column;
  .discount-val {
    color: #dc2626;
    font-size: 13.5px;
  }
  .discount-cap {
    font-size: 11px;
    color: #64748b;
  }
}

.tag-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.sub-hint {
  font-size: 11px;
  color: #737373;
}

.date-cell {
  display: flex;
  flex-direction: column;
  font-size: 11.5px;
  color: #404040;
  gap: 2px;
}

.order-link {
  font-weight: 700;
  color: #303133;
  text-decoration: underline;
  &:hover {
    color: #dc2626;
  }
}
</style>
