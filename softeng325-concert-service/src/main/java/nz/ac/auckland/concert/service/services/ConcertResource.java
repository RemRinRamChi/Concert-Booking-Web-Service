package nz.ac.auckland.concert.service.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import nz.ac.auckland.concert.common.dto.CreditCardDTO;
import nz.ac.auckland.concert.common.dto.PerformerDTO;
import nz.ac.auckland.concert.common.dto.ReservationDTO;
import nz.ac.auckland.concert.common.dto.ReservationRequestDTO;
import nz.ac.auckland.concert.common.dto.SeatDTO;
import nz.ac.auckland.concert.common.dto.UserDTO;
import nz.ac.auckland.concert.common.message.Messages;
import nz.ac.auckland.concert.service.domain.Booking;
import nz.ac.auckland.concert.service.domain.Concert;
import nz.ac.auckland.concert.service.domain.CreditCard;
import nz.ac.auckland.concert.service.domain.Performer;
import nz.ac.auckland.concert.service.domain.User;
import nz.ac.auckland.concert.service.util.TheatreUtility;

/**
 * Class to implement a simple REST Web service for managing Concerts.
 *
 */

@Path("/concerts")
public class ConcertResource {
	/**
	 * Name of a cookie exchanged by clients and the Web service.
	 */
	public static final String CLIENT_COOKIE = "clientId";

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
	 * Creates a new User.
	 */
	@POST
	@Path("/signup")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	@Produces(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response createUser(UserDTO userDTO) {
		ResponseBuilder builder = null;

		// fields missing //TODO can be done in client
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
		builder.cookie(makeCookie(user.getUsername()));

		return builder.build();
	}
	
	/**
	 * Authenticates a User
	 */
	@POST
	@Path("/login")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	@Produces(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response authenticateUser(UserDTO userDTO) {
		ResponseBuilder builder = null;

		// fields missing //TODO can be done in client
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
			builder.cookie(makeCookie(user.getUsername()));
		} else {
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.AUTHENTICATE_USER_WITH_ILLEGAL_PASSWORD);
			throw new BadRequestException(builder.build());	
		}

		return builder.build();
	}
	
	/**
	 * Registers a credit card with the user
	 */
	@POST
	@Path("/creditcards")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response registerCreditCard(CreditCardDTO creditCardDTO, @CookieParam(CLIENT_COOKIE) String token) {
		ResponseBuilder builder = null;

		handlePossibleUnauthenticatedRequest(token);
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		User user = em.find(User.class, token);
		
		handlePossibleUnrecognisedToken(user);
		
		// token identifies a user, so proceed to adding credit card information to user 
		CreditCard creditCard = DTOMapper.creditCardToDomainModel(creditCardDTO);
		user.addCreditCard(creditCard);
		
		// Commit the transaction.
		em.getTransaction().commit();
		
		builder = Response.status(Status.NO_CONTENT);
		
		return builder.build();
	}
	
	
	/**
	 * Retrieves all Bookings of a user
	 * 
	 * @return a Response object containing all the user's Bookings.
	 */
	@GET
	@Path("/bookings")
	@Produces(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response retrieveBookings(@CookieParam(CLIENT_COOKIE) String token) {
		ResponseBuilder builder = null;

		handlePossibleUnauthenticatedRequest(token);
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		User user = em.find(User.class, token);
		
		handlePossibleUnrecognisedToken(user);
		
		// Use the EntityManager to retrieve all Bookings with the same username.
		TypedQuery<Booking> bookingQuery = 
				em.createQuery("select b from Booking b where n.USER_ID = :username", Booking.class)
				.setParameter("username", user.getUsername());
		List<Booking> bookings = bookingQuery.getResultList();
		
		// Convert to DTOs
		List<BookingDTO> bookingDTOs = new ArrayList<BookingDTO>();
		for(Booking b : bookings){
			bookingDTOs.add(DTOMapper.bookingToDTO(b));
		}
		
		GenericEntity<List<BookingDTO>> entity = new GenericEntity<List<BookingDTO>>(bookingDTOs){};
		
		builder = Response.ok(entity);

		return builder.build();
	}
	
	
	/**
	 * Reserves seats
	 */
	@POST
	@Path("/reservations")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response reserveSeats(ReservationRequestDTO reservationRequestDTO, @CookieParam(CLIENT_COOKIE) String token) {
		ResponseBuilder builder = null;

		handlePossibleUnauthenticatedRequest(token);
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		User user = em.find(User.class, token);
		
		handlePossibleUnrecognisedToken(user);
		
		Concert concert = em.find(Concert.class, reservationRequestDTO.getConcertId());

		// if concert not on that date
		if((concert == null) || (! concert.getDates().contains(reservationRequestDTO.getDate()))){
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.CONCERT_NOT_SCHEDULED_ON_RESERVATION_DATE);
			throw new BadRequestException(builder.build());
		}
		
		// Concert, date and price brand classification is used to narrow down the 
		// number of locks to only those of the same price brand
		// Use the EntityManager to retrieve all Bookings with the same user name.
		TypedQuery<Booking> bookingQuery = 
				em.createQuery("select b from Booking b "
						+ "left join fetch b._seats "
						+ "where n.CONCERT_ID = :concert_id and "
						+ "n._dateTime = :date and "
						+ "n._priceBrand = :seat_type", Booking.class)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.setHint("javax.persistence.lock.timeout", 5000)
				.setParameter("concert_id", reservationRequestDTO.getConcertId())
				.setParameter("date", DTOMapper.convertToDatabaseColumn(reservationRequestDTO.getDate()))
				.setParameter("seat_type", reservationRequestDTO.getSeatType().toString());
		//TODO ^ check check
		
		List<Booking> bookings = bookingQuery.getResultList();		
		
		// get all booked seats
		Set<SeatDTO> bookedSeats = new HashSet<SeatDTO>();
		for(Booking b : bookings){
			bookedSeats.addAll(DTOMapper.seatsToDTO(b.getSeats()));
		}
		
		// get seats to reserve
		Set<SeatDTO> reservationSeatDTOS = 
				TheatreUtility.findAvailableSeats(reservationRequestDTO.getNumberOfSeats(), 
						reservationRequestDTO.getSeatType(), bookedSeats);
		
		final Booking unconfirmedBooking;
		
		if(reservationSeatDTOS.size() != 0){
			unconfirmedBooking = new Booking(em.find(Concert.class, reservationRequestDTO.getConcertId()),
					reservationRequestDTO.getDate(), DTOMapper.seatsToDomainModel(reservationSeatDTOS), 
					reservationRequestDTO.getSeatType(), user);
			
			em.persist(unconfirmedBooking);
			
			// Commit the transaction and get rid of locks
			em.getTransaction().commit();
		} else {
			em.getTransaction().commit();
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.INSUFFICIENT_SEATS_AVAILABLE_FOR_RESERVATION);
			throw new BadRequestException(builder.build());
		}
		
		builder = Response.ok(new ReservationDTO(unconfirmedBooking.getId(), reservationRequestDTO, reservationSeatDTOS));
		em.close();
		
		// Remove the booking if still unconfirmed at expiry time
		new Thread(){
			public void run(){
				try {
					Thread.sleep(ConcertApplication.RESERVATION_EXPIRY_TIME_IN_SECONDS*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// Acquire an EntityManager (creating a new persistence context).
				EntityManager em = PersistenceManager.instance().createEntityManager();
				// Start a new transaction.
				em.getTransaction().begin();				
				
				Booking bookingToConfirm = em.find(Booking.class, unconfirmedBooking.getId(), LockModeType.PESSIMISTIC_WRITE);
				if(bookingToConfirm.getConfirmationStatus() == false){
					em.remove(bookingToConfirm);
				}
				// Commit the transaction and get rid of locks
				em.getTransaction().commit();
				em.close();
			}
		}.start();
		
		return builder.build();
	}
	
	/**
	 * Confirm reservation
	 */
	@POST
	@Path("/reservations/confirmation")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response confirmReservation(ReservationDTO reservationDTO, @CookieParam(CLIENT_COOKIE) String token) {
		ResponseBuilder builder = null;

		handlePossibleUnauthenticatedRequest(token);
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		User user = em.find(User.class, token);
		
		handlePossibleUnrecognisedToken(user);
				
		if(user.getCreditCards().size() == 0){
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.CREDIT_CARD_NOT_REGISTERED);
			throw new BadRequestException(builder.build());
		}
		
		Booking bookingToConfirm = em.find(Booking.class, reservationDTO.getId(), LockModeType.PESSIMISTIC_WRITE);
		
		if(bookingToConfirm == null || !bookingToConfirm.getUser().equals(user)){
			em.getTransaction().commit();
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.EXPIRED_RESERVATION);
			throw new BadRequestException(builder.build());			
		} else {
			bookingToConfirm.setConfirmed();
			em.getTransaction().commit();
		}
		
		builder = Response.status(Status.NO_CONTENT);
		
		return builder.build();
	}
	
	
	private void handlePossibleUnauthenticatedRequest(String token){
		if(token == null){
			throw new BadRequestException(Response.status(Status.UNAUTHORIZED).entity(Messages.UNAUTHENTICATED_REQUEST).build());
		}
	}
	
	private void handlePossibleUnrecognisedToken(User user){
		if(user == null){
			throw new BadRequestException(Response.status(Status.UNAUTHORIZED).entity(Messages.BAD_AUTHENTICATON_TOKEN).build());
		}
	}
	
	/**
	 * Make a new token (cookie) using the user's username and password
	 * @param username
	 * @param password
	 * @return token
	 */
	private NewCookie makeCookie(String username){
		NewCookie newCookie = null;
	
		newCookie = new NewCookie(CLIENT_COOKIE, username);
		_logger.info("Generated cookie: " + newCookie.getValue());
		
		return newCookie;
	}
	
}
