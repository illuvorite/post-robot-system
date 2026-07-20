import request from './request'

export function getPaymentsByOrderId(orderId) {
  return request.get(`/payment/list/${orderId}`)
}

export function getPaymentByFlowNo(paymentFlowNo) {
  return request.get(`/payment/getByFlow/${paymentFlowNo}`)
}
