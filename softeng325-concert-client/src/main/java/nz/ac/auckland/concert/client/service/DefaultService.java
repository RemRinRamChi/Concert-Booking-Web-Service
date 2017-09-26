package nz.ac.auckland.concert.client.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import nz.ac.auckland.concert.common.dto.BookingDTO;
import nz.ac.auckland.concert.common.dto.ConcertDTO;
import nz.ac.auckland.concert.common.dto.CreditCardDTO;
import nz.ac.auckland.concert.common.dto.NewsItemDTO;
import nz.ac.auckland.concert.common.dto.PerformerDTO;
import nz.ac.auckland.concert.common.dto.ReservationDTO;
import nz.ac.auckland.concert.common.dto.ReservationRequestDTO;
import nz.ac.auckland.concert.common.dto.UserDTO;
import nz.ac.auckland.concert.common.message.Messages;
import nz.ac.auckland.concert.common.util.Config;

/**
 * REST Web service client that implements ConcertService. 
 *
 */
public class DefaultService implements ConcertService {

	private static Logger _logger = LoggerFactory
			.getLogger(DefaultService.class);
	
	// AWS S3 access credentials for concert images.
	private static final String AWS_ACCESS_KEY_ID = "AKIAIDYKYWWUZ65WGNJA";
	private static final String AWS_SECRET_ACCESS_KEY = "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";

	// Name of the S3 bucket that stores images.
	private static final String AWS_BUCKET = "concert.aucklanduni.ac.nz";

	private String WEB_SERVICE_URI = "http://localhost:10000/services/concerts";

	private AmazonS3 s3;

	private List<String> imageNames;

	private Cookie authenticationToken;

	private String subscriptionId;
	
	private boolean subscribe;

	public DefaultService() {		
		// Initially unauthenticated
		authenticationToken = null;

		// Initially no registered for subscription
		subscriptionId = null;
		subscribe = false;
	}

	@Override
	public Set<ConcertDTO> getConcerts() throws ServiceException {
		Client client = ClientBuilder.newClient();
		Response response = null;
		Set<ConcertDTO> concerts = null;

		try {
			// Make a get request for all concerts
			Builder builder = client.target(WEB_SERVICE_URI).request().accept(MediaType.APPLICATION_XML);

			response = builder.get();

			handlePossibleServiceCommunicationError(response);
			
			concerts = response.readEntity(new GenericType<Set<ConcertDTO>>() {
			});

		} catch(ProcessingException e){
			handleServiceCommunicationError();
		} finally {
			// Close the Response object.
			response.close();
			client.close();
		}

		// return the acquired response
		return concerts;
	}

	@Override
	public Set<PerformerDTO> getPerformers() throws ServiceException {
		Client client = ClientBuilder.newClient();
		Response response = null;
		Set<PerformerDTO> performers = null;

		try {
			// Make a get request for all performers
			Builder builder = client.target(WEB_SERVICE_URI + "/performers").request()
					.accept(MediaType.APPLICATION_XML);

			response = builder.get();

			handlePossibleServiceCommunicationError(response);
			
			performers = response.readEntity(new GenericType<Set<PerformerDTO>>() {
			});

		} catch(ProcessingException e){
			handleServiceCommunicationError();
		} finally {
			// Close the Response object.
			response.close();
			client.close();
		}

		// return the acquired response
		return performers;
	}

	@Override
	public UserDTO createUser(UserDTO newUser) throws ServiceException {
		Client client = ClientBuilder.newClient();
		Response response = null;
		UserDTO user = null;

		// fields missing
		if (newUser.getFirstname() == null || newUser.getLastname() == null || newUser.getUsername() == null
				|| newUser.getPassword() == null) {
			throw new ServiceException(Messages.CREATE_USER_WITH_MISSING_FIELDS);
		}

		try {
			// Post newUser to be processed for new user sign up
			Builder builder = client.target(WEB_SERVICE_URI + "/signup").request();
			response = builder.post(Entity.entity(newUser, MediaType.APPLICATION_XML));

			handlePossibleServiceCommunicationError(response);
			
			// Check response
			int responseCode = response.getStatus();
			switch (responseCode) {
			case 400: // BAD REQUEST
				processErrorMessage(response);
			case 200: // OK
				user = response.readEntity(UserDTO.class);
				storeTokenReceived(response);
			}
		} catch(ProcessingException e){
			handleServiceCommunicationError();
		} finally {			// Close the Response object.
			response.close();
			client.close();
		}

		return user;
	}

	@Override
	public UserDTO authenticateUser(UserDTO user) throws ServiceException {
		Client client = ClientBuilder.newClient();
		Response response = null;

		// fields missing
		if (user.getUsername() == null || user.getPassword() == null) {
			throw new ServiceException(Messages.AUTHENTICATE_USER_WITH_MISSING_FIELDS);
		}

		try {
			// Post user to be processed for authentication
			Builder builder = client.target(WEB_SERVICE_URI + "/login").request();

			response = builder.post(Entity.entity(user, MediaType.APPLICATION_XML));

			handlePossibleServiceCommunicationError(response);
			
			int responseCode = response.getStatus();

			switch (responseCode) {
			case 400: // BAD REQUEST
				processErrorMessage(response);
			case 200: // OK
				user = response.readEntity(UserDTO.class);
				storeTokenReceived(response);
			}

		} catch(ProcessingException e){
			handleServiceCommunicationError();
		} finally {			// Close the Response object.
			response.close();
			client.close();
		}

		// return user with all properties set
		return user;
	}

	@Override
	public Image getImageForPerformer(PerformerDTO performer) throws ServiceException {
		BufferedImage downloadedImage = null;
		if(performer.getImageName() == null){
			throw new ServiceException(Messages.NO_IMAGE_FOR_PERFORMER);
		}
		
		try {
			// Create an AmazonS3 object that represents a connection with the
			// remote S3 service.
			BasicAWSCredentials awsCredentials = new BasicAWSCredentials(
					AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY);
			
			s3 = AmazonS3ClientBuilder
					.standard()
					.withRegion(Regions.AP_SOUTHEAST_2)
					.withCredentials(
							new AWSStaticCredentialsProvider(awsCredentials))
					.build();
			
			// Find images names stored in S3. 
			imageNames = getImageNames(s3);
		} catch (Exception e) {
			handleServiceCommunicationError();
		}

		// download performer's image if exist
		if (imageNames.contains(performer.getImageName())) {
			downloadedImage = downloadImage(performer.getImageName());
		} else {
			throw new ServiceException(Messages.NO_IMAGE_FOR_PERFORMER);
		}

		return downloadedImage;
	}

	@Override
	public ReservationDTO reserveSeats(ReservationRequestDTO reservationRequest) throws ServiceException {
		Client client = ClientBuilder.newClient();
		Response response = null;

		ReservationDTO reservation = null;

		// fields missing
		if (reservationRequest.getConcertId() == null || reservationRequest.getDate() == null
				|| reservationRequest.getNumberOfSeats() == 0 || reservationRequest.getSeatType() == null) {
			throw new ServiceException(Messages.RESERVATION_REQUEST_WITH_MISSING_FIELDS);
		}

		handlePossibleUnauthenticatedRequest();

		try {
			// Make a post request to reserve seats
			Builder builder = client.target(WEB_SERVICE_URI + "/reservations").request().cookie(authenticationToken);

			response = builder.post(Entity.entity(reservationRequest, MediaType.APPLICATION_XML));

			handlePossibleServiceCommunicationError(response);
			
			int responseCode = response.getStatus();

			switch (responseCode) {
			case 400: // BAD REQUEST
				processErrorMessage(response);
			case 401: // UNAUTHORIZED
				processErrorMessage(response);
			case 200: // OK
				reservation = response.readEntity(ReservationDTO.class);
			}

		} catch(ProcessingException e){
			handleServiceCommunicationError();
		} finally {			// Close the Response object.
			response.close();
			client.close();
		}

		return reservation;
	}

	@Override
	public void confirmReservation(ReservationDTO reservation) throws ServiceException {
		Client client = ClientBuilder.newClient();
		Response response = null;

		handlePossibleUnauthenticatedRequest();

		try {
			// Make a post request to confirm seats
			Builder builder = client.target(WEB_SERVICE_URI + "/reservations/confirmation").request()
					.cookie(authenticationToken);

			response = builder.post(Entity.entity(reservation, MediaType.APPLICATION_XML));

			handlePossibleServiceCommunicationError(response);
			
			int responseCode = response.getStatus();

			switch (responseCode) {
			case 400: // BAD REQUEST
				processErrorMessage(response);
			case 401: // UNAUTHORIZED
				processErrorMessage(response);
			}

		} catch(ProcessingException e){
			handleServiceCommunicationError();
		} finally {			// Close the Response object.
			response.close();
			client.close();
		}

	}

	@Override
	public void registerCreditCard(CreditCardDTO creditCard) throws ServiceException {
		Client client = ClientBuilder.newClient();
		Response response = null;

		handlePossibleUnauthenticatedRequest();

		try {
			// Post credit card to be registered with user
			Builder builder = client.target(WEB_SERVICE_URI + "/creditcards").request().cookie(authenticationToken);

			response = builder.post(Entity.entity(creditCard, MediaType.APPLICATION_XML));

			handlePossibleServiceCommunicationError(response);
			
			if (response.getStatus() == 401) { // UNAUTHORIZED
				processErrorMessage(response);
			}

		} catch(ProcessingException e){
			handleServiceCommunicationError();
		} finally {			// Close the Response object.
			response.close();
			client.close();
		}

	}

	@Override
	public Set<BookingDTO> getBookings() throws ServiceException {
		Client client = ClientBuilder.newClient();
		Response response = null;
		Set<BookingDTO> bookings = null;

		handlePossibleUnauthenticatedRequest();

		try {
			// Get all user bookings
			Builder builder = client.target(WEB_SERVICE_URI + "/bookings").request().accept(MediaType.APPLICATION_XML)
					.cookie(authenticationToken);

			response = builder.get();

			handlePossibleServiceCommunicationError(response);
			
			int responseCode = response.getStatus();

			switch (responseCode) {
			case 401: // UNAUTHORIZED
				processErrorMessage(response);
			case 200: // OK
				bookings = response.readEntity(new GenericType<Set<BookingDTO>>() {
				});
			}

		} catch(ProcessingException e){
			handleServiceCommunicationError();
		} finally {			// Close the Response object.
			response.close();
			client.close();
		}

		return bookings;
	}
	
	@Override
	public void subscribeForNewsItems(NewsItemListener listener) {
		Client client = ClientBuilder.newClient();
		
		Response response = null;
		
		// first try to get a subscription id
		try {
			Builder builder = client.target(WEB_SERVICE_URI + "/news/subscription/signup").request();

			response = builder.get();

			handlePossibleServiceCommunicationError(response);
			
			subscriptionId = response.readEntity(String.class);

		} catch(ProcessingException e){
			handleServiceCommunicationError();
		} finally {			// Close the Response object.
			response.close();
		}
		
		subscribe = true;

		// Get subscribed
		WebTarget target = client.target(WEB_SERVICE_URI + "/news/subscription");

		_logger.info("Try to subscribe");
		// include subscription id when subscribing with an invocation callback object
		target.request().cookie(new Cookie(Config.CLIENT_SUBSCRIPTION,subscriptionId))
		.async().get(new InvocationCallback<Response>() {
			public void completed(Response response) {
				handlePossibleServiceCommunicationError(response);
				
				// post any news items received
				Set<NewsItemDTO> newsItems = response.readEntity(new GenericType<Set<NewsItemDTO>>(){});
				if(subscribe){
					for(NewsItemDTO n : newsItems){
						listener.newsItemReceived(n);
					}
				}
				response.close();
				
				if(subscribe){
					target.request().cookie(new Cookie(Config.CLIENT_SUBSCRIPTION,subscriptionId)).async().get(this);
				}
			}

			public void failed(Throwable t) {
			}

		});


	}

	@Override
	public void cancelSubscription() {
		subscribe = false;
		
		// if client is actually subscribed
		if(subscriptionId != null){
			Client client = ClientBuilder.newClient();
			Response response = null;

			_logger.info("Try to unsubscribe");
			try {
				// Get all user bookings
				Builder builder = client.target(WEB_SERVICE_URI + "/news/subscription/{id}")
						.resolveTemplate("id", subscriptionId).request();

				response = builder.delete();

				handlePossibleServiceCommunicationError(response);

			} catch(ProcessingException e){
				handleServiceCommunicationError();
			} finally {			// Close the Response object.
				response.close();
				client.close();
			}
		}
	}

	/**
	 * Finds image names stored in a bucket named AWS_BUCKET.
	 */
	private List<String> getImageNames(AmazonS3 s3) {
		ArrayList<String> imageNames = new ArrayList<>();
		ObjectListing ol = s3.listObjects(AWS_BUCKET);
		List<S3ObjectSummary> objects = ol.getObjectSummaries();
		for (S3ObjectSummary os : objects) {
			imageNames.add(os.getKey());
		}
		return imageNames;
	}

	/**
	 * Downloads a specific performer's image
	 */
	private BufferedImage downloadImage(String img){
		BufferedImage image  = null;
        S3Object o = s3.getObject(AWS_BUCKET, img);
        S3ObjectInputStream s3is = o.getObjectContent();
        try {
			image = ImageIO.read(s3is);
	        s3is.close();
		} catch (Exception e) {
			handleServiceCommunicationError();
		}
		return image;
	}

	/**
	 * Store the authentication token received from the concert application
	 */
	private void storeTokenReceived(Response response) {
		Map<String, NewCookie> cookies = response.getCookies();

		if (cookies.containsKey(Config.CLIENT_COOKIE)) {
			authenticationToken = new Cookie(Config.CLIENT_COOKIE, cookies.get(Config.CLIENT_COOKIE).getValue());
		}
	}

	/**
	 * Throw an appropriate ServiceException based on the response
	 */
	private void processErrorMessage(Response response) {
		throw new ServiceException(response.readEntity(String.class));
	}

	/**
	 * Throws the suitable ServiceException for unauthenticated user
	 */
	private void handlePossibleUnauthenticatedRequest() {
		if (authenticationToken == null) {
			throw new ServiceException(Messages.UNAUTHENTICATED_REQUEST);
		}
	}
	
	/**
	 * Throws the suitable ServiceException for service communication error
	 * that is to do with 5XX server errors
	 */
	private void handlePossibleServiceCommunicationError(Response response) {
		char firstIntInResponseStatus = String.valueOf(response.getStatus()).charAt(0);
		if(firstIntInResponseStatus == 5){
			throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
		}
	}
	
	/**
	 * Throws the suitable ServiceException for service communication error
	 * that is to do when any exception is thrown because service is unable to
	 * process request
	 */
	private void handleServiceCommunicationError() {
		throw new ServiceException(Messages.SERVICE_COMMUNICATION_ERROR);
	}
}
