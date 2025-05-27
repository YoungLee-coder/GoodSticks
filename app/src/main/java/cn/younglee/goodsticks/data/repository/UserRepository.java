package cn.younglee.goodsticks.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.younglee.goodsticks.data.dao.UserDao;
import cn.younglee.goodsticks.data.database.AppDatabase;
import cn.younglee.goodsticks.data.entity.User;

public class UserRepository {
    
    private final UserDao userDao;
    private final Executor executor;
    
    public UserRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        userDao = db.userDao();
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 注册新用户
     * @param username 用户名
     * @param password 密码
     * @return 注册结果，返回用户ID，如果注册失败返回-1
     */
    public CompletableFuture<Long> register(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            // 检查用户名是否已存在
            if (userDao.isUsernameExist(username) > 0) {
                return -1L; // 用户名已存在
            }
            
            // 创建新用户并插入数据库
            User newUser = new User(username, password);
            return userDao.insert(newUser);
        }, executor);
    }
    
    /**
     * 登录验证
     * @param username 用户名
     * @param password 密码
     * @return 登录结果，返回用户对象，如果登录失败返回null
     */
    public CompletableFuture<User> login(String username, String password) {
        return CompletableFuture.supplyAsync(() -> 
            userDao.getUserByUsernameAndPassword(username, password), executor);
    }
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    public boolean isUsernameExists(String username) {
        try {
            return CompletableFuture.supplyAsync(() -> 
                userDao.isUsernameExist(username) > 0, executor).get();
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }
    }
    
    /**
     * 根据ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息LiveData
     */
    public LiveData<User> getUserById(long userId) {
        return userDao.getUserById(userId);
    }
    
    /**
     * 更新用户信息
     * @param user 用户对象
     */
    public void updateUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> userDao.update(user));
    }
} 