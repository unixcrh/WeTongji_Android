package com.wetongji.net;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.wetongji.daofactory.UserFactory;

import android.util.Log;

/**
 * @author hezibo
 *
 */
public class WTClient 
{
	private static WTClient client = null;
	
	private HttpClient httpClient;
	private HttpGet request;
	private HttpPost post;
	
	private boolean hasError;
	private String errorDesc;
	private int responseStatusCode;
	
	private String responseStr;
	private String session;
	private String uid;
	
	private Map<String, String> params;
	private Map<String, String> data;

	
	private static String APIDomain = "http://we.tongji.edu.cn/api/call";
	
	//实现单例模式
	private WTClient()
	{
		httpClient = new DefaultHttpClient();
		request = new HttpGet();
		post = new HttpPost();
		setHasError(false);
		setSession(null);
		setErrorDesc(null);
		setResponseStatusCode(0);
		setUid(null);
		params = new HashMap<String, String>();
		setData(new HashMap<String, String>());
		params.put("D", "android");
		params.put("V", "1.0");
	}
	public static synchronized WTClient getInstance()
	{
		if(client == null)
			client = new WTClient();
		return client;
	}
	
	//用来生成MD5 HASH散列值
	public String hashQueryString(String s)
	{
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',   
				  'a', 'b', 'c', 'd', 'e', 'f' };   
		  try {   
			   byte[] strTemp = s.getBytes();   
			   //使用MD5创建MessageDigest对象   
			   MessageDigest mdTemp = MessageDigest.getInstance("MD5");   
			   mdTemp.update(strTemp);   
			   byte[] md = mdTemp.digest();   
			   int j = md.length;   
			   char str[] = new char[j * 2];   
			   int k = 0;   
			   for (int i = 0; i < j; i++) {   
				    byte b = md[i];   
				    //System.out.println((int)b);   
				    //将没个数(int)b进行双字节加密   
				    str[k++] = hexDigits[b >> 4 & 0xf];   
				    str[k++] = hexDigits[b & 0xf];   
			   }   
			   return new String(str);   
		  } 
		  catch (Exception e) {
			  return null;
		  } 
	}
	
	//形成排序后的参数串
	public String queryString()
	{
		String str = "?";
		List<Map.Entry<String, String>> mappingList = new ArrayList<Map.Entry<String, String>>(params.entrySet());
		
		Collections.sort(mappingList, new Comparator<Map.Entry<String, String>>(){
					public int compare(Map.Entry<String, String> entry1, Map.Entry<String, String> entry2)
					{
						return entry1.getKey().compareTo(entry2.getKey());
					}
				}
				);
		
		for(Map.Entry<String, String> entry : mappingList)
		{
			str += entry.getKey() + "=" + entry.getValue();
			str += "&";
		}
		
		String subStr = str.substring(1, str.length() - 1);
		return subStr;
	}
	
	//形成新的URL值
	public void buildURL() throws Exception
	{
		String queryStr = queryString();
		String hashStr = hashQueryString(queryStr);
		String url = APIDomain + "?" + queryStr + "&H=" + hashStr;
		
		Log.v("final", url);
		
		URI finalURI = new URI(url);
		request.setURI(finalURI);
	}
	
	//执行request
	public void executeRequest() throws Exception
	{
		this.buildURL();
		HttpResponse response = httpClient.execute(request);
		
		switch(response.getStatusLine().getStatusCode())
		{
			case 200:
			{
				this.requestFinished(response);
			}
				break;
			default:
			{
				//出现网络错误
				this.setHasError(true);
				this.setResponseStatusCode(response.getStatusLine().getStatusCode());
				this.setErrorDesc(response.getStatusLine().getReasonPhrase());
				Log.v("REQUESTFAILED", this.getErrorDesc());
			}
		}
	}
	public void requestFinished(HttpResponse response) throws RuntimeException, Exception
	{
		responseStr = EntityUtils.toString(response.getEntity());
		
		JSONObject json = new JSONObject(responseStr);
		JSONObject data = json.getJSONObject("Data");
		JSONObject status = json.getJSONObject("Status");
	
		
		String id = status.getString("Id");
		String memo = status.getString("Memo");
		
		if(data.length() == 0)
		{
			this.setHasError(true);
			this.setResponseStatusCode(Integer.valueOf(id));
			this.setErrorDesc(memo);
			
			Log.v("id", id);
			Log.v("Memo", memo);
		}else
		{
			this.setResponseStr(data.toString());//responseStr存放的是整个data这个json
			String session = data.getString("Session");
			this.setSession(session);
			
			Log.v("session", session);
			Log.v("responseStr", responseStr);
		}
	}
	//激活用户账号
	public void activeUser(String num, String name, String password) throws Exception
	{
	    name = URLEncoder.encode(name, "UTF-8");
		params.put("M", "User.Active");
		params.put("NO", num);
		params.put("Name", name);
		params.put("Password", password);
		this.executeRequest();
		
	}
	
	//验证用户登录
	public void login(String name, String password) throws Exception
	{
	    //name = URLEncoder.encode(name, "UTF-8");

		params.put("M", "User.LogOn");
		params.put("NO", name);
		params.put("Password", password);
		this.executeRequest();
		
	}
	
	//修改用户密码
	public void updatePassword(String oldPassword, String newPassword, String session, String uid) throws Exception
	{
		params.put("M", "User.Update.Password");
		params.put("Old", oldPassword);
		params.put("New", newPassword);
		params.put("S", session);
		params.put("U", uid);
		
		this.executeRequest();
	}
	public void resetPasswordWithUserName(String name, String number) throws Exception
	{
		name = URLEncoder.encode(name, "UTF-8");
		
		params.put("M", "User.Reset.Password");
		params.put("Name", name);
		params.put("NO", number);
		
		this.executeRequest();
	}
	//用户登出
	public void logout() throws Exception
	{
		params.put("M", "User.LogOut");
		this.executeRequest();
	}
	
	//修改用户头像
	public void updateUserAvatar(File imageFile, String session, String uid) throws ClientProtocolException, IOException, Exception
	{
		params.put("M", "User.Update.Avatar");
		params.put("S", session);
		params.put("U", uid);
		
		String queryStr = this.queryString();
		String hashStr = this.hashQueryString(queryStr);
		params.put("H", hashStr);
		
		Set<Map.Entry<String, String>> paramsSet = new HashSet<Map.Entry<String, String>>(params.entrySet());
		UploadImage(APIDomain, paramsSet, imageFile);
		
		HttpResponse response = httpClient.execute(post);
		
		switch(response.getStatusLine().getStatusCode())
		{
			case 200:
			{
				this.requestFinished(response);
			}
				break;
			default:
			{
				//出现网络错误
				this.setHasError(true);
				this.setResponseStatusCode(response.getStatusLine().getStatusCode());
				this.setErrorDesc(response.getStatusLine().getReasonPhrase());
				Log.v("REQUESTFAILED", this.getErrorDesc());
			}
		}
	}
	
	//修改用户资料
	public void updateUser(String phone, String email, String qq, String weibo, String session, String uid) throws JSONException, Exception
	{
		params.put("M", "User.Update");
		params.put("S", session);
		params.put("U", uid);
		
		JSONObject json = new JSONObject();
		
		json.put("Email", email);
		json.put("SinaWeiBo", weibo);
		json.put("Phone", phone);
		json.put("QQ", qq);
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("User", json);
		
		Log.v("userJSON", jsonObject.toString());
		
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("jsonString",jsonObject.toString()));
		post.setEntity(new UrlEncodedFormEntity(pairs));
	}
	
	//获取用户信息
	public void getUser()
	{
		params.put("M", "User.Get");
	}
	
	//用来上传图片
	public void UploadImage(String basicURL, Set<Map.Entry<String, String>> params, File imageFile) throws Exception
	{
		String url = basicURL;
		post.setURI(new URI(url));
		
		 //设置MultipartEntity的模式为BROWSER_COMPATIBLE
		 MultipartEntity multiEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		 
		 //图片内容
		 FileBody binaryContent = new FileBody(imageFile);
         multiEntity.addPart("Image", binaryContent);
         
         //构造表单参数
         for(Map.Entry<String, String> param : params) 
         {

         	multiEntity.addPart(param.getKey(),
         			new StringBody(param.getValue()));
         }
         
         post.setEntity(multiEntity);
	}
	
	//获取校园新闻列表
	public void getNewsList(int page) throws Exception
	{
		params.put("M", "News.GetList");
		if(page <= 0)
			page = 1;
		String pageStr = String.valueOf(page);
		params.put("P", pageStr);
		this.executeRequest();
	}
	
	//获取某个频道的活动列表
	public void getActivitiesWithChannelIds(String channelId, String sortType, int page)
	{
		
	}
	
	//喜欢某个活动
	public void likeActivity(String activityId) throws Exception
	{
		params.put("M", "Activity.Like");
		params.put("Id", activityId);
		this.executeRequest();
	}
	
	//添加活动到日程列表
	public void scheduleActivity(String activityId) throws Exception
	{
		params.put("M", "Activity.Schedule");
		params.put("Id", activityId);
		this.executeRequest();
	}
	
	//关注某个活动
	public void favoriteActivity(String activityId) throws Exception
	{
		params.put("M", "Activity.Favorite");
		params.put("Id", activityId);
		this.executeRequest();
	}
	
	//取消喜欢某个活动
	public void unlikeActivity(String activityId) throws Exception
	{
		params.put("M", "Activity.UnLike");
		params.put("Id", activityId);
		this.executeRequest();
	}
	
	//从日程删除某个活动
	public void unscheduleActivity(String activityId) throws Exception
	{
		params.put("M", "Activity.UnScheduleLike");
		params.put("Id", activityId);
		this.executeRequest();
	}
	
	//取消关注某个活动
	public void unfavoriteActivity(String activityId) throws Exception
	{
		params.put("M", "Activity.UnFavoriteLike");
		params.put("Id", activityId);
		this.executeRequest();
	}
	
	//获取课程表
	public void getCourse() throws Exception
	{
		params.put("M", "TimeTable.Get");
		this.executeRequest();
	}
	
	//喜欢某个课程
	public void likeCourse(String courseId) throws Exception
	{
		params.put("M", "Course.Like");
		params.put("Id", courseId);
		this.executeRequest();
	}
	
	//取消喜欢某个课程
	public void unlikeCourse(String courseId) throws Exception
	{
		params.put("M", "Course.UnLike");
		params.put("Id", courseId);
		this.executeRequest();
	}
	
	//
	public void getFavoriteList() throws Exception
	{
		params.put("M", "Favorite.Get");
		this.executeRequest();
	}
	
	//
	public void readNews(String newsId) throws Exception
	{
		params.put("M", "News.Get");
		params.put("Id", "newsId");
		this.executeRequest();
	}
	
	//userService,用来调用dao层的方法
	public void userService() throws Exception
	{
		if(!this.isHasError())
		{
			JSONObject responseData = new JSONObject(this.getResponseStr());
			JSONObject user = responseData.getJSONObject("User");
			
			UserFactory userDao = new UserFactory();
			userDao.create(user);
		}
	}
	
	//some set/get methods
	public String getErrorDesc() 
	{
		return errorDesc;
	}
	public void setErrorDesc(String errorDesc) 
	{
		this.errorDesc = errorDesc;
	}
	public boolean isHasError() 
	{
		return hasError;
	}
	public void setHasError(boolean hasError) 
	{
		this.hasError = hasError;
	}
	public int getResponseStatusCode() 
	{
		return responseStatusCode;
	}
	public void setResponseStatusCode(int responseStatusCode) 
	{
		this.responseStatusCode = responseStatusCode;
	}
	public String getResponseStr() 
	{
		return responseStr;
	}
	public void setResponseStr(String responseStr) 
	{
		this.responseStr = responseStr;
	}
	public Map<String, String> getData() 
	{
		return data;
	}
	public void setData(Map<String, String> data) 
	{
		this.data = data;
	}
	public String getSession() 
	{
		return session;
	}
	public void setSession(String session) 
	{
		this.session = session;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
}
