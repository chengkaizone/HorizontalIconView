package org.lance.main;

import java.util.ArrayList;
import java.util.List;

import org.lance.widget.HorizontalIconView;
import org.lance.widget.HorizontalIconView.IconOnClickListener;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

/**
 * 测试水平显示组件
 * 受到Android User Interface Design一书的想法和草案写的第一个自定义的滚动视图
 * 再次感谢本书的作者
 * @author ganchengkai
 *
 */
public class MainActivity extends ActionBarActivity implements IconOnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final Resources res=getResources();
		final List<Drawable> list=new ArrayList<Drawable>();
		
		list.add(res.getDrawable(R.drawable.icon_00));
		list.add(res.getDrawable(R.drawable.icon_01));
		list.add(res.getDrawable(R.drawable.icon_02));
		list.add(res.getDrawable(R.drawable.icon_03));
		list.add(res.getDrawable(R.drawable.icon_04));
		list.add(res.getDrawable(R.drawable.icon_05));
		list.add(res.getDrawable(R.drawable.icon_06));
		list.add(res.getDrawable(R.drawable.icon_07));
		list.add(res.getDrawable(R.drawable.icon_08));
		list.add(res.getDrawable(R.drawable.icon_09));
		list.add(res.getDrawable(R.drawable.icon_10));
		list.add(res.getDrawable(R.drawable.icon_11));
		list.add(res.getDrawable(R.drawable.icon_12));
		list.add(res.getDrawable(R.drawable.icon_13));
		list.add(res.getDrawable(R.drawable.icon_14));
		list.add(res.getDrawable(R.drawable.icon_15));
		list.add(res.getDrawable(R.drawable.icon_16));
		list.add(res.getDrawable(R.drawable.icon_17));
		list.add(res.getDrawable(R.drawable.icon_18));
		list.add(res.getDrawable(R.drawable.icon_19));
		
		HorizontalIconView view=(HorizontalIconView)findViewById(R.id.horizontal_view);
		view.setDrawables(list);
		view.setIconListener(this);
		
	}

	@Override
	public void onItemClick(int position) {
		System.out.println("position:"+position);
		Toast.makeText(this, "position:"+position, Toast.LENGTH_SHORT).show();
	}
	
}
