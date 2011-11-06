package com.miz.mizuuphones;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class Search extends ListActivity {

	static ArrayList<HashMap<String, ?>> data;
	private ArrayList<String> videoUrls;

	ListView lv;
	EditText editText;
	TextView searchResults;

	private DbAdapter dbHelper;
	private Cursor cursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		
		// Create and open database
		dbHelper = new DbAdapter(this);
		dbHelper.open();

		lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				//Object o = Search.this.getListAdapter().getItem(arg2);
				//String selectedMovie = o.toString().substring(o.toString().lastIndexOf("Title=") + 6, o.toString().length() - 1);

				Cursor cursorRowId = dbHelper.fetchRowId(videoUrls.get(arg2));

				if (cursorRowId.moveToFirst()) {
					Bundle bundle = new Bundle();
					bundle.putInt("rowId", cursorRowId.getInt(0));

					Intent intent = new Intent();
					intent.setClass(Search.this, movieDetails.class);
					intent.putExtras(bundle);

					Search.this.startActivity(intent);

					finish();
				}

			}
		});

		editText = (EditText) findViewById(R.id.editText1);
		editText.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {

				// Clears the list
				displayNoResults();
				
				if (!(s.toString().length() == 0)) {
					addResults(s.toString());
				}

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub

			}

		});
		
		searchResults = (TextView) findViewById(R.id.txtResultsFound);
	}
	
	public void displayNoResults() {
		searchResults.setText("");
		data = new ArrayList<HashMap<String, ?>>();
		SimpleAdapter adapter = new SimpleAdapter(Search.this,
				data,
				R.layout.list_item,
				new String[] {"Title","Genre"},
				new int[] {R.id.textView1, R.id.textView2});
		setListAdapter(adapter);
	}

	public void addResults(String query) {
		// Create Cursor and fetch all movies from the database
		cursor = dbHelper.search(query);

		startManagingCursor(cursor);

		videoUrls = new ArrayList<String>();
		videoUrls.clear();
		
		data = new ArrayList<HashMap<String, ?>>();
		data.clear();

		while (cursor.moveToNext()) {
			HashMap<String, Object> row  = new HashMap<String, Object>();
			row.put("Title", cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE)));
			row.put("Genre", cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_GENRES)));
			data.add(row);
			videoUrls.add(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)));
		}

		if (cursor.getCount() > 0) {
			searchResults.setText(cursor.getCount() + " " + getString(R.string.ResultsFoundFor) + " \'" + query + "\'");
			SimpleAdapter adapter = new SimpleAdapter(this,
					data,
					R.layout.list_item,
					new String[] {"Title","Genre"},
					new int[] {R.id.textView1, R.id.textView2});
			setListAdapter(adapter);
		} else {
			searchResults.setText(getString(R.string.NoResultsFoundFor) + " \'" + query + "\'");
		}

	}
}