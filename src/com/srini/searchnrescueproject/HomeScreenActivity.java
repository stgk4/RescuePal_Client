package com.srini.searchnrescueproject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class HomeScreenActivity extends Activity implements OnItemSelectedListener {

	private EditText et_DutyCycle = null;
	private EditText et_tON = null;
	private EditText et_Comment = null;
	private TextView tv_display_tOFF = null;
	private Button button_Start = null;
	private Spinner sp_forExpType = null;

	private FileWriter fw = null;
	public File folder = null;

	private long StartTime = 0;
	private long StopTime = 0;

	public WifiConfiguration wifiConfiguration = null;

	public WifiManager wifiManager = null;

	public ScheduledExecutorService scheduleTaskExecutor = null;

	public Intent batteryStatus = null;



	ImageView imageView;
	Bitmap bitmap;
	Canvas canvas;
	Paint paint;

	/*
	 * Audio Stuff(non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */

	int audioSource = MediaRecorder.AudioSource.MIC;    // Audio source is the device MIC
	int channelConfig = AudioFormat.CHANNEL_IN_MONO;    // Recording in mono
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; // Records in 16bit
	private RealDoubleFFT transformer;
	int blockSize = 1024;                               // deal with this many samples at a time
	int sampleRate = 44100;                             // Sample rate in Hz
	public double frequency = 0.0;                      // the frequency given

	private TextView tv_forFrequency = null;
	RecordAudio recordTask;                             // Creates a Record Audio command
	Thread threadForFrequencySampling = null;


	private class RecordAudio extends AsyncTask<Void, double[], Void>{

		@Override
		protected Void doInBackground(Void... params){      

			/*Calculates the fft and frequency of the input*/
			//try{
			int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding);                // Gets the minimum buffer needed
			AudioRecord audioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioEncoding, (bufferSize*1));   // The RAW PCM sample recording



			short[] buffer = new short[blockSize];          // Save the raw PCM samples as short bytes

			// double[] audioDataDoubles = new double[(blockSize*2)]; // Same values as above, as doubles
			//   ----------------------------------------------- 

			//   ----------------------------------------------------
			double[] toTransform = new double[blockSize];

			// fft = new DoubleFFT_1D(blockSize);


			try{
				audioRecord.startRecording();  //Start
			}catch(Throwable t){
				Log.e("AudioRecord", "Recording Failed");
			}

			while(!GlobalClass.FrequencyTrigger && !recordTask.isCancelled()){
				/* Reads the data from the microphone. it takes in data 
				 * to the size of the window "blockSize". The data is then
				 * given in to audioRecord. The int returned is the number
				 * of bytes that were read*/

				int bufferReadResult = audioRecord.read(buffer, 0, blockSize);
				Log.i("bufferReadResult:", bufferReadResult+"");

				// Read in the data from the mic to the array
				for(int i = 0; i < blockSize && i < bufferReadResult; i++) {

					/* dividing the short by 32768.0 gives us the 
					 * result in a range -1.0 to 1.0.
					 * Data for the compextForward is given back 
					 * as two numbers in sequence. Therefore audioDataDoubles
					 * needs to be twice as large*/

					// audioDataDoubles[2*i] = (double) buffer[i]/32768.0; // signed 16 bit
					//audioDataDoubles[(2*i)+1] = 0.0;
					toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
				}

				//audiodataDoubles now holds data to work with
				// fft.complexForward(audioDataDoubles);
				transformer.ft(toTransform);
				//System.out.println(Arrays.toString(toTransform));
				publishProgress(toTransform);

				//waitForFrequencyInterval();
			}
			try{
				audioRecord.stop();
				audioRecord.release();
			}
			catch(IllegalStateException e){
				Log.e("Stop failed", e.toString());

			}

			//    } 
			return null;
		}

		protected void onProgressUpdate(double[]... toTransform){

			double[] re = new double[blockSize];
			double[] im = new double[blockSize];
			double[] magnitude = new double[blockSize];  

			// Calculate the Real and imaginary and Magnitude.
			for(int i = 0; i < blockSize/2; i++){
				// real is stored in first part of array
				re[i] = toTransform[0][i*2];
				// imaginary is stored in the sequential part
				im[i] = toTransform[0][(i*2)+1];
				// magnitude is calculated by the square root of (imaginary^2 + real^2)
				magnitude[i] = Math.sqrt((re[i] * re[i]) + (im[i]*im[i]));
			}

			double peak = -1.0;
			// Get the largest magnitude peak
			for(int i = 0; i < blockSize; i++){
				if(peak < magnitude[i])
					peak = magnitude[i];
			}
			// calculated the frequency
			frequency = (sampleRate * peak)/blockSize;
			Log.i("Frequency:"+frequency, "Power:" + 20.0 * Math.log10(peak/GlobalClass.AmpRef)+"");

			if(frequency >= GlobalClass.Min_Frequency && 
					frequency <= GlobalClass.MaxFrequency && 
					(20.0 * Math.log10(peak/GlobalClass.AmpRef)) >= GlobalClass.PowerIndB){
				GlobalClass.FrequencyTrigger = true;
				GlobalClass.RescuerDetectedAtFrequency = frequency;
				GlobalClass.RescuerDetectedFrequencyPower = (20.0 * Math.log10(peak/GlobalClass.AmpRef));

				Toast.makeText(HomeScreenActivity.this, "Rescuer Detected at Frequency: " + 
						GlobalClass.RescuerDetectedAtFrequency + "Hz", Toast.LENGTH_LONG).show();
				tv_forFrequency.setText("Rescuer Detected at Frequency: " + GlobalClass.RescuerDetectedAtFrequency + 
						"Hz Power:" + GlobalClass.RescuerDetectedFrequencyPower + "dB" );

				//Start Wifi DutyCycle
				System.out.println("Starting Wifi Duty Cycling");
				DutyCycle();
			}
			
			canvas.drawColor(Color.BLACK);
			 
            for (int i = 0; i < toTransform[0].length; i++) {
                int x = i;
                int downy = (int) (100 - (toTransform[0][i] * 10));
                int upy = 100;
 
                canvas.drawLine(x, downy, x, upy, paint);
            }
 
            imageView.invalidate();
		}

	}


	public void waitForFrequencyInterval(){
		try {
			Thread.sleep((long)(1000.0/GlobalClass.Frequency_Detection_SamplingRate));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_screen);

		et_DutyCycle = (EditText)findViewById(R.id.editText_DutyCycle);
		et_tON = (EditText)findViewById(R.id.editText_tON);
		et_Comment = (EditText)findViewById(R.id.editText_Comment);
		tv_display_tOFF = (TextView)findViewById(R.id.textView_tOFF);
		sp_forExpType = (Spinner)findViewById(R.id.spinner_forExpType);
		button_Start = (Button)findViewById(R.id.button_Start);
		button_Start.setText(GlobalClass.buttonText_one);


		/*
		 * Audio Initializations
		 */
		tv_forFrequency = (TextView)findViewById(R.id.textView_Frequency);
		transformer = new RealDoubleFFT(blockSize);
		tv_forFrequency.setText("Hello");

		ArrayAdapter<CharSequence> adapter_cars = ArrayAdapter.createFromResource(this,
				R.array.stringArray_expType, android.R.layout.simple_spinner_item);
		adapter_cars.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_forExpType.setAdapter(adapter_cars);
		sp_forExpType.setOnItemSelectedListener(this);

		/*
		 * Getting Phone IMEI number
		 */
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		GlobalClass.Phone_IMEI = tm.getDeviceId();

		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		batteryStatus = this.registerReceiver(null, ifilter);



		/*
		 * Wifi Configuration
		 */
		wifiConfiguration = new WifiConfiguration();



		/*
		 * Creating a file folder for reports
		 */
		folder = new File(Environment.getExternalStorageDirectory()	+ "/VictimTracer_Reports");

		boolean var = false;
		if (!folder.exists()){
			var = folder.mkdir();
			Log.i("folder.mkdir()_returntype" , var+"");
		}


		Log.i(GlobalClass.STATE0, "AppStatus: " + GlobalClass.isAppRunning + "\tDutyCycleStatus: " + GlobalClass.FrequencyTrigger);


		imageView = (ImageView) this.findViewById(R.id.imageView_Graph);
		bitmap = Bitmap.createBitmap((int) 256, (int) 100,
				Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		paint = new Paint();
		paint.setColor(Color.GREEN);
		imageView.setImageBitmap(bitmap);
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		//Log.i("onDestroy", "Destroy method Called0000000000000000000000");
		if(GlobalClass.reportGeneration){
			commitFile();
		}
	}

	/*	public void getInfoClickied(View v){
		commitFile();
		android.os.Process.killProcess(android.os.Process.myPid());
	}*/

	public void commitFile(){
		//tv_timeStamp.setText(System.currentTimeMillis()+"");
		//Log.i("commitedFile", "FileDoneeeeeeeeee1111111111111");
		try {
			fw.close();
			sp_forExpType.setSelection(0);
			GlobalClass.ExperimentType = -1;
			GlobalClass.Experiment_StopTime = -1;
			GlobalClass.Preset_Experiment_Duration = -1;
			button_Start.setEnabled(true);
			Toast.makeText(this, "Report Saved Successfully...", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.home_screen, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub


		switch (item.getItemId()) {
		case R.id.action_settings:

			Intent goToSettings  = new Intent(this, SettingsActivity.class);
			startActivity(goToSettings);

			break;

		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	/*
	 * Create Report File
	 */
	public void createReportFile(){
		/*
		 * File Creation for report
		 */
		GlobalClass.filename = 
				folder.toString() + "/" + GlobalClass.Phone_IMEI + "_"+ System.currentTimeMillis() + "_" + (GlobalClass.ExperimentType+1) + ".csv";
		try {
			fw = new FileWriter(GlobalClass.filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void writeARow(String row){
		try {
			/*
			 * Line_Row
			 */
			fw.append(row);
			fw.append("\n");
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
		}
	}

	public void writeSummary(){

		writeARow(getResources().getString(R.string.Report_Star_Seperator));
		writeARow("Total Recording Time : " + (StopTime-StartTime) + " Milliseconds" + " (" + (double)(StopTime-StartTime)/1000  + " Seconds)");
		writeARow(getResources().getString(R.string.Report_Star_Seperator));
	}

	public void writeInitialFileHeadersToFile(){

		writeARow(getResources().getString(R.string.Report_Star_Seperator));

		/*
		 * Start of META-DATA
		 */

		//Title 
		writeARow("* "+getResources().getString(R.string.Report_Title));


		//Line1
		writeARow("* Report Generated on " + java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()));

		//Line2
		//writeARow("* Preset Experiment Duration  : " + GlobalClass.Preset_Experiment_Duration + "milliSeconds  (Negative for preSet duration DISABLED)");

		//Line3
		//writeARow("* Email For which Report is Sent : Option DISABLED"  );

		//Line4
		writeARow("* TargetSSID : " + GlobalClass.Target_SSID);

		//Line5
		writeARow("* PassKey : " + GlobalClass.Target_PassKey);

		//Line6
		writeARow("* Duty Cycle : " + GlobalClass.entered_DutyCycle);

		//Line7
		writeARow("* T-ON : " + GlobalClass.DutyCycle_tON + " milliSeconds");

		//Line8
		writeARow("* T-OFF : " + GlobalClass.DutyCycle_tOFF + " milliSeconds");

		//Line9 
		writeARow("* Report SamplingRate : " + GlobalClass.Report_Samplerate + " Samples per Second");

		//Line10
		writeARow("* IMEI of the Phone Used : " + GlobalClass.Phone_IMEI);

		//Line11
		//writeARow("* Distance : " + et_Comment.getText().toString() + "  (Phase-1:This field is used to log distance From the HotSpot)");

		//Line12
		writeARow("* FrequencyRangeSet : " + GlobalClass.Min_Frequency + "Hz to " + GlobalClass.MaxFrequency + "Hz" );

		//Line 13
		writeARow("* LookupSignalPowerSet : " + GlobalClass.PowerIndB + "dB");

		//Line 14
		writeARow("* FreqLookupSamplingrate : " + GlobalClass.Frequency_Detection_SamplingRate + " timesLookup per Second");

		//Line15		 
		writeARow("* RescuerDetectedFreq : " + 	GlobalClass.RescuerDetectedAtFrequency + "Hz");

		/*
		 * End of META-DATA
		 */
		writeARow(getResources().getString(R.string.Report_Star_Seperator));

		writeARow("TimeStamp" +
				"," + "T-ON/T-OFF" +
				"," + "Con-Status" +
				"," + "RSSI" +
				"," + "Conneted Phone" );
		writeARow(getResources().getString(R.string.Report_Star_Seperator));
	}

	/************************************************************************
	 * Method Used For Wifi Connectivity
	 * Step1: Preparing Wifi Configuration with the Target SSID and Passkey
	 * Step2: Registering the Wifi Manager
	 * Step3: Adding Configutation to the Wifimanager and Enabling it
	 * 
	 ************************************************************************/
	public void wifiConnectMethod_basic(String ssid, String passkey){
		/*
		 * Step 1
		 */
		wifiConfiguration.SSID = "\"" + ssid + "\"";
		wifiConfiguration.preSharedKey = "\"" + passkey + "\"";

		/*
		 * Step 2
		 */
		wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
		/*
		 * Checking for wifi Enabled or not and Enabling it if not
		 */
		if(!wifiManager.isWifiEnabled()){
			//Log.i("wifiManager.isWifiEnabled()", wifiManager.isWifiEnabled() + "");
			wifiManager.setWifiEnabled(true);
		}

		/*
		 * Step3
		 */
		int res = wifiManager.addNetwork(wifiConfiguration);
		wifiManager.enableNetwork(res, true);

		/*List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
		for( WifiConfiguration i : list ) {
			if(i.SSID != null && i.SSID.equals("\"" + "TinyBox" + "\"")) {
				try {
					wifiManager.disconnect();
					wifiManager.enableNetwork(i.networkId, true);
					System.out.print("i.networkId " + i.networkId + "\n");
					wifiManager.reconnect();               
					break;
				}
				catch (Exception e) {
					e.printStackTrace();
				}

			}           
		}*/
	}

	/**
	 * Wifi Connectivity Method which checks for the security type
	 * of the scanned networks and connects accordingly
	 * @param ssid - Target SSID of the Hotspot network
	 * @param passkey - Passkey of the network
	 */
	public void connectToAP(String ssid, String passkey) {
		//Log.i("connectToAP", "* connectToAP");

		String networkSSID = ssid;
		String networkPass = passkey;

		Log.d("connectToAP", "# password " + networkPass);

		List<ScanResult> scanResultList = wifiManager.getScanResults();

		for (ScanResult result : scanResultList) {
			if (result.SSID.equals(networkSSID)) {
				Log.i("target_SSID_Found", result.SSID);

				String securityMode = getScanResultSecurity(result);

				if (securityMode.equalsIgnoreCase("OPEN")) {

					Log.i("Connect to AP********", "Case-OPEN");

					wifiConfiguration.SSID = "\"" + networkSSID + "\"";
					wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
					int res = wifiManager.addNetwork(wifiConfiguration);
					Log.d("connectToAP", "# add Network returned " + res);

					boolean b = wifiManager.enableNetwork(res, true);
					Log.d("connectToAP", "# enableNetwork returned " + b);

					wifiManager.setWifiEnabled(true);

				} else if (securityMode.equalsIgnoreCase("WEP")) {

					Log.i("Connect to AP********", "Case-WEP");

					wifiConfiguration.SSID = "\"" + networkSSID + "\"";
					wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\"";
					wifiConfiguration.wepTxKeyIndex = 0;
					wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
					wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
					int res = wifiManager.addNetwork(wifiConfiguration);
					Log.d("connectToAP", "### 1 ### add Network returned " + res);

					boolean b = wifiManager.enableNetwork(res, true);
					Log.d("connectToAP", "# enableNetwork returned " + b);

					wifiManager.setWifiEnabled(true);
				}
				else{

					/*
					 * 
					 * 
					 * If looking for adding to the list, a network which is the target SSID that is found in scanned results 
					 * 
					 * 
					 * 
					 * Preparing This target SSID found and adding it to the wifi Manager
					 */

					Log.i("Connect to AP********", "Case-OTHER SECURITY mode");

					wifiConfiguration.SSID = "\"" + networkSSID + "\"";
					wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
					wifiConfiguration.hiddenSSID = true;
					wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
					wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
					wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
					wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
					wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
					wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
					wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
					wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

					int res = wifiManager.addNetwork(wifiConfiguration);
					Log.d("connectToAP", "### 2 ### add Network returned " + res);

					/*
					 * Enabling the target SSID wifi and disabling all others
					 */
					wifiManager.enableNetwork(res, true);

					boolean changeHappen = wifiManager.saveConfiguration();

					if(res != -1 && changeHappen){
						Log.d("connectToAP", "### Change happen");

						GlobalClass.connected_Wifi_SSID = networkSSID;

					}else{
						Log.d("connectToAP", "*** Change NOT happen");
					}

					wifiManager.setWifiEnabled(true);
				}
			}
			//When other than target SSID is encountered then the below else block logs all such reachable SSID's
			else{
				Log.i("Other_SSID", result.SSID);
			}
		}

	}

	public double getPrecisionAfterDecimalByTrimming(double value, int preciscionAfterPoint){
		return (double)((long)(Math.floor(value * Math.pow(10, preciscionAfterPoint))))/1000;		
	}

	public long secondsToMilliseconds(double timeInSec){
		return (long)(timeInSec*1000);
	}
	public String getScanResultSecurity(ScanResult scanResult) {
		Log.d("connectToAP", "* getScanResultSecurity");

		final String cap = scanResult.capabilities;
		final String[] securityModes = { "WEP", "PSK", "EAP" };

		for (int i = securityModes.length - 1; i >= 0; i--) {
			if (cap.contains(securityModes[i])) {
				return securityModes[i];
			}
		}

		return "OPEN";
	}

	public void startButtonClicked(View v){

		/*
		 * Changing Text from "Searching..." to "Start"
		 */
		if(button_Start.getText().toString().equals(GlobalClass.buttonText_two)){

			GlobalClass.isAppRunning = false;

			if(!GlobalClass.FrequencyTrigger){
				recordTask.cancel(true);
			}
			else if(GlobalClass.FrequencyTrigger){
				scheduleTaskExecutor.shutdown();
				StopTime = System.currentTimeMillis();

				if(GlobalClass.reportGeneration){
					/*
					 * Write summary and close the app
					 */
					writeSummary();
					commitFile();
				}
				wifiManager.setWifiEnabled(false);
			}
			else{
				Log.i("ButtonClickEvent:", "FrequencyTriggerNotFALSE/NotTRUE");
			}

			button_Start.setText(GlobalClass.buttonText_one);
			et_DutyCycle.setEnabled(true);
			et_tON.setEnabled(true);
			et_Comment.setEnabled(true);
			sp_forExpType.setEnabled(true);

			et_DutyCycle.getText().clear();
			et_tON.getText().clear();
			et_Comment.getText().clear();
			sp_forExpType.setSelection(0);
			tv_display_tOFF.setText("");	
			tv_forFrequency.setText("");
			GlobalClass.FrequencyTrigger = false;
		} 

		/*
		 * Changing Text from "Start" to "Searching..."
		 */
		else if(button_Start.getText().toString().equals(GlobalClass.buttonText_one)){
			if(et_DutyCycle.getText().toString().equals("") || et_tON.getText().toString().equals("")){
				Toast.makeText(this, "Enter all the details", Toast.LENGTH_LONG).show();
			}
			else{
				GlobalClass.entered_DutyCycle = Double.parseDouble(et_DutyCycle.getText().toString());
				GlobalClass.entered_tON = Double.parseDouble(et_tON.getText().toString());

				if(GlobalClass.entered_DutyCycle < 0 || GlobalClass.entered_DutyCycle > 1){
					Toast.makeText(this, "Duty Cycle must be between '0' and '1'", Toast.LENGTH_LONG).show();
				}
				else{
					GlobalClass.isAppRunning = true;	
					tv_forFrequency.setText("Looking for FREQ: " +
							GlobalClass.Min_Frequency + "Hz to " + GlobalClass.MaxFrequency + 
							"Hz and Power:"+GlobalClass.PowerIndB + "dB");
					/*
					 * Preparing DutyCycle Values
					 */
					GlobalClass.calculated_tOFF = (GlobalClass.entered_tON / GlobalClass.entered_DutyCycle) - GlobalClass.entered_tON;//In seconds
					GlobalClass.DutyCycle_tON = secondsToMilliseconds(GlobalClass.entered_tON);//In milliSeconds
					GlobalClass.DutyCycle_tOFF = secondsToMilliseconds(getPrecisionAfterDecimalByTrimming(GlobalClass.calculated_tOFF, 3));//In milliSeconds

					/*
					 *UI Display
					 */
					button_Start.setText(GlobalClass.buttonText_two);

					if(GlobalClass.Preset_Experiment_Duration != -1){
						button_Start.setEnabled(false);
						GlobalClass.Experiment_StopTime = StartTime + GlobalClass.Preset_Experiment_Duration;
					}
					et_DutyCycle.setEnabled(false);
					et_tON.setEnabled(false);
					et_Comment.setEnabled(false);
					sp_forExpType.setEnabled(false);
					tv_display_tOFF.setText("T-OFF:" + GlobalClass.DutyCycle_tOFF + "ms  T-ON:" + GlobalClass.DutyCycle_tON + "ms");

					/*
					 * Starting Frequency Searching AsyncTask
					 */
					recordTask = new RecordAudio();
					recordTask.execute();
				}
			}	
		}

		else{
			Log.i("startButtonClicked", "Error occured while setting button text");
		}
	}


	/**********************************************************************************
	 * Method for running duty cycle as per t-on and t-off
	 **********************************************************************************/
	public void DutyCycle(){
		/*
		 * Stopping AsyncTask for FrequencySearch
		 */
		recordTask.cancel(true);
		/*
		 * Writing duty cycle data
		 */
		StartTime = System.currentTimeMillis();
		if(GlobalClass.reportGeneration){

			//Creating a File in the app folder	 
			createReportFile();

			//write header Metadata info to csv file 
			writeInitialFileHeadersToFile();
		}

		scheduleTaskExecutor = Executors.newScheduledThreadPool(5);

		Log.i("GlobalTon globalToff", GlobalClass.DutyCycle_tON + "," + GlobalClass.DutyCycle_tOFF);

		// This schedule a runnable task every sampling interval set
		scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
			long tON_Counter = 0; 
			long tOFF_Counter = 0;
			int DutyCycle = 0;

			public void run() {

				//Log.i("ScheduledExecutorService", "*"+i);
				//writeARow("*"+i);
				//GlobalClass.DutyCycle_Counter++;
				if(tON_Counter < GlobalClass.DutyCycle_tON){
					if(tON_Counter == 0){
						wifiConnectMethod_basic(GlobalClass.Target_SSID, GlobalClass.Target_PassKey);
					}
					tON_Counter = tON_Counter + (1000/GlobalClass.Report_Samplerate);
					DutyCycle = 1;
				}
				else{
					if(tOFF_Counter < GlobalClass.DutyCycle_tOFF){
						wifiManager.setWifiEnabled(false);
						tOFF_Counter = tOFF_Counter + (1000/GlobalClass.Report_Samplerate);
						DutyCycle = 0;
					}
					else{
						tOFF_Counter = 0;
						tON_Counter = 0;
						DutyCycle = 1;
					}		
				}
				/*Log.i("DATA", System.currentTimeMillis() + "," 
						+ DutyCycle + ","
						+ wifiManager.getConnectionInfo().getNetworkId() + "," 
						+ wifiManager.getConnectionInfo().getRssi() + ","
						+ wifiManager.getConnectionInfo().getSSID());*/

				if(GlobalClass.isAppRunning && GlobalClass.reportGeneration){
					writeARow(System.currentTimeMillis() + "," 
							+ DutyCycle + ","
							+ wifiManager.getConnectionInfo().getNetworkId() + "," 
							+ wifiManager.getConnectionInfo().getRssi() + ","
							+ wifiManager.getConnectionInfo().getSSID());
				}
				else{
					//Log.i("AppIsClosed", "Scheduler task came inside the block even after the app is closed");
				}
			}
		}, 0, 1000/GlobalClass.Report_Samplerate , TimeUnit.MILLISECONDS);
	}


	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		GlobalClass.ExperimentType = arg2;
	}


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}
}
