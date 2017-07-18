package com.example.android.inventorymanager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventorymanager.Data.ProductContract.ProductEntry;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ProductDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = ProductDetailActivity.class.getSimpleName();

    /** EditText field to enter product name */
    private EditText pNameET;

    /** EditText field to enter product large quantity */
    private EditText pQuantityET;

    /** EditText field to enter product price */
    private EditText pPriceET;

    /** TextView to show current product quantity */
    private TextView pQuantityTV;

    /** Product information variables */
    private String productName;
    private int productQuantity;

    /** Four Buttons that will be used to modify quantity */
    private Button increaseQuantityButton;    // Increase by one
    private Button decreaseQuantityButton;    // Decrease by one
    private Button increaseQuantityIntervalButton;    // Increase by many
    private Button decreaseQuantityIntervalButton;    // Decrease by many

    /** Final for the image intent request code */
    private final static int SELECT_PHOTO = 200;

    /** Constant to be used when asking for storage read */
    private final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 300;

    /** Button to select image, ImageView to display selected image
     * and Bitmap to store/retrieve from the database */
    private ImageView productImageView;
    private Bitmap productBitmap;

    /** Constant field for email intent */
    private static final String URI_EMAIL = "mailto:";

    /** Uri loader */
    private static final int URI_LOADER = 0;

    /** Uri received with the Intent from {@link InventoryActivity} */
    private Uri productUri;

    /** Boolean to check whether or not the register has changed */
    private boolean productHasChanged = false;

    private static final String CAMERA_DIR = "/dcim/";
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static final int PICK_IMAGE_REQUEST = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_PERMISSIONS_REQUEST = 2;

    private boolean isGalleryPicture = false;
    public String mImage;
    private Uri photoUri;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        requestPermissions();

        // Receive Uri data from intent
        Intent intent = getIntent();
        productUri = intent.getData();

        // Check if Uri is null or not
        if (productUri != null) {
            // If not null means that a product register will be edited
            setTitle(R.string.activity_detail_edit);
            // Kick off LoaderManager
            getLoaderManager().initLoader(URI_LOADER, null, this);
        } else {
            // If null means that a new product register will be created
            setTitle(R.string.activity_detail_new);
            // Invalidate options menu (delete button) since there's no record
            invalidateOptionsMenu();
        }

        // Find all relevant views that we will need to read or show user input
        initialiseViews();

        // Set on touch listener to all relevant views
        setOnTouchListener();
    }

    private void initialiseViews() {
        // Check if it's an existing product to make the btton visible so
        // the user can order more from existing product
        if (productUri != null) {
            // Initialise Button to order more from supplier
            /* Button to order more quantity from supplier */
            Button orderButton = (Button) findViewById(R.id.button_order_from_supplier);

            orderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setData(Uri.parse("mailto:"));
                    intent.setType("text/plain");
                    // Defining supplier's email. Ideally it would come from the product database in a real world
                    // application but I am using a string to make it simple for this exercise.
                    intent.putExtra(Intent.EXTRA_EMAIL, getString(R.string.supplier_email));
                    intent.putExtra(Intent.EXTRA_SUBJECT, productName);
                    startActivity(Intent.createChooser(intent, "Send mail..."));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });
        }

        // Initialise EditTexts
        pNameET = (EditText) findViewById(R.id.edit_text_name);
        pQuantityET = (EditText) findViewById(R.id.edit_text_quantity);
        pPriceET = (EditText) findViewById(R.id.edit_text_price);

        // Initialise TextView
        pQuantityTV = (TextView) findViewById(R.id.text_view_quantity_final);

        // Initialise increase Button and set click listener
        increaseQuantityButton = (Button) findViewById(R.id.button_increase_one);
        increaseQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add +1 to product quantity
                productQuantity++;
                // Update UI
                pQuantityTV.setText(String.valueOf(productQuantity));
            }
        });

        // Initialise decrease Button and set click listener
        decreaseQuantityButton = (Button) findViewById(R.id.button_decrease_one);
        decreaseQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Decrease 1 to product quantity if higher than 0
                if (productQuantity > 0) {
                    productQuantity--;
                    // Update UI
                    pQuantityTV.setText(String.valueOf(productQuantity));
                } else {
                    Toast.makeText(ProductDetailActivity.this,
                            getString(R.string.toast_invalid_quantity), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Initialise increase large quantity Button and set click listener
        increaseQuantityIntervalButton = (Button) findViewById(R.id.button_increase_n);
        increaseQuantityIntervalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if quantity edit text is empty and higher than zero
                if (!TextUtils.isEmpty(pQuantityET.getText()) &&
                        Integer.valueOf(pQuantityET.getText().toString()) > 0) {
                    // Add the quantity in the edit text to the variable keeping track of product stock quantity
                    productQuantity += Integer.valueOf(pQuantityET.getText().toString());
                    // Update the UI
                    pQuantityTV.setText(String.valueOf(productQuantity));
                } else {
                    // Show toast asking user to fill out edit text
                    Toast.makeText(ProductDetailActivity.this,
                            getString(R.string.toast_missing_quantity), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Initialise decrease large quantity Button and set click listener
        decreaseQuantityIntervalButton = (Button) findViewById(R.id.button_decrease_n);
        decreaseQuantityIntervalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if quantity edit text is empty and higher than zero
                if (!TextUtils.isEmpty(pQuantityET.getText()) &&
                        Integer.valueOf(pQuantityET.getText().toString()) > 0) {
                    int newQuantity = productQuantity - Integer.valueOf(pQuantityET.getText().toString());
                    if (newQuantity < 0) {
                        Toast.makeText(ProductDetailActivity.this,
                                getString(R.string.toast_invalid_quantity), Toast.LENGTH_SHORT).show();
                    } else {
                        // Decrease the quantity in the edit text to the variable keeping track of product stock quantity
                        productQuantity -= Integer.valueOf(pQuantityET.getText().toString());
                        // Update the UI
                        pQuantityTV.setText(String.valueOf(productQuantity));
                    }
                } else {
                    // Show toast asking user to fill out edit text
                    Toast.makeText(ProductDetailActivity.this,
                            getString(R.string.toast_missing_quantity), Toast.LENGTH_SHORT).show();
                }
            }
        });

        productImageView = (ImageView) findViewById(R.id.image);
        // Initialise the image view to show alert dialog
        productImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_main_activity.xml file.
        // This adds the given menu to the app bar.
        getMenuInflater().inflate(R.menu.product_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (productUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Add" menu option
            case R.id.action_add:
                if (productHasChanged) {
                    // Call save/edit method
                    saveProduct();
                } else {
                    // Show toast when no product is updated nor created
                    Toast.makeText(this, getString(R.string.toast_insert_or_update_product_failed),
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_delete:
                // Call delete confirmation dialog
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                // If product hasn't changed, continue with navigating up to parent activity
                if (!productHasChanged) {
                    NavUtils.navigateUpFromSameTask(ProductDetailActivity.this);
                    return true;
                } else {
                    // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                    // Create a click listener to handle the user confirming that
                    // changes should be discarded.
                    DialogInterface.OnClickListener discardButtonClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // User clicked "Discard" button, navigate to parent activity
                                    NavUtils.navigateUpFromSameTask(ProductDetailActivity.this);
                                }
                            };

                    // Show a dialog that notifies the user they have unsaved changes
                    showUnsavedChangesDialog(discardButtonClickListener);
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle the back button press on the device
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with closing and back to parent activity
        if (!productHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user
        // Create a click listener to handle the user confirming that changes should be discarded
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Close the current activity without adding/saving
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * Add new product or commit changes to existing register being edited
     */
    private void saveProduct() {
        String name = pNameET.getText().toString().trim();
        String quantity = pQuantityET.getText().toString().trim();
        String unitPrice = pPriceET.getText().toString().trim();
        String photoPath = null;

        if (productUri != null) {
            photoPath = productUri.getPath();
            productImageView.setTag(photoPath);
        }

        // Check to see if this is a new item
        if (productUri == null && TextUtils.isEmpty(name) || photoPath == "") {
            Toast.makeText(this, "All Fields Must Be Filled Out", Toast.LENGTH_LONG).show();
            return;
        }

        // Build a ContentValues with the input
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_NAME, name);
        values.put(ProductEntry.COLUMN_IMAGE, photoPath);

        // If a price is not included, set to 0
        Double price = 0.00;
        if (!TextUtils.isEmpty(unitPrice)) {
            price = Double.parseDouble(unitPrice);
        }
        values.put(ProductEntry.COLUMN_PRICE, price);

        // If a quantity is not provided, set to 0
        int stock = 0;
        if (!TextUtils.isEmpty(quantity)) {
            stock = Integer.parseInt(quantity);
        }
        values.put(ProductEntry.COLUMN_QUANTITY, stock);


        // Determine if this is a new or existing item
        if (productUri == null) {
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            if (newUri == null) {
                Toast.makeText(this, R.string.error_making_item, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.item_saved, Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsChanged = getContentResolver().update(productUri, values, null, null);

            if (rowsChanged == 0) {
                Toast.makeText(this, R.string.update_error, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.update_successful, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Convert from bitmap to byte array
     *
     * @param bitmap: Data retrieved from the user galery that will be
     *              converted to byte[] in order to store in database BLOB
     */
    public static byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    /**
     * Convert from byte array to bitmap
     *
     * @param image: BLOB from the database converted to a Bitmap
     *             in order to display in the UI
     */
    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    /**
     * Method to define if any of the EditText fields are empty or contain invalid inputs
     *
     * @param string: String received as a parameter to be checked with this method
     */
    private boolean checkFieldEmpty(String string) {
        return TextUtils.isEmpty(string) || string.equals(".");
    }

    /**
     * Perform the deletion of the product record in the database.
     */
    private void deleteProduct() {
        if (productUri != null) {
            int rowsDeleted = getContentResolver().delete(productUri, null, null);
            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.toast_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.toast_delete_product_success),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Ask for user confirmation before deleting product from database
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.prompt_delete_product));
        builder.setPositiveButton(getString(R.string.prompt_delete), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Call deleteProduct method, so delete the product register from database.
                deleteProduct();
                finish();
            }
        });
        builder.setNegativeButton(getString(R.string.prompt_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Dismiss the dialog and continue editing the product record.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Ask for user confirmation to exit activity before saving
     */
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.prompt_leave_no_save));
        builder.setPositiveButton(getString(R.string.prompt_yes), discardButtonClickListener);
        builder.setNegativeButton(getString(R.string.prompt_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product register
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Set touch listeners to the UI
     */
    private void setOnTouchListener() {
        pNameET.setOnTouchListener(mTouchListener);
        pQuantityET.setOnTouchListener(mTouchListener);
        pPriceET.setOnTouchListener(mTouchListener);
        increaseQuantityButton.setOnTouchListener(mTouchListener);
        decreaseQuantityButton.setOnTouchListener(mTouchListener);
        increaseQuantityIntervalButton.setOnTouchListener(mTouchListener);
        decreaseQuantityIntervalButton.setOnTouchListener(mTouchListener);
        productImageView.setOnTouchListener(mTouchListener);
    }

    /**
     * Set onTouchListener on the UI and changes the boolean value to TRUE in order to indicate
     * that the user is changing the current product register
     */
    View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            productHasChanged = true;
            return false;
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case URI_LOADER:
                return new CursorLoader(
                        this,
                        productUri,
                        null,
                        null,
                        null,
                        null
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            productName = data.getString(data.getColumnIndex(ProductEntry.COLUMN_NAME));
            pNameET.setText(productName);

            pPriceET.setText(data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRICE)));

            productQuantity = data.getInt(data.getColumnIndex(ProductEntry.COLUMN_QUANTITY));
            pQuantityET.setText(String.valueOf(productQuantity));

            if (data.getBlob(data.getColumnIndex(ProductEntry.COLUMN_IMAGE)) != null) {
                productImageView.setImageBitmap(getImage(
                        data.getBlob(data.getColumnIndex(ProductEntry.COLUMN_IMAGE))));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        pNameET.getText().clear();
        pQuantityET.getText().clear();
        pQuantityTV.setText("");
    }

    public void requestPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                }
            }
        }
    }

    public void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            File f = createImageFile();

            Log.d(LOG_TAG, "File: " + f.getAbsolutePath());

            photoUri = FileProvider.getUriForFile(
                    this, FILE_PROVIDER_AUTHORITY, f);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

            // Solution taken from http://stackoverflow.com/a/18332000/3346625
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                List<ResolveInfo> resInfoList =
                        getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.i(LOG_TAG, "Received an \"Activity Result\"");
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                photoUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + photoUri.toString());

                productBitmap = getBitmapFromUri(photoUri);
                productImageView.setImageBitmap(productBitmap);

                isGalleryPicture = true;
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Log.i(LOG_TAG, "Uri: " + photoUri.toString());

            productBitmap = getBitmapFromUri(photoUri);
            productImageView.setImageBitmap(productBitmap);

            isGalleryPicture = false;
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File(Environment.getExternalStorageDirectory()
                    + CAMERA_DIR
                    + getString(R.string.app_name));

            Log.d(LOG_TAG, "Dir: " + storageDir);

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d(LOG_TAG, "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }
}
