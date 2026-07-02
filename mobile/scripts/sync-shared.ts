/**
 * 代码同步脚本
 *
 * 用途：将 Web 端的 API 模块（纯 TypeScript，无 UI 依赖）同步到移动端 shared/api/ 目录，
 * 同时补全移动端缺失的 API 文件。
 *
 * 不需要复制：
 *   - agent.ts —— 移动端有独立适配版（SSE 流式请求用 uni.request 替代 EventSource）
 *   - file.ts   —— 移动端有独立适配版（文件上传用 uni.uploadFile 替代 FormData）
 *
 * 使用方式：npx tsx scripts/sync-shared.ts
 *
 * 注：API 文件的 import 路径 `../utils/request` 在 UniApp 中通过 tsconfig paths
 * 别名 `@/utils/request` 解析时会自动处理。由于 Web 端 API 文件使用相对路径
 * `../utils/request`，移动端需要将其视为 `@/utils/request`。
 * 解决方法：在移动端 tsconfig.json 中配置 baseUrl+paths，使编译时正确解析。
 *
 * 实际运行中，HBuilderX 使用自己的模块解析，可能需要手动将 API 文件中的
 * `import request from '../utils/request'` 替换为 `import request from '@/utils/request'`。
 * 本脚本处理此转换。
 */

import { readdir, readFile, writeFile, copyFile, mkdir } from 'node:fs/promises'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { existsSync } from 'node:fs'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const WEB_API = join(ROOT, '..', 'frontend', 'src', 'api')
const MOBILE_API = join(ROOT, 'src', 'api')
const WEB_STORES = join(ROOT, '..', 'frontend', 'src', 'stores')
const MOBILE_STORES = join(ROOT, 'src', 'stores')

/**
 * 需要跳过复制的文件（移动端有独立适配版）
 */
const MOBILE_OVERRIDES = new Set(['agent.ts', 'file.ts'])

/**
 * 需要跳过复制的 Store（移动端有独立适配版）
 */
const MOBILE_STORE_OVERRIDES = new Set(['user.ts', 'adminTheme.ts'])

async function syncApiFiles() {
  const files = await readdir(WEB_API)

  let count = 0
  for (const file of files) {
    if (!file.endsWith('.ts')) continue
    if (MOBILE_OVERRIDES.has(file)) {
      console.log(`  ⏭  跳过（移动端独立适配）: ${file}`)
      continue
    }

    const srcPath = join(WEB_API, file)
    const destPath = join(MOBILE_API, file)

    // 读取内容，替换 import 路径
    let content = await readFile(srcPath, 'utf-8')

    // 替换相对路径为别名路径
    content = content.replace(
      /import\s+request\s+from\s+['"]\.\.\/utils\/request['"]/g,
      "import request from '@/utils/request'",
    )
    // 也处理跨 API 文件的引用（如 user.ts import UserRole from './auth'）
    content = content.replace(
      /from\s+['"]\.\.\/api\//g,
      "from '@/api/",
    )

    await writeFile(destPath, content, 'utf-8')
    console.log(`  ✅ 同步: ${file}`)
    count++
  }

  console.log(`\n  API 同步完成: ${count} 个文件`)
}

async function syncStoreFiles() {
  const files = await readdir(WEB_STORES)

  let count = 0
  for (const file of files) {
    if (!file.endsWith('.ts')) continue
    if (MOBILE_STORE_OVERRIDES.has(file)) {
      console.log(`  ⏭  跳过（移动端独立适配）: ${file}`)
      continue
    }

    const srcPath = join(WEB_STORES, file)
    const destPath = join(MOBILE_STORES, file)

    await copyFile(srcPath, destPath)
    console.log(`  ✅ 同步 Store: ${file}`)
    count++
  }

  console.log(`\n  Store 同步完成: ${count} 个文件`)
}

// ===== 执行 =====
console.log('🔄 同步共享代码...\n')

;(async () => {
  if (!existsSync(WEB_API)) {
    console.error(`❌ Web API 目录不存在: ${WEB_API}`)
    console.log('请确保在项目根目录下执行此脚本')
    process.exit(1)
  }

  // 确保目标目录存在
  if (!existsSync(MOBILE_API)) {
    await mkdir(MOBILE_API, { recursive: true })
  }
  if (!existsSync(MOBILE_STORES)) {
    await mkdir(MOBILE_STORES, { recursive: true })
  }

  console.log('📦 API 模块:')
  await syncApiFiles()

  console.log('\n📦 Pinia Store:')
  await syncStoreFiles()

  console.log('\n✅ 同步完成！')
})()
