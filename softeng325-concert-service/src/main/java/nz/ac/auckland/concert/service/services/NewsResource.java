package nz.ac.auckland.concert.service.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nz.ac.auckland.concert.common.dto.NewsItemDTO;
import nz.ac.auckland.concert.common.util.Config;
import nz.ac.auckland.concert.service.domain.NewsItem;

/**
 * Class to implement a simple REST Web service for managing NewsItems.
 *
 */
@Path("/concerts/news")
public class NewsResource {

	private static Logger _logger = LoggerFactory
			.getLogger(NewsResource.class);

	/**
	 * Unique list of ids of clients that intend on subscribing
	 */
	private List<String> _subscriptionIds = new ArrayList<String>();
	
	/**
	 * The subscribed clients' asyncresponses for news item
	 */
	private HashMap<String,AsyncResponse> _responses = new HashMap<>();
	
	/**
	 * The clients' most recent news items
	 */
	private HashMap<String,Long> _mostRecentClientNews = new HashMap<>();

	
	/**
	 * Returns a special unique id identifying a client intending to subscribe
	 * , to be useful when canceling subscription
	 */
	@GET
	@Path("/subscription/signup")
	public Response registerForSubscription() {
		ResponseBuilder builder = null;

		String randomId = null;
		
		boolean notUnique = true;
		
		while(notUnique){
			randomId = generateRandomString();
			if(!_subscriptionIds.contains(randomId)){
				notUnique = false;
			}
		}
		
		_subscriptionIds.add(randomId);
		
		_logger.info("Subscription id generated: "+randomId);
		
		builder = Response.ok(randomId);

		return builder.build();
	}
	
	/**
	 * Deletes subscription details associated with an unsubscribed client
	 */
	@DELETE
	@Path("/subscription/{id}")
	public Response unsubscribe(@PathParam("id") String id){
		_logger.info("Subscription cancelled: "+id);
		ResponseBuilder builder = null;

		// get response to resume
		AsyncResponse response = _responses.get(id);
		
		if(response != null){
			// create empty list for resuming
			List<NewsItemDTO> newNewsItems = new ArrayList<>();
			GenericEntity<List<NewsItemDTO>> entity = new GenericEntity<List<NewsItemDTO>>(newNewsItems){};
			ResponseBuilder newsBuilder = Response.ok(entity);
			
			// close the connection by resuming then asyncresponse
			response.resume(newsBuilder.build());
			
			
			// remove subscription details 
			_responses.remove(id);
			_mostRecentClientNews.remove(id);
			_subscriptionIds.remove(id);
		}
		builder = Response.status(Status.NO_CONTENT);
		
		return builder.build();
	}
	
	/**
	 * Subscribe for news item
	 */
	@GET
	@Path("/subscription")
	public void subscribe(@Suspended AsyncResponse clientResponse, @CookieParam(Config.CLIENT_SUBSCRIPTION) String token){
		_logger.info("subscriber detected");
		// associate with subscription id
		_responses.put(token, clientResponse);
		
	}
	
	@POST
	@Consumes(javax.ws.rs.core.MediaType.APPLICATION_XML)
	public void sendNewsItem(NewsItemDTO newsItemDTO){
		_logger.info("News posted: "+newsItemDTO.getId());
		// Acquire an EntityManager (creating a new persistence context).
		EntityManager em = PersistenceManager.instance().createEntityManager();
		// Start a new transaction.
		em.getTransaction().begin();
		// store the news item to database
		em.persist(DomainMapper.newsItemToDomainModel(newsItemDTO));
		
		em.getTransaction().commit();

		em.getTransaction().begin();

		// get all the news items in the database
		TypedQuery<NewsItem> newsItemQuery = 
				em.createQuery("select n from NewsItem n", NewsItem.class);
		List<NewsItem> newsItems = newsItemQuery.getResultList();

		
		if(!_responses.isEmpty()){
			_logger.info("Need to notify subscribers");

			// respond to all the subscribed clients
			for(String id : _responses.keySet()){
				AsyncResponse response = _responses.get(id);
				Long recentNews = _mostRecentClientNews.get(id);
				
				// null = recently subscribed
				if(recentNews == null){
					List<NewsItemDTO> newNewsItems = new ArrayList<>();
					
					// return only the just posted news
					newNewsItems.add(newsItemDTO);
					GenericEntity<List<NewsItemDTO>> entity = new GenericEntity<List<NewsItemDTO>>(newNewsItems){};
					ResponseBuilder builder = Response.ok(entity);
					_logger.info("Returning 1st time news id: "+newsItemDTO.getId());

					// register the most recent news as most recent news
					_mostRecentClientNews.put(id, newsItems.get(newsItems.size()-1).getId());
					
					// return the news item
					response.resume(builder.build());
					_responses.remove(id);

				} else {
					List<NewsItemDTO> newNewsItems = new ArrayList<>();
					// return all the news after a certain news id (the subscriber's last received news item)
					for(NewsItem n : newsItems){
						if(n.getId() > recentNews){
							newNewsItems.add(DomainMapper.newsItemToDTO(n));
							_logger.info("Returning news id: "+n.getId());
							_logger.info("marks end of consecutives");
							if(_mostRecentClientNews.get(id) < n.getId()){
								_mostRecentClientNews.put(id, n.getId());
							}
						}
					}
					GenericEntity<List<NewsItemDTO>> entity = new GenericEntity<List<NewsItemDTO>>(newNewsItems){};
					ResponseBuilder builder = Response.ok(entity);
					
					// return the news items
					response.resume(builder.build());
					_responses.remove(id);
				}
				
			}
		} else {
			_logger.info("Nobody subscribed");
		}
		
	}
	
	
	/**
	 * Generate a random string of length 12
	 */
	private String generateRandomString() {
        Random random = new Random();
        // Cookies don't allow a starting value of ","
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder stringBuilder = new StringBuilder();
        while (stringBuilder.length() < 12) {
            stringBuilder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return stringBuilder.toString();

    }
	
}
