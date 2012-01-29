package com.miz.mizuuphones;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {

	// Database name
	private static final String DATABASE_NAME = "mizuu_data";

	// Database version
	private static final int DATABASE_VERSION = 2;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table movie (_id INTEGER PRIMARY KEY AUTOINCREMENT, filepath TEXT, " +
	"coverpath TEXT, title TEXT, plot TEXT, tmdbid TEXT," +
	"imdbid TEXT, rating TEXT, tagline TEXT, release TEXT, certification TEXT," +
	"runtime TEXT, trailer TEXT, genres TEXT, favourite INTEGER, watched INTEGER);";

	public DbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Method is called during creation of the database
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	// Method is called during an upgrade of the database, e.g. if you increase
	// the database version
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(DbHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS movie");
		onCreate(database);
	}

}