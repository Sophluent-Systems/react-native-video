package com.brentvatne.common.api

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.text.TextUtils
import com.brentvatne.common.api.DRMProps.Companion.parse
import com.brentvatne.common.toolbox.DebugLog
import com.brentvatne.common.toolbox.DebugLog.e
import com.brentvatne.common.toolbox.ReactBridgeUtils.safeGetArray
import com.brentvatne.common.toolbox.ReactBridgeUtils.safeGetBool
import com.brentvatne.common.toolbox.ReactBridgeUtils.safeGetInt
import com.brentvatne.common.toolbox.ReactBridgeUtils.safeGetMap
import com.brentvatne.common.toolbox.ReactBridgeUtils.safeGetString
import com.brentvatne.react.BuildConfig
import com.facebook.react.bridge.ReadableMap
import java.util.Locale
import java.util.Objects

/**
 * Class representing Source props for host.
 * Only generic code here, no reference to the player.
 */
class Source {
    /** String value of source to playback */
    private var uriString: String? = null

    /** Parsed value of source to playback */
    var uri: Uri? = null

    /** True if source is a local JS asset */
    var isLocalAssetFile: Boolean = false

    /** True if source is a local file asset://, ... */
    var isAsset: Boolean = false

    /** Start position of playback used to resume playback */
    var startPositionMs: Int = -1

    /** Will crop content start at specified position */
    var cropStartMs: Int = -1

    /** Will crop content end at specified position */
    var cropEndMs: Int = -1

    /** Will virtually consider that content before contentStartTime is a preroll ad */
    var contentStartTime: Int = -1

    /** Allow to force stream content, necessary when uri doesn't contain content type (.mlp4, .m3u, ...) */
    var extension: String? = null

    /** Metadata to display in notification */
    var metadata: Metadata? = null

    /** Allowed reload before failure notification */
    var minLoadRetryCount = 3

    /** http header list */
    val headers: MutableMap<String, String> = HashMap()

    /**
     * DRM properties linked to the source
     */
    var drmProps: DRMProps? = null

    /** enable chunkless preparation for HLS
     * see:
     */
    var textTracksAllowChunklessPreparation: Boolean = false

    /**
     * CMCD properties linked to the source
     */
    var cmcdProps: CMCDProps? = null

    /**
     * Ads playback properties
     */
    var adsProps: AdsProps? = null

    /*
     * buffering configuration
     */
    var bufferConfig = BufferConfig()

    /**
     * The list of sideLoaded text tracks
     */
    var sideLoadedTextTracks: SideLoadedTextTrackList? = null

    override fun hashCode(): Int = Objects.hash(uriString, uri, startPositionMs, cropStartMs, cropEndMs, extension, metadata, headers)

    /** return true if this and src are equals  */
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Source) return false
        return (
            uri == other.uri &&
                cropStartMs == other.cropStartMs &&
                cropEndMs == other.cropEndMs &&
                startPositionMs == other.startPositionMs &&
                extension == other.extension &&
                drmProps == other.drmProps &&
                contentStartTime == other.contentStartTime &&
                cmcdProps == other.cmcdProps &&
                sideLoadedTextTracks == other.sideLoadedTextTracks &&
                adsProps == other.adsProps &&
                minLoadRetryCount == other.minLoadRetryCount &&
                isLocalAssetFile == other.isLocalAssetFile &&
                isAsset == other.isAsset &&
                bufferConfig == other.bufferConfig
            )
    }

    /** return true if this and src are equals  */
    fun isEquals(source: Source): Boolean = this == source

    /** Metadata to display in notification */
    class Metadata {
        /** Metadata title */
        var title: String? = null

        /** Metadata subtitle */
        var subtitle: String? = null

        /** Metadata description */
        var description: String? = null

        /** Metadata artist */
        var artist: String? = null

        /** image uri to display */
        var imageUri: Uri? = null

        companion object {
            private const val PROP_SRC_METADATA_TITLE = "title"
            private const val PROP_SRC_METADATA_SUBTITLE = "subtitle"
            private const val PROP_SRC_METADATA_DESCRIPTION = "description"
            private const val PROP_SRC_METADATA_ARTIST = "artist"
            private const val PROP_SRC_METADATA_IMAGE_URI = "imageUri"

            /** parse metadata object */
            @JvmStatic
            fun parse(src: ReadableMap?): Metadata? {
                if (src != null) {
                    val metadata = Metadata()
                    metadata.title = safeGetString(src, PROP_SRC_METADATA_TITLE)
                    metadata.subtitle = safeGetString(src, PROP_SRC_METADATA_SUBTITLE)
                    metadata.description = safeGetString(src, PROP_SRC_METADATA_DESCRIPTION)
                    metadata.artist = safeGetString(src, PROP_SRC_METADATA_ARTIST)
                    val imageUriString = safeGetString(src, PROP_SRC_METADATA_IMAGE_URI)
                    try {
                        metadata.imageUri = Uri.parse(imageUriString)
                    } catch (e: Exception) {
                        e(TAG, "Could not parse imageUri in metadata")
                    }
                    return metadata
                }
                return null
            }
        }
    }

    companion object {
        private const val TAG = "Source"
        private const val PROP_SRC_URI = "uri"
        private const val PROP_SRC_IS_LOCAL_ASSET_FILE = "isLocalAssetFile"
        private const val PROP_SRC_IS_ASSET = "isAsset"
        private const val PROP_SRC_START_POSITION = "startPosition"
        private const val PROP_SRC_CROP_START = "cropStart"
        private const val PROP_SRC_CROP_END = "cropEnd"
        private const val PROP_SRC_CONTENT_START_TIME = "contentStartTime"
        private const val PROP_SRC_TYPE = "type"
        private const val PROP_SRC_METADATA = "metadata"
        private const val PROP_SRC_HEADERS = "requestHeaders"
        private const val PROP_SRC_DRM = "drm"
        private const val PROP_SRC_CMCD = "cmcd"
        private const val PROP_SRC_ADS = "ad"
        private const val PROP_SRC_TEXT_TRACKS_ALLOW_CHUNKLESS_PREPARATION = "textTracksAllowChunklessPreparation"
        private const val PROP_SRC_TEXT_TRACKS = "textTracks"
        private const val PROP_SRC_MIN_LOAD_RETRY_COUNT = "minLoadRetryCount"
        private const val PROP_SRC_BUFFER_CONFIG = "bufferConfig"

        @SuppressLint("DiscouragedApi")
        private fun getUriFromAssetId(context: Context, uriString: String): Uri? {
            val resources: Resources = context.resources
            val packageName: String = context.packageName
            var identifier = resources.getIdentifier(
                uriString,
                "drawable",
                packageName
            )
            if (identifier == 0) {
                identifier = resources.getIdentifier(
                    uriString,
                    "raw",
                    packageName
                )
            }

            if (identifier <= 0) {
                // cannot find identifier of content
                DebugLog.d(TAG, "cannot find identifier")
                return null
            }
            return Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).path(identifier.toString()).build()
        }

        /** parse the source ReadableMap received from app */
        @JvmStatic
        fun parse(src: ReadableMap?, context: Context): Source {
            val source = Source()

            if (src != null) {
                val uriString = safeGetString(src, PROP_SRC_URI, null)
                if (uriString == null || TextUtils.isEmpty(uriString)) {
                    DebugLog.d(TAG, "isEmpty uri:$uriString")
                    return source
                }
                var uri = Uri.parse(uriString)
                if (uri == null) {
                    // return an empty source
                    DebugLog.d(TAG, "Invalid uri:$uriString")
                    return source
                } else if (!isValidScheme(uri.scheme)) {
                    uri = getUriFromAssetId(context, uriString)
                    if (uri == null) {
                        // cannot find identifier of content
                        DebugLog.d(TAG, "cannot find identifier")
                        return source
                    }
                }
                source.uriString = uriString
                source.uri = uri
                source.isLocalAssetFile = safeGetBool(src, PROP_SRC_IS_LOCAL_ASSET_FILE, false)
                source.isAsset = safeGetBool(src, PROP_SRC_IS_ASSET, false)
                source.startPositionMs = safeGetInt(src, PROP_SRC_START_POSITION, -1)
                source.cropStartMs = safeGetInt(src, PROP_SRC_CROP_START, -1)
                source.cropEndMs = safeGetInt(src, PROP_SRC_CROP_END, -1)
                source.contentStartTime = safeGetInt(src, PROP_SRC_CONTENT_START_TIME, -1)
                source.extension = safeGetString(src, PROP_SRC_TYPE, null)
                source.drmProps = parse(safeGetMap(src, PROP_SRC_DRM))
                source.cmcdProps = CMCDProps.parse(safeGetMap(src, PROP_SRC_CMCD))
                if (BuildConfig.USE_EXOPLAYER_IMA) {
                    source.adsProps = AdsProps.parse(safeGetMap(src, PROP_SRC_ADS))
                }
                source.textTracksAllowChunklessPreparation = safeGetBool(src, PROP_SRC_TEXT_TRACKS_ALLOW_CHUNKLESS_PREPARATION, true)
                source.sideLoadedTextTracks = SideLoadedTextTrackList.parse(safeGetArray(src, PROP_SRC_TEXT_TRACKS))
                source.minLoadRetryCount = safeGetInt(src, PROP_SRC_MIN_LOAD_RETRY_COUNT, 3)
                source.bufferConfig = BufferConfig.parse(safeGetMap(src, PROP_SRC_BUFFER_CONFIG))

                val propSrcHeadersArray = safeGetArray(src, PROP_SRC_HEADERS)
                if (propSrcHeadersArray != null) {
                    if (propSrcHeadersArray.size() > 0) {
                        for (i in 0 until propSrcHeadersArray.size()) {
                            val current = propSrcHeadersArray.getMap(i)
                            val key = current?.getString("key")
                            val value = current?.getString("value")
                            if (key != null && value != null) {
                                source.headers[key] = value
                            }
                        }
                    }
                }
                source.metadata = Metadata.parse(safeGetMap(src, PROP_SRC_METADATA))
            }
            return source
        }

        /** return true if rui scheme is supported for android playback */
        private fun isValidScheme(scheme: String?): Boolean {
            if (scheme == null) {
                return false
            }
            val lowerCaseUri = scheme.lowercase(Locale.getDefault())
            return (
                lowerCaseUri == "http" ||
                    lowerCaseUri == "https" ||
                    lowerCaseUri == "content" ||
                    lowerCaseUri == "file" ||
                    lowerCaseUri == "rtsp" ||
                    lowerCaseUri == "asset"
                )
        }
    }
}
