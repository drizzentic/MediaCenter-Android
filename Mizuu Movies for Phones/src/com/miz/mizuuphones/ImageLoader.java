package com.miz.mizuuphones;

import java.util.Collections;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.widget.ImageView;

public class ImageLoader {

	private Map<ImageView, String> imageViews=Collections.synchronizedMap(new WeakHashMap<ImageView, String>());

	public ImageLoader() {
		photoLoaderThread.setPriority(Thread.NORM_PRIORITY-1);
	}

	public void DisplayImage(String fileUrl, Activity activity, ImageView imageView, final int pos) {
		imageViews.put(imageView, fileUrl);
		queuePhoto(fileUrl, activity, imageView, pos);
		imageView.setImageResource(R.drawable.noposter);
	}

	private void queuePhoto(String url, Activity activity, ImageView imageView, int position) {
		//This ImageView may be used for other images before. So there may be some old tasks in the queue. We need to discard them.
		photosQueue.Clean(imageView);
		PhotoToLoad p=new PhotoToLoad(url, imageView, position);
		synchronized(photosQueue.photosToLoad){
			photosQueue.photosToLoad.push(p);
			photosQueue.photosToLoad.notifyAll();
		}

		//start thread if it's not started yet
		if(photoLoaderThread.getState()==Thread.State.NEW)
			photoLoaderThread.start();
	}

	private Bitmap getBitmap(String fileUrl, int position) {
		
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Config.RGB_565;
		
		Bitmap bm = null;

		try {
			if (!fileUrl.contains("NOCOVERIMG")) {
				if (!fileUrl.contains("com.miz.mizuu")) {
					// If it's not an automatically downloaded cover
					bm = BitmapFactory.decodeFile(fileUrl);
					bm = Bitmap.createScaledBitmap(bm, 225, 330, true);
				} else {
					bm = BitmapFactory.decodeFile(fileUrl, options);
					bm = Bitmap.createBitmap(bm, 0, 0, 225, 330);
				}
			} else {
				bm = Main.nocover;
				if (Main.videoUrls.size() > 0) {
					bm = bm.copy(Config.RGB_565, true);
					Canvas c = new Canvas(bm);
					Paint paint = new Paint();
					paint.setAntiAlias(true);
					paint.setColor(Color.BLACK); 
					paint.setTextSize(24);
					paint.setTypeface(Typeface.DEFAULT);
					c.drawText(Main.videoUrls.get(position).substring(Main.videoUrls.get(position).lastIndexOf("/") + 1, Main.videoUrls.get(position).lastIndexOf(".")), 5, bm.getHeight() - 5, paint);
					c = null;
				}
			}
		} catch(NullPointerException e) {
			bm = Main.nocover;
			if (Main.videoUrls.size() > 0) {
				bm = bm.copy(Config.RGB_565, true);
				Canvas c = new Canvas(bm);
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(Color.BLACK); 
				paint.setTextSize(24);
				paint.setTypeface(Typeface.DEFAULT);
				c.drawText(Main.videoUrls.get(position).substring(Main.videoUrls.get(position).lastIndexOf("/") + 1, Main.videoUrls.get(position).lastIndexOf(".")), 5, bm.getHeight() - 5, paint);
				c = null;
			}
		}
		
		return bm;
	}

	//Task for the queue
	private class PhotoToLoad
	{
		public String url;
		public ImageView imageView;
		public int pos;
		public PhotoToLoad(String u, ImageView i, int p){
			url=u;
			imageView=i;
			pos = p;
		}
	}

	PhotosQueue photosQueue=new PhotosQueue();

	public void stopThread()
	{
		photoLoaderThread.interrupt();
	}

	//stores list of photos to download
	class PhotosQueue
	{
		private Stack<PhotoToLoad> photosToLoad=new Stack<PhotoToLoad>();

		//removes all instances of this ImageView
		public void Clean(ImageView image)
		{
			for(int j=0 ;j<photosToLoad.size();){
				if(photosToLoad.get(j).imageView==image)
					photosToLoad.remove(j);
				else
					++j;
			}
		}
	}

	class PhotosLoader extends Thread {
		public void run() {
			try {
				while(true)
				{
					//thread waits until there are any images to load in the queue
					if(photosQueue.photosToLoad.size()==0)
						synchronized(photosQueue.photosToLoad){
							photosQueue.photosToLoad.wait();
						}
					if(photosQueue.photosToLoad.size()!=0)
					{
						PhotoToLoad photoToLoad;
						synchronized(photosQueue.photosToLoad){
							photoToLoad=photosQueue.photosToLoad.pop();
						}
						Bitmap bmp = getBitmap(photoToLoad.url, photoToLoad.pos);
						String tag=imageViews.get(photoToLoad.imageView);
						
						if(tag!=null && tag.equals(photoToLoad.url)){
							BitmapDisplayer bd=new BitmapDisplayer(bmp, photoToLoad.imageView);
							Activity a=(Activity)photoToLoad.imageView.getContext();
							a.runOnUiThread(bd);
						}
					}
					if(Thread.interrupted())
						break;
				}
			} catch (InterruptedException e) {
				//allow thread to exit
			}
		}
	}

	PhotosLoader photoLoaderThread=new PhotosLoader();

	//Used to display bitmap in the UI thread
	class BitmapDisplayer implements Runnable
	{
		Bitmap bitmap;
		ImageView imageView;
		public BitmapDisplayer(Bitmap b, ImageView i){
			bitmap=b;
			imageView=i;
		}
		public void run()
		{
			if(bitmap!=null)
				imageView.setImageBitmap(bitmap);
			else
				imageView.setImageResource(R.drawable.noposter);
		}
	}

}