package nz.ac.auckland.concert.service.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import nz.ac.auckland.concert.common.types.PriceBand;
import nz.ac.auckland.concert.service.domain.jpa.LocalDateTimeConverter;

/**
 * Class to represent bookings (confirmed reservations). 
 * 
 * A Booking describes a booking in terms of:
 * _id			   the unique identifier for the booking
 * _concert        the concert.
 * _uesr		   the user.
 * _dateTime       the concert's scheduled date and time for which the booking 
 *                 applies.
 * _seats          the seats that have been booked (represented as a  Set of 
 *                 SeatDTO objects).
 * _priceBand      the price band of the booked seats (all seats are within the 
 *                 same price band).
 *
 */
@Entity
public class Booking {
	@Id
	@GeneratedValue
	private Long _id;
	
	@ManyToOne
	@JoinColumn(name = "CONCERT_ID", nullable = false)
	private Concert _concert;

	@ManyToOne
	@JoinColumn(name = "USER_ID", nullable = false)
	private User _user;
	
	@Convert(converter = LocalDateTimeConverter.class)
	private LocalDateTime _dateTime;
	
	@ElementCollection
	@CollectionTable(name = "BOOKING_SEATS")
	private Set<Seat> _seats;
	
    @Enumerated(EnumType.STRING)
	private PriceBand _priceBand;

    private boolean _confirmed;
    
	public Booking() {
	}

	public Booking(Concert concert,
			LocalDateTime dateTime, Set<Seat> seats, PriceBand priceBand, User user) {
		_concert = concert;
		_dateTime = dateTime;

		_seats = new HashSet<Seat>();
		_seats.addAll(seats);

		_priceBand = priceBand;
		
		_user = user;
		
		_confirmed = false;
	}

	public Long getId(){
		return _id;
	}
	
	public void setConfirmed(){
		_confirmed = true;
	}	
	
	public boolean getConfirmationStatus(){
		return _confirmed;
	}	
	
	public Concert getConcert() {
		return _concert;
	}

	public LocalDateTime getDateTime() {
		return _dateTime;
	}

	public Set<Seat> getSeats() {
		return _seats;
	}

	public PriceBand getPriceBand() {
		return _priceBand;
	}

	public User getUser() {
		return _user;
	}
	
	public String getUsername(){
		return _user.getUsername();
	}
	
	public Long getConcertId(){
		return _concert.getId();
	}
	
	public String getConcertTitle(){
		return _concert.getTitle();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Booking))
			return false;
		if (obj == this)
			return true;

		Booking rhs = (Booking) obj;
		return new EqualsBuilder()
				.append(_concert, rhs._concert)
				.append(_user, rhs._user)
				.append(_dateTime, rhs._dateTime)
				.append(_seats, rhs._seats)
				.append(_priceBand, rhs._priceBand)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31)
				.append(_concert)
				.append(_user)
				.append(_dateTime)
				.append(_seats)
				.append(_priceBand)
				.hashCode();
	}
}
