# Maven 测试验证记录

scores 仅保存作者测试；独立评审测试统一位于 solution/reviewer-tests，由 reviewer profile 引入。测试按来源分类，不按是否通过分类。3、4 分作者组为模拟测试，6、7 分为对话重建，8 分保留作者断言。

本次使用临时下载的 Maven 3.9.9、Java 17 和实际 Surefire/JUnit Platform 执行，读取 surefire-reports XML 汇总。未修改系统 Maven 安装或 PATH。

| 版本 | profile | 作者通过/总数 | 评审通过/总数 |
| --- | --- | --- | --- |
| 8 | 默认 | 8/8 | 0/0 |
| 3 | reviewer | 2/2 | 1/7 |
| 4 | reviewer | 2/2 | 4/7 |
| 6 | reviewer | 3/3 | 5/9 |
| 7 | reviewer | 5/5 | 6/11 |
| 8 | reviewer | 8/8 | 7/9 |

reviewer 模式出现 BUILD FAILURE 是测试暴露教学版本缺陷，不是测试源未加载。8 分版失败项为末尾空 token 和重复执行；生命周期测试当前选择可重复求值，若实现明确选用一次性对象，应按明确契约调整该测试。

从仓库根目录运行作者和评审测试：

```text
mvn -f simple-calculator/scores/8/pom.xml -Previewer clean test
```

去掉 -Previewer 只运行作者测试，替换路径中的分数可切换实现。保留 clean 避免旧测试 class 残留。详情见 [共享测试说明](../solution/reviewer-tests/README.md)。

6 分现已按首次评审复原，作者实际发现 3 项（另一个失败方法漏了 @Test）。本次重新运行 Maven：默认作者测试通过；reviewer 为 5/9，通过数变化源于恢复原始接口和新增 legacy 验证，不是删除失败断言。失败项为减法顺序、末尾空 token、历史失败方法的异常断言，以及算术失败后的状态一致性。

7 分已恢复 10:52 的 ExecutionContext 与双次撤销阶段，并重新执行 Maven。作者 5/5 通过；评审 6/11 通过。评审失败覆盖失败历史、部分修改恢复、脱离额外 save 的撤销耦合、末尾空 token 和重复求值。
