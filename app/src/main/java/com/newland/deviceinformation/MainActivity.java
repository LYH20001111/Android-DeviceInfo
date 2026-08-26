package com.newland.deviceinformation;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /** 添加数据行：左侧属性名，右侧属性值；highlight 为 true 时红色突出显示 */
    private void addDataRow(String label, String value, boolean highlight) {
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
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    /** 构建设备信息行数据：{类型, 名称, 值}，类型为 "header" 分组标题 / "highlight" 高亮行 / "data" 普通行 */
    private List<String[]> buildDeviceInfo() {
        List<String[]> rows = new ArrayList<>();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Configuration config = getResources().getConfiguration();

        // 设备信息
        rows.add(new String[]{"header", "设备信息"});
        rows.add(new String[]{"highlight", "设备型号", Build.MODEL});
        rows.add(new String[]{"data", "制造商", Build.MANUFACTURER});
        rows.add(new String[]{"data", "Android 版本",
                Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"});
        rows.add(new String[]{"data", "状态栏高度", getStatusBarHeight() + " px"});
        rows.add(new String[]{"data", "导航栏高度", getNavigationBarHeight() + " px"});

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

        // 可用显示区域
        Rect visibleFrame = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleFrame);
        rows.add(new String[]{"data", "可用区域",
                visibleFrame.width() + " x " + visibleFrame.height() + " px"});
        rows.add(new String[]{"highlight", "应用窗口高度 (含导航栏)",
                visibleFrame.width() + " x " + (visibleFrame.height() + getNavigationBarHeight()) + " px"});

        // 副屏信息
        DisplayManager dmService = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
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
}
