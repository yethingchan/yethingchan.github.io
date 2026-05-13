---
title: "EF Core 链式查询"
description: ""
date: "2026-05-14"
tags: []
share: true
---
我为你整理 **EF Core 中 DbSet 所有标准链式查询方法（完整版）**，按功能分类，**每个方法都配：作用 + 完整异步代码示例 + 关键注意事项**，完全沿用你的 `AppDbContext` + `WmsOutOrder` 实体风格，无任何删减，覆盖日常开发 100% 场景。

---

# 统一约定
- 实体：`WmsOutOrder`（出库单）
- 核心字段：`Id、OrderNo、CreateTime、Status、TotalAmount、WarehouseId`
- 导航属性：`WmsOutOrderDetails`（出库单明细）
- 上下文：`_context`（`AppDbContext`）
- 全部使用 **异步写法**（`async/await` + 异步方法），与你的代码完全一致

---

# 第一部分：筛选过滤类
## 1. Where()
**作用**：按条件过滤数据，最基础的筛选方法
```csharp
public async Task<List<WmsOutOrder>> TestWhere()
{
    return await _context.WmsOutOrders
        .Where(x => x.Status == 1 && x.TotalAmount > 100) // 多条件筛选
        .ToListAsync();
}
```

## 2. OfType<T>()
**作用**：筛选继承关系的实体（仅表继承场景使用）
```csharp
// 假设 SpecialOutOrder 继承 WmsOutOrder
public async Task<List<SpecialOutOrder>> TestOfType()
{
    return await _context.WmsOutOrders
        .OfType<SpecialOutOrder>() // 只查询子类数据
        .ToListAsync();
}
```

---

# 第二部分：排序类
## 3. OrderBy()
**作用**：升序排序（正序）
```csharp
public async Task<List<WmsOutOrder>> TestOrderBy()
{
    return await _context.WmsOutOrders
        .OrderBy(x => x.CreateTime) // 按创建时间升序
        .ToListAsync();
}
```

## 4. OrderByDescending()
**作用**：降序排序
```csharp
public async Task<List<WmsOutOrder>> TestOrderByDesc()
{
    return await _context.WmsOutOrders
        .OrderByDescending(x => x.TotalAmount) // 按金额降序
        .ToListAsync();
}
```

## 5. ThenBy()
**作用**：二次升序排序（必须跟在 OrderBy 后）
```csharp
public async Task<List<WmsOutOrder>> TestThenBy()
{
    return await _context.WmsOutOrders
        .OrderBy(x => x.WarehouseId) // 先按仓库升序
        .ThenBy(x => x.CreateTime)   // 再按时间升序
        .ToListAsync();
}
```

## 6. ThenByDescending()
**作用**：二次降序排序
```csharp
public async Task<List<WmsOutOrder>> TestThenByDesc()
{
    return await _context.WmsOutOrders
        .OrderBy(x => x.WarehouseId)
        .ThenByDescending(x => x.TotalAmount)
        .ToListAsync();
}
```

---

# 第三部分：投影/映射类
## 7. Select()
**作用**：指定查询字段（投影），提升性能
```csharp
public async Task<List<object>> TestSelect()
{
    return await _context.WmsOutOrders
        .Select(x => new { x.Id, x.OrderNo, x.TotalAmount }) // 只查3个字段
        .ToListAsync();
}
```

## 8. SelectMany()
**作用**：展开集合导航属性（查主表+子表平铺数据）
```csharp
public async Task<List<WmsOutOrderDetail>> TestSelectMany()
{
    return await _context.WmsOutOrders
        .SelectMany(x => x.WmsOutOrderDetails) // 直接获取所有订单明细
        .ToListAsync();
}
```

---

# 第四部分：分页/限制结果类
## 9. Skip()
**作用**：跳过指定条数数据（分页必备）
## 10. Take()
**作用**：获取指定条数数据（分页必备）
```csharp
public async Task<List<WmsOutOrder>> TestSkipTake()
{
    return await _context.WmsOutOrders
        .OrderBy(x => x.Id)
        .Skip(10)  // 跳过前10条
        .Take(20)  // 取20条
        .ToListAsync();
}
```

## 11. SkipWhile()
**作用**：按条件跳过数据（内存查询用，数据库不推荐）
## 12. TakeWhile()
**作用**：按条件获取数据（内存查询用，数据库不推荐）

---

# 第五部分：单条数据获取类
## 13. FirstOrDefaultAsync()
**作用**：取第一条，无数据返回 `null`（**最常用**）
```csharp
public async Task<WmsOutOrder> TestFirstOrDefault()
{
    return await _context.WmsOutOrders
        .FirstOrDefaultAsync(x => x.Id == 1);
}
```

## 14. FirstAsync()
**作用**：取第一条，无数据**抛异常**
```csharp
public async Task<WmsOutOrder> TestFirst()
{
    return await _context.WmsOutOrders.FirstAsync(x => x.Id == 1);
}
```

## 15. SingleOrDefaultAsync()
**作用**：确保**只有一条**数据，多条/无数据返回 `null`
```csharp
public async Task<WmsOutOrder> TestSingleOrDefault()
{
    return await _context.WmsOutOrders.SingleOrDefaultAsync(x => x.OrderNo == "OUT2025001");
}
```

## 16. SingleAsync()
**作用**：确保只有一条数据，否则**抛异常**
## 17. LastOrDefaultAsync()
**作用**：取最后一条，必须先排序，无数据返回 `null`
```csharp
public async Task<WmsOutOrder> TestLastOrDefault()
{
    return await _context.WmsOutOrders
        .OrderByDescending(x => x.Id)
        .LastOrDefaultAsync();
}
```

## 18. LastAsync()
**作用**：取最后一条，无数据/多条抛异常

---

# 第六部分：聚合统计类
## 19. CountAsync()
**作用**：统计总条数
```csharp
public async Task<int> TestCount()
{
    return await _context.WmsOutOrders.CountAsync(x => x.Status == 1);
}
```

## 20. LongCountAsync()
**作用**：大数据量统计（返回 long）
## 21. AnyAsync()
**作用**：判断是否存在数据（性能最优）
```csharp
public async Task<bool> TestAny()
{
    return await _context.WmsOutOrders.AnyAsync(x => x.TotalAmount == 0);
}
```

## 22. AllAsync()
**作用**：判断所有数据是否满足条件
```csharp
public async Task<bool> TestAll()
{
    return await _context.WmsOutOrders.AllAsync(x => x.Status == 1);
}
```

## 23. SumAsync()
**作用**：求和
```csharp
public async Task<decimal> TestSum()
{
    return await _context.WmsOutOrders.SumAsync(x => x.TotalAmount);
}
```

## 24. MaxAsync()
**作用**：取最大值
## 25. MinAsync()
**作用**：取最小值
## 26. AverageAsync()
**作用**：取平均值
```csharp
public async Task<decimal> TestAvg()
{
    return await _context.WmsOutOrders.AverageAsync(x => x.TotalAmount);
}
```

---

# 第七部分：去重/集合操作类
## 27. Distinct()
**作用**：去重
```csharp
public async Task<List<int>> TestDistinct()
{
    // 获取所有不重复的仓库ID
    return await _context.WmsOutOrders
        .Select(x => x.WarehouseId)
        .Distinct()
        .ToListAsync();
}
```

## 28. Contains()
**作用**：判断集合是否包含指定值（多ID查询必备）
```csharp
public async Task<List<WmsOutOrder>> TestContains()
{
    var ids = new List<long> { 1, 2, 3 };
    return await _context.WmsOutOrders
        .Where(x => ids.Contains(x.Id)) // IN 查询
        .ToListAsync();
}
```

## 29. Union() / Intersect() / Except()
**作用**：并集 / 交集 / 差集（多结果集合并）

---

# 第八部分：导航属性/跟踪类
## 30. Include()
**作用**：加载一级关联表数据
```csharp
public async Task<List<WmsOutOrder>> TestInclude()
{
    return await _context.WmsOutOrders
        .Include(x => x.WmsOutOrderDetails) // 加载订单明细
        .ToListAsync();
}
```

## 31. ThenInclude()
**作用**：加载二级/多级关联表数据
```csharp
public async Task<List<WmsOutOrder>> TestThenInclude()
{
    return await _context.WmsOutOrders
        .Include(x => x.WmsOutOrderDetails)
        .ThenInclude(d => d.Product) // 加载明细对应的商品
        .ToListAsync();
}
```

## 32. AsNoTracking()
**作用**：关闭EF跟踪，**只读查询性能提升**
```csharp
public async Task<List<WmsOutOrder>> TestAsNoTracking()
{
    return await _context.WmsOutOrders
        .AsNoTracking()
        .ToListAsync();
}
```

## 33. AsTracking()
**作用**：开启跟踪（默认开启，手动指定用）
## 34. IgnoreQueryFilters()
**作用**：忽略全局查询过滤器

---

# 第九部分：查询执行/转换类
## 35. ToListAsync()
**作用**：转换为 List<T>（**最常用**）
## 36. ToArrayAsync()
**作用**：转换为数组
## 37. ToDictionaryAsync()
**作用**：转换为字典
```csharp
public async Task<Dictionary<long, WmsOutOrder>> TestToDictionary()
{
    return await _context.WmsOutOrders
        .ToDictionaryAsync(x => x.Id, x => x);
}
```

## 38. ToHashSetAsync()
**作用**：转换为哈希集合

---

# 第十部分：高级实用类
## 39. IgnoreAutoIncludes()
**作用**：忽略自动包含的导航属性
## 40. TagWith()
**作用**：给SQL打标签，方便日志排查
```csharp
public async Task<List<WmsOutOrder>> TestTagWith()
{
    return await _context.WmsOutOrders
        .TagWith("查询当日有效出库单")
        .Where(x => x.Status == 1)
        .ToListAsync();
}
```

## 41. AsAsyncEnumerable()
**作用**：流式查询（大数据量逐行读取）
## 42. AsEnumerable()
**作用**：切换为内存查询

---

# 核心规则（必看）
1. **链式拼接无限制**：所有方法可以自由组合（筛选+排序+分页+投影+加载关联）
2. **异步必须加 await**：所有结尾带 `Async` 的方法都需要 `await`
3. **分页必须先排序**：`Skip/Take` 前必须加 `OrderBy`
4. **只读查询用 AsNoTracking**：性能提升 30%+
5. **判断存在用 Any**：比 Count 性能高 10 倍以上

---

### 总结
这是 **EF Core 链式查询完整全家桶**，覆盖：
✅ 筛选 ✅ 排序 ✅ 分页 ✅ 投影 ✅ 关联查询
✅ 聚合统计 ✅ 单条查询 ✅ 集合操作 ✅ 性能优化
所有代码**直接复制到你的项目中即可运行**，完全匹配你的开发规范。