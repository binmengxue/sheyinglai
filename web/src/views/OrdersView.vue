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
      <OrderCard v-for="v in list" :key="v.booking.id" :v="v" :role="role" class="order" @review="openReview" />
    </div>
    <div v-if="loading" v-loading="true" style="height: 120px" />
    <el-empty v-else-if="error && !list.length" description="加载失败"><el-button round @click="load(true)">重试</el-button></el-empty>
    <el-empty v-else-if="!list.length" :description="role === 'customer' ? '还没有预约，去首页找个摄影师吧' : '还没有人约你，完善资料和客片会更容易被约哦'" />
    <div v-else-if="list.length < total" class="more"><el-button round @click="load()">加载更多</el-button></div>

    <ReviewDialog v-model="reviewVisible" :v="reviewing" />
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import OrderCard from '@/components/OrderCard.vue'
import ReviewDialog from '@/components/ReviewDialog.vue'
import { listBookings } from '@/api/booking'
import { usePagedList } from '@/composables/usePagedList'
import { BOOKING_STATUS } from '@/utils/format'
import type { BookingStatus, BookingView } from '@/types'

type Role = 'customer' | 'photographer'
const PAGE_SIZE = 20
const route = useRoute()
const router = useRouter()

const role = ref<Role>(route.query.role === 'photographer' ? 'photographer' : 'customer')
const status = ref<BookingStatus>()
/** 上次加载时间：从别的页面返回时，超过 30 秒才刷新 */
let loadedAt = 0

const { list, total, loading, error, load: loadPage } = usePagedList<BookingView>((page) =>
  listBookings({ role: role.value, status: status.value, page, size: PAGE_SIZE }))

function load(reset = false) {
  if (reset) loadedAt = Date.now()
  return loadPage(reset)
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

const reviewVisible = ref(false)
const reviewing = ref<BookingView>()

function openReview(v: BookingView) {
  reviewing.value = v
  reviewVisible.value = true
}

onMounted(() => load(true))
onActivated(() => {
  if (Date.now() - loadedAt > 30_000) load(true)
})

// 带 ?role=xxx 进入时已在订单页（页面被缓存），也要切到对应 tab；离开订单页时不处理
watch(() => route.query.role, (r) => {
  if (route.name !== 'orders') return
  const next: Role = r === 'photographer' ? 'photographer' : 'customer'
  if (next === role.value) return
  role.value = next
  load(true)
})
</script>

<style lang="scss" scoped>
.top { position: sticky; top: 0; z-index: 5; padding: 16px 12px 10px; background: var(--bg); }
h1 { margin: 0 0 12px; font-size: 24px; }
.roles { display: flex; gap: 24px; margin-bottom: 10px; }
.roles button { border: none; background: none; padding: 4px 0; font-size: 16px; color: #888; cursor: pointer; }
.roles button.active { color: var(--text); font-weight: 700; box-shadow: inset 0 -4px var(--brand); }
.list { padding: 0 12px; }
.order { margin-bottom: 12px; }
.more { text-align: center; padding: 12px; }

/* PC 工作台：标题和筛选不吸顶，订单两列 */
@media (min-width: 768px) {
  .top { position: static; padding: 0 0 12px; }
  h1 { font-size: 22px; margin-bottom: 16px; }
  .list { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 14px; padding: 0; align-items: start; }
  .order { margin: 0; }
}
</style>
