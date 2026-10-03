import dayjs from 'dayjs'

export const fmtTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')

export const INCIDENT_STATUS = {
  REPORTED: { label: '已上报', type: 'danger' },
  SECURED: { label: '已临时围护', type: 'warning' },
  OWNER_CONFIRMED: { label: '权属已确认', type: 'primary' },
  REPAIRING: { label: '维修中', type: 'primary' },
  REPAIRED: { label: '待验收', type: 'warning' },
  ACCEPTED: { label: '验收通过', type: 'success' },
  CLOSED: { label: '已结案', type: 'info' }
}

export const EMERGENCY_STATUS = {
  PENDING: { label: '应急待派单', type: 'danger' },
  DISPATCHED: { label: '应急已派单', type: 'warning' },
  SECURED: { label: '围护已完成', type: 'success' }
}

export const OWNER_STATUS = {
  UNCONFIRMED: { label: '权属未确认', type: 'info' },
  INVESTIGATING: { label: '权属排查中', type: 'warning' },
  CONFIRMED: { label: '权属已确认', type: 'success' },
  PENDING: { label: '责任待定', type: 'danger' }
}

export const REPAIR_STATUS = {
  ASSIGNED: { label: '待接单', type: 'warning' },
  ACCEPTED: { label: '维修中', type: 'primary' },
  FINISHED: { label: '已完成', type: 'success' },
  RETURNED: { label: '退回重修', type: 'danger' }
}

export const COMP_STATUS = {
  FILED: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已核定', type: 'primary' },
  REJECTED: { label: '已驳回', type: 'info' },
  PAID: { label: '已赔付', type: 'success' }
}

export const COST_STATUS = {
  PENDING: { label: '待复核', type: 'warning' },
  ALLOCATED: { label: '已分摊', type: 'success' }
}

export const RESPONSIBILITY = {
  OWNER: '权属单位全责',
  STREET: '街道责任',
  SHARED: '多方分摊',
  UNDETERMINED: '无法认定'
}

export const NIGHT_FAILURE_STATUS = {
  REPORTED: { label: '已登记待核查', type: 'danger' },
  VERIFIED: { label: '已到场待认定', type: 'warning' },
  ASSESSED: { label: '已认定待处置', type: 'warning' },
  HANDLED: { label: '已加固处置', type: 'success' }
}

export const NIGHT_CAUSE = {
  INADEQUATE_PROTECTION: '围护方式不足',
  PATROL_GAP: '巡查间隔过长',
  EXTERNAL_DAMAGE: '外力破坏'
}

export const NIGHT_DECISION = {
  REFLECTIVE_BARRIER: '加派反光围挡',
  ROAD_CLOSURE: '临时封道',
  GUARD_DUTY: '专人值守'
}

export const SENSITIVE_LOCATION = {
  NONE: '非敏感场所',
  SCHOOL_GATE: '学校门口',
  BUS_STOP: '公交站附近'
}

export const dangerType = (level) => ({ 高: 'danger', 中: 'warning', 低: 'info' }[level] || 'info')

export const money = (v) => `¥${Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
