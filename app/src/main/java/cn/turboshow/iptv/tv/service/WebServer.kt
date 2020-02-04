package cn.turboshow.iptv.tv.service

import android.content.Context
import cn.turboshow.iptv.tv.data.PlaylistRepository
import cn.turboshow.iptv.tv.data.SettingsRepository
import cn.turboshow.iptv.tv.model.Channel
import cn.turboshow.iptv.tv.model.UdpxySettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import fi.iki.elonen.NanoHTTPD
import net.lingala.zip4j.ZipFile
import okio.BufferedSource
import okio.Okio
import okio.Source
import org.apache.commons.io.IOUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServer @Inject constructor(
    private val context: Context,
    private val moshi: Moshi,
    private val settingsRepository: SettingsRepository,
    private val playlistRepository: PlaylistRepository
) : NanoHTTPD("0.0.0.0", 1213) {
    private val wwwZipFileName = "www.zip"
    private val wwwRoot = "www"

    init {
        extractWWWFiles()
    }

    // TODO: optimization (caching and async)
    private fun extractWWWFiles() {
        val zipFile = File(context.filesDir, wwwZipFileName)
        val wwwDir = File(context.filesDir, wwwRoot)
        zipFile.delete()
        wwwDir.deleteRecursively()
        IOUtils.copy(context.assets.open(wwwZipFileName), zipFile.outputStream())
        ZipFile(zipFile).extractAll(context.filesDir.absolutePath)
    }

    private fun get(session: IHTTPSession, pathPattern: String): Boolean {
        return session.method == Method.GET && session.uri == pathPattern
    }

    private fun post(session: IHTTPSession, pathPattern: String): Boolean {
        return session.method == Method.POST && session.uri == pathPattern
    }

    private fun getPlaylist(session: IHTTPSession): Response {
        return newFixedLengthResponse("playlist")
    }

    private fun updatePlaylist(session: IHTTPSession): Response {
        val channelListType = Types.newParameterizedType(List::class.java, Channel::class.java)
        val adapter = moshi.adapter<List<Channel>>(channelListType)

        val files = mutableMapOf<String, String>()
        session.parseBody(files)
        val playlist = if (files.containsKey("playlist")) {
            val tmp = File(files["playlist"])
            var fileSource: Source = Okio.source(File(files["playlist"]).inputStream());
            val filename: String? = session.parameters.get("playlist")?.get(0)
            if (filename != null && (filename.endsWith("txt")||filename.endsWith("csv"))) {//支持txt 格式和csv格式
                var clist: MutableList<Channel> = ArrayList(32)
                tmp.forEachLine {
                    println(it)
                    var strs = it.split(",")
                    clist.add(Channel(strs[0], strs[1]))
                }
                clist!!

            } else {
                adapter.fromJson(Okio.buffer(fileSource))
            }
        } else {
            adapter.fromJson(files["postData"]!!)
        }
        playlistRepository.updateChannels(playlist!!)

        return newFixedLengthResponse("accepted")
    }

    private fun getUdpxySettings(session: IHTTPSession): Response {
        val adapter = moshi.adapter(UdpxySettings::class.java)
        return newFixedLengthResponse(adapter.toJson(UdpxySettings(settingsRepository.udpxyAddr.value)))
    }

    private fun updateUdpxySettings(session: IHTTPSession): Response {
        val adapter = moshi.adapter(UdpxySettings::class.java)
        val settings = adapter.fromJson(Okio.buffer(Okio.source(session.inputStream)))
        settingsRepository.updateUdpxyAddr(settings!!.addr)

        return newFixedLengthResponse("accepted")
    }

    private fun serveFiles(session: IHTTPSession): Response {
        var targetFile = File(context.filesDir, "$wwwRoot${session.uri}".dropLastWhile { it == '/' })
        if (targetFile.isDirectory) {
            targetFile = File(targetFile, "index.html")
        }

        if (!targetFile.exists()) {
            targetFile = File(context.filesDir, "$wwwRoot/index.html")
        }

        return newFixedLengthResponse(
            Response.Status.OK,
            null,
            targetFile.inputStream(),
            targetFile.length()
        )
    }

    private fun handleRequests(session: IHTTPSession): Response {
        return when {
            get(session, "/api/settings/playlist") -> {
                getPlaylist(session)
            }
            post(session, "/api/settings/playlist") -> {
                updatePlaylist(session)
            }
            get(session, "/api/settings/udpxy") -> {
                getUdpxySettings(session)
            }
            post(session, "/api/settings/udpxy") -> {
                updateUdpxySettings(session)
            }
            else -> {
                serveFiles(session)
            }
        }
    }

    override fun serve(session: IHTTPSession): Response {
        return handleRequests(session)
    }
}