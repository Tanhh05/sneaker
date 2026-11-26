<template>
  <div class="page-container invoice-detail-page" v-loading="loading">
    <!-- Top Header Bar -->
    <div class="page-header">
      <div class="header-left">
        <el-button class="btn-soft" @click="$router.push('/admin/invoices')">
          <el-icon><ArrowLeft /></el-icon>
          <span>Danh sách hóa đơn</span>
        </el-button>
        <div v-if="invoice" class="title-block">
          <div class="code-row">
            <h2>{{ invoice.invoiceCode }}</h2>
            <span class="type-pill" :class="invoice.invoiceType === 'VAT' ? 'type-vat' : 'type-retail'">
              {{ invoice.invoiceType === 'VAT' ? 'HÓA ĐƠN VAT' : 'HÓA ĐƠN BÁN LẺ' }}
            </span>
            <span class="status-pill" :class="`st-${(invoice.invoiceStatus || '').toLowerCase()}`">
              {{ invoiceStatusLabel(invoice.invoiceStatus) }}
            </span>
          </div>
          <div class="sub-row">
            Đơn hàng nguồn:
            <router-link :to="`/admin/orders/${invoice.orderId}`" class="ref-link">{{ invoice.orderCode }}</router-link>
            <span v-if="invoice.originalInvoiceId">
              &middot; Hóa đơn gốc:
              <router-link :to="`/admin/invoices/${invoice.originalInvoiceId}`" class="ref-link">
                {{ invoice.originalInvoiceCode }}
              </router-link>
            </span>
          </div>
        </div>
      </div>

      <div v-if="invoice" class="header-actions">
        <el-button class="btn-soft" @click="printVisible = true">
          <el-icon><Printer /></el-icon>
          <span>Xem trước &amp; In</span>
        </el-button>
        <el-button class="btn-soft" :loading="downloadingPdf" @click="handleDownloadPdf">
          <el-icon><Download /></el-icon>
          <span>Tải PDF</span>
        </el-button>
        <el-button type="primary" class="btn-primary-dark" @click="openSendEmail">
          <el-icon><Message /></el-icon>
          <span>{{ invoice.emailStatus === 'NOT_SENT' ? 'Gửi Email' : 'Gửi lại Email' }}</span>
        </el-button>
        <el-button
          v-if="canCancelDirect && invoice.invoiceStatus !== 'CANCELLED'"
          type="danger"
          plain
          @click="handleCancelDirect"
        >
          Hủy hóa đơn
        </el-button>
        <el-button
          v-else-if="!canCancelDirect && invoice.invoiceStatus === 'ISSUED'"
          type="warning"
          plain
          @click="handleRequestCancel"
        >
          Xin duyệt hủy
        </el-button>
      </div>
    </div>

    <!-- Pending Cancel Banner on Detail -->
    <div
      v-if="invoice && invoice.invoiceStatus === 'PENDING_CANCEL' && invoice.pendingCancelRequestId"
      class="cancel-review-banner"
    >
      <div>
        <strong>Yêu cầu hủy hóa đơn đang chờ duyệt:</strong>
        Nhân viên <strong>{{ invoice.pendingCancelRequestedBy }}</strong> đề nghị hủy với lý do:
        "{{ invoice.pendingCancelReason }}"
      </div>
      <div v-if="canCancelDirect" class="banner-actions">
        <el-button size="small" type="danger" @click="approveCancel">Phê duyệt hủy</el-button>
        <el-button size="small" @click="rejectCancel">Từ chối</el-button>
      </div>
    </div>

    <template v-if="invoice">
      <!-- A & B: General Info & Customer Info -->
      <el-row :gutter="16">
        <el-col :xs="24" :lg="12">
          <div class="detail-card">
            <div class="card-title">A. Thông tin chung</div>
            <div class="kv-grid">
              <div class="kv-row"><span>Mã hóa đơn:</span><strong>{{ invoice.invoiceCode }}</strong></div>
              <div class="kv-row"><span>Mã đơn hàng nguồn:</span><router-link :to="`/admin/orders/${invoice.orderId}`" class="ref-link">{{ invoice.orderCode }}</router-link></div>
              <div class="kv-row"><span>Kênh bán hàng:</span><strong>{{ invoice.saleChannel === 'ONLINE' ? 'Online Store' : 'Tại quầy (POS)' }}</strong></div>
              <div class="kv-row"><span>Loại hóa đơn:</span><strong>{{ invoice.invoiceType === 'VAT' ? 'Hóa đơn VAT' : 'Hóa đơn bán lẻ' }} ({{ invoice.recordKind }})</strong></div>
              <div class="kv-row"><span>Ngày lập hóa đơn:</span><strong>{{ formatDateTime(invoice.issuedAt) }}</strong></div>
              <div class="kv-row"><span>Trạng thái hóa đơn:</span><strong>{{ invoiceStatusLabel(invoice.invoiceStatus) }}</strong></div>
              <div class="kv-row"><span>Người lập hóa đơn:</span><strong>{{ invoice.createdBy || 'SYSTEM' }}</strong></div>
              <div v-if="invoice.cancelReason" class="kv-row danger-row">
                <span>Lý do hủy ({{ invoice.cancelledBy }}):</span>
                <strong>{{ invoice.cancelReason }}</strong>
              </div>
              <div v-if="invoice.adjustmentReason" class="kv-row">
                <span>Lý do điều chỉnh:</span>
                <strong>{{ invoice.adjustmentReason }}</strong>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :xs="24" :lg="12">
          <div class="detail-card">
            <div class="card-title">B. Thông tin khách hàng &amp; Xuất hóa đơn</div>
            <div class="kv-grid">
              <div class="kv-row"><span>Tên khách hàng:</span><strong>{{ invoice.customerName }}</strong></div>
              <div class="kv-row"><span>Số điện thoại:</span><strong>{{ invoice.customerPhone || '---' }}</strong></div>
              <div class="kv-row"><span>Email nhận hóa đơn:</span><strong>{{ invoice.customerEmail || 'Chưa cung cấp' }}</strong></div>
              <div class="kv-row"><span>Địa chỉ:</span><span>{{ invoice.customerAddress || '---' }}</span></div>
              <div class="kv-row"><span>Tên công ty (VAT):</span><strong>{{ invoice.companyName || '---' }}</strong></div>
              <div class="kv-row"><span>Mã số thuế (MST):</span><code>{{ invoice.taxCode || '---' }}</code></div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- C: Product Details -->
      <div class="detail-card">
        <div class="card-title">C. Chi tiết sản phẩm</div>
        <el-table :data="invoice.details || []" border stripe style="width: 100%">
          <el-table-column prop="productCode" label="Mã SP" width="110" />
          <el-table-column label="Tên sản phẩm" min-width="220">
            <template #default="{ row }">
              <strong>{{ row.productName }}</strong>
              <div v-if="row.sku" style="font-size: 11.5px; color: #64748b">SKU: {{ row.sku }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="variantLabel" label="Biến thể" min-width="150" />
          <el-table-column prop="quantity" label="Số lượng" width="95" align="center" />
          <el-table-column label="Đơn giá" width="130" align="right">
            <template #default="{ row }">{{ formatCurrency(row.unitPrice) }}</template>
          </el-table-column>
          <el-table-column label="Giảm giá" width="115" align="right">
            <template #default="{ row }">{{ formatCurrency(row.discountAmount) }}</template>
          </el-table-column>
          <el-table-column label="Thuế VAT" width="120" align="right">
            <template #default="{ row }">
              {{ Number(row.taxRate || 0) }}% ({{ formatCurrency(row.taxAmount) }})
            </template>
          </el-table-column>
          <el-table-column label="Thành tiền" width="140" align="right">
            <template #default="{ row }">
              <strong>{{ formatCurrency(row.lineTotal) }}</strong>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- D & Related Adjustments -->
      <el-row :gutter="16">
        <el-col :xs="24" :lg="12">
          <div class="detail-card">
            <div class="card-title">D. Thông tin tài chính</div>
            <div class="finance-list">
              <div class="fin-row"><span>Tổng tiền hàng trước thuế (Subtotal)</span><strong>{{ formatCurrency(invoice.subtotal) }}</strong></div>
              <div class="fin-row"><span>Tổng chiết khấu / Giảm giá</span><span>-{{ formatCurrency(invoice.discountAmount) }}</span></div>
              <div class="fin-row"><span>Phí vận chuyển</span><span>{{ formatCurrency(invoice.shippingFee) }}</span></div>
              <div class="fin-row"><span>Giá trị tính thuế</span><span>{{ formatCurrency(invoice.taxableAmount) }}</span></div>
              <div class="fin-row"><span>Thuế suất VAT</span><span>{{ Number(invoice.taxRate || 0) }}%</span></div>
              <div class="fin-row"><span>Tiền thuế VAT</span><strong class="tax-val">{{ formatCurrency(invoice.taxAmount) }}</strong></div>
              <div class="fin-row fin-grand"><span>Tổng thanh toán</span><strong>{{ formatCurrency(invoice.totalAmount) }}</strong></div>
              <div class="fin-row"><span>Phương thức thanh toán</span><strong>{{ paymentMethodLabel }}</strong></div>
              <div class="fin-row"><span>Trạng thái thanh toán</span><strong>{{ invoice.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán' }}</strong></div>
            </div>
          </div>
        </el-col>

        <el-col :xs="24" :lg="12">
          <!-- Related adjustment invoices -->
          <div v-if="invoice.adjustmentInvoices && invoice.adjustmentInvoices.length" class="detail-card" style="margin-bottom: 16px">
            <div class="card-title">Hóa đơn điều chỉnh / thay thế liên kết</div>
            <div class="related-list">
              <div v-for="adj in invoice.adjustmentInvoices" :key="adj.id" class="related-row">
                <router-link :to="`/admin/invoices/${adj.id}`" class="ref-link">{{ adj.invoiceCode }}</router-link>
                <span>{{ adj.recordKind }} · {{ adj.invoiceType }}</span>
                <strong>{{ formatCurrency(adj.totalAmount) }}</strong>
              </div>
            </div>
          </div>

          <!-- Email Send History -->
          <div class="detail-card">
            <div class="card-title-row">
              <div class="card-title" style="margin-bottom: 0">Lịch sử gửi Email ({{ emailStatusLabel(invoice.emailStatus) }})</div>
              <el-button size="small" @click="openSendEmail">Gửi / Gửi lại</el-button>
            </div>
            <el-table
              v-if="invoice.emailHistories && invoice.emailHistories.length"
              :data="invoice.emailHistories"
              size="small"
              border
              style="width: 100%; margin-top: 10px"
            >
              <el-table-column prop="recipientEmail" label="Email nhận" min-width="150" />
              <el-table-column label="Trạng thái" width="100" align="center">
                <template #default="{ row }">{{ emailStatusLabel(row.sendStatus) }}</template>
              </el-table-column>
              <el-table-column prop="sentBy" label="Người gửi" width="90" />
              <el-table-column prop="retryCount" label="Retry" width="60" align="center" />
              <el-table-column label="Thời gian / Ghi chú lỗi" min-width="160">
                <template #default="{ row }">
                  <div>{{ formatDateTime(row.sentAt || row.createdAt) }}</div>
                  <div v-if="row.errorMessage" class="err-msg">{{ row.errorMessage }}</div>
                </template>
              </el-table-column>
            </el-table>
            <div v-else class="empty-note">Hóa đơn này chưa được gửi qua Email.</div>
          </div>
        </el-col>
      </el-row>

      <!-- E: Audit History -->
      <div class="detail-card">
        <div class="card-title">E. Lịch sử xử lý hóa đơn (Audit Log bất biến)</div>
        <div class="timeline-list">
          <div v-for="h in invoice.histories || []" :key="h.id" class="timeline-item">
            <div class="tl-dot" />
            <div class="tl-body">
              <div class="tl-head">
                <strong>{{ h.action }}</strong>
                <span class="tl-meta">{{ h.actor }} &middot; {{ formatDateTime(h.createdAt) }}</span>
              </div>
              <div v-if="h.reason" class="tl-text">Lý do: {{ h.reason }}</div>
              <div v-if="h.detail" class="tl-sub">{{ h.detail }}</div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <InvoicePrintPreviewModal v-model="printVisible" :invoice="invoice" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Printer, Download, Message } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { invoiceService } from '@/services/invoiceService'
import { formatCurrency, formatDateTime } from '@/utils/formatters'
import { PAYMENT_METHOD_MAP } from '@/constants'
import InvoicePrintPreviewModal from '@/components/InvoicePrintPreviewModal.vue'

const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const downloadingPdf = ref(false)
const invoice = ref(null)
const printVisible = ref(false)

const canCancelDirect = computed(() => authStore.isManager)
const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const paymentMethodLabel = computed(() => {
  if (!invoice.value) return ''
  if (invoice.value.paymentMethod === 'CASH' && invoice.value.saleChannel === 'ONLINE') {
    return 'COD (Giao hàng thu tiền)'
  }
  return PAYMENT_METHOD_MAP[invoice.value.paymentMethod] || invoice.value.paymentMethod
})

function invoiceStatusLabel(s) {
  return (
    {
      ISSUED: 'Đã phát hành',
      PENDING_CANCEL: 'Chờ duyệt hủy',
      CANCELLED: 'Đã hủy',
      ADJUSTED: 'Đã điều chỉnh'
    }[s] || s
  )
}

function emailStatusLabel(s) {
  return (
    {
      NOT_SENT: 'Chưa gửi',
      SENDING: 'Đang gửi',
      SENT: 'Thành công',
      FAILED: 'Thất bại'
    }[s] || s
  )
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await invoiceService.getInvoiceById(route.params.id)
    invoice.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleDownloadPdf() {
  if (!invoice.value?.id) return
  downloadingPdf.value = true
  try {
    const res = await invoiceService.downloadPdf(invoice.value.id)
    const blob = new Blob([res.data], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `HoaDon_${invoice.value.invoiceCode}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } finally {
    downloadingPdf.value = false
  }
}

function openSendEmail() {
  ElMessageBox.prompt(
    `Nhập địa chỉ Email nhận hóa đơn PDF cho ${invoice.value.invoiceCode}:`,
    'Gửi hóa đơn qua Email',
    {
      confirmButtonText: 'Gửi Email',
      cancelButtonText: 'Hủy',
      inputValue: invoice.value.customerEmail || '',
      inputValidator: (v) => {
        if (!v || !v.trim()) return 'Vui lòng nhập Email người nhận'
        if (!EMAIL_REGEX.test(v.trim())) return 'Email không đúng định dạng'
        return true
      }
    }
  )
    .then(async ({ value }) => {
      await invoiceService.sendEmail(invoice.value.id, { recipientEmail: value.trim() })
      ElMessage.success(`Đã tiếp nhận gửi hóa đơn tới ${value.trim()}`)
      setTimeout(() => loadDetail(), 900)
    })
    .catch(() => {})
}

function handleCancelDirect() {
  ElMessageBox.prompt(`Nhập lý do hủy hóa đơn ${invoice.value.invoiceCode}:`, 'Hủy hóa đơn', {
    confirmButtonText: 'Xác nhận hủy',
    cancelButtonText: 'Đóng',
    confirmButtonClass: 'el-button--danger',
    inputValidator: (v) => (!v || !v.trim() ? 'Bắt buộc nhập lý do hủy' : true)
  })
    .then(async ({ value }) => {
      await invoiceService.cancelInvoiceDirect(invoice.value.id, { reason: value.trim() })
      ElMessage.success('Đã hủy hóa đơn thành công')
      await loadDetail()
    })
    .catch(() => {})
}

function handleRequestCancel() {
  ElMessageBox.prompt(`Nhập lý do đề nghị hủy hóa đơn ${invoice.value.invoiceCode}:`, 'Gửi yêu cầu duyệt hủy', {
    confirmButtonText: 'Gửi yêu cầu',
    cancelButtonText: 'Đóng',
    inputValidator: (v) => (!v || !v.trim() ? 'Vui lòng nhập lý do hủy' : true)
  })
    .then(async ({ value }) => {
      await invoiceService.requestCancelInvoice(invoice.value.id, { reason: value.trim() })
      ElMessage.success('Đã gửi yêu cầu duyệt hủy hóa đơn')
      await loadDetail()
    })
    .catch(() => {})
}

async function approveCancel() {
  await invoiceService.approveCancelRequest(invoice.value.pendingCancelRequestId, { reviewNote: 'Đã duyệt hủy' })
  ElMessage.success('Đã phê duyệt hủy hóa đơn')
  await loadDetail()
}

async function rejectCancel() {
  await invoiceService.rejectCancelRequest(invoice.value.pendingCancelRequestId, { reviewNote: 'Từ chối yêu cầu hủy' })
  ElMessage.info('Đã từ chối yêu cầu hủy hóa đơn')
  await loadDetail()
}

watch(() => route.params.id, () => {
  if (route.params.id) loadDetail()
})

onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.invoice-detail-page {
  gap: 20px;
}

.detail-card {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 20px 22px;
  height: 100%;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.title-block {
  .code-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 800;
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      color: #0f172a;
    }
  }

  .sub-row {
    margin-top: 4px;
    font-size: 13px;
    color: #64748b;
  }
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-soft {
  border-radius: 5px !important;
  font-weight: 600;
}

.btn-primary-dark {
  border-radius: 5px !important;
  font-weight: 700;
  background: #0f172a !important;
  border-color: #0f172a !important;
}

.cancel-review-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 6px;
  font-size: 13px;
  color: #92400e;

  .banner-actions {
    display: flex;
    gap: 8px;
  }
}

.detail-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px 18px;
}

.card-title {
  font-size: 13.5px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: #0f172a;
  margin-bottom: 12px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kv-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}

.kv-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px dashed #f1f5f9;
  padding-bottom: 6px;

  span:first-child {
    color: #64748b;
  }

  &.danger-row {
    color: #dc2626;
  }
}

.finance-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13.5px;
}

.fin-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  border-bottom: 1px dashed #f1f5f9;

  .tax-val {
    color: #1d4ed8;
  }

  &.fin-grand {
    border-top: 2px solid #0f172a;
    border-bottom: none;
    padding-top: 10px;
    font-size: 16px;
    color: #dc2626;
  }
}

.related-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.related-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  background: #f8fafc;
  border-radius: 5px;
  font-size: 13px;
}

.ref-link {
  color: #2563eb;
  font-weight: 700;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.type-pill {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 800;

  &.type-vat {
    background: #eff6ff;
    color: #1d4ed8;
    border: 1px solid #93c5fd;
  }

  &.type-retail {
    background: #f1f5f9;
    color: #334155;
  }
}

.status-pill {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 700;

  &.st-issued { background: #ecfdf5; color: #047857; }
  &.st-pending_cancel { background: #fffbeb; color: #b45309; }
  &.st-cancelled { background: #fef2f2; color: #b91c1c; }
  &.st-adjusted { background: #eff6ff; color: #1d4ed8; }
}

.err-msg {
  font-size: 11px;
  color: #dc2626;
  margin-top: 2px;
}

.empty-note {
  margin-top: 10px;
  font-size: 13px;
  color: #94a3b8;
  font-style: italic;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.tl-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #0f172a;
  margin-top: 6px;
  flex-shrink: 0;
}

.tl-body {
  flex: 1;
  font-size: 13px;
}

.tl-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;

  .tl-meta {
    font-size: 12px;
    color: #64748b;
  }
}

.tl-text {
  color: #0f172a;
  margin-top: 2px;
}

.tl-sub {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}
</style>
