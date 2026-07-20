# 主题邮局机器人系统

基于带灵巧手人形机器人的邮政主题网点原型系统 — 业务中台后端 + 后台管理前端 + 邮政系统对接适配层。

## 环境依赖

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | 17+ | 后端运行环境 |
| Maven | 3.6+ | 后端构建 |
| MySQL | 8.0 | 主数据库 |
| Redis | 6.x+ | 缓存 + 库存原子操作 + 会话管理 |
| Node.js | 18+ | 前端构建 |
| npm | 9+ | 前端包管理 |

## 快速启动

### 1. 启动基础设施

```bash
# MySQL (localhost:3306)
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS post_robot DEFAULT CHARSET utf8mb4;"

# Redis (localhost:6379)
redis-server
```

### 2. 启动后端

```bash
cd post-robot-system
mvn spring-boot:run
```

默认开发模式（Mock 开启，无需真实邮政接口）。访问 `http://localhost:8223/api/doc.html` 查看 Swagger 接口文档。

**配置切换：**
- `application.yml` — 通用配置（数据源、Redis、JWT、邮政接口等）
- `application-dev.yml` — 开发环境（Mock 开启、SQL 日志输出、Swagger 开启）
- `application-prod.yml` — 生产环境（Mock 关闭、SQL 日志关闭、Swagger 关闭）

```bash
# 生产模式
mvn spring-boot:run -Dspring.profiles.active=prod
```

### 3. 启动前端

```bash
cd post-robot-vue
npm install
npm run dev
```

访问 `http://localhost:5173`。默认账号：`admin / admin123`。

## 模块说明

### 后端模块

| 模块 | 路径 | 说明 |
|------|------|------|
| Controller | `controller/` | RESTful API 接口层 |
| Service | `service/` | 业务逻辑层 |
| Mapper | `mapper/` | MyBatis-Plus 数据访问层 |
| Entity | `model/entity/` | 数据模型 |
| Enum | `model/enums/` | 枚举字典 |
| Adapter | `adapter/postal/` | 邮政系统对接适配层 |
| Common | `common/` | 工具层（JWT、签名、异常、响应） |
| Config | `config/` | 安全、CORS、Jackson、MyBatis-Plus 配置 |
| Task | `task/` | 定时任务（支付轮询、订单超时） |

### 前端模块

| 页面 | 路由 | 角色 |
|------|------|------|
| 首页看板 | `/dashboard` | ADMIN/OPERATOR/MAINTAINER |
| 商品管理 | `/products` | ADMIN/OPERATOR |
| 订单管理 | `/orders` | ADMIN/OPERATOR |
| 库存管理 | `/inventory` | ADMIN/OPERATOR |
| 任务监控 | `/tasks` | ADMIN/MAINTAINER |
| 告警管理 | `/alerts` | ADMIN/MAINTAINER |
| 用户管理 | `/users` | ADMIN |
| 审计日志 | `/audit-logs` | ADMIN |
| 邮政对接 | `/postal` | ADMIN |

## 邮政对接适配层

### 架构

```
adapter/postal/
├── config/PostalProperties.java     @ConfigurationProperties 配置绑定
├── model/YYRoot.java                通用报文包装器
├── model/SessionHeader.java         会话头（含签名）
├── model/enums/ServiceCode.java     F1~F5 服务代码
├── model/enums/PostalErrorCode.java  错误码映射
├── model/request/                   5 套请求 DTO
├── model/response/                  5 套响应 DTO
├── spi/PostalApi.java               SPI 接口契约
├── spi/PostalApiMock.java           Mock 实现（支付状态模拟机）
├── spi/PostalApiHttpClient.java     HTTP 实现（含重试+验签）
├── exception/                       异常体系
└── service/PostalAdapterService.java  业务门面
```

### 切换方式

```yaml
postal:
  mock:
    enabled: true   # true=Mock, false=真实HTTP调用
```

### 签名算法

```
Sign = BASE64(MD5(ServiceCode + Version + ActionCode + TransactionID
    + SrcSysID + DstSysID + ReqTime + SessionBodyJSON + Secret))
```

## API 概览（61 个端点）

| Controller | 端点数 | 认证 |
|-----------|--------|------|
| AuthController | 4 | 公开 |
| UserController | 6 | ADMIN + 登录 |
| ProductController | 8 | ADMIN/OPERATOR（推荐公开） |
| InventoryController | 11 | ADMIN/OPERATOR |
| OrderController | 6 | 登录 + ADMIN/OPERATOR |
| PaymentController | 3 | 回调公开，查询需登录 |
| TaskController | 8 | ADMIN/MAINTAINER |
| AlertController | 6 | ADMIN/MAINTAINER |
| AuditLogController | 2 | ADMIN |
| PostalController | 7 | ADMIN |
| HealthController | 1 | 公开 |
