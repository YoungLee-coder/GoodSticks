package cn.younglee.goodsticks.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 用户实体类
 */
@Entity(tableName = "users", indices = {@Index(value = {"username"}, unique = true)})
public class User {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "username")
    @NonNull
    private String username;
    
    @ColumnInfo(name = "password")
    @NonNull
    private String password;
    
    @ColumnInfo(name = "email")
    private String email;
    
    @ColumnInfo(name = "nickname")
    private String nickname;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    @ColumnInfo(name = "updated_at")
    private long updatedAt;
    
    public User(@NonNull String username, @NonNull String password) {
        this.username = username;
        this.password = password;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    @NonNull
    public String getUsername() {
        return username;
    }
    
    public void setUsername(@NonNull String username) {
        this.username = username;
    }
    
    @NonNull
    public String getPassword() {
        return password;
    }
    
    public void setPassword(@NonNull String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
} 