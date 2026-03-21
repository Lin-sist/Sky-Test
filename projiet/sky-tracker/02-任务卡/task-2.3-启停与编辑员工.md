# 任务卡：2.3 启停与编辑员工

## 0. 基本信息
- 任务名称：启停员工 + 根据ID查员工 + 编辑员工 + 修复 createTime 序列化
- 对应接口/方法：
  - `POST /admin/employee/status/{status}` → `startOrStop`
  - `GET /admin/employee/{id}` → `getById`
  - `PUT /admin/employee` → `update`
  - （遗留）`WebMvcConfiguration#extendMessageConverters` 修复日期序列化
- 预计耗时：2 天（每天 1 小时）
- 实际耗时：2 天（D10-D11，2026-03-10～03-11）
- 完成状态：✅ 完成（破坏性测试于 2026-03-12 补全）

---

## 1. 先思考（你的回答写在下面，填完后找我要 TODO 清单）

### Q1：HTTP 方法语义 —— 启停员工用 POST 还是 PUT？

> 本接口是 `POST /admin/employee/status/{status}?id=xxx`
>
> 问题 1：`{status}` 是路径参数，`id` 是查询参数，它们在代码里分别用什么注解接收？
> 问题 2：我们在 2.1 新增员工时用了 `@RequestBody`，这次为什么不用？
> 提示：想想三者分别接收哪种格式的数据（JSON Body / URL路径片段 / URL?后面的键值对）

**你的回答：**

Q1：经过搜索我得知，获取参数的动态路径需要利用spring的@PathVariable 注解，这个注解标注的是资源和状态本身（如status）；另一个@RequestParam 注解通常用于搜索和定位特定对象如id。
Q2:注解@RequestBody的作用是把HTTP中的JSON数据转换为Java对象，而@PathVariable注解的作用是获取URL中的动态参数。我的理解是：新增员工操作中，要的是传递对象给service层，service层不能直接接受JSON数据，而启停员工需要的是得到状态和id等数据，并且直接进行修改，所以没必要转换成对象。（我的理解正确吗？）

---

### Q2：修改操作的自动填充 —— AutoFill 这次会填哪些字段？

> 2.1 新增时，AutoFill 填了：`createTime`、`updateTime`、`createUser`、`updateUser`
> 2.3 修改时，AutoFill 应该只填哪几个字段？为什么另外两个字段不能改？
>
> 追问：`@AutoFill(value = OperationType.UPDATE)` 注解写在哪一层（Controller / Service / Mapper）？
> 为什么不写在 Service 层？（提示：AOP 的切点是基于哪一层定义的？）

**你的回答：**

Q1：应该只填写updatetime和updateUser这两个字段，因为我要做的是修改员工的数据，而不是新增（创建）员工数据，创建的时间和创建人已经是固定的，不需要我们去更改。
Q2：这个注解应该写在Controller层，写在Service层的应该是类似@Transactional的事务管理注解。

---

### Q3：编辑员工的两步接口 —— 为什么"编辑"需要两个接口？

> 编辑功能通常分两步：
> - 第一步：前端跳转到编辑页面，需要 **回显当前数据** → `GET /admin/employee/{id}`
> - 第二步：用户修改完表单，点击保存 → `PUT /admin/employee`（请求体带修改后的数据）
>
> 问题：`getById` 接口返回的是 `Employee` 实体对象还是某个 VO？直接返回 `Employee` 有什么隐患？
> （提示：Employee 里有一个字段你绝对不应该暴露给前端……）

**你的回答：**

Q：如果直接返回Employee对象的话，那么许多不应该出现在前端的数据信息就会暴露了比如"passward"字段，这是不合规范的，也有安全隐患。

---

### Q4：遗留 Bug 修复 —— createTime 为什么返回了数组而不是字符串？

> 2.2 验证时发现：`createTime` 返回 `[2026, 3, 8, 10, 30, 0]` 数组格式，而不是 `"2026-03-08 10:30:00"` 字符串。
>
> 问题 1：这个 bug 在哪一层产生的？（序列化/反序列化在哪里发生？）
> 问题 2：项目里 `sky-common` 的 `json` 包下已经有一个 `JacksonObjectMapper`——它配置了什么？
> 问题 3：为什么配置了 `JacksonObjectMapper` 但还是不生效？缺少了哪一步注册动作？
> （提示：去看 `WebMvcConfiguration`，找找哪个方法是用来扩展消息转换器的）

**你的回答：**

Q1：为什么会是数组格式呢？如果是序列化的问题，我想起了@RequestBody这个注解，也想到了Result类实现的Serializable接口也有序列化的概念，那么应该出在哪里呢》会不会是Controller层？可是我没有明确看到createTime相关的信息，那么也有可能在ServiceImpl层里面吧，是不是那个LocalDateTime.now()有问题？
Q2：这是一个对象映射器，可以将Java对象转为json或者将json转为Java对象，也就是所谓的序列化和反序列化，看起来这里这个问题是Mapper层的，里面的方法都和时间格式有关。
Q3：注册没有生效吗？我想起了WebMvcConfiguration类有一个注册拦截器的操作，查看代码发现这个这个JacksonObjectMapper类没有类似addInterceptors的注册方法，是不是应该加上一个注册方法呢？

---

## 2. 请求流转伪代码

```text
// 启停员工
POST /admin/employee/status/{status}?id=xxx
  → JwtInterceptor 校验 token
  → EmployeeController#startOrStop(@PathVariable Integer status, @RequestParam Long id)
      → employeeService.startOrStop(status, id)
          → new Employee().setId(id).setStatus(status).setUpdateTime/User
          → employeeMapper.update(employee)  // 动态 SQL <set>
      → return Result.success()

// 根据ID查询员工
GET /admin/employee/{id}
  → EmployeeController#getById(@PathVariable Long id)
      → employeeService.getById(id)
          → employeeMapper.getById(id)  // SELECT * WHERE id=#{id}
      → return Result.success(employee)

// 编辑员工
PUT /admin/employee
  → EmployeeController#update(@RequestBody EmployeeDTO employeeDTO)
      → employeeService.update(employeeDTO)
          → BeanUtils.copyProperties(employeeDTO, employee)
          → employee.setUpdateTime/User
          → employeeMapper.update(employee)  // 复用同一个动态 SQL
      → return Result.success()
```

## 3. 边界条件（至少 4 条）
- [ ] 启停时传入不存在的员工 ID
- [ ] 启停时 status 传入非 0/1 的非法值
- [ ] 编辑时 username 改成已存在的其他员工用户名（唯一键冲突）
- [ ] 编辑时不传 id（id 为 null）
- [ ] （补充你想到的）

## 4. 破坏性测试计划
- 测试 1：禁用管理员账号（id=1）→ 观察返回值（正常还是报错？业务上应该怎么处理？）
- 测试 2：启停时传 status=2（非法值）→ 后端应有校验吗？
- 测试 3：编辑时故意把 username 改成已存在的用户名 → 期望返回 code:0 + 提示信息
- 测试 4：`GET /admin/employee/{id}` 传一个数据库不存在的 id → 返回什么？

## 5. 验证证据（Apifox，2026-03-11～03-12）
- [ ] 启停员工：正常路径（待补：URL 写错 /admin/employee/ 前缀，明天重测）
  - ⚠️ 已知 Bug：发请求 POST /status/0 漏写前缀，404；代码无误，改 URL 即可
- [ ] 启停员工：破坏性测试（待补）
- ✅ 根据ID查询员工：GET /admin/employee/2 → code:1，员工详情正确返回
- ✅ 编辑员工：PUT /admin/employee，Body 含 id=3 → code:1，data:null
- [ ] 编辑员工：username 冲突破坏性测试（待补）
- ✅ createTime 格式修复：GET /admin/employee/page → createTime:"2026-03-08 14:25" 字符串格式正确

## 6. 完成定义
- [x] 不看参考源码可写出 startOrStop / getById / update 全链路
- [x] 能解释 @PathVariable vs @RequestParam vs @RequestBody 三者区别
- [x] 能解释 AutoFill AOP 为何切在 Mapper 层而非 Service 层（自定义注解需另行实现，当前手动设置等效）
- [x] createTime 序列化 Bug 已修复并验证
- [ ] 启停员工 Apifox 正常路径验证（修复 URL 后补）
- [ ] 至少 1 条 RCA 已写入 03-RCA故障复盘（遗留：下次开始前补写）

## 7. 可迁移模式
- **动态 SQL 通用 update 模式**：单个 `update(Entity)` 方法 + `<set><if>` 组合，同时服务于「局部字段更新（启停）」和「全字段更新（编辑）」，避免写多个 Mapper 方法。
- **`extendMessageConverters` 注册模式**：继承 `WebMvcConfigurationSupport` 后，override `extendMessageConverters`，将自定义 `MappingJackson2HttpMessageConverter` 插到 index=0，覆盖默认 Jackson 序列化行为。可迁移到任何需要自定义日期/枚举序列化的 Spring Boot 项目。
