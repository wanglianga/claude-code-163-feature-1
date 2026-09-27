<template>
  <div class="page">
    <el-card class="page-card">
      <template #header>
        <div class="flex-between">
          <b>日常巡检路线（依据高发路段统计动态调整）</b>
          <el-button type="primary" @click="openEdit(null)">新增路线</el-button>
        </div>
      </template>
      <el-table :data="list" size="large">
        <el-table-column prop="name" label="路线名称" min-width="180" />
        <el-table-column label="覆盖路段" min-width="240">
          <template #default="{ row }">
            <el-tag v-for="s in (row.sections || '').split(',').filter(Boolean)" :key="s"
                    size="small" style="margin-right: 4px">{{ s }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="frequency" label="巡检频次" width="100" />
        <el-table-column prop="note" label="调整依据" min-width="200" show-overflow-tooltip />
        <el-table-column label="更新时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑路线' : '新增路线'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="路线名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="覆盖路段">
          <el-select v-model="sections" multiple filterable allow-create default-first-option
                     style="width: 100%" placeholder="输入路段名后回车，可从高发路段选择">
            <el-option v-for="h in hotspots" :key="h.address" :value="h.address"
                       :label="`${h.address}（${h.count} 起）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="巡检频次">
          <el-radio-group v-model="form.frequency">
            <el-radio value="每日">每日</el-radio>
            <el-radio value="隔日">隔日</el-radio>
            <el-radio value="每周">每周</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整依据">
          <el-input v-model="form.note" type="textarea" :rows="2" placeholder="如：近 90 天高发路段，加密巡检" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiRoutes, apiSaveRoute, apiDeleteRoute, apiStatsHotspots } from '../api'
import { fmtTime } from '../utils/format'

const list = ref([])
const hotspots = ref([])
const visible = ref(false)
const sections = ref([])
const form = reactive({ id: null, name: '', frequency: '每日', note: '' })

function openEdit(row) {
  form.id = row?.id || null
  form.name = row?.name || ''
  form.frequency = row?.frequency || '每日'
  form.note = row?.note || ''
  sections.value = row ? (row.sections || '').split(',').filter(Boolean) : []
  visible.value = true
}

async function save() {
  if (!form.name.trim()) return ElMessage.warning('请填写路线名称')
  await apiSaveRoute({ ...form, sections: sections.value.join(',') })
  ElMessage.success('已保存')
  visible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除路线「${row.name}」？`, '删除确认', { type: 'warning' })
  await apiDeleteRoute(row.id)
  ElMessage.success('已删除')
  load()
}

async function load() {
  list.value = await apiRoutes()
}

onMounted(async () => {
  await load()
  hotspots.value = await apiStatsHotspots()
})
</script>
