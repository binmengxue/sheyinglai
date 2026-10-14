<template>
  <NavBar title="订单详情" fallback="/orders" />
  <div class="page">
    <OrderCard v-if="v" :v="v" :role="role" detail @review="reviewVisible = true" />
    <el-empty v-else-if="error" :description="error"><el-button round @click="load">重试</el-button></el-empty>
    <div v-else v-loading="true" style="height: 40vh" />
    <ReviewDialog v-if="v" v-model="reviewVisible" :v="v" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import NavBar from '@/components/NavBar.vue'
import OrderCard from '@/components/OrderCard.vue'
import ReviewDialog from '@/components/ReviewDialog.vue'
import { getBooking } from '@/api/booking'
import { useUserStore } from '@/stores/user'
import { ApiError } from '@/utils/request'
import type { BookingView } from '@/types'

const props = defineProps<{ id: string }>()
const user = useUserStore()

const v = ref<BookingView | null>(null)
const error = ref('')
const reviewVisible = ref(false)
/** 当前用户在这个订单里是客户还是摄影师 */
const role = computed(() => (v.value?.booking.customerId === user.user?.id ? 'customer' : 'photographer'))

async function load() {
  v.value = null
  error.value = ''
  try {
    v.value = await getBooking(Number(props.id))
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '加载失败'
  }
}

watch(() => props.id, load, { immediate: true })
</script>

<style lang="scss" scoped>
.page { padding: 12px; }
@media (min-width: 768px) {
  .page { padding: 0; max-width: 560px; }
}
</style>
