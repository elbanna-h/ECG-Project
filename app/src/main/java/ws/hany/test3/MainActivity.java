package ws.hany.test3;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {

    private final FirebaseDatabase db = FirebaseDatabase.getInstance();
    private final DatabaseReference root = db.getReference().child("ECG");

    public final static String RN_42_MAC = "00:06:66:18:9D:FE";
    public final static int ENABLE_BT_REQUEST = 1;
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    GraphView graph;
    private LineGraphSeries<DataPoint> mSeries;
    DBH dbH;

    int counter = 0;
    String values;
    int wait = 0;
    double BPM;

    int flag = 1;

    String TAG = "[Main-Thread]";

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mBluetoothSocket;
    BluetoothDevice mBluetoothDevice;
    RNThread mRNThread = null;
    TextView bpmText;
    public Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "Creating listeners"); // step 1

        graph = findViewById(R.id.graph);
        bpmText= findViewById(R.id.bpm);
        Button start = findViewById(R.id.start);
        Button save_local = findViewById(R.id.save_local);
        Button local_history = findViewById(R.id.local_history);
        Button save_cloud = findViewById(R.id.save_cloud);
        Button cloud_history = findViewById(R.id.cloud_history);



        graph.getViewport().setScrollableY(true);
        graph.getViewport().setScalable(true);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);


        dbH = new DBH(this);

        start.setOnClickListener(v -> {
            Log.i(TAG, "Send Data"); // step 8
            if (mBluetoothSocket.isConnected() && mRNThread != null) {

                wait = 0;
                values = "";
                counter = 0;

                String sendCommand = "s";
                mRNThread.write(sendCommand.getBytes());

                flag =0;
                Toast.makeText(MainActivity.this, "Taking ECG...", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(MainActivity.this, "Something Wrong", Toast.LENGTH_LONG).show();
            }
        });

        save_local.setOnClickListener(v -> {
            if (flag ==2){
                @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                dbH.insertECG(values, Double.toString(BPM), dateFormat.format(date));
                Toast.makeText(MainActivity.this, "Graph Successfully Saved", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(MainActivity.this, "Nothing to Save", Toast.LENGTH_SHORT).show();
            }
        });

        local_history.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, History.class);
            startActivity(i);
        });

        save_cloud.setOnClickListener(v -> {
            if (flag ==2){
                @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();

                HashMap<String, String> ECGMap = new HashMap<>();
                ECGMap.put("date", dateFormat.format(date));
                ECGMap.put("values", values);
                ECGMap.put("BPM", Double.toString(BPM));

                root.setValue(ECGMap).addOnCompleteListener(task -> Toast.makeText(MainActivity.this, "Graph Successfully Saved On Cloud", Toast.LENGTH_SHORT).show());

            }else{
                Toast.makeText(MainActivity.this, "Nothing to Save on cloud", Toast.LENGTH_SHORT).show();
            }
        });

        cloud_history.setOnClickListener(v -> root.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                values = snapshot.child("values").getValue(String.class);
                BPM = Double.parseDouble(Objects.requireNonNull(snapshot.child("BPM").getValue(String.class)));

                mSeries.resetData(generateData());
                bpmText.setText( snapshot.child("BPM").getValue(String.class) );
                calcDanger(BPM);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // ask for enable Bluetooth
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, ENABLE_BT_REQUEST);
        }else{
            startBluetooth();
        }


        mSeries = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(1200);
        graph.getViewport().setXAxisBoundsManual(true);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == ENABLE_BT_REQUEST){
            startBluetooth();
        }
    }

    public void startBluetooth(){

        if(mBluetoothAdapter.isEnabled()){

            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(RN_42_MAC);

            //create socket
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
                mBluetoothSocket.connect();
                Log.i(TAG,"Connected to: "+mBluetoothDevice.getName()); // step 2
            }catch(IOException e){
                try{mBluetoothSocket.close();}catch(IOException c){return;}
            }

            Log.i(TAG, "Creating handler"); // step 3
            //create Handler
            mHandler = new Handler(Looper.getMainLooper()){
                @SuppressLint("SetTextI18n")
                @Override
                public void handleMessage(Message message) {
                    if(message.what == RNThread.MESSAGE_CODE){
                        String messageText = (String)message.obj;

                        wait++;
                        if (wait > 5 && flag ==0) {
                            if (counter < 1200) {

                                if (counter == 0) {
                                    values = messageText;
                                } else {
                                    values += messageText;
                                }

                                values += " ";

                                    counter += 20;

                            }else {

                                flag =2;

                                mSeries.resetData(generateData());
                                bpmText.setText( messageText );
                                BPM = Double.parseDouble(messageText);
                                calcDanger(BPM);

                                Log.d("Debug", "BPM=" + BPM);
                            }
                        }

                    }
                }
            };

            Log.i(TAG, "Thread Starting ..."); // step 4
            mRNThread = new RNThread(mBluetoothSocket, mHandler);
            mRNThread.start(); // start new Tread
        }
    }



    private void calcDanger(double bpm){
        double min = 60;
        double max = 100;
        if (bpm < min){
            Toast.makeText(this, "Your heart rate is below normal", Toast.LENGTH_SHORT).show();
        }else if(bpm > max){
            Toast.makeText(this, "Your heart rate is above normal", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Your heart rate is normal", Toast.LENGTH_SHORT).show();
        }
    }



    private DataPoint[] generateData() {
        String[] parts=values.split(" ");
        //Log.d("Debug", "parts=" + values);
        int count = parts.length;
        DataPoint[] dataValues = new DataPoint[count];
        for (int x=0; x<count; x++) {
            double y= parseInt(parts[x]);
            DataPoint v = new DataPoint(x, y);
            dataValues[x] = v;
        }
        return dataValues;
    }

}