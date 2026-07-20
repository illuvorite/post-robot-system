import request from './request'

export function getProductPage(query) {
  return request.get('/product/list/page/vo', { params: query })
}

export function getProductDetail(id) {
  return request.get(`/product/get/${id}`)
}

export function addProduct(data) {
  return request.post('/product/add', data)
}

export function editProduct(data) {
  return request.put('/product/edit', data)
}

export function changeProductStatus(id, status) {
  return request.post(`/product/status/${id}`, null, { params: { status } })
}

export function deleteProduct(id) {
  return request.delete(`/product/delete/${id}`)
}

export function recommendProduct(data) {
  return request.post('/product/recommend', data)
}

export function updateProductTags(data) {
  return request.post('/product/tags', data)
}
