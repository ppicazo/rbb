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

package com.btmura.android.reddit.data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.btmura.android.reddit.activity.FilterAdapter;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.Thing;

public class Urls {

    private static final String BASE_SEARCH_URL = "http://www.reddit.com/search.json?q=";
    private static final String BASE_SUBREDDIT_SEARCH_URL = "http://www.reddit.com/reddits/search.json?q=";

    public static CharSequence commentsUrl(String id) {
        return new StringBuilder("http://www.reddit.com/comments/").append(id).append(".json");
    }

    public static CharSequence permaUrl(Thing thing) {
        return new StringBuilder("http://www.reddit.com").append(thing.permaLink);
    }

    public static CharSequence sidebarUrl(String name) {
        return new StringBuilder("http://www.reddit.com/r/").append(name).append("/about.json");
    }

    public static CharSequence subredditUrl(Subreddit sr, int filter, String more) {
        StringBuilder b = new StringBuilder("http://www.reddit.com/");

        if (!sr.isFrontPage()) {
            b.append("r/").append(sr.name);
        }

        switch (filter) {
            case FilterAdapter.FILTER_HOT:
                break;

            case FilterAdapter.FILTER_NEW:
                b.append("/new");
                break;

            case FilterAdapter.FILTER_CONTROVERSIAL:
                b.append("/controversial");
                break;

            case FilterAdapter.FILTER_TOP:
                b.append("/top");
                break;

            default:
                throw new IllegalArgumentException(Integer.toString(filter));
        }

        b.append("/.json");

        boolean hasSort = filter == FilterAdapter.FILTER_NEW;
        if (hasSort) {
            b.append("?sort=new");
        }
        if (more != null) {
            b.append(hasSort ? "&" : "?").append("count=25&after=").append(more);
        }
        return b;
    }

    public static CharSequence searchUrl(String query, String more) {
        return getSearchUrl(BASE_SEARCH_URL, query, more);
    }

    public static CharSequence subredditSearchUrl(String query, String more) {
        return getSearchUrl(BASE_SUBREDDIT_SEARCH_URL, query, more);
    }

    private static CharSequence getSearchUrl(String base, String query, String more) {
        try {
            StringBuilder b = new StringBuilder(base).append(URLEncoder.encode(query, "UTF-8"));
            if (more != null) {
                b.append("&count=25&after=").append(more);
            }
            return b;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
