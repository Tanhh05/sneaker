<template>
  <div class="product-studio-page" v-loading="loading">
    <!-- 1. STICKY COMMAND BAR -->
    <div class="studio-topbar">
      <div class="topbar-left">
        <button type="button" class="btn-back" @click="$router.push('/admin/products')">
          <el-icon><ArrowLeft /></el-icon>
          <span>Quay lại</span>
        </button>

        <div class="mode-pill mode-detail">CHI TIẾT SẢN PHẨM</div>

        <div v-if="product" class="topbar-product-title">
          <span class="code-badge">{{ product.productCode }}</span>
          <h2 class="title-text">{{ product.productName }}</h2>
          <span class="status-pill" :class="product.status === 'ACTIVE' ? 'st-active' : 'st-inactive'">
            {{ product.status }}
          </span>
        </div>
      </div>

      <div v-if="product" class="topbar-right">
        <a
          :href="`/product/${product.id}`"
          target="_blank"
          rel="noopener"
          class="btn-ghost"
          title="Xem trang sản phẩm ngoài cửa hàng"
        >
          <el-icon><TopRight /></el-icon>
          <span>Xem Store</span>
        </a>
        <button
          v-if="canManageCatalog"
          type="button"
          class="btn-outline"
          @click="goToVariantStudio"
        >
          <el-icon><Grid /></el-icon>
          <span>Ma trận SKU</span>
        </button>
        <el-button
          v-if="canManageCatalog"
          type="primary"
          class="btn-primary-cmd"
          @click="$router.push(`/admin/products/${product.id}/edit`)"
        >
          <el-icon style="margin-right: 6px"><Edit /></el-icon>
          <span>CHỈNH SỬA SP</span>
        </el-button>
      </div>
    </div>

    <template v-if="product">
      <!-- 2. COMPACT OVERVIEW GRID (MEDIA + SPECS + KPIs) -->
      <div class="overview-grid">
        <!-- Left: Gallery Studio -->
        <div class="studio-card gallery-card">
          <div class="main-img-stage">
            <img
              v-if="currentImg && !imgError"
              :src="currentImg"
              :alt="product.productName"
              class="main-img"
              @error="imgError = true"
            />
            <div v-else class="img-placeholder">
              <el-icon :size="36"><Picture /></el-icon>
              <span>CHƯA CÓ HÌNH ẢNH</span>
            </div>
            <span v-if="galleryImages.length > 0" class="img-count-badge">
              {{ activeImgIndex + 1 }} / {{ galleryImages.length }}
            </span>
          </div>

          <div v-if="galleryImages.length > 1" class="thumb-strip">
            <button
              v-for="(url, idx) in galleryImages"
              :key="idx"
              type="button"
              class="thumb-btn"
              :class="{ active: currentImg === url }"
              @click="activeImg = url; imgError = false"
            >
              <img :src="url" alt="" />
              <span v-if="idx === 0" class="star-mark">★</span>
            </button>
          </div>
        </div>

        <!-- Right: KPI Row + Metadata & Description -->
        <div class="overview-right">
          <!-- 4 Compact KPI Cards -->
          <div class="kpi-strip">
            <div class="kpi-box">
              <span class="kpi-label">GIÁ NHẬP CƠ SỞ</span>
              <strong class="kpi-val">{{ formatCurrency(product.importPrice || 0) }}</strong>
              <span class="kpi-sub">Giá vốn tiêu chuẩn</span>
            </div>
            <div class="kpi-box">
              <span class="kpi-label">GIÁ BÁN NIÊM YẾT</span>
              <strong class="kpi-val price">{{ formatCurrency(product.salePrice || 0) }}</strong>
              <span class="kpi-sub" :class="profitAmount >= 0 ? 'text-green' : 'text-red'">
                Lãi {{ formatCurrency(profitAmount) }} ({{ profitMarginPct }}%)
              </span>
            </div>
            <div class="kpi-box">
              <span class="kpi-label">BIẾN THỂ SKU</span>
              <strong class="kpi-val">{{ variants.length }} SKU</strong>
              <span class="kpi-sub">{{ matrixColors.length }} màu · {{ matrixSizes.length }} size</span>
            </div>
            <div class="kpi-box">
              <span class="kpi-label">TỔNG TỒN KHO</span>
              <strong class="kpi-val">{{ totalStock }} đôi</strong>
              <span class="kpi-sub" :class="outOfStockCount > 0 ? 'text-amber' : 'text-green'">
                {{ outOfStockCount > 0 ? `${outOfStockCount} SKU hết hàng` : 'Đầy đủ hàng sẵn bán' }}
              </span>
            </div>
          </div>

          <!-- Specs & Description Card -->
          <div class="studio-card specs-card">
            <div class="specs-header">
              <span class="badge-black">THÔNG SỐ &amp; MÔ TẢ SẢN PHẨM</span>
              <button
                v-if="canManageCatalog && product.status === 'ACTIVE'"
                type="button"
                class="btn-danger-link"
                @click="handleDiscontinue"
              >
                ✕ Ngừng kinh doanh
              </button>
            </div>

            <div class="specs-grid">
              <div class="spec-item">
                <label>Thương hiệu</label>
                <strong>{{ product.brandName || '---' }}</strong>
              </div>
              <div class="spec-item">
                <label>Danh mục</label>
                <strong>{{ product.categoryName || '---' }}</strong>
              </div>
              <div class="spec-item spec-wide">
                <label>Chất liệu (Upper &amp; Sole)</label>
                <strong>{{ product.material || 'Chưa cập nhật chất liệu' }}</strong>
              </div>
            </div>

            <div class="desc-block">
              <label>Mô tả sản phẩm</label>
              <p>{{ product.description || 'Chưa có mô tả chi tiết cho sản phẩm này.' }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 3. FULL-WIDTH VARIANT MATRIX & TABLE (UNIFIED WITH VARIANT STUDIO) -->
      <div class="studio-card workspace-card">
        <div class="workspace-toolbar">
          <div class="ws-left">
            <span class="badge-black">DANH SÁCH BIẾN THỂ ({{ variants.length }} SKU)</span>
            <div class="view-switch">
              <button
                type="button"
                class="vs-btn"
                :class="{ active: viewMode === 'matrix' }"
                @click="viewMode = 'matrix'"
              >
                <el-icon><Grid /></el-icon>
                <span>Ma trận gọn</span>
              </button>
              <button
                type="button"
                class="vs-btn"
                :class="{ active: viewMode === 'table' }"
                @click="viewMode = 'table'"
              >
                <el-icon><List /></el-icon>
                <span>Bảng chi tiết</span>
              </button>
            </div>
          </div>

          <div class="ws-right">
            <div v-if="viewMode === 'matrix'" class="matrix-legend-inline">
              <span class="leg"><i class="dot dot-ok"></i>Còn hàng</span>
              <span class="leg"><i class="dot dot-low"></i>Sắp hết (≤5)</span>
              <span class="leg"><i class="dot dot-zero"></i>Hết hàng</span>
            </div>
            <button
              v-if="canManageCatalog"
              type="button"
              class="btn-add-variants"
              @click="goToVariantStudio"
            >
              + Thêm / Sửa hàng loạt SKU
            </button>
          </div>
        </div>

        <!-- VIEW A: COMPACT MATRIX -->
        <div v-if="viewMode === 'matrix'">
          <div v-if="variants.length === 0" class="empty-box">
            <p class="empty-title">Sản phẩm này chưa có biến thể Size / Màu nào</p>
            <p class="empty-sub">Bấm nút bên dưới để mở Ma trận SKU và khởi tạo hàng loạt biến thể ngay.</p>
            <button v-if="canManageCatalog" type="button" class="btn-add-variants" @click="goToVariantStudio">
              ⚡ Tạo tổ hợp biến thể ngay
            </button>
          </div>

          <div v-else class="matrix-scroll">
            <table class="variant-matrix-table">
              <thead>
                <tr>
                  <th class="sticky-color-col">MÀU SẮC ({{ matrixColors.length }})</th>
                  <th v-for="s in matrixSizes" :key="s.key" class="size-col-head">
                    <div>{{ s.label }}</div>
                    <small>{{ countBySize(s.key) }}</small>
                  </th>
                  <th class="row-total-head">TỒN</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in matrixColors" :key="c.key">
                  <td class="sticky-color-col">
                    <div class="matrix-color-cell">
                      <span class="color-swatch" :style="{ backgroundColor: c.hexCode || '#111' }"></span>
                      <div class="color-meta">
                        <strong>{{ c.name }}</strong>
                        <span>{{ countByColor(c.key) }} size</span>
                      </div>
                    </div>
                  </td>
                  <td v-for="s in matrixSizes" :key="s.key" class="matrix-cell">
                    <button
                      v-if="findVariant(s.key, c.key)"
                      type="button"
                      class="cell-card"
                      :class="getCellClass(findVariant(s.key, c.key))"
                      :title="getCellTooltip(findVariant(s.key, c.key))"
                      @click="editVariant(findVariant(s.key, c.key))"
                    >
                      <strong class="qty">{{ findVariant(s.key, c.key).availableQuantity }}</strong>
                      <span class="pr">{{ formatShortPrice(findVariant(s.key, c.key).salePrice) }}</span>
                    </button>
                    <span v-else class="cell-empty">—</span>
                  </td>
                  <td class="row-total-cell">
                    <strong>{{ sumStockByColor(c.key) }}</strong>
                  </td>
                </tr>
              </tbody>
            </table>
            <div class="matrix-tip" v-if="canManageCatalog">
              Mẹo: Bấm trực tiếp vào bất kỳ ô biến thể nào trên Ma trận để mở trang <strong>Cập nhật SKU</strong> ngay tại biến thể đó.
            </div>
          </div>
        </div>

        <!-- VIEW B: FILTERED DETAIL TABLE -->
        <div v-else>
          <div v-if="variants.length > 0" class="table-filter-bar">
            <div class="filter-chips">
              <button
                type="button"
                class="filter-chip"
                :class="{ active: filterColorKey === null }"
                @click="filterColorKey = null"
              >
                Tất cả ({{ variants.length }})
              </button>
              <button
                v-for="c in matrixColors"
                :key="c.key"
                type="button"
                class="filter-chip"
                :class="{ active: filterColorKey === c.key }"
                @click="filterColorKey = c.key"
              >
                <span class="chip-dot" :style="{ backgroundColor: c.hexCode || '#111' }"></span>
                {{ c.name }} ({{ countByColor(c.key) }})
              </button>
            </div>
            <el-input
              v-model="searchSku"
              size="small"
              placeholder="Tìm nhanh SKU / Size..."
              clearable
              style="width: 190px"
            />
          </div>

          <div class="table-scroll">
            <el-table :data="filteredVariants" border stripe size="small" max-height="440" style="width: 100%">
              <template #empty>
                <el-empty description="Không có biến thể nào phù hợp." :image-size="60" />
              </template>
              <el-table-column prop="sku" label="Mã SKU" min-width="150">
                <template #default="{ row }">
                  <span class="code-badge">{{ row.sku }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="sizeValue" label="Size EU" width="90" align="center" />
              <el-table-column label="Màu sắc" min-width="150">
                <template #default="{ row }">
                  <span class="color-dot" :style="{ backgroundColor: row.hexCode || '#111' }"></span>
                  <span>{{ row.colorName }}</span>
                </template>
              </el-table-column>
              <el-table-column label="Giá nhập" width="125" align="right">
                <template #default="{ row }">{{ formatCurrency(row.importPrice) }}</template>
              </el-table-column>
              <el-table-column label="Giá bán" width="125" align="right">
                <template #default="{ row }">
                  <span class="price-text">{{ formatCurrency(row.salePrice) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="availableQuantity" label="Tồn kho" width="90" align="center">
                <template #default="{ row }">
                  <strong :class="row.availableQuantity === 0 ? 'text-red' : row.availableQuantity <= 5 ? 'text-amber' : ''">
                    {{ row.availableQuantity }}
                  </strong>
                </template>
              </el-table-column>
              <el-table-column label="Trạng thái" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="(row.status || 'ACTIVE') === 'ACTIVE' ? 'success' : 'info'" size="small">
                    {{ row.status || 'ACTIVE' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column v-if="canManageCatalog" label="" width="60" align="center" fixed="right">
                <template #default="{ row }">
                  <button type="button" class="action-btn" title="Sửa biến thể này" @click="editVariant(row)">
                    <el-icon><Edit /></el-icon>
                  </button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit, Grid, List, Picture, TopRight } from '@element-plus/icons-vue'
import { productService } from '@/services/productService'
import { formatCurrency } from '@/utils/formatters'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { canManageCatalog } = usePermission()

const loading = ref(false)
const product = ref(null)
const activeImg = ref('')
const imgError = ref(false)
const viewMode = ref('matrix')
const filterColorKey = ref(null)
const searchSku = ref('')

const variants = computed(() => product.value?.variants || [])

const galleryImages = computed(() => {
  if (!product.value) return []
  const urls = Array.isArray(product.value.imageUrls) && product.value.imageUrls.length
    ? product.value.imageUrls
    : product.value.thumbnailUrl ? [product.value.thumbnailUrl] : []
  return urls.filter(Boolean)
})

const currentImg = computed(() => activeImg.value || galleryImages.value[0] || '')
const activeImgIndex = computed(() => {
  const idx = galleryImages.value.indexOf(currentImg.value)
  return idx >= 0 ? idx : 0
})

const profitAmount = computed(() =>
  Number(product.value?.salePrice || 0) - Number(product.value?.importPrice || 0)
)

const profitMarginPct = computed(() => {
  const sale = Number(product.value?.salePrice || 0)
  if (sale <= 0) return 0
  return Math.round((profitAmount.value / sale) * 100)
})

const totalStock = computed(() =>
  variants.value.reduce((acc, v) => acc + Number(v.availableQuantity || 0), 0)
)

const outOfStockCount = computed(() =>
  variants.value.filter((v) => Number(v.availableQuantity || 0) === 0).length
)

function sizeKey(v) {
  return String(v.sizeId ?? v.sizeValue ?? '')
}

function colorKey(v) {
  return String(v.colorId ?? v.colorName ?? '')
}

const matrixSizes = computed(() => {
  const map = new Map()
  for (const v of variants.value) {
    const k = sizeKey(v)
    if (!map.has(k)) {
      map.set(k, { key: k, label: v.sizeValue || k, num: parseFloat(v.sizeValue) || 0 })
    }
  }
  return Array.from(map.values()).sort((a, b) => a.num - b.num)
})

const matrixColors = computed(() => {
  const map = new Map()
  for (const v of variants.value) {
    const k = colorKey(v)
    if (!map.has(k)) {
      map.set(k, { key: k, name: v.colorName || k, hexCode: v.hexCode || '#111111' })
    }
  }
  return Array.from(map.values())
})

const filteredVariants = computed(() => {
  return variants.value.filter((v) => {
    if (filterColorKey.value !== null && colorKey(v) !== filterColorKey.value) return false
    if (searchSku.value && searchSku.value.trim()) {
      const q = searchSku.value.trim().toLowerCase()
      const hay = `${v.sku || ''} ${v.sizeValue || ''} ${v.colorName || ''}`.toLowerCase()
      return hay.includes(q)
    }
    return true
  })
})

function findVariant(sKey, cKey) {
  return variants.value.find((v) => sizeKey(v) === sKey && colorKey(v) === cKey)
}

function countBySize(sKey) {
  return variants.value.filter((v) => sizeKey(v) === sKey).length
}

function countByColor(cKey) {
  return variants.value.filter((v) => colorKey(v) === cKey).length
}

function sumStockByColor(cKey) {
  return variants.value
    .filter((v) => colorKey(v) === cKey)
    .reduce((acc, v) => acc + Number(v.availableQuantity || 0), 0)
}

function formatShortPrice(val) {
  const n = Number(val || 0)
  if (n >= 1000000) {
    const m = n / 1000000
    return `${Number.isInteger(m) ? m : m.toFixed(2).replace(/0$/, '')}tr`
  }
  if (n >= 1000) return `${Math.round(n / 1000)}k`
  return `${n}`
}

function getCellClass(v) {
  const qty = Number(v.availableQuantity || 0)
  return {
    'cell-zero': qty === 0,
    'cell-low': qty > 0 && qty <= 5,
    'cell-ok': qty > 5,
    'cell-inactive': v.status === 'INACTIVE'
  }
}

function getCellTooltip(v) {
  return `SKU: ${v.sku} | Size ${v.sizeValue} - ${v.colorName} | Nhập: ${formatCurrency(v.importPrice)} | Bán: ${formatCurrency(v.salePrice)} | Tồn: ${v.availableQuantity}`
}

function editVariant(v) {
  if (!canManageCatalog.value || !v?.id) return
  router.push(`/admin/variants/${v.id}/edit`)
}

function goToVariantStudio() {
  if (!product.value) return
  if (variants.value.length > 0 && variants.value[0]?.id) {
    router.push(`/admin/variants/${variants.value[0].id}/edit`)
  } else {
    router.push({ path: '/admin/variants/create', query: { productId: product.value.id } })
  }
}

function handleDiscontinue() {
  if (!product.value) return
  ElMessageBox.confirm(
    `Bạn có chắc chắn muốn ngừng kinh doanh sản phẩm "${product.value.productName}"?`,
    'Xác nhận ngừng kinh doanh',
    { type: 'warning' }
  ).then(async () => {
    await productService.discontinueProduct(product.value.id)
    ElMessage.success('Đã chuyển sản phẩm sang trạng thái Ngừng kinh doanh')
    await loadProduct()
  }).catch(() => {})
}

async function loadProduct() {
  loading.value = true
  try {
    const res = await productService.getProductById(route.params.id)
    product.value = res.data
    activeImg.value = galleryImages.value[0] || ''
    imgError.value = false
  } catch (_) {
    ElMessage.error('Không tìm thấy thông tin sản phẩm')
    router.push('/admin/products')
  } finally {
    loading.value = false
  }
}

onMounted(loadProduct)
</script>

<style scoped lang="scss">
.product-studio-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 24px;
}

/* 1. STICKY TOP COMMAND BAR */
.studio-topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  background: #ffffff;
  border: 1px solid #000000;
  padding: 10px 14px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
  min-width: 0;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #ffffff;
  border: 1px solid #d4d4d4;
  color: #171717;
  font-size: 11.5px;
  font-weight: 800;
  padding: 6px 10px;
  cursor: pointer;
  flex-shrink: 0;

  &:hover {
    border-color: #000000;
    background: #000000;
    color: #ffffff;
  }
}

.mode-pill {
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.08em;
  padding: 4px 8px;
  flex-shrink: 0;

  &.mode-detail {
    background: #000000;
    color: #ffffff;
  }
}

.topbar-product-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex-wrap: wrap;

  .title-text {
    margin: 0;
    font-size: 15px;
    font-weight: 900;
    color: #000000;
    letter-spacing: -0.01em;
  }
}

.status-pill {
  font-size: 10px;
  font-weight: 900;
  padding: 2px 7px;
  letter-spacing: 0.05em;

  &.st-active {
    background: #ecfdf5;
    color: #047857;
    border: 1px solid #6ee7b7;
  }
  &.st-inactive {
    background: #f3f4f6;
    color: #525252;
    border: 1px solid #d4d4d4;
  }
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #ffffff;
  border: 1px solid #d4d4d4;
  color: #171717;
  font-size: 11.5px;
  font-weight: 700;
  padding: 6px 11px;
  text-decoration: none;
  cursor: pointer;

  &:hover {
    border-color: #000000;
    color: #000000;
  }
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #fafafa;
  border: 1px solid #000000;
  color: #000000;
  font-size: 11.5px;
  font-weight: 800;
  padding: 6px 11px;
  cursor: pointer;

  &:hover {
    background: #000000;
    color: #ffffff;
  }
}

.btn-primary-cmd {
  font-weight: 800;
  letter-spacing: 0.04em;
}

.studio-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
}

.badge-black {
  background: #000000;
  color: #ffffff;
  font-size: 9.5px;
  font-weight: 900;
  letter-spacing: 0.08em;
  padding: 4px 8px;
  display: inline-block;
}

/* 2. OVERVIEW GRID */
.overview-grid {
  display: grid;
  grid-template-columns: 310px 1fr;
  gap: 12px;
  align-items: stretch;
}

.gallery-card {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .main-img-stage {
    position: relative;
    width: 100%;
    height: 235px;
    background: #fafafa;
    border: 1px solid #e5e5e5;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;

    .main-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .img-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      color: #a3a3a3;
      font-size: 11px;
      font-weight: 800;
    }

    .img-count-badge {
      position: absolute;
      bottom: 8px;
      right: 8px;
      background: rgba(0, 0, 0, 0.78);
      color: #ffffff;
      font-size: 10px;
      font-weight: 800;
      padding: 2px 7px;
      font-family: var(--font-mono);
    }
  }

  .thumb-strip {
    display: flex;
    gap: 6px;
    overflow-x: auto;
    padding-bottom: 2px;

    .thumb-btn {
      position: relative;
      width: 50px;
      height: 50px;
      padding: 0;
      border: 1px solid #d4d4d4;
      background: #fafafa;
      cursor: pointer;
      flex-shrink: 0;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      &.active {
        border: 2px solid #000000;
      }

      .star-mark {
        position: absolute;
        top: 1px;
        left: 2px;
        font-size: 9px;
        color: #f59e0b;
        text-shadow: 0 0 2px #000;
      }
    }
  }
}

.overview-right {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kpi-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;

  .kpi-box {
    background: #ffffff;
    border: 1px solid #e5e5e5;
    padding: 12px 14px;
    display: flex;
    flex-direction: column;
    gap: 3px;

    .kpi-label {
      font-size: 10px;
      font-weight: 800;
      letter-spacing: 0.06em;
      color: #737373;
    }

    .kpi-val {
      font-size: 17px;
      font-weight: 900;
      color: #000000;
      font-variant-numeric: tabular-nums;

      &.price {
        color: #000000;
      }
    }

    .kpi-sub {
      font-size: 11px;
      color: #737373;
      font-weight: 600;
    }
  }
}

.text-green { color: #059669 !important; }
.text-amber { color: #d97706 !important; }
.text-red { color: #dc2626 !important; }

.specs-card {
  padding: 14px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .specs-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  .btn-danger-link {
    background: none;
    border: 1px solid #fca5a5;
    color: #dc2626;
    font-size: 11px;
    font-weight: 800;
    padding: 4px 9px;
    cursor: pointer;

    &:hover {
      background: #dc2626;
      color: #ffffff;
      border-color: #dc2626;
    }
  }

  .specs-grid {
    display: grid;
    grid-template-columns: 1fr 1fr 1.6fr;
    gap: 10px;
    padding-bottom: 10px;
    border-bottom: 1px dashed #e5e5e5;

    .spec-item {
      display: flex;
      flex-direction: column;
      gap: 2px;

      label {
        font-size: 10px;
        font-weight: 800;
        text-transform: uppercase;
        color: #737373;
      }
      strong {
        font-size: 13px;
        color: #171717;
      }
    }
  }

  .desc-block {
    label {
      display: block;
      font-size: 10px;
      font-weight: 800;
      text-transform: uppercase;
      color: #737373;
      margin-bottom: 4px;
    }
    p {
      margin: 0;
      font-size: 13px;
      line-height: 1.55;
      color: #404040;
      white-space: pre-line;
    }
  }
}

/* 3. VARIANT MATRIX WORKSPACE */
.workspace-card {
  padding: 14px;
}

.workspace-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.ws-left,
.ws-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.view-switch {
  display: inline-flex;
  border: 1px solid #000000;

  .vs-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 5px 10px;
    background: #ffffff;
    color: #000000;
    border: none;
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
    cursor: pointer;

    &.active {
      background: #000000;
      color: #ffffff;
    }
  }
}

.matrix-legend-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  color: #525252;

  .leg {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .dot {
    width: 9px;
    height: 9px;
    display: inline-block;
    border: 1px solid #000;

    &.dot-ok { background: #ecfdf5; border-color: #10b981; }
    &.dot-low { background: #fffbeb; border-color: #f59e0b; }
    &.dot-zero { background: #fef2f2; border-color: #ef4444; }
  }
}

.btn-add-variants {
  background: #000000;
  color: #ffffff;
  border: 1px solid #000000;
  padding: 6px 12px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.04em;
  cursor: pointer;

  &:hover {
    background: #262626;
  }
}

.empty-box {
  padding: 32px 16px;
  text-align: center;
  border: 1px dashed #d4d4d4;
  background: #fafafa;

  .empty-title {
    margin: 0;
    font-size: 14px;
    font-weight: 800;
    color: #171717;
  }
  .empty-sub {
    margin: 6px 0 14px;
    font-size: 12px;
    color: #737373;
  }
}

.matrix-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.variant-matrix-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;

  th,
  td {
    border: 1px solid #e5e5e5;
    padding: 5px;
    text-align: center;
    vertical-align: middle;
  }

  thead th {
    background: #111111;
    color: #ffffff;
    font-size: 11px;
    font-weight: 800;
    padding: 7px 6px;

    &.size-col-head {
      min-width: 68px;
      small {
        display: block;
        font-size: 9.5px;
        color: #a3a3a3;
        font-weight: 600;
      }
    }

    &.row-total-head {
      min-width: 54px;
      background: #262626;
    }
  }

  .sticky-color-col {
    position: sticky;
    left: 0;
    z-index: 2;
    min-width: 180px;
    text-align: left;
    background: #fafafa;
  }

  thead .sticky-color-col {
    background: #000000;
    z-index: 3;
    padding-left: 10px;
  }

  .matrix-color-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 2px 4px;

    .color-swatch {
      width: 13px;
      height: 13px;
      border: 1px solid rgba(0, 0, 0, 0.25);
      flex-shrink: 0;
    }

    .color-meta {
      display: flex;
      flex-direction: column;
      line-height: 1.2;

      strong { font-size: 12px; color: #000; }
      span { font-size: 10px; color: #737373; }
    }
  }

  .cell-card {
    width: 100%;
    padding: 4px 3px;
    border: 1px solid #d4d4d4;
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
    transition: all 0.12s ease;

    .qty {
      font-size: 13px;
      font-weight: 900;
      font-family: var(--font-mono);
      line-height: 1.1;
    }
    .pr {
      font-size: 9px;
      font-weight: 700;
      font-family: var(--font-mono);
      opacity: 0.75;
    }

    &.cell-ok {
      background: #ecfdf5;
      border-color: #6ee7b7;
      color: #065f46;
    }
    &.cell-low {
      background: #fffbeb;
      border-color: #fcd34d;
      color: #92400e;
    }
    &.cell-zero {
      background: #fef2f2;
      border-color: #fca5a5;
      color: #b91c1c;
    }
    &.cell-inactive {
      opacity: 0.55;
      border-style: dashed;
    }

    &:hover {
      outline: 2px solid #000000;
      outline-offset: -1px;
    }
  }

  .cell-empty {
    color: #d4d4d4;
    font-weight: 700;
  }

  .row-total-cell {
    background: #fafafa;
    font-family: var(--font-mono);
    font-size: 12px;
    color: #000000;
  }
}

.matrix-tip {
  margin-top: 8px;
  font-size: 11px;
  color: #737373;
}

.table-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;

  .filter-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
  }

  .filter-chip {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 4px 9px;
    background: #ffffff;
    border: 1px solid #d4d4d4;
    font-size: 11px;
    font-weight: 700;
    color: #262626;
    cursor: pointer;

    &.active {
      background: #000000;
      border-color: #000000;
      color: #ffffff;
    }

    .chip-dot {
      width: 8px;
      height: 8px;
      border: 1px solid rgba(0, 0, 0, 0.25);
      display: inline-block;
    }
  }
}

.table-scroll {
  overflow-x: auto;
}

@media (max-width: 1024px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
  .kpi-strip {
    grid-template-columns: repeat(2, 1fr);
  }
  .specs-card .specs-grid {
    grid-template-columns: 1fr 1fr;
    .spec-wide {
      grid-column: span 2;
    }
  }
}

@media (max-width: 640px) {
  .studio-topbar {
    position: static;
  }
  .topbar-right {
    width: 100%;
    .btn-primary-cmd {
      flex: 1;
    }
  }
  .kpi-strip {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
