package nz.ac.auckland.concert.client.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

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

public class DefaultService implements ConcertService {

	// AWS S3 access credentials for concert images.
	private static final String AWS_ACCESS_KEY_ID = "AKIAIDYKYWWUZ65WGNJA";
	private static final String AWS_SECRET_ACCESS_KEY = "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";

	// Name of the S3 bucket that stores images.
	private static final String AWS_BUCKET = "concert.aucklanduni.ac.nz";

	private AmazonS3 s3;
	
	private List<String> imageNames;
	
	public DefaultService() {
		// Create an AmazonS3 object that represents a connection with the
		// remote S3 service.
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY);
		s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.AP_SOUTHEAST_2)
				.withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();
		

		// Find images names stored in S3.
		imageNames = getImageNames(s3);
		
	}

	@Override
	public Set<ConcertDTO> getConcerts() throws ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<PerformerDTO> getPerformers() throws ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserDTO createUser(UserDTO newUser) throws ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserDTO authenticateUser(UserDTO user) throws ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Image getImageForPerformer(PerformerDTO performer) throws ServiceException {
		String imageName = performer.getImageName();
		if(imageNames.contains(imageName)){
			downloadImage(s3, imageName);
		} else {
			//TODO throw exception
		}
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void confirmReservation(ReservationDTO reservation) throws ServiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerCreditCard(CreditCardDTO creditCard) throws ServiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<BookingDTO> getBookings() throws ServiceException {
		// TODO Auto-generated method stub
		return null;
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
		    Download xfer = mgr.download(AWS_BUCKET, img, new File(img)); //TODO
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
}
