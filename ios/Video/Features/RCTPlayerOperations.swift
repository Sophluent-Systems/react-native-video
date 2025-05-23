import AVFoundation
import MediaAccessibility

let RCTVideoUnset = -1

// MARK: - RCTPlayerOperations

/*!
 * Collection of mutating functions
 */
enum RCTPlayerOperations {
    static func setSideloadedText(player: AVPlayer?, textTracks: [TextTrack], criteria: SelectedTrackCriteria?) {
        let type = criteria?.type

        let trackCount: Int! = player?.currentItem?.tracks.count ?? 0

        // The first few tracks will be audio & video track
        var firstTextIndex = -1
        for i in 0 ..< trackCount where player?.currentItem?.tracks[i].assetTrack?.hasMediaCharacteristic(.legible) ?? false {
            firstTextIndex = i
            break
        }
        if firstTextIndex == -1 {
            // no sideLoaded text track available (can happen with invalid vtt url)
            return
        }

        var selectedTrackIndex: Int = RCTVideoUnset

        if type == "disabled" {
            // Select the last text index which is the disabled text track
            selectedTrackIndex = trackCount - firstTextIndex
        } else if type == "language" {
            let selectedValue = criteria?.value as? String
            for i in 0 ..< textTracks.count {
                let currentTextTrack = textTracks[i]
                if selectedValue == currentTextTrack.language {
                    selectedTrackIndex = i
                    break
                }
            }
        } else if type == "title" {
            let selectedValue = criteria?.value as? String
            for i in 0 ..< textTracks.count {
                let currentTextTrack = textTracks[i]
                if selectedValue == currentTextTrack.title {
                    selectedTrackIndex = i
                    break
                }
            }
        } else if type == "index" {
            if let value = criteria?.value { // check value is provided
                if let indexValue = Int(value as String) { // ensure value is an integer an String to Snt
                    if textTracks.count > indexValue { // ensure value is in group range
                        selectedTrackIndex = indexValue
                    }
                }
            }
        }

        // in the situation that a selected text track is not available (eg. specifies a textTrack not available)
        if (type != "disabled") && selectedTrackIndex == RCTVideoUnset {
            let captioningMediaCharacteristics = MACaptionAppearanceCopyPreferredCaptioningMediaCharacteristics(.user)
            let captionSettings = captioningMediaCharacteristics as? [AnyHashable]
            if (captionSettings?.contains(AVMediaCharacteristic.transcribesSpokenDialogForAccessibility)) != nil {
                selectedTrackIndex = 0 // If we can't find a match, use the first available track
                let systemLanguage = NSLocale.preferredLanguages.first
                for i in 0 ..< textTracks.count {
                    let currentTextTrack = textTracks[i]
                    if systemLanguage == currentTextTrack.language {
                        selectedTrackIndex = i
                        break
                    }
                }
            }
        }

        for i in firstTextIndex ..< trackCount {
            var isEnabled = false
            if selectedTrackIndex != RCTVideoUnset {
                isEnabled = i == selectedTrackIndex + firstTextIndex
            }
            player?.currentItem?.tracks[i].isEnabled = isEnabled
        }
    }

    static func setMediaSelectionTrackForCharacteristic(player: AVPlayer?, characteristic: AVMediaCharacteristic, criteria: SelectedTrackCriteria?) async {
        let type = criteria?.type
        var mediaOption: AVMediaSelectionOption!

        guard let group = await RCTVideoAssetsUtils.getMediaSelectionGroup(asset: player?.currentItem?.asset, for: characteristic) else {
            return
        }

        if type == "disabled" {
            // Do nothing. We want to ensure option is nil
        } else if (type == "language") || (type == "title") {
            let value = criteria?.value as? String
            for i in 0 ..< group.options.count {
                let currentOption: AVMediaSelectionOption! = group.options[i]
                var optionValue: String!
                if type == "language" {
                    optionValue = currentOption.extendedLanguageTag
                } else {
                    optionValue = currentOption.commonMetadata.map(\.value)[0] as? String
                }
                if value == optionValue {
                    mediaOption = currentOption
                    break
                }
            }
            // } else if ([type isEqualToString:@"default"]) {
            //  option = group.defaultOption; */
        } else if type == "index" {
            if let value = criteria?.value { // check value is provided
                if let indexValue = Int(value as String) { // ensure value is an integer an String to Snt
                    if group.options.count > indexValue { // ensure value is in group range
                        mediaOption = group.options[indexValue]
                    }
                }
            }
        } else { // default. invalid type or "system"
            await player?.currentItem?.selectMediaOptionAutomatically(in: group)
            return
        }

        // If a match isn't found, option will be nil and text tracks will be disabled
        await player?.currentItem?.select(mediaOption, in: group)
    }

    static func seek(
        player: AVPlayer, playerItem: AVPlayerItem, paused: Bool, seekTime: Float,
        seekTolerance: Float, completion: @escaping (Bool) -> Void
    ) {
        let timeScale = 1000
        let cmSeekTime: CMTime = CMTimeMakeWithSeconds(
            Float64(seekTime), preferredTimescale: Int32(timeScale)
        )
        let current: CMTime = playerItem.currentTime()
        let tolerance: CMTime = CMTimeMake(value: Int64(seekTolerance), timescale: Int32(timeScale))

        guard CMTimeCompare(current, cmSeekTime) != 0 else {
            // skip if there is no diff in current time and seek time
            return
        }

        if !paused { player.pause() }

        player.seek(
            to: cmSeekTime, toleranceBefore: tolerance, toleranceAfter: tolerance,
            completionHandler: { (finished: Bool) in
                completion(finished)
            }
        )
    }
}
