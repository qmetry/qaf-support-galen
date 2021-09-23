[![License](https://img.shields.io/github/license/qmetry/qaf-support-galen.svg)](http://www.opensource.org/licenses/mit-license.php)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.qmetry/qaf-support-galen/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.qmetry/qaf-support-galen)
[![GitHub tag](https://img.shields.io/github/tag/qmetry/qaf-support-galen.svg)](https://github.com/qmetry/qaf-support-galen/tags)
[![javadoc](https://javadoc.io/badge2/com.qmetry/qaf-support-galen/javadoc.svg)](https://javadoc.io/doc/com.qmetry/qaf-support-galen)
# qaf-support-galen
Galen framework support library for automated testing of look and feel for your responsive websites



## Features: ##
 - Ease of use
 - detailed reporting
 - Fail/Success element screen shot
 - Multiple web platforms ui layout validation support
 - CSS and broken image validataion
 - 
This library provides [utility methods](/src/com/qmetry/qaf/automation/support/galen/UiValidationUtils.java) for automated testing of look and feel validation for responsive websites using [galen specs](http://galenframework.com/docs/reference-galen-spec-language-guide/).

## Methods: 
|Method|Description|
|:------|----|
|checkLayout(String specPath)|Checks layout of the page that is currently open in current thread. Takes driver from QAFTestBase  <br/>This method uses following properties: <ul><li><code>layoutvalidation.platforms=list of tags to be included from specification (default is desktop)</code></li><li><code>layoutvalidation.&lt;platform>.screensize=<json value Dimension object> (default is browser maximized)</code></li></ul>Example: <br/><code>layoutvalidation.platforms=desktop;tablet</code><br/><code>layoutvalidation.tablet.screensize={'width':800,'height':750}</code>
|checkLayout(String specPath, String... platforms)|Checks layout of the page that is currently open in current thread for given platforms. <br/>This method uses <code>layoutvalidation.&ltplatform>.screensize</code> property. For example, platform provided in argument are tablet and desktop than it will look for <code>layoutvalidation.tablet.screensize</code> property for tablet and <code>layoutvalidation.desktop.screensize</code> property for desktop|
|verifyImagesNotBroken|verifies images provided usig &lt;img> tag on the page are not broken|

## Usage:
- Add [qaf-support-galen dependecy](https://mvnrepository.com/artifact/com.qmetry/qaf-support-galen/latest) to your project.
- Create [layout specification](http://galenframework.com/docs/reference-galen-spec-language-guide/)
- Call approprivate [utility method](/src/com/qmetry/qaf/automation/support/galen/UiValidationUtils.java)

```java
import static com.qmetry.qaf.automation.support.galen.UiValidationUtils.checkLayout;

@Test
public void validateOroductFaqPageLayout() throws IOException {
  getDriver().get("/ww/en/Categories/Products/PRD-101#tab-faqs");
  checkLayout("resources/ui-specs/item-support-tab.spec");
}

```
**BDD**
```
When get '/ww/en/Categories/Products/PRD-101#tab-faqs'
Then check layout using 'resources/ui-specs/item-support-tab.spec'
```

**Data-driven example:**

|recId |specs |url |
|:------|:-------|:-----|
|products faq tab|resources/ui-specs/item-support-tab.spec|/ww/en/Categories/Products/PRD-101#tab-faqs|
products accessories tab|resources/ui-specs/pdp-page.spec|/ww/en/Categories/Products/PRD-101#tab-accessories|

```
@QAFDataProvider(dataFile = "resources/data/testdata.csv")
@Test
public void validateProductPageLayout(Map<String, Object> data) throws IOException {
  getDriver().get((String) data.get("url"));
  checkLayout((String) data.get("specs"));
}
```

**BDD**
```
@dataFile:resources/data/testdata.csv
Scenario: Validate ProductPage Layout
When get '${url}'
Then check layout using '${specs}'
```
