package com.aci.smart_onboarding.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class CustomPageImplTest {

  @Test
  void constructor_WithValidData_ShouldCreateInstance() {
    // Given
    List<String> content = Arrays.asList("Item1", "Item2");
    Pageable pageable = PageRequest.of(0, 2);
    long total = 5;
    boolean isFirst = true;
    boolean isLast = false;
    int numberOfElements = 2;
    int totalPages = 3;

    // When
    CustomPageImpl<String> page =
        new CustomPageImpl<>(
            content, pageable, total, isFirst, isLast, numberOfElements, totalPages);

    // Then
    assertEquals(content, page.getContent());
    assertEquals(pageable, page.getPageable());
    assertEquals(total, page.getTotalElements());
    assertTrue(page.isFirst());
    assertFalse(page.isLast());
    assertEquals(numberOfElements, page.getNumberOfElements());
    assertEquals(totalPages, page.getTotalPages());
  }

  @Test
  void firstPage_ShouldHaveCorrectProperties() {
    // Given
    List<String> content = Arrays.asList("Item1", "Item2");
    Pageable pageable = PageRequest.of(0, 2);

    // When
    CustomPageImpl<String> page = new CustomPageImpl<>(content, pageable, 5, true, false, 2, 3);

    // Then
    assertTrue(page.isFirst());
    assertFalse(page.isLast());
    assertEquals(0, page.getNumber());
    assertEquals(2, page.getSize());
  }

  @Test
  void lastPage_ShouldHaveCorrectProperties() {
    // Given
    List<String> content = Collections.singletonList("LastItem");
    Pageable pageable = PageRequest.of(2, 2);

    // When
    CustomPageImpl<String> page = new CustomPageImpl<>(content, pageable, 5, false, true, 1, 3);

    // Then
    assertFalse(page.isFirst());
    assertTrue(page.isLast());
    assertEquals(2, page.getNumber());
    assertEquals(2, page.getSize());
  }

  @Test
  void emptyPage_ShouldHaveCorrectProperties() {
    // Given
    List<String> content = Collections.emptyList();
    Pageable pageable = PageRequest.of(0, 10);

    // When
    CustomPageImpl<String> page = new CustomPageImpl<>(content, pageable, 0, true, true, 0, 0);

    // Then
    assertTrue(page.isEmpty());
    assertEquals(0, page.getNumberOfElements());
    assertEquals(0, page.getTotalElements());
    assertEquals(0, page.getTotalPages());
  }

  @Test
  void equals_WithSameData_ShouldBeEqual() {
    // Given
    List<String> content = Arrays.asList("Item1", "Item2");
    Pageable pageable = PageRequest.of(0, 2);

    CustomPageImpl<String> page1 = new CustomPageImpl<>(content, pageable, 5, true, false, 2, 3);
    CustomPageImpl<String> page2 = new CustomPageImpl<>(content, pageable, 5, true, false, 2, 3);

    // Then
    assertEquals(page1, page2);
    assertEquals(page1.hashCode(), page2.hashCode());
  }

  @Test
  void equals_WithDifferentData_ShouldNotBeEqual() {
    // Given
    List<String> content1 = Arrays.asList("Item1", "Item2");
    List<String> content2 = Arrays.asList("Item3", "Item4");
    Pageable pageable = PageRequest.of(0, 2);

    CustomPageImpl<String> page1 = new CustomPageImpl<>(content1, pageable, 5, true, false, 2, 3);
    CustomPageImpl<String> page2 = new CustomPageImpl<>(content2, pageable, 5, false, true, 2, 3);

    // Then
    assertNotEquals(page1, page2);
    assertNotEquals(page1.hashCode(), page2.hashCode());
  }

  @Test
  void withDifferentTypes_ShouldWorkCorrectly() {
    // Given
    List<Integer> content = Arrays.asList(1, 2, 3);
    Pageable pageable = PageRequest.of(0, 3);

    // When
    CustomPageImpl<Integer> page = new CustomPageImpl<>(content, pageable, 5, true, false, 3, 2);

    // Then
    assertEquals(content, page.getContent());
    assertEquals(3, page.getNumberOfElements());
    assertEquals(2, page.getTotalPages());
  }

  @Test
  void middlePage_ShouldHaveCorrectProperties() {
    // Given
    List<String> content = Arrays.asList("Item3", "Item4");
    Pageable pageable = PageRequest.of(1, 2);

    // When
    CustomPageImpl<String> page = new CustomPageImpl<>(content, pageable, 6, false, false, 2, 3);

    // Then
    assertFalse(page.isFirst());
    assertFalse(page.isLast());
    assertEquals(1, page.getNumber());
    assertEquals(2, page.getSize());
    assertEquals(2, page.getNumberOfElements());
    assertEquals(3, page.getTotalPages());
  }
}
