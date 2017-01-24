package com.srini.searchnrescueproject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends Activity implements OnItemSelectedListener, OnCheckedChangeListener{
	
	private Spinner sp_forSamplerate = null;
	//private EditText et_EmailId = null;
	private EditText et_SSID = null;
	//private EditText et_ExpTime = null;
	private EditText et_PassKey = null;
	private Switch sw_forReportGeneration = null;
	
	private Spinner sp_forFrequencyDetectionSampling = null;
	private Spinner sp_forMinFrequency = null;
	private Spinner sp_forMaxFrequency = null;
	private Spinner sp_forPowerIndB = null;
	
	private double local_MinFreq = 0;
	private int local_Spinner_MinSel = 0;
	private double local_MaxFreq = 0;
	private int local_Spinner_MaxSel = 0;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		sw_forReportGeneration = (Switch)findViewById(R.id.switch_ReportGeneration);
		sp_forSamplerate = (Spinner)findViewById(R.id.spinner_SampleRate);
		//et_EmailId = (EditText)findViewById(R.id.editText_EmailId);
		
		sp_forFrequencyDetectionSampling = (Spinner)findViewById(R.id.spinner_FrequencyDetectionSampling);
		sp_forFrequencyDetectionSampling.setEnabled(false);
		sp_forMinFrequency = (Spinner)findViewById(R.id.spinner_MinFrequency);
		sp_forMaxFrequency = (Spinner)findViewById(R.id.spinner_MaxFrequency);
		sp_forPowerIndB = (Spinner)findViewById(R.id.spinner_PowerIndB);
		
		et_SSID = (EditText)findViewById(R.id.et_SSID);
		et_PassKey = (EditText)findViewById(R.id.et_Passkey);
		//et_ExpTime = (EditText)findViewById(R.id.editText_ExpTime);
		
		et_SSID.setText(GlobalClass.Target_SSID);
		et_PassKey.setText(GlobalClass.Target_PassKey);
		
		/*
		 * Spinner For ReportSamplingFrequencies
		 */
		ArrayAdapter<CharSequence> adapter_ReportSampling = ArrayAdapter.createFromResource(this,
				R.array.stringArray_samplerate, android.R.layout.simple_spinner_item);
		adapter_ReportSampling.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_forSamplerate.setAdapter(adapter_ReportSampling);
		sp_forSamplerate.setOnItemSelectedListener(this);
		sp_forSamplerate.setSelection(GlobalClass.spinner_SampleRate_Selected_ID);
		
		/*
		 * Spinner For FrequencyDetection_SamplingFrequencies
		 */
		ArrayAdapter<CharSequence> adapter_FrequencyDetection = ArrayAdapter.createFromResource(this,
				R.array.stringArray_FrequencyDetectionSamplerate, android.R.layout.simple_spinner_item);
		adapter_FrequencyDetection.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_forFrequencyDetectionSampling.setAdapter(adapter_FrequencyDetection);
		sp_forFrequencyDetectionSampling.setOnItemSelectedListener(this);
		sp_forFrequencyDetectionSampling.setSelection(GlobalClass.spinner_FreqDetSamRate_Selected_ID);
		
		/*
		 * Spinner For MinFrequency Selection
		 */
		ArrayAdapter<CharSequence> adapter_MinFrequency = ArrayAdapter.createFromResource(this,
				R.array.stringArray_Frequencies, android.R.layout.simple_spinner_item);
		adapter_MinFrequency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_forMinFrequency.setAdapter(adapter_MinFrequency);
		sp_forMinFrequency.setOnItemSelectedListener(this);
		sp_forMinFrequency.setSelection(GlobalClass.spinner_MinFrequency_Selected_ID);
		
		/*
		 * Spinner For MaxFrequency Selection
		 */
		ArrayAdapter<CharSequence> adapter_MaxFrequency = ArrayAdapter.createFromResource(this,
				R.array.stringArray_Frequencies, android.R.layout.simple_spinner_item);
		adapter_MaxFrequency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_forMaxFrequency.setAdapter(adapter_MaxFrequency);
		sp_forMaxFrequency.setOnItemSelectedListener(this);
		sp_forMaxFrequency.setSelection(GlobalClass.spinner_MaxFrequency_Selected_ID);
		
		/*
		 * Spinner For PowerIndB
		 */
		ArrayAdapter<CharSequence> adapter_PowerIndB = ArrayAdapter.createFromResource(this,
				R.array.stringArray_PowerIndB, android.R.layout.simple_spinner_item);
		adapter_PowerIndB.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_forPowerIndB.setAdapter(adapter_PowerIndB);
		sp_forPowerIndB.setOnItemSelectedListener(this);
		sp_forPowerIndB.setSelection(GlobalClass.spinner_PowerIndB_Selected_ID);
		
		
		sw_forReportGeneration.setChecked(GlobalClass.reportGeneration);
		sw_forReportGeneration.setOnCheckedChangeListener(this);
		
		local_MinFreq = GlobalClass.Min_Frequency;
		local_Spinner_MinSel = GlobalClass.spinner_MinFrequency_Selected_ID;
		
		local_MaxFreq = GlobalClass.MaxFrequency;
		local_Spinner_MaxSel = GlobalClass.spinner_MaxFrequency_Selected_ID;
	}

	
	public void doneButtonClicked(View v){
		if(local_MaxFreq <= local_MinFreq){
			Toast.makeText(this, "MinFrequency must be less than MaxFrequency", Toast.LENGTH_LONG).show();
		}
		else{
			GlobalClass.Target_SSID	= et_SSID.getText().toString();
			GlobalClass.Target_PassKey = et_PassKey.getText().toString();
			/*GlobalClass.Email_Address = et_EmailId.getText().toString();
			if(!et_ExpTime.getText().toString().equals("")){
				GlobalClass.Preset_Experiment_Duration =(long)(1000 * Double.parseDouble(et_ExpTime.getText().toString()));
			}*/
			
			
			GlobalClass.Min_Frequency = local_MinFreq;
			GlobalClass.spinner_MinFrequency_Selected_ID = local_Spinner_MinSel;
			
			GlobalClass.MaxFrequency = local_MaxFreq;
			GlobalClass.spinner_MaxFrequency_Selected_ID = local_Spinner_MaxSel;
			this.finish();
		}
	}

	
	/*
	 * For Spinners
	 */

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		
		switch (arg0.getId()) {
		case R.id.spinner_SampleRate:
			GlobalClass.Report_Samplerate = Long.parseLong(arg0.getItemAtPosition(arg2).toString().replaceAll("[^0-9]", ""));
			GlobalClass.spinner_SampleRate_Selected_ID = arg2;
			Log.i("Report_Samplerate:", GlobalClass.Report_Samplerate+"");
			break;
		
		case R.id.spinner_FrequencyDetectionSampling:
			String s_DetFreq = arg0.getItemAtPosition(arg2).toString();
			double d_DetFreq = Long.parseLong(s_DetFreq.replaceAll("[^0-9]", ""));
			if(s_DetFreq.contains(".")){
				d_DetFreq = d_DetFreq/10;
			}
			GlobalClass.Frequency_Detection_SamplingRate = d_DetFreq;
			GlobalClass.spinner_FreqDetSamRate_Selected_ID = arg2;
			Log.i("Frequency_Detection_SamplingRate:", GlobalClass.Frequency_Detection_SamplingRate+"");
			break;
			
		case R.id.spinner_MinFrequency:
			String s_Min = arg0.getItemAtPosition(arg2).toString();
			double d_Min = Long.parseLong(s_Min.replaceAll("[^0-9]", ""));
			if(s_Min.contains("KHz")){
				d_Min = d_Min*1000;
				d_Min = d_Min/10; // This is for extra zero parsed after "."
			}
			local_MinFreq = d_Min;
			local_Spinner_MinSel = arg2;
			break;
			
		case R.id.spinner_MaxFrequency:
			String s_Max = arg0.getItemAtPosition(arg2).toString();
			double d_Max = Long.parseLong(s_Max.replaceAll("[^0-9]", ""));
			if(s_Max.contains("KHz")){
				d_Max = d_Max*1000;
				d_Max = d_Max/10; // This is for extra zero parsed after "."
			}
			local_MaxFreq = d_Max;
			local_Spinner_MaxSel = arg2;
			break;
			
		case R.id.spinner_PowerIndB:
			GlobalClass.PowerIndB = (-1) * Long.parseLong(arg0.getItemAtPosition(arg2).toString().replaceAll("[^0-9]", ""));
			GlobalClass.spinner_PowerIndB_Selected_ID = arg2;
			Log.i("PowerIndB:", GlobalClass.PowerIndB+"");
			break;
		

		default:
			break;
		}
	}


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}

	
	
	/*
	 * For Switch
	 */

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		if(isChecked){
			Log.i("onCheckedChanged", "ONNNNNNNNNNNNNNNNN");
			GlobalClass.reportGeneration = true;
		}
		else{
			Log.i("onCheckedChanged", "OFFFFFFFFFFFFFFFFF");
			GlobalClass.reportGeneration = false;
		}
	}
}
