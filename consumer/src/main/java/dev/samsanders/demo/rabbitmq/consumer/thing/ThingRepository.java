package dev.samsanders.demo.rabbitmq.consumer.thing;

import org.springframework.data.repository.CrudRepository;

public interface ThingRepository extends CrudRepository<Thing, Long> {

}
