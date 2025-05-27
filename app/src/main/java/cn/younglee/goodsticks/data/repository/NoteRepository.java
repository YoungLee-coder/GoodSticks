package cn.younglee.goodsticks.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import cn.younglee.goodsticks.data.dao.NoteDao;
import cn.younglee.goodsticks.data.database.AppDatabase;
import cn.younglee.goodsticks.data.entity.Note;

public class NoteRepository {
    private final NoteDao noteDao;
    private final LiveData<List<Note>> allNotes;
    
    public NoteRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        noteDao = db.noteDao();
        allNotes = noteDao.getAllNotes();
    }
    
    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }
    
    public LiveData<Note> getNoteById(long id) {
        return noteDao.getNoteById(id);
    }
    
    public LiveData<List<Note>> searchNotes(String searchQuery) {
        return noteDao.searchNotes(searchQuery);
    }
    
    public void insert(Note note) {
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