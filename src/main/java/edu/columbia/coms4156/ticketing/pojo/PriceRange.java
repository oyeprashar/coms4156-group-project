
package edu.columbia.coms4156.ticketing.pojo;

import java.math.BigDecimal;

public record PriceRange(
  BigDecimal min,
  BigDecimal max,
  String currency) {
}
