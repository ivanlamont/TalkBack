package com.maltino.net.talkback;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.FileSystem;
import java.nio.file.Path;

public class CloudContextStore {

    private Activity appActivity;
    private SharedPreferences sharedPref;
    private DownloadManager manager;
    private long requestedFileReference;
    private String contextFileFolder = "/storage/emulated/0/Android/data/com.maltino.net.talkback/files/Download";
    private String contextFileName = "bert_context_novato.json";
    private String cloud_URL_Scheme = "https";
    private String cloud_URL_Server = "9abemzvla6.execute-api.us-east-1.amazonaws.com"; //AWS API Gateway - Resources - GoCarAIChatContext (9abemzvla6)
    private String cloud_URL_Stage = "gocar-ai-beta";
    private String cloud_URL_Folder = "gocar-ai";
    private String cloud_URL_FileName = "bert_context_novato.json";
    private Location latLng;
    private TextView locationDisplay;
    private TextView fileDisplay;
    private LocalAreaDescriptions locationInfo;

    private String JSON_FILE = "JSON-FILE";

    public CloudContextStore(MainActivity parent, TextView fileNameDisplay) {
        try {
            appActivity = parent;
            fileDisplay = fileNameDisplay;
            sharedPref = appActivity.getPreferences(Context.MODE_PRIVATE);
            contextFileName = getLastSuccessfulFileName();
            File stored = new File(contextFileFolder + "/" + contextFileName);
            if (!stored.exists()) {
                File alternative = getBestGuessJson(contextFileFolder);
                if (alternative != null)
                    stored = alternative;
            }
            prepareLocalContextData(stored);
        } catch (Exception e) {
            Toast
                    .makeText(appActivity, "Creating cloud engine failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private File getBestGuessJson(String path) {

        File s = new File(path);
        File[] jsons = new File(path).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("json");
            }
        });
        if (jsons != null && jsons.length > 0)
            return jsons[0];
        else
            return null;
    }

    public void setRefreshButton(FloatingActionButton refreshButton) {
        refreshButton.setOnClickListener(new audioRequestor());
        appActivity.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    class audioRequestor implements View.OnClickListener {
        @Override
        public void onClick(View v)
        {
            ReloadFromCloud();
        }
    }

    public void ReloadFromCloud() {
        try {

            cloud_URL_FileName = "san_francisco_bert_context.json";

            manager = (DownloadManager) appActivity.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri source = new Uri.Builder()
                    .scheme(cloud_URL_Scheme)
                    .authority(cloud_URL_Server)
                    .appendPath(cloud_URL_Stage)
                    .appendPath(cloud_URL_Folder)
                    .appendPath(cloud_URL_FileName)
                    .build();

            DownloadManager.Request request = new DownloadManager
                    .Request(source)
                    .setTitle(cloud_URL_FileName)
                    .setDescription("Local Context Information for AI engine")
                    .setDestinationInExternalFilesDir(appActivity, Environment.DIRECTORY_DOWNLOADS, cloud_URL_FileName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    ;
            requestedFileReference = manager.enqueue(request);

        }
        catch (Exception e) {
            Toast
                    .makeText(appActivity, "Download of AI Context failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (requestedFileReference == reference) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    Bundle extras = intent.getExtras();
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
                    Cursor c = manager.query(q);
                    if (c.moveToFirst()) {
                        int colStatus = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = c.getInt(colStatus);
                        switch (status) {
                            case DownloadManager.STATUS_SUCCESSFUL:
                                String fullPath = null;
                                File source = null;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    int colURI = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                                    fullPath = c.getString(colURI);
                                    source = new File(Uri.parse(fullPath).getPath());
                                } else {
                                    int localFileName = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                                    fullPath = c.getString(localFileName);
                                    source = new File(fullPath);
                                }
                                prepareLocalContextData(source);
                                break;
                            case DownloadManager.STATUS_FAILED:
                                int reasonIndex = c.getColumnIndex(DownloadManager.COLUMN_REASON);
                                int reason = c.getInt(reasonIndex);
                                Toast.makeText(appActivity, String.format("Download Failed: %d", reason), Toast.LENGTH_LONG);
                        }
                    }
                    c.close();
                }
            }


            //appActivity.unregisterReceiver(this);


        }
    };

    public String CurrentContext() {
        if (locationInfo != null)
            return locationInfo.currentContext;
        else
            return "";
    }

    public void updateLocation(Location latLng, TextView userInterface) {
        this.latLng = latLng;
        this.locationDisplay = userInterface;
        if (locationInfo != null)
            locationInfo.updateLocation(latLng, userInterface);
    }

    private void prepareLocalContextData(File filename) {
        prepareLocalContextData(filename, false);
    }

    private void prepareLocalContextData(File jsonfile, boolean useTestIfNotFound) {
        LocalAreaDescriptions newContextInfo = null;
        if (jsonfile.exists()) {
            long len = jsonfile.length();
            try {
                newContextInfo = ObjectSerialization.getInstance().DeserializeFromFile(jsonfile, LocalAreaDescriptions.class);
                setLastSuccessfulFileName(jsonfile.getName());
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        if (useTestIfNotFound && (newContextInfo == null || newContextInfo.IsEmpty())) {
            newContextInfo = useTestLocalContextData();
        }
        if (newContextInfo != null) {
            locationInfo = newContextInfo;
            fileDisplay.setText(jsonfile.getName() + " loaded");
//            Toast.makeText(appActivity, String.format("Using Context File: %d", jsonfile.getName()), Toast.LENGTH_LONG);
            if (latLng != null)
                locationInfo.updateLocation(latLng, locationDisplay);
        }
    }

    private LocalAreaDescriptions useTestLocalContextData() {
        LocalAreaDescriptions result = LocalAreaDescriptions.getTestData();
        ObjectSerialization.getInstance().SerializeToFile(result, contextFileName);
        return result;
    }

    private String getLastSuccessfulFileName() {
        return sharedPref.getString(JSON_FILE, "localData.json");
    }

    private void setLastSuccessfulFileName(String fileName) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(JSON_FILE, fileName);
        editor.apply();
    }
}
