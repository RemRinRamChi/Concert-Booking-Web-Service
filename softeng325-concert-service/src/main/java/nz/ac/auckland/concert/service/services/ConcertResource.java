package nz.ac.auckland.concert.service.services;

import java.net.URI;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import nz.ac.auckland.concert.service.domain.Concert;

/**
 * Class to implement a simple REST Web service for managing Concerts.
 *
 */

@Path("/concerts")
public class ConcertResource {
 
	/**
	 * Retrieves a Concert based on its unique id. The HTTP response message 
	 * has a status code of either 200 or 404, depending on whether the 
	 * specified Concert is found. 
	 * 
	 * This method maps to the URI pattern <base-uri>/concerts/{id}.
	 * 
	 * @param id the unique ID of the Concert.
	 * 
	 * @return a Response object containing the required Concert.
	 */
	@GET
	@Path("{id}")
	@Produces(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response retrieveConcert(@PathParam("id") long id) {
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		// Use the EntityManager to retrieve Concert.
		Concert concert = em.find(Concert.class,id);
		
		ResponseBuilder builder = null;
		
		if (concert == null) {
			// Return a HTTP 404 response if the specified Parolee isn't found.
			builder = Response.status(Response.Status.NOT_FOUND);
		} else {
			builder = Response.ok(concert);
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
	 * @param concert the new Concert to create.
	 * 
	 * @return a Response object containing the status code 201 and a Location
	 * header.
	 */
	@POST
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response createConcert(Concert concert) {	
		ResponseBuilder builder = null;
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager
		.instance()
		.createEntityManager();
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
	 * Updates an existing Concert. The HTTP Response message returns 
	 * a status code of 204 or 404.
	 * 
	 * This method maps to the URI pattern <base-uri>/concerts.
	 * 
	 * @param concert the new Concert to create.
	 * 
	 * @return a Response object containing the status code 201 and a Location
	 * header.
	 */
	@PUT
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public Response updateConcert(Concert concert) {	
		ResponseBuilder builder = null;
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager
		.instance()
		.createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		// Use the EntityManager to update Concert.
		if(em.find(Concert.class, concert.getId()) != null){
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
		EntityManager em = PersistenceManager
		.instance()
		.createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		// Use the EntityManager to delete object.
		Concert concert = em.find(Concert.class,id);
		if(concert != null){ // found
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
	 * cookie, whose value is a randomly generated UUID. This method returns 
	 * the new cookie as part of the HTTP response message.
	 * 
	 * This method maps to the URI pattern <base-uri>/concerts.
	 * 
	 * @param clientId a cookie named Config.CLIENT_COOKIE that may be sent 
	 * by the client.
	 * 
	 * @return a Response object containing the status code 204.
	 */
	@DELETE
	public Response deleteAllConcerts() {
		ResponseBuilder builder = null;
		
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager
		.instance()
		.createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		// Use the EntityManager to retrieve all Concerts, then delete them all.
		TypedQuery<Concert> concertQuery =
				em.createQuery("select c from Concert c", Concert.class);
		List<Concert> concerts = concertQuery.getResultList();
		for(Concert c : concerts){
			em.remove(c);
			
		}
		// Commit the transaction.
		em.getTransaction().commit();
		
		builder = Response.status(Response.Status.NO_CONTENT);
		
		return builder.build();
	}
}
