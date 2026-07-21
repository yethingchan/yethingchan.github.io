# 09 · Windows 服务化部署

`java -jar` 前台运行一关命令行就停。生产需注册成 Windows 服务，开机自启、崩溃自启。

## 方案一：使用 winsw（推荐，轻量）

1. 下载 `WinSW-x64.exe`：https://github.com/winsw/winsw/releases
2. 改名 `admin-service.exe`，同目录放 `admin-service.xml`：

```xml
<service>
  <id>admin-backend</id>
  <name>Admin Backend Service</name>
  <description>Spring Boot 后台服务</description>
  <executable>java</executable>
  <arguments>-jar D:\deploy\backend\admin.jar</arguments>
  <workingdirectory>D:\deploy\backend</workingdirectory>
  <logpath>D:\deploy\logs</logpath>
  <logmode>roll</logmode>
</service>
```

3. 安装/启停：
   ```bat
   admin-service.exe install
   admin-service.exe start
   admin-service.exe stop
   ```

## 方案二：用 NSSM

```bat
nssm install AdminBackend "java" "-jar D:\deploy\backend\admin.jar"
nssm start AdminBackend
```

## 前端/Nginx

- Nginx 本身可注册服务（用 `nginx-service` 工具或 winsw），或保持手动 `start nginx` + 开机脚本。
- 前端是静态文件，Nginx 托管即"服务化"，无需单独守护。

## 开机自启

- 服务设"自动启动"（winsw/nssm 默认），重启服务器自动拉起后端。
- MySQL 安装时勾选开机自启。

## 验证

```bat
sc query admin-backend     # 看状态 RUNNING
```
浏览器访问 `http://localhost` 正常。

## 小结

Windows 服务化用 winsw/nssm 把 `java -jar` 包成服务，开机自启、崩溃恢复。前端靠 Nginx 托管。

下一篇：[Linux 环境准备](./10-Linux环境准备.md)
