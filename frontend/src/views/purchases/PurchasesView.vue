<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">Quản lý Phiếu Nhập Hàng</h2>
        <p class="page-subtitle">Quản lý chứng từ và lịch sử nhập hàng từ Nhà cung cấp theo từng biến thể SKU</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon style="margin-right: 6px"><Plus /></el-icon> Tạo phiếu nhập hàng
      </el-button>
    </div>

    <!-- Purchase Summary Banner -->
    <div class="summary-strip">
      <div class="strip-item">
        <span class="label">Tổng số phiếu hiển thị:</span>
        <strong>{{ pagination.total }} phiếu</strong>
      </div>
      <div class="strip-item">
        <span class="label">Giá trị phiếu đã xác nhận (trang này):</span>
        <strong class="price-text">{{ formatCurrency(confirmedPageSum) }}</strong>
      </div>
    </div>

    <!-- Filter -->
    <div class="filter-card">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="8" :md="7">
          <el-input
            v-model="filters.keyword"
            placeholder="Tìm theo mã phiếu nhập hoặc tên NCC..."
            clearable
            @keyup.enter="loadData"
          />
        </el-col>
        <el-col :xs="12" :sm="6" :md="6">
          <QuickSelect
            v-model="filters.supplierId"
            :options="suppliers"
            value-key="id"
            label-key="supplierName"
            :search-keys="['supplierCode', 'supplierName']"
            placeholder="Nhà cung cấp"
            search-placeholder="Tìm nhanh nhà cung cấp..."
            clearable
            @change="loadData"
          />
        </el-col>
        <el-col :xs="12" :sm="5" :md="5">
          <el-select v-model="filters.status" placeholder="Trạng thái phiếu" filterable clearable style="width: 100%" @change="loadData">
            <el-option label="Nháp (DRAFT)" value="DRAFT" />
            <el-option label="Đã xác nhận (CONFIRMED)" value="CONFIRMED" />
            <el-option label="Đã hủy (CANCELLED)" value="CANCELLED" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="5" :md="6">
          <el-button type="primary" @click="loadData">Lọc phiếu</el-button>
          <el-button @click="resetFilter">Làm mới</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- Table -->
    <div class="table-card" v-loading="loading">
      <el-table :data="items" stripe border style="width: 100%">
        <template #empty>
          <el-empty description="Không tìm thấy phiếu nhập phù hợp" :image-size="84" />
        </template>
        <el-table-column label="Mã phiếu nhập" width="135">
          <template #default="{ row }">
            <span class="code-badge">{{ row.purchaseCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="supplierName" label="Nhà cung cấp" min-width="230" />
        <el-table-column prop="employeeName" label="Nhân viên lập" width="160" />
        <el-table-column label="Ngày nhập" width="160">
          <template #default="{ row }">{{ formatDateTime(row.purchaseDate) }}</template>
        </el-table-column>
        <el-table-column label="Tổng tiền nhập" width="165" align="right">
          <template #default="{ row }">
            <span class="price-text">{{ formatCurrency(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Trạng thái" width="155" align="center">
          <template #default="{ row }">
            <el-tag :type="PURCHASE_STATUS_MAP[row.status]?.type || 'info'">
              {{ PURCHASE_STATUS_MAP[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="Ghi chú" min-width="180" show-overflow-tooltip />
        <el-table-column label="Thao tác" width="215" fixed="right" align="center" class-name="small-padding fixed-width">
          <template #default="{ row }">
            <div class="vea-table-actions">
              <el-button
                v-if="row.status === 'DRAFT'"
                type="success"
                size="small"
                class="vea-cta-btn"
                @click="handleConfirm(row)"
              >
                <el-icon><Check /></el-icon>
                <span>Duyệt</span>
              </el-button>
              <el-tooltip content="Xem chi tiết phiếu nhập" placement="top" :show-after="200">
                <el-button size="small" class="vea-icon-btn vea-icon-info" aria-label="Xem chi tiết" @click="openDetail(row)">
                  <el-icon><View /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip v-if="row.status === 'DRAFT'" content="Chỉnh sửa phiếu nháp" placement="top" :show-after="200">
                <el-button size="small" class="vea-icon-btn vea-icon-primary" aria-label="Sửa phiếu nháp" @click="openEdit(row)">
                  <el-icon><Edit /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip v-if="row.status === 'DRAFT'" content="Hủy phiếu nhập nháp" placement="top" :show-after="200">
                <el-button size="small" class="vea-icon-btn vea-icon-warning" aria-label="Hủy phiếu nhập" @click="handleCancel(row)">
                  <el-icon><Close /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip v-if="authStore.isAdmin" content="Xóa mềm phiếu nhập" placement="top" :show-after="200">
                <el-button
                  size="small"
                  class="vea-icon-btn vea-icon-danger"
                  aria-label="Xóa mềm"
                  @click="handleSoftDelete(row)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- Modal Create / Update Draft Purchase Order -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? `Cập nhật Phiếu nhập nháp (${form.purchaseCode})` : 'Tạo Phiếu Nhập Hàng Mới'"
      width="860px"
    >
      <el-form label-position="top">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="Chọn Nhà cung cấp (*)">
              <QuickSelect
                v-model="form.supplierId"
                :options="suppliers"
                value-key="id"
                :label-key="(s) => `${s.supplierCode} - ${s.supplierName}`"
                :search-keys="['supplierCode', 'supplierName', 'phone']"
                placeholder="Chọn nhà cung cấp"
                search-placeholder="Tìm nhanh mã hoặc tên NCC..."
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Ghi chú chứng từ nhập">
              <el-input v-model="form.note" placeholder="VD: Nhập lô hàng Nike & Adidas đầu quý..." />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="items-header">
          <h4>Danh sách sản phẩm &amp; biến thể nhập hàng</h4>
          <el-button type="primary" plain size="small" @click="addLineItem">
            + Thêm dòng sản phẩm SKU
          </el-button>
        </div>

        <el-table :data="form.items" border size="small" style="width: 100%">
          <el-table-column label="Biến thể Sản phẩm (SKU - Tên giày - Size - Màu)" min-width="320">
            <template #default="{ row }">
              <QuickSelect
                v-model="row.productVariantId"
                :options="variants"
                value-key="id"
                :label-key="(v) => `${v.sku} | ${v.productName} (Size ${v.sizeValue} - ${v.colorName})`"
                :search-keys="['sku', 'productName', 'sizeValue', 'colorName']"
                placeholder="Chọn biến thể SKU..."
                search-placeholder="Tìm nhanh SKU, tên giày, size, màu..."
                @change="(val) => onVariantSelect(row, val)"
              />
            </template>
          </el-table-column>

          <el-table-column label="Số lượng nhập" width="135" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" size="small" style="width: 110px" />
            </template>
          </el-table-column>

          <el-table-column label="Đơn giá nhập (VNĐ)" width="175" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0" :step="50000" size="small" style="width: 150px" />
            </template>
          </el-table-column>

          <el-table-column label="Thành tiền" width="150" align="right">
            <template #default="{ row }">
              <strong class="price-text">{{ formatCurrency((row.quantity || 0) * (row.unitPrice || 0)) }}</strong>
            </template>
          </el-table-column>

          <el-table-column label="" width="56" align="center">
            <template #default="{ $index }">
              <el-tooltip content="Xóa dòng" placement="top" :show-after="200">
                <el-button
                  size="small"
                  class="vea-icon-btn vea-icon-danger"
                  aria-label="Xóa dòng"
                  :disabled="form.items.length <= 1"
                  @click="form.items.splice($index, 1)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>

        <div class="total-bar">
          <span>Tổng cộng giá trị phiếu nhập:</span>
          <strong class="price-text" style="font-size: 18px">{{ formatCurrency(computedFormTotal) }}</strong>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Đóng</el-button>
        <el-button type="info" :loading="saving" @click="submitForm(false)">
          {{ isEdit ? 'Lưu cập nhật nháp' : 'Lưu nháp (DRAFT)' }}
        </el-button>
        <el-button v-if="!isEdit" type="primary" :loading="saving" @click="submitForm(true)">
          Tạo &amp; Xác nhận nhập ngay
        </el-button>
      </template>
    </el-dialog>

    <!-- Detail Dialog -->
    <PurchaseDetailDialog v-model="detailVisible" :purchase="selectedPurchase" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View, Edit, Check, Close, Delete } from '@element-plus/icons-vue'
import { purchaseService } from '@/services/purchaseService'
import { supplierService } from '@/services/supplierService'
import { variantService } from '@/services/variantService'
import { softDeleteService } from '@/services/softDeleteService'
import { useAuthStore } from '@/stores/auth'
import { formatCurrency, formatDateTime } from '@/utils/formatters'
import { PURCHASE_STATUS_MAP } from '@/constants'
import { useDebouncedWatch } from '@/composables/useDebounce'
import PurchaseDetailDialog from './components/PurchaseDetailDialog.vue'

const authStore = useAuthStore()

const loading = ref(false)
const saving = ref(false)
const items = ref([])
const suppliers = ref([])
const variants = ref([])

const filters = reactive({
  keyword: '',
  supplierId: null,
  status: ''
})

const pagination = reactive({ page: 1, size: 10, total: 0 })

useDebouncedWatch(
  () => filters.keyword,
  () => {
    pagination.page = 1
    loadData()
  },
  350
)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)

const detailVisible = ref(false)
const selectedPurchase = ref(null)

const form = reactive({
  purchaseCode: '',
  supplierId: null,
  note: '',
  items: []
})

const confirmedPageSum = computed(() => {
  return items.value
    .filter((x) => x.status === 'CONFIRMED')
    .reduce((sum, x) => sum + Number(x.totalAmount || 0), 0)
})

const computedFormTotal = computed(() => {
  return form.items.reduce((sum, i) => sum + Number(i.quantity || 0) * Number(i.unitPrice || 0), 0)
})

async function loadMeta() {
  const [sRes, vRes] = await Promise.all([
    supplierService.getAllSuppliers(),
    variantService.getAllVariants()
  ])
  suppliers.value = sRes.data || []
  variants.value = vRes.data || []
}

async function loadData() {
  loading.value = true
  try {
    const res = await purchaseService.getPurchases({
      keyword: filters.keyword,
      supplierId: filters.supplierId || undefined,
      status: filters.status,
      page: pagination.page - 1,
      size: pagination.size
    })
    items.value = res.data.content || []
    pagination.total = res.data.totalElements || 0
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filters.keyword = ''
  filters.supplierId = null
  filters.status = ''
  pagination.page = 1
  loadData()
}

function addLineItem() {
  const firstVar = variants.value[0]
  form.items.push({
    productVariantId: firstVar?.id || null,
    quantity: 10,
    unitPrice: firstVar ? Number(firstVar.importPrice) : 1800000
  })
}

function onVariantSelect(row, variantId) {
  const found = variants.value.find((v) => v.id === variantId)
  if (found) {
    row.unitPrice = Number(found.importPrice)
  }
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  form.purchaseCode = ''
  form.supplierId = suppliers.value[0]?.id || null
  form.note = ''
  form.items = []
  addLineItem()
  dialogVisible.value = true
}

async function openEdit(row) {
  const res = await purchaseService.getPurchaseById(row.id)
  const po = res.data
  isEdit.value = true
  editingId.value = po.id
  form.purchaseCode = po.purchaseCode
  form.supplierId = po.supplierId
  form.note = po.note || ''
  form.items = (po.details || []).map((d) => ({
    productVariantId: d.productVariantId,
    quantity: d.quantity,
    unitPrice: Number(d.unitPrice)
  }))
  dialogVisible.value = true
}

async function openDetail(row) {
  const res = await purchaseService.getPurchaseById(row.id)
  selectedPurchase.value = res.data
  detailVisible.value = true
}

async function submitForm(confirmImmediately) {
  if (!form.supplierId) {
    ElMessage.warning('Vui lòng chọn nhà cung cấp')
    return
  }
  if (!form.items.length || form.items.some((i) => !i.productVariantId || i.quantity < 1)) {
    ElMessage.warning('Vui lòng chọn đầy đủ biến thể sản phẩm và số lượng nhập hợp lệ')
    return
  }
  saving.value = true
  try {
    const payload = {
      supplierId: form.supplierId,
      note: form.note,
      confirmImmediately,
      items: form.items
    }
    if (isEdit.value) {
      await purchaseService.updatePurchase(editingId.value, payload)
      ElMessage.success('Đã cập nhật phiếu nhập nháp')
    } else {
      await purchaseService.createPurchase(payload)
      ElMessage.success(confirmImmediately ? 'Đã tạo và xác nhận phiếu nhập thành công' : 'Đã lưu phiếu nhập nháp')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

function handlePurchaseAction(cmd, row) {
  if (cmd === 'confirm') handleConfirm(row)
  else if (cmd === 'cancel') handleCancel(row)
}

function handleConfirm(row) {
  ElMessageBox.confirm(
    `Xác nhận nhập hàng cho phiếu "${row.purchaseCode}"? Sau khi xác nhận sẽ không thể chỉnh sửa.`,
    'Xác nhận phiếu nhập',
    { type: 'warning' }
  ).then(async () => {
    await purchaseService.confirmPurchase(row.id)
    ElMessage.success(`Đã xác nhận phiếu nhập ${row.purchaseCode}`)
    loadData()
  }).catch(() => {})
}

function handleCancel(row) {
  ElMessageBox.confirm(
    `Bạn có chắc chắn muốn hủy phiếu nhập nháp "${row.purchaseCode}"?`,
    'Hủy phiếu nhập',
    { type: 'error' }
  ).then(async () => {
    await purchaseService.cancelPurchase(row.id)
    ElMessage.success(`Đã hủy phiếu nhập ${row.purchaseCode}`)
    loadData()
  }).catch(() => {})
}

function handleSoftDelete(row) {
  ElMessageBox.confirm(
    `Bạn có chắc chắn muốn xóa mềm phiếu nhập "${row.purchaseCode}"? Dữ liệu lịch sử vẫn được lưu giữ trong hệ thống.`,
    'Xác nhận xóa mềm',
    { type: 'warning', confirmButtonText: 'Xóa mềm', cancelButtonText: 'Hủy', confirmButtonClass: 'el-button--danger' }
  ).then(async () => {
    await softDeleteService.remove('purchases', row.id)
    ElMessage.success(`Đã xóa mềm phiếu nhập ${row.purchaseCode}`)
    loadData()
  }).catch(() => {})
}

onMounted(() => {
  loadMeta()
  loadData()
})
</script>

<style scoped lang="scss">
.summary-strip {
  display: flex;
  gap: 24px;
  background: #ffffff;
  padding: 12px 20px;
  border-radius: 5px;
  border: 1px solid #e5e5e5;
  font-size: 13.5px;
  .strip-item {
    display: flex;
    align-items: center;
    gap: 8px;
    .label {
      color: #64748b;
    }
  }
}
.items-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 10px 0;
}
.total-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}
</style>
