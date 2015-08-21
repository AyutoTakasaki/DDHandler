package com.jdbnz.android.ddabslistview;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * AbsListViewを継承したViewにドラッグ＆ドロップで項目を移動する機能を実装する
 * */
public class DDHandler implements OnTouchListener, OnItemLongClickListener {

	/**
	 * @param mActivity
	 *            対象のAdapterViewを配置しているActivity
	 *
	 * @param targetView
	 *            対象のAbsListView
	 *
	 * @param tarAdapter
	 *            関連付けられているAdapter
	 */
	public DDHandler(Activity mActivity, AbsListView targetView,
			SortableAdapter tarAdapter) {
		this.mActivity = mActivity;
		this.tarView = targetView;

		tarView.setOnItemLongClickListener(this);
		tarView.setOnTouchListener(this);

		this.tarAdapter = tarAdapter;
	}

	// スクロールの範囲
	private int SCROLL_RANGE = 50;

	// 自動スクロールの長さ
	private int getScrollSize() {
		int scroll = (int) tarView.getHeight() / 7;
		return scroll;
	}

	private Activity mActivity = null;
	private AbsListView tarView = null;
	private SortableAdapter tarAdapter = null;

	// ドラッグ中に表示する画像
	private ImageView oIView;
	private WindowManager.LayoutParams oILayoutParams;
	private WindowManager wm = null;

	// ドラッグ中の画像の元のView
	private View touchedView = null;

	// タップ時の座標（ドラッグ開始時に使用）
	private PointF touchedPoint = null;

	// スクロール中か否か
	private boolean isScrollLoop = false;

	// Window Managerを取得
	private WindowManager getWM() {
		if (wm == null) {
			wm = (WindowManager) mActivity.getSystemService("window");
		}
		return wm;
	}

	// ドラッグ中か否か
	private boolean isDragging = false;

	/**
	 * ドラッグ中か否かを示す値を取得
	 *
	 * @return ドラッグ中であればtrue
	 */
	public boolean getDraggingState() {
		return isDragging;
	}

	/**
	 * ドラッグ中か否かを示す値を設定
	 *
	 * @param draggingState
	 *            設定する値
	 */
	public void setDraggingState(boolean draggingState) {
		this.isDragging = draggingState;
		tarAdapter.setDraggingState(draggingState);
	}

	// タイトルバーの位置の保持
	private Point titleBarLoc;

	private Point getTitlebarLocation() {
		Point l = new Point(0, 0);
		// タイトルバーの位置を取得
		try {
			ViewGroup decorView = (ViewGroup) mActivity.getWindow()
					.getDecorView();
			LinearLayout root = (LinearLayout) decorView.getChildAt(0);
			FrameLayout titleContainer = (FrameLayout) root.getChildAt(0);
			int tLoc[] = new int[2];
			titleContainer.getLocationOnScreen(tLoc);
			l.set(tLoc[0], tLoc[1]);
		} catch (Exception e) {
			// タイトルバーがないなど
		}
		return l;
	}

	private WindowManager.LayoutParams getNewLayoutParams() {
		WindowManager.LayoutParams lParam = new WindowManager.LayoutParams();
		lParam.gravity = Gravity.TOP | Gravity.LEFT;
		lParam.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		lParam.format = PixelFormat.TRANSLUCENT;
		lParam.windowAnimations = 0;
		lParam.x = 0;
		lParam.y = 0;
		return lParam;
	}

	/**
	 * ドラッグ中に表示される画像の移動
	 */
	private boolean moveOvarlay(PointF p) {
		if (oILayoutParams == null)
			oILayoutParams = getNewLayoutParams();

		if (touchedView != null) {
			oILayoutParams.x = (int) Math.round(p.x);
			oILayoutParams.y = (int) Math.round(p.y);

			// AbsListViewの位置を考慮
			int tarViewLoc[] = new int[2];
			tarView.getLocationOnScreen(tarViewLoc);
			int tarViewY = tarViewLoc[1] - titleBarLoc.y;
			oILayoutParams.y += tarViewY;

			// 真ん中を持ってタップしているよう見せる
			oILayoutParams.x -= touchedView.getWidth() / 2;
			oILayoutParams.y -= touchedView.getHeight() / 2;
		}
		try {
			getWM().updateViewLayout(oIView, oILayoutParams);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * 長押しを処理
	 */
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View v, int position,
			long id) {

		// ドラッグ中に表示するView
		ImageView iv = new ImageView(mActivity);

		// Viewをキャプチャするためにキャッシュ
		v.setDrawingCacheEnabled(true);

		// 取得したViewの表示内容をBitmapで取得
		Bitmap b = v.getDrawingCache();

		if (b != null) {
			b = b.copy(Bitmap.Config.ARGB_8888, false);

			// 次のキャプチャに備えキャッシュを削除 ←これがないと画像が変わらない場合も
			v.destroyDrawingCache();

			iv.setImageBitmap(b);

			// フィルタをつけたければ…
			// iv.setColorFilter(Color.RED,Mode.SRC_ATOP);

			// ドラッグ中の画像の表示形態指定
			WindowManager.LayoutParams lParam = getNewLayoutParams();
			lParam.height = v.getHeight();
			lParam.width = v.getWidth();

			setDraggingState(true);

			getWM().addView(iv, lParam);

			oIView = iv;
			oILayoutParams = lParam;
			touchedView = v;

			titleBarLoc = getTitlebarLocation();

			if (touchedPoint != null) {
				// タッチした瞬間の場所に画像を移動
				moveOvarlay(new PointF(touchedPoint.x, touchedPoint.y));
			}

			// 移動中の画像は隠す
			touchedView.setVisibility(View.INVISIBLE);

			return true;
		}

		return false;
	}

	/**
	 * タッチ中の指の移動とタッチの終了を処理
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP: // ドラッグ終了
			if (!getDraggingState())
				return false;
			try {
				getWM().removeView(oIView);
			} catch (Exception e) {
				// 実はオーバーレイしてなかった？
			}

			View tarView = new View(mActivity);
			int i;
			Rect rect;

			int x = Math.round(event.getX());
			int y = Math.round(event.getY());

			setDraggingState(false);

			// ドロップした場所の当たり判定
			for (i = 0; i < this.tarView.getChildCount(); i++) {
				tarView = (View) this.tarView.getChildAt(i);
				if (tarView != null && tarView != touchedView) {
					rect = new Rect();
					tarView.getHitRect(rect);
					if (rect.contains(x, y)) {
						tarView.setVisibility(View.INVISIBLE);
						tarAdapter.exchangeItem(touchedView, tarView);
						break;
					}
				}
			}

			touchedView.setVisibility(View.VISIBLE);
			if (tarView != null)
				tarView.setVisibility(View.VISIBLE);

			// 一応解放しておく
			wm = null;
			oIView = null;
			oILayoutParams = null;

			return true;

		case MotionEvent.ACTION_MOVE: // ドラッグ中
			if (!getDraggingState())
				return false;

			PointF herePoint = new PointF(event.getX(), event.getY());

			// ドラッグ画像の移動
			moveOvarlay(herePoint);

			float hereY = event.getY();

			hereY -= touchedView.getHeight() / 2;

			int accept = SCROLL_RANGE;

			// 画面端ならスクロール
			if (hereY < accept) {
				isScrollLoop = true;
				startPrevScroll();
			} else if (hereY + touchedView.getHeight() > this.tarView
					.getHeight() - accept) {
				isScrollLoop = true;
				startNextScroll();
			} else {
				stopScroll();
			}

			return true;
		case MotionEvent.ACTION_DOWN:
			// ドラッグ開始時の位置
			touchedPoint = new PointF(event.getX(), event.getY());
			return false;
		}
		return false;
	}

	// スクロール処理
	private void startPrevScroll() {
		if (!getDraggingState())
			return;
		tarView.getHandler().postDelayed(prevScroll, 200);
	}

	private void startNextScroll() {
		if (!getDraggingState())
			return;
		tarView.getHandler().postDelayed(nextScroll, 200);
	}

	private Runnable prevScroll = new Runnable() {
		@Override
		public void run() {
			tarView.smoothScrollBy(getScrollSize() * -1, 400);
			if (isScrollLoop) {
				startPrevScroll();
			}
		}
	};
	private Runnable nextScroll = new Runnable() {
		@Override
		public void run() {
			tarView.smoothScrollBy(getScrollSize(), 400);
			if (isScrollLoop) {
				startNextScroll();
			}
		}
	};

	private void stopScroll() {
		isScrollLoop = false;
	}

	/**
	 * ArrayListの指定位置の要素を入れ替える
	 *
	 * @param data
	 *            入れ替えるArrayList
	 *
	 * @param idxFrom
	 *            入れ替える元の要素のインデックス
	 *
	 * @param idxTo
	 *            入れ替える先の要素のインデックス
	 *
	 * @param mode
	 *            入れ替えの方法{@link ExOption}
	 *
	 * @return 入れ替えた新しいArrayList
	 */
	public static <E> ArrayList<E> arrayListExchanger(ArrayList<E> data,
			int idxFrom, int idxTo, int mode) {
		@SuppressWarnings("unchecked")
		ArrayList<E> tmp = (ArrayList<E>) data.clone();

		switch (mode) {
		case ExOption.EXCHANGE_CLOSE:
			if (idxTo < idxFrom) {
				for (int i = 0; i < tmp.size(); i++) {
					if (i < idxTo) {
						tmp.set(i, data.get(i));
					} else if (i == idxTo) {
						tmp.set(i, data.get(idxFrom));
					} else if (i <= idxFrom) {
						tmp.set(i, data.get(i - 1));
					} else if (i > idxFrom) {
						tmp.set(i, data.get(i));
					}
				}
			} else {
				for (int i = 0; i < tmp.size(); i++) {
					if (i < idxFrom) {
						tmp.set(i, data.get(i));
					} else if (i < idxTo) {
						tmp.set(i, data.get(i + 1));
					} else if (i == idxTo) {
						tmp.set(i, data.get(idxFrom));
					} else if (i > idxTo) {
						tmp.set(i, data.get(i));
					}
				}
			}
			break;
		case ExOption.EXCHANGE_SIMPLE:
		default:
			tmp.set(idxTo, tmp.get(idxFrom));
			tmp.set(idxFrom, data.get(idxTo));
			break;
		}

		return tmp;
	}

	public class ExOption {
		/**
		 * ただ入れ替えるだけ
		 */
		public static final int EXCHANGE_SIMPLE = 0;

		/**
		 * 入れ替えた後詰める
		 */
		public static final int EXCHANGE_CLOSE = 1;
	}

}
