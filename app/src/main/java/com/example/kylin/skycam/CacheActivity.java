package com.example.kylin.skycam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class CacheActivity extends ActionBarActivity {
    static ImageAdapter gridadapter;
    String ip;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache);

        ip = getResources().getString(R.string.ip);
        gridadapter = new ImageAdapter(this);
        GridView gridview = (GridView) findViewById(R.id.gridView);
        gridview.setNumColumns(3);
        gridview.setAdapter(gridadapter);


    }//end oncreate


    private String getfilename(final String serUrl) {
        HttpPost request = new HttpPost(serUrl);
        List<NameValuePair> parlist = new ArrayList<NameValuePair>();
        parlist.add(new BasicNameValuePair("data", "haha"));
        final String result;
        try {
            request.setEntity(new UrlEncodedFormEntity(parlist, HTTP.UTF_8));
            HttpResponse resp = new DefaultHttpClient().execute(request);
            if (resp.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(resp.getEntity());
                return result;
            }
        } catch (IOException e) {

        }
        return null;
    }

    public class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private boolean update;
        int downloadInd;

        public ImageAdapter(Context c) {
            mContext = c;
            update = false;
            downloadInd=0;
        }

        public int getCount() {
            return mThumbIds.length;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }



        // create a new ImageView for each item referenced by the Adapter
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(120, 120));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }



            new AsyncTask<String, Void, Uri>() {
                @Override
                protected Uri doInBackground(String... params) {
//                    String dirurl="http://192.168.1.142/upload_sky/";
                    String dir= "upload_sky_compre/";
                    if (!update) {
                        //TODO Cache store
                        String serUrl = "http://" + ip + "/getUrl_JSON.php";
                        String result = getfilename(serUrl);
                        String filename ="";
                        JSONObject obj;
                        try {
                            JSONArray jsonArray = new JSONArray(result);
                            for(int i=0;i<jsonArray.length();i++){
                                obj = jsonArray.getJSONObject(i);
                                filename= obj.getString("name");
                                UriList.add(getFileUri("http://" + ip + "/" + dir + filename, filename));
                            }

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        mThumbIds = filename.split(" ");
                        update = true;
                    }

                    return UriList.get(position);
                    //return getBitmapFromURL("http://" + ip + "/"+dir + mThumbIds[position]);
                }

                @Override
                protected void onPostExecute(Uri result) {
                    //TODO OOM

                    imageView.setImageURI(result);
                    super.onPostExecute(result);
//                    if(!result.isRecycled()){
//                        result.recycle();
//                    }
                }
            }.execute();

            return imageView;
        }


        // references to our images
        //"http://"+ip+"/upload_sky/IMG_20150313_134714.jpg";
        private  ArrayList<Uri> UriList = new ArrayList<>();

        private String[] mThumbIds = {
                ""
        };

        private Uri getFileUri(String fileUrl, String filename){

//            File CacheDir = new File(getApplicationContext().getCacheDir(),getResources().getString(R.string.app_name));
            File CacheDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES),getResources().getString(R.string.app_name));

            if (!CacheDir.exists()) {
                if (!CacheDir.mkdirs()) {
                    Log.d("SkyCam", "failed to create directory");
                    return null;
                }
            }
            File FileTemp = new File(CacheDir.getPath() + File.separator + filename);
            Uri uritmep;

            try {
                //outputStream = openFileOutput(filename,Context.MODE_PRIVATE);
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream input = connection.getInputStream();    //get input stream
                BufferedInputStream bis = new BufferedInputStream(input);   //read in buffer stream

                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current =0 ;
                while ((current= bis.read()) != -1){
                    baf.append((byte)current);
                }
                FileOutputStream fos = new FileOutputStream(FileTemp);
                fos.write(baf.toByteArray());
                fos.close();

            }catch (IOException e){
                e.printStackTrace();
            }
            return Uri.parse(FileTemp.getAbsolutePath());
        }
    }




    private static Bitmap getBitmapFromURL(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();

            InputStream input = connection.getInputStream();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;

//            File mediaStorageDir = new File(Environment.getDownloadCacheDirectory(), "SkyCam");
//            FileOutputStream outputStream= new FileOutputStream()
//

            return BitmapFactory.decodeStream(input, null, options);
//            return BitmapFactory.decodeStream(input);
            //直接調用JNI>>nativeDecodeAsset()來完成decode，無需再使用java層的createBitmap，從而節省了java層的空間。
        } catch (IOException e) {

            e.printStackTrace();
            return null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cache, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
