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
import java.util.Comparator;
import com.agenciahub.api.exception.TripConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

/** Viagens/vendas emitidas: localizador, itinerário e vínculo opcional com cliente, fornecedor, cotação e venda. */
@RestController @RequestMapping("/trips") @PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')") @RequiredArgsConstructor
public class TripController {
 private final TripRepository trips; private final TripSegmentRepository segments; private final AgencyRepository agencies; private final CrmCustomerRepository customers; private final SupplierRepository suppliers; private final QuotationRepository quotations; private final SaleRepository sales;
 private final JdbcTemplate jdbc; private final ObjectMapper mapper;

 @GetMapping @Transactional public List<TripSummary> list(@RequestParam(required=false) UUID customerId,@RequestParam(required=false) UUID quotationId,@RequestParam(required=false) String locator,@RequestParam(required=false) String status,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(defaultValue="TRAVEL_START_ASC") TripSort sort){
  UUID a=TenantContext.requireAgencyId();
  List<Trip> all=customerId==null?trips.findAllByAgency_Id(a):trips.findAllByAgency_IdAndCustomer_Id(a,customerId);
  return all.stream()
   .filter(t->quotationId==null||(t.getQuotation()!=null&&quotationId.equals(t.getQuotation().getId())))
   .filter(t->locator==null||locator.isBlank()||(t.getBookingLocator()!=null&&t.getBookingLocator().toLowerCase().contains(locator.toLowerCase())))
   .filter(t->status==null||status.isBlank()||status.equalsIgnoreCase(t.getStatus()))
   // Quando há filtro de período, uma viagem sem partida não pertence ao resultado.
   .filter(t->from==null||(t.getTravelStartDate()!=null&&!t.getTravelStartDate().isBefore(from)))
   .filter(t->to==null||(t.getTravelStartDate()!=null&&!t.getTravelStartDate().isAfter(to)))
   .sorted(sort.comparator())
   .map(TripSummary::from).toList();
 }

 @GetMapping("/supplier-options") @Transactional public List<SupplierOption> supplierOptions(){
  UUID agency=TenantContext.requireAgencyId();
  List<SupplierOption> options=new ArrayList<>();
  suppliers.findAllByAgency_IdOrderByNameAsc(agency).forEach(s->options.add(new SupplierOption(s.getId(),s.getName(),"supplier","Fornecedor cadastrado")));
  customers.findAllByAgency_IdOrderByCreatedAtDesc(agency).stream()
   .filter(this::isSupplierPerson)
   .sorted(Comparator.comparing(CrmCustomer::getName,String.CASE_INSENSITIVE_ORDER))
   .forEach(c->options.add(new SupplierOption(c.getId(),c.getName(),"person",personTypes(c))));
  return options;
 }

 @GetMapping("/{id}") @Transactional public TripDetails get(@PathVariable UUID id){Trip t=find(id);return TripDetails.from(t,segments.findByTrip_IdOrderBySegmentNumberAsc(id));}

 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional public TripDetails create(@Valid @RequestBody TripRequest r,@RequestHeader(value="Idempotency-Key",required=false) UUID requestId){
  UUID a=TenantContext.requireAgencyId();
  String hash=null;
  if(requestId!=null){
   // Shared across application replicas and released automatically with the transaction.
   jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",a+":"+requestId);
   hash=requestHash(r);
   Trip previous=trips.findByAgency_IdAndCreationRequestId(a,requestId).orElse(null);
   if(previous!=null){
    if(!hash.equals(previous.getCreationRequestHash()))throw new TripConflictException("Os dados deste envio mudaram. Reabra o cadastro para registrar uma nova viagem.");
    return TripDetails.from(previous,segments.findByTrip_IdOrderBySegmentNumberAsc(previous.getId()));
   }
  }
  CrmCustomer customer=customers.findByIdAndAgency_Id(r.customerId(),a).orElseThrow(()->new ResourceNotFoundException("cliente não encontrado"));
  Quotation quotation=r.quotationId()==null?null:quotations.findForUpdate(r.quotationId(),a).orElseThrow(()->new ResourceNotFoundException("cotação não encontrada"));
  if(quotation!=null&&!customer.getId().equals(quotation.getCustomer().getId()))throw new IllegalArgumentException("A cotação deve pertencer ao cliente da viagem");
  ensureConversionAvailable(quotation,r.serviceType(),null);
  Supplier supplier=resolveSupplier(r.supplierId(),r.supplierCustomerId(),a);
  Sale sale=r.saleId()==null?null:sales.findByIdAndAgency_Id(r.saleId(),a).orElseThrow(()->new ResourceNotFoundException("venda não encontrada"));
  if(sale!=null&&!customer.getId().equals(sale.getCustomer().getId()))throw new IllegalArgumentException("A venda deve pertencer ao cliente da viagem");
  Trip trip=trips.save(Trip.builder().agency(agencies.getReferenceById(a)).customer(customer).supplier(supplier).quotation(quotation).sale(sale)
   .serviceType(r.serviceType().strip().toUpperCase(Locale.ROOT)).bookingLocator(blank(r.bookingLocator())).airline(blank(r.airline())).status(r.status()==null||r.status().isBlank()?"UPCOMING":r.status())
   .saleDate(r.saleDate()).travelStartDate(r.travelStartDate()).travelEndDate(r.travelEndDate()).notes(r.notes()==null?"":r.notes().strip()).creationRequestId(requestId).creationRequestHash(hash).build());
  saveSegments(trip,r.segments());
  return TripDetails.from(trip,segments.findByTrip_IdOrderBySegmentNumberAsc(trip.getId()));
 }

 @PatchMapping("/{id}") @Transactional public TripDetails update(@PathVariable UUID id,@Valid @RequestBody TripRequest r){
  UUID a=TenantContext.requireAgencyId(); Trip trip=find(id);
  CrmCustomer customer=customers.findByIdAndAgency_Id(r.customerId(),a).orElseThrow(()->new ResourceNotFoundException("cliente não encontrado"));
  Supplier supplier=resolveSupplier(r.supplierId(),r.supplierCustomerId(),a);
  Quotation quotation=r.quotationId()==null?null:quotations.findForUpdate(r.quotationId(),a).orElseThrow(()->new ResourceNotFoundException("cotação não encontrada"));
  if(quotation!=null&&!customer.getId().equals(quotation.getCustomer().getId()))throw new IllegalArgumentException("A cotação deve pertencer ao cliente da viagem");
  // Legacy duplicates remain editable; changing their linkage/type cannot create another duplicate.
  if(!Objects.equals(trip.getQuotation()==null?null:trip.getQuotation().getId(),r.quotationId())||!trip.getServiceType().equalsIgnoreCase(r.serviceType().strip()))ensureConversionAvailable(quotation,r.serviceType(),id);
  Sale sale=r.saleId()==null?null:sales.findByIdAndAgency_Id(r.saleId(),a).orElseThrow(()->new ResourceNotFoundException("venda não encontrada"));
  if(sale!=null&&!customer.getId().equals(sale.getCustomer().getId()))throw new IllegalArgumentException("A venda deve pertencer ao cliente da viagem");
  trip.setCustomer(customer);trip.setSupplier(supplier);trip.setQuotation(quotation);trip.setSale(sale);
  trip.setServiceType(r.serviceType().strip().toUpperCase(Locale.ROOT));trip.setBookingLocator(blank(r.bookingLocator()));trip.setAirline(blank(r.airline()));
  trip.setStatus(r.status()==null||r.status().isBlank()?trip.getStatus():r.status());
  trip.setSaleDate(r.saleDate());trip.setTravelStartDate(r.travelStartDate());trip.setTravelEndDate(r.travelEndDate());trip.setNotes(r.notes()==null?"":r.notes().strip());
  trips.save(trip);
  segments.findByTrip_IdOrderBySegmentNumberAsc(id).forEach(segments::delete);
  saveSegments(trip,r.segments());
  return TripDetails.from(trip,segments.findByTrip_IdOrderBySegmentNumberAsc(id));
 }

 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){trips.delete(find(id));}

 private void saveSegments(Trip trip,List<SegmentRequest> list){int n=1;for(SegmentRequest s:safe(list))segments.save(TripSegment.builder().trip(trip).segmentNumber(n++).origin(s.origin()).destination(s.destination()).departureAt(s.departureAt()).arrivalAt(s.arrivalAt()).airline(s.airline()).flightNumber(s.flightNumber()).ticketNumber(s.ticketNumber()).build());}
 private Trip find(UUID id){return trips.findByIdAndAgency_Id(id,TenantContext.requireAgencyId()).orElseThrow(()->new ResourceNotFoundException("viagem não encontrada"));}
 private Supplier resolveSupplier(UUID supplierId,UUID supplierCustomerId,UUID agencyId){
  if(supplierCustomerId!=null){
   CrmCustomer person=customers.findByIdAndAgency_Id(supplierCustomerId,agencyId).orElseThrow(()->new ResourceNotFoundException("pessoa fornecedora não encontrada"));
   if(!isSupplierPerson(person))throw new IllegalArgumentException("A pessoa selecionada não está marcada como fornecedor.");
   return suppliers.findFirstByAgency_IdAndNameIgnoreCase(agencyId,person.getName()).orElseGet(()->suppliers.save(Supplier.builder().agency(agencies.getReferenceById(agencyId)).name(person.getName()).contactName(person.getName()).email(blank(person.getEmail())).phone(blank(person.getPhone())).notes("Criado a partir da ficha de pessoa fornecedora.").build()));
  }
  return supplierId==null?null:suppliers.findByIdAndAgency_Id(supplierId,agencyId).orElseThrow(()->new ResourceNotFoundException("fornecedor não encontrado"));
 }
 private boolean isSupplierPerson(CrmCustomer customer){return customer.getProfileData()!=null&&customer.getProfileData().path("tipoFornecedor").asBoolean(false);}
 private String personTypes(CrmCustomer customer){
  List<String> types=new ArrayList<>(); var data=customer.getProfileData();
  if(data.path("tipoCliente").asBoolean(false))types.add("Cliente");
  if(data.path("tipoPassageiro").asBoolean(false))types.add("Passageiro");
  if(data.path("tipoFornecedor").asBoolean(false))types.add("Fornecedor");
  if(data.path("tipoRepresentante").asBoolean(false))types.add("Representante");
  return String.join(" · ",types);
 }
 private static <T> List<T> safe(List<T> list){return list==null?List.of():list;}
 private static String blank(String v){return v==null||v.isBlank()?null:v.strip();}
 private void ensureConversionAvailable(Quotation quotation,String serviceType,UUID currentId){
  if(quotation==null)return;
  boolean exists=trips.findByQuotation_Id(quotation.getId()).stream().filter(t->!t.getId().equals(currentId)).anyMatch(t->
   t.getServiceType().strip().equalsIgnoreCase(serviceType.strip())||
   ("PACKAGE".equalsIgnoreCase(t.getServiceType())&&blank(t.getBookingLocator())==null&&t.getSale()!=null&&t.getSale().isApprovalManaged()));
  if(exists)throw new TripConflictException("Já existe uma viagem para esta conversão. Abra a viagem vinculada à cotação para completar os dados.");
 }
 private String requestHash(TripRequest request){
  try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(request)));}
  catch(java.io.IOException|java.security.NoSuchAlgorithmException e){throw new IllegalStateException("Não foi possível identificar o envio da viagem.",e);}
 }

 public record SegmentRequest(String origin,String destination,Instant departureAt,Instant arrivalAt,String airline,String flightNumber,String ticketNumber){}
 public record TripRequest(@NotNull UUID customerId,UUID supplierId,UUID supplierCustomerId,UUID quotationId,UUID saleId,@jakarta.validation.constraints.NotBlank String serviceType,String bookingLocator,String airline,String status,LocalDate saleDate,LocalDate travelStartDate,LocalDate travelEndDate,String notes,List<@Valid SegmentRequest> segments){}
 public record SupplierOption(UUID id,String name,String source,String typeLabel){}
 public record SegmentResponse(UUID id,Integer segmentNumber,String origin,String destination,Instant departureAt,Instant arrivalAt,String airline,String flightNumber,String ticketNumber){static SegmentResponse from(TripSegment s){return new SegmentResponse(s.getId(),s.getSegmentNumber(),s.getOrigin(),s.getDestination(),s.getDepartureAt(),s.getArrivalAt(),s.getAirline(),s.getFlightNumber(),s.getTicketNumber());}}
 public enum TripSort {
  TRAVEL_START_ASC(Comparator.comparing(Trip::getTravelStartDate, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Trip::getId)),
  TRAVEL_START_DESC(Comparator.comparing(Trip::getTravelStartDate, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Trip::getId)),
  CREATED_AT_DESC(Comparator.comparing(Trip::getCreatedAt, Comparator.reverseOrder()).thenComparing(Trip::getId));
  private final Comparator<Trip> comparator;
  TripSort(Comparator<Trip> comparator){this.comparator=comparator;}
  Comparator<Trip> comparator(){return comparator;}
 }
 public record TripSummary(UUID id,UUID customerId,String customerName,String serviceType,String bookingLocator,String airline,String status,LocalDate travelStartDate,LocalDate travelEndDate,Instant createdAt,boolean approvalPlaceholder){static TripSummary from(Trip t){return new TripSummary(t.getId(),t.getCustomer().getId(),t.getCustomer().getName(),t.getServiceType(),t.getBookingLocator(),t.getAirline(),t.getStatus(),t.getTravelStartDate(),t.getTravelEndDate(),t.getCreatedAt(),"PACKAGE".equalsIgnoreCase(t.getServiceType())&&blank(t.getBookingLocator())==null&&t.getSale()!=null&&t.getSale().isApprovalManaged());}}
 public record TripDetails(TripSummary trip,UUID supplierId,String supplierName,UUID quotationId,UUID saleId,LocalDate saleDate,String notes,List<SegmentResponse> segments){static TripDetails from(Trip t,List<TripSegment> s){return new TripDetails(TripSummary.from(t),t.getSupplier()==null?null:t.getSupplier().getId(),t.getSupplier()==null?null:t.getSupplier().getName(),t.getQuotation()==null?null:t.getQuotation().getId(),t.getSale()==null?null:t.getSale().getId(),t.getSaleDate(),t.getNotes(),s.stream().map(SegmentResponse::from).toList());}}
}
