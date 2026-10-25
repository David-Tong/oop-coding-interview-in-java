# 测试案例与验证方法

新版 `scores/7` 的查询 API 改为 Query，评审使用独立 query 测试组。命令：`mvn -f meeting-room-booking/scores/7/pom.xml -Previewer clean test`；去掉 profile 仅运行该版原有测试。新版实测原有 23/23、评审 8/12，详见 [新版评分](../scores/7/README.md)。以下 scores/6 命令仍用于上一版。

从仓库根目录执行，需要 JDK 17 或以上与 Maven：

```text
mvn -f meeting-room-booking/scores/6/pom.xml clean test
mvn -f meeting-room-booking/scores/6/pom.xml -Previewer clean test
mvn -f meeting-room-booking/scores/6/pom.xml -Previewer "-Dtest=booking.reviewer.*" clean test
```

依次运行原有测试、原有加评审测试、仅评审测试。保留 clean，防止切换 profile 后旧 class 残留。原有测试不改写；评审源码位于 [reviewer-tests](reviewer-tests/README.md)，通过 Maven reviewer profile 加入。报告在 `scores/6/target/surefire-reports`。

## 测试设计清单

下表是完整测试设计，不表示每个边界都有独立可执行方法。可执行覆盖与实测统计见 [验证记录](../scores/TESTS.md)。

| 场景 | 操作或输入 | 预期 |
| --- | --- | --- |
| 半开边界 | A `[60,90)` 后创建 `[90,120)` | 成功 |
| 冲突类型 | 完全相同、包含、被包含、左右部分重叠 | 拒绝且状态不变 |
| ID 比较 | A、a、空格包围的 A | 不同 ID；new String("A") 是重复 ID |
| 非法 ID | null、空、空白 | 创建、取消、改期均报错 |
| 时间边界 | `[0,1440)`；负数、end=1441、start=end、start>end | 首项成功，其余报错 |
| 取消 | 删除 A，再用 A 预订原时间 | 成功 |
| 未知 ID | 取消或改期 missing | 报错且已有记录不变 |
| 改期排除自身 | A 改到相同或与自己原时间重叠的合法区间 | 成功 |
| 改期跨越自身和其他记录 | A `[60,90)`、B `[120,150)`，A 改为 `[50,130)` | 报错，A、B 均保留；之后仍能取消 A |
| 非法改期 | A 改为 `[90,60)`、`[60,60)`、越界时间 | 报错且原记录保留 |
| 有序读取 | 先插入晚的，再插入早的 | 按 start 排序，反复读取不变 |
| 输入对象隔离 | 创建后修改传入对象的 ID 和时间 | 内部记录不变 |
| 返回值隔离 | 清空返回列表、修改返回记录 | 内部记录不变；允许禁止修改 |
| 空日程 | `(0,1440,1440)` | 返回 0 |
| 查询从首条之前开始 | A、B 如上，`(20,200,50)` | 返回 150 |
| 无足够空档 | A、B 如上，`(20,150,50)` | 无结果 |
| 恰好放下 | `(70,180,30)` | 返回 90 |
| 不能拼接 | `(70,180,31)` | 无结果 |
| 合法但时长过大 | 空日程 `(60,90,31)` | 无结果，不是参数异常 |
| 非法查询 | from<0、to>1440、from>=to、duration<=0 或 >1440 | 报错 |
| 查询纯读与实例隔离 | 重复查空档，另建实例 | 查询不创建记录，新实例为空 |

检查失败一致性时，应对照独立的预期 Booking 值，不能把浅复制的列表作为唯一快照：如果元素被共用修改，错误实现可能与预期一起改变而掩盖问题。

评审通过公共 BookingService 测试，保留原 API 的 `reserver` 拼写和 `-1` 无空档表示。冲突的异常契约单独测试；状态保留测试允许返回 false 或抛业务异常，避免把接口风格与数据丢失混在一起。null Booking 是对象式 API 的补充健壮性测试，不是题目示例三个标量参数的独立要求，不作为主要扣分项。
