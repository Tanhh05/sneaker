<template>
  <div class="store-layout" @mouseleave="closeMegaMenu">
    <!-- 1. TOP BLACK USP PROMO BAR (40px) -->
    <div class="adi-promo-bar" @click="promoSheetOpen = !promoSheetOpen">
      <div class="adi-header-container promo-inner">
        <span class="promo-message">{{ promoMessages[activePromoIdx] }}</span>
        <svg
          class="promo-chevron"
          :class="{ rotated: promoSheetOpen }"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
      </div>
    </div>

    <!-- Clickable Slide-Down Policy Drawer -->
    <div v-if="promoSheetOpen" class="adi-promo-sheet">
      <div class="adi-container promo-sheet-inner">
        <div class="promo-sheet-col">
          <h4>{{ t('promo.sheet1Title') }}</h4>
          <p>{{ t('promo.sheet1Desc') }}</p>
          <button type="button" class="sheet-link-btn" @click="showSizeGuide = true; promoSheetOpen = false">
            {{ t('promo.sheet1Cta') }}
          </button>
        </div>
        <div class="promo-sheet-col">
          <h4>{{ t('promo.sheet2Title') }}</h4>
          <p>{{ t('promo.sheet2Desc') }}</p>
          <router-link :to="lp('shop')" @click="promoSheetOpen = false">{{ t('promo.sheet2Cta') }}</router-link>
        </div>
        <div class="promo-sheet-col">
          <h4>{{ t('promo.sheet3Title') }}</h4>
          <p>{{ t('promo.sheet3Desc') }}</p>
          <router-link :to="lp('tracking')" @click="promoSheetOpen = false">{{ t('promo.sheet3Cta') }}</router-link>
        </div>
        <button type="button" class="promo-sheet-close" @click="promoSheetOpen = false" aria-label="Close">✕</button>
      </div>
    </div>

    <!-- 2. STICKY 81px WHITE HEADER -->
    <header class="adi-header" :class="{ scrolled: isScrolled }">
      <!-- Tier 1: Top-Right Utility Links + Exact Adidas Flag Language Switcher -->
      <div class="adi-utility-row">
        <div class="adi-header-container utility-inner">
          <router-link :to="lp('shop')" class="utility-link">{{ t('utility.storeFinder') }}</router-link>
          <button type="button" class="utility-link" @click="showSizeGuide = true">{{ t('utility.help') }}</button>
          <router-link :to="authStore.isAuthenticated ? lp('order-history') : lp('tracking')" class="utility-link">{{ t('utility.orderTracker') }}</router-link>
          <button
            v-if="!authStore.isAuthenticated"
            type="button"
            class="utility-link"
            @click="openAuthModal('register')"
          >
            {{ t('utility.becomeMember') }}
          </button>
          <button
            v-else
            type="button"
            class="utility-link"
            @click="openMemberDrawer"
          >
            {{ authStore.user?.fullName?.split(' ').slice(-1)[0] || authStore.user?.username }}
          </button>
          <router-link v-if="isStaffOrAdmin" to="/admin/dashboard" class="utility-link utility-admin">
            {{ t('utility.admin') }}
          </router-link>

          <!-- Exact Adidas Flag Language Selector + Connected White Bordered Popover -->
          <div ref="langWrapRef" class="adi-lang-wrap">
            <button
              type="button"
              class="adi-flag-trigger"
              :class="{ open: langMenuOpen }"
              @click.stop="toggleLangMenu"
              :title="locale === 'vi' ? 'Tiếng Việt (VN)' : 'English (US)'"
              aria-label="Language"
            >
              <!-- Vietnam Flag when locale === 'vi' -->
              <svg v-if="locale === 'vi'" viewBox="0 0 30 20" width="20" height="13" aria-hidden="true">
                <rect width="30" height="20" fill="#da251d" />
                <polygon
                  points="15,4 16.76,9.41 22.45,9.41 17.85,12.76 19.61,18.17 15,14.82 10.39,18.17 12.15,12.76 7.55,9.41 13.24,9.41"
                  fill="#ffff00"
                />
              </svg>
              <!-- United States Flag when locale === 'en' -->
              <svg v-else viewBox="0 0 30 20" width="20" height="13" aria-hidden="true">
                <rect width="30" height="20" fill="#ffffff" />
                <rect y="0" width="30" height="1.54" fill="#b22234" />
                <rect y="3.08" width="30" height="1.54" fill="#b22234" />
                <rect y="6.15" width="30" height="1.54" fill="#b22234" />
                <rect y="9.23" width="30" height="1.54" fill="#b22234" />
                <rect y="12.31" width="30" height="1.54" fill="#b22234" />
                <rect y="15.38" width="30" height="1.54" fill="#b22234" />
                <rect y="18.46" width="30" height="1.54" fill="#b22234" />
                <rect width="12.5" height="10.77" fill="#3c3b6e" />
                <circle cx="2.5" cy="2.2" r="0.65" fill="#fff" />
                <circle cx="5.2" cy="2.2" r="0.65" fill="#fff" />
                <circle cx="7.9" cy="2.2" r="0.65" fill="#fff" />
                <circle cx="10.4" cy="2.2" r="0.65" fill="#fff" />
                <circle cx="3.8" cy="4.4" r="0.65" fill="#fff" />
                <circle cx="6.5" cy="4.4" r="0.65" fill="#fff" />
                <circle cx="9.2" cy="4.4" r="0.65" fill="#fff" />
                <circle cx="2.5" cy="6.6" r="0.65" fill="#fff" />
                <circle cx="5.2" cy="6.6" r="0.65" fill="#fff" />
                <circle cx="7.9" cy="6.6" r="0.65" fill="#fff" />
                <circle cx="10.4" cy="6.6" r="0.65" fill="#fff" />
                <circle cx="3.8" cy="8.8" r="0.65" fill="#fff" />
                <circle cx="6.5" cy="8.8" r="0.65" fill="#fff" />
                <circle cx="9.2" cy="8.8" r="0.65" fill="#fff" />
              </svg>
            </button>

            <!-- Exact Adidas Bordered Language Popover -->
            <div v-if="langMenuOpen" class="adi-lang-popover" @click.stop>
              <div class="lang-pop-title">{{ t('lang.title') }}</div>

              <label class="lang-radio-row" @click="pendingLocale = 'en'">
                <span class="radio-circle" :class="{ checked: pendingLocale === 'en' }">
                  <span class="radio-inner"></span>
                </span>
                <span class="lang-label" :class="{ bold: pendingLocale === 'en' }">English</span>
                <svg class="opt-flag" viewBox="0 0 30 20" width="18" height="12" aria-hidden="true">
                  <rect width="30" height="20" fill="#ffffff" />
                  <rect y="0" width="30" height="2" fill="#b22234" />
                  <rect y="4" width="30" height="2" fill="#b22234" />
                  <rect y="8" width="30" height="2" fill="#b22234" />
                  <rect y="12" width="30" height="2" fill="#b22234" />
                  <rect y="16" width="30" height="2" fill="#b22234" />
                  <rect width="12.5" height="10.8" fill="#3c3b6e" />
                </svg>
              </label>

              <label class="lang-radio-row" @click="pendingLocale = 'vi'">
                <span class="radio-circle" :class="{ checked: pendingLocale === 'vi' }">
                  <span class="radio-inner"></span>
                </span>
                <span class="lang-label" :class="{ bold: pendingLocale === 'vi' }">Tiếng Việt</span>
                <svg class="opt-flag" viewBox="0 0 30 20" width="18" height="12" aria-hidden="true">
                  <rect width="30" height="20" fill="#da251d" />
                  <polygon
                    points="15,4 16.76,9.41 22.45,9.41 17.85,12.76 19.61,18.17 15,14.82 10.39,18.17 12.15,12.76 7.55,9.41 13.24,9.41"
                    fill="#ffff00"
                  />
                </svg>
              </label>

              <button type="button" class="btn-lang-save" @click="applyLanguage">
                <span>{{ t('lang.save') }}</span>
                <span class="save-arrow">→</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Tier 2: Main Navigation Bar -->
      <div class="adi-nav-row">
        <div class="adi-header-container nav-inner">
          <div class="header-left">
            <button
              type="button"
              class="mobile-icon-btn mobile-menu-btn"
              @click="mobileMenuOpen = true"
              aria-label="Open menu"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="3" y1="6" x2="21" y2="6"></line>
                <line x1="3" y1="12" x2="21" y2="12"></line>
                <line x1="3" y1="18" x2="21" y2="18"></line>
              </svg>
            </button>

            <router-link :to="lp('')" class="adi-logo" @click="closeMegaMenu" aria-label="ADHA Sneaker">
              <AdhaLogoMark :size="44" variant="dark" />
            </router-link>
          </div>

          <!-- Center: Dead-Center Main Nav -->
          <nav class="adi-main-nav" aria-label="Main navigation">
            <router-link
              :to="{ path: lp('shop'), query: { sort: 'price_asc' } }"
              class="adi-nav-link sale-link"
              :class="{ active: route.name === 'StoreCatalog' && route.query.sort === 'price_asc' }"
              @mouseenter="openMegaMenu('sale')"
              @click="closeMegaMenu"
            >
              {{ t('nav.sale') }}
            </router-link>
            <router-link
              :to="lp('shop')"
              class="adi-nav-link strong-link"
              :class="{ active: activeMegaMenu === 'men' }"
              @mouseenter="openMegaMenu('men')"
              @click="closeMegaMenu"
            >
              {{ t('nav.men') }}
            </router-link>
            <router-link
              :to="lp('shop')"
              class="adi-nav-link strong-link"
              :class="{ active: activeMegaMenu === 'women' }"
              @mouseenter="openMegaMenu('women')"
              @click="closeMegaMenu"
            >
              {{ t('nav.women') }}
            </router-link>
            <router-link
              :to="{ path: lp('shop'), query: { sort: 'newest' } }"
              class="adi-nav-link strong-link"
              :class="{ active: route.name === 'StoreCatalog' && route.query.sort === 'newest' }"
              @mouseenter="openMegaMenu('new')"
              @click="closeMegaMenu"
            >
              {{ t('nav.newArrivals') }}
            </router-link>
            <router-link
              :to="lp('shop')"
              class="adi-nav-link regular-link"
              :class="{ active: activeMegaMenu === 'sports' || Boolean(route.query.category) }"
              @mouseenter="openMegaMenu('sports')"
              @click="closeMegaMenu"
            >
              {{ t('nav.sports') }}
            </router-link>
            <router-link
              :to="lp('shop')"
              class="adi-nav-link regular-link"
              :class="{ active: activeMegaMenu === 'brands' || Boolean(route.query.brand) }"
              @mouseenter="openMegaMenu('brands')"
              @click="closeMegaMenu"
            >
              {{ t('nav.brands') }}
            </router-link>
            <router-link
              :to="{ path: lp('shop'), query: { sort: 'price_asc' } }"
              class="adi-nav-link regular-link"
              @mouseenter="closeMegaMenu"
            >
              {{ t('nav.outlet') }}
            </router-link>
          </nav>

          <!-- Right: #e9ecef 32px Search Box + Monochrome Icons -->
          <div class="adi-header-actions">
            <div class="adi-search-box desktop-search">
              <input
                type="text"
                v-model="searchKeyword"
                :placeholder="t('nav.searchPlaceholder')"
                @keyup.enter="handleSearch"
                class="adi-search-input"
              />
              <button type="button" class="adi-search-submit" @click="handleSearch" aria-label="Search">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <circle cx="11" cy="11" r="7.5"></circle>
                  <line x1="21" y1="21" x2="16.5" y2="16.5"></line>
                </svg>
              </button>
            </div>

            <button
              type="button"
              class="mobile-icon-btn mobile-search-trigger"
              @click="mobileSearchOpen = !mobileSearchOpen"
              aria-label="Search"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <circle cx="11" cy="11" r="7.5"></circle>
                <line x1="21" y1="21" x2="16.5" y2="16.5"></line>
              </svg>
            </button>

            <!-- Account Icon (Opens authentic live adidas 390px ADHA Club Drawer when logged in) -->
            <button
              v-if="authStore.isAuthenticated"
              type="button"
              class="adi-icon-action"
              @click="openMemberDrawer"
              :aria-label="t('account.profile')"
              :title="authStore.user?.fullName || authStore.user?.username"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
              <span v-if="accountNoticeCount > 0" class="adi-green-pill">
                {{ accountNoticeCount > 9 ? '9+' : accountNoticeCount }}
              </span>
            </button>
            <button
              v-else
              type="button"
              class="adi-icon-action"
              @click="openAuthModal('login')"
              aria-label="Sign in"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </button>

            <button
              type="button"
              class="adi-icon-action"
              @click="showWishlist = true"
              aria-label="Wishlist"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
              <span v-if="wishlistStore.count > 0" class="adi-badge">{{ wishlistStore.count }}</span>
            </button>

            <router-link :to="lp('cart')" class="adi-icon-action" aria-label="Shopping bag">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                <line x1="3" y1="6" x2="21" y2="6"></line>
                <path d="M16 10a4 4 0 0 1-8 0"></path>
              </svg>
              <span v-if="cartStore.totalItems > 0" class="adi-badge adi-badge-cart">
                {{ cartStore.totalItems }}
              </span>
            </router-link>
          </div>
        </div>
      </div>

      <!-- Full-Width Adidas Hover Mega-Menu -->
      <div
        v-if="activeMegaMenu"
        class="adi-mega-menu"
        @mouseenter="keepMegaMenu"
        @mouseleave="closeMegaMenu"
      >
        <div class="adi-container mega-grid">
          <div class="mega-col">
            <h4 class="mega-heading">{{ t('mega.featured') }}</h4>
            <ul class="mega-list">
              <li>
                <router-link :to="{ path: lp('shop'), query: { sort: 'price_asc' } }" class="mega-sale" @click="closeMegaMenu">
                  {{ t('mega.saleLink') }}
                </router-link>
              </li>
              <li>
                <router-link :to="{ path: lp('shop'), query: { sort: 'newest' } }" @click="closeMegaMenu">
                  {{ t('mega.newLink') }}
                </router-link>
              </li>
              <li>
                <router-link :to="lp('shop')" @click="closeMegaMenu">
                  {{ t('mega.topSellers') }}
                </router-link>
              </li>
              <li>
                <router-link :to="lp('shop')" @click="closeMegaMenu">
                  {{ t('mega.allShoes') }}
                </router-link>
              </li>
              <li>
                <a href="javascript:void(0)" @click="showSizeGuide = true; closeMegaMenu()">
                  {{ t('mega.sizeChart') }}
                </a>
              </li>
            </ul>
          </div>

          <div class="mega-col">
            <h4 class="mega-heading">{{ t('mega.brands') }}</h4>
            <ul class="mega-list">
              <li v-for="b in brands" :key="b.id">
                <router-link :to="{ path: lp('shop'), query: { brand: b.id } }" @click="closeMegaMenu">
                  {{ b.brandName }}
                </router-link>
              </li>
            </ul>
          </div>

          <div class="mega-col">
            <h4 class="mega-heading">{{ t('mega.sports') }}</h4>
            <ul class="mega-list">
              <li v-for="c in categories" :key="c.id">
                <router-link :to="{ path: lp('shop'), query: { category: c.id } }" @click="closeMegaMenu">
                  {{ c.categoryName }}
                </router-link>
              </li>
              <li><router-link :to="lp('tracking')" @click="closeMegaMenu">{{ t('mega.orderTracker') }}</router-link></li>
              <li><router-link :to="lp('my-orders')" @click="closeMegaMenu">{{ t('mega.myOrders') }}</router-link></li>
            </ul>
          </div>

          <div class="mega-col">
            <div class="mega-spotlight-card" @click="goMegaSpotlight">
              <div class="mega-spotlight-img-wrap">
                <img
                  :src="megaSpotlightProduct?.thumbnailUrl || 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=700&q=80'"
                  :alt="megaSpotlightProduct?.productName || 'ADHA Archive'"
                />
              </div>
              <div class="mega-spot-title">
                {{ megaSpotlightProduct?.productName || 'ADHA COLLABORATIONS ARCHIVE' }}
              </div>
              <span class="mega-spot-cta">{{ t('mega.exploreNow') }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Expandable Mobile Search Bar -->
      <div v-if="mobileSearchOpen" class="mobile-search-dropdown">
        <div class="adi-container mobile-search-row">
          <input
            type="text"
            v-model="searchKeyword"
            :placeholder="t('nav.searchPlaceholder')"
            @keyup.enter="handleSearch"
            class="mobile-search-input"
            autofocus
          />
          <button type="button" class="btn-search-exec" @click="handleSearch">{{ t('nav.searchBtn') }}</button>
          <button type="button" class="btn-close-search" @click="mobileSearchOpen = false" aria-label="Close">✕</button>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="store-main">
      <router-view />
    </main>

    <!-- 3. FOREST-GREEN MEMBERSHIP BANNER (#427F5C) -->
    <section class="adi-club-banner">
      <div class="adi-container club-inner">
        <h2 class="club-headline">{{ t('club.headline') }}</h2>
        <button
          v-if="!authStore.isAuthenticated"
          type="button"
          class="adi-btn-white-offset"
          @click="openAuthModal('register')"
        >
          <span>{{ t('club.registerCta') }}</span>
          <span class="adi-btn-arrow">→</span>
        </button>
        <router-link v-else :to="lp('shop')" class="adi-btn-white-offset">
          <span>{{ t('club.shopCta') }}</span>
          <span class="adi-btn-arrow">→</span>
        </router-link>
      </div>
    </section>

    <!-- 4. SOLID BLACK 6-COLUMN FOOTER -->
    <footer class="adi-footer">
      <div class="adi-container footer-directory">
        <div class="footer-col">
          <h4 class="footer-heading">{{ t('footer.products') }}</h4>
          <ul class="footer-links">
            <li><router-link :to="lp('shop')">{{ t('footer.shoes') }}</router-link></li>
            <li><router-link :to="{ path: lp('shop'), query: { sort: 'newest' } }">{{ t('footer.newArrivals') }}</router-link></li>
            <li><router-link :to="lp('shop')">{{ t('footer.releaseDates') }}</router-link></li>
            <li><router-link :to="lp('shop')">{{ t('footer.topSellers') }}</router-link></li>
            <li><a href="javascript:void(0)" @click="openAuthModal('register')">{{ t('footer.memberExclusives') }}</a></li>
            <li><router-link :to="{ path: lp('shop'), query: { sort: 'price_asc' } }">{{ t('footer.outlet') }}</router-link></li>
          </ul>
        </div>

        <div class="footer-col">
          <h4 class="footer-heading">{{ t('footer.sports') }}</h4>
          <ul class="footer-links">
            <li v-for="c in categories" :key="c.id">
              <router-link :to="{ path: lp('shop'), query: { category: c.id } }">{{ c.categoryName }}</router-link>
            </li>
            <li><router-link :to="lp('shop')">Gym &amp; Training</router-link></li>
            <li><router-link :to="lp('shop')">Outdoor</router-link></li>
          </ul>
        </div>

        <div class="footer-col">
          <h4 class="footer-heading">{{ t('footer.collections') }}</h4>
          <ul class="footer-links">
            <li v-for="b in brands.slice(0, 7)" :key="b.id">
              <router-link :to="{ path: lp('shop'), query: { brand: b.id } }">{{ b.brandName }}</router-link>
            </li>
          </ul>
        </div>

        <div class="footer-col">
          <h4 class="footer-heading">{{ t('footer.company') }}</h4>
          <ul class="footer-links">
            <li><router-link :to="lp('')">{{ t('footer.about') }}</router-link></li>
            <li><router-link :to="lp('shop')">{{ t('footer.careers') }}</router-link></li>
            <li><router-link :to="lp('shop')">{{ t('footer.news') }}</router-link></li>
            <li><router-link :to="lp('shop')">{{ t('footer.stories') }}</router-link></li>
            <li v-if="isStaffOrAdmin"><router-link to="/admin/dashboard">{{ t('footer.adminPortal') }}</router-link></li>
          </ul>
        </div>

        <div class="footer-col">
          <h4 class="footer-heading">{{ t('footer.support') }}</h4>
          <ul class="footer-links">
            <li><a href="javascript:void(0)" @click="promoSheetOpen = true">{{ t('footer.help') }}</a></li>
            <li><router-link :to="lp('tracking')">{{ t('footer.tracker') }}</router-link></li>
            <li><a href="javascript:void(0)" @click="showSizeGuide = true">{{ t('footer.sizeChart') }}</a></li>
            <li><router-link :to="lp('cart')">{{ t('footer.payment') }}</router-link></li>
            <li><router-link :to="lp('my-orders')">{{ t('footer.deliveryReturns') }}</router-link></li>
            <li><a href="javascript:void(0)" @click="showWishlist = true">{{ t('footer.wishlist') }}</a></li>
          </ul>
        </div>

        <div class="footer-col">
          <h4 class="footer-heading">{{ t('footer.followUs') }}</h4>
          <div class="footer-social-stack">
            <a href="javascript:void(0)" aria-label="Facebook" class="social-icon-link">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"/></svg>
            </a>
            <a href="javascript:void(0)" aria-label="Instagram" class="social-icon-link">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="20" height="20" rx="5"/><circle cx="12" cy="12" r="4"/><circle cx="17.5" cy="6.5" r="1" fill="currentColor"/></svg>
            </a>
            <a href="javascript:void(0)" aria-label="X" class="social-icon-link">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
            </a>
            <a href="javascript:void(0)" aria-label="TikTok" class="social-icon-link">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19.59 6.69a4.83 4.83 0 0 1-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 0 1-5.2 1.74 2.89 2.89 0 0 1 2.31-4.64 2.93 2.93 0 0 1 .88.13V9.4a6.84 6.84 0 0 0-1-.05A6.33 6.33 0 0 0 5 20.1a6.34 6.34 0 0 0 10.86-4.43v-7a8.16 8.16 0 0 0 4.77 1.52v-3.4a4.85 4.85 0 0 1-1-.1z"/></svg>
            </a>
            <a href="javascript:void(0)" aria-label="YouTube" class="social-icon-link">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M23.5 6.2a3 3 0 0 0-2.1-2.1C19.5 3.5 12 3.5 12 3.5s-7.5 0-9.4.6A3 3 0 0 0 .5 6.2 31.4 31.4 0 0 0 0 12a31.4 31.4 0 0 0 .5 5.8 3 3 0 0 0 2.1 2.1c1.9.6 9.4.6 9.4.6s7.5 0 9.4-.6a3 3 0 0 0 2.1-2.1A31.4 31.4 0 0 0 24 12a31.4 31.4 0 0 0-.5-5.8zM9.6 15.6V8.4l6.3 3.6-6.3 3.6z"/></svg>
            </a>
          </div>
        </div>
      </div>

      <!-- 5. SINGLE-LINE BLACK LEGAL BAR -->
      <div class="adi-footer-bottom">
        <div class="adi-container legal-inner">
          <div class="legal-links-row">
            <span>{{ t('footer.cookie') }}</span>
            <span class="legal-sep">|</span>
            <span>{{ t('footer.privacy') }}</span>
            <span class="legal-sep">|</span>
            <span>{{ t('footer.terms') }}</span>
            <span class="legal-sep">|</span>
            <span>{{ t('footer.imprint') }}</span>
            <span class="legal-sep">|</span>
            <span>{{ t('footer.copyright') }}</span>
          </div>
        </div>
      </div>
    </footer>

    <!-- Size Guide Modal -->
    <el-dialog
      v-model="showSizeGuide"
      :title="t('footer.sizeDialogTitle')"
      width="640px"
      class="custom-size-dialog"
    >
      <div class="dialog-content">
        <p class="dialog-tip">{{ t('footer.sizeDialogTip') }}</p>
        <el-table :data="sizeGuideData" border stripe class="size-table">
          <el-table-column prop="eu" label="EU (VN)" align="center" width="90" />
          <el-table-column prop="us" label="US Men" align="center" width="90" />
          <el-table-column prop="uk" label="UK" align="center" width="90" />
          <el-table-column prop="cm" :label="t('footer.footLength')" align="center" />
        </el-table>
      </div>
    </el-dialog>

    <!-- Client Auth Modal -->
    <ClientAuthModal v-model="authModalVisible" :initial-tab="authModalTab" />

    <!-- Wishlist Drawer -->
    <WishlistDrawer v-model="showWishlist" />

    <!-- Mobile Navigation Drawer -->
    <el-drawer
      v-model="mobileMenuOpen"
      direction="left"
      size="320px"
      :with-header="false"
      class="mobile-nav-drawer"
    >
      <div class="mobile-drawer-inner">
        <div class="drawer-header">
          <router-link :to="lp('')" class="adi-logo" @click="mobileMenuOpen = false">
            <AdhaLogoMark :size="32" variant="dark" />
          </router-link>
          <button type="button" class="btn-drawer-close" @click="mobileMenuOpen = false" aria-label="Close">✕</button>
        </div>

        <div v-if="authStore.isAuthenticated" class="drawer-user-box">
          <div class="user-chip-avatar">{{ userInitials }}</div>
          <div class="user-chip-info">
            <strong>{{ authStore.user?.fullName || authStore.user?.username }}</strong>
            <span>{{ authStore.user?.phone || authStore.user?.email || t('account.memberSub') }}</span>
          </div>
        </div>
        <div v-else class="drawer-guest-box" @click="openAuthModal('login'); mobileMenuOpen = false">
          <div>
            <strong>{{ t('account.mobileLoginTitle') }}</strong>
            <span>{{ t('account.mobileLoginSub') }}</span>
          </div>
          <span class="arrow">→</span>
        </div>

        <!-- Mobile Language Switcher Row -->
        <div class="drawer-lang-row">
          <button
            type="button"
            class="drawer-lang-btn"
            :class="{ active: locale === 'vi' }"
            @click="switchLocale('vi', router, route); mobileMenuOpen = false"
          >
            VN · Tiếng Việt
          </button>
          <button
            type="button"
            class="drawer-lang-btn"
            :class="{ active: locale === 'en' }"
            @click="switchLocale('en', router, route); mobileMenuOpen = false"
          >
            US · English
          </button>
        </div>

        <nav class="drawer-nav-items">
          <router-link :to="{ path: lp('shop'), query: { sort: 'price_asc' } }" class="drawer-item sale-item" @click="mobileMenuOpen = false">
            <span>{{ t('nav.sale') }}</span>
            <span class="arrow">›</span>
          </router-link>
          <router-link :to="lp('shop')" class="drawer-item strong-item" @click="mobileMenuOpen = false">
            <span>{{ t('nav.men') }}</span>
            <span class="arrow">›</span>
          </router-link>
          <router-link :to="lp('shop')" class="drawer-item strong-item" @click="mobileMenuOpen = false">
            <span>{{ t('nav.women') }}</span>
            <span class="arrow">›</span>
          </router-link>
          <router-link :to="{ path: lp('shop'), query: { sort: 'newest' } }" class="drawer-item strong-item" @click="mobileMenuOpen = false">
            <span>{{ t('nav.newArrivals') }}</span>
            <span class="arrow">›</span>
          </router-link>
          <router-link :to="{ path: lp('shop'), query: { sort: 'price_asc' } }" class="drawer-item" @click="mobileMenuOpen = false">
            <span>{{ t('nav.outlet') }}</span>
            <span class="arrow">›</span>
          </router-link>

          <div class="drawer-section-label">{{ t('mega.brands') }}</div>
          <router-link
            v-for="b in brands.slice(0, 6)"
            :key="b.id"
            :to="{ path: lp('shop'), query: { brand: b.id } }"
            class="drawer-item sub-item"
            @click="mobileMenuOpen = false"
          >
            <span>{{ b.brandName }}</span>
            <span class="arrow">›</span>
          </router-link>

          <div class="drawer-section-label">{{ t('account.mobileSupport') }}</div>
          <router-link v-if="authStore.isAuthenticated" :to="lp('my-account/profile')" class="drawer-item" @click="mobileMenuOpen = false">
            <span>{{ t('account.profile') }}</span>
            <span class="arrow">›</span>
          </router-link>
          <router-link v-if="authStore.isAuthenticated" :to="lp('my-account/address-book')" class="drawer-item" @click="mobileMenuOpen = false">
            <span>{{ locale === 'en' ? 'Address Book' : 'Sổ địa chỉ' }}</span>
            <span class="arrow">›</span>
          </router-link>
          <router-link :to="lp('my-orders')" class="drawer-item" @click="mobileMenuOpen = false">
            <span>{{ t('account.myOrders') }}</span>
            <span class="arrow">›</span>
          </router-link>
          <router-link :to="lp('tracking')" class="drawer-item" @click="mobileMenuOpen = false">
            <span>{{ t('account.orderTracker') }}</span>
            <span class="arrow">›</span>
          </router-link>
          <button type="button" class="drawer-item as-button" @click="showWishlist = true; mobileMenuOpen = false">
            <span>{{ t('account.wishlist') }}</span>
            <span class="badge" v-if="wishlistStore.count > 0">{{ wishlistStore.count }}</span>
            <span class="arrow" v-else>›</span>
          </button>
          <button type="button" class="drawer-item as-button" @click="showSizeGuide = true; mobileMenuOpen = false">
            <span>{{ t('footer.sizeChart') }}</span>
            <span class="arrow">›</span>
          </button>
          <router-link
            v-if="isStaffOrAdmin"
            to="/admin/dashboard"
            class="drawer-item admin-item"
            @click="mobileMenuOpen = false"
          >
            <span>{{ t('footer.adminPortal') }}</span>
            <span class="arrow">›</span>
          </router-link>
        </nav>

        <div class="drawer-footer-row" v-if="authStore.isAuthenticated">
          <button type="button" class="btn-drawer-logout" @click="authStore.logout(); mobileMenuOpen = false">
            <el-icon><SwitchButton /></el-icon>
            <span>{{ t('account.logout') }}</span>
          </button>
        </div>
      </div>
    </el-drawer>

    <!-- ================================================================ -->
    <!-- EXACT LIVE ADIDAS 390px RIGHT SLIDE-OVER "adhaclub" MEMBER DRAWER -->
    <!-- ================================================================ -->
    <transition name="club-drawer-fade">
      <div v-if="memberDrawerOpen && authStore.isAuthenticated" class="adi-club-scrim" @click.self="memberDrawerOpen = false">
        <aside class="adi-club-drawer" role="dialog" aria-label="ADHA Club Member">
          <!-- Top White Wordmark Header -->
          <div class="club-drawer-topbar">
            <span class="club-wordmark">adhaclub</span>
            <div class="club-top-icons">
              <button
                type="button"
                class="club-icon-btn"
                :title="t('account.profile')"
                @click="memberDrawerOpen = false; router.push(lp('my-account/profile'))"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
              </button>
              <button
                type="button"
                class="club-icon-btn"
                :title="t('account.orderTracker')"
                @click="memberDrawerOpen = false; router.push(lp('tracking'))"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <circle cx="12" cy="12" r="9"></circle>
                  <line x1="12" y1="16" x2="12" y2="12"></line>
                  <line x1="12" y1="8" x2="12.01" y2="8"></line>
                </svg>
              </button>
              <button type="button" class="club-icon-btn" @click="memberDrawerOpen = false" aria-label="Close">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>

          <!-- Cream #f8f8f0 Points & Primary Nav Rows (Exact adidas adiClub) -->
          <div class="club-cream-hero">
            <div class="club-points-row">
              <span class="club-star-badge">★</span>
              <div class="club-pts-text">
                <strong class="pts-num">{{ memberPoints }}</strong>
                <span class="pts-sub">{{ locale === 'en' ? 'Points to spend' : 'Điểm Cần Dùng' }}</span>
              </div>
              <span class="club-user-pill">{{ authStore.user?.fullName || authStore.user?.username }}</span>
            </div>

            <div class="club-arrow-nav">
              <button type="button" class="club-nav-row" @click="memberDrawerOpen = false; router.push(lp('order-history'))">
                <span>
                  {{ locale === 'en' ? 'MY ORDERS' : 'ĐƠN HÀNG CỦA TÔI' }}
                  <em v-if="activeOrdersCount > 0" class="row-cnt-tag">{{ activeOrdersCount }}</em>
                </span>
                <span class="row-arrow">→</span>
              </button>
              <button type="button" class="club-nav-row" @click="memberDrawerTab = 'rewards'">
                <span>
                  {{ locale === 'en' ? 'MY REWARDS & VOUCHERS' : 'PHẦN THƯỞNG CỦA TÔI' }}
                  <em v-if="personalVouchersCount > 0" class="row-cnt-tag">{{ personalVouchersCount }}</em>
                </span>
                <span class="row-arrow">→</span>
              </button>
              <button type="button" class="club-nav-row" @click="memberDrawerOpen = false; router.push(lp('my-account/profile'))">
                <span>{{ locale === 'en' ? 'PERSONAL INFORMATION' : 'THÔNG TIN CÁ NHÂN' }}</span>
                <span class="row-arrow">→</span>
              </button>
              <button type="button" class="club-nav-row" @click="memberDrawerOpen = false; router.push(lp('my-account/address-book'))">
                <span>{{ locale === 'en' ? 'ADDRESS BOOK' : 'SỔ ĐỊA CHỈ' }}</span>
                <span class="row-arrow">→</span>
              </button>
            </div>
          </div>

          <!-- Underline Tab Bar -->
          <div class="club-drawer-tabs">
            <button
              type="button"
              class="club-tab-btn"
              :class="{ active: memberDrawerTab === 'foryou' }"
              @click="memberDrawerTab = 'foryou'"
            >
              {{ locale === 'en' ? 'JUST FOR YOU' : 'CHỈ DÀNH CHO BẠN' }}
            </button>
            <button
              type="button"
              class="club-tab-btn"
              :class="{ active: memberDrawerTab === 'rewards' }"
              @click="memberDrawerTab = 'rewards'"
            >
              {{ locale === 'en' ? 'REWARDS' : 'PHẦN THƯỞNG' }}
            </button>
          </div>

          <!-- Scrollable Club Body -->
          <div class="club-drawer-body">
            <template v-if="memberDrawerTab === 'foryou'">
              <div class="club-promo-card" @click="memberDrawerOpen = false; router.push({ path: lp('shop'), query: { sort: 'price_asc' } })">
                <div class="card-copy">
                  <strong>{{ locale === 'en' ? 'ADHA Club Black Friday Access' : 'Đặc quyền ADHA Club Black Friday' }}</strong>
                  <p>{{ locale === 'en' ? 'Unlock exclusive sneaker archive deals and extra 10% member voucher.' : 'Mở khóa ưu đãi giảm đến 50% và nhận thêm voucher hội viên 10% tại giỏ hàng.' }}</p>
                </div>
                <span class="card-arrow">→</span>
              </div>

              <div class="club-promo-card green-framed" @click="memberDrawerOpen = false; router.push(lp('my-account/profile'))">
                <span class="green-pts-pill">{{ locale === 'en' ? 'EARN 50 POINTS' : 'KIẾM 50 ĐIỂM' }}</span>
                <div class="card-copy">
                  <strong>{{ locale === 'en' ? 'Complete your member profile' : 'Hoàn tất hồ sơ ADHA Club' }}</strong>
                  <p>{{ locale === 'en' ? 'Save your phone & shipping address for instant 1-click checkout.' : 'Lưu sẵn số điện thoại & địa chỉ giao hàng để đặt giày siêu tốc.' }}</p>
                </div>
                <span class="card-arrow">→</span>
              </div>

              <div class="club-promo-card" @click="memberDrawerOpen = false; showWishlist = true">
                <div class="card-copy">
                  <strong>{{ locale === 'en' ? `Your Wishlist (${wishlistStore.count})` : `Danh sách yêu thích (${wishlistStore.count})` }}</strong>
                  <p>{{ locale === 'en' ? 'Quickly access all sneakers you have saved for later.' : 'Xem lại nhanh những đôi giày sneaker bạn đã lưu giữ.' }}</p>
                </div>
                <span class="card-arrow">→</span>
              </div>

              <div class="club-benefits-box">
                <h4>{{ locale === 'en' ? 'Your instant benefits' : 'Các lợi ích tức thời của bạn' }}</h4>
                <ul>
                  <li>✓ {{ locale === 'en' ? 'Free nationwide shipping on orders from 1,000,000₫' : 'Miễn phí vận chuyển toàn quốc đơn từ 1.000.000₫' }}</li>
                  <li>✓ {{ locale === 'en' ? '100% Legit Check authenticity guarantee' : 'Bảo chứng 100% Sneaker chính hãng Legit Check' }}</li>
                  <li>✓ {{ locale === 'en' ? 'Free 7-day sneaker size exchanges' : 'Hỗ trợ đổi size miễn phí trong 7 ngày' }}</li>
                </ul>
              </div>
            </template>

            <template v-else>
              <div v-if="personalVouchersList.length > 0" class="club-vouchers-stack">
                <div v-for="v in personalVouchersList" :key="v.code" class="club-voucher-card">
                  <div class="cv-top">
                    <span class="cv-code">{{ v.code }}</span>
                    <span class="cv-badge">{{ v.sourceType === 'PERSONAL' ? 'MEMBER' : 'PUBLIC' }}</span>
                  </div>
                  <p class="cv-desc">
                    {{
                      v.discountType === 'PERCENT'
                        ? `${locale === 'en' ? 'Save' : 'Giảm'} ${v.discountValue}%`
                        : `${locale === 'en' ? 'Save' : 'Giảm'} ${v.discountValue?.toLocaleString('vi-VN')}₫`
                    }}
                  </p>
                  <button type="button" class="btn-cv-use" @click="memberDrawerOpen = false; router.push(lp('cart'))">
                    {{ locale === 'en' ? 'Use at Checkout →' : 'Dùng tại Giỏ hàng →' }}
                  </button>
                </div>
              </div>
              <div v-else class="club-no-rewards">
                <p>{{ locale === 'en' ? 'No active vouchers yet. Place an order to earn points & unlock rewards!' : 'Bạn chưa có voucher cá nhân nào. Đặt đơn ngay để tích điểm và nhận mã giảm giá!' }}</p>
                <button type="button" class="btn-cv-use" @click="memberDrawerOpen = false; router.push(lp('shop'))">
                  {{ locale === 'en' ? 'Shop Sneakers →' : 'Khám phá Giày ngay →' }}
                </button>
              </div>
            </template>
          </div>

          <!-- Bottom Sign Out -->
          <div class="club-drawer-footer">
            <button type="button" class="btn-club-logout" @click="memberDrawerOpen = false; handleUserMenuCommand('logout')">
              <span>{{ t('account.logout') }}</span>
              <span>→</span>
            </button>
          </div>
        </aside>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AdhaLogoMark from '@/components/common/AdhaLogoMark.vue'
import { useCartStore } from '@/stores/cart'
import { useClientAuthStore } from '@/stores/clientAuth'
import { useWishlistStore } from '@/stores/wishlist'
import { useStoreI18n } from '@/composables/useStoreI18n'
import storeService from '@/services/storeService'
import { ElMessage } from 'element-plus'
import { Tickets, Star, Search, User, SwitchButton } from '@element-plus/icons-vue'
import ClientAuthModal from '@/components/store/ClientAuthModal.vue'
import WishlistDrawer from '@/components/store/WishlistDrawer.vue'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()
const authStore = useClientAuthStore()
const wishlistStore = useWishlistStore()
const { locale, t, lp, switchLocale } = useStoreI18n()

const searchKeyword = ref('')
const isScrolled = ref(false)
const showSizeGuide = ref(false)
const showWishlist = ref(false)
const mobileMenuOpen = ref(false)
const mobileSearchOpen = ref(false)
const authModalVisible = ref(false)
const authModalTab = ref('login')
const promoSheetOpen = ref(false)
const activeMegaMenu = ref(null)
const memberDrawerOpen = ref(false)
const memberDrawerTab = ref('foryou')
const totalOrdersCount = ref(0)
const personalVouchersList = ref([])

const memberPoints = computed(() => 50 + totalOrdersCount.value * 100)

const openMemberDrawer = () => {
  memberDrawerTab.value = 'foryou'
  memberDrawerOpen.value = true
  loadAccountNotifications()
}

// Language Popover State
const langWrapRef = ref(null)
const langMenuOpen = ref(false)
const pendingLocale = ref(locale.value)

const toggleLangMenu = () => {
  if (!langMenuOpen.value) {
    pendingLocale.value = locale.value
  }
  langMenuOpen.value = !langMenuOpen.value
}

const applyLanguage = () => {
  langMenuOpen.value = false
  switchLocale(pendingLocale.value, router, route)
}

const handleDocumentClick = (e) => {
  if (langMenuOpen.value && langWrapRef.value && !langWrapRef.value.contains(e.target)) {
    langMenuOpen.value = false
  }
}

const brands = ref([])
const categories = ref([])
const megaSpotlightProduct = ref(null)
const activeOrdersCount = ref(0)
const personalVouchersCount = ref(0)

const accountNoticeCount = computed(() => activeOrdersCount.value + personalVouchersCount.value)

const loadAccountNotifications = async () => {
  if (!authStore.isAuthenticated) {
    activeOrdersCount.value = 0
    totalOrdersCount.value = 0
    personalVouchersCount.value = 0
    personalVouchersList.value = []
    return
  }
  try {
    const phone = authStore.user?.phone || undefined
    const [ordersRes, vouchersRes] = await Promise.all([
      storeService.getMyOrders(phone ? { phone } : {}).catch(() => null),
      storeService.getAvailableVouchers(phone ? { phone } : {}).catch(() => null)
    ])
    const ordersList = Array.isArray(ordersRes?.data)
      ? ordersRes.data
      : ordersRes?.data?.content || []
    totalOrdersCount.value = ordersList.length
    activeOrdersCount.value = ordersList.filter((o) =>
      ['PENDING', 'CONFIRMED', 'SHIPPING'].includes(String(o?.status || '').toUpperCase())
    ).length

    const vouchersList = Array.isArray(vouchersRes?.data) ? vouchersRes.data : []
    personalVouchersList.value = vouchersList
    personalVouchersCount.value = vouchersList.filter((v) => v?.sourceType === 'PERSONAL').length
  } catch {
    activeOrdersCount.value = 0
    totalOrdersCount.value = 0
    personalVouchersCount.value = 0
    personalVouchersList.value = []
  }
}

const promoMessages = computed(() => [t('promo.m1'), t('promo.m2'), t('promo.m3')])
const activePromoIdx = ref(0)
let promoTimer = null
let megaCloseTimer = null

const openMegaMenu = (key) => {
  if (megaCloseTimer) {
    clearTimeout(megaCloseTimer)
    megaCloseTimer = null
  }
  activeMegaMenu.value = key
}

const keepMegaMenu = () => {
  if (megaCloseTimer) {
    clearTimeout(megaCloseTimer)
    megaCloseTimer = null
  }
}

const closeMegaMenu = () => {
  megaCloseTimer = window.setTimeout(() => {
    activeMegaMenu.value = null
  }, 120)
}

const goMegaSpotlight = () => {
  const id = megaSpotlightProduct.value?.id
  activeMegaMenu.value = null
  if (id) {
    router.push(lp(`product/${id}`))
  } else {
    router.push(lp('shop'))
  }
}

const isStaffOrAdmin = computed(() => {
  try {
    const token = localStorage.getItem('token')
    const rawUser = localStorage.getItem('user')
    if (!token || !rawUser) return false
    const parsed = JSON.parse(rawUser)
    const role = String(parsed?.role || parsed?.roleName || '').toUpperCase()
    return role.includes('ADMIN') || role.includes('STAFF') || role.includes('MANAGER')
  } catch {
    return false
  }
})

const userInitials = computed(() => {
  const name = authStore.user?.fullName || authStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const openAuthModal = (tab = 'login') => {
  authModalTab.value = tab
  authModalVisible.value = true
}

watch(
  () => route.query.auth,
  (authQuery) => {
    if (authQuery === 'login' || authQuery === 'register') {
      if (!authStore.isAuthenticated) {
        openAuthModal(authQuery)
      }
      const nextQuery = { ...route.query }
      delete nextQuery.auth
      router.replace({ path: route.path, query: nextQuery })
    }
  },
  { immediate: true }
)

const handleUserMenuCommand = (command) => {
  if (command === 'my-orders') {
    router.push(lp('my-orders'))
  } else if (command === 'cart') {
    router.push(lp('cart'))
  } else if (command === 'wishlist') {
    showWishlist.value = true
  } else if (command === 'tracking') {
    router.push(lp('tracking'))
  } else if (command === 'profile') {
    openAuthModal('profile')
  } else if (command === 'logout') {
    authStore.logout()
    activeOrdersCount.value = 0
    personalVouchersCount.value = 0
    ElMessage.success(t('account.loggedOut'))
  }
}

watch(
  () => authStore.isAuthenticated,
  () => {
    loadAccountNotifications()
  }
)

const sizeGuideData = [
  { eu: '36', us: '4.0', uk: '3.5', cm: '22.5 cm' },
  { eu: '37', us: '5.0', uk: '4.5', cm: '23.0 cm' },
  { eu: '38', us: '5.5', uk: '5.0', cm: '24.0 cm' },
  { eu: '39', us: '6.5', uk: '6.0', cm: '24.5 cm' },
  { eu: '40', us: '7.0', uk: '6.0', cm: '25.0 cm' },
  { eu: '41', us: '8.0', uk: '7.0', cm: '26.0 cm' },
  { eu: '42', us: '8.5', uk: '7.5', cm: '26.5 cm' },
  { eu: '43', us: '9.5', uk: '8.5', cm: '27.5 cm' },
  { eu: '44', us: '10.0', uk: '9.0', cm: '28.0 cm' },
  { eu: '45', us: '11.0', uk: '10.0', cm: '29.0 cm' }
]

const handleSearch = () => {
  const kw = searchKeyword.value.trim()
  mobileSearchOpen.value = false
  activeMegaMenu.value = null
  if (kw) {
    router.push({ path: lp('shop'), query: { q: kw } })
  } else {
    router.push(lp('shop'))
  }
}

const loadNavMeta = async () => {
  try {
    const [bRes, cRes, bestRes] = await Promise.all([
      storeService.getBrands(),
      storeService.getCategories(),
      storeService.getBestSellerProduct().catch(() => null)
    ])
    brands.value = bRes?.data || []
    categories.value = cRes?.data || []
    megaSpotlightProduct.value = bestRes?.data || null
  } catch (err) {
    console.warn('Không tải được danh mục điều hướng:', err)
  }
}

const handleScroll = () => {
  isScrolled.value = window.scrollY > 40
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  document.addEventListener('click', handleDocumentClick)
  loadNavMeta()
  loadAccountNotifications()
  promoTimer = window.setInterval(() => {
    activePromoIdx.value = (activePromoIdx.value + 1) % 3
  }, 5500)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  document.removeEventListener('click', handleDocumentClick)
  if (promoTimer) clearInterval(promoTimer)
  if (megaCloseTimer) clearTimeout(megaCloseTimer)
})
</script>

<style scoped lang="scss">
.store-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  color: #000000;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  width: 100%;
  max-width: 100%;
  overflow-x: clip;
}

.adi-header-container {
  width: 100%;
  max-width: none;
  margin: 0 auto;
  padding: 0 40px;
  box-sizing: border-box;

  @media (max-width: 1024px) {
    padding: 0 20px;
  }
}

.adi-container {
  max-width: none;
  width: 100%;
  margin: 0 auto;
  padding: 0 6.2vw;
  box-sizing: border-box;

  @media (max-width: 1024px) {
    padding: 0 24px;
  }

  @media (max-width: 640px) {
    padding: 0 16px;
  }
}

/* ============ 1. TOP BLACK USP BAR (40px) ============ */
.adi-promo-bar {
  background: #000000;
  color: #ffffff;
  height: 40px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #c8cbcc;
  cursor: pointer;
  user-select: none;

  .promo-inner {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }

  .promo-message {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    text-align: center;
  }

  .promo-chevron {
    width: 14px;
    height: 14px;
    flex-shrink: 0;
    transition: transform 0.2s ease;

    &.rotated {
      transform: rotate(180deg);
    }
  }
}

.adi-promo-sheet {
  background: #ffffff;
  border-bottom: 1px solid #000000;
  padding: 32px 0;
  position: relative;

  .promo-sheet-inner {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 32px;
    position: relative;

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }

  .promo-sheet-col {
    h4 {
      font-size: 13px;
      font-weight: 700;
      letter-spacing: 0.06em;
      margin: 0 0 8px;
    }

    p {
      font-size: 13px;
      line-height: 1.55;
      color: #444444;
      margin: 0 0 12px;
    }

    a,
    .sheet-link-btn {
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.08em;
      color: #000000;
      text-decoration: underline;
      text-underline-offset: 3px;
      background: none;
      border: none;
      padding: 0;
      cursor: pointer;
      font-family: inherit;
    }
  }

  .promo-sheet-close {
    position: absolute;
    top: -12px;
    right: 6.2vw;
    width: 32px;
    height: 32px;
    border: 1px solid #000000;
    background: #ffffff;
    color: #000000;
    cursor: pointer;
  }
}

/* ============ 2. STICKY 81px WHITE HEADER ============ */
.adi-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: #ffffff;
  border-bottom: 1px solid #eceff1;
  transition: box-shadow 0.2s ease;

  &.scrolled {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }
}

.adi-utility-row {
  height: 32px;
  display: flex;
  align-items: center;

  @media (max-width: 960px) {
    display: none;
  }

  .utility-inner {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 18px;
  }

  .utility-link {
    font-size: 12px;
    line-height: 20px;
    font-weight: 400;
    color: #000000;
    text-decoration: none;
    background: none;
    border: none;
    padding: 0;
    font-family: inherit;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }

    &.utility-admin {
      font-weight: 700;
      padding: 0 6px;
      background: #eceff1;
      text-decoration: none;
    }
  }
}

/* ============ EXACT ADIDAS LANGUAGE SWITCHER POPOVER ============ */
.adi-lang-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.adi-flag-trigger {
  position: relative;
  z-index: 1020;
  height: 30px;
  padding: 0 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid transparent;
  border-bottom: none;
  cursor: pointer;

  svg {
    display: block;
    border: 1px solid rgba(0, 0, 0, 0.15);
  }

  &.open {
    background: #ffffff;
    border-color: #000000;
    margin-bottom: -1px;
    padding-bottom: 1px;
  }
}

.adi-lang-popover {
  position: absolute;
  top: 100%;
  right: 0;
  z-index: 1015;
  width: 268px;
  background: #ffffff;
  border: 1px solid #000000;
  padding: 22px 24px 26px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;

  .lang-pop-title {
    font-size: 16px;
    font-weight: 500;
    color: #000000;
    margin-bottom: 20px;
  }

  .lang-radio-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 0;
    cursor: pointer;
    user-select: none;

    .radio-circle {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      border: 1.5px solid #767677;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      transition: border-color 0.14s ease;

      .radio-inner {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background: transparent;
        transition: background-color 0.14s ease;
      }

      &.checked {
        border-color: #000000;

        .radio-inner {
          background: #000000;
        }
      }
    }

    .lang-label {
      font-size: 15px;
      color: #000000;
      font-weight: 400;
      flex: 1;

      &.bold {
        font-weight: 700;
      }
    }

    .opt-flag {
      display: block;
      border: 1px solid rgba(0, 0, 0, 0.15);
      flex-shrink: 0;
    }
  }

  .btn-lang-save {
    position: relative;
    margin-top: 18px;
    height: 46px;
    padding: 0 22px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    font-family: inherit;
    font-size: 14px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    gap: 16px;
    cursor: pointer;

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      transform: translate(3px, 3px);
      border: 1px solid #000000;
      pointer-events: none;
    }

    .save-arrow {
      font-size: 18px;
      line-height: 1;
      transition: transform 0.15s ease;
    }

    &:hover {
      color: #b8b8b8;

      .save-arrow {
        transform: translateX(4px);
      }
    }
  }
}

.adi-nav-row {
  height: 49px;
  display: flex;
  align-items: center;

  @media (max-width: 960px) {
    height: 58px;
  }

  .nav-inner {
    position: relative;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.adi-logo {
  display: inline-flex;
  align-items: center;
  text-decoration: none;
  transform: translateY(-14px);

  @media (max-width: 960px) {
    transform: none;
  }
}

.adi-main-nav {
  position: absolute;
  left: 48.5%;
  transform: translateX(-50%);
  height: 100%;
  display: flex;
  align-items: stretch;
  justify-content: center;

  @media (max-width: 1360px) {
    position: static;
    transform: none;
    flex: 1;
  }

  @media (max-width: 1080px) {
    display: none;
  }

  .adi-nav-link {
    position: relative;
    display: inline-flex;
    align-items: center;
    height: 100%;
    padding: 0 11px;
    font-size: 14px;
    line-height: 24px;
    letter-spacing: 0.01em;
    text-transform: uppercase;
    color: #000000;
    text-decoration: none;
    white-space: nowrap;

    &.sale-link {
      color: #e32b2b;
      font-weight: 700;

      &::after {
        background: #e32b2b;
      }
    }

    &.strong-link {
      font-weight: 700;
    }

    &.regular-link {
      font-weight: 400;
    }

    &::after {
      content: '';
      position: absolute;
      left: 10px;
      right: 10px;
      bottom: 0;
      height: 3px;
      background: #000000;
      transform: scaleX(0);
      transition: transform 0.15s ease;
    }

    &:hover::after,
    &.active::after {
      transform: scaleX(1);
    }
  }
}

.adi-mega-menu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #ffffff;
  border-top: 1px solid #eceff1;
  border-bottom: 1px solid #000000;
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.08);
  padding: 28px 0 34px;
  z-index: 950;

  @media (max-width: 1080px) {
    display: none;
  }

  .mega-grid {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr 260px;
    gap: 32px;
  }

  .mega-heading {
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: #000000;
    margin: 0 0 14px;
    text-transform: uppercase;
  }

  .mega-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 9px;

    a {
      font-size: 14px;
      color: #000000;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }

      &.mega-sale {
        color: #e32b2b;
        font-weight: 700;
      }
    }
  }

  .mega-spotlight-card {
    cursor: pointer;

    .mega-spotlight-img-wrap {
      aspect-ratio: 4 / 3;
      background: #eceff1;
      overflow: hidden;
      margin-bottom: 10px;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
      }
    }

    .mega-spot-title {
      font-size: 14px;
      font-weight: 700;
      margin-bottom: 4px;
    }

    .mega-spot-cta {
      font-size: 13px;
      font-weight: 700;
      text-decoration: underline;
      text-underline-offset: 3px;
    }
  }
}

.adi-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.adi-search-box {
  display: flex;
  align-items: center;
  background: #e9ecef;
  border: 1px solid transparent;
  height: 32px;
  width: 192px;
  padding: 0 4px 0 10px;
  margin-right: 8px;

  &:focus-within {
    border-color: #000000;
    background: #ffffff;
  }

  @media (max-width: 1080px) {
    display: none;
  }

  .adi-search-input {
    flex: 1;
    min-width: 0;
    border: none;
    background: transparent;
    font-family: inherit;
    font-size: 14px;
    color: #000000;
    outline: none;

    &::placeholder {
      color: #555555;
    }
  }

  .adi-search-submit {
    width: 28px;
    height: 28px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: none;
    border: none;
    cursor: pointer;
    color: #000000;

    svg {
      width: 18px;
      height: 18px;
    }
  }
}

.adi-icon-action {
  position: relative;
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #000000;
  cursor: pointer;
  text-decoration: none;

  svg {
    width: 22px;
    height: 22px;
  }

  &:hover {
    background: #f4f5f6;
  }

  .adi-green-pill {
    position: absolute;
    top: 2px;
    right: 2px;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: #2e7d5b;
    color: #ffffff;
    font-size: 10px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .adi-badge {
    position: absolute;
    top: 3px;
    right: 3px;
    min-width: 17px;
    height: 17px;
    padding: 0 4px;
    border-radius: 50%;
    background: #000000;
    color: #ffffff;
    font-size: 10px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .adi-badge-cart {
    background: #0071ae;
  }
}

.mobile-icon-btn {
  display: none;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #000000;
  cursor: pointer;

  svg {
    width: 22px;
    height: 22px;
  }

  @media (max-width: 1080px) {
    display: inline-flex;
  }
}

.mobile-search-dropdown {
  background: #ffffff;
  border-top: 1px solid #eceff1;
  border-bottom: 1px solid #000000;
  padding: 10px 0;

  .mobile-search-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .mobile-search-input {
    flex: 1;
    height: 40px;
    background: #e9ecef;
    border: 1px solid transparent;
    padding: 0 12px;
    font-family: inherit;
    font-size: 14px;
    outline: none;

    &:focus {
      background: #ffffff;
      border-color: #000000;
    }
  }

  .btn-search-exec {
    height: 40px;
    padding: 0 18px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.08em;
    cursor: pointer;
  }

  .btn-close-search {
    width: 36px;
    height: 40px;
    background: none;
    border: none;
    font-size: 16px;
    cursor: pointer;
    color: #000000;
  }
}

.store-main {
  flex: 1;
  width: 100%;
}

/* ============ 3. MEMBERSHIP BANNER (#427F5C) ============ */
.adi-club-banner {
  background: #427f5c;
  color: #ffffff;
  padding: 42px 0;

  .club-inner {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 32px;
    flex-wrap: wrap;
    text-align: center;
  }

  .club-headline {
    font-family: 'Barlow Condensed', 'Inter', sans-serif;
    font-size: 32px;
    font-weight: 700;
    letter-spacing: 0.08em;
    line-height: 1.1;
    margin: 0;
    text-transform: uppercase;
    color: #ffffff;

    @media (max-width: 640px) {
      font-size: 24px;
    }
  }
}

.adi-btn-white-offset {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 48px;
  padding: 0 20px;
  background: #ffffff;
  color: #000000;
  border: 1px solid #ffffff;
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.15s ease;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    transform: translate(3px, 3px);
    border: 1px solid #ffffff;
    pointer-events: none;
  }

  .adi-btn-arrow {
    font-size: 18px;
    line-height: 1;
    transition: transform 0.15s ease;
  }

  &:hover {
    color: #555555;

    .adi-btn-arrow {
      transform: translateX(4px);
    }
  }
}

/* ============ 4. BLACK 6-COLUMN FOOTER ============ */
.adi-footer {
  background: #000000;
  color: #ffffff;
}

.footer-directory {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 28px;
  padding-top: 52px;
  padding-bottom: 56px;

  @media (max-width: 1024px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
    gap: 28px 18px;
    padding-top: 36px;
    padding-bottom: 36px;
  }

  .footer-heading {
    font-family: 'Barlow Condensed', 'Inter', sans-serif;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 0.06em;
    color: #ffffff;
    margin: 0 0 14px;
    text-transform: uppercase;
  }

  .footer-links {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;

    a {
      display: inline-block;
      padding: 6px 0;
      font-size: 13.5px;
      line-height: 20px;
      color: #ffffff;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .footer-social-stack {
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding-top: 4px;

    .social-icon-link {
      width: 22px;
      height: 22px;
      color: #ffffff;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      transition: opacity 0.15s ease;

      svg {
        width: 20px;
        height: 20px;
      }

      &:hover {
        opacity: 0.7;
      }
    }
  }
}

.adi-footer-bottom {
  background: #000000;
  border-top: 1px solid #444444;
  color: #ffffff;
  padding: 28px 0;

  .legal-inner {
    display: flex;
    justify-content: center;
    text-align: center;
  }

  .legal-links-row {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    align-items: center;
    gap: 10px;
    font-size: 12.5px;
    color: #d3d7da;

    .legal-sep {
      color: #555555;
    }
  }
}

.dropdown-user-header {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 2px;

  strong {
    font-size: 13.5px;
    color: #000000;
  }

  .dropdown-user-sub {
    font-size: 11.5px;
    color: #767676;
  }
}

.mobile-drawer-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eceff1;

  .btn-drawer-close {
    width: 36px;
    height: 36px;
    background: none;
    border: 1px solid #eceff1;
    font-size: 16px;
    cursor: pointer;
    color: #000000;
  }
}

.drawer-user-box,
.drawer-guest-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  background: #427f5c;
  color: #ffffff;
  cursor: pointer;

  strong {
    display: block;
    font-size: 12.5px;
    font-weight: 700;
    letter-spacing: 0.06em;
  }

  span {
    font-size: 12px;
    opacity: 0.9;
  }
}

.drawer-lang-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border-bottom: 1px solid #eceff1;

  .drawer-lang-btn {
    height: 40px;
    background: #ffffff;
    border: none;
    font-family: inherit;
    font-size: 12.5px;
    font-weight: 600;
    color: #000000;
    cursor: pointer;

    &.active {
      background: #000000;
      color: #ffffff;
    }
  }
}

.user-chip-avatar {
  width: 36px;
  height: 36px;
  background: #000000;
  color: #ffffff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-chip-info {
  flex: 1;
  min-width: 0;
}

.drawer-section-label {
  padding: 14px 20px 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #767676;
  background: #f4f5f6;
}

.drawer-nav-items {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;

  .drawer-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 20px;
    font-size: 14px;
    font-weight: 500;
    color: #000000;
    text-decoration: none;
    background: none;
    border: none;
    border-bottom: 1px solid #f2f4f5;
    font-family: inherit;
    text-align: left;
    cursor: pointer;
    width: 100%;

    &.strong-item {
      font-weight: 700;
    }

    &.sale-item {
      font-weight: 700;
      color: #e32b2b;
    }

    &.sub-item {
      padding-left: 28px;
    }

    .badge {
      background: #000000;
      color: #ffffff;
      font-size: 11px;
      font-weight: 700;
      padding: 2px 7px;
    }
  }
}

.drawer-footer-row {
  padding: 16px 20px;
  border-top: 1px solid #eceff1;

  .btn-drawer-logout {
    width: 100%;
    height: 44px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    background: #ffffff;
    color: #000000;
    border: 1px solid #000000;
    font-family: inherit;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.1em;
    cursor: pointer;
  }
}

.dialog-tip {
  margin: 0 0 14px;
  font-size: 13px;
  color: #555555;
}

/* ================================================================ */
/* EXACT LIVE ADIDAS 390px "adhaclub" RIGHT SLIDE-OVER MEMBER DRAWER */
/* ================================================================ */
.adi-club-scrim {
  position: fixed;
  inset: 0;
  z-index: 2200;
  background: rgba(0, 0, 0, 0.48);
  display: flex;
  justify-content: flex-end;
}

.adi-club-drawer {
  width: 390px;
  max-width: 100vw;
  height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.2);
}

.club-drawer-topbar {
  height: 60px;
  padding: 0 20px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;

  .club-wordmark {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 26px;
    font-weight: 900;
    font-style: italic;
    letter-spacing: -0.02em;
    color: #000000;
  }

  .club-top-icons {
    display: flex;
    align-items: center;
    gap: 10px;

    .club-icon-btn {
      width: 34px;
      height: 34px;
      background: transparent;
      border: none;
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
        background: #eceff1;
      }
    }
  }
}

.club-cream-hero {
  background: #f8f8f0;
  padding: 18px 20px 14px;
  flex-shrink: 0;

  .club-points-row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 18px;

    .club-star-badge {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: #000000;
      color: #ffffff;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      flex-shrink: 0;
    }

    .club-pts-text {
      display: flex;
      flex-direction: column;
      line-height: 1.15;

      .pts-num {
        font-size: 20px;
        font-weight: 800;
        color: #000000;
      }

      .pts-sub {
        font-size: 12px;
        color: #555555;
      }
    }

    .club-user-pill {
      margin-left: auto;
      font-size: 12px;
      font-weight: 700;
      color: #000000;
      max-width: 150px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .club-arrow-nav {
    display: flex;
    flex-direction: column;
    gap: 2px;

    .club-nav-row {
      width: 100%;
      padding: 9px 0;
      background: transparent;
      border: none;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-family: inherit;
      font-size: 13px;
      font-weight: 800;
      letter-spacing: 0.08em;
      color: #000000;
      cursor: pointer;

      .row-cnt-tag {
        font-style: normal;
        margin-left: 6px;
        padding: 1px 6px;
        background: #427f5c;
        color: #ffffff;
        font-size: 11px;
      }

      .row-arrow {
        font-size: 18px;
        line-height: 1;
      }

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.club-drawer-tabs {
  display: flex;
  border-bottom: 1px solid #d9dbdd;
  padding: 0 20px;
  gap: 20px;
  background: #ffffff;
  flex-shrink: 0;

  .club-tab-btn {
    padding: 14px 0;
    background: transparent;
    border: none;
    border-bottom: 2.5px solid transparent;
    font-family: inherit;
    font-size: 12.5px;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: #767677;
    cursor: pointer;

    &.active {
      color: #000000;
      border-bottom-color: #000000;
    }
  }
}

.club-drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.club-promo-card {
  position: relative;
  border: 1px solid #d9dbdd;
  padding: 18px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  background: #ffffff;
  transition: border-color 0.14s ease;

  &:hover {
    border-color: #000000;
  }

  &.green-framed {
    border-color: #427f5c;
    margin-top: 8px;
  }

  .green-pts-pill {
    position: absolute;
    top: -11px;
    left: 14px;
    background: #427f5c;
    color: #ffffff;
    font-size: 10.5px;
    font-weight: 800;
    letter-spacing: 0.08em;
    padding: 3px 8px;
  }

  .card-copy {
    strong {
      display: block;
      font-size: 14px;
      font-weight: 700;
      color: #000000;
      margin-bottom: 5px;
    }

    p {
      margin: 0;
      font-size: 13px;
      line-height: 1.45;
      color: #555555;
    }
  }

  .card-arrow {
    font-size: 18px;
    color: #000000;
    flex-shrink: 0;
  }
}

.club-benefits-box {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #eceff1;

  h4 {
    font-size: 14.5px;
    font-weight: 700;
    margin: 0 0 10px;
    color: #000000;
  }

  ul {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;
    font-size: 13px;
    color: #000000;
  }
}

.club-vouchers-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .club-voucher-card {
    border: 1px solid #000000;
    padding: 14px 16px;

    .cv-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;

      .cv-code {
        font-family: monospace;
        font-size: 15px;
        font-weight: 800;
      }

      .cv-badge {
        font-size: 10px;
        font-weight: 800;
        background: #427f5c;
        color: #ffffff;
        padding: 2px 6px;
      }
    }

    .cv-desc {
      margin: 0 0 10px;
      font-size: 13px;
      color: #333333;
    }
  }
}

.btn-cv-use {
  background: #000000;
  color: #ffffff;
  border: 1px solid #000000;
  padding: 8px 14px;
  font-family: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.club-no-rewards {
  p {
    font-size: 13.5px;
    color: #555555;
    line-height: 1.5;
    margin: 0 0 14px;
  }
}

.club-drawer-footer {
  padding: 16px 20px;
  border-top: 1px solid #d9dbdd;
  flex-shrink: 0;

  .btn-club-logout {
    width: 100%;
    height: 44px;
    padding: 0 16px;
    background: #ffffff;
    color: #000000;
    border: 1px solid #000000;
    font-family: inherit;
    font-size: 12.5px;
    font-weight: 800;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;

    &:hover {
      background: #000000;
      color: #ffffff;
    }
  }
}

.club-drawer-fade-enter-active,
.club-drawer-fade-leave-active {
  transition: opacity 0.2s ease;
}
.club-drawer-fade-enter-from,
.club-drawer-fade-leave-to {
  opacity: 0;
}
</style>
