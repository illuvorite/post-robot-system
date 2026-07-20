<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h3 style="margin: 0;">商品管理</h3>
      <div>
        <el-button type="primary" @click="$router.push('/products/add')">
          <el-icon><Plus /></el-icon>新增商品
        </el-button>
      </div>
    </div>
    <el-card>
      <el-form :model="query" inline style="margin-bottom: 8px;">
        <el-form-item label="商品名称">
          <el-input v-model="query.name" placeholder="名称模糊搜索" clearable style="width: 180px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="机器人抓取">
          <el-select v-model="query.robotGraspable" placeholder="全部" clearable style="width: 140px;">
            <el-option label="支持" :value="true" />
            <el-option label="不支持" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="商品名称" min-width="160" />
        <el-table-column prop="price" label="售价" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" width="180">
          <template #default="{ row }">
            <el-tag v-for="t in (row.tags || '').split(',')" :key="t" size="small" style="margin-right: 4px; margin-bottom: 2px;" v-if="t">{{ t }}</el-tag>
            <span v-if="!row.tags">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="robotGraspable" label="机器人抓取" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.robotGraspable" type="success" size="small">支持</el-tag>
            <el-tag v-else type="info" size="small">不支持</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="displayPoint" label="陈列点位" width="100" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <StatusTag :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/products/edit/${row.id}`)">编辑</el-button>
            <el-popconfirm
              :title="row.status === 1 ? '确认下架该商品？' : '确认上架该商品？'"
              @confirm="toggleStatus(row)"
            >
              <template #reference>
                <el-button link type="primary">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="确认删除该商品？" @confirm="handleDelete(row.id)">
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getProductPage, changeProductStatus, deleteProduct } from '@/api/product'
import StatusTag from '@/components/StatusTag.vue'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const query = reactive({
  name: '',
  status: null,
  robotGraspable: null
})

function resetQuery() {
  query.name = ''
  query.status = null
  query.robotGraspable = null
  pageNum.value = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (query.name) params.name = query.name
    if (query.status !== null && query.status !== '') params.status = query.status
    if (query.robotGraspable !== null && query.robotGraspable !== '') params.robotGraspable = query.robotGraspable
    const res = await getProductPage(params)
    list.value = res.records || []
    total.value = res.total || 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await changeProductStatus(row.id, newStatus)
  ElMessage.success(newStatus === 1 ? '上架成功' : '下架成功')
  loadData()
}

async function handleDelete(id) {
  await deleteProduct(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
