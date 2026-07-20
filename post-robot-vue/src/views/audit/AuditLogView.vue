<template>
  <div>
    <el-card>
      <template #header>
        <span><el-icon style="vertical-align: middle"><Clock /></el-icon> 审计日志</span>
      </template>

      <el-form :model="query" inline size="small" style="margin-bottom: 12px">
        <el-form-item label="操作人">
          <el-input v-model="query.operator" placeholder="模糊搜索" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.operationType" placeholder="全部" clearable style="width: 160px">
            <el-option label="登录" value="LOGIN" />
            <el-option label="登出" value="LOGOUT" />
            <el-option label="创建订单" value="ORDER_CREATE" />
            <el-option label="取消订单" value="ORDER_CANCEL" />
            <el-option label="支付回调" value="PAYMENT_CALLBACK" />
            <el-option label="商品新增" value="PRODUCT_ADD" />
            <el-option label="商品编辑" value="PRODUCT_EDIT" />
            <el-option label="入库" value="STOCK_INBOUND" />
            <el-option label="出库" value="STOCK_OUTBOUND" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象类型">
          <el-select v-model="query.targetType" placeholder="全部" clearable style="width: 140px">
            <el-option label="订单" value="ORDER" />
            <el-option label="商品" value="PRODUCT" />
            <el-option label="库存" value="INVENTORY" />
            <el-option label="用户" value="USER" />
            <el-option label="告警" value="ALERT" />
            <el-option label="系统" value="SYSTEM" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.result" placeholder="全部" clearable style="width: 100px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="追踪ID">
          <el-input v-model="query.traceId" placeholder="流水号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logs" v-loading="loading" stripe size="small">
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column label="操作类型" width="130">
          <template #default="{ row }"><StatusTag :value="row.operationType" /></template>
        </el-table-column>
        <el-table-column prop="targetType" label="对象类型" width="100" />
        <el-table-column prop="targetId" label="对象ID" width="120" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }"><StatusTag :value="row.result" /></template>
        </el-table-column>
        <el-table-column prop="detail" label="操作详情" min-width="250" show-overflow-tooltip />
        <el-table-column prop="traceId" label="追踪ID" width="180" />
        <el-table-column prop="ipAddress" label="IP" width="140" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @change="fetchLogs"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="审计日志详情" width="600px">
      <template v-if="detailData">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="操作人">{{ detailData.operator }}</el-descriptions-item>
          <el-descriptions-item label="操作类型"><StatusTag :value="detailData.operationType" /></el-descriptions-item>
          <el-descriptions-item label="对象类型">{{ detailData.targetType }}</el-descriptions-item>
          <el-descriptions-item label="对象ID">{{ detailData.targetId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结果"><StatusTag :value="detailData.result" /></el-descriptions-item>
          <el-descriptions-item label="操作详情">{{ detailData.detail || '-' }}</el-descriptions-item>
          <el-descriptions-item label="追踪ID">{{ detailData.traceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求IP">{{ detailData.ipAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ detailData.createTime }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAuditLogPage, getAuditLogDetail } from '@/api/auditLog'
import StatusTag from '@/components/StatusTag.vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const query = reactive({
  operator: '',
  operationType: '',
  targetType: '',
  result: '',
  traceId: ''
})

onMounted(() => fetchLogs())

async function fetchLogs() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    Object.keys(query).forEach(k => { if (query[k]) params[k] = query[k] })
    const data = await getAuditLogPage(params)
    logs.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    logs.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; fetchLogs() }
function resetSearch() {
  Object.keys(query).forEach(k => { query[k] = '' })
  pageNum.value = 1; fetchLogs()
}

// 详情弹窗
const detailVisible = ref(false)
const detailData = ref(null)
async function showDetail(row) {
  try {
    detailData.value = await getAuditLogDetail(row.id)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取日志详情失败')
  }
}

</script>
