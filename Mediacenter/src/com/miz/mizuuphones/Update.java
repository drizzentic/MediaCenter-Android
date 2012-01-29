package com.miz.mizuuphones;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Update extends Activity {

	private static DbAdapter dbHelper;
	private static Cursor cursor;

	private static ArrayList<String> videoFiles;
	private static ArrayList<File> results;
	private ArrayList<String> existingMovies;

	private static ArrayList<Tmdb> tmdbMovies;

	static ProgressBar pBar;

	private static Context context;

	private static ProgressDialog dialog;

	private static int count = 0;

	private static TextView txtWorking;

	private CheckBox checkBox;
	private static boolean check = false;

	private String CustomDirPref;
	private boolean fileTypePref;

	private static Pattern searchPattern;
	private static Matcher searchMatcher;

	private String[] normalFileTypes = new String[]{".3gp", ".3gpp", ".mp4"};
	private String[] allFileTypes = new String[]{".3gp", ".aaf", ".mp4", ".ts", ".webm", ".m4v", ".mkv", ".divx",
			".xvid", ".avi", ".flv", ".f4v", ".moi", ".mpeg", ".mpg", ".mts", ".ogv", ".rm", ".rmvb", ".mov", ".wmv"};

	private static String[] warezTags = new String[]{"dvdrip", "dvd5", "dvd", "xvid", "divx", "m-480p", "m-576p", "m-720p", "m-864p", "m-900p",
		"m-1080p", "m480p", "m576p", "m720p", "m864p", "m900p", "m1080p", "480p", "576p", "720p", "864p", "900p", "1080p",
		"brrip", "bdrip", "aac", "x264", "bdrip", "bluray", "dts", "screener", "hdtv", "ac3", "repack", "2.1", "5.1",
		"7.1", "h264", "264", "hdrip", "dvdscr", "pal", "ntsc", "proper", "readnfo", "rerip", "vcd", "scvd", "pdtv", "sdtv",
		"tvrip", "extended"};

	private ArrayList<String> storageDirectories;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		CustomDirPref = settings.getString("prefsCustomDir", "");
		fileTypePref = settings.getBoolean("prefsFileTypes", false);

		setContentView(R.layout.update1);

		context = this;

		// Initialize ArrayList - looks like crap, but it's better for performance than double bracing it
		storageDirectories = new ArrayList<String>();
		//storageDirectories.add("/mnt/");
		storageDirectories.add(Environment.getExternalStorageDirectory().toString());
		//storageDirectories.add("/Removable/");
		//storageDirectories.add("/sdcard/");
		//storageDirectories.add("/sdcard-ext/");
		if (!(CustomDirPref.length() == 0) && !CustomDirPref.equals("/")) {
			storageDirectories.add(CustomDirPref);
		}

		checkBox = (CheckBox) findViewById(R.id.checkBox1);
		checkBox.setChecked(false);

	}

	public boolean checkFileTypes(String file) {

		if (fileTypePref) {
			for (String type : allFileTypes) {
				if (file.toLowerCase().endsWith(type)) {
					return true;
				}
			}
		} else {
			for (String type : normalFileTypes) {
				if (file.toLowerCase().endsWith(type)) {
					return true;
				}
			}
		}

		// If it reaches this point, the file doesn't end with one of the extensions
		return false;
	}

	public void startUpdate(View v) {

		dialog = ProgressDialog.show(this, getString(R.string.dialogSearching), getString(R.string.pleaseWait), true);

		count = 0;
		existingMovies = new ArrayList<String>();
		videoFiles = new ArrayList<String>();
		results = new ArrayList<File>();

		// Create and open database
		dbHelper = new DbAdapter(this);
		dbHelper.open();

		if (isOnline()) {
			StartUpdate startUpdate = new StartUpdate();
			startUpdate.execute(true);

		} else {

			dialog.dismiss();

			// Change the view to the second part of the setup guide
			setContentView(R.layout.update2);

			// Set up TextView and ProgressBar variables
			TextView textTitle = (TextView) findViewById(R.id.txtNewUser2Title);
			TextView textDescription = (TextView) findViewById(R.id.txtNewUser2Description);
			TextView textWorking = (TextView) findViewById(R.id.txtWorking);
			pBar = (ProgressBar) findViewById(R.id.progressBar);

			textTitle.setText(getString(R.string.noInternet));
			textDescription.setText(getString(R.string.noInternetDescription));
			textWorking.setVisibility(TextView.INVISIBLE);
			pBar.setVisibility(ProgressBar.INVISIBLE);
		}

	}

	protected class StartUpdate extends AsyncTask<Boolean, String, String>
	{
		@Override
		protected String doInBackground(Boolean... params) {

			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Update.this);
			boolean ignoreTV = settings.getBoolean("prefsIgnoreTVShows", true);
			boolean ignoreSmallFiles = settings.getBoolean("prefsIgnoreSmallerFiles", true);

			boolean addMovie;

			videoFiles.clear();
			existingMovies.clear();
			results.clear();

			cursor = dbHelper.fetchAllMovies("ASC"); // Query database to return all movies to a cursor
			while (cursor.moveToNext()) { // Add all movies in cursor to ArrayList of all existing movies
				existingMovies.add(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)));
			}
			cursor.close(); // Close cursor
			cursor.deactivate(); // Deactivate cursor, test if this fixes the ConcurrentModificationExceptions
			dbHelper.close(); // Close database

			for (String directory : storageDirectories) {
				File storageDirectory = new File(directory);
				if (storageDirectory.exists()) {
					checkAllDirsAndFiles(storageDirectory);
				}
			}

			for (File fil : results) {
				if (checkFileTypes(fil.toString())) {
					addMovie = true;

					if (checkBox.isChecked()) {
						check = true;
					} else {
						check = false;
					}

					if (!check) {
						for (String existingMovie : existingMovies) {
							if (existingMovie.substring(existingMovie.lastIndexOf("/") + 1, existingMovie.length()).equals(fil.getAbsoluteFile().
									toString().substring(fil.getAbsoluteFile().toString().lastIndexOf("/") + 1, fil.getAbsoluteFile().toString().length()))) {
								addMovie = false;
							}
						}
					}

					for (String video : videoFiles) {		
						File videoFile = new File(video);
						String videoFileName = videoFile.getName().substring(0, videoFile.getName().lastIndexOf("."));
						String filName = fil.getName().substring(0, fil.getName().lastIndexOf("."));
						if (filName.equals(videoFileName)) {
							addMovie = false;
						}
					}

					if (ignoreTV) {
						if (fil.toString().toLowerCase().matches(".*s[0-9][0-9].*e[0-9][0-9].*") ||
								fil.toString().toLowerCase().matches(".*[0-9].*x[0-9][0-9].*")) {
							addMovie = false;
						}
					}

					if (ignoreSmallFiles) {
						if (fil.length() < 52428800) {
							// If file size is smaller than 50 MB (52428800 bytes)
							addMovie = false;
						}
					}

					if (addMovie) {
						videoFiles.add(fil.toString());
					}
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) 
		{
			try {
				dialog.dismiss();
			} catch (IllegalArgumentException e) {
				Toast.makeText(Update.this, getString(R.string.errorOccured) + ":\n" + e, Toast.LENGTH_LONG).show();
			}

			tmdbMovies = new ArrayList<Tmdb>();
			tmdbMovies.clear();
			
			// Change the view to the second part of the setup guide
			setContentView(R.layout.update2);

			// Set up TextView and ProgressBar variables
			txtWorking = (TextView) findViewById(R.id.txtWorking);
			TextView textTitle = (TextView) findViewById(R.id.txtNewUser2Title);
			TextView textDescription = (TextView) findViewById(R.id.txtNewUser2Description);
			TextView textWorking = (TextView) findViewById(R.id.txtWorking);
			pBar = (ProgressBar) findViewById(R.id.progressBar);

			if (videoFiles.size() > 0) {
				textTitle.setText(getString(R.string.newuser2_donesearching));
				textDescription.setText(videoFiles.size() + " " + getString(R.string.newSetupMoviesFound));
				textWorking.setText(getString(R.string.identifyingmovies));
				pBar.setMax(videoFiles.size());
				pBar.setProgress(0);

				if (isOnline()) {

					int MAX_COUNT = 0;

					if (videoFiles.size() < 5) {
						MAX_COUNT = videoFiles.size();
					} else {
						MAX_COUNT = 5;
					}

					for (int i = 0; i < MAX_COUNT; i++) {

						File file = new File(videoFiles.get(i).toString());

						Tmdb tmdbMovie = new Tmdb(Update.this, decryptWarezName(file.getName()), 2);								
						tmdbMovies.add(tmdbMovie);
					}
				}

			} else {
				// If no new videos were found

				// Sets texts and progress bar to match no videos found
				textTitle.setText(getString(R.string.noVideosFound));
				textDescription.setText(getString(R.string.noVideosFoundDescription));
				textWorking.setVisibility(TextView.GONE);
				pBar.setVisibility(ProgressBar.GONE);
				Button button = (Button) findViewById(R.id.button1);
				button.setVisibility(View.VISIBLE);
			}
		}

	}

	public void goBackToMain(View v) {
		finish();
	}

	private void checkAllDirsAndFiles(File dir) {

		if (!dir.getAbsolutePath().toString().contains("DCIM")) {
			if (dir.isDirectory()) {
				try {
					String[] children = dir.list();
					for (int i = 0; i < children.length; i++) {
						checkAllDirsAndFiles(new File(dir, children[i]));
					}
				} catch (NullPointerException e) {
					Log.d("ERROR", "NullPointerException in Update.checkAllDirsAndFiles(): " + e);
				}
			}

			results.add(dir);
		}

	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	public static void startMovieSearch(int counter) {
		if (counter < videoFiles.size()) {
			File file = new File(videoFiles.get(counter).toString());

			Tmdb tmdbMovie = new Tmdb(context, decryptWarezName(file.getName()), 2);								
			tmdbMovies.add(tmdbMovie);
		}
	}

	public static void updateProgress() {

		Log.d("SIZE", "SIZE: " + tmdbMovies.size());

		pBar.setProgress(pBar.getProgress() + 1);
		startMovieSearch(pBar.getProgress() + 4);

		txtWorking.setText(context.getString(R.string.identifyingmovies) + " " + videoFiles.get(pBar.getProgress() - 1).substring(videoFiles.get(pBar.getProgress() - 1).lastIndexOf("/") + 1,
				videoFiles.get(pBar.getProgress() - 1).length()) + " (" + (pBar.getProgress()) + "/" + pBar.getMax() + ")");

		if (pBar.getProgress() == pBar.getMax()) {
			Log.v("STATUS", "Will now try to add the movies to the database and download covers...");
			addMoviesToDb();
		}
	}

	public static void addMoviesToDb() {

		if (check) {
			// Remove all previous entries from the database
			removeMoviesFromDb();
		}

		// Create and open database
		dbHelper = new DbAdapter(context);
		dbHelper.open();

		for (int i = 0; i < pBar.getMax(); i++) {
			dbHelper.createMovie(videoFiles.get(i).toString(), tmdbMovies.get(i).getCover(), tmdbMovies.get(i).getTitle(), tmdbMovies.get(i).getPlot(), tmdbMovies.get(i).getTmdbId(),
					tmdbMovies.get(i).getImdbId(), tmdbMovies.get(i).getRating(), tmdbMovies.get(i).getTagline(), tmdbMovies.get(i).getReleasedate(), tmdbMovies.get(i).getCertification(),
					tmdbMovies.get(i).getRuntime(), tmdbMovies.get(i).getTrailer(), tmdbMovies.get(i).getGenres(), "0", "0"); // "0" = not a favourite / not watched
			Log.v("DB STATUS", tmdbMovies.get(i).getTitle() + " has been added to the database.");
		}

		pBar.setProgress(0);
		txtWorking.setText(context.getString(R.string.stringIdentificationDone));

		int MAX_COUNT = 0;

		if (tmdbMovies.size() < 5) {
			MAX_COUNT = tmdbMovies.size();
		} else {
			MAX_COUNT = 5;
		}

		try {
			for (int i = 0; i < MAX_COUNT; i++) {
				if (tmdbMovies.get(i).getCover().contains("http://")) {
					new downloadImage(tmdbMovies.get(i).getCover(), videoFiles.get(i).toString(), 2);
				} else {
					checkImageDownload();
				}
				System.gc();
			}
		} catch (IndexOutOfBoundsException e) {
			Log.e("ERROR", "Index out of bounds. Error: " + e);
			checkImageDownload(); // Call checkImageDownload to continue even if the download fails
		}

		dbHelper.close();

	}

	public static void startCoverDownload(int counter) {
		if (counter < videoFiles.size()) {
			if (tmdbMovies.get(counter).getCover().contains("http://")) {
				new downloadImage(tmdbMovies.get(counter).getCover(), videoFiles.get(counter).toString(), 2);
			} else {
				checkImageDownload();
			}
		}
	}

	public static void removeMoviesFromDb() {

		// Create and open database
		dbHelper = new DbAdapter(context);
		dbHelper.open();

		// Removes any previous database entries
		dbHelper.deleteAllMovies();

		dbHelper.close();

		// Delete all cover art images in the data directory
		File fileDirectory = new File(Environment.getExternalStorageDirectory().toString() + "/data/com.miz.mizuu/");
		if (fileDirectory.isDirectory()) {
			try {
				String[] children = fileDirectory.list();
				for (int i = 0; i < children.length; i++) {
					new File(fileDirectory, children[i]).delete();
				}
			} catch (NullPointerException e) {
				Log.d("ERROR", "NullPointerException in Update.removeMoviesFromDb() - trying to delete cover art: " + e);
			}
		}
	}

	public static void checkImageDownload() {

		// Increment the counter
		// NOTE: The counter MUST in this method
		count++;

		pBar.setProgress(pBar.getProgress() + 1);
		txtWorking.setText(context.getString(R.string.stringIdentificationDone) + " (" + pBar.getProgress() + "/" + pBar.getMax() + ")");
		startCoverDownload(pBar.getProgress() + 4);

		Log.v("COUNT", "Downloaded " + count + " out of " + pBar.getMax() + " cover arts.");

		if (count == pBar.getMax()) {
			dbHelper.close();
			Intent intent = new Intent(context, Main.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
			((Activity) context).finish();
		}

	}

	private static String decryptWarezName(String input) {

		if (input.length() > 0) {

			try {
				// Set output string as input in lower case
				String output = input.toLowerCase();

				// Remove extension
				output = output.substring(0, output.lastIndexOf("."));

				// Replace . with spaces
				output = output.replace(".", " ");
				output = output.replace(" - ", " ");
				output = output.replace("  ", " ");

				searchPattern = Pattern.compile(".*?[1-2][0-9][0-9][0-9].*?");
				searchMatcher = searchPattern.matcher(output);

				if (searchMatcher.find()) {
					output = output.substring(0, searchMatcher.end());
					output = output.substring(0, output.lastIndexOf(" "));
				}

				// Remove each tag in warezTags from the input
				for (String tag : warezTags) {
					output = output.replace(tag, "");
				}

				final StringBuilder result = new StringBuilder(input.length());
				String[] splitOutput = output.split(" ");

				for (String split : splitOutput) {
					if (split.length() > 0) {
						result.append(Character.toUpperCase(split.charAt(0)));
						result.append(split.substring(1, split.length()) + " ");
					}
				}

				output = result.toString();

				// Removes the space in the end
				output = output.substring(0, output.length() - 1);

				Log.d("decrypter", input + " decrypted to " + output);

				return output;
			} catch (Exception e) {
				Log.d("decrypter", input + " failed decrypting. Error: " + e);
				return input;
			}
		} else {
			return input;
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();
		finish(); // Finishes activity when the Main activity has been started
	}

}