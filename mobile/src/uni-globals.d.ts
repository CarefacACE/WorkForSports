/// <reference types="@dcloudio/types" />

/**
 * UniApp 全局类型补充
 *
 * UniApp 运行时环境提供了以下全局 API（不同于浏览器 DOM）：
 *   console / setTimeout / setInterval / clearInterval / Date / JSON 等
 *
 * 本次声明补充了 TypeScript lib 中缺少但 UniApp 实际可用的全局类型。
 */

/* UniApp 内置可用全局对象 */
declare var console: {
  log(...args: unknown[]): void
  warn(...args: unknown[]): void
  error(...args: unknown[]): void
  info(...args: unknown[]): void
}

declare function setTimeout(fn: () => void, ms: number): ReturnType<typeof setInterval>
declare function setInterval(fn: () => void, ms: number): ReturnType<typeof setInterval>
declare function clearInterval(id: unknown): void

/* 补充 ES2020+ 浏览器 API（UniApp 运行时提供了 polyfill） */
declare var URL: typeof globalThis extends { URL: infer T } ? T : never
