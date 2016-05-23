/*
 * Created by GONGYIN HE （何功垠） in 2016
 * Copyright (c) 2016. All right reserved.
 *
 * Last modified 16-5-23 下午9:37
 */

package com.joeyhe.pocketlab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.cengalabs.flatui.FlatUI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends Activity {
    private Socket clientSocket = null;
    private OutputStream outStream = null;
    private ReceiveThread mReceiveThread = null;
    private boolean stop = true;
    private PrintWriter mPrintWriter = null;
    private Context context = this;
    private boolean isConnected = false;
    private SweetAlertDialog s;

    //发送握手信号
    private Handler handler2 = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == 2 ){
                mReceiveThread = new ReceiveThread(clientSocket);
                stop = false;
                //开启线程
                mReceiveThread.start();
                try {
                    mPrintWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mPrintWriter.print("HELLO\r");
                mPrintWriter.flush();
            }
        }
    };

    //提示连接成功，改变按钮
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            //显示接收到的内容
            if(msg.what == 1) {
                new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("恭喜！")
                        .setContentText("已成功连接到PocketLab!")
                        .show();
                (findViewById(R.id.buttonswitch)).setBackgroundResource(R.drawable.switch_on);
                (findViewById(R.id.buttonswitch)).setEnabled(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlatUI.initDefaultValues(this);
        FlatUI.setDefaultTheme(FlatUI.DEEP);
        setContentView(R.layout.activity_main);
        findViewById(R.id.buttonswitch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            clientSocket = new Socket("192.168.1.1", 5001);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Message msg2 = new Message();
                        msg2.what = 2;
                        handler2.sendMessage(msg2);
                    }
                }).start();
            }
        });
    }

    //提示按钮
    public void buttonhelp_Click(View v1) {
        new SweetAlertDialog(this)
                .setTitleText("帮助")
                .setContentText("1.请先将移动端Wifi连接至相应PocketLab硬件端的热点\n\n" +
                        "2.点击右上角开关，提示“连接成功”即可\n\n" +
                        "3.否则重启App与硬件重试\n\n" +
                        "4.进入功能后可在屏幕左边缘侧滑调出菜单\n\n" +
                        "制作人\n东南大学 何功垠")
                .show();
    }


    private class ReceiveThread extends Thread
    {
        private InputStream inStream = null;

        private byte[] buf;
        private String str = null;

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
                        byte[] b = new byte[r];
                        inStream.read(b);
                        this.str = new String(b,"utf-8");
                        Message msg = new Message();
                        msg.obj = this.str;
                        //确定返还消息
                        if((this.str).equals("I am PocketLab V2.0.0!\r\n")){
                            msg.what = 1;
                            isConnected = true;
                            stop = true;
                            mHandler.sendMessage(msg);
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

    public void click(View view) {
//        if (true){           //for test
        if (isConnected) {
            Intent intent = new Intent();
            switch (view.getId()) {
                case R.id.button_Sig:
                    intent.setClass(MainActivity.this, SignalGenerator.class);
                    SignalGenerator.clientSocket = clientSocket;
                    break;
                case R.id.button_Osc:
                    intent.setClass(MainActivity.this, Oscillocope.class);
                    Oscillocope.clientSocket = clientSocket;
                    break;
                case R.id.button_Log:
                    intent.setClass(this, LogicAnalyzer.class);
                    LogicAnalyzer.clientSocket = clientSocket;
                    break;
                case R.id.button_DC:
                    intent.setClass(MainActivity.this, Voltmeter.class);
                    Voltmeter.clientSocket = clientSocket;
                    break;
                case R.id.button_Bode:
                    new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("抱歉")
                            .setContentText("尚未开发完成")
                            .show();
            }
            if (view.getId()!=R.id.button_Bode) {
                startActivity(intent);
//            System.exit(0);
                MainActivity.this.finish();
            }
        }
        else{
            new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("请先连接至硬件端")
                    .setContentText("可查看左上角帮助")
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
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
                        System.exit(0);
                    }
                })
                .show();
    }
}


