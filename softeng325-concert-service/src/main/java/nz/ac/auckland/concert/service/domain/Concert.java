package nz.ac.auckland.concert.service.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import nz.ac.auckland.concert.common.types.PriceBand;
import nz.ac.auckland.concert.service.domain.jpa.LocalDateTimeConverter;

/**
 * Class to represent concerts. 
 * 
 * A Concert describes a concert in terms of:
 * _id           the unique identifier for a concert.
 * _title        the concert's title.
 * _dates        the concert's scheduled dates and times (represented as a 
 *               Set of LocalDateTime instances).
 * _tariff       concert pricing - the cost of a ticket for each price band 
 *               (A, B and C) is set individually for each concert. 
 * _performers   each performer playing at a concert 
 *
 */
@Entity
public class Concert {
	
	@Id
	@GeneratedValue
	private Long _id;
	
	private String _title;
	
	@ElementCollection
	@CollectionTable(name = "CONCERT_DATES")
	@Convert(converter = LocalDateTimeConverter.class)
	private Set<LocalDateTime> _dates;
	
	@ElementCollection
    @MapKeyColumn(name="PRICE_BAND")
	@MapKeyEnumerated(EnumType.STRING)
    @Column(name="COST")
	@CollectionTable(name = "CONCERT_TARIFS")	
	private Map<PriceBand, BigDecimal> _tariff;
	
	@ManyToMany
	@JoinTable(
		name = "CONCERT_PERFORMER"
	)
	private Set<Performer> _performers;

	public Concert() {
	}

	public Concert(Long id, String title, Set<LocalDateTime> dates,
			Map<PriceBand, BigDecimal> ticketPrices, Set<Performer> performers) {
		_id = id;
		_title = title;
		_dates = new HashSet<LocalDateTime>(dates);
		_tariff = new HashMap<PriceBand, BigDecimal>(ticketPrices);
		_performers = new HashSet<Performer>(performers);
	}

	public Concert(String title, Set<LocalDateTime> dates,
			Map<PriceBand, BigDecimal> ticketPrices, Set<Performer> performers) {
		this(null, title, dates, ticketPrices, performers);
	}
	
	public Long getId() {
		return _id;
	}

	public String getTitle() {
		return _title;
	}

	public Set<LocalDateTime> getDates() {
		return Collections.unmodifiableSet(_dates);
	}

	public BigDecimal getTicketPrice(PriceBand seatType) {
		return _tariff.get(seatType);
	}

	public Set<Performer> getPerformers() {
		return Collections.unmodifiableSet(_performers);
	}
	
	public Map<PriceBand, BigDecimal> getTariff(){
		return _tariff;
	}
	
	public Set<Long> getPerformerIds(){
		Set<Long> performerIds = new HashSet<Long>();
		for(Performer p : _performers){
			performerIds.add(p.getId());
		}
		return performerIds;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Concert))
            return false;
        if (obj == this)
            return true;

        Concert rhs = (Concert) obj;
        return new EqualsBuilder().
            append(_title, rhs._title).
            append(_dates, rhs._dates).
            append(_tariff, rhs._tariff).
            append(_performers, rhs._performers).
            isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31). 
	            append(_title).
	            append(_dates).
	            append(_tariff).
	            append(_performers).
	            hashCode();
	}
}
