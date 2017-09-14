package nz.ac.auckland.concert.service.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nz.ac.auckland.concert.common.dto.BookingDTO;
import nz.ac.auckland.concert.common.dto.ConcertDTO;
import nz.ac.auckland.concert.common.dto.PerformerDTO;
import nz.ac.auckland.concert.common.dto.UserDTO;
import nz.ac.auckland.concert.common.message.Messages;
import nz.ac.auckland.concert.service.domain.Booking;
import nz.ac.auckland.concert.service.domain.Concert;
import nz.ac.auckland.concert.service.domain.Performer;
import nz.ac.auckland.concert.service.domain.User;

/**
 * Class to implement a simple REST Web service for managing Concerts.
 *
 */

@Path("/concerts")
public class ConcertResource {


	private static Logger _logger = LoggerFactory
			.getLogger(ConcertResource.class);
	
	/**
	 * Retrieves all Concerts
	 * 
	 * @return a Response object containing all the Concerts.
	 */
	@GET
	@Produces(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response retrieveConcerts() {
		ResponseBuilder builder = null;

		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();

		// Use the EntityManager to retrieve all Concerts.
		TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c", Concert.class);
		List<Concert> concerts = concertQuery.getResultList();

		List<ConcertDTO> concertDTOs = new ArrayList<ConcertDTO>();
		
		for(Concert c : concerts){
			concertDTOs.add(DTOMapper.concertToDTO(c));
		}
		
		GenericEntity<List<ConcertDTO>> entity = new GenericEntity<List<ConcertDTO>>(concertDTOs){};
		
		builder = Response.ok(entity);

		return builder.build();
	}

	/**
	 * Retrieves all Performers
	 * 
	 * @return a Response object containing all the Performers.
	 */
	@GET
	@Path("/performers")
	@Produces(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response retrievePerformers() {
		ResponseBuilder builder = null;

		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();

		// Use the EntityManager to retrieve all Performers.
		TypedQuery<Performer> performerQuery = em.createQuery("select p from Performer p", Performer.class);
		List<Performer> performers = performerQuery.getResultList();

		List<PerformerDTO> performerDTOs = new ArrayList<PerformerDTO>();
		
		for(Performer p : performers){
			performerDTOs.add(DTOMapper.performerToDTO(p));
		}
		
		GenericEntity<List<PerformerDTO>> entity = new GenericEntity<List<PerformerDTO>>(performerDTOs){};
		
		builder = Response.ok(entity);

		return builder.build();
	}

	/**
	 * Retrieves all Bookings of a user
	 * 
	 * @return a Response object containing all the user's Bookings.
	 */
	@GET
	@Path("/bookings/{id}")
	@Produces(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response retrieveBookings(@PathParam("id") String id) {
		ResponseBuilder builder = null;

		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();

		// Use the EntityManager to retrieve all Bookings for the user.
		TypedQuery<Booking> bookingQuery = em.createQuery("select b from Booking b", Booking.class);
		List<Booking> bookings = bookingQuery.getResultList();

		List<BookingDTO> bookingDTOs = new ArrayList<BookingDTO>();
		
		for(Booking b : bookings){
			if(b.getUsername().equals(id)){
				bookingDTOs.add(DTOMapper.bookingToDTO(b));
			}
		}
		
		GenericEntity<List<BookingDTO>> entity = new GenericEntity<List<BookingDTO>>(bookingDTOs){};
		
		builder = Response.ok(entity);

		return builder.build();
	}
	
	/**
	 * Creates a new User.
	 */
	@POST
	@Path("/users/signup")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response createUser(UserDTO userDTO) {
		ResponseBuilder builder = null;

		// fields missing
		if(userDTO.getFirstname() == null || userDTO.getLastname() == null || userDTO.getUsername() == null || userDTO.getPassword() == null ){
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.CREATE_USER_WITH_MISSING_FIELDS);
			throw new BadRequestException(builder.build());
		}
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		// user name taken
		if(em.find(User.class, userDTO.getUsername()) != null){ //TODO dk if bad request
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.CREATE_USER_WITH_NON_UNIQUE_NAME);
			throw new BadRequestException(builder.build());			
		}
		
		// convert to domain object
		User user = DTOMapper.userToDomainModel(userDTO);
		// Use the EntityManager to persist object.
		em.persist(user);
		// Commit the transaction.
		em.getTransaction().commit();

		// return the UserDTO
		builder = Response.ok(DTOMapper.userToDTO(user));
		// with token
		builder.cookie(makeCookie(user.getUsername(), user.getPassword()));

		return builder.build();
	}
	
	/**
	 * Authenticates a User
	 */
	@POST
	@Path("/users/signin")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response authenticateUser(UserDTO userDTO) {
		ResponseBuilder builder = null;

		// fields missing
		if(userDTO.getFirstname() == null || userDTO.getLastname() == null || userDTO.getUsername() == null || userDTO.getPassword() == null ){
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.AUTHENTICATE_USER_WITH_MISSING_FIELDS);
			throw new BadRequestException(builder.build());
		}
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		User user = em.find(User.class, userDTO.getUsername());
		
		if(user == null){
			builder = Response.status(Status.NOT_FOUND).entity(Messages.AUTHENTICATE_NON_EXISTENT_USER);
			throw new BadRequestException(builder.build());	
		} else if (user.getPassword().equals(userDTO.getPassword())){
			builder = Response.ok(DTOMapper.userToDTO(user));
			builder.cookie(makeCookie(user.getUsername(), user.getPassword()));
		} else {
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.AUTHENTICATE_USER_WITH_ILLEGAL_PASSWORD);
			throw new BadRequestException(builder.build());	
		}

		return builder.build();
	}
	
	/**
	 * Creates a new Concert. This method assigns an ID to the new Concert and
	 * stores it in memory. The HTTP Response message returns a Location header
	 * with the URI of the new Concert and a status code of 201.
	 * 
	 * This method maps to the URI pattern <base-uri>/concerts.
	 * 
	 * @param concert
	 *            the new Concert to create.
	 * 
	 * @return a Response object containing the status code 201 and a Location
	 *         header.
	 */
	@POST
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response createConcert(Concert concert) {
		ResponseBuilder builder = null;

		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		// Use the EntityManager to persist object.
		em.persist(concert);
		long id = concert.getId();
		// Commit the transaction.
		em.getTransaction().commit();

		builder = Response.created(URI.create("/concerts/" + id));

		return builder.build();
	}

	/**
	 * Updates an existing Concert. The HTTP Response message returns a status
	 * code of 204 or 404.
	 * 
	 * This method maps to the URI pattern <base-uri>/concerts.
	 * 
	 * @param concert
	 *            the new Concert to create.
	 * 
	 * @return a Response object containing the status code 201 and a Location
	 *         header.
	 */
	@PUT
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response updateConcert(Concert concert) {
		ResponseBuilder builder = null;

		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		// Use the EntityManager to update Concert.
		if (em.find(Concert.class, concert.getId()) != null) {
			em.merge(concert);
			builder = Response.status(Response.Status.NO_CONTENT);
		} else {
			builder = Response.status(Response.Status.NOT_FOUND);
		}
		// Commit the transaction.
		em.getTransaction().commit();

		return builder.build();
	}

	/**
	 * Deletes a single Concert, returning a status code of 204 if the Concert
	 * exists and 404 if it doesn't
	 * 
	 * This method maps to the URI pattern <base-uri>/concerts/{id}.
	 * 
	 * @return a Response object containing the status code 204 or 404.
	 */
	@DELETE
	@Path("{id}")
	public Response deleteConcert(@PathParam("id") long id) {
		ResponseBuilder builder = null;

		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		// Use the EntityManager to delete object.
		Concert concert = em.find(Concert.class, id);
		if (concert != null) { // found
			em.remove(concert);
			builder = Response.status(Response.Status.NO_CONTENT);
		} else { // not found
			builder = Response.status(Response.Status.NOT_FOUND);
		}
		// Commit the transaction.
		em.getTransaction().commit();

		return builder.build();
	}

	/**
	 * Deletes all Concerts, returning a status code of 204.
	 * 
	 * When clientId is null, the HTTP request message doesn't contain a cookie
	 * named clientId (Config.CLIENT_COOKIE), this method generates a new
	 * cookie, whose value is a randomly generated UUID. This method returns the
	 * new cookie as part of the HTTP response message.
	 * 
	 * This method maps to the URI pattern <base-uri>/concerts.
	 * 
	 * @param clientId
	 *            a cookie named Config.CLIENT_COOKIE that may be sent by the
	 *            client.
	 * 
	 * @return a Response object containing the status code 204.
	 */
	@DELETE
	public Response deleteAllConcerts() {
		ResponseBuilder builder = null;

		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		// Use the EntityManager to retrieve all Concerts, then delete them all.
		TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c", Concert.class);
		List<Concert> concerts = concertQuery.getResultList();
		for (Concert c : concerts) {
			em.remove(c);

		}
		// Commit the transaction.
		em.getTransaction().commit();

		builder = Response.status(Response.Status.NO_CONTENT);

		return builder.build();
	}
	
	
	private NewCookie makeCookie(String username, String password){
		NewCookie newCookie = null;
	
		newCookie = new NewCookie(username, password);
		_logger.info("Generated cookie: " + newCookie.getValue());
		
		return newCookie;
	}
	
}
