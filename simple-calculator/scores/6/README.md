# 6/10：原始 Command + 快照版本

## 复原范围

按用户指定的首次评审阶段及对话中保留的源码复原，不再使用后来的 Engine 版本。生产类和作者断言恢复为该阶段行为；缩进及测试包布局适配当前仓库。

- Calculator 同时持有 Stack<Command> engine、Stack<Integer> current 和 commands。
- Command 保存 snapshot，execute 接收两个栈并返回操作数栈，undo 返回自身快照。
- NumberCommand / ArithmeticCommand 在修改前复制当前栈，并将自身加入历史。
- UndoCommand 通过 engine.pop().undo() 恢复状态。
- 保留减法 operand - operand2 的顺序错误。
- 保留作者失败方法缺少 @Test，以及断言 IllegalArgumentException 而实际抛 IllegalStateException 的问题。
- 没有 Engine.java，也没有后来的独立减法及失败恢复测试。

Maven 的 Java 17、JUnit 5 插件和 reviewer profile 为当前教学运行设施，未回退到当时 Java 8 的 Eclipse 配置，也不声称 pom.xml 是原始逐字备份。作者测试保存在标准 calculator 包，断言不因复原而修正。

## 统一评分与分级 Hire 判断

统一评分沿用用户指定的 **6/10** 目录定位，不因这次复原重新提升或降低；目标职级不改变分数。这与对话早期曾使用过的不同评分范围应区别理解。

| 目标级别 | 本轮判断 | 理由 |
| --- | --- | --- |
| Junior | Hire | 具有栈与命令拆分的基础能力，但需要辅导修正减法和测试遗漏 |
| Senior | No Hire | 核心运算及失败测试存在明显遗漏，状态处理尚不能独立保证 |
| Staff | No Hire | 命令管理历史和快照，缺少统一的执行一致性规则 |
| Principal | No Hire | 接口、状态归属和生命周期的契约尚未形成 |

Hire 是本仓库统一学习尺度下的单轮判断，不表示功能验收通过或最终录用。若岗位把核心功能完整正确作为硬性门槛，减法错误也可以导致 Junior No Hire。

## 值得保留

解析与执行分离；数字、算术、撤销有各自实现；快照方案不依赖逆运算；作者已分别为解析和执行写测试。小题无需引入框架。

## 主要问题与复现

| 问题 | 位置 | 证据 |
| --- | --- | --- |
| 减法顺序反了 | ArithmeticCommand.execute | 13, 5, - 返回 -8，应为 8 |
| 主例掩盖错误 | TestCalculator.testSuccessfulExecution | 错误减法被撤销，最终仍得到 27 |
| 失败测试不参与执行 | TestCalculator.testFailedExecution | 缺少 @Test；实际异常类型也与断言不符 |
| 失败前已污染状态与历史 | ArithmeticCommand.execute | 对 [7] 执行 + 后栈为空，历史也新增失败操作 |
| 操作描述与执行记录绑定 | Command.snapshot | 每个命令只持有一份可变快照，直接返回其引用 |
| 历史规则分散 | 数字、算术命令 | 新命令必须自行正确执行快照和历史操作 |
| 生命周期不明确 | Calculator.execute | 读取用 pop，重复调用仍保留旧命令历史 |
| 输入边界不明确 | CalculatorHelper.parse | 1, 被接受；空历史和操作数不足抛底层异常 |

不要把后来 Engine 版的“缺少初始历史”和“history.peek 快照别名”故障归到这里。此版本的 `1, U, 2` 正确返回 2，`1, 2, U, 3, +, U, U` 正确返回 1。

## 测试来源与执行

作者组保留两个原测试类：TestCalculator 与 helper/TestCalculatorHelper。JUnit 实际发现 3 项测试；漏注解的失败方法不计入通过数。

独立评审测试在 solution/reviewer-tests：common 验证通用表达式；legacy 直接验证旧命令 API 的失败一致性，并显式调用漏注解的方法暴露断言问题。不会为了让作者测试变红而给历史方法补 @Test。

从仓库根目录运行：

```text
mvn -f simple-calculator/scores/6/pom.xml clean test
mvn -f simple-calculator/scores/6/pom.xml -Previewer clean test
```

## 改进顺序

1. 修正减法顺序，并添加独立减法测试。
2. 给失败测试补注解、修正异常断言。
3. 把快照、历史和失败恢复收敛到统一执行入口。
4. 让命令不再持有某次执行的历史状态。
5. 明确一次性求值与会话生命周期，再补输入边界测试。

完整快照最坏 O(n²) 对本练习可以接受；优先保证语义和封装，而不是优化性能。
