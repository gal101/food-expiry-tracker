package com.gal101.foodexpirytracker.firebaseSync

// Importuri noi pentru Firestore și Corutine
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration // <-- Import nou
import com.google.firebase.firestore.firestore
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ... (enum AuthState rămâne la fel) ...
enum class AuthState {
    LOADING,
    LOGGED_IN,
    LOGGED_IN_ANONYMOUSLY,
    LOGGED_OUT
}

enum class AuthOperationState {
    SUCCESS,
    FAILED_EMAIL_EXISTS,
    FAILED_WEAK_PASSWORD,
    FAILED_INVALID_CREDENTIALS,
    FAILED_OTHER,
    LOADING,
    IDLE,
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    // --- StateFlows for UI ---

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _currentHouseholdId = MutableStateFlow<String?>(null)
    val currentHouseholdId: StateFlow<String?> = _currentHouseholdId.asStateFlow()

    private val _authState: MutableStateFlow<AuthState> =
            MutableStateFlow<AuthState>(AuthState.LOADING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _authOperationState = MutableStateFlow(AuthOperationState.IDLE)
    val authOperationState: StateFlow<AuthOperationState> = _authOperationState.asStateFlow()

    private val _joinCode = MutableStateFlow<String?>(null)
    val joinCode: StateFlow<String?> = _joinCode.asStateFlow()

    private val _personalHouseholdId = MutableStateFlow<String?>(null)
    val personalHouseholdId: StateFlow<String?> = _personalHouseholdId.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _householdMemberIds = MutableStateFlow<List<String>>(emptyList())
    val householdMemberIds: StateFlow<List<String>> = _householdMemberIds.asStateFlow()

    private val _householdMembers = MutableStateFlow<Map<String, String>>(emptyMap())
    val householdMembers: StateFlow<Map<String, String>> = _householdMembers.asStateFlow()

    private val _householdMemberNames = MutableStateFlow<List<String>>(emptyList())
    val householdMemberNames: StateFlow<List<String>> = _householdMemberNames.asStateFlow()

    private val _householdOwnerId = MutableStateFlow<String?>(null)
    val householdOwnerId: StateFlow<String?> = _householdOwnerId.asStateFlow()

    private val _householdOwnerName = MutableStateFlow<String?>(null)
    val householdOwnerName: StateFlow<String?> = _householdOwnerName.asStateFlow()

    private val _joinState = MutableStateFlow(JoinState.IDLE)
    val joinState: StateFlow<JoinState> = _joinState.asStateFlow()

    val isViewingPersonalHousehold: StateFlow<Boolean> =
            currentHouseholdId
                    .combine(personalHouseholdId) { current, personal ->
                        // They are equal if both are not null AND they are the same string
                        current != null && current == personal
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = false // Default to false
                    )

    val currentUserEmail: StateFlow<String?> =
            currentUser
                    .map { firebaseUser ->
                        // Dacă user-ul există ȘI NU e anonim, returnează email-ul lui
                        if (firebaseUser != null && !firebaseUser.isAnonymous) {
                            firebaseUser.email
                        } else {
                            null // Altfel, email-ul e null
                        }
                    }
                    .stateIn( // Transformă fluxul într-un StateFlow
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            // Setează valoarea inițială corect
                            initialValue =
                                    if (auth.currentUser != null && !auth.currentUser!!.isAnonymous)
                                            auth.currentUser!!.email
                                    else null
                    )

    // Listener-ul pentru autentificare
    private val authStateListener =
            FirebaseAuth.AuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser

                // --- MODIFICARE NOUĂ: Când user-ul se schimbă, pornim un listener nou pentru
                // profilul lui ---
                if (firebaseAuth.currentUser != null) {
                    if (firebaseAuth.currentUser!!.isAnonymous) {
                        _authState.value = AuthState.LOGGED_IN_ANONYMOUSLY // Logged in as Anonymous
                    } else {
                        _authState.value = AuthState.LOGGED_IN // Logged in Permanently
                    }
                    startUserProfileListener(firebaseAuth.currentUser!!.uid)
                } else {
                    // User-ul s-a delogat, oprim listener-ul de profil
                    _authState.value = AuthState.LOGGED_OUT
                    stopUserProfileListener()
                    _currentHouseholdId.value = null
                }
            }

    // --- MODIFICARE NOUĂ: Listener-ul pentru documentul 'user' ---
    private var userProfileListener: ListenerRegistration? = null
    private var householdListener: ListenerRegistration? = null
    private var isCreatingProfile = false
    private var recreateAttempts = 0 // To prevent quota exhaustion loops

    private fun startUserProfileListener(userId: String) {
        // Oprim listener-ul vechi, dacă există
        stopUserProfileListener()

        recreateAttempts = 0 // Reset pe un nou listener de profil valid

        val userDocRef = db.collection("users").document(userId)

        userProfileListener =
                userDocRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("AuthViewModel", "UserProfile listener failed.", error)
                        _currentHouseholdId.value = null
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        _userName.value =
                                snapshot.getString("userName") ?: "User_${userId.substring(0, 6)}"

                        // Extragem ID-ul casei curente și îl publicăm
                        val householdId = snapshot.getString("currentHouseholdId")
                        _currentHouseholdId.value = householdId
                        Log.d("AuthViewModel", "currentHouseholdId updated to: $householdId")

                        val personalId = snapshot.getString("personalHouseholdId")
                        _personalHouseholdId.value = personalId
                        // Verificăm dacă avem un ID și dacă nu am preluat deja codul
                        if (householdId != null) {
                            fetchJoinCode(householdId)
                            startHouseholdListener(householdId)
                        }
                    } else {
                        // Documentul user-ului (încă) nu există sau a fost șters!
                        _currentHouseholdId.value = null
                        _joinCode.value = null
                        _personalHouseholdId.value = null
                        _userName.value = null
                        Log.d(
                                "AuthViewModel",
                                "User document not found. Attempting to recreate (Attempt $recreateAttempts)."
                        )

                        // Dacă utilizatorul este încă logat în Firebase Auth, dar i-a dispărut
                        // profilul:
                        val currentUser = auth.currentUser
                        if (currentUser != null && !isCreatingProfile) {
                            if (recreateAttempts >= 3) {
                                Log.e(
                                        "AuthViewModel",
                                        "Exceeded max recreate attempts. Likely quota exceeded or permission denied. Signing out to prevent infinite loop."
                                )
                                _authState.value = AuthState.LOGGED_OUT // Force UI into error state
                                auth.signOut()
                                return@addSnapshotListener
                            }
                            isCreatingProfile = true
                            recreateAttempts++
                            viewModelScope.launch {
                                try {
                                    // Try to reload to detect if the auth account was deleted from
                                    // server
                                    currentUser.reload().await()
                                    checkAndCreateHouseholdForNewUser(currentUser)
                                } catch (e: Exception) {
                                    Log.e(
                                            "AuthViewModel",
                                            "Failed to recreate user. Account likely deleted or quota exceeded.",
                                            e
                                    )
                                    _authState.value = AuthState.LOGGED_OUT // Force Error UI
                                    auth.signOut()
                                } finally {
                                    isCreatingProfile = false
                                }
                            }
                        }
                    }
                }
    }

    private fun stopUserProfileListener() {
        userProfileListener?.remove()
        userProfileListener = null
        _personalHouseholdId.value = null
        _userName.value = null
    }
    // --- Sfârșitul modificărilor pentru noul listener ---

    private fun startHouseholdListener(householdId: String) {
        stopHouseholdListener() // Oprește listener-ul anterior

        val householdDocRef = db.collection("households").document(householdId)
        householdListener =
                householdDocRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("AuthViewModel", "Household listener failed.", error)
                        // --- MODIFICARE NOUĂ: Golește lista la eroare ---
                        _householdMemberIds.value = emptyList()
                        _householdMembers.value = emptyMap()
                        _householdOwnerId.value = null
                        _householdOwnerName.value = null
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // --- MODIFICARE NOUĂ: Preluăm lista de ID-uri ---
                        // Câmpul 'members' este acum o List<String>
                        val ownerId = snapshot.getString("ownerId")
                        if (ownerId != _householdOwnerId.value) {
                            _householdOwnerId.value = ownerId // Actualizăm ID-ul
                            if (ownerId != null) {
                                fetchOwnerName(ownerId) // Pornim preluarea numelui
                            } else {
                                _householdOwnerName.value = null // Dacă ID-ul e null, numele e null
                            }
                        }
                        val membersList = snapshot.get("members") as? List<String>
                        // Include owner in the IDs list so fetchMemberNames() covers everyone.
                        // The household doc stores ownerId separately from the members array.
                        val allMemberIds =
                                buildList {
                                            if (ownerId != null) add(ownerId)
                                            addAll(membersList ?: emptyList())
                                        }
                                        .distinct()
                        _householdMemberIds.value = allMemberIds
                    } else {
                        // Golește lista dacă documentul casei nu există
                        _householdMemberIds.value = emptyList()
                        _householdMembers.value = emptyMap()
                        _householdOwnerId.value = null
                        _householdOwnerName.value = null

                        // AUTO-RECOVERY: Casa curentă a dispărut!
                        val personalId = _personalHouseholdId.value
                        val currentUser = auth.currentUser
                        if (currentUser != null && !isCreatingProfile) {
                            if (householdId == personalId) {
                                // Casa principală a fost ștearsă (probabil curățare manuală a bazei
                                // de date).
                                // Ștergem și profilul utilizatorului pentru a declanșa re-crearea
                                // completă
                                // din `startUserProfileListener`.
                                Log.w(
                                        "AuthViewModel",
                                        "Personal household $householdId deleted! Hard resetting profile."
                                )
                                isCreatingProfile = true
                                viewModelScope.launch {
                                    try {
                                        db.collection("users")
                                                .document(currentUser.uid)
                                                .delete()
                                                .await()
                                    } catch (e: Exception) {
                                        Log.e("AuthViewModel", "Failed to reset corrupt profile", e)
                                    }
                                    isCreatingProfile = false
                                }
                            } else if (personalId != null) {
                                // Eram într-o casă partajată care a fost ștearsă. Ne întoarcem la
                                // casa personală.
                                Log.w(
                                        "AuthViewModel",
                                        "Shared household $householdId deleted. Removing access."
                                )
                                viewModelScope.launch {
                                    try {
                                        db.collection("users")
                                                .document(currentUser.uid)
                                                .update("currentHouseholdId", personalId)
                                                .await()
                                    } catch (e: Exception) {
                                        Log.e(
                                                "AuthViewModel",
                                                "Failed to revert to personal household",
                                                e
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
    }

    init {
        auth.addAuthStateListener(authStateListener)

        val user = auth.currentUser
        if (user != null) {
            // Utilizatorul e deja logat (dintr-o sesiune veche), e perfect
            startUserProfileListener(user.uid)
            if (user.isAnonymous) {
                _authState.value = AuthState.LOGGED_IN_ANONYMOUSLY
            } else {
                _authState.value = AuthState.LOGGED_IN
            }

            householdMemberIds
                    .onEach { ids -> // Reacționează de fiecare dată când lista de ID-uri se schimbă
                        fetchMemberNames(ids)
                    }
                    .launchIn(viewModelScope) // Pornește ascultarea în scope-ul ViewModel-ului
        } else {
            // --- MODIFICARE CHEIE ---
            // NU există utilizator (prima pornire a aplicației).
            // Trebuie să-l logăm anonim.
            _authState.value = AuthState.LOADING
            viewModelScope.launch {
                signInAnonymously() // Apelăm noi funcția aici
            }
        }
    }

    private fun stopHouseholdListener() {
        householdListener?.remove()
        householdListener = null
        _householdMemberIds.value = emptyList() // Golește lista când oprim
        _householdMembers.value = emptyMap()
        _householdOwnerId.value = null
        _householdOwnerName.value = null
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        stopUserProfileListener() // <-- MODIFICARE NOUĂ
        stopHouseholdListener()
    }

    public fun fetchMemberNames(memberIds: List<String>) {
        // Dacă lista de ID-uri e goală, golește harta de nume și ieși
        if (memberIds.isEmpty()) {
            _householdMembers.value = emptyMap()
            _householdMemberNames.value = emptyList()
            return
        }

        // Lansăm o corutină pentru operațiunea de rețea
        viewModelScope.launch {
            try {
                // Folosim 'whereIn' pentru a prelua TOATE documentele user dintr-o singură mișcare
                val usersSnapshot =
                        db.collection("users")
                                .whereIn(
                                        FieldPath.documentId(),
                                        memberIds
                                ) // Caută după ID-ul documentului
                                .get()
                                .await()

                // Transformăm rezultatele într-o hartă ID -> Nume
                val membersMap =
                        usersSnapshot.documents.associate { doc ->
                            // Cheia este ID-ul, valoarea este numele (cu un default)
                            doc.id to
                                    (doc.getString("userName") ?: "User_${doc.id.substring(0, 6)}")
                        }
                _householdMembers.value = membersMap // Actualizăm StateFlow-ul cu numele
                _householdMemberNames.value = membersMap.values.toList()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching member names", e)
                _householdMembers.value = emptyMap() // Golește harta la eroare
                _householdMemberNames.value = emptyList()
            }
        }
    }

    private fun fetchOwnerName(ownerId: String) {
        viewModelScope.launch {
            try {
                val ownerDoc = db.collection("users").document(ownerId).get().await()

                if (ownerDoc.exists()) {
                    _householdOwnerName.value =
                            ownerDoc.getString("userName") ?: "Owner_${ownerId.substring(0, 6)}"
                } else {
                    _householdOwnerName.value = "Unknown Owner" // Owner document not found
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching owner name", e)
                _householdOwnerName.value = "Error" // Error fetching name
            }
        }
    }

    val isAnonymous: StateFlow<Boolean> =
            currentUser
                    .map { user -> user?.isAnonymous == true }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = auth.currentUser?.isAnonymous == true
                    )

    private suspend fun checkAndCreateHouseholdForNewUser(
            user: FirebaseUser,
            providedUserName: String? = null
    ) {
        val userDocRef = db.collection("users").document(user.uid)

        val document = userDocRef.get().await()

        if (!document.exists()) {
            Log.d("AuthViewModel", "User ${user.uid} is new. Creating household.")

            // Create household
            val newHouseholdRef = db.collection("households").document()
            val newCode = generateJoinCode()
            val initialMembers = emptyList<String>()
            val householdData =
                    mapOf(
                            "ownerId" to user.uid,
                            "joinCode" to newCode,
                            "members" to initialMembers,
                            "hasSeededLocations" to true
                    )

            val finalUserName =
                    if (providedUserName.isNullOrBlank()) {
                        "User_${user.uid.substring(0, 6)}"
                    } else {
                        providedUserName
                    }
            // Create user profile
            val userProfile =
                    mapOf(
                            "personalHouseholdId" to newHouseholdRef.id,
                            "currentHouseholdId" to newHouseholdRef.id,
                            "userName" to finalUserName
                    )

            // Default Custom Locations
            val defaultLocations =
                    listOf(
                            mapOf("name" to "Fridge", "iconName" to "kitchen", "orderIndex" to 0),
                            mapOf("name" to "Freezer", "iconName" to "ac_unit", "orderIndex" to 1),
                            mapOf(
                                    "name" to "Pantry",
                                    "iconName" to "inventory_2",
                                    "orderIndex" to 2
                            ),
                            mapOf(
                                    "name" to "Non-food",
                                    "iconName" to "cleaning_services",
                                    "orderIndex" to 3
                            )
                    )

            // Folosim un batch write pentru a le scrie pe amândouă atomic
            val batch = db.batch()
            batch.set(newHouseholdRef, householdData)
            batch.set(userDocRef, userProfile)

            // Add default locations to the subcollection
            defaultLocations.forEach { loc ->
                val locRef = newHouseholdRef.collection("locations").document()
                batch.set(locRef, loc)
            }

            batch.commit().await()

            // NU e nevoie să setăm _currentHouseholdId.value manual.
            // Listener-ul 'startUserProfileListener' va vedea noul document 'user'
            // și va actualiza automat StateFlow-ul.

        } else {
            Log.d("AuthViewModel", "User ${user.uid} already exists. No household created.")
        }
    }

    suspend fun signInAnonymously() {
        try {
            val authResult = auth.signInAnonymously().await()
            val user = authResult.user

            if (user != null) {
                _authState.value = AuthState.LOGGED_IN_ANONYMOUSLY
                checkAndCreateHouseholdForNewUser(user)
            }
        } catch (e: Exception) {
            Log.e(
                    "AuthViewModel",
                    "Error signing in anonymously, quota exceeded or no connection",
                    e
            )
            _authState.value = AuthState.LOGGED_OUT // Force UI into error state
            auth.signOut()
        }
    }

    private fun fetchJoinCode(householdId: String) {
        // Lansăm o corutină separată pentru a nu bloca listener-ul
        viewModelScope.launch {
            try {
                val householdDoc =
                        db.collection("households")
                                .document(householdId)
                                .get() // Facem o singură citire, nu un listener
                                .await()

                if (householdDoc.exists()) {
                    _joinCode.value = householdDoc.getString("joinCode")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching join code", e)
            }
        }
    }

    /**
     * Registers a user.
     * - If the current user is anonymous, it links the email/password/username.
     * - If the current user is already logged in permanently, it creates a *new* account with the
     * details and automatically logs into it. Updates the authOperationState Flow.
     */
    fun register(userName: String, email: String, password: String) {
        val currentUser = currentUser.value // Get current user state at the start

        _authOperationState.value = AuthOperationState.LOADING // Set LOADING state

        viewModelScope.launch {
            try {
                // --- CASE 1: User is Anonymous -> Link Account ---
                if (currentUser != null && currentUser.isAnonymous) {
                    Log.d("AuthViewModel", "Register: Linking anonymous user.")
                    // 1. Link credentials
                    val credential = EmailAuthProvider.getCredential(email, password)
                    auth.currentUser!!.linkWithCredential(credential).await()
                    // Link successful, currentUser UID remains the same but is no longer anonymous

                    // 2. Update username for the (now permanent) user
                    try {
                        db.collection("users")
                                .document(currentUser.uid)
                                .update("userName", userName.trim())
                                .await()
                        Log.d("AuthViewModel", "Username updated successfully after linking.")
                    } catch (updateError: Exception) {
                        Log.e("AuthViewModel", "Error updating username after linking", updateError)
                        // Treat as partial success
                    }
                    _authOperationState.value = AuthOperationState.SUCCESS
                    // authStateListener will update _authState to LOGGED_IN

                    // --- CASE 2: User is Logged In Permanently -> Create NEW Account ---
                } else if (currentUser != null && !currentUser.isAnonymous) {
                    Log.d(
                            "AuthViewModel",
                            "Register: Creating new account (will log out current user)."
                    )
                    // 1. Create the new user. This automatically logs them in.
                    val createResult = auth.createUserWithEmailAndPassword(email, password).await()
                    val newUser = createResult.user

                    if (newUser != null) {
                        // 2. Create Firestore documents for the NEW user
                        //    (Household and User profile)
                        //    We reuse checkAndCreateHouseholdForNewUser, but it will
                        //    detect the user is new based on Firestore check.
                        //    Need to ensure userName is set correctly though.
                        checkAndCreateHouseholdForNewUser(newUser, userName.trim()) // Pass username

                        _authOperationState.value = AuthOperationState.SUCCESS
                        // authStateListener will update _authState to LOGGED_IN (for the new user)
                    } else {
                        // Should not happen if createUser succeeds
                        _authOperationState.value = AuthOperationState.FAILED_OTHER
                    }

                    // --- CASE 3: No User Logged In (Shouldn't happen with init logic, but handle
                    // just in case) ---
                } else {
                    Log.d("AuthViewModel", "Register: No user logged in, creating new account.")
                    val createResult = auth.createUserWithEmailAndPassword(email, password).await()
                    val newUser = createResult.user
                    if (newUser != null) {
                        checkAndCreateHouseholdForNewUser(newUser, userName.trim())
                        _authOperationState.value = AuthOperationState.SUCCESS
                    } else {
                        _authOperationState.value = AuthOperationState.FAILED_OTHER
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during registration process", e)
                // Set specific FAILED state based on the exception
                _authOperationState.value =
                        when (e) {
                            // These can happen during link OR create
                            is FirebaseAuthUserCollisionException ->
                                    AuthOperationState.FAILED_EMAIL_EXISTS
                            is FirebaseAuthWeakPasswordException ->
                                    AuthOperationState.FAILED_WEAK_PASSWORD
                            // FirebaseAuthInvalidCredentialsException might happen for invalid
                            // email format
                            is FirebaseAuthInvalidCredentialsException ->
                                    AuthOperationState
                                            .FAILED_OTHER // Or a more specific email error state
                            else -> AuthOperationState.FAILED_OTHER
                        }
                // If linking failed, user remains anonymous.
                // If creation failed, user remains the previously logged-in user (or null).
                // authStateListener will handle the final AuthState.
            }
        }
    }

    /**
     * Încearcă să autentifice un utilizator cu email/parolă (Login sau Switch Account).
     * Actualizează fluxul authOperationState.
     */
    suspend fun signInWithEmail(email: String, password: String) {
        _authOperationState.value = AuthOperationState.LOADING // Setează LOADING
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            // Succes! authStateListener va seta automat _authState la LOGGED_IN.
            _authOperationState.value = AuthOperationState.SUCCESS // Setează SUCCESS
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error signing in with email", e)
            // Setează starea FAILED specifică
            _authOperationState.value =
                    when (e) {
                        is FirebaseAuthInvalidUserException,
                        is FirebaseAuthInvalidCredentialsException ->
                                AuthOperationState.FAILED_INVALID_CREDENTIALS
                        else -> AuthOperationState.FAILED_OTHER
                    }
            // Nu e nevoie să revertim _authState, utilizatorul rămâne cel de dinainte.
        }
    }

    /** Attempts to authenticate a user using a Google ID token. */
    suspend fun signInWithGoogle(idToken: String) {
        val userFlowVal = currentUser.value
        _authOperationState.value = AuthOperationState.LOADING
        try {
            val credential =
                    com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)

            if (userFlowVal != null && userFlowVal.isAnonymous) {
                // Try linking to preserve anonymous household
                try {
                    val authResult = userFlowVal.linkWithCredential(credential).await()
                    val linkedUser = authResult.user
                    val displayName =
                            linkedUser?.displayName ?: "User_${linkedUser?.uid?.substring(0, 6)}"

                    try {
                        db.collection("users")
                                .document(linkedUser!!.uid)
                                .update("userName", displayName)
                                .await()
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to update linked user display name", e)
                    }
                    _authOperationState.value = AuthOperationState.SUCCESS
                } catch (linkError: Exception) {
                    Log.e("AuthViewModel", "Linking failed, trying normal sign in", linkError)
                    if (linkError is FirebaseAuthUserCollisionException) {
                        // User already has an account, log them into it instead of linking
                        auth.signInWithCredential(credential).await()
                        _authOperationState.value = AuthOperationState.SUCCESS
                    } else {
                        throw linkError
                    }
                }
            } else {
                // Normal Auth Flow
                val authResult = auth.signInWithCredential(credential).await()
                val loggedInUser = authResult.user

                if (loggedInUser != null) {
                    if (authResult.additionalUserInfo?.isNewUser == true) {
                        val displayName =
                                loggedInUser.displayName
                                        ?: "User_${loggedInUser.uid.substring(0, 6)}"
                        checkAndCreateHouseholdForNewUser(loggedInUser, displayName)
                    }
                    _authOperationState.value = AuthOperationState.SUCCESS
                } else {
                    _authOperationState.value = AuthOperationState.FAILED_OTHER
                }
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error signing in with Google", e)
            _authOperationState.value = AuthOperationState.FAILED_OTHER
        }
    }

    /**
     * Resetează starea operațiunii de autentificare la IDLE. Apelează asta când ecranul de
     * Login/Register este părăsit sau când începe o nouă operațiune.
     */
    fun resetAuthOperationState() {
        _authOperationState.value = AuthOperationState.IDLE
    }

    suspend fun logout() {
        _authState.value = AuthState.LOADING // UPDATE STATE
        auth.signOut()
        // The listener will fire, set state to LOGGED_OUT
    }

    /**
     * Generates a simple, 6-character, human-readable join code. e.g., "A4B-Z2C" Using uppercase
     * letters and numbers.
     */
    private fun generateJoinCode(): String {
        val charPool = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789"
        val code =
                (1..6)
                        .map { Random.nextInt(0, charPool.length) }
                        .map(charPool::get)
                        .joinToString("")

        return code.substring(0, 3) + "-" + code.substring(3, 6)
    }

    /**
     * Removes the current user from the current household (if they are not the owner) and switches
     * their view back to their personal household.
     */
    suspend fun leaveHousehold() {
        // 1. Get necessary IDs from current state
        val user = currentUser.value
        val userId = user?.uid
        val leavingHouseholdId = currentHouseholdId.value // The household we are leaving
        val ownPersonalId = personalHouseholdId.value // The household to return to

        // Check if we have all the required information
        if (userId == null || leavingHouseholdId == null || ownPersonalId == null) {
            Log.e("AuthViewModel", "Cannot leave household: Missing required IDs.")
            // Optionally set an error state here
            return
        }

        // Prevent leaving the personal household (shouldn't be possible via UI, but good safety
        // check)
        if (leavingHouseholdId == ownPersonalId) {
            Log.w("AuthViewModel", "Attempted to leave personal household. Action ignored.")
            return
        }

        Log.d(
                "AuthViewModel",
                "Attempting to leave household: $leavingHouseholdId, returning to: $ownPersonalId"
        )

        try {
            // 2. Perform updates using a batched write
            db
                    .runBatch { batch ->
                        // a. Update the user's document: set currentHouseholdId back to personalId
                        val userDocRef = db.collection("users").document(userId)
                        batch.update(userDocRef, "currentHouseholdId", ownPersonalId)

                        // b. Update the old household document: remove user ID from 'members' array
                        val householdDocRef =
                                db.collection("households").document(leavingHouseholdId)
                        batch.update(householdDocRef, "members", FieldValue.arrayRemove(userId))
                    }
                    .await() // Wait for the batch to complete

            // Success!
            Log.d("AuthViewModel", "Successfully left household $leavingHouseholdId.")
            // No need to manually update _currentHouseholdId.
            // The userProfileListener will detect the change in the user document
            // and update the StateFlow automatically.

        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error leaving household", e)
            // Handle error (e.g., show message via another StateFlow)
        }
    }

    /**
     * Removes a specified member from the current household. Only the owner of the household can
     * perform this action. It also attempts to reset the kicked user's currentHouseholdId back to
     * their personalHouseholdId.
     * @param userIdToKick The ID of the user to remove.
     */
    suspend fun kickMember(userIdToKick: String) {
        // 1. Get necessary IDs from current state
        val currentUser = currentUser.value
        val currentUserId = currentUser?.uid
        val kickingHouseholdId = currentHouseholdId.value
        val currentOwnerId = householdOwnerId.value // Get the owner ID we already loaded

        // Check if we have all the required information
        if (currentUserId == null || kickingHouseholdId == null || currentOwnerId == null) {
            Log.e(
                    "AuthViewModel",
                    "Cannot kick member: Missing required IDs (current user, household, or owner)."
            )
            // Optionally set an error state
            return
        }

        // 2. Verify the current user is the owner
        if (currentUserId != currentOwnerId) {
            Log.e(
                    "AuthViewModel",
                    "Permission denied: User $currentUserId is not the owner of household $kickingHouseholdId."
            )
            // Optionally set an error state
            return
        }

        // 3. Prevent owner from kicking themselves
        if (userIdToKick == currentUserId) {
            Log.w("AuthViewModel", "Owner cannot kick themselves.")
            return
        }

        // Indicate loading state (optional)
        // _authState.value = AuthState.LOADING
        Log.d(
                "AuthViewModel",
                "Attempting to kick member: $userIdToKick from household: $kickingHouseholdId"
        )

        try {
            // 4. Get the personalHouseholdId of the user being kicked
            val kickedUserDocRef = db.collection("users").document(userIdToKick)
            val kickedUserDoc = kickedUserDocRef.get().await()
            val kickedUserPersonalId =
                    if (kickedUserDoc.exists()) {
                        kickedUserDoc.getString("personalHouseholdId")
                    } else {
                        Log.w(
                                "AuthViewModel",
                                "Kicked user's document ($userIdToKick) not found. Cannot reset their household."
                        )
                        null // Proceed with kicking, but cannot reset their ID
                    }

            // 5. Perform updates using a batched write
            db
                    .runBatch { batch ->
                        // a. Update the household document: remove userIdToKick from 'members'
                        // array
                        val householdDocRef =
                                db.collection("households").document(kickingHouseholdId)
                        batch.update(
                                householdDocRef,
                                "members",
                                FieldValue.arrayRemove(userIdToKick)
                        )

                        // b. (Recommended) Update the kicked user's document: set
                        // currentHouseholdId back to personalId
                        if (kickedUserPersonalId != null) {
                            batch.update(
                                    kickedUserDocRef,
                                    "currentHouseholdId",
                                    kickedUserPersonalId
                            )
                        } else {
                            // If we couldn't get their personal ID, we can't reset it.
                            // The user will see an empty list next time they open the app
                            // until they manually join a household again.
                        }
                    }
                    .await() // Wait for the batch to complete

            // Success!
            Log.d(
                    "AuthViewModel",
                    "Successfully kicked member $userIdToKick from household $kickingHouseholdId."
            )
            // The household listener will automatically update the member list UI.
            // The kicked user's app will update automatically if it's open
            // because its userProfileListener will see the change in currentHouseholdId.

            // Ensure AuthState is correct (if you used LOADING)
            // _authState.value = if (currentUser.isAnonymous) AuthState.LOGGED_IN_ANONYMOUSLY else
            // AuthState.LOGGED_IN

        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error kicking member", e)
            // Handle error (e.g., show message via another StateFlow)
            // Revert AuthState if needed
            // _authState.value = if (currentUser.isAnonymous) AuthState.LOGGED_IN_ANONYMOUSLY else
            // AuthState.LOGGED_IN
        }
    }

    /**
     * Attempts to join a household using a join code.
     * @param joinCode The code entered by the user.
     * @return True if the join was successful, false otherwise.
     */
    suspend fun joinHousehold(joinCode: String): Boolean {
        // Get the current user, fail if null
        val user = currentUser.value
        val userId = user?.uid
        val currentUserName =
                userName.value // Get the current username (needed for adding to members map later?)
        if (userId == null || currentUserName == null) {
            Log.e("AuthViewModel", "Cannot join household: User not logged in or username missing.")
            _joinState.value = JoinState.ERROR // Set Error state
            return false
        }

        // Get the user's personal household ID for comparison
        val userPersonalId = personalHouseholdId.value
        if (userPersonalId == null) {
            Log.e("AuthViewModel", "Cannot join household: Personal household ID not loaded.")
            _joinState.value = JoinState.ERROR // Set Error state
            return false
        }

        // --- Set Loading State ---
        _joinState.value = JoinState.LOADING

        return try {
            // 1. Query for the household with the given join code
            val querySnapshot =
                    db.collection("households")
                            .whereEqualTo("joinCode", joinCode.trim().uppercase())
                            .limit(1)
                            .get()
                            .await()

            if (querySnapshot.isEmpty) {
                // No household found with that code
                Log.w("AuthViewModel", "Join failed: No household found with code $joinCode")
                _joinState.value = JoinState.ERROR // --- Set Error state ---
                return false
            }

            // 2. Get the found household's document
            val householdDoc = querySnapshot.documents.first()
            val foundHouseholdId = householdDoc.id

            // 3. Check if the user is trying to join their own personal household
            if (foundHouseholdId == userPersonalId) {
                Log.i("AuthViewModel", "User is already in their personal household.")
                // Update currentHouseholdId just in case it was different
                db.collection("users")
                        .document(userId)
                        .update("currentHouseholdId", foundHouseholdId)
                        .await()
                _joinState.value = JoinState.SUCCESS // --- Set Success state ---
                return true // Consider this a "success"
            }

            // 4. Perform the join using a batched write for atomicity
            db
                    .runBatch { batch ->
                        // a. Update the user's document to point to the new household
                        val userDocRef = db.collection("users").document(userId)
                        batch.update(userDocRef, "currentHouseholdId", foundHouseholdId)

                        // b. Add the user's ID to the household's members list
                        batch.update(
                                householdDoc.reference,
                                "members",
                                FieldValue.arrayUnion(userId)
                        )
                    }
                    .await() // Wait for the batch to complete

            // Success!
            Log.d("AuthViewModel", "User $userId joined household $foundHouseholdId successfully.")
            _joinState.value = JoinState.SUCCESS // --- Set Success state ---
            true
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error joining household", e)
            _joinState.value = JoinState.ERROR // --- Set Error state ---
            false
        }
    }

    fun resetJoinState() {
        _joinState.value = JoinState.IDLE
    }

    /**
     * Attempts to change the current user's username in Firestore.
     * @param newName The desired new username.
     */
    suspend fun changeUsername(newName: String) {
        // Basic validation: Check if name is blank or unchanged
        if (newName.isBlank() || newName == userName.value) {
            Log.w("AuthViewModel", "changeUsername cancelled: Name is blank or unchanged.")
            return
        }

        // Ensure user is logged in
        val user = currentUser.value
        if (user == null) {
            Log.e("AuthViewModel", "changeUsername failed: User is not logged in.")
            return
        }
        val userId = user.uid

        // Indicate loading state (optional)
        // _authState.value = AuthState.LOADING

        try {
            // Attempt to update the document in Firestore
            db.collection("users")
                    .document(userId)
                    .update("userName", newName.trim()) // Trim whitespace just in case
                    .await() // Wait for the operation to complete

            // Success! No need to update _userName.value manually.
            // The userProfileListener will detect the change and update the StateFlow.
            Log.d("AuthViewModel", "Username changed successfully for user $userId.")

            // Reset AuthState if you used LOADING
            // _authState.value = if (user.isAnonymous) AuthState.LOGGED_IN_ANONYMOUSLY else
            // AuthState.LOGGED_IN

        } catch (e: Exception) {
            // Handle potential errors (network, permissions, etc.)
            Log.e("AuthViewModel", "Error changing username for user $userId", e)
            // Optionally, set an error state Flow for the UI to display a message
            // Revert AuthState if you used LOADING
            // _authState.value = if (user.isAnonymous) AuthState.LOGGED_IN_ANONYMOUSLY else
            // AuthState.LOGGED_IN
        }
    }
}

enum class JoinState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}
