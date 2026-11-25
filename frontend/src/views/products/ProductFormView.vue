<template>
  <div class="product-studio-page" v-loading="loadingPage">
    <!-- 1. STICKY TOP COMMAND BAR (MATCHING VARIANT STUDIO) -->
    <div class="studio-topbar">
      <div class="topbar-left">
        <button type="button" class="btn-back" @click="handleCancel">
          <el-icon><ArrowLeft /></el-icon>
          <span>Quay lại</span>
        </button>

        <div class="mode-pill" :class="isEdit ? 'mode-edit' : 'mode-create'">
          {{ isEdit ? 'CẬP NHẬT SẢN PHẨM' : 'THÊM SẢN PHẨM MỚI' }}
        </div>

        <div class="topbar-live-meta">
          <span class="code-badge">{{ form.productCode || 'MÃ SP' }}</span>
          <strong class="live-name">{{ form.productName || 'Mẫu giày Sneaker mới...' }}</strong>
          <span class="kpi-chip" :class="profitAmount >= 0 ? 'kpi-green' : 'kpi-red'">
            Lãi: <strong>{{ formatCurrency(profitAmount) }}</strong> ({{ profitMarginPct }}%)
          </span>
          <span class="kpi-chip">
            <strong>{{ imageItems.length }}</strong> ảnh
          </span>
          <span v-if="isEdit" class="kpi-chip">
            <strong>{{ existingVariants.length }}</strong> SKU
          </span>
        </div>
      </div>

      <div class="topbar-right">
        <button
          v-if="isEdit"
          type="button"
          class="btn-ghost"
          @click="$router.push(`/admin/products/${productId}`)"
        >
          <el-icon><View /></el-icon>
          <span>Chi tiết</span>
        </button>
        <button
          v-if="isEdit"
          type="button"
          class="btn-outline"
          @click="goToVariantStudio"
        >
          <el-icon><Grid /></el-icon>
          <span>Ma trận SKU</span>
        </button>
        <el-button
          type="primary"
          class="btn-save-primary"
          :loading="saving"
          @click="submitForm"
        >
          <el-icon style="margin-right: 6px"><Check /></el-icon>
          <span>{{ saving ? 'ĐANG LƯU...' : (isEdit ? 'LƯU CẬP NHẬT' : 'LƯU SẢN PHẨM') }}</span>
        </el-button>
      </div>
    </div>

    <!-- 2. COMPACT 2-COLUMN STUDIO WORKSPACE -->
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="studio-form"
    >
      <div class="studio-grid">
        <!-- LEFT COLUMN: COMPACT MEDIA STUDIO + LIVE CARD (340px) -->
        <aside class="media-column">
          <div class="studio-card media-card">
            <div class="card-head-row">
              <span class="badge-black">HÌNH ẢNH &amp; XEM TRƯỚC</span>
              <div class="media-head-right">
                <span v-if="pendingCount > 0" class="pending-pill">{{ pendingCount }} chờ tải</span>
                <button
                  v-if="imageItems.length > 0"
                  type="button"
                  class="btn-text-danger"
                  @click="clearAllImages"
                >
                  Xóa hết
                </button>
              </div>
            </div>

            <!-- Main Preview Stage -->
            <div class="preview-stage">
              <img
                v-if="currentPreviewUrl && !imgError"
                :src="currentPreviewUrl"
                alt="Sneaker preview"
                class="preview-img"
                @error="imgError = true"
                @load="imgError = false"
              />
              <div v-else class="preview-empty">
                <el-icon :size="36"><Picture /></el-icon>
                <strong>CHƯA CÓ HÌNH ẢNH</strong>
                <span>Kéo thả hoặc chọn nhiều ảnh bên dưới</span>
              </div>
              <span class="preview-status-tag" :class="form.status === 'ACTIVE' ? 'st-active' : 'st-inactive'">
                {{ form.status }}
              </span>
            </div>

            <!-- Compact Live Card Footer -->
            <div class="live-card-strip">
              <div class="lc-meta">
                <span>{{ getBrandName(form.brandId) }}</span>
                <span>•</span>
                <span>{{ getCategoryName(form.categoryId) }}</span>
              </div>
              <div class="lc-bottom">
                <strong class="lc-price">{{ formatCurrency(form.salePrice || 0) }}</strong>
                <span class="lc-code">{{ form.productCode || '---' }}</span>
              </div>
            </div>

            <!-- Compact Drag & Drop Zone -->
            <div
              class="upload-zone"
              :class="{ dragover: isDragOver, uploading: uploadingImage }"
              @dragover.prevent="isDragOver = true"
              @dragleave.prevent="isDragOver = false"
              @drop.prevent="handleDropFile"
              @click="triggerFileInput"
            >
              <input
                ref="fileInputRef"
                type="file"
                multiple
                accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml"
                class="hidden-input"
                @change="handleFileInputChange"
              />
              <el-icon v-if="uploadingImage" class="is-loading" :size="20"><Loading /></el-icon>
              <el-icon v-else :size="20"><UploadFilled /></el-icon>
              <div class="uz-text">
                <strong>{{ uploadingImage ? 'Đang tải ảnh lên MinIO S3...' : 'Chọn / kéo thả nhiều ảnh từ máy' }}</strong>
                <span>PNG, JPG, WEBP ≤ 20MB · Upload khi bấm Lưu</span>
              </div>
            </div>

            <!-- Compact Gallery Thumbnails -->
            <div v-if="imageItems.length > 0" class="gallery-grid">
              <div
                v-for="(item, idx) in imageItems"
                :key="item.id || idx"
                class="g-tile"
                :class="{ primary: idx === 0, viewing: currentPreviewUrl === item.previewUrl }"
                @click="activePreviewImage = item.previewUrl"
              >
                <img :src="item.previewUrl" alt="" />
                <span v-if="idx === 0" class="g-badge-main">★ CHÍNH</span>
                <span v-else class="g-badge-num">#{{ idx + 1 }}</span>
                <span v-if="item.isNew" class="g-badge-new">MỚI</span>

                <div class="g-actions">
                  <button
                    v-if="idx !== 0"
                    type="button"
                    class="g-act-btn"
                    title="Đặt làm ảnh chính"
                    @click.stop="setAsPrimary(idx)"
                  >
                    ★
                  </button>
                  <button
                    type="button"
                    class="g-act-btn del"
                    title="Xóa ảnh này"
                    @click.stop="removeImage(idx)"
                  >
                    ×
                  </button>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <!-- RIGHT COLUMN: COMPACT STRUCTURED FORM CARDS -->
        <main class="fields-column">
          <!-- CARD 1: ĐỊNH DANH & PHÂN LOẠI -->
          <div class="studio-card form-card">
            <div class="card-head-row">
              <span class="badge-black">01. THÔNG TIN ĐỊNH DANH &amp; PHÂN LOẠI</span>
              <div class="status-toggle-group">
                <button
                  type="button"
                  class="st-btn"
                  :class="{ active: form.status === 'ACTIVE' }"
                  @click="form.status = 'ACTIVE'"
                >
                  ACTIVE (Đang bán)
                </button>
                <button
                  type="button"
                  class="st-btn inactive"
                  :class="{ active: form.status === 'INACTIVE' }"
                  @click="form.status = 'INACTIVE'"
                >
                  INACTIVE (Tạm ẩn)
                </button>
              </div>
            </div>

            <el-row :gutter="12">
              <el-col :xs="24" :sm="8">
                <el-form-item label="Mã sản phẩm (*)" prop="productCode">
                  <div class="code-input-group">
                    <el-input v-model="form.productCode" placeholder="VD: SP012" class="mono-input" />
                    <button type="button" class="btn-gen-code" title="Tạo mã nhanh" @click="autoGenerateCode">
                      ⚡ Tạo
                    </button>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="16">
                <el-form-item label="Tên sản phẩm Sneaker (*)" prop="productName">
                  <el-input v-model="form.productName" placeholder="VD: Nike Dunk Low Retro Panda Black White" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :xs="24" :sm="12">
                <el-form-item label="Thương hiệu (*)" prop="brandId">
                  <el-select v-model="form.brandId" placeholder="Chọn hãng giày" filterable style="width: 100%">
                    <el-option
                      v-for="b in brands"
                      :key="b.id"
                      :label="`${b.brandName}${b.country ? ` (${b.country})` : ''}`"
                      :value="b.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="Danh mục giày (*)" prop="categoryId">
                  <el-select v-model="form.categoryId" placeholder="Chọn danh mục" filterable style="width: 100%">
                    <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <!-- CARD 2: ĐỊNH GIÁ & LỢI NHUẬN -->
          <div class="studio-card form-card">
            <div class="card-head-row">
              <span class="badge-black">02. ĐỊNH GIÁ CƠ SỞ &amp; BIÊN LỢI NHUẬN</span>
              <div class="margin-quick-btns">
                <span>Gợi ý giá bán:</span>
                <button type="button" @click="applyMarginPreset(30)">+30%</button>
                <button type="button" @click="applyMarginPreset(50)">+50%</button>
                <button type="button" @click="applyMarginPreset(65)">+65%</button>
              </div>
            </div>

            <div class="pricing-grid">
              <el-form-item label="Giá nhập cơ sở (VNĐ) (*)" prop="importPrice">
                <el-input-number
                  v-model="form.importPrice"
                  :min="0"
                  :step="50000"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>

              <el-form-item label="Giá bán niêm yết (VNĐ) (*)" prop="salePrice">
                <el-input-number
                  v-model="form.salePrice"
                  :min="0"
                  :step="50000"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>

              <div class="profit-box" :class="profitAmount >= 0 ? 'ok' : 'warn'">
                <span class="pb-label">LỢI NHUẬN / ĐÔI</span>
                <strong class="pb-val">{{ formatCurrency(profitAmount) }}</strong>
                <span class="pb-sub">Biên lợi nhuận: {{ profitMarginPct }}%</span>
              </div>
            </div>
          </div>

          <!-- CARD 3: CHẤT LIỆU & MÔ TẢ -->
          <div class="studio-card form-card">
            <div class="card-head-row">
              <span class="badge-black">03. CHẤT LIỆU &amp; MÔ TẢ CHI TIẾT</span>
            </div>

            <el-form-item label="Chất liệu cấu thành (Upper & Sole)" prop="material">
              <el-input
                v-model="form.material"
                placeholder="VD: Da thật cao cấp kết hợp đế cao su đúc lưu hóa..."
              />
              <div class="quick-chips-row">
                <button
                  v-for="chip in materialChips"
                  :key="chip"
                  type="button"
                  class="mini-chip"
                  @click="appendMaterial(chip)"
                >
                  + {{ chip }}
                </button>
              </div>
            </el-form-item>

            <el-form-item label="Mô tả sản phẩm" prop="description" style="margin-bottom: 0">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                placeholder="Mô tả phom dáng, công nghệ đệm, nguồn gốc thiết kế và gợi ý chọn size..."
              />
            </el-form-item>
          </div>

          <!-- CARD 4 (EDIT ONLY): COMPACT MATRIX PREVIEW OF PRODUCT VARIANTS -->
          <div v-if="isEdit" class="studio-card form-card">
            <div class="card-head-row">
              <span class="badge-black">04. BIẾN THỂ SKU HIỆN CÓ ({{ existingVariants.length }} SKU · {{ totalVariantStock }} ĐÔI)</span>
              <button type="button" class="btn-manage-sku" @click="goToVariantStudio">
                ⚡ Mở Ma trận sửa / thêm SKU →
              </button>
            </div>

            <div v-if="existingVariants.length === 0" class="empty-sku-strip">
              <span>Sản phẩm chưa có biến thể Size / Màu nào.</span>
              <button type="button" class="btn-outline-xs" @click="goToVariantStudio">+ Tạo biến thể ngay</button>
            </div>

            <div v-else class="mini-matrix-scroll">
              <table class="mini-matrix-table">
                <thead>
                  <tr>
                    <th class="sticky-col">MÀU \ SIZE</th>
                    <th v-for="s in matrixSizes" :key="s.key">{{ s.label }}</th>
                    <th class="sum-col">TỒN</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="c in matrixColors" :key="c.key">
                    <td class="sticky-col">
                      <span class="swatch-sm" :style="{ backgroundColor: c.hexCode }"></span>
                      <span class="cname">{{ c.name }}</span>
                    </td>
                    <td v-for="s in matrixSizes" :key="s.key">
                      <button
                        v-if="findVariant(s.key, c.key)"
                        type="button"
                        class="m-cell"
                        :class="findVariant(s.key, c.key).availableQuantity === 0 ? 'zero' : 'ok'"
                        :title="`Bấm để sửa SKU ${findVariant(s.key, c.key).sku}`"
                        @click="$router.push(`/admin/variants/${findVariant(s.key, c.key).id}/edit`)"
                      >
                        {{ findVariant(s.key, c.key).availableQuantity }}
                      </button>
                      <span v-else class="m-dash">—</span>
                    </td>
                    <td class="sum-col"><strong>{{ sumStockByColor(c.key) }}</strong></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </main>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Check,
  Loading,
  Grid,
  View,
  Picture,
  UploadFilled
} from '@element-plus/icons-vue'
import { productService } from '@/services/productService'
import { brandService } from '@/services/brandService'
import { categoryService } from '@/services/categoryService'
import { fileService } from '@/services/fileService'
import { formatCurrency } from '@/utils/formatters'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const loadingPage = ref(false)
const saving = ref(false)
const imgError = ref(false)

const fileInputRef = ref(null)
const uploadingImage = ref(false)
const isDragOver = ref(false)
const activePreviewImage = ref('')
const imageItems = ref([])

const pendingCount = computed(() => imageItems.value.filter((i) => i.isNew).length)
const currentPreviewUrl = computed(
  () => activePreviewImage.value || imageItems.value[0]?.previewUrl || ''
)

const isEdit = computed(() => !!route.params.id)
const productId = computed(() => route.params.id)

const brands = ref([])
const categories = ref([])
const existingVariants = ref([])

const form = reactive({
  productCode: '',
  productName: '',
  brandId: null,
  categoryId: null,
  material: 'Da thật cao cấp kết hợp đế cao su đúc lưu hóa',
  description: '',
  thumbnailUrl: '',
  imageUrls: [],
  importPrice: 1800000,
  salePrice: 2950000,
  status: 'ACTIVE'
})

const rules = {
  productCode: [
    { required: true, message: 'Vui lòng nhập mã sản phẩm', trigger: 'blur' },
    { max: 30, message: 'Mã sản phẩm tối đa 30 ký tự', trigger: 'blur' }
  ],
  productName: [
    { required: true, message: 'Vui lòng nhập tên sản phẩm', trigger: 'blur' },
    { max: 200, message: 'Tên sản phẩm tối đa 200 ký tự', trigger: 'blur' }
  ],
  brandId: [{ required: true, message: 'Vui lòng chọn thương hiệu', trigger: 'change' }],
  categoryId: [{ required: true, message: 'Vui lòng chọn danh mục', trigger: 'change' }],
  importPrice: [{ required: true, message: 'Vui lòng nhập giá nhập cơ sở', trigger: 'blur' }],
  salePrice: [{ required: true, message: 'Vui lòng nhập giá bán niêm yết', trigger: 'blur' }]
}

const materialChips = [
  'Da thật (Leather)',
  'Da lộn (Suede)',
  'Vải lưới Flyknit',
  'Đệm khí Air Zoom',
  'Đế bọt ZoomX',
  'Chống nước GORE-TEX'
]

const profitAmount = computed(() => Number(form.salePrice || 0) - Number(form.importPrice || 0))
const profitMarginPct = computed(() => {
  const sale = Number(form.salePrice || 0)
  if (sale <= 0) return 0
  return Math.round((profitAmount.value / sale) * 100)
})

const totalVariantStock = computed(() =>
  existingVariants.value.reduce((acc, v) => acc + Number(v.availableQuantity || 0), 0)
)

function sizeKey(v) {
  return String(v.sizeId ?? v.sizeValue ?? '')
}
function colorKey(v) {
  return String(v.colorId ?? v.colorName ?? '')
}

const matrixSizes = computed(() => {
  const map = new Map()
  for (const v of existingVariants.value) {
    const k = sizeKey(v)
    if (!map.has(k)) map.set(k, { key: k, label: v.sizeValue || k, num: parseFloat(v.sizeValue) || 0 })
  }
  return Array.from(map.values()).sort((a, b) => a.num - b.num)
})

const matrixColors = computed(() => {
  const map = new Map()
  for (const v of existingVariants.value) {
    const k = colorKey(v)
    if (!map.has(k)) map.set(k, { key: k, name: v.colorName || k, hexCode: v.hexCode || '#111' })
  }
  return Array.from(map.values())
})

function findVariant(sKey, cKey) {
  return existingVariants.value.find((v) => sizeKey(v) === sKey && colorKey(v) === cKey)
}

function sumStockByColor(cKey) {
  return existingVariants.value
    .filter((v) => colorKey(v) === cKey)
    .reduce((acc, v) => acc + Number(v.availableQuantity || 0), 0)
}

function getBrandName(brandId) {
  const b = brands.value.find((item) => item.id === brandId)
  return b ? b.brandName : 'ADHA SNEAKER'
}

function getCategoryName(catId) {
  const c = categories.value.find((item) => item.id === catId)
  return c ? c.categoryName : 'FOOTWEAR'
}

function applyMarginPreset(markupPct) {
  const base = Number(form.importPrice || 0)
  if (base <= 0) return
  const raw = base * (1 + markupPct / 100)
  form.salePrice = Math.round(raw / 10000) * 10000
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileInputChange(e) {
  const files = e.target?.files
  if (files && files.length > 0) queueSelectedFiles(files)
  if (e.target) e.target.value = ''
}

function handleDropFile(e) {
  isDragOver.value = false
  const files = e.dataTransfer?.files
  if (files && files.length > 0) queueSelectedFiles(files)
}

function queueSelectedFiles(files) {
  const validFiles = Array.from(files).filter((f) => f.type.startsWith('image/'))
  if (!validFiles.length) {
    ElMessage.error('Vui lòng chỉ chọn tệp hình ảnh (PNG, JPG, WEBP, GIF, SVG)!')
    return
  }
  for (const f of validFiles) {
    if (f.size > 20 * 1024 * 1024) {
      ElMessage.error(`Ảnh "${f.name}" vượt quá giới hạn 20MB!`)
      return
    }
  }

  const newItems = validFiles.map((f) => ({
    id: `pending-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    file: f,
    previewUrl: URL.createObjectURL(f),
    isNew: true
  }))

  imageItems.value.push(...newItems)
  activePreviewImage.value = newItems[newItems.length - 1].previewUrl
  imgError.value = false
  ElMessage.success(`Đã thêm ${newItems.length} ảnh xem trước (upload MinIO khi ấn Lưu)`)
}

function setAsPrimary(index) {
  if (index <= 0 || index >= imageItems.value.length) return
  const item = imageItems.value.splice(index, 1)[0]
  imageItems.value.unshift(item)
  activePreviewImage.value = item.previewUrl
}

function removeImage(index) {
  if (index < 0 || index >= imageItems.value.length) return
  const removed = imageItems.value.splice(index, 1)[0]
  if (removed.isNew && removed.previewUrl.startsWith('blob:')) {
    URL.revokeObjectURL(removed.previewUrl)
  }
  if (activePreviewImage.value === removed.previewUrl) {
    activePreviewImage.value = imageItems.value[0]?.previewUrl || ''
  }
}

function clearAllImages() {
  imageItems.value.forEach((item) => {
    if (item.isNew && item.previewUrl.startsWith('blob:')) {
      URL.revokeObjectURL(item.previewUrl)
    }
  })
  imageItems.value = []
  activePreviewImage.value = ''
  imgError.value = false
}

function appendMaterial(chip) {
  if (!form.material) form.material = chip
  else if (!form.material.includes(chip)) form.material = `${form.material}, ${chip}`
}

function autoGenerateCode() {
  const rand = Math.floor(10000 + Math.random() * 90000)
  form.productCode = `SP${rand}`
}

function handleCancel() {
  router.push('/admin/products')
}

function goToVariantStudio() {
  if (existingVariants.value.length > 0 && existingVariants.value[0]?.id) {
    router.push(`/admin/variants/${existingVariants.value[0].id}/edit`)
  } else {
    router.push({ path: '/admin/variants/create', query: { productId: productId.value } })
  }
}

async function loadInitialData() {
  loadingPage.value = true
  try {
    const [bRes, cRes] = await Promise.all([
      brandService.getAllBrands(),
      categoryService.getAllCategories()
    ])
    brands.value = bRes.data || []
    categories.value = cRes.data || []

    if (isEdit.value) {
      const pRes = await productService.getProductById(productId.value)
      const p = pRes.data
      form.productCode = p.productCode || ''
      form.productName = p.productName || ''
      form.brandId = p.brandId || null
      form.categoryId = p.categoryId || null
      form.material = p.material || ''
      form.description = p.description || ''

      const rawImages = p.imageUrls?.length ? p.imageUrls : (p.thumbnailUrl ? [p.thumbnailUrl] : [])
      imageItems.value = rawImages.map((url, i) => ({
        id: `existing-${i}-${Date.now()}`,
        file: null,
        previewUrl: url,
        isNew: false
      }))
      activePreviewImage.value = imageItems.value[0]?.previewUrl || ''

      form.importPrice = Number(p.importPrice) || 0
      form.salePrice = Number(p.salePrice) || 0
      form.status = p.status || 'ACTIVE'
      existingVariants.value = p.variants || []
    } else {
      autoGenerateCode()
      if (brands.value.length) form.brandId = brands.value[0].id
      if (categories.value.length) form.categoryId = categories.value[0].id
    }
  } catch (_) {
    ElMessage.error('Không thể tải thông tin sản phẩm hoặc danh mục')
  } finally {
    loadingPage.value = false
  }
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('Vui lòng kiểm tra lại các trường thông tin bắt buộc (*)')
      return
    }

    saving.value = true
    try {
      const pendingItems = imageItems.value.filter((i) => i.isNew && i.file)
      if (pendingItems.length > 0) {
        uploadingImage.value = true
        const uploadResults = await fileService.uploadImages(pendingItems.map((i) => i.file))

        let resIdx = 0
        for (const item of imageItems.value) {
          if (item.isNew && item.file) {
            const uploaded = uploadResults[resIdx]
            if (uploaded && uploaded.url) {
              if (item.previewUrl.startsWith('blob:')) URL.revokeObjectURL(item.previewUrl)
              item.previewUrl = uploaded.url
              item.isNew = false
              item.file = null
            }
            resIdx++
          }
        }
        uploadingImage.value = false
      }

      const finalImageUrls = imageItems.value.map((i) => i.previewUrl).filter(Boolean)
      const payload = {
        ...form,
        thumbnailUrl: finalImageUrls[0] || '',
        imageUrls: finalImageUrls
      }

      if (isEdit.value) {
        await productService.updateProduct(productId.value, payload)
        ElMessage.success('Cập nhật sản phẩm Sneaker thành công!')
        router.push(`/admin/products/${productId.value}`)
      } else {
        const res = await productService.createProduct(payload)
        ElMessage.success('Khởi tạo sản phẩm Sneaker mới thành công!')
        const createdId = res.data?.id
        router.push(createdId ? `/admin/products/${createdId}` : '/admin/products')
      }
    } catch (err) {
      ElMessage.error(err.response?.data?.message || 'Có lỗi xảy ra khi lưu sản phẩm!')
    } finally {
      uploadingImage.value = false
      saving.value = false
    }
  })
}

function handleKeydown(e) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 's') {
    e.preventDefault()
    if (!saving.value) submitForm()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  loadInitialData()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
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

  &.mode-create {
    background: #000000;
    color: #ffffff;
  }
  &.mode-edit {
    background: #eff6ff;
    color: #1d4ed8;
    border: 1px solid #93c5fd;
  }
}

.topbar-live-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
  min-width: 0;

  .live-name {
    font-size: 13.5px;
    font-weight: 800;
    color: #000000;
    max-width: 260px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .kpi-chip {
    font-size: 11px;
    color: #525252;
    background: #f5f5f5;
    border: 1px solid #e5e5e5;
    padding: 3px 7px;
    font-variant-numeric: tabular-nums;

    strong { color: #000000; }

    &.kpi-green {
      background: #ecfdf5;
      border-color: #6ee7b7;
      color: #065f46;
      strong { color: #047857; }
    }
    &.kpi-red {
      background: #fef2f2;
      border-color: #fca5a5;
      color: #991b1b;
      strong { color: #dc2626; }
    }
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
  padding: 6px 10px;
  cursor: pointer;

  &:hover {
    border-color: #000000;
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
  padding: 6px 10px;
  cursor: pointer;

  &:hover {
    background: #000000;
    color: #ffffff;
  }
}

.btn-save-primary {
  font-weight: 800;
  letter-spacing: 0.04em;
}

/* 2. STUDIO GRID */
.studio-grid {
  display: grid;
  grid-template-columns: 330px 1fr;
  gap: 12px;
  align-items: start;
}

.studio-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
}

.card-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.badge-black {
  background: #000000;
  color: #ffffff;
  font-size: 9.5px;
  font-weight: 900;
  letter-spacing: 0.08em;
  padding: 4px 8px;
}

/* LEFT MEDIA STUDIO */
.media-column {
  position: sticky;
  top: 66px;
}

.media-card {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  .card-head-row {
    margin-bottom: 0;
  }

  .media-head-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .pending-pill {
    font-size: 10px;
    font-weight: 800;
    background: #fffbeb;
    border: 1px solid #fcd34d;
    color: #b45309;
    padding: 2px 6px;
  }

  .btn-text-danger {
    background: none;
    border: none;
    color: #dc2626;
    font-size: 11px;
    font-weight: 700;
    cursor: pointer;
    padding: 0;

    &:hover {
      text-decoration: underline;
    }
  }
}

.preview-stage {
  position: relative;
  width: 100%;
  height: 220px;
  background: #fafafa;
  border: 1px solid #e5e5e5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  .preview-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .preview-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 5px;
    color: #a3a3a3;
    text-align: center;
    padding: 16px;

    strong {
      font-size: 11.5px;
      color: #525252;
    }
    span {
      font-size: 11px;
    }
  }

  .preview-status-tag {
    position: absolute;
    top: 8px;
    right: 8px;
    font-size: 9.5px;
    font-weight: 900;
    padding: 2px 7px;

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
}

.live-card-strip {
  background: #fafafa;
  border: 1px solid #e5e5e5;
  padding: 8px 10px;

  .lc-meta {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 10.5px;
    font-weight: 700;
    text-transform: uppercase;
    color: #737373;
  }

  .lc-bottom {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-top: 3px;

    .lc-price {
      font-size: 15px;
      font-weight: 900;
      color: #000000;
      font-variant-numeric: tabular-nums;
    }
    .lc-code {
      font-size: 11px;
      font-family: var(--font-mono);
      color: #525252;
    }
  }
}

.upload-zone {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1.5px dashed #a3a3a3;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.14s ease;

  .hidden-input {
    display: none;
  }

  .uz-text {
    display: flex;
    flex-direction: column;
    line-height: 1.25;

    strong {
      font-size: 11.5px;
      color: #171717;
    }
    span {
      font-size: 10.5px;
      color: #737373;
    }
  }

  &:hover,
  &.dragover {
    border-color: #000000;
    background: #f5f5f5;
  }
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.g-tile {
  position: relative;
  aspect-ratio: 1;
  border: 1px solid #d4d4d4;
  overflow: hidden;
  cursor: pointer;
  background: #fafafa;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &.primary {
    border-color: #000000;
  }

  &.viewing {
    outline: 2px solid #000000;
    outline-offset: -1px;
  }

  .g-badge-main,
  .g-badge-num {
    position: absolute;
    left: 2px;
    top: 2px;
    font-size: 8.5px;
    font-weight: 900;
    padding: 1px 4px;
    background: rgba(0, 0, 0, 0.8);
    color: #ffffff;
  }

  .g-badge-new {
    position: absolute;
    left: 2px;
    bottom: 2px;
    font-size: 8px;
    font-weight: 900;
    padding: 1px 4px;
    background: #10b981;
    color: #ffffff;
  }

  .g-actions {
    position: absolute;
    right: 2px;
    top: 2px;
    display: flex;
    gap: 2px;

    .g-act-btn {
      width: 17px;
      height: 17px;
      border: none;
      background: rgba(255, 255, 255, 0.92);
      color: #000000;
      font-size: 11px;
      font-weight: 800;
      line-height: 1;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;

      &:hover {
        background: #000000;
        color: #ffffff;
      }

      &.del:hover {
        background: #dc2626;
        color: #ffffff;
      }
    }
  }
}

/* RIGHT FORM CARDS */
.fields-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-card {
  padding: 14px 16px;

  :deep(.el-form-item) {
    margin-bottom: 12px;
  }

  :deep(.el-form-item__label) {
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #404040;
    padding-bottom: 4px;
    line-height: 1.2;
  }
}

.status-toggle-group {
  display: inline-flex;
  border: 1px solid #d4d4d4;

  .st-btn {
    padding: 4px 10px;
    background: #ffffff;
    border: none;
    font-size: 10.5px;
    font-weight: 800;
    color: #525252;
    cursor: pointer;

    &.active {
      background: #059669;
      color: #ffffff;
    }

    &.inactive.active {
      background: #525252;
      color: #ffffff;
    }
  }
}

.code-input-group {
  display: flex;
  gap: 6px;
  width: 100%;

  .mono-input :deep(input) {
    font-family: var(--font-mono);
    font-weight: 800;
    text-transform: uppercase;
  }

  .btn-gen-code {
    background: #000000;
    color: #ffffff;
    border: 1px solid #000000;
    padding: 0 10px;
    font-size: 11px;
    font-weight: 800;
    cursor: pointer;
    white-space: nowrap;

    &:hover {
      background: #262626;
    }
  }
}

.margin-quick-btns {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #737373;

  button {
    background: #fafafa;
    border: 1px solid #d4d4d4;
    color: #171717;
    font-size: 10.5px;
    font-weight: 800;
    padding: 2px 7px;
    cursor: pointer;

    &:hover {
      border-color: #000000;
      background: #000000;
      color: #ffffff;
    }
  }
}

.pricing-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  align-items: start;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.profit-box {
  border: 1px solid #d4d4d4;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-height: 60px;
  justify-content: center;

  &.ok {
    background: #f0fdf4;
    border-color: #86efac;
    .pb-val { color: #047857; }
  }
  &.warn {
    background: #fef2f2;
    border-color: #fca5a5;
    .pb-val { color: #dc2626; }
  }

  .pb-label {
    font-size: 9.5px;
    font-weight: 800;
    color: #525252;
    letter-spacing: 0.05em;
  }
  .pb-val {
    font-size: 15px;
    font-weight: 900;
    font-variant-numeric: tabular-nums;
  }
  .pb-sub {
    font-size: 10.5px;
    color: #525252;
    font-weight: 600;
  }
}

.quick-chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 6px;

  .mini-chip {
    background: #fafafa;
    border: 1px solid #d4d4d4;
    color: #404040;
    font-size: 10.5px;
    font-weight: 700;
    padding: 3px 8px;
    cursor: pointer;

    &:hover {
      border-color: #000000;
      background: #000000;
      color: #ffffff;
    }
  }
}

/* CARD 4 MINI VARIANT MATRIX */
.btn-manage-sku {
  background: #000000;
  color: #ffffff;
  border: 1px solid #000000;
  font-size: 11px;
  font-weight: 800;
  padding: 5px 11px;
  cursor: pointer;

  &:hover {
    background: #262626;
  }
}

.empty-sku-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #fafafa;
  border: 1px dashed #d4d4d4;
  padding: 12px 14px;
  font-size: 12px;
  color: #525252;

  .btn-outline-xs {
    background: #ffffff;
    border: 1px solid #000000;
    color: #000000;
    font-size: 11px;
    font-weight: 800;
    padding: 5px 10px;
    cursor: pointer;

    &:hover {
      background: #000000;
      color: #ffffff;
    }
  }
}

.mini-matrix-scroll {
  overflow-x: auto;
}

.mini-matrix-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 11.5px;

  th,
  td {
    border: 1px solid #e5e5e5;
    padding: 4px 5px;
    text-align: center;
  }

  thead th {
    background: #171717;
    color: #ffffff;
    font-size: 10.5px;
    font-weight: 800;
    font-family: var(--font-mono);
  }

  .sticky-col {
    position: sticky;
    left: 0;
    background: #fafafa;
    text-align: left;
    min-width: 135px;
    white-space: nowrap;
  }

  thead .sticky-col {
    background: #000000;
  }

  .swatch-sm {
    display: inline-block;
    width: 10px;
    height: 10px;
    border: 1px solid rgba(0, 0, 0, 0.25);
    margin-right: 6px;
    vertical-align: middle;
  }

  .cname {
    font-weight: 700;
    color: #171717;
  }

  .m-cell {
    min-width: 34px;
    padding: 3px 5px;
    border: 1px solid #93c5fd;
    background: #eff6ff;
    color: #1e3a8a;
    font-weight: 800;
    font-family: var(--font-mono);
    font-size: 11.5px;
    cursor: pointer;

    &.zero {
      background: #fef2f2;
      border-color: #fca5a5;
      color: #b91c1c;
    }

    &:hover {
      background: #000000;
      border-color: #000000;
      color: #ffffff;
    }
  }

  .m-dash {
    color: #d4d4d4;
  }

  .sum-col {
    background: #fafafa;
    font-family: var(--font-mono);
  }
}

@media (max-width: 1024px) {
  .studio-grid {
    grid-template-columns: 1fr;
  }
  .media-column {
    position: static;
  }
}

@media (max-width: 768px) {
  .studio-topbar {
    position: static;
  }
  .topbar-right {
    width: 100%;
    .btn-save-primary {
      flex: 1;
    }
  }
  .pricing-grid {
    grid-template-columns: 1fr;
  }
}
</style>
