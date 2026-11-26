<template>
  <div class="page-container reports-workspace" v-loading="loading">
    <!-- Header (Compact Command Bar + Quick Time Pills + Filter Popover — 0px wasted height!) -->
    <div class="page-header">
      <div class="page-title-group">
        <div class="report-title-line">
          <h2 class="page-title">BÁO CÁO &amp; THỐNG KÊ</h2>
          <span class="report-period-chip">{{ activeReportPeriodLabel }}</span>
        </div>
        <p class="page-subtitle">
          Doanh thu, dòng tiền, kênh bán, đơn hủy, biến thể Size/Màu và hiệu suất nhân viên.
        </p>
      </div>
      <div class="header-actions">
        <div class="quick-time-group">
          <button
            v-for="p in quickPresets"
            :key="p.key"
            type="button"
            class="qt-btn"
            :class="{ active: activeQuickPreset === p.key }"
            @click="applyQuickPreset(p.key)"
          >
            {{ p.label }}
          </button>
        </div>

        <el-popover placement="bottom-end" :width="780" trigger="click">
          <template #reference>
            <el-button size="small" :type="activeFilterCount > 0 ? 'primary' : ''" :plain="activeFilterCount > 0">
              <el-icon><Filter /></el-icon>
              <span>Bộ lọc</span>
              <span v-if="activeFilterCount > 0" class="filter-count-dot">{{ activeFilterCount }}</span>
            </el-button>
          </template>
          <AnalyticsFilterBar
            v-model="filters"
            :brands="brands"
            :categories="categories"
            @change="onAdvancedFilterChange"
          />
        </el-popover>

        <el-tooltip content="Làm mới báo cáo" placement="bottom">
          <el-button size="small" class="btn-soft" :loading="loading" @click="loadReport">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-tooltip>

        <el-button type="success" size="small" class="btn-excel" :loading="exporting" @click="exportExcel">
          <el-icon><Download /></el-icon>
          <span>Xuất Excel</span>
        </el-button>
      </div>
    </div>

    <!-- Minimalist Financial Summary Bar -->
    <div class="summary-strip">
      <div class="sum-box primary">
        <span class="sum-label">Doanh thu thực nhận</span>
        <strong class="sum-val">{{ formatCurrency(report.totalRevenue) }}</strong>
        <span class="sum-sub">{{ report.totalCompletedOrders }} đơn &middot; {{ report.totalQuantitySold }} đôi</span>
      </div>
      <div class="sum-box">
        <span class="sum-label">Trước thuế / VAT</span>
        <strong class="sum-val">{{ formatCurrency(report.totalPreTaxRevenue) }}</strong>
        <span class="sum-sub">VAT: {{ formatCurrency(report.totalVatAmount) }}</span>
      </div>
      <div class="sum-box">
        <span class="sum-label">Chuyển khoản / Tiền mặt</span>
        <strong class="sum-val">{{ formatCurrency(report.totalBankTransferRevenue) }}</strong>
        <span class="sum-sub">Tiền mặt: {{ formatCurrency(report.totalCashRevenue) }} &middot; COD: {{ formatCurrency(report.totalCodRevenue) }}</span>
      </div>
      <div class="sum-box">
        <span class="sum-label">Thất thoát hủy đơn</span>
        <strong class="sum-val text-red">{{ formatCurrency(report.cancelledValue) }}</strong>
        <span class="sum-sub">Tỷ lệ hủy: {{ report.cancellationRate }}%</span>
      </div>
    </div>

    <!-- 5 Specialized Report Modules -->
    <div class="reports-surface">
      <el-tabs v-model="activeTab" class="report-tabs">
        <!-- MODULE 1: Doanh thu & Dòng tiền -->
        <el-tab-pane label="1. Doanh thu & Dòng tiền" name="cashflow">
          <div class="tab-intro">
            Biểu đồ dòng tiền và bảng kê chi tiết theo chu kỳ đã chọn (đối soát tiền hàng trước thuế, VAT và 3 luồng tiền mặt / chuyển khoản SePay / thu hộ COD).
          </div>
          <div v-if="(report.revenueSeries || []).length" class="report-chart-card">
            <BaseChart :option="cashflowChartOption" height="260px" />
          </div>
          <el-table :data="report.revenueSeries || []" border stripe show-summary :summary-method="cashflowSummary" style="width: 100%">
            <template #empty><el-empty description="Không có dữ liệu doanh thu trong kỳ lọc" :image-size="70" /></template>
            <el-table-column prop="period" label="Kỳ báo cáo" width="135" fixed="left">
              <template #default="{ row }"><span class="mono-tag">{{ row.period }}</span></template>
            </el-table-column>
            <el-table-column prop="orderCount" label="Số đơn hoàn thành" width="140" align="center" />
            <el-table-column prop="preTaxRevenue" label="Tiền hàng trước thuế" min-width="155" align="right">
              <template #default="{ row }">{{ formatCurrency(row.preTaxRevenue) }}</template>
            </el-table-column>
            <el-table-column prop="vatAmount" label="Tiền thuế VAT" min-width="135" align="right">
              <template #default="{ row }"><span class="text-blue">{{ formatCurrency(row.vatAmount) }}</span></template>
            </el-table-column>
            <el-table-column prop="cashRevenue" label="Tiền mặt thu được" min-width="150" align="right">
              <template #default="{ row }">{{ formatCurrency(row.cashRevenue) }}</template>
            </el-table-column>
            <el-table-column prop="bankTransferRevenue" label="Tiền chuyển khoản (SePay)" min-width="175" align="right">
              <template #default="{ row }">{{ formatCurrency(row.bankTransferRevenue) }}</template>
            </el-table-column>
            <el-table-column prop="codRevenue" label="Tiền COD thu được" min-width="150" align="right">
              <template #default="{ row }">{{ formatCurrency(row.codRevenue) }}</template>
            </el-table-column>
            <el-table-column prop="revenue" label="Tổng doanh thu thực nhận" min-width="180" align="right" fixed="right">
              <template #default="{ row }"><strong class="text-red">{{ formatCurrency(row.revenue) }}</strong></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- MODULE 2: Hiệu quả Kênh bán (POS vs Online) -->
        <el-tab-pane label="2. Hiệu quả Kênh bán (POS vs Online)" name="channels">
          <div v-if="(report.channelComparison || []).length" class="report-chart-card">
            <BaseChart :option="reportChannelChartOption" height="230px" />
          </div>
          <div class="channel-compare-cards">
            <div
              v-for="ch in report.channelComparison || []"
              :key="ch.channel"
              class="ch-detail-card"
              :class="ch.channel === 'POS' ? 'is-pos' : 'is-online'"
            >
              <div class="ch-card-title">
                <strong>{{ ch.label }}</strong>
                <span class="share-pill">{{ ch.revenueSharePercent }}% tổng doanh thu</span>
              </div>
              <div class="ch-big-money">{{ formatCurrency(ch.actualRevenue) }}</div>
              <div class="ch-kv-list">
                <div><span>Tổng số đơn đặt:</span><strong>{{ ch.totalOrders }} đơn</strong></div>
                <div><span>Đơn giao thành công / hoàn thành:</span><strong class="text-green">{{ ch.completedOrders }} đơn ({{ ch.successRate }}%)</strong></div>
                <div><span>Đơn hủy / Bom hàng:</span><strong class="text-red">{{ ch.cancelledOrders }} đơn ({{ ch.cancellationRate }}%)</strong></div>
                <div><span>Tỷ lệ thanh toán ngay:</span><strong>{{ ch.instantPaymentRate }}%</strong></div>
                <div><span>Phí giao vận phát sinh:</span><strong>{{ formatCurrency(ch.shippingFee) }}</strong></div>
                <div><span>Giá trị trung bình đơn (AOV):</span><strong>{{ formatCurrency(ch.aov) }}</strong></div>
              </div>
            </div>
          </div>

          <el-table :data="report.channelComparison || []" border stripe style="width: 100%; margin-top: 16px">
            <el-table-column prop="label" label="Kênh bán hàng" min-width="190" />
            <el-table-column prop="totalOrders" label="Tổng đơn đặt" width="115" align="center" />
            <el-table-column prop="completedOrders" label="Thành công" width="110" align="center" />
            <el-table-column prop="cancelledOrders" label="Hủy / Bom hàng" width="125" align="center" />
            <el-table-column label="Tỷ lệ thành công" width="130" align="center">
              <template #default="{ row }"><strong class="text-green">{{ row.successRate }}%</strong></template>
            </el-table-column>
            <el-table-column label="Phí giao vận" width="135" align="right">
              <template #default="{ row }">{{ formatCurrency(row.shippingFee) }}</template>
            </el-table-column>
            <el-table-column label="Giá trị hủy" width="145" align="right">
              <template #default="{ row }"><span class="text-red">{{ formatCurrency(row.cancelledValue) }}</span></template>
            </el-table-column>
            <el-table-column label="Doanh thu thực nhận" width="170" align="right">
              <template #default="{ row }"><strong>{{ formatCurrency(row.actualRevenue) }}</strong></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- MODULE 3: Đơn hàng & Tỷ lệ Hủy/Hoàn trả -->
        <el-tab-pane label="3. Đơn hàng & Tỷ lệ Hủy / Hoàn trả" name="cancellations">
          <el-row :gutter="16">
            <el-col :xs="24" :lg="10">
              <h4 class="sub-heading">Phân loại lý do hủy đơn / bom hàng</h4>
              <div v-if="(report.cancelReasonStats || []).length" class="report-chart-card" style="margin-bottom: 0">
                <BaseChart :option="cancelReasonChartOption" height="280px" />
              </div>
              <el-empty v-else description="Chưa có dữ liệu hủy đơn" :image-size="60" />
            </el-col>
            <el-col :xs="24" :lg="14">
              <h4 class="sub-heading">Chi tiết danh sách đơn không thành công ({{ (report.cancelledOrders || []).length }} đơn)</h4>
              <el-table :data="report.cancelledOrders || []" border stripe size="small" style="width: 100%">
                <template #empty><el-empty description="Không có đơn hàng bị hủy trong kỳ lọc" :image-size="60" /></template>
                <el-table-column label="Mã đơn" width="110">
                  <template #default="{ row }">
                    <router-link :to="`/admin/orders/${row.orderId}`" class="ref-link">{{ row.orderCode }}</router-link>
                  </template>
                </el-table-column>
                <el-table-column label="Kênh" width="85" align="center">
                  <template #default="{ row }">{{ row.channel === 'ONLINE' ? 'Online' : 'POS' }}</template>
                </el-table-column>
                <el-table-column prop="customerName" label="Khách hàng" min-width="130" />
                <el-table-column label="Giá trị" width="120" align="right">
                  <template #default="{ row }"><span class="text-red">{{ formatCurrency(row.amount) }}</span></template>
                </el-table-column>
                <el-table-column prop="reasonCategory" label="Nhóm lý do" min-width="160" />
                <el-table-column prop="cancelNote" label="Ghi chú chi tiết" min-width="180" show-overflow-tooltip />
              </el-table>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- MODULE 4: Biến thể Size, Màu sắc & Thương hiệu -->
        <el-tab-pane label="4. Biến thể Size, Màu sắc & Thương hiệu" name="variants">
          <!-- Size Group Summary (Women 36-39 vs Men 40-45 + Golden Sizes) -->
          <div class="size-insight-row">
            <div class="insight-box">
              <div class="ins-label">Tỷ trọng Size Nữ (EU 36–39)</div>
              <div class="ins-val">{{ womenSizeStats.qty }} đôi ({{ womenSizeStats.pct }}%)</div>
            </div>
            <div class="insight-box">
              <div class="ins-label">Tỷ trọng Size Nam (EU 40–45)</div>
              <div class="ins-val">{{ menSizeStats.qty }} đôi ({{ menSizeStats.pct }}%)</div>
            </div>
            <div class="insight-box golden">
              <div class="ins-label">Nhóm "Size Vàng" (Ưu tiên 60–70% nhập hàng)</div>
              <div class="ins-val">{{ goldenSizesLabel }}</div>
            </div>
          </div>

          <el-row v-if="(report.sizeStats || []).length || (report.brandStats || []).length" :gutter="16" style="margin-top: 14px">
            <el-col :xs="24" :lg="14">
              <div class="report-chart-card" style="margin-bottom: 0">
                <div class="rc-title">Biểu đồ Hấp thụ Size giày (Màu vàng = Size Vàng ưu tiên nhập)</div>
                <BaseChart :option="sizeAbsorptionChartOption" height="230px" />
              </div>
            </el-col>
            <el-col :xs="24" :lg="10">
              <div class="report-chart-card" style="margin-bottom: 0">
                <div class="rc-title">Tỷ trọng Doanh thu theo Thương hiệu</div>
                <BaseChart :option="brandDonutChartOption" height="230px" />
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="16" style="margin-top: 14px">
            <el-col :xs="24" :lg="12">
              <h4 class="sub-heading">Độ hấp thụ thị trường theo từng Size giày</h4>
              <el-table :data="report.sizeStats || []" border stripe size="small" style="width: 100%">
                <el-table-column label="Size" width="130">
                  <template #default="{ row }">
                    <strong>EU {{ row.sizeValue }}</strong>
                    <span v-if="row.goldenSize" class="gold-badge">SIZE VÀNG</span>
                  </template>
                </el-table-column>
                <el-table-column label="Nhóm" width="110">
                  <template #default="{ row }">
                    {{ row.sizeGroup === 'SIZE_NU' ? 'Size Nữ' : row.sizeGroup === 'SIZE_NAM' ? 'Size Nam' : 'Khác' }}
                  </template>
                </el-table-column>
                <el-table-column prop="quantitySold" label="Đã bán" width="80" align="center" />
                <el-table-column label="Tỷ trọng" width="85" align="center">
                  <template #default="{ row }"><strong>{{ row.sharePercent }}%</strong></template>
                </el-table-column>
                <el-table-column prop="currentStock" label="Tồn kho" width="80" align="center" />
                <el-table-column label="Doanh thu" min-width="125" align="right">
                  <template #default="{ row }">{{ formatCurrency(row.revenue) }}</template>
                </el-table-column>
              </el-table>
            </el-col>

            <el-col :xs="24" :lg="12">
              <h4 class="sub-heading">Doanh thu &amp; Số lượng theo Thương hiệu</h4>
              <el-table :data="report.brandStats || []" border stripe size="small" style="width: 100%; margin-bottom: 16px">
                <el-table-column prop="brandName" label="Thương hiệu" min-width="130" />
                <el-table-column prop="productCount" label="Số mẫu" width="75" align="center" />
                <el-table-column prop="quantitySold" label="Đã bán" width="80" align="center" />
                <el-table-column label="Tỷ trọng" width="85" align="center">
                  <template #default="{ row }">{{ row.sharePercent }}%</template>
                </el-table-column>
                <el-table-column label="Doanh thu" width="140" align="right">
                  <template #default="{ row }"><strong class="text-red">{{ formatCurrency(row.revenue) }}</strong></template>
                </el-table-column>
              </el-table>

              <h4 class="sub-heading">Phân tích theo Màu sắc bán chạy</h4>
              <el-table :data="report.colorStats || []" border stripe size="small" style="width: 100%">
                <el-table-column label="Màu sắc" min-width="140">
                  <template #default="{ row }">
                    <span class="color-swatch" :style="{ background: row.hexCode || '#cbd5e1' }" />
                    <span>{{ row.colorName }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="quantitySold" label="Đã bán" width="85" align="center" />
                <el-table-column label="Tỷ trọng" width="85" align="center">
                  <template #default="{ row }">{{ row.sharePercent }}%</template>
                </el-table-column>
                <el-table-column label="Doanh thu" width="140" align="right">
                  <template #default="{ row }">{{ formatCurrency(row.revenue) }}</template>
                </el-table-column>
              </el-table>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- MODULE 5: Hiệu suất Nhân viên -->
        <el-tab-pane label="5. Hiệu suất Nhân viên (KPI / Hoa hồng)" name="employees">
          <div class="tab-intro">
            Thống kê doanh số bán hàng của từng nhân viên thu ngân / tư vấn tại quầy phục vụ đánh giá KPI &amp; tính hoa hồng định kỳ.
          </div>
          <el-table :data="report.employeeRevenueStats || []" border stripe style="width: 100%">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="employeeCode" label="Mã NV" width="120">
              <template #default="{ row }"><span class="mono-tag">{{ row.employeeCode }}</span></template>
            </el-table-column>
            <el-table-column prop="employeeName" label="Tên nhân viên" min-width="200">
              <template #default="{ row, $index }">
                <strong>{{ row.employeeName }}</strong>
                <span v-if="$index === 0 && Number(row.totalRevenue) > 0" class="top-emp-badge">TOP 1</span>
              </template>
            </el-table-column>
            <el-table-column prop="workingShifts" label="Số ca làm việc" width="145" align="center">
              <template #default="{ row }">{{ row.workingShifts }} ca</template>
            </el-table-column>
            <el-table-column prop="completedOrderCount" label="Số đơn tạo thành công" width="175" align="center">
              <template #default="{ row }"><strong>{{ row.completedOrderCount }}</strong> đơn</template>
            </el-table-column>
            <el-table-column label="Tổng doanh thu mang về" width="195" align="right">
              <template #default="{ row }"><strong class="text-red">{{ formatCurrency(row.totalRevenue) }}</strong></template>
            </el-table-column>
            <el-table-column label="Giá trị TB / đơn tư vấn (AOV)" width="205" align="right">
              <template #default="{ row }">{{ formatCurrency(row.averageOrderValue) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Download, Refresh, Filter } from '@element-plus/icons-vue'
import { reportService } from '@/services/reportService'
import { brandService } from '@/services/brandService'
import { categoryService } from '@/services/categoryService'
import { useAppSettingsStore } from '@/stores/appSettings'
import { formatCurrency } from '@/utils/formatters'
import AnalyticsFilterBar from '@/components/AnalyticsFilterBar.vue'
import BaseChart from '@/components/charts/BaseChart.vue'

const settings = useAppSettingsStore()
const themeColor = computed(() => settings.theme || '#409EFF')

function formatShortMoney(val) {
  const n = Number(val || 0)
  if (n >= 1_000_000_000) return (n / 1_000_000_000).toFixed(1) + 'B'
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M'
  if (n >= 1_000) return Math.round(n / 1_000) + 'K'
  return String(n)
}

const loading = ref(false)
const exporting = ref(false)
const activeTab = ref('cashflow')
const activeQuickPreset = ref('ALL')

const quickPresets = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'TODAY', label: 'Hôm nay' },
  { key: 'LAST_7', label: '7 ngày' },
  { key: 'THIS_MONTH', label: 'Tháng này' }
]

const brands = ref([])
const categories = ref([])

const filters = ref({
  groupBy: 'DAY',
  fromDate: null,
  toDate: null,
  channel: '',
  orderStatus: '',
  paymentMethod: '',
  brandId: null,
  categoryId: null
})

const activeFilterCount = computed(() => {
  const f = filters.value
  let c = 0
  if (f.channel) c++
  if (f.orderStatus) c++
  if (f.paymentMethod) c++
  if (f.brandId) c++
  if (f.categoryId) c++
  if (f.fromDate || f.toDate) c++
  return c
})

const activeReportPeriodLabel = computed(() => {
  const f = filters.value
  if (f.fromDate && f.toDate) return `${f.fromDate} → ${f.toDate}`
  return 'Toàn thời gian'
})

function ymd(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function applyQuickPreset(key) {
  activeQuickPreset.value = key
  const now = new Date()
  if (key === 'TODAY') {
    filters.value = { ...filters.value, fromDate: ymd(now), toDate: ymd(now) }
  } else if (key === 'LAST_7') {
    const s = new Date(now)
    s.setDate(s.getDate() - 6)
    filters.value = { ...filters.value, fromDate: ymd(s), toDate: ymd(now) }
  } else if (key === 'THIS_MONTH') {
    const s = new Date(now.getFullYear(), now.getMonth(), 1)
    filters.value = { ...filters.value, fromDate: ymd(s), toDate: ymd(now) }
  } else {
    filters.value = { ...filters.value, fromDate: null, toDate: null }
  }
  loadReport()
}

function onAdvancedFilterChange() {
  activeQuickPreset.value = filters.value.fromDate ? 'CUSTOM' : 'ALL'
  loadReport()
}

const report = ref({
  totalRevenue: 0,
  totalPreTaxRevenue: 0,
  totalVatAmount: 0,
  totalCashRevenue: 0,
  totalBankTransferRevenue: 0,
  totalCodRevenue: 0,
  provisionalRevenue: 0,
  cancelledValue: 0,
  totalOrders: 0,
  totalCompletedOrders: 0,
  totalCancelledOrders: 0,
  totalQuantitySold: 0,
  aov: 0,
  deliverySuccessRate: 0,
  cancellationRate: 0,
  revenueSeries: [],
  channelComparison: [],
  cancelReasonStats: [],
  cancelledOrders: [],
  sizeStats: [],
  colorStats: [],
  brandStats: [],
  employeeRevenueStats: []
})

const totalSizeQty = computed(() =>
  (report.value.sizeStats || []).reduce((s, x) => s + Number(x.quantitySold || 0), 0)
)

const womenSizeStats = computed(() => {
  const qty = (report.value.sizeStats || [])
    .filter((s) => s.sizeGroup === 'SIZE_NU')
    .reduce((sum, s) => sum + Number(s.quantitySold || 0), 0)
  const pct = totalSizeQty.value > 0 ? Math.round((qty / totalSizeQty.value) * 1000) / 10 : 0
  return { qty, pct }
})

const menSizeStats = computed(() => {
  const qty = (report.value.sizeStats || [])
    .filter((s) => s.sizeGroup === 'SIZE_NAM')
    .reduce((sum, s) => sum + Number(s.quantitySold || 0), 0)
  const pct = totalSizeQty.value > 0 ? Math.round((qty / totalSizeQty.value) * 1000) / 10 : 0
  return { qty, pct }
})

const goldenSizesLabel = computed(() => {
  const golds = (report.value.sizeStats || []).filter((s) => s.goldenSize).map((s) => `EU ${s.sizeValue}`)
  return golds.length ? golds.join(', ') : 'Chưa có dữ liệu bán'
})

// ==================== ECHARTS FOR REPORTS ====================
const cashflowChartOption = computed(() => {
  const list = report.value.revenueSeries || []
  return {
    animationDuration: 500,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: '#ffffff',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#303133', fontSize: 12 }
    },
    legend: {
      top: 0,
      left: 0,
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { color: '#606266', fontSize: 12 }
    },
    grid: { top: 38, left: 56, right: 44, bottom: 26 },
    xAxis: {
      type: 'category',
      data: list.map((x) => x.period),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLabel: { color: '#64748b', fontSize: 11, formatter: (v) => formatShortMoney(v) }
      },
      {
        type: 'value',
        minInterval: 1,
        splitLine: { show: false },
        axisLabel: { color: '#f97316', fontSize: 11, formatter: (v) => `${v} đ` }
      }
    ],
    series: [
      {
        name: 'Chuyển khoản (SePay)',
        type: 'bar',
        stack: 'cf',
        barMaxWidth: 38,
        data: list.map((x) => Number(x.bankTransferRevenue || 0)),
        itemStyle: { color: '#3b82f6' }
      },
      {
        name: 'Tiền mặt (CASH)',
        type: 'bar',
        stack: 'cf',
        barMaxWidth: 38,
        data: list.map((x) => Number(x.cashRevenue || 0)),
        itemStyle: { color: '#f59e0b' }
      },
      {
        name: 'Thu hộ (COD)',
        type: 'bar',
        stack: 'cf',
        barMaxWidth: 38,
        data: list.map((x) => Number(x.codRevenue || 0)),
        itemStyle: { color: '#10b981', borderRadius: [6, 6, 0, 0] }
      },
      {
        name: 'Số đơn hoàn thành',
        type: 'line',
        yAxisIndex: 1,
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 7,
        data: list.map((x) => Number(x.orderCount || 0)),
        lineStyle: { color: '#f97316', width: 2.5 },
        itemStyle: { color: '#ffffff', borderColor: '#f97316', borderWidth: 2 }
      }
    ]
  }
})

const reportChannelChartOption = computed(() => {
  const list = report.value.channelComparison || []
  return {
    animationDuration: 500,
    tooltip: {
      trigger: 'item',
      formatter: (p) => `<strong>${p.name}</strong><br/>${formatCurrency(p.value)} (${p.percent}%)`
    },
    legend: {
      orient: 'vertical',
      right: '12%',
      top: 'middle',
      icon: 'circle',
      textStyle: { color: '#475569', fontSize: 12.5, fontWeight: 600 }
    },
    series: [
      {
        name: 'Kênh bán hàng',
        type: 'pie',
        radius: ['52%', '78%'],
        center: ['32%', '50%'],
        itemStyle: { borderRadius: 8, borderColor: '#ffffff', borderWidth: 3 },
        label: { show: true, formatter: '{d}%', fontWeight: 700, fontSize: 12 },
        data: list.map((ch) => ({
          name: ch.label || ch.channel,
          value: Number(ch.actualRevenue || 0),
          itemStyle: { color: ch.channel === 'POS' ? themeColor.value : '#f97316' }
        }))
      }
    ]
  }
})

const cancelReasonChartOption = computed(() => {
  const list = [...(report.value.cancelReasonStats || [])].reverse()
  return {
    animationDuration: 500,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const idx = params?.[0]?.dataIndex ?? 0
        const r = list[idx]
        if (!r) return ''
        return `<strong>${r.reasonCategory}</strong><br/>Số đơn: <b>${r.count} (${r.percentage}%)</b><br/>Thất thoát: <b style="color:#dc2626">${formatCurrency(r.lostValue)}</b>`
      }
    },
    grid: { top: 10, left: 10, right: 48, bottom: 16, containLabel: true },
    xAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: list.map((r) => r.reasonCategory),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#334155', fontSize: 11.5, fontWeight: 600 }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 22,
        data: list.map((r) => Number(r.count || 0)),
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#fca5a5' },
            { offset: 1, color: '#ef4444' }
          ])
        },
        label: { show: true, position: 'right', fontSize: 11, fontWeight: 700, color: '#dc2626', formatter: '{c} đơn' }
      }
    ]
  }
})

const sizeAbsorptionChartOption = computed(() => {
  const list = report.value.sizeStats || []
  return {
    animationDuration: 500,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const idx = params?.[0]?.dataIndex ?? 0
        const s = list[idx]
        if (!s) return ''
        return `<strong>EU ${s.sizeValue}</strong>${s.goldenSize ? ' (Size Vàng)' : ''}<br/>Đã bán: <b>${s.quantitySold} đôi (${s.sharePercent}%)</b><br/>Tồn kho: <b>${s.currentStock}</b><br/>Doanh thu: <b>${formatCurrency(s.revenue)}</b>`
      }
    },
    grid: { top: 18, left: 36, right: 16, bottom: 24 },
    xAxis: {
      type: 'category',
      data: list.map((s) => `EU ${s.sizeValue}`),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#475569', fontSize: 11, fontWeight: 600 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 30,
        data: list.map((s) => ({
          value: Number(s.quantitySold || 0),
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: s.goldenSize ? '#f59e0b' : themeColor.value
          }
        })),
        label: { show: true, position: 'top', fontSize: 10.5, fontWeight: 700, color: '#475569' }
      }
    ]
  }
})

const brandDonutChartOption = computed(() => {
  const list = report.value.brandStats || []
  const palette = [themeColor.value, '#10b981', '#f59e0b', '#f97316', '#8b5cf6', '#ec4899', '#14b8a6']
  return {
    animationDuration: 500,
    tooltip: {
      trigger: 'item',
      formatter: (p) => `<strong>${p.name}</strong><br/>${formatCurrency(p.value)} (${p.percent}%)`
    },
    legend: {
      orient: 'vertical',
      right: 4,
      top: 'middle',
      icon: 'circle',
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: '#475569', fontSize: 11.5 }
    },
    series: [
      {
        type: 'pie',
        radius: ['48%', '74%'],
        center: ['36%', '50%'],
        itemStyle: { borderRadius: 6, borderColor: '#ffffff', borderWidth: 2 },
        label: { show: false },
        data: list.map((b, i) => ({
          name: b.brandName,
          value: Number(b.revenue || 0),
          itemStyle: { color: palette[i % palette.length] }
        }))
      }
    ]
  }
})

function cashflowSummary({ columns, data }) {
  const sums = []
  columns.forEach((col, index) => {
    if (index === 0) {
      sums[index] = 'TỔNG CỘNG'
      return
    }
    const vals = data.map((row) => Number(row[col.property] || 0))
    const total = vals.reduce((a, b) => a + b, 0)
    sums[index] = index === 1 ? `${total} đơn` : formatCurrency(total)
  })
  return sums
}

function buildParams() {
  const f = filters.value
  return {
    groupBy: f.groupBy || 'DAY',
    fromDate: f.fromDate || undefined,
    toDate: f.toDate || undefined,
    channel: f.channel || undefined,
    orderStatus: f.orderStatus || undefined,
    paymentMethod: f.paymentMethod || undefined,
    brandId: f.brandId || undefined,
    categoryId: f.categoryId || undefined
  }
}

async function loadMeta() {
  const [bRes, cRes] = await Promise.all([
    brandService.getAllBrands(),
    categoryService.getAllCategories()
  ])
  brands.value = bRes.data || []
  categories.value = cRes.data || []
}

async function loadReport() {
  loading.value = true
  try {
    const res = await reportService.getSummaryReport(buildParams())
    report.value = res.data || report.value
  } finally {
    loading.value = false
  }
}

async function exportExcel() {
  exporting.value = true
  try {
    const res = await reportService.exportReportExcel(buildParams())
    const raw = res instanceof Blob ? res : (res?.data ?? res)
    const blob = new Blob([raw], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `ADHA_BangKeKeToan_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('Đã xuất Bảng kê Kế toán Excel (5 Sheets) thành công!')
  } catch {
    ElMessage.error('Không thể xuất file Excel')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadMeta()
  loadReport()
})
</script>

<style scoped lang="scss">
.reports-workspace {
  gap: 20px;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.sum-box {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: border-color 0.16s ease;

  &:hover {
    border-color: #a3a3a3;
  }

  &.primary {
    background: var(--vea-theme, #409eff);
    border-color: var(--vea-theme, #409eff);
    .sum-label { color: rgba(255, 255, 255, 0.9); }
    .sum-val { color: #ffffff; }
    .sum-sub { color: rgba(255, 255, 255, 0.82); }
  }

  .sum-label { font-size: 11px; font-weight: 800; color: #737373; text-transform: uppercase; letter-spacing: 0.05em; }
  .sum-val { font-size: 22px; font-weight: 900; color: #303133; font-variant-numeric: tabular-nums; white-space: nowrap; }
  .sum-sub { font-size: 12px; color: #737373; line-height: 1.4; }
}

.reports-surface {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 16px 24px 24px;
}

.report-tabs {
  :deep(.el-tabs__item) {
    font-size: 13px;
    font-weight: 700;
    color: #525252;

    &.is-active {
      color: #303133;
      font-weight: 800;
    }
  }

  :deep(.el-tabs__active-bar) {
    background-color: var(--vea-theme, #409eff);
    height: 2.5px;
  }

  :deep(.el-tabs__content) {
    padding-top: 8px;
  }
}

.tab-intro {
  font-size: 12.5px;
  color: #737373;
  margin-bottom: 14px;
}

.sub-heading {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: #303133;
}

.mono-tag {
  background: #f5f5f5;
  border: 1px solid #d4d4d4;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 800;
  color: #303133;
}

.text-red { color: #dc2626; }
.text-green { color: #047857; }
.text-blue { color: #1d4ed8; }

.ref-link {
  color: #2563eb;
  font-weight: 700;
  text-decoration: none;
  &:hover { text-decoration: underline; }
}

/* Channels */
.channel-compare-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.ch-detail-card {
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 18px 20px;
  background: #fafafa;

  &.is-pos { border-top: 3px solid var(--vea-theme, #409eff); }
  &.is-online { border-top: 3px solid #ea580c; }
}

.ch-card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #303133;
}

.share-pill {
  padding: 2px 8px;
  border-radius: 4px;
  background: #e5e5e5;
  font-size: 11px;
  font-weight: 800;
  color: #262626;
}

.ch-big-money {
  font-size: 24px;
  font-weight: 900;
  color: #303133;
  margin: 10px 0 14px;
  font-variant-numeric: tabular-nums;
}

.ch-kv-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;

  div {
    display: flex;
    justify-content: space-between;
    color: #525252;
    padding-bottom: 5px;
    border-bottom: 1px dashed #e5e5e5;

    &:last-child { border-bottom: none; padding-bottom: 0; }
  }
}

/* Cancel Reason Bars */
.reason-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reason-item {
  background: #fafafa;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 12px 14px;
}

.reason-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12.5px;
  margin-bottom: 7px;
  color: #262626;
}

.reason-track {
  height: 8px;
  background: #e5e5e5;
  border-radius: 999px;
  overflow: hidden;
}

.reason-fill {
  height: 100%;
  background: #dc2626;
  border-radius: 999px;
}

/* Size Insights */
.size-insight-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.insight-box {
  background: #fafafa;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 14px 18px;

  &.golden {
    background: #fffbeb;
    border-color: #fcd34d;
  }

  .ins-label { font-size: 11.5px; font-weight: 800; text-transform: uppercase; color: #737373; letter-spacing: 0.03em; }
  .ins-val { font-size: 19px; font-weight: 900; color: #303133; margin-top: 6px; }
}

.gold-badge {
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: 3px;
  background: #f59e0b;
  color: #ffffff;
  font-size: 10px;
  font-weight: 800;
}

.color-swatch {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid #d4d4d4;
  margin-right: 8px;
  vertical-align: middle;
}

.top-emp-badge {
  margin-left: 8px;
  padding: 2px 6px;
  border-radius: 3px;
  background: var(--vea-theme, #409eff);
  color: #ffffff;
  font-size: 10px;
  font-weight: 800;
}

.report-chart-card {
  background: #fafbfc;
  border: 1px solid #e6ebf5;
  border-radius: 6px;
  padding: 12px 14px;
  margin-bottom: 12px;
}

.rc-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #475569;
  margin-bottom: 6px;
}

.report-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.report-period-chip {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--vea-theme-light-9, #ecf5ff);
  color: var(--vea-theme, #409eff);
  border: 1px solid var(--vea-theme-light-7, #c6e2ff);
  white-space: nowrap;
}

.quick-time-group {
  display: inline-flex;
  background: #f4f6f9;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  padding: 2px;
  gap: 2px;
}

.qt-btn {
  border: none;
  background: transparent;
  color: #606266;
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 600;
  padding: 4px 9px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.14s ease;
  white-space: nowrap;

  &:hover {
    color: var(--vea-theme, #409eff);
  }

  &.active {
    background: #ffffff;
    color: var(--vea-theme, #409eff);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
  }
}

.filter-count-dot {
  margin-left: 4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--vea-theme, #409eff);
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

@media (min-width: 1025px) {
  .summary-strip {
    flex-shrink: 0;
    gap: 10px;
  }

  .sum-box {
    padding: 10px 14px;
    gap: 2px;

    .sum-val {
      font-size: 17px;
    }
  }

  .reports-surface {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .report-tabs {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    :deep(.el-tabs__header) {
      flex-shrink: 0;
      margin-bottom: 10px;
    }

    :deep(.el-tabs__content) {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    :deep(.el-tab-pane) {
      flex: 1;
      min-height: 0;
      height: 100%;
      overflow-y: auto;
      padding-right: 4px;
    }
  }
}

@media (max-width: 1200px) {
  .summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .summary-strip,
  .channel-compare-cards,
  .size-insight-row {
    grid-template-columns: 1fr;
  }
}
</style>
