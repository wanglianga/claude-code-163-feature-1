<template>
  <div class="page">
    <el-card class="page-card">
      <template #header>
        <div class="flex-between">
          <b>费用分摊复核（应急处置费 + 维修费 + 赔付金）</b>
          <span class="text-muted">街道先行垫付的费用，复核后按权属追责结论分摊</span>
        </div>
      </template>
      <el-table :data="list" size="large">
        <el-table-column label="单号" width="70" prop="id" />
        <el-table-column label="关联事件" width="160">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/incidents/${row.incidentId}`)">
              {{ codeOf(row.incidentId) }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="应急费" width="100">
          <template #default="{ row }">{{ money(row.emergencyCost) }}</template>
        </el-table-column>
        <el-table-column label="维修费" width="100">
          <template #default="{ row }">{{ money(row.repairCost) }}</template>
        </el-table-column>
        <el-table-column label="赔付金" width="100">
          <template #default="{ row }">{{ money(row.compensationAmount) }}</template>
        </el-table-column>
        <el-table-column label="合计" width="110">
          <template #default="{ row }"><b>{{ money(row.totalAmount) }}</b></template>
        </el-table-column>
        <el-table-column label="分摊明细" min-width="220">
          <template #default="{ row }">
            <div v-for="(a, i) in parseAlloc(row.allocations)" :key="i" style="font-size: 12px">
              {{ a.party }}：{{ money(a.amount) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="COST_STATUS[row.status]?.type" size="small">{{ COST_STATUS[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="复核时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.reviewedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING' && role === 'ADMIN'" type="primary"
                       size="small" @click="$router.push(`/incidents/${row.incidentId}`)">
              去复核
            </el-button>
            <el-button v-else size="small" @click="$router.push(`/incidents/${row.incidentId}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '../store/auth'
import { apiCostAllocations, apiIncidents } from '../api'
import { COST_STATUS, fmtTime, money } from '../utils/format'

const auth = useAuthStore()
const role = computed(() => auth.role)
const list = ref([])
const incidents = ref([])

function codeOf(incidentId) {
  return incidents.value.find(i => i.id === incidentId)?.code || `#${incidentId}`
}

function parseAlloc(json) {
  try {
    return JSON.parse(json || '[]')
  } catch {
    return []
  }
}

onMounted(async () => {
  list.value = await apiCostAllocations()
  incidents.value = await apiIncidents()
})
</script>
