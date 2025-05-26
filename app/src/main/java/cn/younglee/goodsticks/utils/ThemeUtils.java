package cn.younglee.goodsticks.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

import cn.younglee.goodsticks.GoodSticksApplication;
import cn.younglee.goodsticks.R;

public class ThemeUtils {
    
    public enum Theme {
        BLUE("溏心蓝", "#1140b6", R.style.Theme_GoodSticks_Blue),
        RED("枫叶红", "#ab3b2d", R.style.Theme_GoodSticks_Red),
        PINK("活力粉", "#fe578d", R.style.Theme_GoodSticks_Pink),
        PURPLE("闪电紫", "#bd90e8", R.style.Theme_GoodSticks_Purple),
        YELLOW("便单黄", "#eca113", R.style.Theme_GoodSticks_Yellow),
        GREEN("宝石绿", "#0dae68", R.style.Theme_GoodSticks_Green),
        CYAN("草木青", "#33bfb6", R.style.Theme_GoodSticks_Cyan);
        
        private final String name;
        private final String color;
        private final int styleResId;
        
        Theme(String name, String color, int styleResId) {
            this.name = name;
            this.color = color;
            this.styleResId = styleResId;
        }
        
        public String getName() {
            return name;
        }
        
        public String getColor() {
            return color;
        }
        
        public int getColorInt() {
            return Color.parseColor(color);
        }
        
        public int getStyleResId() {
            return styleResId;
        }
    }
    
    private static final String PREF_THEME = "app_theme";
    
    public static void applyTheme(Context context) {
        SharedPreferences prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
        String themeName = prefs.getString(PREF_THEME, Theme.BLUE.name());
        Theme theme = Theme.valueOf(themeName);
        context.setTheme(theme.getStyleResId());
    }
    
    public static void saveTheme(Context context, Theme theme) {
        SharedPreferences.Editor editor = GoodSticksApplication.getInstance()
                .getSecureSharedPreferences().edit();
        editor.putString(PREF_THEME, theme.name());
        editor.apply();
    }
    
    public static Theme getCurrentTheme() {
        SharedPreferences prefs = GoodSticksApplication.getInstance().getSecureSharedPreferences();
        String themeName = prefs.getString(PREF_THEME, Theme.BLUE.name());
        return Theme.valueOf(themeName);
    }
    
    public static int getThemeColor(Context context, int attrId) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attrId, typedValue, true);
        return typedValue.data;
    }
} 