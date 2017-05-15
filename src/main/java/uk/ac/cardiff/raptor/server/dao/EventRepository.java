package uk.ac.cardiff.raptor.server.dao;

import org.springframework.data.repository.CrudRepository;

import uk.ac.cardiff.model.event.Event;

/**
 * Specialisation of a {@link CrudRepository} for {@link Event} types.
 * 
 * @author philsmart
 *
 */
public interface EventRepository extends CrudRepository<Event, Integer> {

}
