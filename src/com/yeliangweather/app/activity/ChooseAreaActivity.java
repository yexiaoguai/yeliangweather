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
	 * ʡ�б�
	 */
	private List<Province> provinceList;
	/**
	 * ���б�
	 */
	private List<City> cityList;
	/**
	 * ���б�
	 */
	private List<County> countyList;
	/**
	 * ѡ�е�ʡ��
	 */
	private Province selectedProvince;
	/**
	 * ѡ�еĳ���
	 */
	private City selectedCity;
	/**
	 * ��ǰѡ�еļ���
	 */
	private int currentLevel;
	/**
	 * �Ƿ��WeatherActivity����ת����
	 */
	private boolean isFromWeatherActivity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//����Ǵ�WeatherActivity��ת�����ģ�isFromWeatherActivityΪtrue���������ȡĬ��ֵfalse
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		//���֮ǰ�Ѿ��鿴��ĳ�����е�������Ϣ��ֱ����ת����ҳ�棬����ֱ��return
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//�Ѿ�ѡ���˳����Ҳ��Ǵ�WeatherActivity��ת�������Ż�ֱ������WeatherActivity����
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
		//��adapter����ΪListView��������
		listView.setAdapter(adapter);
		//��ȡYeliangWeatherDB��ʵ�����������ݿ�ͱ�
		yeliangWeatherDB = YeliangWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id)
			{
				//�жϵ�ǰѡ�еļ����Ƿ���ʡ��
				if(currentLevel == LEVEL_PROVINCE)
				{
					//��ѡ�е�ʡ��(����Ϊposition)��ֵ��selectedProvince
					selectedProvince = provinceList.get(position);
					//�����м�����
					queryCities();
				}else if (currentLevel == LEVEL_CITY)
				{
					//�����ǰ���漶�����м���ִ�������߼�
					selectedCity = cityList.get(position);
					queryCounties();
				}else if (currentLevel == LEVEL_COUNTY) 
				{
					//�����ǰ���漶�����ؼ���ִ�������߼�
					String countyCode = countyList.get(position).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}				
			}
		});
		//����ʡ������
		queryProvince();
	}
	
	/**
	 * ��ѯȫ�����е�ʡ�����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
	 */
	private void queryProvince()
	{
		//�����ݿ��ж�ȡʡ����Ϣ
		provinceList = yeliangWeatherDB.loadProvince();
		//�ж����ݿ����Ƿ���ʡ������
		if(provinceList.size() > 0)
		{
			dataList.clear();
			for(Province province : provinceList)
			{
				//�����ݿ��ж�ȡ��ʡ��������ӵ��ַ�������
				dataList.add(province.getProvinceName());			
			}
			//������������notifyDataSetChanged()����������֪ͨ�б�����ݷ����˱仯
			//������������Ϣ���ܹ���ListView����ʾ����
			adapter.notifyDataSetChanged();
			//���ŵ���ListView��setSelection()��������ʾ�����ݶ�λ�ڵ�һ��
			listView.setSelection(0);
			titleText.setText("�й�");
			//����ǰѡ�еļ�������Ϊʡ�ݼ���
			currentLevel = LEVEL_PROVINCE;
		}else 
		{
			//������ݿ������ݣ��͵��������ϲ�ѯ������Ϊ���ź�����
			queryFromServer(null, "province");
		}		
	}
	
	/**
	 * ��ѯѡ��ʡ�����е��У����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
	 */
	private void queryCities()
	{
		//ͨ��ʡ��ID��ѯ��ʡ�����е���
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
			//����¼����Ѿ����������ʡ�ݵ�ʵ������ֵ��selectedProvince
			titleText.setText(selectedProvince.getProvinceName());
			//����ǰѡ�еļ�������Ϊ�м���
			currentLevel = LEVEL_CITY;
		}else 
		{
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}		
	}
	
	/**
	 * ��ѯѡ���������е��أ����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
	 */
	private void queryCounties()
	{
		//����ѡ�е���ID��ѯ���������е���
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
			//����ǰѡ�еļ�������Ϊ�ؼ���
			currentLevel = LEVEL_COUNTY;
		}else
		{
			queryFromServer(selectedCity.getCityCode(), "county");
		}	
	}
	
	/**
	 * ���ݴ���Ĵ��ź����ʹӷ������ϲ�ѯʡ��������
	 */
	private void queryFromServer(final String code, final String type)
	{
		String address;
		if(!TextUtils.isEmpty(code))
		{
			address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		}else 
		{
			//���code����null�����ʵĵ�ַ���£����ص���ʡ��
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		//ȷ���˲�ѯ��ַ֮�󣬽������͵���HttpUtil��sendHttpRequest()���������������������
		//��Ӧ�����ݻ�ص���onFinish()������
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener()
		{
			
			@Override
			public void onFinish(String response)
			{
				boolean result = false;
				if("province".equals(type))
				{
					//����Utility��handleProvincesResponse()�����������ʹ�����������ص����ݣ����Ҵ洢�����ݿ���
					result = Utility.handleProvincesResponse(yeliangWeatherDB, response);
				}else if("city".equals(type))
				{
					result = Utility.handleCitiesResponse(yeliangWeatherDB, response, selectedProvince.getId());
				}else if("county".equals(type))
				{
					result = Utility.handleCountiesResponse(yeliangWeatherDB, response, selectedCity.getId());
				}
				//������ݶ������������ʹ�����ִ��queryXXXX()���������¼��������ڽ���
				//�����漰��UI��������Ҫ�����߳��л������߳�
				if(result)
				{
					//ͨ��runOnUiThread()�����ص����̴߳����߼�
					runOnUiThread(new Runnable()
					{
						
						@Override
						public void run()
						{
							//�رս��ȶԻ���
							closeProgressDialog();
							if("province".equals(type))
							{
								//ִ�в�ѯ���������¼���ҳ��
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
				//ͨ��runOnUiThread()�����ص����̴߳����߼�
				runOnUiThread(new Runnable()
				{
					
					@Override
					public void run()
					{
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "�������ݼ���ʧ��", Toast.LENGTH_SHORT).show();
					}
				});				
			}
		});	
	}
	
	/**
	 * ��ʾ���ȶԻ���
	 */
	private void showProgressDialog()
	{
		if(progressDialog == null)
		{
			//�����Ի���ʵ��
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("���ڼ���......");
			//����Ի����ⲿ���Ի��򲻻���ʧ
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();	
	}
	
	/**
	 * �رս��ȶԻ���
	 */
	private void closeProgressDialog()
	{
		if(progressDialog != null)
		{
			progressDialog.dismiss();
		}	
	}
	
	/**
	 * ����Back���������ݵ�ǰ�ļ������жϣ���ʱӦ�÷������б�ʡ�б�����ֱ���˳�
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
			//������Back��ʱ������Ǵ�WeatherActivity��ת�����ģ���Ӧ�����»ص�WeatherActivity
			if(isFromWeatherActivity)
			{
				Intent intent = new Intent(this, WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}	
	}	
}