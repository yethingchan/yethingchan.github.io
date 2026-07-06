# 06 - TCP/UDP 网络编程

---

## 一、知识讲解

### 1.1 TCP vs UDP 区别与选型

| 特性 | TCP | UDP |
|------|-----|-----|
| 连接方式 | 面向连接（三次握手） | 无连接 |
| 可靠性 | 可靠传输（确认重传） | 不可靠（尽力交付） |
| 有序性 | 保证数据有序到达 | 不保证顺序 |
| 流量控制 | 有（滑动窗口） | 无 |
| 传输效率 | 较低（协议开销大） | 高（头部仅8字节） |
| 通信模式 | 一对一 | 一对一/一对多/多对多 |
| 实时性 | 较低 | 高 |
| 适用场景 | 数据采集、文件传输、Web | 视频流、DNS、实时控制 |

#### 工业场景选型

- **选 TCP**：Modbus TCP、OPC UA、与PLC/上位机可靠通信、大数据量传输
- **选 UDP**：实时控制指令（丢失一两帧无所谓，但延迟必须低）、广播通知、设备发现
- **混合使用**：TCP用于数据采集，UDP用于心跳检测或设备状态广播

### 1.2 TcpClient / TcpListener 基础

C# 中使用 `System.Net.Sockets` 命名空间下的类进行 TCP 通信。

```
TCP通信模型：

客户端（TcpClient）                服务端（TcpListener）
    │                                    │
    │──── Connect() 连接请求 ──────────>│
    │<─── 接受连接 ──────────────────────│
    │                                    │
    │──── Write() 发送数据 ─────────────>│  NetworkStream
    │<─── Read() 读取数据 ──────────────│
    │                                    │
    │──── Close() 关闭连接 ─────────────>│
```

#### TcpClient（客户端）

```csharp
using System.Net.Sockets;

// 创建客户端并连接
TcpClient client = new TcpClient();
client.Connect("192.168.1.100", 8080);  // 连接到服务器

// 获取网络流，用于读写数据
NetworkStream stream = client.GetStream();

// 发送数据
byte[] sendData = Encoding.UTF8.GetBytes("Hello Server");
stream.Write(sendData, 0, sendData.Length);

// 读取数据
byte[] buffer = new byte[1024];
int bytesRead = stream.Read(buffer, 0, buffer.Length);
string response = Encoding.UTF8.GetString(buffer, 0, bytesRead);

// 关闭
stream.Close();
client.Close();
```

#### TcpListener（服务端）

```csharp
using System.Net;
using System.Net.Sockets;

// 创建监听器，绑定IP和端口
TcpListener listener = new TcpListener(IPAddress.Any, 8080);
listener.Start();
Console.WriteLine("服务器已启动，等待连接...");

// 接受客户端连接（阻塞方法）
TcpClient client = listener.AcceptTcpClient();
Console.WriteLine($"客户端已连接: {client.Client.RemoteEndPoint}");

// 获取网络流
NetworkStream stream = client.GetStream();

// 读取客户端数据
byte[] buffer = new byte[1024];
int bytesRead = stream.Read(buffer, 0, buffer.Length);
string message = Encoding.UTF8.GetString(buffer, 0, bytesRead);

// 发送响应
byte[] response = Encoding.UTF8.GetBytes("Message received");
stream.Write(response, 0, response.Length);

// 关闭
stream.Close();
client.Close();
listener.Stop();
```

### 1.3 异步 TCP 通信

在工业上位机中，推荐使用异步方式避免阻塞UI线程。

```csharp
using System;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;

/// <summary>
/// 异步TCP客户端示例
/// 上位机作为客户端连接到设备/服务器
/// </summary>
public class AsyncTcpClient
{
    private TcpClient _client;
    private NetworkStream _stream;

    /// <summary>
    /// 异步连接
    /// </summary>
    public async Task<bool> ConnectAsync(string ip, int port)
    {
        try
        {
            _client = new TcpClient();
            await _client.ConnectAsync(ip, port);
            _stream = _client.GetStream();
            Console.WriteLine($"已连接到 {ip}:{port}");

            // 启动异步接收
            _ = ReceiveAsync();

            return true;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"连接失败: {ex.Message}");
            return false;
        }
    }

    /// <summary>
    /// 异步发送数据
    /// </summary>
    public async Task SendAsync(byte[] data)
    {
        if (_stream == null) return;

        try
        {
            await _stream.WriteAsync(data, 0, data.Length);
            Console.WriteLine($"发送: {BitConverter.ToString(data)}");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"发送失败: {ex.Message}");
        }
    }

    /// <summary>
    /// 异步持续接收数据（后台循环）
    /// </summary>
    private async Task ReceiveAsync()
    {
        byte[] buffer = new byte[4096];

        try
        {
            while (true)
            {
                int bytesRead = await _stream.ReadAsync(buffer, 0, buffer.Length);
                if (bytesRead == 0) break;  // 连接已断开

                byte[] data = new byte[bytesRead];
                Array.Copy(buffer, data, bytesRead);

                Console.WriteLine($"收到: {BitConverter.ToString(data)}");
                // 触发数据处理
                ProcessData(data);
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"接收异常: {ex.Message}");
        }
    }

    /// <summary>
    /// 处理接收到的数据
    /// </summary>
    private void ProcessData(byte[] data)
    {
        // 根据自定义协议解析数据
        string text = Encoding.UTF8.GetString(data);
        Console.WriteLine($"解析: {text}");
    }

    public void Close()
    {
        _stream?.Close();
        _client?.Close();
    }
}
```

### 1.4 UdpClient 基础

```csharp
using System;
using System.Net;
using System.Net.Sockets;
using System.Text;

/// <summary>
/// UDP通信示例
/// UDP是无连接的，不需要先建立连接
/// 适合广播、设备发现、实时数据推送
/// </summary>
public class UdpCommunicationExample
{
    // ===== UDP发送端 =====
    public void SendExample()
    {
        UdpClient udp = new UdpClient();
        // 不需要Connect，直接发送
        string message = "Hello UDP";
        byte[] data = Encoding.UTF8.GetBytes(message);

        // 发送到指定IP和端口
        udp.Send(data, data.Length, "192.168.1.255", 5000);  // 广播
        // udp.Send(data, data.Length, "192.168.1.100", 5000); // 单播

        udp.Close();
    }

    // ===== UDP接收端 =====
    public void ReceiveExample()
    {
        UdpClient udp = new UdpClient(5000);  // 绑定本地端口5000
        IPEndPoint remoteEP = new IPEndPoint(IPAddress.Any, 0);

        Console.WriteLine("等待UDP数据...");

        // 接收数据（阻塞）
        byte[] data = udp.Receive(ref remoteEP);
        string message = Encoding.UTF8.GetString(data);

        Console.WriteLine($"来自 {remoteEP}: {message}");
        udp.Close();
    }

    // ===== UDP广播 =====
    public void BroadcastExample()
    {
        UdpClient udp = new UdpClient();
        udp.EnableBroadcast = true;  // 启用广播

        string message = "DISCOVER";  // 设备发现消息
        byte[] data = Encoding.UTF8.GetBytes(message);

        // 发送到广播地址（255.255.255.255）
        udp.Send(data, data.Length, "255.255.255.255", 5000);
        Console.WriteLine("广播已发送");

        udp.Close();
    }
}
```

### 1.5 心跳保活机制

在长时间通信中，需要定期发送心跳包检测连接是否仍然存活。

```csharp
using System;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;

/// <summary>
/// 心跳保活机制
/// 定期发送心跳包，如果连续多次未收到响应则判定断线
/// </summary>
public class HeartbeatKeeper
{
    private TcpClient _client;
    private NetworkStream _stream;
    private Timer _heartbeatTimer;
    private volatile bool _isAlive = false;
    private int _missedHeartbeats = 0;
    private const int MaxMissedHeartbeats = 3;
    private const int HeartbeatIntervalMs = 5000;  // 5秒一次

    /// <summary>连接断开事件</summary>
    public event Action OnDisconnected;

    /// <summary>当前连接是否存活</summary>
    public bool IsAlive => _isAlive;

    /// <summary>
    /// 启动心跳
    /// </summary>
    public void StartHeartbeat()
    {
        _isAlive = true;
        _missedHeartbeats = 0;

        // 定时发送心跳包
        _heartbeatTimer = new Timer(async _ =>
        {
            await SendHeartbeatAsync();
        }, null, HeartbeatIntervalMs, HeartbeatIntervalMs);

        Console.WriteLine($"心跳已启动，间隔 {HeartbeatIntervalMs}ms");
    }

    /// <summary>
    /// 停止心跳
    /// </summary>
    public void StopHeartbeat()
    {
        _heartbeatTimer?.Dispose();
        _heartbeatTimer = null;
    }

    /// <summary>
    /// 发送心跳包
    /// 心跳包格式：[0xAA][0x55][时间戳4字节]
    /// </summary>
    private async Task SendHeartbeatAsync()
    {
        try
        {
            if (_client == null || !_client.Connected)
            {
                HandleDisconnect();
                return;
            }

            // 构造心跳包
            byte[] heartbeat = new byte[6];
            heartbeat[0] = 0xAA;        // 心跳标识头
            heartbeat[1] = 0x55;
            int timestamp = (int)(DateTime.UtcNow.Ticks / TimeSpan.TicksPerSecond);
            byte[] tsBytes = BitConverter.GetBytes(timestamp);
            heartbeat[2] = tsBytes[0];
            heartbeat[3] = tsBytes[1];
            heartbeat[4] = tsBytes[2];
            heartbeat[5] = tsBytes[3];

            await _stream.WriteAsync(heartbeat, 0, heartbeat.Length);

            // 设置一个标志，等待响应
            // 如果在下一个心跳周期前没有收到响应，missed+1
            // （实际实现中需要配合接收逻辑中的心跳响应检测）
        }
        catch
        {
            _missedHeartbeats++;
            if (_missedHeartbeats >= MaxMissedHeartbeats)
            {
                HandleDisconnect();
            }
        }
    }

    /// <summary>
    /// 收到心跳响应时调用
    /// </summary>
    public void OnHeartbeatResponseReceived()
    {
        _missedHeartbeats = 0;
        _isAlive = true;
    }

    /// <summary>
    /// 处理断线
    /// </summary>
    private void HandleDisconnect()
    {
        _isAlive = false;
        StopHeartbeat();
        OnDisconnected?.Invoke();
        Console.WriteLine("心跳超时，连接已断开");
    }
}
```

### 1.6 断线重连策略

```csharp
using System;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;

/// <summary>
/// 自动断线重连策略
/// 指数退避重连，避免频繁重连消耗资源
/// </summary>
public class AutoReconnector
{
    private string _ip;
    private int _port;
    private TcpClient _client;
    private Timer _reconnectTimer;
    private volatile bool _isRunning;
    private int _reconnectAttempts = 0;
    private const int MaxReconnectAttempts = 10;
    private const int BaseDelayMs = 1000;      // 基础延迟1秒
    private const int MaxDelayMs = 30000;       // 最大延迟30秒

    /// <summary>连接恢复事件</summary>
    public event Action<TcpClient> OnReconnected;
    /// <summary>连接断开事件</summary>
    public event Action OnDisconnected;
    /// <summary>重连失败事件（已达最大重试次数）</summary>
    public event Action OnReconnectFailed;

    public bool IsConnected => _client?.Connected ?? false;

    public AutoReconnector(string ip, int port)
    {
        _ip = ip;
        _port = port;
    }

    /// <summary>
    /// 初始连接
    /// </summary>
    public async Task<bool> ConnectAsync()
    {
        return await TryConnectAsync();
    }

    /// <summary>
    /// 启动自动重连
    /// </summary>
    public void StartAutoReconnect()
    {
        if (_isRunning) return;
        _isRunning = true;
        _reconnectAttempts = 0;

        // 使用Timer定时尝试重连
        _reconnectTimer = new Timer(async _ =>
        {
            if (!_isRunning) return;

            int delay = CalculateDelay(_reconnectAttempts);
            await Task.Delay(delay);

            if (await TryConnectAsync())
            {
                StopAutoReconnect();
            }
            else
            {
                _reconnectAttempts++;
                if (_reconnectAttempts >= MaxReconnectAttempts)
                {
                    StopAutoReconnect();
                    OnReconnectFailed?.Invoke();
                }
            }
        }, null, 0, Timeout.Infinite);
    }

    /// <summary>
    /// 停止自动重连
    /// </summary>
    public void StopAutoReconnect()
    {
        _isRunning = false;
        _reconnectTimer?.Dispose();
        _reconnectTimer = null;
    }

    /// <summary>
    /// 指数退避计算延迟时间
    /// 第1次: 1秒, 第2次: 2秒, 第3次: 4秒, 第4次: 8秒 ... 最大30秒
    /// </summary>
    private int CalculateDelay(int attempt)
    {
        int delay = BaseDelayMs * (int)Math.Pow(2, attempt);
        return Math.Min(delay, MaxDelayMs);
    }

    /// <summary>
    /// 尝试连接
    /// </summary>
    private async Task<bool> TryConnectAsync()
    {
        try
        {
            _client?.Close();
            _client = new TcpClient();
            await _client.ConnectAsync(_ip, _port);
            _reconnectAttempts = 0;

            Console.WriteLine($"连接成功: {_ip}:{_port}");
            OnReconnected?.Invoke(_client);
            return true;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"重连失败(第{_reconnectAttempts + 1}次): {ex.Message}");
            return false;
        }
    }
}
```

---

## 二、代码示例

### 2.1 上位机场景案例：实现自定义TCP通信协议与设备通信

```csharp
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

/// <summary>
/// 自定义TCP协议通信案例
/// 场景：上位机通过自定义TCP协议与智能设备通信
/// 协议格式：[帧头0x5A][帧头0xA5][命令字1B][数据长度2B][数据NB][校验1B][帧尾0x0D][帧尾0x0A]
/// </summary>
public class CustomProtocolClient : IDisposable
{
    // ========== 协议常量 ==========
    private const byte FRAME_HEAD_1 = 0x5A;
    private const byte FRAME_HEAD_2 = 0xA5;
    private const byte FRAME_TAIL_1 = 0x0D;
    private const byte FRAME_TAIL_2 = 0x0A;

    // ========== 命令字定义 ==========
    private const byte CMD_READ_DATA = 0x01;
    private const byte CMD_WRITE_DATA = 0x02;
    private const byte CMD_HEARTBEAT = 0x03;
    private const byte CMD_RESPONSE = 0x81;  // 响应 = 请求 + 0x80

    // ========== 私有字段 ==========
    private TcpClient _client;
    private NetworkStream _stream;
    private List<byte> _receiveBuffer = new List<byte>();
    private readonly object _bufferLock = new object();
    private Timer _heartbeatTimer;
    private bool _isConnected;

    // ========== 事件 ==========
    public event Action<byte, byte[]> OnDataReceived;  // (命令字, 数据)
    public event Action<string> OnLog;
    public event Action OnDisconnected;
    public event Action OnConnected;

    // ========== 属性 ==========
    public string Ip { get; set; } = "127.0.0.1";
    public int Port { get; set; } = 9000;
    public bool IsConnected => _isConnected;

    /// <summary>
    /// 连接设备
    /// </summary>
    public async Task<bool> ConnectAsync()
    {
        try
        {
            _client = new TcpClient();
            _client.ReceiveTimeout = 0;
            _client.SendTimeout = 3000;
            await _client.ConnectAsync(Ip, Port);
            _stream = _client.GetStream();
            _isConnected = true;

            Log($"已连接到 {Ip}:{Port}");
            OnConnected?.Invoke();

            // 启动接收线程
            _ = ReceiveLoopAsync();

            // 启动心跳
            StartHeartbeat();

            return true;
        }
        catch (Exception ex)
        {
            Log($"连接失败: {ex.Message}");
            return false;
        }
    }

    /// <summary>
    /// 断开连接
    /// </summary>
    public void Disconnect()
    {
        StopHeartbeat();
        _isConnected = false;
        _stream?.Close();
        _client?.Close();
        Log("连接已断开");
    }

    /// <summary>
    /// 发送命令（自动构造协议帧）
    /// </summary>
    public async Task<bool> SendCommandAsync(byte command, byte[] data = null)
    {
        if (!_isConnected || _stream == null)
        {
            Log("未连接，无法发送");
            return false;
        }

        try
        {
            byte[] frame = BuildFrame(command, data);
            await _stream.WriteAsync(frame, 0, frame.Length);
            Log($"发送: [{command:X2}] {BitConverter.ToString(frame)}");
            return true;
        }
        catch (Exception ex)
        {
            Log($"发送失败: {ex.Message}");
            HandleDisconnect();
            return false;
        }
    }

    // ========== 协议帧构造 ==========

    /// <summary>
    /// 构造协议帧
    /// 帧格式：[5A][A5][CMD][LEN_H][LEN_L][DATA...][XOR][0D][0A]
    /// </summary>
    private byte[] BuildFrame(byte command, byte[] data)
    {
        data ??= Array.Empty<byte>();
        int dataLen = data.Length;

        // 帧总长度 = 帧头(2) + 命令(1) + 长度(2) + 数据(N) + 校验(1) + 帧尾(2)
        byte[] frame = new byte[2 + 1 + 2 + dataLen + 1 + 2];
        int offset = 0;

        // 帧头
        frame[offset++] = FRAME_HEAD_1;
        frame[offset++] = FRAME_HEAD_2;

        // 命令字
        frame[offset++] = command;

        // 数据长度（大端序）
        frame[offset++] = (byte)(dataLen >> 8);
        frame[offset++] = (byte)(dataLen & 0xFF);

        // 数据
        if (dataLen > 0)
        {
            Array.Copy(data, 0, frame, offset, dataLen);
            offset += dataLen;
        }

        // 校验（XOR：帧头到数据的所有字节异或）
        byte xor = 0;
        for (int i = 0; i < offset; i++)
        {
            xor ^= frame[i];
        }
        frame[offset++] = xor;

        // 帧尾
        frame[offset++] = FRAME_TAIL_1;
        frame[offset++] = FRAME_TAIL_2;

        return frame;
    }

    // ========== 接收与解析 ==========

    /// <summary>
    /// 异步接收循环
    /// </summary>
    private async Task ReceiveLoopAsync()
    {
        byte[] buffer = new byte[4096];

        try
        {
            while (_isConnected)
            {
                int bytesRead = await _stream.ReadAsync(buffer, 0, buffer.Length);
                if (bytesRead == 0)
                {
                    HandleDisconnect();
                    break;
                }

                // 将新数据追加到缓冲区
                lock (_bufferLock)
                {
                    byte[] newData = new byte[bytesRead];
                    Array.Copy(buffer, newData, bytesRead);
                    _receiveBuffer.AddRange(newData);
                }

                // 尝试解析完整帧
                TryParseFrames();
            }
        }
        catch (IOException)
        {
            HandleDisconnect();
        }
        catch (Exception ex)
        {
            Log($"接收异常: {ex.Message}");
            HandleDisconnect();
        }
    }

    /// <summary>
    /// 尝试从缓冲区中解析完整帧
    /// </summary>
    private void TryParseFrames()
    {
        lock (_bufferLock)
        {
            while (_receiveBuffer.Count >= 7)  // 最小帧长度
            {
                // 查找帧头
                int headIndex = -1;
                for (int i = 0; i < _receiveBuffer.Count - 1; i++)
                {
                    if (_receiveBuffer[i] == FRAME_HEAD_1 &&
                        _receiveBuffer[i + 1] == FRAME_HEAD_2)
                    {
                        headIndex = i;
                        break;
                    }
                }

                if (headIndex < 0)
                {
                    // 没找到帧头，清空缓冲区
                    _receiveBuffer.Clear();
                    break;
                }

                // 丢弃帧头之前的无效数据
                if (headIndex > 0)
                {
                    _receiveBuffer.RemoveRange(0, headIndex);
                }

                // 检查是否有足够数据解析帧头+命令+长度
                if (_receiveBuffer.Count < 5)
                    break;

                byte command = _receiveBuffer[2];
                int dataLength = (_receiveBuffer[3] << 8) | _receiveBuffer[4];

                // 计算完整帧长度
                int frameLength = 2 + 1 + 2 + dataLength + 1 + 2;
                if (_receiveBuffer.Count < frameLength)
                    break;  // 数据不够，等待更多数据

                // 提取完整帧
                byte[] frame = _receiveBuffer.GetRange(0, frameLength).ToArray();
                _receiveBuffer.RemoveRange(0, frameLength);

                // 校验帧尾
                if (frame[frameLength - 2] != FRAME_TAIL_1 ||
                    frame[frameLength - 1] != FRAME_TAIL_2)
                {
                    Log("帧尾校验失败，丢弃");
                    continue;
                }

                // 校验XOR
                byte xor = 0;
                for (int i = 0; i < frameLength - 3; i++)
                {
                    xor ^= frame[i];
                }
                if (xor != frame[frameLength - 3])
                {
                    Log("XOR校验失败，丢弃");
                    continue;
                }

                // 提取数据部分
                byte[] data = new byte[dataLength];
                if (dataLength > 0)
                {
                    Array.Copy(frame, 5, data, 0, dataLength);
                }

                Log($"收到: [{command:X2}] 数据长度={dataLength}");
                OnDataReceived?.Invoke(command, data);
            }
        }
    }

    // ========== 心跳 ==========

    private void StartHeartbeat()
    {
        _heartbeatTimer = new Timer(async _ =>
        {
            await SendCommandAsync(CMD_HEARTBEAT);
        }, null, 5000, 5000);  // 每5秒发送一次心跳
    }

    private void StopHeartbeat()
    {
        _heartbeatTimer?.Dispose();
        _heartbeatTimer = null;
    }

    private void HandleDisconnect()
    {
        _isConnected = false;
        StopHeartbeat();
        OnDisconnected?.Invoke();
        Log("连接已断开");
    }

    private void Log(string message)
    {
        OnLog?.Invoke($"[{DateTime.Now:HH:mm:ss.fff}] {message}");
    }

    // ========== 使用示例 ==========

    public static async Task RunExample()
    {
        var client = new CustomProtocolClient
        {
            Ip = "192.168.1.200",
            Port = 9000
        };

        client.OnLog = msg => Console.WriteLine(msg);
        client.OnDataReceived = (cmd, data) =>
        {
            Console.WriteLine($"处理命令 0x{cmd:X2}, 数据: {BitConverter.ToString(data)}");
        };
        client.OnDisconnected = () =>
        {
            Console.WriteLine("连接断开！5秒后重连...");
            Task.Delay(5000).ContinueWith(_ => client.ConnectAsync());
        };

        // 连接
        if (await client.ConnectAsync())
        {
            // 读取设备数据
            byte[] readCmd = new byte[] { 0x00, 0x00, 0x00, 0x0A };  // 地址0，读10个
            await client.SendCommandAsync(CMD_READ_DATA, readCmd);

            // 写入设备数据
            byte[] writeData = new byte[] { 0x00, 0x01, 0x03, 0xE8 };  // 地址1，写1000
            await client.SendCommandAsync(CMD_WRITE_DATA, writeData);

            Console.WriteLine("按任意键退出...");
            Console.ReadKey();

            client.Disconnect();
        }

        client.Dispose();
    }

    public void Dispose()
    {
        Disconnect();
    }
}
```

---

## 三、注意事项

1. **粘包与拆包**：TCP是流式传输，没有消息边界。接收方必须通过帧头/帧尾/长度字段来识别消息边界。切勿假设一次 `Read` 就能收到完整的消息。
2. **大端序 vs 小端序**：网络协议通常使用大端序（高位在前），C# 的 `BitConverter` 取决于平台字节序。发送多字节数值时需注意字节顺序。
3. **连接超时**：`TcpClient.Connect()` 默认没有超时，如果目标不可达会卡很久。建议使用异步 `ConnectAsync()` 配合 `Task.WhenAny()` 或设置 Socket 超时。
4. **资源释放**：`NetworkStream`、`TcpClient`、`TcpListener` 必须正确关闭，否则会泄漏端口和文件描述符。
5. **TCP KeepAlive**：除了应用层心跳，还可以启用 TCP 层的 KeepAlive 机制检测死连接。
6. **线程安全**：异步方法可以在多线程中使用，但 `NetworkStream` 不是线程安全的，不应同时从多个线程调用 `WriteAsync`。

---

## 四、练习建议

### 练习1：TCP 聊天程序
- 实现一个 TCP 服务端和客户端
- 支持双向文本通信
- 显示连接/断开状态
- 记录通信日志

### 练习2：自定义协议设备通信
- 设计一个简单的二进制协议（帧头+长度+命令+数据+校验+帧尾）
- 实现客户端连接、发送命令、接收解析
- 实现心跳保活和断线重连
- 用两台电脑互测

### 练习3：多设备TCP管理器
- 同时管理多个TCP连接（多台设备）
- 每个设备独立通信、独立断线重连
- 使用 `ConcurrentDictionary` 管理连接池
- 支持设备上线/下线事件通知

---

## 五、常见错误

### 错误1：接收数据粘包
```
现象：一次Read收到了多条消息合并在一起
```
**原因**：TCP流式传输没有消息边界，发送方快速发送的多条消息可能被合并接收。
**解决**：必须使用帧头/帧尾或长度字段进行消息拆分，将缓冲区中的数据解析为独立的消息帧。

### 错误2：接收数据不完整
```
现象：一次Read只收到了消息的一部分
```
**原因**：TCP数据可能分片到达，一次Read不能保证收到完整的消息。
**解决**：将数据缓存到缓冲区，根据长度字段判断是否收齐完整帧再解析。

### 错误3：SocketException: 远程主机强迫关闭连接
```
现象：通信过程中突然断开
```
**原因**：对方设备/程序关闭了连接，或网络中断。
**解决**：捕获异常，记录日志，启动断线重连。

### 错误4： InvalidOperationException: 无法访问已释放的对象
```
原因：在关闭连接后仍然尝试读写NetworkStream
```
**解决**：在读写前检查连接状态，关闭后停止所有定时器和异步任务。

### 错误5：端口占用
```
现象：TcpListener启动时报 "地址已被使用"
```
**原因**：上一次程序未正确释放端口。
**解决**：设置 `SocketOptionName.ReuseAddress` 允许端口复用，或等待系统释放端口（通常1-2分钟）。
