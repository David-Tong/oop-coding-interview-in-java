# 8/10：用户当前实现评审

来源：用户提供的实际实现。本次保留 src 与 pom.xml，不为符合分数而修改代码。

## 统一评分与分级 Hire 判断

**统一评分：8/10。** 同一实现的分数不随目标级别改变，职级只影响本轮 Hire 判断。

| 目标级别 | 本轮判断 | 理由 |
| --- | --- | --- |
| Junior | Hire | 核心执行、撤销和恢复能力充分。 |
| Senior | Hire | 状态管理集中，有针对性的回归测试。 |
| Staff | Hire | 统一执行规则与失败恢复达到本轮要求。 |
| Principal | No Hire | 命令权限、重入及生命周期契约仍不足以支持本轮 Hire。 |

这是学习用的 coding + OOP 单轮判断，不是完整职级或最终录用结论。统一尺度和局限见 [评分总览](../README.md#统一评分与-hire-标准)。


## 做得好的地方

- Calculator 调度解析后的命令，Engine 集中负责成功历史、执行及撤销。
- 普通命令不保存快照，Command 不再混合执行描述和历史记录。
- 保存初始空状态，撤销前检查历史长度，解决首个数字撤销和过度撤销问题。
- save、undo、restore 都复制容器，正常路径不会把历史快照直接交给后续写入。
- 失败时从最近成功快照恢复，原异常重新抛出；测试覆盖了部分执行失败后的数字恢复。
- getResult 使用 peek，读取没有副作用。
- TestCalculator 补充了首个数字撤销、撤销后继续操作的回归测试。

## 验证结果

生产源码使用 javac --release 17 编译。父目录 verify.py 的独立验证确认：

- 主示例为 27，独立减法为 8。
- `1, U, 2` 为 2。
- `1, 2, U, 3, +, U, U` 为 1。
- 先弹出 7 再抛异常后仍能读取 7，且传播的是原异常对象。
- 空历史撤销失败后，仍能输入数字并撤销。

后续已使用临时 Maven 3.9.9 实跑作者及共享评审测试，结果见上级 TESTS.md；作者测试通过，共享测试暴露严格输入与生命周期缺口。

## 分项评价

| 维度 | 分数 | 理由 |
| --- | --- | --- |
| 核心功能 | 8 | 主要运算、撤销和恢复路径已验证 |
| 职责划分 | 8 | 历史管理集中，普通命令职责收敛 |
| OOP 封装 | 7 | Engine 对命令仍暴露过多能力 |
| 测试 | 7 | 有针对性的回归，历史和边界断言仍可增强 |

## 剩余问题及优先级

### 1. 收紧普通命令的权限

位置：Command.execute(Engine)、Engine 的公开 push/pop/fetchOperands/execute。

命令拿到整个 Engine，可以修改操作数，也可以嵌套调用 execute。后者会写历史，因此现有 restore 保证依赖于“命令不调用调度器”的隐含约定。例如一个命令先执行另一个成功命令再失败，history 已经改变。

建议让普通命令只接收操作数接口，执行和历史只由 Engine 管理。不是为了接口数量，而是让失败契约的适用范围清楚、可保证。

### 2. 明确撤销指令的类型契约

位置：Engine.execute 的 instanceof 和 UndoCommand.execute。

Engine 特判本身在小题中可以接受；问题在于 UndoCommand 声称可 execute，实际上抛 UnsupportedOperationException。可以用普通操作与控制指令两种类型表达，而非提供一个不成立的方法实现。

### 3. 明确 Calculator 的生命周期

Calculator 将 Engine 保存为字段，重复执行会累积状态。Calculator("1") 第二次 execute 报最终栈错误，而不是清晰的生命周期错误。

每次求值创建新 Engine，或者显式限制一次执行，均可。verify.py 已记录现有行为。

### 4. 补齐后来题目的输入要求

CalculatorHelper.parse 使用 split(",")，`1,` 被接受并返回 1，与当前 problem 要求不符。应保留空 token 并明确校验。null 当前会触发底层 NullPointerException，缺少输入语义。负数字面量和溢出不属于基础功能，不因未实现额外扣分。

### 5. 加强测试及可读性

TestEngine 目前确认异常消息和结果 7。建议继续确认失败没有新增历史、空历史失败不破坏后续操作、读取结果可重复、生命周期契约，以及空 token。

operand/operand2 改成 right/left，字段按需要设 final，OPERAND_SIZE 使用 static final int；无共享实现的 Command 可以改成接口。以上是低成本改进，不应代替状态规则验证。

## 向更高分前进

优先解决类型能力和生命周期，然后补充输入边界与失败历史断言。无需为提分引入框架或优化快照性能；完整快照最坏 O(n²) 对本练习可以接受。


## 测试来源与运行

本目录的 `src/test/java/calculator` 只保存作者测试。3、4 分为模拟作者测试；6、7 分根据对话重建；8 分保留原有断言。

评审测试统一位于 `../../solution/reviewer-tests`，不在本目录复制。默认运行作者测试；启用 `reviewer` profile 会额外加入共享评审测试。通用表达式测试适用于所有版本；依赖 Engine 扩展接口的测试只加入 6、7、8 分版本。

从仓库根目录运行本档作者测试：

```text
mvn -f simple-calculator/scores/8/pom.xml clean test
```

加入评审测试：

```text
mvn -f simple-calculator/scores/8/pom.xml -Previewer clean test
```

将 8 替换为本目录分数。低分版本或尚未满足严格边界的实现出现测试失败是预期的教学结果，不能修改断言迎合缺陷。
