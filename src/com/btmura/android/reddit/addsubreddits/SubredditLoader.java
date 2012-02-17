package com.btmura.android.reddit.addsubreddits;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.btmura.android.reddit.common.JsonParser;

class SubredditLoader extends AsyncTaskLoader<List<Subreddit>> {
	
	private static final String TAG = "SubredditLoader";
	
	private List<Subreddit> results;

	private String query;
	
	public SubredditLoader(Context context, String query) {
		super(context);
		this.query = query;
	}
	
	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		if (results != null) {
			deliverResult(results);
		} else {
			forceLoad();
		}
	}
	
	@Override
	public List<Subreddit> loadInBackground() {
		try {
			URL subredditUrl = new URL("http://www.reddit.com/reddits/search.json?q=" + URLEncoder.encode(query));
			
			HttpURLConnection connection = (HttpURLConnection) subredditUrl.openConnection();
			connection.connect();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			SearchParser parser = new SearchParser();
			parser.parseListingObject(reader);
			stream.close();
			
			connection.disconnect();
			
			return parser.results;

		} catch (MalformedURLException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		
		return null;
	}
	
	@Override
	protected void onStopLoading() {
		super.onStopLoading();
	}
	
	class SearchParser extends JsonParser {
		
		private List<Subreddit> results = new ArrayList<Subreddit>();
		
		@Override
		public void onEntityStart(int index) {
			results.add(new Subreddit());
		}
		
		@Override
		public void onDisplayName(JsonReader reader, int index) throws IOException {
			results.get(index).displayName = reader.nextString();
		}
		
		@Override
		public void onTitle(JsonReader reader, int index) throws IOException {
			results.get(index).title = reader.nextString();
		}
		
		@Override
		public void onDescription(JsonReader reader, int index) throws IOException {
			results.get(index).description = reader.peek() == JsonToken.NULL ? "" : reader.nextString();
		}
		
		@Override
		public void onEntityEnd(int index) {
		}
	}
}
