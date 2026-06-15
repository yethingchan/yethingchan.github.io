# Git 版本控制技巧

## 相关链接

- [[00-VS开发环境搭建]] - Git 环境准备
- [[03-项目配置与构建]] - 项目文件的结构
- [[05-代码重构与质量工具]] - 代码审查与 Git 配合
- [[驱动插件架构]] - 驱动插件的二进制文件管理
- [[架构总览]] - IoTGateway 项目结构

---

## Git 在上位机项目中的挑战

上位机项目的版本控制比普通 Web 项目更复杂，原因包括：

1. **大量二进制文件**：驱动 DLL、数据库文件、协议定义文件
2. **多分支并行开发**：同时维护生产版本和开发版本
3. **.sln 和 .csproj 冲突**：多人协作时解决方案文件的合并
4. **环境配置差异**：不同开发者的本地配置（串口、IP 地址等）
5. **硬件配置版本**：不同硬件版本对应不同的驱动代码

---

## 一、.gitignore 配置

### 1.1 .NET 项目标准 .gitignore

`.gitignore` 是 Git 项目的第一个文件，它定义了哪些文件不应该被版本控制。

```gitignore
# === .NET 项目标准忽略规则 ===

# 构建输出
[Bb]in/
[Oo]bj/
[Dd]ebug/
[Rr]elease/
x64/
x86/
[Ww][Ii][Nn]32/
build/

# VS 项目文件
.vs/
*.suo
*.user
*.userosscache
*.sln.docstates
*.rsuser

# NuGet 包
**/[Pp]ackages/*
!**/[Pp]ackages/build/
*.nupkg
*.snupkg
**/[Pp]ackages/repositories.config
project.lock.json
project.fragment.lock.json

# 编译生成的文件
*.dll
*.exe
*.pdb
*.cache
*.ilk
*.log
*.lib
*.sbr

# Rider（如果使用）
.idea/
*.sln.iml

# VS Code
.vscode/

# 用户特定文件
*.DotSettings.user
launchSettings.json

# 测试结果
[Tt]est[Rr]esult*/
[Bb]uild[Ll]og.*
coverage/
coveragereport/
*.trx
*.coverage
*.coveragexml

# 发布输出
publish/
artifacts/

# Docker
docker-compose.override.yml

# 操作系统文件
Thumbs.db
ehthumbs.db
Desktop.ini
.DS_Store
```

### 1.2 上位机项目额外忽略规则

```gitignore
# === 上位机项目特定忽略 ===

# 驱动编译输出（DLL 不纳入版本控制）
**/drivers/*.dll
!**/drivers/README.md

# 串口和网络配置文件（每个开发者环境不同）
*.local.json
appsettings.Development.json
appsettings.*.local.json

# 数据库文件（开发环境数据库）
*.db
*.sqlite
*.sqlite3
*.mdf
*.ldf

# 日志文件
logs/
*.log
Serilog*.txt

# 协议跟踪文件（调试时生成的大文件）
*.trace
*.pcap
*.cap

# 设备配置备份
device_backup/
config_backup/

# 临时测试数据
test_data/
mock_responses/
```

### 1.3 不应该忽略的文件

```
需要纳入版本控制的文件：

✓ *.sln                      - 解决方案文件
✓ *.csproj                   - 项目文件
✓ *.cs                       - 源代码
✓ *.json (配置文件模板)       - appsettings.json
✓ *.proto                    - Protocol Buffers 定义
✓ *.xml (协议定义)            - Modbus 寄存器映射
✓ Dockerfile                 - 容器配置
✓ docker-compose.yml         - 编排配置
✓ .editorconfig              - 编码规范
✓ .gitignore                 - Git 忽略规则
✓ NuGet.Config               - NuGet 源配置
✓ Directory.Build.props      - 共享构建属性
✓ packages.lock.json         - 包锁定文件
```

---

## 二、分支策略

### 2.1 Git Flow（推荐用于上位机项目）

上位机项目通常需要同时维护多个版本（现场运行版本和开发版本），Git Flow 非常适合这种场景：

```
分支结构：

main (生产分支)
│   只包含已发布的稳定版本
│   每次发布时从 release 合并
│
├── develop (开发分支)
│   │   集成所有功能的开发分支
│   │
│   ├── feature/modbus-optimization
│   │   Modbus 通信优化功能
│   │
│   ├── feature/opcua-alarm
│   │   OPC UA 告警功能
│   │
│   └── feature/new-plc-driver
│       新增 PLC 驱动
│
├── release/v1.2.0 (发布分支)
│   准备发布的版本，只接受 bug 修复
│
├── hotfix/modbus-timeout (热修复分支)
│   紧急修复生产环境的 Modbus 超时问题
│
└── support/v1.1 (长期支持分支)
    维护旧版本的 bug 修复
```

### 2.2 分支命名规范

```bash
# 功能分支
feature/驱动名称-功能描述
# 例如：
feature/modbus-batch-read        # Modbus 批量读取
feature/opcua-subscription       # OPC UA 订阅功能
feature/s7-1500-support          # S7-1500 PLC 支持

# 修复分支
fix/问题描述
# 例如：
fix/modbus-crc-calculation       # Modbus CRC 计算修复
fix/serial-port-disconnect       # 串口断开重连修复
fix/memory-leak-in-polling       # 轮询内存泄漏修复

# 发布分支
release/v主版本.次版本.修订号
# 例如：
release/v2.1.0

# 热修复分支
hotfix/问题描述
# 例如：
hotfix/critical-plc-crash
```

### 2.3 实际工作流示例

```bash
# 场景：开发新的 Modbus 批量读取功能

# 1. 从 develop 创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/modbus-batch-read

# 2. 开发和提交
git add .
git commit -m "feat(modbus): 实现 Modbus 批量寄存器读取

- 支持连续地址的批量读取优化
- 自动分割超过 125 个寄存器的请求
- 添加读取超时重试机制
- 添加相关单元测试"

# 3. 推送到远程
git push -u origin feature/modbus-batch-read

# 4. 创建 Pull Request（在 GitHub/GitLab 中）
# 目标分支：develop
# 标题：feat(modbus): 实现批量寄存器读取优化

# 5. 代码审查通过后合并
# 在 PR 中使用 Squash Merge，保持提交历史干净

# 6. 删除功能分支
git branch -d feature/modbus-batch-read
git push origin --delete feature/modbus-batch-read
```

---

## 三、处理 .sln 和 .csproj 冲突

### 3.1 .sln 文件冲突

解决方案文件（.sln）是文本格式，但结构复杂，手动合并容易出错。

**预防冲突的策略：**

```
1. 约定：添加新项目时通知团队
   - 一个人添加项目，其他人 pull

2. 使用解决方案文件夹组织项目
   - 减少项目添加时的行冲突

3. 避免频繁重排项目顺序
```

**合并 .sln 文件：**

```bash
# 如果 .sln 冲突，最安全的做法：
# 1. 接受任一方的版本
git checkout --ours IoTGateway.sln     # 接受当前分支
# 或
git checkout --theirs IoTGateway.sln   # 接受对方分支

# 2. 在 VS 中重新打开解决方案
# 3. 手动添加缺失的项目引用
# 4. 验证构建成功
```

### 3.2 .csproj 文件冲突

.csproj 文件是 XML 格式，冲突相对容易处理：

```xml
<!-- 冲突示例 -->
<<<<<<< HEAD
    <PackageReference Include="MQTTnet" Version="4.3.3.952" />
=======
    <PackageReference Include="MQTTnet" Version="4.3.7.1207" />
>>>>>>> feature/update-mqtt

<!-- 解决方法：选择正确的版本 -->
    <PackageReference Include="MQTTnet" Version="4.3.7.1207" />
```

> [!tip] 使用中央包管理减少冲突
> 如果使用 [[04-NuGet包管理]] 中介绍的中央包管理（Directory.Packages.props），.csproj 文件中不再包含版本号，大大减少了包版本冲突的可能性。

### 3.3 配置 .gitattributes

```gitignore
# .gitattributes
# 确保 .sln 文件使用正确的行结尾
*.sln text eol=crlf

# 标记二进制文件
*.dll binary
*.exe binary
*.pdb binary
*.db binary
*.sqlite binary

# 标记 .csproj 为合并友好
*.csproj merge=ours
```

---

## 四、Git LFS 管理大文件

### 4.1 为什么需要 Git LFS

上位机项目中可能包含以下大文件：

| 文件类型 | 典型大小 | 来源 |
|---------|---------|------|
| 厂商驱动 DLL | 5-50 MB | PLC/仪表厂商提供 |
| 协议文档 | 10-100 MB | PDF 格式通信手册 |
| 数据库备份 | 50-500 MB | 测试数据 |
| 固件文件 | 1-20 MB | 设备固件更新 |
| 抓包文件 | 10-200 MB | 调试时捕获的网络包 |

如果不使用 LFS，这些文件会让 Git 仓库迅速膨胀，导致克隆和拉取变慢。

### 4.2 配置 Git LFS

```bash
# 安装 Git LFS
git lfs install

# 追踪大文件类型
git lfs track "*.dll"         # 追踪 DLL 文件
git lfs track "*.exe"         # 追踪可执行文件
git lfs track "*.pdf"         # 追踪文档
git lfs track "*.bin"         # 追踪二进制文件
git lfs track "*.zip"         # 追踪压缩包
git lfs track "*.sqlite"      # 追踪数据库文件

# 提交 .gitattributes 文件
git add .gitattributes
git commit -m "chore: 配置 Git LFS 追踪大文件"
```

### 4.3 LFS 最佳实践

```
Git LFS 使用建议：

1. 只追踪真正需要版本控制的大文件
   ✓ 厂商驱动 DLL（可能更新）
   ✓ 协议定义文件
   ✗ 临时抓包文件（用完即弃）
   ✗ 构建产物（应通过 CI 生成）

2. 设置 LFS 存储限制
   - GitHub: 1 GB 免费存储，50 GB 数据包
   - GitLab: 10 GB 每仓库
   - 自建 LFS 服务器：无限制

3. 定期清理不需要的 LFS 文件
   git lfs prune
```

---

## 五、VS 内置 Git 功能

### 5.1 Git 变更窗口

```
打开方式：Ctrl+0, G 或 视图 → Git 更改
```

VS 的 Git 变更窗口提供了图形化的 Git 操作界面：

- **暂存文件**：勾选文件旁的复选框
- **提交**：输入提交消息并点击"提交"
- **查看差异**：双击文件查看修改内容
- **撤消更改**：右键文件 → 撤消更改

### 5.2 Git 提交历史

```
打开方式：视图 → Git 存储库 或 在 Git 变更窗口点击 "存储库"
```

在提交历史中可以：
- 查看每个提交的变更内容
- 比较两个提交的差异
- 回退到特定提交
- 创建分支

### 5.3 内联 Git Blame

```
操作：右键编辑器 → Git → 注释
```

Git Blame 在代码旁边显示每行的最后修改者和修改时间，非常适合理解代码的变更历史：

```csharp
// 行号  修改者      日期        代码
// 156   张三    2024-01-10  public async Task ReadRegisters()
// 157   张三    2024-01-10  {
// 158   李四    2024-01-15      // 添加超时重试  ← 可以追问李四为什么加这个
// 159   李四    2024-01-15      for (int i = 0; i < 3; i++)
// 160   张三    2024-01-10      {
```

### 5.4 Git 差异比较

VS 的差异比较工具非常适合审查通信协议代码的变更：

```
操作：在 Git 变更窗口中双击文件

特性：
- 左右并排显示修改前后的代码
- 高亮显示变更的行
- 可以逐行接受或拒绝变更
- 支持内联差异显示（修改直接在原文中高亮）
```

---

## 六、命令行 Git 技巧

### 6.1 常用命令速查

```bash
# 状态和日志
git status -s                    # 简短状态
git log --oneline -20            # 最近 20 条提交
git log --graph --oneline --all  # 图形化分支历史
git diff --stat                  # 变更文件统计

# 暂存和提交
git add -p                       # 交互式暂存（逐块选择）
git commit --amend               # 修改最后一个提交
git stash                        # 暂存当前修改
git stash pop                    # 恢复暂存的修改

# 分支
git branch -a                    # 列出所有分支（含远程）
git branch -d feature/xxx        # 删除本地分支
git push origin --delete feature/xxx  # 删除远程分支

# 合并
git merge --squash feature/xxx   # 压缩合并（所有变更合为一个提交）
git rebase -i HEAD~3             # 交互式变基（整理提交历史）

# 标签
git tag -a v1.2.0 -m "Release 1.2.0"  # 创建标签
git push origin v1.2.0                 # 推送标签
git push origin --tags                 # 推送所有标签
```

### 6.2 查看特定文件的变更历史

```bash
# 查看某个驱动文件的完整变更历史
git log --follow -p Driver.Modbus/ModbusDriver.cs

# 查看某个函数的变更历史
git log -L :ReadRegisters:Driver.Modbus/ModbusDriver.cs

# 查看特定时间段的提交
git log --since="2024-01-01" --until="2024-01-31" --author="张三"

# 搜索提交消息
git log --grep="modbus" --oneline
git log --grep="fix" --grep="serial" --all-match
```

### 6.3 找回误删的提交

```bash
# 查看 reflog（所有 Git 操作记录）
git reflog

# 输出示例：
# abc1234 HEAD@{0}: commit: feat: 添加 Modbus 批量读取
# def5678 HEAD@{1}: reset: moving to HEAD~1   ← 误操作！
# ghi9012 HEAD@{2}: commit: feat: 优化通信超时

# 恢复到误删的提交
git cherry-pick abc1234
# 或
git reset --hard abc1234
```

### 6.4 创建提交消息模板

```bash
# 创建提交消息模板文件
# ~/.gitmessage
#
# feat: 简短描述
#
# 详细描述（可选）
#
# 关联问题：#123
# 影响范围：Driver.Modbus
# 测试状态：单元测试通过

# 配置 Git 使用模板
git config --global commit.template ~/.gitmessage
```

---

## 七、提交消息规范

### 7.1 Conventional Commits

上位机项目建议使用 Conventional Commits 规范：

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

**类型（type）：**

| 类型 | 说明 | 上位机场景 |
|------|------|-----------|
| `feat` | 新功能 | 新增 Modbus 批量读取 |
| `fix` | Bug 修复 | 修复串口超时处理 |
| `perf` | 性能优化 | 优化设备轮询效率 |
| `refactor` | 重构 | 重构驱动插件加载机制 |
| `docs` | 文档 | 更新通信协议文档 |
| `test` | 测试 | 添加 Modbus 解析测试 |
| `chore` | 维护 | 更新 NuGet 包版本 |
| `ci` | CI/CD | 配置自动发布流程 |

**作用域（scope）：**

```
常用作用域：
modbus      - Modbus 通信相关
opcua       - OPC UA 相关
mqtt        - MQTT 消息相关
serial      - 串口通信相关
web         - Web 界面相关
core        - 核心逻辑
driver      - 驱动框架
db          - 数据库相关
```

### 7.2 示例

```
feat(modbus): 实现 Modbus RTU 批量寄存器读取

- 支持连续地址自动合并为单次请求
- 超过 125 个寄存器自动分割
- 添加读取超时重试（最多 3 次）
- 添加 CRC16 校验验证

关联：#42
测试：单元测试已添加
```

---

## 八、Git Hooks 自动化

### 8.1 Pre-commit Hook

```bash
# .git/hooks/pre-commit（确保代码可以构建）
#!/bin/sh

echo "正在检查代码是否可以构建..."
dotnet build --no-restore --verbosity quiet

if [ $? -ne 0 ]; then
    echo "❌ 构建失败！请修复编译错误后再提交。"
    exit 1
fi

echo "✅ 构建检查通过"
```

### 8.2 Commit-msg Hook

```bash
# .git/hooks/commit-msg（验证提交消息格式）
#!/bin/sh

commit_msg=$(cat "$1")

# 检查是否遵循 Conventional Commits 格式
if ! echo "$commit_msg" | grep -qE "^(feat|fix|perf|refactor|docs|test|chore|ci)(\(.+\))?: .+"; then
    echo "❌ 提交消息不符合 Conventional Commits 格式"
    echo "正确格式：<type>(<scope>): <description>"
    echo "例如：feat(modbus): 添加批量读取功能"
    exit 1
fi

echo "✅ 提交消息格式检查通过"
```

### 8.3 使用 Husky.NET 管理 Git Hooks

```bash
# 安装 Husky
dotnet tool install --global husky

# 初始化 Husky
husky install

# 添加 pre-commit hook
husky add .husky/pre-commit "dotnet build --no-restore"
```

---

## 九、常见场景处理

### 场景 1：同时开发多个功能

```bash
# 正在开发 feature-A，突然需要修复紧急 bug

# 1. 暂存当前修改
git stash push -m "feature-A: 进行中的 Modbus 批量读取"

# 2. 切换到 main 创建修复分支
git checkout main
git pull origin main
git checkout -b hotfix/serial-crash

# 3. 修复并提交
git add .
git commit -m "fix(serial): 修复串口断开后的空引用异常"

# 4. 回到 feature-A 继续开发
git checkout feature/modbus-batch-read
git stash pop
```

### 场景 2：合并冲突处理

```bash
# 合并 develop 到功能分支时发生冲突
git merge develop

# 冲突文件：IoTGateway.sln
# 处理方法：
# 1. 在 VS 中打开解决方案
# 2. 使用 VS 的三方合并工具
# 3. 手动解决冲突
# 4. 验证构建成功
# 5. git add . && git commit
```

### 场景 3：清理提交历史

```bash
# 合并前整理提交（将多个小提交合并为一个有意义的提交）
git rebase -i HEAD~5

# 在编辑器中：
# pick   abc1234 feat: 添加基础读取功能
# squash def5678 fix: 修复读取偏移
# squash ghi9012 fix: 添加超时处理
# squash jkl3456 style: 格式化代码
# squash mno7890 docs: 添加注释

# 最终合并为一个干净的提交：
# feat(modbus): 实现 Modbus 寄存器读取（含超时重试）
```

### 场景 4：二进制文件变更

```bash
# 查看二进制文件是否变更（看不到 diff）
git diff --stat

# 输出：
# Driver.S7/Resources/s7_client.dll | Bin 52428 -> 53248 bytes

# 处理策略：
# 1. 小 DLL（<1MB）：直接提交
# 2. 大文件（>1MB）：使用 Git LFS
# 3. 可构建的：从源码构建，不提交二进制
```

---

## 十、团队协作规范

```
上位机项目 Git 协作规范总结：

1. 分支规范
   - main: 只接受 release 和 hotfix 合并
   - develop: 日常开发集成分支
   - feature/*: 功能开发分支
   - hotfix/*: 紧急修复分支

2. 提交规范
   - 使用 Conventional Commits 格式
   - 每个提交只做一件事
   - 提交消息说明"为什么"而不只是"做了什么"

3. 代码审查
   - 所有合并必须通过 Pull Request
   - 至少一名同事审查通过
   - CI 构建和测试必须通过

4. 发布流程
   - 从 develop 创建 release 分支
   - 在 release 分支上修复发布前问题
   - 合并到 main 并打标签
   - 合并回 develop

5. 冲突处理
   - .sln 冲突：使用 VS 合并工具
   - .csproj 冲突：手动检查 XML
   - 代码冲突：理解双方意图后合并
   - 二进制文件：通常接受最新版本
```

---

## 小结

Git 版本控制是上位机项目协作的基础。本章涵盖了：

- 完整的 .gitignore 配置（针对 .NET 上位机项目）
- Git Flow 分支策略
- .sln 和 .csproj 冲突处理
- Git LFS 管理二进制文件
- VS 内置 Git 功能
- 命令行 Git 技巧
- 提交消息规范和 Git Hooks

下一章学习 [[07-性能分析与优化]]，掌握上位机程序的性能优化技术。

---

上一篇: [[05-代码重构与质量工具]] | 下一篇: [[07-性能分析与优化]]
