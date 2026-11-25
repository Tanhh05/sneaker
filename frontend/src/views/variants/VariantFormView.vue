<template>
  <div class="page-container variant-form-page" v-loading="loading">
    <!-- Top Header -->
    <div class="page-header">
      <div class="page-title-group">
        <div class="title-row">
          <button type="button" class="btn-back" @click="$router.push('/admin/variants')">
            ← Quay lại
          </button>
          <h2 class="page-title">
            {{ isEdit ? 'CẬP NHẬT BIẾN THỂ SẢN PHẨM (MA TRẬN & HÀNG LOẠT)' : 'THÊM NHIỀU BIẾN THỂ SẢN PHẨM (SKU)' }}
          </h2>
        </div>
        <p class="page-subtitle">
          {{
            isEdit
              ? 'Chỉnh sửa hàng loạt toàn bộ biến thể (SKU, giá nhập, giá bán, tồn kho) của sản phẩm hoặc bấm + để bổ sung thêm biến thể mới ngay tại đây'
              : 'Chọn 1 sản phẩm Sneaker → tick nhiều Size & Màu → xem và chỉnh toàn bộ biến thể trên 1 màn hình dạng Ma trận gọn gàng'
          }}
        </p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/admin/variants')">Hủy bỏ</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="!selectedProductId || totalPendingChanges === 0"
          @click="submitAllChanges"
        >
          <el-icon style="margin-right: 6px"><Check /></el-icon>
          {{ saveButtonLabel }}
        </el-button>
      </div>
    </div>

    <!-- STEP 1: SELECT PRODUCT -->
    <div class="form-section-card">
      <div class="section-header">
        <span class="step-badge">BƯỚC 1</span>
        <div>
          <h3>{{ isEdit ? 'Sản phẩm Sneaker đang cập nhật' : 'Chọn sản phẩm Sneaker' }}</h3>
          <p>
            {{
              isEdit
                ? 'Đang nạp toàn bộ biến thể hiện có của sản phẩm (có thể chuyển sang sản phẩm khác nếu muốn)'
                : 'Các biến thể được thêm sẽ gắn trực tiếp vào sản phẩm này'
            }}
          </p>
        </div>
      </div>

      <el-row :gutter="20" align="middle">
        <el-col :xs="24" :md="12">
          <el-select
            v-model="selectedProductId"
            placeholder="Tìm và chọn sản phẩm Sneaker..."
            filterable
            size="large"
            style="width: 100%"
            @change="handleProductChange"
          >
            <el-option
              v-for="p in products"
              :key="p.id"
              :label="`${p.productCode} — ${p.productName}`"
              :value="p.id"
            >
              <div class="product-option-row">
                <span class="code-badge">{{ p.productCode }}</span>
                <span class="p-name">{{ p.productName }}</span>
                <span class="p-price">{{ formatCurrency(p.salePrice) }}</span>
              </div>
            </el-option>
          </el-select>
        </el-col>

        <el-col :xs="24" :md="12" v-if="selectedProduct">
          <div class="selected-product-summary">
            <img
              v-if="selectedProduct.thumbnailUrl"
              :src="selectedProduct.thumbnailUrl"
              class="summary-thumb"
              alt=""
            />
            <div class="summary-info">
              <div class="summary-top">
                <span class="code-badge">{{ selectedProduct.productCode }}</span>
                <strong>{{ selectedProduct.productName }}</strong>
              </div>
              <div class="summary-meta">
                <span>Giá nhập: <strong>{{ formatCurrency(selectedProduct.importPrice) }}</strong></span>
                <span>•</span>
                <span>Giá bán: <strong class="price-text">{{ formatCurrency(selectedProduct.salePrice) }}</strong></span>
                <span>•</span>
                <span>Đang có: <strong>{{ existingRowsCount }} SKU</strong></span>
                <span v-if="newRowsCount > 0">•</span>
                <span v-if="newRowsCount > 0" class="text-green">Thêm mới: <strong>+{{ newRowsCount }}</strong></span>
                <span v-if="modifiedRowsCount > 0">•</span>
                <span v-if="modifiedRowsCount > 0" class="text-amber">Đã sửa: <strong>{{ modifiedRowsCount }}</strong></span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- STEP 2: QUICK MATRIX GENERATOR & BULK DEFAULTS -->
    <div class="form-section-card" v-if="selectedProduct">
      <div class="section-header between">
        <div class="header-left-group">
          <span class="step-badge">BƯỚC 2</span>
          <div>
            <h3>{{ isEdit ? 'Bổ sung nhanh tổ hợp biến thể & Áp dụng giá/SL hàng loạt' : 'Bộ sinh tổ hợp biến thể hàng loạt (Size × Màu sắc)' }}</h3>
            <p>Tick chọn nhiều Size và nhiều Màu để sinh thêm SKU mới, hoặc nhập giá/SL mặc định để áp dụng nhanh cho toàn bộ bảng</p>
          </div>
        </div>
        <button
          v-if="isEdit"
          type="button"
          class="btn-toggle-step2"
          @click="showGenerator = !showGenerator"
        >
          {{ showGenerator ? 'Thu gọn bộ sinh tổ hợp ▲' : 'Mở rộng bộ sinh tổ hợp Size × Màu ▼' }}
        </button>
      </div>

      <el-row v-if="showGenerator" :gutter="24">
        <!-- Multi Size Selector -->
        <el-col :xs="24" :md="12" class="picker-col">
          <div class="picker-label-row">
            <span class="picker-label">Chọn nhiều Size EU ({{ selectedSizeIds.length }} đã chọn)</span>
            <div class="picker-quick-links">
              <button type="button" class="link-btn" @click="selectAllSizes">Chọn tất cả</button>
              <span>/</span>
              <button type="button" class="link-btn" @click="selectedSizeIds = []">Bỏ chọn</button>
            </div>
          </div>
          <div class="pill-grid">
            <button
              v-for="s in sizes"
              :key="s.id"
              type="button"
              class="option-pill"
              :class="{ active: selectedSizeIds.includes(s.id) }"
              @click="toggleId(selectedSizeIds, s.id)"
            >
              Size {{ s.sizeValue }}
            </button>
          </div>
        </el-col>

        <!-- Multi Color Selector -->
        <el-col :xs="24" :md="12" class="picker-col">
          <div class="picker-label-row">
            <span class="picker-label">Chọn nhiều Màu sắc ({{ selectedColorIds.length }} đã chọn)</span>
            <div class="picker-quick-links">
              <button type="button" class="link-btn" @click="selectAllColors">Chọn tất cả</button>
              <span>/</span>
              <button type="button" class="link-btn" @click="selectedColorIds = []">Bỏ chọn</button>
            </div>
          </div>
          <div class="pill-grid">
            <button
              v-for="c in colors"
              :key="c.id"
              type="button"
              class="option-pill color-pill"
              :class="{ active: selectedColorIds.includes(c.id) }"
              @click="toggleId(selectedColorIds, c.id)"
            >
              <span class="color-swatch" :style="{ backgroundColor: c.hexCode }"></span>
              <span>{{ c.colorName }} ({{ c.colorCode }})</span>
            </button>
          </div>
        </el-col>
      </el-row>

      <!-- Default Bulk Values -->
      <div class="defaults-row" :class="{ 'no-top-border': !showGenerator }">
        <div class="default-field">
          <label>Giá nhập mặc định (VNĐ)</label>
          <el-input-number v-model="defaults.importPrice" :min="0" :step="50000" style="width: 100%" />
        </div>
        <div class="default-field">
          <label>Giá bán mặc định (VNĐ)</label>
          <el-input-number v-model="defaults.salePrice" :min="0" :step="50000" style="width: 100%" />
        </div>
        <div class="default-field">
          <label>Số lượng tồn mặc định</label>
          <el-input-number v-model="defaults.availableQuantity" :min="0" style="width: 100%" />
        </div>
        <div class="default-field">
          <label>Trạng thái mặc định</label>
          <el-select v-model="defaults.status" style="width: 100%">
            <el-option label="ACTIVE (Đang bán)" value="ACTIVE" />
            <el-option label="INACTIVE (Tạm ẩn)" value="INACTIVE" />
          </el-select>
        </div>
      </div>

      <div class="generator-actions">
        <button
          v-if="showGenerator"
          type="button"
          class="btn-generate-matrix"
          :disabled="selectedSizeIds.length === 0 || selectedColorIds.length === 0"
          @click="generateMatrixVariants"
        >
          ⚡ TẠO TỔ HỢP BIẾN THỂ ({{ selectedSizeIds.length * selectedColorIds.length }} tổ hợp)
        </button>
        <el-button @click="addManualRow">
          <el-icon style="margin-right: 6px"><Plus /></el-icon> Thêm 1 dòng thủ công
        </el-button>
        <el-button :disabled="allVariantRows.length === 0" @click="applyPricesToAll">
          Áp dụng giá mặc định cho tất cả
        </el-button>
        <el-button :disabled="allVariantRows.length === 0" @click="applyStockToAll">
          Áp dụng SL tồn cho tất cả
        </el-button>
      </div>
    </div>

    <!-- STEP 3: UNIFIED COMPACT MATRIX & FILTERED TABLE -->
    <div class="form-section-card" v-if="selectedProduct">
      <div class="section-header between">
        <div class="header-left-group">
          <span class="step-badge">BƯỚC 3</span>
          <div>
            <h3>
              {{
                isEdit
                  ? `Toàn bộ biến thể của ${selectedProduct.productCode} (${allVariantRows.length} SKU)`
                  : `Danh sách biến thể chuẩn bị lưu (${newRowsCount} biến thể mới)`
              }}
            </h3>
            <p>
              {{
                viewMode === 'matrix'
                  ? 'Xem toàn bộ biến thể trên 1 khung Ma trận (Màu × Size) — gõ trực tiếp số lượng tồn, hoặc chuyển Bảng chi tiết để sửa SKU & giá'
                  : 'Chế độ bảng chi tiết có lọc theo Màu & Size — sửa trực tiếp SKU, giá nhập, giá bán, tồn kho và trạng thái từng dòng'
              }}
            </p>
          </div>
        </div>

        <div class="table-tools">
          <div class="view-mode-switch">
            <button
              type="button"
              class="mode-btn"
              :class="{ active: viewMode === 'matrix' }"
              @click="viewMode = 'matrix'"
            >
              <el-icon><Grid /></el-icon> Ma trận gọn
            </button>
            <button
              type="button"
              class="mode-btn"
              :class="{ active: viewMode === 'table' }"
              @click="viewMode = 'table'"
            >
              <el-icon><List /></el-icon> Bảng chi tiết
            </button>
          </div>
          <el-button
            v-if="newRowsCount > 0"
            size="small"
            type="danger"
            plain
            @click="clearNewRows"
          >
            Xóa các dòng mới ({{ newRowsCount }})
          </el-button>
        </div>
      </div>

      <!-- ========================================= -->
      <!-- VIEW A: COMPACT MATRIX GRID (ZERO SCROLL) -->
      <!-- ========================================= -->
      <div v-if="viewMode === 'matrix'" class="matrix-view-wrap">
        <div v-if="matrixColors.length === 0 || matrixSizes.length === 0" class="empty-matrix-box">
          <el-empty
            description="Chưa có biến thể nào. Hãy chọn Size & Màu ở Bước 2 rồi bấm Tạo tổ hợp để hiển thị Ma trận gọn!"
            :image-size="72"
          />
        </div>

        <div v-else class="matrix-table-scroll">
          <div class="matrix-legend">
            <span class="legend-item"><i class="dot dot-exist"></i> Đã có (sửa trực tiếp SL)</span>
            <span class="legend-item"><i class="dot dot-modified"></i> Đã chỉnh sửa (chờ lưu)</span>
            <span class="legend-item"><i class="dot dot-new"></i> Biến thể mới (chờ tạo)</span>
            <span class="legend-item"><i class="dot dot-empty"></i> + = Bấm thêm biến thể vào ô trống</span>
          </div>

          <table class="variant-matrix-table">
            <thead>
              <tr>
                <th class="sticky-color-col">MÀU SẮC ({{ matrixColors.length }})</th>
                <th v-for="s in matrixSizes" :key="s.id" class="size-col-head">
                  <div>{{ s.sizeValue }}</div>
                  <span class="col-count">{{ countTotalBySize(s.id) }}</span>
                </th>
                <th class="row-total-head">TỔNG</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in matrixColors" :key="c.id">
                <td class="sticky-color-col">
                  <div class="matrix-color-cell">
                    <span class="color-swatch" :style="{ backgroundColor: c.hexCode }"></span>
                    <div class="color-meta">
                      <strong>{{ c.colorName }}</strong>
                      <span>{{ c.colorCode }} • {{ countTotalByColor(c.id) }} size</span>
                    </div>
                    <button
                      v-if="countNewByColor(c.id) > 0"
                      type="button"
                      class="btn-clear-row"
                      title="Bỏ các biến thể mới của màu này"
                      @click="removeNewColorRows(c.id)"
                    >
                      ✕
                    </button>
                  </div>
                </td>

                <td v-for="s in matrixSizes" :key="s.id" class="matrix-cell">
                  <!-- Variant exists in list (either DB existing or newly added draft) -->
                  <div
                    v-if="findRow(s.id, c.id)"
                    class="cell-box"
                    :class="getCellClass(findRow(s.id, c.id))"
                  >
                    <input
                      type="number"
                      min="0"
                      class="cell-qty-input"
                      :value="findRow(s.id, c.id).availableQuantity"
                      :title="getCellTooltip(findRow(s.id, c.id))"
                      @input="onMatrixQtyInput(s.id, c.id, $event.target.value)"
                    />
                    <button
                      v-if="findRow(s.id, c.id).isNew"
                      type="button"
                      class="cell-remove-btn"
                      title="Xóa biến thể mới này"
                      @click="removeNewByCombo(s.id, c.id)"
                    >
                      ×
                    </button>
                    <button
                      v-else
                      type="button"
                      class="cell-detail-btn"
                      title="Sửa chi tiết SKU & giá dòng này"
                      @click="focusRowInTable(findRow(s.id, c.id))"
                    >
                      ✎
                    </button>
                  </div>

                  <!-- Empty slot: click to add instantly -->
                  <button
                    v-else
                    type="button"
                    class="cell-add-btn"
                    title="Bấm để thêm biến thể mới vào ô này"
                    @click="addSingleCombo(s.id, c.id)"
                  >
                    +
                  </button>
                </td>

                <td class="row-total-cell">
                  <strong>{{ countTotalByColor(c.id) }}</strong>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ================================================ -->
      <!-- VIEW B: FILTERED COMPACT TABLE (PER-ROW EDITING) -->
      <!-- ================================================ -->
      <div v-else>
        <!-- Quick Color & Status Filter Pills -->
        <div v-if="allVariantRows.length > 0" class="table-filter-bar">
          <div class="filter-chips">
            <button
              type="button"
              class="filter-chip"
              :class="{ active: tableFilterColorId === null }"
              @click="tableFilterColorId = null"
            >
              Tất cả ({{ allVariantRows.length }})
            </button>
            <button
              v-for="c in rowColorTabs"
              :key="c.id"
              type="button"
              class="filter-chip"
              :class="{ active: tableFilterColorId === c.id }"
              @click="tableFilterColorId = c.id"
            >
              <span class="chip-dot" :style="{ backgroundColor: c.hexCode }"></span>
              {{ c.colorName }} ({{ countTotalByColor(c.id) }})
            </button>
          </div>
          <el-input
            v-model="tableSearchSku"
            size="small"
            placeholder="Lọc nhanh SKU / Size..."
            clearable
            style="width: 190px"
          />
        </div>

        <div class="table-scroll-wrap">
          <el-table
            :data="filteredVariantRows"
            border
            stripe
            max-height="440"
            :row-class-name="tableRowClassName"
            style="width: 100%"
          >
            <template #empty>
              <el-empty description="Chưa có biến thể nào phù hợp." :image-size="64" />
            </template>

            <el-table-column label="Loại" width="76" align="center">
              <template #default="{ row }">
                <span v-if="row.isNew" class="row-badge badge-new">MỚI</span>
                <span v-else-if="isRowModified(row)" class="row-badge badge-mod">ĐÃ SỬA</span>
                <span v-else class="row-badge badge-db">ĐÃ CÓ</span>
              </template>
            </el-table-column>

            <el-table-column label="Size EU" width="120">
              <template #default="{ row }">
                <el-select v-model="row.sizeId" size="small" style="width: 100%" @change="refreshRowSku(row)">
                  <el-option v-for="s in sizes" :key="s.id" :label="`Size ${s.sizeValue}`" :value="s.id" />
                </el-select>
              </template>
            </el-table-column>

            <el-table-column label="Màu sắc" min-width="165">
              <template #default="{ row }">
                <el-select v-model="row.colorId" size="small" style="width: 100%" @change="refreshRowSku(row)">
                  <el-option
                    v-for="c in colors"
                    :key="c.id"
                    :label="`${c.colorName} (${c.colorCode})`"
                    :value="c.id"
                  />
                </el-select>
              </template>
            </el-table-column>

            <el-table-column label="Mã SKU" min-width="165">
              <template #default="{ row }">
                <el-input v-model="row.sku" size="small" placeholder="VD: AF1-41-W" />
                <div v-if="getRowError(row)" class="row-error-text">{{ getRowError(row) }}</div>
              </template>
            </el-table-column>

            <el-table-column label="Giá nhập (VNĐ)" width="145">
              <template #default="{ row }">
                <el-input-number v-model="row.importPrice" :min="0" :step="50000" size="small" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>

            <el-table-column label="Giá bán (VNĐ)" width="145">
              <template #default="{ row }">
                <el-input-number v-model="row.salePrice" :min="0" :step="50000" size="small" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>

            <el-table-column label="SL sẵn có" width="105">
              <template #default="{ row }">
                <el-input-number v-model="row.availableQuantity" :min="0" size="small" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>

            <el-table-column label="Trạng thái" width="115">
              <template #default="{ row }">
                <el-select v-model="row.status" size="small" style="width: 100%">
                  <el-option label="ACTIVE" value="ACTIVE" />
                  <el-option label="INACTIVE" value="INACTIVE" />
                </el-select>
              </template>
            </el-table-column>

            <el-table-column label="Thao tác" width="68" align="center" fixed="right">
              <template #default="{ row }">
                <button
                  v-if="row.isNew"
                  type="button"
                  class="action-btn danger"
                  title="Xóa dòng mới này"
                  @click="removeRowByRef(row)"
                >
                  <el-icon><Delete /></el-icon>
                </button>
                <button
                  v-else-if="isRowModified(row)"
                  type="button"
                  class="action-btn"
                  title="Hoàn tác thay đổi dòng này"
                  @click="revertRow(row)"
                >
                  ↺
                </button>
                <span v-else class="muted-dash">—</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- Bottom Submit Bar -->
      <div class="bottom-submit-bar" v-if="allVariantRows.length > 0">
        <span class="summary-count">
          Tổng <strong>{{ allVariantRows.length }}</strong> SKU của <strong>{{ selectedProduct.productName }}</strong>
          <template v-if="modifiedRowsCount > 0">
            • <strong class="text-amber">{{ modifiedRowsCount }}</strong> chỉnh sửa
          </template>
          <template v-if="newRowsCount > 0">
            • <strong class="text-green">+{{ newRowsCount }}</strong> thêm mới
          </template>
          (Tổng tồn kho: <strong>{{ totalStockQty }}</strong> đôi)
        </span>
        <div class="bottom-btns">
          <el-button @click="$router.push('/admin/variants')">Quay lại</el-button>
          <el-button
            type="primary"
            size="large"
            :loading="saving"
            :disabled="totalPendingChanges === 0"
            @click="submitAllChanges"
          >
            <el-icon style="margin-right: 6px"><Check /></el-icon>
            {{ saveButtonLabel }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Check, Grid, List } from '@element-plus/icons-vue'
import { variantService } from '@/services/variantService'
import { productService } from '@/services/productService'
import { formatCurrency } from '@/utils/formatters'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.params.id)
const focusedVariantId = ref(null)
const loading = ref(false)
const saving = ref(false)
const showGenerator = ref(true)

const products = ref([])
const sizes = ref([])
const colors = ref([])

const selectedProductId = ref(null)
const selectedSizeIds = ref([])
const selectedColorIds = ref([])

// Unified list containing both existing DB rows (isNew: false) and newly added rows (isNew: true)
const allVariantRows = ref([])

const viewMode = ref('matrix')
const tableFilterColorId = ref(null)
const tableSearchSku = ref('')

const defaults = reactive({
  importPrice: 1800000,
  salePrice: 2950000,
  availableQuantity: 10,
  status: 'ACTIVE'
})

const selectedProduct = computed(() =>
  products.value.find((p) => p.id === selectedProductId.value) || null
)

function isRowModified(row) {
  if (row.isNew || !row._orig) return false
  return (
    row.sizeId !== row._orig.sizeId ||
    row.colorId !== row._orig.colorId ||
    (row.sku || '').trim().toUpperCase() !== (row._orig.sku || '').trim().toUpperCase() ||
    Number(row.importPrice) !== Number(row._orig.importPrice) ||
    Number(row.salePrice) !== Number(row._orig.salePrice) ||
    Number(row.availableQuantity) !== Number(row._orig.availableQuantity) ||
    row.status !== row._orig.status
  )
}

const existingRowsCount = computed(() => allVariantRows.value.filter((r) => !r.isNew).length)
const newRowsCount = computed(() => allVariantRows.value.filter((r) => r.isNew).length)
const modifiedRowsCount = computed(() => allVariantRows.value.filter((r) => isRowModified(r)).length)
const totalPendingChanges = computed(() => newRowsCount.value + modifiedRowsCount.value)

const saveButtonLabel = computed(() => {
  if (totalPendingChanges.value === 0) {
    return isEdit.value ? 'CHƯA CÓ THAY ĐỔI' : 'CHƯA CÓ BIẾN THỂ MỚI'
  }
  const parts = []
  if (modifiedRowsCount.value > 0) parts.push(`CẬP NHẬT ${modifiedRowsCount.value}`)
  if (newRowsCount.value > 0) parts.push(`THÊM ${newRowsCount.value}`)
  return `LƯU (${parts.join(' + ')} SKU)`
})

// Matrix axes: show all sizes & colors present in rows or selected pills
const matrixSizes = computed(() => {
  const idSet = new Set([
    ...selectedSizeIds.value,
    ...allVariantRows.value.map((r) => r.sizeId)
  ])
  if (idSet.size === 0) return []
  return sizes.value.filter((s) => idSet.has(s.id))
})

const matrixColors = computed(() => {
  const idSet = new Set([
    ...selectedColorIds.value,
    ...allVariantRows.value.map((r) => r.colorId)
  ])
  if (idSet.size === 0) return []
  return colors.value.filter((c) => idSet.has(c.id))
})

const rowColorTabs = computed(() => {
  const idSet = new Set(allVariantRows.value.map((r) => r.colorId))
  return colors.value.filter((c) => idSet.has(c.id))
})

const filteredVariantRows = computed(() => {
  return allVariantRows.value.filter((row) => {
    if (tableFilterColorId.value !== null && row.colorId !== tableFilterColorId.value) {
      return false
    }
    if (tableSearchSku.value && tableSearchSku.value.trim()) {
      const q = tableSearchSku.value.trim().toLowerCase()
      const s = sizes.value.find((x) => x.id === row.sizeId)
      const c = colors.value.find((x) => x.id === row.colorId)
      const hay = `${row.sku || ''} ${s?.sizeValue || ''} ${c?.colorName || ''}`.toLowerCase()
      return hay.includes(q)
    }
    return true
  })
})

const totalStockQty = computed(() =>
  allVariantRows.value.reduce((acc, r) => acc + Number(r.availableQuantity || 0), 0)
)

function toggleId(arr, id) {
  const idx = arr.indexOf(id)
  if (idx >= 0) {
    arr.splice(idx, 1)
  } else {
    arr.push(id)
  }
}

function selectAllSizes() {
  selectedSizeIds.value = sizes.value.map((s) => s.id)
}

function selectAllColors() {
  selectedColorIds.value = colors.value.map((c) => c.id)
}

function buildSku(product, sizeId, colorId) {
  const s = sizes.value.find((x) => x.id === sizeId)
  const c = colors.value.find((x) => x.id === colorId)
  if (product && s && c) {
    return `${product.productCode}-${s.sizeValue}-${c.colorCode}`.toUpperCase()
  }
  return ''
}

function refreshRowSku(row) {
  if (!selectedProduct.value || !row.isNew) return
  const generated = buildSku(selectedProduct.value, row.sizeId, row.colorId)
  if (generated) {
    row.sku = generated
  }
}

function findRow(sizeId, colorId) {
  return allVariantRows.value.find((r) => r.sizeId === sizeId && r.colorId === colorId)
}

function countTotalByColor(colorId) {
  return allVariantRows.value.filter((r) => r.colorId === colorId).length
}

function countNewByColor(colorId) {
  return allVariantRows.value.filter((r) => r.isNew && r.colorId === colorId).length
}

function countTotalBySize(sizeId) {
  return allVariantRows.value.filter((r) => r.sizeId === sizeId).length
}

function getCellClass(row) {
  return {
    'cell-new': row.isNew,
    'cell-modified': !row.isNew && isRowModified(row),
    'cell-existing-edit': !row.isNew && !isRowModified(row),
    'cell-focused': row.id && row.id === focusedVariantId.value
  }
}

function getCellTooltip(row) {
  const statusLabel = row.isNew ? '[MỚI]' : isRowModified(row) ? '[ĐÃ SỬA]' : '[ĐÃ CÓ]'
  return `${statusLabel} SKU: ${row.sku} | Nhập: ${formatCurrency(row.importPrice)} | Bán: ${formatCurrency(row.salePrice)}`
}

function tableRowClassName({ row }) {
  if (row.id && row.id === focusedVariantId.value) return 'focused-table-row'
  if (row.isNew) return 'new-table-row'
  if (isRowModified(row)) return 'modified-table-row'
  return ''
}

function onMatrixQtyInput(sizeId, colorId, rawVal) {
  const row = findRow(sizeId, colorId)
  if (!row) return
  const num = Number(rawVal)
  row.availableQuantity = Number.isFinite(num) && num >= 0 ? Math.floor(num) : 0
}

function focusRowInTable(row) {
  focusedVariantId.value = row.id || null
  tableFilterColorId.value = row.colorId
  tableSearchSku.value = ''
  viewMode.value = 'table'
}

function addSingleCombo(sizeId, colorId) {
  if (!selectedProduct.value || findRow(sizeId, colorId)) return
  allVariantRows.value.push({
    id: null,
    isNew: true,
    sizeId,
    colorId,
    sku: buildSku(selectedProduct.value, sizeId, colorId),
    importPrice: defaults.importPrice,
    salePrice: defaults.salePrice,
    availableQuantity: defaults.availableQuantity,
    status: defaults.status
  })
}

function removeNewByCombo(sizeId, colorId) {
  const idx = allVariantRows.value.findIndex((r) => r.isNew && r.sizeId === sizeId && r.colorId === colorId)
  if (idx >= 0) {
    allVariantRows.value.splice(idx, 1)
  }
}

function removeNewColorRows(colorId) {
  allVariantRows.value = allVariantRows.value.filter((r) => !(r.isNew && r.colorId === colorId))
}

function clearNewRows() {
  allVariantRows.value = allVariantRows.value.filter((r) => !r.isNew)
}

function removeRowByRef(row) {
  if (!row.isNew) return
  const idx = allVariantRows.value.indexOf(row)
  if (idx >= 0) {
    allVariantRows.value.splice(idx, 1)
  }
}

function revertRow(row) {
  if (!row._orig) return
  row.sizeId = row._orig.sizeId
  row.colorId = row._orig.colorId
  row.sku = row._orig.sku
  row.importPrice = row._orig.importPrice
  row.salePrice = row._orig.salePrice
  row.availableQuantity = row._orig.availableQuantity
  row.status = row._orig.status
}

function getRowError(row) {
  if (!row.sizeId || !row.colorId) return 'Thiếu Size hoặc Màu'
  if (!row.sku || !row.sku.trim()) return 'Chưa nhập mã SKU'

  const dupCombo = allVariantRows.value.filter(
    (r) => r.sizeId === row.sizeId && r.colorId === row.colorId
  ).length
  if (dupCombo > 1) return 'Trùng tổ hợp Size + Màu'

  const normalizedSku = row.sku.trim().toUpperCase()
  const dupSku = allVariantRows.value.filter(
    (r) => (r.sku || '').trim().toUpperCase() === normalizedSku
  ).length
  if (dupSku > 1) return 'Trùng mã SKU'

  return ''
}

function mapExistingToRow(ev) {
  const orig = {
    sizeId: ev.sizeId,
    colorId: ev.colorId,
    sku: ev.sku,
    importPrice: Number(ev.importPrice || 0),
    salePrice: Number(ev.salePrice || 0),
    availableQuantity: Number(ev.availableQuantity || 0),
    status: ev.status || 'ACTIVE'
  }
  return {
    id: ev.id,
    isNew: false,
    ...orig,
    _orig: { ...orig }
  }
}

async function handleProductChange(productId) {
  allVariantRows.value = []
  tableFilterColorId.value = null
  tableSearchSku.value = ''
  if (!productId) return

  const p = products.value.find((x) => x.id === productId)
  if (p) {
    defaults.importPrice = Number(p.importPrice || 0)
    defaults.salePrice = Number(p.salePrice || 0)
  }

  try {
    const res = await variantService.getVariantsByProduct(productId)
    const list = res.data || []
    allVariantRows.value = list.map(mapExistingToRow)
  } catch (_) {
    allVariantRows.value = []
  }
}

function generateMatrixVariants() {
  if (!selectedProduct.value) {
    ElMessage.warning('Vui lòng chọn sản phẩm trước')
    return
  }
  let added = 0
  let skipped = 0

  for (const sizeId of selectedSizeIds.value) {
    for (const colorId of selectedColorIds.value) {
      if (findRow(sizeId, colorId)) {
        skipped++
        continue
      }
      allVariantRows.value.push({
        id: null,
        isNew: true,
        sizeId,
        colorId,
        sku: buildSku(selectedProduct.value, sizeId, colorId),
        importPrice: defaults.importPrice,
        salePrice: defaults.salePrice,
        availableQuantity: defaults.availableQuantity,
        status: defaults.status
      })
      added++
    }
  }

  if (added > 0) {
    ElMessage.success(
      `Đã tạo ${added} biến thể mới` + (skipped > 0 ? ` (bỏ qua ${skipped} tổ hợp đã có sẵn)` : '')
    )
  } else {
    ElMessage.info('Tất cả các tổ hợp Size × Màu anh chọn đã có trong bảng rồi!')
  }
}

function addManualRow() {
  if (!selectedProduct.value) {
    ElMessage.warning('Vui lòng chọn sản phẩm trước')
    return
  }
  let pickSize = sizes.value[0]?.id || null
  let pickColor = colors.value[0]?.id || null
  outer: for (const s of sizes.value) {
    for (const c of colors.value) {
      if (!findRow(s.id, c.id)) {
        pickSize = s.id
        pickColor = c.id
        break outer
      }
    }
  }

  allVariantRows.value.push({
    id: null,
    isNew: true,
    sizeId: pickSize,
    colorId: pickColor,
    sku: buildSku(selectedProduct.value, pickSize, pickColor),
    importPrice: defaults.importPrice,
    salePrice: defaults.salePrice,
    availableQuantity: defaults.availableQuantity,
    status: defaults.status
  })
  viewMode.value = 'table'
}

function applyPricesToAll() {
  allVariantRows.value.forEach((row) => {
    row.importPrice = defaults.importPrice
    row.salePrice = defaults.salePrice
  })
  ElMessage.success('Đã áp dụng Giá nhập & Giá bán mặc định cho toàn bộ biến thể')
}

function applyStockToAll() {
  allVariantRows.value.forEach((row) => {
    row.availableQuantity = defaults.availableQuantity
    row.status = defaults.status
  })
  ElMessage.success('Đã áp dụng Số lượng tồn & Trạng thái mặc định cho toàn bộ biến thể')
}

async function submitAllChanges() {
  if (!selectedProductId.value) {
    ElMessage.warning('Vui lòng chọn sản phẩm Sneaker')
    return
  }

  const rowsToCreate = allVariantRows.value.filter((r) => r.isNew)
  const rowsToUpdate = allVariantRows.value.filter((r) => !r.isNew && isRowModified(r))

  if (rowsToCreate.length === 0 && rowsToUpdate.length === 0) {
    ElMessage.info('Chưa có thay đổi nào cần lưu')
    return
  }

  const invalidRow = [...rowsToUpdate, ...rowsToCreate].find((row) => !!getRowError(row))
  if (invalidRow) {
    viewMode.value = 'table'
    ElMessage.error(`Vui lòng kiểm tra dòng SKU "${invalidRow.sku || '(trống)'}": ${getRowError(invalidRow)}`)
    return
  }

  saving.value = true
  let updatedCount = 0
  let createdCount = 0
  let errorCount = 0

  try {
    for (const row of rowsToUpdate) {
      try {
        await variantService.updateVariant(row.id, {
          productId: selectedProductId.value,
          sizeId: row.sizeId,
          colorId: row.colorId,
          sku: row.sku.trim().toUpperCase(),
          importPrice: row.importPrice,
          salePrice: row.salePrice,
          availableQuantity: row.availableQuantity,
          status: row.status
        })
        updatedCount++
      } catch (_) {
        errorCount++
      }
    }

    for (const row of rowsToCreate) {
      try {
        await variantService.createVariant({
          productId: selectedProductId.value,
          sizeId: row.sizeId,
          colorId: row.colorId,
          sku: row.sku.trim().toUpperCase(),
          importPrice: row.importPrice,
          salePrice: row.salePrice,
          availableQuantity: row.availableQuantity,
          status: row.status
        })
        createdCount++
      } catch (_) {
        errorCount++
      }
    }

    if (errorCount === 0) {
      const msgs = []
      if (updatedCount > 0) msgs.push(`cập nhật ${updatedCount} biến thể`)
      if (createdCount > 0) msgs.push(`thêm mới ${createdCount} biến thể`)
      ElMessage.success(`Đã ${msgs.join(' và ')} thành công!`)
      router.push('/admin/variants')
    } else {
      await handleProductChange(selectedProductId.value)
      ElMessage.warning(
        `Hoàn tất: cập nhật ${updatedCount}, thêm mới ${createdCount}, lỗi ${errorCount} dòng — vui lòng kiểm tra lại!`
      )
    }
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [pRes, sRes, cRes] = await Promise.all([
      productService.getAllProducts(),
      variantService.getSizes(),
      variantService.getColors()
    ])
    products.value = pRes.data || []
    sizes.value = sRes.data || []
    colors.value = cRes.data || []

    if (isEdit.value) {
      showGenerator.value = false
      focusedVariantId.value = Number(route.params.id)
      const vRes = await variantService.getVariantById(route.params.id)
      const v = vRes.data
      if (v) {
        selectedProductId.value = v.productId
        await handleProductChange(v.productId)
      }
    } else {
      showGenerator.value = true
      const preProductId = route.query.productId ? Number(route.query.productId) : products.value[0]?.id
      if (preProductId) {
        selectedProductId.value = preProductId
        await handleProductChange(preProductId)
      }
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-back {
  background: #ffffff;
  border: 1px solid #000000;
  color: #000000;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  padding: 6px 12px;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    background: #000000;
    color: #ffffff;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.form-section-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  padding: 20px 22px;
}

.section-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;

  &.between {
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .header-left-group {
    display: flex;
    align-items: flex-start;
    gap: 12px;
  }

  .step-badge {
    background: #000000;
    color: #ffffff;
    font-size: 10px;
    font-weight: 900;
    letter-spacing: 0.08em;
    padding: 4px 10px;
    flex-shrink: 0;
    margin-top: 2px;
  }

  h3 {
    margin: 0;
    font-size: 14.5px;
    font-weight: 900;
    color: #000000;
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }

  p {
    margin: 3px 0 0;
    font-size: 12px;
    color: #737373;
  }
}

.btn-toggle-step2 {
  background: #fafafa;
  border: 1px solid #000000;
  color: #000000;
  font-size: 11px;
  font-weight: 800;
  padding: 6px 12px;
  cursor: pointer;
  text-transform: uppercase;
  letter-spacing: 0.04em;

  &:hover {
    background: #000000;
    color: #ffffff;
  }
}

.text-green {
  color: #059669;
}

.text-amber {
  color: #d97706;
}

.product-option-row {
  display: flex;
  align-items: center;
  gap: 10px;

  .p-name {
    flex: 1;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .p-price {
    font-weight: 800;
    color: #000000;
    font-size: 12px;
  }
}

.selected-product-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fafafa;
  border: 1px solid #e5e5e5;
  padding: 10px 14px;

  .summary-thumb {
    width: 48px;
    height: 48px;
    object-fit: cover;
    border: 1px solid #e5e5e5;
    flex-shrink: 0;
  }

  .summary-info {
    min-width: 0;
    flex: 1;

    .summary-top {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      font-size: 13.5px;
    }

    .summary-meta {
      margin-top: 4px;
      font-size: 12px;
      color: #525252;
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
  }
}

.picker-col {
  margin-bottom: 14px;
}

.picker-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;

  .picker-label {
    font-size: 11.5px;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #171717;
  }

  .picker-quick-links {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 11.5px;
    color: #a3a3a3;

    .link-btn {
      background: none;
      border: none;
      color: #000000;
      font-weight: 700;
      font-size: 11.5px;
      cursor: pointer;
      padding: 0;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.pill-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.option-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 11px;
  background: #ffffff;
  border: 1px solid #d4d4d4;
  color: #171717;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.14s ease;

  &:hover {
    border-color: #000000;
  }

  &.active {
    background: #000000;
    border-color: #000000;
    color: #ffffff;
  }

  .color-swatch {
    width: 11px;
    height: 11px;
    border: 1px solid rgba(0, 0, 0, 0.25);
    display: inline-block;
    flex-shrink: 0;
  }
}

.defaults-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding-top: 14px;
  border-top: 1px dashed #e5e5e5;
  margin-top: 4px;

  &.no-top-border {
    padding-top: 0;
    border-top: none;
    margin-top: 0;
  }

  .default-field {
    display: flex;
    flex-direction: column;
    gap: 5px;

    label {
      font-size: 10.5px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: #525252;
    }
  }
}

.generator-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;

  .btn-generate-matrix {
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    padding: 9px 18px;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.06em;
    cursor: pointer;
    transition: all 0.15s ease;

    &:hover:not(:disabled) {
      background: #262626;
    }

    &:disabled {
      background: #a3a3a3;
      border-color: #a3a3a3;
      cursor: not-allowed;
    }
  }
}

.table-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.view-mode-switch {
  display: inline-flex;
  border: 1px solid #000000;

  .mode-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 5px 11px;
    background: #ffffff;
    color: #000000;
    border: none;
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    cursor: pointer;
    transition: all 0.12s ease;

    &.active {
      background: #000000;
      color: #ffffff;
    }
  }
}

/* MATRIX GRID STYLES */
.matrix-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 11.5px;
  color: #525252;
  margin-bottom: 10px;

  .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }

  .dot {
    width: 10px;
    height: 10px;
    display: inline-block;
    border: 1px solid #000000;

    &.dot-exist {
      background: #eff6ff;
      border-color: #3b82f6;
    }
    &.dot-modified {
      background: #fffbeb;
      border-color: #f59e0b;
    }
    &.dot-new {
      background: #ecfdf5;
      border-color: #10b981;
    }
    &.dot-empty {
      background: #ffffff;
      border-style: dashed;
      border-color: #a3a3a3;
    }
  }
}

.matrix-table-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.variant-matrix-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  background: #ffffff;

  th,
  td {
    border: 1px solid #e5e5e5;
    padding: 6px 5px;
    text-align: center;
    vertical-align: middle;
  }

  thead th {
    background: #111111;
    color: #ffffff;
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 0.05em;
    padding: 8px 6px;

    &.size-col-head {
      min-width: 72px;

      .col-count {
        display: inline-block;
        margin-top: 2px;
        font-size: 9.5px;
        font-weight: 700;
        color: #a3a3a3;
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
    min-width: 190px;
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
      width: 14px;
      height: 14px;
      border: 1px solid rgba(0, 0, 0, 0.28);
      flex-shrink: 0;
    }

    .color-meta {
      display: flex;
      flex-direction: column;
      line-height: 1.2;
      min-width: 0;
      flex: 1;

      strong {
        font-size: 12px;
        color: #000000;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      span {
        font-size: 10px;
        color: #737373;
        font-family: var(--font-mono);
      }
    }

    .btn-clear-row {
      background: transparent;
      border: 1px solid #e5e5e5;
      color: #737373;
      width: 20px;
      height: 20px;
      font-size: 10px;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &:hover {
        border-color: #ef4444;
        color: #ef4444;
        background: #fef2f2;
      }
    }
  }

  .matrix-cell {
    padding: 4px;
  }

  .cell-box {
    position: relative;
    display: flex;
    align-items: center;
    border: 1px solid #d4d4d4;

    &.cell-existing-edit {
      background: #eff6ff;
      border-color: #93c5fd;

      .cell-qty-input {
        color: #1e3a8a;
      }
    }

    &.cell-modified {
      background: #fffbeb;
      border-color: #f59e0b;

      .cell-qty-input {
        color: #92400e;
      }
    }

    &.cell-new {
      background: #ecfdf5;
      border-color: #10b981;

      .cell-qty-input {
        color: #065f46;
      }
    }

    &.cell-focused {
      outline: 2px solid #000000;
      outline-offset: -1px;
    }

    .cell-qty-input {
      width: 100%;
      min-width: 44px;
      border: none;
      background: transparent;
      text-align: center;
      font-size: 12.5px;
      font-weight: 800;
      padding: 5px 15px 5px 4px;
      outline: none;
      font-family: var(--font-mono);

      &::-webkit-inner-spin-button,
      &::-webkit-outer-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }
    }

    .cell-remove-btn,
    .cell-detail-btn {
      position: absolute;
      right: 2px;
      top: 50%;
      transform: translateY(-50%);
      width: 15px;
      height: 15px;
      border: none;
      background: transparent;
      font-size: 11px;
      line-height: 1;
      cursor: pointer;
      padding: 0;
      color: #64748b;

      &:hover {
        color: #000000;
      }
    }

    .cell-remove-btn {
      font-size: 13px;
      color: #059669;
      &:hover {
        color: #dc2626;
      }
    }
  }

  .cell-add-btn {
    width: 100%;
    height: 28px;
    background: #ffffff;
    border: 1px dashed #d4d4d4;
    color: #a3a3a3;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.12s ease;

    &:hover {
      border-color: #000000;
      color: #000000;
      background: #f5f5f5;
    }
  }

  .row-total-cell {
    background: #fafafa;
    font-family: var(--font-mono);
    font-size: 12px;
    color: #000000;
  }
}

/* FILTER BAR & TABLE BADGES */
.table-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;

  .filter-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .filter-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 5px 10px;
    background: #ffffff;
    border: 1px solid #d4d4d4;
    font-size: 11.5px;
    font-weight: 700;
    color: #262626;
    cursor: pointer;

    &.active {
      background: #000000;
      border-color: #000000;
      color: #ffffff;
    }

    .chip-dot {
      width: 9px;
      height: 9px;
      border: 1px solid rgba(0, 0, 0, 0.25);
      display: inline-block;
    }
  }
}

.row-badge {
  display: inline-block;
  font-size: 9.5px;
  font-weight: 900;
  padding: 2px 6px;
  letter-spacing: 0.04em;

  &.badge-new {
    background: #ecfdf5;
    color: #047857;
    border: 1px solid #10b981;
  }
  &.badge-mod {
    background: #fffbeb;
    color: #b45309;
    border: 1px solid #f59e0b;
  }
  &.badge-db {
    background: #f3f4f6;
    color: #4b5563;
    border: 1px solid #d1d5db;
  }
}

.muted-dash {
  color: #cbd5e1;
  font-weight: 700;
}

.table-scroll-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;

  :deep(.focused-table-row td) {
    background-color: #fef9c3 !important;
  }
}

.row-error-text {
  color: #dc2626;
  font-size: 11px;
  font-weight: 700;
  margin-top: 3px;
}

.bottom-submit-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #e5e5e5;

  .summary-count {
    font-size: 13px;
    color: #404040;
  }

  .bottom-btns {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
  }
}

@media (max-width: 900px) {
  .defaults-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .form-section-card {
    padding: 16px 14px;
  }

  .defaults-row {
    grid-template-columns: 1fr;
  }

  .selected-product-summary {
    margin-top: 12px;
  }

  .header-actions {
    width: 100%;
    .el-button {
      flex: 1;
    }
  }
}
</style>
