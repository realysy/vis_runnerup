package com.realysy.visrunnerup;

// 定义一个类表示每一年的数据
import java.util.ArrayList;

public class YearData {
    // 存储多个记录
    public ArrayList<Long> start_stamps = new ArrayList<>();
    public ArrayList<Long> durations = new ArrayList<>();
    public ArrayList<Float> distances = new ArrayList<>();

    // 总计
    public int count = 0;
    public long total_duration;
    public float total_distance;

    /**
     * 添加新数据
     * @params start_stamp: 运动开始时间戳 (秒级)
     * @param duration: 运动持续时间 (秒)
     * @param distance: 运动距离 (米)
     */
    public void addRecord(long start_stamp, long duration, float distance) {
        start_stamps.add(start_stamp);
        durations.add(duration);
        distances.add(distance);

        count += 1;
        total_duration += duration;
        total_distance += distance;
    }

    @Override
    public String toString() {
        if (count == 0) {
            return "No data";
        }
        return "count=" + count +
            ", total_duration=" + total_duration +
            ", total_distance=" + total_distance;
    }
}