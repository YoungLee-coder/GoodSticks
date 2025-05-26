package cn.younglee.goodsticks.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import cn.younglee.goodsticks.GoodSticksApplication;
import cn.younglee.goodsticks.MainActivity;
import cn.younglee.goodsticks.databinding.ActivitySplashBinding;
import cn.younglee.goodsticks.ui.auth.LoginActivity;
import cn.younglee.goodsticks.utils.ThemeUtils;

public class SplashActivity extends AppCompatActivity {
    
    private ActivitySplashBinding binding;
    private static final int SPLASH_DURATION = 2000; // 2秒
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 启动动画
        startAnimation();
        
        // 延迟后跳转
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAndNavigate, SPLASH_DURATION);
    }
    
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
                
        // Slogan 渐入动画
        binding.tvSlogan.setAlpha(0f);
        binding.tvSlogan.animate()
                .alpha(1f)
                .setStartDelay(800)
                .setDuration(1000)
                .setListener(null);
    }
    
    private void checkAndNavigate() {
        SharedPreferences prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        boolean rememberPassword = prefs.getBoolean("remember_password", false);
        
        Intent intent;
        if (isLoggedIn && rememberPassword) {
            // 已登录且记住密码，直接进入主页
            intent = new Intent(this, MainActivity.class);
        } else {
            // 未登录或未记住密码，进入登录页
            intent = new Intent(this, LoginActivity.class);
        }
        
        startActivity(intent);
        finish();
    }
} 