package com.sentinel.android.checkservice;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;
import com.sentinel.android.services.check.ICheckService;

import android.content.Intent;
import android.os.Process;
import android.content.ComponentName;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import java.lang.String;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.content.Context;

import android.content.pm.PackageManager;
import java.util.List;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.content.pm.ProviderInfo;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.DefaultPhoneNotifier;
import android.telephony.TelephonyManager;
import android.os.Binder;
import android.app.PendingIntent;

import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.os.Bundle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.app.Activity;
import java.lang.Byte;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.OutputStream;
import android.os.Environment;

import android.provider.MediaStore;
import android.bluetooth.BluetoothAdapter; 
import android.app.admin.DevicePolicyManager;

import java.io.InputStream;
import java.io.FileNotFoundException;

import android.app.NotificationManager;
import android.app.Notification.Builder;
import android.app.Notification;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.text.DateFormat;

class ICheckServiceImpl extends ICheckService.Stub {
  private static final String TAG = "ICheckServiceImpl";
  private final Context context;

  ICheckServiceImpl(Context context) {
	this.context = context;
  }

  public void flushLog() throws RemoteException {
	if (this.context.checkCallingOrSelfPermission(Manifest.permission.CHECK_UID) != 
	    PackageManager.PERMISSION_GRANTED) {
      throw new SecurityException("Requires CHECK_UID permission");
    }
    Log.d(TAG, "Flushing Logs");
  }

  /**
   * Returns true if the Uid of thecalling process belongs to the monitoring list.
   * Return false if the Uid not on the monitoring list.
   */
  public boolean compareUid(int callingUid) {

	String result;
	//mock up for monitoring list - need to change
	PackageInfo pInfo = null;
	try {
		pInfo = context.getPackageManager().getPackageInfo("com.example.retrievephoneinfoapplication", 0);
	} catch (NameNotFoundException e) {
		e.printStackTrace();
	}
	int checkUid = pInfo.applicationInfo.uid;
	
	//displayNotice(String.format("The calling uid is %d and the checking uid is %d", callingUid, checkUid));	

	if (callingUid == checkUid)        
		return true;
	else
		return false;
  }

  /**
   * Display the message in an activity.
   * 
   */
  public void displayNotice(String msg) {

	String result;
	// send and intent to activity
        Intent intent = new Intent(context, NoticeActivity.class);
	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String pkg = "com.sentinel.android.checkservice";
        String cls = "com.sentinel.android.checkservice.NoticeActivity";
        intent.setComponent(new ComponentName(pkg, cls));
	result = String.format(msg);
	intent.putExtra("result", result);
        context.startActivity(intent);
  }

  /**
   * Send an intent that tells the app that the system
   * is trying to intialize the call. 
   * 
   * @param the UID of the the app we want to notice
   * 
   * @param the intent to extract the data of the calling phone number
   */
  public void sendFakeOutgoingCallIntent(int appUid, Intent callIntent) {

	// get the phone number of the outgoing call
        Uri handle = callIntent.getData();
        String scheme = handle.getScheme();
        String uriString = handle.getSchemeSpecificPart();

	// make a forged an intent to send to the blacklisted app
        Intent broadcastIntent = new Intent(Intent.ACTION_NEW_OUTGOING_CALL);
        if (uriString != null) {
            broadcastIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, uriString);
        }	

        // Force receivers of this broadcast intent to run at foreground priority because we
        // want to finish processing the broadcast intent as soon as possible.
        broadcastIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		
	// send the fake intent to every app in the blacklist
	PackageManager mPackageManager = context.getPackageManager();
	String[] pkgs = mPackageManager.getPackagesForUid(appUid); 
	for(int i=0;i<pkgs.length;i++){
		broadcastIntent.setPackage(pkgs[i]);
		context.sendBroadcast(broadcastIntent, android.Manifest.permission.PROCESS_OUTGOING_CALLS);
	}
	//displayNotice(String.format("Trying to call " + uriString));
  }

  /**
   * Block the components of a blacklisted app that associate
   * with the given intent.
   * 
   * @param the intent that we don't want the blacklisted components to know
   */
  public void blockIntentReceiver(Intent callIntent) {

	String s = "The list of components associate with intent:";
	PackageManager manager = context.getPackageManager();

	// list all the activites that associate with the intent
	List<ResolveInfo> infosActivities = manager.queryIntentActivities (callIntent,
                                   PackageManager.GET_RESOLVED_FILTER);
	for (ResolveInfo infoActivities : infosActivities) {
        	ActivityInfo activityInfo = infoActivities.activityInfo;
        	IntentFilter filter = infoActivities.filter;
        	if (filter != null && filter.hasAction(callIntent.getAction())) {
        	    // This activity resolves my Intent with the filter I'm looking for
        	    String activityPackageName = activityInfo.packageName;
        	    String activityName = activityInfo.name;
        	    s = s.concat("\n Activity "+ activityPackageName + "/" + activityName);
        	}
    	}

	// list all the services that associate with the intent
	List<ResolveInfo> infosServices = manager.queryIntentServices (callIntent,
                                   PackageManager.GET_RESOLVED_FILTER);
	for (ResolveInfo infoServices : infosServices) {
        	ServiceInfo serviceInfo = infoServices.serviceInfo;
        	IntentFilter filter = infoServices.filter;
        	if (filter != null && filter.hasAction(callIntent.getAction())) {
        	    // This service resolves my Intent with the filter I'm looking for
        	    String servicePackageName = serviceInfo.packageName;
        	    String serviceName = serviceInfo.name;
        	    s = s.concat("\n Service "+ servicePackageName + "/" + serviceName);
        	}
    	}

	// list all the content providers that associate with the intent
	List<ResolveInfo> infosProviders = manager.queryIntentContentProviders (callIntent,
                                   PackageManager.GET_RESOLVED_FILTER);
	for (ResolveInfo infoProviders : infosProviders) {
        	ProviderInfo providerInfo = infoProviders.providerInfo;
        	IntentFilter filter = infoProviders.filter;
        	if (filter != null && filter.hasAction(callIntent.getAction())) {
        	    // This service resolves my Intent with the filter I'm looking for
        	    String providerPackageName = providerInfo.packageName;
        	    String providerName = providerInfo.name;
        	    s = s.concat("\n Provider "+ providerPackageName + "/" + providerName);
        	}
    	}	

	// list all the broadcast receivers that associate with the intent
	List<ResolveInfo> infosBroadcastReceivers = manager.queryBroadcastReceivers (callIntent,
                                   PackageManager.GET_RESOLVED_FILTER);
	for (ResolveInfo infoBroadcastReceivers : infosBroadcastReceivers) {
        	ActivityInfo broadcastreceiverInfo = infoBroadcastReceivers.activityInfo;
        	IntentFilter filter = infoBroadcastReceivers.filter;
        	if (filter != null && filter.hasAction(callIntent.getAction())) {
        	    // This service resolves my Intent with the filter I'm looking for
        	    String broadcastreceiverPackageName = broadcastreceiverInfo.packageName;
        	    String broadcastreceiverName = broadcastreceiverInfo.name;
        	    s = s.concat("\n Broadcast receiver "+ broadcastreceiverPackageName + "/" + broadcastreceiverName);
        	}
    	}	

	//ComponentName componentName = new ComponentName("com.example.retrievephoneinfoapplication","com.example.retrievephoneinfoapplication.the_receiver");
	//manager.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);	

	displayNotice(String.format(s));
  }

  /**
   * Send fake phone call notification to the destination application.The function
   * sends a seri of notifications that tells the app that there is an ongoing/missed
   * phone call, while it does not happen.
   * 
   * @param The name of the package that we want to notify
   * 
   * @param The type of call we want to notify (incoming/outgoing) 
   * 
   * @param The phone number that associate with the fake call
   * 
   * @param The time duration of the fake call (leave as 0 for missed call)
   * measure in seconds
   */
  public void sendFakeCallNotification(String packageName, String callType, String phoneNumber, int duration) {
	
	Thread thread;
	final int mDuration = duration;

	// if the callType is unknown, we will stop executing
	if (!callType.equals("fake_incoming_call") && !callType.equals("fake_outgoing_call") )
		return;

	// Set up the intent for NEW_OUTGOING_CALL
	final Intent outgoingIntent = new Intent(Intent.ACTION_NEW_OUTGOING_CALL);
	outgoingIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, phoneNumber);
	outgoingIntent.setPackage(packageName);

	// Set up the intent for CALL_STATE_IDLE
	final Intent idleIntent = new Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
	idleIntent.putExtra(PhoneConstants.STATE_KEY, DefaultPhoneNotifier.convertCallState(TelephonyManager.CALL_STATE_IDLE).toString());
	idleIntent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, phoneNumber);
	idleIntent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
	idleIntent.setPackage(packageName);		

	// Set up the intent for CALL_STATE_RINGING
	final Intent ringingIntent = new Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
	ringingIntent.putExtra(PhoneConstants.STATE_KEY, DefaultPhoneNotifier.convertCallState(TelephonyManager.CALL_STATE_RINGING).toString());
	ringingIntent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, phoneNumber);
	ringingIntent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
	ringingIntent.setPackage(packageName);	
	
	// Set up the intent for CALL_STATE_OFFHOOK
	final Intent offhookIntent = new Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
	offhookIntent.putExtra(PhoneConstants.STATE_KEY, DefaultPhoneNotifier.convertCallState(TelephonyManager.CALL_STATE_OFFHOOK).toString());
	offhookIntent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, phoneNumber);
	offhookIntent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
	offhookIntent.setPackage(packageName);		

	// if it is fake outgoing call process that we want to perform
	if (callType.equals("fake_outgoing_call")) {
		thread = new Thread() {
			@Override
			public void run () {
				try {
					sleep(5000);
					// Starting the sequence by telling the app that the phone is available for calling
					context.sendBroadcast(idleIntent, android.Manifest.permission.READ_PHONE_STATE);
					sleep(3000);
					// The next step is faking that we are processing a outgoing call request
					context.sendBroadcast(outgoingIntent, android.Manifest.permission.PROCESS_OUTGOING_CALLS);
					//Next, if the input duration is 0, we simulate missed outgoing call
					// else, we simulate the connected call, after the duration ends, broadcast end call state
					if (mDuration == 0) {
						context.sendBroadcast(idleIntent, android.Manifest.permission.READ_PHONE_STATE);
					} else {
						context.sendBroadcast(offhookIntent, android.Manifest.permission.READ_PHONE_STATE);
						sleep(mDuration * 1000);
						context.sendBroadcast(idleIntent, android.Manifest.permission.READ_PHONE_STATE);
					}
					
				} catch (InterruptedException e) {
					Log.e("Fake outgoing call: ", "Thread interrupted");
				}
			}
		};
		thread.start();
	} else if (callType.equals("fake_incoming_call")) {
		thread = new Thread() {
			@Override
			public void run () {
				try {
					sleep(5000);
					// Starting the sequence by telling the app that the phone is available for calling
					context.sendBroadcast(idleIntent, android.Manifest.permission.READ_PHONE_STATE);
					sleep(3000);
					// The next step is faking that we are processing a incoming call request
					context.sendBroadcast(ringingIntent, android.Manifest.permission.READ_PHONE_STATE);
					//Next, if the input duration is 0, we simulate missed incoming call
					// else, we simulate the connected call, after the duration ends, broadcasts end call state
					if (mDuration == 0) {
						context.sendBroadcast(idleIntent, android.Manifest.permission.READ_PHONE_STATE);
					} else {
						context.sendBroadcast(offhookIntent, android.Manifest.permission.READ_PHONE_STATE);
						sleep(mDuration * 1000);
						context.sendBroadcast(idleIntent, android.Manifest.permission.READ_PHONE_STATE);
					}
					
				} catch (InterruptedException e) {
					Log.e("Fake outgoing call: ", "Thread interrupted");
				}
			}
		};
		thread.start();
	} 
  }

  /**
   * Get the UID of the calling application
   * 
   */
  public int getCallingAppUid() {

	int uid;
	// get uid
        uid = Binder.getCallingUid();
	return uid;
  }

  /**
   * Check if the intent is for asking to make a phone call
   * 
   * @param the intent that we want to examine
   */
  public boolean isRequestingCall(Intent callIntent) {

	boolean result = false;
	String data = null;

	// get the intent action
	String intentAction = callIntent.getAction();
	// get uri content
	if (callIntent.getData() != null)
        	data = callIntent.getData().toString();

	// if we get nothing then this is not the intent of interest
	if (intentAction == null || data == null)
		return result;

	// if the action is ACTION_DIAL or ACTION_CALL and the uri contains the word "tel"
	// then it is requesting a call
	if ((intentAction.equals(Intent.ACTION_CALL) || intentAction.equals(Intent.ACTION_DIAL)) && (data.startsWith("tel:")))
		result = true;
	displayNotice(String.format(intentAction+data));
	return result;
  }

  /**
   * Check if the intent is for asking to make a phone call
   * 
   * @param the intent that we want to examine
   */
  public boolean isRequestingSendSms(Intent callIntent) {

	boolean result = false;
	String data = null;

	// get the type of the intent
	String intentType = callIntent.getType();
	// get uri content
	if (callIntent.getData() != null)
        	data = callIntent.getData().toString();

	// if we get nothing then this is not the intent of interest
	if (intentType == null || data == null)
		return result;

	// if the type has certain string or the uri contains the word "smsto"
	// then it is requesting a call
	if (intentType.equals("vnd.android-dir/mms-sms") || data.startsWith("smsto:") || data.startsWith("sms:"))
		result = true;
	displayNotice(String.format(intentType+data));
	return result;
  }

  /**
   * Send fake notification to the application that uses the SmsManager 
   * utility to send text message
   * 
   * @param the UID of the application that we want to send fake notification
   */
  public void processSendTextMessageUtility(int Uid, String destAddress, String srcAddress, String text,
		 PendingIntent sentIntent, PendingIntent deliveryIntent) {

	//TODO: save text message to fake database
	boolean result = false;

	//if the caller asks to know if the sms is sent through sentIntent
	if (sentIntent != null)
		try {
		sentIntent.send(Activity.RESULT_OK);
		} catch (PendingIntent.CanceledException ex) {
		}
	//if the caller asks to know if the sms is delivered through deliveryIntent
	if (deliveryIntent != null)
		try {
		deliveryIntent.send(Activity.RESULT_OK);	
		} catch (PendingIntent.CanceledException ex) {
		}	
  }

  /**
   * Determine the purpose of the Intent, if it is to access the built-in apps 
   * or the buit-in provider then acts upon the result.
   * 
   * For example: if the Intent were to use the built-in SMS to send message facility
   * and it comes from a blacklisted app then the Intent will not go through
   * instead a new entry will appears that indicates the request was successful
   *
   * @param the UID of the application that we want to send fake notification
   * @param the Intent comes from that application
   */
  public void processIndirectIntent(int Uid, Intent deliveryIntent, int requestCode) {

	//TODO: save text message to fake database, save fake calllog to fake database

	// get the package name of the calling app
	PackageManager mPackageManager = context.getPackageManager();
	String[] pkgs = mPackageManager.getPackagesForUid(Uid);
	
	// check which category the intent belongs to and if that app has the required permission 
	// if the package does not had the required permission then we will let the built-in
	// facility takes care of the rest
	for(int i=0;i<pkgs.length;i++){
		if (isRequestingCall(deliveryIntent) == true) {
			if (mPackageManager.checkPermission(pkgs[i], android.Manifest.permission.CALL_PHONE) 
								== PackageManager.PERMISSION_DENIED)
				return;				
		// if the app holds the required permission then we proceed to tell that its
		// request is completed while no real action has taken
		} else {
			sendFakeOutgoingCallIntent(Uid, deliveryIntent);
		}
		if (isRequestingSendSms(deliveryIntent) == true) {
			if (mPackageManager.checkPermission(pkgs[i], android.Manifest.permission.SEND_SMS) 
								== PackageManager.PERMISSION_DENIED)
				return;
		} else {
			//TODO: add entry to fake sms log
		}

		// if the intent is for taking photo from camera
		if (isRequestingCameraPhoto(deliveryIntent) == true) {
			// get the fake photo ready
			Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.smile1);
			//Intent returnIntent = new Intent();
			//returnIntent.setPackage(pkgs[i]);
         		//returnIntent.putExtra("data",bm);	
			//Activity con = new Activity();
         		//con.setResult(Activity.RESULT_OK,returnIntent); 
		}
	}

  }

  /**
   * Return an fake Location object to the requesting process.
   * 
   * Instead of letting the real Location Manage retrieve the stored location,
   * we craft a fake one and let the requested process to have it.
   *
   * @param The string that indicates the location provider name
   * @param the Intent comes from that application
   */
  public Location answerWithFakeLocation(String provider) {

	// Setup a location object that contains fake altitude, longtitude
	// and latitude. The time attribute is the time this object got generated.
	// The remaining attributes are also made up so it appears to be similar
	// to the legitimate one
	Location location = new Location(provider);

	//Set the estimated accuracy of this location
	location.setAccuracy(10.0f);

	//Set the altitude, in meters above the WGS 84 reference ellipsoid.
	location.setAltitude(14.0);

	//Set the time of this fix, in elapsed real-time since system boot. 
	location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

	//Sets the extra information associated with this fix to the given Bundle.
	Bundle mBundle = new Bundle();
	mBundle.putInt("satellites", 11);
	location.setExtras(mBundle);

	//Set the latitude, in degrees. 
	location.setLatitude(28.937176);

	//Set the longitude, in degrees. 
	location.setLongitude(-96.204529);

	//Sets the name of the provider
	location.setProvider(provider);

	//Set the UTC time in milliseconds
	location.setTime(System.currentTimeMillis());

	return location;
  }

  /**
   * Check if the intent is for asking to take a photo from
   * 
   * @param the intent that we want to examine
   */
  public boolean isRequestingCameraPhoto(Intent callIntent) {

	boolean result = false;

	// get the action of the intent
	String intentAction = callIntent.getAction();

	// if we get nothing then this is not the intent of interest
	if (intentAction == null)
		return result;

	// if the type has a string like ACTION_IMAGE_CAPTURE or ACTION_IMAGE_CAPTURE_SECURE
	// then it is requesting photo from camera
	if (intentAction.equals(MediaStore.ACTION_IMAGE_CAPTURE) || 
			intentAction.equals(MediaStore.ACTION_IMAGE_CAPTURE_SECURE))
		result = true;

	return result;
  }

  /**
   * Create an intent that contain a fake photo for the requesting app
   * 
   * @param none
   */
  public Intent getFakePhotoByIntent() {

	//get the fake photo ready
	Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.smile1);
	Intent returnIntent = new Intent();
        returnIntent.putExtra("data",bm);	

	return returnIntent;
  }

  /**
   * Make a copy of the captured photo into our own service
   * directory for later inspection
   * 
   * @param the uid of the blacklisted app that used the camera
   * @param the captured image photo
   */
  public void saveCapturedPhotoFromCamera(int uid, byte[] bytes) {

	//Locate the storage place
	String path = context.getExternalFilesDir(null) + "/" + String.valueOf(uid) + "/" + "photo/";
	final File file = new File (path);
	if (!file.exists()){
		file.mkdirs();
	}

	//Make the name for the photo
	String name = generateFileName();
	
	// Save the photo in ".jpg" format
	File saved = new File (path + name + ".jpg"); 
	FileOutputStream output = null;
	try {
		output = new FileOutputStream(saved);
		output.write(bytes);
	} catch (IOException e) {
		e.printStackTrace();	
 	} finally {
		try{
			output.flush();
			output.close();
		} catch (IOException e) {
		}
	}
  }

  /**
   * Give the connected Connection a fake requested InputStream
   * 
   * @param none
   */
  public void saveCapturedInputStream(int uid, byte[] bytes, String extension) {

	//Locate the storage place
	String path = context.getExternalFilesDir(null) + "/" + String.valueOf(uid) + "/" + "internet/";
	final File file = new File (path);
	if (!file.exists()){
		file.mkdirs();
	}
	//Make the name for the file
	String name = generateFileName();
	
	// Save the photo in ".jpg" format
	File saved = new File (path + name + "-input." + extension); 
	FileOutputStream output = null;
	try {
		output = new FileOutputStream(saved);
		output.write(bytes);
	} catch (IOException e) {
		e.printStackTrace();	
 	} finally {
		try{
			output.flush();
			output.close();
		} catch (IOException e) {
		}
	}

  }

  /**
   * Generate the file name to store
   * 
   * @param none
   */
  public String generateFileName() {

	//Make the name for the file
	Date today = Calendar.getInstance().getTime();
	SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-hhmmss");
	String name = formatter.format(today);
	return name;
  }

  /**
   * Get file extension from donwload httpURLConnection 
   * 
   * @param the URL to extract the extension string
   */
  public String extractExtFromURL(String url) {

	//Extract the extension string
	String extension = "";
	
	// locate the extension name
	int i = url.lastIndexOf('.');

	// in case there is "." in the path string 
	int p = Math.max(url.lastIndexOf('/'), url.lastIndexOf('\\'));

	if (i > p) {
		extension = url.substring(i+1);
	}  

	return extension;
  }

  /**
   * Generate the path for public storage 
   * 
   * @param none
   */
  public String generatePublicPath() {

	//Make the name for the path
	String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
	return path;
  }

  /**
   * Check if the intent is for asking to use the BluetoothAdapter
   * 
   * @param the intent that we want to examine
   */
  public boolean isRequestingBluetooth(Intent callIntent) {

	boolean result = false;

	// get the action of the intent
	String intentAction = callIntent.getAction();

	// if we get nothing then this is not the intent of interest
	if (intentAction == null)
		return result;

	// if the type has a string like ACTION_REQUEST_DISCOVERABLE or ACTION_REQUEST_ENABLE
	// then it is requesting bluetooth usage
	if (intentAction.equals(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE) || 
			intentAction.equals(BluetoothAdapter.ACTION_REQUEST_ENABLE))
		result = true;

	return result;
  }

  /**
   * Check if the intent is for asking to use the DevicePolicyManager
   * 
   * @param the intent that we want to examine
   */
  public boolean isRequestingDevicePolicyManager(Intent callIntent) {

	boolean result = false;

	// get the action of the intent
	String intentAction = callIntent.getAction();

	// if we get nothing then this is not the intent of interest
	if (intentAction == null)
		return result;

	// if the type has a string like ACTION_ADD_DEVICE_ADMIN or ACTION_SET_NEW_PASSWORD
	// or ACTION_START_ENCRYPTION then it is requesting bluetooth usage
	if (intentAction.equals(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN) || 
			intentAction.equals(DevicePolicyManager.ACTION_SET_NEW_PASSWORD) ||
			intentAction.equals(DevicePolicyManager.ACTION_START_ENCRYPTION))
		result = true;

	return result;
  }

  /**
   * Convert the URI to an other one that refers to a different
   * content provider
   * 
   * @param the URI that we want to convert
   */
  public Uri convertTheUri(Uri uri) {

	String str = uri.toString();
	String newStr = null;

	if (str.contains("content://com.android.contacts"))
	{
		newStr = str.replace("content://com.android.contacts","content://com.sentinel.android.providers.contacts");
	}
	if (str.contains("content://telephony"))
	{
		newStr = str.replace("content://telephony","content://com.sentinel.android.providers.telephony.telephony");
	}
	if (str.contains("content://sms"))
	{
		newStr = str.replace("content://sms","content://com.sentinel.android.providers.telephony.sms");
	}
	if (str.contains("content://mms"))
	{
		newStr = str.replace("content://mms","content://com.sentinel.android.providers.telephony.mms");
	}
	if (str.contains("content://mms-sms"))
	{
		newStr = str.replace("content://mms-sms","content://com.sentinel.android.providers.telephony.mms-sms");
	}
	if (str.contains("content://hbpcd_lookup"))
	{
		newStr = str.replace("content://hbpcd_lookup","content://com.sentinel.android.providers.telephony.hbpcd_lookup");
	}
	if (str.contains("content://call_log"))
	{
		newStr = str.replace("content://call_log","content://com.sentinel.android.providers.contacts.call_log");
	}

	Uri myUri = Uri.parse(newStr);

	return myUri;
  }

  /**
   * Check and create fake database corresponding to contact and
   * profile data storage
   * 
   * @param none
   */
  public void checkAndCreateFakeContactDatabase() {

	//locate storage directory
	PackageManager m = context.getPackageManager();
	String path = context.getPackageName();
	try {
    		PackageInfo p = m.getPackageInfo(path, 0);
    		path = p.applicationInfo.dataDir;
	} catch (PackageManager.NameNotFoundException e) {
    		Log.w("yourtag", "Error Package name not found ", e);
	}
	final File file = new File (path+"/databases/");
	if (!file.exists()){
		file.mkdirs();
	}

	//Copy the premade databases and related files to fake
	//contact provider directory
	copyFromAssetFile("contacts/contacts2.db", path+"/databases/contacts2.db");
	copyFromAssetFile("contacts/profile.db", path+"/databases/profile.db");
	copyFromAssetFile("contacts/contacts2.db-journal", path+"/databases/contacts2.db-journal");
	copyFromAssetFile("contacts/profile.db-journal", path+"/databases/profile.db-journal");
  }

  /**
   * Copy the file stored in asset directory to data directory
   * 
   * @param the source file in asset directory
   * @param the desination file 
   */
  public void copyFromAssetFile(String srcAssetFile, String dstFile) {

	final File file = new File(dstFile);
	//if (!file.exists()) {
		try {
			InputStream mInput = context.getAssets().open(srcAssetFile);
			OutputStream mOutput = new FileOutputStream(dstFile);
			byte[] mBuffer = new byte[1024];
        		int mLength;
        		while ((mLength = mInput.read(mBuffer))>0)
        		{
            			mOutput.write(mBuffer, 0, mLength);
        		}
        		mOutput.flush();
        		mOutput.close();
        		mInput.close();
		} catch (IOException e) {
      			Log.w("Copy database", "what happen "+dstFile, e);
		} 
	//}
  }

  /**
   * Notify the user that one of the system functionality that we are 
   * monitoring is being used by an application that we are monitoring
   * 
   * @param the title of the notification
   * @param the message that the notification shows
   */
  public void Notify(String notificationTitle, String notificationMessage){
	NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	@SuppressWarnings("deprecation")
      
	final Notification.Builder notification = new Notification.Builder(context);
	notification.setSmallIcon(R.drawable.ic_android_black_24dp)
		.setContentTitle(notificationTitle)		
		.setStyle(new Notification.BigTextStyle(notification)
			.bigText(notificationMessage));
	
	//Intent notificationIntent = new Intent(context,NotificationView.class);
        //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,notificationIntent, 0);
      
        //notification.setLatestEventInfo(context, notificationTitle,notificationMessage, pendingIntent);
        notificationManager.notify(0, notification.build());
  }

  /**
   * Produce a log entry in the logfile. The function format the string
   * into an convention style for ease of reading
   * 
   * @param The package name
   * @param The package uid
   * @param The message
   * @param allow/block policy
   */
  public void logEntry(int uid, String message, boolean la) {

	final DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
	final DateFormat dtfile = new SimpleDateFormat("dd-MM-yyyy");

	String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SentinelLogs/";
	final File dfile = new File (path);
	if (!dfile.exists()){
		dfile.mkdirs();
	}
	
	final File file = new File (path+ dtfile.format(Calendar.getInstance().getTime()) + ".txt");
	if (!file.exists()){
		try {
			file.createNewFile();
		} catch(IOException e){	
		}
	}

	final File file1 = new File (path+ String.valueOf(uid)+".txt");
	if (!file1.exists()){
		try {
			file1.createNewFile();
		} catch (IOException e) {
		}
	}

	StringBuilder builder = new StringBuilder(1000);
        builder.append(df.format(Calendar.getInstance().getTime())).append(" - ");
        builder.append("[").append(String.valueOf(uid)).append("] - ");
	if (la == true) {
	        builder.append("[").append("BLOCKED").append("] - ");
	} else {
		builder.append("[").append("UNBLOCKED").append("] - ");
	}

        builder.append(message);
        builder.append("\n");
	
	try {
		FileWriter fileWritter = new FileWriter(file,true);
        	BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
        	bufferWritter.write(builder.toString());
        	bufferWritter.close();
	} catch (IOException e){
	}
	
	try {
		FileWriter fileWritter1 = new FileWriter(file1,true);
        	BufferedWriter bufferWritter1 = new BufferedWriter(fileWritter1);
        	bufferWritter1.write(builder.toString());
        	bufferWritter1.close();
	} catch (IOException e) {
	}
  }	

}
