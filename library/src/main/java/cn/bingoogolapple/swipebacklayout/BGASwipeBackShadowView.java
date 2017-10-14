package cn.bingoogolapple.swipebacklayout;

import android.app.Activity;
import android.graphics.Canvas;
import android.support.annotation.DrawableRes;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

/**
 * 作者:王浩 邮件:wanghao76@meituan.com
 * 创建时间:2017/10/13
 * 描述:左侧阴影控件
 */
class BGASwipeBackShadowView extends FrameLayout {
    private static final String TAG = BGASwipeBackShadowView.class.getSimpleName();
    private Activity mActivity;
    private WeakReference<Activity> mPreActivity;
    private ViewGroup mPreDecorView;
    private View mPreContentView;
    private View mShadowView;
    /**
     * 是否使用透明模式
     */
    private boolean mIsTranslucent = true;
    /**
     * 是否显示滑动返回的阴影效果
     */
    private boolean mIsNeedShowShadow = true;
    /**
     * 阴影资源 id
     */
    private int mShadowResId = R.drawable.bga_sbl_shadow;
    /**
     * 阴影区域的透明度是否根据滑动的距离渐变
     */
    private boolean mIsShadowAlphaGradient = true;
    /**
     * 是否是微信滑动返回样式
     */
    private boolean mIsWeChatStyle = true;

    BGASwipeBackShadowView(Activity activity, boolean isTranslucent) {
        super(activity);
        mActivity = activity;
        mIsTranslucent = isTranslucent;
    }

    /**
     * 设置是否显示滑动返回的阴影效果。默认值为 true
     *
     * @param isNeedShowShadow
     */
    void setIsNeedShowShadow(boolean isNeedShowShadow) {
        mIsNeedShowShadow = isNeedShowShadow;
        updateShadow();
    }

    /**
     * 设置阴影资源 id。默认值为 R.drawable.bga_sbl_shadow
     *
     * @param shadowResId
     */
    void setShadowResId(@DrawableRes int shadowResId) {
        mShadowResId = shadowResId;
        updateShadow();
    }

    /**
     * 设置阴影区域的透明度是否根据滑动的距离渐变。默认值为 true
     *
     * @param isShadowAlphaGradient
     */
    void setIsShadowAlphaGradient(boolean isShadowAlphaGradient) {
        mIsShadowAlphaGradient = isShadowAlphaGradient;
    }

    /**
     * 设置是否是微信滑动返回样式。默认值为 true
     */
    void setIsWeChatStyle(boolean isWeChatStyle) {
        mIsWeChatStyle = isWeChatStyle;
    }

    private void updateShadow() {
        if (mIsTranslucent) {
            if (mIsNeedShowShadow) {
                setBackgroundResource(mShadowResId);
            } else {
                setBackgroundResource(android.R.color.transparent);
            }
        } else {
            if (mIsNeedShowShadow) {
                if (mShadowView == null) {
                    mShadowView = new View(getContext());
                    addView(mShadowView, getChildCount(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                }
                mShadowView.setBackgroundResource(mShadowResId);
            } else {
                if (mShadowView != null) {
                    removeView(mShadowView);
                }
            }
        }
    }

    void bindPreActivity() {
        if (mIsTranslucent) {
            return;
        }

        if (mPreActivity == null) {
            Activity preActivity = BGASwipeBackManager.getInstance().getPenultimateActivity(mActivity);
            if (preActivity != null) {
                mPreActivity = new WeakReference<>(preActivity);
                mPreDecorView = (ViewGroup) preActivity.getWindow().getDecorView();
                mPreContentView = mPreDecorView.getChildAt(0);
                mPreDecorView.removeView(mPreContentView);

                addView(mPreContentView, 0, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            }
        }
    }

    void unBindPreActivity() {
        if (mIsTranslucent) {
            return;
        }

        if (mPreActivity == null) {
            return;
        }

        Activity activity = mPreActivity.get();
        if (activity == null || activity.isFinishing()) {
            return;
        }

        if (mPreDecorView != null && mPreContentView != null) {
            removeView(mPreContentView);
            mPreDecorView.addView(mPreContentView, 0);

            mPreContentView = null;
            mPreActivity.clear();
            mPreActivity = null;
        }
    }

    @Override
    protected void dispatchDraw(final Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mIsTranslucent) {
            return;
        }

        if (mPreContentView == null && mPreDecorView != null) {
            // TODO 处理部分 View 调用 draw 后立即手指触摸界面会卡死
//            if (containsProblemView(mPreDecorView)) {
//                dispatchDrawProblemView(canvas, mPreDecorView);
//            } else {
//                mPreDecorView.draw(canvas);
//            }

            mPreDecorView.draw(canvas);
        }
    }

    private void dispatchDrawProblemView(Canvas canvas, ViewGroup viewGroup) {
        int count = viewGroup.getChildCount();
        View childView;
        for (int i = 0; i < count; i++) {
            childView = viewGroup.getChildAt(i);
            if (isNeedIgnore(childView)) {
                Log.d(TAG, "忽略" + childView.getClass().getSimpleName());
                continue;
            } else if (childView instanceof ViewGroup && containsProblemView((ViewGroup) childView)) {
                dispatchDrawProblemView(canvas, (ViewGroup) childView);
            } else {
                childView.draw(canvas);
                Log.d(TAG, "没忽略" + childView.getClass().getSimpleName());
            }
        }
    }

    private boolean containsProblemView(ViewGroup viewGroup) {
        int childCount = viewGroup.getChildCount();
        View childView;
        for (int i = 0; i < childCount; i++) {
            childView = viewGroup.getChildAt(i);
            if (isNeedIgnore(childView)) {
                return true;
            } else if (childView instanceof ViewGroup) {
                if (containsProblemView((ViewGroup) childView)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNeedIgnore(View view) {
        if (view instanceof WebView) {
            return true;
        }
        if (view instanceof SurfaceView) {
            return true;
        }
        return false;
    }

    void setShadowAlpha(float alpha) {
        if (mIsNeedShowShadow && mIsShadowAlphaGradient) {
            if (mIsTranslucent) {
                ViewCompat.setAlpha(this, alpha);
            } else if (mShadowView != null) {
                ViewCompat.setAlpha(mShadowView, alpha);
            }
        }
    }

    void onPanelSlide(float slideOffset) {
        if (!mIsWeChatStyle) {
            return;
        }

        if (mIsTranslucent) {
            BGASwipeBackManager.onPanelSlide(mActivity, slideOffset);
        } else if (mPreContentView != null) {
            ViewCompat.setTranslationX(mPreContentView, (mPreContentView.getMeasuredWidth() / 3.0f) * (1 - slideOffset));
        }
    }

    void onPanelClosed() {
        if (!mIsWeChatStyle) {
            return;
        }

        if (mIsTranslucent) {
            BGASwipeBackManager.onPanelClosed(mActivity);
        } else if (mPreContentView != null) {
            ViewCompat.setTranslationX(mPreContentView, 0);
        }
        unBindPreActivity();
    }
}
