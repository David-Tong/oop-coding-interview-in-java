# 7.5/10：状态与封装明显完善，查询校验仍缺两个关键规则

**本轮实际评分为 7.5/10，比 scores/7 的 7/10 提升 0.5 分。** 目录名 8 不预设评分；沿用 calculator 的统一质量分与 Hire 参考线，不按测试通过率换算分数。

| Junior | Senior | Staff | Principal |
| --- | --- | --- | --- |
| Hire | Hire | No Hire | No Hire |

参考线为 6、7、8、9。这是 coding + OOP 单轮学习判断，不代表完整职级认证或最终录用；Hire 不代表全部验收通过。

## 本轮进步

- Manager 已移到 Service 同包并限制为包内 final 类，方法使用包权限，Service 成为明确的公共入口。
- Booking 已是 final 类，字段 private final，普通调用者与子类都无法改变存储记录。
- reschedule 检查存在与冲突后，直接替换 TreeSet 和 Map，不再调用 cancel/reserve 或二次检查冲突。
- 创建和改期冲突均报告异常，修复此前的错误契约差异。
- null Booking/Query 显式拒绝；合法 duration 大于窗口在 Manager 返回 -1。
- 原有测试使用参数化案例，实际执行 44 项；之前补过的空档和改期回归仍通过。

这些是实质性改进，不再因为之前已解决的问题重复扣分。

## 主要扣分项：查询校验

BookingValidator.validateQuery 当前判断 `query.getTo() < 1440`，但题目允许 `to == 1440`。

```java
new BookingService().findEarliestSlot(new Query(0, 1440, 1440));
```

空日程应返回 0，当前抛 IllegalArgumentException，合法的当天结束边界被拒绝。

同时，当前没有检查 `1 <= duration <= 1440`：

| 输入 | 正确行为 | 当前行为（空日程） |
| --- | --- | --- |
| `(0,100,0)` | 参数异常 | 返回 0 |
| `(0,100,-1)` | 参数异常 | 返回 0 |
| `(0,100,1441)` | 参数异常 | 返回 -1 |

这不是“找不到空档”，而是非法时长。应在 Validator 中检查：

```java
if (query.getFrom() < 0
        || query.getFrom() >= query.getTo()
        || query.getTo() > 1440) {
    throw new IllegalArgumentException(
        "Query window must satisfy 0 <= from < to <= 1440.");
}
if (query.getDuration() < 1 || query.getDuration() > 1440) {
    throw new IllegalArgumentException(
        "Duration must be between 1 and 1440 minutes.");
}
```

保留 Manager 对合法但时长大于窗口的 -1 处理。先限制范围，也避免任意 duration 导致加法溢出。

## 其他改进空间

- Booking 的越界时间错误仍只提示 start>=end，原因不准确。改期冲突消息 `Cam reschedule` 也没有说明冲突，应改为明确文案。
- reserve/cancel 的 catch 会丢失 cause，且未提供恢复逻辑；对本题有效输入没有必要包装这些操作。
- `reserver` 拼写、空 if 分支可以整理，集合字段可以 final。这些不是主要扣分依据。
- cancel 的 ID 检查仍在 Manager，空白 ID 被按未知记录拒绝；功能上仍报错且状态不变，不声称它会错误取消。可统一复用 ID 校验以改善错误原因。
- 原有查询校验测试没有覆盖 duration 和合法 to=1440；Manager 测试绕过 Service，不能单独证明公共入口契约完整。

## 测试证据

运行 `mvn -f meeting-room-booking/scores/8/pom.xml -Previewer clean test`：

| 来源 | 通过/总数 | Failure | Error |
| --- | --- | --- | --- |
| 原有测试 | 44/44 | 0 | 0 |
| query8 评审测试 | 10/16 | 4 | 2 |

全部 60 项无跳过。评审失败集中于上述两类查询缺陷，包含组合测试和独立反例的重复覆盖，不能解释成六个独立设计缺陷。Error 是合法全天查询被实现抛异常，不是构建配置错误。

query8 在上一轮 12 项基础上，仅将冲突状态测试适配为捕获当前的异常契约，再新增 4 个查询边界案例；比较记录仍按字段值。生产 Java 与原有测试未改动，仅为执行测试补充固定构建插件及 reviewer profile。

仅运行原有测试：

```text
mvn -f meeting-room-booking/scores/8/pom.xml clean test
```

综合评价：核心预订、失败提交和封装已接近 8 分水平，但公共查询入口仍接受非法值、拒绝合法边界，因此本轮为 7.5。修复并回归这些规则后再评估 8 分，不需要增加架构层次。
