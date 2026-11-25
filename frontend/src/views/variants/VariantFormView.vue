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
              : 'Chọn 1 sản phẩm Sneaker rồi tick nhiều Size & Màu sắc để sinh hàng loạt biến thể cùng lúc, hoặc thêm từng dòng thủ công'
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
                  <span>Giá nhập gốc: <strong>{{ formatCurrency(selectedProduct.importPrice) }}</strong></span>
                  <span>•</span>
                  <span>Giá bán gốc: <strong class="price-text">{{ formatCurrency(selectedProduct.salePrice) }}</strong></span>
                  <span>•</span>
                  <span>Hiện có: <strong>{{ existingVariants.length }} biến thể</strong></span>
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

      <!-- STEP 3: EDITABLE DRAFT VARIANTS TABLE -->
      <div class="form-section-card" v-if="selectedProduct">
        <div class="section-header between">
          <div class="header-left-group">
            <span class="step-badge">BƯỚC 3</span>
            <div>
              <h3>Danh sách biến thể chuẩn bị lưu ({{ draftVariants.length }} biến thể)</h3>
              <p>Anh có thể sửa trực tiếp mã SKU, giá nhập, giá bán, tồn kho của từng dòng trước khi bấm Lưu tất cả</p>
            </div>
          </div>
          <div class="table-tools">
            <el-button size="small" :disabled="draftVariants.length === 0" @click="applyDefaultsToAll">
              Áp dụng giá &amp; SL mặc định cho tất cả
            </el-button>
            <el-button size="small" type="danger" plain :disabled="draftVariants.length === 0" @click="draftVariants = []">
              Xóa hết danh sách
            </el-button>
          </div>
        </div>

        <div v-if="existingVariants.length > 0" class="existing-variants-bar">
          <span class="ex-label">Đã tồn tại trên sản phẩm này ({{ existingVariants.length }}):</span>
          <span v-for="ev in existingVariants" :key="ev.id" class="ex-chip">
            {{ ev.sku }} (Size {{ ev.sizeValue }} / {{ ev.colorName }})
          </span>
        </div>

        <div class="table-scroll-wrap">
          <el-table :data="draftVariants" border stripe style="width: 100%">
            <template #empty>
              <el-empty
                description="Chưa có biến thể nào trong danh sách. Hãy chọn Size & Màu ở Bước 2 rồi bấm Tạo tổ hợp, hoặc bấm Thêm 1 dòng thủ công."
                :image-size="72"
              />
            </template>

            <el-table-column type="index" label="#" width="50" align="center" />

            <el-table-column label="Size EU" width="135">
              <template #default="{ row }">
                <el-select v-model="row.sizeId" size="small" style="width: 100%" @change="refreshRowSku(row)">
                  <el-option v-for="s in sizes" :key="s.id" :label="`Size ${s.sizeValue}`" :value="s.id" />
                </el-select>
              </template>
            </el-table-column>

            <el-table-column label="Màu sắc" min-width="180">
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

            <el-table-column label="Mã SKU" min-width="175">
              <template #default="{ row }">
                <el-input v-model="row.sku" size="small" placeholder="VD: AF1-41-W" />
                <div v-if="getRowError(row)" class="row-error-text">{{ getRowError(row) }}</div>
              </template>
            </el-table-column>

            <el-table-column label="Giá nhập (VNĐ)" width="155">
              <template #default="{ row }">
                <el-input-number v-model="row.importPrice" :min="0" :step="50000" size="small" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>

            <el-table-column label="Giá bán (VNĐ)" width="155">
              <template #default="{ row }">
                <el-input-number v-model="row.salePrice" :min="0" :step="50000" size="small" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>

            <el-table-column label="SL sẵn có" width="115">
              <template #default="{ row }">
                <el-input-number v-model="row.availableQuantity" :min="0" size="small" controls-position="right" style="width: 100%" />
              </template>
            </el-table-column>

            <el-table-column label="Trạng thái" width="125">
              <template #default="{ row }">
                <el-select v-model="row.status" size="small" style="width: 100%">
                  <el-option label="ACTIVE" value="ACTIVE" />
                  <el-option label="INACTIVE" value="INACTIVE" />
                </el-select>
              </template>
            </el-table-column>

            <el-table-column label="Xóa" width="68" align="center" fixed="right">
              <template #default="{ $index }">
                <button type="button" class="action-btn danger" title="Xóa dòng này" @click="removeRow($index)">
                  <el-icon><Delete /></el-icon>
                </button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="bottom-submit-bar" v-if="draftVariants.length > 0">
          <span class="summary-count">
            Sẵn sàng tạo mới <strong>{{ draftVariants.length }}</strong> biến thể cho <strong>{{ selectedProduct.productName }}</strong>
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
import { Plus, Delete, Check } from '@element-plus/icons-vue'
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

const defaults = reactive({
  importPrice: 1800000,
  salePrice: 2950000,
  availableQuantity: 10,
  status: 'ACTIVE'
})

const selectedProduct = computed(() =>
  products.value.find((p) => p.id === selectedProductId.value) || null
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
    if (skippedDuplicate > 0) notes.push(`bỏ qua ${skippedDuplicate} dòng đã có trong bảng`)
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
  // Pick first unused Size + Color combo if available
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

function removeRow(index) {
  draftVariants.value.splice(index, 1)
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
      draftVariants.value = failedRows
      await handleProductChange(selectedProductId.value)
      draftVariants.value = failedRows
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
  padding: 22px 24px;
}

.edit-single-card {
  max-width: 720px;
}

.section-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
  padding-bottom: 14px;
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
    font-size: 15px;
    font-weight: 900;
    color: #000000;
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }

  p {
    margin: 4px 0 0;
    font-size: 12.5px;
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
    width: 52px;
    height: 52px;
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
      margin-top: 5px;
      font-size: 12px;
      color: #525252;
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
  }
}

.picker-col {
  margin-bottom: 16px;
}

.picker-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;

  .picker-label {
    font-size: 12px;
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
  gap: 8px;
}

.option-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 12px;
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
    width: 12px;
    height: 12px;
    border: 1px solid rgba(0, 0, 0, 0.25);
    display: inline-block;
    flex-shrink: 0;
  }
}

.defaults-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  padding-top: 16px;
  border-top: 1px dashed #e5e5e5;
  margin-top: 6px;

  .default-field {
    display: flex;
    flex-direction: column;
    gap: 6px;

    label {
      font-size: 11px;
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
  margin-top: 18px;

  .btn-generate-matrix {
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    padding: 10px 20px;
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
  gap: 8px;
  flex-wrap: wrap;
}

.existing-variants-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  background: #fafafa;
  border: 1px solid #e5e5e5;
  padding: 10px 12px;
  margin-bottom: 14px;

  .ex-label {
    font-size: 11px;
    font-weight: 800;
    color: #525252;
    text-transform: uppercase;
    margin-right: 4px;
  }

  .ex-chip {
    font-size: 11px;
    font-family: var(--font-mono);
    background: #ffffff;
    border: 1px solid #d4d4d4;
    padding: 2px 7px;
    color: #404040;
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
  margin-top: 18px;
  padding-top: 16px;
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
