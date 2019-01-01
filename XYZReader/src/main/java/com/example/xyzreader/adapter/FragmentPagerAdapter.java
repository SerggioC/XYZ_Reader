package com.example.xyzreader.adapter;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.ViewGroup;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.ArticleDetailFragment;

import java.lang.ref.WeakReference;

public class FragmentPagerAdapter extends android.support.v4.app.FragmentPagerAdapter {
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

    @NonNull
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        return super.instantiateItem(container, position);
    }

    @Override
    public int getCount() {
        return (weakCursor != null) ? weakCursor.get().getCount() : 0;
    }
}