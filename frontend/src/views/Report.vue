<template>
  <div class="page">
    <el-card class="page-card" style="max-width: 860px; margin: 0 auto">
      <template #header><b>上报井盖问题</b></template>
      <el-alert type="warning" :closable="false" class="mb16"
                title="发现井盖缺失、松动、异响、塌陷请立即上报；如遇紧急情况请同时拨打 110 / 12345。" />
      <el-form :model="form" label-width="110px">
        <el-form-item label="问题类型" required>
          <el-radio-group v-model="form.issueType">
            <el-radio-button v-for="t in meta.issueTypes" :key="t" :value="t">{{ t }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="详细地址" required>
          <el-input v-model="form.address" placeholder="如：人民路与建设大街交口东北角" />
        </el-form-item>
        <el-form-item label="经纬度" required>
          <el-input-number v-model="form.longitude" :precision="6" :step="0.001" :min="70" :max="140" />
          <span style="margin: 0 8px">,</span>
          <el-input-number v-model="form.latitude" :precision="6" :step="0.001" :min="3" :max="55" />
          <el-button style="margin-left: 12px" size="small" @click="mockLocate">📍 模拟定位</el-button>
        </el-form-item>
        <el-form-item label="道路类型" required>
          <el-select v-model="form.roadType" style="width: 220px">
            <el-option v-for="t in meta.roadTypes" :key="t" :value="t" :label="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="现场危险程度" required>
          <el-radio-group v-model="form.dangerLevel">
            <el-radio-button value="高">高（井口裸露/塌陷）</el-radio-button>
            <el-radio-button value="中">中（明显松动）</el-radio-button>
            <el-radio-button value="低">低（异响等）</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="现场照片">
          <el-upload list-type="picture-card" :file-list="fileList" :http-request="doUpload"
                     :on-remove="onRemove" accept="image/*">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="已造成损伤">
          <el-switch v-model="form.hasDamage" active-text="已造成人车损伤" />
        </el-form-item>
        <el-form-item v-if="form.hasDamage" label="损伤情况">
          <el-input v-model="form.damageDesc" type="textarea" :rows="3"
                    placeholder="请描述人员受伤或车辆/财产损失情况（将自动转交赔付专员）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="submit">提交上报</el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiMeta, apiCreateIncident, apiUpload } from '../api'

const router = useRouter()
const meta = ref({ issueTypes: [], roadTypes: [] })
const submitting = ref(false)
const fileList = ref([])
const photos = ref([])

const form = reactive({
  issueType: '缺失',
  address: '',
  longitude: 114.514,
  latitude: 38.042,
  roadType: '主干道',
  dangerLevel: '高',
  hasDamage: false,
  damageDesc: ''
})

function mockLocate() {
  form.longitude = +(114.48 + Math.random() * 0.08).toFixed(6)
  form.latitude = +(38.02 + Math.random() * 0.05).toFixed(6)
  ElMessage.success('已模拟获取当前位置')
}

async function doUpload({ file }) {
  const url = await apiUpload(file)
  photos.value.push(url)
  fileList.value.push({ name: file.name, url })
}

function onRemove(file) {
  const idx = photos.value.indexOf(file.url)
  if (idx >= 0) photos.value.splice(idx, 1)
  fileList.value = fileList.value.filter(f => f.url !== file.url)
}

async function submit() {
  if (!form.address.trim()) {
    ElMessage.warning('请填写详细地址')
    return
  }
  if (form.hasDamage && !form.damageDesc.trim()) {
    ElMessage.warning('已造成损伤时请填写损伤情况')
    return
  }
  submitting.value = true
  try {
    const inc = await apiCreateIncident({ ...form, photos: photos.value })
    ElMessage.success(`上报成功，事件编号 ${inc.code}，已通知街道值班员`)
    router.push(`/incidents/${inc.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  meta.value = await apiMeta()
})
</script>
