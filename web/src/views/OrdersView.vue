<template>
  <div class="orders">
    <header class="top">
      <h1>订单</h1>
      <div class="roles">
        <button :class="{ active: role === 'customer' }" @click="setRole('customer')">我约的</button>
        <button :class="{ active: role === 'photographer' }" @click="setRole('photographer')">约我的</button>
      </div>
      <div class="chips">
        <button class="chip" :class="{ active: !status }" @click="setStatus(undefined)">全部</button>
        <button v-for="(v, k) in BOOKING_STATUS" :key="k" class="chip" :class="{ active: status === k }" @click="setStatus(k)">
          {{ v.label }}
        </button>
      </div>
    </header>

    <div class="list">
      <div v-for="v in list" :key="v.booking.id" class="card order">
        <div class="row head">
          <component :is="role === 'customer' ? RouterLink : 'div'" :to="role === 'customer' ? `/p/${other(v).id}` : undefined" class="who">
            <el-avatar :size="36" :src="other(v).avatarUrl">{{ other(v).nickname.slice(0, 1) }}</el-avatar>
            <span class="name">{{ other(v).nickname }}</span>
          </component>
          <el-tag :type="BOOKING_STATUS[v.booking.status].type" effect="light" round>{{ BOOKING_STATUS[v.booking.status].label }}</el-tag>
        </div>
        <div class="when">
          <el-icon><Calendar /></el-icon>
          <span>{{ fmtDay(v.booking.shootDate) }} {{ hm(v.booking.startTime) }}~{{ addHours(hm(v.booking.startTime), v.booking.hours) }}</span>
          <span class="muted">{{ v.booking.hours }}小时 · {{ v.booking.people }}人</span>
        </div>
        <dl>
          <template v-if="v.booking.style"><dt>类型</dt><dd>{{ v.booking.style }}</dd></template>
          <template v-if="v.booking.address"><dt>地点</dt><dd>{{ v.booking.address }}</dd></template>
          <template v-if="v.booking.remark"><dt>留言</dt><dd>{{ v.booking.remark }}</dd></template>
          <template v-if="role === 'photographer'"><dt>客户联系</dt><dd class="contact">{{ v.booking.customerContact }}</dd></template>
          <template v-if="v.photographerContact"><dt>摄影师</dt><dd class="contact">{{ v.photographerContact }}</dd></template>
          <dt>预估费用</dt><dd class="money">¥{{ v.booking.priceEstimate }}</dd>
        </dl>
        <p v-if="hint(v)" class="hint">{{ hint(v) }}</p>
        <div class="ops">
          <span class="muted">{{ fmtDateTime(v.booking.createdAt) }} 提交</span>
          <template v-if="role === 'photographer'">
            <template v-if="v.booking.status === 'PENDING'">
              <el-button round size="small" @click="act(v, 'reject')">拒绝</el-button>
              <el-button round size="small" class="cta" @click="act(v, 'confirm')">确认接单</el-button>
            </template>
            <template v-else-if="v.booking.status === 'CONFIRMED'">
              <el-button round size="small" @click="act(v, 'cancel')">取消</el-button>
              <el-button round size="small" class="cta" :disabled="v.booking.shootDate > today" @click="act(v, 'complete')">拍摄完成</el-button>
            </template>
          </template>
          <template v-else>
            <el-button v-if="['PENDING', 'CONFIRMED'].includes(v.booking.status)" round size="small" @click="act(v, 'cancel')">取消预约</el-button>
            <el-button v-if="v.booking.status === 'COMPLETED' && !v.booking.reviewed" round size="small" class="cta" @click="openReview(v)">去评价</el-button>
            <span v-if="v.booking.reviewed" class="muted">已评价</span>
          </template>
        </div>
      </div>
    </div>
    <div v-if="loading" v-loading="true" style="height: 120px" />
    <el-empty v-else-if="!list.length" :description="role === 'customer' ? '还没有预约，去首页找个摄影师吧' : '还没有人约你，完善资料和客片会更容易被约哦'" />
    <div v-else-if="list.length < total" class="more"><el-button round @click="load()">加载更多</el-button></div>

    <el-dialog v-model="reviewVisible" title="评价摄影师" width="400px" align-center>
      <div class="rate"><el-rate v-model="score" size="large" show-text :texts="['很差', '较差', '一般', '满意', '超棒']" /></div>
      <el-input v-model="content" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="说说这次拍摄的体验吧" />
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button class="cta" :loading="saving" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bookingAction, listBookings, reviewBooking, type BookingAction } from '@/api/booking'
import { addHours, BOOKING_STATUS, fmtDateTime, fmtDay, hm, ymd } from '@/utils/format'
import type { BookingStatus, BookingView } from '@/types'

type Role = 'customer' | 'photographer'
const PAGE_SIZE = 20
const route = useRoute()
const router = useRouter()
const today = ymd()

const role = ref<Role>(route.query.role === 'photographer' ? 'photographer' : 'customer')
const status = ref<BookingStatus>()
const list = ref<BookingView[]>([])
const total = ref(0)
const loading = ref(false)
let page = 0

const other = (v: BookingView) => (role.value === 'customer' ? v.photographer : v.customer)

function hint(v: BookingView) {
  const b = v.booking
  if (role.value === 'customer') {
    if (b.status === 'PENDING') return '等待摄影师确认，确认后可看到摄影师联系方式'
    if (b.status === 'CONFIRMED') return '摄影师已接单，请通过上面的联系方式沟通拍摄细节和付款'
  } else {
    if (b.status === 'PENDING') return '确认前可以先通过客户留下的联系方式沟通'
    if (b.status === 'CONFIRMED' && b.shootDate > today) return '拍摄当天及之后可以标记完成'
  }
  return ''
}

async function load(reset = false) {
  if (reset) {
    list.value = []
    page = 0
  }
  loading.value = true
  try {
    const res = await listBookings({ role: role.value, status: status.value, page: page + 1, size: PAGE_SIZE })
    list.value.push(...res.records)
    total.value = res.total
    page++
  } finally {
    loading.value = false
  }
}

function setRole(r: Role) {
  role.value = r
  router.replace({ query: { role: r } })
  load(true)
}

function setStatus(s?: BookingStatus) {
  status.value = s
  load(true)
}

const ACTION_TEXT: Record<BookingAction, string> = {
  confirm: '确认接这个单？确认后客户可以看到你的联系方式',
  reject: '拒绝这个预约？',
  cancel: '取消这个预约？请先和对方沟通好',
  complete: '确认已完成拍摄？完成后客户可以评价',
}

async function act(v: BookingView, action: BookingAction) {
  const ok = await ElMessageBox.confirm(ACTION_TEXT[action], '提示', { type: action === 'confirm' || action === 'complete' ? 'info' : 'warning' })
    .catch(() => false)
  if (!ok) return
  const fresh = await bookingAction(v.booking.id, action)
  Object.assign(v, fresh)
  ElMessage.success('操作成功')
}

const reviewVisible = ref(false)
const reviewing = ref<BookingView>()
const score = ref(5)
const content = ref('')
const saving = ref(false)

function openReview(v: BookingView) {
  reviewing.value = v
  score.value = 5
  content.value = ''
  reviewVisible.value = true
}

async function submitReview() {
  saving.value = true
  try {
    await reviewBooking(reviewing.value!.booking.id, score.value, content.value || undefined)
    reviewing.value!.booking.reviewed = true
    reviewVisible.value = false
    ElMessage.success('感谢你的评价')
  } finally {
    saving.value = false
  }
}

onMounted(() => load(true))
</script>

<style lang="scss" scoped>
.top { position: sticky; top: 0; z-index: 5; padding: 16px 12px 10px; background: var(--bg); }
h1 { margin: 0 0 12px; font-size: 24px; }
.roles { display: flex; gap: 24px; margin-bottom: 10px; }
.roles button { border: none; background: none; padding: 4px 0; font-size: 16px; color: #888; cursor: pointer; }
.roles button.active { color: var(--text); font-weight: 700; box-shadow: inset 0 -4px var(--brand); }
.list { padding: 0 12px; }
.order { margin-bottom: 12px; }
.row { display: flex; align-items: center; justify-content: space-between; }
.who { display: flex; align-items: center; gap: 8px; }
.name { font-weight: 700; }
.when { display: flex; align-items: center; flex-wrap: wrap; gap: 4px 6px; margin: 12px 0 8px; font-weight: 600; }
.when span { white-space: nowrap; }
.when .muted { font-weight: 400; }
dl { display: grid; grid-template-columns: 72px 1fr; gap: 6px 8px; margin: 0; font-size: 14px; }
dt { color: var(--text-muted); }
dd { margin: 0; word-break: break-all; }
.contact { font-weight: 700; color: var(--link); user-select: all; }
.money { color: #e5483d; font-weight: 700; }
.hint { margin: 10px 0 0; padding: 8px 10px; border-radius: 8px; background: var(--brand-soft); font-size: 13px; }
.ops { display: flex; align-items: center; justify-content: flex-end; gap: 8px; margin-top: 12px; padding-top: 10px; border-top: 1px solid #f2f2f2; }
.ops .muted { margin-right: auto; font-size: 12px; }
.more { text-align: center; padding: 12px; }
.rate { display: flex; justify-content: center; margin-bottom: 12px; }

/* PC 工作台：标题和筛选不吸顶，订单两列 */
@media (min-width: 768px) {
  .top { position: static; padding: 0 0 12px; }
  h1 { font-size: 22px; margin-bottom: 16px; }
  .list { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 14px; padding: 0; align-items: start; }
  .order { margin: 0; }
}
</style>
