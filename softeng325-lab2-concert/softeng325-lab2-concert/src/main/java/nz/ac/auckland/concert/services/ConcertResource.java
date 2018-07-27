package nz.ac.auckland.concert.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import nz.ac.auckland.concert.common.Config;
import nz.ac.auckland.concert.domain.Concert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to implement a simple REST Web service for managing Concerts.
 *
 */
@Path("concerts")
@Produces({javax.ws.rs.core.MediaType.APPLICATION_XML, SerializationMessageBodyReaderAndWriter.APPLICATION_JAVA_SERIALIZED_OBJECT})
public class ConcertResource {

	private static Logger _logger = LoggerFactory
			.getLogger(ConcertResource.class);
	

	// Declare necessary instance variables.
	
	private Map<Long, Concert> concerts = new ConcurrentHashMap<>();
	private AtomicLong id = new AtomicLong();

 
	/**
	 * Retrieves a Concert based on its unique id. The HTTP response message 
	 * has a status code of either 200 or 404, depending on whether the 
	 * specified Concert is found. 
	 * 
	 * When clientId is null, the HTTP request message doesn't contain a cookie 
	 * named clientId (Config.CLIENT_COOKIE), this method generates a new 
	 * cookie, whose value is a randomly generated UUID. This method returns 
	 * the new cookie as part of the HTTP response message.
	 * 
	 * This method maps to the URI pattern <base-uri>/concerts/{id}.
	 * 
	 * @param id the unique ID of the Concert.
	 * 
	 * @param clientId a cookie named Config.CLIENT_COOKIE that may be sent 
	 * by the client.
	 * 
	 * @return a Response object containing the required Concert.
	 */
	@GET
	@Path("{id}")
	public Response retrieveConcert(@PathParam("id") long id, @CookieParam("clientId") Cookie clientId) {
		Concert c = null;
		ResponseBuilder rb;
		if (concerts.containsKey(id)) {
			rb = Response.ok(concerts.get(id));
		} else {
			return null;
		}
		if (clientId == null ) {
			rb = rb.cookie(makeCookie(clientId));
		}
		return rb.build();
	}
	

	/**
	 * Retrieves a collection of Concerts, where the "start" query parameter
     * identifies an index position, and "size" represents the maximum number
     * of successive Concerts to return. The HTTP response message returns 200.
     * 
     * When clientId is null, the HTTP request message doesn't contain a cookie 
	 * named clientId (Config.CLIENT_COOKIE), this method generates a new 
	 * cookie, whose value is a randomly generated UUID. This method returns 
	 * the new cookie as part of the HTTP response message.
	 * 
	 * This method maps to the URI pattern <base-uri>/concerts?start&size.
	 * 
	 * @param start the ID of a Concert from which to start retrieving 
	 * Concerts.
	 * 
	 * @param size the maximum number of Concerts to retrieve.
	 * 
	 * @param clientId a cookie named Config.CLIENT_COOKIE that may be sent 
	 * by the client.
	 * 
	 * @return a Response object containing a List of Concerts. The List may be
	 * empty.
	 */
	@GET
	public Response retrieveConcerts(@QueryParam("start") long start, @QueryParam("size") int size, @CookieParam("clientId") Cookie clientId) {
		ResponseBuilder rb;
		ArrayList<Concert> retrieved = new ArrayList<>();
		for (Concert c : concerts.values()) {
			if (retrieved.size() < size && c.getId() >= start) {
				retrieved.add(c);
			}
		}
		GenericEntity<List<Concert>> entity = new GenericEntity<List<Concert>>(retrieved) {};
		rb = Response.ok(entity);
		if (clientId == null ) {
			rb = rb.cookie(makeCookie(clientId));
		}
		return rb.build();
	}
	
	
	/**
	 * Creates a new Concert. This method assigns an ID to the new Concert and
	 * stores it in memory. The HTTP Response message returns a Location header 
	 * with the URI of the new Concert and a status code of 201.
	 * 
	 * When clientId is null, the HTTP request message doesn't contain a cookie 
	 * named clientId (Config.CLIENT_COOKIE), this method generates a new 
	 * cookie, whose value is a randomly generated UUID. This method returns 
	 * the new cookie as part of the HTTP response message.
	 * 
	 * This method maps to the URI pattern <base-uri>/concerts.
	 * 
	 * @param concert the new Concert to create.
	 * 
	 * @param clientId a cookie named Config.CLIENT_COOKIE that may be sent 
	 * by the client.
	 * 
	 * @return a Response object containing the status code 201 and a Location
	 * header.
	 */
	@POST
	public Response createConcert(Concert concert, @CookieParam("clientId") Cookie clientId) {
		ResponseBuilder rb;
		if (concert == null) {
			return null; //404
		} else {
			long id = this.id.incrementAndGet();
			concert = new Concert(id, concert.getTitle(), concert.getDate());
			concerts.put(id, concert);
			rb = Response.created(URI.create("/concerts/" + id));
			if (clientId == null ) {
				rb = rb.cookie(makeCookie(clientId));
			}
			return rb.build();
		}
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
	public Response deleteAllConcerts(@CookieParam("clientId") Cookie clientId) {
		ResponseBuilder rb;
		concerts = new HashMap<>();
		rb = Response.noContent();
		if (clientId == null ) {
			rb = rb.cookie(makeCookie(clientId));
		}
		return rb.build();
	}
	
	/**
	 * Helper method that can be called from every service method to generate a 
	 * NewCookie instance, if necessary, based on the clientId parameter.
	 * 
	 * @param clientId the Cookie whose name is Config.CLIENT_COOKIE, extracted
	 * from a HTTP request message. This can be null if there was no cookie 
	 * named Config.CLIENT_COOKIE present in the HTTP request message. 
	 * 
	 * @return a NewCookie object, with a generated UUID value, if the clientId 
	 * parameter is null. If the clientId parameter is non-null (i.e. the HTTP 
	 * request message contained a cookie named Config.CLIENT_COOKIE), this 
	 * method returns null as there's no need to return a NewCookie in the HTTP
	 * response message. 
	 */
	private NewCookie makeCookie(Cookie clientId){
		NewCookie newCookie = null;
		
		if(clientId == null) {
			newCookie = new NewCookie(Config.CLIENT_COOKIE, UUID.randomUUID().toString());
			_logger.info("Generated cookie: " + newCookie.getValue());
		} 
		
		return newCookie;
	}
}
