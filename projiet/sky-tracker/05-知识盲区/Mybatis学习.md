# Spring-Boot项目结构
这个文档旨在帮助我在局部编码后，理解学习什么是Mybatis，了解什么是持久层框架。

## 模式名称
Mybatis学习

## 1.我的疑问

> Q1：我注意到项目中包含许多mapper类和mapper.xml文件以及@Mapper注解，这些都是持久层框架规定的吗？

A1：
是 MyBatis 的常见组织方式，但不是唯一写法。

你现在项目里的组合是：
- `EmployeeMapper.java`：定义数据库操作方法签名
- `EmployeeMapper.xml`：写 SQL（含动态 SQL）
- `@Mapper`：告诉 Spring 这是 MyBatis 映射接口，需要生成代理对象

要点：
- MyBatis 本质是"接口 + SQL 映射"
- SQL 可以写在 XML，也可以写在注解（如 `@Select`）
- 复杂 SQL 更推荐 XML，可读性更好

技术演进脉络：
- Naive：JDBC 手写连接、拼 SQL、手动封装结果集
- Standard：MyBatis Mapper + XML
- Advanced：MyBatis-Plus / JOOQ / 分库分表中间件

---

> Q2：为什么使用mybatis？如果不用mybatis，项目代码改如何组织？

A2：
为什么用 MyBatis：
- SQL 可控：复杂查询、动态条件、性能调优更直接
- 成本适中：比纯 JDBC 少大量模板代码
- 与 Spring 集成成熟：事务、异常体系、配置都顺滑

如果不用 MyBatis，常见替代：
- 方案1：纯 JDBC
	- DAO 层会充满连接管理、PreparedStatement、ResultSet 映射代码
- 方案2：Spring Data JPA/Hibernate
	- 开发快，但复杂 SQL 与性能调优时可控性不如 MyBatis

对比结论：
- 业务 SQL 较复杂、需要精细控制：优先 MyBatis
- 简单 CRUD 为主、追求开发速度：JPA 也可

为什么不做 ORM 全自动（灾难场景）：
- 当查询跨多表且条件动态变化时，若只依赖自动生成 SQL，可能出现慢 SQL 且难以定位。

---

> Q3：项目中有哪些地方是用到了Mybatis的？以后推进项目又有哪些地方需要用到mybatis?

A3：
你当前项目已使用 MyBatis 的位置：
- 员工登录：`EmployeeMapper.getByUsername`
- 员工分页：`EmployeeMapper.pageQuery` + `EmployeeMapper.xml` 动态 SQL
- 员工启停/编辑：`EmployeeMapper.update` + `<set><if>`
- 员工新增/按 id 查：`insert`、`getById`（在 Mapper 接口中）

后续推进时几乎所有"落库"能力都离不开 MyBatis，例如：
- 分类、菜品、套餐、订单等模块的 CRUD
- 条件检索、分页、统计报表
- 批量更新、逻辑删除、状态流转

建议你形成固定判断：
- 只要涉及数据库读写，就先想：DTO/Entity 边界、Mapper 方法签名、SQL 是否需要动态条件。

🔥 面试高频追问
1. `#{}` 和 `${}` 区别是什么？为什么 `${}` 有注入风险？

答：#{}是预编译的方式，用占位符?代替参数，更加安全，也可以防止SQL注入；${}是字符拼接，不安全，存在SQL注入风险。前者类似于JDBC的PrepareStatement，后者则像Statement。#{}会把带占位符的模板发送给数据库，数据库会先编译这个模板，然后再查找真实的数据，而不会有SQL注入那样的风险。

2. MyBatis 一级缓存生效范围是什么？跨 SqlSession 生效吗？

答：经过搜索，Mybatis的一级缓存生效范围是sqlsession级别的，不能跨sqlsession生效。mybatis底层维护了一个HashMap用于存储查询结果。如果执行相同的SQL语句，mybatis首先会在一级缓存查询，如果命中，则直接返回结果，而不查询数据库。如果未命中，则查询数据库后将结果返回，再记入缓存中供下次使用。

可记到 Obsidian 的关键词：
- SqlSession
- 动态 SQL（`<where> <if> <set>`）
- Mapper 代理机制

---