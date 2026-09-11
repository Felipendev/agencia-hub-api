package com.agenciahub.api.application.usecases.quotation.shared;

import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.domain.QuotationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

/** Runs inside the quotation transaction, with the quotation row locked before the sale. */
@Service
@RequiredArgsConstructor
public class QuotationApprovalService {
    private final SaleRepository sales;
    private final SaleItemRepository items;
    private final ReceivableRepository receivables;
    private final PayableRepository payables;
    private final TripRepository trips;

    @Transactional
    public void synchronize(Quotation quotation) {
        List<Sale> linked = sales.findByQuotation_IdOrderByCreatedAtAsc(quotation.getId());
        if (linked.size() > 1) throw new IllegalArgumentException("Há várias vendas nesta cotação. Revise os vínculos antes de alterar a aprovação.");
        Sale sale = linked.isEmpty() ? null : sales.findForUpdate(linked.get(0).getId(), quotation.getAgency().getId()).orElseThrow();
        if (quotation.getStatus() != QuotationStatus.ACCEPTED) {
            if (sale != null && !"CANCELLED".equals(sale.getStatus())) {
                assertUnsettled(sale);
                sale.setStatus("CANCELLED");
                receivables.findBySale_Id(sale.getId()).forEach(r -> r.setStatus("CANCELLED"));
                payables.findBySale_Id(sale.getId()).forEach(p -> p.setStatus("CANCELLED"));
                trips.findByQuotation_Id(quotation.getId()).forEach(t -> t.setStatus("CANCELLED"));
            }
            return;
        }
        if (!"BRL".equals(quotation.getCurrency()) || quotation.getTotalAmount().signum() <= 0)
            throw new IllegalArgumentException("Para aprovar, informe um valor maior que zero em BRL.");
        if (sale == null) {
            sale = sales.save(Sale.builder().agency(quotation.getAgency()).customer(quotation.getCustomer())
                    .quotation(quotation).approvalManaged(true).status("DRAFT").totalAmount(quotation.getTotalAmount())
                    .saleDate(LocalDate.now()).notes("Cotação: " + quotation.getTitle()).build());
            receivables.save(Receivable.builder().sale(sale).installmentNumber(1).amount(sale.getTotalAmount())
                    .dueDate(LocalDate.now()).status("PENDING").build());
            items.save(SaleItem.builder().sale(sale).description(quotation.getTitle()).itemType("OTHER")
                    .saleAmount(sale.getTotalAmount()).customerPaysSupplierDirectly(false).build());
        } else {
            boolean reopening = "CANCELLED".equals(sale.getStatus());
            if (sale.getTotalAmount().compareTo(quotation.getTotalAmount()) != 0) {
                if (!"DRAFT".equals(sale.getStatus()) && !reopening)
                    throw new IllegalArgumentException("A venda já foi confirmada. Reverta a aprovação antes de alterar o valor da cotação.");
                List<Receivable> rows = receivables.findBySale_Id(sale.getId());
                List<SaleItem> lines = items.findBySale_Id(sale.getId());
                if (rows.size() != 1 || lines.size() != 1 || Boolean.TRUE.equals(lines.get(0).getCustomerPaysSupplierDirectly()))
                    throw new IllegalArgumentException("Revise o parcelamento da venda antes de alterar o valor da cotação.");
                assertUnsettled(sale);
                sale.setTotalAmount(quotation.getTotalAmount());
                rows.get(0).setAmount(sale.getTotalAmount());
                lines.get(0).setSaleAmount(sale.getTotalAmount());
            }
            if (reopening) {
                sale.setStatus("DRAFT");
                receivables.findBySale_Id(sale.getId()).forEach(r -> r.setStatus("PENDING"));
                payables.findBySale_Id(sale.getId()).forEach(p -> p.setStatus("PENDING"));
            }
        }
        List<Trip> linkedTrips = trips.findByQuotation_Id(quotation.getId());
        if (linkedTrips.isEmpty()) {
            trips.save(Trip.builder().agency(quotation.getAgency()).customer(quotation.getCustomer())
                    .quotation(quotation).sale(sale).serviceType("PACKAGE").status("UPCOMING")
                    .saleDate(sale.getSaleDate()).travelStartDate(quotation.getTravelStartDate())
                    .travelEndDate(quotation.getTravelEndDate()).notes(quotation.getTitle() + " — " + quotation.getDestination()).build());
        } else for (Trip trip : linkedTrips) {
            if (trip.getSale() != null && !trip.getSale().getId().equals(sale.getId()))
                throw new IllegalArgumentException("A viagem já está vinculada a outra venda.");
            trip.setSale(sale);
            if ("CANCELLED".equals(trip.getStatus())) trip.setStatus("UPCOMING");
        }
    }

    private void assertUnsettled(Sale sale) {
        if (receivables.findBySale_Id(sale.getId()).stream().anyMatch(r -> "PAID".equals(r.getStatus()))
                || payables.findBySale_Id(sale.getId()).stream().anyMatch(p -> "PAID".equals(p.getStatus())))
            throw new IllegalArgumentException("Estorne os recebimentos e pagamentos da venda antes de reverter a aprovação.");
    }
}
