import request from './request'

export function getAuditLogPage(params) {
  return request.get('/audit-log/list/page/vo', { params })
}

export function getAuditLogDetail(id) {
  return request.get(`/audit-log/get/${id}`)
}
