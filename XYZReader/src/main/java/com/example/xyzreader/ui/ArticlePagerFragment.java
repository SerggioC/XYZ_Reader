package com.example.xyzreader.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.R;
import com.example.xyzreader.adapter.FragmentPagerAdapter;
import com.example.xyzreader.data.ArticleLoader;

import java.util.List;
import java.util.Map;

public class ArticlePagerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int ALL_ARTICLES_LOADER_ID = 0;

    private ViewPager viewPager;
    private FragmentPagerAdapter fragmentPagerAdapter;
    private ViewPager.OnPageChangeListener onPageChangeListener;
    private Cursor mCursor;
    private long mSelectedItemId;
    private long mStartId;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        //getLoaderManager().initLoader(ALL_ARTICLES_LOADER_ID, null, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.fragment_viewpager, container, false);

        setupViewPager(mRootView);
        prepareEnterSharedElementTransition();

        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        if (savedInstanceState == null) {
            postponeEnterTransition();
            mStartId = MainActivity.currentItemId;
        }

        // Get back to the Article list
//        mUpButton = mRootView.findViewById(R.id.action_up);
//        mUpButton.setOnClickListener(view -> getChildFragmentManager().popBackStack());

        getLoaderManager().initLoader(ALL_ARTICLES_LOADER_ID, null, this);

        return mRootView;
    }

    private void setupViewPager(View mRootView) {
        viewPager = mRootView.findViewById(R.id.pager);
        fragmentPagerAdapter = new FragmentPagerAdapter(this);

        // Transformation animation on page switch
        viewPager.setPageTransformer(true, new ZoomOutPageTransformer());

        onPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                MainActivity.currentPosition = position;

                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                    MainActivity.currentItemId = mCursor.getLong(ArticleLoader.Query._ID);
                    viewPager.setCurrentItem(position, true);
                }
            }

        };

        viewPager.addOnPageChangeListener(onPageChangeListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case ALL_ARTICLES_LOADER_ID:
                return ArticleLoader.newAllArticlesInstance(getActivity());
            default:
                Log.e("Sergio>", " onCreateLoader:\n= " + "Wrong Loader ID Provided: " + loaderId);
                return null;
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        mCursor = cursor;

        // Select the start ID
        mCursor.moveToFirst();
        int position = 0;
        while (cursor.moveToNext()) {
            long itemId = cursor.getLong(ArticleLoader.Query._ID);
            if (itemId == mStartId) {
                position = cursor.getPosition();
                MainActivity.currentPosition = position;
                MainActivity.currentItemId = itemId;
            }
        }
        updateViewPager(cursor, position);
        prepareEnterSharedElementTransition();

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mCursor = null;
        fragmentPagerAdapter.swapCursor(null);
    }


    private void updateViewPager(Cursor cursor, int position) {
        fragmentPagerAdapter.swapCursor(cursor);
        viewPager.setAdapter(fragmentPagerAdapter);
        viewPager.setCurrentItem(position);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        viewPager.removeOnPageChangeListener(onPageChangeListener);
    }

    /**
     * Prepares the shared element transition from and back to the grid fragment.
     */
    private void prepareEnterSharedElementTransition() {
        Transition transition = TransitionInflater.from(getContext()).inflateTransition(R.transition.image_shared_element_transition);
        setSharedElementEnterTransition(transition);

        // A similar mapping is set at the ArticleListFragment with a setExitSharedElementCallback.
        setEnterSharedElementCallback(new SharedElementCallback() {

            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                // Locate the image view at the primary fragment (the ImageFragment that is currently
                // visible). To locate the fragment, call instantiateItem with the selection position.
                // At this stage, the method will simply return the fragment at the position and will
                // not create a new one.
                Fragment currentFragment = (Fragment) viewPager.getAdapter().instantiateItem(viewPager, MainActivity.currentPosition);
                View view = currentFragment.getView();
                if (view == null) {
                    return;
                }

                // Map the first shared element name to the child ImageView.
                sharedElements.put(names.get(0), view.findViewById(R.id.photo));
            }
        });
    }


}
