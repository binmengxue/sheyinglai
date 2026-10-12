<template>
  <div class="stats-page">
    <NavBar dark fallback="/me" title="" class="mobile-only" />
    <h1>经营数据</h1>
    <div v-if="s" class="page">
      <div class="card">
        <h2 class="section-title">重要数据</h2>
        <div class="grid four">
          <div><span>订单均价</span><b>{{ money(s.avgPrice) }}</b></div>
          <div><span>订单最高价</span><b>{{ money(s.maxPrice) }}</b></div>
          <div><span>成交率</span><b>{{ pct(s.dealRate) }}</b></div>
          <div><span>接单率</span><b>{{ pct(s.acceptRate) }}</b></div>
          <div><span>取消率</span><b>{{ pct(s.cancelRate) }}</b></div>
          <div><span>回复时长</span><b>{{ reply(s.avgReplyMinutes) }}</b></div>
        </div>
      </div>

      <div class="card">
        <h2 class="section-title">昨日核心数据</h2>
        <div class="grid three">
          <div v-for="m in yesterday" :key="m.label">
            <span>{{ m.label }}</span>
            <b>{{ m.v.value }}</b>
            <ul class="changes">
              <li>日 <i :class="cls(m.v.dayChange)">{{ chg(m.v.dayChange) }}</i></li>
              <li>周 <i :class="cls(m.v.weekChange)">{{ chg(m.v.weekChange) }}</i></li>
              <li>月 <i :class="cls(m.v.monthChange)">{{ chg(m.v.monthChange) }}</i></li>
            </ul>
          </div>
        </div>
      </div>

      <div class="card">
        <h2 class="section-title">30天核心数据</h2>
        <div class="grid three">
          <div><span>曝光量</span><b>{{ s.exposure30 }}</b></div>
          <div><span>主页浏览量</span><b>{{ s.profileView30 }}</b></div>
          <div><span>咨询量</span><b>{{ s.consult30 }}</b></div>
        </div>
      </div>
      <p class="muted note">曝光：出现在首页列表的次数；咨询：收到的预约数。成交率 = 已完成 / 全部预约，接单率 = 确认 / (确认 + 拒绝)。</p>
    </div>
    <div v-else v-loading="true" style="height: 50vh" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import NavBar from '@/components/NavBar.vue'
import { myStats } from '@/api/booking'
import type { StatsView } from '@/types'

const s = ref<StatsView | null>(null)
const yesterday = computed(() => s.value ? [
  { label: '曝光量', v: s.value.exposure },
  { label: '主页浏览量', v: s.value.profileView },
  { label: '咨询量', v: s.value.consult },
] : [])

const money = (v?: number) => (v == null ? '-' : `¥${v}`)
const pct = (v?: number) => (v == null ? '-' : `${v}%`)
const chg = (v?: number) => (v == null ? '-' : `${v > 0 ? '+' : ''}${v}%`)
const cls = (v?: number) => (v == null || v === 0 ? '' : v > 0 ? 'up' : 'down')
function reply(m?: number) {
  if (m == null) return '-'
  if (m < 60) return `${Math.max(1, m)}分钟`
  if (m < 60 * 24) return `${Math.round(m / 60)}小时`
  return `${Math.round(m / 60 / 24)}天`
}

onMounted(async () => { s.value = await myStats() })
</script>

<style lang="scss" scoped>
.stats-page { min-height: 100vh; background: linear-gradient(#111 0, #111 190px, var(--bg) 190px); }
h1 { margin: 4px 20px 16px; color: #fff; font-size: 26px; }
.page { padding-top: 0; }
.grid { display: grid; gap: 18px 8px; text-align: center; }
.grid.four { grid-template-columns: repeat(4, 1fr); }
.grid.three { grid-template-columns: repeat(3, 1fr); }
.grid > div { display: flex; flex-direction: column; gap: 10px; }
.grid span { font-size: 13px; color: #555; }
.grid b { font-size: 20px; font-weight: 500; }
.changes { list-style: none; margin: 0; padding: 0; font-size: 12px; color: #555; line-height: 1.8; }
.changes i { font-style: normal; }
.up { color: #e5483d; }
.down { color: var(--ok); }
.note { padding: 0 4px; line-height: 1.6; }
@media (max-width: 420px) { .grid.four { grid-template-columns: repeat(3, 1fr); } }

/* PC 工作台：普通标题，不要黑色顶部背景 */
@media (min-width: 768px) {
  .stats-page { min-height: 0; background: none; max-width: 880px; }
  h1 { margin: 0 0 16px; color: var(--text); font-size: 22px; }
  .grid.four { grid-template-columns: repeat(6, 1fr); }
}
</style>
