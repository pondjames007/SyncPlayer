package com.example.sync_player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class ListOfClientAdapter extends BaseAdapter {

	private LayoutInflater mInflater;

	private List<Map<String, String>> listData;
	
	private Map<String, Integer> allValues=new HashMap<String, Integer>();
	private Map<String, Boolean> isChecked=new HashMap<String, Boolean>();	

	private Map<Integer, Map<String, String>> selectMap = new HashMap<Integer, Map<String, String>>();
	private Context mContext;

	private class ViewHolder {
		public ImageView img;
		public TextView title;
		public CheckBox checkBox;
		public Spinner roleSpinner;
	}

	public ListOfClientAdapter(Context context, List<Map<String, String>> listData) {
		this.mInflater = LayoutInflater.from(context);
		this.listData = listData;
		this.mContext=context;
	}
	
//	/*****************************************************
//	 * put the add Item into list
//	 ******************************************************/
//	
//	private void putAllValues() {
//		for (String str : checkListName) {
//			allValues.put(str, 0);
//		}
//	}
//	
//	public void setAllValues(Map<String, Integer> allValues){
//		this.allValues = allValues;
//	}
	

	@Override
	public int getCount() {
		return listData.size();
	}

	@Override
	public Object getItem(int position) {
		return listData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.server_list_of_client, null);
			final View view = convertView;
			//holder.img = (ImageView) convertView.findViewById(R.id.img);
			holder.title = (TextView) convertView.findViewById(R.id.itemTitle);
			//checkBox
			holder.checkBox = (CheckBox) convertView.findViewById(R.id.cb);
			holder.checkBox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (selectMap.get(position) != null) {
						selectMap.remove(position);
						//isChecked.put(listData.get(position).get("itemTitle"),true);
					} else {
						selectMap.put(position, listData.get(position));
					}
					Toast.makeText(view.getContext(),
							"you choose:" + selectMap.size() + "items.",
							Toast.LENGTH_LONG).show();
					isChecked.put(listData.get(position).get("itemTitle"),!isChecked.get(listData.get(position).get("itemTitle")) );
				}//onClick				
			});
			//spinner
			holder.roleSpinner=(Spinner) convertView.findViewById(R.id.client_role_assign);
			ArrayAdapter<String> RoleItemAdapter = new ArrayAdapter<String>(mContext,android.R.layout.simple_spinner_item,
					new String[]{"video","music only","left sound track","right sound track"});			
			RoleItemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			//spinner.setAdapter(adapter);
			//SpinnerAdapter SpinnerAdapter = new SpinnerAdapter(mContext);
			holder.roleSpinner.setAdapter(RoleItemAdapter);
			holder.roleSpinner.setOnItemSelectedListener(new ItemClickSelectListener(
							holder.roleSpinner));
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}//if contvertView is null
		
		//use string as the prompt of the spinner
		String checkedName=listData.get(position).get("itemTitle");		
		holder.roleSpinner.setPrompt(checkedName);		
		int spinnerOptionPosition=0;
		try {
			spinnerOptionPosition = allValues.get(checkedName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("CheckList", checkedName + " = = " + spinnerOptionPosition);
		holder.roleSpinner.setSelection(spinnerOptionPosition);

		//holder.img.setBackgroundResource(R.drawable.ic_launcher);
		holder.title.setText(listData.get(position).get("id"));

		if (selectMap.get(position) != null) {
			holder.checkBox.setChecked(false);
		} else {
			holder.checkBox.setChecked(true);
		}

		return convertView;
	}
	/**************************************************************************
	 * Spinner ItemClickSelectListener
	 **************************************************************************/
	
	private class ItemClickSelectListener implements OnItemSelectedListener {
		Spinner checkinfo_item_value ;

		public ItemClickSelectListener(Spinner checkinfo_item_value) {
			this.checkinfo_item_value = checkinfo_item_value;
		}

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			//关键代码
			allValues.put(checkinfo_item_value.getPrompt().toString(), position);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {

		}
	}//ItemClickSelectListener
	
	/**************************************************************
	 *return value for all spinner Value 
	 **************************************************************/
	public Map<String,Integer> getSelectValues() {
		return allValues;
	}
	
	public Map<String,Boolean> getCheck() {
		return isChecked;
	}
	
	/***************************************************************
	 * Add data to the list
	 ***************************************************************/
	public void addItem(String clientName,String id){
		List<Map<String, String>> mylist = new ArrayList<Map<String, String>>();
		Map<String, String> map = new HashMap<String, String>();
				
		map.put("id", clientName);
		map.put("itemTitle", clientName + id);
		
		allValues.put(clientName + id, 0); //put clientName into allValue map
		listData.add(map);
		isChecked.put(clientName+id, true);
		this.notifyDataSetChanged();
	}
	
}//ListOfClientAdapter





