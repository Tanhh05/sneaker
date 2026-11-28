<template>
  <div class="store-catalog">
    <!-- Exact Live 2026 Adidas PLP Header -->
    <section class="adi-plp-header">
      <div class="adi-plp-container">
        <nav class="adi-breadcrumb" aria-label="Breadcrumb">
          <button type="button" class="bc-back-btn" @click="router.back()">
            <span class="bc-back-icon">↩</span>
            <span class="bc-back-text">{{ isEn ? 'Back' : 'Trở lại' }}</span>
          </button>
          <router-link :to="lp('')">{{ t('catalog.home') }}</router-link>
          <span class="sep">/</span>
          <span class="current">
            {{ isBlackFridayView ? 'Black Friday' : t('catalog.shoes') }}
          </span>
          <template v-if="filters.brandId">
            <span class="sep">/</span>
            <span class="current">{{ getBrandName(filters.brandId) }}</span>
          </template>
          <template v-if="filters.categoryId">
            <span class="sep">/</span>
            <span class="current">{{ getCategoryName(filters.categoryId) }}</span>
          </template>
        </nav>

        <div class="plp-title-wrap">
          <h1 class="plp-headline" :class="{ 'bf-headline': isBlackFridayView }">
            {{ activeHeadingTitle }}<sup class="plp-count-sup">{{ totalElements }}</sup>
          </h1>
        </div>

        <p v-if="!isBlackFridayView" class="plp-description">
          {{ t('catalog.desc') }}
        </p>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- EXACT LIVE ADIDAS COBALT-BAND 7-CARD VISUAL COLLECTION CAROUSEL  -->
    <!-- Matches adidas.com.vn/vi/black_friday pixel-for-pixel            -->
    <!-- ================================================================ -->
    <section class="adi-cobalt-carousel-section" :class="{ 'is-plain': !isBlackFridayView }">
      <div class="cobalt-backdrop-band" v-if="isBlackFridayView"></div>

      <div class="adi-plp-container carousel-stage-wrap">
        <div ref="catRailRef" class="adi-cat-cards-rail" @scroll="handleRailScroll">
          <article
            v-for="card in visualCollectionCards"
            :key="card.key"
            class="adi-cat-visual-card"
            :class="{ active: isCardActive(card) }"
            @click="handleVisualCardClick(card)"
          >
            <!-- Cobalt Promo Graphic Tile (Cards 6 & 7 like live adidas) -->
            <div v-if="card.variant === 'cobalt'" class="cat-media-box cobalt-typo">
              <span class="cobalt-typo-text">{{ card.graphicText }}</span>
            </div>
            <!-- Photo Tile -->
            <div v-else class="cat-media-box">
              <img :src="card.image" :alt="card.label" loading="lazy" />
            </div>

            <div class="cat-label-foot">
              <span class="cat-label-link">{{ card.label }}</span>
            </div>
          </article>
        </div>

        <!-- Floating Sharp 40x40 Right Arrow Button -->
        <button
          type="button"
          class="cat-rail-arrow right"
          @click="scrollCatRail(1)"
          :aria-label="isEn ? 'Next' : 'Tiếp theo'"
        >
          →
        </button>

        <!-- Minimal Centered Carousel Progress Dashes -->
        <div class="cat-rail-dots" v-if="isBlackFridayView">
          <span class="rail-dash" :class="{ active: railPage === 0 }"></span>
          <span class="rail-dash" :class="{ active: railPage === 1 }"></span>
        </div>
      </div>
    </section>

    <!-- Exact Live Adidas Action Bar: ONLY Clean Right-Aligned "Lọc & Sắp xếp" Button -->
    <section class="adi-plp-toolbar-wrap">
      <div class="adi-plp-container adi-plp-toolbar">
        <!-- Left: Active Filter Chips (only shown when user filters) -->
        <div class="toolbar-active-chips">
          <button v-if="filters.keyword" type="button" class="active-chip" @click="clearKeyword">
            <span class="chip-x">×</span>
            <span>"{{ filters.keyword }}"</span>
          </button>
          <button v-if="filters.brandId" type="button" class="active-chip" @click="selectBrand(null)">
            <span class="chip-x">×</span>
            <span>{{ getBrandName(filters.brandId) }}</span>
          </button>
          <button v-if="filters.categoryId" type="button" class="active-chip" @click="selectCategory(null)">
            <span class="chip-x">×</span>
            <span>{{ getCategoryName(filters.categoryId) }}</span>
          </button>
          <button
            v-if="filters.brandId || filters.categoryId || filters.keyword"
            type="button"
            class="clear-all-inline"
            @click="resetFilters"
          >
            {{ t('catalog.clearAll') }}
          </button>
        </div>

        <!-- Right: Exact Adidas "Lọc & Sắp xếp" Trigger -->
        <button
          type="button"
          class="btn-adi-filter-sort"
          @click="filterDrawerOpen = true"
        >
          <span>{{ isEn ? 'Filter & Sort' : 'Lọc & Sắp xếp' }}</span>
          <span class="filter-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <line x1="3" y1="7" x2="21" y2="7"></line>
              <rect x="7" y="4.5" width="4" height="5" fill="#ffffff" stroke="currentColor" stroke-width="1.6"></rect>
              <line x1="3" y1="17" x2="21" y2="17"></line>
              <rect x="13" y="14.5" width="4" height="5" fill="#ffffff" stroke="currentColor" stroke-width="1.6"></rect>
            </svg>
            <span v-if="activeFilterCount > 0" class="filter-count-square">{{ activeFilterCount }}</span>
          </span>
        </button>
      </div>
    </section>

    <!-- Main Full-Width 4-Column Adidas PLP Grid (Aligned to 6.2vw Gutters) -->
    <div class="adi-plp-container catalog-main-wrap">
      <!-- Skeleton Loading -->
      <div v-if="loading" class="products-grid-skeleton">
        <div class="card-skeleton" v-for="i in 8" :key="i"></div>
      </div>

      <!-- Empty Results -->
      <div v-else-if="products.length === 0" class="empty-results-box">
        <h3 class="empty-title">{{ t('catalog.emptyTitle') }}</h3>
        <p class="empty-desc">{{ t('catalog.emptyDesc') }}</p>
        <button type="button" class="adi-btn-offset" @click="resetFilters">
          <span>{{ t('catalog.resetAll') }}</span>
          <span>→</span>
        </button>
      </div>

      <!-- Exact 4-Column 6px-Gap Adidas Product Grid -->
      <div v-else class="products-grid">
        <article
          v-for="product in sortedProducts"
          :key="product.id"
          class="adi-product-card"
          @click="viewProduct(product.id)"
        >
          <div class="adi-card-media">
            <img
              :src="product.thumbnailUrl || (product.imageUrls && product.imageUrls[0]) || FALLBACK_IMG"
              :alt="product.productName"
              class="adi-card-img"
              loading="lazy"
            />

            <button
              type="button"
              class="adi-wishlist-btn"
              :class="{ active: wishlistStore.isFavorite(product.id) }"
              @click.stop="toggleWishlist(product)"
              :title="wishlistStore.isFavorite(product.id) ? (isEn ? 'Remove from wishlist' : 'Bỏ khỏi yêu thích') : (isEn ? 'Save to wishlist' : 'Lưu vào yêu thích')"
              :aria-label="t('account.wishlist')"
            >
              <svg
                viewBox="0 0 24 24"
                :fill="wishlistStore.isFavorite(product.id) ? '#000000' : 'none'"
                stroke="#000000"
                stroke-width="1.5"
              >
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
            </button>
          </div>

          <div class="adi-card-details">
            <div class="adi-card-price">{{ formatCurrency(product.salePrice) }}</div>
            <h3 class="adi-card-title">{{ product.productName }}</h3>
            <p class="adi-card-category">{{ product.brandName || 'ADHA' }}</p>
            <p class="adi-card-meta-sub">{{ product.categoryName || 'Originals' }}</p>
          </div>
        </article>
      </div>

      <!-- Sharp Adidas Pagination -->
      <div class="pagination-footer" v-if="totalPages > 1">
        <button
          type="button"
          class="pag-nav-btn"
          :disabled="filters.page === 0"
          @click="handlePageChange(filters.page)"
        >
          {{ t('catalog.prevPage') }}
        </button>
        <div class="pag-numbers">
          <button
            v-for="p in totalPages"
            :key="p"
            type="button"
            class="pag-num-btn"
            :class="{ active: filters.page === p - 1 }"
            @click="handlePageChange(p)"
          >
            {{ p }}
          </button>
        </div>
        <button
          type="button"
          class="pag-nav-btn"
          :disabled="filters.page >= totalPages - 1"
          @click="handlePageChange(filters.page + 2)"
        >
          {{ t('catalog.nextPage') }}
        </button>
      </div>
    </div>

    <!-- ================================================================ -->
    <!-- EXACT LIVE ADIDAS 430px RIGHT SLIDE-OVER "LỌC & SẮP XẾP" DRAWER  -->
    <!-- ================================================================ -->
    <transition name="adi-drawer-fade">
      <div v-if="filterDrawerOpen" class="adi-filter-scrim" @click.self="filterDrawerOpen = false">
        <aside class="adi-filter-drawer" role="dialog" :aria-label="isEn ? 'Filter & Sort' : 'Lọc & Sắp xếp'">
          <!-- Drawer Top Header -->
          <div class="drawer-head">
            <h2 class="drawer-title">{{ isEn ? 'FILTER & SORT' : 'LỌC & SẮP XẾP' }}</h2>
            <div class="drawer-head-right">
              <button type="button" class="btn-drawer-clear" @click="resetFilters">
                {{ isEn ? 'Clear all' : 'Xoá tất cả' }}
              </button>
              <button type="button" class="btn-drawer-close" @click="filterDrawerOpen = false" aria-label="Close">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>

          <!-- Scrollable Accordions Body -->
          <div class="drawer-scroll-body">
            <!-- Applied Filters Chips -->
            <div class="drawer-section applied-box" v-if="activeFilterCount > 0">
              <div class="applied-label">{{ isEn ? 'Applied filters' : 'Bộ lọc được áp dụng' }}</div>
              <div class="applied-chips-wrap">
                <button v-if="filters.keyword" type="button" class="applied-chip" @click="clearKeyword">
                  <span>×</span><span>"{{ filters.keyword }}"</span>
                </button>
                <button v-if="filters.brandId" type="button" class="applied-chip" @click="selectBrand(null)">
                  <span>×</span><span>{{ getBrandName(filters.brandId) }}</span>
                </button>
                <button v-if="filters.categoryId" type="button" class="applied-chip" @click="selectCategory(null)">
                  <span>×</span><span>{{ getCategoryName(filters.categoryId) }}</span>
                </button>
                <button v-if="sortBy !== 'newest'" type="button" class="applied-chip" @click="sortBy = 'newest'">
                  <span>×</span><span>{{ sortBy === 'price_asc' ? t('catalog.sortAsc') : t('catalog.sortDesc') }}</span>
                </button>
              </div>
            </div>

            <!-- Accordion 1: Sắp xếp theo -->
            <div class="drawer-accordion">
              <button type="button" class="acc-trigger" @click="accOpen.sort = !accOpen.sort">
                <span>{{ isEn ? 'Sort by' : 'Sắp xếp theo' }}</span>
                <svg class="acc-chev" :class="{ open: accOpen.sort }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div v-if="accOpen.sort" class="acc-panel sort-radio-list">
                <label class="adi-radio-row" :class="{ selected: sortBy === 'price_asc' }" @click="sortBy = 'price_asc'">
                  <span class="radio-ring" :class="{ checked: sortBy === 'price_asc' }"><span class="radio-dot"></span></span>
                  <span>{{ isEn ? 'Price (low - high)' : 'Giá (thấp - cao)' }}</span>
                </label>
                <label class="adi-radio-row" :class="{ selected: sortBy === 'newest' }" @click="sortBy = 'newest'">
                  <span class="radio-ring" :class="{ checked: sortBy === 'newest' }"><span class="radio-dot"></span></span>
                  <span>{{ isEn ? 'Newest first' : 'Mới nhất trước' }}</span>
                </label>
                <label class="adi-radio-row" :class="{ selected: sortBy === 'price_desc' }" @click="sortBy = 'price_desc'">
                  <span class="radio-ring" :class="{ checked: sortBy === 'price_desc' }"><span class="radio-dot"></span></span>
                  <span>{{ isEn ? 'Price (high - low)' : 'Giá (cao - thấp)' }}</span>
                </label>
              </div>
            </div>

            <!-- Accordion 2: Tìm theo tên / SKU -->
            <div class="drawer-accordion">
              <button type="button" class="acc-trigger" @click="accOpen.search = !accOpen.search">
                <span>{{ t('catalog.searchLabel') }}</span>
                <svg class="acc-chev" :class="{ open: accOpen.search }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div v-if="accOpen.search" class="acc-panel">
                <div class="drawer-search-box">
                  <input
                    v-model="filters.keyword"
                    type="text"
                    :placeholder="t('catalog.searchPlaceholder')"
                    class="drawer-text-input"
                    @input="handleFilterChange"
                  />
                  <button v-if="filters.keyword" type="button" class="drawer-input-clear" @click="clearKeyword">✕</button>
                </div>
              </div>
            </div>

            <!-- Accordion 3: Thương hiệu -->
            <div class="drawer-accordion">
              <button type="button" class="acc-trigger" @click="accOpen.brand = !accOpen.brand">
                <span>{{ t('catalog.brandsLabel') }} <small class="acc-cnt">[{{ brands.length }}]</small></span>
                <svg class="acc-chev" :class="{ open: accOpen.brand }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div v-if="accOpen.brand" class="acc-panel">
                <input
                  v-model="brandQuickQuery"
                  type="text"
                  :placeholder="isEn ? 'Quick filter brands...' : 'Lọc nhanh thương hiệu...'"
                  class="drawer-mini-search"
                />
                <div class="drawer-check-list">
                  <button
                    v-if="!brandQuickQuery.trim()"
                    type="button"
                    class="drawer-check-row"
                    :class="{ active: filters.brandId === null }"
                    @click="selectBrand(null)"
                  >
                    <span class="chk-box"></span>
                    <span>{{ t('catalog.allBrands') }}</span>
                  </button>
                  <button
                    v-for="b in filteredBrands"
                    :key="b.id"
                    type="button"
                    class="drawer-check-row"
                    :class="{ active: filters.brandId === b.id }"
                    @click="selectBrand(b.id)"
                  >
                    <span class="chk-box"></span>
                    <span>{{ b.brandName }}</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Accordion 4: Dòng sản phẩm -->
            <div class="drawer-accordion">
              <button type="button" class="acc-trigger" @click="accOpen.category = !accOpen.category">
                <span>{{ t('catalog.categoriesLabel') }} <small class="acc-cnt">[{{ categories.length }}]</small></span>
                <svg class="acc-chev" :class="{ open: accOpen.category }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div v-if="accOpen.category" class="acc-panel">
                <input
                  v-model="categoryQuickQuery"
                  type="text"
                  :placeholder="isEn ? 'Quick filter categories...' : 'Lọc nhanh dòng sản phẩm...'"
                  class="drawer-mini-search"
                />
                <div class="drawer-cat-pills">
                  <button
                    v-if="!categoryQuickQuery.trim()"
                    type="button"
                    class="drawer-pill"
                    :class="{ active: filters.categoryId === null }"
                    @click="selectCategory(null)"
                  >
                    {{ t('catalog.all') }}
                  </button>
                  <button
                    v-for="c in filteredCategories"
                    :key="c.id"
                    type="button"
                    class="drawer-pill"
                    :class="{ active: filters.categoryId === c.id }"
                    @click="selectCategory(c.id)"
                  >
                    {{ c.categoryName }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Sticky Bottom Apply CTA Bar -->
          <div class="drawer-footer-bar">
            <div class="found-count-text">
              {{ isEn ? `Found ${totalElements} products` : `Tìm thấy ${totalElements} sản phẩm` }}
            </div>
            <button type="button" class="btn-drawer-apply" @click="filterDrawerOpen = false">
              <span>{{ isEn ? 'Show products' : 'Hiển thị sản phẩm' }}</span>
              <span class="arrow">→</span>
            </button>
          </div>
        </aside>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import storeService from '@/services/storeService'
import { formatCurrency } from '@/utils/formatters'
import { useWishlistStore } from '@/stores/wishlist'
import { useStoreI18n } from '@/composables/useStoreI18n'

const router = useRouter()
const route = useRoute()
const wishlistStore = useWishlistStore()
const { t, lp, isEn } = useStoreI18n()

const FALLBACK_IMG =
  'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=600&q=80'

const loading = ref(false)
const brands = ref([])
const categories = ref([])
const brandQuickQuery = ref('')
const categoryQuickQuery = ref('')
const products = ref([])
const showcasePool = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const sortBy = ref(route.query.sort === 'price_asc' || route.query.sort === 'price_desc' ? route.query.sort : 'newest')
const filterDrawerOpen = ref(false)
const catRailRef = ref(null)
const railPage = ref(0)

const accOpen = reactive({
  sort: true,
  search: true,
  brand: true,
  category: true
})

const normalizeVi = (str) =>
  String(str ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'd')
    .toLowerCase()
    .trim()

const filteredBrands = computed(() => {
  const q = normalizeVi(brandQuickQuery.value)
  if (!q) return brands.value
  return brands.value.filter((b) => normalizeVi(b.brandName).includes(q))
})

const filteredCategories = computed(() => {
  const q = normalizeVi(categoryQuickQuery.value)
  if (!q) return categories.value
  return categories.value.filter((c) => normalizeVi(c.categoryName).includes(q))
})

const filters = reactive({
  keyword: route.query.q || '',
  brandId: route.query.brand ? Number(route.query.brand) : null,
  categoryId: route.query.category ? Number(route.query.category) : null,
  page: 0,
  size: 16
})

const isBlackFridayView = computed(
  () => !filters.keyword && !filters.brandId && !filters.categoryId && route.query.sort === 'price_asc'
)

const activeFilterCount = computed(() => {
  let n = 0
  if (filters.keyword) n++
  if (filters.brandId) n++
  if (filters.categoryId) n++
  if (sortBy.value !== 'newest') n++
  return n
})

/* Exact 7-Card Visual Category Carousel (mirrors adidas.com.vn/vi/black_friday) */
const visualCollectionCards = computed(() => {
  const pool = showcasePool.value.length ? showcasePool.value : products.value
  const imgAt = (idx, fallback) =>
    pool[idx]?.thumbnailUrl || (pool[idx]?.imageUrls && pool[idx].imageUrls[0]) || fallback

  const findCat = (kw) =>
    categories.value.find((c) => normalizeVi(c.categoryName).includes(kw))?.id || null
  const findBrand = (kw) =>
    brands.value.find((b) => normalizeVi(b.brandName).includes(kw))?.id || null

  return [
    {
      key: 'originals-icons',
      variant: 'photo',
      label: isEn.value ? 'ORIGINALS ICONS' : 'ORIGINALS ICONS',
      image: imgAt(0, 'https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?auto=format&fit=crop&w=500&q=80'),
      action: 'brand',
      targetId: findBrand('adidas') || brands.value[0]?.id || null
    },
    {
      key: 'daily-sneakers',
      variant: 'photo',
      label: isEn.value ? 'EVERYDAY SNEAKERS' : 'GIÀY SNEAKER HẰNG NGÀY',
      image: imgAt(1, 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=500&q=80'),
      action: 'category',
      targetId: findCat('lifestyle') || categories.value[0]?.id || null
    },
    {
      key: 'running-shoes',
      variant: 'photo',
      label: isEn.value ? 'RUNNING SHOES' : 'GIÀY CHẠY BỘ',
      image: imgAt(2, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=500&q=80'),
      action: 'category',
      targetId: findCat('running') || findCat('chay') || categories.value[1]?.id || null
    },
    {
      key: 'men-edit',
      variant: 'photo',
      label: isEn.value ? 'MEN' : 'ĐỒ NAM',
      image: imgAt(3, 'https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&w=500&q=80'),
      action: 'brand',
      targetId: findBrand('jordan') || findBrand('nike') || brands.value[1]?.id || null
    },
    {
      key: 'women-edit',
      variant: 'photo',
      label: isEn.value ? 'WOMEN' : 'ĐỒ NỮ',
      image: imgAt(4, 'https://images.unsplash.com/photo-1551107696-a4b0c5a0d9a2?auto=format&fit=crop&w=500&q=80'),
      action: 'brand',
      targetId: findBrand('converse') || findBrand('new balance') || brands.value[2]?.id || null
    },
    {
      key: 'hot-deals',
      variant: 'cobalt',
      graphicText: isEn.value ? 'HOT DEALS,\nBIG SAVES' : 'DEAL HOT,\nGIÁ HỜI',
      label: isEn.value ? 'HOT DEALS, BIG SAVES' : 'DEAL HOT, GIÁ HỜI',
      action: 'sort',
      sortVal: 'price_asc'
    },
    {
      key: 'best-sellers',
      variant: 'cobalt',
      graphicText: 'B E S T\nSELLERS',
      label: isEn.value ? 'BLACK FRIDAY MUST-HAVES' : 'BLACK FRIDAY MUST-HAVES',
      action: 'sort',
      sortVal: 'newest'
    },
    {
      key: 'archive-all',
      variant: 'photo',
      label: isEn.value ? 'ALL ARCHIVE' : 'TẤT CẢ SNEAKER',
      image: imgAt(5, 'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&w=500&q=80'),
      action: 'reset'
    }
  ]
})

const isCardActive = (card) => {
  if (card.action === 'brand' && card.targetId) return filters.brandId === card.targetId
  if (card.action === 'category' && card.targetId) return filters.categoryId === card.targetId
  return false
}

const handleVisualCardClick = (card) => {
  if (card.action === 'brand' && card.targetId) {
    filters.categoryId = null
    selectBrand(filters.brandId === card.targetId ? null : card.targetId)
  } else if (card.action === 'category' && card.targetId) {
    filters.brandId = null
    selectCategory(filters.categoryId === card.targetId ? null : card.targetId)
  } else if (card.action === 'sort') {
    filters.brandId = null
    filters.categoryId = null
    sortBy.value = card.sortVal
    filters.page = 0
    loadProducts()
  } else {
    resetFilters()
  }
}

const scrollCatRail = (dir = 1) => {
  const el = catRailRef.value
  if (!el) return
  const maxScroll = el.scrollWidth - el.clientWidth
  if (dir > 0 && el.scrollLeft >= maxScroll - 8) {
    el.scrollTo({ left: 0, behavior: 'smooth' })
  } else {
    el.scrollBy({ left: dir * Math.max(320, el.clientWidth * 0.6), behavior: 'smooth' })
  }
}

const handleRailScroll = () => {
  const el = catRailRef.value
  if (!el) return
  railPage.value = el.scrollLeft > 40 ? 1 : 0
}

const toggleWishlist = (product) => {
  const isAdded = wishlistStore.toggleFavorite(product)
  if (isAdded) {
    ElMessage.success(
      isEn.value
        ? `Added "${product.productName}" to wishlist`
        : `Đã thêm "${product.productName}" vào danh sách yêu thích`
    )
  } else {
    ElMessage.info(
      isEn.value
        ? `Removed "${product.productName}" from wishlist`
        : `Đã bỏ "${product.productName}" khỏi danh sách yêu thích`
    )
  }
}

const activeHeadingTitle = computed(() => {
  if (filters.keyword) return `${t('catalog.titleSearch')} "${filters.keyword}"`
  if (filters.brandId) {
    const prefix = t('catalog.titleBrandPrefix')
    return prefix ? `${prefix} ${getBrandName(filters.brandId)}` : `${getBrandName(filters.brandId)} SNEAKERS`
  }
  if (filters.categoryId) return getCategoryName(filters.categoryId)
  if (route.query.sort === 'price_asc') {
    return isEn.value
      ? 'EXCLUSIVE BLACK FRIDAY DEALS: UP TO 50% OFF SELECTED STYLES + EXTRA 10% AT CHECKOUT'
      : 'ƯU ĐÃI ĐỘC QUYỀN BLACK FRIDAY: GIẢM ĐẾN 50% CHO CÁC SẢN PHẨM ĐƯỢC CHỌN + GIẢM THÊM 10% KHI THANH TOÁN'
  }
  if (route.query.sort === 'newest') return t('catalog.titleNewest')
  return t('catalog.titleAll')
})

const sortedProducts = computed(() => {
  const list = [...products.value]
  if (sortBy.value === 'price_asc') {
    list.sort((a, b) => (a.salePrice || 0) - (b.salePrice || 0))
  } else if (sortBy.value === 'price_desc') {
    list.sort((a, b) => (b.salePrice || 0) - (a.salePrice || 0))
  }
  return list
})

const getBrandName = (id) => {
  const b = brands.value.find((item) => item.id === id)
  return b ? b.brandName : (isEn.value ? 'Brand' : 'Thương hiệu')
}

const getCategoryName = (id) => {
  const c = categories.value.find((item) => item.id === id)
  return c ? c.categoryName : (isEn.value ? 'Category' : 'Dòng sản phẩm')
}

const loadMeta = async () => {
  try {
    const [bRes, cRes, poolRes] = await Promise.all([
      storeService.getBrands(),
      storeService.getCategories(),
      storeService.getProducts({ page: 0, size: 12 }).catch(() => null)
    ])
    brands.value = bRes?.data || []
    categories.value = cRes?.data || []
    showcasePool.value = poolRes?.data?.content || []
  } catch (err) {
    console.error('Lỗi nạp metadata:', err)
  }
}

const loadProducts = async () => {
  loading.value = true
  try {
    const params = {
      keyword: filters.keyword || null,
      brandId: filters.brandId || null,
      categoryId: filters.categoryId || null,
      page: filters.page,
      size: filters.size
    }
    const res = await storeService.getProducts(params)
    const pageData = res?.data || {}
    products.value = pageData.content || []
    totalElements.value = pageData.totalElements || 0
    totalPages.value = pageData.totalPages || 0
  } catch (err) {
    console.error('Lỗi nạp danh sách sản phẩm:', err)
  } finally {
    loading.value = false
  }
}

const selectBrand = (brandId) => {
  filters.brandId = brandId
  filters.page = 0
  loadProducts()
}

const selectCategory = (catId) => {
  filters.categoryId = catId
  filters.page = 0
  loadProducts()
}

const clearKeyword = () => {
  filters.keyword = ''
  filters.page = 0
  loadProducts()
}

const handleFilterChange = () => {
  filters.page = 0
  loadProducts()
}

const handlePageChange = (p) => {
  filters.page = p - 1
  loadProducts()
  window.scrollTo({ top: 120, behavior: 'smooth' })
}

const resetFilters = () => {
  filters.keyword = ''
  filters.brandId = null
  filters.categoryId = null
  sortBy.value = route.query.sort === 'price_asc' ? 'price_asc' : 'newest'
  filters.page = 0
  loadProducts()
}

const viewProduct = (id) => {
  router.push(lp(`product/${id}`))
}

watch(
  () => route.query,
  (newQ) => {
    filters.brandId = newQ.brand ? Number(newQ.brand) : null
    filters.categoryId = newQ.category ? Number(newQ.category) : null
    filters.keyword = newQ.q || ''
    if (newQ.sort === 'price_asc' || newQ.sort === 'price_desc' || newQ.sort === 'newest') {
      sortBy.value = newQ.sort
    }
    filters.page = 0
    loadProducts()
  }
)

onMounted(() => {
  loadMeta()
  loadProducts()
})
</script>

<style scoped lang="scss">
.store-catalog {
  background: #ffffff;
  color: #000000;
  padding-bottom: 96px;
}

/* Aligned to exact 6.2vw adidas horizontal rhythm */
.adi-plp-container {
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

/* ============ EXACT ADIDAS PLP HEADER (↩ Trở lại + 2-LINE BLACK FRIDAY H1) ============ */
.adi-plp-header {
  padding: 24px 0 18px;
  background: #ffffff;

  .adi-breadcrumb {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13.5px;
    color: #000000;
    margin-bottom: 22px;
    flex-wrap: wrap;

    .bc-back-btn {
      background: none;
      border: none;
      padding: 0;
      margin-right: 6px;
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-family: inherit;
      font-size: 13.5px;
      font-weight: 700;
      color: #000000;
      cursor: pointer;

      .bc-back-text {
        text-decoration: underline;
        text-underline-offset: 3px;
      }

      &:hover .bc-back-text {
        background: #000000;
        color: #ffffff;
        text-decoration: none;
      }
    }

    a {
      color: #000000;
      text-decoration: underline;
      text-underline-offset: 3px;

      &:hover {
        background: #000000;
        color: #ffffff;
        text-decoration: none;
      }
    }

    .sep {
      color: #767677;
    }

    .current {
      color: #000000;
    }
  }

  .plp-title-wrap {
    margin-bottom: 6px;

    .plp-headline {
      font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
      font-size: 46px;
      font-style: normal;
      font-weight: 800;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      line-height: 1.08;
      margin: 0;
      color: #000000;

      &.bf-headline {
        font-size: 40px;
        letter-spacing: 0.035em;
        line-height: 1.12;
        max-width: 1120px;

        @media (max-width: 900px) {
          font-size: 30px;
        }
      }

      @media (max-width: 640px) {
        font-size: 30px;
      }
    }

    .plp-count-sup {
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
      font-size: 13px;
      font-weight: 700;
      color: #000000;
      margin-left: 8px;
      vertical-align: super;
      letter-spacing: 0;
    }
  }

  .plp-description {
    font-size: 14.5px;
    color: #000000;
    max-width: 740px;
    line-height: 1.55;
    margin: 8px 0 0;
  }
}

/* ================================================================ */
/* EXACT LIVE ADIDAS COBALT-BAND 7-CARD VISUAL COLLECTION CAROUSEL  */
/* ================================================================ */
.adi-cobalt-carousel-section {
  position: relative;
  padding: 4px 0 18px;
  margin-bottom: 18px;
  overflow: hidden;

  &.is-plain {
    padding-bottom: 8px;
    margin-bottom: 12px;
  }

  .cobalt-backdrop-band {
    position: absolute;
    left: 0;
    right: 0;
    top: 38%;
    bottom: 0;
    background:
      linear-gradient(125deg, rgba(255, 255, 255, 0) 64%, rgba(255, 255, 255, 0.22) 64.2%, rgba(255, 255, 255, 0.22) 72%, rgba(255, 255, 255, 0) 72.2%),
      linear-gradient(125deg, rgba(255, 255, 255, 0) 76%, rgba(255, 255, 255, 0.14) 76.2%, rgba(255, 255, 255, 0.14) 84%, rgba(255, 255, 255, 0) 84.2%),
      #36559a;
    z-index: 0;
  }

  .carousel-stage-wrap {
    position: relative;
    z-index: 1;
  }

  .adi-cat-cards-rail {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    scroll-snap-type: x proximity;
    scrollbar-width: none;
    padding-bottom: 2px;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .adi-cat-visual-card {
    flex: 0 0 calc((100% - 48px) / 7);
    min-width: 158px;
    background: #ffffff;
    border: 1px solid #eceff1;
    cursor: pointer;
    scroll-snap-align: start;
    display: flex;
    flex-direction: column;
    transition: border-color 0.14s ease, transform 0.14s ease;

    @media (max-width: 1200px) {
      flex: 0 0 calc((100% - 32px) / 5);
    }

    @media (max-width: 760px) {
      flex: 0 0 154px;
    }

    &:hover,
    &.active {
      border-color: #000000;
    }

    .cat-media-box {
      width: 100%;
      aspect-ratio: 1.32 / 1;
      background: #eceff1;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
      }

      &.cobalt-typo {
        background: #36559a;
        padding: 12px;
        text-align: center;

        .cobalt-typo-text {
          font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
          font-size: 24px;
          font-weight: 800;
          line-height: 1.05;
          letter-spacing: 0.06em;
          color: #ffffff;
          white-space: pre-line;
          text-transform: uppercase;
        }
      }
    }

    .cat-label-foot {
      padding: 14px 10px 16px;
      background: #ffffff;
      flex: 1;
      display: flex;
      align-items: flex-start;

      .cat-label-link {
        font-size: 11.5px;
        font-weight: 800;
        line-height: 1.35;
        letter-spacing: 0.04em;
        color: #000000;
        text-transform: uppercase;
        text-decoration: underline;
        text-underline-offset: 3px;
      }
    }

    &:hover .cat-label-link {
      background: #000000;
      color: #ffffff;
      text-decoration: none;
    }
  }

  .cat-rail-arrow {
    position: absolute;
    right: 6.2vw;
    top: 44%;
    transform: translateY(-50%);
    width: 44px;
    height: 44px;
    background: #ffffff;
    color: #000000;
    border: 1px solid #000000;
    border-radius: 0;
    font-size: 20px;
    line-height: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    z-index: 3;

    @media (max-width: 1024px) {
      right: 24px;
    }

    &:hover {
      background: #000000;
      color: #ffffff;
    }
  }

  .cat-rail-dots {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    margin-top: 14px;

    .rail-dash {
      width: 16px;
      height: 2px;
      background: rgba(0, 0, 0, 0.35);
      transition: background 0.16s ease, height 0.16s ease;

      &.active {
        height: 4px;
        background: #000000;
      }
    }
  }
}

/* ============ TOOLBAR: CLEAN RIGHT-ALIGNED "LỌC & SẮP XẾP" ONLY ============ */
.adi-plp-toolbar-wrap {
  padding: 6px 0 14px;
  background: #ffffff;
}

.adi-plp-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-active-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;

  .active-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 32px;
    padding: 0 12px;
    background: #ffffff;
    border: 1px solid #767677;
    border-radius: 0;
    font-family: inherit;
    font-size: 13px;
    font-weight: 500;
    color: #000000;
    cursor: pointer;

    .chip-x {
      font-size: 15px;
      line-height: 1;
    }

    &:hover {
      border-color: #000000;
      background: #eceff1;
    }
  }

  .clear-all-inline {
    background: none;
    border: none;
    font-family: inherit;
    font-size: 13px;
    font-weight: 600;
    color: #000000;
    text-decoration: underline;
    cursor: pointer;
    margin-left: 6px;
  }
}

.btn-adi-filter-sort {
  margin-left: auto;
  height: 36px;
  padding: 0 10px 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: #ffffff;
  color: #000000;
  border: 1px solid #000000;
  border-radius: 0;
  font-family: inherit;
  font-size: 14.5px;
  font-weight: 700;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.14s ease, color 0.14s ease;

  &:hover {
    background: #000000;
    color: #ffffff;

    .filter-icon-wrap svg rect {
      fill: #000000;
    }
  }

  .filter-icon-wrap {
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: 5px;

    svg {
      width: 18px;
      height: 18px;
    }

    .filter-count-square {
      min-width: 18px;
      height: 18px;
      padding: 0 4px;
      background: #000000;
      color: #ffffff;
      font-size: 11px;
      font-weight: 800;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }
  }
}

/* ============ EXACT 4-COLUMN 6px-GAP ADIDAS PRODUCT GRID ============ */
.products-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px 6px;

  @media (max-width: 1080px) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  @media (max-width: 680px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px 6px;
  }
}

.adi-product-card {
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid transparent;
  cursor: pointer;
  transition: border-color 0.14s ease;

  &:hover {
    border-color: #000000;

    .adi-card-img {
      transform: scale(1.03);
    }
  }

  .adi-card-media {
    position: relative;
    aspect-ratio: 1 / 1;
    background: #eceff1;
    overflow: hidden;

    .adi-card-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: transform 0.3s ease;
    }

    .adi-wishlist-btn {
      position: absolute;
      top: 10px;
      right: 10px;
      width: 34px;
      height: 34px;
      background: transparent;
      border: none;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;

      svg {
        width: 20px;
        height: 20px;
      }
    }
  }

  .adi-card-details {
    padding: 12px 10px 16px;
    display: flex;
    flex-direction: column;
    gap: 3px;

    .adi-card-price {
      font-size: 14px;
      font-weight: 700;
      color: #000000;
      margin-bottom: 4px;
      font-variant-numeric: tabular-nums;
    }

    .adi-card-title {
      font-size: 14px;
      font-weight: 400;
      color: #000000;
      margin: 0;
      line-height: 1.35;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .adi-card-category {
      font-size: 13px;
      color: #767677;
      margin: 2px 0 0;
    }

    .adi-card-meta-sub {
      font-size: 12.5px;
      color: #767677;
      margin: 0;
    }
  }
}

/* Skeleton & Empty State */
.products-grid-skeleton {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px 6px;

  @media (max-width: 1080px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 680px) {
    grid-template-columns: repeat(2, 1fr);
  }

  .card-skeleton {
    aspect-ratio: 4 / 5;
    background: linear-gradient(90deg, #eceff1 25%, #f5f7f8 50%, #eceff1 75%);
    background-size: 200% 100%;
    animation: shimmer 1.4s infinite;
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.empty-results-box {
  padding: 64px 0;
  text-align: left;

  .empty-title {
    font-family: 'Barlow Condensed', sans-serif;
    font-size: 32px;
    font-weight: 800;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    margin: 0 0 10px;
  }

  .empty-desc {
    font-size: 14.5px;
    color: #555555;
    margin: 0 0 24px;
    max-width: 480px;
  }

  .adi-btn-offset {
    position: relative;
    height: 48px;
    padding: 0 24px;
    display: inline-flex;
    align-items: center;
    gap: 14px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    border-radius: 0;
    font-family: inherit;
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0.12em;
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

/* Pagination */
.pagination-footer {
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid #eceff1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;

  .pag-nav-btn {
    height: 40px;
    padding: 0 18px;
    background: #ffffff;
    border: 1px solid #000000;
    border-radius: 0;
    font-family: inherit;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.1em;
    color: #000000;
    cursor: pointer;

    &:hover:not(:disabled) {
      background: #000000;
      color: #ffffff;
    }

    &:disabled {
      border-color: #d3d7da;
      color: #a3a3a3;
      cursor: not-allowed;
    }
  }

  .pag-numbers {
    display: flex;
    gap: 6px;

    .pag-num-btn {
      width: 40px;
      height: 40px;
      background: #ffffff;
      border: 1px solid #d3d7da;
      border-radius: 0;
      font-family: inherit;
      font-size: 13px;
      font-weight: 700;
      color: #000000;
      cursor: pointer;

      &.active {
        background: #000000;
        color: #ffffff;
        border-color: #000000;
      }

      &:hover:not(.active) {
        border-color: #000000;
      }
    }
  }
}

/* ================================================================ */
/* EXACT LIVE ADIDAS 430px RIGHT SLIDE-OVER "LỌC & SẮP XẾP" DRAWER  */
/* ================================================================ */
.adi-filter-scrim {
  position: fixed;
  inset: 0;
  z-index: 2100;
  background: rgba(0, 0, 0, 0.48);
  display: flex;
  justify-content: flex-end;
}

.adi-filter-drawer {
  width: 430px;
  max-width: 100vw;
  height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.18);
}

.drawer-head {
  height: 68px;
  padding: 0 24px;
  border-bottom: 1px solid #d9dbdd;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;

  .drawer-title {
    font-family: 'Barlow Condensed', 'Plus Jakarta Sans', sans-serif;
    font-size: 24px;
    font-weight: 800;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    margin: 0;
    color: #000000;
  }

  .drawer-head-right {
    display: flex;
    align-items: center;
    gap: 18px;

    .btn-drawer-clear {
      background: none;
      border: none;
      font-family: inherit;
      font-size: 14px;
      font-weight: 600;
      color: #000000;
      text-decoration: underline;
      text-underline-offset: 3px;
      cursor: pointer;
    }

    .btn-drawer-close {
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

.drawer-scroll-body {
  flex: 1;
  overflow-y: auto;
}

.applied-box {
  padding: 18px 24px;
  border-bottom: 1px solid #d9dbdd;

  .applied-label {
    font-size: 14px;
    font-weight: 700;
    color: #000000;
    margin-bottom: 12px;
  }

  .applied-chips-wrap {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .applied-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 34px;
    padding: 0 12px;
    background: #ffffff;
    border: 1px solid #767677;
    border-radius: 0;
    font-family: inherit;
    font-size: 13.5px;
    color: #000000;
    cursor: pointer;

    &:hover {
      border-color: #000000;
      background: #eceff1;
    }
  }
}

.drawer-accordion {
  border-bottom: 1px solid #d9dbdd;

  .acc-trigger {
    width: 100%;
    padding: 18px 24px;
    background: #ffffff;
    border: none;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-family: inherit;
    font-size: 15px;
    font-weight: 700;
    color: #000000;
    cursor: pointer;
    text-align: left;

    .acc-cnt {
      font-weight: 500;
      color: #767677;
      margin-left: 4px;
    }

    .acc-chev {
      width: 18px;
      height: 18px;
      transition: transform 0.16s ease;

      &.open {
        transform: rotate(180deg);
      }
    }
  }

  .acc-panel {
    padding: 0 24px 20px;
  }
}

.sort-radio-list {
  display: flex;
  flex-direction: column;
  padding-left: 0 !important;
  padding-right: 0 !important;

  .adi-radio-row {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 13px 24px;
    font-size: 15px;
    color: #000000;
    cursor: pointer;
    transition: background 0.12s ease;

    &:hover,
    &.selected {
      background: #f5f5f5;
    }

    .radio-ring {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      border: 1.5px solid #767677;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      .radio-dot {
        width: 12px;
        height: 12px;
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

.drawer-search-box {
  position: relative;

  .drawer-text-input {
    width: 100%;
    height: 44px;
    padding: 0 34px 0 12px;
    border: 1px solid #767677;
    border-radius: 0;
    background: #ffffff;
    font-family: inherit;
    font-size: 14px;
    color: #000000;
    box-sizing: border-box;
    outline: none;

    &:focus {
      border-color: #000000;
    }
  }

  .drawer-input-clear {
    position: absolute;
    right: 10px;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    cursor: pointer;
    font-size: 14px;
  }
}

.drawer-mini-search {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  margin-bottom: 12px;
  border: 1px solid #d3d7da;
  border-radius: 0;
  background: #f7f8f9;
  font-family: inherit;
  font-size: 13px;
  box-sizing: border-box;
  outline: none;

  &:focus {
    background: #ffffff;
    border-color: #000000;
  }
}

.drawer-check-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 220px;
  overflow-y: auto;

  .drawer-check-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 4px 0;
    background: none;
    border: none;
    font-family: inherit;
    font-size: 14.5px;
    color: #000000;
    cursor: pointer;
    text-align: left;

    .chk-box {
      width: 18px;
      height: 18px;
      border: 1.5px solid #767677;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    &.active {
      font-weight: 700;

      .chk-box {
        background: #000000;
        border-color: #000000;

        &::after {
          content: '✓';
          color: #ffffff;
          font-size: 12px;
          font-weight: 800;
        }
      }
    }
  }
}

.drawer-cat-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .drawer-pill {
    padding: 8px 14px;
    background: #ffffff;
    border: 1px solid #d3d7da;
    border-radius: 0;
    font-family: inherit;
    font-size: 13px;
    font-weight: 600;
    color: #000000;
    cursor: pointer;

    &:hover {
      border-color: #000000;
    }

    &.active {
      background: #000000;
      color: #ffffff;
      border-color: #000000;
    }
  }
}

.drawer-footer-bar {
  padding: 16px 24px 20px;
  border-top: 1px solid #d9dbdd;
  background: #ffffff;
  flex-shrink: 0;

  .found-count-text {
    font-size: 13.5px;
    color: #000000;
    margin-bottom: 12px;
  }

  .btn-drawer-apply {
    position: relative;
    width: 100%;
    height: 48px;
    padding: 0 20px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    border-radius: 0;
    font-family: inherit;
    font-size: 14.5px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: space-between;
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
      font-size: 20px;
      line-height: 1;
    }

    &:hover {
      color: #cccccc;
    }
  }
}

.adi-drawer-fade-enter-active,
.adi-drawer-fade-leave-active {
  transition: opacity 0.2s ease;
}
.adi-drawer-fade-enter-from,
.adi-drawer-fade-leave-to {
  opacity: 0;
}
</style>
