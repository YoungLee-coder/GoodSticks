package cn.younglee.goodsticks.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import cn.younglee.goodsticks.GoodSticksApplication;
import cn.younglee.goodsticks.data.database.AppDatabase;

/**
 * 数据迁移工具类，处理数据库升级相关的用户数据迁移
 */
public class DataMigrationUtil {
    private static final String TAG = "DataMigrationUtil";
    private static final String MIGRATION_DONE_KEY = "migration_1_2_done";
    
    /**
     * 将无用户ID的记事本关联到当前登录用户
     */
    public static void migrateNotesToCurrentUser(Context context) {
        SharedPreferences prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
        boolean migrationDone = prefs.getBoolean(MIGRATION_DONE_KEY, false);
        
        // 检查是否已经执行过迁移
        if (migrationDone) {
            return;
        }
        
        // 检查用户是否登录
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        if (!isLoggedIn) {
            return;
        }
        
        // 获取当前用户ID
        long userId = prefs.getLong("user_id", -1);
        if (userId == -1) {
            return;
        }
        
        // 执行数据库操作，将无用户ID的笔记关联到当前用户
        AppDatabase db = AppDatabase.getDatabase(context);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 执行SQL更新操作
                String sql = "UPDATE notes SET user_id = " + userId + " WHERE user_id = -1 OR user_id IS NULL";
                db.getOpenHelper().getWritableDatabase().execSQL(sql);
                
                // 标记迁移已完成
                prefs.edit().putBoolean(MIGRATION_DONE_KEY, true).apply();
                
                            Log.i(TAG, "记事本数据迁移完成，关联到用户ID: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "记事本数据迁移失败", e);
            }
        });
    }
} 