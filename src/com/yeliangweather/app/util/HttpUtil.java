package com.yeliangweather.app.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil
{
	public static void sendHttpRequest(final String address, 
			final HttpCallbackListener listener)
	{
		//访问网络开启子线程
		new Thread(new Runnable()
		{
			
			@Override
			public void run()
			{
				//通过HttpURLConnection来访问网络
				HttpURLConnection connection = null;
				try
				{
					URL url = new URL(address);
					connection = (HttpURLConnection) url.openConnection();
					//连接配置
					connection.setRequestMethod("GET");
					connection.setConnectTimeout(8000);
					connection.setReadTimeout(8000);
					InputStream in = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));
					StringBuilder response = new StringBuilder();
					String line;
					while((line = reader.readLine()) != null)
					{
						response.append(line);
					}
					if(listener != null)
					{
						//回调onFinish()方法
						listener.onFinish(response.toString());
					}	
				} catch (Exception e)
				{
					if(listener != null)
					{
						//回调onError()方法
						listener.onError(e);
					}
				}finally
				{
					if(connection != null)
					{
						connection.disconnect();
					}
				}
			}
		}).start();	
	}	
}
