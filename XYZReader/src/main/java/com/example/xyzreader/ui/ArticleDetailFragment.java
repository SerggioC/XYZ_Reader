package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.adapter.BodyDataAdapter;
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

    // Big text data RecyclerView variables
    private static final int CHUNK_SIZE = 10;
    private static final int SINGLE_ARTICLE_LOADER_ID = 1;
    private int FROM_INDEX = 0;
    private int TO_INDEX = 20;

    private Cursor mCursor;
    private long mItemId;
    private int mMutedColor = 0xFF333333;

    private View mPhotoContainerView;
    private ImageView mPhotoView;
    private int mScrollY;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat();     // Use default locale format
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1); // Most time functions can only handle 1902 - 2037
    private BodyDataAdapter bodyDataAdapter;
    private List<String> bodyDataList;
    private int dataListSize;


    private long mSelectedItemId;
    private View mUpButton;
    private TextView titleView;
    private TextView bylineView;
    private Toolbar mToolbar;
    private RecyclerView bodyDataRecyclerView;
    private NestedScrollView nestedScrollView;
    private FloatingActionButton shareFAB;
    private ActionBar actionBar;
    private Drawable backIcon;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_article_detail_page, container, false);
        bindTheViews(rootView);

        Bundle arguments = getArguments();
        if (arguments.containsKey(ARG_ITEM_ID)) {
            mItemId = arguments.getLong(ARG_ITEM_ID);
        }

        //mStartId = arguments.containsKey(ARTICLE_ITEM_ID) ? arguments.getLong(ARTICLE_ITEM_ID) : 0;


        bylineView.setMovementMethod(new LinkMovementMethod());
        shareFAB.setOnClickListener(view -> shareArticle());

        if (savedInstanceState == null) {
            mSelectedItemId = mItemId;
        } else {
            mSelectedItemId = MainActivity.currentItemId;
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(rootView.getContext(), LinearLayoutManager.VERTICAL, false);
        bodyDataRecyclerView.setLayoutManager(linearLayoutManager);
        bodyDataRecyclerView.setHasFixedSize(false);

        // Code to load more data incrementally and reduce start up time.
        nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (scrollView, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if ((scrollY >= (bodyDataRecyclerView.getMeasuredHeight() - scrollView.getMeasuredHeight())) && scrollY > oldScrollY) {
                TO_INDEX += CHUNK_SIZE;
                if (TO_INDEX >= dataListSize) TO_INDEX = dataListSize - 1;
                redrawRecyclerViewSubList();
            }
        });

        bodyDataAdapter = new BodyDataAdapter(rootView.getContext());
        bodyDataRecyclerView.setAdapter(bodyDataAdapter);

        getLoaderManager().initLoader(SINGLE_ARTICLE_LOADER_ID, null, this);

        return rootView;
    }

    private void bindTheViews(View rootView) {
        mToolbar = rootView.findViewById(R.id.toolbar);
        setUpToolbar();
        backIcon = mToolbar.getNavigationIcon();
        titleView = rootView.findViewById(R.id.article_title);
        bylineView = rootView.findViewById(R.id.article_byline);
        mPhotoView = rootView.findViewById(R.id.photo);
        mPhotoContainerView = rootView.findViewById(R.id.photo_container);
        shareFAB = rootView.findViewById(R.id.share_fab);
        bodyDataRecyclerView = rootView.findViewById(R.id.article_body_recyclerView);
        nestedScrollView = rootView.findViewById(R.id.nested_scrollview);
        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    private void setUpToolbar() {
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);

        // Show the Up button in the action bar.
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);

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

            updateViews();
        }

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mCursor = null;
        updateViews();
    }


    private void updateViews() {
        //if (mRootView == null) return;

        if (mCursor == null) {
            //mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyDataAdapter.swapBodyDataList(null);
            return;
        }

        backIcon.setAlpha(0);
        backIcon.setVisible(true, true);
        backIcon.setAlpha(1);
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
                            mToolbar.setBackgroundColor(mMutedColor);
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


    private void shareArticle() {
        startActivity(Intent
                .createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
    }

}
