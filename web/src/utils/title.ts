/** 设置浏览器标题；详情页加载完数据后用名称覆盖路由里的默认标题 */
export function setTitle(title?: string) {
  document.title = title ? `${title} · 摄影来` : '摄影来 · 约拍摄影师'
}
