package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@RestController @RequestMapping("/attachments") @PreAuthorize("hasRole('AGENCY_OWNER')") @RequiredArgsConstructor
public class AttachmentController {
 private static final long MAX_BYTES=5L*1024*1024;
 private static final Set<String> ALLOWED=Set.of("application/pdf","text/plain","text/csv","image/jpeg","image/png","image/webp");
 private final AgencyAttachmentRepository attachments; private final SaleRepository sales; private final FinancialEntryRepository financialEntries; private final AgencyRepository agencies;
 @PostMapping(value="/sales/{saleId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
 public AttachmentResponse uploadSale(@PathVariable UUID saleId,@RequestPart("file") MultipartFile file) throws IOException {UUID agency=TenantContext.requireAgencyId();Sale sale=sales.findByIdAndAgency_Id(saleId,agency).orElseThrow(()->new ResourceNotFoundException("venda não encontrada"));validate(file);AgencyAttachment saved=attachments.save(AgencyAttachment.builder().agency(agencies.getReferenceById(agency)).sale(sale).originalFilename(safeName(file.getOriginalFilename())).contentType(file.getContentType()).byteSize(file.getSize()).content(file.getBytes()).build());return AttachmentResponse.from(saved);}
 @PostMapping(value="/financial-entries/{entryId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
 public AttachmentResponse uploadFinancialEntry(@PathVariable UUID entryId,@RequestPart("file") MultipartFile file) throws IOException {UUID agency=TenantContext.requireAgencyId();FinancialEntry entry=financialEntries.findByIdAndAgency_Id(entryId,agency).orElseThrow(()->new ResourceNotFoundException("lançamento não encontrado"));validate(file);AgencyAttachment saved=attachments.save(AgencyAttachment.builder().agency(agencies.getReferenceById(agency)).financialEntry(entry).originalFilename(safeName(file.getOriginalFilename())).contentType(file.getContentType()).byteSize(file.getSize()).content(file.getBytes()).build());return AttachmentResponse.from(saved);}
 @GetMapping("/sales/{saleId}") public java.util.List<AttachmentResponse> listSale(@PathVariable UUID saleId){UUID agency=TenantContext.requireAgencyId();sales.findByIdAndAgency_Id(saleId,agency).orElseThrow(()->new ResourceNotFoundException("venda não encontrada"));return attachments.findBySale_IdAndAgency_IdOrderByCreatedAtDesc(saleId,agency).stream().map(AttachmentResponse::from).toList();}
 @GetMapping("/financial-entries/{entryId}") public java.util.List<AttachmentResponse> listFinancialEntry(@PathVariable UUID entryId){UUID agency=TenantContext.requireAgencyId();financialEntries.findByIdAndAgency_Id(entryId,agency).orElseThrow(()->new ResourceNotFoundException("lançamento não encontrado"));return attachments.findByFinancialEntry_IdAndAgency_IdOrderByCreatedAtDesc(entryId,agency).stream().map(AttachmentResponse::from).toList();}
 @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@PathVariable UUID id){AgencyAttachment a=attachments.findByIdAndAgency_Id(id,TenantContext.requireAgencyId()).orElseThrow(()->new ResourceNotFoundException("anexo não encontrado"));return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.getContentType())).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(a.getOriginalFilename()).build().toString()).body(a.getContent());}
 private static void validate(MultipartFile file){if(file.isEmpty()||file.getSize()>MAX_BYTES)throw new IllegalArgumentException("O anexo deve ter até 5 MB");if(file.getContentType()==null||!ALLOWED.contains(file.getContentType()))throw new IllegalArgumentException("Envie imagem, PDF ou arquivo de texto");}
 private static String safeName(String name){return name==null||name.isBlank()?"anexo":name.replaceAll("[\\r\\n]","");}
 public record AttachmentResponse(UUID id,String filename,String contentType,long size){static AttachmentResponse from(AgencyAttachment a){return new AttachmentResponse(a.getId(),a.getOriginalFilename(),a.getContentType(),a.getByteSize());}}
}
