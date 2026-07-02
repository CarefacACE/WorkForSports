# HBuilderX 安装 & 鸿蒙验证指南

## 适用项目

`D:\Project\Github\WorkForSports\mobile\`（UniApp Vue 3 项目）

---

## 一、安装 HBuilderX

### 1.1 下载

访问 https://www.dcloud.io/hbuilderx.html ，选择 **Windows 版**（推荐 `正式版` 而非 Alpha）：

```
https://qiniu-ecdn.dcloud.net.cn/download/HBuilderX.zip
```

### 1.2 安装

解压到任意目录，例如：

```
D:\HBuilderX\
```

直接运行 `D:\HBuilderX\HBuilderX.exe` 即可，不需要安装程序。

> ⚠️ 不要放在中文路径下，不要放在需要管理员权限的目录（如 `C:\Program Files\`）。

---

## 二、配置鸿蒙编译环境

### 2.1 安装 HarmonyOS 运行插件

1. 打开 HBuilderX
2. 菜单栏 → **工具** → **插件安装**
3. 搜索并安装以下插件：
   - **HarmonyOS 运行插件**（核心——编译生成鸿蒙 `.app` 包）
   - **uni-app 编译插件**（如果缺失）

### 2.2 配置 DevEco Studio（用于签名和真机调试）

HBuilderX 编译鸿蒙包时，需要借用 DevEco Studio 的签名工具链：

1. 下载 DevEco Studio（只需基础版，不需要完整 IDE）：
   ```
   https://developer.huawei.com/consumer/cn/download/
   ```

2. 安装后，在 HBuilderX 中配置路径：
   - 菜单栏 → **运行** → **运行到鸿蒙** → **鸿蒙模拟器配置**
   - 填入 DevEco Studio 安装路径

### 2.3 配置华为开发者签名

需要在 HBuilderX 的 `manifest.json` 中配置签名证书（用于真机安装）：

1. 登录 [华为 AppGallery Connect](https://developer.huawei.com/consumer/cn/service/josp/agc/index.html)
2. 创建应用 → 获取 `bundleName` 和签名证书
3. 在 HBuilderX 中：`manifest.json` → **鸿蒙配置** → 填入 bundleName 和签名信息

> 模拟器调试不需要签名，可直接运行。

---

## 三、打开项目

1. 启动 HBuilderX
2. 菜单栏 → **文件** → **打开目录**
3. 选择：`D:\Project\Github\WorkForSports\mobile`

HBuilderX 会自动识别项目类型（UniApp Vue 3），并在左侧显示项目文件树。

---

## 四、修改 API 地址（关键步骤）

在运行前，需要确认移动端的 API 指向正确的后端地址。

打开 `src/utils/request.ts` 第 33 行：

```typescript
const BASE_URL = 'http://192.168.1.100:8080/api'
```

根据你的实际情况修改：

| 调试方式 | BASE_URL 值 |
|----------|-------------|
| HBuilderX 内置浏览器预览 | `http://localhost:8080/api` |
| 鸿蒙模拟器 | `http://10.0.2.2:8080/api` 或你的电脑局域网 IP |
| 鸿蒙真机 | 电脑的局域网 IP，如 `http://192.168.1.100:8080/api` |

> ⚠️ 模拟器和真机无法访问 `localhost`，需要用电脑的局域网 IP。确保手机/模拟器和后端在同一网络。

---

## 五、启动后端

在运行移动端之前，先启动后端：

```bash
cd D:\Project\Github\WorkForSports\backend
mvn spring-boot:run
```

或直接运行 JAR 包：

```bash
java -jar target/zhixun-erp-backend-0.0.1-SNAPSHOT.jar
```

确认后端启动成功：
- 访问 `http://localhost:8080/api` 应该返回 JSON 响应（不是 404）

---

## 六、运行到鸿蒙

### 6.1 鸿蒙模拟器

1. HBuilderX 菜单栏 → **运行** → **运行到鸿蒙** → **启动鸿蒙模拟器**
2. 等待模拟器启动（首次需要 2-5 分钟）
3. 再次点击 → **运行** → **运行到鸿蒙** → **运行到鸿蒙模拟器**
4. 等待编译完成（首次 1-3 分钟），应用会自动安装到模拟器并启动

### 6.2 浏览器预览（快速调试）

如果暂时没有鸿蒙环境，可以先在浏览器预览里验证逻辑：

1. HBuilderX 菜单栏 → **运行** → **运行到浏览器** → **Chrome**
2. 会在浏览器中打开，模拟移动端 UI

> 浏览器预览不支持 `uni.storage`、WebSocket 等原生 API，主要用于快速验证 UI 和 HTTP 请求逻辑。

---

## 七、验证流程

按以下步骤验证：

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 应用启动 | 看到登录页，蓝色渐变背景 + 会员/教练/管理角色切换 |
| 2 | 选择角色 → 输入用户名密码 → 登录 | 跳转到首页工作台，显示用户名和问候语 |
| 3 | 点击底部「课程」Tab | 看到公共课程列表（或空状态提示） |
| 4 | 点击底部「课表」Tab | 看到课表页面 |
| 5 | 点击底部「聊天」Tab | 看到会话列表 |
| 6 | 点击底部「我的」Tab → 编辑资料 | 看到资料编辑表单 |
| 7 | 「我的」→ 退出登录 | 返回登录页 |

---

## 八、常见问题

### Q: HBuilderX 提示 "项目类型无法识别"
**A**: 检查 `manifest.json`、`pages.json`、`App.vue` 三个文件是否存在。缺一不可。

### Q: 编译时报 "模块 xxx 未找到"
**A**: 在 HBuilderX 终端中运行 `npm install`，或点击菜单栏 **运行** → **npm install**。

### Q: 鸿蒙模拟器启动失败
**A**: 确保已安装 DevEco Studio（华为要求），并在 HBuilderX 插件中安装鸿蒙运行插件。

### Q: 登录报错 "网络请求失败"
**A**: 检查 `src/utils/request.ts` 中 `BASE_URL` 是否正确，以及后端是否启动。模拟器用 `10.0.2.2` 代替 `localhost`。

### Q: 后端返回 401
**A**: 确保数据库已初始化，用户表有数据。后端 `application.yml` 中 `spring.sql.init.mode: always` 会在启动时自动建表。

---

## 九、开发工作流

后续日常开发流程：

```
1. 启动后端 (mvn spring-boot:run)
2. 打开 HBuilderX → 打开 mobile/ 项目
3. 修改 Vue 页面 / API 模块
4. HBuilderX → 运行 → 运行到鸿蒙模拟器（或浏览器预览）
5. 查看日志 / 调试
6. 代码同步回 Web 端（如有共用部分修改）
```

Web 端（Vite）和移动端（HBuilderX）是两个独立的开发环境，互不影响。后端 API 是二者共用的一套。
