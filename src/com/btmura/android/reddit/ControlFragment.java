package com.btmura.android.reddit;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

public class ControlFragment extends Fragment implements TopicHolder, ThingHolder {
	
	private static final String TAG = "MainControlFragment";

	private static final String STATE_TOPIC = "topic";
	private static final String STATE_THING = "thing";
	private static final String STATE_TOPIC_POSITION = "topicPosition";
	private static final String STATE_THING_POSITION = "thingPosition";
	
	private Topic topic;
	private Thing thing;
	private int topicPosition;
	private int thingPosition;
	
	public static ControlFragment newInstance(Topic topic, int topicPosition, Thing thing, int thingPosition) {
		ControlFragment frag = new ControlFragment();
		frag.topic = topic;
		frag.thing = thing;
		frag.topicPosition = topicPosition;
		frag.thingPosition = thingPosition;
		return frag;
	}
	
	public Topic getTopic() {
		return topic;
	}
	
	public Thing getThing() {
		return thing;
	}
	
	public int getTopicPosition() {
		return topicPosition;
	}
	
	public int getThingPosition() {
		return thingPosition;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
        	topic = savedInstanceState.getParcelable(STATE_TOPIC);
        	thing = savedInstanceState.getParcelable(STATE_THING);
        	topicPosition = savedInstanceState.getInt(STATE_TOPIC_POSITION);
        	thingPosition = savedInstanceState.getInt(STATE_THING_POSITION);
        }
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_TOPIC, topic);
		outState.putParcelable(STATE_THING, thing);
		outState.putInt(STATE_TOPIC_POSITION, topicPosition);
		outState.putInt(STATE_THING_POSITION, thingPosition);
	}
}