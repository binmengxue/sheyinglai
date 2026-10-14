import { readdirSync, readFileSync, existsSync } from 'node:fs'
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'

/**
 * 扫描 element-plus/es/components/<dir>/index.mjs 的导出，得到「导出名 → 目录」的映射，
 * 例如 ElDropdownItem → dropdown、vLoading → loading。
 */
export function elementPlusEntries(): Map<string, string> {
  const require = createRequire(import.meta.url)
  const root = join(dirname(require.resolve('element-plus/package.json')), 'es', 'components')
  const map = new Map<string, string>()
  for (const dir of readdirSync(root)) {
    const file = join(root, dir, 'index.mjs')
    if (!existsSync(file)) continue
    for (const m of readFileSync(file, 'utf8').matchAll(/export\s*\{([^}]*)\}/g)) {
      for (const part of m[1].split(',')) {
        // "vLoading as ElLoadingDirective" 取 as 后面的名字
        const name = part.trim().split(/\s+as\s+/).pop()
        if (name && !map.has(name)) map.set(name, dir)
      }
    }
  }
  return map
}
