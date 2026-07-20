<template>
  <div>
    <h3 style="margin-bottom: 16px;">用户管理</h3>
    <div style="margin-bottom: 16px;">
      <el-button type="primary" @click="openAdd">
        <el-icon><Plus /></el-icon>新增用户
      </el-button>
    </div>
    <el-card>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="role" label="角色" width="120" align="center">
          <template #default="{ row }">
            <StatusTag :value="row.role" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">启用</el-tag>
            <el-tag v-else type="danger" size="small">停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该用户？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" :disabled="row.id === currentUserId">删除</el-button>
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

    <!-- 新增/编辑用户对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%;">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="运营人员" value="OPERATOR" />
            <el-option label="维护人员" value="MAINTAINER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getUserPage, addUser, editUser, deleteUser, getUserDetail } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import StatusTag from '@/components/StatusTag.vue'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const list = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const currentEditId = ref(null)
const formRef = ref(null)

const currentUserId = computed(() => auth.user?.id)

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  role: 'OPERATOR',
  status: 1
})

const rules = {
  username: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
  realName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getUserPage({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = res.records || []
    total.value = res.total || 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  isEdit.value = false
  currentEditId.value = null
  form.username = ''
  form.realName = ''
  form.phone = ''
  form.email = ''
  form.role = 'OPERATOR'
  form.status = 1
  dialogVisible.value = true
}

async function openEdit(row) {
  isEdit.value = true
  currentEditId.value = row.id
  Object.assign(form, {
    username: row.username,
    realName: row.realName,
    phone: row.phone || '',
    email: row.email || '',
    role: row.role,
    status: row.status
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await editUser({ id: currentEditId.value, ...form })
      ElMessage.success('更新成功')
    } else {
      await addUser(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  await deleteUser(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
