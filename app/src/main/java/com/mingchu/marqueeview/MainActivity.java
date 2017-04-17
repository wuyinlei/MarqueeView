package com.mingchu.marqueeview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<MarqueeBean> mMarqueeBeen = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();

        AutoScrollListView listView = (AutoScrollListView) findViewById(R.id.list_view);
        listView.setAdapter(new AutoScrollAdapter(mMarqueeBeen,this));
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Toast.makeText(getApplicationContext(), "点击条目 " + position,
                        Toast.LENGTH_SHORT).show();
            }
        });
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                Toast.makeText(getApplicationContext(), "长按条目 " + position,
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void initData() {
        MarqueeBean beanOne = new MarqueeBean();
        beanOne.setTitle("你要帮助我哈");
        beanOne.setSubtitle("我为啥要帮助你,你说呢");
        beanOne.setImgurl("http://icon.xinliji.me//avatar_0_63.png");
        mMarqueeBeen.add(beanOne);

        MarqueeBean beanTwo = new MarqueeBean();
        beanTwo.setTitle("你是不是傻");
        beanTwo.setSubtitle("我就是傻,你能怎么着呢");
        beanTwo.setImgurl("http://icon.xinliji.me//avatar_0_63.png");
        mMarqueeBeen.add(beanTwo);

        MarqueeBean beanThree = new MarqueeBean();
        beanThree.setTitle("鸣狐工作");
        beanThree.setSubtitle("这样的工作就是好");
        beanThree.setImgurl("http://icon.xinliji.me//avatar_0_63.png");
        mMarqueeBeen.add(beanThree);
    }
}
