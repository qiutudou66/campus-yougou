import request from './request'

export function getShopTypes() {
  return request.get('/shop-type/list')
}

export function getShop(id, config = {}) {
  return request.get(`/shop/${id}`, config)
}

export function getShopsByType({ typeId, current = 1, x, y }) {
  const params = { typeId, current }
  if (x !== undefined && x !== null) params.x = x
  if (y !== undefined && y !== null) params.y = y
  return request.get('/shop/of/type', { params })
}

export function getShopsByName({ name = '', current = 1 }) {
  return request.get('/shop/of/name', { params: { name, current } })
}
