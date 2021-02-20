package jp.co.troot.llog

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import java.util.*

class PhotoAdapter internal constructor(private val mContext: Context, private val mPhotoList: ArrayList<PhotoData>) : PagerAdapter() {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        // リストから取得
        val photoList = mPhotoList[position]

        // View を生成
        val imageView = ImageView(mContext)
        val bitmap = MyUtils.getBitmapFromServer(photoList.mDate, photoList.mFolderName, photoList.mFileName)
        imageView.setImageBitmap(bitmap)

        // コンテナに追加
        container.addView(imageView)

        return imageView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        // コンテナから View を削除
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        // リストのアイテム数を返す
        return mPhotoList.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        // Object 内に View が存在するか判定する
        return view === `object`
    }
}