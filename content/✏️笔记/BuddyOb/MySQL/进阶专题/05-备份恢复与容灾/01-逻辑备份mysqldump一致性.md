# 05-备份恢复 · 逻辑备份 mysqldump 与一致性

> 前置：[[MySQL/进阶专题/05-备份恢复与容灾/00-索引]] ｜ MVCC 背景：[[MySQL/进阶专题/01-事务与锁深度/01-MVCC原理undo版本链与ReadView]]

## 逻辑备份是什么

`mysqldump` 把**数据导出成 SQL（INSERT 语句）或 CSV**。恢复 = 重跑这些 SQL。

## 关键：一致性怎么保

### InnoDB + 单事务（最常用，不锁表）

```bash
mysqldump --single-transaction --routines --triggers --events \
  -uroot -p mydb > mydb_$(date +%F).sql
```
- `--single-transaction`：备份开始时开一个**一致性快照读**（RR 级别 ReadView），整个导出看到的是同一时刻的快照。
- **不锁表**，业务照常读写（靠 MVCC，见 01-事务）。
- 前提：全是 InnoDB。MyISAM 不支持 MVCC → 要锁表。

### MyISAM / 混合表（锁表保一致）

```bash
mysqldump --lock-all-tables -uroot -p mydb > dump.sql
```
- `--lock-all-tables`：备份期间**全局读锁（FTWRL 类）**，表只读 → 一致但业务停写。

### 按表 / 按条件

```bash
mysqldump -uroot -p mydb t_user > t_user.sql        # 单表
mysqldump -uroot -p mydb t_order --where="create_time>'2026-01-01'" > part.sql
```

## 导出格式选项

| 选项 | 效果 |
|------|------|
| 默认 | 长 INSERT（多行合并），恢复快 |
| `--opt`（默认开） | 含 `--add-drop-table`/`--extended-insert` 等优化 |
| `--skip-extended-insert` | 每行一个 INSERT（便于 grep 单条，但恢复慢） |
| `--tab=dir` | 表结构 + 数据 TSV 分开（LOAD DATA 快） |
| `--hex-blob` | blob 十六进制，避免字符问题 |

## 恢复

```bash
mysql -uroot -p mydb < mydb_2026-01-01.sql
# 大文件可加 --force 跳过错误继续，或 pv 看进度
pv mydb.sql | mysql -uroot -p mydb
```

## 逻辑备份的优缺点

| 优点 | 缺点 |
|------|------|
| 跨版本/跨实例可恢复 | **慢**（大库几小时） |
| 可读、可单表/单行恢复 | 恢复也慢（要重跑 INSERT） |
| 不依赖存储引擎 | 备份期间若表结构变会不一致（非单事务时） |

## 适用场景

- 中小库（< 几百 GB）的**日常全量备份**。
- 单表/单行找回（用 `--where` 或恢复时 grep 单表）。
- 跨版本迁移。

> 大库（TB 级）逻辑备份太慢 → 用物理备份 xtrabackup（见 02）。

## 与 binlog 的配合

`mysqldump` 全量 + **binlog 增量** = 完整恢复链（见 03 PITR）。
- 导出时记录 binlog 位点：`--master-data=2`（SQL 里写 `CHANGE MASTER` 注释，含位点）。

## 结论

- InnoDB 用 `--single-transaction`：**不锁表、靠 MVCC 一致快照**。
- MyISAM/混合用 `--lock-all-tables`：锁表保一致。
- 逻辑备份可读可单表恢复，但**慢**，适合中小库日常全量。

下一步：[[MySQL/进阶专题/05-备份恢复与容灾/02-物理备份xtrabackup]]
