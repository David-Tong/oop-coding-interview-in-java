# 评审验证记录

## scores/7：最新重新评分，2026-10-31

按 SCORING.md 读取当前源码后执行 `mvn -f task-scheduler/scores/7/pom.xml clean test -q`，退出码 0：52 次测试执行、0 failures、0 errors（含重复及参数化测试）。原有接口不匹配问题已修复。

重新编译并运行 ReviewCurrent.java，均退出码 0；新增修复结果断言。跨技能并发负载 300/300、记录 200；顺序重复提交后负载与记录总负载均为 3；登记前失败后负载 0、记录 0、状态 PENDING。同一任务并发防重与实时查询快照风险仍来自代码审查，并未全面验证。

以下保留历史记录，当前结论以本节及 scores/7/README.md 为准。

## scores/7：当前版本重新评审，2026-10-31

从仓库根目录执行：

```powershell
mvn -f task-scheduler/scores/7/pom.xml clean test -q
mvn -f task-scheduler/scores/7/pom.xml '-Dmaven.test.skip=true' package -q
javac -encoding UTF-8 -cp task-scheduler/scores/7/target/classes -d task-scheduler/scores/7/target/review-current task-scheduler/solution/reviewer-tests-v7-current/ReviewCurrent.java
java -cp 'task-scheduler/scores/7/target/classes;task-scheduler/scores/7/target/review-current' ReviewCurrent
```

标准测试退出码 1：旧测试引用已删除 API，testCompile 失败，未执行测试。跳过测试的生产构建退出码 0。独立验证编译和执行均退出码 0，输出为观察结果，其中重复提交与登记失败展示缺陷，并不代表这些行为正确：

```text
PASS concurrent cross-skill balance: 300,300; records=200
Duplicate rejected: load=6, recordedLoad=3
Recording failure: load=4, records=0, status=PENDING
```

源码：[ReviewCurrent.java](../solution/reviewer-tests-v7-current/ReviewCurrent.java)。它独立运行，不修改或跳过标准测试来声称整套测试通过。下面保留的旧结果只适用于此前接口版本。
## scores/7：历史验证（旧接口版本，当前已不适用）

同下述 JDK/Maven 环境，从仓库根目录执行：

```text
mvn -f task-scheduler/scores/7/pom.xml clean test -q
mvn -f task-scheduler/scores/7/pom.xml -Previewer clean test -q
```

原有测试 37/37 通过（包括重复测试执行），退出码 0。包含独立评审时共 43 次执行，39 通过、4 failures、0 errors，退出码 1；独立评审为 2/6。

| 独立评审测试 | 结果 | 观察 |
| --- | --- | --- |
| readerMustNotObserveStaleSkillQueueDuringRefresh | 失败 | B 队列仍选择负载 10 的 shared，而非负载 5 的 lower |
| selectionAndAssignmentMustNotUseSameStaleMinimum | 失败 | 两个任务使用旧选择，最终负载 20、1，而非 10、11 |
| failureBeforeAssignmentMustNotSubtractUnaddedLoad | 失败 | 尚未增加负载就失败，回滚后为 -3 |
| failureAfterRecordingMustNotLeaveRecordWithoutLoad | 失败 | 记录总负载 3，但员工负载 0 |
| failureBeforeRecordingRestoresLoad | 通过 | 记录前失败正确恢复负载，员工仍可选择 |
| sequentialDuplicateSubmissionPreservesOriginalAssignment | 通过 | 顺序重复提交被拒绝，原负载和记录不变 |

测试源码：[ReviewConsistencyTest.java](../solution/reviewer-tests-v7/ReviewConsistencyTest.java)。并发场景用 latch 固定交错；异常场景用子类注入。v7 使用独立测试目录，以适配业务包重构和 getLoad 返回值变化，未改动 v6 的测试。

## scores/6：2026-10-31

2026-10-31，Windows，Oracle JDK 17.0.12，Maven 3.10.0。

从仓库根目录执行：

```text
mvn -f task-scheduler/scores/6/pom.xml test -q
mvn -f task-scheduler/scores/6/pom.xml -Previewer clean test
```

第一条退出码 0，原有测试 25/25 通过。第二条运行 29 项，26 项通过、3 项断言失败、0 errors；原有测试仍为 25/25，独立评审为 1/4。

| 独立评审测试 | 结果 | 观察 |
| --- | --- | --- |
| successfulSchedulingUpdatesTaskMetadata | 通过 | assignee 和 ASSIGNED 状态正确 |
| recordingFailureDoesNotLeaveUnrecordedWorkload | 失败 | 期望负载 0，实际 3 |
| assignmentFailureDoesNotPermanentlyRemoveEmployee | 失败 | 期望仍可取出员工，实际 null |
| anotherSkillQueueMustNotChooseAnEmployeeWhoseLoadHasIncreased | 失败 | 期望负载 5 的员工，实际选中负载 10 的员工 |

独立测试位于 [ReviewInvariantsTest.java](../solution/reviewer-tests/ReviewInvariantsTest.java)，断言目标是正确行为，因此当前版本的 reviewer 构建失败是已确认的产品缺陷。跨技能场景使用 CountDownLatch 控制交错；异常场景通过子类注入失败。它们不依赖随机执行顺序。

切换是否包含 reviewer 测试时使用 clean，防止旧测试 class 残留。报告中的其余封装及并发读写问题来自代码审查，不声称已由这四项测试全部复现。

