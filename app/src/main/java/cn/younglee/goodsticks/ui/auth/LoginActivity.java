package cn.younglee.goodsticks.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import cn.younglee.goodsticks.GoodSticksApplication;
import cn.younglee.goodsticks.MainActivity;
import cn.younglee.goodsticks.R;
import cn.younglee.goodsticks.databinding.ActivityLoginBinding;
import cn.younglee.goodsticks.utils.ThemeUtils;

public class LoginActivity extends AppCompatActivity {
    
    private ActivityLoginBinding binding;
    private SharedPreferences prefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
        
        initViews();
        loadSavedCredentials();
    }
    
    private void initViews() {
        // 登录按钮点击事件
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        
        // 注册文本点击事件
        binding.tvRegister.setOnClickListener(v -> {
            // 为演示，这里直接创建新用户
            showRegisterDialog();
        });
        
        // 记住密码复选框
        binding.cbRememberPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("remember_password", isChecked).apply();
        });
    }
    
    private void loadSavedCredentials() {
        boolean rememberPassword = prefs.getBoolean("remember_password", false);
        binding.cbRememberPassword.setChecked(rememberPassword);
        
        if (rememberPassword) {
            String savedUsername = prefs.getString("username", "");
            String savedPassword = prefs.getString("password", "");
            binding.etUsername.setText(savedUsername);
            binding.etPassword.setText(savedPassword);
        }
    }
    
    private void attemptLogin() {
        // 重置错误
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        
        boolean cancel = false;
        View focusView = null;
        
        // 检查密码
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_field_required));
            focusView = binding.etPassword;
            cancel = true;
        } else if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_invalid_password));
            focusView = binding.etPassword;
            cancel = true;
        }
        
        // 检查用户名
        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.setError(getString(R.string.error_field_required));
            focusView = binding.etUsername;
            cancel = true;
        }
        
        if (cancel) {
            focusView.requestFocus();
        } else {
            performLogin(username, password);
        }
    }
    
    private void performLogin(String username, String password) {
        // 显示进度条
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);
        
        // 模拟登录验证（实际应用中应该连接服务器）
        binding.getRoot().postDelayed(() -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnLogin.setEnabled(true);
            
            // 检查保存的用户名和密码
            String savedUsername = prefs.getString("registered_username", "admin");
            String savedPassword = prefs.getString("registered_password", "123456");
            
            if (username.equals(savedUsername) && password.equals(savedPassword)) {
                // 登录成功
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_logged_in", true);
                editor.putString("username", username);
                
                if (binding.cbRememberPassword.isChecked()) {
                    editor.putString("password", password);
                } else {
                    editor.remove("password");
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
        }, 1000);
    }
    
    private void showRegisterDialog() {
        // 简单的注册逻辑，实际应用中应该有完整的注册流程
        binding.etUsername.setText("admin");
        binding.etPassword.setText("123456");
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("registered_username", "admin");
        editor.putString("registered_password", "123456");
        editor.apply();
        
        Toast.makeText(this, "默认账号已创建\n用户名: admin\n密码: 123456", Toast.LENGTH_LONG).show();
    }
} 