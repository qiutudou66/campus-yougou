import request from './request'

export function getVouchersByShop(shopId) {
  return request.get(`/voucher/list/${shopId}`)
}

export function seckillVoucher(voucherId) {
  return request.post(`/voucher-order/seckill/${voucherId}`)
}
