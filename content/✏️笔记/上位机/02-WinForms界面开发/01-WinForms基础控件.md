# WinForms 基础控件

> WinForms（Windows Forms）是 .NET 平台上最经典的桌面 GUI 框架，也是工业上位机开发中最常用的界面技术之一。本章将系统学习 WinForms 的基础控件，掌握上位机界面开发的核心技能。

---

## 目录

1. [WinForms 项目创建步骤](#1-winforms-项目创建步骤)
2. [Form 窗体基础](#2-form-窗体基础)
3. [Button 按钮](#3-button-按钮)
4. [TextBox 文本框](#4-textbox-文本框)
5. [Label 标签](#5-label-标签)
6. [ComboBox 下拉框](#6-combobox-下拉框)
7. [CheckBox 与 RadioButton](#7-checkbox-与-radiobutton)
8. [NumericUpDown 数字输入](#8-numericupdown-数字输入)
9. [PictureBox 图片展示](#9-picturebox-图片展示)
10. [Timer 定时器](#10-timer-定时器)
11. [综合案例：PLC 地址读写测试面板](#11-综合案例plc-地址读写测试面板)

---

## 1. WinForms 项目创建步骤

### 知识讲解

WinForms 项目是 .NET 中创建桌面应用程序的标准方式。一个 WinForms 项目本质上是一个 Windows 窗体应用程序，拥有可视化的窗体设计器，支持拖拽控件来快速构建界面。

**创建步骤：**

1. 打开 Visual Studio，点击「创建新项目」
2. 在搜索框中输入「Windows Forms」，选择「Windows Forms 应用程序」模板
3. 选择框架版本（.NET 6/7/8 或 .NET Framework 4.x）
4. 填写项目名称（如 `PlcMonitorApp`），选择保存路径
5. 点击「创建」，Visual Studio 会自动生成一个默认窗体 `Form1`

**项目结构说明：**

```
PlcMonitorApp/
├── Form1.cs              // 窗体逻辑代码（partial class）
├── Form1.Designer.cs     // 设计器自动生成的控件布局代码
├── Form1.resx            // 窗体资源文件
├── Program.cs            // 程序入口（Main 方法）
└── PlcMonitorApp.csproj  // 项目配置文件
```

### 代码示例

```csharp
// Program.cs —— 程序入口点
using System;
using System.Windows.Forms;

namespace PlcMonitorApp
{
    static class Program
    {
        [STAThread]  // 单线程单元标记，WinForms 必须有此特性
        static void Main()
        {
            Application.EnableVisualStyles();     // 启用可视化样式（XP 及以后的主题）
            Application.SetCompatibleTextRenderingDefault(false); // 使用 GDI+ 渲染文字
            Application.Run(new Form1());           // 启动主窗体，开始消息循环
        }
    }
}
```

```csharp
// Form1.cs —— 主窗体
using System;
using System.Windows.Forms;

namespace PlcMonitorApp
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent(); // 初始化设计器中的所有控件（自动生成）
            
            // 可在此处进行额外的初始化操作
            this.Text = "PLC 监控上位机 v1.0";
            this.StartPosition = FormStartPosition.CenterScreen; // 窗体居中显示
        }
    }
}
```

### 注意事项

- `[STAThread]` 特性是 WinForms 的必需标记，表示应用程序使用单线程单元（Single-Threaded Apartment），缺少此特性会导致 COM 组件调用失败。
- `InitializeComponent()` 方法由设计器自动生成，包含所有在设计时添加的控件及其属性设置，**不要手动修改此方法**，应通过设计器进行修改。
- .NET 6/7/8 使用 `Windows Forms App` 模板，.NET Framework 4.x 使用 `Windows Forms Application` 模板，两者项目结构略有不同。

### 练习建议

1. 创建一个新的 WinForms 项目，命名为 `MyFirstWinFormsApp`
2. 尝试修改窗体的 `Text` 属性，观察窗口标题栏的变化
3. 修改 `BackColor` 属性，设置窗体背景颜色
4. 设置 `FormBorderStyle` 为 `None`，观察窗体边框消失的效果

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| 窗体无法显示 | 忘记调用 `Application.Run()` | 在 `Main` 方法中添加 `Application.Run(new Form1())` |
| 控件在设计器中看不到 | 控件被其他控件遮挡或 `Visible=false` | 选中控件后在属性窗口设置 `Visible=true` |
| 编译报错找不到控件 | `Designer.cs` 文件损坏 | 右键窗体 →「还原设计器生成的代码」 |

---

## 2. Form 窗体基础

### 知识讲解

`Form` 是所有 WinForms 窗体的基类，每个窗口都是一个 `Form` 对象。理解 Form 的属性、事件和生命周期是 WinForms 开发的基础。

**常用属性：**

| 属性 | 说明 | 典型值 |
|------|------|--------|
| `Text` | 窗口标题 | "PLC 监控系统" |
| `Size` | 窗体大小 | `1200, 800` |
| `StartPosition` | 启动位置 | `CenterScreen`、`CenterParent`、`Manual` |
| `FormBorderStyle` | 边框样式 | `Sizable`、`FixedSingle`、`None` |
| `MaximizeBox` | 是否显示最大化按钮 | `true/false` |
| `MinimizeBox` | 是否显示最小化按钮 | `true/false` |
| `WindowState` | 窗口状态 | `Normal`、`Maximized`、`Minimized` |
| `TopMost` | 是否置顶 | `true/false` |
| `Opacity` | 透明度（0.0~1.0） | `0.8` |
| `KeyPreview` | 是否先接收键盘事件 | `true`（在按钮处理前截获按键） |

**窗体生命周期事件：**

```
构造函数 → Load → Shown（已显示，可交互）→ ... 运行中 ... → FormClosing → FormClosed
```

### 代码示例

```csharp
public partial class MainForm : Form
{
    public MainForm()
    {
        InitializeComponent();
        
        // ========== 属性设置 ==========
        this.Text = "PLC 数据采集上位机";
        this.Size = new Size(1200, 800);          // 设置窗体初始大小
        this.StartPosition = FormStartPosition.CenterScreen;  // 居中启动
        this.FormBorderStyle = FormBorderStyle.FixedSingle;   // 固定单边框
        this.MaximizeBox = false;                   // 禁止最大化（工业软件常用）
        this.BackColor = Color.FromArgb(240, 240, 240);       // 浅灰背景色
        this.KeyPreview = true;                     // 开启键盘事件预览
        this.Font = new Font("微软雅黑", 9);        // 全局字体设置
    }

    // ========== Load 事件：窗体加载时触发（但窗体尚未可见）==========
    private void MainForm_Load(object sender, EventArgs e)
    {
        // 在这里初始化数据、连接数据库、加载配置等
        LoadConfiguration();
        InitializePlcConnection();
        LogInfo("系统启动完成");
    }

    // ========== Shown 事件：窗体首次显示后触发（用户已可看到窗体）==========
    private void MainForm_Shown(object sender, EventArgs e)
    {
        // 在这里执行需要在界面显示后才能做的操作
        // 例如：设置焦点、显示启动画面、自动开始采集
        txtIpAddress.Focus();
        StartDataCollection();
    }

    // ========== FormClosing 事件：窗体关闭前触发（可取消关闭）==========
    private void MainForm_FormClosing(object sender, FormClosingEventArgs e)
    {
        // 弹出确认对话框
        var result = MessageBox.Show(
            "确定要关闭系统吗？关闭后数据采集将停止。",
            "退出确认",
            MessageBoxButtons.YesNo,
            MessageBoxIcon.Question
        );

        if (result == DialogResult.No)
        {
            e.Cancel = true;  // 取消关闭操作
            return;
        }

        // 执行清理操作
        StopDataCollection();
        DisconnectPlc();
        SaveConfiguration();
        LogInfo("系统已安全关闭");
    }

    // ========== FormClosed 事件：窗体已关闭后触发（不可取消）==========
    private void MainForm_FormClosed(object sender, FormClosedEventArgs e)
    {
        // 释放非托管资源
    }

    // ========== 键盘事件预览示例 ==========
    private void MainForm_KeyDown(object sender, KeyEventArgs e)
    {
        // 因为 KeyPreview=true，此处可以在按钮之前截获按键
        if (e.KeyCode == Keys.F5)
        {
            // F5 键刷新数据
            RefreshData();
            e.Handled = true;  // 标记已处理，阻止事件继续传递
        }
        else if (e.Control && e.KeyCode == Keys.S)
        {
            // Ctrl+S 保存
            SaveData();
            e.Handled = true;
        }
    }
}
```

### 注意事项

- `Load` 事件在窗体可见之前触发，**不适合做耗时的初始化操作**（会导致界面显示缓慢）。耗时操作应放在 `Shown` 事件或使用异步方式。
- `Shown` 事件只会在窗体**首次显示时**触发一次（从 `Visible=false` 变为 `true`），之后即使最小化再恢复也不会再次触发。
- `FormClosing` 中设置 `e.Cancel = true` 可以阻止窗体关闭，这在防止用户误操作时非常有用。
- 工业上位机中，通常建议**禁用最大化按钮**（`MaximizeBox = false`），因为界面布局是针对固定分辨率设计的。

### 练习建议

1. 创建一个窗体，分别订阅 `Load`、`Shown`、`FormClosing` 事件，在事件处理方法中使用 `MessageBox` 弹出提示，观察事件的触发顺序。
2. 设置 `KeyPreview = true`，实现按 `Esc` 键关闭窗体的功能。
3. 实现 `FormClosing` 事件中的退出确认逻辑。

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| 事件处理方法未触发 | 忘记在属性窗口中绑定事件 | 双击控件或手动在属性窗口的事件页中绑定 |
| Load 事件中界面卡顿 | 在 Load 事件中执行了耗时操作 | 将耗时操作移到 Shown 事件，或使用 async/await |
| FormClosing 中退出对话框死循环 | 事件处理中又调用了 `this.Close()` | 使用 `e.Cancel` 而不是手动关闭 |

---

## 3. Button 按钮

### 知识讲解

`Button` 是最常用的交互控件，用户通过点击按钮来触发特定操作。在上位机开发中，按钮用于启动/停止采集、手动读写 PLC、连接/断开设备等操作。

**常用属性：**

| 属性 | 说明 |
|------|------|
| `Text` | 按钮文字 |
| `Enabled` | 是否可用（灰显禁用状态） |
| `Visible` | 是否可见 |
| `FlatStyle` | 按钮样式（`Flat`、`Popup`、`Standard`） |
| `BackColor` / `ForeColor` | 背景色 / 前景色 |
| `Image` / `ImageAlign` | 按钮图片 / 图片对齐方式 |

**常用事件：**

| 事件 | 说明 |
|------|------|
| `Click` | 单击事件（最常用） |
| `MouseEnter` / `MouseLeave` | 鼠标进入/离开 |
| `MouseDown` / `MouseUp` | 鼠标按下/抬起 |

### 代码示例

```csharp
public partial class ButtonDemoForm : Form
{
    public ButtonDemoForm()
    {
        InitializeComponent();
    }

    // ========== 基本 Click 事件 ==========
    private void btnConnect_Click(object sender, EventArgs e)
    {
        try
        {
            // 禁用按钮，防止重复点击
            btnConnect.Enabled = false;
            btnDisconnect.Enabled = true;

            // 执行连接操作
            string ip = txtIpAddress.Text.Trim();
            int port = (int)nudPort.Value;
            ConnectToPlc(ip, port);

            lblStatus.Text = "已连接";
            lblStatus.ForeColor = Color.Green;
        }
        catch (Exception ex)
        {
            MessageBox.Show($"连接失败：{ex.Message}", "错误", 
                MessageBoxButtons.OK, MessageBoxIcon.Error);
            
            // 恢复按钮状态
            btnConnect.Enabled = true;
            btnDisconnect.Enabled = false;
        }
    }

    // ========== 断开连接按钮 ==========
    private void btnDisconnect_Click(object sender, EventArgs e)
    {
        DisconnectFromPlc();
        
        btnConnect.Enabled = true;
        btnDisconnect.Enabled = false;
        
        lblStatus.Text = "已断开";
        lblStatus.ForeColor = Color.Red;
    }

    // ========== 异步按钮点击（避免界面卡死）==========
    private async void btnReadData_Click(object sender, EventArgs e)
    {
        btnReadData.Enabled = false;    // 禁用按钮防止重复点击
        btnReadData.Text = "读取中..."; // 更新按钮文字提示用户

        try
        {
            // 使用 async/await 在后台执行耗时操作
            var data = await Task.Run(() =>
            {
                // 模拟耗时操作（如读取 PLC 数据）
                System.Threading.Thread.Sleep(2000);
                return new { Value = 1234.56, Status = "OK" };
            });

            // 回到 UI 线程更新界面
            txtResult.Text = $"读取结果：{data.Value}";
            lblReadStatus.Text = data.Status;
        }
        catch (Exception ex)
        {
            MessageBox.Show($"读取失败：{ex.Message}", "错误",
                MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
        finally
        {
            // 无论成功失败，都恢复按钮状态
            btnReadData.Enabled = true;
            btnReadData.Text = "读取数据";
        }
    }

    // ========== 按钮悬停效果 ==========
    private void btnAction_MouseEnter(object sender, EventArgs e)
    {
        var btn = (Button)sender;
        btn.BackColor = Color.FromArgb(0, 120, 215);  // 悬停变深蓝色
        btn.Cursor = Cursors.Hand;                     // 鼠标变手型
    }

    private void btnAction_MouseLeave(object sender, EventArgs e)
    {
        var btn = (Button)sender;
        btn.BackColor = Color.FromArgb(0, 153, 255);  // 恢复原色
        btn.Cursor = Cursors.Default;                  // 恢复默认鼠标
    }
}
```

### 注意事项

- **按钮互斥**：在「连接」与「断开」这类互斥操作中，点击一个按钮后应禁用另一个，防止用户误操作。
- **异步处理**：如果按钮点击会执行耗时操作（如网络请求、PLC 通信），必须使用 `async/await` 或后台线程，否则界面会卡死无响应。
- `async void` 事件处理方法需要注意异常处理：在 `async void` 方法中抛出的异常无法被外部捕获，必须在方法内部使用 `try-catch`。

### 练习建议

1. 实现一对互斥按钮：「开始采集」和「停止采集」，点击其中一个后自动禁用另一个。
2. 创建一个按钮，点击后使用 `async/await` 模拟耗时操作（`Task.Delay`），期间按钮文字显示「处理中...」，完成后恢复。
3. 为按钮添加鼠标悬停变色效果。

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| 点击按钮界面卡死 | Click 事件中执行了耗时操作 | 使用 `async/await` 配合 `Task.Run` |
| async void 方法异常导致程序崩溃 | 异常未在方法内捕获 | 在 async void 方法中始终使用 try-catch |
| 按钮灰色无法点击 | `Enabled` 属性为 `false` | 在操作完成后记得恢复 `Enabled = true` |

---

## 4. TextBox 文本框

### 知识讲解

`TextBox` 用于文本输入和显示，是上位机中输入 IP 地址、端口号、PLC 地址等信息的核心控件。

**常用属性：**

| 属性 | 说明 |
|------|------|
| `Text` | 文本内容 |
| `ReadOnly` | 只读模式（可选中不可编辑） |
| `Multiline` | 是否允许多行 |
| `PasswordChar` | 密码字符（如 `*`） |
| `MaxLength` | 最大输入长度 |
| `TextAlign` | 文本对齐（`Left`、`Center`、`Right`） |
| `ScrollBars` | 滚动条（多行模式下使用） |
| `WordWrap` | 自动换行（多行模式下使用） |

**常用事件：**

| 事件 | 说明 |
|------|------|
| `TextChanged` | 文本改变时触发 |
| `KeyPress` | 按键时触发（可拦截非法输入） |
| `KeyDown` / `KeyUp` | 键盘按下/抬起 |
| `Enter` / `Leave` | 获得焦点 / 失去焦点 |

### 代码示例

```csharp
public partial class TextBoxDemoForm : Form
{
    public TextBoxDemoForm()
    {
        InitializeComponent();
    }

    // ========== KeyPress 事件 —— 输入验证（只允许数字）==========
    // 场景：端口号输入框，只允许输入数字
    private void txtPort_KeyPress(object sender, KeyPressEventArgs e)
    {
        // 允许数字（0-9）
        if (char.IsDigit(e.KeyChar))
            return;

        // 允许控制字符（如退格键 Backspace）
        if (char.IsControl(e.KeyChar))
            return;

        // 其他字符全部拦截
        e.Handled = true;  // 标记为已处理，阻止字符输入
    }

    // ========== KeyPress 事件 —— IP 地址输入验证（允许数字和小数点）==========
    private void txtIpAddress_KeyPress(object sender, KeyPressEventArgs e)
    {
        if (char.IsDigit(e.KeyChar) || e.KeyChar == '.')
            return;
        if (char.IsControl(e.KeyChar))
            return;

        e.Handled = true;
    }

    // ========== TextChanged 事件 —— 实时输入过滤 ==========
    // 场景：PLC 寄存器地址输入，实时验证格式
    private void txtPlcAddress_TextChanged(object sender, EventArgs e)
    {
        string text = txtPlcAddress.Text.Trim();

        // 验证地址格式（如 D100、M0.1 等）
        if (IsValidPlcAddress(text))
        {
            txtPlcAddress.BackColor = Color.White;         // 合法：白色背景
            errorProvider1.SetError(txtPlcAddress, "");      // 清除错误提示
        }
        else if (text.Length > 0)
        {
            txtPlcAddress.BackColor = Color.FromArgb(255, 230, 230); // 非法：浅红背景
            errorProvider1.SetError(txtPlcAddress, "地址格式不正确，示例：D100");
        }
        else
        {
            txtPlcAddress.BackColor = Color.White;
            errorProvider1.SetError(txtPlcAddress, "");
        }
    }

    /// <summary>
    /// 验证 PLC 地址格式的辅助方法
    /// </summary>
    private bool IsValidPlcAddress(string address)
    {
        if (string.IsNullOrEmpty(address)) return true;

        // 匹配 D100、M0、X10.0 等常见 PLC 地址格式
        return System.Text.RegularExpressions.Regex.IsMatch(
            address, @"^[DMXY]\d+(\.\d+)?$"
        );
    }

    // ========== Enter 事件 —— 获得焦点时全选 ==========
    private void txtInput_Enter(object sender, EventArgs e)
    {
        var textBox = (TextBox)sender;
        textBox.SelectAll();  // 获得焦点时选中所有文字，方便重新输入
    }

    // ========== 只读多行 TextBox —— 用作日志显示 ==========
    private void InitializeLogTextBox()
    {
        txtLog.ReadOnly = true;                          // 只读
        txtLog.Multiline = true;                         // 多行
        txtLog.ScrollBars = ScrollBars.Vertical;          // 垂直滚动条
        txtLog.BackColor = Color.FromArgb(30, 30, 30);   // 深色背景（日志区）
        txtLog.ForeColor = Color.FromArgb(200, 200, 200); // 浅灰文字
        txtLog.Font = new Font("Consolas", 9);           // 等宽字体
    }

    /// <summary>
    /// 向日志区追加一条消息
    /// </summary>
    public void LogMessage(string message)
    {
        string timeStr = DateTime.Now.ToString("HH:mm:ss.fff");
        string line = $"[{timeStr}] {message}";

        txtLog.AppendText(line + Environment.NewLine);  // 追加文本
        
        // 自动滚动到底部
        txtLog.SelectionStart = txtLog.Text.Length;
        txtLog.ScrollToCaret();
        
        // 限制日志行数，避免内存占用过大
        if (txtLog.Lines.Length > 1000)
        {
            string[] lines = txtLog.Lines;
            txtLog.Lines = lines.Skip(500).ToArray();   // 只保留最近 500 行
        }
    }
}
```

### 注意事项

- `KeyPress` 事件只能处理字符键，无法拦截功能键（如 Delete、Home、End），这些键需要用 `KeyDown` 事件处理。
- 在使用 `KeyPress` 做输入限制时，**务必保留控制字符**（`char.IsControl`），否则用户无法使用 Backspace 删除字符。
- 日志 TextBox 长时间运行后内存可能膨胀，建议定期清理旧日志（如保留最近 500~1000 行）。
- `ErrorProvider` 组件可以在控件旁边显示红色感叹号和错误提示文字，是 WinForms 中标准的输入验证提示方式。

### 练习建议

1. 创建一个「端口号」输入框，使用 `KeyPress` 事件限制只能输入数字。
2. 创建一个日志显示区域，实现 `LogMessage` 方法，自动追加时间戳并滚动到底部。
3. 实现一个 PLC 地址输入框，输入时实时验证格式是否合法，不合法时显示红色背景和错误提示。

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| 无法输入退格键删除文字 | KeyPress 事件中拦截了所有非数字字符，未放行控制字符 | 添加 `char.IsControl(e.KeyChar)` 判断 |
| 日志区内存占用越来越大 | AppendText 不清理旧内容 | 定期截断文本行数 |
| TextChanged 事件中修改 Text 导致死循环 | 在事件处理中又修改了 Text 属性 | 使用标志位防止重入，或使用 `RemoveHandler` 临时取消事件 |

---

## 5. Label 标签

### 知识讲解

`Label` 控件用于显示静态文字或动态状态信息，是不可交互的文本展示控件。在上位机中常用于显示标题、设备名称、状态标签、数值等。

**常用属性：**

| 属性 | 说明 |
|------|------|
| `Text` | 显示的文本 |
| `ForeColor` | 文字颜色 |
| `BackColor` | 背景颜色 |
| `TextAlign` | 对齐方式（`TopLeft`、`TopCenter`、`MiddleCenter` 等） |
| `AutoSize` | 是否自动调整大小以适应文字 |
| `BorderStyle` | 边框样式（`None`、`FixedSingle`、`Fixed3D`） |
| `Font` | 字体设置 |
| `Image` / `ImageAlign` | 图片及对齐方式 |

### 代码示例

```csharp
public partial class LabelDemoForm : Form
{
    public LabelDemoForm()
    {
        InitializeComponent();
    }

    // ========== 设备状态标签 ==========
    private void UpdateDeviceStatus(bool isConnected, string deviceName)
    {
        // 标题标签
        lblDeviceName.Text = deviceName;
        lblDeviceName.Font = new Font("微软雅黑", 12, FontStyle.Bold);
        lblDeviceName.TextAlign = ContentAlignment.MiddleLeft;

        // 状态标签 —— 根据连接状态改变颜色
        if (isConnected)
        {
            lblStatus.Text = "● 已连接";
            lblStatus.ForeColor = Color.FromArgb(0, 180, 0);   // 绿色
        }
        else
        {
            lblStatus.Text = "● 已断开";
            lblStatus.ForeColor = Color.FromArgb(220, 50, 50);  // 红色
        }
    }

    // ========== 动态数值显示标签 ==========
    private void UpdateTemperatureLabel(double temperature)
    {
        lblTemperature.Text = $"温度：{temperature:F1} °C";

        // 根据温度范围变色（工业报警用）
        if (temperature < 30)
            lblTemperature.ForeColor = Color.Green;       // 正常：绿色
        else if (temperature < 60)
            lblTemperature.ForeColor = Color.Orange;      // 警告：橙色
        else
            lblTemperature.ForeColor = Color.Red;          // 报警：红色

        // 也可以通过背景色强调
        if (temperature >= 80)
        {
            lblTemperature.BackColor = Color.FromArgb(255, 200, 200); // 浅红背景
        }
        else
        {
            lblTemperature.BackColor = Color.Transparent;
        }
    }

    // ========== 创建状态指示标签（模拟 LED 灯）==========
    private Label CreateStatusLabel(string text, Color color)
    {
        var label = new Label
        {
            Text = text,
            Font = new Font("微软雅黑", 10),
            ForeColor = color,
            BackColor = Color.Transparent,
            AutoSize = true,
            Margin = new Padding(5)
        };
        return label;
    }
}
```

### 注意事项

- `AutoSize = true`（默认值）时，标签大小会自动适应文字长度。如果需要标签保持固定大小，应设置 `AutoSize = false` 并配合 `TextAlign` 控制对齐。
- Label 可以通过设置 `BorderStyle = BorderStyle.FixedSingle` 来添加边框，在上位机中常用于模拟数据面板的效果。
- 动态更新 Label 颜色时，建议使用 `Color.FromArgb()` 指定自定义颜色，而不是使用预设颜色，以获得更专业的视觉效果。

### 练习建议

1. 创建一组状态标签（运行中、停止、故障），用不同颜色区分状态。
2. 实现一个温度显示标签，温度低于 30 度显示绿色，30~60 度显示橙色，60 度以上显示红色。
3. 使用 Label 模拟一个简单的仪表数值面板（显示电压、电流、功率）。

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| 标签文字被截断 | `AutoSize=false` 且容器太小 | 设置 `AutoSize=true` 或增大容器 |
| 颜色在深色背景上看不清 | 未设置合适的 `ForeColor` | 根据 `BackColor` 选择对比色 |

---

## 6. ComboBox 下拉框

### 知识讲解

`ComboBox` 是一个组合控件，结合了文本框和下拉列表的功能，用于从多个选项中选择一个值。在上位机中常用于选择 PLC 型号、通信协议、波特率、数据格式等。

**常用属性：**

| 属性 | 说明 |
|------|------|
| `Items` | 下拉选项集合 |
| `SelectedIndex` | 当前选中项的索引（-1 表示未选中） |
| `SelectedItem` | 当前选中项对象 |
| `SelectedText` | 选中项的文本 |
| `DropDownStyle` | 下拉样式（`DropDown`、`DropDownList`、`Simple`） |
| `Sorted` | 是否排序 |
| `Text` | 显示的文本 |

**DropDownStyle 枚举说明：**

| 值 | 说明 |
|----|------|
| `DropDown` | 可编辑 + 可下拉（默认） |
| `DropDownList` | 只能选择，不可手动输入 |
| `Simple` | 始终显示列表，可编辑 |

**常用事件：**

| 事件 | 说明 |
|------|------|
| `SelectedIndexChanged` | 选中项改变时触发 |
| `SelectedValueChanged` | 选中值改变时触发 |
| `TextChanged` | 文本改变时触发 |

### 代码示例

```csharp
public partial class ComboBoxDemoForm : Form
{
    public ComboBoxDemoForm()
    {
        InitializeComponent();
        InitializeComboBoxes();
    }

    private void InitializeComboBoxes()
    {
        // ========== 方式一：设计时添加项 ==========
        // 在属性窗口中编辑 Items 集合即可

        // ========== 方式二：代码添加项 ==========
        // PLC 型号选择
        cmbPlcType.Items.Clear();
        cmbPlcType.Items.AddRange(new object[]
        {
            "汇川 INOVANCE - IN100",
            "汇川 INOVANCE - IN200",
            "汇川 INOVANCE - IN300",
            "西门子 S7-1200",
            "西门子 S7-1500",
            "三菱 FX5U",
            "欧姆龙 NJ/NX"
        });
        cmbPlcType.SelectedIndex = 0;  // 默认选中第一项

        // 波特率选择
        cmbBaudRate.Items.AddRange(new object[]
        {
            9600, 19200, 38400, 57600, 115200
        });
        cmbBaudRate.SelectedIndex = 0;

        // ========== 方式三：绑定数据源（推荐） ==========
        // 适合选项较多或需要动态更新的场景
        BindDeviceList();
    }

    // ========== 绑定数据源示例 ==========
    private void BindDeviceList()
    {
        // 创建数据列表
        var devices = new List<PlcDeviceInfo>
        {
            new PlcDeviceInfo { Id = 1, Name = "1#PLC-主站", Ip = "192.168.1.10" },
            new PlcDeviceInfo { Id = 2, Name = "2#PLC-从站1", Ip = "192.168.1.11" },
            new PlcDeviceInfo { Id = 3, Name = "3#PLC-从站2", Ip = "192.168.1.12" },
            new PlcDeviceInfo { Id = 4, Name = "4#PLC-温控", Ip = "192.168.1.20" },
        };

        // 绑定数据源
        cmbDeviceList.DisplayMember = "Name";  // 显示的文本字段
        cmbDeviceList.ValueMember = "Id";        // 实际值的字段
        cmbDeviceList.DataSource = devices;       // 绑定数据
    }

    // ========== SelectedIndexChanged 事件 ==========
    private void cmbPlcType_SelectedIndexChanged(object sender, EventArgs e)
    {
        // 注意：绑定数据源时，初始化也会触发此事件
        // 需要检查 SelectedIndex 是否有效
        if (cmbPlcType.SelectedIndex < 0) return;

        string selectedPlc = cmbPlcType.SelectedItem.ToString();
        LogMessage($"已选择 PLC 型号：{selectedPlc}");

        // 根据选择的 PLC 型号，动态更新相关配置
        UpdateConfigForPlcType(selectedPlc);
    }

    // ========== 获取选中的值 ==========
    private void btnConfirm_Click(object sender, EventArgs e)
    {
        // 获取显示文本
        string plcType = cmbPlcType.Text;

        // 获取选中索引
        int index = cmbPlcType.SelectedIndex;

        // 获取绑定数据源的 ValueMember 值
        if (cmbDeviceList.SelectedValue != null)
        {
            int deviceId = (int)cmbDeviceList.SelectedValue;
            LogMessage($"选中设备 ID：{deviceId}");
        }
    }

    // ========== 动态刷新下拉列表（重新绑定数据源）==========
    private void RefreshDeviceList()
    {
        // 先暂存当前选中值
        int? currentId = cmbDeviceList.SelectedValue as int?;

        // 重新获取数据
        var devices = PlcManager.GetAllDevices();

        // 重新绑定
        cmbDeviceList.DataSource = null;          // 先清空（重要！）
        cmbDeviceList.DisplayMember = "Name";
        cmbDeviceList.ValueMember = "Id";
        cmbDeviceList.DataSource = devices;

        // 恢复之前的选中项
        if (currentId.HasValue)
        {
            cmbDeviceList.SelectedValue = currentId.Value;
        }
    }
}

// ========== 数据模型类 ==========
public class PlcDeviceInfo
{
    public int Id { get; set; }
    public string Name { get; set; }
    public string Ip { get; set; }
}
```

### 注意事项

- **绑定数据源时先设置 `DataSource = null`**：重新绑定数据源前必须先清空，否则可能出现显示异常。
- **SelectedIndexChanged 在初始化时也会触发**：绑定数据源后，`SelectedIndexChanged` 事件会被自动触发一次，需要在事件处理方法中添加 `SelectedIndex < 0` 的判断。
- **`DropDownStyle = DropDownList`** 是工业软件中最常用的设置，防止用户手动输入不合法的值。
- `DisplayMember` 和 `ValueMember` 必须在设置 `DataSource` 之前或之后配套设置，推荐先清空再重新设置的顺序。

### 练习建议

1. 创建一个 ComboBox，绑定一个自定义类的列表（如设备列表），显示设备名称，获取设备 ID。
2. 实现 PLC 型号切换后，自动更新下方的配置区域（如波特率、数据位等）。
3. 动态刷新下拉列表，刷新后保持用户之前的选中项不变。

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| 重新绑定后选中项显示空白 | 未先设置 `DataSource = null` | 绑定前先清空数据源 |
| 选中项索引为 -1 | 清空了 Items 或未选中任何项 | 在事件处理中检查 `SelectedIndex >= 0` |
| 初始化时触发多余的事件处理 | 绑定数据源会触发 SelectedIndexChanged | 添加 `if (cmb.SelectedIndex < 0) return;` |
| SelectedValue 为 null | ValueMember 设置的字段不存在 | 检查 ValueMember 属性名是否与类属性名一致 |

---

## 7. CheckBox 与 RadioButton

### 知识讲解

**CheckBox**（复选框）用于多选场景，允许用户同时选中多个选项。

**RadioButton**（单选按钮）用于单选场景，同一容器内只能选中一个。

在工业上位机中，CheckBox 常用于功能开关（如「自动采集」「报警提示」），RadioButton 常用于互斥配置（如「Modbus TCP」「Modbus RTU」）。

### 代码示例

```csharp
public partial class CheckRadioDemoForm : Form
{
    public CheckBox chkAutoCollect;      // 自动采集
    public CheckBox chkAlarm;            // 报警提示
    public CheckBox chkLogRecord;        // 记录日志
    public RadioButton rdoModbusTcp;     // Modbus TCP
    public RadioButton rdoModbusRtu;    // Modbus RTU
    public RadioButton rdoCustom;        // 自定义协议

    public CheckRadioDemoForm()
    {
        InitializeComponent();
        InitializeControls();
    }

    private void InitializeControls()
    {
        // ========== CheckBox 初始化 ==========
        chkAutoCollect = new CheckBox
        {
            Text = "自动采集数据",
            Checked = false,                      // 默认不选中
            Location = new Point(20, 20),
            Font = new Font("微软雅黑", 9)
        };

        chkAlarm = new CheckBox
        {
            Text = "数据超限时弹出报警",
            Checked = true,                       // 默认选中
            Location = new Point(20, 50)
        };

        chkLogRecord = new CheckBox
        {
            Text = "记录运行日志",
            Checked = true,
            Location = new Point(20, 80)
        };

        this.Controls.AddRange(new Control[] { chkAutoCollect, chkAlarm, chkLogRecord });

        // ========== RadioButton 初始化 ==========
        // 重要：RadioButton 的分组通过容器（GroupBox/Panel）实现
        // 同一容器内的 RadioButton 自动互斥

        // 创建协议选择分组
        var grpProtocol = new GroupBox
        {
            Text = "通信协议",
            Location = new Point(20, 130),
            Size = new Size(200, 120)
        };

        rdoModbusTcp = new RadioButton
        {
            Text = "Modbus TCP",
            Checked = true,                       // 默认选中 TCP
            Location = new Point(10, 30)
        };
        rdoModbusRtu = new RadioButton
        {
            Text = "Modbus RTU",
            Location = new Point(10, 60)
        };
        rdoCustom = new RadioButton
        {
            Text = "自定义协议",
            Location = new Point(10, 90)
        };

        // 将所有 RadioButton 添加到同一个 GroupBox 中
        grpProtocol.Controls.AddRange(new Control[] 
        { 
            rdoModbusTcp, rdoModbusRtu, rdoCustom 
        });
        this.Controls.Add(grpProtocol);
    }

    // ========== CheckBox CheckedChanged 事件 ==========
    private void chkAutoCollect_CheckedChanged(object sender, EventArgs e)
    {
        if (chkAutoCollect.Checked)
        {
            StartAutoCollect();  // 开始自动采集
            LogMessage("自动采集已开启");
        }
        else
        {
            StopAutoCollect();   // 停止自动采集
            LogMessage("自动采集已关闭");
        }
    }

    private void chkAlarm_CheckedChanged(object sender, EventArgs e)
    {
        bool enable = chkAlarm.Checked;
        // 根据报警开关状态，启用或禁用相关控件
        nudAlarmThreshold.Enabled = enable;
        lblAlarmThreshold.Enabled = enable;
    }

    // ========== RadioButton CheckedChanged 事件 ==========
    private void rdoProtocol_CheckedChanged(object sender, EventArgs e)
    {
        // RadioButton 的 CheckedChanged 事件：每个 RadioButton 都会触发
        // 需要只处理被选中的那个
        var radio = (RadioButton)sender;
        if (!radio.Checked) return;  // 忽略取消选中的事件

        switch (radio.Name)
        {
            case nameof(rdoModbusTcp):
                LogMessage("协议切换为 Modbus TCP");
                ShowTcpSettings();     // 显示 TCP 相关设置
                break;
            case nameof(rdoModbusRtu):
                LogMessage("协议切换为 Modbus RTU");
                ShowRtuSettings();     // 显示 RTU 相关设置
                break;
            case nameof(rdoCustom):
                LogMessage("协议切换为自定义协议");
                ShowCustomSettings();  // 显示自定义设置
                break;
        }
    }

    // ========== 读取所有 CheckBox 的状态 ==========
    private void GetCheckBoxStates()
    {
        var config = new
        {
            AutoCollect = chkAutoCollect.Checked,
            EnableAlarm = chkAlarm.Checked,
            EnableLog = chkLogRecord.Checked
        };
        
        LogMessage($"配置：自动采集={config.AutoCollect}, 报警={config.EnableAlarm}, 日志={config.EnableLog}");
    }
}
```

### 注意事项

- **RadioButton 分组**：RadioButton 的互斥是通过**容器**实现的。同一个 `GroupBox`、`Panel` 或 `Form` 内的 RadioButton 自动互斥。如果想要两组独立的单选，必须分别放在两个不同的容器中。
- **CheckedChanged 对所有状态变化都触发**：无论是从选中变为未选中，还是从未选中变为选中，都会触发 `CheckedChanged`。在事件处理中通常需要先检查 `Checked` 属性。
- **CheckBox 的 ThreeState 属性**：设置 `ThreeState = true` 后，CheckBox 有三种状态（选中、未选中、不确定），通过 `CheckState` 属性访问。

### 练习建议

1. 创建一个设置面板，使用 CheckBox 实现「自动采集」「报警提示」「数据记录」三个功能开关，每个开关改变时输出日志。
2. 使用 GroupBox + RadioButton 实现两组独立的单选：通信协议（TCP/RTU）和数据格式（十进制/十六进制/浮点）。
3. 实现 CheckBox 状态变化时，联动启用/禁用相关的设置控件。

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| 两个 GroupBox 中的 RadioButton 互相影响 | RadioButton 被放在了同一个父容器中 | 确保不同组的 RadioButton 放在不同容器中 |
| CheckedChanged 触发了两次 | 切换 RadioButton 时，旧的和新的都会触发事件 | 在事件处理中检查 `Checked` 属性 |

---

## 8. NumericUpDown 数字输入

### 知识讲解

`NumericUpDown` 是一个专用的数值输入控件，自带上下箭头按钮，可以精确控制输入范围和步长。在上位机中常用于设置端口号、超时时间、采集频率、报警阈值等数值参数。

**常用属性：**

| 属性 | 说明 |
|------|------|
| `Value` | 当前值（decimal 类型） |
| `Minimum` / `Maximum` | 最小值 / 最大值 |
| `Increment` | 步长（每次点击箭头变化的值） |
| `DecimalPlaces` | 小数位数 |
| `ThousandsSeparator` | 是否显示千分位分隔符 |
| `Hexadecimal` | 是否以十六进制显示 |
| `ReadOnly` | 是否只读 |
| `InterceptArrowKeys` | 是否拦截上下箭头键 |

**常用事件：**

| 事件 | 说明 |
|------|------|
| `ValueChanged` | 值改变时触发 |

### 代码示例

```csharp
public partial class NumericUpDownDemoForm : Form
{
    public NumericUpDownDemoForm()
    {
        InitializeComponent();
        InitializeNumericControls();
    }

    private void InitializeNumericControls()
    {
        // ========== 端口号：整数，范围 1~65535，步长 1 ==========
        nudPort.Minimum = 1;
        nudPort.Maximum = 65535;
        nudPort.Value = 502;        // Modbus 默认端口
        nudPort.Increment = 1;
        nudPort.DecimalPlaces = 0;

        // ========== 采集周期：浮点数，范围 0.1~60 秒，步长 0.1 ==========
        nudInterval.Minimum = 0.1m;
        nudInterval.Maximum = 60m;
        nudInterval.Value = 1.0m;
        nudInterval.Increment = 0.1m;
        nudInterval.DecimalPlaces = 1;
        nudInterval.ThousandsSeparator = false;

        // ========== 报警温度阈值：整数，范围 0~200，步长 5 ==========
        nudAlarmTemp.Minimum = 0;
        nudAlarmTemp.Maximum = 200;
        nudAlarmTemp.Value = 80;
        nudAlarmTemp.Increment = 5;
        nudAlarmTemp.DecimalPlaces = 0;

        // ========== 寄存器地址：使用十六进制显示 ==========
        nudAddress.Minimum = 0;
        nudAddress.Maximum = 65535;
        nudAddress.Value = 100;
        nudAddress.Hexadecimal = true;   // 十六进制显示
        nudAddress.Increment = 1;
    }

    // ========== ValueChanged 事件 ==========
    private void nudInterval_ValueChanged(object sender, EventArgs e)
    {
        // 采集周期改变时，更新定时器
        int intervalMs = (int)(nudInterval.Value * 1000);
        timerPoll.Interval = intervalMs;
        LogMessage($"采集周期已更新为 {nudInterval.Value:F1} 秒");
    }

    private void nudAlarmTemp_ValueChanged(object sender, EventArgs e)
    {
        // 更新报警阈值
        int threshold = (int)nudAlarmTemp.Value;
        lblAlarmThreshold.Text = $"报警阈值：{threshold}°C";
        
        // 检查当前温度是否超过新阈值
        CheckAlarmCondition(threshold);
    }

    // ========== 读取和设置值 ==========
    private void btnApply_Click(object sender, EventArgs e)
    {
        // 读取值（注意 Value 是 decimal 类型）
        int port = (int)nudPort.Value;
        double interval = (double)nudInterval.Value;
        int alarmTemp = (int)nudAlarmTemp.Value;

        LogMessage($"配置应用：端口={port}, 周期={interval}s, 阈值={alarmTemp}°C");
        
        // 应用配置...
        ApplyConfiguration(port, interval, alarmTemp);
    }

    // ========== 设置值时的注意事项 ==========
    private void LoadConfiguration()
    {
        // 从配置文件加载值时，需要在 Minimum~Maximum 范围内
        int savedPort = ConfigManager.GetPort();  // 假设读取到 502

        // 方式一：直接赋值（会自动 clamp 到范围内）
        nudPort.Value = savedPort;

        // 方式二：先检查范围再赋值
        if (savedPort >= nudPort.Minimum && savedPort <= nudPort.Maximum)
        {
            nudPort.Value = savedPort;
        }
        else
        {
            nudPort.Value = nudPort.Minimum;  // 超出范围则设为最小值
            MessageBox.Show($"端口号 {savedPort} 超出范围，已设为默认值", "警告");
        }
    }
}
```

### 注意事项

- `Value` 属性类型是 `decimal`，不是 `int` 或 `double`。使用时需要根据场景进行类型转换。
- 直接给 `Value` 赋一个超出 `Minimum`~`Maximum` 范围的值会抛出 `ArgumentOutOfRangeException`。建议先检查范围再赋值。
- `Hexadecimal = true` 只是改变了**显示方式**，`Value` 仍然是十进制的 `decimal` 类型。
- `Increment` 步长建议根据场景合理设置（如温度设为 0.5，端口号设为 1）。

### 练习建议

1. 创建一个设置面板，包含端口号（1~65535）、采集周期（0.1~60s，步长 0.1）、重连次数（1~10）三个数值输入控件。
2. 设置 `Hexadecimal = true`，创建一个寄存器地址输入框（十六进制显示）。
3. 实现 ValueChanged 事件联动：修改采集周期时，同步更新 Timer 的 Interval。

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| ArgumentOutOfRangeException | 赋值超出 Minimum~Maximum 范围 | 赋值前检查范围 |
| 值显示不正确 | 忘记设置 DecimalPlaces | 根据需要设置小数位数 |
| Value 是 decimal 报编译错误 | 直接赋值给 int/double 未转换 | 使用 `(int)nud.Value` 或 `(double)nud.Value` |

---

## 9. PictureBox 图片展示

### 知识讲解

`PictureBox` 用于显示图片，在上位机中常用于显示设备照片、状态图标、工艺流程图、实时监控画面等。

**常用属性：**

| 属性 | 说明 |
|------|------|
| `Image` | 显示的图片对象 |
| `SizeMode` | 图片显示模式 |
| `BorderStyle` | 边框样式 |
| `BackColor` | 背景色（图片加载前的底色） |

**SizeMode 枚举说明：**

| 值 | 说明 |
|----|------|
| `Normal` | 原始大小，超出部分裁剪 |
| `StretchImage` | 拉伸填满（可能变形） |
| `AutoSize` | 自动调整控件大小以适应图片 |
| `CenterImage` | 居中显示，不缩放 |
| `Zoom` | 等比缩放以适应控件（推荐） |

### 代码示例

```csharp
public partial class PictureBoxDemoForm : Form
{
    public PictureBoxDemoForm()
    {
        InitializeComponent();
        InitializePictureBox();
    }

    private void InitializePictureBox()
    {
        // ========== 基本设置 ==========
        picDeviceImage.SizeMode = PictureBoxSizeMode.Zoom;     // 等比缩放
        picDeviceImage.BorderStyle = BorderStyle.FixedSingle;
        picDeviceImage.BackColor = Color.FromArgb(240, 240, 240); // 加载前显示浅灰底
    }

    // ========== 加载图片的几种方式 ==========

    // 方式一：从文件加载
    private void LoadImageFromFile()
    {
        try
        {
            // 使用 using 确保释放文件资源
            using (var fs = new FileStream(@"Images\device_photo.png", FileMode.Open, FileAccess.Read))
            {
                picDeviceImage.Image = Image.FromStream(fs);
            }
        }
        catch (FileNotFoundException)
        {
            LogMessage("图片文件不存在");
            picDeviceImage.Image = null;  // 清空图片
        }
    }

    // 方式二：从项目资源加载（嵌入资源）
    private void LoadImageFromResource()
    {
        // 先将图片添加到项目资源（Resources.resx）
        picDeviceImage.Image = Properties.Resources.device_photo;
    }

    // 方式三：从网络 URL 加载
    private async void LoadImageFromUrl(string url)
    {
        try
        {
            using (var client = new HttpClient())
            {
                var bytes = await client.GetByteArrayAsync(url);
                using (var ms = new MemoryStream(bytes))
                {
                    picDeviceImage.Image = Image.FromStream(ms);
                }
            }
        }
        catch (Exception ex)
        {
            LogMessage($"加载网络图片失败：{ex.Message}");
        }
    }

    // ========== 动态更新状态图标 ==========
    private void UpdateDeviceStatusIcon(bool isOnline)
    {
        if (isOnline)
        {
            picStatus.Image = Properties.Resources.icon_online;    // 绿色在线图标
            picStatus.ToolTipText = "设备在线";
        }
        else
        {
            picStatus.Image = Properties.Resources.icon_offline;   // 红色离线图标
            picStatus.ToolTipText = "设备离线";
        }
    }

    // ========== 动态绘制实时数据到 PictureBox ==========
    private void DrawRealTimeData()
    {
        // 创建一个 Bitmap，在上面绘制自定义内容
        var bmp = new Bitmap(picCanvas.Width, picCanvas.Height);
        using (var g = Graphics.FromImage(bmp))
        {
            g.SmoothingMode = System.Drawing.Drawing2D.SmoothingMode.AntiAlias;

            // 绘制背景
            g.FillRectangle(Brushes.White, 0, 0, bmp.Width, bmp.Height);

            // 绘制网格
            using (var pen = new Pen(Color.FromArgb(200, 200, 200), 1))
            {
                for (int x = 0; x < bmp.Width; x += 50)
                    g.DrawLine(pen, x, 0, x, bmp.Height);
                for (int y = 0; y < bmp.Height; y += 50)
                    g.DrawLine(pen, 0, y, bmp.Width, y);
            }

            // 绘制数据点
            // ... 此处省略具体绘图代码，详见"自定义控件"章节

            // 更新到 PictureBox
            picCanvas.Image = bmp;
        }

        // 注意：旧的 Image 需要手动释放
        // 如果频繁更新，建议保留旧的引用并 Dispose
    }

    // ========== 释放旧的 Image 资源 ==========
    private Image _oldCanvasImage = null;

    private void UpdateCanvasSafe(Bitmap newBmp)
    {
        var old = _oldCanvasImage;   // 暂存旧图片
        picCanvas.Image = newBmp;     // 设置新图片
        _oldCanvasImage = newBmp;     // 记录当前图片

        // 释放旧图片（不能在 Image 赋值之后立即 Dispose 旧的，
        // 因为 PictureBox 可能还在使用它进行绘制）
        old?.Dispose();
    }
}
```

### 注意事项

- **文件锁定问题**：使用 `Image.FromFile()` 加载图片会导致文件被锁定，直到 `Image` 被 `Dispose`。推荐使用 `FileStream` 或 `MemoryStream` 加载以避免锁定。
- **GDI+ 资源泄漏**：`Image`、`Bitmap`、`Graphics` 等对象都实现了 `IDisposable`，必须及时释放，否则会导致内存泄漏。频繁更新图片时尤其要注意。
- **SizeMode 选择**：工业上位机中推荐使用 `Zoom`（等比缩放）或 `CenterImage`（居中不缩放），避免 `StretchImage` 导致图片变形。

### 练习建议

1. 实现从文件、资源、网络三种方式加载图片到 PictureBox。
2. 创建一个状态图标区域，根据设备在线/离线状态切换显示不同的图标。
3. 实现在 PictureBox 上动态绘制简单的实时数据（如一组随机数据点）。

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| 图片文件被占用无法删除 | 使用 `Image.FromFile()` 导致文件锁定 | 改用 `FileStream` + `Image.FromStream()` |
| 内存持续增长 | 频繁创建 Bitmap 未释放旧对象 | 释放旧的 Image 对象 |
| Image.FromStream 参数无效 | Stream 已关闭后再使用 | 确保在 Image 使用期间 Stream 保持打开 |

---

## 10. Timer 定时器

### 知识讲解

`Timer` 是 WinForms 中最常用的定时控件，按照设定的时间间隔周期性触发事件。在上位机开发中，Timer 的核心用途是**轮询 PLC 数据**——定时读取寄存器值并更新界面。

**常用属性：**

| 属性 | 说明 |
|------|------|
| `Interval` | 定时间隔（毫秒），范围 1~65535 |
| `Enabled` | 是否启动定时器 |
| `Tag` | 附加数据（可存储自定义对象） |

**常用方法：**

| 方法 | 说明 |
|------|------|
| `Start()` | 启动定时器（等同于 `Enabled = true`） |
| `Stop()` | 停止定时器（等同于 `Enabled = false`） |

**常用事件：**

| 事件 | 说明 |
|------|------|
| `Tick` | 每隔 Interval 毫秒触发一次 |

### 代码示例

```csharp
public partial class TimerDemoForm : Form
{
    // ========== 定时器声明 ==========
    private System.Windows.Forms.Timer timerPoll;    // PLC 数据轮询定时器
    private System.Windows.Forms.Timer timerHeartbeat; // 心跳检测定时器
    private System.Windows.Forms.Timer timerClock;     // 界面时钟定时器

    // ========== 统计计数器 ==========
    private int _pollCount = 0;       // 轮询次数
    private int _errorCount = 0;       // 错误次数

    public TimerDemoForm()
    {
        InitializeComponent();
        InitializeTimers();
    }

    private void InitializeTimers()
    {
        // ========== PLC 数据轮询定时器 ==========
        timerPoll = new System.Windows.Forms.Timer
        {
            Interval = 1000,   // 1000ms = 1秒，即每秒读取一次 PLC
            Enabled = false    // 初始状态不启动，等待用户点击"开始采集"后启动
        };
        timerPoll.Tick += TimerPoll_Tick;

        // ========== 心跳检测定时器 ==========
        timerHeartbeat = new System.Windows.Forms.Timer
        {
            Interval = 5000,   // 每 5 秒检测一次连接状态
            Enabled = false
        };
        timerHeartbeat.Tick += TimerHeartbeat_Tick;

        // ========== 界面时钟定时器 ==========
        timerClock = new System.Windows.Forms.Timer
        {
            Interval = 1000,   // 每秒更新一次时钟显示
            Enabled = true     // 界面时钟始终运行
        };
        timerClock.Tick += TimerClock_Tick;
    }

    // ========== PLC 数据轮询 Tick 事件（核心！）==========
    private async void TimerPoll_Tick(object sender, EventArgs e)
    {
        _pollCount++;

        try
        {
            // 更新状态栏：正在读取...
            toolStripStatusPoll.Text = $"轮询中... (第 {_pollCount} 次)";

            // ---- 读取 PLC 数据 ----
            // 注意：PLC 通信是网络/串口操作，需要异步执行
            // 否则会阻塞 UI 线程
            var readResults = await Task.Run(() =>
            {
                return PlcClient.ReadRegisters(
                    address: "D100",
                    count: 10
                );
            });

            // ---- 更新界面 ----
            UpdateDataDisplay(readResults);

            // 更新状态栏：读取成功
            toolStripStatusPoll.Text = 
                $"正常 | 轮询次数：{_pollCount} | 错误次数：{_errorCount} | 上次读取：{DateTime.Now:HH:mm:ss}";
        }
        catch (Exception ex)
        {
            _errorCount++;

            // 更新状态栏：读取失败
            toolStripStatusPoll.Text = 
                $"通信异常 | 错误次数：{_errorCount} | {ex.Message}";

            // 连续错误超过 5 次，自动停止
            if (_errorCount >= 5)
            {
                StopPolling();
                MessageBox.Show(
                    $"连续 {_errorCount} 次通信失败，已自动停止采集。\n请检查网络连接后重试。",
                    "通信异常",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Warning
                );
            }
        }
    }

    // ========== 心跳检测 Tick 事件 ==========
    private void TimerHeartbeat_Tick(object sender, EventArgs e)
    {
        bool isConnected = PlcClient.CheckConnection();

        if (!isConnected && timerPoll.Enabled)
        {
            // 连接断开，停止轮询
            StopPolling();
            LogMessage("心跳检测：连接已断开，轮询已停止");
            MessageBox.Show("PLC 连接已断开，请重新连接。", "连接断开",
                MessageBoxButtons.OK, MessageBoxIcon.Warning);
        }
    }

    // ========== 界面时钟 Tick 事件 ==========
    private void TimerClock_Tick(object sender, EventArgs e)
    {
        lblCurrentTime.Text = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
    }

    // ========== 开始/停止采集按钮 ==========
    private void btnStartPoll_Click(object sender, EventArgs e)
    {
        StartPolling();
    }

    private void btnStopPoll_Click(object sender, EventArgs e)
    {
        StopPolling();
    }

    private void StartPolling()
    {
        if (!PlcClient.IsConnected)
        {
            MessageBox.Show("请先连接 PLC", "提示");
            return;
        }

        _pollCount = 0;
        _errorCount = 0;
        timerPoll.Start();          // 启动轮询定时器
        timerHeartbeat.Start();      // 启动心跳检测

        btnStartPoll.Enabled = false;
        btnStopPoll.Enabled = true;
        LogMessage("数据采集已启动");
    }

    private void StopPolling()
    {
        timerPoll.Stop();           // 停止轮询定时器
        timerHeartbeat.Stop();      // 停止心跳检测

        btnStartPoll.Enabled = true;
        btnStopPoll.Enabled = false;
        toolStripStatusPoll.Text = "已停止";
        LogMessage($"数据采集已停止（共 {_pollCount} 次轮询，{_errorCount} 次错误）");
    }

    // ========== 动态调整轮询周期 ==========
    private void nudInterval_ValueChanged(object sender, EventArgs e)
    {
        int newInterval = (int)(nudInterval.Value * 1000);
        
        // 直接修改 Interval，无需重启定时器
        timerPoll.Interval = newInterval;
        LogMessage($"轮询周期已调整为 {nudInterval.Value:F1} 秒");
    }
}
```

### 注意事项

- **Timer 在 UI 线程上触发 Tick 事件**：`System.Windows.Forms.Timer` 的 Tick 事件在主线程（UI 线程）上执行，因此可以直接更新界面控件，但**不能在 Tick 中执行耗时操作**，否则界面会卡顿。
- **轮询中的耗时操作必须异步**：如果轮询需要网络通信（如读 PLC），应在 Tick 中使用 `async/await` + `Task.Run` 将通信操作放到线程池中执行。
- **Interval 最小值**：虽然可以设为 1ms，但实际上受限于系统的定时器精度（约 15ms），且不建议设置过快。上位机轮询 PLC 通常设为 100ms~5000ms。
- **定时器的生命周期**：在窗体关闭时应停止所有定时器（在 `FormClosing` 中调用 `Stop()`），否则定时器会继续触发事件，可能导致访问已释放的资源。

### 练习建议

1. 创建一个每秒触发一次的 Timer，在状态栏显示当前时间和已触发的次数。
2. 实现一个带有「开始」「停止」按钮的数据轮询面板，Timer 间隔可通过 NumericUpDown 调整。
3. 添加错误计数和连续错误自动停止的功能。

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| Timer 关窗后仍触发异常 | 窗体关闭时未停止定时器 | 在 FormClosing 中调用 `timer.Stop()` |
| 界面卡顿 | Tick 事件中执行了耗时操作 | 使用 `async/await` + `Task.Run` |
| 修改 Interval 不生效 | 修改后未调用 timer 的刷新方法 | 直接赋值 `timer.Interval = newValue` 即可，无需重启 |
| Tick 事件未触发 | `Enabled = false` 或忘记订阅事件 | 检查 `Enabled` 和事件绑定 |

---

## 11. 综合案例：PLC 地址读写测试面板

### 知识讲解

本案例将前面学习的所有基础控件整合在一起，创建一个完整的 PLC 地址读写测试面板。该面板包含：连接设置、地址输入、读写操作、数据展示、日志输出等功能，是工业上位机中最基础也最常见的界面形态。

### 代码示例

```csharp
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Windows.Forms;

namespace PlcMonitorApp
{
    /// <summary>
    /// PLC 地址读写测试面板
    /// 综合运用：Form、Button、TextBox、Label、ComboBox、CheckBox、
    ///           NumericUpDown、PictureBox、Timer 等基础控件
    /// </summary>
    public partial class PlcTestPanel : Form
    {
        #region ===== 私有字段 =====

        private System.Windows.Forms.Timer _timerAutoPoll; // 自动轮询定时器
        private int _readCount = 0;                          // 读取计数
        private int _writeCount = 0;                         // 写入计数
        private PlcSimulator _plcSimulator;                // PLC 模拟器（替代真实 PLC）

        #endregion

        public PlcTestPanel()
        {
            InitializeComponent();
            InitializeForm();
            InitializeControls();
            InitializeTimer();
            _plcSimulator = new PlcSimulator(); // 初始化模拟器
        }

        // ========== 窗体初始化 ==========
        private void InitializeForm()
        {
            this.Text = "PLC 地址读写测试面板";
            this.Size = new Size(900, 650);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedSingle;
            this.MaximizeBox = false;
            this.Font = new Font("微软雅黑", 9);
        }

        // ========== 控件初始化 ==========
        private void InitializeControls()
        {
            // 此处展示代码初始化方式，实际项目中建议使用设计器拖拽

            // ---------- 连接设置区域 ----------
            var grpConnection = new GroupBox
            {
                Text = "连接设置",
                Location = new Point(10, 10),
                Size = new Size(260, 140),
                Font = new Font("微软雅黑", 9, FontStyle.Bold)
            };

            // PLC IP 地址
            var lblIp = new Label { Text = "IP 地址：", Location = new Point(10, 30), AutoSize = true };
            var txtIp = new TextBox
            {
                Name = "txtIpAddress",
                Text = "192.168.1.10",
                Location = new Point(80, 27),
                Size = new Size(160, 23)
            };
            txtIp.KeyPress += (s, e) =>
            {
                if (!char.IsControl(e.KeyChar) && !char.IsDigit(e.KeyChar) && e.KeyChar != '.')
                    e.Handled = true;
            };

            // 端口号
            var lblPort = new Label { Text = "端  口：", Location = new Point(10, 60), AutoSize = true };
            var nudPort = new NumericUpDown
            {
                Name = "nudPort",
                Minimum = 1, Maximum = 65535, Value = 502,
                Location = new Point(80, 57), Size = new Size(100, 23),
                DecimalPlaces = 0
            };

            // 连接/断开按钮
            var btnConnect = new Button
            {
                Name = "btnConnect",
                Text = "连接",
                Location = new Point(10, 95), Size = new Size(115, 30),
                BackColor = Color.FromArgb(0, 153, 255),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btnConnect.Click += BtnConnect_Click;

            var btnDisconnect = new Button
            {
                Name = "btnDisconnect",
                Text = "断开",
                Location = new Point(135, 95), Size = new Size(115, 30),
                BackColor = Color.FromArgb(200, 200, 200),
                Enabled = false
            };
            btnDisconnect.Click += BtnDisconnect_Click;

            // 状态指示
            var picStatus = new PictureBox
            {
                Name = "picStatus",
                Size = new Size(16, 16),
                Location = new Point(235, 100),
                SizeMode = PictureBoxSizeMode.StretchImage,
                BackColor = Color.Red
            };
            var lblConnStatus = new Label
            {
                Name = "lblConnStatus",
                Text = "未连接",
                Location = new Point(200, 122),
                ForeColor = Color.Red,
                AutoSize = true,
                Font = new Font("微软雅黑", 8)
            };

            grpConnection.Controls.AddRange(new Control[]
            {
                lblIp, txtIp, lblPort, nudPort,
                btnConnect, btnDisconnect,
                picStatus, lblConnStatus
            });

            // ---------- 读写操作区域 ----------
            var grpReadWrite = new GroupBox
            {
                Text = "地址读写",
                Location = new Point(10, 160),
                Size = new Size(260, 260),
                Font = new Font("微软雅黑", 9, FontStyle.Bold)
            };

            // 寄存器地址
            var lblAddr = new Label { Text = "寄存器地址：", Location = new Point(10, 30), AutoSize = true };
            var txtAddr = new TextBox
            {
                Name = "txtAddress",
                Text = "D100",
                Location = new Point(110, 27),
                Size = new Size(130, 23)
            };

            // 数据类型
            var lblType = new Label { Text = "数据类型：", Location = new Point(10, 60), AutoSize = true };
            var cmbType = new ComboBox
            {
                Name = "cmbDataType",
                DropDownStyle = ComboBoxStyle.DropDownList,
                Location = new Point(110, 57),
                Size = new Size(130, 23)
            };
            cmbType.Items.AddRange(new object[] { "Int16", "UInt16", "Float32", "Bool" });
            cmbType.SelectedIndex = 0;

            // 写入值
            var lblWriteVal = new Label { Text = "写入值：", Location = new Point(10, 90), AutoSize = true };
            var txtWriteVal = new TextBox
            {
                Name = "txtWriteValue",
                Text = "0",
                Location = new Point(110, 87),
                Size = new Size(130, 23)
            };

            // 读取按钮
            var btnRead = new Button
            {
                Name = "btnRead",
                Text = "读取",
                Location = new Point(10, 130), Size = new Size(120, 35),
                BackColor = Color.FromArgb(40, 167, 69),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btnRead.Click += BtnRead_Click;

            // 写入按钮
            var btnWrite = new Button
            {
                Name = "btnWrite",
                Text = "写入",
                Location = new Point(140, 130), Size = new Size(100, 35),
                BackColor = Color.FromArgb(255, 193, 7),
                ForeColor = Color.Black,
                FlatStyle = FlatStyle.Flat
            };
            btnWrite.Click += BtnWrite_Click;

            // 读取结果显示
            var lblReadResult = new Label { Text = "读取结果：", Location = new Point(10, 185), AutoSize = true };
            var txtReadResult = new TextBox
            {
                Name = "txtReadResult",
                ReadOnly = true,
                Location = new Point(110, 182),
                Size = new Size(130, 23),
                BackColor = Color.FromArgb(240, 248, 255),
                Font = new Font("Consolas", 11, FontStyle.Bold),
                ForeColor = Color.FromArgb(0, 100, 200)
            };

            // 自动轮询
            var chkAutoPoll = new CheckBox
            {
                Name = "chkAutoPoll",
                Text = "自动轮询",
                Location = new Point(10, 220)
            };
            chkAutoPoll.CheckedChanged += ChkAutoPoll_CheckedChanged;

            // 轮询间隔
            var lblPollInterval = new Label { Text = "间隔(ms)：", Location = new Point(10, 248), AutoSize = true };
            var nudPollInterval = new NumericUpDown
            {
                Name = "nudPollInterval",
                Minimum = 100, Maximum = 10000, Value = 1000, Increment = 100,
                Location = new Point(110, 245), Size = new Size(80, 23)
            };
            nudPollInterval.ValueChanged += (s, e) =>
            {
                if (_timerAutoPoll != null)
                    _timerAutoPoll.Interval = (int)nudPollInterval.Value;
            };

            grpReadWrite.Controls.AddRange(new Control[]
            {
                lblAddr, txtAddr, lblType, cmbType,
                lblWriteVal, txtWriteVal,
                btnRead, btnWrite,
                lblReadResult, txtReadResult,
                chkAutoPoll, lblPollInterval, nudPollInterval
            });

            // ---------- 日志区域 ----------
            var grpLog = new GroupBox
            {
                Text = "运行日志",
                Location = new Point(280, 10),
                Size = new Size(600, 530),
                Font = new Font("微软雅黑", 9, FontStyle.Bold)
            };

            var txtLog = new TextBox
            {
                Name = "txtLog",
                Multiline = true,
                ReadOnly = true,
                ScrollBars = ScrollBars.Vertical,
                Location = new Point(10, 25),
                Size = new Size(580, 490),
                BackColor = Color.FromArgb(30, 30, 30),
                ForeColor = Color.FromArgb(200, 200, 200),
                Font = new Font("Consolas", 9)
            };

            grpLog.Controls.Add(txtLog);

            // ---------- 底部状态栏 ----------
            var statusStrip = new StatusStrip();

            var toolStripStatusLabel = new ToolStripStatusLabel("就绪");
            toolStripStatusLabel.Name = "toolStripStatus";
            toolStripStatusLabel.Spring = true;

            var toolStripReadCount = new ToolStripStatusLabel("读取：0");
            toolStripReadCount.Name = "toolStripReadCount";

            var toolStripWriteCount = new ToolStripStatusLabel("写入：0");
            toolStripWriteCount.Name = "toolStripWriteCount";

            statusStrip.Items.AddRange(new ToolStripItem[]
            {
                toolStripStatusLabel,
                toolStripReadCount,
                toolStripWriteCount
            });

            // 将所有控件添加到窗体
            this.Controls.Add(grpConnection);
            this.Controls.Add(grpReadWrite);
            this.Controls.Add(grpLog);
            this.Controls.Add(statusStrip);
        }

        // ========== 定时器初始化 ==========
        private void InitializeTimer()
        {
            _timerAutoPoll = new System.Windows.Forms.Timer
            {
                Interval = 1000,
                Enabled = false
            };
            _timerAutoPoll.Tick += async (s, e) =>
            {
                await AutoPollReadAsync();
            };
        }

        // ========== 事件处理方法 ==========

        /// <summary>
        /// 连接按钮点击事件
        /// </summary>
        private void BtnConnect_Click(object sender, EventArgs e)
        {
            try
            {
                string ip = GetControl<TextBox>("txtIpAddress").Text.Trim();
                int port = (int)GetControl<NumericUpDown>("nudPort").Value;

                // 模拟连接（实际项目替换为真实 PLC 连接代码）
                _plcSimulator.Connect(ip, port);

                // 更新界面状态
                GetControl<TextBox>("txtIpAddress").BackColor = Color.FromArgb(200, 255, 200);
                GetControl<PictureBox>("picStatus").BackColor = Color.Green;
                GetControl<Label>("lblConnStatus").Text = "已连接";
                GetControl<Label>("lblConnStatus").ForeColor = Color.Green;
                GetControl<Button>("btnConnect").Enabled = false;
                GetControl<Button>("btnDisconnect").Enabled = true;

                LogMessage($"已连接到 PLC：{ip}:{port}");
                UpdateStatus("已连接");
            }
            catch (Exception ex)
            {
                LogMessage($"连接失败：{ex.Message}");
                MessageBox.Show($"连接失败：{ex.Message}", "错误",
                    MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        /// <summary>
        /// 断开连接按钮点击事件
        /// </summary>
        private void BtnDisconnect_Click(object sender, EventArgs e)
        {
            // 停止自动轮询
            _timerAutoPoll.Stop();
            GetControl<CheckBox>("chkAutoPoll").Checked = false;

            // 断开连接
            _plcSimulator.Disconnect();

            // 更新界面状态
            GetControl<TextBox>("txtIpAddress").BackColor = Color.White;
            GetControl<PictureBox>("picStatus").BackColor = Color.Red;
            GetControl<Label>("lblConnStatus").Text = "未连接";
            GetControl<Label>("lblConnStatus").ForeColor = Color.Red;
            GetControl<Button>("btnConnect").Enabled = true;
            GetControl<Button>("btnDisconnect").Enabled = false;

            LogMessage("已断开 PLC 连接");
            UpdateStatus("已断开");
        }

        /// <summary>
        /// 读取按钮点击事件
        /// </summary>
        private async void BtnRead_Click(object sender, EventArgs e)
        {
            var btn = (Button)sender;
            btn.Enabled = false;
            btn.Text = "读取中...";

            try
            {
                string address = GetControl<TextBox>("txtAddress").Text.Trim();
                string dataType = GetControl<ComboBox>("cmbDataType").Text;

                // 模拟异步读取
                var result = await Task.Run(() => _plcSimulator.Read(address, dataType));

                GetControl<TextBox>("txtReadResult").Text = result.ToString();
                _readCount++;
                UpdateReadWriteCount();
                LogMessage($"读取成功：{address} = {result} ({dataType})");
                UpdateStatus("读取成功");
            }
            catch (Exception ex)
            {
                GetControl<TextBox>("txtReadResult").Text = "ERROR";
                LogMessage($"读取失败：{ex.Message}");
                UpdateStatus("读取失败");
            }
            finally
            {
                btn.Enabled = true;
                btn.Text = "读取";
            }
        }

        /// <summary>
        /// 写入按钮点击事件
        /// </summary>
        private async void BtnWrite_Click(object sender, EventArgs e)
        {
            var btn = (Button)sender;
            btn.Enabled = false;

            try
            {
                string address = GetControl<TextBox>("txtAddress").Text.Trim();
                string dataType = GetControl<ComboBox>("cmbDataType").Text;
                string writeValue = GetControl<TextBox>("txtWriteValue").Text.Trim();

                // 验证输入
                if (string.IsNullOrWhiteSpace(address))
                {
                    MessageBox.Show("请输入寄存器地址", "提示");
                    return;
                }

                // 模拟异步写入
                await Task.Run(() => _plcSimulator.Write(address, dataType, writeValue));

                _writeCount++;
                UpdateReadWriteCount();
                LogMessage($"写入成功：{address} = {writeValue} ({dataType})");
                UpdateStatus("写入成功");
            }
            catch (Exception ex)
            {
                LogMessage($"写入失败：{ex.Message}");
                UpdateStatus("写入失败");
            }
            finally
            {
                btn.Enabled = true;
            }
        }

        /// <summary>
        /// 自动轮询开关变化事件
        /// </summary>
        private void ChkAutoPoll_CheckedChanged(object sender, EventArgs e)
        {
            bool startPoll = GetControl<CheckBox>("chkAutoPoll").Checked;

            if (startPoll)
            {
                if (!_plcSimulator.IsConnected)
                {
                    GetControl<CheckBox>("chkAutoPoll").Checked = false;
                    MessageBox.Show("请先连接 PLC", "提示");
                    return;
                }
                _timerAutoPoll.Start();
                LogMessage("自动轮询已启动");
            }
            else
            {
                _timerAutoPoll.Stop();
                LogMessage("自动轮询已停止");
            }
        }

        /// <summary>
        /// 自动轮询读取
        /// </summary>
        private async System.Threading.Tasks.Task AutoPollReadAsync()
        {
            try
            {
                string address = GetControl<TextBox>("txtAddress").Text.Trim();
                string dataType = GetControl<ComboBox>("cmbDataType").Text;

                var result = await Task.Run(() => _plcSimulator.Read(address, dataType));

                GetControl<TextBox>("txtReadResult").Text = result.ToString();
                _readCount++;
                UpdateReadWriteCount();
            }
            catch
            {
                // 自动轮询中出错，仅更新状态，不弹窗
                GetControl<TextBox>("txtReadResult").Text = "ERROR";
            }
        }

        // ========== 辅助方法 ==========

        /// <summary>
        /// 获取指定名称的控件（泛型辅助方法）
        /// </summary>
        private T GetControl<T>(string name) where T : Control
        {
            return (T)this.Controls.Find(name, true).FirstOrDefault();
        }

        /// <summary>
        /// 向日志区追加消息
        /// </summary>
        private void LogMessage(string message)
        {
            var txtLog = GetControl<TextBox>("txtLog");
            if (txtLog == null) return;

            string timeStr = DateTime.Now.ToString("HH:mm:ss.fff");
            txtLog.AppendText($"[{timeStr}] {message}{Environment.NewLine}");

            // 自动滚动到底部
            txtLog.SelectionStart = txtLog.Text.Length;
            txtLog.ScrollToCaret();

            // 限制日志行数
            if (txtLog.Lines.Length > 500)
            {
                txtLog.Text = string.Join(Environment.NewLine,
                    txtLog.Lines.Skip(300).ToArray());
            }
        }

        /// <summary>
        /// 更新状态栏文字
        /// </summary>
        private void UpdateStatus(string status)
        {
            var toolStrip = this.Controls.OfType<StatusStrip>().FirstOrDefault();
            if (toolStrip?.Items["toolStripStatus"] is ToolStripStatusLabel label)
            {
                label.Text = status;
            }
        }

        /// <summary>
        /// 更新读写计数
        /// </summary>
        private void UpdateReadWriteCount()
        {
            var toolStrip = this.Controls.OfType<StatusStrip>().FirstOrDefault();
            if (toolStrip?.Items["toolStripReadCount"] is ToolStripStatusLabel readLabel)
                readLabel.Text = $"读取：{_readCount}";
            if (toolStrip?.Items["toolStripWriteCount"] is ToolStripStatusLabel writeLabel)
                writeLabel.Text = $"写入：{_writeCount}";
        }

        // ========== 窗体关闭事件 ==========
        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            // 停止定时器
            _timerAutoPoll?.Stop();

            // 断开连接
            _plcSimulator?.Disconnect();

            base.OnFormClosing(e);
        }
    }

    #region ===== PLC 模拟器（替代真实 PLC 通信）=====

    /// <summary>
    /// PLC 模拟器类 —— 用于演示，替代真实 PLC 通信
    /// 实际项目中替换为真实的 PLC 通信客户端
    /// </summary>
    public class PlcSimulator
    {
        private Random _random = new Random();
        private Dictionary<string, object> _registers = new Dictionary<string, object>();
        public bool IsConnected { get; private set; }

        public void Connect(string ip, int port)
        {
            // 模拟连接
            System.Threading.Thread.Sleep(500); // 模拟延迟
            IsConnected = true;
        }

        public void Disconnect()
        {
            IsConnected = false;
        }

        public object Read(string address, string dataType)
        {
            if (!IsConnected) throw new Exception("PLC 未连接");

            System.Threading.Thread.Sleep(100); // 模拟通信延迟

            // 模拟返回数据
            switch (dataType)
            {
                case "Int16":
                    return (short)_random.Next(-1000, 1001);
                case "UInt16":
                    return (ushort)_random.Next(0, 10001);
                case "Float32":
                    return Math.Round(_random.NextDouble() * 100, 2);
                case "Bool":
                    return _random.Next(2) == 1;
                default:
                    return 0;
            }
        }

        public void Write(string address, string dataType, string value)
        {
            if (!IsConnected) throw new Exception("PLC 未连接");

            System.Threading.Thread.Sleep(100); // 模拟通信延迟

            // 模拟写入
            _registers[address] = value;
        }
    }

    #endregion
}
```

### 注意事项

- 本案例使用了一个 `PlcSimulator` 模拟器来替代真实的 PLC 通信，实际项目中应替换为真实的 Modbus/S7 等通信客户端。
- 按钮的 `async void Click` 事件中，始终使用 `try-catch-finally` 结构，确保无论成功或失败都能恢复按钮状态。
- 日志区域有行数限制（500 行），防止长时间运行导致内存膨胀。
- 窗体关闭时在 `OnFormClosing` 中停止定时器和断开连接，确保资源正确释放。

### 练习建议

1. 在本案例的基础上，添加「批量读取」功能：可以一次读取多个连续地址的值，显示在 DataGridView 中。
2. 添加寄存器地址的格式验证（如 D100、M0.1 等），不合法时禁止操作。
3. 实现自动轮询时，在日志中每隔 10 次轮询输出一次统计信息（成功次数、失败次数、平均耗时）。

### 常见错误

| 错误现象 | 原因 | 解决方法 |
|---------|------|---------|
| 轮询期间界面卡顿 | Timer Tick 中执行了同步通信操作 | 使用 `async/await` + `Task.Run` |
| 关闭窗体报异常 | 定时器仍在触发事件但控件已释放 | 在 FormClosing 中停止定时器 |
| 日志区内存持续增长 | 未限制日志行数 | 定期截断日志文本 |

---

## 总结

本章学习了 WinForms 的基础控件，包括：

| 控件 | 核心用途 | 上位机典型场景 |
|------|---------|--------------|
| Form | 窗体容器 | 主界面、设置对话框 |
| Button | 触发操作 | 连接/断开、读取/写入、开始/停止 |
| TextBox | 文本输入/显示 | IP 地址、寄存器地址、日志显示 |
| Label | 状态显示 | 设备名称、连接状态、数据标签 |
| ComboBox | 选项选择 | PLC 型号、通信协议、数据类型 |
| CheckBox | 多选开关 | 自动采集、报警提示、日志记录 |
| RadioButton | 单选互斥 | 通信协议选择、数据格式选择 |
| NumericUpDown | 数值输入 | 端口号、采集周期、报警阈值 |
| PictureBox | 图片显示 | 设备照片、状态图标、数据图表 |
| Timer | 定时触发 | PLC 数据轮询、心跳检测、界面时钟 |

掌握这些基础控件的使用，是开发工业上位机界面的第一步。下一章将学习布局与容器控件，实现更专业的界面布局设计。
