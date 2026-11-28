<template>
  <div class="store-track-order">
    <div class="adi-track-container">
      <!-- Exact Live 2026 Adidas Left-Aligned Borderless Order Tracker Form (600px column) -->
      <section class="adi-track-form-col">
        <h1 class="track-headline">
          {{ isEn ? 'ORDERS AND RETURNS' : 'ĐƠN HÀNG VÀ TRẢ HÀNG' }}
        </h1>
        <p class="track-intro">
          <template v-if="isEn">
            Track your order by entering your order number or phone number below. You can
            <button type="button" class="inline-underline-link" @click="actionMode = 'return'">exchange or return</button>
            eligible sneakers within 7 days of delivery.
          </template>
          <template v-else>
            Theo dõi đơn hàng của bạn bằng cách nhập mã đơn hàng hoặc số điện thoại bên dưới. Bạn có thể
            <button type="button" class="inline-underline-link" @click="actionMode = 'return'">đổi trả</button>
            sản phẩm trong vòng 7 ngày sau khi nhận được đơn hàng.
          </template>
        </p>

        <form class="adi-stacked-form" @submit.prevent="handleSearch">
          <div class="adi-field-group">
            <input
              type="text"
              v-model="searchCode"
              :placeholder="isEn ? 'Order number *' : 'Mã đơn hàng *'"
              class="adi-sharp-input"
            />
            <span class="field-helper">
              {{ isEn ? 'You can find this in your confirmation email or My Orders (e.g. HD0012)' : 'Bạn có thể tìm thấy thông tin này trong email xác nhận (VD: HD0012)' }}
            </span>
          </div>

          <div class="adi-field-group">
            <input
              type="text"
              v-model="searchPhone"
              :placeholder="isEn ? 'Phone number (optional)' : 'Số điện thoại (tùy chọn)'"
              class="adi-sharp-input"
            />
          </div>

          <!-- Exact Live Adidas "BẠN MUỐN LÀM GÌ?" Radio Rows -->
          <div class="what-to-do-block">
            <h2 class="sub-heading">{{ isEn ? 'WHAT WOULD YOU LIKE TO DO?' : 'BẠN MUỐN LÀM GÌ?' }}</h2>

            <label class="adi-action-radio" @click="actionMode = 'track'">
              <span class="radio-circle" :class="{ checked: actionMode === 'track' }">
                <span class="radio-dot"></span>
              </span>
              <span>{{ isEn ? 'Track my order' : 'Theo dõi đơn hàng' }}</span>
            </label>

            <label class="adi-action-radio" @click="actionMode = 'return'">
              <span class="radio-circle" :class="{ checked: actionMode === 'return' }">
                <span class="radio-dot"></span>
              </span>
              <span>{{ isEn ? 'Exchange or return an item' : 'Đổi hàng hoặc trả hàng' }}</span>
            </label>
          </div>

          <div v-if="actionMode === 'return'" class="return-policy-note">
            {{
              isEn
                ? 'Enter your order number above to check eligibility. Unworn sneakers with original box & tags qualify for free size exchange within 7 days.'
                : 'Nhập mã đơn hàng ở trên để kiểm tra tình trạng đơn. Giày còn nguyên hộp và tem mác được hỗ trợ đổi size miễn phí trong 7 ngày.'
            }}
          </div>

          <button type="submit" class="btn-adi-start" :disabled="searching">
            <span>{{ searching ? (isEn ? 'SEARCHING...' : 'ĐANG TRA CỨU...') : (isEn ? 'CONTINUE' : 'BẮT ĐẦU') }}</span>
            <span class="btn-arrow">→</span>
          </button>
        </form>
      </section>

      <!-- State: Loading -->
      <div v-if="searching" class="track-loading-box">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- State: Not Found -->
      <div v-else-if="hasSearched && !orderData" class="track-empty-box">
        <h3 class="empty-title">{{ isEn ? 'ORDER NOT FOUND' : 'KHÔNG TÌM THẤY THÔNG TIN ĐƠN HÀNG' }}</h3>
        <p class="empty-desc">
          {{
            isEn
              ? 'Please verify your order number or phone number and try again. For urgent help, call hotline 1900 6868.'
              : 'Vui lòng kiểm tra lại mã đơn hàng hoặc số điện thoại bạn đã nhập. Nếu cần hỗ trợ khẩn cấp, vui lòng gọi hotline 1900 6868.'
          }}
        </p>
        <button type="button" class="btn-reset-track" @click="resetSearch">
          {{ isEn ? 'TRY AGAIN' : 'THỬ LẠI' }}
        </button>
      </div>

      <!-- State: Order Result Found -->
      <div v-else-if="orderData" class="track-result-area">
        <!-- Top Summary Bar -->
        <div class="order-hero-card">
          <div class="order-meta-col">
            <span class="meta-label">{{ isEn ? 'ORDER NUMBER' : 'MÃ ĐƠN HÀNG' }}</span>
            <span class="meta-code">{{ orderData.orderCode }}</span>
          </div>
          <div class="order-meta-col">
            <span class="meta-label">{{ isEn ? 'ORDER DATE' : 'NGÀY TẠO ĐƠN' }}</span>
            <span class="meta-val">{{ formatDate(orderData.orderDate) }}</span>
          </div>
          <div class="order-meta-col">
            <span class="meta-label">{{ isEn ? 'PAYMENT METHOD' : 'PHƯƠNG THỨC' }}</span>
            <span class="meta-val">
              {{ orderData.paymentMethod === 'BANK_TRANSFER' ? 'VietQR / SePay' : (isEn ? 'Cash on Delivery (COD)' : 'Thanh toán COD') }}
            </span>
          </div>
          <div class="order-status-badge" :class="orderData.status ? orderData.status.toLowerCase() : 'confirmed'">
            {{ formatStatus(orderData.status) }}
          </div>
        </div>

        <!-- Tracking Progress Stepper -->
        <div class="progress-box">
          <h3 class="progress-title">{{ isEn ? 'SHIPMENT PROGRESS' : 'HÀNH TRÌNH KIỆN HÀNG' }}</h3>
          <div class="progress-steps-row">
            <div
              v-for="(step, idx) in stepsList"
              :key="idx"
              class="step-item"
              :class="{
                completed: stepActiveIndex > idx + 1,
                active: stepActiveIndex === idx + 1,
                pending: stepActiveIndex < idx + 1
              }"
            >
              <div class="step-marker">
                <span v-if="stepActiveIndex > idx + 1">✓</span>
                <span v-else>{{ idx + 1 }}</span>
              </div>
              <div class="step-text">
                <span class="step-name">{{ step.title }}</span>
                <span class="step-sub">{{ step.desc }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="track-details-grid">
          <!-- Left: Shipping & Customer -->
          <div class="detail-panel">
            <h3 class="panel-heading">{{ isEn ? 'DELIVERY DETAILS' : 'THÔNG TIN NHẬN HÀNG' }}</h3>
            <div class="info-specs">
              <div class="spec-item">
                <span class="lbl">{{ isEn ? 'Recipient:' : 'Họ tên:' }}</span>
                <span class="val">{{ orderData.customerName }}</span>
              </div>
              <div class="spec-item">
                <span class="lbl">{{ isEn ? 'Phone:' : 'Số điện thoại:' }}</span>
                <span class="val">{{ orderData.customerPhone }}</span>
              </div>
              <div class="spec-item">
                <span class="lbl">{{ isEn ? 'Address:' : 'Địa chỉ:' }}</span>
                <span class="val">{{ orderData.shippingAddress }}</span>
              </div>
              <div class="spec-item" v-if="orderData.note">
                <span class="lbl">{{ isEn ? 'Note:' : 'Ghi chú:' }}</span>
                <span class="val">{{ orderData.note }}</span>
              </div>
            </div>

            <!-- SePay QR reminder if needed -->
            <div v-if="orderData.paymentMethod === 'BANK_TRANSFER' && orderData.qrPaymentUrl" class="qr-quick-box">
              <span class="qr-quick-title">{{ isEn ? 'SEPAY QR FOR THIS ORDER' : 'MÃ QR SEPAY ĐƠN HÀNG NÀY' }}</span>
              <div class="qr-flex">
                <img :src="orderData.qrPaymentUrl" alt="SePay QR" class="qr-small" />
                <div class="qr-text">
                  <p>{{ orderData.bankName || 'MBBank' }}: <strong>{{ orderData.bankAccount || '0346771322' }}</strong></p>
                  <p>{{ isEn ? 'Reference:' : 'Nội dung:' }} <strong>{{ orderData.orderCode }}</strong></p>
                </div>
              </div>
            </div>
          </div>

          <!-- Right: Items list -->
          <div class="detail-panel">
            <h3 class="panel-heading">
              {{ isEn ? 'ORDER ITEMS' : 'SẢN PHẨM TRONG ĐƠN' }} ({{ (orderData.items && orderData.items.length) || 0 }})
            </h3>
            <div class="order-items-table">
              <div
                v-for="(item, idx) in orderData.items"
                :key="idx"
                class="track-item-row"
              >
                <img
                  :src="item.productImageUrl || 'https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&w=150&q=80'"
                  :alt="item.productName"
                  class="item-img"
                />
                <div class="item-meta">
                  <h4 class="item-name">{{ item.productName }}</h4>
                  <div class="item-tags">
                    <span v-if="item.sizeValue" class="tag">Size: {{ item.sizeValue }}</span>
                    <span v-if="item.colorName" class="tag">{{ item.colorName }}</span>
                    <span class="qty">x{{ item.quantity }}</span>
                  </div>
                </div>
                <div class="item-sum">
                  {{ formatCurrency(item.unitPrice * item.quantity) }}
                </div>
              </div>
            </div>

            <div class="totals-bar">
              <div class="bar-line">
                <span>{{ isEn ? 'Subtotal' : 'Tổng tiền hàng' }}</span>
                <span>{{ formatCurrency(orderData.totalAmount) }}</span>
              </div>
              <div class="bar-line">
                <span>{{ isEn ? 'Delivery' : 'Vận chuyển' }}</span>
                <span class="free">{{ isEn ? 'Free' : 'Miễn phí' }}</span>
              </div>
              <div class="bar-line grand">
                <span>{{ isEn ? 'TOTAL' : 'TỔNG THANH TOÁN' }}</span>
                <span class="grand-num">{{ formatCurrency(orderData.finalAmount || orderData.totalAmount) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Exact Live Adidas 4-Item FAQ Strip Below -->
      <section class="adi-track-faq">
        <h2 class="faq-heading">{{ isEn ? 'FREQUENTLY ASKED QUESTIONS' : 'CÂU HỎI THƯỜNG GẶP' }}</h2>
        <div class="faq-grid">
          <div class="faq-item">
            <h4>{{ isEn ? 'Where is my order number?' : 'Tôi tìm mã đơn hàng ở đâu?' }}</h4>
            <p>{{ isEn ? 'Your order number (e.g. HD0012) is shown on the confirmation screen, in your email, and inside My Orders.' : 'Mã đơn hàng (VD: HD0012) hiển thị ngay sau khi đặt hàng, trong email xác nhận và mục Đơn hàng của tôi.' }}</p>
          </div>
          <div class="faq-item">
            <h4>{{ isEn ? 'How long does shipping take?' : 'Thời gian giao hàng mất bao lâu?' }}</h4>
            <p>{{ isEn ? '1–2 days in Hanoi/HCMC and 2–4 working days nationwide. Every pair is shipped Double-Boxed.' : '1–2 ngày tại nội thành Hà Nội/TP.HCM và 2–4 ngày toàn quốc. Mọi đơn đều đóng gói Double-Box chống móp hộp.' }}</p>
          </div>
          <div class="faq-item">
            <h4>{{ isEn ? 'Can I exchange my sneaker size?' : 'Tôi có thể đổi size giày không?' }}</h4>
            <p>{{ isEn ? 'Yes — free size exchanges within 7 days of delivery for unworn pairs with original tags and box.' : 'Có — hỗ trợ đổi size miễn phí trong 7 ngày từ khi nhận hàng nếu giày chưa qua sử dụng và còn nguyên tem hộp.' }}</p>
          </div>
          <div class="faq-item">
            <h4>{{ isEn ? 'How does VietQR SePay confirmation work?' : 'Chuyển khoản VietQR xác nhận thế nào?' }}</h4>
            <p>{{ isEn ? 'Keep the exact order code in the transfer note; SePay automatically confirms payment within seconds.' : 'Giữ nguyên mã đơn hàng trong nội dung chuyển khoản, hệ thống SePay tự động gạch nợ trong vài giây.' }}</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import storeService from '@/services/storeService'
import { formatCurrency } from '@/utils/formatters'
import { useStoreI18n } from '@/composables/useStoreI18n'

const route = useRoute()
const { isEn } = useStoreI18n()

const searchCode = ref('')
const searchPhone = ref('')
const actionMode = ref('track')
const searching = ref(false)
const hasSearched = ref(false)
const orderData = ref(null)

const stepsList = computed(() =>
  isEn.value
    ? [
        { title: 'Order received', desc: 'Recorded in system' },
        { title: 'Warehouse verified', desc: 'Legit Check & Double-Box' },
        { title: 'In transit', desc: 'Handed to courier' },
        { title: 'Delivered', desc: 'Received by customer' }
      ]
    : [
        { title: 'Tiếp nhận đơn', desc: 'Đơn hàng được ghi nhận' },
        { title: 'Xác nhận kho', desc: 'Kiểm tra & đóng gói' },
        { title: 'Đang vận chuyển', desc: 'Bàn giao đơn vị giao vận' },
        { title: 'Giao thành công', desc: 'Khách hàng nhận giày' }
      ]
)

const stepActiveIndex = computed(() => {
  if (!orderData.value) return 1
  const st = orderData.value.status
  if (st === 'COMPLETED') return 4
  if (st === 'SHIPPING') return 3
  if (st === 'CONFIRMED') return 2
  if (st === 'PENDING') return 1
  return 1
})

onMounted(() => {
  const code = route.query.code
  const phone = route.query.phone
  if (code) {
    searchCode.value = code
    if (phone) searchPhone.value = phone
    handleSearch()
  }
})

const handleSearch = async () => {
  if (!searchCode.value && !searchPhone.value) {
    ElMessage.warning(
      isEn.value
        ? 'Please enter your order number or phone number to track'
        : 'Vui lòng nhập mã đơn hàng hoặc số điện thoại để tra cứu'
    )
    return
  }

  searching.value = true
  hasSearched.value = true
  try {
    const res = await storeService.trackOrder(searchCode.value.trim(), searchPhone.value.trim() || undefined)
    orderData.value = res?.data || null
  } catch (err) {
    console.error('Lỗi tra cứu đơn:', err)
    orderData.value = null
  } finally {
    searching.value = false
  }
}

const resetSearch = () => {
  searchCode.value = ''
  searchPhone.value = ''
  hasSearched.value = false
  orderData.value = null
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    return d.toLocaleString(isEn.value ? 'en-US' : 'vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return dateStr
  }
}

const formatStatus = (st) => {
  const map = isEn.value
    ? {
        DRAFT: 'DRAFT',
        PENDING: 'PENDING',
        CONFIRMED: 'CONFIRMED',
        SHIPPING: 'SHIPPING',
        COMPLETED: 'COMPLETED',
        CANCELLED: 'CANCELLED'
      }
    : {
        DRAFT: 'ĐƠN NHÁP',
        PENDING: 'CHỜ XÁC NHẬN',
        CONFIRMED: 'ĐÃ XÁC NHẬN',
        SHIPPING: 'ĐANG GIAO HÀNG',
        COMPLETED: 'HOÀN THÀNH',
        CANCELLED: 'ĐÃ HỦY'
      }
  return map[st] || st || (isEn.value ? 'PENDING' : 'CHỜ XÁC NHẬN')
}
</script>

<style scoped lang="scss">
.store-track-order {
  padding: 48px 0 96px;
  background: #ffffff;
  color: #000000;
}

.adi-track-container {
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

/* Exact Live Adidas Left-Aligned 600px Column (NO Outer Box) */
.adi-track-form-col {
  max-width: 600px;
  margin-bottom: 56px;

  .track-headline {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 44px;
    font-style: normal;
    font-weight: 800;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: #000000;
    margin: 0 0 14px;
    line-height: 1.05;

    @media (max-width: 640px) {
      font-size: 34px;
    }
  }

  .track-intro {
    font-size: 15px;
    line-height: 1.55;
    color: #000000;
    margin: 0 0 32px;

    .inline-underline-link {
      background: none;
      border: none;
      padding: 0;
      font-family: inherit;
      font-size: inherit;
      font-weight: 700;
      color: #000000;
      text-decoration: underline;
      cursor: pointer;
    }
  }

  .adi-stacked-form {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .adi-field-group {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .adi-sharp-input {
        width: 100%;
        height: 52px;
        padding: 0 16px;
        border: 1px solid #767677;
        border-radius: 0;
        background: #ffffff;
        font-family: inherit;
        font-size: 15px;
        color: #000000;
        box-sizing: border-box;
        outline: none;
        transition: border-color 0.14s ease;

        &::placeholder {
          color: #767677;
        }

        &:focus {
          border-color: #000000;
          border-width: 1.5px;
        }
      }

      .field-helper {
        font-size: 13px;
        color: #767677;
        padding-left: 14px;
      }
    }

    .what-to-do-block {
      margin-top: 16px;

      .sub-heading {
        font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
        font-size: 22px;
        font-weight: 800;
        letter-spacing: 0.05em;
        text-transform: uppercase;
        color: #000000;
        margin: 0 0 16px;
      }

      .adi-action-radio {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 18px 0;
        border-top: 1px solid #d9dbdd;
        font-size: 15px;
        color: #000000;
        cursor: pointer;
        user-select: none;

        &:last-of-type {
          border-bottom: 1px solid #d9dbdd;
        }

        .radio-circle {
          width: 24px;
          height: 24px;
          border-radius: 50%;
          border: 1.5px solid #767677;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;

          .radio-dot {
            width: 13px;
            height: 13px;
            border-radius: 50%;
            background: transparent;
          }

          &.checked {
            border-color: #000000;
            .radio-dot {
              background: #000000;
            }
          }
        }
      }
    }

    .return-policy-note {
      padding: 14px 16px;
      background: #eceff1;
      font-size: 13.5px;
      line-height: 1.5;
      color: #000000;
    }

    .btn-adi-start {
      position: relative;
      align-self: flex-start;
      margin-top: 8px;
      height: 50px;
      padding: 0 26px;
      background: #000000;
      color: #ffffff;
      border: 1px solid #000000;
      border-radius: 0;
      font-family: inherit;
      font-size: 14px;
      font-weight: 800;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      display: inline-flex;
      align-items: center;
      gap: 20px;
      cursor: pointer;

      &::after {
        content: '';
        position: absolute;
        inset: 0;
        transform: translate(3px, 3px);
        border: 1px solid #000000;
        pointer-events: none;
      }

      .btn-arrow {
        font-size: 18px;
        line-height: 1;
      }

      &:hover:not(:disabled) {
        color: #cccccc;
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
  }
}

.track-loading-box {
  max-width: 900px;
  margin-bottom: 40px;
}

/* Empty State */
.track-empty-box {
  max-width: 600px;
  padding: 28px;
  border: 1px solid #000000;
  background: #eceff1;
  margin-bottom: 48px;

  .empty-title {
    font-family: 'Barlow Condensed', sans-serif;
    font-size: 24px;
    font-weight: 800;
    letter-spacing: 0.04em;
    margin: 0 0 8px;
  }

  .empty-desc {
    font-size: 14px;
    color: #333333;
    margin: 0 0 20px;
    line-height: 1.55;
  }

  .btn-reset-track {
    height: 42px;
    padding: 0 22px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    border-radius: 0;
    font-family: inherit;
    font-size: 12.5px;
    font-weight: 800;
    letter-spacing: 0.1em;
    cursor: pointer;
  }
}

/* Tracking Result Area */
.track-result-area {
  margin-bottom: 64px;

  .order-hero-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #000000;
    color: #ffffff;
    padding: 24px 30px;
    margin-bottom: 24px;
    flex-wrap: wrap;
    gap: 20px;

    .order-meta-col {
      display: flex;
      flex-direction: column;

      .meta-label {
        font-size: 10px;
        font-weight: 700;
        letter-spacing: 0.12em;
        color: #a3a3a3;
      }

      .meta-code {
        font-size: 18px;
        font-weight: 800;
        font-family: monospace;
        margin-top: 3px;
      }

      .meta-val {
        font-size: 13.5px;
        font-weight: 600;
        margin-top: 3px;
      }
    }

    .order-status-badge {
      padding: 6px 14px;
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.08em;
      background: #427f5c;
      color: #ffffff;

      &.pending {
        background: #b45309;
      }
      &.cancelled {
        background: #e32b2b;
      }
    }
  }

  .progress-box {
    background: #ffffff;
    border: 1px solid #d9dbdd;
    padding: 28px 30px;
    margin-bottom: 24px;

    .progress-title {
      font-size: 13px;
      font-weight: 800;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      margin: 0 0 24px;
    }

    .progress-steps-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;

      @media (max-width: 700px) {
        grid-template-columns: 1fr;
      }

      .step-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;

        .step-marker {
          width: 28px;
          height: 28px;
          border-radius: 50%;
          border: 2px solid #d3d7da;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 11px;
          font-weight: 800;
          color: #767677;
          flex-shrink: 0;
        }

        .step-text {
          display: flex;
          flex-direction: column;

          .step-name {
            font-size: 13.5px;
            font-weight: 700;
            color: #000000;
          }

          .step-sub {
            font-size: 12px;
            color: #767677;
            margin-top: 2px;
          }
        }

        &.completed .step-marker {
          background: #000000;
          border-color: #000000;
          color: #ffffff;
        }

        &.active .step-marker {
          background: #ffffff;
          border-color: #000000;
          color: #000000;
        }

        &.pending {
          opacity: 0.5;
        }
      }
    }
  }

  .track-details-grid {
    display: grid;
    grid-template-columns: 1fr 1.2fr;
    gap: 24px;

    @media (max-width: 800px) {
      grid-template-columns: 1fr;
    }

    .detail-panel {
      background: #ffffff;
      border: 1px solid #d9dbdd;
      padding: 24px;

      .panel-heading {
        font-size: 13px;
        font-weight: 800;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        margin: 0 0 16px;
        padding-bottom: 12px;
        border-bottom: 1px solid #eceff1;
      }
    }

    .info-specs {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .spec-item {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        font-size: 13.5px;
        padding-bottom: 8px;
        border-bottom: 1px solid #f2f2f2;

        &:last-child {
          border-bottom: none;
        }

        .lbl {
          color: #767677;
        }

        .val {
          color: #000000;
          font-weight: 600;
          text-align: right;
        }
      }
    }

    .qr-quick-box {
      margin-top: 20px;
      padding: 16px;
      background: #eceff1;

      .qr-quick-title {
        font-size: 10.5px;
        font-weight: 800;
        letter-spacing: 0.08em;
        color: #000000;
        display: block;
        margin-bottom: 10px;
      }

      .qr-flex {
        display: flex;
        gap: 14px;
        align-items: center;

        .qr-small {
          width: 84px;
          height: 84px;
          border: 1px solid #d9dbdd;
          background: #ffffff;
        }

        .qr-text p {
          margin: 0 0 4px;
          font-size: 13px;
        }
      }
    }

    .order-items-table {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 16px;

      .track-item-row {
        display: flex;
        gap: 12px;
        align-items: center;
        padding-bottom: 12px;
        border-bottom: 1px solid #f2f2f2;

        &:last-child {
          border-bottom: none;
          padding-bottom: 0;
        }

        .item-img {
          width: 56px;
          height: 56px;
          background: #eceff1;
          object-fit: cover;
          flex-shrink: 0;
        }

        .item-meta {
          flex: 1;

          .item-name {
            font-size: 13.5px;
            font-weight: 700;
            margin: 0 0 4px;
          }

          .item-tags {
            display: flex;
            gap: 6px;
            align-items: center;

            .tag {
              font-size: 11px;
              background: #eceff1;
              padding: 2px 6px;
            }

            .qty {
              font-size: 12px;
              font-weight: 700;
            }
          }
        }

        .item-sum {
          font-size: 13.5px;
          font-weight: 800;
        }
      }
    }

    .totals-bar {
      border-top: 1px solid #d9dbdd;
      padding-top: 12px;
      display: flex;
      flex-direction: column;
      gap: 6px;

      .bar-line {
        display: flex;
        justify-content: space-between;
        font-size: 13.5px;

        .free {
          color: #427f5c;
          font-weight: 700;
        }

        &.grand {
          margin-top: 8px;
          padding-top: 10px;
          border-top: 1px solid #000000;
          font-weight: 800;

          .grand-num {
            font-size: 16px;
          }
        }
      }
    }
  }
}

/* Adidas 4-Item FAQ Strip */
.adi-track-faq {
  padding-top: 40px;
  border-top: 1px solid #d9dbdd;

  .faq-heading {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 28px;
    font-weight: 800;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    margin: 0 0 24px;
  }

  .faq-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 24px;

    @media (max-width: 960px) {
      grid-template-columns: repeat(2, 1fr);
    }

    @media (max-width: 560px) {
      grid-template-columns: 1fr;
    }

    .faq-item {
      h4 {
        font-size: 14.5px;
        font-weight: 700;
        margin: 0 0 8px;
        color: #000000;
      }

      p {
        font-size: 13.5px;
        line-height: 1.55;
        color: #555555;
        margin: 0;
      }
    }
  }
}
</style>
