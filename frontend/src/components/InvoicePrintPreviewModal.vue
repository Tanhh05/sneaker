<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="invoice?.invoiceType === 'VAT' ? 'XEM TRƯỚC & IN HÓA ĐƠN VAT' : 'XEM TRƯỚC & IN HÓA ĐƠN BÁN LẺ'"
    width="760px"
    destroy-on-close
  >
    <div v-if="invoice" id="adha-invoice-print-sheet" class="print-sheet" :class="{ 'is-vat': invoice.invoiceType === 'VAT' }">
      <!-- Cancelled Watermark -->
      <div v-if="invoice.invoiceStatus === 'CANCELLED'" class="watermark-cancelled">
        ĐÃ HỦY
      </div>

      <!-- Header -->
      <div class="sheet-header">
        <div class="brand-block">
          <div class="brand-name">ADHA SNEAKER STORE</div>
          <div class="brand-meta">MST đơn vị bán: <strong>0319886688</strong></div>
          <div class="brand-meta">Địa chỉ: 120 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh</div>
          <div class="brand-meta">Hotline: 0901 000 001 &middot; Website: adhasneaker.duckdns.org</div>
        </div>
        <div class="doc-block">
          <div class="doc-title">
            {{ invoice.invoiceType === 'VAT' ? 'HÓA ĐƠN GIÁ TRỊ GIA TĂNG (VAT)' : 'HÓA ĐƠN BÁN LẺ' }}
          </div>
          <div v-if="invoice.recordKind && invoice.recordKind !== 'ORIGINAL'" class="doc-kind-tag">
            {{ invoice.recordKind === 'REPLACEMENT' ? 'HÓA ĐƠN THAY THẾ' : 'HÓA ĐƠN ĐIỀU CHỈNH' }}
            <span v-if="invoice.originalInvoiceCode">(Gốc: {{ invoice.originalInvoiceCode }})</span>
          </div>
          <div class="doc-code">{{ invoice.invoiceCode }}</div>
          <div class="doc-meta">Đơn hàng nguồn: <strong>{{ invoice.orderCode }}</strong> ({{ invoice.saleChannel === 'ONLINE' ? 'Online' : 'Tại quầy' }})</div>
          <div class="doc-meta">Ngày lập: {{ formatDateTime(invoice.issuedAt) }}</div>
        </div>
      </div>

      <!-- Buyer Information -->
      <div class="buyer-box">
        <div class="buyer-row">
          <span><strong>Người mua hàng:</strong> {{ invoice.customerName }}</span>
          <span><strong>Điện thoại:</strong> {{ invoice.customerPhone || '---' }}</span>
        </div>
        <div v-if="invoice.invoiceType === 'VAT' || invoice.companyName || invoice.taxCode" class="buyer-row vat-row">
          <span><strong>Tên đơn vị / Công ty:</strong> {{ invoice.companyName || '---' }}</span>
          <span><strong>Mã số thuế (MST):</strong> <code>{{ invoice.taxCode || '---' }}</code></span>
        </div>
        <div class="buyer-row">
          <span><strong>Địa chỉ:</strong> {{ invoice.customerAddress || 'Mua trực tiếp tại cửa hàng' }}</span>
          <span><strong>Email nhận HĐ:</strong> {{ invoice.customerEmail || 'Không cung cấp' }}</span>
        </div>
      </div>

      <!-- Line Items Table -->
      <table class="sheet-table">
        <thead>
          <tr>
            <th style="width: 34px" class="tc">#</th>
            <th>Mã SP / Tên sản phẩm</th>
            <th>Biến thể</th>
            <th style="width: 48px" class="tc">SL</th>
            <th style="width: 105px" class="tr">Đơn giá</th>
            <th style="width: 85px" class="tr">Giảm giá</th>
            <th style="width: 55px" class="tc">VAT</th>
            <th style="width: 115px" class="tr">Thành tiền</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in invoice.details || []" :key="item.id || idx">
            <td class="tc">{{ idx + 1 }}</td>
            <td>
              <div class="item-title">{{ item.productName }}</div>
              <div class="item-sku">{{ item.productCode || '' }} {{ item.sku ? `· ${item.sku}` : '' }}</div>
            </td>
            <td>{{ item.variantLabel || '---' }}</td>
            <td class="tc fw-700">{{ item.quantity }}</td>
            <td class="tr">{{ formatCurrency(item.unitPrice) }}</td>
            <td class="tr">{{ formatCurrency(item.discountAmount) }}</td>
            <td class="tc">{{ Number(item.taxRate || 0) }}%</td>
            <td class="tr fw-700">{{ formatCurrency(item.lineTotal) }}</td>
          </tr>
        </tbody>
      </table>

      <!-- Financial Summary -->
      <div class="sheet-bottom">
        <div class="payment-notes">
          <div><strong>Phương thức thanh toán:</strong> {{ paymentMethodLabel }}</div>
          <div><strong>Trạng thái thanh toán:</strong> {{ invoice.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán' }}</div>
          <div v-if="invoice.note" class="note-line"><strong>Ghi chú:</strong> {{ invoice.note }}</div>
          <div v-if="invoice.invoiceType === 'VAT'" class="legal-notice">
            Lưu ý: Đây là bản thể hiện hóa đơn VAT nội bộ quản lý tại ADHA Sneaker. Khi tích hợp nhà cung cấp hóa đơn điện tử (CQT), mã tra cứu pháp lý sẽ hiển thị tại mục này.
          </div>
        </div>

        <div class="totals-box">
          <div class="tot-row">
            <span>Tổng tiền hàng trước thuế:</span>
            <strong>{{ formatCurrency(invoice.subtotal) }}</strong>
          </div>
          <div class="tot-row">
            <span>Tổng chiết khấu:</span>
            <span>-{{ formatCurrency(invoice.discountAmount) }}</span>
          </div>
          <div class="tot-row">
            <span>Phí vận chuyển:</span>
            <span>{{ formatCurrency(invoice.shippingFee) }}</span>
          </div>
          <div class="tot-row">
            <span>Giá trị tính thuế:</span>
            <span>{{ formatCurrency(invoice.taxableAmount) }}</span>
          </div>
          <div class="tot-row">
            <span>Tiền thuế GTGT ({{ Number(invoice.taxRate || 0) }}%):</span>
            <strong>{{ formatCurrency(invoice.taxAmount) }}</strong>
          </div>
          <div class="tot-row grand">
            <span>TỔNG THANH TOÁN:</span>
            <span>{{ formatCurrency(invoice.totalAmount) }}</span>
          </div>
        </div>
      </div>

      <!-- Signatures -->
      <div class="signatures">
        <div class="sig-col">
          <div class="sig-title">Người mua hàng</div>
          <div class="sig-sub">(Ký, ghi rõ họ tên)</div>
        </div>
        <div class="sig-col">
          <div class="sig-title">Đại diện ADHA Sneaker</div>
          <div class="sig-sub">(Người lập: {{ invoice.createdBy || 'SYSTEM' }})</div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="modal-footer-row">
        <el-button @click="$emit('update:modelValue', false)">Đóng</el-button>
        <el-button :loading="downloadingPdf" @click="handleDownloadPdf">
          <el-icon style="margin-right: 5px"><Download /></el-icon>
          Tải file PDF
        </el-button>
        <el-button type="primary" @click="handlePrint">
          <el-icon style="margin-right: 5px"><Printer /></el-icon>
          In hóa đơn ngay
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Printer, Download } from '@element-plus/icons-vue'
import { formatCurrency, formatDateTime } from '@/utils/formatters'
import { PAYMENT_METHOD_MAP } from '@/constants'
import { invoiceService } from '@/services/invoiceService'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  invoice: { type: Object, default: null }
})

defineEmits(['update:modelValue'])

const downloadingPdf = ref(false)

const paymentMethodLabel = computed(() => {
  if (!props.invoice) return ''
  if (props.invoice.paymentMethod === 'CASH' && props.invoice.saleChannel === 'ONLINE') {
    return 'COD (Giao hàng thu tiền)'
  }
  return PAYMENT_METHOD_MAP[props.invoice.paymentMethod] || props.invoice.paymentMethod
})

async function handleDownloadPdf() {
  if (!props.invoice?.id) return
  downloadingPdf.value = true
  try {
    const res = await invoiceService.downloadPdf(props.invoice.id)
    const blob = new Blob([res.data], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `HoaDon_${props.invoice.invoiceCode}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('Không thể tải file PDF hóa đơn')
  } finally {
    downloadingPdf.value = false
  }
}

function handlePrint() {
  const el = document.getElementById('adha-invoice-print-sheet')
  if (!el) return
  const win = window.open('', '_blank', 'width=880,height=760')
  if (!win) {
    window.print()
    return
  }
  win.document.write(`
    <!DOCTYPE html>
    <html lang="vi">
      <head>
        <meta charset="UTF-8" />
        <title>In Hóa Đơn - ${props.invoice?.invoiceCode || ''}</title>
        <style>
          * { box-sizing: border-box; }
          body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif; color: #0f172a; padding: 20px; margin: 0; }
          .print-sheet { position: relative; border: 1.5px solid #0f172a; border-radius: 6px; padding: 22px; }
          .watermark-cancelled { position: absolute; top: 42%; left: 50%; transform: translate(-50%,-50%) rotate(-24deg); font-size: 64px; font-weight: 900; color: rgba(220,38,38,0.16); border: 5px solid rgba(220,38,38,0.2); padding: 8px 28px; pointer-events: none; }
          .sheet-header { display: flex; justify-content: space-between; gap: 16px; border-bottom: 2px solid #0f172a; padding-bottom: 14px; margin-bottom: 14px; }
          .brand-name { font-size: 20px; font-weight: 900; letter-spacing: 0.04em; margin-bottom: 4px; }
          .brand-meta { font-size: 12px; color: #475569; margin: 2px 0; }
          .doc-block { text-align: right; }
          .doc-title { font-size: 15px; font-weight: 800; color: #dc2626; }
          .doc-kind-tag { font-size: 11px; font-weight: 700; color: #1d4ed8; margin-top: 2px; }
          .doc-code { font-size: 16px; font-weight: 800; margin-top: 4px; font-family: monospace; }
          .doc-meta { font-size: 12px; color: #475569; margin-top: 2px; }
          .buyer-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 10px 14px; margin-bottom: 14px; font-size: 12.5px; display: flex; flex-direction: column; gap: 6px; }
          .buyer-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
          .vat-row { border-top: 1px dashed #cbd5e1; border-bottom: 1px dashed #cbd5e1; padding: 5px 0; }
          .sheet-table { width: 100%; border-collapse: collapse; font-size: 12.5px; margin-bottom: 16px; }
          .sheet-table th, .sheet-table td { border: 1px solid #cbd5e1; padding: 7px 8px; }
          .sheet-table th { background: #0f172a; color: #fff; font-weight: 700; }
          .tc { text-align: center; } .tr { text-align: right; } .fw-700 { font-weight: 700; }
          .item-title { font-weight: 600; } .item-sku { font-size: 11px; color: #64748b; }
          .sheet-bottom { display: flex; justify-content: space-between; gap: 18px; }
          .payment-notes { flex: 1; font-size: 12.5px; color: #334155; display: flex; flex-direction: column; gap: 5px; }
          .legal-notice { margin-top: 8px; font-size: 11px; color: #64748b; font-style: italic; background: #fffbeb; border: 1px solid #fde68a; padding: 6px 8px; border-radius: 4px; }
          .totals-box { width: 310px; border: 1px solid #cbd5e1; border-radius: 6px; padding: 10px 12px; font-size: 12.5px; }
          .tot-row { display: flex; justify-content: space-between; padding: 3px 0; }
          .tot-row.grand { border-top: 2px solid #0f172a; margin-top: 6px; padding-top: 8px; font-size: 15px; font-weight: 800; color: #dc2626; }
          .signatures { display: grid; grid-template-columns: 1fr 1fr; margin-top: 28px; text-align: center; font-size: 12.5px; }
          .sig-title { font-weight: 700; } .sig-sub { font-size: 11.5px; color: #64748b; margin-top: 2px; height: 56px; }
        </style>
      </head>
      <body>${el.outerHTML}</body>
    </html>
  `)
  win.document.close()
  win.focus()
  setTimeout(() => win.print(), 200)
}
</script>

<style scoped lang="scss">
.print-sheet {
  position: relative;
  border: 1.5px solid #0f172a;
  border-radius: 6px;
  padding: 20px;
  background: #ffffff;
  color: #0f172a;

  &.is-vat {
    border-color: #1d4ed8;
  }
}

.watermark-cancelled {
  position: absolute;
  top: 42%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-24deg);
  font-size: 56px;
  font-weight: 900;
  color: rgba(220, 38, 38, 0.16);
  border: 5px solid rgba(220, 38, 38, 0.22);
  padding: 8px 26px;
  pointer-events: none;
  z-index: 2;
}

.sheet-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 2px solid #0f172a;
  padding-bottom: 14px;
  margin-bottom: 14px;
}

.brand-name {
  font-size: 19px;
  font-weight: 900;
  letter-spacing: 0.04em;
  margin-bottom: 4px;
}

.brand-meta {
  font-size: 12px;
  color: #475569;
  margin: 2px 0;
}

.doc-block {
  text-align: right;
}

.doc-title {
  font-size: 15px;
  font-weight: 800;
  color: #dc2626;
}

.doc-kind-tag {
  font-size: 11px;
  font-weight: 700;
  color: #1d4ed8;
  margin-top: 2px;
}

.doc-code {
  font-size: 16px;
  font-weight: 800;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  margin-top: 4px;
}

.doc-meta {
  font-size: 12px;
  color: #475569;
  margin-top: 2px;
}

.buyer-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 14px;
  font-size: 12.5px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.buyer-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.vat-row {
  border-top: 1px dashed #cbd5e1;
  border-bottom: 1px dashed #cbd5e1;
  padding: 5px 0;
}

.sheet-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
  margin-bottom: 16px;

  th, td {
    border: 1px solid #cbd5e1;
    padding: 7px 8px;
  }

  th {
    background: #0f172a;
    color: #ffffff;
    font-weight: 700;
  }

  .tc { text-align: center; }
  .tr { text-align: right; }
  .fw-700 { font-weight: 700; }
  .item-title { font-weight: 600; }
  .item-sku { font-size: 11px; color: #64748b; }
}

.sheet-bottom {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
}

.payment-notes {
  flex: 1;
  min-width: 240px;
  font-size: 12.5px;
  color: #334155;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.legal-notice {
  margin-top: 6px;
  font-size: 11px;
  color: #854d0e;
  background: #fffbeb;
  border: 1px solid #fde68a;
  padding: 6px 8px;
  border-radius: 4px;
}

.totals-box {
  width: 300px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 12.5px;
}

.tot-row {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;

  &.grand {
    border-top: 2px solid #0f172a;
    margin-top: 6px;
    padding-top: 8px;
    font-size: 15px;
    font-weight: 800;
    color: #dc2626;
  }
}

.signatures {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-top: 22px;
  text-align: center;
  font-size: 12.5px;

  .sig-title { font-weight: 700; }
  .sig-sub { font-size: 11.5px; color: #64748b; margin-top: 2px; height: 42px; }
}

.modal-footer-row {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
