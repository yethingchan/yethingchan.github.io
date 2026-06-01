我来**逐行、逐段、超通俗、零基础**给你讲懂这段代码，**不跳步、不装专业、全部大白话**。
这是 **若依（RuoYi）框架里的文件工具类**，专门用来：**文件上传、下载、删除、文件名处理、图片类型判断**。

我直接**完整逐行解释**，你照着看就能完全懂👇

---

# 一、包名 + 导入依赖
```java
package com.ruoyi.common.utils.file;
```
→ 这个类放在哪个文件夹里

```java
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
...
```
→ 导入 Java 自带的**文件读写、流操作**工具

```java
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
```
→ 导入**请求、响应**（网页上传下载用）

```java
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
```
→ 别人写好的**文件工具包**，简化代码

```java
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
...
```
→ 若依框架自己的配置、常量

---

# 二、类定义
```java
public class FileUtils
```
→ **文件工具类**，专门提供各种文件操作方法
→ 里面全是 **static 方法**，直接用，不用 new 对象

---

# 三、常量（文件名允许的字符规则）
```java
public static String FILENAME_PATTERN = "[a-zA-Z0-9_\\-\\|\\.\\u4e00-\\u9fa5]+";
```
→ 文件名只能包含：**字母、数字、下划线、点、中文**
→ 防止恶意文件名（安全校验）

---

# 四、核心方法 1：输出文件到浏览器（下载用）
```java
public static void writeBytes(String filePath, OutputStream os) throws IOException
{
    FileInputStream fis = null;
    try
    {
        File file = new File(filePath); // 根据路径创建文件对象
        if (!file.exists()) { // 如果文件不存在
            throw new FileNotFoundException(filePath); // 抛异常
        }
        fis = new FileInputStream(file); // 打开文件读取流
        byte[] b = new byte[1024]; // 缓冲区，一次读1KB
        int length;
        while ((length = fis.read(b)) > 0) { // 循环读文件
            os.write(b, 0, length); // 写到浏览器输出流
        }
    }
    catch (IOException e) { throw e; } // 异常抛出
    finally {
        IOUtils.close(os); // 关闭输出流
        IOUtils.close(fis); // 关闭文件流
    }
}
```
**作用：把服务器上的文件，读出来，写给浏览器 → 实现下载**

---

# 五、核心方法 2：把字节数据写入文件（上传用）
```java
public static String writeBytes(byte[] data, String uploadDir) throws IOException
{
    FileOutputStream fos = null;
    String pathName = "";
    try
    {
        String extension = getFileExtendName(data); // 获取文件后缀（jpg/png）
        pathName = DateUtils.datePath() + "/" + IdUtils.fastUUID() + "." + extension;
        // 路径例子：2025/12/25/xxxx.jpg
        File file = FileUploadUtils.getAbsoluteFile(uploadDir, pathName);
        fos = new FileOutputStream(file); // 创建输出流
        fos.write(data); // 写字节数据到文件
    }
    finally {
        IOUtils.close(fos); // 关闭流
    }
    return FileUploadUtils.getPathFileName(uploadDir, pathName);
}
```
**作用：把前端传过来的文件字节，保存到服务器硬盘**

---

# 六、方法 3：删除文件
```java
public static boolean deleteFile(String filePath)
{
    boolean flag = false;
    File file = new File(filePath);
    if (file.isFile() && file.exists()) { // 如果是文件且存在
        flag = file.delete(); // 删除
    }
    return flag; // 返回是否删除成功
}
```

---

# 七、方法 4：文件名是否合法（安全）
```java
public static boolean isValidFilename(String filename)
{
    return filename.matches(FILENAME_PATTERN);
}
```
→ 用前面定义的规则校验，**防止恶意文件名攻击**

---

# 八、方法 5：检查文件是否允许下载
```java
public static boolean checkAllowDownload(String resource)
{
    if (StringUtils.contains(resource, "..")) { // 禁止 ../ 跳目录
        return false;
    }
    // 检查后缀是否在允许列表（jpg,png,txt,zip...）
    if (ArrayUtils.contains(MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION, FileTypeUtils.getFileType(resource))) {
        return true;
    }
    return false;
}
```
→ **安全！防止黑客下载服务器配置文件**

---

# 九、方法 6：下载时文件名编码（解决中文乱码）
```java
public static void setAttachmentResponseHeader(HttpServletResponse response, String realFileName)
{
    String percentEncodedFileName = percentEncode(realFileName); // 编码中文

    StringBuilder contentDispositionValue = new StringBuilder();
    contentDispositionValue.append("attachment; filename=")
        .append(percentEncodedFileName)
        .append(";filename*=utf-8''").append(percentEncodedFileName);

    response.setHeader("Content-disposition", contentDispositionValue.toString());
}
```
→ **告诉浏览器：这是下载文件，中文名不乱码**

---

# 十、方法 7：百分号编码（把中文转成浏览器能识别的格式）
```java
public static String percentEncode(String s)
{
    String encode = URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
    return encode.replaceAll("\\+", "%20");
}
```

---

# 十一、方法 8：根据文件头判断图片类型（超级常用）
```java
public static String getFileExtendName(byte[] photoByte)
{
    String strFileExtendName = "jpg";
    if ((photoByte[0] == 71) && (photoByte[1] == 73)) { // GIF 文件头
        strFileExtendName = "gif";
    } else if ((photoByte[0] == 66) && (photoByte[1] == 77)) { // BMP
        strFileExtendName = "bmp";
    } else if ((photoByte[1] == 80) && (photoByte[2] == 78)) { // PNG
        strFileExtendName = "png";
    }
    return strFileExtendName;
}
```
→ **不看后缀，直接读文件内容判断真实类型，防篡改**

---

# 十二、方法 9：从路径里获取文件名
```java
public static String getName(String fileName)
{
    int lastUnixPos = fileName.lastIndexOf('/');
    int lastWindowsPos = fileName.lastIndexOf('\\');
    int index = Math.max(lastUnixPos, lastWindowsPos);
    return fileName.substring(index + 1);
}
```
例如：
`/upload/2025/12/abc.png` → 返回 `abc.png`

---

# 十三、方法 10：获取不带后缀的文件名
```java
public static String getNameNotSuffix(String fileName)
{
    String baseName = FilenameUtils.getBaseName(fileName);
    return baseName;
}
```
`abc.png` → `abc`

---

# 🔥 超级总结（一句话懂整个类）
## 这个 `FileUtils` 就是若依的**万能文件工具箱**
它能做：
1. **文件下载**（写给浏览器）
2. **文件上传**（保存到服务器）
3. **文件删除**
4. **文件名安全验证**
5. **中文乱码处理**
6. **判断图片真实类型**
7. **路径处理**

全部方法都是 **static 静态方法** → 直接调用，不用 new 对象。

---

## 你随便指一个方法，我可以给你画流程图 + 举实际运行例子！
要不要我给你演示：
**前端上传一张图片 → 这个类怎么一步步保存到硬盘？**
超级直观！