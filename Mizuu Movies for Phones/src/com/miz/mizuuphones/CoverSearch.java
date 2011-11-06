package com.miz.mizuuphones;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.miz.mizuuphones.AsyncTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class CoverSearch extends Activity {

	// TMDb API KEY
	private String API_KEY = "INSERT_YOUR_OWN_TMDB_API_KEY_HERE";

	ArrayList<String> pics_sources;
	ArrayList<Drawable> pics;

	private GridView gridview;

	// ProgressDialog variabel
	static ProgressDialog dialog;

	public String imgUrl;
	public String fileNm;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set content view to the main.xml file
		setContentView(R.layout.coverselector);

		gridview = (GridView) findViewById(R.id.gridView1);

		pics_sources = new ArrayList<String>();
		pics = new ArrayList<Drawable>();

		dialog = ProgressDialog.show(CoverSearch.this, getString(R.string.searchingCoversTitle), getString(R.string.searchingCovers), true);
		dialog.setCancelable(true);

		GetCoverImages GetCoverImages = new GetCoverImages();
		GetCoverImages.execute(this.getIntent().getExtras().getString("id"));
	}

	protected class GetCoverImages extends AsyncTask<String, String, String>
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
						NodeList list;
						Element element;

						list = firstElement.getChildNodes();
						nodeList = doc.getElementsByTagName("images");
						if (nodeList.getLength() > 0) {
							try {
								list = firstElement.getElementsByTagName("image");
								for (int i = 0; i < list.getLength(); i++) {
									element = (Element) list.item(i);
									if (element.getAttribute("type").equals("poster") && element.getAttribute("size").equals("cover")) {
										pics_sources.add(element.getAttribute("url"));
									}
								}
							} catch(Exception e) {
								// No such tag
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) 
		{
			GetImage getImage = new GetImage();
			getImage.execute(true);
		}

	}

	protected class GetImage extends AsyncTask<Boolean, String, String> {

		@Override
		protected String doInBackground(Boolean... params) {

			for (int i = 0; i < pics_sources.size(); i++) {
				try {
					pics.add(new BitmapDrawable(BitmapFactory.decodeStream(new java.net.URL(pics_sources.get(i)).openStream())));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) 
		{
			dialog.dismiss();
			gridview.setAdapter(new ImageAdapter(CoverSearch.this));
			gridview.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {

					dialog = ProgressDialog.show(CoverSearch.this, getString(R.string.addingCoverTitle), getString(R.string.addingCover), true);
					dialog.setCancelable(true);

					imgUrl = pics_sources.get(arg2).replace("-cover.jpg", "-mid.jpg");
					fileNm = CoverSearch.this.getIntent().getExtras().getString("filename");

					StartDownload startDownload = new StartDownload();
					startDownload.execute(true);
				}

			});
		}

	}

	public class ImageAdapter extends BaseAdapter {
		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			return pics.size();
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
			
			if (convertView == null) {  // if it's not recycled, initialize some attributes
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(140, 216));
	            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
				imageView.setPadding(5, 5, 5, 5);
			} else {
				imageView = (ImageView) convertView;
			}

			imageView.setImageDrawable(pics.get(position));

			return imageView;
		}

	}

	protected class StartDownload extends AsyncTask<Boolean, String, String>
	{

		@Override
		protected String doInBackground(Boolean... params) {

			try {

				URL url = new URL(imgUrl);

				File temp = new File(fileNm);
				String tempName = temp.getName().substring(0, temp.getName().lastIndexOf("."));

				File fileDirectory = new File(Environment.getExternalStorageDirectory().toString() + "/data/com.miz.mizuu/");
				fileDirectory.mkdirs();

				URLConnection ucon = url.openConnection();

				InputStream is = ucon.getInputStream();

				Bitmap resizedBitmap = BitmapFactory.decodeStream(is);
				resizedBitmap = Bitmap.createScaledBitmap(resizedBitmap, 225, 330, true);

				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				resizedBitmap.compress(Bitmap.CompressFormat.PNG, 80, bytes);

				//you can create a new file name "test.jpg" in sdcard folder.
				File f = new File(fileDirectory + "/" + tempName + ".jpg");
				f.createNewFile();
				
				//write the bytes in file
				FileOutputStream fo = new FileOutputStream(f);
				fo.write(bytes.toByteArray());

			} catch (IOException e) {

				Log.d("Download Image", "Error: " + e);

			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) 
		{
			for (int i = 0; i < pics_sources.size(); i++) {
				dialog.dismiss();
				setResult(1);
				CoverSearch.this.finish();
			}
		}

	}
}