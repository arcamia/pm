package com.sentinel.android.services.check;

import android.content.Intent;
import android.app.PendingIntent;
import android.location.Location;
import android.net.Uri;

/**
 * System-private API for talking to the LogService.
 *
 * {@hide}
 */
interface ICheckService {
	void flushLog();

    	/**
     	* Compare the two given Uids. This function tells if the
     	* calling process belongs to the monitoring list.
     	* @param the Uid of the calling process.
     	*/
	boolean compareUid(int callingUid);

    	/**
     	* Display the message in an activity.
     	* 
     	* @param the message to display
     	*/
	void displayNotice(String msg);

    	/**
     	* Send an intent that tells the app that the system
	* is trying to intialize the call.
     	* 
     	* @param the app name
     	*/
	void sendFakeOutgoingCallIntent(int appUid, in Intent callIntent);

    	/**
     	* Block the components of a blacklisted app that associate
	* with the given intent
     	* 
     	* @param the intent that we don't want the blacklisted components to know
     	*/
	void blockIntentReceiver(in Intent callIntent);
    	
	/**
     	* Send fake phone call notification to the destination application.The function
	* sends a seri of notifications that tells the app that there is an ongoing/missed
	* phone call, while it does not happen.
     	* 
     	* @param The name of the package that we want to notify
	* @param The type of call we want to notify (incoming/outgoing)
	* @param The phone number that associate with the fake call
	* @param The time duration of the fake call (leave as 0 for missed call)
	* measure in seconds
     	*/
	void sendFakeCallNotification(String packageName, String callType, String phoneNumber, int duration);

    	/**
     	* Get the UID of the calling application
     	*/
	int getCallingAppUid();

    	/**
     	* Check if the intent is for asking to make a phone call
     	* 
     	* @param the intent that we want to examine
     	*/
	boolean isRequestingCall(in Intent callIntent);

    	/**
     	* Check if the intent is for asking to send a sms message
     	* 
     	* @param the intent that we want to examine
     	*/
	boolean isRequestingSendSms(in Intent callIntent);

    	/**
     	* Send fake notification to the application that uses the SmsManager 
	* utility to send text message
     	* 
     	* @param the UID of the application that we want to send fake notification
     	*/
	void processSendTextMessageUtility(int Uid, String destAddress, String srcAddress, String text,
		 in PendingIntent sentIntent, in PendingIntent deliveryIntent);

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
	void processIndirectIntent(int Uid, in Intent deliveryIntent, int requestCode);

    	/**
     	* Return an fake Location object to the requesting process.
	*
	* Instead of letting the real Location Manage retrieve the stored location,
	* we craft a fake one and let the requested process to have it.
	* 
      	* @param The string that indicates the location provider name
	* @param the Intent comes from that application
     	*/
	Location answerWithFakeLocation(String provider);

    	/**
     	* Check if the intent is for asking to take a photo from camera
     	* 
     	* @param the intent that we want to examine
     	*/
	boolean isRequestingCameraPhoto(in Intent callIntent);

    	/**
     	* Create an intent that contain a fake photo for the requesting app
     	* 
     	* @param none
     	*/
	Intent getFakePhotoByIntent();

    	/**
     	* Make a copy of the captured photo into our own service
	* directory for later inspection
     	* 
     	* @param the uid of the blacklisted app that used the camera
	* @param the captured image photo
     	*/
	void saveCapturedPhotoFromCamera(int uid, in byte[] bytes);	

    	/**
     	* Give the connected Connection a fake requested InputStream
     	* 
     	* @param the uid of the blacklisted app that used the camera
	* @param the captured image photo
     	*/
	void saveCapturedInputStream(int uid, in byte[] bytes, String extension);

    	/**
     	* Generate the file name to store 
     	* 
     	* @param none
     	*/
	String generateFileName();

    	/**
     	* Get file extension from donwload httpURLConnection 
     	* 
     	* @param the URL to extract the extension string
     	*/
	String extractExtFromURL(String url);

    	/**
     	* Generate the path for public storage 
     	* 
     	* @param none
     	*/
	String generatePublicPath();

    	/**
     	* Check if the intent is for asking to use the BluetoothAdapter
     	* 
     	* @param the intent that we want to examine
     	*/
	boolean isRequestingBluetooth(in Intent callIntent);

    	/**
     	* Check if the intent is for asking to use the DevicePolicyManager
     	* 
     	* @param the intent that we want to examine
     	*/
	boolean isRequestingDevicePolicyManager(in Intent callIntent);

    	/**
     	* Convert the URI to an other one that refers to a different
	* content provider
     	* 
     	* @param the URI that we want to convert
     	*/
	Uri convertTheUri(in Uri uri);

    	/**
     	* Check and create fake database corresponding to contact and
	* profile data storage
     	* 
     	* @param none
     	*/
	void checkAndCreateFakeContactDatabase();

    	/**
        * Copy the file stored in asset directory to data directory
        * 
        * @param the source file in asset directory
        * @param the desination file 
        */
        void copyFromAssetFile(String srcAssetFile, String dstFile);

        /**
        * Notify the user that one of the system functionality that we are 
        * monitoring is being used by an application that we are monitoring
        * 
        * @param the title of the notification
        * @param the message that the notification shows
        */
        void Notify(String notificationTitle, String notificationMessage);

        /**
        * Produce a log entry in the logfile. The function format the string
        * into an convention style for ease of reading
        * 
        * @param The package uid
	* @param The message
	* @param allow/block policy
        */
        void logEntry(int uid, String message, boolean la);

}
