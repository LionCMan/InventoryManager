package com.example.android.inventorymanager;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.inventorymanager.Data.ProductContract.ProductEntry;

public class InventoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Identifier for the loader
    private static final int INVENTORY_LOADER = 0;

    // Cursor Adapter
    ProductCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Setup the FAB to open the editor Activity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent productDetailIntent = new Intent(InventoryActivity.this, ProductDetailActivity.class);
                startActivity(productDetailIntent);
            }
        });

        // Start the loader
        getSupportLoaderManager().initLoader(INVENTORY_LOADER, null, this);

        initialiseListView();
    }

    /**
     * Helper method for inserting test data
     */
    private void insertItem() {
        // Build a ContentValues object for an inventory item
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_NAME, "Can of Coke");
        values.put(ProductEntry.COLUMN_PRICE, 1.20);
        values.put(ProductEntry.COLUMN_QUANTITY, 2);
        values.put(ProductEntry.COLUMN_IMAGE, R.drawable.image_placeholder);

        // Uri for test data
        Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);
    }

    /**
     * Method to delete all Items in the table
     */
    private void deleteAllItems() {
        int rowsDeleted = getContentResolver().delete(ProductEntry.CONTENT_URI, null, null);
        Log.v("InventoryCatalog", rowsDeleted + " rows deleted from inventory database");
    }

    /**
     * Methods to activate Options Items
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to click on Insert Test Data
            case R.id.action_insert_test_data:
                insertItem();
                return true;
            case R.id.action_delete_all:
                deleteAllItems();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from the res menu_catalog.xml file
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns we want
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_NAME,
                ProductEntry.COLUMN_PRICE,
                ProductEntry.COLUMN_QUANTITY,
                ProductEntry.COLUMN_IMAGE };

        // Return the CursorLoader
        return new CursorLoader(this,       // Parent Activity
                ProductEntry.CONTENT_URI, // Content Uri to query
                projection,                 // Projection
                null,                       // No selection clause
                null,                       // No selectionArgs
                null);                      // Default sortOrder
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update the InventoryCursorAdapter with the new Cursor
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Called when data is to be deleted
        mCursorAdapter.swapCursor(null);
    }

    private void initialiseListView() {
        // Find list view from the layout file
        ListView listView = (ListView) findViewById(R.id.list_view);
        // Define empty view so a specific layout can be displayed when
        // there's no data to be shown in the UI
        View emptyView = findViewById(R.id.empty_view);
        // Attach the empty view to the list view when there's no data to show
        listView.setEmptyView(emptyView);
        // Initialise cursor adapter
        mCursorAdapter = new ProductCursorAdapter(this, null, false);
        // Attach cursor adapter to the list view
        listView.setAdapter(mCursorAdapter);
        // Set click listener to the listView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(InventoryActivity.this, ProductDetailActivity.class);
                intent.setData(ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id));
                startActivity(intent);
            }
        });
    }
}