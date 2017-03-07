package com.example.mohit.bluetoothapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    public AudioManager audioManager;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile    boolean stopWorker = false;
    public MainActivity() {
        //
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void VolumeUp(View view) {
        Context context = getApplicationContext();
        CharSequence temp = "Volume up";
        Toast toast = Toast.makeText(context, temp, Toast.LENGTH_SHORT);
        toast.show();
        audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);


//        audioManager.dispatchMediaKeyEvent(new KeyEvent());
//        Intent intent = new Intent("com.android.music.musicservicecommand");
//        intent.putExtra("command", "Pause");
//        sendBroadcast(intent);

    }


    public void VolumeDown(View view) {
        audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
    }

    public void Play(View view) {

        audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY);
        audioManager.dispatchMediaKeyEvent(event);

        KeyEvent event2 = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY);
        audioManager.dispatchMediaKeyEvent(event2);
        //sendMediaButton(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_PLAY);
    }

    public void Pause(View view) {
        audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE);
        audioManager.dispatchMediaKeyEvent(event);

        KeyEvent event2 = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE);
        audioManager.dispatchMediaKeyEvent(event2);

        //sendMediaButton(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_PAUSE);
        }

    public void NextSong(View view) {
        audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
        audioManager.dispatchMediaKeyEvent(event);

        KeyEvent event2 = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT);
        audioManager.dispatchMediaKeyEvent(event2);

        //sendMediaButton(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_PAUSE);
    }

    public void EstablishBtConnect(View view)
    {
        try
        {
            findBT();
            openBT();
        }
        catch (IOException ex) { }
    }

    public void CloseBtConnect(View view) throws IOException {
        closeBT();
    }
    public void PreviousSong(View view) {
        audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        audioManager.dispatchMediaKeyEvent(event);

        KeyEvent event2 = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        audioManager.dispatchMediaKeyEvent(event2);

        //sendMediaButton(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_PAUSE);
    }


    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {

            Context context = getApplicationContext();
            CharSequence temp = "No bluetooth adapter available";
            Toast toast = Toast.makeText(context, temp, Toast.LENGTH_SHORT);
            toast.show();

        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-06"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        Context context = getApplicationContext();
        CharSequence temp = "Bluetooth Device Found";
        Toast toast = Toast.makeText(context, temp, Toast.LENGTH_SHORT);
        toast.show();

    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        Context context = getApplicationContext();
        CharSequence temp = "Bluetooth Device Opened";
        Toast toast = Toast.makeText(context, temp, Toast.LENGTH_SHORT);
        toast.show();
     //   myLabel.setText("Bluetooth Opened");
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
//                                    Context context = getApplicationContext();
//                                    CharSequence temp = data;
//                                    Toast toast = Toast.makeText(context, temp, Toast.LENGTH_SHORT);
//                                    toast.show();
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            if(data.trim().equals("1"))
                                            {
                                                NextSong();
                                            }
                                            else if(data.trim().equals("-1"))
                                            {
                                                PreviousSong();
                                            }
                                            else if(data.trim().equals("2"))
                                            {
                                                IncreaseVolume();
                                            }
                                            else if(data.trim().equals("-2"))
                                            {
                                                DecreaseVolume();
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public void NextSong()
    {
        audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
        audioManager.dispatchMediaKeyEvent(event);

        KeyEvent event2 = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT);
        audioManager.dispatchMediaKeyEvent(event2);
    }
    public void PreviousSong()
    {
        audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        audioManager.dispatchMediaKeyEvent(event);

        KeyEvent event2 = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        audioManager.dispatchMediaKeyEvent(event2);
    }

    public void DecreaseVolume()
    {
        audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
    }

    public void IncreaseVolume()
    {
        audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
    }
    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Context context = getApplicationContext();
        CharSequence temp = "Bluetooth Closed";
        Toast toast = Toast.makeText(context, temp, Toast.LENGTH_SHORT);
        toast.show();

    }
}