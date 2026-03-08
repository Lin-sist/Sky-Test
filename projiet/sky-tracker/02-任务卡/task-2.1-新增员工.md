# 任务卡：2.1 新增员工

## 0. 基本信息
- 任务名称：新增员工
- 对应接口/方法：`EmployeeController#save` → `EmployeeServiceImpl#save` → `EmployeeMapper#insert`
- 预计耗时：2~3 天（每天 1 小时）
- 实际耗时：6 天（D3~D8，含概念理解与多次 RCA）
- 完成状态：✅ 完成（2026-03-08）

## 1. 先思考（你的回答写在后面）
- 在 EmployeeServiceImpl#save 方法里，你需要给新员工设置"创建人ID"（createUser 字段）。你打算怎么拿到"当前登录用户的 ID"？请说出你的思路，不需要写代码，用一句话描述方案就行。

答：在员工登录时，claims变量通过employee.getId()方法获得了员工ID，这个变量包含在token变量里面，token变量是JWT令牌的组成部分，可以在进行新增员工操作之前通过JWT拦截器校验jwt令牌，通过Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());得到empId员工ID。
```
HTTP请求到达
  → JwtTokenAdminInterceptor.preHandle()
      → 解析JWT，拿到 empId
      → BaseContext.setCurrentId(empId)   ← ⚠️ 这步你漏了！
  → 请求放行，进入 Controller/Service 层
      → EmployeeServiceImpl.save()
          → BaseContext.getCurrentId()    ← 在这里取，而不是再解析JWT
```

- 新员工的默认密码是写死的 "123456"，但存进数据库之前需要处理一下。为什么不能直接把 "123456" 字符串存进 password 字段？应该怎么处理？
这个处理的目的是什么？它能防住哪种攻击？

  答（纠正版）：这与传输安全无关（传输安全靠 HTTPS）。核心目的是**防数据库拖库后明文密码泄露**。
  MD5 是**单向哈希**（不是加密），存库的是哈希值，即使数据库被 dump 也无法反推原文。
  登录时：前端传密码 → 后端 MD5 → 与库中哈希比对，无需解密。
  ⚠️ 裸 MD5 存在彩虹表攻击风险（预先计算常见密码的 MD5），生产环境应使用 **BCrypt（自带盐值）**。

---

- HTTP 方法用 @PostMapping，路径是什么？（提示：类上已有 @RequestMapping("/admin/employee")，方法级别空路径意味着什么？）

答：路径参照@RequestMapping("/admin/employee")中的参数路径，这里代表的是总路径。方法上的路径如@PostMapping("/save")代表专门处理save方法的post请求。

- 参数前要加什么注解才能从请求体读取 JSON？

答：@RequestBody注解，作用为把HTTP请求体中的JSON数据转换为Java对象。

---

```
1. 创建 Employee 对象
2. 把 DTO 的同名字段拷贝到 Employee   ← 用哪个工具类？
3. 设置默认密码（MD5哈希）            ← 参考 login() 里怎么做的
4. 设置账号状态（默认启用）            ← 去找 StatusConstant
5. 设置创建时间、更新时间              ← LocalDateTime.now()
6. 设置创建人、更新人 ID              ← 从哪取？（回忆上次讨论）
7. 调用 Mapper 插入
```

- 第2步，BeanUtils.copyProperties(a, b) — a 是 DTO 还是 Entity？顺序反了会怎样？

答：经过搜索得知：BeanUtils.copyProperties(a, b) 是一个对象属性拷贝工具方法。它的作用是：把对象 a 中同名的属性值复制到对象 b 中。即：把 employeeDTO 对象中的属性值复制到 employee 对象中。只要 属性名相同、类型兼容，就会被复制。

应该是先DTO再Entity,如果反了，会怎么样？

- 第6步，当前登录人 ID 不是从参数传进来的，从哪取？去看 BaseContext 这个类。

答：大概率是通过BaseContext中的getCurrentId()方法取到的，可是具体是怎么做到的呢？我不清楚。

---
- 参考已有的 getByUsername，它用了 @Select 注解直接写 SQL，你的 insert 也用注解方式。

答：我猜测@Select和@Insert是mybaties的注解，那么后面的@Insert()括号内应该填写什么字段呢？在项目中从何得知？

- SQL 里字段名是 id_number（下划线），但 Java 属性名是 idNumber（驼峰），MyBatis 怎么知道对应关系？去看 application.yml 里有没有配置 map-underscore-to-camel-case。

答：配置文件中关于mybatis写了configuration:#开启驼峰命名map-underscore-to-camel-case: true
所以mybatis可以知道id_number就是Java中的属性名idNumber。

---
- 这个异常的 getMessage() 内容类似：Duplicate entry 'zhangsan' for key 'employee.idx_username'，怎么从这个字符串里提取 zhangsan？（提示：String.split("'")）

答：可以通过String.split("'")方法，进行分割字符串（具体的原理我解释不清楚）

- 友好提示的文案 "已存在" 应该放在哪里，而不是硬编码在 Handler 里？

答：这个确实不知道。

---

## 2. 请求流转伪代码（先写再编码）
```text
Controller 接收 DTO
  -> 转换为 Entity
  -> 调用 Service.save
Service.save
  -> 设置默认密码(加密)
  -> 设置状态与通用字段
  -> 调用 Mapper.insert
Mapper.insert
  -> 执行 insert SQL
```

## 3. 边界条件（至少命中 4 条）
- [ ] username 重复
- [ ] 空用户名或非法手机号
- [ ] 创建人ID为空（ThreadLocal 取值失败）
- [ ] 数据库字段约束触发

## 4. 破坏性测试计划
- 测试 1：构造重复用户名，观察异常处理返回
- 测试 2：手动让 `BaseContext.getCurrentId()` 为空，观察行为
- 测试 3：删除必要字段，观察 DB 约束反馈

## 5. 完成定义
- [x] 不看参考源码可写出 save 全链路
- [x] 能解释默认密码策略风险（裸 MD5 vs BCrypt）
- [x] 已产出 RCA（见 rca-阶段A.md：DTO类型错误、静态调用错误、时间类型错误、ThreadLocal用法错误）
- [x] 已提炼可迁移模式：DTO白名单 / ThreadLocal隐式传递 / 全局异常分层处理

## 6. 验证证据（Apifox，2026-03-08）
- ✅ 正常路径：POST /admin/employee → `{"code":1,"msg":null,"data":null}`
- ✅ 破坏性测试：重复 username → `{"code":0,"msg":"test001已存在","data":null}`
