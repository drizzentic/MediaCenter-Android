package com.miz.mizuuphones;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class IdentifyMovie extends ListActivity {

	/** FIELDS **/

	public String movieName;

	private static DbAdapter dbHelper;
	private static Cursor cursor;

	ArrayList<String> resultater;
	ArrayList<String> resultater_date;
	ArrayList<String> resultater_links;
	ArrayList<String> resultater_pics;
	ArrayList<Bitmap> resultater_pics2;
	ArrayList<String> resultater_plot;

	// TMDb movie data fields
	public String TITLE = null;				// Movie title
	public String COVER = "";				// Movie cover path
	public String PLOT = null;				// Movie plot
	public String TMDBID = null;			// Movie TMDb ID
	public String IMDBID = null;			// Movie IMDb ID
	public String RATING = "0.0";			// Movie rating
	public String TAGLINE = null;			// Movie tagline
	public String RELEASEDATE = null;		// Movie releasedate
	public String CERTIFICATION = null;		// Movie certification (i.e. PG-13)
	public String RUNTIME = "0";			// Movie runtime
	public String TRAILER = null;			// Movie trailer
	public String GENRES = "";				// Movie genres

	public long ROWID;

	public String SELECTED_RESULT = null;

	public HttpClient client;
	public HttpGet request;
	public HttpResponse response;
	public InputStream in;
	public BufferedReader reader;
	public StringBuilder str;
	public String line = null;
	public String htmlContent = null;
	public String htmlContentPlot = null;
	public String searchQuery = null;

	public Pattern searchPattern;
	public Matcher searchMatcher = null;

	ListView lv;
	EditText searchText;
	TextView searchResults;

	static Bundle bundle;
	private static Context context;

	static ArrayList<HashMap<String, ?>> data = new ArrayList<HashMap<String, ?>>();

	// ProgressDialog variabel
	static ProgressDialog dialog;

	private StartSearch startSearch;

	private String[] warezTags = new String[]{"dvdrip", "dvd5", "dvd", "xvid", "divx", "m-480p", "m-576p", "m-720p", "m-864p", "m-900p",
			"m-1080p", "m480p", "m576p", "m720p", "m864p", "m900p", "m1080p", "480p", "576p", "720p", "864p", "900p", "1080p",
			"brrip", "bdrip", "aac", "x264", "bdrip", "bluray", "dts", "screener", "hdtv", "ac3", "repack", "2.1", "5.1",
			"7.1", "h264", "264", "hdrip", "dvdscr", "pal", "ntsc", "proper", "readnfo", "rerip", "vcd", "scvd", "pdtv", "sdtv",
			"tvrip", "extended"};

	// TMDb API KEY
	private String API_KEY = "INSERT_YOUR_OWN_TMDB_API_KEY_HERE";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.identify);

		// Create and open database
		dbHelper = new DbAdapter(this);
		dbHelper.open();

		context = this;

		lv = getListView();
		lv.setTextFilterEnabled(true);

		searchResults = (TextView) findViewById(R.id.txtResultsFound);

		bundle = this.getIntent().getExtras();
		movieName = bundle.getString("fileName");

		// Hack to get the filename instead of the full destination
		movieName = movieName.substring(movieName.lastIndexOf("/") + 1, movieName.length());

		startSearch = new StartSearch();

		searchText = (EditText) findViewById(R.id.editText1);
		searchText.setText(decryptWarezName(movieName));
		searchText.setSelection(searchText.length());

		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				updateMovie(arg2);
			}
		});

		if (isOnline()) {
			searchResults.setText(getString(R.string.searchingFor) + " \'" + searchText.getText().toString().replace("&quot;", "\"") + "\'...");
			startSearch.execute(searchText.getText().toString());
		} else {
			searchResults.setText(getString(R.string.noInternet));
		}
	}
	
	public void searchForMovies(View v) {
		if (isOnline()) {
			if (!(searchText.getText().toString().length() == 0)) {
				startSearch.cancel(true);
				searchResults.setText(getString(R.string.searchingFor) + " \'" + searchText.getText().toString() + "\'...");
				startSearch = new StartSearch();
				startSearch.execute(searchText.getText().toString());
			} else {
				lv.setAdapter(new ArrayAdapter<String>(IdentifyMovie.this, android.R.layout.simple_list_item_1 , new ArrayList<String>()));
				searchResults.setText("");
			}
		} else {
			searchResults.setText(getString(R.string.noInternet));
		}
	}

	protected class StartSearch extends AsyncTask<String, String, String>
	{
		@Override
		protected String doInBackground(String... params) {

			resultater = new ArrayList<String>();
			resultater_date = new ArrayList<String>();
			resultater_plot = new ArrayList<String>();
			resultater_links = new ArrayList<String>();
			resultater_pics = new ArrayList<String>();
			resultater_pics2 = new ArrayList<Bitmap>();

			try {

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();

				if (isCancelled()) {
					return "CANCELLED";
				}

				Document doc = db.parse("http://api.themoviedb.org/2.1/Movie.search/en/xml/" + API_KEY + "/" + params[0].replaceAll(" ", "+"));

				if (isCancelled()) {
					return "CANCELLED";
				}

				doc.getDocumentElement().normalize();
				NodeList nodeList = doc.getElementsByTagName("movie");
				if (nodeList.getLength() > 0) {
					for (int i = 0; i < nodeList.getLength(); i++) {
						Node firstNode = nodeList.item(i);
						if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
							Element firstElement = (Element) firstNode;
							NodeList list;
							Element element;
							NodeList tag;

							try {
								// Get the movie ID
								list = firstElement.getElementsByTagName("id");
								element = (Element) list.item(0);
								tag = element.getChildNodes();
								resultater_links.add(((Node) tag.item(0)).getNodeValue());
							} catch(Exception e) {
								// No search results found, do nothing
								Log.d("exc", e.toString());
							}

							try {
								// Get the movie release date
								list = firstElement.getElementsByTagName("released");
								element = (Element) list.item(0);
								tag = element.getChildNodes();
								resultater_date.add("(" + ((Node) tag.item(0)).getNodeValue().substring(0,4) + ")");
							} catch(Exception e) {
								resultater_date.add(getString(R.string.unknownYear));
							}

							try {
								// Get the movie titles
								list = firstElement.getElementsByTagName("original_name");
								element = (Element) list.item(0);
								tag = element.getChildNodes();
								resultater.add(((Node) tag.item(0)).getNodeValue() + " " + resultater_date.get(i));
							} catch(Exception e) {
								// No search results found, do nothing
								Log.d("exc", e.toString());
							}

							try {
								// Get the movie titles
								list = firstElement.getElementsByTagName("overview");
								element = (Element) list.item(0);
								tag = element.getChildNodes();
								resultater_plot.add(((Node) tag.item(0)).getNodeValue());
							} catch(Exception e) {
								// No search results found, do nothing
								resultater_plot.add(getString(R.string.stringNA));
							}

							list = firstElement.getChildNodes();
							NodeList ndList = doc.getElementsByTagName("images");
							if (ndList.getLength() > 0) {
								try {
									list = firstElement.getElementsByTagName("image");
									boolean found = false;
									for (int i2 = 0; i2 < list.getLength(); i2++) {
										element = (Element) list.item(i2);
										if (element.getAttribute("type").equals("poster") && element.getAttribute("size").equals("thumb") && !found) {
											resultater_pics.add(element.getAttribute("url"));
											try {
												resultater_pics2.add(BitmapFactory.decodeStream(new java.net.URL(element.getAttribute("url")).openStream()));
												//pics.add(new BitmapDrawable(BitmapFactory.decodeStream(new java.net.URL(pics_sources.get(i)).openStream())));
											} catch (Exception e) {
												resultater_pics2.add(BitmapFactory.decodeResource(getResources(), R.drawable.noposter));
												e.printStackTrace();
											}
											found = true;
										}
									}
									if (!found) {
										resultater_pics2.add(BitmapFactory.decodeResource(getResources(), R.drawable.noposter));
									}
								} catch(Exception e) {
									// Element not found
								}
							}
						}
					}
				}
				return "FOUND";
			} catch (Exception e) {
				return "NOTFOUND";
			}
		}

		@Override
		protected void onPostExecute(String result) 
		{
			if (result.equals("FOUND")) {
				// If results were found
				resultater.add(getString(R.string.NoMatchUseFilename));
				resultater_links.add("NOMATCH");
				resultater_plot.add(getString(R.string.NoMatchUseFilenameDescritpion));
				resultater_pics2.add(BitmapFactory.decodeResource(getResources(), R.drawable.noposter));

				for (int i = 0; i < resultater.size(); i++) {
					if (resultater_plot.get(i).length() > 400) {
						resultater_plot.set(i, resultater_plot.get(i).substring(0, 400) + " [...]");
					}
				}

				if (searchText.getText().toString().length() > 0) {
					searchResults.setText(getString(R.string.Found_number) + " " + (resultater.size() - 1) + " " + getString(R.string.ResultsFor) + " \'" + searchText.getText().toString() + "\'");
					ListAdapter listAdapter = new ListAdapter();
					lv.setAdapter(listAdapter);
					//lv.setAdapter(adapter);
				}
			} else {

				resultater = new ArrayList<String>();
				resultater_date = new ArrayList<String>();
				resultater_plot = new ArrayList<String>();
				resultater_links = new ArrayList<String>();
				resultater_pics = new ArrayList<String>();
				resultater_pics2 = new ArrayList<Bitmap>();

				// None found
				resultater.add(getString(R.string.NoMatchUseFilename));
				resultater_links.add("NOMATCH");
				resultater_plot.add(getString(R.string.NoMatchUseFilenameDescritpion));
				resultater_pics2.add(BitmapFactory.decodeResource(getResources(), R.drawable.noposter));

				for (int i = 0; i < resultater.size(); i++) {
					if (resultater_plot.get(i).length() > 320) {
						resultater_plot.set(i, resultater_plot.get(i).replaceAll(System.getProperty("line.separator"), ""));
						resultater_plot.set(i, resultater_plot.get(i).substring(0, 320) + " [...]");
					}
				}

				// If no results were found
				searchResults.setText(getString(R.string.NoResultsFoundFor));
			}
		}
	}

	protected class GetMovieDetails extends AsyncTask<String, String, String>
	{

		@Override
		protected String doInBackground(String... params) {

			TMDBID = params[0];

			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse("http://api.themoviedb.org/2.1/Movie.getInfo/en/xml/" + API_KEY + "/" + params[0]);
				doc.getDocumentElement().normalize();
				NodeList nodeList = doc.getElementsByTagName("movie");
				if (nodeList.getLength() > 0) {
					Node firstNode = nodeList.item(0);
					if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
						Element firstElement = (Element) firstNode;

						// Create reusable variables
						Element element;
						NodeList list;
						NodeList tag;

						// Set the TITLE variable
						try {
							list = firstElement.getElementsByTagName("original_name");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							TITLE = ((Node) tag.item(0)).getNodeValue();
						} catch(Exception e) {
							// Element not found
						}

						// Set the PLOT variable
						try {
							list = firstElement.getElementsByTagName("overview");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							PLOT = ((Node) tag.item(0)).getNodeValue();
						} catch(Exception e) {
							// Element not found
						}

						// Set the IMDBID variable
						try {
							list = firstElement.getElementsByTagName("imdb_id");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							IMDBID = ((Node) tag.item(0)).getNodeValue();
						} catch(Exception e) {
							// Element not found
						}

						// Set the RATING variable
						try {
							list = firstElement.getElementsByTagName("rating");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							RATING = ((Node) tag.item(0)).getNodeValue();
						} catch(Exception e) {
							// Element not found
						}

						// Set the TAGLINE variable
						try {
							list = firstElement.getElementsByTagName("tagline");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							TAGLINE = ((Node) tag.item(0)).getNodeValue();
						} catch(Exception e) {
							// Element not found
						}

						// Set the RELEASEDATE variable
						try {
							list = firstElement.getElementsByTagName("released");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							RELEASEDATE = ((Node) tag.item(0)).getNodeValue();
						} catch(Exception e) {
							// Element not found
						}

						// Set the CERTIFICATION variable
						try {
							list = firstElement.getElementsByTagName("certification");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							CERTIFICATION = ((Node) tag.item(0)).getNodeValue();
						} catch(Exception e) {
							// Element not found
						}

						// Set the RUNTIME variable
						try {
							list = firstElement.getElementsByTagName("runtime");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							RUNTIME = ((Node) tag.item(0)).getNodeValue();
						} catch(Exception e) {
							// Element not found
						}

						// Set the TRAILER variable
						try {
							list = firstElement.getElementsByTagName("trailer");
							element = (Element) list.item(0);
							tag = element.getChildNodes();
							TRAILER = ((Node) tag.item(0)).getNodeValue();
						} catch(Exception e) {
							// Element not found
						}

						// Set the COVER variable
						list = firstElement.getChildNodes();
						nodeList = doc.getElementsByTagName("images");
						if (nodeList.getLength() > 0) {
							try {
								list = firstElement.getElementsByTagName("image");
								boolean found = false;
								for (int i = 0; i < list.getLength(); i++) {
									element = (Element) list.item(i);
									if (element.getAttribute("type").equals("poster") && element.getAttribute("size").equals("mid") && !found) {
										COVER = element.getAttribute("url");
										found = true;
									}
								}
							} catch(Exception e) {
								// Element not found
							}
						}

						// Set the GENRES variable
						list = firstElement.getChildNodes();
						nodeList = doc.getElementsByTagName("categories");
						if (nodeList.getLength() > 0) {
							try {
								list = firstElement.getElementsByTagName("category");
								for (int i = 0; i < list.getLength(); i++) {
									element = (Element) list.item(i);
									if (element.getAttribute("type").equals("genre")) {
										GENRES = GENRES + element.getAttribute("name") + ", ";
									}
								}
								GENRES = GENRES.substring(0, GENRES.length() - 2);
							} catch(Exception e) {
								GENRES = context.getString(R.string.stringNA);
							}
						}
					}
				} else {
					Log.v("NODE", "Error");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) 
		{
			cursor = dbHelper.fetchRowId(bundle.getString("fileName"));

			if (cursor.moveToFirst()) {
				ROWID = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_ROWID));
				if (dbHelper.updateMovie(ROWID, bundle.getString("fileName"), COVER, TITLE, PLOT, TMDBID, IMDBID,
						RATING, TAGLINE, RELEASEDATE, CERTIFICATION, RUNTIME, TRAILER, GENRES, "0", "0")) {
					new downloadImage(COVER, bundle.getString("fileName"), 3);
				}
			} else {
				try {
					dialog.dismiss();
					Toast.makeText(IdentifyMovie.this, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();
				} catch (IllegalArgumentException e) {
					Toast.makeText(IdentifyMovie.this, getString(R.string.errorOccured) + ":\n" + e, Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	private void updateMovie(int id) {
		Log.d("ID", resultater_links.get(id));
		if (!resultater_links.get(id).equals("NOMATCH")) {
			if (isOnline()) {
				dialog = ProgressDialog.show(IdentifyMovie.this, getString(R.string.updatingMovieInfo), getString(R.string.pleaseWait), true);
				GetMovieDetails getMovieDetails = new GetMovieDetails();
				getMovieDetails.execute(resultater_links.get(id));
			} else {
				Toast.makeText(this, getString(R.string.noInternet), Toast.LENGTH_SHORT).show();
			}
		} else {
			dialog = ProgressDialog.show(IdentifyMovie.this, getString(R.string.updatingMovieInfo), getString(R.string.pleaseWait), true);
			cursor = dbHelper.fetchRowId(bundle.getString("fileName"));
			ROWID = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_ROWID));
			cursor.close();
			dbHelper.updateMovie(ROWID, bundle.getString("fileName"), "NOCOVERIMG", movieName, getString(R.string.stringNA), null,
					"", "0.0", getString(R.string.stringNA), getString(R.string.stringNA), getString(R.string.stringNA), "0", "", getString(R.string.stringNA), "0", "0");
			checkImageDownload();
		}

	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	public static void checkImageDownload() {
		try {
			dialog.dismiss();
		} catch (IllegalArgumentException e) {
			Toast.makeText(context, context.getString(R.string.errorOccured) + ":\n" + e, Toast.LENGTH_LONG).show();
		}

		((Activity) context).setResult(2);
		((Activity) context).finish();
	}

	private String decryptWarezName(String input) {

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

	static class ViewHolder {
		TextView textTitle;
		TextView textPlot;
		ImageView cover;
	}

	public class ListAdapter extends BaseAdapter {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		public int getCount() {
			return resultater.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item_pic, null);

				holder = new ViewHolder();
				holder.textTitle = (TextView) convertView.findViewById(R.id.txtListTitle);
				holder.textPlot = (TextView) convertView.findViewById(R.id.txtListPlot);
				holder.cover = (ImageView) convertView.findViewById(R.id.imageView1);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			Log.d("POSITION", position + " " + resultater.size());
			try {
				holder.cover.setImageBitmap(resultater_pics2.get(position));
				holder.textTitle.setText(resultater.get(position));
				holder.textPlot.setText(resultater_plot.get(position));
			} catch (Exception e) {
				
			}


			return convertView;
		}

	}
}