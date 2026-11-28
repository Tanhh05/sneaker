<template>
  <div class="adi-order-history-page">
    <div class="adi-oh-container">
      <!-- ================================================================ -->
      <!-- EXACT LIVE 2026 ADIDAS /order-history HEADER & CONTROLS ROW      -->
      <!-- Pure white canvas, upright H1 "ĐƠN HÀNG", Online/In-store tabs   -->
      <!-- ================================================================ -->
      <header class="adi-oh-header">
        <h1 class="adi-oh-h1">{{ isEn ? 'ORDERS' : 'ĐƠN HÀNG' }}</h1>

        <div class="adi-oh-controls-row">
          <!-- Left: Exact Adidas Underline Channel Tabs ("Trực tuyến" | "Tại cửa hàng") -->
          <div class="adi-channel-tabs" role="tablist">
            <button
              type="button"
              role="tab"
              class="channel-tab-btn"
              :class="{ active: channelTab === 'ONLINE' }"
              @click="channelTab = 'ONLINE'"
            >
              {{ isEn ? 'Online' : 'Trực tuyến' }}
            </button>
            <button
              type="button"
              role="tab"
              class="channel-tab-btn"
              :class="{ active: channelTab === 'INSTORE' }"
              @click="channelTab = 'INSTORE'"
            >
              {{ isEn ? 'In-store' : 'Tại cửa hàng' }}
            </button>
          </div>

          <!-- Right: Exact Adidas "Xem đơn hàng từ: [Bất kỳ lúc nào ⌄]" Select -->
          <div class="adi-time-filter">
            <label for="oh-time-select" class="time-filter-lbl">
              {{ isEn ? 'View orders from:' : 'Xem đơn hàng từ:' }}
            </label>
            <div class="time-select-wrap">
              <select id="oh-time-select" v-model="timeRange" class="adi-time-select">
                <option value="ALL">{{ isEn ? 'Any time' : 'Bất kỳ lúc nào' }}</option>
                <option value="30D">{{ isEn ? 'Last 30 days' : '30 ngày qua' }}</option>
                <option value="90D">{{ isEn ? 'Last 3 months' : '3 tháng qua' }}</option>
                <option value="2026">{{ isEn ? 'Year 2026' : 'Năm 2026' }}</option>
              </select>
              <svg class="select-chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
        </div>
      </header>

      <!-- Secondary Status + Search Strip (Only shown cleanly when user actually has orders) -->
      <div v-if="channelOrders.length > 0" class="adi-orders-toolbar">
        <div class="status-pills-row">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            type="button"
            class="st-pill-btn"
            :class="{ active: currentTab === tab.value }"
            @click="currentTab = tab.value"
          >
            <span>{{ tab.label }}</span>
            <span class="st-cnt">{{ getCountByStatus(tab.value) }}</span>
          </button>
        </div>

        <div class="toolbar-search-right">
          <input
            v-model="searchQuery"
            type="text"
            class="oh-search-input"
            :placeholder="isEn ? 'Search order code or sneaker...' : 'Tìm mã đơn hoặc tên giày...'"
          />
          <button type="button" class="btn-oh-refresh" @click="fetchMyOrders" :disabled="loading">
            {{ isEn ? 'Refresh' : 'Làm mới' }}
          </button>
        </div>
      </div>

      <!-- ================================================================ -->
      <!-- ORDERS CONTENT AREA                                              -->
      <!-- ================================================================ -->
      <div class="adi-oh-body">
        <!-- LOADING -->
        <div v-if="loading" class="oh-loading-wrap">
          <el-skeleton :rows="5" animated />
        </div>

        <!-- EXACT LIVE ADIDAS CENTERED EMPTY STATE ("CHƯA CÓ ĐƠN HÀNG NÀO") -->
        <div v-else-if="filteredOrders.length === 0" class="adi-oh-empty">
          <h2 class="empty-h2">
            {{
              channelOrders.length === 0
                ? (isEn ? 'NO ORDERS YET' : 'CHƯA CÓ ĐƠN HÀNG NÀO')
                : (isEn ? 'NO MATCHING ORDERS' : 'KHÔNG CÓ ĐƠN HÀNG PHÙ HỢP')
            }}
          </h2>
          <p class="empty-sub">
            {{
              channelOrders.length === 0
                ? (isEn ? 'Ready to get started?' : 'Bạn đã sẵn sàng để bắt đầu chưa?')
                : (isEn ? 'Try switching the timeframe or clearing your filter.' : 'Hãy thử thay đổi khoảng thời gian hoặc đặt lại bộ lọc.')
            }}
          </p>

          <button
            v-if="channelOrders.length === 0"
            type="button"
            class="btn-adi-start-shopping"
            @click="router.push(lp('shop'))"
          >
            <span>{{ isEn ? 'Start shopping' : 'Bắt đầu mua sắm' }}</span>
            <span class="arrow">→</span>
          </button>
          <button v-else type="button" class="btn-adi-start-shopping" @click="resetFilters">
            <span>{{ isEn ? 'Reset filters' : 'Đặt lại bộ lọc' }}</span>
            <span class="arrow">→</span>
          </button>
        </div>

        <!-- SHARP ADIDAS ORDER CARDS LIST (WHEN ORDERS EXIST) -->
        <div v-else class="adi-orders-stack">
          <article
            v-for="order in filteredOrders"
            :key="order.id"
            class="adi-order-card"
          >
            <!-- Card Top Meta Row -->
            <div class="oc-header">
              <div class="oc-meta-left">
                <button
                  type="button"
                  class="oc-code-btn"
                  @click="copyOrderCode(order.orderCode)"
                  :title="isEn ? 'Copy order number' : 'Sao chép mã đơn'"
                >
                  #{{ order.orderCode }}
                </button>
                <span class="oc-date">{{ formatDateTime(order.orderDate) }}</span>
                <span class="oc-channel-tag">
                  {{ order.employeeId ? (isEn ? 'In-store' : 'Tại cửa hàng') : (isEn ? 'Online' : 'Trực tuyến') }}
                </span>
              </div>

              <div class="oc-meta-right">
                <span class="oc-pay-tag">
                  {{ order.paymentMethod === 'BANK_TRANSFER' ? 'VietQR SePay' : 'COD' }}
                </span>
                <span class="oc-status-tag" :class="(order.status || 'pending').toLowerCase()">
                  {{ getStatusLabel(order.status) }}
                </span>
              </div>
            </div>

            <!-- Items List -->
            <div class="oc-items">
              <div v-for="item in (order.details || [])" :key="item.id" class="oc-item-row">
                <div class="oc-thumb">
                  <img v-if="item.thumbnailUrl" :src="item.thumbnailUrl" :alt="item.productName" />
                  <span v-else class="thumb-fallback">SNK</span>
                </div>

                <div class="oc-item-info">
                  <h4 class="oc-item-name">{{ item.productName }}</h4>
                  <div class="oc-item-specs">
                    <span v-if="item.sizeValue">{{ isEn ? 'Size' : 'Kích cỡ' }}: EU {{ item.sizeValue }}</span>
                    <span v-if="item.colorName"> · {{ item.colorName }}</span>
                    <span v-if="item.sku"> · {{ item.sku }}</span>
                  </div>
                  <div class="oc-item-qty">
                    {{ formatCurrency(item.unitPrice) }} × {{ item.quantity }}
                  </div>
                </div>

                <div class="oc-item-right">
                  <strong class="oc-item-sub">{{ formatCurrency(item.subtotal) }}</strong>
                  <button
                    v-if="order.status === 'COMPLETED'"
                    type="button"
                    class="btn-item-review-link"
                    @click="openReviewModal(order, item)"
                  >
                    {{ isEn ? 'WRITE A REVIEW' : 'ĐÁNH GIÁ & ẢNH ON-FEET' }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Recipient + Footer Actions -->
            <div class="oc-footer">
              <div class="oc-footer-left">
                <div class="oc-recipient">
                  <strong>{{ order.customerName || (isEn ? 'Customer' : 'Khách hàng') }}</strong>
                  <span v-if="order.customerPhone"> · {{ order.customerPhone }}</span>
                  <span v-if="order.customerAddress"> · {{ order.customerAddress }}</span>
                </div>
                <div class="oc-total-line">
                  <span>{{ isEn ? 'Total:' : 'Tổng thanh toán:' }}</span>
                  <strong>{{ formatCurrency(order.finalAmount || order.totalAmount) }}</strong>
                </div>
              </div>

              <div class="oc-footer-actions">
                <button type="button" class="btn-oc-outline" @click="openOrderDetailDrawer(order)">
                  {{ isEn ? 'View details' : 'Chi tiết & Lịch sử' }}
                </button>

                <button
                  v-if="order.paymentMethod === 'BANK_TRANSFER' && order.status === 'PENDING'"
                  type="button"
                  class="btn-oc-black"
                  @click="openQrPayModal(order)"
                >
                  {{ isEn ? 'Pay with VietQR' : 'Quét mã VietQR' }}
                </button>

                <button
                  v-if="order.status === 'COMPLETED'"
                  type="button"
                  class="btn-oc-outline"
                  @click="openReviewForFirstItem(order)"
                >
                  {{ isEn ? 'Review' : 'Đánh giá' }}
                </button>

                <button
                  v-if="order.status === 'PENDING'"
                  type="button"
                  class="btn-oc-danger"
                  @click="openCancelModal(order)"
                >
                  {{ isEn ? 'Cancel' : 'Hủy đơn' }}
                </button>

                <button type="button" class="btn-oc-black" @click="handleReorder(order)">
                  {{ isEn ? 'Buy again →' : 'Mua lại →' }}
                </button>
              </div>
            </div>
          </article>
        </div>
      </div>

      <!-- ================================================================ -->
      <!-- EXACT LIVE ADIDAS GUEST ORDER TRACKER LINK PROMPT                -->
      <!-- ================================================================ -->
      <section class="adi-other-account-prompt">
        <p class="prompt-text">
          {{
            isEn
              ? 'Looking for an order placed from another account or as a guest?'
              : 'Bạn đang tìm đơn hàng được đặt từ một tài khoản khác?'
          }}
        </p>
        <router-link :to="lp('tracking')" class="prompt-underline-link">
          {{ isEn ? 'Check your order status' : 'Kiểm tra trạng thái đơn hàng của bạn' }}
        </router-link>
      </section>

      <!-- ================================================================ -->
      <!-- EXACT LIVE ADIDAS "CÂU HỎI THƯỜNG GẶP" 2×2 SHARP CARD GRID       -->
      <!-- Matches steps/692/media_0.jpg pixel-for-pixel                    -->
      <!-- ================================================================ -->
      <section class="adi-oh-faq-section">
        <h2 class="faq-section-heading">
          {{ isEn ? 'FREQUENTLY ASKED QUESTIONS' : 'CÂU HỎI THƯỜNG GẶP' }}
        </h2>

        <div class="adi-faq-grid-2x2">
          <article
            v-for="faq in faqItems"
            :key="faq.id"
            class="adi-faq-card"
            :class="{ open: expandedFaqId === faq.id }"
            @click="expandedFaqId = expandedFaqId === faq.id ? null : faq.id"
          >
            <div class="faq-top">
              <h3 class="faq-q">{{ faq.question }}</h3>
              <p v-if="expandedFaqId === faq.id" class="faq-answer">{{ faq.answer }}</p>
            </div>
            <div class="faq-category">{{ faq.category }}</div>
          </article>
        </div>
      </section>
    </div>

    <!-- DETAIL & SHIPMENT TIMELINE DRAWER -->
    <el-drawer
      v-model="drawerVisible"
      :title="`${isEn ? 'ORDER TRACKING' : 'HÀNH TRÌNH VẬN ĐƠN'}: #${selectedOrder?.orderCode || ''}`"
      size="540px"
      direction="rtl"
      class="order-detail-drawer"
    >
      <div v-if="selectedOrder" class="drawer-inner">
        <div class="drawer-hero-card">
          <div class="hero-top-row">
            <div>
              <span class="drawer-kicker">{{ isEn ? 'ORDER NUMBER' : 'MÃ ĐƠN HÀNG' }}</span>
              <h2 class="drawer-code">{{ selectedOrder.orderCode }}</h2>
            </div>
            <span class="oc-status-tag" :class="(selectedOrder.status || 'pending').toLowerCase()">
              {{ getStatusLabel(selectedOrder.status) }}
            </span>
          </div>

          <div class="hero-meta-grid">
            <div class="hero-meta-item">
              <span class="label">{{ isEn ? 'Date:' : 'Ngày đặt:' }}</span>
              <span class="val">{{ formatDateTime(selectedOrder.orderDate) }}</span>
            </div>
            <div class="hero-meta-item">
              <span class="label">{{ isEn ? 'Payment:' : 'Phương thức:' }}</span>
              <span class="val">{{ selectedOrder.paymentMethod === 'BANK_TRANSFER' ? 'VietQR MBBank' : 'COD' }}</span>
            </div>
            <div class="hero-meta-item">
              <span class="label">{{ isEn ? 'Recipient:' : 'Người nhận:' }}</span>
              <span class="val">{{ selectedOrder.customerName }} ({{ selectedOrder.customerPhone || '---' }})</span>
            </div>
            <div class="hero-meta-item">
              <span class="label">{{ isEn ? 'Address:' : 'Địa chỉ:' }}</span>
              <span class="val">{{ selectedOrder.customerAddress || (isEn ? 'Store pickup' : 'Nhận tại cửa hàng') }}</span>
            </div>
          </div>
        </div>

        <div class="drawer-block">
          <h3 class="drawer-block-title">{{ isEn ? 'SHIPMENT TIMELINE' : 'HÀNH TRÌNH TIẾN ĐỘ ĐƠN HÀNG' }}</h3>
          <div v-if="selectedOrder.histories && selectedOrder.histories.length > 0" class="timeline-list">
            <div v-for="(hist, idx) in selectedOrder.histories" :key="hist.id || idx" class="tl-row">
              <span class="tl-dot"></span>
              <div class="tl-body">
                <div class="tl-head">
                  <strong>{{ getStatusLabel(hist.toStatus) }}</strong>
                  <span>{{ formatDateTime(hist.changedAt) }}</span>
                </div>
                <p>{{ hist.note || (isEn ? 'Order status updated' : 'Cập nhật tiến trình đơn hàng') }}</p>
              </div>
            </div>
          </div>
          <p v-else class="tl-empty">
            {{ isEn ? 'Order recorded and awaiting warehouse confirmation.' : 'Đơn hàng đã được ghi nhận và đang chờ xác nhận.' }}
          </p>
        </div>

        <div class="drawer-block">
          <h3 class="drawer-block-title">
            {{ isEn ? 'ITEMS' : 'DANH SÁCH SẢN PHẨM' }} ({{ selectedOrder.details?.length || 0 }})
          </h3>
          <div class="drawer-items-list">
            <div v-for="item in (selectedOrder.details || [])" :key="item.id" class="d-item">
              <img v-if="item.thumbnailUrl" :src="item.thumbnailUrl" :alt="item.productName" class="d-thumb" />
              <div v-else class="d-thumb fallback">SNK</div>
              <div class="d-meta">
                <strong>{{ item.productName }}</strong>
                <span>EU {{ item.sizeValue }} · {{ item.colorName }}</span>
                <span>{{ formatCurrency(item.unitPrice) }} × {{ item.quantity }} = <strong>{{ formatCurrency(item.subtotal) }}</strong></span>
              </div>
            </div>
          </div>
        </div>

        <div class="drawer-total-box">
          <div class="dt-row">
            <span>{{ isEn ? 'Subtotal' : 'Tổng tiền hàng' }}</span>
            <span>{{ formatCurrency(selectedOrder.totalAmount) }}</span>
          </div>
          <div class="dt-row">
            <span>{{ isEn ? 'Delivery' : 'Vận chuyển' }}</span>
            <span>{{ isEn ? 'Free' : 'Miễn phí' }}</span>
          </div>
          <div class="dt-row grand">
            <span>{{ isEn ? 'TOTAL' : 'TỔNG THANH TOÁN' }}</span>
            <span>{{ formatCurrency(selectedOrder.finalAmount || selectedOrder.totalAmount) }}</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- VIETQR PAYMENT MODAL -->
    <el-dialog
      v-model="qrModalVisible"
      :title="isEn ? 'VIETQR SEPAY PAYMENT' : 'QUÉT MÃ VIETQR THANH TOÁN ĐƠN HÀNG'"
      width="440px"
      class="vietqr-dialog"
      :show-close="true"
    >
      <div v-if="qrOrder" class="qr-dialog-content">
        <div v-if="qrOrder.status === 'CONFIRMED' || isQrOrderPaid" class="qr-paid-box">
          <div class="paid-check">✓</div>
          <h4>{{ isEn ? 'PAYMENT AUTO-CONFIRMED!' : 'ĐÃ GẠCH NỢ TỰ ĐỘNG THÀNH CÔNG!' }}</h4>
          <p>
            {{
              isEn
                ? `SePay verified transfer for order #${qrOrder.orderCode}.`
                : `Cổng SePay đã khớp lệnh chuyển khoản cho đơn #${qrOrder.orderCode}.`
            }}
          </p>
          <button type="button" class="btn-oc-black full" @click="qrModalVisible = false">
            {{ isEn ? 'DONE' : 'HOÀN TẤT' }}
          </button>
        </div>

        <template v-else>
          <div class="qr-img-stage">
            <img v-if="qrOrder.qrPaymentUrl" :src="qrOrder.qrPaymentUrl" alt="VietQR" class="qr-img" />
          </div>
          <div class="qr-info-rows">
            <div><span>{{ isEn ? 'Bank:' : 'Ngân hàng:' }}</span><strong>{{ qrOrder.bankName || 'MBBank' }}</strong></div>
            <div>
              <span>{{ isEn ? 'Account:' : 'Số tài khoản:' }}</span>
              <strong class="copyable" @click="copyText(qrOrder.bankAccount || '0346771322', 'STK')">
                {{ qrOrder.bankAccount || '0346771322' }}
              </strong>
            </div>
            <div><span>{{ isEn ? 'Amount:' : 'Số tiền:' }}</span><strong>{{ formatCurrency(qrOrder.finalAmount || qrOrder.totalAmount) }}</strong></div>
            <div>
              <span>{{ isEn ? 'Reference:' : 'Nội dung CK:' }}</span>
              <strong class="copyable" @click="copyText(qrOrder.orderCode, 'Mã đơn')">{{ qrOrder.orderCode }}</strong>
            </div>
          </div>
          <button type="button" class="btn-oc-black full" @click="qrModalVisible = false">
            {{ isEn ? 'I HAVE COMPLETED TRANSFER' : 'ĐÃ HOÀN TẤT CHUYỂN KHOẢN' }}
          </button>
        </template>
      </div>
    </el-dialog>

    <!-- CANCEL ORDER MODAL -->
    <el-dialog
      v-model="cancelModalVisible"
      :title="isEn ? 'CANCEL ORDER' : 'XÁC NHẬN HỦY ĐƠN HÀNG'"
      width="460px"
      class="cancel-order-dialog"
    >
      <div v-if="orderToCancel" class="cancel-body">
        <p class="cancel-warn">
          {{
            isEn
              ? `Cancel order #${orderToCancel.orderCode}? Reserved pairs will be returned to stock.`
              : `Bạn có chắc muốn hủy đơn #${orderToCancel.orderCode}? Số lượng giày sẽ được hoàn trả về kho.`
          }}
        </p>

        <el-radio-group v-model="cancelReason" class="cancel-radios">
          <el-radio label="Đổi ý, không có nhu cầu mua nữa" />
          <el-radio label="Muốn đổi size / đổi màu sắc khác" />
          <el-radio label="Muốn thay đổi địa chỉ nhận hàng" />
          <el-radio label="Lý do khác" />
        </el-radio-group>

        <el-input
          v-if="cancelReason === 'Lý do khác'"
          v-model="cancelCustomNote"
          type="textarea"
          :rows="3"
          :placeholder="isEn ? 'Enter reason...' : 'Nhập chi tiết lý do...'"
          class="cancel-note"
        />

        <div class="cancel-btn-row">
          <button type="button" class="btn-oc-outline" @click="cancelModalVisible = false">
            {{ isEn ? 'Keep order' : 'Giữ lại đơn' }}
          </button>
          <button type="button" class="btn-oc-danger solid" :disabled="cancelling" @click="confirmCancelOrder">
            {{ cancelling ? '...' : (isEn ? 'Confirm cancel' : 'Xác nhận hủy đơn') }}
          </button>
        </div>
      </div>
    </el-dialog>

    <!-- CUSTOMER REVIEW MODAL -->
    <CustomerReviewModal
      v-model="reviewModalVisible"
      :product="reviewProduct"
      :order-code="reviewOrderCode"
      :customer-phone="reviewCustomerPhone"
      @submitted="handleReviewSubmitted"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CustomerReviewModal from '@/components/store/CustomerReviewModal.vue'
import { useClientAuthStore } from '@/stores/clientAuth'
import { useCartStore } from '@/stores/cart'
import storeService from '@/services/storeService'
import { formatCurrency, formatDateTime } from '@/utils/formatters'
import { useStoreI18n } from '@/composables/useStoreI18n'

const router = useRouter()
const authStore = useClientAuthStore()
const cartStore = useCartStore()
const { lp, isEn } = useStoreI18n()

// STATE
const loading = ref(false)
const allOrders = ref([])
const channelTab = ref('ONLINE') // 'ONLINE' | 'INSTORE'
const timeRange = ref('ALL') // 'ALL' | '30D' | '90D' | '2026'
const currentTab = ref('ALL')
const searchQuery = ref('')
const expandedFaqId = ref(null)

// REVIEW STATE
const reviewModalVisible = ref(false)
const reviewProduct = ref(null)
const reviewOrderCode = ref('')
const reviewCustomerPhone = ref('')

// DRAWER & MODALS
const drawerVisible = ref(false)
const selectedOrder = ref(null)
const qrModalVisible = ref(false)
const qrOrder = ref(null)
const isQrOrderPaid = ref(false)
let qrPollTimer = null

const cancelModalVisible = ref(false)
const orderToCancel = ref(null)
const cancelReason = ref('Đổi ý, không có nhu cầu mua nữa')
const cancelCustomNote = ref('')
const cancelling = ref(false)

const statusTabs = computed(() =>
  isEn.value
    ? [
        { label: 'All', value: 'ALL' },
        { label: 'Pending', value: 'PENDING' },
        { label: 'Confirmed', value: 'CONFIRMED' },
        { label: 'Shipping', value: 'SHIPPING' },
        { label: 'Completed', value: 'COMPLETED' },
        { label: 'Cancelled', value: 'CANCELLED' }
      ]
    : [
        { label: 'Tất cả', value: 'ALL' },
        { label: 'Chờ xác nhận', value: 'PENDING' },
        { label: 'Đã xác nhận', value: 'CONFIRMED' },
        { label: 'Đang giao', value: 'SHIPPING' },
        { label: 'Hoàn thành', value: 'COMPLETED' },
        { label: 'Đã hủy', value: 'CANCELLED' }
      ]
)

/* Exact 4 FAQ cards matching live adidas.com.vn/vi/order-history */
const faqItems = computed(() =>
  isEn.value
    ? [
        {
          id: 'return',
          question: 'HOW CAN I RETURN OR EXCHANGE AN ITEM?',
          category: 'Returns & Refunds',
          answer: 'Unworn sneakers with original tags and box can be exchanged for a new size or returned within 7 days of delivery.'
        },
        {
          id: 'refund',
          question: 'When will I receive my refund?',
          category: 'Returns & Refunds',
          answer: 'Once our warehouse receives and inspects your return, refunds are processed back to your bank account within 2–5 business days.'
        },
        {
          id: 'cancel',
          question: 'Why was my online order cancelled?',
          category: 'Ordering',
          answer: 'Orders can be cancelled by request while still Pending, or automatically if an unverified bank transfer expires.'
        },
        {
          id: 'delivery',
          question: 'When will my order be delivered?',
          category: 'DELIVERY',
          answer: 'Standard Double-Box delivery takes 1–2 days in Hanoi/HCMC and 2–4 working days nationwide.'
        }
      ]
    : [
        {
          id: 'return',
          question: 'LÀM CÁCH NÀO TÔI CÓ THỂ TRẢ HÀNG?',
          category: 'Trả Lại Hàng & Hoàn Tiền',
          answer: 'Sản phẩm còn nguyên tem mác, chưa qua sử dụng và giữ nguyên hộp giày được hỗ trợ đổi size miễn phí hoặc hoàn trả trong 7 ngày từ khi nhận hàng.'
        },
        {
          id: 'refund',
          question: 'Khi nào tôi sẽ được hoàn tiền?',
          category: 'Trả Lại Hàng & Hoàn Tiền',
          answer: 'Sau khi kho nhận lại kiện hàng và kiểm định hoàn tất, tiền hoàn sẽ được chuyển về tài khoản ngân hàng của bạn trong 2–5 ngày làm việc.'
        },
        {
          id: 'cancel',
          question: 'Tại sao đơn hàng trực tuyến của tôi bị hủy?',
          category: 'Đang đặt hàng',
          answer: 'Đơn hàng có thể bị hủy theo yêu cầu của bạn khi đang ở trạng thái Chờ xác nhận, hoặc khi giao dịch chuyển khoản chưa được hoàn tất.'
        },
        {
          id: 'delivery',
          question: 'Thời gian giao hàng là khi nào?',
          category: 'GIAO HÀNG',
          answer: 'Giao hàng Double-Box toàn quốc mất 1–2 ngày tại nội thành Hà Nội/TP.HCM và 2–4 ngày làm việc tới các tỉnh thành khác.'
        }
      ]
)

const matchesTimeRange = (order) => {
  if (timeRange.value === 'ALL' || !order.orderDate) return true
  const d = new Date(order.orderDate)
  if (Number.isNaN(d.getTime())) return true
  const now = new Date()
  if (timeRange.value === '30D') return now - d <= 30 * 86400000
  if (timeRange.value === '90D') return now - d <= 90 * 86400000
  if (timeRange.value === '2026') return d.getFullYear() === 2026
  return true
}

const channelOrders = computed(() =>
  allOrders.value.filter((o) => {
    const isStore = Boolean(o.employeeId)
    return channelTab.value === 'INSTORE' ? isStore : !isStore
  })
)

const getCountByStatus = (status) => {
  const base = channelOrders.value.filter(matchesTimeRange)
  if (status === 'ALL') return base.length
  return base.filter((o) => o.status === status).length
}

const filteredOrders = computed(() =>
  channelOrders.value.filter((order) => {
    if (!matchesTimeRange(order)) return false
    if (currentTab.value !== 'ALL' && order.status !== currentTab.value) return false
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.trim().toLowerCase()
      const matchCode = order.orderCode?.toLowerCase().includes(q)
      const matchProd = (order.details || []).some((d) => d.productName?.toLowerCase().includes(q))
      if (!matchCode && !matchProd) return false
    }
    return true
  })
)

const getStatusLabel = (status) => {
  const map = isEn.value
    ? {
        PENDING: 'Pending',
        CONFIRMED: 'Confirmed',
        SHIPPING: 'Shipping',
        COMPLETED: 'Completed',
        CANCELLED: 'Cancelled'
      }
    : {
        PENDING: 'Chờ xác nhận',
        CONFIRMED: 'Đã xác nhận',
        SHIPPING: 'Đang giao hàng',
        COMPLETED: 'Hoàn thành',
        CANCELLED: 'Đã hủy'
      }
  return map[status] || status || (isEn.value ? 'Processing' : 'Đang xử lý')
}

const fetchMyOrders = async () => {
  loading.value = true
  try {
    const params = {}
    if (authStore.isAuthenticated && authStore.user) {
      if (authStore.user.phone) params.phone = authStore.user.phone
      if (authStore.user.email) params.email = authStore.user.email
    } else {
      allOrders.value = []
      loading.value = false
      return
    }
    const res = await storeService.getMyOrders(params)
    allOrders.value = res?.data || []
  } catch (err) {
    console.error('Lỗi tải lịch sử đơn hàng:', err)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  currentTab.value = 'ALL'
  timeRange.value = 'ALL'
  searchQuery.value = ''
}

const copyOrderCode = (code) => {
  if (!code) return
  navigator.clipboard.writeText(code)
  ElMessage.success(isEn.value ? `Copied #${code}` : `Đã sao chép mã #${code}`)
}

const copyText = (text, label) => {
  if (!text) return
  navigator.clipboard.writeText(text)
  ElMessage.success(isEn.value ? `Copied ${label}` : `Đã sao chép ${label}`)
}

const openOrderDetailDrawer = (order) => {
  selectedOrder.value = order
  drawerVisible.value = true
}

const openReviewModal = (order, item) => {
  reviewOrderCode.value = order.orderCode
  reviewCustomerPhone.value = order.customerPhone || authStore.user?.phone || ''
  reviewProduct.value = {
    id: item.productId,
    productName: item.productName,
    thumbnailUrl: item.thumbnailUrl
  }
  reviewModalVisible.value = true
}

const openReviewForFirstItem = (order) => {
  if (order.details && order.details.length > 0) {
    openReviewModal(order, order.details[0])
  }
}

const handleReviewSubmitted = () => {
  ElMessage.success(isEn.value ? 'Thank you for your review!' : 'Cảm ơn bạn đã gửi đánh giá!')
  fetchMyOrders()
}

const startQrPolling = (orderCode) => {
  stopQrPolling()
  isQrOrderPaid.value = false
  if (!orderCode) return
  qrPollTimer = setInterval(async () => {
    try {
      const res = await storeService.trackOrder(orderCode)
      if (res?.data && (res.data.status === 'CONFIRMED' || res.data.status === 'COMPLETED')) {
        isQrOrderPaid.value = true
        if (qrOrder.value) qrOrder.value.status = res.data.status
        stopQrPolling()
        ElMessage.success(isEn.value ? 'SePay payment confirmed!' : 'SePay đã tự động xác nhận chuyển khoản!')
        fetchMyOrders()
      }
    } catch {
      // ignore
    }
  }, 2500)
}

const stopQrPolling = () => {
  if (qrPollTimer) {
    clearInterval(qrPollTimer)
    qrPollTimer = null
  }
}

const openQrPayModal = (order) => {
  qrOrder.value = order
  isQrOrderPaid.value = order.status === 'CONFIRMED' || order.status === 'COMPLETED'
  qrModalVisible.value = true
}

watch(qrModalVisible, (visible) => {
  if (visible && qrOrder.value && qrOrder.value.status === 'PENDING') {
    startQrPolling(qrOrder.value.orderCode)
  } else {
    stopQrPolling()
  }
})

const openCancelModal = (order) => {
  orderToCancel.value = order
  cancelReason.value = 'Đổi ý, không có nhu cầu mua nữa'
  cancelCustomNote.value = ''
  cancelModalVisible.value = true
}

const confirmCancelOrder = async () => {
  if (!orderToCancel.value) return
  cancelling.value = true
  try {
    const finalReason =
      cancelReason.value === 'Lý do khác'
        ? cancelCustomNote.value.trim() || 'Khách hàng yêu cầu hủy đơn'
        : cancelReason.value

    const res = await storeService.cancelMyOrder(orderToCancel.value.orderCode, {
      reason: finalReason,
      phone: orderToCancel.value.customerPhone || authStore.user?.phone || ''
    })
    ElMessage.success(isEn.value ? 'Order cancelled successfully.' : 'Đã hủy đơn hàng thành công!')
    const updated = res?.data
    if (updated) {
      const idx = allOrders.value.findIndex((o) => o.orderCode === updated.orderCode)
      if (idx !== -1) allOrders.value[idx] = updated
    } else {
      await fetchMyOrders()
    }
    cancelModalVisible.value = false
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || (isEn.value ? 'Failed to cancel order' : 'Không thể hủy đơn hàng'))
  } finally {
    cancelling.value = false
  }
}

const handleReorder = (order) => {
  if (!order.details || order.details.length === 0) return
  let count = 0
  for (const item of order.details) {
    cartStore.addItem(
      {
        variantId: item.productVariantId,
        productId: item.productId,
        productName: item.productName,
        sku: item.sku,
        sizeValue: item.sizeValue,
        colorName: item.colorName,
        salePrice: item.unitPrice,
        thumbnailUrl: item.thumbnailUrl
      },
      item.quantity
    )
    count += item.quantity
  }
  ElMessage.success(isEn.value ? `Added ${count} item(s) to bag!` : `Đã thêm ${count} sản phẩm vào giỏ hàng!`)
  router.push(lp('cart'))
}

watch(() => authStore.isAuthenticated, () => fetchMyOrders())

onMounted(() => {
  fetchMyOrders()
})

onUnmounted(() => {
  stopQrPolling()
})
</script>

<style scoped lang="scss">
/* Exact Live 2026 Adidas /vi/order-history Pure White Canvas */
.adi-order-history-page {
  background: #ffffff;
  color: #000000;
  padding: 40px 0 96px;
  min-height: calc(100vh - 121px);
}

.adi-oh-container {
  max-width: 1296px;
  width: 100%;
  margin: 0 auto;
  padding: 0 40px;
  box-sizing: border-box;

  @media (max-width: 1024px) {
    padding: 0 24px;
  }

  @media (max-width: 640px) {
    padding: 0 16px;
  }
}

/* ============ HEADER & CHANNEL TABS + TIMEFRAME SELECT ============ */
.adi-oh-header {
  margin-bottom: 24px;

  .adi-oh-h1 {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 40px;
    font-style: normal;
    font-weight: 800;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #000000;
    margin: 0 0 24px;
    line-height: 1.05;
  }

  .adi-oh-controls-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    gap: 20px;
    flex-wrap: wrap;
  }

  .adi-channel-tabs {
    display: flex;
    align-items: flex-end;

    .channel-tab-btn {
      height: 52px;
      padding: 0 12px;
      background: transparent;
      border: none;
      border-bottom: 1px solid #d9dbdd;
      font-family: inherit;
      font-size: 16px;
      font-weight: 400;
      color: #000000;
      cursor: pointer;

      &:first-child {
        padding-left: 2px;
      }

      &.active {
        font-weight: 700;
        border-bottom: 2px solid #000000;
      }
    }
  }

  .adi-time-filter {
    display: flex;
    align-items: center;
    gap: 14px;

    .time-filter-lbl {
      font-size: 15px;
      color: #000000;
      white-space: nowrap;
    }

    .time-select-wrap {
      position: relative;
      min-width: 196px;

      .adi-time-select {
        width: 100%;
        height: 48px;
        padding: 0 40px 0 16px;
        border: 1px solid #767677;
        border-radius: 0;
        background: #ffffff;
        font-family: inherit;
        font-size: 15px;
        color: #555555;
        appearance: none;
        cursor: pointer;
        outline: none;

        &:focus {
          border-color: #000000;
          color: #000000;
        }
      }

      .select-chev {
        position: absolute;
        right: 14px;
        top: 50%;
        transform: translateY(-50%);
        width: 18px;
        height: 18px;
        pointer-events: none;
        color: #000000;
      }
    }
  }
}

/* Secondary Status Toolbar (when orders exist) */
.adi-orders-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 0;
  margin-bottom: 20px;
  border-top: 1px solid #eceff1;
  border-bottom: 1px solid #eceff1;

  .status-pills-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .st-pill-btn {
      height: 34px;
      padding: 0 12px;
      background: #ffffff;
      border: 1px solid #d9dbdd;
      border-radius: 0;
      font-family: inherit;
      font-size: 12.5px;
      font-weight: 700;
      color: #000000;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;

      .st-cnt {
        font-size: 11px;
        color: #767677;
      }

      &.active {
        background: #000000;
        border-color: #000000;
        color: #ffffff;

        .st-cnt {
          color: #cccccc;
        }
      }
    }
  }

  .toolbar-search-right {
    display: flex;
    gap: 8px;

    .oh-search-input {
      height: 36px;
      width: 240px;
      padding: 0 12px;
      border: 1px solid #767677;
      border-radius: 0;
      font-family: inherit;
      font-size: 13px;
      outline: none;

      &:focus {
        border-color: #000000;
      }
    }

    .btn-oh-refresh {
      height: 36px;
      padding: 0 14px;
      background: #ffffff;
      border: 1px solid #000000;
      border-radius: 0;
      font-family: inherit;
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;

      &:hover {
        background: #000000;
        color: #ffffff;
      }
    }
  }
}

/* ============ EXACT LIVE ADIDAS CENTERED EMPTY STATE ============ */
.oh-loading-wrap {
  padding: 40px 0;
}

.adi-oh-empty {
  text-align: center;
  padding: 68px 16px 80px;

  .empty-h2 {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 40px;
    font-style: normal;
    font-weight: 800;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #000000;
    margin: 0 0 10px;
    line-height: 1.05;
  }

  .empty-sub {
    font-size: 15.5px;
    color: #000000;
    margin: 0 0 24px;
  }

  .btn-adi-start-shopping {
    position: relative;
    height: 48px;
    padding: 0 24px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    border-radius: 0;
    font-family: inherit;
    font-size: 15px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    gap: 18px;
    cursor: pointer;

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      transform: translate(3px, 3px);
      border: 1px solid #000000;
      pointer-events: none;
    }

    .arrow {
      font-size: 18px;
      line-height: 1;
    }

    &:hover {
      color: #cccccc;
    }
  }
}

/* ============ POPULATED SHARP ADIDAS ORDER CARDS ============ */
.adi-orders-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 44px;
}

.adi-order-card {
  border: 1px solid #d9dbdd;
  background: #ffffff;
  transition: border-color 0.14s ease;

  &:hover {
    border-color: #000000;
  }

  .oc-header {
    padding: 16px 24px;
    border-bottom: 1px solid #eceff1;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;

    .oc-meta-left,
    .oc-meta-right {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .oc-code-btn {
      background: #000000;
      color: #ffffff;
      border: none;
      padding: 4px 10px;
      font-family: monospace;
      font-size: 13px;
      font-weight: 800;
      cursor: pointer;
    }

    .oc-date {
      font-size: 13.5px;
      color: #555555;
    }

    .oc-channel-tag,
    .oc-pay-tag {
      font-size: 11.5px;
      font-weight: 700;
      padding: 3px 8px;
      background: #eceff1;
      color: #000000;
    }
  }

  .oc-items {
    padding: 18px 24px;
    display: flex;
    flex-direction: column;
    gap: 14px;

    .oc-item-row {
      display: flex;
      gap: 16px;
      align-items: center;

      @media (max-width: 600px) {
        flex-wrap: wrap;
      }

      .oc-thumb {
        width: 84px;
        height: 84px;
        background: #eceff1;
        flex-shrink: 0;
        overflow: hidden;
        display: flex;
        align-items: center;
        justify-content: center;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .thumb-fallback {
          font-size: 12px;
          font-weight: 800;
          color: #767677;
        }
      }

      .oc-item-info {
        flex: 1;
        min-width: 180px;

        .oc-item-name {
          font-size: 14.5px;
          font-weight: 700;
          margin: 0 0 4px;
        }

        .oc-item-specs {
          font-size: 13px;
          color: #555555;
          margin-bottom: 4px;
        }

        .oc-item-qty {
          font-size: 13px;
          color: #767677;
        }
      }

      .oc-item-right {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 6px;

        .oc-item-sub {
          font-size: 14.5px;
          font-weight: 800;
        }

        .btn-item-review-link {
          background: none;
          border: none;
          border-bottom: 1.5px solid #000000;
          padding: 1px 0;
          font-family: inherit;
          font-size: 11px;
          font-weight: 800;
          cursor: pointer;
        }
      }
    }
  }

  .oc-footer {
    padding: 16px 24px;
    background: #fafafa;
    border-top: 1px solid #eceff1;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;

    .oc-footer-left {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .oc-recipient {
        font-size: 13px;
        color: #555555;
      }

      .oc-total-line {
        font-size: 14.5px;
        display: flex;
        gap: 8px;
        align-items: baseline;

        strong {
          font-size: 16px;
          font-weight: 800;
        }
      }
    }

    .oc-footer-actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
  }
}

.oc-status-tag {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 4px 10px;
  background: #eceff1;
  color: #000000;

  &.completed {
    background: #427f5c;
    color: #ffffff;
  }
  &.cancelled {
    background: #e32b2b;
    color: #ffffff;
  }
  &.shipping,
  &.confirmed {
    background: #000000;
    color: #ffffff;
  }
}

.btn-oc-outline,
.btn-oc-black,
.btn-oc-danger {
  height: 38px;
  padding: 0 16px;
  border-radius: 0;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;

  &.full {
    width: 100%;
    height: 44px;
  }
}

.btn-oc-outline {
  background: #ffffff;
  color: #000000;
  border: 1px solid #000000;

  &:hover {
    background: #eceff1;
  }
}

.btn-oc-black {
  background: #000000;
  color: #ffffff;
  border: 1px solid #000000;

  &:hover {
    background: #2b2b2b;
  }
}

.btn-oc-danger {
  background: #ffffff;
  color: #e32b2b;
  border: 1px solid #e32b2b;

  &:hover,
  &.solid {
    background: #e32b2b;
    color: #ffffff;
  }
}

/* ============ EXACT LIVE ADIDAS GUEST ORDER TRACKER PROMPT ============ */
.adi-other-account-prompt {
  padding: 12px 0 44px;

  .prompt-text {
    font-size: 15px;
    color: #000000;
    margin: 0 0 8px;
  }

  .prompt-underline-link {
    display: inline-block;
    font-size: 15px;
    font-weight: 700;
    color: #000000;
    text-decoration: none;
    border-bottom: 2px solid #000000;
    padding-bottom: 2px;

    &:hover {
      background: #000000;
      color: #ffffff;
    }
  }
}

/* ============ EXACT LIVE ADIDAS "CÂU HỎI THƯỜNG GẶP" 2×2 CARDS ============ */
.adi-oh-faq-section {
  padding-top: 44px;
  border-top: 1px solid #d9dbdd;

  .faq-section-heading {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 28px;
    font-style: normal;
    font-weight: 800;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: #000000;
    margin: 0 0 24px;
  }

  .adi-faq-grid-2x2 {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 24px;

    @media (max-width: 720px) {
      grid-template-columns: 1fr;
    }
  }

  .adi-faq-card {
    min-height: 128px;
    padding: 24px;
    background: #ffffff;
    border: 1px solid #d9dbdd;
    border-radius: 0;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 18px;
    cursor: pointer;
    box-sizing: border-box;
    transition: border-color 0.14s ease;

    &:hover,
    &.open {
      border-color: #000000;
    }

    .faq-q {
      font-size: 16px;
      font-weight: 700;
      color: #000000;
      margin: 0;
      line-height: 1.4;
    }

    .faq-answer {
      font-size: 14px;
      line-height: 1.55;
      color: #333333;
      margin: 12px 0 0;
    }

    .faq-category {
      font-size: 14px;
      color: #767677;
    }
  }
}

/* Drawer & Modal Inner Styles */
.drawer-inner {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.drawer-hero-card {
  background: #000000;
  color: #ffffff;
  padding: 20px;

  .hero-top-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 14px;
  }

  .drawer-kicker {
    font-size: 10px;
    letter-spacing: 0.12em;
    color: #a3a3a3;
  }

  .drawer-code {
    margin: 2px 0 0;
    font-family: monospace;
    font-size: 20px;
  }

  .hero-meta-grid {
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 13px;

    .label {
      color: #a3a3a3;
      margin-right: 6px;
    }
  }
}

.drawer-block {
  .drawer-block-title {
    font-size: 12.5px;
    font-weight: 800;
    letter-spacing: 0.08em;
    margin: 0 0 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #eceff1;
  }
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .tl-row {
    display: flex;
    gap: 12px;
    align-items: flex-start;

    .tl-dot {
      width: 10px;
      height: 10px;
      background: #000000;
      margin-top: 5px;
      flex-shrink: 0;
    }

    .tl-body {
      flex: 1;

      .tl-head {
        display: flex;
        justify-content: space-between;
        font-size: 13px;
      }

      p {
        margin: 3px 0 0;
        font-size: 12.5px;
        color: #555555;
      }
    }
  }
}

.tl-empty {
  font-size: 13px;
  color: #555555;
}

.drawer-items-list {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .d-item {
    display: flex;
    gap: 12px;
    align-items: center;

    .d-thumb {
      width: 56px;
      height: 56px;
      object-fit: cover;
      background: #eceff1;

      &.fallback {
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
        font-weight: 800;
      }
    }

    .d-meta {
      display: flex;
      flex-direction: column;
      gap: 2px;
      font-size: 13px;
    }
  }
}

.drawer-total-box {
  border-top: 1px solid #000000;
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  .dt-row {
    display: flex;
    justify-content: space-between;
    font-size: 13.5px;

    &.grand {
      margin-top: 6px;
      padding-top: 8px;
      border-top: 1px solid #eceff1;
      font-size: 15px;
      font-weight: 800;
    }
  }
}

.qr-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .qr-paid-box {
    text-align: center;
    padding: 16px 0;

    .paid-check {
      width: 48px;
      height: 48px;
      margin: 0 auto 12px;
      background: #427f5c;
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      font-weight: 800;
    }
  }

  .qr-img-stage {
    text-align: center;

    .qr-img {
      width: 220px;
      height: 220px;
      border: 1px solid #d9dbdd;
      object-fit: contain;
    }
  }

  .qr-info-rows {
    display: flex;
    flex-direction: column;
    gap: 8px;
    font-size: 13.5px;

    > div {
      display: flex;
      justify-content: space-between;
      border-bottom: 1px solid #eceff1;
      padding-bottom: 6px;
    }

    .copyable {
      text-decoration: underline;
      cursor: pointer;
    }
  }
}

.cancel-body {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .cancel-warn {
    margin: 0;
    font-size: 13.5px;
    line-height: 1.5;
    padding: 12px;
    background: #eceff1;
  }

  .cancel-radios {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .cancel-btn-row {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 6px;
  }
}
</style>
