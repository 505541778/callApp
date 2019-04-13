package com.realview.holo.call.activity;

import android.Manifest;
import android.animation.Animator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.holo.tvwidget.DrawingOrderRelativeLayout;
import com.holo.tvwidget.MetroItemFrameLayout;
import com.holo.tvwidget.MetroViewBorderHandler;
import com.holo.tvwidget.MetroViewBorderImpl;
import com.holoview.usbcameralib.UVCCameraHelper;
import com.holoview.usbcameralib.utils.FileUtils;
import com.hv.calllib.bean.CaptureImageEvent;
import com.hv.calllib.bean.HoloEvent;
import com.hv.calllib.bean.MajorCommand;
import com.realview.holo.call.FastYUVtoRGB;
import com.realview.holo.call.HoloCallApp;
import com.realview.holo.call.ImageUtil;
import com.realview.holo.call.R;
import com.realview.holo.call.basic.BaseActivity;
import com.realview.holo.call.bean.AudioOrderMessage;
import com.realview.holo.call.bean.Constants;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class UVCCameraActivity extends BaseActivity implements CameraDialog.CameraDialogParent, CameraViewInterface.Callback {
    private static final String TAG = "UVCCameraActivity";

    @BindView(R.id.camera_view)
    UVCCameraTextureView mTextureView;
    //    @BindView(R.id.switch_camera_button)
//    MicImageButton switchCameraButton;
    @BindView(R.id.bottom_view)
    LinearLayout bottomView;
    @BindView(R.id.mif_switch_camera)
    MetroItemFrameLayout mifSwitchCamera;
    @BindView(R.id.mif_take_photo)
    MetroItemFrameLayout mifTakePhoto;
    @BindView(R.id.iv_usb_camera_show)
    ImageView ivUsbCameraShow;
    @BindView(R.id.fl_lines)
    ImageView flLines;
    @BindView(R.id.mif_post_photo_sure)
    MetroItemFrameLayout mifPostPhotoSure;
    @BindView(R.id.mif_post_photo_reject)
    MetroItemFrameLayout mifPostPhotoReject;
    @BindView(R.id.send_image_check)
    TextView sendImageCheck;
    @BindView(R.id.dor_extra_content)
    DrawingOrderRelativeLayout dorExtraContent;

    private UVCCameraHelper mCameraHelper;
    private CameraViewInterface mUVCCameraView;
    private boolean isRequest;
    private boolean isPreview;


    public boolean inSendingProgress = false;

    private static final int VoiceItem_Home = 0;
    private static final int VoiceItem_TakePhoto = 1;

    private boolean isFinishing = false;
    private boolean isDestroyed = false;

    //private VoiceCmdEngine voiceCmdEngine = null;
    //private UVCCameraActivity.UIHandler uiHandler;

    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            if (mCameraHelper == null || mCameraHelper.getUsbDeviceCount() == 0) {
                showShortMsg("check no usb camera");
                return;
            }
            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                showShortMsg(device.getDeviceName() + " is out");
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params");
                isPreview = false;
            } else {
                isPreview = true;
                showShortMsg("connecting");
                // initialize seekbar
                // need to wait UVCCamera initialize over
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Looper.prepare();
                        if (mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                            //mSeekBrightness.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_BRIGHTNESS));
                            //mSeekContrast.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_CONTRAST));
                        }
                        Looper.loop();
                    }
                }).start();
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {

            showShortMsg("disconnecting");
        }
    };


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioOrderMessage(AudioOrderMessage message) {
        if (message.getType() == 100) {
            //确认
            if (inSendingProgress == false) {
                TakePhoto();
            } else {
                showShortMsg("文件正在发送中...");
            }

        } else if (message.getType() == 101 || message.getType() == 105) {
            //取消
            isFinishing = true;
            finish();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uvccamera);

        Intent intent = getIntent();
        String launchid = intent.getStringExtra("launchid");
        if (launchid != null) {
            if (launchid.compareTo("holoviewcall") != 0) {
                finish();
            }
        } else {
            finish();
        }
        EventBus.getDefault().register(this);
        ButterKnife.bind(this);
        initView();
        initTVStatusView();
        // step.1 initialize UVCCameraHelper
        mUVCCameraView = (CameraViewInterface) mTextureView;
        mUVCCameraView.setCallback(this);
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_YUYV);
        mCameraHelper.setDefaultPreviewSize(640, 480);
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);


        mCameraHelper.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
            @Override
            public void onPreviewResult(byte[] nv21Yuv) {

            }
        });
        RequestPermission();
    }

    private void initTVStatusView() {
        FrameLayout roundedFrameLayout = new FrameLayout(this);
        final MetroViewBorderImpl metroViewBorderImpl = new MetroViewBorderImpl(roundedFrameLayout);
        metroViewBorderImpl.setBackgroundResource(R.drawable.border_color);

        ViewGroup list = findViewById(R.id.dor_extra_content);
        metroViewBorderImpl.attachTo(list);

        metroViewBorderImpl.getViewBorder().addOnFocusChanged(new MetroViewBorderHandler.FocusListener() {
            @Override
            public void onFocusChanged(View oldFocus, final View newFocus) {
                metroViewBorderImpl.getView().setTag(newFocus);
            }
        });
        metroViewBorderImpl.getViewBorder().addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                View t = metroViewBorderImpl.getView().findViewWithTag("top");
                if (t != null) {
                    ((ViewGroup) t.getParent()).removeView(t);
                    View of = (View) metroViewBorderImpl.getView().getTag(metroViewBorderImpl.getView().getId());
                    ((ViewGroup) of).addView(t);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                View nf = (View) metroViewBorderImpl.getView().getTag();
                if (nf != null) {
                    View top = nf.findViewWithTag("top");
                    if (top != null) {
                        ((ViewGroup) top.getParent()).removeView(top);
                        ((ViewGroup) metroViewBorderImpl.getView()).addView(top);
                        metroViewBorderImpl.getView().setTag(metroViewBorderImpl.getView().getId(), nf);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void RequestPermission() {

        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS_STORAGE = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            int permission = ContextCompat.checkSelfPermission(UVCCameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        UVCCameraActivity.this,
                        PERMISSIONS_STORAGE,
                        0
                );
            }
        }
    }

    private void initView() {
        isFinishing = false;
        isDestroyed = false;
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mCameraHelper.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            showShortMsg("取消操作");
        }
    }


    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        if (!isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.startPreview(mUVCCameraView);
            isPreview = true;
        }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }

    private void destroy() {
        if (isDestroyed) {
            return;
        }

        //  回收
        FileUtils.releaseFile();
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }

        //UnRegisterVoiceCmd();

        isDestroyed = true;
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        /*
        FileUtils.releaseFile();
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }

        //UnRegisterVoiceCmd();
        */

        this.destroy();
    }

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public boolean isCameraOpened() {
        return mCameraHelper.isCameraOpened();
    }

    // example: {640x480,320x240,etc}
    private List<String> getResolutionList() {
        List<Size> list = mCameraHelper.getSupportedPreviewSizes();
        List<String> resolutions = null;
        if (list != null && list.size() != 0) {
            resolutions = new ArrayList<>();
            for (Size size : list) {
                if (size != null) {
                    resolutions.add(size.width + "x" + size.height);
                }
            }
        }
        return resolutions;
    }


    private void TakePhoto() {
        Log.d(TAG, "TakePhoto");
        if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
            showShortMsg("sorry,camera open failed");
            return;
        }

        mCameraHelper.capturePicture(new AbstractUVCCameraHandler.OnCaptureListener() {
            @Override
            public void onCaputreResult(final byte[] data) {
                Log.d(TAG, "TakePhoto" + data.length);
                inSendingProgress = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bottomView.setVisibility(View.VISIBLE);
                        ivUsbCameraShow.setVisibility(View.VISIBLE);
                        mifPostPhotoReject.setVisibility(View.VISIBLE);
                        mifPostPhotoSure.setVisibility(View.VISIBLE);
                        sendImageCheck.setVisibility(View.VISIBLE);


                        FastYUVtoRGB fastYUVtoRGB = new FastYUVtoRGB(UVCCameraActivity.this);
                        bitmap = fastYUVtoRGB.convertYUVtoRGB(data, UVCCameraHelper.getInstance().getPreviewWidth(), UVCCameraHelper.getInstance().getPreviewHeight());
                        ivUsbCameraShow.setImageBitmap(bitmap);
                    }
                });
            }
        });

    }

    Bitmap bitmap;

    private void saveYuv2Jpeg(String path) {
        try {
            File file = new File(path);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            bitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void PostSendPICMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String srcPath = ImageUtil.getCachePath(UVCCameraActivity.this) + "/" + "uvc-" + System.currentTimeMillis() + UVCCameraHelper.SUFFIX_JPEG;
                File pictureFile = new File(srcPath);
                if (!pictureFile.getParentFile().exists()) {
                    pictureFile.getParentFile().mkdirs();
                }
                saveYuv2Jpeg(srcPath);
                CaptureImageEvent captureImageEvent = new CaptureImageEvent();
                captureImageEvent.setImagepath(srcPath);
                captureImageEvent.setFromeid("0");
                captureImageEvent.setTouid("0");
                Gson gson = new Gson();
                String jsonString = gson.toJson(captureImageEvent);
                HoloEvent hermesEvent = new HoloEvent();
                hermesEvent.setAction("api.camera.capture");
                hermesEvent.setBody(jsonString);
                EventBus.getDefault().postSticky(hermesEvent);
                inSendingProgress = false;
            }
        }).start();
    }

    @OnClick(R.id.mif_switch_camera)
    public void onBack() {
        isFinishing = true;
        finish();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMajorCommand(MajorCommand command) {
        if (command.getData().toLowerCase().equals(Constants.CALLMSG_ID_REMOTE_SWITCH_CAMERA.toLowerCase())) {
            if (HoloCallApp.isActivityTop(UVCCameraActivity.class, this)) {
                onBack();
            }
        }
    }

    @OnClick({R.id.mif_take_photo, R.id.mif_post_photo_sure, R.id.mif_post_photo_reject})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.mif_take_photo:
                TakePhoto();
                break;
            case R.id.mif_post_photo_sure:
                PostSendPICMessage();
                bottomView.setVisibility(View.GONE);
                ivUsbCameraShow.setVisibility(View.GONE);
                mifPostPhotoReject.setVisibility(View.GONE);
                mifPostPhotoSure.setVisibility(View.GONE);
                sendImageCheck.setVisibility(View.GONE);
                break;
            case R.id.mif_post_photo_reject:
                bottomView.setVisibility(View.GONE);
                ivUsbCameraShow.setVisibility(View.GONE);
                mifPostPhotoReject.setVisibility(View.GONE);
                mifPostPhotoSure.setVisibility(View.GONE);
                sendImageCheck.setVisibility(View.GONE);
                break;
        }
    }

}