<template>
  <div>
    <h3 style="margin-bottom: 16px;">库存管理</h3>
    <el-card>
      <!-- 查询表单 -->
      <el-form :model="query" inline style="margin-bottom: 8px;">
        <el-form-item label="商品名称">
          <el-input v-model="query.productName" placeholder="名称模糊搜索" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="商品ID">
          <el-input v-model="query.productId" placeholder="精确匹配" clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="样品状态">
          <el-select v-model="query.sampleStatus" placeholder="全部" clearable style="width: 140px;">
            <el-option label="正常" value="NORMAL" />
            <el-option label="缺失" value="MISSING" />
            <el-option label="错位" value="DISPLACED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 库存列表 -->
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="productId" label="商品ID" width="80" />
        <el-table-column prop="productName" label="商品名称" min-width="140" />
        <el-table-column prop="realStock" label="实时库存" width="100" align="center" />
        <el-table-column prop="lockedStock" label="锁定库存" width="100" align="center" />
        <el-table-column prop="availableStock" label="可用库存" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.availableStock <= 0" type="danger" size="small">{{ row.availableStock }}</el-tag>
            <el-tag v-else-if="row.availableStock <= (row.lowStockThreshold || 0)" type="warning" size="small">{{ row.availableStock }}</el-tag>
            <span v-else>{{ row.availableStock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sampleStatus" label="样品状态" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :value="row.sampleStatus" />
          </template>
        </el-table-column>
        <el-table-column prop="mismatchFlag" label="账实一致" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="!row.mismatchFlag" type="success" size="small">一致</el-tag>
            <el-tag v-else type="danger" size="small">异常</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lowStockThreshold" label="告警阈值" width="90" align="center" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openInbound(row)">入库</el-button>
            <el-button link type="warning" @click="openOutbound(row)">出库</el-button>
            <el-button link type="info" @click="openAdjust(row)">调整</el-button>
            <el-popconfirm title="确认删除该库存记录？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
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
    </el-card>

    <!-- 入库/出库对话框 -->
    <el-dialog v-model="stockDialogVisible" :title="stockDialogTitle" width="400px">
      <el-form :model="stockForm" label-width="80px">
        <el-form-item label="商品">
          <span>{{ currentRow?.productName }}（ID: {{ currentRow?.productId }}）</span>
        </el-form-item>
        <el-form-item label="当前可用">
          <span>{{ currentRow?.availableStock }}</span>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="stockForm.quantity" :min="1" :max="9999" style="width: 200px;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmStock" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>

    <!-- 调整库存对话框 -->
    <el-dialog v-model="adjustDialogVisible" title="手动调整库存（盘点修正）" width="450px">
      <el-form :model="adjustForm" label-width="120px">
        <el-form-item label="商品">
          <span>{{ currentRow?.productName }}（ID: {{ currentRow?.productId }}）</span>
        </el-form-item>
        <el-form-item label="实时库存">
          <el-input-number v-model="adjustForm.realStock" :min="0" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="锁定库存">
          <el-input-number v-model="adjustForm.lockedStock" :min="0" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="低库存阈值">
          <el-input-number v-model="adjustForm.lowStockThreshold" :min="0" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="样品状态">
          <el-select v-model="adjustForm.sampleStatus" style="width: 200px;">
            <el-option label="正常" value="NORMAL" />
            <el-option label="缺失" value="MISSING" />
            <el-option label="错位" value="DISPLACED" />
          </el-select>
        </el-form-item>
        <el-form-item label="账实一致">
          <el-switch v-model="adjustForm.mismatchFlag" :active-value="true" :inactive-value="false" active-text="异常" inactive-text="一致" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjust" :loading="submitting">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getInventoryPage, inboundStock, outboundStock, adjustStock, deleteInventory } from '@/api/inventory'
import StatusTag from '@/components/StatusTag.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const query = reactive({
  productName: '',
  productId: '',
  sampleStatus: null
})

// 入库/出库
const stockDialogVisible = ref(false)
const submitting = ref(false)
const currentRow = ref(null)
const stockMode = ref('inbound')
const stockForm = reactive({ quantity: 1 })
const stockDialogTitle = computed(() => stockMode.value === 'inbound' ? '入库操作' : '出库操作')

// 调整库存
const adjustDialogVisible = ref(false)
const adjustForm = reactive({
  realStock: 0,
  lockedStock: 0,
  lowStockThreshold: 10,
  sampleStatus: 'NORMAL',
  mismatchFlag: false
})

function resetQuery() {
  query.productName = ''
  query.productId = ''
  query.sampleStatus = null
  pageNum.value = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (query.productName) params.productName = query.productName
    if (query.productId) params.productId = query.productId
    if (query.sampleStatus) params.sampleStatus = query.sampleStatus
    const res = await getInventoryPage(params)
    list.value = res.records || []
    total.value = res.total || 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

// 入库/出库
function openInbound(row) {
  stockMode.value = 'inbound'
  currentRow.value = row
  stockForm.quantity = 1
  stockDialogVisible.value = true
}

function openOutbound(row) {
  stockMode.value = 'outbound'
  currentRow.value = row
  stockForm.quantity = 1
  stockDialogVisible.value = true
}

async function confirmStock() {
  submitting.value = true
  try {
    if (stockMode.value === 'inbound') {
      await inboundStock(currentRow.value.productId, stockForm.quantity)
      ElMessage.success('入库成功')
    } else {
      await outboundStock(currentRow.value.productId, stockForm.quantity)
      ElMessage.success('出库成功')
    }
    stockDialogVisible.value = false
    loadData()
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}

// 调整库存
function openAdjust(row) {
  currentRow.value = row
  adjustForm.realStock = row.realStock
  adjustForm.lockedStock = row.lockedStock
  adjustForm.lowStockThreshold = row.lowStockThreshold
  adjustForm.sampleStatus = row.sampleStatus || 'NORMAL'
  adjustForm.mismatchFlag = !!row.mismatchFlag
  adjustDialogVisible.value = true
}

async function confirmAdjust() {
  submitting.value = true
  try {
    await adjustStock({
      productId: currentRow.value.productId,
      realStock: adjustForm.realStock,
      lockedStock: adjustForm.lockedStock,
      lowStockThreshold: adjustForm.lowStockThreshold,
      sampleStatus: adjustForm.sampleStatus,
      mismatchFlag: adjustForm.mismatchFlag
    })
    ElMessage.success('调整成功')
    adjustDialogVisible.value = false
    loadData()
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}

// 删除
async function handleDelete(row) {
  try {
    await deleteInventory(row.productId)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // handled
  }
}

onMounted(loadData)
</script>
