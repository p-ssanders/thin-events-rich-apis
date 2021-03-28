package dev.samsanders.demo.rabbitmq.publisher.thing;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;
import java.util.Objects;

@Entity
public class ThingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long thingId;
    private Instant createdInstant;
    private Instant publishedInstant;

    public ThingEvent() {
    }

    public ThingEvent(long thingId) {
        this.thingId = thingId;
        this.createdInstant = Instant.now();
    }

    public long getId() {
        return id;
    }

    public long getThingId() {
        return thingId;
    }

    public Instant getCreatedInstant() {
        return createdInstant;
    }

    public Instant getPublishedInstant() {
        return publishedInstant;
    }

    public void setPublishedInstant(Instant publishedInstant) {
        this.publishedInstant = publishedInstant;
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
                thingId == that.thingId &&
                Objects.equals(createdInstant, that.createdInstant) &&
                Objects.equals(publishedInstant, that.publishedInstant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, thingId, createdInstant, publishedInstant);
    }

    @Override
    public String toString() {
        return "ThingEvent{" +
                "id=" + id +
                ", thingId=" + thingId +
                ", createdInstant=" + createdInstant +
                ", publishedInstant=" + publishedInstant +
                '}';
    }
}
