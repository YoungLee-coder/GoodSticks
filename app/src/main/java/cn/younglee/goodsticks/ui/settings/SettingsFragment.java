package cn.younglee.goodsticks.ui.settings;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import cn.younglee.goodsticks.GoodSticksApplication;
import cn.younglee.goodsticks.R;
import cn.younglee.goodsticks.databinding.FragmentSettingsBinding;
import cn.younglee.goodsticks.ui.auth.LoginActivity;
import cn.younglee.goodsticks.utils.ThemeUtils;

public class SettingsFragment extends Fragment {
    
    private FragmentSettingsBinding binding;
    private SharedPreferences prefs;
    private Uri photoUri;
    private ImageView currentAvatarImageView;
    private long currentUserId;
    
    // 拍照启动器
    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result && photoUri != null) {
                    updateAvatar(photoUri);
                }
            });
    
    // 从相册选择图片启动器
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    updateAvatar(uri);
                }
            });
    
    // 相机权限请求启动器
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    takePhoto();
                } else {
                    Toast.makeText(getContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
        currentUserId = prefs.getLong("user_id", 0);
        
        setupViews();
        loadSettings();
    }
    
    private void setupViews() {
        // 用户信息
        String username = prefs.getString("username", "");
        binding.tvUsername.setText(username);
        
        // 加载头像
        loadAvatar();
        
        // 用户信息卡片点击事件
        binding.cardUserInfo.setOnClickListener(v -> showEditProfileDialog());
        
        // 隐藏深色模式开关
        binding.cardDarkMode.setVisibility(View.GONE);
        
        // 主题色选择
        binding.layoutThemeColor.setOnClickListener(v -> showThemeColorDialog());
        
        // 关于
        binding.layoutAbout.setOnClickListener(v -> showAboutDialog());
        
        // 退出登录
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
    }
    
    private void loadSettings() {
        // 显示当前主题色
        ThemeUtils.Theme currentTheme = ThemeUtils.getCurrentTheme();
        binding.tvCurrentTheme.setText(currentTheme.getName());
        binding.viewThemeColor.setBackgroundColor(currentTheme.getColorInt());
    }
    
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
                    
                    // 通知主题已更改，将在下次启动应用时生效
                    showThemeChangeNotice();
                })
                .show();
    }
    
    private void showThemeChangeNotice() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.theme_changed)
                .setMessage(R.string.theme_change_notice)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
    
    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.about)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
    
    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    // 清除登录状态
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("is_logged_in", false);
                    editor.putBoolean("remember_password", false);
                    editor.remove("password");
                    // 不删除头像数据，让每个用户保留自己的头像
                    editor.apply();
                    
                    // 跳转到登录页
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText etUsername = dialogView.findViewById(R.id.et_username);
        ImageView ivAvatar = dialogView.findViewById(R.id.iv_dialog_avatar);
        
        // 设置当前信息
        String currentUsername = prefs.getString("username", "");
        etUsername.setText(currentUsername);
        
        // 加载当前头像到对话框
        String avatarKey = "avatar_" + currentUserId;
        String avatarBase64 = prefs.getString(avatarKey, "");
        if (!TextUtils.isEmpty(avatarBase64)) {
            byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            ivAvatar.setImageBitmap(bitmap);
            ivAvatar.setColorFilter(null);
        }
        
        currentAvatarImageView = ivAvatar;
        
        // 头像更换按钮
        dialogView.findViewById(R.id.btn_change_avatar).setOnClickListener(v -> showAvatarOptions());
        
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_profile)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newUsername = etUsername.getText().toString().trim();
                    if (!TextUtils.isEmpty(newUsername)) {
                        prefs.edit().putString("username", newUsername).apply();
                        binding.tvUsername.setText(newUsername);
                        Toast.makeText(getContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void showAvatarOptions() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_from_gallery)};
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_avatar)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 拍照
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            takePhoto();
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        // 从相册选择
                        pickImageLauncher.launch("image/*");
                    }
                })
                .show();
    }
    
    private void takePhoto() {
        try {
            File photoFile = new File(requireContext().getCacheDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
            photoUri = FileProvider.getUriForFile(requireContext(), 
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(photoUri);
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.error_create_image_file, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateAvatar(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            // 压缩图片
            int maxSize = 512;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            if (width > maxSize || height > maxSize) {
                float scale = Math.min((float) maxSize / width, (float) maxSize / height);
                width = Math.round(scale * width);
                height = Math.round(scale * height);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }
            
            // 转换为Base64保存
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] bytes = baos.toByteArray();
            String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
            
            // 保存到SharedPreferences，使用用户ID区分
            String avatarKey = "avatar_" + currentUserId;
            prefs.edit().putString(avatarKey, base64).apply();
            
            // 更新UI
            if (currentAvatarImageView != null) {
                currentAvatarImageView.setImageBitmap(bitmap);
                currentAvatarImageView.setColorFilter(null);
            }
            binding.ivAvatar.setImageBitmap(bitmap);
            binding.ivAvatar.setColorFilter(null);
            
            Toast.makeText(getContext(), R.string.avatar_updated, Toast.LENGTH_SHORT).show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadAvatar() {
        // 使用用户ID获取对应的头像
        String avatarKey = "avatar_" + currentUserId;
        String avatarBase64 = prefs.getString(avatarKey, "");
        if (!TextUtils.isEmpty(avatarBase64)) {
            byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            binding.ivAvatar.setImageBitmap(bitmap);
            binding.ivAvatar.setColorFilter(null);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 