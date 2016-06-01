package com.holenstudio.excamera.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.holenstudio.excamera.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: document your custom view class.
 */
public class CameraParasPopupWindow extends PopupWindow {
    private static final String TAG = "CameraParasPopupWindow";

    private Context mContext;
    private Camera.Parameters mParams;
    private LinkedHashMap<String, String> mMap;
    private ListView leftLv;
    private ListView rightLv;
    private List<String> leftList = new ArrayList<>();
    private List<String> rightList = new ArrayList<>();

    private String mLeftValue;
    private String mRightValue;

    private ArrayAdapter leftListAdapter;
    private RightListAdapter rightListAdapter;
    private OnParameterSelectedListener mSelectedListener;

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (parent.getId()) {
                case R.id.popup_listview_left:
                    mLeftValue = leftList.get(position);
                    reAddRightListDataFromString(mLeftValue);
                    break;
                case R.id.popup_listview_right:
                    dismiss();
                    mRightValue = rightList.get(position);
                    mParams.set(mLeftValue, mRightValue);
                    mMap.put(mLeftValue, mRightValue);
                    if (mSelectedListener != null) {
                        mSelectedListener.selectedParameter(mParams);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public CameraParasPopupWindow(Context context) {
        this(context, null);
    }

    public CameraParasPopupWindow(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        initView();
    }

    private void initView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.view_camera_params_popup_window, null);
        leftLv = (ListView) view.findViewById(R.id.popup_listview_left);
        rightLv = (ListView) view.findViewById(R.id.popup_listview_right);
        setContentView(view);
        setWidth(getScreenWidth() * 7 / 8);
        setHeight(getScreenHeight() * 2 / 3);
        leftLv.setOnItemClickListener(mItemClickListener);
        rightLv.setOnItemClickListener(mItemClickListener);
        leftListAdapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_list_item_1, leftList);
        rightListAdapter = new RightListAdapter();
        initData();
    }

    private void initData() {
        if (mParams == null) {
            return;
        }
        String paramsFlattenString = mParams.flatten();
        mMap = unflatten(paramsFlattenString);
        reAddLeftListDataFromMap(mMap);
        mLeftValue = leftList.get(0);
        reAddRightListDataFromString(mLeftValue);
        leftLv.setAdapter(leftListAdapter);
        rightLv.setAdapter(rightListAdapter);
    }

    public void setCameraParams(Camera.Parameters params) {
        mParams = params;
        initData();
    }

    public void setOnParameterSelectedListener (OnParameterSelectedListener parameterSelectedListener) {
        mSelectedListener = parameterSelectedListener;
    }

    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics metric = new DisplayMetrics();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric;
    }

    private int getScreenWidth() {
        return getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {
        return getDisplayMetrics().heightPixels;
    }

    private LinkedHashMap unflatten(String flattened) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(';');
        splitter.setString(flattened);
        for (String kv : splitter) {
            int pos = kv.indexOf('=');
            if (pos == -1) {
                continue;
            }
            String k = kv.substring(0, pos);
            String v = kv.substring(pos + 1);
            map.put(k, v);
        }
        return map;
    }

    private void reAddLeftListDataFromMap(Map<String, String> map) {
        leftList.clear();
        for (String str : map.keySet()) {
            if (map.keySet().contains(str + "-values") && map.get(str + "-values").contains(",")) {
                leftList.add(str);
            }
        }
        leftListAdapter.notifyDataSetChanged();
    }

    private void reAddRightListDataFromString(String str) {
        rightList.clear();
        if (!str.contains("values")) {
            str = str + "-values";
        }
        String[] data = mMap.get(str).split(",");
        if (data.length <= 1) {
            return;
        }
        for (String tmp : data) {
            rightList.add(tmp);
        }
        rightListAdapter.notifyDataSetChanged();
    }

    class RightListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return rightList == null? 0 : rightList.size();
        }

        @Override
        public Object getItem(int position) {
            return rightList == null? null : rightList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_1, parent, false);
            if (mMap.get(mLeftValue).equals(getItem(position))) {
                view.setBackgroundColor(Color.CYAN);
            } else {
                view.setBackgroundColor(Color.alpha(Color.BLACK));
            }

            Object item = getItem(position);
            if (item instanceof CharSequence) {
                view.setText((CharSequence)item);
            } else {
                view.setText(item.toString());
            }
            return view;
        }
    }

    public interface OnParameterSelectedListener {
        void selectedParameter(Camera.Parameters params);
    }
}
