package kg.delletenebre.serialmanager2.fragments

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.realm.OrderedRealmCollection
import io.realm.Realm
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.R
import kg.delletenebre.serialmanager2.widgets.WidgetSimpleModel
import kg.delletenebre.serialmanager2.widgets.WidgetSimpleSettingsActivity

class WidgetsAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<WidgetsAdapter.ViewHolder>() {

    //private lateinit var mItems: OrderedRealmCollection<WidgetSimpleModel>
    private var mRealm: Realm? = null
    private var mItems: OrderedRealmCollection<WidgetSimpleModel>? = null

    init {
        updateRealmInstance()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.widget_list_item_layout, parent, false)

        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mItems!![position]

        holder.widgetId = item.id
        holder.itemView.setBackgroundColor(Color.WHITE)
        holder.mTitle.visibility = View.VISIBLE
        holder.mSummary.visibility = View.VISIBLE
        holder.mTitle.text = item.getTitleLabel(App.getInstance().applicationContext)
        holder.mSummary.text = item.getSubtitleLabel(App.getInstance().applicationContext)

        holder.mItem = item
    }

    override fun getItemCount(): Int {
        return mItems?.size ?: 0
    }

    fun updateRealmInstance() {
        mRealm = App.getInstance().realm
        mItems = mRealm!!.where(WidgetSimpleModel::class.java).findAll().sort("position")
        notifyDataSetChanged()
    }


    class ViewHolder(layout: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(layout), View.OnClickListener {
        var widgetId: Int = 0
        val mTitle: TextView = layout.findViewById(R.id.title)
        val mSummary: TextView = layout.findViewById(R.id.summary)
        lateinit var mItem: WidgetSimpleModel

        init {
            layout.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val context = view.context
            val intent = Intent(view.context, WidgetSimpleSettingsActivity::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            intent.putExtra(App.EXTRA_APPWIDGET_EDIT, true)
            context.startActivity(intent)
        }
    }
}