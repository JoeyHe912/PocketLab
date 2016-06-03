/*
 * Created by GONGYIN HE （何功垠） in 2016
 * Copyright (c) 2016. All right reserved.
 *
 * Last modified 16-6-3 下午12:15
 */

package com.joeyhe.pocketlab;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.codetroopers.betterpickers.numberpicker.NumberPickerBuilder;
import com.codetroopers.betterpickers.numberpicker.NumberPickerDialogFragment;

import net.simonvt.menudrawer.MenuDrawer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Socket;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class Voltmeter extends FragmentActivity
        implements NumberPickerDialogFragment.NumberPickerDialogHandlerV2{

    private TextView VICH1,VICH2,VOCH1,VOCH2,mResultTextView;
    private String VO1 = "0", VO2 = "0", s;
    private ReceiveThread mReceiveThread = null;
    private OReceiveThread oReceiveThread = null;
    private int max,min,picker,m = 0,
            count = 0,
            isFirsttime = 0,
            offset1 = 0, offset2 = 0;
    private float gain1 = 0, gain2 = 0, avg1 = 0, avg2 = 0;
    private boolean isStop = true, toContinue = false, isContinue = false;
    private int[] AD1,AD2;
    private float[] vinput1,vinput2;
    private PrintWriter mPrintWriter = null;
    public static Socket clientSocket;
    private boolean isSet = false, toShow = false, wait;
    private Context context = this;
    private MenuDrawer mDrawer;

    //    显示电压
    private Handler drawHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what){
                case 1:
                    VICH1.setText(avg1+"mV");
                    VICH2.setText(avg2+"mV");
                    break;
            }
        }
    };

    private Handler sHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            //显示接收到的内容
            if(msg.what == 1) {
                new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("恭喜！")
                        .setContentText(s)
                        .show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawer = MenuDrawer.attach(this);
        mDrawer.setContentView(R.layout.activity_voltmeter);
        mDrawer.setMenuView(R.layout.menu);
        ActivityManager.getInstance().addActivity(this);
        setTypeface();
        try {
            mPrintWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setTypeface(){
        Typeface myTypeface = Typeface.createFromAsset(this.getAssets(),"DS-DIGIB.TTF");
        VICH1 = (TextView) findViewById(R.id.textView_VI_CH1);
        VICH1.setTypeface(myTypeface);
        VICH2 = (TextView) findViewById(R.id.textView_VI_CH2);
        VICH2.setTypeface(myTypeface);
        VOCH1 = (TextView) findViewById(R.id.textView_VO_CH1);
        VOCH1.setTypeface(myTypeface);
        VOCH2 = (TextView) findViewById(R.id.textView_VO_CH2);
        VOCH2.setTypeface(myTypeface);
    }

    public void Click_runVI(View view){
        mReceiveThread = new ReceiveThread(clientSocket);
        isStop = false;
        isFirsttime = 1;
        //开启线程
        mReceiveThread.start();
        //                OPEN_OSC2凵<A1>凵<A2>凵<A3>凵<A4>凵<A5>↙
//        instruction = "OPEN_OSC2 20 " + A2 + " " + A3 + " " + A4 + " " + A5 + "\r";
        mPrintWriter.print("OPEN_OSC2 20 0 1 0 1\r");
        mPrintWriter.flush();
        ((ImageView)findViewById(R.id.imageView_vI)).setImageResource(R.drawable.on);
    }

    private class ReceiveThread extends Thread
    {
        private InputStream inStream = null;
        private boolean stop = false;
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
                    if(r >= 600*2+15*isFirsttime){
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
                        }
                        AD1 = getAD(b,600,1);
                        AD2 = getAD(b,600,2);
                        vinput1 = getVinput(gain1,offset1,AD1,0);
                        vinput2 = getVinput(gain2,offset2,AD2,0);
                        getAvg(vinput1,vinput2);
                        Message msg = new Message();
                        msg.what = 1;
                        drawHandler.sendMessage(msg);
                        stop = true;
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

    public void getAvg(float[] input1, float[] input2){
        float sum1 = 0, sum2 = 0;
        for (int i=0; i<input1.length; i++){
            sum1 += input1[i];
            sum2 += input2[i];
        }
        avg1 = (float)(Math.round(sum1/input1.length*10))/10;
        avg2 = (float)(Math.round(sum2/input1.length*10))/10;

    }

    public void continueOsc(){
        mReceiveThread = new ReceiveThread(clientSocket);
        System.out.println("第"+count+"次延时执行");
        //开启线程
        mReceiveThread.start();
    }

    public void Click_stopVI(View view){
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
        ((ImageView)findViewById(R.id.imageView_vI)).setImageResource(R.drawable.off);
    }

    public void Click_VO(View view){
        switch (view.getId()){
            case R.id.button_VO1:
                max = 4000;
                min = -4000;
                mResultTextView = VOCH1;
                picker = 1;
                break;
            case R.id.button_VO2:
                max = 4000;
                min = -4000;
                mResultTextView = VOCH2;
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
        mResultTextView.setText(number.toString()+"mV");
        switch (picker){
            case 1:
                VO1 = number.toString();
                break;
            case 2:
                VO2 = number.toString();
                break;
        }
    }

    public void Click_setVO(View view){
        toShow = false;
        wait = true;
        oReceiveThread = new OReceiveThread(clientSocket);
        //开启线程
        oReceiveThread.start();
        mPrintWriter.print("OPEN_SIG2 0 REC 1000 0 " + VO1 + " 0\r");
        mPrintWriter.flush();
        ((ImageView)findViewById(R.id.imageView_vO)).setImageResource(R.drawable.on);
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("开等");
                while (wait){
                }
                System.out.println("等完");
                toShow = true;
                oReceiveThread = new OReceiveThread(clientSocket);
                //开启线程
                oReceiveThread.start();
                mPrintWriter.print("OPEN_SIG2 1 REC 1000 0 " + VO2 + " 0\r");
                mPrintWriter.flush();
            }
        }).start();
    }

    public void Click_stopVO(View view){
        if(isSet) {
            oReceiveThread = new OReceiveThread(clientSocket);
            //开启线程
            oReceiveThread.start();
            mPrintWriter.print("SET_SIG_OFF\r");
            mPrintWriter.flush();
        }
        ((ImageView)findViewById(R.id.imageView_vO)).setImageResource(R.drawable.off);
    }

    private class OReceiveThread extends Thread
    {
        private InputStream inStream = null;
        private String str = null;
        private boolean stop = false;

        OReceiveThread(Socket s)
        {
            try {
                //获得输入流
                this.inStream = s.getInputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run()
        {
            while(!stop)
            {
                try{
                    int r = inStream.available();
                    if(r!=0){
                        byte[] b = new byte[r];
                        inStream.read(b);
                        this.str = new String(b,"utf-8");
                        Message msg = new Message();
                        msg.obj = this.str;
                        //发送消息
                        if((this.str).equals("Setting SIG OK!\r\n")){
                            if(toShow) {
                                msg.what = 1;
                                s = "设置成功！";
                                sHandler.sendMessage(msg);
                                isSet = true;
                            }
                            else {
                                wait = false;
                                System.out.println("第一次收到");
                            }
                            stop = true;
                        }
                        if((this.str).equals("SIG turned off!\r\n")){
                            msg.what = 1;
                            s = "停止成功！";
                            sHandler.sendMessage(msg);
                            stop = true;
                            isSet = false;
                        }
                    }
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void Click_attentionV(View view){
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("注意！")
                .setContentText("使用直流电压表前请关闭示波器！")
//                .setConfirmText("Yes,delete it!")
                .show();
    }

    //《当用户按下了"手机上的返回功能按键"的时候会回调这个方法》
    @Override
    public void onBackPressed() {
        final int drawerState = mDrawer.getDrawerState();
        if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
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
        if (drawerState == MenuDrawer.STATE_CLOSED){
            mDrawer.openMenu();
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
            case R.id.button2Log:
                intent.setClass(this, LogicAnalyzer.class);
                LogicAnalyzer.clientSocket = clientSocket;
                break;
            case R.id.button2DC:
                intent.setClass(this, Voltmeter.class);
                Voltmeter.clientSocket = clientSocket;
                break;
            case R.id.button2Bode:
        }
        mDrawer.closeMenu();
        if(view.getId()!=R.id.button2Bode) {
            startActivity(intent);
        }
    }
}

