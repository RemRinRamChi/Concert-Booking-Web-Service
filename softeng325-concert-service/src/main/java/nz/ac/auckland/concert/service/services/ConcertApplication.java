package nz.ac.auckland.concert.service.services;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS application subclass for the Concert Web service. This class is
 * discovered by the JAX-RS run-time and is used to obtain a reference to the
 * ConcertResource object that will process Web service requests.
 * 
 * The base URI for the Concert Web service is:
 * 
 * http://<host-name>:<port>/services.
 *
 */
@ApplicationPath("/services")
public class ConcertApplication extends Application {
	// This property should be used by your Resource class. It represents the
	// period of time, in seconds, that reservations are held for. If a
	// reservation isn't confirmed within this period, the reserved seats are
	// returned to the pool of seats available for booking.
	//
	// This property is used by class ConcertServiceTest.
	public static final int RESERVATION_EXPIRY_TIME_IN_SECONDS = 5;

	private Set<Object> _singletons = new HashSet<Object>();
	private Set<Class<?>> _classes = new HashSet<Class<?>>();

	public ConcertApplication() {
		_singletons.add(PersistenceManager.instance());
		_classes.add(ConcertResource.class);
	}

	@Override
	public Set<Object> getSingletons() {

		return _singletons;
	}

	@Override
	public Set<Class<?>> getClasses() {
		return _classes;
	}
}