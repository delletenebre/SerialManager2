package kg.delletenebre.serialmanager2.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Selection
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import kg.delletenebre.serialmanager2.App
import kg.delletenebre.serialmanager2.R
import java.text.SimpleDateFormat
import java.util.*


class LogsFragment : androidx.fragment.app.Fragment() {
    private lateinit var mTextView: TextView
    private lateinit var mAutoscrollCheckbox: CheckBox
    private lateinit var mLocalBroadcastManager: androidx.localbroadcastmanager.content.LocalBroadcastManager
    private lateinit var mLocalBroadcastReceiver: BroadcastReceiver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layoutInflater = activity!!.layoutInflater
        val layout = layoutInflater.inflate(R.layout.fragment_logs, container, false)

        mTextView = layout.findViewById(R.id.logs_text)
        mAutoscrollCheckbox = layout.findViewById(R.id.autoscroll_checkbox)

        val clearButton : Button = layout.findViewById(R.id.clear_button)
        clearButton.setOnClickListener {
            mTextView.text = ""
        }

        val sendButton : Button = layout.findViewById(R.id.send_button)
        sendButton.setOnClickListener {
            val sendTextView : TextView = layout.findViewById(R.id.send_text)
            sendData(sendTextView.text.toString())
        }

        mTextView.movementMethod = ScrollingMovementMethod()

        mLocalBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    App.LOCAL_ACTION_CONNECTION_ESTABLISHED -> {
                        addMessage("${App.ICONS["ok"]} ${App.ICONS[intent.getStringExtra("type")]}\t${getString(R.string.connection_established)}\t[ ${intent.getStringExtra("name")} ]")
                    }

                    App.LOCAL_ACTION_CONNECTION_CLOSED -> {
                        addMessage("${App.ICONS["cancel"]} ${App.ICONS[intent.getStringExtra("type")]}\t${getString(R.string.connection_closed)}\t[ ${intent.getStringExtra("name")} ]")
                    }

                    App.LOCAL_ACTION_COMMAND_RECEIVED -> {
                        addMessage("${App.ICONS["receive"]} ${App.ICONS[intent.getStringExtra("from")]}\t${intent.getStringExtra("command")}")
                    }

                    App.LOCAL_ACTION_DATA_SENT -> {
                        addMessage("${App.ICONS["send"]} ${App.ICONS["controller"]}\t${intent.getStringExtra("data")}")
                    }
                }
            }
        }
        val localIntentFilter = IntentFilter()
        localIntentFilter.addAction(App.LOCAL_ACTION_CONNECTION_ESTABLISHED)
        localIntentFilter.addAction(App.LOCAL_ACTION_CONNECTION_CLOSED)
        localIntentFilter.addAction(App.LOCAL_ACTION_COMMAND_RECEIVED)
        localIntentFilter.addAction(App.LOCAL_ACTION_DATA_SENT)
        mLocalBroadcastManager = androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context!!)
        mLocalBroadcastManager.registerReceiver(mLocalBroadcastReceiver, localIntentFilter)

        return layout
    }

    override fun onDestroy() {
        mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver)
        super.onDestroy()
    }

    fun addMessage(message: String) {
        if (message.isNotBlank()) {
            val timestamp = (SimpleDateFormat("HH:mm:ss", Locale.ROOT))
                    .format(Calendar.getInstance().time)

            var crlf = ""
            if (message.last() != '\n') {
                crlf = "\r\n"
            }
            mTextView.append("$timestamp\t$message$crlf")
            mTextView.text = mTextView.text // update text to apply lineheight

            val editable = mTextView.editableText
            if (mAutoscrollCheckbox.isChecked) {
                Selection.setSelection(editable, editable.length)
            } else {
                Selection.removeSelection(editable)
            }
        }
    }

    fun sendData(data: String) {
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context!!).sendBroadcast(
                Intent(App.LOCAL_ACTION_SEND_DATA)
                        .putExtra("data", data))
    }
}