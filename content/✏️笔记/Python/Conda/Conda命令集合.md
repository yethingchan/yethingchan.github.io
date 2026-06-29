# Conda 常用命令汇总表（Windows 适用）
## 一、环境管理（创建/查看/切换/删除/克隆）
| 功能             | 命令                                             | 说明                     |
| -------------- | ---------------------------------------------- | ---------------------- |
| 查看全部虚拟环境       | `conda env list` / `conda info --envs`         | 带 `*` 为当前激活环境          |
| 创建指定Python版本环境 | `conda create -n 环境名 python=3.10`              | -n 指定环境名称，可自定义Python版本 |
| 创建环境并预装包       | `conda create -n 环境名 python=3.10 numpy pandas` | 创建时直接安装依赖              |
| 激活/切换环境        | `conda activate 环境名`                           | Windows无需source，直接执行   |
| 退出当前环境         | `conda deactivate`                             | 回到base基础环境             |
| 克隆复制现有环境       | `conda create -n 新环境名 --clone 旧环境名`            | 完整复制所有包与配置             |
| 删除整个虚拟环境       | `conda remove -n 环境名 --all`                    | 会二次确认，彻底删除             |
| 导出环境配置         | `conda env export > environment.yml`           | 保存环境所有依赖               |
| 根据配置文件重建环境     | `conda env create -f environment.yml`          | 跨机器恢复环境                |

## 二、包管理（安装/卸载/查询/更新）
| 功能 | 命令 | 说明 |
| ---- | ---- | ---- |
| 查看当前环境所有包 | `conda list` | 列出已安装包、版本 |
| 搜索可安装包 | `conda search 包名` | 查询conda源里存在的版本 |
| 安装包 | `conda install 包名` | conda官方源安装 |
| 指定版本安装 | `conda install 包名=1.2.3` | 锁定特定版本 |
| 卸载包 | `conda remove 包名` | 删除单个包 |
| 更新单个包 | `conda update 包名` | 升级至最新稳定版 |
| 更新conda本身 | `conda update conda` | 更新conda工具本体 |
| 更新所有已安装包 | `conda update --all` | 批量升级全部依赖 |

## 三、镜像源配置（解决下载慢）
| 功能 | 命令 | 说明 |
| ---- | ---- | ---- |
| 查看当前配置源 | `conda config --show channels` | 查看已添加镜像地址 |
| 添加清华源 | `conda config --add channels https://mirrors.tuna.tsinghua.edu.cn/anaconda/pkgs/free/` | 加速国内下载 |
| 设置优先显示包版本 | `conda config --set show_channel_urls yes` | 安装时显示来源通道 |
| 删除指定源 | `conda config --remove channels 源地址` | 移除失效镜像 |
| 重置所有源为默认 | `conda config --remove-key channels` | 清空自定义镜像 |

## 四、基础信息 & 清理命令
| 功能 | 命令 | 说明 |
| ---- | ---- | ---- |
| 查看conda版本 | `conda -V` / `conda --version` | 输出当前conda工具版本 |
| 查看系统环境信息 | `conda info` | 环境路径、Python版本、缓存位置 |
| 清理缓存安装包 | `conda clean -p` | 删除无用缓存包，释放磁盘空间 |
| 清理所有缓存 | `conda clean -i -t -p` | 一次性清空索引、缓存、无用包 |

## 五、实用示例组合流程
```cmd
# 1. 查看所有环境
conda env list
# 2. 创建python3.9环境
conda create -n py39 python=3.9
# 3. 切换进入环境
conda activate py39
# 4. 安装接口开发常用包
conda install flask requests pymysql
# 5. 导出环境配置文件
conda env export > env_backup.yml
# 6. 退出环境
conda deactivate
# 7. 删除无用旧环境
conda remove -n old_env --all
```