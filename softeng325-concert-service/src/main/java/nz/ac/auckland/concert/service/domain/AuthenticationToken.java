package nz.ac.auckland.concert.service.domain;

import java.util.Random;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class AuthenticationToken {

	@Id 
	private String _value;
	
	@OneToOne(optional = false)
	private User _user;
	
	protected AuthenticationToken(){
	}
	
	public AuthenticationToken(User user){
		_user = user;
		// will be unique since usernames are unique
		_value = generateRandomString(5)+user.getUsername();
	}
	
	public String getValue(){
		return _value; 
	}
	
	public User getUser(){
		return _user;
	}
	
	/**
	 * Generate a random string of a chosen length
	 */
	private String generateRandomString(int stringLength) {
        Random random = new Random();
        // Cookies don't allow a starting value of ","
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder stringBuilder = new StringBuilder();
        while (stringBuilder.length() < stringLength) {
            stringBuilder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return stringBuilder.toString();

    }
}
