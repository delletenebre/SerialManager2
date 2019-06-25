package kg.delletenebre.serialmanager2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kg.delletenebre.serialmanager2.R

class WidgetsFragment : androidx.fragment.app.Fragment() {
    private lateinit var mRecyclerView: androidx.recyclerview.widget.RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layoutInflater = activity!!.layoutInflater
        val layout = layoutInflater.inflate(R.layout.fragment_widgets, container, false)

        mRecyclerView = layout.findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        mRecyclerView.adapter = WidgetsAdapter()
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.addItemDecoration(
                androidx.recyclerview.widget.DividerItemDecoration(context, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL))

        return layout
    }

    override fun onResume() {
        super.onResume()
        (mRecyclerView.adapter as WidgetsAdapter).updateRealmInstance()
    }
}