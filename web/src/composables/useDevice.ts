import { ref } from 'vue'

/** PC 断点，和 style.css 里的 @media (min-width: 768px) 保持一致 */
const query = window.matchMedia('(min-width: 768px)')
const isDesktop = ref(query.matches)
query.addEventListener('change', (e) => (isDesktop.value = e.matches))

/** 是否 PC 布局（全局共享一个监听） */
export function useDevice() {
  return { isDesktop }
}
