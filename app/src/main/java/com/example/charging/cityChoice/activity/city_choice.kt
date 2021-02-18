package com.example.charging.cityChoice.activity

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import com.example.charging.R
import com.example.charging.cityChoice.binding.Bind
import com.example.charging.cityChoice.binding.ViewBinder
import com.example.charging.cityChoice.model.CityEntity
import com.example.charging.cityChoice.utils.JsonReadUtil
import com.example.charging.cityChoice.utils.ScreenUtils
import com.example.charging.cityChoice.utils.ToastUtils
import com.example.charging.cityChoice.view.LetterListView
import org.json.JSONException
import org.json.JSONObject
import java.util.*


class city_choice : AppCompatActivity(), AbsListView.OnScrollListener {
    private val CityFileName = "allcity.json"
    @Bind(R.id.tool_bar_fl)
    private val mToolbar: FrameLayout? = null

    @Bind(R.id.search_locate_content_et)
    private val searchContentEt: EditText? = null

    @Bind(R.id.total_city_lv)
    private val totalCityLv: ListView? = null

    @Bind(R.id.total_city_letters_lv)
    private val lettersLv: LetterListView? = null

    @Bind(R.id.search_city_lv)
    private val searchCityLv: ListView? = null

    @Bind(R.id.no_search_result_tv)
    private val noSearchDataTv: TextView? = null
    private var handler: Handler? = null
    private var overlay // 对话框首字母TextView
            : TextView? = null
    private var overlayThread=OverlayThread();// 显示首字母对话框
    private var mReady = false
    private var isScroll = false
    private var alphaIndexer= HashMap<String, Int>()   // 存放存在的汉语拼音首字母和与之对应的列表位置
    protected var hotCityList: MutableList<CityEntity> = ArrayList<CityEntity>()
    protected var totalCityList: MutableList<CityEntity> = ArrayList<CityEntity>()
    protected var curCityList: MutableList<CityEntity> = ArrayList<CityEntity>()
    protected var searchCityList: MutableList<CityEntity> = ArrayList<CityEntity>()
    private var cityListAdapter: com.example.charging.cityChoice.activity.city_choice.CityListAdapter? = null  //protect改private，下一行同
    private var searchCityListAdapter: com.example.charging.cityChoice.activity.city_choice.SearchCityListAdapter? = null
    private var locationCity: String? = null
    private var curSelCity: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 默认软键盘不弹出
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setSystemBarTransparent()
        setContentView(R.layout.city_choice_layout)
        //setContentView(R.layout.text_layout)
        initView()    //有问题  ,已解决但是搜索界面会挡住刘海处的空隙
        //initData()     //有问题
        //initListener()    //有问题
    }

    private fun initView() {
        ViewBinder.bind(this)
        println("here is the output")
        println(Build.VERSION.SDK_INT)
        println(Build.VERSION_CODES.KITKAT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {       //此if语句有问题,已改，top为0，手动设置，top应该是手机配置的刘海的长度，但是一直有bug
            ScreenUtils.init(this)  //不知道为什么Java中不需要这句话，但是kotlin需要手动初始化静态类
            var top: Int = ScreenUtils.systemBarHeight
            println("top="+top)
            mToolbar?.setPadding(0, top, 0, 0)
        }
        handler = object:Handler(){}
        overlayThread = com.example.charging.cityChoice.activity.city_choice().OverlayThread() //kotlin内部类的实例化需要外部类也有一个实例
        searchCityListAdapter = com.example.charging.cityChoice.activity.city_choice().SearchCityListAdapter(this, searchCityList)
        searchCityLv!!.adapter = searchCityListAdapter
        locationCity = "杭州"
        curSelCity = locationCity
    }

    private fun initData() {
        initTotalCityList()
        cityListAdapter = com.example.charging.cityChoice.activity.city_choice().CityListAdapter(this, totalCityList, hotCityList)
        totalCityLv!!.adapter = cityListAdapter
        totalCityLv.setOnScrollListener(this)
        totalCityLv.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            if (position > 1) {
                val cityEntity: CityEntity = totalCityList[position]
                showSetCityDialog(cityEntity.name, cityEntity.cityCode!!)
            }
        }
        lettersLv?.onTouchingLetterChangedListener=com.example.charging.cityChoice.activity.city_choice().LetterListViewListener()

        initOverlay()
    }

    private fun initListener() {         //大问题
        searchCityLv!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val cityEntity: CityEntity = searchCityList[position]
            showSetCityDialog(cityEntity.name, cityEntity.cityCode!!)
        }
        searchContentEt!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val content = searchContentEt.text.toString().trim { it <= ' ' }.toLowerCase()
                setSearchCityList(content)
            }
        })
        searchContentEt.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideSoftInput(searchContentEt.windowToken)
                val content = searchContentEt.text.toString().trim { it <= ' ' }.toLowerCase()
                setSearchCityList(content)
                return@OnEditorActionListener true
            }
            false
        })
    }

    /**
     * 设置搜索数据展示
     */
    private fun setSearchCityList(content: String) {
        searchCityList.clear()
        if (TextUtils.isEmpty(content)) {
            totalCityLv!!.visibility = View.VISIBLE
            lettersLv?.setVisibility(View.VISIBLE)
            searchCityLv!!.visibility = View.GONE
            noSearchDataTv!!.visibility = View.GONE
        } else {
            totalCityLv!!.visibility = View.GONE
            lettersLv?.setVisibility(View.GONE)
            for (i in curCityList.indices) {
                val cityEntity: CityEntity = curCityList[i]
                if (cityEntity.name!!.contains(content) || cityEntity.pinyin!!.contains(content)
                        || cityEntity.first!!.contains(content)) {
                    searchCityList.add(cityEntity)
                }
            }
            if (searchCityList.size != 0) {
                noSearchDataTv!!.visibility = View.GONE
                searchCityLv!!.visibility = View.VISIBLE
            } else {
                noSearchDataTv!!.visibility = View.VISIBLE
                searchCityLv!!.visibility = View.GONE
            }
            searchCityListAdapter?.notifyDataSetChanged()
        }
    }

    /**
     * 初始化全部城市列表
     */
    fun initTotalCityList() {
        hotCityList.clear()
        totalCityList.clear()
        curCityList.clear()
        val cityListJson: String = JsonReadUtil.getJsonStr(this, CityFileName)
        val jsonObject: JSONObject
        try {
            jsonObject = JSONObject(cityListJson)
            val array = jsonObject.getJSONArray("City")
            for (i in 0 until array.length()) {
                val `object` = array.getJSONObject(i)
                val name = `object`.getString("name")
                val key = `object`.getString("key")
                val pinyin = `object`.getString("full")
                val first = `object`.getString("first")
                val cityCode = `object`.getString("code")
                val cityEntity = CityEntity()
                cityEntity.name=name
                cityEntity.first=first
                cityEntity.key=key
                cityEntity.pinyin=pinyin
                cityEntity.cityCode=cityCode
                if (key == "热门") {
                    hotCityList.add(cityEntity)
                } else {
                    if (!cityEntity.key.equals("0") && !cityEntity.key.equals("1")) {
                        curCityList.add(cityEntity)
                    }
                    totalCityList.add(cityEntity)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
        isScroll = if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
                || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            true
        } else {
            false
        }
    }

    override fun onScroll(view: AbsListView, firstVisibleItem: Int,
                          visibleItemCount: Int, totalItemCount: Int) {
        if (!isScroll) {
            return
        }
        if (mReady) {
            val key = getAlpha(totalCityList[firstVisibleItem].key!!)
            overlay!!.text = key
            overlay!!.visibility = View.VISIBLE
            handler!!.removeCallbacks(overlayThread)
            // 延迟让overlay为不可见
            handler!!.postDelayed(overlayThread, 700)
        }
    }

    /**
     * 总城市适配器
     */
    private inner class CityListAdapter internal constructor(private val context: Context,
                                                             totalCityList: List<CityEntity>,
                                                             hotCityList: List<CityEntity>) : BaseAdapter() {

        private val totalCityList: List<CityEntity>?
        private val hotCityList: List<CityEntity>
        private val inflater: LayoutInflater
        val VIEW_TYPE = 3
        override fun getViewTypeCount(): Int {
            return VIEW_TYPE
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < 2) position else 2
        }

        override fun getCount(): Int {
            return totalCityList?.size ?: 0
        }

        override fun getItem(position: Int): Any {
            return totalCityList!![position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView
            val curCityNameTv: TextView
            val holder: com.example.charging.cityChoice.activity.city_choice.CityListAdapter.ViewHolder
            val viewType = getItemViewType(position)
            if (viewType == 0) { // 定位
                convertView = inflater.inflate(R.layout.select_city_location_item, null)
                val noLocationLl = convertView.findViewById<LinearLayout>(R.id.cur_city_no_data_ll)
                val getLocationTv = convertView.findViewById<TextView>(R.id.cur_city_re_get_location_tv)
                curCityNameTv = convertView.findViewById(R.id.cur_city_name_tv)
                if (TextUtils.isEmpty(locationCity)) {
                    noLocationLl.visibility = View.VISIBLE
                    curCityNameTv.visibility = View.GONE
                    getLocationTv.setOnClickListener {
                        //获取定位
                    }
                } else {
                    noLocationLl.visibility = View.GONE
                    curCityNameTv.visibility = View.VISIBLE
                    curCityNameTv.text = locationCity
                    curCityNameTv.setOnClickListener {
                        if (locationCity != curSelCity) {
                            //设置城市代码
                            var cityCode = ""
                            for (cityEntity in curCityList) {
                                if (cityEntity.name.equals(locationCity)) {
                                    cityCode = cityEntity.cityCode!!
                                    break
                                }
                            }
                            showSetCityDialog(locationCity, cityCode)
                        } else {
                            ToastUtils.show("当前定位城市" + curCityNameTv.text.toString())
                        }
                    }
                }
            } else if (viewType == 1) { //热门城市
                convertView = inflater.inflate(R.layout.recent_city_item, null)
                val hotCityGv = convertView.findViewById<GridView>(R.id.recent_city_gv)
                hotCityGv.adapter = com.example.charging.cityChoice.activity.city_choice().HotCityListAdapter(context, this.hotCityList)
                hotCityGv.onItemClickListener = OnItemClickListener { parent, view, position, id ->
                    val cityEntity: CityEntity = hotCityList[position]
                    showSetCityDialog(cityEntity.name, cityEntity.cityCode!!)
                }
            } else {
                if (null == convertView) {
                    holder = com.example.charging.cityChoice.activity.city_choice().CityListAdapter(context,totalCityList!!,hotCityList).ViewHolder()
                    convertView = inflater.inflate(R.layout.city_list_item_layout, null)
                    ViewBinder.bind(holder, convertView)
                    convertView.tag = holder
                } else {
                    holder = convertView.tag as com.example.charging.cityChoice.activity.city_choice.CityListAdapter.ViewHolder
                }
                val cityEntity: CityEntity = totalCityList!![position]
                holder.cityKeyTv?.setVisibility(View.VISIBLE)
                holder.cityKeyTv?.setText(getAlpha(cityEntity.key!!))
                holder.cityNameTv?.setText(cityEntity.name)
                if (position >= 1) {
                    val preCity: CityEntity = totalCityList[position - 1]
                    if (preCity.key.equals(cityEntity.key)) {
                        holder.cityKeyTv?.setVisibility(View.GONE)
                    } else {
                        holder.cityKeyTv?.setVisibility(View.VISIBLE)
                    }
                }
            }
            return convertView
        }

        private inner class ViewHolder {
            @Bind(R.id.city_name_tv)
            var cityNameTv: TextView? = null

            @Bind(R.id.city_key_tv)
            var cityKeyTv: TextView? = null
        }

        init {
            this.totalCityList = totalCityList
            this.hotCityList = hotCityList
            inflater = LayoutInflater.from(context)
            alphaIndexer = HashMap()
            for (i in totalCityList.indices) {
                // 当前汉语拼音首字母
                val currentStr: String = totalCityList[i].key!!
                val previewStr = if (i - 1 >= 0) totalCityList[i - 1].key else " "
                if (previewStr != currentStr) {
                    val name = getAlpha(currentStr)
                    alphaIndexer[name] = i
                }
            }
        }
    }

    /**
     * 热门城市适配器
     */
    private inner class HotCityListAdapter internal constructor(mContext: Context?, cityEntities: List<CityEntity>?) : BaseAdapter() {
        private val cityEntities: List<CityEntity>?
        private val inflater: LayoutInflater
        override fun getCount(): Int {
            return cityEntities?.size ?: 0
        }

        override fun getItem(position: Int): Any {
            return cityEntities!![position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView
            val holder: com.example.charging.cityChoice.activity.city_choice.HotCityListAdapter.ViewHolder
            if (null == convertView) {
                holder = com.example.charging.cityChoice.activity.city_choice().HotCityListAdapter(null, cityEntities).ViewHolder()
                convertView = inflater.inflate(R.layout.city_list_grid_item_layout, null)
                ViewBinder.bind(holder, convertView)
                convertView.tag = holder
            } else {
                holder = convertView.tag as com.example.charging.cityChoice.activity.city_choice.HotCityListAdapter.ViewHolder
            }
            val cityEntity: CityEntity = cityEntities!![position]
            holder.cityNameTv?.setText(cityEntity.name)
            return convertView
        }

        private inner class ViewHolder {
            @Bind(R.id.city_list_grid_item_name_tv)
            var cityNameTv: TextView? = null
        }

        init {
            this.cityEntities = cityEntities
            inflater = LayoutInflater.from(mContext)
        }
    }

    /**
     * 搜索城市列表适配器
     */
    private inner class SearchCityListAdapter internal constructor(mContext: Context?, cityEntities: List<CityEntity>?) : BaseAdapter() {
        private val cityEntities: List<CityEntity>?
        private val inflater: LayoutInflater
        override fun getCount(): Int {
            return cityEntities?.size ?: 0
        }

        override fun getItem(position: Int): Any {
            return cityEntities!![position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView
            val holder: com.example.charging.cityChoice.activity.city_choice.SearchCityListAdapter.ViewHolder
            if (null == convertView) {
                holder = com.example.charging.cityChoice.activity.city_choice().SearchCityListAdapter(null,cityEntities).ViewHolder()
                convertView = inflater.inflate(R.layout.city_list_item_layout, null)
                ViewBinder.bind(holder, convertView)
                convertView.tag = holder
            } else {
                holder = convertView.tag as com.example.charging.cityChoice.activity.city_choice.SearchCityListAdapter.ViewHolder
            }
            val cityEntity: CityEntity = cityEntities!![position]
            holder.cityKeyTv?.setVisibility(View.GONE)
            holder.cityNameTv?.setText(cityEntity.name)
            return convertView
        }

        private inner class ViewHolder {
            @Bind(R.id.city_name_tv)
            var cityNameTv: TextView? = null

            @Bind(R.id.city_key_tv)
            var cityKeyTv: TextView? = null
        }

        init {
            this.cityEntities = cityEntities
            inflater = LayoutInflater.from(mContext)
        }
    }

    /**
     * 获得首字母
     */
    private fun getAlpha(key: String): String {
        return if (key == "0") {
            "定位"
        } else if (key == "1") {
            "热门"
        } else {
            key
        }
    }

    /**
     * 初始化汉语拼音首字母弹出提示框
     */
    private fun initOverlay() {
        mReady = true
        val inflater = LayoutInflater.from(this)
        overlay = inflater.inflate(R.layout.overlay, null) as TextView?
        overlay!!.visibility = View.INVISIBLE
        val lp = WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT)
        val windowManager = this
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlay, lp)
    }

    private inner class LetterListViewListener : LetterListView.OnTouchingLetterChangedListener {
        override fun onTouchingLetterChanged(s: String?) {
            isScroll = false
            if (alphaIndexer[s] != null) {
                val position: Int = alphaIndexer.get(s)!!
                totalCityLv!!.setSelection(position)
                overlay!!.text = s
                overlay!!.visibility = View.VISIBLE
                handler!!.removeCallbacks(overlayThread!!)
                // 延迟让overlay为不可见
                handler!!.postDelayed(overlayThread!!, 700)
            }
        }
    }

    /**
     * 设置overlay不可见
     */
    private inner class OverlayThread : Runnable {
        override fun run() {
            overlay!!.visibility = View.GONE
        }
    }

    /**
     * 展示设置城市对话框
     */
    private fun showSetCityDialog(curCity: String?, cityCode: String) {
        if (curCity == curSelCity) {
            ToastUtils.show("当前定位城市$curCity")
            return
        }
        val builder: AlertDialog.Builder = AlertDialog.Builder(this) //先得到构造器
        builder.setTitle("提示") //设置标题
        builder.setMessage("是否设置 $curCity 为您的当前城市？") //设置内容
        builder.setPositiveButton("确定", DialogInterface.OnClickListener { dialog, which ->

            //设置确定按钮
            dialog.dismiss()
            //选中之后做你的方法
        })
        builder.setNegativeButton("取消", DialogInterface.OnClickListener { dialog, which ->
            //设置取消按钮
            dialog.dismiss()
        })
        builder.create().show()
    }

    /**
     * 隐藏软件盘
     */
    fun hideSoftInput(token: IBinder?) {
        if (token != null) {
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(token,
                    InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    /**
     * 设置沉浸式状态栏
     */
    private fun setSystemBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 5.0 LOLLIPOP解决方案
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            getWindow().setStatusBarColor(Color.TRANSPARENT)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 4.4 KITKAT解决方案
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    companion object {
        //文件名称
        private const val CityFileName = "allcity.json"
    }
}