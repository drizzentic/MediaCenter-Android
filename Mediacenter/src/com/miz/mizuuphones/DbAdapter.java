package com.miz.mizuuphones;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DbAdapter {

	// Database fields
	public static final String KEY_ROWID = "_id";
	public static final String KEY_FILEPATH = "filepath";
	public static final String KEY_COVERPATH = "coverpath";
	public static final String KEY_TITLE = "title";
	public static final String KEY_PLOT = "plot";
	public static final String KEY_TMDBID = "tmdbid";
	public static final String KEY_IMDBID = "imdbid";
	public static final String KEY_RATING = "rating";
	public static final String KEY_TAGLINE = "tagline";
	public static final String KEY_RELEASEDATE = "release";
	public static final String KEY_CERTIFICATION = "certification";
	public static final String KEY_RUNTIME = "runtime";
	public static final String KEY_TRAILER = "trailer";
	public static final String KEY_GENRES = "genres";
	public static final String KEY_FAVOURITE = "favourite";
	public static final String KEY_WATCHED = "watched";

	private static final String DATABASE_TABLE = "movie";

	private Context context;
	private SQLiteDatabase database;
	private DbHelper dbHelper;

	public DbAdapter(Context context) {
		this.context = context;
	}

	public DbAdapter open() throws SQLException {
		dbHelper = new DbHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		dbHelper.close();
	}

	/**
	 * Create a new movie
	 * If the movie is successfully created, return the new
	 * rowId for that movie, otherwise return a -1 to indicate failure.
	 */
	public long createMovie(String filepath, String coverpath, String title,
			String plot, String tmdbid, String imdbid, String rating, String tagline, String release,
			String certification, String runtime, String trailer, String genres, String favourite, String watched) {
		ContentValues initialValues = createContentValues(filepath, coverpath, title, plot, tmdbid, imdbid,
				rating, tagline, release, certification, runtime, trailer, genres, favourite, watched);
		return database.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Update the movie
	 */
	public boolean updateMovie(long rowId, String filepath, String coverpath, String title,
			String plot, String tmdbid, String imdbid, String rating, String tagline, String release,
			String certification, String runtime, String trailer, String genres, String favourite, String watched) {
		ContentValues updateValues = createContentValues(filepath, coverpath, title, plot, tmdbid, imdbid,
				rating, tagline, release, certification, runtime, trailer, genres, favourite, watched);
		return database.update(DATABASE_TABLE, updateValues, KEY_ROWID + "="
				+ rowId, null) > 0;
	}
	
	public boolean updateMovieSingleItem(long rowId, String table, String value) {
		
		ContentValues values = new ContentValues();
		values.put(table, value);
		
		return database.update(DATABASE_TABLE, values, KEY_ROWID + "="
				+ rowId, null) > 0;
	}

	/**
	 * Deletes movie
	 */
	public boolean deleteMovie(long rowId) {
		return database.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteAllMovies() {
		return database.delete(DATABASE_TABLE, null, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all movies in the database
	 */
	public Cursor fetchAllMovies(String sort) {
		return database.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_FILEPATH, KEY_COVERPATH,
				KEY_TITLE, KEY_PLOT, KEY_TMDBID, KEY_IMDBID, KEY_RATING, KEY_TAGLINE, KEY_RELEASEDATE, KEY_CERTIFICATION,
				KEY_RUNTIME, KEY_TRAILER, KEY_GENRES, KEY_FAVOURITE, KEY_WATCHED},
				null, null, null, null, KEY_TITLE + " " + sort);
	}

	/**
	 * Return a Cursor over the list of all favourite movies in the database
	 */
	public Cursor fetchAllFavs(String sort) {
		return database.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_FILEPATH, KEY_COVERPATH,
				KEY_TITLE, KEY_PLOT, KEY_TMDBID, KEY_IMDBID, KEY_RATING, KEY_TAGLINE, KEY_RELEASEDATE, KEY_CERTIFICATION,
				KEY_RUNTIME, KEY_TRAILER, KEY_GENRES, KEY_FAVOURITE, KEY_WATCHED},
				KEY_FAVOURITE + " = \'1\'", null, null, null, KEY_TITLE + " " + sort);
	}

	/**
	 * Return a Cursor positioned at the defined movie
	 */
	public Cursor fetchMovie(long rowId) throws SQLException {
		Cursor mCursor = database.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_FILEPATH, KEY_COVERPATH,
				KEY_TITLE, KEY_PLOT, KEY_TMDBID, KEY_IMDBID, KEY_RATING, KEY_TAGLINE, KEY_RELEASEDATE, KEY_CERTIFICATION,
				KEY_RUNTIME, KEY_TRAILER, KEY_GENRES, KEY_FAVOURITE, KEY_WATCHED}, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public Cursor fetchRowId(String movieUrl) {
		movieUrl = movieUrl.replaceAll("\'", "\'\'");
		Cursor mCursor = database.query(DATABASE_TABLE, new String[] {KEY_ROWID,},
				KEY_FILEPATH + " = \'" + movieUrl + "\'", null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public boolean isMovieInDb(String file) {
		file = file.replaceAll("\'", "\'\'");
		Cursor mCursor = database.query(DATABASE_TABLE, new String[] {KEY_ROWID,},
				"filepath = \'" + file + "\'", null, null, null, null);
		if (mCursor.moveToFirst()) {
			return true;
		} else {
			return false;
		}
	}

	private ContentValues createContentValues(String filepath, String coverpath, String title,
			String plot, String tmdbid, String imdbid, String rating, String tagline, String release,
			String certification, String runtime, String trailer, String genres, String favourite, String watched) {
		ContentValues values = new ContentValues();
		values.put(KEY_FILEPATH, filepath);
		values.put(KEY_COVERPATH, coverpath);
		values.put(KEY_TITLE, title);
		values.put(KEY_PLOT, plot);
		values.put(KEY_TMDBID, tmdbid);
		values.put(KEY_IMDBID, imdbid);
		values.put(KEY_RATING, rating);
		values.put(KEY_TAGLINE, tagline);
		values.put(KEY_RELEASEDATE, release);
		values.put(KEY_CERTIFICATION, certification);
		values.put(KEY_RUNTIME, runtime);
		values.put(KEY_TRAILER, trailer);
		values.put(KEY_GENRES, genres);
		values.put(KEY_FAVOURITE, favourite);
		values.put(KEY_WATCHED, watched);
		return values;
	}

	public Cursor search(String query) {
		String newQuery = query.replace("\'", "");
		Cursor mCursor = database.query(DATABASE_TABLE, new String[] {KEY_TITLE, KEY_GENRES, KEY_FILEPATH}, 
				KEY_TITLE + " like '%" + newQuery + "%'", null, null, null, KEY_TITLE + " ASC");
		return mCursor;
	}

}