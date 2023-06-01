package com.example.imagepro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG="MainActivity";

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;

    private ImageView translate_button;
    private ImageView take_picture_button;
    private ImageView show_image_button;

    private ImageView current_image;
    private TextView textview;

    private TextRecognizer textRecognizer;

    private String Camera_or_recognizeText="Camera";
    private Bitmap bitmap=null;

    private ArrayList<String> extractAlphabeticText(String inputText) {
        ArrayList<String> names = new ArrayList<String>();
//        StringBuilder extractedText = new StringBuilder();
        String lines[] = inputText.split("\n");
        for (String s : lines) {
            System.out.println(s);
            String a[] = s.split(" |/");
            names.add(a[1]);
            System.out.println("쪼갠결과: ");
            for (String ss : a) {
                System.out.println(ss);
            }
        }

        return names;
//        System.out.println(inputText);
//        for (char c : inputText.toCharArray()) {
//            if (Character.isLetter(c)) {
//                extractedText.append(c);
//            }
//        }
//       return extractedText.toString();
    }


    private BaseLoaderCallback mLoaderCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface
                        .SUCCESS:{
                    Log.i(TAG,"OpenCv Is loaded");
                    mOpenCvCameraView.enableView();
                }
                default:
                {
                    super.onManagerConnected(status);

                }
                break;
            }
        }
    };

    public CameraActivity(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int MY_PERMISSIONS_REQUEST_CAMERA=0;
        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView=(CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        textRecognizer=TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
//part4 3:34
        textview=findViewById(R.id.textview);
        textview.setVisibility(View.GONE);
        current_image=findViewById(R.id.current_image);
        current_image.setVisibility(View.GONE);


        take_picture_button=findViewById(R.id.take_picture_button);
        take_picture_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {

                    if(Camera_or_recognizeText=="camera"){//카메라를 사용하고 사진 버튼을 누르면
                        take_picture_button.setColorFilter(Color.DKGRAY);//버튼 색 바뀜
                        Mat a=mRgba.t();//현재 mRgba 프레임을 피트맵으로 전환하고 저장한다 mRgba는 제일 먼저 정의 된다.
                        Core.flip(a,mRgba,1);//셀카모드 사진을 반전시킴
                        a.release();
                        bitmap=Bitmap.createBitmap(mRgba.cols(),mRgba.rows(),Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(mRgba,bitmap);
                        mOpenCvCameraView.disableView();

                        Camera_or_recognizeText="recognizerText";
                    }
                    else{
                        take_picture_button.setColorFilter(Color.WHITE);
                        textview.setVisibility(View.GONE);
                        current_image.setVisibility(View.GONE);
                        textview.setText("");
                        Camera_or_recognizeText="camera";

                    }

                    return true;
                }
                return false;
            }
        });

        translate_button=findViewById(R.id.translate_button);
        translate_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    translate_button.setColorFilter(Color.DKGRAY);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    translate_button.setColorFilter(Color.WHITE);
                    if(Camera_or_recognizeText=="recognizerText"){

                        textview.setVisibility(View.VISIBLE);

                        InputImage image=InputImage.fromBitmap(bitmap,0);

                        Task<Text> result = textRecognizer.process(image)
                                .addOnSuccessListener(new OnSuccessListener<Text>() {
                                    @Override
                                    public void onSuccess(Text text) {
                                        ArrayList<String> names;
                                        names = extractAlphabeticText(text.getText());

                                        for(String name : names) {
                                            Log.d("CameraActivity","Out: "+ name);//->getText()해온거 보내기
                                            Toast.makeText(CameraActivity.this, name, Toast.LENGTH_SHORT).show();
                                        }



                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                    }
                    else{
                        Toast.makeText(CameraActivity.this,"Please take a picture", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });
        show_image_button=findViewById(R.id.show_image_button);
        show_image_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    show_image_button.setColorFilter(Color.DKGRAY);
                    return true;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    show_image_button.setColorFilter(Color.WHITE);
                    if(Camera_or_recognizeText=="recognizeText"){


                    }
                    else{
                        Toast.makeText(CameraActivity.this,"Please take a picture", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            //if load success
            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            //if not loaded
            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width ,int height){
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        mGray =new Mat(height,width,CvType.CV_8UC1);
    }
    public void onCameraViewStopped(){
        mRgba.release();
    }
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mRgba=inputFrame.rgba();
        mGray=inputFrame.gray();

        return mRgba;

    }

}