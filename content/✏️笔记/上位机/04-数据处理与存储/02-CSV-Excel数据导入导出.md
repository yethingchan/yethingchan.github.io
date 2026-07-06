# 02 - CSV / Excel 数据导入导出

---

## 一、知识讲解

### 1.1 CSV 文件读写

CSV（Comma-Separated Values）是最简单的数据交换格式，每行一条记录，字段用逗号分隔。上位机中常用于数据导出、配置文件、与其他系统交换数据。

#### CSV 格式规范

```
设备ID,标签名,值,质量,时间戳
PLC_01,Temperature,25.6,1,2024-01-15 10:30:00
PLC_01,Pressure,1.25,1,2024-01-15 10:30:00
PLC_02,FlowRate,156.8,1,2024-01-15 10:30:00
```

#### 注意事项

- 包含逗号、换行或引号的字段需要用双引号包裹
- 字段内的双引号需要转义为两个双引号
- 手动解析 CSV 容易出错，推荐使用 CsvHelper 库

#### 安装 CsvHelper

```bash
dotnet add package CsvHelper
```

### 1.2 Excel 文件操作

Excel 是工业现场最常用的报表格式。C# 中操作 Excel 有两种主流库：

| 库 | NuGet包 | 特点 | 授权 |
|----|---------|------|------|
| **NPOI** | `NPOI` | 纯C#实现，无需Office，支持xls/xlsx | Apache 2.0（免费） |
| **EPPlus** | `EPPlus` | 简洁API，功能丰富，仅支持xlsx | LGPL（商用需付费）或 Poly戈利亚 |

```bash
# NPOI（推荐，开源免费）
dotnet add package NPOI

# EPPlus（更简洁的API）
dotnet add package EPPlus
```

### 1.3 DataTable 与 CSV 互转

`System.Data.DataTable` 是 C# 中内存表格的标准数据结构，适合作为 CSV、Excel、数据库之间的中间格式。

---

## 二、代码示例

### 2.1 CSV 文件读写（CsvHelper 库）

```csharp
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Text;
using CsvHelper;
using CsvHelper.Configuration;

// ===== 定义CSV对应的实体类 =====
public class PlcDataRecord
{
    public string DeviceId { get; set; }       // 设备ID
    public string TagName { get; set; }         // 标签名
    public double Value { get; set; }           // 数据值
    public int Quality { get; set; }            // 数据质量
    public string Timestamp { get; set; }      // 时间戳
    public string Remark { get; set; }          // 备注
}

// ===== CsvHelper 配置类（可选，用于自定义映射）=====
public class PlcDataRecordMap : ClassMap<PlcDataRecord>
{
    public PlcDataRecordMap()
    {
        Map(m => m.DeviceId).Index(0);     // 第0列
        Map(m => m.TagName).Index(1);       // 第1列
        Map(m => m.Value).Index(2);         // 第2列
        Map(m => m.Quality).Index(3);       // 第3列
        Map(m => m.Timestamp).Index(4);     // 第4列
        Map(m => m.Remark).Index(5).Optional(); // 第5列，可选
    }
}

/// <summary>
/// CSV文件读写工具类（基于CsvHelper）
/// </summary>
public class CsvFileHelper
{
    /// <summary>
    /// 写入CSV文件
    /// 自动处理特殊字符（逗号、引号、换行）
    /// </summary>
    public static void WriteCsv<T>(string filePath, List<T> records, bool hasHeader = true)
    {
        // 配置编码为UTF-8（带BOM，避免Excel打开中文乱码）
        var encoding = new UTF8Encoding(true);

        using var writer = new StreamWriter(filePath, false, encoding);
        using var csv = new CsvWriter(writer, CultureInfo.InvariantCulture);

        if (hasHeader)
        {
            csv.WriteHeader<T>();   // 写入表头
            csv.NextRecord();
        }

        foreach (var record in records)
        {
            csv.WriteRecord(record); // 写入一行数据
            csv.NextRecord();
        }

        Console.WriteLine($"CSV写入成功: {filePath}, 共 {records.Count} 条记录");
    }

    /// <summary>
    /// 读取CSV文件
    /// 自动解析为实体类列表
    /// </summary>
    public static List<T> ReadCsv<T>(string filePath) where T : class
    {
        var encoding = Encoding.UTF8;

        using var reader = new StreamReader(filePath, encoding);
        using var csv = new CsvReader(reader, CultureInfo.InvariantCulture);

        var records = csv.GetRecords<T>();
        var result = new List<T>(records);

        Console.WriteLine($"CSV读取成功: {filePath}, 共 {result.Count} 条记录");
        return result;
    }

    /// <summary>
    /// 追加写入CSV（不覆盖已有内容）
    /// </summary>
    public static void AppendCsv<T>(string filePath, T record)
    {
        var encoding = new UTF8Encoding(true);
        using var writer = new StreamWriter(filePath, true, encoding);  // append=true
        using var csv = new CsvWriter(writer, CultureInfo.InvariantCulture);
        csv.WriteRecord(record);
        csv.NextRecord();
    }
}
```

### 2.2 CSV 手动解析（无第三方库）

```csharp
using System;
using System.Collections.Generic;
using System.IO;

/// <summary>
/// 手动CSV解析（适用于简单场景或无法安装第三方库时）
/// 注意：手动解析无法正确处理所有边缘情况
/// </summary>
public class SimpleCsvParser
{
    /// <summary>
    /// 手动解析CSV文件为字符串二维数组
    /// </summary>
    public static List<string[]> Parse(string filePath, char separator = ',')
    {
        var rows = new List<string[]>();

        using var reader = new StreamReader(filePath);
        string line;
        while ((line = reader.ReadLine()) != null)
        {
            string[] fields = SplitCsvLine(line, separator);
            rows.Add(fields);
        }

        return rows;
    }

    /// <summary>
    /// 手动分割CSV行（处理引号内的逗号）
    /// </summary>
    private static string[] SplitCsvLine(string line, char separator)
    {
        var fields = new List<string>();
        bool inQuotes = false;
        var currentField = new System.Text.StringBuilder();

        for (int i = 0; i < line.Length; i++)
        {
            char c = line[i];

            if (c == '"')
            {
                // 处理转义引号（两个引号 = 一个引号）
                if (inQuotes && i + 1 < line.Length && line[i + 1] == '"')
                {
                    currentField.Append('"');
                    i++;  // 跳过下一个引号
                }
                else
                {
                    inQuotes = !inQuotes;
                }
            }
            else if (c == separator && !inQuotes)
            {
                fields.Add(currentField.ToString());
                currentField.Clear();
            }
            else
            {
                currentField.Append(c);
            }
        }

        fields.Add(currentField.ToString());
        return fields.ToArray();
    }

    /// <summary>
    /// 手动写入CSV
    /// </summary>
    public static void WriteCsvManual(string filePath, List<string[]> data, char separator = ',')
    {
        using var writer = new StreamWriter(filePath, false, System.Text.Encoding.UTF8);
        foreach (var row in data)
        {
            writer.WriteLine(string.Join(separator, row));
        }
    }
}
```

### 2.3 Excel 文件操作（NPOI 库）

```csharp
using System;
using System.Collections.Generic;
using System.IO;
using NPOI.HSSF.UserModel;        // .xls 格式
using NPOI.XSSF.UserModel;        // .xlsx 格式
using NPOI.SS.UserModel;          // 通用接口
using NPOI.HPSF;                  // 文档属性

/// <summary>
/// Excel文件操作工具类（基于NPOI）
/// 支持 .xls（HSSFWorkbook）和 .xlsx（XSSFWorkbook）
/// </summary>
public class ExcelHelper
{
    /// <summary>
    /// 创建Excel文件并写入数据
    /// </summary>
    public static void CreateExcel(
        string filePath,
        string sheetName,
        string[] headers,
        List<object[]> dataRows)
    {
        IWorkbook workbook;

        // 根据扩展名创建不同类型的workbook
        if (Path.GetExtension(filePath).ToLower() == ".xlsx")
            workbook = new XSSFWorkbook();   // .xlsx格式
        else
            workbook = new HSSFWorkbook();   // .xls格式

        // 创建工作表
        ISheet sheet = workbook.CreateSheet(sheetName);

        // 写入表头
        IRow headerRow = sheet.CreateRow(0);
        ICellStyle headerStyle = CreateHeaderStyle(workbook);
        for (int i = 0; i < headers.Length; i++)
        {
            ICell cell = headerRow.CreateCell(i);
            cell.SetCellValue(headers[i]);
            cell.CellStyle = headerStyle;
        }

        // 写入数据行
        for (int rowIdx = 0; rowIdx < dataRows.Count; rowIdx++)
        {
            IRow row = sheet.CreateRow(rowIdx + 1);
            object[] rowData = dataRows[rowIdx];

            for (int colIdx = 0; colIdx < rowData.Length; colIdx++)
            {
                ICell cell = row.CreateCell(colIdx);
                SetCellValue(cell, rowData[colIdx]);
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.Length; i++)
        {
            sheet.AutoSizeColumn(i);
        }

        // 保存文件
        using var fs = new FileStream(filePath, FileMode.Create, FileAccess.Write);
        workbook.Write(fs);

        Console.WriteLine($"Excel创建成功: {filePath}");
    }

    /// <summary>
    /// 读取Excel文件
    /// </summary>
    public static List<string[]> ReadExcel(string filePath, int sheetIndex = 0)
    {
        using var fs = new FileStream(filePath, FileMode.Open, FileAccess.Read);
        IWorkbook workbook;

        if (Path.GetExtension(filePath).ToLower() == ".xlsx")
            workbook = new XSSFWorkbook(fs);
        else
            workbook = new HSSFWorkbook(fs);

        ISheet sheet = workbook.GetSheetAt(sheetIndex);
        var result = new List<string[]>();

        // 遍历所有行
        for (int i = 0; i <= sheet.LastRowNum; i++)
        {
            IRow row = sheet.GetRow(i);
            if (row == null) continue;

            var cells = new List<string>();
            for (int j = 0; j < row.LastCellNum; j++)
            {
                ICell cell = row.GetCell(j);
                cells.Add(cell != null ? GetCellStringValue(cell) : "");
            }
            result.Add(cells.ToArray());
        }

        return result;
    }

    /// <summary>
    /// 设置单元格值（自动根据类型设置）
    /// </summary>
    private static void SetCellValue(ICell cell, object value)
    {
        switch (value)
        {
            case null:
                cell.SetCellValue("");
                break;
            case string str:
                cell.SetCellValue(str);
                break;
            case int num:
                cell.SetCellValue(num);
                break;
            case double dbl:
                cell.SetCellValue(dbl);
                break;
            case bool b:
                cell.SetCellValue(b);
                break;
            case DateTime dt:
                cell.SetCellValue(dt.ToString("yyyy-MM-dd HH:mm:ss"));
                break;
            default:
                cell.SetCellValue(value.ToString());
                break;
        }
    }

    /// <summary>
    /// 获取单元格的字符串值
    /// </summary>
    private static string GetCellStringValue(ICell cell)
    {
        switch (cell.CellType)
        {
            case CellType.String:
                return cell.StringCellValue;
            case CellType.Numeric:
                if (DateUtil.IsCellDateFormatted(cell))
                    return cell.DateCellValue.ToString("yyyy-MM-dd HH:mm:ss");
                return cell.NumericCellValue.ToString();
            case CellType.Boolean:
                return cell.BooleanCellValue.ToString();
            case CellType.Formula:
                return cell.CellFormula;
            default:
                return "";
        }
    }

    /// <summary>
    /// 创建表头样式（加粗、灰色背景）
    /// </summary>
    private static ICellStyle CreateHeaderStyle(IWorkbook workbook)
    {
        ICellStyle style = workbook.CreateCellStyle();
        IFont font = workbook.CreateFont();
        font.Boldweight = (short)NPOI.SS.UserModel.FontBoldWeight.Bold;
        font.FontHeightInPoints = 11;
        style.SetFont(font);

        // 浅灰背景
        style.FillForegroundColor = NPOI.HSSF.Util.HSSFColor.LightGrey.Index;
        style.FillPattern = FillPattern.SolidForeground;

        // 边框
        style.BorderBottom = BorderStyle.Thin;
        style.BorderTop = BorderStyle.Thin;
        style.BorderLeft = BorderStyle.Thin;
        style.BorderRight = BorderStyle.Thin;

        return style;
    }
}
```

### 2.4 DataTable 与 CSV 互转

```csharp
using System;
using System.Data;
using System.IO;
using System.Text;
using CsvHelper;

/// <summary>
/// DataTable 与 CSV 互转工具
/// DataTable 作为中间格式，方便在 CSV、Excel、数据库之间转换
/// </summary>
public class DataTableCsvConverter
{
    /// <summary>
    /// DataTable 写入CSV文件
    /// </summary>
    public static void DataTableToCsv(DataTable dt, string filePath)
    {
        var encoding = new UTF8Encoding(true);  // UTF-8 BOM
        using var writer = new StreamWriter(filePath, false, encoding);
        using var csv = new CsvWriter(writer, System.Globalization.CultureInfo.InvariantCulture);

        // 写入列名
        foreach (DataColumn column in dt.Columns)
        {
            csv.WriteField(column.ColumnName);
        }
        csv.NextRecord();

        // 写入数据行
        foreach (DataRow row in dt.Rows)
        {
            foreach (DataColumn column in dt.Columns)
            {
                csv.WriteField(row[column]);
            }
            csv.NextRecord();
        }
    }

    /// <summary>
    /// CSV文件读取为DataTable
    /// </summary>
    public static DataTable CsvToDataTable(string filePath)
    {
        var dt = new DataTable();
        var encoding = Encoding.UTF8;

        using var reader = new StreamReader(filePath, encoding);
        using var csv = new CsvReader(reader, System.Globalization.CultureInfo.InvariantCulture);

        // 读取表头
        csv.Read();
        csv.ReadHeader();
        foreach (var header in csv.HeaderRecord)
        {
            dt.Columns.Add(header);
        }

        // 读取数据行
        while (csv.Read())
        {
            var row = dt.NewRow();
            foreach (DataColumn column in dt.Columns)
            {
                row[column.ColumnName] = csv.GetField<string>(column.ColumnName);
            }
            dt.Rows.Add(row);
        }

        return dt;
    }

    /// <summary>
    /// 创建示例DataTable（模拟PLC数据）
    /// </summary>
    public static DataTable CreateSampleDataTable()
    {
        var dt = new DataTable("PlcData");
        dt.Columns.Add("DeviceId", typeof(string));
        dt.Columns.Add("TagName", typeof(string));
        dt.Columns.Add("Value", typeof(double));
        dt.Columns.Add("Quality", typeof(int));
        dt.Columns.Add("Timestamp", typeof(string));

        // 添加示例数据
        var rand = new Random();
        for (int i = 0; i < 100; i++)
        {
            dt.Rows.Add(
                $"PLC_0{rand.Next(1, 4)}",
                new[] { "Temperature", "Pressure", "MotorSpeed", "FlowRate" }[rand.Next(4)],
                Math.Round(rand.NextDouble() * 100, 2),
                rand.Next(0, 2),
                DateTime.Now.AddSeconds(-i).ToString("yyyy-MM-dd HH:mm:ss")
            );
        }

        return dt;
    }
}
```

### 2.5 大量数据导出优化

```csharp
using System;
using System.Collections.Generic;
using System.Data;
using System.IO;
using System.Text;
using NPOI.SS.UserModel;
using NPOI.XSSF.UserModel;

/// <summary>
/// 大量数据导出优化策略
/// 上位机长期运行可能积累数十万甚至百万条数据
/// 直接导出可能导致内存溢出或生成超大文件
/// </summary>
public class LargeDataExportHelper
{
    /// <summary>
    /// 优化策略1：分批写入CSV（流式写入，不占大量内存）
    /// 适用于导出百万级数据
    /// </summary>
    public static void ExportLargeCsv(
        string filePath, Func<int, int, DataTable> fetchBatch, int totalRows, int batchSize = 10000)
    {
        var encoding = new UTF8Encoding(true);
        using var writer = new StreamWriter(filePath, false, encoding);

        bool isFirstBatch = true;
        for (int offset = 0; offset < totalRows; offset += batchSize)
        {
            int count = Math.Min(batchSize, totalRows - offset);
            DataTable batch = fetchBatch(offset, count);

            if (isFirstBatch)
            {
                // 写入表头
                var headers = new List<string>();
                foreach (DataColumn col in batch.Columns)
                    headers.Add(col.ColumnName);
                writer.WriteLine(string.Join(",", headers));
                isFirstBatch = false;
            }

            // 写入数据行（不使用逗号拼接，使用StringBuilder）
            var sb = new StringBuilder();
            foreach (DataRow row in batch.Rows)
            {
                for (int i = 0; i < batch.Columns.Count; i++)
                {
                    if (i > 0) sb.Append(",");
                    string val = row[i]?.ToString() ?? "";
                    // 如果值包含逗号或引号，用引号包裹
                    if (val.Contains(",") || val.Contains("\""))
                    {
                        val = "\"" + val.Replace("\"", "\"\"") + "\"";
                    }
                    sb.Append(val);
                }
                writer.WriteLine(sb.ToString());
                sb.Clear();
            }

            Console.WriteLine($"已写入 {Math.Min(offset + batchSize, totalRows)}/{totalRows}");
        }
    }

    /// <summary>
    /// 优化策略2：数据分页查询导出
    /// 不要一次性加载所有数据，使用LIMIT/OFFSET分页
    /// </summary>
    public static DataTable FetchPage(SqliteConnection conn, string query,
        int page, int pageSize)
    {
        string pagedQuery = $"{query} LIMIT {pageSize} OFFSET {page * pageSize}";
        using var cmd = new SqliteCommand(pagedQuery, conn);
        using var reader = cmd.ExecuteReader();
        var dt = new DataTable();
        dt.Load(reader);
        return dt;
    }

    /// <summary>
    /// 优化策略3：时间范围筛选导出
    /// 只导出用户选择的时间范围，而非全量导出
    /// </summary>
    public static string BuildTimeRangeQuery(
        string deviceId, string tagName, string startTime, string endTime)
    {
        return $@"
            SELECT DeviceId, TagName, Value, Timestamp
            FROM plc_data
            WHERE DeviceId = '{deviceId}'
              AND TagName = '{tagName}'
              AND Timestamp >= '{startTime}'
              AND Timestamp <= '{endTime}'
            ORDER BY Timestamp";
    }

    // Microsoft.Data.Sqlite 引用已在上文中
    private class SqliteConnection { }
    private class SqliteCommand { public SqliteCommand(string q, SqliteConnection c) { } public System.Data.IDataReader ExecuteReader() => null; } private class DataTable { public void Load(System.Data.IDataReader r) { } public System.Data.DataColumnCollection Columns => null; } }
}

/// <summary>
/// 修正：正确的分页查询（使用Dapper + SQLite）
/// </summary>
public class CorrectPagedExport
{
    /// <summary>
    /// 使用Dapper分页查询并导出CSV
    /// </summary>
    public static void ExportPagedCsv(
        string dbPath, string outputPath,
        string deviceId, string tagName,
        string startTime, string endTime,
        int pageSize = 10000)
    {
        using var conn = new Microsoft.Data.Sqlite.SqliteConnection($"Data Source={dbPath}");
        var encoding = new UTF8Encoding(true);

        using var writer = new StreamWriter(outputPath, false, encoding);
        writer.WriteLine("DeviceId,TagName,Value,Timestamp,Quality");

        int offset = 0;
        bool hasData = true;

        while (hasData)
        {
            var data = conn.Query<PlcDataRecord>(@"
                SELECT DeviceId, TagName, Value, Timestamp, Quality
                FROM plc_data
                WHERE DeviceId = @Device AND TagName = @Tag
                  AND Timestamp >= @Start AND Timestamp <= @End
                ORDER BY Timestamp
                LIMIT @Size OFFSET @Offset",
                new
                {
                    Device = deviceId,
                    Tag = tagName,
                    Start = startTime,
                    End = endTime,
                    Size = pageSize,
                    Offset = offset
                });

            if (data == null || !data.Any())
            {
                hasData = false;
                break;
            }

            foreach (var record in data)
            {
                writer.WriteLine($"{record.DeviceId},{record.TagName}," +
                    $"{record.Value},{record.Timestamp},{record.Quality}");
            }

            offset += pageSize;
            Console.WriteLine($"已导出 {offset} 条...");
        }
    }
}
```

### 2.6 上位机场景案例：导出采集数据为 Excel 报表

```csharp
using System;
using System.Collections.Generic;
using System.Data;
using System.IO;
using System.Linq;
using Dapper;
using Microsoft.Data.Sqlite;
using NPOI.SS.UserModel;
using NPOI.XSSF.UserModel;

/// <summary>
/// 上位机报表导出完整案例
/// 场景：从SQLite数据库读取PLC历史数据，生成格式化的Excel报表
/// 报表包含：设备数据汇总表、报警记录表、统计信息表
/// </summary>
public class PlcReportExporter
{
    private readonly string _dbPath;

    public PlcReportExporter(string dbPath)
    {
        _dbPath = dbPath;
    }

    /// <summary>
    /// 生成完整报表
    /// </summary>
    public void GenerateReport(string outputPath)
    {
        var workbook = new XSSFWorkbook();  // 创建 .xlsx 工作簿

        // Sheet1：原始数据
        CreateDataSheet(workbook);

        // Sheet2：报警记录
        CreateAlarmSheet(workbook);

        // Sheet3：统计汇总
        CreateSummarySheet(workbook);

        // 保存
        using var fs = new FileStream(outputPath, FileMode.Create, FileAccess.Write);
        workbook.Write(fs);

        Console.WriteLine($"报表已生成: {outputPath}");
    }

    /// <summary>
    /// Sheet1：PLC原始数据表
    /// </summary>
    private void CreateDataSheet(IWorkbook workbook)
    {
        ISheet sheet = workbook.CreateSheet("采集数据");

        // 表头
        var headers = new[] { "序号", "设备ID", "标签名", "数据值", "质量", "时间戳" };
        IRow headerRow = sheet.CreateRow(0);
        ICellStyle headerStyle = CreateHeaderStyle(workbook);

        for (int i = 0; i < headers.Length; i++)
        {
            ICell cell = headerRow.CreateCell(i);
            cell.SetCellValue(headers[i]);
            cell.CellStyle = headerStyle;
        }

        // 从数据库读取数据
        using var conn = new SqliteConnection($"Data Source={_dbPath}");
        var data = conn.Query<PlcDataRecord>(@"
            SELECT * FROM plc_data
            ORDER BY Timestamp DESC
            LIMIT 10000").ToList();

        // 写入数据
        for (int i = 0; i < data.Count; i++)
        {
            IRow row = sheet.CreateRow(i + 1);
            row.CreateCell(0).SetCellValue(i + 1);
            row.CreateCell(1).SetCellValue(data[i].DeviceId);
            row.CreateCell(2).SetCellValue(data[i].TagName);
            row.CreateCell(3).SetCellValue(data[i].Value);
            row.CreateCell(4).SetCellValue(data[i].Quality);
            row.CreateCell(5).SetCellValue(data[i].Timestamp);
        }

        // 自动列宽
        for (int i = 0; i < headers.Length; i++)
            sheet.AutoSizeColumn(i);
    }

    /// <summary>
    /// Sheet2：报警记录表
    /// </summary>
    private void CreateAlarmSheet(IWorkbook workbook)
    {
        ISheet sheet = workbook.CreateSheet("报警记录");
        var headers = new[] { "序号", "设备ID", "标签名", "报警级别", "描述", "触发值", "阈值", "时间", "确认状态" };

        IRow headerRow = sheet.CreateRow(0);
        ICellStyle headerStyle = CreateHeaderStyle(workbook);
        for (int i = 0; i < headers.Length; i++)
        {
            ICell cell = headerRow.CreateCell(i);
            cell.SetCellValue(headers[i]);
            cell.CellStyle = headerStyle;
        }

        // 读取报警数据
        using var conn = new SqliteConnection($"Data Source={_dbPath}");
        var alarms = conn.Query(@"
            SELECT * FROM alarm_records ORDER BY Timestamp DESC LIMIT 1000",
            new { }).ToList();

        for (int i = 0; i < alarms.Count; i++)
        {
            IRow row = sheet.CreateRow(i + 1);
            var alarm = alarms[i];
            row.CreateCell(0).SetCellValue(i + 1);
            row.CreateCell(1).SetCellValue(alarm.DeviceId);
            row.CreateCell(2).SetCellValue(alarm.TagName);
            row.CreateCell(3).SetCellValue(alarm.AlarmLevel);
            row.CreateCell(4).SetCellValue(alarm.Message);
            row.CreateCell(5).SetCellValue(alarm.Value);
            row.CreateCell(6).SetCellValue(alarm.Threshold);
            row.CreateCell(7).SetCellValue(alarm.Timestamp);
            row.CreateCell(8).SetCellValue(alarm.AckStatus == 1 ? "已确认" : "未确认");
        }

        for (int i = 0; i < headers.Length; i++)
            sheet.AutoSizeColumn(i);
    }

    /// <summary>
    /// Sheet3：统计汇总
    /// </summary>
    private void CreateSummarySheet(IWorkbook workbook)
    {
        ISheet sheet = workbook.CreateSheet("统计汇总");

        using var conn = new SqliteConnection($"Data Source={_dbPath}");
        var stats = conn.Query(@"
            SELECT DeviceId, TagName,
                   COUNT(*) as RecordCount,
                   ROUND(AVG(Value), 2) as AvgValue,
                   ROUND(MAX(Value), 2) as MaxValue,
                   ROUND(MIN(Value), 2) as MinValue
            FROM plc_data
            GROUP BY DeviceId, TagName").ToList();

        var headers = new[] { "设备ID", "标签名", "记录数", "平均值", "最大值", "最小值" };
        IRow headerRow = sheet.CreateRow(0);
        ICellStyle headerStyle = CreateHeaderStyle(workbook);
        for (int i = 0; i < headers.Length; i++)
        {
            ICell cell = headerRow.CreateCell(i);
            cell.SetCellValue(headers[i]);
            cell.CellStyle = headerStyle;
        }

        for (int i = 0; i < stats.Count; i++)
        {
            IRow row = sheet.CreateRow(i + 1);
            row.CreateCell(0).SetCellValue(stats[i].DeviceId);
            row.CreateCell(1).SetCellValue(stats[i].TagName);
            row.CreateCell(2).SetCellValue(stats[i].RecordCount);
            row.CreateCell(3).SetCellValue(stats[i].AvgValue);
            row.CreateCell(4).SetCellValue(stats[i].MaxValue);
            row.CreateCell(5).SetCellValue(stats[i].MinValue);
        }

        for (int i = 0; i < headers.Length; i++)
            sheet.AutoSizeColumn(i);
    }

    private ICellStyle CreateHeaderStyle(IWorkbook workbook)
    {
        var style = workbook.CreateCellStyle();
        var font = workbook.CreateFont();
        font.Boldweight = 700;
        style.SetFont(font);
        style.FillForegroundColor = NPOI.HSSF.Util.HSSFColor.LightGrey.Index;
        style.FillPattern = FillPattern.SolidForeground;
        return style;
    }

    // 动态类型，用于Dapper匿名查询
    private class AlarmRecord
    {
        public string DeviceId { get; set; }
        public string TagName { get; set; }
        public int AlarmLevel { get; set; }
        public string Message { get; set; }
        public double? Value { get; set; }
        public double? Threshold { get; set; }
        public string Timestamp { get; set; }
        public int AckStatus { get; set; }
    }

    private class StatRecord
    {
        public string DeviceId { get; set; }
        public string TagName { get; set; }
        public int RecordCount { get; set; }
        public double AvgValue { get; set; }
        public double MaxValue { get; set; }
        public double MinValue { get; set; }
    }
}
```

---

## 三、注意事项

1. **CSV编码**：导出中文CSV时使用 `UTF-8 BOM`（`new UTF8Encoding(true)`），否则用Excel打开会出现乱码。
2. **CSV特殊字符**：CSV中的逗号、引号、换行需要特殊处理，推荐使用CsvHelper库自动处理。
3. **Excel行数限制**：`.xls` 格式最多65536行，`.xlsx` 格式最多1048576行。大量数据推荐导出CSV。
4. **NPOI性能**：NPOI在处理大量数据时内存占用较高。超大数据量建议分批写入或使用SXSSFWorkbook（流式API）。
5. **EPPlus授权**：EPPlus 5.0+ 使用 Poly戈利亚 许可，非商业用途免费，商业使用需要购买License。如果不确定，选NPOI。
6. **文件锁定**：Excel文件打开时会被锁定，其他程序无法同时写入。写入前确保文件没有被Excel打开。

---

## 四、练习建议

### 练习1：CSV数据导入导出工具
- 读取CSV文件并显示在DataGridView中
- 支持修改数据并导出回CSV
- 支持自定义分隔符（逗号、制表符、分号）
- 支持编码选择（UTF-8、GBK）

### 练习2：Excel报表生成器
- 从数据库读取采集数据
- 生成多Sheet的Excel报表
- 包含数据表、报警表、统计表
- 支持选择时间范围导出

### 练习3：大批量数据导出优化
- 测试不同数据量（1万、10万、100万条）的导出速度
- 实现分批写入和进度显示
- 对比CSV和Excel的导出速度和文件大小

---

## 五、常见错误

### 错误1：Excel打开CSV中文乱码
```
原因：CSV使用UTF-8无BOM编码
```
**解决**：导出时使用 `new UTF8Encoding(true)` 写入BOM头。

### 错误2：CSV数据被截断或错位
```
原因：字段中包含逗号但未用引号包裹
```
**解决**：使用CsvHelper库自动处理，或手动检查特殊字符。

### 错误3：NPOI写入Excel行数超过限制
```
现象：超过65536行时.xls格式报错
```
**解决**：使用 `.xlsx` 格式（XSSFWorkbook），最多支持1048576行。

### 错误4：进程占用Excel文件
```
现象：导出Excel后无法删除或移动文件
```
**原因**：文件流未关闭或Excel打开了该文件。
**解决**：确保 `using` 语句正确关闭文件流。

### 错误5：EPPlus License异常
```
现象：EPPlus 5+报License异常
```
**原因**：EPPlus 5+ 需要设置 License 上下文。
**解决**：在程序启动时添加 `ExcelPackage.LicenseContext = LicenseContext.NonCommercial;`（非商业）。
