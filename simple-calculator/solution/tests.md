# 测试案例与验证方法

## 如何运行评估测试

准备 JDK 17 或以上及 Maven，确保 `java`、`javac`、`mvn` 在 PATH 中。以下命令从仓库根目录 `oop-coding-interview-in-java` 执行；首次运行可能需要联网下载依赖。

### 运行作者测试和评估测试

```bash
mvn -f simple-calculator/scores/8/pom.xml -Previewer clean test
```

`-Previewer` 启用名为 `reviewer` 的 Maven profile，将 `solution/reviewer-tests` 中的评估测试加入测试源，同时运行该实现自己的测试。把路径中的 `8` 替换为 `3`、`4`、`6` 或 `7`，即可评估对应版本。

### 只运行评估测试

```bash
mvn -f simple-calculator/scores/8/pom.xml -Previewer "-Dtest=calculator.reviewer.*" clean test
```

### 只运行作者测试

```bash
mvn -f simple-calculator/scores/8/pom.xml clean test
```

保留 `clean`，避免切换 profile 后，旧的评估测试 class 残留在 `target/test-classes` 中被再次执行。

如果已经进入某个版本目录，例如 `simple-calculator/scores/8`，运行全部测试可简写为：

```bash
mvn -Previewer clean test
```

### 测试范围和结果

- `scores/<分数>/src/test/java`：该版本的作者测试。
- `solution/reviewer-tests/common`：所有版本的通用表达式与输入契约测试。
- `solution/reviewer-tests/legacy`：6 分版的原始双栈 Command 接口测试。
- `solution/reviewer-tests/engine`：8 分版的 Engine 接口测试。

Maven 控制台按测试类显示 `Tests run`、`Failures`、`Errors` 和 `Skipped`。详细报告在对应版本的 `target/surefire-reports`，评估类的包名为 `calculator.reviewer`。

`BUILD FAILURE` 若由断言或测试异常导致，表示实现未满足某项验证，不表示 Maven 配置失效；如果是依赖下载或编译失败，应先解决运行环境或编译问题。教学中的低分版本保留缺陷，因此评估测试出现失败是预期的。

当前 8 分版仍有末尾空 token、重复执行两项评估不通过。重复执行测试选择“每次求值相互独立”的契约；若实现明确采用一次性对象，应改为验证明确的生命周期异常，不能将不明确的最终栈错误视为契约。

历史验证结果见 [scores/TESTS.md](../scores/TESTS.md)，共享测试说明见 [reviewer-tests/README.md](reviewer-tests/README.md)。下列案例表是测试设计清单，不表示每个案例目前都已写成可执行测试。

## 表达式测试

| 场景 | 输入 | 预期 |
| --- | --- | --- |
| 单个数字 | `7` | 7 |
| 零 | `0` | 0 |
| 输入上界 | `2147483647` | 2147483647 |
| 空白 | ` 13 , 5 , + ` | 18 |
| 减法顺序 | `13, 5, -` | 8 |
| 负数结果 | `5, 13, -` | -8 |
| 连续运算 | `13, 5, +, 9, -` | 9 |
| 撤销运算 | `13, 5, +, 9, -, U, +` | 27 |
| 撤销首个数字后继续 | `1, U, 2` | 2 |
| 撤销后继续操作并再次撤销 | `1, 2, U, 3, +, U, U` | 1 |
| 操作数不足 | `1, +` | 执行异常 |
| 无历史可撤销 | `U` | 执行异常 |
| 过度撤销 | `1, U, U` | 执行异常 |
| 最终无操作数 | `1, U` | 结果异常 |
| 最终多个操作数 | `1, 2` | 结果异常 |
| 非法 token | `1, X` | 输入异常 |
| 不支持的负数字面量 | `-1` | 输入异常 |
| 输入越界 | `2147483648` | 输入异常 |
| null、空字符串、纯空白 | 分别传入 | 输入异常 |
| 空 token | `,1`、`1,,2`、`1,` | 输入异常 |

## 执行器状态测试

这些场景应直接测试执行器，避免完整表达式失败后停止调度，掩盖后续状态问题。

### 操作部分修改后失败

1. 成功输入 7。
2. 执行一个先弹出数字、再抛异常的测试操作。
3. 断言原始异常原因得到保留。
4. 断言结果仍为 7。
5. 执行撤销，应恢复为空；这验证失败操作没有增加历史。
6. 再次撤销应明确报错；随后输入 9，再撤销，应仍可恢复为空。

以下 JUnit 示例针对当前练习中的 `Engine` / `Command` API；若实现改用普通操作接口，相应替换类型即可：

```java
@Test
void shouldRestoreStateWhenCommandFailsAfterMutation() {
    Engine engine = new Engine();
    engine.execute(new NumberCommand(7));

    IllegalStateException failure =
        new IllegalStateException("Simulated failure");
    Command failingCommand = new Command() {
        @Override
        public void execute(Engine context) {
            context.pop();
            throw failure;
        }
    };

    IllegalStateException actual = assertThrows(
        IllegalStateException.class,
        () -> engine.execute(failingCommand)
    );
    assertSame(failure, actual);
    assertEquals(7, engine.getResult());

    engine.execute(new UndoCommand());
    assertThrows(IllegalStateException.class, () -> engine.getResult());
    assertThrows(IllegalStateException.class,
        () -> engine.execute(new UndoCommand()));

    engine.execute(new NumberCommand(9));
    assertEquals(9, engine.getResult());
    engine.execute(new UndoCommand());
    assertThrows(IllegalStateException.class, () -> engine.getResult());
}
```

示例需导入 JUnit 的 `Test`、`assertEquals`、`assertSame`、`assertThrows` 以及项目的命令类型。空状态的断言最好使用只读状态视图（若实现提供），直接确认操作数为空；当前示例通过结果读取失败及后续行为共同验证。

### 撤销边界不破坏状态

空引擎执行撤销应报错；随后输入 7、撤销，再输入 9，应能读取到 9。最后撤销 9 应成功，不能因先前的失败而丢失初始状态。

### 读取结果不改变状态

输入 7，连续两次读取均返回 7；随后撤销应恢复为空。

### 操作数不足不改变状态和历史

输入 7，执行加法应报错；仍能读取到 7。再撤销一次应恢复为空，不能只撤销一条失败记录。

### 生命周期

如果提供可重复求值 API，连续两次计算同一个表达式应得到相同结果。如果使用一次性对象，第二次执行应抛明确的生命周期异常。测试需固定所选择的契约。

7 分版使用 `reviewer-tests/context`，对应 ExecutionContext 与 save-before-command 协议，不依赖 Engine。
