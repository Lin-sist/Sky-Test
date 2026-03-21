# Spring-Boot项目结构
这个文档旨在帮助我在局部编码后，整体理解一下项目概况，从局部视野过渡到全局视野。

## 模式名称
Spring Boot项目结构（Controller-Service-Mapper结构）


每次做TODO，我都是:
写Controller层的代码 -> 定义Service层接口 -> 在ServiceImpl实现具体方法 -> 写Mapper层接口 -> 在Mapper.xml写具体的动态SQL

## 2. 我的疑问

>Q1: Controller-Service-Mapper这种结构是sprigboot的项目规范吗？企业里的springboot项目是怎么样的？

A1:
不是 Spring Boot 的"官方强制规范"，而是 Java 后端里最常见的分层实践之一。

你现在这套写法本质是经典三层：
- Controller：接收 HTTP 请求、参数校验、返回统一响应
- Service：承载业务规则与事务边界
- Mapper：只做数据访问（SQL）

企业里常见 3 种形态：
- 中小项目：Controller-Service-Mapper（你现在这种）
- 复杂业务：在 Service 内再拆 DomainService、ApplicationService、Repository（DDD 风格）
- 微服务：每个服务内部仍分层，但在服务之间再通过 RPC/消息队列协作

技术演进脉络：
- Naive：Controller 里直接写 SQL 或直接调 Mapper
- Standard：Controller-Service-Mapper，职责清晰，可维护
- Advanced：领域分层 + 事件驱动 + 可观测性（Trace/Metrics/Log）

为什么不把业务直接写 Controller（灾难场景）：
- 当接口从 5 个涨到 50 个时，Controller 会变成"巨石类"，同一业务规则复制粘贴到多个接口，改一处漏三处。

🔥 面试高频追问
1. 为什么事务通常放在 Service 层而不是 Controller 层？

答：根据搜索得知，将事务放在Service层，则spring的@Transactional会帮忙管理事务，将事务打包成完整的整体，保证事务要么全部成功要么全部失败，有效的防止了数据混乱的错误。具体如何实现？假如用户给到的请求需要执行3条SQL语句，只有当3条SQL语句都执行成功时，spring才会通知数据库“全部生效（commit）”，这时数据库的数据才会同步改变。如果在执行某条SQL语句时出现错误抛出了异常，那么就会背spring的AOP拦截器捕获，然后执行“全部回滚（rollback）”，数据库的数据不会修改，恢复如初。

2. Service 层能不能直接返回 Entity 给前端？DTO/VO 的边界怎么定？

答：不能直接返回entity，实体类是在service/mapper层与数据库交互时使用的，entity通常包含数据库的所有相关字段。如果直接用entity返回给前端，那么如password等敏感字段会泄露给前端，有安全风险。DTO是数据传输对象，通常由前端传递给Controller层，只承载Controller需要的字段。VO是视图对象，通常由Service层转换后传递给Controller层后传递给前端，承载的是想展示给前端的数据字段。

3. 

---

>Q2：我注意到项目大体分为3个文件夹：sky-common,sky-pojo,sky-server等。他们3个文件夹分别的职责是什么？在企业规范里面，common和pojo里面的内容是要提前设计并编写好的吗？

A2：
这 3 个模块是典型的 Maven 多模块拆分：

- sky-common：跨模块复用能力
	- 例如：`BaseContext`、`JwtUtil`、常量类、统一返回结构、异常等
	- 价值：避免重复代码，统一规范

- sky-pojo：数据模型层
	- 例如：DTO（入参）、Entity（数据库映射）、VO（出参）
	- 价值：让接口契约和数据库模型分离，避免互相污染

- sky-server：应用启动与业务落地
	- 例如：Controller、Service、Mapper、配置、拦截器
	- 价值：把"可复用能力"和"具体业务实现"隔离

common/pojo 要不要提前一次性写完？
- 不建议一次性全设计完。
- 推荐"最小可用 + 演进"：先满足当前业务，再按复用频率沉淀到 common 或 pojo。

技术演进脉络：
- Naive：所有类都放一个模块，短期快，长期乱
- Standard：common/pojo/server 分层，复用与业务解耦
- Advanced：再拆 api、domain、infrastructure，支持团队并行开发

---

>Q3: 以当前项目结构的登录请求为例，给我画一个流程图，重点告诉我sky-common,sky-pojo,sky-server中的哪些具体类具体方法参与了这个请求操作。

A3:
下面用你当前项目真实类名说明登录全链路：

```mermaid
flowchart TD
	A[前端 POST /admin/employee/login] --> B[sky-server: EmployeeController.login]
	B --> C[sky-pojo: EmployeeLoginDTO 反序列化]
	B --> D[sky-server: EmployeeServiceImpl.login]
	D --> E[sky-server: EmployeeMapper.getByUsername]
	E --> F[(MySQL employee 表)]
	D --> G[密码比对: DigestUtils.md5DigestAsHex]
	B --> H[sky-common: JwtUtil.createJWT]
	H --> I[claims 放入 EMP_ID]
	B --> J[sky-pojo: EmployeeLoginVO]
	J --> K[返回 Result.success(vo)]
```

登录后访问其他 `/admin/**` 接口时，还会走：
- `sky-server/interceptor/JwtTokenAdminInterceptor.preHandle`
- `sky-common/JwtUtil.parseJWT`
- `sky-common/BaseContext.setCurrentId`

关键代码片段（登录生成 token）：

```java
Map<String, Object> claims = new HashMap<>();
claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
String token = JwtUtil.createJWT(
		jwtProperties.getAdminSecretKey(),
		jwtProperties.getAdminTtl(),
		claims);
```

为什么这样设计：
- Controller 负责协议层（HTTP）
- Service 负责业务判断（账号、密码、状态）
- common 负责通用基础设施（JWT 工具、上下文）
- pojo 负责数据载体（DTO/VO/Entity）

---

>Q4: 重点聚焦在sky-server这个文件夹，(config-controller-handler-interceptor-mapper-service-service/impl-resources-mapper-target)这些文件夹里面的内容各种的职责是什么？在项目中有什么作用？重点是告诉我为什么这样设计。

A4:
`sky-server` 各目录职责如下：

- config
	- 放 Spring 配置类（如 `WebMvcConfiguration`）
	- 作用：统一注册拦截器、消息转换器、文档配置

- controller
	- 放接口入口（如 `EmployeeController`）
	- 作用：接收请求、调用 Service、返回 Result

- handler
	- 常见放全局异常处理器（如 `GlobalExceptionHandler`）
	- 作用：把异常统一转换成前端可识别响应

- interceptor
	- 放请求拦截器（如 `JwtTokenAdminInterceptor`）
	- 作用：在业务方法前做鉴权、上下文注入

- mapper
	- 放 MyBatis 接口（如 `EmployeeMapper`）
	- 作用：定义数据库访问方法

- service / service/impl
	- service 放接口，impl 放实现
	- 作用：隔离"能力定义"与"具体实现"，便于测试与替换

- resources/mapper
	- 放 MyBatis XML（动态 SQL）
	- 作用：SQL 与 Java 代码解耦，复杂查询更清晰

- target
	- Maven 编译输出目录
	- 作用：存放 class、资源拷贝结果，运行依赖它

为什么这样设计（核心）：
- 低耦合：改 SQL 时通常不动 Controller
- 高内聚：同一层关注同一类问题
- 易测试：Service 可以单测，Controller 可以做接口测
- 易扩展：后续新增模块可复制同样骨架

🔥 面试高频追问
1. 为什么 Mapper 接口和 XML 要分离？注解 SQL 不行吗？

答：因为Mapper是定义数据库访问方法的，而Mapper.xml写的是动态SQL，定义的是查数据库时的具体SQL逻辑，两者职责不同，分离有助于划分职责。注解SQL通常和Java代码放在一起，分散在各个接口中不便于管理。

2. 拦截器、过滤器、AOP 三者在请求链路中的位置和区别？

答：拦截器在Spring MVC,通过URL路径来判断用户是否要权限访问某个Controller接口；过滤器在项目最外层，处理与业务无关的的底层协议；AOP深入到方法执行前后，哪些重复度高的业务操作。

---

>Q5: 我注意到sky-common,sky-pojo,sky-server等文件夹中都有target目录和pom.xml文件，项目根目录也有一共pom.xml文件，它们都是干什么用的？

A5:
这是 Maven 多模块项目的标准结构：

- 每个子模块自己的 `pom.xml`
	- 定义该模块的依赖、打包方式、插件
	- 例如 `sky-server` 依赖 `sky-common`、`sky-pojo`

- 根目录 `pom.xml`
	- 作为父工程与聚合工程
	- 管理统一版本（dependencyManagement）和模块列表（modules）

- 每个模块的 `target`
	- 构建输出目录（编译产物）
	- 包含 `.class`、复制后的资源文件、临时构建文件

一句话理解：V
- `pom.xml` 是"构建说明书"
- `target` 是"构建产物仓库"

为什么不能把 target 提交到 git：
- 它是可再生文件，提交后会导致仓库膨胀、冲突频发、跨平台脏差异。

可记到 Obsidian 的关键词：
- Maven Reactor：多模块统一构建机制
- dependencyManagement：统一依赖版本管理
- WebMvcConfigurationSupport：Spring MVC 扩展入口

---