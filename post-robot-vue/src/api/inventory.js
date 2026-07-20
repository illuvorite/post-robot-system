import request from './request'

export function getInventoryByProductId(productId) {
  return request.get(`/inventory/get/${productId}`)
}

export function getInventoryPage(query) {
  return request.get('/inventory/list/page/vo', { params: query })
}

export function inboundStock(productId, quantity) {
  return request.post('/inventory/inbound', null, { params: { productId, quantity } })
}

export function outboundStock(productId, quantity) {
  return request.post('/inventory/outbound', null, { params: { productId, quantity } })
}

export function lockStock(productId, quantity) {
  return request.post('/inventory/lock', null, { params: { productId, quantity } })
}

export function releaseStock(productId, quantity) {
  return request.post('/inventory/release', null, { params: { productId, quantity } })
}

export function deductStock(productId, quantity) {
  return request.post('/inventory/deduct', null, { params: { productId, quantity } })
}

export function adjustStock(data) {
  return request.put('/inventory/adjust', data)
}

export function deleteInventory(productId) {
  return request.delete(`/inventory/delete/${productId}`)
}

export function visionInspect(productId, data) {
  return request.put(`/inventory/vision/${productId}`, data)
}

export function createInventory(productId) {
  return request.post('/inventory/create', null, { params: { productId } })
}
