<template>
  <!-- 手机从底部弹出，PC 从右侧弹出 -->
  <el-drawer v-model="visible" :direction="isDesktop ? 'rtl' : 'btt'" :size="isDesktop ? '420px' : '82%'" title="收费详情" class="price-drawer">
    <section class="block">
      <h3>基础费用</h3>
      <p>
        {{ p.pricePerHour }}元/小时<template v-if="p.extraPersonPrice">，加人：{{ p.extraPersonPrice }}元/人/小时</template>，{{ p.minHours }}小时起订
      </p>
      <p v-if="p.freeKm">{{ p.freeKm }}km内免交通费<template v-if="p.kmFee">，超出{{ p.kmFee }}元/公里</template></p>
    </section>
    <section class="block">
      <h3>妆造说明</h3>
      <p>{{ p.makeupNote || '不提供妆造' }}</p>
    </section>
    <section class="block">
      <h3>精修说明</h3>
      <p>
        <template v-if="p.rawCount">每小时最少{{ p.rawCount }}张底片，</template>免费精修{{ p.retouchCount }}张<template v-if="p.retouchExtraPrice">，超出¥{{ p.retouchExtraPrice }}/张</template>
      </p>
      <p v-if="p.retouchRule">{{ p.retouchRule }}</p>
    </section>
    <section v-if="p.equipment" class="block">
      <h3>拍摄设备</h3>
      <p>{{ p.equipment }}</p>
    </section>
    <section class="block">
      <h3>爽约规则</h3>
      <p>
        <span class="policy" :class="p.cancelPolicy">{{ CANCEL_POLICY[p.cancelPolicy].label }}</span>
        {{ CANCEL_POLICY[p.cancelPolicy].desc(p.penaltyPercent) }}
      </p>
      <p class="muted">平台只提供预约，费用和违约金由双方线下结算</p>
    </section>
    <section v-if="p.refundPromise" class="block">
      <h3>服务承诺</h3>
      <p class="promise"><el-icon><CircleCheckFilled /></el-icon> 已承诺不满意全额退款</p>
    </section>
  </el-drawer>
</template>

<script setup lang="ts">
import { CANCEL_POLICY } from '@/utils/format'
import { useDevice } from '@/composables/useDevice'
import type { Photographer } from '@/types'

defineProps<{ p: Photographer }>()
const { isDesktop } = useDevice()
const visible = defineModel<boolean>({ required: true })
</script>

<style lang="scss" scoped>
.block { background: #f6f7f9; border-radius: 12px; padding: 14px 16px; margin-bottom: 12px; }
h3 { margin: 0 0 8px; font-size: 16px; }
p { margin: 4px 0; color: #666; line-height: 1.7; font-size: 14px; }
.policy { display: inline-block; font-size: 12px; padding: 1px 8px; border-radius: 6px; margin-right: 6px; background: #eee; color: #333; }
.policy.STRICT { background: #111; color: var(--brand); }
.policy.LOOSE { background: #e7f8f0; color: var(--ok); }
.promise { color: var(--ok); display: flex; align-items: center; gap: 4px; }
</style>
