<template>
  <div>
    <h3 style="margin-bottom: 16px;">任务监控</h3>
    <el-card>
      <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
        <el-form :model="query" inline>
          <el-form-item label="任务状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px;">
              <el-option label="已创建" value="CREATED" />
              <el-option label="排队中" value="QUEUED" />
              <el-option label="执行中" value="RUNNING" />
              <el-option label="已完成" value="SUCCEEDED" />
              <el-option label="失败" value="FAILED" />
              <el-option label="需人工处理" value="MANUAL_REQUIRED" />
            </el-select>
          </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="query.taskType" placeholder="全部" clearable style="width: 140px;">
            <el-option label="导航" value="NAVIGATION" />
            <el-option label="抓取" value="GRASP" />
            <el-option label="展示" value="DISPLAY" />
            <el-option label="讲解" value="EXPLAIN" />
            <el-option label="结算" value="SETTLEMENT" />
            <el-option label="库存巡检" value="INVENTORY_CHECK" />
            <el-option label="巡检" value="PATROL" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
        </el-form>
        <el-button type="success" @click="showCreateDialog">新建任务</el-button>
      </div>
      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="taskNo" label="任务编号" width="160" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.taskType)" size="small">{{ typeLabel(row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <StatusTag :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.priority === 1 ? 'danger' : row.priority <= 2 ? 'warning' : 'info'" size="small">
              P{{ row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="80" align="center" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" align="center" />
        <el-table-column prop="failReason" label="失败原因" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'RUNNING'" link type="warning" @click="handleCancel(row)">取消</el-button>
            <el-button v-if="row.status === 'FAILED'" link type="primary" @click="handleRetry(row)">重试</el-button>
            <el-button v-if="row.status === 'MANUAL_REQUIRED'" link type="danger" @click="handleRetry(row)">人工重试</el-button>
            <el-button v-if="row.status !== 'RUNNING'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
      <!-- 新建任务 -->
      <el-button type="success" size="small" @click="showCreateDialog" style="margin-top: 8px">新建任务</el-button>
    </el-card>

    <!-- 新建任务对话框 -->
    <el-dialog v-model="createVisible" title="新建任务" width="500px">
      <el-form :model="createForm" label-width="100px" size="small">
        <el-form-item label="任务类型" required>
          <el-select v-model="createForm.taskType" style="width: 100%">
            <el-option v-for="t in taskTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="createForm.priority" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="超时(秒)">
          <el-input-number v-model="createForm.timeoutSeconds" :min="10" :step="10" />
        </el-form-item>
        <el-form-item label="最大重试">
          <el-input-number v-model="createForm.maxRetry" :min="0" :max="10" />
        </el-form-item>
        <el-form-item label="依赖任务">
          <el-input v-model="createForm.dependencyTaskNo" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate" :loading="submitting">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑任务对话框 -->
    <el-dialog v-model="editVisible" title="编辑任务" width="500px">
      <el-form :model="editForm" label-width="100px" size="small">
        <el-form-item label="任务编号"><el-input v-model="editForm.taskNo" disabled /></el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="editForm.priority" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="超时(秒)">
          <el-input-number v-model="editForm.timeoutSeconds" :min="10" :step="10" />
        </el-form-item>
        <el-form-item label="最大重试">
          <el-input-number v-model="editForm.maxRetry" :min="0" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { getTaskPage, cancelTask, retryTask, createTask, editTask, deleteTask } from '@/api/task'
import StatusTag from '@/components/StatusTag.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const submitting = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
let timer = null

const query = reactive({
  status: null,
  taskType: null
})

const taskTypeOptions = [
  { value: 'NAVIGATION', label: '导航' }, { value: 'GRASP', label: '抓取' },
  { value: 'DISPLAY', label: '展示' }, { value: 'EXPLAIN', label: '讲解' },
  { value: 'SETTLEMENT', label: '结算' }, { value: 'INVENTORY_CHECK', label: '库存巡检' },
  { value: 'PATROL', label: '巡检' }, { value: 'OTHER', label: '其他' }
]

// 新建任务
const createVisible = ref(false)
const createForm = reactive({
  taskType: 'NAVIGATION', priority: 5, timeoutSeconds: 300, maxRetry: 3, dependencyTaskNo: ''
})
function showCreateDialog() {
  createForm.taskType = 'NAVIGATION'; createForm.priority = 5
  createForm.timeoutSeconds = 300; createForm.maxRetry = 3; createForm.dependencyTaskNo = ''
  createVisible.value = true
}
async function confirmCreate() {
  submitting.value = true
  try {
    const data = { taskType: createForm.taskType, priority: createForm.priority, timeoutSeconds: createForm.timeoutSeconds, maxRetry: createForm.maxRetry }
    if (createForm.dependencyTaskNo) data.dependencyTaskNo = createForm.dependencyTaskNo
    await createTask(data)
    ElMessage.success('任务创建成功')
    createVisible.value = false
    loadData()
  } catch {} finally { submitting.value = false }
}

// 编辑任务
const editVisible = ref(false)
const editForm = reactive({ id: null, taskNo: '', priority: 5, timeoutSeconds: 300, maxRetry: 3 })
const selectedRow = ref(null)
function showEditDialog(row) {
  editForm.id = row.id; editForm.taskNo = row.taskNo
  editForm.priority = row.priority; editForm.timeoutSeconds = row.timeoutSeconds
  editForm.maxRetry = row.maxRetry
  editVisible.value = true
}
async function confirmEdit() {
  submitting.value = true
  try {
    await editTask({ id: editForm.id, priority: editForm.priority, timeoutSeconds: editForm.timeoutSeconds, maxRetry: editForm.maxRetry })
    ElMessage.success('任务已更新')
    editVisible.value = false
    loadData()
  } catch {} finally { submitting.value = false }
}

function typeTag(type) {
  const map = { NAVIGATION: '', GRASP: 'warning', DISPLAY: 'success', EXPLAIN: 'primary', SETTLEMENT: 'primary', INVENTORY_CHECK: 'info', PATROL: 'info', OTHER: '' }
  return map[type] || ''
}

function typeLabel(type) {
  const map = { NAVIGATION: '导航', GRASP: '抓取', DISPLAY: '展示', EXPLAIN: '讲解', SETTLEMENT: '结算', INVENTORY_CHECK: '库存巡检', PATROL: '巡检', OTHER: '其他' }
  return map[type] || type
}

function resetQuery() {
  query.status = null
  query.taskType = null
  pageNum.value = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (query.status) params.status = query.status
    if (query.taskType) params.taskType = query.taskType
    const res = await getTaskPage(params)
    list.value = res.records || []
    total.value = res.total || 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function handleCancel(row) {
  await cancelTask(row.id)
  ElMessage.success('已取消')
  loadData()
}

async function handleRetry(row) {
  await retryTask(row.id)
  ElMessage.success('已重试')
  loadData()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除任务 ${row.taskNo} 吗？`, '提示')
    await deleteTask(row.id)
    ElMessage.success('任务已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadData()
  timer = setInterval(loadData, 5000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>
