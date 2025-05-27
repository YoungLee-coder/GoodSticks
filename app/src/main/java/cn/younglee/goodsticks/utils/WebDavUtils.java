package cn.younglee.goodsticks.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.younglee.goodsticks.GoodSticksApplication;
import cn.younglee.goodsticks.data.entity.Note;
import cn.younglee.goodsticks.data.entity.User;
import cn.younglee.goodsticks.data.repository.NoteRepository;
import cn.younglee.goodsticks.data.repository.UserRepository;

public class WebDavUtils {
    private static final String TAG = "WebDavUtils";
    private static final Executor executor = Executors.newSingleThreadExecutor();
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    // WebDAV服务器相关配置
    private static final String WEBDAV_PREFS_NAME = "webdav_prefs";
    private static final String KEY_WEBDAV_URL = "webdav_url";
    private static final String KEY_WEBDAV_USERNAME = "webdav_username";
    private static final String KEY_WEBDAV_PASSWORD = "webdav_password";
    private static final String KEY_WEBDAV_FOLDER = "webdav_folder";
    private static final String KEY_WEBDAV_ENABLED = "webdav_enabled";
    private static final String KEY_WEBDAV_LAST_BACKUP = "webdav_last_backup";

    // 备份文件名称格式
    private static final String BACKUP_FILE_NAME_FORMAT = "goodsticks_backup_%s.json";

    /**
     * 保存WebDAV设置
     */
    public static void saveWebDavSettings(Context context, String url, String username, 
                                          String password, String folder, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(WEBDAV_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_WEBDAV_URL, url);
        editor.putString(KEY_WEBDAV_USERNAME, username);
        editor.putString(KEY_WEBDAV_PASSWORD, password);
        editor.putString(KEY_WEBDAV_FOLDER, folder);
        editor.putBoolean(KEY_WEBDAV_ENABLED, enabled);
        editor.apply();
    }

    /**
     * 获取WebDAV设置
     */
    public static WebDavSettings getWebDavSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(WEBDAV_PREFS_NAME, Context.MODE_PRIVATE);
        String url = prefs.getString(KEY_WEBDAV_URL, "");
        String username = prefs.getString(KEY_WEBDAV_USERNAME, "");
        String password = prefs.getString(KEY_WEBDAV_PASSWORD, "");
        String folder = prefs.getString(KEY_WEBDAV_FOLDER, "/GoodSticks/");
        boolean enabled = prefs.getBoolean(KEY_WEBDAV_ENABLED, false);
        String lastBackup = prefs.getString(KEY_WEBDAV_LAST_BACKUP, "");
        
        return new WebDavSettings(url, username, password, folder, enabled, lastBackup);
    }

    /**
     * 更新最后备份时间
     */
    private static void updateLastBackupTime(Context context, String time) {
        SharedPreferences prefs = context.getSharedPreferences(WEBDAV_PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WEBDAV_LAST_BACKUP, time).apply();
    }

    /**
     * 测试WebDAV连接
     */
    public static CompletableFuture<Boolean> testConnection(String url, String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Sardine sardine = new OkHttpSardine();
                sardine.setCredentials(username, password);
                
                // 首先尝试exists方法
                try {
                    sardine.exists(url);
                    return true;
                } catch (Exception e) {
                    Log.w(TAG, "WebDAV exists方法失败，尝试list方法: " + e.getMessage());
                    
                    // 如果exists方法失败，尝试list方法
                    try {
                        sardine.list(url);
                        return true;
                    } catch (Exception e2) {
                        Log.w(TAG, "WebDAV list方法失败，尝试head方法: " + e2.getMessage());
                        
                        // 最后尝试一个简单的head请求
                        try {
                            sardine.exists(url);
                            return true;
                        } catch (Exception e3) {
                            Log.e(TAG, "WebDAV所有连接测试方法都失败", e3);
                            return false;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "WebDAV连接测试失败", e);
                return false;
            }
        }, executor);
    }

    /**
     * 获取Sardine客户端
     */
    private static Sardine getSardine(Context context) {
        WebDavSettings settings = getWebDavSettings(context);
        Sardine sardine = new OkHttpSardine();
        sardine.setCredentials(settings.getUsername(), settings.getPassword());
        
        // 记录WebDAV连接信息（不包含密码）以便排查问题
        Log.d(TAG, "WebDAV URL: " + settings.getUrl());
        Log.d(TAG, "WebDAV Username: " + settings.getUsername());
        Log.d(TAG, "WebDAV Folder: " + settings.getFolder());
        
        return sardine;
    }

    /**
     * 确保WebDAV文件夹存在
     */
    private static void ensureDirectoryExists(Sardine sardine, String url) throws IOException {
        try {
            if (!sardine.exists(url)) {
                try {
                    sardine.createDirectory(url);
                } catch (Exception e) {
                    Log.w(TAG, "无法创建目录，将尝试直接上传: " + e.getMessage());
                    // 忽略目录创建错误，尝试直接上传文件
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "检查目录存在性失败，将尝试直接上传: " + e.getMessage());
            // 忽略目录检查错误，尝试直接上传文件
        }
    }

    /**
     * 创建备份数据对象
     */
    private static BackupData createBackupData(Context context, long userId) {
        BackupData backupData = new BackupData();
        
        // 获取用户信息
        UserRepository userRepository = new UserRepository(GoodSticksApplication.getInstance());
        User user = userRepository.getUserByIdSync(userId);
        if (user != null) {
            backupData.setUser(user);
            
            // 获取笔记信息
            NoteRepository noteRepository = new NoteRepository(GoodSticksApplication.getInstance());
            List<Note> notes = noteRepository.getNotesByUserIdSync(userId);
            backupData.setNotes(notes);
            
            // 获取用户头像
            SharedPreferences prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
            String avatarKey = "avatar_" + userId;
            String avatarBase64 = prefs.getString(avatarKey, "");
            backupData.setAvatarBase64(avatarBase64);
        }
        
        return backupData;
    }

    /**
     * 创建备份
     */
    public static CompletableFuture<String> createBackup(Context context, long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                WebDavSettings settings = getWebDavSettings(context);
                if (!settings.isEnabled() || settings.getUrl().isEmpty()) {
                    return "WebDAV设置未启用或URL为空";
                }
                
                // 创建备份数据
                BackupData backupData = createBackupData(context, userId);
                String jsonData = gson.toJson(backupData);
                
                // 准备WebDAV客户端和文件名
                Sardine sardine = getSardine(context);
                String baseUrl = settings.getUrl();
                if (!baseUrl.endsWith("/")) {
                    baseUrl += "/";
                }
                
                String folder = settings.getFolder();
                if (!folder.startsWith("/")) {
                    folder = "/" + folder;
                }
                if (!folder.endsWith("/")) {
                    folder += "/";
                }
                
                String webDavFolderUrl = baseUrl + (folder.startsWith("/") ? folder.substring(1) : folder);
                Log.d(TAG, "尝试访问的WebDAV完整URL: " + webDavFolderUrl);
                
                try {
                    // 尝试确保目录存在，但即使失败也继续
                    ensureDirectoryExists(sardine, webDavFolderUrl);
                } catch (Exception e) {
                    Log.w(TAG, "确保目录存在时出错，将尝试直接上传: " + e.getMessage());
                    // 忽略目录错误，继续尝试上传
                }
                
                // 生成备份文件名，使用时间戳
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                String timestamp = sdf.format(new Date());
                String fileName = String.format(BACKUP_FILE_NAME_FORMAT, timestamp);
                String fileUrl = webDavFolderUrl + fileName;
                
                Log.d(TAG, "尝试上传到: " + fileUrl);
                
                // 使用try-catch专门处理上传操作
                try {
                    // 上传备份文件
                    byte[] data = jsonData.getBytes(StandardCharsets.UTF_8);
                    
                    // 指定Content-Type为application/json
                    String contentType = "application/json";
                    
                    sardine.put(fileUrl, data, contentType);
                    
                    // 更新最后备份时间
                    updateLastBackupTime(context, timestamp);
                    
                    return "备份成功: " + fileName;
                } catch (Exception e) {
                    Log.e(TAG, "文件上传失败: " + e.getMessage(), e);
                    return "文件上传失败: " + e.getMessage();
                }
            } catch (Exception e) {
                Log.e(TAG, "创建备份失败", e);
                return "备份失败: " + e.getMessage();
            }
        }, executor);
    }

    /**
     * 获取可用的备份文件列表
     */
    public static CompletableFuture<List<BackupFileInfo>> getBackupFiles(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            List<BackupFileInfo> backupFiles = new ArrayList<>();
            try {
                WebDavSettings settings = getWebDavSettings(context);
                if (!settings.isEnabled() || settings.getUrl().isEmpty()) {
                    return backupFiles;
                }
                
                Sardine sardine = getSardine(context);
                String baseUrl = settings.getUrl();
                if (!baseUrl.endsWith("/")) {
                    baseUrl += "/";
                }
                
                String folder = settings.getFolder();
                if (!folder.startsWith("/")) {
                    folder = "/" + folder;
                }
                if (!folder.endsWith("/")) {
                    folder += "/";
                }
                
                String webDavFolderUrl = baseUrl + (folder.startsWith("/") ? folder.substring(1) : folder);
                
                try {
                    // 检查目录是否存在
                    boolean exists = false;
                    try {
                        exists = sardine.exists(webDavFolderUrl);
                    } catch (Exception e) {
                        Log.w(TAG, "检查目录存在性失败，将尝试直接列出文件: " + e.getMessage());
                        // 即使检查目录失败，也尝试列出文件
                        exists = true;
                    }
                    
                    if (!exists) {
                        return backupFiles;
                    }
                    
                    // 获取目录中的所有文件
                    List<DavResource> resources = sardine.list(webDavFolderUrl);
                    for (DavResource resource : resources) {
                        if (!resource.isDirectory() && resource.getName().startsWith("goodsticks_backup_") 
                                && resource.getName().endsWith(".json")) {
                            BackupFileInfo fileInfo = new BackupFileInfo();
                            fileInfo.setFileName(resource.getName());
                            fileInfo.setFileSize(resource.getContentLength());
                            fileInfo.setLastModified(resource.getModified());
                            fileInfo.setFileUrl(webDavFolderUrl + resource.getName());
                            backupFiles.add(fileInfo);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "获取WebDAV文件列表失败: " + e.getMessage(), e);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "获取备份文件列表失败", e);
            }
            return backupFiles;
        }, executor);
    }

    /**
     * 从WebDAV恢复备份
     */
    public static CompletableFuture<String> restoreBackup(Context context, String fileUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                WebDavSettings settings = getWebDavSettings(context);
                if (!settings.isEnabled() || settings.getUrl().isEmpty()) {
                    return "WebDAV设置未启用或URL为空";
                }
                
                Sardine sardine = getSardine(context);
                
                // 下载备份文件
                InputStream is = sardine.get(fileUrl);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                String jsonData = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                
                // 解析备份数据
                BackupData backupData = gson.fromJson(jsonData, BackupData.class);
                if (backupData == null || backupData.getUser() == null) {
                    return "备份文件数据无效";
                }
                
                // 恢复用户数据
                UserRepository userRepository = new UserRepository(GoodSticksApplication.getInstance());
                User user = backupData.getUser();
                long userId = userRepository.getUserIdByUsernameSync(user.getUsername());
                
                if (userId == 0) {
                    // 如果用户不存在，则创建新用户
                    userId = userRepository.insertUserSync(user);
                } else {
                    // 如果用户存在，则更新用户信息
                    user.setId(userId);
                    userRepository.updateUserSync(user);
                }
                
                // 恢复笔记数据
                NoteRepository noteRepository = new NoteRepository(GoodSticksApplication.getInstance());
                List<Note> notes = backupData.getNotes();
                if (notes != null && !notes.isEmpty()) {
                    // 删除当前用户的所有笔记
                    noteRepository.deleteAllNotesByUserIdSync(userId);
                    
                    // 插入备份中的笔记
                    for (Note note : notes) {
                        note.setUserId(userId);
                        noteRepository.insertNoteSync(note);
                    }
                }
                
                // 恢复头像
                String avatarBase64 = backupData.getAvatarBase64();
                if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                    SharedPreferences prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
                    String avatarKey = "avatar_" + userId;
                    prefs.edit().putString(avatarKey, avatarBase64).apply();
                }
                
                return "恢复成功: " + userId;
            } catch (Exception e) {
                Log.e(TAG, "恢复备份失败", e);
                return "恢复失败: " + e.getMessage();
            }
        }, executor);
    }

    /**
     * WebDAV设置数据类
     */
    public static class WebDavSettings {
        private String url;
        private String username;
        private String password;
        private String folder;
        private boolean enabled;
        private String lastBackup;

        public WebDavSettings(String url, String username, String password, 
                              String folder, boolean enabled, String lastBackup) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.folder = folder;
            this.enabled = enabled;
            this.lastBackup = lastBackup;
        }

        public String getUrl() {
            return url;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getFolder() {
            return folder;
        }

        public boolean isEnabled() {
            return enabled;
        }
        
        public String getLastBackup() {
            return lastBackup;
        }
    }

    /**
     * 备份文件信息数据类
     */
    public static class BackupFileInfo {
        private String fileName;
        private long fileSize;
        private Date lastModified;
        private String fileUrl;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public Date getLastModified() {
            return lastModified;
        }

        public void setLastModified(Date lastModified) {
            this.lastModified = lastModified;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }
    }

    /**
     * 备份数据类
     */
    public static class BackupData {
        private User user;
        private List<Note> notes;
        private String avatarBase64;

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public List<Note> getNotes() {
            return notes;
        }

        public void setNotes(List<Note> notes) {
            this.notes = notes;
        }

        public String getAvatarBase64() {
            return avatarBase64;
        }

        public void setAvatarBase64(String avatarBase64) {
            this.avatarBase64 = avatarBase64;
        }
    }
} 