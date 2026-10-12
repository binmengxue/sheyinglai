<template>
  <div class="pcard">
    <RouterLink :to="`/p/${c.id}`" class="main">
      <div class="left">
        <el-avatar :size="72" :src="c.avatarUrl">{{ c.nickname.slice(0, 1) }}</el-avatar>
        <div class="badge"><b>{{ badge[0] }}</b><span>{{ badge[1] }}</span></div>
      </div>
      <div class="body">
        <div class="row">
          <span class="name ellipsis">{{ c.nickname }}</span>
          <span v-if="genderMark(c.gender)" class="gender" :class="{ male: c.gender === 1 }">{{ genderMark(c.gender) }}</span>
          <span v-if="c.tag" class="tag">{{ c.tag }}</span>
          <span v-if="c.recentConsult" class="muted consult">近期{{ c.recentConsult }}人咨询</span>
          <span class="price right">¥<b>{{ c.pricePerHour }}</b><small>/时</small></span>
        </div>
        <div class="row">
          <span class="score hl">{{ c.ratingCount ? Number(c.ratingAvg).toFixed(1) : '新' }}</span>
          <span class="score-label">{{ ratingLabel(Number(c.ratingAvg), c.ratingCount) }}</span>
          <span class="deal">成交{{ c.dealCount }}单 “评价{{ c.ratingCount }}条”</span>
          <span class="muted right">{{ c.minHours }}h起拍</span>
        </div>
        <div v-if="c.refundPromise" class="promise"><el-icon><CircleCheckFilled /></el-icon> 放心约 · 不满意全额退款</div>
        <div class="row tags">
          <span v-if="c.freeKm" class="t">{{ c.freeKm }}km免交通费</span>
          <span v-if="c.rawCount" class="t">含{{ c.rawCount }}张底片{{ c.retouchCount ? `,精修${c.retouchCount}张` : '' }}</span>
          <span v-if="c.distanceKm != null" class="muted right dist"><el-icon><Location /></el-icon>{{ c.distanceKm }}km</span>
        </div>
      </div>
    </RouterLink>
    <template v-if="!compact">
      <div v-if="c.styles.length" class="good">TA擅长 “<span class="hl">{{ c.styles.slice(0, 3).join('、') }}</span>” 等类型</div>
      <div v-if="c.albums.length" class="thumbs">
        <RouterLink v-for="(a, i) in c.albums" :key="a.id" :to="`/albums/${a.id}`" class="thumb">
          <img :src="a.coverUrl" alt="" loading="lazy" />
          <span v-if="i === 0" class="kp">客片</span>
          <span class="n">{{ a.photoCount }}</span>
        </RouterLink>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { genderMark, ratingLabel, workDaysBadge } from '@/utils/format'
import type { PhotographerCard } from '@/types'

const props = defineProps<{ c: PhotographerCard; compact?: boolean }>()
const badge = computed(() => workDaysBadge(props.c.workDays))
</script>

<style lang="scss" scoped>
.pcard { background: var(--card); border-radius: var(--radius); padding: 14px 12px; }
.main { display: flex; gap: 12px; }
.left { display: flex; flex-direction: column; align-items: center; gap: 8px; flex-shrink: 0; width: 76px; }
.badge { display: flex; font-size: 11px; border: 1px solid var(--brand-strong); border-radius: 6px; overflow: hidden; }
.badge b { background: var(--brand); padding: 1px 4px; }
.badge span { padding: 1px 4px; }
.body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.row { display: flex; align-items: center; gap: 6px; min-width: 0; }
.right { margin-left: auto; flex-shrink: 0; }
.name { font-size: 17px; font-weight: 700; max-width: 7em; }
.gender { color: #ff5c8a; }
.gender.male { color: #3c7bff; }
.tag { flex-shrink: 0; font-size: 11px; color: #fff; background: linear-gradient(90deg, #3c7bff, #4fc3ff); padding: 1px 6px; border-radius: 6px; font-style: italic; font-weight: 700; }
.consult { flex-shrink: 0; font-size: 12px; }
.price { font-size: 13px; }
.price b { font-size: 20px; }
.score { font-size: 18px; font-weight: 800; }
.score { flex-shrink: 0; }
.score-label { flex-shrink: 0; font-weight: 700; font-size: 13px; white-space: nowrap; }
.deal { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; color: var(--link); background: #eef4ff; padding: 1px 6px; border-radius: 4px; }
.promise { font-size: 13px; color: var(--ok); display: flex; align-items: center; gap: 4px; }
.tags { flex-wrap: nowrap; }
.t { font-size: 12px; color: #555; background: #f4f5f7; padding: 2px 8px; border-radius: 6px; white-space: nowrap; }
.dist { display: flex; align-items: center; gap: 2px; }
.good { margin: 12px 0 8px 88px; padding-top: 10px; border-top: 1px dashed #eee; font-size: 14px; }
.thumbs { display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; margin-left: 88px; }
.thumb { position: relative; aspect-ratio: 1; border-radius: 10px; overflow: hidden; background: #eee; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.kp { position: absolute; top: 0; left: 0; font-size: 11px; font-weight: 700; background: var(--brand); padding: 1px 6px; border-radius: 0 0 8px 0; }
.n { position: absolute; right: 4px; bottom: 2px; color: #fff; font-size: 12px; text-shadow: 0 0 3px rgba(0, 0, 0, 0.6); }
@media (max-width: 480px) {
  .good, .thumbs { margin-left: 0; }
  .consult { display: none; }
}
</style>
