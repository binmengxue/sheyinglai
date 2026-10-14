<template>
  <div class="card order">
    <div class="row head">
      <component :is="role === 'customer' ? RouterLink : 'div'" :to="role === 'customer' ? `/p/${other.id}` : undefined" class="who">
        <el-avatar :size="36" :src="other.avatarUrl">{{ other.nickname.slice(0, 1) }}</el-avatar>
        <span class="name">{{ other.nickname }}</span>
      </component>
      <el-tag :type="BOOKING_STATUS[b.status].type" effect="light" round>{{ BOOKING_STATUS[b.status].label }}</el-tag>
    </div>
    <div class="when">
      <el-icon><Calendar /></el-icon>
      <span>{{ fmtDay(b.shootDate) }} {{ hm(b.startTime) }}~{{ addHours(hm(b.startTime), b.hours) }}</span>
      <span class="muted">{{ b.hours }}小时 · {{ b.people }}人</span>
    </div>
    <dl>
      <template v-if="b.style"><dt>类型</dt><dd>{{ b.style }}</dd></template>
      <template v-if="b.address"><dt>地点</dt><dd>{{ b.address }}</dd></template>
      <template v-if="b.remark"><dt>留言</dt><dd>{{ b.remark }}</dd></template>
      <template v-if="role === 'photographer'">
        <dt>客户联系</dt><dd class="contact">{{ b.customerContact }} <CopyButton :text="b.customerContact" /></dd>
      </template>
      <template v-if="v.photographerContact">
        <dt>摄影师</dt><dd class="contact">{{ v.photographerContact }} <CopyButton :text="v.photographerContact" /></dd>
      </template>
      <dt>预估费用</dt><dd class="money">¥{{ b.priceEstimate }}</dd>
      <template v-if="b.status === 'CANCELLED' && b.cancelReason"><dt>取消原因</dt><dd>{{ b.cancelReason }}</dd></template>
      <template v-if="b.penaltyAmount"><dt>违约金</dt><dd class="money">¥{{ b.penaltyAmount }}（线下结算）</dd></template>
      <template v-if="detail && b.rescheduleCount"><dt>改期</dt><dd>已改期 {{ b.rescheduleCount }} 次</dd></template>
      <template v-if="detail"><dt>提交时间</dt><dd>{{ fmtDateTime(b.createdAt) }}</dd></template>
    </dl>
    <p v-if="hint" class="hint">{{ hint }}</p>
    <div class="ops">
      <RouterLink v-if="!detail" :to="`/orders/${b.id}`" class="muted more-link">{{ fmtDateTime(b.createdAt) }} 提交 · 详情</RouterLink>
      <template v-if="role === 'photographer'">
        <template v-if="b.status === 'PENDING'">
          <el-button round size="small" :loading="acting === b.id" @click="act(v, 'reject')">拒绝</el-button>
          <el-button round size="small" class="cta" :loading="acting === b.id" @click="act(v, 'confirm')">确认接单</el-button>
        </template>
        <template v-else-if="b.status === 'CONFIRMED'">
          <el-button round size="small" :loading="acting === b.id" @click="act(v, 'cancel')">取消</el-button>
          <el-button round size="small" class="cta" :disabled="b.shootDate > today" :loading="acting === b.id" @click="act(v, 'complete')">拍摄完成</el-button>
        </template>
      </template>
      <template v-else>
        <template v-if="b.status === 'PENDING' || b.status === 'CONFIRMED'">
          <el-button round size="small" :loading="acting === b.id" @click="act(v, 'cancel')">取消预约</el-button>
          <el-button round size="small" @click="$router.push(`/p/${b.photographerId}/schedule?reschedule=${b.id}`)">改期</el-button>
        </template>
        <el-button v-if="b.status === 'COMPLETED' && !b.reviewed" round size="small" class="cta" @click="emit('review', v)">去评价</el-button>
        <span v-if="b.reviewed" class="muted">已评价</span>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import CopyButton from '@/components/CopyButton.vue'
import { useBookingActions } from '@/composables/useBookingActions'
import { addHours, BOOKING_STATUS, fmtDateTime, fmtDay, hm, ymd } from '@/utils/format'
import type { BookingView } from '@/types'

/** role：当前用户在这个订单里的身份；detail：订单详情页，显示更多信息 */
const props = defineProps<{ v: BookingView; role: 'customer' | 'photographer'; detail?: boolean }>()
const emit = defineEmits<{ review: [v: BookingView] }>()
const { acting, act } = useBookingActions()
const today = ymd()

const b = computed(() => props.v.booking)
const other = computed(() => (props.role === 'customer' ? props.v.photographer : props.v.customer))

const hint = computed(() => {
  const s = b.value.status
  if (props.role === 'customer') {
    if (s === 'PENDING') return '等待摄影师确认，确认后可看到摄影师联系方式'
    if (s === 'CONFIRMED') return '摄影师已接单，请通过上面的联系方式沟通拍摄细节和付款'
    if (s === 'EXPIRED') return '摄影师没有在拍摄前确认，预约已自动关闭'
  } else {
    if (s === 'PENDING') return '确认前可以先通过客户留下的联系方式沟通'
    if (s === 'CONFIRMED' && b.value.shootDate > today) return '拍摄当天及之后可以标记完成；拍摄日 3 天后会自动完成'
  }
  return ''
})
</script>

<style lang="scss" scoped>
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
.ops { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 8px; margin-top: 12px; padding-top: 10px; border-top: 1px solid #f2f2f2; }
.ops .muted { margin-right: auto; font-size: 12px; }
.more-link:hover { color: var(--link); }
.ops .el-button + .el-button { margin-left: 0; }
</style>
