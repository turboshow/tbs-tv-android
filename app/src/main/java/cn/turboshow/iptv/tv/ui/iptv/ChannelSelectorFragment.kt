package cn.turboshow.iptv.tv.ui.iptv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import cn.turboshow.iptv.tv.AppViewModel
import cn.turboshow.iptv.tv.R
import cn.turboshow.iptv.tv.di.activityViewModelProvider
import dagger.android.support.DaggerDialogFragment
import kotlinx.android.synthetic.main.fragment_channels.*
import javax.inject.Inject

class ChannelSelectorFragment : DaggerDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activityViewModelProvider(viewModelFactory)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_channels, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initChannelsView()
    }

    private fun initChannelsView() {
        viewModel.channels.observe(this, androidx.lifecycle.Observer {
            channelsView.adapter = ArrayAdapter(context!!, R.layout.channel_text, it)
            channelsView.setOnItemClickListener { _, _, position, _ ->
                viewModel.selectChannel(position)
                dismiss()
            }
            channelsView.post { channelsView.setSelection(it.indexOf(viewModel.currentChannel.value)) }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = ChannelSelectorFragment()
    }
}
