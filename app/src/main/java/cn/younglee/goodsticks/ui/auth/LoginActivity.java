package cn.younglee.goodsticks.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import cn.younglee.goodsticks.GoodSticksApplication;
import cn.younglee.goodsticks.MainActivity;
import cn.younglee.goodsticks.R;
import cn.younglee.goodsticks.data.entity.User;
import cn.younglee.goodsticks.data.repository.UserRepository;
import cn.younglee.goodsticks.databinding.ActivityLoginBinding;
import cn.younglee.goodsticks.utils.ThemeUtils;

public class LoginActivity extends AppCompatActivity {
    
    private ActivityLoginBinding binding;
    private SharedPreferences prefs;
    private UserRepository userRepository;
    
    private final ActivityResultLauncher<Intent> registerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String username = result.getData().getStringExtra("username");
                    if (username != null) {
                        binding.etUsername.setText(username);
                        binding.etPassword.requestFocus();
                    }
                }
            });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
        userRepository = new UserRepository(getApplication());
        
        initViews();
        loadSavedCredentials();
    }
    
    private void initViews() {
        // 登录按钮点击事件
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        
        // 注册文本点击事件
        binding.tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            registerLauncher.launch(intent);
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
        
        userRepository.login(username, password).thenAccept(user -> {
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnLogin.setEnabled(true);
                
                if (user != null) {
                    // 登录成功
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("is_logged_in", true);
                    editor.putString("username", username);
                    editor.putLong("user_id", user.getId());
                    
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
            });
        }).exceptionally(throwable -> {
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnLogin.setEnabled(true);
                Snackbar.make(binding.getRoot(), R.string.error_incorrect_password, Snackbar.LENGTH_LONG).show();
            });
            return null;
        });
    }
} 