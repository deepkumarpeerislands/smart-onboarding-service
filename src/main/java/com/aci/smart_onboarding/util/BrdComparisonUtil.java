package com.aci.smart_onboarding.util;

import com.aci.smart_onboarding.dto.BrdComparisonResponse;
import com.aci.smart_onboarding.dto.BrdForm;
import com.aci.smart_onboarding.model.BRD;
import com.aci.smart_onboarding.model.Site;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class BrdComparisonUtil {

  private BrdComparisonUtil() {
    // Private constructor to prevent instantiation
  }

  public static BrdComparisonResponse compareBrdAndSiteBrdForm(
      BRD brd, Site site, String sectionName) {
    BrdComparisonResponse.BrdComparisonResponseBuilder builder =
        BrdComparisonResponse.builder()
            .brdId(brd.getBrdId())
            .siteId(site.getSiteId())
            .siteName(site.getSiteName());

    BrdForm siteBrdForm = site.getBrdForm();
    Map<String, Object> differences = new HashMap<>();

    // Compare only the specified section
    Object brdSection = getSection(brd, sectionName);
    Object siteSection = getSection(siteBrdForm, sectionName);

    if (brdSection == null && siteSection == null) {
      return builder.differences(differences).build();
    }

    if (brdSection == null || siteSection == null) {
      differences.put(sectionName, "One of the sections is null");
      return builder.differences(differences).build();
    }

    Map<String, Boolean> sectionDifferences = new HashMap<>();
    compareFields(brdSection, siteSection, sectionDifferences);

    if (!sectionDifferences.isEmpty()) {
      differences.put(sectionName, sectionDifferences);
    }

    return builder.differences(differences).build();
  }

  private static Object getSection(Object obj, String sectionName) {
    try {
      return obj.getClass().getMethod("get" + capitalize(sectionName)).invoke(obj);
    } catch (Exception e) {
      return null;
    }
  }

  private static String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }

  private static void compareFields(
      Object brdObj, Object siteObj, Map<String, Boolean> differences) {
    BeanWrapper brdWrapper = new BeanWrapperImpl(brdObj);
    BeanWrapper siteWrapper = new BeanWrapperImpl(siteObj);

    for (java.beans.PropertyDescriptor pd : brdWrapper.getPropertyDescriptors()) {
      String propertyName = pd.getName();
      Object brdValue = brdWrapper.getPropertyValue(propertyName);
      Object siteValue = siteWrapper.getPropertyValue(propertyName);

      boolean hasDifference = false;

      if (brdValue == null && siteValue == null) {
        hasDifference = false;
      } else if (brdValue == null || siteValue == null) {
        hasDifference = true;
      } else if (isNestedObject(brdValue)) {
        Map<String, Boolean> nestedDifferences = new HashMap<>();
        compareFields(brdValue, siteValue, nestedDifferences);
        hasDifference =
            !nestedDifferences.isEmpty()
                && nestedDifferences.values().stream().anyMatch(Boolean::booleanValue);
      } else {
        hasDifference = !Objects.equals(brdValue, siteValue);
      }

      differences.put(propertyName, hasDifference);
    }
  }

  private static boolean isNestedObject(Object obj) {
    return obj.getClass()
        .getPackage()
        .getName()
        .startsWith("com.aci.smart_onboarding.util.brd_form");
  }
}
