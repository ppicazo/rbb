package com.btmura.android.reddit.addsubreddits;

import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.Provider.Subreddits;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.addsubreddits.SubredditListFragment.OnSelectedListener;

public class AddSubredditsActivity extends Activity implements OnQueryTextListener, OnSelectedListener,
		OnBackStackChangedListener {

	public static final String EXTRA_QUERY = "q";
	
	private static final String FRAG_SUBREDDITS = "s";
	private static final String FRAG_DETAILS = "d";
	
	private static final String STATE_QUERY = "q";
	
	private FragmentManager manager;
	private SearchView sv;
	private View singleContainer;

	private ActionBar bar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_subreddits);
	
		manager = getFragmentManager();
		manager.addOnBackStackChangedListener(this);
		
		bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setDisplayShowTitleEnabled(false);
		bar.setDisplayShowCustomEnabled(true);
		bar.setCustomView(R.layout.subreddits_search);
		
		sv = (SearchView) bar.getCustomView();
		sv.setOnQueryTextListener(this);
		sv.setFocusable(false);
		
		singleContainer = findViewById(R.id.single_container);

		if (savedInstanceState == null) {
			String q = getIntent().getStringExtra(EXTRA_QUERY);
			if (q != null && !q.trim().isEmpty()) {
				sv.setQuery(q.trim(), true);
			}
		}
	}
	
	public boolean onQueryTextSubmit(String query) {
		sv.clearFocus();
		FragmentTransaction ft = manager.beginTransaction();
		if (singleContainer != null) {
			ft.replace(R.id.single_container, SubredditListFragment.newInstance(query, false), FRAG_SUBREDDITS);
		} else {
			ft.replace(R.id.subreddits_container, SubredditListFragment.newInstance(query, true), FRAG_SUBREDDITS);
			Fragment details = getDetailsFragment();
			if (details != null) {
				ft.remove(details);
			}
		}
		ft.commit();
		return true;
	}
	
	public void onSelected(List<SubredditInfo> infos, int position, int event) {
		switch (event) {
		case OnSelectedListener.EVENT_LIST_LOADED:
			handleListLoaded(infos, position);
			break;
			
		case OnSelectedListener.EVENT_LIST_ITEM_CLICKED:
			handleListItemClicked(infos, position);
			break;
			
		case OnSelectedListener.EVENT_ACTION_ITEM_CLICKED:
			handleActionItemClicked(infos);
			break;
			
		default:
			throw new IllegalArgumentException("Unexpected event: " + event);
		}
	}
	
	private void handleListLoaded(List<SubredditInfo> infos, int position) {
		if (singleContainer == null && getDetailsFragment() == null) {
			DetailsFragment frag = replaceDetails(infos.get(0), position, false);
			refreshActionBar(frag);
		}
	}
	
	private void handleListItemClicked(List<SubredditInfo> infos, int position) {
		replaceDetails(infos.get(0), position, true);
	}
	
	private DetailsFragment replaceDetails(SubredditInfo info, int position, boolean addToBackStack) {
		FragmentTransaction ft = manager.beginTransaction();
		int containerId = singleContainer != null ? R.id.single_container : R.id.details_container; 
		DetailsFragment frag = DetailsFragment.newInstance(info, position);
		ft.replace(containerId, frag, FRAG_DETAILS);
		if (addToBackStack) {
			ft.addToBackStack(null);
		}
		ft.commit();	
		return frag;
	}
	
	private void handleActionItemClicked(List<SubredditInfo> infos) {
		int size = infos.size();
		ContentValues[] values = new ContentValues[size];
		for (int i = 0; i < size; i++) {
			values[i] = new ContentValues(1);
			values[i].put(Subreddits.COLUMN_NAME, infos.get(i).displayName);
		}
		
		Provider.addSubredditsInBackground(getApplicationContext(), values);
		Toast.makeText(getApplicationContext(), getString(R.string.subreddits_added, infos.size()), 
				Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			handleHome();
			return true;
			
		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}
	
	private void handleHome() {
		if (singleContainer != null) {
			if (getDetailsFragment() != null) {
				manager.popBackStack();
			} else {
				finish();
			}
		} else {
			finish();
		}
	}
	
	public void onBackStackChanged() {
		Fragment detailsFrag = getDetailsFragment();
		refreshPosition(detailsFrag);
		refreshActionBar(detailsFrag);
	}
	
	private void refreshPosition(Fragment detailsFrag) {
		if (singleContainer == null) {
			int position = detailsFrag.getArguments().getInt(DetailsFragment.ARGS_POSITION);
			getSubredditListFragment().setChosenPosition(position);
		}
	}
	
	private void refreshActionBar(Fragment detailsFrag) {
		if (singleContainer != null) {
			if (detailsFrag != null) {
				bar.setDisplayShowTitleEnabled(true);
				bar.setDisplayShowCustomEnabled(false);
				SubredditInfo info = detailsFrag.getArguments().getParcelable(DetailsFragment.ARGS_SUBREDDIT_INFO);
				bar.setTitle(info.title);
			} else {
				bar.setDisplayShowTitleEnabled(false);
				bar.setDisplayShowCustomEnabled(true);
				bar.setTitle(null);
			}
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(STATE_QUERY, sv.getQuery().toString());
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null) {
			sv.setQuery(savedInstanceState.getString(STATE_QUERY), false);
			refreshActionBar(getDetailsFragment());
		}
	}
	
	private SubredditListFragment getSubredditListFragment() {
		return (SubredditListFragment) manager.findFragmentByTag(FRAG_SUBREDDITS);
	}
	
	private DetailsFragment getDetailsFragment() {
		return (DetailsFragment) manager.findFragmentByTag(FRAG_DETAILS);
	}
	
	public boolean onQueryTextChange(String newText) {
		return false;
	}
}