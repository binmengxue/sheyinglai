<template>
  <div class="reports">
    <div class="chips">
      <button v-for="s in STATUSES" :key="s.key ?? 'all'" class="chip" :class="{ active: status === s.key }" @click="setStatus(s.key)">
        {{ s.label }}
      </button>
    </div>

    <div v-for="v in list" :key="v.report.id" class="card item">
      <div class="head">
        <el-tag size="small" effect="plain">{{ TARGET_LABEL[v.report.targetType] }}</el-tag>
        <RouterLink v-if="v.target" :to="v.target.link" class="summary ellipsis">{{ v.target.summary }}</RouterLink>
        <span v-else class="muted">内容已被删除</span>
        <el-tag v-if="v.report.status !== 'PENDING'" size="small" :type="v.report.status === 'HANDLED' ? 'success' : 'info'">
          {{ v.report.status === 'HANDLED' ? '已处理' : '不处理' }}
        </el-tag>
      </div>
      <div class="reason"><b>{{ v.report.reason }}</b><span v-if="v.report.content">：{{ v.report.content }}</span></div>
      <div class="muted meta">{{ v.reporter.nickname }} 举报于 {{ fmtDateTime(v.report.createdAt) }}</div>
      <div v-if="v.report.status === 'PENDING'" class="ops">
        <template v-if="v.target">
          <el-button v-if="v.report.targetType === 'REVIEW'" size="small" round type="danger" plain @click="act(v, 'deleteReview')">删除评价</el-button>
          <el-button v-if="v.report.targetType === 'ALBUM'" size="small" round type="danger" plain @click="act(v, 'deleteAlbum')">删除客片</el-button>
          <template v-if="v.report.targetType === 'PHOTOGRAPHER'">
            <el-button size="small" round plain @click="act(v, 'hide')">从首页隐藏</el-button>
            <el-button size="small" round type="danger" plain @click="act(v, 'ban')">封禁账号</el-button>
          </template>
        </template>
        <el-button v-else size="small" round @click="act(v, 'done')">标记已处理</el-button>
        <el-button size="small" round @click="act(v, 'ignore')">不处理</el-button>
      </div>
    </div>

    <div v-if="loading" v-loading="true" style="height: 100px" />
    <el-empty v-else-if="error && !list.length" description="加载失败"><el-button round @click="load(true)">重试</el-button></el-empty>
    <el-empty v-else-if="!list.length" description="没有举报" />
    <div v-else-if="list.length < total" class="more"><el-button round @click="load()">加载更多</el-button></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { deleteReview, handleReport, listReports, setBanned, setPhotographerVisible } from '@/api/admin'
import { deleteAlbum } from '@/api/album'
import { usePagedList } from '@/composables/usePagedList'
import { fmtDateTime } from '@/utils/format'
import { confirm, singleFlight, toast } from '@/utils/message'
import type { ReportStatus, ReportTargetType, ReportView } from '@/types'

const TARGET_LABEL: Record<ReportTargetType, string> = { PHOTOGRAPHER: '摄影师', REVIEW: '评价', ALBUM: '客片' }
const STATUSES: { key?: ReportStatus; label: string }[] = [
  { key: 'PENDING', label: '待处理' },
  { key: 'HANDLED', label: '已处理' },
  { key: 'IGNORED', label: '不处理' },
  { key: undefined, label: '全部' },
]

const status = ref<ReportStatus | undefined>('PENDING')
const { list, total, loading, error, load } = usePagedList<ReportView>((page) => listReports(status.value, page))

function setStatus(s?: ReportStatus) {
  status.value = s
  load(true)
}

type Action = 'deleteReview' | 'deleteAlbum' | 'hide' | 'ban' | 'done' | 'ignore'
const CONFIRM: Partial<Record<Action, string>> = {
  deleteReview: '删除这条评价？',
  deleteAlbum: '删除这组客片和其中所有照片？此操作不可恢复',
  hide: '把这个摄影师从首页隐藏？',
  ban: '封禁这个账号？封禁后无法登录，主页和客片对外隐藏',
}

/** 处理举报：先执行对应操作，再把举报标记为已处理（不处理则直接标记），举报人会收到通知 */
const act = singleFlight(async (v: ReportView, action: Action) => {
  const text = CONFIRM[action]
  if (text && !(await confirm(text))) return
  const id = v.report.targetId
  if (action === 'deleteReview') await deleteReview(id)
  else if (action === 'deleteAlbum') await deleteAlbum(id)
  else if (action === 'hide') await setPhotographerVisible(id, false)
  else if (action === 'ban') await setBanned(id, true)
  const result = action === 'ignore' ? 'IGNORED' : 'HANDLED'
  await handleReport(v.report.id, result)
  v.report.status = result
  toast.success(result === 'HANDLED' ? '已处理，举报人会收到通知' : '已标记为不处理')
  if (status.value === 'PENDING') list.value = list.value.filter((x) => x !== v)
})

onMounted(() => load(true))
</script>

<style lang="scss" scoped>
.chips { margin-bottom: 12px; }
.item { margin-bottom: 10px; }
.head { display: flex; align-items: center; gap: 8px; }
.summary { flex: 1; min-width: 0; color: var(--link); font-weight: 600; }
.reason { margin: 8px 0 4px; font-size: 14px; line-height: 1.6; word-break: break-all; }
.meta { font-size: 12px; }
.ops { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; margin-top: 10px; }
.ops .el-button + .el-button { margin-left: 0; }
.more { text-align: center; padding: 12px; }
</style>
