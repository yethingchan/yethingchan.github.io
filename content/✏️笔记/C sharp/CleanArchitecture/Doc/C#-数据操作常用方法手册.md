## C# 数据操作常用方法手册

> 聚焦 C# 后端开发中与数据打交道的高频操作。
> 涵盖 EF Core、ADO.NET、DataTable、CSV/Excel、数据转换、事务、批量操作等。
> 面向实际业务场景，每个方法都附有可直接运行的代码示例。

---

### 一、EF Core 数据操作（CRUD）

#### 1.1 查询（Read）

| 方法 | 作用 | 使用场景 |
|------|------|----------|
| `FindAsync(key)` | 按主键查找，先查内存缓存再查数据库 | 按 ID 获取单条记录 |
| `FirstOrDefaultAsync()` | 取第一条（无数据返回 null） | 条件查询单条 |
| `SingleAsync()` | 取唯一一条（多条或无数据抛异常） | 确保数据唯一性 |
| `ToListAsync()` | 查询结果加载到 List | 获取列表数据 |
| `CountAsync()` / `AnyAsync()` | 计数 / 判断存在 | 统计和存在性检查 |
| `AsNoTracking()` | 关闭变更追踪 | 只读查询，提升性能 |
| `Include()` / `ThenInclude()` | 加载关联实体 | 一对多/多对多关联查询 |

```csharp
// ===== 按主键查询 =====
var entity = await _context.Products.FindAsync(productId);
// FindAsync 会先查 ChangeTracker 内存缓存，命中则不发 SQL

// ===== 条件查询单条 =====
var product = await _context.Products
    .FirstOrDefaultAsync(p => p.Name == "机械键盘");
// SQL: SELECT TOP(1) * FROM Products WHERE Name = '机械键盘'

// ===== 确认唯一 =====
var config = await _context.Configs
    .SingleAsync(c => c.Key == "MaxRetries");
// 如果配置缺失抛 InvalidOperationException
// 如果重复也抛异常——适合启动时校验配置完整性

// ===== 只读查询（推荐）=====
var products = await _context.Products
    .AsNoTracking()                    // 不追踪变更，查询更快
    .Where(p => p.IsActive)
    .OrderBy(p => p.Name)
    .ToListAsync();
// AsNoTracking 适用于：展示列表、报表、导出等不需要修改数据的场景

// ===== 关联查询 =====
var orders = await _context.Orders
    .AsNoTracking()
    .Include(o => o.Customer)                  // 加载客户信息
        .ThenInclude(c => c.Address)            // 再加载客户的地址
    .Include(o => o.Items)                      // 加载订单项
        .ThenInclude(i => i.Product)            // 再加载每个订单项的产品
    .Where(o => o.Status == OrderStatus.Pending)
    .ToListAsync();

// ===== 分页查询 =====
int page = 3, pageSize = 20;
var pagedData = await _context.Products
    .AsNoTracking()
    .OrderBy(p => p.Id)                        // 分页必须排序
    .Skip((page - 1) * pageSize)
    .Take(pageSize)
    .ToListAsync();

// 同时获取总数（用于分页 UI）
var totalCount = await _context.Products.CountAsync();

// ===== 存在性检查（用 AnyAsync 不用 CountAsync）=====
bool hasPendingOrders = await _context.Orders
    .AnyAsync(o => o.Status == OrderStatus.Pending);
// AnyAsync 生成 EXISTS 查询，找到第一条就返回
// CountAsync > 0 会扫描全部匹配行，性能差

// ===== 投影查询（只查需要的字段）=====
var summaries = await _context.Products
    .AsNoTracking()
    .Select(p => new ProductSummary
    {
        Id = p.Id,
        Name = p.Name,
        Price = p.Price,
        CategoryName = p.Category.Name   // 自动 JOIN
    })
    .ToListAsync();
// SQL 只 SELECT 这四个字段，不加载整行数据

// ===== 动态条件查询 =====
IQueryable<Product> query = _context.Products.AsNoTracking();

if (!string.IsNullOrWhiteSpace(keyword))
    query = query.Where(p => p.Name.Contains(keyword));

if (minPrice.HasValue)
    query = query.Where(p => p.Price >= minPrice.Value);

if (maxPrice.HasValue)
    query = query.Where(p => p.Price <= maxPrice.Value);

if (categoryId.HasValue)
    query = query.Where(p => p.CategoryId == categoryId.Value);

query = query.OrderBy(p => p.Name);
var results = await query.Skip(offset).Take(limit).ToListAsync();
```

**注意细节：**
- `AsNoTracking()` 在只读场景下一定要加。EF Core 默认追踪所有查询出来的实体，用于检测变更，这会消耗额外内存和 CPU
- `Include` 会生成 JOIN 查询，过多 Include 会导致"笛卡尔积爆炸"。如果关联数据量大，考虑**拆分查询**（EF Core 5+）：`.AsSplitQuery()`
- 分页查询必须 `OrderBy`，否则 `Skip`/`Take` 的结果不可预测
- 动态条件用 `IQueryable` 链式拼接，EF Core 最终会生成一条 SQL，不会多次查数据库

#### 1.2 新增（Create）

| 方法 | 作用 | 使用场景 |
|------|------|----------|
| `Add(entity)` | 标记为 Added 状态 | 新增单个实体 |
| `AddRange(entities)` | 批量标记为 Added | 批量新增 |
| `AddAsync(entity)` | 异步新增（仅在有值生成策略时需要） | 主键由数据库生成时 |
| `SaveChangesAsync()` | 执行实际的 INSERT SQL | 提交所有变更 |

```csharp
// ===== 新增单条 =====
var product = new Product
{
    Name = "无线鼠标",
    Price = 129.00m,
    CategoryId = 3
};
_context.Products.Add(product);
await _context.SaveChangesAsync();
// SaveChanges 后 product.Id 自动填充（数据库自增 ID）

// ===== 新增并获取 ID =====
var product = new Product { Name = "键盘", Price = 299m, CategoryId = 1 };
_context.Products.Add(product);
await _context.SaveChangesAsync();
int newId = product.Id;  // EF Core 自动回填自增 ID

// ===== 批量新增 =====
var products = new List<Product>
{
    new() { Name = "鼠标垫", Price = 29.9m, CategoryId = 2 },
    new() { Name = "键盘手托", Price = 49.9m, CategoryId = 2 },
    new() { Name = "显示器支架", Price = 199m, CategoryId = 3 }
};
_context.Products.AddRange(products);
await _context.SaveChangesAsync();
// EF Core 会生成一条 INSERT ... VALUES (...), (...), (...) 批量语句

// ===== 新增主从表（一对多）=====
var order = new Order
{
    CustomerId = 1,
    OrderDate = DateTime.UtcNow,
    Items = new List<OrderItem>       // 同时新增子表
    {
        new() { ProductId = 1, Quantity = 2, UnitPrice = 129m },
        new() { ProductId = 3, Quantity = 1, UnitPrice = 299m }
    }
};
_context.Orders.Add(order);          // 只需 Add 主表，子表自动关联
await _context.SaveChangesAsync();    // 一条事务中完成所有 INSERT
```

**注意细节：**
- `Add` 和 `AddAsync` 的区别：只有在主键需要数据库端生成（如 Guid 由 DB 生成）时才需要 `AddAsync`。自增 ID 用 `Add` 即可
- `AddRange` 比循环调用 `Add` 性能更好，因为 EF Core 可以合并为一条批量 INSERT
- `SaveChangesAsync` 是原子操作——要么全部成功，要么全部回滚。不需要手动开事务

#### 1.3 修改（Update）

| 方法 | 作用 | 使用场景 |
|------|------|----------|
| 直接修改属性 + `SaveChangesAsync` | 通过 ChangeTracker 检测变更 | 查询后修改 |
| `Update(entity)` | 标记整个实体为 Modified | 断开连接的实体（如 API 接收的 DTO） |
| `ExecuteUpdateAsync()` | 直接执行 UPDATE SQL（EF Core 7+） | 批量更新，不加载实体 |
| `Entry(entity).Property(...).IsModified` | 标记特定属性为已修改 | 部分更新 |

```csharp
// ===== 方式1：查询后修改（最常用，推荐）=====
var product = await _context.Products.FindAsync(id);
if (product != null)
{
    product.Price = 99.99m;
    product.Name = "机械键盘（促销价）";
    await _context.SaveChangesAsync();
}
// EF Core 的 ChangeTracker 检测到 Price 和 Name 变了
// 只生成 UPDATE ... SET Price=99.99, Name='...' WHERE Id=@id

// ===== 方式2：不查询直接更新（EF Core 7+，高性能）=====
int affected = await _context.Products
    .Where(p => p.CategoryId == 2)
    .ExecuteUpdateAsync(setters => setters
        .SetProperty(p => p.Price, p => p.Price * 0.9m)   // 打九折
        .SetProperty(p => p.UpdatedAt, DateTime.UtcNow));
// SQL: UPDATE Products SET Price = Price * 0.9, UpdatedAt = GETDATE()
//      WHERE CategoryId = 2
// 不加载实体到内存，直接在数据库端执行，适合批量更新

// ===== 方式3：部分更新（只更新指定字段）=====
var product = new Product { Id = 5, Price = 99.99m };
_context.Products.Attach(product);
_context.Entry(product).Property(p => p.Price).IsModified = true;
await _context.SaveChangesAsync();
// 只 UPDATE Price，不影响其他字段

// ===== 方式4：Update 整个实体（API 接收 DTO 场景）=====
public async Task UpdateProduct(UpdateProductDto dto)
{
    var entity = await _context.Products.FindAsync(dto.Id);
    if (entity == null) throw new NotFoundException("Product", dto.Id);
    
    entity.Name = dto.Name;
    entity.Price = dto.Price;
    entity.Description = dto.Description;
    await _context.SaveChangesAsync();
}
// 先查后改，确保实体存在，且只更新 DTO 中提供的字段
```

**注意细节：**
- 方式 1（查询后修改）会触发审计拦截器（`AuditableEntityInterceptor`），自动更新 `LastModifiedBy` 等字段
- 方式 2（`ExecuteUpdateAsync`）不经过 `SaveChanges`，不会触发拦截器。如果需要审计字段自动填充，不要用这种方式
- 在并发场景下，方式 1 可能出现"最后写入者获胜"的问题。如果需要乐观并发控制，给实体加 `[ConcurrencyCheck]` 或 `[Timestamp]` 字段

#### 1.4 删除（Delete）

| 方法 | 作用 | 使用场景 |
|------|------|----------|
| `Remove(entity)` | 标记为 Deleted 状态 | 删除已查询的实体 |
| `RemoveRange(entities)` | 批量标记为 Deleted | 批量删除 |
| `ExecuteDeleteAsync()` | 直接执行 DELETE SQL（EF Core 7+） | 批量删除，不加载实体 |

```csharp
// ===== 查询后删除 =====
var product = await _context.Products.FindAsync(id);
if (product != null)
{
    _context.Products.Remove(product);
    await _context.SaveChangesAsync();
}

// ===== 不查询直接删除（EF Core 7+）=====
int affected = await _context.Products
    .Where(p => p.CategoryId == 5 && !p.IsActive)
    .ExecuteDeleteAsync();
// SQL: DELETE FROM Products WHERE CategoryId = 5 AND IsActive = 0

// ===== 批量删除 =====
var expiredOrders = await _context.Orders
    .Where(o => o.CreatedAt < DateTime.UtcNow.AddYears(-1))
    .ToListAsync();
_context.Orders.RemoveRange(expiredOrders);
await _context.SaveChangesAsync();

// ===== 软删除（推荐）=====
// 不真正删除，而是标记为已删除
var entity = await _context.Products.FindAsync(id);
if (entity != null)
{
    entity.IsDeleted = true;
    entity.DeletedAt = DateTime.UtcNow;
    await _context.SaveChangesAsync();
}
// 配合全局查询过滤器，后续查询自动排除已删除数据
```

#### 1.5 全局查询过滤器

```csharp
// 在 DbContext 配置中设置（Infrastructure/Data/ApplicationDbContext.cs）
protected override void OnModelCreating(ModelBuilder modelBuilder)
{
    // 所有实现了 ISoftDeletable 的实体自动过滤
    modelBuilder.Entity<Product>().HasQueryFilter(p => !p.IsDeleted);
    modelBuilder.Entity<Order>().HasQueryFilter(o => !o.IsDeleted);
    
    // 多租户过滤器
    modelBuilder.Entity<Product>().HasQueryFilter(p => p.TenantId == _currentTenantId);
}

// 查询时自动生效
var products = await _context.Products.ToListAsync();
// SQL: SELECT * FROM Products WHERE IsDeleted = 0

// 需要包含已删除数据时
var allProducts = await _context.Products
    .IgnoreQueryFilters()
    .ToListAsync();
// SQL: SELECT * FROM Products（不过滤）
```

---

### 二、EF Core 事务管理

```csharp
// ===== 方式1：SaveChanges 自带事务（最常用）=====
// SaveChanges/SaveChangesAsync 内部自动开启事务
// 所有 Add/Update/Remove 操作在同一个事务中提交
_context.Products.Add(product1);
_context.Products.Add(product2);
_context.OrderItems.Remove(item);
await _context.SaveChangesAsync();  // 三条操作要么全成功，要么全回滚

// ===== 方式2：显式事务（跨多个 SaveChanges）=====
await using var transaction = await _context.Database.BeginTransactionAsync();
try
{
    // 第一阶段
    _context.Products.Add(newProduct);
    await _context.SaveChangesAsync();
    
    // 第二阶段（依赖第一阶段的结果）
    newProduct.Stock = initialStock;
    _context.StockRecords.Add(new StockRecord { ProductId = newProduct.Id, ... });
    await _context.SaveChangesAsync();
    
    await transaction.CommitAsync();
}
catch
{
    await transaction.RollbackAsync();
    throw;
}

// ===== 方式3：使用 IExecutionStrategy 处理瞬态故障=====
// SQL Server / PostgreSQL 可能因网络抖动导致临时失败
// EF Core 内置重试策略
builder.Services.AddDbContext<AppDbContext>(options =>
{
    options.UseSqlServer(connectionString, sqlOptions =>
    {
        sqlOptions.EnableRetryOnFailure(
            maxRetryCount: 3,
            maxRetryDelay: TimeSpan.FromSeconds(5),
            errorNumbersToAdd: null);
    });
});
```

---

### 三、ADO.NET 直接操作数据库

当 EF Core 的性能无法满足需求（如超大批量操作、复杂存储过程）时，使用 ADO.NET。

#### 3.1 基本操作

```csharp
using Microsoft.Data.SqlClient;  // SQL Server
// using Npgsql;                  // PostgreSQL
// using Microsoft.Data.Sqlite;   // SQLite

// ===== 连接字符串 =====
var connectionString = "Server=(localdb)\\mssqllocaldb;Database=MyDb;Trusted_Connection=True;";

// ===== 查询 =====
await using var connection = new SqlConnection(connectionString);
await connection.OpenAsync();

await using var command = new SqlCommand(
    "SELECT Id, Name, Price FROM Products WHERE CategoryId = @categoryId",
    connection);
command.Parameters.AddWithValue("@categoryId", categoryId);

await using var reader = await command.ExecuteReaderAsync();
var products = new List<ProductDto>();
while (await reader.ReadAsync())
{
    products.Add(new ProductDto
    {
        Id = reader.GetInt32(0),
        Name = reader.GetString(1),
        Price = reader.GetDecimal(2)
    });
}

// ===== 更安全的读取方式（处理 NULL）=====
while (await reader.ReadAsync())
{
    var name = reader.IsDBNull(1) ? null : reader.GetString(1);
    var price = reader.IsDBNull(2) ? 0m : reader.GetDecimal(2);
}

// ===== 使用列名而非索引（更健壮）=====
while (await reader.ReadAsync())
{
    products.Add(new ProductDto
    {
        Id = (int)reader["Id"],
        Name = reader["Name"] as string ?? "",
        Price = reader["Price"] != DBNull.Value ? (decimal)reader["Price"] : 0m
    });
}

// ===== 插入/更新/删除（非查询）=====
await using var cmd = new SqlCommand(
    "INSERT INTO Products (Name, Price, CategoryId) VALUES (@name, @price, @catId); SELECT SCOPE_IDENTITY();",
    connection);
cmd.Parameters.AddWithValue("@name", "键盘");
cmd.Parameters.AddWithValue("@price", 299.00m);
cmd.Parameters.AddWithValue("@catId", 3);

var newId = Convert.ToInt32(await cmd.ExecuteScalarAsync());

// ===== 调用存储过程 =====
await using var cmd = new SqlCommand("sp_UpdateStock", connection);
cmd.CommandType = CommandType.StoredProcedure;
cmd.Parameters.AddWithValue("@ProductId", productId);
cmd.Parameters.AddWithValue("@Quantity", quantity);
cmd.Parameters.Add("@Result", SqlDbType.Int).Direction = ParameterDirection.Output;

await cmd.ExecuteNonQueryAsync();
int result = (int)cmd.Parameters["@Result"].Value;
```

**注意细节：**
- 参数化查询（`@param`）可以防止 SQL 注入，永远不要用字符串拼接 SQL
- `AddWithValue` 虽然方便但可能导致类型推断问题。对于 `decimal`、`DateTime` 等类型，建议用 `new SqlParameter("@price", SqlDbType.Decimal) { Value = price }`
- `using` 声明确保 `SqlConnection`、`SqlCommand`、`SqlDataReader` 在使用完后自动释放
- `ExecuteScalarAsync` 用于返回单个值（如 COUNT、SUM、SCOPE_IDENTITY），`ExecuteNonQueryAsync` 用于 INSERT/UPDATE/DELETE

#### 3.2 批量操作（高性能）

```csharp
// ===== SqlBulkCopy（SQL Server 超高性能批量插入）=====
// 比 EF Core AddRange 快 10-100 倍
var dataTable = new DataTable();
dataTable.Columns.Add("Name", typeof(string));
dataTable.Columns.Add("Price", typeof(decimal));
dataTable.Columns.Add("CategoryId", typeof(int));

foreach (var p in products)
{
    dataTable.Rows.Add(p.Name, p.Price, p.CategoryId);
}

await using var bulkCopy = new SqlBulkCopy(connection);
bulkCopy.DestinationTableName = "Products";
bulkCopy.ColumnMappings.Add("Name", "Name");
bulkCopy.ColumnMappings.Add("Price", "Price");
bulkCopy.ColumnMappings.Add("CategoryId", "CategoryId");
bulkCopy.BatchSize = 5000;  // 每批插入 5000 条

await bulkCopy.WriteToServerAsync(dataTable);
// 100 万条数据通常几秒内完成

// ===== EF Core 7+ 的批量操作替代 =====
// 如果不想用 ADO.NET，EF Core 7+ 的 ExecuteInsert/Update/Delete 也是高性能选择
await _context.Products
    .Where(p => p.CategoryId == oldCategoryId)
    .ExecuteUpdateAsync(s => s.SetProperty(p => p.CategoryId, newCategoryId));
```

---

### 四、DataTable / DataSet（WinForms 常用）

WinForms 项目中 `DataTable` 和 `DataSet` 仍然广泛使用（绑定 DataGridView 等控件）。

#### 4.1 DataTable 基本操作

```csharp
// ===== 创建 DataTable =====
var dt = new DataTable("Products");

// 添加列
dt.Columns.Add("Id", typeof(int));
dt.Columns.Add("Name", typeof(string));
dt.Columns.Add("Price", typeof(decimal));
dt.Columns.Add("IsActive", typeof(bool));
dt.Columns.Add("CreatedAt", typeof(DateTime));

// 设置列属性
dt.Columns["Id"].AutoIncrement = true;
dt.Columns["Id"].AutoIncrementSeed = 1;
dt.Columns["Id"].AutoIncrementStep = 1;
dt.Columns["Name"].AllowDBNull = false;
dt.Columns["Price"].DefaultValue = 0m;

// 设置主键
dt.PrimaryKey = new[] { dt.Columns["Id"] };

// 添加行
dt.Rows.Add(null, "机械键盘", 299.00m, true, DateTime.Now);  // Id 自增，传 null
dt.Rows.Add(null, "无线鼠标", 129.00m, true, DateTime.Now);

// 遍历行
foreach (DataRow row in dt.Rows)
{
    string name = row["Name"].ToString();
    decimal price = Convert.ToDecimal(row["Price"]);
}

// 筛选
DataRow[] found = dt.Select("Price > 100 AND IsActive = true");
DataRow[] byName = dt.Select("Name LIKE '%键盘%'");
DataRow[] sorted = dt.Select("", "Price DESC");

// 修改行
DataRow row = dt.Select("Id = 1").FirstOrDefault();
if (row != null)
{
    row["Price"] = 199.00m;
}

// 删除行
dt.Rows.Remove(row);        // 直接删除
row.Delete();               // 标记删除（调用 AcceptChanges 后真正删除）
dt.AcceptChanges();          // 提交删除

// 排序 DataView
var dv = new DataView(dt)
{
    Sort = "Price DESC",
    RowFilter = "IsActive = true"
};
dataGridView1.DataSource = dv;  // 绑定到 DataGridView

// ===== DataTable 转 List =====
var list = dt.AsEnumerable()
    .Select(row => new Product
    {
        Id = row.Field<int>("Id"),
        Name = row.Field<string>("Name"),
        Price = row.Field<decimal>("Price")
    })
    .ToList();

// ===== List 转 DataTable =====
var dt = new DataTable();
dt.Columns.Add("Id", typeof(int));
dt.Columns.Add("Name", typeof(string));
foreach (var p in products)
{
    dt.Rows.Add(p.Id, p.Name);
}
```

#### 4.2 SqlDataAdapter 填充 DataTable

```csharp
// ===== 从数据库填充 DataTable =====
var connectionString = "Server=.;Database=MyDb;Trusted_Connection=True;";
var sql = "SELECT Id, Name, Price FROM Products WHERE IsActive = 1";

var dt = new DataTable();
using var adapter = new SqlDataAdapter(sql, connectionString);
adapter.Fill(dt);

// 绑定到 DataGridView
dataGridView1.DataSource = dt;
dataGridView1.AutoGenerateColumns = true;

// ===== 带参数的查询 =====
var cmd = new SqlCommand(
    "SELECT * FROM Products WHERE CategoryId = @catId AND Price >= @minPrice",
    new SqlConnection(connectionString));
cmd.Parameters.AddWithValue("@catId", categoryId);
cmd.Parameters.AddWithValue("@minPrice", minPrice);

var adapter = new SqlDataAdapter(cmd);
var dt = new DataTable();
adapter.Fill(dt);

// ===== 使用 SqlDataAdapter 更新数据库 =====
// 配合 SqlCommandBuilder 自动生成 INSERT/UPDATE/DELETE
using var connection = new SqlConnection(connectionString);
using var adapter = new SqlDataAdapter("SELECT * FROM Products", connection);
using var builder = new SqlCommandBuilder(adapter);

var dt = new DataTable();
adapter.Fill(dt);

// 修改 DataTable...
dt.Rows[0]["Price"] = 99.99m;
dt.Rows.Add(null, "新产品", 59.99m, true, DateTime.Now);

// 提交变更到数据库
adapter.Update(dt);
```

#### 4.3 DataGridView 常用操作

```csharp
// ===== 绑定数据 =====
dataGridView1.DataSource = dataTable;

// ===== 配置列 =====
dataGridView1.Columns["Id"].Visible = false;              // 隐藏 ID 列
dataGridView1.Columns["Name"].HeaderText = "产品名称";     // 设置列标题
dataGridView1.Columns["Price"].DefaultCellStyle.Format = "N2";  // 小数点后两位
dataGridView1.Columns["Price"].DefaultCellStyle.Alignment = 
    DataGridViewContentAlignment.MiddleRight;              // 右对齐

// ===== 获取选中行 =====
if (dataGridView1.CurrentRow != null)
{
    var row = (DataRowView)dataGridView1.CurrentRow.DataBoundItem;
    int id = Convert.ToInt32(row["Id"]);
    string name = row["Name"].ToString();
}

// ===== 批量获取选中行 =====
var selectedIds = dataGridView1.SelectedRows
    .Cast<DataGridViewRow>()
    .Select(r => Convert.ToInt32(r.Cells["Id"].Value))
    .ToList();

// ===== 单元格事件 =====
private void dataGridView1_CellValueChanged(object sender, DataGridViewCellEventArgs e)
{
    if (e.ColumnIndex == dataGridView1.Columns["Price"].Index)
    {
        var newValue = dataGridView1.Rows[e.RowIndex].Cells["Price"].Value;
        // 处理价格变更...
    }
}

// ===== 行号显示 =====
private void dataGridView1_RowPostPaint(object sender, DataGridViewRowPostPaintEventArgs e)
{
    dataGridView1.Rows[e.RowIndex].HeaderCell.Value = (e.RowIndex + 1).ToString();
}
```

---

### 五、CSV 数据操作

#### 5.1 读取 CSV

```csharp
// ===== 简单读取（手动解析）=====
var lines = await File.ReadAllLinesAsync("data.csv", Encoding.UTF8);
var headers = lines[0].Split(',');

var records = new List<Dictionary<string, string>>();
for (int i = 1; i < lines.Length; i++)
{
    if (string.IsNullOrWhiteSpace(lines[i])) continue;
    
    var values = lines[i].Split(',');
    var record = new Dictionary<string, string>();
    for (int j = 0; j < headers.Length; j++)
    {
        record[headers[j].Trim()] = j < values.Length ? values[j].Trim() : "";
    }
    records.Add(record);
}

// ===== 使用 CsvHelper 库（推荐，处理引号和转义）=====
// dotnet add package CsvHelper
using CsvHelper;
using CsvHelper.Configuration;
using System.Globalization;

// 读取为强类型对象
using var reader = new StreamReader("data.csv", Encoding.UTF8);
using var csv = new CsvReader(reader, CultureInfo.InvariantCulture);

// 配置
csv.Configuration.HasHeaderRecord = true;
csv.Configuration.Delimiter = ",";
csv.Configuration.TrimOptions = TrimOptions.Trim;
csv.Configuration.MissingFieldFound = null;  // 忽略缺失字段

var records = await csv.GetRecordsAsync<ProductCsvModel>().ToListAsync();

// 自定义映射（列名和属性名不一致时）
public sealed class ProductCsvMap : ClassMap<ProductCsvModel>
{
    public ProductCsvMap()
    {
        Map(m => m.Name).Name("产品名称");
        Map(m => m.Price).Name("价格");
        Map(m => m.Quantity).Name("数量");
    }
}

csv.Context.RegisterClassMap<ProductCsvMap>();

// ===== 处理中文 GBK 编码的 CSV（Excel 导出的常见编码）=====
using var reader = new StreamReader("data.csv", Encoding.GetEncoding("GBK"));
using var csv = new CsvReader(reader, CultureInfo.InvariantCulture);
var records = csv.GetRecords<ProductCsvModel>().ToList();
```

#### 5.2 写入 CSV

```csharp
// ===== 简单写入 =====
var sb = new StringBuilder();
sb.AppendLine("Name,Price,CategoryId");
foreach (var p in products)
{
    // 处理字段中包含逗号或换行的情况
    var name = p.Name.Contains(',') ? $"\"{p.Name}\"" : p.Name;
    sb.AppendLine($"{name},{p.Price},{p.CategoryId}");
}
await File.WriteAllTextAsync("output.csv", sb.ToString(), Encoding.UTF8);

// ===== 使用 CsvHelper 写入（推荐）=====
using var writer = new StreamWriter("output.csv", false, new UTF8Encoding(true));  // 带 BOM
using var csv = new CsvWriter(writer, CultureInfo.InvariantCulture);

// 写入表头
csv.WriteField("产品名称");
csv.WriteField("价格");
csv.WriteField("分类");
await csv.NextRecordAsync();

// 写入数据
foreach (var p in products)
{
    csv.WriteField(p.Name);
    csv.WriteField(p.Price);
    csv.WriteField(p.CategoryName);
    await csv.NextRecordAsync();
}

// ===== 写入 UTF-8 BOM（Excel 打开不乱码）=====
using var writer = new StreamWriter("output.csv", false, new UTF8Encoding(true));
// new UTF8Encoding(true) 表示写入 BOM（Byte Order Mark）
// Excel 识别 BOM 后才会用 UTF-8 解码，否则会用 GBK 导致中文乱码
```

**注意细节：**
- Excel 导出的 CSV 经常使用 GBK 编码，读取时要用 `Encoding.GetEncoding("GBK")`。如果报错说找不到 GBK，需要注册编码提供程序：`Encoding.RegisterProvider(CodePagesEncodingProvider.Instance);`
- 写入 CSV 给 Excel 打开时，必须用 `new UTF8Encoding(true)` 写 BOM，否则中文乱码
- CSV 字段中如果包含逗号、换行、引号，必须用双引号包裹。CsvHelper 库会自动处理这些边界情况

---

### 六、Excel 数据操作

```csharp
// ===== 使用 EPPlus（推荐，免费用于非商业用途）=====
// dotnet add package EPPlus

using OfficeOpenXml;

ExcelPackage.LicenseContext = LicenseContext.NonCommercial;

// 读取 Excel
using var package = new ExcelPackage(new FileInfo("data.xlsx"));
var sheet = package.Workbook.Worksheets[0];  // 第一个工作表

int rowCount = sheet.Dimension.Rows;      // 总行数
int colCount = sheet.Dimension.Columns;   // 总列数

for (int row = 2; row <= rowCount; row++)  // 从第2行开始（第1行是表头）
{
    var name = sheet.Cells[row, 1].Value?.ToString();
    var price = Convert.ToDecimal(sheet.Cells[row, 2].Value ?? 0);
    var date = sheet.Cells[row, 3].GetValue<DateTime>();
}

// 写入 Excel
using var package = new ExcelPackage();
var sheet = package.Workbook.Worksheets.Add("产品列表");

// 写表头
sheet.Cells[1, 1].Value = "产品名称";
sheet.Cells[1, 2].Value = "价格";
sheet.Cells[1, 3].Value = "库存";
sheet.Cells[1, 1].Style.Font.Bold = true;

// 写数据
for (int i = 0; i < products.Count; i++)
{
    sheet.Cells[i + 2, 1].Value = products[i].Name;
    sheet.Cells[i + 2, 2].Value = products[i].Price;
    sheet.Cells[i + 2, 3].Value = products[i].Stock;
}

// 格式化
sheet.Cells[sheet.Dimension.Address].AutoFitColumns();  // 自适应列宽
sheet.Column(2).Style.Numberformat.Format = "#,##0.00"; // 价格格式
sheet.Column(3).Style.Numberformat.Format = "#,##0";    // 整数格式

// 保存
await package.SaveAsAsync(new FileInfo("output.xlsx"));

// ===== 使用 MiniExcel（高性能，适合大数据量）=====
// dotnet add package MiniExcel
using MiniExcelLibs;

// 读取
var rows = await connection.QueryAsync("SELECT * FROM Products");
await MiniExcel.SaveAsAsync("output.xlsx", rows);

// 写入（100万行级别的数据）
var dataTable = GetLargeDataTable();  // DataTable
await MiniExcel.SaveAsAsync("output.xlsx", dataTable);

// 读取为 IEnumerable（流式读取，不一次性加载到内存）
var stream = File.OpenRead("large.xlsx");
foreach (var row in stream.Query(true))  // true = 第一行是表头
{
    var name = row["Name"];
    var price = row["Price"];
}
```

---

### 七、JSON 数据处理

```csharp
using System.Text.Json;
using System.Text.Json.Nodes;

// ===== 对象 → JSON 字符串 =====
var config = new PlcConfig { Ip = "192.168.1.100", Port = 502, Timeout = 3000 };
string json = JsonSerializer.Serialize(config, new JsonSerializerOptions
{
    WriteIndented = true,                              // 美化输出
    PropertyNamingPolicy = JsonNamingPolicy.CamelCase, // 驼峰命名
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
});

// ===== JSON 字符串 → 对象 =====
var parsed = JsonSerializer.Deserialize<PlcConfig>(json, new JsonSerializerOptions
{
    PropertyNameCaseInsensitive = true   // 不区分大小写
});

// ===== 异步读写 JSON 文件 =====
// 写入
await using var stream = File.Create("config.json");
await JsonSerializer.SerializeAsync(stream, config, new JsonSerializerOptions { WriteIndented = true });

// 读取
await using var stream = File.OpenRead("config.json");
var config = await JsonSerializer.DeserializeAsync<PlcConfig>(stream);

// ===== 动态操作 JSON（不需要定义类）=====
// 使用 JsonNode（.NET 6+）
var node = JsonNode.Parse(jsonString);
string name = (string)node["name"];
int age = (int)node["age"];
string city = (string)node["address"]["city"];  // 嵌套访问

// 修改 JSON
node["name"] = "新名称";
node["tags"] = new JsonArray("tag1", "tag2");

// 序列化回字符串
string modified = node.ToJsonString(new JsonSerializerOptions { WriteIndented = true });

// ===== 处理 JSON 数组 =====
string jsonArray = """[{"name":"Alice"},{"name":"Bob"},{"name":"Charlie"}]""";
var people = JsonSerializer.Deserialize<List<Person>>(jsonArray);

// ===== JsonNode 遍历数组 =====
var array = JsonNode.Parse(jsonArray).AsArray();
foreach (var item in array)
{
    string name = (string)item["name"];
}

// ===== 实际场景：解析 PLC 返回的 JSON 数据 =====
string plcResponse = """
{
    "device": "PLC-001",
    "registers": [
        { "address": "D100", "value": 1234.5 },
        { "address": "D101", "value": 6789.0 }
    ],
    "timestamp": "2026-06-20T14:30:00+08:00"
}
""";

var doc = JsonDocument.Parse(plcResponse);
var root = doc.RootElement;

string device = root.GetProperty("device").GetString();
var timestamp = root.GetProperty("timestamp").GetDateTimeOffset();

foreach (var reg in root.GetProperty("registers").EnumerateArray())
{
    string address = reg.GetProperty("address").GetString();
    double value = reg.GetProperty("value").GetDouble();
    Console.WriteLine($"{address} = {value}");
}
```

---

### 八、数据转换与映射

#### 8.1 类型安全转换

```csharp
// ===== Convert 类 =====
Convert.ToInt32("42");           // 42
Convert.ToInt32(3.7);            // 4（四舍五入）
Convert.ToInt32(null);           // 0（不抛异常！）
Convert.ToDouble("3.14");        // 3.14
Convert.ToDecimal("99.99");      // 99.99m
Convert.ToBoolean(1);              // true
Convert.ToBoolean("True");         // true
Convert.ToDateTime("2026-06-20"); // DateTime
Convert.ToBase64String(bytes);   // 字节数组 → Base64 字符串
Convert.FromBase64String(str);   // Base64 字符串 → 字节数组

// ===== Parse 系列（字符串专用）=====
int.Parse("42");                 // 42（非数字抛 FormatException）
int.TryParse("abc", out int n);  // false，n = 0
decimal.Parse("99.99");          // 99.99m
decimal.TryParse("99,99", NumberStyles.AllowThousands, 
    CultureInfo.InvariantCulture, out decimal price);  // 处理千分位
double.Parse("3.14", CultureInfo.InvariantCulture);    // 确保用点号做小数点
DateTime.Parse("2026-06-20");
DateTime.TryParseExact("20260620", "yyyyMMdd", 
    CultureInfo.InvariantCulture, DateTimeStyles.None, out var date);

// ===== 枚举转换 =====
Enum.Parse<DayOfWeek>("Monday");            // DayOfWeek.Monday
Enum.TryParse<PriorityLevel>("High", out var priority);  // 安全转换
Enum.GetNames<PriorityLevel>();              // ["None","Low","Medium","High"]
Enum.GetValues<PriorityLevel>();             // [0, 1, 2, 3]
(PriorityLevel)2;                            // PriorityLevel.Medium（int → 枚举）
(int)PriorityLevel.High;                     // 3（枚举 → int）

// ===== 字节数组转换 =====
BitConverter.GetBytes(12345);               // int → byte[]
BitConverter.ToInt32(bytes, 0);             // byte[] → int（从偏移量 0 开始）
BitConverter.ToDouble(bytes, 0);            // byte[] → double

// PLC 通讯中常用：高低字节序转换
byte[] data = { 0x12, 0x34, 0x56, 0x78 };
if (BitConverter.IsLittleEndian)
    Array.Reverse(data);                    // 大端序转小端序
int value = BitConverter.ToInt32(data, 0);
```

#### 8.2 AutoMapper 对象映射

```csharp
// ===== 配置映射 =====
// Application/Common/Mappings/MappingConfigurations.cs
public class MappingConfigurations : IProfileExpression
{
    public void Configure()
    {
        CreateMap<Product, ProductDto>();
        CreateMap<Product, ProductListDto>()
            .ForMember(d => d.CategoryName, opt => opt.MapFrom(s => s.Category.Name));
        CreateMap<CreateProductCommand, Product>();
        CreateMap<UpdateProductCommand, Product>()
            .ForAllMembers(opt => opt.Condition((src, dest, srcMember) => srcMember != null));
    }
}

// ===== 使用映射 =====
// 实体 → DTO
var dto = _mapper.Map<ProductDto>(entity);

// 实体列表 → DTO 列表
var dtos = _mapper.Map<List<ProductDto>>(entities);

// Command → 实体
var entity = _mapper.Map<Product>(command);

// 更新已有实体（只映射非 null 的属性）
_mapper.Map(updateCommand, existingEntity);

// ===== 手动映射（不依赖 AutoMapper）=====
// 很多团队倾向于手动映射，因为更清晰、更易调试
var dto = new ProductDto
{
    Id = entity.Id,
    Name = entity.Name,
    Price = entity.Price,
    CategoryName = entity.Category?.Name ?? ""
};
```

---

### 九、数据校验

```csharp
// ===== FluentValidation（推荐）=====
public class ProductValidator : AbstractValidator<Product>
{
    public ProductValidator()
    {
        RuleFor(p => p.Name)
            .NotEmpty().WithMessage("产品名称不能为空")
            .MaximumLength(100).WithMessage("产品名称不能超过100个字符");

        RuleFor(p => p.Price)
            .GreaterThan(0).WithMessage("价格必须大于0")
            .LessThanOrEqualTo(999999).WithMessage("价格不能超过999999");

        RuleFor(p => p.Email)
            .EmailAddress().WithMessage("邮箱格式不正确")
            .When(p => !string.IsNullOrEmpty(p.Email));  // 条件验证

        RuleFor(p => p.StartDate)
            .LessThan(p => p.EndDate)
            .WithMessage("开始日期必须早于结束日期");

        // 异步验证（如数据库唯一性检查）
        RuleFor(p => p.Code)
            .MustAsync(BeUniqueCode)
            .WithMessage("产品编码已存在");
    }

    private async Task<bool> BeUniqueCode(string code, CancellationToken ct)
    {
        return !await _context.Products.AnyAsync(p => p.Code == code, ct);
    }
}

// 执行验证
var validator = new ProductValidator();
var result = validator.Validate(product);
if (!result.IsValid)
{
    foreach (var error in result.Errors)
    {
        Console.WriteLine($"{error.PropertyName}: {error.ErrorMessage}");
    }
}

// ===== DataAnnotations（简单场景）=====
public class Product
{
    [Required(ErrorMessage = "名称不能为空")]
    [StringLength(100, ErrorMessage = "名称不能超过{1}个字符")]
    public string Name { get; set; }

    [Range(0.01, 999999, ErrorMessage = "价格必须在0.01到999999之间")]
    public decimal Price { get; set; }

    [RegularExpression(@"^\d{6}$", ErrorMessage = "编码必须是6位数字")]
    public string Code { get; set; }
}
```

---

### 十、常用数据结构与选择

| 场景 | 推荐结构 | 不推荐 | 原因 |
|------|----------|--------|------|
| 按索引频繁访问 | `List<T>` / `T[]` | `LinkedList<T>` | 数组/List 是连续内存，O(1) 索引访问 |
| 频繁头部插入/删除 | `LinkedList<T>` | `List<T>` | List 头部插入需要移动所有元素 |
| 键值对查找 | `Dictionary<K,V>` | `List<KeyValuePair>` | Dictionary O(1) 查找 vs List O(n) |
| 线程安全键值对 | `ConcurrentDictionary<K,V>` | `Dictionary` + lock | 内置细粒度锁 |
| 先进先出 | `Queue<T>` | `List<T>` | Queue 语义清晰 |
| 后进先出 | `Stack<T>` | `List<T>` | Stack 语义清晰 |
| 有序集合（去重） | `SortedSet<T>` | `List<T>` + Distinct | 自动排序去重 |
| 优先级队列 | `PriorityQueue<T,P>`（.NET 6+） | 手写堆 | 内置实现 |
| 大量布尔判断 | `HashSet<T>` | `List<T>.Contains` | HashSet O(1) vs List O(n) |
| 不可变集合 | `ImmutableList<T>` | `ReadOnlyCollection<T>` | 线程安全，修改返回新实例 |

```csharp
// ===== HashSet：快速去重和判断 =====
var validCodes = new HashSet<string>(StringComparer.OrdinalIgnoreCase)
{
    "ABC", "DEF", "GHI"
};
validCodes.Contains("abc");  // true（忽略大小写）
validCodes.Add("JKL");       // 添加，返回 true
validCodes.Add("ABC");       // 已存在，返回 false

// ===== ConcurrentDictionary：多线程缓存 =====
private readonly ConcurrentDictionary<string, DeviceStatus> _cache = new();

public DeviceStatus GetOrFetchStatus(string deviceId)
{
    return _cache.GetOrAdd(deviceId, id => FetchStatusFromPlc(id));
}

// ===== Queue：消息队列 =====
var messageQueue = new Queue<string>();
messageQueue.Enqueue("消息1");
messageQueue.Enqueue("消息2");
var first = messageQueue.Dequeue();  // "消息1"（FIFO）

// ===== PriorityQueue：任务调度（.NET 6+）=====
var tasks = new PriorityQueue<string, int>();
tasks.Enqueue("低优先级任务", 3);
tasks.Enqueue("高优先级任务", 1);
tasks.Enqueue("中优先级任务", 2);
var next = tasks.Dequeue();  // "高优先级任务"（数字越小优先级越高）
```

---

### 十一、性能优化清单

| 操作 | 慢的方式 | 快的方式 | 性能差距 |
|------|----------|----------|----------|
| 字符串拼接（循环内） | `str += "..."` | `StringBuilder` | 100x+ |
| 判断集合非空 | `list.Count() > 0` | `list.Any()` | 10x+（LINQ 场景） |
| 只读查询 | `_context.X.ToList()` | `_context.X.AsNoTracking().ToList()` | 2-5x 内存节省 |
| 批量插入 | 循环 `Add` + 每次 `SaveChanges` | `AddRange` + 一次 `SaveChanges` | 100x+ |
| 大批量插入 | EF Core `AddRange` | `SqlBulkCopy` | 10-100x |
| 批量更新/删除 | 先查后改 | `ExecuteUpdateAsync` / `ExecuteDeleteAsync` | 10x+ |
| 分页计数 | `Count()` + `Skip/Take` 两次查询 | 一次查询用 `COUNT(*) OVER()` | 减少一次往返 |
| 读取大文件 | `File.ReadAllLines()` | `File.ReadLines()`（延迟） | 内存：O(n) → O(1) |
| JSON 序列化 | `Newtonsoft.Json` | `System.Text.Json` | 2-3x |
| 查找元素 | `List<T>.Contains` | `HashSet<T>.Contains` | O(n) → O(1) |

```csharp
// ===== StringBuilder 高性能拼接 =====
var sb = new StringBuilder();
for (int i = 0; i < 100000; i++)
{
    sb.Append($"Row {i}: value={i * 2}\n");
}
string result = sb.ToString();

// ===== 异步读取大文件 =====
await using var stream = File.OpenRead("large-file.csv");
using var reader = new StreamReader(stream);
string line;
int lineCount = 0;
while ((line = await reader.ReadLineAsync()) != null)
{
    lineCount++;
    // 逐行处理，不占用大量内存
}

// ===== EF Core 批量操作性能对比 =====
// 慢：10万条，每条 SaveChanges 一次 → 10万次数据库往返
foreach (var item in items) {
    _context.Products.Add(item);
    await _context.SaveChangesAsync();  // 极慢！
}

// 快：10万条，一次 SaveChanges → 一次批量 INSERT
_context.Products.AddRange(items);
await _context.SaveChangesAsync();

// 更快：SqlBulkCopy → 直接走 BCP 协议
await using var bulkCopy = new SqlBulkCopy(connection);
bulkCopy.DestinationTableName = "Products";
await bulkCopy.WriteToServerAsync(dataTable);
```
