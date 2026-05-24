---
title: "PcbStockNetCore_HuaTian"
description: ""
date: "2026-05-07"
tags: []
share: true
---
# 项目结构
- ![[📂assets/Pasted image 20260419164521.png]]

# DB
---
|类名|作用）|
|---|---|
|DataBases|空类，仅占位，无实际作用|
|MTLList|**物料主数据**：所有物料的基础档案（物料号、规格、型号、价格等）|
|InWcs|**入库单**：记录物料什么时候入库、入多少、从哪来|
|OutWcs|**出库单**：记录物料什么时候出库、出多少、发给谁|
|ReturnWms|**退库单**：生产线多余物料退回仓库|
|TailBag|**尾包 / 散料**：管理一包料用剩的零散数量|
|WCSList|**库位库存**：货架上实际有什么料、多少数量、在哪个位置|
|WCSListResult|**库存汇总**：按物料 + 批次汇总数量（查询用，非数据库表）|
|InProcessOrder|**在制工单**：生产线上正在加工的任务|
|OrderList|**订单总表**：所有出入库 / 加工订单的统一记录|
# 代码分析
---
### 项目文件


### Classes

#### AesEncryptionAndDecryption
- **[[💰项目/物料仓储机/代码文件/AesEncryptionAndDecryption]]**

#### AppSettingHelper
- **[[💰项目/物料仓储机/代码文件/AppSettingHelper]]**

#### ImageForBase64
- **[[💰项目/物料仓储机/代码文件/ImageForBase64]]**

#### RestClient
- **[[💰项目/物料仓储机/代码文件/RestClient]]**


### Controllers
---

#### _Other
---


##### InWcs
---

##### OutWcs
---

##### ReturnWms
---

##### TailBag
---
##### AlarmAcceptController
- **[[💰项目/物料仓储机/代码文件/AlarmAcceptController]]**
##### AlarmListController.cs
- getAlarmList的查询
##### CheckLineController.cs

##### CheckUserController.cs
- **[[💰项目/物料仓储机/代码文件/CheckUserController.cs]]**
##### CreationAlarmController.cs

##### DeleteAlarmController.cs
##### GetAlarmByDateController. cs
##### GetAlarmByLineIDController.cs
##### GetAlarmListByIDController.cs
##### GetLineByNameController.cs
##### GetLinesController.cs
##### GetUserByUserNameController.cs
##### GetUserByUserNoController .cs
##### GetUsersController.cs
##### InProcessOrderController.cs
##### LinesController.cs
##### TestAPIController.cs
##### TouristRoutePicturesController.cs
##### TouristRoutesController.cs
##### UpdateAlarmlController.cs
##### UpdateAlarm2Controller.cs
##### UpdateAlarmController.cs
##### UpdatePassWordController.cs
##### UpdateUserController.cs
##### UsersController.cs




#### Order
---

#### WcsList
---




 
### DataBase
### Dtos
### Migrations
### Models
### Profiles
### Services
### System


### Program.cs
### ReflectionHelper.cs
### Properties
### Startup.cs


















































































































































































































































































































































































