package com.ftn.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RenewalRequest(
    LocalDate newEndDate,
    BigDecimal newValue
) {}
