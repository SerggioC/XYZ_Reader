package com.example.xyzreader.adapter;


import android.animation.Animator;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.TransitionSet;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.ArticleListFragment;
import com.example.xyzreader.ui.ArticlePagerFragment;
import com.example.xyzreader.ui.DynamicHeightNetworkImageView;
import com.example.xyzreader.ui.MainActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;

public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ViewHolder> {
    private final RequestManager glideManager;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
    private Cursor mCursor;

    public ArticleListAdapter(Fragment fragment, Cursor cursor) {
        this.glideManager = Glide.with(fragment);
        this.mCursor = cursor;
        this.viewHolderListener = new ViewHolderListenerImpl(fragment);
    }

    /**
     * A listener that is attached to all ViewHolders
     * to handle image loading events and view clicks.
     */
    private interface ViewHolderListener {
        void onLoadCompleted(ImageView view, int adapterPosition);
        void onItemClicked(View view, int adapterPosition);
    }

    private final ViewHolderListener viewHolderListener;

    private class ViewHolderListenerImpl implements ViewHolderListener {
        private Fragment fragment;
        private AtomicBoolean enterTransitionStarted;

        ViewHolderListenerImpl(Fragment fragment) {
            this.fragment = fragment;
            this.enterTransitionStarted = new AtomicBoolean();
        }

        @Override
        public void onLoadCompleted(ImageView view, int adapterPosition) {
            if (MainActivity.currentPosition != adapterPosition) return;
            if (enterTransitionStarted.getAndSet(true)) return;
            fragment.startPostponedEnterTransition();
        }

        @Override
        public void onItemClicked(View view, int adapterPosition) {
            // Update the position.
            MainActivity.currentPosition = adapterPosition;

            // Update the corresponding Item ID
            MainActivity.currentItemId = getItemId(adapterPosition);

            // Create circular reveal animation on item click
            int finalRadius = (int) Math.hypot(view.getWidth() / 2, view.getHeight() / 2);
            Animator circularReveal = ViewAnimationUtils.createCircularReveal(view, view.getWidth() / 2, view.getHeight() / 2, 0, finalRadius);
            circularReveal.setDuration(100);
            circularReveal.start();

            // Exclude the clicked card from the exit transition (e.g. the card will disappear immediately
            // instead of fading out with the rest to prevent an overlapping animation of fade and move).
            ((TransitionSet) fragment.getExitTransition()).excludeTarget(view, true);

            DynamicHeightNetworkImageView transitioningView = view.findViewById(R.id.thumbnail);

//            ArticleDetailFragment articleDetailFragment = new ArticleDetailFragment();
//            Bundle bundle = new Bundle();
//            bundle.putLong(ARTICLE_ITEM_ID, itemId);
//            articleDetailFragment.setArguments(bundle);

            Fragment fragmentByTag = fragment.getFragmentManager().findFragmentByTag(ArticleListFragment.class.getSimpleName());
//            fragment.getFragmentManager().beginTransaction()
//                    .remove(fragmentByTag)
//                    .hide(fragmentByTag)
//                    .detach(fragmentByTag)
//                    .commitNow();


            fragment.getFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true) // Optimize for shared element transition
                    .addSharedElement(transitioningView, transitioningView.getTransitionName())
                    .replace(R.id.fragment_container, new ArticlePagerFragment(), ArticlePagerFragment.class.getSimpleName())
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_article, parent, false);
        return new ViewHolder(view, glideManager, viewHolderListener);
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e("Sergio> ", ex.getMessage() + "\n" + "Passing today's date");
            return new Date();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        int adapterPosition = viewHolder.getAdapterPosition();
        mCursor.moveToPosition(adapterPosition);

        viewHolder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

        Date publishedDate = parsePublishedDate();
        Spanned fromHtml;
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            fromHtml = Html.fromHtml(DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
        } else {
            fromHtml = Html.fromHtml(outputFormat.format(publishedDate)
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
        }
        viewHolder.subtitleView.setText(fromHtml);

        String imageUrl = mCursor.getString(ArticleLoader.Query.THUMB_URL);
        glideManager.load(imageUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        viewHolderListener.onLoadCompleted(viewHolder.thumbnailImageView, adapterPosition);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        viewHolderListener.onLoadCompleted(viewHolder.thumbnailImageView, adapterPosition);
                        return false;
                    }
                })
                .into(viewHolder.thumbnailImageView);

        float aspectRatio = mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO);
        viewHolder.thumbnailImageView.setAspectRatio(aspectRatio);
        viewHolder.thumbnailImageView.setTransitionName(imageUrl);

    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        RequestManager glideManager;
        ViewHolderListener viewHolderListener;
        DynamicHeightNetworkImageView thumbnailImageView;
        TextView titleView;
        TextView subtitleView;

        ViewHolder(View itemView, RequestManager glideManager, ViewHolderListener viewHolderListener) {
            super(itemView);
            this.glideManager = glideManager;
            this.viewHolderListener = viewHolderListener;
            thumbnailImageView = itemView.findViewById(R.id.thumbnail);
            titleView = itemView.findViewById(R.id.article_title);
            subtitleView = itemView.findViewById(R.id.article_subtitle);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            viewHolderListener.onItemClicked(view, getAdapterPosition());
        }
    }

}