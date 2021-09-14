package com.qmetry.qaf.automation.support.galen;

import static com.qmetry.qaf.automation.core.TestBaseProvider.instance;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;

import com.galenframework.page.PageElement;
import com.galenframework.page.selenium.WebPageElement;
import com.galenframework.specs.Spec;
import com.galenframework.specs.page.PageSection;
import com.galenframework.suite.GalenPageAction;
import com.galenframework.validation.PageValidation;
import com.galenframework.validation.ValidationListener;
import com.galenframework.validation.ValidationResult;
import com.qmetry.qaf.automation.core.CheckpointResultBean;
import com.qmetry.qaf.automation.core.MessageTypes;
import com.qmetry.qaf.automation.core.QAFTestBase;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.step.StringTestStep;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebElement;
import com.qmetry.qaf.automation.util.FileUtil;
import com.qmetry.qaf.automation.util.StringUtil;

public class ValidationListenerImpl implements ValidationListener {
	private CheckpointResultBean sectionResult;

	@Override
	public void onObject(PageValidation pageValidation, String objectName) {
	}

	@Override
	public void onAfterObject(PageValidation pageValidation, String objectName) {
	}

	@Override
	public void onBeforeSpec(PageValidation pageValidation, String objectName, Spec spec) {
	}

	@Override
	public void onSpecError(PageValidation pageValidation, String objectName, Spec spec,
			ValidationResult validationResult) {
		
		sectionResult.setType(MessageTypes.TestStepFail);
		QAFTestBase testbase = instance().get();
		int verificationErrors = testbase.getVerificationErrors() + 1;
		testbase.getContext().setProperty("verificationErrors", verificationErrors);
		MessageTypes mtype = validationResult.getError().isOnlyWarn() ? MessageTypes.Warn : MessageTypes.Fail;

		if (mtype.shouldReport()) {
			CheckpointResultBean result = new CheckpointResultBean();
			result.setMessage(String.join("\n", validationResult.getError().getMessages()));
			result.setType(mtype);
			result.setScreenshot(getScreenShot(pageValidation, objectName, spec.toText()));
			sectionResult.getSubCheckPoints().add(result);
		}
	}

	@Override
	public void onSpecSuccess(PageValidation pageValidation, String objectName, Spec spec,
			ValidationResult validationResult) {
		if (MessageTypes.Pass.shouldReport()) {
			CheckpointResultBean result = new CheckpointResultBean();
			result.setMessage(validationResult.getValidationObjects().get(0).getName() + "  " + spec.toText());
			result.setType(MessageTypes.Pass);
			if (ApplicationProperties.SUCEESS_SCREENSHOT.getBoolenVal(true)) {
				result.setScreenshot(getScreenShot(pageValidation, objectName, spec.toText()));
			}
			sectionResult.getSubCheckPoints().add(result);
		}
	}

	@Override
	public void onGlobalError(Exception e) {
	}

	@Override
	public void onBeforePageAction(GalenPageAction action) {
	}

	@Override
	public void onAfterPageAction(GalenPageAction action) {
		System.out.println("GalenPageAction:: " + action.getOriginalCommand());
	}

	@Override
	public void onBeforeSection(PageValidation pageValidation, PageSection pageSection) {
		sectionResult = new CheckpointResultBean();
		sectionResult.setMessage("Verify " + pageSection.getName() +" On " + pageValidation.getSectionFilter().getIncludedTags());
		sectionResult.setType(MessageTypes.TestStepPass);
	}

	@Override
	public void onAfterSection(PageValidation pageValidation, PageSection pageSection) {
		try {
			sectionResult
					.setScreenshot(screenshotOfEle(new QAFExtendedWebElement("tagName=body"), pageSection.getName()));
		} catch (Exception e) {
			sectionResult.setScreenshot(getScreenShot(pageValidation, pageSection.getName()));
		}
		instance().get().getCheckPointResults().add(sectionResult);
		new StringTestStep("COMMENT: '"+pageSection.getName()+"'").execute();
	}

	@Override
	public void onSubLayout(PageValidation pageValidation, String objectName) {
	}

	@Override
	public void onAfterSubLayout(PageValidation pageValidation, String objectName) {
	}

	@Override
	public void onSpecGroup(PageValidation pageValidation, String specGroupName) {
	}

	@Override
	public void onAfterSpecGroup(PageValidation pageValidation, String specGroupName) {
	}

	private String getScreenShot(PageValidation pageValidation, String name) {
		try {
			BufferedImage bi = pageValidation.getPage().getScreenshotImage();
			if (null != bi) {
				File outputfile = FileUtil.generateFile(StringUtil.toCamelCaseIdentifier(name), ".png",
						ApplicationProperties.SCREENSHOT_DIR.getStringVal("img"));
				ImageIO.write(bi, "png", outputfile);
				instance().get().setLastCapturedScreenShot(outputfile.getName());
				return outputfile.getPath();
			}
		} catch (IOException e) {
		}
		return null;
	}

	private String getScreenShot(PageValidation pageValidation, String objectName, String name) {
		PageElement ele = pageValidation.findPageElement(objectName);
		if (ele instanceof WebPageElement) {
			WebElement webele = ((WebPageElement) ele).getWebElement();
			if (ele.isPresent() && ele.isVisible())
				try {
					return screenshotOfEle(webele, name);
				} catch (Exception e) {
					// return getScreenShot(pageValidation, name);
				}
		}
		return null;
	}

	private String screenshotOfEle(WebElement webele, String name) throws Exception {
		String base64Image = webele.getScreenshotAs(OutputType.BASE64);
		String filename = FileUtil.saveImageFile(base64Image, StringUtil.toCamelCaseIdentifier(name),
				ApplicationProperties.SCREENSHOT_DIR.getStringVal("img"));
		return ApplicationProperties.SCREENSHOT_DIR.getStringVal("img") + "/" + filename;
	}

}
