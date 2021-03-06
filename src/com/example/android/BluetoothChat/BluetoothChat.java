/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.example.android.BluetoothChat.GalleryActivity.ImageAdapter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mTitle;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    
    private Button mSendFButton;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    private byte[] F_HEADER = new byte[3];  
    private byte[] F_SIZE = new byte[3];
    
    //hardcoded path to image file
    private String file_name = "/mnt/sdcard/GIF/b6.bmp";
    
    //gallery shit
	LinearLayout mLinearLayout;
	int pos_temp = 0;

    private Context mContext;

    private Integer[] mImageIds = {
//            R.drawable.b1,
//            R.drawable.b2,
//            R.drawable.b3,
//            R.drawable.b4,
//            R.drawable.b5,
//            R.drawable.b6,
//    		R.drawable.b1_large,
//    		R.drawable.stripes,
    		R.drawable.b2small,
    		R.drawable.b3small,
    		R.drawable.bls,
    		R.drawable.s1,
    		R.drawable.s2
    };
    
    
    //thread shit and progress bar shit
    
     int max = 10000000;
	 int count = 0;
	 ProgressDialog dialog;
	 int increment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
//
//        
//        dialog = new ProgressDialog(this);
//        dialog.setCancelable(true);
//        dialog.setMessage("Sending...");
//       //  set the progress to be horizontal
//        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        // reset the bar to the default value of 0
//        dialog.setProgress(0);
// 
//        dialog.setMax(max);
//        // display the progressbar
//        dialog.show();
//        
        Gallery g = (Gallery) findViewById(R.id.gallery);
        g.setAdapter(new ImageAdapter(this));

        g.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
    
                ImageButton image = (ImageButton) findViewById(R.id.image_button);
                image.setImageResource(mImageIds[position]);
                selectImagePath(position);
//            	
////            	Context c = getApplicationContext();
////                dialog = new ProgressDialog(c);
////                dialog.setCancelable(true);
////                dialog.setMessage("Sending...");
////               //  set the progress to be horizontal
////                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
////                // reset the bar to the default value of 0
////                dialog.setProgress(0);
////         
////                dialog.setMax(max);
////                // display the progressbar
////                dialog.show();
                
            }
        });
        
        
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }
    
    private void sendFile(String filename)
    {
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

    	DataInputStream dis;
    	File imagefile = new File(filename);
    	FileOutputStream fOut = null;
    	OutputStreamWriter osw = null;
 
    	dis = null;
    	int marker = 0;
    	int filesize = (int) imagefile.length();
    	

    	try {
			dis = new DataInputStream(new FileInputStream(imagefile));
			dis.skipBytes(54);
			filesize = (filesize - 54);
		}
		catch(FileNotFoundException e) {
			dis = null;
			System.exit(-1);
		}
        catch (IOException e)
        {
        	System.exit(-1);
        }

        byte tp;
//
//		byte[] imag = new byte[packetsize];
        
        byte[] img = new byte[filesize];
        
        int cur_byte=0;

//        
//        int row = 1;
//        marker = 272*3+1;
//        int bpline = 272*3+1;
//      
        
        int x = 272;
        int y = 204;

//        for (int i = 0; i < y; i++)
//        {
//        	marker = (x*3*(i+1))-1;
//        	for (int j = 0; j < x*3; j++)
//        	{
//        		try {
//					tp = dis.readByte();
//	        		img[marker] = tp;
//	        		marker--;
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//					System.exit(0);
//				}
//        	}
//        }
//        
        int bytesSent = 0;
        for (int i = 0; i < y; i++)
        {
        	marker = (x*3*(i+1))-1;
        	for (int j = 0; j < x*3; j=j+3)
        	{
        		try {
        			for (int k = 2; k >= 0; k--)
        			{
        				tp = dis.readByte();
						img[marker-k] = tp;
						bytesSent++;
					}

					marker = marker - 3 ;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.exit(-1);
				}
        	}
        }
        
//		for (int i = 0; i < filesize; i++)
//		{
//			
//			try {
//				tp = dis.readByte();	
//				img[marker] = tp;
//				Log.e(TAG,Integer.toString(marker));
//				marker++;
//				//marker--;
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				System.exit(0);
//			} 
//			catch (ArrayIndexOutOfBoundsException a )
//			{
//				Log.e(TAG, Integer.toString(marker));
//				System.exit(0);
//			}
//			
//		    if (marker % bpline == 0)
//		    {
//		    	row = row + 1;
//		    	marker = marker + (bpline-1) * row;
//		    }	
//		}
		
		if (img.length > 0) {
        //send file
        mChatService.write(img);

        Log.d(TAG, "6");
        // Reset out string buffer to zero and clear the edit text field
//        mOutStringBuffer.setLength(0);
//        mOutEditText.setText(mOutStringBuffer);
    }
		Toast.makeText(this, Integer.toString(bytesSent), Toast.LENGTH_SHORT).show();
		Log.e(TAG, Integer.toString(bytesSent));
		img = null;
//    	while(filesize > 0)
//    	{
//    		marker = 0;
//    		if (filesize >= packetsize)
//    		{
//    			
//    			for (int i = 0; i < packetsize; i++)
//    			{
//    				try {
//						tp = dis.readByte();
//						imag[marker] = tp;
//						marker++;
//	    				filesize--;	
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//						System.exit(0);
//					}
//
//    			}
//
//    	        mChatService.write(imag);
//
//        	    Log.d(TAG, "2");
//        	    
//    	            // Reset out string buffer to zero and clear the edit text field
//    	            mOutStringBuffer.setLength(0);
//    	            mOutEditText.setText(mOutStringBuffer);
//    		}
//    		else if (filesize >= 0 && filesize < packetsize) 
//    		{
//    	        Log.d(TAG, "4");
//
//    			byte[] imags = new byte[filesize];
//    			int count = filesize;
//    			for (int i = 0; i < count; i++)
//    			{
//    				try {
//						tp = dis.readByte();
//
//						imags[marker] = tp;
//						marker++;
//	    				filesize--;	
//	    				
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//						System.exit(0);
//					}
//
//	    	        Log.d(TAG, "5");	
//    			}
//    			if (imags.length > 0) {
//    	            //send file
//    	            mChatService.write(imags);
//
//        	        Log.d(TAG, "6");
//    	            // Reset out string buffer to zero and clear the edit text field
//    	            mOutStringBuffer.setLength(0);
//    	            mOutEditText.setText(mOutStringBuffer);
//    	        }
//
//    	        Log.d(TAG, "7");
//    		}
//    	}

        Log.d(TAG, "8");
//		
//    	byte[] img = new byte[3+4+filesize+500];
//    	byte tmp;
//    	
//    	img[marker] = 'a';
//    	marker++;
//    	img[marker] = 'b';
//    	marker++;
//    	img[marker] = 'c';
//    	marker++;
//    	
//    	img[marker] = (byte) (filesize >> 24);
//    	marker++;
//    	img[marker] = (byte) (filesize >> 16);
//    	marker++;
//    	img[marker] = (byte) (filesize >> 8);
//    	marker++;
//    	img[marker] = (byte) (filesize >> 0);
//    	marker++;
//    	
//    	try {
//			while(marker < (7+filesize+500)) {
//				if (marker == 6+filesize+500)
//				{
//					tmp = dis.readByte();
//					img[marker] = tmp;
//					img[marker] = 'B';
//				}
//				else
//					img[marker] = 'A';
//				marker++;
//			}
//		}
//		catch(EOFException e) {}
//		catch(IOException e) {}

		try {
			dis.close();
		}
		catch(IOException e) {}
    

//        // Check that there's actually something to send
//        if (img.length > 0) {
//            //send file
//            mChatService.write(img);
//
//            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
//        }
    }
    
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
//        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
//        mConversationView = (ListView) findViewById(R.id.in);
//        mConversationView.setAdapter(mConversationArrayAdapter);
//
//         //Initialize the compose field with a listener for the return key
//        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
//        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
//        mSendButton = (Button) findViewById(R.id.button_send);
//        // Initialize send file button
//        mSendFButton = (Button) findViewById(R.id.button_send_image);
//		// init image button
        ImageButton image = (ImageButton) findViewById(R.id.image_button);
//        
//        mSendButton.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                // Send a message using content of the edit text widget
//                TextView view = (TextView) findViewById(R.id.edit_text_out);
//                String message = view.getText().toString();
//                sendMessage(message);
//            }
//        });
//
//        mSendFButton.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//            	
////            	Context c = getApplicationContext();
////                dialog = new ProgressDialog(c);
////                dialog.setCancelable(true);
////                dialog.setMessage("Sending...");
////               //  set the progress to be horizontal
////                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
////                // reset the bar to the default value of 0
////                dialog.setProgress(0);
////         
////                dialog.setMax(max);
////                // display the progressbar
////                dialog.show();
//                
////            	SendImageTask s = new SendImageTask();
////            	int a = s.doInBackground("");
////            	s.onPostExecute(count);
////            	
////                // create a thread for updating the progress bar
////                Thread background = new Thread (new Runnable() {
////                   public void run() {
////                       try {
////                           // enter the code to be run while displaying the progressbar.
////                           //
////                           // This example is just going to increment the progress bar:
////                           // So keep running until the progress value reaches maximum value
////                           while (dialog.getProgress()<= dialog.getMax()) {
////                               // wait 500ms between each update
////                               Thread.sleep(50);
////         
////                               // active the update handler
////                               progressHandler.sendMessage(progressHandler.obtainMessage());
////                           }
////                       } catch (java.lang.InterruptedException e) {
////                           // if something fails do something smart
////                    	   System.exit(1);
////                       }
////                   }
////                });
////         
////                // start the background thread
////                background.start();
//            }
////            
////            Handler progressHandler = new Handler() {
////                public void handleMessage(Message msg) {
////                    dialog.setProgress(count);
////                }
////            };
//        });
//        
        image.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                //String message = view.getText().toString();
                //sendMessage(message);
                sendFile(file_name);
                //toast file size
                //message = Long.toString(sendFile(file_name));
                //Context context = getApplicationContext();
                
                //CharSequence text = message;
                //int duration = Toast.LENGTH_LONG;
                //Toast toast = Toast.makeText(context, text, duration);
                //toast.show();
            }
        });
        

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        //mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void selectImagePath(int img)
    {
    	switch (img)
    	{
//    	case 0:
//    		file_name = "/mnt/sdcard/GIF/b1.bmp";
//    		break;
//    	case 1:
//    		file_name = "/mnt/sdcard/GIF/b2.bmp";
//    		break;
//    	case 2:
//    		file_name = "/mnt/sdcard/GIF/b3.bmp";
//    		break;
//    	case 3:
//    		file_name = "/mnt/sdcard/GIF/b4.bmp";
//    		break;
//    	case 4:
//    		file_name = "/mnt/sdcard/GIF/b5.bmp";
//    		break;
//    	case 5:
//    		file_name = "/mnt/sdcard/GIF/b6.bmp";
//    		break;
//    	case 6:
//    		file_name = "/mnt/sdcard/GIF/b1_large.bmp";
//    		break;
//    	case 7:
//    		file_name = "mnt/sdcard/GIF/stripes.bmp";
//    		break;
    	case 0:
    		file_name = "mnt/sdcard/GIF/b2small.bmp";
    		break;
    	case 1:
    		file_name = "mnt/sdcard/GIF/b3small.bmp";
    		break;
    	case 2:
    		file_name = "mnt/sdcard/GIF/bls.bmp";
    		break;
    	case 3:
    		file_name = "mnt/sdcard/GIF/s1.bmp";
    		break;
    	case 4:
    		file_name = "mnt/sdcard/GIF/s2.bmp";
    		break;
    	default:
    		Toast.makeText(this, "File does not exist", Toast.LENGTH_SHORT).show();
    		break;
    	}
    }
    
    /**
     * Sends a message.
     * @param message  A string of text to send.
//     */
//    private void sendMessage(String message) {
//        // Check that we're actually connected before trying anything
//        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
//            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Check that there's actually something to send
//        if (message.length() > 0) {
//            // Get the message bytes and tell the BluetoothChatService to write
//            byte[] send = message.getBytes();
//            mChatService.write(send);
//
//            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
//        }
//    }
    
//    // The action listener for the EditText widget, to listen for the return key
//    private TextView.OnEditorActionListener mWriteListener =
//        new TextView.OnEditorActionListener() {
//        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
//            // If the action is a key-up event on the return key, send the message
//            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
//                String message = view.getText().toString();
//                sendMessage(message);
//            }
//            if(D) Log.i(TAG, "END onEditorAction");
//            return true;
//        }
//    };

    
    
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }
    
    
//    private class SendImageTask


    private class SendImageTask extends AsyncTask<String, Void, Integer> {
    	 protected Integer doInBackground(String... urls) {
             for (int i = 0;i < 1000000; i++)
            	 count++;
             return count;
         }

         protected void onPostExecute(int count) {

        	 Context c = getApplicationContext();
             Toast.makeText(c, Integer.toString(count), Toast.LENGTH_LONG).show();
         }
     }
    
    
    public class ImageAdapter extends BaseAdapter {
        int mGalleryItemBackground;


        public ImageAdapter(Context c) {
            mContext = c;
            TypedArray a = c.obtainStyledAttributes(R.styleable.Gallery1);
            mGalleryItemBackground = a.getResourceId(R.styleable.Gallery1_android_galleryItemBackground, 0);
            a.recycle();
        }
        
        public int getCount() {
            return mImageIds.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(mContext);

            i.setImageResource(mImageIds[position]);
            i.setLayoutParams(new Gallery.LayoutParams(120, 100));
            i.setScaleType(ImageView.ScaleType.FIT_XY);
            i.setBackgroundResource(mGalleryItemBackground);

            return i;
        }
    }

}