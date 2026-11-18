package transfers.core;

import java.math.BigDecimal;

/** Exchange rates, in recipient currency units per US dollar. */
public interface RateSource {
    BigDecimal unitsPerUsd(String currency);
}
