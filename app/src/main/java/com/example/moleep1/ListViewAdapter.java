package com.example.moleep1;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ListViewAdapter extends BaseAdapter {

    Context mContext = null;
    LayoutInflater mLayoutInflater = null;
    ArrayList<list_item> list;

    public ListViewAdapter(Context context, ArrayList<list_item> listViewItemList){
        mContext = context;
        list = listViewItemList;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount(){
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = mLayoutInflater.inflate(R.layout.activity_list_item, null);
        TextView titleTextView = (TextView) view.findViewById(R.id.textView1);
        TextView descTextView = (TextView) view.findViewById(R.id.textView2);

        ImageView imageView = view.findViewById(R.id.imageView1);
        list_item item = list.get(position);
        titleTextView.setText(list.get(position).getName());
        descTextView.setText(list.get(position).getDesc());

        String imageUriStr = item.getImageUri();
        Uri uri = Uri.parse(imageUriStr);
        imageView.setImageURI(uri);
        return view;
    }

    public void updateData(ArrayList<list_item> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    public void setItems(ArrayList<list_item> newItems) {
        list.clear();           // 기존 목록 비움
        list.addAll(newItems);  // 새 데이터 추가
        notifyDataSetChanged();      // 리스트뷰 새로고침
    }


}
