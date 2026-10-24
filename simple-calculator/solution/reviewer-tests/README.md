# 共享评审测试

这些测试由评审补充，不复制到 scores，不属于作者原有覆盖。

- `common`：所有版本共同的表达式行为、撤销序列、空 token 和重复求值测试。
- `engine`：依赖当前 Engine / Command API 的空历史恢复与部分执行失败恢复测试，仅加入 8 分实现。3、4 分没有这个扩展 API，不应报告为已通过这些测试。

## 一条命令加入评审测试

从仓库根目录运行：

```text
mvn -f simple-calculator/scores/8/pom.xml -Previewer clean test
```

默认执行作者测试和共享评审测试。将 8 改为 3、4、6、7 可验证其他实现。去掉 -Previewer 只运行作者测试。应保留 clean，避免切换 profile 后旧编译产物参与发现。

只运行评审测试（仍启用测试源 profile）：

```text
mvn -f simple-calculator/scores/8/pom.xml -Previewer "-Dtest=calculator.reviewer.*" clean test
```

Maven 使用 build-helper-maven-plugin 添加外部测试源，Surefire 明确配置 JUnit 5 兼容版本，首次运行可能下载依赖。

## 测试契约

断言正确规格，不禁用、不迁就低分实现的错误。8 分版仍会在末尾空 token 和重复执行测试失败。

重复执行测试当前选择“可重复求值”的契约。若题解明确选择一次性对象，应将断言改为该实现规定的精确生命周期异常，不能把当前不明确的最终栈错误当作成功。这个选择需对所有接受该契约的版本一致应用。

使用共享测试衡量规格，不用通过率直接推导职级；后来补充的严格输入要求也不追溯改变此前评分范围。

- `legacy`：仅加入 6 分版，针对原始 Command 双栈接口检查失败一致性及漏注解测试的错误断言。6 分作者组实际发现 3 个测试，不包含漏注解方法。

7 分版使用 `reviewer-tests/context`，对应 ExecutionContext 与 save-before-command 协议，不依赖 Engine。
