package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import com.btmura.android.reddit.JsonParser.JsonParseListener;
import com.google.gson.stream.JsonReader;

public class ThingLoaderTask extends AsyncTask<Topic, Void, List<Thing>> implements JsonParseListener {
	
	private static final String TAG = "ThingLoaderTask";

	private final TaskListener<List<Thing>> listener;
	private final List<Thing> things = new ArrayList<Thing>();
	
	private String id;
	private String title;
	private String url;
	private boolean isSelf;
	
	public ThingLoaderTask(TaskListener<List<Thing>> listener) {
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}
	
	@Override
	protected void onPostExecute(List<Thing> things) {
		listener.onPostExecute(things);
	}

	@Override
	protected List<Thing> doInBackground(Topic... topics) {
		try {
			URL url = new URL(topics[0].getUrl());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			new JsonParser(this).parseListing(reader);
			stream.close();
			
			connection.disconnect();
			
			return things;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return null;
	}

	public void onId(String id) {
		this.id = id;
	}
	
	public void onTitle(String title) {
		this.title = title;
	}
	
	public void onUrl(String url) {	
		this.url = url;
	}
	
	public void onIsSelf(boolean isSelf) {
		this.isSelf = isSelf;
	}
	
	public void onDataEnd() {
		if (id != null && title != null && url != null) {
			things.add(new Thing(id, Html.fromHtml(title).toString(), url, isSelf));
		}
		id = title = url = null;
	}
	
	public void onDataStart(int nesting) {
	}
	
	public void onSelfText(String text) {
	}
	
	public void onBody(String body) {
	}
}
