# 7/10：主要状态缺陷已修复，查询校验与接口契约仍需完善

## 统一评分与分级 Hire 判断

**本轮统一评分：7/10，比上一版 6/10 提升 1 分。** 沿用 simple-calculator 的评分口径：功能正确性、状态和失败一致性、OOP 职责及封装、可读性和测试证据。目录名不作为评分依据，分数也不按测试通过率换算。

| 目标级别 | 本轮判断 | 依据 |
| --- | --- | --- |
| Junior | Hire | 五个操作完整，已能定位并修复重要边界与封装问题 |
| Senior | Hire | 主流程和职责清楚，已修复此前数据丢失路径，但查询契约仍不完整 |
| Staff | No Hire | 参数有效性与正常无结果混淆，公开入口的校验责任仍不统一 |
| Principal | No Hire | 接口和状态规则尚未形成简洁、自洽且完整验证的模型 |

参考线仍为 Junior 6、Senior 7、Staff 8、Principal 9。这是 coding + OOP 单轮学习判断，不代表完整职级或最终录用；Hire 也不表示已通过全部题目验收。没有独立完成时间等证据，不推断这些能力。

## 相比 6 分版的实际进步

1. findEarliestSlot 独立查找 ceiling，从第一条相关记录继续扫描；此前 `(20,200,50)` 错返 20、`(20,150,50)` 错返 20 的问题均已修复。
2. canReserve 在 next 为自身时继续 higher，修复 A `[60,90)`、B `[120,150)`，A 改为 `[50,130)` 时漏掉 B 的路径。评审验证原 A 保留，之后仍能取消和继续预订。
3. Booking 从 `@Data` 改为 `@Getter`，普通调用者不再能通过 setter 修改输入对象或返回记录，关闭了上一版直接破坏 TreeSet 排序及 Map 键的入口。
4. Service.reschedule 先执行 validateBooking，创建与改期均验证当天边界及 start<end。
5. dict 和 bookings 已改为 private；补充两项 Manager 回归测试。

这些修改解决了上一版主要扣分项，所以本轮不再按原来的数据丢失和公开 setter 问题重复扣分。

## 仍需修复的问题

### 1. 查询把“无空档”当成非法输入

BookingValidator.validateQuery 当前只有：

```java
if (query.getFrom() + query.getDuration() > query.getTo()) {
    throw new IllegalArgumentException("Illegal query condition.");
}
```

空日程查询 `new Query(60, 90, 31)` 的三个参数都合法，题目要求返回 -1，因为窗口装不下；当前公共 Service 抛异常。Manager 的原有短窗口测试仍通过，是因为绕过了 Service 的校验，未覆盖完整调用链。

### 2. 真正非法的查询参数反而被接受

应分别检查 `0 <= from < to <= 1440`、`1 <= duration <= 1440`。当前 `(-1,100,10)`、`(0,1441,10)`、`(0,100,0)`、`(0,100,-1)` 都没有被正确拒绝。

建议先完成范围校验；再让合法但 duration>to-from 的请求返回 -1。这样也能把时间计算限制在安全范围，避免先加后比的溢出风险。

### 3. 冲突错误契约仍有差异

创建与改期冲突目前返回 false；题目明确要求抛带原因的异常。评审单独测试这项，不把 false 风格与丢失记录等量评价。即使认可 false 作为替代 API，查询问题仍需修复。

### 4. 封装与对象协作还可以收紧

- Manager 是 public 且可直接使用，但创建、改期和查询依赖 Service 校验。若 Manager 是内部组件应明确边界；若是公共 API，应保证自身入口的前置条件。无需把同样逻辑复制到每一层。
- Booking 虽然没有 setter，但类及 getter 可以被继承重写，字段也不是 final。对本题普通使用路径已足够关闭直接修改入口；进一步使用 `final class` + `private final` 字段或 record 可把不可变性变成明确保证。此项不是本轮主要扣分原因。
- reschedule 仍先 cancel 再 reserve。目前合法单线程输入下，此前漏查路径已修复，不能因此继续声称它必然丢记录；但结构仍让失败保证依赖两次冲突检查的一致性，先检查后统一替换更易维护。
- null Booking/Query 仍触发 NPE；属于对象式 API 的补充健壮性问题。错误消息对越界时间仍只描述开始不小于结束，原因不够准确。
- 无实际处理的 catch 再原样 throw、丢失 cause 的异常包装、reserver 拼写仍可整理。

## 分项评价

| 维度 | 评价 |
| --- | --- |
| 核心功能 | 8/10：普通五个操作与此前查询反例已修复；合法短窗口被错误拒绝 |
| 状态与失败一致性 | 7/10：此前改期丢记录路径已修复，提交结构仍可简化 |
| OOP 职责及封装 | 7/10：私有状态与只读 Booking 明显改进，公共边界责任仍不统一 |
| 可读性 | 7/10：实现直观，校验、异常与命名仍需整理 |
| 测试证据 | 原有 23/23 通过；新版 API 评审 8/12 通过 |

统一分数是整体判断，不是分项算术平均。达到 8 分的重点是完整查询契约、统一公开边界和更清楚的提交规则，不需要继续增加类或设计模式。

## 测试与复现

未修改生产 Java 和原有测试。仅补充 Maven Compiler/Surefire 固定版本和 reviewer profile，以执行 JUnit 5 及独立评审测试。

新版 API 使用 Query，Booking 不再实现值相等，因此新增 `solution/reviewer-tests/query` 测试组，通过显式字段快照比较状态。旧 common 测试依赖三整数查询与 setter，不能直接用于此版，也不能将编译不兼容算成业务失败。

从仓库根目录运行：

```text
mvn -f meeting-room-booking/scores/7/pom.xml clean test
mvn -f meeting-room-booking/scores/7/pom.xml -Previewer clean test
```

实测 reviewer 同时运行原有 23 项及新增 12 项：原有全部通过；新增 8 项通过、3 项 Failure、1 项 Error，无跳过。Error 是合法查询被实现抛出的 IllegalArgumentException，不是编译或依赖错误。完整记录见 [TESTS.md](../TESTS.md)。
