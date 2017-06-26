package com.chadali.opskinsprices;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private SearchView searchView;
    private MenuItem searchMenuItem;
    private ListView list_view;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> displayList = new ArrayList<String>();
    private ArrayList<SkinObject> skinsList = new ArrayList<SkinObject>();
    private Toast toastMessage;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String[] keysArray = {"Chroma 2 Case Key", "Chroma Case Key", "CS:GO Case Key", "eSports Key",
            "Falchion Case Key", "Huntsman Case Key", "Operation Breakout Case Key", "Operation Phoenix Case Key",
            "Shadow Case Key", "Operation Vanguard Case Key", "Winter Offensive Case Key", "Revolver Case Key",
            "Operation Wildfire Case Key", "Chroma 3 Case Key", "Gamma Case Key", "Gamma 2 Case Key",
            "Glove Case Key", "Spectrum Case Key", "Operation Hydra Case Key"};

    private int masterPriceMin = 0;
    private int masterPriceMax = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RangeSeekBar<Integer> rangeSeekBar = new RangeSeekBar<Integer>(this);
        rangeSeekBar.setTextAboveThumbsColor(Color.BLACK);
        rangeSeekBar.setRangeValues(0, 1000);
        rangeSeekBar.setSelectedMinValue(0);
        rangeSeekBar.setSelectedMaxValue(1000);

        LinearLayout layout = (LinearLayout) findViewById(R.id.seekbar_placeholder);
        layout.addView(rangeSeekBar);

        rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                mSwipeRefreshLayout.setRefreshing(true);
                displayList.clear();
                adapter.notifyDataSetChanged();

                if(maxValue == 1000) { masterPriceMax = 10000; } else { masterPriceMax = maxValue; }
                masterPriceMin = minValue;

                Boolean notSearching = searchView.isIconified();
                if(!notSearching) {
                    String query = searchView.getQuery().toString();
                    for(SkinObject skin : skinsList) {
                        if(skin.name.toString().toLowerCase().contains(query.toLowerCase())){
                            if(skin.cost >= masterPriceMin && skin.cost <= masterPriceMax) {
                                displayList.add(skin.name.toString() + " - $" + skin.cost.toString());
                            }
                        }
                    }
                } else{
                    for(SkinObject skin : skinsList) {
                        if(skin.cost >= masterPriceMin && skin.cost <= masterPriceMax) { displayList.add(skin.name + " - $" + skin.cost);}
                    }
                }

                adapter.notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

        rangeSeekBar.setNotifyWhileDragging(true);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle("OPSKINS PRICE SEARCH");

        listView();
        fetchAPI();
    }

    public void listView() {
        list_view = (ListView) findViewById(R.id.skins_list);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchAPI();
            }
        });

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayList);
        list_view.setAdapter(adapter);
        list_view.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (toastMessage != null) {
                        toastMessage.cancel();
                    }

                    toastMessage = Toast.makeText(MainActivity.this, "??", Toast.LENGTH_LONG);
                    toastMessage.show();
                    }
                }
        );
    }

    /* GET Request on API, stores into skinsList, copies to displayList, and displays */
    public void fetchAPI(){
        mSwipeRefreshLayout.setRefreshing(true);
        displayList.clear();
        skinsList.clear();
        adapter.notifyDataSetChanged();

        String url ="https://api.opskins.com/IPricing/GetAllLowestListPrices/v1/?appid=730";

        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.GET, url, null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONObject obj = new JSONObject(response.toString());
                        JSONObject skins = obj.getJSONObject("response");
                        Iterator<String> iter = skins.keys();

                        while (iter.hasNext()) {
                            String key = iter.next();
                            JSONObject skin = skins.getJSONObject(key.toString());
                            skinsList.add(new SkinObject(key.toString(), skin.getDouble("price")/100,
                                    skin.getInt("quantity")));
                        }

                        for(SkinObject skin : skinsList) {
                            if(skin.cost >= masterPriceMin && skin.cost <= masterPriceMax) { displayList.add(skin.name + " - $" + skin.cost);}
                        }
                    } catch (Exception e) {
                        Log.d("Request", "Could not parse json " + e.getMessage());
                    }

                    adapter.notifyDataSetChanged();
                    mSwipeRefreshLayout.setRefreshing(false);

                }
            }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        displayList.add("API Fetch failed. Refresh Mayb");
                        adapter.notifyDataSetChanged();
                    }
            });

        VolleyController.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        SearchManager searchManager = (SearchManager)
                getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mSwipeRefreshLayout.setRefreshing(true);
                displayList.clear();
                for(SkinObject skin : skinsList) {
                    if(skin.name.toString().toLowerCase().contains(newText.toLowerCase())){
                        if(skin.cost >= masterPriceMin && skin.cost <= masterPriceMax) {
                            displayList.add(skin.name.toString() + " - $" + skin.cost.toString());
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
                return true;
            }

        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.keys_only:
                showOnlyKeys();
                return true;

            case R.id.refresh:
                fetchAPI();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }




    public void showOnlyKeys(){
        try {
            mSwipeRefreshLayout.setRefreshing(true);
            displayList.clear();
            adapter.notifyDataSetChanged();

            for(SkinObject skin : skinsList) {
                if(Arrays.asList(keysArray).contains(skin.name)){
                    displayList.add(skin.name + " - $" + skin.cost);
                }
            }

            adapter.notifyDataSetChanged();
            mSwipeRefreshLayout.setRefreshing(false);
        } catch(Exception e) {
            Log.d("KeyFilter", "Could not filter keys: "  + e);
        }
    }

}
