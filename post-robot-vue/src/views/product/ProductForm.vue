<template>
  <div>
    <h3 style="margin-bottom: 16px;">{{ isEdit ? '编辑商品' : '新增商品' }}</h3>
    <el-card style="max-width: 700px;">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" v-loading="loading">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入商品描述" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，如：礼品,纪念,收藏" />
        </el-form-item>
        <el-form-item label="售价" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="商品图片URL">
          <el-input v-model="form.imageUrl" placeholder="请输入图片URL" />
        </el-form-item>
        <el-form-item label="机器人抓取">
          <el-switch v-model="form.robotGraspable" :active-value="true" :inactive-value="false" />
        </el-form-item>
        <el-form-item label="陈列点位">
          <el-input v-model="form.displayPoint" placeholder="如：A1-03" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="状态" v-if="isEdit">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">{{ isEdit ? '保存修改' : '确认新增' }}</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, addProduct, editProduct } from '@/api/product'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  name: '',
  description: '',
  tags: '',
  price: undefined,
  originalPrice: undefined,
  imageUrl: '',
  robotGraspable: false,
  displayPoint: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '商品名称不能为空', trigger: 'blur' }],
  price: [{ required: true, message: '售价不能为空', trigger: 'blur' }]
}

onMounted(async () => {
  if (isEdit.value) {
    loading.value = true
    try {
      const data = await getProductDetail(route.params.id)
      Object.assign(form, {
        name: data.name,
        description: data.description || '',
        tags: data.tags || '',
        price: data.price,
        originalPrice: data.originalPrice || undefined,
        imageUrl: data.imageUrl || '',
        robotGraspable: data.robotGraspable ?? false,
        displayPoint: data.displayPoint || '',
        status: data.status
      })
    } catch {
      ElMessage.error('商品不存在')
      router.push('/products')
    } finally {
      loading.value = false
    }
  }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await editProduct({ id: route.params.id, ...form })
      ElMessage.success('更新成功')
    } else {
      await addProduct(form)
      ElMessage.success('新增成功')
    }
    router.push('/products')
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}
</script>
