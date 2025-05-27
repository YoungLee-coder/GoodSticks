package cn.younglee.goodsticks.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import cn.younglee.goodsticks.data.entity.User;

@Dao
public interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(User user);
    
    @Update
    void update(User user);
    
    @Delete
    void delete(User user);
    
    @Query("SELECT * FROM users WHERE username = :username")
    User getUserByUsername(String username);
    
    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    User getUserByUsernameAndPassword(String username, String password);
    
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    int isUsernameExist(String username);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserById(long userId);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserByIdSync(long userId);
    
    @Query("SELECT id FROM users WHERE username = :username")
    long getUserIdByUsername(String username);
    
    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
} 