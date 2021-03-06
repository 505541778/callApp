package com.realview.holo.call.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.holoview.aidl.AudioMessage;
import com.holoview.aidl.ProcessServiceIAidl;
import com.hv.calllib.CallManager;
import com.hv.calllib.HoloCallMsgHandler;
import com.hv.imlib.HoloMessage;
import com.hv.imlib.model.Message;
import com.hv.imlib.model.message.ImageMessage;
import com.hv.imlib.model.message.TextMessage;
import com.realview.holo.call.CallApp;
import com.realview.holo.call.bean.AudioOrderMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.voiceengine.WebRtcAudioRecord;

import cn.holo.call.bean.message.CallAcceptMessage;
import cn.holo.call.bean.message.CallHangupMessage;
import cn.holo.call.bean.message.CallInviteMessage;
import cn.holo.call.bean.message.CallModifyMemberMessage;
import cn.holo.call.bean.message.CallRingingMessage;


/**
 * Created by admin on 2019/1/25.
 */

public class CallBackgroundService extends Service {
    private static final String APP_KEY = "qd46yzrfqu7gf";
    private static final String TAG = "BackgroundService";
    private ProcessServiceIAidl mProcessAidl;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        EventBus.getDefault().register(this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallMessage(HoloMessage message) {
        CallApp.getInstance().sendMessage(message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

//    private boolean audioUnsubscribed = false;
    /**
     * 收到的消息
     */
    Binder binder = new ProcessServiceIAidl.Stub() {


        @Override
        public void sendMessage(String json)  {
            Log.i("lipengfei",json);
            HoloMessage holoMessage = JSON.parseObject(json, HoloMessage.class);
            String action = holoMessage.getAction();
//            if (action.equals("api.voice.order")) {
//                AudioOrderMessage message = new AudioOrderMessage();
//                message.setType(Integer.parseInt(holoMessage.getExtraMsg()));
//                EventBus.getDefault().post(message);
//                return;
//            }
//            if (action.equals("api.audio.unsubscribe")){
//                audioUnsubscribed = true;
//            }

            Message message = holoMessage.getMessage();
            try {
                JSONObject object = new JSONObject(json);
                JSONObject content = object.getJSONObject("message").getJSONObject("messageContent");

                if (action.contains("CallAcceptMessage")) {
                    CallAcceptMessage callAcceptMessage = JSON.parseObject(content.toString(), CallAcceptMessage.class);
                    message.setMessageContent(callAcceptMessage);
                } else if (action.contains("CallHangupMessage")) {
                    CallHangupMessage callHangupMessage = JSON.parseObject(content.toString(), CallHangupMessage.class);
                    message.setMessageContent(callHangupMessage);
//                    HoloMessage holoMsg = new HoloMessage();
//                    holoMsg.setAction("api.audio.unsubscribe");
//                    CallApp.getInstance().sendMessage(holoMsg);
                } else if (action.contains("CallInviteMessage")) {
                    CallInviteMessage callInviteMessage = JSON.parseObject(content.toString(), CallInviteMessage.class);
                    message.setMessageContent(callInviteMessage);
                } else if (action.contains("CallModifyMemberMessage")) {
                    CallModifyMemberMessage callModifyMemberMessage = JSON.parseObject(content.toString(), CallModifyMemberMessage.class);
                    message.setMessageContent(callModifyMemberMessage);
                } else if (action.contains("CallRingingMessage")) {
                    CallRingingMessage callRingingMessage = JSON.parseObject(content.toString(), CallRingingMessage.class);
                    message.setMessageContent(callRingingMessage);
                } else if (action.contains("ImageMessage")) {
                    ImageMessage imageMessage = JSON.parseObject(content.toString(), ImageMessage.class);
                    message.setMessageContent(imageMessage);
                    EventBus.getDefault().post(imageMessage);
                    return;
                } else if (action.contains("TextMessage")) {
                    TextMessage textMessage = JSON.parseObject(content.toString(), TextMessage.class);
                    EventBus.getDefault().post(textMessage);
                    return;
                } else if (action.contains("ArMarkMessage")) {
                    CallManager.getInstance().onArMarkMessage(message);
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            HoloCallMsgHandler.getInstance().handleMessage(message);
        }

        /**
         * 使用callLib必须初始化这些消息类型
         * @throws RemoteException
         */
        @Override
        public void initCallMessage(){

        }

        @Override
        public void onBindSuccess(String packageName, String serviceName)  {
        }

        @Override
        public void onAudioData(AudioMessage audio) {
//            if (audio == null || audioUnsubscribed) {
//                return;
//            }
//            WebRtcAudioRecord record = WebRtcAudioRecord.getInstance();
//            if (record == null) {
//                HoloMessage message = new HoloMessage();
//                message.setAction("api.audio.unsubscribe");
//                CallApp.getInstance().sendMessage(message);
//                return;
//            }
//            record.onAudioData(audio.getAudioData());
        }

    };

}
