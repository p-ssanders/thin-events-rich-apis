package dev.samsanders.demo.rabbitmq.consumer.thing;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@Entity
public class ThingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private URI thingUri;
    private Instant createdInstant;
    private Instant consumedInstant;

    public ThingEvent() {
    }

    public ThingEvent(URI thingUri, Instant createdInstant) {
        this.thingUri = thingUri;
        this.createdInstant = createdInstant;
    }

    public long getId() {
        return id;
    }

    public URI getThingUri() {
        return thingUri;
    }

    public Instant getCreatedInstant() {
        return createdInstant;
    }

    public Instant getConsumedInstant() {
        return consumedInstant;
    }

    public void setConsumedInstant(Instant consumedInstant) {
        this.consumedInstant = consumedInstant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ThingEvent)) {
            return false;
        }
        ThingEvent that = (ThingEvent) o;
        return id == that.id &&
                Objects.equals(thingUri, that.thingUri) &&
                Objects.equals(createdInstant, that.createdInstant) &&
                Objects.equals(consumedInstant, that.consumedInstant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, thingUri, createdInstant, consumedInstant);
    }

    @Override
    public String toString() {
        return "ThingEvent{" +
                "id=" + id +
                ", thingUri=" + thingUri +
                ", createdInstant=" + createdInstant +
                ", consumedInstant=" + consumedInstant +
                '}';
    }
}
