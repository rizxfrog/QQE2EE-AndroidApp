package me.fuckqq.e2ee.util

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.fuckqq.e2ee.QQE2EEApp
import me.fuckqq.e2ee.SettingKeys
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

object SessionKeyManager {
    private val dataStoreManager by lazy { QQE2EEApp.instance.dataStoreManager }
    private val appScope by lazy { QQE2EEApp.instance.applicationScope }
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storeLock = Any()
    private var cachedStore: MutableMap<String, PeerSession> = mutableMapOf()
    @Volatile
    private var loaded = false
    @Volatile
    private var loading = false

    fun initialize() {
        ensureLoaded()
    }

    fun buildPeerDescriptor(
        packageName: String,
        uniqueId: String?,
        displayName: String?
    ): PeerDescriptor? {
        val normalizedUniqueId = uniqueId?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedDisplayName = displayName?.trim()?.takeIf { it.isNotEmpty() }
        val rawId = normalizedUniqueId ?: normalizedDisplayName ?: return null
        return PeerDescriptor(
            rawId = rawId,
            hash = sha256Hex("$packageName:$rawId"),
            displayName = normalizedDisplayName ?: rawId,
            uniqueId = normalizedUniqueId
        )
    }

    fun getActiveSharedKey(peer: PeerDescriptor?): String? {
        if (peer == null) return null
        val session = getSession(peer.hash) ?: return null
        return if (session.mode == SecretChatMode.SECRET_ESTABLISHED && session.sharedKey != null) {
            session.sharedKey
        } else {
            null
        }
    }

    fun initiateSecretChat(peer: PeerDescriptor): HandshakeAction {
        ensureLoaded()
        val existing = synchronized(storeLock) { cachedStore[peer.hash] }
        if (existing?.mode == SecretChatMode.SECRET_ESTABLISHED && existing.sharedKey != null) {
            return HandshakeAction(
                type = HandshakeActionType.ALREADY_ESTABLISHED,
                payload = null,
                notice = "Secret chat is already active with ${peer.displayName}."
            )
        }

        val keyPair = generateKeyPair()
        val publicKeyBase64 = keyPair.public.encoded.toBase64()
        val session = (existing ?: PeerSession(peerHash = peer.hash)).copy(
            peerIdRaw = peer.rawId,
            peerDisplayName = peer.displayName,
            peerUniqueId = peer.uniqueId,
            mode = SecretChatMode.SECRET_PENDING_OUT,
            localPrivateKey = keyPair.private.encoded.toBase64(),
            localPublicKey = publicKeyBase64,
            remotePublicKey = null,
            sharedKey = null,
            updatedAt = System.currentTimeMillis()
        )
        synchronized(storeLock) {
            cachedStore[peer.hash] = session
        }
        persist()

        return HandshakeAction(
            type = HandshakeActionType.SEND_INIT,
            payload = encodePayload(
                HandshakePayload(
                    v = PROTOCOL_VERSION,
                    type = PayloadType.DH_INIT.value,
                    peer = peer.hash,
                    pub = publicKeyBase64
                )
            ),
            notice = "Secret chat request sent to ${peer.displayName}."
        )
    }

    fun handleIncomingPayload(peer: PeerDescriptor, payloadText: String): HandshakeAction? {
        val payload = decodePayload(payloadText) ?: return null

        return when (payload.type) {
            PayloadType.DH_INIT.value -> handleIncomingInit(peer, payload)
            PayloadType.DH_REPLY.value -> handleIncomingReply(peer, payload)
            else -> null
        }
    }

    fun clearInMemoryCache() {
        synchronized(storeLock) {
            loaded = false
            loading = false
            cachedStore.clear()
        }
    }

    fun removeSession(peerHash: String) {
        ensureLoaded()
        val removed = synchronized(storeLock) {
            cachedStore.remove(peerHash) != null
        }
        if (removed) {
            persist()
        }
    }

    private fun handleIncomingInit(peer: PeerDescriptor, payload: HandshakePayload): HandshakeAction {
        ensureLoaded()
        val existing = synchronized(storeLock) { cachedStore[peer.hash] }
        val incomingPublicKey = payload.pub

        if (
            existing?.remotePublicKey == incomingPublicKey &&
            existing.sharedKey != null &&
            (existing.mode == SecretChatMode.SECRET_PENDING_IN || existing.mode == SecretChatMode.SECRET_ESTABLISHED)
        ) {
            return HandshakeAction(
                type = HandshakeActionType.NO_OP,
                payload = null,
                notice = "Duplicate secret chat request ignored."
            )
        }

        val localKeyPair = if (existing?.localPrivateKey != null && existing.localPublicKey != null) {
            StoredKeyPair(existing.localPrivateKey, existing.localPublicKey)
        } else {
            val keyPair = generateKeyPair()
            StoredKeyPair(
                privateKey = keyPair.private.encoded.toBase64(),
                publicKey = keyPair.public.encoded.toBase64()
            )
        }

        val sharedKey = deriveSharedKey(
            privateKeyBase64 = localKeyPair.privateKey,
            publicKeyBase64 = incomingPublicKey
        )

        val session = (existing ?: PeerSession(peerHash = peer.hash)).copy(
            peerIdRaw = peer.rawId,
            peerDisplayName = peer.displayName,
            peerUniqueId = peer.uniqueId,
            mode = SecretChatMode.SECRET_ESTABLISHED,
            localPrivateKey = localKeyPair.privateKey,
            localPublicKey = localKeyPair.publicKey,
            remotePublicKey = incomingPublicKey,
            sharedKey = sharedKey,
            updatedAt = System.currentTimeMillis()
        )
        synchronized(storeLock) {
            cachedStore[peer.hash] = session
        }
        persist()

        return HandshakeAction(
            type = HandshakeActionType.SEND_REPLY,
            payload = encodePayload(
                HandshakePayload(
                    v = PROTOCOL_VERSION,
                    type = PayloadType.DH_REPLY.value,
                    peer = peer.hash,
                    pub = localKeyPair.publicKey
                )
            ),
            notice = "Secret chat established with ${peer.displayName}."
        )
    }

    private fun handleIncomingReply(peer: PeerDescriptor, payload: HandshakePayload): HandshakeAction {
        ensureLoaded()
        val existing = synchronized(storeLock) { cachedStore[peer.hash] }
            ?: return HandshakeAction(
                type = HandshakeActionType.NO_OP,
                payload = null,
                notice = "Secret chat reply ignored because no local handshake is pending."
            )

        if (existing.remotePublicKey == payload.pub && existing.sharedKey != null) {
            return HandshakeAction(
                type = HandshakeActionType.NO_OP,
                payload = null,
                notice = "Duplicate secret chat reply ignored."
            )
        }

        val localPrivateKey = existing.localPrivateKey
            ?: return HandshakeAction(
                type = HandshakeActionType.NO_OP,
                payload = null,
                notice = "Secret chat reply ignored because local private key is missing."
            )

        val sharedKey = deriveSharedKey(
            privateKeyBase64 = localPrivateKey,
            publicKeyBase64 = payload.pub
        )

        synchronized(storeLock) {
            cachedStore[peer.hash] = existing.copy(
                peerIdRaw = peer.rawId,
                peerDisplayName = peer.displayName,
                peerUniqueId = peer.uniqueId,
                mode = SecretChatMode.SECRET_ESTABLISHED,
                remotePublicKey = payload.pub,
                sharedKey = sharedKey,
                updatedAt = System.currentTimeMillis()
            )
        }
        persist()

        return HandshakeAction(
            type = HandshakeActionType.ESTABLISHED,
            payload = null,
            notice = "Secret chat established with ${peer.displayName}."
        )
    }

    private fun getSession(peerHash: String): PeerSession? {
        ensureLoaded()
        return synchronized(storeLock) { cachedStore[peerHash] }
    }

    private fun ensureLoaded() {
        if (loaded || loading) return
        synchronized(storeLock) {
            if (loaded || loading) return
            loading = true
        }
        appScope.launch {
            val raw = runCatching {
                dataStoreManager.readSetting(SettingKeys.SESSION_STORE, "{}")
            }.getOrElse { exception ->
                Log.e("QQE2EE", "Failed to load secret chat sessions", exception)
                "{}"
            }
            val decoded = try {
                json.decodeFromString<Map<String, PeerSession>>(raw).toMutableMap()
            } catch (_: Exception) {
                mutableMapOf()
            }
            synchronized(storeLock) {
                cachedStore = decoded
                loaded = true
                loading = false
            }
        }
    }

    private fun persist() {
        val encoded = synchronized(storeLock) {
            json.encodeToString(cachedStore)
        }
        appScope.launch {
            runCatching {
                dataStoreManager.saveSetting(SettingKeys.SESSION_STORE, encoded)
            }.onFailure { exception ->
                Log.e("QQE2EE", "Failed to persist secret chat sessions", exception)
            }
        }
    }

    private fun encodePayload(payload: HandshakePayload): String = json.encodeToString(payload)

    private fun decodePayload(payloadText: String): HandshakePayload? {
        return try {
            json.decodeFromString<HandshakePayload>(payloadText)
        } catch (_: Exception) {
            null
        }
    }

    private fun generateKeyPair() = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec(CURVE_NAME))
    }.generateKeyPair()

    private fun deriveSharedKey(
        privateKeyBase64: String,
        publicKeyBase64: String
    ): String {
        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(Base64.decode(privateKeyBase64, Base64.NO_WRAP))
        )
        val publicKey = keyFactory.generatePublic(
            X509EncodedKeySpec(Base64.decode(publicKeyBase64, Base64.NO_WRAP))
        )
        val sharedSecret = KeyAgreement.getInstance("ECDH").run {
            init(privateKey)
            doPhase(publicKey, true)
            generateSecret()
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val protocolContext = "qqe2ee_secret_chat_v1".toByteArray(Charsets.UTF_8)
        val derived = digest.digest(sharedSecret + protocolContext)
        return derived.toBase64()
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { eachByte -> "%02x".format(eachByte) }
    }

    private data class StoredKeyPair(
        val privateKey: String,
        val publicKey: String
    )

    private const val PROTOCOL_VERSION = 1
    private const val CURVE_NAME = "secp256r1"
}

@Serializable
data class PeerSession(
    val peerHash: String,
    val peerIdRaw: String? = null,
    val peerDisplayName: String? = null,
    val peerUniqueId: String? = null,
    val mode: SecretChatMode = SecretChatMode.DEFAULT,
    val localPrivateKey: String? = null,
    val localPublicKey: String? = null,
    val remotePublicKey: String? = null,
    val sharedKey: String? = null,
    val updatedAt: Long = 0L
)

@Serializable
enum class SecretChatMode {
    DEFAULT,
    SECRET_PENDING_OUT,
    SECRET_PENDING_IN,
    SECRET_ESTABLISHED
}

data class PeerDescriptor(
    val rawId: String,
    val hash: String,
    val displayName: String,
    val uniqueId: String? = null
)

@Serializable
data class HandshakePayload(
    val v: Int,
    val type: String,
    val peer: String,
    val pub: String
)

data class HandshakeAction(
    val type: HandshakeActionType,
    val payload: String?,
    val notice: String
)

enum class HandshakeActionType {
    SEND_INIT,
    SEND_REPLY,
    ESTABLISHED,
    ALREADY_ESTABLISHED,
    NO_OP
}

private enum class PayloadType(val value: String) {
    DH_INIT("dh_init"),
    DH_REPLY("dh_reply")
}
