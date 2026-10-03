<template>
  <div class="page" v-if="d">
    <!-- 头部状态 -->
    <el-card class="page-card">
      <div class="flex-between">
        <div>
          <span style="font-size: 20px; font-weight: 700">{{ d.incident.code }}</span>
          <el-tag style="margin-left: 10px" :type="INCIDENT_STATUS[d.incident.status]?.type">
            {{ INCIDENT_STATUS[d.incident.status]?.label }}
          </el-tag>
          <el-tag v-if="d.incident.responsibilityPending" type="danger" effect="dark" style="margin-left: 6px">
            责任待定
          </el-tag>
        </div>
        <div>
          <span class="text-muted">应急线：</span>
          <el-tag :type="EMERGENCY_STATUS[d.incident.emergencyStatus]?.type" effect="plain">
            {{ EMERGENCY_STATUS[d.incident.emergencyStatus]?.label }}
          </el-tag>
          <span class="text-muted" style="margin-left: 12px">权属线：</span>
          <el-tag :type="OWNER_STATUS[d.incident.ownerStatus]?.type" effect="plain">
            {{ OWNER_STATUS[d.incident.ownerStatus]?.label }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="15">
        <!-- 上报信息 -->
        <el-card class="page-card">
          <template #header><b>上报信息</b></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="问题类型">{{ d.incident.issueType }}</el-descriptions-item>
            <el-descriptions-item label="危险程度">
              <el-tag :type="dangerType(d.incident.dangerLevel)" size="small">{{ d.incident.dangerLevel }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ d.incident.address }}</el-descriptions-item>
            <el-descriptions-item label="道路类型">{{ d.incident.roadType }}</el-descriptions-item>
            <el-descriptions-item label="经纬度">
              {{ d.incident.longitude }}, {{ d.incident.latitude }}
            </el-descriptions-item>
            <el-descriptions-item label="上报人">{{ d.reporterName }}</el-descriptions-item>
            <el-descriptions-item label="上报时间">{{ fmtTime(d.incident.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="人车损伤" :span="2">
              <el-tag v-if="d.incident.hasDamage" type="danger" size="small">有损伤</el-tag>
              <span v-else>无</span>
              <span v-if="d.incident.damageDesc" style="margin-left: 8px">{{ d.incident.damageDesc }}</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="d.ownerOrg" label="权属单位" :span="2">
              <el-tag type="success">{{ d.ownerOrg }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div v-if="d.photoList?.length" class="mt16">
            <el-image v-for="(p, i) in d.photoList" :key="i" :src="p" :preview-src-list="d.photoList"
                      fit="cover" style="width: 110px; height: 110px; margin-right: 8px; border-radius: 4px" />
          </div>
        </el-card>

        <!-- 应急处置记录 -->
        <el-card v-if="d.emergency" class="page-card">
          <template #header><b>应急处置记录（应急线）</b></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="处置人">{{ d.emergencyOfficer }}</el-descriptions-item>
            <el-descriptions-item label="围护措施">
              <el-tag v-for="m in (d.emergency.measures || '').split(',')" :key="m" size="small"
                      style="margin-right: 4px">{{ m }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="派单时间">{{ fmtTime(d.emergency.dispatchedAt) }}</el-descriptions-item>
            <el-descriptions-item label="到场时间">{{ fmtTime(d.emergency.arrivedAt) }}</el-descriptions-item>
            <el-descriptions-item label="围护完成">{{ fmtTime(d.emergency.securedAt) }}</el-descriptions-item>
            <el-descriptions-item label="应急费用（街道垫付）">{{ money(d.emergency.cost) }}</el-descriptions-item>
            <el-descriptions-item v-if="d.emergency.note" label="备注" :span="2">{{ d.emergency.note }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 夜间巡查记录 -->
        <el-card v-if="d.nightPatrols?.length" class="page-card">
          <template #header><b>夜间围护巡查记录</b></template>
          <el-timeline>
            <el-timeline-item v-for="(p, i) in d.nightPatrols" :key="i"
                              :timestamp="fmtTime(p.patrol.arrivedAt)"
                              :type="p.patrol.intact ? 'success' : 'danger'">
              <b>{{ p.patrol.roundName || `第 ${i + 1} 轮巡查` }}</b>
              <el-tag :type="p.patrol.intact ? 'success' : 'danger'" size="small" style="margin-left: 8px">
                {{ p.patrol.intact ? '围护完好' : '发现围护异常' }}
              </el-tag>
              <span style="margin-left: 8px" class="text-muted">巡查人：{{ p.officerName }}</span>
              <div v-if="p.patrol.siteStatus" class="text-muted" style="margin-top: 4px">
                现场状态：{{ p.patrol.siteStatus }}
              </div>
              <div v-if="p.patrol.coneMovedAt" style="margin-top: 4px; color: #f56c6c">
                警示锥移位时间：{{ fmtTime(p.patrol.coneMovedAt) }}
              </div>
              <el-image v-for="(ph, j) in p.photoList" :key="j" :src="ph"
                        :preview-src-list="p.photoList" fit="cover"
                        style="width: 90px; height: 90px; margin: 6px 6px 0 0; border-radius: 4px" />
              <div v-if="p.patrol.note" class="text-muted" style="margin-top: 4px">备注：{{ p.patrol.note }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 夜间围护失效处理 -->
        <el-card v-for="(f, idx) in d.nightFailures" :key="idx" class="page-card"
                 style="border-color: #f56c6c">
          <template #header>
            <div class="flex-between">
              <b>夜间围护失效处理 {{ f.failure.code }}</b>
              <div>
                <el-tag v-if="f.failure.hasSecondAccident" type="danger" effect="dark" size="small">
                  发生二次事故
                </el-tag>
                <el-tag :type="f.failure.reinforced ? 'success' : 'warning'" size="small" style="margin-left: 6px">
                  {{ f.failure.reinforced ? '加固已落实' : '待加固落实' }}
                </el-tag>
              </div>
            </div>
          </template>

          <!-- 串联时间链 -->
          <el-steps :active="f.failure.hasSecondAccident ? 5 : 4" align-center size="small" finish-status="success"
                    style="margin: 4px 0 14px">
            <el-step title="首次上报" :description="fmtTime(d.incident.createdAt)" />
            <el-step :title="`围护（${d.emergency?.measures || '-'}）`"
                     :description="fmtTime(d.emergency?.securedAt)" />
            <el-step title="巡查到场" :description="fmtTime(f.failure.lastPatrolAt)" />
            <el-step title="警示锥移位" :description="fmtTime(f.failure.coneMovedAt)" />
            <el-step v-if="f.failure.hasSecondAccident" title="二次事故"
                     :description="fmtTime(f.failure.secondAccidentAt)" />
          </el-steps>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="发现来源">{{ f.failure.reportedBy || '夜间巡查发现' }}</el-descriptions-item>
            <el-descriptions-item label="登记人">{{ f.officerName }}</el-descriptions-item>
            <el-descriptions-item v-if="f.failure.patrolGapMinutes != null" label="距上次巡查">
              <el-tag :type="f.failure.patrolGapMinutes > (meta.nightPatrolLimitMinutes || 180) ? 'danger' : 'info'"
                      size="small">
                {{ f.failure.patrolGapMinutes }} 分钟
                （时限 {{ meta.nightPatrolLimitMinutes || 180 }} 分钟）
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="失效原因">
              <el-tag type="danger" size="small">{{ causeLabel(f.failure.cause) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item v-if="f.failure.causeAnalysis" label="判定依据" :span="2">
              {{ f.failure.causeAnalysis }}
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="f.photoList?.length" class="mt16">
            <div class="text-muted" style="margin-bottom: 4px">失效现场照片：</div>
            <el-image v-for="(ph, j) in f.photoList" :key="j" :src="ph"
                      :preview-src-list="f.photoList" fit="cover"
                      style="width: 100px; height: 100px; margin-right: 8px; border-radius: 4px" />
          </div>

          <!-- 二次事故 -->
          <div v-if="f.failure.hasSecondAccident" class="mt16">
            <el-divider content-position="left">二次事故记录</el-divider>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="事故时间">{{ fmtTime(f.failure.secondAccidentAt) }}</el-descriptions-item>
              <el-descriptions-item label="受伤骑车人">
                {{ f.failure.injuredName || '未留名' }}
                <span v-if="f.failure.injuredPhone" class="text-muted">（{{ f.failure.injuredPhone }}）</span>
              </el-descriptions-item>
              <el-descriptions-item label="伤情" :span="2">{{ f.failure.injuryDesc || '-' }}</el-descriptions-item>
              <el-descriptions-item v-if="f.failure.accidentDesc" label="事故经过" :span="2">
                {{ f.failure.accidentDesc }}
              </el-descriptions-item>
              <el-descriptions-item label="赔付案件" :span="2">
                <el-tag type="warning" size="small">已自动立案（二次事故）</el-tag>
              </el-descriptions-item>
            </el-descriptions>
            <div v-if="f.accidentPhotoList?.length" class="mt16">
              <el-image v-for="(ph, j) in f.accidentPhotoList" :key="j" :src="ph"
                        :preview-src-list="f.accidentPhotoList" fit="cover"
                        style="width: 100px; height: 100px; margin-right: 8px; border-radius: 4px" />
            </div>
          </div>

          <!-- 处置决策 -->
          <div class="mt16">
            <el-divider content-position="left">加固处置决策</el-divider>
            <div v-if="f.failure.reinforceMeasures">
              <el-tag v-for="m in f.failure.reinforceMeasures.split(',')" :key="m"
                      type="warning" effect="dark" size="small" style="margin-right: 6px">
                {{ reinforceLabel(m) }}
              </el-tag>
              <span class="text-muted" style="margin-left: 8px">
                {{ f.decisionOfficerName || '值班员' }} · {{ fmtTime(f.failure.decisionAt) }}
              </span>
              <div v-if="f.failure.reinforced" style="margin-top: 6px; color: #67c23a">
                已落实于 {{ fmtTime(f.failure.reinforcedAt) }}
              </div>
              <div v-if="f.failure.decisionNote" class="text-muted" style="margin-top: 4px">
                处置说明：{{ f.failure.decisionNote }}
              </div>
            </div>
            <el-alert v-else type="error" :closable="false" title="尚未作出加固处置决策，请值班员立即研判" />
          </div>

          <!-- 学校门口/公交站敏感信息 -->
          <div v-if="f.failure.sensitiveLocation" class="mt16">
            <el-divider content-position="left">
              敏感地点（{{ f.failure.locationType }}）
            </el-divider>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="场所名称" :span="2">{{ f.failure.locationName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="人流高峰">{{ f.failure.peakHours || '-' }}</el-descriptions-item>
              <el-descriptions-item label="照明条件">
                <el-tag :type="{ 良好: 'success', 一般: 'warning', 昏暗无路灯: 'danger' }[f.failure.lighting] || 'info'"
                        size="small">{{ f.failure.lighting || '-' }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="周边监控编号" :span="2">
                <el-tag v-for="c in (f.failure.cameraIds || '').split(',').filter(Boolean)" :key="c"
                        size="small" type="info" effect="plain" style="margin-right: 4px">{{ c }}</el-tag>
                <span v-if="!f.failure.cameraIds">-</span>
              </el-descriptions-item>
              <el-descriptions-item label="绕行提示">
                <el-tag :type="f.failure.detourNotice ? 'warning' : 'info'" size="small">
                  {{ f.failure.detourNotice ? '已安排绕行提示' : '未安排' }}
                </el-tag>
                <div v-if="f.failure.detourNote" class="text-muted" style="margin-top: 2px">
                  {{ f.failure.detourNote }}
                </div>
              </el-descriptions-item>
              <el-descriptions-item label="交警协助">
                <el-tag :type="f.failure.trafficPoliceAssist ? 'danger' : 'info'" size="small">
                  {{ f.failure.trafficPoliceAssist ? '已请求交警协助' : '未请求' }}
                </el-tag>
                <div v-if="f.failure.trafficPoliceNote" class="text-muted" style="margin-top: 2px">
                  {{ f.failure.trafficPoliceNote }}
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 卡片内操作 -->
          <div v-if="['STREET_DUTY', 'ADMIN'].includes(role) && d.incident.status !== 'CLOSED'"
               style="margin-top: 14px; text-align: right">
            <el-button v-if="!f.failure.reinforceMeasures" type="danger" size="small"
                       @click="openDecision(f.failure)">补充原因判定与处置决策</el-button>
            <el-button v-if="f.failure.reinforceMeasures && !f.failure.reinforced" type="success" size="small"
                       @click="doReinforce(f.failure)">确认加固已落实</el-button>
          </div>
        </el-card>

        <!-- 权属排查记录 -->
        <el-card v-if="d.investigations?.length" class="page-card">
          <template #header><b>权属排查记录（权属线）</b></template>
          <div v-for="(item, i) in d.investigations" :key="i" style="margin-bottom: 12px">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="排查人">
                {{ item.investigatorName }} · {{ fmtTime(item.investigation.createdAt) }}
                <el-tag :type="item.investigation.conclusion === 'CONFIRMED' ? 'success' : 'danger'"
                        size="small" style="margin-left: 8px">
                  {{ item.investigation.conclusion === 'CONFIRMED' ? '确认权属' : '无法确认（责任待定）' }}
                </el-tag>
                <el-tag v-if="item.confirmedOwnerOrg" type="success" size="small" style="margin-left: 4px">
                  {{ item.confirmedOwnerOrg }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="井盖标识">{{ item.investigation.manholeMarking || '-' }}</el-descriptions-item>
              <el-descriptions-item label="地下管线">{{ item.investigation.pipelineInfo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="历史施工">{{ item.investigation.constructionHistory || '-' }}</el-descriptions-item>
              <el-descriptions-item label="周边商户">{{ item.investigation.merchantInfo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="城管台账">{{ item.investigation.cityLedger || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>

        <!-- 认领记录 -->
        <el-card v-if="d.claims?.length" class="page-card">
          <template #header><b>权属认领记录</b></template>
          <el-timeline>
            <el-timeline-item v-for="(c, i) in d.claims" :key="i" :timestamp="fmtTime(c.claim.createdAt)" type="success">
              <b>{{ c.ownerOrg }}</b> {{ c.claim.claimType }}
              <span v-if="c.claim.note" class="text-muted">（{{ c.claim.note }}）</span>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 维修工单 -->
        <el-card v-if="d.repairOrder" class="page-card">
          <template #header><b>维修工单</b></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="维修队">{{ d.repairTeamName }}</el-descriptions-item>
            <el-descriptions-item label="工单状态">
              <el-tag :type="REPAIR_STATUS[d.repairOrder.status]?.type" size="small">
                {{ REPAIR_STATUS[d.repairOrder.status]?.label }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="派单时间">{{ fmtTime(d.repairOrder.assignedAt) }}</el-descriptions-item>
            <el-descriptions-item label="到场时间">{{ fmtTime(d.repairOrder.arrivedAt) }}</el-descriptions-item>
            <el-descriptions-item label="井室状况">{{ d.repairOrder.chamberCondition || '-' }}</el-descriptions-item>
            <el-descriptions-item label="井盖规格">{{ d.repairOrder.coverSpec || '-' }}</el-descriptions-item>
            <el-descriptions-item label="需要封路">{{ d.repairOrder.needRoadClosure ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="专业协同">
              {{ d.repairOrder.needCoord ? `涉及：${d.repairOrder.coordUnits}` : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="维修费用">{{ money(d.repairOrder.repairCost) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ fmtTime(d.repairOrder.finishedAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="d.repairOrder.repairNote" label="维修说明" :span="2">
              {{ d.repairOrder.repairNote }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 验收记录 -->
        <el-card v-if="d.acceptances?.length" class="page-card">
          <template #header><b>验收记录</b></template>
          <div v-for="(a, i) in d.acceptances" :key="i" style="margin-bottom: 10px">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="巡查员">{{ a.inspectorName }}</el-descriptions-item>
              <el-descriptions-item label="结论">
                <el-tag :type="a.acceptance.result === 'PASSED' ? 'success' : 'danger'" size="small">
                  {{ a.acceptance.result === 'PASSED' ? '验收通过' : '验收不通过' }}
                </el-tag>
                <span class="text-muted" style="margin-left: 8px">{{ fmtTime(a.acceptance.inspectedAt) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="验收项" :span="2">
                <el-tag :type="a.acceptance.photoOk ? 'success' : 'danger'" size="small" effect="plain">照片比对</el-tag>
                <el-tag :type="a.acceptance.locationOk ? 'success' : 'danger'" size="small" effect="plain" style="margin-left: 4px">定位一致</el-tag>
                <el-tag :type="a.acceptance.flatnessOk ? 'success' : 'danger'" size="small" effect="plain" style="margin-left: 4px">路面平整度</el-tag>
                <el-tag :type="a.acceptance.rollingTestOk ? 'success' : 'danger'" size="small" effect="plain" style="margin-left: 4px">车辆碾压测试</el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="a.acceptance.note" label="备注" :span="2">{{ a.acceptance.note }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>

        <!-- 赔付案件 -->
        <el-card v-for="(cm, ci) in d.compensations" :key="ci" class="page-card">
          <template #header>
            <div class="flex-between">
              <b>赔付案件{{ cm.case.caseType === 'SECOND' ? '（夜间围护失效二次事故）' : '' }}</b>
              <el-tag :type="COMP_STATUS[cm.case.status]?.type" size="small">
                {{ COMP_STATUS[cm.case.status]?.label }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="索赔人">{{ cm.case.claimantName }}</el-descriptions-item>
            <el-descriptions-item label="损失类型">{{ cm.case.damageType }}</el-descriptions-item>
            <el-descriptions-item label="索赔金额">{{ money(cm.case.claimAmount) }}</el-descriptions-item>
            <el-descriptions-item label="核定金额">{{ money(cm.case.approvedAmount) }}</el-descriptions-item>
            <el-descriptions-item label="事故证明" :span="2">{{ cm.case.accidentProof || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="cm.case.analysis" label="责任分析" :span="2">
              {{ cm.case.analysis }}
            </el-descriptions-item>
            <el-descriptions-item v-if="cm.case.responsibility" label="责任认定" :span="2">
              {{ RESPONSIBILITY[cm.case.responsibility] }}
              <span v-if="cm.case.responsibility === 'SHARED'" class="text-muted">
                （权属 {{ cm.case.ownerRatio }}% / 街道 {{ cm.case.streetRatio }}%）
              </span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 费用分摊 -->
        <el-card v-if="d.costAllocation" class="page-card">
          <template #header>
            <div class="flex-between">
              <b>费用分摊</b>
              <el-tag :type="COST_STATUS[d.costAllocation.status]?.type" size="small">
                {{ COST_STATUS[d.costAllocation.status]?.label }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="4" border>
            <el-descriptions-item label="应急费">{{ money(d.costAllocation.emergencyCost) }}</el-descriptions-item>
            <el-descriptions-item label="维修费">{{ money(d.costAllocation.repairCost) }}</el-descriptions-item>
            <el-descriptions-item label="赔付金">{{ money(d.costAllocation.compensationAmount) }}</el-descriptions-item>
            <el-descriptions-item label="合计"><b>{{ money(d.costAllocation.totalAmount) }}</b></el-descriptions-item>
          </el-descriptions>
          <el-table :data="allocList" size="small" class="mt16">
            <el-table-column prop="party" label="分摊方" min-width="180" />
            <el-table-column label="金额" width="130">
              <template #default="{ row }">{{ money(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="依据" min-width="220" />
          </el-table>
          <div v-if="d.costAllocation.reviewNote" class="text-muted mt16">
            复核意见：{{ d.costAllocation.reviewNote }}
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：操作面板 + 时间线 -->
      <el-col :xs="24" :md="9">
        <el-card v-if="actions.length" class="page-card">
          <template #header><b>可执行操作</b></template>

          <!-- 街道：应急派单 -->
          <div v-if="actions.includes('dispatch')" class="mb16">
            <el-alert type="error" :closable="false" title="事件待应急处置，请立即派单到场围护" class="mb16" />
            <el-button type="danger" style="width: 100%" :loading="acting" @click="doDispatch">
              立即派单（街道值班员）
            </el-button>
          </div>

          <!-- 街道：完成围护 -->
          <div v-if="actions.includes('secure')">
            <el-divider content-position="left">完成临时围护</el-divider>
            <el-form label-width="90px">
              <el-form-item label="围护措施">
                <el-checkbox-group v-model="secureForm.measures">
                  <el-checkbox v-for="m in meta.measureTypes || []" :key="m" :value="m">{{ m }}</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="应急费用">
                <el-input-number v-model="secureForm.cost" :min="0" :precision="2" />
                <span class="text-muted" style="margin-left: 6px">元（街道先行垫付）</span>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="secureForm.note" type="textarea" :rows="2" />
              </el-form-item>
              <el-button type="warning" style="width: 100%" :loading="acting" @click="doSecure">
                确认到场并完成围护
              </el-button>
            </el-form>
          </div>

          <!-- 街道/巡查：夜间巡查到场 -->
          <div v-if="actions.includes('nightPatrol')">
            <el-divider content-position="left">夜间巡查到场</el-divider>
            <el-form label-width="90px">
              <el-form-item label="巡查班次">
                <el-input v-model="patrolForm.roundName" placeholder="如：22:00 第一轮" />
              </el-form-item>
              <el-form-item label="到场时间">
                <el-date-picker v-model="patrolForm.arrivedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                                placeholder="默认当前时间" style="width: 100%" />
              </el-form-item>
              <el-form-item label="围护状态">
                <el-radio-group v-model="patrolForm.intact">
                  <el-radio :value="true">围护完好</el-radio>
                  <el-radio :value="false">已失效/移位</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="现场状态">
                <el-input v-model="patrolForm.siteStatus" type="textarea" :rows="2"
                          placeholder="警示锥数量、反光标识、照明可见度" />
              </el-form-item>
              <el-form-item label="巡查照片">
                <el-upload list-type="picture-card" :file-list="patrolFileList" :http-request="(opt) => doUpload(opt, patrolPhotos, patrolFileList)"
                           :on-remove="(file) => onRemoveUpload(file, patrolPhotos, patrolFileList)" accept="image/*">
                  <el-icon><Plus /></el-icon>
                </el-upload>
              </el-form-item>
              <el-form-item v-if="!patrolForm.intact" label="锥移位时间">
                <el-date-picker v-model="patrolForm.coneMovedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                                placeholder="发现警示锥移位/被撞时间" style="width: 100%" />
              </el-form-item>
              <el-button type="primary" plain style="width: 100%" :loading="acting" @click="doPatrol">
                提交巡查记录
              </el-button>
            </el-form>
          </div>

          <!-- 街道：登记夜间围护失效 + 二次事故 -->
          <div v-if="actions.includes('nightFailure')">
            <el-divider content-position="left">夜间围护失效处理</el-divider>
            <el-alert type="error" :closable="false" class="mb16"
                      title="警示锥被撞移位/围护夜间失效时登记，系统将串联首次上报→围护→巡查→移位→二次事故" />
            <el-form label-width="100px">
              <el-form-item label="移位时间" required>
                <el-date-picker v-model="failForm.coneMovedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                                placeholder="警示锥被撞倒/移位时间" style="width: 100%" />
              </el-form-item>
              <el-form-item label="发现来源">
                <el-input v-model="failForm.reportedBy" placeholder="巡查发现/市民二次上报/交警转递" />
              </el-form-item>
              <el-form-item label="最近巡查到场">
                <el-date-picker v-model="failForm.lastPatrolAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                                placeholder="留空自动取最近一次巡查" style="width: 100%" />
              </el-form-item>
              <el-form-item label="失效现场照">
                <el-upload list-type="picture-card" :file-list="failFileList" :http-request="(opt) => doUpload(opt, failPhotos, failFileList)"
                           :on-remove="(file) => onRemoveUpload(file, failPhotos, failFileList)" accept="image/*">
                  <el-icon><Plus /></el-icon>
                </el-upload>
              </el-form-item>

              <el-form-item label="二次事故">
                <el-switch v-model="failForm.hasSecondAccident" active-text="发生二次事故（有人车损伤）" />
              </el-form-item>
              <template v-if="failForm.hasSecondAccident">
                <el-form-item label="事故时间" required>
                  <el-date-picker v-model="failForm.secondAccidentAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                                  placeholder="二次事故发生时间" style="width: 100%" />
                </el-form-item>
                <el-form-item label="受伤骑车人">
                  <el-input v-model="failForm.injuredName" placeholder="姓名（可选）" style="width: 48%; margin-right: 4%" />
                  <el-input v-model="failForm.injuredPhone" placeholder="联系电话" style="width: 48%" />
                </el-form-item>
                <el-form-item label="伤情描述">
                  <el-input v-model="failForm.injuryDesc" type="textarea" :rows="2"
                            placeholder="如：骑行摔倒致左臂擦伤、膝盖挫伤，已送医" />
                </el-form-item>
                <el-form-item label="事故经过">
                  <el-input v-model="failForm.accidentDesc" type="textarea" :rows="2"
                            placeholder="车辆、经过、现场处置（自动转交赔付专员）" />
                </el-form-item>
                <el-form-item label="事故现场照">
                  <el-upload list-type="picture-card" :file-list="accFileList" :http-request="(opt) => doUpload(opt, accPhotos, accFileList)"
                             :on-remove="(file) => onRemoveUpload(file, accPhotos, accFileList)" accept="image/*">
                    <el-icon><Plus /></el-icon>
                  </el-upload>
                </el-form-item>
              </template>

              <el-form-item label="失效原因">
                <el-select v-model="failForm.cause" placeholder="留空由系统按时间链建议" clearable style="width: 100%">
                  <el-option v-for="c in meta.failureCauses || []" :key="c.value" :value="c.value" :label="c.label" />
                </el-select>
              </el-form-item>
              <el-form-item label="判定说明">
                <el-input v-model="failForm.causeAnalysis" type="textarea" :rows="2"
                          placeholder="留空自动生成：围护措施、巡查间隔、外力痕迹" />
              </el-form-item>

              <el-form-item label="加固措施">
                <el-checkbox-group v-model="failForm.reinforceMeasures">
                  <el-checkbox v-for="m in meta.reinforceMeasures || []" :key="m.value" :value="m.value">
                    {{ m.label }}
                  </el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="处置说明">
                <el-input v-model="failForm.decisionNote" type="textarea" :rows="2"
                          placeholder="围挡数量/封道范围/值守时段" />
              </el-form-item>

              <el-form-item label="敏感地点">
                <el-switch v-model="failForm.sensitiveLocation"
                           active-text="学校门口或公交站附近" />
              </el-form-item>
              <template v-if="failForm.sensitiveLocation">
                <el-form-item label="地点类型">
                  <el-radio-group v-model="failForm.locationType">
                    <el-radio v-for="t in meta.sensitiveLocationTypes || ['学校门口', '公交站']" :key="t" :value="t">
                      {{ t }}
                    </el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="场所名称">
                  <el-input v-model="failForm.locationName" placeholder="如：第二实验小学（东门）/ 人民影院公交站" />
                </el-form-item>
                <el-form-item label="人流高峰">
                  <el-input v-model="failForm.peakHours" placeholder="如：07:00-08:00、16:30-18:00" />
                </el-form-item>
                <el-form-item label="照明条件">
                  <el-radio-group v-model="failForm.lighting">
                    <el-radio v-for="l in meta.lightingOptions || ['良好', '一般', '昏暗无路灯']" :key="l" :value="l">
                      {{ l }}
                    </el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="监控编号">
                  <el-input v-model="failForm.cameraIds" placeholder="周边监控编号，逗号分隔，如 JY-0112,JY-0113" />
                </el-form-item>
                <el-form-item label="绕行提示">
                  <el-switch v-model="failForm.detourNotice" />
                </el-form-item>
                <el-form-item v-if="failForm.detourNotice" label="提示安排">
                  <el-input v-model="failForm.detourNote" type="textarea" :rows="2"
                            placeholder="绕行指示牌数量、摆放位置" />
                </el-form-item>
                <el-form-item label="交警协助">
                  <el-switch v-model="failForm.trafficPoliceAssist" active-text="请求交警协助" />
                </el-form-item>
                <el-form-item v-if="failForm.trafficPoliceAssist" label="协助情况">
                  <el-input v-model="failForm.trafficPoliceNote" type="textarea" :rows="2"
                            placeholder="对接警力、到场时间（可后补）" />
                </el-form-item>
              </template>

              <el-button type="danger" style="width: 100%" :loading="acting" @click="doNightFailure">
                登记围护失效并提交处置决策
              </el-button>
            </el-form>
          </div>

          <!-- 街道：权属排查 -->
          <div v-if="actions.includes('investigate')">
            <el-divider content-position="left">权属排查（五类证据）</el-divider>
            <el-form label-width="90px">
              <el-form-item label="井盖标识">
                <el-input v-model="invForm.manholeMarking" placeholder="铸字、编号、权属标记" />
              </el-form-item>
              <el-form-item label="地下管线">
                <el-input v-model="invForm.pipelineInfo" placeholder="管线资料比对" />
              </el-form-item>
              <el-form-item label="历史施工">
                <el-input v-model="invForm.constructionHistory" placeholder="施工记录查询" />
              </el-form-item>
              <el-form-item label="周边商户">
                <el-input v-model="invForm.merchantInfo" placeholder="走访情况" />
              </el-form-item>
              <el-form-item label="城管台账">
                <el-input v-model="invForm.cityLedger" placeholder="台账登记信息" />
              </el-form-item>
              <el-form-item label="排查结论">
                <el-radio-group v-model="invForm.conclusion">
                  <el-radio value="CONFIRMED">确认权属</el-radio>
                  <el-radio value="PENDING">无法确认（责任待定）</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="invForm.conclusion === 'CONFIRMED'" label="权属单位">
                <el-select v-model="invForm.confirmedOwnerId" style="width: 100%">
                  <el-option v-for="o in meta.ownerUnits || []" :key="o.id" :value="o.id" :label="o.orgName" />
                </el-select>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="invForm.note" type="textarea" :rows="2" />
              </el-form-item>
              <el-button type="primary" style="width: 100%" :loading="acting" @click="doInvestigate">
                提交排查结论
              </el-button>
            </el-form>
          </div>

          <!-- 权属单位：认领 -->
          <div v-if="actions.includes('claim')">
            <el-alert type="warning" :closable="false" title="经核实该井盖属我单位权属的，请主动认领" class="mb16" />
            <el-input v-model="claimNote" placeholder="认领说明（可选）" class="mb16" />
            <el-button type="primary" style="width: 100%" :loading="acting" @click="doClaim">
              主动认领该井盖
            </el-button>
          </div>

          <!-- 权属单位：派单 -->
          <div v-if="actions.includes('assign')">
            <el-divider content-position="left">派发维修工单</el-divider>
            <el-select v-model="assignTeamId" placeholder="选择维修队" style="width: 100%" class="mb16">
              <el-option v-for="t in meta.repairTeams || []" :key="t.id" :value="t.id" :label="t.orgName" />
            </el-select>
            <el-button type="primary" style="width: 100%" :loading="acting" @click="doAssign">
              派发工单
            </el-button>
          </div>

          <!-- 维修队：接单 -->
          <div v-if="actions.includes('accept')">
            <el-button type="primary" style="width: 100%" :loading="acting" @click="doAccept">
              接单（维修队）
            </el-button>
          </div>

          <!-- 巡查员：验收 -->
          <div v-if="actions.includes('inspect')">
            <el-divider content-position="left">验收（四项检查）</el-divider>
            <el-form label-width="110px">
              <el-form-item label="照片比对">
                <el-switch v-model="accForm.photoOk" active-text="一致" />
              </el-form-item>
              <el-form-item label="定位一致">
                <el-switch v-model="accForm.locationOk" active-text="一致" />
              </el-form-item>
              <el-form-item label="路面平整度">
                <el-switch v-model="accForm.flatnessOk" active-text="合格" />
              </el-form-item>
              <el-form-item label="车辆碾压测试">
                <el-switch v-model="accForm.rollingTestOk" active-text="通过" />
              </el-form-item>
              <el-form-item label="验收结论">
                <el-radio-group v-model="accForm.result">
                  <el-radio value="PASSED">通过</el-radio>
                  <el-radio value="FAILED">不通过（退回重修）</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="accForm.note" type="textarea" :rows="2" />
              </el-form-item>
              <el-button type="success" style="width: 100%" :loading="acting" @click="doInspect">
                提交验收结论
              </el-button>
            </el-form>
          </div>

          <!-- 管理员：费用复核 -->
          <div v-if="actions.includes('costReview')">
            <el-divider content-position="left">费用分摊复核</el-divider>
            <div v-for="(a, i) in costForm.allocations" :key="i" style="display: flex; gap: 6px; margin-bottom: 8px">
              <el-input v-model="a.party" placeholder="分摊方" style="flex: 2" />
              <el-input-number v-model="a.amount" :min="0" :precision="2" style="flex: 1" />
              <el-button text type="danger" @click="costForm.allocations.splice(i, 1)">删</el-button>
            </div>
            <el-button size="small" @click="costForm.allocations.push({ party: '', amount: 0, reason: '' })">
              + 添加分摊方
            </el-button>
            <el-input v-model="costForm.reviewNote" placeholder="复核意见" type="textarea" :rows="2"
                      class="mt16 mb16" />
            <el-button type="primary" style="width: 100%" :loading="acting" @click="doCostReview">
              确认分摊并结案（合计须等于 {{ money(d.costAllocation?.totalAmount) }}）
            </el-button>
          </div>
        </el-card>

        <!-- 流程时间线 -->
        <el-card class="page-card">
          <template #header><b>全流程时间线</b></template>
          <el-timeline>
            <el-timeline-item v-for="(t, i) in timeline" :key="i" :timestamp="t.time"
                              :type="t.type" :hollow="!t.done">
              {{ t.label }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- 补充原因判定与处置决策 -->
    <el-dialog v-model="decisionVisible" title="失效原因判定与加固处置决策" width="560px">
      <el-form :model="decisionForm" label-width="100px">
        <el-form-item label="失效原因" required>
          <el-radio-group v-model="decisionForm.cause">
            <el-radio v-for="c in meta.failureCauses || []" :key="c.value" :value="c.value">
              {{ c.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="判定依据">
          <el-input v-model="decisionForm.causeAnalysis" type="textarea" :rows="3"
                    placeholder="围护方式、巡查间隔、外力痕迹综合分析" />
        </el-form-item>
        <el-form-item label="加固措施" required>
          <el-checkbox-group v-model="decisionForm.reinforceMeasures">
            <el-checkbox v-for="m in meta.reinforceMeasures || []" :key="m.value" :value="m.value">
              {{ m.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="处置说明">
          <el-input v-model="decisionForm.decisionNote" type="textarea" :rows="2"
                    placeholder="围挡数量/封道范围/值守时段" />
        </el-form-item>
        <el-form-item label="绕行提示">
          <el-switch v-model="decisionForm.detourNotice" />
          <el-input v-if="decisionForm.detourNotice" v-model="decisionForm.detourNote"
                    type="textarea" :rows="2" class="mt16" placeholder="绕行指示牌数量、摆放位置" />
        </el-form-item>
        <el-form-item label="交警协助">
          <el-switch v-model="decisionForm.trafficPoliceAssist" active-text="请求交警协助" />
          <el-input v-if="decisionForm.trafficPoliceAssist" v-model="decisionForm.trafficPoliceNote"
                    type="textarea" :rows="2" class="mt16" placeholder="对接警力、到场时间" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decisionVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doDecision">提交决策</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import {
  apiIncidentDetail, apiMeta, apiDispatch, apiSecure, apiInvestigate, apiClaim,
  apiCreateOrder, apiAcceptOrder, apiAcceptance, apiCostReview,
  apiNightPatrol, apiNightFailure, apiNightFailureDecision, apiNightFailureReinforce, apiUpload
} from '../api'
import {
  INCIDENT_STATUS, EMERGENCY_STATUS, OWNER_STATUS, REPAIR_STATUS, COMP_STATUS,
  COST_STATUS, RESPONSIBILITY, dangerType, fmtTime, money
} from '../utils/format'

const route = useRoute()
const auth = useAuthStore()
const id = route.params.id
const d = ref(null)
const meta = ref({})
const acting = ref(false)
const claimNote = ref('')
const assignTeamId = ref(null)

const secureForm = reactive({ measures: ['警示锥'], cost: 0, note: '' })
const invForm = reactive({
  manholeMarking: '', pipelineInfo: '', constructionHistory: '',
  merchantInfo: '', cityLedger: '', conclusion: 'CONFIRMED', confirmedOwnerId: null, note: ''
})
const accForm = reactive({
  photoOk: true, locationOk: true, flatnessOk: true, rollingTestOk: true, result: 'PASSED', note: ''
})
const costForm = reactive({ allocations: [], reviewNote: '' })

const patrolForm = reactive({
  roundName: '', arrivedAt: null, intact: true, siteStatus: '', coneMovedAt: null, note: ''
})
const patrolPhotos = ref([])
const patrolFileList = ref([])

const failForm = reactive({
  lastPatrolAt: null, coneMovedAt: null, reportedBy: '',
  hasSecondAccident: false, secondAccidentAt: null,
  injuredName: '', injuredPhone: '', injuryDesc: '', accidentDesc: '',
  cause: '', causeAnalysis: '', reinforceMeasures: [], decisionNote: '',
  sensitiveLocation: false, locationType: '学校门口', locationName: '',
  peakHours: '', lighting: '一般', cameraIds: '',
  detourNotice: false, detourNote: '', trafficPoliceAssist: false, trafficPoliceNote: ''
})
const failPhotos = ref([])
const failFileList = ref([])
const accPhotos = ref([])
const accFileList = ref([])

// 失效记录卡片「补充决策」对话框
const decisionVisible = ref(false)
const currentFailure = ref(null)
const decisionForm = reactive({
  cause: '', causeAnalysis: '', reinforceMeasures: [], decisionNote: '',
  detourNotice: false, detourNote: '', trafficPoliceAssist: false, trafficPoliceNote: ''
})

const CAUSE_LABEL = { INADEQUATE: '围护方式不足', INTERVAL_TOO_LONG: '巡查间隔过长', EXTERNAL_DAMAGE: '外力破坏' }
const REINFORCE_LABEL = {
  REFLECTIVE_BARRIER: '加派反光围挡', TEMP_CLOSURE: '临时封道', GUARD_DUTY: '安排专人值守'
}
const causeLabel = (v) => CAUSE_LABEL[v] || v || '-'
const reinforceLabel = (v) => REINFORCE_LABEL[v] || v

const role = computed(() => auth.role)
const inc = computed(() => d.value?.incident || {})

const actions = computed(() => {
  if (!d.value) return []
  const i = inc.value
  const list = []
  if (['STREET_DUTY', 'ADMIN'].includes(role.value)) {
    if (i.emergencyStatus === 'PENDING') list.push('dispatch')
    if (i.emergencyStatus !== 'SECURED') list.push('secure')
    if (i.emergencyStatus === 'SECURED' && i.status !== 'CLOSED') {
      list.push('nightPatrol')
      list.push('nightFailure')
    }
    if (['INVESTIGATING', 'PENDING'].includes(i.ownerStatus)) list.push('investigate')
  }
  if (role.value === 'INSPECTOR' && i.emergencyStatus === 'SECURED' && i.status !== 'CLOSED') {
    list.push('nightPatrol')
  }
  if (role.value === 'OWNER_UNIT') {
    if (i.ownerStatus !== 'CONFIRMED') list.push('claim')
    if (i.ownerStatus === 'CONFIRMED' && i.ownerId === auth.user.id && !d.value.repairOrder
        && i.status === 'OWNER_CONFIRMED') list.push('assign')
  }
  if (role.value === 'REPAIR_TEAM' && d.value.repairOrder
      && ['ASSIGNED', 'RETURNED'].includes(d.value.repairOrder.status)
      && d.value.repairOrder.teamId === auth.user.id) list.push('accept')
  if (role.value === 'INSPECTOR' && i.status === 'REPAIRED') list.push('inspect')
  if (role.value === 'ADMIN' && d.value.costAllocation?.status === 'PENDING') list.push('costReview')
  return list
})

const allocList = computed(() => {
  try {
    return JSON.parse(d.value?.costAllocation?.allocations || '[]')
  } catch {
    return []
  }
})

const timeline = computed(() => {
  if (!d.value) return []
  const i = inc.value
  const em = d.value.emergency
  const o = d.value.repairOrder
  const t = []
  t.push({ label: `市民上报（${i.issueType} · 危险${i.dangerLevel}）`, time: fmtTime(i.createdAt), done: true, type: 'primary' })
  t.push({ label: '街道派单应急处置', time: em ? fmtTime(em.dispatchedAt) : '待派单', done: !!em, type: em ? 'warning' : 'info' })
  t.push({ label: `到场临时围护${em?.measures ? '（' + em.measures + '）' : ''}`, time: em?.securedAt ? fmtTime(em.securedAt) : '待围护', done: !!em?.securedAt, type: em?.securedAt ? 'warning' : 'info' })
  for (const p of d.value.nightPatrols || []) {
    t.push({
      label: `夜间巡查${p.patrol.roundName ? '·' + p.patrol.roundName : ''}：${p.patrol.intact ? '围护完好' : '发现异常'}`,
      time: fmtTime(p.patrol.arrivedAt), done: true, type: p.patrol.intact ? 'success' : 'danger'
    })
  }
  for (const f of d.value.nightFailures || []) {
    t.push({ label: `警示锥移位（${causeLabel(f.failure.cause)}）`, time: fmtTime(f.failure.coneMovedAt), done: true, type: 'danger' })
    if (f.failure.hasSecondAccident) {
      t.push({ label: '二次事故：骑车人摔伤', time: fmtTime(f.failure.secondAccidentAt), done: true, type: 'danger' })
    }
    t.push({
      label: f.failure.reinforceMeasures
        ? `加固处置：${f.failure.reinforceMeasures.split(',').map(reinforceLabel).join('、')}${f.failure.reinforced ? '（已落实）' : ''}`
        : '待加固处置决策',
      time: f.failure.decisionAt ? fmtTime(f.failure.decisionAt) : '待决策',
      done: !!f.failure.reinforceMeasures, type: f.failure.reinforceMeasures ? 'warning' : 'info'
    })
  }
  const inv = d.value.investigations?.[0]
  t.push({ label: inv ? `权属排查：${inv.investigation.conclusion === 'CONFIRMED' ? '确认权属' : '责任待定'}` : '权属排查', time: inv ? fmtTime(inv.investigation.createdAt) : '待排查', done: !!inv, type: inv ? 'primary' : 'info' })
  const cl = d.value.claims?.[0]
  t.push({ label: cl ? `权属确认：${cl.ownerOrg}（${cl.claim.claimType}）` : '权属确认', time: cl ? fmtTime(cl.claim.createdAt) : '待确认', done: !!cl, type: cl ? 'success' : 'info' })
  t.push({ label: o ? `派发维修工单（${d.value.repairTeamName}）` : '派发维修工单', time: o ? fmtTime(o.assignedAt) : '待派单', done: !!o, type: o ? 'primary' : 'info' })
  t.push({ label: '维修完成', time: o?.finishedAt ? fmtTime(o.finishedAt) : '待维修', done: !!o?.finishedAt, type: o?.finishedAt ? 'primary' : 'info' })
  const acc = d.value.acceptances?.find(a => a.acceptance.result === 'PASSED')
  t.push({ label: '验收通过', time: acc ? fmtTime(acc.acceptance.inspectedAt) : '待验收', done: !!acc, type: acc ? 'success' : 'info' })
  if (d.value.compensation) {
    t.push({ label: `赔付：${COMP_STATUS[d.value.compensation.status]?.label}`, time: d.value.compensation.paidAt ? fmtTime(d.value.compensation.paidAt) : (d.value.compensation.decidedAt ? fmtTime(d.value.compensation.decidedAt) : '审核中'), done: d.value.compensation.status === 'PAID', type: 'warning' })
  }
  t.push({ label: '费用分摊复核结案', time: i.closedAt ? fmtTime(i.closedAt) : '待复核', done: i.status === 'CLOSED', type: i.status === 'CLOSED' ? 'success' : 'info' })
  return t
})

async function load() {
  d.value = await apiIncidentDetail(id)
  if (d.value.costAllocation?.allocations) {
    costForm.allocations = allocList.value.map(a => ({ ...a }))
  }
}

async function run(fn, successMsg) {
  acting.value = true
  try {
    await fn()
    ElMessage.success(successMsg)
    await load()
  } finally {
    acting.value = false
  }
}

const doDispatch = () => run(() => apiDispatch(id), '已派单，请尽快到场')
const doSecure = () => {
  if (!secureForm.measures.length) return ElMessage.warning('请选择围护措施')
  run(() => apiSecure(id, secureForm), '临时围护已完成')
}

async function doUpload({ file }, urls, list) {
  const url = await apiUpload(file)
  urls.value.push(url)
  list.value.push({ name: file.name, url })
}
function onRemoveUpload(file, urls, list) {
  const idx = urls.value.indexOf(file.url)
  if (idx >= 0) urls.value.splice(idx, 1)
  list.value = list.value.filter(f => f.url !== file.url)
}

function resetFailureForm() {
  Object.assign(failForm, {
    lastPatrolAt: null, coneMovedAt: null, reportedBy: '',
    hasSecondAccident: false, secondAccidentAt: null,
    injuredName: '', injuredPhone: '', injuryDesc: '', accidentDesc: '',
    cause: '', causeAnalysis: '', reinforceMeasures: [], decisionNote: '',
    sensitiveLocation: false, locationType: '学校门口', locationName: '',
    peakHours: '', lighting: '一般', cameraIds: '',
    detourNotice: false, detourNote: '', trafficPoliceAssist: false, trafficPoliceNote: ''
  })
  failPhotos.value = []; failFileList.value = []; accPhotos.value = []; accFileList.value = []
}

const doPatrol = () => {
  run(() => apiNightPatrol(id, {
    roundName: patrolForm.roundName,
    arrivedAt: patrolForm.arrivedAt,
    intact: patrolForm.intact,
    siteStatus: patrolForm.siteStatus,
    photos: patrolPhotos.value,
    coneMovedAt: patrolForm.intact ? null : patrolForm.coneMovedAt,
    note: patrolForm.note
  }), '巡查记录已提交').then(() => {
    patrolForm.roundName = ''; patrolForm.arrivedAt = null
    patrolForm.intact = true; patrolForm.siteStatus = ''; patrolForm.coneMovedAt = null; patrolForm.note = ''
    patrolPhotos.value = []; patrolFileList.value = []
  })
}

const doNightFailure = () => {
  if (!failForm.coneMovedAt) return ElMessage.warning('请选择警示锥移位时间')
  if (failForm.hasSecondAccident && !failForm.secondAccidentAt) return ElMessage.warning('请选择二次事故时间')
  if (failForm.sensitiveLocation && !failForm.locationType) return ElMessage.warning('请选择敏感地点类型')
  const body = { ...failForm, photos: failPhotos.value, accidentPhotos: accPhotos.value }
  run(() => apiNightFailure(id, body), '围护失效已登记，二次事故已转交赔付专员').then(() => resetFailureForm())
}

function openDecision(f) {
  currentFailure.value = f
  Object.assign(decisionForm, {
    cause: f.cause || '', causeAnalysis: f.causeAnalysis || '',
    reinforceMeasures: f.reinforceMeasures ? f.reinforceMeasures.split(',') : [],
    decisionNote: f.decisionNote || '',
    detourNotice: !!f.detourNotice, detourNote: f.detourNote || '',
    trafficPoliceAssist: !!f.trafficPoliceAssist, trafficPoliceNote: f.trafficPoliceNote || ''
  })
  decisionVisible.value = true
}

const doDecision = () => {
  if (!decisionForm.cause) return ElMessage.warning('请选择失效原因')
  if (!decisionForm.reinforceMeasures.length) return ElMessage.warning('请至少选择一项加固措施')
  run(() => apiNightFailureDecision(currentFailure.value.id, decisionForm), '处置决策已提交')
    .then(() => { decisionVisible.value = false })
}

const doReinforce = (f) => {
  ElMessageBox.prompt('请填写加固落实情况（反光围挡/封道/值守）', '确认加固已落实', {
    confirmButtonText: '确认', cancelButtonText: '取消', inputType: 'textarea'
  }).then(({ value }) => {
    run(() => apiNightFailureReinforce(f.id, { note: value || '已落实' }), '加固处置已确认落实')
  }).catch(() => {})
}
const doInvestigate = () => {
  if (invForm.conclusion === 'CONFIRMED' && !invForm.confirmedOwnerId) {
    return ElMessage.warning('请选择权属单位')
  }
  run(() => apiInvestigate(id, invForm), '排查结论已提交')
}
const doClaim = () => run(() => apiClaim(id, { note: claimNote.value }), '认领成功，请尽快派单维修')
const doAssign = () => {
  if (!assignTeamId.value) return ElMessage.warning('请选择维修队')
  run(() => apiCreateOrder(id, { teamId: assignTeamId.value }), '工单已派发')
}
const doAccept = () => run(() => apiAcceptOrder(d.value.repairOrder.id), '已接单')
const doInspect = () => run(() => apiAcceptance(id, accForm), '验收结论已提交')
const doCostReview = async () => {
  const total = costForm.allocations.reduce((s, a) => s + Number(a.amount || 0), 0)
  if (Math.abs(total - Number(d.value.costAllocation.totalAmount)) > 0.001) {
    return ElMessage.warning(`分摊合计 ${total} 与总费用 ${d.value.costAllocation.totalAmount} 不一致`)
  }
  await ElMessageBox.confirm('分摊复核后事件将结案，确认提交？', '费用复核', { type: 'warning' })
  run(() => apiCostReview(d.value.costAllocation.id, costForm), '费用分摊完成，事件已结案')
}

onMounted(async () => {
  await load()
  meta.value = await apiMeta()
})
</script>
