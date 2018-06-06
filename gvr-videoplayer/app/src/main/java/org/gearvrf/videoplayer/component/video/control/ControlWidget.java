package org.gearvrf.videoplayer.component.video.control;

import android.annotation.SuppressLint;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.gearvrf.GVRContext;
import org.gearvrf.videoplayer.R;
import org.gearvrf.videoplayer.focus.FocusListener;
import org.gearvrf.videoplayer.focus.FocusableViewSceneObject;
import org.gearvrf.videoplayer.util.TimeUtils;

@SuppressLint("InflateParams")
public class ControlWidget extends FocusableViewSceneObject implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private View mMainView;
    private SeekBar mSeekBar;
    private ImageView mPlayPauseButtonImage;
    private TextView mElapsedTime, mDurationTime, mTitle;
    private ControlWidgetListener mOnVideoControllerListener;
    @DrawableRes
    private int mStateResource;

    public ControlWidget(GVRContext gvrContext, float width, float height) {
        super(gvrContext, getMainView(gvrContext, R.layout.layout_player_controller), width, height);

        mMainView = getRootView();
        setName(getClass().getSimpleName());
    }

    private static View getMainView(GVRContext gvrContext, @LayoutRes int layout) {
        return LayoutInflater.from(gvrContext.getContext()).inflate(layout, null);
    }

    @Override
    protected void onInitView() {
        super.onInitView();
        initView();
    }

    private void initView() {

        mSeekBar = mMainView.findViewById(R.id.progressBar);
        LinearLayout playPauseButton = mMainView.findViewById(R.id.playPauseButton);
        mPlayPauseButtonImage = playPauseButton.findViewById(R.id.image);
        mElapsedTime = mMainView.findViewById(R.id.elapsedTimeText);
        mDurationTime = mMainView.findViewById(R.id.durationTimeText);
        mTitle = mMainView.findViewById(R.id.titleText);

        playPauseButton.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);

        showPlay();
    }

    public void showPause() {
        mStateResource = R.drawable.ic_pause;
        mPlayPauseButtonImage.setImageResource(mStateResource);
    }

    public void showPlay() {
        mStateResource = R.drawable.ic_play;
        mPlayPauseButtonImage.setImageResource(mStateResource);
    }

    public void setTitle(final CharSequence title) {
        mTitle.post(new Runnable() {
            @Override
            public void run() {
                mTitle.setText(title);
            }
        });
    }

    private void updateElapsedTimeText(final int progress) {
        mElapsedTime.post(new Runnable() {
            @Override
            public void run() {
                mElapsedTime.setText(TimeUtils.formatDurationFull(progress));
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateDurationTimeText(final int maxProgress) {
        mDurationTime.post(new Runnable() {
            @Override
            public void run() {
                mDurationTime.setText(TimeUtils.formatDurationFull(maxProgress));
            }
        });
    }

    public void setProgress(final int progress) {
        mSeekBar.post(new Runnable() {
            @Override
            public void run() {
                mSeekBar.setProgress(progress);
                updateElapsedTimeText(progress);
            }
        });
    }

    public void setMaxProgress(final int maxProgress) {
        mSeekBar.post(new Runnable() {
            @Override
            public void run() {
                mSeekBar.setMax(maxProgress);
                updateDurationTimeText(maxProgress);
            }
        });
    }

    public void setOnVideoControllerListener(ControlWidgetListener listener) {
        this.mOnVideoControllerListener = listener;
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.playPauseButton) {
            if (mOnVideoControllerListener != null) {
                if (mStateResource == R.drawable.ic_play) {
                    showPause();
                    mOnVideoControllerListener.onPlay();
                } else {
                    showPlay();
                    mOnVideoControllerListener.onPause();
                }
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mOnVideoControllerListener != null && fromUser) {
            mOnVideoControllerListener.onSeek(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void setFocusListener(@NonNull FocusListener<FocusableViewSceneObject> listener) {
        super.setFocusListener(listener);
    }
}