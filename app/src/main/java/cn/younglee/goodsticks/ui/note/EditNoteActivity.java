package cn.younglee.goodsticks.ui.note;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.younglee.goodsticks.R;
import cn.younglee.goodsticks.data.entity.Note;
import cn.younglee.goodsticks.databinding.ActivityEditNoteBinding;
import cn.younglee.goodsticks.utils.ThemeUtils;

public class EditNoteActivity extends AppCompatActivity {
    
    public static final String EXTRA_NOTE_ID = "note_id";
    
    private ActivityEditNoteBinding binding;
    private NoteViewModel noteViewModel;
    private Note currentNote;
    private String currentPhotoPath;
    
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    
    // Activity Result Launchers
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityEditNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        
        initActivityResultLaunchers();
        loadNote();
        setupViews();
    }
    
    private void initActivityResultLaunchers() {
        // 拍照
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        displayImage(currentPhotoPath);
                    }
                });
        
        // 选择图片
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        currentPhotoPath = uri.toString();
                        displayImage(currentPhotoPath);
                    }
                });
    }
    
    private void loadNote() {
        long noteId = getIntent().getLongExtra(EXTRA_NOTE_ID, -1);
        if (noteId != -1) {
            noteViewModel.getNoteById(noteId).observe(this, note -> {
                if (note != null) {
                    currentNote = note;
                    displayNote();
                }
            });
        } else {
            currentNote = new Note();
            setTitle(R.string.new_note);
        }
    }
    
    private void displayNote() {
        binding.etTitle.setText(currentNote.getTitle());
        binding.etContent.setText(currentNote.getContent());
        
        if (currentNote.getImagePath() != null && !currentNote.getImagePath().isEmpty()) {
            currentPhotoPath = currentNote.getImagePath();
            displayImage(currentPhotoPath);
        }
        
        setTitle(R.string.edit_note);
    }
    
    private void setupViews() {
        binding.btnCamera.setOnClickListener(v -> showImageSourceDialog());
    }
    
    private void showImageSourceDialog() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_from_gallery)};
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_image)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        checkStoragePermission();
                    }
                })
                .show();
    }
    
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }
    
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                pickImageFromGallery();
            }
        } else {
            // Android 12及以下
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                pickImageFromGallery();
            }
        }
    }
    
    private void dispatchTakePictureIntent() {
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureLauncher.launch(photoURI);
            }
        } catch (IOException ex) {
            Toast.makeText(this, R.string.error_create_image_file, Toast.LENGTH_SHORT).show();
        }
    }
    
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    private void pickImageFromGallery() {
        pickImageLauncher.launch("image/*");
    }
    
    private void displayImage(String imagePath) {
        binding.ivImage.setVisibility(View.VISIBLE);
        binding.btnRemoveImage.setVisibility(View.VISIBLE);
        
        Glide.with(this)
                .load(imagePath)
                .centerCrop()
                .into(binding.ivImage);
                
        binding.btnRemoveImage.setOnClickListener(v -> {
            currentPhotoPath = null;
            binding.ivImage.setVisibility(View.GONE);
            binding.btnRemoveImage.setVisibility(View.GONE);
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_note, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_save) {
            saveNote();
            return true;
        } else if (itemId == R.id.action_delete) {
            showDeleteConfirmDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void saveNote() {
        String title = binding.etTitle.getText().toString().trim();
        String content = binding.etContent.getText().toString().trim();
        
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content) && TextUtils.isEmpty(currentPhotoPath)) {
            Toast.makeText(this, R.string.note_empty_warning, Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.setImagePath(currentPhotoPath);
        currentNote.setModifiedDate(new Date());
        
        if (currentNote.getId() == 0) {
            noteViewModel.insert(currentNote);
        } else {
            noteViewModel.update(currentNote);
        }
        
        Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_note)
                .setMessage(R.string.delete_note_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (currentNote.getId() != 0) {
                        noteViewModel.delete(currentNote);
                    }
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery();
            } else {
                Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }
} 