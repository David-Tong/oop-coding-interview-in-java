# 参考解决方案

对应题目：[单会议室预订器](../problem/README.md)。与 simple-calculator 的 solution 一样，这里提供设计说明、算法和独立评审测试；用户实现保留在 scores 中。

## 职责与 API

一天内记录很少，推荐一个拥有全部状态的 `BookingCalendar`，配合不可变 `Booking` 即可。不需要为了分层增加只做转发的 Service。

```java
public record Booking(String id, int start, int end) {}
// BookingCalendar 的 API 示例
// void book(String id, int start, int end)
// void cancel(String id)
// void reschedule(String id, int start, int end)
// List<Booking> listBookings()
// OptionalInt findEarliestSlot(int from, int to, int duration)
```

内部维护 `Map<String, Booking>`，校验、冲突检查和提交由同一个入口协调。Booking 不可变，集合使用 `List.copyOf` 返回。若使用可变 Booking，需要在存入和返回时都复制，不能只复制 List。

ID 使用原字符串 equals，不 trim、不忽略大小写。所有 ID 入口检查 null 和全空白；创建和改期检查 `0 <= start < end <= 1440`。查询检查 `0 <= from < to <= 1440` 及 `1 <= duration <= 1440`。非法输入、重复 ID、未知 ID、冲突均抛带原因的异常；无空档使用正常空结果。

## 创建、取消和改期

两个半开区间冲突的充要条件：

```text
candidate.start < existing.end && existing.start < candidate.end
```

创建先完成输入校验、重复 ID 检查以及对全部已有记录的冲突检查，再 put。取消先验证 ID 存在，再 remove。改期先验证 ID 和新时间，扫描全部其他 ID 的记录；只有全部检查通过后才用 put 替换原值。

```text
reschedule(id, start, end):
    validate(id, start, end)
    require map.containsKey(id)
    candidate = immutable Booking(id, start, end)
    for existing in map.values:
        if existing.id != id (按 String.equals) and overlaps(candidate, existing):
            throw conflict
    map.put(id, candidate)
```

改为原时间自然成功。失败前没有修改状态，不需要用先删除再预订的方式实现改期。这里的失败一致性针对题目中的业务与输入失败，不扩展为 JVM 内存耗尽等故障的事务保证。

## 列表与最早空档

列表将 map.values 复制后按 start 排序，返回不可变列表。查询复用这个有序快照，维护最早候选起点 cursor：

```text
validate(from, to, duration)
if duration > to - from: return empty
cursor = from
for booking in sortedBookings:
    if booking.end <= cursor: continue
    if booking.start >= to: break
    if cursor + duration <= booking.start: return cursor
    cursor = max(cursor, booking.end)
    if cursor + duration > to: return empty
return cursor if cursor + duration <= to else empty
```

cursor 之前已不存在可行起点：遇到足够大的间隙立即返回；否则该预订迫使起点移动到它结束之后。因此结果为最早连续空档，不能拼接多个间隙。所有参数已限于一天，时间加法不会 int 溢出。查询不写入 map。

例如 A `[60,90)`、B `[120,150)`：查询 `(70,180,30)` 返回 90；`(70,180,31)` 无结果；`(20,200,50)` 返回 150，不能返回会覆盖 A 的 20。

## 复杂度与取舍

设已有 n 条预订，采用 HashMap 和每次查询排序的简单方案：

| 操作 | 时间 | 额外空间 |
| --- | --- | --- |
| 创建、改期 | O(n)，含冲突扫描 | O(1) |
| 取消 | 平均 O(1) | O(1) |
| 列表、空档查询 | O(n log n) | O(n) |

持久保存空间 O(n)。此复杂度足够满足少量预订的要求。也可以维护按 start 的 TreeMap 与 ID 索引，但需确保双索引始终同步；排除改期对象后必须继续检查真正的前后邻居，不能遇到自身就停止。

测试设计与运行方法见 [tests.md](tests.md)，当前实现的评审见 [scores/6](../scores/6/README.md)。
