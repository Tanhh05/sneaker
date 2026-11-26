<template>
  <div class="variant-studio-page" v-loading="loading">
    <!-- ========================================================= -->
    <!-- 1. STICKY COMMAND BAR (PRODUCT + LIVE STATS + SAVE)       -->
    <!-- ========================================================= -->
    <div class="studio-topbar">
      <div class="topbar-left">
        <button type="button" class="btn-back" @click="$router.push('/admin/variants')" title="Quay lại danh sách biến thể">
          <el-icon><ArrowLeft /></el-icon>
          <span>Quay lại</span>
        </button>

        <div class="mode-pill" :class="isEdit ? 'mode-edit' : 'mode-create'">
          {{ isEdit ? 'CẬP NHẬT SKU' : 'THÊM NHIỀU SKU' }}
        </div>

        <div class="product-picker-inline">
          <QuickSelect
            v-model="selectedProductId"
            :options="products"
            value-key="id"
            :label-key="(p) => `${p.productCode} — ${p.productName}`"
            :search-keys="['productCode', 'productName', 'brandName']"
            placeholder="Tìm & chọn sản phẩm Sneaker..."
            search-placeholder="Gõ mã SP hoặc tên giày..."
            class="product-select"
            @change="handleProductChange"
          >
            <template #option="{ item }">
              <div class="product-option-row">
                <span class="code-badge">{{ item.productCode }}</span>
                <span class="p-name">{{ item.productName }}</span>
                <span class="p-price">{{ formatCurrency(item.salePrice) }}</span>
              </div>
            </template>
          </QuickSelect>
        </div>

        <div v-if="selectedProduct" class="product-mini-meta">
          <img
            v-if="selectedProduct.thumbnailUrl"
            :src="selectedProduct.thumbnailUrl"
            class="mini-thumb"
            alt=""
          />
          <div class="mini-kpi-group">
            <span class="kpi-chip">
              <strong>{{ existingRowsCount }}</strong> đã có
            </span>
            <span v-if="modifiedRowsCount > 0" class="kpi-chip kpi-amber">
              <strong>{{ modifiedRowsCount }}</strong> đã sửa
            </span>
            <span v-if="newRowsCount > 0" class="kpi-chip kpi-green">
              <strong>+{{ newRowsCount }}</strong> mới
            </span>
            <span class="kpi-chip kpi-stock">
              Tồn: <strong>{{ totalStockQty }}</strong>
            </span>
          </div>
        </div>
      </div>

      <div class="topbar-right">
        <button
          v-if="totalPendingChanges > 0"
          type="button"
          class="btn-ghost"
          title="Hoàn tác toàn bộ thay đổi chưa lưu"
          @click="revertAllChanges"
        >
          ↺ Hoàn tác
        </button>
        <el-button
          type="primary"
          class="btn-save-primary"
          :loading="saving"
          :disabled="!selectedProductId || totalPendingChanges === 0"
          @click="submitAllChanges"
        >
          <el-icon style="margin-right: 6px"><Check /></el-icon>
          <span>{{ saveButtonLabel }}</span>
        </el-button>
      </div>
    </div>

    <template v-if="selectedProduct">
      <!-- ========================================================= -->
      <!-- 2. COMPACT CONTROL DOCK (BULK DEFAULTS + COLLAPSIBLE GEN) -->
      <!-- ========================================================= -->
      <div class="studio-card control-dock">
        <!-- Compact 1-line Bulk Defaults Strip -->
        <div class="bulk-strip">
          <div class="bulk-strip-title">
            <span class="badge-black">THIẾT LẬP NHANH</span>
            <button
              type="button"
              class="btn-toggle-gen"
              :class="{ open: showGenerator }"
              @click="showGenerator = !showGenerator"
            >
              <el-icon><MagicStick /></el-icon>
              <span>{{ showGenerator ? 'Ẩn bộ sinh Size × Màu' : 'Sinh hàng loạt Size × Màu' }}</span>
              <span class="caret">{{ showGenerator ? '▲' : '▼' }}</span>
            </button>
          </div>

          <div class="bulk-inputs">
            <div class="compact-field">
              <label>Giá nhập</label>
              <el-input-number
                v-model="defaults.importPrice"
                :min="0"
                :step="50000"
                size="small"
                controls-position="right"
              />
            </div>
            <div class="compact-field">
              <label>Giá bán</label>
              <el-input-number
                v-model="defaults.salePrice"
                :min="0"
                :step="50000"
                size="small"
                controls-position="right"
              />
            </div>
            <div class="compact-field qty-field">
              <label>Tồn kho</label>
              <el-input-number
                v-model="defaults.availableQuantity"
                :min="0"
                size="small"
                controls-position="right"
              />
            </div>
            <div class="compact-field status-field">
              <label>Trạng thái</label>
              <el-select filterable v-model="defaults.status" size="small">
                <el-option label="ACTIVE" value="ACTIVE" />
                <el-option label="INACTIVE" value="INACTIVE" />
              </el-select>
            </div>
          </div>

          <div class="bulk-apply-group">
            <el-select filterable v-model="bulkScope" size="small" class="scope-select" title="Phạm vi áp dụng">
              <el-option label="Áp dụng: Tất cả SKU" value="all" />
              <el-option label="Chỉ SKU mới thêm" value="new" />
              <el-option
                v-if="activeInspectColor"
                :label="`Chỉ màu ${activeInspectColor.colorName}`"
                value="color"
              />
            </el-select>
            <button
              type="button"
              class="btn-mini-apply"
              :disabled="allVariantRows.length === 0"
              @click="applyPricesByScope"
            >
              Áp giá
            </button>
            <button
              type="button"
              class="btn-mini-apply"
              :disabled="allVariantRows.length === 0"
              @click="applyStockByScope"
            >
              Áp tồn & TT
            </button>
          </div>
        </div>

        <!-- Collapsible Batch Generator (Size × Color Pills) -->
        <transition name="slide-fade">
          <div v-if="showGenerator" class="generator-drawer">
            <div class="gen-grid">
              <!-- Compact Size Chips -->
              <div class="gen-col">
                <div class="gen-col-head">
                  <span class="gen-label">SIZE EU <em>({{ selectedSizeIds.length }}/{{ sizes.length }})</em></span>
                  <div class="gen-quick">
                    <button type="button" @click="selectPopularSizes">Phổ biến (38–43)</button>
                    <span>·</span>
                    <button type="button" @click="selectAllSizes">Tất cả</button>
                    <span>·</span>
                    <button type="button" @click="selectedSizeIds = []">Bỏ</button>
                  </div>
                </div>
                <div class="chip-wrap">
                  <button
                    v-for="s in sizes"
                    :key="s.id"
                    type="button"
                    class="size-chip"
                    :class="{ active: selectedSizeIds.includes(s.id) }"
                    @click="toggleId(selectedSizeIds, s.id)"
                  >
                    {{ s.sizeValue }}
                  </button>
                </div>
              </div>

              <!-- Compact Color Chips -->
              <div class="gen-col">
                <div class="gen-col-head">
                  <span class="gen-label">MÀU SẮC <em>({{ selectedColorIds.length }}/{{ colors.length }})</em></span>
                  <div class="gen-quick">
                    <button type="button" @click="selectAllColors">Tất cả</button>
                    <span>·</span>
                    <button type="button" @click="selectedColorIds = []">Bỏ</button>
                  </div>
                </div>
                <div class="chip-wrap">
                  <button
                    v-for="c in colors"
                    :key="c.id"
                    type="button"
                    class="color-chip"
                    :class="{ active: selectedColorIds.includes(c.id) }"
                    @click="toggleId(selectedColorIds, c.id)"
                  >
                    <span class="swatch" :style="{ backgroundColor: c.hexCode }"></span>
                    <span>{{ c.colorName }}</span>
                    <small>{{ c.colorCode }}</small>
                  </button>
                </div>
              </div>
            </div>

            <div class="gen-footer">
              <div class="gen-preview-hint">
                <template v-if="selectedSizeIds.length > 0 && selectedColorIds.length > 0">
                  Sẽ tạo tối đa <strong>{{ pendingGenerateCount }}</strong> biến thể mới
                  <span v-if="alreadyExistsInSelection > 0" class="muted">
                    (đã có sẵn {{ alreadyExistsInSelection }} tổ hợp)
                  </span>
                </template>
                <template v-else>
                  Tick nhanh Size & Màu rồi bấm tạo tổ hợp — hoặc bấm trực tiếp dấu <strong>+</strong> trên ma trận bên dưới.
                </template>
              </div>
              <div class="gen-btns">
                <button type="button" class="btn-secondary-sm" @click="addManualRow">
                  + Thêm 1 dòng thủ công
                </button>
                <button
                  type="button"
                  class="btn-generate-now"
                  :disabled="selectedSizeIds.length === 0 || selectedColorIds.length === 0"
                  @click="generateMatrixVariants"
                >
                  TẠO {{ pendingGenerateCount > 0 ? pendingGenerateCount : selectedSizeIds.length * selectedColorIds.length }} BIẾN THỂ
                </button>
              </div>
            </div>
          </div>
        </transition>
      </div>

      <!-- ========================================================= -->
      <!-- 3. WORKSPACE CARD (MATRIX + INLINE INSPECTOR / TABLE)     -->
      <!-- ========================================================= -->
      <div class="studio-card workspace-card">
        <!-- Workspace Toolbar -->
        <div class="workspace-toolbar">
          <div class="ws-left">
            <div class="view-switch">
              <button
                type="button"
                class="vs-btn"
                :class="{ active: viewMode === 'matrix' }"
                @click="viewMode = 'matrix'"
              >
                <el-icon><Grid /></el-icon>
                <span>Ma trận ({{ allVariantRows.length }})</span>
              </button>
              <button
                type="button"
                class="vs-btn"
                :class="{ active: viewMode === 'table' }"
                @click="viewMode = 'table'"
              >
                <el-icon><List /></el-icon>
                <span>Bảng chi tiết</span>
              </button>
            </div>

            <label v-if="viewMode === 'matrix'" class="full-grid-toggle">
              <input type="checkbox" v-model="showFullGrid" />
              <span>Hiện đủ lưới {{ sizes.length }} Size × {{ colors.length }} Màu</span>
            </label>
          </div>

          <div class="ws-right">
            <div v-if="viewMode === 'matrix'" class="matrix-legend-inline">
              <span class="leg"><i class="dot dot-exist"></i>Đã có</span>
              <span class="leg"><i class="dot dot-mod"></i>Đã sửa</span>
              <span class="leg"><i class="dot dot-new"></i>Mới</span>
              <span class="leg"><i class="dot dot-off"></i>Tạm ẩn</span>
            </div>

            <button
              v-if="newRowsCount > 0"
              type="button"
              class="btn-danger-ghost"
              @click="clearNewRows"
            >
              Xóa {{ newRowsCount }} dòng mới
            </button>
          </div>
        </div>

        <!-- ================================================ -->
        <!-- INLINE QUICK INSPECTOR BAR (DOCKED IN MATRIX)    -->
        <!-- ================================================ -->
        <div v-if="viewMode === 'matrix' && inspectedRow" class="quick-inspector-bar" :class="getInspectorClass(inspectedRow)">
          <div class="inspector-head">
            <div class="insp-identity">
              <span class="swatch-lg" :style="{ backgroundColor: getColorObj(inspectedRow.colorId)?.hexCode || '#ccc' }"></span>
              <div class="insp-title">
                <strong>{{ getColorObj(inspectedRow.colorId)?.colorName || 'Màu' }} · Size {{ getSizeObj(inspectedRow.sizeId)?.sizeValue || '?' }}</strong>
                <span v-if="inspectedRow.isNew" class="row-badge badge-new">MỚI</span>
                <span v-else-if="isRowModified(inspectedRow)" class="row-badge badge-mod">ĐÃ SỬA</span>
                <span v-else class="row-badge badge-db">ĐÃ CÓ</span>
                <span v-if="getRowError(inspectedRow)" class="insp-error">⚠ {{ getRowError(inspectedRow) }}</span>
              </div>
            </div>
            <div class="insp-profit">
              Lợi nhuận dự kiến:
              <strong :class="inspectedRow.salePrice >= inspectedRow.importPrice ? 'text-green' : 'text-red'">
                {{ formatCurrency((inspectedRow.salePrice || 0) - (inspectedRow.importPrice || 0)) }}
              </strong>
            </div>
          </div>

          <div class="inspector-fields">
            <div class="insp-field sku-f">
              <label>Mã SKU</label>
              <el-input v-model="inspectedRow.sku" size="small" placeholder="VD: AF1-41-W" />
            </div>
            <div class="insp-field">
              <label>Giá nhập (VNĐ)</label>
              <el-input-number v-model="inspectedRow.importPrice" :min="0" :step="50000" size="small" controls-position="right" style="width: 100%" />
            </div>
            <div class="insp-field">
              <label>Giá bán (VNĐ)</label>
              <el-input-number v-model="inspectedRow.salePrice" :min="0" :step="50000" size="small" controls-position="right" style="width: 100%" />
            </div>
            <div class="insp-field qty-f">
              <label>Tồn kho</label>
              <el-input-number v-model="inspectedRow.availableQuantity" :min="0" size="small" controls-position="right" style="width: 100%" />
            </div>
            <div class="insp-field status-f">
              <label>Trạng thái</label>
              <el-select filterable v-model="inspectedRow.status" size="small" style="width: 100%">
                <el-option label="ACTIVE" value="ACTIVE" />
                <el-option label="INACTIVE" value="INACTIVE" />
              </el-select>
            </div>
            <div class="insp-actions">
              <button
                v-if="!inspectedRow.isNew && isRowModified(inspectedRow)"
                type="button"
                class="btn-insp-act"
                title="Hoàn tác thay đổi của biến thể này"
                @click="revertRow(inspectedRow)"
              >
                ↺ Hoàn tác
              </button>
              <button
                v-if="inspectedRow.isNew"
                type="button"
                class="btn-insp-act danger"
                title="Xóa biến thể mới này"
                @click="removeInspectedRow"
              >
                ✕ Xóa
              </button>
              <button
                type="button"
                class="btn-insp-close"
                title="Đóng thanh sửa nhanh"
                @click="inspectedKey = null"
              >
                ×
              </button>
            </div>
          </div>
        </div>

        <!-- ================================================ -->
        <!-- VIEW A: COMPACT INTERACTIVE MATRIX GRID          -->
        <!-- ================================================ -->
        <div v-if="viewMode === 'matrix'" class="matrix-view-wrap">
          <div v-if="matrixColors.length === 0 || matrixSizes.length === 0" class="empty-matrix-box">
            <p class="empty-title">Sản phẩm này chưa có biến thể nào trên lưới</p>
            <p class="empty-sub">Chọn nhanh Size & Màu ở khung phía trên hoặc mở toàn bộ lưới để bấm <strong>+</strong> thêm trực tiếp từng ô.</p>
            <div class="empty-actions">
              <button type="button" class="btn-generate-now" @click="showGenerator = true">
                Mở bộ sinh tổ hợp Size × Màu
              </button>
              <button type="button" class="btn-secondary-sm" @click="showFullGrid = true">
                Hiện đủ lưới {{ sizes.length }} Size × {{ colors.length }} Màu
              </button>
            </div>
          </div>

          <div v-else class="matrix-table-scroll">
            <table class="variant-matrix-table">
              <thead>
                <tr>
                  <th class="sticky-color-col">
                    <div class="corner-head">
                      <span>MÀU \ SIZE</span>
                      <small>{{ matrixColors.length }} màu · {{ matrixSizes.length }} size</small>
                    </div>
                  </th>
                  <th v-for="s in matrixSizes" :key="s.id" class="size-col-head">
                    <div class="size-head-top">{{ s.sizeValue }}</div>
                    <div class="size-head-sub">
                      <span>{{ countTotalBySize(s.id) }}</span>
                      <button
                        v-if="countTotalBySize(s.id) < matrixColors.length"
                        type="button"
                        class="btn-fill-axis"
                        :title="`Điền nhanh tất cả màu còn thiếu cho Size ${s.sizeValue}`"
                        @click="fillSizeColumn(s.id)"
                      >
                        +cột
                      </button>
                    </div>
                  </th>
                  <th class="row-total-head">TỒN</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in matrixColors" :key="c.id">
                  <td class="sticky-color-col">
                    <div class="matrix-color-cell">
                      <span class="color-swatch" :style="{ backgroundColor: c.hexCode }"></span>
                      <div class="color-meta">
                        <strong>{{ c.colorName }}</strong>
                        <span>{{ c.colorCode }} · {{ countTotalByColor(c.id) }}/{{ matrixSizes.length }}</span>
                      </div>
                      <div class="color-row-acts">
                        <button
                          v-if="countTotalByColor(c.id) < matrixSizes.length"
                          type="button"
                          class="btn-row-fill"
                          :title="`Thêm đủ các size còn thiếu cho màu ${c.colorName}`"
                          @click="fillColorRow(c.id)"
                        >
                          +hàng
                        </button>
                        <button
                          v-if="countNewByColor(c.id) > 0"
                          type="button"
                          class="btn-clear-row"
                          title="Gỡ các biến thể mới của màu này"
                          @click="removeNewColorRows(c.id)"
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  </td>

                  <td v-for="s in matrixSizes" :key="s.id" class="matrix-cell">
                    <div
                      v-if="findRow(s.id, c.id)"
                      class="cell-box"
                      :class="getCellClass(findRow(s.id, c.id))"
                      @click="selectInspectRow(findRow(s.id, c.id))"
                    >
                      <input
                        type="number"
                        min="0"
                        class="cell-qty-input"
                        :value="findRow(s.id, c.id).availableQuantity"
                        :title="getCellTooltip(findRow(s.id, c.id))"
                        @focus="selectInspectRow(findRow(s.id, c.id))"
                        @input="onMatrixQtyInput(s.id, c.id, $event.target.value)"
                      />
                      <span class="cell-price-mini">
                        {{ formatShortPrice(findRow(s.id, c.id).salePrice) }}
                      </span>
                      <button
                        v-if="findRow(s.id, c.id).isNew"
                        type="button"
                        class="cell-corner-btn remove"
                        title="Xóa biến thể mới này"
                        @click.stop="removeNewByCombo(s.id, c.id)"
                      >
                        ×
                      </button>
                      <button
                        v-else-if="isRowModified(findRow(s.id, c.id))"
                        type="button"
                        class="cell-corner-btn revert"
                        title="Hoàn tác biến thể này"
                        @click.stop="revertRow(findRow(s.id, c.id))"
                      >
                        ↺
                      </button>
                    </div>

                    <button
                      v-else
                      type="button"
                      class="cell-add-btn"
                      :title="`Bấm thêm nhanh Size ${s.sizeValue} - ${c.colorName}`"
                      @click="addSingleCombo(s.id, c.id)"
                    >
                      +
                    </button>
                  </td>

                  <td class="row-total-cell">
                    <strong>{{ sumStockByColor(c.id) }}</strong>
                    <small>{{ countTotalByColor(c.id) }} sku</small>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="matrix-footer-tip">
            <span>Mẹo: Bấm vào bất kỳ ô nào để sửa nhanh <strong>SKU / Giá nhập / Giá bán / Trạng thái</strong> ngay trên thanh sửa nhanh phía trên. Nhấn <kbd>Ctrl+S</kbd> / <kbd>⌘S</kbd> để lưu nhanh.</span>
          </div>
        </div>

        <!-- ================================================ -->
        <!-- VIEW B: FILTERED COMPACT TABLE                   -->
        <!-- ================================================ -->
        <div v-else class="table-view-wrap">
          <div v-if="allVariantRows.length > 0" class="table-filter-bar">
            <div class="filter-group">
              <button
                type="button"
                class="filter-chip"
                :class="{ active: tableStatusFilter === 'all' }"
                @click="tableStatusFilter = 'all'"
              >
                Tất cả ({{ allVariantRows.length }})
              </button>
              <button
                v-if="newRowsCount > 0"
                type="button"
                class="filter-chip chip-green"
                :class="{ active: tableStatusFilter === 'new' }"
                @click="tableStatusFilter = 'new'"
              >
                Mới ({{ newRowsCount }})
              </button>
              <button
                v-if="modifiedRowsCount > 0"
                type="button"
                class="filter-chip chip-amber"
                :class="{ active: tableStatusFilter === 'modified' }"
                @click="tableStatusFilter = 'modified'"
              >
                Đã sửa ({{ modifiedRowsCount }})
              </button>
              <button
                type="button"
                class="filter-chip"
                :class="{ active: tableStatusFilter === 'out' }"
                @click="tableStatusFilter = 'out'"
              >
                Hết hàng ({{ outOfStockCount }})
              </button>

              <span class="filter-divider">|</span>

              <button
                type="button"
                class="filter-chip"
                :class="{ active: tableFilterColorId === null }"
                @click="tableFilterColorId = null"
              >
                Mọi màu
              </button>
              <button
                v-for="c in rowColorTabs"
                :key="c.id"
                type="button"
                class="filter-chip"
                :class="{ active: tableFilterColorId === c.id }"
                @click="tableFilterColorId = c.id"
              >
                <span class="chip-dot" :style="{ backgroundColor: c.hexCode }"></span>
                {{ c.colorName }} ({{ countTotalByColor(c.id) }})
              </button>
            </div>

            <el-input
              v-model="tableSearchSku"
              size="small"
              placeholder="Tìm nhanh SKU / Size..."
              clearable
              style="width: 190px"
            />
          </div>

          <div class="table-scroll-wrap">
            <el-table
              :data="filteredVariantRows"
              border
              size="small"
              max-height="520"
              :row-class-name="tableRowClassName"
              style="width: 100%"
            >
              <template #empty>
                <el-empty description="Không có biến thể nào khớp bộ lọc." :image-size="60" />
              </template>

              <el-table-column label="Loại" width="76" align="center">
                <template #default="{ row }">
                  <span v-if="row.isNew" class="row-badge badge-new">MỚI</span>
                  <span v-else-if="isRowModified(row)" class="row-badge badge-mod">ĐÃ SỬA</span>
                  <span v-else class="row-badge badge-db">ĐÃ CÓ</span>
                </template>
              </el-table-column>

              <el-table-column label="Size" width="112">
                <template #default="{ row }">
                  <QuickSelect
                    v-model="row.sizeId"
                    :options="sizes"
                    value-key="id"
                    :label-key="(s) => `Size ${s.sizeValue}`"
                    size="small"
                    search-placeholder="Tìm size..."
                    @change="refreshRowSku(row)"
                  />
                </template>
              </el-table-column>

              <el-table-column label="Màu sắc" min-width="165">
                <template #default="{ row }">
                  <QuickSelect
                    v-model="row.colorId"
                    :options="colors"
                    value-key="id"
                    :label-key="(c) => `${c.colorName} (${c.colorCode})`"
                    :search-keys="['colorName', 'colorCode']"
                    size="small"
                    search-placeholder="Tìm màu..."
                    @change="refreshRowSku(row)"
                  />
                </template>
              </el-table-column>

              <el-table-column label="Mã SKU" min-width="155">
                <template #default="{ row }">
                  <el-input v-model="row.sku" size="small" placeholder="VD: AF1-41-W" />
                  <div v-if="getRowError(row)" class="row-error-text">{{ getRowError(row) }}</div>
                </template>
              </el-table-column>

              <el-table-column label="Giá nhập" width="136">
                <template #default="{ row }">
                  <el-input-number v-model="row.importPrice" :min="0" :step="50000" size="small" controls-position="right" style="width: 100%" />
                </template>
              </el-table-column>

              <el-table-column label="Giá bán" width="136">
                <template #default="{ row }">
                  <el-input-number v-model="row.salePrice" :min="0" :step="50000" size="small" controls-position="right" style="width: 100%" />
                </template>
              </el-table-column>

              <el-table-column label="Tồn kho" width="96">
                <template #default="{ row }">
                  <el-input-number v-model="row.availableQuantity" :min="0" size="small" controls-position="right" style="width: 100%" />
                </template>
              </el-table-column>

              <el-table-column label="Trạng thái" width="108">
                <template #default="{ row }">
                  <el-select filterable v-model="row.status" size="small" style="width: 100%">
                    <el-option label="ACTIVE" value="ACTIVE" />
                    <el-option label="INACTIVE" value="INACTIVE" />
                  </el-select>
                </template>
              </el-table-column>

              <el-table-column label="Thao tác" width="80" align="center" fixed="right">
                <template #default="{ row }">
                  <div class="row-actions">
                    <button
                      v-if="row.isNew"
                      type="button"
                      class="action-btn danger"
                      title="Xóa dòng mới"
                      @click="removeRowByRef(row)"
                    >
                      <el-icon><Delete /></el-icon>
                    </button>
                    <button
                      v-else-if="isRowModified(row)"
                      type="button"
                      class="action-btn"
                      title="Hoàn tác dòng này"
                      @click="revertRow(row)"
                    >
                      ↺
                    </button>
                    <span v-else class="muted-dash">—</span>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Delete, Check, Grid, List, MagicStick } from '@element-plus/icons-vue'
import { variantService } from '@/services/variantService'
import { productService } from '@/services/productService'
import { formatCurrency } from '@/utils/formatters'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.params.id)
const focusedVariantId = ref(null)
const inspectedKey = ref(null) // `${sizeId}-${colorId}`
const loading = ref(false)
const saving = ref(false)
const showGenerator = ref(true)
const showFullGrid = ref(false)
const bulkScope = ref('all')

const products = ref([])
const sizes = ref([])
const colors = ref([])

const selectedProductId = ref(null)
const selectedSizeIds = ref([])
const selectedColorIds = ref([])

const allVariantRows = ref([])

const viewMode = ref('matrix')
const tableFilterColorId = ref(null)
const tableStatusFilter = ref('all')
const tableSearchSku = ref('')

const defaults = reactive({
  importPrice: 1800000,
  salePrice: 2950000,
  availableQuantity: 10,
  status: 'ACTIVE'
})

const selectedProduct = computed(() =>
  products.value.find((p) => p.id === selectedProductId.value) || null
)

function getSizeObj(sizeId) {
  return sizes.value.find((s) => s.id === sizeId)
}

function getColorObj(colorId) {
  return colors.value.find((c) => c.id === colorId)
}

function makeComboKey(sizeId, colorId) {
  return `${sizeId}-${colorId}`
}

const inspectedRow = computed(() => {
  if (!inspectedKey.value) return null
  return allVariantRows.value.find((r) => makeComboKey(r.sizeId, r.colorId) === inspectedKey.value) || null
})

const activeInspectColor = computed(() => {
  if (inspectedRow.value) return getColorObj(inspectedRow.value.colorId)
  if (tableFilterColorId.value) return getColorObj(tableFilterColorId.value)
  return null
})

function selectInspectRow(row) {
  if (!row) return
  inspectedKey.value = makeComboKey(row.sizeId, row.colorId)
  if (row.id) focusedVariantId.value = row.id
}

function removeInspectedRow() {
  if (!inspectedRow.value || !inspectedRow.value.isNew) return
  removeRowByRef(inspectedRow.value)
  inspectedKey.value = null
}

function isRowModified(row) {
  if (row.isNew || !row._orig) return false
  return (
    row.sizeId !== row._orig.sizeId ||
    row.colorId !== row._orig.colorId ||
    (row.sku || '').trim().toUpperCase() !== (row._orig.sku || '').trim().toUpperCase() ||
    Number(row.importPrice) !== Number(row._orig.importPrice) ||
    Number(row.salePrice) !== Number(row._orig.salePrice) ||
    Number(row.availableQuantity) !== Number(row._orig.availableQuantity) ||
    row.status !== row._orig.status
  )
}

const existingRowsCount = computed(() => allVariantRows.value.filter((r) => !r.isNew).length)
const newRowsCount = computed(() => allVariantRows.value.filter((r) => r.isNew).length)
const modifiedRowsCount = computed(() => allVariantRows.value.filter((r) => isRowModified(r)).length)
const outOfStockCount = computed(() => allVariantRows.value.filter((r) => Number(r.availableQuantity || 0) === 0).length)
const totalPendingChanges = computed(() => newRowsCount.value + modifiedRowsCount.value)

const saveButtonLabel = computed(() => {
  if (totalPendingChanges.value === 0) {
    return isEdit.value ? 'ĐÃ ĐỒNG BỘ' : 'CHƯA CÓ SKU MỚI'
  }
  const parts = []
  if (modifiedRowsCount.value > 0) parts.push(`SỬA ${modifiedRowsCount.value}`)
  if (newRowsCount.value > 0) parts.push(`THÊM ${newRowsCount.value}`)
  return `LƯU (${parts.join(' + ')})`
})

const pendingGenerateCount = computed(() => {
  let count = 0
  for (const sId of selectedSizeIds.value) {
    for (const cId of selectedColorIds.value) {
      if (!findRow(sId, cId)) count++
    }
  }
  return count
})

const alreadyExistsInSelection = computed(() =>
  selectedSizeIds.value.length * selectedColorIds.value.length - pendingGenerateCount.value
)

const matrixSizes = computed(() => {
  if (showFullGrid.value) return sizes.value
  const idSet = new Set([
    ...selectedSizeIds.value,
    ...allVariantRows.value.map((r) => r.sizeId)
  ])
  if (idSet.size === 0) return []
  return sizes.value.filter((s) => idSet.has(s.id))
})

const matrixColors = computed(() => {
  if (showFullGrid.value) return colors.value
  const idSet = new Set([
    ...selectedColorIds.value,
    ...allVariantRows.value.map((r) => r.colorId)
  ])
  if (idSet.size === 0) return []
  return colors.value.filter((c) => idSet.has(c.id))
})

const rowColorTabs = computed(() => {
  const idSet = new Set(allVariantRows.value.map((r) => r.colorId))
  return colors.value.filter((c) => idSet.has(c.id))
})

const filteredVariantRows = computed(() => {
  return allVariantRows.value.filter((row) => {
    if (tableStatusFilter.value === 'new' && !row.isNew) return false
    if (tableStatusFilter.value === 'modified' && !isRowModified(row)) return false
    if (tableStatusFilter.value === 'out' && Number(row.availableQuantity || 0) !== 0) return false

    if (tableFilterColorId.value !== null && row.colorId !== tableFilterColorId.value) {
      return false
    }
    if (tableSearchSku.value && tableSearchSku.value.trim()) {
      const q = tableSearchSku.value.trim().toLowerCase()
      const s = getSizeObj(row.sizeId)
      const c = getColorObj(row.colorId)
      const hay = `${row.sku || ''} ${s?.sizeValue || ''} ${c?.colorName || ''}`.toLowerCase()
      return hay.includes(q)
    }
    return true
  })
})

const totalStockQty = computed(() =>
  allVariantRows.value.reduce((acc, r) => acc + Number(r.availableQuantity || 0), 0)
)

function formatShortPrice(val) {
  const n = Number(val || 0)
  if (n >= 1000000) {
    const m = n / 1000000
    return `${Number.isInteger(m) ? m : m.toFixed(2).replace(/0$/, '')}tr`
  }
  if (n >= 1000) return `${Math.round(n / 1000)}k`
  return `${n}`
}

function toggleId(arr, id) {
  const idx = arr.indexOf(id)
  if (idx >= 0) arr.splice(idx, 1)
  else arr.push(id)
}

function selectPopularSizes() {
  selectedSizeIds.value = sizes.value
    .filter((s) => {
      const num = parseFloat(s.sizeValue)
      return num >= 38 && num <= 43
    })
    .map((s) => s.id)
}

function selectAllSizes() {
  selectedSizeIds.value = sizes.value.map((s) => s.id)
}

function selectAllColors() {
  selectedColorIds.value = colors.value.map((c) => c.id)
}

function buildSku(product, sizeId, colorId) {
  const s = getSizeObj(sizeId)
  const c = getColorObj(colorId)
  if (product && s && c) {
    return `${product.productCode}-${s.sizeValue}-${c.colorCode}`.toUpperCase()
  }
  return ''
}

function refreshRowSku(row) {
  if (!selectedProduct.value || !row.isNew) return
  const generated = buildSku(selectedProduct.value, row.sizeId, row.colorId)
  if (generated) row.sku = generated
}

function findRow(sizeId, colorId) {
  return allVariantRows.value.find((r) => r.sizeId === sizeId && r.colorId === colorId)
}

function countTotalByColor(colorId) {
  return allVariantRows.value.filter((r) => r.colorId === colorId).length
}

function sumStockByColor(colorId) {
  return allVariantRows.value
    .filter((r) => r.colorId === colorId)
    .reduce((acc, r) => acc + Number(r.availableQuantity || 0), 0)
}

function countNewByColor(colorId) {
  return allVariantRows.value.filter((r) => r.isNew && r.colorId === colorId).length
}

function countTotalBySize(sizeId) {
  return allVariantRows.value.filter((r) => r.sizeId === sizeId).length
}

function getCellClass(row) {
  const isSelected = inspectedKey.value === makeComboKey(row.sizeId, row.colorId)
  return {
    'cell-new': row.isNew,
    'cell-modified': !row.isNew && isRowModified(row),
    'cell-existing': !row.isNew && !isRowModified(row),
    'cell-inactive': row.status === 'INACTIVE',
    'cell-error': !!getRowError(row),
    'cell-selected': isSelected
  }
}

function getInspectorClass(row) {
  if (row.isNew) return 'insp-new'
  if (isRowModified(row)) return 'insp-mod'
  return 'insp-exist'
}

function getCellTooltip(row) {
  const tag = row.isNew ? '[MỚI]' : isRowModified(row) ? '[ĐÃ SỬA]' : '[ĐÃ CÓ]'
  return `${tag} ${row.sku} | Nhập: ${formatCurrency(row.importPrice)} | Bán: ${formatCurrency(row.salePrice)} | TT: ${row.status}`
}

function tableRowClassName({ row }) {
  if (row.id && row.id === focusedVariantId.value) return 'focused-table-row'
  if (row.isNew) return 'new-table-row'
  if (isRowModified(row)) return 'modified-table-row'
  return ''
}

function onMatrixQtyInput(sizeId, colorId, rawVal) {
  const row = findRow(sizeId, colorId)
  if (!row) return
  const num = Number(rawVal)
  row.availableQuantity = Number.isFinite(num) && num >= 0 ? Math.floor(num) : 0
}

function createDraftRow(sizeId, colorId) {
  return {
    id: null,
    isNew: true,
    sizeId,
    colorId,
    sku: buildSku(selectedProduct.value, sizeId, colorId),
    importPrice: defaults.importPrice,
    salePrice: defaults.salePrice,
    availableQuantity: defaults.availableQuantity,
    status: defaults.status
  }
}

function addSingleCombo(sizeId, colorId) {
  if (!selectedProduct.value || findRow(sizeId, colorId)) return
  const row = createDraftRow(sizeId, colorId)
  allVariantRows.value.push(row)
  selectInspectRow(row)
}

function fillColorRow(colorId) {
  if (!selectedProduct.value) return
  let added = 0
  for (const s of matrixSizes.value) {
    if (!findRow(s.id, colorId)) {
      allVariantRows.value.push(createDraftRow(s.id, colorId))
      added++
    }
  }
  if (added > 0) ElMessage.success(`Đã thêm nhanh ${added} size cho màu này`)
}

function fillSizeColumn(sizeId) {
  if (!selectedProduct.value) return
  let added = 0
  for (const c of matrixColors.value) {
    if (!findRow(sizeId, c.id)) {
      allVariantRows.value.push(createDraftRow(sizeId, c.id))
      added++
    }
  }
  if (added > 0) ElMessage.success(`Đã thêm nhanh ${added} màu cho size này`)
}

function removeNewByCombo(sizeId, colorId) {
  const idx = allVariantRows.value.findIndex((r) => r.isNew && r.sizeId === sizeId && r.colorId === colorId)
  if (idx >= 0) {
    if (inspectedKey.value === makeComboKey(sizeId, colorId)) inspectedKey.value = null
    allVariantRows.value.splice(idx, 1)
  }
}

function removeNewColorRows(colorId) {
  allVariantRows.value = allVariantRows.value.filter((r) => !(r.isNew && r.colorId === colorId))
  if (inspectedRow.value === null) inspectedKey.value = null
}

function clearNewRows() {
  allVariantRows.value = allVariantRows.value.filter((r) => !r.isNew)
  if (inspectedRow.value === null) inspectedKey.value = null
}

function removeRowByRef(row) {
  if (!row.isNew) return
  const idx = allVariantRows.value.indexOf(row)
  if (idx >= 0) allVariantRows.value.splice(idx, 1)
}

function revertRow(row) {
  if (!row._orig) return
  row.sizeId = row._orig.sizeId
  row.colorId = row._orig.colorId
  row.sku = row._orig.sku
  row.importPrice = row._orig.importPrice
  row.salePrice = row._orig.salePrice
  row.availableQuantity = row._orig.availableQuantity
  row.status = row._orig.status
}

function revertAllChanges() {
  allVariantRows.value = allVariantRows.value.filter((r) => !r.isNew)
  allVariantRows.value.forEach((r) => revertRow(r))
  if (inspectedRow.value === null) inspectedKey.value = null
  ElMessage.info('Đã hoàn tác toàn bộ thay đổi chưa lưu')
}

function getScopedRows() {
  if (bulkScope.value === 'new') {
    return allVariantRows.value.filter((r) => r.isNew)
  }
  if (bulkScope.value === 'color' && activeInspectColor.value) {
    return allVariantRows.value.filter((r) => r.colorId === activeInspectColor.value.id)
  }
  return allVariantRows.value
}

function applyPricesByScope() {
  const targets = getScopedRows()
  if (targets.length === 0) {
    ElMessage.warning('Không có dòng nào thuộc phạm vi đang chọn')
    return
  }
  targets.forEach((row) => {
    row.importPrice = defaults.importPrice
    row.salePrice = defaults.salePrice
  })
  ElMessage.success(`Đã áp giá nhập & giá bán cho ${targets.length} SKU`)
}

function applyStockByScope() {
  const targets = getScopedRows()
  if (targets.length === 0) {
    ElMessage.warning('Không có dòng nào thuộc phạm vi đang chọn')
    return
  }
  targets.forEach((row) => {
    row.availableQuantity = defaults.availableQuantity
    row.status = defaults.status
  })
  ElMessage.success(`Đã áp tồn kho & trạng thái cho ${targets.length} SKU`)
}

function getRowError(row) {
  if (!row.sizeId || !row.colorId) return 'Thiếu Size hoặc Màu'
  if (!row.sku || !row.sku.trim()) return 'Chưa nhập mã SKU'

  const dupCombo = allVariantRows.value.filter(
    (r) => r.sizeId === row.sizeId && r.colorId === row.colorId
  ).length
  if (dupCombo > 1) return 'Trùng tổ hợp Size + Màu'

  const normalizedSku = row.sku.trim().toUpperCase()
  const dupSku = allVariantRows.value.filter(
    (r) => (r.sku || '').trim().toUpperCase() === normalizedSku
  ).length
  if (dupSku > 1) return 'Trùng mã SKU'

  return ''
}

function mapExistingToRow(ev) {
  const orig = {
    sizeId: ev.sizeId,
    colorId: ev.colorId,
    sku: ev.sku,
    importPrice: Number(ev.importPrice || 0),
    salePrice: Number(ev.salePrice || 0),
    availableQuantity: Number(ev.availableQuantity || 0),
    status: ev.status || 'ACTIVE'
  }
  return {
    id: ev.id,
    isNew: false,
    ...orig,
    _orig: { ...orig }
  }
}

async function handleProductChange(productId) {
  allVariantRows.value = []
  tableFilterColorId.value = null
  tableStatusFilter.value = 'all'
  tableSearchSku.value = ''
  inspectedKey.value = null
  if (!productId) return

  const p = products.value.find((x) => x.id === productId)
  if (p) {
    defaults.importPrice = Number(p.importPrice || 0)
    defaults.salePrice = Number(p.salePrice || 0)
  }

  try {
    const res = await variantService.getVariantsByProduct(productId)
    const list = res.data || []
    allVariantRows.value = list.map(mapExistingToRow)

    // Auto-select focused variant in Quick Inspector when editing
    if (focusedVariantId.value) {
      const target = allVariantRows.value.find((r) => r.id === focusedVariantId.value)
      if (target) selectInspectRow(target)
    }
  } catch (_) {
    allVariantRows.value = []
  }
}

function generateMatrixVariants() {
  if (!selectedProduct.value) {
    ElMessage.warning('Vui lòng chọn sản phẩm trước')
    return
  }
  let added = 0
  let skipped = 0
  let firstNew = null

  for (const sizeId of selectedSizeIds.value) {
    for (const colorId of selectedColorIds.value) {
      if (findRow(sizeId, colorId)) {
        skipped++
        continue
      }
      const row = createDraftRow(sizeId, colorId)
      allVariantRows.value.push(row)
      if (!firstNew) firstNew = row
      added++
    }
  }

  if (added > 0) {
    if (firstNew) selectInspectRow(firstNew)
    showGenerator.value = false
    ElMessage.success(
      `Đã tạo ${added} biến thể mới` + (skipped > 0 ? ` (bỏ qua ${skipped} tổ hợp đã có)` : '')
    )
  } else {
    ElMessage.info('Tất cả tổ hợp Size × Màu đã có sẵn trên ma trận rồi!')
  }
}

function addManualRow() {
  if (!selectedProduct.value) return
  let pickSize = sizes.value[0]?.id || null
  let pickColor = colors.value[0]?.id || null
  outer: for (const s of sizes.value) {
    for (const c of colors.value) {
      if (!findRow(s.id, c.id)) {
        pickSize = s.id
        pickColor = c.id
        break outer
      }
    }
  }
  const row = createDraftRow(pickSize, pickColor)
  allVariantRows.value.push(row)
  selectInspectRow(row)
  viewMode.value = 'table'
}

async function submitAllChanges() {
  if (!selectedProductId.value) {
    ElMessage.warning('Vui lòng chọn sản phẩm Sneaker')
    return
  }

  const rowsToCreate = allVariantRows.value.filter((r) => r.isNew)
  const rowsToUpdate = allVariantRows.value.filter((r) => !r.isNew && isRowModified(r))

  if (rowsToCreate.length === 0 && rowsToUpdate.length === 0) {
    ElMessage.info('Chưa có thay đổi nào cần lưu')
    return
  }

  const invalidRow = [...rowsToUpdate, ...rowsToCreate].find((row) => !!getRowError(row))
  if (invalidRow) {
    selectInspectRow(invalidRow)
    ElMessage.error(`Lỗi dòng SKU "${invalidRow.sku || '(trống)'}": ${getRowError(invalidRow)}`)
    return
  }

  saving.value = true
  let updatedCount = 0
  let createdCount = 0
  let errorCount = 0

  try {
    for (const row of rowsToUpdate) {
      try {
        await variantService.updateVariant(row.id, {
          productId: selectedProductId.value,
          sizeId: row.sizeId,
          colorId: row.colorId,
          sku: row.sku.trim().toUpperCase(),
          importPrice: row.importPrice,
          salePrice: row.salePrice,
          availableQuantity: row.availableQuantity,
          status: row.status
        })
        updatedCount++
      } catch (_) {
        errorCount++
      }
    }

    for (const row of rowsToCreate) {
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
        createdCount++
      } catch (_) {
        errorCount++
      }
    }

    if (errorCount === 0) {
      const msgs = []
      if (updatedCount > 0) msgs.push(`cập nhật ${updatedCount} SKU`)
      if (createdCount > 0) msgs.push(`thêm mới ${createdCount} SKU`)
      ElMessage.success(`Đã ${msgs.join(' và ')} thành công!`)
      router.push('/admin/variants')
    } else {
      await handleProductChange(selectedProductId.value)
      ElMessage.warning(
        `Hoàn tất: sửa ${updatedCount}, thêm ${createdCount}, lỗi ${errorCount} dòng — vui lòng kiểm tra lại!`
      )
    }
  } finally {
    saving.value = false
  }
}

function handleKeydown(e) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 's') {
    e.preventDefault()
    if (!saving.value && totalPendingChanges.value > 0) {
      submitAllChanges()
    }
  }
}

onMounted(async () => {
  window.addEventListener('keydown', handleKeydown)
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
      showGenerator.value = false
      focusedVariantId.value = Number(route.params.id)
      const vRes = await variantService.getVariantById(route.params.id)
      const v = vRes.data
      if (v) {
        selectedProductId.value = v.productId
        await handleProductChange(v.productId)
      }
    } else {
      showGenerator.value = true
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

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped lang="scss">
.variant-studio-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 24px;
}

/* ========================================================= */
/* 1. STICKY TOP COMMAND BAR                                 */
/* ========================================================= */
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
  transition: all 0.14s ease;
  flex-shrink: 0;

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

.product-picker-inline {
  width: 300px;
  max-width: 100%;

  .product-select {
    width: 100%;
  }
}

.product-mini-meta {
  display: flex;
  align-items: center;
  gap: 8px;

  .mini-thumb {
    width: 32px;
    height: 32px;
    object-fit: cover;
    border: 1px solid #e5e5e5;
    flex-shrink: 0;
  }

  .mini-kpi-group {
    display: flex;
    align-items: center;
    gap: 5px;
    flex-wrap: wrap;
  }

  .kpi-chip {
    font-size: 11px;
    color: #525252;
    background: #f5f5f5;
    border: 1px solid #e5e5e5;
    padding: 3px 7px;
    font-variant-numeric: tabular-nums;

    strong {
      color: #303133;
      font-weight: 800;
    }

    &.kpi-amber {
      background: #fffbeb;
      border-color: #fcd34d;
      color: #92400e;
      strong { color: #b45309; }
    }

    &.kpi-green {
      background: #ecfdf5;
      border-color: #6ee7b7;
      color: #065f46;
      strong { color: #047857; }
    }

    &.kpi-stock {
      background: #ffffff;
      border-color: #d4d4d4;
    }
  }
}

.btn-ghost {
  background: #ffffff;
  border: 1px solid #d4d4d4;
  color: #404040;
  font-size: 11.5px;
  font-weight: 700;
  padding: 7px 11px;
  cursor: pointer;

  &:hover {
    border-color: var(--vea-theme, #409eff);
    color: #303133;
  }
}

.btn-save-primary {
  font-weight: 800;
  letter-spacing: 0.04em;
}

.product-option-row {
  display: flex;
  align-items: center;
  gap: 8px;

  .p-name {
    flex: 1;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .p-price {
    font-weight: 800;
    color: #303133;
    font-size: 12px;
  }
}

/* ========================================================= */
/* 2. STUDIO CARD & COMPACT CONTROL DOCK                     */
/* ========================================================= */
.studio-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
}

.control-dock {
  padding: 12px 14px;
}

.bulk-strip {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.bulk-strip-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;

  .badge-black {
    background: var(--vea-theme, #409eff);
    color: #ffffff;
    font-size: 9.5px;
    font-weight: 900;
    letter-spacing: 0.08em;
    padding: 4px 8px;
  }

  .btn-toggle-gen {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    background: #fafafa;
    border: 1px solid var(--vea-theme, #409eff);
    color: #303133;
    font-size: 11px;
    font-weight: 800;
    padding: 5px 10px;
    cursor: pointer;
    transition: all 0.14s ease;

    &.open,
    &:hover {
      background: var(--vea-theme, #409eff);
      color: #ffffff;
    }

    .caret {
      font-size: 9px;
      opacity: 0.8;
    }
  }
}

.bulk-inputs {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;

  .compact-field {
    display: flex;
    flex-direction: column;
    gap: 3px;
    width: 132px;

    &.qty-field {
      width: 92px;
    }

    &.status-field {
      width: 108px;
    }

    label {
      font-size: 10px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: #737373;
    }

    :deep(.el-input-number),
    :deep(.el-select) {
      width: 100%;
    }
  }
}

.bulk-apply-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;

  .scope-select {
    width: 158px;
  }

  .btn-mini-apply {
    background: #ffffff;
    border: 1px solid var(--vea-theme, #409eff);
    color: #303133;
    font-size: 11px;
    font-weight: 800;
    padding: 5px 10px;
    height: 28px;
    cursor: pointer;
    white-space: nowrap;

    &:hover:not(:disabled) {
      background: var(--vea-theme, #409eff);
      color: #ffffff;
    }

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }
}

/* Collapsible Generator Drawer */
.generator-drawer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #d4d4d4;
}

.gen-grid {
  display: grid;
  grid-template-columns: 1fr 1.25fr;
  gap: 16px;
}

.gen-col-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 7px;

  .gen-label {
    font-size: 11px;
    font-weight: 900;
    letter-spacing: 0.05em;
    color: #303133;

    em {
      font-style: normal;
      font-weight: 700;
      color: #737373;
    }
  }

  .gen-quick {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 11px;
    color: #a3a3a3;

    button {
      background: none;
      border: none;
      padding: 0;
      color: #303133;
      font-weight: 700;
      font-size: 11px;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.chip-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.size-chip {
  min-width: 40px;
  height: 30px;
  padding: 0 8px;
  background: #ffffff;
  border: 1px solid #d4d4d4;
  color: #303133;
  font-size: 12px;
  font-weight: 800;
  font-family: var(--font-mono);
  cursor: pointer;
  transition: all 0.12s ease;

  &:hover {
    border-color: var(--vea-theme, #409eff);
  }

  &.active {
    background: var(--vea-theme, #409eff);
    border-color: var(--vea-theme, #409eff);
    color: #ffffff;
  }
}

.color-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 9px;
  background: #ffffff;
  border: 1px solid #d4d4d4;
  color: #303133;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.12s ease;

  .swatch {
    width: 11px;
    height: 11px;
    border: 1px solid rgba(0, 0, 0, 0.25);
    flex-shrink: 0;
  }

  small {
    font-size: 10px;
    font-family: var(--font-mono);
    opacity: 0.65;
  }

  &:hover {
    border-color: var(--vea-theme, #409eff);
  }

  &.active {
    background: var(--vea-theme, #409eff);
    border-color: var(--vea-theme, #409eff);
    color: #ffffff;

    small {
      opacity: 0.85;
    }
  }
}

.gen-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;

  .gen-preview-hint {
    font-size: 12px;
    color: #404040;

    .muted {
      color: #737373;
      margin-left: 4px;
    }
  }

  .gen-btns {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
}

.btn-secondary-sm {
  background: #ffffff;
  border: 1px solid #d4d4d4;
  color: #303133;
  font-size: 11.5px;
  font-weight: 700;
  padding: 7px 12px;
  cursor: pointer;

  &:hover {
    border-color: var(--vea-theme, #409eff);
  }
}

.btn-generate-now {
  background: var(--vea-theme, #409eff);
  color: #ffffff;
  border: 1px solid var(--vea-theme, #409eff);
  padding: 7px 16px;
  font-size: 11.5px;
  font-weight: 900;
  letter-spacing: 0.05em;
  cursor: pointer;

  &:hover:not(:disabled) {
    background: var(--vea-theme-light-3, #66b1ff);
  }

  &:disabled {
    background: #a3a3a3;
    border-color: #a3a3a3;
    cursor: not-allowed;
  }
}

/* ========================================================= */
/* 3. WORKSPACE TOOLBAR & INLINE QUICK INSPECTOR             */
/* ========================================================= */
.workspace-card {
  padding: 12px 14px;
}

.workspace-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.ws-left,
.ws-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.view-switch {
  display: inline-flex;
  border: 1px solid var(--vea-theme, #409eff);

  .vs-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 5px 11px;
    background: #ffffff;
    color: #303133;
    border: none;
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    cursor: pointer;

    &.active {
      background: var(--vea-theme, #409eff);
      color: #ffffff;
    }
  }
}

.full-grid-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  color: #262626;
  cursor: pointer;
  user-select: none;

  input {
    accent-color: #000000;
    width: 14px;
    height: 14px;
    cursor: pointer;
  }
}

.matrix-legend-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  color: #525252;

  .leg {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .dot {
    width: 9px;
    height: 9px;
    border: 1px solid var(--vea-theme, #409eff);
    display: inline-block;

    &.dot-exist { background: #eff6ff; border-color: #3b82f6; }
    &.dot-mod { background: #fffbeb; border-color: #f59e0b; }
    &.dot-new { background: #ecfdf5; border-color: #10b981; }
    &.dot-off { background: #f5f5f5; border-color: #a3a3a3; border-style: dashed; }
  }
}

.btn-danger-ghost {
  background: #fff5f5;
  border: 1px solid #fca5a5;
  color: #dc2626;
  font-size: 11px;
  font-weight: 800;
  padding: 5px 10px;
  cursor: pointer;

  &:hover {
    background: #dc2626;
    color: #ffffff;
    border-color: #dc2626;
  }
}

/* Quick Inspector Bar */
.quick-inspector-bar {
  border: 1.5px solid var(--vea-theme, #409eff);
  background: #fafafa;
  padding: 10px 12px;
  margin-bottom: 10px;

  &.insp-new {
    border-color: #059669;
    background: #f0fdf4;
  }
  &.insp-mod {
    border-color: #d97706;
    background: #fffbeb;
  }
  &.insp-exist {
    border-color: #1d4ed8;
    background: #f8fafc;
  }
}

.inspector-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.insp-identity {
  display: flex;
  align-items: center;
  gap: 8px;

  .swatch-lg {
    width: 14px;
    height: 14px;
    border: 1px solid rgba(0, 0, 0, 0.3);
    flex-shrink: 0;
  }

  .insp-title {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    font-size: 12.5px;
  }

  .insp-error {
    font-size: 11px;
    font-weight: 800;
    color: #dc2626;
  }
}

.insp-profit {
  font-size: 11.5px;
  color: #525252;
  font-variant-numeric: tabular-nums;

  .text-green { color: #059669; }
  .text-red { color: #dc2626; }
}

.inspector-fields {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 0.75fr 0.85fr auto;
  gap: 8px;
  align-items: end;

  .insp-field {
    display: flex;
    flex-direction: column;
    gap: 3px;

    label {
      font-size: 10px;
      font-weight: 800;
      text-transform: uppercase;
      color: #525252;
    }
  }

  .insp-actions {
    display: flex;
    align-items: center;
    gap: 5px;
  }

  .btn-insp-act {
    height: 24px;
    padding: 0 9px;
    background: #ffffff;
    border: 1px solid var(--vea-theme, #409eff);
    color: #303133;
    font-size: 11px;
    font-weight: 800;
    cursor: pointer;

    &.danger {
      border-color: #dc2626;
      color: #dc2626;
      &:hover { background: #dc2626; color: #ffffff; }
    }
    &:hover {
      background: var(--vea-theme, #409eff);
      color: #ffffff;
    }
  }

  .btn-insp-close {
    width: 24px;
    height: 24px;
    background: transparent;
    border: 1px solid #d4d4d4;
    color: #525252;
    font-size: 15px;
    line-height: 1;
    cursor: pointer;

    &:hover {
      border-color: var(--vea-theme, #409eff);
      color: #303133;
    }
  }
}

/* ========================================================= */
/* 4. INTERACTIVE MATRIX TABLE                               */
/* ========================================================= */
.empty-matrix-box {
  padding: 36px 20px;
  text-align: center;
  border: 1px dashed #d4d4d4;
  background: #fafafa;

  .empty-title {
    margin: 0;
    font-size: 14px;
    font-weight: 800;
    color: #303133;
  }
  .empty-sub {
    margin: 6px 0 14px;
    font-size: 12px;
    color: #737373;
  }
  .empty-actions {
    display: flex;
    justify-content: center;
    gap: 10px;
    flex-wrap: wrap;
  }
}

.matrix-table-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.variant-matrix-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  background: #ffffff;

  th,
  td {
    border: 1px solid #e5e5e5;
    padding: 4px;
    text-align: center;
    vertical-align: middle;
  }

  thead th {
    background: var(--vea-theme, #409eff);
    color: #ffffff;
    padding: 7px 5px;

    .corner-head {
      display: flex;
      flex-direction: column;
      text-align: left;
      padding-left: 4px;

      span {
        font-size: 10.5px;
        font-weight: 900;
        letter-spacing: 0.06em;
      }
      small {
        font-size: 9.5px;
        color: #a3a3a3;
        font-weight: 600;
      }
    }

    &.size-col-head {
      min-width: 72px;

      .size-head-top {
        font-size: 12px;
        font-weight: 900;
        font-family: var(--font-mono);
      }

      .size-head-sub {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        margin-top: 2px;
        font-size: 9.5px;
        color: #a3a3a3;

        .btn-fill-axis {
          background: var(--vea-theme-light-3, #66b1ff);
          border: 1px solid #525252;
          color: #e5e5e5;
          font-size: 9px;
          font-weight: 700;
          padding: 0 4px;
          cursor: pointer;

          &:hover {
            background: #ffffff;
            color: #303133;
            border-color: #ffffff;
          }
        }
      }
    }

    &.row-total-head {
      min-width: 56px;
      background: var(--vea-theme-light-3, #66b1ff);
      font-size: 10.5px;
      font-weight: 900;
    }
  }

  .sticky-color-col {
    position: sticky;
    left: 0;
    z-index: 2;
    min-width: 196px;
    text-align: left;
    background: #fafafa;
  }

  thead .sticky-color-col {
    background: var(--vea-theme, #409eff);
    z-index: 3;
  }

  .matrix-color-cell {
    display: flex;
    align-items: center;
    gap: 7px;
    padding: 2px 4px;

    .color-swatch {
      width: 13px;
      height: 13px;
      border: 1px solid rgba(0, 0, 0, 0.28);
      flex-shrink: 0;
    }

    .color-meta {
      display: flex;
      flex-direction: column;
      line-height: 1.2;
      min-width: 0;
      flex: 1;

      strong {
        font-size: 11.5px;
        color: #303133;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      span {
        font-size: 9.5px;
        color: #737373;
        font-family: var(--font-mono);
      }
    }

    .color-row-acts {
      display: flex;
      align-items: center;
      gap: 3px;
      flex-shrink: 0;
    }

    .btn-row-fill {
      background: #ffffff;
      border: 1px solid #d4d4d4;
      color: #404040;
      font-size: 9.5px;
      font-weight: 800;
      padding: 2px 5px;
      cursor: pointer;

      &:hover {
        border-color: var(--vea-theme, #409eff);
        background: var(--vea-theme, #409eff);
        color: #ffffff;
      }
    }

    .btn-clear-row {
      background: transparent;
      border: 1px solid #e5e5e5;
      color: #737373;
      width: 18px;
      height: 18px;
      font-size: 9.5px;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;

      &:hover {
        border-color: #ef4444;
        color: #ef4444;
        background: #fef2f2;
      }
    }
  }

  .cell-box {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 1px solid #d4d4d4;
    padding: 3px 2px 2px;
    cursor: pointer;
    transition: all 0.12s ease;

    &.cell-existing {
      background: #eff6ff;
      border-color: #93c5fd;
      .cell-qty-input { color: #1e3a8a; }
    }

    &.cell-modified {
      background: #fffbeb;
      border-color: #f59e0b;
      .cell-qty-input { color: #92400e; }
    }

    &.cell-new {
      background: #ecfdf5;
      border-color: #10b981;
      .cell-qty-input { color: #065f46; }
    }

    &.cell-inactive {
      opacity: 0.55;
      border-style: dashed;
    }

    &.cell-error {
      border-color: #dc2626 !important;
      background: #fef2f2 !important;
    }

    &.cell-selected {
      outline: 2px solid var(--vea-theme, #409eff);
      outline-offset: -1px;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
    }

    .cell-qty-input {
      width: 100%;
      border: none;
      background: transparent;
      text-align: center;
      font-size: 13px;
      font-weight: 900;
      padding: 1px 12px 0 4px;
      outline: none;
      font-family: var(--font-mono);

      &::-webkit-inner-spin-button,
      &::-webkit-outer-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }
    }

    .cell-price-mini {
      font-size: 9px;
      font-weight: 700;
      color: #64748b;
      font-family: var(--font-mono);
      line-height: 1.1;
    }

    .cell-corner-btn {
      position: absolute;
      top: 1px;
      right: 1px;
      width: 14px;
      height: 14px;
      border: none;
      background: transparent;
      font-size: 11px;
      line-height: 1;
      cursor: pointer;
      padding: 0;

      &.remove {
        color: #059669;
        &:hover { color: #dc2626; }
      }
      &.revert {
        color: #d97706;
        &:hover { color: #303133; }
      }
    }
  }

  .cell-add-btn {
    width: 100%;
    height: 36px;
    background: #ffffff;
    border: 1px dashed #e5e5e5;
    color: #d4d4d4;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.12s ease;

    &:hover {
      border-color: var(--vea-theme, #409eff);
      color: #303133;
      background: #f5f5f5;
    }
  }

  .row-total-cell {
    background: #fafafa;
    font-family: var(--font-mono);
    line-height: 1.2;

    strong {
      display: block;
      font-size: 12px;
      color: #303133;
    }
    small {
      font-size: 9.5px;
      color: #737373;
    }
  }
}

.matrix-footer-tip {
  margin-top: 8px;
  font-size: 11px;
  color: #737373;

  kbd {
    background: #f5f5f5;
    border: 1px solid #d4d4d4;
    padding: 1px 4px;
    font-size: 10px;
    font-family: var(--font-mono);
    color: #303133;
  }
}

/* ========================================================= */
/* 5. TABLE VIEW FILTERS & BADGES                            */
/* ========================================================= */
.table-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;

  .filter-group {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 5px;
  }

  .filter-divider {
    color: #d4d4d4;
    margin: 0 2px;
  }

  .filter-chip {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 4px 9px;
    background: #ffffff;
    border: 1px solid #d4d4d4;
    font-size: 11px;
    font-weight: 700;
    color: #262626;
    cursor: pointer;

    &.active {
      background: var(--vea-theme, #409eff);
      border-color: var(--vea-theme, #409eff);
      color: #ffffff;
    }

    &.chip-green:not(.active) {
      border-color: #6ee7b7;
      color: #047857;
      background: #ecfdf5;
    }

    &.chip-amber:not(.active) {
      border-color: #fcd34d;
      color: #b45309;
      background: #fffbeb;
    }

    .chip-dot {
      width: 8px;
      height: 8px;
      border: 1px solid rgba(0, 0, 0, 0.25);
      display: inline-block;
    }
  }
}

.row-badge {
  display: inline-block;
  font-size: 9.5px;
  font-weight: 900;
  padding: 2px 6px;
  letter-spacing: 0.04em;

  &.badge-new {
    background: #ecfdf5;
    color: #047857;
    border: 1px solid #10b981;
  }
  &.badge-mod {
    background: #fffbeb;
    color: #b45309;
    border: 1px solid #f59e0b;
  }
  &.badge-db {
    background: #f3f4f6;
    color: #4b5563;
    border: 1px solid #d1d5db;
  }
}

.table-scroll-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;

  :deep(.focused-table-row td) {
    background-color: #fef9c3 !important;
  }
  :deep(.new-table-row td) {
    background-color: #f0fdf4 !important;
  }
  :deep(.modified-table-row td) {
    background-color: #fffbeb !important;
  }
}

.row-error-text {
  color: #dc2626;
  font-size: 10.5px;
  font-weight: 700;
  margin-top: 2px;
}

.muted-dash {
  color: #cbd5e1;
  font-weight: 700;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.16s ease;
}
.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ========================================================= */
/* 6. RESPONSIVE MOBILE / TABLET                             */
/* ========================================================= */
@media (max-width: 1100px) {
  .inspector-fields {
    grid-template-columns: repeat(3, 1fr);
  }
  .gen-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .studio-topbar {
    position: static;
    padding: 10px;
  }

  .product-picker-inline {
    width: 100%;
  }

  .topbar-right {
    width: 100%;
    .btn-save-primary {
      flex: 1;
    }
  }

  .bulk-inputs .compact-field {
    flex: 1 1 calc(50% - 6px);
    width: auto !important;
  }

  .bulk-apply-group {
    width: 100%;
    .scope-select {
      flex: 1;
    }
  }

  .inspector-fields {
    grid-template-columns: 1fr 1fr;
    .sku-f {
      grid-column: span 2;
    }
    .insp-actions {
      grid-column: span 2;
      justify-content: flex-end;
    }
  }
}
</style>
