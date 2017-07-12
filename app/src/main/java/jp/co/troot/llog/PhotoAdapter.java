package jp.co.troot.llog;

import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.support.v4.view.PagerAdapter;
import java.util.ArrayList;

public class PhotoAdapter extends PagerAdapter {
    private Context mContext;
    private ArrayList<PhotoData> mPhotoList;

    public PhotoAdapter(Context context, ArrayList<PhotoData> photoList) {
        mContext = context;
        mPhotoList = photoList;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // リストから取得
        PhotoData photoList = mPhotoList.get(position);

        // View を生成
        ImageView imageView = new ImageView(mContext);
        try {
            Bitmap bitmap = MyUtils.getBitmapFromServer(photoList.mDate, photoList.mFolderName, photoList.mFileName);
            imageView.setImageBitmap(bitmap);

            // コンテナに追加
            container.addView(imageView);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // コンテナから View を削除
        container.removeView((View)object);
    }

    @Override
    public int getCount() {
        // リストのアイテム数を返す
        return mPhotoList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        // Object 内に View が存在するか判定する
        return view == (ImageView)object;
    }

}
