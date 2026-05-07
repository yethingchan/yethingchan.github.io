---
title: "AesEncryptionAndDecryption"
description: ""
date: "2026-05-07"
tags: []
share: true
---
好的，我来为你逐段、逐逻辑深度讲解这段 **AES 对称加密/解密** 代码。

这段代码是一个典型的 AES 加密工具类，但请注意：**代码中存在一个严重的 Bug**，以及一些不推荐的写法。我会在讲解中明确指出。

---

### 一、整体概览
这是一个名为 `AesEncryptionAndDecryption` 的工具类，提供了两组功能：
1.  **带向量（IV）的 AES 加解密**（更安全，推荐）
2.  **不带向量的 AES 加解密**（使用 ECB 模式，安全性较低）

**核心概念铺垫（类比理解）：**
*   **密钥 (Key)**：就像你家门的钥匙，加密解密都用同一把（对称加密）。
*   **向量 (IV)**：就像给加密过程加的“随机盐”，即使同样的原文和密钥，每次生成的密文也不一样，防止被猜出规律。

---

### 二、第一部分：带向量的加密（推荐用法）

```csharp
public string AESEncryptedString(string encrypteStr, string key, string vector)
{
    // 1. 把待加密的字符串（明文）转换成字节数组
    byte[] aesBytes = Encoding.UTF8.GetBytes(encrypteStr);

    // 2. 准备密钥 Key
    // AES-256 需要 32 字节（256位）的密钥
    byte[] aesKey = new byte[32]; 
    // 将传入的 Base64 格式的 key 转成字节，并复制到 aesKey 数组中（截取或填充至32位）
    Array.Copy(Convert.FromBase64String(key), aesKey, aesKey.Length);

    // 3. 准备向量 IV
    // AES 的块大小固定是 16 字节（128位），所以 IV 也必须是 16 位
    byte[] aesVector = new byte[16];
    // 同样处理 IV
    Array.Copy(Convert.FromBase64String(vector), aesVector, aesVector.Length);

    // 4. 创建 AES 加密算法实例
    Rijndael Aes = Rijndael.Create(); 

    // 5. 开始加密流程（使用“流”的方式）
    // 5.1 开辟一块内存流，用来存放加密后的结果
    using MemoryStream memoryStream = new MemoryStream();
    
    // 5.2 把内存流包装成“加密流”
    // 这里传入了 Key 和 IV，并指定模式为“写入”（把数据写进去进行加密）
    using CryptoStream cryptoStream = new CryptoStream(
        memoryStream, 
        Aes.CreateEncryptor(aesKey, aesVector), 
        CryptoStreamMode.Write);
    
    // 5.3 把明文数据写入加密流
    cryptoStream.Write(aesBytes, 0, aesBytes.Length);
    // 5.4 刷新缓冲区，告诉加密流：我写完了，处理最后一块数据
    cryptoStream.FlushFinalBlock();

    // 6. 把内存流里的密文转成 Base64 字符串返回（方便传输和存储）
    string result = Convert.ToBase64String(memoryStream.ToArray());
    return result;
}
```

---

### 三、第二部分：带向量的解密（注意这里有 Bug！）

```csharp
public string AESDecryptString(string decryptStr, string key, string vector)
{
    // 1. 把密文（Base64字符串）转回字节数组
    byte[] aesBytes = Convert.FromBase64String(decryptStr);

    // 2. 处理 Key 和 IV（必须和加密时完全一致！）
    byte[] aesKey = new byte[32];
    Array.Copy(Convert.FromBase64String(key), aesKey, aesKey.Length);
    byte[] aesVector = new byte[16];
    Array.Copy(Convert.FromBase64String(vector), aesVector, aesVector.Length);
    
    Rijndael Aes = Rijndael.Create();

    // 3. 开始解密流程
    // 3.1 把密文放进内存流
    using MemoryStream memoryStream = new MemoryStream(aesBytes);
    
    // 3.2 包装成解密流，模式改为 Read（读取数据进行解密）
    using CryptoStream Decryptor = new CryptoStream(
        memoryStream, 
        Aes.CreateDecryptor(aesKey, aesVector), 
        CryptoStreamMode.Read);
    
    // 3.3 准备另一个内存流存放解密后的明文
    using MemoryStream originalMemory = new MemoryStream();
    byte[] Buffer = new byte[1024];
    int readBytes = 0;
    
    // 3.4 循环读取解密流（因为数据可能很大，分块读取）
    while ((readBytes = Decryptor.Read(Buffer, 0, Buffer.Length)) > 0)
    {
        originalMemory.Write(Buffer, 0, readBytes);
    }

    // ⚠️⚠️⚠️ 这里有个严重的 Bug！！！
    // 代码里写的是：Convert.ToBase64String(...)
    // 但实际上解密出来的是原始的 UTF-8 字节，应该用 Encoding.UTF8.GetString(...)
    // 否则你得到的不是原文，而是一段乱码的 Base64 字符串！
    
    // 错误代码：
    // string result = Convert.ToBase64String(originalMemory.ToArray());
    
    // 修正代码应该是：
    byte[] original = originalMemory.ToArray();
    string result = Encoding.UTF8.GetString(original); 
    return result;
}
```

---

### 四、第三部分：不带向量的加密（ECB 模式，不推荐）

```csharp
public static string AESEncryptedString(string encrypteStr, string key)
{
    byte[] aesBytes = Encoding.UTF8.GetBytes(encrypteStr);
    byte[] aesKey = new byte[32];

    // 这里的 Key 处理方式变了：不是 Base64 转，而是直接把字符串转成 UTF8
    // 如果 key 不够 32 位，用 PadRight 在右边补空格（这是一种很不规范的做法，空格熵太低）
    Array.Copy(Encoding.UTF8.GetBytes(key.PadRight(aesKey.Length)), aesKey, aesKey.Length);

    using MemoryStream memoryStream = new MemoryStream();
    Rijndael Aes = Rijndael.Create();

    // 显式设置加密模式为 ECB
    // ECB 模式的特点：不需要 IV，但安全性低。相同的明文块会生成相同的密文块。
    Aes.Mode = CipherMode.ECB; 
    Aes.Padding = PaddingMode.PKCS7; // 填充模式
    Aes.KeySize = 128; // 这里虽然 Key 数组是 32 位，但 KeySize 设为 128，实际只用到前 16 位（逻辑有点混乱）
    Aes.Key = aesKey;

    // 后续加密流程和之前一样
    using CryptoStream cryptoStream = new CryptoStream(memoryStream, Aes.CreateEncryptor(), CryptoStreamMode.Write);
    cryptoStream.Write(aesBytes, 0, aesBytes.Length);
    cryptoStream.FlushFinalBlock();
    Aes.Clear();
    return Convert.ToBase64String(memoryStream.ToArray());
}
```

---

### 五、总结与建议

这段代码可以学习，但**不建议直接用于生产环境**，主要问题如下：

1.  **致命 Bug**：带 IV 的解密方法最后一步用错了转换函数。
2.  **安全性**：ECB 模式（无 IV）安全性差，尽量不要用。
3.  **密钥处理**：用 `PadRight` 补空格作为密钥是极其危险的，密钥应该是随机的字节数组。
4.  **资源释放**：`Rijndael Aes` 没有用 `using` 包裹，虽然它有 `Clear()`，但最好遵循 IDisposable 规范。

**如果你需要我帮你写一份更安全、更规范的现代版 AES 加解密代码（基于 .NET 6+），请告诉我！**