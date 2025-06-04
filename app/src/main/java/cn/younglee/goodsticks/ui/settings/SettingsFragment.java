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
import android.widget.ProgressBar;
import android.widget.TextView;
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
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.younglee.goodsticks.GoodSticksApplication;
import cn.younglee.goodsticks.R;
import cn.younglee.goodsticks.data.entity.User;
import cn.younglee.goodsticks.data.repository.UserRepository;
import cn.younglee.goodsticks.databinding.FragmentSettingsBinding;
import cn.younglee.goodsticks.ui.auth.LoginActivity;
import cn.younglee.goodsticks.utils.ThemeUtils;
import cn.younglee.goodsticks.utils.WebDavUtils;

public class SettingsFragment extends Fragment {
    
    private FragmentSettingsBinding binding;
    private SharedPreferences prefs;
    private Uri photoUri;
    private ImageView currentAvatarImageView;
    private long currentUserId;
    private UserRepository userRepository;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private BackupFileAdapter backupFileAdapter;
    
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
        userRepository = new UserRepository(requireActivity().getApplication());
        
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
        
        // WebDAV设置
        binding.cardWebdavSettings.setOnClickListener(v -> showWebDavSettingsDialog());
        
        // 备份
        binding.cardBackup.setOnClickListener(v -> createBackup());
        
        // 恢复
        binding.cardRestore.setOnClickListener(v -> showBackupFilesDialog());
        
        // 关于
        binding.layoutAbout.setOnClickListener(v -> showAboutDialog());
        
        // 仓库信息
        binding.layoutRepository.setOnClickListener(v -> openRepository());
        
        // 退出登录
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
    }
    
    private void loadSettings() {
        // 显示当前主题色
        ThemeUtils.Theme currentTheme = ThemeUtils.getCurrentTheme();
        binding.tvCurrentTheme.setText(currentTheme.getName());
        binding.viewThemeColor.setBackgroundColor(currentTheme.getColorInt());
        
        // 加载WebDAV状态
        WebDavUtils.WebDavSettings webDavSettings = WebDavUtils.getWebDavSettings(requireContext());
        if (webDavSettings.isEnabled() && !webDavSettings.getUrl().isEmpty()) {
            binding.tvWebdavStatus.setText(R.string.configured);
            
            // 显示最近备份时间
            String lastBackup = webDavSettings.getLastBackup();
            if (!TextUtils.isEmpty(lastBackup)) {
                binding.tvLastBackupTime.setText(getString(R.string.last_backup, lastBackup));
            } else {
                binding.tvLastBackupTime.setText(R.string.never_backed_up);
            }
        } else {
            binding.tvWebdavStatus.setText(R.string.not_configured);
            binding.tvLastBackupTime.setText(R.string.never_backed_up);
        }
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
    
    private void openRepository() {
        try {
            String repositoryUrl = getString(R.string.repository_url);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(repositoryUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.cannot_open_browser, Toast.LENGTH_SHORT).show();
        }
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
                        String oldUsername = prefs.getString("username", "");
                        if (!newUsername.equals(oldUsername)) {
                            // 检查新用户名是否已存在
                            boolean usernameExists = userRepository.isUsernameExists(newUsername);
                            if (usernameExists) {
                                Toast.makeText(getContext(), R.string.username_already_exists, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // 更新SharedPreferences
                            prefs.edit().putString("username", newUsername).apply();
                            
                            // 更新数据库中的用户信息
                            updateUserInDatabase(newUsername);
                            
                            // 更新UI
                            binding.tvUsername.setText(newUsername);
                            Toast.makeText(getContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void updateUserInDatabase(String newUsername) {
        LiveData<User> userLiveData = userRepository.getUserById(currentUserId);
        userLiveData.observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                user.setUsername(newUsername);
                user.setUpdatedAt(System.currentTimeMillis());
                userRepository.updateUser(user);
                userLiveData.removeObservers(getViewLifecycleOwner());
            }
        });
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
    
    /**
     * 显示WebDAV设置对话框
     */
    private void showWebDavSettingsDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_webdav_settings, null);
        
        SwitchMaterial switchEnable = dialogView.findViewById(R.id.switch_enable_webdav);
        TextInputEditText etUrl = dialogView.findViewById(R.id.et_webdav_url);
        TextInputEditText etUsername = dialogView.findViewById(R.id.et_webdav_username);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_webdav_password);
        TextInputEditText etFolder = dialogView.findViewById(R.id.et_webdav_folder);
        
        // 加载当前设置
        WebDavUtils.WebDavSettings settings = WebDavUtils.getWebDavSettings(requireContext());
        switchEnable.setChecked(settings.isEnabled());
        etUrl.setText(settings.getUrl());
        etUsername.setText(settings.getUsername());
        etPassword.setText(settings.getPassword());
        etFolder.setText(settings.getFolder());
        
        // 测试连接按钮
        dialogView.findViewById(R.id.btn_test_connection).setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(requireContext(), R.string.error_field_required, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 显示加载提示
            Toast.makeText(requireContext(), R.string.connection_successful, Toast.LENGTH_SHORT).show();
            
            // 测试连接
            WebDavUtils.testConnection(url, username, password).thenAccept(result -> {
                requireActivity().runOnUiThread(() -> {
                    if (result) {
                        Toast.makeText(requireContext(), R.string.connection_successful, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.connection_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.webdav_settings)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        
        dialog.setOnShowListener(d -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                // 保存设置
                String url = etUrl.getText().toString().trim();
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String folder = etFolder.getText().toString().trim();
                boolean enabled = switchEnable.isChecked();
                
                if (enabled && TextUtils.isEmpty(url)) {
                    Toast.makeText(requireContext(), R.string.error_field_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                WebDavUtils.saveWebDavSettings(requireContext(), url, username, password, folder, enabled);
                
                // 更新UI
                if (enabled && !url.isEmpty()) {
                    binding.tvWebdavStatus.setText(R.string.configured);
                } else {
                    binding.tvWebdavStatus.setText(R.string.not_configured);
                }
                
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
    
    /**
     * 创建WebDAV备份
     */
    private void createBackup() {
        // 检查WebDAV是否已配置
        WebDavUtils.WebDavSettings settings = WebDavUtils.getWebDavSettings(requireContext());
        if (!settings.isEnabled() || settings.getUrl().isEmpty()) {
            showWebDavSettingsDialog();
            return;
        }
        
        // 显示正在备份提示
        AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.backup_data)
                .setMessage(R.string.backup_in_progress)
                .setCancelable(false)
                .create();
        progressDialog.show();
        
        // 执行备份
        WebDavUtils.createBackup(requireContext(), currentUserId).thenAccept(result -> {
            requireActivity().runOnUiThread(() -> {
                progressDialog.dismiss();
                
                if (result.startsWith("备份成功")) {
                    Toast.makeText(requireContext(), R.string.backup_successful, Toast.LENGTH_SHORT).show();
                    
                    // 更新最后备份时间显示
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String formattedDate = sdf.format(new Date());
                    binding.tvLastBackupTime.setText(getString(R.string.last_backup, formattedDate));
                } else {
                    Toast.makeText(requireContext(), getString(R.string.backup_failed, result), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    
    /**
     * 显示备份文件列表对话框
     */
    private void showBackupFilesDialog() {
        // 检查WebDAV是否已配置
        WebDavUtils.WebDavSettings settings = WebDavUtils.getWebDavSettings(requireContext());
        if (!settings.isEnabled() || settings.getUrl().isEmpty()) {
            showWebDavSettingsDialog();
            return;
        }
        
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_backup_list, null);
        RecyclerView rvBackupList = dialogView.findViewById(R.id.rv_backup_list);
        ProgressBar progressLoading = dialogView.findViewById(R.id.progress_loading_backups);
        TextView tvNoBackups = dialogView.findViewById(R.id.tv_no_backups);
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.restore_data)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .create();
        
        // 设置RecyclerView
        rvBackupList.setLayoutManager(new LinearLayoutManager(requireContext()));
        backupFileAdapter = new BackupFileAdapter(backupFile -> onBackupFileClick(backupFile));
        rvBackupList.setAdapter(backupFileAdapter);
        
        // 加载备份文件列表
        WebDavUtils.getBackupFiles(requireContext()).thenAccept(backupFiles -> {
            requireActivity().runOnUiThread(() -> {
                progressLoading.setVisibility(View.GONE);
                
                if (backupFiles.isEmpty()) {
                    tvNoBackups.setVisibility(View.VISIBLE);
                } else {
                    rvBackupList.setVisibility(View.VISIBLE);
                    backupFileAdapter.setBackupFiles(backupFiles);
                }
            });
        });
        
        dialog.show();
    }
    
    public void onBackupFileClick(WebDavUtils.BackupFileInfo backupFile) {
        // 确认恢复对话框
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_restore)
                .setMessage(R.string.confirm_restore_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    restoreBackup(backupFile.getFileUrl());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    /**
     * 从WebDAV恢复备份
     */
    private void restoreBackup(String fileUrl) {
        // 显示正在恢复提示
        AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.restore_data)
                .setMessage(R.string.restore_in_progress)
                .setCancelable(false)
                .create();
        progressDialog.show();
        
        // 执行恢复
        WebDavUtils.restoreBackup(requireContext(), fileUrl).thenAccept(result -> {
            requireActivity().runOnUiThread(() -> {
                progressDialog.dismiss();
                
                if (result.startsWith("恢复成功")) {
                    // 仅显示成功提示，不强制退出登录
                    Toast.makeText(requireContext(), R.string.restore_successful, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.restore_failed, result), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        executor.shutdown();
    }
    
    /**
     * 备份文件列表适配器
     */
    public static class BackupFileAdapter extends RecyclerView.Adapter<BackupFileAdapter.ViewHolder> {
        
        private List<WebDavUtils.BackupFileInfo> backupFiles;
        private final OnBackupFileClickListener listener;
        
        public BackupFileAdapter(OnBackupFileClickListener listener) {
            this.listener = listener;
        }
        
        public void setBackupFiles(List<WebDavUtils.BackupFileInfo> backupFiles) {
            this.backupFiles = backupFiles;
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backup_file, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WebDavUtils.BackupFileInfo backupFile = backupFiles.get(position);
            holder.bind(backupFile, listener);
        }
        
        @Override
        public int getItemCount() {
            return backupFiles == null ? 0 : backupFiles.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvBackupName;
            private final TextView tvBackupDate;
            private final TextView tvBackupSize;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvBackupName = itemView.findViewById(R.id.tv_backup_name);
                tvBackupDate = itemView.findViewById(R.id.tv_backup_date);
                tvBackupSize = itemView.findViewById(R.id.tv_backup_size);
            }
            
            public void bind(WebDavUtils.BackupFileInfo backupFile, OnBackupFileClickListener listener) {
                tvBackupName.setText(backupFile.getFileName());
                
                // 格式化日期
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String dateStr = sdf.format(backupFile.getLastModified());
                tvBackupDate.setText(itemView.getContext().getString(R.string.backup_date, dateStr));
                
                // 格式化文件大小
                String sizeStr = formatFileSize(backupFile.getFileSize());
                tvBackupSize.setText(itemView.getContext().getString(R.string.backup_size, sizeStr));
                
                itemView.setOnClickListener(v -> listener.onBackupFileClick(backupFile));
            }
            
            private String formatFileSize(long size) {
                if (size < 1024) {
                    return size + " B";
                } else if (size < 1024 * 1024) {
                    return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
                } else {
                    return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024));
                }
            }
        }
        
        public interface OnBackupFileClickListener {
            void onBackupFileClick(WebDavUtils.BackupFileInfo backupFile);
        }
    }
} 