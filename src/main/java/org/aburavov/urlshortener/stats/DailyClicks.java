package org.aburavov.urlshortener.stats;

import java.time.LocalDate;

public interface DailyClicks {

    LocalDate getDay();

    long getClicks();
}
