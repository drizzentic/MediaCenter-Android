package com.miz.mizuuphones;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class movieDetails extends Activity {

	// Database and cursor variables
	private static DbAdapter dbHelper;		// Database adapter
	private static Cursor cursor;			// Cursor for accessing the database
	
	// Movie database variables
	private int rowID;						// The row ID of the selected movie

	// UI VARIABLES
	// ImageView
	private ImageView imgPoster;			// Movie poster ImageView
	
	// TextViews
	private TextView textTitle;				// Movie title TextView
	private TextView textPlot;				// Movie plot TextView
	private TextView textSrc;				// Movie file source TextView
	private TextView textGenre;				// Movie genre TextView
	private TextView textRelease;			// Movie release date TextView
	private TextView textRuntime;			// Movie runtime TextView
	private TextView textTagline;			// Movie tagline TextView
	private TextView textRating;			// Movie rating TextView

	// Strings
	private String IMDBID = "";				// Movie IMDb ID
	private String TMDBID = null;			// Movie TMDb ID
	private String trailerUrl = "";			// Movie trailer URL
	
	private float rating;					// Rating float value

	// Data path
	private String dataPath;				// Path to the application data folder

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.popupdetails);
		
		// Initializes the dataPath variable
		dataPath = Environment.getExternalStorageDirectory().toString() + "/data/com.miz.mizuu/";

		Bundle bundle = this.getIntent().getExtras();
		rowID = bundle.getInt("rowId");
		
		initializeDetails();
		
	}
	
	public void initializeDetails() {
		// Create and open database
		dbHelper = new DbAdapter(this);
		dbHelper.open();

		textTitle = (TextView) findViewById(R.id.textView1);
		textPlot = (TextView) findViewById(R.id.textView2);
		textSrc = (TextView) findViewById(R.id.textView3);
		textRelease = (TextView) findViewById(R.id.textView8);
		textGenre = (TextView) findViewById(R.id.textView5);
		textRuntime = (TextView) findViewById(R.id.textView9);
		textRating = (TextView) findViewById(R.id.textView10);
		textTagline = (TextView) findViewById(R.id.textView6);
		imgPoster = (ImageView) findViewById(R.id.imageView1);

		cursor = dbHelper.fetchMovie(rowID);

		if (cursor.moveToFirst()) {
			
			TMDBID = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TMDBID));

			trailerUrl = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TRAILER));

			textTitle.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE)));
			textPlot.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_PLOT)));
			textSrc.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)));
			textSrc.setTypeface(null, Typeface.ITALIC);
			textRelease.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RELEASEDATE)));
			if (textRelease.getText().toString().startsWith(" ")) { // If there's a space before the release date
				textRelease.setText(textRelease.getText().toString().substring(1,textRelease.getText().toString().length()));
			}
			
			textTagline.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TAGLINE)));
			if (!(textTagline.getText().toString().length() > 0)) {
				textTagline.setVisibility(View.GONE);
			}
			
			
			textGenre.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_GENRES)));
			if (textGenre.getText().toString().length() == 0) {
				textGenre.setText(getString(R.string.stringNA));
			}

			textRuntime.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RUNTIME)) + " " + getString(R.string.minutes));

			try {
				rating = Float.valueOf(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RATING)).trim()).floatValue();
			} catch (Exception e) {
				rating = Float.valueOf("0.0".trim()).floatValue();
			}
			textRating.setText(String.valueOf(rating));

			// Poster
			File posterFile = new File(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)));

			// Get the file name of the movie file
			String fileName = posterFile.getName().substring(0, posterFile.getName().lastIndexOf("."));

			// Create variables for potential custom art check
			boolean potentialImage = false;
			String ImageFile = null;
			String[] potentialImageFiles = new String[]{posterFile.getAbsolutePath().substring(0, posterFile.getAbsolutePath().lastIndexOf(".")) + ".jpg",
					posterFile.getAbsolutePath().substring(0, posterFile.getAbsolutePath().lastIndexOf(".")) + ".jpeg",
					posterFile.getAbsolutePath().substring(0, posterFile.getAbsolutePath().lastIndexOf(".")) + ".JPG",
					posterFile.getAbsolutePath().substring(0, posterFile.getAbsolutePath().lastIndexOf(".")) + ".JPEG"};

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
				// Add the custom cover art as the ImageView image
				Bitmap bm = BitmapFactory.decodeFile(ImageFile);
				imgPoster.setImageBitmap(createReflection(bm));
				bm.recycle();
				bm = null;
			} else {
				// Check if there's a cover art
				if (cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COVERPATH)).equals("NOCOVERIMG")) {
					// Add "NOCOVERIMG" as the ImageView image
					imgPoster.setImageBitmap(createReflection(BitmapFactory.decodeResource(this.getResources(),R.drawable.nocover)));
				} else {
					// Create new File with the path of the automatically downloaded cover art
					posterFile = new File(dataPath + fileName + ".jpg");
					if (posterFile.exists()) {
						imgPoster.setImageBitmap(createReflection(BitmapFactory.decodeFile(posterFile.toString())));
					} else {
						/// Add "NOCOVERIMG" as the ImageView image
						imgPoster.setImageBitmap(createReflection(BitmapFactory.decodeResource(this.getResources(),R.drawable.nocover)));
					}
				}
			}

			IMDBID = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_IMDBID));

		}
		
		cursor.close();
		dbHelper.close();
	}
	
	public Bitmap createReflection(Bitmap sprite) {
		try {		
			//The gap we want between the reflection and the original image
			final int reflectionGap = 1;

			int width = 0;
			int height = 0;

			try {
				width = sprite.getWidth();
				height = sprite.getHeight();
			} catch (NullPointerException e) {
				width = 225;
				height = 330;
				Log.d("ERROR", "NullPointerException in movieDetails.createReflection(): " + e);
			}

			final double relation = ((double) height / (double) width);
			width = (int) (330 / relation);
			height = 330;

			sprite = Bitmap.createScaledBitmap(sprite, width, 330, true);

			//This will not scale but will flip on the Y axis
			Matrix matrix = new Matrix();
			matrix.preScale(1, -1);

			//Create a Bitmap with the flip matix applied to it.
			//We only want the bottom half of the image
			Bitmap reflectionImage = Bitmap.createBitmap(sprite, 0, 301, width, 29, matrix, true);

			//Create a new bitmap with same width but taller to fit reflection
			Bitmap bitmapWithReflection = Bitmap.createBitmap(width, 360, Config.ARGB_8888);

			//Create a new Canvas with the bitmap that's big enough for
			//the image plus gap plus reflection
			Canvas canvas = new Canvas(bitmapWithReflection);

			//Draw in the original image
			canvas.drawBitmap(sprite, 0, 0, null);

			//Draw in the gap
			Paint deafaultPaint = new Paint();
			canvas.drawRect(0, height, width, height + reflectionGap, deafaultPaint);

			//Draw in the reflection
			canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

			//Create a shader that is a linear gradient that covers the reflection
			Paint paint = new Paint(); 
			LinearGradient shader = new LinearGradient(0, height, 0, bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);

			//Set the paint to use this shader (linear gradient)
			paint.setShader(shader); 

			//Set the Transfer mode to be porter duff and destination in
			paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN)); 

			//Draw a rectangle using the paint with our linear gradient
			canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint); 
			
			reflectionImage.recycle();
			reflectionImage = null;

			sprite.recycle();
			sprite = null;

			canvas = null;

			return bitmapWithReflection;

		} catch (OutOfMemoryError e) {
			Log.d("ERROR", "OutOfMemoryError in movieDetails.createReflection(): " + e);
			return sprite;
		}
	}

	public void playMovie(View v) {
		TextView textSrc = (TextView) findViewById(R.id.textView3);
		try
		{
			Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
			File file = new File(textSrc.getText().toString()); 
			myIntent.setDataAndType(Uri.fromFile(file), "video/*");
			startActivity(myIntent);
		}
		catch (Exception e) 
		{
			Toast.makeText(this, getString(R.string.movieCannotPlay), Toast.LENGTH_LONG).show();
			Log.d("playMovie", "Error: " + e);
		}
	}

	public void findTrailer(View v) {

		if (isOnline()) {
			if (trailerUrl.contains("youtube")) {
				Intent videoClient = new Intent(Intent.ACTION_VIEW);
				videoClient.setData(Uri.parse(trailerUrl));
				startActivity(videoClient);
			} else {
				TextView textTitle = (TextView) findViewById(R.id.textView1);
				StartSearch startSearch = new StartSearch();
				startSearch.execute(textTitle.getText().toString());
			}
		} else {
			Toast.makeText(this, getString(R.string.noInternet), Toast.LENGTH_SHORT).show();
		}

	}

	protected class StartSearch extends AsyncTask<String, String, String>
	{

		@Override
		protected String doInBackground(String... params) {

			final String query = params[0].replaceAll(" ", "%20");

			try {

				URL jsonURL = new URL("https://gdata.youtube.com/feeds/api/videos?q=" + query + "%20trailer&max-results=1&alt=json");
				URLConnection jc = jsonURL.openConnection(); 

				InputStream is = jc.getInputStream();

				BufferedReader r = new BufferedReader(new InputStreamReader(is));
				StringBuilder total = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					total.append(line);
				}

				JSONObject jObject;
				jObject = new JSONObject(total.toString()); 
				JSONObject jdata = jObject.getJSONObject("feed");
				JSONArray aitems = jdata.getJSONArray("entry");
				JSONObject item0 = aitems.getJSONObject(0);
				JSONObject id = item0.getJSONObject("id");

				String fullYTlink = id.getString("$t");
				String YTid = fullYTlink.substring(fullYTlink.lastIndexOf("videos/") + 7, fullYTlink.length());

				if (!(YTid.length() == 0)) {
					Intent videoClient = new Intent(Intent.ACTION_VIEW);
					videoClient.setData(Uri.parse("http://youtube.com/watch?v=" + YTid));
					startActivity(videoClient);
				} else {
					Toast.makeText(movieDetails.this, "No trailer found", Toast.LENGTH_SHORT).show();
				}

				Log.v("YOUTUBE", YTid);

			} catch (Exception e) {

			}

			return null;			
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.details, menu);
		return true;
	}
	
	public void deleteMovie(final View v) {
		// Create and open database
		dbHelper = new DbAdapter(this);
		dbHelper.open();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.sureToDelete))
		.setCancelable(false)
		.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (dbHelper.deleteMovie(rowID)) {
					Intent intent = new Intent(movieDetails.this, Main.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					dbHelper.close();
					finish();
				} else {
					Toast.makeText(movieDetails.this, getString(R.string.couldntDeleteMovie), Toast.LENGTH_SHORT).show();
				}
			}
		})
		.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void identifyMovie(View v) {
		Intent intent = new Intent();
		intent.putExtra("fileName", textSrc.getText().toString());
		intent.setClass(this, IdentifyMovie.class);
		this.startActivityForResult(intent, 0);
	}

	public void shareMovie() {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.shareILoved) + " " + textTitle.getText().toString() + " " + getString(R.string.shareOnMizuu) + ":\nhttp://www.imdb.com/title/" + IMDBID + "/");
		startActivity(Intent.createChooser(intent, getString(R.string.shareWith)));
	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	/*
	 * Called when the user presses the Change cover button. Starts a new CoverSearch intent
	 */
	public void searchCover(View v) {
		if (TMDBID != null && isOnline()) { // Make sure that the device is connected to the web and has the TMDb ID
			Intent intent = new Intent();
			intent.putExtra("id", TMDBID);
			intent.putExtra("filename", textSrc.getText().toString());
			intent.setClass(this, CoverSearch.class);
			this.startActivityForResult(intent, 0); // Start the intent for result
		} else {
			// No movie ID / internet connection
			Toast.makeText(this, getString(R.string.coverSearchFailed), Toast.LENGTH_LONG).show();
		}
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.share_this:
			shareMovie();
			break;
		}
		
		  return true;
		}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == 1) {
			File posterFile = new File(Environment.getExternalStorageDirectory().toString()
					+ "/data/com.miz.mizuu/" + textSrc.getText().toString().substring(textSrc.getText().toString().lastIndexOf("/"), textSrc.getText().toString().lastIndexOf(".")) + ".jpg");
			if (posterFile.exists()) {
				imgPoster.setImageBitmap(createReflection(BitmapFactory.decodeFile(posterFile.getAbsolutePath()))); // If the cover has been changed, recreate the UI
			} else {
				Toast.makeText(this, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();
				imgPoster.setImageResource(R.drawable.nocover); // If the cover has been changed, recreate the UI
			}
			Main.movieChanged = true;
		} else if (resultCode == 2) {
			initializeDetails();
			Main.movieChanged = true;
		}
	}
}