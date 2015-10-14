package com.yeliangweather.app.receiver;

import com.yeliangweather.app.service.AutoUpdateService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoUpdateReceiver extends BroadcastReceiver
{

	/**
	 * 时间间隔8小时，每8个小时就接到广播并且执行下列方法，开启服务
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Intent i = new Intent(context, AutoUpdateService.class);
		context.startService(i);
	}

}
