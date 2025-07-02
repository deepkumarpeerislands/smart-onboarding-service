package com.aci.smart_onboarding.dto;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Getter
public class CustomPageImpl<T> extends PageImpl<T> {
  private final boolean isFirst;
  private final boolean isLast;
  private final int numberOfElements;
  private final int totalPages;

  public CustomPageImpl(
      List<T> content,
      Pageable pageable,
      long total,
      boolean isFirst,
      boolean isLast,
      int numberOfElements,
      int totalPages) {
    super(content, pageable, total);
    this.isFirst = isFirst;
    this.isLast = isLast;
    this.numberOfElements = numberOfElements;
    this.totalPages = totalPages;
  }

  @Override
  public boolean isFirst() {
    return isFirst;
  }

  @Override
  public boolean isLast() {
    return isLast;
  }

  @Override
  public int getNumberOfElements() {
    return numberOfElements;
  }

  @Override
  public int getTotalPages() {
    return totalPages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CustomPageImpl)) return false;
    if (!super.equals(o)) return false;

    CustomPageImpl<?> that = (CustomPageImpl<?>) o;
    return isFirst == that.isFirst
        && isLast == that.isLast
        && numberOfElements == that.numberOfElements
        && totalPages == that.totalPages;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), isFirst, isLast, numberOfElements, totalPages);
  }
}
