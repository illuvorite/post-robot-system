<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span><el-icon style="vertical-align: middle"><WarningFilled /></el-icon> 告警管理</span>
          <el-button size="small" @click="fetchAlerts">刷新</el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :model="query" inline size="small" style="margin-bottom: 12px">
        <el-form-item label="类型">
          <el-select v-model="query.alertType" placeholder="全部" clearable style="width: 160px">
            <el-option label="库存不足" value="LOW_STOCK" />
            <el-option label="库存差异" value="STOCK_DISCREPANCY" />
            <el-option label="样品缺失" value="SAMPLE_MISSING" />
            <el-option label="任务失败" value="TASK_FAILURE" />
            <el-option label="支付超时" value="PAYMENT_TIMEOUT" />
            <el-option label="系统错误" value="SYSTEM_ERROR" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="未处理" value="UNRESOLVED" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="alerts" v-loading="loading" stripe size="small">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <StatusTag :value="row.alertType" />
          </template>
        </el-table-column>
        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <StatusTag :value="row.alertLevel" />
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="sourceId" label="来源ID" width="120" />
        <el-table-column prop="message" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="处理人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link :disabled="row.status === 'RESOLVED'" @click="handleResolve(row)">处理</el-button>
            <el-button size="small" type="danger" link @click="handleDeleteAlert(row)">删除</el-button>
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
        @change="fetchAlerts"
      />
    </el-card>

    <!-- 处理对话框 -->
    <el-dialog v-model="dialogVisible" title="处理告警" width="450px">
      <el-form :model="handleForm" label-width="80px">
        <el-form-item label="处理人">
          <el-input v-model="handleForm.handler" />
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="handleForm.note" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmHandle" :loading="submitting">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAlertPage, handleAlert, deleteAlert } from '@/api/alert'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'
import StatusTag from '@/components/StatusTag.vue'

const auth = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const alerts = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const currentAlert = ref(null)
const handleForm = reactive({ handler: '', note: '' })
const query = reactive({ alertType: '', status: '' })

onMounted(() => { fetchAlerts() })

async function fetchAlerts() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (query.alertType) params.alertType = query.alertType
    if (query.status) params.status = query.status
    const data = await getAlertPage(params)
    alerts.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    alerts.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; fetchAlerts() }
function resetSearch() { query.alertType = ''; query.status = ''; pageNum.value = 1; fetchAlerts() }

function handleResolve(row) {
  currentAlert.value = row
  handleForm.handler = auth.user?.username || 'admin'
  handleForm.note = ''
  dialogVisible.value = true
}

async function confirmHandle() {
  submitting.value = true
  try {
    await handleAlert(currentAlert.value.id, handleForm.handler, handleForm.note)
    ElMessage.success('告警已处理')
    dialogVisible.value = false
    fetchAlerts()
  } catch (e) {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDeleteAlert(row) {
  try {
    await ElMessageBox.confirm(`确定删除告警 #${row.id} 吗？`, '提示')
    await deleteAlert(row.id)
    ElMessage.success('告警已删除')
    fetchAlerts()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}
</script>
