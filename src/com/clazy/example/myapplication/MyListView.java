package com.clazy.example.myapplication;

import java.util.ArrayList;

import com.clazy.example.myapplication.MainActivity.News;
import com.clazy.example.myapplication.MainActivity.NewsAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class MyListView extends ListView implements android.widget.AdapterView.OnItemClickListener, AbsListView.OnScrollListener {

	private Handler MyHandler;
	private ArrayList<News> ForumNews;
	private ArrayList<News> VisibleNews;
	private NewsAdapter VisibleNewsAdapter;
	private MainActivity Activity;
	
	private View FootView;
	private int ItemCount;
	private boolean IsLoading = false;
	
	public void GiveHandlers(Handler MyHandler, ArrayList<News> ForumNews, ArrayList<News> VisibleNews, NewsAdapter VisibleNewsAdapter, MainActivity Activity) {
		this.MyHandler = MyHandler;
		this.ForumNews = ForumNews;
		this.VisibleNews = VisibleNews;
		this.VisibleNewsAdapter = VisibleNewsAdapter;
		this.Activity = Activity;
	}
	
	@SuppressLint("InflateParams")
	private void Initialize(Context context) {
		FootView = LayoutInflater.from(context).inflate(R.layout.foot_view, null);
		setOnScrollListener(this);
		setOnItemClickListener(this);
	}
	
	public MyListView(Context context) {
		super(context);
		Initialize(context);
	}
	public MyListView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		Initialize(context);
	}
	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Initialize(context);
	}
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		int LastVisibleIndex = view.getLastVisiblePosition();
		if (!IsLoading && scrollState == OnScrollListener.SCROLL_STATE_IDLE && LastVisibleIndex == ItemCount -1) {
			IsLoading = true;
			addFooterView(FootView);
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					int count = 0;
					for (News news : ForumNews) {
						if (!news.Enabled) {
							if (count < MainActivity.NewsOnAPage) {
								++count;
							} else {
								break;
							}
							news.Enabled = true;
							VisibleNews.add(news);
						}
					}
					if (count == 0) {
						Log.d("end", "footer");
						final TextView FootText = (TextView)findViewById(R.id.FootText);
						if (FootText != null) {
							MyHandler.post(new Runnable() {
								@Override
								public void run() {
									Log.d("set", "footer");
									FootText.setText("没有更多内容了亲~");
								}
							});
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							MyHandler.post(new Runnable() {
								@Override
								public void run() {
									IsLoading = false;
									Log.d("Remove", "footer");
									removeFooterView(FootView);
									Log.d("Remove2", "footer");
								}
							});
						}
					} else {
						MyHandler.post(new Runnable() {
							@Override
							public void run() {
								VisibleNewsAdapter.notifyDataSetChanged();
								IsLoading = false;
								removeFooterView(FootView);
							}
						});
					}
				}
			}).start();
		}
	}
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		ItemCount = totalItemCount;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		News OpenNews = (News)(getItemAtPosition(position));
		OpenNews.Read = true;
		Activity.EnterNews(OpenNews);
	}
}