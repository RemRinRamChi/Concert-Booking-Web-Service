package nz.ac.auckland.concert.service.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Class to represent users. 
 * 
 * A User describes a user in terms of:
 * _username    the user's unique username.
 * _password    the user's password.
 * _firstname   the user's first name.
 * _lastname    the user's family name.
 * _creditCards each of the user's registered credit cards
 *
 */
@Entity
public class User {
	
	@Id
	private String _username;
	private String _password;
	private String _firstname;
	private String _lastname;
	
	@ElementCollection
	@CollectionTable(name = "USER_CREDITCARDS")
	private Set<CreditCard> _creditCards;

	protected User() {}
	
	public User(String username, String password, String lastname, String firstname) {
		_username = username;
		_password = password;
		_lastname = lastname;
		_firstname = firstname;
		_creditCards = new HashSet<CreditCard>();
	}
	
	public User(String username, String password) {
		this(username, password, null, null);
	}
	
	public String getUsername() {
		return _username;
	}
	
	public String getPassword() {
		return _password;
	}
	
	public String getFirstname() {
		return _firstname;
	}
	
	public String getLastname() {
		return _lastname;
	}

	public void addCreditCard(CreditCard creditCard){
		_creditCards.add(creditCard);
	}
	
	public Set<CreditCard> getCreditCards() {
		return _creditCards;
	}


	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof User))
            return false;
        if (obj == this)
            return true;

        User rhs = (User) obj;
        return new EqualsBuilder().
            append(_username, rhs._username).
            append(_password, rhs._password).
            append(_firstname, rhs._firstname).
            append(_lastname, rhs._lastname).
            isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31). 
	            append(_username).
	            append(_password).
	            append(_firstname).
	            append(_password).
	            hashCode();
	}
}
