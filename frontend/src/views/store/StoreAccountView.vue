<template>
  <div class="adi-account-page">
    <div class="adi-account-container">
      <!-- Not Logged In State -->
      <div v-if="!authStore.isAuthenticated" class="adi-account-guest-card">
        <span class="guest-wordmark">adhaclub</span>
        <h1 class="guest-heading">
          {{ isEn ? 'SIGN IN TO MANAGE YOUR ACCOUNT' : 'ĐĂNG NHẬP ĐỂ QUẢN LÝ TÀI KHOẢN' }}
        </h1>
        <p class="guest-sub">
          {{
            isEn
              ? 'View and edit your personal profile, manage up to 5 delivery addresses, and track your sneaker orders.'
              : 'Xem và chỉnh sửa thông tin cá nhân, quản lý tối đa 5 địa chỉ giao hàng và theo dõi toàn bộ đơn hàng của bạn.'
          }}
        </p>
        <button type="button" class="btn-adi-black-offset" @click="authModalVisible = true">
          <span>{{ isEn ? 'SIGN IN / REGISTER' : 'ĐĂNG NHẬP / ĐĂNG KÝ' }}</span>
          <span class="arrow">→</span>
        </button>
      </div>

      <!-- ================================================================ -->
      <!-- EXACT LIVE 2026 ADIDAS 2-COLUMN ACCOUNT LAYOUT (#f5f5f5 CANVAS)  -->
      <!-- Matches https://www.adidas.com.vn/vi/my-account/profile 99%      -->
      <!-- ================================================================ -->
      <div v-else class="adi-account-grid">
        <!-- LEFT SIDEBAR: TỔNG QUAN TÀI KHOẢN -->
        <aside class="adi-account-sidebar">
          <h2 class="sidebar-kicker">
            {{ isEn ? 'ACCOUNT OVERVIEW' : 'TỔNG QUAN TÀI KHOẢN' }}
          </h2>

          <nav class="sidebar-menu-card" aria-label="Account navigation">
            <router-link
              :to="lp('my-account/profile')"
              class="sidebar-item"
              :class="{ active: activeSection === 'profile' }"
            >
              <span>{{ isEn ? 'Personal Information' : 'Thông Tin Cá Nhân' }}</span>
              <span class="chev">›</span>
            </router-link>

            <router-link
              :to="lp('my-account/address-book')"
              class="sidebar-item"
              :class="{ active: activeSection === 'address-book' }"
            >
              <span>{{ isEn ? 'Address Book' : 'Sổ Địa Chỉ' }}</span>
              <span class="chev">›</span>
            </router-link>

            <router-link :to="lp('order-history')" class="sidebar-item">
              <span>{{ isEn ? 'My Orders' : 'Đơn Hàng Của Tôi' }}</span>
              <span class="chev">›</span>
            </router-link>

            <router-link
              :to="lp('my-account/preferences')"
              class="sidebar-item"
              :class="{ active: activeSection === 'preferences' }"
            >
              <span>{{ isEn ? 'Preferences' : 'Tuỳ chọn' }}</span>
              <span class="chev">›</span>
            </router-link>

            <button type="button" class="sidebar-item logout-item" @click="handleLogout">
              <span>{{ isEn ? 'Log out' : 'Đăng xuất' }}</span>
            </button>
          </nav>
        </aside>

        <!-- RIGHT WHITE CONTENT CARD -->
        <main class="adi-account-card">
          <!-- ========================================================== -->
          <!-- SECTION 1: THÔNG TIN CÁ NHÂN (/my-account/profile)          -->
          <!-- ========================================================== -->
          <section v-if="activeSection === 'profile'" class="account-pane">
            <h1 class="pane-title">{{ isEn ? 'MY DETAILS' : 'THÔNG TIN CỦA TÔI' }}</h1>
            <p class="pane-subtitle">
              {{
                isEn
                  ? 'Feel free to edit any of your details below so your account is always up to date.'
                  : 'Hãy chỉnh sửa bất kỳ thông tin chi tiết nào bên dưới để tài khoản của bạn luôn được cập nhật.'
              }}
            </p>

            <!-- Block 1: THÔNG TIN CHI TIẾT -->
            <div class="detail-group">
              <h3 class="group-heading">{{ isEn ? 'DETAILS' : 'THÔNG TIN CHI TIẾT' }}</h3>
              <div class="detail-lines">
                <div class="detail-line uppercase-val">
                  {{ (authStore.user?.fullName || authStore.user?.username || '—').toUpperCase() }}
                </div>
                <div class="detail-line" v-if="extraProfile.dob">
                  {{ extraProfile.dob }}
                </div>
                <div class="detail-line uppercase-val">
                  {{ genderDisplayLabel }}
                </div>
                <div class="detail-line" v-if="authStore.user?.phone">
                  {{ authStore.user.phone }}
                </div>
              </div>
              <button type="button" class="btn-adi-edit-underline" @click="openEditProfileModal">
                {{ isEn ? 'EDIT' : 'CHỈNH SỬA' }}
              </button>
            </div>

            <!-- Block 2: CHI TIẾT ĐĂNG NHẬP -->
            <div class="detail-group">
              <h3 class="group-heading">{{ isEn ? 'LOGIN DETAILS' : 'CHI TIẾT ĐĂNG NHẬP' }}</h3>

              <div class="sub-detail-block">
                <div class="sub-label">EMAIL</div>
                <div class="detail-line uppercase-val">
                  {{ (authStore.user?.email || (isEn ? 'NOT SET' : 'CHƯA CẬP NHẬT')).toUpperCase() }}
                </div>
                <button type="button" class="btn-adi-edit-underline" @click="openEditEmailModal">
                  {{ isEn ? 'EDIT' : 'CHỈNH SỬA' }}
                </button>
              </div>

              <div class="sub-detail-block">
                <div class="sub-label">{{ isEn ? 'USERNAME' : 'TÊN ĐĂNG NHẬP' }}</div>
                <div class="detail-line">@{{ authStore.user?.username }}</div>
              </div>

              <div class="sub-detail-block">
                <div class="sub-label">{{ isEn ? 'PASSWORD' : 'MẬT KHẨU' }}</div>
                <div class="detail-line password-dots">************</div>
              </div>
            </div>

            <!-- Block 3: ĐỊA CHỈ MẶC ĐỊNH QUICK SUMMARY -->
            <div class="detail-group">
              <h3 class="group-heading">{{ isEn ? 'DEFAULT DELIVERY ADDRESS' : 'ĐỊA CHỈ GIAO HÀNG MẶC ĐỊNH' }}</h3>
              <div class="detail-lines">
                <div class="detail-line">
                  {{ defaultAddressText || (isEn ? 'No default address saved yet.' : 'Bạn chưa lưu địa chỉ giao hàng mặc định.') }}
                </div>
              </div>
              <router-link :to="lp('my-account/address-book')" class="btn-adi-edit-underline">
                {{ isEn ? 'MANAGE ADDRESS BOOK' : 'QUẢN LÝ SỔ ĐỊA CHỈ' }}
              </router-link>
            </div>

            <!-- Block 4: QUẢN LÝ TÀI KHOẢN -->
            <div class="detail-group">
              <h3 class="group-heading">{{ isEn ? 'MANAGE ACCOUNT' : 'QUẢN LÝ TÀI KHOẢN' }}</h3>
              <p class="manage-note">
                {{
                  isEn
                    ? 'Sign out of your ADHA Club session on this browser.'
                    : 'Đăng xuất phiên làm việc ADHA Club của bạn khỏi trình duyệt này.'
                }}
              </p>
              <button type="button" class="btn-adi-outline-sharp" @click="handleLogout">
                <span>{{ isEn ? 'LOG OUT' : 'ĐĂNG XUẤT' }}</span>
                <span>→</span>
              </button>
            </div>
          </section>

          <!-- ========================================================== -->
          <!-- SECTION 2: SỔ ĐỊA CHỈ (/my-account/address-book)            -->
          <!-- ========================================================== -->
          <section v-else-if="activeSection === 'address-book'" class="account-pane">
            <h1 class="pane-title">{{ isEn ? 'ADDRESS BOOK' : 'SỔ ĐỊA CHỈ' }}</h1>
            <p class="pane-subtitle">
              {{
                isEn
                  ? `You have ${Math.max(0, 5 - addresses.length)}/5 address slots remaining. Your default address auto-fills at checkout.`
                  : `Bạn còn ${Math.max(0, 5 - addresses.length)}/5 ô địa chỉ. Địa chỉ mặc định sẽ tự động điền khi thanh toán.`
              }}
            </p>

            <!-- Exact Adidas 2-Column Sharp Address Cards Grid -->
            <div class="adi-address-grid">
              <!-- Card 1: "+ Thêm Địa Chỉ" Tile -->
              <button
                v-if="addresses.length < 5"
                type="button"
                class="adi-address-tile add-tile"
                @click="openAddressModal(null)"
              >
                <span class="add-tile-title">{{ isEn ? 'Add New Address' : 'Thêm Địa Chỉ' }}</span>
                <span class="add-tile-plus">+</span>
              </button>

              <!-- Saved Address Cards -->
              <article
                v-for="addr in addresses"
                :key="addr.id"
                class="adi-address-tile saved-tile"
                :class="{ 'is-default': addr.isDefault }"
              >
                <div class="addr-top">
                  <div class="addr-head-row">
                    <strong class="addr-name">{{ addr.fullName }}</strong>
                    <span v-if="addr.isDefault" class="addr-default-pill">
                      {{ isEn ? 'DEFAULT' : 'MẶC ĐỊNH' }}
                    </span>
                  </div>
                  <div class="addr-lines">
                    <p>{{ addr.streetAddress || addr.fullAddress }}</p>
                    <p v-if="addr.wardName || addr.districtName || addr.provinceName">
                      {{ [addr.wardName, addr.districtName, addr.provinceName].filter(Boolean).join(', ') }}
                    </p>
                    <p class="addr-phone">{{ addr.phone }}</p>
                  </div>
                </div>

                <div class="addr-actions">
                  <button type="button" class="addr-link-btn" @click="openAddressModal(addr)">
                    {{ isEn ? 'EDIT' : 'CHỈNH SỬA' }}
                  </button>
                  <button type="button" class="addr-link-btn" @click="removeAddress(addr)">
                    {{ isEn ? 'REMOVE' : 'XÓA' }}
                  </button>
                  <button
                    v-if="!addr.isDefault"
                    type="button"
                    class="addr-link-btn"
                    @click="setDefaultAddress(addr)"
                  >
                    {{ isEn ? 'SET AS DEFAULT' : 'ĐẶT MẶC ĐỊNH' }}
                  </button>
                </div>
              </article>
            </div>
          </section>

          <!-- ========================================================== -->
          <!-- SECTION 3: TUỲ CHỌN (/my-account/preferences)               -->
          <!-- ========================================================== -->
          <section v-else class="account-pane">
            <h1 class="pane-title">{{ isEn ? 'PREFERENCES' : 'TUỲ CHỌN' }}</h1>
            <p class="pane-subtitle">
              {{
                isEn
                  ? 'Choose your preferred storefront language and ADHA Club notifications.'
                  : 'Chọn ngôn ngữ hiển thị cửa hàng và tuỳ chọn thông báo hội viên ADHA Club.'
              }}
            </p>

            <div class="detail-group">
              <h3 class="group-heading">{{ isEn ? 'STOREFRONT LANGUAGE' : 'NGÔN NGỮ HIỂN THỊ' }}</h3>
              <div class="pref-lang-row">
                <button
                  type="button"
                  class="pref-lang-btn"
                  :class="{ active: locale === 'vi' }"
                  @click="switchLocale('vi', router, route)"
                >
                  Tiếng Việt (VN)
                </button>
                <button
                  type="button"
                  class="pref-lang-btn"
                  :class="{ active: locale === 'en' }"
                  @click="switchLocale('en', router, route)"
                >
                  English (US)
                </button>
              </div>
            </div>

            <div class="detail-group">
              <h3 class="group-heading">{{ isEn ? 'COMMUNICATION' : 'THÔNG BÁO ƯU ĐÃI' }}</h3>
              <label class="pref-check-row">
                <input type="checkbox" v-model="prefDrops" />
                <span>
                  {{
                    isEn
                      ? 'Notify me about Black Friday sales, limited sneaker releases & personal member vouchers.'
                      : 'Nhận thông báo ưu đãi Black Friday, đợt phát hành sneaker giới hạn và voucher hội viên.'
                  }}
                </span>
              </label>
            </div>
          </section>
        </main>
      </div>

      <!-- Exact Live Adidas Bottom Help Strip ("BẠN CẦN TRỢ GIÚP?") -->
      <section class="adi-account-help-strip">
        <h3 class="help-heading">{{ isEn ? 'NEED HELP?' : 'BẠN CẦN TRỢ GIÚP?' }}</h3>
        <div class="help-links-grid">
          <router-link :to="lp('shop')">{{ isEn ? 'Products & Sizes' : 'Sản phẩm & Kích cỡ' }}</router-link>
          <router-link :to="lp('cart')">{{ isEn ? 'Ordering & Payment' : 'Đặt Hàng & Thanh Toán' }}</router-link>
          <router-link :to="lp('tracking')">{{ isEn ? 'Delivery & Order Tracking' : 'Giao hàng & Theo dõi đơn' }}</router-link>
          <router-link :to="{ path: lp('shop'), query: { sort: 'price_asc' } }">{{ isEn ? 'Promotions & Vouchers' : 'Chương Trình Khuyến Mãi' }}</router-link>
          <router-link :to="lp('my-orders')">{{ isEn ? 'Returns & Exchanges' : 'Trả Lại Hàng & Hoàn Tiền' }}</router-link>
        </div>
      </section>
    </div>

    <!-- ================================================================ -->
    <!-- EXACT LIVE ADIDAS "CHỈNH SỬA THÔNG TIN CỦA BẠN" MODAL            -->
    <!-- Matches steps/645/media_0.jpg pixel-for-pixel                    -->
    <!-- ================================================================ -->
    <transition name="adi-modal-fade">
      <div v-if="editProfileModalOpen" class="adi-modal-scrim" @click.self="editProfileModalOpen = false">
        <div class="adi-sharp-modal" role="dialog" aria-modal="true">
          <div class="modal-top-bar">
            <h2 class="modal-heading">
              {{ isEn ? 'EDIT YOUR DETAILS' : 'CHỈNH SỬA THÔNG TIN CỦA BẠN' }}
            </h2>
            <button type="button" class="modal-x-btn" @click="editProfileModalOpen = false" aria-label="Close">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>

          <form class="adi-modal-form" @submit.prevent="saveProfileDetails">
            <!-- Floating-Inside-Label Input: Tên* -->
            <div class="adi-float-box" :class="{ valid: profileDraft.firstName.trim().length > 0 }">
              <label class="float-lbl">{{ isEn ? 'First Name*' : 'Tên*' }}</label>
              <input v-model="profileDraft.firstName" type="text" class="float-input" required />
              <span v-if="profileDraft.firstName.trim()" class="valid-tick">✓</span>
            </div>

            <!-- Floating-Inside-Label Input: Họ* -->
            <div class="adi-float-box" :class="{ valid: profileDraft.lastName.trim().length > 0 }">
              <label class="float-lbl">{{ isEn ? 'Last Name*' : 'Họ*' }}</label>
              <input v-model="profileDraft.lastName" type="text" class="float-input" />
              <span v-if="profileDraft.lastName.trim()" class="valid-tick">✓</span>
            </div>

            <!-- Floating-Inside-Label Input: Số điện thoại* -->
            <div class="adi-float-box" :class="{ valid: /^[0-9]{9,11}$/.test(profileDraft.phone.trim()) }">
              <label class="float-lbl">{{ isEn ? 'Phone Number*' : 'Số điện thoại*' }}</label>
              <input v-model="profileDraft.phone" type="text" class="float-input" />
              <span v-if="/^[0-9]{9,11}$/.test(profileDraft.phone.trim())" class="valid-tick">✓</span>
            </div>

            <!-- NGÀY SINH (3-Column Row: ngày* | tháng* | năm*) -->
            <div class="modal-section-lbl">{{ isEn ? 'DATE OF BIRTH' : 'NGÀY SINH' }}</div>
            <div class="dob-grid-3">
              <div class="adi-float-box" :class="{ valid: profileDraft.dobDay.trim().length > 0 }">
                <label class="float-lbl">{{ isEn ? 'day' : 'ngày' }}</label>
                <input v-model="profileDraft.dobDay" type="text" maxlength="2" placeholder="DD" class="float-input" />
                <span v-if="profileDraft.dobDay.trim()" class="valid-tick">✓</span>
              </div>
              <div class="adi-float-box" :class="{ valid: profileDraft.dobMonth.trim().length > 0 }">
                <label class="float-lbl">{{ isEn ? 'month' : 'tháng' }}</label>
                <input v-model="profileDraft.dobMonth" type="text" maxlength="2" placeholder="MM" class="float-input" />
                <span v-if="profileDraft.dobMonth.trim()" class="valid-tick">✓</span>
              </div>
              <div class="adi-float-box" :class="{ valid: profileDraft.dobYear.trim().length === 4 }">
                <label class="float-lbl">{{ isEn ? 'year' : 'năm' }}</label>
                <input v-model="profileDraft.dobYear" type="text" maxlength="4" placeholder="YYYY" class="float-input" />
                <span v-if="profileDraft.dobYear.trim().length === 4" class="valid-tick">✓</span>
              </div>
            </div>

            <!-- GIỚI TÍNH (Horizontal Circular Radios: Nam · Nữ · Khác) -->
            <div class="modal-section-lbl">{{ isEn ? 'GENDER' : 'GIỚI TÍNH' }}</div>
            <div class="gender-radio-row">
              <label
                v-for="g in genderOptions"
                :key="g.value"
                class="adi-radio-inline"
                @click="profileDraft.gender = g.value"
              >
                <span class="radio-ring" :class="{ checked: profileDraft.gender === g.value }">
                  <span class="radio-dot"></span>
                </span>
                <span>{{ g.label }}</span>
              </label>
            </div>

            <!-- Stacked Full-Width Adidas Modal Actions -->
            <div class="modal-actions-stack">
              <button type="submit" class="btn-modal-primary" :disabled="savingProfile">
                <span>
                  {{
                    savingProfile
                      ? (isEn ? 'UPDATING DETAILS...' : 'ĐANG CẬP NHẬT...')
                      : (isEn ? 'UPDATE DETAILS' : 'CẬP NHẬT THÔNG TIN')
                  }}
                </span>
                <span class="arrow">→</span>
              </button>
              <button type="button" class="btn-modal-cancel" @click="editProfileModalOpen = false">
                <span>{{ isEn ? 'CANCEL' : 'HỦY' }}</span>
                <span class="arrow">→</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <!-- ================================================================ -->
    <!-- EDIT EMAIL MODAL (Matching Sharp Adidas Modal Style)             -->
    <!-- ================================================================ -->
    <transition name="adi-modal-fade">
      <div v-if="editEmailModalOpen" class="adi-modal-scrim" @click.self="editEmailModalOpen = false">
        <div class="adi-sharp-modal compact-modal" role="dialog" aria-modal="true">
          <div class="modal-top-bar">
            <h2 class="modal-heading">{{ isEn ? 'EDIT YOUR EMAIL' : 'CHỈNH SỬA ĐỊA CHỈ EMAIL' }}</h2>
            <button type="button" class="modal-x-btn" @click="editEmailModalOpen = false" aria-label="Close">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>

          <form class="adi-modal-form" @submit.prevent="saveEmailChange">
            <div class="adi-float-box" :class="{ valid: /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailDraft.trim()) }">
              <label class="float-lbl">Email*</label>
              <input v-model="emailDraft" type="email" class="float-input" required />
              <span v-if="/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailDraft.trim())" class="valid-tick">✓</span>
            </div>

            <div class="modal-actions-stack">
              <button type="submit" class="btn-modal-primary" :disabled="savingProfile">
                <span>{{ savingProfile ? (isEn ? 'SAVING...' : 'ĐANG LƯU...') : (isEn ? 'SAVE EMAIL' : 'CẬP NHẬT EMAIL') }}</span>
                <span class="arrow">→</span>
              </button>
              <button type="button" class="btn-modal-cancel" @click="editEmailModalOpen = false">
                <span>{{ isEn ? 'CANCEL' : 'HỦY' }}</span>
                <span class="arrow">→</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <!-- ================================================================ -->
    <!-- ADD / EDIT ADDRESS MODAL (Exact Adidas Sharp Geometry + VN API)  -->
    <!-- ================================================================ -->
    <transition name="adi-modal-fade">
      <div v-if="addressModalOpen" class="adi-modal-scrim" @click.self="addressModalOpen = false">
        <div class="adi-sharp-modal" role="dialog" aria-modal="true">
          <div class="modal-top-bar">
            <h2 class="modal-heading">
              {{
                editingAddressId
                  ? (isEn ? 'EDIT ADDRESS' : 'CHỈNH SỬA ĐỊA CHỈ')
                  : (isEn ? 'ADD NEW ADDRESS' : 'THÊM ĐỊA CHỈ MỚI')
              }}
            </h2>
            <button type="button" class="modal-x-btn" @click="addressModalOpen = false" aria-label="Close">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>

          <form class="adi-modal-form" @submit.prevent="saveAddressEntry">
            <div class="modal-row-2">
              <div class="adi-float-box" :class="{ valid: addrForm.fullName.trim().length > 0 }">
                <label class="float-lbl">{{ isEn ? 'Recipient Full Name*' : 'Họ và tên người nhận*' }}</label>
                <input v-model="addrForm.fullName" type="text" class="float-input" required />
                <span v-if="addrForm.fullName.trim()" class="valid-tick">✓</span>
              </div>

              <div class="adi-float-box" :class="{ valid: /^[0-9]{9,11}$/.test(addrForm.phone.trim()) }">
                <label class="float-lbl">{{ isEn ? 'Phone Number*' : 'Số điện thoại*' }}</label>
                <input v-model="addrForm.phone" type="text" class="float-input" required />
                <span v-if="/^[0-9]{9,11}$/.test(addrForm.phone.trim())" class="valid-tick">✓</span>
              </div>
            </div>

            <!-- Vietnam Province / District / Ward Cascade -->
            <div class="modal-section-lbl">{{ isEn ? 'REGION (PROVINCE / DISTRICT / WARD)' : 'KHU VỰC (TỈNH / HUYỆN / XÃ)' }}</div>
            <div class="vn-cascade-grid-3">
              <div class="cascade-field">
                <label>{{ isEn ? 'Province / City*' : 'Tỉnh / Thành phố*' }}</label>
                <QuickSelect
                  v-model="addrForm.provinceCode"
                  :options="provincesList"
                  value-key="code"
                  label-key="name"
                  :placeholder="isEn ? 'Select Province' : 'Chọn Tỉnh / TP'"
                  :search-placeholder="isEn ? 'Search province...' : 'Tìm Tỉnh / TP...'"
                  clearable
                  :loading="loadingProvinces"
                  @change="onAddrProvinceChange"
                />
              </div>

              <div class="cascade-field">
                <label>{{ isEn ? 'District*' : 'Quận / Huyện*' }}</label>
                <QuickSelect
                  v-model="addrForm.districtCode"
                  :options="districtsList"
                  value-key="code"
                  label-key="name"
                  :placeholder="isEn ? 'Select District' : 'Chọn Quận / Huyện'"
                  :search-placeholder="isEn ? 'Search district...' : 'Tìm Quận / Huyện...'"
                  clearable
                  :disabled="!addrForm.provinceCode"
                  :loading="loadingDistricts"
                  @change="onAddrDistrictChange"
                />
              </div>

              <div class="cascade-field">
                <label>{{ isEn ? 'Ward*' : 'Phường / Xã*' }}</label>
                <QuickSelect
                  v-model="addrForm.wardCode"
                  :options="wardsList"
                  value-key="code"
                  label-key="name"
                  :placeholder="isEn ? 'Select Ward' : 'Chọn Phường / Xã'"
                  :search-placeholder="isEn ? 'Search ward...' : 'Tìm Phường / Xã...'"
                  clearable
                  :disabled="!addrForm.districtCode"
                  :loading="loadingWards"
                  @change="onAddrWardChange"
                />
              </div>
            </div>

            <div class="adi-float-box" :class="{ valid: addrForm.streetAddress.trim().length > 0 }">
              <label class="float-lbl">{{ isEn ? 'Street Address, Apartment*' : 'Số nhà, tên đường, tòa nhà*' }}</label>
              <input v-model="addrForm.streetAddress" type="text" class="float-input" required />
              <span v-if="addrForm.streetAddress.trim()" class="valid-tick">✓</span>
            </div>

            <label class="pref-check-row">
              <input type="checkbox" v-model="addrForm.isDefault" />
              <span>{{ isEn ? 'Set as my default delivery address' : 'Đặt làm địa chỉ giao hàng mặc định' }}</span>
            </label>

            <div class="modal-actions-stack">
              <button type="submit" class="btn-modal-primary" :disabled="savingAddress">
                <span>
                  {{
                    savingAddress
                      ? (isEn ? 'SAVING ADDRESS...' : 'ĐANG LƯU ĐỊA CHỈ...')
                      : (isEn ? 'SAVE ADDRESS' : 'LƯU ĐỊA CHỈ')
                  }}
                </span>
                <span class="arrow">→</span>
              </button>
              <button type="button" class="btn-modal-cancel" @click="addressModalOpen = false">
                <span>{{ isEn ? 'CANCEL' : 'HỦY' }}</span>
                <span class="arrow">→</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <!-- Auth Modal if user visits while logged out -->
    <ClientAuthModal v-model="authModalVisible" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useClientAuthStore } from '@/stores/clientAuth'
import { vnAddressService } from '@/services/vnAddressService'
import { useStoreI18n } from '@/composables/useStoreI18n'
import ClientAuthModal from '@/components/store/ClientAuthModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useClientAuthStore()
const { locale, isEn, lp, switchLocale } = useStoreI18n()

const authModalVisible = ref(false)
const prefDrops = ref(true)

const activeSection = computed(() => {
  if (route.name === 'StoreAccountAddressBook') return 'address-book'
  if (route.name === 'StoreAccountPreferences') return 'preferences'
  return 'profile'
})

/* ==================== EXTRA PROFILE METADATA (DOB & GENDER) ==================== */
const extraKey = computed(() => `adha_profile_meta_${authStore.user?.id || authStore.user?.username || 'guest'}`)
const extraProfile = reactive({
  dob: '',
  gender: ''
})

const loadExtraProfile = () => {
  try {
    const raw = localStorage.getItem(extraKey.value)
    if (raw) {
      const parsed = JSON.parse(raw)
      extraProfile.dob = parsed.dob || ''
      extraProfile.gender = parsed.gender || ''
    } else {
      extraProfile.dob = ''
      extraProfile.gender = ''
    }
  } catch {
    extraProfile.dob = ''
    extraProfile.gender = ''
  }
}

const genderOptions = computed(() =>
  isEn.value
    ? [
        { value: 'MALE', label: 'Male' },
        { value: 'FEMALE', label: 'Female' },
        { value: 'OTHER', label: 'Other' }
      ]
    : [
        { value: 'MALE', label: 'Nam' },
        { value: 'FEMALE', label: 'Nữ' },
        { value: 'OTHER', label: 'Khác' }
      ]
)

const genderDisplayLabel = computed(() => {
  if (extraProfile.gender === 'MALE') return isEn.value ? 'MALE' : 'NAM'
  if (extraProfile.gender === 'FEMALE') return isEn.value ? 'FEMALE' : 'NỮ'
  if (extraProfile.gender === 'OTHER') return isEn.value ? 'OTHER' : 'KHÁC'
  return isEn.value ? 'GENDER' : 'GIỚI TÍNH'
})

/* ==================== EDIT PROFILE MODAL ==================== */
const editProfileModalOpen = ref(false)
const editEmailModalOpen = ref(false)
const savingProfile = ref(false)
const emailDraft = ref('')

const profileDraft = reactive({
  firstName: '',
  lastName: '',
  phone: '',
  dobDay: '',
  dobMonth: '',
  dobYear: '',
  gender: ''
})

const openEditProfileModal = () => {
  const full = String(authStore.user?.fullName || '').trim()
  const parts = full.split(/\s+/).filter(Boolean)
  if (parts.length > 1) {
    profileDraft.lastName = parts[0]
    profileDraft.firstName = parts.slice(1).join(' ')
  } else {
    profileDraft.firstName = full || authStore.user?.username || ''
    profileDraft.lastName = ''
  }
  profileDraft.phone = authStore.user?.phone || ''

  const dobParts = (extraProfile.dob || '').split('-')
  if (dobParts.length === 3) {
    profileDraft.dobYear = dobParts[0]
    profileDraft.dobMonth = dobParts[1]
    profileDraft.dobDay = dobParts[2]
  } else {
    profileDraft.dobYear = ''
    profileDraft.dobMonth = ''
    profileDraft.dobDay = ''
  }
  profileDraft.gender = extraProfile.gender || ''
  editProfileModalOpen.value = true
}

const saveProfileDetails = async () => {
  const combinedName = [profileDraft.lastName.trim(), profileDraft.firstName.trim()].filter(Boolean).join(' ')
  if (!combinedName) {
    ElMessage.warning(isEn.value ? 'Please enter your name' : 'Vui lòng nhập họ tên')
    return
  }
  if (profileDraft.phone.trim() && !/^[0-9]{9,11}$/.test(profileDraft.phone.trim())) {
    ElMessage.warning(isEn.value ? 'Phone must be 9–11 digits' : 'Số điện thoại phải gồm 9–11 chữ số')
    return
  }

  savingProfile.value = true
  try {
    await authStore.updateProfile({
      fullName: combinedName,
      phone: profileDraft.phone.trim() || authStore.user?.phone || null,
      email: authStore.user?.email || null,
      address: authStore.user?.address || null
    })

    const y = profileDraft.dobYear.trim()
    const m = profileDraft.dobMonth.trim().padStart(2, '0')
    const d = profileDraft.dobDay.trim().padStart(2, '0')
    extraProfile.dob = y && profileDraft.dobMonth.trim() && profileDraft.dobDay.trim() ? `${y}-${m}-${d}` : ''
    extraProfile.gender = profileDraft.gender || ''
    localStorage.setItem(extraKey.value, JSON.stringify({ dob: extraProfile.dob, gender: extraProfile.gender }))

    ElMessage.success(isEn.value ? 'Profile updated!' : 'Đã cập nhật thông tin cá nhân!')
    editProfileModalOpen.value = false
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || (isEn.value ? 'Update failed' : 'Không thể cập nhật thông tin'))
  } finally {
    savingProfile.value = false
  }
}

const openEditEmailModal = () => {
  emailDraft.value = authStore.user?.email || ''
  editEmailModalOpen.value = true
}

const saveEmailChange = async () => {
  const cleanEmail = emailDraft.value.trim()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleanEmail)) {
    ElMessage.warning(isEn.value ? 'Invalid email address' : 'Địa chỉ email không đúng định dạng')
    return
  }
  savingProfile.value = true
  try {
    await authStore.updateProfile({
      fullName: authStore.user?.fullName || authStore.user?.username,
      phone: authStore.user?.phone || null,
      email: cleanEmail,
      address: authStore.user?.address || null
    })
    ElMessage.success(isEn.value ? 'Email updated!' : 'Đã cập nhật địa chỉ Email!')
    editEmailModalOpen.value = false
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || (isEn.value ? 'Failed to update email' : 'Không thể cập nhật email'))
  } finally {
    savingProfile.value = false
  }
}

/* ==================== ADDRESS BOOK (SYNCED WITH BACKEND USER PROFILE) ==================== */
const addrStorageKey = computed(() => `adha_address_book_${authStore.user?.id || authStore.user?.username || 'guest'}`)
const addresses = ref([])
const addressModalOpen = ref(false)
const editingAddressId = ref(null)
const savingAddress = ref(false)

const provincesList = ref([])
const districtsList = ref([])
const wardsList = ref([])
const loadingProvinces = ref(false)
const loadingDistricts = ref(false)
const loadingWards = ref(false)

const addrForm = reactive({
  fullName: '',
  phone: '',
  provinceCode: null,
  districtCode: null,
  wardCode: null,
  provinceName: '',
  districtName: '',
  wardName: '',
  streetAddress: '',
  isDefault: true
})

const defaultAddressText = computed(() => {
  const def = addresses.value.find((a) => a.isDefault) || addresses.value[0]
  return def?.fullAddress || authStore.user?.address || ''
})

const persistAddresses = () => {
  try {
    localStorage.setItem(addrStorageKey.value, JSON.stringify(addresses.value))
  } catch {
    // ignore
  }
}

const loadAddresses = () => {
  try {
    const raw = localStorage.getItem(addrStorageKey.value)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        addresses.value = parsed
        return
      }
    }
  } catch {
    // ignore
  }
  // Seed initial default address from backend user.address if present
  if (authStore.user?.address) {
    addresses.value = [
      {
        id: 'primary-1',
        fullName: authStore.user.fullName || authStore.user.username,
        phone: authStore.user.phone || '',
        streetAddress: authStore.user.address,
        wardName: '',
        districtName: '',
        provinceName: '',
        fullAddress: authStore.user.address,
        isDefault: true
      }
    ]
    persistAddresses()
  } else {
    addresses.value = []
  }
}

const ensureProvincesLoaded = async () => {
  if (provincesList.value.length > 0) return
  loadingProvinces.value = true
  try {
    provincesList.value = await vnAddressService.getProvinces()
  } catch (err) {
    console.error(err)
  } finally {
    loadingProvinces.value = false
  }
}

const onAddrProvinceChange = async (val) => {
  addrForm.districtCode = null
  addrForm.wardCode = null
  addrForm.districtName = ''
  addrForm.wardName = ''
  districtsList.value = []
  wardsList.value = []
  const found = provincesList.value.find((p) => p.code === val)
  addrForm.provinceName = found ? found.name : ''
  if (val) {
    loadingDistricts.value = true
    try {
      districtsList.value = await vnAddressService.getDistricts(val)
    } finally {
      loadingDistricts.value = false
    }
  }
}

const onAddrDistrictChange = async (val) => {
  addrForm.wardCode = null
  addrForm.wardName = ''
  wardsList.value = []
  const found = districtsList.value.find((d) => d.code === val)
  addrForm.districtName = found ? found.name : ''
  if (val) {
    loadingWards.value = true
    try {
      wardsList.value = await vnAddressService.getWards(val)
    } finally {
      loadingWards.value = false
    }
  }
}

const onAddrWardChange = (val) => {
  const found = wardsList.value.find((w) => w.code === val)
  addrForm.wardName = found ? found.name : ''
}

const openAddressModal = async (existing) => {
  await ensureProvincesLoaded()
  if (existing) {
    editingAddressId.value = existing.id
    addrForm.fullName = existing.fullName || authStore.user?.fullName || ''
    addrForm.phone = existing.phone || authStore.user?.phone || ''
    addrForm.streetAddress = existing.streetAddress || existing.fullAddress || ''
    addrForm.provinceCode = existing.provinceCode || null
    addrForm.districtCode = existing.districtCode || null
    addrForm.wardCode = existing.wardCode || null
    addrForm.provinceName = existing.provinceName || ''
    addrForm.districtName = existing.districtName || ''
    addrForm.wardName = existing.wardName || ''
    addrForm.isDefault = Boolean(existing.isDefault)
  } else {
    editingAddressId.value = null
    addrForm.fullName = authStore.user?.fullName || ''
    addrForm.phone = authStore.user?.phone || ''
    addrForm.streetAddress = ''
    addrForm.provinceCode = null
    addrForm.districtCode = null
    addrForm.wardCode = null
    addrForm.provinceName = ''
    addrForm.districtName = ''
    addrForm.wardName = ''
    addrForm.isDefault = addresses.value.length === 0
  }
  addressModalOpen.value = true
}

const syncDefaultToBackend = async (defaultAddr) => {
  if (!defaultAddr || !authStore.isAuthenticated) return
  try {
    await authStore.updateProfile({
      fullName: authStore.user?.fullName || defaultAddr.fullName,
      phone: authStore.user?.phone || defaultAddr.phone || null,
      email: authStore.user?.email || null,
      address: defaultAddr.fullAddress
    })
  } catch (err) {
    console.warn('Sync default address failed:', err)
  }
}

const saveAddressEntry = async () => {
  if (!addrForm.fullName.trim() || !addrForm.streetAddress.trim()) {
    ElMessage.warning(isEn.value ? 'Please fill required fields (*)' : 'Vui lòng điền đầy đủ các trường bắt buộc (*)')
    return
  }
  if (!/^[0-9]{9,11}$/.test(addrForm.phone.trim())) {
    ElMessage.warning(isEn.value ? 'Phone number must be 9–11 digits' : 'Số điện thoại không đúng định dạng')
    return
  }

  savingAddress.value = true
  try {
    const fullAddress = [addrForm.streetAddress.trim(), addrForm.wardName, addrForm.districtName, addrForm.provinceName]
      .filter(Boolean)
      .join(', ')

    const entry = {
      id: editingAddressId.value || `addr-${Date.now()}`,
      fullName: addrForm.fullName.trim(),
      phone: addrForm.phone.trim(),
      streetAddress: addrForm.streetAddress.trim(),
      provinceCode: addrForm.provinceCode,
      districtCode: addrForm.districtCode,
      wardCode: addrForm.wardCode,
      provinceName: addrForm.provinceName,
      districtName: addrForm.districtName,
      wardName: addrForm.wardName,
      fullAddress,
      isDefault: addrForm.isDefault || addresses.value.length === 0
    }

    if (entry.isDefault) {
      addresses.value.forEach((a) => {
        a.isDefault = false
      })
    }

    if (editingAddressId.value) {
      const idx = addresses.value.findIndex((a) => a.id === editingAddressId.value)
      if (idx !== -1) addresses.value[idx] = entry
    } else {
      addresses.value.unshift(entry)
    }

    persistAddresses()
    if (entry.isDefault) {
      await syncDefaultToBackend(entry)
    }

    ElMessage.success(isEn.value ? 'Address saved!' : 'Đã lưu địa chỉ thành công!')
    addressModalOpen.value = false
  } finally {
    savingAddress.value = false
  }
}

const setDefaultAddress = async (addr) => {
  addresses.value.forEach((a) => {
    a.isDefault = a.id === addr.id
  })
  persistAddresses()
  await syncDefaultToBackend(addr)
  ElMessage.success(isEn.value ? 'Default delivery address updated!' : 'Đã cập nhật địa chỉ giao hàng mặc định!')
}

const removeAddress = (addr) => {
  ElMessageBox.confirm(
    isEn.value ? 'Remove this address from your Address Book?' : 'Bạn có chắc muốn xóa địa chỉ này khỏi Sổ Địa Chỉ?',
    isEn.value ? 'Confirm' : 'Xác nhận',
    {
      confirmButtonText: isEn.value ? 'Remove' : 'Xóa',
      cancelButtonText: isEn.value ? 'Cancel' : 'Hủy',
      type: 'warning'
    }
  )
    .then(async () => {
      addresses.value = addresses.value.filter((a) => a.id !== addr.id)
      if (addr.isDefault && addresses.value.length > 0) {
        addresses.value[0].isDefault = true
        await syncDefaultToBackend(addresses.value[0])
      }
      persistAddresses()
      ElMessage.success(isEn.value ? 'Address removed' : 'Đã xóa địa chỉ')
    })
    .catch(() => {})
}

const handleLogout = async () => {
  await authStore.logout()
  ElMessage.success(isEn.value ? 'Signed out successfully' : 'Đã đăng xuất tài khoản')
  router.push(lp(''))
}

onMounted(async () => {
  if (authStore.isAuthenticated) {
    await authStore.fetchCurrentUser()
    loadExtraProfile()
    loadAddresses()
  }
})

watch(
  () => authStore.isAuthenticated,
  async (loggedIn) => {
    if (loggedIn) {
      await authStore.fetchCurrentUser()
      loadExtraProfile()
      loadAddresses()
    }
  }
)
</script>

<style scoped lang="scss">
/* Exact Live 2026 Adidas #f5f5f5 Account Canvas */
.adi-account-page {
  background: #f5f5f5;
  color: #000000;
  min-height: calc(100vh - 121px);
  padding: 56px 0 88px;
}

.adi-account-container {
  max-width: 1216px;
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

/* Guest Prompt */
.adi-account-guest-card {
  max-width: 580px;
  background: #ffffff;
  padding: 44px 48px;

  .guest-wordmark {
    display: inline-block;
    font-family: 'Barlow Condensed', sans-serif;
    font-size: 28px;
    font-weight: 900;
    font-style: italic;
    margin-bottom: 10px;
  }

  .guest-heading {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 36px;
    font-weight: 800;
    letter-spacing: 0.04em;
    margin: 0 0 12px;
    line-height: 1.05;
  }

  .guest-sub {
    font-size: 15px;
    line-height: 1.55;
    color: #333333;
    margin: 0 0 28px;
  }

  .btn-adi-black-offset {
    position: relative;
    height: 50px;
    padding: 0 26px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    border-radius: 0;
    font-family: inherit;
    font-size: 13.5px;
    font-weight: 800;
    letter-spacing: 0.1em;
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
  }
}

/* Exact Adidas 2-Column Layout: 210px Left Sidebar + Wide White Right Card */
.adi-account-grid {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  gap: 88px;
  align-items: start;

  @media (max-width: 1080px) {
    grid-template-columns: 196px minmax(0, 1fr);
    gap: 40px;
  }

  @media (max-width: 820px) {
    grid-template-columns: 1fr;
    gap: 28px;
  }
}

.adi-account-sidebar {
  .sidebar-kicker {
    font-size: 15.5px;
    font-weight: 800;
    line-height: 1.35;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: #000000;
    margin: 0 0 18px;
  }

  .sidebar-menu-card {
    background: #ffffff;
    display: flex;
    flex-direction: column;
    padding: 6px 0;
  }

  .sidebar-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    min-height: 56px;
    padding: 15px 18px;
    background: transparent;
    border: none;
    text-decoration: none;
    font-family: inherit;
    font-size: 15.5px;
    line-height: 1.35;
    font-weight: 400;
    color: #000000;
    text-align: left;
    cursor: pointer;
    box-sizing: border-box;
    transition: background 0.14s ease;

    &:hover {
      background: #f5f5f5;
    }

    &.active {
      font-weight: 700;
    }

    .chev {
      font-size: 18px;
      line-height: 1;
      color: #000000;
    }
  }
}

/* Right Pure White Content Card */
.adi-account-card {
  background: #ffffff;
  padding: 44px 48px 56px;

  @media (max-width: 640px) {
    padding: 28px 20px 36px;
  }

  .pane-title {
    font-family: 'Plus Jakarta Sans', 'Inter', sans-serif;
    font-size: 30px;
    font-style: normal;
    font-weight: 800;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: #000000;
    margin: 0 0 10px;
    line-height: 1.15;
  }

  .pane-subtitle {
    font-size: 15.5px;
    line-height: 1.55;
    color: #000000;
    margin: 0 0 36px;
  }
}

.detail-group {
  margin-bottom: 42px;

  &:last-child {
    margin-bottom: 0;
  }

  .group-heading {
    font-family: 'Plus Jakarta Sans', 'Inter', sans-serif;
    font-size: 22px;
    font-weight: 800;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: #000000;
    margin: 0 0 16px;
    line-height: 1.25;
  }

  .detail-lines {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 16px;
  }

  .detail-line {
    font-size: 15.5px;
    line-height: 1.5;
    color: #000000;

    &.uppercase-val {
      letter-spacing: 0.02em;
    }

    &.password-dots {
      letter-spacing: 0.14em;
      font-weight: 700;
    }
  }

  .sub-detail-block {
    margin-bottom: 28px;

    &:last-child {
      margin-bottom: 0;
    }

    .sub-label {
      font-size: 15px;
      font-weight: 800;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      color: #000000;
      margin-bottom: 10px;
    }

    .detail-line {
      margin-bottom: 12px;
    }
  }

  .manage-note {
    font-size: 14.5px;
    color: #555555;
    margin: 0 0 16px;
  }
}

/* Signature Adidas Thick-Underlined "CHỈNH SỬA" Button */
.btn-adi-edit-underline {
  display: inline-block;
  background: transparent;
  border: none;
  border-bottom: 2px solid #000000;
  padding: 2px 0 3px;
  font-family: inherit;
  font-size: 14.5px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  text-decoration: none;
  color: #000000;
  cursor: pointer;
  transition: background 0.14s ease, color 0.14s ease;

  &:hover {
    background: #000000;
    color: #ffffff;
  }
}

.btn-adi-outline-sharp {
  height: 48px;
  padding: 0 22px;
  background: #ffffff;
  color: #000000;
  border: 1px solid #000000;
  border-radius: 0;
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 800;
  letter-spacing: 0.1em;
  display: inline-flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;

  &:hover {
    background: #000000;
    color: #ffffff;
  }
}

/* ============ EXACT ADIDAS SỔ ĐỊA CHỈ 2-COLUMN CARD GRID ============ */
.adi-address-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;

  @media (max-width: 680px) {
    grid-template-columns: 1fr;
  }
}

.adi-address-tile {
  min-height: 204px;
  padding: 22px 24px;
  background: #ffffff;
  border: 1px solid #d9dbdd;
  border-radius: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  text-align: left;
  font-family: inherit;

  &.add-tile {
    border-color: #767677;
    cursor: pointer;
    transition: border-color 0.14s ease, background 0.14s ease;

    &:hover {
      border-color: #000000;
      background: #fafafa;
    }

    .add-tile-title {
      font-size: 16px;
      font-weight: 700;
      color: #000000;
    }

    .add-tile-plus {
      font-size: 32px;
      font-weight: 300;
      line-height: 1;
      color: #000000;
    }
  }

  &.saved-tile.is-default {
    border-color: #000000;
  }

  .addr-head-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 10px;
    margin-bottom: 10px;

    .addr-name {
      font-size: 15.5px;
      font-weight: 800;
      text-transform: uppercase;
      color: #000000;
    }

    .addr-default-pill {
      font-size: 10.5px;
      font-weight: 800;
      letter-spacing: 0.08em;
      background: #eceff1;
      color: #000000;
      padding: 3px 8px;
    }
  }

  .addr-lines {
    p {
      margin: 0 0 4px;
      font-size: 14.5px;
      line-height: 1.45;
      color: #000000;
    }

    .addr-phone {
      margin-top: 8px;
      color: #555555;
    }
  }

  .addr-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-top: 20px;

    .addr-link-btn {
      background: none;
      border: none;
      border-bottom: 1.5px solid #000000;
      padding: 1px 0;
      font-family: inherit;
      font-size: 12.5px;
      font-weight: 800;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      color: #000000;
      cursor: pointer;

      &:hover {
        background: #000000;
        color: #ffffff;
      }
    }
  }
}

/* Preferences */
.pref-lang-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  .pref-lang-btn {
    height: 44px;
    padding: 0 22px;
    background: #ffffff;
    color: #000000;
    border: 1px solid #767677;
    border-radius: 0;
    font-family: inherit;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;

    &.active {
      background: #000000;
      color: #ffffff;
      border-color: #000000;
    }
  }
}

.pref-check-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 14.5px;
  line-height: 1.5;
  color: #000000;
  cursor: pointer;

  input {
    width: 18px;
    height: 18px;
    margin-top: 2px;
    accent-color: #000000;
    flex-shrink: 0;
  }
}

/* Bottom Help Strip ("BẠN CẦN TRỢ GIÚP?") */
.adi-account-help-strip {
  margin-top: 64px;
  padding-top: 36px;
  border-top: 1px solid #d9dbdd;

  .help-heading {
    font-size: 18px;
    font-weight: 800;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    margin: 0 0 18px;
  }

  .help-links-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 320px));
    gap: 12px 40px;

    @media (max-width: 600px) {
      grid-template-columns: 1fr;
    }

    a {
      font-size: 14.5px;
      color: #000000;
      text-decoration: underline;
      text-underline-offset: 3px;

      &:hover {
        background: #000000;
        color: #ffffff;
        text-decoration: none;
      }
    }
  }
}

/* ================================================================ */
/* EXACT LIVE ADIDAS "CHỈNH SỬA THÔNG TIN CỦA BẠN" MODAL            */
/* ================================================================ */
.adi-modal-scrim {
  position: fixed;
  inset: 0;
  z-index: 1500;
  background: rgba(0, 0, 0, 0.62);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.adi-sharp-modal {
  width: 780px;
  max-width: 100%;
  max-height: 92vh;
  overflow-y: auto;
  background: #ffffff;
  border-radius: 0;
  padding: 36px 40px 38px;
  box-sizing: border-box;

  &.compact-modal {
    width: 560px;
  }

  @media (max-width: 640px) {
    padding: 24px 20px;
  }

  .modal-top-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    .modal-heading {
      font-family: 'Plus Jakarta Sans', 'Inter', sans-serif;
      font-size: 26px;
      font-weight: 800;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      color: #000000;
      margin: 0;

      @media (max-width: 640px) {
        font-size: 20px;
      }
    }

    .modal-x-btn {
      width: 34px;
      height: 34px;
      background: transparent;
      border: none;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: #000000;

      svg {
        width: 22px;
        height: 22px;
      }
    }
  }
}

.adi-modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .modal-row-2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;

    @media (max-width: 600px) {
      grid-template-columns: 1fr;
    }
  }

  .adi-float-box {
    position: relative;
    height: 56px;
    border: 1px solid #767677;
    background: #ffffff;
    padding: 8px 36px 6px 14px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    box-sizing: border-box;

    &:focus-within {
      border-color: #000000;
      border-bottom-width: 2px;
    }

    .float-lbl {
      font-size: 11.5px;
      color: #767677;
      line-height: 1.1;
      margin-bottom: 2px;
    }

    .float-input {
      border: none;
      outline: none;
      background: transparent;
      font-family: inherit;
      font-size: 15px;
      color: #000000;
      width: 100%;
      padding: 0;
    }

    .valid-tick {
      position: absolute;
      right: 14px;
      top: 50%;
      transform: translateY(-50%);
      font-size: 15px;
      color: #000000;
    }
  }

  .modal-section-lbl {
    font-size: 15px;
    font-weight: 800;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: #000000;
    margin-top: 8px;
  }

  .dob-grid-3 {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }

  .gender-radio-row {
    display: flex;
    align-items: center;
    gap: 24px;
    flex-wrap: wrap;
    margin-bottom: 8px;

    .adi-radio-inline {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      font-size: 15px;
      color: #000000;
      cursor: pointer;

      .radio-ring {
        width: 22px;
        height: 22px;
        border-radius: 50%;
        border: 1.5px solid #767677;
        display: inline-flex;
        align-items: center;
        justify-content: center;

        .radio-dot {
          width: 11px;
          height: 11px;
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

  .vn-cascade-grid-3 {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;

    @media (max-width: 680px) {
      grid-template-columns: 1fr;
    }

    .cascade-field {
      display: flex;
      flex-direction: column;
      gap: 5px;

      label {
        font-size: 11.5px;
        font-weight: 700;
        color: #555555;
      }

      :deep(.el-input__wrapper) {
        border-radius: 0 !important;
        height: 46px;
        box-shadow: 0 0 0 1px #767677 inset !important;
      }
    }
  }

  .modal-actions-stack {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-top: 12px;

    .btn-modal-primary {
      position: relative;
      width: 100%;
      height: 50px;
      padding: 0 20px;
      background: #000000;
      color: #ffffff;
      border: 1px solid #000000;
      border-radius: 0;
      font-family: inherit;
      font-size: 14px;
      font-weight: 800;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      display: flex;
      justify-content: space-between;
      align-items: center;
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
        font-size: 19px;
      }

      &:hover:not(:disabled) {
        color: #cccccc;
      }
    }

    .btn-modal-cancel {
      width: 100%;
      height: 50px;
      padding: 0 20px;
      background: #ffffff;
      color: #000000;
      border: 1px solid #000000;
      border-radius: 0;
      font-family: inherit;
      font-size: 14px;
      font-weight: 800;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;

      .arrow {
        font-size: 19px;
      }

      &:hover {
        background: #eceff1;
      }
    }
  }
}

.adi-modal-fade-enter-active,
.adi-modal-fade-leave-active {
  transition: opacity 0.18s ease;
}
.adi-modal-fade-enter-from,
.adi-modal-fade-leave-to {
  opacity: 0;
}
</style>
