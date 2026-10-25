# 独立评审测试

`scores/8` 使用 query8 测试组，共 16 项，适配冲突抛异常契约并增加查询边界。运行 `mvn -f meeting-room-booking/scores/8/pom.xml -Previewer clean test`，原有测试与评审一同执行。

新版 `scores/7` 使用 `query/booking/reviewer/TestQueryBookingReview.java`，适配 Query 参数与无 setter 的 Booking，共 12 项。运行 `mvn -f meeting-room-booking/scores/7/pom.xml -Previewer clean test`。旧版 common 组继续用于 scores/6；不同组因 API 差异而独立，通过率不直接横比。

`common/booking/reviewer/TestBookingReview.java` 包含面向当前 BookingService API 的 JUnit 5 测试。测试由评审新增，不计入原有测试覆盖。未来版本若更换 API，需要增加适配，不能直接声称兼容所有实现。

从仓库根目录运行：

```text
mvn -f meeting-room-booking/scores/6/pom.xml -Previewer clean test
```

去掉 `-Previewer` 仅运行原有测试。仅运行评审测试时，加上 `"-Dtest=booking.reviewer.*"`。具体场景见 [tests.md](../tests.md)。

测试断言正确契约，不为已有缺陷改写预期。BUILD FAILURE 若来自断言失败，表示实现与契约不符；应与编译或依赖失败区分。通过率不直接换算评分或职级。
