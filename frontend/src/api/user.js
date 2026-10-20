import request from './request'

export function sendCode(phone) {
  return request.post('/user/code', null, { params: { phone } })
}

export function loginByCode({ phone, code }) {
  return request.post('/user/login', { phone, code })
}

export function getMe(config = {}) {
  return request.get('/user/me', config)
}

export function getUserInfo(id, config = {}) {
  return request.get(`/user/info/${id}`, config)
}

export function getUser(id, config = {}) {
  return request.get(`/user/${id}`, config)
}

export function logout(config = {}) {
  return request.post('/user/logout', null, config)
}
