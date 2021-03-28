package wsc.hudsquad2driver;

/*
 * Author: Rishabh Bhatia
 * Author email: bhatiari@deakin.edu.au
 * Year: 2019
 * */

import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Surface;



import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;


public class MainActivity extends BaseActivity {

    private Handler mDelayHandler = new Handler();
    private Runnable mCameraRunnable = () -> {
        checkPermissionOpenCamera();
    };

    //SeekBar speedSet;//Seekbar object
    public TextView speed, time, distance, temp, batterytv, rangeLeft, motorTempView;//Textview objects
    int touchCount = 0;
    EditText link;//Under cover Url input edit text
    //BigDecimal dist = new BigDecimal(0.1);
    ProgressBar battery, motortempbar;
    double dist = 0;//Distance travelled
    ImageView left, right, hazard, lowBeam, highBeam, handBrake, seatBelt, airBag, doorOpen, abs, malfunction;//Imageview objects
    public String leftFlag = "OFF", rightFlag = "OFF";//Flags for indicators
    String url = "0.0.0.0:5000";//url for simulator network
    int delay = 0;//Time taken by the timer before the first execution
    int period = 500;//Interval after which the timer repeats
    int s = 0;//Value of ProgressBar's realtime position based on which odometric calculations are conducted
    double ss;//double value of s for distance calculation
    int speedDelay = 0;//Timer delay. Can be removed after simulator is connected
    int speedPeriod = 500;//Timer period. Can be removed after simulator is connected
    int flag = 0;//Flag for speed timer
    float bat1 = 0, bat2 = 35, bat3 = 45, bat4 = 0, avgBat;//Battery percentage from individual cell
    float x1, x2, y1, y2;//Initialising coordinates of Ontouchevent
    static boolean active = false;//Setting a boolean to check if an activity is active
    String dummy = "";//Empty string in case no url has been entered

    private static final float[] BANDWIDTH_FACTORS = {0.5f, 0.5f};

    private USBMonitor mUSBMonitor;

    private List<UsbDevice> mUsbDeviceList = new ArrayList<>();
    private ArrayList<UVCCameraHandler> mUvcCameraHandlerList = new ArrayList<>();
    private ArrayList<CameraViewInterface> mCameraViewInterface = new ArrayList<>();
    public List<DeviceFilter> deviceFilters;

    int mUvcCameraSum = 2;

    public void initCamera(CameraViewInterface uvcCameraInterface) {
        uvcCameraInterface.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        UVCCameraHandler cameraHandler = UVCCameraHandler.createHandler(this, uvcCameraInterface
                , UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT
                , BANDWIDTH_FACTORS[0]);
        mCameraViewInterface.add(uvcCameraInterface);
        mUvcCameraHandlerList.add(cameraHandler);


    }

    public void checkPermissionOpenCamera() {
        int j = 0;
        for (int i = 0; i < mUsbDeviceList.size(); i++) {
            UsbDevice usbDevice = mUsbDeviceList.get(i);

            if (usbDevice.getVendorId() == 1266)
            {
                j=0;
            }

            if (usbDevice.getVendorId() == 6408)
            {
                j=1;
            }
            UVCCameraHandler uvcCameraHandler = mUvcCameraHandlerList.get(i);

            if (uvcCameraHandler.isOpened()) {
                uvcCameraHandler.close();
            }

            if (null != mUSBMonitor) mUSBMonitor.requestPermission(mUsbDeviceList.get(j));
            SystemClock.sleep(500);
        }


    }



    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
        for (CameraViewInterface cameraViewInterface : mCameraViewInterface) {
            if (null != cameraViewInterface) cameraViewInterface.onResume();
        }
    }

    @Override
    protected void onStop() {
        for (int i = 0; i < mCameraViewInterface.size(); i++){
            UVCCameraHandler uvcCameraHandler = mUvcCameraHandlerList.get(i);
            if (null != uvcCameraHandler) uvcCameraHandler.close();
            CameraViewInterface cameraViewInterface = mCameraViewInterface.get(i);
            if (null != cameraViewInterface) cameraViewInterface.onPause();
        }
        mUSBMonitor.unregister();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        super.onDestroy();
    }




    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {

            for (int i = 0; i < mUvcCameraHandlerList.size(); i++) {
                UVCCameraHandler uvcCameraHandler = mUvcCameraHandlerList.get(i);
                if (!uvcCameraHandler.isOpened()) {
                    uvcCameraHandler.open(ctrlBlock);
                    CameraViewInterface cameraViewInterface = mCameraViewInterface.get(i);
                    SurfaceTexture st = cameraViewInterface.getSurfaceTexture();
                    uvcCameraHandler.startPreview(new Surface(st));
                    break;
                }
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
        }

        @Override
        public void onDettach(final UsbDevice device) {
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };


    private Socket socket;
    {
        try {
            //localhost, 127.0.0.1 OR [::1] - will not work here. Use your local ip address
            socket = IO.socket("http://" + url); //http://YourLocalIPAddress:8000
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        deviceFilters = DeviceFilter.getDeviceFilters(this, R.xml.device_filter);
        mUsbDeviceList = mUSBMonitor.getDeviceList(deviceFilters.get(0));
        for (int i = 0; i < mUvcCameraSum; i++) {
            CameraViewInterface uvcCameraInterface = null;
            switch (i) {
                case 0:
                    uvcCameraInterface = findViewById(R.id.camera_view_L);
                    break;
                case 1:
                    uvcCameraInterface = findViewById(R.id.camera_view_R);
                    break;
            }
            if (null != uvcCameraInterface) initCamera(uvcCameraInterface);
        }
        mDelayHandler.postDelayed(mCameraRunnable, 1000);

        //speedSet = findViewById(R.id.seekBar);
        speed = findViewById(R.id.speedTextView);
        temp = (TextView) findViewById(R.id.ambTemp);//Ambient temparature
        time = (TextView) findViewById((R.id.textView2));
        distance = (TextView) findViewById(R.id.distText);
        left = (ImageView) findViewById(R.id.leftIndicateImage);
        right = (ImageView) findViewById(R.id.rightIndicateImage);
        lowBeam = (ImageView) findViewById(R.id.lowBeam);//Low beam icon
        highBeam = (ImageView) findViewById(R.id.highBeam);//High beam icon
        handBrake = (ImageView) findViewById(R.id.handbrake);//Handbrake icon
        seatBelt = (ImageView) findViewById(R.id.seatBelt);//Seatbelt icon
        airBag = (ImageView) findViewById(R.id.airBag);//Seatbelt icon
        doorOpen = findViewById(R.id.doorOpen);//Door open icon
        abs = findViewById(R.id.abs);//abs icon
        malfunction = findViewById(R.id.malfunction);//Malfunction for battery or motor
        link = findViewById(R.id.editText);
        battery = findViewById((R.id.batteryBar));
        motortempbar = findViewById((R.id.motorTempBar));
        batterytv = findViewById((R.id.battv));
        rangeLeft = findViewById(R.id.rangeView);
        hazard = findViewById(R.id.hazardSwitch);
        motorTempView = findViewById(R.id.motorTempView);

        battery.setScaleY(10f);
        motortempbar.setScaleY(10f);



        hazard.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                String imageName1 = (String)hazard.getTag();
                Log.d("State", "a");





            }
        });

        //Setting long press on temperature text to enable/disable url input
        temp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(link.getVisibility() == View.VISIBLE)
                {
                    link.setVisibility(view.GONE);
                    url = link.getText().toString();

                    Toast.makeText(getApplicationContext(), url, Toast.LENGTH_LONG).show();
                    try {
                        //localhost, 127.0.0.1 OR [::1] - will not work here. Use your local ip address
                        socket = IO.socket("http://" + url); //http://YourLocalIPAddress:8000
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                    //Calls the socket on function which looks for the update event being emitted from the server and receives the messages
                    socket.on("update", onNewMessage); //This occurs each time a message is sent from the server. Calls onNewMessage() method.
                    socket.connect();   //Connects to the server


                }
                else
                    link.setVisibility(View.VISIBLE);
                return false;
            }
        });

        //Initializing timer
        Timer timer = new Timer();//Timer initialization

        //Updating the time
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Calendar c = Calendar.getInstance();
                //System.out.println("Current time => "+c.getTime());

                SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                String formattedDate = df.format(c.getTime());
                time.setText(formattedDate);//.toString());


            }
        }, 0, 100);

        //Odometer timer
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ss = s;
                dist = dist + ss/3600;
                if(dist > 1)
                {
                    distance.setText("Distance: "+String.format("%.1f", dist) + " kms");//Show distance upto 1 decimal place in km
                }
                else if (dist <= 1)
                {
                    distance.setText("Distance: "+String.format("%.1f", dist) + " km");//Show distance upto 1 decimal place in km
                }


            }
        } , delay, period);


        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (leftFlag.equals("ON")) {
                            if ((left.getDrawable().getConstantState()).equals(getResources().getDrawable(R.drawable.turnleftoff).getConstantState()))
                                left.setImageResource(R.drawable.turnleft);

                            else
                                left.setImageResource(R.drawable.turnleftoff);
                        }

                        if (leftFlag.equals("OFF")) {
                            left.setImageResource(R.drawable.turnleftoff);
                        }


                        if(rightFlag.equals("ON")) {
                            if ((right.getDrawable().getConstantState()).equals(getResources().getDrawable(R.drawable.turnrightoff).getConstantState()))
                                right.setImageResource(R.drawable.turnright);

                            else
                                right.setImageResource(R.drawable.turnrightoff);
                        }

                        if (rightFlag.equals("OFF")) {
                            right.setImageResource(R.drawable.turnrightoff);
                        }

                    }
                });

            }
        } , delay, period);

    }

    /*Calculating the Average of total battery left and then also setting the range
     * by assuming that on 100%  avg. battery left the car can travel 400kms*/

    private  void avgBattery(float batt1, float batt2, float batt3, float batt4)
    {
        avgBat = (batt1 + batt2 + batt3 + batt4)/4;
        battery.setProgress((int) avgBat);//setting battery to sensor's avg battery value using formula
        batterytv.setText(((int) avgBat) + "%");
        rangeLeft.setText("Range: " + String.format("%.1f", (400 * (avgBat/100))) + " kms");

    }

    //Setting the status of HUD Icons using simulator data
    private void lightState(String status, String itemName)
    {
        if (itemName == "LowBeam")
        {
            if (status.equals("ON"))
            {
                lowBeam.setImageResource(R.drawable.lowbeams);
            }
            else
                lowBeam.setImageResource(R.drawable.lowbeamsoff);
        }

        if (itemName == "HighBeam")
        {
            if (status.equals("ON"))
            {
                highBeam.setImageResource(R.drawable.highbeams);
            }
            else
                highBeam.setImageResource(R.drawable.highbeamsoff);
        }

        if (itemName == "HandBrake")
        {
            if (status.equals("ON"))
            {
                handBrake.setImageResource(R.drawable.brakesystemwarning);
            }
            else
                handBrake.setImageResource(R.drawable.brakesystemwarningoff);
        }

        if (itemName == "SeatBelt")
        {
            if (status.equals("ON"))
            {
                seatBelt.setImageResource(R.drawable.seatbelt);
            }
            else
                seatBelt.setImageResource(R.drawable.seatbeltoff);
        }

        if (itemName == "AirBag")
        {
            if (status.equals("ON"))
            {
                airBag.setImageResource(R.drawable.airbag);
            }
            else
                airBag.setImageResource(R.drawable.airbagoff);
        }

        if (itemName == "DoorOpen")
        {
            if (status.equals("ON"))
            {
                doorOpen.setImageResource(R.drawable.dooropen);
            }
            else
                doorOpen.setImageResource(R.drawable.dooropenoff);
        }

        if (itemName == "ABS")
        {
            if (status.equals("ON"))
            {
                abs.setImageResource(R.drawable.abs);
            }
            else
                abs.setImageResource(R.drawable.absoff);
        }

        if (itemName == "Malfunction")
        {
            if (status.equals("ON"))
            {
                malfunction.setImageResource(R.drawable.engine);
            }
            else
                malfunction.setImageResource(R.drawable.engineoff);
        }

        if (itemName == "Malfunction")
        {
            if (status.equals("ON"))
            {
                malfunction.setImageResource(R.drawable.engine);
            }
            else
                malfunction.setImageResource(R.drawable.engineoff);
        }
    }




    //Reads the JSON object that is being sent from the server and changes the TextView value.
    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    String Sensor;
                    String SensorValue;

                    //Checks the sensor coming in and sets the sensor value to the correct TextView
                    try {

                        JSONObject data = (JSONObject) args[0];

                        Sensor = data.get("tag").toString();
                        SensorValue = data.getString("value");

                        switch (Sensor) {
                            case "speed":
                                speed.setText(SensorValue);
                                s = Integer.parseInt(SensorValue);
                                break;
                            case "ambient_temp":
                                temp.setText(SensorValue + "°c");
                                break;
                            case "signal_Left":
                                leftFlag = SensorValue;
                                break;
                            case "signal_Right":
                                rightFlag = SensorValue;
                                break;
                            case "signal_LowBeam":
                                lightState(SensorValue, "LowBeam");
//                                Toast.makeText(getApplicationContext(), "Low beam: " + SensorValue, Toast.LENGTH_LONG).show();
                                break;
                            case "signal_HighBeam":
                                lightState(SensorValue, "HighBeam");
//                                Toast.makeText(getApplicationContext(), "High beam: " + SensorValue, Toast.LENGTH_LONG).show();
                                break;
                            case "signal_Hazard":
                                leftFlag = SensorValue;
                                rightFlag = SensorValue;
                                break;
                            case "warning_Handbrake":
                                lightState(SensorValue, "HandBrake");
                                break;
                            case "warning_Seatbelt":
                                lightState(SensorValue, "SeatBelt");
                                break;
                            case "warning_Airbag":
                                lightState(SensorValue, "AirBag");
//                                Toast.makeText(getApplicationContext(), "Airbag: " + SensorValue, Toast.LENGTH_LONG).show();
                                break;
                            case "warning_Door":
                                lightState(SensorValue, "DoorOpen");
                                break;
                            case "warning_ABS":
                                lightState(SensorValue, "ABS");
                                break;
                            case "warning_Engine":
                                lightState(SensorValue, "Malfunction");
                                break;
                            case "batt_cellA":
                                bat1 = Float.parseFloat(SensorValue);
                                avgBattery(bat1, bat2, bat3, bat4);
                                break;
                            case "batt_cellB":
                                bat2 = Float.parseFloat(SensorValue);
                                avgBattery(bat1, bat2, bat3, bat4);
                                break;
                            case "batt_cellC":
                                bat3 = Float.parseFloat(SensorValue);
                                avgBattery(bat1, bat2, bat3, bat4);
                                break;
                            case "batt_cellD":
                                bat4 = Float.parseFloat(SensorValue);
                                avgBattery(bat1, bat2, bat3, bat4);
                                break;
                            //T3 2019
                            case "motor_temp":
                                motorTempView.setText(SensorValue + " °c");
                                //set temperature bar
                                motortempbar.setProgress((int) Float.parseFloat(SensorValue));
                                break;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

}
