# 苍穹外卖 (Sky Takeout) - 学习追踪器
> **Copilot 核心指令**：每次新开窗口时，执行以下恢复流程：
> 1. **读取本文件**，检查下方 `[x]` 和 `[ ]` 的状态，确定当前开发进度。
> 2. **扫描 `sky-server/src/main/java/com/sky/` 目录**，确认实际代码与「代码快照表」一致；如不一致，先提醒我更新快照。
> 3. **定位当前阶段的第一个未完成任务 `[ ]`**，作为本次会话的起点。
> 4. **用苏格拉底式提问引导我开始该任务**——不要直接给代码，先问我思路。
>
> 忽略已被划掉（~~删除线~~）的模块。
> 始终遵守 `.github/copilot-instructions.md` 中的 Role Definition 和 Response Guidelines。

---

##  项目代码现状快照 (Code Snapshot)
> 每完成一个功能点后更新此区域，方便 Copilot 快速恢复上下文。精确到**方法级别**。

| 模块 | 文件 | 已实现方法 | 待实现 / 已知缺陷 |
|------|------|-----------|-------------------|
| 启动类 | `SkyApplication.java` | `main()` | — |
| 配置 | `WebMvcConfiguration.java` | `addInterceptors()`, `docket()`, `addResourceHandlers()` | 后续可能需注册消息转换器（日期格式化） |
| 拦截器 | `JwtTokenAdminInterceptor.java` | `preHandle()` (JWT 校验 + BaseContext 存 empId), `afterCompletion()` (清理 ThreadLocal) | — |
| 异常处理 | `GlobalExceptionHandler.java` | `exceptionHandler(BaseException)` | ⚠️ 缺少 `SQLIntegrityConstraintViolationException` 处理（2.1 新增员工时会碰到）；缺少兜底 `Exception` 处理 |
| Controller | `EmployeeController.java` | `login()`, `logout()` (空实现) | 待实现：`save()`, `page()`, `startOrStop()`, `getById()`, `update()` |
| Service | `EmployeeService.java` / `Impl` | `login()` | 待实现：`save()`, `pageQuery()`, `startOrStop()`, `getById()`, `update()` |
| Mapper | `EmployeeMapper.java` | `@Select getByUsername()` | 待实现：`insert()`, `pageQuery()` (XML), `update()` (XML 动态 SQL) |
| XML | `EmployeeMapper.xml` | — (空文件) | 第二阶段分页查询和动态更新将写在这里 |
| 其他业务 | 菜品/订单/分类等 | — | 第三阶段起开始 |

**最后更新日期**：2026-02-27

---

## 🔧 技术债务 / 已知留坑 (Tech Debt Backlog)
> 记录已写代码中**故意留的坑**或**已知的不完美**，标注将在哪个阶段解决。这也是面试「你遇到过什么问题」的素材。

| # | 债务描述 | 所在文件 | 风险等级 | 计划解决阶段 | 状态 |
|---|---------|---------|---------|-------------|------|
| D1 | 密码使用裸 MD5 (`DigestUtils.md5DigestAsHex`)，无加盐，彩虹表可破解 | `EmployeeServiceImpl#login` | 🟡 中 | 第二阶段讨论，了解原理即可（项目不改） | 待讨论 |
| D2 | `GlobalExceptionHandler` 只捕获 `BaseException`，无兜底 `Exception` 处理 → 前端可能收到 500 HTML | `GlobalExceptionHandler.java` | 🔴 高 | 第二阶段 2.1（新增员工触发 SQL 异常时补充） | 待修复 |
| D3 | `EmployeeMapper.xml` 空文件，目前全用注解 SQL | `EmployeeMapper.xml` | 🟢 低 | 第二阶段 2.2-2.3（分页 + 动态更新必须用 XML） | 待实现 |
| D4 | 新增员工时需手动 set `createTime`, `updateTime`, `createUser`, `updateUser` 等公共字段 → 冗余代码 | 第二阶段将产生 | 🟡 中 | 第四阶段 AOP `@AutoFill` 重构消除 | 待重构 |
| D5 | `logout()` 为空实现，服务端未做 Token 失效处理（JWT 无状态的固有问题） | `EmployeeController#logout` | 🟢 低 | 了解原理即可（Redis 黑名单方案） | 待讨论 |

---

##  当前进度状态 (Status)
- [x] 第一阶段：项目骨架与登录鉴权 *(登录链路已跑通)*
- [ ] 第二阶段：管理端-员工模块  **当前阶段**
- [ ] 第三阶段：复杂表与事务操作 (精简版)
- [ ] 第四阶段：AOP 进阶实战
- [ ] 第五阶段：Redis 高并发实战
- [ ] 第六阶段：订单核心状态机

---

## 📋 每个功能点的标准学习流程 (Standard Workflow)
> 每个任务严格按此流程执行，养成工程化学习习惯。

```
1.【思考】回答前置问题 / Copilot 苏格拉底提问（不写代码）
2.【设计】画出请求流转伪代码 / Mermaid 流程图
3.【编码】独立编写代码，不看参考源码
4.【对比】和参考源码 diff，记录差异与原因
5.【破坏】执行破坏性测试，验证理解
6.【面试】整理面试追问点，填入面试题积累库
7.【记录】更新踩坑日志 + 知识盲区 + 代码快照 + 技术债务
```

---

##  执行路线与破坏性验证任务

### 第一阶段：项目骨架与登录鉴权 (基础地基) 
- [x] 1.1 `WebMvcConfiguration` (静态资源与拦截器注册)
- [x] 1.2 `JwtTokenAdminInterceptor` (JWT 拦截与校验)
  -  **破坏性测试**：注释掉 Token 校验逻辑，传假 Token，观察 `GlobalExceptionHandler` 返回。
- [x] 1.3 `BaseContext` (ThreadLocal 上下文)
- [x] 1.4 `EmployeeController#login` (员工登录逻辑)
-  **通关白板测试**：向导师解释 JWT 三段结构 (Header.Payload.Signature)，以及 ThreadLocal 内存泄漏的底层原因 (ThreadLocalMap 的弱引用 Key + 强引用 Value)。
- [ ]  **回顾任务**：独立重写 `JwtTokenAdminInterceptor` 和 `EmployeeController#login`，不看源码，写完对比差异。

### 第二阶段：管理端-员工模块 (单表 CRUD 肌肉记忆)
- [ ] 2.1 新增员工 (`EmployeeServiceImpl#save`)
  -  **重点**：密码 MD5 加密、手动设置 `createTime` 等通用字段（为第四阶段 AOP 重构留坑）。
  -  **前置问题**：`DigestUtils.md5DigestAsHex()` 为什么不够安全？面试中被问到"加盐"怎么答？
- [ ] 2.2 员工分页查询 (PageHelper 原理)
  -  **前置问题**：PageHelper 是怎么做到你不写 LIMIT 它帮你加上的？（提示：MyBatis 拦截器 / Plugin 机制）
- [ ] 2.3 启用/禁用与编辑员工 (MyBatis 动态 SQL `<if>` 标签实战)
  -  **前置问题**：为什么用一个通用的 `update` 方法 + 动态 SQL，而不是给每个字段写一个 `updateXxx` 方法？
- [ ] 2.4 **阶段复盘**：独立画出"新增员工"请求的完整流转图 (前端  Nginx  Controller  Service  Mapper  DB)。

### 第三阶段：复杂表与事务操作 (精简版)
- [ ] 3.1 菜品管理 (`Dish` + `DishFlavor` 多表操作)
  -  **重点**：在 `saveWithFlavor` 上使用 `@Transactional`。
  -  **破坏性测试**：故意在保存 Flavor 时抛 `RuntimeException`，去数据库看 `Dish` 表是否回滚。
  -  **通关白板测试**：画出 Spring 事务失效的 3 种常见场景（自调用、非 public、异常被 catch）。
- ~~3.2 分类管理~~ *(高度重复，精读源码理解即可，不手写)*
- ~~3.3 套餐管理~~ *(高度重复，精读源码理解即可，不手写)*

### 第四阶段：AOP 公共字段自动填充 (重构时刻)
- [ ] 4.1 自定义注解 `@AutoFill`
- [ ] 4.2 编写切面 `AutoFillAspect` (AOP + 反射)
  -  **破坏性测试**：把 `@Before` 改成 `@After`，观察插入数据库时是否报 Null 约束异常。
- [ ] 4.3 重构阶段二的业务代码，删掉手动 set 的冗余代码。

### 第五阶段：Redis 高并发实战
- [ ] 5.1 微信登录接口逻辑 (对接微信 API)
- [ ] 5.2 引入 Spring Cache (`@Cacheable`, `@CacheEvict`) 缓存菜品数据
  -  **破坏性测试**：`docker stop redis`，触发缓存雪崩，观察无降级策略时的表现。

### 第六阶段：订单核心逻辑 (状态机流转)
- [ ] 6.1 用户下单 (多表复杂校验 + 事务)
- ~~6.2 微信支付~~ *(个人无资质，精读逻辑即可)*
- [ ] 6.3 管理端接单/拒单/状态流转
  -  **通关白板测试**：口述订单状态机的防并发篡改策略（乐观锁 / 状态前置校验）。

---

##  踩坑日志 (Bug Journal)
> 每次遇到报错，按以下格式记录。这是面试时"你遇到过什么技术问题"的素材库。

| 日期 | 错误现象 | 根因分析 | 解决方案 | 关联知识点 |
|------|----------|----------|----------|------------|
| *示例* | *启动报 `BeanCreationException`* | *`@Component` 漏写导致 Spring 扫描不到* | *加上注解* | *IoC 容器、组件扫描* |

---

##  Java 基础知识盲区追踪 (Knowledge Gaps)
> 遇到不理解的 Java 基础概念，记录在此。
> - `[ ]` = 未理解 → `[~]` = 能说出是什么但说不清原理 → `[x]` = 能白板画图 + 口述原理

| 概念 | 触发场景 | 状态 | 通关标准（能做到才算 `[x]`） | 一句话总结 |
|------|----------|------|---------------------------|------------|
| ThreadLocal | 拦截器存用户ID | [ ] | 能画出 `Thread → ThreadLocalMap → Entry(WeakRef Key, Strong Value)` 引用链；能口述 GC 后 Value 泄漏原因和 `remove()` 的必要性 | *待填写* |
| 泛型 `<T>` | `Result<T>` 统一返回 | [ ] | 能解释类型擦除（编译期 vs 运行期）；能手写一个简单泛型方法 | *待填写* |
| 反射 | AOP 自动填充字段 | [ ] | 能手写 `clazz.getDeclaredMethod().invoke()` 调用链；能说出反射的性能代价和安全隐患 | *待填写* |
| 代理模式 | Spring AOP / 事务 | [ ] | 能画出 JDK 动态代理 vs CGLIB 的类图；能解释为什么自调用会导致事务失效 | *待填写* |
| Builder 模式 | Lombok `@Builder` | [ ] | 能手写一个简化版 Builder（不用 Lombok）；能说出 Builder vs 构造函数 vs setter 的取舍 | *待填写* |

---

##  面试题积累库 (Interview Bank)
> 每个阶段学到的面试高频题，按知识点归档。
> **深度等级**：⭐ 能说出「是什么」 / ⭐⭐ 能说出「为什么 + 对比方案」 / ⭐⭐⭐ 能说出「底层原理 + 源码级理解 + 横向对比」

### Spring Boot / MVC
| 问题 | 我的回答要点 | 深度 | 来源阶段 |
|------|-------------|------|----------|
| *Spring Boot 自动配置原理是什么？* | *待填写* | ⭐ | 第一阶段 |
| *拦截器和过滤器的区别？* | *待填写* | ⭐ | 第一阶段 |

### 数据库 / MyBatis
| 问题 | 我的回答要点 | 深度 | 来源阶段 |
|------|-------------|------|----------|
| *MyBatis `#{}` 和 `${}` 的区别？* | *待填写* | ⭐ | 第二阶段 |

### 缓存 / Redis
| 问题 | 我的回答要点 | 深度 | 来源阶段 |
|------|-------------|------|----------|
| *缓存穿透、击穿、雪崩分别是什么？* | *待填写* | ⭐ | 第五阶段 |

### 并发 / JVM
| 问题 | 我的回答要点 | 深度 | 来源阶段 |
|------|-------------|------|----------|
| *ThreadLocal 为什么会内存泄漏？* | *待填写* | ⭐ | 第一阶段 |
