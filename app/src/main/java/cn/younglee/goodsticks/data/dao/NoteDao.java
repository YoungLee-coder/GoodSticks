package cn.younglee.goodsticks.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import cn.younglee.goodsticks.data.entity.Note;

@Dao
public interface NoteDao {
    @Insert
    long insert(Note note);
    
    @Update
    void update(Note note);
    
    @Delete
    void delete(Note note);
    
    @Query("DELETE FROM notes WHERE id = :id")
    void deleteById(long id);
    
    @Query("SELECT * FROM notes ORDER BY is_pinned DESC, modified_date DESC")
    LiveData<List<Note>> getAllNotes();
    
    @Query("SELECT * FROM notes WHERE id = :id")
    LiveData<Note> getNoteById(long id);
    
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%' ORDER BY is_pinned DESC, modified_date DESC")
    LiveData<List<Note>> searchNotes(String searchQuery);
    
    @Query("UPDATE notes SET is_pinned = :isPinned WHERE id = :id")
    void updatePinStatus(long id, boolean isPinned);
} 