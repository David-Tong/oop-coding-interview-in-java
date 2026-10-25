# Maven 测试验证记录

## 最新版 scores/8：2026-10-24

执行 `mvn -f meeting-room-booking/scores/8/pom.xml -Previewer clean test`：原有 Manager 17/17、Service 5/5、Validator 22/22，共 44/44；query8 评审 10/16，4 Failure、2 Error，无跳过。Error 为合法全天查询被实现抛异常。

未通过的六项是两类问题的组合与单独覆盖：to=1440 被错误拒绝，以及 duration 的 1..1440 范围未验证。不是六个独立根因。query8 基于旧 query 组适配了冲突异常契约，新增四项独立查询边界测试；未修改生产 Java 和原有测试。评分与修复建议见 [8/README.md](8/README.md)。

## 新版 scores/7：2026-10-24

执行 `mvn -f meeting-room-booking/scores/7/pom.xml -Previewer clean test`，实际读取 Surefire 统计：

| 测试组 | 通过/总数 | Failure | Error | Skipped |
| --- | --- | --- | --- | --- |
| 原有 Manager | 17/17 | 0 | 0 | 0 |
| 原有 Service | 5/5 | 0 | 0 | 0 |
| 原有 Validator | 1/1 | 0 | 0 | 0 |
| 新增 query 评审组 | 8/12 | 3 | 1 | 0 |

失败分别为：非法查询参数校验不全；合法 duration 大于窗口时错误抛异常（Error）；冲突未按题目异常契约报告；null 对象缺少显式校验。null 是补充健壮性测试，非主要评分依据。非法参数及 null 测试使用 assertAll，分支均执行。

通过项包括：首条之前查询的两个反例、改期跨越自身后与邻居冲突时保留原记录并继续操作、双向与同时间改期、非法创建及改期、精确空档与全天、读取和实例隔离、无公开 setter、取消和重复 ID。

评审使用 query 测试源，未改动生产 Java 和原有断言。Booking 无值 equals，状态比较显式读取字段，不因对象身份不同误判。此版 12 项与旧版 20 项存在分组和 API 差异，不能用通过率直接衡量提升幅度。新版评分见 [7/README.md](7/README.md)。

## 上一版 scores/6

2026-10-24 实际执行 Maven 3.10.0，使用 Java 17 编译目标、Surefire 3.5.3 与 JUnit 5，统计对应 surefire-reports。生产源码和原有测试未修改；POM 增加固定插件版本、UTF-8 与独立评审测试 profile。

| 运行方式 | 原有通过/总数 | 评审通过/总数 | 结果 |
| --- | --- | --- | --- |
| 默认 clean test | 21/21 | 0/0 | BUILD SUCCESS |
| reviewer clean test | 21/21 | 9/20 | BUILD FAILURE，11 项断言失败，无 Error 或 Skip |

原有测试：Manager 15、Service 5、Validator 1。Validator 一个方法含多个断言，JUnit 统计为一个测试。新增评审源码与原有测试分开保存。

## 评审失败项

| 方法 | 实际问题 |
| --- | --- |
| searchBeforeFirstBookingSkipsOccupiedTime | 预期 150，实际 20 |
| searchBeforeFirstBookingCanReportNoSlot | 预期 -1，实际 20 |
| failedRescheduleBehindSelfKeepsOriginalAndRemainsUsable | 改期失败后 A 消失，只剩 B |
| reservationRejectsOutsideDay | 接受负时间 |
| invalidReschedulePreservesOriginal | 接受反向区间 |
| invalidSearchArgumentsAreErrors | 非法查询参数未报错 |
| reservationInputObjectCannotMutateStoredState | 修改输入对象改变内部记录 |
| returnedRecordCannotMutateStoredState | 修改返回记录改变内部状态 |
| conflictsAreReportedAsErrorsPerProblemContract | 创建冲突返回 false，未抛异常 |
| rescheduleConflictsAreReportedAsErrorsPerProblemContract | 改期冲突返回 false，未抛异常 |
| nullBookingHasExplicitReason | null 对象触发 NPE，未显式校验 |

有循环的测试在首个失败断言处停止，不能从这一轮统计声称循环内每个非法值都已单独运行。null 对象测试是 API 健壮性补充；冲突异常测试用于明确规格差异，不与数据丢失等量扣分。

其余九项验证相接与排序、重复和未知 ID、取消释放、ID 精确比较、原时间改期、精确及不连续空档、空日程及过长 duration、纯查询与实例隔离、列表容器隔离。

从仓库根目录复现：

```text
mvn -f meeting-room-booking/scores/6/pom.xml clean test
mvn -f meeting-room-booking/scores/6/pom.xml -Previewer clean test
```

第二条当前失败是实现缺陷被测出，不是 Maven 配置错误。详见 [测试说明](../solution/tests.md)。
