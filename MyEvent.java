package com.example.quickstart;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;


/**
 * Created by Pavan on 7/14/2018.
 */

public class MyEvent implements Comparable<MyEvent> {
    public DateTime startDate; // org.joda.time.DateTime
    public DateTime endDate; // org.joda.time.DateTime

    public MyEvent(DateTime startDate, DateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    /**
     *
     * @param  anotherEvent
     * @return 0 for same time, 1 : this>anotherEvent, -1 : this<anotherEvent
     */
    @Override
    public int compareTo(@NonNull MyEvent anotherEvent) {
        // implement 0/1/-1 based on your DateTime object

        return 0;
    }
}
