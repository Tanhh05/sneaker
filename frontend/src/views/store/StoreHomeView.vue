<template>
  <div class="store-home">
    <!-- ================================================================ -->
    <!-- SECTION 1: TOP 4-COLUMN BLACK FRIDAY PROMO SPLIT (adidas 2026)   -->
    <!-- Exact mirror of steps/21/media_0.jpg above the fold!             -->
    <!-- ================================================================ -->
    <section class="adi-top-promo-section">
      <div class="adi-container">
        <div class="adi-top-promo-grid">
          <!-- Col 1: Solid Black Campaign Card -->
          <div class="promo-black-card">
            <div class="promo-black-top">
              <h1 class="promo-black-title">{{ t('home.bfTitle') }}</h1>
              <p class="promo-black-desc">{{ t('home.bfDesc') }}</p>
            </div>
            <router-link :to="{ path: lp('shop'), query: { sort: 'price_asc' } }" class="promo-black-cta">
              {{ t('home.bfCta') }}
            </router-link>
          </div>

          <!-- Cols 2–4: Clean Studio #f4f5f6 Product Tiles with Bottom-Left % Pill -->
          <div
            v-for="tile in topPromoTiles"
            :key="tile.key"
            class="promo-studio-card"
            @click="router.push(tile.to)"
          >
            <img :src="tile.image" :alt="tile.label" class="promo-studio-img" />
            <div class="promo-studio-pill">
              <span class="pill-pct">%</span>
              <span class="pill-label">{{ tile.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- SECTION 2: BEST DEALS PORTRAIT 3:4 CAROUSEL (Right-edge bleed)   -->
    <!-- ================================================================ -->
    <section class="adi-deals-strip">
      <div class="adi-bleed-container">
        <div class="adi-deals-rail">
          <article
            v-for="deal in dealCards"
            :key="deal.key"
            class="adi-deal-card"
            @click="router.push(deal.to)"
          >
            <div class="deal-poster" :class="{ 'cobalt-poster': deal.variant === 'cobalt' }">
              <template v-if="deal.variant === 'cobalt'">
                <div class="cobalt-stripes"></div>
                <div class="cobalt-typo">
                  <span class="cobalt-sub">{{ deal.overlaySub }}</span>
                  <strong class="cobalt-big">{{ deal.overlayBig }}</strong>
                </div>
              </template>
              <img v-else :src="deal.image" :alt="deal.title" loading="lazy" />
            </div>
            <h3 class="deal-title">{{ deal.title }}</h3>
            <span class="adi-underline-link">{{ t('home.buyNow') }}</span>
          </article>
        </div>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- SECTION 3: FULL-BLEED EDITORIAL HERO BANNER #1 (1440×620px)      -->
    <!-- Exact mirror of ADIDAS COLLABORATIONS ARCHIVE (steps/28)         -->
    <!-- ================================================================ -->
    <section class="adi-campaign-hero" @click="router.push(lp('shop'))">
      <img
        src="https://images.unsplash.com/photo-1556906781-9a412961c28c?auto=format&fit=crop&w=2000&q=85"
        alt="ADHA Collaborations Archive"
        class="campaign-hero-img"
      />
      <div class="campaign-hero-scrim"></div>
      <div class="adi-container campaign-hero-inner">
        <div class="campaign-hero-stack">
          <h2 class="hero-hl-row">
            <span class="hero-white-hl">{{ t('home.hero1Title') }}</span>
          </h2>
          <p class="hero-sub-row">
            <span class="hero-white-sub">{{ t('home.hero1Sub') }}</span>
          </p>
          <div class="hero-btn-row">
            <router-link :to="lp('shop')" class="adi-hero-btn" @click.stop>
              <span>{{ t('home.hero1Cta') }}</span>
              <span class="adi-arrow">→</span>
            </router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- SECTION 4: TABBED PRODUCT CAROUSEL #1 (2026 Live Adidas Cards)   -->
    <!-- Soft #d9dbdd chips + Price cleanly BELOW 1:1 #eceff1 image!      -->
    <!-- ================================================================ -->
    <section class="adi-carousel-section">
      <div class="adi-container carousel-top-row">
        <div class="adi-chip-tabs">
          <button
            type="button"
            class="adi-chip-btn"
            :class="{ active: activeTab === 'sale' }"
            @click="activeTab = 'sale'"
          >
            {{ t('home.tabSale') }}
          </button>
          <button
            type="button"
            class="adi-chip-btn"
            :class="{ active: activeTab === 'new' }"
            @click="activeTab = 'new'"
          >
            {{ t('home.tabNew') }}
          </button>
          <button
            v-for="b in topFilterBrands"
            :key="b.id"
            type="button"
            class="adi-chip-btn"
            :class="{ active: activeTab === `brand-${b.id}` }"
            @click="activeTab = `brand-${b.id}`"
          >
            {{ b.brandName }}
          </button>
        </div>

        <router-link :to="lp('shop')" class="adi-see-all-link">{{ t('home.seeAll') }}</router-link>
      </div>

      <div class="adi-bleed-container rail-nav-wrap">
        <div v-if="loading" class="adi-skeleton-rail">
          <div class="adi-skeleton-card" v-for="i in 5" :key="i"></div>
        </div>

        <div v-else ref="carouselRef1" class="adi-product-rail">
          <article
            v-for="(product, idx) in displayedProducts"
            :key="product.id"
            class="adi-2026-card"
            @click="viewProduct(product.id)"
          >
            <div class="card-img-stage">
              <img :src="getProductImage(product)" :alt="product.productName" loading="lazy" />
              <button
                type="button"
                class="card-heart-btn"
                @click.stop="toggleWishlist(product)"
                :aria-label="wishlistStore.isFavorite(product.id) ? 'Bỏ yêu thích' : 'Yêu thích'"
              >
                <svg
                  viewBox="0 0 24 24"
                  :fill="wishlistStore.isFavorite(product.id) ? '#000000' : 'none'"
                  stroke="#000000"
                  stroke-width="1.6"
                >
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                </svg>
              </button>
            </div>

            <!-- Exact 2026 Live Adidas Price-Below-Image Layout -->
            <div class="card-info">
              <div class="price-block" v-if="activeTab === 'sale' && idx < 4">
                <div class="price-sale">{{ formatCurrency(product.salePrice) }}</div>
                <div class="price-original-row">
                  <span class="price-strike">{{ formatCurrency(Math.round((product.salePrice || 0) * 1.25)) }}</span>
                  <span class="price-orig-label">{{ t('home.origPrice') }}</span>
                  <span class="price-pct">-20%</span>
                </div>
              </div>
              <div class="price-block" v-else>
                <div class="price-regular">{{ formatCurrency(product.salePrice) }}</div>
              </div>

              <h4 class="card-title">{{ product.productName }}</h4>
              <p class="card-cat">{{ product.brandName || 'Originals' }}</p>
            </div>
          </article>
        </div>

        <button
          type="button"
          class="adi-rail-arrow rail-arrow-next"
          @click="scrollRail(carouselRef1, 1)"
          aria-label="Xem thêm sản phẩm"
        >
          →
        </button>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- SECTION 5: FULL-BLEED EDITORIAL HERO BANNER #2 (1440×600px)      -->
    <!-- Exact mirror of GIÀY SAMBA BALLET (steps/42) with 2 white CTAs   -->
    <!-- ================================================================ -->
    <section class="adi-campaign-hero secondary-hero" @click="router.push(lp('shop'))">
      <img
        src="https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=2000&q=85"
        alt="ORIGINALS STREETWEAR"
        class="campaign-hero-img"
      />
      <div class="campaign-hero-scrim"></div>
      <div class="adi-container campaign-hero-inner">
        <div class="campaign-hero-stack">
          <h2 class="hero-hl-row">
            <span class="hero-white-hl">{{ t('home.hero2Title') }}</span>
          </h2>
          <p class="hero-sub-row">
            <span class="hero-white-sub">{{ t('home.hero2Sub') }}</span>
          </p>
          <div class="hero-btn-row">
            <router-link :to="lp('shop')" class="adi-hero-btn" @click.stop>
              <span>{{ t('home.shopNowBtn') }}</span>
              <span class="adi-arrow">→</span>
            </router-link>
            <router-link :to="{ path: lp('shop'), query: { sort: 'newest' } }" class="adi-hero-btn" @click.stop>
              <span>{{ t('home.exploreMoreBtn') }}</span>
              <span class="adi-arrow">→</span>
            </router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- SECTION 6: "STEP INTO YOUR NEXT ICON" CIRCULAR LOOKBOOK CAROUSEL -->
    <!-- Exact mirror of steps/42/media_0.jpg                             -->
    <!-- ================================================================ -->
    <section class="adi-icons-section" v-if="iconLookbooks.length">
      <div class="adi-container">
        <h2 class="adi-display-h2">{{ t('home.iconsHeading') }}</h2>
      </div>
      <div class="adi-bleed-container">
        <div class="adi-icons-rail">
          <div
            v-for="item in iconLookbooks"
            :key="item.key"
            class="icon-circle-card"
            @click="router.push(item.to)"
          >
            <div class="icon-circle-disc">
              <img :src="item.image" :alt="item.label" loading="lazy" />
            </div>
            <span class="icon-circle-label">{{ item.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- SECTION 7: "WHAT'S HOT" 4.3-CARD PORTRAIT 3:4 CAROUSEL           -->
    <!-- Fixed hover state (white text on black hover!) — steps/56        -->
    <!-- ================================================================ -->
    <section class="adi-whats-hot-section" v-if="whatsHotItems.length">
      <div class="adi-container">
        <h2 class="adi-display-h2">{{ t('home.whatsHotHeading') }}</h2>
      </div>
      <div class="adi-bleed-container">
        <div class="adi-hot-rail">
          <article
            v-for="story in whatsHotItems"
            :key="story.id"
            class="adi-hot-card"
            @click="viewProduct(story.id)"
          >
            <div class="hot-poster">
              <img :src="getProductImage(story)" :alt="story.productName" loading="lazy" />
            </div>
            <div class="hot-body">
              <h3 class="hot-title">{{ story.productName }}</h3>
              <p class="hot-desc">
                {{ story.description || `${story.brandName} ${story.categoryName || ''} — ADHA Sneaker.` }}
              </p>
              <span class="adi-underline-link">{{ t('home.buyNow') }}</span>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- SECTION 8: "BẠN VẪN QUAN TÂM?" PRODUCT CAROUSEL #2 (steps/63)    -->
    <!-- ================================================================ -->
    <section class="adi-still-interested-section" v-if="secondRowProducts.length">
      <div class="adi-container">
        <h2 class="adi-display-h2">{{ t('home.stillInterestedHeading') }}</h2>
      </div>
      <div class="adi-bleed-container rail-nav-wrap">
        <div ref="carouselRef2" class="adi-product-rail">
          <article
            v-for="product in secondRowProducts"
            :key="product.id"
            class="adi-2026-card"
            @click="viewProduct(product.id)"
          >
            <div class="card-img-stage">
              <img :src="getProductImage(product)" :alt="product.productName" loading="lazy" />
              <button
                type="button"
                class="card-heart-btn"
                @click.stop="toggleWishlist(product)"
                aria-label="Yêu thích"
              >
                <svg
                  viewBox="0 0 24 24"
                  :fill="wishlistStore.isFavorite(product.id) ? '#000000' : 'none'"
                  stroke="#000000"
                  stroke-width="1.6"
                >
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                </svg>
              </button>
            </div>
            <div class="card-info">
              <div class="price-block">
                <div class="price-regular">{{ formatCurrency(product.salePrice) }}</div>
              </div>
              <h4 class="card-title">{{ product.productName }}</h4>
              <p class="card-cat">{{ product.categoryName || product.brandName || 'Originals' }}</p>
            </div>
          </article>
        </div>
        <button
          type="button"
          class="adi-rail-arrow rail-arrow-next"
          @click="scrollRail(carouselRef2, 1)"
          aria-label="Xem thêm sản phẩm"
        >
          →
        </button>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- SECTION 9: 4-COL QUICK SEO DIRECTORY (GIÀY / THỂ THAO / ...)     -->
    <!-- Exact mirror of steps/63/media_0.jpg                             -->
    <!-- ================================================================ -->
    <section class="adi-quick-seo-section">
      <div class="adi-container quick-seo-grid">
        <div class="seo-col">
          <h4>{{ t('home.seoCol1') }}</h4>
          <ul>
            <li><router-link :to="lp('shop')">{{ t('home.seoRun') }}</router-link></li>
            <li><router-link :to="lp('shop')">{{ t('home.seoLife') }}</router-link></li>
            <li><router-link :to="lp('shop')">{{ t('home.seoBsk') }}</router-link></li>
            <li><router-link :to="{ path: lp('shop'), query: { sort: 'newest' } }">{{ t('home.seoNew') }}</router-link></li>
          </ul>
        </div>
        <div class="seo-col">
          <h4>{{ t('home.seoCol2') }}</h4>
          <ul>
            <li v-for="b in brands.slice(0, 4)" :key="b.id">
              <router-link :to="{ path: lp('shop'), query: { brand: b.id } }">{{ b.brandName.toLowerCase() }}</router-link>
            </li>
          </ul>
        </div>
        <div class="seo-col">
          <h4>{{ t('home.seoCol3') }}</h4>
          <ul>
            <li v-for="c in categories.slice(0, 4)" :key="c.id">
              <router-link :to="{ path: lp('shop'), query: { category: c.id } }">{{ c.categoryName.toLowerCase() }}</router-link>
            </li>
          </ul>
        </div>
        <div class="seo-col">
          <h4>{{ t('home.seoCol4') }}</h4>
          <ul>
            <li><router-link :to="lp('tracking')">{{ t('home.seoTracker') }}</router-link></li>
            <li><router-link :to="lp('my-orders')">{{ t('home.seoOrders') }}</router-link></li>
            <li><router-link :to="lp('cart')">{{ t('home.seoCart') }}</router-link></li>
            <li><router-link :to="{ path: lp('shop'), query: { sort: 'price_asc' } }">{{ t('home.seoOutlet') }}</router-link></li>
          </ul>
        </div>
      </div>
    </section>

    <!-- ================================================================ -->
    <!-- SECTION 10: BLACK CENTERED NARROW-COLUMN SEO BRAND STORY         -->
    <!-- Exact mirror of steps/70 + steps/84 (604px col, logo AT BOTTOM!) -->
    <!-- ================================================================ -->
    <section class="adi-black-seo-band">
      <div class="adi-seo-narrow">
        <h2 class="seo-brand-heading">{{ t('home.storyHeading') }}</h2>
        <p>{{ t('home.storyP1') }}</p>
        <p>{{ t('home.storyP2') }}</p>
        <p>{{ t('home.storyP3') }}</p>
        <div class="seo-bottom-logo">
          <AdhaLogoMark :size="40" variant="light" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdhaLogoMark from '@/components/common/AdhaLogoMark.vue'
import storeService from '@/services/storeService'
import { formatCurrency } from '@/utils/formatters'
import { useWishlistStore } from '@/stores/wishlist'
import { useStoreI18n } from '@/composables/useStoreI18n'

const router = useRouter()
const wishlistStore = useWishlistStore()
const { t, lp, isEn } = useStoreI18n()

const loading = ref(false)
const brands = ref([])
const categories = ref([])
const featuredProducts = ref([])
const latestProducts = ref([])
const activeTab = ref('sale')
const carouselRef1 = ref(null)
const carouselRef2 = ref(null)

const scrollRail = (el, dir = 1) => {
  if (!el) return
  const step = Math.max(320, el.clientWidth * 0.72)
  el.scrollBy({ left: dir * step, behavior: 'smooth' })
}

const FALLBACK_IMG =
  'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=900&q=80'

const getProductImage = (product) => {
  if (!product) return FALLBACK_IMG
  return product.thumbnailUrl || (product.imageUrls && product.imageUrls[0]) || FALLBACK_IMG
}

const allCatalogPool = computed(() => {
  const map = new Map()
  ;[...latestProducts.value, ...featuredProducts.value].forEach((p) => {
    if (p?.id && !map.has(p.id)) map.set(p.id, p)
  })
  return Array.from(map.values())
})

/* Section 1: 3 Studio Promo Tiles alongside the Black Friday Card */
const topPromoTiles = computed(() => {
  const pool = allCatalogPool.value
  return [
    {
      key: 'men',
      label: t('home.menPill'),
      image:
        pool[0]?.thumbnailUrl ||
        'https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?auto=format&fit=crop&w=700&q=80',
      to: lp('shop')
    },
    {
      key: 'women',
      label: t('home.womenPill'),
      image:
        pool[1]?.thumbnailUrl ||
        'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=700&q=80',
      to: { path: lp('shop'), query: { sort: 'newest' } }
    },
    {
      key: 'new',
      label: t('home.colPill'),
      image:
        pool[2]?.thumbnailUrl ||
        'https://images.unsplash.com/photo-1551107696-a4b0c5a0d9a2?auto=format&fit=crop&w=700&q=80',
      to: { path: lp('shop'), query: { sort: 'price_asc' } }
    }
  ]
})

/* Section 2: Best Deals Portrait 3:4 Rail */
const dealCards = computed(() => {
  const pool = allCatalogPool.value
  return [
    {
      key: 'last-size',
      variant: 'cobalt',
      overlaySub: t('home.deal1Sub'),
      overlayBig: '50%',
      title: t('home.deal1Title'),
      to: { path: lp('shop'), query: { sort: 'price_asc' } }
    },
    {
      key: 'style-gu',
      variant: 'cobalt',
      overlaySub: t('home.deal2Sub'),
      overlayBig: t('home.deal2Big'),
      title: t('home.deal2Title'),
      to: lp('shop')
    },
    {
      key: 'deal-1',
      variant: 'photo',
      image:
        pool[3]?.thumbnailUrl ||
        'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&w=700&q=80',
      title: pool[3]
        ? `${pool[3].brandName} ${isEn.value ? 'from' : 'từ'} ${formatCurrency(pool[3].salePrice)}`
        : (isEn.value ? 'Lifestyle Sneakers' : 'Giày Lifestyle chính hãng'),
      to: pool[3] ? lp(`product/${pool[3].id}`) : lp('shop')
    },
    {
      key: 'deal-2',
      variant: 'photo',
      image:
        pool[4]?.thumbnailUrl ||
        'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=700&q=80',
      title: pool[4]
        ? `${isEn.value ? 'Running from' : 'Giày chạy bộ từ'} ${formatCurrency(pool[4].salePrice)}`
        : (isEn.value ? 'Performance Running' : 'Giày chạy bộ hiệu năng cao'),
      to: pool[4] ? lp(`product/${pool[4].id}`) : lp('shop')
    },
    {
      key: 'deal-3',
      variant: 'photo',
      image:
        pool[5]?.thumbnailUrl ||
        'https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&w=700&q=80',
      title: pool[5] ? `${pool[5].productName}` : (isEn.value ? 'Retro Classics' : 'Bộ sưu tập Retro kinh điển'),
      to: pool[5] ? lp(`product/${pool[5].id}`) : lp('shop')
    }
  ]
})

const topFilterBrands = computed(() => brands.value.slice(0, 4))

const displayedProducts = computed(() => {
  if (activeTab.value === 'sale') {
    return allCatalogPool.value.slice(0, 10)
  }
  if (activeTab.value === 'new') {
    return latestProducts.value.length ? latestProducts.value.slice(0, 10) : allCatalogPool.value.slice(0, 10)
  }
  if (String(activeTab.value).startsWith('brand-')) {
    const brandId = Number(String(activeTab.value).replace('brand-', ''))
    const brandObj = brands.value.find((b) => b.id === brandId)
    const filtered = allCatalogPool.value.filter(
      (p) => Number(p.brandId) === brandId || (brandObj && p.brandName === brandObj.brandName)
    )
    return filtered.length ? filtered : allCatalogPool.value.slice(0, 8)
  }
  return allCatalogPool.value.slice(0, 10)
})

/* Section 6: Circular Icon Lookbook Carousel */
const iconLookbooks = computed(() => {
  const pool = allCatalogPool.value
  return brands.value.slice(0, 6).map((b, idx) => {
    const prod = pool.find((p) => Number(p.brandId) === Number(b.id) || p.brandName === b.brandName) || pool[idx]
    return {
      key: `icon-${b.id}`,
      label: b.brandName.toUpperCase(),
      image: getProductImage(prod),
      to: { path: lp('shop'), query: { brand: b.id } }
    }
  })
})

/* Section 7: What's Hot (pick distinct slice of products so photos vary!) */
const whatsHotItems = computed(() => {
  const pool = allCatalogPool.value
  return pool.length >= 9 ? pool.slice(4, 9) : pool.slice(0, 5)
})

/* Section 8: Second Product Carousel ("BẠN VẪN QUAN TÂM?") */
const secondRowProducts = computed(() => {
  const pool = allCatalogPool.value
  return pool.length >= 10 ? [...pool].reverse().slice(0, 8) : pool.slice(0, 8)
})

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

const loadData = async () => {
  loading.value = true
  try {
    const [bRes, cRes, pRes, latestRes] = await Promise.all([
      storeService.getBrands(),
      storeService.getCategories(),
      storeService.getFeaturedProducts(),
      storeService.getProducts({ page: 0, size: 16 })
    ])
    brands.value = bRes?.data || []
    categories.value = cRes?.data || []
    featuredProducts.value = pRes?.data || []
    latestProducts.value = latestRes?.data?.content || []
  } catch (err) {
    console.error('Lỗi nạp dữ liệu trang chủ:', err)
  } finally {
    loading.value = false
  }
}

const viewProduct = (productId) => {
  router.push(lp(`product/${productId}`))
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.store-home {
  background: #ffffff;
  color: #000000;
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

/* Right-edge bleeding carousel aligned pixel-exact to 6.2vw left gutter (no double margin!) */
.adi-bleed-container {
  padding-left: 6.2vw;
  padding-right: 0;
  box-sizing: border-box;

  @media (max-width: 1024px) {
    padding-left: 24px;
  }

  @media (max-width: 640px) {
    padding-left: 16px;
  }
}

.rail-nav-wrap {
  position: relative;

  .adi-rail-arrow {
    position: absolute;
    top: 38%;
    transform: translateY(-50%);
    width: 44px;
    height: 44px;
    background: #ffffff;
    border: 1px solid #000000;
    color: #000000;
    font-size: 20px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    z-index: 6;
    transition: background-color 0.14s ease, color 0.14s ease;

    &.rail-arrow-next {
      right: 6.2vw;
    }

    &:hover {
      background: #000000;
      color: #ffffff;
    }

    @media (max-width: 768px) {
      display: none;
    }
  }
}

.adi-display-h2 {
  font-family: 'Barlow Condensed', 'Inter', sans-serif;
  font-size: 38px;
  font-weight: 700;
  letter-spacing: 2.5px;
  line-height: 1.08;
  text-transform: uppercase;
  color: #000000;
  margin: 0 0 24px;

  @media (max-width: 640px) {
    font-size: 28px;
    letter-spacing: 1.5px;
  }
}

/* Underlined link with FIXED hover contrast (never black-on-black!) */
.adi-underline-link {
  display: inline-block;
  font-size: 14px;
  font-weight: 700;
  color: #000000;
  text-decoration: underline;
  text-underline-offset: 4px;
  padding: 2px 4px;
  margin-left: -4px;
  transition: background-color 0.14s ease, color 0.14s ease;
}

/* ============ SECTION 1: TOP 4-COL BLACK FRIDAY PROMO SPLIT ============ */
.adi-top-promo-section {
  padding: 36px 0 48px;

  @media (max-width: 768px) {
    padding: 20px 0 32px;
  }
}

.adi-top-promo-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 520px) {
    grid-template-columns: 1fr;
  }
}

.promo-black-card {
  background: #000000;
  color: #ffffff;
  padding: 24px 22px;
  aspect-ratio: 5 / 4;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-sizing: border-box;

  @media (max-width: 520px) {
    aspect-ratio: auto;
    min-height: 240px;
  }

  .promo-black-title {
    font-family: 'Barlow Condensed', 'Inter', sans-serif;
    font-size: clamp(26px, 2vw, 34px);
    font-weight: 700;
    letter-spacing: 1.5px;
    line-height: 1.06;
    margin: 0 0 12px;
    text-transform: uppercase;
    color: #ffffff;
  }

  .promo-black-desc {
    font-size: 13.5px;
    line-height: 1.52;
    color: #e5e7eb;
    margin: 0;
  }

  .promo-black-cta {
    position: relative;
    align-self: flex-start;
    display: inline-flex;
    align-items: center;
    background: #ffffff;
    color: #000000;
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0.08em;
    padding: 9px 14px;
    text-decoration: none;
    text-transform: uppercase;

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      transform: translate(3px, 3px);
      border: 1px solid #ffffff;
      pointer-events: none;
    }

    &:hover {
      color: #555555;
    }
  }
}

.promo-studio-card {
  position: relative;
  aspect-ratio: 5 / 4;
  background: #f4f5f6;
  overflow: hidden;
  cursor: pointer;

  .promo-studio-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.35s ease;
  }

  &:hover .promo-studio-img {
    transform: scale(1.04);
  }

  .promo-studio-pill {
    position: absolute;
    left: 14px;
    bottom: 14px;
    display: inline-flex;
    align-items: center;
    background: #ffffff;
    padding: 6px 10px;
    gap: 8px;

    .pill-pct {
      color: #e32b2b;
      font-size: 12px;
      font-weight: 700;
    }

    .pill-label {
      color: #000000;
      font-size: 12.5px;
      font-weight: 700;
      letter-spacing: 0.06em;
      text-transform: uppercase;
    }
  }
}

/* ============ SECTION 2: BEST DEALS PORTRAIT 3:4 CAROUSEL ============ */
.adi-deals-strip {
  padding: 16px 0 56px;
}

.adi-deals-rail {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-right: 24px;
  padding-bottom: 10px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.adi-deal-card {
  flex: 0 0 calc((100vw - 12.4vw - 30px) / 4);
  min-width: 260px;
  cursor: pointer;

  @media (max-width: 1024px) {
    flex: 0 0 280px;
  }

  @media (max-width: 640px) {
    flex: 0 0 230px;
  }

  &:hover .adi-underline-link {
    background: #000000;
    color: #ffffff;
    text-decoration: none;
  }

  .deal-poster {
    aspect-ratio: 3 / 4;
    background: #eceff1;
    overflow: hidden;
    position: relative;
    margin-bottom: 14px;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: transform 0.4s ease;
    }

    &.cobalt-poster {
      background: linear-gradient(155deg, #3c64a8 0%, #244583 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      text-align: center;
      color: #ffffff;

      .cobalt-stripes {
        position: absolute;
        top: -20px;
        right: -20px;
        width: 140px;
        height: 180px;
        background: repeating-linear-gradient(
          -35deg,
          rgba(255, 255, 255, 0.25) 0 18px,
          transparent 18px 34px
        );
      }

      .cobalt-typo {
        position: relative;
        z-index: 2;
        display: flex;
        flex-direction: column;
        gap: 8px;

        .cobalt-sub {
          font-size: 13px;
          font-weight: 700;
          letter-spacing: 0.16em;
        }

        .cobalt-big {
          font-family: 'Barlow Condensed', sans-serif;
          font-size: 46px;
          font-weight: 800;
          line-height: 1.02;
          letter-spacing: 0.04em;
        }
      }
    }
  }

  &:hover .deal-poster img {
    transform: scale(1.04);
  }

  .deal-title {
    font-size: 14px;
    font-weight: 700;
    color: #000000;
    margin: 0 0 18px;
    line-height: 1.4;
  }
}

/* ============ SECTION 3 & 5: FULL-BLEED EDITORIAL CAMPAIGN HEROS ============ */
.adi-campaign-hero {
  position: relative;
  width: 100%;
  height: 620px;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
  background: #141414;
  cursor: pointer;

  @media (max-width: 900px) {
    height: 480px;
  }

  @media (max-width: 640px) {
    height: 420px;
  }

  .campaign-hero-img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .campaign-hero-scrim {
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(0, 0, 0, 0.05) 35%, rgba(0, 0, 0, 0.45) 100%);
  }

  .campaign-hero-inner {
    position: relative;
    z-index: 2;
    padding-bottom: 44px;
  }

  /* Exact live adidas.com.vn/vi sleek 24px white-highlight ribbon stack */
  .campaign-hero-stack {
    max-width: 470px;

    .hero-hl-row {
      margin: 0 0 6px;
      line-height: 1.25;

      .hero-white-hl {
        display: inline;
        background: #ffffff;
        color: #000000;
        font-family: 'Barlow Condensed', 'Inter', sans-serif;
        font-size: 26px;
        font-weight: 700;
        letter-spacing: 2.2px;
        text-transform: uppercase;
        padding: 3px 8px;
        box-decoration-break: clone;
        -webkit-box-decoration-break: clone;
      }
    }

    .hero-sub-row {
      margin: 0 0 18px;
      line-height: 1.55;

      .hero-white-sub {
        display: inline;
        background: #ffffff;
        color: #000000;
        font-size: 15px;
        font-weight: 400;
        padding: 3px 8px;
        box-decoration-break: clone;
        -webkit-box-decoration-break: clone;
      }
    }

    .hero-btn-row {
      display: flex;
      align-items: center;
      gap: 14px;
      flex-wrap: wrap;
    }

    .adi-hero-btn {
      position: relative;
      display: inline-flex;
      align-items: center;
      gap: 14px;
      height: 46px;
      padding: 0 18px;
      background: #ffffff;
      color: #000000;
      border: 1px solid #ffffff;
      font-size: 14px;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      text-decoration: none;

      &::after {
        content: '';
        position: absolute;
        inset: 0;
        transform: translate(3px, 3px);
        border: 1px solid #ffffff;
        pointer-events: none;
      }

      .adi-arrow {
        font-size: 17px;
        transition: transform 0.15s ease;
      }

      &:hover {
        color: #555555;
        .adi-arrow {
          transform: translateX(4px);
        }
      }
    }
  }
}

/* ============ SECTION 4 & 8: 2026 LIVE ADIDAS PRODUCT CAROUSELS ============ */
.adi-carousel-section {
  padding: 44px 0 56px;
}

.adi-still-interested-section {
  padding: 20px 0 60px;
}

.carousel-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

/* Soft #d9dbdd 32px chips (Exact live adidas.com.vn/vi) */
.adi-chip-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;

  .adi-chip-btn {
    height: 34px;
    padding: 0 12px;
    background: #ffffff;
    color: #000000;
    border: 1px solid #d9dbdd;
    font-family: inherit;
    font-size: 14.5px;
    font-weight: 400;
    cursor: pointer;
    transition: border-color 0.14s ease, background-color 0.14s ease, color 0.14s ease;

    &:hover {
      border-color: #767677;
    }

    &.active {
      background: #000000;
      color: #ffffff;
      border-color: #000000;
    }
  }
}

.adi-see-all-link {
  font-size: 14px;
  font-weight: 700;
  color: #000000;
  text-decoration: underline;
  text-underline-offset: 4px;

  &:hover {
    background: #000000;
    color: #ffffff;
    text-decoration: none;
    padding: 2px 6px;
    margin: -2px -6px;
  }
}

.adi-skeleton-rail,
.adi-product-rail {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-right: 6.2vw;
  padding-bottom: 6px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.adi-skeleton-card {
  flex: 0 0 calc((100vw - 12.4vw - 30px) / 4);
  min-width: 260px;
  height: 410px;
  background: #eceff1;
}

/* Exact 2026 Live Adidas Product Card: 4 full cards span 87.6vw + 5th card peeks right! */
.adi-2026-card {
  flex: 0 0 calc((100vw - 12.4vw - 30px) / 4);
  min-width: 260px;
  background: #ffffff;
  border: 1px solid transparent;
  cursor: pointer;
  transition: border-color 0.14s ease;

  @media (max-width: 1024px) {
    flex: 0 0 280px;
  }

  @media (max-width: 640px) {
    flex: 0 0 220px;
  }

  &:hover {
    border-color: #000000;
  }

  .card-img-stage {
    position: relative;
    width: 100%;
    aspect-ratio: 1 / 1;
    background: #eceff1;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: transform 0.35s ease;
    }

    &:hover img {
      transform: scale(1.03);
    }

    .card-heart-btn {
      position: absolute;
      top: 12px;
      right: 12px;
      width: 34px;
      height: 34px;
      background: transparent;
      border: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;

      svg {
        width: 20px;
        height: 20px;
      }

      &:hover {
        background: rgba(255, 255, 255, 0.85);
      }
    }
  }

  .card-info {
    padding: 12px 10px 26px;

    .price-block {
      margin-bottom: 6px;

      .price-regular {
        font-size: 14px;
        font-weight: 700;
        color: #000000;
      }

      .price-sale {
        font-size: 14px;
        font-weight: 700;
        color: #e32b2b;
      }

      .price-original-row {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-top: 2px;
        font-size: 12px;

        .price-strike {
          color: #767677;
          text-decoration: line-through;
        }

        .price-orig-label {
          color: #767677;
        }

        .price-pct {
          color: #e32b2b;
          font-weight: 600;
        }
      }
    }

    .card-title {
      font-size: 14px;
      font-weight: 400;
      line-height: 20px;
      color: #000000;
      margin: 0 0 4px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-cat {
      font-size: 13.5px;
      color: #767677;
      margin: 0;
    }
  }
}

/* ============ SECTION 6: "STEP INTO YOUR NEXT ICON" CIRCULAR CAROUSEL ============ */
.adi-icons-section {
  padding: 56px 0 48px;
}

.adi-icons-rail {
  display: flex;
  gap: 28px;
  overflow-x: auto;
  padding-right: 24px;
  padding-bottom: 12px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.icon-circle-card {
  flex: 0 0 264px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  cursor: pointer;

  @media (max-width: 640px) {
    flex: 0 0 190px;
  }

  .icon-circle-disc {
    width: 248px;
    height: 248px;
    border-radius: 50%;
    background: #dcd3cc;
    overflow: hidden;

    @media (max-width: 640px) {
      width: 180px;
      height: 180px;
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: transform 0.4s ease;
    }
  }

  &:hover .icon-circle-disc img {
    transform: scale(1.06);
  }

  .icon-circle-label {
    font-size: 14px;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: #000000;
    text-decoration: underline;
    text-underline-offset: 4px;
  }
}

/* ============ SECTION 7: "WHAT'S HOT" 4.3-CARD PORTRAIT CAROUSEL ============ */
.adi-whats-hot-section {
  padding: 20px 0 56px;
}

.adi-hot-rail {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-right: 24px;
  padding-bottom: 10px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.adi-hot-card {
  flex: 0 0 calc((100vw - 12.4vw - 30px) / 4);
  min-width: 260px;
  display: flex;
  flex-direction: column;
  cursor: pointer;

  @media (max-width: 1024px) {
    flex: 0 0 280px;
  }

  @media (max-width: 640px) {
    flex: 0 0 240px;
  }

  .hot-poster {
    aspect-ratio: 3 / 4;
    background: #eceff1;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: transform 0.45s ease;
    }
  }

  &:hover {
    .hot-poster img {
      transform: scale(1.04);
    }

    .adi-underline-link {
      background: #000000;
      color: #ffffff;
      text-decoration: none;
    }
  }

  .hot-body {
    padding-top: 14px;
    display: flex;
    flex-direction: column;
    flex: 1;

    .hot-title {
      font-size: 15px;
      font-weight: 700;
      color: #000000;
      margin: 0 0 8px;
      line-height: 1.35;
    }

    .hot-desc {
      font-size: 13.5px;
      line-height: 1.5;
      color: #333333;
      margin: 0 0 18px;
      flex: 1;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .adi-underline-link {
      align-self: flex-start;
    }
  }
}

/* ============ SECTION 9: 4-COLUMN QUICK SEO DIRECTORY ============ */
.adi-quick-seo-section {
  padding: 40px 0 72px;
  background: #ffffff;

  .quick-seo-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 32px;

    @media (max-width: 768px) {
      grid-template-columns: repeat(2, 1fr);
    }

    h4 {
      font-size: 15px;
      font-weight: 700;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      margin: 0 0 18px;
      color: #000000;
    }

    ul {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 14px;

      a {
        font-size: 14px;
        color: #000000;
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
}

/* ============ SECTION 10: BLACK CENTERED NARROW-COLUMN BRAND STORY ============ */
.adi-black-seo-band {
  background: #000000;
  color: #ffffff;
  padding: 80px 24px 68px;

  .adi-seo-narrow {
    max-width: 620px;
    margin: 0 auto;

    .seo-brand-heading {
      font-family: 'Barlow Condensed', 'Inter', sans-serif;
      font-size: 38px;
      font-weight: 700;
      letter-spacing: 2.5px;
      line-height: 1.1;
      text-align: center;
      text-transform: uppercase;
      color: #ffffff;
      margin: 0 0 32px;

      @media (max-width: 640px) {
        font-size: 28px;
      }
    }

    p {
      font-size: 14.5px;
      line-height: 1.7;
      color: #ffffff;
      margin: 0 0 20px;
    }

    .seo-bottom-logo {
      display: flex;
      justify-content: center;
      padding-top: 28px;
    }
  }
}
</style>
