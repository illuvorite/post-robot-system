import request from './request'

export function getUserPage(query) {
  return request.get('/user/list/page/vo', { params: query })
}

export function getUserDetail(id) {
  return request.get(`/user/get/${id}`)
}

export function addUser(data) {
  return request.post('/user/add', data)
}

export function editUser(data) {
  return request.put('/user/edit', data)
}

export function deleteUser(id) {
  return request.delete(`/user/delete/${id}`)
}
