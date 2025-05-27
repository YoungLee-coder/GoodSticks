package cn.younglee.goodsticks.data.repository;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import java.util.List;

import cn.younglee.goodsticks.GoodSticksApplication;
import cn.younglee.goodsticks.data.dao.NoteDao;
import cn.younglee.goodsticks.data.database.AppDatabase;
import cn.younglee.goodsticks.data.entity.Note;

public class NoteRepository {
    private final NoteDao noteDao;
    private final LiveData<List<Note>> allNotes;
    private final long currentUserId;
    private LiveData<List<Note>> userNotes;
    
    public NoteRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        noteDao = db.noteDao();
        
        // 从SharedPreferences获取当前用户ID
        SharedPreferences prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
        currentUserId = prefs.getLong("user_id", -1);
        
        if (currentUserId != -1) {
            userNotes = noteDao.getNotesByUserId(currentUserId);
            allNotes = userNotes; // 返回当前用户的笔记
        } else {
            allNotes = noteDao.getAllNotes(); // 如果没有用户ID，返回所有笔记（这种情况不应该发生）
        }
    }
    
    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }
    
    public LiveData<Note> getNoteById(long id) {
        return noteDao.getNoteById(id);
    }
    
    public LiveData<List<Note>> searchNotes(String searchQuery) {
        if (currentUserId != -1) {
            return noteDao.searchNotesByUserId(currentUserId, searchQuery);
        } else {
            return noteDao.searchNotes(searchQuery);
        }
    }
    
    public void insert(Note note) {
        if (currentUserId != -1) {
            note.setUserId(currentUserId);
        }
        AppDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.insert(note);
        });
    }
    
    public void update(Note note) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.update(note);
        });
    }
    
    public void delete(Note note) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.delete(note);
        });
    }
    
    public void deleteById(long id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.deleteById(id);
        });
    }
    
    public void updatePinStatus(long id, boolean isPinned) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.updatePinStatus(id, isPinned);
        });
    }
} 