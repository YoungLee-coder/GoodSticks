package cn.younglee.goodsticks;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

import cn.younglee.goodsticks.utils.DataMigrationUtil;
import cn.younglee.goodsticks.utils.ThemeUtils;

public class GoodSticksApplication extends Application {
    private static GoodSticksApplication instance;
    private SharedPreferences sharedPreferences;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initEncryptedSharedPreferences();
        
        // 应用保存的主题设置
        ThemeUtils.applyTheme(this);
        
        // 强制使用浅色模式
        sharedPreferences.edit().putBoolean("dark_mode", false).apply();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        // 执行数据迁移
        DataMigrationUtil.migrateNotesToCurrentUser(this);
    }
    
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
            e.printStackTrace();
            // 如果加密失败，使用普通的SharedPreferences
            sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        }
    }
    
    public static GoodSticksApplication getInstance() {
        return instance;
    }
    
    public SharedPreferences getSecureSharedPreferences() {
        return sharedPreferences;
    }
} 