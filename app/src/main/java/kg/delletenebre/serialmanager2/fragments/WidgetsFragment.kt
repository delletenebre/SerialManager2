package kg.delletenebre.serialmanager2.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kg.delletenebre.serialmanager2.R

class WidgetsFragment : Fragment() {
    private lateinit var mRecyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layoutInflater = activity!!.layoutInflater
        val layout = layoutInflater.inflate(R.layout.fragment_widgets, container, false)

        mRecyclerView = layout.findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(context)
        mRecyclerView.adapter = WidgetsAdapter()
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        return layout
    }

    override fun onResume() {
        super.onResume()
        (mRecyclerView.adapter as WidgetsAdapter).updateRealmInstance()
    }
}