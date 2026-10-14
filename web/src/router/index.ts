import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/message'
import { setTitle } from '@/utils/title'

declare module 'vue-router' {
  interface RouteMeta {
    /** 显示底部导航，值为高亮的 tab */
    tab?: 'home' | 'orders' | 'me'
    /** 需要登录 */
    auth?: boolean
    /** 需要管理员 */
    admin?: boolean
    /** PC 端用工作台布局（左侧菜单） */
    studio?: boolean
    title?: string
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { tab: 'home' } },
    { path: '/p/:id', name: 'photographer', component: () => import('@/views/PhotographerView.vue'), props: true },
    { path: '/p/:id/schedule', name: 'schedule', component: () => import('@/views/ScheduleView.vue'), props: true, meta: { title: '达人档期' } },
    { path: '/albums/:id', name: 'album', component: () => import('@/views/AlbumDetailView.vue'), props: true },
    { path: '/orders/:id(\\d+)', name: 'order', component: () => import('@/views/OrderDetailView.vue'), props: true, meta: { auth: true, studio: true, title: '订单详情' } },
    { path: '/p/:id/follows', name: 'follows', component: () => import('@/views/FollowsView.vue'), props: true, meta: { title: '关注和粉丝' } },
    { path: '/orders', name: 'orders', component: () => import('@/views/OrdersView.vue'), meta: { tab: 'orders', auth: true, studio: true, title: '我的订单' } },
    { path: '/me', name: 'me', component: () => import('@/views/MeView.vue'), meta: { tab: 'me', auth: true, studio: true, title: '个人中心' } },
    { path: '/messages', name: 'messages', component: () => import('@/views/MessagesView.vue'), meta: { auth: true, studio: true, title: '消息' } },
    { path: '/me/edit', name: 'profile-edit', component: () => import('@/views/ProfileEditView.vue'), meta: { auth: true, studio: true, title: '编辑资料' } },
    { path: '/studio/profile', name: 'studio-profile', component: () => import('@/views/StudioProfileView.vue'), meta: { auth: true, studio: true, title: '接单设置' } },
    { path: '/studio/albums', name: 'studio-albums', component: () => import('@/views/StudioAlbumsView.vue'), meta: { auth: true, studio: true, title: '我的客片' } },
    { path: '/studio/stats', name: 'studio-stats', component: () => import('@/views/StatsView.vue'), meta: { auth: true, studio: true, title: '经营数据' } },
    { path: '/admin', name: 'admin', component: () => import('@/views/AdminView.vue'), meta: { auth: true, admin: true, studio: true, title: '管理后台' } },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
    { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { title: '注册' } },
    { path: '/agreement', name: 'agreement', component: () => import('@/views/AgreementView.vue'), props: { type: 'agreement' }, meta: { title: '用户协议' } },
    { path: '/privacy', name: 'privacy', component: () => import('@/views/AgreementView.vue'), props: { type: 'privacy' }, meta: { title: '隐私政策' } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: (_to, _from, saved) => saved ?? { top: 0 },
})

router.beforeEach(async (to) => {
  const user = useUserStore()
  await user.ensure()
  if (to.meta.auth && !user.loggedIn) {
    toast.info('请先登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && !user.isAdmin) {
    toast.warning('无权访问该页面')
    return '/'
  }
})

router.afterEach((to) => setTitle(to.meta.title))

export default router
