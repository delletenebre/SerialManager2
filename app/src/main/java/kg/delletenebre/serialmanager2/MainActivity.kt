package kg.delletenebre.serialmanager2

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewConfiguration
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kg.delletenebre.serialmanager2.communication.CommunicationService
import kg.delletenebre.serialmanager2.fragments.CommandsFragment
import kg.delletenebre.serialmanager2.fragments.LogsFragment
import kg.delletenebre.serialmanager2.fragments.WidgetsFragment
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity: AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!App.getInstance().isSystemOverlaysPermissionGranted) {
            App.getInstance().requestSystemOverlaysPermission()
        }

        try {
            val config = ViewConfiguration.get(this)
            val menuKeyField = ViewConfiguration::class.java.getDeclaredField("sHasPermanentMenuKey")
            menuKeyField.isAccessible = true
            menuKeyField.setBoolean(config, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setupViewPager(xViewPager)
        xTabLayout.setupWithViewPager(xViewPager)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))

            R.id.restart_service -> {
                val communicationServiceIntent = Intent(this, CommunicationService::class.java)
                stopService(communicationServiceIntent)
                startService(communicationServiceIntent)
            }

            R.id.close_application -> {
                val communicationServiceIntent = Intent(this, CommunicationService::class.java)
                stopService(communicationServiceIntent)
                finishAffinity()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupViewPager(viewPager: androidx.viewpager.widget.ViewPager) {
        val commandsFragmentInstance = CommandsFragment()

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(commandsFragmentInstance, getString(R.string.tab_title_commands))
        adapter.addFragment(WidgetsFragment(), getString(R.string.tab_title_widgets))
        adapter.addFragment(LogsFragment(), getString(R.string.tab_title_logs))
        viewPager.adapter = adapter
    }

    inner class ViewPagerAdapter(fm: androidx.fragment.app.FragmentManager) : androidx.fragment.app.FragmentStatePagerAdapter(fm) {
        private val mFragmentList = mutableListOf<androidx.fragment.app.Fragment>()
        private val mFragmentTitleList = mutableListOf<String>()

        override fun getItem(position: Int): androidx.fragment.app.Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitleList[position]
        }

        fun addFragment(fragment: androidx.fragment.app.Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

    }
}
