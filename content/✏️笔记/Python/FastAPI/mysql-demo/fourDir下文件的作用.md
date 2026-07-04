### 前置整体说明  
先宏观介绍这4个文件夹的职责分工（FastAPI业界标准四层拆分）：  
1. `api`：路由接口层，对接前端HTTP请求；  
2. `crud`：纯粹数据库操作层，封装增删改查；  
3. `models`：SQLAlchemy ORM模型，映射MySQL真实数据表；  
4. `schemas`：Pydantic模型，做请求参数校验、接口返回格式化。  
   下面逐个目录详细拆解写法缘由，沿用此前抖音爬虫的`video.py`文件举例，每个代码片段逐行解释设计目的。  
  
# 一、models文件夹（数据库映射层）  
存放`video.py`  
```python  
# 从SQLAlchemy导入构建数据表字段需要的类  
from sqlalchemy import Column, Integer, String, DateTime  
# 导入database.py提前定义好的ORM基类Base，只有继承Base，SQLAlchemy才识别这是数据表实体  
from database import Base  
  
# Video类对应MySQL里的videos数据表  
class Video(Base):  
    # __tablename__固定内置属性，显式声明映射MySQL中哪一张物理数据表，命名要和DataGrip建好的表完全一致  
    __tablename__ = "videos"        # Column用来定义数据表单列；Integer对应mysql的int；primary_key=True标记主键；autoincrement=True开启主键自增，新增数据不用手动填写id；comment写入数据库字段注释  
    id = Column(Integer, primary_key=True, autoincrement=True, comment="主键")  
    # varchar(100)字符串类型；unique=True设置数据库层面唯一约束，避免重复录入同一个抖音作品aweme_id；nullable=False意味着数据库禁止此字段为空  
    aweme_id = Column(String(100), unique=True, nullable=False, comment="作品ID")  
    # 后续字段默认允许为空，因为作者名、作者uid、视频文案偶尔会抓取不到  
    author_name = Column(String(100), comment="作者名")  
    author_sec_uid = Column(String(100), comment="作者UID")  
    description = Column(String(1000), comment="视频文案")  
    # DateTime适配MySQL datetime类型，用来存储抓取时刻  
    crawl_time = Column(DateTime, comment="爬取时间")  
```  
#### 为什么这样设计？  
1. 把数据库表结构固化在Python代码，方便版本管控；  
2. 约束（唯一键、非空）下沉到MySQL层面，即便上层代码出现疏漏，数据库也能拦截脏数据；  
3. 后续crud层直接操作Video这个Python类，不用手写原生SQL字符串，规避SQL注入隐患。  
  
# 二、schemas文件夹（Pydantic校验层）  
存放`video.py`  
```python  
# BaseModel是Pydantic基类，自带参数校验、类型自动转换、序列化能力  
from pydantic import BaseModel  
# Optional用于标记字段可空缺，datetime适配时间类型  
from typing import Optional  
from datetime import datetime  
  
# 新增视频的时候接收前端传参的模型  
class VideoCreate(BaseModel):  
    # 前端提交新增数据，aweme_id必填，所以不加Optional  
    aweme_id: str    # 下面几项前端提交时可以不传，默认值就是None  
    author_name: Optional[str] = None    author_sec_uid: Optional[str] = None    description: Optional[str] = None  
# 修改视频专用模型：所有字段都可选，前端只传想要改动的字段即可  
class VideoUpdate(BaseModel):  
    author_name: Optional[str] = None    author_sec_uid: Optional[str] = None    description: Optional[str] = None  
# 控制接口向前端输出哪些字段的返回模型  
class VideoResp(BaseModel):  
    id: int    aweme_id: str    author_name: Optional[str]    author_sec_uid: Optional[str]    description: Optional[str]    crawl_time: Optional[datetime]  
    # 内部Config配置：from_attributes = True，允许直接把SQLAlchemy的models对象转换为这个响应模型，免去手工逐个赋值  
    class Config:        from_attributes = True  
```  
#### 为什么拆分Create、Update、Resp三套模型？  
1. `VideoCreate`：新增场景强制必填作品id；  
2. `VideoUpdate`：更新业务一般不需要改动aweme_id，就不在模型内写它，避免前端误改作品编号；  
3. `VideoResp`：可以按需屏蔽数据库内部字段（后续如果新增内部标记字段，不会暴露前端），做好数据隔离；  
4. Pydantic会在请求刚抵达的时候就完成格式校验，参数不合法直接提前报错，不会向下进入数据库逻辑，减轻数据库压力。  
  
# 三、crud文件夹（数据库操作层）  
存放`video.py`  
```python  
# AsyncSession是异步数据库会话对象，每一次请求独立一个会话操作数据库  
from sqlalchemy.ext.asyncio import AsyncSession  
# select、update、delete用于构造ORM风格SQL语句  
from sqlalchemy import select, update, delete  
# 引入数据表ORM模型  
from models.video import Video  
# 引入schemas里的入参校验模型  
from schemas.video import VideoCreate, VideoUpdate  
  
# 创建数据：函数参数接收数据库会话、前端校验完毕的新增参数  
async def create_video(db: AsyncSession, data: VideoCreate):  
    # model_dump()把Pydantic校验完成的对象转为字典，**解包之后初始化ORM实体  
    obj = Video(**data.model_dump())    # db.add()把对象放进会话缓存，此刻数据暂存内存，尚未写入MySQL  
    db.add(obj)    # await提交事务，正式持久化写入数据库  
    await db.commit()    # refresh主动从数据库拉取自增id这类数据库生成的值，回填到Python对象  
    await db.refresh(obj)    return obj  
# 根据主键查询单条视频  
async def get_video_by_id(db: AsyncSession, vid: int):  
    # 拼装SELECT语句：筛选id匹配的数据  
    stmt = select(Video).where(Video.id == vid)    # 异步执行SQL语句  
    res = await db.execute(stmt)    # scalar_one_or_none：匹配到就返回ORM对象，没有匹配结果安全返回None，不会抛出异常  
    return res.scalar_one_or_none()  
# 分页查询列表  
async def list_video(db: AsyncSession, page: int = 1, size: int = 20):  
    # 依据页码计算跳过多少条旧数据，实现分页  
    offset = (page - 1) * size    stmt = select(Video).offset(offset).limit(size)    res = await db.execute(stmt)    # scalars().all()取出多条ORM记录封装列表返回  
    return res.scalars().all()  
# 更新视频  
async def update_video(db: AsyncSession, vid: int, update_info: VideoUpdate):  
    # exclude_unset=True：只保留前端实际传来的字段，前端没传的字段不会被覆盖为null  
    update_dict = update_info.model_dump(exclude_unset=True)    stmt = update(Video).where(Video.id == vid).values(**update_dict)    await db.execute(stmt)    await db.commit()    # 更新完成重新查询，拿到最新的数据  
    return await get_video_by_id(db, vid)  
# 删除视频  
async def delete_video(db: AsyncSession, vid: int):  
    stmt = delete(Video).where(Video.id == vid)    await db.execute(stmt)    await db.commit()    return True  
```  
#### 单独抽出crud目录的意义  
1. **关注点分离**：全部数据库操作收拢在这里，api路由文件就不会充斥SQL细节；后续更换数据表细节，只要修改crud层，前端接口代码基本不用改动；  
2. 函数全部标记async，适配异步数据库连接，保障FastAPI高并发性能；  
3. 通用CRUD逻辑可以复用，多个路由接口都能够调用`get_video_by_id`等函数，杜绝重复代码。  
  
# 四、api文件夹（路由接口层）  
存放`video.py`  
```python  
# APIRouter用来模块化拆分路由；Depends实现依赖注入；HTTPException向前端抛出规范HTTP错误码  
from fastapi import APIRouter, Depends, HTTPException  
from sqlalchemy.ext.asyncio import AsyncSession  
# 导入database.py写好的依赖函数，自动拿到数据库会话  
from database import get_db  
# 引入crud层封装好的所有数据库方法  
from crud.video import create_video, get_video_by_id, list_video, update_video, delete_video  
# 引入schemas入参、出参模型  
from schemas.video import VideoCreate, VideoUpdate, VideoResp  
  
# 创建路由分组，统一前缀、统一接口文档标签，接口归类更清晰  
router = APIRouter(prefix="/video", tags=["视频管理"])  
  
# POST请求实现新增，response_model约束返回内容严格匹配VideoResp结构  
@router.post("/add", response_model=VideoResp)  
async def add_video(form: VideoCreate, db: AsyncSession = Depends(get_db)):  
    # form自动解析前端JSON并且完成Pydantic校验；Depends(get_db)自动注入数据库会话  
    return await create_video(db, form)  
# 通过URL路径参数vid获取单条视频  
@router.get("/{vid}", response_model=VideoResp)  
async def get_single(vid: int, db: AsyncSession = Depends(get_db)):  
    video = await get_video_by_id(db, vid)    # crud查询返回None，就在路由层抛出404提示，业务提示文案放在这一层  
    if not video:        raise HTTPException(status_code=404, detail="视频不存在")  
    return video  
# 分页列表接口，给页码、每页条数设置默认值，前端不传就采用默认分页参数  
@router.get("/list", response_model=list[VideoResp])  
async def get_list(page: int = 1, size: int = 20, db: AsyncSession = Depends(get_db)):  
    return await list_video(db, page, size)  
# PUT方法做整体更新  
@router.put("/{vid}", response_model=VideoResp)  
async def edit_video(vid: int, form: VideoUpdate, db: AsyncSession = Depends(get_db)):  
    video = await get_video_by_id(db, vid)    if not video:        raise HTTPException(status_code=404, detail="视频不存在")  
    return await update_video(db, vid, form)  
# DELETE方法删除资源  
@router.delete("/{vid}")  
async def del_video(vid: int, db: AsyncSession = Depends(get_db)):  
    video = await get_video_by_id(db, vid)    if not video:        raise HTTPException(status_code=404, detail="视频不存在")  
    await delete_video(db, vid)    return {"msg": "删除成功"}  
```  
#### api层的设计考量  
1. 只负责HTTP层面工作：解析前端参数、依赖拿数据库连接、做资源是否存在的校验、抛出HTTP标准异常、规定返回格式；不会编写任何SQL；  
2. `prefix="/video"`批量统一接口前缀，接口地址整洁；`tags=["视频管理"]`会展示在FastAPI自动生成的Swagger接口文档中，方便调试；  
3. HTTP方法严格遵守REST风格：POST新增、GET查询、PUT修改、DELETE删除，前后端协作规范统一。  
  
# 四者之间调用顺序（单向依赖，禁止反向依赖）  
```  
api（路由） → crud（数据库操作） → models（表结构）  
schemas 同时供给api做参数校验、供给crud接收数据  
```  
- api可以导入crud、schemas；  
- crud可以导入models、schemas；  
- models只能导入database的Base，不能反向导入crud/api，分层互不耦合，后续迭代维护很轻松。