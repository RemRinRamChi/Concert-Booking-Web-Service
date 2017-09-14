package nz.ac.auckland.concert.service.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import nz.ac.auckland.concert.common.types.PriceBand;
import nz.ac.auckland.concert.service.domain.jpa.LocalDateTimeConverter;

/**
 * Class to represent reservations. 
 * 
 * A Reservation describes a reservation in terms of:
 * _id			   the unique identifier for the booking
 * _concert        the concert.
 *                 applies.
 * _seats          the seats that have been booked (represented as a  Set of 
 *                 SeatDTO objects).
 * _priceBand      the price band of the booked seats (all seats are within the 
 *                 same price band).
 *
 */
public class Reservation {
	@Id
	@GeneratedValue
	private Long _id;
	
	@ManyToOne
	private Concert _concert;
	
	@Convert(converter = LocalDateTimeConverter.class)
	private LocalDateTime _dateTime;
	
	@ElementCollection
	@CollectionTable(name = "RESERVATION_SEATS")
	private Set<Seat> _seats;
	
    @Enumerated(EnumType.STRING)
	private PriceBand _priceBand;


	public Reservation() {
	}

	public Reservation(Long id, Concert concert,
			LocalDateTime dateTime, Set<Seat> seats, PriceBand priceBand) {
		_id = id;
		_concert = concert;
		_dateTime = dateTime;

		_seats = new HashSet<Seat>();
		_seats.addAll(seats);

		_priceBand = priceBand;
	}

	public Reservation(Concert concert,
			LocalDateTime dateTime, Set<Seat> seats, PriceBand priceBand) {
		this(null, concert, dateTime, seats, priceBand);
	}
	
	public Concert getConcert() {
		return _concert;
	}

	public LocalDateTime getDateTime() {
		return _dateTime;
	}

	public Set<Seat> getSeats() {
		return Collections.unmodifiableSet(_seats);
	}

	public PriceBand getPriceBand() {
		return _priceBand;
	}

	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Reservation))
			return false;
		if (obj == this)
			return true;

		Reservation rhs = (Reservation) obj;
		return new EqualsBuilder().append(_concert, rhs._concert)
				.append(_dateTime, rhs._dateTime)
				.append(_seats, rhs._seats)
				.append(_priceBand, rhs._priceBand).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(_concert)
				.append(_dateTime).append(_seats)
				.append(_priceBand).hashCode();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("concert: ");
		buffer.append(_concert.getTitle());
		buffer.append(", date/time ");
		buffer.append(_seats.size());
		buffer.append(" ");
		buffer.append(_priceBand);
		buffer.append(" seats.");
		return buffer.toString();
	}
}