package com.deep.profilemaper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.drawermodel.NavDrawerItem;
import com.drawermodel.NavDrawerListAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class ShowCurrentLocation extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, OnClickListener, OnMarkerClickListener,
		OnMarkerDragListener
{

	private ProgressBar				 searching;
	CustomAutoCompleteTextView			 myTextView;
	private LocationClient				 mLocationClient;
	private GoogleMap				 googleMap;

	private Double					 latitude= 0d, longitude = 0d;
	private Button					 saveLocationButton, viewSavedLocations;

	LatLng						 searchposition;

	private DrawerLayout				 drawerlayout;
	private ListView				 drawerlist;
	private ActionBarDrawerToggle		         drawertoggle;

	// slide menu items
	private String[]				 navMenuTitles;
	private TypedArray				 navMenuIcons;

	private ArrayList<NavDrawerItem>	         navDrawerItems;
	private NavDrawerListAdapter		         adapter;
	LocationManager					 lm;
	int status;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapwithdrawer);

		saveLocationButton = (Button) findViewById(R.id.save_btn);
		viewSavedLocations = (Button) findViewById(R.id.view_saved_locs);
		searching = (ProgressBar) findViewById(R.id.address_progress);

		saveLocationButton.setOnClickListener(this);
		viewSavedLocations.setOnClickListener(this);

		status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		searching = (ProgressBar) findViewById(R.id.address_progress);
		/*
		 * Create a new location client, using the enclosing class to handle callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);

		// load slide menu items
		navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);

		// nav drawer icons from resources
		navMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);

		drawerlayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerlist = (ListView) findViewById(R.id.list_slidermenu);

		navDrawerItems = new ArrayList<NavDrawerItem>();

		// adding nav drawer items to array

		// Normal Map
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1)));

		// Satellite Map
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1)));

		// Terrain Map
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));

		// Hybrid Map
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));

		// Recycle the typed array
		navMenuIcons.recycle();

		drawerlist.setOnItemClickListener(new SlideMenuClickListener());

		// setting the nav drawer list adapter
		adapter = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems);
		drawerlist.setAdapter(adapter);

		// enabling action bar app icon and behaving it as toggle button
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		drawertoggle = new ActionBarDrawerToggle(this, drawerlayout, R.drawable.ic_drawer, // nav menu toggle icon
				R.string.app_name, // nav drawer open - description for accessibility
				R.string.app_name // nav drawer close - description for accessibility
		)
		{
			public void onDrawerClosed(View view)
			{
				// calling onPrepareOptionsMenu() to show action bar icons
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView)
			{
				// calling onPrepareOptionsMenu() to hide action bar icons
				invalidateOptionsMenu();
			}
		};

		drawerlayout.setDrawerListener(drawertoggle);

		initilizeMap();

	}

	private boolean isLocationEnabled()
	{
		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
				|| lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
		{
			return true;
		}

		return false;
	}

	/**
	 * Function to show settings alert dialog
	 * */
	public void showSettingsAlert()
	{
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

		// Setting Dialog Title
		alertDialog.setTitle("Location Access");

		// Setting Dialog Message
		alertDialog.setMessage("Location Access is not enabled. Do you want to go to settings menu?");

		// Setting Icon to Dialog
		// alertDialog.setIcon(R.drawable.delete);

		// On pressing Settings button
		alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				ShowCurrentLocation.this.startActivity(intent);
				ShowCurrentLocation.this.finish();
			}
		});

		// on pressing cancel button
		alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
				ShowCurrentLocation.this.finish();
			}
		});

		// Showing Alert Message
		alertDialog.show();
	}

	/**
	 * Function to show settings alert dialog
	 * */
	public void showSettingsAlertForMobileData()
	{
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

		// Setting Dialog Title
		alertDialog.setTitle("Mobile Data Settings");

		// Setting Dialog Message
		alertDialog.setMessage("Mobile Data is not enabled. Do you want to go to settings menu?");

		// Setting Icon to Dialog
		// alertDialog.setIcon(R.drawable.delete);

		// On pressing Settings button
		alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				Intent intent = new Intent();
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setAction(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
				startActivity(intent);
				ShowCurrentLocation.this.finish();
			}
		});

		// on pressing cancel button
		alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
				ShowCurrentLocation.this.finish();
			}
		});

		// Showing Alert Message
		alertDialog.show();
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		if (status == ConnectionResult.SUCCESS)
		{
			Log.d("Play Service ", status+"");
			if (isLocationEnabled() && isOnline())
			{
				if (mLocationClient != null)
				{
					Log.d("Location Service ", "Location client not null");
				}
				mLocationClient.connect();
			}
			else
			{
				if (!isLocationEnabled())
				{
					showSettingsAlert();
				}
				else if (!isOnline())
				{
					showSettingsAlertForMobileData();
				}
			}
		}
		else
		{
			int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.home, menu);

		/*
		 * Setting customview to action bar
		 */

		android.app.ActionBar actionBar = getActionBar();

		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO
				| ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);

		View v = (View) menu.findItem(R.id.search).getActionView();

		myTextView = (CustomAutoCompleteTextView) v.findViewById(R.id.MyAutoCompleteTextView);
		myTextView.imgCloseButton = getResources().getDrawable(R.drawable.abc_ic_clear);

		// Setting google places API as autocomplete
		myTextView.setAdapter(new PlacesAutoCompleteAdapter(this, android.R.layout.simple_list_item_1));

		myTextView.setOnEditorActionListener(new OnEditorActionListener()
		{

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_SEARCH)
				{
					searching.setVisibility(View.VISIBLE);
					new GeocoderTask().execute();
					return true;
				}
				return false;

			}
		});

		// Toggle close image
		myTextView.addTextChangedListener(new TextWatcher()
		{

			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				myTextView.clrButtonHandler();
				if (myTextView.justCleared)
				{

					myTextView.justCleared = false;
				}

			}

			@Override
			public void afterTextChanged(Editable s)
			{

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

		});

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// toggle nav drawer on selecting action bar app icon/title
		if (drawertoggle.onOptionsItemSelected(item))
		{
			return true;
		}

		// Handle action bar actions click
		switch (item.getItemId())
		{

			case R.id.search:
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during onPostCreate() and onConfigurationChanged()...
	 */

	private class SlideMenuClickListener implements ListView.OnItemClickListener
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			// display view for selected nav drawer item
			displayView(position);
		}
	}

	/**
	 * Diplaying fragment view for selected nav drawer list item
	 * */
	private void displayView(int position)
	{
		switch (position)
		{
			case 0:
				googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				break;
			case 1:
				googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				break;
			case 2:
				googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
				break;
			case 3:
				googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
				break;
		}
		// update selected item and title, then close the drawer
		drawerlist.setItemChecked(position, true);
		drawerlist.setSelection(position);
		drawerlayout.closeDrawer(drawerlist);

	}

	/* *
	 * Called when invalidateOptionsMenu() is triggered
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		// if nav drawer is opened, hide the action items
		boolean drawerOpen = drawerlayout.isDrawerOpen(drawerlist);
		menu.findItem(R.id.search).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		drawertoggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		drawertoggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0)
	{

		Toast.makeText(this, "Connection Failed Try Again Later", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onConnected(Bundle arg0)
	{
		settingUserLocation();
	}

	private void initilizeMap()
	{
		if (googleMap == null)
		{
			googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			googleMap.setBuildingsEnabled(true);
			googleMap.setIndoorEnabled(true);

			// check if map is created successfully or not
			if (googleMap == null)
			{
				Toast.makeText(this, "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private boolean isOnline()
	{
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();

		if (netInfo != null && netInfo.isConnected())
		{
			return true;
		}
		return false;
	}

	private void settingUserLocation()
	{

		try
		{
			if (mLocationClient.getLastLocation() != null)
			{
				latitude = mLocationClient.getLastLocation().getLatitude();
				longitude = mLocationClient.getLastLocation().getLongitude();

				if (latitude != 0 && longitude != 0)
				{
					googleMap.clear();

					Marker currentLocationMarker = googleMap.addMarker(new MarkerOptions().position(
							new LatLng(latitude, longitude)).icon(
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

					googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));

					googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));

					currentLocationMarker.setDraggable(true);
					googleMap.setOnMarkerClickListener(this);
					googleMap.setOnMarkerDragListener(this);

				}

			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	@Override
	public void onDisconnected()
	{
		Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
		Log.d("Disconnect", "Client Disconnected");
		// Destroy the current location client
		mLocationClient = null;
	}

	@Override
	protected void onStop()
	{
		mLocationClient.disconnect();
		super.onStop();

	}

	@Override
	public void onClick(View view)
	{
		switch (view.getId())
		{
			case R.id.save_btn:
				openSaveLocationDataActivity();
				break;

			case R.id.view_saved_locs:
				Intent targetIntent = new Intent(this, ShowSavedLocationsActivity.class);
				startActivity(targetIntent);
				break;
		}

	}

	@Override
	public boolean onMarkerClick(Marker arg0)
	{
		openSaveLocationDataActivity();
		return true;
	}

	private void openSaveLocationDataActivity()
	{
		Intent saveLocationIntent = new Intent(this, LocationDataSavingActivity.class);

		saveLocationIntent.putExtra(AppConstants.KEY_LATITUDE, latitude);
		saveLocationIntent.putExtra(AppConstants.KEY_LONGITUDE, longitude);

		startActivity(saveLocationIntent);
		this.finish();
	}

	@Override
	public void onMarkerDrag(Marker arg0)
	{

	}

	@Override
	public void onMarkerDragEnd(Marker endMarker)
	{

		LatLng lastMarker = endMarker.getPosition();
		latitude = lastMarker.latitude;
		longitude = lastMarker.longitude;
	}

	@Override
	public void onMarkerDragStart(Marker arg0)
	{
	}

	/**
	 * Find the searched address on map
	 * 
	 */

	private class GeocoderTask extends AsyncTask<String, Void, List<Address>>
	{

		@Override
		protected List<Address> doInBackground(String... locationName)
		{
			// Creating an instance of Geocoder class

			Geocoder geocoder = new Geocoder(getBaseContext());
			List<Address> addresses = null;
			String locationames = myTextView.getText().toString();

			try
			{
				// Getting a maximum of 3 Address that matches the input text
				addresses = geocoder.getFromLocationName(locationames, 3);

				// If no address found that matches user's query
				if (addresses.size() == 0)
				{
					addresses = null;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			return addresses;
		}

		@Override
		protected void onPostExecute(List<Address> addresses)
		{

			searching.setVisibility(View.INVISIBLE);

			if (addresses == null || addresses.size() == 0)
			{
				Toast.makeText(getBaseContext(), "No Location found", Toast.LENGTH_SHORT).show();
			}
			else

			{

				// Clears all the existing markers on the map
				googleMap.clear();

				Address address = (Address) addresses.get(0);

				latitude = address.getLatitude();
				longitude = address.getLongitude();

				searchposition = new LatLng(address.getLatitude(), address.getLongitude());
				Marker searchLocationMarker = googleMap.addMarker(new MarkerOptions().position(searchposition).icon(
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

				CameraPosition cameraPosition = new CameraPosition.Builder().target(searchposition).zoom(17).build();
				googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
				searchLocationMarker.setDraggable(true);
			}

		}

	}

}
