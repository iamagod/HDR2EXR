/*
 * Based on Ichi Hirota's dual-fisheye plug-in for the THETA V.
 * Modified to use Shutter speed instead of exposure compensation
 * Added openCV support
 *
 * TODO v1
 * - make colors en sounds better
 * - clean up code?
 * - do all dirs exist? if not create!
 * - are all permissions okey? no error...
 * - get check if full white/black and stop
 * - set check if exposure values are the same
 * - set fixed wb
 * - split in two seperate pieces with unstiched version to get seperate crc
 * - save crc to disk
 * - load highend src from disk
 *
 * TODO v2
 * - add web interface
 * - turn sound on/off
 * - turn iso looping on/off
 * - exr half/full float on/off
 * - download exr
 * - name session
 * - viewer
 * - show status
 * - stops step setting?
 * - number of pics?
 * - dng and exr support
 */



//package com.theta360.pluginapplication;
package com.kasper.hdr4exr;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.support.media.ExifInterface;

import org.opencv.android.OpenCVLoader;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import org.opencv.core.Mat;
import java.lang.String;

import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;

import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;

import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.text.DecimalFormat;
import java.util.Arrays;

import static java.lang.Thread.sleep;
import java.util.ArrayList;


public class MainActivity extends PluginActivity implements SurfaceHolder.Callback {
    private Camera mCamera = null;
    private Context mcontext;
    private int bcnt = 0; //bracketing count
    private int shutterSpeedValue = 0;  // can be 0 to 62. 0 is 1/25000

    private static final int numberOfPictures = 13;
    private static final int shutterSpeedSpacing = 6;
    private static final Double stop_jumps = 1.0;



    Double[][] bracket_array = new Double[numberOfPictures][4];
    Mat times = new Mat(numberOfPictures,1,org.opencv.core.CvType.CV_32F);


    int current_count = 0;

    String session_name ="";
    List<Mat> images = new ArrayList<Mat>(numberOfPictures);

    // true will start with bracket
    private boolean m_is_bracket = true;
    private boolean m_is_auto_pic = true;

    private MatOfInt compressParams;


    Double shutter_table[][] =
            {
                    {0.0,  1/25000.0}, {1.0, 1/20000.0}, {2.0,  1/16000.0}, {3.0,  1/12500.0},
                    {4.0,  1/10000.0}, {5.0, 1/8000.0},  {6.0,  1/6400.0},  {7.0,  1/5000.0},
                    {8.0,  1/4000.0},  {9.0, 1/3200.0},  {10.0,	1/2500.0},  {11.0, 1/2000.0},
                    {12.0, 1/1600.0}, {14.0, 1/1000.0},  {15.0,	1/800.0},   {16.0, 1/640.0},
                    {17.0, 1/500.0},  {18.0, 1/400.0},   {19.0, 1/320.0},   {20.0, 1/250.0},
                    {21.0, 1/200.0},  {22.0, 1/160.0},   {23.0,	1/125.0},   {24.0, 1/100.0},
                    {25.0,	1/80.0}, {26.0,	1/60.0}, {27.0,	1/50.0}, {28.0,	1/40.0},
                    {29.0,	1/30.0}, {30.0,	1/25.0}, {31.0,	1/20.0}, {32.0,	1/15.0},
                    {33.0,	1/13.0}, {34.0,	1/10.0}, {35.0,	1/8.0}, {36.0,	1/6.0},
                    {37.0,	1/5.0}, {38.0,	1/4.0}, {39.0,	1/3.0}, {40.0,	1/2.5},
                    {41.0,	1/2.0}, {42.0,	1/1.6}, {43.0,	1/1.3}, {44.0,	1.0},
                    {45.0,	1.3}, {46.0,	1.6}, {47.0,	2.0}, {48.0,	2.5},
                    {49.0,	3.2}, {50.0,	4.0}, {51.0,	5.0}, {52.0,	6.0},
                    {53.0,	8.0}, {54.0,	10.0}, {55.0,	13.0}, {56.0,	15.0},
                    {57.0,	20.0}, {58.0,	25.0}, {59.0,	30.0}, {60.0,	60.0}
            };

    private static final String TAG = "MainActivity";

    static {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG,"OpenCV initialize success");
        } else {
            Log.i(TAG, "OpenCV initialize failed");
        }
    }
    /* Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_main);
        mcontext = this;
        SurfaceView preview = (SurfaceView)findViewById(R.id.preview_id);
        SurfaceHolder holder = preview.getHolder();
        holder.addCallback(this);
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                    // If on second run we need to reset everything.
                    notificationLedBlink(LedTarget.LED3, LedColor.GREEN, 300);
                    current_count = 0;
                    m_is_auto_pic = true;
                    times = new Mat(numberOfPictures,1,org.opencv.core.CvType.CV_32F);
                    images = new ArrayList<Mat>(numberOfPictures);
                    customShutter();
                }
                else if(keyCode == KeyReceiver.KEYCODE_WLAN_ON_OFF){ // Old code
                    notificationLedBlink(LedTarget.LED3, LedColor.MAGENTA, 300);

                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {
                /*
                 * You can control the LED of the camera.
                 * It is possible to change the way of lighting, the cycle of blinking, the color of light emission.
                 * Light emitting color can be changed only LED3.
                 */
            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
                notificationError("theta debug: " + Integer.toString(keyCode) + " was pressed too long");
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if(m_is_bracket){
            notificationLedBlink(LedTarget.LED3, LedColor.MAGENTA, 300);
        }
        else {
            notificationLedBlink(LedTarget.LED3, LedColor.CYAN, 2000);
        }
    }

    public void onPause() {
        super.onPause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.i(TAG,"Camera opened");
        Intent intent = new Intent("com.theta360.plugin.ACTION_MAIN_CAMERA_CLOSE");
        sendBroadcast(intent);
        mCamera = Camera.open();
        try {

            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {

            //e.printStackTrace();
            Log.i(TAG,"Camera opening error.");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        mCamera.stopPreview();
        Camera.Parameters params = mCamera.getParameters();
        params.set("RIC_SHOOTING_MODE", "RicMonitoring");

        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        Camera.Size size = previewSizes.get(0);
        for(int i = 0; i < previewSizes.size(); i++) {
            size = previewSizes.get(i);
            Log.d(TAG,"preview size = " + size.width + "x" + size.height);
        }
        params.setPreviewSize(size.width, size.height);
        mCamera.setParameters(params);
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        Log.d(TAG,"camera closed");
        notificationLedHide(LedTarget.LED3);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        Intent intent = new Intent("com.theta360.plugin.ACTION_MAIN_CAMERA_OPEN");
        sendBroadcast(intent);
    }

    private void customShutter(){
        Intent intent = new Intent("com.theta360.plugin.ACTION_AUDIO_SH_OPEN");
        sendBroadcast(intent);

        Camera.Parameters params = mCamera.getParameters();
        //Log.d("shooting mode", params.flatten());
        params.set("RIC_SHOOTING_MODE", "RicStillCaptureStd");

        params.set("RIC_PROC_STITCHING", "RicNonStitching");
        params.setPictureSize(5792, 2896); // no stiching

        params.setPictureFormat(ImageFormat.JPEG);
        params.set("jpeg-quality",100);
        //params.setPictureSize(5376, 2688); // stiched

        // https://api.ricoh/docs/theta-plugin-reference/camera-api/
        //Shutter speed. To convert this value to ordinary 'Shutter Speed';
        // calculate this value's power of 2, then reciprocal. For example,
        // if value is '4', shutter speed is 1/(2^4)=1/16 second.
        //params.set("RIC_EXPOSURE_MODE", "RicManualExposure");

        //params.set("RIC_MANUAL_EXPOSURE_TIME_REAR", -1);
        //params.set("RIC_MANUAL_EXPOSURE_ISO_REAR", -1);


        // So here we take our first picture on full auto settings to get
        // proper lighting settings to use a our middle exposure value
        params.set("RIC_EXPOSURE_MODE", "RicAutoExposureP");

        bcnt = numberOfPictures;



        mCamera.setParameters(params);
        //params = mCamera.getParameters();

        session_name = getSessionName();
        Log.i(TAG,"Starting new session with name: " + session_name);
        Log.i(TAG,"About to take first auto picture to measure lighting settings.");
        new File(Environment.getExternalStorageDirectory().getPath()+ "/DCIM/100RICOH/"+session_name).mkdir();

        //Log.d("get", params.get("RIC_MANUAL_EXPOSURE_ISO_BACK"));

        //3sec delay timer to run away

        try{
            sleep(3000);
        } catch (InterruptedException e) {
            //e.printStackTrace();
            Log.i(TAG,"Sleep error.");


        }
        intent = new Intent("com.theta360.plugin.ACTION_AUDIO_SHUTTER");
        sendBroadcast(intent);
        mCamera.takePicture(null,null, null, pictureListener);
    }

    private void nextShutter(){
        //restart preview
        Camera.Parameters params = mCamera.getParameters();
        params.set("RIC_SHOOTING_MODE", "RicMonitoring");
        mCamera.setParameters(params);
        mCamera.startPreview();

        //shutter speed based bracket
        if(bcnt > 0) {
            params = mCamera.getParameters();
            params.set("RIC_SHOOTING_MODE", "RicStillCaptureStd");
            shutterSpeedValue = shutterSpeedValue + shutterSpeedSpacing;
            if ( m_is_auto_pic) {
                // So here we take our first picture on full auto settings to get
                // proper lighting settings to use a our middle exposure value
                params.set("RIC_EXPOSURE_MODE", "RicAutoExposureP");
            }
            else
            {
                params.set("RIC_EXPOSURE_MODE", "RicManualExposure");
                params.set("RIC_MANUAL_EXPOSURE_TIME_REAR", bracket_array[current_count][1].intValue());
                params.set("RIC_MANUAL_EXPOSURE_ISO_REAR",  bracket_array[current_count][0].intValue());
                // for future possibilities we add this but it turns out to be discarded
                params.set("RIC_MANUAL_EXPOSURE_TIME_FRONT", bracket_array[current_count][1].intValue());
                params.set("RIC_MANUAL_EXPOSURE_ISO_FRONT",  bracket_array[current_count][0].intValue());
            }

            bcnt = bcnt - 1;
            mCamera.setParameters(params);
            Intent intent = new Intent("com.theta360.plugin.ACTION_AUDIO_SHUTTER");
            sendBroadcast(intent);
            mCamera.takePicture(null, null, null, pictureListener);
        }
        else{
            // reset shutterSpeedValue
            shutterSpeedValue = 0;

            Log.i(TAG,"Done with picture taking, let's start with the HDR merge.");
            Log.d(TAG,"images is: "+Integer.toString(images.size()) );
            Log.d(TAG,"times length is: " + Long.toString(times.total()));
            notificationLedBlink(LedTarget.LED3, LedColor.BLUE, 300);
            String opath ="";

            //Log.d(TAG,"starting align");
            //org.opencv.photo.AlignMTB align = org.opencv.photo.Photo.createAlignMTB();
            //align.process(images,images);

            Log.i(TAG,"Starting calibration.");

            Mat responseDebevec = new Mat();
            org.opencv.photo.CalibrateDebevec calibrateDebevec = org.opencv.photo.Photo.createCalibrateDebevec();
            calibrateDebevec.process(images, responseDebevec, times);

            Log.i(TAG,"Preping merge.");

            Mat hdrDebevec = new Mat();
            org.opencv.photo.MergeDebevec mergeDebevec = org.opencv.photo.Photo.createMergeDebevec();
            Log.i(TAG,"Starting merge.");
            mergeDebevec.process(images, hdrDebevec, times, responseDebevec);

            // Save HDR image.
            //Log.i(TAG,"Saving file.");
            opath = Environment.getExternalStorageDirectory().getPath()+ "/DCIM/100RICOH/" + session_name + ".exr";
            Log.i(TAG,"Saving file as " + opath + ".");
            compressParams = new MatOfInt(org.opencv.imgcodecs.Imgcodecs.CV_IMWRITE_EXR_TYPE, org.opencv.imgcodecs.Imgcodecs.IMWRITE_EXR_TYPE_HALF);


            //We divide by the mean value of the whole picture to get the exposure values with a proper range.

            //Mat divide_hdr  = new Mat();

            Scalar mean =  org.opencv.core.Core.mean(hdrDebevec);
            Log.d(TAG,"Mean: " + mean.toString());
            double new_mean = (mean.val[0]+mean.val[1]+mean.val[2])/3.0;
            Log.i(TAG,"Average Mean: " + Double.toString(new_mean));
            org.opencv.core.Core.divide(hdrDebevec,new Scalar(new_mean,new_mean,new_mean,0),hdrDebevec);

            //opath = Environment.getExternalStorageDirectory().getPath()+ "/DCIM/100RICOH/" + session_name + "_mean.exr";
            imwrite(opath, hdrDebevec,compressParams);

            Log.i(TAG,"HDR save done.");

            Log.i(TAG,"----- JOB DONE -----");
            notificationLedBlink(LedTarget.LED3, LedColor.MAGENTA, 300);

            Intent intent = new Intent("com.theta360.plugin.ACTION_AUDIO_SH_CLOSE");
            sendBroadcast(intent);
        }

    }
    private double find_closest_shutter(double shutter_in)
    {
        int i;
        for( i=0; i<60; i++){
            if (shutter_table[i][1] > shutter_in) {
                break;
            }
        }
        return shutter_table[i][0];
    }

    private Camera.PictureCallback pictureListener = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //save image to storage
            Log.d(TAG,"onpicturetaken called ok");
            if (data != null) {
                FileOutputStream fos;
                try {
                    String tname = getNowDate();
                    String extra;

                    // get picture info, iso and shutter
                    Camera.Parameters params = mCamera.getParameters();
                    String flattened = params.flatten();
                    StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
                    String text;
                    String cur_shutter = "";
                    String cur_iso  = "";
                    while (tokenizer.hasMoreElements())
                    {
                        text = tokenizer.nextToken();
                        if (text.contains("cur-exposure-time"))
                        {
                            cur_shutter = text.split("=")[1];
                            Log.d(TAG,"INFO after: "+text);
                        }
                        /*else if (text.contains("RIC_"))
                        {
                            Log.d("INFO" ,"after: "+text);
                        }*/
                        else if (text.contains("cur-iso"))
                        {
                            cur_iso = text.split("=")[1];
                            Log.d(TAG,"INFO after: "+text);
                        }
                    }
                    if ( m_is_auto_pic)
                    {
                        // Here we populate the bracket_array based on the base auto exposure picture.
                        extra = "auto_pic";

                        // cur_shutter is in mille seconds and a string
                        Float shutter = Float.parseFloat(cur_shutter)/1000;
                        Float iso_flt  =  Float.parseFloat(cur_iso);

                        Float new_shutter = shutter * iso_flt/100*2;
                        //find_closest_shutter(new_shutter);
                        Log.d(TAG,"New shutter number " + Double.toString(new_shutter));

                        Log.d(TAG,"Closest shutter number " + Double.toString(find_closest_shutter(new_shutter)));

                        // iso is always the lowest for now maybe alter we can implement a fast option with higher iso
                        // bracket_array =
                        // {{iso,shutter,bracketpos, shutter_length_real },{iso,shutter,bracketpos,shutter_length_real },{iso,shutter,bracketpos,shutter_length_real },....}
                        // {{50, 1/50, 0},{50, 1/25, +1},{50,1/100,-1},{50,1/13,+2},....}
                        for( int i=0; i<numberOfPictures; i++)
                        {
                            boolean reached_18 = false;
                            bracket_array[i][0] = 1.0;
                            // 0=0  1 = *2,+1  2 = /2, -1, 3 = *4=2^2,+2, 4=/4=2^2,-2 5 = *8=2^3,+3, 6 = /8=2^3
                            if ( (i & 1) == 0 )
                            {
                                //even...
                                bracket_array[i][1] = find_closest_shutter(new_shutter/( Math.pow(2,stop_jumps *  Math.ceil(i/2.0))));
                                bracket_array[i][2] = -1 * Math.ceil(i/2.0);
                                bracket_array[i][3] = shutter_table[bracket_array[i][1].intValue()][1];
                                times.put(i,0, shutter_table[bracket_array[i][1].intValue()][1]);
                            }
                            else
                             {
                                 //odd...
                                 Double corrected_shutter = new_shutter*(Math.pow(2,stop_jumps *Math.ceil(i/2.0)));
                                 int iso = 1;

                                 int j;
                                 for( j=1; j<shutter_table.length-1; j++){
                                     if (shutter_table[j][1] > corrected_shutter) {
                                         break;
                                     }
                                 }
                                 bracket_array[i][3] = shutter_table[j][1];
                                 times.put(i,0, shutter_table[j][1]);

                                 if ((corrected_shutter >= 1.0))
                                 {
                                     // If shutter value goes above 1 sec we increase iso unless we have reached highest iso already

                                     while (corrected_shutter >=1.0 && !( reached_18))
                                     {
                                         corrected_shutter = corrected_shutter/2.0;
                                         if (iso == 1) { iso =3; }
                                         else          { iso = iso + 3; }
                                         if (iso >=18)
                                         {
                                             iso=18;
                                             //if (reached_18) {corrected_shutter = corrected_shutter * 2.0;}
                                             reached_18 = true;

                                         }

                                     }
                                 }
                                 if ((reached_18) && (bracket_array[i-2][0] == 18))
                                 {
                                     // previous one was already at highest iso.
                                     bracket_array[i][0] = 18.0;
                                     bracket_array[i][1] = find_closest_shutter(corrected_shutter);

                                 }
                                 bracket_array[i][0] = Double.valueOf(iso);
                                 bracket_array[i][1] = find_closest_shutter(corrected_shutter);
                                 bracket_array[i][2] = Math.ceil(i/2.0);

                             }
                            Log.i(TAG,"Array: index "+Integer.toString(i) +
                                    " iso #: "+Integer.toString(bracket_array[i][0].intValue())+
                                    " shutter #: "+Integer.toString(bracket_array[i][1].intValue())+
                                    " bracketpos : "+Integer.toString(bracket_array[i][2].intValue())+
                                    " real shutter length : "+Double.toString(bracket_array[i][3]));
                        }
                        m_is_auto_pic = false;
                    }
                    else // not auto pic so we are in bracket loop
                    {
                        if ( (current_count & 1) == 0 ) {
                            //even is min
                            extra = "i" + Integer.toString(current_count) + "_m" + Integer.toString(Math.abs(bracket_array[current_count][2].intValue()));
                        }
                        else
                        {
                            //oneven is plus
                            extra = "i" + Integer.toString(current_count) + "_p" + Integer.toString(bracket_array[current_count][2].intValue());
                        }

                        current_count++;

                    }

                    //sort array from high to low
                    Arrays.sort(bracket_array, (a, b) -> Double.compare(a[2], b[2]));

                    String opath = Environment.getExternalStorageDirectory().getPath()+ "/DCIM/100RICOH/" +  session_name + "/" + extra + ".jpg";
                    //String opath = Environment.getExternalStorageDirectory().getPath()+ "/DCIM/100RICOH/IMG_" + Integer.toString(current_count) + ".JPG";

                    fos = new FileOutputStream(opath);
                    fos.write(data);
                    ExifInterface exif = new ExifInterface(opath);

                    if (!extra.contains("auto_pic")) // setup opencv array for hdr merge
                            {
                                images.add(imread(opath));
                            };

                    String shutter_str = exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE);
                    Float shutter_flt = (Float.parseFloat(shutter_str.split("/")[0]) / Float.parseFloat(shutter_str.split("/")[1]));
                    String out ="";
                    if ( shutter_flt>0 )
                    {
                        out = "1/"+Double.toString(Math.floor(Math.pow(2,shutter_flt)));
                    }
                    else
                    {
                        out = Double.toString(1.0/(Math.pow(2,shutter_flt)));
                    }
                    //String shttr_str = exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE);
                    //Log.i(TAG,"shutter_float is" + shutter_flt);
                    Float shutter_speed_float = Float.parseFloat(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME));
                    DecimalFormat df = new DecimalFormat("00.00000");
                    df.setMaximumFractionDigits(5);
                    String shutter_speed_string = df.format(shutter_speed_float);

                    //File fileold = new File(opath);
                    String opath_new = Environment.getExternalStorageDirectory().getPath()+ "/DCIM/100RICOH/" +
                            session_name + "/" + extra +
                            "_iso" +exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS) +
                            "_shutter" + shutter_speed_string +
                            "sec.jpg";
                    //File filenew = ;

                    new File(opath).renameTo(new File(opath_new));
                    Log.i(TAG,"Saving file " + opath_new);
                    Log.i(TAG,"Shot with iso " + exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS) +" and a shutter of "+  shutter_speed_string + " sec.\n");
                    Log.d(TAG,"EXIF iso value: " + exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS));
                    Log.d(TAG,"EXIF shutter value " + exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE) + " or " + out + " sec.");
                    Log.d(TAG,"EXIF shutter value/exposure value " + exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) + " sec.");
                    Log.d(TAG,"EXIF Color Temp: " + exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE));
                    Log.d(TAG,"EXIF white point: " + exif.getAttribute(ExifInterface.TAG_WHITE_POINT));


                    fos.close();
                    registImage(tname, opath, mcontext, "image/jpeg");
                } catch (Exception e) {
                    Log.i(TAG,"Begin big error.");
                    e.printStackTrace();
                    Log.i(TAG,"End big error.");

                }

                nextShutter();
            }
        }
    };
    private static String getNowDate(){
        final DateFormat df = new SimpleDateFormat("HH_mm_ss");
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

    private static String getSessionName(){
        final DateFormat df = new SimpleDateFormat("yyMMdd_HHmm");
        final Date date = new Date(System.currentTimeMillis());
        return "S" + df.format(date) ;
    }

    private static void registImage(String fileName, String filePath, Context mcontext, String mimetype) {
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = mcontext.getContentResolver();
        //"image/jpeg"
        values.put(MediaStore.Images.Media.MIME_TYPE, mimetype);
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put("_data", filePath);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }


}



