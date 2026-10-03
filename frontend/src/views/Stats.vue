<template>
  <div class="page">
    <!-- 总览卡片 -->
    <el-row :gutter="16">
      <el-col v-for="c in overviewCards" :key="c.label" :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="page-card" style="text-align: center">
          <div class="text-muted">{{ c.label }}</div>
          <div class="stat-num" :style="{ color: c.color }">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card class="page-card">
          <template #header><b>高发路段 TOP10</b></template>
          <div ref="hotspotChart" style="height: 320px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="page-card">
          <template #header><b>赔付金额（按月）</b></template>
          <div ref="compChart" style="height: 320px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card class="page-card">
          <template #header>
            <div class="flex-between">
              <b>权属争议单位</b>
              <span class="text-muted">仍责任待定 {{ disputes.stillPending || 0 }} 起 · 历史待定 {{ disputes.totalEverPending || 0 }} 起</span>
            </div>
          </template>
          <el-table :data="disputes.byOwner || []" size="large">
            <el-table-column prop="orgName" label="权属单位" min-width="140" />
            <el-table-column prop="total" label="承接事件" width="100" />
            <el-table-column label="曾责任待定" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.disputed > 0" type="danger" size="small">{{ row.disputed }} 起</el-tag>
                <span v-else class="text-muted">0</span>
              </template>
            </el-table-column>
            <el-table-column label="争议率" width="120">
              <template #default="{ row }">
                <el-progress :percentage="row.total ? Math.round(row.disputed / row.total * 100) : 0"
                             :stroke-width="10" :color="row.disputed ? '#f56c6c' : '#67c23a'" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="page-card">
          <template #header>
            <div class="flex-between">
              <b>维修超时工单</b>
              <span class="text-muted">阈值 {{ timeouts.timeoutHours || 48 }} 小时</span>
            </div>
          </template>
          <el-table :data="timeouts.list || []" size="large" max-height="300">
            <el-table-column prop="incidentCode" label="事件编号" width="150" />
            <el-table-column prop="team" label="维修队" width="130" />
            <el-table-column prop="owner" label="权属单位" width="130" />
            <el-table-column label="耗时" width="110">
              <template #default="{ row }">
                <el-tag type="danger" size="small">{{ row.hours }} 小时</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.done ? 'info' : 'warning'" size="small">
                  {{ row.done ? '已完成' : '进行中' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card class="page-card">
          <template #header><b>应急处置时效（按危险等级）</b></template>
          <el-table :data="emergency" size="large">
            <el-table-column prop="dangerLevel" label="危险等级" width="100">
              <template #default="{ row }">
                <el-tag :type="dangerType(row.dangerLevel)" size="small">{{ row.dangerLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="total" label="处置数" width="90" />
            <el-table-column label="平均到场" width="110">
              <template #default="{ row }">{{ row.avgMinutes }} 分钟</template>
            </el-table-column>
            <el-table-column label="时限" width="100">
              <template #default="{ row }">{{ row.limitMinutes }} 分钟</template>
            </el-table-column>
            <el-table-column label="超时次数">
              <template #default="{ row }">
                <el-tag v-if="row.overLimit > 0" type="danger" size="small">{{ row.overLimit }}</el-tag>
                <span v-else class="text-muted">0</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="page-card">
          <template #header><b>赔付责任分布</b></template>
          <div ref="respChart" style="height: 260px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 夜间围护失效 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="10">
        <el-card class="page-card">
          <template #header>
            <div class="flex-between">
              <b>夜间围护失效原因</b>
              <span class="text-muted">共 {{ nightFailures.total || 0 }} 起 · 待处置 {{ nightFailures.unhandled || 0 }} 起</span>
            </div>
          </template>
          <el-table :data="nightFailures.byCause" size="large">
            <el-table-column prop="cause" label="失效原因" min-width="110" />
            <el-table-column prop="count" label="次数" width="70" />
            <el-table-column label="敏感场所" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.sensitive > 0" type="danger" size="small">{{ row.sensitive }}</el-tag>
                <span v-else class="text-muted">0</span>
              </template>
            </el-table-column>
            <el-table-column label="平均响应" width="100">
              <template #default="{ row }">{{ row.avgResponseMinutes }} 分</template>
            </el-table-column>
            <el-table-column label="已处置">
              <template #default="{ row }">{{ row.handled }}/{{ row.count }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card class="page-card">
          <template #header><b>二次事故事件清单</b></template>
          <el-table :data="nightFailures.list" size="large" max-height="300"
                    @row-click="(r) => $router.push(`/incidents/${r.incidentId}`)" style="cursor: pointer">
            <el-table-column prop="incidentCode" label="事件编号" width="150" />
            <el-table-column prop="address" label="地址" min-width="140" show-overflow-tooltip />
            <el-table-column label="原因" width="100">
              <template #default="{ row }">
                <el-tag type="warning" size="small">{{ row.cause }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="场所/照明" width="110">
              <template #default="{ row }">
                <div>{{ row.sensitiveLocation }}</div>
                <div class="text-muted">{{ row.lighting || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="处置" width="90">
              <template #default="{ row }">
                <el-tag :type="NIGHT_FAILURE_STATUS[row.status]?.type" size="small">
                  {{ NIGHT_FAILURE_STATUS[row.status]?.label }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 管理建议 -->
    <el-card class="page-card">
      <template #header><b>治理建议（依据统计数据自动生成）</b></template>
      <el-alert v-for="(s, i) in suggestions" :key="i" :title="s" type="warning"
                :closable="false" style="margin-bottom: 8px" show-icon />
      <div class="text-muted mt16">
        建议落实入口：「巡检路线」调整高发路段巡检频次；「移交规则」完善施工移交与费用回补规则。
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  apiStatsOverview, apiStatsHotspots, apiStatsDisputes,
  apiStatsTimeouts, apiStatsCompensation, apiStatsEmergency, apiStatsNightFailures
} from '../api'
import { NIGHT_FAILURE_STATUS, dangerType, money } from '../utils/format'

const overview = ref({})
const hotspots = ref([])
const disputes = ref({})
const timeouts = ref({})
const compensation = ref({ byMonth: [], byResponsibility: [] })
const emergency = ref([])
const nightFailures = ref({ byCause: [], list: [] })

const hotspotChart = ref(null)
const compChart = ref(null)
const respChart = ref(null)

const overviewCards = computed(() => [
  { label: '事件总数', value: overview.value.totalIncidents ?? '-', color: '#1f3a5f' },
  { label: '待应急处置', value: overview.value.pendingEmergency ?? '-', color: '#f56c6c' },
  { label: '责任待定', value: overview.value.pendingOwner ?? '-', color: '#f56c6c' },
  { label: '维修超时工单', value: overview.value.timeoutOrders ?? '-', color: '#e6a23c' },
  { label: '夜间围护失效', value: overview.value.nightFailures ?? '-', color: '#f56c6c' },
  { label: '待处置/二次伤', value: overview.value.nightFailures != null
      ? `${overview.value.nightFailureUnhandled ?? 0} / ${overview.value.nightSecondInjuries ?? 0}` : '-',
    color: '#f56c6c' },
  { label: '赔付总额', value: overview.value.totalCompensation != null ? money(overview.value.totalCompensation) : '-', color: '#b88230' },
  { label: '街道垫付应急费', value: overview.value.totalEmergencyCost != null ? money(overview.value.totalEmergencyCost) : '-', color: '#7b61ff' }
])

const suggestions = computed(() => {
  const s = []
  const hot = hotspots.value.filter(h => h.count >= 2)
  if (hot.length) {
    s.push(`高发路段：${hot.map(h => `${h.address}（${h.count} 起）`).join('、')}，建议纳入每日巡检路线并加密巡查频次。`)
  }
  const disputed = (disputes.value.byOwner || []).filter(o => o.disputed > 0)
  if (disputed.length) {
    s.push(`权属争议单位：${disputed.map(o => o.orgName).join('、')} 曾出现责任待定，建议在施工移交规则中明确其设施台账移交时限与核验责任。`)
  }
  if ((disputes.value.stillPending || 0) > 0) {
    s.push(`当前仍有 ${disputes.value.stillPending} 起事件责任待定，街道先行处置费用待复核分摊，请尽快组织权属复核。`)
  }
  if ((timeouts.value.list || []).length) {
    s.push(`存在 ${timeouts.value.list.length} 个维修超时工单（阈值 ${timeouts.value.timeoutHours} 小时），建议纳入权属单位履约考核并约谈维修队。`)
  }
  const over = emergency.value.filter(e => e.overLimit > 0)
  if (over.length) {
    s.push(`应急到场超时：${over.map(e => `${e.dangerLevel}危 ${e.overLimit} 次`).join('、')}，建议优化街道值班调度与夜间备勤。`)
  }
  const nightUnhandled = nightFailures.value.unhandled || 0
  if (nightUnhandled > 0) {
    s.push(`当前有 ${nightUnhandled} 起夜间围护失效尚未加固处置，存在再次伤人风险，请立即加派反光围挡/封道或专人值守。`)
  }
  const nightCauses = nightFailures.value.byCause || []
  const inadequate = nightCauses.find(c => c.cause === '围护方式不足')
  const gap = nightCauses.find(c => c.cause === '巡查间隔过长')
  const external = nightCauses.find(c => c.cause === '外力破坏')
  if (inadequate?.count) s.push(`夜间围护方式不足 ${inadequate.count} 起：夜间高危点警示锥应升级为反光围挡并增设警示灯，学校门口/公交站配套绕行提示。`)
  if (gap?.count) s.push(`巡查间隔过长 ${gap.count} 起：应缩短夜间巡查间隔，学校门口/公交站等人流密集点高峰前后加密巡查或安排专人值守。`)
  if (external?.count) s.push(`外力破坏 ${external.count} 起：应登记周边监控编号、联动交警追查肇事车辆，必要时临时封道。`)
  if (!s.length) s.push('各项指标正常，保持现有巡检与移交规则。')
  return s
})

function renderCharts() {
  if (hotspotChart.value) {
    echarts.init(hotspotChart.value).setOption({
      tooltip: {},
      grid: { left: 10, right: 20, top: 30, bottom: 10, containLabel: true },
      xAxis: { type: 'value', minInterval: 1 },
      yAxis: { type: 'category', data: hotspots.value.map(h => h.address).reverse() },
      series: [
        { name: '事件数', type: 'bar', data: hotspots.value.map(h => h.count).reverse(), itemStyle: { color: '#2b5876' } },
        { name: '高危数', type: 'bar', data: hotspots.value.map(h => h.highDanger).reverse(), itemStyle: { color: '#f56c6c' } }
      ],
      legend: { top: 0 }
    })
  }
  if (compChart.value) {
    const months = compensation.value.byMonth.map(m => m.month)
    echarts.init(compChart.value).setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 10, right: 20, top: 30, bottom: 10, containLabel: true },
      xAxis: { type: 'category', data: months },
      yAxis: { type: 'value', name: '元' },
      series: [
        { name: '赔付金额', type: 'bar', data: compensation.value.byMonth.map(m => m.amount), itemStyle: { color: '#b88230' } },
        { name: '案件数', type: 'line', yAxisIndex: 0, data: compensation.value.byMonth.map(m => m.count), itemStyle: { color: '#2b5876' } }
      ],
      legend: { top: 0 }
    })
  }
  if (respChart.value) {
    echarts.init(respChart.value).setOption({
      tooltip: { trigger: 'item', formatter: '{b}：¥{c}（{d}%）' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        data: compensation.value.byResponsibility.map(r => ({ name: r.responsibility, value: r.amount }))
      }]
    })
  }
}

onMounted(async () => {
  ;[overview.value, hotspots.value, disputes.value, timeouts.value, compensation.value, emergency.value, nightFailures.value] =
    await Promise.all([
      apiStatsOverview(), apiStatsHotspots(), apiStatsDisputes(),
      apiStatsTimeouts(), apiStatsCompensation(), apiStatsEmergency(), apiStatsNightFailures()
    ])
  await nextTick()
  renderCharts()
})
</script>
