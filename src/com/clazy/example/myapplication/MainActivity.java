package com.clazy.example.myapplication;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
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
import android.widget.Button;
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
		public boolean MarkedFlag;
		protected News() { }
		public News(String _Title, String _Desc, String _Link, String _Time, int LargeGroup, int Group) {
			this.Title = _Title;
			this.Description = _Desc;
			this.Link = _Link;
			this.LargeGroupIndex = LargeGroup;
			this.GroupIndex = Group;
			this.Enabled = false;
			this.Read = false;
			this.MarkedFlag = false;
			if (LargeGroup < 0) {
				this.MarkedFlag = true;
				this.Enabled = true;
				this.Read = true;
			} else {
				if (Modules != null && Modules.get(Marked) != null) {
					for (News news : Modules.get(Marked)) {
						if (news.Title.equals(Title)) {
							this.MarkedFlag = true;
							break;
						}
					}
 				}
				if (History != null) {
					for (News s : History) {
						if (s.Title.equals(Title)) {
							this.Read = true;
							break;
						}
					}
				}
			}
			this.MarkedFlag = (LargeGroup < 0);
			try {
				this.Time = (InputDateFormat.parse(_Time));
				if (this.Time.getTime() > NowTime.getTime()) {
					this.Time.setTime(NowTime.getTime() - 1000 * 60 * 60 * 24);
				}
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
	        if (NewsTitle != null) {
	        	NewsTitle.setText(news.Title);
		        if (news.Read) {
		        	NewsTitle.setTextColor(Color.GRAY);
		        }
	        }
	        String DateText = GetPastTime(news.Time);
	        if (news.MarkedFlag) {
	        	DateText = "已收藏 " + DateText;
	        }
	        if (NewsDate != null) {
	        	NewsDate.setText(DateText);
	        }
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
			if (NewsView == null) {
				return null;
			}
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
						(new GetDrawableTask(NewsView, HtmlText)).execute(source);
					} else if (!source.startsWith("http")) {
						final Bitmap bitmap = BitmapFactory.decodeFile(source);
						if (bitmap != null) {
							Drawable d = new BitmapDrawable(getResources(), bitmap);
							d.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
							return d;
						}
					} else {
						(new GetDrawableTask(NewsView, HtmlText)).execute(source);
					}
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
		if (minuteCount < 0) {
			return String.valueOf("1天前");
		} else if (minuteCount < 60) {
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
	
	synchronized private void WritePoints() {
		File PointsFile = new File(this.getExternalFilesDir(null), PointsFileName);
		if (!PointsFile.exists()) {
			try {
				PointsFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(PointsFile);
			if (Points != null) {
				for (String s : Points) {
					fos.write((s+ "\n").getBytes("UTF-8"));
				}
			}
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	synchronized private void WriteHistory() {
		File HistoryFile = new File(this.getExternalFilesDir(null), HistoryFileName);
		if (!HistoryFile.exists()) {
			try {
				HistoryFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(HistoryFile);
			if (History != null) {
				for (News news : History) {
					fos.write((news.Title + "\n").getBytes("UTF-8"));
					fos.write((news.LargeGroupIndex + "\n").getBytes("UTF-8"));
					fos.write((news.GroupIndex + "\n").getBytes("UTF-8"));
				}
			}
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	synchronized private void WriteMarkedFile() {
		File MarkedFile = new File(this.getExternalFilesDir(null), MarkedFileName);
		if (!MarkedFile.exists()) {
			try {
				MarkedFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(MarkedFile);
			if (Modules != null && Modules.get(Marked)!=null) {
				for (News news : Modules.get(Marked)) {
					fos.write((news.Title + "\n").getBytes("UTF-8"));
					fos.write((InputDateFormat.format(news.Time) + "\n").getBytes("UTF-8"));
					fos.write((news.Link + "\n").getBytes("UTF-8"));
				}
			}
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
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
					if (listview != null) {
						listview.setAdapter(VisibleNewsAdapter);
					}
				}
			}
		});
	}
	
	public void onRecommendButtonClick(View view) {
		class WeightNews extends News {
			public int Weight = 0;
			public WeightNews(News news, int Weight) {
				super();
				this.Weight = Weight;
				this.Title = news.Title;
				this.Link = news.Link;
				this.Time = news.Time;
				this.Read = news.Read;
				this.MarkedFlag = news.MarkedFlag;
				this.Enabled = false;
				this.GroupIndex = news.GroupIndex;
				this.LargeGroupIndex = news.LargeGroupIndex;
				this.Description = news.Description;
			}
		};
		class LCSAlgorithm {
			public LCSAlgorithm() {	}
			public int LCS(String x, String y) {
				int[][] d = new int[x.length()+1][y.length() +1];
				int maxlen = 0;
				for (int i = 1; i <= x.length(); ++ i) {
					for (int j = 1; j <= y.length(); ++ j) {
						if (x.charAt(i-1) == y.charAt(j-1)) {
							d[i][j] = d[i-1][j-1] +1;
							if (d[i][j] > maxlen) {
								maxlen = d[i][j];
							}
						}
					}
				}
				return maxlen;
			}
		}
		ArrayList<WeightNews> Recommended = new ArrayList<WeightNews>();
		LCSAlgorithm LCS = new LCSAlgorithm();
		for (String key : Modules.keySet()) {
			if (!key.equals(Marked)) {
				for (News news : Modules.get(key)) {
					if (!news.Read) {
						int Weight = 0;
						for (News his : History) {
							if (his.LargeGroupIndex == news.LargeGroupIndex) {
								Weight += 1;
								if (his.GroupIndex == news.GroupIndex) {
									Weight += 2;
								}
							}
							int lcs = LCS.LCS(his.Title, news.Title);
							if (lcs >= 3) {
								Weight += lcs -2;
							}
						}
						Recommended.add(new WeightNews(news, Weight));
					}
				}
			}
		}
		Collections.sort(Recommended, new Comparator<WeightNews>() {
			@Override
			public int compare(WeightNews x, WeightNews y) {
				return ((Integer)(y.Weight)).compareTo((Integer)(x.Weight));
			}
		});
		ForumNews = new ArrayList<News>();
		int count = 0;
		for (News news : Recommended) {
			if (count < 20) {
				count ++;
			} else {
				break;
			}
			ForumNews.add(news);
		}
		if (ForumNews.size() == 0) {
			return;
		}
		DisplayForumNews();
	}
	
	public void onSearchButtonClick(View view) {
		EditText editText = (EditText)findViewById(R.id.Search);
		if (editText == null) {
			return;
		}
		String[] SearchedWords = editText.getText().toString().split(" ");
		ForumNews = new ArrayList<News>();
		for (String key : Modules.keySet()) {
			if (!key.equals(Marked)) {
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
		LastVisitedForum = forum;
		ForumNews = Modules.get(forum);
		if (ForumNews == null || ForumNews.size() == 0) {
			return;
		}
		DisplayForumNews();
	}
	
	public void OnAnotherReturnButtonClick(View view) {
		WritePoints();
		MyHandler.post(new Runnable() {
			@Override
			public void run() {
				setContentView(R.layout.activity_main);
				RefreshForums();
			}
		});
	} 
	
	public void ChangePoint(String point) {
		boolean inPoint = false;
		for (String s : Points) {
			if (s.equals(point)) {
				inPoint = true;
			}
		}
		if (inPoint) {
			Points.remove(point);
			int Id = getPointButtonId(point);
			if (Id != 0) {
				Button b = (Button)findViewById(Id);
				if (b != null) {
					b.setTextColor(Color.BLACK);
				}
			}
		} else {
			Points.add(point);
			int Id = getPointButtonId(point);
			if (Id != 0) {
				Button b = (Button)findViewById(Id);
				if (b != null) {
					b.setTextColor(Color.GRAY);
				}
			}
		}
	}
	
	public void onChangePointButton(View view) {
		switch (view.getId()) {
		case R.id.NewsPointButton:
			ChangePoint("News");
			break;
		case R.id.NewsCjPointButton:
			ChangePoint("NewsCj");
			break;
		case R.id.NewsJyPointButton:
			ChangePoint("NewsJy");
			break;
		case R.id.NewsKjPointButton:
			ChangePoint("NewsKj");
			break;
		case R.id.NewsTyPointButton:
			ChangePoint("NewsTy");
			break;
		case R.id.NewsYlPointButton:
			ChangePoint("NewsYl");
			break;
		case R.id.NewsYxPointButton:
			ChangePoint("NewsYx");
			break;
		}
	}
	
	public void onEditButtonClick(View view) {
		MyHandler.post(new Runnable() {
			@Override
			public void run() {
				setContentView(R.layout.points_view);
				for (int i = 0; i < Urls.length; ++ i) {
					int Id = getPointButtonId(Urls[i][0]);
					if (Id != 0) {
						boolean inPoints = false;
						for (String s : Points) {
							if (s.equals(Urls[i][0])) {
								inPoints= true;
								break;
							}
						}
						Button b = (Button)findViewById(Id);
						if (b != null) {
							if (inPoints) {
								b.setTextColor(Color.GRAY);
							} else {
								b.setTextColor(Color.BLACK);
							}
						}
					}
				}
			}
		});
	}
	
	public void onReturnButtonClick(View view) {
		if (LastMarkedLength != Modules.get(Marked).size()) {
			LastMarkedLength = Modules.get(Marked).size();
			WriteMarkedFile();
		}
		MyHandler.post(new Runnable() {
			@Override
			public void run() {
				setContentView(R.layout.activity_main);
				DisplayNews(LastVisitedForum, NewsOnAPage);
			}
		});
	}
	
	public void onMarkedButtonClick(View view) {
		if (NowNews != null) {
			if (NowNews.MarkedFlag) {
				NowNews.MarkedFlag = false;
				if (Modules.get(Marked) != null) {
					for (int i = 0; i < Modules.get(Marked).size(); ++ i) {
						if (Modules.get(Marked).get(i).Title.equals(NowNews.Title)) {
							Modules.get(Marked).remove(i);
							break;
						}
					}
				}
				MyHandler.post(new Runnable() {
					@Override
					public void run() {
						Button button = (Button)findViewById(R.id.MarkedButton);
						if (button != null) {
							button.setText("设为收藏");
						}
					}
				});
			} else {
				NowNews.MarkedFlag = true;
				if (Modules.get(Marked) != null) {
					boolean Add = true;
					for (int i = 0; i < Modules.get(Marked).size(); ++ i) {
						if (Modules.get(Marked).get(i).Title.equals(NowNews.Title)) {
							Add = false;
							break;
						}
					}
					if (Add) {
						Modules.get(Marked).add(new News(NowNews.Title, "", NowNews.Link, 
								InputDateFormat.format(new Date()), -1, -1));
					}
				}
				MyHandler.post(new Runnable() {
					@Override
					public void run() {
						Button button = (Button)findViewById(R.id.MarkedButton);
						if (button != null) {
							button.setText("取消收藏");
						}
					}
				});
			}
		}
	}
	
	public void EnterNews(final News news) {
		news.Read = true;
		NowNews = news;
		if (History != null && news.LargeGroupIndex >= 0) {
			while (History.size() >= 100) {
				History.remove(0);
			}
			History.add(new News(news.Title, "", "", "", news.LargeGroupIndex, news.GroupIndex));
			WriteHistory();
		}
		MyHandler.post(new Runnable() {
			@Override
			public void run() {
				setContentView(R.layout.news_content);
				Button button = (Button)findViewById(R.id.MarkedButton);
				if (button != null) {
					if (news.MarkedFlag) {
						button.setText("取消收藏");
					}
				}
				new Thread(new Runnable() {
					@Override
					public void run() {
						final File NewsDirectory = new File(MainActivity.this.getExternalFilesDir(null), news.Title);
						String HtmlText = "";
						if (NewsDirectory.exists()) {
							File NewsFile = new File(NewsDirectory, "index.htm");
							if (NewsFile.exists()) {
								try {
									Scanner in = new Scanner(NewsFile);
									while (in.hasNext()) {
										HtmlText += in.nextLine();
									}
									in.close();
								} catch (FileNotFoundException e) {
									e.printStackTrace();
								}
							}
						} else {
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
							HtmlText = "<p><h1>" + news.Title + "</h1></p>";
							if (doc == null) {
								HtmlText += "<p>连接超时。</p>";
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
									HtmlText += "<p>此内容已被删除。</p>";
								} else {
									HtmlText += CoreNews.html();
								}
							}
							if (!NewsDirectory.exists()) {
								NewsDirectory.mkdir();
								File NewsFile = new File(NewsDirectory, "index.htm");
								if (!NewsFile.exists()) {
									try {
										NewsFile.createNewFile();
										FileOutputStream fos = new FileOutputStream(NewsFile);
										String NewNews = "";
										Matcher matcher = ImagePattern.matcher(HtmlText);
										int startn = 0, endn = 0;
										while (matcher.find(endn)) {
											startn = matcher.start(1);
											String OldImageName = HtmlText.substring(startn, matcher.end(3));
											final String NewImageName = OldImageName.hashCode() + ".png";
											if (OldImageName.startsWith("//")) {
												OldImageName = "http:" + OldImageName;
											}
											final String PreImageName = OldImageName;
											new Thread(new Runnable() {
												@Override
												public void run() {
													URL sourceURL;
													try {
														sourceURL = new URL(PreImageName);
														URLConnection urlConnection = sourceURL.openConnection();
														urlConnection.setConnectTimeout(5000);
														urlConnection.setReadTimeout(5000);
														urlConnection.connect();
														InputStream inputStream = urlConnection.getInputStream();
														BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
														Bitmap bitmap = BitmapFactory.decodeStream(bufferedInputStream);
														File ImageFile = new File(NewsDirectory, NewImageName);
														if (!ImageFile.exists()) {
															ImageFile.createNewFile();
														}
														FileOutputStream imageout = new FileOutputStream(ImageFile);
														bitmap.compress(CompressFormat.PNG, 100, imageout);
														imageout.close();
													} catch (IOException e) {
														e.printStackTrace();
													}
												}
											}).start();
											NewNews += HtmlText.substring(endn, startn) + NewsDirectory + "/" + NewImageName;
											endn = matcher.end(3);
										}
										NewNews += HtmlText.substring(endn);
										fos.write(NewNews.getBytes());
										fos.close();
									} catch (IOException e1) {
										e1.printStackTrace();
									}
								}
							}
						}					
						Elements HtmlItems = Jsoup.parse(HtmlText).select("p");
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
								if (listview != null) {
									listview.setDivider(null);
									listview.setAdapter(adapter);
								}
							}
						});
					}
				}).start();
			}
		});
	}
	
	synchronized private void GetHistory() {
		File HistoryFile = new File(this.getExternalFilesDir(null), HistoryFileName);
		if (!HistoryFile.exists()) {
			try {
				HistoryFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		History = new ArrayList<News>();
		try {
			Scanner in = new Scanner(HistoryFile);
			while (in.hasNext()) {
				String Title = in.nextLine();
				Integer LargeGroup = in.hasNext() ? Integer.parseInt(in.nextLine()) : null;
				Integer Group = in.hasNext() ? Integer.parseInt(in.nextLine()) : null;
				if (LargeGroup != null && Group != null) {
					History.add(new News(Title, "", "", "", LargeGroup, Group));
				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	synchronized private void GetPoints() {
		File PointsFile = new File(this.getExternalFilesDir(null), PointsFileName);
		if (!PointsFile.exists()) {
			try {
				PointsFile.createNewFile();
				FileOutputStream fos = new FileOutputStream(PointsFile);
				for (int i = 0; i < Urls.length; ++ i) {
					Points.add(Urls[i][0]);
					fos.write((Urls[i][0] + "\n").getBytes());
				}
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			Scanner in = new Scanner(PointsFile);
			while (in.hasNext()) {
				String Forum = in.nextLine();
				Points.add(Forum);
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("finally")
	synchronized private void ProcessMarked() {
		File MarkedFile = new File(this.getExternalFilesDir(null), MarkedFileName);
		if (!MarkedFile.exists()) {
			try {
				MarkedFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				Modules.put(Marked, new ArrayList<News>());
				return;
			}
		}
		ArrayList<News> MarkedList = new ArrayList<News>();
		try {
			Scanner in = new Scanner(MarkedFile);
			while (in.hasNext()) {
				String Title = in.nextLine();
				String Time = in.hasNext() ? in.nextLine() : null;
				String Url = in.hasNext() ? in.nextLine() : null;
				if (Time != null && Url != null) {
					MarkedList.add(new News(Title, "", Url, Time, -1, -1));
				}
			}
			Modules.put(Marked, MarkedList);
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("finally")
	private ArrayList<News> ParseUrl(String Link, int LargeGroup, int Group) {
		ArrayList<News> NewsList = new ArrayList<News>();
		try {
			Document doc = null;
			URLConnection conn = (new URL(Link)).openConnection();
			conn.setConnectTimeout(1000);
			conn.setReadTimeout(1000);
			boolean use锟斤拷 = false;
			for (int i = 0; i < 锟斤拷.length; ++i) {
				if (锟斤拷[i].equals(Link)) {
					doc = Jsoup.parse(conn.getInputStream(), "gb2312", Link);
					use锟斤拷 = true;
					break;
				}
			}
			if (!use锟斤拷) {
				doc = Jsoup.parse(conn.getInputStream(), "UTF-8", Link);
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
	private Pattern ImagePattern = Pattern.compile("src=\"([^\\s]+/)([^\\s/\\.]+)(\\.[^\\s]+)\"");
	private Pattern LinkPattern= Pattern.compile("<link>([^<>\\s]+)");
	private HashMap<String,ArrayList<News> > Modules;
	private Date NowTime;
	private final String Marked = "Marked";
	private final String MarkedFileName = "marked.txt";
	private final String HistoryFileName = "history.txt";
	private final String PointsFileName = "points.txt";
	static final int NewsOnAPage = 8;
	private boolean LoadFinished = false;
	private ArrayList<News> ForumNews;
	private ArrayList<News> VisibleNews;
	private ArrayList<News> History;
	private ArrayList<String> Points;
	private NewsAdapter VisibleNewsAdapter;
	private String LastVisitedForum = Marked;
	private int LastMarkedLength = 0;
	private News NowNews;
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
			DisplayNews(Marked, NewsOnAPage);
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
		MyListView lv = (MyListView)findViewById(R.id.NewsList);
		if (lv != null) {
			lv.GiveHandlers(MyHandler, ForumNews, VisibleNews, VisibleNewsAdapter, this);
		}
	}
	
	private void RefreshForums() {
		for (int i = 0; i < Urls.length; ++ i) {
			boolean InPoints = false;
			for (String s : Points) {
				if (Urls[i][0].equals(s)) {
					InPoints = true;
					break;
				}
			}
			if (!InPoints) {
				int Id = getButtonId(Urls[i][0]);
				if (Id != 0) {
					Button targetButton = (Button)findViewById(Id);
					if (targetButton != null) {
						targetButton.setVisibility(View.GONE);
					}
				}
			}
		}
	}
	
	synchronized private void DetectLoadFinished(String ModuleName, ArrayList<News> ModuleNews) {
		Modules.put(ModuleName, ModuleNews);
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
			final String ModuleName = Urls[id][0];
			ArrayList<News> ModuleNews = new ArrayList<News>();
			for (int j = 1; j < Urls[id].length; ++ j) {
				ModuleNews.addAll(ParseUrl(Urls[id][j], id, j));
			}
			DetectLoadFinished(ModuleName, ModuleNews);
		}
	}
 	
	
	private int getPointButtonId(String name) {
		if (name.equals("News")) {
			return R.id.NewsPointButton;
		} else if (name.equals("NewsYl")) {
			return R.id.NewsYlPointButton;
		} else if (name.equals("NewsCj")) {
			return R.id.NewsCjPointButton;
		} else if (name.equals("NewsTy")) {
			return R.id.NewsTyPointButton;
		} else if (name.equals("NewsKj")) {
			return R.id.NewsKjPointButton;
		} else if (name.equals("NewsYx")) {
			return R.id.NewsYxPointButton;
		} else if (name.equals("NewsJy")) {
			return R.id.NewsJyPointButton;
		} else {
			return 0;
		}
	}
	
	private int getButtonId(String name) {
		if (name.equals("News")) {
			return R.id.NewsButton;
		} else if (name.equals("NewsYl")) {
			return R.id.NewsYlButton;
		} else if (name.equals("NewsCj")) {
			return R.id.NewsCjButton;
		} else if (name.equals("NewsTy")) {
			return R.id.NewsTyButton;
		} else if (name.equals("NewsKj")) {
			return R.id.NewsKjButton;
		} else if (name.equals("NewsYx")) {
			return R.id.NewsYxButton;
		} else if (name.equals("NewsJy")) {
			return R.id.NewsJyButton;
		} else {
			return 0;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Modules = new HashMap<String, ArrayList<News> >();
		NowTime = new Date();
		ForumNews = new ArrayList<News>();
		VisibleNews = new ArrayList<News>();
		Points = new ArrayList<String>();
		VisibleNewsAdapter = null;
		LoadFinished = false;
		loaded = 0;
		NowNews = null;
		NowTime = new Date();
		setContentView(R.layout.activity_main);
		RefreshList();
		GetHistory();
		GetPoints();
		RefreshForums();
		ProcessMarked();
		LastMarkedLength = Modules.get(Marked).size();
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
