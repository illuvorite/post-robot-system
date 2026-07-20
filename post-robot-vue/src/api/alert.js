import request from './request'

export function getAlertPage(params) {
  return request.get('/alert/list/page/vo', { params })
}

export function getAlertDetail(id) {
  return request.get(`/alert/get/${id}`)
}

export function handleAlert(id, handler, note) {
  return request.post(`/alert/handle/${id}`, null, { params: { handler, note } })
}

export function countUnresolvedAlerts() {
  return request.get('/alert/count/unresolved')
}

export function updateAlert(data) {
  return request.put('/alert/update', data)
}

export function deleteAlert(id) {
  return request.delete(`/alert/delete/${id}`)
}
