import api from './api'

export const voucherService = {
  searchVouchers(params) {
    return api.get('/vouchers', { params })
  },
  getAllVouchers() {
    return api.get('/vouchers/all')
  },
  getById(id) {
    return api.get(`/vouchers/${id}`)
  },
  createVoucher(data) {
    return api.post('/vouchers', data)
  },
  updateVoucher(id, data) {
    return api.put(`/vouchers/${id}`, data)
  },
  toggleVoucherStatus(id, status) {
    return api.patch(`/vouchers/${id}/status`, { status })
  },
  softDeleteVoucher(id) {
    return api.delete(`/vouchers/${id}/soft-delete`)
  },

  // Dynamic Trigger Rules
  getRules() {
    return api.get('/vouchers/rules')
  },
  createRule(data) {
    return api.post('/vouchers/rules', data)
  },
  updateRule(id, data) {
    return api.put(`/vouchers/rules/${id}`, data)
  },
  toggleRuleStatus(id, status) {
    return api.patch(`/vouchers/rules/${id}/status`, { status })
  },
  softDeleteRule(id) {
    return api.delete(`/vouchers/rules/${id}/soft-delete`)
  },

  // Usages & POS
  searchUsages(params) {
    return api.get('/vouchers/usages', { params })
  },
  getPosAvailableVouchers(params) {
    return api.get('/vouchers/pos/available', { params })
  },
  validateVoucher(payload) {
    return api.post('/vouchers/validate', payload)
  },
  triggerExpiryScan() {
    return api.post('/vouchers/scan-expiring')
  }
}

export default voucherService
