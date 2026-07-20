<template>
  <el-tag :type="type" :size="size" effect="plain">
    {{ text }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: [String, Number], default: '' },
  mapping: { type: Object, default: () => ({}) },
  size: { type: String, default: 'small' }
})

const typeMap = {
  // 订单状态
  PENDING_PAY: 'warning', PAYING: 'warning', PAID: 'success',
  PAY_FAILED: 'danger', CANCELLED: 'info', TIMEOUT: 'info',
  MANUAL_REQUIRED: 'danger',
  // 任务状态
  CREATED: 'info', QUEUED: 'warning', RUNNING: 'primary',
  PAUSED: 'warning', SUCCEEDED: 'success', FAILED: 'danger',
  CANCELLED: 'info',
  // 支付状态
  '00': 'warning', '01': 'success', '02': 'danger',
  '03': 'info', '05': 'warning',
  // 通用
  NORMAL: 'success', MISSING: 'danger', DISPLACED: 'warning',
  UNRESOLVED: 'danger', RESOLVED: 'success', PROCESSING: 'warning',
  // 商品状态
  0: 'info', 1: 'success',
  OFF_SHELF: 'info', ON_SHELF: 'success',
  // 角色
  ADMIN: 'danger', OPERATOR: 'primary', MAINTAINER: 'warning',
  // 告警类型
  LOW_STOCK: 'warning', STOCK_DISCREPANCY: 'danger', SAMPLE_MISSING: 'danger',
  TASK_FAILURE: 'danger', PAYMENT_TIMEOUT: 'warning', NETWORK_DOWN: 'danger', SYSTEM_ERROR: 'danger',
  // 告警级别
  INFO: 'info', WARNING: 'warning', CRITICAL: 'danger',
  // 操作类型
  LOGIN: 'primary', LOGOUT: 'info', ORDER_CREATE: 'success', ORDER_CANCEL: 'warning',
  PAYMENT_CALLBACK: 'success', PRODUCT_ADD: 'success', PRODUCT_EDIT: 'primary',
  STOCK_INBOUND: 'success', STOCK_OUTBOUND: 'warning',
  // 操作结果
  SUCCESS: 'success', FAIL: 'danger',
}

const type = computed(() => {
  const key = String(props.value)
  return typeMap[key] || props.mapping[key] || 'info'
})

const text = computed(() => {
  const labelMap = {
    // 订单
    PENDING_PAY: '待支付', PAYING: '支付中', PAID: '已支付',
    PAY_FAILED: '支付失败', CANCELLED: '已取消', TIMEOUT: '已超时',
    MANUAL_REQUIRED: '需人工处理',
    // 任务
    CREATED: '已创建', QUEUED: '排队中', RUNNING: '执行中',
    PAUSED: '已暂停', SUCCEEDED: '已完成', FAILED: '失败',
    // 支付
    '00': '支付中', '01': '成功', '02': '失败', '03': '已退款', '05': '部分退款',
    // 样品
    NORMAL: '正常', MISSING: '缺失', DISPLACED: '错位',
    UNRESOLVED: '未处理', RESOLVED: '已处理', PROCESSING: '处理中',
    // 商品
    0: '下架', 1: '上架',
    OFF_SHELF: '下架', ON_SHELF: '上架',
    // 角色
    ADMIN: '管理员', OPERATOR: '运营人员', MAINTAINER: '维护人员',
    // 操作
    LOGIN: '登录', LOGOUT: '登出', ORDER_CREATE: '创建订单', ORDER_CANCEL: '取消订单',
    PAYMENT_CALLBACK: '支付回调', PRODUCT_ADD: '新增商品', PRODUCT_EDIT: '编辑商品',
    STOCK_INBOUND: '入库', STOCK_OUTBOUND: '出库',
    // 操作结果
    SUCCESS: '成功', FAIL: '失败',
    // 告警类型
    LOW_STOCK: '库存不足', STOCK_DISCREPANCY: '库存差异', SAMPLE_MISSING: '样品缺失',
    TASK_FAILURE: '任务失败', PAYMENT_TIMEOUT: '支付超时', NETWORK_DOWN: '网络中断',
    SYSTEM_ERROR: '系统错误',
    // 告警级别
    INFO: '提示', WARNING: '警告', CRITICAL: '严重',
  }
  return labelMap[props.value] || props.mapping[props.value] || String(props.value)
})
</script>
