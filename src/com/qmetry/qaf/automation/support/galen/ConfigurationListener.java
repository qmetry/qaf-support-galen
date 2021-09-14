package com.qmetry.qaf.automation.support.galen;

import com.qmetry.qaf.automation.core.QAFConfigurationListener;
import com.qmetry.qaf.automation.util.PropertyUtil;
import static com.qmetry.qaf.automation.step.JavaStepFinder.GLOBAL_STEPS_PACKAGES;

/**
 * 
 * @author chirag.jayswal
 *
 */
public class ConfigurationListener implements QAFConfigurationListener{
	//@Override
	public void onLoad(PropertyUtil bundle) {
		GLOBAL_STEPS_PACKAGES.add("com.qmetry.qaf.automation.support.galen");
	}

	//@Override
	public void onChange() {
	}

}
