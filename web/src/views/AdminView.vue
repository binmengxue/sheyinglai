<template>
  <NavBar fallback="/me" />
  <div class="page">
    <el-input v-model="keyword" placeholder="搜索手机号或昵称" clearable :prefix-icon="'Search'" @change="load(true)" />
    <p class="muted tip">入驻不需要审核，发现违规内容时可以封禁账号、从首页隐藏摄影师，或进入主页删除客片。</p>

    <!-- PC：网盘式表格 -->
    <div v-if="isDesktop" class="card table-card">
      <el-table v-loading="loading" :data="list" row-key="user.id" empty-text="没有找到用户">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.user.avatarUrl">{{ row.user.nickname.slice(0, 1) }}</el-avatar>
              <div>
                <div class="name">{{ row.user.nickname }}</div>
                <div class="muted">UID {{ row.user.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" prop="user.phone" width="130" />
        <el-table-column label="注册时间" width="150">
          <template #default="{ row }">{{ fmtDateTime(row.user.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="接单信息" min-width="180">
          <template #default="{ row }">
            <span v-if="row.photographer?.pricePerHour">¥{{ row.photographer.pricePerHour }}/时 · 成交{{ row.photographer.dealCount }} · 评价{{ row.photographer.ratingCount }}</span>
            <span v-else class="muted">未设置接单</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.user.role === 'ADMIN'" size="small">管理员</el-tag>
            <el-tag v-if="row.user.status === 'BANNED'" size="small" type="danger">已封禁</el-tag>
            <el-tag v-if="row.photographer && !row.photographer.visible" size="small" type="warning">已隐藏</el-tag>
            <el-tag v-if="row.user.status === 'NORMAL' && row.user.role !== 'ADMIN' && row.photographer?.visible !== false" size="small" type="success">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link @click="$router.push(`/p/${row.user.id}`)">主页</el-button>
            <template v-if="row.user.role !== 'ADMIN'">
              <el-button v-if="row.photographer" link @click="toggleVisible(row as AdminUserView)">{{ row.photographer.visible ? '隐藏' : '取消隐藏' }}</el-button>
              <el-button link :type="row.user.status === 'BANNED' ? 'primary' : 'danger'" @click="toggleBan(row as AdminUserView)">
                {{ row.user.status === 'BANNED' ? '解封' : '封禁' }}
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 手机：卡片 -->
    <template v-else>
    <div v-for="v in list" :key="v.user.id" class="card row">
      <el-avatar :size="44" :src="v.user.avatarUrl">{{ v.user.nickname.slice(0, 1) }}</el-avatar>
      <div class="info">
        <div class="name">
          {{ v.user.nickname }}
          <el-tag v-if="v.user.role === 'ADMIN'" size="small">管理员</el-tag>
          <el-tag v-if="v.user.status === 'BANNED'" size="small" type="danger">已封禁</el-tag>
          <el-tag v-if="v.photographer && !v.photographer.visible" size="small" type="warning">已隐藏</el-tag>
        </div>
        <div class="muted">UID {{ v.user.id }} · {{ v.user.phone }} · {{ fmtDateTime(v.user.createdAt) }} 注册</div>
        <div class="muted">
          {{ v.photographer?.pricePerHour ? `¥${v.photographer.pricePerHour}/时 · 成交${v.photographer.dealCount} · 评价${v.photographer.ratingCount}` : '未设置接单' }}
        </div>
        <div class="ops">
          <el-button link @click="$router.push(`/p/${v.user.id}`)">主页</el-button>
          <template v-if="v.user.role !== 'ADMIN'">
            <el-button v-if="v.photographer" link @click="toggleVisible(v)">{{ v.photographer.visible ? '隐藏' : '取消隐藏' }}</el-button>
            <el-button link :type="v.user.status === 'BANNED' ? 'primary' : 'danger'" @click="toggleBan(v)">
              {{ v.user.status === 'BANNED' ? '解封' : '封禁' }}
            </el-button>
          </template>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && !list.length" description="没有找到用户" />
    </template>
    <div v-if="list.length < total" class="more"><el-button round :loading="loading" @click="load()">加载更多</el-button></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import { adminUsers, setBanned, setPhotographerVisible } from '@/api/admin'
import { fmtDateTime } from '@/utils/format'
import { useDevice } from '@/composables/useDevice'
import type { AdminUserView } from '@/types'

const { isDesktop } = useDevice()

const keyword = ref('')
const list = ref<AdminUserView[]>([])
const total = ref(0)
const loading = ref(false)
let page = 0

async function load(reset = false) {
  if (reset) {
    list.value = []
    page = 0
  }
  loading.value = true
  try {
    const res = await adminUsers(keyword.value, page + 1)
    list.value.push(...res.records)
    total.value = res.total
    page++
  } finally {
    loading.value = false
  }
}

async function toggleBan(v: AdminUserView) {
  const ban = v.user.status !== 'BANNED'
  const ok = await ElMessageBox.confirm(ban ? `封禁「${v.user.nickname}」？封禁后无法登录，主页和客片对外隐藏` : `解封「${v.user.nickname}」？`, '确认', { type: 'warning' })
    .catch(() => false)
  if (!ok) return
  await setBanned(v.user.id, ban)
  v.user.status = ban ? 'BANNED' : 'NORMAL'
  ElMessage.success('已更新')
}

async function toggleVisible(v: AdminUserView) {
  const p = v.photographer!
  await setPhotographerVisible(v.user.id, !p.visible)
  p.visible = !p.visible
  ElMessage.success(p.visible ? '已在首页恢复展示' : '已从首页隐藏')
}

onMounted(() => load(true))
</script>

<style lang="scss" scoped>
.tip { margin: 10px 0; }
.row { display: flex; gap: 12px; margin-bottom: 10px; }
.info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.name { display: flex; align-items: center; gap: 6px; font-weight: 700; }
.ops { display: flex; gap: 4px; }
.more { text-align: center; padding: 12px; }
.table-card { padding: 4px 8px; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.el-tag + .el-tag { margin-left: 4px; }
@media (min-width: 768px) {
  .page > .el-input { max-width: 360px; }
}
</style>
