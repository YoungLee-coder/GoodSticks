# GoodSticks - 备忘录应用

一个简洁优雅的Android备忘录应用，采用Material Design 3设计规范，界面类似iOS风格。

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

## 注意事项

- 应用使用加密存储保护用户密码
- 拍照功能需要相机权限
- 选择图片需要存储权限
- 图片存储在应用私有目录，卸载应用会删除所有数据
- 备份恢复功能需要联网权限

## 开发者

[YoungLee](https://younglee.cn/)