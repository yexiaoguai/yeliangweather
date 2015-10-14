package com.yeliangweather.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yeliangweather.app.R;
import com.yeliangweather.app.service.AutoUpdateService;
import com.yeliangweather.app.util.HttpCallbackListener;
import com.yeliangweather.app.util.HttpUtil;
import com.yeliangweather.app.util.Utility;

public class WeatherActivity extends Activity implements OnClickListener
{
	private LinearLayout weatherInfoLayout;
	
	/**
	 * 用于显示城市名
	 */
	private TextView cityNameText;
	/**
	 * 用于显示发布时间
	 */
	private TextView publishText;
	/**
	 * 用于显示天气描述信息
	 */
	private TextView weatherDsepText;
	/**
	 * 用于显示气温1
	 */
	private TextView temp1Text;
	/**
	 * 用于显示气温2
	 */
	private TextView temp2Text;
	/**
	 * 用于显示当前日期
	 */
	private TextView currentDateText;
	/**
	 * 切换城市按钮
	 */
	private Button switchCity;
	/**
	 * 更新天气按钮
	 */
	private Button refreshWeather;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		//初始化各控件
		weatherInfoLayout = (LinearLayout)findViewById(R.id.weather_info_layout);
		cityNameText = (TextView)findViewById(R.id.city_name);
		publishText = (TextView)findViewById(R.id.publish_text);
		weatherDsepText = (TextView)findViewById(R.id.weather_desp);
		temp1Text = (TextView)findViewById(R.id.temp1);
		temp2Text = (TextView)findViewById(R.id.temp2);
		currentDateText = (TextView)findViewById(R.id.current_data);
		switchCity = (Button)findViewById(R.id.switch_city);
		refreshWeather = (Button)findViewById(R.id.refresh_weather);
		//添加点击事件
		switchCity.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);
		//从上一个页面转过来，取出其中的countyCode县级代号
		String countyCode = getIntent().getStringExtra("county_code");
		if(!TextUtils.isEmpty(countyCode))
		{
			//有县级代号是就去查询天气
			publishText.setText("同步中......");
			//使得控件不可见的状态
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			//将县级代号传入下列方法，查询到相应的天气代号
			queryWeatherCode(countyCode);
		}else
		{
			//没有县级代号时就直接显示本地天气
			showWeather();		
		}	
	}
	
	/**
	 * 根据县级代号查询所对应的天气代号
	 */
	private void queryWeatherCode(String countyCode)
	{
		//拼装一个地址，调用queryFromServer()方法来查询天气代号
		String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
		queryFromServer(address, "countyCode");	
	}
	
	/**
	 * 根据天气代号查询所对应的天气信息
	 */
	private void queryWeatherInfo(String weatherCode)
	{
		String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		queryFromServer(address, "weatherCode");
	}
	
	/**
	 * 根据传入的地址和类型去向服务器查询天气代号或者天气信息
	 */
	private void queryFromServer(final String address, final String type)
	{
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener()
		{
			
			@Override
			public void onFinish(String response)
			{
				if("countyCode".equals(type))
				{
					//如果类型是县级代号的查询
					if(!TextUtils.isEmpty(response))
					{
						//从服务器返回的数据中解析出天气代号
						String[] array = response.split("\\|");
						if(array != null && array.length == 2)
						{
							//返回的数据的例子是：190404|101190404，天气代号是第二个
							String weatherCode = array[1];
							//根据天气代号查询对应的天气信息
							queryWeatherInfo(weatherCode);
						}		
					}	
				}else if ("weatherCode".equals(type)) 
				{
					//如果类型是天气代号的查询
					//处理服务器返回的天气信息
					Utility.handleWeatherResponse(WeatherActivity.this, response);
					//跳出子线程返回主线程
					runOnUiThread(new Runnable()
					{
						
						@Override
						public void run()
						{
							showWeather();						
						}
					});
				}				
			}
			
			@Override
			public void onError(Exception e)
			{
				runOnUiThread(new Runnable()
				{
					
					@Override
					public void run()
					{
						publishText.setText("同步失败");					
					}
				});			
			}
		});	
	}
	
	/**
	 * 从SharedPreferences文件中读取存储的天气信息，并显示到界面上
	 */
	private void showWeather()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//将所有得到的天气信息显示到界面上
		cityNameText.setText(prefs.getString("city_name", ""));
		temp1Text.setText(prefs.getString("temp1", ""));
		temp2Text.setText(prefs.getString("temp2", ""));
		weatherDsepText.setText(prefs.getString("weather_desp", ""));
		publishText.setText("今天" + prefs.getString("publish_time", "") + "发布");
		currentDateText.setText(prefs.getString("current_date", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		//一旦选中了某个城市并成功更新天气之后，AutoUpdateService就会一直在后台运行，并保证每8小时更新一次天气
		Intent intent = new Intent(this, AutoUpdateService.class);
		startService(intent);
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.switch_city:
			//切换城市，就启动ChooseAreaActivity活动，在带一个数据过去，用来判断是否从WeatherActivity跳转过来的
			Intent intent = new Intent(this, ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			break;
			
		case R.id.refresh_weather:
			//更新天气数据，从本地文件中取出weatherCode，调用queryWeatherInfo()方法来查询天气
			publishText.setText("数据同步中......");
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String weatherCode = prefs.getString("weather_code", "");
			if(!TextUtils.isEmpty(weatherCode))
			{
				queryWeatherInfo(weatherCode);
			}
			break;
			
		default:
			break;
		}	
	}
}
