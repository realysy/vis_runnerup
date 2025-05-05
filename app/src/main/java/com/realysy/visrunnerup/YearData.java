package com.realysy.visrunnerup;

// 定义一个类表示每一年的数据
import java.util.ArrayList;

public class YearData {
    // 存储多个记录
    public ArrayList<Long> start_stamps = new ArrayList<>();
    public ArrayList<Long> durations = new ArrayList<>();
    public ArrayList<Float> distances = new ArrayList<>();
    public ArrayList<long[]> paces = new ArrayList<>();

    // 总计
    public int count = 0;
    public long total_duration;   // 运动持续时间 (秒)
    public float total_distance;  // 运动距离 (米)

    /**
     * 添加新数据
     * @params start_stamp: 运动开始时间戳 (秒级)
     * @param duration: 运动持续时间 (秒)
     * @param distance: 运动距离 (米)
     * @param pace: 运动配速 (秒/公里)
     *
     */
    public void addRecord(long start_stamp, long duration, float distance, long[] pace) {
        start_stamps.add(start_stamp);
        durations.add(duration);
        distances.add(distance);
        paces.add(pace);

        count += 1;
        total_duration += duration;
        total_distance += distance;
    }

    @Override
    public String toString() {
        if (count == 0) {
            return "No data";
        }

        if (total_duration / 60.0 / count < 60)
            return String.format("%d 次, 平均 %.2f km, %.2f min",
                count, total_distance / 1000.0 / count, total_duration / 60.0 / count);
        else
            return String.format("%d 次, 平均 %.2f km, %.2f h",
                count, total_distance / 1000.0 / count, total_duration / 3600.0 / count);

    }
}