/*
 * Created by GONGYIN HE （何功垠） in 2016
 * Copyright (c) 2016. All right reserved.
 *
 * Last modified 16-6-3 下午12:15
 */

package com.joeyhe.pocketlab;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.cengalabs.flatui.FlatUI;
import com.codetroopers.betterpickers.numberpicker.NumberPickerBuilder;
import com.codetroopers.betterpickers.numberpicker.NumberPickerDialogFragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;
import cn.qqtheme.framework.picker.OptionPicker;

public class Oscillocope extends FragmentActivity
        implements NumberPickerDialogFragment.NumberPickerDialogHandlerV2, OnChartValueSelectedListener {
    private TextView mResultTextView;
    private PrintWriter mPrintWriter = null;
    public static Socket clientSocket;
    private ReceiveThread mReceiveThread = null;
    private boolean stop = true;
    private boolean isStop = true ,toContinue = false, isContinue = false;
    private SlidingMenu menu = null;
    private int max,min,picker,count = 0,m = 0,
            volts1 = 1000, volts2 = 1000,
            vd1 = 0, vd2 = 0,
            A3 = 2, A5 = 2,
            A2 = 1, A4 = 1,
            time = 8,
            offset1 = 0, offset2 = 0,
            numberX = 600,
            isFirsttime = 0;
    private float gain1 = 0, gain2 = 0;
    private int[] AD1,AD2;
    private float[] vinput1,vinput2,inputDraw1,inputDraw2;
    private LineChart chart;

//    制表线程
    private Handler drawHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what){
                case 1:
                    DrawChart();
                    break;
                case 2:
                    ChangeChartY();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlatUI.initDefaultValues(this);
        FlatUI.setDefaultTheme(FlatUI.SNOW);

        MenuDrawer pDrawer = MenuDrawer.attach(this, Position.BOTTOM);
        pDrawer.setContentView(R.layout.activity_oscilloscope);
        pDrawer.setMenuView(R.layout.osc_picker);

        // configure the SlidingMenu
        menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        // 设置触摸屏幕的模式
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.shadow);

        // 设置滑动菜单视图的宽度
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        // 设置渐入渐出效果的值
        menu.setFadeDegree(0.35f);
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        //为侧滑菜单设置布局
        menu.setMenu(R.layout.menu);

        ActivityManager.getInstance().addActivity(this);
        ListenCoupling1();
        ListenCoupling2();

        try {
            mPrintWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//        findViewById(R.id.button_OscStop).setEnabled(false);
    }

//    监听耦合
    public void ListenCoupling1(){
        Switch switch1;
        switch1 = (Switch)findViewById(R.id.switchCoupling1);
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    A2 = 0;
                }
                else{
                    A2 = 1;
                }
            }
        });
    }
    public void ListenCoupling2(){
        Switch switch1;
        switch1 = (Switch)findViewById(R.id.switchCoupling2);
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    A4 = 0;
                }
                else{
                    A4 = 1;
                }
            }
        });
    }

    //垂直位移
    public void Click_vd(View view){
        switch (view.getId()){
            case R.id.button_vd1:
                max = volts1*2;
                min = -max;
                mResultTextView = (TextView) findViewById(R.id.textVD1);
                picker = 1;
                break;
            case R.id.button_vd2:
                max = volts2*2;
                min = -max;
                mResultTextView = (TextView) findViewById(R.id.textVD2);
                picker = 2;
                break;
        }
        NumberPickerBuilder npb = new NumberPickerBuilder()
                .setFragmentManager(getSupportFragmentManager())
                .setStyleResId(R.style.BetterPickersDialogFragment)
                .setLabelText("mV")
                .setMaxNumber(new BigDecimal(max))
                .setMinNumber(new BigDecimal(min));
        npb.show();
    }
    @Override
    public void onDialogNumberSet(int reference, BigInteger number, double decimal, boolean isNegative, BigDecimal fullNumber) {
        mResultTextView.setText(number.toString()+" mV");
        switch (picker){
            case 1:
                vd1 = Integer.parseInt(number.toString());
                break;
            case 2:
                vd2 = Integer.parseInt(number.toString());
                break;
        }
    }

    //Volts选择
    public void Click_volts(View view) {
        switch (view.getId()) {
            case R.id.button_volts1:
                mResultTextView = (TextView) findViewById(R.id.textVolts1);
                picker = 1;
                break;
            case R.id.button_volts2:
                mResultTextView = (TextView) findViewById(R.id.textVolts2);
                picker = 2;
                break;
        }
        final OptionPicker pickerr = new OptionPicker(this, new String[]{
                "2 V","1 V","0.5 V","0.2 V","0.1 V","50 mV","20 mV","10 mV"
        });

        int index;
        if(picker==1) {
            index = A3-1;
        }
        else{index = A5-1;}
        pickerr.setOffset(1);
        pickerr.setSelectedIndex(index);
        pickerr.setTextSize(20);
        pickerr.setOnOptionPickListener(new OptionPicker.OnOptionPickListener() {
            @Override
            public void onOptionPicked(String option) {
                mResultTextView.setText(option);
                switch (option){
                    case "2 V":
                        if(picker==1) {
                            A3 = 1;
                        }
                        else{A5 = 1;}
                        break;
                    case "1 V":
                        if(picker==1) {
                            A3 = 2;
                        }
                        else{A5 = 2;}
                        break;
                    case "0.5 V":
                        if(picker==1) {
                            A3 = 3;
                        }
                        else{A5 = 3;}
                        break;
                    case "0.2 V":
                        if(picker==1) {
                            A3 = 4;
                        }
                        else{A5 = 4;}
                        break;
                    case "0.1 V":
                        if(picker==1) {
                            A3 = 5;
                        }
                        else{A5 = 5;}
                        break;
                    case "50 mV":
                        if(picker==1) {
                            A3 = 6;
                        }
                        else{A5 = 6;}
                        break;
                    case "20 mV":
                        if(picker==1) {
                            A3 = 7;
                        }
                        else{A5 = 7;}
                        break;
                    case "10 mV":
                        if(picker==1) {
                            A3 = 8;
                        }
                        else{A5 = 8;}
                        break;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isContinue) {
                        }
                        while (!isContinue){
                        }
                        toContinue = false;
                        while (!isStop) {
                        }
                        runOsc();
                        Message msg = new Message();
                        msg.what = 2;
                        drawHandler.sendMessage(msg);
                    }
                }).start();
            }
        });
        pickerr.show();
    }

//    Time选择
    public void Click_TIME(View view){
        OptionPicker picker = new OptionPicker(this, new String[]{
                "500 ms","200 ms","100 ms","50 ms","20 ms","10 ms","5 ms","2 ms","1 ms","500 us","200 us","100 us"
        });
        picker.setOffset(2);
        picker.setSelectedIndex(time);
        picker.setTextSize(20);
        picker.setOnOptionPickListener(new OptionPicker.OnOptionPickListener() {
            @Override
            public void onOptionPicked(String option) {
                ((TextView)findViewById(R.id.textTime)).setText(option);
                switch (option){
                    case "500 ms":
                        time = 0;
                        numberX = 500*50*6*2;
                        break;
                    case "200 ms":
                        time = 1;
                        numberX = 200*50*6*2;
                        break;
                    case "100 ms":
                        time = 2;
                        numberX = 100*50*6*2;
                        break;
                    case "50 ms":
                        time = 3;
                        numberX = 50*50*6*2;
                        break;
                    case "20 ms":
                        time = 4;
                        numberX = 20*50*6*2;
                        break;
                    case "10 ms":
                        time = 5;
                        numberX = 10*50*6*2;
                        break;
                    case "5 ms":
                        time = 6;
                        numberX = 5*50*6*2;
                        break;
                    case "2 ms":
                        time = 7;
                        numberX = 2*50*6*2;
                        break;
                    case "1 ms":
                        time = 8;
                        numberX = 1*50*6*2;
                        break;
                    case "500 us":
                        time = 9;
                        numberX = 50*6*2/2;
                        break;
                    case "200 us":
                        time = 10;
                        numberX = 50*6*2/5;
                        break;
                    case "100 us":
                        time = 11;
                        numberX = 50*6*2/10;
                        break;
                }
//                if(toContinue) {
//                    new Thread(new Runnable() {
//                        public void run() {
//                            while (toContinue) {
//                            }
//                            while (!isContinue){
//                            }
//                            toContinue = false;
//                            while (!isStop) {
//                            }
//                            toContinue = true;
//                            continueOsc();
//                        }
//                    }).start();
//                }
            }
        });
        picker.show();
    }

    public void Click_runOsc(View view){
        ((ImageView)findViewById(R.id.imageView_oscO)).setImageResource(R.drawable.on);
        ((ImageView)findViewById(R.id.imageView_oscO2)).setImageResource(R.drawable.on);
        runOsc();
    }
    public void runOsc(){
//        findViewById(R.id.button_OscRun).setEnabled(false);
//        findViewById(R.id.button_OscStop).setEnabled(true);
        mReceiveThread = new ReceiveThread(clientSocket);
        stop = false;
        isStop = false;
        isFirsttime = 1;
        //开启线程
        mReceiveThread.start();
        //                OPEN_OSC2凵<A1>凵<A2>凵<A3>凵<A4>凵<A5>↙
        String instruction = "OPEN_OSC2 20 " + A2 + " " + A3 + " " + A4 + " " + A5 + "\r";
        mPrintWriter.print(instruction);
        mPrintWriter.flush();
    }

    private class ReceiveThread extends Thread
    {
        private InputStream inStream = null;
        ReceiveThread(Socket s)
        {
            try {
                //获得输入流
                inStream = s.getInputStream();
//                inn = new DataInputStream(s.getInputStream());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run()
        {
            isContinue = false;
            System.out.println("开始第"+(++count)+"接收线程");
            if(toContinue){
                new Thread(new Runnable() {
                    public void run() {
                        while (!isContinue) {
                            System.out.println("第"+(m++)+"次重发");
                            mPrintWriter.print("CON_OSC2\r");
                            mPrintWriter.flush();
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
            while(!stop)
            {
                try{
//                                         0   1    3   5    7   11  15
//                    1ms   r > 50*6*2*2 + 1 + 2 +  2 + 2  + 4 + 4 = 1215
//                                          A3  14 00 offset   gain
                    int r = inStream.available();
                    if(r >= numberX*2+15*isFirsttime){
                        isContinue = true;
                        mPrintWriter.print("SET_OSC_OFF\r");
                        mPrintWriter.flush();
                        System.out.println("r= "+r);
                        byte[] b = new byte[r];
                        inStream.read(b);
                        try {
                            r = inStream.available();
                            if(r!=0) {
                                byte[] c = new byte[r];
                                inStream.read(c);
                                mPrintWriter.print("SET_OSC_OFF\r");
                                mPrintWriter.flush();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("清空r= "+r);

                        if(isFirsttime==1) {
                            toContinue = true;
                            byte[] offsetbyte = new byte[2];
                            offsetbyte[1] = b[3 + 1];
                            offsetbyte[0] = b[3];
                            offset1 = byteArrayToInt(offsetbyte);
                            offsetbyte[1] = b[5 + 1];
                            offsetbyte[0] = b[5];
                            offset2 = byteArrayToInt(offsetbyte);
                            byte[] gain = new byte[4];
                            gain[3] = b[7 + 3];
                            gain[2] = b[7 + 2];
                            gain[1] = b[7 + 1];
                            gain[0] = b[7 + 0];
                            gain1 = Float.intBitsToFloat(byte4ArrayToInt(gain));
                            gain[3] = b[11 + 3];
                            gain[2] = b[11 + 2];
                            gain[1] = b[11 + 1];
                            gain[0] = b[11 + 0];
                            gain2 = Float.intBitsToFloat(byte4ArrayToInt(gain));
                            System.out.println(offset1);
                            System.out.println(offset2);
                            System.out.println(gain1);
                            System.out.println(gain2);
                            String s = new String(b,"utf-8");
                            System.out.println(s);
                        }
                        AD1 = getAD(b,numberX,1);
                        AD2 = getAD(b,numberX,2);
                        vinput1 = getVinput(gain1,offset1,AD1,vd1);
                        vinput2 = getVinput(gain2,offset2,AD2,vd2);
                        FindZero(vinput1,vinput2,numberX/2);
                        Message msg = new Message();
                        msg.what = 1;
                        drawHandler.sendMessage(msg);
                        stop = true;
                    }
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("结束第"+count+"接收线程");
        }
    }

    public int byteArrayToInt(byte[] b) {
        return   b[0] & 0xFF |
                (b[1] & 0xFF) << 8;
    }

    public int byte4ArrayToInt(byte[] b) {
        return   b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 |
                (b[3] & 0xFF) << 24;
    }

    public int[] getAD(byte[] b,int n,int ch){
        int[] AD = new int[n];
        for (int i = 0; i<n ; i++){
            AD[i] = b[15*isFirsttime+i*2+ch-1] & 0xFF;
        }
        return AD;
    }

    public float[] getVinput(float gain, int offset, int[] AD, int vd) {
        float[] vinput = new float[AD.length];
        for(int i=0; i<AD.length; i++){
            vinput[i] = -AD[i]*gain + offset + vd;
        }
        return vinput;
    }

    public void FindZero(float[] input1, float[] input2, int n){
        boolean isFindbelow = false;
        inputDraw1 = new float[n];
        inputDraw2 = new float[n];
        int i;
        for (i=0; i<input1.length; i++ ){
            if (input1[i]<0){
                isFindbelow = true;
            }
            else {
                if(isFindbelow){
                    break;
                }
            }
        }
        for (int k=0; k<n ; k++){
            if (i+k < input1.length){
                inputDraw1[k] = input1[i+k];
                inputDraw2[k] = input2[i+k];
            }
            else {
                inputDraw1[k] = 0;
                inputDraw2[k] = 0;
            }
        }
    }

    public void continueOsc(){
        mReceiveThread = new ReceiveThread(clientSocket);
        stop = false;

        System.out.println("第"+count+"次延时执行");
        //开启线程
        mReceiveThread.start();
    }

    private void ChangeChartY(){
        chart = (LineChart) findViewById(R.id.chart);

        YAxis leftAxis = chart.getAxisLeft();
        YAxis rightAxis = chart.getAxisRight();
        leftAxis.setAxisMinValue(-getY(A3));
        leftAxis.setAxisMaxValue(getY(A3));
        rightAxis.setAxisMinValue(-getY(A5));
        rightAxis.setAxisMaxValue(getY(A5));

        chart.invalidate(); // refresh

    }
    private int getY(int A){
        int y = 3000;
        switch (A){
            case 1:
                y = 6000;
                break;
            case 2:
                y = 3000;
                break;
            case 3:
                y = 1500;
                break;
            case 4:
                y = 600;
                break;
            case 5:
                y = 300;
                break;
            case 6:
                y = 150;
                break;
            case 7:
                y = 60;
                break;
            case 8:
                y = 30;
        }
        return y;
    }

    private void DrawChart(){
        chart = (LineChart) findViewById(R.id.chart);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setDescription("示波器波形");
        chart.setDrawGridBackground(true);
        chart.setDrawBorders(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setLabelsToSkip(numberX/6/2);//过50单位画线
        xAxis.setAvoidFirstLastClipping(true);//边界开始
        xAxis.setDrawAxisLine(true);//轴线
        xAxis.setDrawGridLines(true);//网格
        xAxis.setDrawLabels(false);//标签

        YAxis leftAxis = chart.getAxisLeft();
        YAxis rightAxis = chart.getAxisRight();
//        chart.getAxisRight().setEnabled(false);

        leftAxis.setAxisMinValue(-getY(A3));
        leftAxis.setAxisMaxValue(getY(A3));
        leftAxis.setLabelCount(7, true);
        leftAxis.setDrawLabels(true);
        leftAxis.setDrawAxisLine(true);
        leftAxis.setTextColor(Color.RED);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);

        rightAxis.setAxisMinValue(-getY(A5));
        rightAxis.setAxisMaxValue(getY(A5));
        rightAxis.setLabelCount(7, false);
        rightAxis.setDrawLabels(true);
        rightAxis.setDrawAxisLine(true);
        rightAxis.setTextColor(Color.GREEN);
        rightAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);

        ArrayList<Entry> valsCh1 = new ArrayList<Entry>();
        ArrayList<Entry> valsCh2 = new ArrayList<Entry>();

        for(int i=0;i<inputDraw1.length;i++){
            valsCh1.add(new Entry(inputDraw1[i],i));
        }
        for(int i=0;i<inputDraw2.length;i++){
            valsCh2.add(new Entry(inputDraw2[i],i));
        }

        LineDataSet setCh1 = new LineDataSet(valsCh1, "CH 1");
        setCh1.setAxisDependency(YAxis.AxisDependency.LEFT);
        setCh1.setDrawCircles(false);
        setCh1.setHighLightColor(Color.RED); // 设置点击某个点时，横竖两条线的颜色
        setCh1.setDrawValues(false); // 是否在点上绘制Value
        setCh1.setHighlightEnabled(true);
        setCh1.setColor(Color.RED);
        LineDataSet setCh2 = new LineDataSet(valsCh2, "Ch 2");
        setCh2.setAxisDependency(YAxis.AxisDependency.RIGHT);
        setCh2.setDrawCircles(false);
        setCh2.setHighLightColor(Color.GREEN); // 设置点击某个点时，横竖两条线的颜色
        setCh2.setDrawValues(false); // 是否在点上绘制Value
        setCh2.setHighlightEnabled(true);
        setCh2.setColor(Color.GREEN);


        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(setCh1);
        dataSets.add(setCh2);

        ArrayList<String> xVals = new ArrayList<>();
        for (int j=0;j<numberX/2;j++){
            xVals.add(j+"");
        }


        LineData data = new LineData(xVals,dataSets);
        chart.setData(data);
        chart.notifyDataSetChanged();
        chart.invalidate(); // refresh

//        TimerTask task = new TimerTask(){
//
//            public void run() {
//                if (toContinue) {
//                    System.out.println("第"+count+"次延时执行");
//                    isFirsttime = 0;
//                    isStop = false;
//                    continueOsc();
//                }
//            }
//        };
//
//        System.out.println("第"+count+"次延时开始");
//        Timer timer = new Timer();
//        timer.schedule(task, 5000);
        if(toContinue) {
            new Thread(new Runnable() {
                public void run() {
                    System.out.println("第" + count + "次延时开始");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isFirsttime = 0;
                    isStop = false;
                    continueOsc();  //执行的方法
                }
            }).start();
        }
        else {
            isStop = true;
        }

        chart.setOnChartValueSelectedListener(this);
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        if(dataSetIndex==0){
            ((TextView)findViewById(R.id.textView_value)).setText((float)(Math.round(inputDraw1[h.getXIndex()]*100))/100+"mV");
        }

        else {
            ((TextView)findViewById(R.id.textView_value)).setText((float)(Math.round(inputDraw1[h.getXIndex()]*100))/100+"mV");
        }

    }

    @Override
    public void onNothingSelected() {
        ((TextView)findViewById(R.id.textView_value)).setText("");
    }

    public void Click_stopOsc(View view){
//        findViewById(R.id.button_OscStop).setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isContinue) {
                }
                while (!isContinue){
                }
                toContinue = false;
            }
        }).start();
        findViewById(R.id.button_OscRun).setEnabled(true);
        ((ImageView)findViewById(R.id.imageView_oscO)).setImageResource(R.drawable.off);
        ((ImageView)findViewById(R.id.imageView_oscO2)).setImageResource(R.drawable.off);
    }

//    private class ReceiveStopThread extends Thread
//    {
//        private InputStream inStream = null;
//        private String str = null;
//
//        ReceiveStopThread(Socket s)
//        {
//            try {
//                //获得输入流
//                inStream = s.getInputStream();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        public void run()
//        {
//            while(!stop)
//            {
//                try{
//                    int r = inStream.available();
//                    if(r!=0){
//                        byte[] b = new byte[r];
//                        inStream.read(b);
//                        this.str = new String(b,"utf-8");
//                        Message msg = new Message();
//                        msg.obj = this.str;
//                        if((this.str).equals("SIG turned off!\r\n")){
//
//                        }
//                    }
//                }
//                catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    //《当用户按下了"手机上的返回功能按键"的时候会回调这个方法》
    @Override
    public void onBackPressed() {
        if (menu.isMenuShowing()) {
            new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("确定退出Pocketlab吗？")
                    .setContentText("退出后再次打开需要重新连接至硬件端!")
                    .setConfirmText("是")
                    .setCancelText("否")
                    .showCancelButton(true)
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            sDialog.dismissWithAnimation();
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ActivityManager.getInstance().finshAllActivities();
                        }
                    })
                    .show();
        }
        else{
            menu.showMenu();
        }
    }

    public void Click_menu(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.button2Sin:
                intent.setClass(this, SignalGenerator.class);
//                    if(SignalGenerator.clientSocket == null)
                SignalGenerator.clientSocket = clientSocket;
                break;
            case R.id.button2Osc:
                intent.setClass(this, Oscillocope.class);
                Oscillocope.clientSocket = clientSocket;
                break;
            case R.id.button2DC:
                intent.setClass(this, Voltmeter.class);
                Voltmeter.clientSocket = clientSocket;
                break;
            case R.id.button2Log:
                intent.setClass(this, LogicAnalyzer.class);
                LogicAnalyzer.clientSocket = clientSocket;
                break;
            case R.id.button2Bode:
        }
//        mDrawer.closeMenu();
        menu.showContent();
        if(view.getId()!=R.id.button2Bode) {
            startActivity(intent);
        }
    }
}
