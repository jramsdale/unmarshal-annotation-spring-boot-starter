package systems.misnomer.spring.unmarshal;

import java.time.Instant;
import java.time.LocalDateTime;

class DatetimeBean {

    private Instant epoch;

    private LocalDateTime localDateTime;

    public Instant getEpoch() {
        return epoch;
    }

    public void setEpoch(Instant epoch) {
        this.epoch = epoch;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }


}
