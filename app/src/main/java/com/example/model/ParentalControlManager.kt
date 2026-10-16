package com.example.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LockedChannelInfo(
    val url: String,
    val title: String,
    val group: String,
    val type: String = "LIVE"
)

object ParentalControlManager {
    const val DEFAULT_PIN = "0000"
    private const val PREFS_NAME = "parental_control_prefs"
    private const val KEY_PIN = "pin_code"
    private const val KEY_SECRET_QUESTION = "secret_question"
    private const val KEY_SECRET_ANSWER = "secret_answer"
    private const val KEY_LOCKED_CHANNELS = "locked_channels"
    private const val KEY_LOCKED_GROUPS = "locked_groups"
    private const val KEY_EXPLICITLY_UNLOCKED_GROUPS = "explicitly_unlocked_groups"
    private const val KEY_EXPLICITLY_UNLOCKED_CHANNELS = "explicitly_unlocked_channels"
    private const val KEY_LOCKED_CHANNEL_METAS = "locked_channel_metas"

    private lateinit var prefs: SharedPreferences

    private val _pinCode = MutableStateFlow<String?>(null)
    val pinCode: StateFlow<String?> = _pinCode.asStateFlow()

    private val _secretQuestion = MutableStateFlow<String?>(null)
    val secretQuestion: StateFlow<String?> = _secretQuestion.asStateFlow()

    private val _secretAnswer = MutableStateFlow<String?>(null)
    val secretAnswer: StateFlow<String?> = _secretAnswer.asStateFlow()

    private val _lockedChannels = MutableStateFlow<Set<String>>(emptySet())
    val lockedChannels: StateFlow<Set<String>> = _lockedChannels.asStateFlow()
    
    private val _lockedGroups = MutableStateFlow<Set<String>>(emptySet())
    val lockedGroups: StateFlow<Set<String>> = _lockedGroups.asStateFlow()

    private val _explicitlyUnlockedGroups = MutableStateFlow<Set<String>>(emptySet())
    val explicitlyUnlockedGroups: StateFlow<Set<String>> = _explicitlyUnlockedGroups.asStateFlow()

    private val _explicitlyUnlockedChannels = MutableStateFlow<Set<String>>(emptySet())
    val explicitlyUnlockedChannels: StateFlow<Set<String>> = _explicitlyUnlockedChannels.asStateFlow()

    private val _sessionUnlockedGroups = MutableStateFlow<Set<String>>(emptySet())
    val sessionUnlockedGroups: StateFlow<Set<String>> = _sessionUnlockedGroups.asStateFlow()

    private val ADULT_KEYWORDS = listOf(
        "adult", "adults", "adulte", "adultes",
        "yetiskin", "yetişkin",
        "porn", "porno", "pornos",
        "erotic", "erotica", "erotik", "erotizm",
        "redlight", "playboy", "penthouse", "hustler", "brazzers",
        "dorcel", "vivid", "babes", "hentai", "nsfw",
        "hardcore", "softcore", "cinerotica", "passie", "man-x",
        "daring", "blue hustler", "evil angel", "private tv",
        "red hot", "bangbros", "naughty", "venus", "jasmin", "cam4"
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _pinCode.value = prefs.getString(KEY_PIN, null)
        _secretQuestion.value = prefs.getString(KEY_SECRET_QUESTION, null)
        _secretAnswer.value = prefs.getString(KEY_SECRET_ANSWER, null)
        _lockedChannels.value = prefs.getStringSet(KEY_LOCKED_CHANNELS, emptySet()) ?: emptySet()
        _lockedGroups.value = prefs.getStringSet(KEY_LOCKED_GROUPS, emptySet()) ?: emptySet()
        _explicitlyUnlockedGroups.value = prefs.getStringSet(KEY_EXPLICITLY_UNLOCKED_GROUPS, emptySet()) ?: emptySet()
        _explicitlyUnlockedChannels.value = prefs.getStringSet(KEY_EXPLICITLY_UNLOCKED_CHANNELS, emptySet()) ?: emptySet()
    }

    fun setupPin(pin: String, question: String, answer: String) {
        _pinCode.value = pin
        _secretQuestion.value = question
        _secretAnswer.value = answer
        prefs.edit()
            .putString(KEY_PIN, pin)
            .putString(KEY_SECRET_QUESTION, question)
            .putString(KEY_SECRET_ANSWER, answer)
            .apply()
    }

    fun resetPin(newPin: String) {
        _pinCode.value = newPin
        prefs.edit().putString(KEY_PIN, newPin).apply()
    }
    
    fun removePin() {
        _pinCode.value = null
        _secretQuestion.value = null
        _secretAnswer.value = null
        _lockedChannels.value = emptySet()
        _lockedGroups.value = emptySet()
        _explicitlyUnlockedGroups.value = emptySet()
        _explicitlyUnlockedChannels.value = emptySet()
        _sessionUnlockedGroups.value = emptySet()
        prefs.edit().clear().apply()
    }

    fun isAdultCategory(groupName: String?): Boolean {
        if (groupName.isNullOrBlank()) return false
        val clean = groupName.lowercase().trim()

        if (clean.contains("xxx") || clean.contains("+18") || clean.contains("18+") || clean.contains("18 +") || clean.contains("+ 18")) {
            return true
        }

        val tokens = clean.split(Regex("[\\s\\-_/|:\\[\\]().,*#+]+")).filter { it.isNotBlank() }
        for (token in tokens) {
            if (token == "sex" || token == "xxx" || token == "18" || token == "porn" || token == "porno") return true
            if (token.contains("xxx") || token.contains("18+")) return true
        }

        for (kw in ADULT_KEYWORDS) {
            if (clean.contains(kw)) return true
        }
        return false
    }

    fun isAdultChannel(title: String?, group: String? = null, url: String? = null): Boolean {
        if (isAdultCategory(group)) return true
        if (title.isNullOrBlank()) return false
        val clean = title.lowercase().trim()

        if (clean.contains("xxx") || clean.contains("+18") || clean.contains("18+") || clean.contains("18 +") || clean.contains("+ 18")) {
            return true
        }

        val tokens = clean.split(Regex("[\\s\\-_/|:\\[\\]().,*#+]+")).filter { it.isNotBlank() }
        for (token in tokens) {
            if (token == "sex" || token == "xxx" || token == "18" || token == "porn" || token == "porno") return true
            if (token.contains("xxx") || token.contains("18+")) return true
        }

        for (kw in ADULT_KEYWORDS) {
            if (clean.contains(kw)) return true
        }
        return false
    }

    fun unlockGroupForSession(groupName: String) {
        _sessionUnlockedGroups.value = _sessionUnlockedGroups.value + groupName
    }

    fun unlockChannelForSession(channelUrl: String) {
        // Kanal oynatılacak; ancak kalıcı kilit kaldırılmaz
    }

    private fun getStoredMetas(): MutableMap<String, String> {
        val raw = prefs.getString(KEY_LOCKED_CHANNEL_METAS, null) ?: return mutableMapOf()
        val result = mutableMapOf<String, String>()
        raw.split("###").forEach { entry ->
            val idx = entry.indexOf("===")
            if (idx != -1) {
                val key = entry.substring(0, idx)
                val value = entry.substring(idx + 3)
                result[key] = value
            }
        }
        return result
    }

    private fun saveStoredMetas(map: Map<String, String>) {
        val raw = map.entries.joinToString("###") { "${it.key}===${it.value}" }
        prefs.edit().putString(KEY_LOCKED_CHANNEL_METAS, raw).apply()
    }

    fun lockChannel(channelUrl: String, title: String? = null, group: String? = null, type: String = "LIVE") {
        if (isAdultChannel(title, group, channelUrl)) {
            val currentUnlocked = _explicitlyUnlockedChannels.value.toMutableSet()
            currentUnlocked.remove(channelUrl)
            _explicitlyUnlockedChannels.value = currentUnlocked
            prefs.edit().putStringSet(KEY_EXPLICITLY_UNLOCKED_CHANNELS, currentUnlocked).apply()
        } else {
            val current = _lockedChannels.value.toMutableSet()
            current.add(channelUrl)
            _lockedChannels.value = current
            prefs.edit().putStringSet(KEY_LOCKED_CHANNELS, current).apply()

            val metas = getStoredMetas()
            val metaTitle = title?.ifBlank { "Kilitli Kanal" } ?: "Kilitli Kanal"
            val metaGroup = group?.ifBlank { "Genel" } ?: "Genel"
            metas[channelUrl] = "$metaTitle||$metaGroup||$type"
            saveStoredMetas(metas)
        }
    }

    fun unlockChannel(channelUrl: String) {
        val current = _lockedChannels.value.toMutableSet()
        current.remove(channelUrl)
        _lockedChannels.value = current
        prefs.edit().putStringSet(KEY_LOCKED_CHANNELS, current).apply()

        val metas = getStoredMetas()
        metas.remove(channelUrl)
        saveStoredMetas(metas)

        val unlocked = _explicitlyUnlockedChannels.value.toMutableSet()
        unlocked.add(channelUrl)
        _explicitlyUnlockedChannels.value = unlocked
        prefs.edit().putStringSet(KEY_EXPLICITLY_UNLOCKED_CHANNELS, unlocked).apply()
    }

    fun unlockGroup(groupName: String) {
        val current = _lockedGroups.value.toMutableSet()
        current.remove(groupName)
        _lockedGroups.value = current
        prefs.edit().putStringSet(KEY_LOCKED_GROUPS, current).apply()

        val unlocked = _explicitlyUnlockedGroups.value.toMutableSet()
        unlocked.add(groupName)
        _explicitlyUnlockedGroups.value = unlocked
        prefs.edit().putStringSet(KEY_EXPLICITLY_UNLOCKED_GROUPS, unlocked).apply()
    }

    fun clearAllLocks() {
        _lockedChannels.value = emptySet()
        _lockedGroups.value = emptySet()
        _sessionUnlockedGroups.value = emptySet()
        prefs.edit()
            .putStringSet(KEY_LOCKED_CHANNELS, emptySet())
            .putStringSet(KEY_LOCKED_GROUPS, emptySet())
            .putString(KEY_LOCKED_CHANNEL_METAS, "")
            .apply()
    }

    fun getLockedChannelInfoList(): List<LockedChannelInfo> {
        val metas = getStoredMetas()
        return _lockedChannels.value.map { url ->
            val metaStr = metas[url]
            if (!metaStr.isNullOrBlank()) {
                val parts = metaStr.split("||")
                LockedChannelInfo(
                    url = url,
                    title = parts.getOrNull(0) ?: "Kanal",
                    group = parts.getOrNull(1) ?: "Genel",
                    type = parts.getOrNull(2) ?: "LIVE"
                )
            } else {
                LockedChannelInfo(
                    url = url,
                    title = "Kilitli Kanal",
                    group = "Genel",
                    type = "LIVE"
                )
            }
        }
    }

    fun getLockedGroupsList(): List<String> {
        return _lockedGroups.value.toList().sorted()
    }

    fun toggleChannelLock(channelUrl: String, title: String? = null, group: String? = null) {
        if (_lockedChannels.value.contains(channelUrl) || isAdultChannel(title, group, channelUrl)) {
            unlockChannel(channelUrl)
        } else {
            lockChannel(channelUrl, title, group)
        }
    }

    fun isChannelLocked(channelUrl: String, title: String? = null, group: String? = null): Boolean {
        if (_explicitlyUnlockedChannels.value.contains(channelUrl)) return false
        if (!group.isNullOrBlank() && isGroupLocked(group)) return true
        if (isAdultChannel(title, group, channelUrl)) return true
        if (_pinCode.value == null) return false
        return _lockedChannels.value.contains(channelUrl)
    }
    
    fun isItemLocked(item: com.example.parser.M3uItem): Boolean {
        if (_explicitlyUnlockedChannels.value.contains(item.url)) return false
        if (!item.group.isNullOrBlank() && isGroupLocked(item.group)) return true
        if (isAdultChannel(item.title, item.group, item.url)) return true
        if (_pinCode.value == null) return false
        return _lockedChannels.value.contains(item.url)
    }

    fun toggleGroupLock(groupName: String) {
        if (_lockedGroups.value.contains(groupName) || isAdultCategory(groupName)) {
            unlockGroup(groupName)
        } else {
            val current = _lockedGroups.value.toMutableSet()
            current.add(groupName)
            _lockedGroups.value = current
            prefs.edit().putStringSet(KEY_LOCKED_GROUPS, current).apply()
        }
    }

    fun isGroupLocked(groupName: String): Boolean {
        if (_explicitlyUnlockedGroups.value.contains(groupName)) return false
        if (isAdultCategory(groupName)) return true
        if (_pinCode.value == null) return false
        return _lockedGroups.value.contains(groupName)
    }

    /**
     * Kilitli kanalların isimlerini yıldızlarla (*) gizler.
     * Örneğin: "Kanal D" -> "K**** *", "Karakol" -> "K******", "Adult Zone" -> "A**** Z***"
     */
    fun maskTitle(title: String): String {
        if (title.isBlank()) return "***"
        return title.split(" ").joinToString(" ") { word ->
            if (word.isEmpty()) ""
            else {
                val firstIdx = word.indexOfFirst { it.isLetterOrDigit() }
                if (firstIdx == -1) {
                    "*".repeat(word.length)
                } else {
                    val sb = StringBuilder()
                    var seenFirst = false
                    for (i in word.indices) {
                        val c = word[i]
                        if (c.isLetterOrDigit()) {
                            if (!seenFirst && word.length > 1) {
                                sb.append(c)
                                seenFirst = true
                            } else {
                                sb.append('*')
                            }
                        } else {
                            sb.append(c)
                        }
                    }
                    sb.toString()
                }
            }
        }
    }
}
