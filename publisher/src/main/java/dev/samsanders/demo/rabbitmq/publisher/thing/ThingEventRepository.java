package dev.samsanders.demo.rabbitmq.publisher.thing;

import org.springframework.data.repository.CrudRepository;

public interface ThingEventRepository extends CrudRepository<ThingEvent, Long> {

    Iterable<ThingEvent> findAllByPublishedInstantIsNullOrderByCreatedInstant();

    Iterable<ThingEvent> findAllByThingId(long thingId);
}
