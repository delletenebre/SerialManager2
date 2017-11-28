package kg.delletenebre.serialmanager2

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_widgets.*

class WidgetsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widgets)

        xRecyclerView.layoutManager = LinearLayoutManager(this)
        xRecyclerView.adapter = WidgetsActivityListAdapter()
        xRecyclerView.setHasFixedSize(true)
        xRecyclerView.addItemDecoration(
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun onResume() {
        super.onResume()
        (xRecyclerView.adapter as WidgetsActivityListAdapter).updateRealmInstance()
    }
}