package edu.columbia.coms4156.ticketing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies the Mockito agent and Jupiter extension on each supported JDK. */
@ExtendWith(MockitoExtension.class)
class MockitoConfigurationTests {

  // LocalDate is final, so this also exercises the inline mock maker.
  @Mock private LocalDate date;

  @Test
  void finalClassesCanBeMocked() {
    when(date.getYear()).thenReturn(2026);

    assertEquals(2026, date.getYear());
    verify(date).getYear();
  }
}
