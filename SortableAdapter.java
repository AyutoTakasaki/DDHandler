package com.jdbnz.android.ddabslistview;

import android.view.View;

/**
 * {@link DDHandler}の使用に必要な機能をAdapterに実装する
 */
public interface SortableAdapter {

	/**
	 * ドラッグ完了時の処理（どう処理するかは関知しない）
	 *
	 * @param touchedView
	 *            ドラッグしたView
	 *
	 * @param tarView
	 *            ドロップ地点のView
	 */
	void exchangeItem(View viewFrom, View viewTo);

	/**
	 * DDHandlerからドラッグ中か否かを通知
	 */
	void setDraggingState(boolean draggingState);

	/**
	 * @return getViewでinflateするかどうかを判断するために使用する（ドラッグ中は毎回inflateした方が良い）
	 */
	boolean getDraggingState();

}
