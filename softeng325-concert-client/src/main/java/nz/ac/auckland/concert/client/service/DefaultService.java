package nz.ac.auckland.concert.client.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

import nz.ac.auckland.concert.common.dto.BookingDTO;
import nz.ac.auckland.concert.common.dto.ConcertDTO;
import nz.ac.auckland.concert.common.dto.CreditCardDTO;
import nz.ac.auckland.concert.common.dto.PerformerDTO;
import nz.ac.auckland.concert.common.dto.ReservationDTO;
import nz.ac.auckland.concert.common.dto.ReservationRequestDTO;
import nz.ac.auckland.concert.common.dto.UserDTO;
import nz.ac.auckland.concert.common.message.Messages;
import nz.ac.auckland.concert.common.util.Config;

// TODO javadoc and the special message exception
public class DefaultService implements ConcertService {
	// AWS S3 access credentials for concert images.
	private static final String AWS_ACCESS_KEY_ID = "AKIAIDYKYWWUZ65WGNJA";
	private static final String AWS_SECRET_ACCESS_KEY = "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";

	// Name of the S3 bucket that stores images.
	private static final String AWS_BUCKET = "concert.aucklanduni.ac.nz";

	private String WEB_SERVICE_URI = "http://localhost:10000/services/concerts";

	private Client _client;
	
	private AmazonS3 s3;
	
	private List<String> imageNames;
	
	private Cookie authenticationToken;
	
	public DefaultService() {
		// Use ClientBuilder to create a new client that can be used to create
		// connections to the Web service.
		_client = ClientBuilder.newClient();
		/*// TODO uncomment when submitting, this just speeds up testing
		// Create an AmazonS3 object that represents a connection with the
		// remote S3 service.
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY);
		s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.AP_SOUTHEAST_2)
				.withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();
		

		// Find images names stored in S3.
		imageNames = getImageNames(s3);
		*/
		// Initially unauthenticated
		authenticationToken = null;
	}

	@Override
	public Set<ConcertDTO> getConcerts() throws ServiceException {
		Response response = null;
		Set<ConcertDTO> concerts = null; 

		try {
			// Make a get request for all concerts
			Builder builder = _client.target(WEB_SERVICE_URI).request()
					.accept(MediaType.APPLICATION_XML);

			response = builder.get();
			
			concerts = response.readEntity(new GenericType<Set<ConcertDTO>>() {});
			
		} finally {
			// Close the Response object.
			response.close();
		}
		
		// return the acquired response
		return concerts;
	}

	@Override
	public Set<PerformerDTO> getPerformers() throws ServiceException {
		Response response = null;
		Set<PerformerDTO> performers = null; 

		try {
			// Make a get request for all performers
			Builder builder = _client.target(WEB_SERVICE_URI+"/performers").request()
					.accept(MediaType.APPLICATION_XML);
			
			response = builder.get();
			
			performers = response.readEntity(new GenericType<Set<PerformerDTO>>() {});
			
		} finally {
			// Close the Response object.
			response.close();
		}
		
		// return the acquired response
		return performers;
	}

	@Override
	public UserDTO createUser(UserDTO newUser) throws ServiceException {
		Response response = null;
		UserDTO user = null;
		
		// fields missing
		if(newUser.getFirstname() == null || newUser.getLastname() == null || 
				newUser.getUsername() == null || newUser.getPassword() == null ){
			throw new ServiceException(Messages.CREATE_USER_WITH_MISSING_FIELDS);
		}
		
		try {
			// Post newUser to be processed for new user sign up
			Builder builder = _client.target(WEB_SERVICE_URI+"/signup").request();
			response = builder.post(Entity.entity(newUser,
					MediaType.APPLICATION_XML));

			// Check response 
			int responseCode = response.getStatus();
			switch (responseCode) {
			case 400: // BAD REQUEST
				processErrorMessage(response);
			case 200: // OK
				user = response.readEntity(UserDTO.class);
				storeTokenReceived(response);
			}
		} finally {
			// Close the Response object.
			response.close();
		}
		
		return user;
	}

	@Override
	public UserDTO authenticateUser(UserDTO user) throws ServiceException {
		Response response = null;
		
		// fields missing
		if(user.getUsername() == null || user.getPassword() == null ){
			throw new ServiceException(Messages.AUTHENTICATE_USER_WITH_MISSING_FIELDS);
		}
		
		try {
			// Post user to be processed for authentication
			Builder builder = _client.target(WEB_SERVICE_URI+"/login").request();

			response = builder.post(Entity.entity(user,
					MediaType.APPLICATION_XML));

			int responseCode = response.getStatus();
			
			switch (responseCode) {
			case 400: // BAD REQUEST
				processErrorMessage(response);
			case 200: // OK
				user = response.readEntity(UserDTO.class);
				storeTokenReceived(response);
			}
			
		} finally {
			// Close the Response object.
			response.close();
		}
		
		// return user with all properties set
		return user;
	}

	@Override
	public Image getImageForPerformer(PerformerDTO performer) throws ServiceException {
		String imageName = performer.getImageName();
		
		// download performer's image if exist
		if(imageNames.contains(imageName)){
			downloadImage(s3, imageName);
		} else {
			throw new ServiceException(Messages.NO_IMAGE_FOR_PERFORMER);
		}
		
		// read the image from the downloaded image file and then delete it to not take up unnecessary space
		BufferedImage image = null;
		try {
			File imageFile = new File(imageName);
		    image = ImageIO.read(imageFile);
		    imageFile.delete();
		} catch (IOException e) {
		}
		return image;
	}

	@Override
	public ReservationDTO reserveSeats(ReservationRequestDTO reservationRequest) throws ServiceException {
		Response response = null;
		
		ReservationDTO reservation = null;
		
		// fields missing //TODO is 0 alright for concert id?
		if(reservationRequest.getConcertId() == null || reservationRequest.getDate() == null
				|| reservationRequest.getNumberOfSeats() == 0 || reservationRequest.getSeatType() == null){
			throw new ServiceException(Messages.CREATE_USER_WITH_MISSING_FIELDS);
		}
		
		handlePossibleUnauthenticatedRequest();
		
		try {
			// Make a post request to reserve seats
			Builder builder = _client.target(WEB_SERVICE_URI+"/reservations").request()
					.cookie(authenticationToken);

			response = builder.post(Entity.entity(reservationRequest,
					MediaType.APPLICATION_XML));

			int responseCode = response.getStatus();
			
			switch (responseCode) {
			case 400: // BAD REQUEST
				processErrorMessage(response);
			case 401: // UNAUTHORIZED
				processErrorMessage(response);
			case 200: // OK
				reservation = response.readEntity(ReservationDTO.class);
			}
			
		} finally {
			// Close the Response object.
			response.close();
		}
		
		return reservation;
	}

	@Override
	public void confirmReservation(ReservationDTO reservation) throws ServiceException {
		Response response = null;
		
		handlePossibleUnauthenticatedRequest();
		
		try {
			// Make a post request to confirm seats
			Builder builder = _client.target(WEB_SERVICE_URI+"/reservations/confirmation").request()
					.cookie(authenticationToken);

			response = builder.post(Entity.entity(reservation,
					MediaType.APPLICATION_XML));
			
			int responseCode = response.getStatus();
			
			switch (responseCode) {
			case 400: // BAD REQUEST
				processErrorMessage(response);
			case 401: // UNAUTHORIZED
				processErrorMessage(response);
			}
			
		} finally {
			// Close the Response object.
			response.close();
		}

	}

	@Override
	public void registerCreditCard(CreditCardDTO creditCard) throws ServiceException {
		Response response = null;
		
		handlePossibleUnauthenticatedRequest();
		
		try {
			// Post credit card to be registered with user
			Builder builder = _client.target(WEB_SERVICE_URI+"/creditcards").request()
					.cookie(authenticationToken);

			response = builder.post(Entity.entity(creditCard,
					MediaType.APPLICATION_XML));

			if(response.getStatus() == 401){ // UNAUTHORIZED
				processErrorMessage(response);
			}
			
		} finally {
			// Close the Response object.
			response.close();
		}

	}

	@Override
	public Set<BookingDTO> getBookings() throws ServiceException {
		Response response = null;
		Set<BookingDTO> bookings = null; 

		handlePossibleUnauthenticatedRequest();
		
		try {
			// Get all user bookings
			Builder builder = _client.target(WEB_SERVICE_URI+"/bookings").request()
					.accept(MediaType.APPLICATION_XML)
					.cookie(authenticationToken);

			response = builder.get();
			
			int responseCode = response.getStatus();
			
			switch (responseCode) {
			case 401: // UNAUTHORIZED
				processErrorMessage(response);
			case 200: // OK
				bookings = response.readEntity(new GenericType<Set<BookingDTO>>() {});
			}
			
		} finally {
			// Close the Response object.
			response.close();
		}
		
		return bookings;
	}

	@Override
	public void subscribeForNewsItems(NewsItemListener listener) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void cancelSubscription() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Finds image names stored in a bucket named AWS_BUCKET.
	 * 
	 * @param s3 the AmazonS3 connection.
	 * 
	 * @return a List of images names.
	 * 
	 */
	private List<String> getImageNames(AmazonS3 s3) {
		ArrayList<String> imageNames = new ArrayList<>();
		ObjectListing ol = s3.listObjects(AWS_BUCKET);
		List<S3ObjectSummary> objects = ol.getObjectSummaries();
		for (S3ObjectSummary os: objects) {
		    imageNames.add(os.getKey());
		}
		return imageNames;
	}
	

	// TODO does a performer has more than 1 image??
	/**
	 * Downloads a specific performer's image
	 * 
	 * @param s3 the AmazonS3 connection.
	 * 
	 * @param imageNames the named images to download.
	 * 
	 */
	private void downloadImage(AmazonS3 s3, String img) {
		
		TransferManager mgr = TransferManagerBuilder
				.standard()
				.withS3Client(s3)
				.build();
		try {
		    Download xfer = mgr.download(AWS_BUCKET, img, new File(img)); //TODO maybe there's a better way
		    xfer.waitForCompletion();
		} catch (AmazonServiceException e) {
		    System.err.println("Amazon service error: " + e.getMessage());
		    System.exit(1);
		} catch (AmazonClientException e) {
		    System.err.println("Amazon client error: " + e.getMessage());
		    System.exit(1);
		} catch (InterruptedException e) {
		    System.err.println("Transfer interrupted: " + e.getMessage());
		    System.exit(1);
		}	
		mgr.shutdownNow();
	}
	
	/**
	 * Store the authentication token received from the concert application 
	 */
	private void storeTokenReceived(Response response) {
		Map<String, NewCookie> cookies = response.getCookies();
		
		if(cookies.containsKey(Config.CLIENT_COOKIE)) {
			authenticationToken = new Cookie(Config.CLIENT_COOKIE,cookies.get(Config.CLIENT_COOKIE).getValue());
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
	private void handlePossibleUnauthenticatedRequest(){
		if(authenticationToken == null){
			throw new ServiceException(Messages.UNAUTHENTICATED_REQUEST);
		}
	}
}
