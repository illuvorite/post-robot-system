import request from './request'

export function queryPostage(data) {
  return request.post('/postal/postage-query', data)
}

export function generateMailNumber(data) {
  return request.post('/postal/mail-number', data)
}

export function submitOrder(data) {
  return request.post('/postal/order-submit', data)
}

export function generateQrCode(params) {
  return request.post('/postal/qr-code', null, { params })
}

export function getPaymentStatus(params) {
  return request.get('/postal/payment-status', { params })
}

export function getMockStatus() {
  return request.get('/postal/mock/status')
}

export function resetMock() {
  return request.post('/postal/mock/reset')
}
