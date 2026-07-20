import request from './request'

export function getTaskPage(query) {
  return request.get('/task/list/page/vo', { params: query })
}

export function getTaskDetail(id) {
  return request.get(`/task/get/${id}`)
}

export function cancelTask(id) {
  return request.post(`/task/${id}/cancel`)
}

export function retryTask(id) {
  return request.post(`/task/${id}/retry`)
}

export function createTask(data) {
  return request.post('/task/create', data)
}

export function editTask(data) {
  return request.put('/task/edit', data)
}

export function deleteTask(id) {
  return request.delete(`/task/delete/${id}`)
}

export function reportRobotStatus(data) {
  return request.post('/task/robot-status', data)
}
