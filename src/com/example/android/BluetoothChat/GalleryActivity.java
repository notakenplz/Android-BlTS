package com.example.android.BluetoothChat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Gallery;

public class GalleryActivity extends Activity implements OnClickListener{
	LinearLayout mLinearLayout;
	int pos_temp = 0;

    private Context mContext;

    private Integer[] mImageIds = {
            R.drawable.p1,
            R.drawable.p2,
            R.drawable.p3,
            R.drawable.p4,
            R.drawable.p5,
            R.drawable.p6,
            R.drawable.b1,
            R.drawable.b2
    };
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);
        mLinearLayout = new LinearLayout(this);
       
        Gallery g = (Gallery) findViewById(R.id.gallerys);
        g.setAdapter(new ImageAdapter(this));

        g.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Toast.makeText(GalleryActivity.this, "" + position, Toast.LENGTH_SHORT).show();
                pos_temp = position;

                ImageButton image = (ImageButton) findViewById(R.id.image_button);
                image.setImageResource(mImageIds[position]);
            }
        });
        
        ImageButton ibutton = (ImageButton) findViewById(R.id.image_button);
        ibutton.setOnClickListener(this);
    }

    public class ImageAdapter extends BaseAdapter {
        int mGalleryItemBackground;


        public ImageAdapter(Context c) {
            mContext = c;
            TypedArray a = c.obtainStyledAttributes(R.styleable.Gallery1);
            mGalleryItemBackground = a.getResourceId(R.styleable.Gallery1_android_galleryItemBackground, 0);
            a.recycle();
        }
        
        public int getCount() {
            return mImageIds.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(mContext);

            i.setImageResource(mImageIds[position]);
            i.setLayoutParams(new Gallery.LayoutParams(120, 100));
            i.setScaleType(ImageView.ScaleType.FIT_XY);
            i.setBackgroundResource(mGalleryItemBackground);

            return i;
        }
    }

    public void onClick(View src) {
        Intent i = new Intent(this, BluetoothChat.class);
        i.putExtra("image", pos_temp);
        startActivity(i);
    }
}
