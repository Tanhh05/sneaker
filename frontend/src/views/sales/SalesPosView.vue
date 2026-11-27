<template>
  <div class="page-container pos-page">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">Bán Hàng Tại Quầy (POS Counter)</h2>
        <p class="page-subtitle">Quét mã vạch SKU trên hộp giày hoặc tìm kiếm mẫu giày, kích cỡ, màu sắc và xuất hóa đơn</p>
      </div>
      <div class="header-action-group">
        <button class="pos-hdr-btn" @click="loadCatalog">
          <el-icon><Refresh /></el-icon>
          <span>Làm mới SKU</span>
        </button>
        <button class="pos-hdr-btn primary" @click="$router.push('/admin/orders')">
          <span>Danh sách đơn hàng &rarr;</span>
        </button>
      </div>
    </div>

    <el-row :gutter="18">
      <!-- LEFT COLUMN: Product & Variant Picker -->
      <el-col :xs="24" :lg="14">
        <div class="table-card pos-catalog-card" v-loading="loading">
          <!-- UNIFIED MASTER SEARCH & BARCODE SCANNER TOOLBAR -->
          <div class="pos-master-toolbar">
            <div class="search-scan-row">
              <el-input
                ref="barcodeInputRef"
                v-model="searchQuery"
                placeholder="Quét mã vạch trên hộp giày (SKU) hoặc gõ tìm tên giày..."
                clearable
                class="master-search-input"
                @keyup.enter="handleScanOrEnter"
              >
                <template #prefix>
                  <el-icon class="search-icon"><Search /></el-icon>
                </template>
              </el-input>

              <button class="btn-scan-trigger" @click="handleScanOrEnter" title="Nhấn để quét mã hoặc thêm nhanh (Enter)">
                <el-icon><Tickets /></el-icon>
                <span>Quét mã (Enter)</span>
              </button>

              <button class="btn-camera-trigger" @click="openCameraScanner" title="Mở camera quét mã vạch">
                <el-icon><Camera /></el-icon>
                <span>Camera</span>
              </button>
            </div>

            <!-- Filter Controls + Inline Quick Step Selector -->
            <div class="sub-controls-row">
              <div class="filter-dropdowns">
                <QuickSelect
                  v-model="selectedSizeFilter"
                  :options="sizes"
                  value-key="id"
                  :label-key="(s) => `Size EU ${s.sizeValue}`"
                  placeholder="Lọc Size EU"
                  search-placeholder="Tìm size..."
                  clearable
                  class="filter-select"
                />
                <QuickSelect
                  v-model="selectedColorFilter"
                  :options="colors"
                  value-key="id"
                  label-key="colorName"
                  :search-keys="['colorName', 'colorCode']"
                  placeholder="Lọc Màu sắc"
                  search-placeholder="Tìm màu sắc..."
                  clearable
                  class="filter-select"
                />
              </div>

              <!-- Compact Step Picker Strip -->
              <div class="quick-step-strip">
                <span class="strip-label">CHỌN NHANH:</span>
                <QuickSelect
                  v-model="stepProductId"
                  :options="activeProducts"
                  value-key="id"
                  :label-key="(p) => `${p.productCode} - ${p.productName}`"
                  :search-keys="['productCode', 'productName']"
                  placeholder="Mẫu giày..."
                  search-placeholder="Tìm mã SP hoặc tên giày..."
                  clearable
                  class="step-select prod"
                  @change="onStepProductChange"
                />
                <QuickSelect
                  v-model="stepSizeId"
                  :options="availableSizesForStepProduct"
                  value-key="sizeId"
                  :label-key="(s) => `EU ${s.sizeValue}`"
                  placeholder="Size"
                  search-placeholder="Tìm size..."
                  :disabled="!stepProductId"
                  clearable
                  class="step-select size"
                  @change="onStepSizeChange"
                />
                <QuickSelect
                  v-model="stepColorId"
                  :options="availableColorsForStep"
                  value-key="colorId"
                  label-key="colorName"
                  placeholder="Màu"
                  search-placeholder="Tìm màu..."
                  :disabled="!stepSizeId"
                  clearable
                  class="step-select color"
                />
                <button
                  class="step-add-btn"
                  :disabled="!matchedStepVariant || matchedStepVariant.availableQuantity <= 0"
                  title="Thêm biến thể đã chọn vào đơn"
                  @click="addVariantToCart(matchedStepVariant)"
                >
                  <el-icon><Plus /></el-icon>
                  <span>Thêm</span>
                </button>
              </div>
            </div>

            <div v-if="matchedStepVariant" class="step-match-alert">
              <span>SKU: <strong>{{ matchedStepVariant.sku }}</strong></span>
              <span class="divider">/</span>
              <span>Giá: <strong>{{ formatCurrency(matchedStepVariant.salePrice) }}</strong></span>
              <span class="divider">/</span>
              <span>Sẵn có: <strong class="stock-num">{{ matchedStepVariant.availableQuantity }} đôi</strong></span>
            </div>
          </div>

          <!-- Variant SKU Grid / Table -->
          <el-table :data="paginatedVariants" stripe border style="width: 100%" class="pos-table">
            <template #empty>
              <el-empty description="Không tìm thấy biến thể SKU phù hợp" :image-size="72" />
            </template>
            <el-table-column label="Mã SKU" width="120">
              <template #default="{ row }">
                <span class="code-badge">{{ row.sku }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Sản phẩm Sneaker" min-width="170">
              <template #default="{ row }">
                <div class="pos-prod-cell">
                  <img
                    v-if="row.thumbnailUrl"
                    :src="row.thumbnailUrl"
                    alt="sneaker"
                    class="pos-prod-thumb"
                  />
                  <div v-else class="pos-prod-thumb placeholder">SNK</div>
                  <div class="pos-prod-meta">
                    <span class="pos-prod-title">{{ row.productName }}</span>
                    <span class="pos-prod-sub">{{ row.productCode }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Size / Màu" width="155">
              <template #default="{ row }">
                <div class="pos-attr-cell">
                  <span class="size-pill">{{ row.sizeValue }}</span>
                  <span class="color-dot" :style="{ backgroundColor: row.hexCode || '#000000' }"></span>
                  <span class="color-text" :title="row.colorName">{{ row.colorName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Đơn giá" width="115" align="right">
              <template #default="{ row }">
                <span class="price-text">{{ formatCurrency(row.salePrice) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Sẵn có" width="85" align="center">
              <template #default="{ row }">
                <span :class="['stock-pill', row.availableQuantity > 0 ? 'in-stock' : 'out-stock']">
                  {{ row.availableQuantity }} đôi
                </span>
              </template>
            </el-table-column>
            <el-table-column label="Thao tác" width="128" align="center" class-name="small-padding fixed-width">
              <template #default="{ row }">
                <div class="vea-table-actions">
                  <el-tooltip content="In tem mã vạch SKU" placement="top" :show-after="200">
                    <el-button size="small" class="vea-icon-btn vea-icon-info" aria-label="In tem mã vạch" @click="printSingleVariant(row)">
                      <el-icon><Printer /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-button
                    type="primary"
                    size="small"
                    class="vea-cta-btn"
                    :disabled="row.availableQuantity <= 0"
                    @click="addVariantToCart(row)"
                  >
                    <el-icon><Plus /></el-icon>
                    <span>Thêm</span>
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- Pagination Bar -->
          <div class="pos-pagination-bar">
            <span class="pos-pagination-info">
              Hiển thị <strong>{{ paginatedVariants.length }}</strong> / <strong>{{ filteredVariants.length }}</strong> biến thể
            </span>
            <el-pagination
              v-model:current-page="posPagination.page"
              :page-size="posPagination.size"
              :total="filteredVariants.length"
              layout="prev, pager, next"
              small
            />
          </div>
        </div>
      </el-col>

      <!-- RIGHT COLUMN: Current Order Cart & Checkout -->
      <el-col :xs="24" :lg="10">
        <div class="table-card pos-cart-card">
          <div class="cart-header">
            <div class="cart-header-title">
              <h3>Đơn hàng hiện tại</h3>
              <span class="cart-count-badge">{{ totalCartQty }} đôi</span>
            </div>
            <button
              v-if="cart.length > 0"
              class="cart-clear-btn"
              @click="cart = []"
            >
              Xóa giỏ
            </button>
          </div>

          <!-- Scrollable Middle Body: Customer + Cart Items (never squished!) + Checkout Options -->
          <div class="pos-cart-scroll-body">
            <!-- Customer Selection + Quick Add Customer -->
            <div class="customer-box">
              <div class="box-label">Khách hàng mua tại quầy:</div>
              <div class="cust-row">
                <QuickSelect
                  v-model="checkoutForm.customerId"
                  :options="customers"
                  value-key="id"
                  :label-key="(c) => `${c.fullName} - ${c.phone} (${c.customerCode})`"
                  :search-keys="['fullName', 'phone', 'customerCode']"
                  clearable
                  placeholder="Khách lẻ tại quầy (Hoặc tìm theo tên/SĐT)"
                  search-placeholder="Tìm nhanh tên, SĐT hoặc mã KH..."
                  style="flex: 1"
                />
                <button class="pos-cust-btn" @click="quickCustVisible = true">
                  + Khách mới
                </button>
              </div>
            </div>

            <!-- Cart Items -->
            <div v-if="cart.length === 0" class="empty-cart-state">
              <div class="empty-box-icon">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#a1a1aa" stroke-width="1.5">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                  <line x1="3" y1="6" x2="21" y2="6"></line>
                  <path d="M16 10a4 4 0 0 1-8 0"></path>
                </svg>
              </div>
              <div class="empty-title">CHƯA CÓ SẢN PHẨM TRONG ĐƠN</div>
              <div class="empty-hint">Quét mã vạch hộp giày hoặc chọn từ danh mục bên trái</div>
            </div>
            <div v-else class="cart-list">
              <div v-for="(item, index) in cart" :key="item.variant.id" class="cart-item">
                <div class="item-info">
                  <div class="item-title">{{ item.variant.productName }}</div>
                  <div class="item-meta">
                    <span class="code-badge">{{ item.variant.sku }}</span>
                    <span class="attr-text">Size {{ item.variant.sizeValue }} · {{ item.variant.colorName }}</span>
                    <span class="avail">(Còn: {{ item.variant.availableQuantity }})</span>
                  </div>
                  <div class="item-price">
                    {{ formatCurrency(item.variant.salePrice) }} &times; {{ item.quantity }} =
                    <strong class="price-text">{{ formatCurrency(item.variant.salePrice * item.quantity) }}</strong>
                  </div>
                </div>
                <div class="item-actions">
                  <el-input-number
                    v-model="item.quantity"
                    :min="1"
                    :max="item.variant.availableQuantity"
                    size="small"
                    style="width: 92px"
                  />
                  <button
                    class="cart-delete-btn"
                    title="Xóa khỏi đơn"
                    @click="cart.splice(index, 1)"
                  >
                    <el-icon><Delete /></el-icon>
                  </button>
                </div>
              </div>
            </div>

            <!-- Payment, Note, Voucher & VAT Form -->
            <div class="checkout-section">
              <el-form label-position="top" size="small" class="pos-checkout-form">
                <el-row :gutter="10">
                  <el-col :span="12">
                    <el-form-item label="PHƯƠNG THỨC THANH TOÁN">
                      <el-radio-group v-model="checkoutForm.paymentMethod" class="pos-radio-group">
                        <el-radio-button label="CASH">Tiền mặt</el-radio-button>
                        <el-radio-button label="BANK_TRANSFER">Chuyển khoản</el-radio-button>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="TRẠNG THÁI XÁC NHẬN">
                      <el-select filterable v-model="checkoutForm.status" style="width: 100%">
                        <el-option label="Hoàn thành ngay (COMPLETED)" value="COMPLETED" />
                        <el-option label="Xác nhận giữ đơn (CONFIRMED)" value="CONFIRMED" />
                        <el-option label="Lưu đơn nháp (DRAFT)" value="DRAFT" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item
                  v-if="checkoutForm.paymentMethod === 'BANK_TRANSFER'"
                  label="MÃ GIAO DỊCH CHUYỂN KHOẢN / NGÂN HÀNG"
                >
                  <el-input v-model="checkoutForm.paymentReference" placeholder="VD: Vietcombank FT26313 hoặc SePay..." />
                </el-form-item>

                <el-form-item label="GHI CHÚ ĐƠN HÀNG">
                  <el-input v-model="checkoutForm.note" placeholder="Ghi chú thêm nếu có..." />
                </el-form-item>

                <!-- VOUCHER POS & VÍ VOUCHER KHÁCH HÀNG (Thu gọn / Mở rộng) -->
                <div class="pos-voucher-box" :class="{ 'is-expanded': voucherExpanded }">
                  <div
                    class="pos-voucher-head"
                    role="button"
                    tabindex="0"
                    @click="voucherExpanded = !voucherExpanded"
                    @keyup.enter="voucherExpanded = !voucherExpanded"
                  >
                    <div class="pv-head-left">
                      <el-icon class="pv-icon"><Tickets /></el-icon>
                      <span class="pos-voucher-title">VOUCHER / MÃ GIẢM GIÁ</span>
                    </div>
                    <div class="pv-head-right">
                      <span v-if="appliedPosVoucher" class="pv-applied-pill">
                        ✓ {{ appliedPosVoucher.appliedCode }} (-{{ formatCurrency(appliedPosVoucher.discountAmount) }})
                        <button
                          type="button"
                          class="pv-inline-clear"
                          title="Gỡ voucher"
                          @click.stop="clearPosVoucher"
                        >
                          &times;
                        </button>
                      </span>
                      <span v-else-if="posAvailableVouchers.length" class="pos-voucher-count">
                        {{ posAvailableVouchers.length }} khả dụng
                      </span>
                      <el-icon class="pv-chevron" :class="{ rotated: voucherExpanded }"><ArrowDown /></el-icon>
                    </div>
                  </div>

                  <div v-if="voucherExpanded" class="pos-voucher-body">
                    <div class="pos-voucher-input-row">
                      <el-input
                        v-model="posVoucherInput"
                        size="small"
                        placeholder="Nhập mã voucher (VD: POS50K, SNEAKER10...)"
                        clearable
                        @keyup.enter="applyPosVoucherCode()"
                        @input="posVoucherInput = posVoucherInput.toUpperCase()"
                      />
                      <el-button
                        type="primary"
                        size="small"
                        :loading="validatingPosVoucher"
                        :disabled="!posVoucherInput.trim() || cart.length === 0"
                        @click="applyPosVoucherCode()"
                      >
                        Áp dụng
                      </el-button>
                    </div>

                    <div v-if="appliedPosVoucher" class="pos-voucher-applied">
                      <div>
                        <strong>✓ {{ appliedPosVoucher.appliedCode }}</strong>
                        <span> — Giảm {{ formatCurrency(appliedPosVoucher.discountAmount) }}</span>
                      </div>
                      <button type="button" class="pos-voucher-remove" @click="clearPosVoucher">Gỡ</button>
                    </div>
                    <div v-else-if="posVoucherError" class="pos-voucher-err">
                      {{ posVoucherError }}
                    </div>

                    <div v-if="posAvailableVouchers.length > 0" class="pos-voucher-list">
                      <div
                        v-for="v in posAvailableVouchers"
                        :key="v.code"
                        class="pos-voucher-chip"
                        :class="{
                          active: appliedPosVoucher?.appliedCode === v.code,
                          disabled: !v.eligible || cart.length === 0
                        }"
                        @click="v.eligible && cart.length > 0 && applyPosVoucherCode(v.code)"
                      >
                        <div class="chip-top">
                          <span class="chip-code">{{ v.code }}</span>
                          <span class="chip-tag" :class="v.sourceType === 'PERSONAL' ? 'personal' : 'public'">
                            {{ v.sourceType === 'PERSONAL' ? 'Ví KH' : 'Công khai' }}
                          </span>
                        </div>
                        <div class="chip-desc">
                          {{ v.discountType === 'PERCENT' ? `Giảm ${v.discountValue}%` : `Giảm ${formatCurrency(v.discountValue)}` }}
                          <span v-if="v.minOrderAmount > 0"> · Đơn từ {{ formatCurrency(v.minOrderAmount) }}</span>
                        </div>
                        <div v-if="!v.eligible && cart.length > 0" class="chip-reason">{{ v.ineligibleReason }}</div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Tùy chọn phát hành hóa đơn VAT tại quầy -->
                <div v-if="checkoutForm.status === 'COMPLETED'" style="margin-top: 6px; padding: 8px 10px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 5px;">
                  <el-checkbox v-model="vatForm.requestVat">
                    <strong style="font-size: 12px; color: #303133">Xuất hóa đơn VAT theo yêu cầu khách hàng</strong>
                  </el-checkbox>
                  <div v-if="vatForm.requestVat" style="margin-top: 8px; display: flex; flex-direction: column; gap: 6px;">
                    <el-input v-model="vatForm.companyName" size="small" placeholder="Tên công ty / Đơn vị mua hàng *" />
                    <el-row :gutter="8">
                      <el-col :span="14">
                        <el-input v-model="vatForm.taxCode" size="small" placeholder="Mã số thuế (MST) *" />
                      </el-col>
                      <el-col :span="10">
                        <el-input
                          :model-value="`VAT ${dbPosVatRate}% (DB)`"
                          size="small"
                          disabled
                          title="Thuế suất VAT lấy động từ cấu hình hệ thống (DB), không được phép sửa trên giao diện"
                        />
                      </el-col>
                    </el-row>
                    <el-input v-model="vatForm.address" size="small" placeholder="Địa chỉ công ty" />
                    <el-input v-model="vatForm.email" size="small" placeholder="Email nhận hóa đơn PDF (không bắt buộc)" />
                  </div>
                </div>
              </el-form>
            </div>
          </div>

          <!-- Pinned Bottom Checkout Summary & CTA -->
          <div class="pos-cart-sticky-footer">
            <div class="summary-totals">
              <div class="row">
                <span>Tổng số lượng:</span>
                <strong>{{ totalCartQty }} đôi</strong>
              </div>
              <div class="row">
                <span>Tổng tiền hàng:</span>
                <strong>{{ formatCurrency(totalCartAmount) }}</strong>
              </div>
              <div v-if="posVoucherDiscount > 0" class="row voucher-discount-row">
                <span>Giảm giá Voucher ({{ appliedPosVoucher?.appliedCode }}):</span>
                <strong style="color: #fef08a">-{{ formatCurrency(posVoucherDiscount) }}</strong>
              </div>
              <div class="row final">
                <span>THÀNH TIỀN THANH TOÁN:</span>
                <span>{{ formatCurrency(finalPosAmount) }}</span>
              </div>
            </div>

            <button
              class="pos-checkout-btn"
              :disabled="cart.length === 0 || submitting"
              @click="submitSaleOrder"
            >
              <span v-if="submitting">ĐANG TẠO ĐƠN HÀNG...</span>
              <span v-else>XÁC NHẬN ĐƠN HÀNG &amp; XUẤT HÓA ĐƠN</span>
            </button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Quick Add Customer Modal -->
    <PosQuickCustomerDialog v-model="quickCustVisible" @created="onQuickCustomerCreated" />

    <!-- Printable Invoice Modal -->
    <InvoicePrintModal v-model="invoiceVisible" :order="createdOrder" />

    <!-- CAMERA BARCODE SCANNER MODAL -->
    <el-dialog
      v-model="cameraScannerVisible"
      title="CAMERA QUÉT MÃ VẠCH HỘP GIÀY"
      width="460px"
      :before-close="closeCameraScanner"
      class="camera-scanner-dialog"
    >
      <div class="camera-scanner-body">
        <div class="camera-viewport-wrap">
          <video ref="cameraVideoRef" autoplay playsinline muted class="camera-feed"></video>
          <div class="camera-overlay">
            <div class="scanner-aim-frame">
              <div class="laser-line"></div>
            </div>
            <p class="camera-hint">Căn chỉnh mã vạch Code128 hoặc QR trên hộp giày vào giữa khung quét</p>
          </div>
        </div>
        <div v-if="cameraError" class="camera-err-msg">
          {{ cameraError }}
        </div>
        <div class="camera-manual-input">
          <el-input
            v-model="cameraFallbackInput"
            placeholder="Hoặc gõ / paste mã SKU..."
            size="small"
            @keyup.enter="handleCameraFallbackSubmit"
          >
            <template #append>
              <el-button @click="handleCameraFallbackSubmit">Nhập</el-button>
            </template>
          </el-input>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeCameraScanner">Đóng Camera</el-button>
      </template>
    </el-dialog>

    <!-- BARCODE PRINT MODAL -->
    <BarcodeLabelPrintModal
      v-model="printBarcodeVisible"
      :items="printBarcodeItems"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Delete, Plus, Printer, Tickets, Camera, Refresh, ArrowDown } from '@element-plus/icons-vue'
import { variantService } from '@/services/variantService'
import { productService } from '@/services/productService'
import { customerService } from '@/services/customerService'
import { orderService } from '@/services/orderService'
import { invoiceService } from '@/services/invoiceService'
import { voucherService } from '@/services/voucherService'
import { formatCurrency } from '@/utils/formatters'
import { useDebouncedWatch } from '@/composables/useDebounce'
import InvoicePrintModal from '@/components/InvoicePrintModal.vue'
import PosQuickCustomerDialog from './components/PosQuickCustomerDialog.vue'
import BarcodeLabelPrintModal from '@/components/common/BarcodeLabelPrintModal.vue'

const loading = ref(false)
const submitting = ref(false)
const voucherExpanded = ref(false)

// High-speed Barcode Scanner State
const barcodeScanInput = ref('')
const barcodeInputRef = ref(null)
const cameraScannerVisible = ref(false)
const cameraVideoRef = ref(null)
const cameraStream = ref(null)
const cameraError = ref('')
const cameraFallbackInput = ref('')

// Barcode Print Label State
const printBarcodeVisible = ref(false)
const printBarcodeItems = ref([])

const variants = ref([])
const products = ref([])
const sizes = ref([])
const colors = ref([])
const customers = ref([])

const searchQuery = ref('')
const debouncedSearchQuery = ref('')
const selectedSizeFilter = ref(null)
const selectedColorFilter = ref(null)

useDebouncedWatch(
  searchQuery,
  () => {
    debouncedSearchQuery.value = searchQuery.value
  },
  300
)

// Step-by-step picker state
const stepProductId = ref(null)
const stepSizeId = ref(null)
const stepColorId = ref(null)

const cart = ref([])
const checkoutForm = reactive({
  customerId: null,
  paymentMethod: 'CASH',
  status: 'COMPLETED',
  paymentReference: '',
  note: ''
})

// Quick customer modal
const quickCustVisible = ref(false)

// Invoice print modal
const invoiceVisible = ref(false)
const createdOrder = ref(null)

const activeProducts = computed(() => products.value.filter((p) => p.status === 'ACTIVE'))

const availableSizesForStepProduct = computed(() => {
  if (!stepProductId.value) return []
  const map = new Map()
  variants.value
    .filter((v) => v.productId === stepProductId.value && v.status === 'ACTIVE')
    .forEach((v) => {
      if (!map.has(v.sizeId)) {
        map.set(v.sizeId, { sizeId: v.sizeId, sizeValue: v.sizeValue })
      }
    })
  return Array.from(map.values())
})

const availableColorsForStep = computed(() => {
  if (!stepProductId.value || !stepSizeId.value) return []
  return variants.value
    .filter((v) => v.productId === stepProductId.value && v.sizeId === stepSizeId.value && v.status === 'ACTIVE')
    .map((v) => ({ colorId: v.colorId, colorName: v.colorName }))
})

const matchedStepVariant = computed(() => {
  if (!stepProductId.value || !stepSizeId.value || !stepColorId.value) return null
  return variants.value.find(
    (v) =>
      v.productId === stepProductId.value &&
      v.sizeId === stepSizeId.value &&
      v.colorId === stepColorId.value
  )
})

const filteredVariants = computed(() => {
  const q = debouncedSearchQuery.value.trim().toLowerCase()
  return variants.value.filter((v) => {
    if (v.status !== 'ACTIVE') return false
    if (selectedSizeFilter.value && v.sizeId !== selectedSizeFilter.value) return false
    if (selectedColorFilter.value && v.colorId !== selectedColorFilter.value) return false
    if (!q) return true
    return (
      v.sku.toLowerCase().includes(q) ||
      v.productName.toLowerCase().includes(q) ||
      (v.productCode && v.productCode.toLowerCase().includes(q))
    )
  })
})

const posPagination = reactive({
  page: 1,
  size: 8
})

const paginatedVariants = computed(() => {
  const start = (posPagination.page - 1) * posPagination.size
  return filteredVariants.value.slice(start, start + posPagination.size)
})

watch([debouncedSearchQuery, selectedSizeFilter, selectedColorFilter], () => {
  posPagination.page = 1
})

const totalCartQty = computed(() => cart.value.reduce((s, i) => s + Number(i.quantity || 0), 0))
const totalCartAmount = computed(() =>
  cart.value.reduce((s, i) => s + Number(i.quantity || 0) * Number(i.variant.salePrice || 0), 0)
)

// ==================== POS VOUCHER STATE ====================
const posVoucherInput = ref('')
const posAvailableVouchers = ref([])
const appliedPosVoucher = ref(null)
const posVoucherError = ref('')
const validatingPosVoucher = ref(false)

const posVoucherDiscount = computed(() => Number(appliedPosVoucher.value?.discountAmount || 0))
const finalPosAmount = computed(() => Math.max(0, totalCartAmount.value - posVoucherDiscount.value))

async function loadPosAvailableVouchers() {
  try {
    const selectedCust = customers.value.find((c) => c.id === checkoutForm.customerId)
    const res = await voucherService.getPosAvailableVouchers({
      customerId: checkoutForm.customerId || undefined,
      phone: selectedCust?.phone || undefined,
      orderAmount: totalCartAmount.value || 0
    })
    posAvailableVouchers.value = res.data || []
  } catch {
    posAvailableVouchers.value = []
  }
}

async function applyPosVoucherCode(codeOverride) {
  const code = (codeOverride || posVoucherInput.value || '').trim().toUpperCase()
  if (!code) return
  if (cart.value.length === 0) {
    ElMessage.warning('Vui lòng thêm sản phẩm vào đơn trước khi áp dụng voucher')
    return
  }
  posVoucherInput.value = code
  validatingPosVoucher.value = true
  posVoucherError.value = ''
  try {
    const selectedCust = customers.value.find((c) => c.id === checkoutForm.customerId)
    const res = await voucherService.validateVoucher({
      voucherCode: code,
      channel: 'POS',
      orderAmount: totalCartAmount.value,
      customerId: checkoutForm.customerId || null,
      customerPhone: selectedCust?.phone || null
    })
    const data = res.data
    if (data?.valid) {
      appliedPosVoucher.value = data
      posVoucherError.value = ''
      ElMessage.success(data.message || `Đã áp dụng mã ${data.appliedCode}`)
    } else {
      appliedPosVoucher.value = null
      posVoucherError.value = data?.message || 'Mã voucher không hợp lệ'
    }
  } catch (err) {
    appliedPosVoucher.value = null
    posVoucherError.value = err?.response?.data?.message || 'Không thể kiểm tra mã voucher'
  } finally {
    validatingPosVoucher.value = false
  }
}

function clearPosVoucher() {
  appliedPosVoucher.value = null
  posVoucherInput.value = ''
  posVoucherError.value = ''
}

watch(
  () => [checkoutForm.customerId, totalCartAmount.value],
  async () => {
    await loadPosAvailableVouchers()
    if (appliedPosVoucher.value && totalCartAmount.value > 0) {
      // Re-validate silently when customer or subtotal changes
      try {
        const selectedCust = customers.value.find((c) => c.id === checkoutForm.customerId)
        const res = await voucherService.validateVoucher({
          voucherCode: appliedPosVoucher.value.appliedCode,
          channel: 'POS',
          orderAmount: totalCartAmount.value,
          customerId: checkoutForm.customerId || null,
          customerPhone: selectedCust?.phone || null
        })
        if (res.data?.valid) {
          appliedPosVoucher.value = res.data
          posVoucherError.value = ''
        } else {
          appliedPosVoucher.value = null
          posVoucherError.value = res.data?.message || 'Mã voucher không còn đủ điều kiện'
        }
      } catch {
        appliedPosVoucher.value = null
      }
    } else if (totalCartAmount.value === 0) {
      appliedPosVoucher.value = null
      posVoucherError.value = ''
    }
  }
)

function onStepProductChange() {
  stepSizeId.value = availableSizesForStepProduct.value[0]?.sizeId || null
  onStepSizeChange()
}

function onStepSizeChange() {
  stepColorId.value = availableColorsForStep.value[0]?.colorId || null
}

function addVariantToCart(variant) {
  if (!variant || variant.availableQuantity <= 0) {
    ElMessage.warning('Biến thể này đã hết số lượng có sẵn')
    return
  }
  const existing = cart.value.find((x) => x.variant.id === variant.id)
  if (existing) {
    if (existing.quantity + 1 > variant.availableQuantity) {
      ElMessage.warning(`SKU ${variant.sku} chỉ còn tối đa ${variant.availableQuantity} đôi có sẵn`)
      return
    }
    existing.quantity += 1
  } else {
    cart.value.push({
      variant,
      quantity: 1
    })
  }
  ElMessage.success(`Đã thêm ${variant.sku} vào đơn hàng`)
}

async function loadCatalog() {
  loading.value = true
  try {
    loadVatConfig()
    const [vRes, pRes, sRes, cRes, custRes] = await Promise.all([
      variantService.getAllVariants(),
      productService.getAllProducts(),
      variantService.getSizes(),
      variantService.getColors(),
      customerService.getAllCustomers()
    ])
    variants.value = vRes.data || []
    products.value = pRes.data || []
    sizes.value = sRes.data || []
    colors.value = cRes.data || []
    customers.value = custRes.data || []
    await loadPosAvailableVouchers()
  } finally {
    loading.value = false
  }
}

function onQuickCustomerCreated(newCust) {
  customers.value.unshift(newCust)
  checkoutForm.customerId = newCust.id
}

const dbPosVatRate = ref(8)
const vatForm = reactive({
  requestVat: false,
  companyName: '',
  taxCode: '',
  address: '',
  email: ''
})

async function loadVatConfig() {
  try {
    const res = await invoiceService.getVatConfig()
    if (res?.data?.posVatRate !== undefined && res?.data?.posVatRate !== null) {
      dbPosVatRate.value = Number(res.data.posVatRate)
    }
  } catch {
    // keep fallback
  }
}

async function submitSaleOrder() {
  if (cart.value.length === 0) return
  if (checkoutForm.status === 'COMPLETED' && vatForm.requestVat) {
    if (!vatForm.companyName.trim() || !vatForm.taxCode.trim()) {
      ElMessage.warning('Vui lòng nhập Tên công ty và Mã số thuế (MST) để xuất hóa đơn VAT')
      return
    }
  }
  submitting.value = true
  try {
    const payload = {
      customerId: checkoutForm.customerId || null,
      paymentMethod: checkoutForm.paymentMethod,
      status: checkoutForm.status,
      paymentReference: checkoutForm.paymentReference || null,
      voucherCode: appliedPosVoucher.value?.appliedCode || null,
      note: checkoutForm.note || 'Bán hàng trực tiếp tại quầy POS',
      invoiceType: checkoutForm.status === 'COMPLETED' && vatForm.requestVat ? 'VAT' : 'RETAIL',
      vatCompanyName: vatForm.requestVat ? vatForm.companyName.trim() : null,
      vatTaxCode: vatForm.requestVat ? vatForm.taxCode.trim() : null,
      vatAddress: vatForm.requestVat ? vatForm.address.trim() : null,
      vatEmail: vatForm.requestVat ? vatForm.email.trim() : null,
      items: cart.value.map((i) => ({
        productVariantId: i.variant.id,
        quantity: i.quantity
      }))
    }
    const res = await orderService.createSaleOrder(payload)
    createdOrder.value = res.data
    ElMessage.success(
      `Đã tạo đơn hàng ${createdOrder.value.orderCode}` +
        (checkoutForm.status === 'COMPLETED'
          ? ` & tự động phát hành hóa đơn ${vatForm.requestVat ? 'VAT' : 'bán lẻ'}!`
          : ' thành công!')
    )
    cart.value = []
    clearPosVoucher()
    checkoutForm.note = ''
    checkoutForm.paymentReference = ''
    vatForm.requestVat = false
    vatForm.companyName = ''
    vatForm.taxCode = ''
    vatForm.address = ''
    vatForm.email = ''
    invoiceVisible.value = true
    await loadCatalog()
  } finally {
    submitting.value = false
  }
}

// --- AUDIO SYNTHESIZER FOR SCAN BEEP ---
const playScanBeep = (isSuccess = true) => {
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext
    if (!AudioContext) return
    const ctx = new AudioContext()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.connect(gain)
    gain.connect(ctx.destination)
    if (isSuccess) {
      osc.type = 'sine'
      osc.frequency.setValueAtTime(1400, ctx.currentTime)
      gain.gain.setValueAtTime(0.15, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.12)
      osc.start(ctx.currentTime)
      osc.stop(ctx.currentTime + 0.12)
    } else {
      osc.type = 'sawtooth'
      osc.frequency.setValueAtTime(250, ctx.currentTime)
      gain.gain.setValueAtTime(0.2, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.22)
      osc.start(ctx.currentTime)
      osc.stop(ctx.currentTime + 0.22)
    }
  } catch (e) {
    // Ignore audio error
  }
}

// --- UNIFIED SEARCH & BARCODE SCAN PROCESSOR ---
function handleScanOrEnter() {
  const code = searchQuery.value.trim()
  if (!code) return

  const cleanCode = code.toLowerCase()
  const matched = variants.value.find(
    (v) =>
      v.sku.toLowerCase() === cleanCode ||
      (v.barcode && v.barcode.toLowerCase() === cleanCode) ||
      String(v.id) === cleanCode ||
      (v.productCode && v.productCode.toLowerCase() === cleanCode)
  )

  if (matched) {
    if (matched.availableQuantity <= 0) {
      playScanBeep(false)
      ElMessage.error(`⚠️ Đôi ${matched.productName} (SKU: ${matched.sku}) đã hết hàng trong kho!`)
      return
    }
    addVariantToCart(matched)
    playScanBeep(true)
    searchQuery.value = ''
    debouncedSearchQuery.value = ''
    nextTick(() => {
      barcodeInputRef.value?.focus()
    })
  } else {
    if (filteredVariants.value.length === 1) {
      const single = filteredVariants.value[0]
      if (single.availableQuantity <= 0) {
        playScanBeep(false)
        ElMessage.error(`⚠️ Đôi ${single.productName} (SKU: ${single.sku}) đã hết hàng trong kho!`)
        return
      }
      addVariantToCart(single)
      playScanBeep(true)
      searchQuery.value = ''
      debouncedSearchQuery.value = ''
      nextTick(() => {
        barcodeInputRef.value?.focus()
      })
    } else if (filteredVariants.value.length === 0) {
      playScanBeep(false)
      ElMessage.warning(`❌ Không tìm thấy mã vạch / SKU: "${code}". Vui lòng thử lại!`)
    } else {
      ElMessage.info(`Tìm thấy ${filteredVariants.value.length} mẫu phù hợp. Bấm dấu [+] để thêm vào đơn.`)
    }
  }
}

function handleBarcodeScanned() {
  const code = barcodeScanInput.value.trim() || searchQuery.value.trim()
  if (!code) return
  processScannedSku(code)
}

function processScannedSku(code) {
  if (!code) return
  const cleanCode = code.trim().toLowerCase()
  const matched = variants.value.find(
    (v) =>
      v.sku.toLowerCase() === cleanCode ||
      (v.barcode && v.barcode.toLowerCase() === cleanCode) ||
      String(v.id) === cleanCode ||
      (v.productCode && v.productCode.toLowerCase() === cleanCode)
  )

  if (matched) {
    if (matched.availableQuantity <= 0) {
      playScanBeep(false)
      ElMessage.error(`⚠️ Đôi ${matched.productName} (SKU: ${matched.sku}) đã hết hàng trong kho!`)
      return
    }
    addVariantToCart(matched)
    playScanBeep(true)
    barcodeScanInput.value = ''
    searchQuery.value = ''
    debouncedSearchQuery.value = ''
    nextTick(() => {
      barcodeInputRef.value?.focus()
    })
  } else {
    playScanBeep(false)
    ElMessage.warning(`❌ Không tìm thấy mã vạch / SKU: "${code}". Vui lòng thử lại!`)
  }
}

// Global Hardware Scanner Listener
let barcodeBuffer = ''
let lastKeyTime = 0

function handleGlobalKeydown(e) {
  const targetTag = e.target.tagName ? e.target.tagName.toLowerCase() : ''
  if (targetTag === 'input' || targetTag === 'textarea' || targetTag === 'select') {
    return
  }

  const currentTime = Date.now()
  if (currentTime - lastKeyTime > 150) {
    barcodeBuffer = ''
  }
  lastKeyTime = currentTime

  if (e.key === 'Enter') {
    if (barcodeBuffer.length >= 3) {
      processScannedSku(barcodeBuffer)
      barcodeBuffer = ''
      e.preventDefault()
    }
  } else if (e.key.length === 1) {
    barcodeBuffer += e.key
  }
}

// --- CAMERA BARCODE SCANNER ---
let cameraAnimationId = null

async function openCameraScanner() {
  cameraScannerVisible.value = true
  cameraError.value = ''
  cameraFallbackInput.value = ''
  await nextTick()

  try {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      cameraError.value = 'Trình duyệt không hỗ trợ truy cập camera trực tiếp.'
      return
    }
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment' }
    })
    cameraStream.value = stream
    if (cameraVideoRef.value) {
      cameraVideoRef.value.srcObject = stream
      await cameraVideoRef.value.play()
      startCameraDetectionLoop()
    }
  } catch (err) {
    console.warn('Camera access error:', err)
    cameraError.value = 'Không thể mở Camera (Có thể do quyền bị từ chối hoặc máy không có camera).'
  }
}

function startCameraDetectionLoop() {
  if (!('BarcodeDetector' in window)) {
    return
  }

  try {
    const barcodeDetector = new window.BarcodeDetector({
      formats: ['code_128', 'qr_code', 'ean_13', 'code_39']
    })

    const detectFrame = async () => {
      if (!cameraScannerVisible.value || !cameraVideoRef.value) return
      try {
        if (cameraVideoRef.value.readyState >= 2) {
          const barcodes = await barcodeDetector.detect(cameraVideoRef.value)
          if (barcodes.length > 0) {
            const rawValue = barcodes[0].rawValue
            if (rawValue) {
              processScannedSku(rawValue)
              closeCameraScanner()
              return
            }
          }
        }
      } catch (e) {
        // Frame error
      }
      cameraAnimationId = requestAnimationFrame(detectFrame)
    }

    detectFrame()
  } catch (e) {
    console.warn('BarcodeDetector init error:', e)
  }
}

function closeCameraScanner() {
  if (cameraAnimationId) {
    cancelAnimationFrame(cameraAnimationId)
    cameraAnimationId = null
  }
  if (cameraStream.value) {
    cameraStream.value.getTracks().forEach((t) => t.stop())
    cameraStream.value = null
  }
  cameraScannerVisible.value = false
}

function handleCameraFallbackSubmit() {
  if (cameraFallbackInput.value.trim()) {
    processScannedSku(cameraFallbackInput.value.trim())
    cameraFallbackInput.value = ''
    closeCameraScanner()
  }
}

function printSingleVariant(row) {
  printBarcodeItems.value = [{
    sku: row.sku,
    productName: row.productName,
    sizeValue: row.sizeValue,
    colorName: row.colorName,
    salePrice: row.salePrice,
    brandName: row.brandName || 'ADHA SNEAKER',
    printQuantity: 1
  }]
  printBarcodeVisible.value = true
}

onMounted(() => {
  loadCatalog()
  window.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
  closeCameraScanner()
})
</script>

<style scoped lang="scss">
.header-action-group {
  display: flex;
  align-items: center;
  gap: 8px;

  .pos-hdr-btn {
    height: 36px;
    padding: 0 16px;
    background: #ffffff;
    color: #303133;
    border: 1px solid var(--vea-theme, #409eff);
    font-family: var(--font-sans);
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    cursor: pointer;
    transition: all 0.15s ease;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    border-radius: 5px;

    &:hover {
      background: #f4f4f5;
    }

    &.primary {
      background: var(--vea-theme, #409eff);
      color: #ffffff;

      &:hover {
        background: #27272a;
        border-color: #27272a;
      }
    }
  }
}

/* UNIFIED MASTER TOOLBAR */
.pos-master-toolbar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;

  .search-scan-row {
    display: flex;
    gap: 8px;
    align-items: stretch;

    .master-search-input {
      flex: 1;

      :deep(.el-input__wrapper) {
        border-radius: 5px !important;
        box-shadow: 0 0 0 1px var(--vea-theme, #409eff) inset !important;
        height: 40px;
        background-color: #ffffff;

        &.is-focus {
          box-shadow: 0 0 0 2px var(--vea-theme, #409eff) inset !important;
        }

        input {
          font-weight: 600;
          font-size: 13px;

          &::placeholder {
            color: #71717a;
            font-size: 12px;
            font-weight: 400;
          }
        }
      }
    }

    .btn-scan-trigger {
      height: 40px;
      padding: 0 18px;
      background: var(--vea-theme, #409eff);
      color: #ffffff;
      border: 1px solid var(--vea-theme, #409eff);
      border-radius: 5px;
      font-family: var(--font-sans);
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.15s ease;
      white-space: nowrap;
      box-sizing: border-box;

      &:hover {
        background: #27272a;
        border-color: #27272a;
      }
    }

    .btn-camera-trigger {
      height: 40px;
      padding: 0 14px;
      background: #ffffff;
      color: #303133;
      border: 1px solid var(--vea-theme, #409eff);
      border-radius: 5px;
      font-family: var(--font-sans);
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.15s ease;
      white-space: nowrap;

      &:hover {
        background: var(--vea-theme, #409eff);
        color: #ffffff;
      }
    }
  }

  .sub-controls-row {
    display: flex;
    gap: 8px;
    align-items: center;
    flex-wrap: wrap;

    .filter-dropdowns {
      display: flex;
      gap: 6px;

      .filter-select {
        width: 135px;

        :deep(.el-input__wrapper) {
          border-radius: 5px !important;
          height: 34px;
        }
      }
    }

    .quick-step-strip {
      flex: 1;
      display: flex;
      align-items: center;
      gap: 6px;
      background: #fafafa;
      border: 1px solid #e5e5e5;
      padding: 4px 8px;
      min-width: 320px;

      .strip-label {
        font-size: 10px;
        font-weight: 900;
        letter-spacing: 0.06em;
        color: #303133;
        white-space: nowrap;
        margin-right: 2px;
      }

      .step-select {
        :deep(.el-input__wrapper) {
          border-radius: 5px !important;
          height: 28px;
          font-size: 11.5px;
        }

        &.prod {
          flex: 2;
          min-width: 140px;
        }

        &.size {
          width: 85px;
        }

        &.color {
          width: 100px;
        }
      }

      .step-add-btn {
        height: 28px;
        padding: 0 10px;
        background: var(--vea-theme, #409eff);
        color: #ffffff;
        border: 1px solid var(--vea-theme, #409eff);
        border-radius: 5px;
        font-family: var(--font-sans);
        font-size: 10px;
        font-weight: 800;
        letter-spacing: 0.05em;
        text-transform: uppercase;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 4px;
        white-space: nowrap;
        transition: all 0.12s ease;

        &:hover:not(:disabled) {
          background: #27272a;
          border-color: #27272a;
        }

        &:disabled {
          background: #e4e4e7;
          border-color: #d4d4d8;
          color: #a1a1aa;
          cursor: not-allowed;
        }
      }
    }
  }

  .step-match-alert {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 12px;
    background: #f4f4f5;
    border: 1px dashed #d4d4d8;
    font-size: 11.5px;
    color: #3f3f46;

    .divider {
      color: #a1a1aa;
    }

    strong {
      color: #303133;
    }

    .stock-num {
      color: #16a34a;
    }
  }
}

/* DATA TABLE ATTRS & BADGES */
.pos-prod-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .pos-prod-thumb {
    width: 34px;
    height: 34px;
    border-radius: 5px;
    object-fit: cover;
    border: 1px solid #e5e5e5;
    flex-shrink: 0;

    &.placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--vea-theme, #409eff);
      color: #ffffff;
      font-size: 9px;
      font-weight: 800;
      letter-spacing: 0.05em;
    }
  }

  .pos-prod-meta {
    display: flex;
    flex-direction: column;
    min-width: 0;

    .pos-prod-title {
      font-size: 12px;
      font-weight: 700;
      color: #303133;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .pos-prod-sub {
      font-size: 10px;
      color: #71717a;
      font-family: var(--font-mono);
    }
  }
}

.pos-attr-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;

  .size-pill {
    font-family: var(--font-mono);
    font-size: 11px;
    font-weight: 800;
    background: #f4f4f5;
    color: #303133;
    border: 1px solid #d4d4d4;
    padding: 1px 6px;
    border-radius: 5px;
    flex-shrink: 0;
  }

  .color-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    border: 1px solid rgba(0, 0, 0, 0.2);
    flex-shrink: 0;
  }

  .color-text {
    font-size: 11.5px;
    color: #18181b;
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 85px;
  }
}

.stock-pill {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: 5px;
  border: 1px solid;
  letter-spacing: 0.02em;

  &.in-stock {
    background: #ecfdf5;
    color: #059669;
    border-color: #a7f3d0;
  }

  &.out-stock {
    background: #fef2f2;
    color: #dc2626;
    border-color: #fecaca;
  }
}

.pos-pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #e5e5e5;

  .pos-pagination-info {
    font-size: 11px;
    font-weight: 700;
    color: #71717a;
    text-transform: uppercase;
    letter-spacing: 0.04em;

    strong {
      color: #303133;
    }
  }
}

/* RIGHT CART CARD */
.pos-cart-card {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .cart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e5e5e5;
    padding-bottom: 12px;

    .cart-header-title {
      display: flex;
      align-items: center;
      gap: 8px;

      h3 {
        font-size: 13px;
        font-weight: 900;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        color: #303133;
        margin: 0;
      }

      .cart-count-badge {
        font-family: var(--font-mono);
        font-size: 10.5px;
        font-weight: 800;
        background: var(--vea-theme, #409eff);
        color: #ffffff;
        padding: 2px 7px;
        border-radius: 5px;
      }
    }

    .cart-clear-btn {
      background: transparent;
      border: none;
      color: #ef4444;
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      cursor: pointer;
      padding: 0;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .pos-cart-scroll-body {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding-right: 3px;
  }

  .pos-cart-sticky-footer {
    flex-shrink: 0;
    padding-top: 10px;
    border-top: 1px solid #e6ebf5;
    background: #ffffff;
  }

  .customer-box {
    flex-shrink: 0;
    background: #fafafa;
    padding: 10px 12px;
    border-radius: 5px;
    border: 1px solid #e5e5e5;

    .box-label {
      font-size: 10px;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: #737373;
      margin-bottom: 6px;
    }

    .cust-row {
      display: flex;
      gap: 8px;

      :deep(.el-input__wrapper) {
        border-radius: 5px !important;
      }
    }
  }

  .empty-cart-state {
    flex-shrink: 0;
    padding: 20px 16px;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    border: 1px dashed #dcdfe6;
    border-radius: 5px;
    background: #fafafa;

    .empty-title {
      font-size: 12px;
      font-weight: 800;
      letter-spacing: 0.06em;
      color: #303133;
      text-transform: uppercase;
    }

    .empty-hint {
      font-size: 11px;
      color: #71717a;
      max-width: 260px;
      line-height: 1.4;
    }
  }

  .cart-list {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .cart-item {
      flex-shrink: 0;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 10px;
      padding: 9px 11px;
      border-radius: 5px;
      border: 1px solid var(--vea-theme-light-7, #c6e2ff);
      background: #f8fbff;

      .item-info {
        min-width: 0;
        flex: 1;

        .item-title {
          font-size: 12.5px;
          font-weight: 700;
          color: #303133;
          line-height: 1.3;
        }

        .item-meta {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 5px;
          font-size: 11px;
          color: #64748b;
          margin: 3px 0;

          .avail {
            color: #10b981;
            font-weight: 700;
          }
        }

        .item-price {
          font-size: 12px;
          color: #475569;
        }
      }

      .item-actions {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-shrink: 0;
      }
    }
  }

  .checkout-section {
    flex-shrink: 0;
    border-top: 1px solid #e6ebf5;
    padding-top: 10px;

    .pos-checkout-form {
      :deep(.el-form-item) {
        margin-bottom: 10px !important;
      }

      :deep(.el-form-item__label) {
        font-size: 10px !important;
        font-weight: 800 !important;
        letter-spacing: 0.06em !important;
        text-transform: uppercase !important;
        color: #64748b !important;
        padding-bottom: 3px !important;
      }

      :deep(.el-input__wrapper) {
        border-radius: 5px !important;
      }
    }

    .pos-radio-group {
      width: 100%;
      display: flex;

      :deep(.el-radio-button) {
        flex: 1;

        .el-radio-button__inner {
          width: 100%;
          border-radius: 5px !important;
          font-weight: 800 !important;
          font-size: 11px !important;
          letter-spacing: 0.05em !important;
          text-transform: uppercase !important;
        }

        &.is-active .el-radio-button__inner {
          background-color: var(--vea-theme, #409eff) !important;
          border-color: var(--vea-theme, #409eff) !important;
          color: #ffffff !important;
          box-shadow: -1px 0 0 0 var(--vea-theme, #409eff) !important;
        }
      }
    }
  }

  .summary-totals {
    background: linear-gradient(135deg, var(--vea-theme, #409eff) 0%, var(--vea-theme-dark-2, #337ecc) 100%);
    color: #ffffff;
    padding: 10px 14px;
    border-radius: 6px;
    margin-bottom: 8px;

    .row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12.5px;
      color: rgba(255, 255, 255, 0.9);
      margin-bottom: 3px;

      strong,
      span:last-child {
        color: #ffffff;
        font-variant-numeric: tabular-nums;
      }
    }

    .final {
      border-top: 1px solid rgba(255, 255, 255, 0.25);
      padding-top: 7px;
      margin-top: 5px;
      margin-bottom: 0;
      font-size: 14px;
      font-weight: 800;
      color: #ffffff;

      span:last-child {
        color: #ffffff;
        font-size: 17px;
        font-weight: 900;
      }
    }
  }
}

.pos-cust-btn {
  height: 32px;
  padding: 0 12px;
  background: var(--vea-theme, #409eff);
  color: #ffffff;
  border: 1px solid var(--vea-theme, #409eff);
  border-radius: 5px;
  font-family: var(--font-sans);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.12s ease;
  white-space: nowrap;

  &:hover {
    background: #27272a;
    border-color: #27272a;
  }
}

.cart-delete-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid #ef4444;
  background: #ffffff;
  color: #ef4444;
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;

  &:hover {
    background: #ef4444;
    color: #ffffff;
  }

  .el-icon {
    font-size: 13px;
  }
}

.pos-checkout-btn {
  width: 100%;
  height: 40px;
  background: var(--vea-theme, #409eff);
  color: #ffffff;
  border: 1px solid var(--vea-theme, #409eff);
  border-radius: 6px;
  font-family: var(--font-sans);
  font-size: 12.5px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover:not(:disabled) {
    background: var(--vea-theme-dark-2, #337ecc);
    border-color: var(--vea-theme-dark-2, #337ecc);
  }

  &:disabled {
    background: #e4e4e7;
    color: #a1a1aa;
    border-color: #e4e4e7;
    cursor: not-allowed;
  }
}

/* CAMERA SCANNER DIALOG */
.camera-scanner-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;

  .camera-viewport-wrap {
    position: relative;
    width: 100%;
    height: 280px;
    background: var(--vea-theme, #409eff);
    overflow: hidden;
    border-radius: 5px;
    display: flex;
    align-items: center;
    justify-content: center;

    .camera-feed {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .camera-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      pointer-events: none;

      .scanner-aim-frame {
        width: 260px;
        height: 160px;
        border: 2px solid #10b981;
        border-radius: 5px;
        position: relative;
        box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.45);

        .laser-line {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          height: 2px;
          background: #ef4444;
          box-shadow: 0 0 8px #ef4444;
          animation: scanLineAnim 2s infinite ease-in-out;
        }
      }

      .camera-hint {
        color: #ffffff;
        font-size: 11px;
        font-weight: 600;
        text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8);
        margin-top: 14px;
        text-align: center;
        padding: 0 16px;
      }
    }
  }

  .camera-err-msg {
    color: #ef4444;
    font-size: 12px;
    font-weight: 600;
    text-align: center;
  }

  .camera-manual-input {
    width: 100%;
  }
}

@keyframes scanLineAnim {
  0% { top: 0%; }
  50% { top: 100%; }
  100% { top: 0%; }
}

.pos-voucher-box {
  margin-bottom: 8px;
  padding: 8px 10px;
  background: #fafbfc;
  border: 1px solid #e6ebf5;
  border-radius: 5px;
  transition: border-color 0.15s ease;

  &:hover,
  &.is-expanded {
    border-color: var(--vea-theme-light-5, #a0cfff);
  }

  .pos-voucher-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    user-select: none;

    .pv-head-left {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      min-width: 0;
    }

    .pv-icon {
      color: var(--vea-theme, #409eff);
      font-size: 14px;
      flex-shrink: 0;
    }

    .pos-voucher-title {
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.05em;
      color: #303133;
      white-space: nowrap;
    }

    .pv-head-right {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      flex-shrink: 0;
    }

    .pos-voucher-count {
      font-size: 10.5px;
      font-weight: 700;
      color: #059669;
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
      padding: 1px 6px;
      border-radius: 999px;
      white-space: nowrap;
    }

    .pv-applied-pill {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 10.5px;
      font-weight: 700;
      color: #065f46;
      background: #ecfdf5;
      border: 1px solid #6ee7b7;
      padding: 1px 6px;
      border-radius: 4px;
      white-space: nowrap;

      .pv-inline-clear {
        border: none;
        background: transparent;
        color: #dc2626;
        font-weight: 800;
        font-size: 13px;
        line-height: 1;
        cursor: pointer;
        padding: 0 2px;
      }
    }

    .pv-chevron {
      font-size: 13px;
      color: #909399;
      transition: transform 0.18s ease;

      &.rotated {
        transform: rotate(180deg);
        color: var(--vea-theme, #409eff);
      }
    }
  }

  .pos-voucher-body {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px dashed #e2e8f0;
  }

  .pos-voucher-input-row {
    display: flex;
    gap: 6px;
  }

  .pos-voucher-applied {
    margin-top: 8px;
    padding: 6px 10px;
    background: #ecfdf5;
    border: 1px solid #a7f3d0;
    border-radius: 4px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    color: #065f46;
    .pos-voucher-remove {
      background: none;
      border: none;
      color: #dc2626;
      font-weight: 700;
      font-size: 11.5px;
      cursor: pointer;
      text-decoration: underline;
    }
  }

  .pos-voucher-err {
    margin-top: 6px;
    font-size: 11.5px;
    color: #dc2626;
    font-weight: 600;
  }

  .pos-voucher-list {
    margin-top: 8px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    max-height: 145px;
    overflow-y: auto;
  }

  .pos-voucher-chip {
    padding: 6px 8px;
    background: #ffffff;
    border: 1px solid #e5e5e5;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.14s ease;

    &:hover:not(.disabled) {
      border-color: var(--vea-theme, #409eff);
    }
    &.active {
      border-color: var(--vea-theme, #409eff);
      background: #f4f4f5;
      box-shadow: inset 3px 0 0 var(--vea-theme, #409eff);
    }
    &.disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .chip-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      .chip-code {
        font-family: var(--font-mono);
        font-size: 12px;
        font-weight: 800;
        color: #303133;
      }
      .chip-tag {
        font-size: 9.5px;
        font-weight: 800;
        padding: 1px 5px;
        border-radius: 2px;
        &.personal {
          background: #fee2e2;
          color: #b91c1c;
        }
        &.public {
          background: #f1f5f9;
          color: #334155;
        }
      }
    }
    .chip-desc {
      font-size: 11.5px;
      color: #404040;
      margin-top: 2px;
    }
    .chip-reason {
      font-size: 10.5px;
      color: #dc2626;
      margin-top: 2px;
    }
  }
}

@media (max-width: 1024px) {
  .pos-page {
    > .el-row {
      row-gap: 14px;
    }
  }
}

@media (max-width: 768px) {
  .pos-master-toolbar {
    .search-scan-row {
      flex-wrap: wrap;
      gap: 6px;

      .master-search-input {
        width: 100%;
        flex: 1 0 100%;
      }

      .btn-scan-trigger,
      .btn-camera-trigger {
        flex: 1;
        height: 36px;
        justify-content: center;
        padding: 0 10px;
      }
    }

    .sub-controls-row {
      flex-direction: column;
      align-items: stretch;
      gap: 8px;

      .filter-dropdowns {
        width: 100%;
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 6px;

        .filter-select {
          width: 100%;
        }
      }

      .quick-step-strip {
        min-width: 0 !important;
        width: 100%;
        flex-wrap: wrap;

        .step-select.prod {
          min-width: 100%;
        }
      }
    }
  }

  .pos-cart-card {
    .cart-item {
      flex-direction: column;
      align-items: stretch !important;
      gap: 8px;

      .item-actions {
        justify-content: flex-end;
      }
    }

    .customer-box .cust-row {
      flex-direction: column;
    }
  }

  .pos-pagination-bar {
    flex-direction: column;
    gap: 6px;
    align-items: center;
  }
}
</style>
