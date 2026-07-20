import request from './request'

export function getOrderPage(params) {
  return request.get('/order/list/page', { params })
}

export function getOrderById(id) {
  return request.get(`/order/get/${id}`)
}

export function getOrderByNo(orderNo) {
  return request.get(`/order/getByNo/${orderNo}`)
}

export function createOrder(data) {
  return request.post('/order/create', data)
}

export function cancelOrder(orderNo) {
  return request.post(`/order/cancel/${orderNo}`)
}

export function getOrderStatusLogs(orderNo) {
  return request.get(`/order/statusLogs/${orderNo}`)
}
