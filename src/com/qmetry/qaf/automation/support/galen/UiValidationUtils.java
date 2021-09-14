/**
 * 
 */
package com.qmetry.qaf.automation.support.galen;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;
import static com.qmetry.qaf.automation.core.TestBaseProvider.instance;
import static com.qmetry.qaf.automation.ui.WebDriverCommandLogger.getMsgForElementOp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.support.Color;

import com.galenframework.api.Galen;
import com.galenframework.speclang2.pagespec.SectionFilter;
import com.galenframework.validation.ValidationListener;
import com.qmetry.qaf.automation.core.AutomationError;
import com.qmetry.qaf.automation.core.MessageTypes;
import com.qmetry.qaf.automation.core.QAFTestBase;
import com.qmetry.qaf.automation.keys.ApplicationProperties;
import com.qmetry.qaf.automation.step.QAFTestStep;
import com.qmetry.qaf.automation.ui.WebDriverTestBase;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebElement;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebElement;
import com.qmetry.qaf.automation.util.FileUtil;
import com.qmetry.qaf.automation.util.JSONUtil;
import com.qmetry.qaf.automation.util.StringUtil;
import com.qmetry.qaf.automation.util.Validator;

/**
 * @author chirag.jayswal
 *
 */
public class UiValidationUtils {
	private UiValidationUtils() {
	}

	/**
	 * Checks layout of the page that is currently open in current thread. Takes
	 * driver from {@link QAFTestBase} <br/>
	 * This method uses following properties: <code><pre>
	 * layoutvalidation.platforms=list of tags to be included from specification (default is desktop)
	 * layoutvalidation.&ltplatform>.screensize=&ltjson value Dimension object> (default is browser maximized)
	 * Example:
	 * layoutvalidation.platforms=desktop;tablet
	 * layoutvalidation.tablet.screensize={'width':800,'height':750}
	 *  </pre>
	 *  </code>
	 *
	 * @param specPath a path to galen spec file
	 * @throws IOException
	 */
	@QAFTestStep(description = "check layout using {specification}")
	public static void checkLayout(String specPath) {
		String[] platforms = getBundle().getStringArray("layoutvalidation.platforms", "desktop");
		checkLayout(specPath, platforms);
	}

	/**
	 * Checks layout of the page that is currently open in current thread for given
	 * platforms. <br/>
	 * This method uses <code>layoutvalidation.&ltplatform>.screensize</code> property. For
	 * example, platform provided in argument are tablet and desktop than it will look
	 * for <code>layoutvalidation.tablet.screensize</code> property for tablet and
	 * <code>layoutvalidation.desktop.screensize</code> property for desktop <code><pre>
	 * layoutvalidation.&ltplatform>.screensize=&ltjson value Dimension object> (default is browser maximized). 
	 * #Example:
	 * layoutvalidation.tablet.screensize={'width':800,'height':750}
	 * </pre></code> 
	 * @param specPath
	 * @param platforms
	 */
	@QAFTestStep(description = "check layout using {specification} for {platforms}")
	public static void checkLayout(String specPath, String... platforms) {

		QAFExtendedWebDriver driver = new WebDriverTestBase().getDriver();
		Dimension originalSize = driver.manage().window().getSize();
		Properties props = new Properties();
		File configfile = new File(getBundle().getString("galen.config", "resources/galen.config"));
		if (configfile.exists()) {
			try(FileInputStream in = new FileInputStream(configfile)) {
				props.load(in);
			} catch (IOException e) {
				System.err.println("Unable to load galen confuguration from " + configfile + ": " + e.getMessage());
			}
		}
		Arrays.asList(platforms).stream().forEach((platform) -> {
			try {
				if (platforms.length > 1) {
					String dimension = getBundle().getString("layoutvalidation." + platform + ".screensize");
					if (StringUtil.isNotBlank(dimension)) {
						driver.manage().window().setSize(JSONUtil.toObject(dimension, Dimension.class));
					} else {
						String msg = String.format(
								"Screen size not specified for \"%s\" platform, using available maximum screen size. You can specify screen size for \"%s\" using \"layoutvalidation.%s.screensize\" property",
								platform, platform, platform);
						System.out.println(msg);
						driver.manage().window().maximize();
					}
				}
				ValidationListener validationListener = new ValidationListenerImpl();

				Galen.checkLayout(driver, specPath,
						new SectionFilter(Arrays.asList(platform), Collections.<String>emptyList()), props, null, null,
						validationListener);
			} catch (IOException e) {
				new AutomationError("Unable to check layout", e);
			}
		});

		if (platforms.length > 1) {
			driver.manage().window().setSize(originalSize);
		}

	}

	@QAFTestStep(description = "verify images are not broken")
	public static void verifyImagesNotBroken() {
		QAFExtendedWebDriver driver = new WebDriverTestBase().getDriver();

		AtomicBoolean loaded = new AtomicBoolean(true);
		StringBuilder sb = new StringBuilder("Images should not be broken");
		driver.waitForAjax();

		driver.findElements("css=img").parallelStream().forEach((image) -> {
			String src = image.getAttribute("src");
			if (StringUtil.isNotBlank(src)) {
				String script = "function isNotBroken(imgUrl){ var xmlHttp = new XMLHttpRequest(); xmlHttp.open( \"GET\", imgUrl, false );xmlHttp.send( null ); return xmlHttp.status==200;}"
						+ "return arguments[0].complete && (((typeof arguments[0].naturalWidth != \"undefined\") && arguments[0].naturalWidth > 0) || isNotBroken(arguments[1]));";

				Object imglaoded = ((QAFExtendedWebElement) image).getWrappedDriver().executeScript(script, image, src);
				if (!(boolean) imglaoded) {
					src = image.getAttribute("outerHTML");
					loaded.set(false);
					image.executeScript("scrollIntoView(false);");
					sb.append("\n").append("Broken Image: ").append(src);
				}
			}
		});
		Validator.verifyTrue(loaded.get(), sb.toString(), sb.toString());
	}

	public static void verifyCss(QAFWebElement ele, String propertyName, String val, String... label) {
		String actual = ele.getCssValue(propertyName);
		boolean outcome = StringUtil.seleniumEquals(actual, val);

		try {
			if (((ApplicationProperties.FAILURE_SCREENSHOT.getBoolenVal(true) && !outcome))
					|| (ApplicationProperties.SUCEESS_SCREENSHOT.getBoolenVal(false))) {
				instance().get().setLastCapturedScreenShot(screenshotOfEle(ele, propertyName));
			}
		} catch (Exception e) {
		}
		String msg = getMsgForElementOp("cssstyle", outcome, ((QAFExtendedWebElement) ele).getDescription(label), val,
				actual);
		instance().get().addAssertionLog(msg, (outcome ? MessageTypes.Pass : MessageTypes.Fail));
	}

	public static void verifyColor(QAFWebElement ele, String propertyName, String val, String... label) {
		String actual = ele.getCssValue(propertyName);
		boolean outcome = Color.fromString(actual).asRgba().equals(Color.fromString(String.valueOf(val)).asRgba());

		try {
			if (((ApplicationProperties.FAILURE_SCREENSHOT.getBoolenVal(true) && !outcome))
					|| (ApplicationProperties.SUCEESS_SCREENSHOT.getBoolenVal(false))) {
				instance().get().setLastCapturedScreenShot(screenshotOfEle(ele, propertyName));
			}
		} catch (Exception e) {
		}
		String msg = getMsgForElementOp("cssstyle", outcome, ((QAFExtendedWebElement) ele).getDescription(label),
				val + " " + Color.fromString(val).asRgba(), actual);
		instance().get().addAssertionLog(msg, (outcome ? MessageTypes.Pass : MessageTypes.Fail));
	}

	private static String screenshotOfEle(QAFWebElement webele, String name) throws Exception {
		String base64Image = webele.getScreenshotAs(OutputType.BASE64);
		String filename = FileUtil.saveImageFile(base64Image, StringUtil.toCamelCaseIdentifier(name),
				ApplicationProperties.SCREENSHOT_DIR.getStringVal("img"));
		return filename;
	}
}
