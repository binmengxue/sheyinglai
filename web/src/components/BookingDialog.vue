<template>
  <el-dialog v-model="visible" title="预约拍摄" width="440px" align-center>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="82px">
      <el-form-item label="日期">
        <span>{{ fmtDay(date) }}</span>
      </el-form-item>
      <el-form-item label="开始" prop="startTime">
        <el-select v-model="form.startTime" placeholder="选择开始时间">
          <el-option v-for="t in startOptions" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>
      <el-form-item label="时长">
        <el-input-number v-model="form.hours" :min="minHours" :max="maxHours" />
        <span class="muted gap">小时，{{ form.startTime ? `${form.startTime}~${addHours(form.startTime, form.hours)}` : '' }}</span>
      </el-form-item>
      <el-form-item label="人数">
        <el-input-number v-model="form.people" :min="1" :max="20" />
      </el-form-item>
      <el-form-item v-if="styles.length" label="类型">
        <el-select v-model="form.style" clearable placeholder="想拍什么类型">
          <el-option v-for="s in styles" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="地点">
        <el-input v-model="form.address" maxlength="200" placeholder="拍摄地点，如：xx 公园东门" />
      </el-form-item>
      <el-form-item label="联系方式" prop="contact">
        <el-input v-model="form.contact" maxlength="64" placeholder="微信号或手机号，方便摄影师联系你" />
      </el-form-item>
      <el-form-item label="留言">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" placeholder="想要的风格、参考图说明等" />
      </el-form-item>
    </el-form>
    <div class="estimate">
      预估费用 <b>¥{{ estimate }}</b>
      <div class="muted">平台不在线收款，摄影师确认后双方线下沟通付款；交通费、超出精修另算</div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button class="cta" :loading="saving" @click="submit">提交预约</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { type FormInstance, type FormRules } from 'element-plus'
import { toast } from '@/utils/message'
import { createBooking } from '@/api/booking'
import { useUserStore } from '@/stores/user'
import { addHours, fmtDay, hm, toMinutes } from '@/utils/format'
import type { Photographer, Slot } from '@/types'

const props = defineProps<{ p: Photographer; date: string; slots: Slot[] }>()
const visible = defineModel<boolean>({ required: true })
const user = useUserStore()
const router = useRouter()

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ startTime: '', hours: 1, people: 1, style: '', address: '', contact: '', remark: '' })
const rules: FormRules = {
  startTime: [{ required: true, message: '请选择开始时间' }],
  contact: [{ required: true, message: '请留下联系方式', trigger: 'blur' }],
}

const minHours = computed(() => props.p.minHours)
const styles = computed(() => props.p.styles?.split(',').filter(Boolean) ?? [])

/** 每半小时一个开始时间，保证从这里开始至少能拍满起拍时长 */
const startOptions = computed(() => {
  const out: string[] = []
  for (const s of props.slots) {
    const end = toMinutes(hm(s.end))
    for (let m = toMinutes(hm(s.start)); m + minHours.value * 60 <= end; m += 30) {
      out.push(`${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`)
    }
  }
  return out
})

/** 所选开始时间所在空档最多能拍几个小时 */
const maxHours = computed(() => {
  if (!form.startTime) return 12
  const start = toMinutes(form.startTime)
  const slot = props.slots.find((s) => start >= toMinutes(hm(s.start)) && start < toMinutes(hm(s.end)))
  return slot ? Math.max(minHours.value, Math.floor((toMinutes(hm(slot.end)) - start) / 60)) : minHours.value
})

const estimate = computed(() =>
  (props.p.pricePerHour ?? 0) * form.hours + props.p.extraPersonPrice * Math.max(0, form.people - 1) * form.hours)

watch(maxHours, (m) => { if (form.hours > m) form.hours = m })

watch(visible, (v) => {
  if (!v) return
  Object.assign(form, {
    startTime: startOptions.value[0] ?? '',
    hours: minHours.value,
    people: 1,
    style: '',
    address: '',
    remark: '',
    contact: form.contact || user.user?.wechat || user.user?.phone || '',
  })
})

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    await createBooking({ photographerId: props.p.userId, shootDate: props.date, ...form, style: form.style || undefined })
    toast.success('预约已提交，等待摄影师确认')
    visible.value = false
    router.push('/orders')
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.gap { margin-left: 8px; }
.estimate { padding: 12px; border-radius: 10px; background: var(--brand-soft); }
.estimate b { font-size: 20px; color: #e5483d; }
.estimate .muted { margin-top: 4px; font-size: 12px; }
</style>
