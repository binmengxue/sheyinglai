import { computed, ref, type Ref } from 'vue'
import type { PageResult } from '@/types'

/**
 * 分页列表：加载、加载更多、出错、是否还有更多。
 * 切换筛选条件后 load(true) 重新加载；之前还没返回的请求结果会被丢弃，不会混进新列表。
 */
export function usePagedList<T>(fetchPage: (page: number) => Promise<PageResult<T>>) {
  const list = ref([]) as Ref<T[]>
  const total = ref(0)
  const loading = ref(false)
  const error = ref(false)
  const hasMore = computed(() => list.value.length < total.value)
  let page = 0
  /** 每次请求的序号，只认最新一次的结果 */
  let seq = 0

  async function load(reset = false) {
    if (reset) {
      list.value = []
      total.value = 0
      page = 0
    } else if (loading.value) {
      return
    }
    const mine = ++seq
    loading.value = true
    error.value = false
    try {
      const res = await fetchPage(page + 1)
      if (mine !== seq) return
      list.value.push(...res.records)
      total.value = res.total
      page++
    } catch {
      if (mine === seq) error.value = true
    } finally {
      if (mine === seq) loading.value = false
    }
  }

  return { list, total, loading, error, hasMore, load }
}
