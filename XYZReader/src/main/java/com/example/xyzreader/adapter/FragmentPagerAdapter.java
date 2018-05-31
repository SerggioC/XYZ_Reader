package com.example.xyzreader.adapter;

import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.ArticleDetailFragment;

import java.lang.ref.WeakReference;

public class FragmentPagerAdapter extends FragmentStatePagerAdapter {
    private WeakReference<Cursor> weakCursor;

    public FragmentPagerAdapter(Fragment fragment) {
        super(fragment.getChildFragmentManager());
    }

    public void swapCursor(Cursor cursor) {
        weakCursor = new WeakReference<>(cursor);
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        weakCursor.get().moveToPosition(position);
        long id = weakCursor.get().getLong(ArticleLoader.Query._ID);
        return ArticleDetailFragment.newInstance(id);
    }

    @Override
    public int getCount() {
        return (weakCursor.get() != null) ? weakCursor.get().getCount() : 0;
    }
}