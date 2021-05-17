package io.rong.imkit.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;

public class AutoRefreshListView extends ListView {
    private static final String TAG = AutoRefreshListView.class.getSimpleName();

    public enum State {
        REFRESHING,
        RESET,
    }

    public enum Mode {
        START,
        END,
        BOTH,
    }

    public interface OnRefreshListener {
        void onRefreshFromStart();

        void onRefreshFromEnd();
    }

    private OnRefreshListener refreshListener;
    private List<OnScrollListener> scrollListeners = new ArrayList<>();

    private State state = State.RESET;
    private Mode mode = Mode.START;
    private Mode currentMode = Mode.START;

    private boolean refreshableStart = true;
    private boolean refreshableEnd = true;

    private ViewGroup refreshHeader;
    private ViewGroup refreshFooter;

    private Iterator<OnScrollListener> iterator;

    private int offsetY;

    public AutoRefreshListView(Context context) {
        super(context);
        init(context);
    }

    public AutoRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AutoRefreshListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setOnRefreshListener(OnRefreshListener refreshListener) {
        this.refreshListener = refreshListener;
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        // replaced by addOnScrollListener
        throw new UnsupportedOperationException("Use addOnScrollListener instead!");
    }

    public void addOnScrollListener(OnScrollListener l) {
        scrollListeners.add(l);
    }

    /**
     * 不能在 OnScrollListener.onScroll 方法中调用此方法
     */
    public void removeOnScrollListener(OnScrollListener l) {
        scrollListeners.remove(l);
    }

    /**
     * OnScrollListener.onScroll 方法中移除当前 OnScrollListener 调用此方法
     */
    public void removeCurrentOnScrollListener() {
        iterator.remove();
    }

    private void init(Context context) {
        addRefreshView(context);

        super.setOnScrollListener(new OnScrollListener() {
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                iterator = scrollListeners.iterator();
                while (iterator.hasNext()) {
                    OnScrollListener listener = iterator.next();
                    listener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                for (OnScrollListener listener : scrollListeners) {
                    listener.onScrollStateChanged(view, scrollState);
                }
            }
        });

        initRefreshListener();

        state = State.RESET;
    }

    private void addRefreshView(Context context) {
        refreshHeader = (ViewGroup) View.inflate(context, R.layout.rc_refresh_list_view, null);
        addHeaderView(refreshHeader, null, false);
        refreshFooter = (ViewGroup) View.inflate(context, R.layout.rc_refresh_list_view, null);
        addFooterView(refreshFooter, null, false);
    }

    private void initRefreshListener() {
        OnScrollListener listener = new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE && state == State.RESET) {
                    boolean reachTop = (getFirstVisiblePosition() < getHeaderViewsCount() && getCount() > getHeaderViewsCount());
                    if (reachTop) {
                        onRefresh(true, false);
                    } else {
                        boolean reachBottom = getLastVisiblePosition() >= getCount() - 1;
                        if (reachBottom) {
                            onRefresh(false, true);
                        }
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        };

        addOnScrollListener(listener);
    }

    private void onRefresh(boolean start, boolean end) {
        if (refreshListener != null) {
            View firstVisibleChild = getChildAt(getHeaderViewsCount());
            if (firstVisibleChild != null) {
                offsetY = firstVisibleChild.getTop();
            }

            if (start && refreshableStart && mode != Mode.END) {
                currentMode = Mode.START;
                state = State.REFRESHING;
                refreshListener.onRefreshFromStart();
            } else if (end && refreshableEnd && mode != Mode.START) {
                currentMode = Mode.END;
                state = State.REFRESHING;
                refreshListener.onRefreshFromEnd();
            }
            updateRefreshView();
        }
    }

    private void updateRefreshView() {
        switch (state) {
            case REFRESHING:
                getRefreshView().getChildAt(0).setVisibility(View.VISIBLE);
                break;
            case RESET:
                if (currentMode == Mode.START) {
                    refreshHeader.getChildAt(0).setVisibility(View.GONE);
                } else {
                    refreshFooter.getChildAt(0).setVisibility(View.GONE);
                }
                break;
        }
    }

    private ViewGroup getRefreshView() {
        switch (currentMode) {
            case END:
                return refreshFooter;
            case START:
            default:
                return refreshHeader;
        }
    }

    public void onRefreshStart(Mode mode) {
        state = State.REFRESHING;
        currentMode = mode;
    }

    public State getRefreshState() {
        return state;
    }

    /**
     * 加载完成
     */
    public void onRefreshComplete(int count, int requestCount, boolean needOffset) {
        state = State.RESET;
        resetRefreshView(count, requestCount);
        if (!needOffset) {
            return;
        }

        if (currentMode == Mode.START) {
            setSelectionFromTop(count + getHeaderViewsCount(), refreshableStart ? offsetY : 0);
        }
    }

    public void onRefreshComplete() {
        state = State.RESET;
        updateRefreshView();
    }

    private void resetRefreshView(int count, int requestCount) {
        if (currentMode == Mode.START) {
            /* 如果是第一次加载，如果count<requestCount, 表示没有数据了。
             * 如果是后面的加载，为了列表稳定，只有count>0, 就保留header的高度
             */
            if (getCount() == count + getHeaderViewsCount() + getFooterViewsCount()) {
                refreshableStart = (count == requestCount);
            } else {
                refreshableStart = (count > 0);
            }
        } else {
            refreshableEnd = (count > 0);
        }
        updateRefreshView();
    }

    /**
     * handle over scroll when no more data
     */
    private boolean isBeingDragged = false;
    private int startY = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            return onTouchEventInternal(event);
        } catch (Exception e) {
            RLog.e(TAG, "onTouchEvent", e);
            return false;
        }
    }

    private boolean onTouchEventInternal(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchBegin(event);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                onTouchEnd();
                break;
        }
        try {
            return super.onTouchEvent(event);
        } catch (Exception e) {
            RLog.e(TAG, "onTouchEventInternal catch", e);
            return false;
        }
    }

    private void onTouchBegin(MotionEvent event) {
        int firstItemIndex = getFirstVisiblePosition();
        if (!refreshableStart && firstItemIndex <= getHeaderViewsCount() && !isBeingDragged) {
            isBeingDragged = true;
            startY = (int) event.getY();
        }
    }

    private void onTouchMove(MotionEvent event) {
        onTouchBegin(event);
        if (!isBeingDragged) {
            return;
        }

        int offsetY = (int) (event.getY() - startY);
        offsetY = Math.max(offsetY, 0) / 2;
        refreshHeader.setPadding(0, offsetY, 0, 0);
    }

    private void onTouchEnd() {
        if (isBeingDragged) {
            refreshHeader.setPadding(0, 0, 0, 0);
        }

        isBeingDragged = false;
    }

    /**
     * 从列表底部开始计算，指定的第几项是否可见
     * @param lastVisibleCount 列表底部开始计算，排除footer后的项数
     * @return 最后一项是否可见
     */
    public boolean isLastItemVisible(int lastVisibleCount) {
        boolean result = false;
        int itemCount = getAdapter().getCount();
        int headerViewsCount = getHeaderViewsCount();
        int lastItemPosition = itemCount - headerViewsCount - 1;//列表项最后一项的下标
        int lastVisiblePosition = getLastVisiblePosition();

        /*
         * 若最后一个可见项大于列表项最后一项下标（当显示footer时，最后一个可见可能为footer下标）
         * 则将列表项中最后一项下标记为最后一项可见下标
         */
        if(lastVisiblePosition > lastItemPosition){
            lastVisiblePosition = lastItemPosition;
        }

        if (lastVisiblePosition >= lastItemPosition - lastVisibleCount + 1) {
            result = true;
        }

        return result;
    }
}