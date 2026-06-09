# Android 锁屏保持 WiFi 连接——双重锁技术  
  
## 问题背景  
  
Android 系统为了省电，在屏幕关闭后会进入休眠模式：  
- **CPU 降频/休眠** → 后台服务暂停，网络请求无法处理  
- **WiFi 进入低功耗模式** → WiFi 连接可能断开或降速  
  
这导致手机开启 WiFi 热点后，一旦锁屏，电脑浏览器就无法再访问 App 的 Web 服务。  
  
## 解决方案：双重锁（WakeLock + WifiLock）  
  
### 1. WakeLock（保持 CPU 运行）  
  
```kotlin  
import android.os.PowerManager  
import android.content.Context  
  
val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager  
val wakeLock = powerManager.newWakeLock(  
    PowerManager.PARTIAL_WAKE_LOCK,  // 只保持 CPU 运行，不保持屏幕亮  
    "ToDoList::WifiServer"           // 锁的标签，用于调试  
)  
wakeLock.acquire()   // 获取锁  
wakeLock.release()   // 释放锁  
```  
  
**作用**：防止 CPU 进入深度休眠，后台的 Socket 服务和网络轮询可以继续运行。  
  
**注意**：`PARTIAL_WAKE_LOCK` 是最轻量的锁，不会保持屏幕亮起，耗电最低。  
  
### 2. WifiLock（保持 WiFi 连接活跃）  
  
```kotlin  
import android.net.wifi.WifiManager  
  
val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager  
val wifiLock = wifiManager.createWifiLock(  
    WifiManager.WIFI_MODE_FULL_HIGH_PERF,  // 高性能模式，WiFi 不会降速  
    "ToDoList::WifiLock"                    // 锁的标签  
)  
wifiLock.acquire()   // 获取锁  
wifiLock.release()   // 释放锁  
```  
  
**作用**：防止 WiFi 进入低功耗模式，保持网络连接稳定。  
  
**WifiLock 模式对比**：  
  
| 模式 | 说明 | 适用场景 |  
|------|------|---------|  
| `WIFI_MODE_FULL` | 保持 WiFi 活跃，但可能降速 | 普通后台同步 |  
| `WIFI_MODE_FULL_HIGH_PERF` | 保持 WiFi 高性能，不降速 | 实时通信、热点服务 |  
| `WIFI_MODE_SCAN_ONLY` | 只允许扫描，不允许连接 | 仅扫描 WiFi 列表 |  
  
### 3. AndroidManifest.xml 权限声明  
  
```xml  
<uses-permission android:name="android.permission.WAKE_LOCK" />  
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />  
```  
  
## 完整使用示例（Jetpack Compose）  
  
```kotlin  
@Composable  
fun ToDoListApp(viewModel: TaskViewModel) {  
    val context = LocalContext.current  
    // 创建 WakeLock（保持 CPU）  
    val wakeLock = remember {        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ToDoList::WifiServer")    }  
    // 创建 WifiLock（保持 WiFi 连接）  
    val wifiLock = remember {        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager        wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ToDoList::WifiLock")    }  
    // 开启 WiFi 服务时获取锁  
    fun startServer() {        server.start()        try { wakeLock.acquire() } catch (_: Exception) {}        try { wifiLock.acquire() } catch (_: Exception) {}    }  
    // 关闭 WiFi 服务时释放锁  
    fun stopServer() {        try { server.stop() } catch (_: Exception) {}        try { if (wakeLock.isHeld) wakeLock.release() } catch (_: Exception) {}        try { if (wifiLock.isHeld) wifiLock.release() } catch (_: Exception) {}    }  
    // App 退出时自动释放  
    DisposableEffect(Unit) {        onDispose {            try { server.stop() } catch (_: Exception) {}            try { if (wakeLock.isHeld) wakeLock.release() } catch (_: Exception) {}            try { if (wifiLock.isHeld) wifiLock.release() } catch (_: Exception) {}        }    }}  
```  
  
## 生命周期管理  
  
| 时机 | 操作 |  
|------|------|  
| 用户点击"开启 WiFi 传输" | `wakeLock.acquire()` + `wifiLock.acquire()` + `server.start()` |  
| 用户点击"关闭 WiFi 传输" | `server.stop()` + `wakeLock.release()` + `wifiLock.release()` |  
| App 退出（Activity 销毁） | `DisposableEffect.onDispose` 中释放所有锁 |  
| App 切到后台 | 锁仍然持有，服务继续运行 |  
  
## 注意事项  
  
1. **必须释放锁**：持有 WakeLock 会增加耗电，不用时必须调用 `release()`  
2. **使用 `isHeld` 检查**：释放前检查 `wakeLock.isHeld`，避免重复释放抛异常  
3. **try-catch 保护**：`acquire()` 和 `release()` 都可能抛异常，务必包裹在 try-catch 中  
4. **WifiLock 用 applicationContext**：创建 WifiLock 时使用 `applicationContext` 而非 Activity Context，避免内存泄漏  
5. **Android 6.0+ 权限**：`WAKE_LOCK` 是普通权限，不需要运行时请求，只需在 Manifest 中声明