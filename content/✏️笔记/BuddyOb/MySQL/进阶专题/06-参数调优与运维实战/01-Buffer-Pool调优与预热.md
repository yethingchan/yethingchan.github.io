# 06-参数调优 · Buffer Pool 调优与预热

> 前置：[[MySQL/进阶专题/06-参数调优与运维实战/00-索引]] ｜ 页结构：[[MySQL/进阶专题/02-索引底层与优化器执行计划/01-B+树页结构与回表]]

## Buffer Pool 是什么

InnoDB 的**内存数据缓存**（默认 128MB，太小）。所有**读**先查 Buffer Pool，命中就不碰磁盘；**写**先改内存页（脏页），后台刷盘。它是 MySQL 性能的第一杠杆。

```
SELECT 某行：
  Buffer Pool 命中 → 直接返回（纳秒级）
  Buffer Pool 未命中 → 磁盘读页进 Buffer Pool（毫秒级，慢 1000 倍）
```

## `innodb_buffer_pool_size` —— 最重要的参数

| 规则 | 值 |
|------|-----|
| 专用 MySQL 服务器 | **物理内存的 60%~80%** |
| 和别的吃内存服务混布 | 留足对方内存后取剩余大部分 |
| 太小（默认 128M） | 热数据装不下 → 频繁磁盘读 → 巨慢 |

> 本仓库开发机默认小，生产一定要调大。调完**重启生效**。

## 命中率怎么看

```sql
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_read%';
-- 命中率 ≈ 1 - (read_disk / (read_disk + read_ahead + read_page))
-- 经验：> 99% 好；< 95% 考虑加大 buffer pool
```
> 命中率低 = 在频繁读磁盘，先把 buffer pool 调大再说。

## Buffer Pool 实例数（大内存必调）

```
innodb_buffer_pool_instances = 8   # 默认 1（< 1GB 时）
```
- 单个大 Buffer Pool 的**并发访问要加全局锁** → 多实例把锁分散（每实例一个锁）。
- 经验：每实例 **1GB** 左右，8~16 实例常见。
- 仅当 `buffer_pool_size > 1GB` 时多实例才生效。

## 脏页刷盘（别等崩了再刷）

```
innodb_max_dirty_page_pct = 90      # 脏页占比上限（到这比例开始猛刷）
innodb_io_capacity = 2000          # 后台刷盘/落盘 IO 能力（按磁盘 iops 设）
innodb_io_capacity_max = 4000       # 紧急情况上限
```
- `io_capacity` 设太低 → 脏页堆积，突发要刷时卡顿。
- 设太高 → 刷盘占用 IO，影响前台查询。按**磁盘真实 iops** 设（SSD 可到几千）。

## 预热（warmup）—— 重启别"冷启动"

MySQL 重启后 Buffer Pool 是空的 → 所有查询都"未命中" → 雪崩式慢（直到热数据重新进内存）。

### 8.0 自动预热（推荐）

```ini
innodb_buffer_pool_dump_at_shutdown = ON   # 关库时把热页列表存盘
innodb_buffer_pool_load_at_startup  = ON   # 启动时按列表把热页加载回内存
```
> 8.0 默认 ON。效果：重启后很快恢复到关闭前的命中率，避免冷启动慢查询。

### 手动预热（老版本）

```sql
-- 重启后手动 touch 热表，把数据读进 Buffer Pool
SELECT COUNT(*) FROM hot_table FORCE INDEX(PRIMARY);
SELECT * FROM hot_table ORDER BY pk;   -- 或全表扫一遍
```

## Buffer Pool 里的"链表"管理（原理）

- **LRU 链表**：最近用的放头，久不用的淘汰（近似 LRU，有 `young`/`old` 分代，防全表扫把热数据冲掉）。
- **flush 链表**：记录脏页，后台按 `io_capacity` 刷盘。
- **free 链表**：空闲页。

## 结论

- 第一参数 `innodb_buffer_pool_size` = 内存 60~80%，命中率盯 > 99%。
- 大内存配 `buffer_pool_instances`（每 ~1GB 一个）分散锁。
- `io_capacity` 按磁盘 iops 设，防脏页堆积。
- 8.0 开 `dump/load_at_*` 自动预热，告别冷启动雪崩。

下一步：[[MySQL/进阶专题/06-参数调优与运维实战/02-连接线程与wait_timeout]]
