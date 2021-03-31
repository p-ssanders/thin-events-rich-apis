package dev.samsanders.demo.rabbitmq.publisher.thing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.AbstractAggregateRoot;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Entity
public class Thing extends AbstractAggregateRoot<Thing> {

    private static final AtomicInteger idGenerator = new AtomicInteger();

    @Id
    private long id;
    private String content;

    public Thing() {
    }

    @JsonCreator
    public Thing(@JsonProperty("content") String content) {
        this.id = idGenerator.getAndIncrement();
        this.content = content;
        this.registerEvent(new ThingEvent(this.getId()));
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Thing)) {
            return false;
        }
        Thing thing = (Thing) o;
        return id == thing.id &&
                Objects.equals(content, thing.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content);
    }

    @Override
    public String toString() {
        return "Thing{" +
                "id=" + id +
                ", content='" + content + '\'' +
                '}';
    }
}
