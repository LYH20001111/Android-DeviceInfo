package com.newland.deviceinformation;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.nfc.NfcAdapter;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private TableLayout mTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTable = findViewById(R.id.table_info);

        for (String[] row : buildDeviceInfo()) {
            if ("header".equals(row[0])) {
                addHeaderRow(row[1]);
            } else if ("cutout".equals(row[0])) {
                addCutoutRow(row[1]);
            } else {
                addDataRow(row[1], row[2], "highlight".equals(row[0]));
            }
        }
    }

    /**
     * 添加分组标题行。
     * 直接作为 TableLayout 的子 View 添加（官方标准用法），
     * 自动显示为跨所有列的整行，深紫色背景白字，与表头风格一致。
     */
    private void addHeaderRow(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackgroundColor(Color.parseColor("#6200EE"));
        TableLayout.LayoutParams lp = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        tv.setLayoutParams(lp);
        mTable.addView(tv);
    }

    /** 添加数据行：左侧属性名，右侧属性值；highlight 为 true 时红色突出显示。返回右侧值 TextView */
    private TextView addDataRow(String label, String value, boolean highlight) {
        TableRow row = new TableRow(this);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tvLabel.setPadding(dp(12), dp(8), dp(8), dp(8));

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvValue.setGravity(Gravity.END);
        tvValue.setPadding(dp(8), dp(8), dp(12), dp(8));

        if (highlight) {
            int accent = Color.parseColor("#D32F2F");
            tvLabel.setTextColor(accent);
            tvValue.setTextColor(accent);
            tvLabel.setBackgroundColor(Color.parseColor("#FFEBEE"));
            tvValue.setBackgroundColor(Color.parseColor("#FFEBEE"));
        }

        row.addView(tvLabel, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 3f));
        row.addView(tvValue, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));
        mTable.addView(row);
        return tvValue;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    /** 构建设备信息行数据：{类型, 名称, 值}，类型为 "header" 分组标题 / "highlight" 高亮行 / "data" 普通行 / "cutout" 刘海屏行 */
    private List<String[]> buildDeviceInfo() {
        List<String[]> rows = new ArrayList<>();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Configuration config = getResources().getConfiguration();
        DisplayManager dmService = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display primaryDisplay = dmService.getDisplay(Display.DEFAULT_DISPLAY);

        // 设备信息
        rows.add(new String[]{"header", "设备信息"});
        rows.add(new String[]{"highlight", "设备型号", Build.MODEL});
        rows.add(new String[]{"data", "制造商", Build.MANUFACTURER});
        rows.add(new String[]{"data", "品牌", Build.BRAND});
        rows.add(new String[]{"data", "设备代号", Build.DEVICE});
        rows.add(new String[]{"data", "硬件", Build.HARDWARE});
        rows.add(new String[]{"data", "主板", Build.BOARD});
        rows.add(new String[]{"data", "Android 版本",
                Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"});
        rows.add(new String[]{"data", "状态栏高度", getStatusBarHeight() + " px"});
        rows.add(new String[]{"data", "导航栏高度", getNavigationBarHeight() + " px"});

        // 硬件信息
        rows.add(new String[]{"header", "硬件信息"});
        rows.add(new String[]{"data", "CPU 架构", String.join(", ", Build.SUPPORTED_ABIS)});
        rows.add(new String[]{"data", "CPU 核心数",
                Runtime.getRuntime().availableProcessors() + " 核"});
        rows.add(new String[]{"data", "GPU 型号", getGpuRenderer()});
        rows.add(new String[]{"data", "OpenGL ES 版本", getGlesVersion()});
        rows.add(new String[]{"data", "运行内存", getMemoryInfo()});
        rows.add(new String[]{"data", "内部存储", getStorageInfo()});
        rows.add(new String[]{"data", "外置 SD 卡", getSdCardInfo()});

        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            rows.add(new String[]{"data", "电池电量",
                    level >= 0 && scale > 0 ? Math.round(level * 100f / scale) + "%" : "未知"});
            rows.add(new String[]{"data", "充电状态", getChargeStatus(battery)});
            int temp = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            rows.add(new String[]{"data", "电池温度",
                    temp >= 0 ? String.format(Locale.US, "%.1f °C", temp / 10f) : "未知"});
        }

        rows.add(new String[]{"data", "摄像头", getCameraInfo()});
        rows.add(new String[]{"data", "NFC",
                NfcAdapter.getDefaultAdapter(this) != null ? "支持" : "不支持"});
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sensorNames = new StringBuilder();
        for (Sensor s : sensors) {
            if (sensorNames.length() > 0) {
                sensorNames.append(", ");
            }
            sensorNames.append(s.getName());
        }
        rows.add(new String[]{"data", "传感器 (" + sensors.size() + " 个)", sensorNames.toString()});

        // 系统信息
        rows.add(new String[]{"header", "系统信息"});
        rows.add(new String[]{"data", "系统版本号", Build.DISPLAY});
        rows.add(new String[]{"data", "安全补丁级别", Build.VERSION.SECURITY_PATCH});
        rows.add(new String[]{"data", "内核版本", System.getProperty("os.version", "未知")});
        rows.add(new String[]{"data", "运行时间", formatUptime(SystemClock.elapsedRealtime())});
        rows.add(new String[]{"data", "系统语言", config.getLocales().toLanguageTags()});
        rows.add(new String[]{"data", "时区", TimeZone.getDefault().getID()});
        rows.add(new String[]{"data", "开发者选项",
                getGlobalSwitch(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED)});
        rows.add(new String[]{"data", "USB 调试", getGlobalSwitch(Settings.Global.ADB_ENABLED)});

        // 屏幕信息
        rows.add(new String[]{"header", "屏幕信息"});
        float widthInches = dm.widthPixels / dm.xdpi;
        float heightInches = dm.heightPixels / dm.ydpi;
        double diagonalInches = Math.hypot(widthInches, heightInches);

        rows.add(new String[]{"data", "分辨率", dm.widthPixels + " x " + dm.heightPixels + " px"});
        rows.add(new String[]{"data", "像素密度",
                dm.densityDpi + " dpi (" + getDensityQualifier(dm.densityDpi) + ")"});
        rows.add(new String[]{"data", "物理尺寸",
                String.format(Locale.US, "%.2f x %.2f 英寸", widthInches, heightInches)});
        rows.add(new String[]{"data", "对角线",
                String.format(Locale.US, "%.2f 英寸", diagonalInches)});
        rows.add(new String[]{"data", "尺寸 (dp)",
                config.screenWidthDp + " x " + config.screenHeightDp + " dp"});
        rows.add(new String[]{"highlight", "最小宽度 (sw)",
                config.smallestScreenWidthDp + " dp"});
        rows.add(new String[]{"data", "字体缩放",
                String.format(Locale.US, "%.2f", dm.scaledDensity)});
        rows.add(new String[]{"data", "刷新率", getRefreshRateInfo(primaryDisplay)});
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            rows.add(new String[]{"data", "HDR",
                    primaryDisplay.getHdrCapabilities() != null ? "支持" : "不支持"});
            rows.add(new String[]{"data", "广色域",
                    primaryDisplay.isWideColorGamut() ? "支持" : "不支持"});
        }
        rows.add(new String[]{"cutout", "刘海屏 (Cutout)"});
        rows.add(new String[]{"data", "屏幕方向",
                config.orientation == Configuration.ORIENTATION_LANDSCAPE ? "横屏" : "竖屏"});
        rows.add(new String[]{"data", "夜间模式",
                (config.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES ? "开启" : "关闭"});
        rows.add(new String[]{"data", "圆屏", config.isScreenRound() ? "是" : "否"});

        // 可用显示区域
        Rect visibleFrame = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleFrame);
        rows.add(new String[]{"data", "可用区域",
                visibleFrame.width() + " x " + visibleFrame.height() + " px"});
        rows.add(new String[]{"highlight", "应用窗口高度 (含导航栏)",
                visibleFrame.width() + " x " + (visibleFrame.height() + getNavigationBarHeight()) + " px"});

        // 副屏信息
        Display[] displays = dmService.getDisplays();
        if (displays.length > 1) {
            rows.add(new String[]{"header", "显示屏信息"});
            for (Display display : displays) {
                if (display.getDisplayId() == Display.DEFAULT_DISPLAY) {
                    continue; // 主屏信息已在"屏幕信息"分组中展示
                }
                DisplayMetrics subMetrics = new DisplayMetrics();
                display.getMetrics(subMetrics);
                Configuration subConfig = createDisplayContext(display)
                        .getResources().getConfiguration();
                rows.add(new String[]{"data", "显示屏 " + display.getDisplayId(),
                        display.getName()});
                rows.add(new String[]{"data", "分辨率",
                        subMetrics.widthPixels + " x " + subMetrics.heightPixels + " px"});
                rows.add(new String[]{"data", "像素密度",
                        subMetrics.densityDpi + " dpi ("
                                + getDensityQualifier(subMetrics.densityDpi) + ")"});
                rows.add(new String[]{"data", "尺寸 (dp)",
                        subConfig.screenWidthDp + " x " + subConfig.screenHeightDp + " dp"});
            }
        }

        return rows;
    }

    /** 刘海屏行：WindowInsets 在首帧布局后才可用，先占位再异步回填 */
    private void addCutoutRow(String label) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            addDataRow(label, "不支持 (API 28+)", false);
            return;
        }
        TextView tvValue = addDataRow(label, "—", false);
        mTable.post(() -> {
            WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            DisplayCutout cutout = insets != null ? insets.getDisplayCutout() : null;
            tvValue.setText(cutout == null ? "无"
                    : "有 (" + cutout.getBoundingRects().size() + " 个开孔)");
        });
    }

    /** 根据 dpi 返回密度等级限定符 */
    private String getDensityQualifier(int dpi) {
        if (dpi <= DisplayMetrics.DENSITY_LOW) {
            return "ldpi";
        }
        if (dpi <= DisplayMetrics.DENSITY_MEDIUM) {
            return "mdpi";
        }
        if (dpi <= DisplayMetrics.DENSITY_HIGH) {
            return "hdpi";
        }
        if (dpi <= DisplayMetrics.DENSITY_XHIGH) {
            return "xhdpi";
        }
        if (dpi <= DisplayMetrics.DENSITY_XXHIGH) {
            return "xxhdpi";
        }
        if (dpi <= DisplayMetrics.DENSITY_XXXHIGH) {
            return "xxxhdpi";
        }
        return "xxxhdpi+";
    }

    /** 获取状态栏高度 */
    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
    }

    /** 获取导航栏高度 */
    private int getNavigationBarHeight() {
        int resourceId = getResources().getIdentifier(
                "navigation_bar_height", "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
    }

    /** 充电状态描述 */
    private String getChargeStatus(Intent battery) {
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            return "已充满";
        }
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            switch (battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                case BatteryManager.BATTERY_PLUGGED_AC:
                    return "充电中 (AC)";
                case BatteryManager.BATTERY_PLUGGED_USB:
                    return "充电中 (USB)";
                case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                    return "充电中 (无线)";
                default:
                    return "充电中";
            }
        }
        if (status == BatteryManager.BATTERY_STATUS_DISCHARGING
                || status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            return "未充电";
        }
        return "未知";
    }

    /** 通过临时 EGL pbuffer 上下文查询 GPU 渲染器名称 */
    private String getGpuRenderer() {
        try {
            EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            if (display == EGL14.EGL_NO_DISPLAY
                    || !EGL14.eglInitialize(display, version, 0, version, 1)) {
                return "未知";
            }
            int[] configAttribs = {
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_NONE};
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0)
                    || numConfigs[0] == 0) {
                EGL14.eglTerminate(display);
                return "未知";
            }
            int[] pbufferAttribs = {EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE};
            EGLSurface surface = EGL14.eglCreatePbufferSurface(display, configs[0], pbufferAttribs, 0);
            int[] contextAttribs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
            EGLContext context = EGL14.eglCreateContext(
                    display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0);
            String renderer = "未知";
            if (surface != null && context != null
                    && EGL14.eglMakeCurrent(display, surface, surface, context)) {
                String queried = GLES20.glGetString(GLES20.GL_RENDERER);
                if (queried != null) {
                    renderer = queried;
                }
            }
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            if (context != null) {
                EGL14.eglDestroyContext(display, context);
            }
            if (surface != null) {
                EGL14.eglDestroySurface(display, surface);
            }
            EGL14.eglTerminate(display);
            return renderer;
        } catch (Exception e) {
            return "未知";
        }
    }

    private String getGlesVersion() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        return am.getDeviceConfigurationInfo().getGlEsVersion();
    }

    private String getMemoryInfo() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return formatBytes(mi.totalMem) + " (可用 " + formatBytes(mi.availMem) + ")";
    }

    private String getStorageInfo() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return formatBytes(stat.getTotalBytes())
                + " (可用 " + formatBytes(stat.getAvailableBytes()) + ")";
    }

    private String getSdCardInfo() {
        StringBuilder sb = new StringBuilder();
        for (File dir : getExternalFilesDirs(null)) {
            try {
                if (dir == null || !Environment.isExternalStorageRemovable(dir)) {
                    continue;
                }
                StatFs stat = new StatFs(dir.getPath());
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(formatBytes(stat.getTotalBytes()))
                        .append(" (可用 ").append(formatBytes(stat.getAvailableBytes())).append(")");
            } catch (Exception ignored) {
            }
        }
        return sb.length() > 0 ? sb.toString() : "无";
    }

    private String getCameraInfo() {
        try {
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            int back = 0;
            int front = 0;
            for (String id : cm.getCameraIdList()) {
                Integer facing = cm.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    front++;
                } else {
                    back++;
                }
            }
            return (back + front) + " 个 (后置 " + back + " / 前置 " + front + ")";
        } catch (Exception e) {
            return "未知";
        }
    }

    private String getRefreshRateInfo(Display display) {
        float[] supported = display.getSupportedRefreshRates();
        StringBuilder sb = new StringBuilder(formatRate(display.getRefreshRate()) + " Hz");
        if (supported.length > 1) {
            sb.append(" (支持: ");
            for (int i = 0; i < supported.length; i++) {
                if (i > 0) {
                    sb.append("/");
                }
                sb.append(formatRate(supported[i]));
            }
            sb.append(")");
        }
        return sb.toString();
    }

    private String formatRate(float rate) {
        String s = String.format(Locale.US, "%.1f", rate);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return s;
    }

    private String formatBytes(long bytes) {
        float gb = bytes / 1024f / 1024f / 1024f;
        if (gb >= 1) {
            return String.format(Locale.US, "%.1f GB", gb);
        }
        return String.format(Locale.US, "%.0f MB", bytes / 1024f / 1024f);
    }

    private String formatUptime(long ms) {
        long minutes = ms / 60000;
        long days = minutes / (60 * 24);
        long hours = minutes / 60 % 24;
        long mins = minutes % 60;
        if (days > 0) {
            return days + " 天 " + hours + " 小时 " + mins + " 分";
        }
        if (hours > 0) {
            return hours + " 小时 " + mins + " 分";
        }
        return mins + " 分";
    }

    private String getGlobalSwitch(String key) {
        try {
            return Settings.Global.getInt(getContentResolver(), key, 0) == 1 ? "开启" : "关闭";
        } catch (Exception e) {
            return "未知";
        }
    }
}
