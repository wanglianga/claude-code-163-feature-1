<template>
  <div class="page">
    <el-card class="page-card">
      <template #header>
        <div class="flex-between">
          <b>施工移交规则（依据权属争议与维修超时沉淀）</b>
          <el-button type="primary" @click="openEdit(null)">新增规则</el-button>
        </div>
      </template>
      <el-table :data="list" size="large">
        <el-table-column prop="title" label="规则标题" min-width="180" />
        <el-table-column prop="content" label="规则内容" min-width="380" show-overflow-tooltip />
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

    <el-dialog v-model="visible" :title="form.id ? '编辑规则' : '新增规则'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="规则标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="规则内容" required>
          <el-input v-model="form.content" type="textarea" :rows="5" />
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
import { apiRules, apiSaveRule, apiDeleteRule } from '../api'
import { fmtTime } from '../utils/format'

const list = ref([])
const visible = ref(false)
const form = reactive({ id: null, title: '', content: '' })

function openEdit(row) {
  form.id = row?.id || null
  form.title = row?.title || ''
  form.content = row?.content || ''
  visible.value = true
}

async function save() {
  if (!form.title.trim() || !form.content.trim()) return ElMessage.warning('请填写规则标题与内容')
  await apiSaveRule(form)
  ElMessage.success('已保存')
  visible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除规则「${row.title}」？`, '删除确认', { type: 'warning' })
  await apiDeleteRule(row.id)
  ElMessage.success('已删除')
  load()
}

async function load() {
  list.value = await apiRules()
}

onMounted(load)
</script>
