<template>
  <NavBar :fallback="`/p/${id}`" />
  <div class="page layout">
    <div v-if="detail" class="head">
      <PhotographerCard :c="detail.card" compact />
      <RouterLink :to="`/p/${id}`" class="to-home">点击去主页看客片</RouterLink>
    </div>

    <div class="card cal">
      <div class="month">
        <button class="nav" :disabled="!canPrev" @click="shift(-1)"><el-icon><ArrowLeft /></el-icon></button>
        <h2>{{ month.replace('-', '年') }}月</h2>
        <button class="nav" :disabled="!canNext" @click="shift(1)"><el-icon><ArrowRight /></el-icon></button>
      </div>
      <div class="week"><span v-for="w in WEEK" :key="w">{{ w }}</span></div>
      <div v-loading="loading" class="days">
        <span v-for="n in lead" :key="'b' + n" />
        <button
          v-for="day in schedule?.days ?? []" :key="day.date" class="day" :class="[day.status, { sel: day.date === selected }]"
          @click="pick(day)"
        >
          <b>{{ Number(day.date.slice(8)) }}</b>
          <small>{{ DAY_STATUS[day.status] }}</small>
        </button>
      </div>
      <p v-if="self" class="muted tip">点击未来的日期可以设置/取消休息；已确认的预约会自动占用档期</p>
    </div>

    <div v-if="selectedDay" class="card slots">
      <div class="slot-title">{{ fmtDay(selectedDay.date) }}</div>
      <template v-if="selectedDay.status === 'AVAILABLE'">
        <div v-for="s in selectedDay.free" :key="s.start" class="slot">
          <div class="time"><b>{{ hm(s.start) }}</b><b>{{ hm(s.end) }}</b></div>
          <span class="dot" /> 可约
          <el-button v-if="!self" class="cta right" round @click="book">预约</el-button>
        </div>
        <p class="muted">{{ schedule?.minHours }}小时起拍，开始时间可选整点或半点</p>
      </template>
      <p v-else class="muted">{{ DAY_STATUS[selectedDay.status] }}，请选择其它日期</p>
    </div>
  </div>

  <BookingDialog v-if="detail && selectedDay" v-model="bookingVisible" :p="detail.profile" :date="selectedDay.date" :slots="selectedDay.free" />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import PhotographerCard from '@/components/PhotographerCard.vue'
import BookingDialog from '@/components/BookingDialog.vue'
import { getPhotographer, getSchedule, setDayOff } from '@/api/photographer'
import { useUserStore } from '@/stores/user'
import { DAY_STATUS, fmtDay, getLocation, hm, ymd } from '@/utils/format'
import type { DayInfo, PhotographerDetail, ScheduleView } from '@/types'

const props = defineProps<{ id: string }>()
const user = useUserStore()
const router = useRouter()

const WEEK = ['日', '一', '二', '三', '四', '五', '六']
/** 最多看到 3 个月后（后端最多允许预约 90 天内） */
const MAX_MONTHS_AHEAD = 3

const thisMonth = ymd().slice(0, 7)
const month = ref(thisMonth)
const detail = ref<PhotographerDetail | null>(null)
const schedule = ref<ScheduleView | null>(null)
const loading = ref(false)
const selected = ref('')
const bookingVisible = ref(false)

const self = computed(() => user.user?.id === Number(props.id))
const selectedDay = computed(() => schedule.value?.days.find((d) => d.date === selected.value))
/** 1 号前面空几格 */
const lead = computed(() => {
  const [y, m] = month.value.split('-').map(Number)
  return new Date(y, m - 1, 1).getDay()
})
const monthIndex = (s: string) => Number(s.slice(0, 4)) * 12 + Number(s.slice(5, 7)) - 1
const canPrev = computed(() => month.value > thisMonth)
const canNext = computed(() => monthIndex(month.value) - monthIndex(thisMonth) < MAX_MONTHS_AHEAD)

function shift(n: number) {
  const i = monthIndex(month.value) + n
  month.value = `${Math.floor(i / 12)}-${String((i % 12) + 1).padStart(2, '0')}`
}

async function loadSchedule() {
  loading.value = true
  try {
    schedule.value = await getSchedule(Number(props.id), month.value)
    // 默认选中本月第一个可约日
    if (!selectedDay.value) selected.value = schedule.value.days.find((d) => d.status === 'AVAILABLE')?.date ?? ''
  } finally {
    loading.value = false
  }
}

async function pick(day: DayInfo) {
  if (day.status === 'EXPIRED') return
  if (self.value) {
    // 本人：切换休息。约满的天说明有已确认预约，不能整天设为休息
    if (day.status === 'FULL') return void ElMessage.info('这天已约满，有已确认的预约')
    const off = day.status !== 'OFF'
    const ok = await ElMessageBox.confirm(off ? `把 ${fmtDay(day.date)} 设为休息？` : `取消 ${fmtDay(day.date)} 的休息？`, '档期设置')
      .catch(() => false)
    if (!ok) return
    await setDayOff(day.date, off)
    ElMessage.success('已更新')
    await loadSchedule()
    // 按接单规则本来就不接单的星期，取消休息后仍是休息
    const now = schedule.value?.days.find((d) => d.date === day.date)
    if (!off && now?.status === 'OFF') ElMessage.info('这天不在你的可接单日内，可到「接单设置」修改')
  }
  selected.value = day.date
}

function book() {
  if (!user.loggedIn) return router.push({ path: '/login', query: { redirect: `/p/${props.id}/schedule` } })
  bookingVisible.value = true
}

watch(() => props.id, async (id) => {
  detail.value = await getPhotographer(Number(id), await getLocation(), false)
}, { immediate: true })
watch([() => props.id, month], loadSchedule, { immediate: true })
</script>

<style lang="scss" scoped>
.head { background: #fff; border-radius: var(--radius); overflow: hidden; margin-bottom: 12px; }
.to-home { display: block; padding: 10px; text-align: center; font-size: 13px; color: #555; border-top: 1px solid #f2f2f2; }
.month { display: flex; align-items: center; justify-content: space-between; }
.month h2 { margin: 0; font-size: 22px; }
.nav { width: 36px; height: 36px; border: none; background: #f4f5f7; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.nav:disabled { opacity: 0.3; cursor: default; }
.week, .days { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; }
.week { margin: 16px 0 6px; color: #999; font-size: 13px; }
.days { row-gap: 6px; min-height: 200px; }
.day { display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 8px 0; border: none; border-radius: 10px; background: none; cursor: pointer; font: inherit; }
.day b { font-size: 18px; font-weight: 600; }
.day small { font-size: 11px; }
.day.AVAILABLE small { color: var(--link); }
.day.EXPIRED, .day.OFF, .day.FULL { color: #c0c4cc; }
.day.EXPIRED { cursor: default; }
.day.sel { background: var(--brand); color: var(--text); }
.day.sel small { color: var(--text); }
.tip { margin: 12px 0 0; }
.slot-title { font-weight: 700; margin-bottom: 8px; }
.slot { display: flex; align-items: center; gap: 10px; padding: 10px 0; font-weight: 700; }
.time { display: flex; flex-direction: column; font-size: 18px; margin-right: 10px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: var(--ok); }
.right { margin-left: auto; }

/* PC：左边日历，右边摄影师 + 当天时段 */
@media (min-width: 768px) {
  .layout {
    display: grid; grid-template-columns: minmax(0, 1fr) 400px; grid-template-rows: auto 1fr; gap: 16px; align-items: start;
    grid-template-areas: 'cal head' 'cal slots';
  }
  .layout > .head, .layout > .card { margin: 0; }
  .head { grid-area: head; }
  .cal { grid-area: cal; padding: 24px; }
  .slots { grid-area: slots; }
  .day { padding: 14px 0; }
  .day b { font-size: 20px; }
}
</style>
