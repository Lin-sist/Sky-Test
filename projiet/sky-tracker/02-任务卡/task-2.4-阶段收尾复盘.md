# 任务卡：2.4 阶段收尾复盘

## 0. 基本信息
- 任务名称：遗留修复 + 深度复盘 + 阶段退出检查
- 预计耗时：2 天（D16 晚 + D17，各 1 小时）
- 实际耗时：D16-D20（分段完成，含知识沉淀与复盘）
- 完成状态：✅ 已完成（第二阶段收尾完成，进入第三阶段准备）

## 0-1. 下一次收尾路径（严格按顺序）

- [x] 步骤1（10分钟）：补完 startOrStop 正常路径验证（修正 URL）
- [x] 步骤2（20分钟）：跑完 3 条破坏性测试（非法 status、不存在 id、username 真冲突）
- [x] 步骤3（15分钟）：补全 2 条 RCA（extendMessageConverters、@AutoFill
- [x] 步骤5（10分钟）：完善 05-知识盲区（Spring Boot 项目结构）并沉淀困惑
- [x] 步骤6（5分钟）：勾选退出条件并更新 phase-status

## 0-2. 明日执行 SOP（照着做，不要跳步）

### A. 开始前准备（5分钟）

- 打开后端服务，确认应用启动无报错
- 打开 Apifox，确认环境是本地开发环境（`http://localhost:8080`）
- 在 Apifox 全局变量里确认 `token` 已更新（先调用一次登录接口拿新 token）
- 打开本文档，边执行边勾选，不要“做完再补”

状态：A/B 阶段已执行完，下一次从 C 阶段继续。

### B. 接口验证阶段（30分钟）

#### B1. 正常路径：startOrStop（5分钟）

- 请求：`POST /admin/employee/status/0?id=3`（id 用你库里存在的普通员工）
- 预期：`code=1`
- 证据：
记录请求 URL：http://localhost:8080/admin/employee/status/0?id=3
请求头：token：<已脱敏，不在文档中保留真实 token>
响应体：
```json
{
    "code": 1,
    "msg": null,
    "data": null
}
```

#### B2. 破坏性测试1：非法 status（5分钟）

- 请求：`POST /admin/employee/status/2?id=3`
- 观察点：当前项目是静默通过、还是报业务异常
- 证据：
返回结果为
```json
{
    "code": 1,
    "msg": null,
    "data": null
}
```

#### B3. 破坏性测试2：不存在 id（5分钟）

- 请求：`POST /admin/employee/status/1?id=9999`
- 观察点：影响行数为 0 时系统返回什么
- 证据：
返回结果为
```json
{
    "code": 1,
    "msg": null,
    "data": null
}
```

#### B4. 破坏性测试3：username 真冲突（15分钟）

- 第一步：`GET /admin/employee/1`，拿到员工 A 的 username
- 第二步：`GET /admin/employee/page?page=1&pageSize=20`，找员工 B（username 必须与 A 不同）
- 第三步：`PUT /admin/employee`，把 A 的 `username` 改成 B 的 `username`
- 预期：触发唯一键冲突，返回 `code=0`（或统一异常中的失败码）
- 证据：部分数据如下：
```json
{
    "code": 1,
    "msg": null,
    "data": {
      "id": 1,
      "username": "admin",
      "name": "管理员",
    }
}
```

```json
"code": 1,
    "msg": null,
    "data": {
        "total": 5,
        "records": 
            {
                "id": 5,
                "username": "张三",
                "name": "测试员工",
            }
    }
```

第三个PUT请求测试数据：

```json
{
  "id": 1,
  "username": "张三",
  "name": "测试员工",
  "phone": "13800138003",
  "sex": "1",
  "idNumber": "110101199003074512"
}
```

结果：
```json
{
    "code": 0,
    "msg": "张三已存在",
    "data": null
}
```


### C. 文档沉淀阶段（20分钟）

#### C1. RCA 补写（12分钟）

- 在 `rca-阶段A.md` 补 2 条：
  - `extendMessageConverters` 写错位置
  - `@AutoFill` 误判为 MyBatis 原生注解
- 每条必须有：现象、定位、根因、预防（缺一不可）

#### C2. 深度复盘修正（8分钟）

- 修正 Q2：明确 `BaseContext.setCurrentId` 在拦截器 `preHandle` 写入
- 修正 Q5：明确“为什么要注册 converter + 为什么 add(0)”
- 在 `05-知识盲区/Spring-Boot项目结构.md` 增补：
  - Controller-Service-Mapper 分层职责边界
  - 为什么不把业务逻辑写在 Controller
  - 你当前最容易混淆的 3 个点（例如 DTO/Entity、事务边界、异常处理）

### D. 收尾阶段（5分钟）

- 勾选本卡退出项
- 更新 `phase-status.md` 的日期、实际完成、下一动作
- 写一句阶段结论：是否满足进入第三阶段

### E. 卡住时的排查顺序（不要乱试）

- 先看 URL 是否完整（`/admin/employee/...` 前缀是否漏写）
- 再看请求方法是否正确（POST/GET/PUT）
- 再看 token 是否过期或未带上
- 再看参数是否传对（`status` 在路径，`id` 在 query）
- 最后才怀疑代码问题

---

## 1. 遗留小修复（先做，10 分钟）

### 1-1 XML 类型 Bug
- [x] `EmployeeMapper.xml` 第 48 行：`and updateUser != ''` 改为只保留 `updateUser != null`
  - 原因：`updateUser` 是 `Long` 类型，与空字符串 `''` 比较类型不符
  - 复核结论（2026-03-16）：代码已符合预期，无需重复修改

### 1-2 username 冲突谜题（排查后补结论）
- 问题：把 id=1 的 username 改为 test001 → 返回 code:1（本应 code:0）
- [x] 先用 `GET /admin/employee/1` 查出当前 id=1 的 username 是什么
- [x] 再用 `GET /admin/employee/page` 确认 test001 属于哪个 id
- 如果 id=1 本身 username 就已经是 test001（前几次测试改过），改成一样的值不触发唯一约束，这是正常的
- 重新构造真正的冲突：找两个 **username 不同的员工**，把 A 的 username 改成 B 的 username


建议你明天直接按这个模板填证据：
- A员工：id=___1_, username=__admin__
- B员工：id=___5_, username=___张三_
- 冲突请求：把 A.username 改成 B.username
- 实际响应：code=___0_, msg=_"张三已存在"__

补充：如果没有触发冲突，按下面顺序排查
- A 和 B 是否其实是同一个用户
- A 修改后的 username 是否与 B 完全一致（注意大小写与空格）
- 数据库 employee.username 是否有唯一索引

---

## 2. 深度复盘问题（先自己写答案，再翻代码对照）

> 这是本阶段最重要的环节。先合上 IDE，凭记忆作答，再翻代码验证自己的盲点。

### Q1：全链路口述（架构理解）

不看代码，描述一次 `POST /admin/employee`（新增员工）请求的完整流转：
- 请求从哪里进来？经过了哪些类？每一层的职责是什么？
- `EmployeeDTO` 在哪一层被转成 `Employee` 实体？为什么要这么转？
- 密码在哪一层被加密？为什么不在 Controller 层做？

**你的回答：**
Q1:请求从前端传来，依次经过controller层，service层，mapper层；controller层负责将请求转换为可被service层读取的json数据；service层有定义的方法接口也实现了具体的方法，是数据被处理的地方；mapper层是与数据库交互的地方，mapper层可以查询数据库具体信息并将数据返回给service层。

Q2：在controller层转换。至于为什么我还不清楚。

Q3：密码在service层被加密，为什么不在controller层加密？一个是为了职责划分，一个是在controller层要与前端对接，有被截获等的安全问题。

---

### Q2：ThreadLocal 与 BaseContext（核心机制）

> 代码里有 `BaseContext.getCurrentId()`，它用的是 ThreadLocal。

- `ThreadLocal` 存储的数据在哪里？服务器上有 100 个并发请求，数据会互相干扰吗？为什么？
- HTTP 请求进来时，是谁把 userId 存进 ThreadLocal 的？在哪个类的哪个方法里？
- 如果你在 `save()` 方法里调 `BaseContext.getCurrentId()` 返回了 null，第一个怀疑对象应该是哪里？

**你的回答：**

Q1:ThreadLocal的数据存放在内存中？如果服务器有100个并发请求，数据之间不会互相干扰，因为各个线程是独立的（我初略理解）

Q2:在controller层通过getCurrentId()方法存入，这个方法在threadlocal的工具类BaseContext中。

Q3:如果返回了null，那么极有可能是这个线程还没有创建，应该重点看一下setCurrentId方法是否成功执行了。


---

### Q3：JWT 鉴权流程（安全机制）

- JWT Token 是在哪个接口生成的？生成时往 claims 里放了什么数据？
- 之后每次请求，Token 在哪里被校验？校验逻辑做了什么？校验失败会发生什么（抛什么/返回什么）？
- 为什么 `/admin/employee/login` 这个路径要从拦截器里排除？如果不排除会怎样？

**你的回答：**

Q1:是在login接口生成的。claims中主要存放了用户id，token值。

Q2:在jwt拦截器校验，具体是JwtTokenAdminInterceptor类当中。校验时，有一个解析jwt的方法，该方法会逐一比对token值，过期时间，用户id等。校验失败则会抛出“用户未登录”的异常提示。

Q3:如果不排除，那么用户永远无法登录，这个login请求会一直被拦截器拦截（写道这里我本来想看一下我无法登录是不是拦截器造成的，但是想了一下拦截器排除了login接口，也就是说问题不可能在这里）

---

### Q4：PageHelper 原理（面试高频）

- `PageHelper.startPage(page, pageSize)` 这行代码执行后，分页参数存到了哪里？
- `employeeMapper.pageQuery(dto)` 执行时，SQL 里明明没有 LIMIT，为什么数据库只返回了当页的数据？
- 如果 `pageQuery()` 方法里不 cast 成 `Page<Employee>` 而直接用 `List<Employee>`，`total` 字段能拿到吗？为什么？

**你的回答：**

Q1:这个确实不清楚，我模糊记得pagehelper是一个工具类，这个startpage方法是启动这个工具类用的，具体参数我不知道存放在哪里，可能是threadlocal里面？

Q2:这好像涉及到了mybatis的机制，这个limit会被自动填写成一个默认值。

Q3:不能。我模糊记得total应该是属于Page类下的字段，使用List是取不到的。

---

### Q5：序列化与 extendMessageConverters（经典坑）

- `LocalDateTime` 被默认 Jackson 序列化成数组的根本原因是什么？（Jackson 不认识 Java8 时间类型）
- `JacksonObjectMapper` 已经在 `sky-common` 里配置好了，为什么还要在 `WebMvcConfiguration` 里注册？不注册会怎样？
- `converters.add(0, converter)` 为什么要插到第 0 位，而不是追加到末尾？

**你的回答：**

Q1: 不知道。

Q2: 不知道。

Q3: 不知道。


---

### Q6：静默失败的设计取舍（工程思维）

本次测试发现两处"静默失败"：
1. `startOrStop(status, id=9999)` → 影响行数 0，返回 code:1
2. `getById(id=9999)` → data 为 null，返回 code:1

- 这两种行为在当前教学项目里是否可以接受？
- 如果是生产系统，应该怎么改？（提示：Service 层加什么判断？抛什么异常？GlobalExceptionHandler 怎么处理？）
- `getById` 的 null data 有没有可能让前端崩溃？前端通常怎么防御这种情况？

**你的回答：**

Q1: 如果是目前教学环节的话，其实不影响，但是等到再更高阶的场景时，这种问题是不容许的，因为可能真的有id为9999或其他id的用户，如果静默失败，那么完全不知道问题出在哪里。

Q2: Service追加一个确认id是否存在的判断，如果不存在，可以抛出类似IdNOTFOUND的异常，在全局异常处理中，不知道如何处理。

Q3: 理论上有可能，因为id是不存在的，如果重复请求的话可能会让前端崩溃（因为用户在调用一个不存在的请求），前端可以增加一个id存在判断，如果不存在可以直接抛出异常，避免后续重复的操作导致崩溃。

---

## 3. 可迁移模式沉淀（写入 04-可迁移模式库）

- [x] **动态 SQL 通用 update 模式**
  - 单 `update(Entity)` 方法 + `<set><if>` 组合，同时服务局部字段更新（启停）和全量更新（编辑）
  - 注意：数字/Long 类型字段只判 `!= null`，String 类型字段判 `!= null and != ''`

- [x] **自定义消息转换器注册模式**
  - 继承 `WebMvcConfigurationSupport` → override `extendMessageConverters` → `converters.add(0, new MappingJackson2HttpMessageConverter(new JacksonObjectMapper()))`
  - 核心：index=0 插入，保证优先级高于 Spring 默认 Jackson 转换器

说明（2026-03-20）：
- 本阶段模式沉淀采用“问题模板 + 费曼复述清单”形态，已写入 `04-可迁移模式库/2.4-可迁移模式.md`，用于后续高频复盘与迁移。

模式库落地要求（避免写成空话）：
- 场景：什么情况下用
- 做法：最小实现步骤（3-5条）
- 边界：什么时候不适用
- 迁移：可迁移到 RAG 项目的哪个模块

---

## 4. 阶段退出检查（全部打勾才能进入第三阶段）

- [x] 4 个功能点 Apifox 正常路径全部通过
- [x] username 冲突破坏性测试返回 code:0（真正构造冲突后验证）
- [x] rca-阶段A.md 共计 ≥ 4 条 RCA
- [x] 04-可迁移模式库 ≥ 2 条模式已写入（本阶段至少补齐 2 条）
- [x] Q1~Q6 深度复盘问题全部填写完毕

### 明日验收打卡区（做完立刻勾选）

- [x] 已完成 4/4 正常路径接口验证
- [x] 已完成 3/3 破坏性测试并记录响应
- [x] 已补 2 条 RCA，且每条含「现象-定位-根因-预防」
- [x] 已补 2 条可迁移模式，且包含可复用条件
- [x] 已补 05-知识盲区文档并完成自我提问
- [x] 已更新 phase-status（日期、实际完成、下一动作）

### 明日最终提交清单（完成后一次性发给我）

- [x] 4 条接口验证结果（正常+破坏）
- [x] 2 条 RCA 正文
- [x] 2 条可迁移模式正文
- [x] phase-status 最终版截图或文本

---

## 5. 完成后更新 phase-status.md
- 阶段状态改为：第二阶段完成，进入第三阶段准备
- 下一步：待定（全部退出条件满足后决定）

当前未完成最小集合（进入第三阶段前必须完成）：
- 无（第二阶段退出条件已满足）
