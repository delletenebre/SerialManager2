package kg.delletenebre.serialmanager2.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kg.delletenebre.serialmanager2.R;

public class AppChooserView extends TextInputEditText implements View.OnClickListener {
    private String value = "";
    private String label = "";
    private String shortcutTempLabel = "";


    public AppChooserView(Context context) {
        super(context);
        init();
    }

    public AppChooserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AppChooserView(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setFocusable(false);
        setFocusableInTouchMode(false);
        setKeyListener(null);
        setOnClickListener(this);
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down_black_24dp, 0);
    }


    public void setValue(Intent intent) {
        setValue(intent == null ? "" : intent.toUri(Intent.URI_INTENT_SCHEME));
    }


    public void setValue(String value) {
        this.value = value;
        setLabel(getLabelByValue(getContext(), value));
    }

    public String getValue() {
        return value;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
        setText(this.label);
    }

    public static Intent getIntentValue(String value, Intent defaultIntent) {
        try {
            if (TextUtils.isEmpty(value)) {
                return defaultIntent;
            }

            return Intent.parseUri(value, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException e) {
            return defaultIntent;
        }
    }

    public static String getLabelByValue(Context context, String value) {
        String defStr = context.getString(R.string.app_chooser_no_app);
        if (TextUtils.isEmpty(value)) {
            return defStr;
        }

        Intent intent;
        try {
            intent = Intent.parseUri(value, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException e) {
            return defStr;
        }

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        if (resolveInfos.isEmpty()) {
            return defStr;
        }

        return resolveInfos.get(0).loadLabel(pm).toString();
    }

    public static void dumpIntent(Intent i){
        String LOG_TAG = "DELLE";
        Bundle bundle = i.getExtras();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            Log.e(LOG_TAG,"Dumping Intent start");
            while (it.hasNext()) {
                String key = it.next();
                Log.e(LOG_TAG,"[" + key + "=" + bundle.get(key)+"]");
            }
            Log.e(LOG_TAG,"Dumping Intent end");
        }
    }


    @Override
    public void onClick(View view) {
        FragmentActivity activity = (FragmentActivity) getContext();
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        AppChooserDialogFragment fragment = new AppChooserDialogFragment();
        fragment.setAppChooser(this);
        fragmentManager.beginTransaction().add(fragment, "app_chooser").commit();
    }

    public String getShortcutTempLabel() {
        return shortcutTempLabel;
    }

    public void setShortcutTempLabel(String shortcutTempLabel) {
        this.shortcutTempLabel = shortcutTempLabel;
    }

    public static class AppChooserDialogFragment extends DialogFragment {
        public static int REQUEST_CREATE_SHORTCUT = 1;

        private AppChooserView mAppChooser;

        private ActivityListAdapter mAppsAdapter;
        private ActivityListAdapter mShortcutsAdapter;

        private ListView mAppsList;
        private ListView mShortcutsList;

        public AppChooserDialogFragment() {
        }

        public void setAppChooser(AppChooserView appChooser) {
            mAppChooser = appChooser;
            tryBindLists();
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            tryBindLists();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context layoutContext = new ContextThemeWrapper(getActivity(),
                    android.support.v7.appcompat.R.style.Base_Theme_AppCompat_Dialog);//R.style.cpv_ColorPickerViewStyle);//Chroma_Dialog_Default);//Theme_Dialog);

            LayoutInflater layoutInflater = LayoutInflater.from(layoutContext);
            View rootView = layoutInflater.inflate(R.layout.app_chooser_dialog, null);
            final ViewGroup tabWidget = rootView.findViewById(android.R.id.tabs);
            final ViewPager pager = rootView.findViewById(R.id.pager);
            pager.setPageMargin((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                    getResources().getDisplayMetrics()));

            TabsHelper helper = new TabsHelper(
                    layoutContext, tabWidget, pager);
            helper.addTab(R.string.app_chooser_title_apps, R.id.apps_list);
            helper.addTab(R.string.app_chooser_title_shortcuts, R.id.shortcuts_list);

            // Set up apps
            mAppsList = rootView.findViewById(R.id.apps_list);
            mAppsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> listView, View view,
                                        int position, long itemId) {
                    Intent intent = mAppsAdapter.getIntent(position);
                    if (intent != null) {
                        intent = Intent.makeMainActivity(intent.getComponent());
                    }
                    mAppChooser.setValue(intent);
                    dismiss();
                }
            });

            // Set up shortcuts
            mShortcutsList = rootView.findViewById(R.id.shortcuts_list);
            mShortcutsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> listView, View view,
                                        int position, long itemId) {
                    mAppChooser.setShortcutTempLabel(
                            mShortcutsAdapter.getItem(position).label.toString());
                    startActivityForResult(mShortcutsAdapter.getIntent(position),
                            REQUEST_CREATE_SHORTCUT);
                }
            });

            tryBindLists();

            return new AlertDialog.Builder(getActivity())
                    .setView(rootView)
                    .create();
        }

        private void tryBindLists() {
            if (mAppChooser == null) {
                return;
            }

            if (isAdded() && mAppsAdapter == null && mShortcutsAdapter == null) {
                mAppsAdapter = new ActivityListAdapter(
                        new Intent(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_LAUNCHER),
                        true);
                mShortcutsAdapter = new ActivityListAdapter(
                        new Intent(Intent.ACTION_CREATE_SHORTCUT)
                                .addCategory(Intent.CATEGORY_DEFAULT),
                        false);
            }

            if (mAppsAdapter != null && mAppsList != null
                    && mShortcutsAdapter != null && mShortcutsList != null) {
                mAppsList.setAdapter(mAppsAdapter);
                mShortcutsList.setAdapter(mShortcutsAdapter);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent intent) {
            super.onActivityResult(requestCode, resultCode, intent);
            if (requestCode == REQUEST_CREATE_SHORTCUT && resultCode == Activity.RESULT_OK) {
                dumpIntent(intent);
                mAppChooser.setValue(
                        (Intent) intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT));
                mAppChooser.setLabel(mAppChooser.getShortcutTempLabel() + " > " +
                        intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
                dismiss();
            }
        }

        static class ActivityInfo {
            CharSequence label;
            Drawable icon;
            ComponentName componentName;
        }

        private class ActivityListAdapter extends BaseAdapter {
            private Intent mQueryIntent;
            private PackageManager mPackageManager;
            private List<ActivityInfo> mInfos;
            private boolean mAllowUseDefault;

            private ActivityListAdapter(Intent queryIntent, boolean allowUseDefault) {
                mQueryIntent = queryIntent;
                mPackageManager = getActivity().getPackageManager();
                mAllowUseDefault = allowUseDefault;

                mInfos = new ArrayList<>();
                List<ResolveInfo> resolveInfos =
                        mPackageManager.queryIntentActivities(queryIntent,0);

                for (ResolveInfo ri : resolveInfos) {
                    ActivityInfo ai = new ActivityInfo();
                    ai.icon = ri.loadIcon(mPackageManager);
                    ai.label = ri.loadLabel(mPackageManager);
                    ai.componentName = new ComponentName(ri.activityInfo.packageName,
                            ri.activityInfo.name);
                    mInfos.add(ai);
                }

                Collections.sort(mInfos, new Comparator<ActivityInfo>() {
                    @Override
                    public int compare(ActivityInfo activityInfo, ActivityInfo activityInfo2) {
                        return activityInfo.label.toString().compareTo(
                                activityInfo2.label.toString());
                    }
                });
            }

            @Override
            public int getCount() {
                return mInfos.size() + (mAllowUseDefault ? 1 : 0);
            }

            @Override
            public ActivityInfo getItem(int position) {
                if (mAllowUseDefault && position == 0) {
                    return null;
                }

                return mInfos.get(position - (mAllowUseDefault ? 1 : 0));
            }

            Intent getIntent(int position) {
                if (mAllowUseDefault && position == 0) {
                    return null;
                }

                return new Intent(mQueryIntent)
                        .setComponent(mInfos.get(position - (mAllowUseDefault ? 1 : 0))
                                .componentName);
            }

            @Override
            public long getItemId(int position) {
                if (mAllowUseDefault && position == 0) {
                    return -1;
                }

                return mInfos.get(position - (mAllowUseDefault ? 1 : 0)).componentName.hashCode();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup container) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity())
                            .inflate(R.layout.app_chooser_list_item, container, false);
                }

                if (mAllowUseDefault && position == 0) {
                    ((TextView) convertView.findViewById(android.R.id.text1))
                            .setText(getString(R.string.app_chooser_no_app));
                    ((ImageView) convertView.findViewById(android.R.id.icon))
                            .setImageDrawable(null);
                } else {
                    ActivityInfo ai = mInfos.get(position - (mAllowUseDefault ? 1 : 0));
                    ((TextView) convertView.findViewById(android.R.id.text1))
                            .setText(ai.label);
                    ((ImageView) convertView.findViewById(android.R.id.icon))
                            .setImageDrawable(ai.icon);
                }

                return convertView;
            }
        }
    }

    public static class TabsHelper {
        private Context mContext;
        private ViewGroup mTabContainer;
        private ViewPager mPager;
        private Map<View, Integer> mTabPositions = new HashMap<>();
        private List<Integer> mTabContentIds = new ArrayList<>();

        TabsHelper(Context context, ViewGroup tabContainer, ViewPager pager) {
            mContext = context;
            mTabContainer = tabContainer;
            mPager = pager;

            pager.setAdapter(mAdapter);
            pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    for (int i = 0; i < mTabContainer.getChildCount(); i++) {
                        mTabContainer.getChildAt(i).setSelected(i == position);
                    }
                }
            });
        }

        void addTab(int labelResId, int contentViewId) {
            addTab(mContext.getString(labelResId), contentViewId);
        }

        void addTab(CharSequence label, int contentViewId) {
            View tabView = LayoutInflater.from(mContext)
                    .inflate(R.layout.app_chooser_tab,mTabContainer, false);
            ((TextView) tabView.findViewById(R.id.tab)).setText(label);
            tabView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPager.setCurrentItem(mTabPositions.get(view));
                }
            });
            int position = mTabContentIds.size();
            tabView.setSelected(mPager.getCurrentItem() == position);
            mTabPositions.put(tabView, position);
            mTabContainer.addView(tabView);
            mTabContentIds.add(contentViewId);
            mAdapter.notifyDataSetChanged();
        }

        private PagerAdapter mAdapter = new PagerAdapter() {
            @Override
            public int getCount() {
                return mTabContentIds.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object o) {
                return view == o;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                return mPager.findViewById(mTabContentIds.get(position));
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                // No-op
            }
        };
    }

}
