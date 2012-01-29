package com.miz.mizuuphones;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.util.Log;

public class Tmdb {

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

	// Instance fields
	public Context context = null;			// Context used to get strings
	public String movieNameQuery = null;	// The movie name to search for
	public int queryType = 0;				// Which kind of searc this is created in: 0 = nothing, 1 = setup, 2 = update

	// TMDb API KEY
	private String API_KEY = "INSERT_YOUR_OWN_TMDB_API_KEY_HERE";

	// NOTE: Be sure to check for an active internet connection before creating instance
	public Tmdb(Context cxt, String moviename, int querytype) {

		// Initialize local fields with data from the new instance
		context = cxt;
		movieNameQuery = moviename.replaceAll(" ", "+");
		queryType = querytype;

		// TMDb movie data fields
		TITLE = context.getString(R.string.stringNoTitle);		// Movie title
		COVER = "NOCOVERIMG";									// Movie cover path
		PLOT = context.getString(R.string.stringNoPlot);		// Movie plot
		IMDBID = "";											// Movie IMDb ID
		RATING = "0.0";											// Movie rating
		TAGLINE = context.getString(R.string.stringNA);			// Movie tagline
		RELEASEDATE = context.getString(R.string.stringNA);		// Movie releasedate
		CERTIFICATION = context.getString(R.string.stringNA);	// Movie certification (i.e. PG-13)
		RUNTIME = "0";											// Movie runtime
		TRAILER = context.getString(R.string.stringNA);			// Movie trailer

		// Initialize a new StartSearch AsyncTask and execute it
		StartSearch startSearch = new StartSearch();
		startSearch.execute(movieNameQuery);

	}

	protected class StartSearch extends AsyncTask<String, String, String>
	{

		@Override
		protected String doInBackground(String... params) {

			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse("http://api.themoviedb.org/2.1/Movie.search/en/xml/" + API_KEY + "/" + params[0]);
				doc.getDocumentElement().normalize();
				NodeList nodeList = doc.getElementsByTagName("movie");
				if (nodeList.getLength() > 0) {
					Node firstNode = nodeList.item(0);
					if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
						Element firstElement = (Element) firstNode;
						try {
							// Get the TMDd ID of the first search result
							NodeList list = firstElement.getElementsByTagName("id");
							Element element = (Element) list.item(0);
							NodeList tag = element.getChildNodes();
							TMDBID = ((Node) tag.item(0)).getNodeValue();
						} catch(Exception e) {
							// No search results found - return invalid TMDBID
							TMDBID = "invalid";
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return TMDBID;
		}

		@Override
		protected void onPostExecute(String result) 
		{
			GetMovieDetails getMovieDetails = new GetMovieDetails();
			getMovieDetails.execute(result);
		}
	}

	protected class GetMovieDetails extends AsyncTask<String, String, String>
	{

		@Override
		protected String doInBackground(String... params) {

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
									if (element.getAttribute("type").equals("poster") && element.getAttribute("size").equals("w342") && !found) {
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
			switch (queryType) {
			case 1:
				Setup.updateProgress();
				break;
			case 2:
				Update.updateProgress();
				break;
			}
		}
	}

	public String getTitle() {
		return TITLE;
	}

	public String getCover() {
		return COVER;
	}

	public String getPlot() {
		return PLOT;
	}

	public String getTmdbId() {
		return TMDBID;
	}

	public String getImdbId() {
		return IMDBID;
	}

	public String getRating() {
		return RATING;
	}

	public String getTagline() {
		return TAGLINE;
	}

	public String getReleasedate() {
		return RELEASEDATE;
	}

	public String getCertification() {
		return CERTIFICATION;
	}

	public String getRuntime() {
		return RUNTIME;
	}

	public String getTrailer() {
		return TRAILER;
	}

	public String getGenres() {
		return GENRES;
	}

}