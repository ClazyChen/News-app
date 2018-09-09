package com.clazy.example.myapplication;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	
	@SuppressLint("SimpleDateFormat")
	public class News {
		public String Title;
		public String Description;
		public String Link;
		public int LargeGroupIndex;
		public int GroupIndex;
		public Date Time;
		public boolean Enabled;
		public boolean Read;
		public News(String _Title, String _Desc, String _Link, String _Time, int LargeGroup, int Group) {
			this.Title = _Title;
			this.Description = _Desc;
			this.Link = _Link;
			this.LargeGroupIndex = LargeGroup;
			this.GroupIndex = Group;
			this.Enabled = false;
			this.Read = false;
			try {
				this.Time = (InputDateFormat.parse(_Time));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class NewsAdapter extends ArrayAdapter<News> {

		private final int Id;
		public NewsAdapter(Context context, int resource, List<News> objects) {
			super(context, resource, objects);
			Id = resource;
		}
		
		@SuppressLint("ViewHolder")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			News news = (News)getItem(position);
			View view = LayoutInflater.from(getContext()).inflate(Id, null);
	        TextView NewsTitle = (TextView)view.findViewById(R.id.TitleView);
	        TextView NewsDate =  (TextView)view.findViewById(R.id.DateView);
	        NewsTitle.setText(news.Title);
	        if (news.Read) {
	        	NewsTitle.setTextColor(Color.rgb(63, 63, 63));
	        }
	        NewsDate.setText(GetPastTime(news.Time));
	        return view;
		}
		
	}
	
	public class NewsParagraphAdapter extends ArrayAdapter<String> {

		private final int Id;
		public NewsParagraphAdapter(Context context, int resource, List<String> objects) {
			super(context, resource, objects);
			Id = resource;
		}
		
		@SuppressLint("ViewHolder")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = LayoutInflater.from(getContext()).inflate(Id, null);
			final String HtmlText = (String)getItem(position);
			final TextView NewsView = (TextView)view.findViewById(R.id.pTextView);
			NewsView.setText(Html.fromHtml(HtmlText, new ImageGetter() {
				class GetDrawableTask extends AsyncTask<String, Void, Drawable> {
					TextView TaskTextView;
					String TaskHtml;
					GetDrawableTask(TextView TaskTextView, String TaskHtml) {
						this.TaskHtml = TaskHtml;
						this.TaskTextView = TaskTextView;
					}
					@Override
					protected Drawable doInBackground(String... params) {
						Drawable drawable = null;
						URL sourceURL;
						try {
							sourceURL = new URL(params[0]);
							URLConnection urlConnection = sourceURL.openConnection();
							urlConnection.setConnectTimeout(5000);
							urlConnection.setReadTimeout(5000);
							urlConnection.connect();
							InputStream inputStream = urlConnection.getInputStream();
							BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
							Bitmap bitmap = BitmapFactory.decodeStream(bufferedInputStream);
							drawable = new BitmapDrawable(getResources(), bitmap);
							drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
						} catch (IOException e) {
							e.printStackTrace();
						}
						return drawable;
					}
					@Override
					protected void onPostExecute(Drawable result) {
						final Drawable TaskDrawable = result;
						if (TaskDrawable != null) {
							TaskTextView.setText(Html.fromHtml(TaskHtml, new ImageGetter() {
								@Override
								public Drawable getDrawable(String source) {
									return TaskDrawable;
								}
							}, null));
						}
					}
				}
				@Override
				public Drawable getDrawable(String source) {
					if (source.startsWith("//")) {
						source = "http:" + source;
					}
					(new GetDrawableTask(NewsView, HtmlText)).execute(source);
					return null;
				}
			}, null));
			NewsView.setMovementMethod(LinkMovementMethod.getInstance());
			return view;
		}
	}
	
	private String GetPastTime(Date time) {
		if (time == null) {
			return "";
		}
		long minuteCount = (NowTime.getTime() - time.getTime()) / (60*1000);
		if (minuteCount < 60) {
			return String.valueOf(minuteCount) + "分钟前";
		} else if (minuteCount < 60 * 24) {
			return String.valueOf(minuteCount / 60) + "小时前";
		} else if (minuteCount < 60 * 24 * 7) {
			return String.valueOf(minuteCount / (60 * 24)) + "天前";
		} else {
			return OutputDateFormat.format(time);
		}
	}
	
	private ArrayList<News> GetEnabledNews(ArrayList<News> NewsList) {
		if (NewsList == null) {
			return null;
		}
		ArrayList<News> ans = new ArrayList<News>();
		for (News news : NewsList) {
			if (news.Enabled) {
				ans.add(news);
			}
		}
		return ans;
	}
	
	synchronized private void WriteMarkedFile() {
		try {
			XMLWriter writer = new XMLWriter(new FileWriter(MarkedFileName));
			writer.write(MarkedDocument);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("static-access")
	private void DisplayForumNews() {
		Collections.sort(ForumNews, new Comparator<News>() {
			@Override
			public int compare(News x, News y) {
				return ((Long)(y.Time.getTime())).compareTo((Long)(x.Time.getTime()));
			}
		});
		for (int i = ForumNews.size() - 1; i > 0; -- i) {
			if (ForumNews.get(i).Time.getTime() == ForumNews.get(i-1).Time.getTime()) {
				ForumNews.remove(i);
			}
		}
		int count = 0;
		for (News news : ForumNews) {
			if (count < this.NewsOnAPage) {
				++ count;
			} else {
				break;
			}
			news.Enabled = true;
		}
		MyHandler.post(new Runnable() {
			@Override
			public void run() {
				if (ForumNews != null) {
					VisibleNewsAdapter = new NewsAdapter(MainActivity.this,
							R.layout.news_item, VisibleNews = GetEnabledNews(ForumNews));
					RefreshList();
					MyListView listview = (MyListView)findViewById(R.id.NewsList);
					listview.setAdapter(VisibleNewsAdapter);
				}
			}
		});
	}
	
	public void onSearchButtonClick(View view) {
		EditText editText = (EditText)findViewById(R.id.Search);
		String[] SearchedWords = editText.getText().toString().split(" ");
		ForumNews = new ArrayList<News>();
		for (String key : Modules.keySet()) {
			if (!key.equals("Marked")) {
				for (News news : Modules.get(key)) {
					news.Enabled = false;
					for (String s : SearchedWords) {
						if (news.Title.contains(s)) {
							ForumNews.add(news);
							break;
						}
					}
				}
			}
		}
		if (ForumNews.size() == 0) {
			return;
		}
		DisplayForumNews();
	}
	
	private void DisplayNews(String forum, int number) {
		if (!LoadFinished) {
			return;
		}
		ForumNews = Modules.get(forum);
		if (ForumNews == null || ForumNews.size() == 0) {
			return;
		}
		DisplayForumNews();
	}
	
	public void onReturnButtonClick(View view) {
		MyHandler.post(new Runnable() {
			@Override
			public void run() {
				setContentView(R.layout.activity_main);
				VisibleNewsAdapter.notifyDataSetChanged();
			}
		});
	}
	
	public void EnterNews(final News news) {
		news.Read = true;
		MyHandler.post(new Runnable() {
			@Override
			public void run() {
				setContentView(R.layout.news_content);
				new Thread(new Runnable() {
					@Override
					public void run() {
						String Link = news.Link.replace("http://", "https://");
						Log.d("Open", Link);
						Document doc = null;
						try {
							if (!Link.contains("http")) {
								doc = Jsoup.parse(new File(Link), "UTF-8");
							} else {
								URLConnection conn = (new URL(Link)).openConnection();
								conn.setConnectTimeout(5000);
								conn.setReadTimeout(5000);
								doc = Jsoup.parse(conn.getInputStream(), "ISO-8859-1", Link);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						String HTML = "<p>" + news.Title + "</p>";
						if (doc == null) {
							HTML += "<p>连接超时。</p>";
						} else {
							Pattern GetPattern = Pattern.compile("charset=\"(.+?)\"");
							Matcher matcher = GetPattern.matcher(doc.html());
							String CharSet = matcher.find() ? matcher.group(1) : null;
							if (CharSet != null) {
								try {
									doc = Jsoup.parse(new String(doc.outerHtml().getBytes("ISO-8859-1"), CharSet));
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
							}
							Elements CoreNews = doc.select("div#Cnt-Main-Article-QQ");
							if (CoreNews.size() == 0) {
								HTML += "<p>此内容已被删除。</p>";
							} else {
								HTML += CoreNews.html();
							}
						}
						Elements HtmlItems = Jsoup.parse(HTML).select("p");
						final ArrayList<String> Items = new ArrayList<String>();
						for (int i = 0; i < HtmlItems.size(); ++ i) {
							Items.add(HtmlItems.get(i).html());
						}
						MyHandler.post(new Runnable() {
							@Override
							public void run() {
								NewsParagraphAdapter adapter = new NewsParagraphAdapter(MainActivity.this, 
										R.layout.news_paragraph, Items);
								ListView listview = (ListView)findViewById(R.id.NewsTextList);
								listview.setDivider(null);
								listview.setAdapter(adapter);
							}
						});
					}
				}).start();
			}
		});
	}
	
	@SuppressWarnings("finally")
	private ArrayList<News> ParseUrl(String Link, int LargeGroup, int Group) {
		ArrayList<News> NewsList = new ArrayList<News>();
		try {
			Document doc = null;
			if (!Link.contains("http")) {
				File MarkedFile = new File(MarkedFileName);
				if (MarkedFile.exists()) {
					doc = Jsoup.parse(MarkedFile, "UTF-8");
					MarkedDocument = (new SAXReader()).read(MarkedFile);
				} else {
					if (MarkedFile.createNewFile()) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								WriteMarkedFile();
							}
						}).start();
					}
					return NewsList;
				}
			} else {
				URLConnection conn = (new URL(Link)).openConnection();
				conn.setConnectTimeout(1000);
				conn.setReadTimeout(1000);
				boolean use锟斤拷 = false;
				for (int i = 0; i < 锟斤拷.length; ++ i) {
					if (锟斤拷[i].equals(Link)) {
						doc = Jsoup.parse(conn.getInputStream(), "gb2312", Link);
						use锟斤拷 = true;
						break;
					}
				}
				if (!use锟斤拷) {
					doc = Jsoup.parse(conn.getInputStream(), "UTF-8", Link);
				}
			}
			Elements Items = doc.select("item");
			Log.d(String.valueOf(Items.size()), Link);
			for (int i = 0; i < Items.size(); ++ i) {
				Matcher matchItem = LinkPattern.matcher(Items.get(i).html());
					NewsList.add(new News(Items.get(i).select("title").text(),
							Items.get(i).select("description").text(),
							matchItem.find() ? matchItem.group(1) : "",
							Items.get(i).select("pubdate").text(), LargeGroup, Group));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			return NewsList;
		}
	}
	
	private Pattern LinkPattern= Pattern.compile("<link>([^<>\\s]+)");
	private HashMap<String,ArrayList<News> > Modules;
	private org.dom4j.Document MarkedDocument;
	private Date NowTime;
	private final String MarkedFileName = "marked.xml";
	static final int NewsOnAPage = 8;
	private boolean LoadFinished = false;
	private ArrayList<News> ForumNews;
	private ArrayList<News> VisibleNews;
	private NewsAdapter VisibleNewsAdapter;
	private int loaded;
	@SuppressLint("SimpleDateFormat")
	private final SimpleDateFormat OutputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	@SuppressLint("SimpleDateFormat")
	private final SimpleDateFormat InputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public void onNewsButtonClick(View view) {
		switch (view.getId()) {
		case R.id.NewsButton:
			DisplayNews("News", NewsOnAPage);
			break;
		case R.id.NewsCjButton:
			DisplayNews("NewsCj", NewsOnAPage);
			break;
		case R.id.NewsJyButton:
			DisplayNews("NewsJy", NewsOnAPage);
			break;
		case R.id.NewsKjButton:
			DisplayNews("NewsKj", NewsOnAPage);
			break;
		case R.id.NewsTyButton:
			DisplayNews("NewsTy", NewsOnAPage);
			break;
		case R.id.NewsYlButton:
			DisplayNews("NewsYl", NewsOnAPage);
			break;
		case R.id.NewsYxButton:
			DisplayNews("NewsYx", NewsOnAPage);
			break;
		case R.id.RecommendedButton:
			DisplayNews("Recommended", NewsOnAPage);
			break;
		case R.id.MarkedButton:
			DisplayNews("Marked", NewsOnAPage);
			break;
		}
	}

	private final String 锟斤拷[] = new String[] {
			"http://news.qq.com/newsgn/rss_newsgn.xml",
			"http://tech.qq.com/web/webnews/rss_11.xml",
			"http://tech.qq.com/web/it/telerss.xml",
			"http://tech.qq.com/web/tele/telexml.xml",
			"http://tech.qq.com/web/Innovation/rss_cycx.xml",
			"http://tech.qq.com/web/ydhl/ydhl.xml",
			"http://tech.qq.com/web/dzsw/dzsw.xml",
			"http://tech.qq.com/web/sjwl/sjwl.xml",
			"http://tech.qq.com/web/gameol/onlinegame.xml"	
	};
	
	private final String Urls[][] = new String[][] {
		new String[] {
				"Marked",
				MarkedFileName
		}, new String[] {
				"News",
				"http://news.qq.com/newsgn/rss_newsgn.xml",
				"http://news.qq.com/newsgj/rss_newswj.xml",
				"http://news.qq.com/newsgj/rss_newswj.xml",
				"http://news.qq.com/milite/rss_milit.xml"
		}, new String[] {
				"NewsYl",
				"http://ent.qq.com/movie/rss_movie.xml",
				"http://ent.qq.com/tv/rss_tv.xml",
				"http://ent.qq.com/m_news/rss_yinyue.xml"
		}, new String[] {
				"NewsCj",
				"http://finance.qq.com/financenews/domestic/rss_domestic.xml",
				"http://finance.qq.com/financenews/international/rss_international.xml",
				"http://finance.qq.com/financenews/jinrongshichang/rss_jinrongshichang.xml",
				"http://finance.qq.com/money/rss_money.xml"
		}, new String[] {
				"NewsTy",
				"http://sports.qq.com/basket/nba/nbarep/rss_nbarep.xml",
				"http://sports.qq.com/basket/bskb/cba/rss_cba.xml",
				"http://sports.qq.com/isocce/yingc/rss_pl.xml",
				"http://sports.qq.com/isocce/yijia/rss_sereasa.xml",
				"http://sports.qq.com/isocce/xijia/rss_laliga.xml",
				"http://sports.qq.com/csocce/rss_csocce.xml",
				"http://sports.qq.com/tennis/rss_tennis.xml"
		}, new String[] {
				"NewsKj",
				"http://tech.qq.com/web/webnews/rss_11.xml",
				"http://tech.qq.com/web/it/telerss.xml",
				"http://tech.qq.com/web/tele/telexml.xml",
				"http://tech.qq.com/web/Innovation/rss_cycx.xml",
				"http://tech.qq.com/web/ydhl/ydhl.xml",
				"http://tech.qq.com/web/dzsw/dzsw.xml",
				"http://tech.qq.com/web/sjwl/sjwl.xml",
				"http://tech.qq.com/web/gameol/onlinegame.xml"
		}, new String[] {
				"NewsYx",
				"http://games.qq.com/ntgame/rss_ntgame.xml",
				"http://games.qq.com/mobile/rss_mobile.xml"
		}, new String[] {
				"NewsJy",
				"http://edu.qq.com/edunew/rss_edunew.xml",
				"http://edu.qq.com/abroad/rss_abroad.xml"
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler MyHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
	};
	
	private void RefreshList() {
		((MyListView)findViewById(R.id.NewsList)).GiveHandlers(MyHandler, ForumNews, VisibleNews, VisibleNewsAdapter, this);
	}
	
	synchronized private void DetectLoadFinished() {
		if (LoadFinished) {
			return;
		}
		loaded ++;
		if (loaded == Urls.length) {
			LoadFinished = true;
			MyHandler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(MainActivity.this, "加载完成", Toast.LENGTH_SHORT).show();
				}
			});
		}
	}
	
	public class Spider extends Thread {
		final int id;
		public Spider(int id) {
			this.id = id;
		}
		@Override
		public void run() {
			NowTime = new Date();
			final String ModuleName = Urls[id][0];
			ArrayList<News> ModuleNews = new ArrayList<News>();
			for (int j = 1; j < Urls[id].length; ++ j) {
				ModuleNews.addAll(ParseUrl(Urls[id][j], id, j));
			}
			Modules.put(ModuleName, ModuleNews);
			DetectLoadFinished();
		}
	}
 	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Modules = new HashMap<String, ArrayList<News> >();
		MarkedDocument = DocumentHelper.createDocument();
		NowTime = new Date();
		ForumNews = new ArrayList<News>();
		VisibleNews = new ArrayList<News>();
		VisibleNewsAdapter = null;
		LoadFinished = false;
		loaded = 0;
		setContentView(R.layout.activity_main);
		RefreshList();
		for (int i = 0; i < Urls.length; ++i) {
			(new Spider(i)).start();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
