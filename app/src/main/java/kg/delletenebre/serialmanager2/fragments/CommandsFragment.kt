package kg.delletenebre.serialmanager2.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kg.delletenebre.serialmanager2.R
import kg.delletenebre.serialmanager2.commands.CommandEditActivity


class CommandsFragment: androidx.fragment.app.Fragment() {
    private lateinit var mRecyclerView: androidx.recyclerview.widget.RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // return super.onCreateView(inflater, container, savedInstanceState)
        val layoutInflater = activity!!.layoutInflater
        val layout = layoutInflater.inflate(R.layout.fragment_commands, container, false)

        mRecyclerView = layout.findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        mRecyclerView.adapter = CommandsAdapter()
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.addItemDecoration(
                androidx.recyclerview.widget.DividerItemDecoration(context, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL))
        setupItemTouchHelper(context!!, mRecyclerView)

        layout.findViewById<FloatingActionButton>(R.id.floating_action_button).setOnClickListener {
            val intent = Intent(context, CommandEditActivity::class.java)
            intent.putExtra("CommandIndex", mRecyclerView.adapter!!.itemCount)
            intent.putExtra("isNew", true)
            context?.startActivity(intent)
        }

        return layout
    }

    override fun onResume() {
        super.onResume()
        (mRecyclerView.adapter as CommandsAdapter).updateRealmInstance()
    }


    private fun setupItemTouchHelper(context: Context, recyclerView: androidx.recyclerview.widget.RecyclerView) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT) {

            // we want to cache these and not allocate anything repeatedly in the onChildDraw method
            internal lateinit var mBackground: Drawable
            internal var mMark: Drawable? = null
            internal var mMarkMargin: Int = 0
            internal var mInitiated: Boolean = false

            private fun init() {
                mBackground = ColorDrawable(ContextCompat.getColor(context, R.color.danger))
                mMark = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp)
                mMark?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                mMarkMargin = resources.getDimension(R.dimen.ic_clear_margin).toInt()
                mInitiated = true
            }

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val itemIndex = viewHolder.adapterPosition
                        val adapter = recyclerView.adapter as CommandsAdapter
                        AlertDialog.Builder(context)
                                .setTitle(getString(R.string.dialog_title_confirm_delete_command))
                                .setMessage(String.format(getString(R.string.dialog_message_delete_command),
                                        adapter.getItemTitle(context, itemIndex)))
                                .setPositiveButton(getString(R.string.dialog_delete_positive)) { dialog, _ ->
                                    adapter.remove(itemIndex)
                                    dialog.dismiss()
                                }
                                .setNeutralButton(getString(R.string.dialog_negative)) { dialog, _ ->
                                    adapter.notifyItemChanged(itemIndex)
                                    dialog.cancel()
                                }
                                .create()
                                .show()
            }

            override fun onMove(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                                target: androidx.recyclerview.widget.RecyclerView.ViewHolder): Boolean {
                val moveFrom = viewHolder.adapterPosition
                val moveTo = target.adapterPosition
                (recyclerView.adapter as CommandsAdapter).updatePositions(moveFrom, moveTo)
                return true
            }

            override fun getMovementFlags(recyclerView: androidx.recyclerview.widget.RecyclerView,
                                          viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags = ItemTouchHelper.LEFT
                return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onChildDraw(canvas: Canvas, recyclerView: androidx.recyclerview.widget.RecyclerView,
                                     viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                                     dX: Float, dY: Float,
                                     actionState: Int, isCurrentlyActive: Boolean) {

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView

                    // not sure why, but this method get's called for viewholder that are already swiped away
                    if (viewHolder.adapterPosition == -1) {
                        // not interested in those
                        return
                    }

                    if (!mInitiated) {
                        init()
                    }

                    // draw red background
                    mBackground.setBounds(itemView.right + dX.toInt(),
                            itemView.top, itemView.right, itemView.bottom)
                    mBackground.draw(canvas)

                    // draw x mark
                    val itemHeight = itemView.bottom - itemView.top
                    val intrinsicWidth = mMark?.intrinsicWidth ?: 0
                    val intrinsicHeight = mMark?.intrinsicWidth ?: 0
                    val xMarkLeft = itemView.right - mMarkMargin - intrinsicWidth
                    val xMarkRight = itemView.right - mMarkMargin
                    val xMarkTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                    val xMarkBottom = xMarkTop + intrinsicHeight
                    mMark?.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom)
                    mMark?.draw(canvas)
                }
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(recyclerView)
    }
}