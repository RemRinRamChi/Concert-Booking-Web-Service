package nz.ac.auckland.concert.service.services;

import java.util.HashSet;
import java.util.Set;

import nz.ac.auckland.concert.common.dto.BookingDTO;
import nz.ac.auckland.concert.common.dto.ConcertDTO;
import nz.ac.auckland.concert.common.dto.CreditCardDTO;
import nz.ac.auckland.concert.common.dto.PerformerDTO;
import nz.ac.auckland.concert.common.dto.SeatDTO;
import nz.ac.auckland.concert.common.dto.UserDTO;
import nz.ac.auckland.concert.service.domain.Booking;
import nz.ac.auckland.concert.service.domain.Concert;
import nz.ac.auckland.concert.service.domain.CreditCard;
import nz.ac.auckland.concert.service.domain.Performer;
import nz.ac.auckland.concert.service.domain.Seat;
import nz.ac.auckland.concert.service.domain.User;

/**
 * Helper class to convert between domain-model and DTO objects.
 *
 */
public class DTOMapper {
	static ConcertDTO concertToDTO(Concert concert){
		return new ConcertDTO(concert.getId(), concert.getTitle(), 
				concert.getDates(), concert.getTariff(), concert.getPerformerIds());

	}
	
	static PerformerDTO performerToDTO(Performer performer){
		return new PerformerDTO(performer.getId(), performer.getName(), 
				performer.getImageName(),performer.getGenre(), performer.getConcertIds());

	}
	
	static UserDTO userToDTO(User user){
		return new UserDTO(user.getUsername(), user.getPassword(), user.getLastname(), user.getFirstname());
	}
	
	static User userToDomainModel(UserDTO user){
		return new User(user.getUsername(), user.getPassword(), user.getLastname(), user.getFirstname());
	}
	
	static CreditCard creditCardToDomainModel(CreditCardDTO creditCard){
		return new CreditCard(creditCard.getType(), creditCard.getName(), creditCard.getNumber(), creditCard.getExpiryDate());
	}	
	
	static BookingDTO bookingToDTO(Booking booking){
		return new BookingDTO(booking.getConcertId(), booking.getConcertTitle(), booking.getDateTime(), 
				seatsToDTO(booking.getSeats()), booking.getPriceBand());
	}
	
	static SeatDTO seatToDTO(Seat seat){
		return new SeatDTO(seat.getRow(), seat.getNumber());
	}
	
	static Set<SeatDTO> seatsToDTO(Set<Seat> seats){
		Set<SeatDTO> seatDTOs = new HashSet<SeatDTO>();
		for(Seat s : seats){
			seatDTOs.add(seatToDTO(s));
		}
		return seatDTOs;
	}
	
}
