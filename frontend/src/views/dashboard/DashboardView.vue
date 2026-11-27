<template>
  <div
    class="page-container exec-dashboard-viewport"
    :style="{ '--dash-header-h': settings.showTagsView ? '84px' : '50px' }"
    v-loading="loading"
  >
    <!-- 1. Compact Command Bar (Header + Quick Time Pills + Filter Popover + Actions) -->
    <div class="dash-topbar">
      <div class="topbar-left">
        <div class="title-line">
          <h2 class="dash-title">Dashboard Điều hành</h2>
          <span class="period-chip">{{ activePresetLabel }}</span>
        </div>
      </div>

      <div class="topbar-controls">
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

        <el-popover
          placement="bottom-end"
          :width="760"
          trigger="click"
          popper-class="dash-filter-popover"
        >
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
            :show-group-by="false"
            @change="onAdvancedFilterChange"
          />
        </el-popover>

        <div class="layout-switch" role="group" aria-label="Chế độ hiển thị">
          <el-tooltip content="Chế độ Tập trung (1 mục lớn + 3 mục thu gọn)" placement="bottom">
            <button
              type="button"
              class="ls-btn"
              :class="{ active: layoutMode === 'focus' }"
              @click="layoutMode = 'focus'"
            >
              <el-icon><FullScreen /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="Chế độ Lưới 2×2 (Xem đồng thời 4 mục trong 1 màn)" placement="bottom">
            <button
              type="button"
              class="ls-btn"
              :class="{ active: layoutMode === 'grid' }"
              @click="layoutMode = 'grid'"
            >
              <el-icon><Grid /></el-icon>
            </button>
          </el-tooltip>
        </div>

        <div class="topbar-divider" />

        <el-tooltip content="Làm mới dữ liệu" placement="bottom">
          <el-button size="small" class="tb-icon-btn" :loading="loading" @click="loadDashboard">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-tooltip>

        <el-tooltip content="Mở Báo cáo Thống kê chi tiết" placement="bottom">
          <el-button size="small" class="tb-icon-btn" @click="$router.push('/admin/reports')">
            <el-icon><DataAnalysis /></el-icon>
          </el-button>
        </el-tooltip>

        <el-button type="primary" size="small" @click="$router.push('/pos')">
          <el-icon><ShoppingCart /></el-icon>
          <span>POS</span>
        </el-button>
      </div>
    </div>

    <!-- 2. Single-Row 6-KPI Strip (Compact, clickable to focus relevant chart) -->
    <div class="kpi-strip-6">
      <el-tooltip :content="`${data.completedOrderCount} đơn hoàn thành (tiền đã về túi)`" placement="bottom" :show-after="250">
        <div
          class="kpi-mini"
          :class="{ 'is-focus-linked': layoutMode === 'focus' && activePanel === 'trend' }"
          @click="focusPanel('trend')"
        >
          <div class="km-icon tone-teal"><el-icon><Money /></el-icon></div>
          <div class="km-body">
            <div class="km-head">
              <span class="km-label">Thực nhận</span>
              <span
                v-if="data.revenueGrowthPercent !== null && data.revenueGrowthPercent !== undefined"
                class="km-badge"
                :class="data.revenueGrowthPercent >= 0 ? 'up' : 'down'"
              >
                {{ data.revenueGrowthPercent >= 0 ? '+' : '' }}{{ data.revenueGrowthPercent }}%
              </span>
            </div>
            <div class="km-val">{{ formatCurrency(data.actualRevenue) }}</div>
          </div>
        </div>
      </el-tooltip>

      <el-tooltip :content="`${data.provisionalOrderCount} đơn chờ xử lý / đang giao COD`" placement="bottom" :show-after="250">
        <div class="kpi-mini" @click="focusPanel('trend')">
          <div class="km-icon tone-blue"><el-icon><Clock /></el-icon></div>
          <div class="km-body">
            <div class="km-head">
              <span class="km-label">Tạm tính</span>
              <span class="km-badge amber">{{ data.provisionalOrderCount }} đơn</span>
            </div>
            <div class="km-val">{{ formatCurrency(data.provisionalRevenue) }}</div>
          </div>
        </div>
      </el-tooltip>

      <el-tooltip :content="`${data.cancelledOrderCount} đơn hủy / bom hàng không thành công`" placement="bottom" :show-after="250">
        <div class="kpi-mini" @click="focusPanel('channels')">
          <div class="km-icon tone-pink"><el-icon><CircleClose /></el-icon></div>
          <div class="km-body">
            <div class="km-head">
              <span class="km-label">Đơn hủy</span>
              <span class="km-badge red">{{ data.cancelledOrderCount }} đơn</span>
            </div>
            <div class="km-val text-red">{{ formatCurrency(data.cancelledValue) }}</div>
          </div>
        </div>
      </el-tooltip>

      <el-tooltip content="Giá trị trung bình mỗi đơn hoàn thành (AOV)" placement="bottom" :show-after="250">
        <div class="kpi-mini" @click="focusPanel('topProducts')">
          <div class="km-icon tone-green"><el-icon><TrendCharts /></el-icon></div>
          <div class="km-body">
            <div class="km-head">
              <span class="km-label">TB / Đơn (AOV)</span>
            </div>
            <div class="km-val">{{ formatCurrency(data.aov) }}</div>
          </div>
        </div>
      </el-tooltip>

      <el-tooltip :content="`${data.completedOrderCount} / ${data.totalOrders} tổng đơn đặt`" placement="bottom" :show-after="250">
        <div class="kpi-mini" @click="focusPanel('channels')">
          <div class="km-icon tone-primary"><el-icon><Checked /></el-icon></div>
          <div class="km-body">
            <div class="km-head">
              <span class="km-label">Giao thành công</span>
            </div>
            <div class="km-val text-green">{{ data.deliverySuccessRate || 0 }}%</div>
          </div>
        </div>
      </el-tooltip>

      <el-tooltip
        :content="(data.cancellationRate || 0) > 10 ? 'Vượt ngưỡng 10% — cần rà soát giao vận!' : 'Trong ngưỡng kiểm soát an toàn (≤ 10%)'"
        placement="bottom"
        :show-after="250"
      >
        <div
          class="kpi-mini"
          :class="{ 'is-danger': (data.cancellationRate || 0) > 10 }"
          @click="focusPanel('channels')"
        >
          <div class="km-icon" :class="(data.cancellationRate || 0) > 10 ? 'tone-pink' : 'tone-amber'">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="km-body">
            <div class="km-head">
              <span class="km-label">Tỷ lệ hủy</span>
              <span v-if="(data.cancellationRate || 0) > 10" class="km-badge red">&gt;10%</span>
            </div>
            <div class="km-val" :class="{ 'text-red': (data.cancellationRate || 0) > 10 }">
              {{ data.cancellationRate || 0 }}%
            </div>
          </div>
        </div>
      </el-tooltip>
    </div>

    <!-- 3. Single-Viewport Interactive Stage: Focus Mode (1 Expanded + 3 Minimized) OR 2x2 Grid -->
    <div class="dash-stage" :class="layoutMode === 'focus' ? 'mode-focus' : 'mode-grid'">
      <!-- ========== FOCUS MODE ========== -->
      <template v-if="layoutMode === 'focus'">
        <!-- LEFT: EXPANDED ACTIVE PANEL -->
        <div class="focus-hero-card">
          <!-- PANEL 1: REVENUE TREND (APACHE ECHARTS DUAL-AXIS COMBO) -->
          <template v-if="activePanel === 'trend'">
            <div class="panel-head">
              <div class="ph-left">
                <span class="ph-icon tone-primary"><el-icon><TrendCharts /></el-icon></span>
                <div>
                  <h3 class="panel-title">Xu hướng Doanh thu &amp; Tăng trưởng đơn hàng</h3>
                  <p class="panel-sub">Biểu đồ kết hợp: Cột doanh thu thực tế (VNĐ) &amp; Đường cong số lượng đơn hàng</p>
                </div>
              </div>
              <div class="ph-right">
                <el-radio-group v-model="trendDisplayMode" size="small">
                  <el-radio-button value="TOTAL">Tổng doanh thu</el-radio-button>
                  <el-radio-button value="SPLIT">Tiền mặt / CK / COD</el-radio-button>
                </el-radio-group>
                <el-tooltip content="Thu gọn về lưới 2×2" placement="top">
                  <el-button size="small" class="vea-icon-btn vea-icon-more hide-mobile-grid-btn" @click="layoutMode = 'grid'">
                    <el-icon><Grid /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>

            <div class="panel-body-fill panel-chart-host">
              <div v-if="!trendSeries.length" class="empty-fill">
                <el-empty description="Chưa có dữ liệu doanh thu trong kỳ lọc" :image-size="72" />
              </div>
              <BaseChart v-else :option="trendChartOption" height="100%" />
            </div>
          </template>

          <!-- PANEL 2: CHANNELS COMPARISON (ECHARTS DONUT + METRICS) -->
          <template v-else-if="activePanel === 'channels'">
            <div class="panel-head">
              <div class="ph-left">
                <span class="ph-icon tone-teal"><el-icon><Shop /></el-icon></span>
                <div>
                  <h3 class="panel-title">So sánh Hiệu quả 2 Kênh bán (POS vs Online Store)</h3>
                  <p class="panel-sub">Tỷ trọng doanh thu thực nhận, tỷ lệ hoàn thành &amp; tỷ lệ hủy giữa 2 kênh</p>
                </div>
              </div>
              <div class="ph-right">
                <el-button size="small" @click="$router.push('/admin/orders')">Danh sách đơn →</el-button>
                <el-tooltip content="Thu gọn về lưới 2×2" placement="top">
                  <el-button size="small" class="vea-icon-btn vea-icon-more hide-mobile-grid-btn" @click="layoutMode = 'grid'">
                    <el-icon><Grid /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>

            <div class="panel-body-fill channels-split-layout">
              <div class="ch-donut-pane">
                <BaseChart :option="channelDonutOption" height="100%" />
              </div>
              <div class="ch-detail-pane">
                <div class="channel-cards-grid">
                  <div
                    v-for="ch in data.channelComparison || []"
                    :key="ch.channel"
                    class="ch-card"
                    :class="ch.channel === 'POS' ? 'ch-pos' : 'ch-online'"
                  >
                    <div class="ch-head">
                      <span class="ch-badge">{{ ch.channel === 'POS' ? 'TẠI QUẦY (POS)' : 'ONLINE STORE' }}</span>
                      <span class="ch-share-pill">{{ ch.revenueSharePercent }}%</span>
                    </div>
                    <div class="ch-revenue">{{ formatCurrency(ch.actualRevenue) }}</div>
                    <div class="ch-metrics">
                      <div><span>Tổng đơn</span><strong>{{ ch.totalOrders }}</strong></div>
                      <div><span>Thành công</span><strong class="text-green">{{ ch.completedOrders }} ({{ ch.successRate }}%)</strong></div>
                      <div><span>Hủy / Hoàn</span><strong class="text-red">{{ ch.cancelledOrders }} ({{ ch.cancellationRate }}%)</strong></div>
                      <div><span>AOV</span><strong>{{ formatCurrency(ch.aov) }}</strong></div>
                    </div>
                  </div>
                </div>
                <div class="ch-rate-chart-wrap">
                  <BaseChart :option="channelRateBarOption" height="100%" />
                </div>
              </div>
            </div>
          </template>

          <!-- PANEL 3: TOP 5 BEST-SELLING SNEAKERS (ECHARTS HORIZONTAL BAR + TABLE) -->
          <template v-else-if="activePanel === 'topProducts'">
            <div class="panel-head">
              <div class="ph-left">
                <span class="ph-icon tone-green"><el-icon><Trophy /></el-icon></span>
                <div>
                  <h3 class="panel-title">Top 5 Đôi Sneaker bán chạy nhất</h3>
                  <p class="panel-sub">Biểu đồ xếp hạng doanh thu &amp; chi tiết sản phẩm bán chạy trong kỳ</p>
                </div>
              </div>
              <div class="ph-right">
                <el-button size="small" @click="$router.push('/admin/reports')">Báo cáo biến thể →</el-button>
                <el-tooltip content="Thu gọn về lưới 2×2" placement="top">
                  <el-button size="small" class="vea-icon-btn vea-icon-more hide-mobile-grid-btn" @click="layoutMode = 'grid'">
                    <el-icon><Grid /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>

            <div class="panel-body-fill top-products-split">
              <div v-if="!(data.topSellingProducts || []).length" class="empty-fill">
                <el-empty description="Chưa có sản phẩm bán ra trong kỳ" :image-size="64" />
              </div>
              <template v-else>
                <div class="top-chart-pane">
                  <BaseChart :option="topProductsBarOption" height="100%" />
                </div>
                <div class="top-table-pane">
                  <el-table :data="data.topSellingProducts || []" stripe size="small" height="100%" style="width: 100%">
                    <el-table-column type="index" label="#" width="40" align="center" />
                    <el-table-column label="Sản phẩm Sneaker" min-width="190">
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
                    <el-table-column label="Đã bán" width="84" align="center">
                      <template #default="{ row }">
                        <span class="qty-pill"><strong>{{ row.quantitySold }}</strong> đôi</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="Doanh thu" width="130" align="right">
                      <template #default="{ row }">
                        <strong class="money-red">{{ formatCurrency(row.revenue) }}</strong>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </template>
            </div>
          </template>

          <!-- PANEL 4: LOW STOCK ALERTS (EXPANDED) -->
          <template v-else-if="activePanel === 'lowStock'">
            <div class="panel-head">
              <div class="ph-left">
                <span class="ph-icon tone-amber"><el-icon><Warning /></el-icon></span>
                <div>
                  <h3 class="panel-title">Cảnh báo Tồn kho chạm đáy (&lt; 5 đôi)</h3>
                  <p class="panel-sub">Các mẫu &amp; size sắp hết hàng cần báo Quản lý lên phiếu nhập NCC</p>
                </div>
              </div>
              <div class="ph-right">
                <el-button size="small" type="warning" plain @click="$router.push('/admin/variants')">
                  Quản lý tồn kho
                </el-button>
                <el-tooltip content="Thu gọn về lưới 2×2" placement="top">
                  <el-button size="small" class="vea-icon-btn vea-icon-more hide-mobile-grid-btn" @click="layoutMode = 'grid'">
                    <el-icon><Grid /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>

            <div class="panel-body-fill table-scroll-host">
              <el-table :data="data.lowStockAlerts || []" stripe size="small" height="100%" style="width: 100%">
                <template #empty>
                  <el-empty description="Tuyệt vời! Chưa có biến thể nào dưới 5 đôi" :image-size="64" />
                </template>
                <el-table-column label="Mẫu giày &amp; Biến thể" min-width="230">
                  <template #default="{ row }">
                    <div class="prod-cell" @click="$router.push(`/admin/products/${row.productId}`)">
                      <img v-if="row.thumbnailUrl" :src="row.thumbnailUrl" class="prod-thumb" alt="sneaker" />
                      <div v-else class="prod-thumb fallback">AD</div>
                      <div class="prod-info">
                        <div class="prod-name">{{ row.productName }}</div>
                        <div class="prod-sub">
                          <span class="size-chip">EU {{ row.sizeValue || '-' }}</span>
                          <span>{{ row.colorName || '-' }}</span>
                          <span v-if="row.sku">&middot; {{ row.sku }}</span>
                        </div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="Còn lại" width="110" align="center">
                  <template #default="{ row }">
                    <span class="stock-alert-pill" :class="row.availableQuantity === 0 ? 'critical' : 'warning'">
                      {{ row.availableQuantity === 0 ? 'Hết hàng' : `${row.availableQuantity} đôi` }}
                    </span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </div>

        <!-- RIGHT: 3 AUTOMATICALLY MINIMIZED DOCK CARDS (Click any card to expand it!) -->
        <div class="focus-mini-dock">
          <div
            v-for="panelKey in minimizedPanels"
            :key="panelKey"
            class="dock-card"
            role="button"
            tabindex="0"
            @click="focusPanel(panelKey)"
            @keyup.enter="focusPanel(panelKey)"
          >
            <div class="dock-head">
              <div class="dock-title-wrap">
                <span class="dock-icon" :class="panelMeta[panelKey].tone">
                  <el-icon><component :is="panelMeta[panelKey].icon" /></el-icon>
                </span>
                <span class="dock-title">{{ panelMeta[panelKey].title }}</span>
              </div>
              <span class="dock-expand-hint">
                <span>Mở</span>
                <el-icon><FullScreen /></el-icon>
              </span>
            </div>

            <!-- Live Mini ECharts Sparkline for Trend -->
            <div v-if="panelKey === 'trend'" class="dock-preview preview-trend">
              <div class="dock-echart-mini">
                <BaseChart v-if="trendSeries.length" :option="trendSparkOption" height="100%" />
                <span v-else class="dock-muted">Chưa có kỳ doanh thu</span>
              </div>
              <div class="dock-foot-line">
                <span>{{ trendSeries.length }} kỳ thống kê</span>
                <strong>{{ formatCurrency(data.actualRevenue) }}</strong>
              </div>
            </div>

            <div v-else-if="panelKey === 'channels'" class="dock-preview preview-channels">
              <div class="mini-ch-nums">
                <div class="mini-ch-item">
                  <span class="mini-ch-lbl"><i class="dot-pos" /> POS ({{ posSharePercent }}%)</span>
                  <strong>{{ formatShortMoney(posChannel?.actualRevenue || 0) }}</strong>
                </div>
                <div class="mini-ch-item">
                  <span class="mini-ch-lbl"><i class="dot-online" /> Online ({{ onlineSharePercent }}%)</span>
                  <strong>{{ formatShortMoney(onlineChannel?.actualRevenue || 0) }}</strong>
                </div>
              </div>
              <div class="channel-ratio-bar mini-bar">
                <div class="ratio-pos" :style="{ width: posSharePercent + '%' }" />
                <div class="ratio-online" :style="{ width: onlineSharePercent + '%' }" />
              </div>
              <div class="dock-foot-line">
                <span>POS: {{ posChannel?.totalOrders || 0 }} đơn</span>
                <span>Online: {{ onlineChannel?.totalOrders || 0 }} đơn</span>
              </div>
            </div>

            <div v-else-if="panelKey === 'topProducts'" class="dock-preview preview-top">
              <template v-if="topOneProduct">
                <div class="mini-prod-row">
                  <img v-if="topOneProduct.thumbnailUrl" :src="topOneProduct.thumbnailUrl" class="mini-thumb" alt="top" />
                  <div v-else class="mini-thumb fallback">#1</div>
                  <div class="mini-prod-meta">
                    <div class="mini-prod-name">{{ topOneProduct.productName }}</div>
                    <div class="mini-prod-sub">
                      Bán chạy #1 &middot; <strong>{{ topOneProduct.quantitySold }} đôi</strong>
                    </div>
                  </div>
                </div>
                <div class="dock-foot-line">
                  <span>Top {{ (data.topSellingProducts || []).length }} mẫu bán chạy</span>
                  <strong class="money-red">{{ formatCurrency(topOneProduct.revenue) }}</strong>
                </div>
              </template>
              <div v-else class="dock-muted">Chưa có sản phẩm bán ra trong kỳ</div>
            </div>

            <div v-else-if="panelKey === 'lowStock'" class="dock-preview preview-stock">
              <div class="mini-stock-summary">
                <span class="stock-big-num" :class="lowStockCount > 0 ? 'text-red' : 'text-green'">
                  {{ lowStockCount }}
                </span>
                <div class="stock-big-desc">
                  <strong>biến thể dưới 5 đôi</strong>
                  <span>{{ outOfStockCount }} biến thể đã hết hàng</span>
                </div>
              </div>
              <div v-if="firstLowStock" class="dock-foot-line">
                <span class="truncate-txt">{{ firstLowStock.productName }} (EU {{ firstLowStock.sizeValue }})</span>
                <span class="stock-alert-pill" :class="firstLowStock.availableQuantity === 0 ? 'critical' : 'warning'">
                  {{ firstLowStock.availableQuantity === 0 ? 'Hết' : `${firstLowStock.availableQuantity}đ` }}
                </span>
              </div>
              <div v-else class="dock-foot-line">
                <span class="text-green">Tồn kho ổn định</span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- ========== COMPACT 2×2 GRID MODE (ALL 4 IN 1 SCREEN WITH ECHARTS!) ========== -->
      <template v-else>
        <!-- 1. Trend Card -->
        <div class="grid-panel-card" @click="focusPanel('trend')">
          <div class="panel-head compact-head">
            <div class="ph-left">
              <span class="ph-icon tone-primary"><el-icon><TrendCharts /></el-icon></span>
              <h3 class="panel-title">Xu hướng Doanh thu &amp; Đơn hàng</h3>
            </div>
            <span class="dock-expand-hint"><span>Phóng to</span><el-icon><FullScreen /></el-icon></span>
          </div>
          <div class="grid-card-body">
            <div v-if="!trendSeries.length" class="empty-fill">
              <el-empty description="Chưa có dữ liệu" :image-size="48" />
            </div>
            <BaseChart v-else :option="trendGridOption" height="100%" />
          </div>
        </div>

        <!-- 2. Channels Card -->
        <div class="grid-panel-card" @click="focusPanel('channels')">
          <div class="panel-head compact-head">
            <div class="ph-left">
              <span class="ph-icon tone-teal"><el-icon><Shop /></el-icon></span>
              <h3 class="panel-title">So sánh POS vs Online</h3>
            </div>
            <span class="dock-expand-hint"><span>Phóng to</span><el-icon><FullScreen /></el-icon></span>
          </div>
          <div class="grid-card-body grid-channel-body">
            <BaseChart :option="channelGridDonutOption" height="100%" />
          </div>
        </div>

        <!-- 3. Top 5 Sneakers Card -->
        <div class="grid-panel-card" @click="focusPanel('topProducts')">
          <div class="panel-head compact-head">
            <div class="ph-left">
              <span class="ph-icon tone-green"><el-icon><Trophy /></el-icon></span>
              <h3 class="panel-title">Top 5 Sneaker bán chạy</h3>
            </div>
            <span class="dock-expand-hint"><span>Phóng to</span><el-icon><FullScreen /></el-icon></span>
          </div>
          <div class="grid-card-body compact-list">
            <div
              v-for="(row, idx) in (data.topSellingProducts || []).slice(0, 4)"
              :key="row.productId || idx"
              class="compact-row"
            >
              <span class="rank-num">#{{ idx + 1 }}</span>
              <span class="compact-name">{{ row.productName }}</span>
              <span class="compact-qty">{{ row.quantitySold }} đôi</span>
              <strong class="money-red compact-money">{{ formatShortMoney(row.revenue) }}</strong>
            </div>
            <div v-if="!(data.topSellingProducts || []).length" class="dock-muted">Chưa có sản phẩm bán ra</div>
          </div>
        </div>

        <!-- 4. Low Stock Card -->
        <div class="grid-panel-card" @click="focusPanel('lowStock')">
          <div class="panel-head compact-head">
            <div class="ph-left">
              <span class="ph-icon tone-amber"><el-icon><Warning /></el-icon></span>
              <h3 class="panel-title">Tồn kho chạm đáy (&lt; 5 đôi)</h3>
            </div>
            <span class="dock-expand-hint"><span>Phóng to</span><el-icon><FullScreen /></el-icon></span>
          </div>
          <div class="grid-card-body compact-list">
            <div
              v-for="(row, idx) in (data.lowStockAlerts || []).slice(0, 4)"
              :key="row.variantId || idx"
              class="compact-row"
            >
              <span class="size-chip">EU {{ row.sizeValue || '-' }}</span>
              <span class="compact-name">{{ row.productName }}</span>
              <span class="stock-alert-pill" :class="row.availableQuantity === 0 ? 'critical' : 'warning'">
                {{ row.availableQuantity === 0 ? 'Hết hàng' : `${row.availableQuantity} đôi` }}
              </span>
            </div>
            <div v-if="!(data.lowStockAlerts || []).length" class="dock-muted text-green">
              Tuyệt vời! Chưa có biến thể dưới 5 đôi
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import * as echarts from 'echarts'
import {
  Refresh,
  ShoppingCart,
  DataAnalysis,
  Money,
  Clock,
  CircleClose,
  TrendCharts,
  Checked,
  Warning,
  Filter,
  FullScreen,
  Grid,
  Shop,
  Trophy
} from '@element-plus/icons-vue'
import { reportService } from '@/services/reportService'
import { brandService } from '@/services/brandService'
import { categoryService } from '@/services/categoryService'
import { useAppSettingsStore } from '@/stores/appSettings'
import { formatCurrency } from '@/utils/formatters'
import AnalyticsFilterBar from '@/components/AnalyticsFilterBar.vue'
import BaseChart from '@/components/charts/BaseChart.vue'

const settings = useAppSettingsStore()

const loading = ref(false)
const trendDisplayMode = ref('TOTAL')
const layoutMode = ref('focus')
const activePanel = ref('trend')
const activeQuickPreset = ref('ALL')

const brands = ref([])
const categories = ref([])

const allPanels = ['trend', 'channels', 'topProducts', 'lowStock']

const panelMeta = {
  trend: { title: 'Xu hướng Doanh thu', icon: TrendCharts, tone: 'tone-primary' },
  channels: { title: 'So sánh POS vs Online', icon: Shop, tone: 'tone-teal' },
  topProducts: { title: 'Top 5 Sneaker bán chạy', icon: Trophy, tone: 'tone-green' },
  lowStock: { title: 'Cảnh báo Tồn kho (< 5)', icon: Warning, tone: 'tone-amber' }
}

const quickPresets = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'TODAY', label: 'Hôm nay' },
  { key: 'LAST_7', label: '7 ngày' },
  { key: 'THIS_MONTH', label: 'Tháng này' }
]

const minimizedPanels = computed(() => allPanels.filter((k) => k !== activePanel.value))

function focusPanel(key) {
  activePanel.value = key
  layoutMode.value = 'focus'
}

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

const activePresetLabel = computed(() => {
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
  loadDashboard()
}

function onAdvancedFilterChange() {
  activeQuickPreset.value = filters.value.fromDate ? 'CUSTOM' : 'ALL'
  loadDashboard()
}

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
const posChannel = computed(() => (data.value.channelComparison || []).find((c) => c.channel === 'POS'))
const onlineChannel = computed(() => (data.value.channelComparison || []).find((c) => c.channel === 'ONLINE'))
const posSharePercent = computed(() => posChannel.value?.revenueSharePercent || 0)
const onlineSharePercent = computed(() => onlineChannel.value?.revenueSharePercent || 0)

const topOneProduct = computed(() => (data.value.topSellingProducts || [])[0] || null)
const lowStockCount = computed(() => (data.value.lowStockAlerts || []).length)
const outOfStockCount = computed(() => (data.value.lowStockAlerts || []).filter((x) => Number(x.availableQuantity || 0) === 0).length)
const firstLowStock = computed(() => (data.value.lowStockAlerts || [])[0] || null)

function formatShortMoney(val) {
  const n = Number(val || 0)
  if (n >= 1_000_000_000) return (n / 1_000_000_000).toFixed(1) + 'B'
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M'
  if (n >= 1_000) return Math.round(n / 1_000) + 'K'
  return String(n)
}

/* ============================================================================
   APACHE ECHARTS CONFIGURATIONS (Crisp, Smooth, Theme-Synced, Responsive)
   ============================================================================ */
const themeColor = computed(() => settings.theme || '#409EFF')

// 1. Main Expanded Trend Chart (Dual-Axis Rounded Gradient Bars + Smooth Area Spline)
const trendChartOption = computed(() => {
  const list = trendSeries.value
  const periods = list.map((x) => x.period)
  const revenues = list.map((x) => Number(x.revenue || 0))
  const orders = list.map((x) => Number(x.orderCount || 0))
  const banks = list.map((x) => Number(x.bankTransferRevenue || 0))
  const cashes = list.map((x) => Number(x.cashRevenue || 0))
  const cods = list.map((x) => Number(x.codRevenue || 0))

  const barSeries =
    trendDisplayMode.value === 'TOTAL'
      ? [
          {
            name: 'Doanh thu thực nhận',
            type: 'bar',
            yAxisIndex: 0,
            barMaxWidth: 44,
            data: revenues,
            itemStyle: {
              borderRadius: [8, 8, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: themeColor.value },
                { offset: 1, color: '#93c5fd' }
              ])
            },
            emphasis: {
              itemStyle: {
                shadowBlur: 14,
                shadowColor: 'rgba(64, 158, 255, 0.32)'
              }
            },
            label: {
              show: true,
              position: 'top',
              distance: 6,
              color: '#303133',
              fontSize: 11,
              fontWeight: 700,
              formatter: (p) => (p.value > 0 ? formatShortMoney(p.value) : '')
            }
          }
        ]
      : [
          {
            name: 'Chuyển khoản (SePay)',
            type: 'bar',
            stack: 'rev',
            yAxisIndex: 0,
            barMaxWidth: 44,
            data: banks,
            itemStyle: { color: '#3b82f6', borderRadius: [0, 0, 0, 0] }
          },
          {
            name: 'Tiền mặt (CASH)',
            type: 'bar',
            stack: 'rev',
            yAxisIndex: 0,
            barMaxWidth: 44,
            data: cashes,
            itemStyle: { color: '#f59e0b' }
          },
          {
            name: 'Thu hộ (COD)',
            type: 'bar',
            stack: 'rev',
            yAxisIndex: 0,
            barMaxWidth: 44,
            data: cods,
            itemStyle: { color: '#10b981', borderRadius: [6, 6, 0, 0] }
          }
        ]

  return {
    animationDuration: 550,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        shadowStyle: { color: 'rgba(64, 158, 255, 0.06)' }
      },
      backgroundColor: 'rgba(255, 255, 255, 0.97)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#303133', fontSize: 12 },
      formatter: (params) => {
        const idx = params?.[0]?.dataIndex ?? 0
        const pt = list[idx]
        if (!pt) return ''
        return `
          <div style="min-width:190px;line-height:1.65;">
            <div style="font-weight:700;font-size:13px;color:#1e293b;margin-bottom:4px;border-bottom:1px solid #f1f5f9;padding-bottom:4px;">
              Kỳ ${pt.period}
            </div>
            <div style="display:flex;justify-content:space-between;gap:14px;">
              <span style="color:#64748b;">Doanh thu:</span>
              <strong style="color:${themeColor.value};">${formatCurrency(pt.revenue)}</strong>
            </div>
            <div style="display:flex;justify-content:space-between;gap:14px;">
              <span style="color:#64748b;">Số đơn hàng:</span>
              <strong style="color:#f97316;">${pt.orderCount} đơn (${pt.quantitySold || 0} đôi)</strong>
            </div>
            <div style="margin-top:5px;padding-top:5px;border-top:1px dashed #e2e8f0;font-size:11.5px;color:#475569;">
              <div>• Chuyển khoản: <b>${formatCurrency(pt.bankTransferRevenue)}</b></div>
              <div>• Tiền mặt: <b>${formatCurrency(pt.cashRevenue)}</b></div>
              <div>• Thu hộ COD: <b>${formatCurrency(pt.codRevenue)}</b></div>
            </div>
          </div>
        `
      }
    },
    legend: {
      top: 0,
      left: 0,
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 8,
      itemGap: 14,
      textStyle: { color: '#606266', fontSize: 11.5, fontWeight: 500 }
    },
    grid: {
      top: 46,
      left: 46,
      right: 34,
      bottom: 28,
      containLabel: false
    },
    xAxis: {
      type: 'category',
      data: periods,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11, fontWeight: 600, margin: 10 }
    },
    yAxis: [
      {
        type: 'value',
        splitNumber: 4,
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLabel: {
          color: '#64748b',
          fontSize: 10.5,
          formatter: (v) => formatShortMoney(v)
        }
      },
      {
        type: 'value',
        minInterval: 1,
        splitNumber: 4,
        splitLine: { show: false },
        axisLabel: {
          color: '#f97316',
          fontSize: 10.5,
          formatter: (v) => `${v}`
        }
      }
    ],
    series: [
      ...barSeries,
      {
        name: 'Số đơn hàng',
        type: 'line',
        yAxisIndex: 1,
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 8,
        data: orders,
        lineStyle: { color: '#f97316', width: 3 },
        itemStyle: { color: '#ffffff', borderColor: '#f97316', borderWidth: 2.5 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(249, 115, 22, 0.16)' },
            { offset: 1, color: 'rgba(249, 115, 22, 0.01)' }
          ])
        }
      }
    ]
  }
})

// Mini Sparkline EChart (Dock Card when Trend is minimized)
const trendSparkOption = computed(() => {
  const list = trendSeries.value.slice(-10)
  return {
    animationDuration: 400,
    grid: { top: 6, right: 4, bottom: 4, left: 4 },
    xAxis: { type: 'category', show: false, data: list.map((x) => x.period) },
    yAxis: { type: 'value', show: false },
    series: [
      {
        type: 'bar',
        barMaxWidth: 16,
        data: list.map((x) => Number(x.revenue || 0)),
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: themeColor.value },
            { offset: 1, color: '#bfdbfe' }
          ])
        }
      }
    ]
  }
})

// Compact EChart for 2×2 Grid Trend Card
const trendGridOption = computed(() => {
  const list = trendSeries.value.slice(-8)
  return {
    animationDuration: 450,
    grid: { top: 18, right: 24, bottom: 22, left: 42 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: list.map((x) => x.period),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 10.5 }
    },
    yAxis: [
      {
        type: 'value',
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLabel: { color: '#94a3b8', fontSize: 10, formatter: (v) => formatShortMoney(v) }
      },
      { type: 'value', minInterval: 1, splitLine: { show: false }, axisLabel: { show: false } }
    ],
    series: [
      {
        name: 'Doanh thu',
        type: 'bar',
        barMaxWidth: 26,
        data: list.map((x) => Number(x.revenue || 0)),
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: themeColor.value },
            { offset: 1, color: '#93c5fd' }
          ])
        }
      },
      {
        name: 'Số đơn',
        type: 'line',
        yAxisIndex: 1,
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 6,
        data: list.map((x) => Number(x.orderCount || 0)),
        lineStyle: { color: '#f97316', width: 2 },
        itemStyle: { color: '#ffffff', borderColor: '#f97316', borderWidth: 2 }
      }
    ]
  }
})

// 2. Channels Donut Ring Chart (Expanded)
const channelDonutOption = computed(() => {
  const posVal = Number(posChannel.value?.actualRevenue || 0)
  const onlVal = Number(onlineChannel.value?.actualRevenue || 0)
  return {
    animationDuration: 550,
    tooltip: {
      trigger: 'item',
      backgroundColor: '#ffffff',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#303133', fontSize: 12 },
      formatter: (p) => `<strong>${p.name}</strong><br/>${formatCurrency(p.value)} (${p.percent}%)`
    },
    legend: {
      bottom: 6,
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#475569', fontSize: 12, fontWeight: 600 }
    },
    title: {
      text: formatShortMoney(posVal + onlVal),
      subtext: 'Tổng thực thu',
      left: 'center',
      top: '39%',
      textStyle: { fontSize: 22, fontWeight: 800, color: '#1e293b' },
      subtextStyle: { fontSize: 11.5, color: '#94a3b8' }
    },
    series: [
      {
        name: 'Tỷ trọng kênh',
        type: 'pie',
        radius: ['54%', '76%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#ffffff',
          borderWidth: 3
        },
        label: {
          show: true,
          formatter: '{d}%',
          fontSize: 12,
          fontWeight: 700,
          color: '#303133'
        },
        data: [
          { value: posVal, name: 'Tại quầy (POS)', itemStyle: { color: themeColor.value } },
          { value: onlVal, name: 'Online Store', itemStyle: { color: '#f97316' } }
        ]
      }
    ]
  }
})

// Channels Success vs Cancel Rate Grouped Bar Chart (Expanded)
const channelRateBarOption = computed(() => {
  const pos = posChannel.value || {}
  const onl = onlineChannel.value || {}
  return {
    animationDuration: 500,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) =>
        params.map((p) => `${p.marker} ${p.seriesName}: <strong>${p.value}%</strong>`).join('<br/>')
    },
    legend: {
      top: 0,
      right: 4,
      icon: 'roundRect',
      itemWidth: 10,
      itemHeight: 8,
      textStyle: { fontSize: 11.5, color: '#64748b' }
    },
    grid: { top: 28, left: 68, right: 26, bottom: 20 },
    xAxis: {
      type: 'value',
      max: 100,
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 10.5, formatter: '{value}%' }
    },
    yAxis: {
      type: 'category',
      data: ['Online', 'Tại quầy'],
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#475569', fontSize: 11.5, fontWeight: 600 }
    },
    series: [
      {
        name: 'Giao thành công',
        type: 'bar',
        barMaxWidth: 14,
        data: [Number(onl.successRate || 0), Number(pos.successRate || 0)],
        itemStyle: { color: '#10b981', borderRadius: [0, 5, 5, 0] },
        label: { show: true, position: 'right', fontSize: 11, color: '#059669', formatter: '{c}%' }
      },
      {
        name: 'Tỷ lệ hủy',
        type: 'bar',
        barMaxWidth: 14,
        data: [Number(onl.cancellationRate || 0), Number(pos.cancellationRate || 0)],
        itemStyle: { color: '#ef4444', borderRadius: [0, 5, 5, 0] },
        label: { show: true, position: 'right', fontSize: 11, color: '#dc2626', formatter: '{c}%' }
      }
    ]
  }
})

// Compact Channels Donut for 2×2 Grid
const channelGridDonutOption = computed(() => {
  const posVal = Number(posChannel.value?.actualRevenue || 0)
  const onlVal = Number(onlineChannel.value?.actualRevenue || 0)
  return {
    animationDuration: 450,
    tooltip: {
      trigger: 'item',
      formatter: (p) => `${p.name}: <b>${formatCurrency(p.value)}</b> (${p.percent}%)`
    },
    legend: {
      orient: 'vertical',
      right: 8,
      top: 'middle',
      icon: 'circle',
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: '#475569', fontSize: 11.5 }
    },
    series: [
      {
        type: 'pie',
        radius: ['50%', '76%'],
        center: ['32%', '50%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: [
          { value: posVal, name: `POS (${posSharePercent.value}%)`, itemStyle: { color: themeColor.value } },
          { value: onlVal, name: `Online (${onlineSharePercent.value}%)`, itemStyle: { color: '#f97316' } }
        ]
      }
    ]
  }
})

// 3. Top 5 Best-Selling Sneakers Horizontal Gradient Bar Chart (Expanded)
const topProductsBarOption = computed(() => {
  const list = [...(data.value.topSellingProducts || [])].slice(0, 5).reverse()
  return {
    animationDuration: 550,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const idx = params?.[0]?.dataIndex ?? 0
        const item = list[idx]
        if (!item) return ''
        return `<strong>${item.productName}</strong><br/>Đã bán: <b>${item.quantitySold} đôi</b><br/>Doanh thu: <b style="color:#dc2626">${formatCurrency(item.revenue)}</b>`
      }
    },
    grid: { top: 12, left: 10, right: 64, bottom: 16, containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 10.5, formatter: (v) => formatShortMoney(v) }
    },
    yAxis: {
      type: 'category',
      data: list.map((x) => (x.productName?.length > 18 ? x.productName.slice(0, 18) + '…' : x.productName)),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#334155', fontSize: 11.5, fontWeight: 600 }
    },
    series: [
      {
        name: 'Doanh thu',
        type: 'bar',
        barMaxWidth: 24,
        data: list.map((x) => Number(x.revenue || 0)),
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#60a5fa' },
            { offset: 1, color: themeColor.value }
          ])
        },
        label: {
          show: true,
          position: 'right',
          color: '#303133',
          fontSize: 11,
          fontWeight: 700,
          formatter: (p) => formatShortMoney(p.value)
        }
      }
    ]
  }
})

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
.exec-dashboard-viewport {
  height: calc(100vh - var(--dash-header-h, 84px));
  min-height: 0 !important;
  padding: 12px 16px !important;
  gap: 10px !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

/* 1. Compact Command Bar */
.dash-topbar {
  background: #ffffff;
  border: 1px solid #e6ebf5;
  border-radius: 6px;
  padding: 8px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 21, 41, 0.03);
}

.topbar-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dash-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
}

.period-chip {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--vea-theme-light-9, #ecf5ff);
  color: var(--vea-theme, #409eff);
  border: 1px solid var(--vea-theme-light-7, #c6e2ff);
  white-space: nowrap;
}

.topbar-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
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

.layout-switch {
  display: inline-flex;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  background: #ffffff;
}

.ls-btn {
  width: 28px;
  height: 26px;
  border: none;
  background: transparent;
  color: #606266;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    color: var(--vea-theme, #409eff);
    background: var(--vea-theme-light-9, #ecf5ff);
  }

  &.active {
    background: var(--vea-theme, #409eff);
    color: #ffffff;
  }
}

.topbar-divider {
  width: 1px;
  height: 20px;
  background: #e6ebf5;
}

.tb-icon-btn {
  width: 28px !important;
  padding: 0 !important;
}

/* 2. Single-Row 6-KPI Strip */
.kpi-strip-6 {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  flex-shrink: 0;
}

.kpi-mini {
  background: #ffffff;
  border: 1px solid #e6ebf5;
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
  min-width: 0;

  &:hover {
    border-color: var(--vea-theme-light-5, #a0cfff);
    box-shadow: 0 3px 10px rgba(0, 21, 41, 0.06);
    transform: translateY(-1px);
  }

  &.is-focus-linked {
    border-color: var(--vea-theme-light-5, #a0cfff);
  }

  &.is-danger {
    background: #fef2f2;
    border-color: #fca5a5;
  }
}

.km-icon {
  width: 34px;
  height: 34px;
  border-radius: 7px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.tone-teal { color: #14b8a6; background: #f0fdfa; }
.tone-blue { color: #3b82f6; background: #eff6ff; }
.tone-pink { color: #f43f5e; background: #fff1f2; }
.tone-green { color: #10b981; background: #ecfdf5; }
.tone-primary { color: var(--vea-theme, #409eff); background: var(--vea-theme-light-9, #ecf5ff); }
.tone-amber { color: #f59e0b; background: #fffbeb; }

.km-body {
  flex: 1;
  min-width: 0;
}

.km-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
}

.km-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.km-val {
  font-size: 15.5px;
  font-weight: 800;
  color: #303133;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.km-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 3px;
  white-space: nowrap;

  &.up { background: #ecfdf5; color: #059669; }
  &.down { background: #fef2f2; color: #dc2626; }
  &.amber { background: #fffbeb; color: #b45309; }
  &.red { background: #fef2f2; color: #dc2626; }
}

.text-red { color: #dc2626 !important; }
.text-green { color: #059669 !important; }

/* 3. Main Interactive Workspace Stage */
.dash-stage {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.dash-stage.mode-focus {
  display: flex;
  gap: 12px;
}

.focus-hero-card {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: #ffffff;
  border: 1px solid #e6ebf5;
  border-radius: 6px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.04);
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.focus-mini-dock {
  width: 315px;
  flex-shrink: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.dock-card {
  flex: 1;
  min-height: 0;
  background: #ffffff;
  border: 1px solid #e6ebf5;
  border-radius: 6px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
  cursor: pointer;
  transition: all 0.18s ease;
  box-shadow: 0 1px 3px rgba(0, 21, 41, 0.03);

  &:hover {
    border-color: var(--vea-theme, #409eff);
    box-shadow: 0 4px 14px rgba(64, 158, 255, 0.12);
    transform: translateX(-2px);

    .dock-expand-hint {
      color: var(--vea-theme, #409eff);
      background: var(--vea-theme-light-9, #ecf5ff);
      border-color: var(--vea-theme-light-7, #c6e2ff);
    }
  }
}

.dock-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.dock-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.dock-icon {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
}

.dock-title {
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dock-expand-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #909399;
  padding: 2px 7px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  background: #fafafa;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.dock-preview {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 6px;
}

.dock-echart-mini {
  flex: 1;
  min-height: 44px;
  width: 100%;
}

.dock-foot-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11.5px;
  color: #64748b;
  padding-top: 6px;
  border-top: 1px dashed #ebeef5;
}

.dock-muted {
  font-size: 12px;
  color: #94a3b8;
  margin: auto 0;
}

/* Mini Channel Preview */
.mini-ch-nums {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.mini-ch-item {
  display: flex;
  flex-direction: column;
  gap: 2px;

  strong {
    font-size: 14px;
    font-weight: 800;
    color: #303133;
    font-variant-numeric: tabular-nums;
  }
}

.mini-ch-lbl {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.dot-pos,
.dot-online {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.dot-pos { background: var(--vea-theme, #409eff); }
.dot-online { background: #f97316; }

/* Mini Top Product Preview */
.mini-prod-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.mini-thumb {
  width: 36px;
  height: 36px;
  border-radius: 5px;
  object-fit: cover;
  border: 1px solid #e6ebf5;
  flex-shrink: 0;

  &.fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--vea-theme-light-9, #ecf5ff);
    color: var(--vea-theme, #409eff);
    font-size: 11px;
    font-weight: 800;
  }
}

.mini-prod-meta {
  min-width: 0;
  flex: 1;
}

.mini-prod-name {
  font-size: 12.5px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini-prod-sub {
  font-size: 11.5px;
  color: #64748b;
  margin-top: 2px;
}

/* Mini Low Stock Preview */
.mini-stock-summary {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stock-big-num {
  font-size: 28px;
  font-weight: 900;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stock-big-desc {
  display: flex;
  flex-direction: column;
  font-size: 12px;
  color: #303133;

  span {
    font-size: 11px;
    color: #909399;
  }
}

.truncate-txt {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

/* --- SHARED PANEL HEADER & BODY --- */
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;

  &.compact-head {
    padding-bottom: 8px;
  }
}

.ph-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ph-icon {
  width: 32px;
  height: 32px;
  border-radius: 7px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.ph-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.panel-title {
  margin: 0;
  font-size: 14.5px;
  font-weight: 700;
  color: #303133;
}

.panel-sub {
  margin: 2px 0 0;
  font-size: 11.5px;
  color: #909399;
}

.panel-body-fill {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-scroll-host {
  overflow: hidden;
}

.empty-fill {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* --- EXPANDED CHANNELS (DONUT LEFT + CARDS & BAR RIGHT) --- */
.channels-split-layout {
  flex-direction: row !important;
  gap: 16px;
}

.ch-donut-pane {
  width: 38%;
  min-width: 240px;
  height: 100%;
  background: #fafbfc;
  border: 1px solid #edf2f7;
  border-radius: 6px;
  padding: 8px;
}

.ch-detail-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.channel-cards-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  flex-shrink: 0;
}

.ch-card {
  border: 1px solid #e6ebf5;
  border-radius: 6px;
  padding: 12px 14px;
  background: #fafbfc;

  &.ch-pos { border-top: 3px solid var(--vea-theme, #409eff); }
  &.ch-online { border-top: 3px solid #f97316; }
}

.ch-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.ch-share-pill {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 11px;
  color: #475569;
}

.ch-revenue {
  font-size: 19px;
  font-weight: 900;
  color: #303133;
  margin: 6px 0 8px;
  font-variant-numeric: tabular-nums;
}

.ch-metrics {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 12px;

  div {
    display: flex;
    justify-content: space-between;
    color: #525252;
    padding-bottom: 4px;
    border-bottom: 1px dashed #e2e8f0;

    &:last-child { border-bottom: none; padding-bottom: 0; }
  }
}

.ch-rate-chart-wrap {
  flex: 1;
  min-height: 120px;
  background: #fafbfc;
  border: 1px solid #edf2f7;
  border-radius: 6px;
  padding: 8px 10px 4px;
}

.channel-ratio-bar {
  height: 10px;
  border-radius: 999px;
  background: #eef2f7;
  overflow: hidden;
  display: flex;

  &.mini-bar {
    height: 7px;
  }

  .ratio-pos { background: var(--vea-theme, #409eff); height: 100%; transition: width 0.3s ease; }
  .ratio-online { background: #f97316; height: 100%; transition: width 0.3s ease; }
}

/* --- EXPANDED TOP PRODUCTS (ECHARTS BAR LEFT + TABLE RIGHT) --- */
.top-products-split {
  flex-direction: row !important;
  gap: 14px;
}

.top-chart-pane {
  width: 44%;
  min-width: 260px;
  height: 100%;
  background: #fafbfc;
  border: 1px solid #edf2f7;
  border-radius: 6px;
  padding: 8px;
}

.top-table-pane {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

/* --- 2×2 GRID MODE --- */
.dash-stage.mode-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  grid-template-rows: 1fr 1fr;
  gap: 10px;
}

.grid-panel-card {
  background: #ffffff;
  border: 1px solid #e6ebf5;
  border-radius: 6px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;

  &:hover {
    border-color: var(--vea-theme, #409eff);
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);

    .dock-expand-hint {
      color: var(--vea-theme, #409eff);
      background: var(--vea-theme-light-9, #ecf5ff);
      border-color: var(--vea-theme-light-7, #c6e2ff);
    }
  }
}

.grid-card-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}

.compact-list {
  justify-content: flex-start;
  gap: 6px;
}

.compact-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px;
  border-radius: 4px;
  background: #f8fafc;
  font-size: 12px;
}

.rank-num {
  font-weight: 800;
  color: var(--vea-theme, #409eff);
  width: 22px;
  flex-shrink: 0;
}

.compact-name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.compact-qty {
  font-size: 11.5px;
  color: #64748b;
  white-space: nowrap;
}

.compact-money {
  font-size: 12px;
  white-space: nowrap;
}

/* --- PRODUCTS & LOW STOCK CELLS --- */
.prod-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.prod-thumb {
  width: 36px;
  height: 36px;
  border-radius: 5px;
  object-fit: cover;
  border: 1px solid #e5e5e5;
  flex-shrink: 0;

  &.fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f5f5;
    font-size: 10.5px;
    font-weight: 800;
    color: #94a3b8;
  }
}

.prod-name {
  font-size: 12.5px;
  font-weight: 700;
  color: #303133;
}

.prod-sub {
  font-size: 11px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}

.size-chip {
  background: #f1f5f9;
  border: 1px solid #d5dde7;
  padding: 0 5px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
}

.qty-pill {
  background: var(--vea-theme-light-9, #ecf5ff);
  color: var(--vea-theme, #409eff);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11.5px;
}

.money-red {
  color: #dc2626;
  font-variant-numeric: tabular-nums;
}

.stock-alert-pill {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;

  &.critical { background: #fef2f2; color: #dc2626; border: 1px solid #fca5a5; }
  &.warning { background: #fffbeb; color: #b45309; border: 1px solid #fcd34d; }
}

@media (max-width: 1180px) {
  .kpi-strip-6 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .focus-mini-dock {
    width: 265px;
  }
  .channels-split-layout,
  .top-products-split {
    flex-direction: column !important;
  }
  .ch-donut-pane,
  .top-chart-pane {
    width: 100%;
    height: 220px;
  }
}

@media (max-width: 960px) {
  .exec-dashboard-viewport {
    height: auto !important;
    overflow: visible !important;
  }
  .dash-topbar {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    padding: 10px 12px;
  }
  .title-line {
    justify-content: space-between;
  }
  .topbar-controls {
    width: 100%;
    gap: 6px;
  }
  .quick-time-group {
    max-width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .layout-switch,
  .topbar-divider,
  .hide-mobile-grid-btn {
    display: none !important;
  }
  .dash-stage.mode-focus {
    flex-direction: column;
    overflow: visible;
  }
  .focus-hero-card {
    height: auto;
    min-height: 0;
    padding: 12px;
  }
  .focus-hero-card .panel-head {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  .focus-hero-card .ph-right {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
  .focus-hero-card .panel-body-fill {
    height: auto;
    min-height: 0;
  }
  .focus-hero-card .panel-chart-host {
    height: 280px !important;
  }
  .focus-hero-card .table-scroll-host {
    height: 300px !important;
  }
  .ch-donut-pane,
  .top-chart-pane {
    min-width: 0 !important;
    width: 100%;
    height: 240px !important;
    flex: none !important;
  }
  .ch-rate-chart-wrap {
    height: 155px !important;
    flex: none !important;
  }
  .top-table-pane {
    height: 240px !important;
    flex: none !important;
  }
  .channel-cards-grid {
    grid-template-columns: 1fr;
  }
  .focus-mini-dock {
    width: 100%;
    height: auto;
  }
  .dash-stage.mode-grid {
    grid-template-columns: 1fr;
    grid-template-rows: auto;
  }
  .kpi-strip-6 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }
  .kpi-mini {
    padding: 8px 10px;
    gap: 8px;
  }
  .km-icon {
    width: 28px;
    height: 28px;
    font-size: 14px;
  }
  .km-val {
    font-size: 13.5px;
  }
}
</style>
