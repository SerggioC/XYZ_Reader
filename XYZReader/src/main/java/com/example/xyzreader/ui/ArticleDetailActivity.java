package com.example.xyzreader.ui;

import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.lang.ref.WeakReference;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_CURRENT_ID = "current_id_extra";
    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private ViewPager mPager;
    private FragmentPagerAdapter fragmentPagerAdapter;
    private View mUpButtonContainer;
    private View mUpButton;
    private ViewPager.OnPageChangeListener onPageChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        setContentView(R.layout.activity_article_detail);

        getSupportLoaderManager().initLoader(0, null, this);

        mPager = findViewById(R.id.pager);

        onPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                    mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                    mPager.setCurrentItem(position, true);
                    updateUpButtonPosition();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(300);
            }
        };

        mPager.addOnPageChangeListener(onPageChangeListener);


        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int marginPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics);
        mPager.setPageMargin(marginPixels);

        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));


        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(view -> onSupportNavigateUp());

        mUpButtonContainer = findViewById(R.id.up_container);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mUpButtonContainer.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                view.onApplyWindowInsets(windowInsets);
                mTopInset = windowInsets.getSystemWindowInsetTop();
                mUpButtonContainer.setTranslationY(mTopInset);
                updateUpButtonPosition();
                return windowInsets;
            });
        }

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        } else {
            mSelectedItemId = savedInstanceState.getLong(EXTRA_CURRENT_ID);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_CURRENT_ID, mSelectedItemId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPager.removeOnPageChangeListener(onPageChangeListener);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, final Cursor cursor) {

        mCursor = cursor;

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            while (cursor.moveToNext()) {
                if (cursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    int position = cursor.getPosition();

                    fragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager());
                    fragmentPagerAdapter.swapCursor(cursor);
                    fragmentPagerAdapter.notifyDataSetChanged();
                    mPager.setAdapter(fragmentPagerAdapter);
                    mPager.setCurrentItem(position, true);

                    break;
                }
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        fragmentPagerAdapter.swapCursor(null);
        fragmentPagerAdapter.notifyDataSetChanged();
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }


    private class FragmentPagerAdapter extends FragmentStatePagerAdapter {
        private WeakReference<Cursor> weakCursor;

        public FragmentPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        public void swapCursor(Cursor cursor) {
            weakCursor = new WeakReference<>(cursor);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;
            if (fragment != null) {
                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
                updateUpButtonPosition();
            }
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
}
