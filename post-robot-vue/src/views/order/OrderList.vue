<template>
  <div>
    <!-- 搜索栏 -->
    <el-card style="margin-bottom: 16px;">
      <el-form :model="query" inline size="small">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="模糊搜索" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单表格 -->
    <el-card>
      <el-table :data="orders" v-loading="loading" stripe size="small">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="totalAmount" label="总金额" width="120">
          <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="postage" label="邮资" width="100">
          <template #default="{ row }">¥{{ row.postage?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <StatusTag :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="paymentFlowNo" label="支付流水号" width="180" />
        <el-table-column prop="mailNo" label="邮件号码" width="140" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDetail(row)">详情</el-button>
            <el-button
              size="small"
              type="danger"
              link
              :disabled="!canCancel(row.status)"
              @click="handleCancel(row)"
            >取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @change="fetchOrders"
      />
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="700px" top="5vh">
      <template v-if="currentOrder">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号" :span="2">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ currentOrder.totalAmount?.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="邮资">¥{{ currentOrder.postage?.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :value="currentOrder.status" />
          </el-descriptions-item>
          <el-descriptions-item label="邮件号码">{{ currentOrder.mailNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付流水号" :span="2">{{ currentOrder.paymentFlowNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交易流水号" :span="2">{{ currentOrder.transactionId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ currentOrder.createTime }}</el-descriptions-item>
        </el-descriptions>

        <!-- 商品明细 -->
        <h4 style="margin: 16px 0 8px">商品明细</h4>
        <el-table :data="currentOrder.items" size="small" stripe>
          <el-table-column prop="productName" label="商品名称" />
          <el-table-column prop="productPrice" label="单价">
            <template #default="{ row }">¥{{ row.productPrice?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="subtotal" label="小计">
            <template #default="{ row }">¥{{ row.subtotal?.toFixed(2) }}</template>
          </el-table-column>
        </el-table>

        <!-- 状态变更历史 -->
        <h4 v-if="statusLogs.length" style="margin: 16px 0 8px">状态变更历史</h4>
        <el-timeline v-if="statusLogs.length">
          <el-timeline-item
            v-for="log in statusLogs"
            :key="log.createTime"
            :timestamp="log.createTime"
            placement="top"
          >
            <span>{{ log.fromStatus || '-' }} → {{ log.toStatus }}</span>
            <span style="margin-left: 12px; color: #909399; font-size: 12px">{{ log.operator }}</span>
            <div v-if="log.remark" style="color: #909399; font-size: 12px">{{ log.remark }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderPage, getOrderByNo, cancelOrder, getOrderStatusLogs } from '@/api/order'
import StatusTag from '@/components/StatusTag.vue'

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const query = reactive({
  orderNo: '',
  status: ''
})

const statusOptions = [
  { value: 'PENDING_PAY', label: '待支付' },
  { value: 'PAYING', label: '支付中' },
  { value: 'PAID', label: '支付成功' },
  { value: 'FAILED', label: '支付失败' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'TIMEOUT', label: '已超时' },
  { value: 'MANUAL_REQUIRED', label: '需人工处理' }
]

function canCancel(status) {
  return ['PENDING_PAY', 'FAILED'].includes(status)
}

const detailVisible = ref(false)
const currentOrder = ref(null)
const statusLogs = ref([])

onMounted(() => fetchOrders())

async function fetchOrders() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (query.orderNo) params.orderNo = query.orderNo
    if (query.status) params.status = query.status
    const data = await getOrderPage(params)
    orders.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    orders.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchOrders()
}

function resetSearch() {
  query.orderNo = ''
  query.status = ''
  pageNum.value = 1
  fetchOrders()
}

async function showDetail(row) {
  try {
    const order = await getOrderByNo(row.orderNo)
    currentOrder.value = order
    statusLogs.value = await getOrderStatusLogs(row.orderNo)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取订单详情失败')
  }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm(`确定要取消订单 ${row.orderNo} 吗？`, '提示')
    await cancelOrder(row.orderNo)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('取消失败')
  }
}
</script>
