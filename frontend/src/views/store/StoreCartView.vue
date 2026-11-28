<template>
  <div class="store-cart">
    <div class="adi-cart-container">
      <!-- ============================================================== -->
      <!-- EMPTY CART STATE (Exact Live Adidas Clean Left-Aligned Layout) -->
      <!-- ============================================================== -->
      <div v-if="cartStore.items.length === 0" class="adi-empty-cart">
        <h1 class="cart-title">{{ isEn ? 'YOUR BAG IS EMPTY' : 'GIỎ HÀNG CỦA BẠN TRỐNG' }}</h1>
        <p class="cart-empty-sub">
          {{
            isEn
              ? 'Once you add something to your bag — it will appear here. Ready to get started?'
              : 'Khi bạn đã sẵn sàng mua sắm, các sản phẩm bạn thêm sẽ hiển thị tại đây. Bắt đầu ngay nhé?'
          }}
        </p>
        <router-link :to="lp('shop')" class="btn-adi-offset-inline">
          <span>{{ isEn ? 'GET STARTED' : 'BẮT ĐẦU' }}</span>
          <span class="arrow">→</span>
        </router-link>

        <!-- "GỢI Ý CHO BẠN" 4-Column Grid on Empty State -->
        <section v-if="recommendedProducts.length" class="adi-cart-recs">
          <h2 class="recs-heading">{{ isEn ? 'TOP PICKS FOR YOU' : 'GỢI Ý CHO BẠN' }}</h2>
          <div class="recs-grid-4">
            <article
              v-for="item in recommendedProducts"
              :key="item.id"
              class="rec-card"
              @click="router.push(lp(`product/${item.id}`))"
            >
              <div class="rec-img-stage">
                <img :src="item.thumbnailUrl || (item.imageUrls && item.imageUrls[0]) || FALLBACK_IMG" :alt="item.productName" loading="lazy" />
                <button
                  type="button"
                  class="rec-heart"
                  @click.stop="toggleWishlistItem(item)"
                  :aria-label="t('account.wishlist')"
                >
                  <svg viewBox="0 0 24 24" :fill="wishlistStore.isFavorite(item.id) ? '#000' : 'none'" stroke="#000" stroke-width="1.5">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                  </svg>
                </button>
              </div>
              <div class="rec-meta">
                <div class="rec-price">{{ formatCurrency(item.salePrice) }}</div>
                <div class="rec-name">{{ item.productName }}</div>
                <div class="rec-cat">{{ item.brandName || 'Originals' }}</div>
              </div>
            </article>
          </div>
        </section>
      </div>

      <!-- ============================================================== -->
      <!-- POPULATED CART + CHECKOUT (Exact Live 2026 Adidas 2-Col Split) -->
      <!-- ============================================================== -->
      <div v-else class="adi-cart-grid">
        <!-- LEFT COLUMN (~65%): Bag Header, Promo Strip, Sharp Item Cards, Shipping & Payment, Recs -->
        <div class="cart-left-col">
          <!-- Bag Title Row -->
          <header class="adi-bag-header">
            <div class="bag-title-row">
              <h1 class="cart-title">{{ isEn ? 'YOUR BAG' : 'GIỎ HÀNG CỦA BẠN' }}</h1>
              <span class="cart-count-muted">
                ({{ cartStore.totalItems }} {{ isEn ? (cartStore.totalItems > 1 ? 'items' : 'item') : 'sản phẩm' }})
              </span>
            </div>
            <div class="bag-sub-row">
              <p class="bag-reserve-note">
                {{
                  isEn
                    ? 'Items in your bag are not reserved — check out now to make them yours.'
                    : 'Các mặt hàng trong giỏ hàng của bạn không được bảo lưu — hãy kiểm tra ngay để đặt hàng.'
                }}
              </p>
              <button type="button" class="btn-clear-bag" @click="confirmClearCart">
                {{ isEn ? 'Clear bag' : 'Xóa toàn bộ' }}
              </button>
            </div>
          </header>

          <!-- Exact Live Adidas #eceff1 Promo Strip -->
          <div class="adi-promo-strip-box">
            <strong class="promo-strip-title">{{ t('home.bfTitle') }}</strong>
            <p class="promo-strip-copy">
              {{
                isEn
                  ? 'STILL MISSING A PAIR? Enjoy exclusive Black Friday deals plus extra 10% member vouchers at checkout.'
                  : 'CÒN THIẾU MÓN ĐỒ NÀO? MUỐN SẮM THÊM GÌ KHÔNG? Đừng bỏ lỡ ưu đãi độc quyền BLACK FRIDAY.'
              }}
            </p>
            <router-link :to="lp('shop')" class="promo-strip-link">
              {{ isEn ? 'ADD MORE ITEMS TO YOUR BAG' : 'THÊM NGAY SẢN PHẨM VÀO GIỎ HÀNG' }}
            </router-link>
          </div>

          <!-- Exact Live Adidas 1px solid #d9dbdd Cart Item Cards -->
          <div class="adi-cart-items-stack">
            <article
              v-for="item in cartStore.items"
              :key="item.variantId"
              class="adi-bag-item-card"
            >
              <div class="bag-item-media" @click="router.push(lp(`product/${item.productId}`))">
                <img
                  v-if="item.thumbnailUrl || item.imageUrl || (item.imageUrls && item.imageUrls[0])"
                  :src="item.thumbnailUrl || item.imageUrl || (item.imageUrls && item.imageUrls[0])"
                  :alt="item.productName"
                />
                <div v-else class="bag-media-fallback">SNK</div>
              </div>

              <div class="bag-item-body">
                <div class="bag-item-top-row">
                  <div class="bag-item-info">
                    <h3 class="bag-item-title" @click="router.push(lp(`product/${item.productId}`))">
                      {{ item.productName }}
                    </h3>
                    <div class="bag-item-spec" v-if="item.colorName">{{ item.colorName }}</div>
                    <div class="bag-item-spec" v-if="item.sizeValue">
                      {{ isEn ? 'Size:' : 'Kích cỡ:' }} EU {{ item.sizeValue }}
                    </div>
                    <div class="bag-item-amber">
                      {{ isEn ? 'Authentic in-stock release' : 'Mặt hàng có sẵn mới nhất' }}
                    </div>
                  </div>

                  <!-- Top-Right Stacked Trash + Heart Icons (Exact Adidas) -->
                  <div class="bag-item-icons">
                    <button
                      type="button"
                      class="icon-act-btn"
                      @click="cartStore.removeFromCart(item.variantId)"
                      :title="isEn ? 'Remove item' : 'Xóa sản phẩm'"
                      :aria-label="isEn ? 'Remove' : 'Xóa'"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        <line x1="10" y1="11" x2="10" y2="17"></line>
                        <line x1="14" y1="11" x2="14" y2="17"></line>
                      </svg>
                    </button>
                    <button
                      type="button"
                      class="icon-act-btn"
                      @click="toggleWishlistItem({ id: item.productId, productName: item.productName, salePrice: item.salePrice, thumbnailUrl: item.thumbnailUrl, brandName: item.brandName })"
                      :title="t('account.wishlist')"
                      :aria-label="t('account.wishlist')"
                    >
                      <svg viewBox="0 0 24 24" :fill="wishlistStore.isFavorite(item.productId) ? '#000' : 'none'" stroke="currentColor" stroke-width="1.6">
                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                      </svg>
                    </button>
                  </div>
                </div>

                <!-- Bottom Row: Bordered Qty Select Left + Price Right -->
                <div class="bag-item-bottom-row">
                  <div class="adi-qty-select-wrap">
                    <select
                      class="adi-qty-select"
                      :value="item.quantity"
                      @change="cartStore.updateQuantity(item.variantId, Number($event.target.value))"
                      :aria-label="isEn ? 'Quantity' : 'Số lượng'"
                    >
                      <option v-for="n in Math.max(10, item.quantity)" :key="n" :value="n">{{ n }}</option>
                    </select>
                    <svg class="select-chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                      <polyline points="6 9 12 15 18 9"></polyline>
                    </svg>
                  </div>

                  <div class="bag-item-price-col">
                    <span v-if="item.quantity > 1" class="unit-note">
                      {{ formatCurrency(item.salePrice) }} × {{ item.quantity }}
                    </span>
                    <span class="item-total-price">{{ formatCurrency(item.salePrice * item.quantity) }}</span>
                  </div>
                </div>
              </div>
            </article>
          </div>

          <!-- ============================================================== -->
          <!-- CHECKOUT: SHIPPING & PAYMENT SECTION (Razor-Sharp Adidas Form) -->
          <!-- ============================================================== -->
          <section id="checkout-shipping-section" class="adi-checkout-section">
            <h2 class="checkout-heading">
              {{ isEn ? 'DELIVERY & PAYMENT DETAILS' : 'THÔNG TIN NHẬN HÀNG & THANH TOÁN' }}
            </h2>

            <!-- Member Auto-Fill Banner -->
            <div v-if="!authStore.isAuthenticated" class="adi-member-strip">
              <div class="strip-copy">
                <strong>{{ isEn ? 'Already an ADHA Club member?' : 'Bạn đã có tài khoản hội viên ADHA Club?' }}</strong>
                <span>
                  {{
                    isEn
                      ? 'Sign in or continue with Google to auto-fill your delivery address & track orders.'
                      : 'Đăng nhập hoặc tiếp tục bằng Google để tự động điền địa chỉ nhận hàng và theo dõi đơn.'
                  }}
                </span>
              </div>
              <button type="button" class="btn-strip-black" @click="authModalVisible = true">
                {{ isEn ? 'SIGN IN / GOOGLE →' : 'ĐĂNG NHẬP / GOOGLE →' }}
              </button>
            </div>
            <div v-else class="adi-member-strip logged-in">
              <div class="strip-copy">
                <strong>{{ isEn ? 'Member:' : 'Hội viên:' }} {{ authStore.user?.fullName || authStore.user?.username }}</strong>
                <span>{{ isEn ? 'Your saved contact & shipping details are synced automatically.' : 'Thông tin giao hàng được điền tự động từ tài khoản hội viên của bạn.' }}</span>
              </div>
              <div class="strip-actions">
                <button
                  type="button"
                  class="btn-strip-outline"
                  @click="fillCustomerFromAuth(true); ElMessage.success(isEn ? 'Synced shipping details from profile!' : 'Đã điền nhanh thông tin từ tài khoản!')"
                >
                  {{ isEn ? 'Sync profile' : 'Điền lại từ tài khoản' }}
                </button>
                <span class="member-ok-tag">✓ ADHA CLUB</span>
              </div>
            </div>

            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-position="top"
              class="adi-checkout-form"
            >
              <div class="form-row-2">
                <el-form-item :label="isEn ? 'Recipient Full Name *' : 'Họ và tên người nhận *'" prop="customerName">
                  <el-input v-model="form.customerName" :placeholder="isEn ? 'e.g. Nguyen Van A' : 'VD: Nguyễn Văn A'" />
                </el-form-item>
                <el-form-item :label="isEn ? 'Phone Number *' : 'Số điện thoại *'" prop="customerPhone">
                  <el-input v-model="form.customerPhone" :placeholder="isEn ? 'e.g. 0912345678' : 'VD: 0912345678'" />
                </el-form-item>
              </div>

              <!-- Vietnam Administrative Cascade -->
              <div class="form-row-3 address-cascade-row">
                <el-form-item :label="isEn ? 'Province / City *' : 'Tỉnh / Thành phố *'" prop="provinceCode">
                  <QuickSelect
                    v-model="form.provinceCode"
                    :options="provinces"
                    value-key="code"
                    label-key="name"
                    :placeholder="isEn ? 'Select Province' : 'Chọn Tỉnh / TP'"
                    :search-placeholder="isEn ? 'Search province...' : 'Tìm nhanh Tỉnh / TP...'"
                    clearable
                    :loading="loadingProvinces"
                    @change="handleProvinceChange"
                  />
                </el-form-item>

                <el-form-item :label="isEn ? 'District *' : 'Quận / Huyện *'" prop="districtCode">
                  <QuickSelect
                    v-model="form.districtCode"
                    :options="districts"
                    value-key="code"
                    label-key="name"
                    :placeholder="isEn ? 'Select District' : 'Chọn Quận / Huyện'"
                    :search-placeholder="isEn ? 'Search district...' : 'Tìm nhanh Quận / Huyện...'"
                    clearable
                    :disabled="!form.provinceCode"
                    :loading="loadingDistricts"
                    @change="handleDistrictChange"
                  />
                </el-form-item>

                <el-form-item :label="isEn ? 'Ward *' : 'Phường / Xã *'" prop="wardCode">
                  <QuickSelect
                    v-model="form.wardCode"
                    :options="wards"
                    value-key="code"
                    label-key="name"
                    :placeholder="isEn ? 'Select Ward' : 'Chọn Phường / Xã'"
                    :search-placeholder="isEn ? 'Search ward...' : 'Tìm nhanh Phường / Xã...'"
                    clearable
                    :disabled="!form.districtCode"
                    :loading="loadingWards"
                    @change="handleWardChange"
                  />
                </el-form-item>
              </div>

              <el-form-item :label="isEn ? 'Street Address, Building *' : 'Số nhà, tên đường, tòa nhà *'" prop="streetAddress">
                <el-input
                  v-model="form.streetAddress"
                  :placeholder="isEn ? 'e.g. 45 Cau Giay St, Landmark Tower...' : 'VD: Số 45 Ngõ 12 Đường Cầu Giấy...'"
                  @input="updateFullShippingAddress"
                />
              </el-form-item>

              <div v-if="formattedShippingAddress" class="adi-address-preview">
                <span class="preview-lbl">{{ isEn ? 'Full delivery address:' : 'Địa chỉ nhận hàng hoàn chỉnh:' }}</span>
                <strong>{{ formattedShippingAddress }}</strong>
              </div>

              <div class="form-row-2">
                <el-form-item :label="isEn ? 'Email (for receipt & tracking)' : 'Email (nhận hóa đơn & vận đơn)'" prop="customerEmail">
                  <el-input v-model="form.customerEmail" placeholder="example@email.com" />
                </el-form-item>
                <el-form-item :label="isEn ? 'Delivery Note (optional)' : 'Ghi chú giao hàng (tùy chọn)'" prop="note">
                  <el-input v-model="form.note" :placeholder="isEn ? 'e.g. Call before delivery...' : 'VD: Gọi trước khi giao...'" />
                </el-form-item>
              </div>

              <!-- Payment Methods -->
              <div class="adi-payment-block">
                <h3 class="pay-sub-title">{{ isEn ? 'PAYMENT METHOD' : 'HÌNH THỨC THANH TOÁN' }}</h3>
                <div class="adi-pay-grid">
                  <div
                    class="adi-pay-option"
                    :class="{ active: form.paymentMethod === 'BANK_TRANSFER' }"
                    @click="form.paymentMethod = 'BANK_TRANSFER'"
                  >
                    <span class="pay-radio-ring" :class="{ checked: form.paymentMethod === 'BANK_TRANSFER' }">
                      <span class="pay-radio-dot"></span>
                    </span>
                    <div class="pay-option-body">
                      <div class="pay-option-head">
                        <strong>{{ isEn ? 'SePay VietQR Bank Transfer (MBBank)' : 'Chuyển khoản SePay QR (MBBank)' }}</strong>
                        <span class="pay-rec-badge">{{ isEn ? 'RECOMMENDED' : 'KHUYÊN DÙNG' }}</span>
                      </div>
                      <p>
                        {{
                          isEn
                            ? 'Scan instant VietQR via any banking app or MoMo. Auto-confirmed in seconds.'
                            : 'Quét mã QR tự động qua app ngân hàng hoặc MoMo. Hệ thống tự động gạch nợ lập tức.'
                        }}
                      </p>
                    </div>
                  </div>

                  <div
                    class="adi-pay-option"
                    :class="{ active: form.paymentMethod === 'CASH' }"
                    @click="form.paymentMethod = 'CASH'"
                  >
                    <span class="pay-radio-ring" :class="{ checked: form.paymentMethod === 'CASH' }">
                      <span class="pay-radio-dot"></span>
                    </span>
                    <div class="pay-option-body">
                      <div class="pay-option-head">
                        <strong>{{ isEn ? 'Cash on Delivery (COD)' : 'Thanh toán khi nhận hàng (COD)' }}</strong>
                      </div>
                      <p>
                        {{
                          isEn
                            ? 'Pay in cash to the courier upon receiving and inspecting your sneakers.'
                            : 'Thanh toán tiền mặt cho bưu tá sau khi kiểm tra sản phẩm tận tay.'
                        }}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </el-form>
          </section>

          <!-- Exact Live Adidas "GỢI Ý CHO BẠN" 4-Col Grid Below -->
          <section v-if="recommendedProducts.length" class="adi-cart-recs">
            <h2 class="recs-heading">{{ isEn ? 'TOP PICKS FOR YOU' : 'GỢI Ý CHO BẠN' }}</h2>
            <div class="recs-grid-4">
              <article
                v-for="item in recommendedProducts"
                :key="item.id"
                class="rec-card"
                @click="router.push(lp(`product/${item.id}`))"
              >
                <div class="rec-img-stage">
                  <img :src="item.thumbnailUrl || (item.imageUrls && item.imageUrls[0]) || FALLBACK_IMG" :alt="item.productName" loading="lazy" />
                  <button
                    type="button"
                    class="rec-heart"
                    @click.stop="toggleWishlistItem(item)"
                    :aria-label="t('account.wishlist')"
                  >
                    <svg viewBox="0 0 24 24" :fill="wishlistStore.isFavorite(item.id) ? '#000' : 'none'" stroke="#000" stroke-width="1.5">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                    </svg>
                  </button>
                </div>
                <div class="rec-meta">
                  <div class="rec-price">{{ formatCurrency(item.salePrice) }}</div>
                  <div class="rec-name">{{ item.productName }}</div>
                  <div class="rec-cat">{{ item.brandName || 'Originals' }}</div>
                </div>
              </article>
            </div>
          </section>
        </div>

        <!-- ============================================================== -->
        <!-- RIGHT COLUMN (416px BORDERLESS WHITE "TÓM TẮT ĐƠN HÀNG")       -->
        <!-- ============================================================== -->
        <aside class="cart-summary-col">
          <div class="adi-summary-sticky">
            <h2 class="summary-heading">{{ isEn ? 'ORDER SUMMARY' : 'TÓM TẮT ĐƠN HÀNG' }}</h2>

            <div class="summary-table">
              <div class="sum-row">
                <span>{{ cartStore.totalItems }} {{ isEn ? (cartStore.totalItems > 1 ? 'items' : 'item') : 'sản phẩm' }}</span>
                <span>{{ formatCurrency(cartStore.totalPrice) }}</span>
              </div>
              <div class="sum-row">
                <span>{{ isEn ? 'Delivery' : 'Giao hàng' }}</span>
                <span>{{ cartStore.totalPrice >= 1000000 ? (isEn ? 'Free' : 'Miễn phí') : '30.000₫' }}</span>
              </div>
              <div v-if="voucherDiscount > 0" class="sum-row discount">
                <span>{{ isEn ? 'Voucher' : 'Giảm giá' }} ({{ appliedVoucher?.appliedCode }})</span>
                <span>- {{ formatCurrency(voucherDiscount) }}</span>
              </div>

              <div class="sum-total-block">
                <div class="sum-total-row">
                  <strong>{{ isEn ? 'Total' : 'Tổng' }}</strong>
                  <strong>{{ formatCurrency(finalTotal) }}</strong>
                </div>
                <div class="sum-tax-note">
                  {{ isEn ? '(Inclusive of VAT)' : '(Đã bao gồm thuế VAT)' }}
                </div>
              </div>
            </div>

            <div v-if="cartStore.totalPrice < 1000000" class="adi-freeship-hint">
              {{
                isEn
                  ? `Add ${formatCurrency(1000000 - cartStore.totalPrice)} more for FREE nationwide delivery.`
                  : `Mua thêm ${formatCurrency(1000000 - cartStore.totalPrice)} để được MIỄN PHÍ giao hàng toàn quốc.`
              }}
            </div>

            <!-- Exact Adidas Expandable Promo / Voucher Trigger Row -->
            <div class="adi-promo-accordion">
              <button
                type="button"
                class="promo-acc-trigger"
                @click="promoExpanded = !promoExpanded"
              >
                <span class="promo-acc-text">
                  {{ isEn ? 'Use a promo code or member voucher' : 'Sử dụng điểm hoặc mã khuyến mãi' }}
                </span>
                <span class="promo-acc-chev" :class="{ open: promoExpanded }">›</span>
              </button>

              <div v-if="promoExpanded || appliedVoucher || availableVouchers.length > 0" class="promo-acc-body">
                <div class="voucher-input-row">
                  <input
                    v-model="voucherInput"
                    type="text"
                    class="adi-voucher-input"
                    :placeholder="isEn ? 'Enter promo code (e.g. SNEAKER10)' : 'Nhập mã voucher (VD: SNEAKER10)'"
                    @keyup.enter="applyVoucherCode()"
                    @input="voucherInput = voucherInput.toUpperCase()"
                  />
                  <button
                    type="button"
                    class="btn-apply-code"
                    :disabled="validatingVoucher || !voucherInput.trim()"
                    @click="applyVoucherCode()"
                  >
                    {{ validatingVoucher ? '...' : (isEn ? 'APPLY' : 'ÁP DỤNG') }}
                  </button>
                </div>

                <div v-if="appliedVoucher" class="voucher-applied-chip">
                  <div>
                    <strong>✓ {{ appliedVoucher.appliedCode }}</strong>
                    <span>−{{ formatCurrency(appliedVoucher.discountAmount) }}</span>
                  </div>
                  <button type="button" @click="removeVoucher">{{ isEn ? 'Remove' : 'Bỏ' }}</button>
                </div>
                <div v-else-if="voucherError" class="voucher-err-text">{{ voucherError }}</div>

                <!-- Member Voucher Wallet -->
                <div v-if="promoExpanded && availableVouchers.length > 0" class="voucher-wallet-list">
                  <div
                    v-for="v in availableVouchers"
                    :key="v.code"
                    class="v-ticket"
                    :class="{ selected: appliedVoucher?.appliedCode === v.code, disabled: !v.eligible }"
                  >
                    <div class="v-ticket-left">
                      <div class="v-code-line">
                        <strong>{{ v.code }}</strong>
                        <span class="v-tag" :class="v.sourceType === 'PERSONAL' ? 'personal' : 'public'">
                          {{ v.sourceType === 'PERSONAL' ? (isEn ? 'MEMBER' : 'CÁ NHÂN') : (isEn ? 'PUBLIC' : 'CÔNG KHAI') }}
                        </span>
                      </div>
                      <div class="v-desc">
                        {{
                          v.discountType === 'PERCENT'
                            ? `${isEn ? 'Save' : 'Giảm'} ${v.discountValue}%${v.maxDiscountAmount ? ` (max ${formatCurrency(v.maxDiscountAmount)})` : ''}`
                            : `${isEn ? 'Save' : 'Giảm'} ${formatCurrency(v.discountValue)}`
                        }}
                      </div>
                      <div class="v-min">
                        {{ isEn ? 'Min order' : 'Đơn tối thiểu' }} {{ formatCurrency(v.minOrderAmount || 0) }}
                      </div>
                      <div v-if="!v.eligible && v.ineligibleReason" class="v-reason">{{ v.ineligibleReason }}</div>
                    </div>
                    <button
                      v-if="appliedVoucher?.appliedCode === v.code"
                      type="button"
                      class="btn-v-use active"
                      @click="removeVoucher"
                    >
                      ✓
                    </button>
                    <button
                      v-else
                      type="button"
                      class="btn-v-use"
                      :disabled="!v.eligible"
                      @click="applyVoucherCode(v.code)"
                    >
                      {{ v.eligible ? (isEn ? 'Use' : 'Dùng') : '—' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Exact Adidas 48px Full-Width Offset Dropshadow Black Checkout CTA -->
            <button
              type="button"
              class="btn-adi-checkout"
              :disabled="submitting"
              @click="submitOrder"
            >
              <span>{{ submitting ? (isEn ? 'PROCESSING ORDER...' : 'ĐANG XỬ LÝ...') : (isEn ? 'Checkout' : 'Thanh toán') }}</span>
              <span class="arrow">→</span>
            </button>

            <!-- Exact Adidas Accepted Payment Methods Strip -->
            <div class="adi-accepted-payments">
              <div class="accepted-title">
                {{ isEn ? 'ACCEPTED PAYMENT METHODS' : 'PHƯƠNG THỨC THANH TOÁN ĐƯỢC CHẤP NHẬN' }}
              </div>
              <div class="payment-badges-row">
                <span class="pay-badge visa">VISA</span>
                <span class="pay-badge mc">MC</span>
                <span class="pay-badge cod">COD</span>
                <span class="pay-badge qr">VietQR</span>
                <span class="pay-badge momo">MoMo</span>
              </div>
            </div>

            <!-- Subtle Adidas Guarantees -->
            <ul class="adi-bag-perks">
              <li>✓ {{ isEn ? '100% Legit Check authentic sneakers' : '100% Sneaker chính hãng kiểm định Legit Check' }}</li>
              <li>✓ {{ isEn ? 'Free 7-day size exchange' : 'Hỗ trợ đổi size miễn phí trong 7 ngày' }}</li>
              <li>✓ {{ isEn ? 'Double-Box protective shipping' : 'Đóng gói Double-Box bảo vệ hộp giày nguyên vẹn' }}</li>
            </ul>
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
import { useWishlistStore } from '@/stores/wishlist'
import { useClientAuthStore } from '@/stores/clientAuth'
import storeService from '@/services/storeService'
import vnAddressService from '@/services/vnAddressService'
import { formatCurrency } from '@/utils/formatters'
import { useStoreI18n } from '@/composables/useStoreI18n'
import ClientAuthModal from '@/components/store/ClientAuthModal.vue'

const router = useRouter()
const cartStore = useCartStore()
const wishlistStore = useWishlistStore()
const authStore = useClientAuthStore()
const { t, lp, isEn } = useStoreI18n()

const FALLBACK_IMG =
  'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=600&q=80'

const authModalVisible = ref(false)
const promoExpanded = ref(false)
const recommendedProducts = ref([])

const formRef = ref(null)
const submitting = ref(false)

// Administrative division states
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

const toggleWishlistItem = (prod) => {
  const added = wishlistStore.toggleFavorite(prod)
  if (added) {
    ElMessage.success(isEn.value ? `Saved "${prod.productName}" to wishlist` : `Đã lưu "${prod.productName}" vào yêu thích`)
  } else {
    ElMessage.info(isEn.value ? `Removed "${prod.productName}" from wishlist` : `Đã bỏ "${prod.productName}" khỏi yêu thích`)
  }
}

const loadRecommendations = async () => {
  try {
    const res = await storeService.getProducts({ page: 0, size: 8 })
    const list = res?.data?.content || []
    const inBagIds = new Set(cartStore.items.map((i) => Number(i.productId)))
    const filtered = list.filter((p) => !inBagIds.has(Number(p.id)))
    recommendedProducts.value = (filtered.length >= 4 ? filtered : list).slice(0, 4)
  } catch {
    recommendedProducts.value = []
  }
}

const formattedShippingAddress = computed(() => {
  const parts = []
  if (form.streetAddress && form.streetAddress.trim()) parts.push(form.streetAddress.trim())
  if (form.wardName) parts.push(form.wardName)
  if (form.districtName) parts.push(form.districtName)
  if (form.provinceName) parts.push(form.provinceName)
  return parts.join(', ')
})

const updateFullShippingAddress = () => {
  form.shippingAddress = formattedShippingAddress.value
}

const loadProvinces = async () => {
  loadingProvinces.value = true
  try {
    provinces.value = await vnAddressService.getProvinces()
  } catch (err) {
    console.error('Lỗi tải Tỉnh/TP:', err)
  } finally {
    loadingProvinces.value = false
  }
}

const handleProvinceChange = async (val) => {
  form.districtCode = null
  form.wardCode = null
  form.districtName = ''
  form.wardName = ''
  districts.value = []
  wards.value = []

  const selected = provinces.value.find((p) => p.code === val)
  form.provinceName = selected ? selected.name : ''
  updateFullShippingAddress()

  if (val) {
    loadingDistricts.value = true
    try {
      districts.value = await vnAddressService.getDistricts(val)
    } catch (err) {
      console.error('Lỗi tải Quận/Huyện:', err)
    } finally {
      loadingDistricts.value = false
    }
  }
}

const handleDistrictChange = async (val) => {
  form.wardCode = null
  form.wardName = ''
  wards.value = []

  const selected = districts.value.find((d) => d.code === val)
  form.districtName = selected ? selected.name : ''
  updateFullShippingAddress()

  if (val) {
    loadingWards.value = true
    try {
      wards.value = await vnAddressService.getWards(val)
    } catch (err) {
      console.error('Lỗi tải Phường/Xã:', err)
    } finally {
      loadingWards.value = false
    }
  }
}

const handleWardChange = (val) => {
  const selected = wards.value.find((w) => w.code === val)
  form.wardName = selected ? selected.name : ''
  updateFullShippingAddress()
}

const parseAndFillAddress = async (rawAddress) => {
  if (!rawAddress) return
  form.streetAddress = rawAddress
  form.shippingAddress = rawAddress

  if (!provinces.value || provinces.value.length === 0) {
    await loadProvinces()
  }

  const segments = rawAddress.split(',').map((s) => s.trim()).filter(Boolean)
  if (segments.length >= 2 && provinces.value.length) {
    const lastSeg = segments[segments.length - 1].toLowerCase()
    const matchedProv = provinces.value.find((p) => {
      const pName = p.name.toLowerCase()
      const cleanPName = pName.replace('thành phố ', '').replace('tỉnh ', '')
      return lastSeg.includes(cleanPName) || cleanPName.includes(lastSeg)
    })

    if (matchedProv) {
      form.provinceCode = matchedProv.code
      form.provinceName = matchedProv.name

      loadingDistricts.value = true
      try {
        districts.value = await vnAddressService.getDistricts(matchedProv.code)
      } catch (e) {
        console.error(e)
      } finally {
        loadingDistricts.value = false
      }

      if (segments.length >= 3 && districts.value.length) {
        const distSeg = segments[segments.length - 2].toLowerCase()
        const matchedDist = districts.value.find((d) => {
          const dName = d.name.toLowerCase()
          const cleanDName = dName.replace('quận ', '').replace('huyện ', '').replace('thị xã ', '').replace('thành phố ', '')
          return distSeg.includes(cleanDName) || cleanDName.includes(distSeg)
        })

        if (matchedDist) {
          form.districtCode = matchedDist.code
          form.districtName = matchedDist.name

          loadingWards.value = true
          try {
            wards.value = await vnAddressService.getWards(matchedDist.code)
          } catch (e) {
            console.error(e)
          } finally {
            loadingWards.value = false
          }

          if (segments.length >= 4 && wards.value.length) {
            const wardSeg = segments[segments.length - 3].toLowerCase()
            const matchedWard = wards.value.find((w) => {
              const wName = w.name.toLowerCase()
              const cleanWName = wName.replace('phường ', '').replace('xã ', '').replace('thị trấn ', '')
              return wardSeg.includes(cleanWName) || cleanWName.includes(wardSeg)
            })

            if (matchedWard) {
              form.wardCode = matchedWard.code
              form.wardName = matchedWard.name
            }
          }

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
    if (authStore.user.fullName && (force || !form.customerName)) form.customerName = authStore.user.fullName
    if (authStore.user.phone && (force || !form.customerPhone)) form.customerPhone = authStore.user.phone
    if (authStore.user.email && (force || !form.customerEmail)) form.customerEmail = authStore.user.email
    if (authStore.user.address && (force || (!form.streetAddress && !form.provinceCode))) {
      parseAndFillAddress(authStore.user.address)
    }
  }
}

const enrichCartThumbnails = async () => {
  let changed = false
  for (const item of cartStore.items) {
    if (!item.thumbnailUrl && !item.imageUrl && item.productId) {
      try {
        const res = await storeService.getProductDetail(item.productId)
        const p = res?.data
        const img = p?.thumbnailUrl || (p?.imageUrls && p.imageUrls[0]) || ''
        if (img) {
          item.thumbnailUrl = img
          item.imageUrl = img
          changed = true
        }
      } catch {
        // ignore
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
  loadRecommendations()
})

watch(
  () => authStore.isAuthenticated,
  (isAuth) => {
    if (isAuth) fillCustomerFromAuth(true)
    loadAvailableVouchers()
  }
)

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
      ElMessage.success(data.message || `${isEn.value ? 'Applied voucher' : 'Đã áp dụng voucher'} ${data.appliedCode}`)
    } else {
      appliedVoucher.value = null
      voucherError.value = data?.message || (isEn.value ? 'Invalid promo code' : 'Mã voucher không hợp lệ')
    }
  } catch (err) {
    appliedVoucher.value = null
    voucherError.value = err?.response?.data?.message || (isEn.value ? 'Unable to validate voucher' : 'Không thể kiểm tra mã voucher')
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
            voucherError.value = res?.data?.message || ''
          }
        } catch {
          appliedVoucher.value = null
        }
      }
    }, 350)
  }
)

const rules = computed(() => ({
  customerName: [{ required: true, message: isEn.value ? 'Please enter recipient name' : 'Vui lòng nhập họ tên người nhận', trigger: 'blur' }],
  customerPhone: [
    { required: true, message: isEn.value ? 'Please enter phone number' : 'Vui lòng nhập số điện thoại', trigger: 'blur' },
    { pattern: /^[0-9]{9,11}$/, message: isEn.value ? 'Invalid phone number format' : 'Số điện thoại không đúng định dạng', trigger: 'blur' }
  ],
  provinceCode: [{ required: true, message: isEn.value ? 'Select province/city' : 'Vui lòng chọn Tỉnh / Thành phố', trigger: 'change' }],
  districtCode: [{ required: true, message: isEn.value ? 'Select district' : 'Vui lòng chọn Quận / Huyện', trigger: 'change' }],
  wardCode: [{ required: true, message: isEn.value ? 'Select ward' : 'Vui lòng chọn Phường / Xã', trigger: 'change' }],
  streetAddress: [{ required: true, message: isEn.value ? 'Enter street address' : 'Vui lòng nhập số nhà, tên đường', trigger: 'blur' }]
}))

const finalTotal = computed(() => {
  const shipFee = cartStore.totalPrice >= 1000000 ? 0 : 30000
  return Math.max(0, cartStore.totalPrice - voucherDiscount.value) + shipFee
})

const confirmClearCart = () => {
  ElMessageBox.confirm(
    isEn.value ? 'Remove all items from your bag?' : 'Bạn có chắc chắn muốn xóa toàn bộ sản phẩm trong giỏ hàng?',
    isEn.value ? 'Confirm' : 'Xác nhận',
    {
      confirmButtonText: isEn.value ? 'Clear bag' : 'Xóa giỏ',
      cancelButtonText: isEn.value ? 'Cancel' : 'Hủy',
      type: 'warning'
    }
  )
    .then(() => {
      cartStore.clearCart()
    })
    .catch(() => {})
}

const submitOrder = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error(
        isEn.value
          ? 'Please complete required delivery fields (*)'
          : 'Vui lòng điền đầy đủ thông tin nhận hàng bắt buộc (*)'
      )
      document.getElementById('checkout-shipping-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      return
    }

    if (cartStore.items.length === 0) {
      ElMessage.warning(isEn.value ? 'Your bag is empty!' : 'Giỏ hàng của bạn đang trống!')
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
        items: cartStore.items.map((item) => ({
          variantId: item.variantId,
          quantity: item.quantity
        }))
      }

      const res = await storeService.checkout(payload)
      const orderData = res?.data

      sessionStorage.setItem('last_placed_order', JSON.stringify(orderData))
      cartStore.clearCart()

      ElMessage.success(isEn.value ? 'Order placed successfully!' : 'Đặt hàng thành công!')
      router.push(lp(`order-success/${orderData.orderCode}`))
    } catch (err) {
      console.error('Lỗi đặt hàng:', err)
      const msg = (err.response && err.response.data && err.response.data.message) || (isEn.value ? 'Checkout failed' : 'Có lỗi xảy ra khi tạo đơn hàng')
      ElMessage.error(msg)
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.store-cart {
  padding: 40px 0 96px;
  background: #ffffff;
  color: #000000;
}

.adi-cart-container {
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

.cart-title {
  font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
  font-size: 46px;
  font-style: normal;
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  line-height: 1.04;
  color: #000000;
  margin: 0;

  @media (max-width: 640px) {
    font-size: 34px;
  }
}

/* ============ EMPTY CART STATE (CLEAN LEFT-ALIGNED ADIDAS) ============ */
.adi-empty-cart {
  padding-top: 12px;

  .cart-empty-sub {
    font-size: 15px;
    color: #000000;
    margin: 12px 0 28px;
    max-width: 560px;
    line-height: 1.55;
  }

  .btn-adi-offset-inline {
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: 20px;
    height: 50px;
    padding: 0 26px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    text-decoration: none;
    font-size: 14px;
    font-weight: 800;
    letter-spacing: 0.12em;
    text-transform: uppercase;

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
    }

    &:hover {
      color: #cccccc;
    }
  }
}

/* ============ POPULATED CART 2-COLUMN SPLIT (LEFT ~65% / RIGHT 416px) ============ */
.adi-cart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  gap: 56px;
  align-items: start;

  @media (max-width: 1080px) {
    grid-template-columns: minmax(0, 1fr) 340px;
    gap: 36px;
  }

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
    gap: 40px;
  }
}

.adi-bag-header {
  margin-bottom: 24px;

  .bag-title-row {
    display: flex;
    align-items: baseline;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 8px;

    .cart-count-muted {
      font-size: 15px;
      color: #767677;
      font-weight: 500;
    }
  }

  .bag-sub-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;

    .bag-reserve-note {
      font-size: 14.5px;
      color: #000000;
      margin: 0;
    }

    .btn-clear-bag {
      background: none;
      border: none;
      padding: 0;
      font-family: inherit;
      font-size: 13px;
      font-weight: 600;
      color: #767677;
      text-decoration: underline;
      cursor: pointer;

      &:hover {
        color: #000000;
      }
    }
  }
}

/* #eceff1 Promo Strip Box */
.adi-promo-strip-box {
  background: #eceff1;
  padding: 20px 24px;
  margin-bottom: 28px;

  .promo-strip-title {
    display: block;
    font-size: 14px;
    font-weight: 800;
    letter-spacing: 0.04em;
    color: #000000;
    margin-bottom: 6px;
  }

  .promo-strip-copy {
    font-size: 14px;
    line-height: 1.5;
    color: #000000;
    margin: 0 0 12px;
  }

  .promo-strip-link {
    font-size: 13px;
    font-weight: 700;
    color: #000000;
    text-decoration: underline;
    text-underline-offset: 3px;
    letter-spacing: 0.04em;

    &:hover {
      background: #000000;
      color: #ffffff;
      text-decoration: none;
    }
  }
}

/* Exact Live Adidas Cart Item Card (1px solid #d9dbdd, 220px Square Image) */
.adi-cart-items-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 44px;
}

.adi-bag-item-card {
  display: flex;
  border: 1px solid #d9dbdd;
  background: #ffffff;

  @media (max-width: 600px) {
    flex-direction: column;
  }

  .bag-item-media {
    width: 220px;
    height: 220px;
    background: #eceff1;
    flex-shrink: 0;
    cursor: pointer;
    overflow: hidden;

    @media (max-width: 600px) {
      width: 100%;
      height: 240px;
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .bag-media-fallback {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      color: #767677;
    }
  }

  .bag-item-body {
    flex: 1;
    padding: 18px 20px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    min-width: 0;
  }

  .bag-item-top-row {
    display: flex;
    justify-content: space-between;
    gap: 16px;

    .bag-item-info {
      .bag-item-title {
        font-size: 15px;
        font-weight: 500;
        text-transform: uppercase;
        color: #000000;
        margin: 0 0 8px;
        cursor: pointer;
        line-height: 1.35;

        &:hover {
          text-decoration: underline;
        }
      }

      .bag-item-spec {
        font-size: 14px;
        color: #000000;
        line-height: 1.45;
      }

      .bag-item-amber {
        margin-top: 8px;
        font-size: 13.5px;
        font-weight: 500;
        color: #9a6700;
      }
    }

    .bag-item-icons {
      display: flex;
      flex-direction: column;
      gap: 12px;
      flex-shrink: 0;

      .icon-act-btn {
        width: 30px;
        height: 30px;
        background: transparent;
        border: none;
        padding: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #000000;
        cursor: pointer;

        svg {
          width: 20px;
          height: 20px;
        }

        &:hover {
          opacity: 0.65;
        }
      }
    }
  }

  .bag-item-bottom-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-top: 20px;
    gap: 16px;

    .adi-qty-select-wrap {
      position: relative;
      width: 92px;

      .adi-qty-select {
        width: 100%;
        height: 44px;
        padding: 0 32px 0 14px;
        border: 1px solid #767677;
        border-radius: 0;
        background: #ffffff;
        font-family: inherit;
        font-size: 14.5px;
        font-weight: 500;
        color: #000000;
        appearance: none;
        cursor: pointer;
        outline: none;

        &:focus {
          border-color: #000000;
        }
      }

      .select-chev {
        position: absolute;
        right: 12px;
        top: 50%;
        transform: translateY(-50%);
        width: 16px;
        height: 16px;
        pointer-events: none;
      }
    }

    .bag-item-price-col {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 2px;

      .unit-note {
        font-size: 12px;
        color: #767677;
      }

      .item-total-price {
        font-size: 15.5px;
        font-weight: 700;
        color: #000000;
        font-variant-numeric: tabular-nums;
      }
    }
  }
}

/* ============ CHECKOUT SHIPPING & PAYMENT FORM (0px ADIDAS GEOMETRY) ============ */
.adi-checkout-section {
  padding-top: 36px;
  border-top: 1px solid #d9dbdd;
  margin-bottom: 48px;

  .checkout-heading {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 30px;
    font-weight: 800;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    margin: 0 0 20px;
  }

  .adi-member-strip {
    background: #eceff1;
    padding: 16px 20px;
    margin-bottom: 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;

    &.logged-in {
      border-left: 3px solid #427f5c;
    }

    .strip-copy {
      display: flex;
      flex-direction: column;
      gap: 3px;

      strong {
        font-size: 14px;
        font-weight: 700;
      }

      span {
        font-size: 13px;
        color: #555555;
      }
    }

    .btn-strip-black {
      height: 40px;
      padding: 0 18px;
      background: #000000;
      color: #ffffff;
      border: 1px solid #000000;
      border-radius: 0;
      font-family: inherit;
      font-size: 12px;
      font-weight: 800;
      letter-spacing: 0.08em;
      cursor: pointer;
    }

    .strip-actions {
      display: flex;
      align-items: center;
      gap: 10px;

      .btn-strip-outline {
        height: 36px;
        padding: 0 14px;
        background: #ffffff;
        color: #000000;
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

      .member-ok-tag {
        font-size: 11px;
        font-weight: 800;
        letter-spacing: 0.08em;
        color: #427f5c;
      }
    }
  }

  .adi-checkout-form {
    .form-row-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;

      @media (max-width: 640px) {
        grid-template-columns: 1fr;
        gap: 0;
      }
    }

    .form-row-3 {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      gap: 14px;

      @media (max-width: 720px) {
        grid-template-columns: 1fr;
        gap: 0;
      }
    }

    :deep(.el-form-item__label) {
      font-size: 12px;
      font-weight: 700;
      color: #000000;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      padding-bottom: 6px;
    }

    :deep(.el-input__wrapper) {
      border-radius: 0 !important;
      height: 46px;
      box-shadow: 0 0 0 1px #767677 inset !important;
      background: #ffffff;

      &.is-focus {
        box-shadow: 0 0 0 1.5px #000000 inset !important;
      }
    }
  }

  .adi-address-preview {
    padding: 12px 16px;
    background: #f7f8f9;
    border-left: 3px solid #000000;
    margin-bottom: 18px;
    font-size: 13.5px;

    .preview-lbl {
      display: block;
      font-size: 11.5px;
      color: #767677;
      margin-bottom: 2px;
    }
  }

  .adi-payment-block {
    margin-top: 24px;

    .pay-sub-title {
      font-size: 13px;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      margin: 0 0 14px;
    }

    .adi-pay-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;

      @media (max-width: 640px) {
        grid-template-columns: 1fr;
      }
    }

    .adi-pay-option {
      display: flex;
      align-items: flex-start;
      gap: 14px;
      padding: 16px;
      border: 1px solid #d9dbdd;
      background: #ffffff;
      cursor: pointer;
      transition: border-color 0.14s ease;

      &.active {
        border: 1.5px solid #000000;
      }

      .pay-radio-ring {
        width: 20px;
        height: 20px;
        border-radius: 50%;
        border: 1.5px solid #767677;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        margin-top: 2px;

        .pay-radio-dot {
          width: 10px;
          height: 10px;
          border-radius: 50%;
          background: transparent;
        }

        &.checked {
          border-color: #000000;
          .pay-radio-dot {
            background: #000000;
          }
        }
      }

      .pay-option-body {
        .pay-option-head {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;
          margin-bottom: 4px;

          strong {
            font-size: 14px;
            font-weight: 700;
          }

          .pay-rec-badge {
            font-size: 10px;
            font-weight: 800;
            letter-spacing: 0.08em;
            background: #000000;
            color: #ffffff;
            padding: 2px 6px;
          }
        }

        p {
          margin: 0;
          font-size: 12.5px;
          line-height: 1.45;
          color: #555555;
        }
      }
    }
  }
}

/* ============ RIGHT COLUMN: BORDERLESS WHITE "TÓM TẮT ĐƠN HÀNG" ============ */
.adi-summary-sticky {
  position: sticky;
  top: 108px;
  background: #ffffff;

  .summary-heading {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 32px;
    font-style: normal;
    font-weight: 800;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    line-height: 1.05;
    margin: 0 0 24px;
  }

  .summary-table {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 20px;

    .sum-row {
      display: flex;
      justify-content: space-between;
      font-size: 15px;
      color: #000000;

      &.discount {
        color: #767677;
      }
    }

    .sum-total-block {
      margin-top: 10px;
      padding-top: 14px;
      border-top: 1px solid #d9dbdd;

      .sum-total-row {
        display: flex;
        justify-content: space-between;
        font-size: 16px;
        color: #000000;
      }

      .sum-tax-note {
        font-size: 13px;
        color: #767677;
        margin-top: 4px;
      }
    }
  }

  .adi-freeship-hint {
    padding: 10px 14px;
    background: #eceff1;
    font-size: 13px;
    line-height: 1.45;
    margin-bottom: 18px;
  }

  /* Underlined Expandable Promo Trigger Row */
  .adi-promo-accordion {
    margin-bottom: 24px;

    .promo-acc-trigger {
      width: 100%;
      padding: 10px 0;
      background: transparent;
      border: none;
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;
      font-family: inherit;

      .promo-acc-text {
        font-size: 15px;
        font-weight: 700;
        color: #000000;
        text-decoration: underline;
        text-underline-offset: 4px;
      }

      .promo-acc-chev {
        font-size: 22px;
        line-height: 1;
        transition: transform 0.16s ease;

        &.open {
          transform: rotate(90deg);
        }
      }
    }

    .promo-acc-body {
      padding-top: 12px;
    }

    .voucher-input-row {
      display: flex;
      gap: 8px;

      .adi-voucher-input {
        flex: 1;
        height: 44px;
        padding: 0 12px;
        border: 1px solid #767677;
        border-radius: 0;
        font-family: inherit;
        font-size: 13.5px;
        outline: none;

        &:focus {
          border-color: #000000;
        }
      }

      .btn-apply-code {
        height: 44px;
        padding: 0 16px;
        background: #000000;
        color: #ffffff;
        border: 1px solid #000000;
        border-radius: 0;
        font-family: inherit;
        font-size: 12px;
        font-weight: 800;
        letter-spacing: 0.08em;
        cursor: pointer;

        &:disabled {
          opacity: 0.45;
          cursor: not-allowed;
        }
      }
    }

    .voucher-applied-chip {
      margin-top: 10px;
      padding: 10px 12px;
      background: #eceff1;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 13px;

      strong {
        margin-right: 8px;
      }

      button {
        background: none;
        border: none;
        text-decoration: underline;
        font-size: 12.5px;
        cursor: pointer;
        color: #000000;
      }
    }

    .voucher-err-text {
      margin-top: 8px;
      font-size: 12.5px;
      color: #e32b2b;
    }

    .voucher-wallet-list {
      margin-top: 12px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-height: 220px;
      overflow-y: auto;

      .v-ticket {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 10px;
        padding: 10px 12px;
        border: 1px solid #d9dbdd;

        &.selected {
          border-color: #000000;
          background: #f7f8f9;
        }

        &.disabled {
          opacity: 0.55;
        }

        .v-code-line {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 13px;

          .v-tag {
            font-size: 9.5px;
            font-weight: 800;
            padding: 1px 5px;
            background: #000000;
            color: #ffffff;

            &.personal {
              background: #427f5c;
            }
          }
        }

        .v-desc {
          font-size: 12.5px;
          font-weight: 600;
          margin-top: 2px;
        }

        .v-min,
        .v-reason {
          font-size: 11.5px;
          color: #767677;
        }

        .v-reason {
          color: #e32b2b;
        }

        .btn-v-use {
          height: 32px;
          padding: 0 12px;
          background: #000000;
          color: #ffffff;
          border: 1px solid #000000;
          border-radius: 0;
          font-family: inherit;
          font-size: 11.5px;
          font-weight: 700;
          cursor: pointer;

          &:disabled {
            background: #d9dbdd;
            border-color: #d9dbdd;
            color: #767677;
            cursor: not-allowed;
          }
        }
      }
    }
  }

  /* Full-Width 48px Black Offset Dropshadow Checkout CTA */
  .btn-adi-checkout {
    position: relative;
    width: 100%;
    height: 50px;
    padding: 0 20px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    border-radius: 0;
    font-family: inherit;
    font-size: 15px;
    font-weight: 700;
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;
    margin-bottom: 28px;

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      transform: translate(3px, 3px);
      border: 1px solid #000000;
      pointer-events: none;
    }

    .arrow {
      font-size: 20px;
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

  .adi-accepted-payments {
    margin-bottom: 22px;

    .accepted-title {
      font-size: 11.5px;
      font-weight: 800;
      letter-spacing: 0.1em;
      color: #000000;
      margin-bottom: 12px;
    }

    .payment-badges-row {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;

      .pay-badge {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 28px;
        padding: 0 10px;
        border: 1px solid #d9dbdd;
        font-size: 11px;
        font-weight: 900;
        letter-spacing: 0.04em;

        &.visa {
          color: #1a1f71;
        }
        &.mc {
          color: #eb001b;
        }
        &.momo {
          background: #a50064;
          border-color: #a50064;
          color: #ffffff;
        }
        &.qr {
          color: #000000;
        }
        &.cod {
          color: #000000;
        }
      }
    }
  }

  .adi-bag-perks {
    list-style: none;
    padding: 18px 0 0;
    margin: 0;
    border-top: 1px solid #eceff1;
    display: flex;
    flex-direction: column;
    gap: 8px;
    font-size: 13px;
    color: #555555;
  }
}

/* ============ "GỢI Ý CHO BẠN" 4-COL PRODUCT GRID ============ */
.adi-cart-recs {
  padding-top: 40px;
  border-top: 1px solid #d9dbdd;
  margin-top: 40px;

  .recs-heading {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 32px;
    font-weight: 800;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    margin: 0 0 20px;
  }

  .recs-grid-4 {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px 8px;

    @media (max-width: 720px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  .rec-card {
    border: 1px solid transparent;
    cursor: pointer;

    &:hover {
      border-color: #000000;
    }

    .rec-img-stage {
      position: relative;
      aspect-ratio: 1 / 1;
      background: #eceff1;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
      }

      .rec-heart {
        position: absolute;
        top: 8px;
        right: 8px;
        width: 30px;
        height: 30px;
        background: transparent;
        border: none;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;

        svg {
          width: 18px;
          height: 18px;
        }
      }
    }

    .rec-meta {
      padding: 10px 8px 14px;

      .rec-price {
        font-size: 13.5px;
        font-weight: 700;
        margin-bottom: 4px;
      }

      .rec-name {
        font-size: 13.5px;
        line-height: 1.35;
        color: #000000;
      }

      .rec-cat {
        font-size: 12.5px;
        color: #767677;
        margin-top: 2px;
      }
    }
  }
}
</style>
