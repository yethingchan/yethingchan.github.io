# 06-参数调优 · 大表 DDL（gh-ost / pt-osc / instant）

> 前置：[[MySQL/进阶专题/06-参数调优与运维实战/02-连接线程与wait_timeout]]

## 为什么大表 `ALTER` 是事故

```
ALTER TABLE big_order ADD COLUMN remark VARCHAR(255);   -- 几千万行
```
- **5.6 前**：锁全表、建新表拷数据、改名 → **几千万行几分钟到几小时锁表**，线上不可接受。
- 即便 Online DDL，也分"能 inplace"和"要拷表"两类，拷表类仍占 IO/锁。

## Online DDL（5.6+ 内置）的两类

| 类型 | 行为 | 例子 |
|------|------|------|
| **INSTANT** | 只改元数据，**秒级**，不碰数据 | 8.0 加**末尾**列、删列、改默认值 |
| **INPLACE**（algorithm=inplace） | 原地改，不拷全表，但可能锁（短暂） | 加索引（8.0 多数 inplace）、改字符集 |
| **COPY**（algorithm=copy） | 建新表拷数据，**慢+锁** | 改列类型（如 INT→BIGINT 跨字节）、改主键 |

> 加索引在 8.0 是 inplace（不拷表），但**仍会短暂锁 + 占 IO**，大表也要挑低峰。

## `ALGORITHM` / `LOCK` 显式指定

```sql
ALTER TABLE t ADD COLUMN c INT, ALGORITHM=INPLACE, LOCK=NONE;
-- LOCK=NONE 强制"不许锁"，若做不到直接报错（不偷偷锁你）
```
- `LOCK=NONE`：全程不排他锁（最安全意图）。
- `LOCK=SHARED` / `EXCLUSIVE`：显式要共享/排他锁。
- 加 `ALGORITHM=INPLACE, LOCK=NONE` 是"安全 ALTER 声明"，做不到就失败而非默默锁表。

## `INSTANT` DDL（8.0 神器）

```
8.0 起这些操作 INSTANT（只改数据字典，秒级，不碰数据行）：
  - 表末尾加列
  - 删除列
  - 改列的默认值
  - 加/删虚拟列
  - RENAME（部分）
```
- 加**末尾**列是 INSTANT；但**中间插入列**仍不是（会触发重建）。
- 限制：一张表 INSTANT 加列有数量上限（`innodb_max_instant_columns`），超限后退化。

## 大表加索引的安全做法

即便 inplace，大表加索引仍会：
- 占大量 IO（排序建索引）。
- 可能短暂锁（取决于操作）。

**做法**：
1. 低峰期执行。
2. `ALGORITHM=INPLACE, LOCK=NONE`。
3. 或**用 gh-ost/pt-osc** 更稳（见下）。

## gh-ost（GitHub 开源，最稳的大表 DDL）

原理：**用 binlog 增量同步**，不锁源表：

```
1) 建一张"影子表"（带新结构，空的）
2) 在影子表上 ALTER（空表，秒级）
3) 三个流同步数据：
   a. 历史数据：分批拷贝源表 → 影子表
   b. 增量变更：订阅源表 binlog → 实时应用到影子表
   c. 最后切表名（原子 rename，极短锁）
```
- **几乎不锁源表**（只有最后 rename 一瞬），业务无感。
- 比原生 Online DDL 更可控、可暂停/限流/监控进度。
- 依赖 **binlog = ROW**。

```bash
gh-ost \
  --alter="ADD COLUMN remark VARCHAR(255)" \
  --execute \
  --max-load=Threads_running=50 \    # 负载高自动暂停
  --chunk-size=1000                  # 分批拷贝大小
  --table=big_order --database=mydb
```

## pt-online-schema-change（Percona，老牌）

和 gh-ost 思路类似（建影子表 + 触发器/或 binlog 同步增量），用触发器捕获变更：
```
pt-online-schema-change \
  --alter="ADD COLUMN remark VARCHAR(255)" \
  D=mydb,t=big_order \
  --execute --chunk-size=1K --max-load=Threads_running=50
```
- 老版本靠 **触发器**（对源表有写放大），gh-ost 用 binlog 更轻。
- 新项目优先 gh-ost。

## 选择建议

| 场景 | 用 |
|------|-----|
| 小表（< 百万） | 原生 Online DDL（`LOCK=NONE`） |
| 大表加索引/末尾加列 | 低峰原生 inplace，或 gh-ost 更稳 |
| 大表改列类型/改主键（必拷表） | **gh-ost/pt-osc**（可控、可暂停） |
| 8.0 末尾加列/改默认值 | INSTANT（秒级，首选） |

## 结论

- 大表 ALTER 的根险是"锁表 + 拷数据"。
- 8.0 优先用 **INSTANT**（末尾加列/改默认值，秒级）。
- 加索引低峰原生 inplace（`LOCK=NONE`）即可。
- 大表拷表类 DDL 用 **gh-ost**（binlog 同步，几乎不锁）。
- 永远显式加 `ALGORITHM/LOCK` 声明，做不到就失败而非默锁。

下一步：[[MySQL/进阶专题/06-参数调优与运维实战/04-碎片整理与表空间]]
