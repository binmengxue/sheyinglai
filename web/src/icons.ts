import type { App } from 'vue'
import {
  ArrowDown, ArrowLeft, ArrowRight, Bell, Calendar, Camera, ChatDotRound, Check, CircleCheckFilled, CopyDocument, DataLine, Delete,
  Discount, Edit, Grid, House, Iphone, Location, Lock, Management, Picture, Plus, Search, Setting, Star, StarFilled,
  SwitchButton, Tickets, Upload, UploadFilled, User, Warning,
} from '@element-plus/icons-vue'

/**
 * 只注册用到的图标（全量注册会把 300 多个图标都打进首屏包）。
 * 模板里大量用字符串写图标名（:icon="'Edit'"、<component :is="m.icon">），所以需要全局注册；
 * 新用到的图标要加到这里。
 */
const icons = {
  ArrowDown, ArrowLeft, ArrowRight, Bell, Calendar, Camera, ChatDotRound, Check, CircleCheckFilled, CopyDocument, DataLine, Delete,
  Discount, Edit, Grid, House, Iphone, Location, Lock, Management, Picture, Plus, Search, Setting, Star, StarFilled,
  SwitchButton, Tickets, Upload, UploadFilled, User, Warning,
}

export function registerIcons(app: App) {
  for (const [name, comp] of Object.entries(icons)) app.component(name, comp)
}
