package com.yeliangweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yeliangweather.app.R;
import com.yeliangweather.app.db.YeliangWeatherDB;
import com.yeliangweather.app.model.City;
import com.yeliangweather.app.model.County;
import com.yeliangweather.app.model.Province;
import com.yeliangweather.app.util.HttpCallbackListener;
import com.yeliangweather.app.util.HttpUtil;
import com.yeliangweather.app.util.Utility;

public class ChooseAreaActivity extends Activity
{
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private YeliangWeatherDB yeliangWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	
	/**
	 * 省列表
	 */
	private List<Province> provinceList;
	/**
	 * 市列表
	 */
	private List<City> cityList;
	/**
	 * 县列表
	 */
	private List<County> countyList;
	/**
	 * 选中的省份
	 */
	private Province selectedProvince;
	/**
	 * 选中的城市
	 */
	private City selectedCity;
	/**
	 * 当前选中的级别
	 */
	private int currentLevel;
	/**
	 * 是否从WeatherActivity中跳转过来
	 */
	private boolean isFromWeatherActivity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//如果是从WeatherActivity跳转过来的，isFromWeatherActivity为true，如果不是取默认值false
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		//如果之前已经查看过某个城市的天气信息，直接跳转到该页面，并且直接return
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//已经选择了城市且不是从WeatherActivity跳转过来，才会直接跳到WeatherActivity界面
		if(prefs.getBoolean("city_selected", false) && !isFromWeatherActivity)
		{
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		
		listView = (ListView)findViewById(R.id.list_view);
		titleText = (TextView)findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		//将adapter设置为ListView的适配器
		listView.setAdapter(adapter);
		//获取YeliangWeatherDB的实例，创建数据库和表
		yeliangWeatherDB = YeliangWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id)
			{
				//判断当前选中的级别是否是省级
				if(currentLevel == LEVEL_PROVINCE)
				{
					//将选中的省份(行数为position)赋值给selectedProvince
					selectedProvince = provinceList.get(position);
					//加载市级数据
					queryCities();
				}else if (currentLevel == LEVEL_CITY)
				{
					//如果当前界面级别是市级，执行以下逻辑
					selectedCity = cityList.get(position);
					queryCounties();
				}else if (currentLevel == LEVEL_COUNTY) 
				{
					//如果当前界面级别是县级，执行以下逻辑
					String countyCode = countyList.get(position).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}				
			}
		});
		//加载省级数据
		queryProvince();
	}
	
	/**
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
	 */
	private void queryProvince()
	{
		//从数据库中读取省的信息
		provinceList = yeliangWeatherDB.loadProvince();
		//判断数据库中是否有省份数据
		if(provinceList.size() > 0)
		{
			dataList.clear();
			for(Province province : provinceList)
			{
				//从数据库中读取的省份名称添加到字符串数组
				dataList.add(province.getProvinceName());			
			}
			//调用适配器的notifyDataSetChanged()方法，用于通知列表的数据发生了变化
			//这样新增的信息才能够在ListView中显示出来
			adapter.notifyDataSetChanged();
			//接着调用ListView的setSelection()方法将显示的数据定位在第一行
			listView.setSelection(0);
			titleText.setText("中国");
			//将当前选中的级别设置为省份级别
			currentLevel = LEVEL_PROVINCE;
		}else 
		{
			//如果数据库无数据，就到服务器上查询，参数为代号和类型
			queryFromServer(null, "province");
		}		
	}
	
	/**
	 * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
	 */
	private void queryCities()
	{
		//通过省份ID查询到省内所有的市
		cityList = yeliangWeatherDB.loadCities(selectedProvince.getId());
		if(cityList.size() > 0)
		{
			dataList.clear();
			for(City city : cityList)
			{
				dataList.add(city.getCityName());			
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			//点击事件中已经将“被点击省份的实例”赋值给selectedProvince
			titleText.setText(selectedProvince.getProvinceName());
			//将当前选中的级别设置为市级别
			currentLevel = LEVEL_CITY;
		}else 
		{
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}		
	}
	
	/**
	 * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
	 */
	private void queryCounties()
	{
		//根据选中的市ID查询到市内所有的县
		countyList = yeliangWeatherDB.loadCounties(selectedCity.getId());
		if(countyList.size() > 0)
		{
			dataList.clear();
			for(County county : countyList)
			{
				dataList.add(county.getCountyName());	
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			//将当前选中的级别设置为县级别
			currentLevel = LEVEL_COUNTY;
		}else
		{
			queryFromServer(selectedCity.getCityCode(), "county");
		}	
	}
	
	/**
	 * 根据传入的代号和类型从服务器上查询省市县数据
	 */
	private void queryFromServer(final String code, final String type)
	{
		String address;
		if(!TextUtils.isEmpty(code))
		{
			address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		}else 
		{
			//如果code等于null，访问的地址如下，返回的是省份
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		//确定了查询地址之后，接下来就调用HttpUtil的sendHttpRequest()方法来向服务器发送请求
		//相应的数据会回调到onFinish()方法中
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener()
		{
			
			@Override
			public void onFinish(String response)
			{
				boolean result = false;
				if("province".equals(type))
				{
					//调用Utility的handleProvincesResponse()方法来解析和处理服务器返回的数据，并且存储到数据库中
					result = Utility.handleProvincesResponse(yeliangWeatherDB, response);
				}else if("city".equals(type))
				{
					result = Utility.handleCitiesResponse(yeliangWeatherDB, response, selectedProvince.getId());
				}else if("county".equals(type))
				{
					result = Utility.handleCountiesResponse(yeliangWeatherDB, response, selectedCity.getId());
				}
				//如果数据都能正常解析和处理，则执行queryXXXX()方法来重新加载数据在界面
				//由于涉及到UI操作，需要从子线程切换到主线程
				if(result)
				{
					//通过runOnUiThread()方法回到主线程处理逻辑
					runOnUiThread(new Runnable()
					{
						
						@Override
						public void run()
						{
							//关闭进度对话框
							closeProgressDialog();
							if("province".equals(type))
							{
								//执行查询方法，重新加载页面
								queryProvince();
							}else if ("city".equals(type)) 
							{
								queryCities();
							}else if ("county".equals(type)) 
							{
								queryCounties();
							}						
						}
					});					
				}	
			}
			
			@Override
			public void onError(Exception e)
			{
				//通过runOnUiThread()方法回到主线程处理逻辑
				runOnUiThread(new Runnable()
				{
					
					@Override
					public void run()
					{
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "网络数据加载失败", Toast.LENGTH_SHORT).show();
					}
				});				
			}
		});	
	}
	
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog()
	{
		if(progressDialog == null)
		{
			//创建对话框实例
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载......");
			//点击对话框外部，对话框不会消失
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();	
	}
	
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog()
	{
		if(progressDialog != null)
		{
			progressDialog.dismiss();
		}	
	}
	
	/**
	 * 捕获Back按键，根据当前的级别来判断，此时应该返还市列表、省列表、还是直接退出
	 */
	@Override
	public void onBackPressed()
	{
		if(currentLevel == LEVEL_COUNTY)
		{
			queryCities();
		}else if (currentLevel == LEVEL_CITY) 
		{
			queryProvince();
		}else 
		{
			//当按下Back键时，如果是从WeatherActivity跳转过来的，则应该重新回到WeatherActivity
			if(isFromWeatherActivity)
			{
				Intent intent = new Intent(this, WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}	
	}	
}