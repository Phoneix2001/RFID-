package com.android.kepr.RfidReader

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.kepr.R
import com.uk.tsl.rfid.DeviceListActivity
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander
import com.uk.tsl.rfid.asciiprotocol.BuildConfig
import com.uk.tsl.rfid.asciiprotocol.commands.FactoryDefaultsCommand
import com.uk.tsl.rfid.asciiprotocol.device.*
import com.uk.tsl.rfid.asciiprotocol.enumerations.QuerySession
import com.uk.tsl.rfid.asciiprotocol.enumerations.TriState
import com.uk.tsl.rfid.asciiprotocol.responders.LoggerResponder
import com.uk.tsl.utils.Observable
import kotlinx.android.synthetic.main.activity_inventory.*
import kotlinx.android.synthetic.main.fragment_scan_via_rfid.*


class MyCustomScannerFragment : Fragment(R.layout.fragment_scan_via_rfid) {
    private val TAG = "MyCustomScannerFragment"
    private val D = BuildConfig.DEBUG

    // The handler for model messages
    private lateinit var  mGenericModelHandler: GenericHandler
    // The list of results from actions
    private var mResultsArrayAdapter: ArrayAdapter<String>? = null
    private var mResultsListView: ListView? = null








 /*   // Error report
    private var mResultTextView: TextView? = null*/

    // Custom adapter for the session values to display the description rather than the toString() value
    class SessionArrayAdapter(
        context: Context?,
        textViewResourceId: Int,
        private val mValues: Array<QuerySession>
    ) :
        ArrayAdapter<QuerySession?>(context!!, textViewResourceId, mValues) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            view.text = mValues[position].description
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view = super.getDropDownView(position, convertView, parent) as TextView
            view.text = mValues[position].description
            return view
        }
    }

    // The session
    private val mSessions = arrayOf(
        QuerySession.SESSION_0

    )

    // The list of sessions that can be selected
    private var mSessionArrayAdapter: SessionArrayAdapter? = null

    // All of the reader inventory tasks are handled by this class
    private var mModel: InventoryModel? = null

    // The Reader currently in use
    private var mReader: Reader? = null
    private var mLastUserDisconnectedReader: Reader? = null
    private var mIsSelectingReader = false

    // Start stop buttons
    var mStartButton: Button? = null
    var mStopButton: Button? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
      /*  mResultTextView = view.findViewById<View>(R.id.resultTextView) as TextView
        mResultsListView = view.findViewById<View>(R.id.resultListView) as ListView
        mBarcodeResultsListView = view.findViewById<View>(R.id.barcodeListView) as ListView
        mStartButton = view.findViewById<View>(R.id.scanButton) as Button
        mStopButton = view.findViewById<View>(R.id.scanStopButton) as Button
        mPowerLevelTextView = view.findViewById<View>(R.id.powerTextView) as TextView
        mPowerSeekBar = view.findViewById<View>(R.id.powerSeekBar) as SeekBar*/
        mGenericModelHandler = GenericHandler(this)

        mResultsArrayAdapter = ArrayAdapter(activity!!, R.layout.result_item)
        /*mBarcodeResultsArrayAdapter = ArrayAdapter(activity!!, R.layout.result_item)*/

        tv_connectdevice.setOnClickListener {
            mIsSelectingReader = true
            var index = -1
            if (mReader != null) {
                index = ReaderManager.sharedInstance().readerList.list().indexOf(mReader)
            }
            val selectIntent: Intent = Intent(
                activity,
                DeviceListActivity::class.java
            )
            if (index >= 0) {
                selectIntent.putExtra(DeviceListActivity.EXTRA_DEVICE_INDEX, index)
            }
            startActivityForResult(selectIntent, DeviceListActivity.SELECT_DEVICE_REQUEST)
            UpdateUI()
        }

        // Find and set up the results ListView
mResultsListView = view.findViewById(R.id.lv_tagnumbers)
        // Find and set up the results ListView
        mResultsListView!!.setAdapter(mResultsArrayAdapter)
        mResultsListView!!.setFastScrollEnabled(true)

      /*  mBarcodeResultsListView!!.setAdapter(mBarcodeResultsArrayAdapter)
        mBarcodeResultsListView!!.setFastScrollEnabled(true)*/

        // Hook up the button actions

        // Hook up the button actions
      /*  mStartButton!!.setOnClickListener(mScanButtonListener)

        mStopButton!!.setOnClickListener(mScanStopButtonListener)*/
       /* mStopButton!!.isEnabled = false*/

//        val cButton = view.findViewById<View>(R.id.clearButton) as Button
        /*cButton.setOnClickListener(mClearButtonListener)*/

        // The SeekBar provides an integer value for the antenna power

        // The SeekBar provides an integer value for the antenna power

       /* mPowerSeekBar!!.setOnSeekBarChangeListener(mPowerSeekBarListener)*/

        mSessionArrayAdapter = MyCustomScannerFragment.SessionArrayAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            mSessions
        )
        // Find and set up the sessions spinner
        // Find and set up the sessions spinner
        val spinner = view.findViewById<View>(R.id.sessionSpinner) as Spinner
        mSessionArrayAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = mSessionArrayAdapter
        spinner.onItemSelectedListener = mActionSelectedListener
        spinner.setSelection(0)

        // Set up the "Uniques Only" Id check box listener

        // Set up the "Uniques Only" Id check box listener
       /* val ucb = view.findViewById<View>(R.id.uniquesCheckBox) as CheckBox
        ucb.setOnClickListener(mUniquesCheckBoxListener)*/

        // Set up Fast Id check box listener

        // Set up Fast Id check box listener
      /*  val cb = view.findViewById<View>(R.id.fastIdCheckBox) as CheckBox
        cb.setOnClickListener(mFastIdCheckBoxListener)*/

        // Ensure the shared instance of AsciiCommander exists

        // Ensure the shared instance of AsciiCommander exists
        AsciiCommander.createSharedInstance(requireContext())

        val commander: AsciiCommander = getCommander()

        // Ensure that all existing responders are removed

        // Ensure that all existing responders are removed
        commander.clearResponders()

        // Add the LoggerResponder - this simply echoes all lines received from the reader to the log
        // and passes the line onto the next responder
        // This is added first so that no other responder can consume received lines before they are logged.

        // Add the LoggerResponder - this simply echoes all lines received from the reader to the log
        // and passes the line onto the next responder
        // This is added first so that no other responder can consume received lines before they are logged.
        commander.addResponder(LoggerResponder())

        // Add a synchronous responder to handle synchronous commands

        // Add a synchronous responder to handle synchronous commands
        commander.addSynchronousResponder()

        // Create the single shared instance for this ApplicationContext

        // Create the single shared instance for this ApplicationContext
        ReaderManager.create(requireContext())

        // Add observers for changes

        // Add observers for changes
        ReaderManager.sharedInstance().readerList.readerAddedEvent().addObserver(mAddedObserver)
        ReaderManager.sharedInstance().readerList.readerUpdatedEvent().addObserver(mUpdatedObserver)
        ReaderManager.sharedInstance().readerList.readerRemovedEvent().addObserver(mRemovedObserver)

        //Create a (custom) model and configure its commander and handler

        //Create a (custom) model and configure its commander and handler
        mModel = InventoryModel()
        mModel!!.setCommander(getCommander())
        mModel!!.setHandler(mGenericModelHandler)
    }


    companion object {
        fun newInstance(): MyCustomScannerFragment {
            return MyCustomScannerFragment()
        }
    }

     override fun onDestroy() {
        super.onDestroy()

        // Remove observers for changes
        ReaderManager.sharedInstance().readerList.readerAddedEvent().removeObserver(mAddedObserver)
        ReaderManager.sharedInstance().readerList.readerUpdatedEvent()
            .removeObserver(mUpdatedObserver)
        ReaderManager.sharedInstance().readerList.readerRemovedEvent()
            .removeObserver(mRemovedObserver)
    }


    //----------------------------------------------------------------------------------------------
    // Pause & Resume life cycle
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Pause & Resume life cycle
    //----------------------------------------------------------------------------------------------
    @Synchronized
    override fun onPause() {
        super.onPause()
        mModel!!.setEnabled(false)

        // Unregister to receive notifications from the AsciiCommander
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(mCommanderMessageReceiver)

        // Disconnect from the reader to allow other Apps to use it
        // unless pausing when USB device attached or using the DeviceListActivity to select a Reader
        if (!mIsSelectingReader && !ReaderManager.sharedInstance()
                .didCauseOnPause() && mReader != null
        ) {
            mReader!!.disconnect()
        }
        ReaderManager.sharedInstance().onPause()
    }

    @Synchronized
    override fun onResume() {
        super.onResume()
        mModel!!.setEnabled(true)

        // Register to receive notifications from the AsciiCommander
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(
            mCommanderMessageReceiver,
            IntentFilter(AsciiCommander.STATE_CHANGED_NOTIFICATION)
        )

        // Remember if the pause/resume was caused by ReaderManager - this will be cleared when ReaderManager.onResume() is called
        val readerManagerDidCauseOnPause = ReaderManager.sharedInstance().didCauseOnPause()

        // The ReaderManager needs to know about Activity lifecycle changes
        ReaderManager.sharedInstance().onResume()

        // The Activity may start with a reader already connected (perhaps by another App)
        // Update the ReaderList which will add any unknown reader, firing events appropriately
        ReaderManager.sharedInstance().updateList()

        // Locate a Reader to use when necessary
        AutoSelectReader(!readerManagerDidCauseOnPause)
        mIsSelectingReader = false
        displayReaderState()
        UpdateUI()
    }


    //----------------------------------------------------------------------------------------------
    // ReaderList Observers
    //----------------------------------------------------------------------------------------------
    var mAddedObserver: Observable.Observer<Reader> =
        Observable.Observer { observable, reader -> // See if this newly added Reader should be used
            AutoSelectReader(true)
        }

    var mUpdatedObserver =
        Observable.Observer<Reader> { observable, reader ->
            // Is this a change to the last actively disconnected reader
            if (reader === mLastUserDisconnectedReader) {
                // Things have changed since it was actively disconnected so
                // treat it as new
                mLastUserDisconnectedReader = null
            }

            // Was the current Reader disconnected i.e. the connected transport went away or disconnected
            if (reader === mReader && !reader.isConnected) {
                // No longer using this reader
                mReader = null

                // Stop using the old Reader
                getCommander().reader = mReader
            } else {
                // See if this updated Reader should be used
                // e.g. the Reader's USB transport connected
                AutoSelectReader(true)
            }
        }

    var mRemovedObserver =
        Observable.Observer<Reader> { observable, reader ->
            // Is this a change to the last actively disconnected reader
            if (reader === mLastUserDisconnectedReader) {
                // Things have changed since it was actively disconnected so
                // treat it as new
                mLastUserDisconnectedReader = null
            }

            // Was the current Reader removed
            if (reader === mReader) {
                mReader = null

                // Stop using the old Reader
                getCommander().reader = mReader
            }
        }


    private fun AutoSelectReader(attemptReconnect: Boolean) {
        val readerList = ReaderManager.sharedInstance().readerList
        var usbReader: Reader? = null
        if (readerList.list().size >= 1) {
            // Currently only support a single USB connected device so we can safely take the
            // first CONNECTED reader if there is one
            for (reader in readerList.list()) {
                if (reader.hasTransportOfType(TransportType.USB)) {
                    usbReader = reader
                    break
                }
            }
        }
        if (mReader == null) {
            if (usbReader != null && usbReader !== mLastUserDisconnectedReader) {
                // Use the Reader found, if any
                mReader = usbReader
                getCommander().reader = mReader
            }
        } else {
            // If already connected to a Reader by anything other than USB then
            // switch to the USB Reader
            val activeTransport = mReader!!.getActiveTransport()
            if (activeTransport != null && activeTransport.type() != TransportType.USB && usbReader != null) {
                mReader!!.disconnect()
                mReader = usbReader

                // Use the Reader found, if any
                getCommander().reader = mReader
            }
        }

        // Reconnect to the chosen Reader
        if (mReader != null && !mReader!!.isConnecting()
            && (mReader!!.getActiveTransport() == null || mReader!!.getActiveTransport()
                .connectionStatus().value() == ConnectionState.DISCONNECTED)
        ) {
            // Attempt to reconnect on the last used transport unless the ReaderManager is cause of OnPause (USB device connecting)
            if (attemptReconnect) {
                if (mReader!!.allowMultipleTransports() || mReader!!.getLastTransportType() == null) {
                    // Reader allows multiple transports or has not yet been connected so connect to it over any available transport
                    mReader!!.connect()
                } else {
                    // Reader supports only a single active transport so connect to it over the transport that was last in use
                    mReader!!.connect(mReader!!.getLastTransportType())
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Menu
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Menu
    //----------------------------------------------------------------------------------------------
    private var mConnectMenuItem: MenuItem? = null
    private var mDisconnectMenuItem: MenuItem? = null
    private var mResetMenuItem: MenuItem? = null

    /* fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.reader_menu, menu)
        mResetMenuItem = menu.findItem(R.id.reset_reader_menu_item)
        mConnectMenuItem = menu.findItem(R.id.connect_reader_menu_item)
        mDisconnectMenuItem = menu.findItem(R.id.disconnect_reader_menu_item)
        return true
    }*/

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.reader_menu, menu)
        mResetMenuItem = menu.findItem(R.id.reset_reader_menu_item)
        mConnectMenuItem = menu.findItem(R.id.connect_reader_menu_item)
        mDisconnectMenuItem = menu.findItem(R.id.disconnect_reader_menu_item)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * Prepare the menu options
     */
    /* override fun onPrepareOptionsMenu(menu: Menu?) {
        val isConnecting = getCommander().connectionState == ConnectionState.CONNECTING
        val isConnected = getCommander().isConnected
        mDisconnectMenuItem!!.isEnabled = isConnected
        mConnectMenuItem!!.isEnabled = true
        mConnectMenuItem!!.setTitle(if (mReader != null && mReader!!.isConnected()) "Change Reader" else "Change Reader")
        mResetMenuItem!!.isEnabled = isConnected
        return super.onPrepareOptionsMenu(menu!!)
    }*/

    override fun onPrepareOptionsMenu(menu: Menu) {
        val isConnecting = getCommander().connectionState == ConnectionState.CONNECTING
        val isConnected = getCommander().isConnected
        mDisconnectMenuItem!!.isEnabled = isConnected
        mConnectMenuItem!!.isEnabled = true
        mConnectMenuItem!!.setTitle(if (mReader != null && mReader!!.isConnected()) "Change Reader" else "Change Reader")
        mResetMenuItem!!.isEnabled = isConnected
        super.onPrepareOptionsMenu(menu)
    }
    /**
     * Respond to menu item selections
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.reset_reader_menu_item -> {
                resetReader()
                UpdateUI()
                return true
            }
            R.id.connect_reader_menu_item -> {
                // Launch the DeviceListActivity to see available Readers
                mIsSelectingReader = true
                var index = -1
                if (mReader != null) {
                    index = ReaderManager.sharedInstance().readerList.list().indexOf(mReader)
                }
                val selectIntent: Intent = Intent(
                    activity,
                    DeviceListActivity::class.java
                )
                if (index >= 0) {
                    selectIntent.putExtra(DeviceListActivity.EXTRA_DEVICE_INDEX, index)
                }
                startActivityForResult(selectIntent, DeviceListActivity.SELECT_DEVICE_REQUEST)
                UpdateUI()
                return true
            }
            R.id.disconnect_reader_menu_item -> {
                if (mReader != null) {
                    mReader!!.disconnect()
                    mLastUserDisconnectedReader = mReader
                    mReader = null
                    displayReaderState()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    //----------------------------------------------------------------------------------------------
    // Model notifications
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Model notifications
    //----------------------------------------------------------------------------------------------
    private class GenericHandler(t: MyCustomScannerFragment?) :
        WeakHandler<MyCustomScannerFragment?>(t) {


        override fun handleMessage(msg: Message?, t: MyCustomScannerFragment?) {
            try {
                when (msg?.what) {
                    ModelBase.BUSY_STATE_CHANGED_NOTIFICATION -> {}
                    ModelBase.MESSAGE_NOTIFICATION -> {
                        // Examine the message for prefix
                        val message = msg.obj as String
                        if (message.startsWith("ER:")) {
                            t?.displaysomething?.text = message.substring(3)
//                            t?.mResultTextView?.text = message.substring(3)
//                            t?.mResultTextView?.setBackgroundColor(-0x2f000001)
                        } else if (message.startsWith("BC:")) {
//                            t?.mBarcodeResultsListView?.visibility = View.VISIBLE
//                            t?.mBarcodeResultsArrayAdapter?.add(message)
//                            t?.scrollBarcodeListViewToBottom()
                        } else {
                            t?.mResultsArrayAdapter?.add(message)
                            t?.scrollResultsListViewToBottom()
                        }
                        t?.UpdateUI()
                    }
                    else -> {}
                }
            } catch (e: Exception) {
            }
        }
    }



    //----------------------------------------------------------------------------------------------
    // UI state and display update
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // UI state and display update
    //----------------------------------------------------------------------------------------------
    private fun displayReaderState() {
        var connectionMsg: String? = "Reader: "
        connectionMsg += when (getCommander().connectionState) {
            ConnectionState.CONNECTED -> getCommander().connectedDeviceName
            ConnectionState.CONNECTING -> "Connecting..."
            else -> "Disconnected"
        }
        txt_waiting_for_scan.text =connectionMsg
    }


    //
    // Set the state for the UI controls
    //
    private fun UpdateUI() {
        //boolean isConnected = getCommander().isConnected();
        //TODO: configure UI control state
    }


    private fun scrollResultsListViewToBottom() {
        mResultsListView!!.post { // Select the last row so it will scroll into view...
            mResultsListView!!.setSelection(mResultsArrayAdapter!!.count - 1)
        }
    }

/*    private fun scrollBarcodeListViewToBottom() {
        mBarcodeResultsListView!!.post { // Select the last row so it will scroll into view...
            mBarcodeResultsListView!!.setSelection(mBarcodeResultsArrayAdapter!!.count - 1)
        }
    }*/


    //----------------------------------------------------------------------------------------------
    // AsciiCommander message handling
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // AsciiCommander message handling
    //----------------------------------------------------------------------------------------------
    /**
     * @return the current AsciiCommander
     */
    protected fun getCommander(): AsciiCommander {
        return AsciiCommander.sharedInstance()
    }

    //
    // Handle the messages broadcast from the AsciiCommander
    //

    private val mCommanderMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (D) {
                Log.d(
                    javaClass.name,
                    "AsciiCommander state changed - isConnected: " + getCommander().isConnected
                )
            }
            val connectionStateMsg = intent.getStringExtra(AsciiCommander.REASON_KEY)
            displayReaderState()
           /* if (getCommander().isConnected) {
                // Update for any change in power limits
                setPowerBarLimits()
                // This may have changed the current power level setting if the new range is smaller than the old range
                // so update the model's inventory command for the new power value
                mModel!!.command.outputPower = mPowerLevel
                mModel!!.resetDevice()
                mModel!!.updateConfiguration()
            }*/
            UpdateUI()
        }
    }
    //----------------------------------------------------------------------------------------------
    // Reader reset
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Reader reset
    //----------------------------------------------------------------------------------------------
    //
    // Handle reset controls
    //
    private fun resetReader() {
        try {
            // Reset the reader
            val fdCommand = FactoryDefaultsCommand.synchronousCommand()
            fdCommand.resetParameters = TriState.YES
            getCommander().executeCommand(fdCommand)
            val msg = "Reset " + if (fdCommand.isSuccessful) "succeeded" else "failed"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            UpdateUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    //----------------------------------------------------------------------------------------------
    // Power seek bar
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Power seek bar
    //----------------------------------------------------------------------------------------------
    //
    // Set the seek bar to cover the range of the currently connected device
    // The power level is set to the new maximum power
    //
   /* private fun setPowerBarLimits() {
        val deviceProperties = getCommander().deviceProperties
        mPowerSeekBar!!.max =
            deviceProperties.maximumCarrierPower - deviceProperties.minimumCarrierPower
        mPowerLevel = deviceProperties.maximumCarrierPower
        mPowerSeekBar!!.progress = mPowerLevel - deviceProperties.minimumCarrierPower
    }*/


    //
    // Handle events from the power level seek bar. Update the mPowerLevel member variable for use in other actions
    //
/*
    private val mPowerSeekBarListener: SeekBar.OnSeekBarChangeListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Nothing to do here
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

                // Update the reader's setting only after the user has finished changing the value
                updatePowerSetting(getCommander().deviceProperties.minimumCarrierPower + seekBar.progress)
                mModel!!.command.outputPower = mPowerLevel
                mModel!!.updateConfiguration()
            }

            override fun onProgressChanged(
                seekBar: SeekBar, progress: Int,
                fromUser: Boolean
            ) {
                updatePowerSetting(getCommander().deviceProperties.minimumCarrierPower + progress)
            }
        }
*/

   /* private fun updatePowerSetting(level: Int) {
        mPowerLevel = level
        mPowerLevelTextView!!.text = "$mPowerLevel dBm"
    }
*/

    //----------------------------------------------------------------------------------------------
    // Button event handlers
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Button event handlers
    //----------------------------------------------------------------------------------------------
    // Scan (Start) action
/*    private val mScanButtonListener = View.OnClickListener {
        try {
            mResultTextView!!.text = ""
            // Start the continuous inventory
            mModel!!.scanStart()
            mStartButton!!.isEnabled = false
            mStopButton!!.isEnabled = true
            mBarcodeResultsListView!!.visibility = View.GONE
            UpdateUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/

    // Scan Stopaction
  /*  private val mScanStopButtonListener = View.OnClickListener {
        try {
            mResultTextView!!.text = ""
            // Stop the continuous inventory
            mModel!!.scanStop()
            mStartButton!!.isEnabled = true
            mStopButton!!.isEnabled = false
            UpdateUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/

    // Clear action
    /*private val mClearButtonListener = View.OnClickListener {
        try {
            // Clear the list
            mResultsArrayAdapter!!.clear()
            mResultTextView!!.text = ""
            mResultTextView!!.setBackgroundColor(0x00FFFFFF)
            mBarcodeResultsArrayAdapter!!.clear()
            mModel!!.clearUniques()
            mBarcodeResultsListView!!.visibility = View.VISIBLE
            UpdateUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/

    //----------------------------------------------------------------------------------------------
    // Handler for changes in session
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Handler for changes in session
    //----------------------------------------------------------------------------------------------
    private val mActionSelectedListener: AdapterView.OnItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                if (mModel!!.command != null) {
                    val targetSession = parent.getItemAtPosition(pos) as QuerySession
                    mModel!!.command.querySession = targetSession
                    mModel!!.updateConfiguration()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


    //----------------------------------------------------------------------------------------------
    // Handler for changes in Uniques Only
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Handler for changes in Uniques Only
    //----------------------------------------------------------------------------------------------
    private val mUniquesCheckBoxListener =
        View.OnClickListener { v ->
            try {
                val uniquesCheckBox = v as CheckBox
                mModel!!.setUniquesOnly(uniquesCheckBox.isChecked)
                UpdateUI()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    //----------------------------------------------------------------------------------------------
    // Handler for changes in FastId
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Handler for changes in FastId
    //----------------------------------------------------------------------------------------------
   /* private val mFastIdCheckBoxListener =
        View.OnClickListener { v ->
            try {
                val fastIdCheckBox = v as CheckBox
                mModel!!.command.usefastId =
                    if (fastIdCheckBox.isChecked) TriState.YES else TriState.NO
                mModel!!.updateConfiguration()
                UpdateUI()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
*/

    //----------------------------------------------------------------------------------------------
    // Handler for DeviceListActivity
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Handler for DeviceListActivity
    //----------------------------------------------------------------------------------------------
    //
    // Handle Intent results
    //
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            DeviceListActivity.SELECT_DEVICE_REQUEST ->                 // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    val readerIndex = data?.extras!!.getInt(DeviceListActivity.EXTRA_DEVICE_INDEX)
                    val chosenReader = ReaderManager.sharedInstance().readerList.list()[readerIndex]
                    val action = data.extras!!.getInt(DeviceListActivity.EXTRA_DEVICE_ACTION)

                    // If already connected to a different reader then disconnect it
                    if (mReader != null) {
                        if (action == DeviceListActivity.DEVICE_CHANGE || action == DeviceListActivity.DEVICE_DISCONNECT) {
                            mReader!!.disconnect()
                            if (action == DeviceListActivity.DEVICE_DISCONNECT) {
                                mLastUserDisconnectedReader = mReader
                                mReader = null
                            }
                        }
                    }

                    // Use the Reader found
                    if (action == DeviceListActivity.DEVICE_CHANGE || action == DeviceListActivity.DEVICE_CONNECT) {
                        mReader = chosenReader
                        mLastUserDisconnectedReader = null
                        getCommander().reader = mReader
                    }
                    displayReaderState()
                }
        }
    }
   /* override  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

    }*/

}