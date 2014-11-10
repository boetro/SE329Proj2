package app.se329.project2;

import java.util.ArrayList;
import java.util.ListIterator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import app.se329.project2.model.Inventory;
import app.se329.project2.model.InventoryItem;
import app.se329.project2.tools.DatabaseAccess;
import app.se329.project2.util.MyJsonUtil;
import app.se329.project2.views.ListItemView;

/**
 * Displays a list of InventoryItems.
 * @author wdArlen
 *
 */
public class ItemsFragment extends ProjectFragment implements OnClickListener {
	
	View rootView;
	Inventory inventory;
	private ListView itemsListView;
	private InventoryItemAdapter listViewAdapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_items, null,false);
		
		EditText input = (EditText) rootView.findViewById(R.id.inputSearch);
		input.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				listViewAdapter.getFilter().filter(s);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		});
		
		inventory = getSupportActivity().getCurrentInventory();
		
		Log.i("Inventory", "Items View for: "+inventory.getName());
		
		getInventoryItems();
		
		setUpItemsList();
		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.items_menu, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			getFragmentManager().popBackStack();
			return true;
		} else if (itemId == R.id.add_item_butt) {
			launchItemPopup(true, null, inventory.getItems().size()+1);
			return true;
		} else if (itemId == R.id.upload_butt) {
			MyJsonUtil.uploadInventory(inventory);
			return true;
		}else if (itemId == R.id.dload_butt) {
			downloadFromServer();
			return true;
		}
	    return super.onOptionsItemSelected(item);
	}

	private void downloadFromServer() {
		final String filename = inventory.getUser() + "_inv_" + inventory.getId();
		
		Log.i("Download", "Downloading "+filename+" from server...");
		new AsyncTask<String, Object, String>() {
			DatabaseAccess dbAccess = new DatabaseAccess();
			
			protected void onPreExecute() {

			};
			
			@Override
			protected String doInBackground(String... params) {
				return dbAccess.readFromServer(filename);
			}
			
			protected void onPostExecute(String result) {
				Log.i("Result", "Response from server: " + result);
				inventory.inflateInventory(rootView.getContext(), result);
				listViewAdapter.notifyDataSetChanged();
			}
		}.execute();
		
	}

	private void setUpItemsList() {
		
		itemsListView = (ListView) rootView.findViewById(R.id.items_list_view);
		listViewAdapter = new InventoryItemAdapter(rootView.getContext(), R.layout.view_list_item, inventory.getItems());
		itemsListView.setAdapter(listViewAdapter);
		
	}

	private ArrayList<InventoryItem> getInventoryItems() {
		
		if(inventory.getItems().size()==0)
			promptUser("Inventory", "It looks like you have no items! Click the add button at the top to create one!");
		
		return inventory.getItems();
	}
	
	private void launchItemPopup(boolean isNew,  InventoryItem item, int pos){

		PopupActivity.popup(this, getActivity(), 1, new ItemPopup(isNew, item, pos));
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.i("Result", "@@@ Returned with resultCode: " + resultCode);
		
		if (requestCode == 1) {
			if(resultCode == 777){ //save new item
				InventoryItem item = (InventoryItem) data.getExtras().getSerializable("item");
				addItem(item, inventory.getItems().size());
			}
			else if(resultCode == 666){ //delete item
				int toDelete = data.getExtras().getInt("to_delete");
				removeItem(toDelete);
			}
			else if(resultCode == 888){ //replace/edit item
				int toReplace = data.getExtras().getInt("to_replace");
				removeItem(toReplace);
				InventoryItem item = (InventoryItem) data.getExtras().getSerializable("item");
				addItem(item, toReplace);
			}
			else{
				Log.i("Item", "Cancel item add/edit. Result Code: "+ resultCode);
			}
			listViewAdapter.notifyDataSetChanged();
		}
	}

	private void addItem(InventoryItem item, int insertionIndex) {
		inventory.getItems().add(insertionIndex, item);
		inventory.saveInventoryItems(rootView.getContext());
		listViewAdapter.addToOriginal(item);
		//		listViewAdapter.add(item);
	}
	private void removeItem(int toDelete) {
		inventory.getItems().remove(toDelete);
		inventory.saveInventoryItems(rootView.getContext());
		listViewAdapter.removeFromOriginal(toDelete);
//		listViewAdapter.remove(listViewAdapter.getItem(toDelete));
	}
	
	private void promptUser(String title, String message){
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(rootView.getContext());
		alertDialogBuilder.setTitle(title);
		alertDialogBuilder
			.setMessage(message)
			.setCancelable(false)
			.setPositiveButton("Okay", null);
		alertDialogBuilder.create().show();
	}

	@Override
	public void onClick(View arg0) {}
	public class InventoryItemAdapter extends ArrayAdapter<InventoryItem> implements Filterable{

		Context context;
		int layoutResourceId;
		ArrayList<InventoryItem> items;
		ArrayList<InventoryItem> originalItems;
		Filter myFilter;
		
		public InventoryItemAdapter(Context context, int resource, ArrayList<InventoryItem> objects) {
			super(context, resource, objects);
			this.context = context;
			layoutResourceId = resource;
			items = objects;
			originalItems = (ArrayList<InventoryItem>) items.clone();
			myFilter = new Filter(){

				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults filterResults = new FilterResults();
					ArrayList<InventoryItem> tempList = new ArrayList<InventoryItem>();
					if(constraint != null && originalItems != null){
						int i = 0;
						while(i < originalItems.size()){
							InventoryItem item = originalItems.get(i);
							if(item.getName().toLowerCase().startsWith(constraint.toString().toLowerCase())){
								tempList.add(item);
							}
							i++;
						}
						filterResults.count = tempList.size();
						filterResults.values = tempList;
					}
					return filterResults;
				}

				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					items = (ArrayList<InventoryItem>) results.values;
					notifyDataSetChanged();
					clear();
					for(InventoryItem item : items){
						add(item);
					}
					notifyDataSetInvalidated();
					
				}
				
			};
		}
		
		private class ViewHolder {
			private ImageView iconImageView;
			private TextView textView;
			private TextView subTextView;
			private TextView textViewRight;
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder = null;
			if(convertView == null){
				convertView = LayoutInflater.from(context).inflate(R.layout.view_list_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.textView = (TextView) convertView.findViewById(R.id.list_item_textview);
				viewHolder.subTextView = (TextView) convertView.findViewById(R.id.list_item_subtextview);
				viewHolder.iconImageView = (ImageView) convertView.findViewById(R.id.list_item_icon);
				viewHolder.textViewRight = (TextView) convertView.findViewById(R.id.list_item_text_right);
			
				convertView.setTag(viewHolder);
			}else{
				viewHolder = (ViewHolder) convertView.getTag();
			}
			
			InventoryItem item = getItem(position);
			if(item != null){
				viewHolder.textView.setText(item.getName());
				viewHolder.subTextView.setText(item.getDesc());
				viewHolder.textViewRight.setText(String.valueOf(item.getQuantity()));
				if(item.getBitmap()!=null){
					viewHolder.iconImageView.setImageBitmap(item.getBitmap());
				}else{
					Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.box_bud);
					item.setBitmap(icon);
					viewHolder.iconImageView.setImageBitmap(icon);
				}
				convertView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						launchItemPopup(false, items.get(position), position);
					}
				});
			}
			return convertView;
		}
		@Override
		public Filter getFilter() {
			return myFilter;
		}
		public void addToOriginal(InventoryItem item){
			originalItems.add(item);
		}
		
		public void removeFromOriginal(int toDelete){
			originalItems.remove(toDelete);
		}
	}
}
