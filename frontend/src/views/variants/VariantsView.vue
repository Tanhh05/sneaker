<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">Quản lý Biến thể Sản phẩm (SKU) &amp; Size / Màu sắc</h2>
        <p class="page-subtitle">Quản lý chi tiết từng SKU theo Size và Màu sắc (Ví dụ: AF1-39-W, AF1-40-W, AF1-41-B)</p>
      </div>
      <div v-if="canManageCatalog" style="display: flex; gap: 8px;">
        <el-button v-if="activeTab === 'variants'" type="success" plain @click="printAllCurrentVariants">
          <el-icon style="margin-right: 6px"><Printer /></el-icon> In tem nhãn mã vạch
        </el-button>
        <el-button v-if="activeTab === 'variants'" type="primary" @click="openCreateVariant">
          <el-icon style="margin-right: 6px"><Plus /></el-icon> Thêm biến thể SKU
        </el-button>
        <el-button v-else-if="activeTab === 'sizes'" type="primary" @click="sizeTabRef?.openCreateSize()">
          <el-icon style="margin-right: 6px"><Plus /></el-icon> Thêm Size giày
        </el-button>
        <el-button v-else type="primary" @click="colorTabRef?.openCreateColor()">
          <el-icon style="margin-right: 6px"><Plus /></el-icon> Thêm Màu sắc
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <el-tabs v-model="activeTab">
        <!-- TAB 1: PRODUCT VARIANTS (SKU) -->
        <el-tab-pane label="Danh sách Biến thể (SKU)" name="variants">
          <el-row :gutter="12" style="margin-bottom: 16px">
            <el-col :xs="24" :sm="8" :md="6">
              <el-input
                v-model="vFilters.keyword"
                placeholder="Tìm theo SKU hoặc tên giày (tự động lọc 350ms)..."
                clearable
                @keyup.enter="loadVariants"
              />
            </el-col>
            <el-col :xs="12" :sm="6" :md="6">
              <QuickSelect
                v-model="vFilters.productId"
                :options="products"
                value-key="id"
                :label-key="(p) => `${p.productCode} - ${p.productName}`"
                :search-keys="['productCode', 'productName']"
                placeholder="Lọc theo sản phẩm"
                search-placeholder="Tìm nhanh mã SP, tên giày..."
                clearable
                @change="loadVariants"
              />
            </el-col>
            <el-col :xs="12" :sm="5" :md="4">
              <QuickSelect
                v-model="vFilters.sizeId"
                :options="sizes"
                value-key="id"
                :label-key="(s) => `Size ${s.sizeValue}`"
                placeholder="Size EU"
                search-placeholder="Tìm size..."
                clearable
                @change="loadVariants"
              />
            </el-col>
            <el-col :xs="12" :sm="5" :md="4">
              <QuickSelect
                v-model="vFilters.colorId"
                :options="colors"
                value-key="id"
                label-key="colorName"
                :search-keys="['colorName', 'colorCode']"
                placeholder="Màu sắc"
                search-placeholder="Tìm nhanh màu sắc..."
                clearable
                @change="loadVariants"
              >
                <template #option="{ item }">
                  <span class="color-dot" :style="{ backgroundColor: item.hexCode }"></span>
                  <span>{{ item.colorName }}</span>
                </template>
              </QuickSelect>
            </el-col>
            <el-col :xs="12" :sm="24" :md="4">
              <el-button type="primary" @click="loadVariants">Tìm SKU</el-button>
              <el-button @click="resetVariantFilter">Làm mới</el-button>
            </el-col>
          </el-row>

          <el-table :data="variants" v-loading="loading" stripe border style="width: 100%">
            <template #empty>
              <el-empty description="Không tìm thấy biến thể SKU phù hợp" :image-size="84" />
            </template>
            <el-table-column prop="id" label="ID" width="65" />
            <el-table-column label="Mã SKU" width="165">
              <template #default="{ row }">
                <span class="code-badge">{{ row.sku }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="Sản phẩm Sneaker" min-width="200" />
            <el-table-column prop="brandName" label="Hãng" width="120" />
            <el-table-column prop="sizeValue" label="Size EU" width="90" align="center">
              <template #default="{ row }">
                <el-tag effect="plain" type="warning"><strong>{{ row.sizeValue }}</strong></el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Màu sắc" width="160">
              <template #default="{ row }">
                <span class="color-dot" :style="{ backgroundColor: row.hexCode }"></span>
                {{ row.colorName }}
              </template>
            </el-table-column>
            <el-table-column label="Giá nhập" width="135" align="right">
              <template #default="{ row }">{{ formatCurrency(row.importPrice) }}</template>
            </el-table-column>
            <el-table-column label="Giá bán" width="140" align="right">
              <template #default="{ row }">
                <span class="price-text">{{ formatCurrency(row.salePrice) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="availableQuantity" label="SL sẵn có" width="105" align="center" />
            <el-table-column label="Trạng thái" width="115" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="canManageCatalog" label="Thao tác" width="95" fixed="right" align="center">
              <template #default="{ row }">
                <div class="row-actions">
                  <button class="action-btn" title="In tem mã vạch dán hộp giày" @click="printSingleVariant(row)">
                    <el-icon><Printer /></el-icon>
                  </button>
                  <button class="action-btn" title="Chỉnh sửa" @click="openEditVariant(row)">
                    <el-icon><Edit /></el-icon>
                  </button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="vPagination.page"
              v-model:page-size="vPagination.size"
              :total="vPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadVariants"
              @current-change="loadVariants"
            />
          </div>
        </el-tab-pane>

        <!-- TAB 2: SIZES (36 - 45) -->
        <el-tab-pane label="Quản lý Size giày (36 - 45)" name="sizes">
          <SizeManagerTab
            ref="sizeTabRef"
            :sizes="sizes"
            :can-manage-catalog="canManageCatalog"
            @saved="loadMeta"
          />
        </el-tab-pane>

        <!-- TAB 3: COLORS (HEX) -->
        <el-tab-pane label="Quản lý Màu sắc (HEX)" name="colors">
          <ColorManagerTab
            ref="colorTabRef"
            :colors="colors"
            :can-manage-catalog="canManageCatalog"
            @saved="loadMeta"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- BARCODE PRINT MODAL -->
    <BarcodeLabelPrintModal
      v-model="printBarcodeVisible"
      :items="printBarcodeItems"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Edit, Printer } from '@element-plus/icons-vue'
import { variantService } from '@/services/variantService'
import { productService } from '@/services/productService'
import { formatCurrency } from '@/utils/formatters'
import { usePermission } from '@/composables/usePermission'
import { useDebouncedWatch } from '@/composables/useDebounce'
import SizeManagerTab from './components/SizeManagerTab.vue'
import ColorManagerTab from './components/ColorManagerTab.vue'
import BarcodeLabelPrintModal from '@/components/common/BarcodeLabelPrintModal.vue'

const router = useRouter()
const { canManageCatalog } = usePermission()
const activeTab = ref('variants')
const loading = ref(false)

const printBarcodeVisible = ref(false)
const printBarcodeItems = ref([])

function printSingleVariant(row) {
  printBarcodeItems.value = [{
    sku: row.sku,
    productName: row.productName,
    sizeValue: row.sizeValue,
    colorName: row.colorName,
    salePrice: row.salePrice,
    brandName: row.brandName || 'ADHA SNEAKER',
    printQuantity: row.availableQuantity > 0 ? Math.min(row.availableQuantity, 5) : 1
  }]
  printBarcodeVisible.value = true
}

function printAllCurrentVariants() {
  if (!variants.value || variants.value.length === 0) {
    ElMessage.info('Không có biến thể nào để in tem')
    return
  }
  printBarcodeItems.value = variants.value.map(row => ({
    sku: row.sku,
    productName: row.productName,
    sizeValue: row.sizeValue,
    colorName: row.colorName,
    salePrice: row.salePrice,
    brandName: row.brandName || 'ADHA SNEAKER',
    printQuantity: 1
  }))
  printBarcodeVisible.value = true
}

const sizeTabRef = ref(null)
const colorTabRef = ref(null)

const products = ref([])
const sizes = ref([])
const colors = ref([])
const variants = ref([])

const vFilters = reactive({
  keyword: '',
  productId: null,
  sizeId: null,
  colorId: null
})
const vPagination = reactive({ page: 1, size: 10, total: 0 })

useDebouncedWatch(
  () => vFilters.keyword,
  () => {
    vPagination.page = 1
    loadVariants()
  },
  350
)

async function loadMeta() {
  const [pRes, sRes, cRes] = await Promise.all([
    productService.getAllProducts(),
    variantService.getSizes(),
    variantService.getColors()
  ])
  products.value = pRes.data || []
  sizes.value = sRes.data || []
  colors.value = cRes.data || []
}

async function loadVariants() {
  loading.value = true
  try {
    const res = await variantService.getVariants({
      keyword: vFilters.keyword,
      productId: vFilters.productId || undefined,
      sizeId: vFilters.sizeId || undefined,
      colorId: vFilters.colorId || undefined,
      page: vPagination.page - 1,
      size: vPagination.size
    })
    variants.value = res.data.content || []
    vPagination.total = res.data.totalElements || 0
  } finally {
    loading.value = false
  }
}

function resetVariantFilter() {
  vFilters.keyword = ''
  vFilters.productId = null
  vFilters.sizeId = null
  vFilters.colorId = null
  vPagination.page = 1
  loadVariants()
}

function openCreateVariant() {
  const query = vFilters.productId ? { productId: vFilters.productId } : {}
  router.push({ path: '/admin/variants/create', query })
}

function openEditVariant(row) {
  router.push(`/admin/variants/${row.id}/edit`)
}

onMounted(() => {
  loadMeta()
  loadVariants()
})
</script>
