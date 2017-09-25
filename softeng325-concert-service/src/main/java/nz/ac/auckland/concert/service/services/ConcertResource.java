package nz.ac.auckland.concert.service.services;

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
import javax.ws.rs.NotAuthorizedException;
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
import nz.ac.auckland.concert.common.util.Config;
import nz.ac.auckland.concert.service.domain.AuthenticationToken;
import nz.ac.auckland.concert.service.domain.Booking;
import nz.ac.auckland.concert.service.domain.Concert;
import nz.ac.auckland.concert.service.domain.CreditCard;
import nz.ac.auckland.concert.service.domain.Performer;
import nz.ac.auckland.concert.service.domain.User;
import nz.ac.auckland.concert.service.util.TheatreUtility;

/**
 * Class to implement a simple REST Web service for managing Concerts, Performers, Users and Bookings.
 *
 */
@Path("/concerts")
public class ConcertResource {
	public static final int LOCK_TIMEOUT_MILLISECONDS = 5000;

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
				
		// Return all the concerts
		GenericEntity<Set<ConcertDTO>> entity = new GenericEntity<Set<ConcertDTO>>(DomainMapper.concertsToDTO(concerts)){};
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

		// Return all the performers
		GenericEntity<Set<PerformerDTO>> entity = new GenericEntity<Set<PerformerDTO>>(DomainMapper.performersToDTO(performers)){};
		builder = Response.ok(entity);

		return builder.build();
	}

	/**
	 * Creates a new User
	 * 
	 * @return a Response object containing the created User and an authentication token
	 */
	@POST
	@Path("/signup")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	@Produces(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response createUser(UserDTO userDTO) {
		ResponseBuilder builder = null;
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		// user name taken
		if(em.find(User.class, userDTO.getUsername()) != null){
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.CREATE_USER_WITH_NON_UNIQUE_NAME);
			throw new BadRequestException(builder.build());			
		}
		
		// persist the created user into the database
		User user = DomainMapper.userToDomainModel(userDTO);
		em.persist(user);
		
		// Commit the transaction.
		em.getTransaction().commit();

		// return the UserDTO
		builder = Response.ok(DomainMapper.userToDTO(user));
		// with an authentication token
		builder.cookie(makeToken(user));

		return builder.build();
	}
	
	/**
	 * Authenticates a User
	 * 
	 * @return a Response object containing the authentication token
	 */
	@POST
	@Path("/login")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	@Produces(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response authenticateUser(UserDTO userDTO) {
		ResponseBuilder builder = null;
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		User user = em.find(User.class, userDTO.getUsername());
		
		// user not found using authentication token
		if(user == null){
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.AUTHENTICATE_NON_EXISTENT_USER);
			throw new BadRequestException(builder.build());
		// CORRECT password, return authentication token
		} else if (user.getPassword().equals(userDTO.getPassword())){
			// convert and return the user from domain which has all properties set
			builder = Response.ok(DomainMapper.userToDTO(user));
			builder.cookie(makeToken(user));
		// wrong password
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
	public Response registerCreditCard(CreditCardDTO creditCardDTO, @CookieParam(Config.CLIENT_COOKIE) String token) {
		ResponseBuilder builder = null;
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		// check if authentication token exists in database
		AuthenticationToken authenticationToken = em.find(AuthenticationToken.class, token);
		handlePossibleUnrecognisedToken(authenticationToken);
		// then returns the associated user
		User user = authenticationToken.getUser();
		
		// token identifies a user, so proceed to adding credit card information to user 
		CreditCard creditCard = DomainMapper.creditCardToDomainModel(creditCardDTO);
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
	public Response retrieveBookings(@CookieParam(Config.CLIENT_COOKIE) String token) {
		ResponseBuilder builder = null;
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		// check if authentication token exists in database
		AuthenticationToken authenticationToken = em.find(AuthenticationToken.class, token);
		handlePossibleUnrecognisedToken(authenticationToken);
		// then returns the associated user
		User user = authenticationToken.getUser();
		
		
		// Use the EntityManager to retrieve all Bookings with the same username.
		TypedQuery<Booking> bookingQuery = 
				em.createQuery("select b from Booking b "
						+ "left join fetch b._seats " //TODO check if i did this properly
						+ "where b._user._username = :username", Booking.class)
				.setParameter("username", user.getUsername());
		List<Booking> bookings = bookingQuery.getResultList();
		
		// Return the list of bookings
		GenericEntity<Set<BookingDTO>> entity = new GenericEntity<Set<BookingDTO>>(DomainMapper.bookingsToDTO(bookings)){};
		
		builder = Response.ok(entity);

		return builder.build();
	}
	
	
	/**
	 * Reserves seats in a concert after checking for availability
	 * 	 
	 * @return a Response object containing the reservation
	 */
	@POST
	@Path("/reservations")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response reserveSeats(ReservationRequestDTO reservationRequestDTO, @CookieParam(Config.CLIENT_COOKIE) String token) {
		ResponseBuilder builder = null;

		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		// check if authentication token exists in database
		AuthenticationToken authenticationToken = em.find(AuthenticationToken.class, token);
		handlePossibleUnrecognisedToken(authenticationToken);
		// then returns the associated user
		User user = authenticationToken.getUser();
		
		// Get the Concert
		Concert concert = em.find(Concert.class, reservationRequestDTO.getConcertId());

		// if concert not on that date or concert doesn't exist
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
						+ "where "
						+ "b._concert._id = :id "
						+ "and "
						+ "b._dateTime = :date "
					    + "and "
						+ "b._priceBand = :type"
						, Booking.class)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.setHint("javax.persistence.lock.timeout", LOCK_TIMEOUT_MILLISECONDS)
				.setParameter("id", reservationRequestDTO.getConcertId())
				.setParameter("date", reservationRequestDTO.getDate())
				.setParameter("type", reservationRequestDTO.getSeatType())
				;
		
		List<Booking> bookings = bookingQuery.getResultList();		
		
		// get all booked seats
		Set<SeatDTO> bookedSeats = new HashSet<SeatDTO>();
		for(Booking b : bookings){
			bookedSeats.addAll(DomainMapper.seatsToDTO(b.getSeats()));
		}
		
		// randomly select seats for reservation
		Set<SeatDTO> reservationSeatDTOS = 
				TheatreUtility.findAvailableSeats(reservationRequestDTO.getNumberOfSeats(), 
						reservationRequestDTO.getSeatType(), bookedSeats);
		
		// Stores the unconfirmed Booking
		Booking unconfirmedBooking;
		
		// if seats are returned, searching for seats has been successful
		if(reservationSeatDTOS.size() != 0){
			unconfirmedBooking = new Booking(concert,
					reservationRequestDTO.getDate(), DomainMapper.seatsToDomainModel(reservationSeatDTOS), 
					reservationRequestDTO.getSeatType(), user);
			
			// persist the unconfirmed Booking
			em.persist(unconfirmedBooking);
			
			// Commit the transaction and get rid of locks
			em.getTransaction().commit();
		} else {
			// Commit the transaction and get rid of locks
			em.getTransaction().commit();
			// insufficient seats
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.INSUFFICIENT_SEATS_AVAILABLE_FOR_RESERVATION);
			throw new BadRequestException(builder.build());
		}
		
		// prepare to return reservation
		builder = Response.ok(new ReservationDTO(unconfirmedBooking.getId(), reservationRequestDTO, reservationSeatDTOS));
		
		// close entity manager
		em.close();
				
		// Run a separate thread to remove the booking if still unconfirmed at expiry time
		new Thread(){
			public void run(){
				// wait till expiry time
				try {
					Thread.sleep(ConcertApplication.RESERVATION_EXPIRY_TIME_IN_SECONDS*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				// Acquire an EntityManager (creating a new persistence context).
				EntityManager em = PersistenceManager.instance().createEntityManager();
				// Start a new transaction.
				em.getTransaction().begin();				
				em.setProperty("javax.persistence.lock.timeout", LOCK_TIMEOUT_MILLISECONDS);
				Booking bookingToConfirm = em.find(Booking.class, unconfirmedBooking.getId(), LockModeType.PESSIMISTIC_WRITE);
				// remove booking if still unconfirmed AND 
				if(bookingToConfirm != null && bookingToConfirm.getConfirmationStatus() == false){
					em.remove(bookingToConfirm);
				}
				
				// Commit the transaction and get rid of locks
				em.getTransaction().commit();
				em.close();
			}
		}.start();
		
		// return the reservation after starting the separate thread
		return builder.build();
	}
	
	/**
	 * Confirm reservation
	 */
	@POST
	@Path("/reservations/confirmation")
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response confirmReservation(ReservationDTO reservationDTO, @CookieParam(Config.CLIENT_COOKIE) String token) {
		ResponseBuilder builder = null;
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		
		// check if authentication token exists in database
		AuthenticationToken authenticationToken = em.find(AuthenticationToken.class, token);
		handlePossibleUnrecognisedToken(authenticationToken);
		// then returns the associated user
		User user = authenticationToken.getUser();
				
		em.setProperty("javax.persistence.lock.timeout", LOCK_TIMEOUT_MILLISECONDS);
		Booking bookingToConfirm = em.find(Booking.class, reservationDTO.getId(), LockModeType.PESSIMISTIC_WRITE);
		
		if(user.getCreditCards().size() == 0){
			
			// delete booking if it still exists and if credit card is not registered
			if(bookingToConfirm != null && bookingToConfirm.getUser().equals(user)){
				em.remove(bookingToConfirm);
			} 
			
			// commit and release locks
			em.getTransaction().commit();
			
			builder = Response.status(Status.BAD_REQUEST).entity(Messages.CREDIT_CARD_NOT_REGISTERED);
			throw new BadRequestException(builder.build());
		}
		
		// if booking associated with the user cannot be found, that means the booking has expired
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

	/**
	 * BAD_AUTHENTICATON_TOKEN
	 * Check if authentication token is recognised (associated with a user) and 
	 * respond accordingly if not (401 Unauthorized)
	 */
	private void handlePossibleUnrecognisedToken(AuthenticationToken token){
		if(token == null){
			throw new NotAuthorizedException(Response.status(Status.UNAUTHORIZED).entity(Messages.BAD_AUTHENTICATON_TOKEN).build());
		}
	}
	
	/**
	 * Make a new token (cookie) for a specific user
	 */
	private NewCookie makeToken(User user){
		NewCookie newCookie = null;
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		// Try to find if there is already a token associated with the user
		List<AuthenticationToken> tokens = 
				em.createQuery("select a from AuthenticationToken a where a._user = :user", AuthenticationToken.class)
				.setParameter("user", user)
				.getResultList();
		
		AuthenticationToken token = null;
		
		// if there isn't create a new one
		if(tokens.size() == 0){
			token = new AuthenticationToken(user);
			em.persist(token);
		// if there is one, return it
		} else if (tokens.size() == 1){
			token = tokens.get(0);
		}
		
		em.getTransaction().commit();
		em.close();
		
		newCookie = new NewCookie(Config.CLIENT_COOKIE, token.getValue());
		_logger.info("Generated cookie: " + newCookie.getValue());
		
		return newCookie;
	}
	
}
