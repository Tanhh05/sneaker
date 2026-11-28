<template>
  <div class="store-detail">
    <!-- Loading State -->
    <div v-if="loading" class="pdp-loading-wrap">
      <el-skeleton :rows="12" animated />
    </div>

    <!-- Main Full-Bleed 2-Column Adidas PDP Layout (Left 67% / Right 480px Sticky Buy Box) -->
    <div v-else-if="product" class="pdp-fullbleed-grid">
      <!-- ============================================================== -->
      <!-- LEFT COLUMN (67%): 2x2 Photo Grid + Accordions + Related Grid  -->
      <!-- ============================================================== -->
      <div class="pdp-left-col">
        <!-- 1. Edge-to-Edge 2x2 Studio Photo Grid with Floating Breadcrumb -->
        <div class="pdp-gallery-section">
          <!-- Floating Breadcrumb Overlaid Top-Left (Exact live adidas.com.vn/vi) -->
          <nav class="pdp-floating-breadcrumb" aria-label="Breadcrumb">
            <button type="button" class="bc-back-btn" @click="router.back()">
              <span class="bc-back-icon">↩</span>
              <span class="bc-back-text">{{ t('pdp.back') }}</span>
            </button>
            <router-link :to="lp('')" class="bc-link">{{ t('pdp.home') }}</router-link>
            <span class="bc-sep">/</span>
            <router-link
              :to="product.brandId ? { path: lp('shop'), query: { brand: product.brandId } } : lp('shop')"
              class="bc-link"
            >
              {{ product.brandName || 'Sneaker' }}
            </router-link>
            <span class="bc-sep">/</span>
            <router-link
              :to="product.categoryId ? { path: lp('shop'), query: { category: product.categoryId } } : lp('shop')"
              class="bc-link"
            >
              {{ product.categoryName || t('footer.shoes') }}
            </router-link>
          </nav>

          <!-- 2x2 Photo Grid (2px crisp white seam) -->
          <div class="pdp-photo-grid" :class="{ 'single-photo': visibleGalleryGrid.length === 1 }">
            <div
              v-for="(item, idx) in visibleGalleryGrid"
              :key="idx"
              class="pdp-photo-tile"
              @click="openLightbox({ url: item.url, customerName: product.productName, comment: item.colorName ? `${t('pdp.specColor')}: ${item.colorName}` : product.brandName })"
            >
              <img :src="item.url" :alt="`${product.productName} - ${idx + 1}`" />
            </div>
          </div>

          <!-- Centered Straddling "Hiện thêm ⌄" Button when > 4 images -->
          <div v-if="displayGalleryPool.length > 4" class="gallery-expand-wrap">
            <button
              type="button"
              class="btn-gallery-expand"
              @click="galleryExpanded = !galleryExpanded"
            >
              <span>{{ galleryExpanded ? t('pdp.showLess') : t('pdp.showMore') }}</span>
              <span class="chevron" :class="{ open: galleryExpanded }">⌄</span>
            </button>
          </div>
        </div>

        <!-- 2. Crisp Horizontal-Divider Collapsible Accordions (Inside Left Column!) -->
        <div class="pdp-accordions-wrap">
          <!-- Accordion 1: Đánh giá (Reviews & Fit Meter) -->
          <div class="adi-accordion-item" id="customer-reviews">
            <button
              type="button"
              class="adi-accordion-header"
              @click="accordions.reviews = !accordions.reviews"
            >
              <span class="acc-title">
                {{ t('pdp.reviews') }} ({{ reviewsSummary?.totalReviews || 0 }})
              </span>
              <div class="acc-right">
                <span class="acc-stars" :aria-label="t('pdp.reviews')">★★★★★</span>
                <svg
                  class="acc-chevron"
                  :class="{ open: accordions.reviews }"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                >
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </div>
            </button>

            <div v-if="accordions.reviews" class="adi-accordion-body">
              <!-- Overall Rating Hero -->
              <div class="rev-summary-top">
                <span class="rev-big-num">{{ averageRatingDisplay }}</span>
                <span class="rev-big-stars">★★★★★</span>
              </div>

              <!-- 2x2 Segmented Adidas-Style Fit/Comfort/Quality/Width Bars -->
              <div class="rev-segmented-grid">
                <div class="seg-metric">
                  <div class="seg-label">{{ t('pdp.comfort') }}</div>
                  <div class="seg-track">
                    <span></span><span></span><span></span><span class="active"></span>
                  </div>
                  <div class="seg-scale">
                    <span>{{ t('pdp.uncomfortable') }}</span>
                    <span>{{ t('pdp.comfortable') }}</span>
                  </div>
                </div>

                <div class="seg-metric">
                  <div class="seg-label">{{ t('pdp.quality') }}</div>
                  <div class="seg-track">
                    <span></span><span></span><span></span><span class="active"></span>
                  </div>
                  <div class="seg-scale">
                    <span>{{ t('pdp.poor') }}</span>
                    <span>{{ t('pdp.perfect') }}</span>
                  </div>
                </div>

                <div class="seg-metric">
                  <div class="seg-label">{{ t('pdp.size') }}</div>
                  <div class="seg-track">
                    <span :class="{ active: dominantFit === 'HALF_SIZE_UP' }"></span>
                    <span :class="{ active: dominantFit === 'TRUE_TO_SIZE' }"></span>
                    <span :class="{ active: dominantFit === 'HALF_SIZE_DOWN' }"></span>
                  </div>
                  <div class="seg-scale">
                    <span>{{ t('pdp.tooSmall') }}</span>
                    <span>{{ t('pdp.trueToSize') }}</span>
                    <span>{{ t('pdp.tooLarge') }}</span>
                  </div>
                </div>

                <div class="seg-metric">
                  <div class="seg-label">{{ t('pdp.width') }}</div>
                  <div class="seg-track">
                    <span></span><span class="active"></span><span></span>
                  </div>
                  <div class="seg-scale">
                    <span>{{ t('pdp.tooNarrow') }}</span>
                    <span>{{ t('pdp.fitJustRight') }}</span>
                    <span>{{ t('pdp.tooWide') }}</span>
                  </div>
                </div>
              </div>

              <!-- On-Feet Strip -->
              <div v-if="allOnFeetPhotos.length > 0" class="onfeet-strip-block">
                <div class="onfeet-header">{{ t('pdp.onFeetTitle') }} ({{ allOnFeetPhotos.length }})</div>
                <div class="onfeet-thumbs">
                  <div
                    v-for="(photo, idx) in allOnFeetPhotos"
                    :key="idx"
                    class="onfeet-thumb-item"
                    @click="openLightbox(photo)"
                  >
                    <img :src="photo.url" :alt="`On-feet ${idx}`" />
                  </div>
                </div>
              </div>

              <!-- Compact #eceff1 Star Filter Chips -->
              <div class="rev-filter-row">
                <div class="rev-filter-label">{{ t('pdp.filterByStar') }}</div>
                <div class="rev-filter-chips">
                  <button
                    v-for="f in reviewFilterOptions"
                    :key="f.value"
                    type="button"
                    class="rev-chip"
                    :class="{ active: currentReviewFilter === f.value }"
                    @click="currentReviewFilter = f.value"
                  >
                    {{ f.label }} ({{ f.count }})
                  </button>
                </div>
              </div>

              <!-- Reviews Feed -->
              <div v-if="filteredReviewList.length > 0" class="reviews-feed">
                <div v-for="review in filteredReviewList" :key="review.id" class="review-row-item">
                  <div class="row-header">
                    <div class="user-block">
                      <strong class="user-name">{{ review.customerName || (isEn ? 'Customer' : 'Khách hàng') }}</strong>
                      <span v-if="review.verifiedBuyer" class="verified-tag">{{ t('pdp.verifiedBuyer') }}</span>
                      <span class="date-line">{{ formatReviewDate(review.createdAt) }}</span>
                    </div>
                    <div class="rating-block">
                      <span class="review-stars-inline">{{ '★'.repeat(review.rating || 5) }}{{ '☆'.repeat(5 - (review.rating || 5)) }}</span>
                      <span v-if="review.fitFeedback" class="fit-tag">{{ getFitFeedbackLabel(review.fitFeedback) }}</span>
                    </div>
                  </div>

                  <p class="review-text">{{ review.comment }}</p>

                  <div v-if="review.photoUrls && review.photoUrls.length > 0" class="review-photos-row">
                    <div
                      v-for="(pUrl, pIdx) in review.photoUrls"
                      :key="pIdx"
                      class="photo-thumb"
                      @click="openLightbox({ url: pUrl, customerName: review.customerName, comment: review.comment })"
                    >
                      <img :src="pUrl" :alt="`Photo ${pIdx}`" />
                    </div>
                  </div>

                  <div class="row-actions">
                    <button
                      type="button"
                      class="btn-helpful-simple"
                      :class="{ active: helpfulMap[review.id] }"
                      @click="toggleHelpful(review)"
                    >
                      {{ t('pdp.helpful') }} ({{ (review.helpfulCount || 0) + (helpfulMap[review.id] ? 1 : 0) }})
                    </button>
                  </div>
                </div>
              </div>

              <!-- Empty Reviews -->
              <div v-else class="reviews-empty-clean">
                <p>{{ t('pdp.emptyReviews') }}</p>
                <router-link :to="lp('my-orders')" class="btn-orders-link">
                  {{ t('pdp.writeReviewLink') }}
                </router-link>
              </div>
            </div>
          </div>

          <!-- Accordion 2: Mô tả (Exact 2-Column Split: Left Copy + Right Square Studio Photo) -->
          <div class="adi-accordion-item">
            <button
              type="button"
              class="adi-accordion-header"
              @click="accordions.description = !accordions.description"
            >
              <span class="acc-title">{{ t('pdp.description') }}</span>
              <svg
                class="acc-chevron"
                :class="{ open: accordions.description }"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
              >
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </button>

            <div v-if="accordions.description" class="adi-accordion-body">
              <div class="desc-split-grid">
                <div class="desc-copy-col">
                  <h3 class="desc-subtitle">{{ product.productName }}</h3>
                  <p class="desc-text">
                    {{
                      product.description ||
                        (isEn
                          ? `${product.productName} by ${product.brandName} combines a timeless silhouette with responsive all-day cushioning and crisp everyday versatility.`
                          : `${product.productName} đến từ ${product.brandName} mang thiết kế kinh điển dễ phối đồ hằng ngày, thân giày hoàn thiện tỉ mỉ cùng bộ đế êm ái bền bỉ.`)
                    }}
                  </p>
                  <p class="desc-text" v-if="product.material">
                    {{
                      isEn
                        ? `Crafted with ${product.material}. Every pair at ADHA Sneaker is 100% Legit Check verified and Double-Boxed to preserve the original sneaker box.`
                        : `Chất liệu cao cấp: ${product.material}. Mọi sản phẩm tại ADHA Sneaker đều được kiểm định chính hãng 100% và đóng gói Double-Box bảo vệ hộp nguyên bản.`
                    }}
                  </p>
                </div>
                <div class="desc-photo-col">
                  <img :src="activeImage" :alt="product.productName" />
                </div>
              </div>
            </div>
          </div>

          <!-- Accordion 3: Thông tin chi tiết (Exact 2-Column Bulleted Specs) -->
          <div class="adi-accordion-item">
            <button
              type="button"
              class="adi-accordion-header"
              @click="accordions.details = !accordions.details"
            >
              <span class="acc-title">{{ t('pdp.details') }}</span>
              <svg
                class="acc-chevron"
                :class="{ open: accordions.details }"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
              >
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </button>

            <div v-if="accordions.details" class="adi-accordion-body">
              <ul class="specs-bullet-grid">
                <li>{{ t('pdp.specFit') }}</li>
                <li>{{ t('pdp.specLace') }}</li>
                <li v-if="product.material">{{ t('pdp.specMaterial') }}: {{ product.material }}</li>
                <li>{{ t('pdp.specBrand') }}: {{ product.brandName }}</li>
                <li>{{ t('pdp.specCategory') }}: {{ product.categoryName || 'Originals' }}</li>
                <li>{{ t('pdp.specColor') }}: {{ selectedColor ? selectedColor.colorName : (isEn ? 'Standard colorway' : 'Phối màu tiêu chuẩn') }}</li>
                <li>{{ t('pdp.specSku') }}: {{ selectedVariant ? selectedVariant.sku : product.productCode }}</li>
                <li>{{ t('pdp.specStock') }}: {{ currentStock > 0 ? `${currentStock} ${t('pdp.inStockText')}` : t('pdp.outOfStock') }}</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 3. "HOÀN THIỆN PHONG CÁCH" / Related Sneakers 4-Column 2026 Adidas Grid -->
        <section v-if="relatedProducts.length" class="pdp-related-section">
          <h2 class="related-heading">{{ t('pdp.completeLook') }}</h2>
          <div class="related-grid">
            <article
              v-for="item in relatedProducts"
              :key="item.id"
              class="adi-2026-card"
              @click="router.push(lp(`product/${item.id}`))"
            >
              <div class="card-img-stage">
                <img
                  :src="item.thumbnailUrl || (item.imageUrls && item.imageUrls[0]) || FALLBACK_IMG"
                  :alt="item.productName"
                  loading="lazy"
                />
                <button
                  type="button"
                  class="card-heart-btn"
                  @click.stop="toggleRelatedWishlist(item)"
                  :aria-label="t('account.wishlist')"
                >
                  <svg
                    viewBox="0 0 24 24"
                    :fill="wishlistStore.isFavorite(item.id) ? '#000000' : 'none'"
                    stroke="#000000"
                    stroke-width="1.6"
                  >
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                  </svg>
                </button>
              </div>
              <div class="card-info">
                <div class="price-regular">{{ formatCurrency(item.salePrice) }}</div>
                <h4 class="card-title">{{ item.productName }}</h4>
                <p class="card-cat">{{ item.brandName || 'Originals' }}</p>
              </div>
            </article>
          </div>
        </section>
      </div>

      <!-- ============================================================== -->
      <!-- RIGHT COLUMN (480px STICKY BUY BOX — Exact adidas.com.vn/vi)   -->
      <!-- ============================================================== -->
      <aside class="pdp-right-buybox">
        <!-- Row 1: Category Left + Compact Star Rating Right -->
        <div class="buybox-meta-top">
          <span class="buybox-cat">{{ product.categoryName || 'Originals' }}</span>
          <button type="button" class="buybox-rating-btn" @click="scrollToReviews">
            <span class="rating-num">{{ averageRatingDisplay }}</span>
            <span class="rating-stars">★★★★★</span>
            <span class="rating-cnt">({{ reviewsSummary?.totalReviews || 0 }})</span>
          </button>
        </div>

        <!-- Subtle Archive Pill -->
        <div class="buybox-subtle-tag">
          {{ t('pdp.legitTag') }}
        </div>

        <!-- Upright Condensed Bold H1 (26px, normal style, 2px tracking) -->
        <h1 class="buybox-title">{{ product.productName }}</h1>

        <!-- Clean 17px Bold Price -->
        <div class="buybox-price">
          {{ formatCurrency(selectedVariant?.salePrice || product.salePrice) }}
        </div>

        <!-- Color Swatches: 5-Col Square Photo Thumbnails + Active Color Name Below -->
        <div class="buybox-block" v-if="uniqueColors.length">
          <div class="block-label">{{ t('pdp.colors') }}</div>
          <div class="color-thumb-grid">
            <button
              v-for="c in uniqueColors"
              :key="c.id"
              type="button"
              class="color-thumb-tile"
              :class="{ active: selectedColor && selectedColor.id === c.id }"
              :title="c.colorName"
              @click="selectColor(c)"
            >
              <img :src="getColorThumbnail(c)" :alt="c.colorName" />
              <span
                class="color-mini-dot"
                :style="{ backgroundColor: c.hexCode || '#000000' }"
              ></span>
            </button>
          </div>
          <div class="selected-color-caption">
            {{ selectedColor ? selectedColor.colorName : '' }}
          </div>
        </div>

        <!-- Size Selector: 5-Col 40px #eceff1 Flat Tiles -->
        <div class="buybox-block" :class="{ 'size-error-shake': sizeValidationError }">
          <div class="size-head-row">
            <span class="block-label">{{ t('pdp.sizes') }}</span>
            <div class="size-head-links">
              <button type="button" class="underline-action" @click="showSmartSizeFinder = true">
                {{ t('pdp.smartSizeFinder') }}
              </button>
              <button type="button" class="underline-action" @click="showSizeGuide = true">
                {{ t('pdp.sizeGuide') }}
              </button>
            </div>
          </div>

          <div class="size-grid-5">
            <button
              v-for="size in availableSizesForColor"
              :key="size.id"
              type="button"
              class="size-cell"
              :class="{
                active: selectedSize && selectedSize.id === size.id,
                disabled: size.stock <= 0
              }"
              :disabled="size.stock <= 0"
              @click="selectSize(size)"
            >
              {{ size.sizeValue }}
            </button>
          </div>

          <p v-if="sizeValidationError" class="size-err-msg">
            {{ t('pdp.selectSizeErr') }}
          </p>
          <p v-else-if="selectedSize && currentStock > 0" class="size-stock-note">
            Size EU {{ selectedSize.sizeValue }} — {{ currentStock }} {{ t('pdp.inStockText') }}
          </p>

          <!-- Exact Adidas 1px solid #000 Fit Advice Box -->
          <div class="fit-advice-box">
            <span class="fit-info-icon">i</span>
            <span>
              <strong>{{ t('pdp.fitAdviceStrong') }}</strong> {{ t('pdp.fitAdviceBody') }}
            </span>
          </div>
        </div>

        <!-- Quantity Row (only shown cleanly once size picked) -->
        <div class="buybox-qty-row" v-if="selectedVariant && currentStock > 0">
          <span class="qty-label">{{ t('pdp.quantity') }}</span>
          <div class="qty-stepper">
            <button type="button" class="step-btn" :disabled="quantity <= 1" @click="quantity > 1 && quantity--">−</button>
            <span class="qty-val">{{ quantity }}</span>
            <button type="button" class="step-btn" :disabled="quantity >= currentStock" @click="quantity < currentStock && quantity++">+</button>
          </div>
        </div>

        <!-- Primary CTA Row: Always-Active 58px Black Offset Button + 56px Square Heart -->
        <div class="buybox-cta-group">
          <div class="cta-main-row">
            <button
              type="button"
              class="btn-adi-add-bag"
              @click="handleAddToCart"
            >
              <span>{{ t('pdp.addToBag') }}</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" class="bag-plus-svg">
                <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                <line x1="3" y1="6" x2="21" y2="6"></line>
                <path d="M16 10a4 4 0 0 1-8 0"></path>
              </svg>
            </button>

            <button
              type="button"
              class="btn-adi-heart"
              :class="{ active: wishlistStore.isFavorite(product?.id) }"
              @click="handleToggleWishlist"
              :title="wishlistStore.isFavorite(product?.id) ? (isEn ? 'Remove from wishlist' : 'Bỏ khỏi yêu thích') : (isEn ? 'Save to wishlist' : 'Lưu vào yêu thích')"
              :aria-label="t('account.wishlist')"
            >
              <svg
                viewBox="0 0 24 24"
                :fill="wishlistStore.isFavorite(product?.id) ? '#000000' : 'none'"
                stroke="#000000"
                stroke-width="1.7"
              >
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
            </button>
          </div>

          <button type="button" class="btn-adi-secondary" @click="handleBuyNow">
            <span>{{ t('pdp.buyNow') }}</span>
            <span class="sec-arrow">→</span>
          </button>
        </div>

        <!-- ADHA Club Points Strip (Exact mirror of adiClub points row) -->
        <div class="buybox-club-strip">
          <div class="club-strip-top">
            <span class="club-strip-title">{{ t('pdp.memberEarn') }}</span>
            <span class="club-wordmark">adhaclub</span>
          </div>
          <div class="club-points-line">
            <span class="points-star">✪</span>
            <span>{{ estimatedPoints }} {{ t('pdp.pointsLabel') }}</span>
          </div>
        </div>

        <!-- 3 Crisp 1-Line Icon + Chevron USP Rows -->
        <div class="buybox-usp-list">
          <router-link :to="{ path: lp('shop'), query: { sort: 'price_asc' } }" class="usp-row">
            <span class="usp-left">
              <span class="usp-icon">✓</span>
              <span>{{ t('pdp.usp1') }}</span>
            </span>
            <span class="usp-chevron">›</span>
          </router-link>

          <button type="button" class="usp-row" @click="showSizeGuide = true">
            <span class="usp-left">
              <span class="usp-icon">↺</span>
              <span>{{ t('pdp.usp2') }}</span>
            </span>
            <span class="usp-chevron">›</span>
          </button>

          <router-link :to="lp('tracking')" class="usp-row">
            <span class="usp-left">
              <span class="usp-icon">🚚</span>
              <span>{{ t('pdp.usp3') }}</span>
            </span>
            <span class="usp-chevron">›</span>
          </router-link>
        </div>

        <!-- Bottom Quick Shop Category Button -->
        <router-link
          :to="product.brandId ? { path: lp('shop'), query: { brand: product.brandId } } : lp('shop')"
          class="buybox-shop-more-btn"
        >
          <span>{{ t('pdp.shopMorePrefix') }} {{ product.brandName || 'Sneaker' }}</span>
        </router-link>
      </aside>
    </div>

    <!-- Lightbox Modal for Full Image Zoom -->
    <el-dialog
      v-model="lightboxVisible"
      width="720px"
      class="lightbox-dialog"
      :show-close="true"
      align-center
    >
      <div v-if="activeLightboxPhoto" class="lightbox-content">
        <img :src="activeLightboxPhoto.url" alt="Zoom full size" class="lightbox-img" />
        <div class="lightbox-caption">
          <strong>{{ activeLightboxPhoto.customerName }}</strong>
          <p v-if="activeLightboxPhoto.comment">{{ activeLightboxPhoto.comment }}</p>
        </div>
      </div>
    </el-dialog>

    <!-- Smart Size Finder Modal -->
    <SmartSizeFinderModal
      v-model="showSmartSizeFinder"
      :product-name="product ? product.productName : ''"
      :brand-name="product ? product.brandName : ''"
      @select-size="handleSmartSizeSelect"
    />

    <!-- Size Guide Dialog -->
    <el-dialog v-model="showSizeGuide" :title="t('footer.sizeDialogTitle')" width="600px">
      <el-table :data="sizeGuideData" border stripe>
        <el-table-column prop="eu" label="EU (VN)" align="center" width="100" />
        <el-table-column prop="us" label="US Men" align="center" width="100" />
        <el-table-column prop="uk" label="UK" align="center" width="100" />
        <el-table-column prop="cm" :label="t('footer.footLength')" align="center" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import storeService from '@/services/storeService'
import SmartSizeFinderModal from '@/components/store/SmartSizeFinderModal.vue'
import { useCartStore } from '@/stores/cart'
import { useWishlistStore } from '@/stores/wishlist'
import { formatCurrency } from '@/utils/formatters'
import { useStoreI18n } from '@/composables/useStoreI18n'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const wishlistStore = useWishlistStore()
const { t, lp, isEn } = useStoreI18n()

const FALLBACK_IMG =
  'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80'

const loading = ref(false)
const product = ref(null)
const activeImage = ref('')
const quantity = ref(1)
const galleryExpanded = ref(false)
const sizeValidationError = ref(false)
const relatedProducts = ref([])

const accordions = reactive({
  reviews: false,
  description: true,
  details: false
})

// SMART SIZE FINDER STATE
const showSmartSizeFinder = ref(false)

const handleSmartSizeSelect = (recSize) => {
  if (!availableSizesForColor.value || availableSizesForColor.value.length === 0) return
  const found = availableSizesForColor.value.find((s) => String(s.sizeValue) === String(recSize))
  if (found) {
    selectSize(found)
  } else {
    ElMessage.info(`Size ${recSize} tạm hết trong kho màu này, mời bạn chọn size kế cận!`)
  }
}

// REVIEWS & ON-FEET STATE
const reviewsSummary = ref(null)
const currentReviewFilter = ref('ALL')
const helpfulMap = ref({})
const lightboxVisible = ref(false)
const activeLightboxPhoto = ref(null)

const loadReviews = async () => {
  try {
    const id = route.params.id
    const res = await storeService.getProductReviews(id)
    reviewsSummary.value = res?.data || null
  } catch (err) {
    console.warn('Lỗi nạp đánh giá sản phẩm:', err)
  }
}

const loadRelatedProducts = async () => {
  try {
    const res = await storeService.getProducts({ page: 0, size: 8 })
    const list = res?.data?.content || []
    const currentId = Number(route.params.id)
    relatedProducts.value = list.filter((p) => Number(p.id) !== currentId).slice(0, 4)
  } catch (err) {
    console.warn('Lỗi nạp sản phẩm gợi ý:', err)
  }
}

const averageRatingDisplay = computed(() => {
  if (!reviewsSummary.value || !reviewsSummary.value.averageRating) return '5.0'
  return reviewsSummary.value.averageRating.toFixed(1)
})

const dominantFit = computed(() => {
  if (!reviewsSummary.value?.totalReviews) return 'TRUE_TO_SIZE'
  const t = reviewsSummary.value.trueToSizeCount || 0
  const u = reviewsSummary.value.halfSizeUpCount || 0
  const d = reviewsSummary.value.halfSizeDownCount || 0
  if (u > t && u >= d) return 'HALF_SIZE_UP'
  if (d > t && d > u) return 'HALF_SIZE_DOWN'
  return 'TRUE_TO_SIZE'
})

const estimatedPoints = computed(() => {
  const price = Number(selectedVariant.value?.salePrice || product.value?.salePrice || 0)
  return Math.max(50, Math.round(price / 3000))
})

const allOnFeetPhotos = computed(() => {
  if (!reviewsSummary.value?.reviews) return []
  const photos = []
  reviewsSummary.value.reviews.forEach((r) => {
    if (r.photoUrls && r.photoUrls.length > 0) {
      r.photoUrls.forEach((url) => {
        photos.push({
          url,
          customerName: r.customerName || 'Khách hàng',
          comment: r.comment
        })
      })
    }
  })
  return photos
})

const reviewFilterOptions = computed(() => {
  const total = reviewsSummary.value?.totalReviews || 0
  const photoCount = (reviewsSummary.value?.reviews || []).filter(
    (r) => r.photoUrls && r.photoUrls.length > 0
  ).length
  const fiveStar = reviewsSummary.value?.fiveStarCount || 0
  const fourStar = reviewsSummary.value?.fourStarCount || 0
  const threeStar =
    (reviewsSummary.value?.threeStarCount || 0) +
    (reviewsSummary.value?.twoStarCount || 0) +
    (reviewsSummary.value?.oneStarCount || 0)

  return [
    { label: t('pdp.filterAll'), value: 'ALL', count: total },
    { label: '★ 5', value: '5', count: fiveStar },
    { label: '★ 4', value: '4', count: fourStar },
    { label: '★ ≤3', value: '3_DOWN', count: threeStar },
    { label: t('pdp.filterHasPhoto'), value: 'HAS_PHOTO', count: photoCount }
  ]
})

const filteredReviewList = computed(() => {
  const list = reviewsSummary.value?.reviews || []
  if (currentReviewFilter.value === 'ALL') return list
  if (currentReviewFilter.value === 'HAS_PHOTO') {
    return list.filter((r) => r.photoUrls && r.photoUrls.length > 0)
  }
  if (currentReviewFilter.value === '5') {
    return list.filter((r) => r.rating === 5)
  }
  if (currentReviewFilter.value === '4') {
    return list.filter((r) => r.rating === 4)
  }
  if (currentReviewFilter.value === '3_DOWN') {
    return list.filter((r) => r.rating <= 3)
  }
  return list
})

const formatReviewDate = (isoStr) => {
  if (!isoStr) return ''
  try {
    const d = new Date(isoStr)
    return d.toLocaleDateString(isEn.value ? 'en-US' : 'vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit' })
  } catch {
    return isoStr
  }
}

const getFitFeedbackLabel = (fit) => {
  const map = isEn.value
    ? {
        TRUE_TO_SIZE: 'True to size',
        HALF_SIZE_UP: 'Runs small (+0.5)',
        HALF_SIZE_DOWN: 'Runs large (-0.5)'
      }
    : {
        TRUE_TO_SIZE: 'Chuẩn size',
        HALF_SIZE_UP: 'Hơi chật (Tăng 0.5)',
        HALF_SIZE_DOWN: 'Hơi rộng (Lùi 0.5)'
      }
  return map[fit] || (isEn.value ? 'True to size' : 'Chuẩn size')
}

const toggleHelpful = (review) => {
  helpfulMap.value[review.id] = !helpfulMap.value[review.id]
}

const openLightbox = (photo) => {
  activeLightboxPhoto.value = photo
  lightboxVisible.value = true
}

const scrollToReviews = () => {
  accordions.reviews = true
  setTimeout(() => {
    document.getElementById('customer-reviews')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, 60)
}

const handleToggleWishlist = () => {
  if (!product.value) return
  const isAdded = wishlistStore.toggleFavorite(product.value)
  if (isAdded) {
    ElMessage.success(`Đã thêm "${product.value.productName}" vào danh sách yêu thích`)
  } else {
    ElMessage.info(`Đã bỏ "${product.value.productName}" khỏi danh sách yêu thích`)
  }
}

const toggleRelatedWishlist = (item) => {
  const isAdded = wishlistStore.toggleFavorite(item)
  if (isAdded) {
    ElMessage.success(`Đã thêm "${item.productName}" vào danh sách yêu thích`)
  } else {
    ElMessage.info(`Đã bỏ "${item.productName}" khỏi danh sách yêu thích`)
  }
}

const selectedColor = ref(null)
const selectedSize = ref(null)
const showSizeGuide = ref(false)

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

const allImages = computed(() => {
  if (!product.value) return []
  const list = []
  if (product.value.images && product.value.images.length) {
    product.value.images.forEach((img) => {
      if (img?.imageUrl && !list.includes(img.imageUrl)) list.push(img.imageUrl)
    })
  }
  if (product.value.imageUrls && product.value.imageUrls.length) {
    product.value.imageUrls.forEach((img) => {
      if (img && !list.includes(img)) list.push(img)
    })
  }
  if (product.value.thumbnailUrl && !list.includes(product.value.thumbnailUrl)) {
    list.unshift(product.value.thumbnailUrl)
  }
  if (list.length === 0) {
    list.push(FALLBACK_IMG)
  }
  return list
})

const uniqueColors = computed(() => {
  if (!product.value || !product.value.variants) return []
  const map = new Map()
  product.value.variants.forEach((v) => {
    const c = v.color || { id: v.colorId, colorName: v.colorName, hexCode: v.hexCode }
    if (c && c.id && !map.has(c.id)) {
      map.set(c.id, c)
    }
  })
  return Array.from(map.values())
})

const galleryItems = computed(() => {
  if (!product.value) return []
  const colorMap = new Map()
  uniqueColors.value.forEach((c) => colorMap.set(Number(c.id), c))

  if (product.value.images && product.value.images.length) {
    const seen = new Set()
    const items = []
    const sorted = [...product.value.images].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
    sorted.forEach((img) => {
      const key = `${img?.colorId ?? 'all'}::${img?.imageUrl || ''}`
      if (img?.imageUrl && !seen.has(key)) {
        seen.add(key)
        const cid = img.colorId != null ? Number(img.colorId) : null
        const c = cid != null ? colorMap.get(cid) : null
        items.push({
          url: img.imageUrl,
          colorId: cid,
          colorName: c?.colorName || '',
          hexCode: c?.hexCode || null
        })
      }
    })
    if (items.length > 0) return items
  }
  return allImages.value.map((url) => ({ url, colorId: null, colorName: '', hexCode: null }))
})

/* Filter gallery strictly to the selected color's images (never mix other colors!) */
const displayGalleryPool = computed(() => {
  const all = galleryItems.value
  if (!all.length) return []
  if (selectedColor.value) {
    const targetColorId = Number(selectedColor.value.id)
    const colorSpecific = all.filter((item) => item.colorId != null && item.colorId === targetColorId)
    if (colorSpecific.length > 0) {
      return colorSpecific
    }
    const uncolored = all.filter((item) => item.colorId == null)
    if (uncolored.length > 0) {
      return uncolored
    }
  }
  return all
})

const visibleGalleryGrid = computed(() => {
  if (galleryExpanded.value) return displayGalleryPool.value
  return displayGalleryPool.value.slice(0, 4)
})

const getColorThumbnail = (color) => {
  const matched = galleryItems.value.find((item) => item.colorId === Number(color.id))
  return matched?.url || allImages.value[0] || FALLBACK_IMG
}

const availableSizesForColor = computed(() => {
  if (!product.value || !product.value.variants) return []
  return product.value.variants
    .filter((v) => {
      if (!selectedColor.value) return true
      const cId = v.color ? v.color.id : v.colorId
      return cId === selectedColor.value.id
    })
    .map((v) => ({
      id: v.size ? v.size.id : v.sizeId,
      sizeValue: v.size ? v.size.sizeValue : v.sizeValue,
      stock:
        v.availableQuantity !== undefined
          ? v.availableQuantity
          : v.quantityInStock !== undefined
          ? v.quantityInStock
          : 10,
      variant: v
    }))
})

const selectedVariant = computed(() => {
  if (!product.value || !product.value.variants || !selectedSize.value) return null
  return product.value.variants.find((v) => {
    const sId = v.size ? v.size.id : v.sizeId
    const cId = v.color ? v.color.id : v.colorId
    const matchSize = sId === selectedSize.value.id
    const matchColor = selectedColor.value ? cId === selectedColor.value.id : true
    return matchSize && matchColor
  })
})

const currentStock = computed(() => {
  if (!selectedVariant.value) {
    if (product.value && product.value.variants) {
      return product.value.variants.reduce((acc, v) => acc + (v.availableQuantity ?? v.quantityInStock ?? 0), 0)
    }
    return 10
  }
  return selectedVariant.value.availableQuantity ?? selectedVariant.value.quantityInStock ?? 10
})

const syncImageToColor = (color) => {
  if (!color || !galleryItems.value.length) return
  const matched = galleryItems.value.find((item) => item.colorId === Number(color.id))
  if (matched) {
    activeImage.value = matched.url
  } else if (allImages.value.length) {
    activeImage.value = allImages.value[0]
  }
}

const selectColor = (color) => {
  selectedColor.value = color
  selectedSize.value = null
  sizeValidationError.value = false
  galleryExpanded.value = false
  syncImageToColor(color)
}

const selectSize = (size) => {
  selectedSize.value = size
  sizeValidationError.value = false
}

const loadProduct = async () => {
  loading.value = true
  selectedSize.value = null
  selectedColor.value = null
  sizeValidationError.value = false
  galleryExpanded.value = false
  quantity.value = 1
  try {
    const id = route.params.id
    const res = await storeService.getProductDetail(id)
    product.value = res?.data || null
    if (allImages.value.length) {
      activeImage.value = allImages.value[0]
    }
    if (uniqueColors.value.length) {
      selectedColor.value = uniqueColors.value[0]
      syncImageToColor(uniqueColors.value[0])
    }
  } catch (err) {
    console.error('Lỗi nạp sản phẩm:', err)
    ElMessage.error('Không tìm thấy thông tin sản phẩm')
  } finally {
    loading.value = false
  }
}

const handleAddToCart = () => {
  if (!selectedVariant.value) {
    sizeValidationError.value = true
    ElMessage.warning(t('pdp.selectSizeErr'))
    return
  }
  cartStore.addToCart(product.value, selectedVariant.value, quantity.value)
  ElMessage.success(
    isEn.value
      ? `Added "${product.value.productName}" (Size ${selectedSize.value.sizeValue}) to bag!`
      : `Đã thêm "${product.value.productName}" (Size ${selectedSize.value.sizeValue}) vào giỏ hàng!`
  )
}

const handleBuyNow = () => {
  if (!selectedVariant.value) {
    sizeValidationError.value = true
    ElMessage.warning(t('pdp.selectSizeErr'))
    return
  }
  cartStore.addToCart(product.value, selectedVariant.value, quantity.value)
  router.push(lp('cart'))
}

watch(
  () => route.params.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      window.scrollTo({ top: 0, behavior: 'smooth' })
      loadProduct()
      loadReviews()
      loadRelatedProducts()
    }
  }
)

onMounted(() => {
  loadProduct()
  loadReviews()
  loadRelatedProducts()
})
</script>

<style scoped lang="scss">
.store-detail {
  background: #ffffff;
  color: #000000;
  width: 100%;
}

.pdp-loading-wrap {
  max-width: 1280px;
  margin: 0 auto;
  padding: 48px 24px;
}

/* ============ FULL-BLEED 2-COL ADIDAS PDP GRID (67% LEFT / 480px STICKY RIGHT) ============ */
.pdp-fullbleed-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 480px;
  align-items: start;
  width: 100%;

  @media (max-width: 1180px) {
    grid-template-columns: minmax(0, 1fr) 420px;
  }

  @media (max-width: 960px) {
    grid-template-columns: 1fr;
  }
}

/* ============ LEFT COLUMN: 2x2 GALLERY + ACCORDIONS + RELATED ============ */
.pdp-left-col {
  min-width: 0;
  border-right: 1px solid #eceff1;

  @media (max-width: 960px) {
    border-right: none;
  }
}

.pdp-gallery-section {
  position: relative;
  width: 100%;
}

/* Floating Breadcrumb Overlaid Top-Left of Photo #1 (Exact live adidas.com.vn/vi) */
.pdp-floating-breadcrumb {
  position: absolute;
  top: 22px;
  left: 24px;
  z-index: 6;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(4px);
  padding: 6px 12px;
  font-size: 13.5px;
  flex-wrap: wrap;

  @media (max-width: 640px) {
    top: 12px;
    left: 12px;
    font-size: 12px;
    padding: 5px 9px;
  }

  .bc-back-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    background: none;
    border: none;
    padding: 0 8px 0 0;
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

  .bc-link {
    color: #000000;
    text-decoration: underline;
    text-underline-offset: 3px;

    &:hover {
      background: #000000;
      color: #ffffff;
      text-decoration: none;
    }
  }

  .bc-sep {
    color: #555555;
  }
}

/* 2x2 Edge-to-Edge Studio Photo Grid */
.pdp-photo-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px;
  background: #ffffff;

  &.single-photo {
    grid-template-columns: 1fr;
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .pdp-photo-tile {
    aspect-ratio: 1 / 1;
    background: #eceff1;
    overflow: hidden;
    cursor: zoom-in;

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
  }
}

.gallery-expand-wrap {
  display: flex;
  justify-content: center;
  margin-top: -22px;
  margin-bottom: 18px;
  position: relative;
  z-index: 4;

  .btn-gallery-expand {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    background: #ffffff;
    color: #000000;
    border: 1px solid #000000;
    padding: 10px 22px;
    font-family: inherit;
    font-size: 13.5px;
    font-weight: 700;
    cursor: pointer;

    .chevron {
      font-size: 16px;
      line-height: 1;
      transition: transform 0.2s ease;

      &.open {
        transform: rotate(180deg);
      }
    }

    &:hover {
      background: #000000;
      color: #ffffff;
    }
  }
}

/* ============ ACCORDIONS INSIDE LEFT COLUMN (Exact live adidas.com.vn/vi) ============ */
.pdp-accordions-wrap {
  padding: 36px 48px 24px;

  @media (max-width: 1024px) {
    padding: 28px 24px 20px;
  }

  @media (max-width: 640px) {
    padding: 20px 16px;
  }
}

.adi-accordion-item {
  border-top: 1px solid #d9dbdd;

  &:last-child {
    border-bottom: 1px solid #d9dbdd;
  }
}

.adi-accordion-header {
  width: 100%;
  height: 76px;
  background: transparent;
  border: none;
  padding: 0 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-family: inherit;
  cursor: pointer;

  .acc-title {
    font-size: 16px;
    font-weight: 700;
    color: #000000;
  }

  .acc-right {
    display: inline-flex;
    align-items: center;
    gap: 18px;

    .acc-stars {
      font-size: 15px;
      letter-spacing: 2px;
      color: #000000;
    }
  }

  .acc-chevron {
    width: 20px;
    height: 20px;
    color: #000000;
    transition: transform 0.2s ease;

    &.open {
      transform: rotate(180deg);
    }
  }
}

.adi-accordion-body {
  padding: 4px 4px 36px;
}

/* Accordion 2: Mô tả (2-Col Copy + Product Photo Split) */
.desc-split-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 36px;
  align-items: center;

  @media (max-width: 720px) {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .desc-subtitle {
    font-family: 'Barlow Condensed', 'Inter', sans-serif;
    font-size: 26px;
    font-weight: 700;
    letter-spacing: 1px;
    text-transform: uppercase;
    margin: 0 0 12px;
  }

  .desc-text {
    font-size: 14.5px;
    line-height: 1.65;
    color: #111111;
    margin: 0 0 12px;
  }

  .desc-photo-col {
    aspect-ratio: 1 / 1;
    background: #eceff1;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
  }
}

/* Accordion 3: Thông tin chi tiết (2-Col Bulleted List) */
.specs-bullet-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 32px;
  margin: 0;
  padding-left: 18px;
  font-size: 14.5px;
  line-height: 1.55;
  color: #000000;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

/* Accordion 1: Đánh giá (Exact Live Adidas Segmented Bars + Star Chips) */
.rev-summary-top {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;

  .rev-big-num {
    font-size: 40px;
    font-weight: 800;
    line-height: 1;
    color: #000000;
  }

  .rev-big-stars {
    font-size: 18px;
    letter-spacing: 3px;
    color: #000000;
  }
}

.rev-segmented-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px 40px;
  margin-bottom: 30px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .seg-metric {
    .seg-label {
      font-size: 14px;
      font-weight: 700;
      color: #000000;
      margin-bottom: 10px;
    }

    .seg-track {
      display: flex;
      gap: 4px;
      margin-bottom: 8px;

      span {
        flex: 1;
        height: 4px;
        background: #eceff1;

        &.active {
          background: #000000;
        }
      }
    }

    .seg-scale {
      display: flex;
      justify-content: space-between;
      font-size: 12.5px;
      color: #555555;
    }
  }
}

.onfeet-strip-block {
  margin-bottom: 24px;

  .onfeet-header {
    font-size: 13px;
    font-weight: 700;
    margin-bottom: 10px;
  }

  .onfeet-thumbs {
    display: flex;
    gap: 8px;
    overflow-x: auto;

    .onfeet-thumb-item {
      width: 86px;
      height: 86px;
      flex-shrink: 0;
      background: #eceff1;
      cursor: pointer;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }
  }
}

.rev-filter-row {
  margin-bottom: 22px;

  .rev-filter-label {
    font-size: 14px;
    font-weight: 700;
    margin-bottom: 10px;
  }

  .rev-filter-chips {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;

    .rev-chip {
      height: 36px;
      padding: 0 12px;
      background: #f4f5f6;
      border: 1px solid #d9dbdd;
      font-family: inherit;
      font-size: 13px;
      font-weight: 500;
      color: #000000;
      cursor: pointer;

      &:hover {
        border-color: #000000;
      }

      &.active {
        background: #000000;
        border-color: #000000;
        color: #ffffff;
      }
    }
  }
}

.reviews-feed {
  .review-row-item {
    padding: 18px 0;
    border-top: 1px solid #eceff1;

    .row-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 10px;
      margin-bottom: 8px;

      .user-block {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        .user-name {
          font-size: 14px;
          font-weight: 700;
        }

        .verified-tag {
          font-size: 11px;
          background: #eceff1;
          padding: 2px 6px;
          color: #000000;
        }

        .date-line {
          font-size: 12px;
          color: #767677;
        }
      }

      .rating-block {
        display: flex;
        align-items: center;
        gap: 8px;

        .review-stars-inline {
          font-size: 14px;
          letter-spacing: 1px;
        }

        .fit-tag {
          font-size: 11.5px;
          background: #eceff1;
          padding: 2px 8px;
        }
      }
    }

    .review-text {
      font-size: 14px;
      line-height: 1.6;
      color: #222222;
      margin: 0 0 12px;
    }

    .review-photos-row {
      display: flex;
      gap: 8px;
      margin-bottom: 10px;

      .photo-thumb {
        width: 72px;
        height: 72px;
        background: #eceff1;
        cursor: pointer;
        overflow: hidden;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }
    }

    .btn-helpful-simple {
      background: #ffffff;
      border: 1px solid #d9dbdd;
      padding: 5px 12px;
      font-family: inherit;
      font-size: 12px;
      font-weight: 600;
      color: #000000;
      cursor: pointer;

      &:hover,
      &.active {
        background: #000000;
        border-color: #000000;
        color: #ffffff;
      }
    }
  }
}

.reviews-empty-clean {
  padding: 24px 0 8px;
  font-size: 14px;
  color: #555555;

  p {
    margin: 0 0 12px;
  }

  .btn-orders-link {
    display: inline-block;
    font-size: 13.5px;
    font-weight: 700;
    color: #000000;
    text-decoration: underline;
    text-underline-offset: 4px;

    &:hover {
      background: #000000;
      color: #ffffff;
      text-decoration: none;
    }
  }
}

/* ============ RELATED PRODUCTS ("HOÀN THIỆN PHONG CÁCH") ============ */
.pdp-related-section {
  padding: 36px 48px 72px;

  @media (max-width: 1024px) {
    padding: 28px 24px 52px;
  }

  @media (max-width: 640px) {
    padding: 24px 16px 40px;
  }

  .related-heading {
    font-family: 'Barlow Condensed', 'Inter', sans-serif;
    font-size: 36px;
    font-weight: 700;
    letter-spacing: 2.5px;
    text-transform: uppercase;
    color: #000000;
    margin: 0 0 22px;
  }

  .related-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;

    @media (max-width: 768px) {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  .adi-2026-card {
    border: 1px solid transparent;
    cursor: pointer;
    transition: border-color 0.14s ease;

    &:hover {
      border-color: #000000;
    }

    .card-img-stage {
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

      .card-heart-btn {
        position: absolute;
        top: 10px;
        right: 10px;
        width: 32px;
        height: 32px;
        background: transparent;
        border: none;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;

        svg {
          width: 19px;
          height: 19px;
        }
      }
    }

    .card-info {
      padding: 10px 8px 12px;

      .price-regular {
        font-size: 14px;
        font-weight: 700;
        color: #000000;
        margin-bottom: 4px;
      }

      .card-title {
        font-size: 13.5px;
        font-weight: 400;
        line-height: 18px;
        color: #000000;
        margin: 0 0 4px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .card-cat {
        font-size: 13px;
        color: #767677;
        margin: 0;
      }
    }
  }
}

/* ============ RIGHT COLUMN: STICKY 480px ADIDAS BUY BOX ============ */
.pdp-right-buybox {
  position: sticky;
  top: 81px;
  padding: 28px 34px 44px;
  background: #ffffff;
  box-sizing: border-box;

  @media (max-width: 1180px) {
    padding: 24px 24px 36px;
  }

  @media (max-width: 960px) {
    position: static;
    padding: 24px 16px 36px;
    border-top: 1px solid #eceff1;
  }
}

.buybox-meta-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .buybox-cat {
    font-size: 14px;
    color: #000000;
  }

  .buybox-rating-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: none;
    border: none;
    padding: 0;
    font-family: inherit;
    font-size: 13.5px;
    color: #000000;
    cursor: pointer;

    .rating-stars {
      letter-spacing: 1px;
    }

    .rating-cnt {
      text-decoration: underline;
      text-underline-offset: 3px;
    }
  }
}

.buybox-subtle-tag {
  display: inline-block;
  background: #eceff1;
  color: #000000;
  font-size: 11.5px;
  font-weight: 500;
  letter-spacing: 0.04em;
  padding: 5px 8px;
  margin-bottom: 12px;
}

.buybox-title {
  font-family: 'Barlow Condensed', 'Inter', sans-serif;
  font-size: 28px;
  font-style: normal;
  font-weight: 700;
  letter-spacing: 2px;
  line-height: 1.15;
  text-transform: uppercase;
  color: #000000;
  margin: 0 0 10px;
}

.buybox-price {
  font-size: 17px;
  font-weight: 700;
  color: #000000;
  margin-bottom: 26px;
  font-variant-numeric: tabular-nums;
}

.buybox-block {
  margin-bottom: 24px;

  .block-label {
    font-size: 14px;
    font-weight: 700;
    color: #000000;
    margin-bottom: 12px;
  }
}

/* Square Photo-Thumbnail Color Swatches (Exact live adidas.com.vn/vi) */
.color-thumb-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-bottom: 8px;

  .color-thumb-tile {
    position: relative;
    aspect-ratio: 1 / 1;
    padding: 0;
    background: #eceff1;
    border: none;
    border-bottom: 3px solid transparent;
    overflow: hidden;
    cursor: pointer;
    transition: border-color 0.14s ease;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .color-mini-dot {
      position: absolute;
      right: 4px;
      bottom: 4px;
      width: 10px;
      height: 10px;
      border: 1.5px solid #ffffff;
      box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.35);
    }

    &:hover,
    &.active {
      border-bottom-color: #000000;
    }
  }
}

.selected-color-caption {
  font-size: 13.5px;
  color: #000000;
}

/* Size Selector Grid: 5-Col 40px #eceff1 Tiles + 1px Black Fit Box */
.size-head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  gap: 8px;

  .block-label {
    margin-bottom: 0;
  }

  .size-head-links {
    display: flex;
    align-items: center;
    gap: 14px;

    .underline-action {
      background: none;
      border: none;
      padding: 0;
      font-family: inherit;
      font-size: 13.5px;
      color: #000000;
      text-decoration: underline;
      text-underline-offset: 3px;
      cursor: pointer;

      &:hover {
        background: #000000;
        color: #ffffff;
        text-decoration: none;
      }
    }
  }
}

.size-grid-5 {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 5px;
  margin-bottom: 12px;

  .size-cell {
    height: 40px;
    background: #eceff1;
    border: 1px solid #eceff1;
    color: #000000;
    font-family: inherit;
    font-size: 14px;
    font-weight: 400;
    cursor: pointer;
    transition: background-color 0.12s ease, color 0.12s ease;

    &:hover:not(.disabled) {
      background: #000000;
      border-color: #000000;
      color: #ffffff;
    }

    &.active {
      background: #000000;
      border-color: #000000;
      color: #ffffff;
      font-weight: 700;
    }

    &.disabled {
      opacity: 0.4;
      cursor: not-allowed;
      text-decoration: line-through;
    }
  }
}

.size-err-msg {
  font-size: 13px;
  color: #e32b2b;
  font-weight: 600;
  margin: 0 0 10px;
}

.size-stock-note {
  font-size: 12.5px;
  color: #427f5c;
  font-weight: 600;
  margin: 0 0 10px;
}

.fit-advice-box {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid #000000;
  padding: 14px 16px;
  font-size: 13.5px;
  line-height: 1.45;
  color: #000000;

  .fit-info-icon {
    width: 18px;
    height: 18px;
    border: 1px solid #000000;
    border-radius: 50%;
    font-size: 11px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-top: 2px;
  }
}

/* Quantity Stepper */
.buybox-qty-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;

  .qty-label {
    font-size: 14px;
    font-weight: 700;
  }

  .qty-stepper {
    display: flex;
    align-items: center;
    border: 1px solid #000000;

    .step-btn {
      width: 36px;
      height: 36px;
      background: #ffffff;
      border: none;
      font-size: 16px;
      cursor: pointer;

      &:hover:not(:disabled) {
        background: #eceff1;
      }

      &:disabled {
        opacity: 0.35;
        cursor: not-allowed;
      }
    }

    .qty-val {
      width: 38px;
      text-align: center;
      font-size: 14px;
      font-weight: 700;
    }
  }
}

/* CTA Buttons: Always-Active 58px Black Offset CTA + 56px Square Heart */
.buybox-cta-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;

  .cta-main-row {
    display: flex;
    align-items: stretch;
    gap: 10px;
  }

  .btn-adi-add-bag {
    flex: 1;
    position: relative;
    height: 56px;
    padding: 0 20px;
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    font-family: inherit;
    font-size: 14px;
    font-weight: 700;
    letter-spacing: 0.08em;
    display: flex;
    align-items: center;
    justify-content: space-between;
    cursor: pointer;
    transition: color 0.15s ease;

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      transform: translate(3px, 3px);
      border: 1px solid #000000;
      pointer-events: none;
    }

    .bag-plus-svg {
      width: 20px;
      height: 20px;
    }

    &:hover {
      color: #b8b8b8;
    }
  }

  .btn-adi-heart {
    width: 56px;
    height: 56px;
    flex-shrink: 0;
    background: #ffffff;
    border: 1px solid #000000;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;

    svg {
      width: 22px;
      height: 22px;
    }

    &:hover {
      background: #eceff1;
    }
  }

  .btn-adi-secondary {
    height: 48px;
    padding: 0 18px;
    background: #ffffff;
    color: #000000;
    border: 1px solid #000000;
    font-family: inherit;
    font-size: 14px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: space-between;
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease;

    &:hover {
      background: #000000;
      color: #ffffff;
    }
  }
}

/* ADHA Club Points Strip */
.buybox-club-strip {
  border-top: 1px solid #eceff1;
  border-bottom: 1px solid #eceff1;
  padding: 18px 0;
  margin-bottom: 10px;

  .club-strip-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;

    .club-strip-title {
      font-size: 14px;
      font-weight: 700;
    }

    .club-wordmark {
      font-family: 'Barlow Condensed', sans-serif;
      font-size: 18px;
      font-weight: 800;
      color: #427f5c;
      letter-spacing: 0.02em;
    }
  }

  .club-points-line {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #000000;

    .points-star {
      font-size: 16px;
    }
  }
}

/* 3 Crisp 1-Line USP Rows */
.buybox-usp-list {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;

  .usp-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 11px 4px;
    background: none;
    border: none;
    font-family: inherit;
    font-size: 13px;
    color: #000000;
    text-decoration: none;
    cursor: pointer;

    .usp-left {
      display: inline-flex;
      align-items: center;
      gap: 10px;
    }

    .usp-chevron {
      font-size: 18px;
      color: #555555;
    }

    &:hover {
      background: #f4f5f6;
    }
  }
}

.buybox-shop-more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 46px;
  border: 1px solid #000000;
  background: #ffffff;
  color: #000000;
  font-size: 13.5px;
  font-weight: 700;
  text-decoration: none;

  &:hover {
    background: #000000;
    color: #ffffff;
  }
}

/* Lightbox Modal */
.lightbox-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.lightbox-content {
  display: flex;
  flex-direction: column;

  .lightbox-img {
    width: 100%;
    max-height: 560px;
    object-fit: contain;
    background: #eceff1;
  }

  .lightbox-caption {
    padding: 14px 20px;
    background: #000000;
    color: #ffffff;

    strong {
      display: block;
      font-size: 14px;
      margin-bottom: 4px;
    }

    p {
      font-size: 13px;
      color: #d3d7da;
      margin: 0;
    }
  }
}
</style>
