/*
 * Copyright (C) 2012 Brian Muramatsu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.btmura.android.reddit.activity;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.browser.ControlFragment;
import com.btmura.android.reddit.browser.FilterAdapter;
import com.btmura.android.reddit.browser.Subreddit;
import com.btmura.android.reddit.browser.SubredditListFragment;
import com.btmura.android.reddit.browser.SubredditListFragment.OnSubredditSelectedListener;
import com.btmura.android.reddit.browser.Thing;
import com.btmura.android.reddit.browser.ThingListFragment;
import com.btmura.android.reddit.browser.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.browser.ThingPagerAdapter;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;

public class BrowserActivity extends Activity implements
        OnBackStackChangedListener,
        OnItemSelectedListener,        
        OnPageChangeListener,
        OnSubredditSelectedListener,
        OnThingSelectedListener {

    public static final String EXTRA_SUBREDDIT = "subreddit";

    private static final String STATE_LAST_SELECTED_FILTER = "lastSelectedFilter";

    private static final String FRAG_CONTROL = "control";
    private static final String FRAG_SUBREDDIT_LIST = "subredditList";
    private static final String FRAG_THING_LIST = "thingList";

    private static final int NAV_LAYOUT_ORIGINAL = 0;
    private static final int NAV_LAYOUT_SIDENAV = 1;

    private static final int ANIMATION_OPEN_NAV = 0;
    private static final int ANIMATION_CLOSE_NAV = 1;
    private static final int ANIMATION_OPEN_SIDE_NAV = 2;
    private static final int ANIMATION_CLOSE_SIDE_NAV = 3;
    private static final int ANIMATION_EXPAND_NAV = 4;

    private ActionBar bar;

    private Spinner filterSpinner;
    private FilterAdapter filterAdapter;
    private int lastSelectedFilter;

    private View singleContainer;
    private View navContainer;
    private View subredditListContainer;
    private View thingClickAbsorber;
    private ViewPager thingPager;

    private boolean singleChoice;
    private int tlfContainerId;
    private int slfContainerId;

    private int duration;
    private int fullNavWidth;
    private int sideNavWidth;
    private int subredditListWidth;
    private int thingBodyWidth;

    private AnimatorSet openNavAnimator;
    private AnimatorSet closeNavAnimator;
    private AnimatorSet openSideNavAnimator;
    private AnimatorSet closeSideNavAnimator;
    private AnimatorSet expandNavAnimator;

    private boolean insertSlfToBackStack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);

        singleContainer = findViewById(R.id.single_container);
        if (singleContainer != null) {
            initSingleContainer(savedInstanceState);
            return;
        }

        getFragmentManager().addOnBackStackChangedListener(this);

        bar = getActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setCustomView(R.layout.filter_spinner);

        filterAdapter = new FilterAdapter(this);
        filterSpinner = (Spinner) bar.getCustomView();
        filterSpinner.setAdapter(filterAdapter);
        filterSpinner.setOnItemSelectedListener(this);

        thingPager = (ViewPager) findViewById(R.id.thing_pager);
        thingPager.setOnPageChangeListener(this);
        if (singleContainer == null) {
            navContainer = findViewById(R.id.nav_container);
            subredditListContainer = findViewById(R.id.subreddit_list_container);
            thingClickAbsorber = findViewById(R.id.thing_click_absorber);
            if (thingClickAbsorber != null) {
                thingClickAbsorber.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        runAnimation(ANIMATION_CLOSE_SIDE_NAV, null);
                    }
                });
            }
        }

        singleChoice = singleContainer == null;
        if (singleContainer != null) {
            tlfContainerId = slfContainerId = R.id.single_container;
        } else {
            tlfContainerId = R.id.thing_list_container;
            slfContainerId = R.id.subreddit_list_container;
        }

        if (navContainer != null) {
            initNavContainerAnimators();
        }
        initThingBodyWidth();

        insertSlfToBackStack = isSubredditPreview();
        if (savedInstanceState == null) {
            initFragments(getTargetSubreddit());
        }
    }

    private void initNavContainerAnimators() {
        duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        fullNavWidth = getResources().getDisplayMetrics().widthPixels;
        sideNavWidth = fullNavWidth / 2;
        subredditListWidth = getResources().getDimensionPixelSize(R.dimen.subreddit_list_width);

        openNavAnimator = createOpenNavAnimator();
        closeNavAnimator = createCloseNavAnimator();
        openSideNavAnimator = createSideNavAnimator(true);
        closeSideNavAnimator = createSideNavAnimator(false);
        expandNavAnimator = createExpandNavAnimator();
    }

    private void initThingBodyWidth() {
        Resources r = getResources();
        DisplayMetrics dm = r.getDisplayMetrics();
        int padding = r.getDimensionPixelSize(R.dimen.padding);
        if (navContainer != null) {
            thingBodyWidth = dm.widthPixels / 2 - padding * 2;
        } else if (singleContainer == null) {
            int subredditListWidth = r.getDimensionPixelSize(R.dimen.subreddit_list_width);
            thingBodyWidth = dm.widthPixels / 2 - padding * 2 - subredditListWidth;
        }
    }

    private boolean isSubredditPreview() {
        return getIntent().hasExtra(EXTRA_SUBREDDIT);
    }

    private Subreddit getTargetSubreddit() {
        String name = getIntent().getStringExtra(EXTRA_SUBREDDIT);
        return name != null ? Subreddit.newInstance(name) : null;
    }

    private void initSingleContainer(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            SubredditListFragment slf = SubredditListFragment.newInstance(false);
            GlobalMenuFragment gmf = GlobalMenuFragment.newInstance();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(gmf, GlobalMenuFragment.TAG);
            ft.replace(R.id.single_container, slf, SubredditListFragment.TAG);
            ft.commit();            
        }
    }

    private void initFragments(Subreddit sr) {
        refreshActionBar(sr, null, 0);
        refreshContainers(null);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ControlFragment cf = ControlFragment.newInstance(sr, null, -1, lastSelectedFilter);
        ft.add(cf, FRAG_CONTROL);
        if (singleContainer == null || sr == null) {
            ft.replace(slfContainerId, SubredditListFragment.newInstance(singleChoice),
                    FRAG_SUBREDDIT_LIST);
        }
        if (sr != null) {
            ft.replace(tlfContainerId,
                    ThingListFragment.newInstance(sr, lastSelectedFilter, singleChoice),
                    FRAG_THING_LIST);
        }
        ft.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_LAST_SELECTED_FILTER, lastSelectedFilter);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (singleContainer != null) {
            return;
        }

        if (savedInstanceState != null) {
            lastSelectedFilter = savedInstanceState.getInt(STATE_LAST_SELECTED_FILTER);
            updateThingPager(getThing());
            onBackStackChanged();
        }
    }

    public void onItemSelected(AdapterView<?> adapter, View view, int itemPosition, long itemId) {
        lastSelectedFilter = itemPosition;
        if (itemId != getFilter()) {
            selectSubreddit(getSubreddit(), itemPosition);
        }
    }

    public void onSubredditLoaded(Subreddit sr) {
        if (singleContainer == null && !isVisible(FRAG_THING_LIST)) {
            getSubredditListFragment().setSelectedSubreddit(sr);
            selectSubreddit(sr, lastSelectedFilter);
        }
    }

    public void onSubredditSelected(Subreddit sr) {
        if (singleContainer != null) {
            Intent intent = new Intent(this, ThingListActivity.class);
            intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, sr);
            startActivity(intent);
        } else {
            selectSubreddit(sr, lastSelectedFilter);
        }
    }

    private void selectSubreddit(Subreddit sr, int filter) {
        FragmentManager fm = getFragmentManager();
        fm.removeOnBackStackChangedListener(this);
        if (singleContainer != null) {
            // Pop in case the user changed from what's hot to top.
            fm.popBackStackImmediate(FRAG_THING_LIST, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            fm.popBackStackImmediate();
        }
        fm.addOnBackStackChangedListener(this);

        if (singleContainer == null) {
            refreshActionBar(sr, null, filter);
            refreshContainers(null);
        }

        FragmentTransaction ft = fm.beginTransaction();
        ControlFragment controlFrag = ControlFragment.newInstance(sr, null, -1, filter);
        ft.add(controlFrag, FRAG_CONTROL);
        ThingListFragment thingListFrag = ThingListFragment.newInstance(sr, filter, singleChoice);
        ft.replace(tlfContainerId, thingListFrag, FRAG_THING_LIST);
        if (singleContainer != null) {
            ft.addToBackStack(FRAG_THING_LIST);
        } else {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        ft.commit();
    }

    public void onThingSelected(final Thing thing, final int position) {
        if (navContainer != null && isSideNavShowing()) {
            runAnimation(ANIMATION_CLOSE_SIDE_NAV, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animation.removeListener(this);
                    selectThing(thing, position);
                }
            });
        } else {
            selectThing(thing, position);
        }
    }

    private void selectThing(Thing thing, int position) {
        FragmentManager fm = getFragmentManager();
        if (singleContainer == null) {
            fm.removeOnBackStackChangedListener(this);
            fm.popBackStackImmediate();
            fm.addOnBackStackChangedListener(this);
        }

        updateThingPager(thing);

        FragmentTransaction ft = fm.beginTransaction();
        ControlFragment cf = ControlFragment.newInstance(getSubreddit(), thing, position,
                getFilter());
        ft.add(cf, FRAG_CONTROL);

        if (singleContainer != null) {
            ThingListFragment tf = getThingListFragment();
            if (tf != null) {
                ft.remove(tf);
            }
        }

        ft.addToBackStack(null);
        ft.commit();
    }

    private void updateThingPager(Thing thing) {
        if (thing != null) {
            FragmentManager fm = getFragmentManager();
            ThingPagerAdapter adapter = new ThingPagerAdapter(fm, thing);
            thingPager.setAdapter(adapter);
        } else {
            thingPager.setAdapter(null);
        }
    }

    public void onBackStackChanged() {
        Subreddit sr = getSubreddit();
        Thing t = getThing();
        refreshActionBar(sr, t, getFilter());
        refreshCheckedItems();
        refreshContainers(t);
        invalidateOptionsMenu();
    }

    private void refreshActionBar(Subreddit sr, Thing t, int filter) {
        if (t != null) {
            bar.setTitle(t.assureTitle(this).title);
        } else if (sr != null) {
            bar.setTitle(sr.getTitle(this));
        } else {
            bar.setTitle(R.string.app_name);
        }
        bar.setDisplayHomeAsUpEnabled(singleContainer != null && sr != null || t != null
                || getIntent().hasExtra(EXTRA_SUBREDDIT));
    }

    private void refreshCheckedItems() {
        if (isVisible(FRAG_SUBREDDIT_LIST)) {
            getSubredditListFragment().setSelectedSubreddit(getSubreddit());
        }

        if (isVisible(FRAG_THING_LIST)) {
            ControlFragment f = getControlFragment();
            getThingListFragment().setSelectedThing(f.getThing(), f.getThingPosition());
        }
    }

    private void refreshContainers(Thing t) {
        if (navContainer != null) {
            int currVisibility = navContainer.getVisibility();
            int nextVisibility = t == null ? View.VISIBLE : View.GONE;
            if (currVisibility != nextVisibility) {
                if (nextVisibility == View.VISIBLE) {
                    runAnimation(ANIMATION_OPEN_NAV, null);
                } else {
                    runAnimation(ANIMATION_CLOSE_NAV, null);
                }
            } else if (isSideNavShowing()) {
                runAnimation(ANIMATION_EXPAND_NAV, null);
            }
        } else {
            thingPager.setVisibility(t != null ? View.VISIBLE : View.GONE);
            if (t == null) {
                // Avoid nested executePendingTransactions that would occur by
                // doing popBackStack.
                thingPager.post(new Runnable() {
                    public void run() {
                        updateThingPager(null);
                    }
                });
            }
        }

        if (singleContainer != null) {
            singleContainer.setVisibility(t != null ? View.GONE : View.VISIBLE);
        }
    }

    private boolean isVisible(String tag) {
        Fragment f = getFragmentManager().findFragmentByTag(tag);
        return f != null && f.isAdded();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    public void onPageSelected(int position) {
        invalidateOptionsMenu();
    }

    private void handleHome() {
        FragmentManager fm = getFragmentManager();
        int count = fm.getBackStackEntryCount();
        if (count > 0) {
            if (navContainer != null && !isSideNavShowing()) {
                runAnimation(ANIMATION_OPEN_SIDE_NAV, null);
            } else {
                fm.popBackStack();
            }
        } else if (singleContainer != null && insertSlfToBackStack) {
            insertSlfToBackStack = false;
            initFragments(null);
        } else {
            finish();
        }
    }

    private void runAnimation(int type, AnimatorListener listener) {
        navContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        navContainer.setVisibility(View.VISIBLE);
        thingPager.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        thingPager.setVisibility(View.VISIBLE);
        AnimatorSet as = getAnimator(type);
        if (listener != null) {
            as.addListener(listener);
        }
        as.start();
    }

    private AnimatorSet getAnimator(int type) {
        switch (type) {
            case ANIMATION_OPEN_NAV:
                return openNavAnimator;
            case ANIMATION_CLOSE_NAV:
                return closeNavAnimator;
            case ANIMATION_OPEN_SIDE_NAV:
                return openSideNavAnimator;
            case ANIMATION_CLOSE_SIDE_NAV:
                return closeSideNavAnimator;
            case ANIMATION_EXPAND_NAV:
                return expandNavAnimator;
            default:
                throw new IllegalArgumentException();
        }
    }

    private AnimatorSet createOpenNavAnimator() {
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX",
                -fullNavWidth, 0).setDuration(duration);
        ObjectAnimator tpTransX = ObjectAnimator.ofFloat(thingPager, "translationX", 0,
                fullNavWidth).setDuration(duration);

        AnimatorSet as = new AnimatorSet();
        as.play(ncTransX).with(tpTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                changeNavContainerLayout(NAV_LAYOUT_ORIGINAL);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setVisibility(View.GONE);
                updateThingPager(null);
            }
        });
        return as;
    }

    private AnimatorSet createCloseNavAnimator() {
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", 0,
                -subredditListWidth).setDuration(duration);

        AnimatorSet as = new AnimatorSet();
        as.play(ncTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                thingPager.setVisibility(View.GONE);
                thingPager.setTranslationX(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                navContainer.setVisibility(View.GONE);
                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setVisibility(View.VISIBLE);
            }
        });
        return as;
    }

    private AnimatorSet createSideNavAnimator(final boolean open) {
        ObjectAnimator ncTransX;
        ObjectAnimator tpTransX;
        if (open) {
            ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", -sideNavWidth, 0);
            tpTransX = ObjectAnimator.ofFloat(thingPager, "translationX", 0, sideNavWidth);
        } else {
            ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", 0, -sideNavWidth);
            tpTransX = ObjectAnimator.ofFloat(thingPager, "translationX", sideNavWidth, 0);
        }
        ncTransX.setDuration(duration);
        tpTransX.setDuration(duration);

        AnimatorSet as = new AnimatorSet();
        as.play(ncTransX).with(tpTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                changeNavContainerLayout(NAV_LAYOUT_SIDENAV);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                if (!open) {
                    navContainer.setVisibility(View.GONE);
                }
            }
        });
        return as;
    }

    private AnimatorSet createExpandNavAnimator() {
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", 0,
                subredditListWidth);
        ObjectAnimator tpTransX = ObjectAnimator.ofFloat(thingPager, "translationX", sideNavWidth,
                fullNavWidth);
        ncTransX.setDuration(duration);
        tpTransX.setDuration(duration);

        AnimatorSet as = new AnimatorSet();
        as.play(ncTransX).with(tpTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                changeNavContainerLayout(NAV_LAYOUT_ORIGINAL);
                navContainer.setTranslationX(0);

                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setVisibility(View.GONE);
                updateThingPager(null);
            }
        });
        return as;
    }

    private boolean isSideNavShowing() {
        return thingClickAbsorber.isShown();
    }

    private void changeNavContainerLayout(int layout) {
        int subredditListVisibility;
        int clickAbsorberVisibility;
        switch (layout) {
            case NAV_LAYOUT_ORIGINAL:
                subredditListVisibility = View.VISIBLE;
                clickAbsorberVisibility = View.GONE;
                break;

            case NAV_LAYOUT_SIDENAV:
                subredditListVisibility = View.GONE;
                clickAbsorberVisibility = View.VISIBLE;
                break;

            default:
                throw new IllegalStateException();
        }

        subredditListContainer.setVisibility(subredditListVisibility);
        thingClickAbsorber.setVisibility(clickAbsorberVisibility);
    }

    public int getThingBodyWidth() {
        return thingBodyWidth;
    }

    private Subreddit getSubreddit() {
        return getControlFragment().getSubreddit();
    }

    private Thing getThing() {
        return getControlFragment().getThing();
    }

    private int getFilter() {
        return getControlFragment().getFilter();
    }

    private ControlFragment getControlFragment() {
        return (ControlFragment) getFragmentManager().findFragmentByTag(FRAG_CONTROL);
    }

    private SubredditListFragment getSubredditListFragment() {
        return (SubredditListFragment) getFragmentManager().findFragmentByTag(FRAG_SUBREDDIT_LIST);
    }

    private ThingListFragment getThingListFragment() {
        return (ThingListFragment) getFragmentManager().findFragmentByTag(FRAG_THING_LIST);
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }

    public void onNothingSelected(android.widget.AdapterView<?> adapter) {
    }
}