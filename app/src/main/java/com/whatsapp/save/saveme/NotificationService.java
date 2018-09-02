package com.whatsapp.save.saveme;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class NotificationService extends NotificationListenerService {
    int inc = 1;
    int flag = 1;
    private String TAG = this.getClass().getSimpleName();
    private NotificationService.NLServiceReceiver nlservicereciver;

    public NotificationService() {

    }

    public void onCreate() {
        super.onCreate();
        this.nlservicereciver = new NotificationService.NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.whatsapp.save.saveme.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
        this.registerReceiver(this.nlservicereciver, filter);
    }

    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.nlservicereciver);
    }


    public void onNotificationPosted(StatusBarNotification sbn) {
        if (flag == 1) {
            if (sbn.getPackageName().equals("com.whatsapp") || sbn.getPackageName().equals("com.whatsapp.w4b")) {
                Bundle extras = sbn.getNotification().extras;
                String title = extras.getString("android.title");

                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy hh:mm");
                String strDate = formatter.format(date);

                //Date cur = Calendar.getInstance().getTime();
                if (title.contains("+91 ")) {
                    String a = title;
                    String pno = a.substring(5, 16);
                    String reg = "(\\d{5} \\d{5})";

                    System.out.println("Reg:" + Pattern.matches(reg, pno));
                    System.out.println("HERE: " + pno);

                    if (getContactDisplayNameByNumber(pno) == "?" && Pattern.matches(reg, pno)) {
                        String temp = strDate + " - " + inc;
                        writeContact(temp, pno);
                        inc++;
                        String t2 = pno + " --> " + temp;

                        try {
                            logMake(t2);

                        } catch (IOException e) {
                            Toast.makeText(this, "error in saving", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                        Toast.makeText(this, "Contact Added", Toast.LENGTH_SHORT).show();
                    }
                }

                Intent i = new Intent("com.whatsapp.save.saveme.NOTIFICATION_LISTENER_EXAMPLE");
                i.putExtra("notification_event", title + "\n");
                this.sendBroadcast(i);


                try {
                    cancelNotification(sbn.getKey());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class NLServiceReceiver extends BroadcastReceiver {
        NLServiceReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getStringExtra("command").equals("expire")) {
                flag = 0;
            }
        }
    }


    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = "?";

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

    private void writeContact(String displayName, String number) {
        String account = getUsernameLong(getApplicationContext());
        ArrayList op = new ArrayList();
        //insert raw contact using RawContacts.CONTENT_URI
        op.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "com.google")
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account).build());
        //insert contact display name using Data.CONTENT_URI
        op.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                .build());
        //insert mobile number using Data.CONTENT_URI
        op.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build());
        try {
            getApplicationContext().getContentResolver().
                    applyBatch(ContactsContract.AUTHORITY, op);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    public static String getUsernameLong(Context context) {
        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {

            // account.name as an email address only for certain account.type values.
            possibleEmails.add(account.name);
            Log.i("DGEN ACCOUNT", "CALENDAR LIST ACCOUNT/" + account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            String email = possibleEmails.get(0);
            return email;

        }
        return null;
    }

    public void logMake(String str) throws IOException {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy");
        String strDate = formatter.format(date);


        String directory_path = Environment.getExternalStorageDirectory().getPath() + "/SaveMeLogs/";
        File file = new File(directory_path);
        if (!file.exists()) {
            file.mkdirs();
        }
        String ffile = directory_path + "/" + strDate + ".txt";
        File g = new File(ffile);
        if (!g.exists()) {
            g.createNewFile();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(ffile, true));
        writer.append('\n');
        writer.append(str);
        writer.close();

    }
}
