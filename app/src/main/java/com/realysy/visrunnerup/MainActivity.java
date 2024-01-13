package com.realysy.visrunnerup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.File;

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
        SQLiteDatabase db = SQLiteDatabase.openDatabase(path_db, null, SQLiteDatabase.OPEN_READONLY);

        // 读取表中的字段
        String[] columns = {"_id", "start_time", "distance", "time", "type"};
        Cursor cursor = db.query("activity", columns, null, null, null, null, null);

        String run_info = "";
        // 遍历结果集并打印字段值
        if (cursor != null) {
            int run_count = 0;
            long run_time = 0;  // sec
            float run_distance = 0;

            // 农历年份
            long start_2023 = 1674144000;
            long end_2023 = 1707494400;
            long end_2024 = 1740758400;
            int run_count_2023 = 0;
            long run_time_2023 = 0;  // sec
            float run_distance_2023 = 0;
            int run_count_2024 = 0;
            long run_time_2024 = 0;  // sec
            float run_distance_2024 = 0;

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                long startTime = cursor.getLong(cursor.getColumnIndexOrThrow("start_time"));
                float distance = cursor.getFloat(cursor.getColumnIndexOrThrow("distance"));
                long time = cursor.getLong(cursor.getColumnIndexOrThrow("time"));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));

                do {
                    // 统计跑步
                    if (type == 0) {
                        long[] pace = get_pace(distance, time);

                        // 忽略配速太慢的
                        if (pace[0] > 10) {
                            break;
                        }

                        // 全部
                        run_count++;
                        run_time += time;
                        run_distance += distance;

                        // 2023
                        if (startTime >= start_2023) {
                            if (startTime < end_2023) {
                                run_count_2023++;
                                run_time_2023 += time;
                                run_distance_2023 += distance;
                            } else if (startTime < end_2024) {
                                run_count_2024++;
                                run_time_2024 += time;
                                run_distance_2024 += distance;
                            }
                        }
                    }
                } while (false);

                //Log.i("visdebug", "id: " + id + ", start_time: " + startTime + ", distance: " + distance +
                //        ", time: " + time + ", type: " + type);
            }

            long[] pace = get_pace(run_distance_2023, run_time_2023);
            run_info += String.format("2023跑步距离 %.2f km, 共跑步 %d 次\n平均每次 %.2f km, 配速 %d:%02d /km\n\n",
                    run_distance_2023/1000, run_count_2023, run_distance_2023/1000/run_count_2023, pace[0],pace[1]);

            pace = get_pace(run_distance_2024, run_time_2024);
            run_info += String.format("2024跑步距离 %.2f km, 共跑步 %d 次\n平均每次 %.2f km, 配速 %d:%02d /km\n\n",
                    run_distance_2024/1000, run_count_2024, run_distance_2024/1000/run_count_2024, pace[0],pace[1]);

            run_info += String.format("-------------------------------------------------------\n\n");
            pace = get_pace(run_distance, run_time);
            run_info += String.format("总跑步距离 %.2f km, 共跑步 %d 次\n平均每次 %.2f km, 配速 %d:%02d /km\n\n",
                    run_distance/1000, run_count, run_distance/1000/run_count, pace[0],pace[1]);
            run_info += String.format("-------------------------------------------------------\n\n");
            run_info += String.format("Note:\n");
            run_info += String.format(" - 按农历年份统计\n");
            run_info += String.format(" - 忽略配速大于10min的散步式跑步\n");
            Log.i("visdebug", run_info);

            // 关闭游标
            cursor.close();
        }

        // 关闭数据库连接
        db.close();

        // 显示信息
        runinfo.setText(run_info);
/*
 */
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