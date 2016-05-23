/*
 * Created by GONGYIN HE （何功垠） in 2016
 * Copyright (c) 2016. All right reserved.
 *
 * Last modified 16-5-23 下午9:37
 */

package com.joeyhe.pocketlab;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.cengalabs.flatui.FlatUI;
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


public class SignalGenerator extends FragmentActivity
        implements NumberPickerDialogFragment.NumberPickerDialogHandlerV2{

    private TextView mResultTextView,sProgresss;
    private String unit,s,
            wave = "SIN",
            freq = "1000",
            amp = "1000",
            dc = "0",
            duty = "50",
            instruction = null;
    private int max,min,picker,
            mode = 1,
            channel = 0;
    private Switch modeSwitch,channelSwitch;
    private RadioGroup waveRG;
    private SeekBar sb_dc;
    private PrintWriter mPrintWriter = null;
    public static Socket clientSocket;
    private ReceiveThread mReceiveThread = null;
    private boolean stop = true;
    private Context context = this;
    private boolean isSet = false;
    private MenuDrawer mDrawer;

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
        FlatUI.initDefaultValues(this);
        FlatUI.setDefaultTheme(FlatUI.SNOW);
        mDrawer = MenuDrawer.attach(this);
        mDrawer.setContentView(R.layout.activity_signal_generator);
        mDrawer.setMenuView(R.layout.menu);
        ActivityManager.getInstance().addActivity(this);
//        mDrawer.peekDrawer();
        sbdc();
        ListenMode();
        ListenChannel();
        ListenWave();
    }

    private void ListenMode(){
        modeSwitch = (Switch)findViewById(R.id.switch_mode);
        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    (findViewById(R.id.switch_channel)).setEnabled(true);
                    mode = 2;
                }
                else{
                    (findViewById(R.id.switch_channel)).setEnabled(false);
                    mode = 1;
                }
            }
        });
    }

    private void ListenChannel(){
        channelSwitch = (Switch)findViewById(R.id.switch_channel);
        channelSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    channel = 1;
                }
                else{
                    channel = 0;
                }
            }
        });
    }

    private void ListenWave(){
        waveRG = (RadioGroup)findViewById(R.id.radioGroup_wave);
        waveRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.radioButton_sin:
                        wave = "SIN";
                        break;
                    case R.id.radioButton_rec:
                        wave = "REC";
                        break;
                    case R.id.radioButton_tri:
                        wave = "TRI";
                }
            }
        });
    }


    //三个数字输入
    public void Click_k(View view){
        switch (view.getId()){
            case R.id.button_freq:
                max = 20000;
                min = 1;
                unit = "Hz";
                mResultTextView = (TextView) findViewById(R.id.textFreq);
                picker = 1;
                break;
            case R.id.button_amp:
                max = 4000;
                min = 0;
                unit = "mV";
                mResultTextView = (TextView) findViewById(R.id.textAmp);
                picker = 2;
                break;
            case R.id.button_DC:
                max = 4000;
                min = -4000;
                unit = "mV";
                mResultTextView = (TextView) findViewById(R.id.textDC);
                picker = 3;
                break;
        }
        NumberPickerBuilder npb = new NumberPickerBuilder()
                .setFragmentManager(getSupportFragmentManager())
                .setStyleResId(R.style.BetterPickersDialogFragment)
                .setLabelText(unit)
                .setMaxNumber(new BigDecimal(max))
                .setMinNumber(new BigDecimal(min));
        npb.show();
    }

    @Override
    public void onDialogNumberSet(int reference, BigInteger number, double decimal, boolean isNegative, BigDecimal fullNumber) {
        mResultTextView.setText(number.toString()+" "+unit);
        switch (picker){
            case 1:
                freq = number.toString();
                break;
            case 2:
                amp = number.toString();
                break;
            case 3:
                dc = number.toString();
        }
    }

    //占空比进度条
    private void sbdc(){
        sb_dc = (SeekBar) findViewById(R.id.seekBar_dutycycle);
        sProgresss = (TextView) findViewById(R.id.textView_progress);
        sb_dc.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sProgresss.setText(progress + "%");
                duty = Integer.toString(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void Click_set(View view){
        if (freq != null && amp != null && dc != null){
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
            switch (mode){
                case 1:
                    switch (wave){
                        case "REC":
                            instruction = "OPEN_SIG " + wave + " " + freq + " " + amp + " " + dc + " " + duty + "\r";
                            break;
                        case "SIN":
                        case "TRI":
                            instruction = "OPEN_SIG " + wave + " " + freq + " " + amp + " " + dc + "\r";
                    }
                    break;
                case 2:
                    switch (wave){
                        case "REC":
                            instruction = "OPEN_SIG2 " + channel + " " + wave + " " + freq + " " + amp + " " + dc + " " + duty + "\r";
                            break;
                        case "SIN":
                        case "TRI":
                            instruction = "OPEN_SIG2 " + channel + " " + wave + " " + freq + " " + amp + " " + dc + "\r";
                    }
            }
            mPrintWriter.print(instruction);
            mPrintWriter.flush();
            ((ImageView)findViewById(R.id.imageView_sigO)).setImageResource(R.drawable.on);
        }
        else{
            new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Oops...")
                    .setContentText("请先设置好所有参数!")
                    .show();
        }
    }

    public void Click_stop(View view) {
        if (isSet) {
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
            mPrintWriter.print("SET_SIG_OFF\r");
            mPrintWriter.flush();
            ((ImageView)findViewById(R.id.imageView_sigO)).setImageResource(R.drawable.off);
        }
    }

    private class ReceiveThread extends Thread
    {
        private InputStream inStream = null;
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
                        //发送消息
                        if((this.str).equals("Setting SIG OK!\r\n")){
                            msg.what = 1;
                            s = "设置成功！";
                            sHandler.sendMessage(msg);
                            isSet = true;
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

    public void Click_attention(View view){
        new SweetAlertDialog(this)
                .setTitleText("占空比仅在选择方波时有效！")
                .show();
    }

    public void Click_reset(View view){
        ((SeekBar) findViewById(R.id.seekBar_dutycycle)).setProgress(50);
        ((TextView) findViewById(R.id.textView_progress)).setText("50%");
        duty = "50";
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