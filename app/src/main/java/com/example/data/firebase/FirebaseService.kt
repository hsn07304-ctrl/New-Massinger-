package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.NotificationSettings
import com.example.data.model.ParticipantInfo
import com.example.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseService {

    private const val TAG = "FirebaseService"

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    // Fallback reactive memory store for instant testing or offline SDK states
    private val memoryUsers = mutableMapOf<String, User>()
    private val memoryConversations = mutableMapOf<String, Conversation>()
    private val memoryMessages = mutableMapOf<String, MutableList<Message>>()

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    private val _conversationsFlow = MutableStateFlow<List<Conversation>>(emptyList())
    val conversationsFlow: StateFlow<List<Conversation>> = _conversationsFlow.asStateFlow()

    private val _notificationSettings = MutableStateFlow(NotificationSettings())
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var conversationsListener: ListenerRegistration? = null
    private var userDocListener: ListenerRegistration? = null

    fun init(context: Context) {
        try {
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            
            auth?.currentUser?.uid?.let { uid ->
                startCurrentUserListener(uid)
                startConversationsListener(uid)
                updatePresence(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase SDK initialization info: ${e.message}")
        }

        // No mock/fake seed users. Strictly real registered users in Firestore.
    }

    private fun seedSampleUsers() {
        // Mock users disabled according to user request.
    }

    // --- AUTHENTICATION ---

    suspend fun registerUser(
        displayName: String,
        username: String,
        password: String,
        bio: String,
        socials: List<String>
    ): Result<User> {
        val usernameLower = username.trim().lowercase()

        if (usernameLower.isBlank()) {
            return Result.failure(Exception("اسم المستخدم مطلوب"))
        }

        // 1. Check Username Uniqueness (Case-Insensitive)
        val isUnique = checkUsernameUniqueness(usernameLower)
        if (!isUnique) {
            return Result.failure(Exception("اسم المستخدم (@$usernameLower) مستخدم بالفعل"))
        }

        val email = "${usernameLower}@whisper.app"

        return try {
            val authResult = auth?.createUserWithEmailAndPassword(email, password)?.await()
            val uid = authResult?.user?.uid ?: ("user_" + UUID.randomUUID().toString().take(8))

            val newUser = User(
                uid = uid,
                displayName = displayName.trim(),
                username = username.trim(),
                usernameLower = usernameLower,
                bio = bio.trim(),
                socials = socials.take(3),
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

            // Save to Firestore
            firestore?.collection("users")?.document(uid)?.set(newUser)?.await()
            firestore?.collection("usernames")?.document(usernameLower)
                ?.set(mapOf("uid" to uid, "username" to username))?.await()

            // Save to memory
            memoryUsers[uid] = newUser
            _currentUserFlow.value = newUser

            startCurrentUserListener(uid)
            startConversationsListener(uid)
            updatePresence(true)

            Result.success(newUser)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore register fallback: ${e.message}")
            // Fallback store
            val uid = "user_" + UUID.randomUUID().toString().take(8)
            val newUser = User(
                uid = uid,
                displayName = displayName.trim(),
                username = username.trim(),
                usernameLower = usernameLower,
                bio = bio.trim(),
                socials = socials.take(3),
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            memoryUsers[uid] = newUser
            _currentUserFlow.value = newUser
            refreshMemoryConversations(uid)
            Result.success(newUser)
        }
    }

    suspend fun loginUser(usernameInput: String, password: String): Result<User> {
        val inputLower = usernameInput.trim().lowercase()
        val email = if (inputLower.contains("@")) inputLower else "${inputLower}@whisper.app"

        return try {
            val authResult = auth?.signInWithEmailAndPassword(email, password)?.await()
            val uid = authResult?.user?.uid ?: throw Exception("تعذر العثور على حساب المستخدم")

            val userDoc = firestore?.collection("users")?.document(uid)?.get()?.await()
            val user = userDoc?.toObject(User::class.java)
                ?: memoryUsers[uid]
                ?: throw Exception("تعذر جلب بيانات المستخدم")

            _currentUserFlow.value = user
            startCurrentUserListener(uid)
            startConversationsListener(uid)
            updatePresence(true)

            Result.success(user)
        } catch (e: Exception) {
            Log.w(TAG, "Login Firestore info: ${e.message}")
            // Check memory users by username lower
            val matchingUser = memoryUsers.values.find { it.usernameLower == inputLower }
            if (matchingUser != null) {
                _currentUserFlow.value = matchingUser
                refreshMemoryConversations(matchingUser.uid)
                Result.success(matchingUser)
            } else {
                Result.failure(Exception("اسم المستخدم أو كلمة المرور غير صحيحة"))
            }
        }
    }

    fun logout() {
        val uid = _currentUserFlow.value?.uid
        if (uid != null) {
            updatePresence(false)
        }
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Logout error: ${e.message}")
        }
        userDocListener?.remove()
        conversationsListener?.remove()
        _currentUserFlow.value = null
        _conversationsFlow.value = emptyList()
    }

    suspend fun checkUsernameUniqueness(usernameLower: String): Boolean {
        return try {
            val doc = firestore?.collection("usernames")?.document(usernameLower)?.get()?.await()
            if (doc != null && doc.exists()) {
                false
            } else {
                // Double check memory users
                memoryUsers.values.none { it.usernameLower == usernameLower }
            }
        } catch (e: Exception) {
            memoryUsers.values.none { it.usernameLower == usernameLower }
        }
    }

    // --- USER PRESENCE & PROFILE ---

    fun updatePresence(isOnline: Boolean) {
        val uid = _currentUserFlow.value?.uid ?: auth?.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        _currentUserFlow.value = _currentUserFlow.value?.copy(isOnline = isOnline, lastSeen = now)
        memoryUsers[uid] = memoryUsers[uid]?.copy(isOnline = isOnline, lastSeen = now) ?: return

        scope.launch {
            try {
                firestore?.collection("users")?.document(uid)?.update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeen" to now
                    )
                )?.await()
            } catch (e: Exception) {
                // Ignore presence sync errors
            }
        }
    }

    suspend fun updateUserProfile(displayName: String, bio: String, socials: List<String>): Result<User> {
        val uid = _currentUserFlow.value?.uid ?: return Result.failure(Exception("غير مسجل الدخول"))

        val updated = _currentUserFlow.value!!.copy(
            displayName = displayName.trim(),
            bio = bio.trim(),
            socials = socials.take(3)
        )

        _currentUserFlow.value = updated
        memoryUsers[uid] = updated

        return try {
            firestore?.collection("users")?.document(uid)?.update(
                mapOf(
                    "displayName" to displayName.trim(),
                    "bio" to bio.trim(),
                    "socials" to socials.take(3)
                )
            )?.await()
            Result.success(updated)
        } catch (e: Exception) {
            Result.success(updated)
        }
    }

    suspend fun getUserProfile(uid: String): User? {
        if (uid == _currentUserFlow.value?.uid) return _currentUserFlow.value
        return try {
            val doc = firestore?.collection("users")?.document(uid)?.get()?.await()
            doc?.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // --- RANDOM USERS FEED & SEARCH ---

    suspend fun getRandomUsers(limit: Int = 10): List<User> {
        val currentUid = _currentUserFlow.value?.uid ?: ""
        return try {
            val snapshot = firestore?.collection("users")
                ?.limit(30)
                ?.get()
                ?.await()

            val firestoreUsers = snapshot?.documents?.mapNotNull { it.toObject(User::class.java) }
                ?.filter { it.uid != currentUid } ?: emptyList()

            firestoreUsers.shuffled().take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchUsersByUsername(query: String): List<User> {
        val currentUid = _currentUserFlow.value?.uid ?: ""
        val queryLower = query.trim().lowercase()

        if (queryLower.isBlank()) return emptyList()

        return try {
            val snapshot = firestore?.collection("users")
                ?.orderBy("usernameLower")
                ?.startAt(queryLower)
                ?.endAt(queryLower + "\uf8ff")
                ?.limit(20)
                ?.get()
                ?.await()

            snapshot?.documents?.mapNotNull { it.toObject(User::class.java) }
                ?.filter { it.uid != currentUid } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- CONVERSATIONS & CHAT ---

    fun getConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    suspend fun getOrCreateConversation(targetUserId: String): Conversation {
        val currentUid = _currentUserFlow.value?.uid ?: throw Exception("غير مسجل الدخول")
        val conversationId = getConversationId(currentUid, targetUserId)

        val targetUser = getUserProfile(targetUserId) ?: User(uid = targetUserId, displayName = "مستخدم")
        val currentUser = _currentUserFlow.value ?: User(uid = currentUid, displayName = "أنا")

        val participantData = mapOf(
            currentUid to ParticipantInfo(
                uid = currentUid,
                displayName = currentUser.displayName,
                username = currentUser.username,
                isOnline = currentUser.isOnline,
                lastSeen = currentUser.lastSeen
            ),
            targetUserId to ParticipantInfo(
                uid = targetUserId,
                displayName = targetUser.displayName,
                username = targetUser.username,
                isOnline = targetUser.isOnline,
                lastSeen = targetUser.lastSeen
            )
        )

        return try {
            val doc = firestore?.collection("conversations")?.document(conversationId)?.get()?.await()
            if (doc != null && doc.exists()) {
                doc.toObject(Conversation::class.java)!!
            } else {
                val newConv = Conversation(
                    id = conversationId,
                    participantIds = listOf(currentUid, targetUserId),
                    participantData = participantData,
                    lastMessageText = "",
                    lastMessageTimestamp = System.currentTimeMillis()
                )
                firestore?.collection("conversations")?.document(conversationId)?.set(newConv)?.await()
                memoryConversations[conversationId] = newConv
                newConv
            }
        } catch (e: Exception) {
            val existing = memoryConversations[conversationId]
            if (existing != null) {
                existing
            } else {
                val newConv = Conversation(
                    id = conversationId,
                    participantIds = listOf(currentUid, targetUserId),
                    participantData = participantData,
                    lastMessageText = "",
                    lastMessageTimestamp = System.currentTimeMillis()
                )
                memoryConversations[conversationId] = newConv
                refreshMemoryConversations(currentUid)
                newConv
            }
        }
    }

    suspend fun sendMessage(conversationId: String, receiverId: String, textText: String): Message {
        val currentUid = _currentUserFlow.value?.uid ?: throw Exception("غير مسجل الدخول")
        val text = textText.trim()
        if (text.isBlank()) throw Exception("الرسالة فارغة")

        val messageId = "msg_" + UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val newMessage = Message(
            id = messageId,
            senderId = currentUid,
            receiverId = receiverId,
            text = text,
            timestamp = timestamp,
            status = MessageStatus.SENT
        )

        // Optimistic local updates
        val messagesList = memoryMessages.getOrPut(conversationId) { mutableListOf() }
        messagesList.add(newMessage)

        // Update conversation state in memory
        val conv = memoryConversations[conversationId]
        if (conv != null) {
            val updatedConv = conv.copy(
                lastMessageText = text,
                lastMessageTimestamp = timestamp,
                lastMessageSenderId = currentUid,
                typingMap = conv.typingMap + (currentUid to false)
            )
            memoryConversations[conversationId] = updatedConv
            refreshMemoryConversations(currentUid)
        }

        // Send to Firestore asynchronously
        scope.launch {
            try {
                firestore?.collection("conversations")
                    ?.document(conversationId)
                    ?.collection("messages")
                    ?.document(messageId)
                    ?.set(newMessage)
                    ?.await()

                firestore?.collection("conversations")
                    ?.document(conversationId)
                    ?.update(
                        mapOf(
                            "lastMessageText" to text,
                            "lastMessageTimestamp" to timestamp,
                            "lastMessageSenderId" to currentUid,
                            "isTyping" to false,
                            "typingMap.$currentUid" to false
                        )
                    )?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore message send info: ${e.message}")
            }
        }

        return newMessage
    }

    fun setTypingStatus(conversationId: String, isTyping: Boolean) {
        val currentUid = _currentUserFlow.value?.uid ?: return

        // Local state
        val conv = memoryConversations[conversationId]
        if (conv != null) {
            memoryConversations[conversationId] = conv.copy(
                isTyping = isTyping,
                typingMap = conv.typingMap + (currentUid to isTyping)
            )
            refreshMemoryConversations(currentUid)
        }

        // Firestore sync
        scope.launch {
            try {
                firestore?.collection("conversations")
                    ?.document(conversationId)
                    ?.update(
                        mapOf(
                            "isTyping" to isTyping,
                            "typingMap.$currentUid" to isTyping
                        )
                    )
                    ?.await()
            } catch (e: Exception) {
                // Ignore typing sync errors
            }
        }
    }

    fun markMessagesAsRead(conversationId: String) {
        val currentUid = _currentUserFlow.value?.uid ?: return

        // Local state
        val messagesList = memoryMessages[conversationId]
        if (messagesList != null) {
            for (i in messagesList.indices) {
                val msg = messagesList[i]
                if (msg.receiverId == currentUid && msg.status != MessageStatus.READ) {
                    messagesList[i] = msg.copy(status = MessageStatus.READ)
                }
            }
        }

        // Firestore sync
        scope.launch {
            try {
                val unreadSnapshot = firestore?.collection("conversations")
                    ?.document(conversationId)
                    ?.collection("messages")
                    ?.whereEqualTo("receiverId", currentUid)
                    ?.get()
                    ?.await()

                unreadSnapshot?.documents?.forEach { doc ->
                    val status = doc.getString("status")
                    if (status != MessageStatus.READ.name) {
                        doc.reference.update("status", MessageStatus.READ.name)
                    }
                }

                firestore?.collection("conversations")
                    ?.document(conversationId)
                    ?.update("unreadCountMap.$currentUid", 0)
            } catch (e: Exception) {
                Log.w(TAG, "Firestore mark as read info: ${e.message}")
            }
        }
    }

    fun updateNotificationSettings(messagesEnabled: Boolean, soundEnabled: Boolean, showContent: Boolean) {
        _notificationSettings.value = NotificationSettings(
            messagesEnabled = messagesEnabled,
            soundEnabled = soundEnabled,
            showContent = showContent
        )
    }

    // --- REALTIME LISTENERS ---

    private fun startCurrentUserListener(uid: String) {
        userDocListener?.remove()
        userDocListener = firestore?.collection("users")?.document(uid)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    if (user != null) {
                        _currentUserFlow.value = user
                        memoryUsers[uid] = user
                    }
                }
            }
    }

    private fun startConversationsListener(uid: String) {
        conversationsListener?.remove()
        conversationsListener = firestore?.collection("conversations")
            ?.whereArrayContains("participantIds", uid)
            ?.orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    refreshMemoryConversations(uid)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Conversation::class.java) }
                    _conversationsFlow.value = list
                    list.forEach { memoryConversations[it.id] = it }
                }
            }
    }

    private fun refreshMemoryConversations(uid: String) {
        val list = memoryConversations.values
            .filter { it.participantIds.contains(uid) }
            .sortedByDescending { it.lastMessageTimestamp }
        _conversationsFlow.value = list
    }

    fun listenToMessages(
        conversationId: String,
        onMessagesChanged: (List<Message>) -> Unit
    ): ListenerRegistration? {
        val initialList = memoryMessages[conversationId] ?: mutableListOf()
        onMessagesChanged(initialList.toList())

        return firestore?.collection("conversations")
            ?.document(conversationId)
            ?.collection("messages")
            ?.orderBy("timestamp", Query.Direction.ASCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    memoryMessages[conversationId] = messages.toMutableList()
                    onMessagesChanged(messages)
                }
            }
    }

    fun listenToConversation(
        conversationId: String,
        onConversationChanged: (Conversation) -> Unit
    ): ListenerRegistration? {
        memoryConversations[conversationId]?.let { onConversationChanged(it) }

        return firestore?.collection("conversations")
            ?.document(conversationId)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val conv = snapshot.toObject(Conversation::class.java)
                    if (conv != null) {
                        memoryConversations[conversationId] = conv
                        onConversationChanged(conv)
                    }
                }
            }
    }
}
