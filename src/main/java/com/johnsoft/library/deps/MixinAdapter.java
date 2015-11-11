package com.johnsoft.library.deps;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

/**
 * This is a mixin class with RecyclerView.Adapter and BaseAdapter.<br>
 * But method in these two class isn't compatibility. You need check getAdapterType() to choose which one is on.<br>
 * The abstract method in BaseAdapter which named getCount() will return getItemCount(),
 * and the method named getItemId(int position) will return position,
 * the method in BaseAdapter which named notifyDataSetChanged() and named notifyDataSetInvalidated() had changed with
 * fireDataSetChanged() and fireDataSetInvalidated().<br>
 * You can override getView(int position, View convertView, ViewGroup parent) for your custom set,
 * you also can use the default implements, it will apply onCreateViewHolder and onBindViewHolder to handle every thing, just define a ViewHolder.
 *
 * 这是一个混合类, 混合了RecyclerView.Adapter和BaseAdapter, 以便可无缝切换在RecyclerView和AdapterView.<br>
 * 但这两者的方法不是兼容的, 所以只能使用一边的方法. 可以通过检查getAdapterType()来得知选择的类型是RecyclerView.Adapter还是BaseAdapter.<br>
 * BaseAdapter中的抽象方法getCount()将返回getItemCount(), 只需实现getItemCount()即可, getItemId(int position)会返回参数position, 因为这是常见做法,
 * BaseAdapter中的notifyDataSetChanged()和notifyDataSetInvalidated()已被更名为fireDataSetChanged()和fireDataSetInvalidated().<br>
 * 你可以覆写getView(int position, View convertView, ViewGroup parent)去定制行为, 也可以使用默认实现, 它将通过onCreateViewHolder和onBindViewHolder处理一切,
 * 不过你需要继承实现RecyclerView.ViewHolder
 * @author John Kenrinus Lee
 * @version 2015-04-14
 */
public abstract class MixinAdapter extends RecyclerView.Adapter implements ListAdapter, SpinnerAdapter
{
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    private final AdapterType mAdapterType;

    public MixinAdapter(AdapterType pAdapterType) {
        mAdapterType = pAdapterType;
    }

    //////////////////////////////////////RecyclerView.Adapter BEGIN////////////////////////////////////
    @Override
    public abstract RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup pViewGroup, final int i);

    @Override
    public abstract void onBindViewHolder(final RecyclerView.ViewHolder pViewHolder, final int i);

    @Override
    public abstract int getItemCount();
    //////////////////////////////////////RecyclerView.Adapter END//////////////////////////////////////

    //////////////////////////////////////ListAdapter BEGIN/////////////////////////////////////////////
    @Override
    public boolean areAllItemsEnabled()
    {
        return true;
    }

    @Override
    public boolean isEnabled(final int position)
    {
        return true;
    }
    //////////////////////////////////////ListAdapter END///////////////////////////////////////////////

    //////////////////////////////////////SpinnerAdapter BEGIN//////////////////////////////////////////
    @Override
    public View getDropDownView(final int position, final View convertView, final ViewGroup parent)
    {
        return getView(position, convertView, parent);
    }
    //////////////////////////////////////SpinnerAdapter END////////////////////////////////////////////

    //////////////////////////////////////Override START////////////////////////////////////////////////
    @Override
    public long getItemId(final int position)
    {
        return position;
    }

//    @Override
//    public boolean hasStableIds()
//    {
//        return false;
//    }

    @Override
    public int getItemViewType(final int position)
    {
        return 0;
    }
    //////////////////////////////////////Override END//////////////////////////////////////////////////

    @Override
    public void registerDataSetObserver(final DataSetObserver observer)
    {
        mDataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(final DataSetObserver observer)
    {
        mDataSetObservable.unregisterObserver(observer);
    }

    @Override
    public int getCount()
    {
        return getItemCount();
    }

    @Override
    public abstract Object getItem(final int position);

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        RecyclerView.ViewHolder viewHolder;
        if (convertView == null)
        {
            viewHolder = onCreateViewHolder(parent, position);
            convertView = viewHolder.itemView;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (RecyclerView.ViewHolder)convertView.getTag();
        }
        onBindViewHolder(viewHolder, position);
        return convertView;
    }

    @Override
    public int getViewTypeCount()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return getCount() == 0;
    }

    public void fireDataSetChanged() {
        if (getAdapterType() == AdapterType.RecyclerViewAdapter) {
            throw new IllegalStateException("This method is for BaseAdapter, but getAdapterType() return RecyclerViewAdapter");
        }
        mDataSetObservable.notifyChanged();
    }

    public void fireDataSetInvalidated() {
        if (getAdapterType() == AdapterType.RecyclerViewAdapter) {
            throw new IllegalStateException("This method is for BaseAdapter, but getAdapterType() return RecyclerViewAdapter");
        }
        mDataSetObservable.notifyInvalidated();
    }

    public final AdapterType getAdapterType() {
        return mAdapterType;
    }

    public static enum AdapterType {
        RecyclerViewAdapter, BaseAdapter
    }
}
