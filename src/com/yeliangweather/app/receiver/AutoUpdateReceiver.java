package com.yeliangweather.app.receiver;

import com.yeliangweather.app.service.AutoUpdateService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoUpdateReceiver extends BroadcastReceiver
{

	/**
	 * ʱ����8Сʱ��ÿ8��Сʱ�ͽӵ��㲥����ִ�����з�������������
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Intent i = new Intent(context, AutoUpdateService.class);
		context.startService(i);
	}

}
