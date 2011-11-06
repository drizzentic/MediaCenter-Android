package com.miz.mizuuphones;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;

public class splashScreen extends Activity {

	protected boolean _active = true;
	protected int _splashTime = 500; // time to display the splash screen in ms

	private DbAdapter dbHelper;
	private Cursor cursor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		
		StartApp startApp = new StartApp();
		startApp.execute(true);
		
	}
	
	protected class StartApp extends AsyncTask<Boolean, String, String>
	{

		@Override
		protected String doInBackground(Boolean... params) {
			
			try {
				int waited = 0;
				while(_active && (waited < _splashTime)) {
					Thread.sleep(100);
					if(_active) {
						waited += 100;
					}
				}
			} catch(InterruptedException e) {
				// do nothing
			} finally {
				
				if (isDbSetup()) {
					Intent intent = new Intent(getApplicationContext(), Main.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					splashScreen.this.finish();
				} else {
					Intent intent = new Intent(getApplicationContext(), Setup.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					splashScreen.this.finish();
				}

			}
			
			return null;
		}
		
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			_active = false;
		}
		return true;
	}

	private boolean isDbSetup() {

		// Create and open database
		dbHelper = new DbAdapter(this);
		dbHelper.open();
		
		try {
			// Create Cursor and fetch all movies from the database
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			cursor = dbHelper.fetchAllMovies(settings.getString("prefsSorting", "ASC"));

			int count = cursor.getCount();
			
			cursor.close();
			dbHelper.close();
			
			// Check if there's anything in the cursor
			if (count > 0) {
				return true;
			} else {
				return false;
			}
		} catch (SQLiteException e) {
			return false;
		}
	}

}