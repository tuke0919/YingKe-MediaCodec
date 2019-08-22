package com.yingke.mediacodec.widget.localmedia.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 功能：基适配器
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/1/10
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 */
public abstract class BaseRecycleViewAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public Context context;
    // 数据集
    public List<T> mDataList;

    public BaseRecycleViewAdapter(Context context) {
         this(context, null);
    }

    public BaseRecycleViewAdapter(Context context, List<T> mDataList) {
        this.context = context;
        if (mDataList == null) {
            mDataList = new ArrayList<>();
        }
        this.mDataList = mDataList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder (ViewGroup parent, int viewType) {
        View rootView  = LayoutInflater.from(context).inflate(getConvertViewResId(viewType), parent, false);
        return getViewHolder(viewType, rootView);
    }

    @Override
    public void onBindViewHolder (RecyclerView.ViewHolder holder, int position) {
        T item = getItem(position);
        if(holder instanceof BaseViewHolder){
            ((BaseViewHolder<T>) holder).onRefreshData(position, item);
        }
    }

    @Override
    public int getItemCount () {
        if (mDataList != null) {
            return mDataList.size();
        }
        return 0;
    }

    /**
     * 获取数据
     * @param position
     * @return
     */
    public T getItem(int position) {
        if (mDataList == null) {
            mDataList = new ArrayList<>();
        }
        if (position < 0 || position >= mDataList.size()) {
            return null;
        }
        return mDataList.get(position);
    }

    /**
     * 添加 全部新数据 并刷新
     * @param datas
     */
    public void addAllDatas(List<T> datas) {
        if (mDataList == null) {
            mDataList = new ArrayList<>();
        }
        mDataList.clear();
        mDataList.addAll(datas);
        notifyDataSetChanged();
    }

    /**
     * 设置数据级
     * @param datas
     */
    public void setDataList(List<T> datas) {
        this.mDataList = datas;
        notifyDataSetChanged();
    }
    /**
     * @return 返回数据集
     */
    public List<T> getDataList () {
        return mDataList;
    }

    /**
     * t添加数据 并刷新
     * @param position
     * @param data
     */
    public void addData(int position, T data) {
        if (mDataList == null) {
            mDataList = new ArrayList<>();
        }
        if (position < 0) {
            return;
        }
        if (position >= mDataList.size()) {
            mDataList.add(data);
        }else {
            mDataList.add(position, data);
        }
        // 刷新
        notifyItemInserted(position);
    }

    /**
     * 初始化时获取布局文件id
     *
     * @param itemViewType item子项类型 不区分类型可忽略
     *
     * @return 布局文件resid
     */
    public abstract int getConvertViewResId(int itemViewType);

    /**
     * 初始化时获取ViewHolder
     *
     * @param viewType 子项类型
     * @param rootView 初始化根布局
     *
     * @return 构造好ViewHolder
     */
    public abstract RecyclerView.ViewHolder getViewHolder(int viewType, View rootView);



    /**
     * 基holder
     * @param <T>
     */
    public static abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder{
        private View mRootView ;

        public BaseViewHolder (View itemView) {
            super(itemView);
            mRootView = itemView;
        }

        public View findViewById(int resId){
            return itemView.findViewById(resId);
        }

        /**
         * 刷新数据
         * @param position
         * @param data
         */
        public abstract void onRefreshData(int position, T data);
    }

}
