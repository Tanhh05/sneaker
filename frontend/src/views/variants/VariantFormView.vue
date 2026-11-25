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
            {{ isEdit ? 'CẬP NHẬT BIẾN THỂ SẢN PHẨM (SKU)' : 'THÊM NHIỀU BIẾN THỂ SẢN PHẨM (SKU)' }}
          </h2>
        </div>
        <p class="page-subtitle">
          {{
            isEdit
              ? 'Chỉnh sửa chi tiết mã SKU, kích cỡ, màu sắc, giá bán và tồn kho cho biến thể'
              : 'Chọn 1 sản phẩm Sneaker → tick nhiều Size & Màu → xem toàn bộ biến thể trên 1 màn hình dạng Ma trận gọn gàng'
          }}
        </p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/admin/variants')">Hủy bỏ</el-button>
        <el-button
          v-if="!isEdit"
          type="primary"
          :loading="saving"
          :disabled="!selectedProductId || draftVariants.length === 0"
          @click="submitBatchVariants"
        >
          <el-icon style="margin-right: 6px"><Check /></el-icon>
          LƯU TẤT CẢ ({{ draftVariants.length }} BIẾN THỂ)
        </el-button>
        <el-button v-else type="primary" :loading="saving" @click="submitSingleEdit">
          <el-icon style="margin-right: 6px"><Check /></el-icon>
          CẬP NHẬT BIẾN THỂ
        </el-button>
      </div>
    </div>

    <!-- ===================================================== -->
    <!-- MODE 1: BATCH CREATE MULTIPLE VARIANTS FOR 1 PRODUCT  -->
    <!-- ===================================================== -->
    <template v-if="!isEdit">
      <!-- STEP 1: SELECT PRODUCT -->
      <div class="form-section-card">
        <div class="section-header">
          <span class="step-badge">BƯỚC 1</span>
          <div>
            <h3>Chọn sản phẩm Sneaker</h3>
            <p>Các biến thể được thêm sẽ gắn trực tiếp vào sản phẩm này</p>
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
                  <span>Đang có: <strong>{{ existingVariants.length }} SKU</strong></span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- STEP 2: QUICK MATRIX GENERATOR (MULTI-SIZE x MULTI-COLOR) -->
      <div class="form-section-card" v-if="selectedProduct">
        <div class="section-header">
          <span class="step-badge">BƯỚC 2</span>
          <div>
            <h3>Bộ sinh tổ hợp biến thể hàng loạt (Size × Màu sắc)</h3>
            <p>Tick chọn nhiều Size và nhiều Màu cùng lúc để tự động sinh mã SKU (tự bỏ qua các tổ hợp đã tồn tại)</p>
          </div>
        </div>

        <el-row :gutter="24">
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
        <div class="defaults-row">
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
        </div>
      </div>

      <!-- STEP 3: COMPACT MATRIX & FILTERED TABLE PREVIEW -->
      <div class="form-section-card" v-if="selectedProduct">
        <div class="section-header between">
          <div class="header-left-group">
            <span class="step-badge">BƯỚC 3</span>
            <div>
              <h3>Danh sách biến thể chuẩn bị lưu ({{ draftVariants.length }} biến thể)</h3>
              <p>
                {{
                  viewMode === 'matrix'
                    ? 'Xem trọn toàn bộ biến thể trên 1 khung Ma trận (Màu × Size) — gõ trực tiếp tồn kho hoặc bấm + / ✕ từng ô'
                    : 'Chế độ bảng chi tiết có lọc theo từng Màu & Size — tùy chỉnh riêng lẻ SKU, giá nhập, giá bán'
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
            <el-button size="small" :disabled="draftVariants.length === 0" @click="applyDefaultsToAll">
              Áp dụng giá &amp; SL mặc định
            </el-button>
            <el-button size="small" type="danger" plain :disabled="draftVariants.length === 0" @click="draftVariants = []">
              Xóa hết
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
              <span class="legend-item"><i class="dot dot-new"></i> Ô có số = Biến thể mới sẽ tạo (gõ để đổi SL tồn)</span>
              <span class="legend-item"><i class="dot dot-exist"></i> Đã có = Tổ hợp đã tồn tại trong DB</span>
              <span class="legend-item"><i class="dot dot-empty"></i> + = Bấm để thêm nhanh biến thể vào ô trống</span>
            </div>

            <table class="variant-matrix-table">
              <thead>
                <tr>
                  <th class="sticky-color-col">MÀU SẮC ({{ matrixColors.length }})</th>
                  <th v-for="s in matrixSizes" :key="s.id" class="size-col-head">
                    <div>{{ s.sizeValue }}</div>
                    <span class="col-count">{{ countBySize(s.id) }}</span>
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
                        <span>{{ c.colorCode }} • {{ countByColor(c.id) }} size</span>
                      </div>
                      <button
                        v-if="countByColor(c.id) > 0"
                        type="button"
                        class="btn-clear-row"
                        title="Bỏ tất cả biến thể của màu này"
                        @click="removeColorRows(c.id)"
                      >
                        ✕
                      </button>
                    </div>
                  </td>

                  <td v-for="s in matrixSizes" :key="s.id" class="matrix-cell">
                    <!-- Already exists in DB -->
                    <div v-if="comboExistsInDb(s.id, c.id)" class="cell-existing" :title="getExistingSku(s.id, c.id)">
                      <span>Đã có</span>
                    </div>

                    <!-- Draft variant in matrix -->
                    <div v-else-if="findDraft(s.id, c.id)" class="cell-draft">
                      <input
                        type="number"
                        min="0"
                        class="cell-qty-input"
                        :value="findDraft(s.id, c.id).availableQuantity"
                        :title="`SKU: ${findDraft(s.id, c.id).sku} | Giá bán: ${formatCurrency(findDraft(s.id, c.id).salePrice)}`"
                        @input="onMatrixQtyInput(s.id, c.id, $event.target.value)"
                      />
                      <button
                        type="button"
                        class="cell-remove-btn"
                        title="Xóa biến thể này"
                        @click="removeDraftByCombo(s.id, c.id)"
                      >
                        ×
                      </button>
                    </div>

                    <!-- Empty slot: click to add instantly -->
                    <button
                      v-else
                      type="button"
                      class="cell-add-btn"
                      title="Bấm để thêm biến thể này"
                      @click="addSingleCombo(s.id, c.id)"
                    >
                      +
                    </button>
                  </td>

                  <td class="row-total-cell">
                    <strong>{{ countByColor(c.id) }}</strong>
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
          <!-- Quick Color Filter Pills -->
          <div v-if="draftVariants.length > 0" class="table-filter-bar">
            <div class="filter-chips">
              <button
                type="button"
                class="filter-chip"
                :class="{ active: tableFilterColorId === null }"
                @click="tableFilterColorId = null"
              >
                Tất cả ({{ draftVariants.length }})
              </button>
              <button
                v-for="c in draftColorTabs"
                :key="c.id"
                type="button"
                class="filter-chip"
                :class="{ active: tableFilterColorId === c.id }"
                @click="tableFilterColorId = c.id"
              >
                <span class="chip-dot" :style="{ backgroundColor: c.hexCode }"></span>
                {{ c.colorName }} ({{ countByColor(c.id) }})
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
            <el-table :data="filteredDraftVariants" border stripe max-height="420" style="width: 100%">
              <template #empty>
                <el-empty
                  description="Chưa có biến thể nào phù hợp."
                  :image-size="64"
                />
              </template>

              <el-table-column type="index" label="#" width="48" align="center" />

              <el-table-column label="Size EU" width="125">
                <template #default="{ row }">
                  <el-select v-model="row.sizeId" size="small" style="width: 100%" @change="refreshRowSku(row)">
                    <el-option v-for="s in sizes" :key="s.id" :label="`Size ${s.sizeValue}`" :value="s.id" />
                  </el-select>
                </template>
              </el-table-column>

              <el-table-column label="Màu sắc" min-width="170">
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

              <el-table-column label="Xóa" width="60" align="center" fixed="right">
                <template #default="{ row }">
                  <button type="button" class="action-btn danger" title="Xóa dòng này" @click="removeRowByRef(row)">
                    <el-icon><Delete /></el-icon>
                  </button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <div class="bottom-submit-bar" v-if="draftVariants.length > 0">
          <span class="summary-count">
            Sẵn sàng tạo mới <strong>{{ draftVariants.length }}</strong> biến thể cho <strong>{{ selectedProduct.productName }}</strong>
            (Tổng tồn thêm: <strong>{{ totalDraftQty }}</strong> đôi)
          </span>
          <div class="bottom-btns">
            <el-button @click="$router.push('/admin/variants')">Quay lại</el-button>
            <el-button type="primary" size="large" :loading="saving" @click="submitBatchVariants">
              <el-icon style="margin-right: 6px"><Check /></el-icon>
              LƯU TẤT CẢ ({{ draftVariants.length }} BIẾN THỂ)
            </el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- ===================================================== -->
    <!-- MODE 2: EDIT SINGLE EXISTING VARIANT (/variants/:id)  -->
    <!-- ===================================================== -->
    <div v-else class="form-section-card edit-single-card">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="Sản phẩm Sneaker" prop="productId">
          <el-select v-model="editForm.productId" placeholder="Chọn sản phẩm" filterable style="width: 100%">
            <el-option v-for="p in products" :key="p.id" :label="`${p.productCode} — ${p.productName}`" :value="p.id" />
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Size giày (EU)" prop="sizeId">
              <el-select v-model="editForm.sizeId" placeholder="Chọn Size" style="width: 100%">
                <el-option v-for="s in sizes" :key="s.id" :label="`Size ${s.sizeValue}`" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Màu sắc" prop="colorId">
              <el-select v-model="editForm.colorId" placeholder="Chọn Màu sắc" style="width: 100%">
                <el-option v-for="c in colors" :key="c.id" :label="`${c.colorName} (${c.colorCode})`" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="14">
            <el-form-item label="Mã SKU riêng biệt" prop="sku">
              <el-input v-model="editForm.sku" placeholder="VD: AF1-43-W" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="10">
            <el-form-item label="Số lượng có sẵn" prop="availableQuantity">
              <el-input-number v-model="editForm.availableQuantity" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Giá nhập (VNĐ)" prop="importPrice">
              <el-input-number v-model="editForm.importPrice" :min="0" :step="50000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Giá bán (VNĐ)" prop="salePrice">
              <el-input-number v-model="editForm.salePrice" :min="0" :step="50000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Trạng thái" prop="status">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="ACTIVE (Đang kinh doanh)" value="ACTIVE" />
            <el-option label="INACTIVE (Ngừng kinh doanh)" value="INACTIVE" />
          </el-select>
        </el-form-item>

        <div class="edit-footer-btns">
          <el-button @click="$router.push('/admin/variants')">Hủy bỏ</el-button>
          <el-button type="primary" :loading="saving" @click="submitSingleEdit">
            <el-icon style="margin-right: 6px"><Check /></el-icon>
            CẬP NHẬT BIẾN THỂ
          </el-button>
        </div>
      </el-form>
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
const loading = ref(false)
const saving = ref(false)

const products = ref([])
const sizes = ref([])
const colors = ref([])

// Batch Create State
const selectedProductId = ref(null)
const existingVariants = ref([])
const selectedSizeIds = ref([])
const selectedColorIds = ref([])
const draftVariants = ref([])

// View Mode & Table Filters (default: compact zero-scroll Matrix)
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

// Matrix axes: show all sizes & colors present in drafts, existing variants, or selected pills
const matrixSizes = computed(() => {
  const idSet = new Set([
    ...selectedSizeIds.value,
    ...draftVariants.value.map((r) => r.sizeId),
    ...existingVariants.value.map((e) => e.sizeId)
  ])
  if (idSet.size === 0) return []
  return sizes.value.filter((s) => idSet.has(s.id))
})

const matrixColors = computed(() => {
  const idSet = new Set([
    ...selectedColorIds.value,
    ...draftVariants.value.map((r) => r.colorId),
    ...existingVariants.value.map((e) => e.colorId)
  ])
  if (idSet.size === 0) return []
  return colors.value.filter((c) => idSet.has(c.id))
})

const draftColorTabs = computed(() => {
  const idSet = new Set(draftVariants.value.map((r) => r.colorId))
  return colors.value.filter((c) => idSet.has(c.id))
})

const filteredDraftVariants = computed(() => {
  return draftVariants.value.filter((row) => {
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

const totalDraftQty = computed(() =>
  draftVariants.value.reduce((acc, r) => acc + Number(r.availableQuantity || 0), 0)
)

// Single Edit State
const editFormRef = ref(null)
const editForm = reactive({
  productId: null,
  sizeId: null,
  colorId: null,
  sku: '',
  importPrice: 0,
  salePrice: 0,
  availableQuantity: 0,
  status: 'ACTIVE'
})

const editRules = {
  productId: [{ required: true, message: 'Vui lòng chọn sản phẩm', trigger: 'change' }],
  sizeId: [{ required: true, message: 'Vui lòng chọn Size', trigger: 'change' }],
  colorId: [{ required: true, message: 'Vui lòng chọn màu sắc', trigger: 'change' }],
  sku: [{ required: true, message: 'Vui lòng nhập mã SKU', trigger: 'blur' }]
}

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
  if (!selectedProduct.value) return
  const generated = buildSku(selectedProduct.value, row.sizeId, row.colorId)
  if (generated) {
    row.sku = generated
  }
}

function comboExistsInDb(sizeId, colorId) {
  return existingVariants.value.some((ev) => ev.sizeId === sizeId && ev.colorId === colorId)
}

function getExistingSku(sizeId, colorId) {
  const found = existingVariants.value.find((ev) => ev.sizeId === sizeId && ev.colorId === colorId)
  return found ? `Đã có sẵn: ${found.sku} (Tồn: ${found.availableQuantity})` : 'Đã có'
}

function findDraft(sizeId, colorId) {
  return draftVariants.value.find((r) => r.sizeId === sizeId && r.colorId === colorId)
}

function countByColor(colorId) {
  return draftVariants.value.filter((r) => r.colorId === colorId).length
}

function countBySize(sizeId) {
  return draftVariants.value.filter((r) => r.sizeId === sizeId).length
}

function onMatrixQtyInput(sizeId, colorId, rawVal) {
  const row = findDraft(sizeId, colorId)
  if (!row) return
  const num = Number(rawVal)
  row.availableQuantity = Number.isFinite(num) && num >= 0 ? Math.floor(num) : 0
}

function addSingleCombo(sizeId, colorId) {
  if (!selectedProduct.value || comboExistsInDb(sizeId, colorId) || findDraft(sizeId, colorId)) return
  draftVariants.value.push({
    sizeId,
    colorId,
    sku: buildSku(selectedProduct.value, sizeId, colorId),
    importPrice: defaults.importPrice,
    salePrice: defaults.salePrice,
    availableQuantity: defaults.availableQuantity,
    status: defaults.status
  })
}

function removeDraftByCombo(sizeId, colorId) {
  const idx = draftVariants.value.findIndex((r) => r.sizeId === sizeId && r.colorId === colorId)
  if (idx >= 0) {
    draftVariants.value.splice(idx, 1)
  }
}

function removeColorRows(colorId) {
  draftVariants.value = draftVariants.value.filter((r) => r.colorId !== colorId)
  if (tableFilterColorId.value === colorId) {
    tableFilterColorId.value = null
  }
}

function removeRowByRef(row) {
  const idx = draftVariants.value.indexOf(row)
  if (idx >= 0) {
    draftVariants.value.splice(idx, 1)
  }
}

function getRowError(row) {
  if (!row.sizeId || !row.colorId) return 'Thiếu Size hoặc Màu'
  if (!row.sku || !row.sku.trim()) return 'Chưa nhập mã SKU'
  if (comboExistsInDb(row.sizeId, row.colorId)) return 'Tổ hợp Size + Màu này đã có sẵn'

  const dupCombo = draftVariants.value.filter(
    (r) => r.sizeId === row.sizeId && r.colorId === row.colorId
  ).length
  if (dupCombo > 1) return 'Trùng Size + Màu trong danh sách'

  const normalizedSku = row.sku.trim().toUpperCase()
  const dupSku = draftVariants.value.filter(
    (r) => (r.sku || '').trim().toUpperCase() === normalizedSku
  ).length
  if (dupSku > 1) return 'Trùng mã SKU trong danh sách'

  return ''
}

async function handleProductChange(productId) {
  draftVariants.value = []
  existingVariants.value = []
  tableFilterColorId.value = null
  if (!productId) return

  const p = products.value.find((x) => x.id === productId)
  if (p) {
    defaults.importPrice = Number(p.importPrice || 0)
    defaults.salePrice = Number(p.salePrice || 0)
  }

  try {
    const res = await variantService.getVariantsByProduct(productId)
    existingVariants.value = res.data || []
  } catch (_) {
    existingVariants.value = []
  }
}

function generateMatrixVariants() {
  if (!selectedProduct.value) {
    ElMessage.warning('Vui lòng chọn sản phẩm trước')
    return
  }
  let added = 0
  let skippedExisting = 0
  let skippedDuplicate = 0

  for (const sizeId of selectedSizeIds.value) {
    for (const colorId of selectedColorIds.value) {
      if (comboExistsInDb(sizeId, colorId)) {
        skippedExisting++
        continue
      }
      const alreadyInDraft = draftVariants.value.some(
        (r) => r.sizeId === sizeId && r.colorId === colorId
      )
      if (alreadyInDraft) {
        skippedDuplicate++
        continue
      }
      draftVariants.value.push({
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
    const notes = []
    if (skippedExisting > 0) notes.push(`bỏ qua ${skippedExisting} tổ hợp đã có sẵn`)
    if (skippedDuplicate > 0) notes.push(`bỏ qua ${skippedDuplicate} dòng đã có`)
    ElMessage.success(
      `Đã tạo ${added} biến thể mới` + (notes.length ? ` (${notes.join(', ')})` : '')
    )
  } else {
    ElMessage.info('Tất cả các tổ hợp Size × Màu anh chọn đã tồn tại hoặc đã có trong danh sách!')
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
      const inDb = comboExistsInDb(s.id, c.id)
      const inDraft = draftVariants.value.some((r) => r.sizeId === s.id && r.colorId === c.id)
      if (!inDb && !inDraft) {
        pickSize = s.id
        pickColor = c.id
        break outer
      }
    }
  }

  draftVariants.value.push({
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

function applyDefaultsToAll() {
  draftVariants.value.forEach((row) => {
    row.importPrice = defaults.importPrice
    row.salePrice = defaults.salePrice
    row.availableQuantity = defaults.availableQuantity
    row.status = defaults.status
  })
  ElMessage.success('Đã áp dụng giá và số lượng mặc định cho toàn bộ danh sách')
}

async function submitBatchVariants() {
  if (!selectedProductId.value) {
    ElMessage.warning('Vui lòng chọn sản phẩm Sneaker')
    return
  }
  if (draftVariants.value.length === 0) {
    ElMessage.warning('Chưa có biến thể nào để lưu')
    return
  }

  const invalidRow = draftVariants.value.find((row) => !!getRowError(row))
  if (invalidRow) {
    viewMode.value = 'table'
    ElMessage.error(`Vui lòng kiểm tra lại dòng SKU "${invalidRow.sku || '(trống)'}": ${getRowError(invalidRow)}`)
    return
  }

  saving.value = true
  let successCount = 0
  const failedRows = []

  try {
    for (const row of draftVariants.value) {
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
        successCount++
      } catch (_) {
        failedRows.push(row)
      }
    }

    if (failedRows.length === 0) {
      ElMessage.success(`Đã thêm thành công ${successCount} biến thể SKU mới!`)
      router.push('/admin/variants')
    } else {
      await handleProductChange(selectedProductId.value)
      draftVariants.value = failedRows
      viewMode.value = 'table'
      ElMessage.warning(
        `Đã lưu ${successCount} biến thể, còn ${failedRows.length} dòng bị lỗi — vui lòng kiểm tra và lưu lại.`
      )
    }
  } finally {
    saving.value = false
  }
}

async function submitSingleEdit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await variantService.updateVariant(route.params.id, {
        ...editForm,
        sku: editForm.sku.trim().toUpperCase()
      })
      ElMessage.success('Cập nhật biến thể SKU thành công')
      router.push('/admin/variants')
    } finally {
      saving.value = false
    }
  })
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
      const vRes = await variantService.getVariantById(route.params.id)
      const v = vRes.data
      if (v) {
        editForm.productId = v.productId
        editForm.sizeId = v.sizeId
        editForm.colorId = v.colorId
        editForm.sku = v.sku
        editForm.importPrice = Number(v.importPrice || 0)
        editForm.salePrice = Number(v.salePrice || 0)
        editForm.availableQuantity = v.availableQuantity || 0
        editForm.status = v.status || 'ACTIVE'
      }
    } else {
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

.edit-single-card {
  max-width: 720px;
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
  gap: 12px;
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

/* MATRIX GRID STYLES (SEE ALL 78 VARIANTS WITHOUT SCROLLING) */
.matrix-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
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

    &.dot-new {
      background: #ecfdf5;
      border-color: #10b981;
    }
    &.dot-exist {
      background: #f3f4f6;
      border-color: #d1d5db;
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
      min-width: 68px;

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

  .cell-existing {
    background: #f3f4f6;
    color: #9ca3af;
    font-size: 10px;
    font-weight: 700;
    padding: 6px 2px;
    border: 1px solid #e5e7eb;
    user-select: none;
  }

  .cell-draft {
    position: relative;
    display: flex;
    align-items: center;
    background: #ecfdf5;
    border: 1px solid #10b981;

    .cell-qty-input {
      width: 100%;
      min-width: 42px;
      border: none;
      background: transparent;
      text-align: center;
      font-size: 12.5px;
      font-weight: 800;
      color: #065f46;
      padding: 5px 14px 5px 4px;
      outline: none;
      font-family: var(--font-mono);

      &::-webkit-inner-spin-button,
      &::-webkit-outer-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }
    }

    .cell-remove-btn {
      position: absolute;
      right: 2px;
      top: 50%;
      transform: translateY(-50%);
      width: 14px;
      height: 14px;
      border: none;
      background: transparent;
      color: #059669;
      font-size: 13px;
      line-height: 1;
      cursor: pointer;
      padding: 0;

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

/* FILTER BAR FOR TABLE VIEW */
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

.table-scroll-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
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

.edit-footer-btns {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #e5e5e5;
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
