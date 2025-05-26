package cn.younglee.goodsticks.ui.note;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import cn.younglee.goodsticks.data.entity.Note;
import cn.younglee.goodsticks.data.repository.NoteRepository;

public class NoteViewModel extends AndroidViewModel {
    
    private final NoteRepository repository;
    private final LiveData<List<Note>> allNotes;
    
    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);
        allNotes = repository.getAllNotes();
    }
    
    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }
    
    public LiveData<Note> getNoteById(long id) {
        return repository.getNoteById(id);
    }
    
    public LiveData<List<Note>> searchNotes(String searchQuery) {
        return repository.searchNotes(searchQuery);
    }
    
    public void insert(Note note) {
        repository.insert(note);
    }
    
    public void update(Note note) {
        repository.update(note);
    }
    
    public void delete(Note note) {
        repository.delete(note);
    }
    
    public void deleteById(long id) {
        repository.deleteById(id);
    }
    
    public void updatePinStatus(long id, boolean isPinned) {
        repository.updatePinStatus(id, isPinned);
    }
} 