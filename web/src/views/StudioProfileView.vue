<template>
  <NavBar fallback="/me" />
  <div class="page">
    <el-alert v-if="!user.isPhotographerReady" type="warning" :closable="false" show-icon class="tip"
      title="填好价格并保存后，你就会出现在首页，客户可以直接预约你" />

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="card">
        <h3 class="section-title">基础费用</h3>
        <el-form-item label="服务类型">
          <el-radio-group v-model="form.tag">
            <el-radio-button value="陪拍">陪拍</el-radio-button>
            <el-radio-button value="专摄">专摄</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div class="two">
          <el-form-item label="价格（元/小时）" prop="pricePerHour">
            <el-input-number v-model="form.pricePerHour" :min="1" :max="100000" controls-position="right" />
          </el-form-item>
          <el-form-item label="几小时起订">
            <el-input-number v-model="form.minHours" :min="1" :max="12" controls-position="right" />
          </el-form-item>
          <el-form-item label="加人费（元/人/小时）">
            <el-input-number v-model="form.extraPersonPrice" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="免交通费范围（km）">
            <el-input-number v-model="form.freeKm" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="超出交通费（元/公里）">
            <el-input-number v-model="form.kmFee" :min="0" controls-position="right" />
          </el-form-item>
        </div>
      </div>

      <div class="card">
        <h3 class="section-title">底片与精修</h3>
        <div class="two">
          <el-form-item label="每小时底片（张）">
            <el-input-number v-model="form.rawCount" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="免费精修（张）">
            <el-input-number v-model="form.retouchCount" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="超出精修（元/张）">
            <el-input-number v-model="form.retouchExtraPrice" :min="0" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="精修补充说明">
          <el-input v-model="form.retouchRule" maxlength="500" placeholder="如：下单1小时3张；下单2小时6张（以此类推）" />
        </el-form-item>
        <el-form-item label="妆造说明">
          <el-input v-model="form.makeupNote" maxlength="200" placeholder="如：不提供妆造" />
        </el-form-item>
        <el-form-item label="拍摄设备">
          <el-input v-model="form.equipment" maxlength="500" placeholder="如：佳能750d；富士xt5；iPhone16pro" />
        </el-form-item>
      </div>

      <div class="card">
        <h3 class="section-title">擅长类型</h3>
        <el-select v-model="form.styles" multiple filterable allow-create default-first-option :multiple-limit="10"
          placeholder="选择或输入，最多 10 个" style="width: 100%">
          <el-option v-for="s in STYLES" :key="s" :label="s" :value="s" />
        </el-select>
      </div>

      <div class="card">
        <h3 class="section-title">档期规则</h3>
        <el-form-item label="可接单日">
          <el-radio-group v-model="form.workDays">
            <el-radio-button v-for="(v, k) in WORK_DAYS" :key="k" :value="k">{{ v }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="每天接单时间">
          <div class="range">
            <el-time-select v-model="form.workStart" start="06:00" end="23:30" step="00:30" :clearable="false" />
            <span>至</span>
            <el-time-select v-model="form.workEnd" start="06:30" end="23:30" step="00:30" :min-time="form.workStart" :clearable="false" />
          </div>
        </el-form-item>
        <p class="muted">单独某天休息可以到「我的档期」里点击日期设置</p>
      </div>

      <div class="card">
        <h3 class="section-title">爽约规则与承诺</h3>
        <el-form-item label="取消规则">
          <el-radio-group v-model="form.cancelPolicy">
            <el-radio-button v-for="(v, k) in CANCEL_POLICY" :key="k" :value="k">{{ v.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.cancelPolicy !== 'LOOSE'" label="违约金比例（%）">
          <el-input-number v-model="form.penaltyPercent" :min="0" :max="100" controls-position="right" />
        </el-form-item>
        <p class="muted">{{ CANCEL_POLICY[form.cancelPolicy].desc(form.penaltyPercent) }}</p>
        <el-form-item>
          <el-checkbox v-model="form.refundPromise">承诺不满意全额退款</el-checkbox>
        </el-form-item>
      </div>

      <div class="card">
        <h3 class="section-title">接单位置</h3>
        <p class="muted">用来给客户显示距离，不会公开具体坐标</p>
        <el-form-item label="常驻地址">
          <el-input v-model="form.address" maxlength="200" placeholder="如：无锡市新吴区" />
        </el-form-item>
        <el-button round :icon="'Location'" :loading="locating" @click="locate">
          {{ form.lat != null || p.hasLocation ? '已设置位置，点击重新定位' : '使用当前位置' }}
        </el-button>
      </div>

      <el-button class="cta save" round size="large" :loading="saving" @click="save">保存</el-button>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import { updateMyPhotographer } from '@/api/photographer'
import { useUserStore } from '@/stores/user'
import { CANCEL_POLICY, hm, STYLES, WORK_DAYS } from '@/utils/format'
import type { PhotographerForm } from '@/types'

const user = useUserStore()
const router = useRouter()
const p = user.photographer!

const formRef = ref<FormInstance>()
const saving = ref(false)
const locating = ref(false)
// 经纬度不从后端返回（不公开），只在重新定位时提交新值；不传时后端保留原来的位置
const form = reactive<PhotographerForm>({
  tag: p.tag ?? '陪拍',
  pricePerHour: p.pricePerHour,
  extraPersonPrice: p.extraPersonPrice,
  minHours: p.minHours,
  freeKm: p.freeKm,
  kmFee: p.kmFee,
  rawCount: p.rawCount,
  retouchCount: p.retouchCount,
  retouchExtraPrice: p.retouchExtraPrice,
  retouchRule: p.retouchRule ?? '',
  makeupNote: p.makeupNote ?? '不提供妆造',
  equipment: p.equipment ?? '',
  cancelPolicy: p.cancelPolicy,
  penaltyPercent: p.penaltyPercent,
  refundPromise: p.refundPromise,
  styles: p.styles?.split(',').filter(Boolean) ?? [],
  workDays: p.workDays,
  workStart: hm(p.workStart),
  workEnd: hm(p.workEnd),
  address: p.address ?? '',
  lat: undefined,
  lng: undefined,
})
const rules: FormRules = { pricePerHour: [{ required: true, message: '请填写每小时价格' }] }

function locate() {
  if (!navigator.geolocation) return void ElMessage.error('当前浏览器不支持定位')
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      form.lat = pos.coords.latitude
      form.lng = pos.coords.longitude
      locating.value = false
      ElMessage.success('已获取当前位置')
    },
    () => {
      locating.value = false
      ElMessage.error('定位失败，请检查浏览器的定位权限')
    },
    { timeout: 8000 },
  )
}

async function save() {
  await formRef.value?.validate()
  if (form.workEnd <= form.workStart) return void ElMessage.error('结束时间要晚于开始时间')
  saving.value = true
  try {
    user.setPhotographer(await updateMyPhotographer(form))
    ElMessage.success('已保存')
    router.push(`/p/${p.userId}`)
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.tip { margin-bottom: 12px; }
.card { margin-bottom: 12px; }
.two { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 12px; }
.two :deep(.el-input-number) { width: 100%; }
.range { display: flex; align-items: center; gap: 8px; }
.range .el-select { width: 120px; }
.save { width: 100%; }
@media (min-width: 768px) {
  .page { max-width: 880px; }
  .two { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .save { width: 240px; }
}
</style>
