package br.com.carvalho.podcast.core.safety

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import br.com.carvalho.podcast.core.util.deviceCountry
import br.com.carvalho.podcast.domain.model.AgeGroup
import br.com.carvalho.podcast.domain.model.ageGroupOf
import br.com.carvalho.podcast.domain.repository.AgeSignalSource
import com.google.android.play.agesignals.AgeSignalsAccessRequest
import com.google.android.play.agesignals.AgeSignalsManager
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.model.AgeSignalsStatus
import kotlinx.coroutines.tasks.await
import java.lang.ref.WeakReference

/**
 * The Play Age Signals API (26.2, ADR 0012). Since 0.0.4 Play first asks the user, over the screen in front, whether
 * to share; declined, the range stays unknown.
 */
class PlayAgeSignalSource(
    private val context: Context,
    private val manager: AgeSignalsManager,
    private val currentActivity: () -> Activity?,
) : AgeSignalSource {

    // The network's and the SIM's country say where the phone is better than the language does.
    override fun countries(): Set<String> {
        val telephony = context.getSystemService<TelephonyManager>()
        return listOfNotNull(telephony?.networkCountryIso, telephony?.simCountryIso, deviceCountry())
            .filter { it.isNotBlank() }
            .map { it.uppercase() }
            .toSet()
    }

    // ponytail: asked once per process, since the prompt pauses the activity and every resume refreshes; Play may
    // still show it again on the next start. Keep the answer in preferences if that turns out to annoy.
    private var shared: Boolean? = null

    override suspend fun ageGroup(): AgeGroup {
        if (shared == null) shared = askToShare()
        if (shared != true) return AgeGroup.UNKNOWN
        val result = manager.checkAgeSignals(AgeSignalsRequest.builder().build()).await()
        return ageGroupOf(result.ageLower(), result.ageUpper())
    }

    /** Null with no screen in front to ask over: asked again on the next refresh. */
    private suspend fun askToShare(): Boolean? {
        val activity = currentActivity() ?: return null
        val request = AgeSignalsAccessRequest.builder().setActivity(activity).build()
        return manager.requestAgeSignalsAccess(request).await().ageSignalsStatus() == AgeSignalsStatus.SHARED
    }
}

/** The activity in front, for Play's prompt; held weakly so a closed screen is never kept. */
class ResumedActivity(context: Context) : Application.ActivityLifecycleCallbacks {
    private var resumed = WeakReference<Activity>(null)

    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
    }

    fun get(): Activity? = resumed.get()

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed.get() === activity) resumed = WeakReference(null)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
