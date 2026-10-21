package ae.ac.ecae.portablewelcome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val supportedAction =
            intent.action ==
                Intent.ACTION_BOOT_COMPLETED ||
            intent.action ==
                Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action ==
                Intent.ACTION_MY_PACKAGE_REPLACED

        if (!supportedAction) {
            return
        }

        val launchIntent =
            Intent(
                context,
                MainActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

        try {
            context.startActivity(launchIntent)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
}
