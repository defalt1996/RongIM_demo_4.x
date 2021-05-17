package io.rong.imkit.utils;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.rong.imkit.R;
import io.rong.imkit.RongIM;

/**
 * Created by DragonJ on 15/3/25.
 */
public class TimeUtils {


    public static String formatData(long timeMillis) {
        Context context = RongIM.getInstance().getApplicationContext();
        if (context == null || timeMillis == 0)
            return "";

        String result;

        int targetDay = (int) (timeMillis / (24 * 60 * 60 * 1000));
        int nowDay = (int) (System.currentTimeMillis() / (24 * 60 * 60 * 1000));

        if (targetDay == nowDay) {
            result = formatDate(timeMillis, "HH:mm");
        } else if (targetDay + 1 == nowDay) {
            result = context.getResources().getString(R.string.rc_yesterday_format);
//            result = String.format(formatString, fromatDate(timeMillis, "HH:mm"));
        } else {
            result = formatDate(timeMillis, "yyyy-MM-dd");
        }


        return result;

    }


    public static String formatTime(long timeMillis) {
        Context context = RongIM.getInstance().getApplicationContext();
        if (context == null || timeMillis == 0)
            return "";
        String result;

        int targetDay = (int) (timeMillis / (24 * 60 * 60 * 1000));
        int nowDay = (int) (System.currentTimeMillis() / (24 * 60 * 60 * 1000));

        if (targetDay == nowDay) {
            result = formatDate(timeMillis, "HH:mm");
        } else if (targetDay + 1 == nowDay) {
            result = context.getResources().getString(R.string.rc_yesterday_format);
        } else {
            result = formatDate(timeMillis, "yyyy-MM-dd HH:mm");
        }
        return result;
    }


    private static String formatDate(long timeMillis, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(timeMillis));
    }

}
