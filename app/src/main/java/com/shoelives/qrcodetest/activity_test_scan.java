package com.shoelives.qrcodetest;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.nio.ByteBuffer;

import cn.bingoogolapple.photopicker.activity.BGAPhotoPickerActivity;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zbar.ZBarView;

public class activity_test_scan extends AppCompatActivity implements QRCodeView.Delegate {

    private static final String TAG = activity_test_scan.class.getSimpleName();
    private static final int REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY = 666;

    private static QRCodeView mQRCodeView;
    private AsyncTask<Void, Void, String> MyAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_scan);

        mQRCodeView = (ZBarView) findViewById(R.id.zbarview);
        mQRCodeView.setDelegate(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mQRCodeView.startCamera();
//        mQRCodeView.startCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);

        mQRCodeView.showScanRect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mQRCodeView.startSpot(); //一進來就可以掃描
    }

    @Override
    protected void onStop() {
        mQRCodeView.stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mQRCodeView.onDestroy();
        if (MyAsyncTask!=null){
            MyAsyncTask.cancel(true);
            MyAsyncTask=null;
        }
        super.onDestroy();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_spot:
                mQRCodeView.startSpot(); //掃描
                break;
            case R.id.stop_spot:
                mQRCodeView.stopSpot();//停止
                break;
            case R.id.start_spot_showrect:
                mQRCodeView.startSpotAndShowRect();//顯示掃描框
                break;
            case R.id.stop_spot_hiddenrect:
                mQRCodeView.stopSpotAndHiddenRect();//隱藏掃描框
                break;
            case R.id.show_rect:
                mQRCodeView.showScanRect();//顯示框並掃描
                break;
            case R.id.hidden_rect:
                mQRCodeView.hiddenScanRect();//隱藏框停止掃描
                break;
            case R.id.start_preview:
                mQRCodeView.startCamera();//開始預覽相機功能
                break;
            case R.id.stop_preview:
                mQRCodeView.stopCamera(); //停止預覽相機功能
                break;
            case R.id.open_flashlight:
                mQRCodeView.openFlashlight();//打開閃光燈
                break;
            case R.id.close_flashlight:
                mQRCodeView.closeFlashlight();//關閉閃光燈
                break;
            case R.id.scan_barcode:
                mQRCodeView.changeToScanBarcodeStyle();//掃條碼
                break;
            case R.id.scan_qrcode:
                mQRCodeView.changeToScanQRCodeStyle();//掃 RQ code
                break;
            case R.id.choose_qrcde_from_gallery: //從相簿選圖
                /*
                从相册选取二维码图片，这里为了方便演示，使用的是
                https://github.com/bingoogolapple/BGAPhotoPicker-Android
                这个库来从图库中选择二维码图片，这个库不是必须的，你也可以通过自己的方式从图库中选择图片
                 */

                // 识别图片中的二维码还有问题，占时不要用
//                Intent photoPickerIntent = new BGAPhotoPickerActivity.IntentBuilder(this)
//                        .cameraFileDir(null)
//                        .maxChooseCount(1)
//                        .selectedPhotos(null)
//                        .pauseOnScroll(false)
//                        .build();
//                startActivityForResult(photoPickerIntent, REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY);
                break;
        }
    }


    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);//震動
        vibrator.vibrate(200);
    }

    @Override
    public void onScanQRCodeSuccess(String result) { //掃到的字串回傳
        Log.i(TAG, "result:" + result);
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        vibrate();
        mQRCodeView.startSpot(); //繼續下一次掃描
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Log.e(TAG, "打开相机出错");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 识别图片中的二维码还有问题，占时不要用
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY) {
            final String picturePath = BGAPhotoPickerActivity.getSelectedPhotos(data).get(0);

            /*
            这里为了偷懒，就没有处理匿名 AsyncTask 内部类导致 Activity 泄漏的问题
            请开发在使用时自行处理匿名内部类导致Activity内存泄漏的问题，处理方式可参考 https://github.com/GeniusVJR/LearningNotes/blob/master/Part1/Android/Android%E5%86%85%E5%AD%98%E6%B3%84%E6%BC%8F%E6%80%BB%E7%BB%93.md
             */
            if (MyAsyncTask == null) {
                MyAsyncTask = new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        Bitmap bitmap = getDecodeAbleBitmap(picturePath);
                        int picw = bitmap.getWidth();
                        int pich = bitmap.getHeight();
                        int[] pix = new int[picw * pich];
                        byte[] pixytes = new byte[picw * pich];
                        bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);
                        int R, G, B, Y;

                        for (int y = 0; y < pich; y++) {
                            for (int x = 0; x < picw; x++) {
                                int index = y * picw + x;
                                R = (pix[index] >> 16) & 0xff;     //bitwise shifting
                                G = (pix[index] >> 8) & 0xff;
                                B = pix[index] & 0xff;

                                //R,G.B - Red, Green, Blue
                                //to restore the values after RGB modification, use
                                //next statement
                                pixytes[index] = (byte) (0xff000000 | (R << 16) | (G << 8) | B);
                            }
                        }
                        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
                        byte[] data = new byte[(int) (bitmap.getHeight() * bitmap.getWidth() * 1.5)];
                        rgba2Yuv420(pixytes, data, bitmap.getWidth(), bitmap.getHeight());
                        return mQRCodeView.processData(data, bitmap.getWidth(), bitmap.getHeight(), true);
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        if (TextUtils.isEmpty(result)) {
                            Toast.makeText(activity_test_scan.this, "未发现二维码", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity_test_scan.this, result, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    protected void onCancelled(String s) {
                        super.onCancelled(s);
                    }
                }.execute();
            }
        }

    }
    /**
     * 将本地图片文件转换成可解码二维码的 Bitmap。为了避免图片太大，这里对图片进行了压缩。感谢 https://github.com/devilsen 提的 PR
     *
     * @param picturePath 本地图片文件路径
     * @return
     */
    private static Bitmap getDecodeAbleBitmap(String picturePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(picturePath, options);
            int sampleSize = options.outHeight / 400;
            if (sampleSize <= 0) {
                sampleSize = 1;
            }
            options.inSampleSize = sampleSize;
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeFile(picturePath, options);
        } catch (Exception e) {
            return null;
        }
    }

    public static void rgba2Yuv420(byte[] src, byte[] dst, int width, int height) {
        // Y
        for (int y = 0; y < height; y++) {
            int dstOffset = y * width;
            int srcOffset = y * width * 4;
            for (int x = 0; x < width && dstOffset < dst.length && srcOffset < src.length; x++) {
                dst[dstOffset] = src[srcOffset];
                dstOffset += 1;
                srcOffset += 4;
            }
        }
        /* Cb and Cr */
        for (int y = 0; y < height / 2; y++) {
            int dstUOffset = y * width + width * height;
            int srcUOffset = y * width * 8 + 1;

            int dstVOffset = y * width + width * height + 1;
            int srcVOffset = y * width * 8 + 2;
            for (int x = 0; x < width / 2 && dstUOffset < dst.length && srcUOffset < src.length && dstVOffset < dst.length && srcVOffset < src.length; x++) {
                dst[dstUOffset] = src[srcUOffset];
                dst[dstVOffset] = src[srcVOffset];

                dstUOffset += 2;
                dstVOffset += 2;

                srcUOffset += 8;
                srcVOffset += 8;
            }
        }
    }
}
