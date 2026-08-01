# Java IO 与 NIO 完整知识库

> 目标：系统掌握 Java 输入输出体系——传统 BIO（字节流/字符流）、缓冲与转换、对象序列化，以及 NIO 的 Channel/Buffer/Selector 三大核心与 NIO.2 文件 API，达到能处理文件、网络、高并发 IO 的扎实水平。

---

## 目录

1. [IO 模型概述](#一io-模型概述)
2. [File 类](#二file-类)
3. [字节流 InputStream / OutputStream](#三字节流-inputstream--outputstream)
4. [字符流 Reader / Writer](#四字符流-reader--writer)
5. [缓冲流（Buffering）](#五缓冲流buffering)
6. [转换流（字节↔字符）](#六转换流字节字符)
7. [数据流与对象流（序列化）](#七数据流与对象流序列化)
8. [随机访问 RandomAccessFile](#八随机访问-randomaccessfile)
9. [字符编码](#九字符编码)
10. [标准 IO 重定向](#十标准-io-重定向)
11. [NIO 核心：Channel / Buffer / Selector](#十一nio-核心channel--buffer--selector)
12. [Buffer 详解](#十二buffer-详解)
13. [Selector 多路复用](#十三selector-多路复用)
14. [NIO.2：Path / Paths / Files](#十四nio2path--paths--files)
15. [网络 IO（Socket）](#十五网络-iosocket)
16. [BIO vs NIO vs AIO 对比](#十六bio-vs-nio-vs-aio-对比)
17. [最佳实践清单](#十七最佳实践清单)
18. [常见面试题精解](#十八常见面试题精解)

---

## 一、IO 模型概述

### 1.1 Java IO 演进

| 版本 | 体系 | 特点 |
|------|------|------|
| JDK 1.0 | BIO（java.io） | 流式、阻塞、面向流 |
| JDK 1.4 | NIO（java.nio） | 通道+缓冲、非阻塞、选择器 |
| JDK 7 | NIO.2（java.nio.file） | Paths/Files/Path、异步文件通道 |

### 1.2 字节流 vs 字符流

| 维度 | 字节流 | 字符流 |
|------|--------|--------|
| 单位 | 8 位字节（byte） | 16 位字符（char） |
| 处理对象 | 所有二进制数据（图片/视频/压缩包） | 文本 |
| 是否编码 | 否（原样搬运） | 是（按字符集编解码） |
| 基类 | `InputStream`/`OutputStream` | `Reader`/`Writer` |

> **铁律**：文本用字符流，二进制用字节流。用字节流读文本易乱码，用字符流读图片会损坏文件。

### 1.3 流分类图

```
字节流：
  InputStream
    ├── FileInputStream
    ├── ByteArrayInputStream
    ├── BufferedInputStream
    ├── ObjectInputStream
    └── DataInputStream
  OutputStream（对称）
字符流：
  Reader
    ├── InputStreamReader（桥：字节→字符）
    ├── FileReader
    ├── BufferedReader
    └── StringReader
  Writer（对称）
```

---

## 二、File 类

`File` 表示文件/目录的**路径抽象**，不保证文件存在，只操作元数据。

```java
File f = new File("D:/test/a.txt");
System.out.println(f.exists());       // 是否存在
System.out.println(f.isFile());       // 是否文件
System.out.println(f.isDirectory());  // 是否目录
System.out.println(f.length());       // 字节大小
System.out.println(f.getName());      // 文件名
System.out.println(f.getAbsolutePath());

f.mkdir();              // 建单级目录
f.mkdirs();             // 建多级目录
f.createNewFile();      // 建文件
f.delete();             // 删文件/空目录
File[] files = f.listFiles(); // 列出子项
```

> ⚠️ `File` 不能读写内容，读写靠流。`File` 很多方法已过时，建议用 NIO.2 的 `Files`。

---

## 三、字节流 InputStream / OutputStream

### 3.1 文件字节流读

```java
// 方式1：一次读一个字节（慢，仅演示）
try (FileInputStream in = new FileInputStream("a.txt")) {
    int b;
    while ((b = in.read()) != -1) {
        System.out.print((char) b);
    }
} // try-with-resources 自动关闭

// 方式2：缓冲数组批量读（推荐）
try (FileInputStream in = new FileInputStream("a.txt")) {
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) != -1) {
        // 注意用 len，避免最后一次读不满数组
        System.out.write(buf, 0, len);
    }
}
```

### 3.2 文件字节流写 + 拷贝

```java
try (FileInputStream in = new FileInputStream("src.jpg");
     FileOutputStream out = new FileOutputStream("dst.jpg")) {
    byte[] buf = new byte[8192];
    int len;
    while ((len = in.read(buf)) != -1) {
        out.write(buf, 0, len); // 二进制拷贝，无损
    }
}
```

> **务必使用 try-with-resources**（JDK7+），即使发生异常也能自动关闭流，避免资源泄漏。

---

## 四、字符流 Reader / Writer

### 4.1 FileReader / FileWriter（便捷类，默认编码）

```java
try (FileWriter w = new FileWriter("a.txt", true)) { // true 表示追加
    w.write("你好");
    w.write("Java");   // 可写 char / String / int
    w.append("!");     // Writer 实现 Appendable，可 append
}
```

### 4.2 BufferedReader 按行读

```java
try (BufferedReader br = new BufferedReader(new FileReader("a.txt"))) {
    String line;
    while ((line = br.readLine()) != null) {  // 不含换行符
        System.out.println(line);
    }
}
```

### 4.3 BufferedWriter

```java
try (BufferedWriter bw = new BufferedWriter(new FileWriter("a.txt"))) {
    bw.write("第一行");
    bw.newLine();       // 平台无关换行
    bw.write("第二行");
}
```

> 读文本优先 `BufferedReader.readLine()`，简单高效。

---

## 五、缓冲流（Buffering）

缓冲流包装节点流，减少物理 IO 次数，大幅提升性能。

| 类 | 包装 |
|----|------|
| `BufferedInputStream` | `InputStream` |
| `BufferedOutputStream` | `OutputStream` |
| `BufferedReader` | `Reader` |
| `BufferedWriter` | `Writer` |

```java
// 装饰器模式：BufferedInputStream 套 FileInputStream
try (BufferedInputStream bis = new BufferedInputStream(
        new FileInputStream("a.mp4"), 16 * 1024)) { // 指定缓冲 16KB
    byte[] buf = new byte[8192];
    int len;
    while ((len = bis.read(buf)) != -1) { /* ... */ }
}
```

> 缓冲流是**装饰器模式**经典应用：在不改变原有流接口的前提下增强功能。`flush()` 用于强制把缓冲区写出（close 会自动 flush）。

---

## 六、转换流（字节↔字符）

`InputStreamReader` / `OutputStreamWriter` 是字节流与字符流之间的**桥梁**，关键是可指定字符集。

```java
// 指定 UTF-8 读（避免平台默认编码乱码）
try (BufferedReader br = new BufferedReader(
        new InputStreamReader(new FileInputStream("a.txt"), StandardCharsets.UTF_8))) {
    String line;
    while ((line = br.readLine()) != null) System.out.println(line);
}

// 指定 UTF-8 写
try (BufferedWriter bw = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream("b.txt"), StandardCharsets.UTF_8))) {
    bw.write("中文");
}
```

> `FileReader`/`FileWriter` 无法指定编码（用平台默认），处理中文文本务必用转换流 + 显式 `StandardCharsets.UTF_8`。

---

## 七、数据流与对象流（序列化）

### 7.1 DataInputStream / DataOutputStream

按基本类型读写，保证跨平台字节序一致。

```java
try (DataOutputStream dos = new DataOutputStream(new FileOutputStream("d.dat"))) {
    dos.writeInt(100);
    dos.writeDouble(3.14);
    dos.writeUTF("hello"); // 写入 UTF 字符串（带长度前缀）
}
try (DataInputStream dis = new DataInputStream(new FileInputStream("d.dat"))) {
    int i = dis.readInt();
    double d = dis.readDouble();
    String s = dis.readUTF();
}
```

### 7.2 对象序列化 ObjectInputStream / ObjectOutputStream

对象需实现 `Serializable`（标记接口，无方法）。

```java
class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private transient int temp; // transient 不序列化
    // 构造/getter/setter
}

// 序列化
try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("u.dat"))) {
    oos.writeObject(new User("Tom", 99));
}
// 反序列化
try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("u.dat"))) {
    User u = (User) ois.readObject();
}
```

### 7.3 序列化要点

- `Serializable` 是标记接口，仅起声明作用。
- `serialVersionUID`：版本号，反序列化时校验。**强烈建议显式声明**，否则类结构变化后 UID 自动生成不一致会抛 `InvalidClassException`。
- `transient`：修饰的字段不参与序列化（如密码、敏感信息）。
- `static` 字段不属于对象状态，不序列化。
- 父类未实现 `Serializable` 时，父类字段需有无参构造并手动处理。
- 敏感对象建议用 JSON（如 Jackson）而非 Java 原生序列化，避免安全与兼容问题。

---

## 八、随机访问 RandomAccessFile

支持在文件任意位置读写的「指针式」访问，可实现断点续传、分段写入。

```java
// "rw" 读写，"r" 只读
try (RandomAccessFile raf = new RandomAccessFile("a.txt", "rw")) {
    raf.seek(10);              // 移动指针到第10字节
    raf.write("XYZ".getBytes());
    raf.seek(0);
    int b = raf.read();
    long pos = raf.getFilePointer(); // 当前指针位置
    long len = raf.length();         // 文件长度
}
```

> 断点续传思路：记录已下载字节数 `pos`，`seek(pos)` 后继续 `read`/`write`。

---

## 九、字符编码

| 编码 | 说明 |
|------|------|
| ASCII | 单字节，英文 |
| ISO-8859-1 | 单字节，西欧（Latin-1） |
| GBK | 双字节，中文 |
| UTF-8 | 变长（1~4字节），国际通用 **首选** |
| UTF-16 | 双字节，Java 内部 char 使用 |

```java
String s = "中文";
byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
String back = new String(utf8, StandardCharsets.UTF_8);
```

> **乱码根因**：编码与解码使用的字符集不一致。始终显式指定 `StandardCharsets.UTF_8`，不要用 `getBytes()` 无参（依赖平台默认）。

---

## 十、标准 IO 重定向

```java
System.setIn(new FileInputStream("in.txt"));     // 重定向标准输入
System.setOut(new PrintStream("out.txt"));        // 重定向标准输出
System.setErr(new PrintStream("err.txt"));        // 重定向错误输出
```

> 常用于命令行程序批量测试或日志重定向。

---

## 十一、NIO 核心：Channel / Buffer / Selector

NIO（New IO / Non-blocking IO）面向**块**，通过通道和缓冲区操作数据，支持非阻塞与多路复用。

### 11.1 三大核心组件

| 组件 | 作用 |
|------|------|
| `Channel` | 双向通道（可读可写），类似流但更强大 |
| `Buffer` | 数据容器（数组包装），读写都经过它 |
| `Selector` | 多路复用器，一个线程监听多个 Channel 事件 |

### 11.2 传统 IO vs NIO

- 流是单向（InputStream/OutputStream），Channel 双向。
- 流是字节逐个流动，Buffer 是块批量处理。
- NIO 支持非阻塞，BIO 只能阻塞。

---

## 十二、Buffer 详解

### 12.1 核心属性

| 属性 | 含义 |
|------|------|
| `capacity` | 容量，创建后固定 |
| `limit` | 限制，第一个不可读/写的位置 |
| `position` | 当前位置，下一个读/写索引 |
| `mark` | 标记，可 `reset()` 回到此处 |

### 12.2 读写切换（flip 关键）

```
写模式：position 随写入前移，limit = capacity
flip()：切换读模式 → limit=position，position=0
读模式：position 随读取前移，直到 limit
clear()：清空复位（position=0, limit=capacity），数据未真正删除
```

### 12.3 Buffer 使用示例

```java
// 1. 写入
ByteBuffer buf = ByteBuffer.allocate(1024); // 堆内缓冲
buf.put("Hello NIO".getBytes(StandardCharsets.UTF_8));

// 2. 切换为读模式
buf.flip();

// 3. 读取
while (buf.hasRemaining()) {
    System.out.print((char) buf.get());
}

// 4. 复位重写
buf.clear();
```

### 12.4 直接缓冲区

```java
ByteBuffer direct = ByteBuffer.allocateDirect(1024); // 堆外内存
```

> 直接缓冲区（allocateDirect）在堆外，减少一次内核↔用户态拷贝，适合大文件/长生命周期；创建成本更高，由 GC 之外管理，需谨慎。

### 12.5 文件拷贝（NIO 方式）

```java
try (FileChannel in = FileChannel.open(Paths.get("src.jpg"), StandardOpenOption.READ);
     FileChannel out = FileChannel.open(Paths.get("dst.jpg"),
         StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
    in.transferTo(0, in.size(), out); // 零拷贝式传输，高效
}
```

---

## 十三、Selector 多路复用

单线程管理多个 Channel，监听就绪事件，是高并发网络编程基石。

```java
Selector selector = Selector.open();
ServerSocketChannel ssc = ServerSocketChannel.open();
ssc.configureBlocking(false);             // 必须非阻塞
ssc.socket().bind(new InetSocketAddress(8080));
ssc.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();                    // 阻塞直到有事件
    Set<SelectionKey> keys = selector.selectedKeys();
    Iterator<SelectionKey> it = keys.iterator();
    while (it.hasNext()) {
        SelectionKey key = it.next();
        if (key.isAcceptable()) {
            SocketChannel sc = ssc.accept();
            sc.configureBlocking(false);
            sc.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer buf = ByteBuffer.allocate(1024);
            sc.read(buf);
            // 处理数据...
        }
        it.remove();                      // 必须移除已处理 key
    }
}
```

> 事件类型：`OP_ACCEPT`（接受连接）、`OP_CONNECT`（连接完成）、`OP_READ`（可读）、`OP_WRITE`（可写）。Selector 是 Netty 等框架的底层原理。

---

## 十四、NIO.2：Path / Paths / Files

JDK 7 引入，提供更现代、更全面的文件 API，全面优于 `File` 类。

### 14.1 Path 与 Paths

```java
Path path = Paths.get("D:/test/a.txt");
Path parent = path.getParent();
Path fileName = path.getFileName();
Path resolved = path.resolve("sub/b.txt"); // 拼接路径
```

### 14.2 Files 工具类常用方法

```java
Path src = Paths.get("a.txt");
Path dst = Paths.get("b.txt");

Files.exists(src);
Files.size(src);
Files.readAllBytes(src);                       // 小文件一次性读
Files.write(dst, "内容".getBytes(StandardCharsets.UTF_8));
Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
Files.move(src, dst);
Files.delete(src);                             // 不存在抛异常
Files.deleteIfExists(src);
Files.createDirectories(Paths.get("a/b/c"));

// 按行读（最简洁）
List<String> lines = Files.readAllLines(src, StandardCharsets.UTF_8);
Files.lines(src).forEach(System.out::println); // 返回 Stream，可惰性处理大文件

// 遍历目录
Files.walk(Paths.get("d:/")).forEach(System.out::println);
Files.list(Paths.get("d:/")).forEach(System.out::println);
```

### 14.3 递归遍历 + 过滤

```java
Files.walk(Paths.get("d:/project"))
     .filter(p -> p.toString().endsWith(".java"))
     .forEach(System.out::println);
```

> `Files.walk` 返回 `Stream<Path>`，可结合流式操作，处理大量文件时推荐 `forEach` 而非 `readAllLines` 避免 OOM。

---

## 十五、网络 IO（Socket）

### 15.1 BIO 阻塞式 Socket

```java
// 服务端
try (ServerSocket server = new ServerSocket(8080)) {
    Socket socket = server.accept(); // 阻塞等待连接
    try (BufferedReader br = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
         PrintWriter pw = new PrintWriter(socket.getOutputStream(), true)) {
        String msg = br.readLine();
        pw.println("echo: " + msg);
    }
}

// 客户端
try (Socket socket = new Socket("localhost", 8080);
     PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
     BufferedReader br = new BufferedReader(
            new InputStreamReader(socket.getInputStream()))) {
    pw.println("hello");
    System.out.println(br.readLine());
}
```

### 15.2 NIO 非阻塞式（见 13 节 Selector）

服务端用 `ServerSocketChannel` + `Selector` 实现单线程处理多连接，避免每连接一线程。

---

## 十六、BIO vs NIO vs AIO 对比

| 维度 | BIO（同步阻塞） | NIO（同步非阻塞） | AIO（异步非阻塞） |
|------|----------------|-------------------|-------------------|
| 包 | java.io | java.nio | java.nio.channels（AsynchronousChannel） |
| 阻塞 | 读写阻塞 | 多路复用非阻塞 | 回调通知完成 |
| 线程模型 | 一连接一线程 | 少量线程管多连接 | 回调线程 |
| 适用 | 连接少、简单 | 高并发、长连接 | 高并发、长耗时IO |
| 复杂度 | 低 | 高 | 中 |

> AIO（NIO.2 异步通道）在 Linux 上底层仍是 epoll 模拟，实际中 Netty（基于 NIO）更主流。

---

## 十七、最佳实践清单

1. ✅ 始终用 try-with-resources 关闭流，杜绝资源泄漏。
2. ✅ 文本用字符流，二进制用字节流。
3. ✅ 读写显式指定 `StandardCharsets.UTF_8`，避免平台默认编码乱码。
4. ✅ 大文件/性能场景用缓冲流（BufferedXxx）。
5. ✅ 文件拷贝用 NIO `transferTo` 或缓冲字节流，避免逐字节。
6. ✅ 现代文件操作优先 `Files` / `Path`，胜过 `File`。
7. ✅ 序列化显式声明 `serialVersionUID`，敏感字段加 `transient`。
8. ✅ 大文件按行/按块处理，避免 `readAllLines`/`readAllBytes` 撑爆内存。
9. ✅ 需要非阻塞高并发用 NIO + Selector（或 Netty）。
10. ✅ 按行读取文本用 `BufferedReader.readLine()`。

---

## 十八、常见面试题精解

### Q1：字节流和字符流的区别？
答：字节流以 byte 为单位，处理所有二进制数据；字符流以 char 为单位，按字符集编解码，处理文本。文本用字符流，二进制用字节流。

### Q2：什么是 try-with-resources？
答：JDK7 特性，实现 AutoCloseable 的资源写在 try(...) 中，无论是否异常都会自动调用 close()，替代繁琐的 finally。强烈推荐。

### Q3：为什么读文本要用转换流指定编码？
答：FileReader 用平台默认编码，跨平台易乱码。InputStreamReader + 显式 StandardCharsets.UTF_8 才能保证编码一致。

### Q4：什么是缓冲流？为什么快？
答：缓冲流（BufferedXxx）包装节点流，内部维护缓冲区，减少底层物理 IO 次数（批量读写），显著提升性能。装饰器模式实现。

### Q5：Java 序列化是什么？serialVersionUID 作用？
答：将对象转为字节序列以便存储/传输。serialVersionUID 是版本标识，反序列化时校验；不显式声明则随类结构变化自动生成，易导致 InvalidClassException。

### Q6：transient 关键字作用？
答：标记字段不参与序列化（如密码、临时数据）。反序列化时该字段取默认值。

### Q7：BIO、NIO、AIO 区别？
答：见第十六节。BIO 同步阻塞（一连接一线程）；NIO 同步非阻塞（Selector 多路复用）；AIO 异步非阻塞（回调）。

### Q8：NIO 三大核心组件？
答：Channel（双向通道）、Buffer（数据容器）、Selector（多路复用器）。数据的读写都经过 Buffer。

### Q9：Buffer 的 flip() 有什么用？
答：写完后调用 flip() 将 Buffer 从写模式切换为读模式（limit=position，position=0），之后才能正确读取刚写入的数据。

### Q10：NIO.2 的 Files 相比 File 有什么优势？
答：Files 提供原子操作、批量读写、按行读、Stream 遍历目录、更清晰的异常处理；File 已过时。现代代码推荐 Paths/Files。

### Q11：RandomAccessFile 应用场景？
答：支持指针随机读写，适用于断点续传、大文件分段处理、定点修改文件内容。

### Q12：乱码怎么解决？
答：确保写入与读取使用同一字符集（推荐 UTF-8），全程显式指定，避免依赖平台默认编码；确认源文件本身编码与读取编码一致。

---

> IO 是 Java 与外部世界交互的桥梁。建议动手实现：文件拷贝工具、按行统计词频、简易聊天室（BIO/NIO 各一版），并对照 `java.nio` 源码理解 Buffer 的 position/limit 机制。
