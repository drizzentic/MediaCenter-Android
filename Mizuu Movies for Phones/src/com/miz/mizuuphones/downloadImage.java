package com.miz.mizuuphones;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class downloadImage {

	private String imgUrl = "";
	private String fileNm = "";
	private int queryType = 0;

	private final int TIMEOUT_CONNECTION = 5000; // 5sec
	private final int TIMEOUT_SOCKET = 30000; // 30sec

	public downloadImage(String imageURL, String fileName, int type) {

		imgUrl = imageURL;
		fileNm = fileName;
		queryType = type;
		
		if (imgUrl.length() == 0) {
			imgUrl = "";
		}
		
		if (fileNm.length() == 0) {
			fileNm = "";
		}
		
		final File dataFolder = new File(Environment.getExternalStorageDirectory().toString() + "/data/com.miz.mizuu/");
		dataFolder.mkdirs();
		
		File nomedia = new File(dataFolder, ".nomedia");
		try {
			nomedia.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final File fil = new File(Environment.getExternalStorageDirectory().toString()
				+ "/data/com.miz.mizuu/" + fileNm.substring(fileNm.lastIndexOf("/"), fileNm.lastIndexOf(".")) + ".jpg");

		if (!imgUrl.contains("http://")) {
			switch (queryType) {
			case 1: // initial setup
				Setup.checkImageDownload();
				Log.d("Downloaded image", "NOCOVERIMG");
				break;
			case 2: // update
				Update.checkImageDownload();
				Log.d("Downloaded image", "NOCOVERIMG");
				break;
			case 3: // manual identify
				IdentifyMovie.checkImageDownload();
				Log.d("Downloaded image", "NOCOVERIMG");
			}
		} else {
			if (queryType == 3) { // If the downloadImage instance is created from IdentifyMovie.java, it needs to download the image even if it already exists
				StartDownload startDownload = new StartDownload();
				startDownload.execute(fil.getAbsoluteFile().toString());
			}
			if (!fil.exists()) {
				StartDownload startDownload = new StartDownload();
				startDownload.execute(fil.getAbsoluteFile().toString());
			}
		}

	}

	protected class StartDownload extends AsyncTask<String, String, String>
	{

		@Override
		protected String doInBackground(String... params) {

			final String filepath = params[0];

			try {
				URL url = new URL(imgUrl);

				//Open a connection to that URL.
				URLConnection ucon = url.openConnection();

				//this timeout affects how long it takes for the app to realize there's a connection problem
				ucon.setReadTimeout(TIMEOUT_CONNECTION);
				ucon.setConnectTimeout(TIMEOUT_SOCKET);

				//Define InputStreams to read from the URLConnection.
				// uses 3KB download buffer
				InputStream is = ucon.getInputStream();
				BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
				FileOutputStream outStream = new FileOutputStream(filepath);
				byte[] buff = new byte[5 * 1024];

				//Read bytes (and store them) until there is nothing more to read(-1)
				int len;
				while ((len = inStream.read(buff)) != -1)
				{
					outStream.write(buff,0,len);
				}

				//clean up
				outStream.flush();
				outStream.close();
				inStream.close();

			} catch (Exception e) {
				Log.e("DOWNLOADIMAGE", "Error: " + e);
				return "";
			}
			return filepath;
		}

		@Override
		protected void onPostExecute(String result) 
		{
			
			final String filepath = result;

			try {
				Bitmap bm = BitmapFactory.decodeFile(result);
				bm = Bitmap.createScaledBitmap(bm, 225, 330, true);
				FileOutputStream outputStream = new FileOutputStream(filepath);
				bm.compress(CompressFormat.PNG, 100, outputStream);
				outputStream.flush();
				outputStream.close();
				bm.recycle();
				bm = null;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			switch (queryType) {
			case 1: // initial setup
				Setup.checkImageDownload();
				break;
			case 2: // update
				Update.checkImageDownload();
				break;
			case 3: // manual identify
				IdentifyMovie.checkImageDownload();
				break;
			}

		}
	}

}