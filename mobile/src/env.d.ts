/// <reference types="@dcloudio/types" />

/**
 * UniApp 类型补充声明
 *
 * 与 Web 端 typeRoots 的差异在于：
 *   - 不存在 document / window / HTMLDivElement 等浏览器 DOM 类型
 *   - 使用 UniApp 内置的 @dcloudio/types
 */

declare module '*.vue' {
  import { ComponentOptions } from 'vue'
  const component: ComponentOptions
  export default component
}

/* 环境变量 */
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_WS_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

/* 模块路径别名 */
declare module '@/*' {
  const _: unknown
  export default _
}
