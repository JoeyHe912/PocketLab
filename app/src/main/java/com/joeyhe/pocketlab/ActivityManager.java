/*
 * Created by GONGYIN HE （何功垠） in 2016
 * Copyright (c) 2016. All right reserved.
 *
 * Last modified 16-6-3 下午12:15
 */

package com.joeyhe.pocketlab;

/**
 * Created by HGY on 2016/4/26.
 */

import android.app.Activity;
import java.util.Arrays;
import java.util.LinkedList;

public class ActivityManager {

    private LinkedList<Activity> activityLinkedList = new LinkedList<Activity>();

    private ActivityManager() {
    }

    private static ActivityManager instance;

    public static ActivityManager getInstance(){
        if(null == instance){
            instance = new ActivityManager();
        }
        return instance;
    }

    //向list中添加Activity
    public ActivityManager addActivity(Activity activity){
        activityLinkedList.add(activity);
        return instance;
    }

    //结束特定的Activity(s)
    public ActivityManager finshActivities(Class<? extends Activity>... activityClasses){
        for (Activity activity : activityLinkedList) {
            if( Arrays.asList(activityClasses).contains( activity.getClass() ) ){
                activity.finish();
            }
        }
        return instance;
    }

    //结束所有的Activities
    public ActivityManager finshAllActivities() {
        for (Activity activity : activityLinkedList) {
            activity.finish();
        }
        return instance;
    }
}

