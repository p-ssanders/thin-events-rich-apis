package dev.samsanders.demo.rabbitmq.publisher.thing;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/things")
public class ThingController {

    private final ThingRepository thingRepository;
    private final ThingEventRepository thingEventRepository;

    ThingController(ThingRepository thingRepository, ThingEventRepository thingEventRepository) {
        this.thingRepository = thingRepository;
        this.thingEventRepository = thingEventRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> create(@RequestBody Thing thing) {
        thing = thingRepository.save(thing);

        thingEventRepository.save(new ThingEvent(thing.getId()));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(thing.getId()).toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping
    public ResponseEntity<Collection<Thing>> readAll() {
        Iterable<Thing> all = thingRepository.findAll();

        ArrayList<Thing> things = new ArrayList<>();
        all.forEach(things::add);

        return ResponseEntity.ok(things);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Thing> read(@PathVariable Long id) {
        Optional<Thing> optionalThing = thingRepository.findById(id);

        if (optionalThing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(optionalThing.get());
    }

}
