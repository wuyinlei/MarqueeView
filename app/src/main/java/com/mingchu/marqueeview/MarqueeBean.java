package com.mingchu.marqueeview;

/**
 * Created by wuyinlei on 2017/4/17.
 *
 * 滚动view的实体类
 */

public class MarqueeBean {

    //标题
    private String title;
    //子标题
    private String subtitle;
    //图片地址
    private String imgurl;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getImgurl() {
        return imgurl;
    }

    public void setImgurl(String imgurl) {
        this.imgurl = imgurl;
    }

    @Override
    public String toString() {
        return "MarqueeBean{" +
                "title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", imgurl='" + imgurl + '\'' +
                '}';
    }
}

