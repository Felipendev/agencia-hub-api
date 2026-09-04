package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.TenantContext;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Viagens/vendas emitidas: localizador, itinerário e vínculo opcional com cliente, fornecedor, cotação e venda. */
@RestController @RequestMapping("/trips") @PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')") @RequiredArgsConstructor
public class TripController {
 private final TripRepository trips; private final TripSegmentRepository segments; private final AgencyRepository agencies; private final CrmCustomerRepository customers; private final SupplierRepository suppliers; private final QuotationRepository quotations; private final SaleRepository sales;

 @GetMapping @Transactional public List<TripSummary> list(@RequestParam(required=false) UUID customerId,@RequestParam(required=false) String locator,@RequestParam(required=false) String status,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){
  UUID a=TenantContext.requireAgencyId();
  List<Trip> all=customerId==null?trips.findAllByAgency_IdOrderByTravelStartDateDesc(a):trips.findAllByAgency_IdAndCustomer_IdOrderByTravelStartDateDesc(a,customerId);
  return all.stream()
   .filter(t->locator==null||locator.isBlank()||(t.getBookingLocator()!=null&&t.getBookingLocator().toLowerCase().contains(locator.toLowerCase())))
   .filter(t->status==null||status.isBlank()||status.equalsIgnoreCase(t.getStatus()))
   .filter(t->from==null||t.getTravelStartDate()==null||!t.getTravelStartDate().isBefore(from))
   .filter(t->to==null||t.getTravelStartDate()==null||!t.getTravelStartDate().isAfter(to))
   .map(TripSummary::from).toList();
 }

 @GetMapping("/{id}") @Transactional public TripDetails get(@PathVariable UUID id){Trip t=find(id);return TripDetails.from(t,segments.findByTrip_IdOrderBySegmentNumberAsc(id));}

 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional public TripDetails create(@Valid @RequestBody TripRequest r){
  UUID a=TenantContext.requireAgencyId();
  CrmCustomer customer=customers.findByIdAndAgency_Id(r.customerId(),a).orElseThrow(()->new ResourceNotFoundException("cliente não encontrado"));
  Supplier supplier=r.supplierId()==null?null:suppliers.findByIdAndAgency_Id(r.supplierId(),a).orElseThrow(()->new ResourceNotFoundException("fornecedor não encontrado"));
  Quotation quotation=r.quotationId()==null?null:quotations.findByIdAndAgency_Id(r.quotationId(),a).orElseThrow(()->new ResourceNotFoundException("cotação não encontrada"));
  if(quotation!=null&&!customer.getId().equals(quotation.getCustomer().getId()))throw new IllegalArgumentException("A cotação deve pertencer ao cliente da viagem");
  Sale sale=r.saleId()==null?null:sales.findByIdAndAgency_Id(r.saleId(),a).orElseThrow(()->new ResourceNotFoundException("venda não encontrada"));
  if(sale!=null&&!customer.getId().equals(sale.getCustomer().getId()))throw new IllegalArgumentException("A venda deve pertencer ao cliente da viagem");
  Trip trip=trips.save(Trip.builder().agency(agencies.getReferenceById(a)).customer(customer).supplier(supplier).quotation(quotation).sale(sale)
   .serviceType(r.serviceType()).bookingLocator(blank(r.bookingLocator())).airline(blank(r.airline())).status(r.status()==null||r.status().isBlank()?"UPCOMING":r.status())
   .saleDate(r.saleDate()).travelStartDate(r.travelStartDate()).travelEndDate(r.travelEndDate()).notes(r.notes()==null?"":r.notes().strip()).build());
  saveSegments(trip,r.segments());
  return TripDetails.from(trip,segments.findByTrip_IdOrderBySegmentNumberAsc(trip.getId()));
 }

 @PatchMapping("/{id}") @Transactional public TripDetails update(@PathVariable UUID id,@Valid @RequestBody TripRequest r){
  UUID a=TenantContext.requireAgencyId(); Trip trip=find(id);
  CrmCustomer customer=customers.findByIdAndAgency_Id(r.customerId(),a).orElseThrow(()->new ResourceNotFoundException("cliente não encontrado"));
  Supplier supplier=r.supplierId()==null?null:suppliers.findByIdAndAgency_Id(r.supplierId(),a).orElseThrow(()->new ResourceNotFoundException("fornecedor não encontrado"));
  Quotation quotation=r.quotationId()==null?null:quotations.findByIdAndAgency_Id(r.quotationId(),a).orElseThrow(()->new ResourceNotFoundException("cotação não encontrada"));
  Sale sale=r.saleId()==null?null:sales.findByIdAndAgency_Id(r.saleId(),a).orElseThrow(()->new ResourceNotFoundException("venda não encontrada"));
  trip.setCustomer(customer);trip.setSupplier(supplier);trip.setQuotation(quotation);trip.setSale(sale);
  trip.setServiceType(r.serviceType());trip.setBookingLocator(blank(r.bookingLocator()));trip.setAirline(blank(r.airline()));
  trip.setStatus(r.status()==null||r.status().isBlank()?trip.getStatus():r.status());
  trip.setSaleDate(r.saleDate());trip.setTravelStartDate(r.travelStartDate());trip.setTravelEndDate(r.travelEndDate());trip.setNotes(r.notes()==null?"":r.notes().strip());
  trips.save(trip);
  segments.findByTrip_IdOrderBySegmentNumberAsc(id).forEach(segments::delete);
  saveSegments(trip,r.segments());
  return TripDetails.from(trip,segments.findByTrip_IdOrderBySegmentNumberAsc(id));
 }

 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('AGENCY_OWNER')") public void delete(@PathVariable UUID id){trips.delete(find(id));}

 private void saveSegments(Trip trip,List<SegmentRequest> list){int n=1;for(SegmentRequest s:safe(list))segments.save(TripSegment.builder().trip(trip).segmentNumber(n++).origin(s.origin()).destination(s.destination()).departureAt(s.departureAt()).arrivalAt(s.arrivalAt()).airline(s.airline()).flightNumber(s.flightNumber()).ticketNumber(s.ticketNumber()).build());}
 private Trip find(UUID id){return trips.findByIdAndAgency_Id(id,TenantContext.requireAgencyId()).orElseThrow(()->new ResourceNotFoundException("viagem não encontrada"));}
 private static <T> List<T> safe(List<T> list){return list==null?List.of():list;}
 private static String blank(String v){return v==null||v.isBlank()?null:v.strip();}

 public record SegmentRequest(String origin,String destination,Instant departureAt,Instant arrivalAt,String airline,String flightNumber,String ticketNumber){}
 public record TripRequest(@NotNull UUID customerId,UUID supplierId,UUID quotationId,UUID saleId,@NotNull String serviceType,String bookingLocator,String airline,String status,LocalDate saleDate,LocalDate travelStartDate,LocalDate travelEndDate,String notes,List<@Valid SegmentRequest> segments){}
 public record SegmentResponse(UUID id,Integer segmentNumber,String origin,String destination,Instant departureAt,Instant arrivalAt,String airline,String flightNumber,String ticketNumber){static SegmentResponse from(TripSegment s){return new SegmentResponse(s.getId(),s.getSegmentNumber(),s.getOrigin(),s.getDestination(),s.getDepartureAt(),s.getArrivalAt(),s.getAirline(),s.getFlightNumber(),s.getTicketNumber());}}
 public record TripSummary(UUID id,UUID customerId,String customerName,String serviceType,String bookingLocator,String airline,String status,LocalDate travelStartDate,LocalDate travelEndDate){static TripSummary from(Trip t){return new TripSummary(t.getId(),t.getCustomer().getId(),t.getCustomer().getName(),t.getServiceType(),t.getBookingLocator(),t.getAirline(),t.getStatus(),t.getTravelStartDate(),t.getTravelEndDate());}}
 public record TripDetails(TripSummary trip,UUID supplierId,String supplierName,UUID quotationId,UUID saleId,LocalDate saleDate,String notes,List<SegmentResponse> segments){static TripDetails from(Trip t,List<TripSegment> s){return new TripDetails(TripSummary.from(t),t.getSupplier()==null?null:t.getSupplier().getId(),t.getSupplier()==null?null:t.getSupplier().getName(),t.getQuotation()==null?null:t.getQuotation().getId(),t.getSale()==null?null:t.getSale().getId(),t.getSaleDate(),t.getNotes(),s.stream().map(SegmentResponse::from).toList());}}
}
