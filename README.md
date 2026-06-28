# Campus IM校园即时通信与文件传输系统

## 项目简介

[CampusIM](https://github.com/Len-ul/CampusIM.git)是一个基于 C/S 架构的即时通讯与文件传输系统，专为校园环境设计。支持用户注册登录、在线状态管理、点对点私聊、公共群聊、表情及图片富媒体功能、聊天记录查询、个人资料编辑以及文件分块传输、群文件分发等功能。另含一个直连SQLite 的管理后台用于学生和教师信息的CRUD。系统采用 Java 语言开发，基于 JavaFX 构建界面，SQLite数据存储，使用 Socket 进行网络通信，提供稳定高效的实时交互体验。

## 运行环境

- **操作系统**: Windows 10/11, macOS, Linux
- **Java 版本**: JDK 17
- **构建工具**: Maven 3.9+ 

## 依赖安装

Maven 构建，`pom.xml` 文件已包含所有必要依赖，只需执行以下命令自动下载：

```bash
mvn clean install
```

## 启动命令

项目启动可运行根目录下start.bat文件（需手动修改根目录路径），会自动在终端启动服务器和两个客户端应用。（多客户端测试直接启动多个终端窗口）

```powershell
mvn exec:java@run-server	#启动服务器 端口:8888
mvn javafx:run		#运行客户端
mvn exec:java@run-admin		#管理端
```



## 预置账号

|  role  |  username  |  password   | 学号/工号 |
| :----: | :--------: | :---------: | :-------: |
| 管理员 | admin01_li | admin090909 |   null    |
|  学生  |    苏苏    |  edu090909  | S2024001  |
|  学生  |    宁宁    |  isu090909  | S2024002  |
|  学生  |    玉玉    |  edu090909  | S2024003  |
|  教师  |    书书    |  edu090909  | T2024001  |
|  教师  |    秋秋    |  edu090909  | T2024002  |

## 项目结构
---
CampusIM/
├── data/
│   ├── avatars/          # 用户头像 PNG
│   ├── chat_images/      # 客户端聊天图片本地存储
│   ├── emojis/           # 40 个表情 PNG
│   ├── server_files/     # 服务端文件存储
│   └── campusim.db       # SQLite 数据库
├── src/main/java/com/campusim/
│   ├── common/           # 共享协议层
│   │   ├── Message.java         # 通用消息 DTO（20+ 字段）
│   │   ├── MessageType.java     # 47 种消息类型枚举
│   │   └── EmojiManager.java    # 表情加载/渲染管理器
│   ├── server/           # 服务端
│   │   ├── CampusIMServer.java  # TCP 服务器主入口（端口 8888）
│   │   ├── ClientHandler.java   # 客户端连接处理器（25+ 消息处理器）
│   │   ├── UserManager.java     # 用户数据库 DAO（7 张表管理）
│   │   ├── MessageStore.java    # 消息持久化 DAO 
│   │   └── OnlineUserManager.java   # 在线用户管理
│   ├── client/           # 客户端
│   │   ├── CampusIMClient.java  # JavaFX 客户端入口
│   │   ├── network/
│   │   │   └── ServerConnector.java  # 网络连接管理，TCP 连接 + 消息监听线程
│   │   └── ui/
│   │       ├── LoginView.java       # 登录界面
│   │       ├── RegisterView.java    # 注册界面
│   │       ├── MainView.java        # 主聊天窗口
│   │       ├── ChatView.java        # 聊天面板（可复用）
│   │       ├── MessageBubble.java   # 消息气泡组件
│   │       ├── AvatarView.java      # 圆形头像组件
│   │       ├── EmojiInputField.java # 表情输入框
│   │       └── ProfileView.java     # 个人资料窗口
│   └── administrator/    # 管理后台
│       ├── AdminClient.java        # 管理后台入口
│       ├── AdminManager.java       # 管理数据库 DAO
│       ├── model/
│       │   ├── Student.java        # 学生数据模型
│       │   └── Teacher.java        # 教师数据模型
│       └── ui/
│           ├── AdminLoginView.java  # 管理员登录
│           └── AdminMainView.java   # 管理主界面（学生/教师 CRUD）
├── pom.xml
├── start.bat
└── readme.md
---
## 功能模块

---

### 模块一：用户认证

| 需求                  | 实现                                                         |
| --------------------- | ------------------------------------------------------------ |
| 学生/教师角色切换登录 | LoginView ToggleButton → ClientHandler.handleLogin() 按角色查询 users 表 |
| 两种登录方式          | RadioButton 切换：用户名登录 ， 学号登录 / 工号登录 → UserManager.login() JOIN 查询 |
| 密码校验（前端+后端） | 8~15 位，必须包含字母和数字，前后端双重校验                  |
| 学生注册              | RegisterView → 学号(S+7位)、入学年份、专业、班级、学院 → UserManager.registerStudent() |
| 教师注册              | RegisterView → 工号(T+7位)、职称、学院 → UserManager.registerTeacher() |
| 学院/专业级联选择     | 选择学院后 ComboBox listener → 服务端 MAJOR_LIST_REQ → 动态加载专业列表 |
| 密码重置              | 登录页"忘记密码"Dialog → 验证用户名+学号/工号+曾用密码 → 重置为 edu090909 |
| 重复登录拦截          | OnlineUserManager.isOnline() 检查，已在线则拒绝登录          |

**核心流程：** 连接服务器 → 输入凭据 → LOGIN_REQUEST → 服务端验证 → LOGIN_SUCCESS/FAIL → 切换场景

---

### 模块二：即时通讯

| 需求                  | 实现                                                         |
| --------------------- | ------------------------------------------------------------ |
| P2P 私聊              | 双击用户列表项 → 打开私聊 Tab → createTextP2P → 服务端转发 → 收件人 ChatView 追加 |
| 群聊（公共聊天室）    | 默认打开"公共聊天室"Tab → createTextGroup → 服务端 broadcast（排除发送者） |
| 消息发送              | ChatView 输入框 + 发送按钮 / 回车触发 → onSendMessage 回调   |
| 实时消息接收          | ServerConnector 后台线程持续 readObject() → Platform.runLater → handleServerMessage switch |
| 消息气泡（左/右对齐） | MessageBubble isSelf=true → 蓝色圆角气泡靠右；isSelf=false → 灰色圆角气泡靠左 |
| 群聊发送者头部        | MessageBubble.buildGroupMessage() 橙色粗体显示发送者用户名   |
| 离线消息存储          | 收件人不在线 → MessageStore.saveMessage(is_read=0) 存入 messages 表 |
| 消息时间戳展示        | 每条消息显示 HH:mm 格式时间戳                                |
| 自动滚动到底部        | ChatView 监听 messageContainer 高度变化 → scrollPane.setVvalue(1.0) |

---

### 模块三：文件传输

| 需求                    | 实现                                                         |
| :---------------------- | :----------------------------------------------------------- |
| 图片内联发送（<5MB）    | MainView.sendFile() 检测图片扩展名 + 文件大小 ≤5MB → 整张图片 byte[] 直接发送 |
| 图片即时显示            | ChatView.appendImageMessage() → MessageBubble.buildImageBubble() 200px 宽等比缩放 |
| 大文件分块发送          | 64KB 分块循环 → FileInfo + FileChunk(seqNum/totalChunks) → 服务端暂存至 data/server_files/ |
| 文件发送进度条          | ChatView.setProgress() + setStatus() 显示 "发送中... XX%"    |
| 文件接收重组            | ClientHandler.handleFileChunk() 写入 FileOutputStream → 最后一块完成后转发 FILE_INFO |
| 服务端文件暂存          | data/server_files/{fileId}_{fileName}                        |
| 文件下载（FileChooser） | MainView.downloadFile() → FILE_DOWNLOAD_REQ → 服务端逐块回传 → 本地保存 |
| 下载进度提示            | statusLabel "下载 XX... XX%"                                 |
| 群文件下载跟踪          | group_files + group_file_downloads 表记录每个用户下载状态    |
| P2P 文件下载标记        | UserManager.markP2PFileDownloaded() 更新 messages.is_read    |
| 文件气泡（状态显示）    | MessageBubble.buildFileBubble() 文件名+大小+[已下载]/[未下载]标签 |
| 文件双击操作            | 已下载 → explorer 打开；未下载 → 触发下载回调                |

**文件传输分块协议**

```
发送方 → 服务端 → 接收方

FILE_INFO(fileName, fileSize) → 发送方通知接收方文件元数据
FILE_CHUNK(seqNum, totalChunks, chunkData) × N → 逐块发送
最后一块 → 服务端保存到 messages 表 + 转发 FILE_INFO

下载流程：
FILE_DOWNLOAD_REQ(fileId) → 服务端逐块回传 FILE_CHUNK
FILE_DOWNLOAD_DONE(fileId) → 通知服务端下载完成
```

- 分块大小：64KB（65536 字节）
- 图片 ≤5MB：内联整图发送
- 其他文件或大图片：分块发送

**图片发送流程：**

```
Client: 检测扩展名+大小 → 选择发送方式
  ├─ 图片且≤5MB → IMAGE_P2P/IMAGE_GROUP（整图byte[]）
  │   → 服务端保存到 server_files/ → 转发给收件人 → 收件人保存到 chat_images/
  └─ 其他文件或>5MB → FILE_INFO → FILE_CHUNK×N（64KB分块）
      → 服务端暂存 → 最后一块完成后保存到 messages 表 → 转发给收件人
```

---

### 模块四：表情系统

| 需求               | 实现                                                         |
| ------------------ | ------------------------------------------------------------ |
| 40 个 Emoji 映射   | EmojiManager 静态初始化，Unicode 字符 → PNG 文件名           |
| 表情 PNG 缓存      | EmojiManager.getImage() 使用 IMAGE_CACHE 缓存 Image 对象     |
| 输入框表情内联渲染 | EmojiInputField.renderText() 解析文本 → 文字 Text + 表情 ImageView 混合排列 |
| 输入框光标定位     | EmojiInputField.updateCaret() 根据 emoji 图片宽度精确计算光标 X 坐标 |
| 消息气泡表情渲染   | MessageBubble.createBubbleContent() → EmojiManager.renderTo() TextFlow 混合 |
| Emoji 选择器       | ChatView.showEmojiPicker() Popup 弹出 8 列 GridPane → 点击插入到输入框 |
| 回车发送           | EmojiInputField.setOnSendCallback → textField.setOnAction 触发发送按钮 |

---

### 模块五：头像系统

| 需求           | 实现                                                         |
| -------------- | ------------------------------------------------------------ |
| 首字母圆形头像 | AvatarView 构造器：用户名 hashCode → 10 种颜色之一，首字母居中 |
| PNG 头像加载   | AvatarView.loadAvatar() 查找 data/avatars/{identity}.png     |
| 身份缓存       | AvatarView.identityCache(username→学号/工号) 由 USER_LIST_RESP 更新 |
| 头像分块上传   | ProfileView.handleChangeAvatar() 64KB 分块 → AVATAR_UPLOAD_START/CHUNK |
| 头像尺寸限制   | 不超过 500×500 像素                                          |
| 服务端头像管理 | ClientHandler 先写 tmp 文件 → 完成后替换 identity.png → 更新 avatar_url |
| 全局头像刷新   | 收到 USER_LIST_RESP 后所有聊天面板 refreshAvatars()          |

---

### 模块六：在线管理

| 需求              | 实现                                                         |
| ----------------- | ------------------------------------------------------------ |
| 在线用户注册/注销 | OnlineUserManager ConcurrentHashMap addUser/removeUser       |
| 上下线广播        | 登录/登出 → server.broadcast(USER_ONLINE/USER_OFFLINE)       |
| 在线用户列表排序  | MainView.updateUserList() 在线优先 → 学生、教师分组 → 学号/工号升序排序 |
| 绿色在线指示器    | ListCell greenDot Circle(#2ecc71)                            |
| 红色未读指示器    | ListCell redDot Circle(#e74c3c)                              |
| 在线人数统计      | "共 X 人（在线 Y）"                                          |
| 用户列表刷新      | 定时/手动刷新按钮 → USER_LIST_REQ → USER_LIST_RESP           |

---

### 模块七：消息历史

| 需求           | 实现                                                         |
| -------------- | ------------------------------------------------------------ |
| P2P 消息持久化 | ClientHandler.handleP2PText() → MessageStore.saveMessage()   |
| 群聊消息持久化 | ClientHandler.handleGroupText() → MessageStore.saveMessage(receiver="GROUP") |
| P2P 历史拉取   | MessageStore.getP2PHistory(user1, user2) 双向查询            |
| 群聊历史拉取   | MessageStore.getGroupHistory() 查询 receiver="GROUP"         |
| 历史消息渲染   | MainView.displayHistory() 按时间顺序逐一渲染到对应 ChatView  |
| 未读消息计数   | MessageStore.getUnreadCounts(username) GROUP BY sender       |
| 未读计数推送   | USER_LIST_REQ 响应中附带 UNREAD_COUNTS_RESP                  |
| 标记已读       | 打开私聊 Tab → MARK_READ_REQ → MessageStore.markAsRead()     |
| 历史消息分隔线 | "--- 以上为历史消息 ---" 提示                                |

---

### 模块八：个人资料

| 需求         | 实现                                                         |
| ------------ | ------------------------------------------------------------ |
| 查看个人资料 | ProfileView.requestProfile() → PROFILE_GET_REQ → handleProfileResponse() |
| 学生身份信息 | 学号、入学年份、专业、班级、学院（动态显示/隐藏行）          |
| 教师身份信息 | 工号、职称、学院（动态显示/隐藏行）                          |
| 修改用户名   | ProfileView.saveUsername() → 2~10 字符 + 唯一性校验 → 全服广播改名通知 |
| 修改个性签名 | ProfileView.saveSignature() → 2~20 字符                      |
| 修改密码     | showChangePasswordDialog() → 需验证学号/工号 + 旧密码 + 新密码二次确认 |
| 更换头像     | FileChooser → 分块上传 → AVATAR_UPLOAD_DONE 反馈             |
| 模态窗口     | 独立 Stage，Modality.WINDOW_MODAL                            |

---

### 模块九：管理后台

| 需求           | 实现                                                         |
| -------------- | ------------------------------------------------------------ |
| 管理员登录     | AdminLoginView → AdminManager.adminLogin() 查询 role='ADMIN' |
| 学生列表       | AdminMainView TableView：用户名/学号/性别/年级/专业/班级/学院 |
| 教师列表       | AdminMainView TableView：用户名/工号/性别/职称/部门          |
| 添加学生       | 表单输入 → AdminManager.addStudent() 后台线程操作            |
| 添加教师       | 表单输入 → AdminManager.addTeacher() 后台线程操作            |
| 编辑学生       | populateStudentForm() → AdminManager.updateStudent() 事务更新 |
| 编辑教师       | populateTeacherForm() → AdminManager.updateTeacher() 事务更新 |
| 删除用户       | 确认对话框 → AdminManager.deleteUser() 级联删除              |
| 输入校验       | 用户名 2~10 字符、密码 8~13 位含字母                         |
| 本地直连数据库 | AdminManager 直接 JDBC 操作 SQLite，不走 Socket              |

---

### 模块十：通信协议

| 需求          | 实现                                                         |
| ------------- | ------------------------------------------------------------ |
| 47 种消息类型 | MessageType 枚举：LOGIN_REQUEST ~ FILE_DOWNLOAD_DONE         |
| 通用消息 DTO  | Message 类实现 Serializable，20+ 字段覆盖所有业务场景        |
| 工厂方法      | Message.createLoginRequest() / createTextP2P() / createFileInfo() 等 12 个 |
| TCP 连接      | ServerConnector ObjectOutputStream + ObjectInputStream       |
| 消息监听线程  | 后台线程持续 readObject() → MessageListener.onMessageReceived() |
| 消息分发      | switch(MessageType) 分发到各个 handler 方法                  |

**消息类型分类：**

| 类别       | 消息类型                                                     |
| ---------- | ------------------------------------------------------------ |
| 认证类     | LOGIN_REQUEST/SUCCESS/FAIL, REGISTER_REQUEST/SUCCESS/FAIL, LOGOUT_REQUEST |
| 聊天类     | TEXT_P2P, TEXT_GROUP, IMAGE_P2P, IMAGE_GROUP                 |
| 文件类     | FILE_INFO, FILE_CHUNK, FILE_ACCEPT/REJECT, FILE_COMPLETE, FILE_DOWNLOAD_REQ/DONE |
| 在线用户类 | USER_ONLINE, USER_OFFLINE, ONLINE_USERS_REQ/RESP             |
| 历史消息类 | HISTORY_REQUEST/RESPONSE, UNREAD_COUNTS_RESP, MARK_READ_REQ/RES |
| 资料类     | PROFILE_GET_REQ/RESP, PROFILE_UPDATE_USERNAME/SIGNATURE/PASSWORD |
| 头像类     | AVATAR_UPLOAD_START/CHUNK/DONE                               |
| 基础数据类 | USER_LIST_REQ/RESP, DEPT_LIST_REQ/RESP, MAJOR_LIST_REQ/RESP  |

**通用消息DTO**：![image-20260629010052270](https://len-ul.oss-cn-beijing.aliyuncs.com/image-20260629010052270.png)

**消息分发Switch**：ClientHandler中switch分发的25个处理器共 26 个 case 分支（其中FILE_ACCEPT/FILE_REJECT 共用一个handleFileResponse)，不重复计数 handler 方法名为 25 个。

![image-20260629011158304](https://len-ul.oss-cn-beijing.aliyuncs.com/image-20260629011128093.png)![image-20260629011158304](https://len-ul.oss-cn-beijing.aliyuncs.com/image-20260629011158304.png)



---

