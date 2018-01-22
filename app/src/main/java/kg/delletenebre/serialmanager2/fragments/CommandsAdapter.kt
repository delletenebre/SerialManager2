package kg.delletenebre.serialmanager2.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmResults
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.R
import kg.delletenebre.serialmanager2.commands.CommandEditActivity
import kg.delletenebre.serialmanager2.commands.CommandModel

class CommandsAdapter : RecyclerView.Adapter<CommandsAdapter.ViewHolder>() {

    private var mRealm: Realm? = null
    private var mItems: OrderedRealmCollection<CommandModel>? = null

    // Provide a suitable constructor (depends on the kind of dataset)
    init {
        updateRealmInstance()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.command, parent, false)

        return ViewHolder(layout)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = mItems!![position]

        viewHolder.itemView.setBackgroundColor(Color.WHITE)
        viewHolder.mTitle.visibility = View.VISIBLE
        viewHolder.mSummary.visibility = View.VISIBLE

        viewHolder.mTitle.text = item.getTitleLabel(App.getInstance().applicationContext)
        viewHolder.mSummary.text = item.getSubtitleLabel(App.getInstance().applicationContext)

        viewHolder.mUndo.visibility = View.GONE
        viewHolder.mUndo.setOnClickListener(null)

        viewHolder.mCommand = item
    }

    override fun getItemCount(): Int {
        return if (mItems == null) {
            0
        } else mItems!!.size

    }

    fun updateRealmInstance() {
        mRealm = App.getInstance().realm
        mItems = mRealm!!.where(CommandModel::class.java).findAllAsync().sort("index")
        notifyDataSetChanged()
    }

    fun getItemTitle(context: Context, position: Int): String {
        val item = mItems!![position]
        return item.getTitleLabel(context)
    }

    fun add(position: Int, command: CommandModel) {
        mItems!!.add(position, command)
        notifyItemInserted(position)
    }

    fun updatePositions(oldIndex: Int, newIndex: Int) {
        mRealm!!.executeTransaction { realm ->
            val movedCommand = realm.where(CommandModel::class.java).equalTo("index", oldIndex)
                    .findFirst()
            if (movedCommand != null) {
                val commands: RealmResults<CommandModel>
                var way = 1
                if (oldIndex < newIndex) {
                    commands = realm.where(CommandModel::class.java)
                            .greaterThan("index", oldIndex)
                            .lessThanOrEqualTo("index", newIndex)
                            .findAll()
                    way = -1
                } else {
                    commands = realm.where(CommandModel::class.java)
                            .greaterThanOrEqualTo("index", newIndex)
                            .lessThan("index", oldIndex)
                            .findAll()
                }
                for (command in commands) {
                    command.index = command.index + way
                }
                movedCommand.index = newIndex
                notifyItemMoved(oldIndex, newIndex)
            }
        }
    }

    fun remove(position: Int) {
        val item = mItems!![position]
        if (mItems!!.contains(item)) {
            mRealm!!.executeTransaction { realm ->
                val removedCommand = realm.where(CommandModel::class.java)
                        .equalTo("index", position)
                        .findFirst()
                if (removedCommand != null) {
                    removedCommand.deleteFromRealm()

                    val commands = realm.where(CommandModel::class.java)
                            .greaterThan("index", position)
                            .findAll()
                    for (command in commands) {
                        command.index = command.index - 1
                    }

                    notifyItemRemoved(position)
                }
            }
        }
    }

    class ViewHolder internal constructor(layout: View) : RecyclerView.ViewHolder(layout), View.OnClickListener {
        internal var mTitle: TextView = layout.findViewById(R.id.title)
        internal var mSummary: TextView = layout.findViewById(R.id.summary)
        internal var mUndo: Button = layout.findViewById(R.id.undo_button)
        internal var mCommand: CommandModel? = null

        init {
            layout.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val context = view.context
            val intent = Intent(view.context, CommandEditActivity::class.java)
            intent.putExtra("CommandIndex", mCommand!!.index)
            context.startActivity(intent)
        }
    }


}