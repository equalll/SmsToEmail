package com.jason.app.smstoemail.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.jason.app.smstoemail.MainSmsActivity;
import com.jason.app.smstoemail.EmailMager;
import com.jason.app.smstoemail.R;
import com.jason.app.smstoemail.fragments.SettingsFragment;
import com.jason.app.smstoemail.utils.AndrUtils;
import com.jason.app.smstoemail.views.SmsMsg;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive");
        SettingsFragment.load(context, EmailMager.getInstance());
        SmsLocalManager.getInstace().init(context);
        SmsLocalManager.getInstace().load();
        smsReceived(context, intent);
    }

    private void smsReceived(Context context, Intent intent) {
        //有短信到达
        SmsManager sms = SmsManager.getDefault();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            SmsMessage[] messages = new SmsMessage[pdus.length];
            //解析
            for (int i = 0; i < pdus.length; i++)
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
            //按照时间排序
            Arrays.sort(messages, new Comparator<SmsMessage>() {

                @Override
                public int compare(SmsMessage o1, SmsMessage o2) {
                    if (o1.getTimestampMillis() == o2.getTimestampMillis()) {
                        return 0;
                    } else if (o1.getTimestampMillis() > o2.getTimestampMillis()) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });
            //分类合并,排版
            Map<String, StringBuilder> msgMap = new HashMap<>();
            for (SmsMessage msg : messages) {
                String content = msg.getMessageBody();
                String from = msg.getOriginatingAddress();
                long time = msg.getTimestampMillis();
                sendToView(context,msg);
                if (!msgMap.containsKey(from)) {
                    StringBuilder sbf = new StringBuilder();
                    sbf.append(context.getString(R.string.from)).append(from).append("\n");
                    sbf.append(context.getString(R.string.content)).append("\n");
                    sbf.append("---------").append(sdf.format(new Date(time))).append("---------\n");
                    sbf.append(content).append("\n");
                    msgMap.put(from, sbf);
                } else {
                    msgMap.get(from).append("---------").append(sdf.format(new Date(time))).append("---------\n");
                    msgMap.get(from).append(content).append("\n");
                }
            }
            //组装字符串发送
            StringBuilder contbuf = new StringBuilder();
            for (StringBuilder v : msgMap.values()) {
                contbuf.append("----------------------------------------");
                contbuf.append("\n").append(v).append("\n");
                contbuf.append("----------------------------------------");
            }
            Log.e(TAG, "v=\n" + contbuf.toString());
            boolean isSuc = EmailMager.getInstance().sendMail(context.getString(R.string.smsTitle), contbuf.toString());
            if (isSuc) {
                Log.i(TAG, "sended");
            } else {
                sendTips(context.getString(R.string.sendedFailed));
                Log.i(TAG, context.getString(R.string.sendedFailed));
            }
        }
    }

    //通知界面显示
    private void sendToView(Context con,SmsMessage s) {
        SmsMsg smsMsg = new SmsMsg(s);
        SmsLocalManager.getInstace().add(smsMsg);
        if (MainSmsActivity.Inst() != null&&!AndrUtils.isBackground(con)) {
            MainSmsActivity.Inst().addSms(smsMsg);
        }
    }

    private void sendTips(String s) {
        if (MainSmsActivity.Inst() != null) {
            MainSmsActivity.Inst().sendTips(s);
        }
    }
}
