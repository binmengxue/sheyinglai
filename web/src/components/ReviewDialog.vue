<template>
  <el-dialog v-model="visible" title="评价摄影师" width="400px" align-center>
    <div class="rate"><el-rate v-model="score" size="large" show-text :texts="['很差', '较差', '一般', '满意', '超棒']" /></div>
    <el-input v-model="content" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="说说这次拍摄的体验吧" />
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button class="cta" :loading="saving" @click="submit">提交评价</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { reviewBooking } from '@/api/booking'
import { toast } from '@/utils/message'
import type { BookingView } from '@/types'

const props = defineProps<{ v?: BookingView }>()
const visible = defineModel<boolean>({ required: true })

const score = ref(5)
const content = ref('')
const saving = ref(false)

watch(visible, (open) => {
  if (!open) return
  score.value = 5
  content.value = ''
})

async function submit() {
  if (!props.v) return
  saving.value = true
  try {
    await reviewBooking(props.v.booking.id, score.value, content.value || undefined)
    props.v.booking.reviewed = true
    visible.value = false
    toast.success('感谢你的评价')
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.rate { display: flex; justify-content: center; margin-bottom: 12px; }
</style>
