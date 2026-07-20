# 05-备份恢复 · 物理备份 xtrabackup（Percona）

> 前置：[[MySQL/进阶专题/05-备份恢复与容灾/01-逻辑备份mysqldump一致性]]

## 物理备份是什么

直接拷贝 **InnoDB 的 ibd 数据文件 + redo log**，恢复时把文件放回数据目录。比逻辑备份**快几个数量级**（不用重跑 SQL）。

## xtrabackup（Percona XtraBackup，开源）

### 全量备份

```bash
xtrabackup --backup --target-dir=/backup/full \
  --user=root --password=xxx
```
- 边备份边**拷贝 redo log**（持续追），保证备份期间写入也纳入。
- 备份完成要做 **prepare（应用 redo）** 才能让数据"一致可启动"：

```bash
xtrabackup --prepare --target-dir=/backup/full
```
> 不 prepare 就恢复 → 数据不一致（像"断电瞬间拷的文件"）。

### 增量备份（省空间）

```bash
# 基于上次全量/增量的 lsn
xtrabackup --backup --target-dir=/backup/inc1 \
  --incremental-basedir=/backup/full --user=root -p
```
- 只拷"上次以来改过的页"（基于 LSN）。
- 恢复链：全量 → prepare 全量 → 依次 apply 增量 → 整体 prepare。

### 恢复

```bash
systemctl stop mysql
rm -rf /var/lib/mysql/*                 # 清空数据目录
xtrabackup --copy-back --target-dir=/backup/full_prepared
chown -R mysql:mysql /var/lib/mysql
systemctl start mysql
```

## prepare 的原理（为什么必要）

备份过程中数据库还在写，拷贝的数据文件是"时间点 A 的页 + 期间 redo"。prepare 阶段**把 redo 重放**到数据页，让所有页达到"同一一致点"（类似崩溃恢复，见 [[MySQL/进阶专题/01-事务与锁深度/02-redo日志与崩溃恢复两阶段提交]]）。

## 物理 vs 逻辑

| 维度 | xtrabackup 物理 | mysqldump 逻辑 |
|------|-------------------|-------------------|
| 速度 | **快**（TB 级可行） | 慢 |
| 恢复速度 | 快（拷文件） | 慢（重跑 SQL） |
| 粒度 | **整实例/整库** | 可单表/单行 |
| 跨版本/跨引擎 | 同版本同引擎 | 灵活 |
| 可读性 | 不可读（二进制） | 可读 SQL |

## 适用场景

- **大库（百 GB ~ TB）** 的每日全量 + 增量。
- 需要**快速恢复**（RTO 短）的关键业务。
- 搭建从库（备份 + 位点 → 接主库）。

## 8.0 注意

- MySQL 8 用 `xtrabackup 8.0`（支持 redo 新格式、data dictionary 取代 frm）。
- 老版本 `xtrabackup 2.4` 只支持 5.7 及以前。

## 结论

- 物理备份 = 拷数据文件 + redo，恢复快几个量级，**大库必用**。
- 备份后**必须 prepare（应用 redo）** 才一致。
- 增量按 LSN，恢复链 全量→增量→整体 prepare。
- 跨版本/单表细粒度恢复，逻辑备份更灵活。

下一步：[[MySQL/进阶专题/05-备份恢复与容灾/03-PITR时间点恢复与闪回]]
