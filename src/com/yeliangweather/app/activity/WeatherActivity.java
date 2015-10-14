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
	 * ������ʾ������
	 */
	private TextView cityNameText;
	/**
	 * ������ʾ����ʱ��
	 */
	private TextView publishText;
	/**
	 * ������ʾ����������Ϣ
	 */
	private TextView weatherDsepText;
	/**
	 * ������ʾ����1
	 */
	private TextView temp1Text;
	/**
	 * ������ʾ����2
	 */
	private TextView temp2Text;
	/**
	 * ������ʾ��ǰ����
	 */
	private TextView currentDateText;
	/**
	 * �л����а�ť
	 */
	private Button switchCity;
	/**
	 * ����������ť
	 */
	private Button refreshWeather;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		//��ʼ�����ؼ�
		weatherInfoLayout = (LinearLayout)findViewById(R.id.weather_info_layout);
		cityNameText = (TextView)findViewById(R.id.city_name);
		publishText = (TextView)findViewById(R.id.publish_text);
		weatherDsepText = (TextView)findViewById(R.id.weather_desp);
		temp1Text = (TextView)findViewById(R.id.temp1);
		temp2Text = (TextView)findViewById(R.id.temp2);
		currentDateText = (TextView)findViewById(R.id.current_data);
		switchCity = (Button)findViewById(R.id.switch_city);
		refreshWeather = (Button)findViewById(R.id.refresh_weather);
		//��ӵ���¼�
		switchCity.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);
		//����һ��ҳ��ת������ȡ�����е�countyCode�ؼ�����
		String countyCode = getIntent().getStringExtra("county_code");
		if(!TextUtils.isEmpty(countyCode))
		{
			//���ؼ������Ǿ�ȥ��ѯ����
			publishText.setText("ͬ����......");
			//ʹ�ÿؼ����ɼ���״̬
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			//���ؼ����Ŵ������з�������ѯ����Ӧ����������
			queryWeatherCode(countyCode);
		}else
		{
			//û���ؼ�����ʱ��ֱ����ʾ��������
			showWeather();		
		}	
	}
	
	/**
	 * �����ؼ����Ų�ѯ����Ӧ����������
	 */
	private void queryWeatherCode(String countyCode)
	{
		//ƴװһ����ַ������queryFromServer()��������ѯ��������
		String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
		queryFromServer(address, "countyCode");	
	}
	
	/**
	 * �����������Ų�ѯ����Ӧ��������Ϣ
	 */
	private void queryWeatherInfo(String weatherCode)
	{
		String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		queryFromServer(address, "weatherCode");
	}
	
	/**
	 * ���ݴ���ĵ�ַ������ȥ���������ѯ�������Ż���������Ϣ
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
					//����������ؼ����ŵĲ�ѯ
					if(!TextUtils.isEmpty(response))
					{
						//�ӷ��������ص������н�������������
						String[] array = response.split("\\|");
						if(array != null && array.length == 2)
						{
							//���ص����ݵ������ǣ�190404|101190404�����������ǵڶ���
							String weatherCode = array[1];
							//�����������Ų�ѯ��Ӧ��������Ϣ
							queryWeatherInfo(weatherCode);
						}		
					}	
				}else if ("weatherCode".equals(type)) 
				{
					//����������������ŵĲ�ѯ
					//������������ص�������Ϣ
					Utility.handleWeatherResponse(WeatherActivity.this, response);
					//�������̷߳������߳�
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
						publishText.setText("ͬ��ʧ��");					
					}
				});			
			}
		});	
	}
	
	/**
	 * ��SharedPreferences�ļ��ж�ȡ�洢��������Ϣ������ʾ��������
	 */
	private void showWeather()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//�����еõ���������Ϣ��ʾ��������
		cityNameText.setText(prefs.getString("city_name", ""));
		temp1Text.setText(prefs.getString("temp1", ""));
		temp2Text.setText(prefs.getString("temp2", ""));
		weatherDsepText.setText(prefs.getString("weather_desp", ""));
		publishText.setText("����" + prefs.getString("publish_time", "") + "����");
		currentDateText.setText(prefs.getString("current_date", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		//һ��ѡ����ĳ�����в��ɹ���������֮��AutoUpdateService�ͻ�һֱ�ں�̨���У�����֤ÿ8Сʱ����һ������
		Intent intent = new Intent(this, AutoUpdateService.class);
		startService(intent);
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.switch_city:
			//�л����У�������ChooseAreaActivity����ڴ�һ�����ݹ�ȥ�������ж��Ƿ��WeatherActivity��ת������
			Intent intent = new Intent(this, ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			break;
			
		case R.id.refresh_weather:
			//�����������ݣ��ӱ����ļ���ȡ��weatherCode������queryWeatherInfo()��������ѯ����
			publishText.setText("����ͬ����......");
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
