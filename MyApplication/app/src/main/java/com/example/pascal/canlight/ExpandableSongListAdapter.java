package com.example.pascal.canlight;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pascal on 16.10.16.
 */
public class ExpandableSongListAdapter extends BaseExpandableListAdapter {
    private static final String TAG = "ExpdblSngListAdptr";

    private Project mProject;
    private List<List<Song>> mSongs;
    private List<String> mGroupNames;
    private int mNoGroupIndex = -1;
    private Context mContext;
    private boolean mCacheIsInvalid = true;
    private String mQuery;

    public void filter(String query) {
        mCacheIsInvalid = true;
        mQuery = query;
        update();
        notifyDataSetChanged();
    }

    private void update() {
        if (mCacheIsInvalid) {
            mNoGroupIndex = -1;
            mSongs.clear();
            mGroupNames.clear();
            List<Song> songsWithoutGroup = new ArrayList<>();
            for (Song s : mProject.getSongs()) {
                if (mQuery == null || s.getName().toLowerCase().contains(mQuery.toLowerCase())) {
                    for (String groupName : s.getGroups()) {
                        final int i = mGroupNames.indexOf(groupName);
                        if (i < 0) {
                            mGroupNames.add(groupName);
                            mSongs.add(new LinkedList<Song>());
                            mSongs.get(mSongs.size() - 1).add(s);
                        } else {
                            mSongs.get(i).add(s);
                        }
                    }
                    if (s.getGroups().isEmpty()) {
                        songsWithoutGroup.add(s);
                    }
                }
            }
            if (!songsWithoutGroup.isEmpty()) {
                mSongs.add(songsWithoutGroup);
                mNoGroupIndex = mGroupNames.size();
                mGroupNames.add(mContext.getString(R.string.no_group_title));
            }
        }
        mCacheIsInvalid = false;
    }

    public void notifyDataSetChanged() {
        mCacheIsInvalid = true;
        super.notifyDataSetInvalidated();
        super.notifyDataSetChanged();
    }

    ExpandableSongListAdapter(Context context, Project project) {
        mProject = project;
        mContext = context;
        mSongs = new ArrayList<>();
        mGroupNames = new ArrayList<>();
    }

    @Override
    public int getGroupCount() {
        update();
        return mGroupNames.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        update();
        return mSongs.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        update();
        return mGroupNames.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        update();
        return mSongs.get(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        update();
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        update();
        long id = 0;
        for (int i = 0; i < groupPosition - 1; ++i) {
            id += mSongs.get(i).size();
        }
        id += childPosition;
        return id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        update();
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.expandable_song_list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTextSize(18);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        update();
        final Song childSong = (Song) getChild(groupPosition, childPosition);
        final String childText = childSong.getName();

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.expandable_song_list_item, null);
        }

        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.lblListItem);

        txtListChild.setText(childText);
        txtListChild.setTextSize(25);
        txtListChild.setPadding(0, 0, 0, 0);
        txtListChild.setBackgroundColor(prettifyColor(childSong.getColor()));
        return convertView;
    }

    public static int prettifyColor(int color) {
        if (color == Color.TRANSPARENT) {
            return color;
        } else {
            // make the color darker.
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] *= 0.5f;
            return Color.HSVToColor(hsv);
        }
    }

    public List<String> getGroupNames() {
        List<String> groupNames = new ArrayList<>();
        groupNames.addAll(mGroupNames);
        if (mNoGroupIndex >= 0) {
            groupNames.remove(mNoGroupIndex);
        }
        return groupNames;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
