package cn.younglee.goodsticks.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import cn.younglee.goodsticks.data.dao.NoteDao;
import cn.younglee.goodsticks.data.database.NoteDatabase;
import cn.younglee.goodsticks.data.entity.Note;

public class NoteRepository {
    private final NoteDao noteDao;
    private final LiveData<List<Note>> allNotes;
    
    public NoteRepository(Application application) {
        NoteDatabase db = NoteDatabase.getDatabase(application);
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
        NoteDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.insert(note);
        });
    }
    
    public void update(Note note) {
        NoteDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.update(note);
        });
    }
    
    public void delete(Note note) {
        NoteDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.delete(note);
        });
    }
    
    public void deleteById(long id) {
        NoteDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.deleteById(id);
        });
    }
    
    public void updatePinStatus(long id, boolean isPinned) {
        NoteDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.updatePinStatus(id, isPinned);
        });
    }
} 