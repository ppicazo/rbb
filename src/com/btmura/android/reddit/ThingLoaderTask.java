package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.ThingLoaderTask.ThingLoaderResult;


public class ThingLoaderTask extends AsyncTask<Topic, Void, ThingLoaderResult> {
	
	static class ThingLoaderResult {
		ArrayList<Entity> entities;
		String after;
	}
	
	private static final String TAG = "ThingLoaderTask";

	private final TaskListener<ThingLoaderResult> listener;
	private final boolean includeSubreddit;

	public ThingLoaderTask(TaskListener<ThingLoaderResult> listener, boolean includeSubreddit) {
		this.listener = listener;
		this.includeSubreddit = includeSubreddit;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}
	
	@Override
	protected void onPostExecute(ThingLoaderResult result) {
		listener.onPostExecute(result);
	}

	@Override
	protected ThingLoaderResult doInBackground(Topic... topics) {
		ThingLoaderResult result = new ThingLoaderResult();
		try {
			URL url = new URL(topics[0].getUrl().toString());
			Log.v(TAG, url.toString());
			
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			
			long t1 = SystemClock.currentThreadTimeMillis();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			ThingParser parser = new ThingParser(includeSubreddit);
			parser.parseListingObject(reader);
			stream.close();
			
			long t2 = SystemClock.currentThreadTimeMillis();
			Log.v(TAG, Long.toString(t2 - t1));
			Log.v(TAG, Integer.toString(parser.entities.size()));
			
			connection.disconnect();
			
			result.entities = parser.entities;
			result.after = parser.after;

		} catch (MalformedURLException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return result;
	}
	
	static class ThingParser extends JsonParser {
		
		private final boolean includeSubreddit;
		private final ArrayList<Entity> entities = new ArrayList<Entity>(25);
		private String after;
		
		ThingParser(boolean includeSubreddit) {
			this.includeSubreddit = includeSubreddit;
		}
		
		@Override
		public void onEntityStart(int index) {
			Entity e = new Entity();
			e.type = Entity.TYPE_THING;
			entities.add(e);
		}
		
		@Override
		public void onId(JsonReader reader, int index) throws IOException {
			getEntity(index).name = getString(reader);
		}
		
		@Override
		public void onTitle(JsonReader reader, int index) throws IOException {
			getEntity(index).title = Formatter.format(getString(reader)).toString();
		}
		
		@Override
		public void onSubreddit(JsonReader reader, int index) throws IOException {
			if (includeSubreddit) {
				getEntity(index).subreddit = getString(reader);
			} else {
				reader.skipValue();
			}
		}
		
		@Override
		public void onAuthor(JsonReader reader, int index) throws IOException {
			getEntity(index).author = getString(reader);
		}
		
		@Override
		public void onUrl(JsonReader reader, int index) throws IOException {
			getEntity(index).url = getString(reader);
		}
		
		@Override
		public void onPermaLink(JsonReader reader, int index) throws IOException {
			getEntity(index).permaLink = getString(reader);
		}
		
		@Override
		public void onIsSelf(JsonReader reader, int index) throws IOException {
			getEntity(index).isSelf = reader.nextBoolean();
		}
		
		@Override
		public void onScore(JsonReader reader, int index) throws IOException {
			getEntity(index).score = reader.nextInt();
		}
		
		private Entity getEntity(int index) {
			return entities.get(index);
		}
		
		private static String getString(JsonReader reader) throws IOException {
			return reader.nextString().trim();
		}
		
		@Override
		public void onEntityEnd(int index) {
			Entity e = entities.get(index);
			switch (e.type) {
			case Entity.TYPE_THING:
				e.line1 = e.title;
				e.line2 = getInfo(e);
				break;
			}
		}
		
		private CharSequence getInfo(Entity e) {
			SpannableStringBuilder b = new SpannableStringBuilder();
			if (includeSubreddit) {
				b.append(e.subreddit).append("  ");
			}
			b.append(e.author).append("  ");
			if (e.score > 0) {
				b.append("+");
			}
			return b.append(Integer.toString(e.score));
		}
		
		@Override
		public void onAfter(JsonReader reader) throws IOException {
			after = reader.nextString();
		}
	}
}
