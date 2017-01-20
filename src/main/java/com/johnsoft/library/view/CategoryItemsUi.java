/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.library.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * file name: CategoryItemsUi.java
 * @author John Kenrinus Lee
 * @version 2017-01-06
 */
public final class CategoryItemsUi {
    private static final int PRIMARY_COLOR = 0xFF3F51B5;

    private CategoryItemsUi() {
    }

    public static <T> AlertDialog getCategoryItemsDialog(Context context,
                                                         String titleText,
                                                         ItemView<T> itemView,
                                                         Map<String, List<T>> categoryItems) {
        final View view = getParentView(context, titleText, itemView, categoryItems);
        final AlertDialog alert = new AlertDialog.Builder(context)
                .setView(view).setCancelable(false).create();
        // noinspection unchecked
        final View cancel = ((HashMap<String, View>) view.getTag()).get("cancel");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.hide();
                alert.dismiss();
            }
        });
        alert.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    alert.hide();
                    alert.dismiss();
                    return true;
                }
                return false;
            }
        });
        return alert;
    }

    @NonNull
    static <T> View getParentView(final Context context,
                                  final String titleText,
                                  final ItemView<T> itemView,
                                  final Map<String, List<T>> categoryItems) {
        final ToggleButton layoutSwitcher = new ToggleButton(context);
        layoutSwitcher.getBackground().setColorFilter(PRIMARY_COLOR, PorterDuff.Mode.DST_IN);
        layoutSwitcher.setTextOff("切换到选项卡显示");
        layoutSwitcher.setTextOn("切换到列表显示");
        layoutSwitcher.setChecked(false);
        layoutSwitcher.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START));

        final ImageButton cancelBtn = new ImageButton(context);
        cancelBtn.setBackgroundColor(Color.TRANSPARENT);
        cancelBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        rlp.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, metrics);
        cancelBtn.setLayoutParams(rlp);

        final TextView title = new TextView(context);
        title.setText(titleText);
        title.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        final FrameLayout header = new FrameLayout(context);
        header.addView(layoutSwitcher);
        header.addView(title);
        header.addView(cancelBtn);
        header.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        HashMap<String, View> tagMap = new HashMap<>();
        linearLayout.setTag(tagMap);
        linearLayout.addView(header);

        toggleLayout(linearLayout, layoutSwitcher.isChecked(), itemView, categoryItems);

        layoutSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleLayout(linearLayout, layoutSwitcher.isChecked(), itemView, categoryItems);
            }
        });

        // noinspection unchecked
        ((HashMap<String, View>) linearLayout.getTag()).put("cancel", cancelBtn);

        return linearLayout;
    }

    static <T> void toggleLayout(LinearLayout linearLayout, boolean checked,
                                 ItemView<T> itemView, Map<String, List<T>> categoryItems) {
        final Context context = linearLayout.getContext();
        // noinspection unchecked
        HashMap<String, View> tagMap = (HashMap<String, View>) linearLayout.getTag();
        if (checked) {
            TabLayout tab = (TabLayout) tagMap.get("tab");
            if (tab == null) {
                tab = new TabLayout(context);
                tab.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                tagMap.put("tab", tab);
            }
            ViewPager pager = (ViewPager) tagMap.get("pager");
            if (pager == null) {
                pager = new ViewPager(context);
                pager.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                tagMap.put("pager", pager);
            }
            assembleTabPager(context, tab, pager, itemView, categoryItems);
            View view = tagMap.get("list");
            if (view != null) {
                linearLayout.removeView(view);
            }
            linearLayout.addView(tab);
            linearLayout.addView(pager);
        } else {
            ExpandableListView listView = (ExpandableListView) tagMap.get("list");
            if (listView == null) {
                listView = new ExpandableListView(context);
                listView.setAdapter(new CategoryItemsListAdapter<>(context, categoryItems, itemView));
                final int size = categoryItems.size();
                for (int i = 0; i < size; ++i) {
                    listView.expandGroup(i);
                }
                listView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                tagMap.put("list", listView);
            }
            View view = tagMap.get("pager");
            if (view != null) {
                linearLayout.removeView(view);
            }
            view = tagMap.get("tab");
            if (view != null) {
                linearLayout.removeView(view);
            }
            linearLayout.addView(listView);
        }
    }

    static <T> void assembleTabPager(Context context, TabLayout tab, ViewPager pager,
                                     ItemView<T> itemView, Map<String, List<T>> categoryItems) {
        CategoryItemsPagerAdapter<T> categoryItemsPagerAdapter = new CategoryItemsPagerAdapter<>(context,
                categoryItems, itemView);
        pager.setCurrentItem(0);
        pager.setOffscreenPageLimit(2);
        pager.setAdapter(categoryItemsPagerAdapter);

        tab.setTabMode(TabLayout.MODE_FIXED);
        for (String key : categoryItems.keySet()) {
            tab.addTab(tab.newTab().setText(key));
        }
        tab.setBackgroundColor(PRIMARY_COLOR);
        tab.setTabTextColors(Color.LTGRAY, Color.WHITE);
        tab.setupWithViewPager(pager);
    }

    static final class CategoryItemsPagerAdapter<T> extends PagerAdapter {
        private final ArrayList<String> titleList = new ArrayList<>();
        private final ArrayList<WeakReference<ListView>> listViewList = new ArrayList<>();

        public CategoryItemsPagerAdapter(Context context, Map<String, List<T>> categoryItems, ItemView<T> itemView) {
            for (String key : categoryItems.keySet()) {
                titleList.add(key);
                ListView listView = new ListView(context);
                listView.setAdapter(new ItemsListAdapter<>(context, categoryItems.get(key), itemView));
                listView.setLayoutParams(new ViewPager.LayoutParams());
                listViewList.add(new WeakReference<>(listView));
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ListView listView = listViewList.get(position).get();
            if (listView == null) {
                return super.instantiateItem(container, position);
            }
            container.addView(listView);
            return listView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return listViewList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titleList.get(position);
        }
    }

    static final class CategoryItemsListAdapter<T> extends BaseExpandableListAdapter {
        private final ArrayList<List<T>> itemsList = new ArrayList<>();
        private final ArrayList<String> categoryNames = new ArrayList<>();
        private final WeakReference<Context> contextRef;
        private final ItemView<T> itemView;

        public CategoryItemsListAdapter(Context context, Map<String, List<T>> categoryItems, ItemView<T> itemView) {
            contextRef = new WeakReference<>(context);
            for (String key : categoryItems.keySet()) {
                categoryNames.add(key);
                itemsList.add(categoryItems.get(key));
            }
            this.itemView = itemView;
        }

        @Override
        public int getGroupCount() {
            return itemsList.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return getGroup(groupPosition).size();
        }

        @Override
        public List<T> getGroup(int groupPosition) {
            return itemsList.get(groupPosition);
        }

        @Override
        public T getChild(int groupPosition, int childPosition) {
            return getGroup(groupPosition).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            Context context = contextRef.get();
            if (context == null) {
                return null;
            }
            TextView textView;
            if (convertView == null) {
                convertView = new TextView(context);
                textView = (TextView) convertView;
                textView.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                textView.setBackgroundColor(PRIMARY_COLOR);
                textView.setTextColor(Color.WHITE);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20.0f);
                DisplayMetrics metrics = context.getResources()
                        .getDisplayMetrics();
                int w = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics);
                int h = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
                textView.setPadding(w, h, w, h);
                textView.getPaint().setFakeBoldText(true);
            } else {
                textView = (TextView) convertView;
            }
            textView.setText(categoryNames.get(groupPosition));
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                                 ViewGroup parent) {
            Context context = contextRef.get();
            if (context == null) {
                return null;
            }
            T value = getChild(groupPosition, childPosition);
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(itemView.getItemViewLayoutRes(), null);
            }
            itemView.assembleItemView(convertView, value);
            return convertView;
        }
    }

    static final class ItemsListAdapter<T> extends BaseAdapter {
        private final List<T> items;
        private final WeakReference<Context> contextRef;
        private final ItemView<T> itemView;

        public ItemsListAdapter(Context context, List<T> items, ItemView<T> itemView) {
            contextRef = new WeakReference<>(context);
            this.items = items;
            this.itemView = itemView;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public T getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context context = contextRef.get();
            if (context == null) {
                return null;
            }
            T item = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(itemView.getItemViewLayoutRes(), null);
            }
            itemView.assembleItemView(convertView, item);
            return convertView;
        }
    }

    public interface ItemView<T> {
        int getItemViewLayoutRes();
        void assembleItemView(View convertView, T t);
    }
}
