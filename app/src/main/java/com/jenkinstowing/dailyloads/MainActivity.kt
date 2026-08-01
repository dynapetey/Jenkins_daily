package com.jenkinstowing.dailyloads

import android.accounts.Account
import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PROCESSED_LOADS = "processed_loads_json"
    }

    private val repository = GoogleSheetsRepository()
    private val sheetScope = "https://www.googleapis.com/auth/spreadsheets"
    private val driveScope = "https://www.googleapis.com/auth/drive.metadata.readonly"
    private val tokenScope = "oauth2:$sheetScope $driveScope"
    private val amber = Color.rgb(255, 196, 0)
    private val backgroundColor = Color.rgb(11, 13, 16)
    private val ink = Color.rgb(247, 248, 250)
    private val muted = Color.rgb(174, 180, 190)
    private val surface = Color.rgb(23, 26, 31)
    private val surfaceBorder = Color.rgb(53, 58, 67)

    private lateinit var root: LinearLayout
    private lateinit var refreshButton: Button
    private var account: GoogleSignInAccount? = null
    private var accessToken: String? = null
    private var sheet: DailySheet? = null
    private var pendingWrites = 0
    private var loading = false

    private val signInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(sheetScope), Scope(driveScope))
            .build()
    }
    private val signInClient by lazy { GoogleSignIn.getClient(this, signInOptions) }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            account = task.getResult(ApiException::class.java)
            acquireTokenAndLoad()
        } catch (error: ApiException) {
            val message = when (error.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google sign-in was cancelled."
                10 -> "Google OAuth is not configured for this APK. Add Android package com.jenkinstowing.dailyloads with SHA-1 85:AE:71:AA:12:BE:1E:A1:DD:17:67:68:16:00:1E:C9:8A:EF:99:6E to the Google Cloud project."
                else -> "Google sign-in failed (${error.statusCode}): ${GoogleSignInStatusCodes.getStatusCodeString(error.statusCode)}"
            }
            showSignedOut(message)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            acquireTokenAndLoad()
        } else {
            showSignedOut("Google Sheets permission is required to load and complete vehicles.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
            setBackgroundColor(backgroundColor)
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        setContentView(scroll)

        val processedLoads = intent.getStringExtra(EXTRA_PROCESSED_LOADS)?.let(::parseProcessedLoads).orEmpty()
        if (processedLoads.isNotEmpty()) {
            sheet = DailySheet(spreadsheetId = "", completedColumn = -1, loads = processedLoads)
            showLoads(LocalDate.now().toString())
        } else {
            account = GoogleSignIn.getLastSignedInAccount(this)
            if (account == null) showSignedOut() else acquireTokenAndLoad()
        }
    }

    private fun parseProcessedLoads(json: String): List<Load> = runCatching {
        val rows = JSONArray(json)
        (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            Load(
                rowNumber = index + 2,
                vehicleDetails = row.optString("vehicleDetails"),
                vin = row.optString("vin"),
                origin = row.optString("origin"),
                destination = row.optString("destination"),
                notes = row.optString("notes"),
                drivetrain = row.optString("drivetrain"),
                epb = row.optString("epb"),
                completed = false,
            )
        }
    }.getOrDefault(emptyList())

    private fun acquireTokenAndLoad() {
        val email = account?.email
        if (email.isNullOrBlank()) {
            showSignedOut("The selected Google account did not provide an email address.")
            return
        }
        showLoading("Connecting to Google…")
        lifecycleScope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    GoogleAuthUtil.getToken(this@MainActivity, Account(email, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE), tokenScope)
                }
                accessToken = token
                loadToday()
            } catch (recoverable: UserRecoverableAuthException) {
                recoverable.intent?.let(permissionLauncher::launch)
                    ?: showSignedOut("Google Sheets permission could not be requested on this device.")
            } catch (error: Exception) {
                showSignedOut(error.message ?: "Couldn’t connect to Google.")
            }
        }
    }

    private fun loadToday() {
        val token = accessToken ?: return
        if (pendingWrites > 0) return
        val date = LocalDate.now().toString()
        loading = true
        showLoading("Loading $date load sheet…")
        lifecycleScope.launch {
            try {
                sheet = withContext(Dispatchers.IO) { repository.getTodaySheet(token, date) }
                showLoads(date)
            } catch (error: Exception) {
                showAppError(date, error.message ?: "Couldn’t load today’s sheet.")
            } finally {
                loading = false
            }
        }
    }

    private fun showSignedOut(error: String? = null) {
        root.removeAllViews()
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(space(72))
        root.addView(text("JENKINS DAILY", 12f, amber, true).apply { letterSpacing = .14f })
        root.addView(text("Today’s loads,\nready to roll.", 34f, ink, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(12))
        })
        root.addView(text("Sign in with your configured Google account to view and update today’s load sheet.", 16f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), 0, dp(14), dp(24))
        })
        error?.let { root.addView(errorView(it)) }
        root.addView(Button(this).apply {
            text = "Sign in with Google"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            background = rounded(amber, 14)
            setTextColor(Color.rgb(17, 19, 23))
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setOnClickListener { signInLauncher.launch(signInClient.signInIntent) }
        }, matchWrap(top = 10))
    }

    private fun showLoading(message: String) {
        root.removeAllViews()
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(space(100))
        root.addView(ProgressBar(this).apply { isIndeterminate = true })
        root.addView(text(message, 16f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        })
    }

    private fun showAppError(date: String, message: String) {
        root.removeAllViews()
        root.gravity = Gravity.NO_GRAVITY
        addHeader(date)
        root.addView(errorView(message))
        root.addView(Button(this).apply {
            text = "Try again"
            isAllCaps = false
            setOnClickListener { loadToday() }
        }, matchWrap(top = 16))
    }

    private fun showLoads(date: String) {
        root.removeAllViews()
        root.gravity = Gravity.NO_GRAVITY
        addHeader(date)
        val dailySheet = sheet ?: return
        if (dailySheet.loads.isEmpty()) {
            root.addView(text("No vehicle rows yet", 22f, ink, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(72), 0, dp(8))
            })
            root.addView(text("Sheet1 is available, but it doesn’t contain any loads.", 15f, muted).apply {
                gravity = Gravity.CENTER
            })
            return
        }
        val complete = dailySheet.loads.count { it.completed }
        root.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text("${dailySheet.loads.size} vehicles", 14f, muted, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(text("$complete completed", 14f, muted, true))
        }, matchWrap(bottom = 12))
        dailySheet.loads.forEach { root.addView(loadCard(it), matchWrap(bottom = 12)) }
        val nextLoad = dailySheet.loads.firstOrNull { !it.completed }
        root.addView(Button(this).apply {
            isAllCaps = true
            text = if (nextLoad == null) "All loads completed" else "✓  Complete next load"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(17, 19, 23))
            background = rounded(amber, 14)
            isEnabled = nextLoad != null && pendingWrites == 0
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setOnClickListener { nextLoad?.let(::toggleCompleted) }
        }, matchWrap(top = 4, bottom = 28))
    }

    private fun addHeader(date: String) {
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            val title = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(text("JENKINS DAILY", 11f, amber, true).apply { letterSpacing = .14f })
                addView(text("Today’s loads", 32f, ink, true))
                addView(text(date, 14f, muted))
            }
            addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            refreshButton = Button(this@MainActivity).apply {
                text = "↻"
                contentDescription = "Refresh today’s loads"
                textSize = 24f
                setTextColor(ink)
                minWidth = dp(48)
                isEnabled = !loading && pendingWrites == 0
                background = rounded(surface, 14, surfaceBorder)
                setOnClickListener { loadToday() }
            }
            addView(refreshButton, LinearLayout.LayoutParams(dp(52), dp(48)).apply { marginEnd = dp(8) })
            addView(Button(this@MainActivity).apply {
                text = "⎋"
                contentDescription = "Sign out"
                textSize = 20f
                setTextColor(ink)
                minWidth = dp(48)
                background = rounded(surface, 14, surfaceBorder)
                setOnClickListener {
                    lifecycleScope.launch {
                        accessToken?.let { token -> withContext(Dispatchers.IO) { GoogleAuthUtil.clearToken(this@MainActivity, token) } }
                        Tasks.await(signInClient.signOut())
                        account = null
                        accessToken = null
                        sheet = null
                        showSignedOut()
                    }
                }
            }, LinearLayout.LayoutParams(dp(52), dp(48)))
        }
        root.addView(heading, matchWrap(bottom = 24))
    }

    private fun loadCard(load: Load): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
            alpha = if (load.completed) .55f else 1f
            background = rounded(surface, 16, surfaceBorder)

            addView(CheckBox(this@MainActivity).apply {
                text = if (load.completed) "Completed" else "Mark completed"
                isChecked = load.completed
                buttonTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(amber, muted),
                )
                setTextColor(if (load.completed) amber else muted)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 0, 0, dp(10))
                setOnClickListener { toggleCompleted(load) }
            })
            addView(text(load.vehicleDetails.ifBlank { "Vehicle details unavailable" }, 20f, ink, true).apply {
                if (load.completed) paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            })
            addView(text("VIN ${load.vin.ifBlank { "—" }}", 13f, muted).apply {
                setPadding(0, dp(5), 0, dp(14))
            })
            addView(detailRow("Origin", load.origin, "Destination", load.destination))
            addView(detailRow("Drivetrain", load.drivetrain, "EPB", load.epb))
            if (load.notes.isNotBlank()) {
                addView(detail("Notes", load.notes).apply { setPadding(0, dp(10), 0, 0) })
            }
        }
    }

    private fun toggleCompleted(load: Load) {
        val token = accessToken ?: return
        val current = sheet ?: return
        val completed = !load.completed
        pendingWrites += 1
        sheet = current.copy(loads = current.loads.map { if (it.rowNumber == load.rowNumber) it.copy(completed = completed) else it })
        showLoads(LocalDate.now().toString())
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.updateCompleted(token, current, load.rowNumber, completed) }
            } catch (error: Exception) {
                sheet = sheet?.copy(loads = sheet!!.loads.map {
                    if (it.rowNumber == load.rowNumber) it.copy(completed = load.completed) else it
                })
                pendingWrites -= 1
                showAppError(LocalDate.now().toString(), error.message ?: "Couldn’t save completion.")
                return@launch
            }
            pendingWrites -= 1
            showLoads(LocalDate.now().toString())
        }
    }

    private fun detailRow(firstLabel: String, firstValue: String, secondLabel: String, secondValue: String) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(detail(firstLabel, firstValue), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(detail(secondLabel, secondValue), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(14) })
            setPadding(0, 0, 0, dp(10))
        }

    private fun detail(label: String, value: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(label.uppercase(), 11f, amber, true).apply { letterSpacing = .08f })
        addView(text(value.ifBlank { "—" }, 15f, ink))
    }

    private fun errorView(message: String) = text(message, 14f, Color.rgb(255, 190, 178)).apply {
        setPadding(dp(14))
        background = rounded(Color.rgb(61, 28, 25), 12, Color.rgb(132, 66, 58))
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        stroke?.let { setStroke(dp(1), it) }
    }

    private fun matchWrap(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
