# 01 - SQLite 数据库

---

## 一、知识讲解

### 1.1 SQLite 简介

SQLite 是一个轻量级的嵌入式关系型数据库，不需要单独的数据库服务器进程，数据存储在一个本地文件中。它是上位机数据存储的最佳选择之一。

#### 为什么上位机选择 SQLite

- **零配置**：不需要安装数据库服务，一个 `.db` 文件即可
- **轻量级**：DLL 不到 1MB，内存占用极低
- **单文件**：数据库名就是一个文件名，方便备份和迁移
- **SQL支持**：支持标准 SQL 语法，JOIN、子查询、事务等
- **并发**：支持多线程并发读（写操作会锁定）
- **性能**：对于上位机数据量（百万级以内），性能完全足够

#### 安装 NuGet 包

```bash
# 原生 SQLite 访问库
dotnet add package System.Data.SQLite

# 或者使用 Microsoft 提供的轻量库（推荐 .NET Core/.NET 5+）
dotnet add package Microsoft.Data.Sqlite

# Dapper ORM（推荐）
dotnet add package Dapper
```

### 1.2 创建数据库和表

```csharp
using Microsoft.Data.Sqlite;

// 创建数据库连接（文件不存在会自动创建）
string connectionString = "Data Source=plc_data.db";
using var connection = new SqliteConnection(connectionString);
connection.Open();

// 创建表的SQL
string createTableSql = @"
    CREATE TABLE IF NOT EXISTS plc_data (
        Id          INTEGER PRIMARY KEY AUTOINCREMENT,
        DeviceId    TEXT    NOT NULL,       -- 设备标识
        TagName     TEXT    NOT NULL,       -- 数据点名称
        Value       REAL    NOT NULL,       -- 数据值
        Quality     INTEGER DEFAULT 1,     -- 数据质量（1=好, 0=坏）
        Timestamp   TEXT    NOT NULL,       -- 时间戳（ISO8601格式）
        Remark      TEXT                    -- 备注
    );

    CREATE INDEX IF NOT EXISTS idx_plc_data_timestamp
        ON plc_data(Timestamp);
    CREATE INDEX IF NOT EXISTS idx_plc_data_device_tag
        ON plc_data(DeviceId, TagName);

    CREATE TABLE IF NOT EXISTS alarm_records (
        Id          INTEGER PRIMARY KEY AUTOINCREMENT,
        DeviceId    TEXT    NOT NULL,
        AlarmType   TEXT    NOT NULL,       -- 报警类型
        AlarmLevel  INTEGER NOT NULL,       -- 报警级别（1=提示, 2=警告, 3=严重）
        Message     TEXT    NOT NULL,       -- 报警描述
        Value       REAL,                    -- 触发时的值
        Threshold   REAL,                    -- 阈值
        Timestamp   TEXT    NOT NULL,
        AckStatus   INTEGER DEFAULT 0       -- 确认状态（0=未确认, 1=已确认）
    );
";

using var command = connection.CreateCommand();
command.CommandText = createTableSql;
command.ExecuteNonQuery();

Console.WriteLine("数据库和表创建成功");
```

### 1.3 CRUD 操作（增删改查）

```csharp
using Microsoft.Data.Sqlite;

public class PlcDataRepository
{
    private readonly string _connectionString;

    public PlcDataRepository(string dbPath)
    {
        _connectionString = $"Data Source={dbPath}";
    }

    // ===== 增加（Insert）=====

    /// <summary>
    /// 插入一条PLC数据记录
    /// </summary>
    public void InsertPlcData(
        string deviceId, string tagName, double value, string timestamp)
    {
        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        string sql = @"
            INSERT INTO plc_data (DeviceId, TagName, Value, Quality, Timestamp)
            VALUES (@deviceId, @tagName, @value, @quality, @timestamp)";

        using var cmd = new SqliteCommand(sql, conn);
        cmd.Parameters.AddWithValue("@deviceId", deviceId);
        cmd.Parameters.AddWithValue("@tagName", tagName);
        cmd.Parameters.AddWithValue("@value", value);
        cmd.Parameters.AddWithValue("@quality", 1);
        cmd.Parameters.AddWithValue("@timestamp", timestamp);

        cmd.ExecuteNonQuery();
    }

    // ===== 查询（Read）=====

    /// <summary>
    /// 查询某设备某标签的最新N条数据
    /// </summary>
    public List<(string DeviceId, string TagName, double Value, string Timestamp)>
        QueryRecentData(string deviceId, string tagName, int count = 100)
    {
        var results = new List<(string, string, double, string)>();

        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        string sql = @"
            SELECT DeviceId, TagName, Value, Timestamp
            FROM plc_data
            WHERE DeviceId = @deviceId AND TagName = @tagName
            ORDER BY Timestamp DESC
            LIMIT @count";

        using var cmd = new SqliteCommand(sql, conn);
        cmd.Parameters.AddWithValue("@deviceId", deviceId);
        cmd.Parameters.AddWithValue("@tagName", tagName);
        cmd.Parameters.AddWithValue("@count", count);

        using var reader = cmd.ExecuteReader();
        while (reader.Read())
        {
            results.Add((
                reader.GetString(0),
                reader.GetString(1),
                reader.GetDouble(2),
                reader.GetString(3)
            ));
        }

        return results;
    }

    /// <summary>
    /// 按时间范围查询数据
    /// </summary>
    public List<(double Value, string Timestamp)> QueryByTimeRange(
        string deviceId, string tagName,
        string startTime, string endTime)
    {
        var results = new List<(double, string)>();

        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        string sql = @"
            SELECT Value, Timestamp
            FROM plc_data
            WHERE DeviceId = @deviceId
              AND TagName = @tagName
              AND Timestamp >= @startTime
              AND Timestamp <= @endTime
            ORDER BY Timestamp ASC";

        using var cmd = new SqliteCommand(sql, conn);
        cmd.Parameters.AddWithValue("@deviceId", deviceId);
        cmd.Parameters.AddWithValue("@tagName", tagName);
        cmd.Parameters.AddWithValue("@startTime", startTime);
        cmd.Parameters.AddWithValue("@endTime", endTime);

        using var reader = cmd.ExecuteReader();
        while (reader.Read())
        {
            results.Add((reader.GetDouble(0), reader.GetString(1)));
        }

        return results;
    }

    // ===== 更新（Update）=====

    /// <summary>
    /// 更新报警记录的确认状态
    /// </summary>
    public void AcknowledgeAlarm(int alarmId)
    {
        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        string sql = @"
            UPDATE alarm_records
            SET AckStatus = 1
            WHERE Id = @id";

        using var cmd = new SqliteCommand(sql, conn);
        cmd.Parameters.AddWithValue("@id", alarmId);
        cmd.ExecuteNonQuery();
    }

    // ===== 删除（Delete）=====

    /// <summary>
    /// 删除指定时间之前的旧数据（数据清理）
    /// </summary>
    public int DeleteOlderThan(string timestamp)
    {
        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        string sql = "DELETE FROM plc_data WHERE Timestamp < @timestamp";

        using var cmd = new SqliteCommand(sql, conn);
        cmd.Parameters.AddWithValue("@timestamp", timestamp);
        return cmd.ExecuteNonQuery();
    }

    /// <summary>
    /// 删除某设备的所有数据
    /// </summary>
    public int DeleteByDevice(string deviceId)
    {
        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        string sql = "DELETE FROM plc_data WHERE DeviceId = @deviceId";

        using var cmd = new SqliteCommand(sql, conn);
        cmd.Parameters.AddWithValue("@deviceId", deviceId);
        return cmd.ExecuteNonQuery();
    }
}
```

### 1.4 参数化查询（防 SQL 注入）

```csharp
using Microsoft.Data.Sqlite;

/// <summary>
/// SQL注入防护
/// 永远不要直接拼接SQL字符串！必须使用参数化查询
/// </summary>
public class SqlInjectionPrevention
{
    // ===== 错误示范：字符串拼接（存在SQL注入风险）=====
    // 绝对不要这样做！
    public void WrongWay(SqliteConnection conn, string deviceId)
    {
        string sql = $"SELECT * FROM plc_data WHERE DeviceId = '{deviceId}'";
        // 如果deviceId是 "'; DROP TABLE plc_data; --"，后果严重
    }

    // ===== 正确方式：参数化查询 =====
    public void CorrectWay(SqliteConnection conn, string deviceId)
    {
        string sql = "SELECT * FROM plc_data WHERE DeviceId = @deviceId";

        using var cmd = new SqliteCommand(sql, conn);
        cmd.Parameters.AddWithValue("@deviceId", deviceId);
        // AddWithValue 会自动处理特殊字符，防止SQL注入

        using var reader = cmd.ExecuteReader();
        while (reader.Read())
        {
            // 安全地读取数据
        }
    }

    // ===== 批量参数化插入 =====
    public void BatchInsertWithParameters(
        SqliteConnection conn, List<(string Device, string Tag, double Value, string Time)> records)
    {
        string sql = @"
            INSERT INTO plc_data (DeviceId, TagName, Value, Timestamp)
            VALUES (@device, @tag, @value, @time)";

        using var transaction = conn.BeginTransaction();

        try
        {
            foreach (var record in records)
            {
                using var cmd = new SqliteCommand(sql, conn, transaction);
                cmd.Parameters.AddWithValue("@device", record.Device);
                cmd.Parameters.AddWithValue("@tag", record.Tag);
                cmd.Parameters.AddWithValue("@value", record.Value);
                cmd.Parameters.AddWithValue("@time", record.Time);
                cmd.ExecuteNonQuery();
            }

            transaction.Commit();
        }
        catch
        {
            transaction.Rollback();
            throw;
        }
    }
}
```

### 1.5 事务处理

```csharp
using Microsoft.Data.Sqlite;
using System;
using System.Collections.Generic;

/// <summary>
/// 事务处理
/// 多条SQL操作要么全部成功，要么全部回滚
/// 保证数据一致性
/// </summary>
public class TransactionExample
{
    private readonly string _connectionString;

    /// <summary>
    /// 使用事务批量插入数据
    /// 优势：批量插入比逐条插入快10-100倍
    /// </summary>
    public void BatchInsert(List<(string Device, string Tag, double Value, string Time)> records)
    {
        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        // 开启事务
        using var transaction = conn.BeginTransaction();
        try
        {
            string sql = @"
                INSERT INTO plc_data (DeviceId, TagName, Value, Quality, Timestamp)
                VALUES (@device, @tag, @value, 1, @time)";

            foreach (var record in records)
            {
                using var cmd = new SqliteCommand(sql, conn, transaction);
                cmd.Parameters.AddWithValue("@device", record.Device);
                cmd.Parameters.AddWithValue("@tag", record.Tag);
                cmd.Parameters.AddWithValue("@value", record.Value);
                cmd.Parameters.AddWithValue("@time", record.Time);
                cmd.ExecuteNonQuery();
            }

            // 所有插入成功，提交事务
            transaction.Commit();
            Console.WriteLine($"批量插入 {records.Count} 条记录成功");
        }
        catch (Exception ex)
        {
            // 任何一条失败，全部回滚
            transaction.Rollback();
            Console.WriteLine($"批量插入失败，已回滚: {ex.Message}");
            throw;
        }
    }

    /// <summary>
    /// 事务示例：同时写入数据和更新统计
    /// </summary>
    public void InsertAndUpdateStats(string device, string tag, double value)
    {
        using var conn = new SqliteConnection(_connectionString);
        conn.Open();

        using var transaction = conn.BeginTransaction();
        try
        {
            // 操作1：插入数据
            using (var cmd1 = new SqliteCommand(
                "INSERT INTO plc_data (...) VALUES (...)", conn, transaction))
            {
                cmd1.Parameters.AddWithValue("@value", value);
                cmd1.ExecuteNonQuery();
            }

            // 操作2：更新统计信息
            using (var cmd2 = new SqliteCommand(
                "UPDATE device_stats SET LastValue = @value, UpdateTime = @time WHERE DeviceId = @device",
                conn, transaction))
            {
                cmd2.Parameters.AddWithValue("@device", device);
                cmd2.Parameters.AddWithValue("@value", value);
                cmd2.Parameters.AddWithValue("@time", DateTime.Now.ToString("o"));
                cmd2.ExecuteNonQuery();
            }

            transaction.Commit();
        }
        catch
        {
            transaction.Rollback();
            throw;
        }
    }
}
```

### 1.6 Dapper ORM 使用

```csharp
using Dapper;
using Microsoft.Data.Sqlite;
using System;
using System.Collections.Generic;
using System.Linq;

/// <summary>
/// Dapper ORM 使用示例
/// Dapper是轻量级ORM，在原生ADO.NET之上提供简洁的API
/// 优势：代码简洁、类型安全、自动映射、性能接近原生
/// </summary>

// ===== 定义实体类 =====
public class PlcData
{
    public int Id { get; set; }
    public string DeviceId { get; set; }
    public string TagName { get; set; }
    public double Value { get; set; }
    public int Quality { get; set; }
    public string Timestamp { get; set; }
    public string Remark { get; set; }
}

public class AlarmRecord
{
    public int Id { get; set; }
    public string DeviceId { get; set; }
    public string AlarmType { get; set; }
    public int AlarmLevel { get; set; }
    public string Message { get; set; }
    public double? Value { get; set; }
    public double? Threshold { get; set; }
    public string Timestamp { get; set; }
    public int AckStatus { get; set; }
}

public class PlcDataDapperRepository
{
    private readonly string _connectionString;

    public PlcDataDapperRepository(string dbPath)
    {
        _connectionString = $"Data Source={dbPath}";
    }

    /// <summary>
    /// Dapper查询：自动映射到实体类
    /// </summary>
    public PlcData GetById(int id)
    {
        using var conn = new SqliteConnection(_connectionString);
        // Dapper一行代码完成查询+映射
        return conn.QueryFirstOrDefault<PlcData>(
            "SELECT * FROM plc_data WHERE Id = @Id",
            new { Id = id });
    }

    /// <summary>
    /// Dapper查询列表
    /// </summary>
    public List<PlcData> GetRecentData(string deviceId, string tagName, int count = 100)
    {
        using var conn = new SqliteConnection(_connectionString);
        return conn.Query<PlcData>(
            @"SELECT * FROM plc_data
              WHERE DeviceId = @DeviceId AND TagName = @TagName
              ORDER BY Timestamp DESC LIMIT @Count",
            new { DeviceId = deviceId, TagName = tagName, Count = count })
            .ToList();
    }

    /// <summary>
    /// Dapper插入（返回自增ID）
    /// </summary>
    public int Insert(PlcData data)
    {
        using var conn = new SqliteConnection(_connectionString);
        string sql = @"
            INSERT INTO plc_data (DeviceId, TagName, Value, Quality, Timestamp, Remark)
            VALUES (@DeviceId, @TagName, @Value, @Quality, @Timestamp, @Remark)";
        // Dapper自动返回插入后的ID
        return conn.ExecuteScalar<int>(sql, data);
    }

    /// <summary>
    /// Dapper批量插入（配合事务，性能极高）
    /// </summary>
    public void BulkInsert(List<PlcData> dataList)
    {
        using var conn = new SqliteConnection(_connectionString);
        // Dapper的Execute方法支持批量操作
        conn.Execute(@"
            INSERT INTO plc_data (DeviceId, TagName, Value, Quality, Timestamp)
            VALUES (@DeviceId, @TagName, @Value, @Quality, @Timestamp)",
            dataList);
        // 注意：SQLite单次事务中有语句数量限制（约500条）
        // 大量数据建议分批或使用InsertMany扩展
    }

    /// <summary>
    /// Dapper更新
    /// </summary>
    public bool Update(PlcData data)
    {
        using var conn = new SqliteConnection(_connectionString);
        int rows = conn.Execute(@"
            UPDATE plc_data
            SET Value = @Value, Quality = @Quality, Remark = @Remark
            WHERE Id = @Id", data);
        return rows > 0;
    }

    /// <summary>
    /// Dapper删除
    /// </summary>
    public int DeleteOlderThan(string timestamp)
    {
        using var conn = new SqliteConnection(_connectionString);
        return conn.Execute(
            "DELETE FROM plc_data WHERE Timestamp < @Timestamp",
            new { Timestamp = timestamp });
    }

    /// <summary>
    /// Dapper聚合查询
    /// </summary>
    public List<dynamic> GetStatistics(string deviceId)
    {
        using var conn = new SqliteConnection(_connectionString);
        return conn.Query(@"
            SELECT TagName,
                   COUNT(*) as Count,
                   AVG(Value) as AvgValue,
                   MAX(Value) as MaxValue,
                   MIN(Value) as MinValue
            FROM plc_data
            WHERE DeviceId = @DeviceId
            GROUP BY TagName",
            new { DeviceId = deviceId }).ToList();
    }

    /// <summary>
    /// Dapper多结果集查询
    /// </summary>
    public (List<PlcData> Data, List<AlarmRecord> Alarms) GetDataAndAlarms(
        string deviceId, string startTime)
    {
        using var conn = new SqliteConnection(_connectionString);
        using var multi = conn.QueryMultiple(@"
            SELECT * FROM plc_data WHERE DeviceId = @DeviceId AND Timestamp >= @Time;
            SELECT * FROM alarm_records WHERE DeviceId = @DeviceId AND Timestamp >= @Time",
            new { DeviceId = deviceId, Time = startTime });

        var data = multi.Read<PlcData>().ToList();
        var alarms = multi.Read<AlarmRecord>().ToList();
        return (data, alarms);
    }
}
```

---

## 二、代码示例

### 2.1 上位机场景案例：存储PLC采集的历史数据和报警记录

```csharp
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using Dapper;
using Microsoft.Data.Sqlite;

/// <summary>
/// 上位机数据存储完整案例
/// 场景：上位机定时采集PLC数据，存储到SQLite数据库
/// 功能：数据采集存储、报警检测与记录、历史数据查询、数据清理
/// </summary>
public class PlcDataStorageService : IDisposable
{
    private readonly string _dbPath;
    private readonly string _connString;
    private Timer _collectionTimer;
    private Timer _cleanupTimer;
    private bool _isRunning;

    // ========== 事件 ==========
    public event Action<string> OnLog;
    public event Action<string, string, double, double> OnAlarm;  // 设备,标签,值,阈值

    // ========== 构造函数 ==========
    public PlcDataStorageService(string dbPath = "plc_data.db")
    {
        _dbPath = dbPath;
        _connString = $"Data Source={dbPath}";
    }

    /// <summary>
    /// 初始化：创建数据库和表
    /// </summary>
    public void Initialize()
    {
        using var conn = new SqliteConnection(_connString);
        conn.Open();
        conn.Execute(@"
            CREATE TABLE IF NOT EXISTS plc_data (
                Id INTEGER PRIMARY KEY AUTOINCREMENT,
                DeviceId TEXT NOT NULL,
                TagName TEXT NOT NULL,
                Value REAL NOT NULL,
                Quality INTEGER DEFAULT 1,
                Timestamp TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS alarm_records (
                Id INTEGER PRIMARY KEY AUTOINCREMENT,
                DeviceId TEXT NOT NULL,
                TagName TEXT NOT NULL,
                AlarmLevel INTEGER NOT NULL,
                Message TEXT NOT NULL,
                TriggerValue REAL NOT NULL,
                Threshold REAL NOT NULL,
                Timestamp TEXT NOT NULL,
                AckStatus INTEGER DEFAULT 0
            );
            CREATE INDEX IF NOT EXISTS idx_data_ts ON plc_data(Timestamp);
            CREATE INDEX IF NOT EXISTS idx_data_dev_tag ON plc_data(DeviceId, TagName);
            CREATE INDEX IF NOT EXISTS idx_alarm_ts ON alarm_records(Timestamp);
        ");

        Log("数据库初始化完成");
    }

    /// <summary>
    /// 启动数据采集存储
    /// </summary>
    public void StartCollection(int intervalMs = 1000)
    {
        _isRunning = true;
        _collectionTimer = new Timer(OnCollectionTick, null, 0, intervalMs);
        _cleanupTimer = new Timer(OnCleanupTick, null, 3600000, 3600000);  // 每小时清理
        Log($"数据采集已启动，间隔 {intervalMs}ms");
    }

    public void StopCollection()
    {
        _isRunning = false;
        _collectionTimer?.Dispose();
        _cleanupTimer?.Dispose();
    }

    /// <summary>
    /// 模拟采集数据并存储
    /// 实际项目中这里替换为Modbus/OPC UA等通信读取
    /// </summary>
    private void OnCollectionTick(object state)
    {
        try
        {
            string now = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");

            // 模拟采集数据（实际替换为真实PLC读取）
            var readings = new List<(string Device, string Tag, double Value)>
            {
                ("PLC_01", "Temperature", SimulateValue(25, 5)),
                ("PLC_01", "Pressure", SimulateValue(1.5, 0.3)),
                ("PLC_01", "MotorSpeed", SimulateValue(1500, 200)),
                ("PLC_02", "Temperature", SimulateValue(30, 8)),
                ("PLC_02", "FlowRate", SimulateValue(100, 20)),
            };

            // 批量存储
            BatchStoreData(readings, now);

            // 报警检测
            CheckAlarms(readings, now);
        }
        catch (Exception ex)
        {
            Log($"采集异常: {ex.Message}");
        }
    }

    /// <summary>
    /// 批量存储采集数据
    /// </summary>
    private void BatchStoreData(
        List<(string Device, string Tag, double Value)> readings, string timestamp)
    {
        using var conn = new SqliteConnection(_connString);
        conn.Open();
        using var tx = conn.BeginTransaction();

        try
        {
            foreach (var r in readings)
            {
                conn.Execute(@"
                    INSERT INTO plc_data (DeviceId, TagName, Value, Quality, Timestamp)
                    VALUES (@Device, @Tag, @Value, 1, @Time)",
                    new { Device = r.Device, Tag = r.Tag, Value = r.Value, Time = timestamp },
                    tx);
            }
            tx.Commit();
        }
        catch
        {
            tx.Rollback();
            throw;
        }
    }

    /// <summary>
    /// 报警检测（简化版：固定阈值检查）
    /// </summary>
    private void CheckAlarms(
        List<(string Device, string Tag, double Value)> readings, string timestamp)
    {
        var thresholds = new Dictionary<string, (double High, double Low)>
        {
            { "Temperature", (80, 5) },
            { "Pressure", (3.0, 0.1) },
            { "MotorSpeed", (2000, 0) },
            { "FlowRate", (200, 10) },
        };

        using var conn = new SqliteConnection(_connString);
        foreach (var r in readings)
        {
            if (thresholds.TryGetValue(r.Tag, out var threshold))
            {
                bool isAlarm = r.Value > threshold.High || r.Value < threshold.Low;
                if (isAlarm)
                {
                    conn.Execute(@"
                        INSERT INTO alarm_records
                        (DeviceId, TagName, AlarmLevel, Message, TriggerValue, Threshold, Timestamp)
                        VALUES (@Device, @Tag, 2, @Msg, @Val, @Thresh, @Time)",
                        new
                        {
                            Device = r.Device,
                            Tag = r.Tag,
                            Msg = $"{r.Tag} 超出范围: {r.Value:F2} (范围: {threshold.Low}-{threshold.High})",
                            Val = r.Value,
                            Thresh = r.Value > threshold.High ? threshold.High : threshold.Low,
                            Time = timestamp
                        });

                    OnAlarm?.Invoke(r.Device, r.Tag, r.Value,
                        r.Value > threshold.High ? threshold.High : threshold.Low);
                }
            }
        }
    }

    /// <summary>
    /// 定期清理旧数据（保留30天）
    /// </summary>
    private void OnCleanupTick(object state)
    {
        string cutoff = DateTime.Now.AddDays(-30).ToString("yyyy-MM-dd HH:mm:ss");
        using var conn = new SqliteConnection(_connString);
        int deleted = conn.Execute("DELETE FROM plc_data WHERE Timestamp < @Cutoff",
            new { Cutoff = cutoff });
        Log($"数据清理完成，删除 {deleted} 条旧记录");
    }

    // ========== 查询方法 ==========

    /// <summary>
    /// 查询历史数据（供UI绑定/报表使用）
    /// </summary>
    public List<PlcData> QueryHistory(string deviceId, string tagName,
        string start, string end)
    {
        using var conn = new SqliteConnection(_connString);
        return conn.Query<PlcData>(@"
            SELECT * FROM plc_data
            WHERE DeviceId = @Device AND TagName = @Tag
              AND Timestamp >= @Start AND Timestamp <= @End
            ORDER BY Timestamp", new { Device = deviceId, Tag = tagName, Start = start, End = end }).ToList();
    }

    /// <summary>
    /// 查询未确认的报警列表
    /// </summary>
    public List<AlarmRecord> GetUnacknowledgedAlarms()
    {
        using var conn = new SqliteConnection(_connString);
        return conn.Query<AlarmRecord>(
            "SELECT * FROM alarm_records WHERE AckStatus = 0 ORDER BY Timestamp DESC").ToList();
    }

    // ========== 辅助方法 ==========

    private Random _random = new Random();
    private double _lastTemp = 25, _lastPressure = 1.5, _lastSpeed = 1500, _lastFlow = 100;

    private double SimulateValue(double baseValue, double range)
    {
        // 模拟PLC数据变化（带随机波动）
        return baseValue + (_random.NextDouble() - 0.5) * range;
    }

    private void Log(string msg) => OnLog?.Invoke(msg);

    public void Dispose()
    {
        StopCollection();
    }
}
```

---

## 三、注意事项

1. **连接字符串**：SQLite 只需要一个文件名作为数据源。建议使用绝对路径避免混淆。
2. **批量插入性能**：逐条插入性能极低，务必使用事务批量插入。Dapper 的 `Execute` 配合 `List<T>` 参数可以自动批量执行。
3. **WAL 模式**：建议启用 WAL（Write-Ahead Logging）模式以提高并发性能：
   ```sql
   PRAGMA journal_mode=WAL;
   ```
4. **时间格式**：SQLite 没有原生 DateTime 类型，建议存储为 ISO 8601 字符串（`yyyy-MM-dd HH:mm:ss.fff`）。
5. **数据清理**：长期运行的上位机会产生海量数据，必须定期清理或归档。
6. **连接管理**：每次操作使用 `using` 确保连接及时关闭，不要长期持有一个连接。

---

## 四、练习建议

### 练习1：PLC数据存储系统
- 创建 SQLite 数据库，存储模拟的PLC数据（温度、压力等）
- 每1秒采集一次，批量写入数据库
- 实现30天数据自动清理
- 查询并显示历史数据趋势

### 练习2：报警管理系统
- 设计报警记录表
- 实现多级报警（提示、警告、严重）
- 报警确认功能
- 报警历史查询和统计

### 练习3：Dapper综合应用
- 使用Dapper实现完整的CRUD操作
- 多表关联查询（JOIN）
- 聚合统计（AVG、MAX、MIN、COUNT）
- 分页查询

---

## 五、常见错误

### 错误1：`SQLiteException: no such table`
```
原因：数据库文件存在但表未创建
```
**解决**：在程序启动时执行 `CREATE TABLE IF NOT EXISTS` 创建表。

### 错误2：`SQLiteException: database is locked`
```
原因：另一个进程或线程正在写入数据库，SQLite同一时刻只允许一个写入者
```
**解决**：启用 WAL 模式；使用连接池；确保每次操作及时释放连接。

### 错误3：数据写入性能极低
```
原因：逐条插入且未使用事务
```
**解决**：使用事务批量插入（性能可提升10-100倍）。

### 错误4：`SQLiteException: near "SELECT": syntax error`
```
原因：SQL语法错误，最常见的是字符串未用引号
```
**解决**：使用参数化查询，避免手动拼接SQL。

### 错误5：Dapper映射失败
```
现象：Dapper查询结果中所有字段为默认值
```
**原因**：实体类属性名与数据库列名不匹配。
**解决**：使用 `[Column("db_column_name")]` 特性或使用别名 `SELECT col AS PropName`。
