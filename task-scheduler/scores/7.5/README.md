# 7.5/10：并发提交与任务列表快照已修复，员工查询边界仍需完善

2026-11-04 重新评分：**7.5/10，通过 Junior / Senior，未达到 Staff / Principal 门槛。** 按根目录 [SCORING.md](../../../SCORING.md) 评价当前实现，目录名不决定分数，总分不机械平均。

技能匹配与最低负载优先策略合理，员工选择和负载增加在同一个管理器监视器内完成。Scheduler 现在按 Task 加锁，将状态检查、负载预占与登记串行化，并对登记前的 checked / runtime 失败执行补偿。TaskManager 改为发布不可变列表，消除了原地修改 ArrayList 与复制查询之间的竞争。正常业务、同任务并发防重和关键异常路径已有有效验证，达到 7.5 分。员工侧查询同步、修改入口约束及异常状态契约仍不够完整，尚不足以支持 8 分。本判断仅代表本次 coding + OOP 材料。

## 四个维度

| 维度 | 参考分 | 依据 |
| --- | --- | --- |
| 功能与正确性 | 7.5 | 正常调度、最低负载选择、非法输入和防重成立；经 Scheduler 的同任务并发提交保持记录与负载一致，实时员工查询保证仍不足 |
| OOP 设计与可维护性 | 7.5 | 对象职责清楚，方案复杂度适中，技能与列表成员有防御性保护；公开的任务状态修改与管理器操作仍能绕开统一提交入口 |
| Java 与工程实现 | 7 | 按任务同步、员工选择同步及不可变列表发布合理；员工查询未与写入统一同步，负载字段也没有独立可见性保证 |
| 测试与验证 | 7.5 | 标准测试 54 次执行全部通过，新增同任务跨 Scheduler 防重及 runtime 补偿测试；独立验证覆盖双员工重复提交与列表并发查询，员工查询边界尚缺验证 |

## 主要问题

1. **员工查询的同步保证仍未闭合，而且有回退（代码审查结论）。** EmployeeManager.addEmployee、assignEmployee、rollbackAssignment 使用 synchronized，但 getEmployees、getEmployeeWithHighestLoad、getEmployeeWithLowestLoad 不再持有同一个监视器。并发注册与查询会同时访问 ArrayList，极值查询与负载更新也没有同步关系；Employee.getLoad 读取的 load 仍是普通 int，直接查询缺少跨线程可见性保证。上一轮这些管理器查询有锁，这次改写时丢失了保护。本轮未独立复现异常或旧值，不把风险写成必然失败；演示程序等待所有调度结束才报告，其结果不因此无效。建议恢复查询方法同步，并为直接读取负载提供 volatile 或受锁快照。若需整份报告对应同一时刻，还需单独定义统一快照边界。
2. **提交入口与失败状态契约仍需明确。** Scheduler 的任务锁能协调多个 Scheduler 操作同一个 Task，但 Task.assignTask、TaskManager.assignTask 等公开修改入口未遵循同一协议；若允许调用者混用，仍可绕过保护。应收紧内部操作入口或明确使用约束。现有 failureAfterRecordingRollsBackWorkloadButKeepsTaskRecord 测试还证明：扩展 TaskManager 完成登记再抛 checked / runtime 异常时，补偿会清零负载但保留记录与 ASSIGNED 状态。后者依赖子类故障注入，作为契约不足处理，不等同于原始对象普通调用失败，不据此机械压低到 7 分以下。应规定登记失败不改变状态，或提供与实际提交状态匹配的恢复机制。

已修复的问题不再扣分：同任务从 Scheduler 并发提交产生额外负载、登记前 RuntimeException 不补偿、任务列表原地修改与查询竞争。不可变列表只保证成员快照，不代表所有 Task 字段成为不可变对象。单次登记复制该员工已有任务为 O(k)，本题无规模要求，不单独扣分。

## 验证与下一步

从仓库根目录实际执行：

```powershell
mvn -f task-scheduler/scores/7.5/pom.xml clean test
javac -encoding UTF-8 -cp task-scheduler/scores/7.5/target/classes -d task-scheduler/scores/7.5/target/reviewer-tests task-scheduler/scores/7.5/reviewer-tests/ReviewCurrent.java
java -cp 'task-scheduler/scores/7.5/target/classes;task-scheduler/scores/7.5/target/reviewer-tests' ReviewCurrent
```

- Maven clean test：退出码 0，54 次执行，0 failures、0 errors、0 skipped。
- 独立验证编译和执行均退出码 0。两个员工、两个独立 Scheduler 并发提交同一任务，2,000 轮均恰好成功一次、登记一次，且逐员工负载等于记录总负载，任务归属一致。
- 列表验证：一个写线程登记 1,000 个新任务，一个读线程执行 1,000 次快照读取，均通过；旧快照仍保持一条记录且不可修改，最终列表为 1,001 条。源码见 [ReviewCurrent.java](reviewer-tests/ReviewCurrent.java)。

这些结果支持相应场景，不代表穷举全部并发交错；未测量覆盖率。现有故障刻画测试通过不代表其断言的状态就是正确业务目标。不要求为本题捕获任意 Error 或防御任意子类行为。

优先恢复员工查询同步并补足负载可见性，再明确统一提交入口与登记失败保证。本轮仅新增报告与独立验证，未修改生产 Java、原测试或 pom。上一轮 6.5 分是修复前版本的结论，本次按当前源码重新判断。
