package nz.ac.auckland.concert.service.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nz.ac.auckland.concert.common.dto.BookingDTO;
import nz.ac.auckland.concert.common.dto.ConcertDTO;
import nz.ac.auckland.concert.common.dto.CreditCardDTO;
import nz.ac.auckland.concert.common.dto.NewsItemDTO;
import nz.ac.auckland.concert.common.dto.PerformerDTO;
import nz.ac.auckland.concert.common.dto.ReservationDTO;
import nz.ac.auckland.concert.common.dto.ReservationRequestDTO;
import nz.ac.auckland.concert.common.dto.SeatDTO;
import nz.ac.auckland.concert.common.dto.UserDTO;
import nz.ac.auckland.concert.service.domain.Booking;
import nz.ac.auckland.concert.service.domain.Concert;
import nz.ac.auckland.concert.service.domain.CreditCard;
import nz.ac.auckland.concert.service.domain.NewsItem;
import nz.ac.auckland.concert.service.domain.Performer;
import nz.ac.auckland.concert.service.domain.Seat;
import nz.ac.auckland.concert.service.domain.User;

/**
 * Helper class to convert between domain-model and DTO objects.
 *
 */
public class DomainMapper {
	static ConcertDTO concertToDTO(Concert concert){
		return new ConcertDTO(concert.getId(), concert.getTitle(), 
				concert.getDates(), concert.getTariff(), concert.getPerformerIds());
	}
	
	static Set<ConcertDTO> concertsToDTO(List<Concert> concerts){
		Set<ConcertDTO> concertDTOs = new HashSet<ConcertDTO>();
		for(Concert c : concerts){
			concertDTOs.add(concertToDTO(c));
		}
		return concertDTOs;
	}
	
	static PerformerDTO performerToDTO(Performer performer){
		return new PerformerDTO(performer.getId(), performer.getName(), 
				performer.getImageName(),performer.getGenre(), performer.getConcertIds());
	}
	
	
	static Set<PerformerDTO> performersToDTO(List<Performer> performers){
		Set<PerformerDTO> performerDTOs = new HashSet<PerformerDTO>();
		for(Performer p : performers){
			performerDTOs.add(performerToDTO(p));
		}
		return performerDTOs;
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
	
	static ReservationDTO createReservationDTO(Booking booking, ReservationRequestDTO reservationRequestDTO){
		return new ReservationDTO(booking.getId(), reservationRequestDTO, seatsToDTO(booking.getSeats()));
	}
	
	static BookingDTO bookingToDTO(Booking booking){
		return new BookingDTO(booking.getConcertId(), booking.getConcertTitle(), booking.getDateTime(), 
				seatsToDTO(booking.getSeats()), booking.getPriceBand());
	}
	
	static Set<BookingDTO> bookingsToDTO(List<Booking> bookings){
		Set<BookingDTO> bookingDTOs = new HashSet<BookingDTO>();
		for(Booking b : bookings){
			bookingDTOs.add(bookingToDTO(b));
		}
		return bookingDTOs;
	}
	
	static SeatDTO seatToDTO(Seat seat){
		return new SeatDTO(seat.getRow(), seat.getNumber());
	}
	
	static Seat seatToDomainModel(SeatDTO seat){
		return new Seat(seat.getRow(), seat.getNumber());
	}
	
	static Set<SeatDTO> seatsToDTO(Set<Seat> seats){
		Set<SeatDTO> seatDTOs = new HashSet<SeatDTO>();
		for(Seat s : seats){
			seatDTOs.add(seatToDTO(s));
		}
		return seatDTOs;
	}
	
	static Set<Seat> seatsToDomainModel(Set<SeatDTO> seatDTOs){
		Set<Seat> seats = new HashSet<Seat>();
		for(SeatDTO s : seatDTOs){
			seats.add(seatToDomainModel(s));
		}
		return seats;
	}	
	 
	static NewsItemDTO newsItemToDTO(NewsItem newsItem){
		return new NewsItemDTO(newsItem.getNewsItemId(), newsItem.getTimetamp(), newsItem.getContent());
	}
	
	static NewsItem newsItemToDomainModel(NewsItemDTO newsItem){
		return new NewsItem(newsItem.getId(), newsItem.getTimetamp(), newsItem.getContent());
	}
}
