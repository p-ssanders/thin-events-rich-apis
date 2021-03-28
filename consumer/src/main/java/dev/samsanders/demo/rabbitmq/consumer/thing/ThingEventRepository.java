package dev.samsanders.demo.rabbitmq.consumer.thing;

import org.springframework.data.repository.CrudRepository;

public interface ThingEventRepository extends CrudRepository<ThingEvent, Long> {

    Iterable<ThingEvent> findAllByConsumedInstantIsNullOrderByCreatedInstant();

}
