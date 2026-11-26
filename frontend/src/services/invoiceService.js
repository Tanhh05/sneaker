import api from './api'

export const invoiceService = {
  getInvoices(params) {
    return api.get('/invoices', { params })
  },
  getVatConfig() {
    return api.get('/invoices/vat-config')
  },
  getKpi(params) {
    return api.get('/invoices/kpi', { params })
  },
  getInvoiceById(id) {
    return api.get(`/invoices/${id}`)
  },
  issueInvoice(payload) {
    return api.post('/invoices/issue', payload)
  },
  cancelInvoiceDirect(id, payload) {
    return api.post(`/invoices/${id}/cancel`, payload)
  },
  requestCancelInvoice(id, payload) {
    return api.post(`/invoices/${id}/cancel-request`, payload)
  },
  approveCancelRequest(requestId, payload) {
    return api.post(`/invoices/cancel-requests/${requestId}/approve`, payload || {})
  },
  rejectCancelRequest(requestId, payload) {
    return api.post(`/invoices/cancel-requests/${requestId}/reject`, payload || {})
  },
  adjustInvoice(id, payload) {
    return api.post(`/invoices/${id}/adjust`, payload)
  },
  sendEmail(id, payload) {
    return api.post(`/invoices/${id}/send-email`, payload || {})
  },
  downloadPdf(id) {
    return api.get(`/invoices/${id}/pdf`, { responseType: 'blob' })
  },
  exportExcel(params) {
    return api.get('/invoices/export', { params, responseType: 'blob' })
  }
}
