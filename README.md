# 城市无主井盖认领巡检维修与赔付协同平台

市民发现井盖缺失、松动、异响、塌陷后在线上报，平台将事件推送给街道值班员先行临时围护（**应急处置线**，只看现场风险与到场时效），再依据井盖标识、地下管线、历史施工、周边商户、城管台账排查权属（**权属追责线**）。权属无法即时确认时，街道先行设置警示锥、临时盖板、夜间反光标识并垫付费用，事件标记**责任待定**；权属单位认领后派发维修工单，维修队补充到场时间、井室状况、井盖规格、封路与专业协同信息；维修完成后城管巡查员按照片、定位、路面平整度、车辆碾压测试验收；市民或车辆受损的，赔付专员依据上报时间、临时围护、权属确认、维修时长、事故证明定责赔付；最终费用由城管复核分摊，街道垫付费用按复核结果回补。平台沉淀高发路段、权属争议单位、维修超时、赔付金额统计，用于调整日常巡检路线与施工移交规则。

**夜间临时围护失效处理**：街道白天放置的警示锥夜间被车辆撞倒、围护失效并造成第二名骑车人摔伤时，值班员在事件内登记**夜间巡查到场记录**与**围护失效记录**，平台将「首次上报 → 围护照片/措施 → 巡查到场 → 警示锥移位时间 → 二次事故」串成时间链，自动计算移位距上次巡查的间隔，并据围护措施、巡查间隔、外力痕迹判定失效原因（**围护方式不足 / 巡查间隔过长 / 外力破坏**），值班员据此决定**加派反光围挡、临时封道或安排专人值守**。二次事故自动生成「二次事故」赔付案件转交赔付专员。事故点位于**学校门口或公交站附近**时，记录人流高峰时段、照明条件、周边监控编号，并由值班员选择是否**临时安排绕行提示**或**请求交警协助**，加固落实后须确认闭环。

## 技术栈

- **后端**：Spring Boot 3.3（Java 17）、Spring Data JPA、PostgreSQL 15、BCrypt 密码加密、内存令牌认证
- **前端**：Vue 3 + Vite + Vue Router + Pinia + Element Plus + ECharts
- **数据库**：PostgreSQL（本地脚本：用户级集群，端口 5433，免密 trust；Docker：容器内 5432）
- **容器**：Docker Compose（PostgreSQL + 后端 + 前端 nginx）一键启动

## 目录结构

```
├── backend/            # Spring Boot 后端（端口 8080）
│   └── src/main/java/com/city/manhole/
│       ├── config/     # 认证拦截器、Web 配置、演示数据初始化
│       ├── controller/ # REST 接口
│       ├── service/    # 业务与状态机（应急线/权属线分离）
│       ├── model/      # JPA 实体（事件/应急/排查/认领/工单/验收/赔付/分摊/通知/路线/规则）
│       └── repo/       # Spring Data 仓库
├── frontend/           # Vue 3 前端（端口 5173，代理 /api、/uploads 到 8080）
│   └── src/views/      # 登录/首页/上报/工作台/事件详情/赔付/分摊/统计/路线/规则/账号
└── scripts/            # 启动脚本
```

## 本地启动

前置要求：JDK 17、Maven 3.6+、Node 18+、PostgreSQL 15（已安装即可，脚本自建用户级集群）。

```bash
# 方式零（推荐）：Docker Compose 一键启动（无需本机 JDK/Node/PostgreSQL）
docker compose up -d --build
# 访问 http://localhost:5173 （nginx 托管前端并代理 /api、/uploads 到后端）
# 后端直连：http://localhost:8080 ；数据库：localhost:5433/manhole（trust 免密）

# 方式一：本机脚本一键启动（数据库 + 后端 + 前端）
bash scripts/start-all.sh

# 方式二：分步启动
bash scripts/db-start.sh          # 1. 启动 PostgreSQL（端口 5433，自动建库 manhole）
bash scripts/start-backend.sh     # 2. 启动后端  http://localhost:8080
bash scripts/start-frontend.sh    # 3. 启动前端  http://localhost:5173
```

启动后访问 **http://localhost:5173** 。首次启动后端会自动建表并写入演示数据（12 个账号、12 个事件，含两起夜间围护失效场景：学校门口二次事故、公交站围护不足待加固；以及巡检路线与移交规则）。

停止：`bash scripts/db-stop.sh` 或 `docker compose down`（加 `-v` 同时清除数据卷）。

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
          → 夜间巡查(到场复核/移位发现) → 围护失效登记(移位时间) → 二次事故(自动立赔付案)
          → 原因判定(围护不足/巡查间隔过长/外力破坏) → 加固决策(反光围挡/临时封道/专人值守) → 加固落实
权属追责线：围护后 → 五类证据排查 → 确认权属 / 责任待定(街道垫付) → 权属单位认领
          → 派维修工单 → 接单/维修(井室状况/井盖规格/封路/专业协同) → 验收(4 项检查)
          → 费用复核分摊 → 结案
赔付线：    首次上报有损伤 / 夜间二次事故自动立案 → 赔付专员定责(五要素分析) → 核定/驳回 → 赔付
```

- 事件主状态：`REPORTED → SECURED → OWNER_CONFIRMED → REPAIRING → REPAIRED → ACCEPTED → CLOSED`
- 应急线状态：`PENDING → DISPATCHED → SECURED`；权属线状态：`UNCONFIRMED → INVESTIGATING → CONFIRMED / PENDING`
- 夜间围护失效原因：`INADEQUATE`（围护方式不足：仅警示锥、无反光标识/盖板）、`INTERVAL_TOO_LONG`（移位距上次巡查超过夜间巡查时限 180 分钟，可在 `application.yml` 的 `app.night-patrol-limit-minutes` 调整）、`EXTERNAL_DAMAGE`（车辆撞倒等外力破坏）；登记时不选原因则系统按时间链自动建议。
- 学校门口/公交站场景：必须记录人流高峰、照明条件、监控编号；值班员可勾选绕行提示与交警协助并填写落实情况。
- 责任待定事件由街道先行处置并垫付，权属单位后续可认领，费用复核时按追责结论分摊（分摊合计必须等于总费用，否则系统拒绝）。
- 维修超时阈值 48 小时；应急到场时限：高危 120 / 中危 240 / 低危 480 分钟（`application.yml` 可调）。

## 验证方式（均已实际执行）

1. **后端编译**：`cd backend && mvn compile` 通过。
2. **前端构建**：`cd frontend && npm run build` 通过。
3. **全流程 API 验证**（curl 实际执行）：市民上报(含损伤自动立赔付案) → 街道派单/围护 → 排查确认燃气 → 燃气派单 → 维修队接单/完成 → 巡查员验收通过 → 赔付定责并赔付 → 管理员复核分摊 → 事件 `CLOSED`，费用合计校验一致。
4. **责任待定流程验证**：滨河路事件（责任待定）→ 燃气主动认领 → 维修 → 验收 → 赔付按 8:2 分摊 → 复核结案；分摊金额不等于总费用时接口正确拒绝（实测 45100≠44100 被拒）。
5. **夜间围护失效全流程验证**（curl 实际执行）：街道完成围护 → 登记两轮夜间巡查（第二轮发现警示锥 01:10 被撞散）→ 登记围护失效与学校门口二次事故（骑车人摔伤，含高峰时段/照明/监控编号 JY-0112、绕行提示、交警协助）→ 系统串联时间链、计算巡查间隔并给出原因 → 二次事故自动生成 `SECOND` 赔付案件 → 值班员提交加固决策（反光围挡+专人值守）→ 确认加固落实；公交站场景验证「仅警示锥→围护方式不足」自动判定与「反光围挡+临时封道」决策。
6. **角色权限验证**：8 个角色账号逐一调用各自接口成功；越权访问（市政查看燃气事件、维修队查看他人工单、市民访问统计、市民登记夜间失效）均被拒绝，未登录返回 401。
7. **统计接口验证**：总览/高发路段(按路段归集)/权属争议/维修超时/赔付(按月+按责任)/应急时效均返回正确数据。
8. **文件上传验证**：上传 PNG 返回 `/uploads/xxx.png`，经后端与 Vite 代理访问均 200。

人工界面验证建议路径：用 `street` 登录工作台，进入带红色「夜」标记的事件（如 `MH20261001-0001` 学校门口案例）查看时间链与失效卡片；对任意已围护事件在右侧「夜间巡查到场」「夜间围护失效处理」面板录入；用 `comp` 登录可在赔付列表看到「二次事故」类型案件。

## 主要接口

| 接口 | 说明 |
|---|---|
| `POST /api/auth/login` `POST /api/auth/register` | 登录 / 市民注册 |
| `POST /api/incidents` `GET /api/incidents` `GET /api/incidents/{id}` | 上报 / 角色范围列表 / 聚合详情 |
| `POST /api/incidents/{id}/emergency/dispatch` `/secure` | 应急派单 / 完成围护 |
| `POST /api/incidents/{id}/night-patrols` | 夜间巡查到场（班次、围护状态、移位时间、照片） |
| `POST /api/incidents/{id}/night-failures` `GET /api/night-failures` | 登记围护失效与二次事故 / 失效记录列表 |
| `POST /api/night-failures/{id}/decision` `/reinforce` | 原因判定与加固决策 / 加固落实确认 |
| `POST /api/incidents/{id}/investigation` `/claim` | 权属排查 / 主动认领 |
| `POST /api/incidents/{id}/repair-order` `POST /api/repair-orders/{id}/accept` `/finish` | 派单 / 接单 / 完成维修 |
| `POST /api/incidents/{id}/acceptance` | 验收（四项检查） |
| `GET/PUT/POST /api/compensations...` | 赔付列表 / 补充 / 定责 / 赔付 |
| `GET /api/cost-allocations` `POST /api/cost-allocations/{id}/review` | 费用分摊复核 |
| `GET /api/stats/*` | overview / hotspots / disputes / repair-timeouts / compensation / emergency |
| `GET/POST/DELETE /api/admin/users` `/routes` `/rules` | 账号 / 巡检路线 / 移交规则 |
| `POST /api/files` `GET /uploads/*` | 照片上传与访问 |
