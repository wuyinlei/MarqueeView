package com.mingchu.marqueeview;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Random;

public class AutoScrollAdapter extends BaseAdapter implements AutoScrollListView.AutoScroll {

	//用于设置随机颜色
	private Random random = new Random();
	//布局管理器
	private LayoutInflater mLayoutInflater;
	@Override
	public int getCount() {
		return 5;  //返回的最多的数据
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			if (mLayoutInflater == null) {
				mLayoutInflater = LayoutInflater.from(parent.getContext());
			}
			convertView = mLayoutInflater.inflate(R.layout.list_item, parent,false);
			viewHolder = new ViewHolder();
			viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
			convertView.setTag(viewHolder);
		}else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		convertView.setBackgroundColor(Color.argb(100, random.nextInt(255), random.nextInt(255), random.nextInt(255)));
		viewHolder.tvTitle.setText(position + "   标题");
		return convertView;
	}

	@Override
	public int getListItemHeight(Context context) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, context.getResources().getDisplayMetrics());
	}

	@Override
	public int getImmovableCount() {
		return 1;  //显示滚动的item 的个数
	}


	class ViewHolder{
		public TextView tvTitle;
	}
	
}
