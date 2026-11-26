<template>
  <div class="store-cart">
    <div class="layout-container">
      <div class="cart-header">
        <h1 class="cart-title">GIỎ HÀNG CỦA BẠN</h1>
        <p class="cart-subtitle" v-if="cartStore.items.length">
          Đang có <strong>{{ cartStore.totalItems }}</strong> sản phẩm được lưu trong giỏ
        </p>
      </div>

      <!-- Empty Cart State -->
      <div v-if="cartStore.items.length === 0" class="empty-cart-state">
        <div class="empty-icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-bag-svg">
            <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <path d="M16 10a4 4 0 0 1-8 0"></path>
          </svg>
        </div>
        <h2 class="empty-heading">Giỏ hàng của bạn đang trống</h2>
        <p class="empty-text">Hãy khám phá bộ sưu tập những mẫu sneaker hot nhất và thêm đôi giày yêu thích vào giỏ hàng.</p>
        <router-link to="/shop" class="btn-continue-shopping">
          <span>KHÁM PHÁ BỘ SƯU TẬP</span>
          <span>→</span>
        </router-link>
      </div>

      <!-- Cart with Items -->
      <div v-else class="cart-grid">
        <!-- Left: Items list & Shipping details -->
        <div class="cart-main-col">
          <!-- Items Table -->
          <div class="section-box items-section">
            <div class="box-header">
              <h2 class="box-title">DANH SÁCH SẢN PHẨM</h2>
              <button class="btn-clear-bag" @click="confirmClearCart">Xóa toàn bộ</button>
            </div>

            <div class="cart-items-list">
              <div
                v-for="item in cartStore.items"
                :key="item.variantId"
                class="cart-item-row"
              >
                <div class="item-visual" @click="router.push(`/product/${item.productId}`)">
                  <img
                    v-if="item.thumbnailUrl || item.imageUrl || (item.imageUrls && item.imageUrls[0])"
                    :src="item.thumbnailUrl || item.imageUrl || (item.imageUrls && item.imageUrls[0])"
                    :alt="item.productName"
                  />
                  <div v-else class="item-visual-placeholder">
                    <span>SNK</span>
                  </div>
                </div>

                <div class="item-content">
                  <div class="item-top">
                    <span class="item-brand">{{ item.brandName }}</span>
                    <button class="item-remove-btn" @click="cartStore.removeFromCart(item.variantId)" title="Xóa">✕</button>
                  </div>
                  <h3 class="item-name" @click="router.push(`/product/${item.productId}`)">{{ item.productName }}</h3>

                  <div class="item-variants-pills">
                    <span v-if="item.sizeValue" class="variant-pill">Size: {{ item.sizeValue }}</span>
                    <span v-if="item.colorName" class="variant-pill">{{ item.colorName }}</span>
                    <span v-if="item.sku" class="sku-pill">{{ item.sku }}</span>
                  </div>

                  <div class="item-bottom-bar">
                    <div class="item-stepper">
                      <button class="step-btn" @click="cartStore.updateQuantity(item.variantId, item.quantity - 1)">−</button>
                      <span class="step-val">{{ item.quantity }}</span>
                      <button class="step-btn" @click="cartStore.updateQuantity(item.variantId, item.quantity + 1)">+</button>
                    </div>

                    <div class="item-price-block">
                      <span class="unit-price" v-if="item.quantity > 1">{{ formatCurrency(item.salePrice) }} / đôi</span>
                      <span class="total-item-price">{{ formatCurrency(item.salePrice * item.quantity) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Customer & Delivery Form -->
          <div class="section-box shipping-section">
            <div class="box-header">
              <h2 class="box-title">THÔNG TIN NHẬN HÀNG</h2>
            </div>

            <!-- Customer Member Status Banner -->
            <div v-if="!authStore.isAuthenticated" class="auth-suggest-banner">
              <div class="banner-text">
                <strong>Bạn đã có tài khoản thành viên?</strong>
                <span>Đăng nhập hoặc tiếp tục bằng Google để tự động điền thông tin và theo dõi đơn hàng.</span>
              </div>
              <button class="banner-login-btn" type="button" @click="authModalVisible = true">
                ĐĂNG NHẬP / GOOGLE &rarr;
              </button>
            </div>
            <div v-else class="auth-suggest-banner logged-in">
              <div class="banner-text">
                <strong>Thành viên: {{ authStore.user?.fullName || authStore.user?.username }}</strong>
                <span>Thông tin giao hàng được cập nhật tự động từ tài khoản của bạn.</span>
              </div>
              <div class="banner-actions">
                <button
                  type="button"
                  class="btn-sync-profile"
                  @click="fillCustomerFromAuth(true); ElMessage.success('Đã điền nhanh thông tin nhận hàng từ tài khoản!')"
                  title="Điền lại thông tin mới nhất từ tài khoản"
                >
                  Điền nhanh từ tài khoản
                </button>
                <span class="member-tag">✓ ĐÃ ĐĂNG NHẬP</span>
              </div>
            </div>

            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-position="top"
              class="checkout-form"
            >
              <div class="form-row-2">
                <el-form-item label="Họ và tên người nhận (*)" prop="customerName">
                  <el-input v-model="form.customerName" placeholder="VD: Nguyễn Văn A" />
                </el-form-item>
                <el-form-item label="Số điện thoại (*)" prop="customerPhone">
                  <el-input v-model="form.customerPhone" placeholder="VD: 0912345678" />
                </el-form-item>
              </div>

              <!-- TỈNH / THÀNH PHỐ, QUẬN / HUYỆN, PHƯỜNG / XÃ (VIETNAM API) -->
              <div class="form-row-3 address-cascade-row">
                <el-form-item label="Tỉnh / Thành phố (*)" prop="provinceCode">
                  <QuickSelect
                    v-model="form.provinceCode"
                    :options="provinces"
                    value-key="code"
                    label-key="name"
                    placeholder="Chọn Tỉnh / TP"
                    search-placeholder="Tìm nhanh Tỉnh / TP..."
                    clearable
                    :loading="loadingProvinces"
                    @change="handleProvinceChange"
                  />
                </el-form-item>

                <el-form-item label="Quận / Huyện (*)" prop="districtCode">
                  <QuickSelect
                    v-model="form.districtCode"
                    :options="districts"
                    value-key="code"
                    label-key="name"
                    placeholder="Chọn Quận / Huyện"
                    search-placeholder="Tìm nhanh Quận / Huyện..."
                    clearable
                    :disabled="!form.provinceCode"
                    :loading="loadingDistricts"
                    @change="handleDistrictChange"
                  />
                </el-form-item>

                <el-form-item label="Phường / Xã (*)" prop="wardCode">
                  <QuickSelect
                    v-model="form.wardCode"
                    :options="wards"
                    value-key="code"
                    label-key="name"
                    placeholder="Chọn Phường / Xã"
                    search-placeholder="Tìm nhanh Phường / Xã..."
                    clearable
                    :disabled="!form.districtCode"
                    :loading="loadingWards"
                    @change="handleWardChange"
                  />
                </el-form-item>
              </div>

              <!-- SỐ NHÀ, TÊN ĐƯỜNG CỤ THỂ -->
              <el-form-item label="Số nhà, tên đường, tòa nhà (*)" prop="streetAddress">
                <el-input
                  v-model="form.streetAddress"
                  placeholder="VD: Số 45 Ngõ 12 Đường Cầu Giấy, Tòa Landmark..."
                  @input="updateFullShippingAddress"
                />
              </el-form-item>

              <!-- LIVE FORMATTED ADDRESS PREVIEW -->
              <div v-if="formattedShippingAddress" class="address-preview-banner">
                <span class="preview-icon">📍</span>
                <div class="preview-content">
                  <span class="preview-label">Địa chỉ nhận hàng hoàn chỉnh:</span>
                  <strong class="preview-address">{{ formattedShippingAddress }}</strong>
                </div>
              </div>

              <div class="form-row-2">
                <el-form-item label="Email (để nhận hóa đơn & vận đơn)" prop="customerEmail">
                  <el-input v-model="form.customerEmail" placeholder="VD: example@email.com" />
                </el-form-item>
                <el-form-item label="Ghi chú giao hàng (tùy chọn)" prop="note">
                  <el-input v-model="form.note" placeholder="VD: Giao trong giờ hành chính, gọi trước khi giao..." />
                </el-form-item>
              </div>

              <!-- Payment Method Selection -->
              <div class="payment-selection">
                <label class="payment-title">HÌNH THỨC THANH TOÁN</label>
                <div class="payment-cards">
                  <div
                    class="payment-option-card"
                    :class="{ active: form.paymentMethod === 'BANK_TRANSFER' }"
                    @click="form.paymentMethod = 'BANK_TRANSFER'"
                  >
                    <div class="card-radio"></div>
                    <div class="card-text">
                      <div class="card-headline">
                        <strong>Chuyển khoản SePay QR (MBBank)</strong>
                        <span class="recommend-tag">KHUYÊN DÙNG</span>
                      </div>
                      <p>Quét mã QR tự động qua ứng dụng ngân hàng hoặc MoMo. Tự động gạch nợ đơn hàng lập tức.</p>
                    </div>
                  </div>

                  <div
                    class="payment-option-card"
                    :class="{ active: form.paymentMethod === 'CASH' }"
                    @click="form.paymentMethod = 'CASH'"
                  >
                    <div class="card-radio"></div>
                    <div class="card-text">
                      <div class="card-headline">
                        <strong>Thanh toán khi nhận hàng (COD)</strong>
                      </div>
                      <p>Thanh toán tiền mặt cho bưu tá khi nhận giày và kiểm tra sản phẩm tận tay.</p>
                    </div>
                  </div>
                </div>
              </div>
            </el-form>
          </div>
        </div>

        <!-- Right: Sticky Order Summary -->
        <aside class="cart-summary-col">
          <div class="summary-card">
            <h2 class="summary-heading">TÓM TẮT ĐƠN HÀNG</h2>

            <!-- VOUCHER SECTION AT CHECKOUT -->
            <div class="checkout-voucher-box">
              <div class="voucher-box-header">
                <span class="voucher-box-title">MÃ GIẢM GIÁ / VOUCHER</span>
                <span v-if="availableVouchers.length" class="voucher-count-pill">
                  {{ availableVouchers.filter(v => v.eligible).length }}/{{ availableVouchers.length }} áp dụng được
                </span>
              </div>

              <div class="voucher-input-group">
                <input
                  v-model="voucherInput"
                  type="text"
                  class="voucher-code-input"
                  placeholder="Nhập mã voucher (VD: SNEAKER10)..."
                  @keyup.enter="applyVoucherCode()"
                  @input="voucherInput = voucherInput.toUpperCase()"
                />
                <button
                  type="button"
                  class="btn-apply-voucher"
                  :disabled="validatingVoucher || !voucherInput.trim()"
                  @click="applyVoucherCode()"
                >
                  {{ validatingVoucher ? '...' : 'ÁP DỤNG' }}
                </button>
              </div>

              <!-- Applied Success Banner -->
              <div v-if="appliedVoucher" class="voucher-applied-banner">
                <div class="applied-info">
                  <strong>✓ {{ appliedVoucher.appliedCode }}</strong>
                  <span>Đã giảm {{ formatCurrency(appliedVoucher.discountAmount) }}</span>
                </div>
                <button type="button" class="btn-remove-voucher" @click="removeVoucher">Bỏ chọn</button>
              </div>

              <!-- Validation Error Message -->
              <div v-else-if="voucherError" class="voucher-error-msg">
                {{ voucherError }}
              </div>

              <!-- Available Vouchers Wallet + Public Codes -->
              <div v-if="availableVouchers.length > 0" class="voucher-wallet-section">
                <div class="wallet-label">
                  {{ authStore.isAuthenticated ? 'Kho voucher của tôi & ưu đãi hiện có:' : 'Ưu đãi có thể áp dụng ngay:' }}
                </div>
                <div class="voucher-cards-list">
                  <div
                    v-for="v in availableVouchers"
                    :key="v.code"
                    class="voucher-ticket"
                    :class="{
                      selected: appliedVoucher?.appliedCode === v.code,
                      ineligible: !v.eligible
                    }"
                  >
                    <div class="ticket-main">
                      <div class="ticket-top-row">
                        <span class="ticket-code">{{ v.code }}</span>
                        <span class="ticket-source" :class="v.sourceType === 'PERSONAL' ? 'personal' : 'public'">
                          {{ v.sourceType === 'PERSONAL' ? 'CÁ NHÂN' : 'CÔNG KHAI' }}
                        </span>
                      </div>
                      <div class="ticket-discount">
                        {{ v.discountType === 'PERCENT'
                          ? `Giảm ${v.discountValue}%${v.maxDiscountAmount ? ` (tối đa ${formatCurrency(v.maxDiscountAmount)})` : ''}`
                          : `Giảm trực tiếp ${formatCurrency(v.discountValue)}` }}
                      </div>
                      <div class="ticket-meta">
                        <span>Đơn tối thiểu {{ formatCurrency(v.minOrderAmount || 0) }}</span>
                        <span v-if="v.remainingDays !== null && v.remainingDays <= 7" class="exp-warn">
                          · Còn {{ v.remainingDays }} ngày
                        </span>
                      </div>
                      <div v-if="!v.eligible && v.ineligibleReason" class="ticket-reason">
                        {{ v.ineligibleReason }}
                      </div>
                    </div>
                    <div class="ticket-action">
                      <button
                        v-if="appliedVoucher?.appliedCode === v.code"
                        type="button"
                        class="btn-ticket-use active"
                        @click="removeVoucher"
                      >
                        Đang dùng ✓
                      </button>
                      <button
                        v-else
                        type="button"
                        class="btn-ticket-use"
                        :disabled="!v.eligible"
                        @click="applyVoucherCode(v.code)"
                      >
                        {{ v.eligible ? 'Áp dụng' : 'Chưa đủ' }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="summary-calc-rows">
              <div class="calc-row">
                <span class="calc-label">Tạm tính ({{ cartStore.totalItems }} đôi):</span>
                <span class="calc-val">{{ formatCurrency(cartStore.totalPrice) }}</span>
              </div>

              <div v-if="voucherDiscount > 0" class="calc-row discount-row">
                <span class="calc-label">Giảm giá voucher ({{ appliedVoucher?.appliedCode }}):</span>
                <span class="calc-val discount-val">-{{ formatCurrency(voucherDiscount) }}</span>
              </div>

              <div class="calc-row">
                <span class="calc-label">Phí vận chuyển:</span>
                <span class="calc-val" :class="{ 'free': cartStore.totalPrice >= 1000000 }">
                  {{ cartStore.totalPrice >= 1000000 ? 'MIỄN PHÍ' : '30.000₫' }}
                </span>
              </div>

              <div class="freeship-notice" v-if="cartStore.totalPrice < 1000000">
                <span>Mua thêm <strong>{{ formatCurrency(1000000 - cartStore.totalPrice) }}</strong> để được <strong>FREESHIP</strong> toàn quốc!</span>
              </div>

              <div class="calc-row total-row">
                <span class="total-lbl">TỔNG THANH TOÁN:</span>
                <span class="total-price">{{ formatCurrency(finalTotal) }}</span>
              </div>
            </div>

            <button
              class="btn-place-order"
              :disabled="submitting"
              @click="submitOrder"
            >
              <span v-if="submitting">ĐANG XỬ LÝ ĐƠN HÀNG...</span>
              <span v-else>XÁC NHẬN ĐẶT HÀNG →</span>
            </button>

            <div class="guarantee-points">
              <div class="point-item">
                <span class="point-dot">•</span>
                <span>Kiểm tra hàng trước khi thanh toán</span>
              </div>
              <div class="point-item">
                <span class="point-dot">•</span>
                <span>Hỗ trợ đổi size trong 7 ngày</span>
              </div>
              <div class="point-item">
                <span class="point-dot">•</span>
                <span>Bảo mật 100% thông tin giao dịch</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>

    <!-- Client Auth Modal -->
    <ClientAuthModal v-model="authModalVisible" @authenticated="fillCustomerFromAuth" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { useClientAuthStore } from '@/stores/clientAuth'
import storeService from '@/services/storeService'
import vnAddressService from '@/services/vnAddressService'
import { formatCurrency } from '@/utils/formatters'
import ClientAuthModal from '@/components/store/ClientAuthModal.vue'

const router = useRouter()
const cartStore = useCartStore()
const authStore = useClientAuthStore()
const authModalVisible = ref(false)

const formRef = ref(null)
const submitting = ref(false)

// Administrative division states (API)
const provinces = ref([])
const districts = ref([])
const wards = ref([])
const loadingProvinces = ref(false)
const loadingDistricts = ref(false)
const loadingWards = ref(false)

const form = reactive({
  customerName: '',
  customerPhone: '',
  customerEmail: '',
  provinceCode: null,
  districtCode: null,
  wardCode: null,
  provinceName: '',
  districtName: '',
  wardName: '',
  streetAddress: '',
  shippingAddress: '',
  paymentMethod: 'BANK_TRANSFER',
  note: ''
})

// Formatted full address preview
const formattedShippingAddress = computed(() => {
  const parts = []
  if (form.streetAddress && form.streetAddress.trim()) {
    parts.push(form.streetAddress.trim())
  }
  if (form.wardName) parts.push(form.wardName)
  if (form.districtName) parts.push(form.districtName)
  if (form.provinceName) parts.push(form.provinceName)
  return parts.join(', ')
})

const updateFullShippingAddress = () => {
  form.shippingAddress = formattedShippingAddress.value
}

// Fetch Vietnam provinces on mount
const loadProvinces = async () => {
  loadingProvinces.value = true
  try {
    provinces.value = await vnAddressService.getProvinces()
  } catch (err) {
    console.error('Lỗi khi tải danh sách Tỉnh/Thành phố:', err)
  } finally {
    loadingProvinces.value = false
  }
}

// Cascade handlers
const handleProvinceChange = async (val) => {
  form.districtCode = null
  form.wardCode = null
  form.districtName = ''
  form.wardName = ''
  districts.value = []
  wards.value = []

  const selected = provinces.value.find(p => p.code === val)
  form.provinceName = selected ? selected.name : ''
  updateFullShippingAddress()

  if (val) {
    loadingDistricts.value = true
    try {
      districts.value = await vnAddressService.getDistricts(val)
    } catch (err) {
      console.error('Lỗi tải danh sách Quận/Huyện:', err)
    } finally {
      loadingDistricts.value = false
    }
  }
}

const handleDistrictChange = async (val) => {
  form.wardCode = null
  form.wardName = ''
  wards.value = []

  const selected = districts.value.find(d => d.code === val)
  form.districtName = selected ? selected.name : ''
  updateFullShippingAddress()

  if (val) {
    loadingWards.value = true
    try {
      wards.value = await vnAddressService.getWards(val)
    } catch (err) {
      console.error('Lỗi tải danh sách Phường/Xã:', err)
    } finally {
      loadingWards.value = false
    }
  }
}

const handleWardChange = (val) => {
  const selected = wards.value.find(w => w.code === val)
  form.wardName = selected ? selected.name : ''
  updateFullShippingAddress()
}

const parseAndFillAddress = async (rawAddress) => {
  if (!rawAddress) return
  form.streetAddress = rawAddress
  form.shippingAddress = rawAddress

  // Ensure provinces are loaded
  if (!provinces.value || provinces.value.length === 0) {
    await loadProvinces()
  }

  // Check if rawAddress has comma-separated structure (street, ward, district, province)
  const segments = rawAddress.split(',').map(s => s.trim()).filter(Boolean)
  if (segments.length >= 2 && provinces.value.length) {
    // 1. Try finding Province (from the last segment)
    const lastSeg = segments[segments.length - 1].toLowerCase()
    const matchedProv = provinces.value.find(p => {
      const pName = p.name.toLowerCase()
      const cleanPName = pName.replace('thành phố ', '').replace('tỉnh ', '')
      return lastSeg.includes(cleanPName) || cleanPName.includes(lastSeg)
    })

    if (matchedProv) {
      form.provinceCode = matchedProv.code
      form.provinceName = matchedProv.name

      // Load districts for this province
      loadingDistricts.value = true
      try {
        districts.value = await vnAddressService.getDistricts(matchedProv.code)
      } catch (e) {
        console.error(e)
      } finally {
        loadingDistricts.value = false
      }

      // 2. Try finding District (from segment second to last)
      if (segments.length >= 3 && districts.value.length) {
        const distSeg = segments[segments.length - 2].toLowerCase()
        const matchedDist = districts.value.find(d => {
          const dName = d.name.toLowerCase()
          const cleanDName = dName.replace('quận ', '').replace('huyện ', '').replace('thị xã ', '').replace('thành phố ', '')
          return distSeg.includes(cleanDName) || cleanDName.includes(distSeg)
        })

        if (matchedDist) {
          form.districtCode = matchedDist.code
          form.districtName = matchedDist.name

          // Load wards for this district
          loadingWards.value = true
          try {
            wards.value = await vnAddressService.getWards(matchedDist.code)
          } catch (e) {
            console.error(e)
          } finally {
            loadingWards.value = false
          }

          // 3. Try finding Ward (from segment third to last)
          if (segments.length >= 4 && wards.value.length) {
            const wardSeg = segments[segments.length - 3].toLowerCase()
            const matchedWard = wards.value.find(w => {
              const wName = w.name.toLowerCase()
              const cleanWName = wName.replace('phường ', '').replace('xã ', '').replace('thị trấn ', '')
              return wardSeg.includes(cleanWName) || cleanWName.includes(wardSeg)
            })

            if (matchedWard) {
              form.wardCode = matchedWard.code
              form.wardName = matchedWard.name
            }
          }

          // 4. Street address is everything before ward/district/province
          const streetParts = segments.slice(0, segments.length - (form.wardCode ? 3 : 2))
          if (streetParts.length) {
            form.streetAddress = streetParts.join(', ')
          }
        }
      }
    }
  }
  updateFullShippingAddress()
}

const fillCustomerFromAuth = (force = false) => {
  if (authStore.isAuthenticated && authStore.user) {
    if (authStore.user.fullName && (force || !form.customerName)) {
      form.customerName = authStore.user.fullName
    }
    if (authStore.user.phone && (force || !form.customerPhone)) {
      form.customerPhone = authStore.user.phone
    }
    if (authStore.user.email && (force || !form.customerEmail)) {
      form.customerEmail = authStore.user.email
    }
    if (authStore.user.address && (force || (!form.streetAddress && !form.provinceCode))) {
      parseAndFillAddress(authStore.user.address)
    }
  }
}

const enrichCartThumbnails = async () => {
  let changed = false
  for (const item of cartStore.items) {
    if ((!item.thumbnailUrl && !item.imageUrl) && item.productId) {
      try {
        const res = await storeService.getProductDetail(item.productId)
        const p = res?.data
        const img = p?.thumbnailUrl || (p?.imageUrls && p.imageUrls[0]) || ''
        if (img) {
          item.thumbnailUrl = img
          item.imageUrl = img
          changed = true
        }
      } catch (err) {
        console.error('Không thể đồng bộ ảnh sản phẩm:', item.productId, err)
      }
    }
  }
  if (changed) {
    localStorage.setItem('sneakerhub_cart', JSON.stringify(cartStore.items))
  }
}

onMounted(async () => {
  await loadProvinces()
  fillCustomerFromAuth(true)
  enrichCartThumbnails()
  loadAvailableVouchers()
})

watch(() => authStore.isAuthenticated, (isAuth) => {
  if (isAuth) {
    fillCustomerFromAuth(true)
  }
  loadAvailableVouchers()
})

watch(
  () => authStore.user,
  (newUser) => {
    if (newUser) {
      fillCustomerFromAuth(true)
      loadAvailableVouchers()
    }
  },
  { deep: true }
)

// ==================== CHECKOUT VOUCHER LOGIC ====================
const voucherInput = ref('')
const availableVouchers = ref([])
const appliedVoucher = ref(null)
const voucherError = ref('')
const validatingVoucher = ref(false)
let voucherFetchTimer = null

const voucherDiscount = computed(() => Number(appliedVoucher.value?.discountAmount || 0))

const loadAvailableVouchers = async () => {
  try {
    const res = await storeService.getAvailableVouchers({
      phone: form.customerPhone?.trim() || authStore.user?.phone || undefined,
      email: form.customerEmail?.trim() || authStore.user?.email || undefined,
      orderAmount: cartStore.totalPrice || 0
    })
    availableVouchers.value = res?.data || []
  } catch (err) {
    console.error('Lỗi tải danh sách voucher:', err)
  }
}

const applyVoucherCode = async (codeOverride) => {
  const code = (codeOverride || voucherInput.value || '').trim().toUpperCase()
  if (!code) return
  voucherInput.value = code
  validatingVoucher.value = true
  voucherError.value = ''
  try {
    const res = await storeService.validateVoucher({
      voucherCode: code,
      channel: 'ONLINE',
      orderAmount: cartStore.totalPrice,
      customerPhone: form.customerPhone?.trim() || authStore.user?.phone || null,
      customerEmail: form.customerEmail?.trim() || authStore.user?.email || null
    })
    const data = res?.data
    if (data?.valid) {
      appliedVoucher.value = data
      voucherError.value = ''
      ElMessage.success(data.message || `Đã áp dụng voucher ${data.appliedCode}`)
    } else {
      appliedVoucher.value = null
      voucherError.value = data?.message || 'Mã voucher không hợp lệ'
    }
  } catch (err) {
    appliedVoucher.value = null
    voucherError.value = err?.response?.data?.message || 'Không thể kiểm tra mã voucher'
  } finally {
    validatingVoucher.value = false
  }
}

const removeVoucher = () => {
  appliedVoucher.value = null
  voucherInput.value = ''
  voucherError.value = ''
}

watch(
  () => [cartStore.totalPrice, form.customerPhone, form.customerEmail],
  () => {
    clearTimeout(voucherFetchTimer)
    voucherFetchTimer = setTimeout(async () => {
      await loadAvailableVouchers()
      if (appliedVoucher.value && cartStore.totalPrice > 0) {
        try {
          const res = await storeService.validateVoucher({
            voucherCode: appliedVoucher.value.appliedCode,
            channel: 'ONLINE',
            orderAmount: cartStore.totalPrice,
            customerPhone: form.customerPhone?.trim() || authStore.user?.phone || null,
            customerEmail: form.customerEmail?.trim() || authStore.user?.email || null
          })
          if (res?.data?.valid) {
            appliedVoucher.value = res.data
            voucherError.value = ''
          } else {
            appliedVoucher.value = null
            voucherError.value = res?.data?.message || 'Mã voucher không còn đủ điều kiện cho giỏ hàng hiện tại'
          }
        } catch {
          appliedVoucher.value = null
        }
      }
    }, 350)
  }
)

const rules = {
  customerName: [{ required: true, message: 'Vui lòng nhập họ tên người nhận', trigger: 'blur' }],
  customerPhone: [
    { required: true, message: 'Vui lòng nhập số điện thoại', trigger: 'blur' },
    { pattern: /^[0-9]{9,11}$/, message: 'Số điện thoại không đúng định dạng', trigger: 'blur' }
  ],
  provinceCode: [{ required: true, message: 'Vui lòng chọn Tỉnh / Thành phố', trigger: 'change' }],
  districtCode: [{ required: true, message: 'Vui lòng chọn Quận / Huyện', trigger: 'change' }],
  wardCode: [{ required: true, message: 'Vui lòng chọn Phường / Xã', trigger: 'change' }],
  streetAddress: [{ required: true, message: 'Vui lòng nhập số nhà, tên đường cụ thể', trigger: 'blur' }]
}

const finalTotal = computed(() => {
  const shipFee = cartStore.totalPrice >= 1000000 ? 0 : 30000
  return Math.max(0, cartStore.totalPrice - voucherDiscount.value) + shipFee
})

const confirmClearCart = () => {
  ElMessageBox.confirm('Bạn có chắc chắn muốn xóa toàn bộ sản phẩm trong giỏ hàng?', 'Xác nhận', {
    confirmButtonText: 'Xóa giỏ',
    cancelButtonText: 'Hủy',
    type: 'warning'
  }).then(() => {
    cartStore.clearCart()
  })
}

const submitOrder = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('Vui lòng điền đầy đủ các thông tin nhận hàng bắt buộc (*)')
      return
    }

    if (cartStore.items.length === 0) {
      ElMessage.warning('Giỏ hàng của bạn đang trống!')
      return
    }

    updateFullShippingAddress()
    const finalAddress = formattedShippingAddress.value || form.shippingAddress

    submitting.value = true
    try {
      const payload = {
        customerName: form.customerName,
        customerPhone: form.customerPhone,
        customerEmail: form.customerEmail || null,
        shippingAddress: finalAddress,
        provinceName: form.provinceName || null,
        districtName: form.districtName || null,
        wardName: form.wardName || null,
        streetAddress: form.streetAddress || null,
        paymentMethod: form.paymentMethod,
        voucherCode: appliedVoucher.value?.appliedCode || null,
        note: form.note || null,
        items: cartStore.items.map(item => ({
          variantId: item.variantId,
          quantity: item.quantity
        }))
      }

      const res = await storeService.checkout(payload)
      const orderData = res?.data

      // Save order details to sessionStorage for success page
      sessionStorage.setItem('last_placed_order', JSON.stringify(orderData))

      // Clear cart
      cartStore.clearCart()

      ElMessage.success('Đặt hàng thành công!')
      router.push(`/order-success/${orderData.orderCode}`)
    } catch (err) {
      console.error('Lỗi đặt hàng:', err)
      const msg = (err.response && err.response.data && err.response.data.message) || 'Có lỗi xảy ra khi tạo đơn hàng'
      ElMessage.error(msg)
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.store-cart {
  padding: 40px 0 90px;
  background: #ffffff;
}

.layout-container {
  max-width: 1360px;
  margin: 0 auto;
  padding: 0 28px;
}

.cart-header {
  margin-bottom: 36px;

  .cart-title {
    font-size: 32px;
    font-weight: 900;
    letter-spacing: -0.02em;
    color: #000000;
    margin: 0 0 6px;
  }

  .cart-subtitle {
    font-size: 14px;
    color: #737373;
    margin: 0;
  }
}

/* Empty State */
.empty-cart-state {
  text-align: center;
  padding: 100px 20px;
  background: #fafafa;
  border: 1px dashed #e5e5e5;

  .empty-icon-wrap {
    margin-bottom: 20px;
    .empty-bag-svg {
      width: 56px;
      height: 56px;
      color: #a3a3a3;
    }
  }

  .empty-heading {
    font-size: 22px;
    font-weight: 900;
    color: #000000;
    margin: 0 0 8px;
  }

  .empty-text {
    font-size: 14px;
    color: #737373;
    max-width: 440px;
    margin: 0 auto 28px;
    line-height: 1.5;
  }

  .btn-continue-shopping {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    background: #000000;
    color: #ffffff;
    padding: 14px 28px;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-decoration: none;
    transition: background 0.15s ease;

    &:hover {
      background: #262626;
    }
  }
}

/* Cart Grid */
.cart-grid {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 48px;
  align-items: flex-start;

  @media (max-width: 992px) {
    grid-template-columns: 1fr;
    gap: 36px;
  }
}

.section-box {
  background: #ffffff;
  border: 1px solid #ebebeb;
  padding: 28px;
  margin-bottom: 28px;

  @media (max-width: 600px) {
    padding: 18px 14px;
    margin-bottom: 20px;
  }

  .box-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 18px;
    border-bottom: 1px solid #ebebeb;
    margin-bottom: 20px;

    .box-title {
      font-size: 14px;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: #000000;
      margin: 0;
    }

    .btn-clear-bag {
      background: none;
      border: none;
      font-family: inherit;
      font-size: 11px;
      font-weight: 700;
      color: #737373;
      text-decoration: underline;
      cursor: pointer;
      &:hover {
        color: #ef4444;
      }
    }
  }
}

.auth-suggest-banner {
  background: #f4f4f5;
  border: 1px solid #e4e4e7;
  padding: 14px 18px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  &.logged-in {
    background: #f0fdf4;
    border-color: #bbf7d0;
  }

  .banner-text {
    display: flex;
    flex-direction: column;
    gap: 3px;

    strong {
      font-size: 13px;
      font-weight: 800;
      color: #000000;
    }

    span {
      font-size: 12px;
      color: #52525b;
    }
  }

  .banner-login-btn {
    height: 34px;
    padding: 0 14px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    border-radius: 5px;
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    cursor: pointer;
    white-space: nowrap;
    transition: all 0.15s ease;

    &:hover {
      background: #27272a;
    }
  }

  .banner-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    white-space: nowrap;

    .btn-sync-profile {
      height: 32px;
      padding: 0 12px;
      background: #ffffff;
      color: #000000;
      border: 1px solid #000000;
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.04em;
      cursor: pointer;
      transition: all 0.15s ease;

      &:hover {
        background: #000000;
        color: #ffffff;
      }
    }
  }

  .member-tag {
    font-size: 11px;
    font-weight: 800;
    color: #16a34a;
    letter-spacing: 0.05em;
    white-space: nowrap;
  }
}

/* Items List */
.cart-items-list {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .cart-item-row {
    display: flex;
    gap: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
      padding-bottom: 0;
    }

    .item-visual {
      width: 100px;
      height: 100px;
      background: #f5f5f7;
      border: 1px solid #ebebeb;
      padding: 6px;
      flex-shrink: 0;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;

      img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }

      .item-visual-placeholder {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f1f5f9;
        color: #94a3b8;
        font-weight: 800;
        font-size: 13px;
      }
    }

    .item-content {
      flex: 1;

      .item-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 4px;

        .item-brand {
          font-size: 10px;
          font-weight: 800;
          letter-spacing: 0.08em;
          color: #737373;
          text-transform: uppercase;
        }

        .item-remove-btn {
          background: none;
          border: none;
          color: #a3a3a3;
          font-size: 13px;
          cursor: pointer;
          &:hover {
            color: #000000;
          }
        }
      }

      .item-name {
        font-size: 15px;
        font-weight: 800;
        color: #000000;
        margin: 0 0 8px;
        line-height: 1.3;
        cursor: pointer;
        &:hover {
          color: #525252;
        }
      }

      .item-variants-pills {
        display: flex;
        gap: 6px;
        margin-bottom: 14px;

        .variant-pill {
          font-size: 11px;
          font-weight: 700;
          background: #f5f5f5;
          padding: 2px 8px;
          color: #262626;
        }

        .sku-pill {
          font-size: 10px;
          font-family: monospace;
          background: #fafafa;
          border: 1px solid #e5e5e5;
          padding: 2px 6px;
          color: #737373;
        }
      }

      .item-bottom-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .item-stepper {
          display: flex;
          align-items: center;
          border: 1px solid #e5e5e5;

          .step-btn {
            width: 30px;
            height: 30px;
            background: none;
            border: none;
            font-size: 15px;
            cursor: pointer;
            &:hover {
              background: #f5f5f5;
            }
          }

          .step-val {
            width: 30px;
            text-align: center;
            font-size: 12px;
            font-weight: 800;
            font-variant-numeric: tabular-nums;
          }
        }

        .item-price-block {
          text-align: right;

          .unit-price {
            font-size: 11px;
            color: #737373;
            display: block;
          }

          .total-item-price {
            font-size: 15px;
            font-weight: 900;
            color: #000000;
            font-variant-numeric: tabular-nums;
          }
        }
      }
    }
  }
}

/* Shipping Form */
.checkout-form {
  .form-row-2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;

    @media (max-width: 600px) {
      grid-template-columns: 1fr;
    }
  }

  .form-row-3 {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 16px;

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
      gap: 0;
    }
  }

  .address-preview-banner {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 16px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-left: 4px solid #000000;
    margin-bottom: 22px;

    .preview-icon {
      font-size: 18px;
      line-height: 1;
    }

    .preview-content {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .preview-label {
        font-size: 11px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: #64748b;
      }

      .preview-address {
        font-size: 13px;
        font-weight: 700;
        color: #000000;
        line-height: 1.4;
      }
    }
  }

  .payment-selection {
    margin-top: 24px;
    padding-top: 20px;
    border-top: 1px solid #ebebeb;

    .payment-title {
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.1em;
      color: #000000;
      text-transform: uppercase;
      display: block;
      margin-bottom: 12px;
    }

    .payment-cards {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .payment-option-card {
        display: flex;
        gap: 14px;
        padding: 16px 18px;
        border: 1px solid #e5e5e5;
        background: #fafafa;
        cursor: pointer;
        transition: all 0.15s ease;

        .card-radio {
          width: 18px;
          height: 18px;
          border-radius: 50%;
          border: 2px solid #a3a3a3;
          margin-top: 2px;
          flex-shrink: 0;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .card-headline {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 4px;

          strong {
            font-size: 14px;
            font-weight: 800;
            color: #000000;
          }

          .recommend-tag {
            font-size: 9px;
            font-weight: 800;
            letter-spacing: 0.08em;
            background: #000000;
            color: #ffffff;
            padding: 2px 6px;
          }
        }

        p {
          font-size: 12px;
          color: #737373;
          margin: 0;
          line-height: 1.4;
        }

        &:hover {
          border-color: #737373;
        }

        &.active {
          border-color: #000000;
          background: #ffffff;

          .card-radio {
            border-color: #000000;
            background: #000000;
            box-shadow: inset 0 0 0 3px #ffffff;
          }
        }
      }
    }
  }
}

/* Order Summary Box */
.summary-card {
  position: sticky;
  top: 96px;
  background: #fafafa;
  border: 1px solid #ebebeb;
  padding: 30px 28px;

  @media (max-width: 600px) {
    padding: 20px 16px;
    position: static;
  }

  .summary-heading {
    font-size: 14px;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: #000000;
    margin: 0 0 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e5e5e5;
  }

  .summary-calc-rows {
    display: flex;
    flex-direction: column;
    gap: 14px;
    margin-bottom: 28px;

    .calc-row {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      color: #525252;

      .calc-val {
        font-weight: 700;
        color: #000000;
        font-variant-numeric: tabular-nums;

        &.free {
          color: #10b981;
          font-weight: 800;
        }
      }

      &.total-row {
        margin-top: 10px;
        padding-top: 16px;
        border-top: 1px solid #e5e5e5;
        align-items: baseline;

        .total-lbl {
          font-size: 13px;
          font-weight: 900;
          letter-spacing: 0.06em;
          color: #000000;
        }

        .total-price {
          font-size: 24px;
          font-weight: 900;
          color: #000000;
          letter-spacing: -0.02em;
          font-variant-numeric: tabular-nums;
        }
      }
    }

    .freeship-notice {
      background: #ffffff;
      border: 1px solid #e5e5e5;
      padding: 10px 12px;
      font-size: 11px;
      color: #525252;
      line-height: 1.4;

      strong {
        color: #000000;
      }
    }
  }

  .btn-place-order {
    width: 100%;
    height: 52px;
    background: #000000;
    color: #ffffff;
    border: none;
    font-family: inherit;
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0.08em;
    cursor: pointer;
    transition: background 0.15s ease;

    &:hover:not(:disabled) {
      background: #262626;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  .guarantee-points {
    margin-top: 24px;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .point-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 11px;
      color: #737373;

      .point-dot {
        font-size: 14px;
        color: #000000;
      }
    }
  }
}

.checkout-voucher-box {
  margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
  border: 1px solid #e5e5e5;

  .voucher-box-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    .voucher-box-title {
      font-size: 11.5px;
      font-weight: 800;
      letter-spacing: 0.08em;
      color: #000000;
    }
    .voucher-count-pill {
      font-size: 11px;
      font-weight: 700;
      color: #059669;
    }
  }

  .voucher-input-group {
    display: flex;
    gap: 8px;

    .voucher-code-input {
      flex: 1;
      height: 38px;
      padding: 0 12px;
      border: 1px solid #d4d4d4;
      background: #ffffff;
      font-family: inherit;
      font-size: 12.5px;
      font-weight: 700;
      letter-spacing: 0.04em;
      color: #000000;
      outline: none;
      &:focus {
        border-color: #000000;
      }
    }

    .btn-apply-voucher {
      height: 38px;
      padding: 0 16px;
      background: #000000;
      color: #ffffff;
      border: none;
      font-family: inherit;
      font-size: 11.5px;
      font-weight: 800;
      letter-spacing: 0.06em;
      cursor: pointer;
      &:hover:not(:disabled) {
        background: #262626;
      }
      &:disabled {
        opacity: 0.45;
        cursor: not-allowed;
      }
    }
  }

  .voucher-applied-banner {
    margin-top: 10px;
    padding: 8px 12px;
    background: #ecfdf5;
    border: 1px solid #a7f3d0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    color: #065f46;

    .applied-info {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .btn-remove-voucher {
      background: none;
      border: none;
      color: #dc2626;
      font-size: 11.5px;
      font-weight: 700;
      text-decoration: underline;
      cursor: pointer;
    }
  }

  .voucher-error-msg {
    margin-top: 8px;
    font-size: 12px;
    font-weight: 600;
    color: #dc2626;
  }

  .voucher-wallet-section {
    margin-top: 12px;

    .wallet-label {
      font-size: 11.5px;
      font-weight: 700;
      color: #525252;
      margin-bottom: 8px;
    }

    .voucher-cards-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-height: 240px;
      overflow-y: auto;
    }

    .voucher-ticket {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      background: #ffffff;
      border: 1px solid #e5e5e5;
      transition: border-color 0.15s ease;

      &.selected {
        border-color: #000000;
        box-shadow: inset 3px 0 0 #000000;
        background: #f9fafb;
      }

      &.ineligible {
        opacity: 0.72;
        background: #fcfcfc;
      }

      .ticket-main {
        flex: 1;
        min-width: 0;
      }

      .ticket-top-row {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 3px;

        .ticket-code {
          font-family: monospace;
          font-size: 12.5px;
          font-weight: 900;
          color: #000000;
          letter-spacing: 0.04em;
        }

        .ticket-source {
          font-size: 9px;
          font-weight: 800;
          padding: 1px 5px;
          letter-spacing: 0.05em;

          &.personal {
            background: #000000;
            color: #ffffff;
          }
          &.public {
            background: #f1f5f9;
            color: #334155;
            border: 1px solid #cbd5e1;
          }
        }
      }

      .ticket-discount {
        font-size: 12.5px;
        font-weight: 800;
        color: #dc2626;
      }

      .ticket-meta {
        font-size: 11px;
        color: #737373;
        margin-top: 2px;
        .exp-warn {
          color: #d97706;
          font-weight: 700;
        }
      }

      .ticket-reason {
        font-size: 11px;
        color: #dc2626;
        margin-top: 3px;
        font-weight: 600;
      }

      .btn-ticket-use {
        padding: 6px 11px;
        font-size: 11px;
        font-weight: 800;
        border: 1px solid #000000;
        background: #ffffff;
        color: #000000;
        cursor: pointer;
        white-space: nowrap;
        transition: all 0.14s ease;

        &:hover:not(:disabled) {
          background: #000000;
          color: #ffffff;
        }

        &.active {
          background: #000000;
          color: #ffffff;
        }

        &:disabled {
          border-color: #d4d4d4;
          color: #a3a3a3;
          cursor: not-allowed;
        }
      }
    }
  }
}

.summary-calc-rows .calc-row.discount-row {
  .discount-val {
    color: #dc2626;
    font-weight: 800;
  }
}
</style>
