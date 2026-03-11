# 浏览器网关测试用例

## 测试模块覆盖

### 1. Service 模块
- **ChromeSetImplTest**: 浏览器会话管理测试
  - 测试用户管理（获取所有用户）
  - 测试心跳更新和获取
  - 测试不存在用户的处理

- **CseImplTest**: CSE服务注册中心测试
  - 测试外网环境端点获取
  - 测试多次调用一致性

### 2. SDK 模块
- **BrowserOptionsTest**: 浏览器配置选项测试
  - 测试端点地址设置
  - 测试浏览器类型设置
  - 测试无头模式配置
  - 测试URL设置
  - 测试扩展路径配置
  - 测试语言设置
  - 测试默认值

### 3. Adapter 模块
- **AlarmAdapterTest**: 告警适配器测试
  - 测试发送告警
  - 测试清除告警
  - 测试批量发送告警
  - 测试重试机制

### 4. Util 模块
- **UserIdUtilTest**: 用户ID工具测试
  - 测试ID生成
  - 测试ID唯一性
  - 测试ID格式

## 运行测试

### 运行所有测试
```bash
mvn test
```

### 运行特定模块测试
```bash
# Service 模块
mvn test -Dtest=com.huawei.browsergateway.service.*Test

# SDK 模块
mvn test -Dtest=com.huawei.browsergateway.sdk.*Test

# Adapter 模块
mvn test -Dtest=com.huawei.browsergateway.adapter.*Test

# Util 模块
mvn test -Dtest=com.huawei.browsergateway.util.*Test
```

### 运行测试套件
```bash
mvn test -Dtest=BrowserGatewayTestSuite
```

## 测试覆盖率

生成测试覆盖率报告：
```bash
mvn clean test jacoco:report
```

报告位置：`target/site/jacoco/index.html`

## 测试原则

1. **最小功能集覆盖**：每个模块测试核心功能
2. **边界条件测试**：测试空值、null、边界值
3. **异常处理测试**：验证异常情况的处理
4. **Mock使用**：对外部依赖使用Mock隔离

## 扩展测试

如需添加更多测试用例，请遵循以下规范：
- 测试类命名：`<ClassName>Test`
- 测试方法命名：`test<MethodName>_<Scenario>`
- 使用中文描述测试场景
- 遵循 Given-When-Then 模式
