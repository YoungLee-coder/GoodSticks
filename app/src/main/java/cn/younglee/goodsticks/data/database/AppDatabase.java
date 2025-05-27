package cn.younglee.goodsticks.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.younglee.goodsticks.data.converter.DateConverter;
import cn.younglee.goodsticks.data.dao.NoteDao;
import cn.younglee.goodsticks.data.dao.UserDao;
import cn.younglee.goodsticks.data.entity.Note;
import cn.younglee.goodsticks.data.entity.User;

@Database(entities = {Note.class, User.class}, version = 2, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    
    public abstract NoteDao noteDao();
    public abstract UserDao userDao();
    
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    
    // 数据库迁移策略 - 从版本1到版本2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加user_id列，默认值为-1（表示未关联用户）
            database.execSQL("ALTER TABLE notes ADD COLUMN user_id INTEGER NOT NULL DEFAULT -1");
        }
    };
    
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "goodsticks_database")
                            .addMigrations(MIGRATION_1_2) // 使用迁移策略，而不是破坏性重建
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    
    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            
            // 初始化数据库后可以添加默认数据
            databaseWriteExecutor.execute(() -> {
                // 在这里可以初始化一些默认数据，如默认管理员账户等
                UserDao userDao = INSTANCE.userDao();
                // 检查是否已存在用户
                if (userDao.getUserCount() == 0) {
                    User admin = new User("admin", "123456");
                    admin.setNickname("管理员");
                    userDao.insert(admin);
                }
            });
        }
        
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // 数据库打开时执行的操作
        }
    };
} 