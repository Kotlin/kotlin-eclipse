package org.jetbrains.kotlin.ui;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;

public class KotlinUiStartupClass implements IStartup {
    
    @Override
    public void earlyStartup() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                askAboutUsageStatisticsReporting();
            }
        });
    }
    
    private void askAboutUsageStatisticsReporting() {
        IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
        boolean isCurrentlyReportingAvailable = preferenceStore.getBoolean(KotlinUsageReporter.UPDATE_USAGE_AVAILABLE_KEY);
        boolean askAvailable = preferenceStore.getBoolean(KotlinUsageReporter.ASK_FOR_USAGE_REPORTING_KEY);
        
        if (askAvailable && !isCurrentlyReportingAvailable) {
            MessageDialogWithToggle dialogWithToogle = MessageDialogWithToggle.openYesNoQuestion(
                    Display.getDefault().getActiveShell(),
                    "Kotlin plugin usage",
                    "Will you allow the Kotlin plugin team to receive anonymous usage statistics for this Eclipse installation with Kotlin Plugin?",
                    "Don't ask again",
                    false,
                    null,
                    null);
            
            if (dialogWithToogle.getReturnCode() == 2) {
                preferenceStore.setValue(KotlinUsageReporter.UPDATE_USAGE_AVAILABLE_KEY, true);
            }
            
            if (dialogWithToogle.getToggleState()) {
                preferenceStore.setValue(KotlinUsageReporter.ASK_FOR_USAGE_REPORTING_KEY, false);
            }
        }
    }
    
}
