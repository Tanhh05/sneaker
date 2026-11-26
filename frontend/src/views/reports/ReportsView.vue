<template>
  <div class="page-container reports-workspace" v-loading="loading">
    <!-- Header -->
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">BÁO CÁO THỐNG KÊ ĐA CHIỀU – ADHA SNEAKER</h2>
        <p class="page-subtitle">
          5 chuyên đề phân tích chuyên sâu: Doanh thu &amp; Dòng tiền, Kênh bán (POS vs Online), Tỷ lệ Hủy/Bom hàng, Biến thể Size/Màu &amp; Hiệu suất Nhân viên.
        </p>
      </div>
      <div class="header-actions">
        <el-button class="btn-soft" :loading="loading" @click="loadReport">
          <el-icon><Refresh /></el-icon>
          <span>Tổng hợp lại</span>
        </el-button>
        <el-button type="success" class="btn-excel" :loading="exporting" @click="exportExcel">
          <el-icon><Download /></el-icon>
          <span>Xuất Bảng kê Kế toán Excel (5 Sheets)</span>
        </el-button>
      </div>
    </div>

    <!-- Shared Multidimensional Filter Bar -->
    <AnalyticsFilterBar
      v-model="filters"
      :brands="brands"
      :categories="categories"
      @change="loadReport"
    />

    <!-- Financial Summary Strip -->
    <div class="summary-strip">
      <div class="sum-box primary">
        <span class="sum-label">Tổng doanh thu thực nhận</span>
        <strong class="sum-val">{{ formatCurrency(report.totalRevenue) }}</strong>
        <span class="sum-sub">{{ report.totalCompletedOrders }} đơn hoàn thành &middot; {{ report.totalQuantitySold }} đôi</span>
      </div>
      <div class="sum-box">
        <span class="sum-label">Tiền hàng trước thuế</span>
        <strong class="sum-val">{{ formatCurrency(report.totalPreTaxRevenue) }}</strong>
        <span class="sum-sub">Thuế VAT: {{ formatCurrency(report.totalVatAmount) }}</span>
      </div>
      <div class="sum-box">
        <span class="sum-label">Cơ cấu dòng tiền thực thu</span>
        <strong class="sum-val">{{ formatCurrency(report.totalBankTransferRevenue) }}</strong>
        <span class="sum-sub">CK SePay &middot; TM: {{ formatCurrency(report.totalCashRevenue) }} &middot; COD: {{ formatCurrency(report.totalCodRevenue) }}</span>
      </div>
      <div class="sum-box">
        <span class="sum-label">Tạm tính / Thất thoát hủy</span>
        <strong class="sum-val text-red">{{ formatCurrency(report.cancelledValue) }}</strong>
        <span class="sum-sub">Tỷ lệ hủy: {{ report.cancellationRate }}% &middot; Tạm tính: {{ formatCurrency(report.provisionalRevenue) }}</span>
      </div>
    </div>

    <!-- 5 Specialized Report Modules -->
    <div class="reports-surface">
      <el-tabs v-model="activeTab" class="report-tabs">
        <!-- MODULE 1: Doanh thu & Dòng tiền -->
        <el-tab-pane label="1. Doanh thu & Dòng tiền" name="cashflow">
          <div class="tab-intro">
            Bảng kê chi tiết theo chu kỳ đã chọn (đối soát tiền hàng trước thuế, VAT và 3 luồng tiền mặt / chuyển khoản SePay / thu hộ COD).
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
              <div class="reason-bars">
                <div v-for="r in report.cancelReasonStats || []" :key="r.reasonCategory" class="reason-item">
                  <div class="reason-top">
                    <span>{{ r.reasonCategory }}</span>
                    <strong>{{ r.count }} đơn ({{ r.percentage }}%) &middot; {{ formatCurrency(r.lostValue) }}</strong>
                  </div>
                  <div class="reason-track">
                    <div class="reason-fill" :style="{ width: Math.max(r.count > 0 ? 5 : 0, r.percentage) + '%' }" />
                  </div>
                </div>
              </div>
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
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import { reportService } from '@/services/reportService'
import { brandService } from '@/services/brandService'
import { categoryService } from '@/services/categoryService'
import { formatCurrency } from '@/utils/formatters'
import AnalyticsFilterBar from '@/components/AnalyticsFilterBar.vue'

const loading = ref(false)
const exporting = ref(false)
const activeTab = ref('cashflow')

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
    const blob = new Blob([res.data], {
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
    background: #0a0a0a;
    border-color: #0a0a0a;
    .sum-label { color: #a3a3a3; }
    .sum-val { color: #ffffff; }
    .sum-sub { color: #a3a3a3; }
  }

  .sum-label { font-size: 11px; font-weight: 800; color: #737373; text-transform: uppercase; letter-spacing: 0.05em; }
  .sum-val { font-size: 22px; font-weight: 900; color: #000000; font-variant-numeric: tabular-nums; white-space: nowrap; }
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
      color: #000000;
      font-weight: 800;
    }
  }

  :deep(.el-tabs__active-bar) {
    background-color: #000000;
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
  color: #000000;
}

.mono-tag {
  background: #f5f5f5;
  border: 1px solid #d4d4d4;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 800;
  color: #000000;
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

  &.is-pos { border-top: 3px solid #000000; }
  &.is-online { border-top: 3px solid #ea580c; }
}

.ch-card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #000000;
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
  color: #000000;
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
  .ins-val { font-size: 19px; font-weight: 900; color: #000000; margin-top: 6px; }
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
  background: #000000;
  color: #ffffff;
  font-size: 10px;
  font-weight: 800;
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
