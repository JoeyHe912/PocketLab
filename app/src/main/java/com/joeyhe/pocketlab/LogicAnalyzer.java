/*
 * Created by GONGYIN HE （何功垠） in 2016
 * Copyright (c) 2016. All right reserved.
 *
 * Last modified 16-6-3 下午12:15
 */

package com.joeyhe.pocketlab;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Switch;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class LogicAnalyzer extends FragmentActivity {

    private PrintWriter mPrintWriter = null;
    public static Socket clientSocket;
    private boolean[] is2out = {false,false,false,false,false,false,false,false},
            isClk = {false,false,false,false,false,false,false,false};
    private boolean isFull = false, isRun = false, toChecked = false;
    private SlidingMenu menu = null;
    private MenuDrawer pDrawer;
    private int out[] = {0,0,0,0,0,0,0,0},
            inow = 0;
    private char[] result = new char[8];
    private RadioGroup modeRG[] = new RadioGroup[8];
    private Switch outS[] = new Switch[8];
    private ImageView im[][] = new ImageView[8][10];
    private boolean pre0[] = {false,false,false,false,false,false,false,false};
    private int yNow = 0;

//    画图
    private Handler dHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == 1) {
                draw();
            }
        }
    };

    private Handler sHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
                outS[msg.what].setChecked(toChecked);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pDrawer = MenuDrawer.attach(this, Position.BOTTOM);
        pDrawer.setContentView(R.layout.activity_logic_analyzer);
        pDrawer.setMenuView(R.layout.log_picker);

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
        try {
            mPrintWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for(int i=0; i<8;i++){
            for(int j=0; j<10; j++){
                im[i][j] = (ImageView)findViewById(R.id.imageView_00+i*10+j);
            }
        }
        modeRG[0] = (RadioGroup)findViewById(R.id.radioGroup_mode0);
        modeRG[1] = (RadioGroup)findViewById(R.id.radioGroup_mode1);
        modeRG[2] = (RadioGroup)findViewById(R.id.radioGroup_mode2);
        modeRG[3] = (RadioGroup)findViewById(R.id.radioGroup_mode3);
        modeRG[4] = (RadioGroup)findViewById(R.id.radioGroup_mode4);
        modeRG[5] = (RadioGroup)findViewById(R.id.radioGroup_mode5);
        modeRG[6] = (RadioGroup)findViewById(R.id.radioGroup_mode6);
        modeRG[7] = (RadioGroup)findViewById(R.id.radioGroup_mode7);
        outS[0] = (Switch)findViewById(R.id.switch_out0);
        outS[1] = (Switch)findViewById(R.id.switch_out1);
        outS[2] = (Switch)findViewById(R.id.switch_out2);
        outS[3] = (Switch)findViewById(R.id.switch_out3);
        outS[4] = (Switch)findViewById(R.id.switch_out4);
        outS[5] = (Switch)findViewById(R.id.switch_out5);
        outS[6] = (Switch)findViewById(R.id.switch_out6);
        outS[7] = (Switch)findViewById(R.id.switch_out7);
        listenMode();
        listenOut();
    }

    private void listenMode(){
        for (int i = 0; i<8; i++) {
            final int finalI = i;
            modeRG[i].setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int id) {
                    switch (id) {
                        case R.id.radioButton_in:
                            outS[finalI].setEnabled(false);
                            isClk[finalI] = false;
                            is2out[finalI] = false;
                            new ReceiveThread(clientSocket).start();
                            //开启线程
                            mPrintWriter.print("SET_LOG MOD " + finalI + " 0\r");
                            mPrintWriter.flush();
                            break;
                        case R.id.radioButton_out:
                            outS[finalI].setEnabled(true);
                            isClk[finalI] = false;
                            is2out[finalI] = true;
                            inow = finalI;
                            System.out.println("SET_LOG MOD " + finalI + " 1\r");
                            new ReceiveThread(clientSocket).start();
                            mPrintWriter.print("SET_LOG MOD " + finalI + " 1\r");
                            mPrintWriter.flush();
                            break;
                        case R.id.radioButton_clk:
                            outS[finalI].setEnabled(true);
                            isClk[finalI] = true;
                            is2out[finalI] = true;
                            inow = finalI;
                            new ReceiveThread(clientSocket).start();
                            mPrintWriter.print("SET_LOG MOD " + finalI + " 1\r");
                            mPrintWriter.flush();
                    }
                }
            });
        }
    }

    private void listenOut(){
        for(int i=0; i<8; i++){
            final int finalI = i;
            outS[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    System.out.println("触发listener");
                    if (b){
                        out[finalI] = 1;
                        new ReceiveThread(clientSocket).start();
                        mPrintWriter.print("SET_LOG HIG " + finalI + " 0\r");
                        mPrintWriter.flush();
                    }
                    else{
                        out[finalI] = 0;
                        new ReceiveThread(clientSocket).start();
                        mPrintWriter.print("SET_LOG LOW " + finalI + " 0\r");
                        mPrintWriter.flush();
                    }
                }
            });
        }
    }

    public class ReceiveThread extends Thread
    {
        private InputStream inStream = null;
        private String str = null;
        private boolean stop = false;
        private int iNow = inow;
        ReceiveThread(Socket s)
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
                        System.out.println("r="+r);
                        byte[] b = new byte[r];
                        inStream.read(b);
                        str = new String(b,"utf-8");
                        //发送消息
                        if((str).equals("LOG set!\r\n")){
                            stop = true;
                            System.out.println(inow+"设置成功1");
                            if(is2out[iNow]){
                                System.out.println(inow+"设置成功2");
                                is2out[iNow] = false;
                                new ReceiveThread(clientSocket).start();
                                if(out[iNow] == 1) {
                                    mPrintWriter.print("SET_LOG HIG " + iNow + " 0\r");
                                    mPrintWriter.flush();
                                }
                                else {
                                    mPrintWriter.print("SET_LOG LOW " + iNow + " 0\r");
                                    mPrintWriter.flush();
                                }
                            }
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
//            ready = true;
        }
    }

    public void draw(){
        for(int i=0;i<8;i++){
            if(isFull){
                for(int k=0;k<9;k++){
                    im[i][k].setImageDrawable(im[i][k+1].getDrawable());
                }
            }

            if(result[i]=='0'){
                if (yNow!=0) {
                    if (pre0[i]) {
                        im[i][yNow].setImageResource(R.mipmap.p00);}
                    else{
                        im[i][yNow].setImageResource(R.mipmap.p10);}
                }
                else {
                    im[i][yNow].setImageResource(R.mipmap.p00);}
                pre0[i] = true;
            }
            else {
                if (yNow!=0) {
                    if (pre0[i]) {
                        im[i][yNow].setImageResource(R.mipmap.p01);}
                    else{
                        im[i][yNow].setImageResource(R.mipmap.p11);}
                }
                else {
                    im[i][yNow].setImageResource(R.mipmap.p11);}
                pre0[i] = false;
            }
        }
        yNow++;
        if (yNow==10){
            isFull = true;
            yNow--;
        }
        else {
            isFull = false;
        }
    }

    private void step(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i=0;i<8;i++){
                    if(isClk[i]){
                        if(out[i]==1){
                            toChecked = false;
                            Message msg = new Message();
                            msg.what = i;
                            sHandler.sendMessage(msg);
                        }
                        else {
                            toChecked = true;
                            Message msg = new Message();
                            msg.what = i;
                            sHandler.sendMessage(msg);
                        }
                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                new DReceiveThread(clientSocket).start();
                mPrintWriter.print("GET_LOG\r");
                mPrintWriter.flush();
            }
        }).start();
    }

    public void Click_LogStep(View view){
        step();
    }

    public void Click_LogRun(View view){
        if(isRun){
            isRun = false;
            ((Button)findViewById(R.id.button_logRun)).setText("Run");
        }
        else {
            isRun = true;
            ((Button)findViewById(R.id.button_logRun)).setText("Stop");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isRun){
                        step();
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    public class DReceiveThread extends Thread
    {
        private InputStream inStream = null;
        private String str = null;
        private boolean stop = false;
        private String s;
        int j,k,l,f;
        DReceiveThread(Socket s)
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
                        System.out.println("rr="+r);
                        byte[] b = new byte[r];
                        inStream.read(b);
                        if (r==11){
                            j = (char)b[4]-'0';
                            k = (char)b[5]-'0';
                            l = (char)b[6]-'0';
                            f = j*100+k*10+l;
                            System.out.println(f);
                        }
                        if (r==10){
                            j = (char)b[4]-'0';
                            k = (char)b[5]-'0';
                            f = j*10+k*1;
                            System.out.println(f);
                        }
                        if (r==9){
                            f = (char)b[4]-'0';
                            System.out.println(f);
                        }
                        s = Integer.toBinaryString(f);
                        System.out.println(s);
                        char[] res = {'0','0','0','0','0','0','0','0'};
                        for (int i=0; i<s.length(); i++){
                            res[s.length()-1-i] = s.charAt(i);
                        }
                        result = res;

                        Message msg = new Message();
                        msg.what = 1;
                        dHandler.sendMessage(msg);

//                        str = new String(b,"utf-8");
//                        //发送消息
//                        if((str).equals("LOG 254 .\r\n")){
//                            System.out.println("b");
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
        }
    }

    public void Click_LogReset(View view){
        for(int i=0;i<8;i++){
            for(int j=0;j<10;j++) {
                im[i][j].setImageResource(0);
            }
        }
        yNow = 0;
    }







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
