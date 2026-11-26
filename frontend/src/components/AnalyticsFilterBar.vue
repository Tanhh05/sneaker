<template>
  <div class="analytics-filter-bar">
    <!-- Row 1: Quick Time Presets + Custom Date Range + GroupBy -->
    <div class="filter-row top-row">
      <div class="preset-pills">
        <button
          v-for="p in timePresets"
          :key="p.key"
          type="button"
          class="preset-pill"
          :class="{ active: activePreset === p.key }"
          @click="applyPreset(p.key)"
        >
          {{ p.label }}
        </button>
      </div>

      <div class="date-and-group">
        <el-date-picker
          v-model="localDateRange"
          type="daterange"
          unlink-panels
          range-separator="→"
          start-placeholder="Từ ngày"
          end-placeholder="Đến ngày"
          value-format="YYYY-MM-DD"
          format="DD/MM/YYYY"
          size="small"
          clearable
          class="custom-daterange"
          @change="onCustomDateChange"
        />
        <el-select
          v-if="showGroupBy"
          :model-value="modelValue.groupBy"
          size="small"
          class="group-select"
          @change="(v) => emitPatch({ groupBy: v })"
        >
          <el-option label="Gom: Theo Ngày" value="DAY" />
          <el-option label="Gom: Theo Tuần" value="WEEK" />
          <el-option label="Gom: Theo Tháng" value="MONTH" />
          <el-option label="Gom: Theo Quý" value="QUARTER" />
          <el-option label="Gom: Theo Năm" value="YEAR" />
        </el-select>
      </div>
    </div>

    <!-- Row 2: Channel, Status, Payment Method, Brand, Category, Reset -->
    <div class="filter-row bottom-row">
      <el-select
        :model-value="modelValue.channel"
        placeholder="Kênh bán"
        size="small"
        clearable
        class="dim-select"
        @change="(v) => emitPatch({ channel: v || '' })"
      >
        <el-option label="Tất cả kênh bán" value="" />
        <el-option label="Bán tại quầy (POS)" value="POS" />
        <el-option label="Trực tuyến (Online Store)" value="ONLINE" />
      </el-select>

      <el-select
        :model-value="modelValue.orderStatus"
        placeholder="Trạng thái đơn"
        size="small"
        clearable
        class="dim-select"
        @change="(v) => emitPatch({ orderStatus: v || '' })"
      >
        <el-option label="Tất cả trạng thái" value="" />
        <el-option label="Đơn thành công (COMPLETED)" value="COMPLETED" />
        <el-option label="Đang xử lý / Đang giao" value="PROCESSING" />
        <el-option label="Đã hủy / Trả hàng" value="CANCELLED" />
      </el-select>

      <el-select
        :model-value="modelValue.paymentMethod"
        placeholder="Phương thức TT"
        size="small"
        clearable
        class="dim-select"
        @change="(v) => emitPatch({ paymentMethod: v || '' })"
      >
        <el-option label="Tất cả PT thanh toán" value="" />
        <el-option label="Tiền mặt (CASH)" value="CASH" />
        <el-option label="Chuyển khoản (SePay)" value="BANK_TRANSFER" />
        <el-option label="Thu hộ (COD)" value="COD" />
      </el-select>

      <div class="dim-quick-select">
        <QuickSelect
          :model-value="modelValue.brandId"
          :options="brands"
          value-key="id"
          label-key="brandName"
          placeholder="Tất cả Thương hiệu"
          search-placeholder="Tìm thương hiệu..."
          clearable
          @update:model-value="(v) => emitPatch({ brandId: v || null })"
        />
      </div>

      <div class="dim-quick-select">
        <QuickSelect
          :model-value="modelValue.categoryId"
          :options="categories"
          value-key="id"
          label-key="categoryName"
          placeholder="Tất cả Danh mục"
          search-placeholder="Tìm danh mục..."
          clearable
          @update:model-value="(v) => emitPatch({ categoryId: v || null })"
        />
      </div>

      <el-button size="small" class="btn-reset-filter" @click="handleReset">
        <el-icon><RefreshLeft /></el-icon>
        <span>Đặt lại</span>
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { RefreshLeft } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true
  },
  brands: {
    type: Array,
    default: () => []
  },
  categories: {
    type: Array,
    default: () => []
  },
  showGroupBy: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const activePreset = ref('ALL')
const localDateRange = ref(
  props.modelValue.fromDate && props.modelValue.toDate
    ? [props.modelValue.fromDate, props.modelValue.toDate]
    : null
)

watch(
  () => [props.modelValue.fromDate, props.modelValue.toDate],
  ([f, t]) => {
    localDateRange.value = f && t ? [f, t] : null
  }
)

const timePresets = [
  { key: 'ALL', label: 'Toàn thời gian' },
  { key: 'TODAY', label: 'Hôm nay' },
  { key: 'YESTERDAY', label: 'Hôm qua' },
  { key: 'LAST_7', label: '7 ngày qua' },
  { key: 'THIS_MONTH', label: 'Tháng này' },
  { key: 'LAST_MONTH', label: 'Tháng trước' },
  { key: 'THIS_QUARTER', label: 'Quý này' },
  { key: 'THIS_YEAR', label: 'Năm nay' }
]

function ymd(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function emitPatch(patch) {
  const next = { ...props.modelValue, ...patch }
  emit('update:modelValue', next)
  emit('change', next)
}

function applyPreset(key) {
  activePreset.value = key
  const now = new Date()
  let from = null
  let to = null
  let groupBy = props.modelValue.groupBy

  if (key === 'TODAY') {
    from = ymd(now)
    to = ymd(now)
    groupBy = 'DAY'
  } else if (key === 'YESTERDAY') {
    const y = new Date(now)
    y.setDate(y.getDate() - 1)
    from = ymd(y)
    to = ymd(y)
    groupBy = 'DAY'
  } else if (key === 'LAST_7') {
    const s = new Date(now)
    s.setDate(s.getDate() - 6)
    from = ymd(s)
    to = ymd(now)
    groupBy = 'DAY'
  } else if (key === 'THIS_MONTH') {
    const s = new Date(now.getFullYear(), now.getMonth(), 1)
    from = ymd(s)
    to = ymd(now)
    groupBy = 'DAY'
  } else if (key === 'LAST_MONTH') {
    const s = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    const e = new Date(now.getFullYear(), now.getMonth(), 0)
    from = ymd(s)
    to = ymd(e)
    groupBy = 'DAY'
  } else if (key === 'THIS_QUARTER') {
    const qMonth = Math.floor(now.getMonth() / 3) * 3
    const s = new Date(now.getFullYear(), qMonth, 1)
    from = ymd(s)
    to = ymd(now)
    groupBy = 'MONTH'
  } else if (key === 'THIS_YEAR') {
    const s = new Date(now.getFullYear(), 0, 1)
    from = ymd(s)
    to = ymd(now)
    groupBy = 'MONTH'
  } else {
    from = null
    to = null
  }

  localDateRange.value = from && to ? [from, to] : null
  emitPatch({ fromDate: from, toDate: to, groupBy })
}

function onCustomDateChange(val) {
  activePreset.value = val ? 'CUSTOM' : 'ALL'
  if (Array.isArray(val) && val.length === 2) {
    emitPatch({ fromDate: val[0], toDate: val[1] })
  } else {
    emitPatch({ fromDate: null, toDate: null })
  }
}

function handleReset() {
  activePreset.value = 'ALL'
  localDateRange.value = null
  emitPatch({
    groupBy: 'DAY',
    fromDate: null,
    toDate: null,
    channel: '',
    orderStatus: '',
    paymentMethod: '',
    brandId: null,
    categoryId: null
  })
}
</script>

<style scoped lang="scss">
.analytics-filter-bar {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.top-row {
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.bottom-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr)) auto;
  gap: 10px;
  align-items: center;
}

.preset-pills {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.preset-pill {
  height: 30px;
  padding: 0 11px;
  border-radius: 4px;
  border: 1px solid #e5e5e5;
  background: #fafafa;
  color: #525252;
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.14s ease;

  &:hover {
    border-color: var(--vea-theme, #409eff);
    color: #303133;
    background: #ffffff;
  }

  &.active {
    background: var(--vea-theme, #409eff);
    border-color: var(--vea-theme, #409eff);
    color: #ffffff;
  }
}

.date-and-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.custom-daterange {
  width: 245px !important;
}

.group-select {
  width: 152px;
}

.dim-select,
.dim-quick-select {
  width: 100%;
}

.btn-reset-filter {
  border-radius: 5px !important;
  font-weight: 700;
  height: 32px;
  padding: 0 14px;
}

@media (max-width: 1180px) {
  .bottom-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .bottom-row {
    grid-template-columns: 1fr;
  }
  .custom-daterange,
  .group-select {
    width: 100% !important;
  }
}
</style>
