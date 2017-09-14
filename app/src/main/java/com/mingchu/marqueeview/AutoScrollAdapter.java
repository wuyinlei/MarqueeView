package com.mingchu.marqueeview;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoScrollAdapter extends BaseAdapter implements AutoScrollListView.AutoScroll {

	private List<MarqueeBean> mMarqueeBeen = new ArrayList<>();
	private Context mContext;

	public AutoScrollAdapter(List<MarqueeBean> marqueeBeen,Context context) {
		mMarqueeBeen = marqueeBeen;
		this.mContext = context;
	}

	//用于设置随机颜色
	private Random random = new Random();
	//布局管理器
	private LayoutInflater mLayoutInflater;
	@Override
	public int getCount() {
		return mMarqueeBeen.size();  //返回的最多的数据
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

		MarqueeBean marqueeBean = mMarqueeBeen.get(position);

		ViewHolder viewHolder;
		if (convertView == null) {
			if (mLayoutInflater == null) {
				mLayoutInflater = LayoutInflater.from(parent.getContext());
			}
			convertView = mLayoutInflater.inflate(R.layout.list_item, parent,false);
			viewHolder = new ViewHolder();
			viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
			viewHolder.ivImage = (ImageView) convertView.findViewById(R.id.iv_content);
			viewHolder.tvSubTitle = (TextView) convertView.findViewById(R.id.tv_sub_title);
			convertView.setTag(viewHolder);
		}else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		convertView.setBackgroundColor(Color.argb(100, random.nextInt(255), random.nextInt(255), random.nextInt(255)));
		viewHolder.tvTitle.setText(marqueeBean.getTitle());
		viewHolder.tvSubTitle.setText(marqueeBean.getSubtitle());
		Glide.with(mContext).load(marqueeBean.getImgurl()).transform(new GlideCircleTransform(mContext)).into(viewHolder.ivImage);
		return convertView;
	}

	//获取到当前滚动视图的高度
	@Override
	public int getListItemHeight(Context context) {
		//在这里我们要获取到我们的布局的高度  才能实现一些
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
	}


	@Override
	public int getVisiableCount() {
		return 3;  //显示滚动的item 的个数
	}

	class ViewHolder{
		public ImageView ivImage;
		public TextView tvTitle,tvSubTitle;
	}
	
}
