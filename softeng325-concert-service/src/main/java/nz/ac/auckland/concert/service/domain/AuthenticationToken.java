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
		_value = generateRandomString(7)+user.getUsername();
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
        String chars = "THIS SERVICE IS AWESOME, I RATE 7/11 M8";
        StringBuilder stringBuilder = new StringBuilder();
        while (stringBuilder.length() < stringLength) {
            stringBuilder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return stringBuilder.toString();

    }
}
