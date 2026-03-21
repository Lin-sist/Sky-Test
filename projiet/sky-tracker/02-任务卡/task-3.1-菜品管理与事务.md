# 任务卡：3.1 菜品管理与事务

## 0. 基本信息
- 任务名称：菜品管理（Dish + DishFlavor）多表保存 + 事务验证
- 对应接口/方法：DishController 保存接口、DishService saveWithFlavor、DishMapper、DishFlavorMapper
- 预计耗时：2 天（每天 1 小时）
- 实际耗时：
- 完成状态：进行中

## 1. 先思考（不写代码）

### Q1：为什么 saveWithFlavor 必须加事务？
- 你先描述一个失败场景：主表 Dish 保存成功，但口味表 DishFlavor 保存失败。
- 再回答：如果不加事务，数据库会出现什么脏状态？

你的回答：


### Q2：事务边界应该放在哪一层？
- 你在 Controller、Service、Mapper 三层里选一层，并说明原因。
- 追问：为什么不把事务写在 Mapper 层？

你的回答：


### Q3：事务为什么会失效？
- 至少举 2 个常见失效场景（提示：自调用、非 public、异常被 catch）。
- 每个场景写一句“如何规避”。

你的回答：


## 2. 请求流转伪代码
text:
前端提交 DishDTO（含 flavors）
  -> Controller 参数接收
  -> Service.saveWithFlavor(dishDTO) 开启事务
      -> 保存 Dish 主表，拿到 dishId
      -> 批量保存 DishFlavor（逐条填充 dishId）
      -> 任一步异常则整体回滚
  -> 返回统一 Result

## 3. 边界条件（至少 4 条）
- [ ] flavors 为空时是否允许保存
- [ ] flavors 中出现非法值时如何处理
- [ ] Dish 保存成功但 Flavor 保存失败是否回滚
- [ ] 同名菜品/数据约束冲突时返回是否统一

## 4. 破坏性测试计划
- [ ] 测试 1：保存 flavor 前手动抛 RuntimeException，验证 Dish 是否回滚
- [ ] 测试 2：构造非法 flavor 数据，观察异常路径
- [ ] 测试 3：重复提交同一菜品，观察约束行为

## 5. 验证证据（Apifox/Postman）
- 正常路径：
- 破坏性测试：
- 数据库验证截图/结论：

## 6. 结果复盘（能力沉淀）
- 本次新增可迁移能力：
- 迁移到 RAG 项目的落点：
- Why Not 灾难推演结论：

## 7. 完成定义
- [ ] 能口述多表事务完整链路
- [ ] 事务回滚破坏性测试通过并有证据
- [ ] 至少 1 条事务相关 RCA 已写入
- [ ] 至少 1 条可迁移模式已写入模式库
