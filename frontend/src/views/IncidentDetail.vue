<template>
  <div class="page" v-if="d">
    <!-- 头部状态 -->
    <el-card class="page-card">
      <el-alert v-if="d.nightFailure && d.nightFailure.failure.status !== 'HANDLED'"
                type="error" show-icon :closable="false" class="mb16"
                :title="`🌙 夜间围护失效（${d.nightFailure.causeName}）尚未加固处置，请尽快到场核查并加派措施，防止再次伤人`" />
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

        <!-- 夜间围护失效与二次事故 -->
        <el-card v-if="d.nightFailure" class="page-card" style="border-color: #f56c6c">
          <template #header>
            <div class="flex-between">
              <b style="color: #f56c6c">🌙 夜间围护失效与二次事故</b>
              <el-tag :type="NIGHT_FAILURE_STATUS[d.nightFailure.failure.status]?.type" size="small">
                {{ NIGHT_FAILURE_STATUS[d.nightFailure.failure.status]?.label }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="首次上报">{{ fmtTime(d.nightFailure.failure.firstReportAt) }}</el-descriptions-item>
            <el-descriptions-item label="警示锥移位">
              <span style="color: #f56c6c; font-weight: 600">{{ fmtTime(d.nightFailure.failure.displacedAt) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="巡查到场">
              {{ fmtTime(d.nightFailure.failure.patrolArrivedAt) }}
              <span v-if="d.nightFailure.patrolName" class="text-muted">（{{ d.nightFailure.patrolName }}）</span>
            </el-descriptions-item>
            <el-descriptions-item label="二次事故">
              <span style="color: #f56c6c; font-weight: 600">{{ fmtTime(d.nightFailure.failure.secondAccidentAt) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="事故经过" :span="2">{{ d.nightFailure.failure.secondAccidentDesc }}</el-descriptions-item>
            <el-descriptions-item label="二次事故损伤" :span="2">
              <el-tag v-if="d.nightFailure.failure.hasInjury" type="danger" size="small">有人员/财物损伤</el-tag>
              <span v-else>无</span>
              <span v-if="d.nightFailure.failure.injuryDesc" style="margin-left: 8px">
                {{ d.nightFailure.failure.injuryDesc }}
              </span>
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="d.nightFailure.photoList?.length" class="mt16">
            <div class="text-muted" style="margin-bottom: 6px">围护 / 移位现场照片</div>
            <el-image v-for="(p, i) in d.nightFailure.photoList" :key="i" :src="p"
                      :preview-src-list="d.nightFailure.photoList" fit="cover"
                      style="width: 110px; height: 110px; margin-right: 8px; border-radius: 4px" />
          </div>

          <el-descriptions :column="2" border class="mt16">
            <el-descriptions-item label="失效原因认定" :span="2">
              <el-tag v-if="d.nightFailure.failure.cause" type="warning" size="small">
                {{ d.nightFailure.causeName }}
              </el-tag>
              <span v-else class="text-muted">待认定</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="d.nightFailure.failure.causeAnalysis" label="认定分析" :span="2">
              {{ d.nightFailure.failure.causeAnalysis }}
            </el-descriptions-item>
            <el-descriptions-item label="加派措施" :span="2">
              <template v-if="d.nightFailure.failure.decisions">
                <el-tag v-for="dc in (d.nightFailure.failure.decisions || '').split(',')" :key="dc"
                        type="danger" effect="plain" size="small" style="margin-right: 4px">
                  {{ NIGHT_DECISION[dc] }}
                </el-tag>
              </template>
              <span v-else class="text-muted">待处置</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="d.nightFailure.failure.handledAt" label="处置时间">
              {{ fmtTime(d.nightFailure.failure.handledAt) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="d.nightFailure.handledByName" label="处置人">
              {{ d.nightFailure.handledByName }}
            </el-descriptions-item>
            <el-descriptions-item v-if="d.nightFailure.failure.handleNote" label="处置说明" :span="2">
              {{ d.nightFailure.failure.handleNote }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- 学校门口 / 公交站敏感信息 -->
          <el-descriptions v-if="d.nightFailure.failure.sensitiveLocation !== 'NONE'" :column="2" border class="mt16">
            <el-descriptions-item label="敏感场所" :span="2">
              <el-tag type="danger" size="small">{{ d.nightFailure.placeName }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="人流高峰">{{ d.nightFailure.failure.peakHours || '-' }}</el-descriptions-item>
            <el-descriptions-item label="照明条件">
              <el-tag :type="nightLightingType(d.nightFailure.failure.lighting)" size="small">
                {{ d.nightFailure.failure.lighting || '-' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="周边监控编号" :span="2">
              <el-tag v-for="cam in (d.nightFailure.failure.cameraIds || '').split(/[,，、]/).filter(Boolean)"
                      :key="cam" type="info" size="small" effect="plain" style="margin-right: 4px">{{ cam }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="绕行提示" :span="2">
              <el-tag :type="d.nightFailure.failure.detourNotice ? 'warning' : 'info'" size="small">
                {{ d.nightFailure.failure.detourNotice ? '已安排绕行提示' : '未安排' }}
              </el-tag>
              <span v-if="d.nightFailure.failure.detourNote" style="margin-left: 8px">
                {{ d.nightFailure.failure.detourNote }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="交警协助" :span="2">
              <el-tag :type="d.nightFailure.failure.trafficPoliceAssist ? 'danger' : 'info'" size="small">
                {{ d.nightFailure.failure.trafficPoliceAssist ? '已请交警协助' : '未申请' }}
              </el-tag>
              <span v-if="d.nightFailure.failure.trafficPoliceNote" style="margin-left: 8px">
                {{ d.nightFailure.failure.trafficPoliceNote }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
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
        <el-card v-if="d.compensation" class="page-card">
          <template #header>
            <div class="flex-between">
              <b>赔付案件</b>
              <el-tag :type="COMP_STATUS[d.compensation.status]?.type" size="small">
                {{ COMP_STATUS[d.compensation.status]?.label }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="索赔人">{{ d.compensation.claimantName }}</el-descriptions-item>
            <el-descriptions-item label="损失类型">{{ d.compensation.damageType }}</el-descriptions-item>
            <el-descriptions-item label="索赔金额">{{ money(d.compensation.claimAmount) }}</el-descriptions-item>
            <el-descriptions-item label="核定金额">{{ money(d.compensation.approvedAmount) }}</el-descriptions-item>
            <el-descriptions-item label="事故证明" :span="2">{{ d.compensation.accidentProof || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="d.compensation.analysis" label="责任分析" :span="2">
              {{ d.compensation.analysis }}
            </el-descriptions-item>
            <el-descriptions-item v-if="d.compensation.responsibility" label="责任认定" :span="2">
              {{ RESPONSIBILITY[d.compensation.responsibility] }}
              <span v-if="d.compensation.responsibility === 'SHARED'" class="text-muted">
                （权属 {{ d.compensation.ownerRatio }}% / 街道 {{ d.compensation.streetRatio }}%）
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

          <!-- 街道：夜间围护失效处理 -->
          <div v-if="nightActions.length">
            <el-divider content-position="left">🌙 夜间围护失效处理</el-divider>
            <el-alert type="error" :closable="false" class="mb16"
                      title="夜间警示锥被撞倒并发生二次事故，请按到场核查 → 原因认定 → 加派措施处置" />
            <el-button v-if="nightActions.includes('register')" type="danger" plain
                       style="width: 100%" @click="openNightRegister">
              登记夜间围护失效 / 二次事故
            </el-button>
            <el-button v-if="nightActions.includes('patrol')" type="warning" plain
                       style="width: 100%; margin-top: 8px" @click="openNightPatrol">
              巡查到场核查（补充围护照片）
            </el-button>
            <el-button v-if="nightActions.includes('assess')" type="warning" plain
                       style="width: 100%; margin-top: 8px" @click="openNightAssess">
              认定失效原因
            </el-button>
            <el-button v-if="nightActions.includes('handle')" type="danger"
                       style="width: 100%; margin-top: 8px" @click="openNightHandle">
              加派措施处置（反光围挡/封道/值守）
            </el-button>
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

    <!-- 登记夜间围护失效 / 二次事故 -->
    <el-dialog v-model="nightRegVisible" title="登记夜间围护失效与二次事故" width="640px">
      <el-form :model="nightReg" label-width="130px">
        <el-form-item label="警示锥移位时间" required>
          <el-date-picker v-model="nightReg.displacedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="围护被撞离/移位时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="二次事故时间" required>
          <el-date-picker v-model="nightReg.secondAccidentAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="第二名骑车人摔伤时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="二次事故经过" required>
          <el-input v-model="nightReg.secondAccidentDesc" type="textarea" :rows="3"
                    placeholder="如：警示锥夜间被车辆撞倒，第二名骑车人经过时摔伤" />
        </el-form-item>
        <el-form-item label="有人员/财物损伤">
          <el-switch v-model="nightReg.hasInjury" />
        </el-form-item>
        <el-form-item v-if="nightReg.hasInjury" label="损伤情况">
          <el-input v-model="nightReg.injuryDesc" type="textarea" :rows="2"
                    placeholder="伤情/财产损失（自动转交赔付专员）" />
        </el-form-item>
        <el-form-item label="围护现场照片">
          <el-upload list-type="picture-card" :file-list="nightRegFileList" :http-request="doNightUpload"
                     :on-remove="onNightRegRemove" accept="image/*">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="巡查已到场">
          <el-switch v-model="nightReg.patrolArrived" />
        </el-form-item>
        <el-divider content-position="left">如在学校门口 / 公交站附近</el-divider>
        <el-form-item label="现场位置">
          <el-radio-group v-model="nightReg.sensitiveLocation">
            <el-radio v-for="(label, key) in SENSITIVE_LOCATION_OPTS" :key="key" :value="key">{{ label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="nightReg.sensitiveLocation !== 'NONE'">
          <el-form-item label="人流高峰时段" required>
            <el-input v-model="nightReg.peakHours" placeholder="如 07:00-08:00,16:30-18:00" />
          </el-form-item>
          <el-form-item label="照明条件" required>
            <el-radio-group v-model="nightReg.lighting">
              <el-radio v-for="l in meta.lightings || []" :key="l" :value="l">{{ l }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="周边监控编号" required>
            <el-input v-model="nightReg.cameraIds" placeholder="如 JX-0112,JX-0113" />
          </el-form-item>
          <el-form-item label="绕行提示">
            <el-switch v-model="nightReg.detourNotice" />
            <el-input v-if="nightReg.detourNotice" v-model="nightReg.detourNote" class="mt16"
                      placeholder="绕行提示牌设置位置 / 引导路线" />
          </el-form-item>
          <el-form-item label="交警协助">
            <el-switch v-model="nightReg.trafficPoliceAssist" />
            <el-input v-if="nightReg.trafficPoliceAssist" v-model="nightReg.trafficPoliceNote" class="mt16"
                      placeholder="辖区交警 / 协助事项（追查肇事、维持秩序）" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="nightRegVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="submitNightRegister">提交登记</el-button>
      </template>
    </el-dialog>

    <!-- 巡查到场核查 -->
    <el-dialog v-model="nightPatrolVisible" title="巡查到场核查" width="520px">
      <el-form label-width="110px">
        <el-form-item label="围护/移位照片">
          <el-upload list-type="picture-card" :file-list="nightPatrolFileList" :http-request="doPatrolUpload"
                     :on-remove="onPatrolRemove" accept="image/*">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="现场核查备注">
          <el-input v-model="nightPatrolNote" type="textarea" :rows="3"
                    placeholder="到场核查情况（围护状态、碰撞痕迹、伤员处置）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nightPatrolVisible = false">取消</el-button>
        <el-button type="warning" :loading="acting" @click="submitNightPatrol">确认到场核查</el-button>
      </template>
    </el-dialog>

    <!-- 认定失效原因 -->
    <el-dialog v-model="nightAssessVisible" title="认定围护失效原因" width="560px">
      <el-alert type="info" :closable="false" class="mb16"
                title="结合警示锥移位时间、巡查到场间隔、现场碰撞痕迹综合判断" />
      <el-form label-width="110px">
        <el-form-item label="失效原因" required>
          <el-radio-group v-model="nightAssess.cause">
            <el-radio v-for="(label, key) in NIGHT_CAUSE_OPTS" :key="key" :value="key" :border="true"
                      style="margin-bottom: 8px">{{ label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因分析" required>
          <el-input v-model="nightAssess.causeAnalysis" type="textarea" :rows="4"
                    placeholder="如：原围护仅设警示锥，无反光围挡，夜间辨识度与抗撞能力不足……" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nightAssessVisible = false">取消</el-button>
        <el-button type="warning" :loading="acting" @click="submitNightAssess">确认认定</el-button>
      </template>
    </el-dialog>

    <!-- 加派措施处置 -->
    <el-dialog v-model="nightHandleVisible" title="加派措施处置" width="580px">
      <el-alert v-if="nightSuggest.reasons?.length" type="warning" :closable="false" class="mb16">
        <template #title>
          <div v-for="(r, i) in nightSuggest.reasons" :key="i">· {{ r }}</div>
        </template>
      </el-alert>
      <el-form label-width="120px">
        <el-form-item label="加派措施" required>
          <el-checkbox-group v-model="nightHandle.decisions">
            <el-checkbox v-for="(label, key) in NIGHT_DECISION_OPTS" :key="key" :value="key">
              {{ label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="追加应急费用">
          <el-input-number v-model="nightHandle.addCost" :min="0" :precision="2" />
          <span class="text-muted" style="margin-left: 6px">元（并入街道垫付）</span>
        </el-form-item>
        <el-form-item v-if="isSensitive" label="绕行提示">
          <el-switch v-model="nightHandle.detourNotice" />
          <el-input v-if="nightHandle.detourNotice" v-model="nightHandle.detourNote" class="mt16"
                    placeholder="绕行提示安排" />
        </el-form-item>
        <el-form-item v-if="isSensitive" label="交警协助">
          <el-switch v-model="nightHandle.trafficPoliceAssist" />
          <el-input v-if="nightHandle.trafficPoliceAssist" v-model="nightHandle.trafficPoliceNote" class="mt16"
                    placeholder="交警协助事项" />
        </el-form-item>
        <el-form-item label="处置说明">
          <el-input v-model="nightHandle.handleNote" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nightHandleVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="submitNightHandle">确认处置完成</el-button>
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
  apiCreateOrder, apiAcceptOrder, apiAcceptance, apiCostReview, apiUpload,
  apiNightRegister, apiNightPatrol, apiNightAssess, apiNightHandle, apiNightSuggest
} from '../api'
import {
  INCIDENT_STATUS, EMERGENCY_STATUS, OWNER_STATUS, REPAIR_STATUS, COMP_STATUS,
  COST_STATUS, RESPONSIBILITY, NIGHT_FAILURE_STATUS, NIGHT_CAUSE, NIGHT_DECISION,
  SENSITIVE_LOCATION, dangerType, fmtTime, money
} from '../utils/format'
import dayjs from 'dayjs'

const NIGHT_CAUSE_OPTS = NIGHT_CAUSE
const NIGHT_DECISION_OPTS = NIGHT_DECISION
const SENSITIVE_LOCATION_OPTS = SENSITIVE_LOCATION

const nightLightingType = (l) => ({ 充足: 'success', 一般: 'warning', 昏暗: 'danger', 无照明: 'danger' }[l] || 'info')

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

// ---- 夜间围护失效 ----
const nf = computed(() => d.value?.nightFailure?.failure || null)
const nightRegVisible = ref(false)
const nightPatrolVisible = ref(false)
const nightAssessVisible = ref(false)
const nightHandleVisible = ref(false)
const nightSuggest = ref({})
const nightRegFileList = ref([])
const nightRegPhotos = ref([])
const nightPatrolFileList = ref([])
const nightPatrolPhotos = ref([])

const nightReg = reactive({
  displacedAt: '', secondAccidentAt: '', secondAccidentDesc: '',
  hasInjury: true, injuryDesc: '', patrolArrived: true,
  sensitiveLocation: 'NONE', peakHours: '', lighting: '昏暗', cameraIds: '',
  detourNotice: false, detourNote: '', trafficPoliceAssist: false, trafficPoliceNote: ''
})
const nightPatrolNote = ref('')
const nightAssess = reactive({ cause: 'INADEQUATE_PROTECTION', causeAnalysis: '' })
const nightHandle = reactive({
  decisions: [], addCost: 0, handleNote: '',
  detourNotice: false, detourNote: '', trafficPoliceAssist: false, trafficPoliceNote: ''
})

const isSensitive = computed(() => {
  const loc = nf.value?.sensitiveLocation
  return loc && loc !== 'NONE'
})

const nightActions = computed(() => {
  if (!d.value || !['STREET_DUTY', 'ADMIN'].includes(role.value)) return []
  const list = []
  if (!nf.value) {
    // 已完成围护（夜间警示锥已放置）后才登记失效
    if (d.value.emergency?.securedAt) list.push('register')
  } else {
    switch (nf.value.status) {
      case 'REPORTED': list.push('patrol', 'assess'); break
      case 'VERIFIED': list.push('patrol', 'assess'); break
      case 'ASSESSED': list.push('handle'); break
      default: break
    }
  }
  return list
})

const role = computed(() => auth.role)
const inc = computed(() => d.value?.incident || {})

const actions = computed(() => {
  if (!d.value) return []
  const i = inc.value
  const list = []
  if (['STREET_DUTY', 'ADMIN'].includes(role.value)) {
    if (i.emergencyStatus === 'PENDING') list.push('dispatch')
    if (i.emergencyStatus !== 'SECURED') list.push('secure')
    if (['INVESTIGATING', 'PENDING'].includes(i.ownerStatus)) list.push('investigate')
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
  if (d.value.nightFailure) {
    const n = d.value.nightFailure.failure
    t.push({ label: `🌙 警示锥夜间移位（${dayjs(n.displacedAt).format('MM-DD HH:mm')}）`, time: fmtTime(n.displacedAt), done: true, type: 'danger' })
    t.push({ label: '二次事故：' + (n.secondAccidentDesc || '围护失效二次事故'), time: fmtTime(n.secondAccidentAt), done: true, type: 'danger' })
    t.push({ label: `巡查到场核查${d.value.nightFailure.patrolName ? '（' + d.value.nightFailure.patrolName + '）' : ''}`, time: n.patrolArrivedAt ? fmtTime(n.patrolArrivedAt) : '待到场', done: !!n.patrolArrivedAt, type: n.patrolArrivedAt ? 'warning' : 'info' })
    t.push({ label: n.cause ? `失效原因认定：${d.value.nightFailure.causeName}` : '失效原因认定', time: n.cause ? (fmtTime(n.updatedAt)) : '待认定', done: !!n.cause, type: n.cause ? 'warning' : 'info' })
    t.push({ label: n.decisions ? `加派措施：${n.decisions.split(',').map(x => NIGHT_DECISION[x]).join('、')}` : '加派措施处置', time: n.handledAt ? fmtTime(n.handledAt) : '待处置', done: !!n.handledAt, type: n.handledAt ? 'danger' : 'info' })
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

// ---- 夜间围护失效操作 ----
function resetNightReg() {
  Object.assign(nightReg, {
    displacedAt: '', secondAccidentAt: '', secondAccidentDesc: '',
    hasInjury: true, injuryDesc: '', patrolArrived: true,
    sensitiveLocation: 'NONE', peakHours: '', lighting: '昏暗', cameraIds: '',
    detourNotice: false, detourNote: '', trafficPoliceAssist: false, trafficPoliceNote: ''
  })
  nightRegPhotos.value = []
  nightRegFileList.value = []
}

function openNightRegister() {
  resetNightReg()
  nightRegVisible.value = true
}

async function doNightUpload({ file }) {
  const url = await apiUpload(file)
  nightRegPhotos.value.push(url)
  nightRegFileList.value.push({ name: file.name, url })
}
function onNightRegRemove(file) {
  const idx = nightRegPhotos.value.indexOf(file.url)
  if (idx >= 0) nightRegPhotos.value.splice(idx, 1)
  nightRegFileList.value = nightRegFileList.value.filter(f => f.url !== file.url)
}
async function doPatrolUpload({ file }) {
  const url = await apiUpload(file)
  nightPatrolPhotos.value.push(url)
  nightPatrolFileList.value.push({ name: file.name, url })
}
function onPatrolRemove(file) {
  const idx = nightPatrolPhotos.value.indexOf(file.url)
  if (idx >= 0) nightPatrolPhotos.value.splice(idx, 1)
  nightPatrolFileList.value = nightPatrolFileList.value.filter(f => f.url !== file.url)
}

async function submitNightRegister() {
  if (!nightReg.displacedAt) return ElMessage.warning('请选择警示锥移位时间')
  if (!nightReg.secondAccidentAt) return ElMessage.warning('请选择二次事故时间')
  if (!nightReg.secondAccidentDesc.trim()) return ElMessage.warning('请描述二次事故经过')
  if (nightReg.hasInjury && !nightReg.injuryDesc.trim()) return ElMessage.warning('请填写损伤情况')
  if (nightReg.sensitiveLocation !== 'NONE') {
    if (!nightReg.peakHours.trim()) return ElMessage.warning('请填写人流高峰时段')
    if (!nightReg.cameraIds.trim()) return ElMessage.warning('请填写周边监控编号')
  }
  const payload = {
    displacedAt: nightReg.displacedAt,
    secondAccidentAt: nightReg.secondAccidentAt,
    secondAccidentDesc: nightReg.secondAccidentDesc,
    hasInjury: nightReg.hasInjury,
    injuryDesc: nightReg.injuryDesc,
    protectionPhotos: nightRegPhotos.value,
    sensitiveLocation: nightReg.sensitiveLocation,
    peakHours: nightReg.peakHours, lighting: nightReg.lighting, cameraIds: nightReg.cameraIds,
    detourNotice: nightReg.detourNotice, detourNote: nightReg.detourNote,
    trafficPoliceAssist: nightReg.trafficPoliceAssist, trafficPoliceNote: nightReg.trafficPoliceNote,
    patrolArrivedAt: nightReg.patrolArrived ? dayjs().format('YYYY-MM-DDTHH:mm:ss') : null
  }
  await run(() => apiNightRegister(id, payload), '已登记夜间围护失效与二次事故')
  nightRegVisible.value = false
}

function openNightPatrol() {
  nightPatrolPhotos.value = []
  nightPatrolFileList.value = []
  nightPatrolNote.value = ''
  nightPatrolVisible.value = true
}
async function submitNightPatrol() {
  await run(() => apiNightPatrol(id, {
    protectionPhotos: nightPatrolPhotos.value, note: nightPatrolNote.value
  }), '已记录巡查到场核查')
  nightPatrolVisible.value = false
}

function openNightAssess() {
  nightAssess.cause = nf.value?.cause || 'INADEQUATE_PROTECTION'
  nightAssess.causeAnalysis = nf.value?.causeAnalysis || ''
  nightAssessVisible.value = true
}
async function submitNightAssess() {
  if (!nightAssess.causeAnalysis.trim()) return ElMessage.warning('请填写原因分析')
  await run(() => apiNightAssess(id, { ...nightAssess }), '失效原因已认定')
  nightAssessVisible.value = false
}

async function openNightHandle() {
  nightSuggest.value = {}
  Object.assign(nightHandle, {
    decisions: [], addCost: 0, handleNote: '',
    detourNotice: nf.value?.detourNotice || false, detourNote: nf.value?.detourNote || '',
    trafficPoliceAssist: nf.value?.trafficPoliceAssist || false,
    trafficPoliceNote: nf.value?.trafficPoliceNote || ''
  })
  nightHandleVisible.value = true
  try {
    const s = await apiNightSuggest(id)
    nightSuggest.value = s
    nightHandle.decisions = [...(s.decisions || [])]
    if (isSensitive.value) {
      nightHandle.detourNotice = !!s.suggestDetourNotice
      nightHandle.trafficPoliceAssist = !!s.suggestTrafficPolice
    }
  } catch { /* 建议获取失败不阻塞手工处置 */ }
}
async function submitNightHandle() {
  if (!nightHandle.decisions.length) return ElMessage.warning('请至少选择一项加派措施')
  if (isSensitive.value && !nightHandle.detourNotice && !nightHandle.trafficPoliceAssist) {
    return ElMessage.warning('学校门口/公交站附近须至少安排绕行提示或申请交警协助')
  }
  await run(() => apiNightHandle(id, { ...nightHandle }), '加派措施已落实')
  nightHandleVisible.value = false
}

onMounted(async () => {
  await load()
  meta.value = await apiMeta()
})
</script>
