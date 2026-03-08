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
