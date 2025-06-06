# GoodSticks - 记事本应用

一个简洁优雅的Android记事本应用，采用Material Design 3设计规范，界面类似iOS风格。

## 功能特性

- 🔐 **登录功能**：支持记住密码
- 📝 **笔记管理**：创建、编辑、删除笔记
- 📸 **图片支持**：拍照或从相册选择图片
- 🎨 **主题切换**：7种主题色可选
- 📌 **置顶功能**：重要笔记置顶显示
- 🔍 **搜索功能**：快速查找笔记
- 💾 **数据备份**：支持WebDAV备份与恢复

## 技术栈

- **语言**：Java
- **最低SDK**：24 (Android 7.0)
- **目标SDK**：35 (Android 15)
- **架构**：MVVM
- **UI设计**：Material Design 3
- **数据库**：Room
- **图片加载**：Glide 4.16.0
- **安全存储**：EncryptedSharedPreferences
- **动画效果**：Lottie 6.2.0
- **数据同步**：WebDAV (sardine-android)

## 项目结构

```
app/src/main/java/cn/younglee/goodsticks/
├── data                    # 数据层
│   ├── converter           # 类型转换器
│   ├── dao                 # 数据访问对象
│   ├── database            # 数据库定义
│   ├── entity              # 数据实体类
│   └── repository          # 数据仓库
├── ui                      # 用户界面
│   ├── auth                # 登录/注册
│   ├── home                # 主页
│   ├── note                # 笔记编辑
│   ├── settings            # 设置
│   └── splash              # 启动页
└── utils                   # 工具类
```

## 主题色

- 溏心蓝：#1140b6
- 枫叶红：#ab3b2d
- 活力粉：#fe578d
- 闪电紫：#bd90e8
- 便单黄：#eca113
- 宝石绿：#0dae68
- 草木青：#33bfb6

## 使用说明

1. **首次使用**
   - 启动应用后会进入登录页面
   - 点击"还没有账号？点击注册"创建默认账号
   - 默认用户名：admin，密码：123456

2. **创建笔记**
   - 点击右下角的"新建笔记"按钮
   - 输入标题和内容
   - 可点击相机按钮添加图片

3. **管理笔记**
   - 长按笔记可进行置顶或删除操作
   - 点击笔记进入编辑模式
   - 搜索框可快速查找笔记内容

4. **个性化设置**
   - 在设置页面可切换深色模式
   - 可选择7种不同的主题色
   - 支持通过WebDAV备份和恢复数据

## 编译运行

1. 使用Android Studio打开项目
2. 同步Gradle依赖
3. 连接Android设备或启动模拟器
4. 点击运行按钮

## 技术实现

### 1. 用户认证系统

用户认证系统实现了用户的注册、登录和注销功能，采用安全的`EncryptedSharedPreferences`保护用户数据。

```java
private void performLogin(String username, String password) {
    // 显示进度条
    binding.progressBar.setVisibility(View.VISIBLE);
    binding.btnLogin.setEnabled(false);
    
    userRepository.login(username, password).thenAccept(user -> {
        runOnUiThread(() -> {
            if (user != null) {
                // 登录成功，保存用户信息
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_logged_in", true);
                editor.putString("username", username);
                editor.putLong("user_id", user.getId());
                
                if (binding.cbRememberPassword.isChecked()) {
                    editor.putString("password", password);
                }
                editor.apply();
                
                // 跳转到主页
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                // 登录失败
                Snackbar.make(binding.getRoot(), R.string.error_incorrect_password, Snackbar.LENGTH_LONG).show();
            }
        });
    });
}
```

### 2. 笔记管理系统

#### 笔记实体设计

```java
@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "title")
    private String title;
    
    @ColumnInfo(name = "content")
    private String content;
    
    @ColumnInfo(name = "created_date")
    private Date createdDate;
    
    @ColumnInfo(name = "modified_date")
    private Date modifiedDate;
    
    @ColumnInfo(name = "image_path")
    private String imagePath;
    
    @ColumnInfo(name = "is_pinned")
    private boolean isPinned;
    
    @ColumnInfo(name = "color")
    private int color;
    
    @ColumnInfo(name = "user_id")
    private long userId;
    
    // 构造函数和getter/setter省略
}
```

#### 瀑布流布局实现

```java
private void setupRecyclerView() {
    // 使用瀑布流布局，类似iOS的卡片式设计
    StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL);
    binding.recyclerView.setLayoutManager(layoutManager);
    
    adapter = new NoteAdapter(new NoteAdapter.NoteClickListener() {
        @Override
        public void onNoteClick(long noteId) {
            // 打开编辑页面
            Intent intent = new Intent(getActivity(), EditNoteActivity.class);
            intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID, noteId);
            startActivity(intent);
        }
        
        @Override
        public void onNoteLongClick(long noteId) {
            // 长按显示选项菜单
            showNoteOptionsDialog(noteId);
        }
    });
    
    binding.recyclerView.setAdapter(adapter);
}
```

#### 搜索功能实现

```java
private void setupSearch() {
    binding.etSearch.addTextChangedListener(new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            String query = s.toString().trim();
            if (query.isEmpty()) {
                observeNotes();
            } else {
                noteViewModel.searchNotes(query).observe(getViewLifecycleOwner(), notes -> {
                    adapter.submitList(notes);
                    updateEmptyView(notes == null || notes.isEmpty());
                });
            }
        }
        // 其他方法省略
    });
}
```

### 3. WebDAV云同步

应用通过WebDAV协议实现了笔记的云备份和恢复功能：

```java
public static CompletableFuture<String> createBackup(Context context, long userId) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            // 创建备份数据
            BackupData backupData = createBackupData(context, userId);
            String jsonData = gson.toJson(backupData);
            
            // 准备WebDAV客户端和文件名
            Sardine sardine = getSardine(context);
            
            // 上传备份文件
            // 代码省略
            
            // 更新最后备份时间
            String readableTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            updateLastBackupTime(context, readableTime);
            
            return "备份成功";
        } catch (Exception e) {
            return "备份失败: " + e.getMessage();
        }
    }, executor);
}
```

### 4. 主题切换系统

```java
private void showThemeColorDialog() {
    ThemeUtils.Theme[] themes = ThemeUtils.Theme.values();
    String[] themeNames = new String[themes.length];
    for (int i = 0; i < themes.length; i++) {
        themeNames[i] = themes[i].getName();
    }
    
    new AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_theme_color)
            .setItems(themeNames, (dialog, which) -> {
                ThemeUtils.Theme selectedTheme = themes[which];
                ThemeUtils.saveTheme(requireContext(), selectedTheme);
                
                // 更新显示
                binding.tvCurrentTheme.setText(selectedTheme.getName());
                binding.viewThemeColor.setBackgroundColor(selectedTheme.getColorInt());
                
                // 通知主题已更改
                showThemeChangeNotice();
            })
            .show();
}
```

## 技术选择 Q&A

### Q1: 数据库为什么不使用SQLite，而是用Room，二者的差别是什么？

**A:** 本项目选择Room作为数据库解决方案，主要基于以下考量：

#### Room相比SQLite的优势：

**1. 编译时验证**
- Room在编译时会验证SQL查询的正确性，而SQLite只能在运行时发现错误
- 项目中的`NoteDao.java`使用`@Query`注解，Room会检查SQL语句语法正确性

**2. 类型安全**
- Room提供强类型的数据库访问，避免了SQLite中容易出现的类型转换错误
- `Note.java`实体类使用`@Entity`注解，Room自动处理Java对象和数据库字段的映射

**3. 减少样板代码**
```java
// Room只需要定义接口
@Dao
public interface NoteDao {
    @Insert
    long insert(Note note);
    
    @Query("SELECT * FROM notes WHERE id = :id")
    Note getNoteByIdSync(long id);
}

// 传统SQLite需要大量样板代码
// 需要手动写SQLiteOpenHelper、ContentValues、Cursor处理等
```

**4. LiveData支持**
- Room原生支持LiveData，实现响应式编程
- 项目中使用`LiveData<List<Note>>`，数据变化时UI会自动更新

**5. 线程安全**
- Room自动处理数据库操作的线程安全问题
- 传统SQLite需要手动管理数据库连接和线程

### Q2: 图片加载为什么用Glide，除了这个，传统安卓开发使用的是什么，二者的差别是什么？

**A:** 本项目选择Glide进行图片加载，相比传统方式有显著优势：

#### 传统Android图片加载方式：
- **BitmapFactory** + **ImageView.setImageBitmap()**
- **AsyncTask** + **HttpURLConnection**
- 手动实现内存管理和缓存机制

#### Glide相比传统方式的优势：

**1. 自动内存管理**
```java
// 项目中的Glide使用（简洁高效）
Glide.with(this)
    .load(imagePath)
    .centerCrop()
    .into(binding.ivImage);

// 传统方式需要手动管理内存
Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
if (bitmap != null) {
    imageView.setImageBitmap(bitmap);
    // 需要手动回收bitmap避免内存泄漏
    // bitmap.recycle();
}
```

**2. 生命周期感知**
- `Glide.with(this)`会自动绑定Activity/Fragment的生命周期
- 当页面销毁时自动取消图片加载，避免内存泄漏

**3. 多格式支持**
- 支持网络图片、本地文件、资源文件、Uri等多种数据源
- 项目中既加载本地图片路径，也处理相机拍摄的Uri

**4. 智能缓存机制**
- 自动提供内存缓存和磁盘缓存
- 传统方式需要手动实现LruCache等缓存策略

**5. 图片变换**
- 内置centerCrop、圆角等变换效果
- 传统方式需要手动使用Canvas和Matrix进行变换

**6. 占位符和错误处理**
```java
// Glide可以轻松设置占位图和错误图
Glide.with(context)
    .load(url)
    .placeholder(R.drawable.loading)
    .error(R.drawable.error)
    .into(imageView);
```

#### 项目中的实际应用：

**笔记图片显示**（NoteAdapter.java）：
```java
if (note.getImagePath() != null && !note.getImagePath().isEmpty()) {
    binding.ivImage.setVisibility(View.VISIBLE);
    Glide.with(binding.getRoot())
            .load(note.getImagePath())
            .centerCrop()
            .into(binding.ivImage);
} else {
    binding.ivImage.setVisibility(View.GONE);
}
```

**头像处理**（SettingsFragment.java）：
- 支持拍照和相册选择
- 自动压缩和Base64转换
- 圆形头像显示效果

#### 技术选择总结：

**Room的核心价值：** 提供了更安全、更简洁的数据库操作方式，减少了样板代码，提高了开发效率和代码质量。在MVVM架构中与LiveData完美配合。

**Glide的核心价值：** 解决了传统图片加载的内存管理难题，提供了更高级的功能和更好的用户体验。特别适合处理用户上传的图片内容。

这两个技术选择都体现了现代Android开发向更高效、更安全、更易维护方向发展的趋势。

### Q3: 为什么采用MVVM架构，与传统MVC架构有什么区别？

**A:** 本项目采用MVVM（Model-View-ViewModel）架构模式，这是Android Jetpack推荐的现代架构方案：

#### MVVM相比传统MVC/MVP的优势：

**1. 数据绑定与响应式编程**
```java
// 项目中的ViewModel实现
public class NoteViewModel extends AndroidViewModel {
    private final NoteRepository repository;
    private final LiveData<List<Note>> allNotes;
    
    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);
        allNotes = repository.getAllNotes();
    }
    
    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }
}

// Fragment中的数据观察
noteViewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
    adapter.submitList(notes);
    updateEmptyView(notes == null || notes.isEmpty());
});
```

**2. 生命周期感知**
- ViewModel会在配置变更（如屏幕旋转）时保留数据
- LiveData自动处理生命周期，避免内存泄漏

**3. 职责清晰分离**
- **Model（Repository + Entity）**：数据获取和业务逻辑
- **View（Activity/Fragment）**：UI显示和用户交互
- **ViewModel**：连接View和Model，处理UI相关的数据逻辑

**4. 便于单元测试**
- ViewModel独立于Android组件，容易进行单元测试
- 传统MVC中Activity职责过重，难以测试

#### 传统MVC的问题：
- Activity既是Controller又是View，职责不清
- 数据状态管理困难
- 配置变更时需要手动保存和恢复数据
- 紧耦合，难以测试和维护

### Q4: 为什么选择Material Design 3，与传统UI设计有什么不同？

**A:** 本项目全面采用Material Design 3设计系统，这是Google最新的设计语言：

#### Material Design 3的特色：

**1. 现代化组件**
```xml
<!-- 项目中的MaterialCardView使用 -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="16dp"
    app:cardElevation="2dp"
    app:cardBackgroundColor="?attr/colorSurface">
    
<!-- TextInputLayout with Material 3 style -->
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
    android:hint="@string/username"
    app:boxCornerRadiusTopEnd="12dp"
    app:startIconDrawable="@drawable/ic_person">
```

**2. 动态主题色系统**
```java
// 项目中的主题切换实现
public enum Theme {
    BLUE("溏心蓝", R.style.Theme_GoodSticks_Blue),
    RED("枫叶红", R.style.Theme_GoodSticks_Red),
    PINK("活力粉", R.style.Theme_GoodSticks_Pink),
    PURPLE("闪电紫", R.style.Theme_GoodSticks_Purple),
    YELLOW("便单黄", R.style.Theme_GoodSticks_Yellow),
    GREEN("宝石绿", R.style.Theme_GoodSticks_Green),
    CYAN("草木青", R.style.Theme_GoodSticks_Cyan);
}
```

**3. 一致的设计语言**
- 圆角半径：16dp用于卡片，12dp用于输入框
- 卡片阴影：2dp提供适度的层次感
- 颜色系统：使用`?attr/colorPrimary`等动态颜色

#### 相比传统UI设计的优势：

**传统Android UI的问题：**
- 组件样式不统一，需要大量自定义
- 缺乏统一的设计规范
- 适配深色模式困难
- 缺乏现代感的视觉效果

**Material Design 3的改进：**
- 提供完整的设计系统和组件库
- 自动适配深色模式
- 符合现代用户期望的交互体验
- 减少UI开发工作量

### Q5: 为什么使用EncryptedSharedPreferences，与普通SharedPreferences有什么区别？

**A:** 本项目使用EncryptedSharedPreferences保护用户敏感数据，这是Android Security Crypto库提供的安全存储方案：

#### 具体实现：

```java
// 项目中的安全存储初始化
private void initEncryptedSharedPreferences() {
    try {
        MasterKey masterKey = new MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
                
        sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    } catch (GeneralSecurityException | IOException e) {
        // 降级到普通SharedPreferences
        sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }
}
```

#### 安全优势对比：

**普通SharedPreferences：**
- 数据以明文形式存储在`/data/data/包名/shared_prefs/`
- Root设备或恶意应用可直接读取
- 密码、Token等敏感信息暴露风险高

**EncryptedSharedPreferences：**
- 使用AES256-GCM加密算法
- 密钥存储在Android Keystore中，硬件级别保护
- 即使获取文件内容也无法解密
- 自动处理密钥生成和管理

#### 项目中的应用场景：
```java
// 存储用户登录信息
editor.putBoolean("is_logged_in", true);
editor.putString("username", username);
editor.putLong("user_id", user.getId());
editor.putString("password", password); // 加密存储密码

// 存储WebDAV配置信息
editor.putString("webdav_url", url);
editor.putString("webdav_username", username);
editor.putString("webdav_password", password);

// 存储用户头像数据
String avatarKey = "avatar_" + currentUserId;
prefs.edit().putString(avatarKey, base64).apply();
```

### Q6: 为什么使用Lottie动画，与传统动画方案有什么区别？

**A:** 虽然项目中主要使用了属性动画，但集成了Lottie库为未来的复杂动画做准备：

#### 当前动画实现：

```java
// 项目中的启动页动画（属性动画）
private void startAnimation() {
    // Logo 渐入动画
    binding.ivLogo.setAlpha(0f);
    binding.ivLogo.animate()
            .alpha(1f)
            .setDuration(1000)
            .setListener(null);
            
    // App 名称渐入动画
    binding.tvAppName.setAlpha(0f);
    binding.tvAppName.animate()
            .alpha(1f)
            .setStartDelay(500)
            .setDuration(1000)
            .setListener(null);
}

// Fragment切换动画
getSupportFragmentManager().beginTransaction()
        .setCustomAnimations(
                R.anim.fade_in,    // 淡入动画
                R.anim.fade_out    // 淡出动画
        )
        .hide(activeFragment)
        .show(targetFragment)
        .commitNow();
```

#### Lottie相比传统动画的优势：

**传统Android动画的局限：**
- **帧动画**：占用内存大，文件体积大
- **补间动画**：只能实现简单的变换效果
- **属性动画**：复杂动画需要大量代码
- **Vector动画**：创建和调试困难

**Lottie的优势：**
1. **设计师友好**：支持After Effects导出，设计师可直接创建
2. **文件小巧**：JSON格式，比GIF/视频小很多
3. **无损缩放**：基于矢量，适配所有屏幕密度
4. **交互性强**：支持动态改变颜色、播放控制等
5. **跨平台**：同一个文件可在Android、iOS、Web使用

#### 使用场景规划：
- 启动页品牌动画
- 加载状态指示器
- 用户操作反馈动画
- 空状态页面插图

### Q7: 为什么选择WebDAV进行数据同步，与其他同步方案的对比？

**A:** 项目选择WebDAV协议实现云端数据备份，这是一个开放标准的文件传输协议：

#### WebDAV实现：

```java
// 项目中的WebDAV备份实现
public static CompletableFuture<String> createBackup(Context context, long userId) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            // 创建备份数据
            BackupData backupData = createBackupData(context, userId);
            String jsonData = gson.toJson(backupData);
            
            // 初始化WebDAV客户端
            Sardine sardine = getSardine(context);
            String fileName = "goodsticks_backup_" + 
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                    .format(new Date()) + ".json";
            
            // 上传备份文件
            sardine.put(webdavUrl + "/" + fileName, 
                       jsonData.getBytes(StandardCharsets.UTF_8));
            
            return "备份成功";
        } catch (Exception e) {
            return "备份失败: " + e.getMessage();
        }
    }, executor);
}
```

#### 与其他同步方案的对比：

**1. 云服务API（如Google Drive、iCloud）**
- **优势**：官方支持，稳定性好
- **劣势**：依赖特定平台，用户需要对应账号
- **项目选择**：WebDAV更灵活，用户可选择任意支持的云存储

**2. 自建服务器**
- **优势**：完全控制，功能定制化
- **劣势**：开发维护成本高，需要服务器资源
- **项目选择**：WebDAV利用现有云存储，无需额外服务器

**3. Firebase/云数据库**
- **优势**：实时同步，多设备协作
- **劣势**：依赖网络，有使用限制和费用
- **项目选择**：个人笔记应用，WebDAV的简单备份恢复已足够

#### WebDAV的实际优势：

**1. 广泛兼容性**
- 支持Nextcloud、ownCloud等私有云
- 支持阿里云盘、123网盘等商业服务
- 支持AList等本地文件管理工具

**2. 用户控制权**
```java
// 用户可自由配置WebDAV服务器
SharedPreferences prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
String webdavUrl = prefs.getString("webdav_url", "");
String webdavUsername = prefs.getString("webdav_username", "");
String webdavPassword = prefs.getString("webdav_password", "");
```

**3. 数据安全性**
- 数据存储在用户选择的服务器
- 支持HTTPS加密传输
- 本地数据加密存储

#### 技术选择总结：

本项目的每个技术选择都基于现代Android开发的最佳实践：

- **Room + MVVM**：构建清晰的架构层次，便于维护和测试
- **Glide + Material Design 3**：提供现代化的用户体验
- **EncryptedSharedPreferences**：保护用户隐私和数据安全
- **Lottie + WebDAV**：为未来功能扩展提供技术基础

这些技术选择不仅解决了当前需求，也为应用的长期发展奠定了坚实基础。

## 注意事项

- 应用使用加密存储保护用户密码
- 拍照功能需要相机权限
- 选择图片需要存储权限
- 图片存储在应用私有目录，卸载应用会删除所有数据
- 备份恢复功能需要联网权限

## 开发者

[YoungLee](https://younglee.cn/)