package com.example.kylin.skycam;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends ActionBarActivity {
    private String ip;
    private String uploadUrl = "http://" + getResources().getString(R.string.ip) + "/upload_sky.php";
    ProgressDialog dialog = null;
    Button CamB, SettingB;
    ImageButton imgtake;
    Camera Cam;
    CameraPreview mPreview;
    //    Bitmap bitmapPicture;
    private boolean preview = false;
    private int serverResponseCode;

//    SampleFileUpload fileUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CamB = (Button) findViewById(R.id.CamB);
        SettingB = (Button) findViewById(R.id.setting);
        imgtake = (ImageButton) findViewById(R.id.imageButton);
        ip = getResources().getString(R.string.ip);
        uploadUrl = "http://" + ip + "/upload_sky.php";
        Cam = getCameraInstance();

        getWindow().setFormat(PixelFormat.UNKNOWN);//initial to zero
        mPreview = new CameraPreview(this, Cam);

        FrameLayout preview = (FrameLayout) findViewById(R.id.CamView);
        preview.addView(mPreview);
        preview.setPadding(0, 0, 0, 0);
        mPreview.setPadding(0, 0, 0, 0);
//        fileUpload = new SampleFileUpload () ;
        settingBuild();
        CamB.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        Cam.takePicture(shutterCallback, null, mPicture);
//                        Cam.takePicture();
                    }
                }
        );

        SettingB.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        //todo
                        SettingDialog.show();
                    }
                }
        );

        imgtake.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //todo upload file
//                        String response = fileUpload.executeMultiPartRequest(uploadUrl,
//                                pictureFile, pictureFile.getName(), "File Upload test jpg description") ;
//                        Toast.makeText(getBaseContext(), response, Toast.LENGTH_SHORT).show();

                        dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
                        new Thread(new Runnable() {
                            public void run() {

                                uploadFile(pictureFile.getAbsolutePath());

                            }
                        }).start();

                    }
                }
        );
    }

    final Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            Cam.startPreview();
        }
    };

    public int uploadFile(String sourceFileUri) {


        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {
            //if no file
            dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :" + sourceFile.getPath());

            runOnUiThread(new Runnable() {
                public void run() {
//                    messageText.setText("Source File not exist :"+ imagepath);
                }
            });
            return 0;

        }//end if
        else {
            try {

                // open a URL connection to the Servlet
                URL url = new URL(uploadUrl);
                FileInputStream fileInputStream = new FileInputStream(sourceFile);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + fileName + "\"" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            String msg = "File Upload Completed.\n\n See uploaded file your server. \n\n";
                            //messageText.setText(msg);
                            Toast.makeText(getBaseContext(), "File Upload Complete.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
//                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(getBaseContext(), "MalformedURLException", Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {

                        Toast.makeText(getBaseContext(), "Got Exception : see logcat ", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file Exception", "Exception : " + e.getMessage(), e);
            }
            dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }

    View settingV;
    AlertDialog SettingDialog;
    EditText iptext;

    public void settingBuild() {
        LayoutInflater inflater = LayoutInflater.from(this);
        settingV = inflater.inflate(R.layout.dialog_setting, null);
        iptext = (EditText) settingV.findViewById(R.id.ip_edit);

        SettingDialog = new AlertDialog.Builder(this)
                .setView(settingV)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setIp(iptext.getText().toString());

                        uploadUrl = "http://" + ip + "/upload_sky.php";
                    }
                })
                .create();
    }

    public void setIp(String s) {
        ip = s;
    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    File pictureFile;
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        private String TAG = "PictureCallback";

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
//            pictureFile.delete();
            pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                byte[] array;
                if (rotation == Surface.ROTATION_0) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();
// Setting post rotate to 90
                    Matrix mtx = new Matrix();
                    mtx.postRotate(90);
// Rotating Bitmap
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);

                    imgtake.setImageBitmap(bitmap);
                    imgtake.refreshDrawableState();
                    array = out.toByteArray();
                    fos.write(array);   // after rotate

                } else {
                    fos.write(data);
                    imgtake.setImageBitmap(bitmap);
                    imgtake.refreshDrawableState();
                }


                fos.close();

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            } finally {

            }
        }
    };

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private String TAG = "CAM";

        public CameraPreview(MainActivity context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                if (mCamera == null) {
                    mCamera = getCameraInstance();
                }
                holder.addCallback(this);
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                preview = true;
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
            mCamera.stopPreview();
            mCamera.release();
            mHolder.removeCallback(mPreview);
            preview = false;
        }


        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.
            if (preview) {
                Cam.stopPreview();
                preview = false;
            }
            // stop preview before making changes
            try {
                mCamera.setPreviewDisplay(holder);
                setCameraDisplayOrientation(mCamera);
                mCamera.startPreview();
                preview = true;
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
                Log.d(TAG, "Error starting camera preview 1: " + e.getMessage());

            }

            // set preview size and make any resize, rotate or
            // reformatting changes here
            // start preview with new settings
        }
    }//end campreview class

    public void setCameraDisplayOrientation(android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            c.setDisplayOrientation(90);
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            CamB.setRotation(0);

        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            CamB.setRotation(90);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent = new Intent();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_GridView) {

//            intent.setClass(this, WebViewActivity.class);
            intent.setClass(this, GridViewActivity.class);
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "MyCameraApp");
            Bundle bundle = new Bundle();
            bundle.putString(getResources().getString(R.string.PictureDir), mediaStorageDir.getPath());
            intent.putExtras(bundle);

//            startActivityForResult(intent,getResources().getInteger(R.integer.MainRequest));
            startActivity(intent);
//            Cam =null;
        } else if (id == R.id.action_webView) {

            intent.setClass(this, WebViewActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }


}
