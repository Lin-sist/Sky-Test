# RCA 故障复盘模板（Root Cause Analysis）

---

# 报错：EmployeeDTO cannot be resolved to a type！

## 1. 事故摘要
- 日期：03-03
- 功能点：接口新增save方法声明
- 现象（用户看到什么）：EmployeeDTO cannot be resolved to a type！

## 2. 证据链（必须有）
- 相关代码位置：EmployeeService:16行
- 触发接口与参数：Employee save(EmployeeLoginDTO employeeLoginDTO);
- 实际上是没有导入相关包，我的参数也写错了，应该是EmployeeDTO employeeDTO

## 3. 修复方案
- 导入相关的包：import com.sky.dto.EmployeeDTO;

---

# 报错：Cannot make a static reference to the non-static method save(EmployeeDTO) from the type EmployeeService!

## 1. 事故摘要
- 日期：03-03
- 功能点：调用EmployeeService.save(employeeDTO);
- 现象（用户看到什么）：# Cannot make a static reference to the non-static method save(EmployeeDTO) from the type EmployeeService!

## 2. 证据链（必须有）
- 触发接口与参数：EmployeeService.save(employeeDTO);
- 相关代码位置：EmployeeController:76行
- 我最初的错误判断：我的save方法的声明写错了，但是我不知道哪里错了？

```java
public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    public void save(EmployeeDTO employeeDTO);

}
```

从EmployeeService.save改为employeeService.save就没问题了，怎么回事？
```java
@PostMapping("/save")
    public Result<String> save(@RequestBody EmployeeDTO employeeDTO) {
        log.info("新增员工：{}", employeeDTO);

        employeeService.save(employeeDTO);

        return Result.success();
    }
```

## 3.修复方案
- 根因：EmployeeService代表的是一个类，而employeeService代表的是一个对象；类调用的是静态方法，对象调用的是实例方法。类是就像一个叫“厨师”的职称，职称是不会炒菜的，一个活生生的“人”从会炒菜。即必须向一个对象下命令，对象才可以真正调用执行方法。**在 Java 中，只有当一个方法被 static 关键字 修饰时，才能直接通过类名调用。**
- 具体代码：在EmployeeService类中可以看到这是一个接口，下面的save方法没有static声明，当然不可以直接调用。
```java
    public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    void save(EmployeeDTO employeeDTO);     //接口方法默认为public所以不需要额外添加

}
```
- 修复：将EmployeeService.save改为employeeService.save

---

# 报错：The type EmployeeServiceImpl must implement the inherited abstract method EmployeeService.save(EmployeeDTO)Java(67109264)

## 1. 事故摘要
- 日期：03-08
- 功能点：新增EmployeeServiceImpl的save方法
- 现象（用户看到什么）：The type EmployeeServiceImpl must implement the inherited abstract method EmployeeService.save(EmployeeDTO)Java(67109264)

## 2. 证据链（必须有）
- 相关代码位置：EmployeeServiceImpl：19行
- 触发接口与参数：public class EmployeeServiceImpl implements EmployeeService
- 我最初的错误判断：没有导入save方法参数的包：import com.sky.dto.EmployeeDTO;

---

# 报错：The method setCreateTime(LocalDateTime) in the type Employee is not applicable for the arguments (long)Java(67108979)

## 1. 事故摘要
- 日期：03-08
- 功能点：设置创建时间、更新时间 
- 现象（用户看到什么）：The method setCreateTime(LocalDateTime) in the type Employee is not applicable for the arguments (long)Java(67108979)

## 2. 证据链（必须有）
- 相关代码位置：EmployeeServiceImpl：79-80行
- 触发接口与参数：看到Employee类中的private LocalDateTime createTime;createTime的字段类型为LocalDateTime，所以需要使用LocalDateTime.now()而不是System.currentTimeMillis()

```java
    //设置默认状态为启用
    employee.setStatus(StatusConstant.ENABLE);
    //设置创建时间和更新时间
    employee.setCreateTime(LocalDateTime.now());
    employee.setUpdateTime(LocalDateTime.now());
```

---

# 报错：The method pageQuery(PageQueryDTO) from the type EmployeeService refers to the missing type PageQueryDTOJava(67108984)

## 1. 事故摘要
- 日期：03-09
- 功能点：员工分页查询
- 现象（用户看到什么）：The method pageQuery(PageQueryDTO) from the type EmployeeService refers to the missing type PageQueryDTOJava(67108984)

## 2. 证据链（必须有）
- 相关代码位置：EmployeeController：93行
- 触发接口与参数：pageQuery方法报错了
- 我最初的错误判断：service层的接口方法声明没写好

```java
    @GetMapping("/page")
    public Result<PageResult> page(EmployeePageQueryDTO pageQueryDTO) {
        log.info("员工分页查询：{}", pageQueryDTO);
        
        employeeService.pageQuery(pageQueryDTO);

        return Result.success();

    }
```
EmployeeServie的代码当时是这样的：PageQueryDTO参数错了，应该是EmployeePagQuery，而且我还没有导入这个包，所以这里同样报了“PageQueryDTO cannot be resolved to a type！”的错误。
```java
    PageResult pageQuery(PageQueryDTO pageQueryDTO);
```

---

# 报错：Cannot make a static reference to the non-static method pageQuery(EmployeePageQueryDTO) from the type EmployeeMapper

## 1. 事故摘要
- 日期：03-09
- 功能点：员工分页查询实现方法
- 现象（用户看到什么）：Cannot make a static reference to the non-static method pageQuery(EmployeePageQueryDTO) from the type EmployeeMapper

## 2. 证据链（必须有）
- 相关代码位置：EmployeeServiceImpl：101行

```java
     EmployeeMapper.pageQuery(pageQueryDTO);
```

## 3.修复方案
- 根因：还是老问题，这是一个静态调用，EmployeeMapper是一个类，不能调用非静态方法。pageQuery是一个实例方法，只可以通过实例对象来调用。
（方法声明没有static）
```java
    List<Employee> pageQuery(EmployeePageQueryDTO pageQueryDTO);
```

---

# 报错：Type mismatch: cannot convert from Result<PageResult> to PageResult

## 1. 事故摘要
- 日期：03-09
- 功能点：员工分页查询实现
- 现象（用户看到什么）：Result.success(pageResult);整段有红色波浪报错。

## 2. 证据链（必须有）
- 相关代码位置：EmployeeServiceImpl：110行

```java
    return Result.success(pageResult);
```

## 3.修复方案
- 根因：错误的很离谱。提示类型不匹配，看一下方法声明的是PageResul，而返回的类型是Result。 方法Result.success()应该在 Controller 层调用，Service 层只负责返回业务数据本身。Service 不知道也不应该关心"怎么包装给前端"。
```java
    public PageResult pageQuery(EmployeePageQueryDTO pageQueryDTO)
```
修复：
```java
    return new PageResult(page.getTotal(), page.getResult());
```

---

# 报错：There is no getter for property named 'status' in 'class com.sky.dto.EmployeePageQueryDTO'

## 1. 事故摘要
- 日期：03-09
- 功能点：测试分页查询功能
- 现象（用户看到什么）：在APIfox发送GET:http://localhost:8080/admin/employee/page 时，返回了500报错。

## 2. 证据链（必须有）
- 相关代码位置：EmployeePageQueryDTO：14行
- 触发接口与参数：
- 我最初的错误判断：

```java
    // 账号状态 0-禁用 1-启用（Integer 而非 int，不传时为 null，动态 SQL 条件不生效）
    private Integer status;
```

## 3. 修复方案
- 根因：`EmployeePageQueryDTO` 里缺少 `status` 字段，MyBatis XML 的 `<if test="status != null">` 找不到对应的 getter 方法，直接抛 `ReflectionException`。
- 核心教训：**DTO 字段必须与 XML 动态 SQL 中引用的属性名完全对应**。写 XML 之前先确认 DTO 字段清单。
- 次要教训：`status` 字段类型必须用 `Integer`（包装类），而不是 `int`（基础类型）。`int` 默认值是 0，不传时 `<if test="status != null">` 永远为 true，导致只查状态为 0 的员工。`Integer` 默认值是 `null`，不传时条件不生效，符合预期。
- 修复：在 `EmployeePageQueryDTO` 中补充字段：
```java
    private Integer status;
```

---

# 报错：The method extendMessageConverters(JacksonObjectMapper) is undefined for the type InterceptorRegistration

## 1. 事故摘要
- 日期：03-10
- 功能点：修复createTime 返回数组格式为数组而非时间字符串的Bug
- 现象（用户看到什么）：添加这个extendMessageConverters方法后就有红波浪线报错。

## 2. 证据链（必须有）
- 相关代码位置：WebMvcConfiguration：38行
- 触发接口与参数：.extendMessageConverters(new JacksonObjectMapper())

```java
 protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .extendMessageConverters(new JacksonObjectMapper())
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");
                
    }
```

## 3. 修复方案
- 根因：把 WebMvcConfigurationSupport 的 override 方法误写成 InterceptorRegistration 的链式调用
- 预防：override 方法应独立定义，和 addInterceptors 平级，不能链式调用

---

# 报错：AutoFill cannot be resolved to a typeJava(16777218)

## 1. 事故摘要
- 日期：03-10
- 功能点：在 EmployeeMapper 接口的update方法上加 @AutoFill(value = OperationType.UPDATE)

## 2. 证据链（必须有）
- 相关代码位置：EmployeeMapper：45行
- 触发接口与参数：@AutoFill(value = OperationType.UPDATE)
- 我最初的错误判断：没有导入正确的包，可是我导入import com.sky.annotation.AutoFill;或者import org.apache.ibatis.annotations.AutoFill;都没用。

```java
    @AutoFill(value = OperationType.UPDATE)
    void update(Employee employee);
```

## 3. 修复方案
- 根因：@AutoFill 是项目自定义注解，需自行实现，MyBatis 原生不提供
- 预防：使用第三方注解前，先确认它属于哪个包/是否需要自己实现

---

# 报错：

## 1. 事故摘要
- 日期：
- 功能点：
- 现象（用户看到什么）：

## 2. 证据链（必须有）
- 相关代码位置：
- 触发接口与参数：
- 我最初的错误判断：

```java

```

---

# 报错：

## 1. 事故摘要
- 日期：
- 功能点：
- 现象（用户看到什么）：

## 2. 证据链（必须有）
- 相关代码位置：
- 触发接口与参数：
- 我最初的错误判断：

```java

```

---

# 报错：

## 1. 事故摘要
- 日期：
- 功能点：
- 现象（用户看到什么）：

## 2. 证据链（必须有）
- 相关代码位置：
- 触发接口与参数：
- 我最初的错误判断：

```java

```

---

# 报错：

## 1. 事故摘要
- 日期：
- 功能点：
- 现象（用户看到什么）：

## 2. 证据链（必须有）
- 相关代码位置：
- 触发接口与参数：
- 我最初的错误判断：

```java

```