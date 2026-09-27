<template>
  <div class="page">
    <el-card class="page-card">
      <template #header><b>赔付案件管理</b></template>
      <el-tabs v-model="tab">
        <el-tab-pane v-for="t in tabs" :key="t.label" :label="`${t.label}(${t.list.length})`" :name="t.label" />
      </el-tabs>
      <el-table :data="current" size="large">
        <el-table-column label="案件号" width="80" prop="id" />
        <el-table-column label="关联事件" width="160">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/incidents/${row.incidentId}`)">
              {{ codeOf(row.incidentId) }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="claimantName" label="索赔人" width="100" />
        <el-table-column prop="damageType" label="损失类型" width="100" />
        <el-table-column label="索赔金额" width="110">
          <template #default="{ row }">{{ money(row.claimAmount) }}</template>
        </el-table-column>
        <el-table-column label="核定金额" width="110">
          <template #default="{ row }">{{ money(row.approvedAmount) }}</template>
        </el-table-column>
        <el-table-column label="责任认定" width="130">
          <template #default="{ row }">
            {{ row.responsibility ? RESPONSIBILITY[row.responsibility] : '-' }}
            <span v-if="row.responsibility === 'SHARED'" class="text-muted">
              {{ row.ownerRatio }}:{{ row.streetRatio }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="COMP_STATUS[row.status]?.type" size="small">{{ COMP_STATUS[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="立案时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'FILED' && role === 'COMPENSATOR'" type="primary"
                       size="small" @click="openDecide(row)">审核定责</el-button>
            <el-button v-if="row.status === 'APPROVED' && role === 'COMPENSATOR'" type="success"
                       size="small" @click="pay(row)">确认赔付</el-button>
            <el-button size="small" @click="view(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 审核定责对话框 -->
    <el-dialog v-model="decideVisible" title="赔付审核定责" width="640px">
      <el-alert type="info" :closable="false" class="mb16"
                title="依据上报时间、临时围护时效、权属确认过程、维修时长与事故证明综合判断责任。" />
      <el-form :model="decideForm" label-width="110px">
        <el-form-item label="损失类型">
          <el-select v-model="decideForm.damageType" style="width: 200px">
            <el-option v-for="t in meta.damageTypes || []" :key="t" :value="t" :label="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="索赔金额">
          <el-input-number v-model="decideForm.claimAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="事故证明">
          <el-input v-model="decideForm.accidentProof" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="审核结论">
          <el-radio-group v-model="decideForm.result">
            <el-radio value="APPROVED">核定赔付</el-radio>
            <el-radio value="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="decideForm.result === 'APPROVED'">
          <el-form-item label="责任认定">
            <el-select v-model="decideForm.responsibility" style="width: 200px">
              <el-option value="OWNER" label="权属单位全责" />
              <el-option value="STREET" label="街道责任（围护不及时）" />
              <el-option value="SHARED" label="多方分摊" />
              <el-option value="UNDETERMINED" label="无法认定" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="decideForm.responsibility === 'SHARED'" label="分摊比例">
            <el-input-number v-model="decideForm.ownerRatio" :min="0" :max="100" />
            <span style="margin: 0 6px">% 权属单位 / 街道</span>
            <el-input-number v-model="decideForm.streetRatio" :min="0" :max="100" />
            <span style="margin-left: 6px">%</span>
          </el-form-item>
          <el-form-item label="核定金额">
            <el-input-number v-model="decideForm.approvedAmount" :min="0" :precision="2" />
          </el-form-item>
        </template>
        <el-form-item label="责任分析" required>
          <el-input v-model="decideForm.analysis" type="textarea" :rows="4"
                    placeholder="从上报时间、临时围护时效、权属确认、维修时长、事故证明五方面分析" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decideVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="decide">提交审核</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="viewVisible" title="赔付案件详情" size="480px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="索赔人">{{ current.claimantName }}（{{ current.claimantPhone }}）</el-descriptions-item>
        <el-descriptions-item label="损失类型">{{ current.damageType }}</el-descriptions-item>
        <el-descriptions-item label="索赔金额">{{ money(current.claimAmount) }}</el-descriptions-item>
        <el-descriptions-item label="事故证明">{{ current.accidentProof }}</el-descriptions-item>
        <el-descriptions-item label="责任分析">{{ current.analysis || '-' }}</el-descriptions-item>
        <el-descriptions-item label="责任认定">
          {{ current.responsibility ? RESPONSIBILITY[current.responsibility] : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="核定金额">{{ money(current.approvedAmount) }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ fmtTime(current.decidedAt) }}</el-descriptions-item>
        <el-descriptions-item label="赔付时间">{{ fmtTime(current.paidAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { apiCompensations, apiCompUpdate, apiCompDecide, apiCompPay, apiIncidents, apiMeta } from '../api'
import { COMP_STATUS, RESPONSIBILITY, fmtTime, money } from '../utils/format'

const auth = useAuthStore()
const role = computed(() => auth.role)
const list = ref([])
const incidents = ref([])
const meta = ref({})
const tab = ref('全部')
const acting = ref(false)

const tabs = computed(() => [
  { label: '全部', list: list.value },
  { label: '待审核', list: list.value.filter(c => c.status === 'FILED') },
  { label: '待赔付', list: list.value.filter(c => c.status === 'APPROVED') },
  { label: '已赔付', list: list.value.filter(c => c.status === 'PAID') },
  { label: '已驳回', list: list.value.filter(c => c.status === 'REJECTED') }
])
const current = computed(() => tabs.value.find(t => t.label === tab.value)?.list || [])

function codeOf(incidentId) {
  return incidents.value.find(i => i.id === incidentId)?.code || `#${incidentId}`
}

const decideVisible = ref(false)
const viewVisible = ref(false)
const currentCase = ref(null)
const decideForm = reactive({
  damageType: '', claimAmount: 0, accidentProof: '', result: 'APPROVED',
  responsibility: 'OWNER', ownerRatio: 100, streetRatio: 0, approvedAmount: 0, analysis: ''
})

function openDecide(row) {
  currentCase.value = row
  Object.assign(decideForm, {
    damageType: row.damageType === '待补充' ? '车辆损失' : row.damageType,
    claimAmount: row.claimAmount,
    accidentProof: row.accidentProof,
    result: 'APPROVED',
    responsibility: 'OWNER',
    ownerRatio: 100,
    streetRatio: 0,
    approvedAmount: row.claimAmount,
    analysis: ''
  })
  decideVisible.value = true
}

async function decide() {
  if (!decideForm.analysis.trim()) return ElMessage.warning('请填写责任分析')
  if (decideForm.responsibility === 'SHARED'
      && decideForm.ownerRatio + decideForm.streetRatio !== 100) {
    return ElMessage.warning('分摊比例之和须为 100%')
  }
  acting.value = true
  try {
    await apiCompUpdate(currentCase.value.id, {
      damageType: decideForm.damageType,
      claimAmount: decideForm.claimAmount,
      accidentProof: decideForm.accidentProof
    })
    await apiCompDecide(currentCase.value.id, decideForm)
    ElMessage.success('审核完成')
    decideVisible.value = false
    load()
  } finally {
    acting.value = false
  }
}

async function pay(row) {
  await ElMessageBox.confirm(`确认向 ${row.claimantName} 赔付 ${money(row.approvedAmount)}？`, '赔付确认')
  await apiCompPay(row.id)
  ElMessage.success('已标记赔付完成')
  load()
}

function view(row) {
  currentCase.value = row
  viewVisible.value = true
}

async function load() {
  list.value = await apiCompensations()
  incidents.value = await apiIncidents()
}

onMounted(async () => {
  await load()
  meta.value = await apiMeta()
})
</script>
