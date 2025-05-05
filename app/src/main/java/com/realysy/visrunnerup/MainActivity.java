package com.realysy.visrunnerup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    public static Context context;

    public static String path_root;
    public static String path_db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        requestReadWritePermission();
        File root_folder = getContext().getExternalFilesDir(null);
        if (root_folder != null) {
            path_root = root_folder.getAbsolutePath();
        } else {
            path_root = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        Log.i("visdebug", path_root);
        ensureDirs(path_root);

        TextView runinfo = findViewById(R.id.run_info);

        // 读取数据库
        path_db = path_root + "/runnerup.db.export";
        Log.i("visdebug", path_db);

        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(path_db, null, SQLiteDatabase.OPEN_READONLY);
        } catch (Exception e) {
            runinfo.setText("DB file not found: " + path_db + ", \n\nwhich can be exported from Runnerup App, and should been put to that path.");
            return ;
        }

        // 读取表中的字段
        String[] columns = {"_id", "start_time", "distance", "time", "type", "deleted"};
        Cursor cursor = db.query("activity", columns, null, null, null, null, null);
        if (cursor == null) {
            return ;
        }

        // 遍历结果集并打印字段值
        int run_count = 0;
        long run_time = 0;  // sec
        float run_distance = 0;

        int run_count_2023 = 0;
        long run_time_2023 = 0;  // sec
        float run_distance_2023 = 0;

        int run_count_2024 = 0;
        long run_time_2024 = 0;  // sec
        float run_distance_2024 = 0;

        // 0跑步, 1骑行, 2其他, 3越野, 4行走
        final String[] ACTIVITY_TYPE_NAMES = {"跑步", "骑行", "其他", "越野", "行走"};

        //  key: year, value map:
        //      key: activity type, value: YearData
        Map<Integer, Map<Integer, YearData>> allActivityData = new HashMap<>();
        while (cursor.moveToNext()) {
            // 从表中读取数据
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            long startTime = cursor.getLong(cursor.getColumnIndexOrThrow("start_time"));
            float distance = cursor.getFloat(cursor.getColumnIndexOrThrow("distance"));
            long duration = cursor.getLong(cursor.getColumnIndexOrThrow("time"));
            // type: see ACTIVITY_TYPE_NAMES
            int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
            int deleted = cursor.getInt(cursor.getColumnIndexOrThrow("deleted"));

            // 忽略删除的运动 or 其他未知类型的运动
            if (deleted == 1 || type == 2 || (type < 0 || type > 4)) {
                continue;
            }

            // 忽略配速太慢 && 距离太短的
            long[] pace = get_pace(distance, duration);
            if ((type == 0 || type == 1) && pace[0] > 11 && distance < 2000) {  // 跑步 || 骑行
                continue;
            } else if ((type == 3 || type == 4) && pace[0] > 15 && distance < 3000) {  // 行走
                continue;
            }

            Integer activity_year = getYearFromTimestamp(startTime);

            // if this year not contained in data, add it
            if (!allActivityData.containsKey(activity_year)) {
                allActivityData.put(activity_year, new HashMap<>());
                allActivityData.get(activity_year).put(type, new YearData());
            } else if (allActivityData.containsKey(activity_year) &&
                        !allActivityData.get(activity_year).containsKey(type))
            {
                allActivityData.get(activity_year).put(type, new YearData());
            }

            // add record to data of the year
            allActivityData.get(activity_year).get(type)
                .addRecord(startTime, duration, distance, pace);

            //Log.i("visdebug", "id: " + id + ", start_time: " + startTime + ", distance: " + distance +
            //        ", time: " + time + ", type: " + type);
        }

        String run_info = "";
        // loop allActivityData
        for (Map.Entry<Integer, Map<Integer, YearData>> entry : allActivityData.entrySet()) {
            run_info += entry.getKey() + " 年:\n";

            int count = 0;
            float total_distance = 0;  // km
            float total_duration = 0;  // min
            for (Map.Entry<Integer, YearData> entry2 : entry.getValue().entrySet()) {
                String type_name = ACTIVITY_TYPE_NAMES[entry2.getKey()];
                YearData year_data = entry2.getValue();
                String year_summary = year_data.toString();
                run_info += "  - " + type_name + " " + year_summary;
                // TODO 添加配速
                long[] pace = get_pace(year_data.total_distance, year_data.total_duration);
                run_info += String.format(", %d'%02d''\n", pace[0], pace[1]);


                count += year_data.count;
                total_distance += year_data.total_distance / 1000;
                total_duration += year_data.total_duration / 60.f;
            }
            if (count > 0) {
                if (total_duration < 60)
                    run_info += String.format("  - 共运动: %d 次, %.2f km, %.2f min", count, total_distance, total_duration);
                else {
                    total_duration /= 60.0f;   // h
                    run_info += String.format("  - 共运动: %d 次, %.2f km, %.2f h", count, total_distance, total_duration);
                }
            }
            run_info += "\n";
        }

        run_info += String.format("----------------------------------------------------------------\n\n");
        run_info += String.format("Note:\n");
        run_info += String.format(" - 忽略配速慢且距离短的运动\n");
        Log.i("visdebug", run_info);

        cursor.close();  // 关闭游标
        db.close();      // 关闭数据库连接
        runinfo.setText(run_info);   // 显示信息
    }

    /**
     * 计算配速。
     * @param distance 运动距离 米
     * @param time 运动时长 秒
     * @return 返回一个 long[2] 类型的数组，含义为每千米用时 [pace_minute, pace_sec]
     */
    public long[] get_pace(float distance, long time) {
        long[] values = new long[2];

        long pace = (long) (time / (distance/1000));  // sec/km
        values[0] = pace / 60; // 计算商, min
        values[1] = pace % 60; // 计算余数, sec

        return values;
    }

    /**
     * 获取时间戳所属的年份
     * @param timestamp，秒级时间戳
     * @return 年份，如2022
     */
    public int getYearFromTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        // Set the time zone to UTC if your timestamp is in UTC
//        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        // Convert seconds to milliseconds since Calendar expects milliseconds
        calendar.setTimeInMillis(timestamp * 1000);
        return calendar.get(Calendar.YEAR);
    }

    // 请求应用内存读写权限
    public void requestReadWritePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        1
                );
            }
        }
    }

    public static Context getContext() {
        return context;
    }

    public void ensureDirs(String dir){
        File file = new File(dir);
        if (!file.exists())
            file.mkdirs();
    }
}