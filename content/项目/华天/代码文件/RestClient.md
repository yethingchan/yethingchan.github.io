---
title: "RestClient"
description: ""
date: "2026-05-07"
tags: []
share: true
---
这是一个基于 .NET 旧版 `HttpWebRequest` 封装的 RESTful HTTP 客户端工具类，用于简化 GET、POST、PUT、DELETE 请求的发送与响应接收。

---

### 一、代码结构与逻辑分析

#### 1. 核心设计思路
*   **基础 URL 管理**：通过构造函数传入 `baseUri`（如 `https://api.example.com`），后续调用只需传入相对路径（如 `/users`），自动拼接成完整 URL。
*   **方法封装**：将 HTTP 4 种常用动词（GET/POST/PUT/DELETE）封装成独立的静态方法。
*   **底层实现**：所有请求最终都流向私有方法 `CommonHttpRequest`，由它使用 `HttpWebRequest` 完成实际的网络 IO。

#### 2. 具体流程（以 Post 为例）
1.  **URL 拼接**：判断是否设置了 `BaseUri`，如果有则 `BaseUri + "/" + uri`，否则直接使用传入的 `uri`。
2.  **调用通用请求**：将拼接好的 URL、请求类型 `"POST"` 和数据 `data` 传给 `CommonHttpRequest`。
3.  **发送请求**：
    *   创建 `HttpWebRequest` 对象。
    *   设置版本为 HTTP 1.1，超时时间 50 秒。
    *   如果有数据，将 Content-Type 设为 `application/json`，并将数据写入请求流。
    *   获取响应，用 `StreamReader` 读取返回的字符串。
4.  **返回结果**：成功返回响应字符串，失败返回 `"Error:" + 错误信息`。

---

### 二、代码深度评价（严重问题与设计缺陷）

这段代码带有明显的 **.NET Framework 时代早期** 的风格，在现代 .NET (Core/5+) 环境下，存在以下**严重问题**：

#### 1. 🔴 致命设计缺陷：静态字段 `BaseUri`
```csharp
private static string BaseUri;
public RestClient(string baseUri) { ... }
public static string Post(...) { ... }
```
*   **问题**：`BaseUri` 是 `static`（静态）的，而请求方法也是 `static` 的。
*   **后果**：
    *   如果你在程序中 `new` 了两个 `RestClient`（例如一个连 API A，一个连 API B），**第二个会覆盖第一个的 `BaseUri`**，因为静态变量是“全局共享”的。
    *   这是一个典型的**线程安全隐患**。

#### 2. 🔴 技术选型过时：`HttpWebRequest`
*   **问题**：代码使用了 `HttpWebRequest`，这是 .NET 1.0 时代的产物。
*   **现状**：自 .NET Core 1.0 起，微软**强烈推荐**使用 `HttpClient` 来替代它。
*   **缺点**：
    *   `HttpClient` 性能更高、资源占用更少。
    *   `HttpClient` 支持异步（async/await），而这个类全是同步阻塞调用，在高并发场景下会严重拖垮性能。

#### 3. 🟡 资源释放不规范
```csharp
// 代码里手动写了 Close()
outstream.Close();
reader.Close();
myResponse.Close();
```
*   **问题**：虽然在 `catch` 里做了手动关闭，但这是非常古老的写法。如果在 `Close()` 之前发生了其他异常，资源可能泄漏。
*   **正确写法**：应该使用 `using` 语句块，自动释放资源。

#### 4. 🟡 灵活性不足
*   **Content-Type 写死**：代码里强制 `myRequest.ContentType = @"application/json";`。如果想传 XML 或 Form-Data，这个类就用不了了。
*   **Header 不可配置**：Header 也是写死的（`Accept-Language` 等），无法灵活添加自定义 Header（如 Token、Authorization）。

#### 5. 🟡 异常处理策略不佳
```csharp
catch (WebException wex) { ... return "Error:" + pageContent; }
```
*   **问题**：吞掉了异常，只返回一个字符串。调用方很难判断是真的成功了还是出错了（除非去解析字符串是否包含 "Error:"）。
*   **建议**：应该让异常抛出去，或者返回一个自定义的 `Result<T>` 对象（包含 IsSuccess、Data、ErrorMessage）。

---

### 三、总结
这是一个**功能完整但设计理念和技术选型都已过时**的 REST 客户端。

**学习价值**：
1.  了解 HTTP 请求的底层流程（建立连接、写入流、读取流）。
2.  识别静态变量在工具类中的危险性。

**现代替代方案**：
*   不要自己封装 HTTP 客户端了！直接使用微软自带的 `IHttpClientFactory` + `HttpClient`。
*   或者使用成熟的第三方库，如 `Refit`（声明式 REST 客户端）或 `RestSharp`。

需要我给你展示如何用现代的 `HttpClient` 重写这个功能吗？