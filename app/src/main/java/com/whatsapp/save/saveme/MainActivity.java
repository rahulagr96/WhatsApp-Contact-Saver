package com.whatsapp.save.saveme;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_REQUEST_CODE = 101;
    public static final int MULTIPLE_PERMISSIONS = 10;
    private TextView txtView;
    private MainActivity.NotificationReceiver nReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtView = findViewById(R.id.textView);
        this.nReceiver = new MainActivity.NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.whatsapp.save.saveme.NOTIFICATION_LISTENER_EXAMPLE");
        this.registerReceiver(this.nReceiver, filter);
    }

    private void checkContactPermission() {
        Context context = getApplicationContext();
        String[] permissions = new String[]{
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(context, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissionsList[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissionsList) {
                        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                            permissionsDenied += "\n" + per;

                        }

                    }
                    // Show permissionsDenied

                }
                return;
            }
        }
    }

    private void checkNotifAccess() {
        if (!NotificationManagerCompat.getEnabledListenerPackages(getApplicationContext())
                .contains(getApplicationContext().getPackageName())) {
            AlertDialog alertDialog = new AlertDialog.Builder(
                    MainActivity.this).create();
            alertDialog.setTitle("Alert");
            alertDialog.setMessage("You need to give notifiaction access");
            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(intent, NOTIFICATION_REQUEST_CODE);
                }
            });
            alertDialog.show();
        } else {
            Log.d("TAG", "You have Notification Access");
        }
        NotificationManagerCompat.getEnabledListenerPackages(getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.feed: {
                Intent intent = new Intent(Intent.ACTION_SENDTO);//common intent
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"1.rahul1096@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for ContactSave");
                intent.putExtra(Intent.EXTRA_TEXT, "Hi,");
                startActivity(Intent.createChooser(intent, "Email via..."));
                return true;
            }
            case R.id.action_settings: {
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class NotificationReceiver extends BroadcastReceiver {
        NotificationReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String temp = intent.getStringExtra("notification_event") + "\n" + MainActivity.this.txtView.getText();
            MainActivity.this.txtView.setText(temp);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        checkNotifAccess();
        checkContactPermission();
    }
}
