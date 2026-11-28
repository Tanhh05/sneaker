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
        <!-- LEFT COLUMN: SPACIOUS COLOR-TABBED MEDIA STUDIO (50% WIDTH) -->
        <aside class="media-column">
          <div class="studio-card media-card">
            <div class="card-head-row">
              <span class="badge-black">HÌNH ẢNH THEO MÀU &amp; XEM TRƯỚC</span>
              <div class="media-head-right">
                <span class="media-summary-pill">{{ selectedColorIds.length }} màu · {{ imageItems.length }} ảnh</span>
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
                <el-icon :size="40"><Picture /></el-icon>
                <strong>CHƯA CÓ HÌNH ẢNH</strong>
                <span>Chọn hoặc thêm màu bên dưới rồi kéo thả 4 ảnh góc chụp</span>
              </div>
              <span class="preview-status-tag" :class="form.status === 'ACTIVE' ? 'st-active' : 'st-inactive'">
                {{ form.status }}
              </span>
              <span v-if="activeColorObj" class="preview-color-pill">
                <i class="swatch-dot" :style="{ backgroundColor: activeColorObj.hexCode || '#111' }"></i>
                {{ activeColorObj.colorName }} ({{ visibleImages.length }}/4)
              </span>
            </div>

            <!-- Sleek 1-line Live Card Strip -->
            <div class="live-card-strip">
              <div class="lc-left">
                <span class="lc-meta">{{ getBrandName(form.brandId) }} · {{ getCategoryName(form.categoryId) }}</span>
                <span class="lc-code">{{ form.productCode || '---' }}</span>
              </div>
              <strong class="lc-price">{{ formatCurrency(form.salePrice || 0) }}</strong>
            </div>

            <!-- Unified Scalable Color Bar (handles 1,000+ colors with instant search popover) -->
            <div class="color-studio-box">
              <div class="cs-head">
                <span class="cs-title">MÀU ĐANG CHỌN ({{ selectedColorObjects.length }})</span>
                <div class="cs-head-actions">
                  <input
                    v-if="selectedColorObjects.length > 8"
                    v-model="selectedTabFilter"
                    type="text"
                    class="cs-tab-filter"
                    placeholder="Lọc tab màu..."
                  />
                  <el-popover
                    v-model:visible="colorPickerOpen"
                    placement="bottom-end"
                    :width="320"
                    trigger="click"
                    popper-class="color-picker-popover"
                    @show="onColorPickerShow"
                  >
                    <template #reference>
                      <button type="button" class="btn-add-color">
                        + Thêm màu <span v-if="colors.length > 0" class="btn-color-total">({{ colors.length }})</span>
                      </button>
                    </template>

                    <div class="cp-pop">
                      <div class="cp-search-row">
                        <input
                          ref="colorSearchInputRef"
                          v-model="colorSearchQuery"
                          type="text"
                          class="cp-search-input"
                          :placeholder="`Tìm trong ${colors.length} màu (tên, mã, hex)...`"
                          @keydown.enter.prevent="pickFirstFilteredColor"
                        />
                        <button
                          v-if="colorSearchQuery"
                          type="button"
                          class="cp-clear-btn"
                          @click="colorSearchQuery = ''"
                        >
                          ×
                        </button>
                      </div>

                      <div class="cp-list">
                        <div v-if="displayedPickerColors.length === 0" class="cp-empty">
                          Không tìm thấy màu phù hợp với "{{ colorSearchQuery }}"
                        </div>
                        <button
                          v-for="c in displayedPickerColors"
                          :key="c.id"
                          type="button"
                          class="cp-item"
                          :class="{ selected: selectedColorIds.includes(c.id) }"
                          @click="pickColorFromPopover(c.id)"
                        >
                          <span class="swatch-dot lg" :style="{ backgroundColor: c.hexCode || '#111' }"></span>
                          <span class="cp-name">{{ c.colorName }}</span>
                          <span class="cp-code">{{ c.colorCode }}</span>
                          <span v-if="selectedColorIds.includes(c.id)" class="cp-picked">
                            ✓ {{ countImagesByColor(c.id) }}/4
                          </span>
                          <span v-else class="cp-add-tag">+ Chọn</span>
                        </button>
                      </div>

                      <div class="cp-foot">
                        <span v-if="filteredPickerColors.length > displayedPickerColors.length">
                          Hiển thị {{ displayedPickerColors.length }}/{{ filteredPickerColors.length }} màu — gõ thêm để lọc nhanh
                        </span>
                        <span v-else>
                          {{ filteredPickerColors.length }} màu · Nhấn Enter để chọn nhanh kết quả đầu
                        </span>
                      </div>
                    </div>
                  </el-popover>
                </div>
              </div>

              <!-- Selected Color Tabs ONLY (zero clutter even with 1,000 DB colors) -->
              <div v-if="filteredSelectedColorObjects.length > 0 || unassignedImagesCount > 0" class="color-tabs-bar">
                <div
                  v-for="c in filteredSelectedColorObjects"
                  :key="c.id"
                  class="ctab"
                  :class="{
                    active: activeColorTab === c.id,
                    missing: variantColorIdSet.has(c.id) && countImagesByColor(c.id) === 0
                  }"
                  @click="switchColorTab(c.id)"
                >
                  <span class="swatch-dot" :style="{ backgroundColor: c.hexCode || '#111' }"></span>
                  <span class="ctab-name">{{ c.colorName }}</span>
                  <span class="ctab-badge" :class="getColorCountClass(c.id)">
                    {{ countImagesByColor(c.id) }}/4
                  </span>
                  <span
                    v-if="isEdit && existingVariants.length > 0 && !variantColorIdSet.has(c.id)"
                    class="ctab-nosku"
                    title="Màu này có ảnh nhưng chưa tạo biến thể SKU"
                  >
                    chưa SKU
                  </span>
                  <button
                    v-if="!variantColorIdSet.has(c.id)"
                    type="button"
                    class="ctab-close"
                    title="Bỏ màu này khỏi bộ ảnh"
                    @click.stop="removeColorTab(c.id)"
                  >
                    ×
                  </button>
                </div>
                <div
                  v-if="unassignedImagesCount > 0"
                  class="ctab unassigned"
                  :class="{ active: activeColorTab === 'unassigned' }"
                  @click="switchColorTab('unassigned')"
                >
                  <span class="ctab-name">Chưa gắn màu</span>
                  <span class="ctab-badge warn">{{ unassignedImagesCount }}</span>
                </div>
              </div>
              <div v-else class="cs-no-color-hint">
                Chưa chọn màu nào — bấm <strong>+ Thêm màu</strong> ở góc phải để thêm màu &amp; tải ảnh.
              </div>
            </div>

            <!-- Color-Aware Drag & Drop Zone -->
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
                <strong v-if="uploadingImage">Đang tải ảnh lên MinIO S3...</strong>
                <strong v-else-if="activeColorObj">
                  Kéo thả / chọn ảnh cho màu
                  <span class="uz-color-tag">
                    <i class="swatch-dot" :style="{ backgroundColor: activeColorObj.hexCode || '#111' }"></i>
                    {{ activeColorObj.colorName }}
                  </span>
                </strong>
                <strong v-else>Bấm "+ Thêm màu" trước hoặc kéo thả ảnh từ máy</strong>
                <span>Khuyến nghị 4 góc chụp/màu · Tự gắn đúng màu đang mở · Upload khi bấm Lưu</span>
              </div>
            </div>

            <!-- Active Color Gallery Grid (4 columns — all 4 angles side-by-side!) -->
            <div class="gallery-section-bar">
              <span class="gs-label">
                <template v-if="activeColorObj">
                  Bộ ảnh màu <strong>{{ activeColorObj.colorName }}</strong> ({{ visibleImages.length }}/4)
                </template>
                <template v-else-if="activeColorTab === 'unassigned'">
                  Ảnh chưa gắn màu ({{ visibleImages.length }})
                </template>
                <template v-else>
                  Tất cả ảnh ({{ imageItems.length }})
                </template>
              </span>
              <button
                v-if="visibleImages.length > 0 && activeColorObj"
                type="button"
                class="btn-text-danger"
                @click="clearActiveColorImages"
              >
                Xóa ảnh màu này
              </button>
            </div>

            <div v-if="visibleImages.length === 0" class="color-empty-box">
              <span v-if="activeColorObj">
                Chưa có ảnh cho màu <strong>{{ activeColorObj.colorName }}</strong>. Kéo thả 4 ảnh vào khung trên để gắn tự động.
              </span>
              <span v-else>
                Bấm <strong>+ Thêm màu</strong> để chọn màu bán cho sản phẩm này.
              </span>
            </div>

            <div v-else class="gallery-grid">
              <div
                v-for="(item, localIdx) in visibleImages"
                :key="item.id"
                class="g-tile-wrap"
              >
                <div
                  class="g-tile"
                  :class="{
                    primary: item.id === primaryImageId,
                    viewing: currentPreviewUrl === item.previewUrl
                  }"
                  @click="activePreviewImage = item.previewUrl"
                >
                  <img :src="item.previewUrl" alt="" />
                  <span v-if="item.id === primaryImageId" class="g-badge-main">★ BÌA SP</span>
                  <span v-else-if="localIdx === 0 && activeColorObj" class="g-badge-swatch">#1 SWATCH</span>
                  <span v-else class="g-badge-num">#{{ localIdx + 1 }}</span>
                  <span v-if="item.isNew" class="g-badge-new">MỚI</span>

                  <div class="g-actions">
                    <button
                      v-if="localIdx > 0"
                      type="button"
                      class="g-act-btn"
                      title="Đưa lên trước (ảnh #1 làm ảnh đại diện màu)"
                      @click.stop="moveImageInActiveColor(item.id, -1)"
                    >
                      ←
                    </button>
                    <button
                      v-if="localIdx < visibleImages.length - 1"
                      type="button"
                      class="g-act-btn"
                      title="Đẩy ra sau"
                      @click.stop="moveImageInActiveColor(item.id, 1)"
                    >
                      →
                    </button>
                    <button
                      v-if="item.id !== primaryImageId"
                      type="button"
                      class="g-act-btn"
                      title="Đặt làm ảnh bìa chính của sản phẩm"
                      @click.stop="setAsPrimaryById(item.id)"
                    >
                      ★
                    </button>
                    <button
                      type="button"
                      class="g-act-btn del"
                      title="Xóa ảnh này"
                      @click.stop="removeImageById(item.id)"
                    >
                      ×
                    </button>
                  </div>
                </div>
                <select
                  :value="item.colorId || ''"
                  class="g-color-select"
                  title="Chuyển ảnh này sang màu khác đang chọn"
                  @change="reassignImageColor(item, $event.target.value)"
                >
                  <option value="">Chưa gắn màu</option>
                  <option v-for="c in selectedColorObjects" :key="c.id" :value="c.id">
                    {{ c.colorName }}
                  </option>
                </select>
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
                      Tạo mã
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
                  <QuickSelect
                    v-model="form.brandId"
                    :options="brands"
                    value-key="id"
                    :label-key="(b) => `${b.brandName}${b.country ? ` (${b.country})` : ''}`"
                    :search-keys="['brandName', 'country']"
                    placeholder="Chọn hãng giày"
                    search-placeholder="Tìm nhanh thương hiệu, quốc gia..."
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="Danh mục giày (*)" prop="categoryId">
                  <QuickSelect
                    v-model="form.categoryId"
                    :options="categories"
                    value-key="id"
                    label-key="categoryName"
                    :search-keys="['categoryName', 'categoryCode']"
                    placeholder="Chọn danh mục"
                    search-placeholder="Tìm nhanh danh mục..."
                  />
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
                :rows="2"
                placeholder="Mô tả phom dáng, công nghệ đệm, nguồn gốc thiết kế và gợi ý chọn size..."
              />
            </el-form-item>
          </div>

          <!-- CARD 4 (EDIT ONLY): COMPACT MATRIX PREVIEW OF PRODUCT VARIANTS -->
          <div v-if="isEdit" class="studio-card form-card">
            <div class="card-head-row">
              <span class="badge-black">04. BIẾN THỂ SKU HIỆN CÓ ({{ existingVariants.length }} SKU · {{ totalVariantStock }} ĐÔI)</span>
              <button type="button" class="btn-manage-sku" @click="goToVariantStudio">
                Mở Ma trận sửa / thêm SKU →
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
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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
import { variantService } from '@/services/variantService'
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
const selectedColorIds = ref([])
const activeColorTab = ref(null) // number (colorId) | 'unassigned' | null
const primaryImageId = ref(null)
const colorPickerOpen = ref(false)
const colorSearchQuery = ref('')
const colorSearchInputRef = ref(null)
const selectedTabFilter = ref('')

function normalizeViText(str) {
  return String(str || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim()
}

const isEdit = computed(() => !!route.params.id)
const productId = computed(() => route.params.id)

const brands = ref([])
const categories = ref([])
const colors = ref([])
const existingVariants = ref([])

const variantColorIdSet = computed(
  () => new Set(existingVariants.value.map((v) => Number(v.colorId)).filter(Boolean))
)

const selectedColorObjects = computed(() =>
  selectedColorIds.value
    .map((id) => colors.value.find((c) => c.id === id))
    .filter(Boolean)
)

const filteredSelectedColorObjects = computed(() => {
  const q = normalizeViText(selectedTabFilter.value)
  if (!q) return selectedColorObjects.value
  return selectedColorObjects.value.filter((c) =>
    normalizeViText(`${c.colorName} ${c.colorCode || ''}`).includes(q)
  )
})

const filteredPickerColors = computed(() => {
  const q = normalizeViText(colorSearchQuery.value)
  const list = !q
    ? colors.value
    : colors.value.filter((c) =>
        normalizeViText(`${c.colorName} ${c.colorCode || ''} ${c.hexCode || ''}`).includes(q)
      )
  // Put unselected matches cleanly after selected ones or keep fast natural order
  return list
})

const MAX_PICKER_RENDER = 40
const displayedPickerColors = computed(() =>
  filteredPickerColors.value.slice(0, MAX_PICKER_RENDER)
)

function onColorPickerShow() {
  colorSearchQuery.value = ''
  nextTick(() => {
    colorSearchInputRef.value?.focus()
  })
}

function pickColorFromPopover(colorId) {
  selectOrActivateColor(colorId)
  colorPickerOpen.value = false
  colorSearchQuery.value = ''
}

function pickFirstFilteredColor() {
  const first = displayedPickerColors.value[0]
  if (first) pickColorFromPopover(first.id)
}

const activeColorObj = computed(() => {
  if (!activeColorTab.value || activeColorTab.value === 'unassigned') return null
  return colors.value.find((c) => c.id === activeColorTab.value) || null
})

const unassignedImagesCount = computed(
  () => imageItems.value.filter((i) => !i.colorId).length
)

const visibleImages = computed(() => {
  if (activeColorTab.value === 'unassigned') {
    return imageItems.value.filter((i) => !i.colorId)
  }
  if (typeof activeColorTab.value === 'number') {
    return imageItems.value.filter((i) => Number(i.colorId) === activeColorTab.value)
  }
  return imageItems.value
})

const pendingCount = computed(() => imageItems.value.filter((i) => i.isNew).length)
const currentPreviewUrl = computed(() => {
  if (activePreviewImage.value && imageItems.value.some((i) => i.previewUrl === activePreviewImage.value)) {
    return activePreviewImage.value
  }
  return visibleImages.value[0]?.previewUrl || imageItems.value[0]?.previewUrl || ''
})

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

function countImagesByColor(colorId) {
  return imageItems.value.filter((i) => Number(i.colorId) === Number(colorId)).length
}

function getColorCountClass(colorId) {
  const n = countImagesByColor(colorId)
  if (n >= 4) return 'ok'
  if (n > 0) return 'warn'
  return 'zero'
}

function selectOrActivateColor(colorId) {
  if (!selectedColorIds.value.includes(colorId)) {
    selectedColorIds.value.push(colorId)
  }
  switchColorTab(colorId)
}

function switchColorTab(tabKey) {
  activeColorTab.value = tabKey
  const list = tabKey === 'unassigned'
    ? imageItems.value.filter((i) => !i.colorId)
    : imageItems.value.filter((i) => Number(i.colorId) === Number(tabKey))
  activePreviewImage.value = list[0]?.previewUrl || ''
  imgError.value = false
}

async function removeColorTab(colorId) {
  if (variantColorIdSet.value.has(colorId)) return
  const colorImgs = imageItems.value.filter((i) => Number(i.colorId) === Number(colorId))
  if (colorImgs.length > 0) {
    const cObj = colors.value.find((c) => c.id === colorId)
    try {
      await ElMessageBox.confirm(
        `Màu "${cObj?.colorName || colorId}" đang có ${colorImgs.length} ảnh. Bỏ màu này sẽ xóa luôn ${colorImgs.length} ảnh đó, bạn chắc chắn chứ?`,
        'Xác nhận bỏ màu',
        { type: 'warning', confirmButtonText: 'Bỏ màu & xóa ảnh', cancelButtonText: 'Giữ lại' }
      )
    } catch (_) {
      return
    }
    colorImgs.forEach((item) => {
      if (item.isNew && item.previewUrl.startsWith('blob:')) URL.revokeObjectURL(item.previewUrl)
    })
    imageItems.value = imageItems.value.filter((i) => Number(i.colorId) !== Number(colorId))
    ensureValidPrimaryImage()
  }
  selectedColorIds.value = selectedColorIds.value.filter((id) => id !== colorId)
  if (activeColorTab.value === colorId) {
    const nextTab = selectedColorIds.value[0] ?? (unassignedImagesCount.value > 0 ? 'unassigned' : null)
    switchColorTab(nextTab)
  }
}

function ensureValidPrimaryImage() {
  if (primaryImageId.value && imageItems.value.some((i) => i.id === primaryImageId.value)) return
  primaryImageId.value = imageItems.value[0]?.id || null
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

  // Auto-attach active color tab (fallback to first selected color if none active)
  let targetColorId = null
  if (typeof activeColorTab.value === 'number') {
    targetColorId = activeColorTab.value
  } else if (activeColorTab.value === null && selectedColorIds.value.length > 0) {
    targetColorId = selectedColorIds.value[0]
    activeColorTab.value = targetColorId
  }

  const newItems = validFiles.map((f) => ({
    id: `pending-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    file: f,
    previewUrl: URL.createObjectURL(f),
    colorId: targetColorId,
    isNew: true
  }))

  imageItems.value.push(...newItems)
  ensureValidPrimaryImage()
  activePreviewImage.value = newItems[0].previewUrl
  imgError.value = false

  const colorName = targetColorId ? colors.value.find((c) => c.id === targetColorId)?.colorName : null
  ElMessage.success(
    colorName
      ? `Đã thêm ${newItems.length} ảnh vào màu "${colorName}" (upload MinIO khi ấn Lưu)`
      : `Đã thêm ${newItems.length} ảnh xem trước (upload MinIO khi ấn Lưu)`
  )
}

function setAsPrimaryById(id) {
  const target = imageItems.value.find((i) => i.id === id)
  if (!target) return
  primaryImageId.value = id
  activePreviewImage.value = target.previewUrl
  ElMessage.success('Đã đặt làm ảnh bìa chính của sản phẩm')
}

function moveImageInActiveColor(id, delta) {
  const list = visibleImages.value
  const localIdx = list.findIndex((i) => i.id === id)
  const swapIdx = localIdx + delta
  if (localIdx < 0 || swapIdx < 0 || swapIdx >= list.length) return
  const globalA = imageItems.value.findIndex((i) => i.id === list[localIdx].id)
  const globalB = imageItems.value.findIndex((i) => i.id === list[swapIdx].id)
  if (globalA < 0 || globalB < 0) return
  const arr = [...imageItems.value]
  const tmp = arr[globalA]
  arr[globalA] = arr[globalB]
  arr[globalB] = tmp
  imageItems.value = arr
}

function reassignImageColor(item, rawVal) {
  const newColorId = rawVal ? Number(rawVal) : null
  item.colorId = newColorId
  if (newColorId && !selectedColorIds.value.includes(newColorId)) {
    selectedColorIds.value.push(newColorId)
  }
  if (activeColorTab.value === 'unassigned' && unassignedImagesCount.value === 0 && selectedColorIds.value.length > 0) {
    switchColorTab(newColorId || selectedColorIds.value[0])
  }
}

function removeImageById(id) {
  const idx = imageItems.value.findIndex((i) => i.id === id)
  if (idx < 0) return
  const removed = imageItems.value.splice(idx, 1)[0]
  if (removed.isNew && removed.previewUrl.startsWith('blob:')) {
    URL.revokeObjectURL(removed.previewUrl)
  }
  ensureValidPrimaryImage()
  if (activePreviewImage.value === removed.previewUrl) {
    activePreviewImage.value = visibleImages.value[0]?.previewUrl || imageItems.value[0]?.previewUrl || ''
  }
  if (activeColorTab.value === 'unassigned' && unassignedImagesCount.value === 0 && selectedColorIds.value.length > 0) {
    switchColorTab(selectedColorIds.value[0])
  }
}

function clearActiveColorImages() {
  if (!activeColorObj.value) return
  const cid = activeColorObj.value.id
  imageItems.value.forEach((item) => {
    if (Number(item.colorId) === cid && item.isNew && item.previewUrl.startsWith('blob:')) {
      URL.revokeObjectURL(item.previewUrl)
    }
  })
  imageItems.value = imageItems.value.filter((i) => Number(i.colorId) !== cid)
  ensureValidPrimaryImage()
  activePreviewImage.value = ''
}

function clearAllImages() {
  imageItems.value.forEach((item) => {
    if (item.isNew && item.previewUrl.startsWith('blob:')) {
      URL.revokeObjectURL(item.previewUrl)
    }
  })
  imageItems.value = []
  primaryImageId.value = null
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

function getPresetColorIdsForVariants() {
  const fromImages = []
  for (const cid of selectedColorIds.value) {
    if (countImagesByColor(cid) > 0 && !fromImages.includes(cid)) fromImages.push(cid)
  }
  return fromImages.length > 0 ? fromImages : [...selectedColorIds.value]
}

function goToVariantStudio() {
  const preset = getPresetColorIdsForVariants()
  const query = { productId: productId.value }
  if (preset.length > 0) query.presetColors = preset.join(',')

  if (existingVariants.value.length > 0 && existingVariants.value[0]?.id) {
    router.push({ path: `/admin/variants/${existingVariants.value[0].id}/edit`, query })
  } else {
    router.push({ path: '/admin/variants/create', query })
  }
}

async function loadInitialData() {
  loadingPage.value = true
  try {
    const [bRes, cRes, colRes] = await Promise.all([
      brandService.getAllBrands(),
      categoryService.getAllCategories(),
      variantService.getColors()
    ])
    brands.value = bRes.data || []
    categories.value = cRes.data || []
    colors.value = colRes.data || []

    if (isEdit.value) {
      const pRes = await productService.getProductById(productId.value)
      const p = pRes.data
      form.productCode = p.productCode || ''
      form.productName = p.productName || ''
      form.brandId = p.brandId || null
      form.categoryId = p.categoryId || null
      form.material = p.material || ''
      form.description = p.description || ''

      if (p.images?.length) {
        imageItems.value = p.images.map((img, i) => ({
          id: `existing-${img.id || i}-${Date.now()}`,
          file: null,
          previewUrl: img.imageUrl,
          colorId: img.colorId ? Number(img.colorId) : null,
          isPrimary: img.isPrimary === 1,
          isNew: false
        }))
      } else {
        const rawImages = p.imageUrls?.length ? p.imageUrls : (p.thumbnailUrl ? [p.thumbnailUrl] : [])
        imageItems.value = rawImages.map((url, i) => ({
          id: `existing-${i}-${Date.now()}`,
          file: null,
          previewUrl: url,
          colorId: null,
          isPrimary: i === 0,
          isNew: false
        }))
      }

      form.importPrice = Number(p.importPrice) || 0
      form.salePrice = Number(p.salePrice) || 0
      form.status = p.status || 'ACTIVE'
      existingVariants.value = p.variants || []

      // Build selectedColorIds from existing variants + existing tagged images
      const colorSet = []
      for (const v of existingVariants.value) {
        const cid = Number(v.colorId)
        if (cid && !colorSet.includes(cid)) colorSet.push(cid)
      }
      for (const img of imageItems.value) {
        const cid = Number(img.colorId)
        if (cid && !colorSet.includes(cid)) colorSet.push(cid)
      }
      selectedColorIds.value = colorSet

      const primaryItem = imageItems.value.find((i) => i.isPrimary) || imageItems.value[0] || null
      primaryImageId.value = primaryItem?.id || null

      if (primaryItem?.colorId && colorSet.includes(primaryItem.colorId)) {
        switchColorTab(primaryItem.colorId)
      } else if (colorSet.length > 0) {
        switchColorTab(colorSet[0])
      } else if (imageItems.value.some((i) => !i.colorId)) {
        switchColorTab('unassigned')
      } else if (colors.value.length > 0) {
        selectedColorIds.value = [colors.value[0].id]
        switchColorTab(colors.value[0].id)
      }
    } else {
      autoGenerateCode()
      if (brands.value.length) form.brandId = brands.value[0].id
      if (categories.value.length) form.categoryId = categories.value[0].id
      // Pre-select first color so admin can drop images right away with a valid colorId
      if (colors.value.length > 0) {
        selectedColorIds.value = [colors.value[0].id]
        activeColorTab.value = colors.value[0].id
      }
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

      // Order images cleanly: primary cover FIRST, then grouped by selectedColorIds preserving per-color order
      const validItems = imageItems.value.filter((i) => !!i.previewUrl)
      const orderedItems = []
      const pushedIds = new Set()
      const primaryItem = validItems.find((i) => i.id === primaryImageId.value) || validItems[0] || null
      if (primaryItem) {
        orderedItems.push(primaryItem)
        pushedIds.add(primaryItem.id)
        // Keep the rest of the primary color together right after primaryItem
        if (primaryItem.colorId) {
          for (const it of validItems) {
            if (!pushedIds.has(it.id) && Number(it.colorId) === Number(primaryItem.colorId)) {
              orderedItems.push(it)
              pushedIds.add(it.id)
            }
          }
        }
      }
      for (const cid of selectedColorIds.value) {
        for (const it of validItems) {
          if (!pushedIds.has(it.id) && Number(it.colorId) === Number(cid)) {
            orderedItems.push(it)
            pushedIds.add(it.id)
          }
        }
      }
      for (const it of validItems) {
        if (!pushedIds.has(it.id)) {
          orderedItems.push(it)
          pushedIds.add(it.id)
        }
      }

      const finalImageUrls = orderedItems.map((i) => i.previewUrl)
      const structuredImages = orderedItems.map((i, idx) => ({
        imageUrl: i.previewUrl,
        colorId: i.colorId || null,
        isPrimary: idx === 0 ? 1 : 0,
        displayOrder: idx + 1
      }))
      const payload = {
        ...form,
        thumbnailUrl: finalImageUrls[0] || '',
        imageUrls: finalImageUrls,
        images: structuredImages
      }

      if (isEdit.value) {
        await productService.updateProduct(productId.value, payload)
        ElMessage.success('Cập nhật sản phẩm Sneaker thành công!')
        router.push(`/admin/products/${productId.value}`)
      } else {
        const res = await productService.createProduct(payload)
        const createdId = res.data?.id
        const presetColors = getPresetColorIdsForVariants()
        ElMessage.success('Khởi tạo sản phẩm Sneaker mới thành công!')

        if (createdId && presetColors.length > 0) {
          try {
            await ElMessageBox.confirm(
              `Đã lưu sản phẩm cùng ${presetColors.length} màu đã chọn. Chuyển sang Ma trận SKU ngay để sinh Size cho đúng ${presetColors.length} màu này?`,
              'Tạo biến thể Size × Màu ngay',
              {
                type: 'success',
                confirmButtonText: `Tạo SKU cho ${presetColors.length} màu →`,
                cancelButtonText: 'Xem chi tiết SP'
              }
            )
            router.push({
              path: '/admin/variants/create',
              query: { productId: createdId, presetColors: presetColors.join(',') }
            })
            return
          } catch (_) {
            router.push(`/admin/products/${createdId}`)
            return
          }
        }
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
  border: 1px solid var(--vea-theme, #409eff);
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
  color: #303133;
  font-size: 11.5px;
  font-weight: 800;
  padding: 6px 10px;
  cursor: pointer;

  &:hover {
    border-color: var(--vea-theme, #409eff);
    background: var(--vea-theme, #409eff);
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
    background: var(--vea-theme, #409eff);
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
    color: #303133;
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

    strong { color: #303133; }

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
  color: #303133;
  font-size: 11.5px;
  font-weight: 700;
  padding: 6px 10px;
  cursor: pointer;

  &:hover {
    border-color: var(--vea-theme, #409eff);
  }
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #fafafa;
  border: 1px solid var(--vea-theme, #409eff);
  color: #303133;
  font-size: 11.5px;
  font-weight: 800;
  padding: 6px 10px;
  cursor: pointer;

  &:hover {
    background: var(--vea-theme, #409eff);
    color: #ffffff;
  }
}

.btn-save-primary {
  font-weight: 800;
  letter-spacing: 0.04em;
}

/* 2. STUDIO GRID — 50/50 SPACIOUS PHOTO STUDIO + COMPACT FORM */
.studio-grid {
  display: grid;
  grid-template-columns: minmax(480px, 50%) 1fr;
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
  background: var(--vea-theme, #409eff);
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

  .media-summary-pill {
    font-size: 10.5px;
    font-weight: 800;
    background: #f1f5f9;
    border: 1px solid #cbd5e1;
    color: #334155;
    padding: 2px 7px;
    border-radius: 2px;
    font-variant-numeric: tabular-nums;
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
  height: 270px;
  background: #f3f5f7;
  border: 1px solid #e5e5e5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  .preview-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    background: #f3f5f7;
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

  .preview-color-pill {
    position: absolute;
    left: 8px;
    bottom: 8px;
    display: inline-flex;
    align-items: center;
    gap: 5px;
    background: rgba(17, 17, 17, 0.84);
    color: #ffffff;
    font-size: 10.5px;
    font-weight: 700;
    padding: 3px 8px;
    border-radius: 2px;
  }
}

.swatch-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.22);
  flex-shrink: 0;

  &.lg {
    width: 13px;
    height: 13px;
  }
}

.color-studio-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .cs-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    flex-wrap: wrap;

    .cs-title {
      font-size: 10px;
      font-weight: 900;
      letter-spacing: 0.05em;
      color: #303133;
    }

    .cs-head-actions {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .cs-tab-filter {
      width: 120px;
      height: 25px;
      padding: 0 7px;
      font-size: 11px;
      border: 1px solid #cbd5e1;
      border-radius: 3px;
      background: #ffffff;
      outline: none;

      &:focus {
        border-color: var(--vea-theme, #409eff);
      }
    }

    .btn-add-color {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      height: 26px;
      padding: 0 10px;
      font-size: 11px;
      font-weight: 800;
      color: #ffffff;
      background: var(--vea-theme, #409eff);
      border: 1px solid var(--vea-theme, #409eff);
      border-radius: 3px;
      cursor: pointer;

      .btn-color-total {
        font-weight: 600;
        opacity: 0.85;
      }

      &:hover {
        filter: brightness(0.94);
      }
    }
  }

  .cs-no-color-hint {
    font-size: 11px;
    color: #64748b;
    padding: 4px 2px;
  }
}

.cp-pop {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .cp-search-row {
    position: relative;
    display: flex;
    align-items: center;
  }

  .cp-search-input {
    width: 100%;
    height: 32px;
    padding: 0 26px 0 9px;
    font-size: 12px;
    border: 1px solid #cbd5e1;
    border-radius: 4px;
    outline: none;

    &:focus {
      border-color: var(--vea-theme, #409eff);
    }
  }

  .cp-clear-btn {
    position: absolute;
    right: 6px;
    background: none;
    border: none;
    font-size: 15px;
    color: #94a3b8;
    cursor: pointer;

    &:hover { color: #334155; }
  }

  .cp-list {
    max-height: 240px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .cp-empty {
    padding: 16px 8px;
    text-align: center;
    font-size: 12px;
    color: #94a3b8;
  }

  .cp-item {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 6px 8px;
    background: #ffffff;
    border: 1px solid transparent;
    border-radius: 4px;
    text-align: left;
    cursor: pointer;
    font-size: 12px;
    color: #1e293b;

    &:hover {
      background: #f1f5f9;
    }

    &.selected {
      background: #eff6ff;
      border-color: #bfdbfe;
    }

    .cp-name {
      flex: 1;
      font-weight: 700;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .cp-code {
      font-size: 10.5px;
      font-family: var(--font-mono);
      color: #64748b;
    }

    .cp-picked {
      font-size: 10.5px;
      font-weight: 800;
      color: #059669;
    }

    .cp-add-tag {
      font-size: 10.5px;
      font-weight: 700;
      color: var(--vea-theme, #409eff);
    }
  }

  .cp-foot {
    border-top: 1px solid #e2e8f0;
    padding-top: 6px;
    font-size: 10.5px;
    color: #64748b;
  }
}

.color-tabs-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  max-height: 92px;
  overflow-y: auto;

  .ctab {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 4px 8px;
    font-size: 11px;
    font-weight: 700;
    color: #334155;
    background: #ffffff;
    border: 1px solid #cbd5e1;
    border-radius: 3px;
    cursor: pointer;

    &.missing:not(.active) {
      border-color: #f59e0b;
      background: #fffbeb;
    }

    &.active {
      background: #111111;
      border-color: #111111;
      color: #ffffff;

      .ctab-close { color: #e2e8f0; }
      .ctab-nosku { background: rgba(255, 255, 255, 0.16); color: #e2e8f0; }
    }

    &.unassigned {
      border-style: dashed;
    }

    .ctab-badge {
      font-size: 9.5px;
      font-weight: 900;
      padding: 0 4px;
      border-radius: 2px;

      &.ok { background: #10b981; color: #ffffff; }
      &.warn { background: #f59e0b; color: #ffffff; }
      &.zero { background: #ef4444; color: #ffffff; }
    }

    .ctab-nosku {
      font-size: 9px;
      font-weight: 700;
      padding: 0 4px;
      border-radius: 2px;
      background: #e2e8f0;
      color: #475569;
    }

    .ctab-close {
      background: none;
      border: none;
      color: #64748b;
      font-size: 13px;
      font-weight: 800;
      line-height: 1;
      padding: 0 1px;
      cursor: pointer;

      &:hover { color: #dc2626; }
    }
  }
}

.uz-color-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 1px 6px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  border-radius: 2px;
  margin-left: 2px;
}

.gallery-section-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: #525252;

  strong { color: #111111; }
}

.color-empty-box {
  background: #fafafa;
  border: 1px dashed #d4d4d4;
  padding: 12px 10px;
  text-align: center;
  font-size: 11px;
  color: #737373;
  line-height: 1.45;
}

.g-badge-swatch {
  position: absolute;
  left: 2px;
  top: 2px;
  font-size: 8px;
  font-weight: 900;
  padding: 1px 4px;
  background: #1d4ed8;
  color: #ffffff;
}

.live-card-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: #fafafa;
  border: 1px solid #e5e5e5;
  padding: 6px 10px;

  .lc-left {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .lc-meta {
    font-size: 10.5px;
    font-weight: 700;
    text-transform: uppercase;
    color: #737373;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .lc-code {
    font-size: 10.5px;
    font-family: var(--font-mono);
    font-weight: 700;
    color: #525252;
    background: #efefef;
    padding: 1px 5px;
    border-radius: 2px;
  }

  .lc-price {
    font-size: 14px;
    font-weight: 900;
    color: #303133;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
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
      color: #303133;
    }
    span {
      font-size: 10.5px;
      color: #737373;
    }
  }

  &:hover,
  &.dragover {
    border-color: var(--vea-theme, #409eff);
    background: #f5f5f5;
  }
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.g-tile-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.g-color-select {
  width: 100%;
  font-size: 10.5px;
  font-weight: 700;
  padding: 3px 4px;
  border: 1px solid #d4d4d4;
  border-radius: 4px;
  background: #ffffff;
  color: #262626;
  outline: none;
  cursor: pointer;

  &:focus {
    border-color: var(--vea-theme, #409eff);
  }
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
    border-color: var(--vea-theme, #409eff);
  }

  &.viewing {
    outline: 2px solid var(--vea-theme, #409eff);
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
      color: #303133;
      font-size: 11px;
      font-weight: 800;
      line-height: 1;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;

      &:hover {
        background: var(--vea-theme, #409eff);
        color: #ffffff;
      }

      &.del:hover {
        background: #dc2626;
        color: #ffffff;
      }
    }
  }
}

/* RIGHT FORM CARDS — COMPACT TO GIVE PHOTO STUDIO MORE ROOM */
.fields-column {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-card {
  padding: 11px 13px;

  .card-head-row {
    margin-bottom: 9px;
  }

  :deep(.el-form-item) {
    margin-bottom: 9px;
  }

  :deep(.el-form-item__label) {
    font-size: 10.5px;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #404040;
    padding-bottom: 3px;
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
    background: var(--vea-theme, #409eff);
    color: #ffffff;
    border: 1px solid var(--vea-theme, #409eff);
    padding: 0 10px;
    font-size: 11px;
    font-weight: 800;
    cursor: pointer;
    white-space: nowrap;

    &:hover {
      background: var(--vea-theme-light-3, #66b1ff);
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
    color: #303133;
    font-size: 10.5px;
    font-weight: 800;
    padding: 2px 7px;
    cursor: pointer;

    &:hover {
      border-color: var(--vea-theme, #409eff);
      background: var(--vea-theme, #409eff);
      color: #ffffff;
    }
  }
}

.pricing-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 0.92fr;
  gap: 8px;
  align-items: start;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.profit-box {
  border: 1px solid #d4d4d4;
  padding: 6px 10px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-height: 52px;
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
      border-color: var(--vea-theme, #409eff);
      background: var(--vea-theme, #409eff);
      color: #ffffff;
    }
  }
}

/* CARD 4 MINI VARIANT MATRIX */
.btn-manage-sku {
  background: var(--vea-theme, #409eff);
  color: #ffffff;
  border: 1px solid var(--vea-theme, #409eff);
  font-size: 11px;
  font-weight: 800;
  padding: 5px 11px;
  cursor: pointer;

  &:hover {
    background: var(--vea-theme-light-3, #66b1ff);
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
    border: 1px solid var(--vea-theme, #409eff);
    color: #303133;
    font-size: 11px;
    font-weight: 800;
    padding: 5px 10px;
    cursor: pointer;

    &:hover {
      background: var(--vea-theme, #409eff);
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
    background: var(--vea-theme, #409eff);
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
    background: var(--vea-theme, #409eff);
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
    color: #303133;
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
      background: var(--vea-theme, #409eff);
      border-color: var(--vea-theme, #409eff);
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

@media (max-width: 1120px) {
  .studio-grid {
    grid-template-columns: 1fr;
  }
  .media-column {
    position: static;
  }
}

@media (max-width: 768px) {
  .gallery-grid {
    grid-template-columns: repeat(2, 1fr);
  }
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
