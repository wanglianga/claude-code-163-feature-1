<template>
  <div class="page">
    <el-card class="page-card">
      <template #header>
        <div class="flex-between">
          <b>平台账号管理</b>
          <el-button type="primary" @click="openCreate">新建账号</el-button>
        </div>
      </template>
      <el-table :data="list" size="large">
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="name" label="姓名" width="130" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ ROLE_NAMES[row.role] || row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orgName" label="所属单位" min-width="160" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="新建账号" width="480px">
      <el-form label-width="90px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" placeholder="至少 6 位" show-password />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.role" style="width: 100%">
            <el-option v-for="(label, r) in ROLE_NAMES" :key="r" :value="r" :label="label" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属单位">
          <el-input v-model="form.orgName" placeholder="如：城市燃气集团 / 市政维修三队" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiUsers, apiCreateUser } from '../api'
import { ROLE_NAMES } from '../store/auth'
import { fmtTime } from '../utils/format'

const list = ref([])
const visible = ref(false)
const form = reactive({ username: '', password: '', name: '', role: 'CITIZEN', orgName: '', phone: '' })

function openCreate() {
  Object.assign(form, { username: '', password: '', name: '', role: 'CITIZEN', orgName: '', phone: '' })
  visible.value = true
}

async function save() {
  if (!form.username || !form.password) return ElMessage.warning('请填写用户名和密码')
  await apiCreateUser(form)
  ElMessage.success('账号已创建')
  visible.value = false
  load()
}

async function load() {
  list.value = await apiUsers()
}

onMounted(load)
</script>
