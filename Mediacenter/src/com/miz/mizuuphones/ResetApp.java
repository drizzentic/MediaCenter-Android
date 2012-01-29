package com.miz.mizuuphones;

import java.io.File;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

public class ResetApp extends Activity {

	SharedPreferences settings;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		setContentView(R.layout.reset);
		
	}
	
	public void resetDb(View v) {
		
		this.deleteDatabase("mizuu_data");

		// Delete all cover art images in the data directory
		File fileDirectory = new File(Environment.getExternalStorageDirectory().toString() + "/data/com.miz.mizuu/");
		if (fileDirectory.isDirectory()) {
			try {
				String[] children = fileDirectory.list();
				for (int i = 0; i < children.length; i++) {
					new File(fileDirectory, children[i]).delete();
				}
			} catch (NullPointerException e) {
				Log.d("ERROR", "NullPointerException in Setup.removeMoviesFromDb() - trying to delete cover art: " + e);
			}
		}
		
		Editor editor = settings.edit();
		editor.putBoolean("prefsReset", true);
		editor.commit();
		
		finish();
		
	}
	
}