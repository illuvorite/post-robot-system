import request from './request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function refreshToken(refreshToken) {
  return request.post('/auth/refresh', null, { params: { refreshToken } })
}

export function getCurrentUser() {
  return request.get('/user/current')
}
