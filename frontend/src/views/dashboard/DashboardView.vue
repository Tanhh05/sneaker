<template>
  <div class="page-container exec-dashboard" v-loading="loading">
    <!-- Header -->
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">DASHBOARD ĐIỀU HÀNH – ADHA SNEAKER</h2>
        <p class="page-subtitle">
          Minh bạch dòng tiền thực thu vs tạm tính, kiểm soát tỷ lệ hủy/bom hàng, hành vi thanh toán &amp; cảnh báo tồn kho thời gian thực.
        </p>
      </div>
      <div class="header-actions">
        <el-button class="btn-soft" :loading="loading" @click="loadDashboard">
          <el-icon><Refresh /></el-icon>
          <span>Cập nhật</span>
        </el-button>
        <el-button class="btn-soft" @click="$router.push('/admin/reports')">
          <el-icon><DataAnalysis /></el-icon>
          <span>Báo cáo chi tiết</span>
        </el-button>
        <el-button type="primary" class="btn-dark" @click="$router.push('/pos')">
          <el-icon><ShoppingCart /></el-icon>
          <span>Mở POS</span>
        </el-button>
      </div>
    </div>

    <!-- Shared Multidimensional Filter Bar -->
    <AnalyticsFilterBar
      v-model="filters"
      :brands="brands"
      :categories="categories"
      @change="loadDashboard"
    />

    <!-- III.1: 6 Executive KPI Cards -->
    <div class="kpi-exec-grid">
      <!-- 1. Doanh thu thực nhận -->
      <div class="kpi-card kpi-hero">
        <div class="kpi-top">
          <span class="kpi-label">DOANH THU THỰC NHẬN</span>
          <span
            v-if="data.revenueGrowthPercent !== null && data.revenueGrowthPercent !== undefined"
            class="growth-pill"
            :class="data.revenueGrowthPercent >= 0 ? 'up' : 'down'"
          >
            {{ data.revenueGrowthPercent >= 0 ? '+' : '' }}{{ data.revenueGrowthPercent }}% vs kỳ trước
          </span>
          <span v-else class="live-pill">THỰC THU</span>
        </div>
        <div class="kpi-val">{{ formatCurrency(data.actualRevenue) }}</div>
        <div class="kpi-sub">{{ data.completedOrderCount }} đơn hoàn thành (tiền đã về túi)</div>
      </div>

      <!-- 2. Doanh thu tạm tính -->
      <div class="kpi-card kpi-provisional">
        <div class="kpi-top">
          <span class="kpi-label">DOANH THU TẠM TÍNH</span>
          <span class="tag-amber">ĐANG ĐI ĐƯỜNG</span>
        </div>
        <div class="kpi-val">{{ formatCurrency(data.provisionalRevenue) }}</div>
        <div class="kpi-sub">{{ data.provisionalOrderCount }} đơn chờ xử lý / đang giao COD</div>
      </div>

      <!-- 3. Giá trị đơn hủy / Bom hàng -->
      <div class="kpi-card kpi-cancelled">
        <div class="kpi-top">
          <span class="kpi-label">ĐƠN HỦY / BOM HÀNG</span>
          <span class="tag-red">THẤT THOÁT</span>
        </div>
        <div class="kpi-val">{{ formatCurrency(data.cancelledValue) }}</div>
        <div class="kpi-sub">{{ data.cancelledOrderCount }} đơn không thành công</div>
      </div>

      <!-- 4. AOV -->
      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-label">GIÁ TRỊ TB / ĐƠN (AOV)</span>
        </div>
        <div class="kpi-val">{{ formatCurrency(data.aov) }}</div>
        <div class="kpi-sub">Doanh thu thực nhận / Số đơn hoàn thành</div>
      </div>

      <!-- 5. Tỷ lệ giao hàng thành công -->
      <div class="kpi-card kpi-success">
        <div class="kpi-top">
          <span class="kpi-label">TỶ LỆ GIAO THÀNH CÔNG</span>
        </div>
        <div class="kpi-val">{{ data.deliverySuccessRate || 0 }}%</div>
        <div class="kpi-sub">{{ data.completedOrderCount }} / {{ data.totalOrders }} tổng đơn đặt</div>
      </div>

      <!-- 6. Tỷ lệ hủy đơn (Cảnh báo đỏ > 10%) -->
      <div class="kpi-card" :class="{ 'kpi-danger-alert': (data.cancellationRate || 0) > 10 }">
        <div class="kpi-top">
          <span class="kpi-label">TỶ LỆ HỦY ĐƠN</span>
          <span v-if="(data.cancellationRate || 0) > 10" class="alert-badge-red">CẢNH BÁO &gt; 10%</span>
        </div>
        <div class="kpi-val" :class="{ 'text-red': (data.cancellationRate || 0) > 10 }">
          {{ data.cancellationRate || 0 }}%
        </div>
        <div class="kpi-sub">
          {{ (data.cancellationRate || 0) > 10 ? 'Vượt ngưỡng an toàn 10% — cần rà soát giao vận!' : 'Trong ngưỡng kiểm soát an toàn (≤ 10%)' }}
        </div>
      </div>
    </div>

    <!-- III.2: Charts Row 1 (Trend Combo Chart + Payment Donut Chart) -->
    <el-row :gutter="20" class="equal-row">
      <!-- Chart 1: Combo Bar + Line (Revenue & Order Growth) -->
      <el-col :xs="24" :lg="16">
        <div class="panel-card">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">1. Xu hướng Doanh thu &amp; Tăng trưởng đơn hàng</h3>
              <p class="panel-sub">Cột: Doanh thu thực tế &middot; Đường nối: Số lượng đơn hàng phát sinh theo kỳ</p>
            </div>
            <el-radio-group v-model="trendDisplayMode" size="small">
              <el-radio-button value="TOTAL">Tổng doanh thu</el-radio-button>
              <el-radio-button value="SPLIT">Tách Tiền mặt / CK / COD</el-radio-button>
            </el-radio-group>
          </div>

          <div v-if="!trendSeries.length" class="empty-wrap">
            <el-empty description="Chưa có dữ liệu doanh thu trong kỳ lọc" :image-size="72" />
          </div>
          <div v-else class="combo-chart-area">
            <!-- Legend -->
            <div class="chart-legend">
              <template v-if="trendDisplayMode === 'TOTAL'">
                <span class="leg-item"><i class="sw sw-dark" /> Doanh thu thực nhận</span>
              </template>
              <template v-else>
                <span class="leg-item"><i class="sw sw-bank" /> Chuyển khoản (SePay)</span>
                <span class="leg-item"><i class="sw sw-cash" /> Tiền mặt (CASH)</span>
                <span class="leg-item"><i class="sw sw-cod" /> Thu hộ (COD)</span>
              </template>
              <span class="leg-item"><i class="sw-line" /> Số đơn hàng (Đường)</span>
            </div>

            <!-- Interactive Bar + SVG Line Overlay -->
            <div class="combo-canvas">
              <div class="bars-layer">
                <div
                  v-for="(pt, idx) in trendSeries"
                  :key="pt.period + idx"
                  class="bar-col"
                >
                  <el-tooltip placement="top">
                    <template #content>
                      <div style="font-size: 12px; line-height: 1.5">
                        <strong>Kỳ {{ pt.period }}</strong><br />
                        Tổng doanh thu: <strong>{{ formatCurrency(pt.revenue) }}</strong><br />
                        Số đơn: <strong>{{ pt.orderCount }}</strong> ({{ pt.quantitySold }} đôi)<br />
                        Tiền mặt: {{ formatCurrency(pt.cashRevenue) }}<br />
                        Chuyển khoản: {{ formatCurrency(pt.bankTransferRevenue) }}<br />
                        COD: {{ formatCurrency(pt.codRevenue) }}
                      </div>
                    </template>
                    <div class="bar-col-inner">
                      <div class="bar-top-label">{{ formatShortMoney(pt.revenue) }}</div>
                      <div class="bar-track">
                        <div
                          v-if="trendDisplayMode === 'TOTAL'"
                          class="bar-solid"
                          :style="{ height: pctOfMax(pt.revenue) + '%' }"
                        />
                        <div
                          v-else
                          class="bar-stacked"
                          :style="{ height: pctOfMax(pt.revenue) + '%' }"
                        >
                          <div
                            class="seg seg-bank"
                            :style="{ flex: Number(pt.bankTransferRevenue || 0) }"
                          />
                          <div
                            class="seg seg-cash"
                            :style="{ flex: Number(pt.cashRevenue || 0) }"
                          />
                          <div
                            class="seg seg-cod"
                            :style="{ flex: Number(pt.codRevenue || 0) }"
                          />
                        </div>
                      </div>
                      <div class="bar-x-label">{{ pt.period }}</div>
                      <div class="bar-order-badge">{{ pt.orderCount }} đơn</div>
                    </div>
                  </el-tooltip>
                </div>
              </div>

              <!-- SVG Order Trend Line Overlay -->
              <svg
                v-if="trendSeries.length > 1"
                class="order-line-svg"
                viewBox="0 0 1000 180"
                preserveAspectRatio="none"
              >
                <polyline
                  :points="orderLinePoints"
                  fill="none"
                  stroke="#ea580c"
                  stroke-width="3"
                  stroke-linejoin="round"
                  stroke-linecap="round"
                />
                <circle
                  v-for="(pt, i) in orderLineDots"
                  :key="i"
                  :cx="pt.x"
                  :cy="pt.y"
                  r="5"
                  fill="#ffffff"
                  stroke="#ea580c"
                  stroke-width="2.5"
                />
              </svg>
            </div>
          </div>
        </div>
      </el-col>

      <!-- Chart 2: Payment Method Donut -->
      <el-col :xs="24" :lg="8">
        <div class="panel-card">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">2. Cơ cấu Phương thức thanh toán</h3>
              <p class="panel-sub">Tỷ trọng dòng tiền thu về: SePay vs Tiền mặt vs COD</p>
            </div>
          </div>

          <div class="donut-layout">
            <div class="donut-svg-wrap">
              <svg viewBox="0 0 160 160" class="donut-svg">
                <circle cx="80" cy="80" r="58" fill="none" stroke="#f1f5f9" stroke-width="22" />
                <circle
                  v-for="seg in donutSegments"
                  :key="seg.key"
                  cx="80"
                  cy="80"
                  r="58"
                  fill="none"
                  :stroke="seg.color"
                  stroke-width="22"
                  :stroke-dasharray="`${seg.dash} ${donutCircumference}`"
                  :stroke-dashoffset="seg.offset"
                  transform="rotate(-90 80 80)"
                />
              </svg>
              <div class="donut-center">
                <div class="dc-label">Tổng thu</div>
                <div class="dc-val">{{ formatShortMoney(paymentTotalAmount) }}</div>
              </div>
            </div>

            <div class="donut-legend">
              <div
                v-for="pm in data.paymentMethodStats || []"
                :key="pm.paymentMethod"
                class="pm-row"
              >
                <div class="pm-left">
                  <span class="pm-dot" :style="{ background: paymentColor(pm.paymentMethod) }" />
                  <div>
                    <div class="pm-name">{{ pm.label }}</div>
                    <div class="pm-sub">{{ pm.orderCount }} đơn hàng</div>
                  </div>
                </div>
                <div class="pm-right">
                  <div class="pm-pct">{{ pm.percentage }}%</div>
                  <div class="pm-amt">{{ formatCurrency(pm.amount) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- III.2: Charts Row 2 (POS vs Online + Conversion Funnel) -->
    <el-row :gutter="20" class="equal-row">
      <!-- Chart 3: POS vs Online Channel Comparison -->
      <el-col :xs="24" :lg="12">
        <div class="panel-card">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">3. So sánh Hiệu quả 2 Kênh bán (POS vs Online)</h3>
              <p class="panel-sub">Đánh giá doanh số, tỷ lệ thành công và rủi ro hủy giữa bán tại quầy &amp; online</p>
            </div>
          </div>

          <div class="channel-cards-grid">
            <div
              v-for="ch in data.channelComparison || []"
              :key="ch.channel"
              class="ch-card"
              :class="ch.channel === 'POS' ? 'ch-pos' : 'ch-online'"
            >
              <div class="ch-head">
                <span class="ch-badge">{{ ch.channel === 'POS' ? 'TẠI QUẦY (POS)' : 'ONLINE STORE' }}</span>
                <span class="ch-share">{{ ch.revenueSharePercent }}% doanh thu</span>
              </div>
              <div class="ch-revenue">{{ formatCurrency(ch.actualRevenue) }}</div>
              <div class="ch-metrics">
                <div><span>Tổng đơn:</span><strong>{{ ch.totalOrders }}</strong></div>
                <div><span>Thành công:</span><strong class="text-green">{{ ch.completedOrders }} ({{ ch.successRate }}%)</strong></div>
                <div><span>Hủy / Hoàn:</span><strong class="text-red">{{ ch.cancelledOrders }} ({{ ch.cancellationRate }}%)</strong></div>
                <div><span>AOV:</span><strong>{{ formatCurrency(ch.aov) }}</strong></div>
              </div>
            </div>
          </div>

          <!-- Proportional Split Bar -->
          <div class="channel-ratio-bar">
            <div
              class="ratio-pos"
              :style="{ width: posSharePercent + '%' }"
              :title="`POS: ${posSharePercent}%`"
            />
            <div
              class="ratio-online"
              :style="{ width: onlineSharePercent + '%' }"
              :title="`Online: ${onlineSharePercent}%`"
            />
          </div>
        </div>
      </el-col>

      <!-- Chart 4: Order Status Conversion Funnel -->
      <el-col :xs="24" :lg="12">
        <div class="panel-card">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">4. Phễu chuyển đổi Trạng thái đơn hàng</h3>
              <p class="panel-sub">Đặt đơn ➔ Xác nhận ➔ Giao hàng ➔ Thành công &amp; Thu tiền (kèm nhánh rơi rụng Hủy)</p>
            </div>
          </div>

          <div class="funnel-list">
            <div
              v-for="st in data.orderFunnel || []"
              :key="st.stageKey"
              class="funnel-row"
              :class="{ 'is-dropoff': st.stageKey === 'CANCELLED' }"
            >
              <div class="funnel-meta">
                <span class="funnel-label">{{ st.label }}</span>
                <span class="funnel-nums">
                  <strong>{{ st.count }}</strong> đơn ({{ st.rateFromTotal }}%) &middot; {{ formatCurrency(st.amount) }}
                </span>
              </div>
              <div class="funnel-track">
                <div
                  class="funnel-fill"
                  :class="`fill-${st.stageKey.toLowerCase()}`"
                  :style="{ width: Math.max(st.count > 0 ? 6 : 0, st.rateFromTotal) + '%' }"
                />
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- III.3: Top 5 Best-Selling Sneakers + Low Stock Alerts (< 5 pairs) -->
    <el-row :gutter="20" class="equal-row">
      <el-col :xs="24" :lg="13">
        <div class="panel-card">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">Top 5 Đôi Sneaker bán chạy nhất</h3>
              <p class="panel-sub">Xếp hạng theo số đôi đã bán và doanh thu mang về trong kỳ</p>
            </div>
            <el-button size="small" @click="$router.push('/admin/reports')">Xem báo cáo biến thể →</el-button>
          </div>

          <el-table :data="data.topSellingProducts || []" stripe size="small" style="width: 100%">
            <template #empty>
              <el-empty description="Chưa có sản phẩm bán ra trong kỳ" :image-size="60" />
            </template>
            <el-table-column type="index" label="#" width="42" align="center" />
            <el-table-column label="Sản phẩm Sneaker" min-width="210">
              <template #default="{ row }">
                <div class="prod-cell" @click="$router.push(`/admin/products/${row.productId}`)">
                  <img v-if="row.thumbnailUrl" :src="row.thumbnailUrl" class="prod-thumb" alt="sneaker" />
                  <div v-else class="prod-thumb fallback">AD</div>
                  <div class="prod-info">
                    <div class="prod-name">{{ row.productName }}</div>
                    <div class="prod-sub">{{ row.productCode }} &middot; {{ row.brandName }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Đã bán" width="90" align="center">
              <template #default="{ row }">
                <strong>{{ row.quantitySold }}</strong> đôi
              </template>
            </el-table-column>
            <el-table-column label="Doanh thu" width="145" align="right">
              <template #default="{ row }">
                <strong class="money-red">{{ formatCurrency(row.revenue) }}</strong>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <el-col :xs="24" :lg="11">
        <div class="panel-card">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">Cảnh báo Tồn kho chạm đáy (&lt; 5 đôi)</h3>
              <p class="panel-sub">Các mẫu &amp; size sắp hết hàng cần báo Quản lý lên đơn nhập NCC</p>
            </div>
            <el-button size="small" type="warning" plain @click="$router.push('/admin/variants')">
              Quản lý tồn kho
            </el-button>
          </div>

          <el-table :data="data.lowStockAlerts || []" size="small" style="width: 100%" max-height="320">
            <template #empty>
              <el-empty description="Tuyệt vời! Chưa có biến thể nào dưới 5 đôi" :image-size="60" />
            </template>
            <el-table-column label="Mẫu giày &amp; Size" min-width="195">
              <template #default="{ row }">
                <div class="prod-cell" @click="$router.push(`/admin/products/${row.productId}`)">
                  <img v-if="row.thumbnailUrl" :src="row.thumbnailUrl" class="prod-thumb" alt="sneaker" />
                  <div v-else class="prod-thumb fallback">AD</div>
                  <div class="prod-info">
                    <div class="prod-name">{{ row.productName }}</div>
                    <div class="prod-sub">
                      <span class="size-chip">EU {{ row.sizeValue || '-' }}</span>
                      <span>{{ row.colorName || '-' }}</span>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Còn lại" width="92" align="center">
              <template #default="{ row }">
                <span class="stock-alert-pill" :class="row.availableQuantity === 0 ? 'critical' : 'warning'">
                  {{ row.availableQuantity === 0 ? 'Hết hàng' : `${row.availableQuantity} đôi` }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, ShoppingCart, DataAnalysis } from '@element-plus/icons-vue'
import { reportService } from '@/services/reportService'
import { brandService } from '@/services/brandService'
import { categoryService } from '@/services/categoryService'
import { formatCurrency } from '@/utils/formatters'
import AnalyticsFilterBar from '@/components/AnalyticsFilterBar.vue'

const loading = ref(false)
const trendDisplayMode = ref('TOTAL')
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

const data = ref({
  actualRevenue: 0,
  previousActualRevenue: 0,
  revenueGrowthPercent: null,
  provisionalRevenue: 0,
  provisionalOrderCount: 0,
  cancelledValue: 0,
  cancelledOrderCount: 0,
  completedOrderCount: 0,
  totalOrders: 0,
  aov: 0,
  deliverySuccessRate: 0,
  cancellationRate: 0,
  revenueTrend: [],
  paymentMethodStats: [],
  channelComparison: [],
  orderFunnel: [],
  topSellingProducts: [],
  lowStockAlerts: []
})

const trendSeries = computed(() => data.value.revenueTrend || [])

const maxTrendRevenue = computed(() => {
  const vals = trendSeries.value.map((x) => Number(x.revenue || 0))
  return Math.max(...vals, 1)
})

const maxTrendOrders = computed(() => {
  const vals = trendSeries.value.map((x) => Number(x.orderCount || 0))
  return Math.max(...vals, 1)
})

function pctOfMax(val) {
  const p = Math.round((Number(val || 0) / maxTrendRevenue.value) * 100)
  return Number(val || 0) > 0 ? Math.max(p, 8) : 0
}

const orderLineDots = computed(() => {
  const arr = trendSeries.value
  const n = arr.length
  if (n <= 1) return []
  return arr.map((pt, i) => {
    const x = ((i + 0.5) / n) * 1000
    const ratio = Number(pt.orderCount || 0) / maxTrendOrders.value
    const y = 160 - ratio * 135
    return { x: Math.round(x), y: Math.round(y) }
  })
})

const orderLinePoints = computed(() =>
  orderLineDots.value.map((d) => `${d.x},${d.y}`).join(' ')
)

// Donut Chart
const donutCircumference = 2 * Math.PI * 58
const paymentTotalAmount = computed(() =>
  (data.value.paymentMethodStats || []).reduce((s, p) => s + Number(p.amount || 0), 0)
)

function paymentColor(pm) {
  if (pm === 'BANK_TRANSFER') return '#2563eb'
  if (pm === 'CASH') return '#f59e0b'
  return '#10b981'
}

const donutSegments = computed(() => {
  const stats = data.value.paymentMethodStats || []
  const total = paymentTotalAmount.value
  if (total <= 0) return []
  let acc = 0
  return stats
    .filter((s) => Number(s.amount || 0) > 0)
    .map((s) => {
      const frac = Number(s.amount) / total
      const dash = frac * donutCircumference
      const offset = -acc * donutCircumference
      acc += frac
      return {
        key: s.paymentMethod,
        color: paymentColor(s.paymentMethod),
        dash,
        offset
      }
    })
})

const posSharePercent = computed(() => {
  const ch = (data.value.channelComparison || []).find((c) => c.channel === 'POS')
  return ch ? ch.revenueSharePercent : 0
})

const onlineSharePercent = computed(() => {
  const ch = (data.value.channelComparison || []).find((c) => c.channel === 'ONLINE')
  return ch ? ch.revenueSharePercent : 0
})

function formatShortMoney(val) {
  const n = Number(val || 0)
  if (n >= 1_000_000_000) return (n / 1_000_000_000).toFixed(1) + 'B'
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M'
  if (n >= 1_000) return Math.round(n / 1_000) + 'K'
  return String(n)
}

async function loadMeta() {
  const [bRes, cRes] = await Promise.all([
    brandService.getAllBrands(),
    categoryService.getAllCategories()
  ])
  brands.value = bRes.data || []
  categories.value = cRes.data || []
}

async function loadDashboard() {
  loading.value = true
  try {
    const f = filters.value
    const res = await reportService.getDashboard({
      groupBy: f.groupBy || 'DAY',
      fromDate: f.fromDate || undefined,
      toDate: f.toDate || undefined,
      channel: f.channel || undefined,
      orderStatus: f.orderStatus || undefined,
      paymentMethod: f.paymentMethod || undefined,
      brandId: f.brandId || undefined,
      categoryId: f.categoryId || undefined
    })
    data.value = res.data || data.value
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMeta()
  loadDashboard()
})
</script>

<style scoped lang="scss">
.exec-dashboard {
  gap: 20px;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.equal-row {
  display: flex;
  flex-wrap: wrap;
  row-gap: 20px;

  > .el-col {
    display: flex;
    flex-direction: column;
  }
}

/* 6 KPI Grid */
.kpi-exec-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.kpi-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;

  &:hover {
    border-color: #a3a3a3;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  }

  .kpi-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
  }

  .kpi-label {
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 0.06em;
    color: #737373;
  }

  .kpi-val {
    font-size: 24px;
    font-weight: 900;
    color: #000000;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }

  .kpi-sub {
    font-size: 12px;
    color: #737373;
    line-height: 1.4;
  }

  &.kpi-hero {
    background: #0a0a0a;
    border-color: #0a0a0a;
    color: #ffffff;

    &:hover {
      border-color: #000000;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
    }

    .kpi-label { color: #a3a3a3; }
    .kpi-val { color: #ffffff; }
    .kpi-sub { color: #a3a3a3; }
  }

  &.kpi-provisional {
    border-left: 3px solid #f59e0b;
    .kpi-val { color: #b45309; }
  }

  &.kpi-cancelled {
    border-left: 3px solid #ef4444;
    .kpi-val { color: #dc2626; }
  }

  &.kpi-success {
    border-left: 3px solid #10b981;
    .kpi-val { color: #047857; }
  }

  &.kpi-danger-alert {
    background: #fef2f2;
    border-color: #fca5a5;
    border-left: 3px solid #dc2626;
  }
}

.text-red { color: #dc2626 !important; }
.text-green { color: #047857 !important; }

.growth-pill {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10.5px;
  font-weight: 800;

  &.up { background: rgba(16, 185, 129, 0.2); color: #34d399; }
  &.down { background: rgba(239, 68, 68, 0.25); color: #fca5a5; }
}

.live-pill {
  padding: 2px 7px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.14);
  color: #e5e5e5;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.tag-amber {
  padding: 2px 7px;
  border-radius: 4px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #b45309;
  font-size: 10px;
  font-weight: 800;
}

.tag-red,
.alert-badge-red {
  padding: 2px 7px;
  border-radius: 4px;
  background: #dc2626;
  color: #ffffff;
  font-size: 10px;
  font-weight: 800;
}

/* Panels */
.panel-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 20px 22px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-title {
  margin: 0;
  font-size: 14px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  color: #000000;
}

.panel-sub {
  margin: 4px 0 0;
  font-size: 12px;
  color: #737373;
}

/* Combo Chart */
.chart-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12px;
  font-weight: 600;
  color: #525252;
  margin-bottom: 14px;
}

.leg-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.sw {
  width: 11px;
  height: 11px;
  border-radius: 2px;
  display: inline-block;

  &.sw-dark { background: #111111; }
  &.sw-bank { background: #2563eb; }
  &.sw-cash { background: #f59e0b; }
  &.sw-cod { background: #10b981; }
}

.sw-line {
  width: 18px;
  height: 3px;
  background: #ea580c;
  display: inline-block;
  border-radius: 2px;
}

.combo-canvas {
  position: relative;
  min-height: 255px;
  padding: 6px 8px 4px;
  background: repeating-linear-gradient(
    to bottom,
    transparent,
    transparent 44px,
    #f5f5f5 44px,
    #f5f5f5 45px
  );
  border-radius: 4px;
}

.bars-layer {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 14px;
  overflow-x: auto;
  padding-top: 10px;
}

.bar-col {
  flex: 1;
  min-width: 64px;
  max-width: 120px;
}

.bar-col-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.bar-top-label {
  font-size: 11px;
  font-weight: 800;
  color: #000000;
  font-variant-numeric: tabular-nums;
}

.bar-track {
  width: 38px;
  height: 170px;
  background: #f5f5f5;
  border-radius: 4px 4px 0 0;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.bar-solid {
  width: 100%;
  background: linear-gradient(180deg, #262626 0%, #000000 100%);
  border-radius: 4px 4px 0 0;
  transition: height 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}

.bar-stacked {
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 4px 4px 0 0;
  overflow: hidden;

  .seg-bank { background: #2563eb; }
  .seg-cash { background: #f59e0b; }
  .seg-cod { background: #10b981; }
}

.bar-x-label {
  font-size: 11px;
  font-weight: 700;
  color: #525252;
  white-space: nowrap;
}

.bar-order-badge {
  font-size: 10.5px;
  font-weight: 800;
  color: #ea580c;
  background: #fff7ed;
  padding: 1px 6px;
  border-radius: 3px;
}

.order-line-svg {
  position: absolute;
  top: 26px;
  left: 0;
  width: 100%;
  height: 170px;
  pointer-events: none;
}

/* Donut */
.donut-layout {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  flex: 1;
}

.donut-svg-wrap {
  position: relative;
  width: 168px;
  height: 168px;
}

.donut-svg {
  width: 100%;
  height: 100%;
}

.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  .dc-label { font-size: 11px; font-weight: 700; color: #737373; text-transform: uppercase; }
  .dc-val { font-size: 18px; font-weight: 900; color: #000000; margin-top: 2px; }
}

.donut-legend {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pm-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 5px;
}

.pm-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pm-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.pm-name { font-size: 12.5px; font-weight: 700; color: #000000; }
.pm-sub { font-size: 11px; color: #737373; }
.pm-right { text-align: right; }
.pm-pct { font-size: 13px; font-weight: 900; color: #000000; }
.pm-amt { font-size: 11.5px; color: #525252; font-variant-numeric: tabular-nums; }

/* Channels */
.channel-cards-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.ch-card {
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 16px;
  background: #fafafa;

  &.ch-pos { border-top: 3px solid #000000; }
  &.ch-online { border-top: 3px solid #ea580c; }
}

.ch-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.ch-share { color: #737373; font-weight: 700; }

.ch-revenue {
  font-size: 20px;
  font-weight: 900;
  color: #000000;
  margin: 8px 0 12px;
  font-variant-numeric: tabular-nums;
}

.ch-metrics {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12.5px;

  div {
    display: flex;
    justify-content: space-between;
    color: #525252;
    padding-bottom: 4px;
    border-bottom: 1px dashed #ebebeb;

    &:last-child { border-bottom: none; padding-bottom: 0; }
  }
}

.channel-ratio-bar {
  height: 10px;
  border-radius: 999px;
  background: #f0f0f0;
  overflow: hidden;
  display: flex;
  margin-top: auto;

  .ratio-pos { background: #111111; height: 100%; transition: width 0.3s ease; }
  .ratio-online { background: #ea580c; height: 100%; transition: width 0.3s ease; }
}

/* Funnel */
.funnel-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  justify-content: center;
}

.funnel-row {
  display: flex;
  flex-direction: column;
  gap: 5px;

  &.is-dropoff {
    margin-top: 6px;
    padding-top: 10px;
    border-top: 1px dashed #fca5a5;
  }
}

.funnel-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12.5px;
  color: #404040;
}

.funnel-label { font-weight: 700; color: #000000; }
.funnel-nums { font-variant-numeric: tabular-nums; }

.funnel-track {
  height: 11px;
  border-radius: 4px;
  background: #f5f5f5;
  overflow: hidden;
}

.funnel-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;

  &.fill-placed { background: #111111; }
  &.fill-confirmed { background: #2563eb; }
  &.fill-shipping { background: #d97706; }
  &.fill-completed { background: #059669; }
  &.fill-cancelled { background: #dc2626; }
}

/* Products & Low stock */
.prod-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.prod-thumb {
  width: 42px;
  height: 42px;
  border-radius: 5px;
  object-fit: cover;
  border: 1px solid #e5e5e5;
  flex-shrink: 0;

  &.fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f5f5;
    font-size: 11px;
    font-weight: 800;
    color: #a3a3a3;
  }
}

.prod-name {
  font-size: 13px;
  font-weight: 700;
  color: #000000;
}

.prod-sub {
  font-size: 11.5px;
  color: #737373;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}

.size-chip {
  background: #f5f5f5;
  border: 1px solid #e5e5e5;
  padding: 0 6px;
  border-radius: 3px;
  font-weight: 800;
  color: #000000;
}

.money-red { color: #dc2626; font-variant-numeric: tabular-nums; }

.stock-alert-pill {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 800;

  &.critical { background: #fef2f2; color: #dc2626; border: 1px solid #fca5a5; }
  &.warning { background: #fffbeb; color: #b45309; border: 1px solid #fcd34d; }
}

@media (max-width: 1180px) {
  .kpi-exec-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .kpi-exec-grid,
  .channel-cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
