package cn.younglee.goodsticks;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationBarView;

import cn.younglee.goodsticks.databinding.ActivityMainBinding;
import cn.younglee.goodsticks.ui.home.HomeFragment;
import cn.younglee.goodsticks.ui.settings.SettingsFragment;
import cn.younglee.goodsticks.utils.ThemeUtils;

public class MainActivity extends AppCompatActivity {
    
    private ActivityMainBinding binding;
    private Fragment homeFragment;
    private Fragment settingsFragment;
    private Fragment activeFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        if (savedInstanceState == null) {
            // 初始化Fragment
            initFragments();
            
            // 检查是否需要显示设置页面
            String lastFragment = getSecurePreferences().getString("last_fragment", "");
                    
            if ("settings".equals(lastFragment)) {
                // 清除记录
                getSecurePreferences().edit().remove("last_fragment").apply();
                        
                // 切换到设置页面
                binding.bottomNavigation.setSelectedItemId(R.id.navigation_settings);
                switchFragment(settingsFragment);
            }
        } else {
            // Activity重建，恢复Fragment引用
            restoreFragments();
            // 恢复底部导航栏的选中状态
            String activeTag = savedInstanceState.getString("active_fragment", "home");
            if ("settings".equals(activeTag)) {
                binding.bottomNavigation.setSelectedItemId(R.id.navigation_settings);
            }
        }
        
        setupBottomNavigation();
    }
    
    private void initFragments() {
        homeFragment = new HomeFragment();
        settingsFragment = new SettingsFragment();
        
        // 正常情况，显示首页
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, settingsFragment, "settings")
                .hide(settingsFragment)
                .add(R.id.fragment_container, homeFragment, "home")
                .commit();
        activeFragment = homeFragment;
    }
    
    private void restoreFragments() {
        // 从FragmentManager中恢复已存在的Fragment
        homeFragment = getSupportFragmentManager().findFragmentByTag("home");
        settingsFragment = getSupportFragmentManager().findFragmentByTag("settings");
        
        // 如果Fragment为null（不应该发生），则创建新的
        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }
        if (settingsFragment == null) {
            settingsFragment = new SettingsFragment();
        }
        
        // 恢复activeFragment
        if (homeFragment.isVisible()) {
            activeFragment = homeFragment;
        } else if (settingsFragment.isVisible()) {
            activeFragment = settingsFragment;
        } else {
            // 默认显示home
            activeFragment = homeFragment;
            getSupportFragmentManager().beginTransaction()
                    .show(homeFragment)
                    .hide(settingsFragment)
                    .commitAllowingStateLoss();
        }
    }
    
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int itemId = item.getItemId();
                
                if (itemId == R.id.navigation_home) {
                    switchFragment(homeFragment);
                    return true;
                } else if (itemId == R.id.navigation_settings) {
                    switchFragment(settingsFragment);
                    return true;
                }
                
                return false;
            }
        });
    }
    
    private void switchFragment(Fragment targetFragment) {
        if (activeFragment != targetFragment) {
            // 使用commitNow确保立即执行，避免异步问题
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.fade_in,
                            R.anim.fade_out
                    )
                    .hide(activeFragment)
                    .show(targetFragment)
                    .commitNow();
            activeFragment = targetFragment;
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存当前活动的Fragment标签
        if (activeFragment == homeFragment) {
            outState.putString("active_fragment", "home");
        } else if (activeFragment == settingsFragment) {
            outState.putString("active_fragment", "settings");
        }
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 恢复底部导航栏的选中状态
        String activeTag = savedInstanceState.getString("active_fragment", "home");
        if ("settings".equals(activeTag) && activeFragment == settingsFragment) {
            binding.bottomNavigation.setSelectedItemId(R.id.navigation_settings);
        } else {
            binding.bottomNavigation.setSelectedItemId(R.id.navigation_home);
        }
    }
    
    private SharedPreferences getSecurePreferences() {
        return GoodSticksApplication.getInstance().getSecureSharedPreferences();
    }
}