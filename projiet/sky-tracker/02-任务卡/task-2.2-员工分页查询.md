# 任务卡：2.2 员工分页查询

## 0. 基本信息
- 任务名称：员工分页查询
- 对应接口/方法：`GET /admin/employee/page` → `EmployeeController#pageQuery` → `EmployeeServiceImpl#pageQuery` → `EmployeeMapper#pageQuery`
- 预计耗时：2 天（每天 1 小时）
- 实际耗时：1天（D9，2026-03-09）
- 完成状态：✅ 完成（2026-03-09）

---

## 1. 先思考（你的回答写在下面，填完后找我要 TODO 清单）

### Q1：不用 PageHelper，纯手写分页会碰到哪些麻烦？

> 提示：分页查询需要两件事：① 返回"当前页的数据" ② 返回"总条数"。
> 如果完全手写，你需要在 SQL 里写什么？每增加一个新的分页接口，你要重复做什么工作？

答：如果手写分页查询，需要执行类似select * (表名) order by (字段) limit 10 offset 10;的语句。每次新增一个分页接口，都需要重新写多个这样的SQL语句，十分繁琐，代码臃肿。

---

### Q2：动态 SQL——name 和 status 都是可选过滤条件

> 场景：前端传来的请求参数可能是：
> - 只传了 name="张三"（不传 status）
> - 只传了 status=1（不传 name）
> - 两个都没传（查全部）
>
> 问题：如果你在 Mapper 里写死 `WHERE name LIKE '%张三%' AND status = 1`，当前端只传 name 时会发生什么？
> 应该用什么技术解决"字段可选"的问题？（提示：MyBatis 有一个 XML 标签专门处理这个）

答：当前端传来的字段不全时，可能会查询不到相关的数据，发生报错。应该用什么技术解决"字段可选"的问题？这个目前不太了解，但是我找到了EmployeeMapper.xml文件，但是不明白是干嘛的。

---

### Q3：分页结果用什么 VO 返回？

> 前端需要知道：① 当前页的员工列表 ② 总共有多少条记录（用于渲染分页组件）
> 去 sky-pojo 里找一找，有没有合适的 VO 类？它有哪两个字段？

答：我似乎没有在vo中找到合适的视图，应该要包含员工的id和当前页面员工的数量。

---

## 2. 请求流转伪代码
```text
GET /admin/employee/page?page=1&pageSize=10&name=xx&status=1
  → JwtInterceptor 校验 token
  → EmployeeController#page(EmployeePageQueryDTO)
      → employeeService.pageQuery(dto)
          → PageHelper.startPage(page, pageSize)  // 把分页参数存入 ThreadLocal
          → employeeMapper.pageQuery(dto)          // MyBatis 拦截器自动加 LIMIT + COUNT(*)
          → List<Employee> 强转 Page<Employee>
          → return new PageResult(total, records)
      → return Result.success(pageResult)
```

## 3. 边界条件（至少 4 条）
- [ ] name 和 status 都不传 → 返回全部员工
- [ ] name 模糊匹配（输入"张"能命中"张三"）
- [ ] pageSize 超大时的性能风险
- [ ] status 传入非 0/1 的非法值
- [ ] （补充你想到的）

## 4. 破坏性测试计划
- 测试 1：只传 name，不传 status → 结果应只按 name 过滤
- 测试 2：只传 status=0（禁用），不传 name → 结果应只返回禁用员工
- 测试 3：page=1, pageSize=1 → 验证分页确实生效（数据库有多条时，只返回 1 条）
- 测试 4：name 传空字符串 → 行为应等同于不传

## 5. 验证证据（Apifox，2026-03-09）
- ✅ 正常路径：GET /admin/employee/page?page=1&pageSize=10 → code:1, data.total=5, 5条员工数据
- ✅ 破坏性测试1：?name=张（数据库无含"张"员工）→ total:0, records:[] 正确返回空
- ✅ 破坏性测试2（待补）：?page=1&pageSize=1 → 需修复 Apifox 前置URL后重测
- ⚠️ 待观察：createTime 以数组形式返回，需注册 JacksonObjectMapper 修复（WebMvcConfiguration#extendMessageConverters）

## 6. 完成定义
- [x] 不看参考源码可写出 pageQuery 全链路
- [x] 能解释 PageHelper 的工作原理（拦截器 + ThreadLocal）
- [x] 能解释动态 SQL `<if>` 标签的作用
- [x] 测试通过，Apifox 正常路径 + 破坏性测试验证（name 模糊为空、空结果集均正确）
- [x] 已补 RCA：status 字段缺失（Integer vs int）
