package com.kiroule.campsitebooking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.kiroule.campsitebooking.TestHelper;
import com.kiroule.campsitebooking.exception.BookingDatesNotAvailableException;
import com.kiroule.campsitebooking.exception.BookingNotFoundException;
import com.kiroule.campsitebooking.exception.IllegalBookingStateException;
import com.kiroule.campsitebooking.model.Booking;
import com.kiroule.campsitebooking.repository.BookingRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link BookingServiceImpl}.
 *
 * @author Igor Baiborodine
 */
@RunWith(MockitoJUnitRunner.class)
public class BookingServiceImplTest {

  @Mock
  private BookingRepository bookingRepository;

  @InjectMocks
  private BookingServiceImpl bookingService;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private TestHelper helper = new TestHelper();

  @Test
  public void findBookingById_nonExistingBookingId_bookingNotFoundExceptionThrown() {
    exception.expect(BookingNotFoundException.class);
    // given
    long id = 1L;
    doReturn(Optional.empty()).when(bookingRepository).findById(id);
    // when
    bookingService.findBookingById(id);
    // then
    // BookingNotFoundException is thrown
  }

  @Test
  public void findBookingById_existingBookingId_bookingFound() {
    // given
    long id = 1L;
    Booking savedBooking = helper.buildBooking(LocalDate.now(), LocalDate.now().plusDays(1));
    doReturn(Optional.of(savedBooking)).when(bookingRepository).findById(id);
    // when
    Booking booking = bookingService.findBookingById(id);
    // then
    assertThat(booking).isEqualTo(savedBooking);
  }

  @Test
  public void findVacantDate_rangeStartDateIsNow_illegalArgumentExceptionThrown() {
    exception.expect(IllegalArgumentException.class);
    // given
    LocalDate startDate = LocalDate.now();
    LocalDate endDate = LocalDate.now().plusDays(2);
    // when
    bookingService.findVacantDays(startDate, endDate);
    // then
    // IllegalArgumentException is thrown
  }

  @Test
  public void findVacantDate_rangeEndDateIsNow_illegalArgumentExceptionThrown() {
    exception.expect(IllegalArgumentException.class);
    // given
    LocalDate startDate = LocalDate.now().plusDays(2);
    LocalDate endDate = LocalDate.now();
    // when
    bookingService.findVacantDays(startDate, endDate);
    // then
    // IllegalArgumentException is thrown
  }

  @Test
  public void findVacantDate_rangeEndDateIsBeforeRangeStartDate_illegalArgumentExceptionThrown() {
    exception.expect(IllegalArgumentException.class);
    // given
    LocalDate startDate = LocalDate.now().plusDays(2);
    LocalDate endDate = LocalDate.now().plusDays(1);
    // when
    bookingService.findVacantDays(startDate, endDate);
    // then
    // IllegalArgumentException is thrown
  }

  @Test
  public void findVacantDates_bookingDatesOverlapRangeDates_noVacantDates() {
    // given: -S|-|----|-|E-
    LocalDate startDate = LocalDate.now().plusDays(2);
    LocalDate endDate = LocalDate.now().plusDays(3);
    doReturn(Lists.newArrayList(helper.buildBooking(
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(4))))
        .when(bookingRepository).findForDateRange(startDate, endDate);
    // then
    List<LocalDate> vacantDates = bookingService.findVacantDays(startDate, endDate);
    // when
    assertThat(vacantDates).isEmpty();
  }

  @Test
  public void findVacantDates_bookingDatesSameAsRangeDates_vacantRangeEndDate() {
    // given: --|S|----|E|--
    LocalDate startDate = LocalDate.now().plusDays(1);
    LocalDate endDate = LocalDate.now().plusDays(4);
    doReturn(
        Lists.newArrayList(helper.buildBooking(startDate, endDate)))
        .when(bookingRepository).findForDateRange(startDate, endDate);
    // then
    List<LocalDate> vacantDates = bookingService.findVacantDays(startDate, endDate);
    // when
    assertThat(vacantDates).size().isEqualTo(1);
    assertThat(vacantDates).contains(endDate);
  }

  @Test
  public void findVacantDates_noBookingsFound_vacantDatesWithinDateRangeInclusive() {
    // given: --|-|----|-|--
    LocalDate startDate = LocalDate.now().plusDays(1);
    LocalDate endDate = LocalDate.now().plusDays(4);
    doReturn(Lists.newArrayList())
        .when(bookingRepository).findForDateRange(startDate, endDate);
    // when
    List<LocalDate> vacantDates = bookingService.findVacantDays(startDate, endDate);
    // then
    List<LocalDate> expected = startDate
        .datesUntil(endDate.plusDays(1))
        .collect(Collectors.toList());
    assertThat(vacantDates).isEqualTo(expected);
  }

  @Test
  public void createBooking_bookingIsNotNew_illegalBookingStateExceptionThrown() {
    exception.expect(IllegalBookingStateException.class);
    // given
    Booking booking = helper.buildBooking(LocalDate.now(), LocalDate.now().plusDays(1));
    booking.setId(1L);
    // when
    bookingService.createBooking(booking);
    // then
    // IllegalBookingStateException thrown
  }

  @Test
  public void createBooking_bookingDatesNotAvailable_bookingDatesNotAvailableExceptionThrown() {
    exception.expect(BookingDatesNotAvailableException.class);
    // given
    Booking booking = helper.buildBooking(LocalDate.now().plusDays(1), LocalDate.now().plusDays(4));
    doReturn(Lists.newArrayList(helper.buildBooking(
        LocalDate.now().plusDays(1), LocalDate.now().plusDays(2))))
        .when(bookingRepository).findForDateRange(booking.getStartDate(), booking.getEndDate());
    // when
    bookingService.createBooking(booking);
    // then
    // BookingDatesNotAvailableException thrown
  }

  @Test
  public void createBooking_bookingDatesAvailable_bookingCreated() {
    // given
    Booking booking = helper.buildBooking(LocalDate.now().plusDays(1), LocalDate.now().plusDays(4));
    doReturn(Lists.newArrayList())
        .when(bookingRepository).findForDateRange(booking.getStartDate(), booking.getEndDate());
    // when
    bookingService.createBooking(booking);
    // then
    verify(bookingRepository, times(1)).save(booking);
  }

  @Test
  public void updateBooking_bookingIsCancelled_illegalBookingStateExceptionThrown() {
    exception.expect(IllegalBookingStateException.class);
    // given
    Long id = 1L;
    Booking booking = helper.buildBooking(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
    booking.setId(id);

    Booking persistedBooking = helper.buildBooking(
        LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
    persistedBooking.setId(id);
    persistedBooking.setActive(false);
    doReturn(Optional.of(persistedBooking)).when(bookingRepository).findById(id);
    // when
    bookingService.updateBooking(id, booking);
    // then
    // IllegalBookingStateException thrown
  }

  @Test
  public void updateBooking_bookingDatesNotAvailable_bookingDatesNotAvailableExceptionThrown() {
    exception.expect(BookingDatesNotAvailableException.class);
    // given
    Long id = 1L;
    Booking booking = helper.buildBooking(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
    booking.setId(id);

    Booking persistedBooking = helper.buildBooking(
        LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
    persistedBooking.setId(id);
    doReturn(Optional.of(persistedBooking)).when(bookingRepository).findById(id);

    Booking otherBooking = helper.buildBooking(
        LocalDate.now().plusDays(2), LocalDate.now().plusDays(3));
    otherBooking.setId(2L);
    doReturn(Lists.newArrayList(persistedBooking, otherBooking))
        .when(bookingRepository).findForDateRange(booking.getStartDate(), booking.getEndDate());
    // when
    bookingService.updateBooking(id, booking);
    // then
    // IllegalBookingStateException thrown
  }

  @Test
  public void updateBooking_bookingDatesAvailable_bookingUpdated() {
    // given
    Long id = 1L;
    Booking booking = helper.buildBooking(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
    booking.setId(id);

    Booking persistedBooking = helper.buildBooking(
        LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
    persistedBooking.setId(id);
    doReturn(Optional.of(persistedBooking)).when(bookingRepository).findById(id);

    doReturn(Lists.newArrayList(persistedBooking))
        .when(bookingRepository).findForDateRange(booking.getStartDate(), booking.getEndDate());
    // when
    bookingService.updateBooking(id, booking);
    // then
    verify(bookingRepository, times(1)).save(booking);
  }

}