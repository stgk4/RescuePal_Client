package com.srini.searchnrescueproject;

public class GlobalClass {

	public static final String buttonText_one	= "Start Searching";
	public static final String buttonText_two	= "Searching...Click to Stop";

	public static final String STATE0 = "S0:AppOFF/DutyCycleOFF:";
	public static final String STATE1 = "S1:AppON/DutyCycleOFF:";
	public static final String STATE2 = "S2:AppON/DutyCycleON:";
	public static String Phone_IMEI = null;
	public static String filename = null;

	/*
	 * Default Sample Rate Selection Values
	 */
	public static long Report_Samplerate = 10;//Default Sample rate(10-0,50-1,100-2,150-3,200-4,...450-9,500-10)
	public static int spinner_SampleRate_Selected_ID = 0;

	public static double Frequency_Detection_SamplingRate = 0.2;
	public static int spinner_FreqDetSamRate_Selected_ID = 0;

	public static double Min_Frequency = 100;
	public static int spinner_MinFrequency_Selected_ID = 0;

	public static double MaxFrequency = 500;
	public static int spinner_MaxFrequency_Selected_ID = 1;

	public static double PowerIndB = -90;
	public static int spinner_PowerIndB_Selected_ID = 0;

	public static final double AmpRef = 1000.0;

	public static boolean isAppRunning = false;
	public static boolean reportGeneration = false;
	public static boolean FrequencyTrigger = false;
	public static double RescuerDetectedAtFrequency = 0;
	public static double RescuerDetectedFrequencyPower = 0;

	public static String Email_Address = null;

	public static int ExperimentType = -1;
	public static long Preset_Experiment_Duration = -1;
	public static long Experiment_StopTime = -1;

	public static double entered_DutyCycle = 0;
	public static double entered_tON = 0;
	public static double calculated_tOFF = 0;

	public static String connected_Wifi_SSID = null;

	public static String Target_SSID = "";//"Galaxy_S_III_2542";
	public static String Target_PassKey = "";//"vpmr0149";


	public static long DutyCycle_Counter = 0;
	public static long DutyCycle_tON = 0;
	public static long DutyCycle_tOFF = 0;
}
