package cn.younglee.goodsticks.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import cn.younglee.goodsticks.R;
import cn.younglee.goodsticks.data.repository.UserRepository;
import cn.younglee.goodsticks.databinding.ActivityRegisterBinding;
import cn.younglee.goodsticks.utils.ThemeUtils;

public class RegisterActivity extends AppCompatActivity {
    
    private ActivityRegisterBinding binding;
    private UserRepository userRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        userRepository = new UserRepository(getApplication());
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        // 返回登录页面
        binding.tvLogin.setOnClickListener(v -> {
            finish();
        });
        
        // 注册按钮点击事件
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        
        // 添加文本变化监听器
        binding.etUsername.addTextChangedListener(new TextValidator(binding.tilUsername));
        binding.etPassword.addTextChangedListener(new TextValidator(binding.tilPassword));
        binding.etConfirmPassword.addTextChangedListener(new TextValidator(binding.tilConfirmPassword));
    }
    
    private void setupListeners() {
        // 用户名失去焦点时验证用户名是否已存在
        binding.etUsername.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String username = binding.etUsername.getText().toString().trim();
                if (!TextUtils.isEmpty(username) && username.length() >= 3) {
                    validateUsername(username);
                }
            }
        });
    }
    
    private void validateUsername(String username) {
        // 验证用户名是否已存在
        try {
            if (userRepository.isUsernameExists(username)) {
                binding.tilUsername.setError(getString(R.string.username_exists));
            } else {
                binding.tilUsername.setError(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void attemptRegister() {
        // 重置错误
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
        binding.tilEmail.setError(null);
        
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        
        boolean cancel = false;
        View focusView = null;
        
        // 检查用户名
        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.setError(getString(R.string.error_field_required));
            focusView = binding.etUsername;
            cancel = true;
        } else if (username.length() < 3 || username.length() > 20) {
            binding.tilUsername.setError(getString(R.string.username_rule));
            focusView = binding.etUsername;
            cancel = true;
        }
        
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
        
        // 检查确认密码
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_field_required));
            focusView = binding.etConfirmPassword;
            cancel = true;
        } else if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_not_match));
            focusView = binding.etConfirmPassword;
            cancel = true;
        }
        
        // 检查邮箱(可选)
        if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_field_required));
            focusView = binding.etEmail;
            cancel = true;
        }
        
        if (cancel) {
            focusView.requestFocus();
        } else {
            performRegister(username, password, email);
        }
    }
    
    private void performRegister(String username, String password, String email) {
        // 显示进度条
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);
        
        userRepository.register(username, password).thenAccept(userId -> {
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnRegister.setEnabled(true);
                
                if (userId > 0) {
                    // 注册成功，跳转到登录页面
                    Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                    
                    // 返回登录页面并自动填充用户名
                    Intent intent = new Intent();
                    intent.putExtra("username", username);
                    setResult(RESULT_OK, intent);
                    finish();
                } else if (userId == -1) {
                    // 用户名已存在
                    binding.tilUsername.setError(getString(R.string.username_exists));
                    binding.etUsername.requestFocus();
                } else {
                    // 注册失败
                    Snackbar.make(binding.getRoot(), R.string.registration_failed, Snackbar.LENGTH_LONG).show();
                }
            });
        }).exceptionally(throwable -> {
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnRegister.setEnabled(true);
                Snackbar.make(binding.getRoot(), R.string.registration_failed, Snackbar.LENGTH_LONG).show();
            });
            return null;
        });
    }
    
    // 文本验证辅助类
    private class TextValidator implements TextWatcher {
        private final com.google.android.material.textfield.TextInputLayout textInputLayout;
        
        TextValidator(com.google.android.material.textfield.TextInputLayout til) {
            this.textInputLayout = til;
        }
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // 不需要实现
        }
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // 清除错误提示
            textInputLayout.setError(null);
        }
        
        @Override
        public void afterTextChanged(Editable s) {
            // 不需要实现
        }
    }
} 