package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.adapter.BodyDataAdapter;
import com.example.xyzreader.adapter.FragmentPagerAdapter;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * A fragment representing a single Article detail screen.
 */
public class ArticleDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ITEM_ID = "item_id"; // On creating ViewPager fragments
    public static final String ARTICLE_ITEM_ID = "article_item_ID"; // On selecting article
    public static final String EXTRA_CURRENT_ID = "current_id_extra"; // To bundle

    // Big text data RecyclerView variables
    private static final int CHUNK_SIZE = 10;
    private static final int SINGLE_ARTICLE_LOADER_ID = 0;
    private static final int ALL_ARTICLES_LOADER_ID = 1;
    private int FROM_INDEX = 0;
    private int TO_INDEX = 20;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;

    private View mPhotoContainerView;
    private ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat();     // Use default locale format
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1); // Most time functions can only handle 1902 - 2037
    private BodyDataAdapter bodyDataAdapter;
    private List<String> bodyDataList;
    private int dataListSize;


    private long mSelectedItemId;
    private int mTopInset;


    private ViewPager viewPager;
    private FragmentPagerAdapter fragmentPagerAdapter;
    private View mUpButtonContainer;
    private View mUpButton;
    private ViewPager.OnPageChangeListener onPageChangeListener;

    private long mStartId;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment articleDetailFragment = new ArticleDetailFragment();
        articleDetailFragment.setArguments(arguments);
        return articleDetailFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mStartId = getArguments().containsKey(ARTICLE_ITEM_ID) ? getArguments().getLong(ARTICLE_ITEM_ID) : 0;

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        setHasOptionsMenu(true);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        //getLoaderManager().initLoader(SINGLE_ARTICLE_LOADER_ID, null, this);
        getLoaderManager().initLoader(ALL_ARTICLES_LOADER_ID, null, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_viewpager, container, false);

        mPhotoView = mRootView.findViewById(R.id.photo);
        mPhotoContainerView = mRootView.findViewById(R.id.photo_container);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(view ->
                startActivity(Intent
                        .createChooser(ShareCompat.IntentBuilder.from(getActivity())
                                .setType("text/plain")
                                .setText("Some sample text")
                                .getIntent(), getString(R.string.action_share)))
        );





        viewPager = mRootView.findViewById(R.id.pager);

        viewPager.setPageTransformer(true, new ZoomOutPageTransformer());

        onPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                    mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                    viewPager.setCurrentItem(position, true);
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

        viewPager.addOnPageChangeListener(onPageChangeListener);


        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int marginPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics);
        viewPager.setPageMargin(marginPixels);

        viewPager.setPageMarginDrawable(new ColorDrawable(0x22000000));


        mUpButton = mRootView.findViewById(R.id.action_up);
        mUpButton.setOnClickListener(view -> {
            getFragmentManager().popBackStack();
        });

        mUpButtonContainer = mRootView.findViewById(R.id.up_container);
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
  //          if (getIntent() != null && getIntent().getData() != null) {
  //              mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
 //           }
        } else {
            mSelectedItemId = savedInstanceState.getLong(EXTRA_CURRENT_ID);
        }









        RecyclerView bodyDataRecyclerView = mRootView.findViewById(R.id.article_body_recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mRootView.getContext(), LinearLayoutManager.VERTICAL, false);
        bodyDataRecyclerView.setLayoutManager(linearLayoutManager);
        bodyDataRecyclerView.setHasFixedSize(false);

        NestedScrollView nestedScrollView = mRootView.findViewById(R.id.nested_scrollview);

        // Code to load more data incrementally and reduce start up time.
        nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (scrollView, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if ((scrollY >= (bodyDataRecyclerView.getMeasuredHeight() - scrollView.getMeasuredHeight())) && scrollY > oldScrollY) {
                TO_INDEX += CHUNK_SIZE;
                if (TO_INDEX >= dataListSize) TO_INDEX = dataListSize - 1;
                redrawRecyclerViewSubList();
            }
        });

        bodyDataAdapter = new BodyDataAdapter(mRootView.getContext());
        bodyDataRecyclerView.setAdapter(bodyDataAdapter);

        bindViews();

        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_CURRENT_ID, mSelectedItemId);
    }

    private void redrawRecyclerViewSubList() {
        List<String> subBodyDataList = bodyDataList.subList(FROM_INDEX, TO_INDEX);
        bodyDataAdapter.swapBodyDataList(subBodyDataList);
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e("Sergio> ", ex.getMessage() + "passing today's date");
            return new Date();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case SINGLE_ARTICLE_LOADER_ID:
                return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
            case ALL_ARTICLES_LOADER_ID:
                return ArticleLoader.newAllArticlesInstance(getActivity());
            default:
                Log.e("Sergio>", " onCreateLoader:\n= " + "Wrong Loader ID Provided: " + loaderId);
                return null;
        }

    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {


        if (loader.getId() == SINGLE_ARTICLE_LOADER_ID) {
            if (!isAdded()) {
                if (cursor != null) cursor.close();
                return;
            }

            mCursor = cursor;
            if (mCursor != null && !mCursor.moveToFirst()) {
                Log.e("Sergio> ", "Error reading item detail cursor");
                mCursor.close();
                mCursor = null;
            }

            bindViews();
        }


        if (loader.getId() == ALL_ARTICLES_LOADER_ID) {
            mCursor = cursor;


            // Select the start ID
            mCursor.moveToFirst();
            int position = 0;
            while (cursor.moveToNext()) {
                if (cursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    position = cursor.getPosition();
                    bindViews();
                }
            }

            fragmentPagerAdapter = new FragmentPagerAdapter(this);
            fragmentPagerAdapter.swapCursor(cursor);
            viewPager.setAdapter(fragmentPagerAdapter);
            viewPager.setCurrentItem(position, true);

        }


    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mCursor = null;
        fragmentPagerAdapter.swapCursor(null);

        bindViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewPager.removeOnPageChangeListener(onPageChangeListener);

    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }


    private void bindViews() {
        if (mRootView == null) return;

        TextView titleView = mRootView.findViewById(R.id.article_title);
        TextView bylineView = mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());

        if (mCursor == null) {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyDataAdapter.swapBodyDataList(null);
            return;
        }

        mRootView.setAlpha(0);
        mRootView.setVisibility(View.VISIBLE);
        mRootView.animate().alpha(1);
        String title = mCursor.getString(ArticleLoader.Query.TITLE);
        titleView.setText(title);

        Date publishedDate = parsePublishedDate();
        Spanned fromHtml;
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            fromHtml = Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>");

        } else {
            // If date is before 1902, just show the string
            fromHtml = Html.fromHtml(
                    outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>");
        }

        bylineView.setText(fromHtml);

        String bodyData = mCursor.getString(ArticleLoader.Query.BODY);
        bodyDataList = Arrays.asList(bodyData.split("(\r\n\r\n)"));
        dataListSize = bodyDataList.size();

        redrawRecyclerViewSubList();

        String imageUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);



        ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                .get(imageUrl, new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                        Bitmap bitmap = imageContainer.getBitmap();
                        if (bitmap != null) {
                            Palette palette = new Palette
                                    .Builder(bitmap)
                                    .maximumColorCount(12)
                                    .generate();
                            mMutedColor = palette.getDarkMutedColor(0xFF333333);
                            mPhotoView.setImageBitmap(imageContainer.getBitmap());
                            mRootView.findViewById(R.id.toolbar).setBackgroundColor(mMutedColor);
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });


//
//        Glide.with(this).asBitmap().load(imageUrl)
//                .listener(new RequestListener<Bitmap>() {
//                    @Override
//                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
//                        mPhotoView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.empty_detail));
//                        return false;
//                    }
//
//                    @Override
//                    public boolean onResourceReady(Bitmap bitmap, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
//                        if (bitmap != null) {
//                            Palette palette = new Palette
//                                    .Builder(bitmap)
//                                    .maximumColorCount(16)
//                                    .generate();
//                            mMutedColor = palette.getDarkMutedColor(0xFF333333);
//                            mPhotoView.setImageBitmap(bitmap);
//                            mRootView.findViewById(R.id.toolbar).setBackgroundColor(mMutedColor);
//                        }
//                        return false;
//                    }
//
//                }).into(mPhotoView);

    }


    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(Integer.MAX_VALUE - upButtonNormalBottom, 0));
    }
}
