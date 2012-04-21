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

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.browser.Thing;
import com.btmura.android.reddit.browser.ThingPagerAdapter;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.ThingMenuFragment;

public class ThingActivity extends Activity implements
        ThingMenuFragment.ThingMenuListener,
        ViewPager.OnPageChangeListener {

    public static final String EXTRA_THING = "t";

    private Thing thing;
    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thing_activity);

        thing = getIntent().getParcelableExtra(EXTRA_THING);
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new ThingPagerAdapter(getFragmentManager(), thing));
        pager.setOnPageChangeListener(this);

        ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP
                | ActionBar.DISPLAY_SHOW_TITLE);
        bar.setTitle(thing.assureTitle(this).title);

        if (savedInstanceState == null) {
            GlobalMenuFragment gmf = GlobalMenuFragment.newInstance();
            ThingMenuFragment tmf = ThingMenuFragment.newInstance(thing);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(tmf, ThingMenuFragment.TAG);
            ft.add(gmf, GlobalMenuFragment.TAG);
            ft.commit();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
                getGlobalMenuFragment().handleSearch();
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onLinkSelected() {
        pager.setCurrentItem(0);
    }

    public void onCommentsSelected() {
        pager.setCurrentItem(1);
    }

    public boolean isShowingLink() {
        return ThingPagerAdapter.getType(thing, pager.getCurrentItem()) == ThingPagerAdapter.TYPE_LINK;
    }

    public void onPageSelected(int position) {
        invalidateOptionsMenu();
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
    }

    private GlobalMenuFragment getGlobalMenuFragment() {
        return (GlobalMenuFragment) getFragmentManager().findFragmentByTag(GlobalMenuFragment.TAG);
    }
}