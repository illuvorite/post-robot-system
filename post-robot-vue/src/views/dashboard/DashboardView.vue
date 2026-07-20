<template>
  <div>
    <h3 style="margin-bottom: 16px;">运营概览</h3>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon :size="32" color="#409eff"><Goods /></el-icon>
            <div>
              <div class="stat-value">{{ stats.productCount }}</div>
              <div class="stat-label">商品总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon :size="32" color="#67c23a"><Coin /></el-icon>
            <div>
              <div class="stat-value">{{ stats.inventoryCount }}</div>
              <div class="stat-label">库存记录</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon :size="32" color="#e6a23c"><List /></el-icon>
            <div>
              <div class="stat-value">{{ stats.taskCount }}</div>
              <div class="stat-label">任务总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon :size="32" color="#f56c6c"><WarningFilled /></el-icon>
            <div>
              <div class="stat-value">{{ stats.alertCount }}</div>
              <div class="stat-label">待处理告警</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <!-- 商品推荐预览 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>商品推荐预览</span>
          </template>
          <el-form :model="recForm" inline style="margin-bottom: 8px;">
            <el-form-item label="偏好标签">
              <el-input v-model="recForm.intentTags" placeholder="逗号分隔，如：礼品,纪念,收藏" clearable style="width: 220px;" />
            </el-form-item>
            <el-form-item label="预算范围">
              <el-input-number v-model="recForm.budgetMin" :min="0" :precision="2" placeholder="最低" style="width: 120px;" controls-position="right" />
              <span style="margin: 0 6px;">~</span>
              <el-input-number v-model="recForm.budgetMax" :min="0" :precision="2" placeholder="最高" style="width: 120px;" controls-position="right" />
            </el-form-item>
            <el-form-item label="仅可抓取">
              <el-switch v-model="recForm.onlyGraspable" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadRecommend" :loading="recLoading">推荐</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="recList" v-loading="recLoading" border stripe empty-text="点击「推荐」查看结果">
            <el-table-column label="商品名称" min-width="140">
              <template #default="{ row }">
                <el-popover placement="right" trigger="hover" :width="280">
                  <template #reference>
                    <el-link type="primary" :underline="false">{{ row.name }}</el-link>
                  </template>
                  <div>
                    <p style="margin: 0 0 6px; font-weight: bold;">{{ row.name }}</p>
                    <p style="margin: 0 0 4px; font-size: 13px; color: #666;">{{ row.description || '暂无描述' }}</p>
                    <p style="margin: 0; font-size: 13px; color: #999;">标签：{{ row.tags || '-' }}</p>
                  </div>
                </el-popover>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="售价" width="90" align="center">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="matchScore" label="匹配度" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="scoreType(row.matchScore)" size="small">{{ row.matchScore }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="robotGraspable" label="可抓取" width="70" align="center">
              <template #default="{ row }">
                <el-icon v-if="row.robotGraspable" color="#67c23a" size="18"><Select /></el-icon>
                <el-icon v-else color="#c0c4cc" size="18"><Close /></el-icon>
              </template>
            </el-table-column>
            <el-table-column prop="displayPoint" label="点位" width="80" align="center" />
            <el-table-column prop="recommendReason" label="推荐理由" min-width="200" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>

      <!-- 系统信息 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>系统信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="当前用户">{{ auth.user?.realName || auth.user?.username }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <StatusTag :value="auth.user?.role" />
            </el-descriptions-item>
            <el-descriptions-item label="系统版本">v1.0.0</el-descriptions-item>
            <el-descriptions-item label="运行状态">
              <el-tag type="success" effect="plain" size="small">正常</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getProductPage, recommendProduct } from '@/api/product'
import { getInventoryPage } from '@/api/inventory'
import { getTaskPage } from '@/api/task'
import { countUnresolvedAlerts } from '@/api/alert'
import StatusTag from '@/components/StatusTag.vue'

const auth = useAuthStore()

const stats = ref({
  productCount: '-',
  inventoryCount: '-',
  taskCount: '-',
  alertCount: '-'
})

// 推荐
const recList = ref([])
const recLoading = ref(false)
const recForm = reactive({
  intentTags: '',
  budgetMin: null,
  budgetMax: null,
  onlyGraspable: false
})

function scoreType(score) {
  if (score >= 80) return 'success'
  if (score >= 50) return 'warning'
  return 'info'
}

async function loadRecommend() {
  recLoading.value = true
  try {
    const params = {}
    if (recForm.intentTags.trim()) {
      params.intentTags = recForm.intentTags.split(',').map(t => t.trim()).filter(Boolean)
    }
    if (recForm.budgetMin !== null && recForm.budgetMin > 0) params.budgetMin = recForm.budgetMin
    if (recForm.budgetMax !== null && recForm.budgetMax > 0) params.budgetMax = recForm.budgetMax
    params.onlyGraspable = recForm.onlyGraspable
    const res = await recommendProduct(params)
    recList.value = res || []
  } catch {
    recList.value = []
  } finally {
    recLoading.value = false
  }
}

onMounted(async () => {
  try {
    const prodRes = await getProductPage({ pageNum: 1, pageSize: 1 })
    stats.value.productCount = prodRes.total || 0
  } catch {}
  try {
    const invRes = await getInventoryPage({ pageNum: 1, pageSize: 1 })
    stats.value.inventoryCount = invRes.total || 0
  } catch {}
  try {
    const taskRes = await getTaskPage({ pageNum: 1, pageSize: 1 })
    stats.value.taskCount = taskRes.total || 0
  } catch {}
  try {
    stats.value.alertCount = await countUnresolvedAlerts()
  } catch {}
})
</script>

<style scoped>
.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
