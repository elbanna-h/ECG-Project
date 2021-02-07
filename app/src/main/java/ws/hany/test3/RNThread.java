package ws.hany.test3;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class RNThread extends Thread { // java.lang.Thread

    private final InputStream mInputStream;
    private final OutputStream mOutputStream;
    public static final int MESSAGE_CODE = 1;
    Handler mHandler;

    String TAG = "[Back-Thread]";

    public RNThread(BluetoothSocket bluetoothSocket, Handler handler){
        InputStream inputStream = null;
        OutputStream outputStream = null;
        mHandler = handler;
        Log.i(TAG,"Creating thread"); // step 5

        try{
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();

        } catch(IOException e) {
            Log.e(TAG,"Error:"+ e.getMessage());
        }

        mInputStream = inputStream;
        mOutputStream = outputStream;

        try {
            mOutputStream.flush();
        } catch (IOException e) {
            return;
        }
        Log.i(TAG,"I-O initialized"); // step 6
    }

    @Override
    public void run(){
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mInputStream));
        Log.i(TAG,"Starting thread"); // step 7
        while(true){
            try{
                String reading = bufferedReader.readLine();
                //Transfer  data to UI thread by UI Handler
                Message message = new Message();
                message.what = MESSAGE_CODE; // to identify what this message is about
                message.obj = reading;
                mHandler.sendMessage(message);
            }catch(IOException e){
                break;
            }
        }
        Log.i(TAG,"Infinite loop ended"); // step 10
    }

    public void write(byte[] bytes){
        try{
            Log.i(TAG, "Writing bytes"); // step 9
            mOutputStream.write(bytes);

        }catch(IOException ignored){}
    }
}
