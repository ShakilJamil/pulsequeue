package com.shakil.pulsequeue.dto;

import java.util.Map;

public class QueueStatsResponse {

    private long totalWaiting;
    private long totalServing;
    private long totalCompletedToday;
    private double averageWaitTimeMinutes;
    private Map<String, Long> waitingByDepartment;

    public QueueStatsResponse() {}

    public QueueStatsResponse(long totalWaiting, long totalServing, long totalCompletedToday, double averageWaitTimeMinutes, Map<String, Long> waitingByDepartment) {
        this.totalWaiting = totalWaiting;
        this.totalServing = totalServing;
        this.totalCompletedToday = totalCompletedToday;
        this.averageWaitTimeMinutes = averageWaitTimeMinutes;
        this.waitingByDepartment = waitingByDepartment;
    }

    public long getTotalWaiting() { return totalWaiting; }
    public void setTotalWaiting(long totalWaiting) { this.totalWaiting = totalWaiting; }

    public long getTotalServing() { return totalServing; }
    public void setTotalServing(long totalServing) { this.totalServing = totalServing; }

    public long getTotalCompletedToday() { return totalCompletedToday; }
    public void setTotalCompletedToday(long totalCompletedToday) { this.totalCompletedToday = totalCompletedToday; }

    public double getAverageWaitTimeMinutes() { return averageWaitTimeMinutes; }
    public void setAverageWaitTimeMinutes(double averageWaitTimeMinutes) { this.averageWaitTimeMinutes = averageWaitTimeMinutes; }

    public Map<String, Long> getWaitingByDepartment() { return waitingByDepartment; }
    public void setWaitingByDepartment(Map<String, Long> waitingByDepartment) { this.waitingByDepartment = waitingByDepartment; }
}
