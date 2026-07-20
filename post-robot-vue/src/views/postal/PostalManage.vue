<template>
  <div style="display: flex; flex-direction: column; gap: 16px;">
    <!-- Mock 状态卡片 -->
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span><el-icon style="vertical-align: middle"><Connection /></el-icon> 邮政系统对接状态</span>
          <div>
            <el-tag :type="mockEnabled ? 'success' : 'warning'" size="small">
              {{ mockEnabled ? 'Mock 模式' : '生产模式' }}
            </el-tag>
            <el-button size="small" style="margin-left: 12px" @click="loadMockStatus">刷新</el-button>
            <el-button size="small" type="warning" @click="handleResetMock">重置 Mock</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="API 地址">{{ apiBaseUrl }}</el-descriptions-item>
        <el-descriptions-item label="机构编码">{{ orgCode }}</el-descriptions-item>
        <el-descriptions-item label="平台编码">{{ platformCode }}</el-descriptions-item>
        <el-descriptions-item label="Mock 状态记录数">{{ mockStatusCount }}</el-descriptions-item>
        <el-descriptions-item label="签名密钥">{{ maskedSecret }}</el-descriptions-item>
        <el-descriptions-item label="目标系统ID">XYDYYQDXT</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 接口测试选项卡 -->
    <el-card>
      <template #header>
        <span><el-icon style="vertical-align: middle"><Tools /></el-icon> 接口测试</span>
      </template>
      <el-tabs type="border-card">
        <!-- F1 资费查询 -->
        <el-tab-pane label="F1 资费查询">
          <el-form :model="postageForm" label-width="100px" size="small">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="产品代码">
                  <el-input v-model="postageForm.productCode" placeholder="YT" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="重量(克)">
                  <el-input-number v-model="postageForm.weight" :min="1" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="是否保价">
                  <el-switch v-model="postageForm.insured" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="寄达省">
                  <el-input v-model="postageForm.destProvince" placeholder="安徽省" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="寄达市">
                  <el-input v-model="postageForm.destCity" placeholder="合肥市" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="寄达区">
                  <el-input v-model="postageForm.destDistrict" placeholder="蜀山区" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" @click="handleQueryPostage" :loading="loading.postage">
                查询资费
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="postageResult" type="success" show-icon :closable="false">
            <template #title>
              总资费: ¥{{ (postageResult.totalPostage / 100).toFixed(2) }} |
              运费: ¥{{ (postageResult.freight / 100).toFixed(2) }} |
              保价费: ¥{{ (postageResult.insuranceFee / 100).toFixed(2) }} |
              包装费: ¥{{ (postageResult.packingFee / 100).toFixed(2) }}
            </template>
          </el-alert>
        </el-tab-pane>

        <!-- F2 号码生成 -->
        <el-tab-pane label="F2 号码生成">
          <el-form :model="mailNumberForm" label-width="120px" size="small">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="省份代码">
                  <el-input v-model="mailNumberForm.provinceCode" placeholder="340000" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="机构编号">
                  <el-input v-model="mailNumberForm.orgCode" placeholder="AHFY01" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="产品代码">
                  <el-input v-model="mailNumberForm.productCode" placeholder="YT" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" @click="handleGenerateMailNo" :loading="loading.mailNo">
                生成号码
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="mailNoResult" type="success" show-icon :closable="false">
            <template #title>
              生成的邮件号码: <strong>{{ mailNoResult.mailNo }}</strong>
            </template>
          </el-alert>
        </el-tab-pane>

        <!-- F3 收寄订单提交 -->
        <el-tab-pane label="F3 收寄订单提交">
          <el-form :model="orderSubmitForm" label-width="120px" size="small">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="邮件号码">
                  <el-input v-model="orderSubmitForm.mailNo" placeholder="邮件号码" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="重量(克)">
                  <el-input-number v-model="orderSubmitForm.weight" :min="1" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="件数">
                  <el-input-number v-model="orderSubmitForm.pieceCount" :min="1" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" @click="handleSubmitOrder" :loading="loading.submitOrder">
                提交订单
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="orderSubmitResult" type="success" show-icon :closable="false">
            <template #title>
              交易流水号: {{ orderSubmitResult.transactionId }} |
              总资费: ¥{{ orderSubmitResult.totalPostage ? (orderSubmitResult.totalPostage / 100).toFixed(2) : '-' }}
            </template>
          </el-alert>
        </el-tab-pane>

        <!-- F4/F5 QR码 + 支付状态 -->
        <el-tab-pane label="F4+F5 二维码与支付">
          <el-form :model="qrForm" label-width="120px" size="small">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="订单号">
                  <el-input v-model="qrForm.orderNo" placeholder="ORD20260720000001" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="金额(分)">
                  <el-input-number v-model="qrForm.amount" :min="1" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="支付流水号">
                  <el-input v-model="qrForm.paymentFlowNo" placeholder="PAY20260720000001" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" @click="handleGenerateQr" :loading="loading.qrCode">
                生成二维码
              </el-button>
              <el-button
                style="margin-left: 12px"
                @click="handleQueryPayment"
                :loading="loading.paymentStatus"
                :disabled="!qrResult?.payQueryNo"
              >
                查询支付状态
              </el-button>
            </el-form-item>
          </el-form>
          <template v-if="qrResult">
            <el-alert type="info" show-icon :closable="false" style="margin-bottom: 8px;">
              <template #title>
                二维码链接: {{ qrResult.qrCodeUrl }} | 查询流水号: {{ qrResult.payQueryNo }}
              </template>
            </el-alert>
          </template>
          <el-alert v-if="paymentStatusResult" :type="paymentStatusType" show-icon :closable="false">
            <template #title>
              支付状态: {{ paymentStatusResult.statusDesc }} ({{ paymentStatusResult.payStatus }})
              <template v-if="paymentStatusResult.paidTime">
                | 支付完成时间: {{ paymentStatusResult.paidTime }}
              </template>
            </template>
          </el-alert>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  queryPostage,
  generateMailNumber,
  submitOrder,
  generateQrCode,
  getPaymentStatus,
  getMockStatus,
  resetMock
} from '@/api/postal'

// Mock 状态
const mockEnabled = ref(true)
const apiBaseUrl = ref('')
const orgCode = ref('')
const platformCode = ref('')
const mockStatusCount = ref(0)
const maskedSecret = ref('')

const loading = reactive({
  postage: false,
  mailNo: false,
  submitOrder: false,
  qrCode: false,
  paymentStatus: false
})

// 表单数据
const postageForm = reactive({
  productCode: 'YT',
  weight: 500,
  insured: false,
  destProvince: '安徽省',
  destCity: '合肥市',
  destDistrict: '蜀山区'
})

const mailNumberForm = reactive({
  provinceCode: '340000',
  orgCode: 'AHFY01',
  productCode: 'YT'
})

const orderSubmitForm = reactive({
  mailNo: '202600000001',
  weight: 500,
  pieceCount: 1
})

const qrForm = reactive({
  orderNo: 'ORD' + new Date().toISOString().slice(0,10).replace(/-/g,'') + '00001',
  amount: 1200,
  paymentFlowNo: 'PAY' + new Date().toISOString().slice(0,10).replace(/-/g,'') + '00001'
})

// 结果
const postageResult = ref(null)
const mailNoResult = ref(null)
const orderSubmitResult = ref(null)
const qrResult = ref(null)
const paymentStatusResult = ref(null)
const paymentStatusType = ref('info')

onMounted(() => {
  loadMockStatus()
})

async function loadMockStatus() {
  try {
    const data = await getMockStatus()
    mockEnabled.value = data.mockEnabled
    mockStatusCount.value = data.paymentStatusCount || 0
    apiBaseUrl.value = localStorage.getItem('apiBaseUrl') || 'yyqduat.11185.cn'
    orgCode.value = 'AHFY01'
    platformCode.value = 'POSTR'
    maskedSecret.value = 'dcff****nrq8'
  } catch (e) {
    // 静默处理
  }
}

async function handleResetMock() {
  try {
    await ElMessageBox.confirm('确定要重置 Mock 状态机吗？', '提示')
    await resetMock()
    ElMessage.success('Mock 状态已重置')
    await loadMockStatus()
    paymentStatusResult.value = null
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('重置失败')
  }
}

async function handleQueryPostage() {
  loading.postage = true
  try {
    postageResult.value = await queryPostage({
      productCode: postageForm.productCode,
      weight: postageForm.weight,
      insured: postageForm.insured,
      destProvince: postageForm.destProvince,
      destCity: postageForm.destCity,
      destDistrict: postageForm.destDistrict
    })
  } catch (e) {
    postageResult.value = null
  } finally {
    loading.postage = false
  }
}

async function handleGenerateMailNo() {
  loading.mailNo = true
  try {
    mailNoResult.value = await generateMailNumber({
      provinceCode: mailNumberForm.provinceCode,
      orgCode: mailNumberForm.orgCode,
      productCode: mailNumberForm.productCode,
      productName: '标准快递',
      sourceCode: 'ROBOT'
    })
  } catch (e) {
    mailNoResult.value = null
  } finally {
    loading.mailNo = false
  }
}

async function handleSubmitOrder() {
  loading.submitOrder = true
  try {
    orderSubmitResult.value = await submitOrder({
      mailNo: orderSubmitForm.mailNo,
      weight: orderSubmitForm.weight,
      pieceCount: orderSubmitForm.pieceCount,
      productCode: 'YT',
      productName: '标准快递',
      staffCode: 'ROBOT01',
      orgCode: 'AHFY01',
      sender: {
        name: '主题邮局',
        phone: '0551-12345678',
        province: '安徽省',
        city: '合肥市',
        district: '包河区',
        address: '主题邮局营业厅'
      },
      recipient: {
        name: '测试收件人',
        phone: '13800138000',
        province: '北京市',
        city: '北京市',
        district: '朝阳区',
        address: '测试地址'
      }
    })
  } catch (e) {
    orderSubmitResult.value = null
  } finally {
    loading.submitOrder = false
  }
}

async function handleGenerateQr() {
  loading.qrCode = true
  try {
    qrResult.value = await generateQrCode({
      orderNo: qrForm.orderNo,
      amount: String(qrForm.amount),
      paymentFlowNo: qrForm.paymentFlowNo
    })
    paymentStatusResult.value = null
    ElMessage.success('二维码生成成功')
  } catch (e) {
    qrResult.value = null
  } finally {
    loading.qrCode = false
  }
}

async function handleQueryPayment() {
  if (!qrResult.value?.payQueryNo) {
    ElMessage.warning('请先生成二维码')
    return
  }
  loading.paymentStatus = true
  try {
    const data = await getPaymentStatus({
      queryNo: qrResult.value.payQueryNo,
      paymentFlowNo: qrForm.paymentFlowNo
    })
    paymentStatusResult.value = data

    if (data.payStatus === '01') paymentStatusType.value = 'success'
    else if (data.payStatus === '02') paymentStatusType.value = 'danger'
    else paymentStatusType.value = 'warning'
  } catch (e) {
    paymentStatusResult.value = null
  } finally {
    loading.paymentStatus = false
  }
}
</script>
