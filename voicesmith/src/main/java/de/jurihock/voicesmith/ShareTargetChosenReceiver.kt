package de.jurihock.voicesmith

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.chooser.ChooserResult
import de.jurihock.voicesmith.etc.Preferences

class ShareTargetChosenReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val component = selectedComponent(intent) ?: return
    Preferences(context.applicationContext).lastShareTarget = component.flattenToString()
  }

  private fun selectedComponent(intent: Intent): ComponentName? {
    if (Build.VERSION.SDK_INT >= 35) {
      val result = intent.getParcelableExtra(
        Intent.EXTRA_CHOOSER_RESULT,
        ChooserResult::class.java)

      if (result?.type == ChooserResult.CHOOSER_RESULT_SELECTED_COMPONENT) {
        return result.selectedComponent
      }
    }

    return intent.getParcelableExtra(
      Intent.EXTRA_CHOSEN_COMPONENT,
      ComponentName::class.java)
  }

}
