# 05-备份恢复 · PITR 时间点恢复与闪回

> 前置：[[MySQL/进阶专题/05-备份恢复与容灾/02-物理备份xtrabackup]] ｜ binlog：[[MySQL/进阶专题/04-高可用复制与集群/01-主从复制原理binlog三格式]]

## PITR 是什么

**Point-In-Time Recovery（时间点恢复）**：用"**全量备份** + **binlog 增量**"恢复到任意历史时刻。

```
全量备份(昨晚 02:00)
  + binlog 从 02:00 到现在
  = 能恢复到任意时间点（如误删前的 14:32）
```

## 为什么需要 PITR

全量备份只到"备份时刻"。之后到现在的改动全在 **binlog** 里。只恢复全量 → 丢了一天数据。PITR 用 binlog 补上这段时间。

## 步骤（逻辑备份版示例）

### 1) 全量备份（带位点）

```bash
mysqldump --single-transaction --master-data=2 mydb > full.sql
# --master-data=2 在文件头注释里记下备份时的 binlog 位点/GTID
```

### 2) 误删发生（比如 14:35 DROP TABLE）

### 3) 先恢复全量

```bash
mysql mydb < full.sql
```

### 4) 再回放 binlog 到"误删前一刻"（14:34）

```bash
# 方式 A：按时间
mysqlbinlog --start-datetime="2026-01-01 02:00:00" \
             --stop-datetime="2026-01-01 14:34:00" \
             /var/lib/mysql/binlog.000123 | mysql mydb

# 方式 B：按位点（GTID 更稳）
mysqlbinlog --exclude-gtid='...误删那笔...' binlog.* | mysql mydb
```
- `--stop-datetime` 卡在误删**之前** → 不重放误删语句。
- GTID 模式用 `--exclude-gtid` 精确跳过那笔坏事。

## 闪回（Flashback）—— 不恢复全量，直接"反向"

误 `DELETE`/`UPDATE` 了一批，想"原地改回去"而不是整库回放：

### binlog2sql（美团开源思路）

binlog 是 **ROW 格式**（见 04-01），每行记录改前/改后值。闪回工具把它**反向生成补偿 SQL**：

```
原 binlog：DELETE id=5 (old: name='张')
闪回生成：INSERT id=5 (name='张')     ← 反向补回
```
```bash
# 解析出误删的反向 SQL（仅预览）
binlog2sql -h127.0.0.1 -P3306 -uroot -p \
  --start-datetime="2026-01-01 14:30:00" \
  --stop-datetime="2026-01-01 14:36:00" \
  -d mydb -t t_user --flashback > rollback.sql
mysql mydb < rollback.sql
```

### 前提与限制

- binlog 必须是 **ROW 格式**（STATEMENT 没法精确反向）。
- `binlog_row_image=FULL`（默认）→ 改前改后都留，才能反向。
- `TRUNCATE`/`DROP` 这类 DDL **无法闪回**（表都没了，要整库 PITR）。
- 生产闪回前**先备份当前状态**，防二次事故。

## 闪回 vs PITR 怎么选

| 场景 | 用 |
|------|-----|
| 误 `DELETE/UPDATE` 一批（DML） | **闪回**（反向补偿，快、影响小） |
| 误 `DROP/TRUNCATE`（DDL） | **PITR 整库/整表恢复到删前** |
| 要回到"任意历史时刻" | PITR（binlog 回放） |

## 关键：binlog 必须开着且保留够久

```ini
[mysqld]
log-bin = mysql-bin       # 开 binlog
binlog_format = ROW
expire_logs_days = 7     # 保留 7 天（8.0 用 binlog_expire_logs_seconds）
```
- 没开 binlog → **无法 PITR/闪回**，只能认栽。
- 保留时长 ≥ 两次全量备份间隔（否则早的 binlog 被清了接不上）。

## 结论

- PITR = 全量 + binlog 回放到误删**前一刻**。
- 闪回 = ROW binlog 反向生成补偿 SQL，适合误 DML，快且影响小。
- DDL（DROP/TRUNCATE）只能 PITR，闪回无效。
- **binlog 必须开 + 保留够久**，否则一切免谈。

下一步：[[MySQL/进阶专题/05-备份恢复与容灾/04-误删库表应急流程]]
