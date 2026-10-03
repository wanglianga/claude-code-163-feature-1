# 城市无主井盖认领巡检维修与赔付协同平台

市民发现井盖缺失、松动、异响、塌陷后在线上报，平台将事件推送给街道值班员先行临时围护（**应急处置线**，只看现场风险与到场时效），再依据井盖标识、地下管线、历史施工、周边商户、城管台账排查权属（**权属追责线**）。权属无法即时确认时，街道先行设置警示锥、临时盖板、夜间反光标识并垫付费用，事件标记**责任待定**；权属单位认领后派发维修工单，维修队补充到场时间、井室状况、井盖规格、封路与专业协同信息；维修完成后城管巡查员按照片、定位、路面平整度、车辆碾压测试验收；市民或车辆受损的，赔付专员依据上报时间、临时围护、权属确认、维修时长、事故证明定责赔付；最终费用由城管复核分摊，街道垫付费用按复核结果回补。平台沉淀高发路段、权属争议单位、维修超时、赔付金额统计，用于调整日常巡检路线与施工移交规则。

**夜间临时围护失效处理**：街道放置警示锥等围护后，夜间若被车辆撞倒（如第二名骑车人摔伤），值班员可在同一事件下登记「夜间围护失效与二次事故」，把**首次上报 → 围护照片 → 巡查到场 → 警示锥移位时间 → 二次事故**串联起来，据巡查到场间隔、碰撞痕迹与监控认定失效原因（**围护方式不足 / 巡查间隔过长 / 外力破坏**），系统按原因、危险等级、敏感场所与照明给出处置建议，值班员据此**加派反光围挡、临时封道或安排专人值守**（追加费用并入街道应急垫付，供后续复核分摊）。若二次事故发生在**学校门口或公交站附近**，还须记录**人流高峰时段、照明条件、周边监控编号**，并至少选择**安排绕行提示或请交警协助**之一；二次事故造成人车损伤的自动立案并转交赔付专员，失效原因与处置结论同步供赔付定责参考。

## 技术栈

- **后端**：Spring Boot 3.3（Java 17）、Spring Data JPA、PostgreSQL 15、BCrypt 密码加密、内存令牌认证
- **前端**：Vue 3 + Vite + Vue Router + Pinia + Element Plus + ECharts
- **数据库**：PostgreSQL（本机脚本为用户级集群，端口 5433，免密 trust，不依赖系统服务；Docker Compose 为 postgres:15 容器）
- **容器化**：`docker-compose.yml` + 后端/前端多阶段 `Dockerfile`（前端 Nginx 反代 `/api`、`/uploads`）

## 目录结构

```
├── docker-compose.yml  # 一键启动 db + backend + frontend
├── backend/            # Spring Boot 后端（端口 8080，含 Dockerfile）
│   └── src/main/java/com/city/manhole/
│       ├── config/     # 认证拦截器、Web 配置、演示数据初始化
│       ├── controller/ # REST 接口（含 NightFailureController）
│       ├── service/    # 业务与状态机（应急线/权属线分离，含 NightFailureService）
│       ├── model/      # JPA 实体（事件/应急/夜间围护失效/排查/认领/工单/验收/赔付/分摊/通知/路线/规则）
│       └── repo/       # Spring Data 仓库
├── frontend/           # Vue 3 前端（本机 5173 / 容器 5180，含 Dockerfile、nginx.conf）
│   └── src/views/      # 登录/首页/上报/工作台/事件详情/赔付/分摊/统计/路线/规则/账号
└── scripts/            # 本机启动脚本（无 Docker 时）
```

## 本地启动

前置要求：JDK 17、Maven 3.6+、Node 18+、PostgreSQL 15（已安装即可，脚本自建用户级集群）。

### 方式一：Docker Compose 一键启动（推荐，无需本地安装 JDK/Node/PG）

```bash
docker compose up -d --build
```

启动后访问 **http://localhost:5180**（Nginx 托管前端并反代后端；后端直连 http://localhost:8080，数据库映射到本机 5433）。首次启动后端自动建表并写入演示数据；PostgreSQL 数据与上传照片保存在命名卷 `manhole-pgdata`、`manhole-uploads` 中。停止：`docker compose down`（加 `-v` 同时清空数据卷）。

### 方式二：本机脚本启动

```bash
# 一键启动（数据库 + 后端 + 前端）
bash scripts/start-all.sh

# 或分步启动
bash scripts/db-start.sh          # 1. 启动 PostgreSQL（端口 5433，自动建库 manhole）
bash scripts/start-backend.sh     # 2. 启动后端  http://localhost:8080
bash scripts/start-frontend.sh    # 3. 启动前端  http://localhost:5173
```

启动后访问 **http://localhost:5173** 。首次启动后端会自动建表并写入演示数据（12 个账号、12 个覆盖各状态的事件——其中含 2 起夜间围护失效二次事故、巡检路线与移交规则）。

停止数据库：`bash scripts/db-stop.sh`

> 说明：数据库连接配置在 `backend/src/main/resources/application.yml`（jdbc:postgresql://localhost:5433/manhole，用户 postgres，trust 免密）。如需使用已有 PostgreSQL 实例，修改该文件中的地址/账号即可。

## 测试账号

| 账号 | 密码 | 角色 | 职责 |
|---|---|---|---|
| citizen / citizen2 | citizen123 | 市民 | 上报井盖问题、查看进度 |
| street | street123 | 街道值班员 | 应急派单、临时围护、权属排查 |
| gas / power / telecom / municipal | owner123 | 权属单位（燃气/电力/通信/市政） | 认领井盖、派发维修工单 |
| repair1 / repair2 | repair123 | 维修队（市政一队/燃气专业队） | 接单、填写维修信息、完成维修 |
| inspector | inspect123 | 城管巡查员 | 验收（照片/定位/平整度/碾压测试） |
| comp | comp123 | 赔付专员 | 赔付案件审核定责、标记赔付 |
| admin | admin123 | 城管管理员 | 统计分析、费用复核分摊、路线/规则/账号管理 |

市民也可在登录页自助注册（仅市民角色）。

## 业务流程（两条线）

```
应急处置线：上报 → 街道派单 → 到场 → 临时围护(警示锥/临时盖板/夜间反光标识, 记录费用)
          → 〔夜间围护被撞倒〕登记二次事故 → 巡查到场核查 → 认定原因 → 加派反光围挡/封道/专人值守
权属追责线：围护后 → 五类证据排查 → 确认权属 / 责任待定(街道垫付) → 权属单位认领
          → 派维修工单 → 接单/维修(井室状况/井盖规格/封路/专业协同) → 验收(4 项检查)
          → 费用复核分摊 → 结案
赔付线：    上报时或夜间二次事故造成损伤自动立案 → 赔付专员定责(五要素+围护失效分析) → 核定/驳回 → 赔付
```

- 事件主状态：`REPORTED → SECURED → OWNER_CONFIRMED → REPAIRING → REPAIRED → ACCEPTED → CLOSED`
- 应急线状态：`PENDING → DISPATCHED → SECURED`；权属线状态：`UNCONFIRMED → INVESTIGATING → CONFIRMED / PENDING`
- 夜间围护失效处置状态：`REPORTED（已登记待核查）→ VERIFIED（已到场待认定）→ ASSESSED（已认定待处置）→ HANDLED（已加固）`。
  一个事件至多一条夜间失效记录；学校门口/公交站处置时强制绕行提示或交警协助至少一项。
- 失效原因 → 建议处置：**围护方式不足**→反光围挡（高危/照明昏暗再加专人值守）；**巡查间隔过长**→专人值守+反光围挡；**外力破坏**→反光围挡，学校门口/公交站或高危再临时封道、否则专人值守并调监控。建议可由值班员调整。
- 责任待定事件由街道先行处置并垫付，权属单位后续可认领，费用复核时按追责结论分摊（分摊合计必须等于总费用，否则系统拒绝）。
- 维修超时阈值 48 小时；应急到场时限：高危 120 / 中危 240 / 低危 480 分钟（`application.yml` 可调）。

## 验证方式（均已实际执行）

1. **后端编译**：`cd backend && mvn compile` 通过。
2. **前端构建**：`cd frontend && npm run build` 通过。
3. **全流程 API 验证**（curl 实际执行）：市民上报(含损伤自动立赔付案) → 街道派单/围护 → 排查确认燃气 → 燃气派单 → 维修队接单/完成 → 巡查员验收通过 → 赔付定责并赔付 → 管理员复核分摊 → 事件 `CLOSED`，费用合计校验一致。
4. **责任待定流程验证**：滨河路事件（责任待定）→ 燃气主动认领 → 维修 → 验收 → 赔付按 8:2 分摊 → 复核结案；分摊金额不等于总费用时接口正确拒绝（实测 45100≠44100 被拒）。
5. **角色权限验证**：8 个角色账号逐一调用各自接口成功；越权访问（市政查看燃气事件、维修队查看他人工单、市民访问统计、市民登记夜间失效）均被拒绝，未登录返回 401。
6. **统计接口验证**：总览/高发路段(按路段归集)/权属争议/维修超时/赔付(按月+按责任)/应急时效均返回正确数据。
7. **文件上传验证**：上传 PNG 返回 `/uploads/xxx.png`，经后端与 Vite 代理访问均 200。
8. **夜间围护失效专项验证**（curl 实际执行，全新建库后）：
   - 市民上报 → 街道派单/仅放置警示锥 → 登记夜间失效与二次事故（学校门口：人流高峰 `07:00-08:00,16:30-18:00`、照明昏暗、监控 `SX-201,SX-202`、绕行提示）→ 自动生成赔付案件（事故证明含“二次事故（夜间围护失效）”）。
   - 校验：移位时间晚于事故时间被拒；二次事故早于首次上报被拒；未围护不能登记失效；同一事件重复登记被拒；非法措施、敏感场所不选绕行/交警均被拒。
   - 巡查到场补拍照片（与原围护照片合并）→ 认定外力破坏 → `GET /suggest` 正确返回“反光围挡+临时封道”，并对昏暗/学校门口建议绕行提示与交警协助；处置后状态 `HANDLED`，追加费用 600 元并入街道应急垫付（100+600=700，经事件详情核对）。
   - 建议规则分支逐一验证：围护方式不足（高危→围挡+值守 / 低危→仅围挡）、巡查间隔过长（值守+围挡）、外力破坏（学校门口高危→围挡+封道；非敏感低危→围挡+值守、不强制绕行/交警）。
   - 统计：`/api/stats/overview` 返回夜间失效数、待处置数、二次伤人数；`/api/stats/night-failures` 按原因归集次数/敏感场所/平均响应分钟并列明事件；种子 2 起（学校门口外力破坏已处置、公交站围护方式不足待处置）展示正确。
9. **界面联调**：Vite 开发服务器启动，经其代理登录、`/api/stats/night-failures`、含 `nightFailure` 聚合的事件详情均返回 200 且字段完整；`npm run build` 通过。

人工界面验证建议路径：用 **street/street123** 登录，「应急处置台 → 夜间失效」页签可看到待处置事件；进入事件详情，顶部红色横幅与「🌙 夜间围护失效与二次事故」卡片展示串联时间，右侧「夜间围护失效处理」依次完成登记/到场/认定/处置；管理员在「统计分析」查看夜间失效原因分布与治理建议。

## 主要接口

| 接口 | 说明 |
|---|---|
| `POST /api/auth/login` `POST /api/auth/register` | 登录 / 市民注册 |
| `POST /api/incidents` `GET /api/incidents` `GET /api/incidents/{id}` | 上报 / 角色范围列表 / 聚合详情（含 nightFailure） |
| `POST /api/incidents/{id}/emergency/dispatch` `/secure` | 应急派单 / 完成围护 |
| `POST /api/incidents/{id}/night-failure` `POST .../patrol` `.../assess` `.../handle` `GET .../suggest` | 夜间围护失效：登记二次事故 / 到场核查 / 认定原因 / 加派处置 / 处置建议 |
| `POST /api/incidents/{id}/investigation` `/claim` | 权属排查 / 主动认领 |
| `POST /api/incidents/{id}/repair-order` `POST /api/repair-orders/{id}/accept` `/finish` | 派单 / 接单 / 完成维修 |
| `POST /api/incidents/{id}/acceptance` | 验收（四项检查） |
| `GET/PUT/POST /api/compensations...` | 赔付列表 / 补充 / 定责 / 赔付 |
| `GET /api/cost-allocations` `POST /api/cost-allocations/{id}/review` | 费用分摊复核 |
| `GET /api/stats/*` | overview / hotspots / disputes / repair-timeouts / compensation / emergency / night-failures |
| `GET/POST/DELETE /api/admin/users` `/routes` `/rules` | 账号 / 巡检路线 / 移交规则 |
| `POST /api/files` `GET /uploads/*` | 照片上传与访问 |
