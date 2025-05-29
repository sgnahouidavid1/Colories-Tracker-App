package com.example.coloriestrackerremastered;

import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class DateAdapter {
    private TextView textView;
    private int style;
    private Date date;

    DateAdapter(TextView textView, int style) {
        this.textView = textView;
        this.style = style;
    }
    public void updateDateField(long timeInMillis) {
        DateFormat df = DateFormat.getDateInstance(style);
        this.date = new Date(timeInMillis);
        textView.setText(df.format(date));
    }

    public Date getDate() {
        return date;
    }

}
