package com.miz.mizuuphones;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {

	// Database and cursor variables
	private static DbAdapter dbHelper;		// Database adapter
	private static Cursor cursor;			// Cursor for accessing the database

	// Movie counter
	private static int counter;				// Counter for number of movies in Gallery view

	// Context
	private Context context;				// The context of this activity

	// Data path
	private String dataPath;				// Path to the application data folder

	// File ArrayLists
	public static ArrayList<String> coverFileNames;	// Cover art file names
	public static ArrayList<String> movieNames;		// File names for videos
	public static ArrayList<String> videoUrls;		// Full paths to videos

	// TextView variables
	private TextView mainTitle;				// TextView for title of the view
	private TextView movieCounter;			// TextView for the movie counter

	private GridView gridview;				// GridView for the gallery containing movies
	private GridImageAdapter gridImgAdapter;// GridImageAdapter instance used for the GridView view

	public static Bitmap nocover;

	public static boolean movieChanged;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Initialize context variable
		context = this;

		nocover = BitmapFactory.decodeResource(Main.this.getResources(),R.drawable.nocover);

		// Set content view to the main2.xml file
		setContentView(R.layout.main2);

		// Create new ArrayLists
		coverFileNames = new ArrayList<String>();
		movieNames = new ArrayList<String>();
		videoUrls = new ArrayList<String>();

		// Initializes the dataPath variable
		dataPath = Environment.getExternalStorageDirectory().toString() + "/data/com.miz.mizuu/";

		gridImgAdapter = new GridImageAdapter();
		gridview = (GridView) findViewById(R.id.gridView1);

		loadMainGridView(0);

	}

	private void loadMainGridView(int type) {

		// Reset counter each time loadMain() is called
		counter = 0;
		
		// Create new ArrayLists
		coverFileNames = new ArrayList<String>();
		movieNames = new ArrayList<String>();
		videoUrls = new ArrayList<String>();

		// Clear ArrayLists every time loadMain() is called
		coverFileNames.clear();
		movieNames.clear();
		videoUrls.clear();

		// Create and open database
		dbHelper = new DbAdapter(this);
		dbHelper.open();

		cursor = dbHelper.fetchAllMovies("ASC");

		// Do while the cursor can move to the next item in cursor
		while (cursor.moveToNext()) {

			// Create new file for the file path of the movie
			File file = new File(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)));

			// Add item to videoUrls and movieNames
			movieNames.add(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE))); // Add movie title
			videoUrls.add(file.getAbsolutePath().toString()); // Add path to file

			// Get the file name of the movie file
			String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));

			// Create variables for potential custom art check
			boolean potentialImage = false;
			String ImageFile = null;
			String[] potentialImageFiles = new String[]{file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + ".jpg",
					file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + ".jpeg",
					file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + ".JPG",
					file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + ".JPEG"};

			// Check if each file exists and return if one does
			for (String potentialFile : potentialImageFiles) {
				if (!potentialImage) {
					if (new File(potentialFile).exists()) {
						potentialImage = true;
						ImageFile = potentialFile;
					}
				}
			}

			// Check if there's a custom cover art
			if (potentialImage) {
				coverFileNames.add(ImageFile); // Add the custom cover art to the list of coverFileNames
			} else {

				try {
					// Check if there's a cover art
					if (cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COVERPATH)).equals("NOCOVERIMG")) {
						coverFileNames.add("NOCOVERIMG"); // Add "NOCOVERIMG" to the ArrayList, if no cover art was found
					} else {
						// Create new File with the path of the automatically downloaded cover art
						file = new File(dataPath + fileName + ".jpg");
						if (file.exists()) {
							coverFileNames.add(file.toString()); // Add the file path to the coverFileNames ArrayList
						} else {
							coverFileNames.add("NOCOVERIMG"); // Add "NOCOVERIMG" to the ArrayList, if no cover art was found
						}
					}
				} catch (NullPointerException e) {
					coverFileNames.add("NOCOVERIMG"); // Add "NOCOVERIMG" to the ArrayList, if no cover art was found
				}
			}

			// Increment the counter
			counter++;
		}

		// Find and initialize the title TextView
		mainTitle = (TextView) findViewById(R.id.txtMainTitle);

		// Find and initialize the movie counter TextView
		movieCounter = (TextView) findViewById(R.id.txtNumberofMovies);

		// Check files type
		if (type == 0) { // All movies
			mainTitle.setText(getString(R.string.mainMyMoviesTitle)); // Set mainTitle
			movieCounter.setText(counter + " " + getString(R.string.mainNumberofMovies)); // Set movieCounter
		} else if (type == 1) { // Favourites only
			mainTitle.setText(getString(R.string.mainMyFavsTitle)); // Set mainTitle
			movieCounter.setText(counter + " " + getString(R.string.mainNumberofFavs)); // Set movieCounter
		}

		gridImgAdapter.notifyDataSetChanged();
		gridview.setAdapter(gridImgAdapter);
		gridview.setOnItemClickListener(new OnItemClickListener() { // Set OnItemClick listener
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

				// If the dbHelper is closed, open it
				dbHelper.open();

				// Fetch the movie at the pressed item position
				Cursor cursorRowId = dbHelper.fetchRowId(videoUrls.get(position));

				// If the cursor is capable of moving to the first item, start the details view for the selected movie
				if (cursorRowId.moveToFirst()) {

					// Add the rowID of the selected movie into a Bundle
					Bundle bundle = new Bundle();
					bundle.putInt("rowId", cursorRowId.getInt(0));

					// Create a new Intent with the Bundle
					Intent intent = new Intent();
					intent.setClass(context, movieDetails.class);
					intent.putExtras(bundle);

					// Start the Intent for result
					startActivityForResult(intent, 0);
				}

				// Close the dbHelper
				dbHelper.close();
			}
		});

		// Close the cursor and dbHelper
		cursor.close();
		dbHelper.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuSettings:
			Toast.makeText(this, "Sorry - currently not available!", Toast.LENGTH_SHORT).show();
			//startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.menuAbout:
			startActivity(new Intent(this, About.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (movieChanged) {
			loadMainGridView(0);
			movieChanged = false;
		}
	}

	public void startSearch(View v) {
		Intent intent = new Intent();
		intent.setClass(this, Search.class);
		this.startActivity(intent);
	}

	public void startUpdate(View v) {
		Intent intent = new Intent();
		intent.setClass(this, Update.class);
		this.startActivityForResult(intent, 0);
	}
	
	@Override
	public void onBackPressed() {
		finish(); // Instead of going back to update / setup, this will end the Activity
		return;
	}

	public class GridImageAdapter extends BaseAdapter {

		public ImageLoader imageLoader;

		public GridImageAdapter() {
			imageLoader = new ImageLoader();
		}

		public int getCount() {
			return coverFileNames.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {

			ImageView imageView;
			
			if (convertView == null) {
				
				imageView = new ImageView(Main.this);
				imageView.setLayoutParams(new GridView.LayoutParams(225, 330));
	            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
				imageView.setPadding(5, 5, 5, 5);

			} else {
				imageView = (ImageView) convertView;
			}

			// Create new file for the file path of the movie
			File file = new File(videoUrls.get(position));

			// Create variables for potential custom art check
			boolean potentialImage = false;
			String ImageFile = null;
			String[] potentialImageFiles = new String[]{file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + ".jpg",
					file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + ".jpeg",
					file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + ".JPG",
					file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".")) + ".JPEG"};

			// Check if each file exists and return if one does
			for (String potentialFile : potentialImageFiles) {
				if (!potentialImage) {
					if (new File(potentialFile).exists()) {
						potentialImage = true;
						ImageFile = potentialFile;
					}
				}
			}
			
			// Check if there's a custom cover art
			if (potentialImage) {
				imageLoader.DisplayImage(ImageFile, Main.this, imageView, position);
			} else {
				imageLoader.DisplayImage(coverFileNames.get(position), Main.this, imageView, position);
			}
			
			return imageView;
		}

	}
}